# -*- coding: utf-8 -*-
import datetime
import pytest
import mock

from balance import constants as cst

from tests import object_builder as ob
from tests.balance_tests.process_completions.common import (
    create_order,
    migrate_client,
)


@pytest.fixture(autouse=True)
def mock_month_closed(request):
    do_mock = 'dont_mock_mnclose' not in request.keywords
    patcher = mock.patch('balance.mncloselib.is_month_closed', return_value=False)
    if do_mock:
        patcher.start()
    yield
    if do_mock:
        patcher.stop()


@pytest.fixture
def order(request, session):
    return create_order(session, **getattr(request, 'param', {}))


@pytest.fixture
def client(session):
    client = ob.ClientBuilder.construct(session)
    migrate_client(client)
    client.exports['MIGRATE_TO_CURRENCY'].state = cst.ExportState.exported
    client.exports['MIGRATE_TO_CURRENCY'].export_dt = datetime.datetime.now()
    session.flush()
    return client
