# -*- coding: utf-8 -*-

import decimal
import datetime
import hashlib
import logging

import hamcrest
import mock

from balance import mapper
from balance import muzzle_util as ut
from balance.actions.process_completions import ProcessCompletions
from balance.actions.process_completions.shipment import FAIR_PROC_SQL
from cluster_tools.process_completions import process_pl_part
from balance import discounts
from balance import constants as cst

from tests import object_builder as ob
from tests.base_routine import consumes_match

D = decimal.Decimal
NOW = datetime.datetime.now().replace(microsecond=0)


def consume_order(order, consumes_qtys, discount_pct=0, dt=None):
    session = order.session
    client = order.client
    if client.persons:
        person = client.persons[0]
    else:
        person = ob.PersonBuilder(client=client).build(session).obj
    paysys = ob.Getter(mapper.Paysys, 1000).build(session).obj

    def mock_discounts(ns):
        return discounts.DiscountProof('mock', discount=discount_pct, adjust_quantity=False), \
               None, None

    def mock_update_taxes(self, qty, sum_):
        return self._unchanged(qty, sum_)

    patch_tax = mock.patch('balance.actions.taxes_update.TaxUpdater.calculate_updated_parameters', mock_update_taxes)
    patch_discounts = mock.patch('balance.discounts.calc_from_ns', mock_discounts)

    with patch_tax, patch_discounts:
        for qty in consumes_qtys:
            invoice = ob.InvoiceBuilder(
                person=person,
                paysys=paysys,
                request=ob.RequestBuilder(
                    basket=ob.BasketBuilder(
                        client=client,
                        dt=dt,
                        rows=[ob.BasketItemBuilder(order=order, quantity=qty)]
                    )
                )
            ).build(session).obj
            invoice.create_receipt(invoice.effective_sum)
            invoice.turn_on_rows(cut_agava=True)


def create_order(session, product_id=cst.DIRECT_PRODUCT_ID, consumes_qtys=None, discount_pct=0, dt=None):
    client = ob.ClientBuilder().build(session).obj
    order = ob.OrderBuilder(
        client=client,
        product=ob.Getter(mapper.Product, product_id),
    ).build(session).obj

    consumes_qtys = consumes_qtys if consumes_qtys is not None else [666]
    consume_order(order, consumes_qtys, discount_pct, dt)
    return order


def process_completions(order, shipment_info, stop=0, dt=None):
    if shipment_info:
        order.shipment.update(dt or NOW, shipment_info, stop=stop)
    ProcessCompletions(order).process_completions()
    order.session.flush()


def process_completions_plsql(order, shipment_info, stop=0, dt=None):
    session = order.session
    if shipment_info:
        order.shipment.update(dt or NOW, shipment_info, stop=stop)
    session.execute(FAIR_PROC_SQL, {
        'service_id': order.service_id,
        'service_order_id': order.service_order_id,
        'skip_deny_shipment': 0
    })
    session.expire_all()


def process_completions_plsql_batch(order, shipment_info, stop=0, dt=None):
    session = order.session
    if shipment_info:
        order.shipment.update(dt or NOW, shipment_info, stop=stop)
    process_pl_part(
        [{'service_id': order.service_id, 'service_order_id': order.service_order_id}],
        is_test=True
    )
    session.expire_all()


def migrate_client(client, currency='RUB', convert_type=cst.CONVERT_TYPE_MODIFY):
    # мультивалютность
    client.set_currency(
        cst.ServiceId.DIRECT, currency,
        ut.trunc_date(NOW),
        convert_type,
        force=1
    )
    client.session.flush()


def calc_log_tariff_state(first_id, cons_data):
    hash_ = hashlib.sha256()
    for consume, qty in cons_data:
        hash_val = '%s_%s|' % (consume.id, qty)
        hash_.update(hash_val)
    return '%s:%s' % (first_id, hash_.hexdigest())


