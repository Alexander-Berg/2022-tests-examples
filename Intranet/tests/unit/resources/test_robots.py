import json

from django.conf import settings
from django.urls import reverse
from mock import patch, call
import pytest
from library.python.vault_client.errors import ClientError

from plan.internal_roles.utils import assign_perms_for_internal_role
from plan.resources.models import Resource, ServiceResource
from plan.resources.tasks import grant_resource
from common import factories
from utils import Response


pytestmark = [
    pytest.mark.django_db,
    pytest.mark.usefixtures('robot'),
]


@pytest.fixture
def robot_resource_type(owner_role):
    resource_type = factories.ResourceTypeFactory(
        code='staff-robot', supplier_plugin='robots',
        has_multiple_consumers=True, has_automated_grant=True,
    )
    resource_type.consumer_roles.add(owner_role)
    return resource_type


class FakeVaultClient(object):
    def __init__(self, status=True, secret_roles=None, *args, **kwargs):
        self.status = status
        self.secret_dict = {
            'secret_roles': secret_roles
        }
        self.delete_roles_count = 0

    def add_user_role_to_secret(self, *args, **kwargs):
        return self.status

    def delete_user_role_from_secret(self, *args, **kwargs):
        self.delete_roles_count+=1
        return self.status

    def get_secret(self, *args, **kwargs):
        return self.secret_dict

    def update_secret(self, *args, **kwargs):
        return self.status


def test_permissions_denied(client, staff_factory):
    staff = staff_factory()
    client.login(staff.login)

    response = client.json.post(
        reverse('resources-api:robots-list'),
        {'service': 1, 'robot': 'login', 'secret_id': '123'}
    )
    assert response.status_code == 403


def test_bad_service(client, staff_factory):
    staff = staff_factory()
    assign_perms_for_internal_role('robot_creator', staff)
    client.login(staff.login)

    response = client.json.post(
        reverse('resources-api:robots-list'),
        {'service': 1, 'robot': 'login', 'secret_id': '123'}
    )
    assert response.status_code == 400
    assert response.json()['error']['extra']['service'] == ['Invalid pk "1" - object does not exist.']


@pytest.mark.parametrize('author_status', ['in_service', 'not_in_service', None])
@pytest.mark.parametrize('is_robot', [True, False])
@pytest.mark.parametrize('is_dismissed', [True, False])
@pytest.mark.parametrize('with_users', [True, False])
def test_create(client, robot_resource_type, author_status, staff_factory, is_robot, is_dismissed, with_users):
    service = factories.ServiceFactory()
    staff = staff_factory()
    assign_perms_for_internal_role('robot_creator', staff)
    client.login(staff.login)
    author = factories.StaffFactory(login='author')
    scope = factories.RoleScopeFactory(slug=settings.ABC_ROBOTS_MANAGEMENT_SCOPE)
    factories.RoleFactory(code=settings.ABC_ROBOTS_MANAGER_ROLE, scope=scope)
    user_scope = factories.RoleScopeFactory(slug=settings.ABC_ROBOTS_USERS_SCOPE)
    robot = factories.StaffFactory(login='login', is_robot=is_robot, is_dismissed=is_dismissed)

    if author_status == 'in_service':
        factories.ServiceMemberFactory(staff=author, service=service)
    if with_users:
        user_group = factories.ServiceScopeFactory(service=service, role_scope=user_scope)
        user_data = {
            'comment': 'Выдача ресурса робота login',
            'path': '/robots/{}/user/'.format(robot.staff_id),
            'group': user_group.staff_id,
            'system': 'staff',
            'fields_data': {},
            'no_meta': True,
        }

    group = factories.ServiceScopeFactory(service=service, role_scope=scope)
    url = 'rolerequests/'
    data = {
        'comment': 'Выдача ресурса робота login',
        'path': '/robots/{}/owner/'.format(robot.staff_id),
        'group': group.staff_id,
        'system': 'staff',
        'fields_data': {},
        'no_meta': True,
    }

    assert Resource.objects.count() == 0

    with patch('plan.resources.suppliers.robots._get_vault_client') as _get_vault_client,\
            patch('plan.idm.manager.Manager.post') as post,\
            patch('plan.api.idm.actions.request_membership') as request_membership:
        post.return_value = {'id': 10101}

        _get_vault_client.return_value = FakeVaultClient()
        create_data = {'service': service.id, 'robot': 'login', 'secret_id': '123'}
        if author_status:
            create_data['owner'] = 'author'
        response = client.json.post(reverse('resources-api:robots-list'), create_data)

    if not is_robot or is_dismissed:
        assert response.status_code == 400
    else:
        if with_users:
            assert post.call_args_list == [
                call(url, data=data),
                call(url, data=user_data),
            ]
        else:
            assert post.call_args_list == [call(url, data=data)]
        if author_status:
            assert request_membership.call_count == 1
        assert response.status_code == 201
        resource = Resource.objects.get()

        assert resource.type == robot_resource_type
        assert resource.external_id == 'login'
        assert resource.attributes == {
            'secret_id': {
                'type': 'link',
                'value': '123',
                'url': 'https://yav-test.yandex-team.ru/secret/123/'
            }
        }


