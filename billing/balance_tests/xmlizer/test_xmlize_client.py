# -*- coding: utf-8 -*-

from balance.xmlizer import getxmlizer
from tests.balance_tests.xmlizer.xmlizer_common import create_client


def test_smoke(session):
    client = create_client(session)
    getxmlizer(client).xmlize()
