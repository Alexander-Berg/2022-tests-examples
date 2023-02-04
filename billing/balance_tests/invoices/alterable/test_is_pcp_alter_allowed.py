# -*- coding: utf-8 -*-
import pytest
import mock
import datetime

from balance import mapper, exc, constants as cst
from balance.providers.invoice_alterable import (
    is_pcp_alter_allowed
)

from tests.balance_tests.invoices.alterable.alterable_common import (
    create_invoice, check_fail_depend_on_strict, SHOP_PRODUCT_ID, BILLING_SUPPORT,
    ALTER_INVOICE_PAYSYS, ALTER_INVOICE_PERSON, ALTER_INVOICE_CONTRACT,
    create_role, create_passport, create_person, create_contract, create_payment,
    create_y_invoice)

pytestmark = [
    pytest.mark.permissions,
]


@pytest.mark.parametrize('strict', [False, True])
def test_no_change(session, invoice, strict):
    """если ничего не меняем, ничего не проверяем"""
    with mock.patch('balance.providers.invoice_alterable._is_base_alterable', return_value=False):
        assert is_pcp_alter_allowed(invoice, strict=strict, person=invoice.person,
                                    contract=invoice.contract, paysys=invoice.paysys) is True


@pytest.mark.parametrize('_is_base_alterable', [True, False])
@pytest.mark.parametrize('strict', [False, True])
def test_base_fail(session, strict, _is_base_alterable):
    """если счет нельзя менять ни при каких условиях, не даем менять счет"""
    invoice = create_invoice(session, person_type='ph', paysys_id=1000)
    new_paysys = session.query(mapper.Paysys).getone(1001)

    with mock.patch('balance.providers.invoice_alterable._is_base_alterable', return_value=_is_base_alterable):
        if _is_base_alterable:
            assert is_pcp_alter_allowed(invoice, strict=strict, person=invoice.person,
                                        contract=invoice.contract, paysys=new_paysys) is True
        else:
            check_fail_depend_on_strict(func=is_pcp_alter_allowed, strict=strict,
                                        invoice=invoice, exc=exc.INVALID_PARAM,
                                        msg="Invalid parameter for function: Can't alter invoice's pcp",
                                        person=invoice.person, contract=invoice.contract, paysys=new_paysys)


@pytest.mark.parametrize('strict', [False, True])
def test_currency_change(session, strict):
    """нельзя менять способ оплаты на тот, в котором paysys.currency отличается от invoice.currency"""
    invoice = create_invoice(session, person_type='yt', paysys_id=1013)
    new_paysys = session.query(mapper.Paysys).getone(1014)
    assert invoice.currency != new_paysys.currency
    assert invoice.nds == new_paysys.nds
    check_fail_depend_on_strict(func=is_pcp_alter_allowed, strict=strict,
                                invoice=invoice, exc=exc.CANNOT_PATCH_INVOICE_CURRENCY_OR_NDS,
                                msg="Cannot change currency or nds flag in invoice, invoice_id: {}".format(invoice.id),
                                person=invoice.person, contract=invoice.contract, paysys=new_paysys)


@pytest.mark.parametrize('strict', [False, True])
def test_nds_change(session, strict):
    """нельзя менять способ оплаты на тот, в котором paysys.nds отличается от invoice.nds"""
    invoice = create_invoice(session, person_type='yt', paysys_id=1014)
    new_paysys = session.query(mapper.Paysys).getone(11069)
    assert invoice.paysys.currency == new_paysys.currency
    assert invoice.nds != new_paysys.nds
    check_fail_depend_on_strict(func=is_pcp_alter_allowed, strict=strict,
                                invoice=invoice, exc=exc.CANNOT_PATCH_INVOICE_CURRENCY_OR_NDS,
                                msg="Cannot change currency or nds flag in invoice, invoice_id: {}".format(invoice.id),
                                person=invoice.person, contract=invoice.contract, paysys=new_paysys)


