# coding=utf-8

from unittest import mock

import pytest
from hamcrest import assert_that, contains_inanyorder

from yt.wrapper import ypath_join, TablePath

from billing.library.python.logmeta_utils.meta import (
    get_log_tariff_meta,
)
from billing.log_tariffication.py.lib.constants import (
    LOG_TARIFF_META_ATTR,
)
from billing.library.python.common_utils.wait import (
    WaitTimeoutExceeded,
)
from billing.library.python.logmeta_utils.meta import (
    wait_logs,
)
from billing.log_tariffication.py.jobs.common import prepare_events
from billing.library.python.logfeller_utils.log_interval import (
    LogInterval,
    Subinterval,
    LB_META_ATTR,
    get_next_stream_table_name,
)
from billing.library.python.logfeller_utils.tests.utils import (
    generate_stream_log_table_name
)
from billing.library.python.yt_utils.test_utils.utils import (
    create_subdirectory,
)
from billing.log_tariffication.py.tests.utils import (
    check_node_is_locked,
)
from billing.log_tariffication.py.tests.constants import (
    PREV_RUN_ID,
    CURR_RUN_ID,
    NEXT_RUN_ID,
    PREV_LOG_INTERVAL,
    CURR_LOG_INTERVAL,
    PREV_LOG_TARIFF_META,
    CURR_LOG_TARIFF_META,
    STREAM_LOG_TABLE_SCHEMA,
)


STRIPPED_LOG_COLUMNS = ['OrderID', 'EventTime', 'Bucks']
SORT_COLUMNS = ['OrderID', 'EventTime']


@pytest.fixture(name='time_mock')
def time_mock_fixture():
    with mock.patch('billing.library.python.common_utils.wait.time') as time_mock:
        time_mock.time = mock.Mock()
        time_mock.sleep = mock.Mock()
        yield time_mock


@pytest.fixture(name='full_log_dir')
def full_log_dir_fixture(yt_client, yt_root):
    return create_subdirectory(yt_client, yt_root, 'full_log_dir')


@pytest.fixture(name='full_log_path')
def full_log_path_fixture(full_log_dir):
    return ypath_join(full_log_dir, CURR_RUN_ID)


@pytest.fixture(name='prev_run_stripped_log_path')
def prev_run_stripped_log_path_fixture(stripped_log_dir):
    return ypath_join(stripped_log_dir, PREV_RUN_ID)


@pytest.fixture(name='prev_run_full_log_path')
def prev_run_full_log_path_fixture(full_log_dir):
    return ypath_join(full_log_dir, PREV_RUN_ID)


@pytest.fixture(name='next_run_stripped_log_path')
def next_run_stripped_log_path_fixture(stripped_log_dir):
    return ypath_join(stripped_log_dir, NEXT_RUN_ID)


@pytest.fixture(name='next_run_full_log_path')
def next_run_full_log_path_fixture(full_log_dir):
    return ypath_join(full_log_dir, NEXT_RUN_ID)


@pytest.fixture(name='run_job_params_without_transaction')
def run_job_params_without_transaction_fixture(
    yt_client, yql_client, stream_log_dir,
    stripped_log_dir, full_log_dir
):
    return dict(
        yt_client=yt_client,
        yql_client=yql_client,
        log_tariff_meta=CURR_LOG_TARIFF_META,
        input_log_dir=stream_log_dir,
        res_stripped_log_dir=stripped_log_dir,
        res_full_log_dir=full_log_dir,
        stripped_log_columns=STRIPPED_LOG_COLUMNS,
        sort_columns=SORT_COLUMNS,
    )


@pytest.fixture(name='run_job_params')
def run_job_params_fixture(run_job_params_without_transaction, yt_transaction):
    params = run_job_params_without_transaction.copy()
    params['transaction_id'] = yt_transaction.transaction_id
    return params


@pytest.mark.parametrize(
    ['stripped_meta', 'full_meta'],
    [
        pytest.param(
            dict(
                log_interval=PREV_LOG_INTERVAL.to_meta(),
                run_id=CURR_RUN_ID
            ),
            CURR_LOG_TARIFF_META,
            id="bad interval in stripped table"
        ),
        pytest.param(
            CURR_LOG_TARIFF_META,
            dict(
                log_interval=CURR_LOG_INTERVAL.to_meta(),
                run_id=PREV_RUN_ID
            ),
            id="bad run id in full table"
        )
    ]
)
def test_curr_tables_have_different_meta(
    yt_client, run_job_params, stripped_log_path, full_log_path,
    stripped_meta, full_meta
):
    yt_client.create('table', full_log_path, attributes={
        LOG_TARIFF_META_ATTR: full_meta
    })
    yt_client.create('table', stripped_log_path, attributes={
        LOG_TARIFF_META_ATTR: stripped_meta
    })
    with pytest.raises(AssertionError):
        prepare_events.run_job(**run_job_params)


