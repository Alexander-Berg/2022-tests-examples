# -*- coding: utf-8 -*-

import pytest

from tests import object_builder as ob
from tests.balance_tests.overdraft.common import (
    create_invoice,
)


@pytest.fixture()
def client(request, session):
    return ob.ClientBuilder.construct(session, is_agency=getattr(request, 'param', 0))


@pytest.fixture
def invoice(client):
    return create_invoice(client)


@pytest.fixture
def person(session, client):
    return ob.PersonBuilder.construct(session, client=client)
