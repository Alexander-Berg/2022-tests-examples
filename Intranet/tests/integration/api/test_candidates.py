import json
import uuid

import pytest

from collections import Counter
from unittest.mock import patch, ANY, Mock

from constance.test import override_config
from django.conf import settings
from django.db.models import F
from django.contrib.auth import get_user_model
from django.urls.base import reverse
from waffle.testutils import override_switch

from intranet.femida.src.candidates import choices
from intranet.femida.src.candidates.choices import (
    CANDIDATE_RESPONSIBLE_ROLES,
    CONSIDERATION_EXTENDED_STATUSES,
    CONSIDERATION_RECRUITER_STAGES,
    CONSIDERATION_RECRUITER_STAGE_EXTENDED_STATUSES,
    CONSIDERATION_RESOLUTIONS,
    CONSIDERATION_STATUSES,
    REFERENCE_STATUSES,
    CANDIDATE_SORTING_TYPES,
    VERIFICATION_STATUSES,
    VERIFICATION_TYPES,
)
from intranet.femida.src.candidates.models import (
    Candidate,
    CandidateResponsible,
)
from intranet.femida.src.communications.choices import (
    MESSAGE_TEMPLATE_TYPES,
    MESSAGE_STATUSES,
    MESSAGE_TYPES,
)
from intranet.femida.src.core.switches import TemporarySwitch
from intranet.femida.src.interviews.choices import (
    AA_TYPES,
    APPLICATION_RESOLUTIONS,
    APPLICATION_STATUSES,
    INTERVIEW_ROUND_LUNCH_DURATIONS,
    INTERVIEW_ROUND_ORDERINGS,
    INTERVIEW_ROUND_TYPES,
    INTERVIEW_TYPES,
    INTERVIEW_STATES,
)
from intranet.femida.src.interviews.models import Interview, InterviewRound
from intranet.femida.src.offers.choices import SOURCES
from intranet.femida.src.staff.choices import DEPARTMENT_TAGS
from intranet.femida.src.utils.datetime import shifted_now
from intranet.femida.src.vacancies.choices import VACANCY_PRO_LEVELS, VACANCY_ROLES

from intranet.femida.tests import factories as f
from intranet.femida.tests.utils import run_commit_hooks


User = get_user_model()

pytestmark = pytest.mark.django_db


@pytest.mark.parametrize('extra_flag', (None, 'only_actual', 'only_related_to_user'))
def test_candidate_list(su_client, extra_flag):
    candidate = f.create_heavy_candidate()
    candidate.responsibles.add(f.get_superuser())
    url = reverse('api:candidates:list')
    params = {}
    if extra_flag is not None:
        params[extra_flag] = True
    response = su_client.get(url, params)
    assert response.status_code == 200


@pytest.mark.parametrize('view_name, is_wrong_req, status_code', (
    ('filter', False, 200),
    ('filter', True, 400),
    ('filter-form', False, 200),
    ('filter-form', True, 200),
))
def test_candidates_filter(su_client, view_name, is_wrong_req, status_code):
    candidate_profession = f.CandidateProfessionFactory()
    url = reverse(f'api:candidates:{view_name}')
    value = 100500 if is_wrong_req else candidate_profession.profession.id
    filter_data = [
        {
            'field': 'ProfessionFilter',
            'condition': 'Any',
            'values': [value],
        },
    ]
    params = {'filters': json.dumps(filter_data)}
    response = su_client.get(url, params)
    assert response.status_code == status_code, response.content
    if view_name == 'filter':
        response_data = response.json()
        if is_wrong_req:
            assert response_data['errors']['filters[0][values]'][0]['code'] == 'invalid_choice'
        else:
            assert response_data['results'][0]['id'] == candidate_profession.candidate.id
            assert response_data['count'] == 1


@pytest.mark.parametrize('sort', (
    pytest.param(CANDIDATE_SORTING_TYPES.modified_asc, id='asc'),
    pytest.param(CANDIDATE_SORTING_TYPES.modified_desc, id='desc'),
))
def test_filter_ordering(su_client, sort):
    profession = f.ProfessionFactory()
    candidate_ids = []

    for idx in range(10):
        created_dt = shifted_now(days=idx)
        candidate = f.CandidateFactory(created=created_dt)
        f.CandidateProfessionFactory(profession=profession, candidate=candidate)
        candidate_ids.append(candidate.id)

    Candidate.objects.update(modified=F('created'))

    url = reverse('api:candidates:filter')
    filter_data = [
        {
            'field': 'ProfessionFilter',
            'condition': 'Any',
            'values': [profession.id],
        },
    ]
    params = {'filters': json.dumps(filter_data), 'sort': sort, 'page_size': 5}
    response = su_client.get(url, params)

    assert response.status_code == 200, response.content
    data = response.json()
    result_ids = [item['id'] for item in data['results']]
    if sort == CANDIDATE_SORTING_TYPES.modified_asc:
        assert result_ids == candidate_ids[:5]
    elif sort == CANDIDATE_SORTING_TYPES.modified_desc:
        assert result_ids == list(reversed(candidate_ids))[:5]


@pytest.mark.parametrize('is_recruiter, can_see_notes', (
    pytest.param(True, True, id='recruiter'),
    pytest.param(False, False, id='hiring-manager'),
))
def test_filter_candidate_notes(client, is_recruiter, can_see_notes):
    """
    FEMIDA-6689: Добавление поля заметки, и количество заметок
    """
    profession = f.ProfessionFactory()
    candidate = f.CandidateFactory()
    f.CandidateProfessionFactory(profession=profession, candidate=candidate)

    if is_recruiter:
        user = f.create_recruiter()
    else:
        vacancy = f.create_vacancy()
        f.create_application(candidate=candidate, vacancy=vacancy)
        user = vacancy.hiring_manager

    for _ in range(5):
        f.MessageFactory(candidate=candidate, type=MESSAGE_TYPES.note)
        f.MessageFactory(candidate=candidate, type=MESSAGE_TYPES.incoming)
    last_note = f.MessageFactory(candidate=candidate, type=MESSAGE_TYPES.note)

    url = reverse('api:candidates:filter')
    filter_data = [
        {
            'field': 'ProfessionFilter',
            'condition': 'Any',
            'values': [profession.id],
        },
    ]
    params = {'filters': json.dumps(filter_data), 'page_size': 5}
    client.login(user.username)
    response = client.get(url, params)

    assert response.status_code == 200, response.content
    data = response.json()
    assert data['count'] == 1
    record = data['results'][0]
    notes = record.get('notes', [])
    assert len(notes) == (6 if can_see_notes else 0)
    if notes:
        assert notes[0]['id'] == last_note.id


@pytest.mark.parametrize('missed', (
    'field', 'condition', 'values',
))
def test_candidate_filter_with_missed_filter_params(su_client, missed):
    candidate_skill = f.CandidateSkillFactory()
    url = reverse('api:candidates:filter-form')
    filter_data = {
        'field': 'SkillFilter',
        'condition': 'Any',
        'values': [candidate_skill.skill.id],
    }
    filter_data.pop(missed)
    params = {'filters': json.dumps([filter_data])}
    response = su_client.get(url, params)
    assert response.status_code == 200, response.content


def test_candidate_filter_count(su_client):
    cp = f.CandidateProfessionFactory()
    url = reverse('api:candidates:filter-count')

    filter_data = [
        {
            'field': 'ProfessionFilter',
            'condition': 'Any',
            'values': [cp.profession.id],
        },
    ]
    params = {'filters': json.dumps(filter_data)}
    response = su_client.get(url, params)

    assert response.status_code == 200, response.content
    data = response.json()
    assert data == {'count': 1}


