# -*- coding: utf-8 -*-

from balance.xmlizer import getxmlizer
from balance.actions.invoice_turnon import InvoiceTurnOn
from tests.balance_tests.xmlizer.xmlizer_common import create_invoice


def test_smoke(session):
    inv = create_invoice(session)
    InvoiceTurnOn(inv, manual=True).do()
    offset, limit = 0, 10000
    getxmlizer(inv).xmlize_operations(offset, limit)
    getxmlizer(inv.invoice_orders[0].order).xmlize_operations(offset, limit)
