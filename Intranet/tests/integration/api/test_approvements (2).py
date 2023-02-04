import pytest

from unittest.mock import Mock, patch

from constance.test import override_config
from django.db import IntegrityError
from django.urls import reverse
from django.utils import timezone
from django.utils.translation import ugettext_lazy as __
from ids.exceptions import BackendError
from startrek_client.exceptions import NotFound, Forbidden
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
from ok.scenarios.choices import SCENARIO_STATUSES
from ok.tracker.queues import get_queue_name
from ok.utils.strings import str_to_md5

from tests import factories as f
from tests.utils.approvements import generate_stages_data as _


pytestmark = pytest.mark.django_db


flow_name = 'some_wf'


def _get_form_stages(response):
    """
    Извлекает предзаполненные данные по стадиям из ответа ручки формы
    """
    return response.json()['data']['stages']['value']


def _get_form_substages(stage):
    """
    Извлекает дочерние стадии из одной стадии в формате формы
    """
    return stage['value']['stages']['value']


def _get_form_approver(stage):
    """
    Извлекает логин согласующего из одной стадии в формате формы
    """
    return stage['value']['approver']['value']['login']


@pytest.fixture
def approvement_stages_data():
    denis_an = f.UserFactory(username='denis-an')
    ktussh = f.UserFactory(username='ktussh')
    qazaq = f.UserFactory(username='qazaq')
    tmalikova = f.UserFactory(username='tmalikova')
    return [
        {'approver': denis_an.username},
        {'stages': [{'approver': qazaq.username}, {'approver': tmalikova.username}]},
        {'approver': ktussh.username},
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
def approvement_data(approvement_base_data, approvement_stages_data):
    approvement_base_data['stages'] = approvement_stages_data
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


@pytest.mark.parametrize('data, status_code', (
    ({}, 200),
    ({'text': 'text', 'type': 'general'}, 200),
    ({'scenario': 'non-existent'}, 400),
    ({'object_id': 'invalid'}, 400),
))
def test_approvement_create_validate(client, data, status_code):
    url = reverse('api:approvements:create-validate')
    response = client.post(url, data=data)
    assert response.status_code == status_code, response.content


def test_approvement_create_with_scenario(client, approvement_data):
    scenario = f.ScenarioFactory(approvement_data={
        'text': 'Text from scenario',
        'groups': [f.GroupFactory().url],
    })
    approvement_data['scenario'] = scenario.slug
    approvement_data['groups'] = []
    approvement_data.pop('text')
    url = reverse('api:approvements:list-create')
    response = client.post(url, data=approvement_data)

    assert response.status_code == 201, response.content
    response_data = response.json()
    approvement = Approvement.objects.get(id=response.json()['id'])
    assert approvement.scenario == scenario
    assert approvement.text == 'Text from scenario'
    assert approvement.groups == []
    assert response_data['scenario'] == {
        'slug': scenario.slug,
        'name': scenario.name,
    }


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


def test_approvement_create_normalize_stages(client, approvement_data):
    """
    Проверяет, что при создании согласования стадии нормализуются,
    поэтому сабмит проходит без ошибок:
    – из согласования выкидываются пустые листовые стадии;
    - групповая стадия с одной дочерней превращается в плоскую;
    """
    dmirain = f.UserFactory(username='dmirain')
    dmitrypro = f.UserFactory(username='dmitrypro')
    kiparis = f.UserFactory(username='kiparis')
    zivot = f.UserFactory(username='zivot')
    url = reverse('api:approvements:list-create')
    approvement_data['stages'] = [
        {'approver': '', 'stages': [], 'need_approvals': 1},
        {'approver': dmirain.username, 'stages': [], 'need_approvals': 1},
        _('', '', dmitrypro.username, n=1),
        _('', kiparis.username, zivot.username, n=1),
        _('', n=1),
    ]

    response = client.post(url, data=approvement_data)

    assert response.status_code == 201, response.content


@patch('ok.approvements.tasks.close_or_restore_approvement_task.delay')
def test_approvement_create_comment(patched_task, client, approvement_data):
    url = reverse('api:approvements:list-create')
    approvement_data['create_comment'] = True
    response = client.post(url, data=approvement_data)

    assert response.status_code == 201, response.content
    patched_task.assert_called_once()


def test_approvement_create_with_disapproval_reasons(client, approvement_data):
    url = reverse('api:approvements:list-create')
    approvement_data['disapproval_reasons'] = ['no_budget', 'wont_fix']
    response = client.post(url, data=approvement_data)
    assert response.status_code == 201, response.content


def test_approvement_create_with_invalid_disapproval_reasons(client, approvement_data):
    url = reverse('api:approvements:list-create')
    approvement_data['disapproval_reasons'] = ['invalid reason!!!']
    response = client.post(url, data=approvement_data)
    assert response.status_code == 400, response.content


@pytest.mark.parametrize('is_parallel', (True, False))
def test_approvement_create_with_flow(client, approvement_flow_data, approvement_stages_data,
                                      is_parallel, monkeypatch):
    url = reverse('api:approvements:list-create')
    approvement_flow_data['is_parallel'] = is_parallel

    monkeypatch.setattr(
        target=controllers,
        name='execute_flow',
        value=lambda *a, **kw: {'data': {'stages': approvement_stages_data}},
    )

    response = client.post(url, data=approvement_flow_data)
    assert response.status_code == 201, response.content


@pytest.mark.parametrize('is_parallel', (True, False))
def test_approvement_create_with_nonexisting_flow(client, approvement_flow_data, is_parallel, monkeypatch):
    url = reverse('api:approvements:list-create')
    approvement_flow_data['is_parallel'] = is_parallel

    def raising_fu(*a, **kw):
        raise flow_executor.FlowNotFound(approvement_flow_data['flow_name'])

    monkeypatch.setattr(controllers, 'execute_flow', raising_fu)

    response = client.post(url, data=approvement_flow_data)
    assert response.status_code == 404, response.content


@pytest.mark.parametrize('is_parallel', (True, False))
def test_approvement_create_with_flow_no_returning_stages(client, approvement_flow_data, is_parallel, monkeypatch):
    url = reverse('api:approvements:list-create')
    approvement_flow_data['is_parallel'] = is_parallel

    monkeypatch.setattr(controllers, 'execute_flow', lambda *a, **kw: {'data': {}, 'detail': {}})

    response = client.post(url, data=approvement_flow_data)
    assert response.status_code == 400, response.content


def test_approvement_flow_dropped(client, approvement_flow_data, monkeypatch):
    url = reverse('api:approvements:list-create')

    monkeypatch.setattr(controllers, 'execute_flow', lambda *a, **kw:  {
        'data': {'stages': []},
        'detail': {'error': 'Flow dropped'},
    })

    response = client.post(url, data=approvement_flow_data)
    assert response.status_code == 200, response.content['detail'] == {'error': 'Flow dropped'}


def test_approvement_flow_table_flow_failed(client, approvement_flow_data, monkeypatch):
    url = reverse('api:approvements:list-create')

    monkeypatch.setattr(controllers, 'execute_flow', lambda *a, **kw:  {
        'data': {'stages': []},
        'detail': {'error': 'Table flow call failed',  'code': 429},
    })

    response = client.post(url, data=approvement_flow_data)
    assert response.status_code == 429, response.content['detail'] == {'error': 'Table flow call failed'}


@pytest.mark.parametrize('is_parallel', (True, False))
def test_approvement_create_form_with_flow(client, approvement_base_data, is_parallel, monkeypatch):
    stages_data = _('a', _('b1', 'b2'), 'c')['stages']
    f.create_users('a', 'b1', 'b2', 'c')

    url = reverse('api:approvements:create-form')
    data = approvement_base_data
    data['is_parallel'] = is_parallel
    data['flow_name'] = flow_name
    data['flow_context.asd'] = 10

    monkeypatch.setattr(
        target=controllers,
        name='execute_flow',
        value=lambda *a, **kw: {'data': {'stages': stages_data}},
    )

    response = client.get(url, data=data)
    assert response.status_code == 200, response.content
    stages = _get_form_stages(response)
    assert len(stages) == 3
    b_stages = _get_form_substages(stages[1])
    assert len(b_stages) == 2
    assert _get_form_approver(stages[0]) == 'a'
    assert _get_form_approver(b_stages[0]) == 'b1'
    assert _get_form_approver(b_stages[1]) == 'b2'
    assert _get_form_approver(stages[2]) == 'c'


def test_approvement_create_form_with_flow_unknown_approver(client, monkeypatch,
                                                            approvement_base_data):
    stages_data = _('a', _('b1', 'x', 'b2'), 'c', 'x', _('x', 'd1'))['stages']
    f.create_users('a', 'b1', 'b2', 'c', 'd1')

    url = reverse('api:approvements:create-form')
    data = approvement_base_data
    data['flow_name'] = flow_name

    monkeypatch.setattr(
        target=controllers,
        name='execute_flow',
        value=lambda *a, **kw: {'data': {'stages': stages_data}},
    )

    response = client.get(url, data=data)
    assert response.status_code == 200, response.content
    stages = _get_form_stages(response)
    assert len(stages) == 4
    b_stages = _get_form_substages(stages[1])
    d_stages = _get_form_substages(stages[3])
    assert len(b_stages) == 2
    assert len(d_stages) == 1
    assert _get_form_approver(stages[0]) == 'a'
    assert _get_form_approver(b_stages[0]) == 'b1'
    assert _get_form_approver(b_stages[1]) == 'b2'
    assert _get_form_approver(stages[2]) == 'c'
    assert _get_form_approver(d_stages[0]) == 'd1'


def test_approvement_create_form_callback_url(client, approvement_base_data):
    url = reverse('api:approvements:create-form')
    data = approvement_base_data
    data['callback_url'] = 'http://1.ru'

    response = client.get(url, data=data)
    assert response.status_code == 200, response.content
    assert response.json()['data']['callback_url']['value'] == 'http://1.ru'


@pytest.mark.parametrize('tvm_id', (1, None))
def test_approvement_create_callback_url(client, approvement_base_data, approvement_stages_data,
                                         monkeypatch, tvm_id):
    url = reverse('api:approvements:list-create')
    data = approvement_base_data
    data['callback_url'] = 'http://1.ru'
    data['flow_name'] = flow_name

    if tvm_id:
        data['tvm_id'] = tvm_id

    stages_data = approvement_stages_data

    monkeypatch.setattr(controllers, 'execute_flow', lambda *a, **kw: {'data': {'stages': stages_data}})

    response = client.post(url, data=data)
    assert response.status_code == 201, response.content
    assert response.json()['callback_url'] == 'http://1.ru'


def test_approvement_create_form_with_flow_make_autoapprovable(client, approvement_base_data,
                                                               approvement_stages_data,
                                                               monkeypatch):
    url = reverse('api:approvements:list-create')
    data = approvement_base_data
    data['flow_name'] = flow_name
    data['flow_context.asd'] = 10
    stages_data = approvement_stages_data

    monkeypatch.setattr(controllers, 'execute_flow',
                        lambda *a, **kw: {'data': {'stages': stages_data, 'is_auto_approving': True}})

    response = client.post(url, data=data)
    assert response.status_code == 201, response.content

    assert response.json()['is_auto_approving'] is True



@pytest.mark.parametrize('status', (
    APPROVEMENT_STATUSES.in_progress,
    APPROVEMENT_STATUSES.suspended,
))
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
def test_approvement_create_not_duplicate_integrity_error(mocked_create, client, approvement_data):
    """
    В already_exists превращаются только ошибки про дубликаты данных,
    а всё остальное так и остаётся пятисотымми
    """
    mocked_create.side_effect = IntegrityError()

    url = reverse('api:approvements:list-create')
    with pytest.raises(IntegrityError):
        client.post(url, data=approvement_data)


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


def test_approvement_create_with_external_users(client, approvement_base_data):
    """
    Пробуем создать согласование с внешними пользователями без доступа в очередь
    """
    external_user = f.UserFactory(affiliation='external')
    data = approvement_base_data
    data['stages'] = [{'approver': external_user.username}]
    f.QueueFactory.create(
        name=get_queue_name(data['object_id']),
        allow_externals=False,
    )
    url = reverse('api:approvements:list-create')
    response = client.post(url, data=data)
    assert response.status_code == 400, response.content
    assert response.data['errors']['stages'][0]['code'] == 'external_users'


@pytest.mark.parametrize('author, status_code', (
    ('internal', 201),
    ('external', 400),
    (None, 201),
))
def test_approvement_create_with_external_author(author, status_code, client, approvement_data):
    """
    Пробуем создать согласование с внешними пользователями без доступа в очередь
    """
    f.UserFactory(username='internal')
    external_user = f.UserFactory(username='external', affiliation='external')
    data = approvement_data
    if author:
        data['author'] = author
    f.QueueFactory.create(
        name=get_queue_name(data['object_id']),
        allow_externals=False,
    )
    url = reverse('api:approvements:list-create')
    client.force_authenticate(external_user)
    response = client.post(url, data=data)
    assert response.status_code == status_code, response.content
    if status_code == 400:
        assert response.data['errors']['author'][0]['code'] == 'external_users'


@patch('ok.api.approvements.validators.get_user_tracker_client')
def test_approvement_create_with_nonexistent_object_id(mocked_action, client, approvement_data):
    """
    Пробуем создать согласование с несуществующим object_id
    """
    mocked_action().issues.get.side_effect = NotFound(Mock())

    url = reverse('api:approvements:list-create')
    response = client.post(url, data=approvement_data)
    assert response.status_code == 400, response.content
    assert response.data['errors'][''][0]['code'] == 'issue_does_not_exist'


@pytest.mark.parametrize('user, author, is_flag_active, status_code', (
    ('allowed', 'allowed', False, 201),
    ('allowed', 'not_allowed', True, 201),
    ('allowed', 'not_allowed', False, 201),
    ('not_allowed', 'allowed', True, 400),
    ('not_allowed', 'not_allowed', False, 400),
))
@patch('ok.api.approvements.validators.get_user_tracker_client')
@override_config(APPROVEMENT_AUTHORS_WHITELIST_BY_TRACKER_QUEUE='{"JOB": ["allowed"]}')
def test_approvement_create_without_access_to_issue(mocked_action, user, author, is_flag_active,
                                                    status_code, client, approvement_data):
    mocked_action().issues.get.side_effect = Forbidden(Mock())
    f.create_waffle_flag('can_set_approvement_author', is_flag_active)
    f.UserFactory(username=author)
    approvement_data['author'] = author

    client.force_authenticate(user)
    url = reverse('api:approvements:list-create')
    response = client.post(url, data=approvement_data)
    assert response.status_code == status_code, response.content
    if status_code == 400:
        assert response.data['errors'][''][0]['code'] == 'permission_denied'


@pytest.mark.parametrize('object_id, error', (
    (None, {'object_id': [{'code': 'required'}]}),
    ('', {'object_id': [{'code': 'required'}]}),
    ('JOB', {'': [{'code': 'issue_does_not_exist'}]}),
    ('JOB_1-23', {'': [{'code': 'issue_does_not_exist'}]}),
))
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
def test_do_not_check_tracker_fields_for_non_tracker_approvement(patched_validate_issue_key, client, approvement_data):
    approvement_data['type'] = APPROVEMENT_TYPES.general
    approvement_data['object_id'] = 'anyformatobjectid'

    response = client.post(reverse('api:approvements:list-create'), data=approvement_data)

    assert response.status_code == 201, response.content
    approvement = Approvement.objects.get(id=response.json()['id'])
    assert approvement.type == APPROVEMENT_TYPES.general
    assert approvement.object_id == approvement_data['object_id']
    patched_validate_issue_key.assert_not_called()


def test_approvement_create_with_internal_users(client, approvement_data):
    f.QueueFactory.create(name=get_queue_name(approvement_data['object_id']), allow_externals=False)
    url = reverse('api:approvements:list-create')
    response = client.post(url, data=approvement_data)
    assert response.status_code == 201, response.content


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
def test_approvement_create_with_defined_author(client, can_set_approvement_author, approvement_data):
    f.create_waffle_flag('can_set_approvement_author', can_set_approvement_author)
    defined_author = f.UserFactory()
    approvement_data['author'] = defined_author.username
    user = f.UserFactory(username='user')

    client.force_authenticate(user)
    url = reverse('api:approvements:list-create')
    response = client.post(url, data=approvement_data)

    assert response.status_code == 201, response.content
    result = response.json()
    expected_author = approvement_data['author'] if can_set_approvement_author else user.username
    assert result['author'] == expected_author


@pytest.mark.parametrize('stage_data, status_code, need_approvals', (
    ({'is_with_deputies': True}, 201, 1),
    ({'is_with_deputies': True, 'need_all': True}, 400, None),
    ({'is_with_deputies': True, 'need_approvals': 2}, 400, None),
    ({'need_all': True}, 201, 3),
    ({'need_approvals': 2}, 201, 2),
))
def test_approvement_create_with_deputies(client, approvement_data, stage_data, status_code,
                                          need_approvals):
    stage = dict(
        {'stages': [{'approver': 'ktussh'}, {'approver': 'denis-an'}, {'approver': 'qazaq'}]},
        **stage_data,
    )
    approvement_data['stages'].append(stage)

    user = f.UserFactory(username='user')
    client.force_authenticate(user)
    url = reverse('api:approvements:list-create')
    response = client.post(url, data=approvement_data)

    assert response.status_code == status_code, response.content
    if need_approvals is not None:
        result_stage = response.json()['stages'][-1]
        assert result_stage['need_approvals'] == need_approvals


@pytest.mark.parametrize('is_switch_active', (True, False))
@pytest.mark.parametrize('group_data, is_valid', (
    ({}, True),
    ({'is_deleted': True}, False),
    ({'staff_id': None}, False),
))
def test_approvement_create_with_deleted_groups(client, approvement_data, group_data, is_valid,
                                                is_switch_active):
    f.create_waffle_switch(name='enable_approvement_groups_validation', active=is_switch_active)
    group = f.GroupFactory(**group_data)
    approvement_data['groups'] = [group.url]

    user = f.UserFactory(username='user')
    client.force_authenticate(user)
    url = reverse('api:approvements:list-create')
    response = client.post(url, data=approvement_data)

    status_code = 201 if is_valid or not is_switch_active else 400
    assert response.status_code == status_code, response.content
    if status_code == 400:
        response_data = response.json()
        error = response_data['errors']['groups'][0]
        assert error['code'] == 'group_does_not_exist'


@pytest.mark.parametrize('is_multiplesuggest_active', (True, False))
@pytest.mark.parametrize('is_validation_active', (True, False))
def test_approvement_create_with_unknown_groups(client, approvement_data, is_validation_active,
                                                is_multiplesuggest_active):
    f.create_waffle_switch(
        name='enable_approvement_groups_validation',
        active=is_validation_active,
    )
    approvement_data['groups'] = ['unknown_group']

    user = f.UserFactory(username='user')
    client.force_authenticate(user)
    url = reverse('api:approvements:list-create')
    ctx = override_switch(
        'enable_groups_multiplesuggest_for_approvements',
        is_multiplesuggest_active,
    )
    with ctx:
        response = client.post(url, data=approvement_data)

    status_code = 201 if not is_validation_active and not is_multiplesuggest_active else 400
    assert response.status_code == status_code, response.content
    if status_code == 400:
        response_data = response.json()
        error = response_data['errors']['groups'][0]
        expected_error = 'invalid_choice' if is_multiplesuggest_active else 'group_does_not_exist'
        assert error['code'] == expected_error


def test_approvement_detail(client):
    approvement = f.ApprovementFactory()
    url = reverse('api:approvements:detail', kwargs={'uuid': approvement.uuid})
    response = client.get(url)
    assert response.status_code == 200, response.content


@pytest.mark.parametrize('initiator, is_text_hidden', (
    ('author', False),
    ('approver', False),
    ('group_url_member', False),
    ('unknown', True),
))
def test_approvement_detail_hide_approvement_text_for_observer(client, initiator, is_text_hidden):
    f.create_waffle_flag('hide_approvement_text_for_observer', True)
    approvement = f.ApprovementFactory(
        author='author',
        groups=['group_url'],
    )
    f.ApprovementStageFactory(
        approvement=approvement,
        approver='approver',
    )

    issue_url = 'https://st.yandex-team.ru/OK-1421'
    question = __('Why can\'t I see the text?')
    hidden_text = f'** \*\*\*\*\*\*\*\*\*\* **\n(({issue_url} {question}))'
    expected_text = hidden_text if is_text_hidden else approvement.text

    url = reverse('api:approvements:detail', kwargs={'uuid': approvement.uuid})

    client.force_authenticate(initiator)
    response = client.get(url)
    assert response.status_code == 200, response.content
    data = response.json()
    assert data['text'] == expected_text


def test_approvement_list_query_count(client, django_assert_num_queries):
    f.create_waffle_flag('hide_approvement_text_for_observer', True)

    login = 'user'
    for _ in range(4):
        membership = f.GroupMembershipFactory(login=login)
        approvement = f.ApprovementFactory(groups=[membership.group])
        f.ApprovementStageFactory(
            approvement=approvement,
            approver=login,
            status=APPROVEMENT_STAGE_ACTIVE_STATUSES.current,
        )

    url = reverse('api:approvements:list-create')

    client.force_authenticate(login)
    # 2 - savepoint
    # 7 - prefetch stages, history, tracker_queue, scenario, approvement_groups, group, memberships
    # 1 - waffle_flag
    # 2 - count и select списка
    # Если запускать тест отдельно, то еще 1 - django_content_type
    with django_assert_num_queries(12):
        response = client.get(url)
    assert response.status_code == 200, response.content


def test_approvement_detail_with_scenario(client):
    scenario = f.ScenarioFactory()
    approvement = f.ApprovementFactory(scenario=scenario)
    url = reverse('api:approvements:detail', kwargs={'uuid': approvement.uuid})
    response = client.get(url)
    assert response.status_code == 200, response.content
    response_data = response.json()
    assert response_data['scenario'] == {
        'slug': scenario.slug,
        'name': scenario.name,
    }


def test_approvement_create_form(client, approvement_data):
    url = reverse('api:approvements:create-form')
    response = client.get(url, approvement_data)
    assert response.status_code == 200, response.content


@patch('ok.utils.staff.get_staff_iter', side_effect=BackendError('Staff-api 500'))
def test_approvement_create_form_200_on_staff_500(mocked_staff_iter, client, approvement_data):
    """
    Ошибка предзаполнения формы должна обрабатываться тихо.
    400/500 быть не должно.
    """
    approvement_data['groups'] = ['group']
    url = reverse('api:approvements:create-form')
    response = client.get(url, approvement_data)
    assert response.status_code == 200, response.content
    # В реальности у нас не осталось сейчас походов в staff-api в ручке формы создания,
    # но если появятся, мы хотим об этом знать – тест пока остаётся
    assert not mocked_staff_iter.called


def test_approvement_create_form_preset(client):
    """
    Предустановленные параметры формы корректно работают
    """
    f.create_users('u1', 'u2')
    data = {
        'text': 'test-text',
        'groups': [f.GroupFactory().url, f.GroupFactory().url],
        # Для обратной совместимости логины согласующих прокидываются
        # в поле users, а не stages. При этом в предустановленных
        # параметрах они запишутся как в users, так и в stages
        'users': ['u1', 'u2'],
    }

    url = reverse('api:approvements:create-form')
    response = client.get(url, data)

    assert response.status_code == 200, response.content
    preset = response.json()['data']

    assert preset['text']['value'] == data['text']
    groups = preset['groups']['value']
    assert groups == data['groups']

    stages_users = [_get_form_approver(s) for s in preset['stages']['value']]
    assert stages_users == data['users']


@pytest.mark.parametrize('query_text, scenario_text, expected_text', (
    ('Text', 'Default scenario text', 'Text'),
    (None, 'Default scenario text', 'Default scenario text'),
))
@pytest.mark.parametrize('scenario_status', SCENARIO_STATUSES._db_values)
def test_approvement_create_form_with_scenario(client, scenario_status, query_text, scenario_text,
                                               expected_text):
    f.ScenarioFactory(
        slug='test',
        status=scenario_status,
        approvement_data={'text': scenario_text},
    )
    data = {'scenario': 'test'}
    if query_text is not None:
        data['text'] = query_text

    url = reverse('api:approvements:create-form')
    response = client.get(url, data)

    assert response.status_code == 200, response.content
    form_data = response.json()['data']
    assert form_data['text']['value'] == expected_text


@pytest.mark.parametrize('need_all', (True, False))
def test_approvement_create_form_with_scenario_stages(client, need_all):
    f.create_users('a', 'b1', 'b2', 'c')

    # Фронт работает с параметром need_all, поэтому убеждаемся, что именно при использовании его
    # всё работает корректно, и в форму подставляется то, что выбрано
    group_stage = _('b1', 'b2')
    group_stage.pop('need_approvals')
    group_stage['need_all'] = need_all

    approvement_data = _('a', group_stage, 'c')
    f.ScenarioFactory(slug='with-stages', approvement_data=approvement_data)

    url = reverse('api:approvements:create-form')
    response = client.get(url, {'scenario': 'with-stages'})

    assert response.status_code == 200, response.content
    stages = _get_form_stages(response)
    assert len(stages) == 3
    assert stages[1]['value']['need_all']['value'] is need_all
    b_stages = _get_form_substages(stages[1])
    assert len(b_stages) == 2
    assert _get_form_approver(stages[0]) == 'a'
    assert _get_form_approver(b_stages[0]) == 'b1'
    assert _get_form_approver(b_stages[1]) == 'b2'
    assert _get_form_approver(stages[2]) == 'c'


@pytest.mark.parametrize('data, expected_scenarios', (
    ({}, {''}),
    ({'object_id': 'KNOWNQUEUE-1'}, {'', 'relevant'}),
    ({'object_id': 'EXISTING-1'}, {''}),
    ({'object_id': 'invalid', 'type': APPROVEMENT_TYPES.tracker}, {''}),
    ({'object_id': 'invalid', 'type': APPROVEMENT_TYPES.general}, {''}),
    ({'scenario': 'irrelevant'}, {'', 'irrelevant'}),
    ({'scenario': 'non-existent'}, {''}),
    ({'scenario': 'invalid', 'object_id': 'KNOWNQUEUE-1'}, {'', 'relevant'}),
    ({'object_id': 'KNOWNQUEUE-1', 'scenario': 'irrelevant'}, {'', 'relevant', 'irrelevant'}),
))
def test_approvement_create_standalone_form(client, data, expected_scenarios):
    known_queue = f.QueueFactory(name='KNOWNQUEUE')
    f.ScenarioTrackerMacroFactory(scenario__slug='relevant', tracker_queue=known_queue)
    f.ScenarioTrackerMacroFactory(scenario__slug='irrelevant')
    f.ScenarioTrackerMacroFactory(
        scenario__slug='archived',
        scenario__status=SCENARIO_STATUSES.archived,
        tracker_queue=known_queue,
    )
    f.create_approvement(object_id='EXISTING-1')

    data |= {'is_standalone': 'true'}
    url = reverse('api:approvements:create-form')
    response = client.get(url, data)

    assert response.status_code == 200, response.content
    response_data = response.json()
    form_data = response_data['data']
    form_structure = response_data['structure']
    assert form_data['object_id']['value'] == data.get('object_id', '')
    assert form_data['type']['value'] == data.get('type', APPROVEMENT_TYPES.tracker)
    assert form_data['create_comment']['value'] is True
    scenarios = {i['value'] for i in form_structure['scenario']['choices']}
    assert scenarios == expected_scenarios


@pytest.mark.parametrize('initiator,status_code', (
    ('author', 200),
    ('approver', 200),
    ('group_url_member', 200),
))
@patch('ok.notifications.approvements.collect_issues_for_approvements', Mock())
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


@patch('ok.notifications.approvements.collect_issues_for_approvements', Mock())
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
@patch('ok.approvements.controllers.execute_flow')
def test_request_context_saved_in_history(mocked_execute_flow, client, approvement_data,
                                          approvement_stages_data, referer, ok_session_id,
                                          flow_name):
    mocked_execute_flow.return_value = {'data': {'stages': approvement_stages_data}}
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


@pytest.mark.parametrize('user, status, returned', (
    ('author', APPROVEMENT_STATUSES.in_progress, 1),
    ('author', APPROVEMENT_STATUSES.suspended, 0),
    ('author', APPROVEMENT_STATUSES.closed, 0),
    ('responsible', APPROVEMENT_STATUSES.in_progress, 0),
    ('approver', APPROVEMENT_STATUSES.in_progress, 0),
))
def test_approvement_list_for_responsibles(client, user, status, returned):
    membership = f.GroupMembershipFactory(login='responsible')
    active_approvement = f.create_approvement(
        approvers=['approver'],
        author='author',
        status=status,
    )
    active_approvement.approvement_groups.create(group=membership.group)

    url = reverse('api:approvements:responsibles-list')
    client.force_authenticate(user)
    response = client.get(url)
    assert response.status_code == 200, response.content
    data = response.json()
    assert data['count'] == returned
    if returned:
        assert data['results'][0]['id'] == active_approvement.id


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


def test_approvement_for_responsibles_filter_form(client):
    url = reverse('api:approvements:responsibles-list-filter-form')
    response = client.get(url)
    assert response.status_code == 200, response.content


@pytest.mark.parametrize('filter_params, status_code, result_count', (
    ({}, 200, 2),
    ({'queues': 'Q'}, 200, 1),
    ({'queues': 'q'}, 200, 1),
    ({'queues': 'QQQ'}, 400, None),
))
@pytest.mark.parametrize('is_for_responsible_view', (True, False))
def test_approvement_list_filter_by_queues(client, is_for_responsible_view, filter_params,
                                           status_code, result_count):
    approver = 'approver'
    author = 'author'
    q1 = f.QueueFactory(name='Q')
    q2 = f.QueueFactory(name='QQ')
    q3 = f.QueueFactory(name='QQQ')
    f.create_approvement(approvers=[approver], tracker_queue=q1, author=author)
    f.create_approvement(approvers=[approver], tracker_queue=q2, author=author)
    f.create_approvement(approvers=['another_user'], tracker_queue=q1, author='another_author')
    f.create_approvement(approvers=['another_user'], tracker_queue=q3, author='another_author')

    if is_for_responsible_view:
        url = reverse('api:approvements:responsibles-list')
        client.force_authenticate(author)
    else:
        url = reverse('api:approvements:list-create')
        client.force_authenticate(approver)
    response = client.get(url, filter_params)
    assert response.status_code == status_code, response.content
    if status_code == 200:
        response_data = response.json()
        assert response_data['count'] == result_count


@pytest.mark.parametrize('stage_statuses, result_count', (
    pytest.param(None, 1, id='default'),
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
    if stage_statuses is not None:
        params['stage_statuses'] = stage_statuses
    response = client.get(url, params)
    assert response.status_code == 200, response.content
    response_data = response.json()
    assert response_data['count'] == result_count


@pytest.mark.parametrize('filter_params, status_code, result_count', (
    ({}, 200, 3),
    ({'scenarios': 'my_scenario_1'}, 200, 1),
    ({'scenarios': 'scenario'}, 400, None),
))
@pytest.mark.parametrize('is_for_responsible_view', (True, False))
def test_approvement_list_filter_by_scenarios(client, is_for_responsible_view, filter_params,
                                              status_code, result_count):
    approver = 'approver'
    author = 'author'
    my_scenario_1 = f.ScenarioFactory(slug='my_scenario_1')
    my_scenario_2 = f.ScenarioFactory(slug='my_scenario_2')
    scenario = f.ScenarioFactory(slug='scenario')
    f.create_approvement(approvers=[approver], scenario=None, author=author)
    f.create_approvement(approvers=[approver], scenario=my_scenario_1, author=author)
    f.create_approvement(approvers=[approver], scenario=my_scenario_2, author=author)
    f.create_approvement(approvers=['another_user'], scenario=scenario, author='another_author')

    if is_for_responsible_view:
        url = reverse('api:approvements:responsibles-list')
        client.force_authenticate(author)
    else:
        url = reverse('api:approvements:list-create')
        client.force_authenticate(approver)
    response = client.get(url, filter_params)
    assert response.status_code == status_code, response.content
    if status_code == 200:
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
@pytest.mark.parametrize('is_for_responsible_view', (True, False))
def test_approvement_list_sort(client, is_for_responsible_view, sort, approvement_idx):
    approver = 'approver'
    author = 'author'
    approvements = [
        f.create_approvement(approvers=[approver], author=author),
        f.create_approvement(approvers=[approver], author=author),
    ]
    approvements[0].modified = timezone.now()
    approvements[0].save()

    if is_for_responsible_view:
        url = reverse('api:approvements:responsibles-list')
        client.force_authenticate(author)
    else:
        url = reverse('api:approvements:list-create')
        client.force_authenticate(approver)
    params = {}
    if sort is not None:
        params['sort'] = sort
    response = client.get(url, params)
    assert response.status_code == 200, response.content
    response_data = response.json()
    assert response_data['results'][0]['id'] == approvements[approvement_idx].id


@patch('ok.approvements.middleware.create_comment_task.delay')
@pytest.mark.parametrize('is_debug', (True, False))
def test_create_approvement_debug(create_comment_mock, is_debug, client, approvement_data):
    url = reverse('api:approvements:list-create')
    headers = {'HTTP_X_OK_TRACKER_DEBUG': 'KEY-1'} if is_debug else {}

    response = client.post(url, data={}, **headers)

    assert response.status_code == 400, response.content
    if is_debug:
        create_comment_mock.assert_called()
    else:
        create_comment_mock.assert_not_called()
