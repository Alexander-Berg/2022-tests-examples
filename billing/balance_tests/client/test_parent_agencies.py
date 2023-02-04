# -*- coding: utf-8 -*-

from __future__ import with_statement

from tests.balance_tests.client.client_common import create_client, create_order


def test_get_parent_agencies(session, client):
    agencies = []
    for _ in range(3):
        agency = create_client(session, is_agency=1)
        agencies.append(agency)
        create_order(session, client=client, agency=agency)

    assert sorted(agencies) == sorted(client.parent_agencies)


def test_get_parent_agencies_empty_list(session, client):
    create_order(session, client=client)
    assert client.parent_agencies == []


def test_get_parent_agencies_no_orders(session, client):
    assert client.parent_agencies == []