@pytest.mark.parametrize('side_effect', [None, ClientError])
def test_consume(client, robot_resource_type, owner_role, side_effect, staff_factory):
    owner = staff_factory()
    service = factories.ServiceFactory(owner=owner)
    scope = factories.RoleScopeFactory(slug=settings.ABC_ROBOTS_MANAGEMENT_SCOPE)
    group = factories.ServiceScopeFactory(service=service, role_scope=scope)
    factories.ServiceMemberFactory(staff=owner, service=service, role=owner_role)
    factories.ServiceMemberFactory(service=service, role__scope=scope)
    resource = factories.ResourceFactory(
        type=robot_resource_type,
        external_id='login',
        attributes={'secret_id': {'value': 'value'}}
    )

    robot = factories.StaffFactory(login='login', is_robot=True)

    service_resource = factories.ServiceResourceFactory(resource=resource)
    client.login(owner.login)
    with patch('plan.idm.manager.Manager.post') as post:
        with patch('plan.resources.suppliers.robots._get_vault_client') as _get_vault_client:
            post.return_value = {'id': 10101}
            _get_vault_client.return_value = FakeVaultClient()

            response = client.json.post(
                reverse('resources-api:serviceresources-list'),
                {
                    'service': service.id,
                    'resource': resource.id,
                }
            )

            url = 'rolerequests/'
            data = {
                'comment': 'Выдача ресурса робота login',
                'path': '/robots/{}/owner/'.format(robot.staff_id),
                'group': group.staff_id,
                'system': 'staff',
                'fields_data': {},
                'no_meta': True,
            }
            assert post.call_args_list == [call(url, data=data)]

    assert response.status_code == 201
    service_resource = ServiceResource.objects.exclude(id=service_resource.id).get()
    assert service_resource.service == service
    assert service_resource.resource == resource
    assert service_resource.state == ServiceResource.GRANTED

    with patch('plan.idm.manager.Manager.post') as post:
        grant_resource(service_resource.id)
        assert post.call_args is None

    role = factories.RoleFactory(code=settings.ABC_ROBOTS_MANAGER_ROLE, scope=scope)
    factories.ServiceMemberFactory(staff=owner, service=service, role=role)

    service_resource.refresh_from_db()
    with patch('plan.idm.manager.Manager.post') as post:
        with patch('plan.resources.suppliers.robots._get_vault_client') as _get_vault_client:
            post.return_value = {'id': 10101}
            if side_effect:
                _get_vault_client.side_effect = side_effect()
            else:
                _get_vault_client.return_value = FakeVaultClient()
            grant_resource(service_resource.id)

    service_resource.refresh_from_db()
    if not side_effect:
        assert service_resource.attributes['staff_role_state'] == 'ok'
        assert service_resource.attributes['secret_role_state'] == 'ok'


def test_edit_wrong_service(client, robot_resource_type, staff_factory):
    service = factories.ServiceFactory()
    staff = staff_factory()
    assign_perms_for_internal_role('robot_creator', staff)
    client.login(staff.login)

    resource = factories.ResourceFactory(type=robot_resource_type, name='login')
    factories.ServiceResourceFactory(resource=resource)
    with patch('plan.resources.suppliers.robots._get_vault_client') as _get_vault_client:
        _get_vault_client.return_value = FakeVaultClient()
        response = client.json.post(
            reverse('resources-api:robots-list'),
            {'service': service.id, 'robot': 'login', 'secret_id': '123'}
        )

    assert response.status_code == 400
    assert response.json()['error']['extra'] == ['This robot is already created']


