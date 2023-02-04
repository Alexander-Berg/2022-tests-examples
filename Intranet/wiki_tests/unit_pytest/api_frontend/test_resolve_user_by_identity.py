from intranet.wiki.tests.wiki_tests.common.skips import only_biz, only_intranet
import pytest


@pytest.mark.django_db
def test_without_client_login(client, wiki_users):
    client.logout()
    data = {'identities': [{'uid': wiki_users.kolomeetz.get_uid()}]}
    response = client.post('/_api/frontend/.resolve_users_identities', data=data)
    assert response.status_code == 403


@pytest.mark.django_db
def test_empty_request_invalid(client, wiki_users):
    client.login(wiki_users.volozh)
    data = {}
    response = client.post('/_api/frontend/.resolve_users_identities', data=data)
    assert response.status_code == 409


@pytest.mark.django_db
def test_one_uid_valid(client, wiki_users):
    client.login(wiki_users.volozh)
    data = {'identities': [{'uid': wiki_users.kolomeetz.get_uid()}]}
    response = client.post('/_api/frontend/.resolve_users_identities', data=data)
    assert response.status_code == 200
    assert len(response.data['data']['users']) == 1


@pytest.mark.django_db
def test_one_uid_invalid(client, wiki_users):
    client.login(wiki_users.volozh)
    data = {'identities': [{'uid': -123456789}]}
    response = client.post('/_api/frontend/.resolve_users_identities', data=data)
    assert response.status_code == 409


@only_biz
@pytest.mark.django_db
def test_one_cloud_valid(client, wiki_users):
    client.login(wiki_users.volozh)
    data = {'identities': [{'cloud_uid': wiki_users.kolomeetz.get_cloud_uid()}]}
    response = client.post('/_api/frontend/.resolve_users_identities', data=data)
    assert response.status_code == 200
    assert len(response.data['data']['users']) == 1


@only_intranet
@pytest.mark.django_db
def test_one_cloud_intranet_invalid(client, wiki_users):
    client.login(wiki_users.volozh)
    data = {'identities': [{'cloud_uid': '474184475c51'}]}  # '474184475c51' == wiki_users.kolomeetz.get_cloud_uid()
    response = client.post('/_api/frontend/.resolve_users_identities', data=data)
    assert response.status_code == 409


@only_biz
@pytest.mark.django_db
def test_one_cloud_invalid(client, wiki_users):
    client.login(wiki_users.volozh)
    data = {'identities': [{'cloud_uid': 'invalid cloud uid'}]}
    response = client.post('/_api/frontend/.resolve_users_identities', data=data)
    assert response.status_code == 409


@pytest.mark.django_db
def test_many_uid_valid(client, wiki_users):
    client.login(wiki_users.volozh)
    data = {
        'identities': [
            {'uid': wiki_users.kolomeetz.get_uid()},
            {'uid': wiki_users.thasonic.get_uid()},
            {'uid': wiki_users.chapson.get_uid()},
        ]
    }
    response = client.post('/_api/frontend/.resolve_users_identities', data=data)
    assert response.status_code == 200
    assert len(response.data['data']['users']) == 3


@only_biz
@pytest.mark.django_db
def test_many_uid_and_cloud_valid(client, wiki_users):
    client.login(wiki_users.volozh)
    data = {
        'identities': [
            {'uid': wiki_users.kolomeetz.get_uid()},
            {'cloud_uid': wiki_users.thasonic.get_cloud_uid()},
            {'uid': wiki_users.chapson.get_uid()},
        ]
    }
    response = client.post('/_api/frontend/.resolve_users_identities', data=data)
    assert response.status_code == 200
    assert len(response.data['data']['users']) == 3


@pytest.mark.django_db
def test_many_valid_one_invalid(client, wiki_users):
    client.login(wiki_users.volozh)
    data = {
        'identities': [
            {'uid': wiki_users.kolomeetz.get_uid()},
            {'uid': wiki_users.thasonic.get_uid()},
            {'uid': 'invalid uid'},
        ]
    }
    response = client.post('/_api/frontend/.resolve_users_identities', data=data)
    assert response.status_code == 409


@pytest.mark.django_db
def test_one_check_received_login(client, wiki_users):
    client.login(wiki_users.volozh)
    data = {'identities': [{'uid': wiki_users.kolomeetz.get_uid()}]}
    response = client.post('/_api/frontend/.resolve_users_identities', data=data)
    assert response.data['data']['users'][0]['login'] == wiki_users.kolomeetz.get_username()


@pytest.mark.django_db
def test_many_check_received_logins(client, wiki_users):
    client.login(wiki_users.volozh)
    data = {'identities': [{'uid': wiki_users.kolomeetz.get_uid()}, {'uid': wiki_users.asm.get_uid()}]}
    response = client.post('/_api/frontend/.resolve_users_identities', data=data)

    received_logins = {user['login'] for user in response.data['data']['users']}
    assert received_logins == {wiki_users.kolomeetz.get_username(), wiki_users.asm.get_username()}


@pytest.mark.django_db
def test_none_invalid(client, wiki_users):
    client.login(wiki_users.volozh)
    data = {'identities': [{'uid': None}, {'cloud_uid': None}]}
    response = client.post('/_api/frontend/.resolve_users_identities', data=data)
    assert response.status_code == 409


@pytest.mark.django_db
def test_empty_invalid(client, wiki_users):
    client.login(wiki_users.volozh)
    data = {'identities': [{'cloud_uid': ''}, {'uid': ''}]}
    response = client.post('/_api/frontend/.resolve_users_identities', data=data)
    assert response.status_code == 409


@pytest.mark.django_db
def test_login__api_v2(client, wiki_users):
    client.login(wiki_users.volozh)

    users = [wiki_users.kolomeetz, wiki_users.thasonic, wiki_users.chapson]
    data = {'identities': [{'uid': user.get_uid()} for user in users]}

    response = client.post('/api/v2/public/users/resolve_identities', data)
    assert response.status_code == 200

    res = response.json()
    assert len(res['users']) == len(users)
    assert {user['id'] for user in res['users']} == {user.id for user in users}
