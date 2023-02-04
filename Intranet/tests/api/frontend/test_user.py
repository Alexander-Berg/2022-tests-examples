# coding: utf-8
import contextlib
import datetime
import re

import pytest
from django.test.utils import override_settings
from django.utils import timezone
from django.utils.timezone import make_aware

from idm.core.models import Action
from idm.tests.utils import create_user, set_roles_tree, raw_make_role, random_slug, create_system
from idm.users.models import Group, User
from idm.utils import reverse

pytestmark = pytest.mark.django_db


def users_urls(api_name):
    urls = {
        'frontend': reverse('api_dispatch_list', api_name='frontend', resource_name='users'),
        'v1': reverse('api_dispatch_list', api_name='v1', resource_name='users'),
    }
    return urls[api_name]


def user_url(username):
    return reverse('api_dispatch_detail', api_name='frontend', resource_name='users', username=username)


def user_logins_url(username):
    return reverse('api_get_passport_logins', api_name='frontend', resource_name='users', username=username)


@pytest.mark.robotless
@pytest.mark.parametrize('api_name', ['frontend', 'v1'])
def test_get_users(client, users_for_test, api_name):
    """
    GET /frontend/users/
    """
    (art, fantom, terran, admin) = users_for_test
    fantom.is_active = False
    fantom.save()

    # Нет департамента - не синхронизирован со стаффом
    terran.department = None
    terran.save()

    client.login('art')

    users_url = users_urls(api_name)
    data = client.json.get(users_url).json()

    assert data['meta']['total_count'] == 4
    if api_name == 'frontend':
        assert data['objects'][0].keys() == {
            'username',
            'fired_at',
            'is_active',
            'sex',
            'full_name',
            'position',
            'department_group',
            'email',
            'date_joined',
            'type',
            'notify_responsibles',
            'is_robot',
        }
    else:
        assert data['objects'][0].keys() == {
            'username',
            'fired_at',
            'is_active',
            'sex',
            'full_name',
            'first_name',
            'last_name',
            'position',
            'department_group',
            'email',
            'date_joined',
            'type',
            'notify_responsibles',
            'is_robot',
        }
        centurio = next(item for item in data['objects'] if item['username'] == 'art')
        assert centurio['first_name'] == {
            'ru': 'Центурион',
            'en': 'Centurio',
        }
        assert centurio['last_name'] == {
            'ru': 'Марк',
            'en': 'Mark',
        }
        assert centurio['full_name'] == {
            'ru': 'Центурион Марк',
            'en': 'Centurio Mark',
        }
        # TODO: пофиксить при переходе на staff api v3
        assert centurio['position'] == {
            'ru': 'centurio',
            'en': 'centurio',
        }

    assert [item['username'] for item in data['objects']] == ['admin', 'art', 'fantom', 'terran']
    assert [item['is_active'] for item in data['objects']] == [True, True, False, True]

    #is_active=False
    data = client.json.get(users_url, {'is_active': False}).json()

    assert data['meta']['total_count'] == 1
    assert all(not obj['is_active'] for obj in data['objects'])

    # is_active=True
    data = client.json.get(users_url, {'is_active': True}).json()

    assert data['meta']['total_count'] == 3
    assert all(obj['is_active'] for obj in data['objects'])


@pytest.mark.parametrize('api_name', ['frontend', 'v1'])
def test_get_users_updated(client, api_name, arda_users):
    client.login('frodo')
    users_url = users_urls(api_name)

    arda_users.pop('tvm_app')
    for i, username in enumerate(arda_users):
        dt = make_aware(datetime.datetime.fromtimestamp(i*3600))
        User.objects.filter(username=username).update(updated=dt)

    response = client.json.get(users_url, {
        'updated__since': datetime.datetime.fromtimestamp(3*3600),
        'updated__until': datetime.datetime.fromtimestamp(7*3600),
    })

    assert response.status_code == 200
    assert len(response.json()['objects']) == 5


