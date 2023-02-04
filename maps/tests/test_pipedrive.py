import requests
from maps.b2bgeo.test_lib.mock_pipedrive import mock_pipedrive


def test_unknown_path():
    with mock_pipedrive() as url:
        params = {
            'api_token': 'xxx'
        }
        resp = requests.get(url + '/unknown', params=params)
        assert resp.status_code == 404


def test_api_token():
    with mock_pipedrive() as url:
        resp = requests.get(url + '/persons')
        assert resp.status_code == 401


def test_persons():
    with mock_pipedrive() as url:
        params = {
            'api_token': 'xxx'
        }
        payload = {
            "name": "Vasya",
            "email": "pupkin@yandex.ru",
            "phone": "112"
        }
        resp = requests.post(url + '/persons', params=params, json=payload)
        assert resp.status_code == 200

        resp = requests.get(url + "/persons", params=params)
        assert resp.status_code == 200
        j = resp.json()
        assert j.get('success')
        persons = j.get('data', [])
        assert len(persons) == 1
        assert persons[0].get("name") == "Vasya"
        assert persons[0].get("id") == 0


def get_custom_fields(url):
    resp = requests.get(f"{url}/dealFields?api_token=xxx")
    return {x['name']: x['key'] for x in resp.json()['data']}


def test_deals():
    with mock_pipedrive() as url:
        custom_fields = get_custom_fields(url)
        params = {
            'api_token': 'xxx'
        }
        payload = {
            "title": "Company",
            "person_id": 1,
            custom_fields["utm_source"]: "utm_source_value"
        }
        resp = requests.post(url + '/deals', params=params, json=payload)
        assert resp.status_code == 200
        j = resp.json()
        assert j.get('success')
        assert j.get('data', {}).get('id') == 0
        assert j.get('data', {}).get(custom_fields['utm_source']) == 'utm_source_value'