@pytest.mark.parametrize(
    'is_owner, perm',
    [
        pytest.param(False, None, id='nobody'),
        pytest.param(True, None, id='owner'),
        pytest.param(False, ALTER_INVOICE_PAYSYS, id='perm'),
        pytest.param(False, ALTER_INVOICE_PERSON, id='wrong_perm'),
        pytest.param(False, BILLING_SUPPORT, id='wrong_perm'),
    ]
)
@pytest.mark.parametrize('strict', [False, True])
def test_paysys(session, is_owner, perm, strict):
    """Менять способ оплаты могут пользователи с правом AlterInvoicePaysys или владельца счета"""
    invoice = create_invoice(session, person_type='ph', paysys_id=1000)
    role = create_role(session, perm)
    passport = create_passport(session, [role], client=invoice.client if is_owner else None)
    new_paysys = session.query(mapper.Paysys).getone(1002)
    if is_owner or perm == ALTER_INVOICE_PAYSYS:
        assert is_pcp_alter_allowed(invoice, strict=strict, person=invoice.person,
                                    contract=invoice.contract, paysys=new_paysys) is True
    else:
        check_fail_depend_on_strict(func=is_pcp_alter_allowed, strict=strict,
                                    invoice=invoice, exc=exc.PERMISSION_DENIED,
                                    msg="User {} has no permission AlterInvoicePaysys.".format(passport.passport_id),
                                    person=invoice.person, contract=invoice.contract, paysys=new_paysys)


@pytest.mark.parametrize(
    'is_owner, perm',
    [
        pytest.param(False, None, id='nobody'),
        pytest.param(True, None, id='owner'),
        pytest.param(False, ALTER_INVOICE_PAYSYS, id='perm'),
        pytest.param(False, ALTER_INVOICE_PERSON, id='wrong_perm'),
    ]
)
@pytest.mark.parametrize('strict', [False, True])
def test_paysys_ur_person(session, is_owner, perm, strict):
    """Владельцы счета не могут менять способ оплаты в счетах с плательщиками юриками"""
    invoice = create_invoice(session, person_type='ur', paysys_id=1033)
    role = create_role(session, perm)
    passport = create_passport(session, [role], client=invoice.client if is_owner else None)
    new_paysys = session.query(mapper.Paysys).getone(1093)
    if perm == ALTER_INVOICE_PAYSYS:
        assert is_pcp_alter_allowed(invoice, strict=strict, person=invoice.person,
                                    contract=invoice.contract, paysys=new_paysys) is True
    elif is_owner:
        check_fail_depend_on_strict(func=is_pcp_alter_allowed, strict=strict,
                                    invoice=invoice, exc=exc.INVALID_PARAM,
                                    msg="Invalid parameter for function: Can't change pcp for invoice with ur person",
                                    person=invoice.person, contract=invoice.contract, paysys=new_paysys)
    else:
        check_fail_depend_on_strict(func=is_pcp_alter_allowed, strict=strict,
                                    invoice=invoice, exc=exc.PERMISSION_DENIED,
                                    msg="User {} has no permission AlterInvoicePaysys.".format(passport.passport_id),
                                    person=invoice.person, contract=invoice.contract, paysys=new_paysys)


@pytest.mark.parametrize(
    'is_owner, perm',
    [
        pytest.param(True, None, id='owner'),
        pytest.param(False, ALTER_INVOICE_PAYSYS, id='perm'),
    ]
)
@pytest.mark.parametrize('strict', [False, True])
def test_paysys_y_invoice(session, is_owner, perm, strict):
    """Владельцы счета не могут менять способ оплаты в кредитных счетах"""
    invoice = create_y_invoice(session, person_type='ph', paysys_id=1001)
    role = create_role(session, perm)
    create_passport(session, [role], client=invoice.client if is_owner else None)
    new_paysys = session.query(mapper.Paysys).getone(1000)
    if perm == ALTER_INVOICE_PAYSYS:
        assert is_pcp_alter_allowed(invoice, strict=strict, person=invoice.person,
                                    contract=invoice.contract, paysys=new_paysys) is True
    elif is_owner:
        check_fail_depend_on_strict(func=is_pcp_alter_allowed, strict=strict,
                                    invoice=invoice, exc=exc.INVALID_PARAM,
                                    msg="Invalid parameter for function: Can't change pcp for credit invoice",
                                    person=invoice.person, contract=invoice.contract, paysys=new_paysys)


