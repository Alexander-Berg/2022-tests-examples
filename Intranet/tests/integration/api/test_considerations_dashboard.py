import datetime
import json

import pytest

from functools import partial

from constance.test import override_config
from django.urls.base import reverse

from intranet.femida.src.candidates.choices import (
    CANDIDATE_STATUSES,
    CANDIDATE_RESPONSIBLE_ROLES,
    CONSIDERATION_STATUSES,
    CONSIDERATION_EXTENDED_STATUSES,
    CONSIDERATION_RESOLUTIONS,
    CONSIDERATION_DASHBOARD_STAGES,
    SLA_STATUSES,
    SUBMISSION_STATUSES,
    ROTATION_STATUSES,
)
from intranet.femida.src.candidates.consideration_issues.issue_types import (
    ExtendedStatusChangedIssueType,
    InterviewNotFinishedIssueType,
)
from intranet.femida.src.offers.choices import SOURCES
from intranet.femida.src.utils.datetime import shifted_now

from intranet.femida.tests import factories as f
from intranet.femida.tests.utils import shifted_now_str


pytestmark = pytest.mark.django_db


def test_dashboard(client, django_assert_num_queries):
    recruiter = f.create_recruiter()

    # Создаём кандидата с активным рассмотрением (с городами и профессиями)
    candidate = f.create_heavy_candidate()
    candidate.responsibles.add(recruiter)

    # Создаём 2 кандидатов с активным рассмотрением (с городами и профессиями)
    # – действующего (ротация) и бывшего сотрудника
    for is_dismissed in (False, True):
        source = {}
        rotation = None
        employee = f.create_user(is_dismissed=is_dismissed)
        if not is_dismissed:
            source['source'] = SOURCES.rotation
            rotation = f.RotationFactory(created_by=employee, status=ROTATION_STATUSES.approved)
        candidate = f.create_heavy_candidate(login=employee.username, **source)
        if not is_dismissed:
            f.SubmissionFactory(candidate=candidate,
                                status=SUBMISSION_STATUSES.closed,
                                rotation=rotation)
        candidate.responsibles.add(recruiter)

    # Создаём рассмотрение с завершённой секцией
    completed_consideration = f.create_completed_consideration()
    completed_consideration.candidate.responsibles.add(recruiter)
    [f.ConsiderationIssueFactory(consideration=completed_consideration) for _ in range(5)]

    client.login(recruiter.username)
    url = reverse('api:considerations:dashboard')
    params = {
        'stage': CONSIDERATION_DASHBOARD_STAGES.in_progress,
        'users': recruiter.username,
        'role': '',
    }

    # 3 запроса: получение пользователя и его прав
    # 1 запрос:  валидация параметра users
    # 4 запроса: основной + count + 2 раза проверка waffle-свитча is_rkn
    # 3 запроса: config.EXTENDED_STATUS_SLA
    # 2 запроса: waffle switch (permissions) TODO: убрать
    with django_assert_num_queries(13):
        response = client.get(url, params)

    assert response.status_code == 200, response.content
    response_data = response.json()
    assert response_data['count'] == 4


@pytest.mark.parametrize('sla_conf, time_delta, result_status', (
    pytest.param(
        'simple', 0, None,
        id='all-ok',
    ),
    pytest.param(
        'other', 100, None,
        id='other-conf',
    ),
    pytest.param(
        None, 100, None,
        id='no-conf',
    ),
    pytest.param(
        'simple', 4, SLA_STATUSES.danger,
        id='danger',
    ),
    pytest.param(
        'simple', 2, SLA_STATUSES.warning,
        id='warning',
    ),
),
)
def test_dashboard_sla(client, sla_conf, time_delta, result_status):
    if sla_conf == 'simple':
        sla_conf = {
            CONSIDERATION_DASHBOARD_STAGES.in_progress: {
                SLA_STATUSES.danger: 3,
                SLA_STATUSES.warning: 2,
            },
        }
    elif sla_conf == 'other':
        sla_conf = {
            CONSIDERATION_DASHBOARD_STAGES.challenge_assigned: {
                SLA_STATUSES.danger: 3,
                SLA_STATUSES.warning: 2,
            },
        }
    else:
        sla_conf = None

    recruiter = f.create_recruiter()
    candidate = f.create_candidate_with_responsibles(main_recruiter=recruiter)
    completed_consideration = f.create_completed_consideration(candidate)
    completed_consideration.candidate.responsibles.add(recruiter)
    history_rec = completed_consideration.consideration_history.first()
    history_rec.changed_at = history_rec.changed_at - datetime.timedelta(days=time_delta)
    history_rec.save()

    client.login(recruiter.username)
    url = reverse('api:considerations:dashboard')
    params = {'stage': CONSIDERATION_DASHBOARD_STAGES.in_progress, 'users': recruiter.username}
    if sla_conf:
        with override_config(EXTENDED_STATUS_SLA=json.dumps(sla_conf)):
            response = client.get(url, params)
    else:
        response = client.get(url, params)

    assert response.status_code == 200, response.content
    data = response.json()
    assert data['results'][0]['sla_status'] == result_status


