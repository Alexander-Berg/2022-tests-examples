from functools import partial
import json
import os

import pytest
from django.conf import settings
from django.contrib.auth.models import Permission
from django.core.urlresolvers import reverse
from mock import patch

from plan.internal_roles.models import InternalRole
from plan.resources.models import Resource
from plan.services.models import ServiceMember, ServiceMemberDepartment
from plan.services.state import SERVICEMEMBER_STATE
from plan.staff.models import Staff
from common import factories
from plan.roles.models import Role
from utils import source_path

pytestmark = pytest.mark.django_db(transaction=True)

STANDARTS_DIR = 'intranet/plan/tests/test_data/idm_api'


@pytest.fixture
def base_data_hooks():
    service1 = factories.ServiceFactory(slug='_slug1', path='/slug1/')
    service2 = factories.ServiceFactory(slug='_slug2', path='/slug1/slug2/', parent=service1)

    dev_scope = factories.RoleScopeFactory(slug='development')
    developer = factories.RoleFactory(name='Разработчик', name_en='Developer', scope=dev_scope, service=service1)
    support_scope = factories.RoleScopeFactory(slug='support')
    support = factories.RoleFactory(name='Поддержка', name_en='Support', scope=support_scope)
    management_scope = factories.RoleScopeFactory(slug='management')
    product_head = factories.RoleFactory(
        name='Руководитель сервиса',
        name_en='Head',
        scope=management_scope,
        code=Role.EXCLUSIVE_OWNER,
        service=service1,
    )

    return {
        'service1': service1,
        'service2': service2,
        'support': support,
        'developer': developer,
        'product_head': product_head,
    }


def test_add_role(base_data_hooks, client, person, robot):
    url = reverse('idm-api:add-role')

    response = client.post(url, data={
        'login': person.login,
        'role': json.dumps({
            'type': 'services',
            'services_key': '_slug1',
            '_slug1_key': '_slug2',
            '_slug2_key': '*',
            'role': base_data_hooks['support'].pk,
        })
    })

    assert response.status_code == 200

    assert ServiceMember.objects.count() == 1

    membership = ServiceMember.objects.get()

    assert membership.service == base_data_hooks['service2']
    assert membership.staff == person
    assert membership.role_id == base_data_hooks['support'].pk
    assert membership.role.scope.slug == 'support'


def test_add_role_with_existing_inactive(base_data_hooks, client, person, robot):
    url = reverse('idm-api:add-role')
    factories.ServiceMemberFactory(
        staff=person,
        service=base_data_hooks['service2'],
        role=base_data_hooks['support'],
        state=ServiceMember.states.DEPRIVED,
    )

    response = client.post(url, data={
        'login': person.login,
        'role': json.dumps({
            'type': 'services',
            'services_key': '_slug1',
            '_slug1_key': '_slug2',
            '_slug2_key': '*',
            'role': base_data_hooks['support'].pk,
        })
    })

    assert response.status_code == 200

    assert ServiceMember.objects.count() == 1

    membership = ServiceMember.objects.get()

    assert membership.service == base_data_hooks['service2']
    assert membership.staff == person
    assert membership.role_id == base_data_hooks['support'].pk
    assert membership.role.scope.slug == 'support'
    assert membership.state == ServiceMember.states.ACTIVE


def test_add_group_role_with_existing_inactive(base_data_hooks, client, person, robot):
    url = reverse('idm-api:add-role')

    department = factories.DepartmentFactory()
    factories.ServiceMemberDepartmentFactory(
        department=department,
        service=base_data_hooks['service2'],
        role=base_data_hooks['support'],
        state=ServiceMember.states.DEPRIVED,
    )

    with patch('plan.services.tasks.update_service_department_members') as update_service_department_members:
        response = client.post(url, data={
            'group': department.staff_id,
            'role': json.dumps({
                'type': 'services',
                'services_key': '_slug1',
                '_slug1_key': '_slug2',
                '_slug2_key': '*',
                'role': base_data_hooks['support'].pk,
            })
        })

    assert response.status_code == 200
    assert response.json()['code'] == 0
    assert ServiceMemberDepartment.objects.count() == 1
    service_department = ServiceMemberDepartment.objects.get()
    assert service_department.state == ServiceMemberDepartment.states.ACTIVE
    assert update_service_department_members.apply_async_on_commit.called


