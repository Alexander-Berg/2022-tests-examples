# -*- coding: utf-8 -*-

import datetime

import pytest
import hamcrest as hm

from balance import exc
from balance.actions.unified_account import UnifiedAccountRelations
from balance.constants import (
    ServiceId,
    UAChildType,
    OrderLogTariffState,
    DIRECT_PRODUCT_ID,
    DIRECT_PRODUCT_RUB_ID,
    DIRECT_MEDIA_PRODUCT_RUB_ID,
    CONVERT_TYPE_MODIFY,
    ExportState,
)

from tests import object_builder as ob

pytestmark = [
    pytest.mark.log_tariff,
]


@pytest.fixture
def client(session):
    return ob.ClientBuilder.construct(session)


def mk_currency(client):
    client.set_currency(ServiceId.DIRECT, 'RUB', datetime.datetime(2000, 1, 1), None)


class TestTurnOn(object):
    def test_not_direct(self, session, client):
        order = ob.OrderBuilder.construct(
            session,
            client=client,
            service_id=ServiceId.MEDIA_SELLING,
            product_id=DIRECT_PRODUCT_ID
        )

        with pytest.raises(exc.ORDER_LOG_TARIFF_TURN_ON_ERROR) as exc_info:
            order.turn_on_log_tariff()

        assert 'not direct' in exc_info.value.msg

    @pytest.mark.parametrize(
        'tariffed_main, child_ua_type',
        [
            pytest.param(True, UAChildType.LOG_TARIFF, id='tariffed'),
            pytest.param(False, UAChildType.TRANSFERS, id='old'),
        ],
    )
    def test_child_order(self, session, client, tariffed_main, child_ua_type):
        # Для дочерних заказов не включаем turn_on_log_tariff
        main_order = ob.OrderBuilder.construct(session, client=client, product_id=DIRECT_PRODUCT_ID)
        mk_currency(main_order.client)
        child_order = ob.OrderBuilder.construct(session, client=client, product_id=DIRECT_PRODUCT_ID)

        if tariffed_main:
            main_order.force_log_tariff()
            session.flush()
        UnifiedAccountRelations().link(main_order, [child_order])

        child_order.turn_on_log_tariff()
        assert child_order.is_log_tariff is False
        assert child_order.child_ua_type == child_ua_type

    def test_not_converted(self, session, client):
        main_order = ob.OrderBuilder.construct(session, client=client, product_id=DIRECT_PRODUCT_ID)
        child_order = ob.OrderBuilder.construct(session, client=client, product_id=DIRECT_PRODUCT_ID)
        UnifiedAccountRelations().link(main_order, [child_order])

        with pytest.raises(exc.ORDER_UA_OPTIMIZE_TURN_ON_ERROR):
            main_order.turn_on_log_tariff()

    def test_converted_transfers(self, session, client):
        mk_currency(client)

        main_order = ob.OrderBuilder.construct(session, client=client, product_id=DIRECT_PRODUCT_RUB_ID)
        child_order = ob.OrderBuilder.construct(session, client=client, product_id=DIRECT_PRODUCT_RUB_ID)
        UnifiedAccountRelations().link(main_order, [child_order])

        assert not main_order.is_ua_optimize
        assert child_order.child_ua_type == UAChildType.TRANSFERS

        main_order.turn_on_log_tariff()

        assert main_order.is_ua_optimize
        assert main_order._is_log_tariff == OrderLogTariffState.INIT
        assert child_order.child_ua_type == UAChildType.LOG_TARIFF

    def test_optimized(self, session, client):
        mk_currency(client)

        main_order = ob.OrderBuilder.construct(session, client=client, product_id=DIRECT_PRODUCT_RUB_ID)
        child_order = ob.OrderBuilder.construct(session, client=client, product_id=DIRECT_PRODUCT_RUB_ID)
        child_order_transfers = ob.OrderBuilder.construct(
            session,
            client=client,
            product_id=DIRECT_MEDIA_PRODUCT_RUB_ID
        )
        UnifiedAccountRelations().link(main_order, [child_order, child_order_transfers])
        main_order.turn_on_optimize()

        assert main_order.is_ua_optimize
        assert child_order.child_ua_type == UAChildType.OPTIMIZED
        assert child_order_transfers.child_ua_type == UAChildType.TRANSFERS

        main_order.turn_on_log_tariff()

        assert main_order.is_ua_optimize
        assert main_order._is_log_tariff == OrderLogTariffState.INIT
        assert child_order.child_ua_type == UAChildType.LOG_TARIFF
        assert child_order_transfers.child_ua_type == UAChildType.LOG_TARIFF

    def test_move_main_log_tariff_to_child(self, session, client):
        mk_currency(client)

        main_order = ob.OrderBuilder.construct(session, client=client, product_id=DIRECT_PRODUCT_RUB_ID)
        main_order.turn_on_log_tariff()
        main_order._is_log_tariff = OrderLogTariffState.MIGRATED
        child_order = ob.OrderBuilder.construct(session, client=client, product_id=DIRECT_PRODUCT_RUB_ID)
        child_order.turn_on_log_tariff()
        child_order._is_log_tariff = OrderLogTariffState.MIGRATED

        UnifiedAccountRelations().link(main_order, [child_order])

        assert main_order.is_ua_optimize
        assert main_order.is_log_tariff
        assert child_order.is_ua_optimize == 0
        assert child_order._is_log_tariff is None
        assert child_order.child_ua_type == UAChildType.LOG_TARIFF


