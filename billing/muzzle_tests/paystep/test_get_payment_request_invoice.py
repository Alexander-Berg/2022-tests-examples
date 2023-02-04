# -*- coding: utf-8 -*-
import pytest
import mock

from balance import constants as cst, exc
from balance.mapper.invoices import Request, Invoice
from tests.muzzle_tests.paystep.paystep_common import (create_request, create_client, create_order,
                            create_invoice, create_paysys, create_person,
                            create_currency, create_firm, create_price_tax_rate, ISSUE_INVOICES,
                            create_passport, create_role, VIEW_INVOICES, ALTER_INVOICE_PAYSYS,
                            ALTER_INVOICE_CONTRACT, ALTER_INVOICE_PERSON)
from tests import object_builder as ob


def create_role_w_constraints(session, *perms):
    permissions = [
        (perm, {cst.ConstraintTypes.firm_id: None, cst.ConstraintTypes.client_batch_id: None})
        for perm in perms
    ]
    return ob.create_role(session, *permissions)


@pytest.mark.parametrize(
    'additional_perm',
    [ALTER_INVOICE_CONTRACT, ALTER_INVOICE_PERSON, ALTER_INVOICE_PAYSYS],
)
@pytest.mark.parametrize(
    'match_client',
    [None, False, True],
)
def test_invoice_w_alter_perm(session, muzzle_logic, firm, currency, additional_perm, match_client):
    """если отрисовываем способы оплаты по счету для сотрудника Яндекса,
     ему нужны права на просмотр счета + любое право на изменение параметров счета"""
    client = create_client(session)

    roles = [create_role(session, VIEW_INVOICES)]
    if match_client is not None:
        role_client = ob.RoleClientBuilder.construct(session, client=client if match_client else None)
        roles.append(
            (create_role_w_constraints(session, additional_perm), None, role_client.client_batch_id)
        )
    create_passport(session, *roles, patch_session=True)

    order = create_order(session, client)
    request = create_request(session, client, order)
    person = create_person(session, client)
    paysys = create_paysys(session, firm=firm, iso_currency=currency.iso_code,
                           group_id=cst.PaysysGroupIDs.default, currency=currency.char_code,
                           category=person.person_category.category, instant=1)
    create_price_tax_rate(session, request.request_orders[0].order.product, firm.country, currency, price=1)
    invoice = create_invoice(session, request=request, paysys=paysys, person=person)

    if match_client:
        _, actual_invoice = muzzle_logic._get_payment_request_invoice(session, request_id=None, invoice_id=invoice.id)
        assert actual_invoice == invoice

    else:
        with pytest.raises(exc.CANNOT_PATCH_NONALTERABLE_INVOICE) as exc_info:
            muzzle_logic._get_payment_request_invoice(session, request_id=None, invoice_id=invoice.id)
        assert exc_info.value.msg == 'Cannot patch non-alterable invoice, invoice_id: %d' % invoice.id


def test_invoice_w_owner(session, muzzle_logic, firm, currency):
    """если отрисовывам способы оплаты по счету, который можно редактировать, для владельца счета
    никакие дополнительные права не нужны"""
    client = create_client(session)
    order = create_order(session, client)
    request = create_request(session, client, order)
    person = create_person(session, client)
    paysys = create_paysys(session, firm=firm, iso_currency=currency.iso_code,
                           group_id=cst.PaysysGroupIDs.default, currency=currency.char_code,
                           category=person.person_category.category, instant=1)
    create_price_tax_rate(session, request.request_orders[0].order.product, firm.country, currency, price=1)
    invoice = create_invoice(session, request=request, paysys=paysys, person=person)
    create_passport(session, create_role(session), patch_session=True, client=invoice.client)
    _, actual_invoice = muzzle_logic._get_payment_request_invoice(session, request_id=None, invoice_id=invoice.id)
    assert actual_invoice == invoice


@pytest.mark.parametrize(
    'match_client',
    [None, False, True],
)
def test_invoice_w_perm(session, muzzle_logic, firm, currency, match_client):
    """Для невладельца счета без нужного набора прав кидаем исключение"""
    client = create_client(session)

    roles = [create_role(session, ALTER_INVOICE_PAYSYS)]  # одно право для изменения
    if match_client is not None:
        role_client = ob.RoleClientBuilder.construct(session, client=client if match_client else None)
        roles.append(
            (create_role_w_constraints(session, VIEW_INVOICES), None, role_client.client_batch_id)
        )
    create_passport(session, *roles, patch_session=True)

    order = create_order(session, client)
    request = create_request(session, client, order)
    person = create_person(session, client)
    paysys = create_paysys(session, firm=firm, iso_currency=currency.iso_code,
                           group_id=cst.PaysysGroupIDs.default, currency=currency.char_code,
                           category=person.person_category.category, instant=1)
    create_price_tax_rate(session, request.request_orders[0].order.product, firm.country, currency, price=1)
    invoice = create_invoice(session, request=request, paysys=paysys, person=person)
    if match_client:
        _, actual_invoice = muzzle_logic._get_payment_request_invoice(session, request_id=None, invoice_id=invoice.id)
        assert actual_invoice == invoice

    else:
        with pytest.raises(exc.PERMISSION_DENIED):
            muzzle_logic._get_payment_request_invoice(session, request_id=None, invoice_id=invoice.id)


@pytest.mark.parametrize(
    'match_client',
    [None, False, True],
)
def test_request_w_perm(session, muzzle_logic, firm, currency, match_client):
    """если отрисовываем способы оплаты по реквесту для сотрудника Яндекса,
     ему нужны права на выставление счетов"""
    client = create_client(session)

    roles = []
    if match_client is not None:
        role_client = ob.RoleClientBuilder.construct(session, client=client if match_client else None)
        roles.append(
            (create_role_w_constraints(session, ISSUE_INVOICES), None, role_client.client_batch_id)
        )
    create_passport(session, *roles, patch_session=True)

    order = create_order(session, client)
    request = create_request(session, client, order)
    if match_client:
        actual_request, _ = muzzle_logic._get_payment_request_invoice(session, request_id=request.id, invoice_id=None)
        assert actual_request == request
    else:
        with pytest.raises(exc.PERMISSION_DENIED):
            muzzle_logic._get_payment_request_invoice(session, request_id=request.id, invoice_id=None)
