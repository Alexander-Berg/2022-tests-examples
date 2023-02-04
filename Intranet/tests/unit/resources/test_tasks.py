from datetime import timedelta, date

import pretend
import pytest
from common import factories
from django.conf import settings
from django.db.utils import OperationalError
from django.test import override_settings
from django.utils.timezone import now
from freezegun import freeze_time
from mock import MagicMock, call, patch, ANY
from plan.idm import exceptions
from plan.resources import constants
from plan.resources.models import (
    ServiceResource,
    ServiceResourceCounter,
    Resource,
)
from plan.resources.suppliers.base import SupplierPlugin
from plan.resources.tasks import (
    grant_chunk_service_resources,
    grant_resource,
    retry_grant,
    revoke_obsolete_direct_roles,
    sync_with_bot,
    update_serviceresources_count,
    upload_gdpr_to_yt,
    sync_with_alert_provider,
    get_hash,
    sync_with_money_map,

)

pytestmark = pytest.mark.django_db


@pytest.fixture
def data(db, owner_role):
    supplier = factories.ServiceFactory()
    resource_type = factories.ResourceTypeFactory(
        supplier=supplier,
        supplier_plugin='tvm',
    )
    resource = factories.ResourceFactory(type=resource_type)
    consumer = factories.ServiceFactory()
    service_resource = factories.ServiceResourceFactory(
        resource=resource,
        service=consumer,
        state=ServiceResource.APPROVED,
    )
    return pretend.stub(
        resource=resource,
        service_resource=service_resource,
    )


@pytest.fixture
def counters_data():
    service_one = factories.ServiceFactory()
    service_two = factories.ServiceFactory()

    resource_type_one = factories.ResourceTypeFactory()
    resource_type_two = factories.ResourceTypeFactory()
    resource_type_three = factories.ResourceTypeFactory()

    for _ in range(10):
        resource = factories.ResourceFactory(type=resource_type_one)
        factories.ServiceResourceFactory(
            resource=resource,
            service=service_one,
            state=ServiceResource.APPROVED,
        )
    factories.ServiceResourceCounterFactory(
        resource_type=resource_type_one,
        service=service_one,
        count=3,
    )

    resource = factories.ResourceFactory(type=resource_type_two)
    factories.ServiceResourceFactory(
        resource=resource,
        service=service_one,
        state=ServiceResource.DEPRIVED,
    )
    factories.ServiceResourceCounterFactory(
        resource_type=resource_type_two,
        service=service_one,
        count=2,
    )

    with freeze_time('2020-12-03'):
        resource = factories.ResourceFactory(type=resource_type_three)
        factories.ServiceResourceFactory(
            resource=resource,
            service=service_one,
            state=ServiceResource.APPROVED,
        )
    factories.ServiceResourceCounterFactory(
        resource_type=resource_type_three,
        service=service_one,
        count=2,
    )

    for _ in range(4):
        resource = factories.ResourceFactory(type=resource_type_two)
        factories.ServiceResourceFactory(
            resource=resource,
            service=service_two,
            state=ServiceResource.APPROVED,
        )
    return pretend.stub(
        service_one=service_one,
        service_two=service_two,
        resource_type_one=resource_type_one,
        resource_type_two=resource_type_two,
        resource_type_three=resource_type_three,
    )


@pytest.fixture
def mocked_grant():
    with patch('plan.resources.tasks.grant_resource.apply_async') as grant_resource:
        yield grant_resource


def test_grant_resource(data):
    with patch('plan.resources.suppliers.tvm.TVMPlugin.create') as patched:
        patched.return_value = 20, {'external_id': 20, 'herp': 'derp'}
        data.service_resource.state = ServiceResource.GRANTING
        data.service_resource.save()
        grant_resource(data.service_resource.id)

        assert patched.called

        data.resource.refresh_from_db()
        assert data.resource.external_id == '20'
        assert data.resource.supplier_response == {'external_id': 20, 'herp': 'derp'}


def test_retry_grant(mocked_grant):
    service_resource = factories.ServiceResourceFactory(state=ServiceResource.GRANTING)

    with freeze_time(now() + timedelta(seconds=40 * 60)):
        retry_grant()

        assert mocked_grant.call_args_list == [call(args=[service_resource.id])]
        mocked_grant.reset_mock()

    with freeze_time(now() + timedelta(seconds=20 * 60)):
        retry_grant()

        assert not mocked_grant.called


def test_retry_grant_max_retries():
    """
    У ресурса две попытки выдачи: после первой он еще пытается выдаться, а потом переходит в ошибку
    """
    resource_type = factories.ResourceTypeFactory(supplier_plugin='tvm')
    resource = factories.ResourceFactory(type=resource_type)
    service_resource = factories.ServiceResourceFactory(grant_retries=2, state=ServiceResource.GRANTING, resource=resource)
    with patch('plan.resources.tasks._send_resource_to_supplier') as _send_resource_to_supplier, \
            freeze_time(now() + timedelta(minutes=40)):
        _send_resource_to_supplier.side_effect = [ValueError(), ValueError()]
        retry_grant()
        service_resource.refresh_from_db()
        assert service_resource.state == ServiceResource.GRANTING

        retry_grant()
        service_resource.refresh_from_db()
        assert service_resource.state == ServiceResource.ERROR


def test_granting_resources_with_complext_editing(mocked_grant):
    obsolete = factories.ServiceResourceFactory(
        state=ServiceResource.GRANTED
    )
    service_resource = factories.ServiceResourceFactory(
        state=ServiceResource.GRANTING,
        obsolete=obsolete
    )

    grant_resource(service_resource.id)

    obsolete.refresh_from_db()
    assert obsolete.state == ServiceResource.OBSOLETE

    service_resource.refresh_from_db()
    assert service_resource.state == ServiceResource.GRANTED