class TestForce(object):
    def test_money(self, session):
        order = ob.OrderBuilder.construct(session, service_id=ServiceId.DIRECT, product_id=DIRECT_PRODUCT_RUB_ID)
        mk_currency(order.client)

        order.force_log_tariff()

        hm.assert_that(
            order,
            hm.has_properties(
                is_ua_optimize=False,
                _is_log_tariff=OrderLogTariffState.MIGRATED,
                child_ua_type=None,
            )
        )

    def test_bucks(self, session):
        order = ob.OrderBuilder.construct(session, service_id=ServiceId.DIRECT, product_id=DIRECT_PRODUCT_ID)
        client = order.client
        client.set_currency(ServiceId.DIRECT, 'RUB', datetime.datetime(2000, 1, 1), CONVERT_TYPE_MODIFY)
        client.exports['MIGRATE_TO_CURRENCY'].state = ExportState.exported
        client.exports['MIGRATE_TO_CURRENCY'].export_dt = datetime.datetime.now()
        session.flush()

        order.force_log_tariff()

        hm.assert_that(
            order,
            hm.has_properties(
                is_ua_optimize=False,
                _is_log_tariff=OrderLogTariffState.MIGRATED,
                child_ua_type=None,
                completion_consumed_money=0,
                shipment=hm.has_properties(
                    bucks=0,
                )
            )
        )

    def test_completed(self, session):
        order = ob.OrderBuilder.construct(session, service_id=ServiceId.DIRECT, product_id=DIRECT_PRODUCT_RUB_ID)
        mk_currency(order.client)
        order.calculate_consumption(datetime.datetime.now(), {order.shipment_type: 1})

        with pytest.raises(exc.ORDER_LOG_TARIFF_TURN_ON_ERROR) as exc_info:
            order.force_log_tariff()

        assert 'order with completions' in exc_info.value.msg
        hm.assert_that(
            order,
            hm.has_properties(
                is_ua_optimize=0,
                _is_log_tariff=None,
                child_ua_type=None,
            )
        )

    def test_w_children(self, session):
        main_order = ob.OrderBuilder.construct(session, service_id=ServiceId.DIRECT, product_id=DIRECT_PRODUCT_RUB_ID)
        child_order = ob.OrderBuilder.construct(
            session,
            service_id=ServiceId.DIRECT,
            product_id=DIRECT_PRODUCT_RUB_ID,
            group_order_id=main_order.id,
            client=main_order.client
        )
        mk_currency(main_order.client)

        main_order.force_log_tariff()

        hm.assert_that(
            main_order,
            hm.has_properties(
                is_ua_optimize=1,
                _is_log_tariff=OrderLogTariffState.MIGRATED,
                child_ua_type=None,
            )
        )
        hm.assert_that(
            child_order,
            hm.has_properties(
                is_ua_optimize=0,
                _is_log_tariff=None,
                child_ua_type=UAChildType.LOG_TARIFF,
            )
        )

    def test_not_converted(self, session):
        order = ob.OrderBuilder.construct(session, service_id=ServiceId.DIRECT, product_id=DIRECT_PRODUCT_RUB_ID)
        ob.OrderBuilder.construct(
            session,
            service_id=ServiceId.DIRECT,
            product_id=DIRECT_PRODUCT_RUB_ID,
            parent_group_order=order
        )

        with pytest.raises(exc.ORDER_UA_OPTIMIZE_TURN_ON_ERROR):
            order.force_log_tariff()

        hm.assert_that(
            order,
            hm.has_properties(
                is_ua_optimize=0,
                _is_log_tariff=None,
                child_ua_type=None,
            )
        )
