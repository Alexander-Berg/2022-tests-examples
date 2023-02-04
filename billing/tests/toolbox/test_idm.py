from json import dumps

import pytest
from django_idm_api.exceptions import RoleNotFound

from bcl.banks.registry import Sber
from bcl.core.integration.idm import Hooks
from bcl.core.models import Role, User
from bcl.exceptions import BclException


def test_role_add_remove(response_mock, get_assoc_acc_curr, make_org, make_org_grp):

    org2 = make_org(name='some')

    org_grp = make_org_grp(name='test')

    _, acc, _ = get_assoc_acc_curr(Sber, account='xyz', org='ere')
    hooks = Hooks()

    idm_success = {
        "links": {},
        "page": 1,
        "limit": 999,
        "result": [
            {
                'work_phone': None,
                'login': 'robot-bcl',
                'location': {
                    'office': {'contacts': {'phone': ''}}
                },
                'name': {
                    'middle': 'me', 'has_namesake': False,
                    'last': {'ru': 'rul', 'en': 'robot-bcl'},
                    'first': {'ru': 'ruf', 'en': 'robot-bcl'},
                    'hidden_middle': True
                }
            },
        ],
        "total": 1,
        "pages": 1
    }

    idm_fail = {
        'error_message': 'not found', 'details': {
            'request': {
                '_one': '1', 'login': '34789722',
                '_fields': 'login,work_phone,name,location.office.contacts.phone'}}
    }

    def for_user(login, role=Role.SALARY_GROUP):
        result = hooks.add_role(login, {'role': role}, {}, 'user')
        user = User.objects.get(username=login)
        restrictions_role = user.restrictions.setdefault(role, {})
        restrictions_role.setdefault('org', [None])
        user.save()
        return result

    from django.conf import settings

    ROBOT_NAME = settings.ROBOT_NAME

    with response_mock(f'GET https://staff-api.test.yandex-team.ru/v3/persons -> 200:{dumps((idm_success))}'):
        result = for_user(ROBOT_NAME)

    assert result == {'code': 0, 'data': {}}
    users = User.objects.filter(username=ROBOT_NAME)
    assert len(users) == 1
    user = users[0]
    assert len(user.roles) == 1
    assert user.name_en == 'robot-bcl robot-bcl'
    assert user.name_ru == 'rul me ruf'

    result = for_user(ROBOT_NAME, Role.ACCOUNTANT)
    assert result == {'code': 0, 'data': {}}
    assert len(User.objects.filter(username=ROBOT_NAME).first().roles) == 2

    with response_mock(f'GET https://staff-api.test.yandex-team.ru/v3/persons -> 200:{dumps(idm_fail)}'):
        with pytest.raises(BclException):
            for_user('34789722')

    assert User.objects.filter(username='34789722').count() == 0

    # Добавляем роли.

    role_spec = {'role': 'support'}
    restr_org = User.RESTR_ORG
    restr_acc = User.RESTR_ACC
    restr_grp = User.RESTR_GRP
    robot = User.objects.get(username=ROBOT_NAME)

    def get_role_restriction(role_spec=None, fields_spec=None):
        if role_spec and fields_spec:
            hooks.remove_role(ROBOT_NAME, role_spec, fields_spec, False, '')
        roles_restr = list(filter(
            lambda role: type(role) == list and role[0]['role'] == 'support', hooks.get_user_roles_impl(robot.username)
        ))
        robot.refresh_from_db()
        return roles_restr

    # Добавляем роль с органичениями и тут же удаляем (проверяем, что роль удалится)
    fields_spec = {restr_org: str(acc.org_id), restr_grp: str(org_grp.id)}
    result = hooks.add_role(ROBOT_NAME, role_spec, fields_spec, '')
    roles_restr = get_role_restriction()

    assert result == {'code': 0, 'data': fields_spec}
    assert roles_restr == [[role_spec, {restr_org: str(acc.org_id)}], [role_spec, {restr_grp: str(org_grp.id)}]]
    assert role_spec['role'] in robot.roles

    roles_restr = get_role_restriction(role_spec, {restr_org: str(acc.org_id), restr_grp: str(org_grp.id)})

    assert not roles_restr
    assert role_spec['role'] not in robot.roles

    # Добавляем на роль два ограничения, потом удаляем по одному
    fields_spec = {restr_org: str(acc.org_id)}
    hooks.add_role(ROBOT_NAME, role_spec, fields_spec, '')
    hooks.add_role(ROBOT_NAME, role_spec, {restr_grp: str(org_grp.id)}, '')
    roles_restr = get_role_restriction()

    assert roles_restr == [[role_spec, {restr_org: str(acc.org_id)}], [role_spec, {restr_grp: str(org_grp.id)}]]
    assert role_spec['role'] in robot.roles

    roles_restr = get_role_restriction(role_spec, {restr_grp: str(org_grp.id)})

    assert roles_restr
    assert role_spec['role'] in robot.roles

    roles_restr = get_role_restriction(role_spec, {restr_org: str(acc.org_id)})

    assert not roles_restr
    assert role_spec['role'] not in robot.roles

    fields_spec = {restr_org: str(acc.org_id)}
    result = hooks.add_role(ROBOT_NAME, role_spec, fields_spec, '')
    assert result == {'code': 0, 'data': fields_spec}
    hooks.add_role(ROBOT_NAME, role_spec, {restr_org: str(org2.id)}, '')

    fields_spec = {restr_acc: str(acc.id)}
    result = hooks.add_role(ROBOT_NAME, role_spec, fields_spec, '')
    assert result == {'code': 0, 'data': fields_spec}

    fields_spec = {restr_grp: str(org_grp.id)}
    result = hooks.add_role(ROBOT_NAME, role_spec, fields_spec, '')
    assert result == {'code': 0, 'data': fields_spec}

    # Повторное добавление.
    fields_spec = {restr_org: str(acc.org_id)}
    result = hooks.add_role(ROBOT_NAME, role_spec, fields_spec, '')
    assert result == {'code': 0, 'data': fields_spec}
    robot.refresh_from_db()
    assert robot.restrictions == {
        'accountant': {'org': [None]},
        'salary_group': {'org': [None]},
        'support': {'grp': [org_grp.id], 'org': [acc.org_id, org2.id]}
    }

    result = hooks.get_user_roles(ROBOT_NAME)
    assert result == {
        'code': 0, 'roles': [
            {'role': 'salary_group'},
            {'role': 'accountant'},
            [{'role': 'support'}, {'org': str(acc.org_id)}],
            [{'role': 'support'}, {'org': str(org2.id)}],
            [{'role': 'support'}, {'grp': str(org_grp.id)}]
        ]}

    # Неизвестная область роли.
    with pytest.raises(RoleNotFound):
        hooks.add_role(ROBOT_NAME, {'unknown_spec': 'unknown_id'}, {}, '')

    # Неизвестная роль.
    with pytest.raises(RoleNotFound):
        hooks.add_role(ROBOT_NAME, {'role': 'unknown_role'}, {}, '')

    # Удаляем роли.
    result = hooks.remove_role(ROBOT_NAME, role_spec, {restr_org: str(acc.org_id)}, False, '')
    assert result == {'code': 0}
    robot.refresh_from_db()
    assert robot.restrictions == {
        'accountant': {'org': [None]},
        'salary_group': {'org': [None]},
        'support': {'grp': [org_grp.id], 'org': [org2.id]}
    }

    result = hooks.remove_role(ROBOT_NAME, role_spec, {restr_grp: str(org_grp.id)}, False, '')
    assert result == {'code': 0}
    robot.refresh_from_db()
    assert robot.restrictions == {
        'accountant': {'org': [None]},
        'salary_group': {'org': [None]},
        'support': {'grp': [], 'org': [org2.id]}
    }
    assert 'support' in robot.roles

    # Повторый вызов удаления с указанием ограничения удалит и саму роль, так как больше нет ограничений.
    result = hooks.remove_role(ROBOT_NAME, role_spec, {restr_org: str(org2.id)}, False, '')
    assert result == {'code': 0}
    robot.refresh_from_db()
    assert robot.restrictions == {
        'accountant': {'org': [None]},
        'salary_group': {'org': [None]},
        'support': {'grp': [], 'org': []}
    }
    assert 'support' not in robot.roles

    result = hooks.remove_role(ROBOT_NAME, {'role': Role.ACCOUNTANT}, {}, False, '')
    assert result == {'code': 0}
    robot.refresh_from_db()
    assert robot.restrictions == {
        'accountant': {'org': []},
        'salary_group': {'org': [None]},
        'support': {'grp': [], 'org': []}
    }
    assert 'accountant' not in robot.roles

    # Неизвестный пользователь.
    result = hooks.remove_role('unknown', role_spec, {restr_acc: str(acc.id)}, False, '')
    assert result == {'code': 0}

    # Неизвестное право.
    result = hooks.remove_role(ROBOT_NAME, {'unknown': 'yyy'}, {}, False, '')
    assert result == {'code': 0}

    result = hooks.get_user_roles(ROBOT_NAME)
    assert result == {'code': 0, 'roles': [{'role': 'salary_group'}]}