def test_grant_direct_roles_with_manager():
    """
    По одному ресурсу могут быть выданы несколько ролей
    """
    direct_client_id = '100500'
    resource_type = factories.ResourceTypeFactory(supplier_plugin='direct', code=settings.DIRECT_RESOURCE_TYPE_CODE)
    resource = factories.ResourceFactory(
        type=resource_type,
        external_id=direct_client_id,
        attributes={'client_id': direct_client_id, 'main_manager': 'mace-windu', 'main_manager_passport_login': 'yndx-windu'}
    )
    service = factories.ServiceFactory()
    service_resource = factories.ServiceResourceFactory(state=ServiceResource.GRANTING, resource=resource, service=service)
    with patch('plan.resources.suppliers.base.RoleRequestManager.request') as request:
        request.side_effect = [
            {'id': 100, 'hello': 'there'},
            {'id': 200, 'general': 'kenobi'},
        ]
        grant_resource(service_resource.id)
        assert len(request.call_args_list) == 2
        assert request.call_args_list[0][1] == {
            'system': 'direct',
            'group': service.staff_id,
            'path': '/manager_for_client/',
            'fields_data': {'client_id': '100500'},
            'request_fields': None,
        }
        assert request.call_args_list[1][1] == {
            'system': 'direct',
            'user': 'mace-windu',
            'path': '/main_manager_for_client/',
            'fields_data': {'client_id': '100500', 'passport-login': 'yndx-windu'},
            'request_fields': {'service': service.slug}
        }

        resource.refresh_from_db()
        assert resource.external_id == direct_client_id
        assert resource.attributes == {
            'client_id': direct_client_id,
            'main_manager': 'mace-windu',
            'main_manager_passport_login': 'yndx-windu'
        }

        service_resource.refresh_from_db()
        service_resource_role_id = [100, 200]
        assert service_resource.attributes['role_id'] == service_resource_role_id
        assert service_resource.state == ServiceResource.GRANTED

    # перевыдадим ресурс (например, вернули после отзыва), но роли останутся в атрибутах
    service_resource.state = ServiceResource.GRANTING
    service_resource.save(update_fields=['state'])
    service_resource.refresh_from_db()
    assert service_resource.attributes['role_id'] == service_resource_role_id

    # после выдачи роли должны обновиться
    with patch('plan.resources.suppliers.base.RoleRequestManager.request') as request_2:
        with patch('plan.resources.suppliers.base.RoleManager.get_role') as get_role:
            request_2.side_effect = [
                {'id': 300}, {'id': 400}
            ]
            get_role.side_effect = [
                {
                    'system': {'slug': 'direct'},
                    'state': 'deprived',
                    'group': {'id': service.staff_id},
                    'user': None,
                    'node': {'value_path': '/manager_for_client/'},
                    'fields_data': {'client_id': '100500'},
                    'request_fields': None,
                },
                {
                    'system': {'slug': 'direct'},
                    'state': 'deprived',
                    'user': {'username': 'mace-windu'},
                    'group': None,
                    'node': {'value_path': '/main_manager_for_client/'},
                    'fields_data': {'client_id': '100500', 'passport-login': 'yndx-windu'},
                    'request_fields': {'service': service.slug}
                }
            ]
            grant_resource(service_resource.id)
            service_resource.refresh_from_db()
            assert service_resource.state == ServiceResource.GRANTED
            assert service_resource.attributes['role_id'] == [300, 400]


def test_grant_direct_roles_without_manager():
    """
    Указание менеджера необязательно, в таком случае роль будет выдана только на сервисную группу
    """
    direct_client_id = '100500'
    resource_type = factories.ResourceTypeFactory(supplier_plugin='direct', code=settings.DIRECT_RESOURCE_TYPE_CODE)
    resource = factories.ResourceFactory(
        type=resource_type,
        external_id=direct_client_id,
        attributes={'client_id': direct_client_id, 'main_manager': None}
    )
    service = factories.ServiceFactory()
    service_resource = factories.ServiceResourceFactory(state=ServiceResource.GRANTING, resource=resource, service=service)
    with patch('plan.resources.suppliers.base.RoleRequestManager.request') as request:
        request.side_effect = [
            {'id': 100, 'hello': 'there'},
            {'id': 200, 'general': 'kenobi'},
        ]
        grant_resource(service_resource.id)
        assert len(request.call_args_list) == 1
        params = {
            'system': 'direct',
            'group': service.staff_id,
            'path': '/manager_for_client/',
            'fields_data': {'client_id': '100500'},
            'request_fields': None,
        }
        assert request.call_args_list[0][1] == params

        resource.refresh_from_db()
        assert resource.external_id == direct_client_id
        assert resource.attributes == {'client_id': direct_client_id, 'main_manager': None}

        service_resource.refresh_from_db()
        assert service_resource.attributes['role_id'] == [100, None]
        assert service_resource.state == ServiceResource.GRANTED


