# -*- coding: utf-8 -*-

import datetime

from balance import mapper
from balance.providers.personal_acc_manager import PersonalAccountManager
from balance.constants import *
from billing.contract_iface import ContractTypeId

from tests import object_builder as ob

UR_BANK_PAYSYS_ID = 1003
UR_CARD_PAYSYS_ID = 1033
UR_CERT_PAYSYS_ID = 1006
YT_BANK_PAYSYS_ID = 1014


def create_paysys(session, group_id=0, payment_method_id=PaymentMethodIDs.bank, extern=1, cc='paysys_cc', **kwargs):
    return ob.PaysysBuilder(
        group_id=group_id,
        payment_method_id=payment_method_id,
        extern=extern,
        cc=cc,
        **kwargs
    ).build(session).obj


def create_invoice(session, qty=100,
                   paysys_id=UR_BANK_PAYSYS_ID,
                   product_id=DIRECT_PRODUCT_RUB_ID,
                   client=None,
                   person=None,
                   contract=None,
                   overdraft=0,
                   requested_type=None,
                   service_id=ServiceId.DIRECT,
                   orders=None,
                   turn_on_rows=None):
    paysys = ob.Getter(mapper.Paysys, paysys_id).build(session).obj

    if contract:
        client = contract.client
        person = contract.person
    elif person:
        client = person.client
    else:
        client = client or ob.ClientBuilder.construct(session)
        person = ob.PersonBuilder.construct(session, client=client, type=paysys.category)

    if orders:
        rows = [
            ob.BasketItemBuilder(order=order, quantity=qty)
            for order, qty in orders
        ]
    else:
        rows = [ob.BasketItemBuilder(
            order=ob.OrderBuilder(client=client, product_id=product_id, service_id=service_id),
            quantity=qty
        )]

    return ob.InvoiceBuilder(
        paysys=paysys,
        person=person,
        contract=contract,
        request=ob.RequestBuilder(
            turn_on_rows=turn_on_rows,
            basket=ob.BasketBuilder(
                client=client,
                rows=rows
            )
        ),
        overdraft=overdraft,
        type=requested_type
    ).build(session).obj


def create_charge_note_register(paysys_id,
                                person=None,
                                orders=None,
                                invoices=None,
                                contract=None,
                                single_account_number=None):
    if person:
        session = person.session
        client = person.client
    else:
        session = contract.session
        client = contract.client
        person = contract.person

    paysys = ob.Getter(mapper.Paysys, paysys_id).build(session).obj
    session.config.__dict__['ALLOW_CHARGE_NOTE_REGISTER_WO_SINGLE_ACCOUNT'] = True

    return ob.InvoiceBuilder(
        paysys=paysys,
        person=person,
        contract=contract,
        request=ob.RequestBuilder(
            basket=ob.BasketBuilder(
                client=client,
                rows=[
                    ob.BasketItemBuilder(order=order, quantity=qty)
                    for order, qty in orders or []
                ],
                register_rows=[
                    ob.BasketRegisterRowBuilder(ref_invoice=invoice)
                    for invoice in invoices or []
                ]
            )
        ),
        type='charge_note_register',
        single_account_number=single_account_number
    ).build(session).obj


def create_refund(invoice, amount, status=None, cpf=None):
    session = invoice.session
    if cpf is None:
        cpf = ob.OebsCashPaymentFactBuilder(
            invoice=invoice,
            operation_type=OebsOperationType.ONLINE,
            amount=invoice.receipt_sum or invoice.effective_sum
        ).build(session).obj
        session.expire(cpf)

    refund = ob.InvoiceRefundBuilder(
        invoice=invoice,
        payment_id=cpf.id,
        amount=amount
    ).build(session).obj
    if status:
        refund.set_status(status)
    session.flush()
    return refund


def create_invoice_transfer(session, src_invoice, dst_invoice, amount, status=InvoiceTransferStatus.exported):
    invoice_transfer = ob.InvoiceTransferBuilder(
        src_invoice=src_invoice,
        dst_invoice=dst_invoice,
        amount=amount
    ).build(session).obj
    invoice_transfer.set_status(status)
    session.flush()
    return invoice_transfer


def create_credit_contract(session, person_type='ur', **kwargs):
    client = kwargs.pop('client', ob.ClientBuilder.construct(session))
    person = kwargs.pop('person', ob.PersonBuilder.construct(session, client=client, type=person_type))
    params = dict(
        client=client,
        person=person,
        commission=ContractTypeId.NON_AGENCY,
        firm=FirmId.YANDEX_OOO,
        payment_type=POSTPAY_PAYMENT_TYPE,
        personal_account=1,
        personal_account_fictive=1,
        services={ServiceId.DIRECT},
        is_signed=datetime.datetime.now(),
        currency=NUM_CODE_RUR,
    )
    params.update(kwargs)
    return ob.ContractBuilder(
        **params
    ).build(session).obj


def create_personal_account(session,
                            paysys_id=1003,
                            contract=None,
                            service_id=ServiceId.DIRECT,
                            personal_account_fictive=None):
    paysys = ob.Getter(mapper.Paysys, paysys_id).build(session).obj
    if contract is None:
        contract = create_credit_contract(
            session,
            paysys.category,
            services={service_id},
            personal_account_fictive=personal_account_fictive
        )
    return (
        PersonalAccountManager(session)
            .for_contract(contract)
            .for_paysys(paysys)
            .get(auto_create=True)
    )


def create_product(session, price, tax_pct=20, unit_id=DAY_UNIT_ID):
    tax_policy = ob.TaxPolicyBuilder(
        tax_pcts=[tax_pct]
    ).build(session).obj
    tpp, = tax_policy.taxes
    return ob.ProductBuilder(
        taxes=[tax_policy],
        prices=[(datetime.datetime(2000, 1, 1), 'RUR', price, tpp)],
        unit=ob.Getter(mapper.ProductUnit, unit_id)
    ).build(session).obj


def create_order(client, service_id=ServiceId.DIRECT, product_id=DIRECT_PRODUCT_RUB_ID, agency=None):
    return ob.OrderBuilder(
        agency=agency,
        client=client,
        product=ob.Getter(mapper.Product, product_id),
        service_id=service_id
    ).build(client.session).obj
