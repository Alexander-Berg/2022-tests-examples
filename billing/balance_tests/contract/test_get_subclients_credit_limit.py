import datetime

import pytest
import hamcrest

from balance.constants import (
    CreditType,
)
from balance import muzzle_util as ut
from balance import exc
from tests import object_builder as ob

TODAY = ut.trunc_date(datetime.datetime.now())


@pytest.fixture
def client(session):
    return ob.ClientBuilder.construct(session, is_agency=1)


@pytest.fixture
def person(session, client):
    return ob.PersonBuilder.construct(session, client=client, type='ur')


@pytest.mark.parametrize(
    'params, req_res',
    [
        pytest.param(
            {'client_limit': 666},
            {'currency': 'RUR', 'limit': 666, 'credit_type': CreditType.PO_SROKU_I_SUMME, 'payment_term': 45},
            id='defaults',
        ),
        pytest.param(
            {'client_limit': 666, 'client_credit_type': CreditType.PO_SROKU},
            {'currency': 'RUR', 'limit': 666, 'credit_type': CreditType.PO_SROKU, 'payment_term': 45},
            id='credit_type',
        ),
        pytest.param(
            {'client_limit': 666, 'client_payment_term': 666},
            {'currency': 'RUR', 'limit': 666, 'credit_type': CreditType.PO_SROKU_I_SUMME, 'payment_term': 666},
            id='payment_term',
        ),
        pytest.param(
            {'client_limit': "0"},
            None,
            id='empty',
        ),
    ]
)
def test_params(session, client, person, params, req_res):
    subclient = ob.ClientBuilder.construct(session, agency=client)

    contract = ob.create_credit_contract(
        session,
        client_limits={
            subclient.id: params,
        }
    )

    res = contract.current.get_subclients_credit_limit([subclient])
    if req_res is None:
        hamcrest.assert_that(res, hamcrest.is_(None))
    else:
        hamcrest.assert_that(res, hamcrest.has_entries(**req_res))


def test_multiple_limits(session, client, person):
    subclient1 = ob.ClientBuilder.construct(session, agency=client)
    subclient2 = ob.ClientBuilder.construct(session, agency=client)
    subclient3 = ob.ClientBuilder.construct(session, agency=client)

    contract = ob.create_credit_contract(
        session,
        client_limits={
            subclient1.id: {'client_limit': 666},
            subclient2.id: {'client_limit': 777},
        }
    )

    hamcrest.assert_that(
        contract.current.get_subclients_credit_limit([subclient1]),
        hamcrest.has_entries(limit=666)
    )
    hamcrest.assert_that(
        contract.current.get_subclients_credit_limit([subclient2]),
        hamcrest.has_entries(limit=777)
    )
    hamcrest.assert_that(
        contract.current.get_subclients_credit_limit([subclient3]),
        hamcrest.is_(None)
    )


def test_multiple_subclients_limits(session, client, person):
    subclient1 = ob.ClientBuilder.construct(session, agency=client)
    subclient2 = ob.ClientBuilder.construct(session, agency=client)

    contract = ob.create_credit_contract(
        session,
        client_limits={
            subclient1.id: {'client_limit': 666},
            subclient2.id: {'client_limit': 777},
        }
    )

    with pytest.raises(exc.INVALID_PARAM) as exc_info:
        contract.current.get_subclients_credit_limit([subclient1, subclient2])
    assert 'There are several subclient limits' in exc_info.value.msg


def test_multiple_subclients_part_without_limit(session, client, person):
    subclient1 = ob.ClientBuilder.construct(session, agency=client)
    subclient2 = ob.ClientBuilder.construct(session, agency=client)

    contract = ob.create_credit_contract(
        session,
        client_limits={
            subclient1.id: {'client_limit': 666},
        }
    )

    with pytest.raises(exc.MIXED_CLIENTS_FOR_SUBCLIENT_PERSONAL_ACCOUNT) as exc_info:
        contract.current.get_subclients_credit_limit([subclient1, subclient2])


@pytest.mark.linked_clients
def test_alias(session, client, person):
    subclient1 = ob.ClientBuilder.construct(session, agency=client)
    subclient2 = ob.ClientBuilder.construct(session, agency=client)
    subclient2.make_equivalent(subclient1)

    contract = ob.create_credit_contract(
        session,
        client_limits={
            subclient1.id: {'client_limit': 666},
        }
    )

    res = contract.current.get_subclients_credit_limit([subclient2])
    hamcrest.assert_that(res, hamcrest.has_entries(limit=666))


