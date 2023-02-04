import re

import pytest
from aioresponses import CallbackResult

from pay.bill_payments.bill_payments.tests.common_conftest import *  # noqa
from pay.bill_payments.bill_payments.tests.db import *  # noqa
from pay.bill_payments.bill_payments.tests.entities import *  # noqa


@pytest.fixture
def db_engine(raw_db_engine):
    return raw_db_engine


@pytest.fixture
def authenticate_client():
    def _authenticate_client(test_client, method='sessionid'):
        if method == 'sessionid':
            test_client.session.cookie_jar.update_cookies({'Session_id': 'UNITTEST_SESSION_ID'})
            return
        raise ValueError('Authentication method not supported')

    return _authenticate_client


@pytest.fixture
def mock_sessionid_auth(aioresponses_mocker, bill_payments_settings, user):
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
        return CallbackResult(status=500)

    base_url = bill_payments_settings.BLACKBOX_API_URL.rstrip('/')
    return aioresponses_mocker.get(
        re.compile(fr'{base_url}\?.*method=sessionid.*'),
        callback=blackbox_callback,
        repeat=True,
    )


@pytest.fixture
def mock_app_authentication(mock_sessionid_auth, app, authenticate_client):
    authenticate_client(app)