def test_add_role_after_adding_group_role(base_data_hooks, client, person, robot):
    member_deparment = factories.ServiceMemberDepartmentFactory()
    factories.ServiceMemberFactory(
        from_department=member_deparment,
        role=Role.objects.get(pk=base_data_hooks['support'].pk),
        service=base_data_hooks['service2'],
        staff=person,
    )
    assert ServiceMember.objects.count() == 1

    url = reverse('idm-api:add-role')
    response = client.post(url, data={
        'login': person.login,
        'role': json.dumps({
            'type': 'services',
            'services_key': '_slug1',
            '_slug1_key': '_slug2',
            '_slug2_key': '*',
            'role': base_data_hooks['support'].pk,
        })
    })

    assert response.status_code == 200
    assert ServiceMember.objects.count() == 2


def test_add_role_returns_warning_on_duplicates(base_data_hooks, client, person, robot):
    url = reverse('idm-api:add-role')

    post_data = {
        'login': person.login,
        'role': json.dumps({
            'type': 'services',
            'services_key': '_slug1',
            '_slug1_key': '_slug2',
            '_slug2_key': '*',
            'role': base_data_hooks['support'].pk,
        })
    }

    request = partial(client.post, url, post_data)

    response = request()
    assert response.status_code == 200
    assert response.json()['code'] == 0
    assert ServiceMember.objects.count() == 1

    response = request()
    assert response.status_code == 200

    error = response.json()
    assert error['code'] == 1
    assert 'warning' in error


def test_add_group_role(base_data_hooks, client, person, robot):
    url = reverse('idm-api:add-role')

    department = factories.DepartmentFactory()

    with patch('plan.services.tasks.update_service_department_members') as update_service_department_members:
        response = client.post(url, data={
            'group': department.staff_id,
            'role': json.dumps({
                'type': 'services',
                'services_key': '_slug1',
                '_slug1_key': '_slug2',
                '_slug2_key': '*',
                'role': base_data_hooks['support'].pk,
            })
        })

    assert response.status_code == 200
    assert response.json()['code'] == 0
    assert ServiceMemberDepartment.objects.count() == 1
    assert update_service_department_members.apply_async_on_commit.called


def test_add_group_role_raises_warning_on_duplicate(base_data_hooks, client, person, robot):
    url = reverse('idm-api:add-role')

    department = factories.DepartmentFactory()

    post_data = {
        'group': department.staff_id,
        'role': json.dumps({
            'type': 'services',
            'services_key': '_slug1',
            '_slug1_key': '_slug2',
            '_slug2_key': '*',
            'role': base_data_hooks['support'].pk,
        })
    }

    request = partial(client.post, url, post_data)

    response = request()
    assert response.status_code == 200
    assert response.json()['code'] == 0
    assert ServiceMemberDepartment.objects.count() == 1

    response = request()
    assert response.status_code == 200
    error = response.json()
    assert error['code'] == 1
    assert 'warning' in error


def test_remove_group_role_with_resource(base_data_hooks, client, robot):
    resource = factories.ResourceFactory()
    department = factories.DepartmentFactory()
    service_member_department = factories.ServiceMemberDepartmentFactory(
        service=base_data_hooks['service2'],
        role=base_data_hooks['support'],
        department=department,
        resource=resource
    )
    plain_service_member_department = factories.ServiceMemberDepartmentFactory(
        service=base_data_hooks['service2'],
        role=base_data_hooks['support'],
        department=department,
    )

    response = client.post(
        reverse('idm-api:remove-role'),
        data={
            'group': department.staff_id,
            'role': json.dumps({
                'type': 'services',
                'services_key': '_slug1',
                '_slug1_key': '_slug2',
                '_slug2_key': '*',
                'role': base_data_hooks['support'].pk,
            })
        }
    )
    assert response.status_code == 200

    assert Resource.objects.filter(pk=resource.id).exists()
    assert ServiceMemberDepartment.objects.filter(pk=service_member_department.id).exists()
    plain_service_member_department = ServiceMemberDepartment.all_states.get(pk=plain_service_member_department.id)
    assert plain_service_member_department.state == SERVICEMEMBER_STATE.DEPRIVED


