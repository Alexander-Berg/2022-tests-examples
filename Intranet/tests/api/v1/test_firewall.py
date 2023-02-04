# -*- coding: utf-8 -*-
# https://st.yandex-team.ru/RULES-1555
from itertools import chain
from typing import List
from unittest import mock

import pytest
from django.core.cache import cache
from waffle.testutils import override_switch

from idm.api.v1 import firewall
from idm.api.v1.firewall import Macro, FIREWALL_ROBOT_USERNAME
from idm.core.constants.system import SYSTEM_GROUP_POLICY
from idm.core.models import GroupMembershipSystemRelation, RoleAlias, Role, RoleField
from idm.core.tasks import ActivateGroupMembershipSystemRelations
from idm.tests.utils import add_perms_by_role, set_workflow, DEFAULT_WORKFLOW, raw_make_role, create_system, \
    create_user, create_group, add_members, random_slug
from idm.utils import reverse

# разрешаем использование базы в тестах
pytestmark = pytest.mark.django_db


@pytest.fixture
def firewall_url():
    return reverse('api_dispatch_list', api_name='v1', resource_name='firewall/rules')


def assert_macro(response, macros, exclude=()):
    expected = [{
        'name': {
            'ru': 'Complex система',
            'en': 'Complex system',
        },
        'slug': 'complex',
        'macros': [{
            'macro': 'complex_test-project',
            'description': {
                'ru': 'Complex система: Все роли',
                'en': 'Complex system: All roles',
            },
            'path': '/',
            'content': {
                'users': [],
                'groups': []
            }
        }, {
            'macro': 'complex_test-project-rules',
            'description': {
                'ru': 'Complex система: IDM',
                'en': 'Complex system: IDM',
            },
            'path': '/rules/',
            'content': {
                'users': [],
                'groups': []
            }
        }, {
            'macro': 'complex_test-rules-auditor',
            'description': {
                'ru': 'Complex система: IDM / Аудитор',
                'en': 'Complex system: IDM / Аудитор',
            },
            'path': '/rules/auditor/',
            'content': {
                'users': [],
                'groups': []
            }
        }, {
            'macro': 'system_complex',
            'description': {
                'ru': 'Complex система: Все роли',
                'en': 'Complex system: All roles',
            },
            'path': '/',
            'content': {
                'users': [],
                'groups': [],
            },
        }],
    }]

    for alias, usernames in list(macros.items()):
        macro = next(item for item in expected[0]['macros'] if item['macro'] == alias)
        macro['content']['users'] = usernames


    expected[0]['macros'] = [item for item in expected[0]['macros'] if item['macro'] not in exclude]
    assert response.json() == expected


@pytest.mark.parametrize('use_cache', [True, False])
def test_firewall_without_permissions(client, complex_system, arda_users, firewall_url: str, use_cache: bool):
    frodo = arda_users.frodo
    legolas = arda_users.legolas

    raw_make_role(frodo, complex_system, {'project': 'rules', 'role': 'admin'})

    client.login('legolas')
    # первый запрос, результат останется в кеше
    response = client.json.get(firewall_url, {'use_cache': use_cache})
    assert_macro(response, {})

    raw_make_role(legolas, complex_system, {'project': 'subs', 'role': 'developer'})
    # второй запрос
    response = client.json.get(firewall_url, {'use_cache': use_cache})
    if use_cache:
        assert_macro(response, {})
    else:
        assert_macro(response, {'system_complex': ['legolas'], 'complex_test-project': ['legolas']})

    add_perms_by_role('viewer', legolas)
    # третий запрос
    response = client.json.get(firewall_url, {'use_cache': use_cache})
    if use_cache:
        assert_macro(response, {})
    else:
        assert_macro(response, {
            'system_complex': ['frodo', 'legolas'],
            'complex_test-project': ['frodo', 'legolas'],
            'complex_test-project-rules': ['frodo'],
        })


