# -*- coding: utf-8 -*-

import datetime

import pytest
import hamcrest

from balance import mapper
from balance import muzzle_util as ut
from billing.contract_iface.cmeta.general import collateral_types
from billing.contract_iface import ContractTypeId, CollateralType
from balance.constants import (
    ClientLinkType,
    FirmId,
)

from tests import object_builder as ob

pytestmark = [
    pytest.mark.linked_clients,
]

END_DT = datetime.datetime(3000, 1, 1)


def get_links(session, group_id, group_type):
    return (
        session.query(mapper.ClientLinkHistory)
            .filter(mapper.ClientLinkHistory.group_id == group_id,
                    mapper.ClientLinkHistory.group_type == group_type)
            .all()
    )


def create_brand(dt, clients, finish_dt=None, is_signed=True, brand_type=ClientLinkType.DIRECT, **kwargs):
    main_client = clients[0]
    params = dict(
        client=main_client,
        commission=ContractTypeId.ADVERTISING_BRAND,
        firm=FirmId.YANDEX_OOO,
        dt=dt,
        payment_type=None,
        finish_dt=finish_dt,
        brand_type=brand_type,
        brand_clients={cl.id: 1 for cl in clients},
        is_signed=dt if is_signed else None,
    )
    params.update(kwargs)
    brand = ob.ContractBuilder.construct(main_client.session, **params)
    main_client.session.flush()
    return brand


def add_collateral(brand, dt, clients, finish_dt=None):
    col_type = collateral_types[1026]
    col = brand.append_collateral(
        dt,
        col_type,
        brand_clients={cl.id: 1 for cl in clients},
        finish_dt=finish_dt,
        is_signed=dt
    )
    brand.session.flush()
    return col


class TestEquivalent(object):
    def test_add(self, session):
        main_client = ob.ClientBuilder.construct(session)
        client1 = ob.ClientBuilder.construct(session)
        client2 = ob.ClientBuilder.construct(session)

        client1.make_equivalent(main_client)
        client2.make_equivalent(main_client)

        links = get_links(session, main_client.id, ClientLinkType.ALIAS)
        hamcrest.assert_that(
            links,
            hamcrest.contains_inanyorder(*[
                hamcrest.has_properties(client_id=cl.id, from_dt=datetime.datetime(1000, 1, 1), till_dt=END_DT)
                for cl in [main_client, client1, client2]
            ])
        )


