# -*- coding: utf-8 -*-

import pytest

from yt.wrapper import (
    ypath_join,
    common as yt_common,
)

from billing.log_tariffication.py.lib.constants import (
    LOG_TARIFF_META_ATTR,
    LOG_INTERVAL_KEY,
    RUN_ID_KEY,
)
from billing.log_tariffication.py.jobs.core_tariff import init_migration

from billing.library.python.yt_utils.test_utils.utils import (
    create_subdirectory,
    create_yt_client,
)
from billing.log_tariffication.py.tests.utils import (
    create_historical_aggregates_dyntable,
    create_billable_log_table,
)
from billing.log_tariffication.py.tests.constants import (
    CURR_RUN_ID,
    NEXT_RUN_ID,
)
from billing.library.python.logfeller_utils.log_interval import (
    LogInterval,
    Subinterval,
)

INPUT_ORDERS_TABLE_SCHEMA = [
    {'name': 'ServiceID', 'type': 'int64'},
    {'name': 'ServiceOrderID', 'type': 'int64'},
    {'name': 'GroupServiceOrderID', 'type': 'int64'},
]

ORDERS_TABLE_SCHEMA = [
    {'name': 'ServiceID', 'type': 'int64'},
    {'name': 'EffectiveServiceOrderID', 'type': 'int64'},
    {'name': 'state', 'type': 'string'},
]

EVENTS_TABLE_SCHEMA = [
    {'name': 'ServiceID', 'type': 'int64', 'sort_order': 'ascending'},
    {'name': 'EffectiveServiceOrderID', 'type': 'int64', 'sort_order': 'ascending'},
    {'name': 'CurrencyID', 'type': 'uint64', 'sort_order': 'ascending'},
    {'name': 'LBMessageUID', 'type': 'string', 'sort_order': 'ascending'},
    {'name': 'EventTime', 'type': 'int64'},
    {'name': 'BillableEventCostCur', 'type': 'double'},
]


@pytest.fixture(name='input_orders_path')
def input_orders_path_fixture(yt_root):
    return ypath_join(yt_root, 'input')


@pytest.fixture(name='aggregates_path')
def aggregates_path_fixture(yt_root):
    return ypath_join(yt_root, 'main_table')


@pytest.fixture(name='log_dir')
def log_dir_fixture(yt_client, yt_root):
    return create_subdirectory(yt_client, yt_root, 'input_log')


@pytest.fixture(name='events_path')
def events_path_fixture(yt_client, yt_root):
    return ypath_join(yt_root, 'events')


@pytest.fixture(name='orders_path')
def orders_path_fixture(yt_client, yt_root):
    return ypath_join(yt_root, 'orders')


@pytest.fixture(name='res_events_dir')
def res_events_dir_fixture(yt_client, yt_root):
    return create_subdirectory(yt_client, yt_root, 'res_events')


@pytest.fixture(name='res_orders_dir')
def res_orders_dir_fixture(yt_client, yt_root):
    return create_subdirectory(yt_client, yt_root, 'res_orders')


def create_input_orders_table(yt_client, path, data):
    yt_client.create('table', path, attributes={'schema': INPUT_ORDERS_TABLE_SCHEMA})

    yt_client.write_table(
        path,
        [
            {'ServiceID': sid, 'ServiceOrderID': soid, 'GroupServiceOrderID': gsoid}
            for sid, soid, gsoid in data
        ]
    )


def create_orders_table(yt_client, path, log_interval, data, run_id=None):
    yt_client.create(
        'table',
        path,
        attributes={
            LOG_TARIFF_META_ATTR: {
                LOG_INTERVAL_KEY: log_interval.to_meta(),
                RUN_ID_KEY: run_id or CURR_RUN_ID,
            },
            'schema': ORDERS_TABLE_SCHEMA,
        }
    )

    yt_client.write_table(
        path,
        [
            {'ServiceID': sid, 'EffectiveServiceOrderID': soid, 'state': 'state_' + str(idx)}
            for idx, (sid, soid) in enumerate(data)
        ]
    )


