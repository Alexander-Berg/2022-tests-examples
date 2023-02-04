import pytest
import datetime

from balance.actions.invoice_create import InvoiceFactory
from billing.contract_iface import ContractTypeId
from balance.constants import POSTPAY_PAYMENT_TYPE, PREPAY_PAYMENT_TYPE
from invoice_factory_common import (create_client, create_request, create_person,
                                    create_paysys, create_contract, create_service,
                                    create_currency, create_firm, create_order)

NOW = datetime.datetime.now()
ONE_TIME_SALE = 35


def test_y_invoice(session):
    i_f = InvoiceFactory(requested_type='y_invoice')
    assert i_f.invoice_type == 'y_invoice'


def test_charge_note(session):
    i_f = InvoiceFactory(requested_type='charge_note')
    assert i_f.invoice_type == 'charge_note'


def test_request_desired_type_charge_note(session, client):
    request = create_request(session, client, invoice_desired_type='charge_note')
    i_f = InvoiceFactory(request=request)
    assert i_f.invoice_type == 'charge_note'


@pytest.mark.parametrize('requested_type', ['prepayment',
                                            'personal_account',
                                            'personal_account_fictive',
                                            'fictive_personal_account',
                                            'fictive',
                                            'repayment',
                                            'bonus_account'])
def test_charge_note_auto(session, client, requested_type, firm, service, currency):
    paysys = create_paysys(session, certificate=0)
    person = create_person(session, client=client)
    contract = create_contract(session, commission=ContractTypeId.NON_AGENCY, firm=firm.id,
                               client=client, payment_type=PREPAY_PAYMENT_TYPE, personal_account=True,
                               services={service.id}, is_signed=NOW, person=person, currency=currency.num_code)
    i_f = InvoiceFactory(requested_type=requested_type, postpay=0, paysys=paysys, firm=firm,
                         contract=contract)
    if requested_type != 'prepayment':
        assert i_f.invoice_type == 'charge_note'
    else:
        assert i_f.invoice_type == 'prepayment'


@pytest.mark.parametrize('requested_type', ['personal_account',
                                            'personal_account_fictive',
                                            'fictive_personal_account',
                                            'fictive',
                                            'repayment',
                                            'bonus_account'])
@pytest.mark.parametrize('postpay', [0, 1])
def test_charge_note_auto_postpay(session, client, requested_type, firm, service, currency, postpay):
    paysys = create_paysys(session, certificate=0)
    person = create_person(session, client=client)
    contract = create_contract(session, commission=ContractTypeId.NON_AGENCY, firm=firm.id,
                               client=client, payment_type=PREPAY_PAYMENT_TYPE, personal_account=True,
                               services={service.id}, is_signed=NOW, person=person, currency=currency.num_code)
    i_f = InvoiceFactory(requested_type=requested_type, postpay=postpay, paysys=paysys, firm=firm,
                         contract=contract)
    if postpay == 0:
        assert i_f.invoice_type == 'charge_note'
    elif postpay == 1:
        assert i_f.invoice_type == 'personal_account'
    else:
        assert i_f.invoice_type == 'fictive_personal_account'


@pytest.mark.parametrize('requested_type', ['personal_account',
                                            'personal_account_fictive',
                                            'fictive_personal_account',
                                            'fictive',
                                            'repayment',
                                            'bonus_account'])
@pytest.mark.parametrize('certificate', [0, 1])
def test_charge_note_auto_certificate(session, client, requested_type, firm, service, currency, certificate):
    paysys = create_paysys(session, certificate=certificate)
    person = create_person(session, client=client)
    contract = create_contract(session, commission=ContractTypeId.NON_AGENCY, firm=firm.id,
                               client=client, payment_type=PREPAY_PAYMENT_TYPE, personal_account=True,
                               services={service.id}, is_signed=NOW, person=person, currency=currency.num_code)
    i_f = InvoiceFactory(requested_type=requested_type, postpay=0, paysys=paysys, firm=firm,
                         contract=contract)
    if certificate == 0:
        assert i_f.invoice_type == 'charge_note'
    elif requested_type == 'bonus_account':
        assert i_f.invoice_type == 'bonus_account'
    else:
        assert i_f.invoice_type == 'prepayment'


