# coding: utf-8


from contextlib import contextmanager
import mock
import pytest
from django.core.cache import cache
from ids.exceptions import BackendError

from idm.core.models import RoleNode
from idm.tests.utils import mock_ids_repo, use_proxied_suggest, mock_tree, set_roles_tree, sync_role_nodes, create_user
from idm.utils import reverse

pytestmark = pytest.mark.django_db


@contextmanager
def mock_sync_result(stage):
    assert stage in ['success', 'fail']
    if stage == 'success':
        yield
    elif stage == 'fail':
        with mock.patch('idm.core.models.RoleNode.get_queue') as get_queue:
            get_queue.side_effect = Exception
            yield


def test_suggest_duplicate_nodes(client, arda_users, pt1_system):
    suggest_url = reverse('api_dispatch_list', api_name='frontend', resource_name='suggest/roles/all')
    frodo = arda_users.frodo
    client.login(frodo)
    tree = pt1_system.plugin.get_info()
    tree['roles']['values']['somenewrole'] = 'Проект 1'
    with mock_tree(pt1_system, tree):
        sync_role_nodes(pt1_system, requester=frodo)
    with use_proxied_suggest(should_use=False):
        with mock.patch('idm.api.frontend.suggest.rolenode.logger.warning') as mocked_logger:
            response = client.json.get(suggest_url, {'system': pt1_system.slug, 'scope': '/'})
            log_args = mocked_logger.call_args[0]
            log_text = log_args[0] % log_args[1:]
            assert log_text.startswith(
                'Duplicate node names found in suggest. Resource: "LocalRoleNodeSuggestResource"'
            )
            assert 'system: "test1", path: "/", name:' in log_text


