# -*- coding: utf-8 -*-

import pytest

from yt.wrapper import (
    ypath_join,
    common as yt_common,
)

from billing.log_tariffication.py.lib.constants import (
    LOG_TARIFF_META_ATTR,
    LOG_INTERVAL_KEY,
)
from billing.log_tariffication.py.jobs.core_tariff import historical_aggregates
from billing.log_tariffication.py.lib.schema import (
    HISTORICAL_AGGREGATES_TABLE_SCHEMA,
)

from billing.library.python.yt_utils.test_utils.utils import (
    create_subdirectory,
    create_yt_client,
)
from billing.log_tariffication.py.tests.utils import (
    create_historical_aggregates_dyntable,
    create_billable_log_table,
    patch_generate_run_id,
)
from billing.log_tariffication.py.tests.constants import (
    PREV_RUN_ID,
    CURR_RUN_ID,
    NEXT_RUN_ID,
    PREV_LOG_INTERVAL,
    CURR_LOG_INTERVAL,
    NEXT_LOG_INTERVAL,
)
from billing.library.python.logfeller_utils.log_interval import (
    LogInterval,
    Subinterval,
)


@pytest.fixture(name='aggregates_path')
def aggregates_path_fixture(yt_root):
    return ypath_join(yt_root, 'main_table')


@pytest.fixture(name='log_dir')
def log_dir_fixture(yt_client, yt_root):
    return create_subdirectory(yt_client, yt_root, 'input_log')


@pytest.fixture(name='offsets_dir')
def offsets_dir_fixture(yt_client, yt_root):
    return create_subdirectory(yt_client, yt_root, 'offsets_log')


@pytest.fixture(name='res_dir')
def res_dir_fixture(yt_client, yt_root):
    return create_subdirectory(yt_client, yt_root, 'result')


def create_offsets_table(yt_client, path, log_interval):
    yt_client.create(
        'table',
        path,
        attributes={
            LOG_TARIFF_META_ATTR: {LOG_INTERVAL_KEY: log_interval.to_meta()},
        }
    )


def create_res_table(yt_client, path, log_interval, data):
    yt_client.create(
        'table',
        path,
        attributes={
            LOG_TARIFF_META_ATTR: {LOG_INTERVAL_KEY: log_interval.to_meta()},
            'schema': HISTORICAL_AGGREGATES_TABLE_SCHEMA,
        }
    )

    yt_client.write_table(
        path,
        [
            {
                'ServiceID': sid,
                'ServiceOrderID': soid,
                'CurrencyID': cid,
                'BillableEventCostCur': float(bc),
                'EventTime': tm,
            }
            for idx, (sid, soid, cid, bc, tm) in enumerate(data)
        ]
    )


def get_result(yt_client, path):
    return {
        'meta': yt_client.get(ypath_join(path, '@' + LOG_TARIFF_META_ATTR)),
        'rows': list(yt_client.read_table(path)),
    }


def test_aggregation(yt_client, yql_client, aggregates_path, log_dir, offsets_dir, res_dir):
    create_historical_aggregates_dyntable(
        yt_client,
        aggregates_path,
        PREV_LOG_INTERVAL,
        [
            (7, 1, 1, 10, 1),
            (7, 1, 2, 66, 1),
            (7, 2, 1, 2, 1),
            (7, 3, 0, 50, 66),
        ]
    )

    create_billable_log_table(
        yt_client,
        ypath_join(log_dir, CURR_RUN_ID),
        CURR_LOG_INTERVAL,
        [
            (7, 1, 1, 1.1666, 0, 2),
            (7, 1, 1, 0.8334, 0, 3),
            (7, 2, 2, 14, 0, 0),
            (7, 3, 0, 666, 16000001, 2),
        ]
    )

    create_offsets_table(yt_client, ypath_join(offsets_dir, CURR_RUN_ID), CURR_LOG_INTERVAL)

    with yt_client.Transaction() as transaction:
        res = historical_aggregates.run_job(
            yt_client,
            yql_client,
            transaction.transaction_id,
            aggregates_path,
            log_dir,
            offsets_dir,
            res_dir,
        )

    return get_result(yt_client, res)


