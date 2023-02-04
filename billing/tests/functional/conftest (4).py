import re

import pytest
import yarl
from aioresponses import CallbackResult

from billing.yandex_pay_admin.yandex_pay_admin.core.entities.enums import PayBackendType
from billing.yandex_pay_admin.yandex_pay_admin.tests.common_conftest import *  # noqa
from billing.yandex_pay_admin.yandex_pay_admin.tests.db import *  # noqa


@pytest.fixture
def db_engine(raw_db_engine):
    return raw_db_engine


@pytest.fixture
def mock_pay_plus_backend_put_merchant(yandex_pay_admin_settings, aioresponses_mocker):
    def _mock_pay_plus_backend_put_merchant(backend_type):
        url_map = {
            PayBackendType.SANDBOX: yandex_pay_admin_settings.YANDEX_PAY_PLUS_BACKEND_SANDBOX_URL,
            PayBackendType.PRODUCTION: yandex_pay_admin_settings.YANDEX_PAY_PLUS_BACKEND_PRODUCTION_URL,
        }
        update_url = yarl.URL(url_map[backend_type])
        update_url /= 'api/internal/v1/merchants'
        return aioresponses_mocker.put(re.compile(f'^{update_url}/.*$'), payload={}, repeat=True)

    return _mock_pay_plus_backend_put_merchant


@pytest.fixture
def mock_pay_plus_backend_put_merchant_both_environments(mock_pay_plus_backend_put_merchant):
    mock_pay_plus_backend_put_merchant(PayBackendType.PRODUCTION)
    mock_pay_plus_backend_put_merchant(PayBackendType.SANDBOX)


@pytest.fixture
def mock_pay_backend_put_merchant(yandex_pay_admin_settings, aioresponses_mocker):
    def _mock_pay_backend_put_merchant(backend_type):
        url_map = {
            PayBackendType.SANDBOX: yandex_pay_admin_settings.YANDEX_PAY_BACKEND_SANDBOX_URL,
            PayBackendType.PRODUCTION: yandex_pay_admin_settings.YANDEX_PAY_BACKEND_PRODUCTION_URL,
        }
        update_url = yarl.URL(url_map[backend_type])
        update_url /= 'api/internal/v1/merchants'
        return aioresponses_mocker.put(re.compile(f'^{update_url}/.*$'), payload={}, repeat=True)

    return _mock_pay_backend_put_merchant


@pytest.fixture
def mock_pay_backend_put_merchant_both_environments(mock_pay_backend_put_merchant):
    mock_pay_backend_put_merchant(PayBackendType.PRODUCTION)
    mock_pay_backend_put_merchant(PayBackendType.SANDBOX)


@pytest.fixture
def authenticate_client():
    def _authenticate_client(test_client, method='sessionid'):
        if method == 'sessionid':
            test_client.session.cookie_jar.update_cookies({'Session_id': 'UNITTEST_SESSION_ID'})
            return
        raise ValueError('Authentication method not supported')

    return _authenticate_client


@pytest.fixture
def mock_sessionid_auth(aioresponses_mocker, yandex_pay_admin_settings, user):
    def blackbox_callback(url, *, params, **kwargs):
        if params.get('sessionid') == 'UNITTEST_SESSION_ID':
            return CallbackResult(
                status=200,
                payload={
                    'status': {'value': 'VALID'},
                    'error': 'OK',
                    'uid': {
                        'value': user.uid,
                    },
                    'login_id': 'loginid:unittest',
                },
            )
        return CallbackResult(status=400)

    base_url = yandex_pay_admin_settings.BLACKBOX_API_URL.rstrip('/')
    return aioresponses_mocker.get(
        re.compile(fr'{base_url}\?.*method=sessionid.*'),
        callback=blackbox_callback,
        repeat=True,
    )


@pytest.fixture
def mock_app_authentication(mock_sessionid_auth, app, authenticate_client):
    authenticate_client(app)


@pytest.fixture
def mock_testing_app_authentication(mock_sessionid_auth, testing_app, authenticate_client):
    authenticate_client(testing_app)
