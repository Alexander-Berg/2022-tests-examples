import json
import pytest

from base64 import b64encode
from unittest.mock import patch, Mock

from constance import config
from django.urls.base import reverse

from intranet.femida.src.core.switches import TemporarySwitch
from intranet.femida.src.candidates.choices import ROTATION_STATUSES, VERIFICATION_STATUSES
from intranet.femida.src.candidates.models import CandidateSubmission
from intranet.femida.src.interviews.choices import APPLICATION_STATUSES
from intranet.femida.src.offers.choices import OFFER_STATUSES
from intranet.femida.src.staff.choices import DEPARTMENT_ROLES
from intranet.femida.src.startrek.utils import StatusEnum, ResolutionEnum
from intranet.femida.src.utils.datetime import shifted_now
from intranet.femida.src.vacancies.choices import VACANCY_STATUSES, VACANCY_ROLES, VACANCY_TYPES
from intranet.femida.src.vacancies.controllers import update_or_create_vacancy

from intranet.femida.tests import factories as f
from intranet.femida.tests.clients import APIClient
from intranet.femida.tests.utils import (
    get_forms_constructor_data,
    eager_task,
    ctx_combine,
    patch_service_permissions,
)


pytestmark = pytest.mark.django_db


@patch(
    target='intranet.femida.src.forms_constructor.controllers.reupload_file_to_mds',
    new=Mock(return_value=None),
)
def test_forms_constructor_integration(su_client):
    f.SubmissionFormFactory.create(id=1)
    url = reverse('private-api:forms-constructor-integration')
    data = get_forms_constructor_data(1)
    response = su_client.post(url, data)
    assert response.status_code == 200


@patch(
    target='intranet.femida.src.forms_constructor.controllers.reupload_file_to_mds',
    new=lambda *args, **kwargs: None,
)
def test_forms_constructor_integration_with_publication(su_client):
    publication = f.PublicationFactory()
    data = get_forms_constructor_data(1, candidate_data={'publication_id': publication.id})
    url = reverse('private-api:forms-constructor-integration')
    response = su_client.post(url, data)
    assert response.status_code == 200
    assert CandidateSubmission.unsafe.filter(publication=publication).exists()


def test_forms_aa_entry(su_client):
    url = reverse('private-api:forms-aa-entry')
    applicant = f.create_user()
    data = {
        'params': {
            'applicant': applicant.username,
            'answers': 'Question:\nAnswer',
        },
        'jsonrpc': '2.0',
        'method': 'POST',
    }
    response = su_client.post(url, data)
    assert response.status_code == 200


def test_forms_verification(su_client):
    url = reverse('private-api:forms-verification')
    verification = f.VerificationFactory(link_expiration_date=shifted_now(days=1))
    data = {
        'params': {
            'uuid': verification.uuid.hex,
            'verification_data': {'some': 'data'}
        },
        'jsonrpc': '2.0',
        'method': 'POST',
    }
    response = su_client.post(url, data)
    assert response.status_code == 200


@pytest.mark.parametrize('url_path', (
    'private-api:forms-verification',
    'private-api:forms-verification-ess-check',
))
def test_forms_verification_success(su_client, url_path):
    url = reverse(url_path)
    verification = f.VerificationFactory(
        status=VERIFICATION_STATUSES.on_check,
        link_expiration_date=shifted_now(days=1),
    )
    data = {
        'params': {
            'uuid': verification.uuid.hex,
            'result': 'text about candidate',
        },
        'jsonrpc': '2.0',
        'method': 'POST',
    }
    response = su_client.post(url, data)
    assert response.status_code == 200


def get_appl_id(mode):
    if mode == 'application':
        application = f.ApplicationFactory.create()
        appl_id = str(application.id)
    else:
        candidate = f.CandidateFactory.create()
        appl_id = 'cand%d' % candidate.id
    return appl_id


appl_id_modes = [
    'application',
    'candidate',
]