@pytest.mark.parametrize('requested_type', ['personal_account',
                                            'personal_account_fictive',
                                            'fictive_personal_account',
                                            'fictive',
                                            'repayment',
                                            'bonus_account'])
@pytest.mark.parametrize('w_contract', [0, 1])
def test_charge_note_auto_w_contract(session, client, requested_type, firm, service, currency, w_contract):
    paysys = create_paysys(session, certificate=0)
    person = create_person(session, client=client)
    if w_contract:
        contract = create_contract(session, commission=ContractTypeId.NON_AGENCY, firm=firm.id,
                                   client=client, payment_type=PREPAY_PAYMENT_TYPE, personal_account=True,
                                   services={service.id}, is_signed=NOW, person=person, currency=currency.num_code)
    else:
        contract = None
    i_f = InvoiceFactory(
        person=person,
        paysys=paysys,
        contract=contract,
        requested_type=requested_type,
        postpay=0,
        firm=firm,
    )
    if w_contract:
        assert i_f.invoice_type == 'charge_note'
    elif requested_type == 'bonus_account':
        assert i_f.invoice_type == 'bonus_account'
    else:
        assert i_f.invoice_type == 'prepayment'


@pytest.mark.parametrize('requested_type', ['personal_account',
                                            'personal_account_fictive',
                                            'fictive_personal_account',
                                            'fictive',
                                            'repayment',
                                            'bonus_account'])
@pytest.mark.parametrize('payment_type', [PREPAY_PAYMENT_TYPE, POSTPAY_PAYMENT_TYPE])
def test_charge_note_auto_payment_type(session, client, requested_type, firm, service, currency, payment_type):
    paysys = create_paysys(session, certificate=0)
    person = create_person(session, client=client)
    contract = create_contract(session, commission=ContractTypeId.NON_AGENCY, firm=firm.id,
                               client=client, payment_type=payment_type, personal_account=True,
                               services={service.id}, is_signed=NOW, person=person, currency=currency.num_code)

    i_f = InvoiceFactory(requested_type=requested_type, postpay=0, paysys=paysys, firm=firm,
                         contract=contract)
    if payment_type == PREPAY_PAYMENT_TYPE:
        assert i_f.invoice_type == 'charge_note'
    elif requested_type == 'bonus_account':
        assert i_f.invoice_type == 'bonus_account'
    else:
        assert i_f.invoice_type == 'prepayment'


@pytest.mark.parametrize('requested_type', ['personal_account',
                                            'personal_account_fictive',
                                            'fictive_personal_account',
                                            'fictive',
                                            'repayment',
                                            'bonus_account'])
@pytest.mark.parametrize('personal_account', [True, False])
def test_charge_note_auto_personal_account(session, client, requested_type, firm, service, currency, personal_account):
    paysys = create_paysys(session, certificate=0)
    person = create_person(session, client=client)
    contract = create_contract(session, commission=ContractTypeId.NON_AGENCY, firm=firm.id,
                               client=client, payment_type=PREPAY_PAYMENT_TYPE, personal_account=personal_account,
                               services={service.id}, is_signed=NOW, person=person, currency=currency.num_code)

    i_f = InvoiceFactory(requested_type=requested_type, postpay=0, paysys=paysys, firm=firm,
                         contract=contract)
    if personal_account:
        assert i_f.invoice_type == 'charge_note'
    elif requested_type == 'bonus_account':
        assert i_f.invoice_type == 'bonus_account'
    else:
        assert i_f.invoice_type == 'prepayment'


@pytest.mark.parametrize('postpay', [0, 1, 2])
def test_prepayment_auto(session, client, firm, service, currency, postpay):
    paysys = create_paysys(session, certificate=0)
    person = create_person(session, client=client)
    contract = create_contract(session, commission=ContractTypeId.NON_AGENCY, firm=firm.id,
                               client=client, payment_type=PREPAY_PAYMENT_TYPE, personal_account=True,
                               personal_account_fictive=False, services={service.id}, is_signed=NOW, person=person,
                               currency=currency.num_code)
    request = create_request(session, client, orders=[create_order(session, client, service_id=ONE_TIME_SALE)])
    i_f = InvoiceFactory(postpay=postpay, paysys=paysys, firm=firm,
                         contract=contract, request=request)
    if postpay == 1:
        assert i_f.invoice_type == 'prepayment'
    elif postpay == 0:
        assert i_f.invoice_type == 'charge_note'
    else:
        assert i_f.invoice_type == 'fictive_personal_account'


