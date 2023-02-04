from contextlib import contextmanager
from itertools import count
from textwrap import dedent
from typing import List, Dict, Union
from unittest import mock

import pytest
import yaml
from _pytest.monkeypatch import MonkeyPatch
from django.core import management
from django.core.cache import cache
from django.core.management import call_command
from django.db import connection
from django.utils import translation

from idm.core.constants.system import SYSTEM_AUDIT_METHOD, SYSTEM_REQUEST_POLICY, SYSTEM_GROUP_POLICY
from idm.core.models import Role, RoleNodeSet, RoleField, RoleNode, NodeResponsibility, RoleAlias, UserPassportLogin
from idm.nodes.hashers import Hasher
from idm.permissions.utils import add_perms_by_role, clear_permission_cache
from idm.services.models import Service
from idm.tests.utils import (create_user, create_system, set_workflow,
                             set_roles_tree, create_group_structure, DEFAULT_WORKFLOW, attrdict,
                             IDMClient, make_role, refresh, mock_tree, raw_make_role, get_idm_robot,
                             ensure_group_roots, sync_role_nodes, batch_sql, LocalFetcher,
                             add_members, _MockedMongo)
from idm.users.constants.group import GROUP_TYPES
from idm.users.constants.user import USER_TYPES
from idm.users.models import Group, User
from idm.users.sync.groups import sync_indirect_memberships
from idm.utils import reverse
from idm.utils.sql import (
    generate_index_for_roles,
    generate_index_for_nodes,
    generate_indexes_for_node_responsibility,
    generate_index_for_rolealias,
)


@pytest.fixture(scope='session')
def session_monkeypatch():
    mpatch = MonkeyPatch()
    yield mpatch
    mpatch.undo()


# pytest фикстуры
@pytest.fixture()
def debug(settings):
    settings.DEBUG = True
    settings.IDM_SQL_DEBUG = True


@pytest.fixture(autouse=True, scope='session')
def no_lock(session_monkeypatch):
    """Для юнит-тестов локи отключены
    """

    @contextmanager
    def _lock(*args, **kwargs):
        yield True

    session_monkeypatch.setattr('idm.utils.lock.manager.lock', _lock)


@pytest.fixture(autouse=True, scope='session')
def no_inflect(session_monkeypatch):
    """Для тестов ничего нигде не склоняем,
    чтобы не ломаться, если склонялка лежит
    """
    session_monkeypatch.setattr('idm.utils.human._inflect',
                                lambda _, word, *args, **kwargs: word)


@pytest.fixture(autouse=True, scope='session')
def no_external_calls(session_monkeypatch):
    """Для тестов никуда по урлам не ходим, все локально"""
    from socket import getaddrinfo

    monkeypatch = session_monkeypatch

    def opener(patched):
        def wrapper(url, *args, **kwargs):
            raise SystemExit("You forget to mock %s locally, it tries to open %s" % (patched, url))

        return wrapper

    monkeypatch.setattr('requests.request', opener('requests.request'))

    def get_class(patched):
        def wrapper(*args, **kwargs):
            raise SystemExit("You forget to mock %s locally" % patched)

        return wrapper

    monkeypatch.setattr('urllib.request.URLopener', get_class('urllib.URLopener'))
    monkeypatch.setattr(
        'requests.packages.urllib3.connection.VerifiedHTTPSConnection',
        get_class('requests.packages.urllib3.connection.VerifiedHTTPSConnection')
    )
    monkeypatch.setattr(
        'requests.packages.urllib3.connection.HTTPConnection',
        get_class('requests.packages.urllib3.connection.HTTPConnection')
    )
    monkeypatch.setattr(
        'urllib3.connectionpool.HTTPConnectionPool.ConnectionCls',
        get_class('urllib3.connectionpool.HTTPConnectionPool.ConnectionCls')
    )
    monkeypatch.setattr(
        'urllib3.connectionpool.HTTPConnectionPool.ConnectionCls',
        get_class('urllib3.connectionpool.HTTPConnectionPool.ConnectionCls')
    )

    def get_curl():
        raise SystemExit("You forget to mock pycurl locally")

    monkeypatch.setattr('pycurl.Curl', get_curl)

    def guarded_getaddrinfo(host, port, *args, **kwargs):
        if host in ('mongo', '127.0.0.1', 'localhost'):
            return getaddrinfo(host, port, *args, **kwargs)
        raise SystemExit('You are trying to connect to host %s on port %s not mocking it' % (host, port))

    monkeypatch.setattr('socket.getaddrinfo', guarded_getaddrinfo)


@pytest.fixture(autouse=True)
def autodrop_cache():
    cache.clear()
    clear_permission_cache()


@pytest.fixture(autouse=True)
def mock_gap():
    with mock.patch('idm.sync.gap.is_user_absent') as is_user_absent:
        is_user_absent.return_value = False
        yield is_user_absent


@pytest.fixture(autouse=True, scope='session')
def no_socket_get_hostname(session_monkeypatch):
    session_monkeypatch.setattr('idm.sync.passport._get_current_ip', lambda: b'127.0.0.1')