def test_role_suggest(client, arda_users, pt1_system, intrasearch_objects):
    suggest_url = reverse('api_dispatch_list', api_name='frontend', resource_name='suggest/roles/all')

    client.login('frodo')

    with use_proxied_suggest(should_use=False):
        response = client.json.get(suggest_url, {'system': pt1_system.slug, 'scope': '/'})
        assert response.status_code == 200
        assert response.json() == {
            'meta': {'offset': 0, 'limit': 20},
            'data': {
                'header': {'slug': 'project', 'name': 'Проект', 'path': '/'},
                'roles': [
                    {'slug': 'proj1', 'id': 'proj1', 'name': 'Проект 1', 'help': 'Проект для связи с общественностью'},
                    {'slug': 'proj2', 'id': 'proj2', 'name': 'Проект 2'},
                    {'slug': 'proj3', 'id': 'proj3', 'name': 'Проект 3'},
                ]
            }
        }

        response = client.json.get(suggest_url, {'system': pt1_system.slug, 'scope': '/proj1/'})
        assert response.json() == {
            'meta': {'offset': 0, 'limit': 20},
            'data': {
                'header': {'slug': 'role', 'name': 'Роль', 'path': '/proj1/'},
                'roles': [
                    {'slug': 'admin', 'id': 'admin', 'name': 'Админ'},
                    {
                        'slug': 'manager', 'id': 'manager', 'name': 'Менеджер',
                        'help': 'Самый главный менеджер для тестов'
                    },
                    {'slug': 'doc', 'id': 'doc', 'name': 'Тех писатель'},
                ]
            }
        }

        # visibility
        response = client.json.get(suggest_url, {'system': pt1_system.slug, 'scope': '/proj2/'})
        assert response.json() == {
            'meta': {'offset': 0, 'limit': 20},
            'data': {
                'header': {'slug': 'role', 'name': 'Роль', 'path': '/proj2/'},
                'roles': [
                    {'slug': 'wizard', 'id': 'wizard', 'name': 'Кудесник'},
                ]
            }
        }

        response = client.json.get(suggest_url, {'system': pt1_system.slug, 'scope': '/proj1/', 'limit': 1})
        assert response.json() == {
            'meta': {'offset': 0, 'limit': 1},
            'data': {
                'header': {'slug': 'role', 'name': 'Роль', 'path': '/proj1/'},
                'roles': [
                    {'slug': 'admin', 'id': 'admin', 'name': 'Админ'},
                ]
            }
        }

        response = client.json.get(suggest_url, {'system': pt1_system.slug, 'scope': '/proj1/', 'offset': 1})
        assert response.json() == {
            'meta': {'offset': 1, 'limit': 20},
            'data': {
                'header': {'slug': 'role', 'name': 'Роль', 'path': '/proj1/'},
                'roles': [
                    {
                        'slug': 'manager', 'id': 'manager', 'name': 'Менеджер',
                        'help': 'Самый главный менеджер для тестов',
                    },
                    {'slug': 'doc', 'id': 'doc', 'name': 'Тех писатель'},
                ]
            }
        }

        response = client.json.get(suggest_url, {'system': pt1_system.slug, 'scope': '/proj1/', 'offset': 1,
                                   'limit': 1})
        assert response.json() == {
            'meta': {'offset': 1, 'limit': 1},
            'data': {
                'header': {'slug': 'role', 'name': 'Роль', 'path': '/proj1/'},
                'roles': [{
                    'slug': 'manager', 'id': 'manager', 'name': 'Менеджер', 'help': 'Самый главный менеджер для тестов',
                }]
            }
        }

        response = client.json.get(suggest_url, {'system': pt1_system.slug, 'scope': '/proj1/', 'q': 'man'})
        assert response.json() == {
            'meta': {'offset': 0, 'limit': 20},
            'data': {
                'header': {'slug': 'role', 'name': 'Роль', 'path': '/proj1/'},
                'roles': [{
                    'slug': 'manager', 'id': 'manager', 'name': 'Менеджер', 'help': 'Самый главный менеджер для тестов',
                }]
            }
        }

        response = client.json.get(suggest_url, {'system': pt1_system.slug, 'scope': '/proj3/subproj1/'})
        assert response.json() == {
            'meta': {'offset': 0, 'limit': 20},
            'data': {
                'header': {'slug': 'role', 'name': 'Роль', 'path': '/proj3/subproj1/'},
                'roles': [
                    {'slug': 'admin', 'id': 'admin', 'name': 'Админ'},
                    {'slug': 'manager', 'id': 'manager', 'name': 'Менеджер'},
                    {'slug': 'developer', 'id': 'developer', 'name': 'Разработчик'},
                ]
            }
        }

        response = client.json.get(suggest_url, {'system': pt1_system.slug, 'scope': '/proj1/admin/'})
        assert response.json() == {
            'meta': {'offset': 0, 'limit': 20},
            'data': {'header': None, 'roles': []}
        }

    with use_proxied_suggest(should_use=True):
        with mock_ids_repo('intrasearch', 'idm_rolenodes', intrasearch_objects) as repo:
            # конвертация формата
            response = client.json.get(suggest_url, {'system': pt1_system.slug})
            assert response.json() == {
                'meta': {'offset': 0, 'limit': 20},
                'data': {
                    'header': {'slug': 'project', 'name': 'Проект', 'path': '/'},
                    'roles': [
                        {
                            'slug': 'proj1',
                            'id': 'proj1',
                            'name': 'Проект 1',
                            'help': 'Проект для связи с общественностью',
                        },
                        {'slug': 'proj2', 'id': 'proj2', 'name': 'Проект 2'},
                        {'slug': 'proj3', 'id': 'proj3', 'name': 'Проект 3'},
                    ]
                }
            }
            repo.get.reset_mock()

            # конвертация limit+offset в page+per_page
            response = client.json.get(suggest_url, {
                'system': pt1_system.slug,
                'limit': 25,
                'offset': 50,
                'q': 'hello',  # чтобы вызвать проксированный ресурс
            })
            repo.get.assert_called_once_with({
                'allow_empty': True,
                'idm_rolenodes.page': 2,
                'idm_rolenodes.per_page': 25,
                'idm_rolenodes.query': 's_parent_path:"/test1/"',
                'language': 'ru',
                'layers': 'idm_rolenodes',
                'text': 'hello'
            })
            repo.get.reset_mock()

            response = client.json.get(suggest_url, {
                'system': pt1_system.slug,
                'limit': 7,
                'offset': 13,
                'q': 'hello',
            })
            repo.get.assert_called_once_with({
                'allow_empty': True,
                'idm_rolenodes.page': 1,
                'idm_rolenodes.per_page': 10,
                'idm_rolenodes.query': 's_parent_path:"/test1/"',
                'language': 'ru',
                'layers': 'idm_rolenodes',
                'text': 'hello'
            })
            repo.get.reset_mock()

            # передача slug_path
            response = client.json.get(suggest_url, {
                'system': pt1_system.slug,
                'scope': '/proj1/',
                'q': 'hello',
            })
            repo.get.assert_called_once_with({
                'allow_empty': True,
                'idm_rolenodes.page': 0,
                'idm_rolenodes.per_page': 20,
                'idm_rolenodes.query': 's_parent_path:"/test1/project/proj1/"',
                'language': 'ru',
                'layers': 'idm_rolenodes',
                'text': 'hello'
            })
            repo.get.reset_mock()

            # передача id
            response = client.json.get(suggest_url, {
                'system': pt1_system.slug,
                'id': 'proj1'
            })
            assert repo.get.call_args_list == []

            response = client.json.get(suggest_url, {'system': pt1_system.slug, 'scope': '/proj1/help/'})
            assert response.status_code == 400
            assert response.json() == {
                'error_code': 'BAD_REQUEST',
                'message': 'scope "/proj1/help/" не найден в дереве ролей',
                'errors': {
                    '__all__': ['scope "/proj1/help/" не найден в дереве ролей'],
                }
            }

            response = client.json.get(suggest_url, {'system': 'notexists', 'user': 'aaa'})
            assert response.status_code == 400
            assert response.json() == {
                'error_code': 'BAD_REQUEST',
                'message': 'Invalid data sent',
                'errors': {
                    'system': ['Система c slug=notexists не найдена'],
                }
            }

            response = client.json.get(suggest_url, {'system': pt1_system.slug, 'offset': 'a'})
            assert response.status_code == 400
            assert response.json() == {
                'error_code': 'BAD_REQUEST',
                'message': 'Invalid data sent',
                'errors': {
                    'offset': ['Ожидается число']
                }
            }

            response = client.json.post(suggest_url, {'system': pt1_system.slug})
            assert response.status_code == 405

            response = client.json.put(suggest_url, {'system': pt1_system.slug})
            assert response.status_code == 405

        with mock_ids_repo('intrasearch', 'idm_rolenodes', [],
                           BackendError('Spooky error (just a timeout, don\'t worry)')) as repo:
            response = client.json.get(suggest_url, {'system': pt1_system.slug, 'q': 'hello'})
            assert response.status_code == 500
            assert response.json() == {
                'message': "IDS: Spooky error (just a timeout, don't worry)",
                'error_code': 'INTERNAL_API_ERROR'
            }


