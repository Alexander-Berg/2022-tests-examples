# -*- coding: utf-8 -*-

import datetime

import hamcrest
import pytest
import mock
from sqlalchemy import orm

from balance import mapper
from balance.utils.sql_alchemy import is_attribute_loaded
from balance.actions import freefunds as a_ff
from balance.constants import (
    DIRECT_PRODUCT_RUB_ID,
    MARKET_FISH_PRODUCT_ID,
)
from tests import object_builder as ob


def create_order(session, qtys, product_id=DIRECT_PRODUCT_RUB_ID, person_type='ph', client=None):
    on_dt = datetime.datetime.now()

    if client is None:
        client = ob.ClientBuilder().build(session).obj
    person = ob.PersonBuilder(client=client, type=person_type).build(session).obj
    order = ob.OrderBuilder(
        client=client,
        product=ob.Getter(mapper.Product, product_id)
    ).build(session).obj

    total_compl_qty = 0
    for cur_qty, compl_qty, act_qty in qtys:
        invoice = ob.InvoiceBuilder(
            person=person,
            request=ob.RequestBuilder(
                basket=ob.BasketBuilder(
                    client=client,
                    rows=[
                        ob.BasketItemBuilder(
                            order=order,
                            quantity=cur_qty
                        )
                    ]
                )
            )
        ).build(session).obj
        invoice.create_receipt(invoice.effective_sum)
        invoice.turn_on_rows()

        if act_qty:
            order.calculate_consumption(on_dt, {order.shipment_type: total_compl_qty + act_qty})
            session.flush()
            invoice.generate_act(backdate=on_dt, force=1)
            session.flush()

        if compl_qty or act_qty:
            order.calculate_consumption(on_dt, {order.shipment_type: total_compl_qty + compl_qty})
            session.flush()
        total_compl_qty += compl_qty
    return order


class TestGetQuery(object):
    def test_acted(self, session):
        order = create_order(
            session,
            [
                (30, 30, 30),
                (20, 7, 6),
                (10, 0, 0),
            ]
        )
        q1, q2, q3 = order.consumes

        query = a_ff.simple.get_query(session, [order.id])
        consumes = list(query)

        assert isinstance(query, orm.Query)
        hamcrest.assert_that(
            consumes,
            hamcrest.contains_inanyorder(q2, q3)
        )

    def test_completed(self, session):
        order = create_order(
            session,
            [
                (30, 30, 0),
                (20, 7, 0),
                (10, 0, 0),
            ]
        )
        q1, q2, q3 = order.consumes

        query = a_ff.simple.get_query(session, [order.id])
        consumes = list(query)

        assert isinstance(query, orm.Query)
        hamcrest.assert_that(
            consumes,
            hamcrest.contains_inanyorder(q2, q3)
        )

    @pytest.mark.parametrize(
        'person_type, product_id, force, res_indexes',
        [
            pytest.param('ur', DIRECT_PRODUCT_RUB_ID, True, [0, 1, 2], id='force'),
            pytest.param('ph', DIRECT_PRODUCT_RUB_ID, False, [0, 1, 2], id='default_force'),
            pytest.param('ur', DIRECT_PRODUCT_RUB_ID, False, [1, 2], id='wrong_category'),
            pytest.param('ph', MARKET_FISH_PRODUCT_ID, False, [1, 2], id='wrong_product'),
        ]
    )
    def test_transfer_acted(self, session, person_type, product_id, force, res_indexes):
        order = create_order(
            session,
            [
                (30, 21, 30),
                (20, 3, 14),
                (10, 0, 0),
            ],
            product_id=product_id,
            person_type=person_type
        )

        query = a_ff.simple.get_query(session, [order.id], force_transfer_acted=force)
        consumes = list(query)

        hamcrest.assert_that(
            consumes,
            hamcrest.contains_inanyorder(*[order.consumes[idx] for idx in res_indexes])
        )

    @pytest.mark.parametrize(
        'old_first, res_indexes',
        [
            pytest.param(True, [1, 2], id='old_first'),
            pytest.param(False, [2, 1], id='new_first'),
        ]
    )
    def test_sort(self, session, old_first, res_indexes):
        order = create_order(
            session,
            [
                (30, 30, 0),
                (20, 7, 0),
                (10, 0, 0),
            ]
        )

        query = a_ff.simple.get_query(session, [order.id], old_consumes_first=old_first)
        consumes = list(query)

        assert isinstance(query, orm.Query)
        hamcrest.assert_that(
            consumes,
            hamcrest.contains(*[order.consumes[idx] for idx in res_indexes])
        )

    @pytest.mark.parametrize(
        'options, expected_is_loaded_value',
        [
            pytest.param(None, False, id='wo_options'),
            pytest.param([orm.joinedload(mapper.Consume.order)], True, id='w_options'),
        ]
    )
    def test_query_options(self, session, options, expected_is_loaded_value):
        order_id = create_order(session, [(30, 30, 0), (10, 0, 0)]).id
        session.expire_all()

        query = a_ff.simple.get_query(
            session,
            [order_id],
            force_transfer_acted=True,
            query_options=options
        )
        consume, = query

        assert is_attribute_loaded(consume, 'order') is expected_is_loaded_value

    def test_orders_subquery(self, session):
        orders = [
            create_order(session, [(10, 0, 0)]),
            create_order(session, [(20, 0, 0)]),
            create_order(session, [(30, 0, 0)]),
        ]

        subquery = session.query(mapper.Order.id).filter(mapper.Order.id.in_(o.id for o in orders))

        query = a_ff.simple.get_query(session, subquery)
        consumes = list(query)

        hamcrest.assert_that(
            consumes,
            hamcrest.contains_inanyorder(*[
                hamcrest.has_properties(parent_order_id=o.id)
                for o in orders
            ])
        )