@pytest.fixture(autouse=True, scope='function')  # нельзя сделать scope=session из-за отмены мока в sync/test_passport
def no_passport(monkeypatch):
    monkeypatch.setattr('idm.sync.passport.exists', lambda *args, **kwargs: False)
    monkeypatch.setattr('idm.sync.passport.register_login', lambda *args, **kwargs: 'pass')
    monkeypatch.setattr('idm.sync.passport.set_strongpwd', lambda *args, **kwargs: True)
    monkeypatch.setattr('idm.sync.passport.remove_strongpwd', lambda *args, **kwargs: True)


@pytest.fixture(scope='session', autouse=True)
def db_postsetup(request, django_db_setup, no_lock, django_db_blocker):
    django_db_blocker.unblock()
    request.addfinalizer(django_db_blocker.restore)
    if connection.vendor == 'postgresql':
        call_command('idm_load_views', 'users', verbosity=0)
        call_command('idm_load_views', 'core', verbosity=0)

    # Индексы для ролей
    indices = list(generate_index_for_roles(concurrently=False))
    roles_fw, roles_bw = zip(*indices)

    # Индексы для узлов
    nodes_fw, nodes_bw = generate_index_for_nodes()

    # Индекс для RoleAlias
    rolealias_fw, rolealias_bw = generate_index_for_rolealias()

    # Индексы для NodeResponsibility
    noderesps_fw, noderesps_bw = generate_indexes_for_node_responsibility()

    forwards = list(roles_fw) + [nodes_fw, rolealias_fw, noderesps_fw]
    backwards = list(roles_bw) + [nodes_bw, rolealias_bw, noderesps_bw]

    batch_sql(connection, forwards)

    def finalizer():
        batch_sql(connection, backwards)

    request.addfinalizer(finalizer)


@pytest.fixture
def rollback_uniqueness(request):
    indices = list(generate_index_for_roles(concurrently=False))
    forwards, backwards = zip(*indices)
    batch_sql(connection, backwards)

    def finalizer():
        # Хак: приходится удалять все роли, так как этот финалайзер
        # вызывается до финалайзера transactional_db, который сделает truncate на таблицу
        Role.objects.all().delete()
        batch_sql(connection, forwards)

    request.addfinalizer(finalizer)


@pytest.fixture
def rollback_index_node_responsibility(request):
    add_index, drop_index = generate_indexes_for_node_responsibility()
    batch_sql(connection, [drop_index])

    def finalizer():
        NodeResponsibility.objects.all().delete()
        batch_sql(connection, [add_index])

    request.addfinalizer(finalizer)


@pytest.fixture
def rollback_index_role_alias(request):
    add_index, drop_index = generate_index_for_rolealias()
    batch_sql(connection, [drop_index])

    def finalizer():
        RoleAlias.objects.all().delete()
        batch_sql(connection, [add_index])

    request.addfinalizer(finalizer)


@pytest.fixture()
def client(request, settings):
    def finalizer():
        client.logout()
        translation.deactivate()

    client = IDMClient()

    def login(username: Union[User, str], lang: str = 'ru'):
        if isinstance(username, User):
            username = username.username
        settings.YAUTH_TEST_USER = {'login': username, 'language': lang}

    client.login = login
    request.addfinalizer(finalizer)
    return client


@pytest.fixture(autouse=True)
def fake_newldap(settings, monkeypatch):
    client = mock.MagicMock()

    def ldap_initialize(uri, *args, **kwargs):
        client.initialize(uri)
        return client

    monkeypatch.setattr('ldap.initialize', ldap_initialize)

    settings.AD_LDAP_HOST = 'ldap://fake.ldap.net'
    settings.CA_BUNDLE = 'fake_ca_bundle'
    settings.AD_LDAP_USERNAME = 'fake_user'
    settings.AD_LDAP_PASSWD = 'fake_password'
    return client


@pytest.fixture
def idm_robot(request):
    """Фикстура для тех тестов, где требуется робот"""
    return get_idm_robot()


@pytest.fixture
def pt1_system(settings):
    """
    Система с плагином idm.core.plugins.test1
    """
    system = create_system('test1', 'idm.tests.base.TestPlugin', name='Test1 система')

    return system


@pytest.fixture
def simple_system(request, settings, idm_robot):
    """
    Система с плагином с замоканными ответами
    """
    system = create_system(
        'simple',
        'idm.tests.base.SimplePlugin',
        audit_method='get_roles' if request.node.get_closest_marker('streaming_roles') else 'get_all_roles',
        name='Simple система',
        name_en='Simple system',
        emails='simple@yandex-team.ru,simplesystem@yandex-team.ru',
        use_tvm_role=False,
        base_url='http://simple-system.localhost',
    )
    return system


@pytest.fixture
def aware_simple_system(simple_system):
    """
    Простая система, следящая за составом групп самостоятельно
    """
    simple_system.group_policy = 'aware'
    simple_system.save()
    return simple_system


@pytest.fixture
def public_simple_system(simple_system):
    """
    Простая система, в которую запрашивать роли могут все
    """
    simple_system.request_policy = SYSTEM_REQUEST_POLICY.ANYONE
    simple_system.save()
    return simple_system


def _make_system_with_mock(system, group_policy):
    system.request_policy = SYSTEM_REQUEST_POLICY.ANYONE
    system.group_policy = group_policy
    system.plugin_type = 'idm.tests.system_mock.PluginWithMockSystem'
    system._plugin = None
    system.plugin.system_mock.reset()
    system.save(update_fields=['group_policy', 'plugin_type', 'request_policy'])
    set_workflow(system, group_code=DEFAULT_WORKFLOW)
    return system