def test_dashboard_extended_status_changed_at(client):
    recruiter = f.create_recruiter()
    candidate = f.create_candidate_with_consideration(main_recruiter=recruiter)
    candidate.responsibles.add(recruiter)
    consideration = candidate.considerations.first()
    client.login(recruiter.username)
    url = reverse('api:considerations:dashboard')
    params = {'stage': consideration.extended_status, 'users': [recruiter.username]}

    response = client.get(url, params)
    assert response.status_code == 200, response.content
    response_data = response.json()
    assert response_data['count'] == 1
    extended_status_changed_at = response_data['results'][0]['extended_status_changed_at']
    assert extended_status_changed_at

    consideration.extended_status = CONSIDERATION_EXTENDED_STATUSES.final_assigned
    consideration.save()
    params['stage'] = consideration.extended_status

    response = client.get(url, params)
    assert response.status_code == 200, response.content
    response_data = response.json()
    assert response_data['count'] == 1
    assert response_data['results'][0]['extended_status_changed_at'] != extended_status_changed_at


@pytest.mark.parametrize('status, resolution, stage, expected_count', (
    (CANDIDATE_STATUSES.in_progress, '', CONSIDERATION_DASHBOARD_STAGES.in_progress, 1),
    (CANDIDATE_STATUSES.in_progress, '', CONSIDERATION_DASHBOARD_STAGES.rejected_by_resume, 0),
    (
        CANDIDATE_STATUSES.closed,
        CONSIDERATION_RESOLUTIONS.rejected_by_resume,
        CONSIDERATION_DASHBOARD_STAGES.rejected_by_resume,
        1,
    ),
))
def test_dashboard_by_stage(client, status, resolution, stage, expected_count):
    recruiter = f.create_recruiter()
    candidate = f.create_candidate_with_consideration(status=status, main_recruiter=recruiter)
    consideration = candidate.considerations.first()
    consideration.resolution = resolution
    consideration.save()

    client.login(recruiter.username)
    url = reverse('api:considerations:dashboard')
    params = {'stage': stage, 'users': recruiter.username}

    response = client.get(url, params)
    assert response.status_code == 200
    response_data = response.json()
    assert response_data['count'] == expected_count
    if expected_count:
        assert response_data['results'][0]['id'] == consideration.id


@pytest.mark.parametrize('role, expected_count', (
    pytest.param(CANDIDATE_RESPONSIBLE_ROLES.main_recruiter, 1, id='main-recruiter'),
    pytest.param(CANDIDATE_RESPONSIBLE_ROLES.recruiter, 2, id='recruiter'),
    pytest.param('', 3, id='all-recruiters'),
    pytest.param(None, 1, id='default-recruiters'),
))
def test_dashboard_filter_by_role(client, role, expected_count):
    recruiter = f.create_recruiter()
    f.create_candidate_with_consideration(main_recruiter=recruiter)

    f.create_candidate_with_consideration(recruiter=recruiter)
    f.create_candidate_with_consideration(recruiter=recruiter)

    client.login(recruiter.username)
    url = reverse('api:considerations:dashboard')
    params = {
        'stage': CONSIDERATION_DASHBOARD_STAGES.in_progress,
        'users': recruiter.username,
    }
    if role is not None:
        params['role'] = role

    response = client.get(url, params)
    assert response.status_code == 200
    response_data = response.json()
    assert response_data['count'] == expected_count


