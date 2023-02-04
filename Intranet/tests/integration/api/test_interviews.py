import pytest

from unittest.mock import patch

from constance.test import override_config
from django.urls.base import reverse

from intranet.femida.src.interviews.choices import (
    INTERVIEW_TYPES,
    INTERVIEW_RESOLUTIONS,
    AA_TYPES,
    INTERVIEW_SCALES,
)
from intranet.femida.src.interviews.models import Interview
from intranet.femida.src.offers.choices import OFFER_STATUSES

from intranet.femida.tests import factories as f
from intranet.femida.tests.utils import get_mocked_event, fake_create_aarev_issue, Contains

pytestmark = pytest.mark.django_db


def test_interview_list(su_client):
    f.create_interview()
    url = reverse('api:interviews:list')
    response = su_client.get(url)
    assert response.status_code == 200


@pytest.mark.parametrize('data', (
    {   # 0. Вообще не фильтруем – находим
        'found': 1,
    },
    {   # 1. Фильтруем по всем параметрам – находим
        'types': [INTERVIEW_TYPES.regular],
        'state': Interview.STATES.estimated,
        'grades': [3],
        'resolution': INTERVIEW_RESOLUTIONS.hire,
        'found': 1,
    },
    {   # 2. Фильтруем по части параметров – находим
        'types': [INTERVIEW_TYPES.regular],
        'grades': [1, 2, 3],
        'found': 1,
    },
    {   # 3. Фильтруем с другим статусом – не находим
        'types': [INTERVIEW_TYPES.regular],
        'state': Interview.STATES.finished,
        'grades': [3],
        'resolution': INTERVIEW_RESOLUTIONS.hire,
        'found': 0,
    },
))
def test_interview_list_filter(client, data):
    interviewer = f.create_user()
    f.create_interview(
        interviewer=interviewer,
        type=INTERVIEW_TYPES.regular,
        state=Interview.STATES.estimated,
        grade=3,
    )
    found = data.pop('found')
    url = reverse('api:interviews:list')
    client.login(interviewer.username)
    response = client.get(url, data)
    assert response.status_code == 200
    response_data = response.json()
    assert response_data['count'] == found


@pytest.mark.parametrize('has_duplicate, enable_has_duplicates_error, status_code', (
    (True, False, 200),
    (False, True, 200),
    (True, True, 403),
))
def test_interview_create_form(su_client, has_duplicate, enable_has_duplicates_error, status_code):
    f.create_waffle_switch('enable_has_duplicates_error', enable_has_duplicates_error)
    application = f.ApplicationFactory.create()
    if has_duplicate:
        f.DuplicationCaseFactory(first_candidate=application.candidate)
    url = reverse('api:interviews:create-form')
    params = {
        'candidate': application.candidate_id,
    }
    response = su_client.get(url, params)
    assert response.status_code == status_code


@pytest.mark.parametrize('forbidden_actions, status_code', (
    ('[]', 200),
    ('["interview_create"]', 403),
))
def test_interview_create_form_is_forbidden_for_autohire(su_client, forbidden_actions, status_code):
    application = f.ApplicationFactory.create()
    f.HireOrderFactory(
        candidate=application.candidate,
        application=application,
    )

    url = reverse('api:interviews:create-form')
    with override_config(AUTOHIRE_FORBIDDEN_ACTIONS=forbidden_actions):
        response = su_client.get(url, {'candidate': application.candidate_id})

    assert response.status_code == status_code
    if status_code == 403:
        assert response.json()['error'][0]['code'] == 'forbidden_for_autohire'


@pytest.mark.parametrize('offer_status, has_duplicate, status_code', (
    (None, False, 201),
    (None, True, 403),
    (OFFER_STATUSES.on_approval, False, 201),
    (OFFER_STATUSES.sent, False, 403),
    (OFFER_STATUSES.accepted, False, 403),
))
@patch('intranet.femida.src.interviews.controllers.update_event', lambda *args, **kwargs: None)
@patch('intranet.femida.src.api.interviews.forms.get_event', get_mocked_event)
def test_interview_create(su_client, offer_status, has_duplicate, status_code):
    f.create_waffle_switch('enable_has_duplicates_error')
    application = f.ApplicationFactory.create()
    if offer_status:
        f.OfferFactory(status=offer_status, candidate=application.candidate)
    if has_duplicate:
        f.DuplicationCaseFactory(first_candidate=application.candidate)
    url = reverse('api:interviews:list')
    data = {
        'section': 'Interview',
        'interviewer': f.UserFactory.create().username,
        'type': INTERVIEW_TYPES.regular,
        'application': application.id,
        'candidate': application.candidate_id,
        'event_url': 'https://calendar.testing.yandex-team.ru/?event_id=1',
    }
    response = su_client.post(url, data)
    assert response.status_code == status_code


def test_interview_filter_form(su_client):
    url = reverse('api:interviews:filter-form')
    response = su_client.get(url)
    assert response.status_code == 200


