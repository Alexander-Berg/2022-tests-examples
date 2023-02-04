import pytest

from intranet.paste.src.api_v1.pastes.schemas import STUB_UUID

pytestmark = [
    pytest.mark.django_db,
]


def test_smoke_view(client, users):
    client.login(users.thasonic).use_oauth()
    response = client.get('/api/v1/diag')
    assert response.status_code == 200, response.json()
    client.logout()
    response = client.get('/api/v1/diag')
    assert response.status_code == 401


def test_autodoc(client, pastes, users):
    client.login(users.thasonic).use_oauth()
    response = client.get('/api/v1/docs')
    assert response.status_code == 200, response.json()


def test_get_paste(client, pastes, users):
    client.login(users.thasonic).use_oauth()

    response = client.get(f'/api/v1/pastes/{pastes[0].uuid}')
    assert response.status_code == 200, response.json()

    response = client.get(f'/api/v1/pastes/{STUB_UUID}')
    assert response.status_code == 404, response.json()

    # --- old style:
    response = client.get(f'/{pastes[0].id}')
    assert response.status_code == 200

    # --- new style:
    response = client.get(f'/{pastes[0].uuid}')
    assert response.status_code == 200


def test_list_pastes(client, pastes, users):
    client.login(users.thasonic).use_oauth()
    pastes[0].text = 'X' * 512
    pastes[0].save()
    response = client.get('/api/v1/me/pastes?page_size=2&page_id=1')
    assert response.status_code == 200, response.json()
    data = response.json()
    assert data['results'][0]['text'] == 'X' * 256 + '...'

    client.login(users.robot_wiki).use_oauth()
    response = client.get('/api/v1/me/pastes?page_size=2&page_id=1')
    assert response.status_code == 200, response.json()
    data = response.json()
    assert len(data['results']) == 0


def test_create_paste(client, users):
    client.login(users.thasonic).use_oauth()

    response = client.post(
        '/api/v1/me/pastes',
        {'syntax': 'plain', 'text': 'some text for test'},
    )
    assert response.status_code == 200, response.json()
    response = client.post(
        '/api/v1/me/pastes',
        {'syntax': 'c#', 'text': 'print(123)'},
    )
    assert response.status_code == 200, response.json()
    assert response.json()['syntax'] == 'c#'

    response = client.post(
        '/api/v1/me/pastes',
        {'syntax': 'azazazaz', 'text': 'another test text'},
    )
    assert response.status_code == 400, response.json()


def test_delete_paste(client, users, pastes):
    client.login(users.thasonic).use_oauth()

    response = client.delete(f'/api/v1/pastes/{pastes[0].uuid}')
    assert response.status_code == 204, response.json()

    response = client.get(f'/api/v1/pastes/{pastes[0].uuid}')
    assert response.status_code == 404, response.json()


def test_delete_forbidden(client, users, pastes):
    client.login(users.robot_wiki).use_oauth()

    response = client.delete(f'/api/v1/pastes/{pastes[0].uuid}')
    assert response.status_code == 403, response.json()
