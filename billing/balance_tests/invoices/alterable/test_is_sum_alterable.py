# -*- coding: utf-8 -*-
import pytest
from balance import constants as cst, exc
from balance.providers.invoice_alterable import is_sum_alterable

from tests.balance_tests.invoices.alterable.alterable_common import (
    create_passport,
    create_role,
    create_invoice,
    check_fail_depend_on_strict,
    SHOP_PRODUCT_ID,
    create_promocode,
    ALTER_INVOICE_SUM,
)
from tests.balance_tests.invoices.invoice_common import (
    create_charge_note_register,
)


@pytest.mark.permissions
@pytest.mark.parametrize('strict', [False, True])
def test_nobody(session, invoice, strict):
    role = create_role(session)
    passport = create_passport(session, [role], client=None)

    check_fail_depend_on_strict(func=is_sum_alterable, strict=strict,
                                invoice=invoice, exc=exc.PERMISSION_DENIED,
                                msg="User {} has no permission AlterInvoiceSum.".format(passport.passport_id))


@pytest.mark.permissions
@pytest.mark.parametrize('strict', [False, True])
def test_perm(session, invoice, strict):
    create_passport(session, [create_role(session, ALTER_INVOICE_SUM)], client=None)
    assert is_sum_alterable(invoice, strict=strict) is True


@pytest.mark.permissions
@pytest.mark.parametrize('strict', [False, True])
def test_client_owner(session, invoice, strict):
    passport = create_passport(session, [create_role(session)], client=invoice.client)
    session.flush()

    check_fail_depend_on_strict(func=is_sum_alterable, strict=strict,
                                invoice=invoice, exc=exc.PERMISSION_DENIED,
                                msg="User {} has no permission AlterInvoiceSum.".format(passport.passport_id))


@pytest.mark.permissions
@pytest.mark.parametrize(
    'firm_id, result',
    [
        pytest.param(cst.FirmId.YANDEX_OOO, True, id='ok'),
        pytest.param(cst.FirmId.TAXI, False, id='not_ok'),
    ]
)
@pytest.mark.parametrize('strict', [False, True])
def test_perm_firm(session, invoice, firm_id, result, strict):
    role = create_role(session, (ALTER_INVOICE_SUM, {cst.ConstraintTypes.firm_id: None}))
    passport = create_passport(session, [(role, firm_id)], client=None)
    if invoice.firm.id == firm_id:
        assert is_sum_alterable(invoice, strict=strict) is True
    else:
        check_fail_depend_on_strict(func=is_sum_alterable, strict=strict,
                                    invoice=invoice, exc=exc.PERMISSION_DENIED,
                                    msg="User {} has no permission AlterInvoiceSum.".format(passport.passport_id))


@pytest.mark.parametrize('strict', [False, True])
def test_postpay(session, invoice, strict):
    create_passport(session, [create_role(session, ALTER_INVOICE_SUM)], client=None)
    invoice.postpay = 1
    session.flush()
    check_fail_depend_on_strict(func=is_sum_alterable, strict=strict,
                                invoice=invoice, exc=exc.INVALID_PARAM,
                                msg="Invalid parameter for function: Can't change sum for postpay invoice")


@pytest.mark.parametrize('flag', [1, 2])
@pytest.mark.parametrize('strict', [False, True])
def test_credit(session, invoice, flag, strict):
    create_passport(session, [create_role(session, ALTER_INVOICE_SUM)], client=None)
    invoice.credit = flag
    session.flush()
    check_fail_depend_on_strict(func=is_sum_alterable, strict=strict,
                                invoice=invoice, exc=exc.INVALID_PARAM,
                                msg="Invalid parameter for function: Can't change sum for credit invoice")


@pytest.mark.parametrize('strict', [False, True])
def test_instant(session, strict):
    create_passport(session, [create_role(session, ALTER_INVOICE_SUM)], client=None)
    invoice = create_invoice(session, person_type='ph', paysys_id=1000)
    check_fail_depend_on_strict(func=is_sum_alterable, strict=strict,
                                invoice=invoice, exc=exc.INVALID_PARAM,
                                msg="Invalid parameter for function: Can't change sum for instant invoice")


@pytest.mark.parametrize('strict', [False, True])
def test_promocode(session, invoice, strict):
    create_passport(session, [create_role(session, ALTER_INVOICE_SUM)], client=None)
    pc, = create_promocode(session)
    invoice.promo_code = pc
    session.flush()
    check_fail_depend_on_strict(func=is_sum_alterable, strict=strict,
                                invoice=invoice, exc=exc.INVALID_PARAM,
                                msg="Invalid parameter for function: Can't change sum for invoice with promocode")


@pytest.mark.parametrize('strict', [False, True])
def test_consumes(session, invoice, strict):
    create_passport(session, [create_role(session, ALTER_INVOICE_SUM)], client=None)
    invoice.turn_on_rows()
    session.flush()
    check_fail_depend_on_strict(func=is_sum_alterable, strict=strict,
                                invoice=invoice, exc=exc.INVALID_PARAM,
                                msg="Invalid parameter for function: Can't change sum for invoice with consumes")


@pytest.mark.permissions
@pytest.mark.parametrize('strict', [False, True])
def test_receipts_perm(session, invoice, strict):
    create_passport(session, [create_role(session, ALTER_INVOICE_SUM)], client=None)
    invoice.create_receipt(666)
    assert is_sum_alterable(invoice, strict=strict) is True


@pytest.mark.parametrize(
    'product_id',
    [
        cst.DIRECT_PRODUCT_ID,
        cst.MARKET_FISH_PRODUCT_ID,
        SHOP_PRODUCT_ID
    ]
)
@pytest.mark.parametrize('strict', [False, True])
def test_services(session, product_id, strict):
    invoice = create_invoice(session, product_ids=[product_id])
    create_passport(session, [create_role(session, ALTER_INVOICE_SUM)], client=None)
    if product_id == SHOP_PRODUCT_ID:
        check_fail_depend_on_strict(func=is_sum_alterable, strict=strict,
                                    invoice=invoice, exc=exc.INVALID_PARAM,
                                    msg="Invalid parameter for function: Can't change sum for invoice with services {}".format(
                                        {cst.ServiceId.ONE_TIME_SALE}))
    else:
        assert is_sum_alterable(invoice, strict=strict) is True


@pytest.mark.charge_note_register
@pytest.mark.parametrize('strict', [False, True])
def test_charge_note_register(session, invoice, strict):
    charge_note = create_charge_note_register(invoice.paysys_id, invoice.person, invoices=[invoice])
    create_passport(session, [create_role(session, ALTER_INVOICE_SUM)])

    check_fail_depend_on_strict(
        func=is_sum_alterable,
        strict=strict,
        invoice=charge_note,
        exc=exc.INVALID_PARAM,
        msg="Invalid parameter for function: Can't alter sum in charge_note_register"
    )
