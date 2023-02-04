import pytest

from yt.wrapper import (
    ypath_join,
    common as yt_common,
)

from billing.log_tariffication.py.lib.constants import (
    LOG_TARIFF_META_ATTR,
    LOG_INTERVAL_KEY,
    CORRECTIONS_LOG_INTERVAL_KEY,
    RUN_ID_KEY,
)
from billing.log_tariffication.py.jobs.core_tariff import get_historical_migration

from billing.log_tariffication.py.tests.utils import (
    create_historical_aggregates_dyntable,
    create_billable_log_table,
)
from billing.library.python.yt_utils.test_utils.utils import (
    create_subdirectory,
    create_yt_client,
)
from billing.log_tariffication.py.tests.constants import (
    PREV_RUN_ID,
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
    {'name': 'EffectiveServiceOrderID', 'type': 'int64'},
]


EVENTS_TABLE_SCHEMA = [
    {'name': 'ServiceID', 'type': 'int64', 'sort_order': 'ascending'},
    {'name': 'ServiceOrderID', 'type': 'int64', 'sort_order': 'ascending'},
    {'name': 'EffectiveServiceOrderID', 'type': 'int64', 'sort_order': 'ascending'},
    {'name': 'CurrencyID', 'type': 'uint64', 'sort_order': 'ascending'},
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


@pytest.fixture(name='res_events_dir')
def res_events_dir_fixture(yt_client, yt_root):
    return create_subdirectory(yt_client, yt_root, 'res_events')


def create_input_orders_table(yt_client, path, data):
    yt_client.create('table', path, attributes={'schema': INPUT_ORDERS_TABLE_SCHEMA})

    yt_client.write_table(
        path,
        [
            {'ServiceID': sid, 'ServiceOrderID': soid, 'EffectiveServiceOrderID': gsoid}
            for sid, soid, gsoid in data
        ]
    )


def create_events_table(yt_client, path, log_interval, corrections_interval, data, run_id=None):
    yt_client.create(
        'table',
        path,
        attributes={
            LOG_TARIFF_META_ATTR: {
                LOG_INTERVAL_KEY: log_interval.to_meta(),
                CORRECTIONS_LOG_INTERVAL_KEY: corrections_interval.to_meta(),
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
                'ServiceOrderID': soid,
                'EffectiveServiceOrderID': esoid,
                'CurrencyID': cid,
                'EventTime': idx,
                'BillableEventCostCur': float((1 + idx) * 6.66),
            }
            for idx, (sid, soid, esoid, cid) in enumerate(data)
        ]
    )


def get_result(yt_client, events_path):
    return {
        'events': {
            'meta': yt_client.get(ypath_join(events_path, '@' + LOG_TARIFF_META_ATTR))[LOG_INTERVAL_KEY],
            'correction_meta': yt_client.get(ypath_join(events_path, '@' + LOG_TARIFF_META_ATTR))[CORRECTIONS_LOG_INTERVAL_KEY],
            'rows': list(yt_client.read_table(events_path)),
        },
    }


def test_base(yt_client, yql_client, input_orders_path, aggregates_path, log_dir, res_events_dir):
    correction_interval = LogInterval([Subinterval('a', 'b', 0, 1, 2)])

    create_input_orders_table(
        yt_client,
        input_orders_path,
        [
            (7, 1, 10),
            (7, 2, 10),
            (7, 3, 20),
            # Надо чтобы ОС доехали обратно до oltp даже есть у них нет откруток
            (7, 8, 30),  # Без откруток
            (7, 40, 40),  # заказ без ОС
        ]
    )

    create_historical_aggregates_dyntable(
        yt_client,
        aggregates_path,
        LogInterval([Subinterval('c', 't', 0, 10, 20)]),
        [
            (7, 1, 1, 10, 1),
            (7, 1, 2, 66, 1),
            (7, 2, 1, 2, 1),
            (7, 3, 0, 50, 66),
            (7, 4, 1, 666, 7),  # этот нам не нужен
            (7, 40, 3, 14, 41),
        ]
    )

    create_billable_log_table(
        yt_client,
        ypath_join(log_dir, CURR_RUN_ID),
        LogInterval([Subinterval('c', 't', 0, 20, 30)]),
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
            (7, 40, 3, 0.0666, 1, 1),
        ],
    )

    create_events_table(  # table doesn't matter because of prev corrections interval
        yt_client,
        ypath_join(res_events_dir, PREV_RUN_ID),
        LogInterval([Subinterval('c', 't', 0, 20, 30)]),
        LogInterval([Subinterval('a', 'b', 0, 0, 1)]),
        [(7, 1, 10, 1), (7, 2, 10, 1)],
        run_id=PREV_RUN_ID,
    )

    with yt_client.Transaction() as transaction:
        res_events_path = get_historical_migration.run_job(
            yt_client,
            yql_client,
            transaction.transaction_id,
            {
                RUN_ID_KEY: CURR_RUN_ID,
                LOG_INTERVAL_KEY: LogInterval([Subinterval('c', 't', 0, 20, 30)]).to_meta(),
                CORRECTIONS_LOG_INTERVAL_KEY: correction_interval.to_meta(),
            },
            input_orders_path,
            aggregates_path,
            log_dir,
            res_events_dir,
        )

    return get_result(yt_client, res_events_path)


