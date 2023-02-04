import pytest

from pay.bill_payments.bill_payments.core.actions.base import BaseAction
from pay.bill_payments.bill_payments.tests.common_conftest import *  # noqa
from pay.bill_payments.bill_payments.tests.db import *  # noqa
from pay.bill_payments.bill_payments.tests.entities import *  # noqa


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