@pytest.mark.parametrize('use_cache', [True, False])
def test_firewall(client, complex_system, arda_users, firewall_url, use_cache: bool):
    frodo = arda_users.frodo
    add_perms_by_role('viewer', frodo)
    legolas = arda_users.legolas
    gandalf = arda_users.gandalf

    complex_system.use_tvm_role = True
    complex_system.save()

    client.login('frodo')
    # первый запрос, результат останется в кеше
    response = client.json.get(firewall_url, {'use_cache': use_cache})  # first request
    assert_macro(response, {})

    raw_make_role(arda_users.tvm_app, complex_system, {'project': 'subs', 'role': 'developer'})
    raw_make_role(frodo, complex_system, {'project': 'subs', 'role': 'developer'})
    raw_make_role(frodo, complex_system, {'project': 'subs', 'role': 'manager'})
    raw_make_role(frodo, complex_system, {'project': 'rules', 'role': 'auditor'})
    raw_make_role(frodo, complex_system, {'project': 'rules', 'role': 'admin'})
    raw_make_role(gandalf, complex_system, {'project': 'rules', 'role': 'admin'})
    raw_make_role(legolas, complex_system, {'project': 'subs', 'role': 'manager'})

    response = client.json.get(firewall_url, {'use_cache': use_cache})
    # второй запрос
    if use_cache:
        assert_macro(response, {})
    else:
        assert_macro(response, {
            'system_complex': ['frodo', 'gandalf', 'legolas'],
            'complex_test-project': ['frodo', 'gandalf', 'legolas'],
            'complex_test-project-rules': ['frodo', 'gandalf'],
            'complex_test-rules-auditor': ['frodo'],
        })

    # третий запрос, не кешируется, т.к. передана система
    response = client.json.get(firewall_url, {'system': 'unknown', 'use_cache': use_cache})
    assert response.json() == []

    # уберем глобальный (для системы) firewall, оставив только для auditor и project rules
    root_key_node = complex_system.root_role_node.get_children().get()
    root_key_node.aliases.filter(type=RoleAlias.FIREWALL_ALIAS).delete()

    response = client.json.get(firewall_url, {'use_cache': use_cache})
    # четвертый запрос
    if use_cache:
        assert_macro(response, {})
    else:
        assert_macro(response, {
            'system_complex': ['frodo', 'gandalf', 'legolas'],
            'complex_test-project-rules': ['frodo', 'gandalf'],
            'complex_test-rules-auditor': ['frodo'],
        }, exclude=['complex_test-project'])

    # уберём все фаервольные правила, должны выгрузиться все пользователи для системы
    RoleAlias.objects.filter(type=RoleAlias.FIREWALL_ALIAS).delete()
    response = client.json.get(firewall_url, {'use_cache': use_cache})
    # пятый запрос
    if use_cache:
        assert_macro(response, {})
    else:
        assert_macro(response, {
            'system_complex': ['frodo', 'gandalf', 'legolas'],
        }, exclude=['complex_test-project', 'complex_test-project-rules', 'complex_test-rules-auditor'])


@pytest.mark.parametrize('use_cache', [True, False])
def test_group_roles_firewall_rules(
        client,
        complex_system,
        arda_users,
        department_structure,
        firewall_url,
        use_cache: bool
):
    """Проверим, что групповые роли не попадают (пока?) в выгрузку в фаервола,
    персональные, выданные по групповым, попадают, и ничего не взрывается"""

    set_workflow(complex_system, group_code=DEFAULT_WORKFLOW)
    RoleField.objects.filter(node__system=complex_system).delete()
    Role.objects.request_role(arda_users.frodo, department_structure.fellowship, complex_system, '',
                              {'project': 'subs', 'role': 'developer'}, None)

    add_perms_by_role('viewer', arda_users.frodo, complex_system)
    client.login('frodo')

    response = client.json.get(firewall_url, {'use_cache': use_cache})
    members = list(department_structure.fellowship.members.values_list('username', flat=True).order_by('username'))
    assert_macro(response, {
        'system_complex': members, 'complex_test-project': members
    })