def test_grant_direct_roles_with_partial_failure():
    """
    Может получиться выдать только часть ролей. В таком случае мы попытаемся перезапросить роль, а потом отвалимся
    """
    direct_client_id = '100500'
    resource_type = factories.ResourceTypeFactory(supplier_plugin='direct', code=settings.DIRECT_RESOURCE_TYPE_CODE)
    resource = factories.ResourceFactory(
        type=resource_type,
        external_id=direct_client_id,
        attributes={'client_id': direct_client_id, 'main_manager': 'mace-windu', 'main_manager_passport_login': 'yndx-windu'}
    )
    service = factories.ServiceFactory()
    service_resource = factories.ServiceResourceFactory(
        grant_retries=3,
        state=ServiceResource.GRANTING,
        resource=resource,
        service=service
    )

    def mocked_request():
        yield {'id': 100, 'hello': 'there'}
        raise exceptions.Conflict()

    with patch('plan.resources.suppliers.base.RoleRequestManager.request') as request:
        with patch('plan.resources.suppliers.base.RoleManager.get_roles') as get_roles:
            request.side_effect = mocked_request()
            get_roles.return_value = {'objects': []}
            grant_resource(service_resource.id)

    assert len(request.call_args_list) == 2
    assert request.call_args_list[0][1] == {
        'system': 'direct',
        'group': service.staff_id,
        'path': '/manager_for_client/',
        'fields_data': {'client_id': '100500'},
        'request_fields': None,
    }
    assert request.call_args_list[1][1] == {
        'system': 'direct',
        'user': 'mace-windu',
        'path': '/main_manager_for_client/',
        'fields_data': {'client_id': '100500', 'passport-login': 'yndx-windu'},
        'request_fields': {'service': service.slug}
    }

    resource.refresh_from_db()
    assert resource.external_id == direct_client_id
    assert resource.attributes == {
        'client_id': direct_client_id,
        'main_manager': 'mace-windu',
        'main_manager_passport_login': 'yndx-windu'
    }

    service_resource.refresh_from_db()
    assert service_resource.attributes['role_id'] == [100, None]
    assert service_resource.state == ServiceResource.GRANTING

    for retry in range(2):
        with patch('plan.resources.suppliers.base.RoleRequestManager.request') as request:
            with patch('plan.resources.suppliers.base.RoleManager.get_roles') as get_roles:
                with patch('plan.resources.suppliers.base.RoleManager.get_role') as get_role:
                    request.side_effect = exceptions.Conflict
                    get_roles.return_value = {'objects': []}
                    get_role.side_effect = [
                        {
                            'system': {'slug': 'direct'},
                            'state': 'granted',
                            'group': {'id': service.staff_id},
                            'user': None,
                            'node': {'value_path': '/manager_for_client/'},
                            'fields_data': {'client_id': '100500'},
                            'request_fields': None,
                        },
                    ]
                    grant_resource(service_resource.id)

        assert len(request.call_args_list) == 1
        assert request.call_args_list[0][1] == {
            'system': 'direct',
            'user': 'mace-windu',
            'path': '/main_manager_for_client/',
            'fields_data': {'client_id': '100500', 'passport-login': 'yndx-windu'},
            'request_fields': {'service': service.slug},
        }

        resource.refresh_from_db()
        assert resource.external_id == direct_client_id
        assert resource.attributes == {
            'client_id': direct_client_id,
            'main_manager': 'mace-windu',
            'main_manager_passport_login': 'yndx-windu'
        }

        service_resource.refresh_from_db()
        assert service_resource.attributes['role_id'] == [100, None]
        if retry == 0:
            assert service_resource.state == ServiceResource.GRANTING
        else:
            assert service_resource.state == ServiceResource.ERROR


plugin_code_map = [
    ('metrika', settings.METRIKA_RESOURCE_TYPE_CODE),
    ('robots', settings.ROBOT_RESOURCE_TYPE_CODE),
]


@pytest.mark.parametrize('plugin,code', plugin_code_map)
def test_grant_missing_idm_roles_with_good_role(plugin, code):
    """
    Ресурсу должна соответствовать активная роль в IDM
    """
    idm_role_id = 1
    metrika_counter_id = '100500'
    robot_name = 'vasya'
    resource_type = factories.ResourceTypeFactory(supplier_plugin=plugin, code=code)
    robot_attrs = {
        'secret_id': {
            'type': 'link',
            'url': 'aaaa',
            'value': 'oooo',
        }
    }
    service = factories.ServiceFactory()
    if plugin == 'metrika':
        resource = factories.ResourceFactory(
            type=resource_type,
            external_id=metrika_counter_id,
            attributes={'counter_id': metrika_counter_id}
        )
        service_resource_attrs = {'role_id': idm_role_id}
    else:
        resource = factories.ResourceFactory(
            type=resource_type,
            external_id=robot_name,
            attributes=robot_attrs,
        )
        factories.ServiceScopeFactory(service=service, role_scope__slug=settings.ABC_ROBOTS_MANAGEMENT_SCOPE)
        factories.StaffFactory(login=robot_name, is_robot=True)
        service_resource_attrs = {
            'role_id': [idm_role_id],
            'secret_role_state': 'ok',
            'staff_role_state': 'ok',
        }
    service_resource = factories.ServiceResourceFactory(
        state=ServiceResource.GRANTED,
        resource=resource,
        service=service,
        attributes=service_resource_attrs,
    )
    plugin_obj = SupplierPlugin.get_plugin_class(resource_type.supplier_plugin)()
    with patch('plan.resources.suppliers.base.RoleManager.get_role') as get_role,\
         patch('plan.resources.suppliers.base.RoleManager.get_roles') as get_roles,\
         patch('plan.resources.suppliers.base.RoleRequestManager.request') as request:
        get_role.return_value = {'is_active': True}
        plugin_obj.create_missing(service_resource)
        assert len(get_role.call_args_list) == 1
        assert get_role.call_args[0][1] == idm_role_id
        assert get_roles.call_args is None
        assert request.call_args is None

    resource.refresh_from_db()
    if plugin == 'metrika':
        assert resource.external_id == metrika_counter_id
        assert resource.attributes == {'counter_id': metrika_counter_id}
        service_resource.refresh_from_db()
        assert service_resource.attributes['role_id'] == [idm_role_id]
    else:
        assert resource.external_id == robot_name
        assert resource.attributes == robot_attrs
        service_resource.refresh_from_db()
        assert service_resource.attributes == service_resource_attrs