def test_remove_group_role_with_part_rate(base_data_hooks, client, robot, owner_role, person):

    owner = factories.ServiceMemberFactory(service=base_data_hooks['service1'], role=owner_role, staff=person)

    department = factories.DepartmentFactory()
    staff = factories.StaffFactory(department=department)

    department_member = factories.ServiceMemberDepartmentFactory(
        service=base_data_hooks['service1'],
        department=department
    )
    service_member = factories.ServiceMemberFactory(
        service=base_data_hooks['service1'],
        staff=staff,
        role=department_member.role,
        from_department=department_member
    )

    part_rate = factories.PartRateHistoryFactory(
        service_member=service_member,
        staff=owner.staff,
        new_part_rate=0.5
    )

    client.login(owner.staff.login)

    response = client.post(
        reverse('idm-api:remove-role'),
        data={
            'group': department.staff_id,
            'role': json.dumps({
                'type': 'services',
                'services_key': '_slug1',
                '_slug1_key': '*',
                'role': department_member.role.pk,
            })
        }
    )
    assert response.status_code == 200

    part_rate.refresh_from_db()
    assert part_rate.service_member_id == service_member.id
    service_member = ServiceMember.all_states.get(pk=service_member.id)
    department = ServiceMemberDepartment.all_states.get(pk=department_member.id)
    assert service_member.state == department.state == SERVICEMEMBER_STATE.DEPRIVED


def test_remove_role_with_resource(base_data_hooks, client, person, robot):
    resource = factories.ResourceFactory()
    service_member = factories.ServiceMemberFactory(
        service=base_data_hooks['service2'],
        role=base_data_hooks['support'],
        staff=person,
        resource=resource
    )
    plain_service_member = factories.ServiceMemberFactory(
        service=base_data_hooks['service2'],
        role=base_data_hooks['support'],
        staff=person,
    )

    response = client.post(
        reverse('idm-api:remove-role'),
        data={
            'login': person.login,
            'role': json.dumps({
                'type': 'services',
                'services_key': '_slug1',
                '_slug1_key': '_slug2',
                '_slug2_key': '*',
                'role': base_data_hooks['support'].pk,
            })
        }
    )
    assert response.status_code == 200

    assert Resource.objects.filter(pk=resource.id).exists()
    assert ServiceMember.objects.filter(pk=service_member.id).exists()
    plain_service_member = ServiceMember.all_states.get(pk=plain_service_member.id)
    assert plain_service_member.state == SERVICEMEMBER_STATE.DEPRIVED


def test_notify_staff_on_add_role(base_data_hooks, client, person, robot):
    url = reverse('idm-api:add-role')

    with patch('plan.services.tasks.notify_staff') as notify:
        response = client.post(url, data={
            'login': person.login,
            'role': json.dumps({
                'type': 'services',
                'services_key': '_slug1',
                '_slug1_key': '_slug2',
                '_slug2_key': '*',
                'role': base_data_hooks['support'].pk,
            })
        })

    assert response.status_code == 200
    assert notify.delay_on_commit.called


def test_add_role_with_resource(base_data_hooks, client, person, robot):
    test_type = factories.ResourceTypeFactory(supplier=base_data_hooks['service2'], name='test_type')
    test_resource = factories.ResourceFactory(name='test_resource', external_id='1', type=test_type)

    url = reverse('idm-api:add-role')

    response = client.post(url, data={
        'login': person.username,
        'role': json.dumps({
            'type': 'services',
            'services_key': '_slug1',
            '_slug1_key': '_slug2',
            '_slug2_key': '*',
            'role': base_data_hooks['support'].pk,
        }),
        'fields': json.dumps({
            'resource': str(test_resource.pk),
        }),
    })

    assert response.status_code == 200

    assert ServiceMember.objects.count() == 1

    membership = ServiceMember.objects.get()

    assert membership.service == base_data_hooks['service2']
    assert membership.staff == person
    assert membership.role_id == base_data_hooks['support'].pk
    assert membership.role.scope.slug == 'support'
    assert membership.resource == test_resource