def create_events_table(yt_client, path, log_interval, data, run_id=None):
    yt_client.create(
        'table',
        path,
        attributes={
            LOG_TARIFF_META_ATTR: {
                LOG_INTERVAL_KEY: log_interval.to_meta(),
                RUN_ID_KEY: run_id or CURR_RUN_ID,
            },
            'schema': EVENTS_TABLE_SCHEMA,
        }
    )

    yt_client.write_table(
        path,
        [
            {
                'ServiceID': sid,
                'EffectiveServiceOrderID': soid,
                'CurrencyID': cid,
                'LBMessageUID': 'uid_' + str(idx),
                'BillableEventCostCur': float((1 + idx) * 6.66),
                'EventTime': idx,
            }
            for idx, (sid, soid, cid) in enumerate(data)
        ]
    )


def get_result(yt_client, events_path, orders_path):
    return {
        'orders': {
            'meta': yt_client.get(ypath_join(orders_path, '@' + LOG_TARIFF_META_ATTR))[LOG_INTERVAL_KEY],
            'rows': list(yt_client.read_table(orders_path)),
        },
        'events': {
            'meta': yt_client.get(ypath_join(events_path, '@' + LOG_TARIFF_META_ATTR))[LOG_INTERVAL_KEY],
            'rows': list(yt_client.read_table(events_path)),
        },
    }


@pytest.mark.parametrize(
    ['have_input_orders_dyntable_update_path'],
    [
        pytest.param(True, id='with_input_orders_dyntable_update_path'),
        pytest.param(False, id='without_input_orders_dyntable_update_path'),
    ]
)
def test_base(yt_client, yql_client, input_orders_path, aggregates_path, log_dir, events_path, orders_path,
              res_events_dir, res_orders_dir, have_input_orders_dyntable_update_path):
    create_input_orders_table(
        yt_client,
        input_orders_path,
        [
            (7, 1, 10),
            (7, 2, 10),
            (7, 3, 20),
            (7, 8, 30),  # Без откруток, но все равно должен попасть в orders
        ]
    )

    create_historical_aggregates_dyntable(
        yt_client,
        aggregates_path,
        LogInterval([Subinterval('c', 't', 0, 0, 10)]),
        [
            (7, 1, 1, 10, 1),
            (7, 1, 2, 66, 1),
            (7, 2, 1, 2, 1),
            (7, 3, 0, 50, 66),
            (7, 4, 1, 666, 7),
        ]
    )

    create_billable_log_table(
        yt_client,
        ypath_join(log_dir, CURR_RUN_ID),
        LogInterval([Subinterval('c', 't', 0, 10, 20)]),
        [
            (7, 1, 1, 1.1666, 0, 2),
            (7, 1, 1, 0.8334, 0, 3),
            (7, 2, 2, 14, 0, 0),
            (7, 3, 0, 666, 16000001, 2),
            (7, 3, 1, 666, 16000001, 2),
            (7, 3, 1, 0.0001, 0, 3),
            (7, 3, 1, 0.0001, 0, 3),
            (7, 3, 1, 0.0666, 0, 3),
            (7, 3, 1, 0.0001, 0, 3),
            (7, 3, 1, 0.0001, 0, 3),
        ]
    )

    create_orders_table(
        yt_client,
        orders_path,
        LogInterval([Subinterval('c', 't', 0, 9, 16)]),
        [
            (7, 6),
            (7, 7),
        ]
    )

    create_events_table(
        yt_client,
        events_path,
        LogInterval([Subinterval('c', 't', 0, 9, 16)]),
        [
            (7, 6, 0),
            (7, 6, 1),
            (7, 7, 666),
        ]
    )

    with yt_client.Transaction() as transaction:
        res_events_path, res_orders_path = init_migration.run_job(
            yt_client,
            yql_client,
            transaction.transaction_id,
            input_orders_path,
            aggregates_path,
            log_dir,
            events_path,
            orders_path if have_input_orders_dyntable_update_path else None,
            res_events_dir,
            res_orders_dir,
            uid_prefix='some_run_id',
        )

    if have_input_orders_dyntable_update_path:
        return get_result(yt_client, res_events_path, res_orders_path)