def test_multiple_rows_tables(yt_client, yql_client, aggregates_path, log_dir, offsets_dir, res_dir):
    create_historical_aggregates_dyntable(
        yt_client,
        aggregates_path,
        LogInterval([Subinterval('c', 't', 0, 0, 10)]),
        [
            (7, 1, 0, 14, 2),
            (7, 1, 1, 10, 1),
        ]
    )

    create_billable_log_table(
        yt_client,
        ypath_join(log_dir, PREV_RUN_ID),
        LogInterval([Subinterval('c', 't', 0, 0, 10)]),
        [
            (7, 1, 0, 666, 666, 3),
            (7, 1, 1, 666, 0, 2),
        ]
    )

    create_billable_log_table(
        yt_client,
        ypath_join(log_dir, CURR_RUN_ID),
        LogInterval([Subinterval('c', 't', 0, 10, 20)]),
        [
            (7, 1, 0, 666.0, 10, idx)
            for idx in range(50)
        ] + [
            (7, 1, 1, 0.0001, 0, idx)
            for idx in range(50)
        ]
    )

    create_billable_log_table(
        yt_client,
        ypath_join(log_dir, NEXT_RUN_ID),
        LogInterval([Subinterval('c', 't', 0, 20, 30)]),
        [
            (7, 1, 0, 666.0, 25, 50 + idx)
            for idx in range(50)
        ] + [
            (7, 1, 1, 0.0014, 0, 50 + idx)
            for idx in range(50)
        ]
    )

    create_offsets_table(
        yt_client,
        ypath_join(offsets_dir, CURR_RUN_ID),
        LogInterval([Subinterval('c', 't', 0, 10, 27)]),
    )

    with yt_client.Transaction() as transaction:
        res = historical_aggregates.run_job(
            yt_client,
            yql_client,
            transaction.transaction_id,
            aggregates_path,
            log_dir,
            offsets_dir,
            res_dir,
        )

    return get_result(yt_client, res)


@pytest.mark.parametrize(
    'init_interval, log_interval, offsets_interval',
    [
        pytest.param(
            LogInterval([
                Subinterval('c', 't', 0, 10, 15),
                Subinterval('c', 't', 1, 10, 15),
            ]),
            LogInterval([
                Subinterval('c', 't', 0, 10, 20),
                Subinterval('c', 't', 1, 10, 20),
            ]),
            LogInterval([
                Subinterval('c', 't', 0, 10, 16),
                Subinterval('c', 't', 1, 10, 16),
            ]),
            id='intersect_continue'
        ),
        pytest.param(
            LogInterval([
                Subinterval('c', 't', 0, 10, 15),
                Subinterval('c', 't', 1, 10, 15),
            ]),
            LogInterval([
                Subinterval('c', 't', 0, 10, 20),
                Subinterval('c', 't', 1, 10, 20),
            ]),
            LogInterval([
                Subinterval('c', 't', 0, 10, 16),
                Subinterval('c', 't', 1, 10, 15),
            ]),
            id='part_continue'
        ),
        pytest.param(
            CURR_LOG_INTERVAL,
            NEXT_LOG_INTERVAL,
            CURR_LOG_INTERVAL,
            id='already_processed'
        ),
        pytest.param(
            LogInterval([Subinterval('c', 't', 0, 10, 15)]),
            LogInterval([
                Subinterval('c', 't', 0, 15, 20),
                Subinterval('c', 't', 1, 0, 50),
            ]),
            LogInterval([
                Subinterval('c', 't', 0, 15, 19),
                Subinterval('c', 't', 1, 0, 20),
            ]),
            id='add_partitions'
        ),
        pytest.param(
            LogInterval([Subinterval('c', 't', 0, 10, 20)]),
            LogInterval([
                Subinterval('c', 't', 0, 20, 30),
                Subinterval('c', 't', 1, 0, 50),
            ]),
            LogInterval([
                Subinterval('c', 't', 0, 20, 20),
                Subinterval('c', 't', 1, 0, 0),
            ]),
            id='add_partitions_empty'
        ),
    ]
)
def test_intervals(yt_client, yql_client, aggregates_path, log_dir, offsets_dir, res_dir,
                   init_interval, log_interval, offsets_interval):
    create_historical_aggregates_dyntable(yt_client, aggregates_path, init_interval, [(7, 1, 1, 10, 1)])
    create_billable_log_table(yt_client, ypath_join(log_dir, CURR_RUN_ID), log_interval, [(7, 1, 1, 3, 0, 2)] * 10)
    create_offsets_table(yt_client, ypath_join(offsets_dir, CURR_RUN_ID), offsets_interval)

    with yt_client.Transaction() as transaction:
        res = historical_aggregates.run_job(
            yt_client,
            yql_client,
            transaction.transaction_id,
            aggregates_path,
            log_dir,
            offsets_dir,
            res_dir,
        )

    if res:
        return get_result(yt_client, res)
    else:
        return {'res': res}


