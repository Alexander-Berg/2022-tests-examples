import pytest
from unittest.mock import patch
from xml.etree.ElementTree import fromstring

from intranet.hrdb_ext.src.ispring.api.connector import HttpConnector
from intranet.hrdb_ext.src.ispring.mock.connector import MockedConnector
from intranet.hrdb_ext.src.ispring.api.users import UserRepository, ISpringUser
from intranet.hrdb_ext.src.ispring.mock.response import Code


@pytest.mark.parametrize('resource,url_vars,result', [
    ('department', {}, Exception),
    ('department', {'department_id': '123'}, '/department/123'),
    ('department', {'department_id': '123', 'q': 1}, '/department/123?q=1')
])
def test_url_build(resource, url_vars, result):
    connector = HttpConnector()

    if isinstance(result, str):
        built = connector.build_url(resource, url_vars)
        built = built.replace(connector.host, '')
        assert result == built
    else:
        with pytest.raises(Exception):
            connector.build_url(resource, url_vars)


@patch.object(UserRepository, 'connector_cls', MockedConnector)
def test_user_get_user():
    repo = UserRepository()
    assert isinstance(repo.connector, MockedConnector)

    with pytest.raises(Exception):
        repo.get_one({})

    with pytest.raises(Exception):
        repo.get_one({'user_id': '123'})

    target = next(iter(repo.connector.users.by_id.values()))
    user = repo.get_one({'user_id': target.user_id})
    assert repo.connector.last_request['method'] == 'get'
    assert repo.connector.last_request['resource'] == 'user'
    assert repo.connector.last_request['url_vars'] == {'user_id': target.user_id}

    assert isinstance(user, ISpringUser)
    assert user.user_id == target.user_id
    assert user.login == target.login
    assert user.first_name == target.first_name
    assert user.last_name == target.last_name
    assert user.department_id == target.department_id


@patch.object(UserRepository, 'connector_cls', MockedConnector)
def test_user_get_users():
    repo = UserRepository()
    assert isinstance(repo.connector, MockedConnector)
    users_count = len(repo.connector.users.by_id)
    assert users_count == 5

    for page_size in range(1, users_count + 10):
        repo.connector.reset()

        users = list(repo.get_iter({'pageSize': page_size}))
        assert len(users) == users_count

        requests_count = (users_count + page_size - 1) // page_size
        assert len(repo.connector.history) == requests_count

        for i, request in enumerate(repo.connector.history):
            assert request['method'] == 'get'
            assert request['resource'] == 'users_iter'
            if i == 0:
                assert sorted(request['url_vars'].keys()) == ['pageSize']
            else:
                assert sorted(request['url_vars'].keys()) == ['pageSize', 'pageToken']

    repo.connector.reset()
    users = repo.get_all({'pageSize': 1})
    assert len(users) == users_count

    assert len(repo.connector.history) == 1
    assert repo.connector.last_request['method'] == 'get'
    assert repo.connector.last_request['resource'] == 'users_all'
    assert repo.connector.last_request['url_vars'] == {'pageSize': 1000000000}


@patch.object(UserRepository, 'connector_cls', MockedConnector)
def test_user_create():
    repo = UserRepository(users_count=0)
    assert isinstance(repo.connector, MockedConnector)
    assert len(repo.connector.users.by_id) == 0

    data = {
        'login': '1',
        'first_name': '2',
        'last_name': '3',
        'department_id': '4',
    }
    response = repo.create({}, data)

    assert response.status_code == Code.OK
    assert len(repo.connector.users.by_id) == 1
    assert len(repo.connector.history) == 1
    assert repo.connector.last_request['method'] == 'post'
    assert repo.connector.last_request['resource'] == 'users_all'
    assert repo.connector.last_request['json'] == data

    root = fromstring(response.content)
    assert root.tag == 'response'
    assert root.text == list(repo.connector.users.by_id.keys())[0]

    user = repo.get_one({'user_id': root.text})
    assert user.login == '1'
    assert user.first_name == '2'
    assert user.last_name == '3'
    assert user.department_id == '4'
    assert user.user_id == root.text


@patch.object(UserRepository, 'connector_cls', MockedConnector)
def test_user_update():
    repo = UserRepository()
    assert isinstance(repo.connector, MockedConnector)

    users = repo.get_all({})
    assert len(users) == len(repo.connector.users.by_id)

    old_user = users[0]
    url_vars = {'user_id': old_user.user_id}
    new_body = {
        'first_name': '1',
        'last_name': '2',
        'department_id': '3',
        'login': '4',
    }

    response = repo.update(url_vars, new_body)
    assert response.status_code == Code.OK
    assert repo.connector.last_request['resource'] == 'user'
    assert repo.connector.last_request['url_vars'] == url_vars
    assert repo.connector.last_request['method'] == 'post'

    new_user = repo.get_one(url_vars)
    assert new_user.user_id == old_user.user_id
    assert new_user.first_name == '1'
    assert new_user.last_name == '2'
    assert new_user.department_id == '3'
    assert new_user.login == '4'


@patch.object(UserRepository, 'connector_cls', MockedConnector)
def test_user_delete():
    repo = UserRepository()
    assert isinstance(repo.connector, MockedConnector)

    users = repo.get_all({})
    assert len(users) == len(repo.connector.users.by_id)
    assert len(repo.connector.history) == 1

    user = users[0]
    lookup = {'user_id': user.user_id}
    response = repo.delete(lookup)
    assert response.status_code == Code.OK

    assert len(repo.connector.history) == 2
    assert repo.connector.last_request['method'] == 'delete'
    assert repo.connector.last_request['url_vars'] == lookup