@pytest.mark.parametrize('use_cache', [True, False])
def test_system_without_roles(client, complex_system, arda_users, firewall_url, use_cache: bool):
    assert Role.objects.count() == 0

    client.login('frodo')
    add_perms_by_role('viewer', arda_users.frodo)
    response = client.json.get(firewall_url, {'use_cache': use_cache})
    assert_macro(response, {})


@pytest.mark.parametrize('use_cache', [True, False])
def test_groupmembership_system_relations_rules(
        client,
        complex_system,
        arda_users,
        firewall_url: str,
        department_structure,
        use_cache: bool,
):
    complex_system.group_policy = SYSTEM_GROUP_POLICY.AWARE_OF_MEMBERSHIPS_WITHOUT_LOGINS
    complex_system.save()
    fellowship = department_structure.fellowship

    raw_make_role(fellowship, complex_system, {'project': 'subs', 'role': 'developer'})
    GroupMembershipSystemRelation.objects.sync_groupmembership_system_relations(complex_system, fellowship)
    ActivateGroupMembershipSystemRelations.run(system_id=complex_system.id)
    assert GroupMembershipSystemRelation.objects.active().filter(
        system=complex_system,
        membership__group=fellowship,
    ).count() == fellowship.members.count()

    client.login('frodo')
    response = client.json.get(firewall_url, {'use_cache': use_cache})
    expected_data = {
        'system_complex': sorted(fellowship.memberships.values_list('user__username', flat=True))
    }
    assert_macro(response, expected_data)


ROLE_TREE = {
    'code': 0,
    'roles': {
        'slug': 'role',
        'values': {
            'editor': 'Editor',
            'reader': 'Reader',
        }
    }
}

ALIAS_ROLE_TREE = {
    'code': 0,
    'roles': {
        'slug': 'role',
        'values': {
            'manager': 'manager',
            'developer': {
                'slug': 'developer',
                'name': 'Developer',
                'aliases': [{'type': 'firewall', 'name': 'dev-team'}],
                'roles': {
                    'slug': 'development-type',
                    'name': 'Development Type',
                    'values': {
                        'frontend': 'Frontend',
                        'backend': {
                            'slug': 'backend',
                            'name': 'Backend',
                            'firewall-declaration': 'backend-dev',
                        },
                    }
                },
            },
        }
    }
}


def _pop_macro_by_slug(macros: List[Macro], slug: str):
    for i, macro in enumerate(macros):
        if macro['macro'] == slug:
            break
    else:
        raise KeyError('No macro with specified macro')

    return macros.pop(i)