def test_add_unknown_service(base_data_hooks, client, person):
    url = reverse('idm-api:add-role')
    support_role_id = base_data_hooks['support'].pk

    response = client.post(url, data={
        'login': person.username,
        'role': json.dumps({
            'type': 'services',
            'services_key': '_slug1',
            '_slug1_key': 'xxx',
            'xxx_key': '*',
            'role': support_role_id,
        })
    })

    assert json.loads(response.content) == {
        'fatal': f'Роль: Unknown service xxx for role {support_role_id}',
        'code': 400
    }


def test_add_unknown_role(base_data_hooks, client, person):
    url = reverse('idm-api:add-role')

    response = client.post(url, data={
        'login': person.username,
        'role': json.dumps({
            'type': 'services',
            'services_key': '_slug1',
            '_slug1_key': '_slug2',
            '_slug2_key': '*',
            'role': 999,  # нет такой роли
        })
    })

    assert json.loads(response.content) == {
        'fatal': "Роль: Cannot find role 999 in role_data {'type': 'services', 'services_key': '_slug1', '_slug1_key': '_slug2', '_slug2_key': '*', 'role': 999}",  # noqa
        'code': 400
    }


def test_add_bad_group(base_data_hooks, client, person, robot):
    department = factories.DepartmentFactory()
    url = reverse('idm-api:add-role')

    response = client.post(url, data={
        'group': department.staff_id + 1,
        'role': json.dumps({
            'type': 'services',
            'services_key': '_slug1',
            '_slug1_key': '_slug2',
            '_slug2_key': '*',
            'role': base_data_hooks['support'].pk,
        })
    })

    assert json.loads(response.content) == {
        'fatal': (
            f'Группа: Роль может быть запрошена только на департаментную группу. '
            f'Департаментная группа с id={department.staff_id + 1} не найдена'
        ),
        'code': 400,
    }


def test_remove_role(base_data_hooks, client, person):
    url = reverse('idm-api:remove-role')
    membership = ServiceMember.objects.create(
        state=ServiceMember.states.ACTIVE,
        service=base_data_hooks['service2'],
        role=base_data_hooks['support'],
        staff=person
    )

    response = client.post(url, data={
        'login': person.username,
        'role': json.dumps({
            'type': 'services',
            'services_key': '_slug1',
            '_slug1_key': '_slug2',
            '_slug2_key': '*',
            'role': base_data_hooks['support'].pk,
        })
    })
    assert response.status_code == 200
    assert ServiceMember.objects.count() == 0
    membership = ServiceMember.all_states.get(pk=membership.id)
    assert membership.state == SERVICEMEMBER_STATE.DEPRIVED


@patch('plan.api.idm.actions.request_membership')
def test_remove_owner(request_membership, service_with_owner, person, client):
    url = reverse('idm-api:remove-role')
    head = Role.get_exclusive_owner()

    response = client.post(url, data={
        'login': service_with_owner.owner.login,
        'role': json.dumps({
            'type': 'services',
            'services_key': service_with_owner.slug,
            '%s_key' % service_with_owner.slug: '*',
            'role': head.pk,
        })
    })
    assert response.status_code == 200

    assert ServiceMember.all_states.get(staff=person, role=head).state == SERVICEMEMBER_STATE.DEPRIVED
    service_with_owner.refresh_from_db()
    assert service_with_owner.owner is None
    assert not request_membership.called


@patch('plan.api.idm.hooks.deprive_role')
def test_promote_deputy(deprive_role, owner_role, client):
    service = factories.ServiceFactory()
    person = factories.StaffFactory()

    deputy_role = factories.RoleFactory(code=Role.DEPUTY_OWNER)
    deputy = factories.ServiceMemberFactory(staff=person, service=service, role=deputy_role)

    url = reverse('idm-api:add-role')

    response = client.post(url, data={
        'login': person.login,
        'role': json.dumps({
            'type': 'services',
            'services_key': service.slug,
            service.slug + '_key': '*',
            'role': owner_role.id,
        })
    })

    assert response.status_code == 200

    deprive_role.apply_async_on_commit.assert_called_with(
        countdown=settings.ABC_DEFAULT_COUNTDOWN,
        args=[
            deputy.id,
            'Роль зама отозвана, так как пользователь стал руководителем сервиса.'
        ]
    )