def test_interview_achievement(client):
    interviewer = f.create_user()
    f.create_interview(
        interviewer=interviewer,
        type=INTERVIEW_TYPES.regular,
        state=Interview.STATES.estimated,
        grade=3,
    )
    f.create_interview(
        interviewer=interviewer,
        type=INTERVIEW_TYPES.regular,
        state=Interview.STATES.finished,
        grade=3,
    )
    f.create_interview(
        interviewer=interviewer,
        type=INTERVIEW_TYPES.final,
        state=Interview.STATES.finished,
    )
    url = reverse('api:interviews:achievements')
    client.login(interviewer.username)
    response = client.get(url)
    assert response.status_code == 200
    response_data = response.json()
    assert response_data['achievement_interview_count'] == 1


def test_interview_report(su_client):
    f.create_interview()
    url = reverse('api:interviews:report')
    response = su_client.get(url)
    assert response.status_code == 200


def test_interview_detail(su_client):
    interview = f.create_interview()
    url = reverse('api:interviews:detail', kwargs={'pk': interview.id})
    response = su_client.get(url)
    assert response.status_code == 200


@patch('intranet.femida.src.interviews.controllers.update_event', lambda *args, **kwargs: None)
@patch('intranet.femida.src.api.interviews.forms.get_event', get_mocked_event)
def test_interview_update(su_client):
    interview = f.create_interview()
    url = reverse('api:interviews:detail', kwargs={'pk': interview.id})
    data = {
        'application': interview.application_id,
        'candidate': interview.candidate_id,
        'section': 'Updated interview',
        'event_url': 'https://calendar.testing.yandex-team.ru/?event_id=1',
        'interviewer': interview.interviewer.username,
    }
    response = su_client.put(url, data)
    assert response.status_code == 200


def test_interview_partial_update(su_client):
    interview = f.create_interview()
    url = reverse('api:interviews:detail', kwargs={'pk': interview.id})
    data = {
        'section': 'Updated interview',
    }
    response = su_client.patch(url, data)
    assert response.status_code == 200


def test_interview_update_form(su_client):
    interview = f.create_interview()
    url = reverse('api:interviews:update-form', kwargs={'pk': interview.id})
    response = su_client.get(url)
    assert response.status_code == 200


@patch('intranet.femida.src.api.interviews.views.get_event', get_mocked_event)
def test_interview_event(su_client):
    interview = f.create_interview(event_id=1)
    url = reverse('api:interviews:event', kwargs={'pk': interview.id})
    response = su_client.get(url)
    assert response.status_code == 200


@pytest.mark.parametrize('interview_status, action_name, data', (
    (Interview.STATES.assigned, 'cancel', {}),
    (Interview.STATES.assigned, 'estimate', {'grade': 3}),
    (Interview.STATES.assigned, 'comment', {'comment': 'comment'}),
    (Interview.STATES.assigned, 'change_resolution', {'resolution': INTERVIEW_RESOLUTIONS.hire}),
    (Interview.STATES.assigned, 'change_grade', {'yandex_grade': 14}),
    (Interview.STATES.assigned, 'choose_scale', {'scale': INTERVIEW_SCALES.new}),
))
def test_interview_action(su_client, interview_status, action_name, data):
    interview = f.create_interview(state=interview_status)
    view_name = 'api:interviews:{}'.format(action_name.replace('_', '-'))
    url = reverse(view_name, kwargs={'pk': interview.id})
    response = su_client.post(url, data)

    assert response.status_code == 200
    response_data = response.json()
    assert all(response_data.get(field) == data[field] for field in data)


@patch('intranet.femida.src.interviews.workflow.replace_event_attendee_task.delay')
def test_interview_reassign(mocked_replace_event_attendee_task, su_client):
    event_id = 1
    interview = f.create_interview(event_id=event_id)
    url = reverse('api:interviews:reassign', kwargs={'pk': interview.id})
    old_interviewer = interview.interviewer
    new_interviewer = f.UserFactory()

    data = {'interviewer': new_interviewer.username}
    response = su_client.post(url, data)

    assert response.status_code == 200, response.content
    interview.refresh_from_db()
    assert interview.interviewer == new_interviewer
    mocked_replace_event_attendee_task.assert_called_once_with(
        event_id=event_id,
        old_attendee_email=old_interviewer.email,
        new_attendee_email=new_interviewer.email,
    )


@pytest.mark.parametrize('interview_status', Interview.STATES._db_values)
def test_interview_rename(su_client, interview_status):
    interview = f.create_interview(state=interview_status)
    url = reverse('api:interviews:rename', kwargs={'pk': interview.id})
    data = {
        'section': 'new_name',
    }
    response = su_client.post(url, data)
    assert response.status_code == 200
    assert response.json()['section'] == 'new_name'


