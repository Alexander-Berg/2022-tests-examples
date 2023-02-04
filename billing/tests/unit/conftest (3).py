import pytest

from billing.yandex_pay.yandex_pay.api.handlers.internal.base import BaseInternalHandler
from billing.yandex_pay.yandex_pay.core.actions.base import BaseAction
from billing.yandex_pay.yandex_pay.tests.common_conftest import *  # noqa
from billing.yandex_pay.yandex_pay.tests.db import *  # noqa


@pytest.fixture
def db_engine(mocked_db_engine):
    return mocked_db_engine


@pytest.fixture(autouse=True)
def action_context_setup(dummy_logger, app, db_engine, request_id, file_storage):
    # 'app' fixture is required to guarantee the execution order
    # since BaseInteractionClient.CONNECTOR is set by BaseYandexPayApplication
    BaseAction.setup_context(
        logger=dummy_logger,
        request_id=request_id,
        db_engine=db_engine,
        file_storage=file_storage,
        retry_budget=None,
    )
    assert BaseAction.context.storage is None


@pytest.fixture
def mock_internal_tvm(mocker):
    mocker.patch.object(BaseInternalHandler.TICKET_CHECKER, 'check_tvm_service_ticket', mocker.AsyncMock())


@pytest.fixture
def session_uid(unique_rand, randn):
    return unique_rand(randn)
