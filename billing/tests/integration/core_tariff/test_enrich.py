# coding=utf-8

import itertools
import mock
import contextlib

import pytest

from yt.wrapper import ypath_join

from billing.library.python.logmeta_utils.meta import (
    RUN_ID_KEY,
    LOG_TARIFF_META_ATTR,
    LOG_INTERVAL_KEY,
)
from billing.log_tariffication.py.jobs.core_tariff import enrich_log
from billing.log_tariffication.py.lib.constants import (
    CORRECTIONS_LOG_INTERVAL_KEY,
)
from billing.library.python.logfeller_utils.log_interval import (
    LB_META_ATTR
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
from billing.library.python.logfeller_utils.tests.utils import (
    mk_interval,
)
from billing.log_tariffication.py.tests.constants import (
    CURR_RUN_ID,
    PREV_RUN_ID,
    LOGFELLER_TABLE_SCHEMA,
)


STREAM_LOG_TABLE_SCHEMA = [
    {'name': 'LogData', 'type': 'uint64'},
    {'name': 'AddLogData', 'type': 'uint64'},
    {'name': 'AddLogOtherData', 'type': 'uint64'},
] + LOGFELLER_TABLE_SCHEMA


STREAM_TARIFFED_TABLE_SCHEMA = [
    {'name': 'LBMessageUID', 'type': 'string'},
    {'name': 'TariffedData', 'type': 'uint64'},
]

STREAM_STRIPPED_LOG_TABLE_SCHEMA = [
    {'name': 'LBMessageUID', 'type': 'string'},
    {'name': 'LogData', 'type': 'uint64'},
]

STREAM_CORRECTIONS_TABLE_SCHEMA = [
    {'name': 'LBMessageUID', 'type': 'string'},
    {'name': 'LogData', 'type': 'uint64'},
    {'name': 'AddLogData', 'type': 'uint64'},
]

PROCESSED_LOG_TABLE_SCHEMA = [
    {'name': 'ID', 'type': 'uint64'},
    {'name': 'LogData', 'type': 'uint64'},
    {'name': 'AddLogData', 'type': 'uint64'},
    {'name': 'AddLogOtherData', 'type': 'uint64'},
]

PROCESSED_TARIFFED_TABLE_SCHEMA = [
    {'name': 'ID', 'type': 'uint64'},
    {'name': 'TariffedData', 'type': 'uint64'},
]

PROCESSED_STRIPPED_LOG_TABLE_SCHEMA = [
    {'name': 'ID', 'type': 'uint64'},
    {'name': 'LogData', 'type': 'uint64'},
]

PROCESSED_CORRECTIONS_TABLE_SCHEMA = [
    {'name': 'ID', 'type': 'uint64'},
    {'name': 'LogData', 'type': 'uint64'},
    {'name': 'AddLogData', 'type': 'uint64'},
]


@pytest.fixture(name='enrich_results_dir')
def enrich_results_dir_fixture(yt_client, yt_root):
    return create_subdirectory(yt_client, yt_root, 'enrich_results')


@pytest.fixture(name='enriched_log_dir')
def enriched_log_dir_fixture(yt_client, enrich_results_dir):
    return create_subdirectory(yt_client, enrich_results_dir, 'enriched_log')


@pytest.fixture(name='stream_table_name')
def stream_table_name_fixture(stream_log_dir):
    return ypath_join(stream_log_dir, generate_stream_log_table_name())


@pytest.fixture(name='curr_enriched_log_path')
def curr_enriched_log_path_fixture(enriched_log_dir):
    return ypath_join(enriched_log_dir, CURR_RUN_ID)


@pytest.fixture(name='prev_enriched_log_path')
def prev_enriched_log_path_fixture(enriched_log_dir):
    return ypath_join(enriched_log_dir, PREV_RUN_ID)


@pytest.fixture(name='published_full_untariffed_dir')
def published_full_untariffed_dir_fixture(yt_client, published_dir):
    return create_subdirectory(yt_client, published_dir, 'full_untariffed')


@pytest.fixture(name='prev_published_full_untariffed_table_path')
def prev_published_full_untariffed_table_path_fixture(yt_root, published_full_untariffed_dir):
    return ypath_join(published_full_untariffed_dir, PREV_RUN_ID)


@pytest.fixture(name='full_untariffed_out_dir')
def full_untariffed_out_dir_fixure(yt_client, enrich_results_dir):
    return create_subdirectory(yt_client, enrich_results_dir, 'full_untariffed_log')


@pytest.fixture(name='curr_full_untariffed_out_path')
def curr_full_untariffed_out_path_fixture(full_untariffed_out_dir):
    return ypath_join(full_untariffed_out_dir, CURR_RUN_ID)


@pytest.fixture(name='corrections_dir')
def corrections_dir_fixture(yt_client, yt_root):
    return create_subdirectory(yt_client, yt_root, 'corrections_dir')


@pytest.fixture(name='run_job')
def run_job_fixture(
    yql_client,
    stream_log_dir,
    curr_tariffed_table_path,
    curr_untariffed_table_path,
    published_full_untariffed_dir,
    enriched_log_dir,
    full_untariffed_out_dir,
):

    def _wrapped(yt_client, transaction, log_type=enrich_log.LogType.stream.value, **kwargs):
        return enrich_log.EnrichLogProcessor(
            yt_client,
            yql_client,
            stream_log_dir,
            str(curr_tariffed_table_path),
            str(curr_untariffed_table_path),
            published_full_untariffed_dir,
            enriched_log_dir,
            full_untariffed_out_dir,
            transaction.transaction_id,
            log_type,
            **kwargs
        ).do()

    return _wrapped


@contextlib.contextmanager
def patch_run_query():
    with mock.patch('billing.log_tariffication.py.jobs.core_tariff.enrich_log.utils.yql.run_query') as mock_obj:
        yield mock_obj


def create_stream_table(yt_client, path, schema, interval, rows):
    yt_client.create(
        'table',
        path,
        attributes={
            LB_META_ATTR: interval.to_meta(),
            'schema': schema
        }
    )
    yt_client.write_table(path, rows or [])


def create_log_table(yt_client, path, schema, run_id, interval, rows, corrections_interval=None):
    meta = {}
    if interval is not None:
        meta[LOG_INTERVAL_KEY] = interval.to_meta()
    if run_id is not None:
        meta[RUN_ID_KEY] = run_id
    if corrections_interval is not None:
        meta[CORRECTIONS_LOG_INTERVAL_KEY] = corrections_interval.to_meta()
    yt_client.create(
        'table',
        path,
        attributes={
            'schema': schema,
            LOG_TARIFF_META_ATTR: meta
        }
    )
    yt_client.write_table(path, rows or [])


def create_stream_log_table(yt_client, path, interval, rows=None):
    create_stream_table(yt_client, path, STREAM_LOG_TABLE_SCHEMA, interval, rows)


def create_stream_full_untariffed_table(yt_client, path, interval, rows=None):
    create_log_table(yt_client, path, STREAM_LOG_TABLE_SCHEMA, None, interval, rows)


def create_stream_tariffed_table(yt_client, path, run_id, interval, rows=None, corrections_interval=None):
    create_log_table(yt_client, path, STREAM_TARIFFED_TABLE_SCHEMA, run_id, interval, rows, corrections_interval)


def create_stream_stripped_untariffed_table(yt_client, path, interval, rows=None):
    create_log_table(yt_client, path, STREAM_STRIPPED_LOG_TABLE_SCHEMA, None, interval, rows)


def create_stream_corrections_table(yt_client, path, interval, rows=None):
    create_log_table(yt_client, path, STREAM_CORRECTIONS_TABLE_SCHEMA, None, None, rows, interval)


def create_processed_log_table(yt_client, path, interval, rows=None):
    create_log_table(yt_client, path, PROCESSED_LOG_TABLE_SCHEMA, None, interval, rows)


def create_processed_full_untariffed_table(yt_client, path, interval, rows=None):
    create_log_table(yt_client, path, PROCESSED_LOG_TABLE_SCHEMA, None, interval, rows)


def create_processed_tariffed_table(yt_client, path, run_id, interval, rows=None, corrections_interval=None):
    create_log_table(yt_client, path, PROCESSED_TARIFFED_TABLE_SCHEMA, run_id, interval, rows, corrections_interval)


def create_processed_stripped_untariffed_table(yt_client, path, interval, rows=None):
    create_log_table(yt_client, path, PROCESSED_STRIPPED_LOG_TABLE_SCHEMA, None, interval, rows)


def create_processed_corrections_table(yt_client, path, interval, rows=None):
    create_log_table(yt_client, path, PROCESSED_CORRECTIONS_TABLE_SCHEMA, None, None, rows, interval)


def get_result(yt_client, res_paths):
    path_names = ['tariffed', 'untariffed']

    res = {
        'rows': {},
        'meta': {}
    }
    for name, path in zip(path_names, res_paths):
        for row in yt_client.read_table(path):
            res['rows'].setdefault(name, []).append(row)

            meta_attr = yt_client.get(ypath_join(path, '@' + LOG_TARIFF_META_ATTR))
            run_id = meta_attr.pop('run_id')
            assert run_id
            res['meta'][name] = meta_attr

    return res


def mk_lb_uid(offset, record):
    return '@'.join(map(str, ('a', 'a', 0, offset, record)))


def mk_stream_log_row(offset, record, counter):
    return {
        'LogData': next(counter),
        'AddLogData': next(counter),
        'AddLogOtherData': next(counter),
        '_topic_cluster': 'a',
        '_topic': 'a',
        '_partition': 0,
        '_offset': offset,
        '_chunk_record_index': record,
    }


def mk_stream_tariffed_row(offset, record, counter):
    return {
        'TariffedData': next(counter),
        'LBMessageUID': mk_lb_uid(offset, record),
    }


def mk_stream_stripped_row(offset, record, counter):
    return {
        'LogData': next(counter),
        'LBMessageUID': mk_lb_uid(offset, record),
    }


def mk_stream_corrections_row(offset, record, counter):
    return {
        'LogData': next(counter),
        'AddLogData': next(counter),
        'LBMessageUID': mk_lb_uid(offset, record),
    }


def mk_processed_log_row(id_, counter):
    return {
        'LogData': next(counter),
        'AddLogData': next(counter),
        'AddLogOtherData': next(counter),
        'ID': id_,
    }


def mk_processed_tariffed_row(id_, counter):
    return {
        'TariffedData': next(counter),
        'ID': id_,
    }


def mk_processed_stripped_row(id_, counter):
    return {
        'LogData': next(counter),
        'ID': id_,
    }


def mk_processed_corrections_row(id_, counter):
    return {
        'LogData': next(counter),
        'AddLogData': next(counter),
        'ID': id_,
    }


def test_stream_log(
    yt_client, run_job, stream_table_name, curr_tariffed_table_path, curr_untariffed_table_path,
    prev_published_full_untariffed_table_path, enriched_log_dir, curr_enriched_log_path,
    curr_full_untariffed_out_path,
):
    counter = itertools.count(1)
    log_rows = [
        mk_stream_log_row(0, 4, counter),
        mk_stream_log_row(1, 1, counter),
        mk_stream_log_row(1, 2, counter),
        mk_stream_log_row(2, 0, counter),
        mk_stream_log_row(2, 1, counter),
        mk_stream_log_row(3, 0, counter),
    ]

    full_untariffed_rows = [
        mk_stream_log_row(0, 1, counter),
        mk_stream_log_row(0, 2, counter),
        mk_stream_log_row(0, 3, counter),
        mk_stream_log_row(0, 4, counter),
    ]

    tariffed_rows = [
        mk_stream_tariffed_row(0, 1, counter),
        mk_stream_tariffed_row(0, 2, counter),
        mk_stream_tariffed_row(1, 1, counter),
        mk_stream_tariffed_row(1, 2, counter),
        mk_stream_tariffed_row(3, 0, counter),
        mk_stream_tariffed_row(666, 666, counter),
        mk_stream_tariffed_row(666, 668, counter),
    ]

    stripped_untariffed_rows = [
        mk_stream_stripped_row(1, 2, counter),
        mk_stream_stripped_row(2, 0, counter),
        mk_stream_stripped_row(0, 2, counter),
        mk_stream_stripped_row(0, 3, counter),
        mk_stream_stripped_row(3, 0, counter),
        mk_stream_stripped_row(666, 666, counter),
        mk_stream_stripped_row(666, 667, counter),
    ]

    create_stream_log_table(
        yt_client,
        stream_table_name,
        mk_interval(0, 4),
        log_rows
    )

    create_stream_full_untariffed_table(
        yt_client,
        prev_published_full_untariffed_table_path,
        mk_interval(0, 1),
        full_untariffed_rows
    )

    create_stream_tariffed_table(
        yt_client,
        curr_tariffed_table_path,
        CURR_RUN_ID,
        mk_interval(1, 3),
        tariffed_rows
    )

    create_stream_stripped_untariffed_table(
        yt_client,
        curr_untariffed_table_path,
        mk_interval(1, 3),
        stripped_untariffed_rows
    )

    expected_result_paths = (curr_enriched_log_path, curr_full_untariffed_out_path)
    with yt_client.Transaction(ping=False) as transaction:
        assert run_job(
            yt_client,
            transaction,
            enrich_log.LogType.stream.value,
            lock_wait_seconds=1
        ) == expected_result_paths
        check_node_is_locked(enriched_log_dir)

    return get_result(yt_client, expected_result_paths)


def test_processed_log(
    yt_client, run_job, stream_log_dir, curr_tariffed_table_path, curr_untariffed_table_path,
    prev_published_full_untariffed_table_path,
):
    counter = itertools.count(1)
    create_processed_log_table(
        yt_client, ypath_join(stream_log_dir, '0'),
        mk_interval(0, 1),
        [
            mk_processed_log_row(0, counter),
        ]
    )
    create_processed_log_table(
        yt_client, ypath_join(stream_log_dir, '1'),
        mk_interval(1, 2),
        [
            mk_processed_log_row(1, counter),
            mk_processed_log_row(2, counter),
        ]
    )
    create_processed_log_table(
        yt_client, ypath_join(stream_log_dir, '2'),
        mk_interval(2, 3),
        [
            mk_processed_log_row(3, counter),
        ]
    )
    create_processed_log_table(
        yt_client, ypath_join(stream_log_dir, '3'),
        mk_interval(3, 666),
        [
            mk_processed_log_row(666, counter),
            mk_processed_log_row(667, counter),
        ]
    )
    create_processed_full_untariffed_table(
        yt_client, prev_published_full_untariffed_table_path,
        mk_interval(0, 1),
        [
            mk_processed_log_row(4, counter),
            mk_processed_log_row(5, counter),
            mk_processed_log_row(6, counter),
            mk_processed_log_row(668, counter),
        ]
    )

    create_processed_tariffed_table(
        yt_client, curr_tariffed_table_path,
        CURR_RUN_ID, mk_interval(1, 3),
        [
            mk_processed_tariffed_row(1, counter),
            mk_processed_tariffed_row(2, counter),
            mk_processed_tariffed_row(4, counter),
            mk_processed_tariffed_row(5, counter),
            mk_processed_tariffed_row(666, counter),
            mk_processed_tariffed_row(6666, counter),
        ]
    )
    create_processed_stripped_untariffed_table(
        yt_client, curr_untariffed_table_path,
        mk_interval(1, 3),
        [
            mk_processed_stripped_row(2, counter),
            mk_processed_stripped_row(3, counter),
            mk_processed_stripped_row(5, counter),
            mk_processed_stripped_row(6, counter),
            mk_processed_stripped_row(666, counter),
            mk_processed_stripped_row(66666, counter),
        ]
    )

    with yt_client.Transaction(ping=False) as transaction:
        res_paths = run_job(
            yt_client,
            transaction,
            log_type=enrich_log.LogType.processed.value,
            join_column='ID'
        )

    return get_result(yt_client, res_paths)


def test_stream_log_corrections(
    yt_client, run_job, stream_table_name, curr_tariffed_table_path, curr_untariffed_table_path,
    prev_published_full_untariffed_table_path, curr_enriched_log_path, curr_full_untariffed_out_path,
    corrections_dir,
):
    counter = itertools.count(1)
    create_stream_log_table(
        yt_client, stream_table_name, mk_interval(1, 2),
        [
            mk_stream_log_row(1, 1, counter),
        ]
    )

    create_stream_full_untariffed_table(
        yt_client,  prev_published_full_untariffed_table_path, mk_interval(0, 1),
        [
            mk_stream_log_row(0, 1, counter),
        ]
    )

    create_stream_corrections_table(
        yt_client, ypath_join(corrections_dir, '0'), mk_interval(665, 666),
        [
            mk_stream_corrections_row(665, 1, counter),
        ]
    )
    create_stream_corrections_table(
        yt_client, ypath_join(corrections_dir, '1'), mk_interval(666, 667),
        [
            mk_stream_corrections_row(666, 1, counter),
        ]
    )
    create_stream_corrections_table(
        yt_client, ypath_join(corrections_dir, '2'), mk_interval(667, 668),
        [
            mk_stream_corrections_row(667, 1, counter),
        ]
    )
    create_stream_corrections_table(
        yt_client, ypath_join(corrections_dir, '3'), mk_interval(668, 669),
        [
            mk_stream_corrections_row(668, 1, counter),
        ]
    )

    create_stream_tariffed_table(
        yt_client, curr_tariffed_table_path, CURR_RUN_ID, mk_interval(1, 2),
        [
            mk_stream_tariffed_row(1, 1, counter),
            mk_stream_tariffed_row(0, 1, counter),
            mk_stream_tariffed_row(665, 1, counter),
            mk_stream_tariffed_row(666, 1, counter),
            mk_stream_tariffed_row(667, 1, counter),
        ],
        corrections_interval=mk_interval(666, 668),
    )

    create_stream_stripped_untariffed_table(
        yt_client, curr_untariffed_table_path, mk_interval(1, 2),
        [
            mk_stream_stripped_row(1, 1, counter),
            mk_stream_stripped_row(0, 1, counter),
            mk_stream_stripped_row(666, 1, counter),
            mk_stream_stripped_row(667, 1, counter),
            mk_stream_stripped_row(668, 1, counter),
        ]
    )

    expected_result_paths = (curr_enriched_log_path, curr_full_untariffed_out_path)
    with yt_client.Transaction(ping=False) as transaction:
        assert run_job(
            yt_client,
            transaction,
            enrich_log.LogType.stream.value,
            corrections_dir=corrections_dir,
        ) == expected_result_paths

    return get_result(yt_client, expected_result_paths)


def test_processed_log_corrections(
    yt_client, run_job, stream_log_dir, curr_tariffed_table_path, curr_untariffed_table_path,
    prev_published_full_untariffed_table_path,
    corrections_dir,
):
    counter = itertools.count(1)
    create_processed_log_table(
        yt_client, ypath_join(stream_log_dir, '1'), mk_interval(1, 2),
        [
            mk_processed_log_row(1, counter),
        ]
    )

    create_processed_full_untariffed_table(
        yt_client,  prev_published_full_untariffed_table_path, mk_interval(0, 1),
        [
            mk_processed_log_row(0, counter),
        ]
    )

    create_processed_corrections_table(
        yt_client, ypath_join(corrections_dir, '0'), mk_interval(665, 666),
        [
            mk_processed_corrections_row(665, counter),
        ]
    )
    create_processed_corrections_table(
        yt_client, ypath_join(corrections_dir, '1'), mk_interval(666, 667),
        [
            mk_processed_corrections_row(666, counter),
        ]
    )
    create_processed_corrections_table(
        yt_client, ypath_join(corrections_dir, '2'), mk_interval(667, 668),
        [
            mk_processed_corrections_row(667, counter),
        ]
    )
    create_processed_corrections_table(
        yt_client, ypath_join(corrections_dir, '3'), mk_interval(668, 669),
        [
            mk_processed_corrections_row(668, counter),
        ]
    )

    create_processed_tariffed_table(
        yt_client, curr_tariffed_table_path, CURR_RUN_ID, mk_interval(1, 2),
        [
            mk_processed_tariffed_row(1, counter),
            mk_processed_tariffed_row(0, counter),
            mk_processed_tariffed_row(665, counter),
            mk_processed_tariffed_row(666, counter),
            mk_processed_tariffed_row(667, counter),
        ],
        corrections_interval=mk_interval(666, 668),
    )

    create_processed_stripped_untariffed_table(
        yt_client, curr_untariffed_table_path, mk_interval(1, 2),
        [
            mk_processed_stripped_row(1, counter),
            mk_processed_stripped_row(0, counter),
            mk_processed_stripped_row(665, counter),
            mk_processed_stripped_row(666, counter),
            mk_processed_stripped_row(667, counter),
        ]
    )

    with yt_client.Transaction(ping=False) as transaction:
        res = run_job(
            yt_client,
            transaction,
            enrich_log.LogType.processed.value,
            join_column='ID',
            corrections_dir=corrections_dir,
        )

    return get_result(yt_client, res)


def test_already_done(
    yt_client, run_job, caplog, stream_table_name, curr_tariffed_table_path, curr_enriched_log_path, yt_transaction,
    prev_published_full_untariffed_table_path, curr_full_untariffed_out_path, curr_untariffed_table_path
):
    create_stream_log_table(yt_client, stream_table_name, mk_interval(2, 3))
    create_stream_full_untariffed_table(yt_client, prev_published_full_untariffed_table_path, mk_interval(0, 2))

    create_stream_tariffed_table(yt_client, curr_tariffed_table_path, CURR_RUN_ID, mk_interval(2, 3))
    create_stream_stripped_untariffed_table(yt_client, curr_untariffed_table_path, mk_interval(2, 3))

    create_stream_tariffed_table(yt_client, curr_enriched_log_path, CURR_RUN_ID, mk_interval(2, 3))
    create_stream_tariffed_table(yt_client, curr_full_untariffed_out_path, CURR_RUN_ID, mk_interval(2, 3))

    assert run_job(yt_client, yt_transaction) == (curr_enriched_log_path, curr_full_untariffed_out_path)
    assert "Already done" in caplog.text


def test_unmatched_untariffed(
    yt_client, run_job, stream_table_name, curr_tariffed_table_path, curr_untariffed_table_path,
    prev_published_full_untariffed_table_path,
):
    create_stream_log_table(yt_client, stream_table_name, mk_interval(2, 3))
    create_stream_full_untariffed_table(yt_client, prev_published_full_untariffed_table_path, mk_interval(0, 1))

    create_stream_tariffed_table(yt_client, curr_tariffed_table_path, CURR_RUN_ID, mk_interval(2, 3))
    create_stream_stripped_untariffed_table(yt_client, curr_untariffed_table_path, mk_interval(2, 3))

    with yt_client.Transaction(ping=False) as transaction, patch_run_query() as mock_obj:
        with pytest.raises(AssertionError) as exc_info:
            run_job(yt_client, transaction, lock_wait_seconds=1)

    assert mock_obj.call_count == 0
    assert 'Full untariffed not preceding' in exc_info.value.args[0]


def test_invalid_prev_result(
    yt_client, run_job, stream_table_name, curr_tariffed_table_path, curr_untariffed_table_path,
    prev_enriched_log_path, prev_published_full_untariffed_table_path,
):
    create_stream_log_table(yt_client, stream_table_name, mk_interval(2, 3))
    create_stream_full_untariffed_table(yt_client, prev_published_full_untariffed_table_path, mk_interval(0, 2))

    create_stream_tariffed_table(yt_client, curr_tariffed_table_path, CURR_RUN_ID, mk_interval(2, 3))
    create_stream_stripped_untariffed_table(yt_client, curr_untariffed_table_path, mk_interval(2, 3))

    create_stream_tariffed_table(yt_client, prev_enriched_log_path, PREV_RUN_ID, mk_interval(0, 1))

    with yt_client.Transaction(ping=False) as transaction, patch_run_query() as mock_obj:
        with pytest.raises(AssertionError) as exc_info:
            run_job(yt_client, transaction, lock_wait_seconds=1)

    assert mock_obj.call_count == 0
    assert 'Previous table is not preceding for' in exc_info.value.args[0]


def test_part_result(
    yt_client, run_job, stream_table_name, curr_tariffed_table_path, curr_untariffed_table_path,
    prev_published_full_untariffed_table_path, curr_full_untariffed_out_path
):
    create_stream_log_table(yt_client, stream_table_name, mk_interval(2, 3))
    create_stream_full_untariffed_table(yt_client, prev_published_full_untariffed_table_path, mk_interval(0, 2))

    create_stream_tariffed_table(yt_client, curr_tariffed_table_path, CURR_RUN_ID, mk_interval(2, 3))
    create_stream_stripped_untariffed_table(yt_client, curr_untariffed_table_path, mk_interval(2, 3))

    create_stream_tariffed_table(yt_client, curr_full_untariffed_out_path, CURR_RUN_ID, mk_interval(2, 3))

    with yt_client.Transaction(ping=False) as transaction, patch_run_query() as mock_obj:
        with pytest.raises(AssertionError) as exc_info:
            run_job(yt_client, transaction, lock_wait_seconds=1)

    assert mock_obj.call_count == 0
    assert 'Partially formed result!' in exc_info.value.args[0]
