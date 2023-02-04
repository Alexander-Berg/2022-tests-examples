# -*- coding: utf-8 -*-

import datetime
import mock

import pytest

from balance import constants as cst
from balance import exc
from balance import mapper
from balance.actions import promocodes as pca
from balance.providers.invoice_alterable import is_pcp_alterable

from tests import object_builder as ob
from tests.balance_tests.invoices.alterable.alterable_common import (
    create_passport,
    create_role,
    create_invoice,
    check_fail_depend_on_strict,
    ALTER_INVOICE_PERSON,
    SHOP_PRODUCT_ID,
    create_payment,
    ALTER_INVOICE_PAYSYS,
    ALTER_INVOICE_CONTRACT,
    ALTER_ALL_INVOICES,
    BILLING_SUPPORT,
)
from tests.balance_tests.invoices.invoice_common import (
    create_charge_note_register,
)

pytestmark = [
    pytest.mark.permissions,
]


def create_promo_code(client, apply_on_create=False):
    session = client.session
    promo_code, = ob.PromoCodeGroupBuilder.construct(
        session,
        calc_class_name='FixedDiscountPromoCodeGroup',
        calc_params={
            'discount_pct': 10,
            'apply_on_create': apply_on_create,
        }
    ).promocodes
    pca.reserve_promo_code(client, promo_code)
    session.flush()
    return promo_code


@pytest.mark.parametrize('strict', [False, True])
def test_wo_perms(session, invoice, strict):
    """Пользователю без прав не показываем кнопку изменения счета"""
    passport = create_passport(session, [create_role(session)])
    check_fail_depend_on_strict(func=is_pcp_alterable, strict=strict,
                                invoice=invoice, exc=exc.PERMISSION_DENIED,
                                msg="User {} has no permission AlterInvoicePaysys.".format(passport.passport_id))


@pytest.mark.parametrize('has_billing_role', [True, False])
@pytest.mark.parametrize('_is_base_alterable', [True, False])
@pytest.mark.parametrize('strict', [False, True])
def test_base_fail(session, strict, _is_base_alterable, has_billing_role):
    """если счет нельзя менять ни при каких условиях, не показываем кнопку изменения счета"""
    perms = [ALTER_INVOICE_PAYSYS]
    if has_billing_role:
        perms.append(BILLING_SUPPORT)
    _passport = create_passport(session, [create_role(session, *perms)])
    invoice = create_invoice(session, person_type='ph', paysys_id=1000)
    with mock.patch('balance.providers.invoice_alterable._is_base_alterable', return_value=_is_base_alterable):
        if _is_base_alterable or has_billing_role:
            assert is_pcp_alterable(invoice, strict=strict) is True
        else:
            check_fail_depend_on_strict(func=is_pcp_alterable, strict=strict,
                                        invoice=invoice, exc=exc.INVALID_PARAM,
                                        msg="Invalid parameter for function: Can't alter invoice's pcp")


@pytest.mark.parametrize('strict', [False, True])
@pytest.mark.parametrize(
    'perm',
    [
        ALTER_INVOICE_PAYSYS,
        ALTER_INVOICE_PERSON,
        ALTER_INVOICE_CONTRACT,
        ALTER_ALL_INVOICES,
        BILLING_SUPPORT,
    ]
)
def test_perms(session, invoice, perm, strict):
    """Сотруднику Яндекса с правами AlterInvoicePerson, AlterInvoiceContract, AlterInvoicePaysys при соблюдении
    прочих условий показываем кнопку изменения счета"""
    passport = create_passport(session, [create_role(session, perm)])
    if perm in [ALTER_INVOICE_PAYSYS, ALTER_INVOICE_PERSON, ALTER_INVOICE_CONTRACT]:
        assert is_pcp_alterable(invoice, strict=strict) is True
    else:
        check_fail_depend_on_strict(func=is_pcp_alterable, strict=strict,
                                    invoice=invoice, exc=exc.PERMISSION_DENIED,
                                    msg="User {} has no permission AlterInvoicePaysys.".format(passport.passport_id))


