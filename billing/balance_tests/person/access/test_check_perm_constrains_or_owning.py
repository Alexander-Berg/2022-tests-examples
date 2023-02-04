# -*- coding: utf-8 -*-
import pytest

from balance.constants import ConstraintTypes
from balance.exc import PERMISSION_DENIED
from tests import object_builder as ob

from tests.balance_tests.person.access.conftest import (
    PERMISSION,
    create_person,
)

pytestmark = [
    pytest.mark.permissions,
]


def test_check_perm_constraints_or_owning_w_perm(session, role_client):
    role = ob.create_role(session, (PERMISSION, {ConstraintTypes.client_batch_id: None}))
    passport = ob.create_passport(session, (role, None, role_client.client_batch_id))
    person = create_person(session, role_client.client)
    assert person.check_perm_constraints_or_owning(passport, PERMISSION, strict=True) is True


def test_check_perm_constraints_or_owning_for_client(session, client, person):
    passport = ob.create_passport(session, client=client)
    assert person.check_perm_constraints_or_owning(passport, PERMISSION, strict=True) is True


@pytest.mark.parametrize('strict', [True, False])
def test_check_perm_constraints_or_owning_for_nobody(session, person, strict):
    passport = ob.create_passport(session)

    if strict:
        with pytest.raises(PERMISSION_DENIED):
            person.check_perm_constraints_or_owning(passport, PERMISSION, strict=True)
    else:
        person.check_perm_constraints_or_owning(passport, PERMISSION, strict=False)
