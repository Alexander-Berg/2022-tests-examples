# -*- coding: utf-8 -*-
import pytest

from balance.exc import PERMISSION_DENIED
from tests import object_builder as ob

# noinspection PyUnresolvedReferences
from tests.balance_tests.order.access.access_common import (
    PERMISSION,
    ORDER_FIRM_IDS,
    create_order_w_firms,
    create_role_client,
    create_right_role,
    create_passport,
)

pytestmark = [
    pytest.mark.permissions,
]


def test_check_perm_constraints_or_owning_w_perm(session, right_role, role_client):
    passport = create_passport(
        session,
        [
            (right_role, firm_id, role_client.client_batch_id)
            for firm_id in ORDER_FIRM_IDS
        ]
    )
    order = create_order_w_firms(session, client=role_client.client)
    assert order.check_perm_constraints_or_owning(passport, PERMISSION, strict=True) is True


def test_check_perm_constraints_or_owning_for_client(session, order_w_firms):
    passport = ob.create_passport(session, client=order_w_firms.client)
    assert order_w_firms.check_perm_constraints_or_owning(passport, PERMISSION, strict=True) is True


@pytest.mark.parametrize('strict', [True, False])
def test_check_perm_constraints_or_owning_for_nobody(session, order_w_firms, strict):
    passport = ob.create_passport(session)

    if strict:
        with pytest.raises(PERMISSION_DENIED):
            order_w_firms.check_perm_constraints_or_owning(passport, PERMISSION, strict=True)
    else:
        order_w_firms.check_perm_constraints_or_owning(passport, PERMISSION, strict=False)
