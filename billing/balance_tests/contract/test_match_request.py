# -*- coding: utf-8 -*-
import pytest
import datetime

from balance.mapper import Service

from tests import object_builder as ob
from tests.balance_tests.contract.contract_common import (create_contract,
                             create_order,
                             create_request,
                             create_client,
                             ACQUIRING,
                             AFISHA,
                             DISTRIBUTION,
                             GENERAL,
                             GEOCONTEXT,
                             PARTNERS,
                             PREFERRED_DEAL,
                             SPENDABLE)

NOW = datetime.datetime.now()
YESTERDAY = NOW - datetime.timedelta(days=1)


@pytest.mark.parametrize('contract_type',
                         [ACQUIRING, AFISHA, DISTRIBUTION, GENERAL, GEOCONTEXT, PARTNERS, PREFERRED_DEAL, SPENDABLE])
def test_match_request_contract_type(session, contract_type):
    request = create_request(session, client=create_client(session))
    contract = create_contract(session, is_signed=NOW, dt=NOW,
                               services=request.request_orders[0].order.service.id,
                               client=request.client, ctype=contract_type)
    is_contract_match_request = contract.match_request(request.client, NOW,
                                                       {request.request_orders[0].order.service.id})
    if contract_type == GENERAL:
        assert is_contract_match_request is True
    else:
        assert is_contract_match_request is False


def test_match_request_cancelled_contract(session):
    """аннулированный договор не соответствует реквесту"""
    request = create_request(session)
    contract = create_contract(session, is_signed=NOW, dt=NOW, is_cancelled=NOW,
                               services=request.request_orders[0].order.service.id,
                               client=request.client, ctype=GENERAL)
    is_contract_match_request = contract.match_request(request.client, NOW,
                                                       request.request_orders[0].order.service.id)

    assert is_contract_match_request is False


def test_match_request_future_dt_contract(session):
    """договор с датой в будущем (даже если подписан раньше даты проверки) не соответствует реквесту"""
    request = create_request(session)
    contract = create_contract(session, is_signed=NOW, dt=NOW + datetime.timedelta(seconds=1),
                               services=request.request_orders[0].order.service.id,
                               client=request.client, ctype=GENERAL)
    is_contract_match_request = contract.match_request(request.client, NOW,
                                                       request.request_orders[0].order.service.id)

    assert is_contract_match_request is False


@pytest.mark.parametrize('finish_dt', [NOW, NOW - datetime.timedelta(seconds=1), NOW + datetime.timedelta(days=1)])
def test_match_request_finish_dt_contract(session, finish_dt):
    """если дата конца договора указана, договор соответствует реквесту, пока дата проверки меньше, чем дата
     конца договора"""
    request = create_request(session)
    contract = create_contract(session, is_signed=YESTERDAY, dt=YESTERDAY, finish_dt=finish_dt,
                               services=request.request_orders[0].order.service.id,
                               client=request.client, ctype=GENERAL)
    is_contract_match_request = contract.match_request(request.client, NOW,
                                                       {request.request_orders[0].order.service.id})
    if finish_dt <= NOW:
        assert is_contract_match_request is False
    else:
        assert is_contract_match_request is True


@pytest.mark.parametrize('services_in_request, services_in_contract, is_contract_match_request_expected',
                         [((7, 11), (11,), False),
                          ((11,), (11,), True),
                          ((11,), (11, 7), True)])
def test_match_request_by_service(session, services_in_request, services_in_contract,
                                  is_contract_match_request_expected):
    """договор соответствует реквесту, если все сервисы из реквеста указаны в нем"""
    client = create_client(session)
    orders = []
    for service_id in services_in_request:
        orders.append(create_order(session, client=client, service=ob.Getter(Service, service_id)))
    request = create_request(session, client=client, orders=orders)
    contract = create_contract(session, is_signed=YESTERDAY, dt=YESTERDAY,
                               services=request.request_orders[0].order.service.id,
                               client=request.client, ctype=GENERAL)
    is_contract_match_request = contract.match_request(request.client, NOW,
                                                       {request_order.order.service.id for request_order in
                                                        request.request_orders})
    assert is_contract_match_request == is_contract_match_request_expected
