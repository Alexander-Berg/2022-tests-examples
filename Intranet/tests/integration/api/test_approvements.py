import pytest

from unittest.mock import Mock, patch

from django.db import IntegrityError
from django.urls import reverse
from django.utils import timezone
from startrek_client.exceptions import NotFound
from waffle.testutils import override_switch

from ok.approvements import controllers
from ok.approvements.choices import (
    APPROVEMENT_STAGE_APPROVEMENT_SOURCES,
    APPROVEMENT_STATUSES,
    APPROVEMENT_TYPES,
    APPROVEMENT_ROLES,
    APPROVEMENT_STAGE_STATUSES,
    APPROVEMENT_STAGE_ACTIVE_STATUSES,
)
from ok.approvements.models import Approvement
from ok.flows import executor as flow_executor
from ok.tracker.queues import get_queue_name
from ok.utils.strings import str_to_md5

from tests import factories as f
from tests.integration.api.mocks import (
    _mock_external_users,
    _mock_get_staff_iter_500,
    _mock_group_members,
    _mock_staff_users,
    _mock_get_validated_map_staff_users,
)


pytestmark = pytest.mark.django_db


flow_name = 'some_wf'


def approvement_stages_data():
    return [
        {'approver': 'agrml'},
        {'stages': [{'approver': 'qazaq'}, {'approver': 'tmalikova'}]},
        {'approver': 'terrmit'},
    ]


@pytest.fixture
def approvement_base_data():
    return {
        'text': 'test-text',
        'object_id': 'JOB-123',
    }


@pytest.fixture
def approvement_edit_data():
    return {
        'text': '123',
        'is_parallel':  True,
        'is_reject_allowed': False,
        'callback_url': 'gsgd.com',
        'tvm_id': 123,
        'disapproval_reasons': ['no_budget', 'wont_fix', 'wont_fix']
    }


@pytest.fixture
def approvement_data(approvement_base_data):
    approvement_base_data['stages'] = approvement_stages_data()
    return approvement_base_data


@pytest.fixture
def approvement_flow_data(approvement_base_data):
    approvement_base_data['flow_name'] = flow_name
    approvement_base_data['context'] = {'asd': 1}
    return approvement_base_data


@pytest.mark.parametrize('is_parallel', (True, False))
@pytest.mark.parametrize(
    'type, expected_type', [
        (None, APPROVEMENT_TYPES.tracker),
        (APPROVEMENT_TYPES.tracker, APPROVEMENT_TYPES.tracker),
        (APPROVEMENT_TYPES.general, APPROVEMENT_TYPES.general),
    ],
)
@patch('ok.api.core.forms.UserValidationFormMixin._get_valid_users', _mock_staff_users)
def test_approvement_create(client, approvement_data, type, expected_type, is_parallel):
    f.create_waffle_flag('can_set_approvement_author', True)
    url = reverse('api:approvements:list-create')
    approvement_data['is_parallel'] = is_parallel
    if type:
        approvement_data['type'] = type

    response = client.post(url, data=approvement_data)

    assert response.status_code == 201, response.content
    approvement = Approvement.objects.get(id=response.json()['id'])
    assert approvement.type == expected_type


@patch('ok.api.core.forms.UserValidationFormMixin._get_valid_users', _mock_staff_users)
def test_approvement_edit(client, approvement_edit_data):
    client.force_authenticate('author')
    approvement = f.create_approvement(stages_count=2, author='author')
    url = reverse('api:approvements:edit', kwargs={'uuid': approvement.uuid})

    response = client.put(url, data={
        'stages': [{'approver': 'dmitrypro'}, {'approver': 'voux'}],
        **approvement_edit_data,
    })

    assert response.status_code == 200, response.content
    approvement.refresh_from_db()

    approvers = {a.approver for a in list(approvement.stages.all())}

    assert approvement.text == approvement_edit_data['text']
    assert 'dmitrypro' in approvers
    assert 'voux' in approvers
    assert approvement.is_parallel
    assert not approvement.is_reject_allowed
    assert approvement.callback_url == approvement_edit_data['callback_url']
    assert approvement.tvm_id == approvement_edit_data['tvm_id']
    assert set(approvement.disapproval_reasons) == set(approvement_edit_data['disapproval_reasons'])


