# -*- coding: utf-8 -*-
from tests.balance_tests.request.request_common import create_request, create_client, create_service, create_order


def test_doesnt_need_a_contract(session):
    r = create_request(session)
    assert not r.needs_a_contract()


def test_needs_a_contract(session):
    service = create_service(session, contract_needed_agency=1)
    agency = create_client(session, is_agency=True)
    r = create_request(session, client=agency,
                       orders=[create_order(session, service=service, client=agency)])
    assert r.needs_a_contract()