@pytest.mark.parametrize('finished, finished__gte, finished__lte, expected_count', (
    (-1, -30, None, 1),
    (-1, None, -30, 0),
    (-10, -14, -7, 1),
    (-20, -14, -7, 0),
))
def test_dashboard_filter_by_finished(client, finished, finished__gte,
                                      finished__lte, expected_count):
    recruiter = f.create_recruiter()
    f.create_consideration_with_responsibles(
        state=CONSIDERATION_STATUSES.archived,
        resolution=CONSIDERATION_RESOLUTIONS.rejected_by_resume,
        main_recruiter=recruiter,
        finished=shifted_now(days=finished),
    )

    client.login(recruiter.username)
    url = reverse('api:considerations:dashboard')
    params = {
        'stage': CONSIDERATION_DASHBOARD_STAGES.rejected_by_resume,
        'users': recruiter.username,
        'finished__gte': shifted_now_str(days=finished__gte) if finished__gte else '',
        'finished__lte': shifted_now_str(days=finished__lte) if finished__lte else '',
    }

    response = client.get(url, params)
    assert response.status_code == 200
    response_data = response.json()
    assert response_data['count'] == expected_count


def test_dashboard_filter_form(client):
    recruiter = f.create_recruiter()
    client.login(recruiter.username)
    url = reverse('api:considerations:dashboard-filter-form')
    response = client.get(url)
    assert response.status_code == 200
    response_data = response.json()
    assert response_data['data']['users']['value'][0] == recruiter.username


@pytest.mark.parametrize('data, expected_count', (
    ({'only_with_issues': True, 'issue_types': ExtendedStatusChangedIssueType.type_name}, 2),
    ({'only_with_issues': True}, 3),
    ({'only_with_issues': False}, 4),
))
def test_dashboard_filter_by_issue_types(client, data, expected_count):
    recruiter = f.create_recruiter()
    client.login(recruiter.username)

    create_candidate_with_in_progress_consideration_and_recruiter = partial(
        f.create_candidate_with_consideration,
        status=CANDIDATE_STATUSES.in_progress,
        main_recruiter=recruiter,
    )
    candidate1 = create_candidate_with_in_progress_consideration_and_recruiter()
    candidate2 = create_candidate_with_in_progress_consideration_and_recruiter()
    candidate3 = create_candidate_with_in_progress_consideration_and_recruiter()
    create_candidate_with_in_progress_consideration_and_recruiter()

    f.ConsiderationIssueFactory(
        consideration=candidate1.considerations.first(),
        type=ExtendedStatusChangedIssueType.type_name,
    )
    f.ConsiderationIssueFactory(
        consideration=candidate2.considerations.first(),
        type=ExtendedStatusChangedIssueType.type_name,
    )
    f.ConsiderationIssueFactory(
        consideration=candidate3.considerations.first(),
        type=InterviewNotFinishedIssueType.type_name,
    )

    url = reverse('api:considerations:dashboard')
    params = {
        'stage': CONSIDERATION_DASHBOARD_STAGES.in_progress,
        'users': recruiter.username,
        **data,
    }

    response = client.get(url, params)
    assert response.status_code == 200, response.content
    response_data = response.json()
    assert response_data['count'] == expected_count


def test_dashboard_filter_by_vacancy(client):
    recruiter = f.create_recruiter()
    client.login(recruiter.username)

    create_candidate_with_in_progress_consideration_and_recruiter = partial(
        f.create_candidate_with_consideration,
        status=CANDIDATE_STATUSES.in_progress,
        main_recruiter=recruiter,
    )
    candidate1 = create_candidate_with_in_progress_consideration_and_recruiter()
    candidate2 = create_candidate_with_in_progress_consideration_and_recruiter()

    vacancy1 = f.create_vacancy()
    vacancy2 = f.create_vacancy()

    f.create_application(vacancy=vacancy1, consideration=candidate1.considerations.first())
    f.create_application(vacancy=vacancy2, consideration=candidate2.considerations.first())

    url = reverse('api:considerations:dashboard')
    params = {
        'stage': CONSIDERATION_DASHBOARD_STAGES.in_progress,
        'users': recruiter.username,
        'vacancies': [vacancy1.id],
    }

    response = client.get(url, params)
    assert response.status_code == 200, response.content
    response_data = response.json()
    assert response_data['count'] == 1
