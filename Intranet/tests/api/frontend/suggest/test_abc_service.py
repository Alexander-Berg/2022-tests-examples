import pytest

pytestmark = pytest.mark.django_db

expected_data = {
    'offset': 0,
    'limit': 10,
    'results': [
        {
            'id': 1,
            'type': 'default',
            'title': {'ru': 'Родительский сервис', 'en': 'Parent service'},
        },
        {
            'id': 2,
            'type': 'default',
            'title': {'ru': 'Тестовый сервис', 'en': 'Test service'},
        },
        {
            'id': 3,
            'type': 'default',
            'title': {'ru': 'Другой сервис', 'en': 'Another service'},
        }
    ]
}


def test_abc_services(crt_client, users, abc_services):
    crt_client.login('normal_user')

    response = crt_client.json.get('/api/frontend/suggest/abc_services/')
    assert response.status_code == 200
    data = response.json()
    assert data == expected_data


def test_limit_offset(crt_client, users, abc_services):
    crt_client.login('normal_user')

    response = crt_client.json.get('/api/frontend/suggest/abc_services/?offset=1&limit=1')
    assert response.status_code == 200
    data = response.json()
    new_data = expected_data.copy()
    new_data['limit'] = 1
    new_data['offset'] = 1
    new_data['results'] = new_data['results'][1:2]
    assert data == new_data


def test_filter_id(crt_client, users, abc_services):
    crt_client.login('normal_user')

    response = crt_client.json.get('/api/frontend/suggest/abc_services/?id=1')
    assert response.status_code == 200
    data = response.json()
    new_data = expected_data.copy()
    new_data['results'] = new_data['results'][0:1]
    assert data == new_data

    response = crt_client.json.get('/api/frontend/suggest/abc_services/?id=1&id=3')
    assert response.status_code == 200
    data = response.json()
    new_data = expected_data.copy()
    new_data['results'] = new_data['results'][0:1] + new_data['results'][2:3]
    assert data == new_data


@pytest.mark.parametrize('text', ['Another', 'Другой', 'ANOTHER_SE'])
def test_search(crt_client, users, abc_services, text):
    crt_client.login('normal_user')

    response = crt_client.json.get('/api/frontend/suggest/abc_services/?search=%s' % text)
    assert response.status_code == 200
    data = response.json()
    new_data = expected_data.copy()
    new_data['results'] = new_data['results'][2:3]
    assert data == new_data