@pytest.mark.parametrize('mode', appl_id_modes)
def test_separator_identify_candidate(su_client, mode):
    appl_id = get_appl_id(mode)
    url = reverse('private-api:separator-identify-candidate')
    data = {
        'type': 'IDENTIFY',
        'messageId': '12345',
        'appl_id': appl_id,
        'from': 'email@email.com',
    }
    response = su_client.post(url, data)
    assert response.status_code == 200


@pytest.mark.parametrize('mode', appl_id_modes)
def test_separator_create_message(su_client, mode):
    appl_id = get_appl_id(mode)
    url = reverse('private-api:separator-create-message')
    data = {
        'type': 'CREATE',
        'messageId': '12345',
        'applId': appl_id,
        'topic': 'MAIN',
        'subject': 'subject',
        'text': b64encode(b'text'),
        'cleaned_text': b64encode(b'cleaned text'),
        'html': b64encode(b'<div>lalala</div>'),
        'attachments': [{
            'name': 'name',
            'contentType': 'plain/text',
            'content': b64encode(b'body'),
        }],
        'from': 'someone@yandex.ru',
    }
    response = su_client.post(url, data)
    assert response.status_code == 200


def test_isearch_candiate_list(su_client):
    f.create_heavy_candidate()
    f.create_heavy_candidate()
    url = reverse('private-api:isearch-candidate-list')
    response = su_client.get(url)
    assert response.status_code == 200


def test_isearch_candidate_detail(su_client):
    f.create_waffle_switch(TemporarySwitch.ENABLE_NEW_ISEARCH_TAGS_SLUGS)
    candidate = f.create_heavy_candidate()
    expected_tags_info = [
        {'slug': str(tag.id), 'name': tag.name}
        for tag in candidate.tags.all()
    ]
    url = reverse('private-api:isearch-candidate-detail', kwargs={'pk': candidate.id})
    response = su_client.get(url)
    assert response.status_code == 200
    response_data = response.json()
    assert response_data['tags'] == expected_tags_info, response.content


def test_isearch_candidate_acl(su_client):
    url = reverse('private-api:isearch-candidate-acl')
    data = {
        'login': 'superuser',
    }
    response = su_client.get(url, data)
    assert response.status_code == 200


def test_staff_vacancies(su_client):
    vacancy = f.VacancyFactory()
    offer = f.OfferFactory(
        status=OFFER_STATUSES.accepted,
        vacancy=vacancy,
        newhire_id=1,
    )
    f.OfferProfileFactory(offer=offer)
    f.VacancyMembershipFactory(vacancy=vacancy)

    url = reverse('private-api:staff-vacancy-list')
    response = su_client.get(url)
    assert response.status_code == 200

    data = response.json()
    assert data['results'][-1]['offer']
    assert data['results'][-1]['access']


@patch('intranet.femida.src.vacancies.workflow.IssueUpdateOperation.delay')
def test_staff_vacancy_change_department(mocked_issue_update, su_client):
    url = reverse(
        'private-api:staff-vacancy-update-department',
        kwargs={'pk': f.create_vacancy(startrek_key='TJOB-1').id},
    )
    data = {
        'department': f.DepartmentFactory().id,
        'startrek_approval_issue_key': 'KEY-123',
    }
    response = su_client.post(url, data)
    assert response.status_code == 200
    mocked_issue_update.assert_called()


@patch('intranet.femida.src.vacancies.workflow.IssueUpdateOperation.delay')
def test_staff_vacancy_change_value_stream(mocked_issue_update, su_client):
    url = reverse(
        'private-api:staff-vacancy-update',
        kwargs={'pk': f.create_vacancy(startrek_key='TJOB-1').id},
    )
    value_stream = f.ValueStreamFactory()
    data = {
        'value_stream': value_stream.staff_id,
        'startrek_approval_issue_key': 'KEY-123',
    }
    response = su_client.post(url, data)
    assert response.status_code == 200
    response_data = response.json()
    assert response_data['value_stream']['id'] == value_stream.id
    mocked_issue_update.assert_called()