@pytest.mark.parametrize('current_user', ('author', 'no_author', 'group_url_member'))
@pytest.mark.parametrize('new_stages', (
        [{'approver': 'dmitrypro'}, {'approver': 'voux'}],
        [{'approver': 'approver0'}, {'approver': 'voux'}]
))
@patch('ok.approvements.workflow.get_staff_group_member_logins', _mock_group_members)
@patch('ok.api.core.forms.UserValidationFormMixin._get_valid_users', _mock_staff_users)
def test_approvement_edit_stages(client, current_user, approvement_edit_data, new_stages):
    client.force_authenticate(current_user)
    f.create_waffle_switch('enable_complex_approve_by_responsible')
    initiator = 'author'
    approvement = f.ApprovementFactory(
        author=initiator,
        groups=['group_url'],
    )
    parent = f.create_parent_stage(approvement=approvement, need_approvals=1)
    f.create_child_stage(parent, approver='approver0')
    f.create_child_stage(parent, approver='approver1')

    url = reverse('api:approvements:edit', kwargs={'uuid': approvement.uuid})

    response = client.put(url, data={
        'stages': [{'stages': new_stages}],
        **approvement_edit_data,
    })

    if current_user == 'no_author':
        assert response.status_code == 403
        return

    assert response.status_code == 200, response.content
    approvement.refresh_from_db()

    approvers = [a.approver for a in list(approvement.stages.all()) if a.approver]
    expected_approvers = [a['approver'] for a in new_stages]

    assert approvement.text == approvement_edit_data['text']
    assert approvers == expected_approvers
    assert approvement.is_parallel
    assert not approvement.is_reject_allowed
    assert approvement.callback_url == approvement_edit_data['callback_url']
    assert approvement.tvm_id == approvement_edit_data['tvm_id']
    assert set(approvement.disapproval_reasons) == set(approvement_edit_data['disapproval_reasons'])


@pytest.mark.parametrize('current_user', ('author', 'group_url_member'))
@patch('ok.approvements.workflow.get_staff_group_member_logins', _mock_group_members)
@patch('ok.api.core.forms.UserValidationFormMixin._get_valid_users', _mock_staff_users)
def test_approvement_edit_approved_stage(client, current_user, approvement_edit_data):
    client.force_authenticate(current_user)
    f.create_waffle_switch('enable_complex_approve_by_responsible')
    initiator = 'author'
    approvement = f.ApprovementFactory(
        author=initiator,
        groups=['group_url'],
    )
    parent = f.create_parent_stage(approvement=approvement, need_approvals=1)
    f.create_child_stage(parent, approver='approver0', is_approved=True)
    f.create_child_stage(parent, approver='approver1')

    url = reverse('api:approvements:edit', kwargs={'uuid': approvement.uuid})

    response = client.put(url, data={
        'stages': [{'stages': [{'approver': 'dmitrypro'}, {'approver': 'voux'}]}],
        **approvement_edit_data,
    })

    assert response.status_code == 400


@patch('ok.api.core.forms.UserValidationFormMixin._get_valid_users', _mock_staff_users)
def test_approvement_create_normalize_stages(client, approvement_data):
    """
    Проверяет, что при создании согласования стадии нормализуются:
    – из согласования выкидываются пустые листовые стадии;
    - групповая стадия с одной дочерней превращается в плоскую;
    """
    url = reverse('api:approvements:list-create')
    approvement_data['stages'] = [
        {'approver': '', 'stages': [], 'need_approvals': 1},
        {'approver': 'dmitrypro', 'stages': [], 'need_approvals': 1},
        {'approver': '', 'stages': [
            {'approver': ''}, {'approver': ''}, {'approver': 'zivot'}
        ], 'need_approvals': 1},
        {'approver': '', 'stages': [
            {'approver': ''}, {'approver': 'kiparis'}, {'approver': 'dmirain'}
        ], 'need_approvals': 1},
        {'approver': '', 'stages': [{'approver': ''}], 'need_approvals': 1}]

    response = client.post(url, data=approvement_data)

    assert response.status_code == 201, response.content


@patch('ok.api.core.forms.UserValidationFormMixin._get_valid_users', _mock_staff_users)
@patch('ok.approvements.tasks.close_or_restore_approvement_task.delay')
def test_approvement_create_comment(patched_task, client, approvement_data):
    url = reverse('api:approvements:list-create')
    approvement_data['create_comment'] = True
    response = client.post(url, data=approvement_data)

    assert response.status_code == 201, response.content
    patched_task.assert_called_once()


@patch('ok.api.core.forms.UserValidationFormMixin._get_valid_users', _mock_staff_users)
def test_approvement_create_with_disapproval_reasons(client, approvement_data):
    url = reverse('api:approvements:list-create')
    approvement_data['disapproval_reasons'] = ['no_budget', 'wont_fix']
    response = client.post(url, data=approvement_data)
    assert response.status_code == 201, response.content


@patch('ok.api.core.forms.UserValidationFormMixin._get_valid_users', _mock_staff_users)
def test_approvement_create_with_invalid_disapproval_reasons(client, approvement_data):
    url = reverse('api:approvements:list-create')
    approvement_data['disapproval_reasons'] = ['invalid reason!!!']
    response = client.post(url, data=approvement_data)
    assert response.status_code == 400, response.content


@pytest.mark.parametrize('is_parallel', (True, False))
@patch('ok.api.core.forms.UserValidationFormMixin._get_valid_users', _mock_staff_users)
def test_approvement_create_with_flow(client, approvement_flow_data, is_parallel, monkeypatch):
    url = reverse('api:approvements:list-create')
    approvement_flow_data['is_parallel'] = is_parallel

    monkeypatch.setattr(controllers, 'execute_flow', lambda *a, **kw: {'data': {'stages': approvement_stages_data()}})

    response = client.post(url, data=approvement_flow_data)
    assert response.status_code == 201, response.content