def test_intervals_mismatch(yt_client, yql_client, aggregates_path, log_dir, offsets_dir, res_dir):
    create_historical_aggregates_dyntable(
        yt_client,
        aggregates_path,
        LogInterval([
            Subinterval('c', 't', 0, 10, 15),
            Subinterval('c', 't', 1, 10, 15),
        ]),
        [(7, 1, 1, 10, 1)]
    )
    create_offsets_table(
        yt_client,
        ypath_join(offsets_dir, CURR_RUN_ID),
        LogInterval([
            Subinterval('c', 't', 0, 10, 16),
            Subinterval('c', 't', 1, 10, 14),
        ])
    )

    with pytest.raises(AssertionError) as exc_info:
        with yt_client.Transaction() as transaction:
            historical_aggregates.run_job(
                yt_client,
                yql_client,
                transaction.transaction_id,

                aggregates_path,
                log_dir,
                offsets_dir,
                res_dir,
            )

    assert "Slices aren't comparable" in exc_info.value.args[0]


def test_add_partitions_misaligned(yt_client, yql_client, aggregates_path, log_dir, offsets_dir, res_dir):
    create_historical_aggregates_dyntable(
        yt_client,
        aggregates_path,
        LogInterval([Subinterval('c', 't', 0, 10, 15)]),
        [(7, 1, 1, 10, 1)]
    )

    create_billable_log_table(
        yt_client,
        ypath_join(log_dir, CURR_RUN_ID),
        LogInterval([
            Subinterval('c', 't', 0, 15, 20),
            Subinterval('c', 't', 1, 0, 50),
        ]),
        [(7, 1, 1, 3, 0, 2)]
    )

    create_offsets_table(
        yt_client,
        ypath_join(offsets_dir, PREV_RUN_ID),
        LogInterval([
            Subinterval('c', 't', 0, 10, 15),
        ])
    )
    create_offsets_table(
        yt_client,
        ypath_join(offsets_dir, CURR_RUN_ID),
        LogInterval([
            Subinterval('c', 't', 0, 15, 19),
            Subinterval('c', 't', 1, 1, 20),
        ])
    )

    with pytest.raises(AssertionError) as exc_info:
        with yt_client.Transaction() as transaction:
            historical_aggregates.run_job(
                yt_client,
                yql_client,
                transaction.transaction_id,
                aggregates_path,
                log_dir,
                offsets_dir,
                res_dir,
            )

    assert 'Intervals misaligned:' in exc_info.value.args[0]


def test_in_progress(yt_client, yql_client, aggregates_path, log_dir, offsets_dir, res_dir):
    create_historical_aggregates_dyntable(
        yt_client,
        aggregates_path,
        CURR_LOG_INTERVAL,
        [(7, 1, 1, 10, 1)],
        is_updating=True
    )

    create_res_table(yt_client, ypath_join(res_dir, PREV_RUN_ID), PREV_LOG_INTERVAL, [(7, 1, 1, 1, 1)])
    create_res_table(yt_client, ypath_join(res_dir, CURR_RUN_ID), CURR_LOG_INTERVAL, [(7, 1, 1, 2, 2)])
    create_res_table(yt_client, ypath_join(res_dir, NEXT_RUN_ID), NEXT_LOG_INTERVAL, [(7, 1, 1, 3, 3)])

    with yt_client.Transaction() as transaction:
        res = historical_aggregates.run_job(
            yt_client,
            yql_client,
            transaction.transaction_id,
            aggregates_path,
            log_dir,
            offsets_dir,
            res_dir,
        )

    assert res == ypath_join(res_dir, CURR_RUN_ID)
    return get_result(yt_client, res)


def test_in_progress_no_table(yt_client, yql_client, aggregates_path, log_dir, offsets_dir, res_dir):
    create_historical_aggregates_dyntable(
        yt_client,
        aggregates_path,
        CURR_LOG_INTERVAL,
        [(7, 1, 1, 10, 1)],
        is_updating=True
    )

    create_res_table(yt_client, ypath_join(res_dir, PREV_RUN_ID), PREV_LOG_INTERVAL, [(7, 1, 1, 1, 1)])
    create_res_table(yt_client, ypath_join(res_dir, CURR_RUN_ID), NEXT_LOG_INTERVAL, [(7, 1, 1, 3, 3)])

    with pytest.raises(AssertionError) as exc_info:
        with yt_client.Transaction() as transaction:
            historical_aggregates.run_job(
                yt_client,
                yql_client,
                transaction.transaction_id,
                aggregates_path,
                log_dir,
                offsets_dir,
                res_dir,
            )

    assert exc_info.value.args[0] == "No prepared data for update in progress"