@patch('intranet.femida.src.vacancies.workflow.IssueUpdateOperation.delay')
def test_staff_vacancy_change_geography(mocked_issue_update, su_client):
    url = reverse(
        'private-api:staff-vacancy-update',
        kwargs={'pk': f.create_vacancy(startrek_key='TJOB-1').id},
    )
    geography = f.GeographyFactory()
    data = {
        'geography': geography.id,
        'startrek_approval_issue_key': 'KEY-123',
    }
    response = su_client.post(url, data)
    assert response.status_code == 200
    response_data = response.json()
    assert response_data['geography']['id'] == geography.id
    mocked_issue_update.assert_called()


@pytest.mark.parametrize('vacancy_status, offer_status, expected_status_code', (
    (VACANCY_STATUSES.draft, None, 400),
    (VACANCY_STATUSES.closed, None, 304),
    (VACANCY_STATUSES.offer_processing, OFFER_STATUSES.on_approval, 304),
    (VACANCY_STATUSES.offer_processing, OFFER_STATUSES.on_rotation_approval, 304),
))
def test_staff_vacancy_change_invalid_status(
    su_client,
    vacancy_status,
    offer_status,
    expected_status_code,
):
    url = _setup_vacancy_change_url(vacancy_status, offer_status)
    data = {
        'value_stream': f.ValueStreamFactory().staff_id,
        'startrek_approval_issue_key': 'KEY-123',
    }

    response = su_client.post(url, data)

    assert response.status_code == expected_status_code


@pytest.mark.parametrize('change_department', (True, False))
@pytest.mark.parametrize('has_new_auto_observers', (True, False))
def test_staff_vacancy_change_department_num_queries(django_assert_num_queries, change_department,
                                                     has_new_auto_observers):
    old_department = f.DepartmentFactory()
    new_department = f.DepartmentFactory()
    instance = f.create_vacancy(department=old_department)
    data = {'department': new_department} if change_department else {}
    modified = instance.modified

    old_department_user = f.DepartmentUserFactory.create(
        department=old_department,
        user=f.create_user(),
    )
    f.VacancyMembershipFactory.create(
        vacancy=instance,
        role=VACANCY_ROLES.auto_observer,
        department_user=old_department_user,
    )
    if has_new_auto_observers:
        f.DepartmentUserFactory.create(
            department=new_department,
            user=f.create_user(),
            role=DEPARTMENT_ROLES.chief,
            is_direct=True,
        )

    # Если департамент не меняется, ожидаем 2 запроса в БД:
    # 1. Получение городов вакансии
    # 2. Каст городов к листу (если приходит queryset)
    # 3. Изменение вакансии
    if not change_department:
        num_queries = 3
    # Если меняется департамент, но нет наблюдателей по новому департаменту, ожидаем запросы в БД:
    # 1. Получение городов вакансии
    # 2. Каст городов к листу (если приходит queryset)
    # 3. Изменение вакансии
    # 4. Удаление наблюдателей по старому департаменту
    # 5. Получение наблюдателей по новому департаменту
    elif not has_new_auto_observers:
        num_queries = 5
    # Если меняется департамент и есть наблюдатели по новому департаменту, ожидаем запросы в БД:
    # 1. Получение городов вакансии
    # 2. Каст городов к листу (если приходит queryset)
    # 3. Изменение вакансии
    # 4. Удаление наблюдателей по старому департаменту
    # 5. Получение наблюдателей по новому департаменту
    # 6. Сохранение наблюдателей для текущей вакансии
    else:
        num_queries = 6
    with django_assert_num_queries(num_queries):
        update_or_create_vacancy(data=data, instance=instance)
        assert modified != instance.modified


def test_vacancy_create_bp(su_client):
    vacancy = f.VacancyFactory()

    url = reverse('private-api:staff-vacancy-bp-created', kwargs={'pk': vacancy.id})
    data = {
        'budget_position_id': 100500,
    }
    response = su_client.post(url, data)
    assert response.status_code == 200