@pytest.mark.parametrize(
    'is_owner, perm',
    [
        pytest.param(True, None, id='owner'),
        pytest.param(False, ALTER_INVOICE_PAYSYS, id='perm'),
    ]
)
@pytest.mark.parametrize('strict', [False, True])
def test_paysys_instant(session, strict, is_owner, perm):
    """владельцам счета нельзя менять способ оплаты в счете, если он не мгновенный"""
    invoice = create_invoice(session, person_type='ph', paysys_id=1001)
    role = create_role(session, perm)
    create_passport(session, [role], client=invoice.client if is_owner else None)
    new_paysys = session.query(mapper.Paysys).getone(1000)
    if perm == ALTER_INVOICE_PAYSYS:
        assert is_pcp_alter_allowed(invoice, strict=strict, person=invoice.person,
                                    contract=invoice.contract, paysys=new_paysys) is True
    else:
        check_fail_depend_on_strict(func=is_pcp_alter_allowed, strict=strict,
                                    invoice=invoice, exc=exc.INVALID_PARAM,
                                    msg="Invalid parameter for function: Can't change pcp for invoice"
                                        " with non-instant payment method",
                                    person=invoice.person, contract=invoice.contract, paysys=new_paysys)


@pytest.mark.parametrize(
    'is_owner, perm',
    [
        pytest.param(True, None, id='owner'),
        pytest.param(False, ALTER_INVOICE_PAYSYS, id='perm'),
    ]
)
@pytest.mark.parametrize('strict', [False, True])
def test_paysys_to_instant(session, strict, is_owner, perm):
    """владельцам счета нельзя менять способ оплаты в счете на не мгновенный"""
    invoice = create_invoice(session, person_type='ph', paysys_id=1000)
    role = create_role(session, perm)
    create_passport(session, [role], client=invoice.client if is_owner else None)
    new_paysys = session.query(mapper.Paysys).getone(1001)
    if perm == ALTER_INVOICE_PAYSYS:
        assert is_pcp_alter_allowed(invoice, strict=strict, person=invoice.person,
                                    contract=invoice.contract, paysys=new_paysys) is True
    else:
        check_fail_depend_on_strict(func=is_pcp_alter_allowed, strict=strict,
                                    invoice=invoice, exc=exc.INVALID_PARAM,
                                    msg="Invalid parameter for function: Can't change pcp to non-instant payment method",
                                    person=invoice.person, contract=invoice.contract, paysys=new_paysys)


@pytest.mark.parametrize(
    'is_owner, perm',
    [
        pytest.param(True, None, id='owner'),
        pytest.param(False, ALTER_INVOICE_PAYSYS, id='perm'),
    ]
)
@pytest.mark.parametrize('strict', [False, True])
def test_paysys_w_payments(session, is_owner, perm, strict):
    """Владельцы счета не могут менять способ оплаты в счетах с оплатами"""
    invoice = create_invoice(session, person_type='ph', paysys_id=1000)
    create_payment(session, invoice)
    role = create_role(session, perm)
    create_passport(session, [role], client=invoice.client if is_owner else None)
    new_paysys = session.query(mapper.Paysys).getone(1002)
    if perm == ALTER_INVOICE_PAYSYS:
        assert is_pcp_alter_allowed(invoice, strict=strict, person=invoice.person,
                                    contract=invoice.contract, paysys=new_paysys) is True
    elif is_owner:
        check_fail_depend_on_strict(func=is_pcp_alter_allowed, strict=strict,
                                    invoice=invoice, exc=exc.INVALID_PARAM,
                                    msg="Invalid parameter for function: Can't change pcp for invoice with payments",
                                    person=invoice.person, contract=invoice.contract, paysys=new_paysys)


