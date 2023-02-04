import pytest

from intranet.crt.core.models import Host

pytestmark = pytest.mark.django_db

expected_data = {
    'offset': 0,
    'limit': 10,
    'results': [
        {
            'id': 'yandex.ru',
            'type': 'default',
            'title': {'ru': 'yandex.ru', 'en': 'yandex.ru'},
        },
        {
            'id': 'crt.yandex-team.ru',
            'type': 'default',
            'title': {'ru': 'crt.yandex-team.ru', 'en': 'crt.yandex-team.ru'},
        },
        {
            'id': 'яндекс.рф',
            'type': 'default',
            'title': {'ru': 'яндекс.рф', 'en': 'яндекс.рф'},
        }
    ]
}


@pytest.fixture(autouse=True)
def hosts():
    Host.objects.create(hostname='yandex.ru')
    Host.objects.create(hostname='crt.yandex-team.ru')
    Host.objects.create(hostname='яндекс.рф')


def test_abc_services(crt_client, users):
    crt_client.login('normal_user')

    response = crt_client.json.get('/api/frontend/suggest/hosts/')
    assert response.status_code == 200
    data = response.json()
    assert data == expected_data


def test_limit_offset(crt_client, users):
    crt_client.login('normal_user')

    response = crt_client.json.get('/api/frontend/suggest/hosts/?offset=1&limit=1')
    assert response.status_code == 200
    data = response.json()
    new_data = expected_data.copy()
    new_data['limit'] = 1
    new_data['offset'] = 1
    new_data['results'] = new_data['results'][1:2]
    assert data == new_data


def test_filter_id(crt_client, users):
    crt_client.login('normal_user')

    response = crt_client.json.get('/api/frontend/suggest/hosts/?id=яндекс.рф')
    assert response.status_code == 200
    data = response.json()
    new_data = expected_data.copy()
    new_data['results'] = new_data['results'][2:3]
    assert data == new_data

    response = crt_client.json.get('/api/frontend/suggest/hosts/?id=яндекс.рф&id=crt.yandex-team.ru')
    assert response.status_code == 200
    data = response.json()
    new_data = expected_data.copy()
    new_data['results'] = new_data['results'][1:3]
    assert data == new_data


def test_search_en(crt_client, users):
    crt_client.login('normal_user')

    response = crt_client.json.get('/api/frontend/suggest/hosts/?search=yandex')
    assert response.status_code == 200
    data = response.json()
    new_data = expected_data.copy()
    new_data['results'] = new_data['results'][0:1]  # Not [0:2]!
    assert data == new_data

    response = crt_client.json.get('/api/frontend/suggest/hosts/?search=янд')
    assert response.status_code == 200
    data = response.json()
    new_data = expected_data.copy()
    new_data['results'] = new_data['results'][2:3]
    assert data == new_data
