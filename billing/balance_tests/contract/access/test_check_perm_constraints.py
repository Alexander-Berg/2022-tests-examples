# -*- coding: utf-8 -*-
import pytest

from balance import constants as cst
from tests import object_builder as ob

from tests.balance_tests.contract.access.access_common import (
    PERMISSION,
    CheckType,
    check_perm_constraints_by_check_type,
    create_contract,
    create_role,
    create_role_client,
    create_firm_id,
    create_passport,
    create_right_role,
    create_wrong_role,
)

pytestmark = [
    pytest.mark.permissions,
    pytest.mark.parametrize(
        'check_type',
        [
            CheckType.object,
            CheckType.query,
        ],
    ),
]


@pytest.mark.parametrize(
    'role_func, role_firm_ids, ans',
    [
        pytest.param(create_wrong_role, [cst.FirmId.YANDEX_OOO], False,
                     id='wrong role'),
        pytest.param(create_right_role, [None], True,
                     id='right role wo constraints'),
        pytest.param(create_right_role, [cst.FirmId.CLOUD, cst.FirmId.AUTORU], False,
                     id='right role w wrong constraints'),
        pytest.param(create_right_role, [cst.FirmId.YANDEX_OOO, cst.FirmId.CLOUD], True,
                     id='right role w right constraints'),
    ],
)
def test_check_firm_constraint(session, check_type, role_func, role_firm_ids, ans):
    ob.set_roles(
        session,
        session.passport,
        [
            (role_func(session), {cst.ConstraintTypes.firm_id: firm_id})
            for firm_id in role_firm_ids
        ],
    )
    contract = create_contract(session)
    assert check_perm_constraints_by_check_type(session, contract, session.passport, check_type) is ans


@pytest.mark.parametrize(
    'role_func, ans',
    [
        pytest.param(create_right_role, True,
                     id='right_role'),
        pytest.param(create_wrong_role, False,
                     id='wrong_role'),
    ],
)
def test_check_contract_wo_firm(session, check_type, role_func, ans):
    """У договора нет фирмы => значит проверяем только наличие правильной роли"""
    ob.set_roles(
        session,
        session.passport,
        [(role_func(session), {cst.ConstraintTypes.firm_id: cst.FirmId.YANDEX_OOO})],
    )
    contract = create_contract(session, firm=None)
    session.flush()
    assert check_perm_constraints_by_check_type(session, contract, session.passport, check_type) is ans


def test_client_w_permission_wo_constraints(session, check_type, contract):
    role = create_role(session, (PERMISSION, {cst.ConstraintTypes.client_batch_id: None}))
    passport = create_passport(session, [role])
    assert check_perm_constraints_by_check_type(session, contract, passport, check_type) is True


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
        (PERMISSION, {cst.ConstraintTypes.client_batch_id: None, cst.ConstraintTypes.firm_id: None}),
    )
    if match_client is None:
        client_batch_id = None
    else:
        client_batch_id = role_client.client_batch_id if match_client else create_role_client(session).client_batch_id
    if match_firm is None:
        role_firm_id = None
    else:
        role_firm_id = firm_id if match_firm else cst.FirmId.TAXI
    passport = create_passport(session, [(role, role_firm_id, client_batch_id)])
    contract = create_contract(session, firm=firm_id, client=role_client.client)
    assert check_perm_constraints_by_check_type(session, contract, passport, check_type) is ans


def test_client_and_firm_in_different_constraints(session, role_client, firm_id, right_role, check_type):
    passport = create_passport(
        session,
        [
            (right_role, None, role_client.client_batch_id),
            (right_role, firm_id, None),
        ],
    )
    contract = create_contract(session, firm=firm_id, client=role_client.client)
    assert check_perm_constraints_by_check_type(session, contract, passport, check_type) is True


def test_client_and_firm_in_different_wrong_constraints(session, role_client, firm_id, check_type, right_role):
    passport = create_passport(
        session,
        [
            (right_role, create_firm_id(session), role_client.client_batch_id),
            (right_role, firm_id, create_role_client(session).client_batch_id),
        ],
    )
    contract = create_contract(session, firm=firm_id, client=role_client.client)
    assert check_perm_constraints_by_check_type(session, contract, passport, check_type) is False


def test_client_batch_id_doesnt_exist(session, role_client, firm_id, check_type, right_role):
    passport = create_passport(
        session,
        [
            (right_role, None, -666),
            (right_role, -666, None),
        ],
    )
    contract = create_contract(session, firm=firm_id, client=role_client.client)
    assert check_perm_constraints_by_check_type(session, contract, passport, check_type) is False


def test_hidden_role_client(session, check_type, role_client):
    role = ob.create_role(session, (PERMISSION, {cst.ConstraintTypes.client_batch_id: None}))
    role_client.hidden = True
    passport = ob.create_passport(
        session,
        (role, None, role_client.client_batch_id),
    )
    contract = create_contract(session, client=role_client.client)
    assert check_perm_constraints_by_check_type(session, contract, passport, check_type) is False