def test_wo_logs(yt_client, yql_client, input_orders_path, aggregates_path, log_dir, res_events_dir):
    correction_interval = LogInterval([Subinterval('a', 'b', 0, 1, 2)])

    create_input_orders_table(
        yt_client,
        input_orders_path,
        [(7, 1, 10)],
    )

    create_historical_aggregates_dyntable(
        yt_client,
        aggregates_path,
        LogInterval([Subinterval('c', 't', 0, 20, 30)]),
        [(7, 1, 1, 10, 1)]
    )

    create_billable_log_table(
        yt_client,
        ypath_join(log_dir, CURR_RUN_ID),
        LogInterval([Subinterval('c', 't', 0, 10, 20)]),
        [(7, 1, 1, 2, 0, 2)],
    )

    with yt_client.Transaction() as transaction:
        res_events_path = get_historical_migration.run_job(
            yt_client,
            yql_client,
            transaction.transaction_id,
            {
                RUN_ID_KEY: CURR_RUN_ID,
                LOG_INTERVAL_KEY: LogInterval([Subinterval('c', 't', 0, 20, 30)]).to_meta(),
                CORRECTIONS_LOG_INTERVAL_KEY: correction_interval.to_meta(),
            },
            input_orders_path,
            aggregates_path,
            log_dir,
            res_events_dir,
        )

    return get_result(yt_client, res_events_path)


def test_no_log(yt_client, yql_client, input_orders_path, aggregates_path, log_dir, res_events_dir):
    log_interval = LogInterval([Subinterval('c', 't', 0, 0, 10)])
    correction_interval = LogInterval([Subinterval('a', 'b', 0, 1, 2)])

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
        log_interval,
        [
            (7, 1, 0, 10, 1),
            (7, 1, 2, 66, 1),
            (7, 2, 1, 2, 1),
        ]
    )

    with yt_client.Transaction() as transaction:
        res_events_path = get_historical_migration.run_job(
            yt_client,
            yql_client,
            transaction.transaction_id,
            {
                RUN_ID_KEY: CURR_RUN_ID,
                LOG_INTERVAL_KEY: log_interval.to_meta(),
                CORRECTIONS_LOG_INTERVAL_KEY: correction_interval.to_meta(),
            },
            input_orders_path,
            aggregates_path,
            log_dir,
            res_events_dir,
        )

    return get_result(yt_client, res_events_path)


def test_log_intervals_mismatch(yt_client, yql_client, input_orders_path, aggregates_path,  res_events_dir, log_dir):
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

    with pytest.raises(AssertionError) as exc_info:
        with yt_client.Transaction() as transaction:
            get_historical_migration.run_job(
                yt_client,
                yql_client,
                transaction.transaction_id,
                {
                    RUN_ID_KEY: CURR_RUN_ID,
                    LOG_INTERVAL_KEY: LogInterval([
                        Subinterval('c', 't', 0, 5, 9),
                        Subinterval('c', 't', 1, 8, 11),
                    ]).to_meta(),
                    CORRECTIONS_LOG_INTERVAL_KEY: LogInterval([Subinterval('a', 'b', 0, 1, 2)]).to_meta(),
                },
                input_orders_path,
                aggregates_path,
                log_dir,
                res_events_dir,
            )

    assert "Tables are broken!" in exc_info.value.args[0]


def test_dyntable_in_progress(yt_client, yql_client, input_orders_path, aggregates_path, log_dir, res_events_dir):
    log_interval = LogInterval([Subinterval('c', 't', 0, 0, 10)])
    correction_interval = LogInterval([Subinterval('a', 'b', 0, 1, 2)])
    create_input_orders_table(yt_client, input_orders_path, [(7, 1, 10)])

    create_historical_aggregates_dyntable(
        yt_client,
        aggregates_path,
        LogInterval([Subinterval('c', 't', 0, 0, 10)]),
        [(7, 1, 1, 666, 1)],
        is_updating=True
    )

    with pytest.raises(AssertionError) as exc_info:
        with yt_client.Transaction() as transaction:
            get_historical_migration.run_job(
                yt_client,
                yql_client,
                transaction.transaction_id,
                {
                    RUN_ID_KEY: NEXT_RUN_ID,
                    LOG_INTERVAL_KEY: log_interval.to_meta(),
                    CORRECTIONS_LOG_INTERVAL_KEY: correction_interval.to_meta(),
                },
                input_orders_path,
                aggregates_path,
                log_dir,
                res_events_dir,
            )

    assert "dyntable {} is updating!".format(aggregates_path) in exc_info.value.args[0]