def test_edit(client, robot_resource_type, staff_factory):
    service = factories.ServiceFactory()
    staff = staff_factory()
    assign_perms_for_internal_role('robot_creator', staff)
    client.login(staff.login)

    resource = factories.ResourceFactory(
        type=robot_resource_type,
        name='login',
        attributes={'secret_id': {'value': '1'}}
    )
    factories.ServiceResourceFactory(resource=resource, service=service)
    with patch('plan.resources.suppliers.robots._get_vault_client') as _get_vault_client:
        _get_vault_client.return_value = FakeVaultClient()
        response = client.json.post(
            reverse('resources-api:robots-list'),
            {'service': service.id, 'robot': 'login', 'secret_id': '123'}
        )

    assert response.status_code == 201
    resource.refresh_from_db()
    assert resource.attributes == {'secret_id': {'url': 'https://yav-test.yandex-team.ru/secret/123/', 'value': '123'}}


def test_error(client, robot_resource_type, staff_factory):
    service = factories.ServiceFactory()
    staff = staff_factory()
    assign_perms_for_internal_role('robot_creator', staff)
    client.login(staff.login)

    scope = factories.RoleScopeFactory(slug=settings.ABC_ROBOTS_MANAGEMENT_SCOPE)
    role = factories.RoleFactory(code=settings.ABC_ROBOTS_MANAGER_ROLE, scope=scope)
    factories.ServiceMemberFactory(staff=staff, service=service, role=role)

    factories.ServiceScopeFactory(service=service, role_scope=scope)
    factories.StaffFactory(login='login', is_robot=True)

    assert Resource.objects.count() == 0
    with patch('plan.resources.suppliers.robots._get_vault_client') as _get_vault_client:
        with patch('plan.idm.manager.Manager.post') as post:
            post.return_value = {'id': 10101}

            _get_vault_client.return_value = FakeVaultClient(status=False)
            response = client.json.post(
                reverse('resources-api:robots-list'),
                {'service': service.id, 'robot': 'login', 'secret_id': '123'}
            )

    assert response.status_code == 500
    assert response.json()['error']['detail'] == "Didn\'t grant access to robot password to {'abc_scope': 'robots_management'}"


def test_reset_password(client, robot_resource_type, staff_factory):
    staff = staff_factory()
    role = factories.RoleFactory(code=settings.ABC_ROBOTS_MANAGER_ROLE)
    service = factories.ServiceFactory()
    factories.ServiceMemberFactory(service=service, staff=staff, role=role)
    resource = factories.ResourceFactory(
        type=robot_resource_type,
        attributes={'secret_id': {'value': 'id'}, 'login': 'login'}
    )
    service_resource = factories.ServiceResourceFactory(resource=resource, state='granted', service=service)
    client.login(staff.login)
    with patch('plan.common.utils.http.Session.post') as post, \
            patch('plan.resources.suppliers.robots.yenv') as yenv:
        yenv.type = 'production'
        post.return_value = Response(200, json.dumps({'result': {'status': 'done', 'lastUpdate': '2018-01-01'}}))
        response = client.json.post(
            reverse('resources-api:serviceresources-actions', args=[service_resource.id]),
            {'action': 'reset_password'}
        )
    assert response.status_code == 200
    resource.refresh_from_db()
    attributes = resource.attributes
    assert attributes['password_status'] == 'ok'
    assert attributes['password_updated_at'] == '2018-01-01'


def test_reset_password_403(client, robot_resource_type):
    factories.RoleFactory(code=settings.ABC_ROBOTS_MANAGER_ROLE)
    resource = factories.ResourceFactory(type=robot_resource_type)
    service_resource = factories.ServiceResourceFactory(resource=resource, state='granted',)
    response = client.json.post(
        reverse('resources-api:serviceresources-actions', args=[service_resource.id]),
        {'action': 'reset_password'}
    )
    assert response.status_code == 403


