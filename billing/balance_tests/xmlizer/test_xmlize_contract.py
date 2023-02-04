# -*- coding: utf-8 -*-

from balance.xmlizer import getxmlizer
from tests.balance_tests.xmlizer.xmlizer_common import create_contract


def test_smoke(session):
    contract = create_contract(session)
    getxmlizer(contract).xmlize()
