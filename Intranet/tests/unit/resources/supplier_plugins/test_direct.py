from textwrap import dedent
from unittest import mock

import pretend
import pytest
from django.core.urlresolvers import reverse
from mock import call, patch

from plan.resources.suppliers.base import SupplierPlugin

from plan.resources.api.base import make_signature
from plan.resources.models import ServiceResource, Resource
from common import factories
from utils import Response
from unit.resources.test_request import fake_form_metadata

pytestmark = pytest.mark.django_db


@pytest.fixture
def data(db, owner_role, staff_factory):
    staff = staff_factory('full_access')
    manager = factories.StaffFactory(user=factories.UserFactory(username='cool-manager'))
    service = factories.ServiceFactory(owner=staff)
    factories.ServiceMemberFactory(service=service, role=owner_role, staff=staff)
    resource_type = factories.ResourceTypeFactory(
        code='direct_client',
        import_plugin='generic',
        import_link='http://yandex.ru/?consumer=abc',
        supplier_plugin='direct',
        has_automated_grant=True,
        form_id=1,
        form_handler=dedent('''
        from plan.resources.handlers.financial_resources.direct.forward import process_form_forward
        result = process_form_forward(data, form_metadata)
        '''),
        form_back_handler=dedent('''
        from plan.resources.handlers.financial_resources.direct.backward import process_form_backward
        result = process_form_backward(attributes, form_metadata)
        '''),
        has_multiple_consumers=True,
    )
    resource_type.consumer_roles.add(owner_role)
    fixture = pretend.stub(
        resource_type=resource_type,
        service=service,
        staff=staff,
        manager=manager,
    )
    return fixture


class MockedRoleRequestManager(object):
    def __init__(self, return_values):
        self.return_values = return_values
        self.current_call = 0
        self.call_args_list = []

    def request(self, *args, **kwargs):
        assert self.current_call < len(self.return_values)
        self.call_args_list.append((args, kwargs))
        self.current_call += 1
        return self.return_values[self.current_call-1]


@patch('plan.resources.models.ResourceType.get_form_metadata', fake_form_metadata)
@pytest.mark.parametrize('with_manager', [False, True])
@pytest.mark.usefixtures('robot')
def test_direct(request, client, data, with_manager):
    client.login(data.staff.user.username)
    request.return_value = Response(200, '{}')

    # дернуть ручку реквест и убедиться что будет дернута ручка tvm
    signature = make_signature(
        service=data.service,
        resource_type=data.resource_type,
        user=data.staff.user
    )

    with patch('plan.resources.tasks._send_resource_to_supplier') as send:
        request.return_value = Response(200, '{"status": "ok", "client_id": 1}')

        form_data = {
            'field_1': '{"question": {"id": 1}, "value": "100500"}',
            'field_4': '{"question": {"id": 4}, "value": "comment_some"}'
        }
        if with_manager:
            form_data['field_2'] = '{"question": {"id": 2}, "value": "cool-manager"}'
            form_data['field_3'] = '{"question": {"id": 3}, "value": "yndx-cool"}'

        response = client.post(
            reverse('resources-api:request-list'),
            form_data,
            **{
                'HTTP_X_FORM_ANSWER_ID': 1,
                'HTTP_X_ABC_USER': data.staff.login,
                'HTTP_X_ABC_SERVICE': data.service.pk,
                'HTTP_X_ABC_RESOURCE_TYPE': data.resource_type.pk,
                'HTTP_X_ABC_SIGNATURE': signature,
            }
        )

    assert response.status_code == 200

    sr = ServiceResource.objects.get()
    assert send.call_args == call(sr)

    assert sr.resource.name == ('100500' if with_manager else '100500-nomanager')
    assert sr.resource.external_id == ('100500' if with_manager else '100500-nomanager')
    assert sr.resource.attributes['client_id'] == '100500'
    assert sr.resource.attributes['comment'] == 'comment_some'
    assert sr.resource.attributes.get('main_manager') == ('cool-manager' if with_manager else None)
    assert sr.resource.attributes.get('main_manager_passport_login') == ('yndx-cool' if with_manager else None)