def test_close_vacancy_by_bp_registry(client):
    vacancy = f.VacancyFactory(status=VACANCY_STATUSES.on_approval)
    url = reverse('private-api:staff-vacancy-close', kwargs={'pk': vacancy.id})
    response = client.post(url, {'comment': 'some'})
    assert response.status_code == 403

    client.login(f.create_user_with_perm('can_use_api_for_staff').username)
    response = client.post(url, {'comment': 'some'})
    assert response.status_code == 200

    response = client.post(url, {'comment': 'some'})
    assert response.status_code == 403  # Если вакансия в неподходящем статусе


def test_preprofile_create(su_client):
    url = reverse('private-api:preprofiles')
    data = {
        'id': 1,
    }
    response = su_client.post(url, data)
    assert response.status_code == 201, response.content


@pytest.mark.parametrize('vacancy_status', (
    VACANCY_STATUSES.on_approval,
    VACANCY_STATUSES.in_progress,
))
@pytest.mark.parametrize('viewname', (
    'tracker-vacancy-change-type-by-issue',
    'tracker-vacancy-approve-by-issue',
    'tracker-vacancy-approve-bp-by-issue',
))
@pytest.mark.parametrize('issue_key', ('TJOB-1', ''))
def test_tracker_vacancy_edit_by_issue(su_client, vacancy_status, viewname, issue_key):
    url = reverse('private-api:' + viewname)
    data = {
        'issue_key': issue_key,
    }
    f.create_vacancy(
        startrek_key=issue_key,
        status=vacancy_status,
    )
    response = su_client.post(url, data)
    assert response.status_code == 200


@pytest.mark.parametrize('startrek_keys, is_mock_called', (
    (('',), True),
    (('TJOB-1',), False),
    (('TJOB-1', 'TJOB-1'), False),
))
@patch('intranet.femida.src.api.tracker.views.tracker_trigger_task.delay')
def test_tracker_vacancy_approve_by_issue_unknown_issue_key(mocked_task, su_client,
                                                            startrek_keys, is_mock_called):
    url = reverse('private-api:tracker-vacancy-approve-by-issue')
    data = {
        'issue_key': 'TJOB-1',
    }
    for key in startrek_keys:
        f.create_vacancy(
            startrek_key=key,
            status=VACANCY_STATUSES.on_approval,
        )
    su_client.post(url, data)
    assert mocked_task.called == is_mock_called


@pytest.mark.parametrize('vacancy_status, is_mock_called', (
    (VACANCY_STATUSES.on_approval, True),
    (VACANCY_STATUSES.in_progress, True),
    (VACANCY_STATUSES.suspended, True),
    (VACANCY_STATUSES.draft, False),
    (VACANCY_STATUSES.offer_accepted, False),
    (VACANCY_STATUSES.offer_processing, False),
    (VACANCY_STATUSES.closed, False),
))
@patch('intranet.femida.src.api.vacancies.views.vacancy_close_by_issue_task.delay')
def test_tracker_vacancy_close_by_issue(mocked_task, su_client, vacancy_status, is_mock_called):
    url = reverse('private-api:tracker-vacancy-close-by-issue')
    issue_key = 'TJOB-1'
    data = {
        'issue_key': issue_key,
    }
    f.create_vacancy(
        startrek_key=issue_key,
        status=vacancy_status,
    )
    response = su_client.post(url, data)
    assert response.status_code == 200
    assert mocked_task.called == is_mock_called


@pytest.mark.parametrize('offer_status', (
    OFFER_STATUSES.draft,
    OFFER_STATUSES.on_approval,
))
@pytest.mark.parametrize('viewname', (
    'tracker-offer-approve-by-issue',
    'tracker-offer-confirm-by-issue',
))
@pytest.mark.parametrize('issue_key', ('TJOB-1', ''))
def test_tracker_offer_confirm_by_issue(su_client, offer_status, viewname, issue_key):
    url = reverse('private-api:' + viewname)
    data = {
        'issue_key': issue_key,
    }
    vacancy = f.create_vacancy(
        startrek_key=issue_key,
        status=VACANCY_STATUSES.offer_processing,
    )
    application = f.create_application(vacancy=vacancy, status=APPLICATION_STATUSES.in_progress)
    f.create_offer(vacancy=vacancy, application=application, status=offer_status)
    response = su_client.post(url, data)
    assert response.status_code == 200