@pytest.mark.parametrize('mock_cache', [True, False])
@pytest.mark.parametrize('sync_stage', ['fail', 'success'])
def test_cached_suggest(client, arda_users, pt1_system, intrasearch_objects, mock_cache, sync_stage):
    suggest_url = reverse('api_dispatch_list', api_name='frontend', resource_name='suggest/roles/all')

    client.login('frodo')
    assert cache.get(pt1_system.suggest_cache_key) is None

    with use_proxied_suggest(should_use=True):
        proj1 = RoleNode.objects.get(slug='proj1')
        if mock_cache:
            cache.set(pt1_system.suggest_cache_key, [{
                'id': proj1.slug, 'slug': proj1.slug, 'name': proj1.name, 'help': proj1.get_description()
            }])
        response = client.json.get(suggest_url, {'system': pt1_system.slug, 'scope': '/'})
        assert response.status_code == 200
        data = response.json()
        assert data['meta'] == {'offset': 0, 'limit': 20}

        usual = {
            'header': {'slug': 'project', 'name': 'Проект', 'path': '/'},
            'roles': [
                {'slug': 'proj1', 'id': 'proj1', 'name': 'Проект 1', 'help': 'Проект для связи с общественностью'},
                {'slug': 'proj2', 'id': 'proj2', 'name': 'Проект 2'},
                {'slug': 'proj3', 'id': 'proj3', 'name': 'Проект 3'},
            ]
        }
        if not mock_cache:
            expected = usual
        else:
            expected = {
                'header': {'slug': 'project', 'name': 'Проект', 'path': '/'},
                'roles': [
                    {'slug': 'proj1', 'id': 'proj1', 'name': 'Проект 1', 'help': 'Проект для связи с общественностью'},
                ]
            }
        assert data['data'] == expected

    tree = pt1_system.plugin.get_info()
    # поменяем что-то в дереве
    tree['roles']['values']['proj1']['responsibilities'] = [{
        'username': 'frodo',
        'notify': True,
    }]
    with mock_tree(pt1_system, tree):
        assert cache.get(pt1_system.suggest_cache_key)
        with mock_sync_result(sync_stage):
            set_roles_tree(pt1_system, tree)
        assert not cache.get(pt1_system.suggest_cache_key)  # любой сколь-либо далеко прошедший синк чистит кеш
        response = client.json.get(suggest_url, {'system': pt1_system.slug, 'scope': '/'})
    data = response.json()
    # в обоих случаях кеш сброшен
    assert data['data'] == usual