@patch('plan.resources.models.ResourceType.get_form_metadata', fake_form_metadata)
@pytest.mark.usefixtures('robot')
@pytest.mark.parametrize('is_immutable', (False, True))
@pytest.mark.parametrize('new_service', (False, True))
def test_change_attributes(request, client, data, is_immutable, new_service):
    """
    Проверяем, что если при запросе уже выданного ресурса передать обновленные значения атрибутов,
    у ресурса они обновятся (либо, для неизменяемых ресурсов, ресурс будет заменен на такой же, но с новыми атрибутами)
    """
    request.return_value = Response(200, '{}')

    data.resource_type.is_immutable = is_immutable
    data.resource_type.save()

    client.login(data.staff.login)

    resource = factories.ResourceFactory(
        external_id='external_id',
        name='name',
        type=data.resource_type,
        attributes={
            'client_id': 'external_id',
            'main_manager': 'old-fat-manager',
            'main_manager_passport_login': 'yndx-old-fat'
        }
    )

    factories.ServiceResourceFactory(
        service=data.service,
        resource=resource,
        state=ServiceResource.GRANTED
    )

    if new_service:
        form_service = factories.ServiceFactory()
    else:
        form_service = data.service

    signature = make_signature(
        service=form_service,
        resource_type=data.resource_type,
        user=data.staff.user
    )

    with patch('plan.resources.tasks._send_resource_to_supplier'):
        request.return_value = Response(200, '{"status": "ok", "client_id": 1}')

        response = client.post(
            reverse('resources-api:request-list'),
            {
                'field_1': '{"question": {"id": 1}, "value": "external_id"}',
                'field_2': '{"question": {"id": 2}, "value": "cool-manager"}',
                'field_3': '{"question": {"id": 2}, "value": "yndx-cool"}',
            },
            **{
                'HTTP_X_FORM_ANSWER_ID': 123,
                'HTTP_X_ABC_USER': data.staff.login,
                'HTTP_X_ABC_SERVICE': form_service.pk,
                'HTTP_X_ABC_RESOURCE_TYPE': data.resource_type.pk,
                'HTTP_X_ABC_SIGNATURE': signature,
            }
        )

        assert response.status_code == 200
        sr_count = 1 + int(is_immutable) + int(new_service)
        assert ServiceResource.objects.count() == sr_count

        if is_immutable:
            new_resource = Resource.objects.get(answer_id=123)
        else:
            resource.refresh_from_db()
            new_resource = resource

        assert new_resource.attributes['main_manager'] == 'cool-manager'
        assert new_resource.attributes['main_manager_passport_login'] == 'yndx-cool'

        updated_sr = ServiceResource.objects.get(resource=new_resource, service=data.service)
        if is_immutable:
            assert updated_sr.request_id == 123

        if new_service:
            new_sr = ServiceResource.objects.get(service=form_service, request_id=123)
            assert new_sr.resource == new_resource

        response = client.post(
            reverse('resources-api:request-list'),
            {
                'field_1': '{"question": {"id": 1}, "value": "external_id"}',
                'field_2': '{"question": {"id": 2}, "value": "cool-manager"}',
                'field_3': '{"question": {"id": 2}, "value": "yndx-cool"}',
            },
            **{
                'HTTP_X_FORM_ANSWER_ID': 123,
                'HTTP_X_ABC_USER': data.staff.login,
                'HTTP_X_ABC_SERVICE': form_service.pk,
                'HTTP_X_ABC_RESOURCE_TYPE': data.resource_type.pk,
                'HTTP_X_ABC_SIGNATURE': signature,
            }
        )

        assert response.status_code == 200
        assert ServiceResource.objects.count() == sr_count