class TestBrand(object):
    @pytest.mark.parametrize('brand_type', ClientLinkType.BRAND_TYPES)
    def test_base(self, session, brand_type):
        clients = [ob.ClientBuilder.construct(session) for _ in range(5)]

        start_dt = ut.trunc_date(datetime.datetime.now()) - datetime.timedelta(1)
        brand = create_brand(start_dt, clients, brand_type=brand_type)

        links = get_links(session, brand.id, brand_type)
        hamcrest.assert_that(
            links,
            hamcrest.contains_inanyorder(*[
                hamcrest.has_properties(client_id=cl.id, from_dt=start_dt, till_dt=END_DT)
                for cl in clients
            ])
        )

    def test_finish_dt(self, session):
        clients = [ob.ClientBuilder.construct(session) for _ in range(3)]

        start_dt = ut.trunc_date(datetime.datetime.now()) - datetime.timedelta(1)
        finish_dt = ut.trunc_date(datetime.datetime.now()) + datetime.timedelta(1)
        brand = create_brand(start_dt, clients, finish_dt)

        links = get_links(session, brand.id, ClientLinkType.DIRECT)
        hamcrest.assert_that(
            links,
            hamcrest.contains_inanyorder(*[
                hamcrest.has_properties(client_id=cl.id, from_dt=start_dt, till_dt=finish_dt)
                for cl in clients
            ])
        )

    def test_unsigned(self, session):
        clients = [ob.ClientBuilder.construct(session) for _ in range(2)]

        start_dt = ut.trunc_date(datetime.datetime.now()) - datetime.timedelta(1)
        brand = create_brand(start_dt, clients, is_signed=False, brand_type=ClientLinkType.DIRECT)

        links = get_links(session, brand.id, ClientLinkType.DIRECT)
        assert links == []

    def test_unsigned_clear_cache(self, session):
        clients = [ob.ClientBuilder.construct(session) for _ in range(5)]

        start_dt = ut.trunc_date(datetime.datetime.now()) - datetime.timedelta(1)
        brand = create_brand(start_dt, clients, brand_type=ClientLinkType.DIRECT)

        links = get_links(session, brand.id, ClientLinkType.DIRECT)
        hamcrest.assert_that(
            links,
            hamcrest.contains_inanyorder(*[
                hamcrest.has_properties(client_id=cl.id, from_dt=start_dt, till_dt=END_DT)
                for cl in clients
            ])
        )

        brand.col0.is_signed = None
        session.flush()

        links = get_links(session, brand.id, ClientLinkType.DIRECT)
        assert links == []

    def test_cancelled(self, session):
        clients = [ob.ClientBuilder.construct(session) for _ in range(5)]

        start_dt = ut.trunc_date(datetime.datetime.now()) - datetime.timedelta(1)
        brand = create_brand(start_dt, clients, brand_type=ClientLinkType.DIRECT)

        links = get_links(session, brand.id, ClientLinkType.DIRECT)
        hamcrest.assert_that(
            links,
            hamcrest.contains_inanyorder(*[
                hamcrest.has_properties(client_id=cl.id, from_dt=start_dt, till_dt=END_DT)
                for cl in clients
            ])
        )

        brand.col0.is_cancelled = datetime.datetime.now()
        session.flush()

        links = get_links(session, brand.id, ClientLinkType.DIRECT)
        assert links == []

    def test_collateral_add(self, session):
        client1, client2, client3 = [ob.ClientBuilder.construct(session) for _ in range(3)]

        col_dt = ut.trunc_date(datetime.datetime.now())
        start_dt = col_dt - datetime.timedelta(1)
        brand = create_brand(start_dt, [client1, client2])
        add_collateral(brand, col_dt, [client1, client2, client3])

        links = get_links(session, brand.id, ClientLinkType.DIRECT)
        hamcrest.assert_that(
            links,
            hamcrest.contains_inanyorder(
                hamcrest.has_properties(client_id=client1.id, from_dt=start_dt, till_dt=END_DT),
                hamcrest.has_properties(client_id=client2.id, from_dt=start_dt, till_dt=END_DT),
                hamcrest.has_properties(client_id=client3.id, from_dt=col_dt, till_dt=END_DT),
            )
        )

    def test_collateral_remove(self, session):
        client1, client2, client3 = [ob.ClientBuilder.construct(session) for _ in range(3)]

        col_dt = ut.trunc_date(datetime.datetime.now())
        start_dt = col_dt - datetime.timedelta(1)
        brand = create_brand(start_dt, [client1, client2, client3])
        add_collateral(brand, col_dt, [client1, client2])

        links = get_links(session, brand.id, ClientLinkType.DIRECT)
        hamcrest.assert_that(
            links,
            hamcrest.contains_inanyorder(
                hamcrest.has_properties(client_id=client1.id, from_dt=start_dt, till_dt=END_DT),
                hamcrest.has_properties(client_id=client2.id, from_dt=start_dt, till_dt=END_DT),
                hamcrest.has_properties(client_id=client3.id, from_dt=start_dt, till_dt=col_dt),
            )
        )

    def test_collateral_change(self, session):
        client1, client2, client3 = [ob.ClientBuilder.construct(session) for _ in range(3)]

        col_dt = ut.trunc_date(datetime.datetime.now())
        start_dt = col_dt - datetime.timedelta(1)
        brand = create_brand(start_dt, [client1, client2])
        add_collateral(brand, col_dt, [client1, client3])

        links = get_links(session, brand.id, ClientLinkType.DIRECT)
        hamcrest.assert_that(
            links,
            hamcrest.contains_inanyorder(
                hamcrest.has_properties(client_id=client1.id, from_dt=start_dt, till_dt=END_DT),
                hamcrest.has_properties(client_id=client2.id, from_dt=start_dt, till_dt=col_dt),
                hamcrest.has_properties(client_id=client3.id, from_dt=col_dt, till_dt=END_DT),
            )
        )

    def test_multiple_collaterals(self, session):
        client1, client2, client3, client4 = [ob.ClientBuilder.construct(session) for _ in range(4)]

        start_dt = ut.trunc_date(datetime.datetime.now())
        col1_dt = start_dt + datetime.timedelta(1)
        col2_dt = start_dt + datetime.timedelta(2)

        brand = create_brand(start_dt, [client1, client2, client3])
        add_collateral(brand, col1_dt, [client1, client2])
        add_collateral(brand, col2_dt, [client1, client3, client4])

        links = get_links(session, brand.id, ClientLinkType.DIRECT)
        hamcrest.assert_that(
            links,
            hamcrest.contains_inanyorder(
                hamcrest.has_properties(client_id=client1.id, from_dt=start_dt, till_dt=END_DT),
                hamcrest.has_properties(client_id=client2.id, from_dt=start_dt, till_dt=col2_dt),
                hamcrest.has_properties(client_id=client3.id, from_dt=start_dt, till_dt=col1_dt),
                hamcrest.has_properties(client_id=client3.id, from_dt=col2_dt, till_dt=END_DT),
                hamcrest.has_properties(client_id=client4.id, from_dt=col2_dt, till_dt=END_DT),
            )
        )

    def test_collateral_finish_dt(self, session):
        clients = [ob.ClientBuilder.construct(session) for _ in range(3)]

        col_dt = ut.trunc_date(datetime.datetime.now())
        start_dt = col_dt - datetime.timedelta(2)
        brand = create_brand(start_dt, clients)

        brand.append_collateral(
            col_dt,
            collateral_types[CollateralType.CONTRACT_CANCELLATION],
            finish_dt=col_dt,
            is_signed=col_dt
        )
        brand.session.flush()

        links = get_links(session, brand.id, ClientLinkType.DIRECT)
        hamcrest.assert_that(
            links,
            hamcrest.contains_inanyorder(*[
                hamcrest.has_properties(
                    client_id=cl.id,
                    from_dt=start_dt,
                    till_dt=col_dt,
                )
                for cl in clients
            ])
        )