@pytest.mark.parametrize('plugin,code', plugin_code_map)
def test_grant_missing_idm_roles_with_failed_role(plugin, code):
    """
    Если роль в статусе ошибки, то не перезапрашиваем.
    """
    idm_role_id = 1
    metrika_counter_id = '100500'
    robot_name = 'vasya'
    resource_type = factories.ResourceTypeFactory(supplier_plugin=plugin, code=code)
    robot_attrs = {
        'secret_id': {
            'type': 'link',
            'url': 'aaaa',
            'value': 'oooo',
        }
    }
    service = factories.ServiceFactory()
    if plugin == 'metrika':
        resource = factories.ResourceFactory(
            type=resource_type,
            external_id=metrika_counter_id,
            attributes={'counter_id': metrika_counter_id}
        )
        service_resource_attrs = {'role_id': idm_role_id}
    else:
        resource = factories.ResourceFactory(
            type=resource_type,
            external_id=robot_name,
            attributes=robot_attrs,
        )
        factories.ServiceScopeFactory(service=service, role_scope__slug=settings.ABC_ROBOTS_MANAGEMENT_SCOPE)
        factories.StaffFactory(login=robot_name, is_robot=True)
        service_resource_attrs = {
            'role_id': [idm_role_id],
            'secret_role_state': 'ok',
            'staff_role_state': 'ok',
        }
    service_resource = factories.ServiceResourceFactory(
        state=ServiceResource.GRANTED,
        resource=resource,
        service=service,
        attributes=service_resource_attrs,
    )
    plugin = SupplierPlugin.get_plugin_class(resource_type.supplier_plugin)()
    with patch('plan.resources.suppliers.base.RoleManager.get_role') as get_role, \
         patch('plan.resources.suppliers.base.RoleManager.get_roles') as get_roles, \
         patch('plan.resources.suppliers.base.RoleRequestManager.request') as request:
        get_role.return_value = {'is_active': False, 'state': 'failed'}
        plugin.create_missing(service_resource)
        assert len(get_role.call_args_list) == 1
        assert get_role.call_args[0][1] == idm_role_id
        assert get_roles.call_args is None
        assert request.call_args is None


@pytest.mark.parametrize('with_users', (True, False))
def test_grant_missing_idm_roles_without_roles_for_robots(with_users):
    """
    Если роль робота, выданную по ресурсу, отозвали мы должны выдать новую
    """
    idm_role_id = 1
    robot_name = 'vasya'
    service = factories.ServiceFactory()
    resource_type = factories.ResourceTypeFactory(supplier_plugin='robots', code=settings.ROBOT_RESOURCE_TYPE_CODE)
    robot_attrs = {
        'secret_id': {
            'type': 'link',
            'url': 'aaaa',
            'value': 'oooo',
        }
    }
    service_resource_attrs = {
        'role_id': [idm_role_id],
        'secret_role_state': 'ok',
        'staff_role_state': 'ok',
    }
    resource = factories.ResourceFactory(
        type=resource_type,
        external_id=robot_name,
        attributes=robot_attrs,
    )
    group = factories.ServiceScopeFactory(
        service=service,
        role_scope__slug=settings.ABC_ROBOTS_MANAGEMENT_SCOPE
    )
    if with_users:
        users_group = factories.ServiceScopeFactory(
            service=service,
            role_scope__slug=settings.ABC_ROBOTS_USERS_SCOPE
        )
    robot = factories.StaffFactory(login=robot_name, is_robot=True)
    service_resource = factories.ServiceResourceFactory(
        state=ServiceResource.GRANTED,
        resource=resource,
        service=service,
        attributes=service_resource_attrs,
    )
    plugin = SupplierPlugin.get_plugin_class(resource_type.supplier_plugin)()
    with patch('plan.resources.suppliers.base.RoleManager.get_role') as get_role,\
         patch('plan.resources.suppliers.base.RoleManager.get_roles') as get_roles,\
         patch('plan.resources.suppliers.base.RoleRequestManager.request') as request:
        get_role.return_value = {'is_active': False, 'state': 'deprived'}
        get_roles.return_value = {'objects': [{'id': 2}, {'id': 205}]}
        get_roles.return_value = {'objects': []}
        request.return_value = {'id': 1234, 'other_response_data': 'something'}
        plugin.create_missing(service_resource)
        assert len(get_role.call_args_list) == 1
        assert get_role.call_args[0][1] == idm_role_id
        expected_calls = [
            call(ANY, filters={
                'comment': 'Выдача ресурса робота vasya',
                'group': group.staff_id, 'system': 'staff',
                'path': '/robots/%s/owner/' % robot.staff_id,
                'fields_data': '{}',
                'type': 'returnable',
                'parent_type': 'absent'
            })
        ]
        if with_users:
            expected_calls.append(
                call(ANY, filters={
                    'comment': 'Выдача ресурса робота vasya',
                    'group': users_group.staff_id, 'system': 'staff',
                    'path': '/robots/%s/user/' % robot.staff_id,
                    'fields_data': '{}',
                    'type': 'returnable',
                    'parent_type': 'absent'
                })
            )
        assert get_roles.call_args_list == expected_calls

        expected_request_calls = [
            call(
                ANY, comment='Выдача ресурса робота vasya',
                fields_data={}, group=group.staff_id,
                path='/robots/%s/owner/' % robot.staff_id,
                system='staff'
            )
        ]
        if with_users:
            expected_request_calls.append(
                call(
                    ANY, comment='Выдача ресурса робота vasya',
                    fields_data={}, group=users_group.staff_id,
                    path='/robots/%s/user/' % robot.staff_id,
                    system='staff'
                )
            )
        assert request.call_args_list == expected_request_calls

        resource.refresh_from_db()
        service_resource.refresh_from_db()
        assert resource.external_id == robot_name
        assert resource.attributes == robot_attrs
        assert service_resource.attributes['role_id'] == ([1234, 1234] if with_users else [1234])


