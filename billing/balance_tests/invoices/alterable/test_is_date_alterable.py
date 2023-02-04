# -*- coding: utf-8 -*-
import pytest
import datetime
from balance import exc
from balance import constants as cst
from balance import mapper
from balance.providers.invoice_alterable import is_date_alterable
from tests.balance_tests.invoices.alterable.alterable_common import (
    create_passport,
    create_invoice,
    create_role,
    create_promocode,
    SHOP_PRODUCT_ID,
    ALTER_INVOICE_DATE,
    check_fail_depend_on_strict,
)
from tests.balance_tests.invoices.invoice_common import (
    create_charge_note_register,
)


@pytest.mark.parametrize('strict', [False, True])
def test_perm(session, invoice, strict):
    create_passport(session, [create_role(session, ALTER_INVOICE_DATE)])
    assert is_date_alterable(invoice, strict=strict) is True


@pytest.mark.parametrize('strict', [False, True])
def test_client_owner(session, invoice, strict):
    passport = create_passport(session, [])
    passport.client = invoice.client
    session.flush()
    check_fail_depend_on_strict(func=is_date_alterable, strict=strict,
                                invoice=invoice, exc=exc.PERMISSION_DENIED,
                                msg='User {} has no permission AlterInvoiceDate.'.format(
                                    passport.passport_id))


@pytest.mark.parametrize('strict', [False, True])
@pytest.mark.parametrize('firm_id',
                         [
                             pytest.param(cst.FirmId.YANDEX_OOO),
                             pytest.param(cst.FirmId.TAXI),
                         ]
                         )
def test_perm_firm(session, invoice, firm_id, strict):
    role = create_role(session, (ALTER_INVOICE_DATE, {cst.ConstraintTypes.firm_id: None}))
    passport = create_passport(session, [(role, firm_id)])
    if invoice.firm.id == firm_id:
        assert is_date_alterable(invoice, strict=strict) is True
    else:
        check_fail_depend_on_strict(func=is_date_alterable, strict=strict,
                                    invoice=invoice, exc=exc.PERMISSION_DENIED,
                                    msg='User {} has no permission AlterInvoiceDate.'.format(
                                        passport.passport_id))


@pytest.mark.parametrize('strict', [False, True])
def test_hidden(session, invoice, strict):
    create_passport(session, [create_role(session, ALTER_INVOICE_DATE)])
    invoice.hidden = 2
    session.flush()
    check_fail_depend_on_strict(func=is_date_alterable, strict=strict,
                                invoice=invoice, exc=exc.INVALID_PARAM,
                                msg="Invalid parameter for function: Can't alter invoice")


@pytest.mark.parametrize('strict', [False, True])
def test_payed(session, invoice, strict):
    create_passport(session, [create_role(session, ALTER_INVOICE_DATE)])
    invoice.create_receipt(666)
    check_fail_depend_on_strict(func=is_date_alterable, strict=strict,
                                invoice=invoice, exc=exc.INVALID_PARAM,
                                msg="Invalid parameter for function: Can't alter date in payed invoice")


@pytest.mark.parametrize('strict', [False, True])
def test_payed_returned(session, invoice, strict):
    create_passport(session, [create_role(session, ALTER_INVOICE_DATE)])
    invoice.create_receipt(666)
    invoice.create_receipt(-666)

    assert is_date_alterable(invoice, strict=strict) is True


@pytest.mark.parametrize('strict', [False, True])
def test_payed_shop(session, strict):
    create_passport(session, [create_role(session, ALTER_INVOICE_DATE)])
    invoice = create_invoice(session, product_ids=[SHOP_PRODUCT_ID])
    invoice.create_receipt(666)
    assert is_date_alterable(invoice, strict=strict) is True


@pytest.mark.parametrize('strict', [False, True])
@pytest.mark.parametrize('loaded, product_id',
                         [
                             pytest.param(False, cst.DIRECT_PRODUCT_RUB_ID, id='direct_unloaded'),
                             pytest.param(True, cst.DIRECT_PRODUCT_RUB_ID, id='direct_loaded'),
                             pytest.param(True, SHOP_PRODUCT_ID, id='shop'),
                         ]
                         )
def test_completions(session, product_id, strict, loaded):
    create_passport(session, [create_role(session, ALTER_INVOICE_DATE)])
    invoice = create_invoice(session, product_ids=[SHOP_PRODUCT_ID])
    invoice.turn_on_rows()
    consume, = invoice.consumes
    order = consume.order
    order.calculate_consumption(datetime.datetime.now(), {order.shipment_type: 1})
    session.flush()

    if not loaded:
        session.expire_all()
    assert invoice.consumes_loaded is loaded
    check_fail_depend_on_strict(func=is_date_alterable, strict=strict,
                                invoice=invoice, exc=exc.INVALID_PARAM,
                                msg="Invalid parameter for function: Can't change date for invoice with any completions")


@pytest.mark.parametrize('strict', [False, True])
def test_repayment(session, invoice, strict):
    create_passport(session, [create_role(session, ALTER_INVOICE_DATE)])
    invoice.type = 'repayment'
    invoice.credit = 1
    invoice.__class__ = mapper.RepaymentInvoice
    session.flush()
    session.expire_all()
    check_fail_depend_on_strict(func=is_date_alterable, strict=strict,
                                invoice=invoice, exc=exc.INVALID_PARAM,
                                msg="Invalid parameter for function: Can't alter invoice")


@pytest.mark.parametrize('product_id',
                         [
                             cst.DIRECT_PRODUCT_RUB_ID,
                             cst.MARKET_FISH_PRODUCT_ID,
                             SHOP_PRODUCT_ID,
                             cst.GEOCON_PRODUCT_ID]
                         )
@pytest.mark.parametrize('strict', [False, True])
def test_services(session, product_id, strict):
    create_passport(session, [create_role(session, ALTER_INVOICE_DATE)])
    invoice = create_invoice(session, product_ids=[product_id])
    if product_id != cst.GEOCON_PRODUCT_ID:
        assert is_date_alterable(invoice, strict=strict) is True
    else:
        check_fail_depend_on_strict(func=is_date_alterable, strict=strict,
                                    invoice=invoice, exc=exc.INVALID_PARAM,
                                    msg="Invalid parameter for function: Can't change invoice"
                                        " date with such service_ids: {}".format({cst.ServiceId.GEOCON}))


@pytest.mark.charge_note_register
@pytest.mark.parametrize('strict', [False, True])
def test_charge_note_register(session, invoice, strict):
    charge_note = create_charge_note_register(invoice.paysys_id, invoice.person, invoices=[invoice])
    create_passport(session, [create_role(session, ALTER_INVOICE_DATE)])

    check_fail_depend_on_strict(
        func=is_date_alterable,
        strict=strict,
        invoice=charge_note,
        exc=exc.INVALID_PARAM,
        msg="Invalid parameter for function: Can't alter date in charge_note_register"
    )


@pytest.mark.parametrize('strict', [False, True])
def test_promocode(session, invoice, strict):
    create_passport(session, [create_role(session, ALTER_INVOICE_DATE)], client=None)
    pc, = create_promocode(session)
    invoice.promo_code = pc
    session.flush()
    check_fail_depend_on_strict(
        func=is_date_alterable,
        strict=strict,
        invoice=invoice,
        exc=exc.INVALID_PARAM,
        msg="Invalid parameter for function: Can't alter date for invoice with promocode"
    )
