# -*- coding: utf-8 -*-
import pytest

from balance import constants as cst

from tests import object_builder as ob
# noinspection PyUnresolvedReferences
from tests.balance_tests.payment.common import (
    create_role,
    create_role_client,
    create_passport,
    create_firm,
    create_invoice,
    create_card_payment,
    check_perm_constraints_by_check_type,
    CheckType,
    PERMISSION,
)


pytestmark = [
    pytest.mark.permissions,
    pytest.mark.parametrize('check_type', [CheckType.object, CheckType.query]),
]


def test_wo_permission(session, check_type):
    ob.set_roles(session, session.passport, [])
    payment = create_card_payment(session)
    assert check_perm_constraints_by_check_type(session, payment, session.passport, check_type) is False


def test_w_permission_wo_constraints(session, check_type):
    role = create_role(session, PERMISSION)
    passport = create_passport(session, role, patch_session=True)
    payment = create_card_payment(session)

    assert check_perm_constraints_by_check_type(session, payment, passport, check_type) is True


def test_firm(session, check_type):
    role = create_role(session, (PERMISSION, {cst.ConstraintTypes.firm_id: None}))
    payment = create_card_payment(session, firm_id=cst.FirmId.YANDEX_OOO)
    passport = create_passport(session, (role, cst.FirmId.YANDEX_OOO))

    assert check_perm_constraints_by_check_type(session, payment, passport, check_type) is True


def test_no_firm(session, check_type):
    role = create_role(session, (PERMISSION, {cst.ConstraintTypes.firm_id: None}))
    passport = create_passport(
        session,
        (role, cst.FirmId.YANDEX_OOO),
        patch_session=True
    )
    payment = create_card_payment(session, firm_id=None)

    assert check_perm_constraints_by_check_type(session, payment, passport, check_type) is True


def test_wrong_firm(session, check_type):
    role = create_role(session, (PERMISSION, {cst.ConstraintTypes.firm_id: None}))
    passport = create_passport(
        session,
        (role, cst.FirmId.YANDEX_OOO),
        patch_session=True
    )
    payment = create_card_payment(session, firm_id=cst.FirmId.TAXI)

    assert check_perm_constraints_by_check_type(session, payment, passport, check_type) is False


def test_multiple_firms(session, check_type):
    role = create_role(session, (PERMISSION, {cst.ConstraintTypes.firm_id: None}))
    passport = create_passport(
        session,
        (role, cst.FirmId.YANDEX_OOO),
        (role, cst.FirmId.TAXI),
        patch_session=True
    )
    payment = create_card_payment(session, firm_id=cst.FirmId.TAXI)

    assert check_perm_constraints_by_check_type(session, payment, passport, check_type) is True


def test_multiple_firms_no_firm(session, check_type):
    role = create_role(session, (PERMISSION, {cst.ConstraintTypes.firm_id: None}))
    passport = create_passport(
        session,
        (role, cst.FirmId.YANDEX_OOO),
        (role, cst.FirmId.TAXI),
        patch_session=True

    )
    payment = create_card_payment(session, firm_id=None)

    assert check_perm_constraints_by_check_type(session, payment, passport, check_type) is True


def test_client_w_permission_wo_constraints(session, invoice, check_type):
    role = create_role(session, (PERMISSION, {cst.ConstraintTypes.client_batch_id: None}))
    passport = create_passport(session, (role,), patch_session=True)
    payment = create_card_payment(session, invoice=invoice)
    assert check_perm_constraints_by_check_type(session, payment, passport, check_type) is True


def test_client_w_right_permission_constraint(session, role_client, check_type):
    role = create_role(session, (PERMISSION, {cst.ConstraintTypes.client_batch_id: [role_client.client_batch_id, 666]}))
    passport = create_passport(session, (role,), patch_session=True)
    payment = create_card_payment(session, client=role_client.client)
    assert check_perm_constraints_by_check_type(session, payment, passport, check_type) is True


