import pytest

from intranet.crt.constants import CERT_TYPE

pytestmark = pytest.mark.django_db


def test_cert_types(crt_client, users):
    crt_client.login('normal_user')

    response = crt_client.json.get('/api/frontend/suggest/cert_types/?limit=100500')
    assert response.status_code == 200
    data = response.json()
    assert len(data['results']) == len(CERT_TYPE.active_types())

    response = crt_client.json.get('/api/frontend/suggest/cert_types/?limit=3')
    assert response.status_code == 200
    data = response.json()
    assert data['offset'] == 0
    assert data['limit'] == 3
    assert len(data['results']) == 3
    assert data['results'][0].keys() == {'id', 'type', 'title'}
    assert data['results'][0]['title'].keys() == {'ru', 'en'}
    assert [item['id'] for item in data['results']] == ['host', 'pc', 'bank-pc']


def test_offset(crt_client, users):
    crt_client.login('normal_user')

    response = crt_client.json.get('/api/frontend/suggest/cert_types/?offset=1&limit=2')
    assert response.status_code == 200
    data = response.json()
    assert data['limit'] == 2
    assert data['offset'] == 1
    assert len(data['results']) == 2


def test_filter_id(crt_client, users):
    crt_client.login('normal_user')

    response = crt_client.json.get('/api/frontend/suggest/cert_types/?id=botik')
    assert response.status_code == 200
    data = response.json()
    assert [item['id'] for item in data['results']] == ['botik']

    response = crt_client.json.get('/api/frontend/suggest/cert_types/?id=botik&id=linux-token')
    assert response.status_code == 200
    data = response.json()
    assert [item['id'] for item in data['results']] == ['linux-token', 'botik']


@pytest.mark.parametrize('text', ['For botik', 'Для ботика', 'bot'])
def test_search(crt_client, users, text):
    crt_client.login('normal_user')

    response = crt_client.json.get('/api/frontend/suggest/cert_types/?search=%s' % text)
    assert response.status_code == 200
    data = response.json()
    assert [item['id'] for item in data['results']] == ['botik']


@pytest.mark.parametrize('ca_name', ['RcInternalCA', 'InternalCA'])
def test_filter_by_ca_name(crt_client, users, settings, ca_name):
    crt_client.login('normal_user')

    response = crt_client.json.get('/api/frontend/suggest/cert_types/', {'ca_name': ca_name})
    assert response.status_code == 200
    data = response.json()['results']
    if ca_name == 'RcInternalCA':
        assert len(data) == 1
        item = data[0]
        assert item['type'] == 'default'
        assert item['id'] == 'rc-server'
    else:
        # множество сертов InternalCA
        assert len(data) >= 10
