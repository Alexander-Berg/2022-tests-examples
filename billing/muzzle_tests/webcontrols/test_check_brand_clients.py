# -*- coding: utf-8 -*-

import datetime

import pytest
import hamcrest

from balance import muzzle_util as ut
from balance import core
from balance.webcontrols.helpers import check_brand_clients

from tests import object_builder as ob

TODAY = ut.trunc_date(datetime.datetime.now())
TOMORROW = TODAY + datetime.timedelta(1)

pytestmark = [
    pytest.mark.linked_clients,
]


def test_ok(session):
    subclient1 = ob.ClientBuilder.construct(session)
    subclient2 = ob.ClientBuilder.construct(session)

    brand = ob.create_brand(session, [(TODAY, [subclient1])], TOMORROW)

    res = check_brand_clients(
        session,
        contract_id=brand.id,
        on_dt=None,
        finish_dt=None,
        client_ids=';'.join(map(str, [subclient1.id, subclient2.id]))
    )

    assert res == {}


def test_autooverdraft(session):
    subclient1 = ob.ClientBuilder.construct(session)
    subclient2 = ob.ClientBuilder.construct(session)
    subclient3 = ob.ClientBuilder.construct(session)

    ob.OverdraftParamsBuilder.construct(session, client=subclient3)

    brand = ob.create_brand(session, [(TODAY, [subclient1])], TOMORROW)

    res = check_brand_clients(
        session,
        contract_id=brand.id,
        on_dt=None,
        finish_dt=None,
        client_ids=';'.join(map(str, [subclient1.id, subclient2.id, subclient3.id]))
    )

    hamcrest.assert_that(
        res,
        hamcrest.has_entries(
            type='autooverdraft',
            ids=str(subclient3.id)
        )
    )


@pytest.mark.parametrize(
    'dt_delta_start, dt_delta_finish, is_ok',
    [
        pytest.param(-2, 0, True, id='past'),
        pytest.param(-2, 1, False, id='present'),
        pytest.param(1, 2, True, id='future'),
    ]
)
def test_added_brands(session, dt_delta_start, dt_delta_finish, is_ok):
    subclient1 = ob.ClientBuilder.construct(session)
    subclient2 = ob.ClientBuilder.construct(session)
    subclient3 = ob.ClientBuilder.construct(session)

    brand = ob.create_brand(session, [(TODAY - datetime.timedelta(10), [subclient1])], TOMORROW)
    ob.create_brand(session, [(TODAY, [subclient2, subclient3])], TOMORROW)

    res = check_brand_clients(
        session,
        contract_id=brand.id,
        on_dt=(TODAY + datetime.timedelta(dt_delta_start)).strftime('%Y-%m-%d'),
        finish_dt=(TODAY + datetime.timedelta(dt_delta_finish)).strftime('%Y-%m-%d'),
        client_ids=';'.join(map(str, [subclient1.id, subclient2.id]))
    )

    if is_ok:
        assert res == {}
    else:
        hamcrest.assert_that(
            res,
            hamcrest.has_entries(
                type='active_brands',
                ids=str(subclient2.id)
            )
        )


def test_new_brands(session):
    subclient1 = ob.ClientBuilder.construct(session)
    subclient2 = ob.ClientBuilder.construct(session)
    subclient3 = ob.ClientBuilder.construct(session)

    ob.create_brand(session, [(TODAY, [subclient2, subclient3])], TOMORROW)

    res = check_brand_clients(
        session,
        contract_id=None,
        on_dt=None,
        finish_dt=None,
        client_ids=';'.join(map(str, [subclient1.id, subclient2.id]))
    )

    hamcrest.assert_that(
        res,
        hamcrest.has_entries(
            type='active_brands',
            ids=str(subclient2.id)
        )
    )


@pytest.mark.parametrize(
    'is_completed, is_ok',
    [
        (True, True),
        (False, False),
    ]
)
def test_removed_unconsumed(session, is_completed, is_ok):
    agency = ob.ClientBuilder.construct(session, is_agency=1)
    person = ob.PersonBuilder.construct(session, client=agency, type='ur')

    subclient1 = ob.ClientBuilder.construct(session, agency=agency)
    subclient2 = ob.ClientBuilder.construct(session, agency=agency)

    brand = ob.create_brand(session, [(TODAY, [subclient1, subclient2])], TOMORROW)

    contract = ob.create_credit_contract(
        session,
        agency,
        person,
        client_limits={subclient1.id: {'client_limit': 666}}
    )

    request = ob.RequestBuilder(
        basket=ob.BasketBuilder(
            client=agency,
            rows=[ob.BasketItemBuilder(
                quantity=1,
                order=ob.OrderBuilder(client=subclient2, agency=agency)
            )]
        )
    ).build(session).obj
    pa, = core.Core(session).pay_on_credit(request.id, 1003, person.id, contract.id)
    if is_completed:
        consume, = pa.consumes
        order = consume.order
        order.calculate_consumption(datetime.datetime.now(), {order.shipment_type: consume.current_qty})
        session.flush()

    res = check_brand_clients(
        session,
        contract_id=brand.id,
        on_dt=None,
        finish_dt=None,
        client_ids=';'.join(map(str, [subclient1.id]))
    )

    if is_ok:
        assert res == {}
    else:
        hamcrest.assert_that(
            res,
            hamcrest.has_entries(
                type='free_ind_credit',
                ids=str(subclient2.id)
            )
        )
