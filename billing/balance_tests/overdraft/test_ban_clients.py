# -*- coding: utf-8 -*-

import datetime

import pytest
import hamcrest

from balance import overdraft
from balance import mapper
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
    'is_overdraft, term_delta, receipt_sum, is_banned, limit',
    [
        pytest.param(1, 666, 0, 1, 0, id='overdraft_old'),
        pytest.param(1, 666, 60, 1, 0, id='overdraft_partial_paid'),
        pytest.param(1, 666, 666, 0, 100, id='overdraft_paid'),
        pytest.param(1, 15, 0, 0, 100, id='overdraft_new'),
        pytest.param(0, 66, 0, 0, 100, id='prepayment'),
    ]
)
@pytest.mark.parametrize('client', [0, 1], ids=['client', 'agency'], indirect=True)
def test_ban(session, client, is_overdraft, receipt_sum, term_delta, is_banned, limit):
    invoice = create_invoice(client, is_overdraft)
    invoice.payment_term_dt = datetime.datetime.now() - datetime.timedelta(term_delta)
    if receipt_sum:
        invoice.receipt_sum_1c = receipt_sum
    set_limit(client, 'RUB', 100)

    overdraft.Overdraft(session).ban_clients([client.id])
    session.expire_all()

    hamcrest.assert_that(
        client,
        hamcrest.has_properties(
            overdraft_ban=is_banned,
            overdraft=hamcrest.has_entry(
                (ServiceId.DIRECT, FirmId.YANDEX_OOO),
                hamcrest.has_properties(overdraft_limit=limit)
            ),
        )
    )


@pytest.mark.linked_clients
@pytest.mark.parametrize('client', [0, 1], ids=['client', 'agency'], indirect=True)
def test_alias_ban(session, client, invoice):
    alias_client = ob.ClientBuilder.construct(session, is_agency=client.is_agency)
    alias_client.make_equivalent(client)

    invoice.payment_term_dt = datetime.datetime.now() - datetime.timedelta(666)
    set_limit(client, 'RUB', 100)
    set_limit(alias_client, 'RUB', 100)

    overdraft.Overdraft(session).ban_clients([client.id])
    session.expire_all()

    hamcrest.assert_that(
        [client, alias_client],
        hamcrest.only_contains(
            hamcrest.has_properties(
                overdraft_ban=1,
                overdraft=hamcrest.has_entry(
                    (ServiceId.DIRECT, FirmId.YANDEX_OOO),
                    hamcrest.has_properties(overdraft_limit=0)
                ),
            )
        )
    )


@pytest.mark.parametrize('client', [0, 1], ids=['client', 'agency'], indirect=True)
def test_unban(session, client, invoice):
    invoice.payment_term_dt = datetime.datetime.now() - datetime.timedelta(666)
    invoice.receipt_sum_1c = invoice.effective_sum.as_decimal()
    invoice.create_receipt(invoice.effective_sum)
    client.overdraft_ban = 1
    set_limit(client, 'RUB', 0)

    overdraft.Overdraft(session).ban_clients([client.id])
    session.expire_all()

    hamcrest.assert_that(
        client,
        hamcrest.has_properties(
            overdraft_ban=0,
            overdraft={}
        )
    )


@pytest.mark.parametrize('is_agency', [0, 1], ids=['client', 'agency'])
def test_both(session, is_agency):
    client_ban = ob.ClientBuilder.construct(session, is_agency=is_agency)
    client_unban = ob.ClientBuilder.construct(session, is_agency=is_agency)

    client_unban.overdraft_ban = 1
    invoice = create_invoice(client_ban)
    invoice.payment_term_dt = datetime.datetime.now() - datetime.timedelta(666)
    session.flush()

    overdraft.Overdraft(session).ban_clients([client_ban.id, client_unban.id])
    session.expire_all()

    hamcrest.assert_that(
        [client_ban, client_unban],
        hamcrest.contains(
            hamcrest.has_properties(overdraft_ban=1),
            hamcrest.has_properties(overdraft_ban=0),
        )
    )


@pytest.mark.parametrize(
    'our_fault, is_banned',
    [
        (0, 1),
        (1, 0),
    ]
)
@pytest.mark.parametrize('client', [0, 1], ids=['client', 'agency'], indirect=True)
def test_bad_debt(session, client, invoice, our_fault, is_banned):
    invoice.payment_term_dt = datetime.datetime.now() - datetime.timedelta(666)
    invoice.close_invoice(datetime.datetime.now())
    act, = invoice.acts
    ob.BadDebtActBuilder.construct(session, act=act, our_fault=our_fault)
    set_limit(client, 'RUB', 100)

    overdraft.Overdraft(session).ban_clients([client.id])
    session.expire_all()

    assert client.overdraft_ban == is_banned


@pytest.mark.parametrize('external_limit', [True, False])
@pytest.mark.parametrize('client', [0, 1], ids=['client', 'agency'], indirect=True)
def test_fixed_limit_ban(session, client, external_limit):
    invoice = create_invoice(client)
    invoice.payment_term_dt = datetime.datetime.now() - datetime.timedelta(999)
    set_limit(client, 'RUB', 100)

    if external_limit:
        client_external_overdraft = mapper.ClientExternalOverdraft(
            client_id=client.id,
            service_id=ServiceId.DIRECT,
            iso_currency='RUB',
            overdraft_limit=100
        )
        session.add(client_external_overdraft)
        session.flush()

    client_overdraft = client.overdraft.get((ServiceId.DIRECT, FirmId.YANDEX_OOO))
    client_overdraft.fixed_limit = 1
    session.flush()

    overdraft.Overdraft(session).ban_clients([client.id])
    session.expire_all()

    hamcrest.assert_that(
        client,
        hamcrest.has_properties(
            overdraft_ban=1,
            overdraft=hamcrest.has_entry(
                (ServiceId.DIRECT, FirmId.YANDEX_OOO),
                hamcrest.has_properties(overdraft_limit=0, fixed_limit=None)
            ),
        )
    )