@pytest.mark.parametrize('strict', [False, True])
@pytest.mark.parametrize(
    'firm_id',
    [
        pytest.param(cst.FirmId.YANDEX_OOO),
        pytest.param(cst.FirmId.TAXI),
    ]
)
@pytest.mark.parametrize('perm', [ALTER_INVOICE_PAYSYS, ALTER_INVOICE_CONTRACT, ALTER_INVOICE_CONTRACT])
def test_perm_firms(session, invoice, firm_id, strict, perm):
    """У обладателей права на изменение параметров счета проверяем, что фирма счета=фирма из роли"""
    role = create_role(session, (perm, {cst.ConstraintTypes.firm_id: None}))
    passport = create_passport(session, [(role, firm_id)], patch_session=True)
    if invoice.firm.id == firm_id:
        assert is_pcp_alterable(invoice, strict=strict) is True
    else:
        check_fail_depend_on_strict(func=is_pcp_alterable, strict=strict,
                                    invoice=invoice, exc=exc.PERMISSION_DENIED,
                                    msg="User {} has no permission AlterInvoicePaysys.".format(passport.passport_id))


@pytest.mark.permissions
@pytest.mark.parametrize('strict', [False, True])
def test_owner(session, strict):
    """Владельцу счета не нужны никакие права для отрисовки кнопок"""
    invoice = create_invoice(session, person_type='ph', paysys_id=1002)
    create_passport(session, [], client=invoice.client)
    session.flush()
    assert is_pcp_alterable(invoice, strict=strict) is True


@pytest.mark.parametrize('strict', [False, True])
@pytest.mark.parametrize('w_perm, is_owner', [(True, False),
                                              (False, True)], ids=['w_perm', 'owner'])
def test_hidden(session, invoice, is_owner, strict, w_perm):
    """Ни владельцам счета, ни обладателям прав не показываем кнопку изменения счета в скрытых счетах"""
    invoice.hidden = 2
    role = create_role(session, ALTER_INVOICE_PAYSYS if w_perm else None)
    create_passport(session, [role], client=invoice.client if is_owner else None)
    check_fail_depend_on_strict(func=is_pcp_alterable, strict=strict,
                                invoice=invoice, exc=exc.INVALID_PARAM,
                                msg="Invalid parameter for function: Can't alter invoice's pcp")


@pytest.mark.parametrize('strict', [False, True])
@pytest.mark.parametrize(
    'w_perm, is_support, is_owner, res',
    [
        pytest.param(True, False, False, False, id='w_perm'),
        pytest.param(False, False, True, False, id='owner'),
        pytest.param(True, True, False, True, id='support'),
    ],
)
def test_repayment(session, invoice, strict, w_perm, is_support, is_owner, res):
    """Ни владельцам счета, ни обладателям прав не показываем кнопку изменения счета в счетах на погашение.
    Показываем только BillingSupport"""
    invoice.type = 'repayment'
    invoice.credit = 1
    invoice.__class__ = mapper.RepaymentInvoice
    session.flush()
    session.expire_all()
    perms = []
    if w_perm:
        perms.append(ALTER_INVOICE_PAYSYS)
    if is_support:
        perms.append(BILLING_SUPPORT)
    role = create_role(session, *perms)
    create_passport(session, [role], client=invoice.client if is_owner else None)
    if res:
        assert is_pcp_alterable(invoice, strict=strict) is True
    else:
        check_fail_depend_on_strict(func=is_pcp_alterable, strict=strict,
                                    invoice=invoice, exc=exc.INVALID_PARAM,
                                    msg="Invalid parameter for function: Can't alter invoice's pcp")


@pytest.mark.parametrize('strict', [False, True])
@pytest.mark.parametrize('w_perm, is_owner', [(True, False),
                                              (False, True)], ids=['w_perm', 'owner'])
def test_y_invoice(session, strict, w_perm, is_owner):
    """Владельцам счетов не рисуем кнопки в кредитных счетах"""
    invoice = create_invoice(session, person_type='ph')
    invoice.type = 'y_invoice'
    invoice.credit = 1
    invoice.__class__ = mapper.YInvoice
    session.flush()
    session.expire_all()
    role = create_role(session, ALTER_INVOICE_PAYSYS if w_perm else None)
    create_passport(session, [role], client=invoice.client if is_owner else None)
    if is_owner:
        check_fail_depend_on_strict(func=is_pcp_alterable, strict=strict,
                                    invoice=invoice, exc=exc.INVALID_PARAM,
                                    msg="Invalid parameter for function: Can't change pcp for credit invoice")
    else:
        assert is_pcp_alterable(invoice, strict=strict) is True


