# -*- coding: utf-8 -*-
import pytest

from balance.exc import PERMISSION_DENIED
from tests import object_builder as ob

from tests.balance_tests.contract.access.access_common import (
    PERMISSION,
    create_role_client,
    create_contract,
    create_right_role,
    create_firm_id,
)

pytestmark = [
    pytest.mark.permissions,
]


def test_check_perm_constraints_or_owning_w_perm(session, role_client, right_role, firm_id):
    contract = create_contract(session, client=role_client.client, firm=firm_id)
    passport = ob.create_passport(session, (right_role, firm_id, role_client.client_batch_id))
    assert contract.check_perm_constraints_or_owning(passport, PERMISSION, strict=True) is True


def test_check_perm_constraints_or_owning_for_client(session, contract):
    passport = ob.create_passport(session, client=contract.client)
    assert contract.check_perm_constraints_or_owning(passport, PERMISSION, strict=True) is True


@pytest.mark.parametrize('strict', [True, False])
def test_check_perm_constraints_or_owning_for_nobody(session, contract, strict):
    passport = ob.create_passport(session)

    if strict:
        with pytest.raises(PERMISSION_DENIED):
            contract.check_perm_constraints_or_owning(passport, PERMISSION, strict=True)
    else:
        contract.check_perm_constraints_or_owning(passport, PERMISSION, strict=False)