@pytest.mark.parametrize('grade, yandex_grade, comment, status_code, error', (
    (None, 14, 'comment', 400, 'missing_grade'),
    (2, None, 'comment', 400, 'missing_yandex_grade'),
    (2, 14, '', 400, 'empty_interview_comment'),
    (2, 14, 'comment', 200, None),
    (0, 0, 'comment', 200, None),
))
def test_interview_finish_gradable(su_client, grade, yandex_grade, comment, status_code, error):
    f.create_waffle_switch('require_interview_yandex_grade')
    interview = f.create_interview(
        type=INTERVIEW_TYPES.regular,
        state=Interview.STATES.estimated,
        grade=grade,
        yandex_grade=yandex_grade,
        comment=comment,
    )
    url = reverse('api:interviews:finish', kwargs={'pk': interview.id})
    response = su_client.post(url)
    assert response.status_code == status_code
    if status_code != 200:
        assert response.json()['errors'][''][0]['code'] == error


@pytest.mark.parametrize('resolution, comment, status_code, error', (
    ('', 'comment', 400, 'missing_resolution'),
    (INTERVIEW_RESOLUTIONS.hire, '', 400, 'empty_interview_comment'),
    (INTERVIEW_RESOLUTIONS.nohire, 'comment', 200, None),
))
def test_interview_finish_non_gradable(su_client, resolution, comment, status_code, error):
    f.create_waffle_switch('require_interview_yandex_grade')
    interview = f.create_interview(
        type=INTERVIEW_TYPES.final,
        state=Interview.STATES.assigned,
        resolution=resolution,
        comment=comment,
    )
    url = reverse('api:interviews:finish', kwargs={'pk': interview.id})
    response = su_client.post(url)
    assert response.status_code == status_code
    if status_code != 200:
        assert response.json()['errors'][''][0]['code'] == error


def test_interview_add_problems_from_preset(su_client):
    interview = f.create_interview(
        state=Interview.STATES.estimated,
        grade=3,
        comment='comment',
    )
    url = reverse('api:interviews:add-problems-from-preset', kwargs={'pk': interview.id})
    data = {
        'preset': f.create_preset_with_problems().id,
    }
    response = su_client.post(url, data)
    assert response.status_code == 200


@pytest.mark.parametrize('interview_status, action_name', (
    # По одному статусу на экшен. Все разрешенные статусы проверяем в тестах на экшены
    (Interview.STATES.assigned, 'reassign'),
    (Interview.STATES.assigned, 'change_resolution'),
    (Interview.STATES.assigned, 'rename'),
    (Interview.STATES.assigned, 'change_grade'),
    (Interview.STATES.assigned, 'choose_scale'),
))
def test_interview_action_form(su_client, interview_status, action_name):
    interview = f.create_interview(state=interview_status)
    view_name = 'api:interviews:{}-form'.format(action_name.replace('_', '-'))
    url = reverse(view_name, kwargs={'pk': interview.id})
    response = su_client.get(url)
    assert response.status_code == 200


@patch('intranet.femida.src.interviews.startrek.issues.create_issue', fake_create_aarev_issue)
@patch('intranet.femida.src.interviews.startrek.issues.inflect_fio', lambda *x, **y: 'First Last')
def test_interview_send_to_review(su_client):
    interview = f.create_interview(
        state=Interview.STATES.finished,
        # Note: все типы проверяем в units/interviews/test_permissions.py
        type=INTERVIEW_TYPES.aa,
        aa_type=AA_TYPES.canonical,
        grade=3,
        comment='comment',
    )
    url = reverse('api:interviews:send-to-review', kwargs={'pk': interview.id})
    response = su_client.post(url, {'comment': 'Поревьюйте это, плз'})
    assert response.status_code == 200, response.content
    response_data = response.json()
    assert 'startrek_review_key' in response_data


def test_interview_send_to_review_form(su_client):
    interview = f.create_interview()
    url = reverse('api:interviews:send-to-review-form', kwargs={'pk': interview.id})
    response = su_client.get(url)
    assert response.status_code == 200, response.content
    response_data = response.json()
    assert 'comment' in response_data['structure']
    assert response_data['structure']['comment'].get('required')


@patch('intranet.femida.src.interviews.startrek.issues.IssueUpdateOperation')
def test_interview_with_existing_review_send_to_review(mocked_operation, su_client):
    vacancy = f.create_vacancy()
    application = f.create_application(vacancy=vacancy)
    interview = f.create_interview(
        state=Interview.STATES.finished,
        type=INTERVIEW_TYPES.regular,
        startrek_review_key='INTREV-1',
        application=application,
    )
    user = f.get_superuser()
    url = reverse('api:interviews:send-to-review', kwargs={'pk': interview.id})
    comment = 'Была зачтена некорректно решённая задача'
    data = {'comment': comment}

    with override_config(INTERVIEW_REVIEW_PROF_SPHERE_IDS=str(vacancy.professional_sphere_id)):
        response = su_client.post(url, data)

    assert response.status_code == 200, response.content

    mocked_operation.assert_called_once_with(interview.startrek_review_key)
    mocked_operation().assert_called_with(
        comment=Contains(comment),
        followers={
            'add': [user.username],
        },
    )