@pytest.mark.parametrize('strict', [False, True])
@pytest.mark.parametrize(
    'is_owner, perms',
    [
        pytest.param(True, [None], id='owner'),
        pytest.param(False, [ALTER_INVOICE_PAYSYS], id='perm'),
        pytest.param(False, [ALTER_INVOICE_PAYSYS, BILLING_SUPPORT], id='billing_support')
    ]
)
def test_receipt(session, perms, is_owner, strict):
    """Сотрудники яндекса с правом BillingSupport и AlterInvoicePaysys показываем кнопку изменения
    счета в оплаченных счетах"""
    invoice = create_invoice(session, person_type='ph', paysys_id=1000)
    role = create_role(session, *perms)
    create_passport(session, [role], client=invoice.client if is_owner else None)
    invoice.create_receipt(666)
    assert invoice.receipt_dt
    assert invoice.receipt_sum
    if perms == [ALTER_INVOICE_PAYSYS, BILLING_SUPPORT]:
        assert is_pcp_alterable(invoice, strict=strict) is True
    else:
        check_fail_depend_on_strict(func=is_pcp_alterable, strict=strict,
                                    invoice=invoice, exc=exc.INVALID_PARAM,
                                    msg="Invalid parameter for function: Can't change pcp in payed invoice")


@pytest.mark.parametrize('strict', [False, True])
@pytest.mark.parametrize(
    'is_owner, perms',
    [
        pytest.param(True, [None], id='owner'),
        pytest.param(False, [ALTER_INVOICE_PAYSYS], id='perm')
    ]
)
def test_receipt_shop(session, invoice, perms, is_owner, strict):
    """Сотрудники Яндекса с правом AlterInvoicePaysys показываем кнопку изменения
    счета в оплаченных, но незаакченных счетах на магазин"""
    invoice = create_invoice(session, person_type='ph', paysys_id=1000, product_ids=[SHOP_PRODUCT_ID])
    role = create_role(session, *perms)
    create_passport(session, [role], client=invoice.client if is_owner else None)
    invoice.turn_on_rows()
    invoice.close_invoice(datetime.datetime.now())
    if is_owner:
        check_fail_depend_on_strict(func=is_pcp_alterable, strict=strict,
                                    invoice=invoice, exc=exc.INVALID_PARAM,
                                    msg="Invalid parameter for function: Can't alter invoice with acts")
    else:
        assert is_pcp_alterable(invoice, strict=strict) is True


@pytest.mark.parametrize('strict', [False, True])
@pytest.mark.parametrize(
    'is_owner, perms',
    [
        (True, [None]),
        (False, [ALTER_INVOICE_PERSON])
    ]
)
def test_payment_obj(session, invoice, perms, is_owner, strict):
    """Владельцам счета не показываем кнопку изменения счета, если по счету есть платежи
    Обладателям прав - показываем"""
    invoice = create_invoice(session, person_type='ph', paysys_id=1000)
    create_payment(session, invoice)
    role = create_role(session, *perms)
    create_passport(session, [role], client=invoice.client if is_owner else None)
    if is_owner:
        check_fail_depend_on_strict(func=is_pcp_alterable, strict=strict,
                                    invoice=invoice, exc=exc.INVALID_PARAM,
                                    msg="Invalid parameter for function: Can't change pcp for invoice with payments")
    else:
        assert is_pcp_alterable(invoice, strict=strict) is True


@pytest.mark.parametrize('strict', [False, True])
@pytest.mark.parametrize('w_perm, is_owner', [(True, False),
                                              (False, True)], ids=['w_perm', 'owner'])
def test_ur_person(session, w_perm, is_owner, strict):
    invoice = create_invoice(session, person_type='ur', paysys_id=1033)  # карта для юрлиц
    role = create_role(session, ALTER_INVOICE_PAYSYS if w_perm else None)
    create_passport(session, [role], client=invoice.client if is_owner else None)
    if w_perm:
        assert is_pcp_alterable(invoice, strict=strict) is True
    else:
        check_fail_depend_on_strict(func=is_pcp_alterable, strict=strict,
                                    invoice=invoice, exc=exc.INVALID_PARAM,
                                    msg="Invalid parameter for function: Can't change pcp for invoice with ur person")


@pytest.mark.parametrize('strict', [False, True])
@pytest.mark.parametrize('w_perm, is_owner', [(True, False),
                                              (False, True)], ids=['w_perm', 'owner'])
