import pytest
from mock import patch

from json import dumps, loads

from django.core.urlresolvers import reverse


from staff.lib.testing import (
    UserFactory,
    StaffFactory,
    GroupFactory,
    GroupMembershipFactory,
    DepartmentRoleFactory,
    DepartmentFactory,
    DepartmentStaffFactory,
)

from staff.person.tests.factories import ResponsibleForRobotFactory
from staff.person.models import ResponsibleForRobot, Staff
from staff.groups.models import GroupMembership, GROUP_TYPE_CHOICES
from staff.departments.models import Department, DepartmentRole


@pytest.fixture
def structure(db):
    """
    +--------------------------+
    | g1                       |
    |             +-------------------+
    | .p1         | .p2        |   g2 |
    |             |            |      |
    |        +-------------+   |      |
    |        |    |   .p3  |   |      |
    +--------|----|--------|---+      |
             | g3 |        |          |
             +-------------+          |
                  |                   |
                  +-------------------+
    """
    DepartmentRole.objects.all().delete()
    Department.objects.all().delete()

    class Struc:
        dep0 = DepartmentFactory(name='деп0', name_en='dep0', intranet_status=0, url='dep0')
        dep1 = DepartmentFactory(name='деп1', name_en='dep1', intranet_status=1, url='dep1')
        dep2 = DepartmentFactory(name='деп2', name_en='dep2', intranet_status=1, url='dep2')

        u1 = UserFactory(is_superuser=True)

        p1 = StaffFactory(login='person1', user=u1)
        p2 = StaffFactory(login='person2')
        p3 = StaffFactory(login='person3')

        GroupMembershipFactory(group=dep1.group, staff=p1)

        wiki_root = GroupFactory(url='__wiki__', type=GROUP_TYPE_CHOICES.WIKI)
        g1 = GroupFactory(url='grp1', name='name1', parent=wiki_root, type=GROUP_TYPE_CHOICES.WIKI)
        g2 = GroupFactory(url='grp2', name='name2', parent=wiki_root, type=GROUP_TYPE_CHOICES.WIKI)
        g3 = GroupFactory(url='grp3', name='name3', parent=wiki_root, type=GROUP_TYPE_CHOICES.WIKI)

        m11 = GroupMembershipFactory(group=g1, staff=p1)
        m12 = GroupMembershipFactory(group=g1, staff=p2)
        m13 = GroupMembershipFactory(group=g1, staff=p3)

        m22 = GroupMembershipFactory(group=g2, staff=p2)
        m23 = GroupMembershipFactory(group=g2, staff=p3)

        m33 = GroupMembershipFactory(group=g3, staff=p3)

        r1 = StaffFactory(login='robot1', is_robot=True)
        r2 = StaffFactory(login='robot2', is_robot=True)

        r1p1 = ResponsibleForRobotFactory(responsible=p1, robot=r1, role='owner')

        x_role = DepartmentRoleFactory(id='X', manage_by_idm=False)
        help_role = DepartmentRoleFactory(id='help', manage_by_idm=True, name='хелп', name_en='help')
        hrbp_role = DepartmentRoleFactory(id='H', manage_by_idm=True, name='хрбп', name_en='hrbp')
        hrbp_dep1 = DepartmentStaffFactory(staff=p1, department=dep1, role=hrbp_role)

    return Struc()


def create_role(role, group):
    return {'type': 'groups', 'role': role, 'group': str(group)}


