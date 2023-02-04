import pytest
from hamcrest import assert_that, contains_inanyorder

from yt.wrapper import ypath_join

from billing.library.python.logfeller_utils import log_interval
from billing.log_tariffication.py.jobs.common import prepare_reference
from billing.log_tariffication.py.tests import constants as tests_constants


@pytest.fixture()
def reference_log(yt_client, yt_root):
    reference_log = ypath_join(yt_root, 'reference_log')
    yield reference_log


@pytest.fixture()
def work_copy_dir(yt_client, yt_root):
    work_copy_dir = ypath_join(yt_root, 'work_copy_test_reference')
    yt_client.create('map_node', work_copy_dir)
    yield work_copy_dir
    yt_client.remove(work_copy_dir, force=True, recursive=True)


@pytest.fixture()
def cache_path(yt_client, yt_root):
    cache_path = ypath_join(yt_root, 'cache_test_reference')
    yield cache_path
    yt_client.remove(cache_path, force=True)


@pytest.fixture()
def full_reflog_cache(yt_client, cache_path):
    yt_client.create('table', cache_path, recursive=True, attributes={
        log_interval.LB_META_ATTR: {
            'topics': [
                {
                    'topic': 't1',
                    'cluster': 'c1',
                    'partitions': [
                        {
                            'partition': 0,
                            'first_offset': 0,
                            'next_offset': 3,
                        },
                    ],
                },
            ],
        },
        'schema': tests_constants.REFERENCE_LOG_TABLE_SCHEMA,
    })
    full_reflog_cache_rows = [
        {'ID': 1, 'Version': 0, 'Object': {'client_id': 1},
            '_topic_cluster': 'c1', '_topic': 't1', '_partition': 0, '_offset': 0},
        {'ID': 2, 'Version': 0, 'Object': {'client_id': 2},
            '_topic_cluster': 'c1', '_topic': 't1', '_partition': 0, '_offset': 1},
        {'ID': 3, 'Version': 0, 'Object': {'client_id': 9},
            '_topic_cluster': 'c1', '_topic': 't1', '_partition': 0, '_offset': 2},
    ]
    yt_client.write_table(cache_path, full_reflog_cache_rows)
    yield cache_path
    yt_client.remove(cache_path)


@pytest.fixture()
def ref_table_5min_00(yt_client, reference_log):
    ref_table_5min = ypath_join(reference_log, '2020-08-11T14:00:00')
    yt_client.create('table', ref_table_5min, recursive=True, attributes={
        log_interval.LB_META_ATTR: {
            'topics': [
                {
                    'topic': 't1',
                    'cluster': 'c1',
                    'partitions': [
                        {
                            'partition': 0,
                            'first_offset': 0,
                            'next_offset': 4,
                        },
                    ],
                },
            ],
        },
        'schema': tests_constants.REFERENCE_LOG_TABLE_SCHEMA,
    })
    table_5min_rows = [
        {'ID': 1, 'Version': 1, 'Object': {'client_id': 3},
            '_topic_cluster': 'c1', '_topic': 't1', '_partition': 0, '_offset': 0},
        {'ID': 2, 'Version': 1, 'Object': {'client_id': 5},
            '_topic_cluster': 'c1', '_topic': 't1', '_partition': 0, '_offset': 1},
        {'ID': 3, 'Version': 1, 'Object': {'client_id': 3},
            '_topic_cluster': 'c1', '_topic': 't1', '_partition': 0, '_offset': 2},
        {'ID': 2, 'Version': 2, 'Object': {'client_id': 6},
            '_topic_cluster': 'c1', '_topic': 't1', '_partition': 0, '_offset': 3},
    ]
    yt_client.write_table(ref_table_5min, table_5min_rows)
    yield ref_table_5min
    yt_client.remove(ref_table_5min)


@pytest.fixture()
def ref_table_5min_05(yt_client, reference_log):
    ref_table_5min = ypath_join(reference_log, '2020-08-11T14:05:00')
    yt_client.create('table', ref_table_5min, recursive=True, attributes={
        log_interval.LB_META_ATTR: {
            'topics': [
                {
                    'topic': 't1',
                    'cluster': 'c1',
                    'partitions': [
                        {
                            'partition': 0,
                            'first_offset': 4,
                            'next_offset': 7,
                        },
                    ],
                },
            ],
        },
        'schema': tests_constants.REFERENCE_LOG_TABLE_SCHEMA,
    })
    table_5min_rows = [
        {'ID': 1, 'Version': 3, 'Object': {'client_id': 3},
            '_topic_cluster': 'c1', '_topic': 't1', '_partition': 0, '_offset': 4},
        {'ID': 2, 'Version': 3, 'Object': {'client_id': 8},
            '_topic_cluster': 'c1', '_topic': 't1', '_partition': 0, '_offset': 5},
        {'ID': 1, 'Version': 4, 'Object': {'client_id': 4},
            '_topic_cluster': 'c1', '_topic': 't1', '_partition': 0, '_offset': 6},
    ]
    yt_client.write_table(ref_table_5min, table_5min_rows)
    yield ref_table_5min
    yt_client.remove(ref_table_5min)