class TestGetByOrders(object):
    @pytest.mark.parametrize(
        'batch_size',
        [
            pytest.param(10, id='single_group'),
            pytest.param(2, id='multiple_groups'),
        ]
    )
    def test_groups(self, session, batch_size):
        o1 = create_order(session, [(30, 0, 0)])
        o2 = create_order(session, [(20, 20, 0)])
        o3 = create_order(session, [(10, 0, 0)])

        with mock.patch('balance.constants.ORACLE_MAX_IN_CONDITION_ENTRIES', batch_size):
            res_consumes = a_ff.simple.get_by_orders(session, [o1.id, o2.id, o3.id])

        hamcrest.assert_that(
            res_consumes,
            hamcrest.contains_inanyorder(
                hamcrest.has_properties(parent_order_id=o1.id),
                hamcrest.has_properties(parent_order_id=o3.id),
            )
        )

    def test_sort(self, session):
        order = create_order(
            session,
            [
                (30, 0, 0),
                (20, 0, 0),
                (10, 0, 0),
            ]
        )

        res_consumes = a_ff.simple.get_by_orders(session, [order.id], old_consumes_first=False)

        hamcrest.assert_that(
            res_consumes,
            hamcrest.contains(*list(reversed(order.consumes)))
        )

    def test_transfer_acted(self, session):
        o1 = create_order(session, [(30, 0, 30)], product_id=MARKET_FISH_PRODUCT_ID)
        o2 = create_order(session, [(20, 20, 20)], product_id=MARKET_FISH_PRODUCT_ID)
        o3 = create_order(session, [(10, 0, 10)], product_id=MARKET_FISH_PRODUCT_ID)

        res_consumes = a_ff.simple.get_by_orders(session, [o1.id, o2.id, o3.id], force_transfer_acted=True)

        hamcrest.assert_that(
            res_consumes,
            hamcrest.contains(
                hamcrest.has_properties(parent_order_id=o1.id),
                hamcrest.has_properties(parent_order_id=o3.id),
            )
        )

    @pytest.mark.parametrize(
        'options, expected_is_loaded_value',
        [
            pytest.param(None, False, id='wo_options'),
            pytest.param([orm.joinedload(mapper.Consume.order)], True, id='w_options'),
        ]
    )
    def test_query_options(self, session, options, expected_is_loaded_value):
        order_id = create_order(session, [(10, 0, 0)]).id
        session.expire_all()

        consume, = a_ff.simple.get_by_orders(
            session,
            [order_id],
            force_transfer_acted=True,
            query_options=options
        )

        assert is_attribute_loaded(consume, 'order') is expected_is_loaded_value
