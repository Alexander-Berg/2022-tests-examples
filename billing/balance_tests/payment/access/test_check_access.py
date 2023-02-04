# -*- coding: utf-8 -*-
import pytest
import mock

from balance import (
    constants as cst,
    exc,
    mapper,
)

from tests import object_builder as ob
# noinspection PyUnresolvedReferences
from tests.balance_tests.payment.common import (
    create_passport,
    create_role,
    create_yamoney_role,
    create_client,
    create_role_client,
    create_card_payment,
    create_firm,
    create_view_payments_role,
    ADMIN_ACCESS,
)

pytestmark = [
    pytest.mark.permissions,
]


def test_nodoby(session):
    ob.set_roles(session, session.passport, [])
    payment = create_card_payment(session)

    with pytest.raises(exc.PERMISSION_DENIED):
        payment.check_access(session)


def test_client(session, client):
    ob.set_roles(session, session.passport, [])
    payment = create_card_payment(session)
    session.passport.client = payment.invoice.client
    session.flush()

    payment.check_access(session)


def test_wo_invoice_client(session):
    ob.set_roles(session, session.passport, [])
    payment = create_card_payment(session, invoice=None)

    with pytest.raises(exc.PERMISSION_DENIED):
        payment.check_access(session)


def test_repr_client(session, client):
    passport = session.passport
    repr_role = ob.create_role(session, cst.PermissionCode.REPRESENT_CLIENT)
    session.add(mapper.RoleClientPassport(passport=passport, role=repr_role, client=client))
    ob.set_roles(session, passport, [repr_role])
    session.flush()

    payment = create_card_payment(session)
    payment.service = None
    payment.invoice.client = client
    session.flush()

    payment.check_access(session)


def test_yamoney_support_simple(session, yamoney_role):
    """если все заказы в реквесте принадлежат простым сервисам и пользователь - саппорт ЯД, есть доступ к акту"""
    ob.set_roles(session, session.passport, [yamoney_role])
    payment = create_card_payment(session)
    with mock.patch('balance.mapper.common.Service.is_simple', True):
        payment.check_access(session)


def test_perm(session, view_payments_role):
    ob.set_roles(session, session.passport, [view_payments_role])
    payment = create_card_payment(session)

    payment.check_access(session)


def test_perm_firm(session, view_payments_role):
    firm_id = cst.FirmId.YANDEX_OOO
    ob.set_roles(session, session.passport, [(view_payments_role, {cst.ConstraintTypes.firm_id: cst.FirmId.YANDEX_OOO})])

    payment = create_card_payment(session)
    payment.firm_id = firm_id
    session.flush()

    payment.check_access(session)


def test_perm_wrong_firm(session, view_payments_role):
    ob.set_roles(session, session.passport, [(view_payments_role, {cst.ConstraintTypes.firm_id: cst.FirmId.YANDEX_OOO})])

    payment = create_card_payment(session)
    payment.firm_id = cst.FirmId.DRIVE
    session.flush()

    with pytest.raises(exc.PERMISSION_DENIED):
        payment.check_access(session)


def test_payment_wo_firm(session, view_payments_role):
    ob.set_roles(session, session.passport, [(view_payments_role, {cst.ConstraintTypes.firm_id: cst.FirmId.YANDEX_OOO})])
    payment = create_card_payment(session)
    payment.firm_id = None
    session.flush()

    payment.check_access(session)


def test_perm_w_clients(session, role_client, view_payments_role):
    payment = create_card_payment(session, client=role_client.client)
    passport = ob.create_passport(session, (view_payments_role, None, role_client.client_batch_id), patch_session=True)
    payment.check_access(session)


def test_perm_w_clients_w_firm(session, role_client, firm, view_payments_role):
    passport = ob.create_passport(
        session,
        (view_payments_role, firm.id, role_client.client_batch_id),
        (view_payments_role, None, create_role_client(session).client_batch_id),
        patch_session = True,
    )
    payment = create_card_payment(session, firm_id=firm.id, client=role_client.client)
    payment.check_access(session)


def test_perm_w_clients_w_firm_in_different_roles_access(session, role_client, firm, view_payments_role):
    passport = ob.create_passport(
        session,
        (view_payments_role, None, role_client.client_batch_id),
        (view_payments_role, firm.id, None),
        patch_session=True,
    )
    payment = create_card_payment(session, firm_id=firm.id, client=role_client.client)
    payment.check_access(session)


def test_perm_w_clients_w_firm_in_different_roles_failed(session, role_client, firm, view_payments_role):
    passport = ob.create_passport(
        session,
        (view_payments_role, create_firm(session).id, role_client.client_batch_id),
        (view_payments_role, firm.id, ob.get_big_number()),
        patch_session=True,
    )
    payment = create_card_payment(session, firm_id=firm.id, client=role_client.client)
    with pytest.raises(exc.PERMISSION_DENIED):
        payment.check_access(session)


def test_perm_wrong_client(session, role_client, view_payments_role):
    passport = ob.create_passport(session, (view_payments_role, None, create_role_client(session).client_batch_id), patch_session=True)
    payment = create_card_payment(session, client=role_client.client)
    with pytest.raises(exc.PERMISSION_DENIED):
        payment.check_access(session)