@pytest.mark.parametrize(
    'src_firm_id, dst_firm_id, role_firm_ids',
    [
        (cst.FirmId.YANDEX_OOO, cst.FirmId.YANDEX_OOO, [cst.FirmId.YANDEX_OOO]),
        (cst.FirmId.YANDEX_OOO, cst.FirmId.TAXI, [cst.FirmId.YANDEX_OOO]),
        (cst.FirmId.TAXI, cst.FirmId.YANDEX_OOO, [cst.FirmId.YANDEX_OOO]),
        (cst.FirmId.TAXI, cst.FirmId.YANDEX_OOO, [cst.FirmId.YANDEX_OOO, cst.FirmId.TAXI]),
    ]
)
@pytest.mark.parametrize('strict', [False, True])
@pytest.mark.parametrize('w_owner', [False, True])
def test_paysys_firm_perms(session, src_firm_id, dst_firm_id, role_firm_ids, strict, w_owner):
    """У владельцев счета не проверяем ничего

    У обладателей права AlterInvoicePaysys проверяем, что фирма счета=фирма из способа оплаты
     и совпадает с фирмой из роли/права"""
    src_paysys = session.query(mapper.Paysys).filter_by(cc='pc', firm_id=src_firm_id).one()
    dst_paysys = session.query(mapper.Paysys).filter_by(cc='pc', firm_id=dst_firm_id).one()

    invoice = create_invoice(session, person_type='ph', firm_id=src_firm_id, paysys_id=src_paysys.id)
    role = create_role(session, (ALTER_INVOICE_PAYSYS, {cst.ConstraintTypes.firm_id: None}))
    passport = create_passport(session, [(role, role_firm_id) for role_firm_id in role_firm_ids],
                               client=invoice.client if w_owner else None)

    if {src_firm_id, dst_firm_id} <= set(role_firm_ids) or w_owner:
        assert is_pcp_alter_allowed(invoice, strict=strict, person=invoice.person,
                                    contract=invoice.contract, paysys=dst_paysys) is True
    else:
        check_fail_depend_on_strict(func=is_pcp_alter_allowed, strict=strict,
                                    invoice=invoice, exc=exc.PERMISSION_DENIED,
                                    msg="User {} has no permission AlterInvoicePaysys.".format(passport.passport_id),
                                    person=invoice.person, contract=invoice.contract, paysys=dst_paysys)


@pytest.mark.parametrize(
    'is_owner, perms',
    [
        pytest.param(True, [None], id='owner'),
        pytest.param(False, [ALTER_INVOICE_PAYSYS], id='perm'),
        pytest.param(False, [ALTER_INVOICE_PAYSYS, BILLING_SUPPORT], id='billing_support')
    ]
)
@pytest.mark.parametrize('strict', [False, True])
def test_payed_paysys(session, is_owner, perms, strict):
    """Сотрудники яндекса с правом BillingSupport и AlterInvoicePaysys могут менять способ оплаты
     в оплаченном счете"""
    invoice = create_invoice(session, person_type='ph', paysys_id=1000)
    invoice.receipt_sum_1c = invoice.total_sum
    invoice.create_receipt(invoice.effective_sum)
    role = create_role(session, *perms)
    create_passport(session, [role], client=invoice.client if is_owner else None)
    new_paysys = session.query(mapper.Paysys).getone(1002)

    if perms == [ALTER_INVOICE_PAYSYS, BILLING_SUPPORT]:
        assert is_pcp_alter_allowed(invoice, strict=strict, person=invoice.person,
                                    contract=invoice.contract, paysys=new_paysys) is True
    else:
        check_fail_depend_on_strict(func=is_pcp_alter_allowed, strict=strict,
                                    invoice=invoice, exc=exc.INVALID_PARAM,
                                    msg="Invalid parameter for function: Can't change pcp in payed invoice",
                                    person=invoice.person, contract=invoice.contract, paysys=new_paysys)


