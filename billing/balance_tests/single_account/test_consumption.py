# -*- coding: utf-8 -*-

import pytest

from balance.actions.single_account import consumption as spa_consumption
from balance.actions.single_account import prepare as spa_prepare
from balance import mapper
from balance.constants import *

from tests import object_builder as ob

pytestmark = [pytest.mark.single_account]


@pytest.fixture
def client(session):
    return ob.ClientBuilder(with_single_account=True).build(session).obj


@pytest.fixture
def order(client):
    return ob.OrderBuilder(
        client=client,
        product=ob.Getter(mapper.Product, DIRECT_PRODUCT_RUB_ID)
    ).build(client.session).obj


def create_spa_invoice(client):
    session = client.session
    person = ob.PersonBuilder(
        client=client,
        type='ur',
        inn=str(ob.get_big_number())
    ).build(session).obj

    return spa_prepare.autocreate_personal_account(person, client.single_account_number)


class TestGetUnusedFunds(object):
    def test_no_unused_funds(self, client, order):
        pa = create_spa_invoice(client)
        pa.create_receipt(100)
        pa.transfer(order)

        res = spa_consumption.get_unused_funds(client)
        assert [(row.invoice, row.amount) for row in res] == [(pa, 0)]

    def test_unused_funds(self, client, order):
        pa = create_spa_invoice(client)
        pa.create_receipt(100)
        pa.transfer(order, TransferMode.src, 34)

        res = spa_consumption.get_unused_funds(client)
        assert [(row.invoice, row.amount) for row in res] == [(pa, 66)]

    def test_negative_unused_funds(self, client, order):
        pa = create_spa_invoice(client)
        pa.create_receipt(50)
        pa.transfer(order, TransferMode.src, 66, skip_check=True)

        res = spa_consumption.get_unused_funds(client)
        assert [(row.invoice, row.amount) for row in res] == [(pa, 0)]

    def test_no_personal_accounts(self, client):
        assert spa_consumption.get_unused_funds(client) == []

    def test_multiple_personal_accounts(self, client):
        pa1 = create_spa_invoice(client)
        pa1.create_receipt(33)

        pa2 = create_spa_invoice(client)
        pa2.create_receipt(33)

        res = spa_consumption.get_unused_funds(client)
        assert {(row.invoice, row.amount) for row in res} == {(pa1, 33), (pa2, 33)}

    def test_single_account_disabled(self, client, order):
        pa = create_spa_invoice(client)
        client.single_account_number = None
        pa.create_receipt(50)

        res = spa_consumption.get_unused_funds(client)
        assert [(row.invoice, row.amount) for row in res] == []