@pytest.fixture
def system_aware_of_memberships(simple_system):
    """
    Простая система с политикой AWARE_OF_MEMBERSHIPS_WITHOUT_LOGINS, и моком для бекенда системы
    """
    return _make_system_with_mock(simple_system, SYSTEM_GROUP_POLICY.AWARE_OF_MEMBERSHIPS_WITHOUT_LOGINS)


@pytest.fixture
def system_with_logins(simple_system):
    """
    Простая система с политикой AWARE_OF_MEMBERSHIPS_WITH_LOGINS, и моком для бекенда системы
    """
    return _make_system_with_mock(simple_system, SYSTEM_GROUP_POLICY.AWARE_OF_MEMBERSHIPS_WITH_LOGINS)


@pytest.fixture
def sox_other_system(other_system):
    """
    Простая система c SOX
    """
    other_system.is_sox = True
    other_system.save()
    return other_system


@pytest.fixture
def complex_system(idm_robot):
    """
    Система с плагином с замоканными ответами, но с большим деревом ролей
    """
    system = create_system(
        'complex', 'idm.tests.base.ComplexPlugin', name='Complex система', name_en='Complex system', auth_factor='cert',
    )
    return system


@pytest.fixture
def dumb_system():
    """
    Система с dumb плагином
    """
    from idm.tests.base import SimplePlugin

    system = create_system('dumb', 'dumb', name='Dumb система', sync_role_tree=False)
    simple_plugin = SimplePlugin(system)
    set_roles_tree(system, simple_plugin.get_info())
    return system


@pytest.fixture
def self_system(settings, idm_robot):
    """
    Система, эквивалентная системе self, для запроса ролей ответственных за систему
    """
    system = create_system(
        settings.IDM_SYSTEM_SLUG,
        'idm.tests.base.SelfPlugin',
        name='IDM',
        name_en='IDM (en)',
        emails='selftest@yandex-team.ru',
        use_tvm_role=False,
        audit_method='get_roles'
    )
    set_workflow(system, code=DEFAULT_WORKFLOW, group_code=DEFAULT_WORKFLOW)
    return system


@pytest.fixture
def ad_system(request, settings):
    """
    AD-система
    """
    system = create_system(
        'ad_system',
        'idm.tests.base.ADSystemPlugin',
        name='AD',
        name_en='AD(en)',
        audit_method='get_roles' if request.node.get_closest_marker('streaming_roles') else 'get_all_roles',
        emails='simple@yandex-team.ru,simplesystem@yandex-team.ru',
    )
    set_workflow(system, code=DEFAULT_WORKFLOW, group_code=DEFAULT_WORKFLOW)
    return system


@pytest.fixture()
def superuser_node(simple_system):
    return simple_system.nodes.get(slug='superuser')


@pytest.fixture()
def complex_system_with_nodesets(complex_system):
    tree = complex_system.plugin.get_info()
    tree['roles']['values']['rules']['roles']['values']['developer'] = {
        'name': 'Разработчик',
        'set': 'developer_id'
    }
    tree['roles']['values']['rules']['roles']['values']['invisic']['set'] = 'invisic_id'
    tree['roles']['values']['subs']['roles']['values']['developer'] = (
        tree['roles']['values']['rules']['roles']['values']['developer']
    )
    tree['roles']['values']['subs']['roles']['values']['manager'] = {
        'name': 'Менеджер',
        'set': 'manager_id'
    }
    tree['roles']['values']['rules']['roles']['values']['manager'] = (
        tree['roles']['values']['subs']['roles']['values']['manager']
    )
    with mock_tree(complex_system, tree):
        sync_role_nodes(complex_system)

    assert RoleNodeSet.objects.count() == 3
    assert set(RoleNodeSet.objects.values_list('set_id', flat=True)) == {'developer_id', 'manager_id', 'invisic_id'}
    return complex_system


@pytest.fixture()
def complex_system_with_responsibilities(complex_system, arda_users):
    tree = complex_system.plugin.get_info()
    tree['roles']['values']['subs']['roles']['values']['manager']['responsibilities'] = [{
        'username': 'frodo',
        'notify': True,
    }, {
        'username': 'sam',
        'notify': False,
    }]
    with mock_tree(complex_system, tree):
        sync_role_nodes(complex_system)

    assert NodeResponsibility.objects.count() == 2
    return complex_system


@pytest.fixture
def other_system():
    """
    Система, аналогичная простой, но с другим слагом и другим классом плагина (плагин работает тоже идентично плагину
    системы simple)
    """
    system = create_system('other', 'idm.tests.base.YaPlugin', name='Другая система')
    return system


