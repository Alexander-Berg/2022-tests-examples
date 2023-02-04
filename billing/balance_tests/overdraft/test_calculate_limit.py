# -*- coding: utf-8 -*-

import datetime

import pytest
import mock

from balance.overdraft.limit import calculate_limit
from balance import muzzle_util as ut
from balance.constants import (
    FirmId,
    ServiceId,
)

from tests import object_builder as ob
from tests.balance_tests.overdraft.common import generate_invoices


@pytest.mark.parametrize('client', [0, 1], ids=['client', 'agency'], indirect=True)
def test_base_multicurrency(session, client):
    client.set_currency(ServiceId.DIRECT, 'RUB', datetime.datetime(2000, 1, 1), None)

    cur_dt = datetime.datetime.now()
    generate_invoices(
        client,
        [
            (ut.add_months_to_date(cur_dt, -i - 1), 60000)
            for i in range(4)
        ]
    )

    limit, _, currency = calculate_limit(session, client, ServiceId.DIRECT, FirmId.YANDEX_OOO)

    assert (limit, currency) == (20000, 'RUB')


@pytest.mark.parametrize('client', [0, 1], ids=['client', 'agency'], indirect=True)
def test_base_fixedcurrency(session, client):
    cur_dt = datetime.datetime.now()
    generate_invoices(
        client,
        [
            (ut.add_months_to_date(cur_dt, -i - 1), 60000)
            for i in range(4)
        ],
        firm_id=FirmId.MARKET,
        service_id=ServiceId.MARKET_VENDORS
    )

    limit, _, currency = calculate_limit(session, client, ServiceId.MARKET_VENDORS, FirmId.MARKET)

    assert (limit, currency) == (20000, 'RUB')


@pytest.mark.linked_clients
@pytest.mark.parametrize('client', [0, 1], ids=['client', 'agency'], indirect=True)
def test_aliases_multicurrency(session, client):
    alias_client = ob.ClientBuilder.construct(session, is_agency=client.is_agency)
    alias_client.make_equivalent(client)

    client.set_currency(ServiceId.DIRECT, 'RUB', datetime.datetime(2000, 1, 1), None)

    cur_dt = datetime.datetime.now()
    generate_invoices(
        client,
        [
            (ut.add_months_to_date(cur_dt, -i - 1), 60000)
            for i in range(2)
        ]
    )
    generate_invoices(
        alias_client,
        [
            (ut.add_months_to_date(cur_dt, -i - 3), 60000)
            for i in range(2)
        ]
    )

    limit, _, currency = calculate_limit(
        session,
        client,
        ServiceId.DIRECT,
        FirmId.YANDEX_OOO
    )
    alias_limit, _, alias_currency = calculate_limit(
        session,
        alias_client,
        ServiceId.DIRECT,
        FirmId.YANDEX_OOO
    )

    assert (limit, currency) == (alias_limit, alias_currency) == (20000, 'RUB')


@pytest.mark.linked_clients
@pytest.mark.parametrize('client', [0, 1], ids=['client', 'agency'], indirect=True)
def test_aliases_fixedcurrency(session, client):
    alias_client = ob.ClientBuilder.construct(session, is_agency=client.is_agency)
    alias_client.make_equivalent(client)

    cur_dt = datetime.datetime.now()
    generate_invoices(
        client,
        [
            (ut.add_months_to_date(cur_dt, -i - 1), 60000)
            for i in range(2)
        ],
        firm_id=FirmId.MARKET,
        service_id=ServiceId.MARKET_VENDORS
    )
    generate_invoices(
        alias_client,
        [
            (ut.add_months_to_date(cur_dt, -i - 3), 60000)
            for i in range(2)
        ],
        firm_id=FirmId.MARKET,
        service_id=ServiceId.MARKET_VENDORS
    )

    limit, _, currency = calculate_limit(
        session,
        client,
        ServiceId.MARKET_VENDORS,
        FirmId.MARKET
    )
    alias_limit, _, alias_currency = calculate_limit(
        session,
        alias_client,
        ServiceId.MARKET_VENDORS,
        FirmId.MARKET
    )

    assert (limit, currency) == (alias_limit, alias_currency) == (20000, 'RUB')


@pytest.mark.linked_clients
def test_brands(session, client):
    cur_dt = datetime.datetime.now()

    brand_client = ob.ClientBuilder.construct(session)
    ob.create_brand(session, [(cur_dt, [client, brand_client])], cur_dt + datetime.timedelta(1))

    client.set_currency(ServiceId.DIRECT, 'RUB', datetime.datetime(2000, 1, 1), None)
    brand_client.set_currency(ServiceId.DIRECT, 'RUB', datetime.datetime(2000, 1, 1), None)

    generate_invoices(
        client,
        [
            (ut.add_months_to_date(cur_dt, -i - 1), 60000)
            for i in range(2)
        ]
    )
    generate_invoices(
        brand_client,
        [
            (ut.add_months_to_date(cur_dt, -i - 3), 60000)
            for i in range(2)
        ]
    )

    limit, _, currency = calculate_limit(
        session,
        client,
        ServiceId.DIRECT,
        FirmId.YANDEX_OOO
    )
    brand_limit, _, brand_currency = calculate_limit(
        session,
        brand_client,
        ServiceId.DIRECT,
        FirmId.YANDEX_OOO
    )

    assert (limit, currency) == (brand_limit, brand_currency) == (20000, 'RUB')


@pytest.mark.taxes_update
@pytest.mark.parametrize('client', [0, 1], ids=['client', 'agency'], indirect=True)
def test_fishes_w_price_conversion(session, client):
    cur_dt = datetime.datetime.now()
    tax_change_dt = ut.add_months_to_date(ut.trunc_date(cur_dt).replace(day=1), -2)
    past = datetime.datetime(2000, 1, 1)

    tax_policy = ob.TaxPolicyBuilder(
        tax_pcts=[
            (past, 18),
            (tax_change_dt, 20)
        ]
    ).build(session).obj

    product = ob.ProductBuilder(
        taxes=tax_policy,
        prices=[(past, 'RUR', 666)]
    ).build(session).obj

    with mock.patch('balance.mapper.products.TAX_POLICY_RUSSIA_RESIDENT', tax_policy.id):
        generate_invoices(
            client,
            [
                (ut.add_months_to_date(cur_dt, -idx - 2), qty)
                for idx, qty in enumerate([80, 90, 100, 110])
            ],
            product_id=product.id
        )
        limit, _, currency = calculate_limit(session, client, ServiceId.DIRECT, FirmId.YANDEX_OOO)

    # (110 + 100 + 90) * 666.0 / 30 * 1.18 + 80 * 666.0 / 30 * 1.2
    assert (limit, currency) == (830, None)