@pytest.mark.parametrize(
    ['with_full', 'full_meta'],
    [
        pytest.param(True, CURR_LOG_TARIFF_META, id='with full'),
        pytest.param(False, PREV_LOG_TARIFF_META, id='bad full ignored'),
        pytest.param(False, None, id='no full'),
    ]
)
def test_already_done(yt_client, run_job_params, stripped_log_path, full_log_path, with_full, full_meta):
    yt_client.create('table', stripped_log_path, attributes={
        LOG_TARIFF_META_ATTR: CURR_LOG_TARIFF_META
    })
    if full_meta:
        yt_client.create('table', full_log_path, attributes={
            LOG_TARIFF_META_ATTR: full_meta
        })
    if not with_full:
        run_job_params['res_full_log_dir'] = None
    actual_stripped_log_path, actual_full_log_path = prepare_events.run_job(**run_job_params)
    assert actual_stripped_log_path == stripped_log_path

    if with_full:
        assert actual_full_log_path == full_log_path
    else:
        assert actual_full_log_path is None


@pytest.mark.parametrize(
    ['run_id', 'stripped_log_path_fixture_name', 'log_interval'],
    [
        pytest.param(
            PREV_RUN_ID,
            'prev_run_stripped_log_path',
            LogInterval([Subinterval('c1', 't1', 0, 5, 15)]),
            id="prev run"
        ),
        pytest.param(
            NEXT_RUN_ID,
            'next_run_stripped_log_path',
            LogInterval([Subinterval('c1', 't1', 0, 15, 25),
                         Subinterval('c1', 't1', 1, 20, 30)]),
            id="next run"
        ),
    ]
)
def test_interval_overlaps_with_adjacent_tables(
    yt_client, run_job_params, run_id, stripped_log_path_fixture_name,
    log_interval, get_fixture
):
    log_tariff_meta = dict(
        log_interval=log_interval.to_meta(),
        run_id=run_id
    )
    yt_client.create('table', get_fixture(stripped_log_path_fixture_name), attributes={
        LOG_TARIFF_META_ATTR: log_tariff_meta
    })
    with pytest.raises(AssertionError):
        prepare_events.run_job(**run_job_params)