def test_no_log(yt_client, yql_client, input_orders_path, aggregates_path, log_dir, events_path, orders_path,
                res_events_dir, res_orders_dir):
    create_input_orders_table(
        yt_client,
        input_orders_path,
        [
            (7, 1, 10),
            (7, 2, 10),
        ]
    )

    create_historical_aggregates_dyntable(
        yt_client,
        aggregates_path,
        LogInterval([Subinterval('c', 't', 0, 0, 10)]),
        [
            (7, 1, 0, 10, 1),
            (7, 1, 2, 66, 1),
            (7, 2, 1, 2, 1),
        ]
    )

    create_orders_table(
        yt_client,
        orders_path,
        LogInterval([Subinterval('c', 't', 0, 5, 10)]),
        [
            (7, 6),
        ]
    )

    create_events_table(
        yt_client,
        events_path,
        LogInterval([Subinterval('c', 't', 0, 5, 10)]),
        [
            (7, 6, 0),
        ]
    )

    with yt_client.Transaction() as transaction:
        res_events_path, res_orders_path = init_migration.run_job(
            yt_client,
            yql_client,
            transaction.transaction_id,
            input_orders_path,
            aggregates_path,
            log_dir,
            events_path,
            orders_path,
            res_events_dir,
            res_orders_dir,
            uid_prefix='some_run_id',
        )

    return get_result(yt_client, res_events_path, res_orders_path)


def test_log_intervals_mismatch(yt_client, yql_client, input_orders_path, aggregates_path, log_dir, events_path,
                                orders_path, res_events_dir, res_orders_dir):
    create_input_orders_table(yt_client, input_orders_path, [(7, 1, 10)])

    create_historical_aggregates_dyntable(
        yt_client,
        aggregates_path,
        LogInterval([
            Subinterval('c', 't', 0, 0, 10),
            Subinterval('c', 't', 1, 0, 10),
        ]),
        [(7, 1, 1, 666, 1)]
    )

    create_orders_table(
        yt_client,
        orders_path,
        LogInterval([
            Subinterval('c', 't', 0, 5, 9),
            Subinterval('c', 't', 1, 8, 11),
        ]),
        [(7, 2)]
    )

    create_events_table(
        yt_client,
        events_path,
        LogInterval([
            Subinterval('c', 't', 0, 5, 9),
            Subinterval('c', 't', 1, 8, 11),
        ]),
        [(7, 2, 0)]
    )

    with pytest.raises(AssertionError) as exc_info:
        with yt_client.Transaction() as transaction:
            init_migration.run_job(
                yt_client,
                yql_client,
                transaction.transaction_id,
                input_orders_path,
                aggregates_path,
                log_dir,
                events_path,
                orders_path,
                res_events_dir,
                res_orders_dir,
            )

    assert "Tables are broken!" in exc_info.value.args[0]


def test_tables_intervals_mismatch(yt_client, yql_client, input_orders_path, aggregates_path, log_dir, events_path,
                                   orders_path, res_events_dir, res_orders_dir):
    create_input_orders_table(yt_client, input_orders_path, [(7, 1, 10)])

    create_historical_aggregates_dyntable(
        yt_client,
        aggregates_path,
        LogInterval([Subinterval('c', 't', 0, 0, 10)]),
        [(7, 1, 1, 666, 1)]
    )

    create_orders_table(
        yt_client,
        orders_path,
        LogInterval([Subinterval('c', 't', 0, 5, 9)]),
        [(7, 2)]
    )

    create_events_table(
        yt_client,
        events_path,
        LogInterval([Subinterval('c', 't', 0, 5, 8)]),
        [(7, 2, 0)]
    )

    with pytest.raises(AssertionError) as exc_info:
        with yt_client.Transaction() as transaction:
            init_migration.run_job(
                yt_client,
                yql_client,
                transaction.transaction_id,
                input_orders_path,
                aggregates_path,
                log_dir,
                events_path,
                orders_path,
                res_events_dir,
                res_orders_dir,
            )

    assert "Intervals mismatch in" in exc_info.value.args[0]


