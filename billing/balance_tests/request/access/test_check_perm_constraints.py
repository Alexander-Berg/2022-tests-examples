# -*- coding: utf-8 -*-
import pytest

from balance.constants import (
    ConstraintTypes,
    FirmId,
    Enum
)
from tests import object_builder as ob

from tests.balance_tests.request.request_common import (
    create_request,
    create_role,
    create_passport,
    create_manager,
    create_client,
    create_order,
    create_firm,
    create_role_client,
    check_perm_constraints_by_check_type,
)

PERMISSION = 'Permission'


class CheckType(Enum):
    object = 'object'
    query = 'query'


pytestmark = [
    pytest.mark.permissions,
    pytest.mark.parametrize('check_type', [CheckType.object, CheckType.query]),
]


def test_wo_permission(session, check_type):
    role = create_role(session)
    passport = create_passport(session, role, patch_session=True)
    request = create_request(session)

    assert check_perm_constraints_by_check_type(session, request, passport, check_type) is False


def test_w_permission_wo_constraints(session, check_type):
    role = create_role(session, PERMISSION)
    passport = create_passport(session, role, patch_session=True)
    request = create_request(session)

    assert check_perm_constraints_by_check_type(session, request, passport, check_type) is True


def test_firm(session, check_type):
    role = create_role(session, (PERMISSION, {ConstraintTypes.firm_id: None}))
    passport = create_passport(session, (role, FirmId.YANDEX_OOO), patch_session=True)
    request = create_request(session, firm_id=FirmId.YANDEX_OOO)

    assert check_perm_constraints_by_check_type(session, request, passport, check_type) is True


def test_no_firm(session, check_type):
    role = create_role(session, (PERMISSION, {ConstraintTypes.firm_id: None}))
    passport = create_passport(
        session,
        (role, FirmId.YANDEX_OOO),
        patch_session=True
    )
    request = create_request(session, None)

    assert check_perm_constraints_by_check_type(session, request, passport, check_type) is True


def test_wrong_firm(session, check_type):
    role = create_role(session, (PERMISSION, {ConstraintTypes.firm_id: None}))
    passport = create_passport(
        session,
        (role, FirmId.YANDEX_OOO),
        patch_session=True
    )
    request = create_request(session, firm_id=FirmId.TAXI)

    assert check_perm_constraints_by_check_type(session, request, passport, check_type) is False


def test_multiple_firms(session, check_type):
    role = create_role(session, (PERMISSION, {ConstraintTypes.firm_id: None}))
    passport = create_passport(
        session,
        (role, FirmId.YANDEX_OOO),
        (role, FirmId.TAXI),
        patch_session=True
    )
    request = create_request(session, firm_id=FirmId.TAXI)

    assert check_perm_constraints_by_check_type(session, request, passport, check_type) is True


def test_multiple_firms_no_firm(session, check_type):
    role = create_role(session, (PERMISSION, {ConstraintTypes.firm_id: None}))
    passport = create_passport(
        session,
        (role, FirmId.YANDEX_OOO),
        (role, FirmId.TAXI),
        patch_session=True

    )
    request = create_request(session, firm_id=None)

    assert check_perm_constraints_by_check_type(session, request, passport, check_type) is True


def test_manager__1(session, check_type, client):
    role = create_role(session, (PERMISSION, {ConstraintTypes.manager: 1}))
    passport = create_passport(session, role, patch_session=True)
    request = create_request(session, firm_id=None, client=client,
                             orders=[create_order(session, client=client)])
    if check_type == CheckType.object:
        assert check_perm_constraints_by_check_type(session, request, passport, check_type) is False
    if check_type == CheckType.query:
        assert check_perm_constraints_by_check_type(session, request, passport, check_type) is True


def test_manager(session, check_type, client):
    role = create_role(session, (PERMISSION, {ConstraintTypes.manager: 1}))
    passport = create_passport(session, role, patch_session=True)
    manager = create_manager(session, passport)
    request = create_request(session, firm_id=None, client=client,
                             orders=[create_order(session, client=client, manager=manager)])

    assert check_perm_constraints_by_check_type(session, request, passport, check_type) is True


def test_wrong_manager(session, check_type, client):
    req_result = {CheckType.object: False, CheckType.query: True}[check_type]
    role = create_role(session, (PERMISSION, {ConstraintTypes.manager: 1}))
    passport = create_passport(session, role, patch_session=True)
    manager = create_manager(session, passport)
    wrong_manager = create_manager(session)
    request = create_request(session, firm_id=None, client=client,
                             orders=[create_order(session, client=client, manager=wrong_manager)])

    assert check_perm_constraints_by_check_type(session, request, passport, check_type) is req_result


def test_client_w_permission_wo_constraints(session, check_type, request_):
    role = create_role(session, (PERMISSION, {ConstraintTypes.client_batch_id: None}))
    passport = create_passport(session, role)
    assert check_perm_constraints_by_check_type(session, request_, passport, check_type) is True


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
    """Разные сочетания клиента-фирмы для реквеста с фирмой"""
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
        role_firm_id = firm.id if match_firm else FirmId.TAXI
    passport = create_passport(session, (role, role_firm_id, client_batch_id))
    request = create_request(session, firm_id=firm.id, client=role_client.client)
    assert check_perm_constraints_by_check_type(session, request, passport, check_type) is ans


def test_client_w_right_permission_constraint(session, role_client, check_type):
    role = create_role(session, (PERMISSION, {ConstraintTypes.client_batch_id: [role_client.client_batch_id, 666]}))
    passport = create_passport(session, role)
    request = create_request(session, client=role_client.client)
    assert check_perm_constraints_by_check_type(session, request, passport, check_type) is True


def test_client_and_firm_in_different_constraints(session, role_client, firm, check_type):
    role = create_role(
        session,
        (PERMISSION, {ConstraintTypes.client_batch_id: None, ConstraintTypes.firm_id: None}),
    )
    passport = create_passport(
        session,
        (role, None, role_client.client_batch_id),
        (role, firm.id, None),
    )
    request = create_request(session, firm_id=firm.id, client=role_client.client)
    assert check_perm_constraints_by_check_type(session, request, passport, check_type) is True


def test_client_and_firm_in_different_wrong_constraints(session, role_client, firm, check_type):
    role = create_role(
        session,
        (PERMISSION, {ConstraintTypes.client_batch_id: None, ConstraintTypes.firm_id: None}),
    )
    passport = create_passport(
        session,
        (role, create_firm(session).id, role_client.client_batch_id),
        (role, firm.id, create_role_client(session).client_batch_id),
    )
    request = create_request(session, firm_id=firm.id, client=role_client.client)
    assert check_perm_constraints_by_check_type(session, request, passport, check_type) is False


def test_hidden_role_client(session, check_type, role_client):
    role = create_role(session, (PERMISSION, {ConstraintTypes.client_batch_id: None}))
    role_client.hidden = True
    passport = create_passport(
        session,
        (role, None, role_client.client_batch_id),
    )
    request = create_request(session, client=role_client.client)
    assert check_perm_constraints_by_check_type(session, request, passport, check_type) is False
