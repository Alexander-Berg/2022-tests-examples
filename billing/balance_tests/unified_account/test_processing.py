# -*- coding: utf-8 -*-

import datetime
import time
import decimal
import functools

import pytest
import hamcrest

from balance import mapper
from balance import muzzle_util as ut
from balance.actions.invoice_turnon import InvoiceTurnOn
from balance.actions import unified_account as a_ua
from balance.actions import process_completions as a_pc
from balance.processors import client_migrate_to_currency
from balance.queue_processor import QueueProcessor
from balance.constants import (
    DIRECT_RUB_UNIT_ID,
    DIRECT_USD_UNIT_ID,
    DIRECT_PRODUCT_ID,
    DIRECT_PRODUCT_RUB_ID,
    DIRECT_MEDIA_PRODUCT_RUB_ID,
    DIRECT_PRODUCT_USD_ID,
    CONVERT_TYPE_MODIFY,
    CONVERT_TYPE_COPY,
    UAChildType,
    OrderLogTariffState,
)
from butils import decimal_unit

from tests.base_routine import (
    consumes_match,
)
from tests import object_builder as ob

D = decimal.Decimal
DU = decimal_unit.DecimalUnit
PAYSYS_BANK_USD_NONRES = 1013
PAYSYS_BANK_RUB_NONRES = 1014


@pytest.fixture
def client(session):
    return ob.ClientBuilder().build(session).obj


@pytest.fixture
def media_rub_product(session):
    return ob.ProductBuilder(
        price=1,
        unit=ob.Getter(mapper.ProductUnit, DIRECT_RUB_UNIT_ID),
        media_discount=1,
        commission_type=1
    ).build(session).obj


@pytest.fixture
def media_usd_product(session):
    return ob.ProductBuilder(
        price=1,
        unit=ob.Getter(mapper.ProductUnit, DIRECT_USD_UNIT_ID),
        media_discount=1,
        commission_type=1
    ).build(session).obj


def migrate_client(client, convert_type, on_dt=None):
    currency = 'RUB'
    migrate_to_currency = on_dt
    if not migrate_to_currency:
        migrate_to_currency = (
            ut.trunc_date(datetime.datetime.now())
            if convert_type else datetime.datetime(2000, 1, 1)
        )
    service_id = 7

    client.set_currency(service_id, currency, migrate_to_currency, convert_type, force=True)
    if convert_type:
        client_migrate_to_currency.process_client(client, client.exports['MIGRATE_TO_CURRENCY'].input)
        client.session.flush()


def create_orders(session, client, main_product_id, product_ids):
    main_order = ob.OrderBuilder(
        client=client,
        product=ob.Getter(mapper.Product, main_product_id)
    ).build(session).obj

    orders = [
        ob.OrderBuilder(
            group_order_id=main_order.id,
            client=client,
            product=ob.Getter(mapper.Product, product_id)
        ).build(session).obj
        for product_id in product_ids
    ]

    return main_order, orders


def create_invoice(client, orders_qtys, dt=None, paysys_id=ob.PAYSYS_ID, person_type='ph'):
    invoice = ob.InvoiceBuilder(
        paysys=ob.Getter(mapper.Paysys, paysys_id),
        person=ob.PersonBuilder(client=client, type=person_type),
        request=ob.RequestBuilder(
            basket=ob.BasketBuilder(
                client=client,
                rows=[
                    ob.BasketItemBuilder(order=order, quantity=qty)
                    for order, qty in orders_qtys
                ])
        ),
        dt=dt or datetime.datetime.now()
    ).build(client.session).obj
    invoice.create_receipt(invoice.effective_sum)
    invoice.turn_on_rows(cut_agava=True, on_dt=dt)
    return invoice


