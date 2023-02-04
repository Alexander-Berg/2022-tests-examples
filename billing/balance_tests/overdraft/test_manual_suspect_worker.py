# -*- coding: utf-8 -*-

import datetime

import pytest
import hamcrest

from balance import overdraft
from balance.constants import (
    ServiceId,
    FirmId,
)

from tests import object_builder as ob
from tests.balance_tests.overdraft.common import (
    set_limit,
    create_invoice,
)


@pytest.mark.parametrize(
    'is_overdraft, term_delta, receipt_sum, is_suspect',
    [
        pytest.param(1, 666, 0, 1, id='overdraft_old'),
        pytest.param(1, 666, 60, 1, id='overdraft_partial_paid'),
        pytest.param(1, 666, 666, 0, id='overdraft_paid'),
        pytest.param(1, 0, 0, 0, id='overdraft_new'),
        pytest.param(0, 66, 0, 0, id='prepayment'),
    ]
)
@pytest.mark.parametrize('client', [0, 1], ids=['client', 'agency'], indirect=True)
def test_set(session, client, is_overdraft, receipt_sum, term_delta, is_suspect):
    invoice = create_invoice(client, is_overdraft)
    invoice.payment_term_dt = datetime.datetime.now() - datetime.timedelta(term_delta)
    if receipt_sum:
        invoice.create_receipt(receipt_sum)
    set_limit(client, 'RUB', 100)

    overdraft.Overdraft(session).manual_suspect_worker([client.id])
    session.expire_all()

    hamcrest.assert_that(
        client,
        hamcrest.has_properties(
            manual_suspect=is_suspect,
            overdraft=hamcrest.has_entry(
                (ServiceId.DIRECT, FirmId.YANDEX_OOO),
                hamcrest.has_properties(overdraft_limit=100)
            ),
        )
    )


@pytest.mark.linked_clients
def test_alias_set(session, client, invoice):
    alias_client = ob.ClientBuilder.construct(session)
    alias_client.make_equivalent(client)

    invoice.payment_term_dt = datetime.datetime.now() - datetime.timedelta(1)
    session.flush()

    overdraft.Overdraft(session).manual_suspect_worker([client.id])
    session.expire_all()

    hamcrest.assert_that(
        [client, alias_client],
        hamcrest.only_contains(
            hamcrest.has_properties(manual_suspect=1)
        )
    )


@pytest.mark.linked_clients
def test_brand_set(session, client, invoice):
    brand_client = ob.ClientBuilder.construct(session)
    cur_dt = datetime.datetime.now()
    ob.create_brand(session, [(cur_dt, [client, brand_client])], cur_dt + datetime.timedelta(1))

    invoice.payment_term_dt = cur_dt - datetime.timedelta(1)
    session.flush()

    overdraft.Overdraft(session).manual_suspect_worker([client.id])
    session.expire_all()

    hamcrest.assert_that(
        [client, brand_client],
        hamcrest.only_contains(
            hamcrest.has_properties(manual_suspect=1)
        )
    )


@pytest.mark.parametrize('client', [0, 1], ids=['client', 'agency'], indirect=True)
def test_unset(session, client, invoice):
    invoice.payment_term_dt = datetime.datetime.now()
    client.manual_suspect = 0
    set_limit(client, 'RUB', 100)

    overdraft.Overdraft(session).manual_suspect_worker([client.id])
    session.expire_all()

    assert client.manual_suspect == 0


@pytest.mark.parametrize('is_agency', [0, 1], ids=['client', 'agency'])
def test_both(session, is_agency):
    client_set = ob.ClientBuilder.construct(session, is_agency=is_agency)
    client_unset = ob.ClientBuilder.construct(session, is_agency=is_agency)

    client_unset.manual_suspect = 1
    invoice = create_invoice(client_set)
    invoice.payment_term_dt = datetime.datetime.now() - datetime.timedelta(666)
    set_limit(client_set, 'RUB', 100)

    overdraft.Overdraft(session).manual_suspect_worker([client_set.id, client_unset.id])
    session.expire_all()

    hamcrest.assert_that(
        [client_set, client_unset],
        hamcrest.contains(
            hamcrest.has_properties(manual_suspect=1),
            hamcrest.has_properties(manual_suspect=0),
        )
    )


@pytest.mark.parametrize(
    'our_fault, is_suspect',
    [
        (0, 1),
        (1, 0),
    ]
)
@pytest.mark.parametrize('client', [0, 1], ids=['client', 'agency'], indirect=True)
def test_bad_debt(session, client, invoice, our_fault, is_suspect):
    invoice.payment_term_dt = datetime.datetime.now() - datetime.timedelta(666)
    invoice.close_invoice(datetime.datetime.now())
    act, = invoice.acts
    ob.BadDebtActBuilder.construct(session, act=act, our_fault=our_fault)
    set_limit(client, 'RUB', 100)

    overdraft.Overdraft(session).manual_suspect_worker([client.id])
    session.expire_all()

    assert client.manual_suspect == is_suspect