@pytest.mark.parametrize('use_cache', [True, False])
def test_get_firewall_rules(client, firewall_url, use_cache: bool):
    client.login(create_user(superuser=True))

    system = create_system(role_tree=ROLE_TREE)
    system_with_aliases = create_system(role_tree=ALIAS_ROLE_TREE)

    user_1, user_2, user_3 = create_user(), create_user(), create_user()
    group_1, group_2, group_3 = create_group(), create_group(), create_group()

    raw_make_role(user_1, system, {'role': 'editor'})
    raw_make_role(user_2, system, {'role': 'reader'})
    raw_make_role(user_1, system_with_aliases, {'role': 'manager'})
    raw_make_role(user_2, system_with_aliases, {'role': 'developer', 'development-type': 'frontend'})
    raw_make_role(user_3, system_with_aliases, {'role': 'developer', 'development-type': 'backend'})

    raw_make_role(group_1, system, {'role': 'editor'})
    raw_make_role(group_2, system, {'role': 'reader'})
    raw_make_role(group_1, system_with_aliases, {'role': 'manager'})
    raw_make_role(group_2, system_with_aliases, {'role': 'developer', 'development-type': 'frontend'})
    raw_make_role(group_3, system_with_aliases, {'role': 'developer', 'development-type': 'backend'})

    response = client.get(firewall_url, {'allow_groups': 'true', 'use_cache': use_cache})
    assert response.status_code == 200
    rules = response.json()
    assert len(rules) == 2
    rules_by_system = {system_rule['slug']: system_rule for system_rule in rules}
    assert set(rules_by_system.keys()) == {system.slug, system_with_aliases.slug}

    system_rule = rules_by_system[system.slug]
    assert system_rule['name']['ru'] == system.name
    assert system_rule['name']['en'] == system.name_en
    assert len(system_rule['macros']) == 1
    macro = _pop_macro_by_slug(system_rule['macros'], f'system_{system.slug}')
    assert macro['path'] == '/'
    assert macro['description']['ru'] == f'{system.name}: Все роли'
    assert macro['description']['en'] == f'{system.name_en}: All roles'
    assert macro['content']['users'] == sorted([user_1.username, user_2.username])
    assert macro['content']['groups'] == \
           sorted([group_1.external_id, group_2.external_id])

    system_with_aliases_rule = rules_by_system[system_with_aliases.slug]
    assert system_with_aliases_rule['name']['ru'] == system_with_aliases.name
    assert system_with_aliases_rule['name']['en'] == system_with_aliases.name_en
    assert len(system_with_aliases_rule['macros']) == 3

    for slug, path, description, users, groups in (
            (
                f'system_{system_with_aliases.slug}',
                '/',
                {'ru': f'{system_with_aliases.name}: Все роли', 'en': f'{system_with_aliases.name_en}: All roles'},
                sorted(user.username for user in (user_1, user_2, user_3)),
                sorted(group.external_id for group in (group_1, group_2, group_3)),
            ),
            (
                f'{system_with_aliases.slug}_dev-team',
                '/developer/',
                {'ru': f'{system_with_aliases.name}: Developer', 'en': f'{system_with_aliases.name_en}: Developer'},
                sorted([user_2.username, user_3.username]),
                sorted([group_2.external_id, group_3.external_id]),
            ),
            (
                f'{system_with_aliases.slug}_backend-dev',
                '/developer/backend/',
                {
                    'ru': f'{system_with_aliases.name}: Developer / Backend',
                    'en': f'{system_with_aliases.name_en}: Developer / Backend',
                },
                [user_3.username],
                [group_3.external_id],
            ),
    ):
        macro = _pop_macro_by_slug(system_with_aliases_rule['macros'], slug)
        assert macro['path'] == path
        assert macro['description'] == description
        assert macro['content']['users'] == users
        assert macro['content']['groups'] == groups


@pytest.mark.parametrize('use_cache', [True, False])
def test_get_firewall_rules__ref_roles(client, firewall_url, use_cache: bool):
    client.login(create_user(superuser=True))

    main_system = create_system(role_tree=ALIAS_ROLE_TREE)
    ref_system = create_system(role_tree=ROLE_TREE)
    user, member = create_user(), create_user()
    group = create_group()
    add_members(group, [member])

    for subject in (user, group):
        role = raw_make_role(
            subject,
            main_system,
            {'role': 'manager'},
            ref_roles=[{'system': ref_system.slug, 'role_data': {'role': 'reader'}}],
        )
        role.request_refs()
        assert subject.roles.count() == 2
    assert member.roles.count() == 2

    response = client.get(firewall_url, {'allow_groups': 'true', 'use_cache': use_cache})
    assert response.status_code == 200
    rules = response.json()
    assert len(rules) == 2
    for system_rule in rules:
        system_slug = system_rule['slug']
        all_roles_macro = _pop_macro_by_slug(system_rule['macros'], f'system_{system_slug}')
        assert all_roles_macro['content']['users'] == [user.username]  # no member
        assert all_roles_macro['content']['groups'] == [group.external_id]