@pytest.mark.parametrize(
    'is_owner, perms',
    [
        pytest.param(True, [None], id='owner'),
        pytest.param(False, [ALTER_INVOICE_PAYSYS], id='perm')
    ]
)
@pytest.mark.parametrize('strict', [False, True])
def test_payed_paysys_shop(session, strict, is_owner, perms):
    """Сотрудники Яндекса с правом AlterInvoicePaysys могут менять способы оплаты в
     оплаченных, но незаакченных счетах на магазин"""
    invoice = create_invoice(session, person_type='ph', paysys_id=1000, product_ids=[SHOP_PRODUCT_ID])
    invoice.receipt_sum_1c = invoice.total_sum
    invoice.create_receipt(invoice.effective_sum)
    role = create_role(session, *perms)
    create_passport(session, [role], client=invoice.client if is_owner else None)
    new_paysys = session.query(mapper.Paysys).getone(1002)

    if is_owner:
        check_fail_depend_on_strict(func=is_pcp_alter_allowed, strict=strict,
                                    invoice=invoice, exc=exc.INVALID_PARAM,
                                    msg="Invalid parameter for function: Can't change pcp in payed invoice",
                                    person=invoice.person, contract=invoice.contract, paysys=new_paysys)
    else:
        assert is_pcp_alter_allowed(invoice, strict=strict, person=invoice.person,
                                    contract=invoice.contract, paysys=new_paysys) is True


@pytest.mark.parametrize(
    'is_owner, perms',
    [
        pytest.param(True, [None], id='owner'),
        pytest.param(False, [ALTER_INVOICE_PAYSYS], id='perm'),
        pytest.param(False, [ALTER_INVOICE_PAYSYS, BILLING_SUPPORT], id='billing_support'),
    ]
)
@pytest.mark.parametrize('strict', [False, True])
def test_acted_paysys(session, is_owner, perms, strict):
    """Владельцы счета не могут менять способ оплаты в заакченных счетах"""
    invoice = create_invoice(session, person_type='ph', paysys_id=1000)
    invoice.turn_on_rows()
    invoice.close_invoice(datetime.datetime.now())

    role = create_role(session, *perms)
    create_passport(session, [role], client=invoice.client if is_owner else None)
    new_paysys = session.query(mapper.Paysys).getone(1002)
    if ALTER_INVOICE_PAYSYS in perms:
        assert is_pcp_alter_allowed(invoice, strict=strict, person=invoice.person,
                                    contract=invoice.contract, paysys=new_paysys) is True
    else:
        check_fail_depend_on_strict(func=is_pcp_alter_allowed, strict=strict,
                                    invoice=invoice, exc=exc.INVALID_PARAM,
                                    msg="Invalid parameter for function: Can't alter invoice with acts",
                                    person=invoice.person, contract=invoice.contract, paysys=new_paysys)


@pytest.mark.parametrize('strict', [False, True])
def test_acted_paysys_overdraft_owner(session, strict):
    """Владельцы счета могут менять способ оплаты в заакченных овердрафтных счетах без поступлений(?)"""
    invoice = create_invoice(session, person_type='ph', paysys_id=1000, overdraft=1)
    invoice.turn_on_rows()
    invoice.close_invoice(datetime.datetime.now())

    role = create_role(session)
    create_passport(session, [role], client=invoice.client)
    new_paysys = session.query(mapper.Paysys).getone(1002)
    assert is_pcp_alter_allowed(invoice, strict=strict, person=invoice.person,
                                contract=invoice.contract, paysys=new_paysys) is True