@pytest.mark.parametrize(
    ['current_meta', 'expected_work_copy_rows', 'expected_index_rows', 'expected_full_log_rows_count'],
    [
        pytest.param(
            {
                'run_id': tests_constants.CURR_RUN_ID,
                'ref_test_interval': {
                    'topics': [
                        {
                            'topic': 't1',
                            'cluster': 'c1',
                            'partitions': [
                                {
                                    'partition': 0,
                                    'first_offset': 0,
                                    'next_offset': 3,
                                },
                            ],
                        },
                    ],
                },
            },
            (
                {'ID': 1, 'Version': 0, 'Object': {'client_id': 1}},
                {'ID': 2, 'Version': 0, 'Object': {'client_id': 2}},
                {'ID': 3, 'Version': 0, 'Object': {'client_id': 9}},
            ),
            (
                {'ClientID': 1, 'ID': 1},
                {'ClientID': 2, 'ID': 2},
                {'ClientID': 9, 'ID': 3},
            ),
            3,
            id='_offset-0-3'
        ),
        pytest.param(
            {
                'run_id': tests_constants.CURR_RUN_ID,
                'ref_test_interval': {
                    'topics': [
                        {
                            'topic': 't1',
                            'cluster': 'c1',
                            'partitions': [
                                {
                                    'partition': 0,
                                    'first_offset': 0,
                                    'next_offset': 7,
                                },
                            ],
                        },
                    ],
                },
            },
            (
                {'ID': 1, 'Version': 4, 'Object': {'client_id': 4}},
                {'ID': 2, 'Version': 3, 'Object': {'client_id': 8}},
                {'ID': 3, 'Version': 0, 'Object': {'client_id': 9}},
            ),
            (
                {'ClientID': 4, 'ID': 1},
                {'ClientID': 8, 'ID': 2},
                {'ClientID': 9, 'ID': 3},
            ),
            7,
            id='_offset-0-7'
        ),
        pytest.param(
            {
                'run_id': tests_constants.CURR_RUN_ID,
                'ref_test_interval': {
                    'topics': [
                        {
                            'topic': 't1',
                            'cluster': 'c1',
                            'partitions': [
                                {
                                    'partition': 0,
                                    'first_offset': 3,
                                    'next_offset': 6,
                                },
                            ],
                        },
                    ],
                },
            },
            (
                {'ID': 1, 'Version': 3, 'Object': {'client_id': 3}},
                {'ID': 2, 'Version': 3, 'Object': {'client_id': 8}},
                {'ID': 3, 'Version': 0, 'Object': {'client_id': 9}},
            ),
            (
                {'ClientID': 3, 'ID': 1},
                {'ClientID': 8, 'ID': 2},
                {'ClientID': 9, 'ID': 3},
            ),
            6,
            id='_offset-3-6'
        ),
    ]
)
def test_prepare_reference(yt_client, yql_client, ref_table_5min_00, ref_table_5min_05,
                           reference_log, work_copy_dir, full_reflog_cache,
                           current_meta, expected_work_copy_rows, expected_index_rows, expected_full_log_rows_count):
    with yt_client.Transaction() as transaction:
        work_copy_reference_path, indexes = prepare_reference.run_job(
            yt_client,
            yql_client,
            current_meta,
            'test',
            reference_log,
            full_reflog_cache,
            work_copy_dir,
            transaction,
            indexes=[prepare_reference.IndexDescription(
                'ClientID',
                'Yson::LookupUint64(Object, "client_id", Yson::Options(false as Strict, false as AutoConvert))'
            )],
        )

    work_copy_rows = list(yt_client.read_table(work_copy_reference_path))
    assert_that(work_copy_rows, contains_inanyorder(*expected_work_copy_rows))

    index_rows = list(yt_client.read_table(indexes[0].path))
    assert_that(index_rows, contains_inanyorder(*expected_index_rows))

    assert len(list(yt_client.read_table(full_reflog_cache))) == expected_full_log_rows_count

    current_meta_interval = log_interval.LogInterval.from_meta(current_meta['ref_test_interval'])
    end_slice = log_interval.LogSlice({
        ('c1', 't1', 0): 3,
    })
    new_cache_interval = log_interval.LogInterval.from_slices(
        log_interval.LogSlice({
            ('c1', 't1', 0): 0,
        }),
        end_slice if end_slice > current_meta_interval.end else current_meta_interval.end
    )
    assert yt_client.get(ypath_join(full_reflog_cache, '@' + log_interval.LB_META_ATTR)) == new_cache_interval.to_meta()


