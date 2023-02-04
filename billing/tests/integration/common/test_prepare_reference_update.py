# -*- coding: utf-8 -*-

import pytest

from yt.wrapper import (
    ypath_join,
)

from billing.log_tariffication.py.lib.constants import (
    LOG_TARIFF_META_ATTR,
    LOG_INTERVAL_KEY,
    RUN_ID_KEY,
    DYN_TABLE_IS_UPDATING_KEY,
)
from billing.log_tariffication.py.jobs.common import prepare_reference_update
from billing.library.python.logfeller_utils.log_interval import (
    LB_META_ATTR,
)

from billing.library.python.logfeller_utils.tests.utils import (
    mk_interval,
)
from billing.library.python.yt_utils.test_utils.utils import (
    create_subdirectory,
    create_dyntable,
)
from billing.log_tariffication.py.tests.constants import (
    LOGFELLER_TABLE_SCHEMA,
    CURR_RUN_ID,
)


@pytest.fixture(name='cache_path')
def cache_path_fixture(yt_root):
    return ypath_join(yt_root, 'cache')


@pytest.fixture(name='log_dir')
def log_dir_fixture(yt_client, yt_root):
    return create_subdirectory(yt_client, yt_root, 'log')


@pytest.fixture(name='res_dir')
def res_dir_fixture(yt_client, yt_root):
    return create_subdirectory(yt_client, yt_root, 'res')


@pytest.fixture(name='run_job')
def run_job_fixture(
    yql_client,
    cache_path,
    log_dir,
    res_dir,
):

    def _wrapped(yt_client, transaction, meta, *args, **kwargs):
        return prepare_reference_update.run_job(
            yt_client,
            yql_client,
            transaction.transaction_id,
            meta,
            cache_path,
            log_dir,
            res_dir,
            ['some_string', 'some_int'],
            *args,
            **kwargs
        )

    return _wrapped


def get_meta(log_interval, run_id=None, log_interval_key=LOG_INTERVAL_KEY):
    meta = {
        log_interval_key: log_interval.to_meta(),
    }
    if run_id is not None:
        meta[RUN_ID_KEY] = run_id
    return meta


def create_cache_table(yt_client, path, data, log_interval, is_updating=False):
    create_dyntable(
        yt_client,
        path,
        [
            {'name': 'ID', 'type': 'int64', 'sort_order': 'ascending'},
            {'name': 'Version', 'type': 'uint64'},
            {'name': 'some_string', 'type': 'string'},
            {'name': 'some_int', 'type': 'int64'},
        ],
        [
            {
                'ID': id_,
                'Version': v,
                'some_string': s,
                'some_int': i,
            }
            for id_, v, s, i in data
        ],
        {
            LOG_TARIFF_META_ATTR: {
                LOG_INTERVAL_KEY: log_interval.to_meta(),
                DYN_TABLE_IS_UPDATING_KEY: is_updating,
            }
        }
    )


def create_log_table(yt_client, path, data, log_interval):
    yt_client.create(
        'table',
        path,
        attributes={
            LB_META_ATTR: log_interval.to_meta(),
            'schema': [
                {'name': 'ID', 'type': 'int64'},
                {'name': 'Version', 'type': 'uint64'},
                {'name': 'some_string', 'type': 'string'},
                {'name': 'some_int', 'type': 'int64'},
            ] + LOGFELLER_TABLE_SCHEMA,
        }
    )

    subinterval, = log_interval.subintervals.values()

    yt_client.write_table(
        path,
        [
            {
                'ID': id_,
                'Version': v,
                'some_string': s,
                'some_int': i,
                '_topic_cluster': subinterval.cluster,
                '_topic': subinterval.topic,
                '_partition': subinterval.partition,
                '_offset': subinterval.first_offset + idx,
                '_chunk_record_index': 1,
            }
            for idx, (id_, v, s, i) in enumerate(data)
        ]
    )


@pytest.mark.parametrize(
    ['log_interval_key'],
    [
        pytest.param(LOG_INTERVAL_KEY, id='default'),
        pytest.param('ref_log_interval', id='custom'),
    ]
)
def test_calculations(yt_client, run_job, cache_path, log_dir, log_interval_key):
    create_cache_table(
        yt_client,
        cache_path,
        [
            (1, 10, 'a', 1),
            (2, 20, 'b', 2),
            (3, 30, 'c', 3),
            (4, 40, 'd', 4),
            (5, 50, 'e', 5),
        ],
        mk_interval(10, 20)
    )
    create_log_table(
        yt_client,
        ypath_join(log_dir, '2020-02-20T00:00:00'),
        [(666, 666, '666', 666)],
        mk_interval(10, 20)
    )
    create_log_table(
        yt_client,
        ypath_join(log_dir, '2020-02-20T00:05:00'),
        [
            (1, 9, '666', 666),
            (2, 20, '666', 666),
            (3, 31, '3', 4),
            (4, 41, 'b', 9),
        ],
        mk_interval(20, 25)
    )
    create_log_table(
        yt_client,
        ypath_join(log_dir, '2020-02-20T00:10:00'),
        [
            (4, 39, 'a', 4),
            (5, 53, 'c', 3),
            (5, 52, 'b', 2),
            (5, 51, 'a', 1),
            (6, 0, 'preved', 10),
            (7, 7, '7', 7),
        ],
        mk_interval(25, 31)
    )
    create_log_table(
        yt_client,
        ypath_join(log_dir, '2020-02-20T00:15:00'),
        [(6666, 6666, '6666', 6666)],
        mk_interval(31, 666)
    )

    with yt_client.Transaction() as transaction:
        res_path = run_job(
            yt_client,
            transaction,
            get_meta(mk_interval(20, 30), '666', log_interval_key=log_interval_key),
            log_interval_key=log_interval_key
        )

    return {
        'rows': list(yt_client.read_table(res_path)),
        'meta': yt_client.get(ypath_join(res_path, '@' + LOG_TARIFF_META_ATTR))
    }