def test_grant_missing_idm_roles_with_similar_role():
    """
    Если роль в метрике, выданную по ресурсу, отозвали, мы должны найти другую
    """
    idm_role_id = 1
    metrika_counter_id = '100500'
    resource_type = factories.ResourceTypeFactory(supplier_plugin='metrika', code=settings.METRIKA_RESOURCE_TYPE_CODE)
    resource = factories.ResourceFactory(
        type=resource_type,
        external_id=metrika_counter_id,
        attributes={'counter_id': metrika_counter_id}
    )
    service = factories.ServiceFactory()
    service_resource = factories.ServiceResourceFactory(
        state=ServiceResource.GRANTED,
        resource=resource,
        service=service,
        attributes={'role_id': idm_role_id},
    )
    plugin = SupplierPlugin.get_plugin_class(resource_type.supplier_plugin)()
    with patch('plan.resources.suppliers.base.RoleManager.get_role') as get_role, \
         patch('plan.resources.suppliers.base.RoleManager.get_roles') as get_roles, \
         patch('plan.resources.suppliers.base.RoleRequestManager.request') as request:
        get_role.return_value = {'is_active': False, 'state': 'deprived'}
        get_roles.return_value = {'objects': [{'id': 2}, {'id': 205}]}
        plugin.create_missing(service_resource)
        assert len(get_role.call_args_list) == 1
        assert get_role.call_args[0][1] == idm_role_id
        assert len(get_roles.call_args_list) == 1
        assert get_roles.call_args[1]['filters'] == {
            'type': 'returnable',
            'parent_type': 'absent',
            'group': service.staff_id,
            'system': 'metrika',
            'path': '/external_counter_grant/',
            'fields_data': '{"counter_id": "%s"}' % metrika_counter_id,
        }
        assert request.call_args is None
        resource.refresh_from_db()
        assert resource.external_id == metrika_counter_id
        assert resource.attributes == {'counter_id': metrika_counter_id}
        service_resource.refresh_from_db()
        assert service_resource.attributes['role_id'] == [2]


def test_grant_missing_idm_roles_without_roles():
    """
    Если роль в метрике, выданную по ресурсу, отозвали, а похожих ролей нет, мы должны выдать новую
    """
    idm_role_id = 1
    metrika_counter_id = '100500'
    resource_type = factories.ResourceTypeFactory(supplier_plugin='metrika', code=settings.METRIKA_RESOURCE_TYPE_CODE)
    resource = factories.ResourceFactory(
        type=resource_type,
        external_id=metrika_counter_id,
        attributes={'counter_id': metrika_counter_id}
    )
    service = factories.ServiceFactory()
    service_resource = factories.ServiceResourceFactory(
        state=ServiceResource.GRANTED,
        resource=resource,
        service=service,
        attributes={'role_id': idm_role_id},
    )
    plugin = SupplierPlugin.get_plugin_class(resource_type.supplier_plugin)()
    with patch('plan.resources.suppliers.base.RoleManager.get_role') as get_role,\
         patch('plan.resources.suppliers.base.RoleManager.get_roles') as get_roles,\
         patch('plan.resources.suppliers.base.RoleRequestManager.request') as request:
        get_role.return_value = {'is_active': False, 'state': 'deprived'}
        get_roles.return_value = {'objects': [{'id': 2}, {'id': 205}]}
        get_roles.return_value = {'objects': []}
        request.return_value = {'id': 1234, 'other_response_data': 'something'}
        plugin.create_missing(service_resource)
        assert len(get_role.call_args_list) == 1
        assert get_role.call_args[0][1] == idm_role_id
        assert len(get_roles.call_args_list) == 1
        assert get_roles.call_args[1]['filters'] == {
            'type': 'returnable',
            'parent_type': 'absent',
            'group': service.staff_id,
            'system': 'metrika',
            'path': '/external_counter_grant/',
            'fields_data': '{"counter_id": "%s"}' % metrika_counter_id,
        }
        assert len(request.call_args_list) == 1
        assert request.call_args[1] == {
            'group': service.staff_id,
            'system': 'metrika',
            'path': '/external_counter_grant/',
            'fields_data': {
                'counter_id': metrika_counter_id
            },
            'request_fields': None,
        }
        resource.refresh_from_db()
        service_resource.refresh_from_db()
        assert resource.external_id == metrika_counter_id
        assert resource.attributes == {'counter_id': metrika_counter_id}
        assert service_resource.attributes['role_id'] == [1234]