@pytest.mark.parametrize(
    'is_owner, perm',
    [
        pytest.param(False, None, id='nobody'),
        pytest.param(True, None, id='owner'),
        pytest.param(False, ALTER_INVOICE_PERSON, id='perm'),
        pytest.param(False, ALTER_INVOICE_PAYSYS, id='wrong_perm'),
    ]
)
@pytest.mark.parametrize('strict', [False, True])
def test_person(session, is_owner, perm, strict):
    """Менять плательщиков могут только пользователи с правом AlterInvoicePerson"""
    invoice = create_invoice(session, person_type='ph', paysys_id=1002)
    role = create_role(session, perm)
    passport = create_passport(session, [role], client=invoice.client if is_owner else None)
    new_person = create_person(session, invoice.client)
    if perm == ALTER_INVOICE_PERSON:
        assert is_pcp_alter_allowed(invoice, strict=strict, person=new_person,
                                    contract=invoice.contract, paysys=invoice.paysys) is True
    else:
        check_fail_depend_on_strict(func=is_pcp_alter_allowed, strict=strict,
                                    invoice=invoice, exc=exc.PERMISSION_DENIED,
                                    msg="User {} has no permission AlterInvoicePerson.".format(passport.passport_id),
                                    person=new_person, contract=invoice.contract, paysys=invoice.paysys)


@pytest.mark.parametrize(
    'is_owner, perm',
    [
        pytest.param(True, None, id='owner'),
        pytest.param(False, ALTER_INVOICE_PERSON, id='perm'),
    ]
)
@pytest.mark.parametrize('strict', [
    False,
    True
])
def test_person_y_invoice(session, is_owner, perm, strict):
    """Никто не может менять плательщиков в ы счетах"""
    invoice = create_y_invoice(session, person_type='ph', paysys_id=1001)
    role = create_role(session, perm)
    create_passport(session, [role], client=invoice.client if is_owner else None)
    new_person = create_person(session, invoice.client)
    error_msg = "Invalid parameter for function: Can\'t alter person or contract of y_invoice"
    check_fail_depend_on_strict(func=is_pcp_alter_allowed, strict=strict,
                                invoice=invoice, exc=exc.INVALID_PARAM,
                                msg=error_msg,
                                person=new_person, contract=invoice.contract, paysys=invoice.paysys)


@pytest.mark.parametrize(
    'perms', [
        pytest.param([ALTER_INVOICE_PERSON], id='perm'),
        pytest.param([ALTER_INVOICE_PERSON, BILLING_SUPPORT], id='billing_support'),
    ]
)
@pytest.mark.parametrize('strict', [False, True])
def test_payed_person(session, perms, strict):
    """Сотрудники яндекса с правом AlterInvoicePerson не могут менять плательщика в оплаченных счетах. Поддержка
     с дополнительным правом BillingSupport может"""
    invoice = create_invoice(session, person_type='ph', paysys_id=1001)
    invoice.receipt_sum_1c = invoice.total_sum
    invoice.create_receipt(invoice.effective_sum)

    role = create_role(session, *perms)
    create_passport(session, [role], client=None)
    new_person = create_person(session, client=invoice.client, type='ph')
    if perms == [ALTER_INVOICE_PERSON, BILLING_SUPPORT]:
        assert is_pcp_alter_allowed(invoice, strict=strict, person=new_person,
                                    contract=invoice.contract, paysys=invoice.paysys) is True
    else:
        check_fail_depend_on_strict(func=is_pcp_alter_allowed, strict=strict,
                                    invoice=invoice, exc=exc.INVALID_PARAM,
                                    msg="Invalid parameter for function: Can't change pcp in payed invoice",
                                    person=new_person, contract=invoice.contract, paysys=invoice.paysys)


