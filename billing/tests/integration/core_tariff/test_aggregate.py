# -*- coding: utf-8 -*-

import itertools
import json
import pytest

from yt.wrapper import TablePath, ypath_join

from billing.library.python.logfeller_utils.log_interval import (
    LogInterval,
    Subinterval,
)
from billing.log_tariffication.py.jobs.core_tariff import aggregate_events
from billing.log_tariffication.py.lib.constants import (
    LOG_TARIFF_META_ATTR,
    LOG_INTERVAL_KEY,
    CORRECTIONS_LOG_INTERVAL_KEY,
    DYN_TABLE_IS_UPDATING_KEY,
)
from billing.library.python.logmeta_utils.meta import (
    get_log_tariff_meta,
    set_log_tariff_meta,
)
from billing.log_tariffication.py.lib.schema import (
    STRIPPED_LOG_TABLE_SCHEMA,
    CORRECTIONS_LOG_TABLE_SCHEMA,
    ORDERS_TABLE_SCHEMA,
)

from billing.library.python.yt_utils.test_utils.utils import (
    create_subdirectory,
    create_dyntable,
)
from billing.log_tariffication.py.tests.utils import (
    OrderDataBuilder,
    check_node_is_locked,
)
from billing.log_tariffication.py.tests.constants import (
    OLD_RUN_ID,
    PREV_RUN_ID,
    CURR_RUN_ID,
    NEXT_RUN_ID,
    OLD_LOG_TARIFF_META,
    PREV_LOG_TARIFF_META,
    CURR_LOG_TARIFF_META,
    NEXT_LOG_TARIFF_META,
    OLD_LOG_INTERVAL,
    PREV_LOG_INTERVAL,
    CURR_LOG_INTERVAL,
    PREV_CORRECTIONS_INTERVAL,
    CURR_CORRECTIONS_INTERVAL,
    NEXT_CORRECTIONS_INTERVAL,
)


@pytest.fixture(name='aggregated_events_dir')
def aggregated_events_dir_fixture(yt_client, yt_root):
    return create_subdirectory(yt_client, yt_root, 'aggregated_events')


@pytest.fixture(name='corrections_dir')
def corrections_dir_fixture(yt_client, yt_root):
    return create_subdirectory(yt_client, yt_root, 'corrections')


@pytest.fixture(name='old_published_untariffed_table_path')
def old_published_untariffed_table_path_fixture(yt_root, published_untariffed_dir):
    return ypath_join(published_untariffed_dir, OLD_RUN_ID)


def create_corrections_table(yt_client, corrections_dir, run_id, orders, corrections_meta):
    corrections_path = ypath_join(corrections_dir, run_id)
    yt_client.write_table(
        TablePath(corrections_path, schema=CORRECTIONS_LOG_TABLE_SCHEMA),
        itertools.chain.from_iterable(o.corrections_untariffed for o in orders)
    )
    set_log_tariff_meta(yt_client, corrections_path, {CORRECTIONS_LOG_INTERVAL_KEY: corrections_meta})