def test_get_user(client, users_for_test):
    """
    GET /frontend/users/art/
    """
    (art, fantom, terran, admin) = users_for_test

    client.login('art')

    data = client.json.get(user_url('art')).json()
    assert data['username'] == 'art'
    assert data['is_active'] is True

    # пользователь с логином, начинающимся с цифры, получает нормальный ответ
    create_user('21st')
    data = client.json.get(user_url('21st')).json()
    assert data['username'] == '21st'
    assert data['is_active'] is True

    # пользователь с логином schema конфликтует со служебным урлом /schema/
    create_user('schema')
    data = client.json.get(user_url('schema')).json()
    assert 'username' not in data

    # но только при полном совпадении
    create_user('schemakov')
    data = client.json.get(user_url('schemakov')).json()
    assert data['username'] == 'schemakov'
    assert data['is_active'] is True

    root = Group.objects.get_root('department')
    g1 = Group.objects.create(external_id=42, slug='foo', name='Г1', name_en='g1', parent=root)
    g2 = Group.objects.create(external_id=43, slug='buz', name='Г2', name_en='g2', parent=g1)

    art.department_group = g2
    art.save(update_fields=('department_group',))

    data = client.json.get(user_url('art')).json()
    assert data['departments'] == [{'id': g1.external_id, 'name': 'Г1', 'url': 'foo'},
                                   {'id': g2.external_id, 'name': 'Г2', 'url': 'buz'}]
    client.login('art', lang='en')
    data = client.json.get(user_url('art')).json()
    assert data['departments'] == [{'id': g1.external_id, 'name': 'g1', 'url': 'foo'},
                                   {'id': g2.external_id, 'name': 'g2', 'url': 'buz'}]


@pytest.mark.robotless
@pytest.mark.parametrize('api_name', ['frontend', 'v1'])
def test_tvm(client, arda_users, api_name):
    client.login('frodo')
    users_url = users_urls(api_name)

    users = client.json.get(users_url).json()['objects']
    assert len(users) == len(arda_users) - 1
    assert arda_users.tvm_app.username not in {x['username'] for x in users}

    tvm_app = client.json.get(user_url(arda_users.tvm_app.username)).json()
    assert tvm_app['is_synced_with_staff']


