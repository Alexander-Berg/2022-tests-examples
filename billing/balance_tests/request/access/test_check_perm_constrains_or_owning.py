# -*- coding: utf-8 -*-
import pytest

from balance.constants import ConstraintTypes, FirmId
from balance.exc import PERMISSION_DENIED
from tests import object_builder as ob

from tests.balance_tests.request.request_common import create_role_client

PERMISSION = 'Perm'

pytestmark = [
    pytest.mark.permissions,
]


@pytest.fixture(name='request_')
def create_request(session, client=None):
    client = client or ob.ClientBuilder()
    return ob.RequestBuilder(
        firm_id=FirmId.YANDEX_OOO,
        basket=ob.BasketBuilder(
            client=client,
            rows=[ob.BasketItemBuilder(
                order=ob.OrderBuilder(client=client),
                quantity=666,
            )]
        )
    ).build(session).obj


def test_check_perm_constraints_or_owning_w_perm(session, role_client):
    role = ob.create_role(
        session,
        (PERMISSION, {ConstraintTypes.firm_id: None, ConstraintTypes.client_batch_id: None}),
    )
    request_ = create_request(session, role_client.client)
    passport = ob.create_passport(session, (role, request_.firm_id, role_client.client_batch_id))
    assert request_.check_perm_constraints_or_owning(passport, PERMISSION, strict=True) is True


def test_check_perm_constraints_or_owning_for_client(session, request_):
    passport = ob.create_passport(session, client=request_.client)
    assert request_.check_perm_constraints_or_owning(passport, PERMISSION, strict=True) is True


@pytest.mark.parametrize('strict', [True, False])
def test_check_perm_constraints_or_owning_for_nobody(session, request_, strict):
    passport = ob.create_passport(session)

    if strict:
        with pytest.raises(PERMISSION_DENIED):
            request_.check_perm_constraints_or_owning(passport, PERMISSION, strict=True)
    else:
        request_.check_perm_constraints_or_owning(passport, PERMISSION, strict=False)
