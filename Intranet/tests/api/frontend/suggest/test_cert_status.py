import pytest

from intranet.crt.constants import CERT_STATUS

pytestmark = pytest.mark.django_db

expected_data = {
    'offset': 0,
    'limit': 3,
    'results': [
        {
            'id': CERT_STATUS.REQUESTED,
            'type': 'default',
            'title': CERT_STATUS.humanized[CERT_STATUS.REQUESTED],
        },
        {
            'id': CERT_STATUS.VALIDATION,
            'type': 'default',
            'title': CERT_STATUS.humanized[CERT_STATUS.VALIDATION],
        },
        {
            'id': CERT_STATUS.ISSUED,
            'type': 'default',
            'title': CERT_STATUS.humanized[CERT_STATUS.ISSUED],
        }
    ]
}


def test_statuses(crt_client, users):
    crt_client.login('normal_user')

    response = crt_client.json.get('/api/frontend/suggest/statuses/?limit=100500')
    assert response.status_code == 200
    data = response.json()
    assert len(data['results']) == len(CERT_STATUS.all_statuses())

    response = crt_client.json.get('/api/frontend/suggest/statuses/?limit=3')
    assert response.status_code == 200
    data = response.json()
    assert data == expected_data


def test_offset(crt_client, users):
    crt_client.login('normal_user')

    response = crt_client.json.get('/api/frontend/suggest/statuses/?offset=1&limit=2')
    assert response.status_code == 200
    data = response.json()
    new_data = expected_data.copy()
    new_data['limit'] = 2
    new_data['offset'] = 1
    new_data['results'] = new_data['results'][1:3]
    assert data == new_data


def test_filter_id(crt_client, users):
    crt_client.login('normal_user')

    response = crt_client.json.get('/api/frontend/suggest/statuses/?limit=3&id=requested')
    assert response.status_code == 200
    data = response.json()
    new_data = expected_data.copy()
    new_data['results'] = new_data['results'][0:1]
    assert data == new_data

    response = crt_client.json.get('/api/frontend/suggest/statuses/?limit=3&id=requested&id=issued')
    assert response.status_code == 200
    data = response.json()
    new_data = expected_data.copy()
    new_data['results'] = new_data['results'][0:1] + new_data['results'][2:3]
    assert data == new_data


@pytest.mark.parametrize('text', ['Needs ', 'Треб', 'need_'])
def test_search(crt_client, users, text):
    crt_client.login('normal_user')

    response = crt_client.json.get('/api/frontend/suggest/statuses/?limit=10&search=%s' % text)
    assert response.status_code == 200
    data = response.json()
    assert len(data['results']) == 1
    assert data['results'][0]['id'] == 'need_approve'
