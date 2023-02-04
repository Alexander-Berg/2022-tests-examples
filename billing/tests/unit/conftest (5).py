import re

import pytest

from sendr_auth.entities import AuthenticationMethod

from billing.yandex_pay_plus.yandex_pay_plus.core.actions.base import BaseAction
from billing.yandex_pay_plus.yandex_pay_plus.tests.common_conftest import *  # noqa
from billing.yandex_pay_plus.yandex_pay_plus.tests.db import *  # noqa


@pytest.fixture
def db_engine(mocked_db_engine):
    return mocked_db_engine


@pytest.fixture(autouse=True)
def action_context_setup(dummy_logger, app, db_engine, request_id):
    # 'app' fixture is required to guarantee the execution order
    # since BaseInteractionClient.CONNECTOR is set by web.Application
    BaseAction.setup_context(
        logger=dummy_logger,
        request_id=request_id,
        db_engine=db_engine,
    )
    assert BaseAction.context.storage is None


@pytest.fixture
def core_context(action_context_setup):
    return BaseAction.context


@pytest.fixture
def mock_internal_tvm(aioresponses_mocker, yandex_pay_plus_settings):
    aioresponses_mocker.get(
        re.compile(f'.*{yandex_pay_plus_settings.TVM_URL}/tvm/checksrv.*'),
        payload={
            'src': yandex_pay_plus_settings.TVM_ALLOWED_SRC[0],
            'dst': yandex_pay_plus_settings.TVM_ALLOWED_SRC[0],
        },
    )


@pytest.fixture
def mock_user_authentication(mocker, entity_auth_user):
    entity_auth_user.auth_method = AuthenticationMethod.SESSION
    return mocker.patch('sendr_auth.BlackboxAuthenticator.get_user', mocker.AsyncMock(return_value=entity_auth_user))
