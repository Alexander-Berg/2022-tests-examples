# -*- coding: utf-8 -*-
import pytest

from balance.constants import ConstraintTypes
from balance.exc import PERMISSION_DENIED
from tests import object_builder as ob

# noinspection PyUnresolvedReferences
from tests.balance_tests.invoices.access.access_common import (
    PERMISSION,
    create_invoice,
    create_role_client,
    create_firm_id,
)

pytestmark = [
    pytest.mark.permissions,
]


def test_check_perm_constraints_or_owning_w_perm(session, firm_id, role_client):
    role = ob.create_role(
        session,
        (PERMISSION, {ConstraintTypes.firm_id: None, ConstraintTypes.client_batch_id: None}),
    )
    passport = ob.create_passport(session, (role, firm_id, role_client.client_batch_id))
    invoice = create_invoice(session, firm_id, client=role_client.client)
    assert invoice.check_perm_constraints_or_owning(passport, PERMISSION, strict=True) is True


def test_check_perm_constraints_or_owning_for_client(session, invoice):
    passport = ob.create_passport(session, client=invoice.client)
    assert invoice.check_perm_constraints_or_owning(passport, PERMISSION, strict=True) is True


@pytest.mark.parametrize('strict', [True, False])
def test_check_perm_constraints_or_owning_for_nobody(session, invoice, strict):
    passport = ob.create_passport(session)

    if strict:
        with pytest.raises(PERMISSION_DENIED):
            invoice.check_perm_constraints_or_owning(passport, PERMISSION, strict=True)
    else:
        invoice.check_perm_constraints_or_owning(passport, PERMISSION, strict=False)