def test_candidate_scoring_filter_ordering(su_client):
    category = f.ScoringCategoryFactory()
    candidate_scoring1 = f.CandidateScoringFactory(scoring_value=0.5, scoring_category=category)
    candidate_scoring2 = f.CandidateScoringFactory(scoring_value=1, scoring_category=category)
    candidate_scoring3 = f.CandidateScoringFactory(scoring_value=0.7, scoring_category=category)
    expected_ordering = [
        candidate_scoring2.candidate_id,
        candidate_scoring3.candidate_id,
        candidate_scoring1.candidate_id,
    ]

    url = reverse('api:candidates:filter')

    filter_data = [
        {
            'field': 'CandidateScoringFilter',
            'condition': 'CandidateScoringCondition',
            'scoring_category': category.id,
        },
    ]
    params = {'filters': json.dumps(filter_data)}
    with override_config(ACTUAL_CANDIDATE_SCORING_VERSIONS=json.dumps({category.id: '1'})):
        response = su_client.get(url, params)

    assert response.status_code == 200, response.content
    data = response.json()

    assert expected_ordering == [item['id'] for item in data['results']]


# TODO: mock для ISearch
# def test_candidate_search(su_client):
#     f.create_heavy_candidate()
#     url = reverse('api:candidates:search')
#     response = su_client.get(url, {'text': 'text'})
#     assert response.status_code == 200


def test_candidate_search_form(su_client):
    url = reverse('api:candidates:search-form')
    response = su_client.get(url)
    assert response.status_code == 200


def _get_candidate_data():
    return {
        'first_name': 'First',
        'middle_name': 'Middle',
        'last_name': 'Last',
        'target_cities': [f.CityFactory.create().id],
        'responsibles': [f.create_recruiter().username],
        'contacts': [{
            'type': choices.CONTACT_TYPES.skype,
            'account_id': 'skype',
        }],
        'educations': [{
            'institution': 'Institution',
            'degree': choices.CANDIDATE_DEGREES.masters,
        }],
        'jobs': [{
            'employer': 'Employer',
            'start_date': '2017-01-01',
        }],
        'skills': [f.SkillFactory.create().id],
        'tags': ['tag1', 'tag2', 'tag3'],
        'candidate_professions': [{
            'profession': f.ProfessionFactory.create().id,
        }],
        'attachments': [f.AttachmentFactory.create().id],
    }


@pytest.mark.parametrize('enable_candidate_main_recruiter', (True, False))
def test_candidate_create(su_client, enable_candidate_main_recruiter):
    f.create_waffle_switch('enable_candidate_main_recruiter', enable_candidate_main_recruiter)
    data = _get_candidate_data()
    superuser = f.get_superuser()
    username = data['responsibles'][0]
    expected_responsibles = {
        superuser.username: CANDIDATE_RESPONSIBLE_ROLES.recruiter,
        username: CANDIDATE_RESPONSIBLE_ROLES.recruiter,
    }
    if enable_candidate_main_recruiter:
        data['recruiters'] = data.pop('responsibles')
        data['main_recruiter'] = superuser.username
        expected_responsibles[superuser.username] = CANDIDATE_RESPONSIBLE_ROLES.main_recruiter

    url = reverse('api:candidates:list')
    response = su_client.post(url, data)

    assert response.status_code == 201, response.content
    response_data = response.json()
    candidate = Candidate.unsafe.get(id=response_data['id'])
    assert candidate.first_name == data['first_name']
    assert candidate.last_name == data['last_name']
    assert {tag.name for tag in candidate.tags.all()} == set(data['tags'])
    assert {skill.id for skill in candidate.skills.all()} == set(data['skills'])
    assert expected_responsibles == dict(
        candidate.candidate_responsibles.values_list(
            'user__username',
            'role',
        )
    )


@pytest.mark.parametrize('enable_candidate_main_recruiter', (True, False))
def test_candidate_create_form(su_client, enable_candidate_main_recruiter):
    f.create_waffle_switch('enable_candidate_main_recruiter', enable_candidate_main_recruiter)
    user = f.get_superuser()

    url = reverse('api:candidates:create-form')
    response = su_client.get(url)

    assert response.status_code == 200, response.content
    response_data = response.json()['data']
    if enable_candidate_main_recruiter:
        assert 'recruiters' in response_data
        assert 'main_recruiter' in response_data
        assert response_data['main_recruiter']['value'] == user.username
    else:
        assert 'responsibles' in response_data
        assert 'main_recruiter' not in response_data
        assert 'recruiters' not in response_data


def test_candidate_recruiter_list(su_client):
    user = f.get_superuser()
    main_recruiter_role = CANDIDATE_RESPONSIBLE_ROLES.main_recruiter
    recruiter_role = CANDIDATE_RESPONSIBLE_ROLES.recruiter

    params = [(user, recruiter_role, CONSIDERATION_EXTENDED_STATUSES.in_progress)] * 3
    params += (
        (user, main_recruiter_role, CONSIDERATION_EXTENDED_STATUSES.in_progress),
        (user, recruiter_role, CONSIDERATION_EXTENDED_STATUSES.challenge_assigned),
        (f.create_recruiter(), main_recruiter_role, CONSIDERATION_EXTENDED_STATUSES.in_progress),
        (f.create_recruiter(), recruiter_role, CONSIDERATION_EXTENDED_STATUSES.in_progress),
        (f.create_recruiter(), recruiter_role, CONSIDERATION_EXTENDED_STATUSES.challenge_assigned),
    )

    candidates = []
    for responsible, role, extended_status in params:
        candidate = f.create_candidate_with_responsibles(**{role: responsible})
        f.ConsiderationFactory(
            candidate=candidate,
            state=CONSIDERATION_STATUSES.in_progress,
            extended_status=extended_status,
        )
        candidates.append(candidate)

    url = reverse('api:candidates:recruiter-list')
    data = {
        'stage': CONSIDERATION_RECRUITER_STAGES.no_assessment,
        'responsible_role': CANDIDATE_RESPONSIBLE_ROLES.recruiter,
        'for': user.username,
    }
    response = su_client.get(url, data)

    assert response.status_code == 200, response.content
    response_data = response.json()
    assert response_data['count'] == 3
    expected_candidate_ids = {candidate.id for candidate in candidates[:3]}
    assert expected_candidate_ids == {item['id'] for item in response_data['results']}


def test_candidate_recruiter_list_form(su_client):
    user = f.get_superuser()
    expected_count_by_responsible_role = {
        CANDIDATE_RESPONSIBLE_ROLES.recruiter: 2,
        CANDIDATE_RESPONSIBLE_ROLES.main_recruiter: 3,
    }
    for role, count in expected_count_by_responsible_role.items():
        for _ in range(count):
            candidate = f.create_candidate_with_responsibles(**{role: user})
            f.ConsiderationFactory(candidate=candidate, state=CONSIDERATION_STATUSES.in_progress)

    expected_count_by_responsible_role[''] = 2 + 3
    url = reverse('api:candidates:recruiter-list-filter-form')
    response = su_client.get(url)

    assert response.status_code == 200, response.content
    response_data = response.json()
    role_choices = response_data['structure']['responsible_role']['choices']
    count_by_responsible_role = {item['value']: item['count'] for item in role_choices}
    assert expected_count_by_responsible_role == count_by_responsible_role