@pytest.mark.parametrize(
    ['prev_tables_interval', 'next_tables_interval', 'with_full'],
    [
        pytest.param(
            # intervals are adjacent
            LogInterval([
                Subinterval('c1', 't1', 0, 10, 10),
                Subinterval('c1', 't1', 1, 10, 15),
            ]),
            LogInterval([
                Subinterval('c1', 't1', 0, 20, 35),
                Subinterval('c1', 't1', 1, 25, 30),
            ]),
            True,
            id='Adjacent other results, with full'
        ),
        pytest.param(
            # intervals are not adjacent
            LogInterval([
                Subinterval('c1', 't1', 0, 5, 8),
                Subinterval('c1', 't1', 1, 10, 12),
            ]),
            LogInterval([
                Subinterval('c1', 't1', 0, 23, 35),
                Subinterval('c1', 't1', 1, 27, 30),
            ]),
            False,
            id='Not adjacent other results, without full'
        ),
    ]
)
def test_productive_run(yt_client, run_job_params_without_transaction,
                        stream_log_dir, stripped_log_dir,
                        stripped_log_path, full_log_path, full_log_dir,
                        prev_run_stripped_log_path, prev_run_full_log_path,
                        next_run_stripped_log_path, next_run_full_log_path,
                        prev_tables_interval, next_tables_interval,
                        with_full):
    """
    Выполняем рядовой продуктивный (в смысле, мы обрабатываем новые логи) запуск.
    """

    # Create previous tables.
    prev_tables_attributes = {
        LOG_TARIFF_META_ATTR: dict(
            log_interval=prev_tables_interval.to_meta(),
            run_id=PREV_RUN_ID
        )
    }
    yt_client.create('table', prev_run_stripped_log_path,
                     attributes=prev_tables_attributes)
    yt_client.create('table', prev_run_full_log_path,
                     attributes=prev_tables_attributes)

    # Create next tables
    next_tables_attributes = {
        LOG_TARIFF_META_ATTR: dict(
            log_interval=next_tables_interval.to_meta(),
            run_id=NEXT_RUN_ID
        )
    }
    yt_client.create('table', next_run_stripped_log_path,
                     attributes=next_tables_attributes)
    yt_client.create('table', next_run_full_log_path,
                     attributes=next_tables_attributes)

    # Список кортежей из двух элементов, описывающих таблицу с сырыми логами:
    # 1. Интервал таблицы
    # 2. Список кортежей:
    #    1. Запись таблицы, колонки как в STREAM_LOG_TABLE_SCHEMA
    #    2. expected - ожидаем ли мы увидеть эту запись в результатах (попадает ли она в CURR_LOG_INTERVAL)
    log_tables = [
        (  # First table, does not intersects with log_interval_meta
            LogInterval([
                Subinterval('c1', 't1', 0, 4, 8),
                Subinterval('c1', 't1', 1, 6, 9),
            ]),
            [
                ((1, 1587456546, 10, 'c1', 't1', 0, 4, 0), 0),
                ((2, 1587456548, 50, 'c1', 't1', 1, 7, 9), 0),
            ]
        ),
        (
            LogInterval([
                Subinterval('c1', 't1', 0, 8, 15),
                Subinterval('c1', 't1', 1, 9, 15),
            ]),
            [
                ((1, 1587456550, 20, 'c1', 't1', 0, 10, 9), 1),
                ((2, 1587456551, 30, 'c1', 't1', 0, 11, 7), 1),
                ((2, 1587456552, 80, 'c1', 't1', 1, 10, 5), 0),
                ((1, 1587456554, 40, 'c1', 't1', 1, 11, 7), 0),
            ]
        ),
        (
            LogInterval([
                Subinterval('c1', 't1', 0, 15, 25),
                Subinterval('c1', 't1', 1, 15, 25),
            ]),
            [
                ((1, 1587456560, 40, 'c1', 't1', 0, 16, 9), 1),
                ((2, 1587456562, 30, 'c1', 't1', 0, 17, 7), 1),
                ((2, 1587456566, 90, 'c1', 't1', 0, 24, 5), 0),
                ((2, 1587456565, 20, 'c1', 't1', 1, 16, 5), 1),
                ((1, 1587456565, 10, 'c1', 't1', 1, 24, 7), 1),
            ]
        ),
        (  # Last table, does not intersects with log_interval_meta
            LogInterval([
                Subinterval('c1', 't1', 0, 25, 28),
                Subinterval('c1', 't1', 1, 25, 29),
            ]),
            [
                ((1, 1587456571, 40, 'c1', 't1', 0, 27, 0), 0),
                ((1, 1587456576, 70, 'c1', 't1', 1, 25, 9), 0),
            ]
        ),
    ]

    log_table_columns = [i['name'] for i in STREAM_LOG_TABLE_SCHEMA]
    expected_stripped_rows = []
    expected_full_rows = []
    log_table_name = generate_stream_log_table_name()
    for table_log_interval, rows in log_tables:
        table_path = TablePath(
            ypath_join(stream_log_dir, log_table_name),
            schema=STREAM_LOG_TABLE_SCHEMA
        )
        log_table_name = get_next_stream_table_name(log_table_name)
        yt_client.create('table', table_path, attributes={
            LB_META_ATTR: table_log_interval.to_meta(),
            'schema': STREAM_LOG_TABLE_SCHEMA
        })
        table_rows = []
        for row, expected in rows:
            lb_uid = '@'.join(map(str, row[3:8]))
            row = dict(zip(log_table_columns, row))
            table_rows.append(row)
            if expected:
                full_row = row.copy()
                full_row['LBMessageUID'] = lb_uid
                stripped_row = {key: value for key, value in row.items() if key in STRIPPED_LOG_COLUMNS}
                stripped_row['LBMessageUID'] = lb_uid
                expected_full_rows.append(full_row)
                expected_stripped_rows.append(stripped_row)
        yt_client.write_table(table_path, table_rows)

    # Existing full table with inconsistent schema must not be a problem.
    yt_client.create(
        'table', full_log_path, attributes={'schema': [
            {'name': 'very_bad', 'type': 'uint64'},
        ]}
    )

    if not with_full:
        # There must not be an attempt to write to this directory.
        yt_client.remove(full_log_dir, recursive=True)
        run_job_params_without_transaction['res_full_log_dir'] = None

    with yt_client.Transaction(ping=False) as transaction:
        actual_stripped_log_path, actual_full_log_path = prepare_events.run_job(
            lock_wait_seconds=1,
            transaction_id=transaction.transaction_id,
            **run_job_params_without_transaction
        )

        check_node_is_locked(stripped_log_dir)

    assert actual_stripped_log_path == stripped_log_path

    if with_full:
        assert actual_full_log_path == full_log_path
    else:
        assert actual_full_log_path is None

    assert get_log_tariff_meta(yt_client, stripped_log_path) == CURR_LOG_TARIFF_META

    if with_full:
        assert get_log_tariff_meta(yt_client, full_log_path) == CURR_LOG_TARIFF_META

    assert yt_client.get(ypath_join(stripped_log_path, '@sorted_by')) == SORT_COLUMNS

    assert_that(list(yt_client.read_table(stripped_log_path)),
                contains_inanyorder(*expected_stripped_rows))

    if with_full:
        assert_that(list(yt_client.read_table(full_log_path)),
                    contains_inanyorder(*expected_full_rows))