class TestBrandEquivalent(object):
    def test_main_client_in_brand(self, session):
        main_client = ob.ClientBuilder.construct(session)
        eq_client = ob.ClientBuilder.construct(session)
        brand_client = ob.ClientBuilder.construct(session)

        eq_client.make_equivalent(main_client)

        start_dt = ut.trunc_date(datetime.datetime.now()) - datetime.timedelta(1)
        brand = create_brand(start_dt, [main_client, brand_client], brand_type=ClientLinkType.DIRECT)

        links = get_links(session, brand.id, ClientLinkType.DIRECT)
        hamcrest.assert_that(
            links,
            hamcrest.contains_inanyorder(*[
                hamcrest.has_properties(client_id=cl.id, from_dt=start_dt, till_dt=END_DT)
                for cl in [main_client, eq_client, brand_client]
            ])
        )

        links = get_links(session, main_client.id, ClientLinkType.ALIAS)
        hamcrest.assert_that(
            links,
            hamcrest.contains_inanyorder(*[
                hamcrest.has_properties(client_id=cl.id, from_dt=datetime.datetime(1000, 1, 1), till_dt=END_DT)
                for cl in [main_client, eq_client]
            ])
        )

    def test_main_client_not_in_brand(self, session):
        main_client = ob.ClientBuilder.construct(session)
        eq_client = ob.ClientBuilder.construct(session)
        brand_client = ob.ClientBuilder.construct(session)

        eq_client.make_equivalent(main_client)

        start_dt = ut.trunc_date(datetime.datetime.now()) - datetime.timedelta(1)
        brand = create_brand(start_dt, [eq_client, brand_client], brand_type=ClientLinkType.DIRECT)

        links = get_links(session, brand.id, ClientLinkType.DIRECT)
        hamcrest.assert_that(
            links,
            hamcrest.contains_inanyorder(*[
                hamcrest.has_properties(client_id=cl.id, from_dt=start_dt, till_dt=END_DT)
                for cl in [main_client, eq_client, brand_client]
            ])
        )

        links = get_links(session, main_client.id, ClientLinkType.ALIAS)
        hamcrest.assert_that(
            links,
            hamcrest.contains_inanyorder(*[
                hamcrest.has_properties(client_id=cl.id, from_dt=datetime.datetime(1000, 1, 1), till_dt=END_DT)
                for cl in [main_client, eq_client]
            ])
        )

    def test_many_aliases(self, session):
        eq_client_1 = ob.ClientBuilder.construct(session)
        eq_client_2 = ob.ClientBuilder.construct(session)  # эквивалентен eq_client_1
        eq_client_3 = ob.ClientBuilder.construct(session)  # эквивалентен brand_client_2

        brand_client_1 = ob.ClientBuilder.construct(session)  # эквивалентен eq_client_1
        brand_client_2 = ob.ClientBuilder.construct(session)

        brand_client_1.make_equivalent(eq_client_1)
        eq_client_2.make_equivalent(eq_client_1)

        eq_client_3.make_equivalent(brand_client_2)

        start_dt = ut.trunc_date(datetime.datetime.now()) - datetime.timedelta(1)
        brand = create_brand(start_dt, [brand_client_1, brand_client_2], brand_type=ClientLinkType.DIRECT)

        links = get_links(session, brand.id, ClientLinkType.DIRECT)
        hamcrest.assert_that(
            links,
            hamcrest.contains_inanyorder(*[
                hamcrest.has_properties(client_id=cl.id, from_dt=start_dt, till_dt=END_DT)
                for cl in [eq_client_1, eq_client_2, eq_client_3, brand_client_1, brand_client_2]
            ])
        )

        links = get_links(session, eq_client_1.id, ClientLinkType.ALIAS)
        hamcrest.assert_that(
            links,
            hamcrest.contains_inanyorder(*[
                hamcrest.has_properties(client_id=cl.id, from_dt=datetime.datetime(1000, 1, 1), till_dt=END_DT)
                for cl in [eq_client_1, brand_client_1, eq_client_2]
            ])
        )

        links = get_links(session, brand_client_2.id, ClientLinkType.ALIAS)
        hamcrest.assert_that(
            links,
            hamcrest.contains_inanyorder(*[
                hamcrest.has_properties(client_id=cl.id, from_dt=datetime.datetime(1000, 1, 1), till_dt=END_DT)
                for cl in [brand_client_2, eq_client_3]
            ])
        )
