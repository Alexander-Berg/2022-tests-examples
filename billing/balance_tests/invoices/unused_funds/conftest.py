# -*- coding: utf-8 -*-

import pytest

from tests import object_builder as ob
from tests.balance_tests.invoices.unused_funds.common import (
    create_order,
    create_invoice,
)


@pytest.fixture
def client(session):
    return ob.ClientBuilder().build(session).obj


@pytest.fixture
def person(session, client):
    return ob.PersonBuilder.construct(session, client=client)


@pytest.fixture
def order(client):
    return create_order(client)


@pytest.fixture
def invoice(client, order):
    return create_invoice(client=client, orders=[order], quantity=100)