def test_dyntable_lock(yt_client, yql_client, input_orders_path, aggregates_path, log_dir, res_events_dir):
    log_interval = LogInterval([Subinterval('c', 't', 0, 0, 10)])
    correction_interval = LogInterval([Subinterval('a', 'b', 0, 1, 2)])
    create_input_orders_table(yt_client, input_orders_path, [(7, 1, 10)])

    create_historical_aggregates_dyntable(
        yt_client,
        aggregates_path,
        LogInterval([Subinterval('c', 't', 0, 0, 10)]),
        [(7, 1, 1, 666, 1)]
    )

    alt_yt_client = create_yt_client()
    with alt_yt_client.Transaction():
        alt_yt_client.lock(aggregates_path)

        with pytest.raises(yt_common.YtError) as exc_info:
            with yt_client.Transaction() as transaction:
                get_historical_migration.run_job(
                    yt_client,
                    yql_client,
                    transaction.transaction_id,
                    {
                        RUN_ID_KEY: NEXT_RUN_ID,
                        LOG_INTERVAL_KEY: log_interval.to_meta(),
                        CORRECTIONS_LOG_INTERVAL_KEY: correction_interval.to_meta(),
                    },
                    input_orders_path,
                    aggregates_path,
                    log_dir,
                    res_events_dir,
                    lock_wait_seconds=0.1,
                )

    assert "Timed out while waiting" in exc_info.value.message


def test_result_exists(yt_client, yql_client, input_orders_path, aggregates_path, log_dir, res_events_dir):
    correction_interval = LogInterval([Subinterval('a', 'b', 0, 1, 2)])
    interval = LogInterval([Subinterval('c', 't', 0, 20, 30)])

    create_input_orders_table(
        yt_client,
        input_orders_path,
        [(7, 1, 10)]
    )
    create_events_table(
        yt_client,
        ypath_join(res_events_dir, CURR_RUN_ID),
        interval,
        correction_interval,
        [(7, 1, 10, 1), (7, 2, 10, 1)],
        run_id=CURR_RUN_ID,
    )

    with yt_client.Transaction() as transaction:
        res_events_path = get_historical_migration.run_job(
            yt_client,
            yql_client,
            transaction.transaction_id,
            {
                RUN_ID_KEY: CURR_RUN_ID,
                LOG_INTERVAL_KEY: interval.to_meta(),
                CORRECTIONS_LOG_INTERVAL_KEY: correction_interval.to_meta(),
            },
            input_orders_path,
            aggregates_path,
            log_dir,
            res_events_dir,
        )

    assert res_events_path == ypath_join(res_events_dir, CURR_RUN_ID)
    return get_result(yt_client, res_events_path)


def test_result_exists_error(yt_client, yql_client, input_orders_path, aggregates_path, log_dir, res_events_dir):
    correction_interval = LogInterval([Subinterval('a', 'b', 0, 1, 2)])
    prev_interval = LogInterval([Subinterval('c', 't', 0, 10, 20)])
    interval = LogInterval([Subinterval('c', 't', 0, 20, 30)])

    create_input_orders_table(
        yt_client,
        input_orders_path,
        [(7, 1, 10)]
    )
    create_events_table(
        yt_client,
        ypath_join(res_events_dir, CURR_RUN_ID),
        prev_interval,
        correction_interval,
        [(7, 1, 10, 1), (7, 2, 10, 1)],
        run_id=CURR_RUN_ID,
    )

    with pytest.raises(AssertionError) as exc_info:
        with yt_client.Transaction() as transaction:
            get_historical_migration.run_job(
                yt_client,
                yql_client,
                transaction.transaction_id,
                {
                    RUN_ID_KEY: CURR_RUN_ID,
                    LOG_INTERVAL_KEY: interval.to_meta(),
                    CORRECTIONS_LOG_INTERVAL_KEY: correction_interval.to_meta(),
                },
                input_orders_path,
                aggregates_path,
                log_dir,
                res_events_dir,
            )
    assert "Bad meta in current table" in exc_info.value.args[0]