@pytest.mark.parametrize('is_parallel', (True, False))
@patch('ok.api.core.forms.UserValidationFormMixin._get_valid_users', _mock_staff_users)
def test_approvement_create_with_nonexisting_flow(client, approvement_flow_data, is_parallel, monkeypatch):
    url = reverse('api:approvements:list-create')
    approvement_flow_data['is_parallel'] = is_parallel

    def raising_fu(*a, **kw):
        raise flow_executor.FlowNotFound(approvement_flow_data['flow_name'])

    monkeypatch.setattr(controllers, 'execute_flow', raising_fu)

    response = client.post(url, data=approvement_flow_data)
    assert response.status_code == 404, response.content


@pytest.mark.parametrize('is_parallel', (True, False))
@patch('ok.api.core.forms.UserValidationFormMixin._get_valid_users', _mock_staff_users)
def test_approvement_create_with_flow_no_returning_stages(client, approvement_flow_data, is_parallel, monkeypatch):
    url = reverse('api:approvements:list-create')
    approvement_flow_data['is_parallel'] = is_parallel

    monkeypatch.setattr(controllers, 'execute_flow', lambda *a, **kw: {'data': {}, 'detail': {}})

    response = client.post(url, data=approvement_flow_data)
    assert response.status_code == 400, response.content


@patch('ok.api.core.forms.UserValidationFormMixin._get_valid_users', _mock_staff_users)
def test_approvement_flow_dropped(client, approvement_flow_data, monkeypatch):
    url = reverse('api:approvements:list-create')

    monkeypatch.setattr(controllers, 'execute_flow', lambda *a, **kw:  {
        'data': {'stages': []},
        'detail': {'error': 'Flow dropped'},
    })

    response = client.post(url, data=approvement_flow_data)
    assert response.status_code == 200, response.content['detail'] == {'error': 'Flow dropped'}


@patch('ok.api.core.forms.UserValidationFormMixin._get_valid_users', _mock_staff_users)
def test_approvement_flow_table_flow_failed(client, approvement_flow_data, monkeypatch):
    url = reverse('api:approvements:list-create')

    monkeypatch.setattr(controllers, 'execute_flow', lambda *a, **kw:  {
        'data': {'stages': []},
        'detail': {'error': 'Table flow call failed',  'code': 429},
    })

    response = client.post(url, data=approvement_flow_data)
    assert response.status_code == 429, response.content['detail'] == {'error': 'Table flow call failed'}


@pytest.mark.parametrize('is_parallel', (True, False))
@patch('ok.api.core.forms.UserValidationFormMixin._get_valid_users', _mock_staff_users)
@patch('ok.approvements.controllers.get_validated_map_staff_users', _mock_get_validated_map_staff_users)
def test_approvement_create_form_with_flow(client, approvement_base_data, is_parallel, monkeypatch):
    url = reverse('api:approvements:create-form')
    data = approvement_base_data
    data['is_parallel'] = is_parallel
    data['flow_name'] = flow_name
    data['context.asd'] = 10
    stages_data = approvement_stages_data()

    monkeypatch.setattr(controllers, 'execute_flow', lambda *a, **kw: {'data': {'stages': stages_data}})

    response = client.get(url, data=data)
    assert response.status_code == 200, response.content
    returned_stages = response.json()['data']['stages']['value'][1]['value']['stages']
    assert (returned_stages['value'][0]['value']['approver']['value']['login'] ==
            stages_data[1]['stages'][0]['approver'])


def test_approvement_create_form_callback_url(client, approvement_base_data):
    url = reverse('api:approvements:create-form')
    data = approvement_base_data
    data['callback_url'] = 'http://1.ru'

    response = client.get(url, data=data)
    assert response.status_code == 200, response.content
    assert response.json()['data']['callback_url']['value'] == 'http://1.ru'


@pytest.mark.parametrize('tvm_id', (1, None))
@patch('ok.api.core.forms.UserValidationFormMixin._get_valid_users', _mock_staff_users)
@patch('ok.approvements.controllers.get_validated_map_staff_users', _mock_get_validated_map_staff_users)
def test_approvement_create_callback_url(client, approvement_base_data, monkeypatch, tvm_id):
    url = reverse('api:approvements:list-create')
    data = approvement_base_data
    data['callback_url'] = 'http://1.ru'
    data['flow_name'] = flow_name

    if tvm_id:
        data['tvm_id'] = tvm_id

    stages_data = approvement_stages_data()

    monkeypatch.setattr(controllers, 'execute_flow', lambda *a, **kw: {'data': {'stages': stages_data}})

    response = client.post(url, data=data)
    assert response.status_code == 201, response.content
    assert response.json()['callback_url'] == 'http://1.ru'


@patch('ok.api.core.forms.UserValidationFormMixin._get_valid_users', _mock_staff_users)
@patch('ok.approvements.controllers.get_validated_map_staff_users', _mock_get_validated_map_staff_users)
def test_approvement_create_form_with_flow_make_autoapprovable(client, approvement_base_data, monkeypatch):
    url = reverse('api:approvements:list-create')
    data = approvement_base_data
    data['flow_name'] = flow_name
    data['context.asd'] = 10
    stages_data = approvement_stages_data()

    monkeypatch.setattr(controllers, 'execute_flow',
                        lambda *a, **kw: {'data': {'stages': stages_data, 'is_auto_approving': True}})

    response = client.post(url, data=data)
    assert response.status_code == 201, response.content

    assert response.json()['is_auto_approving'] is True



