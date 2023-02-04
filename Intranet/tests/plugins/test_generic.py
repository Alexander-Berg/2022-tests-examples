# -*- coding: utf-8 -*-
from unittest import mock

import pycurl
import pytest
from django.core import mail
from mock import call
from math import ceil

from idm.core.models import Role, RoleNode
from idm.core.plugins.errors import (
    BasePluginError,
    PluginFatalError,
    PluginError,
)
from idm.utils.cleansing import SECRET_DATA_PLACEHOLDER
from idm.core.constants.groupmembership import GROUPMEMBERSHIP_STATE
from idm.core.constants.system import SYSTEM_GROUP_POLICY, SYSTEM_PLUGIN_TYPE
from idm.core.models import GroupMembershipSystemRelation
from idm.users.models import GroupMembership
from idm.tests.utils import (refresh, assert_contains, raw_make_role, Response, capture_http, capture_raw_http,
                             assert_http, assert_raw_http, set_workflow, DEFAULT_WORKFLOW)
from idm.utils import json

pytestmark = [pytest.mark.django_db]
with_two_plugins = pytest.mark.parametrize('generic_system', ['generic', 'generic_legacy'], indirect=True)
OK_ANSWER = Response(200, {'code': 0})


@pytest.fixture
def generic_system(request, generic_system):
    generic_system.plugin_type = request.param
    generic_system.save()
    return refresh(generic_system)


@pytest.mark.parametrize('tvm_params', [(False, 'user'), (True, 'user'), (True, 'tvm_app')])
@with_two_plugins
def test_add_role(generic_system, arda_users, tvm_params):
    """Проверка выдачи роли"""

    tvm_system, tvm_user = tvm_params
    frodo = arda_users.frodo
    frodo.type = tvm_user
    frodo.save()
    generic_system.use_tvm_role = tvm_system
    generic_system.save()
    return_value = {
        'code': 0,
        'data': {
            'passport-login': 'frodo-baggins'
        }
    }
    node = RoleNode.objects.get(slug='manager')
    node.unique_id = '123'
    node.save(update_fields=['unique_id'])
    generic_system.fetch_actual_workflow()
    with capture_http(generic_system, return_value) as mocked:
        role = Role.objects.request_role(frodo, frodo, generic_system, '', {'role': 'manager'}, None)

    data = {
        'login': 'frodo',
        'role': '{"role": "manager"}',
        'fields': 'null',
    }
    if generic_system.plugin_type == 'generic':
        if tvm_system:
            data['subject_type'] = tvm_user
        data.update({
            'path': '/role/manager/',
            'unique_id': '123',
        })
    assert_http(mocked.http_post, url='http://example.com/add-role/', data=data, timeout=60)

    role = refresh(role)
    assert role.state == 'granted'
    assert role.system_specific == {'passport-login': 'frodo-baggins'}


@with_two_plugins
@pytest.mark.parametrize('tvm_params', [(False, 'user'), (True, 'user'), (True, 'tvm_app')])
def test_remove_role(generic_system, arda_users, tvm_params):
    """Проверка отзыва роли"""

    tvm_system, tvm_user = tvm_params
    frodo = arda_users.frodo
    frodo.type = tvm_user
    frodo.save()
    generic_system.use_tvm_role = tvm_system
    generic_system.save()
    node = RoleNode.objects.get(slug='manager')
    node.unique_id = '1234'
    node.save(update_fields=['unique_id'])
    role = raw_make_role(frodo, generic_system, {'role': 'manager'}, state='granted',
                         fields_data={'passport-login': 'frodo-baggins'},
                         system_specific={'passport-login': 'frodo-baggins'})

    answer = {
        'data': json.dumps({'passport-login': 'frodo-baggins'}),
        'login': 'frodo',
        'role': json.dumps({'role': 'manager'}),
    }
    if generic_system.plugin_type == 'generic':
        if tvm_system:
            answer['subject_type'] = tvm_user
        answer.update({
            'path': '/role/manager/',
            'fields': json.dumps({'passport-login': 'frodo-baggins'}),
            'unique_id': '1234',
        })
        del answer['data']

    return_value = {
        'code': 0,
        'data': {
            'passport-login': 'frodo-baggins'
        }
    }

    with capture_http(generic_system, return_value) as sender:
        role.deprive_or_decline(frodo)
        assert_http(sender.http_post, url='http://example.com/remove-role/', data=answer, timeout=60)

    role = refresh(role)
    assert role.state == 'deprived'


