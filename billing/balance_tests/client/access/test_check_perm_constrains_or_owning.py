# -*- coding: utf-8 -*-
import pytest

from balance.constants import ConstraintTypes
from balance.exc import PERMISSION_DENIED
from tests import object_builder as ob

from tests.balance_tests.client.access.conftest import (
    PERMISSION,
)

pytestmark = [
    pytest.mark.permissions,
]


def test_check_perm_constraints_or_owning_w_perm(session, role_client):
    role = ob.create_role(session, (PERMISSION, {ConstraintTypes.client_batch_id: None}))
    passport = ob.create_passport(session, (role, None, role_client.client_batch_id))
    assert role_client.client.check_perm_constraints_or_owning(passport, PERMISSION, strict=True) is True


def test_check_perm_constraints_or_owning_for_client(session, client):
    passport = ob.create_passport(session, client=client)
    assert client.check_perm_constraints_or_owning(passport, PERMISSION, strict=True) is True


@pytest.mark.parametrize('strict', [True, False])
def test_check_perm_constraints_or_owning_for_nobody(session, client, strict):
    passport = ob.create_passport(session)

    if strict:
        with pytest.raises(PERMISSION_DENIED):
            client.check_perm_constraints_or_owning(passport, PERMISSION, strict=True)
    else:
        client.check_perm_constraints_or_owning(passport, PERMISSION, strict=False)