@pytest.mark.parametrize('use_cache', [True, False])
def test_get_firewall_rules__specify_system(client, firewall_url, use_cache: bool):
    client.login(create_user(superuser=True))

    system = create_system(role_tree=ROLE_TREE)
    system_with_aliases = create_system(role_tree=ALIAS_ROLE_TREE)

    user_1, user_2 = create_user(), create_user()
    group_1, group_2 = create_group(), create_group()

    raw_make_role(user_1, system, {'role': 'editor'})
    raw_make_role(group_1, system, {'role': 'reader'})
    raw_make_role(user_2, system_with_aliases, {'role': 'manager'})
    raw_make_role(group_2, system_with_aliases, {'role': 'manager'})

    # первый запрос по системе system, кешируется
    response = client.get(firewall_url, {'system': system.slug, 'allow_groups': '1', 'use_cache': use_cache})
    assert response.status_code == 200
    rules = response.json()
    assert len(rules) == 1

    system_rule = rules[0]
    assert system_rule['name']['ru'] == system.name
    assert system_rule['name']['en'] == system.name_en
    assert len(system_rule['macros']) == 1
    macro = _pop_macro_by_slug(system_rule['macros'], f'system_{system.slug}')
    assert macro['path'] == '/'
    assert macro['description']['ru'] == f'{system.name}: Все роли'
    assert macro['description']['en'] == f'{system.name_en}: All roles'
    assert macro['content']['users'] == [user_1.username]
    assert macro['content']['groups'] == [group_1.external_id]

    # первый запрос по системе system_with_aliases, кешируется
    response = client.get(
        firewall_url,
        {'system': system_with_aliases.slug, 'allow_groups': '1', 'use_cache': use_cache}
    )
    assert response.status_code == 200
    rules = response.json()
    assert len(rules) == 1

    system_rule = rules[0]
    assert system_rule['name']['ru'] == system_with_aliases.name
    assert system_rule['name']['en'] == system_with_aliases.name_en
    assert len(system_rule['macros']) == 3
    macro = _pop_macro_by_slug(system_rule['macros'], f'system_{system_with_aliases.slug}')
    assert macro['path'] == '/'
    assert macro['description']['ru'] == f'{system_with_aliases.name}: Все роли'
    assert macro['description']['en'] == f'{system_with_aliases.name_en}: All roles'
    assert macro['content']['users'] == [user_2.username]
    assert macro['content']['groups'] == [group_2.external_id]

    for macro in system_rule['macros']:
        assert macro['content']['users'] == []
        assert macro['content']['groups'] == []


@pytest.mark.parametrize('use_cache', [True, False])
def test_get_firewall_rules__permitted_for_requester(client, firewall_url, use_cache: bool):
    system = create_system(role_tree=ROLE_TREE)
    system_with_aliases = create_system(role_tree=ALIAS_ROLE_TREE)

    user_1, user_2 = create_user(), create_user()
    group_1, group_2 = create_group(), create_group()

    raw_make_role(user_1, system, {'role': 'editor'})
    raw_make_role(group_1, system, {'role': 'reader'})
    raw_make_role(user_2, system_with_aliases, {'role': 'manager'})
    raw_make_role(group_2, system_with_aliases, {'role': 'manager'})

    # nobody
    client.login(create_user())
    # первый запрос из под пользователя nobody, кешируется
    response = client.get(firewall_url, {'allow_groups': 'true', 'use_cache': use_cache})
    rules = response.json()
    assert response.status_code == 200
    assert len(rules) == 2
    for macro in chain(*(rule['macros'] for rule in rules)):
        assert macro['content']['users'] == []
        assert macro['content']['groups'] == []

    # user role owner
    client.login(user_1)
    # первый запрос из под пользователя user_1, кешируется
    response = client.get(firewall_url, {'allow_groups': 'true', 'use_cache': use_cache})
    assert response.status_code == 200
    rules = response.json()

    assert len(rules) == 2
    for macro in chain(*(rule['macros'] for rule in rules)):
        if macro['macro'] == f'system_{system.slug}':
            assert macro['content']['users'] == [user_1.username]
            assert macro['content']['groups'] == []
        else:
            assert macro['content']['users'] == []
            assert macro['content']['groups'] == []

    # group member role owner
    user = create_user()
    add_members(group_1, [user])
    client.login(user)
    # первый запрос из под пользователя user, кешируется
    response = client.get(firewall_url, {'allow_groups': 'true', 'use_cache': use_cache})
    assert response.status_code == 200
    rules = response.json()

    assert len(rules) == 2
    for macro in chain(*(rule['macros'] for rule in rules)):
        if macro['macro'] == f'system_{system.slug}':
            assert macro['content']['users'] == []
            assert macro['content']['groups'] == [group_1.external_id]
        else:
            assert macro['content']['users'] == []
            assert macro['content']['groups'] == []

    # firewall robot
    client.login(create_user(FIREWALL_ROBOT_USERNAME))
    # первый запрос из под пользователя firewall_robot, кешируется
    response = client.get(firewall_url, {'allow_groups': 'true', 'use_cache': use_cache})
    assert response.status_code == 200, response.content
    rules = response.json()

    assert len(rules) == 2
    for macro in chain(*(rule['macros'] for rule in rules)):
        if macro['macro'] == f'system_{system.slug}':
            assert macro['content']['users'] == [user_1.username]
            assert macro['content']['groups'] == [group_1.external_id]
        elif macro['macro'] in (f'system_{system_with_aliases.slug}', f'{system_with_aliases.slug}_manager'):
            assert macro['content']['users'] == [user_2.username]
            assert macro['content']['groups'] == [group_2.external_id]
        else:
            assert macro['content']['users'] == []
            assert macro['content']['groups'] == []


