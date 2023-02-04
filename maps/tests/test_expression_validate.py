from http import HTTPStatus
import json
import requests


CORRECT_EXPRESSION = '100 * duration_h + 8 * distance_km'
API_KEY = "7f963d7c-5fa8-48b9-8ce7-1e8796e0dc76"
REQUEST_PATH = f'/validate/vehicle_cost?apikey={API_KEY}'


def test_expression_validate_response_correct_expression(async_backend_url):
    expression = {
        'expression': CORRECT_EXPRESSION
    }
    resp = requests.post(f'{async_backend_url}{REQUEST_PATH}', json.dumps(expression))
    assert resp.status_code == HTTPStatus.OK
    assert resp.json() == {}


def test_expression_validate_response_incorrect_expression(async_backend_url):
    expression = {
        'expression': 'some strange expression'
    }
    resp = requests.post(f'{async_backend_url}{REQUEST_PATH}', json.dumps(expression))
    assert resp.status_code == HTTPStatus.UNPROCESSABLE_ENTITY
    assert 'error' in resp.json()
    assert 'message' in resp.json()['error']
    assert 'incident_id' in resp.json()['error']


def test_expression_validate(async_backend_url):
    expression = {
        'expression': CORRECT_EXPRESSION
    }
    resp = requests.post(f'{async_backend_url}{REQUEST_PATH}', json.dumps(expression))
    assert resp.status_code == HTTPStatus.OK


def test_expression_validate_incorrect_schema(async_backend_url):
    expression = {
        'not_expression': CORRECT_EXPRESSION
    }
    resp = requests.post(f'{async_backend_url}{REQUEST_PATH}', json.dumps(expression))
    assert resp.status_code == HTTPStatus.BAD_REQUEST
    assert 'Request parameters do not meet the requirements.' in resp.json()['error']['message']


def test_expression_validate_null_expression(async_backend_url):
    expression = {
        'expression': None
    }
    resp = requests.post(f'{async_backend_url}{REQUEST_PATH}', json.dumps(expression))
    assert resp.status_code == HTTPStatus.BAD_REQUEST
    assert 'Request parameters do not meet the requirements.' in resp.json()['error']['message']


def test_expression_validate_incorrect_expression(async_backend_url):
    expression = {
        'expression': 'some incorrect expression'
    }
    resp = requests.post(f'{async_backend_url}{REQUEST_PATH}', json.dumps(expression))
    assert resp.status_code == HTTPStatus.UNPROCESSABLE_ENTITY
    assert 'Error while parsing' in resp.json()['error']['message']


def test_expression_validate_ivalid_json(async_backend_url):
    expression = {
        'expression': '{'
    }
    resp = requests.post(f'{async_backend_url}{REQUEST_PATH}', expression)
    assert resp.status_code == HTTPStatus.BAD_REQUEST
    assert 'Invalid value' in resp.json()['error']['message']