def test_info(client, structure):
    response = client.get(reverse('client-api:info'))

    assert response.status_code == 200

    head = loads(response.content)
    body = head['roles'].pop('values')

    assert head == {
        "code": 0,
        "roles": {
            'slug': 'type',
            'name': {
                'ru': 'Тип',
                'en': 'Type',
            },
        }
    }

    assert set(body.keys()) == {'robots', 'groups', 'user_attrs', 'department_roles'}

    assert body['robots'] == {
        "name": {
            "ru": "Управление роботами",
            "en": "Manage Robots"
        },
        "roles": {
            "values": {
                str(structure.r1.id): {
                    "name": {
                        "ru": "robot1",
                        "en": "robot1"
                    },
                    "roles": {
                        "values": {
                            "owner": {
                                "name": {
                                    "ru": "Владелец",
                                    "en": "Owner"
                                }
                            },
                            "user": {
                                "name": {
                                    "ru": "Пользователь",
                                    "en": "User"
                                }
                            }
                        },
                        "slug": "role",
                        "name": {
                            "ru": "Роль",
                            "en": "Role"
                        }
                    }
                },
                str(structure.r2.id): {
                    "name": {
                        "ru": "robot2",
                        "en": "robot2"
                    },
                    "roles": {
                        "values": {
                            "owner": {
                                "name": {
                                    "ru": "Владелец",
                                    "en": "Owner"
                                }
                            },
                            "user": {
                                "name": {
                                    "ru": "Пользователь",
                                    "en": "User"
                                }
                            }
                        },
                        "slug": "role",
                        "name": {
                            "ru": "Роль",
                            "en": "Role"
                        }
                    }
                }
            },
            "slug": "robot",
            "name": {
                "ru": "Робот",
                "en": "Robot"
            }
        }
    }

    assert body['groups'] == {
        'name': {
            'ru': 'Управление группами',
            'en': 'Manage Groups',
        },
        'roles': {
            "values": {
                str(structure.g3.id): {
                    "name": {
                        'ru': structure.g3.name,
                        'en': structure.g3.name,
                    },
                    "roles": {
                        "slug": "role",
                        "name": {
                            'ru': 'Роль',
                            'en': 'Role',
                        },
                        "values": {
                            "member": {
                                'name': {
                                    'ru': "Участник",
                                    'en': 'Member',
                                },
                            },
                            "responsible": {
                                'name': {
                                    'ru': "Ответственный",
                                    'en': 'Responsible',
                                },
                            },
                        },
                    },
                },
                str(structure.g2.id): {
                    "name": {
                        'ru': structure.g2.name,
                        'en': structure.g2.name,
                    },
                    "roles": {
                        "slug": "role",
                        "name": {
                            'ru': 'Роль',
                            'en': 'Role',
                        },
                        "values": {
                            "member": {
                                'name': {
                                    'ru': "Участник",
                                    'en': 'Member',
                                },
                            },
                            "responsible": {
                                'name': {
                                    'ru': "Ответственный",
                                    'en': 'Responsible',
                                },
                            },
                        },
                    },
                },
                str(structure.g1.id): {
                    "name": {
                        'ru': structure.g1.name,
                        'en': structure.g1.name,
                    },
                    "roles": {
                        "slug": "role",
                        "name": {
                            'ru': 'Роль',
                            'en': 'Role',
                        },
                        "values": {
                            "member": {
                                'name': {
                                    'ru': "Участник",
                                    'en': 'Member',
                                },
                            },
                            "responsible": {
                                'name': {
                                    'ru': "Ответственный",
                                    'en': 'Responsible',
                                },
                            },
                        },
                    },
                },
            },
            "name": {
                'ru': "Группа",
                'en': 'Group',
            },
            "slug": "group",
        }
    }

    assert body['user_attrs'] == {
        'name': {
            'ru': 'Управление свойствами пользователей',
            'en': 'Manage Users Attributes',
        },
        'roles': {
            'slug': 'user_attr',
            'name': {
                'ru': 'Атрибут пользователя',
                'en': 'User Attribute',
            },
            'values': {
                'is_superuser': {
                    'name': {
                        'ru': 'Superuser',
                        'en': 'Superuser',
                    },
                },
                'is_staff': {
                    'name': {
                        'ru': 'StaffAdmin',
                        'en': 'StaffAdmin',
                    },
                }
            }
        }
    }

    assert body['department_roles']['name'] == {
        'ru': 'Роли в подразделениях',
        'en': 'Roles in Departments',
    }

    assert structure.x_role.id not in body['department_roles']['roles']['values']

    deps = body['department_roles']['roles']['values'][str(structure.help_role.id)]['roles'].pop('values')
    assert len(deps) == 8
    assert deps[str(structure.dep1.url)] == {
        'name': {'ru': 'деп1', 'en': 'dep1'},
        'unique_id': 'department_roles__help__dep1',
    }

    assert body['department_roles']['roles']['values'][str(structure.help_role.id)] == {
        'name': {'ru': 'хелп', 'en': 'help'},
        'roles': {
            'slug': 'department',
            'name': {'ru': 'Подразделение', 'en': 'Department'}
        }
    }

    deps = body['department_roles']['roles']['values'][str(structure.hrbp_role.id)]['roles'].pop('values')
    assert len(deps) == 8
    assert deps[str(structure.dep1.url)] == {
        'name': {'ru': 'деп1', 'en': 'dep1'},
        'unique_id': 'department_roles__H__dep1',
    }

    assert body['department_roles']['roles']['values'][str(structure.hrbp_role.id)] == {
        'name': {'ru': 'хрбп', 'en': 'hrbp'},
        'fields': [{
            'name': {
                'en': 'Financial Cabinet is Required',
                'ru': 'Требуется финансовый кабинет'
            },
            'slug': 'fincab_required',
            'type': 'booleanfield'
        }],
        'roles': {
            'slug': 'department',
            'name': {'ru': 'Подразделение', 'en': 'Department'}
        }
    }