def test_notify_staff_on_remove_role(base_data_hooks, client, person):
    url = reverse('idm-api:remove-role')
    ServiceMember.objects.create(
        state=ServiceMember.states.ACTIVE,
        service=base_data_hooks['service2'],
        role=base_data_hooks['support'],
        staff=person,
    )

    with patch('plan.services.tasks.notify_staff') as notify:
        response = client.post(url, data={
            'login': person.username,
            'role': json.dumps({
                'type': 'services',
                'services_key': '_slug1',
                '_slug1_key': '_slug2',
                '_slug2_key': '*',
                'role': base_data_hooks['support'].pk,
            })
        })

        assert response.status_code == 200
        assert notify.delay_on_commit.called


def test_remove_unknown_role(base_data_hooks, client, person):
    # отзыв несуществующей роли не порождает ошибку
    url = reverse('idm-api:remove-role')

    response = client.post(url, data={
        'login': person.username,
        'role': json.dumps({
            'type': 'services',
            'services_key': '_slug1',
            '_slug1_key': '_slug2',
            '_slug2_key': '*',
            'role': base_data_hooks['support'].pk,
        })
    })

    assert json.loads(response.content) == {'code': 0}


def test_get_internal_roles(client, person):
    factories.InternalRoleFactory(
        staff=person,
        role='moderator',
    )

    response = client.json.get(reverse('idm-api:get-roles'))
    assert response.status_code == 200

    data = response.json()

    expected = open(
        source_path(
            os.path.join(STANDARTS_DIR, 'get_roles_internal.json')
        )
    ).read()
    expected_data = json.loads(expected)
    expected_data['roles'][0]['login'] = person.login
    expected_data['roles'][0].pop('_pk')
    data['roles'][0].pop('_pk')
    assert data == expected_data


@pytest.mark.parametrize('role', ['full_access', 'own_only_viewer'])
def test_get_new_internal_roles(client, person, role):
    factories.InternalRoleFactory(
        staff=person,
        role=role,
    )
    tag = factories.ServiceTagFactory()
    person.user.user_permissions.add(Permission.objects.get(codename=f'change_service_tag_{tag.slug}'))

    response = client.json.get(reverse('idm-ext-api:get-roles'))
    assert response.status_code == 200

    data = response.json()

    expected = open(
        source_path(
            os.path.join(STANDARTS_DIR, 'get_new_roles_internal.json')
        )
    ).read()

    expected_data = json.loads(expected)

    role_path = expected_data['roles'][0]['path'].replace('$', role)
    expected_data['roles'][0]['path'] = role_path
    expected_data['roles'][0]['login'] = person.login
    expected_data['roles'][0]['_pk'] = data['roles'][0]['_pk']
    assert data == expected_data


def test_get_service_tags_roles(client, person):
    tag = factories.ServiceTagFactory()
    perm_codename = f'change_service_tag_{tag.slug}'
    person.user.user_permissions.add(Permission.objects.get(codename=perm_codename))

    response = client.json.get(reverse('idm-ext-api:get-roles'), {'type': 'service_tags'})
    assert response.status_code == 200
    data = response.json()
    assert data['roles'] == [{
        'login': person.login,
        'path': f'/type/service_tags/service_tags_key/{tag.slug}/',
        '_pk': person.user.user_permissions.through.objects.get(permission__codename=perm_codename).pk,
    }]


def test_add_internal_role(client):
    person = factories.StaffFactory()
    url = reverse('idm-api:add-role')

    response = client.post(url, data={
        'login': person.username,
        'role': json.dumps({
            'type': 'internal',
            'internal_key': 'moderator',
        }),
    })

    assert response.status_code == 200

    permissions = person.user.user_permissions.all()
    assert len(permissions) == 1
    assert permissions[0].codename == 'moderate'
    assert permissions[0].content_type.app_label == 'internal_roles'


