# -*- coding: utf-8 -*-
import pytest
import datetime
from mock import patch

from billing.contract_iface import ContractTypeId
from balance.actions.invoice_create import InvoiceFactory

from tests.balance_tests.invoices.invoice_factory.invoice_factory_common import (create_client, create_request,
                                                                                 create_person, create_agency,
                                                                                 create_paysys, create_contract,
                                                                                 create_service,
                                                                                 create_currency, create_firm,
                                                                                 create_order,
                                                                                 ACQUIRING, AFISHA, DISTRIBUTION,
                                                                                 GENERAL, GEOCONTEXT, PARTNERS,
                                                                                 PREFERRED_DEAL, SPENDABLE,
                                                                                 NOW, YESTERDAY)


@patch('balance.mapper.contracts.Contract.match')
def test_is_contract_match_called(match_request_mock, session, firm, client):
    paysys = create_paysys(session)
    request = create_request(session, client=client)
    create_contract(session, is_signed=NOW, dt=NOW, services=request.request_orders[0].order.service.id,
                    client=request.client, ctype=GENERAL)
    invoice = InvoiceFactory(request=request, firm=firm, paysys=paysys, invoice_kwargs={}).construct_prepayment()
    invoice.matching_contracts()
    assert match_request_mock.call_count == 1


def test_w_partly_matching_contract(session, client, firm):
    """Если у клиента несколько договоров и некотрые из них не соответствуют счету, возвращаем только оставшиеся"""
    paysys = create_paysys(session)
    request = create_request(session, client)
    contracts = []
    for is_cancelled_value in [NOW, None]:
        contracts.append(create_contract(session, is_signed=NOW, dt=NOW, is_cancelled=is_cancelled_value,
                                         services=request.request_orders[0].order.service.id,
                                         client=request.client, ctype=GENERAL,
                                         commission=ContractTypeId.WITHOUT_PARTICIPATION
                                         ))
    invoice = InvoiceFactory(request=request, firm=firm, paysys=paysys, invoice_kwargs={}).construct_prepayment()
    assert invoice.matching_contracts() == [contracts[1]]


@pytest.mark.parametrize('non_resident_clients_value', [True, False])
def test_commission_contract_with_non_residents(session, agency, firm,service, paysys, non_resident_clients_value):
    orders = []
    person = create_person(session, agency)
    for _ in range(2):
        orders.append(create_order(session, agency=agency, client=create_client(session), service=service))
    orders[0].client.fullname = 'client_fullname'
    orders[0].client.non_resident_currency_payment = 'RUR'
    request = create_request(session, client=agency, orders=orders)

    contract = create_contract(session, is_signed=NOW, dt=NOW, person=person,
                               services=request.request_orders[0].order.service.id,
                               client=request.client, ctype=GENERAL, commission=ContractTypeId.COMMISSION,
                               non_resident_clients=non_resident_clients_value)
    invoice = InvoiceFactory(request=request, firm=firm, paysys=paysys,
                             person=person, invoice_kwargs={}).construct_prepayment()
    invoice_matching_contracts = invoice.matching_contracts()
    assert invoice_matching_contracts == [contract]


@pytest.mark.parametrize('commission_type', [ContractTypeId.COMMISSION, ContractTypeId.NON_AGENCY])
def test_commission_contract_wo_non_residents(session, agency, paysys, firm, service, commission_type):
    orders = []
    person = create_person(session, agency)
    for _ in range(2):
        orders.append(create_order(session, agency=agency, client=create_client(session),
                                   service=service))
    orders[0].client.fullname = 'client_fullname'
    orders[0].client.non_resident_currency_payment = 'RUR'
    request = create_request(session, client=agency, orders=orders)

    contract = create_contract(session, is_signed=NOW, dt=NOW, person=person,
                               services=request.request_orders[0].order.service.id,
                               client=request.client, ctype=GENERAL, commission=commission_type,
                               non_resident_clients=False)
    invoice = InvoiceFactory(request=request, firm=firm, paysys=paysys, person=person,
                             invoice_kwargs={}).construct_prepayment()
    invoice_matching_contracts = invoice.matching_contracts()

    assert invoice_matching_contracts == [contract]


@pytest.mark.parametrize('is_suspended', [NOW, None])
@pytest.mark.parametrize('commission', [ContractTypeId.NON_AGENCY,
                                        ContractTypeId.WITHOUT_PARTICIPATION,
                                        ContractTypeId.OFD_WITHOUT_PARTICIPATION])
def test_wo_participation(session, client, commission, firm, paysys, is_suspended):
    request = create_request(session, client=client)
    person = create_person(session, client)
    contract = create_contract(session, is_signed=NOW, dt=NOW, commission=commission,
                               person=create_person(session, client), is_suspended=is_suspended,
                               services=request.request_orders[0].order.service.id, client=request.client,
                               ctype=GENERAL)
    invoice = InvoiceFactory(request=request, firm=firm, paysys=paysys, person=person,
                             invoice_kwargs={}).construct_prepayment()
    invoice_matching_contracts = invoice.matching_contracts()
    if (commission in [ContractTypeId.WITHOUT_PARTICIPATION,
                       ContractTypeId.OFD_WITHOUT_PARTICIPATION]):
        assert invoice_matching_contracts == [contract]
    else:
        assert invoice_matching_contracts == []


@pytest.mark.parametrize('is_suspended', [NOW, None])
@pytest.mark.parametrize('commission', [ContractTypeId.NON_AGENCY,
                                        ContractTypeId.WITHOUT_PARTICIPATION,
                                        ContractTypeId.OFD_WITHOUT_PARTICIPATION])