@pytest.mark.parametrize('has_secret_role', [True, False])
def test_robots_manager_can_delete(client, robot_resource_type, django_assert_num_queries, has_secret_role, staff_factory):
    service = factories.ServiceFactory()
    role = factories.RoleFactory(code=settings.ABC_ROBOTS_MANAGER_ROLE)
    membership = factories.ServiceMemberFactory(service=service, role=role, staff=staff_factory())
    resource = factories.ResourceFactory(
        type=robot_resource_type,
        external_id='login',
        attributes={'secret_id': {'value': '123'}}
    )
    service_resource = factories.ServiceResourceFactory(
        resource=resource,
        service=service,
        state='granted',
        attributes={'role_id': ['10101', '100056']}
    )
    factories.ServiceResourceFactory(
        resource=resource,
        state='granted',
        attributes={'role_id': '10105'}
    )
    client.login(membership.staff.login)

    secret_roles = []
    if has_secret_role:
        secret_roles = [
            {
                'abc_id': service.id,
                'abc_scope': settings.ABC_ROBOTS_MANAGEMENT_SCOPE,
                'role_slug': 'READER',
            },
            {
                'abc_id': service.id,
                'abc_role': settings.ABC_ROBOTS_USERS_ROLE_ID,
                'role_slug': 'READER',
            },
        ]

    with patch('plan.resources.suppliers.robots._get_vault_client') as _get_vault_client:
        with patch('plan.idm.manager.Manager.delete') as delete:
            with patch('plan.idm.manager.Manager.get') as get:
                delete.return_value = Response(204, '{}')
                get.return_value = {'state': 'granted'}
                vault_client = FakeVaultClient(secret_roles=secret_roles)
                _get_vault_client.return_value = vault_client
                with django_assert_num_queries(14):
                    # 1 select intranet_staff join auth_user
                    # 1 select middleware
                    # 1 select serviceresource
                    # 1 select resourcetype
                    # 1 select service
                    # 1 select role
                    # 1 select servicemember
                    # 1 select resource
                    # 1 select serviceresource проверка наличия других ресурсов такого типа на сервисе
                    # 1 select serviceresource проверка наличия других активных ресурсов с этим роботом
                    # 1 select servicetag для удаления тегов
                    # 1 update serviceresource
                    # 1 pg_is_in_recovery()
                    # 1 select в waffle readonly switch

                    response = client.json.delete(
                        reverse('resources-api:serviceresources-detail', args=[service_resource.id]),
                    )

            assert delete.call_args_list == [
                call('roles/10101/', data={}),
                call('roles/100056/', data={})
            ]

            assert vault_client.delete_roles_count == (2 if has_secret_role else 0)

    assert response.status_code == 200


def test_owner_can_not_delete(client, robot_resource_type, owner_role, staff_factory):
    service = factories.ServiceFactory()
    factories.RoleFactory(code=settings.ABC_ROBOTS_MANAGER_ROLE)
    membership = factories.ServiceMemberFactory(service=service, role=owner_role, staff=staff_factory())
    resource = factories.ResourceFactory(type=robot_resource_type)
    service_resource = factories.ServiceResourceFactory(resource=resource, service=service, state='granted')
    client.login(membership.staff.login)
    response = client.json.delete(
        reverse('resources-api:serviceresources-detail', args=[service_resource.id]),
    )
    assert response.status_code == 403


