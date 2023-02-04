# -*- coding: utf-8 -*-
import pytest
import datetime

from balance.mapper import Service
from billing.contract_iface import ContractTypeId
from tests import object_builder as ob
from tests.balance_tests.contract.contract_common import (create_contract,
                             create_order,
                             create_request,
                             create_person,
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
def test_contract_type(session, client, contract_type):
    request = create_request(session, client=client)
    person = create_person(session, client)
    contract = create_contract(session, is_signed=NOW, dt=NOW, commission=ContractTypeId.NON_AGENCY,
                               services=request.request_orders[0].order.service.id, person=person,
                               client=request.client, ctype=contract_type)
    is_contract_match_invoice = contract.match(request.client, person, NOW,
                                               {request.request_orders[0].order.service.id})
    if contract_type == GENERAL:
        assert is_contract_match_invoice is True
    else:
        assert is_contract_match_invoice is False


@pytest.mark.parametrize('is_cancelled', [None, NOW])
def test_cancelled_contract(session, client, is_cancelled):
    """аннулированный договор не соответствует счету"""
    request = create_request(session, client)
    person = create_person(session, client)
    contract = create_contract(session, is_signed=NOW, dt=NOW, is_cancelled=is_cancelled,
                               commission=ContractTypeId.NON_AGENCY, person=person,
                               services=request.request_orders[0].order.service.id,
                               client=request.client, ctype=GENERAL)
    is_contract_match_invoice = contract.match(request.client, person, NOW,
                                               {request.request_orders[0].order.service.id})
    if is_cancelled:
        assert is_contract_match_invoice is False
    else:
        assert is_contract_match_invoice is True


@pytest.mark.parametrize('dt_in_future', [1, 0])
def test_future_dt_contract(session, client, dt_in_future):
    """договор с датой в будущем (даже если подписан раньше даты проверки) не соответствует счету"""
    request = create_request(session, client)
    person = create_person(session, client)
    contract = create_contract(session, is_signed=NOW,
                               dt=NOW + datetime.timedelta(minutes=1) if dt_in_future else NOW,
                               commission=ContractTypeId.NON_AGENCY, person=person,
                               services=request.request_orders[0].order.service.id,
                               client=request.client, ctype=GENERAL)
    is_contract_match_invoice = contract.match(request.client, person, NOW,
                                               {request.request_orders[0].order.service.id})
    if dt_in_future:
        assert is_contract_match_invoice is False
    else:
        assert is_contract_match_invoice is True


@pytest.mark.parametrize('finish_dt', [NOW, NOW - datetime.timedelta(seconds=1), NOW + datetime.timedelta(days=1)])
def test_finish_dt_contract(session, client, finish_dt):
    """если дата конца договора указана, договор соответствует счету, пока дата проверки меньше, чем дата
     конца договора"""
    request = create_request(session, client)
    person = create_person(session, client)
    contract = create_contract(session, is_signed=YESTERDAY, dt=YESTERDAY, finish_dt=finish_dt,
                               commission=ContractTypeId.NON_AGENCY, person=person,
                               services=request.request_orders[0].order.service.id,
                               client=request.client, ctype=GENERAL)
    is_contract_match_invoice = contract.match(request.client, person, NOW,
                                               {request.request_orders[0].order.service.id})
    if finish_dt <= NOW:
        assert is_contract_match_invoice is False
    else:
        assert is_contract_match_invoice is True


@pytest.mark.parametrize('services_in_request, services_in_contract, is_contract_match_expected',
                         [((7, 11), (11,), False),
                          ((11,), (11,), True),
                          ((11,), (11, 7), True)])
def test_w_service(session, client, services_in_request, services_in_contract,
                   is_contract_match_expected):
    """договор соответствует счету, если все сервисы из реквеста указаны в нем"""
    orders = []
    for service_id in services_in_request:
        orders.append(create_order(session, client=client, service=ob.Getter(Service, service_id)))
    person = create_person(session, client)
    request = create_request(session, client=client, orders=orders)
    contract = create_contract(session, is_signed=YESTERDAY, dt=YESTERDAY, person=person,
                               commission=ContractTypeId.NON_AGENCY,
                               services=request.request_orders[0].order.service.id,
                               client=request.client, ctype=GENERAL)

    is_contract_match_invoice = contract.match(request.client, person, NOW,
                                               {request_order.order.service.id for request_order in
                                                request.request_orders})
    assert is_contract_match_invoice == is_contract_match_expected


@pytest.mark.parametrize('strict', [True, False])
@pytest.mark.parametrize('is_suspended', [NOW, None])
@pytest.mark.parametrize('commission', [ContractTypeId.NON_AGENCY,
                                        ContractTypeId.WITHOUT_PARTICIPATION,
                                        ContractTypeId.OFD_WITHOUT_PARTICIPATION])
def test_wo_participation(session, client, commission, is_suspended, strict):
    request = create_request(session, client=client)
    contract = create_contract(session, is_signed=NOW, dt=NOW, commission=commission,
                               person=create_person(session, client), is_suspended=is_suspended,
                               services=request.request_orders[0].order.service.id, client=request.client,
                               ctype=GENERAL)
    is_contract_match_invoice = contract.match(request.client, create_person(session, client), NOW,
                                               {request.request_orders[0].order.service.id}, strict=strict)
    if (commission in [ContractTypeId.WITHOUT_PARTICIPATION,
                       ContractTypeId.OFD_WITHOUT_PARTICIPATION]
            and not (is_suspended and strict)):
        assert is_contract_match_invoice is True
    else:
        assert is_contract_match_invoice is False


@pytest.mark.parametrize('strict', [True, False])
@pytest.mark.parametrize('is_suspended', [NOW, None])
@pytest.mark.parametrize('commission', [ContractTypeId.NON_AGENCY,
                                        ContractTypeId.WITHOUT_PARTICIPATION,
                                        ContractTypeId.OFD_WITHOUT_PARTICIPATION])
def test_is_suspended(session, client, commission, is_suspended, strict):
    request = create_request(session, client=client)
    person = create_person(session, client)
    contract = create_contract(session, is_signed=NOW, dt=NOW, commission=commission,
                               person=person, is_suspended=is_suspended,
                               services=request.request_orders[0].order.service.id, client=request.client,
                               ctype=GENERAL)
    is_contract_match_invoice = contract.match(request.client, person, NOW,
                                               {request.request_orders[0].order.service.id}, strict=strict)
    if not (is_suspended and strict):
        assert is_contract_match_invoice is True
    else:
        assert is_contract_match_invoice is False
