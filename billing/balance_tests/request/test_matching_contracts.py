# -*- coding: utf-8 -*-
import pytest
import datetime
from mock import patch

from billing.contract_iface import ContractTypeId
from tests.balance_tests.contract.contract_common import (create_contract,
                                                          create_request,
                                                          create_client,
                                                          create_order,
                                                          GENERAL)

NOW = datetime.datetime.now()
YESTERDAY = NOW - datetime.timedelta(days=1)


@patch('balance.mapper.contracts.Contract.match_request')
def test_is_match_request_called(match_request_mock, session):
    request = create_request(session)
    create_contract(session, is_signed=NOW, dt=NOW, services=request.request_orders[0].order.service.id,
                    client=request.client, ctype=GENERAL)
    request.matching_contracts()
    assert match_request_mock.call_count == 1


def test_w_partly_matching_contract(session):
    """Если у клиента несколько договоров и некотрые из них не соответствуют реквесту, возвращаем только оставшиеся"""
    request = create_request(session)
    contracts = []
    for is_cancelled_value in [NOW, None]:
        contracts.append(create_contract(session, is_signed=NOW, dt=NOW, is_cancelled=is_cancelled_value,
                                         services=request.request_orders[0].order.service.id,
                                         client=request.client, ctype=GENERAL))

    assert request.matching_contracts() == [contracts[1]]


def test_wo_matching_contract(session):
    """Если ни один договор не подходит реквесту, возвращаем пустой список"""
    request = create_request(session)
    contract = create_contract(session, is_signed=NOW, dt=NOW, is_cancelled=NOW,
                               services=request.request_orders[0].order.service.id,
                               client=request.client, ctype=GENERAL)

    assert request.matching_contracts() == []


@pytest.mark.parametrize('non_resident_clients_value', [True, False])
def test_commission_contract_with_non_residents(session, non_resident_clients_value):
    """Если в реквесте указан хотя бы один субклиент нерезидент, комиссионные договоры подтягиваем, только если
     в них есть галочка 'работа с нерезидентами', остальные договоры подтягиваем как обычно"""
    agency = create_client(session, is_agency=1)
    orders = []
    for _ in range(2):
        orders.append(create_order(session, agency=agency, client=create_client(session)))
    orders[0].client.fullname = 'client_fullname'
    orders[0].client.non_resident_currency_payment = 'RUR'
    request = create_request(session, client=agency, orders=orders)

    contract = create_contract(session, is_signed=NOW, dt=NOW,
                               services=request.request_orders[0].order.service.id,
                               client=request.client, ctype=GENERAL, commission=ContractTypeId.COMMISSION,
                               non_resident_clients=non_resident_clients_value)
    request_matching_contracts = request.matching_contracts()
    if non_resident_clients_value:
        assert request_matching_contracts == [contract]
    else:
        assert request_matching_contracts == []


@pytest.mark.parametrize('commission_type', [ContractTypeId.COMMISSION, ContractTypeId.NON_AGENCY])
def test_commission_contract_wo_non_residents(session, commission_type):
    """Если в реквесте указан хотя бы один субклиент нерезидент и в договоре нет галки 'работа с нерезидентами',
     комиссионные договоры отфильтровываем,  остальные договоры подтягиваем как обычно"""
    agency = create_client(session, is_agency=1)
    orders = []
    for _ in range(2):
        orders.append(create_order(session, agency=agency, client=create_client(session)))
    orders[0].client.fullname = 'client_fullname'
    orders[0].client.non_resident_currency_payment = 'RUR'
    request = create_request(session, client=agency, orders=orders)

    contract = create_contract(session, is_signed=NOW, dt=NOW,
                               services=request.request_orders[0].order.service.id,
                               client=request.client, ctype=GENERAL, commission=commission_type,
                               non_resident_clients=False)
    request_matching_contracts = request.matching_contracts()
    if commission_type == ContractTypeId.COMMISSION:
        assert request_matching_contracts == []
    else:
        assert request_matching_contracts == [contract]