@override_switch(firewall.FirewallResource.CACHE_DEFAULT_SWITCH, True)
def test_get_firewall_rules__cache(client, firewall_url):
    firewall_robot = create_user(FIREWALL_ROBOT_USERNAME)
    create_system(role_tree=ALIAS_ROLE_TREE)

    patch_get_firewall_rules = lambda: mock.patch(
        'idm.api.v1.firewall.get_firewall_rules',
        wraps=firewall.get_firewall_rules,
    )

    # кеш пустой
    assert cache.get(firewall.get_cache_key(firewall_robot, expand_groups=False)) is None

    client.login(firewall_robot)
    with patch_get_firewall_rules() as get_firewall_rules_mock:
        # первый запрос
        rules_with_groups = client.json.get(firewall_url, {'allow_groups': 'true'}).json()
    get_firewall_rules_mock.assert_called_with(firewall_robot, system_slug=None, expand_groups=False)
    # кеш появился
    assert cache.get(firewall.get_cache_key(firewall_robot, expand_groups=False)) == rules_with_groups

    # добавляем новую систему
    create_system(role_tree=ROLE_TREE)
    with patch_get_firewall_rules() as get_firewall_rules_mock:
        # повторяем первый запрос и проверяем что ничего не поменялось
        assert client.json.get(firewall_url, {'allow_groups': 'true'}).json() == rules_with_groups
        get_firewall_rules_mock.assert_not_called()
        # кеш остался прежним
        assert cache.get(firewall.get_cache_key(firewall_robot, expand_groups=False)) == rules_with_groups

        # повторяем первый запрос, но теперь без кеширования
        updated_rules_with_groups = client.json.get(firewall_url, {'allow_groups': 'true', 'use_cache': False}).json()
        assert updated_rules_with_groups != rules_with_groups
        get_firewall_rules_mock.assert_called_with(firewall_robot, system_slug=None, expand_groups=False)
        assert cache.get(firewall.get_cache_key(firewall_robot, expand_groups=False)) == updated_rules_with_groups

    # меняем пользователя
    superuser = create_user(superuser=True)
    client.login(superuser)
    # передыдущий запрос в кеше остался
    assert cache.get(firewall.get_cache_key(firewall_robot, expand_groups=False)) ==\
           updated_rules_with_groups
    # новый кеш пустой
    assert cache.get(firewall.get_cache_key(superuser, expand_groups=False)) is None
    with patch_get_firewall_rules() as get_firewall_rules_mock:
        # первый запрос
        rules_with_groups = client.json.get(firewall_url, {'allow_groups': 'true'}).json()
        get_firewall_rules_mock.assert_called_with(superuser, system_slug=None, expand_groups=False)
        assert cache.get(firewall.get_cache_key(superuser, expand_groups=False)) == rules_with_groups