@pytest.fixture
def cauth(arda_users):
    serverinfo = {
        'name': 'Роль',
        'slug': 'role',
        'values': {
            'sudo': {
                'set': 'sudo',
                'name': 'Sudo',
                'fields': [{
                    'slug': 'role',
                    'name': 'role',
                    'required': True,
                    'type': 'choicefield',
                    'options': {
                        'custom': True,
                        'choices': [{
                            'name': 'ALL=(ALL) ALL',
                            'value': 'ALL=(ALL) ALL'
                        }, {
                            'name': 'ALL=(ALL) NOPASSWD: ALL',
                            'value': 'ALL=(ALL) NOPASSWD: ALL'
                        }]
                    },
                }],
            },
            'ssh': {
                'set': 'ssh',
                'name': 'Ssh',
                'fields': [{
                    'required': False,
                    'type': 'booleanfield',
                    'slug': 'root',
                    'name': 'root'
                }],
            }
        },
    }

    data = {
        'code': 0,
        'roles': {
            'slug': 'dst',
            'name': 'Назначение',
            'values': {
                # aliases + responsiblities
                'server1': {
                    'name': 'server1',
                    'roles': serverinfo,
                    'aliases': [{
                        'type': 'type',
                        'name': 'server'
                    }, {
                        'type': 'notify-email',
                        'name': 'vs-admin@yandex-team.ru'
                    }],
                    'responsibilities': [{
                        'username': 'legolas',
                        'notify': True,
                    }, {
                        'username': 'gandalf',
                        'notify': False,
                    }, {
                        'username': 'gimli',
                        'notify': True,
                    }]
                },
                # ни alias-ов, ни responsiblities
                'server2': {
                    'name': 'server2',
                    'roles': serverinfo,
                },
                # aliases, но нет responsibilities
                'server3': {
                    'name': 'server3',
                    'roles': serverinfo,
                    'aliases': [{
                        'type': 'notify-email',
                        'name': 'decline-too@yandex-team.ru'
                    }]
                },
                # responsibilities без aliases
                'server4': {
                    'name': 'server4',
                    'roles': serverinfo,
                    'responsibilities': [{
                        'username': 'legolas',
                        'notify': True,
                    }, {
                        'username': 'gandalf',
                        'notify': False,
                    }, {
                        'username': 'gimli',
                        'notify': True,
                    }]
                },
                # aliases+responsibilities, случай decline-email
                'server5': {
                    'name': 'server5',
                    'roles': serverinfo,
                    'responsibilities': [{
                        'username': 'legolas',
                        'notify': False,
                    }],
                    'aliases': [{
                        'type': 'notify-email',
                        'name': 'decline-too@yandex-team.ru'
                    }]
                },
                # comma-separated alias
                'server6': {
                    'name': 'server6',
                    'roles': serverinfo,
                    'responsibilities': [{
                        'username': 'varda',
                        'notify': True,
                    }],
                    'aliases': [{
                        'type': 'notify-email',
                        'name': '  good@valinor.middleearth,  evil@mordor.midleearth '
                    }]
                }
            }
        }
    }

    return create_system('cauth', name='CAuth', group_policy='aware', role_tree=data)


@pytest.fixture
def simple_system_w_refs(simple_system):
    workflow = dedent("""
        approvers = []
        if role.get('role') == 'admin':
            ref_roles = [{
                'system': '%s',
                'role_data': {
                    'role': 'manager'
                },
                'role_fields': {
                    'login': scope
                }
            }]
        """ % simple_system.slug)
    set_workflow(simple_system, workflow, workflow)
    return simple_system


@pytest.fixture
def complex_system_w_deps(complex_system):
    RoleField.objects.update(is_active=False)
    subs = RoleNode.objects.get(slug='subs')
    subs.fields.create(
        slug='ring_type',
        name_en='Ring type',
        name='Тип кольца',
        is_required=False,
        type='choicefield',
        options={
            'widget': 'radio',
            'choices': [
                {
                    'value': 'elvenkings',
                    'name': {
                        'en': 'For Elven-kings',
                        'ru': 'Для королей эльфов',
                    },
                },
                {
                    'value': 'dwarflords',
                    'name': {
                        'en': 'For Dwarf Lords',
                        'ru': 'Для королей гномов',
                    },
                },
                {
                    'value': 'mortalmen',
                    'name': {
                        'en': 'For mortal men',
                        'ru': 'Для людей',
                    },
                },
                {
                    'value': 'darklord',
                    'name': {
                        'en': 'For dark lord',
                        'ru': 'Для Саурона',
                    }
                },
            ]
        }
    )
    subs.fields.create(
        slug='qty',
        name_en='Quantity',
        name='Количество',
        is_required=True,
        type='integerfield',
        dependencies={
            'ring_type': {
                '$exists': True,
            }
        },
        options={
            'placeholder': 'Число же!',
            'default': 1,
            'unknown': True,
        },
    )
    subs.fields.create(
        slug='omnipotence',
        name_en='Omnipotence required',
        name='Требуется всемогущество',
        type='booleanfield',
        is_required=False,
        dependencies={
            '$or': [{
                'ring_type': 'elvenkings',
                'qty': 3
            }, {
                'ring_type': 'dwarflords',
                'qty': 7
            }, {
                'ring_type': 'mortalmen',
                'qty': 9
            }, {
                'ring_type': 'darklord',
                'qty': 1
            }]
        }
    )
    return complex_system


@pytest.fixture
def generic_system(settings):
    """
    Стандартная система с generic плагином
    """
    from idm.tests.base import SimplePlugin
    system = create_system('test', 'generic', name='Generic система', base_url='http://example.com/',
                           auth_factor='cert', sync_role_tree=False)
    simple_plugin = SimplePlugin(system)
    set_roles_tree(system, simple_plugin.get_info())

    return system