@pytest.mark.parametrize(
    'dyntable_interval, run_interval',
    [
        (mk_interval(10, 20), mk_interval(9, 20)),
        (mk_interval(10, 20), mk_interval(11, 20)),
    ]
)
def test_dyntable_interval(yt_client, run_job, cache_path, dyntable_interval, run_interval):
    create_cache_table(
        yt_client,
        cache_path,
        [(1, 10, 'a', 1)],
        dyntable_interval
    )

    with pytest.raises(AssertionError) as exc_info:
        with yt_client.Transaction() as transaction:
            run_job(
                yt_client,
                transaction,
                get_meta(run_interval, CURR_RUN_ID)
            )

    assert 'Broken sequence in dyntable' in exc_info.value.args[0]


@pytest.mark.parametrize(
    'dyntable_interval, run_interval',
    [
        (mk_interval(10, 20), mk_interval(10, 20)),
        (mk_interval(10, 20), mk_interval(20, 30)),
    ]
)
def test_dyntable_updating_unprocessed(yt_client, run_job, cache_path, dyntable_interval, run_interval):
    create_cache_table(
        yt_client,
        cache_path,
        [(1, 10, 'a', 1)],
        dyntable_interval,
        is_updating=True
    )

    with pytest.raises(AssertionError) as exc_info:
        with yt_client.Transaction() as transaction:
            run_job(
                yt_client,
                transaction,
                get_meta(run_interval, CURR_RUN_ID)
            )

    assert 'dyntable is updating!' in exc_info.value.args[0]


@pytest.mark.parametrize('dyntable_updating', [False, True])
def test_already_processed(yt_client, run_job, cache_path, log_dir, res_dir, dyntable_updating):
    create_cache_table(
        yt_client,
        cache_path,
        [(1, 10, 'a', 1)],
        mk_interval(10, 20),
        dyntable_updating
    )
    create_log_table(
        yt_client,
        ypath_join(log_dir, '2020-20-20T00:00:00'),
        [(6666, 6666, '6666', 6666)],
        mk_interval(10, 20)
    )

    req_res_path = ypath_join(res_dir, CURR_RUN_ID)
    yt_client.create(
        'table',
        req_res_path,
        attributes={
            LOG_TARIFF_META_ATTR: get_meta(mk_interval(10, 20), CURR_RUN_ID),
            'schema': [{'name': 'name', 'type': 'string'}]
        }
    )
    yt_client.write_table(req_res_path, [{'name': 'hello world'}])

    with yt_client.Transaction() as transaction:
        res_path = run_job(
            yt_client,
            transaction,
            get_meta(mk_interval(10, 20), CURR_RUN_ID)
        )

    assert res_path == req_res_path
    assert list(yt_client.read_table(res_path)) == [{'name': 'hello world'}]


def test_res_wrong_meta(yt_client, run_job, cache_path, res_dir):
    create_cache_table(
        yt_client,
        cache_path,
        [(1, 10, 'a', 1)],
        mk_interval(10, 20),
    )

    yt_client.create(
        'table',
        ypath_join(res_dir, CURR_RUN_ID),
        attributes={
            LOG_TARIFF_META_ATTR: get_meta(mk_interval(10, 20), 'abyrvalg'),
        }
    )

    with pytest.raises(AssertionError) as exc_info:
        with yt_client.Transaction() as transaction:
            run_job(
                yt_client,
                transaction,
                get_meta(mk_interval(10, 20), CURR_RUN_ID)
            )

    assert 'Bad meta in current table for' in exc_info.value.args[0]


def test_empty_interval(yt_client, run_job, cache_path):
    create_cache_table(
        yt_client,
        cache_path,
        [
            (1, 10, 'a', 1),
        ],
        mk_interval(10, 20)
    )

    with yt_client.Transaction() as transaction:
        res_path = run_job(
            yt_client,
            transaction,
            get_meta(mk_interval(20, 20), '666')
        )

    return {
        'rows': list(yt_client.read_table(res_path)),
        'meta': yt_client.get(ypath_join(res_path, '@' + LOG_TARIFF_META_ATTR))
    }


def test_save_prev_columns(yt_client, run_job, cache_path, log_dir):
    create_cache_table(
        yt_client,
        cache_path,
        [
            (1, 10, 'a', 1),
            (2, 20, 'b', 2),
            (3, 30, 'c', 3),
            (4, 40, 'd', 4),
            (5, 50, 'e', 5),
        ],
        mk_interval(10, 20)
    )
    create_log_table(
        yt_client,
        ypath_join(log_dir, '2020-02-20T00:00:00'),
        [(1, 11, 'b', 2)],
        mk_interval(20, 30)
    )

    with yt_client.Transaction() as transaction:
        res_path = run_job(
            yt_client,
            transaction,
            get_meta(mk_interval(20, 30), '666'),
            ['some_string', 'some_int']
        )

    result = list(yt_client.read_table(res_path))

    assert len(result) == 1
    assert result[0] == {'ID': 1, 'Version': 11, 'some_string': 'b',
                         'some_int': 2, '__prev_some_string': 'a', '__prev_some_int': 1}