def test_bank_paysys(session, w_perm, is_owner, strict):
    invoice = create_invoice(session, person_type='ph', paysys_id=1001)  # банк для физлиц
    role = create_role(session, ALTER_INVOICE_PAYSYS if w_perm else None)
    create_passport(session, [role], client=invoice.client if is_owner else None)
    if w_perm:
        assert is_pcp_alterable(invoice, strict=strict) is True
    else:
        check_fail_depend_on_strict(func=is_pcp_alterable, strict=strict,
                                    invoice=invoice, exc=exc.INVALID_PARAM,
                                    msg="Invalid parameter for function: Can't change pcp for"
                                        " invoice with non-instant payment method")


@pytest.mark.parametrize('strict', [False, True])
@pytest.mark.parametrize('w_perm, is_owner', [(True, False),
                                              (False, True)], ids=['w_perm', 'owner'])
def test_consumed(session, w_perm, is_owner, strict):
    """Включение счета не мешает показывать кнопку изменения счета"""
    invoice = create_invoice(session, person_type='ph', paysys_id=1000)
    role = create_role(session, ALTER_INVOICE_PAYSYS if w_perm else None)
    create_passport(session, [role], client=invoice.client if is_owner else None)
    assert not invoice.consume_sum
    invoice.turn_on_rows()
    assert invoice.consume_sum
    assert is_pcp_alterable(invoice, strict=strict) is True


@pytest.mark.parametrize('strict', [False, True])
@pytest.mark.parametrize('w_perm, is_owner', [(True, False),
                                              (False, True)], ids=['w_perm', 'owner'])
def test_completed(session, invoice, w_perm, is_owner, strict):
    """Показываем кнопку изменения счета в полностью открученных счетах"""
    invoice = create_invoice(session, person_type='ph', paysys_id=1000)
    role = create_role(session, ALTER_INVOICE_PAYSYS if w_perm else None)
    create_passport(session, [role], client=invoice.client if is_owner else None)
    invoice.turn_on_rows()
    consume, = invoice.consumes
    order = consume.order
    order.calculate_consumption(datetime.datetime.now(), {order.shipment_type: 666})
    assert consume.completion_qty
    assert is_pcp_alterable(invoice, strict=strict) is True


@pytest.mark.parametrize('strict', [False, True])
@pytest.mark.parametrize('w_perm, is_owner', [(True, False),
                                              (False, True)], ids=['w_perm', 'owner'])
def test_acted(session, w_perm, is_owner, strict):
    """Владельцы счета не могут менять способ оплаты в заакченных счетах"""
    invoice = create_invoice(session, person_type='ph', paysys_id=1000)
    role = create_role(session, ALTER_INVOICE_PAYSYS if w_perm else None)
    create_passport(session, [role], client=invoice.client if is_owner else None)
    invoice.turn_on_rows()
    invoice.close_invoice(datetime.datetime.now())
    if w_perm:
        assert is_pcp_alterable(invoice, strict=strict) is True
    else:
        check_fail_depend_on_strict(func=is_pcp_alterable, strict=strict,
                                    invoice=invoice, exc=exc.INVALID_PARAM,
                                    msg="Invalid parameter for function: Can't alter invoice with acts")


@pytest.mark.parametrize('strict', [False, True])
@pytest.mark.parametrize('w_perm, is_owner', [(True, False),
                                              (False, True)], ids=['w_perm', 'owner'])
def test_acted_overdraft(session, w_perm, is_owner, strict):
    """Владельцы счета могут менять способ оплаты в заакченных овердрафтных счетах без поступлений(?)"""
    invoice = create_invoice(session, person_type='ph', paysys_id=1000, overdraft=1)
    role = create_role(session, ALTER_INVOICE_PAYSYS if w_perm else None)
    create_passport(session, [role], client=invoice.client if is_owner else None)
    invoice.turn_on_rows()
    invoice.close_invoice(datetime.datetime.now())
    assert is_pcp_alterable(invoice, strict=strict) is True


@pytest.mark.parametrize('strict', [False, True])
@pytest.mark.parametrize('w_perm, is_owner', [(True, False),
                                              (False, True)], ids=['w_perm', 'owner'])