@pytest.mark.parametrize(
    'perms',
    [
        pytest.param([ALTER_INVOICE_PERSON], id='perm'),
        pytest.param([ALTER_INVOICE_PERSON, BILLING_SUPPORT], id='billing_support'),
    ]
)
@pytest.mark.parametrize('strict', [False, True])
def test_acted_person(session, perms, strict):
    """Сотрудники яндекса с правом AlterInvoicePerson не могут менять плательщика в заакченных счетах. Поддержка
     с дополнительным правом BillingSupport может"""
    invoice = create_invoice(session, person_type='ph', paysys_id=1001)
    invoice.turn_on_rows()
    invoice.close_invoice(datetime.datetime.now())

    role = create_role(session, *perms)
    create_passport(session, [role], client=None)
    new_person = create_person(session, client=invoice.client, type='ph')
    if perms == [ALTER_INVOICE_PERSON, BILLING_SUPPORT]:
        assert is_pcp_alter_allowed(invoice, strict=strict, person=new_person,
                                    contract=invoice.contract, paysys=invoice.paysys) is True
    else:
        check_fail_depend_on_strict(func=is_pcp_alter_allowed, strict=strict,
                                    invoice=invoice, exc=exc.INVALID_PARAM,
                                    msg="Invalid parameter for function: Can't change contract "
                                        "or person if invoice has acts or payments",
                                    person=new_person, contract=invoice.contract, paysys=invoice.paysys)


@pytest.mark.parametrize(
    'is_owner, perm',
    [
        pytest.param(False, None, id='nobody'),
        pytest.param(True, None, id='owner'),
        pytest.param(False, ALTER_INVOICE_CONTRACT, id='perm'),
        pytest.param(False, ALTER_INVOICE_PAYSYS, id='wrong_perm'),
    ]
)
@pytest.mark.parametrize('strict', [False, True])
def test_contract(session, is_owner, perm, strict):
    """Менять договор могут только пользователи с правом AlterInvoiceContract"""
    invoice = create_invoice(session, person_type='ph', paysys_id=1000)
    invoice.contract = create_contract(session, client=invoice.client, person=invoice.person)
    role = create_role(session, perm)
    passport = create_passport(session, [role], client=invoice.client if is_owner else None)
    new_contract = create_contract(session, client=invoice.client, person=invoice.person)
    if perm == ALTER_INVOICE_CONTRACT:
        assert is_pcp_alter_allowed(invoice, strict=strict, person=invoice.person,
                                    contract=new_contract, paysys=invoice.paysys) is True
    else:
        check_fail_depend_on_strict(func=is_pcp_alter_allowed, strict=strict,
                                    invoice=invoice, exc=exc.PERMISSION_DENIED,
                                    msg="User {} has no permission AlterInvoiceContract.".format(passport.passport_id),
                                    person=invoice.person, contract=new_contract, paysys=invoice.paysys)


@pytest.mark.parametrize(
    'is_owner, perm',
    [
        pytest.param(True, None, id='owner'),
        pytest.param(False, ALTER_INVOICE_CONTRACT, id='perm'),
    ]
)
@pytest.mark.parametrize('strict', [
    False,
    True
])
def test_contract_y_invoice(session, is_owner, perm, strict):
    """Никто не может менять договор в ы счетах"""
    invoice = create_y_invoice(session, person_type='ph', paysys_id=1001)
    role = create_role(session, perm)
    create_passport(session, [role], client=invoice.client if is_owner else None)
    new_contract = create_contract(session, client=invoice.client, person=invoice.person)
    error_msg = "Invalid parameter for function: Can\'t alter person or contract of y_invoice"
    check_fail_depend_on_strict(func=is_pcp_alter_allowed, strict=strict,
                                invoice=invoice, exc=exc.INVALID_PARAM,
                                msg=error_msg,
                                person=invoice.person, contract=new_contract, paysys=invoice.paysys)


