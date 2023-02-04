import pytest

from maps.b2bgeo.ya_courier.backend.test_lib.conftest import skip_if_remote
from maps.b2bgeo.ya_courier.backend.test_lib.util_offline import Environment
from maps.b2bgeo.libs.py_unistat import global_unistat as unistat
import maps.b2bgeo.test_lib.passport_uid_values as passport_uid_values
from ya_courier_backend.interservice.apikeys import ApikeyStatus
from ya_courier_backend.resources.unistat import (
    SIGNAL_APIKEYS_SERVICE_UNAVAILABLE_COUNT,
    SIGNAL_APIKEYS_BAD_REQUEST_COUNT,
)


@skip_if_remote
def test_create_apikey_ok(env: Environment):
    with env.flask_app.app_context():
        apikeys_service = env.flask_app.apikeys_service
        apikey = apikeys_service.create_key(passport_uid_values.VALID)
        assert apikey
        assert apikeys_service.get_apikey_status(apikey) == ApikeyStatus.active


@pytest.mark.parametrize("passport_value,unistat_signal", [
    (passport_uid_values.SIMULATE_INTERNAL_ERROR, SIGNAL_APIKEYS_SERVICE_UNAVAILABLE_COUNT),
    (passport_uid_values.INVALID, SIGNAL_APIKEYS_BAD_REQUEST_COUNT),
])
@skip_if_remote
def test_create_apikey_error(env: Environment, passport_value, unistat_signal):
    with env.flask_app.app_context():
        apikeys_service = env.flask_app.apikeys_service
        apikey = apikeys_service.create_key(passport_value)
        assert apikey is None
        assert unistat.holes[unistat_signal].get_value(False)[1] > 0