def test_aggregate_events(
        yt_client, yql_client, stripped_log_path,
        prev_published_untariffed_table_path, published_untariffed_dir, orders_table_path, corrections_dir,
        aggregated_events_dir, old_published_untariffed_table_path
):
    consumes_counter = itertools.count(1)
    events_counter = itertools.count(1)

    orders = [
        OrderDataBuilder(1, consumes_counter, events_counter)
        .add_event(10)
        .add_event(3),
        OrderDataBuilder(2, consumes_counter, events_counter)
        .add_event(-6)
        .add_event(-60),
        OrderDataBuilder(3, consumes_counter, events_counter)
        .add_event(3.33)
        .add_event(-7.33)
        .add_event(4),
        OrderDataBuilder(4, consumes_counter, events_counter)
        .add_event(6.66)
        .add_untariffed_event(3.45),
        OrderDataBuilder(5, consumes_counter, events_counter, state='kthulhu fhtagn')
        .add_event(1, 1)
        .add_event(2, 2)
        .add_event(3, 3)
        .add_event(4, 4),
        OrderDataBuilder(6, consumes_counter, events_counter, currency_id=0)
        .add_event(320)
        .add_event(1),
        OrderDataBuilder(6, consumes_counter, events_counter, currency_id=666)
        .add_untariffed_event(345),
        OrderDataBuilder(7, consumes_counter, events_counter, service_id=999)
        .add_untariffed_event(123),
    ]

    # Несмотря на количество событий, никак не сказывается на времени работы теста
    big_order = OrderDataBuilder(7, consumes_counter, events_counter)
    for time_ in range(10001):
        big_order.add_event(0.000001, time_+167800)
    orders.append(big_order)

    orders.extend([
        OrderDataBuilder(8, consumes_counter, events_counter)
        .add_event(3)
        .add_event(4)
        .add_untariffed_event(5)
        .skip(),
    ])

    orders_table_rows = {(o.service_id, o.id, o.state) for o in orders if not o.is_skipped}
    orders_table_rows = [
        {'ServiceID': sid, 'EffectiveServiceOrderID': id_, 'state': state}
        for sid, id_, state in orders_table_rows
    ]
    orders_table_rows = sorted(
        orders_table_rows,
        key=lambda o: (o['ServiceID'], o['EffectiveServiceOrderID'])
    )
    create_dyntable(
        yt_client, orders_table_path,
        schema=ORDERS_TABLE_SCHEMA,
        data=orders_table_rows,
        attributes={
            LOG_TARIFF_META_ATTR: {
                LOG_INTERVAL_KEY: PREV_LOG_TARIFF_META[LOG_INTERVAL_KEY],
                DYN_TABLE_IS_UPDATING_KEY: False,
            },
        }
    )

    yt_client.write_table(
        TablePath(
            prev_published_untariffed_table_path,
            schema=STRIPPED_LOG_TABLE_SCHEMA
        ),
        itertools.chain.from_iterable(o.untariffed_events for o in orders)
    )
    yt_client.run_sort(prev_published_untariffed_table_path,
                       sort_by=['ServiceID', 'EffectiveServiceOrderID', 'EventTime'])
    set_log_tariff_meta(yt_client, prev_published_untariffed_table_path, PREV_LOG_TARIFF_META)

    # Must be ignored in favor of the prev table.
    yt_client.create('table', old_published_untariffed_table_path)
    set_log_tariff_meta(yt_client, old_published_untariffed_table_path,
                        NEXT_LOG_TARIFF_META)

    yt_client.write_table(
        TablePath(
            stripped_log_path,
            schema=STRIPPED_LOG_TABLE_SCHEMA
        ),
        itertools.chain.from_iterable(o.new_events for o in orders)
    )
    yt_client.run_sort(stripped_log_path,
                       sort_by=['ServiceID', 'EffectiveServiceOrderID', 'EventTime'])
    set_log_tariff_meta(yt_client, stripped_log_path, CURR_LOG_TARIFF_META)

    with yt_client.Transaction(ping=False) as transaction:
        aggregated_events_path = aggregate_events.run_job(
            yt_client,
            yql_client,
            stripped_log_path,
            published_untariffed_dir,
            orders_table_path,
            corrections_dir,
            None,
            aggregated_events_dir,
            transaction,
        )

        check_node_is_locked(aggregated_events_dir)

    assert get_log_tariff_meta(yt_client, aggregated_events_path) == CURR_LOG_TARIFF_META

    res = list(yt_client.read_table(aggregated_events_path))
    return {
        'aggregates': res,
    }


