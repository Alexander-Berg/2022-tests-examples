# -*- coding: utf-8 -*-
import pytest

from balance.constants import ConstraintTypes, FirmId
from balance.exc import PERMISSION_DENIED
from tests import object_builder as ob

# noinspection PyUnresolvedReferences
from tests.balance_tests.payment.common import (
    PERMISSION,
    create_card_payment,
    create_role_client,
)


pytestmark = [
    pytest.mark.permissions,
]


def test_check_perm_constraints_or_owning_w_perm(session, role_client):
    role = ob.create_role(
        session,
        (PERMISSION, {ConstraintTypes.firm_id: None, ConstraintTypes.client_batch_id: None}),
    )
    payment = create_card_payment(session, firm_id=FirmId.TAXI, client=role_client.client)
    passport = ob.create_passport(session, (role, FirmId.TAXI, role_client.client_batch_id))
    assert payment.check_perm_constraints_or_owning(passport, PERMISSION, strict=True) is True


def test_check_perm_constraints_or_owning_for_client(session, card_payment):
    passport = ob.create_passport(session, client=card_payment.invoice.client)
    assert card_payment.check_perm_constraints_or_owning(passport, PERMISSION, strict=True) is True


@pytest.mark.parametrize('strict', [True, False])
def test_check_perm_constraints_or_owning_for_nobody(session, card_payment, strict):
    passport = ob.create_passport(session)

    if strict:
        with pytest.raises(PERMISSION_DENIED):
            card_payment.check_perm_constraints_or_owning(passport, PERMISSION, strict=True)
    else:
        card_payment.check_perm_constraints_or_owning(passport, PERMISSION, strict=False)
