# -*- coding: utf-8 -*-


import pytest

from balance import constants as cst
from balance import mapper

from tests import object_builder as ob

SHOP_PRODUCT_ID = 502655
ALTER_INVOICE_SUM = 'AlterInvoiceSum'
ALTER_INVOICE_DATE = 'AlterInvoiceDate'
ALTER_INVOICE_PAYSYS = 'AlterInvoicePaysys'
ALTER_INVOICE_PERSON = 'AlterInvoicePerson'
ALTER_INVOICE_CONTRACT = 'AlterInvoiceContract'
ALTER_ALL_INVOICES = 'AlterAllInvoices'
BILLING_SUPPORT = 'BillingSupport'
EDIT_CONTRACTS = 'EditContracts'
PATCH_INVOICE_CONTRACT = 'PatchInvoiceContract'


def check_fail_depend_on_strict(func, strict, exc, invoice, msg, **kwargs):
    if strict:
        with pytest.raises(exc) as exc_info:
            func(invoice, strict=strict, **kwargs)
        assert exc_info.value.msg == msg
    else:
        assert func(invoice, strict=strict, **kwargs) is False


def create_person(session, client, type='ph'):
    return ob.PersonBuilder(client=client, type=type).build(session).obj


def create_request(session, client, firm_id, product_ids=None):
    product_ids = product_ids or [cst.DIRECT_PRODUCT_RUB_ID]
    products = [
        ob.Getter(mapper.Product, product_id).build(session).obj
        for product_id in product_ids
    ]
    return ob.RequestBuilder(
        firm_id=firm_id,
        basket=ob.BasketBuilder(
            client=client,
            rows=[
                ob.BasketItemBuilder(
                    quantity=1,
                    order=ob.OrderBuilder(
                        client=client,
                        product=product,
                        service_id=product.engine_id,
                        manager=None,
                    )
                ) for product in products
            ]
        )
    ).build(session).obj


@pytest.fixture(name='invoice')
def create_invoice(session, person_type='ur', paysys_id=1003, firm_id=1, product_ids=None, overdraft=0, client=None,
                   **kwargs):
    client = client or ob.ClientBuilder().build(session).obj
    person = ob.PersonBuilder(client=client, type=person_type).build(session).obj
    request = create_request(session, client, firm_id, product_ids)
    return ob.InvoiceBuilder(
        request=request,
        person=person,
        paysys=ob.Getter(mapper.Paysys, paysys_id),
        overdraft=overdraft,
        **kwargs
    ).build(session).obj


def create_y_invoice(session, person_type='ur', paysys_id=1003):
    invoice = create_invoice(session, person_type=person_type, paysys_id=paysys_id)
    invoice.type = 'y_invoice'
    invoice.credit = 1
    invoice.__class__ = mapper.YInvoice
    return invoice


def create_passport(session, roles, client=None, patch_session=True):
    passport = ob.create_passport(session, *roles, patch_session=patch_session, client=client)
    return passport


def create_role(session, *perms):
    return ob.create_role(session, *perms)


def create_contract(session, client, person):
    return ob.ContractBuilder(client=client, person=person).build(session).obj


@pytest.fixture
def passport(request, session):
    perms = getattr(request, 'param', [])
    return create_passport(session, *perms)


def create_payment(session, invoice):
    return ob.YandexMoneyPaymentBuilder(invoice=invoice).build(session).obj


def create_promocode(session):
    return ob.PromoCodeGroupBuilder().build(session).obj.promocodes