@pytest.mark.parametrize('status', (
    APPROVEMENT_STATUSES.in_progress,
    APPROVEMENT_STATUSES.suspended,
))
@patch('ok.api.core.forms.UserValidationFormMixin._get_valid_users', _mock_staff_users)
def test_approvement_create_if_active_exists(client, status, approvement_data):
    """
    Пробуем создать согласование, когда в тикете уже есть другое не закрытое
    """
    approvement = f.ApprovementFactory.create(
        status=status,
        object_id=approvement_data['object_id'],
    )

    url = reverse('api:approvements:list-create')
    response = client.post(url, data=approvement_data)
    assert response.status_code == 400, response.content
    response_data = response.json()
    error = response_data['error'][0]
    assert error['code'] == 'already_exists'
    assert error['params']['uuid'] == str(approvement.uuid)


@patch('ok.api.approvements.forms.ApprovementCreateForm.clean_object_id')
@patch('ok.api.core.forms.UserValidationFormMixin._get_valid_users', _mock_staff_users)
def test_approvement_create_duplicate_integrity_error(mocked_clean_object_id, client, approvement_data):
    # IntegrityError случается, когда пришли одновременно два одинаковых запроса:
    # первый отрабатывает и создаёт согласование, а второй падает с 500, потому что на
    # момент валидации формы дублирующего согласования ещё не сущетсвовало, а в момент вставки
    # оно уже появилось, тем самым нарушается уникальность ключа uid, object_id.
    # Имитируем здесь это же поведение: создаём сразу дублирующее согласование,
    # при этом мокаем форму, чтобы дубликат не отстрелился на валидации
    approvement_data['uid'] = 'some_random_uid'
    approvement = f.ApprovementFactory.create(
        object_id=approvement_data['object_id'],
        uid=str_to_md5(approvement_data['uid']),
    )
    mocked_clean_object_id.return_value = approvement.object_id

    url = reverse('api:approvements:list-create')
    response = client.post(url, data=approvement_data)

    assert response.status_code == 400, response.content
    response_data = response.json()
    error = response_data['error'][0]
    assert error['code'] == 'already_exists'
    assert error['params']['uuid'] == str(approvement.uuid)


@patch('ok.approvements.controllers.ApprovementController.create')
@patch('ok.api.core.forms.UserValidationFormMixin._get_valid_users', _mock_staff_users)
def test_approvement_create_not_duplicate_integrity_error(mocked_create, client, approvement_data):
    """
    В already_exists превращаются только ошибки про дубликаты данных,
    а всё остальное так и остаётся пятисотымми
    """
    mocked_create.side_effect = IntegrityError()

    url = reverse('api:approvements:list-create')
    with pytest.raises(IntegrityError):
        client.post(url, data=approvement_data)


@patch('ok.api.core.forms.UserValidationFormMixin._get_valid_users', _mock_staff_users)
def test_approvement_create_if_closed_exists(client, approvement_data):
    """
    Пробуем создать согласование, когда в тикете уже есть другое закрытое
    """
    f.ApprovementFactory.create(
        status=APPROVEMENT_STATUSES.closed,
        object_id=approvement_data['object_id'],
    )

    url = reverse('api:approvements:list-create')
    response = client.post(url, data=approvement_data)
    assert response.status_code == 201, response.content


@patch('ok.api.core.forms.UserValidationFormMixin._get_valid_users', _mock_external_users)
def test_approvement_create_with_external_users(client, approvement_data):
    """
    Пробуем создать согласование с внешними пользователями без доступа в очередь
    """
    f.QueueFactory.create(name=get_queue_name(approvement_data['object_id']), allow_externals=False)
    url = reverse('api:approvements:list-create')
    response = client.post(url, data=approvement_data)
    assert response.status_code == 400, response.content


@patch('ok.api.core.forms.UserValidationFormMixin._get_valid_users', _mock_external_users)
@patch('ok.api.approvements.validators.tracker.issues.get')
def test_approvement_create_with_nonexistent_object_id(mocked_action, client, approvement_data):
    """
    Пробуем создать согласование с несуществующим object_id
    """
    mocked_action.side_effect = NotFound(Mock())

    url = reverse('api:approvements:list-create')
    response = client.post(url, data=approvement_data)
    assert response.status_code == 400, response.content
    assert response.data['errors'][''][0]['code'] == 'issue_does_not_exist'


@pytest.mark.parametrize('object_id, error', (
    (None, {'object_id': [{'code': 'required'}]}),
    ('', {'object_id': [{'code': 'required'}]}),
    ('JOB', {'': [{'code': 'issue_does_not_exist'}]}),
    ('JOB_1-23', {'': [{'code': 'issue_does_not_exist'}]}),
))
@patch('ok.api.core.forms.UserValidationFormMixin._get_valid_users', _mock_external_users)
def test_approvement_create_with_invalid_object_id(client, approvement_data, object_id, error):
    """
    Пробуем создать согласование с невалидным object_id
    """
    approvement_data['object_id'] = object_id

    url = reverse('api:approvements:list-create')
    response = client.post(url, data=approvement_data)
    assert response.status_code == 400, response.content
    assert response.data['errors'] == error