def test_passport_logins_suggest(client, simple_system, arda_users):
    frodo = arda_users.frodo
    set_roles_tree(simple_system, {
        'fields': [
            {
                'name': 'Паспортный логин',
                'required': True,
                'slug': 'passport-login'
            }
        ],
        'roles': {
            'name': 'Интерфейс',
            'slug': 'interface',
            'values': {
                'group': {
                    'name': 'group',
                    'roles': {
                        'name': 'Роль',
                        'slug': 'role',
                        'values': {
                            'role_one':
                                {'help': 'Роль 1', 'name': 'role_one'},
                            'role_one_x':
                                {'help': 'Роль тоже 1', 'name': 'role_one_x'},
                            'role_two':
                                {'help': 'Роль 2', 'name': 'role_two'},
                            'role_three_with_very_long_name':
                                {'help': 'Роль 3', 'name': 'role_three'},
                        }
                    }
                }
            }
        }
    })

    raw_make_role(frodo, simple_system, {'interface': 'group', 'role': 'role_two'},
                  {'passport-login': 'yndx-frodo-group-role-two'}, state='granted')
    client.login('frodo')

    # Не передаем роль
    data = client.json.get(user_logins_url('frodo'), {'system': simple_system.slug}).json()
    assert 'available' in data
    assert data['available'] == ['yndx-frodo', 'yndx-frodo-group',
                                 'yndx-frodo-group-role-one', 'yndx-frodo-group-role-one-x']

    # Не передаем роль и систему
    data = client.json.get(user_logins_url('frodo')).json()
    assert 'available' in data
    assert data['available'] == ['yndx-frodo']

    # Передаем роль
    data = client.json.get(user_logins_url('frodo'), {
        'system': simple_system.slug,
        'path': '/group/role_two/'
    }).json()
    assert 'available' in data
    assert data['available'] == ['yndx-frodo', 'yndx-frodo-role-two', 'yndx-frodo-group-role-two-1']

    # Занимаем основной логин
    raw_make_role(frodo, simple_system, {'interface': 'group', 'role': 'role_one'},
                  {'passport-login': 'yndx-frodo'}, state='granted')

    # Не передаем роль и систему
    data = client.json.get(user_logins_url('frodo')).json()
    assert 'available' in data
    assert data['available'] == ['yndx-frodo-1']

    # Занимаем альтернативный логин без системы
    raw_make_role(frodo, simple_system, {'interface': 'group', 'role': 'role_one_x'},
                  {'passport-login': 'yndx-frodo-1'}, state='granted')

    # Не передаем роль и систему
    data = client.json.get(user_logins_url('frodo')).json()
    assert 'available' in data
    assert data['available'] == ['yndx-frodo-2']

    # Передаем роль
    data = client.json.get(user_logins_url('frodo'), {
        'system': simple_system.slug,
        'path': '/group/role_two/'
    }).json()
    assert 'available' in data
    assert data['available'] == ['yndx-frodo-role-two', 'yndx-frodo-group-role-two-1']

    # Занимаем первый альтернативный логин
    raw_make_role(frodo, simple_system, {'interface': 'group', 'role': 'role_one'},
                  {'passport-login': 'yndx-frodo-group-role-two-1'}, state='granted')

    # Передаем роль
    data = client.json.get(user_logins_url('frodo'), {
        'system': simple_system.slug,
        'path': '/group/role_two/'
    }).json()
    assert 'available' in data
    assert data['available'] == ['yndx-frodo-role-two', 'yndx-frodo-group-role-two-2']

    # Занимаем логины
    for i in range(2, 100):
        raw_make_role(frodo, simple_system, {'interface': 'group', 'role': 'role_one'},
                      {'passport-login': 'yndx-art-group-role-two-%s' % i}, state='granted')

    # Передаем роль
    data = client.json.get(user_logins_url('frodo'), {
        'system': simple_system.slug,
        'path': '/group/role_two/'
    }).json()
    assert 'available' in data
    available_set = set(data['available'])
    available_set.remove('yndx-frodo-role-two')
    assert len(available_set) == 1
    login = available_set.pop()
    assert re.compile('yndx-frodo-group-role-two-\d+').match(login)

    # Просим логин на очень длинную роль
    data = client.json.get(user_logins_url('frodo'), {
        'system': simple_system.slug,
        'path': '/group/role_three_with_very_long_name/'
    }).json()
    assert 'available' in data
    assert data['available'] == ['yndx-frodo-simple']

    raw_make_role(frodo, simple_system, {'interface': 'group', 'role': 'role_one'},
                  {'passport-login': 'yndx-frodo-simple'}, state='granted')

    data = client.json.get(user_logins_url('frodo'), {
        'system': simple_system.slug,
        'path': '/group/role_three_with_very_long_name/'
    }).json()
    assert 'available' in data
    assert data['available'] == ['yndx-frodo-simple-1']

    # Просим логины для пользователя, у которого логин начинается с цифры
    create_user('21stme')
    data = client.json.get(user_logins_url('21stme'), {
        'system': simple_system.slug
    }).json()
    assert 'available' in data
    assert data['available'] == ['yndx-21stme', 'yndx-21stme-group', 'yndx-21stme-group-role-one',
                                 'yndx-21stme-group-role-two', 'yndx-21stme-group-role-one-x']


def test_current_passport_logins_list_has_no_duplicate_entries(client, simple_system, arda_users):
    """В списке паспортных логинов каждый логин встречается только один раз, даже если на него выдано несколько ролей"""

    frodo = arda_users.frodo
    set_roles_tree(simple_system, {
        'fields': [
            {
                'name': 'Паспортный логин',
                'required': True,
                'slug': 'passport-login'
            }
        ],
        'roles': {
            'name': 'Интерфейс',
            'slug': 'interface',
            'values': {
                'group': {
                    'name': 'group',
                    'roles': {
                        'name': 'Роль',
                        'slug': 'role',
                        'values': {
                            'role_one': 'Роль 1',
                            'role_two': 'Роль 2'
                        }
                    }
                }
            }
        }
    })
    # выдаём две роли на один логин. В списке текущих логинов yndx-art-group-role-one
    # должен быть показан только один раз
    raw_make_role(frodo, simple_system, {'interface': 'group', 'role': 'role_one'},
                  {'passport-login': 'yndx-frodo-group-role-one'}, state='granted')
    raw_make_role(frodo, simple_system, {'interface': 'group', 'role': 'role_two'},
                  {'passport-login': 'yndx-frodo-group-role-one'}, state='granted')
    data = client.json.get(user_logins_url('frodo'), {
        'system': simple_system.slug,
        'path': '/group/role_two/'
    }).json()
    assert data == {
        'current': ['yndx-frodo-group-role-one'],
        'available': ['yndx-frodo', 'yndx-frodo-role-two', 'yndx-frodo-group-role-two']
    }