def test_prepare_reference_raises(yt_client, yql_client, reference_log, work_copy_dir, cache_path):
    yt_client.create('table', cache_path, recursive=True, attributes={
        log_interval.LB_META_ATTR: {
            'topics': [
                {
                    'topic': 't1',
                    'cluster': 'c1',
                    'partitions': [
                        {
                            'partition': 0,
                            'first_offset': 0,
                            'next_offset': 3,
                        },
                    ],
                },
            ],
        },
        'schema': tests_constants.REFERENCE_LOG_TABLE_SCHEMA,
    })
    current_meta = {
        'run_id': tests_constants.CURR_RUN_ID,
        'ref_test_interval': {
            'topics': [
                {
                    'topic': 't1',
                    'cluster': 'c1',
                    'partitions': [
                        {
                            'partition': 0,
                            'first_offset': 6,
                            'next_offset': 7,
                        },
                    ],
                },
            ],
        },
    }
    with pytest.raises(AssertionError):
        with yt_client.Transaction() as transaction:
            prepare_reference.run_job(
                yt_client,
                yql_client,
                current_meta,
                'test',
                reference_log,
                cache_path,
                work_copy_dir,
                transaction,
            )

    yt_client.set(ypath_join(cache_path, '@' + log_interval.LB_META_ATTR), {
        'topics': [
            {
                'topic': 't1',
                'cluster': 'c1',
                'partitions': [
                    {
                        'partition': 0,
                        'first_offset': 1,
                        'next_offset': 7,
                    },
                ],
            },
        ],
    })
    current_meta = {
        'run_id': tests_constants.CURR_RUN_ID,
        'ref_test_interval': {
            'topics': [
                {
                    'topic': 't1',
                    'cluster': 'c1',
                    'partitions': [
                        {
                            'partition': 0,
                            'first_offset': 0,
                            'next_offset': 7,
                        },
                    ],
                },
            ],
        },
    }
    with pytest.raises(AssertionError):
        with yt_client.Transaction() as transaction:
            prepare_reference.run_job(
                yt_client,
                yql_client,
                current_meta,
                'test',
                reference_log,
                cache_path,
                work_copy_dir,
                transaction,
            )


@pytest.mark.parametrize(
    ['current_meta', 'expected_work_copy_rows', 'expected_full_log_rows_count'],
    [
        pytest.param(
            {
                'run_id': tests_constants.CURR_RUN_ID,
                'ref_test_interval': {
                    'topics': [
                        {
                            'topic': 't1',
                            'cluster': 'c1',
                            'partitions': [
                                {
                                    'partition': 0,
                                    'first_offset': 0,
                                    'next_offset': 1,
                                },
                            ],
                        },
                    ],
                },
            },
            (
                {'ID': 1, 'Version': 1, 'Object': {'client_id': 3}},
            ),
            1,
            id='_offset-0-1'
        ),
        pytest.param(
            {
                'run_id': tests_constants.CURR_RUN_ID,
                'ref_test_interval': {
                    'topics': [
                        {
                            'topic': 't1',
                            'cluster': 'c1',
                            'partitions': [
                                {
                                    'partition': 0,
                                    'first_offset': 0,
                                    'next_offset': 4,
                                },
                            ],
                        },
                    ],
                },
            },
            (
                {'ID': 1, 'Version': 1, 'Object': {'client_id': 3}},
                {'ID': 2, 'Version': 2, 'Object': {'client_id': 6}},
                {'ID': 3, 'Version': 1, 'Object': {'client_id': 3}},
            ),
            4,
            id='_offset-0-4'
        ),
        pytest.param(
            {
                'run_id': tests_constants.CURR_RUN_ID,
                'ref_test_interval': {
                    'topics': [
                        {
                            'topic': 't1',
                            'cluster': 'c1',
                            'partitions': [
                                {
                                    'partition': 0,
                                    'first_offset': 1,
                                    'next_offset': 2,
                                },
                            ],
                        },
                    ],
                },
            },
            (
                {'ID': 2, 'Version': 1, 'Object': {'client_id': 5}},
            ),
            1,
            id='_offset-1-2'
        ),
        pytest.param(
            {
                'run_id': tests_constants.CURR_RUN_ID,
                'ref_test_interval': {
                    'topics': [
                        {
                            'topic': 't1',
                            'cluster': 'c1',
                            'partitions': [
                                {
                                    'partition': 0,
                                    'first_offset': 1,
                                    'next_offset': 1,
                                },
                            ],
                        },
                    ],
                },
            },
            (),
            0,
            id='_offset-EMPTY'
        ),
    ]
)
def test_prepare_reference_empty_cache(yt_client, yql_client, ref_table_5min_00,
                                       reference_log, work_copy_dir, cache_path,
                                       current_meta, expected_work_copy_rows, expected_full_log_rows_count):
    with yt_client.Transaction() as transaction:
        work_copy_reference_path, indexes = prepare_reference.run_job(
            yt_client,
            yql_client,
            current_meta,
            'test',
            reference_log,
            cache_path,
            work_copy_dir,
            transaction,
        )

    work_copy_rows = list(yt_client.read_table(work_copy_reference_path))
    assert_that(work_copy_rows, contains_inanyorder(*expected_work_copy_rows))

    assert len(list(yt_client.read_table(cache_path))) == expected_full_log_rows_count

    assert yt_client.get(ypath_join(cache_path, '@' + log_interval.LB_META_ATTR)) == current_meta['ref_test_interval']