@pytest.mark.linked_clients
def test_brand(session, client, person):
    subclient1 = ob.ClientBuilder.construct(session, agency=client)
    subclient2 = ob.ClientBuilder.construct(session, agency=client)

    ob.create_brand(
        session,
        [(TODAY - datetime.timedelta(1), [subclient1, subclient2])],
        TODAY + datetime.timedelta(1)
    )

    contract = ob.create_credit_contract(
        session,
        client_limits={
            subclient1.id: {'client_limit': 666},
        }
    )

    res = contract.current.get_subclients_credit_limit([subclient2])
    hamcrest.assert_that(res, hamcrest.has_entries(limit=666))


@pytest.mark.linked_clients
def test_multiple_subclients_brand(session, client, person):
    subclient1 = ob.ClientBuilder.construct(session, agency=client)
    subclient2 = ob.ClientBuilder.construct(session, agency=client)

    ob.create_brand(
        session,
        [(TODAY - datetime.timedelta(1), [subclient1, subclient2])],
        TODAY + datetime.timedelta(1)
    )

    contract = ob.create_credit_contract(
        session,
        client_limits={
            subclient1.id: {'client_limit': 666},
        }
    )

    res = contract.current.get_subclients_credit_limit([subclient1, subclient2])
    hamcrest.assert_that(res, hamcrest.has_entries(limit=666))


@pytest.mark.linked_clients
def test_mixed_brands(session, client, person):
    subclient1 = ob.ClientBuilder.construct(session, agency=client)
    subclient2 = ob.ClientBuilder.construct(session, agency=client)
    subclient3 = ob.ClientBuilder.construct(session, agency=client)

    contract = ob.create_credit_contract(
        session,
        client_limits={
            subclient1.id: {'client_limit': 666},
        }
    )

    ob.create_brand(
        session,
        [(TODAY - datetime.timedelta(1), [subclient1, subclient2])],
        TODAY + datetime.timedelta(1)
    )
    ob.create_brand(
        session,
        [(TODAY - datetime.timedelta(1), [subclient1, subclient3])],
        TODAY + datetime.timedelta(1)
    )

    with pytest.raises(exc.MIXED_CLIENTS_FOR_SUBCLIENT_PERSONAL_ACCOUNT):
        contract.current.get_subclients_credit_limit([subclient2, subclient3])


@pytest.mark.linked_clients
def test_mixed_brand_zero_limit(session, client, person):
    subclient1 = ob.ClientBuilder.construct(session, agency=client)
    subclient2 = ob.ClientBuilder.construct(session, agency=client)
    subclient3 = ob.ClientBuilder.construct(session, agency=client)

    contract = ob.create_credit_contract(
        session,
        client_limits={
            subclient1.id: {'client_limit': 666},
            subclient2.id: {'client_limit': 0},
        }
    )

    ob.create_brand(
        session,
        [(TODAY - datetime.timedelta(1), [subclient1, subclient2, subclient3])],
        TODAY + datetime.timedelta(1)
    )

    res = contract.current.get_subclients_credit_limit([subclient1, subclient2, subclient3])
    hamcrest.assert_that(res, hamcrest.has_entries(limit=666))


@pytest.mark.linked_clients
@pytest.mark.parametrize(
    'dt_delta, is_ok',
    [
        pytest.param(-1, True, id='past'),
        pytest.param(0, False, id='present'),
    ],
)
def test_brand_on_dt(session, client, person, dt_delta, is_ok):
    subclient1 = ob.ClientBuilder.construct(session, agency=client)
    subclient2 = ob.ClientBuilder.construct(session, agency=client)

    ob.create_brand(
        session,
        [(TODAY - datetime.timedelta(5), [subclient1, subclient2])],
        TODAY
    )

    contract = ob.create_credit_contract(
        session,
        client_limits={
            subclient1.id: {'client_limit': 666},
        }
    )

    state = contract.current_signed(TODAY + datetime.timedelta(dt_delta))
    res = state.get_subclients_credit_limit([subclient2])
    if is_ok:
        hamcrest.assert_that(res, hamcrest.has_entries(limit=666))
    else:
        assert res is None