@pytest.mark.parametrize(
    'match_client, match_firm, ans',
    [
        pytest.param(True, True, True, id='w right client w right firm'),
        pytest.param(True, None, True, id='w right client wo firm'),
        pytest.param(True, False, False, id='w right client w wrong firm'),
        pytest.param(None, True, True, id='wo client w right firm'),
        pytest.param(False, True, False, id='w wrong client w right firm'),
    ],
)
def test_w_constraints(
        session,
        role_client,
        check_type,
        firm,
        match_client,
        match_firm,
        ans,
):
    role = create_role(
        session,
        (PERMISSION, {cst.ConstraintTypes.client_batch_id: None, cst.ConstraintTypes.firm_id: None}),
    )
    if match_client is None:
        client_batch_id = None
    else:
        client_batch_id = role_client.client_batch_id if match_client else create_role_client(session).client_batch_id
    if match_firm is None:
        role_firm_id = None
    else:
        role_firm_id = firm.id if match_firm else cst.FirmId.TAXI
    passport = create_passport(session, (role, role_firm_id, client_batch_id))
    payment = create_card_payment(session, firm_id=firm.id, client=role_client.client)
    assert check_perm_constraints_by_check_type(session, payment, passport, check_type) is ans


def test_client_and_firm_in_different_constraints(session, role_client, firm, check_type):
    role = create_role(
        session,
        (PERMISSION, {cst.ConstraintTypes.client_batch_id: None, cst.ConstraintTypes.firm_id: None}),
    )
    passport = create_passport(
        session,
        (role, None, role_client.client_batch_id),
        (role, firm.id, None),
    )
    payment = create_card_payment(session, firm_id=firm.id, client=role_client.client)
    assert check_perm_constraints_by_check_type(session, payment, passport, check_type) is True


def test_client_and_firm_in_different_wrong_constraints(session, role_client, firm, check_type):
    role = create_role(
        session,
        (PERMISSION, {cst.ConstraintTypes.client_batch_id: None, cst.ConstraintTypes.firm_id: None}),
    )
    passport = create_passport(
        session,
        (role, create_firm(session).id, role_client.client_batch_id),
        (role, firm.id, create_role_client(session).client_batch_id),
    )
    payment = create_card_payment(session, firm_id=firm.id, client=role_client.client)
    assert check_perm_constraints_by_check_type(session, payment, passport, check_type) is False


def test_w_empty_role(session, role_client, firm, check_type):
    role = create_role(
        session,
        (PERMISSION, {cst.ConstraintTypes.client_batch_id: None, cst.ConstraintTypes.firm_id: None}),
    )
    passport = create_passport(
        session,
        (role, firm.id, role_client.client_batch_id),
        role,
    )
    payment = create_card_payment(session)
    assert check_perm_constraints_by_check_type(session, payment, passport, check_type) is True


def test_hidden_role_client(session, check_type, role_client):
    role = create_role(session, (PERMISSION, {cst.ConstraintTypes.client_batch_id: None}))
    role_client.hidden = True
    passport = create_passport(
        session,
        (role, None, role_client.client_batch_id),
    )
    payment = create_card_payment(session)
    assert check_perm_constraints_by_check_type(session, payment, passport, check_type) is False


@pytest.mark.parametrize(
    'w_constr, w_client, ans',
    [
        pytest.param(False, True, True, id='wo constraint, w client'),
        pytest.param(True, False, True, id='w constraint, wo client'),
        pytest.param(True, True, False, id='w constraint, w client'),
    ],
)
def test_payment_wo_invoice(session, check_type, role_client, w_constr, w_client, ans):
    params = {cst.ConstraintTypes.client_batch_id: None} if w_constr else {}
    role = create_role(session, (PERMISSION, params))
    role = (role, None, role_client.client_batch_id) if w_client else role
    passport = create_passport(
        session,
        role,
    )
    payment = create_card_payment(session)
    payment.invoice = None
    session.flush()
    assert check_perm_constraints_by_check_type(session, payment, passport, check_type) is ans