@pytest.mark.parametrize('responsible_role', (
    '',
    CANDIDATE_RESPONSIBLE_ROLES.recruiter,
    CANDIDATE_RESPONSIBLE_ROLES.main_recruiter,
))
def test_candidate_recruiter_list_form_stage_counters(su_client, responsible_role):
    responsible_choices = CANDIDATE_RESPONSIBLE_ROLES
    stages_choices = CONSIDERATION_RECRUITER_STAGES
    count_by_role_and_stage = Counter({
        (responsible_choices.recruiter, stages_choices.no_assessment): 1,
        (responsible_choices.main_recruiter, stages_choices.assessment_assigned): 1,
        (responsible_choices.main_recruiter, stages_choices.no_assessment): 1,
        (responsible_choices.recruiter, stages_choices.assessment_finished): 1,
        ('', stages_choices.assessment_assigned): 1,
        ('', stages_choices.assessment_finished): 1,
        ('', stages_choices.no_assessment): 2,
        ('', ''): 4,
        (responsible_choices.recruiter, ''): 2,
        (responsible_choices.main_recruiter, ''): 2,
    })

    for (role, stage), count in count_by_role_and_stage.items():
        if not role or not stage:
            continue
        suitable_extended_status = CONSIDERATION_RECRUITER_STAGE_EXTENDED_STATUSES[stage][0]
        for _ in range(count):
            candidate = f.create_candidate_with_responsibles(**{role: f.get_superuser()})
            f.ConsiderationFactory(
                candidate=candidate,
                state=CONSIDERATION_STATUSES.in_progress,
                extended_status=suitable_extended_status,
            )

    url = reverse('api:candidates:recruiter-list-filter-form')
    data = {'responsible_role': responsible_role}
    response = su_client.get(url, data)

    assert response.status_code == 200, response.content
    response_data = response.json()
    stage_response_choices = response_data['structure']['stage']['choices']
    for stage in stage_response_choices:
        expected_count = count_by_role_and_stage[(responsible_role, stage['value'])]
        assert stage['count'] == expected_count


def test_check_for_duplicates(su_client):
    f.create_heavy_candidate()
    url = reverse('api:candidates:check-for-duplicates')
    data = {
        'first_name': 'First',
        'middle_name': 'Middle',
        'last_name': 'Last',
        'attachments': [f.AttachmentFactory.create().id],
        'contacts': [{
            'type': choices.CONTACT_TYPES.skype,
            'account_id': 'skype',
        }],
    }
    response = su_client.post(url, data)
    assert response.status_code == 200


def test_candidate_compare(su_client):
    candidate1 = f.create_heavy_candidate()
    candidate2 = f.create_heavy_candidate()
    main_recruiters = {
        candidate1.main_recruiter.username,
        candidate2.main_recruiter.username,
    }

    url = reverse('api:candidates:compare')
    data = {
        'candidates': '{},{}'.format(candidate1.id, candidate2.id),
    }
    response = su_client.get(url, data)
    assert response.status_code == 200, response.content
    response_data = response.json()
    assert 'main_recruiter' in response_data
    options = response_data['main_recruiter']['options']
    assert main_recruiters == {item['value']['username'] for item in options}


def test_candidate_merge(su_client):
    candidate1 = f.create_heavy_candidate()
    candidate2 = f.create_heavy_candidate()
    new_main_recruiter = candidate1.main_recruiter
    responsibles = set(
        CandidateResponsible.objects
        .filter(candidate__in=(candidate1.id, candidate2.id))
        .values_list('user__username', flat=True)
    )

    url = reverse('api:candidates:merge')
    data = {
        'main_recruiter': new_main_recruiter.username,
        'id': [candidate1.id, candidate2.id],
    }
    response = su_client.post(url, data)
    assert response.status_code == 200, response.content
    response_data = response.json()
    new_candidate = Candidate.unsafe.get(id=response_data['id'])
    assert new_candidate.main_recruiter
    assert new_main_recruiter == new_candidate.main_recruiter
    assert set(new_candidate.responsibles.values_list('username', flat=True)) == responsibles


def test_candidate_detail(su_client):
    candidate = f.create_heavy_candidate()
    application = f.create_application(
        candidate=candidate,
        consideration=candidate.considerations.first(),
    )
    f.VerificationFactory(
        candidate=candidate,
        application=application,
    )
    url = reverse('api:candidates:detail', kwargs={'pk': candidate.id})
    response = su_client.get(url)
    assert response.status_code == 200


@pytest.mark.parametrize(
    'vacancies_mailing_agreement, events_mailing_agreement', (
        (True, True),
        (False, False),
    ))
def test_candidate_mailing_agreements(su_client, vacancies_mailing_agreement,
                                      events_mailing_agreement):
    candidate = f.CandidateFactory(
        vacancies_mailing_agreement=vacancies_mailing_agreement,
        events_mailing_agreement=events_mailing_agreement,
    )
    url = reverse('api:candidates:detail', kwargs={'pk': candidate.id})
    response = su_client.get(url)
    response_data = response.json()

    assert 'vacancies_mailing_agreement' in response_data
    assert response_data['vacancies_mailing_agreement'] == vacancies_mailing_agreement
    assert 'events_mailing_agreement' in response_data
    assert response_data['events_mailing_agreement'] == events_mailing_agreement


@pytest.mark.parametrize('enable_candidate_main_recruiter', (True, False))
def test_candidate_update(su_client, enable_candidate_main_recruiter):
    f.create_waffle_switch('enable_candidate_main_recruiter', enable_candidate_main_recruiter)
    candidate = f.create_heavy_candidate()
    url = reverse('api:candidates:detail', kwargs={'pk': candidate.id})
    data = _get_candidate_data()
    if enable_candidate_main_recruiter:
        data['main_recruiter'] = f.create_recruiter().username

    data['ignore_duplicates'] = True
    response = su_client.put(url, data)
    assert response.status_code == 200, response.content
    if enable_candidate_main_recruiter:
        candidate.refresh_from_db()
        assert candidate.main_recruiter
        assert candidate.main_recruiter.username == data['main_recruiter']
        response_data = response.json()
        assert 'main_recruiter' in response_data
        assert response_data['main_recruiter']['username'] == data['main_recruiter']


def test_candidate_patch(su_client):
    candidate = f.create_heavy_candidate()
    tags = ['ml-developer', 'student']
    url = reverse('api:candidates:detail', kwargs={'pk': candidate.id})
    response = su_client.patch(url, {'tags': tags})
    assert response.status_code == 200

    result_tags = (
        candidate.candidate_tags
        .filter(is_active=True)
        .values_list('tag__name', flat=True)
    )
    assert set(result_tags) == set(tags)
    response_data = response.json()
    assert set(tags) == set(response_data['tags'])


@pytest.mark.parametrize('enable_candidate_main_recruiter', (True, False))
def test_candidate_update_form(su_client, enable_candidate_main_recruiter):
    f.create_waffle_switch('enable_candidate_main_recruiter', enable_candidate_main_recruiter)
    candidate = f.create_heavy_candidate()

    url = reverse('api:candidates:update-form', kwargs={'pk': candidate.id})
    response = su_client.get(url)

    assert response.status_code == 200
    response_data = response.json()['data']
    if enable_candidate_main_recruiter:
        assert 'main_recruiter' in response_data
        assert response_data['main_recruiter'].get('required')
        assert response_data['main_recruiter']['value'] == candidate.main_recruiter.username
        assert not response_data['recruiters'].get('required')
    else:
        assert 'main_recruiter' not in response_data
        assert 'responsibles' in response_data


