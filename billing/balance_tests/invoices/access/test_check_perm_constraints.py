# -*- coding: utf-8 -*-
import pytest
import mock

from balance import mapper
from balance.constants import (
    ConstraintTypes,
    FirmId,
)

from tests.balance_tests.invoices.access.access_common import (
    create_role,
    create_passport,
    create_manager,
    PERMISSION,
    CheckType,
    create_invoice,
    check_perm_constraints_by_check_type,
    create_firm_id,
    create_role_client,
)

pytestmark = [
    pytest.mark.permissions,
    pytest.mark.parametrize('check_type', [CheckType.object, CheckType.query])
]


def test_wo_permission(session, check_type):
    role = create_role(session)
    passport = create_passport(session, [role])
    invoice = create_invoice(session)

    assert check_perm_constraints_by_check_type(session, invoice, passport, check_type) is False


def test_w_permission_wo_constraints(session, check_type):
    role = create_role(session, PERMISSION)
    passport = create_passport(session, [role])
    invoice = create_invoice(session)

    assert check_perm_constraints_by_check_type(session, invoice, passport, check_type) is True


def test_firm(session, check_type):
    role = create_role(session, (PERMISSION, {ConstraintTypes.firm_id: None}))
    passport = create_passport(session, [(role, FirmId.YANDEX_OOO)])
    invoice = create_invoice(session, FirmId.YANDEX_OOO)

    assert check_perm_constraints_by_check_type(session, invoice, passport, check_type) is True


def test_multiple_firms(session, check_type):
    role = create_role(session, (PERMISSION, {ConstraintTypes.firm_id: None}))
    passport = create_passport(
        session,
        [(role, FirmId.YANDEX_OOO),
         (role, FirmId.TAXI)],
    )
    invoice = create_invoice(session, FirmId.TAXI)

    assert check_perm_constraints_by_check_type(session, invoice, passport, check_type) is True


def test_wrong_firm(session, check_type):
    role = create_role(session, (PERMISSION, {ConstraintTypes.firm_id: None}))
    passport = create_passport(
        session,
        [(role, FirmId.YANDEX_OOO)],
    )
    invoice = create_invoice(session, FirmId.TAXI)

    assert check_perm_constraints_by_check_type(session, invoice, passport, check_type) is False


def test_manager(session, check_type):
    role = create_role(session, (PERMISSION, {ConstraintTypes.manager: 1}))
    passport = create_passport(session, [role])
    manager = create_manager(session, passport)
    invoice = create_invoice(session, manager=manager)

    assert check_perm_constraints_by_check_type(session, invoice, passport, check_type) is True


def test_wrong_manager(session, check_type):
    req_result = {CheckType.object: False, CheckType.query: True}[check_type]
    role = create_role(session, (PERMISSION, {ConstraintTypes.manager: 1}))
    passport = create_passport(session, [role])
    create_manager(session, passport)
    wrong_manager = create_manager(session)
    invoice = create_invoice(session, manager=wrong_manager)

    assert check_perm_constraints_by_check_type(session, invoice, passport, check_type) is req_result


def test_no_manager(session, check_type):
    req_result = {CheckType.object: False, CheckType.query: True}[check_type]
    role = create_role(session, (PERMISSION, {ConstraintTypes.manager: 1}))
    passport = create_passport(session, [role])
    wrong_manager = create_manager(session)
    invoice = create_invoice(session, manager=wrong_manager)

    assert check_perm_constraints_by_check_type(session, invoice, passport, check_type) is req_result


def test_manager_firm(session, check_type):
    role = create_role(
        session,
        (
            PERMISSION,
            {
                ConstraintTypes.firm_id: None,
                ConstraintTypes.manager: 1,
            }
        )
    )
    passport = create_passport(session, [(role, FirmId.YANDEX_OOO)])
    manager = create_manager(session, passport)
    invoice = create_invoice(session, FirmId.YANDEX_OOO, manager)

    assert check_perm_constraints_by_check_type(session, invoice, passport, check_type) is True


def test_manager_wrong_firm(session, check_type):
    role = create_role(
        session,
        (
            PERMISSION,
            {
                ConstraintTypes.firm_id: None,
                ConstraintTypes.manager: 1,
            }
        )
    )
    passport = create_passport(session, [(role, FirmId.YANDEX_OOO)])
    manager = create_manager(session, passport)
    invoice = create_invoice(session, FirmId.TAXI, manager)

    assert check_perm_constraints_by_check_type(session, invoice, passport, check_type) is False


def test_wrong_manager_firm(session, check_type):
    req_result = {CheckType.object: False, CheckType.query: True}[check_type]
    role = create_role(
        session,
        (PERMISSION,
         {
             ConstraintTypes.firm_id: None,
             ConstraintTypes.manager: 1,
         })
    )
    passport = create_passport(session, [(role, FirmId.YANDEX_OOO)])
    create_manager(session, passport)
    wrong_manager = create_manager(session)
    invoice = create_invoice(session, FirmId.YANDEX_OOO, wrong_manager)

    assert check_perm_constraints_by_check_type(session, invoice, passport, check_type) is req_result


