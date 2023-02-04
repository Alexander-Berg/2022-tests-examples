# -*- coding: utf-8 -*-
import pytest

from balance import constants as cst, exc

from tests import object_builder as ob
from tests.balance_tests.act.access.access_common import (
    is_allowed,
    CheckType,
    create_act,
    PERMISSION,
    create_role,
    create_role_client,
    create_passport,
    INVOICE_FIRM_ID,
)

pytestmark = [
    pytest.mark.permissions,
    pytest.mark.parametrize('check_type', [CheckType.object, CheckType.query]),
]


def test_wo_permission(session, check_type):
    role = ob.create_role(session)
    passport = ob.create_passport(session, role)
    act = create_act(session)

    assert is_allowed(act, passport, check_type) is False


def test_w_permission_wo_constraints(session, check_type):
    role = ob.create_role(session, PERMISSION)
    passport = ob.create_passport(session, role)
    act = create_act(session)

    assert is_allowed(act, passport, check_type) is True


def test_w_permission_wo_constraints_w_role_constraint(session, check_type):
    """Право не ограничено, а роль пользователя имеет ограничения
    => разрешенодаже не смотря на то, что у акта фирма отличается от ограничения в роли"""
    role = ob.create_role(session, PERMISSION)
    passport = ob.create_passport(session, (role, cst.FirmId.CLOUD))
    act = create_act(session, cst.FirmId.YANDEX_OOO)

    assert is_allowed(act, passport, check_type) is True


def test_firm(session, check_type):
    """У клиента есть доступ к фирме, которая указана в счёте акта."""
    role = ob.create_role(session, (PERMISSION, {cst.ConstraintTypes.firm_id: None}))
    passport = ob.create_passport(session, (role, cst.FirmId.YANDEX_OOO))
    act = create_act(session, cst.FirmId.YANDEX_OOO)

    assert is_allowed(act, passport, check_type) is True


def test_wrong_firm(session, check_type):
    """У клиента есть доступ к фирме, но в акте указана другая."""
    role = ob.create_role(session, (PERMISSION, {cst.ConstraintTypes.firm_id: None}))
    passport = ob.create_passport(
        session,
        (role, cst.FirmId.YANDEX_OOO),
    )
    act = create_act(session, cst.FirmId.TAXI)
    assert is_allowed(act, passport, check_type) is False


def test_multiple_firms(session, check_type):
    """У клиента доступ к нескольким фирмам.
    Одна из них проходи проверку."""
    role = ob.create_role(session, (PERMISSION, {cst.ConstraintTypes.firm_id: None}))
    passport = ob.create_passport(
        session,
        (role, cst.FirmId.YANDEX_OOO),
        (role, cst.FirmId.TAXI),
    )
    act = create_act(session, cst.FirmId.TAXI)
    assert is_allowed(act, passport, check_type) is True


@pytest.mark.parametrize(
    'firm_id, ans',
    [
        (cst.FirmId.YANDEX_OOO, True),
        (cst.FirmId.CLOUD, False),
    ],
    ids=['right_constraint', 'wrong_constraint'],
)
def test_permission_constraints(session, check_type, firm_id, ans):
    """Если у пользователя нет ограничений по роли, то для проверки берутся дефолтные из паспорта"""
    role = ob.create_role(session, (PERMISSION, {cst.ConstraintTypes.firm_id: (cst.FirmId.YANDEX_OOO,)}))
    passport = ob.create_passport(session, role)
    act = create_act(session, firm_id)
    assert is_allowed(act, passport, check_type) is ans


def test_constraints_priority(session, check_type):
    """Ограничения, которые пользователь выбрал себе в IDM, имеют больший приоритет,
    чем те, чтозаписаны у права"""
    role = ob.create_role(session, (PERMISSION, {cst.ConstraintTypes.firm_id: (cst.FirmId.YANDEX_OOO,)}))
    passport = ob.create_passport(session, (role, cst.FirmId.CLOUD))
    act = create_act(session, cst.FirmId.YANDEX_OOO)
    assert is_allowed(act, passport, check_type) is False


def test_client_w_permission_wo_constraints(session, act, check_type):
    role = create_role(session, (PERMISSION, {cst.ConstraintTypes.client_batch_id: None}))
    passport = create_passport(session, [role])
    assert is_allowed(act, passport, check_type) is True


def test_client_w_right_permission_constraint(session, role_client, check_type):
    role = create_role(session, (PERMISSION, {cst.ConstraintTypes.client_batch_id: [role_client.client_batch_id, 666]}))
    passport = create_passport(session, [role])
    act = create_act(session, client=role_client.client)
    assert is_allowed(act, passport, check_type) is True


@pytest.mark.parametrize(
    'match_client, role_firm_id, ans',
    [
        pytest.param(True, INVOICE_FIRM_ID, True, id='w right client w right firm'),
        pytest.param(True, None, True, id='w right client wo firm'),
        pytest.param(True, cst.FirmId.TAXI, False, id='w right client w wrong firm'),
        pytest.param(None, INVOICE_FIRM_ID, True, id='wo client w right firm'),
        pytest.param(False, INVOICE_FIRM_ID, False, id='w wrong client w right firm'),
    ],
)
def test_w_constraints(
        session,
        role_client,
        check_type,
        match_client,
        role_firm_id,
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
    passport = create_passport(session, [(role, role_firm_id, client_batch_id)])
    act = create_act(session, firm_id=INVOICE_FIRM_ID, client=role_client.client)
    assert is_allowed(act, passport, check_type) is ans


def test_client_and_firm_in_different_constraints(session, role_client, check_type):
    role = create_role(
        session,
        (PERMISSION, {cst.ConstraintTypes.client_batch_id: None, cst.ConstraintTypes.firm_id: None}),
    )
    passport = create_passport(
        session,
        [
            (role, None, role_client.client_batch_id),
            (role, INVOICE_FIRM_ID, None),
        ],
    )
    act = create_act(session, firm_id=INVOICE_FIRM_ID, client=role_client.client)
    assert is_allowed(act, passport, check_type) is True


def test_client_and_firm_in_different_wrong_constraints(session, role_client, check_type):
    role = create_role(
        session,
        (PERMISSION, {cst.ConstraintTypes.client_batch_id: None, cst.ConstraintTypes.firm_id: None}),
    )
    passport = create_passport(
        session,
        [
            (role, cst.FirmId.TAXI, role_client.client_batch_id),
            (role, INVOICE_FIRM_ID, create_role_client(session).client_batch_id),
        ],
    )
    act = create_act(session, firm_id=INVOICE_FIRM_ID, client=role_client.client)
    assert is_allowed(act, passport, check_type) is False


def test_hidden_role_client(session, check_type, role_client):
    role = ob.create_role(session, (PERMISSION, {cst.ConstraintTypes.client_batch_id: None}))
    role_client.hidden = True
    passport = ob.create_passport(
        session,
        (role, None, role_client.client_batch_id),
    )
    act = create_act(session, firm_id=INVOICE_FIRM_ID, client=role_client.client)
    assert is_allowed(act, passport, check_type) is False