@pytest.mark.parametrize('failures_as_nulls', [False, True])
def test_grant_missing_direct_roles_for_manager(failures_as_nulls):
    """
    Роли ресурса перевыдаются точечно – только те, которые надо перевыдать
    """
    idm_role_id = 1
    direct_client_id = '100500'
    resource_type = factories.ResourceTypeFactory(supplier_plugin='direct', code=settings.DIRECT_RESOURCE_TYPE_CODE)
    resource = factories.ResourceFactory(
        type=resource_type,
        external_id=direct_client_id,
        attributes={'client_id': direct_client_id, 'main_manager': 'mace-windu', 'main_manager_passport_login': 'yndx-windu'}
    )
    service = factories.ServiceFactory()
    if failures_as_nulls:
        attributes = {'role_id': [idm_role_id, None]}
    else:
        attributes = {'role_id': idm_role_id}
    service_resource = factories.ServiceResourceFactory(
        state=ServiceResource.GRANTED,
        resource=resource,
        service=service,
        attributes=attributes,
    )
    plugin = SupplierPlugin.get_plugin_class(resource_type.supplier_plugin)()
    with patch('plan.resources.suppliers.base.RoleManager.get_role') as get_role,\
         patch('plan.resources.suppliers.base.RoleManager.get_roles') as get_roles,\
         patch('plan.resources.suppliers.base.RoleRequestManager.request') as request:
        get_role.side_effect = [{'is_active': True, 'state': 'granted'}, {'is_active': False, 'state': 'deprived'}]
        get_roles.return_value = {'objects': []}
        request.return_value = {'id': 1234, 'other_response_data': 'something'}
        plugin.create_missing(service_resource)
        assert len(get_role.call_args_list) == 1
        assert get_role.call_args_list[0][0][1] == idm_role_id
        assert len(get_roles.call_args_list) == 1
        assert get_roles.call_args[1]['filters'] == {
            'type': 'returnable',
            'parent_type': 'absent',
            'user': 'mace-windu',
            'system': 'direct',
            'path': '/main_manager_for_client/',
            'fields_data': '{"client_id": "%s", "passport-login": "yndx-windu"}' % direct_client_id,
        }
        assert len(request.call_args_list) == 1
        assert request.call_args[1] == {
            'user': 'mace-windu',
            'system': 'direct',
            'path': '/main_manager_for_client/',
            'fields_data': {
                'client_id': direct_client_id,
                'passport-login': 'yndx-windu',
            },
            'request_fields': {'service': service.slug},
        }
        resource.refresh_from_db()
        service_resource.refresh_from_db()
        assert resource.external_id == direct_client_id
        assert resource.attributes == {
            'client_id': direct_client_id,
            'main_manager': 'mace-windu',
            'main_manager_passport_login': 'yndx-windu'
        }
        assert service_resource.attributes['role_id'] == [idm_role_id, 1234]


def test_revoke_obsolete_direct_roles():
    resource_type = factories.ResourceTypeFactory(supplier_plugin='direct', code=settings.DIRECT_RESOURCE_TYPE_CODE)
    resource = factories.ResourceFactory(type=resource_type)
    service_resource = factories.ServiceResourceFactory(
        resource=resource, state='obsolete', attributes={'role_id': [1, 2, None, 3, 4]}
    )
    with patch('plan.resources.suppliers.base.RoleManager.get_role') as get_role, \
            patch('plan.resources.suppliers.base.RoleManager.deprive') as deprive_role, \
            pytest.raises(AssertionError):

        get_role.side_effect = [
            {'group': 1, 'user': None, 'state': 'granted'},
            {'group': None, 'user': 1, 'state': 'granted'},
            # Пропускаем None
            {'group': None, 'user': 2, 'state': 'granted'},
            {'group': None, 'user': 3, 'state': 'granted'},
        ]
        deprive_role.side_effect = [{}, AssertionError(), {}]
        revoke_obsolete_direct_roles()

    assert len(get_role.call_args_list) == 4
    assert len(deprive_role.call_args_list) == 3

    service_resource.refresh_from_db()
    #  Роль 1 групповая - ее просто пропустили
    #  Роль 2 смогли отозвать
    #  Роль None – какая-то роль, которую не выдали/решили не выдавать. Пропускаем.
    #  Роль 3 не смогли отозвать
    #  Роль 4 смогли отозвать
    assert service_resource.attributes['role_id'] == [3]


def test_revoke_obsolete_direct_roles_without_roles():
    resource_type = factories.ResourceTypeFactory(supplier_plugin='direct', code=settings.DIRECT_RESOURCE_TYPE_CODE)
    resource = factories.ResourceFactory(type=resource_type)
    service_resource = factories.ServiceResourceFactory(
        resource=resource, state='obsolete', attributes={'role_id': [None, None]}
    )
    with patch('plan.resources.suppliers.base.RoleManager.get_role') as get_role, \
            patch('plan.resources.suppliers.base.RoleManager.deprive') as deprive_role:

        get_role.side_effect = AssertionError()
        deprive_role.side_effect = AssertionError()
        revoke_obsolete_direct_roles()

    assert len(get_role.call_args_list) == 0
    assert len(deprive_role.call_args_list) == 0

    service_resource.refresh_from_db()
    assert 'role_id' not in service_resource.attributes


@override_settings(EXCLUDE_IN_SYNC_WITH_BOT='B')
@pytest.mark.parametrize('with_arg', [True, False])
def test_sync_with_bot(with_arg):
    a = factories.ServiceFactory(slug='A')
    b = factories.ServiceFactory(slug='B')
    c = factories.ServiceFactory(slug='C')
    bot = factories.ServiceFactory(slug=constants.BOT_SUPPLIER_SERVICE_SLUG)
    with patch(
        'plan.resources.tasks.create_or_update_bot_types'
    ), patch(
        'plan.resources.tasks.sync_service_resource'
    ), patch(
        'plan.resources.tasks.bot.get_associated_data'
    ) as get_data:
        get_data.return_value = {'servers': [], 'type_names': set()}
        if with_arg:
            sync_with_bot(service_slug='B')
        else:
            sync_with_bot()
        service_ids = {arg[0][0] for arg in get_data.call_args_list}
        if with_arg:
            assert service_ids == {b.id}
        else:
            assert service_ids == {a.id, c.id, bot.id}


@pytest.mark.parametrize('fail_count', [0, 1, 100])
def test_retry_grant_chunk_service_resources(fail_count):
    service = factories.ServiceFactory()
    resource_type = factories.ResourceTypeFactory()
    types = {'RT': resource_type}
    data_item = MagicMock()
    data_item.external_id = 'yay'
    data_item.type_name = 'RT'
    data = [data_item]
    found_error = None
    with patch('plan.resources.models.ServiceResource.does_other_resources_of_same_type_exists_on_service') as failer:
        results = [OperationalError for _ in range(fail_count)] + [False]
        failer.side_effect = results
        try:
            grant_chunk_service_resources(service, {('yay', 'RT')}, types, data)
        except OperationalError:
            found_error = True
    if fail_count <= 1:
        assert not found_error
        assert ServiceResource.objects.count() == 1
    else:
        assert found_error