@with_two_plugins
def test_remove_role_on_fire(generic_system, arda_users):
    """Проверка отзыва роли при увольнении"""

    frodo = arda_users.frodo
    role = raw_make_role(frodo, generic_system, {'role': 'manager'}, state='granted',
                         fields_data={'passport-login': 'frodo-baggins'},
                         system_specific={'passport-login': 'frodo-baggins'})
    frodo.is_active = False
    frodo.save()

    answer = {
        'data': json.dumps(role.system_specific),
        'login': 'frodo',
        'role': json.dumps(role.node.data),
        'fired': 1,
    }

    if generic_system.plugin_type == 'generic':
        answer.update({
            'path': '/role/manager/',
            'fields': json.dumps({'passport-login': 'frodo-baggins'}),
        })
        del answer['data']

    with capture_http(generic_system, {'code': 0}) as sender:
        role.deprive_or_decline(None, bypass_checks=True)
        assert_http(sender.http_post, url='http://example.com/remove-role/', data=answer, timeout=60)

    role = refresh(role)
    assert role.state == 'deprived'
    assert not role.is_active
    # Проверим, что добавился role action
    last_action = role.actions.order_by('-id')[0]
    assert last_action.action == 'remove'

    # Уведомление отправилось, уволенным пользователям уведомления тоже отправляются
    assert len(mail.outbox) == 1
    message = mail.outbox[0]
    assert message.to == ['frodo@example.yandex.ru']
    assert message.subject == 'Generic система. Роль отозвана'
    assert message.cc == []
    assert_contains([
        'Робот отозвал вашу роль в системе "Generic система":',
        'Роль: Менеджер',
    ], message.body)


@with_two_plugins
def test_failed_deprive_action(generic_system, arda_users):
    """Проверяем, что если отзыв роли сфейлился, то причина будет указана в info."""

    frodo = arda_users.frodo
    role = raw_make_role(frodo, generic_system, {'role': 'manager'}, state='granted',
                         fields_data={'passport-login': 'frodo-baggins'},
                         system_specific={'passport-login': 'frodo-baggins'})
    fatal_error = PluginFatalError(1, 'something', {'a': 'b'})
    answer = {
        'data': json.dumps(role.system_specific),
        'login': 'frodo',
        'role': json.dumps(role.node.data),
    }
    if generic_system.plugin_type == 'generic':
        answer.update({
            'path': '/role/manager/',
            'fields': json.dumps({'passport-login': 'frodo-baggins'}),
        })
        del answer['data']
    with capture_http(generic_system, side_effect=fatal_error) as sender:
        role.deprive_or_decline(frodo)
        assert_http(sender.http_post, url='http://example.com/remove-role/', data=answer, timeout=60)
    role = refresh(role)
    assert role.state == 'depriving'
    # Проверим, что добавился role action
    last_action = role.actions.filter(action='deprive').order_by('-id').first()
    expected = '''PluginFatalError: code=1, message="something", data={'a': 'b'}, answer="None"'''
    assert last_action.error == expected


@with_two_plugins
def test_failed_grant_action(generic_system, arda_users):
    """Проверяем, что если выдача роли в системе сфейлился, то роль перейдет в состояние failed."""

    frodo = arda_users.frodo
    frodo.passport_logins.create(login='frodo-baggins', state='created', is_fully_registered=True)

    fatal_error = PluginFatalError(1, 'blah minor', {'a': 'b'})

    generic_system.fetch_actual_workflow()
    with capture_http(generic_system, side_effect=fatal_error):
        role = Role.objects.request_role(frodo, frodo, generic_system, '', {'role': 'manager'},
                                         {'passport-login': 'frodo-baggins'})

    role = refresh(role)
    assert role.state == 'failed'
    # Проверим, что добавился role action
    last_action = role.actions.order_by('-id').first()
    assert last_action.action == 'fail'
    expected = '''PluginFatalError: code=1, message="blah minor", data={'a': 'b'}, answer="None"'''
    assert last_action.error == expected