def test_candidate_application_list(su_client):
    candidate = f.create_heavy_candidate()
    consideration = candidate.considerations.first()
    vacancy = f.create_active_vacancy()
    f.VacancyMembershipFactory.create(
        vacancy=vacancy,
        member=User.objects.get(username=settings.AUTH_TEST_USER),
        role=VACANCY_ROLES.head,
    )
    f.ApplicationFactory.create(
        candidate=candidate,
        consideration=consideration,
        vacancy=vacancy,
    )
    url = reverse('api:candidates:actual-application-list', kwargs={'pk': candidate.id})
    response = su_client.get(url)
    assert response.status_code == 200

    response_data = response.json()
    assert response_data.get('results')


def test_candidate_close(su_client):
    f.create_waffle_switch('enable_candidate_main_recruiter')
    f.create_waffle_switch('enable_new_candidate_close_form')
    candidate = f.create_heavy_candidate()
    consideration = candidate.considerations.first()
    f.ApplicationFactory(candidate=candidate, consideration=consideration)
    url = reverse('api:candidates:close', kwargs={'pk': candidate.id})
    data = {
        'consideration_resolution': CONSIDERATION_RESOLUTIONS.rejected_by_resume,
        'close_for_recruiter': False,
    }

    response = su_client.post(url, data)

    assert response.status_code == 200, response.content
    response_data = response.json()
    candidate.refresh_from_db()
    consideration.refresh_from_db()
    application = candidate.applications.first()
    assert candidate.status == choices.CANDIDATE_STATUSES.closed
    assert consideration.status == choices.CONSIDERATION_STATUSES.archived
    assert consideration.resolution == CONSIDERATION_RESOLUTIONS.rejected_by_resume
    assert application.status == APPLICATION_STATUSES.closed
    assert application.resolution == APPLICATION_RESOLUTIONS.consideration_archived
    assert not response_data['main_recruiter']


@pytest.mark.parametrize('is_rotation, consideration_resolution', (
    (True, CONSIDERATION_RESOLUTIONS.offer_rejected),
    (False, CONSIDERATION_RESOLUTIONS.offer_rejected),
))
@patch('intranet.femida.src.interviews.tasks.send_interview_survey_task.delay')
def test_candidate_close_nps_survey_sending_behavior(
    mocked_action: Mock, su_client, is_rotation, consideration_resolution
):
    f.create_waffle_switch('enable_new_candidate_close_form')
    candidate = f.CandidateFactory()
    consideration = f.ConsiderationFactory(
        candidate=candidate,
        is_rotation=is_rotation
    )
    f.ApplicationFactory(candidate=candidate, consideration=consideration)
    url = reverse('api:candidates:close', kwargs={'pk': candidate.id})
    data = {
        'consideration_resolution': consideration_resolution,
        'close_for_recruiter': False,
    }

    response = su_client.post(url, data)
    assert response.status_code == 200

    if is_rotation:
        mocked_action.assert_not_called()
    else:
        mocked_action.assert_called_once()


@pytest.mark.parametrize('first_role, second_role', (
    (CANDIDATE_RESPONSIBLE_ROLES.recruiter, CANDIDATE_RESPONSIBLE_ROLES.main_recruiter),
    (CANDIDATE_RESPONSIBLE_ROLES.main_recruiter, CANDIDATE_RESPONSIBLE_ROLES.recruiter),
))
def test_candidate_close_for_recruiter(client, first_role, second_role):
    f.create_waffle_switch('enable_new_candidate_close_form')
    first_recruiter = f.create_recruiter()
    second_recruiter = f.create_recruiter()
    candidate = f.CandidateFactory()
    consideration = f.ConsiderationFactory(candidate=candidate)
    first_application = f.create_heavy_application(
        candidate=candidate,
        consideration=consideration,
    )
    first_application.vacancy.add_membership(
        member=first_recruiter,
        role=VACANCY_ROLES.recruiter,
    )
    second_application = f.ApplicationFactory(
        candidate=candidate,
        consideration=consideration,
        status=APPLICATION_STATUSES.in_progress,
    )
    second_application.vacancy.add_membership(
        member=second_recruiter,
        role=VACANCY_ROLES.recruiter,
    )
    f.CandidateResponsibleFactory(candidate=candidate, user=first_recruiter, role=first_role)
    f.CandidateResponsibleFactory(candidate=candidate, user=second_recruiter, role=second_role)

    url = reverse('api:candidates:close', kwargs={'pk': candidate.id})
    data = {'close_for_recruiter': True}

    if first_role == CANDIDATE_RESPONSIBLE_ROLES.main_recruiter:
        data['main_recruiter'] = second_recruiter.username

    client.login(login=first_recruiter.username)
    response = client.post(url, data)

    assert response.status_code == 200, response.content
    candidate.refresh_from_db()
    first_application.refresh_from_db()
    second_application.refresh_from_db()
    assert candidate.main_recruiter == second_recruiter
    assert first_recruiter not in candidate.recruiters
    assert candidate.status == choices.CANDIDATE_STATUSES.in_progress
    assert first_application.status == APPLICATION_STATUSES.closed
    assert first_application.resolution == APPLICATION_RESOLUTIONS.consideration_archived
    assert second_application.status == APPLICATION_STATUSES.in_progress


@pytest.mark.parametrize('has_duplicate, forbidden_actions, status_code', (
    (False, '[]', 200),
    (True, '[]', 403),
    (False, '["candidate_close"]', 403),
))
def test_candidate_close_form(su_client, has_duplicate, forbidden_actions, status_code):
    f.create_waffle_switch('enable_has_duplicates_error')
    candidate = f.CandidateFactory(status=choices.CANDIDATE_STATUSES.in_progress)
    f.HireOrderFactory(candidate=candidate)
    if has_duplicate:
        f.DuplicationCaseFactory(first_candidate=candidate)

    url = reverse('api:candidates:close-form', kwargs={'pk': candidate.id})
    with override_config(AUTOHIRE_FORBIDDEN_ACTIONS=forbidden_actions):
        response = su_client.get(url)

    assert response.status_code == status_code


def test_candidate_close_for_recruiter_form(su_client):
    f.create_waffle_switch('enable_new_candidate_close_form')
    responsibles = (
        (f.get_superuser(), CANDIDATE_RESPONSIBLE_ROLES.main_recruiter),
        (f.UserFactory(), CANDIDATE_RESPONSIBLE_ROLES.recruiter),
    )
    candidate = f.CandidateFactory(status=choices.CANDIDATE_STATUSES.in_progress)
    for user, role in responsibles:
        f.CandidateResponsibleFactory(candidate=candidate, user=user, role=role)
    url = reverse('api:candidates:close-form', kwargs={'pk': candidate.id})

    response = su_client.get(url)
    assert response.status_code == 200, response.content
    response_data = response.json()
    assert 'close_for_recruiter' in response_data['structure']
    assert 'main_recruiter' in response_data['structure']