class TestUnifiedAccount(object):
    def _do_ua(self, order, for_dt=None, force_tariff_migration=False):
        a_ua.handle_orders(order.session, [order], for_dt, force_tariff_migration=force_tariff_migration)

    def _transfer2main(self, order, force_tariff_migration=False):
        main_order = order.session.query(mapper.Order).getone(order.id)
        ua = a_ua.UnifiedAccount(
            order.session,
            main_order,
            None,
            force_tariff_migration=force_tariff_migration,
        )
        return ua.transfer2main(order.session.now())

    def test_transfers(self, session, client):
        main_order, child_orders = create_orders(session, client, DIRECT_PRODUCT_ID, [DIRECT_PRODUCT_ID] * 5)
        create_invoice(client, [(main_order, 10)])

        for order in child_orders:
            order.do_process_completion(1)

        self._do_ua(main_order)

        hamcrest.assert_that(
            main_order,
            hamcrest.has_properties(
                consume_qty=5,
                completion_qty=0
            )
        )
        hamcrest.assert_that(
            child_orders,
            hamcrest.only_contains(
                 hamcrest.has_properties(
                     consume_qty=1,
                     completion_qty=1
                 )
            )
        )

    def test_transfers_modify_migration(self, session, client):
        migrate_client(client, CONVERT_TYPE_MODIFY)
        main_order, child_orders = create_orders(
            session,
            client,
            DIRECT_PRODUCT_ID,
            [DIRECT_PRODUCT_RUB_ID] * 5
        )
        create_invoice(client, [(main_order, 10)])

        for order in child_orders:
            order.do_process_completion(30)

        self._do_ua(main_order)

        hamcrest.assert_that(
            main_order,
            hamcrest.has_properties(
                consume_qty=5,
                completion_qty=0,
            )
        )
        hamcrest.assert_that(
            child_orders,
            hamcrest.only_contains(
                 hamcrest.has_properties(
                     consume_qty=30,
                     completion_qty=30
                 )
            )
        )

    def test_optimized(self, session, client):
        main_order, child_orders = create_orders(session, client, DIRECT_PRODUCT_ID, [DIRECT_PRODUCT_ID] * 5)
        create_invoice(client, [(main_order, 10)])
        main_order.is_ua_optimize = True
        session.flush()

        for order in child_orders:
            order.do_process_completion(1, on_dt=datetime.datetime.now() - datetime.timedelta(6))
            order.do_process_completion(2, on_dt=datetime.datetime.now())
            order.update_ua_type()

        for_dt = datetime.datetime.now() - datetime.timedelta(1)
        self._do_ua(main_order, for_dt)

        hamcrest.assert_that(
            main_order,
            hamcrest.has_properties(
                consume_qty=10,
                completion_qty=5,
                shipment_dt=for_dt.replace(microsecond=0)
            )
        )
        hamcrest.assert_that(
            child_orders,
            hamcrest.only_contains(
                hamcrest.has_properties(
                    consume_qty=0,
                    completion_qty=2
                )
            )
        )

    def test_optimized_overconsumed(self, session, client):
        main_order, (child_order_fail, child_order) = create_orders(
            session,
            client,
            DIRECT_PRODUCT_ID,
            [DIRECT_PRODUCT_ID] * 2
        )
        main_order.is_ua_optimize = True
        session.flush()

        create_invoice(client, [(child_order_fail, 2)])
        create_invoice(client, [(child_order, 1)])

        child_order_fail.do_process_completion(1)
        child_order.do_process_completion(2)

        ua = a_ua.UnifiedAccountOptimized(session, main_order)
        ua.do()

        assert child_order_fail.consume_qty == 2
        assert child_order.consume_qty == 1
        hamcrest.assert_that(
            main_order,
            hamcrest.has_properties(
                consume_qty=0,
                completion_qty=1
            )
        )

    def test_optimized_modify_migration(self, session, client):
        migrate_client(client, CONVERT_TYPE_MODIFY)
        main_order, child_orders = create_orders(
            session,
            client,
            DIRECT_PRODUCT_ID,
            [DIRECT_PRODUCT_RUB_ID] * 5
        )
        create_invoice(client, [(main_order, 10)])
        main_order.is_ua_optimize = True
        session.flush()

        for order in child_orders:
            order.do_process_completion(30)

        self._do_ua(main_order)

        hamcrest.assert_that(
            main_order,
            hamcrest.has_properties(
                consume_qty=10,
                completion_qty=5,
            )
        )
        hamcrest.assert_that(
            child_orders,
            hamcrest.only_contains(
                 hamcrest.has_properties(
                     consume_qty=0,
                     completion_qty=30
                 )
            )
        )

    def test_optimized_copy_migration(self, session, client):
        main_order, child_orders = create_orders(
            session,
            client,
            DIRECT_PRODUCT_ID,
            [DIRECT_PRODUCT_ID] * 5
        )
        create_invoice(client, [(main_order, 10)])
        main_order.is_ua_optimize = True
        session.flush()

        for order in child_orders:
            order.do_process_completion(1)

        migrate_client(client, CONVERT_TYPE_COPY)

        self._do_ua(main_order)

        hamcrest.assert_that(
            main_order,
            hamcrest.has_properties(
                consume_qty=10,
                completion_qty=5,
            )
        )
        hamcrest.assert_that(
            child_orders,
            hamcrest.only_contains(
                 hamcrest.has_properties(
                     consume_qty=0,
                     completion_qty=1
                 )
            )
        )

    def test_optimized_modify_migration_completions(self, session, client):
        main_order, child_orders = create_orders(
            session,
            client,
            DIRECT_PRODUCT_ID,
            [DIRECT_PRODUCT_ID] * 2
        )
        create_invoice(client, [(main_order, 100)])
        main_order.is_ua_optimize = True
        session.flush()

        cur_dt = datetime.datetime.now()

        for order in child_orders:
            order.calculate_consumption(cur_dt - datetime.timedelta(2), {'Bucks': 5})

        # откручиваем в фишках
        self._do_ua(main_order, cur_dt - datetime.timedelta(2))
        hamcrest.assert_that(
            main_order,
            hamcrest.has_properties(
                completion_qty=10,
                completion_fixed_qty=0,
                shipment=hamcrest.has_properties(
                    consumption=10,
                    bucks=10,
                    money=hamcrest.is_(None)
                )
            )
        )

        # мигрируем на мультивалютность
        for order in child_orders:
            order.calculate_consumption(cur_dt - datetime.timedelta(1), {'Bucks': 6})

        migrate_client(client, CONVERT_TYPE_MODIFY)
        hamcrest.assert_that(
            main_order,
            hamcrest.has_properties(
                completion_qty=12,
                completion_fixed_qty=12,
                shipment=hamcrest.has_properties(
                    consumption=12,
                    bucks=12,
                    money=hamcrest.is_(None)
                )
            )
        )

        # откручиваем в деньгах
        for order in child_orders:
            order.calculate_consumption(cur_dt, {'Bucks': 6, 'Money': 60})

        self._do_ua(main_order)
        hamcrest.assert_that(
            main_order,
            hamcrest.has_properties(
                completion_qty=16,
                completion_fixed_qty=12,
                shipment=hamcrest.has_properties(
                    consumption=12,
                    bucks=12,
                    money=120
                )
            )
        )

    def test_optimized_modify_migration_wo_consumes(self, session, client):
        main_order, child_orders = create_orders(
            session,
            client,
            DIRECT_PRODUCT_ID,
            [DIRECT_PRODUCT_ID] * 2
        )
        main_order.is_ua_optimize = True
        session.flush()

        cur_dt = datetime.datetime.now()

        for order in child_orders:
            order.calculate_consumption(cur_dt - datetime.timedelta(2), {'Bucks': 5})

        # откручиваем в фишках
        migrate_client(client, CONVERT_TYPE_MODIFY)
        hamcrest.assert_that(
            main_order,
            hamcrest.has_properties(
                completion_qty=10,
                completion_fixed_qty=0,
                shipment=hamcrest.has_properties(
                    consumption=10,
                    bucks=10,
                    money=hamcrest.is_(None)
                )
            )
        )

        # откручиваем в деньгах
        for order in child_orders:
            order.calculate_consumption(cur_dt, {'Bucks': 6, 'Money': 60})

        self._do_ua(main_order)
        hamcrest.assert_that(
            main_order,
            hamcrest.has_properties(
                completion_qty=16,
                completion_fixed_qty=0,
                shipment=hamcrest.has_properties(
                    consumption=0,
                    bucks=0,
                    money=480
                )
            )
        )

        create_invoice(client, [(main_order, 100)])
        for order in child_orders:
            order.calculate_consumption(cur_dt, {'Bucks': 6, 'Money': 90})
        a_pc.ProcessCompletions(main_order).process_completions()

        self._do_ua(main_order)
        hamcrest.assert_that(
            main_order,
            hamcrest.has_properties(
                completion_qty=18,
                completion_fixed_qty=0,
                shipment=hamcrest.has_properties(
                    consumption=0,
                    bucks=0,
                    money=540
                )
            )
        )

    def test_optimized_transfer2main(self, session, client):
        main_order, child_orders = create_orders(
            session,
            client,
            DIRECT_PRODUCT_ID,
            [DIRECT_PRODUCT_ID] * 5
        )
        create_invoice(client, [(main_order, 10)])
        main_order.is_ua_optimize = True
        session.flush()

        create_invoice(client, [(child_orders[-1], 2)])

        for order in child_orders:
            order.do_process_completion(1)

        self._do_ua(main_order)

        hamcrest.assert_that(
            main_order,
            hamcrest.has_properties(
                consume_qty=11,
                completion_qty=4,
            )
        )
        hamcrest.assert_that(
            child_orders[:-1],
            hamcrest.only_contains(
                 hamcrest.has_properties(
                     consume_qty=0,
                     completion_qty=1
                 )
            )
        )
        hamcrest.assert_that(
            child_orders[-1],
            hamcrest.has_properties(
                consume_qty=1,
                completion_qty=1,
            )
        )

    def test_optimized_transfer2main_overacted(self, session, client):
        main_order, child_orders = create_orders(
            session,
            client,
            DIRECT_PRODUCT_ID,
            [DIRECT_PRODUCT_ID] * 5
        )
        main_order.is_ua_optimize = True
        session.flush()

        cons_qtys = [2, 2, 2, 2, 2]
        comp_qtys = [1, 1, 1, 6, 6]

        invoice = create_invoice(
            client,
            zip(child_orders, cons_qtys),
            paysys_id=1003,
            person_type='ur'
        )

        child_orders[0].do_process_completion(2)
        invoice.generate_act(force=1)

        for order, qty in zip(child_orders, comp_qtys):
            order.do_process_completion(qty)

        self._do_ua(main_order)

        hamcrest.assert_that(
            main_order,
            hamcrest.has_properties(
                consume_qty=2,
                completion_qty=8,
            )
        )
        hamcrest.assert_that(
            child_orders,
            hamcrest.contains(
                 hamcrest.has_properties(consume_qty=2, completion_qty=1),
                 hamcrest.has_properties(consume_qty=1, completion_qty=1),
                 hamcrest.has_properties(consume_qty=1, completion_qty=1),
                 hamcrest.has_properties(consume_qty=2, completion_qty=6),
                 hamcrest.has_properties(consume_qty=2, completion_qty=6),
            )
        )
        assert sum(co.act_qty for co in child_orders[0].consumes) == 2

    def test_mixed(self, session, client, media_rub_product):
        main_order, (direct_order, media_order) = create_orders(
            session,
            client,
            DIRECT_PRODUCT_RUB_ID,
            [DIRECT_PRODUCT_RUB_ID, media_rub_product.id]
        )
        main_order.is_ua_optimize = True
        session.flush()

        create_invoice(client, [(main_order, 100)])

        direct_order.do_process_completion(60)
        media_order.do_process_completion(35)

        self._do_ua(main_order)

        assert direct_order.consume_qty == 0
        assert media_order.consume_qty == 35
        hamcrest.assert_that(
            main_order,
            hamcrest.has_properties(
                consume_qty=65,
                completion_qty=60
            )
        )

    def test_mixed_modify_migration(self, session, client, media_rub_product):
        main_order, (direct_order, media_order) = create_orders(
            session,
            client,
            DIRECT_PRODUCT_ID,
            [DIRECT_PRODUCT_RUB_ID, media_rub_product.id]
        )
        main_order.is_ua_optimize = True
        session.flush()

        migrate_client(client, CONVERT_TYPE_MODIFY)

        create_invoice(client, [(main_order, 5)])

        direct_order.do_process_completion(90)
        media_order.do_process_completion(45)

        self._do_ua(main_order)

        assert direct_order.consume_qty == 0
        assert media_order.consume_qty == 45
        hamcrest.assert_that(
            main_order,
            hamcrest.has_properties(
                consume_qty=D('3.5'),
                completion_qty=3
            )
        )

    def test_mixed_copy_migration(self, session, client, media_rub_product):
        main_rub, child_orders = create_orders(
            session,
            client,
            DIRECT_PRODUCT_RUB_ID,
            [DIRECT_PRODUCT_ID] * 3 + [DIRECT_PRODUCT_RUB_ID] * 2 + [media_rub_product.id]
        )

        migrate_client(client, CONVERT_TYPE_COPY)

        main_fish, child_fish, child_fish_free = child_orders[:3]
        child_rub, child_rub_free = child_orders[3:5]
        child_media = child_orders[-1]

        child_fish.group_order_id = child_rub.id
        child_fish_free.group_order_id = child_rub_free.id

        main_rub.is_ua_optimize = True
        main_fish.is_ua_optimize = True  # not sure about that
        session.flush()

        create_invoice(client, [(main_rub, 6000)])

        create_invoice(client, [(main_fish, 2)])
        main_fish.do_process_completion(D('1.5'))

        child_rub.do_process_completion(50)

        child_fish.do_process_completion(2)

        create_invoice(client, [(child_rub_free, 200)])
        child_rub_free.do_process_completion(113)

        create_invoice(client, [(child_fish_free, 2)])
        child_fish_free.do_process_completion(1)

        child_media.do_process_completion(66)

        self._do_ua(main_rub)

        hamcrest.assert_that(
            main_rub,
            hamcrest.has_properties(
                consume_qty=6066,
                completion_qty=50
            )
        )
        hamcrest.assert_that(
            main_fish,
            hamcrest.has_properties(
                consume_qty=D('1.5'),
                completion_qty=D('1.5'),
                child_ua_type=UAChildType.OPTIMIZED
            )
        )
        hamcrest.assert_that(
            child_rub,
            hamcrest.has_properties(
                consume_qty=0,
                completion_qty=50,
                child_ua_type=UAChildType.OPTIMIZED
            )
        )
        hamcrest.assert_that(
            child_fish,
            hamcrest.has_properties(
                consume_qty=0,
                completion_qty=2,
                child_ua_type=UAChildType.OPTIMIZED
            )
        )
        hamcrest.assert_that(
            child_rub_free,
            hamcrest.has_properties(
                consume_qty=113,
                completion_qty=113,
                child_ua_type=UAChildType.OPTIMIZED
            )
        )
        hamcrest.assert_that(
            child_fish_free,
            hamcrest.has_properties(
                consume_qty=1,
                completion_qty=1,
                child_ua_type=UAChildType.OPTIMIZED
            )
        )
        hamcrest.assert_that(
            child_media,
            hamcrest.has_properties(
                consume_qty=66,
                completion_qty=66,
                child_ua_type=UAChildType.TRANSFERS
            )
        )

    def test_mixed_copy_migration_nonrur(self, session, client, media_usd_product):
        main_cur, child_orders = create_orders(
            session,
            client,
            DIRECT_PRODUCT_USD_ID,
            [DIRECT_PRODUCT_ID] * 2 + [DIRECT_PRODUCT_USD_ID] + [media_usd_product.id]
        )

        migrate_client(client, CONVERT_TYPE_COPY)

        main_fish, child_fish = child_orders[:2]
        child_cur = child_orders[2]
        child_media = child_orders[3]

        child_fish.group_order_id = child_cur.id

        main_cur.is_ua_optimize = 1
        main_fish.is_ua_optimize = 1
        session.flush()

        create_invoice(client, [(main_cur, 6000)])

        main_fish.do_process_completion(3)
        child_cur.do_process_completion(666)
        child_fish.do_process_completion(2)
        child_media.do_process_completion(666)

        self._do_ua(main_cur)

        hamcrest.assert_that(
            main_cur,
            hamcrest.has_properties(
                consume_qty=5334,
                completion_qty=666
            )
        )
        hamcrest.assert_that(
            main_fish,
            hamcrest.has_properties(
                consume_qty=0,
                completion_qty=3,
                child_ua_type=UAChildType.TRANSFERS
            )
        )
        hamcrest.assert_that(
            child_cur,
            hamcrest.has_properties(
                consume_qty=0,
                completion_qty=666,
                child_ua_type=UAChildType.OPTIMIZED
            )
        )
        hamcrest.assert_that(
            child_fish,
            hamcrest.has_properties(
                consume_qty=0,
                completion_qty=2,
                child_ua_type=UAChildType.TRANSFERS
            )
        )
        hamcrest.assert_that(
            child_media,
            hamcrest.has_properties(
                consume_qty=666,
                completion_qty=666,
                child_ua_type=UAChildType.TRANSFERS
            )
        )

    @pytest.mark.log_tariff
    @pytest.mark.parametrize(
        'log_tariff_state',
        [
            pytest.param(OrderLogTariffState.INIT, id='init'),
            pytest.param(OrderLogTariffState.MIGRATED, id='migrated'),
        ]
    )
    def test_log_tariff(self, session, client, media_rub_product, log_tariff_state):
        main_order, child_orders = create_orders(
            session,
            client,
            DIRECT_PRODUCT_ID,
            [DIRECT_PRODUCT_ID, DIRECT_PRODUCT_RUB_ID, media_rub_product.id]
        )
        main_order.is_ua_optimize = 1
        main_order._is_log_tariff = OrderLogTariffState.MIGRATED
        migrate_client(client, CONVERT_TYPE_MODIFY)

        create_invoice(client, [(main_order, 100)])
        for order, consume_qty, completion_qty in zip(child_orders, [30, 20, 10], [20, 30, 40]):
            create_invoice(client, [(order, consume_qty)])
            order.do_process_completion(completion_qty)

        self._do_ua(main_order)

        hamcrest.assert_that(
            main_order,
            hamcrest.has_properties(
                consume_qty=110,
                completion_qty=0
            )
        )
        hamcrest.assert_that(
            child_orders,
            hamcrest.contains(
                hamcrest.has_properties(consume_qty=20, completion_qty=20),
                hamcrest.has_properties(consume_qty=20, completion_qty=30),
                hamcrest.has_properties(consume_qty=10, completion_qty=40),
            )
        )

    def test_cross_currency_prices(self, session, client):
        session.config.__dict__['USE_UA_MODE_TRANSFERS'] = True

        main_order, (child_order1, child_order2) = create_orders(
            session,
            client,
            DIRECT_PRODUCT_USD_ID,
            [DIRECT_PRODUCT_USD_ID] * 2
        )
        cur_dt = datetime.datetime.now()
        old_dt = cur_dt - datetime.timedelta(30)

        dt_rates = [
            {
                'RUR': DU(1),
                'RUB': DU(1),
                'USD': DU('30', 'RUB', 'USD'),
            },
            (
                ut.trunc_date(datetime.datetime.now()),
                {
                    'RUR': DU(1),
                    'RUB': DU(1),
                    'USD': DU('60', 'RUB', 'USD'),
                }
            )
        ]
        with ob.patched_currency(dt_rates):
            create_invoice(client, [(main_order, 2)], old_dt, PAYSYS_BANK_RUB_NONRES)
            create_invoice(client, [(main_order, 2)], cur_dt, PAYSYS_BANK_USD_NONRES)
            create_invoice(client, [(main_order, 10)], cur_dt, PAYSYS_BANK_RUB_NONRES)

            child_order1.calculate_consumption(cur_dt, {child_order1.shipment_type: 3})
            child_order2.calculate_consumption(cur_dt, {child_order2.shipment_type: 4})

            self._do_ua(main_order)

        assert main_order.consume_qty == 7
        assert child_order1.consume_qty == 3
        assert child_order2.consume_qty == 4

    @pytest.mark.parametrize(
        'is_log_tariff, act_qty, force_tariff_migration',
        [
            pytest.param(OrderLogTariffState.OFF, 7, False, id='off'),
            pytest.param(OrderLogTariffState.INIT, 7, True, id='init + force + free'),
            pytest.param(OrderLogTariffState.MIGRATED, 7, False, id='migrated + free'),
            pytest.param(OrderLogTariffState.MIGRATED, 10, False, id='migrated + equal'),
        ]
    )
    def test_log_tariff_transfer2main_overacted(self, session, client, is_log_tariff, act_qty, force_tariff_migration):
        main_order, (child_order,) = create_orders(session, client, DIRECT_PRODUCT_ID, [DIRECT_PRODUCT_ID])
        main_order.is_ua_optimize = True
        session.flush()

        invoice = create_invoice(
            client,
            [(child_order, 10)],
            paysys_id=1003,
            person_type='ur'
        )

        child_order.do_process_completion(act_qty)
        invoice.generate_act(force=1)

        child_order.do_process_completion(4)

        main_order._is_log_tariff = is_log_tariff
        session.flush()

        self._do_ua(main_order, force_tariff_migration=force_tariff_migration)

        hamcrest.assert_that(
            main_order,
            hamcrest.has_properties(
                consume_qty=3 if is_log_tariff == OrderLogTariffState.OFF else 6,
                completion_qty=0,
            )
        )
        hamcrest.assert_that(
            child_order,
            hamcrest.has_properties(
                consume_qty=7 if is_log_tariff == OrderLogTariffState.OFF else 4,
                completion_qty=4
            ),
        )

    @pytest.mark.parametrize(
        'is_log_tariff, act_qty, main_completion_qty, force_tariff_migration',
        [
            pytest.param(OrderLogTariffState.OFF, 7, 1, False, id='off'),
            pytest.param(OrderLogTariffState.INIT, 7, 1, True, id='init + force + free'),
            pytest.param(OrderLogTariffState.MIGRATED, 7, 1, False, id='migrated + free + overcompletion_on_main'),
            pytest.param(OrderLogTariffState.MIGRATED, 7, 1, True, id='migrated + free + overcompletion_on_main + force'),
            pytest.param(OrderLogTariffState.MIGRATED, 7, 0, False, id='migrated + free'),
            pytest.param(OrderLogTariffState.MIGRATED, 10, 0, False, id='migrated + equal'),
        ]
    )
    def test_log_tariff_transfer2main_only__overacted(self, session, client, is_log_tariff, act_qty,
                                                          force_tariff_migration,
                                                          main_completion_qty):
        main_order, (child_order,) = create_orders(session, client, DIRECT_PRODUCT_ID, [DIRECT_PRODUCT_ID])
        main_order.is_ua_optimize = True

        session.flush()

        child_current_qty = 10

        invoice = create_invoice(
            client,
            [(child_order, child_current_qty)],
            paysys_id=1003,
            person_type='ur'
        )


        child_order.do_process_completion(act_qty)
        invoice.generate_act(force=1)

        child_completion_qty = 4
        child_order.do_process_completion(child_completion_qty)

        main_order.do_process_completion(main_completion_qty)

        main_order._is_log_tariff = is_log_tariff
        session.flush()

        self._transfer2main(main_order, force_tariff_migration=force_tariff_migration)

        hamcrest.assert_that(
            main_order,
            hamcrest.has_properties(
                consume_qty=child_current_qty-act_qty if is_log_tariff == OrderLogTariffState.OFF else child_current_qty-child_completion_qty,
                completion_qty=main_completion_qty,
            )
        )
        hamcrest.assert_that(
            child_order,
            hamcrest.has_properties(
                consume_qty=act_qty if is_log_tariff == OrderLogTariffState.OFF else child_completion_qty,
                completion_qty=child_completion_qty
            ),
        )

        assert len(main_order.consumes) == 1
        if is_log_tariff == OrderLogTariffState.MIGRATED and not force_tariff_migration:
            assert main_order.consumes[-1].completion_qty == 0
        else:
            assert main_order.consumes[-1].completion_qty == main_completion_qty


    def test_optimized_no_children(self, session, client):
        main_order, (child_order,) = create_orders(
            session,
            client,
            DIRECT_PRODUCT_RUB_ID,
            [DIRECT_MEDIA_PRODUCT_RUB_ID]
        )
        create_invoice(client, [(main_order, 10)])
        main_order.is_ua_optimize = True
        session.flush()
        main_order.do_process_completion(5)

        child_order.do_process_completion(2)
        child_order.update_ua_type()

        self._do_ua(main_order)

        hamcrest.assert_that(
            main_order,
            hamcrest.has_properties(
                consume_qty=8,
                completion_qty=0,
            )
        )
        hamcrest.assert_that(
            child_order,
            hamcrest.has_properties(
                consume_qty=2,
                completion_qty=2
            )
        )