def test_suggest_is_not_cached_on_id_request(client, arda_users, pt1_system, intrasearch_objects):
    """Проверим, что запросы с ?id= не складываются в кеш саджеста"""

    suggest_url = reverse('api_dispatch_list', api_name='frontend', resource_name='suggest/roles/all')

    client.login('frodo')
    assert cache.get(pt1_system.suggest_cache_key) is None

    with use_proxied_suggest(should_use=True):
        response = client.json.get(suggest_url, {'system': pt1_system.slug, 'scope': '/', 'id': 'proj3'})
        assert response.status_code == 200
        data = response.json()
        assert data['meta'] == {'offset': 0, 'limit': 20}

        expected = {
            'header': {'slug': 'project', 'name': 'Проект', 'path': '/'},
            'roles': [
                {'slug': 'proj3', 'id': 'proj3', 'name': 'Проект 3'},
            ]
        }

        assert data['data'] == expected
    assert cache.get(pt1_system.suggest_cache_key) is None


def test_cache_works_with_empty_query(client, simple_system):
    suggest_url = reverse('api_dispatch_list', api_name='frontend', resource_name='suggest/roles/all')
    client.login(create_user())
    client.json.get(suggest_url, data={'system': simple_system.slug, 'scope': '/',
                                       'offset': 0, 'limit': 20, 'q': '', 'id__in': ''})
    data = client.json.get(suggest_url, data={'system': simple_system.slug, 'scope': '/',
                                              'offset': 0, 'limit': 20, 'q': '', 'id__in': 'admin'}).json()
    assert len(data['data']['roles']) == 1
    assert data['data']['roles'][0]['slug'] == 'admin'


def test_suggest_id__in_node_uses_intrasearch(client, complex_system):
    objects = [
        {'title': 'Разработчик',
         'url': '',
         'id': '/complex-system/subs/developer/',
         'fields': [{
             'type': 'slug',
             'value': 'developer'
         }],
         'layer': 'idm_rolenodes',
         'click_urls': []}]
    with \
            use_proxied_suggest(should_use=True), \
            mock_ids_repo('intrasearch', 'idm_rolenodes', objects) as repo:
        suggest_url = reverse('api_dispatch_list', api_name='frontend', resource_name='suggest/roles/all')
        client.login(create_user())
        repo.get.reset_mock()
        data = client.json.get(suggest_url,
                               data={'offset': 0, 'limit': 10, 'system': complex_system.slug, 'scope': '/subs/',
                                     'id__in': 'Разработчик', 'q': ''}).json()
        assert len(data['data']['roles']) == 1
        assert data['data']['roles'][0]['slug'] == 'developer'
        repo.get.assert_called_once_with({
            'layers': 'idm_rolenodes',
            'allow_empty': True,
            'idm_rolenodes.page': 0,
            'idm_rolenodes.per_page': 10,
            'language': 'ru',
            'text': 'Разработчик',
            'idm_rolenodes.query': 's_parent_path:"/complex/project/subs/"'
        })