@pytest.mark.parametrize('enable_candidate_main_recruiter', (True, False))
def test_candidate_open(su_client, enable_candidate_main_recruiter):
    f.create_waffle_switch('enable_candidate_main_recruiter', enable_candidate_main_recruiter)
    candidate = f.CandidateFactory(status=choices.CANDIDATE_STATUSES.closed)
    f.CandidateResponsibleFactory(candidate=candidate, role=CANDIDATE_RESPONSIBLE_ROLES.recruiter)

    data = {
        'responsibles': [f.create_recruiter().username, f.create_recruiter().username],
        'source': SOURCES.candidates_base,
    }
    expected_responsibles = dict.fromkeys(
        data['responsibles'],
        choices.CANDIDATE_RESPONSIBLE_ROLES.recruiter,
    )
    if enable_candidate_main_recruiter:
        data['recruiters'] = data.pop('responsibles')
        data['main_recruiter'] = f.get_superuser().username
        expected_responsibles[f.get_superuser().username] = (
            choices.CANDIDATE_RESPONSIBLE_ROLES.main_recruiter
        )

    url = reverse('api:candidates:open', kwargs={'pk': candidate.id})
    response = su_client.post(url, data)

    assert response.status_code == 200, response.content
    candidate.refresh_from_db()
    assert candidate.status == choices.CANDIDATE_STATUSES.in_progress
    responsibles = dict(candidate.candidate_responsibles.values_list('user__username', 'role'))
    assert responsibles == expected_responsibles


@patch('intranet.femida.src.candidates.tasks.send_candidate_reference_event_task.delay')
def test_candidate_open_with_reference_issue(mocked_action, su_client):
    candidate = f.create_heavy_candidate(status=choices.CANDIDATE_STATUSES.closed)
    reference = f.ReferenceFactory(status=REFERENCE_STATUSES.approved)
    f.create_submission(candidate=candidate, reference=reference)
    url = reverse('api:candidates:open', kwargs={'pk': candidate.id})
    data = {
        'responsibles': [f.create_recruiter().username],
        'source': SOURCES.candidates_base,
    }
    with run_commit_hooks():
        su_client.post(url, data)
    mocked_action.assert_called_once_with(
        candidate_id=candidate.id,
        event='consideration_created',
        initiator_id=f.get_superuser().id,
    )


@patch('intranet.femida.src.notifications.add_comment', lambda *args, **kwargs: str())
def test_candidate_send_for_approval_by_startrek_key(su_client):
    consideration = f.create_completed_consideration()
    url = reverse('api:candidates:send-for-approval', kwargs={'pk': consideration.candidate.id})
    data = {
        'startrek_key': 'EMPTY-1',
    }
    response = su_client.post(url, data)
    assert response.status_code == 200


@patch('intranet.femida.src.notifications.add_comment', lambda *args, **kwargs: str())
def test_candidate_send_for_approval(su_client):
    f.create_waffle_switch('enable_candidate_approval_with_application')
    consideration = f.create_completed_consideration()

    applications = []
    for resolution in ('hire', 'hire', 'nohire'):
        application = f.create_application(
            candidate=consideration.candidate,
            consideration=consideration,
            status=APPLICATION_STATUSES.in_progress,
        )
        f.create_finished_final_interview(
            application=application,
            resolution=resolution,
        )
        applications.append(application)

    target_appl, hire_appl, nohire_appl = applications

    url = reverse('api:candidates:send-for-approval', kwargs={'pk': consideration.candidate.id})
    data = {
        'application': target_appl.id,
    }
    response = su_client.post(url, data)
    assert response.status_code == 200, response.content

    target_appl.refresh_from_db()
    assert target_appl.status == APPLICATION_STATUSES.in_progress

    hire_appl.refresh_from_db()
    assert hire_appl.status == APPLICATION_STATUSES.closed
    assert hire_appl.resolution == APPLICATION_RESOLUTIONS.offer_rejected

    nohire_appl.refresh_from_db()
    assert nohire_appl.status == APPLICATION_STATUSES.closed
    assert nohire_appl.resolution == APPLICATION_RESOLUTIONS.did_not_pass_assessments


def test_candidate_create_proposals(su_client):
    candidate = f.create_heavy_candidate()
    departments = [
        f.DepartmentFactory(tags=[]),
        f.DepartmentFactory(tags=[DEPARTMENT_TAGS.business_unit]),
        f.DepartmentFactory(tags=[DEPARTMENT_TAGS.experiment]),
    ]

    url = reverse('api:candidates:create-proposals', kwargs={'pk': candidate.id})
    data = {
        'skills': [f.SkillFactory.create().id],
        'pro_level_max': VACANCY_PRO_LEVELS.expert,
        'professions': [f.ProfessionFactory.create().id],
        'cities': [f.CityFactory.create().id],
        'responsibles': [f.create_recruiter().username],
        'departments': [departments[0].id, departments[2].id]
    }
    response = su_client.post(url, data)
    assert response.status_code == 200


def test_candidate_create_proposals_form(su_client):
    candidate = f.create_heavy_candidate()

    f.DepartmentFactory(tags=[]),
    f.DepartmentFactory(tags=[DEPARTMENT_TAGS.business_unit]),
    f.DepartmentFactory(tags=[DEPARTMENT_TAGS.experiment]),

    view_name = 'api:candidates:create-proposals-form'
    url = reverse(view_name, kwargs={'pk': candidate.id})
    response = su_client.get(url)
    assert response.status_code == 200

    depts_form_struct = response.json()['structure']['departments']
    assert depts_form_struct['type'] == 'modelmultiplechoice'
    assert len(depts_form_struct["choices"]) == 2


@pytest.mark.parametrize('workflow_data', (
    (choices.CANDIDATE_STATUSES.closed, 'open'),
    (choices.CANDIDATE_STATUSES.in_progress, 'create_proposals'),
    (choices.CANDIDATE_STATUSES.in_progress, 'send_for_approval'),
))
def test_candidate_action_form(su_client, workflow_data):
    from_status, action_name = workflow_data
    candidate = f.create_heavy_candidate(status=from_status)
    view_name = 'api:candidates:{}-form'.format(action_name.replace('_', '-'))
    url = reverse(view_name, kwargs={'pk': candidate.id})
    response = su_client.get(url)
    assert response.status_code == 200


def test_candidate_interview_round_create_form(su_client):
    candidate = f.create_heavy_candidate(
        first_name='Name',
        status=choices.CANDIDATE_STATUSES.in_progress,
    )
    f.MessageTemplateFactory(
        type=MESSAGE_TEMPLATE_TYPES.interview,
        text=(
            'Dear {{ candidate_name }},\n'
            'See you at {interview_datetime} in [УТОЧНИТЬ ПОДЪЕЗД!!!]'
        ),
    )
    template_with_simple_text = f.MessageTemplateFactory(type=MESSAGE_TEMPLATE_TYPES.interview)
    f.MessageTemplateFactory(
        type=MESSAGE_TEMPLATE_TYPES.signature,
        text=(
            '{{ user.get_full_name }}\n'
            '{{ user.get_full_name_en }}\n'
            '{{ user.work_phone }}\n'
            '{{ user.phone }}'
        )
    )
    f.MessageTemplateFactory(type=MESSAGE_TEMPLATE_TYPES.communication)  # неподходящий

    url = reverse('api:candidates:interview-round-create-form', kwargs={'pk': candidate.id})
    response = su_client.get(url)

    assert response.status_code == 200, response.content
    meta = response.json()['meta']

    assert len(meta['templates']) == 2
    assert meta['templates'][0]['text'] == (
        'Dear Name,\n'
        'See you at {interview_datetime} in [УТОЧНИТЬ ПОДЪЕЗД!!!]'
    )
    assert meta['templates'][1]['text'] == template_with_simple_text.text

    assert len(meta['signatures']) == 1
    su = f.get_superuser()
    assert meta['signatures'][0]['text'] == '\n'.join((
        su.get_full_name(),
        su.get_full_name_en(),
        str(su.work_phone),
        su.phone,
    ))