def test_nested_empty_firm(session, check_type):
    role1 = create_role(session, PERMISSION)
    role2 = create_role(session, (PERMISSION, {ConstraintTypes.firm_id: None}))
    passport = create_passport(
        session,
        [role1,
         (role2, FirmId.YANDEX_OOO)],
    )
    invoice = create_invoice(session, FirmId.TAXI)

    assert check_perm_constraints_by_check_type(session, invoice, passport, check_type) is True


def test_nested_firm_managerfirm(session, check_type):
    role1 = create_role(
        session,
        (PERMISSION,
         {
             ConstraintTypes.firm_id: None
         })
    )
    role2 = create_role(
        session,
        (PERMISSION,
         {
             ConstraintTypes.firm_id: None,
             ConstraintTypes.manager: 1
         })
    )
    passport = create_passport(
        session,
        [(role1, FirmId.YANDEX_OOO),
         (role2, FirmId.YANDEX_OOO)],
    )

    create_manager(session, passport)
    wrong_manager = create_manager(session)
    invoice = create_invoice(session, FirmId.YANDEX_OOO, wrong_manager)

    assert check_perm_constraints_by_check_type(session, invoice, passport, check_type) is True


def test_client_w_permission_wo_constraints(session, invoice, check_type):
    role = create_role(session, (PERMISSION, {ConstraintTypes.client_batch_id: None}))
    passport = create_passport(session, [role])
    assert check_perm_constraints_by_check_type(session, invoice, passport, check_type) is True


def test_client_w_right_permission_constraint(session, role_client, check_type):
    role = create_role(session, (PERMISSION, {ConstraintTypes.client_batch_id: [role_client.client_batch_id, 666]}))
    passport = create_passport(session, [role])
    invoice = create_invoice(session, client=role_client.client)
    assert check_perm_constraints_by_check_type(session, invoice, passport, check_type) is True


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
        firm_id,
        match_client,
        match_firm,
        ans,
):
    role = create_role(
        session,
        (PERMISSION, {ConstraintTypes.client_batch_id: None, ConstraintTypes.firm_id: None}),
    )
    if match_client is None:
        client_batch_id = None
    else:
        client_batch_id = role_client.client_batch_id if match_client else create_role_client(session).client_batch_id
    if match_firm is None:
        role_firm_id = None
    else:
        role_firm_id = firm_id if match_firm else FirmId.TAXI
    passport = create_passport(session, [(role, role_firm_id, client_batch_id)])
    invoice = create_invoice(session, firm_id=firm_id, client=role_client.client)
    assert check_perm_constraints_by_check_type(session, invoice, passport, check_type) is ans


def test_client_and_firm_in_different_constraints(session, role_client, firm_id, check_type):
    role = create_role(
        session,
        (PERMISSION, {ConstraintTypes.client_batch_id: None, ConstraintTypes.firm_id: None}),
    )
    passport = create_passport(
        session,
        [
            (role, None, role_client.client_batch_id),
            (role, firm_id, None),
        ],
    )
    invoice = create_invoice(session, firm_id=firm_id, client=role_client.client)
    assert check_perm_constraints_by_check_type(session, invoice, passport, check_type) is True


def test_client_and_firm_in_different_wrong_constraints(session, role_client, firm_id, check_type):
    role = create_role(
        session,
        (PERMISSION, {ConstraintTypes.client_batch_id: None, ConstraintTypes.firm_id: None}),
    )
    passport = create_passport(
        session,
        [
            (role, create_firm_id(session), role_client.client_batch_id),
            (role, firm_id, create_role_client(session).client_batch_id),
        ],
    )
    invoice = create_invoice(session, firm_id=firm_id, client=role_client.client)
    assert check_perm_constraints_by_check_type(session, invoice, passport, check_type) is False


def test_w_empty_role(session, role_client, firm_id, check_type):
    role = create_role(
        session,
        (PERMISSION, {ConstraintTypes.client_batch_id: None, ConstraintTypes.firm_id: None}),
    )
    passport = create_passport(
        session,
        [
            (role, firm_id, role_client.client_batch_id),
            role,
        ],
    )
    invoice = create_invoice(session)
    assert check_perm_constraints_by_check_type(session, invoice, passport, check_type) is True


def test_hidden_role_client(session, check_type, role_client):
    role = create_role(session, (PERMISSION, {ConstraintTypes.client_batch_id: None}))
    role_client.hidden = True
    passport = create_passport(
        session,
        [(role, None, role_client.client_batch_id)],
    )
    invoice = create_invoice(session)
    assert check_perm_constraints_by_check_type(session, invoice, passport, check_type) is False