@patch('staff.emission.django.emission_master.models.logged_models', {GroupMembership})
def test_add_role(client, structure):
    from staff.emission.django.emission_master.controller import controller

    response = client.post(
        reverse('client-api:add-role'),
        {
            'login': structure.p1.login,
            'role': dumps(create_role('member', structure.g3.id))
        },
    )
    assert response.status_code == 200
    assert loads(response.content) == {"code": 0}

    last_emission_data = list(controller.cached_objects.values())[-1][-1][0]
    last_emission_data = loads(last_emission_data)[0]

    assert last_emission_data['model'], 'django_intranet_stuff.groupmembership'
    assert last_emission_data['fields']['group'], int(structure.g3.id)
    assert last_emission_data['fields']['staff'], int(structure.p1.id)

    response = client.post(
        reverse('client-api:add-role'),
        {
            'login': structure.p2.login,
            'role': dumps({'type': 'robots', 'role': 'owner', 'robot': str(structure.r2.id)})
        },
    )
    assert response.status_code == 200
    assert loads(response.content) == {"code": 0}

    response = client.post(
        reverse('client-api:add-role'),
        {
            'login': 'no-such-person',
            'role': dumps(create_role('member', structure.g3.id))
        },
    )
    assert response.status_code == 200
    assert loads(response.content) == {
        "fatal": 'Requested user "no-such-person" does not exist', "code": 400
    }

    response = client.post(
        reverse('client-api:add-role'),
        {
            'login': structure.p1.login,
            'role': dumps(create_role('no-such-role', structure.g3.id))
        },
    )
    assert response.status_code == 200
    assert loads(response.content) == {
        "fatal": 'Role "role:no-such-role" not found', "code": 400
    }

    response = client.post(
        reverse('client-api:add-role'),
        {
            'login': structure.p1.login,
            'role': dumps(create_role('member', -1))
        },
    )
    assert response.status_code == 200
    assert loads(response.content) == {
        "fatal": 'Role "group:-1" not found', "code": 400
    }


@pytest.mark.parametrize(
    'role',
    [
        'is_superuser',
        'is_staff',
    ],
)
def test_add_user_attrs(client, structure, role):
    p1 = Staff.objects.select_related('user').get(login=structure.p1.login)
    setattr(p1.user, role, False)
    p1.user.save()

    response = client.post(
        reverse('client-api:add-role'),
        {
            'login': structure.p1.login,
            'role': dumps({'type': 'user_attrs', 'user_attr': role})
        },
    )
    assert response.status_code == 200
    assert loads(response.content) == {"code": 0}

    p1 = Staff.objects.select_related('user').get(login=structure.p1.login)
    assert getattr(p1.user, role)


def test_remove_role(client, structure):
    response = client.post(
        reverse('client-api:remove-role'),
        {
            'login': structure.p1.login,
            'role': dumps(create_role('member', structure.g1.id))
        },
    )
    assert response.status_code == 200
    assert loads(response.content) == {"code": 0}
    assert GroupMembership.objects.filter(staff=structure.p1, group=structure.g1).count() == 0

    response = client.post(
        reverse('client-api:remove-role'),
        {
            'login': structure.p1.login,
            'role': dumps({'type': 'robots', 'role': 'owner', 'robot': str(structure.r1.id)})
        },
    )
    assert response.status_code == 200
    assert loads(response.content) == {"code": 0}
    assert ResponsibleForRobot.objects.filter(responsible=structure.p1, robot=structure.r1, role='owner').count() == 0

    response = client.post(
        reverse('client-api:remove-role'),
        {
            'login': 'nobody',
            'role': dumps(create_role('member', structure.g1.id))
        },
    )
    assert response.status_code == 200
    assert loads(response.content) == {"code": 0}

    response = client.post(
        reverse('client-api:remove-role'),
        {
            'login': structure.p1.login,
            'role': dumps(create_role('nowhere', structure.g1.id))
        },
    )
    assert response.status_code == 200
    assert loads(response.content) == {"code": 0}


@pytest.mark.parametrize(
    'role',
    [
        'is_superuser',
        'is_staff',
    ],
)
def test_remove_user_attrs(client, structure, role):
    p1 = Staff.objects.select_related('user').get(login=structure.p1.login)
    setattr(p1.user, role, True)
    p1.user.save()

    response = client.post(
        reverse('client-api:remove-role'),
        {
            'login': structure.p1.login,
            'role': dumps({'type': 'user_attrs', 'user_attr': role})
        },
    )
    assert response.status_code == 200
    assert loads(response.content) == {"code": 0}

    p1 = Staff.objects.select_related('user').get(login=structure.p1.login)
    assert not getattr(p1.user, role)


def test_get_all_roles(client, structure):
    response = client.get(reverse('client-api:get-all-roles'))

    assert response.status_code == 200
    assert loads(response.content) == {
      "code": 0,
      "users": [
        {
          "login": "person1",
          "roles": [
            create_role('member', structure.g1.id),
            {'type': 'robots', 'role': 'owner', 'robot': str(structure.r1.id)},
            {'type': 'user_attrs', 'user_attr': 'is_superuser'},
            {'type': 'department_roles', 'role': 'H', 'department': str(structure.dep1.url)},
          ]
        },
        {
          "login": "person2",
          "roles": [
              create_role('member', structure.g1.id),
              create_role('member', structure.g2.id),
          ]
        },
        {
          "login": "person3",
          "roles": [
              create_role('member', structure.g1.id),
              create_role('member', structure.g2.id),
              create_role('member', structure.g3.id),
          ]
        },
      ]
    }