def test_candidate_close_irrelevant_applications_form(su_client):
    interview_types = (
        INTERVIEW_TYPES.screening,
        INTERVIEW_TYPES.aa,
        INTERVIEW_TYPES.regular,
        INTERVIEW_TYPES.final,
    )

    candidate = f.CandidateFactory()
    consideration = f.ConsiderationFactory(candidate=candidate)
    for _type in interview_types:
        f.create_interview(
            consideration=consideration,
            type=_type,
        )

    irrelevant_applications_count = 3
    f.ApplicationFactory.create_batch(
        irrelevant_applications_count,
        consideration=consideration,
        candidate=candidate,
    )

    url = reverse(
        viewname='api:candidates:close-irrelevant-applications-form',
        kwargs={'pk': candidate.id},
    )
    response = su_client.get(url)
    assert response.status_code == 200
    assert response.json()['meta']['irrelevant_applications_count'] == irrelevant_applications_count


def test_candidate_close_irrelevant_applications(su_client):
    candidate = f.CandidateFactory()
    consideration = f.ConsiderationFactory(candidate=candidate)
    f.create_interview(
        consideration=consideration,
        type=INTERVIEW_TYPES.regular,
    )
    f.ApplicationFactory(
        consideration=consideration,
        candidate=candidate,
    )

    url = reverse(
        viewname='api:candidates:close-irrelevant-applications',
        kwargs={'pk': candidate.id},
    )
    response = su_client.post(url)
    assert response.status_code == 200


@pytest.mark.parametrize('verification_type, grade, status_code', (
    (VERIFICATION_TYPES.default, None, 200),
    (VERIFICATION_TYPES.international_by_grade, 1, 200),
    (VERIFICATION_TYPES.international_by_grade, 'dsfj', 400),
    (VERIFICATION_TYPES.international_by_grade, -1, 400),
    (VERIFICATION_TYPES.international_by_grade, None, 400),
))
@patch('intranet.femida.src.notifications.candidates.send_email.delay')
def test_candidate_create_verification(
    send_email_mock,
    su_client,
    verification_type,
    grade,
    status_code,
):
    candidate = f.create_heavy_candidate()
    application = f.ApplicationFactory(
        consideration=candidate.considerations.first(),
        candidate=candidate,
    )

    url = reverse(
        viewname='api:candidates:verification-create',
        kwargs={'pk': candidate.id},
    )
    data = {
        'type': verification_type,
        'grade': grade,
        'receiver': '1@ya.ru',
        'subject': 'subject',
        'text': 'text',
        'application': application.id,
        'uuid': uuid.uuid4().hex,
    }

    response = su_client.post(url, data)
    assert response.status_code == status_code

    # Проверяем только если успешно приняли запрос на создание verification
    if status_code == 200:
        assert send_email_mock.called
        data = response.json()
        # экшн должен быть недоступен, т.к. в один момент времени у кандидата
        # может быть только одна активная проверка
        assert not data['candidate']['actions']['create_verification']


@pytest.mark.parametrize('has_duplicate, status_code', (
    (False, 200),
    (True, 403),
))
def test_candidate_create_verification_form(su_client, has_duplicate, status_code):
    f.create_waffle_switch('enable_has_duplicates_error')
    candidate = f.create_heavy_candidate()
    f.ApplicationFactory(
        consideration=candidate.considerations.first(),
        candidate=candidate,
    )
    if has_duplicate:
        f.DuplicationCaseFactory(first_candidate=candidate)

    url = reverse(
        viewname='api:candidates:verification-create-form',
        kwargs={'pk': candidate.id},
    )
    response = su_client.get(url)
    assert response.status_code == status_code


def test_candidate_partial_form_view(su_client):
    candidate = f.CandidateFactory()
    tag = f.CandidateTagFactory(candidate=candidate).tag
    fields = 'tags,skills'

    url = reverse(viewname='api:candidates:update-form', kwargs={'pk': candidate.id})
    response = su_client.get(url, {
        '_fields': fields,
    })

    assert response.status_code == 200
    response_data = response.json()
    for field in fields.split(','):
        assert field in response_data['data']
    assert len(response_data['structure']) == len(response_data['data']) == 2
    assert response_data['data']['tags']['value'][0] == tag.name


@pytest.fixture(scope='module')
def two_time_slots():
    day1 = shifted_now(days=1)
    day2 = shifted_now(days=3)
    return [
        {
            'start': day1.replace(hour=10).strftime('%Y-%m-%d %H:%M:%S'),
            'end': day1.replace(hour=20).strftime('%Y-%m-%d %H:%M:%S'),
        },
        {
            'start': day2.replace(hour=18, minute=0).strftime('%Y-%m-%d %H:%M:%S'),
            'end': day2.replace(hour=18, minute=59).strftime('%Y-%m-%d %H:%M:%S'),
        },
    ]


@pytest.mark.parametrize('round_type, is_any_time, is_code', (
    (INTERVIEW_ROUND_TYPES.screening, True, True),
    (INTERVIEW_ROUND_TYPES.final, False, False),
))
@patch('intranet.femida.src.candidates.workflow.save_interview_round_data_in_yt.delay')
def test_interview_round_create(mocked_save_interview_round_data_in_yt, su_client, two_time_slots,
                                round_type, is_any_time, is_code):
    application = f.ApplicationFactory()
    office = f.OfficeFactory(name_ru='Удалённо')
    url = reverse(
        viewname='api:candidates:interview-round-create',
        kwargs={'pk': application.candidate.id},
    )

    data = {
        'type': round_type,
        'is_any_time': is_any_time,
        'time_slots': two_time_slots,
        'office': office.id,
        'interviews': [
            {
                'name': 'Name',
                'is_code': is_code,
                'application': application.id,
                'potential_interviewers': [f.UserFactory().username, f.UserFactory().username],
                'preset': f.create_preset_with_problems().id,
            },
        ],
        'comment': 'Коммент для асессоров',
        'need_notify_candidate': True,
        'email': 'a@ya.ru',
        'subject': 'Subj',
        'text': 'Text with {interview_datetime} and [smth in brackents]',
        'attachments': [a.id for a in f.AttachmentFactory.create_batch(3)],
    }

    response = su_client.post(url, data)
    assert response.status_code == 200, response.content
    response_data = response.json()
    interview_round_id = response_data['id']
    mocked_save_interview_round_data_in_yt.assert_called_once_with(interview_round_id)

    interviews = list(Interview.unsafe.all().select_related('round__message'))
    assert len(interviews) == 1
    interview = interviews[0]
    assert interview.section == 'Name'
    assert interview.round_id == interview_round_id
    assert interview.round.time_slots.exists() != is_any_time
    assert interview.round.comment == data['comment']

    message = interview.round.message
    assert message.text == data['text']
    assert message.status == MESSAGE_STATUSES.draft