def test_acted_shop(session, w_perm, is_owner, strict):
    invoice = create_invoice(session, person_type='ph', paysys_id=1000, product_ids=[SHOP_PRODUCT_ID])
    role = create_role(session, ALTER_INVOICE_PAYSYS if w_perm else None)
    create_passport(session, [role], client=invoice.client if is_owner else None)

    invoice.turn_on_rows()
    invoice.close_invoice(datetime.datetime.now())

    if w_perm:
        assert is_pcp_alterable(invoice, strict=strict) is True
    else:
        check_fail_depend_on_strict(func=is_pcp_alterable, strict=strict,
                                    invoice=invoice, exc=exc.INVALID_PARAM,
                                    msg="Invalid parameter for function: Can't alter invoice with acts")


@pytest.mark.parametrize('strict', [False, True])
@pytest.mark.parametrize('w_perm, is_owner', [(True, False),
                                              (False, True)], ids=['w_perm', 'owner'])
def test_payed_acted_shop(session, w_perm, is_owner, strict):
    invoice = create_invoice(session, person_type='ph', paysys_id=1000, product_ids=[SHOP_PRODUCT_ID])
    invoice.create_receipt(666)
    role = create_role(session, ALTER_INVOICE_PAYSYS if w_perm else None)
    create_passport(session, [role], client=invoice.client if is_owner else None)
    invoice.turn_on_rows()
    invoice.close_invoice(datetime.datetime.now())
    check_fail_depend_on_strict(func=is_pcp_alterable, strict=strict,
                                invoice=invoice, exc=exc.INVALID_PARAM,
                                msg="Invalid parameter for function: Can't change pcp in payed invoice")


@pytest.mark.charge_note_register
@pytest.mark.parametrize('strict', [False, True])
@pytest.mark.parametrize('is_owner', [False, True])
def test_charge_note_register(session, invoice, strict, is_owner):
    charge_note = create_charge_note_register(invoice.paysys_id, invoice.person, invoices=[invoice])

    role = create_role(session, None if is_owner else ALTER_INVOICE_PAYSYS)
    create_passport(session, [role], client=invoice.client if is_owner else None)

    check_fail_depend_on_strict(
        func=is_pcp_alterable,
        strict=strict,
        invoice=charge_note,
        exc=exc.INVALID_PARAM,
        msg="Invalid parameter for function: Can't alter pcp in charge_note_register"
    )


@pytest.mark.parametrize('strict', [False, True])
@pytest.mark.parametrize('is_owner', [False, True])
def test_personal_account(session, invoice, strict, is_owner):
    invoice.type = 'personal_account'
    invoice.postpay = 1
    invoice.__class__ = mapper.PersonalAccount
    session.flush()
    session.expire_all()

    role = create_role(session, None if is_owner else ALTER_INVOICE_PAYSYS)
    create_passport(session, [role], client=invoice.client if is_owner else None)

    check_fail_depend_on_strict(
        func=is_pcp_alterable,
        strict=strict,
        invoice=invoice,
        exc=exc.INVALID_PARAM,
        msg="Invalid parameter for function: Can't alter pcp in personal_account"
    )


@pytest.mark.parametrize('strict', [False, True])
@pytest.mark.parametrize('is_owner', [False, True])
def test_promocode(session, strict, is_owner):
    client = ob.ClientBuilder.construct(session)
    create_promo_code(client, apply_on_create=False)
    invoice = create_invoice(session, client=client, person_type='ph', paysys_id=1000)
    invoice.turn_on_rows(apply_promocode=True)

    role = create_role(session, None if is_owner else ALTER_INVOICE_PAYSYS)
    create_passport(session, [role], client=invoice.client if is_owner else None)

    check_fail_depend_on_strict(
        func=is_pcp_alterable,
        strict=strict,
        invoice=invoice,
        exc=exc.INVALID_PARAM,
        msg="Invalid parameter for function: Can't alter pcp for invoice with promocode"
    )


@pytest.mark.parametrize('strict', [False, True])
def test_promocode_billingsupport(session, invoice, strict):
    client = ob.ClientBuilder.construct(session)
    create_promo_code(client, apply_on_create=True)
    invoice = create_invoice(session, client=client, person_type='ph', paysys_id=1000)

    create_role(session, BILLING_SUPPORT, ALTER_INVOICE_PAYSYS)

    with mock.patch('balance.mapper.invoices.Invoice.has_promocode_on_create', True):
        check_fail_depend_on_strict(
            func=is_pcp_alterable,
            strict=strict,
            invoice=invoice,
            exc=exc.INVALID_PARAM,
            msg="Invalid parameter for function: Can't alter pcp in invoice with promocode applied at creation"
        )