def test_update_counters(counters_data, django_assert_num_queries):
    """
    Проверяем работу таски по обновлению счетчиков ресурсов

    Исходные данные
     - три типа ресурсов
     - два сервиса

     - у первого сервиса в базе есть счетички по всем трем ресурсам
     - у второго есть один

    В результате работы таски:
     - у первого сервиса один счетчик ресурса должен удалиться,
       другой обновиться на корректное значение, третий не поменяться
     - у второго - один счетчик должен создаться
    """
    assert ServiceResourceCounter.objects.get(
        service=counters_data.service_one,
        resource_type=counters_data.resource_type_one,
    ).count == 3

    assert ServiceResourceCounter.objects.filter(
        service=counters_data.service_one,
        resource_type=counters_data.resource_type_two,
    ).count() == 1

    assert ServiceResourceCounter.objects.get(
        service=counters_data.service_one,
        resource_type=counters_data.resource_type_three,
    ).count == 2

    assert ServiceResourceCounter.objects.filter(
        service=counters_data.service_two,
        resource_type=counters_data.resource_type_two,
    ).count() == 0

    with django_assert_num_queries(15):
        # получение измененных serviceresource
        # подсчет для типа количества активных выданных ресурсов по сервису по 1 типу
        # получение счетчиков для типа 1
        # обновление данных счетчика service_one-resource_type_one
        # подсчет для типа количества активных выданных ресурсов по сервису по 1 типу
        # получение счетчиков для типа 2
        # создание счетчиков нужных
        # удаление счетчиков не нужных
        # 7 запросов связанных с SAVEPOINT/unistat_taskmetric
        update_serviceresources_count(days_for=1)

    # у счетчика service_one-resource_type_one было значение 3, должно стать 10

    assert ServiceResourceCounter.objects.get(
        service=counters_data.service_one,
        resource_type=counters_data.resource_type_one,
    ).count == 10

    # счетчик service_one-resource_type_two должен удалиться, так как
    # активных ресурсов такого типа не осталось
    assert ServiceResourceCounter.objects.filter(
        service=counters_data.service_one,
        resource_type=counters_data.resource_type_two,
    ).count() == 0

    # у счетчика service_one-resource_type_three значение поменяться не должно
    # так как ресурс изменялся чем переданный в таску период
    assert ServiceResourceCounter.objects.get(
        service=counters_data.service_one,
        resource_type=counters_data.resource_type_three,
    ).count == 2

    # счетчик service_two-resource_type_two должен создаться с правильным значением
    assert ServiceResourceCounter.objects.get(
        service=counters_data.service_two,
        resource_type=counters_data.resource_type_two,
    ).count == 4


@pytest.fixture
def yql_query_mock():
    class FakeYQLRequest:
        def __init__(self):
            self.status = 'COMPLETED'
            self.run = MagicMock()

    with patch('yql.api.v1.client.YqlClient.query', return_value=FakeYQLRequest()) as _mock:
        yield _mock


def test_upload_gdpr_to_yt(yql_query_mock):
    attributes = {
        attribute: str(i)
        for i, attribute in enumerate(
            (
                'data', 'is_using', 'is_storing', 'subject',
                'store_for', 'store_in', 'purpose', 'path_to_data',
                'access_from_yql_sql', 'access_from_api',
                'access_from_web_cloud', 'access_from_file_based',
            )
        )
    }

    source_service = factories.ServiceFactory(name='"Название" с кавычками')
    attributes['service_from_slug'] = source_service.slug
    gdpr_resource_type = factories.ResourceTypeFactory(code=settings.GDPR_RESOURCE_TYPE_CODE)
    gdpr_resource = factories.ResourceFactory(type=gdpr_resource_type, attributes=attributes)
    service_resource = factories.ServiceResourceFactory(resource=gdpr_resource, state=ServiceResource.GRANTED)

    upload_gdpr_to_yt()

    yql_query_mock.assert_called_once_with(
        'USE hahn; INSERT INTO `home/abc/gdpr/{date}` ({columns}) VALUES ({values})'.format(
            date=date.today().isoformat(),
            columns=','.join((
                'destination_id',
                'destination_name',
                'source_id',
                'source_name',
                'data',
                'use_data',
                'store_data',
                'data_owner',
                'period_of_storage',
                'storage_location',
                'purpose_of_processing',
                'path_to_data',
                'access_from_yql_sql',
                'access_from_api',
                'access_from_web_cloud',
                'access_from_file_based',
            )),
            values=', '.join((
                f'{service_resource.service.id}',
                f'"{service_resource.service.name}"',
                f'{source_service.id}',
                '"Название с кавычками"',
                *(f'"{x}"' for x in range(len(attributes) - 1))
            ))
        )
    )
    yql_query_mock.return_value.run.assert_called_once()


def test_upload_gdpr_to_yt_nothing_to_upload(yql_query_mock):
    upload_gdpr_to_yt()

    yql_query_mock.assert_not_called()