@with_two_plugins
def test_token_for_direct(generic_system, arda_users):
    frodo = arda_users.frodo
    frodo.passport_logins.create(login='frodo', state='created', is_fully_registered=True)
    generic_system.base_url = 'https://example.com/%s/?token=DEADBEEF&secret=BAD1DEA'
    generic_system.save()
    generic_system = refresh(generic_system)

    generic_system.fetch_actual_workflow()
    with capture_http(generic_system, OK_ANSWER) as sender:
        role = Role.objects.request_role(frodo, frodo, generic_system, '', {'role': 'manager'},
                                         {'passport-login': 'frodo'})

    expected_data = {
        'fields': '{"passport-login": "frodo"}',
        'secret': 'BAD1DEA',
        'token': 'DEADBEEF',
        'role': '{"role": "manager"}',
        'login': 'frodo',
    }
    if generic_system.plugin_type == 'generic':
        expected_data.update({
            'path': '/role/manager/',
        })
    assert_http(sender.http_post, url='https://example.com/add-role/', data=expected_data)


@with_two_plugins
def test_info_yaml(generic_system, arda_users):
    """
    Протестируем возможность использования дерева ролей из YAML файла
    """
    yaml_response = '''
code: 0
roles:
  slug: role
  name:
    en: Role
    ru: Роль
  values:
    role_1:
        slug: role_1
        name: Роль 1
    role_2:
      slug: role_2
      name:
        ru: Роль 2
        en: Role 2
'''

    generic_system.node_plugin_type = 'yaml'
    generic_system.roles_tree_url = 'https://some.paste/123'
    generic_system.save()

    with mock.patch('idm.core.plugins.plugins_mixin.RequestMixin._make_request') as request_yaml:
        request_yaml.return_value = mock.Mock(
            content=yaml_response,
            status_code=200,
        )
        info_result = generic_system.node_plugin.get_info()

    request_yaml.assert_called_once_with(generic_system.roles_tree_url, 'GET', None, 60, headers=None)
    assert info_result == {
        'code': 0,
        'roles': {
            'slug': 'role',
            'name': {
                'ru': 'Роль',
                'en': 'Role',
            },
            'values': {
                'role_1': {
                    'slug': 'role_1',
                    'name': 'Роль 1',
                },
                'role_2': {
                    'slug': 'role_2',
                    'name': {
                        'ru': 'Роль 2',
                        'en': 'Role 2',
                    }
                }
            }
        }
    }


@with_two_plugins
def test_empty_system_response(generic_system, arda_users):
    frodo = arda_users.frodo

    def create_response(answer, status_code=200, url=''):
        resp = Response(status_code, answer, '', url)
        return resp

    # пустой ответ /info
    with capture_raw_http(generic_system, answer=create_response('')):
        with pytest.raises(PluginError):
            generic_system.plugin.get_info()

    # no code ответ /info
    with capture_raw_http(generic_system, answer=create_response({})):
        with pytest.raises(PluginError):
            generic_system.plugin.get_info()

    # кривой ответ с json, который нельзя распарсить
    with capture_raw_http(generic_system, answer=create_response('''{hello world}''')):
        with pytest.raises(PluginError):
            generic_system.plugin.get_info()

    # ответ с error
    answer = create_response({'code': 500, 'error': 'hello world'}, status_code=500)
    with capture_raw_http(generic_system, answer=answer):
        with pytest.raises(PluginError):
            generic_system.plugin.get_info()

    generic_system.fetch_actual_workflow()
    # пустой ответ
    with capture_raw_http(generic_system, answer=create_response('')):
        role1 = Role.objects.request_role(frodo, frodo, generic_system, '', {'role': 'admin'}, None)

    role1 = refresh(role1)
    assert role1.state == 'failed'

    # ответ без code
    with capture_raw_http(generic_system, answer=create_response('{"notcode": 0}')):
        role2 = Role.objects.request_role(frodo, frodo, generic_system, '', {'role': 'manager'}, None)

    role2 = refresh(role2)
    assert role2.state == 'failed'

    # правильный ответ
    with capture_raw_http(generic_system, answer=OK_ANSWER):
        role3 = Role.objects.request_role(frodo, frodo, generic_system, '', {'role': 'poweruser'}, None)

    role3 = refresh(role3)
    assert role3.state == 'granted'

    # теперь отзовем эту же роль
    with capture_raw_http(generic_system, answer=OK_ANSWER):
        role3.deprive_or_decline(frodo)

    role3 = refresh(role3)
    assert role3.state == 'deprived'


@with_two_plugins
def test_client_cert(generic_system):
    generic_system.auth_factor = 'cert'
    generic_system.save(update_fields=['auth_factor'])
    with capture_raw_http(generic_system, OK_ANSWER) as sender:
        generic_system.plugin.get_info()

    assert_raw_http(sender, method='GET', cert='yandex_internal_cert')


