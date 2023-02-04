# -*- coding: utf-8 -*-
import pytest

from balance import exc
from balance.providers.invoice_alterable import (
    is_contract_alterable
)

from tests.balance_tests.invoices.alterable.alterable_common import (
    create_passport, create_role, check_fail_depend_on_strict, create_payment,
    create_invoice, EDIT_CONTRACTS, PATCH_INVOICE_CONTRACT)


@pytest.mark.permissions
@pytest.mark.parametrize('strict', [False, True])
def test_client_owner(session, invoice, strict):
    passport = create_passport(session, [create_role(session)], client=invoice.client)
    session.flush()
    check_fail_depend_on_strict(func=is_contract_alterable, strict=strict,
                                invoice=invoice, exc=exc.PERMISSION_DENIED,
                                msg="User {} has no permission PatchInvoiceContract.".format(passport.passport_id))


@pytest.mark.parametrize('strict', [False, True])
def test_overdraft(session, strict):
    invoice = create_invoice(session, overdraft=1)
    check_fail_depend_on_strict(func=is_contract_alterable, strict=strict,
                                invoice=invoice, exc=exc.INVALID_PARAM,
                                msg="Invalid parameter for function: "
                                    "Can't patch contract if invoice is not of type \"prepayment\".")


@pytest.mark.permissions
@pytest.mark.parametrize('perm', [EDIT_CONTRACTS, PATCH_INVOICE_CONTRACT])
@pytest.mark.parametrize('strict', [False, True])
def test_perm(session, invoice, perm, strict):
    passport = create_passport(session, [create_role(session, perm)], client=None)
    if perm == PATCH_INVOICE_CONTRACT:
        assert is_contract_alterable(invoice, strict=strict) is True
    else:
        check_fail_depend_on_strict(func=is_contract_alterable, strict=strict,
                                    invoice=invoice, exc=exc.PERMISSION_DENIED,
                                    msg="User {} has no permission PatchInvoiceContract.".format(passport.passport_id))


@pytest.mark.parametrize('pay', [False, True])
@pytest.mark.parametrize('consume', [False, True])
@pytest.mark.parametrize('strict', [False, True])
def test_has_payments(session, invoice, pay, consume, strict):
    create_passport(session, [create_role(session, PATCH_INVOICE_CONTRACT)], client=None)
    if pay:
        payment = create_payment(session, invoice)
        payment.turn_on()
        assert invoice.confirmed_amount > 0
        assert invoice.consume_sum == 0
    if consume:
        invoice.turn_on_rows()
        assert invoice.consume_sum > 0
    if not (pay or consume):
        assert is_contract_alterable(invoice, strict=strict) is True
    else:
        check_fail_depend_on_strict(func=is_contract_alterable, strict=strict,
                                    invoice=invoice, exc=exc.INVALID_PARAM,
                                    msg="Invalid parameter for function: "
                                        "Can't patch contract if invoice has payments or consumes.")