@pytest.mark.parametrize('has_editable_tags', (True, False))
def test_sync_with_alert_provider(has_editable_tags):

    category = factories.ResourceTypeCategoryFactory(
        slug='provider_alerts',
    )
    resource_type = factories.ResourceTypeFactory(
        category=category,
        code='monitoring_project',
        has_editable_tags=has_editable_tags,
    )

    tag_category = factories.ResourceTagCategoryFactory(slug='environment')
    prod_tag = factories.ResourceTagFactory(
        category=tag_category,
        type='internal',
        slug='production',
    )
    test_tag = factories.ResourceTagFactory(
        category=tag_category,
        type='internal',
        slug='testing',
    )
    service = factories.ServiceFactory()

    # существующий ресурс - меняется окружение и меняется статус алертов
    data = [
        service.slug,  # abc slug
        {'projectId': '1'},  # resource_id
        'monitoring',  # service_provider_id
        'project',  # resource_type
        'smosker',  # responsible
        'PRODUCTION',  # environment
        {'alerts_status': {'some_alert_name': 'true', 'other': 'false'}},  # monitoring_stats
    ]

    resource = factories.ResourceFactory(
        type=resource_type,
        provider_hash='some_hash',
        name='projectId:1'
    )

    sr = factories.ServiceResourceFactory(
        resource=resource,
        type=resource_type,
        service=service,
        state=ServiceResource.GRANTED,
        has_monitoring=True,
    )
    sr.tags.add(test_tag)

    # добавляется новый ресурс
    data_new = [
        service.slug,
        {'projectId': '1', 'smth': 'test'},
        'monitoring',
        'project',
        'pixel',
        'TESTING',
        {'alerts_status': {'some_alert_name': 'true'}},
    ]

    # удаляется ресурс (в табличке его больше нет)
    resource_delete = factories.ResourceFactory(
        type=resource_type,
        provider_hash='some_hash3',
        name='projectId:100'
    )
    factories.ServiceResourceFactory(
        resource=resource_delete,
        type=resource_type,
        service=service,
        state=ServiceResource.GRANTED,
        has_monitoring=True,
    )

    class FakeYQLRequest:
        def __init__(self):
            self.status = 'COMPLETED'
            self.run = MagicMock()
            table_mock = MagicMock()
            table_mock.rows = (
                data,
                data_new,
            )
            self.get_results = MagicMock(return_value=[table_mock])

    with patch('yql.api.v1.client.YqlClient.query', return_value=FakeYQLRequest()):
        sync_with_alert_provider()

    sr.refresh_from_db()
    resource.refresh_from_db()
    assert resource.provider_hash == get_hash(*data)
    assert resource.attributes['responsible'] == 'smosker'
    assert resource.attributes['monitoring_stats'] == str(data[-1])
    assert sr.has_monitoring is False
    assert [tag.slug for tag in sr.tags.all()] == ([test_tag.slug] if has_editable_tags else [prod_tag.slug])

    sr_new = ServiceResource.objects.get(
        type=resource_type,
        service=service,
        resource__name='projectId:1,smth:test'
    )

    assert sr_new.has_monitoring is True
    assert sr_new.resource.provider_hash == get_hash(*data_new)
    assert sr_new.resource.attributes['responsible'] == 'pixel'
    assert sr_new.resource.attributes['resource_id'] == str(data_new[1])
    assert sr_new.resource.attributes['service_provider_id'] == data_new[2]
    assert sr_new.resource.attributes['resource_type'] == data_new[3]
    assert sr_new.resource.attributes['monitoring_stats'] == str(data_new[-1])
    assert [tag.slug for tag in sr_new.tags.all()] == [test_tag.slug]

    assert Resource.objects.filter(pk=resource_delete.id).first() is None


def test_sync_with_money_map():
    resource_type = factories.ResourceTypeFactory(code='blocks')
    tag = factories.ServiceTagFactory(slug='money_map')

    # удаляется ресурс (в табличке его больше нет)
    service_wo_resource = factories.ServiceFactory()
    service_wo_resource.tags.add(tag)
    sr_to_deprive = factories.ServiceResourceFactory(
        type=resource_type,
        service=service_wo_resource,
        state=ServiceResource.GRANTED,
    )

    # добавляется ресурс
    service = factories.ServiceFactory()
    data = [
        service.id,  # abc id
        [
            {
                'count': 13,
                'os': None,
                'platform': 'desktop',
            },
        ],  # attributes
        'https://moneymap.yandex-team.ru/blocks/?abc_oebs_id=69',  # link
    ]

    # изменяется ресурс
    service_change = factories.ServiceFactory()
    attributes = [
        {
            'count': 25,
            'os': None,
        },
    ]
    resource = factories.ResourceFactory(
        type=resource_type,
        link='https://moneymap.yandex-team.ru/blocks/?abc_oebs_id=42',
        attributes={
            'attributes': attributes,
        }
    )
    sr = factories.ServiceResourceFactory(
        resource=resource,
        type=resource_type,
        service=service_change,
        state=ServiceResource.DEPRIVED,
    )
    data_change = [
        service_change.id,  # abc id
        [
            {
                'count': 25,
                'os': None,
            },
        ],  # attributes
        'https://moneymap.yandex-team.ru/blocks/?abc_oebs_id=999',  # link
    ]

    class FakeYQLRequest:
        def __init__(self):
            self.status = 'COMPLETED'
            self.run = MagicMock()
            table_mock = MagicMock()
            table_mock.rows = (
                data,
                data_change,
            )
            self.get_results = MagicMock(return_value=[table_mock])

    with patch('yql.api.v1.client.YqlClient.query', return_value=FakeYQLRequest()):
        sync_with_money_map(resource_code='blocks')

    sr_new = ServiceResource.objects.get(
        type=resource_type,
        service=service,
    )
    assert sr_new.resource.attributes['attributes'][0]['count'] == 13
    assert sr_new.resource.link == 'https://moneymap.yandex-team.ru/blocks/?abc_oebs_id=69'
    assert service.tags.filter(slug=tag.slug).exists()

    sr_to_deprive.refresh_from_db()
    assert sr_to_deprive.state == ServiceResource.DEPRIVED
    assert not service_wo_resource.tags.count()

    sr.refresh_from_db()
    resource.refresh_from_db()

    assert sr.state == ServiceResource.GRANTED
    assert resource.attributes['attributes'][0]['count'] == 25
    assert resource.link == 'https://moneymap.yandex-team.ru/blocks/?abc_oebs_id=999'
    assert service_change.tags.filter(slug=tag.slug).exists()