@with_two_plugins
def test_add_role_if_system_answered_correctly(generic_system, arda_users):
    """Проверим, что если система корректным ответом, то мы выдаём роль"""
    frodo = arda_users.frodo

    correct_answers = [
        {
            'code': '0',  # строкой тоже можно передавать код 0
            'data': {'passport-login': 'frodo-baggins'}
        },
        {
            'code': 0,
            'data': {'passport-login': 'frodo-baggins'}
        },
        {
            'code': 0,
            'fatal': 'На самом деле ошибка-то!',  # дополнительные ключи игнорируются при коде 0
            'data': {'passport-login': 'frodo-baggins'}
        },
        {
            'code': '0',
            'fatal': 'На самом деле ошибка-то!',  # дополнительные ключи игнорируются при коде 0 текстом
            'data': {'passport-login': 'frodo-baggins'}
        },
        {
            'code': -2.5,
            'warning': 'Всё хорошо, роль можно считать выданной (ха-ха)',  # любой код не 0 + warning считается успехом
            'data': {'passport-login': 'frodo-baggins'}
        },
        {
            # даже если код совсем странный и строка, но наличие warning считается поводом выдать роль
            'code': 'doge',
            'warning': 'Such wow',
            'data': {'passport-login': 'frodo-baggins'}
        }
    ]

    generic_system.fetch_actual_workflow()
    for correct_answer in correct_answers:
        Role.objects.all().delete()
        with capture_raw_http(generic_system, answer=Response(200, correct_answer)):
            role = Role.objects.request_role(frodo, frodo, generic_system, '', {'role': 'manager'}, None)

        role = refresh(role)
        assert role.state == 'granted'
        assert role.system_specific == {'passport-login': 'frodo-baggins'}


@with_two_plugins
def test_fail_role_if_system_answered_incorrectly(generic_system, arda_users):
    """Проверим, что если система некорректным ответом, то мы переводим роль в статус 'ошибка' """
    frodo = arda_users.frodo

    incorrect_answers = [
        (400, {
            'code': 0,
        }),
        (500, {
            'code': 0,
        }),
        (700, {
            'code': 0,  # код не 200 – значит, ошибка
        }),
        (200, {
            'code': 1,  # при коде не 0 нужно передать error или warning или fatal
        }),
        (200, {
            'code': -1,
        }),
        (200, {
            'code': 500,
            'fatal': 'Кажется, что-то пошло не так',  # код не 0 + fatal или error приводят не невыдаче роли
        }),
        (200, {
            'code': 500,
            'error': 'Кажется, что-то пошло не так',  # код не 0 + fatal или error приводят не невыдаче роли
        }),
        (200, {
            'code': 500,
            'warning': 'Может быть, пропустим?',  # warning+error считаем error-ом, warning игнорируем
            'error': 'Кажется, что-то пошло не так',
        }),
        (200, {
            'code': 500,
            'warning': 'Может быть, пропустим?',  # warning+fatal считаем error-ом, warning игнорируем
            'fatal': 'Кажется, что-то пошло не так',
        }),
    ]

    generic_system.fetch_actual_workflow()
    for http_code, incorrect_answer in incorrect_answers:
        Role.objects.all().delete()
        with capture_raw_http(generic_system, answer=Response(http_code, incorrect_answer)):
            role = Role.objects.request_role(frodo, frodo, generic_system, '', {'role': 'manager'}, None)

        role = refresh(role)
        assert role.state == 'failed'


@with_two_plugins
def test_deprive_role_if_system_answered_correctly(generic_system, arda_users):
    """Проверим, что если система отвечает корректно, то мы отзываем роль"""

    frodo = arda_users.frodo
    correct_answers = [
        {
            'code': '0',  # строкой тоже можно передавать код 0
            'data': {'passport-login': 'frodo-baggins'}
        },
        {
            'code': 0,
            'data': {'passport-login': 'frodo-baggins'}
        },
        {
            'code': 0,
            'fatal': 'На самом деле ошибка-то!',  # дополнительные ключи игнорируются при коде 0
            'data': {'passport-login': 'frodo-baggins'}
        },
        {
            'code': '0',
            'fatal': 'На самом деле ошибка-то!',  # дополнительные ключи игнорируются при коде 0 текстом
            'data': {'passport-login': 'frodo-baggins'}
        },
        {
            'code': -2.5,
            'warning': 'Всё хорошо, роль можно считать выданной (ха-ха)',  # любой код не 0 + warning считается успехом
            'data': {'passport-login': 'frodo-baggins'}
        },
        {
            # даже если код совсем странный и строка, но наличие warning считается поводом выдать роль
            'code': 'doge',
            'warning': 'Such wow',
            'data': {'passport-login': 'frodo-baggins'}
        }
    ]

    generic_system.fetch_actual_workflow()
    for correct_answer in correct_answers:
        Role.objects.all().delete()
        role = raw_make_role(frodo, generic_system, {'role': 'manager'}, state='granted')
        with capture_raw_http(generic_system, answer=Response(200, correct_answer)):
            role.deprive_or_decline(frodo)

        role = refresh(role)
        assert role.state == 'deprived'