@pytest.mark.parametrize(
    'corrections_meta',
    [
        CURR_CORRECTIONS_INTERVAL,
        NEXT_CORRECTIONS_INTERVAL,
        LogInterval([  # sum of intervals
            Subinterval('cor_c1', 'cor_t1', 0, 1, 3),
            Subinterval('cor_c1', 'cor_t1', 1, 15, 35),
        ]),
    ],
)
def test_add_corrections_data(
        yt_client, yql_client, stripped_log_path,
        prev_published_untariffed_table_path, published_untariffed_dir, orders_table_path, corrections_dir,
        aggregated_events_dir, corrections_meta,
):
    consumes_counter = itertools.count(1)
    events_counter = itertools.count(1)
    metadata = {
        'run_id': CURR_RUN_ID,
        'prev_run_id': PREV_RUN_ID,
        LOG_INTERVAL_KEY: CURR_LOG_INTERVAL.to_meta(),
        CORRECTIONS_LOG_INTERVAL_KEY: corrections_meta.to_meta(),
        DYN_TABLE_IS_UPDATING_KEY: False,
        'tariff_date': 172800,
    }

    orders = [
        OrderDataBuilder(1, consumes_counter, events_counter, order_id=1)
            .add_event(1),
        OrderDataBuilder(2, consumes_counter, events_counter, order_id=1)
            .add_event(1),
    ]

    orders_table_rows = {(o.service_id, o.id, o.state) for o in orders}
    orders_table_rows = [
        {'ServiceID': sid, 'EffectiveServiceOrderID': id_, 'state': state}
        for sid, id_, state in orders_table_rows
    ]
    orders_table_rows = sorted(orders_table_rows, key=lambda o: (o['ServiceID'], o['EffectiveServiceOrderID']))
    create_dyntable(
        yt_client, orders_table_path,
        schema=ORDERS_TABLE_SCHEMA,
        data=orders_table_rows,
        attributes={
            LOG_TARIFF_META_ATTR: {
                LOG_INTERVAL_KEY: PREV_LOG_TARIFF_META[LOG_INTERVAL_KEY],
                DYN_TABLE_IS_UPDATING_KEY: False,
            },
        },
    )

    yt_client.write_table(
        TablePath(prev_published_untariffed_table_path, schema=STRIPPED_LOG_TABLE_SCHEMA),
        itertools.chain.from_iterable(o.untariffed_events for o in orders)
    )
    yt_client.run_sort(prev_published_untariffed_table_path, sort_by=['ServiceID', 'EffectiveServiceOrderID', 'EventTime'])
    set_log_tariff_meta(yt_client, prev_published_untariffed_table_path, PREV_LOG_TARIFF_META)

    yt_client.write_table(
        TablePath(stripped_log_path, schema=STRIPPED_LOG_TABLE_SCHEMA),
        itertools.chain.from_iterable(o.new_events for o in orders)
    )
    yt_client.run_sort(stripped_log_path, sort_by=['ServiceID', 'EffectiveServiceOrderID', 'EventTime'])
    set_log_tariff_meta(yt_client, stripped_log_path, metadata)

    curr_orders = [
        OrderDataBuilder(1, consumes_counter, events_counter, order_id=1)
            .add_corrections_untariffed(0.1),
        OrderDataBuilder(1, consumes_counter, events_counter, order_id=2)
            .add_corrections_untariffed(0.01),
        OrderDataBuilder(1, consumes_counter, events_counter, order_id=3)
            .add_corrections_untariffed(0.001),
        OrderDataBuilder(2, consumes_counter, events_counter, order_id=1)
            .add_corrections_untariffed(10),
    ]
    next_orders = [
        OrderDataBuilder(1, consumes_counter, events_counter, order_id=2)
            .add_corrections_untariffed(0.2),
        OrderDataBuilder(1, consumes_counter, events_counter, order_id=3)
            .add_corrections_untariffed(0.02),
        OrderDataBuilder(2, consumes_counter, events_counter, order_id=2)
            .add_corrections_untariffed(100),
    ]
    correction_data = zip(
        [CURR_RUN_ID, NEXT_RUN_ID],
        [CURR_CORRECTIONS_INTERVAL.to_meta(), NEXT_CORRECTIONS_INTERVAL.to_meta()],
        [curr_orders, next_orders],
    )
    for run_id, cor_meta, orders in correction_data:
        create_corrections_table(yt_client, corrections_dir, run_id, orders, cor_meta)

    with yt_client.Transaction(ping=False) as transaction:
        aggregated_events_path = aggregate_events.run_job(
            yt_client,
            yql_client,
            stripped_log_path,
            published_untariffed_dir,
            orders_table_path,
            corrections_dir,
            None,
            aggregated_events_dir,
            transaction,
        )

        check_node_is_locked(aggregated_events_dir)

    assert get_log_tariff_meta(yt_client, aggregated_events_path) == metadata

    res = list(yt_client.read_table(aggregated_events_path))
    return {
        'aggregates': res,
    }