def test_wait_timeout(yt_client, time_mock, stream_log_dir):
    log_wait_minutes = 1
    time_mock.time.side_effect = [
        0,
        1,  # before deadline
        log_wait_minutes * 60 * 2  # past deadline
    ]
    with pytest.raises(WaitTimeoutExceeded):
        wait_logs(
            yt_client, CURR_LOG_INTERVAL.to_meta(),
            stream_log_dir, log_wait_minutes
        )


def test_wait_succeeded(yt_client, time_mock, stream_log_dir):
    log_wait_minutes = 1

    def time_side_effect():
        # empty directory
        yield 0  # deadline calculation

        # list call #1

        # after attempt #1
        # covers only part of CURR_LOG_INTERVAL
        log_table_name = generate_stream_log_table_name()
        interval = LogInterval([
            Subinterval('c1', 't1', 0, 10, 20),
        ])
        yt_client.create('table', ypath_join(stream_log_dir, log_table_name),
                         attributes={LB_META_ATTR: interval.to_meta()})
        yield 1

        # list call #2

        # after attempt #2
        # Now tables fully cover interval
        log_table_name = get_next_stream_table_name(log_table_name)
        interval = LogInterval([
            Subinterval('c1', 't1', 0, 20, 25),
            Subinterval('c1', 't1', 1, 15, 25),
        ])
        yt_client.create('table', ypath_join(stream_log_dir, log_table_name),
                         attributes={LB_META_ATTR: interval.to_meta()})
        yield 2

        # list call #3 (must succeed)

        yield log_wait_minutes * 60 * 2  # past deadline, must not reach it

    time_mock.time.side_effect = time_side_effect()
    wait_logs(
        yt_client, CURR_LOG_INTERVAL.to_meta(),
        stream_log_dir, log_wait_minutes
    )


def test_wait_succeeded_immediately(yt_client, time_mock, stream_log_dir):
    log_wait_minutes = 1
    time_mock.time.side_effect = [
        1,  # deadline calculation
        log_wait_minutes * 60 * 2  # past deadline, must not reach it
    ]
    table_name = ypath_join(stream_log_dir, generate_stream_log_table_name())

    interval_meta = LogInterval([
        Subinterval('c1', 't1', 0, 20, 25),
        Subinterval('c1', 't1', 1, 30, 35),
    ]).to_meta()
    yt_client.create('table', table_name, attributes={LB_META_ATTR: interval_meta})

    wait_logs(
        yt_client, CURR_LOG_INTERVAL.to_meta(),
        stream_log_dir, log_wait_minutes
    )
    time_mock.sleep.assert_not_called()

    # Мы сравниваем только концы отрезков, то есть ждем, когда лог догонит (или перегонит) конец отрезка целевого лога.
    yt_client.remove(table_name)

    interval_meta = LogInterval([
        Subinterval('c1', 't1', 0, 20, 20),
        Subinterval('c1', 't1', 1, 30, 30),
    ]).to_meta()
    yt_client.create('table', table_name, attributes={LB_META_ATTR: interval_meta})

    wait_logs(
        yt_client, interval_meta,
        stream_log_dir, log_wait_minutes
    )
    time_mock.sleep.assert_not_called()