@pytest.fixture
def generic_connect_system(settings):
    """
    Стандартная система с generic плагином
    """
    from idm.tests.base import SimplePlugin
    system = create_system('test', 'generic_connect', name='Generic Connect система', base_url='http://example.com/',
                           sync_role_tree=False)
    simple_plugin = SimplePlugin(system)
    set_roles_tree(system, simple_plugin.get_info())

    return system


@pytest.fixture
def generic_new_system(settings):
    """
    Стандартная система с generic_new плагином
    """
    from idm.tests.base import SimplePlugin
    system = create_system('test', 'generic_new', name='Generic New система', base_url='http://example.com/',
                           group_policy='aware', audit_method=SYSTEM_AUDIT_METHOD.GET_ROLES, sync_role_tree=False)
    simple_plugin = SimplePlugin(system)
    set_roles_tree(system, simple_plugin.get_info())

    return system


@pytest.fixture
def generic_system_with_tvm(generic_system):
    generic_system.tvm_id = '123'
    generic_system.auth_factor = 'tvm'
    generic_system.save()
    return generic_system


@pytest.fixture
def aware_generic_system(generic_system):
    """
    Generic-система, следящая за составом групп самостоятельно
    """
    generic_system.group_policy = 'aware'
    generic_system.save()
    # удалим паспортный логин, так как в aware-системах их, кажется, нельзя иметь
    RoleField.objects.filter(node__system=generic_system, type='passportlogin').update(is_required=False)
    return generic_system


@pytest.fixture
def users_for_test():
    art = create_user('art', **{
        'first_name': 'Центурион',
        'last_name': 'Марк',
        'first_name_en': 'Centurio',
        'last_name_en': 'Mark',
        'position': 'centurio',
    })
    fantom = create_user('fantom', **{
        'first_name': 'Легионер',
        'last_name': 'Тит',
        'first_name_en': 'Legioner',
        'last_name_en': 'Tit'
    })
    terran = create_user('terran', **{
        'first_name': 'Легат',
        'last_name': 'Аврелий',
        'first_name_en': 'Legat',
        'last_name_en': 'Aurelius',
    })
    admin = create_user('admin', superuser=True)
    return art, fantom, terran, admin


@pytest.fixture
def idm(client):
    data = client.json.get(reverse('client-api:info'), with_idm_credentials=True).json()
    system = create_system('self', plugin_type='idm.tests.base.SelfPlugin', name='IDM')
    set_roles_tree(system, data)
    set_workflow(system, 'approvers=[]; no_email=True')
    return system


@pytest.fixture
def more_users_for_test(users_for_test):
    (art, fantom, terran, admin) = users_for_test

    zerg = create_user('zerg')
    protoss = create_user('protoss')

    return (art, fantom, terran, zerg, protoss, admin)


@pytest.fixture
def users_with_roles(users_for_test, pt1_system):
    (art, fantom, terran, admin) = users_for_test

    art.passport_logins.create(login='yndx.art.defaultlogin', state='created', is_fully_registered=True)
    fantom.passport_logins.create(login='yndx.fantom.defaultlogin', state='created', is_fully_registered=True)
    terran.passport_logins.create(login='yndx.terran.defaultlogin', state='created', is_fully_registered=True)
    admin.passport_logins.create(login='yndx.admin.defaultlogin', state='created', is_fully_registered=True)

    with mock.patch('idm.sync.passport.exists') as passport_exists:
        passport_exists.return_value = True
        role1 = make_role(
            art, pt1_system,
            data={'project': 'proj1', 'role': 'admin'},
            fields_data={'passport-login': 'yndx.art.defaultlogin'},
        )
        role1.fetch_node()
        assert role1.node.value_path == '/proj1/admin/'
        role2 = make_role(
            art, pt1_system,
            data={'project': 'proj2', 'role': 'wizard'},
            fields_data={'passport-login': 'yndx.art.defaultlogin'},
        )
        role2.fetch_node()
        assert role2.node.value_path == '/proj2/wizard/'
        role3 = make_role(
            fantom, pt1_system,
            data={'project': 'proj1', 'role': 'admin'},
            fields_data={'passport-login': 'yndx.fantom.defaultlogin'},
        )
        role3.fetch_node()
        assert role3.node.value_path == '/proj1/admin/'
        role4 = make_role(
            terran, pt1_system,
            data={'project': 'proj1', 'role': 'manager'},
            fields_data={'passport-login': 'yndx.terran.defaultlogin'}
        )
    role4.deprive_or_decline(terran)
    role4 = refresh(role4)
    assert role4.is_active is False
    assert role4.state == 'deprived'
    role4.fetch_node()
    assert role4.node.value_path == '/proj1/manager/'
    return users_for_test