@patch('ok.api.approvements.forms.validate_issue_key')
@patch('ok.api.core.forms.UserValidationFormMixin._get_valid_users', _mock_external_users)
def test_do_not_check_tracker_fields_for_non_tracker_approvement(patched_validate_issue_key, client, approvement_data):
    approvement_data['type'] = APPROVEMENT_TYPES.general
    approvement_data['object_id'] = 'anyformatobjectid'

    response = client.post(reverse('api:approvements:list-create'), data=approvement_data)

    assert response.status_code == 201, response.content
    approvement = Approvement.objects.get(id=response.json()['id'])
    assert approvement.type == APPROVEMENT_TYPES.general
    assert approvement.object_id == approvement_data['object_id']
    patched_validate_issue_key.assert_not_called()


@patch('ok.api.core.forms.UserValidationFormMixin._get_valid_users', _mock_staff_users)
def test_approvement_create_with_internal_users(client, approvement_data):
    f.QueueFactory.create(name=get_queue_name(approvement_data['object_id']), allow_externals=False)
    url = reverse('api:approvements:list-create')
    response = client.post(url, data=approvement_data)
    assert response.status_code == 201, response.content


@patch('ok.utils.staff.get_staff_iter', _mock_get_staff_iter_500)
def test_approvement_create_400_on_staff_500(client, approvement_data):
    url = reverse('api:approvements:list-create')
    response = client.post(url, data=approvement_data)
    assert response.status_code == 400, response.content


@patch('ok.api.core.forms.UserValidationFormMixin._get_valid_users')
def test_approvement_create_with_invalid_logins(mocked_get_logins, client, approvement_data):
    approvement_data['author'] = 'defined_author'
    mocked_get_logins.return_value = dict()
    url = reverse('api:approvements:list-create')
    response = client.post(url, data=approvement_data)

    assert response.status_code == 400, response.content
    errors = response.json()['errors']
    assert 'author' in errors
    assert 'stages' in errors
    mocked_get_logins.assert_called_once()


@pytest.mark.parametrize('can_set_approvement_author', (True, False))
@patch('ok.api.core.forms.UserValidationFormMixin._get_valid_users', _mock_staff_users)
def test_approvement_create_with_defined_author(client, can_set_approvement_author, approvement_data):
    f.create_waffle_flag('can_set_approvement_author', can_set_approvement_author)
    approvement_data['author'] = 'defined_author'
    user = 'user'

    client.force_authenticate(user)
    url = reverse('api:approvements:list-create')
    response = client.post(url, data=approvement_data)

    assert response.status_code == 201, response.content
    result = response.json()
    expected_author = approvement_data['author'] if can_set_approvement_author else user
    assert result['author'] == expected_author


@pytest.mark.parametrize('stage_data, status_code, need_approvals', (
    ({'is_with_deputies': True}, 201, 1),
    ({'is_with_deputies': True, 'need_all': True}, 400, None),
    ({'is_with_deputies': True, 'need_approvals': 2}, 400, None),
    ({'need_all': True}, 201, 3),
    ({'need_approvals': 2}, 201, 2),
))
@patch('ok.api.core.forms.UserValidationFormMixin._get_valid_users', _mock_staff_users)
def test_approvement_create_with_deputies(client, approvement_data, stage_data, status_code,
                                          need_approvals):
    stage = dict(
        {'stages': [{'approver': 'ktussh'}, {'approver': 'denis-an'}, {'approver': 'qazaq'}]},
        **stage_data,
    )
    approvement_data['stages'].append(stage)

    user = 'user'
    client.force_authenticate(user)
    url = reverse('api:approvements:list-create')
    response = client.post(url, data=approvement_data)

    assert response.status_code == status_code, response.content
    if need_approvals is not None:
        result_stage = response.json()['stages'][-1]
        assert result_stage['need_approvals'] == need_approvals


def test_approvement_detail(client):
    approvement = f.ApprovementFactory()
    url = reverse('api:approvements:detail', kwargs={'uuid': approvement.uuid})
    response = client.get(url)
    assert response.status_code == 200, response.content


validated_users_mock = {
    'mocked_login': {
        'login': 'mocked_login',
        'fullname': 'Mocked FullName',
    },
}


@patch('ok.approvements.controllers.get_validated_map_staff_users', lambda x: validated_users_mock)
def test_approvement_form(client, approvement_data):
    url = reverse('api:approvements:create-form')
    response = client.get(url, approvement_data)
    assert response.status_code == 200, response.content


@patch('ok.utils.staff.get_staff_iter', _mock_get_staff_iter_500)
def test_approvement_create_form_200_on_staff_500(client, approvement_data):
    """
    Ошибка предзаполнения формы должна обрабатываться тихо.
    400/500 быть не должно.
    """
    url = reverse('api:approvements:create-form')
    response = client.get(url, approvement_data)
    assert response.status_code == 200, response.content