@pytest.mark.parametrize('offer_status', (
    OFFER_STATUSES.draft,
    OFFER_STATUSES.on_rotation_approval,
))
@pytest.mark.parametrize('issue_key', ('TSALARY-1', ''))
def test_tracker_offer_confirm_by_current_team(su_client, offer_status, issue_key):
    url = reverse('private-api:tracker-offer-confirm-by-current-team')
    data = {
        'issue_key': issue_key,
    }
    vacancy = f.create_vacancy(
        startrek_key=issue_key,
        status=VACANCY_STATUSES.offer_processing,
    )
    application = f.create_application(vacancy=vacancy, status=APPLICATION_STATUSES.in_progress)
    f.create_offer(vacancy=vacancy, application=application, status=offer_status)
    response = su_client.post(url, data)
    assert response.status_code == 200


@patch('intranet.femida.src.hire_orders.signals.perform_hire_order_action_task.delay')
def test_tracker_offer_reject_by_issue(mocked_action, su_client):
    issue_key = 'TJOB-1'
    application = f.ApplicationFactory(
        status=APPLICATION_STATUSES.in_progress,
        vacancy__type=VACANCY_TYPES.autohire,
        vacancy__status=VACANCY_STATUSES.offer_processing,
        vacancy__startrek_key=issue_key,
    )
    offer = f.OfferFactory(application=application, status=OFFER_STATUSES.on_approval)
    hire_order = f.HireOrderFactory(offer=offer)

    issue = Mock(key=issue_key)
    issue.status.key = StatusEnum.closed
    issue.resolution.key = ResolutionEnum.wont_fix

    ctx_managers = ctx_combine(
        eager_task('intranet.femida.src.api.offers.views.offer_decline_by_issue_task'),
        patch('intranet.femida.src.offers.tasks.get_issue', return_value=issue),
    )
    with ctx_managers:
        url = reverse('private-api:tracker-offer-decline-by-issue')
        response = su_client.post(url, {'issue_key': issue_key})

    assert response.status_code == 200
    mocked_action.assert_called_once_with(hire_order.id, 'cancel', resolution='offer_unapproved')


@patch('intranet.femida.src.api.rotations.views.rotation_approve_task.delay')
def test_tracker_rotation_approve(mocked_task, client):
    rotation = f.RotationFactory(status=ROTATION_STATUSES.new)
    url = reverse('private-api:tracker-rotation-approve')
    data = {
        'issue_key': rotation.startrek_rotation_key,
    }
    response = client.post(url, data)
    assert response.status_code == 200
    assert mocked_task.called


@patch('intranet.femida.src.api.rotations.views.rotation_approve_task.delay')
def test_tracker_rotation_approve_not_found(mocked_task, client):
    rotation = f.RotationFactory(status=ROTATION_STATUSES.approved)
    url = reverse('private-api:tracker-rotation-approve')
    data = {
        'issue_key': rotation.startrek_rotation_key,
    }
    response = client.post(url, data)
    assert response.status_code == 200
    assert not mocked_task.called


@patch('intranet.femida.src.api.rotations.views.rotation_reject_task.delay')
def test_tracker_rotation_reject(mocked_task, client):
    rotation = f.RotationFactory(status=ROTATION_STATUSES.new)
    url = reverse('private-api:tracker-rotation-reject')
    data = {
        'issue_key': rotation.startrek_rotation_key,
    }
    response = client.post(url, data)
    assert response.status_code == 200
    assert mocked_task.called


@patch('intranet.femida.src.api.rotations.views.rotation_reject_task.delay')
def test_tracker_rotation_reject_not_found(mocked_task, client):
    rotation = f.RotationFactory(status=ROTATION_STATUSES.rejected)
    url = reverse('private-api:tracker-rotation-reject')
    data = {
        'issue_key': rotation.startrek_rotation_key,
    }
    response = client.post(url, data)
    assert response.status_code == 200
    assert not mocked_task.called