@pytest.fixture
def arda_users_with_roles(arda_users, simple_system, department_structure):
    frodo = arda_users.get('frodo')
    legolas = arda_users.get('legolas')
    gandalf = arda_users.get('gandalf')
    fellowship = Group.objects.get(slug='fellowship-of-the-ring')
    set_workflow(simple_system, group_code='approvers = []')
    fellowship_role = Role.objects.request_role(frodo, fellowship, simple_system, '', {'role': 'poweruser'}, None)

    frodo1 = raw_make_role(frodo, simple_system, {'role': 'admin'}, state='granted')
    frodo2 = raw_make_role(frodo, simple_system, {'role': 'manager'}, state='granted')
    legolas1 = raw_make_role(legolas, simple_system, {'role': 'superuser'}, state='granted')
    gandalf1 = raw_make_role(gandalf, simple_system, {'role': 'superuser'}, state='deprived')

    roles = attrdict({
        'fellowship': [fellowship_role],
        'frodo': [frodo1, frodo2],
        'legolas': [legolas1],
        'gandalf': [gandalf1]
    })

    return roles


@pytest.fixture
def depriver_users(simple_system, pt1_system):
    superuser = create_user('superuser')
    add_perms_by_role('superuser', superuser)

    helpdesk = create_user('helpdesk')
    add_perms_by_role('helpdesk', helpdesk)

    security = create_user('security')
    add_perms_by_role('security', security)

    responsible_our = create_user('responsible_our')
    add_perms_by_role('responsible', responsible_our, simple_system)

    responsible_other = create_user('responsible_other')
    add_perms_by_role('responsible', responsible_other, pt1_system)

    team_member = create_user('team_member')
    add_perms_by_role('users_view', team_member, simple_system)

    passer_by = create_user('passer-by')

    management.call_command('idm_sync_permissions')

    deprivers = [
        (superuser, True),
        (helpdesk, True),
        (security, True),
        (responsible_our, True),
        (responsible_other, False),
        (team_member, True),
        (passer_by, False)
    ]
    return deprivers


@pytest.fixture
def group_roots():
    return ensure_group_roots()


@pytest.fixture(autouse=True)
def use_robot_if_not_marker(request):
    django_db_marker = request.node.get_closest_marker('django_db')
    if django_db_marker and not request.node.get_closest_marker('robotless'):
        get_idm_robot()


@pytest.fixture
def flat_arda_users():
    """Фикстура для случаев, когда нужны пользователи, но не нужна их структура и группы"""
    usernames = ('frodo', 'bilbo', 'sam', 'meriadoc', 'peregrin',
                 'aragorn', 'legolas', 'gandalf', 'gimli', 'boromir',
                 'sauron', 'saruman', 'nazgul', 'witch-king-of-angmar',
                 'galadriel', 'varda', 'manve')
    full_names = {
        'frodo': ('Фродо', 'Бэггинс'),
    }
    full_names_en = {
        'frodo': ('Frodo', 'Baggins'),
    }
    users = attrdict()
    for login in usernames:
        kwargs = {'department_group': None}
        names = full_names.get(login)
        names_en = full_names_en.get(login)
        if names:
            kwargs['first_name'], kwargs['last_name'] = names
        if names_en:
            kwargs['first_name_en'], kwargs['last_name_en'] = names_en
        users[login] = create_user(login, **kwargs)
    users['tvm_app'] = create_user('1234567890', type=USER_TYPES.TVM_APP, department_group=None)
    return users


@pytest.fixture
def arda_users(department_structure, flat_arda_users):
    """Фикстура для случаев, когда нужны пользователи и их структура, то есть группы и членства"""
    return flat_arda_users


@pytest.fixture
def robot_gollum():
    return create_user('gollum', is_robot=True)


@pytest.fixture()
def responsible_gandalf(arda_users, simple_system):
    gandalf = arda_users.gandalf
    add_perms_by_role('responsible', gandalf, simple_system)
    return gandalf


@pytest.fixture()
def superuser_gandalf(arda_users):
    gandalf = arda_users.gandalf
    add_perms_by_role('superuser', gandalf)
    return gandalf


@pytest.fixture
def department_structure(group_roots):
    root_dep, root_service, root_wiki = group_roots
    structure = [
        {
            'slug': 'middle-earth',
            'external_id': 101,
            'name': {
                'ru': 'Средиземье',
                'en': 'Middle Earth',
            },
            'children': [
                {
                    'slug': 'lands',
                    'name': {
                        'ru': 'Земли',
                        'en': 'Lands',
                    },
                    'external_id': 102,
                    'children': [
                        {
                            'slug': 'the-shire',
                            'name': {
                                'ru': 'Шир',
                                'en': 'The Shire',
                            },
                            'external_id': 104,
                            'members': ['frodo', 'bilbo', 'sam', 'meriadoc', 'peregrin'],
                            'responsible': [
                                ('frodo', 'head'), ('bilbo', 'head')
                            ]
                        },
                    ],
                },
                {
                    'slug': 'associations',
                    'name': {
                        'ru': 'Объединения',
                        'en': 'Associations',
                    },
                    'external_id': 103,
                    'members': ['sauron'],
                    'responsible': [('varda', 'head'), ('frodo', 'deputy')],
                    'children': [
                        {
                            'slug': 'fellowship-of-the-ring',
                            'external_id': 105,
                            'name': {
                                'ru': 'Братство кольца',
                                'en': 'Fellowship of the Ring'
                            },
                            'members': ['frodo', 'sam', 'meriadoc', 'peregrin', 'gandalf', 'aragorn', 'legolas',
                                        'gimli', 'boromir'],
                            'responsible': [
                                ('frodo', 'head'), ('sam', 'deputy'), ('gandalf', 'deputy'), ('galadriel', 'deputy')
                            ]
                        }
                    ]
                }
            ],
        },
        {
            'slug': 'valinor',
            'name': {
                'ru': 'Валинор',
                'en': 'Valinor',
            },
            'external_id': 999,
            'members': ['varda', 'manve', 'frodo', 'legolas'],
            'responsible': [
                ('varda', 'head'), ('manve', 'deputy')
            ]
        }
    ]
    create_user('witch-king-of-angmar')
    create_group_structure(structure, root_dep)
    return attrdict({
        'fellowship': Group.objects.get(slug='fellowship-of-the-ring'),
        'shire': Group.objects.get(slug='the-shire'),
        'associations': Group.objects.get(slug='associations'),
        'valinor': Group.objects.get(slug='valinor'),
        'earth': Group.objects.get(slug='middle-earth'),
        'lands': Group.objects.get(slug='lands')
    })


