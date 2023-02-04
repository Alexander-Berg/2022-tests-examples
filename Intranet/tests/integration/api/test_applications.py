import pytest

from constance.test import override_config
from django.urls.base import reverse

from intranet.femida.src.candidates.choices import CONSIDERATION_EXTENDED_STATUSES
from intranet.femida.src.interviews.choices import (
    APPLICATION_STATUSES,
    IN_PROGRESS_APPLICATION_RESOLUTIONS,
    CLOSED_APPLICATION_RESOLUTIONS,
    APPLICATION_SOURCES,
    APPLICATION_PROPOSAL_STATUSES,
)
from intranet.femida.src.interviews.models import Interview
from intranet.femida.src.offers.choices import SOURCES
from intranet.femida.src.offers.models import Offer
from intranet.femida.src.staff.choices import GEOGRAPHY_KINDS
from intranet.femida.src.vacancies.choices import VACANCY_ROLES, VACANCY_TYPES

from intranet.femida.tests import factories as f


pytestmark = pytest.mark.django_db


@pytest.mark.parametrize('related_field_name', (
    'consideration_id',
    'vacancy_id',
))
def test_list_applications(su_client, related_field_name):
    application = f.ApplicationFactory.create()
    url = reverse('api:applications:list')
    data = {
        related_field_name: getattr(application, related_field_name)
    }
    response = su_client.get(url, data=data)
    assert response.status_code == 200


def test_list_consideration_applications(su_client):
    application = f.ApplicationFactory.create()
    url = reverse('api:applications:list')
    data = {
        'consideration': application.consideration.id,
    }
    response = su_client.get(url, data=data)
    assert response.status_code == 200


def test_application_filter_form(su_client):
    url = reverse('api:applications:filter-form')
    response = su_client.get(url)
    assert response.status_code == 200


@pytest.mark.parametrize('has_duplicate, status_code', (
    (False, 201),
    (True, 403),
))
def test_bulk_create_application(su_client, has_duplicate, status_code):
    f.create_waffle_switch('enable_has_duplicates_error')
    candidate = f.CandidateFactory()
    vacancies = f.VacancyFactory.create_batch(3)
    if has_duplicate:
        f.DuplicationCaseFactory(first_candidate=candidate)

    # Добавляем одного пользователя на 2 вакансии,
    # чтобы стригерить балковую нотификацию
    recruiter = f.create_recruiter()
    for vacancy in vacancies[:2]:
        f.VacancyMembershipFactory(
            vacancy=vacancy,
            member=recruiter,
            role=VACANCY_ROLES.recruiter,
        )

    interview = f.create_interview(
        candidate=candidate,
        state=Interview.STATES.finished,
    )

    url = reverse('api:applications:bulk-create')
    data = {
        'candidate': candidate.id,
        'vacancies': [vacancy.id for vacancy in vacancies],
        'source': SOURCES.candidates_base,
        'interviews': [interview.id],
    }
    response = su_client.post(url, data)
    assert response.status_code == status_code


@pytest.mark.parametrize('has_duplicate, is_recruiter, status_code', (
    (False, True, 200),
    (True, False, 200),
    (True, True, 403),
))
def test_bulk_create_application_form(client, has_duplicate, is_recruiter, status_code):
    f.create_waffle_switch('enable_has_duplicates_error')
    user = f.create_recruiter() if is_recruiter else f.create_user()
    interview = f.create_interview(interviewer=user)
    candidate = interview.candidate
    if has_duplicate:
        f.DuplicationCaseFactory(first_candidate=candidate)

    url = reverse('api:applications:bulk-create-form')
    params = {
        'candidate': candidate.id,
    }
    client.login(user.username)
    response = client.get(url, params)
    assert response.status_code == status_code


@pytest.mark.parametrize('view_name', ('hiring-team-list', 'hiring-team-list-filter-form'))
def test_applications_hiring_list(su_client, view_name):
    vacancy = f.VacancyFactory()
    f.VacancyMembershipFactory.create(
        vacancy=vacancy,
        member=f.get_superuser(),
        role=VACANCY_ROLES.head,
    )

    f.ApplicationFactory.create(
        vacancy=vacancy,
        proposal_status=APPLICATION_PROPOSAL_STATUSES.accepted,
        resolution=IN_PROGRESS_APPLICATION_RESOLUTIONS.invited_to_preliminary_interview,
    )
    a = f.ApplicationFactory.create(
        vacancy=vacancy,
        status=APPLICATION_STATUSES.in_progress,
        resolution=IN_PROGRESS_APPLICATION_RESOLUTIONS.offer_agreement,
    )
    a.consideration.extended_status = CONSIDERATION_EXTENDED_STATUSES.final_assigned
    a.consideration.save()

    url = reverse('api:applications:' + view_name)
    response = su_client.get(url)
    assert response.status_code == 200


actions_workflow_data = [
    (APPLICATION_STATUSES.draft, 'activate', {
        'resolution': IN_PROGRESS_APPLICATION_RESOLUTIONS.test_task_sent,
    }),
    (APPLICATION_STATUSES.draft, 'close', {
        'resolution': CLOSED_APPLICATION_RESOLUTIONS.did_not_pass_assessments,
    }),
    (APPLICATION_STATUSES.closed, 'reopen', {}),
    (APPLICATION_STATUSES.closed, 'change_resolution', {
        'resolution': CLOSED_APPLICATION_RESOLUTIONS.incorrect,
    }),
    (APPLICATION_STATUSES.in_progress, 'accept_proposal', {}),
    (APPLICATION_STATUSES.in_progress, 'reject_proposal', {}),
]