def test_passport_logins_suggest_unique_for_user_policy(client, simple_system, arda_users):
    """Проверка, что если у системы включена политика 'один логин на все роли', то возвращается всегда один логин"""

    frodo = arda_users.frodo
    set_roles_tree(simple_system, {
        'fields': [
            {
                'name': 'Паспортный логин',
                'required': True,
                'slug': 'passport-login'
            }
        ],
        'roles': {
            'name': 'Интерфейс',
            'slug': 'interface',
            'values': {
                'group': {
                    'name': 'group',
                    'roles': {
                        'name': 'Роль',
                        'slug': 'role',
                        'values': {
                            'role_one':
                                {'help': 'Роль 1', 'name': 'role_one'},
                            'role_two':
                                {'help': 'Роль 2', 'name': 'role_two'},
                            'role_three_with_very_long_name':
                                {'help': 'Роль 3', 'name': 'role_three'},
                        }
                    }
                }
            }
        }
    })
    simple_system.passport_policy = 'unique_for_user'
    simple_system.save()

    # Не передаем роль
    data = client.json.get(user_logins_url('frodo'), {'system': simple_system.slug}).json()
    assert data['available'] == ['yndx-frodo',
                                 'yndx-frodo-group',
                                 'yndx-frodo-group-role-one',
                                 'yndx-frodo-group-role-two']
    assert data['current'] == []

    # Передаем роль
    data = client.json.get(user_logins_url('frodo'), {
        'system': simple_system.slug,
        'path': '/group/role_two/'
    }).json()

    assert data['available'] == ['yndx-frodo',
                                 'yndx-frodo-role-two',
                                 'yndx-frodo-group-role-two']
    assert data['current'] == []

    # захватим логин, для которого запрашивали роль
    raw_make_role(frodo, simple_system, {'interface': 'group', 'role': 'role_two'},
                  {'passport-login': 'yndx-frodo-group-role-two'}, state='granted')
    client.login('frodo')

    # Передаем роль
    data = client.json.get(user_logins_url('frodo'), {
        'system': simple_system.slug,
        'path': '/group/role_two/'
    }).json()
    assert data['available'] == []
    assert data['current'] == ['yndx-frodo-group-role-two']

    # Занимаем основной логин
    raw_make_role(frodo, simple_system, {'interface': 'group', 'role': 'role_two'},
                  {'passport-login': 'yndx-frodo'}, state='granted')

    # Передаем роль
    data = client.json.get(user_logins_url('frodo'), {
        'system': simple_system.slug,
        'path': '/group/role_two/'
    }).json()
    assert data['available'] == []
    assert data['current'] == ['yndx-frodo']

    # Не передаем роль
    data = client.json.get(user_logins_url('frodo'), {'system': simple_system.slug}).json()
    assert data['available'] == []
    assert data['current'] == ['yndx-frodo']


def test_passport_logins_in_different_systems(client, simple_system, pt1_system, arda_users):
    frodo = arda_users.frodo

    raw_make_role(frodo, simple_system, {'role': 'admin'}, {'passport-login': 'yndx-frodo-simple'}, state='granted')
    raw_make_role(frodo, pt1_system, {'project': 'proj1', 'role': 'admin'},
                  {'passport-login': 'yndx-frodo-pt1'}, state='granted')

    client.login('frodo')
    data = client.json.get(user_logins_url('frodo'), {'system': simple_system.slug}).json()
    assert data['current'] == ['yndx-frodo-pt1', 'yndx-frodo-simple']


def test_passport_logins_with_root_rolenode(client, simple_system, arda_users):
    """Проверим, что если узел не задан или задан корневой, то мы отдаём список логинов,
    сгенерированный по дереву ролей"""

    expected = {
        'available': ['yndx-frodo',
                      'yndx-frodo-admin',
                      'yndx-frodo-manager',
                      'yndx-frodo-poweruser',
                      'yndx-frodo-superuser'],
        'current': []
    }

    data = client.json.get(user_logins_url('frodo'), {'system': simple_system.slug, 'path': '/'}).json()
    assert data == expected
    data = client.json.get(user_logins_url('frodo'), {'system': simple_system.slug}).json()
    assert data == expected
    # передадим path, но не передадим систему - получим bad request
    response = client.json.get(user_logins_url('frodo'), {'path': '/'})
    assert response.status_code == 400