@patch('ok.approvements.controllers.get_validated_map_staff_users')
def test_approvement_form_preset(mocked_staff_users, client):
    """
    Предустановленные параметры формы корректно работают
    """
    data = {
        'text': 'test-text',
        'groups': ['g1', 'g2'],
        # Для обратной совместимости логины согласующих прокидываются
        # в поле users, а не stages. При этом в предустановленных
        # параметрах они запишутся как в users, так и в stages
        'users': ['u1', 'u2'],
    }
    mocked_staff_users.return_value = {u: {'login': u, 'fullname': u} for u in data['users']}

    url = reverse('api:approvements:create-form')
    response = client.get(url, data)

    assert response.status_code == 200, response.content
    preset = response.json()['data']

    assert preset['text']['value'] == data['text']
    groups = [g['value'] for g in preset['groups']['value']]
    assert groups == data['groups']

    stages_users = [s['value']['approver']['value']['login'] for s in preset['stages']['value']]
    assert stages_users == data['users']


@pytest.mark.parametrize('initiator,status_code', (
    ('author', 200),
    ('approver', 200),
    ('group_url_member', 200),
))
@patch('ok.approvements.workflow.get_staff_group_member_logins', _mock_group_members)
@patch('ok.notifications.approvements.collect_issues_for_approvements', Mock())
@patch('ok.notifications.approvements.StaffUser.fetch', lambda x: {})
@patch('ok.notifications.base.get_user_to_language', lambda x: {})
def test_approvement_approve(client, initiator, status_code):
    approvement = f.ApprovementFactory(
        author='author',
        groups=['group_url'],
    )
    stage = f.ApprovementStageFactory(
        approvement=approvement,
        approver='approver',
    )

    data = {
        'approver': 'approver',
    }

    url = reverse('api:approvements:approve', kwargs={'uuid': approvement.uuid})
    client.force_authenticate(initiator)
    response = client.post(url, data)
    assert response.status_code == status_code, response.content
    stage.refresh_from_db()
    assert stage.is_approved
    assert stage.approved_by == initiator
    assert stage.approvement_source == APPROVEMENT_STAGE_APPROVEMENT_SOURCES.api


@patch('ok.approvements.workflow.get_staff_group_member_logins', _mock_group_members)
@patch('ok.notifications.approvements.collect_issues_for_approvements', Mock())
@patch('ok.notifications.approvements.StaffUser.fetch', lambda x: {})
@patch('ok.notifications.base.get_user_to_language', lambda x: {})
def test_approvement_approve_complex(client):
    f.create_waffle_switch('enable_complex_approve_by_responsible')
    initiator = 'initiator'
    approvement = f.ApprovementFactory(
        author=initiator,
        groups=['group_url'],
    )
    parent = f.create_parent_stage(approvement=approvement, need_approvals=1)
    stage = f.create_child_stage(parent)
    f.create_child_stage(parent)

    data = {
        'stages': [stage.id],
    }

    url = reverse('api:approvements:approve', kwargs={'uuid': approvement.uuid})
    client.force_authenticate(initiator)
    response = client.post(url, data)
    assert response.status_code == 200, response.content
    for s in (stage, parent):
        s.refresh_from_db()
        assert s.is_approved
        assert s.approved_by == initiator
        assert s.approvement_source == APPROVEMENT_STAGE_APPROVEMENT_SOURCES.api


def test_approvement_approve_unavailable_for_others(client):
    approvement = f.ApprovementFactory(author='author')
    stage = f.ApprovementStageFactory(
        approvement=approvement,
        approver='approver',
    )

    client.force_authenticate('unknown_person')
    url = reverse('api:approvements:approve', kwargs={'uuid': approvement.uuid})
    response = client.post(url, {'approver': 'approver'})

    assert response.status_code == 403, response.content
    stage.refresh_from_db()
    assert stage.is_approved is None


@pytest.mark.parametrize('initiator,status_code', (
    ('author', 200),
    ('unknown', 403),
))
def test_approvement_close(client, initiator, status_code):
    approvement = f.ApprovementFactory(author='author')
    url = reverse('api:approvements:close', kwargs={'uuid': approvement.uuid})
    client.force_authenticate(initiator)
    response = client.post(url)
    assert response.status_code == status_code, response.content


@pytest.mark.parametrize('initiator,status_code', (
    ('author', 200),
    ('unknown', 403),
))
def test_approvement_suspend(client, initiator, status_code):
    approvement = f.ApprovementFactory(author='author', status=APPROVEMENT_STATUSES.in_progress)
    url = reverse('api:approvements:suspend', kwargs={'uuid': approvement.uuid})
    client.force_authenticate(initiator)
    response = client.post(url)
    assert response.status_code == status_code, response.content


@pytest.mark.parametrize('initiator,status_code', (
    ('author', 200),
    ('unknown', 403),
))
@pytest.mark.parametrize('approvement_status', (
    APPROVEMENT_STATUSES.suspended,
    APPROVEMENT_STATUSES.rejected,
))
@patch('ok.notifications.approvements.collect_issues_for_approvements', Mock())
@patch('ok.notifications.approvements.StaffUser.fetch', lambda x: {})
@patch('ok.notifications.base.get_user_to_language', lambda x: {})
def test_approvement_resume(client, initiator, status_code, approvement_status):
    approvement = f.ApprovementFactory(author='author', status=approvement_status)
    url = reverse('api:approvements:resume', kwargs={'uuid': approvement.uuid})
    client.force_authenticate(initiator)
    response = client.post(url)
    assert response.status_code == status_code, response.content