@pytest.mark.parametrize('w_shop', [True, False])
def test_prepayment_w_shop(session, client, firm, service, currency, w_shop):
    paysys = create_paysys(session, certificate=0)
    person = create_person(session, client=client)
    contract = create_contract(session, commission=ContractTypeId.NON_AGENCY, firm=firm.id,
                               client=client, payment_type=PREPAY_PAYMENT_TYPE, personal_account=True,
                               personal_account_fictive=False, services={service.id}, is_signed=NOW, person=person,
                               currency=currency.num_code)
    request = create_request(session, client, orders=[create_order(session, client,
                                                                   service_id=ONE_TIME_SALE if w_shop else None)])
    i_f = InvoiceFactory(postpay=1, paysys=paysys, firm=firm,
                         contract=contract, request=request)
    if w_shop:
        assert i_f.invoice_type == 'prepayment'
    else:
        assert i_f.invoice_type == 'personal_account'


@pytest.mark.parametrize('personal_account_fictive', [True, False])
def test_prepayment_personal_account_fictive(session, client, firm, service, currency, personal_account_fictive):
    paysys = create_paysys(session, certificate=0)
    person = create_person(session, client=client)
    contract = create_contract(session, commission=ContractTypeId.NON_AGENCY, firm=firm.id,
                               client=client, payment_type=PREPAY_PAYMENT_TYPE, personal_account=True,
                               personal_account_fictive=personal_account_fictive, services={service.id}, is_signed=NOW,
                               person=person, currency=currency.num_code)
    request = create_request(session, client, orders=[create_order(session, client,
                                                                   service_id=ONE_TIME_SALE)])
    i_f = InvoiceFactory(postpay=1, paysys=paysys, firm=firm,
                         contract=contract, request=request)
    if personal_account_fictive:
        assert i_f.invoice_type == 'personal_account'
    else:
        assert i_f.invoice_type == 'prepayment'


@pytest.mark.parametrize('credit', [0, 1, 2])
def test_credit(session, client, firm, service, currency, credit):
    paysys = create_paysys(session, certificate=0)
    person = create_person(session, client=client)
    contract = create_contract(session, commission=ContractTypeId.NON_AGENCY, firm=firm.id,
                               client=client, payment_type=POSTPAY_PAYMENT_TYPE, personal_account=True,
                               personal_account_fictive=False, services={service.id}, is_signed=NOW,
                               person=person, currency=currency.num_code)
    request = create_request(session, client)
    i_f = InvoiceFactory(postpay=0, paysys=paysys, firm=firm, credit=credit, contract=contract, request=request)
    if credit == 1:
        assert i_f.invoice_type == 'repayment'
    elif credit == 2:
        assert i_f.invoice_type == 'fictive'
    else:
        assert i_f.invoice_type == 'prepayment'


@pytest.mark.parametrize('overdraft', [0, 1, 2])
def test_overdraft(session, client, firm, service, currency, overdraft):
    paysys = create_paysys(session, certificate=0)
    request = create_request(session, client)
    i_f = InvoiceFactory(postpay=0, paysys=paysys, firm=firm, credit=0, contract=None, overdraft=overdraft,
                         request=request)
    if overdraft == 1:
        assert i_f.invoice_type == 'overdraft'
    else:
        assert i_f.invoice_type == 'prepayment'


def test_bonus_account(session, client, firm, service, currency):
    paysys = create_paysys(session, certificate=0)
    request = create_request(session, client)
    i_f = InvoiceFactory(postpay=0, paysys=paysys, firm=firm, credit=0, contract=None, overdraft=0,
                         request=request, requested_type='bonus_account')
    assert i_f.invoice_type == 'bonus_account'