def test_build_role_data():
    """
    build_role_data для запроса роли в IDM не возвращает личную роль, так как main_manager не указан
    build_roles_data_for_idm_sync возвращает личную роль, так как нам нужно забрать все роли из IDM
    """
    service = factories.ServiceFactory()
    resource_type = factories.ResourceTypeFactory(code='direct_client', supplier_plugin='direct')
    resource = factories.ResourceFactory(
        type=resource_type,
        external_id=123,
        attributes={'client_id': 123, 'main_manager': None, 'main_manager_passport_login': 'yndx-xx'}
    )
    service_resource = factories.ServiceResourceFactory(resource=resource, service=service)

    plugin = SupplierPlugin.get_plugin_class(resource_type.supplier_plugin)()

    assert plugin.build_role_data(service_resource) == [
        {
            'system': 'direct',
            'path': '/manager_for_client/',
            'request_fields': None,
            'group': service.staff_id,
            'fields_data': {'client_id': 123},
        },
        None,
    ]

    assert plugin.build_roles_data_for_idm_sync(service_resource) == [
        {
            'system': 'direct',
            'path': '/manager_for_client/',
            'type': 'active',
            'ownership': 'group',
        },
        {
            'system': 'direct',
            'path': '/main_manager_for_client/',
            'type': 'active',
            'ownership': 'personal',
            'parent_type': 'absent',
        },
    ]


def test_role_dict_to_key():
    resource_type = factories.ResourceTypeFactory(code='direct_client', supplier_plugin='direct')
    plugin = SupplierPlugin.get_plugin_class(resource_type.supplier_plugin)()

    expected_key = ('xxx', None, 'path', '{"a": "a", "b": 1}')

    user_role = {
        'user': {
            'username': 'xxx',
        },
        'group': None,
        'node': {
            'value_path': 'path'
        },
        'fields_data': {
            'b': 1,
            'a': 'a',
        }
    }

    user_role_for_request = {
        'user': 'xxx',
        'path': 'path',
        'fields_data': {
            'b': 1,
            'a': 'a',
        }
    }

    assert (
        plugin.role_dict_to_key(user_role, from_api=True) ==
        plugin.role_dict_to_key(user_role_for_request, from_api=False) ==
        expected_key
    )


def test_revoke_only_personal_roles():
    service = factories.ServiceFactory()
    resource_type = factories.ResourceTypeFactory(code='direct_client', supplier_plugin='direct')
    resource = factories.ResourceFactory(
        type=resource_type,
        external_id=123,
        attributes={'client_id': 123, 'main_manager': None, 'main_manager_passport_login': 'yndx-xx'}
    )
    sr1 = factories.ServiceResourceFactory(resource=resource, service=service, state=ServiceResource.GRANTED)
    sr2 = factories.ServiceResourceFactory(resource=resource, service=service, state=ServiceResource.GRANTED)

    plugin = SupplierPlugin.get_plugin_class(resource_type.supplier_plugin)()
    with mock.patch('plan.resources.suppliers.direct.DirectPlugin.revoke_only_group_roles') as m:
        plugin.delete(sr1, None)
    assert m.call_args_list == [call(sr1)]

    sr2.state = ServiceResource.DEPRIVED
    sr2.save()

    with mock.patch('plan.resources.suppliers.direct.DirectPlugin.revoke_only_group_roles') as m:
        plugin.delete(sr1, None)
    assert m.call_args_list == []


