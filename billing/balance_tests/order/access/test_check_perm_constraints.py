# -*- coding: utf-8 -*-
import pytest

from balance import constants as cst
from tests import object_builder as ob

from tests.balance_tests.order.access.access_common import (
    PERMISSION,
    CheckType,
    check_perm_constraints_by_check_type,
    create_order,
    create_order_w_firms,
    create_client,
    create_agency,
    create_role_client,
    create_passport,
    create_right_role,
    create_wrong_role,
    ORDER_FIRM_IDS,
)

pytestmark = [
    pytest.mark.permissions,
    pytest.mark.parametrize('check_type', [
        CheckType.object,
        CheckType.query,
    ]),
]


@pytest.mark.parametrize(
    'order_func, role_func, role_firm_ids, ans',
    [
        pytest.param(create_order_w_firms, create_wrong_role, [None], False,
                     id='wrong permission'),
        pytest.param(create_order_w_firms, create_right_role, [None], True,
                     id='right permission wo constraints'),
        pytest.param(create_order_w_firms, create_right_role, [ORDER_FIRM_IDS[0]], False,
                     id='order w 2 firm, role w 1 firm'),
        pytest.param(create_order_w_firms, create_right_role, ORDER_FIRM_IDS, True,
                     id='order\'s firms match role\'s firms'),
        pytest.param(create_order_w_firms, create_right_role, [ORDER_FIRM_IDS[0]] + [cst.FirmId.AUTORU], False,
                     id='order\'s firms don\'t match role\'s firms'),
        pytest.param(create_order, create_wrong_role, [None], False,
                     id='order wo firms, wrong permission'),
        pytest.param(create_order, create_right_role, [None], True,
                     id='order wo firms, right permission'),
    ],
)
def test_check_firm_permission(session, check_type, order_func, role_func, role_firm_ids, ans):
    role = role_func(session)
    ob.set_roles(
        session,
        session.passport,
        [
            (role, {cst.ConstraintTypes.firm_id: firm_id})
            for firm_id in role_firm_ids
        ],
    )
    assert check_perm_constraints_by_check_type(session, order_func(session), session.passport, check_type) is ans


def test_different_roles(session, check_type, order_w_firms):
    """У заказа 2 фирмы. У пользователя 2 роли, у каждой по фирмы.
    В сумме фирмы у пользователя совпадают с фирмами в заказе,
    => доступ есть.
    """
    role1 = create_right_role(session)
    role2 = create_right_role(session)
    role_firm_ids = [
        (role1, ORDER_FIRM_IDS[0]),
        (role2, ORDER_FIRM_IDS[1]),
        (role2, cst.FirmId.AUTORU),
    ]
    ob.set_roles(
        session,
        session.passport,
        [
            (role, {cst.ConstraintTypes.firm_id: firm_id})
            for role, firm_id in role_firm_ids
        ],
    )
    assert check_perm_constraints_by_check_type(session, order_w_firms, session.passport, check_type) is True


def test_client_w_permission_wo_constraints(session, check_type, order, right_role):
    passport = create_passport(session, [right_role])
    assert check_perm_constraints_by_check_type(session, order, passport, check_type) is True


@pytest.mark.parametrize(
    'is_agency',
    [
        pytest.param(True, id='agency'),
        pytest.param(False, id='client'),
    ],
)
@pytest.mark.parametrize(
    'match_client, order_func, role_firm_ids, ans',
    [
        pytest.param(True, create_order_w_firms, ORDER_FIRM_IDS, True, id='w right client w right firm'),
        pytest.param(True, create_order_w_firms, [None], True, id='w right client wo role_firm'),
        pytest.param(True, create_order, ORDER_FIRM_IDS, True, id='w right client wo order_firm'),
        pytest.param(True, create_order_w_firms, [cst.FirmId.MARKET, cst.FirmId.DRIVE], False, id='w right client w wrong role_firm'),
        pytest.param(True, create_order_w_firms, [cst.FirmId.YANDEX_OOO, cst.FirmId.DRIVE], False, id='w right client w wrong role_firm 2'),
        pytest.param(True, create_order_w_firms, [cst.FirmId.YANDEX_OOO], False, id='w right client w 1 role_firm'),
        pytest.param(None, create_order_w_firms, ORDER_FIRM_IDS, True, id='wo client w right firm'),
        pytest.param(False, create_order_w_firms, ORDER_FIRM_IDS, False, id='w wrong client w right firm'),
    ],
)
def test_w_constraints(
        session,
        agency,
        client,
        check_type,
        right_role,
        is_agency,
        match_client,
        order_func,
        role_firm_ids,
        ans,
):
    """Разные сочетания клиента-фирмы для заказа с фирмами"""
    params = {'client': client}
    if is_agency:
        params['agency'] = agency
    order = order_func(session, **params)

    if match_client is None:
        client_batch_id = None
    else:
        perm_client = (agency if is_agency else client) if match_client else None
        client_batch_id = create_role_client(session, client=perm_client).client_batch_id
    roles = [
        (right_role, firm_id, client_batch_id)
        for firm_id in role_firm_ids
    ]
    passport = create_passport(session, roles)
    assert check_perm_constraints_by_check_type(session, order, passport, check_type) is ans