def _create_service_groups(yaml_data, users: List['User'] = None, mock_fetcher=False, generate_passport=False):
    """
    создать сервисные группы,
    :param users:, добавить пользователей в группы
    :param mock_fetcher: - мок стаффа будет отдавать заданную структуру
    :param generate_passport: созданным членствам будут сгенерированы паспрортные логины
    """

    def _mock_fetcher(root_service_group: Group, fetcher_data: Dict[str, list]):
        fetcher = root_service_group.fetcher
        fetcher.set_data(('abc', 'services'), [])
        fetcher.set_data(('abc', 'service_members'), [])
        fetcher.set_data(('staff', 'group'), fetcher_data['service'], for_lookup=('type', 'service'))
        fetcher.set_data(('staff', 'group'), fetcher_data['scope'], for_lookup=('type', 'servicerole'))
        fetcher.set_data(('staff', 'groupmembership'), fetcher_data['service_mem'],
                         for_lookup=('group.type', 'service'))
        fetcher.set_data(('staff', 'groupmembership'), fetcher_data['scope_mem'],
                         for_lookup=('group.type', 'servicerole'))

    users_dict = {u.username: u for u in users or []}
    passport_dict = {}
    if generate_passport:
        for username, user in users_dict.items():
            passport_dict[username] = UserPassportLogin.objects.create(login=user.get_passport_login(), user=user)
    counter = iter(count(0))
    fetcher_data = {
        'service': [],
        'scope': [],
        'service_mem': [],
        'scope_mem': [],
    }
    if mock_fetcher:
        for username, user in users_dict.items():
            staff_id = next(counter)
            user.staff_id = staff_id
            user.center_id = staff_id
            user.save(update_fields=['staff_id', 'center_id'])

    def create_services(service_data, parent_service=None, root_group=None):
        all_items = {}

        if root_group is None:
            root_group = Group.objects.get(type=GROUP_TYPES.SERVICE, parent=None)
            all_items['svc_root'] = root_group

        slug = service_data['slug']
        service = Service.objects.create(parent=parent_service, slug=slug, name=slug, external_id=next(counter))
        group = Group.objects.create(type=GROUP_TYPES.SERVICE, slug=f'svc_{slug}', parent=root_group,
                                     name=f'svc_{slug}', external_id=next(counter), description=f'{slug} description')
        if users:
            members = service_data.get('members') or []
            add_members(group, [users_dict[username] for username in members], add_to_service=True)
            if generate_passport:
                for username in members:
                    passport = passport_dict[username]
                    group.memberships.filter(user__username=username).update(passport_login=passport)
        if mock_fetcher:
            fetcher_data['service'].append({
                'id': group.external_id,
                'url': group.slug,
                'description': group.description,
                'service': {'id': service.external_id},
                'name': group.name,
            })
            if users:
                for username in service_data.get('members') or []:
                    fetcher_data['service_mem'].append({
                        'person': {"id": users_dict[username].staff_id, "official": {"is_dismissed": False}},
                        'group': {"id": group.external_id},
                        'id': next(counter)
                    })
        for scope_data in service_data.get('scopes') or []:
            if isinstance(scope_data, str):
                scope_name = scope_data
                scope_members = []
            elif isinstance(scope_data, dict):
                scope_name = scope_data['name']
                scope_members = scope_data.get('members') or []
            else:
                raise ValueError(scope_data)
            scope_slug = f'svc_{slug}_{scope_name}'
            scope = Group.objects.create(type=GROUP_TYPES.SERVICE, slug=scope_slug,
                                         parent=group, name=scope_slug, external_id=next(counter),
                                         description=f'{scope_slug} description')
            all_items[scope_slug] = scope
            if users_dict:
                members = scope_data.get('members') or []
                add_members(scope, [users_dict[username] for username in members], add_to_service=False)
                if generate_passport:
                    for username in members:
                        passport = passport_dict[username]
                        scope.memberships.filter(user__username=username).update(passport_login=passport)
            if mock_fetcher:
                fetcher_data['scope'].append({
                    'parent': {'id': group.external_id, 'service': {'id': service.external_id}},
                    'url': scope.slug,
                    'description': scope.description,
                    'service': {'id': None},
                    'id': scope.external_id,
                    'role_scope': 'role scope',
                    'name': scope.name
                })
                if users_dict:
                    for username in scope_members:
                        fetcher_data['scope_mem'].append({
                            'person': {'id': users_dict[username].staff_id, 'official': {'is_dismissed': False}},
                            'group': {'id': scope.external_id},
                            'id': next(counter),
                        })
        for child in service_data.get('children') or []:
            all_items.update(create_services(child, service, root_group))
        all_items[slug] = service
        all_items[f'svc_{slug}'] = group
        return all_items

    all_items = create_services(yaml.safe_load(yaml_data))
    if mock_fetcher:
        _mock_fetcher(all_items['svc_root'], fetcher_data)
    sync_indirect_memberships('service')
    return all_items