@pytest.fixture
def onsite_interview_round_data(two_time_slots):
    consideration = f.ConsiderationFactory()
    office = f.OfficeFactory(name_ru='Москва, Красная Роза')
    return {
        'candidate_id': consideration.candidate.id,
        'type': INTERVIEW_ROUND_TYPES.onsite,
        'is_strict_order': True,
        'lunch_duration': INTERVIEW_ROUND_LUNCH_DURATIONS.minutes_30,
        'office': office.id,
        'time_slots': two_time_slots,
        'interviews': [
            {
                'name': 'Name1',
                'is_code': True,
                'application': f.ApplicationFactory(
                    candidate=consideration.candidate,
                    consideration=consideration,
                ).id,
                'potential_interviewers': [f.UserFactory().username, f.UserFactory().username],
            },
            {
                'name': 'Name2',
                'is_code': True,
                'application': f.ApplicationFactory(
                    candidate=consideration.candidate,
                    consideration=consideration,
                ).id,
                'potential_interviewers': [f.UserFactory().username],
            },
            {
                'name': 'Name3',
                'is_code': True,
                'aa_type': AA_TYPES.canonical,
            },
        ],
        'need_notify_candidate': False,
    }


def test_interview_round_create_onsite(su_client, onsite_interview_round_data):
    data = onsite_interview_round_data
    url = reverse(
        viewname='api:candidates:interview-round-create',
        kwargs={'pk': data.pop('candidate_id')},
    )

    response = su_client.post(url, data)
    assert response.status_code == 200, response.content
    response_data = response.json()
    assert len(response_data['interviews']) == len(data['interviews'])

    interview_round = InterviewRound.objects.get(id=response_data['id'])
    assert interview_round.type == data['type']
    assert interview_round.is_strict_order == data['is_strict_order']
    assert interview_round.lunch_duration == INTERVIEW_ROUND_LUNCH_DURATIONS.minutes_30
    assert interview_round.office_id == data['office']


@pytest.mark.parametrize('ordering, is_strict_order', (
    (INTERVIEW_ROUND_ORDERINGS.any, False),
    (INTERVIEW_ROUND_ORDERINGS.strict, True),
    (INTERVIEW_ROUND_ORDERINGS.aa_last, False),
))
def test_interview_round_order(su_client, onsite_interview_round_data, ordering, is_strict_order):
    data = onsite_interview_round_data
    data.pop('is_strict_order')
    data['ordering'] = ordering

    url = reverse(
        viewname='api:candidates:interview-round-create',
        kwargs={'pk': data.pop('candidate_id')},
    )

    response = su_client.post(url, data)
    assert response.status_code == 200, response.content
    response_data = response.json()

    interview_round = InterviewRound.objects.get(id=response_data['id'])
    assert interview_round.is_strict_order == is_strict_order
    if ordering == INTERVIEW_ROUND_ORDERINGS.aa_last:
        assert '!!АА секцию последней!!' in interview_round.comment


@override_switch('enable_certification_new_template', active=True)
@override_config(CERTIFICATION_VACANCY_IDS='100500')
@patch('intranet.femida.src.notifications.candidates.send_email.delay')
@pytest.mark.parametrize('vacancy_id, status_code', (
    (100500, 200),
    (500100, 403),
))
def test_certification_create(mocked_send, su_client, vacancy_id, status_code):
    f.MessageTemplateFactory(
        type=MESSAGE_TEMPLATE_TYPES.certification,
        text='Ссылка: {{ instance.private_url }}',
    )
    candidate = f.CandidateFactory()
    consideration = f.ConsiderationFactory.create(candidate=candidate)
    f.InterviewFactory(
        candidate=candidate,
        consideration=consideration,
        application__vacancy__id=vacancy_id,
        state=INTERVIEW_STATES.finished,
    )

    url = reverse('api:candidates:certification-create', kwargs={'pk': candidate.id})
    data = {
        'receiver': 'test@ya.ru',
        'consideration': consideration.id,
    }
    response = su_client.post(url, data)

    assert response.status_code == status_code
    if status_code == 200:
        assert consideration.certification
        params = dict.fromkeys(('subject', 'reply_to', 'from_email', 'message_id'), ANY)
        mocked_send.assert_called_once_with(
            to=['test@ya.ru'],
            is_external=True,
            body=f'Ссылка: {consideration.certification.private_url}',
            **params,
        )


@override_config(CERTIFICATION_VACANCY_IDS='100500')
def test_certification_create_form(su_client):
    contact = f.CandidateContactFactory(type='email', account_id='test-email@yandex-team.ru')
    consideration = f.ConsiderationFactory.create(candidate=contact.candidate)
    f.InterviewFactory(
        candidate=consideration.candidate,
        consideration=consideration,
        application__vacancy__id=100500,
        state=INTERVIEW_STATES.finished,
    )
    url = reverse(
        'api:candidates:certification-create-form',
        kwargs={'pk': contact.candidate.id},
    )
    response = su_client.get(url)
    assert response.status_code == 200
    result = response.json()
    assert len(result['structure']['consideration']['choices']) == 2

    blank_choice = result['structure']['consideration']['choices'][0]
    assert blank_choice['value'] == ''
    assert blank_choice['label'] == '—'

    consideration_choice = result['structure']['consideration']['choices'][1]
    assert consideration_choice['value'] == consideration.id
    assert consideration_choice['label'] == str(consideration)

    assert result['data']['receiver']['value'] == contact.account_id


# TODO: удалить после релиза FEMIDA-7226
@patch('intranet.femida.src.forms_constructor.controllers.IssueTransitionOperation.delay')
def test_verification_old_flow_ok(mocked_transaction, su_client):
    verification = f.VerificationFactory(
        status=VERIFICATION_STATUSES.on_check,
        link_expiration_date=shifted_now(days=1),
        raw_data={
            'params': {
                'inn': '123456789012'
            }
        },
    )
    url = reverse(
        'private-api:forms-verification-success',
    )
    data = {'params':
                {"uuid": verification.uuid.hex,
                 "result": "comment",
                 'inn': "123456789012"
                 }}
    response = su_client.post(url, data)
    assert response.status_code == 200
    tags = {'зеленый'}
    assert set(mocked_transaction.call_args.kwargs['tags']['add']) == tags
    assert mocked_transaction.call_args.kwargs['transition'] == 'close'
    assert mocked_transaction.call_args.kwargs['resolution'] == 'fixed'
    assert mocked_transaction.call_args.kwargs['comment'] == 'comment'
    verification.refresh_from_db()
    assert verification.status == VERIFICATION_STATUSES.closed


# TODO: удалить после релиза FEMIDA-7226
@patch('intranet.femida.src.forms_constructor.controllers.IssueTransitionOperation.delay')
def test_verification_old_flow_not_ok(mocked_transaction, su_client):
    verification = f.VerificationFactory(
        status=VERIFICATION_STATUSES.on_check,
        link_expiration_date=shifted_now(days=1),
        raw_data={
            'params': {
                'inn': '123456789012'
            }
        },
    )
    url = reverse(
        'private-api:forms-verification-ess-check',
    )
    data = {'params':
                {"uuid": verification.uuid.hex,
                 "result": "comment",
                 'inn': "123456789012"
                 }}
    response = su_client.post(url, data)
    assert response.status_code == 200
    tags = {'желтый'}
    assert mocked_transaction.call_args.kwargs['transition'] == 'need_info'
    assert set(mocked_transaction.call_args.kwargs['tags']['add']) == tags
    assert mocked_transaction.call_args.kwargs['comment'] == 'comment'
    verification.refresh_from_db()
    assert verification.status == VERIFICATION_STATUSES.on_ess_check