@pytest.mark.parametrize('workflow_data', actions_workflow_data)
def test_application_action(su_client, workflow_data):
    from_status, action_name, data = workflow_data
    data['comment'] = 'test'
    application = f.create_application(status=from_status)

    if action_name in ('accept_proposal', 'reject_proposal'):
        application.source = APPLICATION_SOURCES.proposal
        application.save()

    view_name = 'api:applications:{}'.format(action_name.replace('_', '-'))
    url = reverse(view_name, kwargs={'pk': application.id})
    response = su_client.post(url, data)
    assert response.status_code == 200


@pytest.mark.parametrize('forbidden_actions, status_code', (
    ('[]', 200),
    ('["application_close"]', 403),
))
def test_application_close_is_forbidden_for_autohire(su_client, forbidden_actions, status_code):
    data = {
        'resolution': CLOSED_APPLICATION_RESOLUTIONS.refused_us,
        'comment': 'test',
    }
    application = f.ApplicationFactory(
        status=APPLICATION_STATUSES.in_progress,
        vacancy__type=VACANCY_TYPES.autohire,
    )
    url = reverse('api:applications:close', kwargs={'pk': application.id})
    with override_config(AUTOHIRE_FORBIDDEN_ACTIONS=forbidden_actions):
        response = su_client.post(url, data)
    assert response.status_code == status_code
    if status_code == 403:
        assert response.json()['error'][0]['code'] == 'forbidden_for_autohire'


@pytest.mark.parametrize('geography_kind', (
    GEOGRAPHY_KINDS.international,
    GEOGRAPHY_KINDS.rus,
))
def test_application_create_offer(su_client, geography_kind):
    vacancy = f.create_active_vacancy(
        geography_international=geography_kind == GEOGRAPHY_KINDS.international,
        geography=f.GeographyFactory(
            is_deleted=False,
            ancestors=[0],
            oebs_code='a code',
            kind=geography_kind,
        ),
    )
    application = f.ApplicationFactory.create(
        status=APPLICATION_STATUSES.in_progress,
        vacancy=vacancy,
    )
    url = reverse('api:applications:create-offer', kwargs={'pk': application.id})

    response = su_client.post(url, {})

    assert response.status_code == 200
    created_offer = Offer.unsafe.all().first()
    assert created_offer.geography == vacancy.geography


action_forms_workflow_data = [
    (APPLICATION_STATUSES.draft, 'activate'),
    (APPLICATION_STATUSES.draft, 'close'),
    (APPLICATION_STATUSES.closed, 'reopen'),
    (APPLICATION_STATUSES.in_progress, 'accept_proposal'),
    (APPLICATION_STATUSES.in_progress, 'reject_proposal'),
    (APPLICATION_STATUSES.closed, 'change_resolution'),
]


@pytest.mark.parametrize('workflow_data', action_forms_workflow_data)
def test_application_action_form(su_client, workflow_data):
    from_status, action_name = workflow_data
    application = f.ApplicationFactory.create(status=from_status)
    view_name = 'api:applications:{}-form'.format(action_name.replace('_', '-'))
    url = reverse(view_name, kwargs={'pk': application.id})
    response = su_client.get(url)
    assert response.status_code == 200


def test_application_detail(su_client):
    application = f.ApplicationFactory.create()
    url = reverse(
        'api:applications:detail',
        kwargs={'pk': application.id}
    )
    response = su_client.get(url)
    assert response.status_code == 200


def test_list_internal_messages(su_client):
    application = f.ApplicationFactory.create()
    f.MessageFactory.create(application=application)
    url = reverse(
        viewname='api:applications:message-list',
        kwargs={'application_id': application.id},
    )
    response = su_client.get(url)
    assert response.status_code == 200


def test_create_internal_message(su_client):
    application = f.create_application(status=APPLICATION_STATUSES.in_progress)
    data = {
        'text': 'text',
    }
    url = reverse(
        viewname='api:applications:message-list',
        kwargs={'application_id': application.id},
    )
    response = su_client.post(url, data)
    assert response.status_code == 201


def test_internal_message_update(su_client):
    user = f.get_superuser()
    message = f.create_message(author=user)
    attachment_to_keep = f.MessageAttachmentFactory(message=message).attachment
    f.MessageAttachmentFactory(message=message)  # attachment_to_delete
    attachment_to_insert = f.AttachmentFactory()

    url = reverse(
        'api:applications:message-detail',
        kwargs={
            # Аргумент application_id не используется в ручке
            'application_id': 0,
            'pk': message.id,
        }
    )
    data = {
        'text': 'text',
        'attachments': [attachment_to_keep.id, attachment_to_insert.id],
    }
    response = su_client.put(url, data)
    assert response.status_code == 200
    response_attachments_ids = {a['id'] for a in response.data['attachments']}
    assert response_attachments_ids == {attachment_to_keep.id, attachment_to_insert.id}


def test_internal_message_delete(su_client):
    user = f.get_superuser()
    message = f.create_message(author=user)
    url = reverse(
        'api:applications:message-detail',
        kwargs={
            # Аргумент application_id не используется в ручке
            'application_id': 0,
            'pk': message.id,
        }
    )
    response = su_client.delete(url)
    assert response.status_code == 204


def test_message_create_form(su_client):
    application = f.ApplicationFactory.create()
    url = reverse(
        'api:applications:message-create-form',
        kwargs={
            'application_id': application.id,
        }
    )
    response = su_client.get(url)
    assert response.status_code == 200


def test_message_update_form(su_client):
    user = f.get_superuser()
    message = f.create_message(author=user)
    url = reverse(
        'api:applications:message-update-form',
        kwargs={
            # Аргумент application_id не используется в ручке
            'application_id': 0,
            'pk': message.id,
        }
    )
    response = su_client.get(url)
    assert response.status_code == 200