@pytest.mark.parametrize('status, is_mock_called', (
    (VERIFICATION_STATUSES.new, False),
    (VERIFICATION_STATUSES.on_check, False),
    (VERIFICATION_STATUSES.on_ess_check, True),
    (VERIFICATION_STATUSES.closed, False),
))
@patch('intranet.femida.src.api.candidates.views.resolve_verification_task.delay')
def test_tracker_verification_result(mocked_task, client, status, is_mock_called):
    verification = f.VerificationFactory(
        startrek_ess_key='TESS-1',
        status=status,
    )
    url = reverse('private-api:tracker-verification-resolve')
    data = {
        'issue_key': verification.startrek_ess_key,
    }
    response = client.post(url, data)
    assert response.status_code == 200
    assert mocked_task.called == is_mock_called


@patch('intranet.femida.src.api.core.validators.yt.exists', lambda x: True)
@patch('intranet.femida.src.api.core.validators.yt.get', lambda *x, **y: {'type': 'table'})
@patch('intranet.femida.src.api.candidates.views.yt_bulk_upload_candidates_task.delay')
def test_yt_candidate_bulk_upload(mocked_task, client):
    url = reverse('private-api:yt-candidate-bulk-upload')
    data = {
        'table': '//home/femida/candidates-list-test',
        'mode': 'check',
    }
    user = f.create_user_with_perm('can_use_yt_candidate_uploader')
    config.YT_CANDIDATE_UPLOAD_SOURCES = json.dumps({user.username: 'yang'})
    client.login(login=user.username)
    response = client.post(url, data)
    assert response.status_code == 201, response.content
    mocked_task.assert_called_once_with(source='yang', **data)


@patch('intranet.femida.src.api.core.validators.yt.exists', lambda x: True)
@patch('intranet.femida.src.api.core.validators.yt.get', lambda *x, **y: {'type': 'table'})
@patch('intranet.femida.src.api.candidates.views.yt_bulk_upload_candidate_scorings_task.delay')
def test_yt_candidate_scoring_bulk_upload(mocked_task, client):
    category = f.ScoringCategoryFactory()
    url = reverse('private-api:yt-candidate-scorings-bulk-upload')
    data = {
        'table': '//home/femida/candidate-scoring1',
        'scoring_category': category.id,
        'version': '1',
    }
    user = f.create_user_with_perm('can_use_yt_candidate_scoring_uploader')
    client.login(login=user.username)
    response = client.post(url, data)
    assert response.status_code == 201, response.content
    mocked_task.assert_called_once_with(**data)


@patch_service_permissions({123: ['permissions.can_access_magiclinks_data']})
def test_magiclinks_candidate_list():
    client = APIClient()
    client.login(
        login=f.create_superuser().username,
        mechanism_name='tvm',
        tvm_client_id=123,
    )

    jamie = f.CandidateFactory(first_name='Jamie', last_name='Vardy')
    kevin = f.CandidateFactory(first_name='Kevin', last_name='De Bruyne')

    url = reverse('private-api:magiclinks-candidate-list')
    fields = ('id', 'first_name', 'last_name')
    params = {
        'id': f'{jamie.id},{kevin.id}',
        '_fields': ','.join(fields),
    }
    response = client.get(url, params)
    assert response.status_code == 200, response.content

    results = sorted(response.json()['results'], key=lambda x: x['id'])
    as_dict = lambda x: {i: getattr(x, i) for i in fields}
    assert results == [as_dict(jamie), as_dict(kevin)], response.content


def _setup_vacancy_change_url(vacancy_status, offer_status=None):
    vacancy = f.create_heavy_vacancy(
        startrek_key='TJOB-1',
        type=VACANCY_TYPES.autohire,
        status=vacancy_status,
    )

    if offer_status is not None:
        application = f.create_application(vacancy=vacancy, status=APPLICATION_STATUSES.in_progress)
        f.create_offer(vacancy=vacancy, application=application, status=offer_status)

    url = reverse(
        'private-api:staff-vacancy-update',
        kwargs={'pk': vacancy.id},
    )

    return url