@pytest.mark.parametrize(
    'is_agency',
    [
        pytest.param(True, id='agency'),
        pytest.param(False, id='client'),
    ],
)
def test_access_only_for_agency(session, check_type, agency, client, right_role, is_agency):
    client_batch_id = create_role_client(session, client=agency if is_agency else client).client_batch_id
    roles = [
        (right_role, None, client_batch_id),
    ]
    passport = create_passport(session, roles)
    order = create_order(session, client=client, agency=agency)
    assert check_perm_constraints_by_check_type(session, order, passport, check_type) is is_agency


def test_client_w_right_permission_constraint(session, role_client, check_type):
    role = ob.create_role(session, (PERMISSION, {cst.ConstraintTypes.client_batch_id: [role_client.client_batch_id, 666]}))
    passport = create_passport(session, [role])
    order = create_order(session, client=role_client.client)
    assert check_perm_constraints_by_check_type(session, order, passport, check_type) is True


def test_client_and_firm_in_different_constraints(session, role_client, right_role, check_type):
    passport = create_passport(
        session,
        [
            (right_role, None, role_client.client_batch_id),
            (right_role, ORDER_FIRM_IDS[0], None),
            (right_role, ORDER_FIRM_IDS[1], None),
        ],
    )
    order = create_order_w_firms(session, client=role_client.client)
    assert check_perm_constraints_by_check_type(session, order, passport, check_type) is True


def test_client_batch_id_doesnt_exist(session, role_client, right_role, check_type):
    passport = create_passport(
        session,
        [
            (right_role, None, -666),
            (right_role, ORDER_FIRM_IDS[0], None),
        ],
    )
    order = create_order_w_firms(session, client=role_client.client)
    assert check_perm_constraints_by_check_type(session, order, passport, check_type) is False


def test_client_doesnt_have_client_batch_id(session, right_role, check_type):
    passport = create_passport(
        session,
        [
            (right_role, None, -666),
        ],
    )
    order = create_order_w_firms(session)
    assert check_perm_constraints_by_check_type(session, order, passport, check_type) is False


def test_order_firm_id_doesnt_exist(session, role_client, right_role, check_type):
    passport = create_passport(
        session,
        [
            (right_role, None, role_client.client_batch_id),
            (right_role, ORDER_FIRM_IDS[0], None),
            (right_role, ORDER_FIRM_IDS[1], None),
        ],
    )
    order = create_order(session, client=role_client.client)
    assert check_perm_constraints_by_check_type(session, order, passport, check_type) is True


def test_only_one_firm_match_w_client(session, role_client, right_role, check_type):
    passport = create_passport(
        session,
        [
            (right_role, ORDER_FIRM_IDS[0], role_client.client_batch_id),
            (right_role, ORDER_FIRM_IDS[1], None),
        ],
    )
    order = create_order_w_firms(session, client=role_client.client)
    assert check_perm_constraints_by_check_type(session, order, passport, check_type) is True


def test_one_firm_match_w_different_client(session, role_client, right_role, check_type):
    passport = create_passport(
        session,
        [
            (right_role, ORDER_FIRM_IDS[0], role_client.client_batch_id),
            (right_role, ORDER_FIRM_IDS[1], create_role_client(session).client_batch_id),
        ],
    )
    order = create_order_w_firms(session, client=role_client.client)
    assert check_perm_constraints_by_check_type(session, order, passport, check_type) is False


def test_one_firm_in_wrong_role(session, role_client, right_role, wrong_role, check_type):
    passport = create_passport(
        session,
        [
            (right_role, ORDER_FIRM_IDS[0], role_client.client_batch_id),
            (wrong_role, ORDER_FIRM_IDS[1], role_client.client_batch_id),
        ],
    )
    order = create_order_w_firms(session, client=role_client.client)
    assert check_perm_constraints_by_check_type(session, order, passport, check_type) is False


def test_hidden_role_client(session, check_type, role_client, right_role):
    role_client.hidden = True
    passport = create_passport(
        session,
        [(right_role, None, role_client.client_batch_id)],
    )
    order = create_order_w_firms(session, client=role_client.client)
    assert check_perm_constraints_by_check_type(session, order, passport, check_type) is False