@with_two_plugins
def test_do_not_deprive_role_if_system_answered_incorrectly(generic_system, arda_users):
    """Проверим, что если система отвечает некорректно, то мы не отзываем роль"""

    frodo = arda_users.frodo
    incorrect_answers = [
        (400, {
            'code': 0,
        }),
        (500, {
            'code': 0,
        }),
        (700, {
            'code': 0,  # код не 200 – значит, ошибка
        }),
        (200, {
            'code': 1,  # при коде не 0 нужно передать error или warning или fatal
        }),
        (200, {
            'code': -1,
        }),
        (200, {
            'code': 500,
            'fatal': 'Кажется, что-то пошло не так',  # код не 0 + fatal или error приводят не невыдаче роли
        }),
        (200, {
            'code': 500,
            'error': 'Кажется, что-то пошло не так',  # код не 0 + fatal или error приводят не невыдаче роли
        }),
        (200, {
            'code': 500,
            'warning': 'Может быть, пропустим?',  # warning+error считаем error-ом, warning игнорируем
            'error': 'Кажется, что-то пошло не так',
        }),
        (200, {
            'code': 500,
            'warning': 'Может быть, пропустим?',  # warning+fatal считаем error-ом, warning игнорируем
            'fatal': 'Кажется, что-то пошло не так',
        }),
    ]

    generic_system.fetch_actual_workflow()
    for http_code, incorrect_answer in incorrect_answers:
        Role.objects.all().delete()
        role = raw_make_role(frodo, generic_system, {'role': 'manager'}, state='granted')

        with capture_raw_http(generic_system, answer=Response(http_code, incorrect_answer)):
            role.deprive_or_decline(frodo)

        role = refresh(role)
        assert role.state == 'depriving'


@with_two_plugins
def test_add_role_curl(generic_system, arda_users):
    """
    Тест добавления роли, проверяющий корректную посылку данных
    """
    frodo = arda_users.frodo
    frodo.passport_logins.create(login='yndx-frodo', state='created', is_fully_registered=True)
    generic_system.use_requests = False
    generic_system.save()

    response = Response(200, {'code': 0, 'data': {'passport-login': 'yndx-frodo'}})
    generic_system.fetch_actual_workflow()
    with capture_raw_http(generic_system, response) as fake_curl:
        role = Role.objects.request_role(frodo, frodo, generic_system, '', {'role': 'manager'},
                                         {'passport-login': 'yndx-frodo'})
        approve_action = role.actions.get(action='approve')
        if generic_system.plugin_type == 'generic':
            fields = '&'.join([
                'fields=%7B%22passport-login%22%3A+%22yndx-frodo%22%7D',
                'login=frodo',
                'path=%2Frole%2Fmanager%2F',
                'role=%7B%22role%22%3A+%22manager%22%7D'
            ])
        else:
            fields = '&'.join([
                'fields=%7B%22passport-login%22%3A+%22yndx-frodo%22%7D',
                'login=frodo',
                'role=%7B%22role%22%3A+%22manager%22%7D'
            ])
        headers = ['accept: application/json; charset=utf8', 'content-type: application/x-www-form-urlencoded', ]
        if generic_system.plugin_type == 'generic':
            headers.append(f'x-idm-request-id: {approve_action.id}')

        fake_curl.setopt.assert_has_calls(
            [
                call(pycurl.POST, 1),
                call(pycurl.URL, 'http://example.com/add-role/'),
                call(pycurl.POSTFIELDS, fields),
                call(pycurl.HTTPHEADER, headers),
                call(pycurl.CONNECTTIMEOUT, 60),
                call(pycurl.TIMEOUT, 60),
            ],
            any_order=True
        )

    role = refresh(role)
    assert role.state == 'granted'
    assert role.system_specific == {'passport-login': 'yndx-frodo'}