@pytest.mark.parametrize(
    'perms',
    [
        pytest.param([ALTER_INVOICE_CONTRACT], id='perm'),
        pytest.param([ALTER_INVOICE_CONTRACT, BILLING_SUPPORT], id='billing_support'
                     ),
    ]
)
@pytest.mark.parametrize('strict', [False, True])
def test_payed_contract(session, perms, strict):
    """Сотрудники яндекса с правом AlterInvoiceContract не могут менять договор в оплаченных счетах. Поддержка
     с дополнительным правом BillingSupport может"""
    invoice = create_invoice(session, person_type='ph', paysys_id=1001)
    invoice.contract = create_contract(session, client=invoice.client, person=invoice.person)
    invoice.receipt_sum_1c = invoice.total_sum
    invoice.create_receipt(invoice.effective_sum)
    create_passport(session, [create_role(session, *perms)], client=None)
    new_contract = create_contract(session, client=invoice.client, person=invoice.person)
    if perms == [ALTER_INVOICE_CONTRACT, BILLING_SUPPORT]:
        assert is_pcp_alter_allowed(invoice, strict=strict, person=invoice.person,
                                    contract=new_contract, paysys=invoice.paysys) is True
    else:
        check_fail_depend_on_strict(func=is_pcp_alter_allowed, strict=strict,
                                    invoice=invoice, exc=exc.INVALID_PARAM,
                                    msg="Invalid parameter for function: Can't change pcp in payed invoice",
                                    person=invoice.person, contract=new_contract, paysys=invoice.paysys)


@pytest.mark.parametrize(
    'perms',
    [
        pytest.param([ALTER_INVOICE_CONTRACT], id='perm'),
        pytest.param([ALTER_INVOICE_CONTRACT, BILLING_SUPPORT], id='billing_support'
                     ),
    ]
)
@pytest.mark.parametrize('strict', [False, True])
def test_acted_contract(session, perms, strict):
    """Сотрудники яндекса с правом AlterInvoiceContract не могут менять договор в заакченных счетах. Поддержка
     с дополнительным правом BillingSupport может"""
    invoice = create_invoice(session, person_type='ph', paysys_id=1001)
    invoice.contract = create_contract(session, client=invoice.client, person=invoice.person)
    invoice.turn_on_rows()
    invoice.close_invoice(datetime.datetime.now())

    create_passport(session, [create_role(session, *perms)], client=None)
    new_contract = create_contract(session, client=invoice.client, person=invoice.person)
    if perms == [ALTER_INVOICE_CONTRACT, BILLING_SUPPORT]:
        assert is_pcp_alter_allowed(invoice, strict=strict, person=invoice.person,
                                    contract=new_contract, paysys=invoice.paysys) is True
    else:
        check_fail_depend_on_strict(func=is_pcp_alter_allowed, strict=strict,
                                    invoice=invoice, exc=exc.INVALID_PARAM,
                                    msg="Invalid parameter for function: Can't change contract "
                                        "or person if invoice has acts or payments",
                                    person=invoice.person, contract=new_contract, paysys=invoice.paysys)


@pytest.mark.parametrize('strict', [False, True])
def test_ya_forbidden(session, strict):
    invoice = create_invoice(session, person_type='ph', paysys_id=1001)
    create_passport(session, [create_role(session, ALTER_INVOICE_PAYSYS)], client=None)
    new_paysys = session.query(mapper.Paysys).getone(1000)

    # де-факто _is_pcp_alterable_ya_conditions проверяет только оплату и акты,
    # на которые есть отдельны тесты, но для порядка проверим и с моком
    with mock.patch('balance.providers.invoice_alterable._is_pcp_alterable_ya_conditions', return_value=False):
        check_fail_depend_on_strict(func=is_pcp_alter_allowed, strict=strict,
                                    invoice=invoice, exc=exc.INVALID_PARAM,
                                    msg="Invalid parameter for function: Can't alter invoice's pcp",
                                    person=invoice.person, contract=invoice.contract, paysys=new_paysys)