@pytest.mark.parametrize('initiator,status_code', (
    ('approver', 200),
    ('author', 403),
    ('unknown', 403),
))
@patch('ok.notifications.approvements.collect_issues_for_approvements', Mock())
@patch('ok.notifications.approvements.StaffUser.fetch', lambda x: {})
@patch('ok.notifications.base.get_user_to_language', lambda x: {})
def test_approvement_reject(client, initiator, status_code):
    approvement = f.create_approvement(
        approvers=['approver', 'another_approver'],
        author='author',
    )
    url = reverse('api:approvements:reject', kwargs={'uuid': approvement.uuid})
    client.force_authenticate(initiator)
    response = client.post(url)
    assert response.status_code == status_code, response.content


@pytest.mark.parametrize('initiator,status_code', (
    ('approver', 200),
    ('author', 403),
    ('unknown', 403),
))
@patch('ok.notifications.approvements.collect_issues_for_approvements', Mock())
@patch('ok.notifications.approvements.StaffUser.fetch', lambda x: {})
@patch('ok.notifications.base.get_user_to_language', lambda x: {})
def test_approvement_reject_with_reason(client, initiator, status_code):
    approvement = f.create_approvement(
        approvers=['approver', 'another_approver'],
        author='author',
        disapproval_reasons=['wont_fix'],
    )
    url = reverse('api:approvements:reject', kwargs={'uuid': approvement.uuid})
    client.force_authenticate(initiator)
    response = client.post(url, data={'disapproval_reason': 'wont_fix'})
    assert response.status_code == status_code, response.content


@patch('ok.notifications.approvements.collect_issues_for_approvements', Mock())
@patch('ok.notifications.approvements.StaffUser.fetch', lambda x: {})
@patch('ok.notifications.base.get_user_to_language', lambda x: {})
def test_approvement_reject_with_invalid_reason(client):
    approvement = f.create_approvement(
        approvers=['approver', 'another_approver'],
        author='author',
        disapproval_reasons=['wont_fix'],
    )
    url = reverse('api:approvements:reject', kwargs={'uuid': approvement.uuid})
    client.force_authenticate('approver')
    response = client.post(url, data={'disapproval_reason': 'invalid_reason!!!'})
    assert response.status_code == 400, response.content


@patch('ok.notifications.approvements.collect_issues_for_approvements', Mock())
@patch('ok.notifications.approvements.StaffUser.fetch', lambda x: {})
@patch('ok.notifications.base.get_user_to_language', lambda x: {})
def test_approvement_reject_with_unavailable_reason(client):
    approvement = f.create_approvement(
        approvers=['approver', 'another_approver'],
        author='author',
        disapproval_reasons=['wont_fix'],
    )
    url = reverse('api:approvements:reject', kwargs={'uuid': approvement.uuid})
    client.force_authenticate('approver')
    response = client.post(url, data={'disapproval_reason': 'no_budget'})
    assert response.status_code == 400, response.content


@pytest.mark.parametrize('referer', ['https://st.yandex-team.ru', None])
@pytest.mark.parametrize('ok_session_id', ['some-uniq-id', None])
@pytest.mark.parametrize('flow_name', ['some_wf', None])
@patch('ok.api.core.forms.UserValidationFormMixin._get_valid_users', _mock_staff_users)
@patch('ok.approvements.controllers.execute_flow', Mock(return_value={'data': {'stages': approvement_stages_data()}}))
def test_request_context_saved_in_history(client, approvement_data, referer, ok_session_id, flow_name):
    url = reverse('api:approvements:list-create')
    if flow_name:
        approvement_data['flow_name'] = flow_name
    headers = {}
    if referer:
        headers['HTTP_REFERER'] = referer
    if ok_session_id:
        headers['HTTP_X_OK_SESSION_ID'] = ok_session_id

    response = client.post(url, data=approvement_data, **headers)

    assert response.status_code == 201, response.content
    approvement = Approvement.objects.get(id=response.json()['id'])
    history = approvement.history.first()
    expected = {
        'referer': referer,
        'flow_name': flow_name,
        'ok_session_id': ok_session_id,
    }
    assert history.context == expected


def test_possible_disaprovement_reasons(client):
    url = reverse('api:approvements:possible-disapproval-reasons')
    response = client.get(url)
    assert response.status_code == 200, response.content


@pytest.mark.parametrize(
    'user,returned',
    [
        pytest.param('current', 1, id='current-approver'),
        pytest.param('pending', 0, id='pending-approver'),
        pytest.param('unknown', 0, id='unknown-approver'),
    ],
)
def test_approvements_list(client, user, returned):
    url = reverse('api:approvements:list-create')
    approvement = f.create_approvement(approvers=['current', 'pending'])

    client.force_authenticate(user)
    response = client.get(url)
    assert response.status_code == 200, response.content
    data = response.json()
    assert data['count'] == returned
    if returned:
        assert data['results'][0]['id'] == approvement.id