@with_two_plugins
def test_add_aware_group_role_request(aware_generic_system, arda_users, department_structure):
    """
    Тест добавления групповой роли в aware-системе, проверяющий корректную посылку данных.
    """

    fellowship = department_structure.fellowship
    aware_generic_system.fetch_actual_workflow()
    set_workflow(aware_generic_system, group_code=DEFAULT_WORKFLOW)
    answer = Response(200, {'code': 0, 'data': {'password': 'mellon', 'hello': 'world'}})
    with capture_raw_http(aware_generic_system, answer) as sender:
        role = Role.objects.request_role(arda_users.frodo, fellowship, aware_generic_system, '', {'role': 'manager'},
                                         {'login': 'fellowship'})

    expected_data = {
        'fields': '{"login": "fellowship"}',
        'group': fellowship.external_id,
        'role': '{"role": "manager"}'
    }
    if aware_generic_system.plugin_type == 'generic':
        expected_data.update({
            'path': '/role/manager/',
        })
    assert_raw_http(sender, method='POST', url='http://example.com/add-role/', data=expected_data)
    role = refresh(role)
    assert role.state == 'granted'
    assert role.system_specific == {'hello': 'world'}


@with_two_plugins
def test_remove_aware_group_role_request(aware_generic_system, arda_users, department_structure):
    """
    Тест отзыва групповой роли в aware-системе, проверяющий корректную посылку данных
    """

    fellowship = department_structure.fellowship
    role = raw_make_role(fellowship, aware_generic_system, {'role': 'manager'}, state='granted',
                         fields_data={'login': 'fellowship'},
                         system_specific={'login': 'fellowship'})

    ok_answer = Response(200, {'code': 0, 'data': {'login': 'fellowship'}})
    with capture_raw_http(aware_generic_system, answer=ok_answer) as sender:
        role.deprive_or_decline(arda_users.frodo)

    expected = {
        'data': '{"login": "fellowship"}',
        'group': fellowship.external_id,
        'role': '{"role": "manager"}'
    }
    if aware_generic_system.plugin_type == 'generic':
        expected.update({
            'path': '/role/manager/',
            'fields': '{"login": "fellowship"}',
        })
        del expected['data']
    assert_raw_http(sender, url='http://example.com/remove-role/', method='POST', data=expected)
    role = refresh(role)
    assert role.state == 'deprived'
    assert not role.is_active
    assert role.system_specific == {'login': 'fellowship'}