def test_already_existing_table(yt_client, yql_client, aggregates_path, log_dir, offsets_dir, res_dir):
    create_historical_aggregates_dyntable(
        yt_client,
        aggregates_path,
        PREV_LOG_INTERVAL,
        [(7, 1, 1, 10, 1)]
    )

    create_offsets_table(yt_client, ypath_join(offsets_dir, 'some_random_table_name'), CURR_LOG_INTERVAL)

    create_res_table(yt_client, ypath_join(res_dir, PREV_RUN_ID), PREV_LOG_INTERVAL, [(7, 1, 1, 3, 1)])
    create_res_table(yt_client, ypath_join(res_dir, CURR_RUN_ID), CURR_LOG_INTERVAL, [(7, 1, 1, 2, 2)])
    create_res_table(yt_client, ypath_join(res_dir, NEXT_RUN_ID), NEXT_LOG_INTERVAL, [(7, 1, 1, 1, 3)])

    with yt_client.Transaction() as transaction:
        res = historical_aggregates.run_job(
            yt_client,
            yql_client,
            transaction.transaction_id,
            aggregates_path,
            log_dir,
            offsets_dir,
            res_dir,
        )

    assert res == ypath_join(res_dir, CURR_RUN_ID)
    return get_result(yt_client, res)


def test_run_id_duplicate(yt_client, yql_client, aggregates_path, log_dir, offsets_dir, res_dir):
    create_historical_aggregates_dyntable(yt_client, aggregates_path, PREV_LOG_INTERVAL, [(7, 1, 1, 10, 1)])

    create_offsets_table(yt_client, ypath_join(offsets_dir, CURR_RUN_ID), CURR_LOG_INTERVAL)
    create_res_table(yt_client, ypath_join(res_dir, CURR_RUN_ID), PREV_LOG_INTERVAL, [(7, 1, 1, 2, 2)])

    with patch_generate_run_id(CURR_RUN_ID):
        with pytest.raises(AssertionError) as exc_info:
            with yt_client.Transaction() as transaction:
                historical_aggregates.run_job(
                    yt_client,
                    yql_client,
                    transaction.transaction_id,
                    aggregates_path,
                    log_dir,
                    offsets_dir,
                    res_dir,
                )

    assert 'Generated path is already used' in exc_info.value.args[0]


def test_from_lb_meta(yt_client, yql_client, aggregates_path, log_dir, offsets_dir, res_dir):
    create_historical_aggregates_dyntable(yt_client, aggregates_path, PREV_LOG_INTERVAL, [(7, 1, 1, 666, 1)])
    create_billable_log_table(yt_client, ypath_join(log_dir, CURR_RUN_ID), CURR_LOG_INTERVAL, [(7, 1, 1, 14, 0, 2)])
    create_billable_log_table(yt_client, ypath_join(offsets_dir, CURR_RUN_ID), CURR_LOG_INTERVAL, [])

    with yt_client.Transaction() as transaction:
        res = historical_aggregates.run_job(
            yt_client,
            yql_client,
            transaction.transaction_id,
            aggregates_path,
            log_dir,
            offsets_dir,
            res_dir,
        )

    return get_result(yt_client, res)


def test_main_lock(yt_client, yql_client, yt_root, aggregates_path, log_dir, offsets_dir, res_dir):
    create_historical_aggregates_dyntable(yt_client, aggregates_path, PREV_LOG_INTERVAL, [(7, 1, 1, 10, 1)])

    alt_yt_client = create_yt_client()
    with alt_yt_client.Transaction():
        alt_yt_client.lock(
            yt_root,
            mode='shared',
            child_key='main_table'
        )

        with pytest.raises(yt_common.YtError) as exc_info:
            with yt_client.Transaction() as transaction:
                historical_aggregates.run_job(
                    yt_client,
                    yql_client,
                    transaction.transaction_id,
                    aggregates_path,
                    log_dir,
                    offsets_dir,
                    res_dir,
                    lock_wait_seconds=1
                )

    assert 'Timed out while waiting' in exc_info.value.message


def test_meta_lock(yt_client, yql_client, aggregates_path, log_dir, offsets_dir, res_dir):
    create_historical_aggregates_dyntable(yt_client, aggregates_path, PREV_LOG_INTERVAL, [(7, 1, 1, 10, 1)])

    alt_yt_client = create_yt_client()
    with alt_yt_client.Transaction():
        alt_yt_client.lock(aggregates_path)

        with pytest.raises(yt_common.YtError) as exc_info:
            with yt_client.Transaction() as transaction:
                historical_aggregates.run_job(
                    yt_client,
                    yql_client,
                    transaction.transaction_id,
                    aggregates_path,
                    log_dir,
                    offsets_dir,
                    res_dir,
                    lock_wait_seconds=1
                )

    assert 'Timed out while waiting' in exc_info.value.message