class TestUnifiedAccountParallelOrders(object):
    @pytest.fixture(autouse=True)
    def mock_config(self, session, client):
        session.config.set('UA_PROCESS_BY_ORDERS', {'Client_ids': [client.id]}, 'value_json')

        # одна сессия, конфиг кешируется и не обновляется с новым клиентом для каждого теста
        session.config._PartialConfigurators.clear()
        session.config._cache.clear()

        session.flush()

    def _create_objects(self, client, orders_count=3, qty=1):
        session = client.session
        orders = [
            ob.OrderBuilder(
                service=ob.Getter(mapper.Service, 7),
                client=client,
                product_id=DIRECT_PRODUCT_ID,
            ).build(session).obj
            for _ in xrange(0, orders_count)
        ]

        children = [
            ob.OrderBuilder(
                service=ob.Getter(mapper.Service, 7),
                client=client,
                product_id=DIRECT_PRODUCT_ID,
                group_order_id=go.id
            ).build(session).obj
            for go in orders
        ]

        invoice = ob.InvoiceBuilder(
            request=ob.RequestBuilder(
                basket=ob.BasketBuilder(
                    rows=[ob.BasketItemBuilder(order=o, quantity=qty) for o in orders])
            )
        ).build(session).obj
        InvoiceTurnOn(invoice, manual=True).do()

        for o in children:
            o.do_process_completion(qty)

        session.flush()
        session.expire_all()

        return orders

    def _enqueue(self, client):
        input_ = {
            'for_dt': datetime.datetime.now(),
            'use_completion_history': True
        }
        client.enqueue('UA_TRANSFER', input_=input_)
        client.session.flush()
        client.session.expire_all()
        return datetime.datetime.now().replace(microsecond=0)

    def _process(self, client):
        QueueProcessor('UA_TRANSFER').process_one(client.exports['UA_TRANSFER'])
        client.session.flush()
        client.session.expire_all()

        return datetime.datetime.now().replace(microsecond=0)

    def test_enqueue(self, session, client):
        qty = 1
        orders_count = 3
        orders = self._create_objects(client, orders_count, qty)

        enq_dt = self._enqueue(client)
        proc_dt = self._process(client)

        cl_export = client.exports['UA_TRANSFER']
        assert cl_export.state == 0
        assert cl_export.rate == 0
        assert cl_export.next_export >= enq_dt.replace(microsecond=0) + datetime.timedelta(minutes=5)
        assert cl_export.input['orders_enqueue_start_dt'] >= enq_dt
        assert cl_export.input['orders_enqueue_start_dt'] <= proc_dt
        assert cl_export.input['orders_enqueue_end_dt'] >= enq_dt
        assert cl_export.input['orders_enqueue_end_dt'] <= proc_dt + datetime.timedelta(seconds=1)
        assert cl_export.input['orders_enqueue_start_dt'] <= cl_export.input['orders_enqueue_end_dt']

        for o in orders:
            o_export = o.exports['UA_TRANSFER']
            assert o.consume_qty == qty
            assert o_export.state == 0
            assert o_export.rate == 0
            assert o_export.input['for_dt'] == cl_export.input['for_dt']
            assert o_export.input['use_completion_history'] == cl_export.input['use_completion_history']

    def test_wait(self, session, client):
        qty = 1
        orders_count = 3
        orders = self._create_objects(client, orders_count, qty)

        enq_dt = self._enqueue(client)
        base_proc_dt = self._process(client)
        cl_export = client.exports['UA_TRANSFER']
        enq_start_dt = cl_export.input['orders_enqueue_start_dt']
        enq_end_dt = cl_export.input['orders_enqueue_end_dt']

        # один отработал, остальные висят - ждём
        orders[0].exports['UA_TRANSFER'].state = 1
        session.flush()
        proc_dt = self._process(client)
        assert cl_export.input['orders_enqueue_start_dt'] == enq_start_dt
        assert cl_export.input['orders_enqueue_end_dt'] == enq_end_dt
        assert cl_export.state == 0
        assert cl_export.rate == 0
        assert cl_export.next_export > proc_dt

        orders[1].exports['UA_TRANSFER'].state = 1

        # state=0, был выгружен, но до постановки - ждём
        orders[2].exports['UA_TRANSFER'].export_dt = enq_dt - datetime.timedelta(seconds=1)
        session.flush()
        proc_dt = self._process(client)
        assert cl_export.input['orders_enqueue_start_dt'] == enq_start_dt
        assert cl_export.input['orders_enqueue_end_dt'] == enq_end_dt
        assert cl_export.state == 0
        assert cl_export.rate == 0
        assert cl_export.next_export > proc_dt

        # state=0, но выгружен уже после постановки - готово!
        orders[2].exports['UA_TRANSFER'].export_dt = base_proc_dt + datetime.timedelta(seconds=1)
        session.flush()
        self._process(client)
        assert cl_export.state == 1

    def test_created_orders(self, session, client):
        qty = 1
        orders_count = 2
        order_before, order_after = self._create_objects(client, orders_count, qty)

        base_enqueue_batch = a_ua.mapper.Order.enqueue_batch

        def sleepy_enqueue_batch(cls, *args, **kwargs):
            time.sleep(5)
            base_enqueue_batch(*args, **kwargs)

        a_ua.mapper.Order.enqueue_batch = functools.partial(sleepy_enqueue_batch, mapper.Order)

        self._enqueue(client)
        self._process(client)
        cl_export = client.exports['UA_TRANSFER']
        enq_start_dt = cl_export.input['orders_enqueue_start_dt']
        enq_end_dt = cl_export.input['orders_enqueue_end_dt']

        # У одного дата создания до постановки, у другого после
        assert enq_start_dt < enq_end_dt - datetime.timedelta(seconds=1)
        order_before.dt = enq_start_dt - datetime.timedelta(seconds=1)
        order_after.dt = enq_start_dt + datetime.timedelta(seconds=1)
        session.flush()

        # оба не разобраны - ждём
        proc_dt = self._process(client)
        assert cl_export.state == 0
        assert cl_export.rate == 0
        assert cl_export.next_export > proc_dt

        # не разобран созданный после начала постановки - готово
        order_before.exports['UA_TRANSFER'].state = 1
        session.flush()
        self._process(client)
        assert cl_export.state == 1