@pytest.mark.parametrize('rolename_internal_key', ['full_access', 'own_only_viewer', 'services_viewer'])
def test_add_new_internal_role(client, staff_factory, rolename_internal_key):
    person = staff_factory(rolename_internal_key)
    url = reverse('idm-ext-api:add-role')
    response = client.post(url, data={
        'login': person.username,
        'role': json.dumps({
            'type': 'internal',
            'internal_key': rolename_internal_key,
        }),
    })

    assert response.status_code == 200

    permissions_codenames = set(
        person.user.user_permissions.values_list('codename', flat=True)
    )
    expected_permissions = set(
        settings.ABC_INTERNAL_ROLES_PERMISSIONS.get(rolename_internal_key, [])
    )

    assert permissions_codenames == expected_permissions

    permissions_app_labels = set(
        person.user.user_permissions.values_list('content_type__app_label', flat=True)
    )
    expected_permission_label = {'internal_roles', }
    assert permissions_app_labels == expected_permission_label


def test_add_service_tag_permission(client):
    tag = factories.ServiceTagFactory()
    person = factories.StaffFactory()
    response = client.post(
        reverse('idm-ext-api:add-role'),
        data={
            'login': person.login,
            'role': json.dumps({
                'type': 'service_tags',
                'service_tags_key': tag.slug,
            }),
        }
    )
    assert response.status_code == 200
    assert person.user.has_perm('services.change_service_tag_' + tag.slug)


def test_remove_internal_role(client, person):
    factories.InternalRoleFactory(staff=person, role='moderator')

    url = reverse('idm-api:remove-role')
    response = client.post(url, data={
        'login': person.username,
        'role': json.dumps({
            'type': 'internal',
            'internal_key': 'moderator'
        })
    })

    assert response.status_code == 200
    assert InternalRole.objects.count() == 0


def test_get_department_roles(base_data_hooks, client, department):
    servicemember_department = factories.ServiceMemberDepartmentFactory(
        service=base_data_hooks['service1'],
        role=base_data_hooks['support'],
        department=department,
    )

    response = client.json.get(reverse('idm-api:get-roles'), data={'type': 'departments'})
    assert response.status_code == 200

    data = response.json()

    expected = open(
        source_path(
            os.path.join(STANDARTS_DIR, 'get_roles_departments.json')
        )
    ).read()
    expected = expected.format(
        role=base_data_hooks['support'].pk,
        pk=servicemember_department.pk,
        group=department.staff_id,
    )
    expected_data = json.loads(expected)
    assert data == expected_data


def test_get_roles_members(base_data_hooks, client, person):
    service_member = factories.ServiceMemberFactory(
        service=base_data_hooks['service1'],
        role=base_data_hooks['support'],
        staff=person,
    )

    response = client.json.get(reverse('idm-api:get-roles'), data={'type': 'members'})
    assert response.status_code == 200

    data = response.json()

    expected = open(
        source_path(
            os.path.join(STANDARTS_DIR, 'get_roles_members.json')
        )
    ).read()
    expected = expected.format(role=base_data_hooks['support'].pk, pk=service_member.pk)
    expected_data = json.loads(expected)
    expected_data['roles'][0]['login'] = person.login
    assert data == expected_data


def test_get_roles_members_with_resource(base_data_hooks, client, person):
    test_type = factories.ResourceTypeFactory(supplier=base_data_hooks['service2'], name='test_type')
    test_resource = factories.ResourceFactory(name='test_resource', external_id='1', type=test_type)

    service_member = factories.ServiceMemberFactory(
        service=base_data_hooks['service1'],
        role=base_data_hooks['support'],
        staff=person,
        resource=test_resource,
    )

    response = client.json.get(reverse('idm-api:get-roles'), data={'type': 'members'})
    assert response.status_code == 200

    data = response.json()
    expected = open(
        source_path(
            os.path.join(STANDARTS_DIR, 'get_roles_members_with_resource.json')
        )
    ).read()
    expected = expected.format(role=base_data_hooks['support'].pk, pk=service_member.pk, resource=test_resource.pk)
    expected_data = json.loads(expected)
    expected_data['roles'][0]['login'] = person.login
    assert data == expected_data