def test_already_done(yt_client, yql_client, caplog, yt_transaction,
                      stripped_log_path, prev_published_untariffed_table_path,
                      published_untariffed_dir, orders_table_path, aggregated_events_dir):
    yt_client.create('table', stripped_log_path, attributes={
        LOG_TARIFF_META_ATTR: CURR_LOG_TARIFF_META
    })

    yt_client.create('table', prev_published_untariffed_table_path, attributes={
        LOG_TARIFF_META_ATTR: PREV_LOG_TARIFF_META
    })

    yt_client.create('table', orders_table_path, attributes={
        LOG_INTERVAL_KEY: PREV_LOG_TARIFF_META[LOG_INTERVAL_KEY],
        DYN_TABLE_IS_UPDATING_KEY: False,
    })

    res_table_path = ypath_join(aggregated_events_dir, CURR_RUN_ID)
    yt_client.create('table', res_table_path, attributes={
        LOG_TARIFF_META_ATTR: CURR_LOG_TARIFF_META
    })
    assert aggregate_events.run_job(
        yt_client, yql_client, stripped_log_path,
        published_untariffed_dir, orders_table_path, None, None,
        aggregated_events_dir, yt_transaction
    ) == res_table_path
    assert "Already done" in caplog.text


@pytest.mark.parametrize(
    ['params', 'exc_match'],
    [
        pytest.param({
            'aggregated_table_meta': NEXT_LOG_TARIFF_META,
            'aggregated_table_name': NEXT_RUN_ID,
        }, 'Next result table exists', id='next result exists'),
        pytest.param({
            'aggregated_table_meta': PREV_LOG_TARIFF_META,
            'aggregated_table_name': CURR_RUN_ID,
        }, 'Bad meta in current result table', id='bad meta in curr table'),
        pytest.param({
            'aggregated_table_meta': NEXT_LOG_TARIFF_META,
            'aggregated_table_name': PREV_RUN_ID,
        }, 'Bad interval in previous result table', id='bad meta in prev table'),
        pytest.param({
            'untariffed_meta': NEXT_LOG_TARIFF_META
        }, 'Bad interval in untariffed table', id='bad meta in untariffed table'),
        pytest.param({
            'orders_meta': {
                LOG_INTERVAL_KEY: NEXT_LOG_TARIFF_META[LOG_INTERVAL_KEY],
                DYN_TABLE_IS_UPDATING_KEY: False,
            }
        }, 'Bad interval in orders table', id='bad meta in orders table'),
        pytest.param({
            'orders_meta': {
                LOG_INTERVAL_KEY: PREV_LOG_TARIFF_META[LOG_INTERVAL_KEY],
                DYN_TABLE_IS_UPDATING_KEY: True,
            }
        }, 'Orders table is being updated', id='orders table is being updated'),
        pytest.param({
            'wo_corrections_dir': True,
        }, 'corrections_dir is required.', id='corrections_dir is required'),
    ]
)
def test_assertions(yt_client, yql_client, yt_transaction, params,
                    stripped_log_path, prev_published_untariffed_table_path, published_untariffed_dir,
                    orders_table_path, corrections_dir, aggregated_events_dir, exc_match):
    yt_client.create('table', stripped_log_path, attributes={
        LOG_TARIFF_META_ATTR: dict(
            log_interval=CURR_LOG_INTERVAL.to_meta(),
            corrections_log_interval=PREV_CORRECTIONS_INTERVAL.to_meta(),
            run_id=CURR_RUN_ID,
            prev_run_id=PREV_RUN_ID,
        ),
    })

    yt_client.create('table', prev_published_untariffed_table_path, attributes={
        LOG_TARIFF_META_ATTR: params.get('untariffed_meta', PREV_LOG_TARIFF_META)
    })

    yt_client.create('table', orders_table_path, attributes={
        LOG_TARIFF_META_ATTR: params.get('orders_meta', {
            LOG_INTERVAL_KEY: PREV_LOG_TARIFF_META[LOG_INTERVAL_KEY],
            CORRECTIONS_LOG_INTERVAL_KEY: PREV_CORRECTIONS_INTERVAL.to_meta(),
            DYN_TABLE_IS_UPDATING_KEY: False,
        })
    })

    yt_client.create('table', ypath_join(corrections_dir, CURR_RUN_ID), attributes={
        LOG_TARIFF_META_ATTR: {
            LOG_INTERVAL_KEY: PREV_LOG_TARIFF_META[LOG_INTERVAL_KEY],
            CORRECTIONS_LOG_INTERVAL_KEY: PREV_CORRECTIONS_INTERVAL.to_meta(),
        },
    })
    create_corrections_table(yt_client, corrections_dir, PREV_RUN_ID,
                             [OrderDataBuilder(1).add_corrections_untariffed(1)], PREV_CORRECTIONS_INTERVAL.to_meta())

    if 'aggregated_table_meta' in params:
        yt_client.create(
            'table',
            ypath_join(aggregated_events_dir, params['aggregated_table_name']),
            attributes={
                LOG_TARIFF_META_ATTR: params['aggregated_table_meta']
            }
        )

    with pytest.raises(AssertionError, match=exc_match):
        aggregate_events.run_job(
            yt_client,
            yql_client,
            stripped_log_path,
            published_untariffed_dir,
            orders_table_path,
            corrections_dir if not params.get('wo_corrections_dir') else None,
            None,
            aggregated_events_dir,
            yt_transaction,
        )