def test_create_no_robot_staff(client, robot_resource_type, staff_factory):
    service = factories.ServiceFactory()
    staff = staff_factory()
    assign_perms_for_internal_role('robot_creator', staff)
    client.login(staff.login)
    author = factories.StaffFactory(login='author')
    role_scope = factories.RoleScopeFactory(slug=settings.ABC_ROBOTS_MANAGEMENT_SCOPE)
    role = factories.RoleFactory(code=settings.ABC_ROBOTS_MANAGER_ROLE, scope=role_scope)
    factories.ServiceMemberFactory(staff=author, service=service)

    factories.ServiceScopeFactory(service=service, role_scope=role_scope)
    factories.ServiceMemberFactory(service=service, role=role)

    assert Resource.objects.count() == 0

    create_data = {
        'service': service.id,
        'robot': 'login',
        'secret_id': '123',
        'owner': 'author'
    }
    with patch('plan.idm.manager.Manager.post') as post:
        with patch('plan.resources.suppliers.robots._get_vault_client') as _get_vault_client:
            post.return_value = {'id': 10101}
            _get_vault_client.return_value = FakeVaultClient()

            response = client.json.post(reverse('resources-api:robots-list'), create_data)

    assert response.status_code == 201
    resource = Resource.objects.get()

    assert resource.type == robot_resource_type
    assert resource.external_id == 'login'
    assert resource.attributes == {
        'secret_id': {
            'type': 'link',
            'value': '123',
            'url': 'https://yav-test.yandex-team.ru/secret/123/'
        }
    }

    service_resourse = ServiceResource.objects.get()
    assert service_resourse.state == ServiceResource.GRANTING

    # после появления стаффа робота роль будет выдана после запуска таски
    # но только если у него тип - робот
    robot = factories.StaffFactory(login='login')
    with patch('plan.idm.manager.Manager.post') as post:
        with patch('plan.resources.suppliers.robots._get_vault_client') as _get_vault_client:
            post.return_value = {'id': 10101}
            _get_vault_client.return_value = FakeVaultClient()

            grant_resource(service_resourse.id)

    service_resourse.refresh_from_db()
    assert service_resourse.state == ServiceResource.GRANTING

    # проверим что если ретраи кончились - будет ошибка
    service_resourse.grant_retries = 1
    service_resourse.save()

    with patch('plan.idm.manager.Manager.post') as post:
        with patch('plan.resources.suppliers.robots._get_vault_client') as _get_vault_client:
            post.return_value = {'id': 10101}
            _get_vault_client.return_value = FakeVaultClient()

            grant_resource(service_resourse.id)

    service_resourse.refresh_from_db()
    assert service_resourse.state == ServiceResource.ERROR

    # а если is_robot=True - выдача должна пройти успешно
    robot.is_robot = True
    robot.save()
    service_resourse.grant_retries = 1
    service_resourse.state = ServiceResource.GRANTING
    service_resourse.save()
    with patch('plan.idm.manager.Manager.post') as post:
        with patch('plan.resources.suppliers.robots._get_vault_client') as _get_vault_client:
            post.return_value = {'id': 10101}
            _get_vault_client.return_value = FakeVaultClient()

            grant_resource(service_resourse.id)

    service_resourse.refresh_from_db()
    assert service_resourse.state == ServiceResource.GRANTED


def test_robots_approver_can_delete(client, robot_resource_type, django_assert_num_queries, staff_factory):
    role = factories.RoleFactory(code='test_role')
    robot_resource_type.supplier_roles.add(role)
    supplier_manager = factories.ServiceMemberFactory(
        service=robot_resource_type.supplier,
        role=role,
        staff=staff_factory()
    )
    resource = factories.ResourceFactory(
        type=robot_resource_type,
        external_id='login',
        attributes={'secret_id': {'value': '123'}}
    )
    service = factories.ServiceFactory()
    service_resource = factories.ServiceResourceFactory(
        resource=resource,
        service=service,
        state='granted',
        attributes={'role_id': '10101'}
    )
    factories.ServiceResourceFactory(
        resource=resource,
        state='granted',
        attributes={'role_id': '10105'}
    )
    factories.RoleFactory(code=settings.ABC_ROBOTS_MANAGER_ROLE)

    secret_roles = [
        {
            'abc_id': service.id,
            'abc_scope': settings.ABC_ROBOTS_MANAGEMENT_SCOPE,
            'role_slug': 'READER',
        },
    ]

    client.login(supplier_manager.staff.login)
    with patch('plan.resources.suppliers.robots._get_vault_client') as _get_vault_client:
        with patch('plan.idm.manager.Manager.delete') as delete:
            with patch('plan.idm.manager.Manager.get') as get:
                delete.return_value = Response(204, '{}')
                get.return_value = {'state': 'granted'}
                _get_vault_client.return_value = FakeVaultClient(secret_roles=secret_roles)

                response = client.json.delete(
                    reverse('resources-api:serviceresources-detail', args=[service_resource.id]),
                )
            assert delete.call_args_list == [call('roles/10101/', data={})]
    assert response.status_code == 200