def test_info(base_data_hooks, client):
    url = reverse('idm-api:info')

    Role.objects.exclude(
        id__in=[
            base_data_hooks['developer'].pk,
            base_data_hooks['support'].pk,
            base_data_hooks['product_head'].pk]
    ).delete()

    data = client.json.get(url).json()
    expected = open(
        source_path(
            os.path.join(STANDARTS_DIR, 'info.json')
        )
    ).read()

    expected = expected.replace('Сервис1', base_data_hooks['service1'].name)
    expected = expected.replace('Service1', base_data_hooks['service1'].name_en)
    expected = expected.replace('service1', 'service_%s' % base_data_hooks['service1'].pk)

    expected = expected.replace('Сервис2', base_data_hooks['service2'].name)
    expected = expected.replace('Service2', base_data_hooks['service2'].name_en)
    expected = expected.replace('service2', 'service_%s' % base_data_hooks['service2'].pk)

    expected = expected.replace('role1_id', str(base_data_hooks['developer'].pk))
    expected = expected.replace('role2_id', str(base_data_hooks['support'].pk))
    expected = expected.replace('role3_id', str(base_data_hooks['product_head'].pk))

    expected = expected.replace('role1', 'role_%s' % base_data_hooks['developer'].pk)
    expected = expected.replace('role2', 'role_%s' % base_data_hooks['support'].pk)
    expected = expected.replace('role3', 'role_%s' % base_data_hooks['product_head'].pk)

    expected_data = json.loads(expected)

    assert data == expected_data


def test_info_ext(client, person):
    """В ручке /info/ отдаются только теги с is_protected=False"""
    tag = factories.ServiceTagFactory()
    factories.ServiceTagFactory(is_protected=True)

    url = reverse('idm-ext-api:info')
    data = client.json.get(url).json()

    expected = open(
        source_path(
            os.path.join(STANDARTS_DIR, 'info-ext.json')
        )
    ).read()
    expected_data = json.loads(expected)
    expected_data['roles']['values']['service_tags']['roles']['values'] = {
        tag.slug: {
            'help': {'en': '', 'ru': ''},
            'name': {'en': tag.name_en, 'ru': tag.name},
        },
    }
    assert data == expected_data


def test_ext_get_roles(client, person):
    tag = factories.ServiceTagFactory()
    person.user.user_permissions.add(
        Permission.objects.get(codename='change_service_tag_' + tag.slug)
    )

    url = reverse('idm-ext-api:get-roles')
    data = client.json.get(url, {'type': 'service_tags'}).json()

    assert len(data['roles']) == 1
    role_data = data['roles'][0]
    assert role_data['login'] == person.login
    assert role_data['path'].endswith(f'{tag.slug}/')


def test_info_closed_service(base_data_hooks, client):
    Role.objects.exclude(
        id__in=[
            base_data_hooks['developer'].pk,
            base_data_hooks['support'].pk,
            base_data_hooks['product_head'].pk]
    ).delete()

    base_data_hooks['service1'].state = 'closed'
    base_data_hooks['service1'].save()
    base_data_hooks['service2'].state = 'deleted'
    base_data_hooks['service2'].save()

    url = reverse('idm-api:info')

    data = client.json.get(url).json()

    expected = open(
        source_path(
            os.path.join(STANDARTS_DIR, 'info.json')
        )
    ).read()

    expected = expected.replace('Сервис1', base_data_hooks['service1'].name)
    expected = expected.replace('Service1', base_data_hooks['service1'].name_en)
    expected = expected.replace('service1', 'service_%s' % base_data_hooks['service1'].pk)

    expected = expected.replace('Сервис2', base_data_hooks['service2'].name)
    expected = expected.replace('Service2', base_data_hooks['service2'].name_en)
    expected = expected.replace('service2', 'service_%s' % base_data_hooks['service2'].pk)

    expected = expected.replace('role1_id', str(base_data_hooks['developer'].pk))
    expected = expected.replace('role2_id', str(base_data_hooks['support'].pk))
    expected = expected.replace('role3_id', str(base_data_hooks['product_head'].pk))

    expected = expected.replace('role1', 'role_%s' % base_data_hooks['developer'].pk)
    expected = expected.replace('role2', 'role_%s' % base_data_hooks['support'].pk)
    expected = expected.replace('role3', 'role_%s' % base_data_hooks['product_head'].pk)

    expected_data = json.loads(expected)
    expected_data['roles']['values']['services']['roles']['values']['_slug1']['name']['ru'] = \
        '%s (Закрыт)' % base_data_hooks['service1'].name
    expected_data['roles']['values']['services']['roles']['values']['_slug1']['name']['en'] = \
        '%s (Closed)' % base_data_hooks['service1'].name_en
    del expected_data['roles']['values']['services']['roles']['values']['_slug1']['roles']['values']['_slug2']

    assert data == expected_data


