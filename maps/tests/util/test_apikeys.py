import pytest
from flask import Flask
import maps.b2bgeo.test_lib.apikey_values as apikey_values
import maps.b2bgeo.test_lib.passport_uid_values as passport_uid_values
from ya_courier_backend.config.common import APIKEYS_TEST_SERVER_URL
from ya_courier_backend.interservice.apikeys import YandexApikeysService, ApikeyStatus
from ya_courier_backend.interservice.apikeys.yandex import APIKEYS_SERVICE_TOKEN
from maps.b2bgeo.ya_courier.backend.test_lib.conftest import (
    MOCK_APIKEYS_CONTEXT,
    APIKEYS_SERVICE_COUNTER,
    skip_if_remote,
)


@pytest.fixture(scope='module')
def _flask_mock_app():
    app = Flask(__name__)
    with app.app_context():
        yield app


def _get_apikeys_url(system_env_with_db):
    return system_env_with_db.mock_apikeys_url or APIKEYS_TEST_SERVER_URL


def _get_mock_counters():
    return MOCK_APIKEYS_CONTEXT[APIKEYS_SERVICE_TOKEN]['counters']


def test_apikey_validity(system_env_with_db, _flask_mock_app):
    apikeys_url = _get_apikeys_url(system_env_with_db)
    apikeys_service = YandexApikeysService(apikeys_url)
    for apikey in [None, 1, {}, [], '', ' ']:
        assert apikeys_service.get_apikey_status(apikey) == ApikeyStatus.invalid_format
    assert apikeys_service.get_apikey_status(apikey_values.ACTIVE) == ApikeyStatus.active
    assert apikeys_service.get_apikey_status(apikey_values.BANNED) == ApikeyStatus.unknown_or_banned
    assert apikeys_service.get_apikey_status(apikey_values.UNKNOWN) == ApikeyStatus.unknown_or_banned


def test_create_apikey(system_env_with_db, _flask_mock_app):
    apikeys_url = _get_apikeys_url(system_env_with_db)
    apikeys_service = YandexApikeysService(apikeys_url)
    apikey = apikeys_service.create_key(passport_uid_values.VALID)
    assert apikey
    assert apikeys_service.get_apikey_status(apikey) == ApikeyStatus.active


@skip_if_remote
def test_server_error_on_mock_apikeys_server(system_env_with_db, _flask_mock_app):
    apikeys_url = _get_apikeys_url(system_env_with_db)
    apikeys_service = YandexApikeysService(apikeys_url)

    counter_value = _get_mock_counters()[APIKEYS_SERVICE_COUNTER]

    assert apikeys_service.get_apikey_status(apikey_values.MOCK_SIMULATE_ERROR) == ApikeyStatus.service_unavailable

    assert _get_mock_counters() == {APIKEYS_SERVICE_COUNTER: counter_value}
