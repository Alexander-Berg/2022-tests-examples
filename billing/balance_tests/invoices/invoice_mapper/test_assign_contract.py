# -*- coding: utf-8 -*-
import pytest
import datetime

from balance.exc import CANNOT_ASSIGN_CONTRACT_TO_OVERDRAFT, ILLEGAL_CONTRACT
from balance.actions.invoice_create import InvoiceFactory
from billing.contract_iface import ContractTypeId
from balance.constants import POSTPAY_PAYMENT_TYPE
from tests.balance_tests.invoices.invoice_factory.invoice_factory_common import (create_client, create_request,
                                                                                 create_person,
                                                                                 create_paysys, create_contract,
                                                                                 create_service,
                                                                                 create_currency, create_firm,
                                                                                 create_order, ACQUIRING, AFISHA,
                                                                                 DISTRIBUTION, GENERAL, GEOCONTEXT,
                                                                                 PARTNERS, PREFERRED_DEAL, SPENDABLE,
                                                                                 NOW, YESTERDAY)


def test_overdraft(session, client, firm, currency):
    person = create_person(session, client=client)
    paysys = create_paysys(session)
    request = create_request(session, client=client)
    contract = create_contract(session, commission=ContractTypeId.NON_AGENCY, firm=firm.id,
                               client=client, payment_type=POSTPAY_PAYMENT_TYPE,
                               services={request.request_orders[0].order.service.id}, is_signed=NOW, person=person,
                               currency=currency.num_code)
    invoice = InvoiceFactory(request=request, firm=firm, paysys=paysys, invoice_kwargs={}).construct_overdraft()
    with pytest.raises(CANNOT_ASSIGN_CONTRACT_TO_OVERDRAFT):
        invoice.assign_contract(contract)
    assert invoice.contract is None


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

    if (commission in [ContractTypeId.WITHOUT_PARTICIPATION,
                       ContractTypeId.OFD_WITHOUT_PARTICIPATION]):
        invoice.assign_contract(contract)
        assert invoice.contract == contract
    else:
        with pytest.raises(ILLEGAL_CONTRACT):
            invoice.assign_contract(contract)


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
    invoice.assign_contract(contract)
    assert invoice.contract == contract


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
    if contract_type == GENERAL:
        invoice.assign_contract(contract)
        assert invoice.contract == contract
    else:
        with pytest.raises(ILLEGAL_CONTRACT):
            invoice.assign_contract(contract)


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
    if not is_cancelled:
        invoice.assign_contract(contract)
        assert invoice.contract == contract
    else:
        with pytest.raises(ILLEGAL_CONTRACT):
            invoice.assign_contract(contract)


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

    if dt_in_future:
        with pytest.raises(ILLEGAL_CONTRACT):
            invoice.assign_contract(contract)
    else:
        invoice.assign_contract(contract)
        assert invoice.contract == contract


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

    if finish_dt <= NOW:
        with pytest.raises(ILLEGAL_CONTRACT):
            invoice.assign_contract(contract)
    else:
        invoice.assign_contract(contract)
        assert invoice.contract == contract


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

    if services_in_request.issubset(services_in_contract):
        invoice.assign_contract(contract)
        assert invoice.contract == contract
    else:
        with pytest.raises(ILLEGAL_CONTRACT):
            invoice.assign_contract(contract)
