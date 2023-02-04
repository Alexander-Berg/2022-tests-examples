# -*- coding: utf-8 -*-

import datetime

import pytest
import hamcrest

from balance import muzzle_util as ut
from balance.webcontrols.helpers import check_client_limits

from tests import object_builder as ob

TODAY = ut.trunc_date(datetime.datetime.now())
TOMORROW = TODAY + datetime.timedelta(1)

pytestmark = [
    pytest.mark.linked_clients,
]


@pytest.fixture
def client(session):
    return ob.ClientBuilder.construct(session, is_agency=1)


@pytest.fixture
def person(session, client):
    return ob.PersonBuilder.construct(session, client=client, type='ur')


@pytest.mark.parametrize(
    'dt_delta, is_ok',
    [
        (0, False),
        (1, True),
    ]
)
def test_dates(session, client, person, dt_delta, is_ok):
    subclient1 = ob.ClientBuilder.construct(session, agency=client)
    subclient2 = ob.ClientBuilder.construct(session, agency=client)
    subclient3 = ob.ClientBuilder.construct(session, agency=client)

    contract = ob.create_credit_contract(
        session,
        client,
        person,
        client_limits={subclient1.id: {'client_limit': 666}},
        finish_dt=TODAY + datetime.timedelta(10)
    )

    ob.create_brand(session, [(TODAY, [subclient1, subclient2])], TOMORROW)

    res = check_client_limits(
        session,
        contract_id=contract.id,
        on_dt=(TODAY + datetime.timedelta(dt_delta)).strftime('%Y-%m-%d'),
        client_ids=';'.join(map(str, [subclient2.id, subclient3.id]))
    )

    if is_ok:
        assert res == {}
    else:
        hamcrest.assert_that(
            res,
            hamcrest.has_entries(
                type='brand',
                ids=str(subclient2.id)
            )
        )


def test_brands_only_in_new(session, client, person):
    subclient1 = ob.ClientBuilder.construct(session, agency=client)
    subclient2 = ob.ClientBuilder.construct(session, agency=client)
    subclient3 = ob.ClientBuilder.construct(session, agency=client)

    contract = ob.create_credit_contract(
        session,
        client,
        person,
        client_limits={subclient1.id: {'client_limit': 666}},
        finish_dt=TODAY + datetime.timedelta(10)
    )

    ob.create_brand(session, [(TODAY, [subclient2, subclient3])], TOMORROW)

    res = check_client_limits(
        session,
        contract_id=contract.id,
        on_dt=None,
        client_ids=';'.join(map(str, [subclient2.id, subclient3.id]))
    )

    hamcrest.assert_that(
        res,
        hamcrest.has_entries(
            type='brand',
            ids=hamcrest.any_of(
                ', '.join(map(str, [subclient2.id, subclient3.id])),
                ', '.join(map(str, [subclient3.id, subclient2.id])),
            )
        )
    )


def test_zero_limit(session, client, person):
    subclient1 = ob.ClientBuilder.construct(session, agency=client)
    subclient2 = ob.ClientBuilder.construct(session, agency=client)

    contract = ob.create_credit_contract(
        session,
        client,
        person,
        client_limits={
            subclient1.id: {'client_limit': 0},
            subclient2.id: {'client_limit': 666}
        },
        finish_dt=TODAY + datetime.timedelta(10)
    )

    ob.create_brand(session, [(TODAY, [subclient1, subclient2])], TOMORROW)

    res = check_client_limits(
        session,
        contract_id=contract.id,
        on_dt=None,
        client_ids=str(subclient2.id)
    )

    assert res == {}