def test_get_roles(init_user):
    hooks = Hooks()
    username = 'test'
    roles = ['accountant', 'salary_group']

    init_user(restr_orgs=[None])
    init_user(username=username, roles=roles)

    for role in roles:
        user = User.objects.get(username=username)
        restrictions_role = user.restrictions.setdefault(role, {})
        restrictions_role.setdefault('org', [None])
        user.save()

    res = hooks.get_all_roles()

    assert not res['code']
    assert len(res['users']) == 2
    assert len([role for user in res['users'] for role in user['roles']]) == 3

    res = hooks.get_user_roles('test')
    assert res == {'code': 0, 'roles': [{'role': 'accountant'}, {'role': 'salary_group'}]}

    res = hooks.get_user_roles('unknown')
    assert res == {'code': 0, 'roles': []}


def test_info(get_assoc_acc_curr):

    _, acc, _ = get_assoc_acc_curr(Sber, account='xyz', org='ere')

    hooks = Hooks()
    res = hooks.info()

    assert res['code'] == 0

    assert res['fields'][0]['slug'] == 'org'
    assert res['fields'][0]['options'] == {
        'widget': 'select',
        'display_name': True,
        'choices': [{'value': '*', 'name': '* Все'}, {'value': f'{acc.org_id}', 'name': 'ere'}]
    }
    assert res['fields'][1]['options']['display_name']