@pytest.mark.parametrize('support_external', [True, False])
def test_get_passportlogins__exclude_external_logins(client, support_external: bool):
    user = create_user()

    external_login = user.passport_logins.create(login='yndx.me', created_by_idm=False)
    internal_login = user.passport_logins.create(login='yndx-me', created_by_idm=False)

    @contextlib.contextmanager
    def empty_ctx():
        yield

    ctx_manager = empty_ctx()
    system = create_system()
    if not support_external:
        ctx_manager = override_settings(
            SYSTEMS_SUPPORT_ONLY_IDM_CREATED_PASSPORT_LOGINS={random_slug(), system.slug}
        )

    with ctx_manager:
        response = client.json.get(user_logins_url(user.username), {'system': system.slug, 'path': '/'})

    assert response.status_code == 200, response.json()
    result = response.json()
    if support_external:
        assert sorted(result['current']) == sorted([external_login.login, internal_login.login])
    else:
        assert result['current'] == [internal_login.login]


def test_unknown_user(client, simple_system, arda_users):
    client.login(random_slug())
    response = client.json.get('/ping-backend/')
    assert response.status_code == 200

    response = client.json.get(users_urls('frontend'))
    assert response.status_code == 401


@pytest.mark.xfail
def test_timezone(client, simple_system, arda_users):
    """Нужно протестировать, что у пользователя меняется таймзона, когда мы начнём синхронизировать её со Стаффа"""

    frodo = arda_users.frodo
    frodo.timezone = 'Asia/Yekaterinburg'
    client.login('frodo')
    response = client.json.get('/ping-backend/')
    assert response.status_code == 200
    assert timezone.get_current_timezone() == 'Asia/Yekaterinburg'


@pytest.mark.parametrize('is_responsible', [True, False])
@pytest.mark.parametrize('is_robot', [True, False])
def test_patch_user_notify_responsibles(client, arda_users, robot_gollum, is_responsible, is_robot):
    """
    PATCH /frontend/users/
    проверяем изменение параметра notify_responsibles
    """
    frodo = arda_users.frodo
    gollum = robot_gollum
    gollum.is_robot = is_robot
    gollum.save()
    assert gollum.notify_responsibles is False
    if is_responsible:
        gollum.add_responsibles([frodo])

    client.login('frodo')
    data = {'notify_responsibles': True}
    response = client.json.patch(user_url(gollum.username), data)

    gollum.refresh_from_db()
    actions = Action.objects.filter(robot=gollum, requester=frodo)
    if is_responsible and is_robot:
        assert response.status_code == 200
        assert gollum.notify_responsibles is True
        assert actions.count() == 1
        assert actions.first().data == {'notify_responsibles': True}
    else:
        assert response.status_code == 403
        assert gollum.notify_responsibles is False
        assert actions.count() == 0


@pytest.mark.parametrize('allowed', [True, False])
@pytest.mark.parametrize('is_robot', [True, False])
def test_patch_user_is_frozen(client, arda_users, robot_gollum, allowed, is_robot):
    """
    PATCH /frontend/users/
    проверяем изменение параметра is_frozen
    """
    frodo = arda_users.frodo
    gollum = robot_gollum
    gollum.is_robot = is_robot
    gollum.save()

    client.login('frodo')
    data = {'is_frozen': True}
    with override_settings(ABC_ROBOT_LOGIN=('frodo' if allowed else 'some_login')):
        response = client.json.patch(user_url(gollum.username), data)

    gollum.refresh_from_db()
    actions = Action.objects.filter(robot=gollum, requester=frodo)
    if allowed:
        assert response.status_code == 200
        assert gollum.is_frozen is True
        assert gollum.notify_responsibles is False
        assert actions.count() == 1
        assert actions.first().data == {'is_frozen': True}
    else:
        assert response.status_code == 403
        assert gollum.is_frozen is False
        assert gollum.notify_responsibles is False
        assert actions.count() == 0