def test_revoke_roles_no_manager():
    service = factories.ServiceFactory()
    service_2 = factories.ServiceFactory()
    resource_type = factories.ResourceTypeFactory(code='direct_client', supplier_plugin='direct')
    resource = factories.ResourceFactory(
        type=resource_type,
        external_id='123',
        attributes={
            'client_id': 123,
            'main_manager': None,
            'main_manager_passport_login': 'yndx-xx'
        }
    )
    sr1 = factories.ServiceResourceFactory(resource=resource, service=service, state=ServiceResource.GRANTED)
    # если только один ресурс - отзываем все роли
    with mock.patch('plan.resources.suppliers.direct.FinancialBasePlugin.delete') as mock_delete:
        plugin = SupplierPlugin.get_plugin_class(resource_type.supplier_plugin)()
        plugin.delete(sr1, None)
        mock_delete.assert_called_once()

    # добавим nomanager ресурс
    resource2 = factories.ResourceFactory(
        type=resource_type,
        external_id='123-nomanager',
        attributes={
            'client_id': 123,
            'main_manager': None,
            'main_manager_passport_login': 'yndx-xx'
        }
    )
    sr2 = factories.ServiceResourceFactory(resource=resource2, service=service, state=ServiceResource.GRANTED)

    # если отзываем nomanger ресурс - никакие роли не должны отозваться
    with mock.patch('plan.resources.suppliers.direct.FinancialBasePlugin.delete') as mock_delete:
        with mock.patch('plan.resources.suppliers.direct.DirectPlugin.revoke_only_group_roles') as mock_group:
            with mock.patch('plan.resources.suppliers.direct.DirectPlugin.revoke_only_personal_roles') as mock_personal:
                plugin = SupplierPlugin.get_plugin_class(resource_type.supplier_plugin)()
                plugin.delete(sr2, None)
                mock_delete.assert_not_called()
                mock_group.assert_not_called()
                mock_personal.assert_not_called()

    # если отзываем ресурс с менеджером - отзывается только роль с менеджером
    with mock.patch('plan.resources.suppliers.direct.FinancialBasePlugin.delete') as mock_delete:
        with mock.patch('plan.resources.suppliers.direct.DirectPlugin.revoke_only_group_roles') as mock_group:
            with mock.patch('plan.resources.suppliers.direct.DirectPlugin.revoke_only_personal_roles') as mock_personal:
                plugin = SupplierPlugin.get_plugin_class(resource_type.supplier_plugin)()
                plugin.delete(sr1, None)
                mock_delete.assert_not_called()
                mock_group.assert_not_called()
                mock_personal.assert_called_once()

    sr2.service = service_2
    sr2.save()

    # nomanager клиент есть - но в другом сервисе, таки связи игнорируем
    with mock.patch('plan.resources.suppliers.direct.FinancialBasePlugin.delete') as mock_delete:
        with mock.patch('plan.resources.suppliers.direct.DirectPlugin.revoke_only_group_roles') as mock_group:
            with mock.patch('plan.resources.suppliers.direct.DirectPlugin.revoke_only_personal_roles') as mock_personal:
                plugin = SupplierPlugin.get_plugin_class(resource_type.supplier_plugin)()
                plugin.delete(sr1, None)
                mock_delete.assert_called_once()
                mock_group.assert_not_called()
                mock_personal.assert_not_called()

    # вернем обратно в этот сервис
    sr2.service = service
    sr2.save()

    # если отзываем ресурс с менеджером и есть nomanager + ресурс с этим менеджером в другом сервисе - не отзываем роли
    factories.ServiceResourceFactory(resource=resource, service=service_2, state=ServiceResource.GRANTED)
    with mock.patch('plan.resources.suppliers.direct.FinancialBasePlugin.delete') as mock_delete:
        with mock.patch('plan.resources.suppliers.direct.DirectPlugin.revoke_only_group_roles') as mock_group:
            with mock.patch('plan.resources.suppliers.direct.DirectPlugin.revoke_only_personal_roles') as mock_personal:
                plugin = SupplierPlugin.get_plugin_class(resource_type.supplier_plugin)()
                plugin.delete(sr1, None)
                mock_delete.assert_not_called()
                mock_group.assert_not_called()
                mock_personal.assert_not_called()

    sr2.state = 'deprived'
    sr2.save()

    # nomanager клиента нет - отзываем групповые роли
    factories.ServiceResourceFactory(resource=resource, service=service_2, state=ServiceResource.GRANTED)
    with mock.patch('plan.resources.suppliers.direct.FinancialBasePlugin.delete') as mock_delete:
        with mock.patch('plan.resources.suppliers.direct.DirectPlugin.revoke_only_group_roles') as mock_group:
            with mock.patch('plan.resources.suppliers.direct.DirectPlugin.revoke_only_personal_roles') as mock_personal:
                plugin = SupplierPlugin.get_plugin_class(resource_type.supplier_plugin)()
                plugin.delete(sr1, None)
                mock_delete.assert_not_called()
                mock_group.assert_called_once()
                mock_personal.assert_not_called()