@pytest.mark.parametrize(
    ['table_interval', 'expected_result'],
    [
        pytest.param(
            LogInterval([Subinterval('c1', 't1', 0, 5, 10)]), True,
            id='precedes'
        ),
        pytest.param(
            LogInterval([Subinterval('c1', 't1', 0, 20, 30)]), True,
            id='ahead'
        ),
        pytest.param(
            LogInterval([Subinterval('c1', 't1', 0, 0, 5)]), False,
            id='behind'
        ),
    ]
)
def test_shared_table_is_ready(table_interval, expected_result):
    assert aggregate_events.shared_table_is_ready(
        table_interval,
        LogInterval([Subinterval('c1', 't1', 0, 10, 20)])
    ) is expected_result


def test_wait_tariff_results_check_untariffed_dir_empty(
        yt_client, published_untariffed_dir, orders_table_path
):
    with pytest.raises(AssertionError, match='Untariffed directory is empty'):
        aggregate_events.wait_tariff_results_check(
            yt_client, CURR_LOG_TARIFF_META[LOG_INTERVAL_KEY],
            orders_table_path, published_untariffed_dir
        )


@pytest.mark.parametrize(
    ['dyn_table_is_updating', 'untariffed_table_meta',
     'orders_table_interval', 'expected_result'],
    [
        pytest.param(False, PREV_LOG_TARIFF_META, PREV_LOG_INTERVAL, True,
                     id='ok'),
        pytest.param(True, PREV_LOG_TARIFF_META, PREV_LOG_INTERVAL, False,
                     id='dyn table is updating'),
        pytest.param(False, OLD_LOG_TARIFF_META, PREV_LOG_INTERVAL, False,
                     id='old untariffed meta'),
        pytest.param(False, PREV_LOG_TARIFF_META, OLD_LOG_INTERVAL, False,
                     id='old orders meta'),
    ]
)
def test_wait_tariff_results_check(
        yt_client, published_untariffed_dir, prev_published_untariffed_table_path, orders_table_path,
        dyn_table_is_updating, untariffed_table_meta, orders_table_interval,
        expected_result
):
    yt_client.create('table', orders_table_path)
    set_log_tariff_meta(yt_client, orders_table_path, {
        LOG_INTERVAL_KEY: orders_table_interval.to_meta(),
        DYN_TABLE_IS_UPDATING_KEY: dyn_table_is_updating,
    })

    yt_client.create('table', prev_published_untariffed_table_path)
    set_log_tariff_meta(yt_client, prev_published_untariffed_table_path, untariffed_table_meta)

    assert aggregate_events.wait_tariff_results_check(
        yt_client, CURR_LOG_INTERVAL,
        orders_table_path, published_untariffed_dir
    ) is expected_result