@patch('ok.api.approvements.views.get_gap_newhire_data', lambda *a, **kw: {'gap': 1, 'newhire': 2})
def test_gap_newhire_counts(client):
    user = 'approver'
    url = reverse('api:approvements:gap-newhire-counts')
    client.force_authenticate(user)

    response = client.get(url)

    assert response.status_code == 200, response.content
    data = response.json()
    assert data['gap_count'] == 1
    assert data['newhire_count'] == 2


def test_approvements_list_approved_stages_not_returned(client):
    url = reverse('api:approvements:list-create')
    user = 'approver'
    my_approvement = f.create_approvement(
        approvers=[user, 'another_approver'],
        author='author',
    )
    f.approve_stage(my_approvement.stages.get(approver=user))

    client.force_authenticate(user)
    response = client.get(url)
    assert response.status_code == 200, response.content
    assert response.json()['count'] == 0


def test_approvement_filter_form(client):
    url = reverse('api:approvements:filter-form')
    response = client.get(url)
    assert response.status_code == 200, response.content


@pytest.mark.parametrize('filter_params, status_code, result_count', (
    ({}, 200, 2),
    ({'queues': 'Q'}, 200, 1),
    ({'queues': 'q'}, 200, 1),
    ({'queues': 'QQQ'}, 400, None),
))
def test_approvement_list_filter_by_queues(client, filter_params, status_code, result_count):
    user = 'approver'
    q1 = f.QueueFactory(name='Q')
    q2 = f.QueueFactory(name='QQ')
    q3 = f.QueueFactory(name='QQQ')
    f.create_approvement(approvers=[user], tracker_queue=q1)
    f.create_approvement(approvers=[user], tracker_queue=q2)
    f.create_approvement(approvers=['another_user'], tracker_queue=q1)
    f.create_approvement(approvers=['another_user'], tracker_queue=q3)

    url = reverse('api:approvements:list-create')
    client.force_authenticate(user)
    response = client.get(url, filter_params)
    assert response.status_code == status_code, response.content
    if status_code == 200:
        response_data = response.json()
        assert response_data['count'] == result_count


@pytest.mark.parametrize('stage_statuses, result_count', (
    pytest.param(True, 1, id='default'),
    pytest.param(False, 2, id='default-with-disabled-switch'),
    pytest.param([APPROVEMENT_STAGE_STATUSES.current], 1, id='current'),
    pytest.param([APPROVEMENT_STAGE_STATUSES.pending], 1, id='pending'),
    pytest.param(list(APPROVEMENT_STAGE_ACTIVE_STATUSES._db_values), 2, id='active'),
))
def test_approvement_list_filter_by_stage_statuses(client, stage_statuses, result_count):
    user = 'user'
    f.create_approvement(approvers=[user, 'another_user'])
    f.create_approvement(approvers=['another_user', user])

    url = reverse('api:approvements:list-create')
    client.force_authenticate(user)
    params = {}
    if isinstance(stage_statuses, list):
        params['stage_statuses'] = stage_statuses
    with override_switch('enable_stage_status_current', bool(stage_statuses)):
        response = client.get(url, params)
    assert response.status_code == 200, response.content
    response_data = response.json()
    assert response_data['count'] == result_count


@pytest.mark.parametrize('roles, result_count', (
    ([], 3),
    ([APPROVEMENT_ROLES.approver], 1),
    ([APPROVEMENT_ROLES.author], 1),
    ([APPROVEMENT_ROLES.responsible], 1),
    ([APPROVEMENT_ROLES.responsible, APPROVEMENT_ROLES.approver], 2),
    ([APPROVEMENT_ROLES.author, APPROVEMENT_ROLES.responsible, APPROVEMENT_ROLES.approver], 3),
))
@pytest.mark.skip('OK-1015. Тест не актуален для списка входящих согласований')
def test_approvement_list_filter_by_roles(client, roles, result_count):
    user = 'user'
    f.create_approvement(author=user)
    f.create_approvement(approvers=[user])
    membership = f.GroupMembershipFactory(login=user)
    approvement = f.create_approvement(groups=[membership.group.url])
    approvement.approvement_groups.create(group=membership.group)

    url = reverse('api:approvements:list-create')
    client.force_authenticate(user)
    response = client.get(url, {'roles': roles})
    assert response.status_code == 200, response.content
    response_data = response.json()
    assert response_data['count'] == result_count


@pytest.mark.parametrize('sort, approvement_idx', (
    ('modified', 1),
    ('-modified', 0),
    (None, 1),
))
def test_approvement_list_sort(client, sort, approvement_idx):
    user = 'approver'
    approvements = [
        f.create_approvement(approvers=[user]),
        f.create_approvement(approvers=[user]),
    ]
    approvements[0].modified = timezone.now()
    approvements[0].save()

    url = reverse('api:approvements:list-create')
    client.force_authenticate(user)
    params = {}
    if sort is not None:
        params['sort'] = sort
    response = client.get(url, params)
    assert response.status_code == 200, response.content
    response_data = response.json()
    assert response_data['results'][0]['id'] == approvements[approvement_idx].id