def test_existing_result(yt_client, yql_client, input_orders_path, aggregates_path, log_dir, events_path,
                         orders_path, res_events_dir, res_orders_dir):
    create_input_orders_table(yt_client, input_orders_path, [(7, 1, 10)])

    create_historical_aggregates_dyntable(
        yt_client,
        aggregates_path,
        LogInterval([Subinterval('c', 't', 0, 0, 10)]),
        [(7, 1, 1, 666, 1)],
        is_updating=True
    )

    create_orders_table(
        yt_client,
        orders_path,
        LogInterval([Subinterval('c', 't', 0, 5, 11)]),
        [(7, 2)],
        run_id=NEXT_RUN_ID,
    )

    create_events_table(
        yt_client,
        events_path,
        LogInterval([Subinterval('c', 't', 0, 5, 11)]),
        [(7, 2, 0)],
        run_id=NEXT_RUN_ID,
    )

    create_orders_table(
        yt_client,
        ypath_join(res_orders_dir, NEXT_RUN_ID),
        LogInterval([Subinterval('c', 't', 0, 5, 11)]),
        [(7, 666)],
        run_id=NEXT_RUN_ID,
    )

    create_events_table(
        yt_client,
        ypath_join(res_events_dir, NEXT_RUN_ID),
        LogInterval([Subinterval('c', 't', 0, 5, 11)]),
        [(7, 666, 0)],
        run_id=NEXT_RUN_ID
    )

    with yt_client.Transaction() as transaction:
        res_events_path, res_orders_path = init_migration.run_job(
            yt_client,
            yql_client,
            transaction.transaction_id,
            input_orders_path,
            aggregates_path,
            log_dir,
            events_path,
            orders_path,
            res_events_dir,
            res_orders_dir,
        )

    assert res_events_path == ypath_join(res_events_dir, NEXT_RUN_ID)
    assert res_orders_path == ypath_join(res_orders_dir, NEXT_RUN_ID)
    return get_result(yt_client, res_events_path, res_orders_path)


def test_part_existing_result(yt_client, yql_client, input_orders_path, aggregates_path, log_dir, events_path,
                              orders_path, res_events_dir, res_orders_dir):
    create_input_orders_table(yt_client, input_orders_path, [(7, 1, 10)])

    create_historical_aggregates_dyntable(
        yt_client,
        aggregates_path,
        LogInterval([Subinterval('c', 't', 0, 0, 10)]),
        [(7, 1, 1, 666, 1)],
        is_updating=True
    )

    create_orders_table(
        yt_client,
        orders_path,
        LogInterval([Subinterval('c', 't', 0, 5, 11)]),
        [(7, 2)],
        run_id=NEXT_RUN_ID,
    )

    create_events_table(
        yt_client,
        events_path,
        LogInterval([Subinterval('c', 't', 0, 5, 11)]),
        [(7, 2, 0)],
        run_id=NEXT_RUN_ID,
    )

    create_orders_table(
        yt_client,
        ypath_join(res_orders_dir, CURR_RUN_ID),
        LogInterval([Subinterval('c', 't', 0, 5, 11)]),
        [(7, 666)],
        run_id=NEXT_RUN_ID,
    )

    create_events_table(
        yt_client,
        ypath_join(res_events_dir, NEXT_RUN_ID),
        LogInterval([Subinterval('c', 't', 0, 5, 11)]),
        [(7, 666, 0)],
        run_id=NEXT_RUN_ID
    )

    with pytest.raises(AssertionError) as exc_info:
        with yt_client.Transaction() as transaction:
            init_migration.run_job(
                yt_client,
                yql_client,
                transaction.transaction_id,
                input_orders_path,
                aggregates_path,
                log_dir,
                events_path,
                orders_path,
                res_events_dir,
                res_orders_dir,
            )

    assert "Partial result cache" in exc_info.value.args[0]


