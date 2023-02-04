# -*- coding: utf-8 -*-

import pytest

from .common import (
    create_client,
    create_order,
)

pytestmark = [
    pytest.mark.log_tariff,
]


@pytest.fixture
def client(session):
    return create_client(session)


@pytest.fixture
def order(session, client):
    return create_order(session, client)