@pytest.fixture
def simple_service_groups():
    return _create_service_groups("""
slug: group
scopes:
  - scope
    """)


@pytest.fixture
def groups_with_roles(simple_system, arda_users, department_structure):
    frodo = arda_users['frodo']
    add_perms_by_role('responsible', frodo, simple_system)
    set_workflow(simple_system, group_code=DEFAULT_WORKFLOW)

    valinor = Group.objects.get(slug='valinor')
    associations = Group.objects.get(slug='associations')
    fellowship = Group.objects.get(slug='fellowship-of-the-ring')

    # выдадим общую групповую роль на middle_earth
    valinor_role = Role.objects.request_role(frodo, valinor, simple_system, '', {'role': 'manager'}, {})

    # выдадим более частную групповую роль на associations
    associations_role = Role.objects.request_role(frodo, associations, simple_system, '',
                                                  {'role': 'poweruser'}, {})

    # выдадим совсем частную роль на fellowship
    fellowship_roles = [Role.objects.request_role(frodo, fellowship, simple_system, '', {'role': 'manager'}, {}),
                        Role.objects.request_role(frodo, fellowship, simple_system, '', {'role': 'superuser'}, {})]

    roles = {
        'valinor': [valinor_role],
        'associations': [associations_role],
        'fellowship': fellowship_roles,
    }
    return roles


@pytest.fixture
def intrasearch_objects():
    return [
        {
            'fields': [
                {
                    'type': 'slug_path',
                    'value': '/project/proj1/'
                }, {
                    'type': 'help',
                    'value': 'Проект для связи с общественностью'
                }, {
                    'type': 'aliases',
                    'value': [{
                        'name': 'test1-fire-proj1',
                        'type': 'firewall'
                    }]
                }
            ],
            'id': '/test1/project/proj1/',
            'layer': 'idm_rolenodes',
            'title': 'Проект 1',
            'url': ''
        }, {
            'fields': [
                {
                    'type': 'slug_path',
                    'value': '/project/proj2/'
                }
            ],
            'id': '/test1/project/proj2/',
            'layer': 'idm_rolenodes',
            'title': 'Проект 2',
            'url': ''
        }, {
            'fields': [
                {
                    'type': 'slug_path',
                    'value': '/project/proj3/'
                }
            ],
            'id': '/test1/project/proj3/',
            'layer': 'idm_rolenodes',
            'title': 'Проект 3',
            'url': ''
        }
    ]


@pytest.fixture(scope='session')
def patch_tvm(session_monkeypatch):
    class FakeTvm(object):
        def __init__(self):
            pass

    session_monkeypatch.setattr('tvm2.TVM', FakeTvm)


@pytest.fixture
def patch_get_systems_in_sync_celery_queue(monkeypatch):
    monkeypatch.setattr(
        'idm.core.querysets.system.SystemQuerySet.get_systems_in_sync_celery_queue',
        lambda x: set(),
    )


@pytest.fixture
def fellowship(arda_users, department_structure):
    return department_structure.fellowship


@pytest.fixture
def default_workflow(simple_system):
    set_workflow(simple_system, group_code=DEFAULT_WORKFLOW)


@pytest.fixture
def mock_fetcher():
    Group.fetcher = LocalFetcher()
    Group.hasher = Hasher(debug=False)


@pytest.fixture
def metrika_system():
    return create_system(
        'metrika',
        name='Метрика',
        public=True,
        role_tree={
            'code': 0,
            'roles': {
                'slug': 'role',
                'name': 'Роль',
                'values': {
                    'accessor': {
                        'slug': 'accessor',
                        'name': 'Ассессор',
                        'fields': [
                            {
                                'slug': 'counter_id',
                                'name': 'Номер счетчика',
                                'type': 'suggestfield',
                                'required': True,
                                'options': {'suggest': 'metrika_counter'},
                            },
                        ],
                    }
                },
            },

        }
    )

@pytest.fixture
def app_metrica_system():
    return create_system(
        'appmetrica',
        name='AppMetrica',
        public=True,
        role_tree={
            'code': 0,
            'roles': {
                'slug': 'role',
                'name': 'Роль',
                'values': {
                    'accessor': {
                        'slug': 'accessor',
                        'name': 'Ассессор',
                        'fields': [
                            {
                                'slug': 'application_id',
                                'name': 'ID приложения',
                                'type': 'suggestfield',
                                'required': True,
                                'options': {'suggest': 'app_metrica'},
                            },
                        ],
                    }
                },
            },

        }
    )

@pytest.fixture
def mongo_mock() -> _MockedMongo:
    fake_mongo = _MockedMongo()
    with mock.patch('idm.utils.mongo.get_mongo_db', return_value=fake_mongo) as _mock:
        yield fake_mongo