@patch('intranet.femida.src.forms_constructor.controllers.IssueUpdateOperation.delay')
def test_verification_new_flow_ok(mocked_transaction, su_client):
    f.create_waffle_switch(TemporarySwitch.NEW_VERIFICATION_FLOW, True)
    verification = f.VerificationFactory(
        status=VERIFICATION_STATUSES.on_check,
        link_expiration_date=shifted_now(days=1),
        raw_data={
            'params': {
                'inn': '123456789012'
            }
        },
    )
    url = reverse(
        'private-api:forms-verification-success',
    )
    data = {'params':
                {"uuid": verification.uuid.hex,
                 "result": "comment",
                 'inn': "123456789012"
                 }}
    response = su_client.post(url, data)
    assert response.status_code == 200
    tags = {'IRIS-vendor-green'}
    assert set(mocked_transaction.call_args.kwargs['tags']['add']) == tags
    assert mocked_transaction.call_args.kwargs['comment'] == 'comment'
    verification.refresh_from_db()
    assert verification.status == VERIFICATION_STATUSES.on_ess_check


@patch('intranet.femida.src.forms_constructor.controllers.IssueUpdateOperation.delay')
def test_verification_new_flow_not_ok(mocked_transaction, su_client):
    f.create_waffle_switch(TemporarySwitch.NEW_VERIFICATION_FLOW, True)
    verification = f.VerificationFactory(
        status=VERIFICATION_STATUSES.on_check,
        link_expiration_date=shifted_now(days=1),
        raw_data={
            'params': {
                'inn': '123456789012'
            }
        },
    )
    url = reverse(
        'private-api:forms-verification-ess-check',
    )
    data = {'params':
                {"uuid": verification.uuid.hex,
                 "result": "comment",
                 'inn': "123456789012"
                 }}
    response = su_client.post(url, data)
    assert response.status_code == 200
    tags = {'IRIS-vendor-yellow', 'желтый'}
    assert set(mocked_transaction.call_args.kwargs['tags']['add']) == tags
    assert mocked_transaction.call_args.kwargs['comment'] == 'comment'
    verification.refresh_from_db()
    assert verification.status == VERIFICATION_STATUSES.on_ess_check


def assert_lists_are_equal(list1, list2):
    def assert_compare(wrap_list1, wrap_list2):
        for item in wrap_list1:
            assert item in wrap_list2

    assert_compare(list1, list2)
    assert_compare(list2, list1)


def prepare_costs_data():
    for code in ['RUB', 'USD', 'EUR']:
        f.currency(code=code)
    return {
        'expectation': {
            'total': {
                'value': 10000, 'currency': 'RUB',
                'rate': 'monthly', 'taxed': True
            },
            'salary': {
                'value': 20000, 'currency': 'USD',
                'rate': 'monthly', 'taxed': True
            },
            'salary_bonus': {
                'value': 30000, 'currency': 'EUR',
                'rate': 'monthly', 'taxed': True
            },
            'rsu': {
                'value': 40000,
                'currency': None,
                'taxed': True,
            },
            'comment': {
                'comment': '50000',
            }
        },
        'current': {
            'total': {
                'value': 1000, 'currency': 'RUB',
                'rate': 'monthly', 'taxed': True
            },
            'salary': {
                'value': 2000, 'currency': 'USD',
                'rate': 'monthly', 'taxed': True
            },
            'salary_bonus': {
                'value': 3000, 'currency': 'EUR',
                'rate': 'monthly', 'taxed': True
            },
            'rsu': {
                'value': 4000,
                'currency': None,
                'taxed': False,
            },
            'comment': {
                'comment': '5000',
            }
        }
    }


@pytest.mark.parametrize('is_history, index', [
    (True, 1),
    (False, 0),
])
def test_get_candidate_costs(su_client, is_history, index):
    data = prepare_costs_data()
    candidate = f.CandidateFactory()
    all_costs = f.CandidateCostsSetFactory(candidate=candidate)
    if is_history:
        f.CandidateCostsSetFactory(candidate=candidate)
    for cost_group, cost in data.items():
        for _type, cost_data in cost.items():
            currency_code = cost_data.get('currency', None)
            currency = f.currency(code=currency_code) if currency_code else None
            f.CandidateCostFactory(
                candidate_costs_set=all_costs,
                cost_group=cost_group,
                type=_type,
                **{**cost_data, 'currency': currency},
            )
    url = reverse('api:candidates:costs', kwargs={'pk': candidate.id})
    response = su_client.get(url, {})
    body = response.json()
    cost = body['costs'][index]
    cost.pop('author', None)
    cost.pop('created', None)
    cost.pop('modified', None)
    assert cost == data


def test_candidate_update_cost_set(su_client):
    data = prepare_costs_data()
    candidate = f.CandidateFactory()
    url = reverse('api:candidates:costs', kwargs={'pk': candidate.id})
    response = su_client.post(url, data)
    assert response.status_code == 201
    assert candidate.candidate_costs_set.exists()
    assert candidate.candidate_costs_set.count() == 1


def test_candidate_update_twice_cost_set(su_client):
    data = prepare_costs_data()
    candidate = f.CandidateFactory()
    url = reverse('api:candidates:costs', kwargs={'pk': candidate.id})
    su_client.post(url, data)
    response = su_client.post(url, data)
    assert response.status_code == 201
    assert candidate.candidate_costs_set.count() == 2


def test_candidate_edit_cost_set(su_client):
    data = prepare_costs_data()
    candidate = f.CandidateFactory()
    new_data = {'expectation': {'rsu': {'value': 99999}}}
    url = reverse('api:candidates:costs', kwargs={'pk': candidate.id})
    su_client.post(url, data)
    modified = candidate.candidate_costs_set.last().modified
    response = su_client.put(url, new_data)
    assert response.status_code == 200
    assert candidate.candidate_costs_set.last().costs.count() == 1
    assert candidate.candidate_costs_set.last().modified > modified


def test_candidate_non_recruiter_cant_access_cost(client):
    user = f.create_user()
    candidate = f.CandidateFactory()
    client.login(user.username)
    url = reverse('api:candidates:costs', kwargs={'pk': candidate.id})
    response = client.get(url, {})
    assert response.status_code == 403


def test_candidate_cant_edit_not_existing_cost(su_client):
    data = prepare_costs_data()
    candidate = f.CandidateFactory()
    url = reverse('api:candidates:costs', kwargs={'pk': candidate.id})
    response = su_client.put(url, data)
    assert response.status_code == 404


def test_candidate_recruiter_cant_edit_not_own_existing_cost(client):
    data = prepare_costs_data()
    candidate = f.CandidateFactory()
    url = reverse('api:candidates:costs', kwargs={'pk': candidate.id})
    user1 = f.create_user_with_perm('recruiter_perm')
    user2 = f.create_user_with_perm('recruiter_perm')
    client.login(user1.username)
    client.post(url, data)
    client.login(user2.username)
    response = client.put(url, data)
    assert response.status_code == 403


def test_candidate_recruiter_can_edit_own_existing_cost(client):
    data = prepare_costs_data()
    candidate = f.CandidateFactory()
    url = reverse('api:candidates:costs', kwargs={'pk': candidate.id})
    user = f.create_user_with_perm('recruiter_perm')
    client.login(user.username)
    client.post(url, data)
    response1 = client.get(url, {})
    assert client.put(url, data).status_code == 200
    response2 = client.get(url, {})
    assert response1.json()['costs'][0]['modified'] != response2.json()['costs'][0]['modified']
