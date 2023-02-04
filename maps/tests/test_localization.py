import json
import requests
from maps.b2bgeo.mvrp_solver.backend.tests_lib.util import API_KEY


def _check_errors_differ(url, method, data=None):
    response_default = method(url, data)
    json_default = response_default.json()

    if '?' in url:
        url += '&lang=ru_RU'
    else:
        url += '?lang=ru_RU'
    response_locale = method(url, data)
    json_locale = response_locale.json()

    assert json_default['error']['message'] != json_locale['error']['message']


def test_invalid_locale(async_backend_url):
    url = f'{async_backend_url}/add/mvrp?apikey={API_KEY}&lang=invalid'
    response = requests.post(url, '{}')
    jsn = response.json()

    assert response.status_code == requests.codes.bad_request, json.dumps(jsn)
    assert 'error' in jsn
    assert len(jsn['error']['incident_id'])
    assert 'Unknown locale' in jsn['error']['message']


def test_add(async_backend_url):
    url = f'{async_backend_url}/add/mvrp?apikey={API_KEY}'
    _check_errors_differ(url, requests.post, '')


def test_result(async_backend_url):
    url = f'{async_backend_url}/result/123'
    _check_errors_differ(url, requests.get)

# TODO: (BBGEO-12278) add test for /children handler