def test_is_suspended(session, client, firm, paysys, commission, is_suspended):
    request = create_request(session, client=client)
    person = create_person(session, client)
    contract = create_contract(session, is_signed=NOW, dt=NOW, commission=commission,
                               person=person, is_suspended=is_suspended,
                               services=request.request_orders[0].order.service.id, client=request.client,
                               ctype=GENERAL)
    invoice = InvoiceFactory(request=request, firm=firm, paysys=paysys, person=person,
                             invoice_kwargs={}).construct_prepayment()
    invoice_matching_contracts = invoice.matching_contracts()
    assert invoice_matching_contracts == [contract]


@pytest.mark.parametrize('contract_type',
                         [ACQUIRING, AFISHA, DISTRIBUTION, GENERAL, GEOCONTEXT, PARTNERS, PREFERRED_DEAL, SPENDABLE])
def test_contract_type(session, client, contract_type, firm, paysys):
    request = create_request(session, client=client)
    person = create_person(session, client)
    contract = create_contract(session, is_signed=NOW, dt=NOW, commission=ContractTypeId.NON_AGENCY,
                               services=request.request_orders[0].order.service.id, person=person,
                               client=request.client, ctype=contract_type)
    invoice = InvoiceFactory(request=request, firm=firm, paysys=paysys, person=person,
                             invoice_kwargs={}).construct_prepayment()
    invoice_matching_contracts = invoice.matching_contracts()
    if contract_type == GENERAL:
        assert invoice_matching_contracts == [contract]
    else:
        assert invoice_matching_contracts == []


@pytest.mark.parametrize('is_cancelled', [None, NOW])
def test_cancelled_contract(session, client, firm, paysys, is_cancelled):
    """аннулированный договор не соответствует счету"""
    request = create_request(session, client)
    person = create_person(session, client)
    contract = create_contract(session, is_signed=NOW, dt=NOW, is_cancelled=is_cancelled,
                               commission=ContractTypeId.NON_AGENCY, person=person,
                               services=request.request_orders[0].order.service.id,
                               client=request.client, ctype=GENERAL)
    invoice = InvoiceFactory(request=request, firm=firm, paysys=paysys, person=person,
                             invoice_kwargs={}).construct_prepayment()
    invoice_matching_contracts = invoice.matching_contracts()
    if is_cancelled:
        assert invoice_matching_contracts == []
    else:
        assert invoice_matching_contracts == [contract]


@pytest.mark.parametrize('dt_in_future', [1, 0])
def test_future_dt_contract(session, client, firm, paysys, dt_in_future):
    """договор с датой в будущем (даже если подписан раньше даты проверки) не соответствует счету"""
    request = create_request(session, client)
    person = create_person(session, client)
    contract = create_contract(session, is_signed=NOW,
                               dt=NOW + datetime.timedelta(days=1) if dt_in_future else NOW,
                               commission=ContractTypeId.NON_AGENCY, person=person,
                               services=request.request_orders[0].order.service.id,
                               client=request.client, ctype=GENERAL)
    invoice = InvoiceFactory(request=request, firm=firm, paysys=paysys, person=person,
                             invoice_kwargs={}).construct_prepayment()
    invoice_matching_contracts = invoice.matching_contracts()
    if dt_in_future:
        assert invoice_matching_contracts == []
    else:
        assert invoice_matching_contracts == [contract]


@pytest.mark.parametrize('finish_dt', [NOW, NOW - datetime.timedelta(seconds=1), NOW + datetime.timedelta(days=1)])
def test_finish_dt_contract(session, client, firm, paysys, finish_dt):
    """если дата конца договора указана, договор соответствует счету, пока дата проверки меньше, чем дата
     конца договора"""
    request = create_request(session, client)
    person = create_person(session, client)
    contract = create_contract(session, is_signed=YESTERDAY, dt=YESTERDAY, finish_dt=finish_dt,
                               commission=ContractTypeId.NON_AGENCY, person=person,
                               services=request.request_orders[0].order.service.id,
                               client=request.client, ctype=GENERAL)
    invoice = InvoiceFactory(request=request, firm=firm, paysys=paysys, person=person,
                             invoice_kwargs={}).construct_prepayment()
    invoice_matching_contracts = invoice.matching_contracts()
    if finish_dt <= NOW:
        assert invoice_matching_contracts == []
    else:
        assert invoice_matching_contracts == [contract]


@pytest.mark.parametrize('services_in_request, services_in_contract',
                         [({7, 11}, {11}),
                          ({11}, {11}),
                          ({11}, {11, 7})])
def test_w_service(session, client, firm, paysys, services_in_request, services_in_contract):
    """договор соответствует счету, если все сервисы из реквеста указаны в нем"""
    orders = []
    for service_id in services_in_request:
        orders.append(create_order(session, client=client, service_id=service_id))
    person = create_person(session, client)
    request = create_request(session, client=client, orders=orders)
    contract = create_contract(session, is_signed=YESTERDAY, dt=YESTERDAY, person=person,
                               commission=ContractTypeId.NON_AGENCY,
                               services=request.request_orders[0].order.service.id,
                               client=request.client, ctype=GENERAL)

    invoice = InvoiceFactory(request=request, firm=firm, paysys=paysys, person=person,
                             invoice_kwargs={}).construct_prepayment()
    invoice_matching_contracts = invoice.matching_contracts()
    if services_in_request.issubset(services_in_contract):
        assert invoice_matching_contracts == [contract]
    else:
        assert invoice_matching_contracts == []