def test_different_services(
        yt_client, yql_client, stripped_log_path,
        prev_published_untariffed_table_path, published_untariffed_dir, orders_table_path, corrections_dir,
        aggregated_events_dir
):
    allowed_services = [7, 3]
    consumes_counter = itertools.count(1)
    events_counter = itertools.count(1)

    orders = [
        OrderDataBuilder(1, consumes_counter, events_counter, service_id=7)
        .add_event(10)
        .add_event(3),
        OrderDataBuilder(2, consumes_counter, events_counter, service_id=7)
        .add_untariffed_event(123),
        OrderDataBuilder(3, consumes_counter, events_counter, service_id=67)
        .add_untariffed_event(678),
        OrderDataBuilder(4, consumes_counter, events_counter, service_id=3)
        .add_untariffed_event(345),
    ]

    create_dyntable(
        yt_client, orders_table_path,
        schema=ORDERS_TABLE_SCHEMA,
        data=[
            {'ServiceID': o.service_id, 'EffectiveServiceOrderID': o.id, 'state': o.state}
            for o in sorted(orders, key=lambda x: (x.service_id, x.id)) if o.service_id in allowed_services
        ],
        attributes={
            LOG_TARIFF_META_ATTR: {
                LOG_INTERVAL_KEY: PREV_LOG_TARIFF_META[LOG_INTERVAL_KEY],
                DYN_TABLE_IS_UPDATING_KEY: False,
            },
        }
    )

    yt_client.write_table(
        TablePath(
            prev_published_untariffed_table_path,
            schema=STRIPPED_LOG_TABLE_SCHEMA
        ),
        itertools.chain.from_iterable(o.untariffed_events for o in orders)
    )
    yt_client.run_sort(prev_published_untariffed_table_path, sort_by=['ServiceID', 'EffectiveServiceOrderID', 'EventTime'])
    set_log_tariff_meta(yt_client, prev_published_untariffed_table_path, PREV_LOG_TARIFF_META)

    yt_client.write_table(
        TablePath(
            stripped_log_path,
            schema=STRIPPED_LOG_TABLE_SCHEMA
        ),
        itertools.chain.from_iterable(o.new_events for o in orders)
    )
    yt_client.run_sort(stripped_log_path, sort_by=['ServiceID', 'EffectiveServiceOrderID', 'EventTime'])
    set_log_tariff_meta(yt_client, stripped_log_path, CURR_LOG_TARIFF_META)

    with yt_client.Transaction(ping=False) as transaction:
        aggregated_events_path = aggregate_events.run_job(
            yt_client,
            yql_client,
            stripped_log_path,
            published_untariffed_dir,
            orders_table_path,
            corrections_dir,
            json.dumps(allowed_services),
            aggregated_events_dir,
            transaction,
        )

        check_node_is_locked(aggregated_events_dir)

    return {
        'aggregates': list(yt_client.read_table(aggregated_events_path)),
    }