@pytest.mark.taxes_update
class TestTaxesUpdate(object):
    @pytest.fixture(autouse=True)
    def ua_mode_config(self, session):
        session.config.__dict__['USE_UA_MODE_TRANSFERS'] = True

    @staticmethod
    def create_fish_product(session):
        past = datetime.datetime(2000, 1, 1)
        present = ut.trunc_date(datetime.datetime.now())

        tax_policy = ob.TaxPolicyBuilder(
            tax_pcts=[
                (past, 18),
                (present, 20)
            ]
        ).build(session).obj
        tpp1, tpp2 = tax_policy.taxes

        return ob.ProductBuilder(
            taxes=tax_policy,
            prices=[
                (past, 'RUR', 30, tpp1),
                (present, 'RUR', 30, tpp2),
            ]
        ).build(session).obj

    @staticmethod
    def do_ua(order, on_dt=None):
        a_ua.handle_orders(order.session, [order], on_dt or datetime.datetime.now() - datetime.timedelta(1))

    @pytest.fixture
    def product(self, session):
        return self.create_fish_product(session)

    def test_transfers_on_change_dt(self, session, client, product):
        tpp1, tpp2 = product.taxes[0].policy.taxes

        main_order, child_orders = create_orders(session, client, product.id, [product.id] * 3)
        i = create_invoice(client, [(main_order, 100)], datetime.datetime.now() - datetime.timedelta(10))

        for order in child_orders:
            order.calculate_consumption(datetime.datetime.now() - datetime.timedelta(1), {order.shipment_type: 6})
            order.calculate_consumption(datetime.datetime.now(), {order.shipment_type: 10})

        self.do_ua(main_order)

        assert main_order.consume_qty == 82
        hamcrest.assert_that(
            main_order.consumes,
            consumes_match(
                [
                    (i.id, 82, 82 * 30, 0, 0, 0, 0, 30, tpp1.id),
                ],
                extra_params=['price', 'tax_policy_pct_id']
            )
        )
        hamcrest.assert_that(
            child_orders,
            hamcrest.only_contains(hamcrest.has_properties(
                consume_qty=6,
                completion_qty=10,
                consumes=consumes_match(
                    [
                        (i.id, 6, 6 * 30, 6, 6 * 30, 0, 0, 30, tpp1.id),
                    ],
                    extra_params=['price', 'tax_policy_pct_id']
                )
            ))
        )

    def test_transfers_after_change_dt(self, session, client, product):
        tpp1, tpp2 = product.taxes[0].policy.taxes

        main_order, child_orders = create_orders(session, client, product.id, [product.id] * 3)
        i = create_invoice(
            client,
            [(main_order, 100)] + [(co, 6) for co in child_orders],
            datetime.datetime.now() - datetime.timedelta(10)
        )

        for order in child_orders:
            order.calculate_consumption(datetime.datetime.now() - datetime.timedelta(1), {order.shipment_type: 6})
            order.calculate_consumption(datetime.datetime.now(), {order.shipment_type: 10})

        self.do_ua(main_order, datetime.datetime.now())

        assert main_order.consume_qty == 88
        hamcrest.assert_that(
            main_order.consumes,
            consumes_match(
                [
                    (i.id, 88, 88 * 30, 0, 0, 0, 0, 30, tpp1.id),
                ],
                extra_params=['price', 'tax_policy_pct_id']
            )
        )
        hamcrest.assert_that(
            child_orders,
            hamcrest.only_contains(hamcrest.has_properties(
                consume_qty=10,
                completion_qty=10,
                consumes=consumes_match(
                    [
                        (i.id, 6, 6 * 30, 6, 6 * 30, 0, 0, 30, tpp1.id),
                        (i.id, 4, 4 * 30, 4, 4 * 30, 0, 0, 30, tpp2.id),
                    ],
                    extra_params=['price', 'tax_policy_pct_id']
                )
            ))
        )

    def test_transfers_mixed_tax(self, session, client, product):
        main_order, child_orders = create_orders(session, client, product.id, [product.id] * 3)
        create_invoice(client, [(main_order, 13)], datetime.datetime.now() - datetime.timedelta(10))
        create_invoice(client, [(main_order, 17)])

        for order in child_orders:
            order.calculate_consumption(datetime.datetime.now(), {order.shipment_type: 10})

        self.do_ua(main_order, datetime.datetime.now())

        assert main_order.consume_qty == 0
        hamcrest.assert_that(
            child_orders,
            hamcrest.only_contains(hamcrest.has_properties(
                consume_qty=10,
                completion_qty=10
            ))
        )

    def test_optimized_on_change_dt(self, session, client, product):
        tpp1, tpp2 = product.taxes[0].policy.taxes

        main_order, child_orders = create_orders(session, client, product.id, [product.id] * 3)
        main_order.is_ua_optimize = 1
        i = create_invoice(client, [(main_order, 100)], datetime.datetime.now() - datetime.timedelta(10))

        for order in child_orders:
            order.calculate_consumption(datetime.datetime.now() - datetime.timedelta(1), {order.shipment_type: 6})
            order.calculate_consumption(datetime.datetime.now(), {order.shipment_type: 10})

        self.do_ua(main_order)

        assert main_order.consume_qty == 100
        hamcrest.assert_that(
            main_order.consumes,
            consumes_match(
                [
                    (i.id, 100, 100 * 30, 18, 18 * 30, 0, 0, 30, tpp1.id),
                ],
                extra_params=['price', 'tax_policy_pct_id']
            )
        )
        hamcrest.assert_that(
            child_orders,
            hamcrest.only_contains(hamcrest.has_properties(
                consume_qty=0,
                completion_qty=10,
                consumes=[],
            ))
        )

    def test_optimized_after_change_dt(self, session, client, product):
        tpp1, tpp2 = product.taxes[0].policy.taxes

        main_order, child_orders = create_orders(session, client, product.id, [product.id] * 3)
        main_order.is_ua_optimize = 1
        i = create_invoice(client, [(main_order, 100)], datetime.datetime.now() - datetime.timedelta(10))

        prev_dt = datetime.datetime.now() - datetime.timedelta(1)
        main_order.calculate_consumption(prev_dt, {main_order.shipment_type: 10})
        for order in child_orders:
            order.calculate_consumption(prev_dt, {order.shipment_type: 6})

        self.do_ua(main_order, datetime.datetime.now())

        assert main_order.consume_qty == 100
        hamcrest.assert_that(
            main_order.consumes,
            consumes_match(
                [
                    (i.id, 10, 10 * 30, 10, 10 * 30, 0, 0, 30, tpp1.id),
                    (i.id, 90, 90 * 30, 8, 8 * 30, 0, 0, 30, tpp2.id),
                ],
                extra_params=['price', 'tax_policy_pct_id']
            )
        )
        hamcrest.assert_that(
            child_orders,
            hamcrest.only_contains(hamcrest.has_properties(
                consume_qty=0,
                completion_qty=6,
                consumes=[],
            ))
        )

    def test_transfer2main(self, session, client, product):
        tpp1, tpp2 = product.taxes[0].policy.taxes

        main_order, child_orders = create_orders(session, client, product.id, [product.id] * 3)
        i = create_invoice(
            client,
            [(main_order, 100)] + [(co, 6) for co in child_orders],
            datetime.datetime.now() - datetime.timedelta(10)
        )

        for order in child_orders:
            order.calculate_consumption(datetime.datetime.now() - datetime.timedelta(1), {order.shipment_type: 5})

        self.do_ua(main_order)

        assert main_order.consume_qty == 103
        hamcrest.assert_that(
            main_order.consumes,
            consumes_match(
                [
                    (i.id, 103, 103 * 30, 0, 0, 0, 0, 30, tpp1.id),
                ],
                extra_params=['price', 'tax_policy_pct_id']
            )
        )
        hamcrest.assert_that(
            child_orders,
            hamcrest.only_contains(hamcrest.has_properties(
                consume_qty=5,
                completion_qty=5,
                consumes=consumes_match(
                    [
                        (i.id, 5, 5 * 30, 5, 5 * 30, 0, 0, 30, tpp1.id),
                    ],
                    extra_params=['price', 'tax_policy_pct_id']
                )
            ))
        )

    def test_optimized_overshipment_after_change_dt(self, session, client, product):
        tpp1, tpp2 = product.taxes[0].policy.taxes

        main_order, child_orders = create_orders(session, client, product.id, [product.id] * 3)
        main_order.is_ua_optimize = 1
        i1 = create_invoice(client, [(main_order, 25)], datetime.datetime.now() - datetime.timedelta(10))

        prev_dt = datetime.datetime.now() - datetime.timedelta(1)
        main_order.calculate_consumption(prev_dt, {main_order.shipment_type: 30})
        for order in child_orders:
            order.calculate_consumption(prev_dt, {order.shipment_type: 10})

        i2 = create_invoice(client, [(main_order, 100)])

        self.do_ua(main_order, datetime.datetime.now())

        assert main_order.consume_qty == 125
        hamcrest.assert_that(
            main_order.consumes,
            consumes_match(
                [
                    (i1.id, 25, 25 * 30, 25, 25 * 30, 0, 0, 30, tpp1.id),
                    (i2.id, 100, 100 * 30, 5, 5 * 30, 0, 0, 30, tpp2.id),
                ],
                extra_params=['price', 'tax_policy_pct_id']
            )
        )
        hamcrest.assert_that(
            child_orders,
            hamcrest.only_contains(hamcrest.has_properties(
                consume_qty=0,
                completion_qty=10,
                consumes=[],
            ))
        )
