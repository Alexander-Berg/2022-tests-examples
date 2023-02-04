import pytest


pytestmark = pytest.mark.django_db

expected_data = {
    'offset': 0,
    'limit': 10,
    'results': [
        {
            'id': 'TestCA',
            'type': 'default',
            'title': {'en': 'TestCA', 'ru': 'TestCA'},
        },
        {
            'id': 'ApprovableTestCA',
            'type': 'default',
            'title': {'en': 'ApprovableTestCA', 'ru': 'ApprovableTestCA'},
        },
        {
            'id': 'InternalTestCA',
            'type': 'default',
            'title': {'en': 'InternalTestCA', 'ru': 'InternalTestCA'},
        },
        {
            'id': 'CertumTestCA',
            'type': 'default',
            'title': {'en': 'CertumTestCA', 'ru': 'CertumTestCA'},
        },
        {
            'id': 'GlobalSignTestCA',
            'type': 'default',
            'title': {'en': 'GlobalSignTestCA', 'ru': 'GlobalSignTestCA'},
        },
    ]
}


def test_ca_names(crt_client, users):
    crt_client.login('normal_user')

    response = crt_client.json.get('/api/frontend/suggest/ca_names/')
    assert response.status_code == 200
    data = response.json()
    assert data == expected_data


def test_offset(crt_client, users):
    crt_client.login('normal_user')

    response = crt_client.json.get('/api/frontend/suggest/ca_names/?offset=1&limit=2')
    assert response.status_code == 200
    data = response.json()
    new_data = expected_data.copy()
    new_data['limit'] = 2
    new_data['offset'] = 1
    new_data['results'] = new_data['results'][1:3]
    assert data == new_data


def test_filter_id(crt_client, users):
    crt_client.login('normal_user')

    response = crt_client.json.get('/api/frontend/suggest/ca_names/?id=GlobalSignTestCA')
    assert response.status_code == 200
    data = response.json()
    new_data = expected_data.copy()
    new_data['results'] = new_data['results'][-1:]
    assert data == new_data

    response = crt_client.json.get('/api/frontend/suggest/ca_names/?id=GlobalSignTestCA&id=InternalTestCA')
    assert response.status_code == 200
    data = response.json()
    new_data = expected_data.copy()
    new_data['results'] = new_data['results'][2:3] + new_data['results'][4:5]
    assert data == new_data


@pytest.mark.parametrize('text', ['ApprovableTestCA', 'approvable', 'abletESTca'])
def test_search(crt_client, users, text):
    crt_client.login('normal_user')

    response = crt_client.json.get('/api/frontend/suggest/ca_names/?search=%s' % text)
    assert response.status_code == 200
    data = response.json()
    new_data = expected_data.copy()
    new_data['results'] = new_data['results'][1:2]
    assert data == new_data


def test_ca_names_in_suggest(crt_client, users):
    crt_client.login('normal_user')

    response = crt_client.json.get('/api/frontend/suggest/ca_names/')

    assert response.status_code == 200
    data = response.json()

    received_ca_names = {ca['id'] for ca in data['results']}
    expected_ca_names = {
        'TestCA', 'ApprovableTestCA', 'CertumTestCA', 'GlobalSignTestCA', 'InternalTestCA', 'TestCA'
    }

    assert received_ca_names == expected_ca_names
