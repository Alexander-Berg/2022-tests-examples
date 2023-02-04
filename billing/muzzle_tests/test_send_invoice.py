# -*- coding: utf-8 -*-

import pytest

from balance import mapper
from balance import exc
from balance.constants import (
    PermissionCode,
    INVOICE_FORM_CREATOR_MESSAGE_OPCODE,
)
from tests import object_builder as ob

PAYSYS_ID_BANK_UR = 1003
PAYSYS_ID_YM = 1000


@pytest.fixture
def invoice(session):
    return ob.InvoiceBuilder(
        paysys=ob.Getter(mapper.Paysys, PAYSYS_ID_BANK_UR)
    ).build(session).obj


@pytest.mark.permissions
@pytest.mark.parametrize('is_owner', [False, True])
def test_access_ok(muzzle_logic, session, invoice, is_owner):
    if is_owner:
        passport = ob.create_passport(session, patch_session=True)
        passport.client = invoice.client
    else:
        role = ob.create_role(session, PermissionCode.SEND_INVOICES)
        ob.create_passport(session, role, patch_session=True)
    session.flush()

    xml_res = muzzle_logic.send_invoice_ex(
        session,
        invoice.id,
        'test-user@yandex-team.ru',
        'mail_memo',
        None,
        INVOICE_FORM_CREATOR_MESSAGE_OPCODE
    )
    msg = session.query(mapper.EmailMessage).filter_by(object_id=invoice.id).one()

    assert msg.opcode == INVOICE_FORM_CREATOR_MESSAGE_OPCODE
    assert msg.recepient_address == 'test-user@yandex-team.ru'
    assert xml_res.attrib == {'invoice_id': str(invoice.id)}


@pytest.mark.permissions
def test_access_fail(muzzle_logic, session, invoice):
    role = ob.create_role(session, PermissionCode.ADMIN_ACCESS)
    ob.create_passport(session, role, patch_session=True)

    with pytest.raises(exc.PERMISSION_DENIED) as exc_info:
        muzzle_logic.send_invoice_ex(
            session,
            invoice.id,
            'test-user@yandex-team.ru',
            'mail_memo',
            None,
            INVOICE_FORM_CREATOR_MESSAGE_OPCODE
        )
    assert 'no permission SendInvoices' in str(exc_info.value)


def test_not_invoice_sendable(muzzle_logic, session):
    invoice = ob.InvoiceBuilder(
        paysys=ob.Getter(mapper.Paysys, PAYSYS_ID_YM)
    ).build(session).obj

    xml_res = muzzle_logic.send_invoice_ex(
        session,
        invoice.id,
        'test-user@yandex-team.ru',
        'mail_memo',
        None,
        INVOICE_FORM_CREATOR_MESSAGE_OPCODE
    )
    msgs = session.query(mapper.EmailMessage).filter_by(object_id=invoice.id).all()

    assert not msgs
    assert xml_res.attrib == {'invoice_id': str(invoice.id)}


def test_w_manager(muzzle_logic, session, invoice):
    manager_email = 'test-manager@yandex-team.ru'
    user_email = 'test-user@yandex-team.ru'
    invoice.manager.email = manager_email

    xml_res = muzzle_logic.send_invoice_ex(
        session,
        invoice.id,
        user_email,
        'mail_memo',
        True,
        INVOICE_FORM_CREATOR_MESSAGE_OPCODE
    )
    msgs = session.query(mapper.EmailMessage).filter_by(object_id=invoice.id).all()

    assert {m.recepient_address for m in msgs} == {manager_email, user_email}
    assert xml_res.attrib == {'invoice_id': str(invoice.id)}