def create_order_task(order, qty, tariff_dt=None, state=cst.SENTINEL, currency_id=cst.ISO_NUM_CODE_RUB, task=None,
                      group_order=None, group_dt=None, skip_update_is_log_tariff=False):
    if not (skip_update_is_log_tariff or order._is_log_tariff):
        order._is_log_tariff = cst.OrderLogTariffState.MIGRATED
    max_cons_id = order.consumes[-1].id + 1 if order.consumes else 0
    return ob.LogTariffOrderBuilder.construct(
        order.session,
        order=order,
        task=task,
        completion_qty_delta=qty,
        group_service_order_id=group_order.service_order_id if group_order else None,
        tariff_dt=tariff_dt or datetime.datetime.now(),
        state=calc_log_tariff_state(max_cons_id, []) if state is cst.SENTINEL else state,
        currency_id=currency_id,
        group_dt=group_dt
    )


def assert_task_consumes(task, task_states):
    matchers = [
        hamcrest.has_properties(
            consume_id=co_id,
            qty=qty,
            sum=sum_,
            consume_qty=k_qty,
            consume_sum=k_sum
        )
        for co_id, qty, sum_, k_qty, k_sum in task_states
    ]
    if matchers:
        match = hamcrest.contains_inanyorder(*matchers)
    else:
        match = hamcrest.empty()
    hamcrest.assert_that(
        task.consumes,
        match
    )


def create_orders(client,
                  main_product_id=cst.DIRECT_PRODUCT_RUB_ID,
                  children_product_ids=None,
                  turn_on_log_tariff=True,
                  is_ua_optimize=1,
                  force_log_tariff=False):

    children_product_ids = children_product_ids or [cst.DIRECT_PRODUCT_RUB_ID]
    main = ob.OrderBuilder.construct(
        client.session,
        client=client,
        product_id=main_product_id,
        is_ua_optimize=is_ua_optimize
    )
    children = [
        ob.OrderBuilder.construct(client.session, client=client, product_id=product_id, parent_group_order=main)
        for product_id in children_product_ids
    ]
    main.main_order = 1
    if force_log_tariff:
        main.force_log_tariff()
    elif turn_on_log_tariff:
        main.turn_on_log_tariff()
    client.session.flush()
    return main, children


def mk_shipment(order, qty, dt=None, money=None, force=True):
    shipment_info = {
        order.shipment_type: qty,
    }
    if order.shipment_type != 'Money':
        shipment_info['Money'] = money

    order.shipment.update(dt or datetime.datetime.now(), shipment_info)
    prev_deny_shipment = order.shipment.deny_shipment
    order.shipment.deny_shipment = None
    ProcessCompletions(order, force_log_tariff_processing=force).process_completions(skip_deny_shipment=True)
    order.shipment.deny_shipment = prev_deny_shipment
    order.session.flush()


def assert_consumes(order, consumes_states):
    params = [
        'current_qty',
        'current_sum',
        'completion_qty',
        'completion_sum',
    ]
    hamcrest.assert_that(
        order.consumes,
        consumes_match(consumes_states, forced_params=params)
    )


def task_assert_from_params(consumes, params, qty_coeff=1):
    consume_rows = ((consumes[i] if i is not None else None, q, s) for i, q, s in params)

    return [
        (co and co.id, q, s, co and co.consume_qty * qty_coeff, co and co.consume_sum)
        for co, q, s in consume_rows
    ]


class LogExceptionInterceptHandler(object):
    level = logging.ERROR

    def __init__(self):
        self.exceptions = []

    def __enter__(self):
        logging.getLogger().addHandler(self)
        return self

    def __exit__(self, exc_type, exc_val, exc_tb):
        logging.getLogger().removeHandler(self)

    def handle(self, record):
        if record.exc_info:
            self.exceptions.append(record.exc_info[1])

    def assert_exc(self, exc_class, msg=None):
        matchers = [hamcrest.instance_of(exc_class)]
        if msg:
            matchers.append(hamcrest.has_properties(msg=hamcrest.contains_string(msg)))

        hamcrest.assert_that(
            self.exceptions,
            hamcrest.only_contains(
                hamcrest.all_of(*matchers)
            )
        )


class BaseProcessCompletionsTest(object):
    @staticmethod
    def _assert_enqueued(order, expected_state=cst.ExportState.enqueued, reason=None):
        if expected_state is None:
            assert 'PROCESS_COMPLETION' not in order.exports
            return

        export = order.exports['PROCESS_COMPLETION']
        assert export.state == expected_state
        assert export.reason == reason

    @staticmethod
    def _assert_consumes(order, consumes_states, extra_params=None):
        hamcrest.assert_that(
            order.consumes,
            consumes_match(consumes_states, extra_params)
        )