def test_result_intervals_mismatch(yt_client, yql_client, input_orders_path, aggregates_path, log_dir, events_path,
                                   orders_path, res_events_dir, res_orders_dir):
    create_input_orders_table(yt_client, input_orders_path, [(7, 1, 10)])

    create_historical_aggregates_dyntable(
        yt_client,
        aggregates_path,
        LogInterval([Subinterval('c', 't', 0, 0, 10)]),
        [(7, 1, 1, 666, 1)],
        is_updating=True
    )

    create_orders_table(
        yt_client,
        orders_path,
        LogInterval([Subinterval('c', 't', 0, 5, 11)]),
        [(7, 2)],
        run_id=NEXT_RUN_ID,
    )

    create_events_table(
        yt_client,
        events_path,
        LogInterval([Subinterval('c', 't', 0, 5, 11)]),
        [(7, 2, 0)],
        run_id=NEXT_RUN_ID,
    )

    create_orders_table(
        yt_client,
        ypath_join(res_orders_dir, NEXT_RUN_ID),
        LogInterval([Subinterval('c', 't', 0, 5, 10)]),
        [(7, 666)],
        run_id=NEXT_RUN_ID,
    )

    create_events_table(
        yt_client,
        ypath_join(res_events_dir, NEXT_RUN_ID),
        LogInterval([Subinterval('c', 't', 0, 5, 11)]),
        [(7, 666, 0)],
        run_id=NEXT_RUN_ID
    )

    with pytest.raises(AssertionError) as exc_info:
        with yt_client.Transaction() as transaction:
            init_migration.run_job(
                yt_client,
                yql_client,
                transaction.transaction_id,
                input_orders_path,
                aggregates_path,
                log_dir,
                events_path,
                orders_path,
                res_events_dir,
                res_orders_dir,
            )

    assert "Wrong meta in results cache" in exc_info.value.args[0]


def test_dyntable_in_progress(yt_client, yql_client, input_orders_path, aggregates_path, log_dir, events_path,
                              orders_path, res_events_dir, res_orders_dir):
    create_input_orders_table(yt_client, input_orders_path, [(7, 1, 10)])

    create_historical_aggregates_dyntable(
        yt_client,
        aggregates_path,
        LogInterval([Subinterval('c', 't', 0, 0, 10)]),
        [(7, 1, 1, 666, 1)],
        is_updating=True
    )

    create_orders_table(
        yt_client,
        orders_path,
        LogInterval([Subinterval('c', 't', 0, 5, 9)]),
        [(7, 2)]
    )

    create_events_table(
        yt_client,
        events_path,
        LogInterval([Subinterval('c', 't', 0, 5, 9)]),
        [(7, 2, 0)]
    )

    with pytest.raises(AssertionError) as exc_info:
        with yt_client.Transaction() as transaction:
            init_migration.run_job(
                yt_client,
                yql_client,
                transaction.transaction_id,
                input_orders_path,
                aggregates_path,
                log_dir,
                events_path,
                orders_path,
                res_events_dir,
                res_orders_dir,
            )

    assert "dyntable {} is updating!".format(aggregates_path) in exc_info.value.args[0]


def test_dyntable_lock(yt_client, yql_client, input_orders_path, aggregates_path, log_dir, events_path,
                              orders_path, res_events_dir, res_orders_dir):
    create_input_orders_table(yt_client, input_orders_path, [(7, 1, 10)])

    create_historical_aggregates_dyntable(
        yt_client,
        aggregates_path,
        LogInterval([Subinterval('c', 't', 0, 0, 10)]),
        [(7, 1, 1, 666, 1)]
    )

    create_orders_table(
        yt_client,
        orders_path,
        LogInterval([Subinterval('c', 't', 0, 5, 9)]),
        [(7, 2)]
    )

    create_events_table(
        yt_client,
        events_path,
        LogInterval([Subinterval('c', 't', 0, 5, 9)]),
        [(7, 2, 0)]
    )

    alt_yt_client = create_yt_client()
    with alt_yt_client.Transaction():
        alt_yt_client.lock(aggregates_path)

        with pytest.raises(yt_common.YtError) as exc_info:
            with yt_client.Transaction() as transaction:
                init_migration.run_job(
                    yt_client,
                    yql_client,
                    transaction.transaction_id,
                    input_orders_path,
                    aggregates_path,
                    log_dir,
                    events_path,
                    orders_path,
                    res_events_dir,
                    res_orders_dir,
                    lock_wait_seconds=1
                )

    assert "Timed out while waiting" in exc_info.value.message
