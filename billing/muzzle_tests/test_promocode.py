# -*- coding: utf-8 -*-
import datetime
import pytest
import mock

from balance import (
    constants as cst,
    exc,
)
from balance.actions import promocodes as promocode_actions
from muzzle.api import promocode as promocode_api
import tests.object_builder as ob

NOW = datetime.datetime.now().replace(microsecond=0)


pytestmark = [
    pytest.mark.promo_code,
]


@pytest.fixture(name='client')
def create_client(session):
    return ob.ClientBuilder(service=cst.ServiceId.DIRECT).build(session).obj


@pytest.fixture(name='promocode')
def create_promocode(session, params=None):
    params = params if params is not None else {}
    group = ob.PromoCodeGroupBuilder(**params).build(session).obj
    session.add(group)
    session.flush()
    pc = group.promocodes[0]
    return pc


def reserve_promocode(session, promocode, client, dt=NOW):
    promocode_actions.reserve_promo_code(client, promocode, dt)
    session.flush()
    return promocode.reservations[0]


class TestTryToReserve(object):
    def test_raise_CANT_RESERVE_PROMOCODE(self, session, client):
        promocode = create_promocode(session, params={'reservation_days': 10})
        reserve_promocode(session, promocode, client, dt=NOW - datetime.timedelta(days=30))
        res = promocode_api.try_to_reserve_promocode(session, client, promocode.code)
        assert res is False