@with_two_plugins
def test_get_info_next_urls(generic_system):
    """Тест на корректное разворачивание next-url-ов"""

    def bunker(url, **kwargs):
        result1 = {
            'code': 0,
            'roles': {
                'slug': 'level0',
                'name': 'Проект',
                'values': {
                    't-test': {
                        'name': 't-test',
                        'roles': {
                            'slug': 'level1',
                            'name': '/',
                            'values': {
                                'next-url': '/info/?node=%2Ft-test'
                            }
                        }
                    }
                }
            }
        }
        result2 = {
            'code': 0,
            'roles': {
                'slug': 'level1',
                'name': '/',
                'values': {
                    '*': {
                        'name': 'Выбор узла завершен (/t-test)',
                        'roles': {
                            'slug': 'role',
                            'name': 'Уровень доступа',
                            'values': {
                                'store': 'Редактирование данных',
                                'publish': 'Публикация узлов',
                                'grant': 'Управление правами'
                            }
                        }
                    }
                }
            }
        }
        if url == 'http://example.com/info/':
            result = result1
        else:
            result = result2
        return Response(200, result)

    with capture_http(generic_system, side_effect=bunker):
        info = generic_system.plugin.get_info()

    assert info == {
        'code': 0,
        'roles': {
            'slug': 'level0',
            'name': 'Проект',
            'values': {
                't-test': {
                    'name': 't-test',
                    'roles': {
                        'name': '/',
                        'slug': 'level1',
                        'values': {
                            '*': {
                                'name': 'Выбор узла завершен (/t-test)',
                                'roles': {
                                    'slug': 'role',
                                    'name': 'Уровень доступа',
                                    'values': {
                                        'store': 'Редактирование данных',
                                        'publish': 'Публикация узлов',
                                        'grant': 'Управление правами'
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }


@with_two_plugins
def test_get_roles_next_url(generic_system):
    result1 = {
        'roles': [
            {
                'login': 'frodo',
                'path': '/role/manager/',
            },
            {
                'login': 'legolas',
                'path': '/role/manager/',
                'subject_type': 'tvm_app',
            }
        ],
        'next-url': '/get-roles/?next'
    }
    result2 = {
        'roles': [
            {
                'login': 'frodo',
                'path': '/role/admin/',
                'fields': {'login': 'legolas'}
            },
        ]
    }

    def streaming_roles(method, url, timeout, **kwargs):
        assert method == 'GET'
        assert timeout == 60
        if url == 'http://example.com/get-roles/':
            result = result1
        else:
            result = result2
        result['code'] = 0
        return Response(200, result)

    with capture_raw_http(generic_system, side_effect=streaming_roles):
        roles = list(generic_system.plugin.get_roles())

    assert roles == [
        {
            'login': 'frodo',
            'path': '/role/manager/',
        },
        {
            'login': 'legolas',
            'path': '/role/manager/',
            'subject_type': 'tvm_app',
        },
        {
            'login': 'frodo',
            'path': '/role/admin/',
            'fields': {'login': 'legolas'}
        }
    ]


@with_two_plugins
def test_next_url_could_not_belong_to_another_domain(generic_system):
    result1 = {
        'roles': [{
            'login': 'frodo',
            'path': '/role/manager/',
        }],
        'next-url': 'http://google.com/get-roles/?next'
    }
    result2 = {
        'roles': [{
            'login': 'frodo',
            'path': '/role/admin/',
            'fields': {'login': 'legolas'}
        }]
    }

    def streaming_roles(method, url, timeout, **kwargs):
        assert method == 'GET'
        assert timeout == 60
        if url == 'http://example.com/get-roles/':
            result = result1
        else:
            result = result2
        result['code'] = 0
        return Response(200, result)

    with capture_raw_http(generic_system, side_effect=streaming_roles):
        roles = list(generic_system.plugin.get_roles())

        assert roles == [{
            'login': 'frodo',
            'path': '/role/manager/',
        }]
        result1['next-url'] = 'http://example.com/get-roles/?next'
        roles = list(generic_system.plugin.get_roles())
        assert roles == [{
            'login': 'frodo',
            'path': '/role/manager/',
        }, {
            'login': 'frodo',
            'path': '/role/admin/',
            'fields': {'login': 'legolas'}
        }]


@with_two_plugins
@pytest.mark.parametrize('batch_size', [1, 100])
@pytest.mark.parametrize('method_name', ['add-batch-memberships', 'remove-batch-memberships'])
@pytest.mark.parametrize('group_policy', SYSTEM_GROUP_POLICY.AWARE_OF_MEMBERSHIPS)
def test_push_membership(generic_system, department_structure, batch_size, method_name, group_policy):
    params = {
        'add-batch-memberships': {
            'initial_state': 'activating',
            'final_state': 'activated',
            'method': generic_system.push_activating_group_memberships_async
        },
        'remove-batch-memberships': {
            'initial_state': 'depriving',
            'final_state': 'deprived',
            'method': generic_system.push_depriving_group_memberships_async
        }
    }[method_name]
    generic_system.group_policy = group_policy
    generic_system.push_batch_size = batch_size
    generic_system.save()
    fellowship = department_structure.fellowship
    shire = department_structure.shire
    valinor = department_structure.valinor
    group_ids = sorted([fellowship.id, shire.id, valinor.id])
    membership_ids = (
        GroupMembership.objects
        .filter(group_id__in=group_ids, state__in=GROUPMEMBERSHIP_STATE.ACTIVE_STATES)
        .values_list('id', flat=True)
    )
    GroupMembershipSystemRelation.objects.bulk_create_groupmembership_system_relations(membership_ids, generic_system)
    GroupMembershipSystemRelation.objects.update(state=params['initial_state'])

    with capture_http(generic_system, {'code': 0}) as mocked:
        params['method']()
    assert mocked.http_post.call_args[0][0] == 'http://example.com/{}/'.format(method_name)
    if generic_system.plugin_type == 'generic':
        assert 'X-IDM-Request-Id' in mocked.http_post.call_args[1]['headers']
    # Число вызовов равно числу батчей
    n_batches = ceil(GroupMembershipSystemRelation.objects.count() / float(batch_size))
    assert mocked.http_post.call_count == n_batches
    # Проверим запушенные данные
    pushed_memberships_data = [element
                               for call in mocked.http_post.call_args_list
                               for element in json.loads(call[1]['data']['data'])]
    assert len(set(map(str, pushed_memberships_data))) == GroupMembershipSystemRelation.objects.count()
    for element in pushed_memberships_data:
        membership = GroupMembershipSystemRelation.objects.get(
            membership__group__external_id=element['group'],
            membership__user__username=element['login']
        )
        assert membership.state == params['final_state']


@with_two_plugins
def test_get_memberships_next_url(generic_system):
    # Аналогично test_get_roles_next_url, next_url_could_not_belong_to_another_domain,
    # только для get_memberships
    result1 = {
        'memberships': [
            {
                'login': 'frodo',
                'group': 'fellowship',
                'passport_login': '',
            },
            {
                'login': 'bilbo',
                'group': 'the-shire',
                'passport_login': '',
            }
        ],
        'next-url': '/get-memberships/?next'
    }
    result2 = {
        'memberships': [
            {
                'login': 'frodo',
                'group': 'the-shire',
                'passport_login': '',
            },
        ],
        'next-url': 'http://microsoft.com/get-memberships/?next'
    }
    result3 = {
        'memberships': [
            {
                'login': 'gill_bates',
                'group': 'apple_fans',
                'passport_login': '',
            }
        ]
    }

    def streaming_memberships(method, url, timeout, **kwargs):
        assert method == 'GET'
        assert timeout == 60
        if url == 'http://example.com/get-memberships/':
            result = result1
        elif url == 'http://example.com/get-memberships/?next':
            result = result2
        else:
            result = result3
        result['code'] = 0
        return Response(200, result)

    with capture_raw_http(generic_system, side_effect=streaming_memberships):
        memberships = list(generic_system.plugin.get_memberships())

    assert memberships == [
        {
            'login': 'frodo',
            'group': 'fellowship',
            'passport_login': '',
        },
        {
            'login': 'bilbo',
            'group': 'the-shire',
            'passport_login': '',
        },
        {
            'login': 'frodo',
            'group': 'the-shire',
            'passport_login': '',
        },
    ]


@pytest.mark.parametrize('data', [
    {
        'a': 'b', 'token': 'my big secret', 'Secret-Token': 111,
        'c': ['d', {'Token': 'secret', 'e': 'f'}, 123], 'g': {}
    },
    'simple string',
    123,
    None,
    ['token'],
])
def test_remove_sensitive_keys_from_error_message(data):
    plugin_error = BasePluginError(1, 'something', data)

    from mock import Mock
    mock = Mock(side_effect=plugin_error)
    with pytest.raises(BasePluginError) as ex:
        mock()
    if isinstance(data, dict):
        assert ex.value.data == {
            'a': 'b',
            'token': SECRET_DATA_PLACEHOLDER,
            'Secret-Token': SECRET_DATA_PLACEHOLDER,
            'c': ['d', {'Token': SECRET_DATA_PLACEHOLDER, 'e': 'f'}, 123],
            'g': {},
        }
    else:
        assert ex.value.data == data


@with_two_plugins
def test_uid_passed_to_plugin(generic_system, arda_users):
    frodo = arda_users.frodo
    frodo.uid = 'frodo-uid-123'
    frodo.save(update_fields=['uid'])
    generic_system.push_uid = True
    generic_system.save(update_fields=['push_uid'])
    generic_system.fetch_actual_workflow()
    with capture_http(generic_system, OK_ANSWER) as mocked:
        role = Role.objects.request_role(frodo, frodo, generic_system, '', {'role': 'manager'}, None)

    expected_data = {
        'login': 'frodo',
        'uid': 'frodo-uid-123',
        'role': '{"role": "manager"}',
        'fields': 'null',
    }
    if generic_system.plugin_type == 'generic':
        expected_data['path'] = '/role/manager/'
    assert_http(mocked.http_post, url='http://example.com/add-role/', data=expected_data, timeout=60)
    if generic_system.plugin_type == SYSTEM_PLUGIN_TYPE.GENERIC_LEGACY:
        expected_data.pop('fields')
        expected_data['data'] = 'null'
    with capture_http(generic_system, OK_ANSWER) as mocked:
        refresh(role).deprive_or_decline(frodo, bypass_checks=True)
    assert_http(mocked.http_post, url='http://example.com/remove-role/', data=expected_data)