def test_cert(client, settings):
    from django_idm_api import constants
    settings.MIDDLEWARE_CLASSES.append('django_idm_api.middleware.CertificateMiddleware')

    url = reverse('idm-api:info')

    response = client.json.get(url)
    assert response.status_code == 403

    response = client.json.get(
        url,
        **{
            'HTTP_X_QLOUD_SSL_VERIFIED': 'success',
            'HTTP_X_QLOUD_SSL_SUBJECT': constants.IDM_CERT_SUBJECTS[0],
            'HTTP_X_QLOUD_SSL_ISSUER': constants.IDM_CERT_ISSUERS[0],
        }
    )
    assert response.status_code == 200

    settings.MIDDLEWARE_CLASSES.pop()


@pytest.mark.parametrize(
    ('role_name', 'perms_count'), [
        ('own_only_viewer', len(settings.ZERO_BASE_PERMISSIONS | settings.ALL_SERVICES_EXTENSION)),
        ('services_viewer', len(settings.ZERO_BASE_PERMISSIONS))
    ]
)
def test_remove_new_internal_role(client, staff_factory, role_name, perms_count):
    person = staff_factory(role_name)
    url = reverse('idm-api:add-role')

    # Выдадим пользователю 2 роли
    response = client.post(url, data={
        'login': person.username,
        'role': json.dumps({
            'type': 'internal',
            'internal_key': 'own_only_viewer',
        }),
    })
    assert response.status_code == 200

    response = client.post(url, data={
        'login': person.username,
        'role': json.dumps({
            'type': 'internal',
            'internal_key': 'services_viewer',
        }),
    })
    assert response.status_code == 200

    services_viewer_perm_count = len(settings.ZERO_BASE_PERMISSIONS | settings.ALL_SERVICES_EXTENSION)
    assert person.user.user_permissions.count() == services_viewer_perm_count

    # Отзовём роль
    url = reverse('idm-api:remove-role')
    response = client.post(url, data={
        'login': person.username,
        'role': json.dumps({
            'type': 'internal',
            'internal_key': role_name,
        })
    })
    assert response.status_code == 200
    assert person.user.user_permissions.count() == perms_count


def test_remove_service_tag_permission(client):
    """Роли на сервисные теги корректно удаляются."""
    tag = factories.ServiceTagFactory()
    person = factories.StaffFactory()
    person.user.user_permissions.add(
        Permission.objects.get(codename='change_service_tag_' + tag.slug)
    )
    assert person.user.has_perm('services.change_service_tag_' + tag.slug)
    response = client.post(
        reverse('idm-ext-api:remove-role'),
        data={
            'login': person.login,
            'role': json.dumps({
                'type': 'service_tags',
                'service_tags_key': tag.slug,
            }),
        }
    )
    assert response.status_code == 200
    person = Staff.objects.get(pk=person.pk)
    assert not person.user.has_perm('services.change_service_tag_' + tag.slug)


def test_remove_service_tag_permission_for_deleted(client):
    """Роли на удаленные сервисные теги корректно удаляются."""
    tag = factories.ServiceTagFactory()
    person = factories.StaffFactory()
    person.user.user_permissions.add(
        Permission.objects.get(codename='change_service_tag_' + tag.slug)
    )
    assert person.user.has_perm('services.change_service_tag_' + tag.slug)

    with patch('plan.idm.nodes.remove_service_tag_node'):
        with patch('plan.services.signals.purge_service_tag_permission'):
            tag.delete()

    response = client.post(
        reverse('idm-ext-api:remove-role'),
        data={
            'login': person.login,
            'role': json.dumps({
                'type': 'service_tags',
                'service_tags_key': tag.slug,
            }),
        }
    )
    assert response.status_code == 200
    person = Staff.objects.get(pk=person.pk)
    assert not person.user.has_perm('services.change_service_tag_' + tag.slug)
