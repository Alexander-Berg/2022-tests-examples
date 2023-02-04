# -*- coding: utf-8 -*-
import pytest

from balance.constants import ConstraintTypes
from balance.exc import PERMISSION_DENIED
from tests import object_builder as ob

# noinspection PyUnresolvedReferences
from tests.balance_tests.act.access.access_common import (
    PERMISSION,
    create_act,
    create_role_client,
)

pytestmark = [
    pytest.mark.permissions,
]


def test_check_perm_constraints_or_owning_w_perm(session, role_client):
    role = ob.create_role(
        session,
        (
            PERMISSION,
            {ConstraintTypes.firm_id: None, ConstraintTypes.client_batch_id: None},
        ),
    )
    act = create_act(session, client=role_client.client)
    passport = ob.create_passport(session, (role, act.invoice.firm_id, role_client.client_batch_id))
    assert act.check_perm_constraints_or_owning(passport, PERMISSION, strict=True) is True


def test_check_perm_constraints_or_owning_for_client(session, act):
    passport = ob.create_passport(session, client=act.client)
    assert act.check_perm_constraints_or_owning(passport, PERMISSION, strict=True) is True


@pytest.mark.parametrize('strict', [True, False])
def test_check_perm_constraints_or_owning_for_nobody(session, act, strict):
    passport = ob.create_passport(session)

    if strict:
        with pytest.raises(PERMISSION_DENIED):
            act.check_perm_constraints_or_owning(passport, PERMISSION, strict=True)
    else:
        act.check_perm_constraints_or_owning(passport, PERMISSION, strict=False)
