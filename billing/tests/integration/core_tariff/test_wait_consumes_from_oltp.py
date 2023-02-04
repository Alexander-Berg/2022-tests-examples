import pytest
from billing.log_tariffication.py.jobs.core_tariff.wait_consumes_from_oltp import (run_job,
                                                                                   get_query,
                                                                                   run_query,
                                                                                   get_table_with_autooverdraft_consumes,
                                                                                   get_consumes_stream_query)
from yt.wrapper import (
    ypath_join,
)

from billing.library.python.logfeller_utils.log_interval import (
    LB_META_ATTR,
)
from billing.log_tariffication.py.lib.constants import (
    LOG_TARIFF_META_ATTR,
    RUN_ID_KEY,
    LOG_INTERVAL_KEY,
    GENERATE_AUTO_OVERDRAFT_KEY,
    AUTO_OVERDRAFT_DT_KEY
)
from billing.log_tariffication.py.tests.constants import (
    PREV_RUN_ID,
    CURR_RUN_ID,
    NEXT_RUN_ID,
    PREV_RUN_DT,
    PREV_DAY_RUN_DT,
    PREV_DAY_RUN_ID,
    CURR_RUN_DT,
    PREV_LOG_INTERVAL,
    CURR_LOG_INTERVAL,
    NEXT_LOG_INTERVAL,
    LOGFELLER_TABLE_SCHEMA
)
from billing.library.python.yt_utils.test_utils.utils import (
    create_subdirectory,
)

IN_PROGRESS = 'in_progress'
FINISHED = 'finished'


def get_proper_run_id(interval):
    if interval == PREV_LOG_INTERVAL:
        return PREV_RUN_ID
    elif interval == CURR_LOG_INTERVAL:
        return CURR_RUN_ID
    elif interval == NEXT_LOG_INTERVAL:
        return NEXT_RUN_ID


def set_log_meta(
    yt_client,
    path,
    run_id,
    auto_overdraft_dt=None,
    generate_auto_overdraft=None,
    log_interval=None
):
    meta = {}

    if generate_auto_overdraft is not None:
        meta[GENERATE_AUTO_OVERDRAFT_KEY] = generate_auto_overdraft
    if auto_overdraft_dt is not None:
        meta[AUTO_OVERDRAFT_DT_KEY] = auto_overdraft_dt.strftime('%Y-%m-%d')
    if run_id is not None:
        meta[RUN_ID_KEY] = run_id
    if log_interval is not None:
        meta[LOG_INTERVAL_KEY] = log_interval.to_meta()

    yt_client.set(ypath_join(path, '@' + LOG_TARIFF_META_ATTR), meta)


@pytest.fixture(name='tariffed_events_dir')
def tariffed_events_dir_fixture(yt_client, yt_root):
    return create_subdirectory(yt_client, yt_root, 'tariffed_events')


@pytest.fixture(name='consumes_stream_dir')
def consumes_stream_dir_fixture(yt_client, yt_root):
    return create_subdirectory(yt_client, yt_root, 'consumes_stream')


@pytest.fixture(name='consumes_path')
def consumes_path_fixture(yt_root):
    return ypath_join(yt_root, 'consumes')


def create_tariffed_events_table(yt_client, path, data=None):
    yt_client.create(
        'table',
        path,
        attributes={
            'schema': [
                {'name': 'consume_id', 'type': 'uint64'},
            ],
        }
    )
    if data:
        yt_client.write_table(
            path,
            [
                {
                    'consume_id': cid
                }
                for cid in data
            ]
        )


def create_consumes_table(yt_client, path, data=None):
    yt_client.create(
        'table',
        path,
        attributes={
            'schema': [
                {'name': 'ID', 'type': 'uint64'},
            ],
        }
    )
    if data:
        yt_client.write_table(
            path,
            [
                {
                    'ID': cid
                }
                for cid in data
            ]
        )


def create_consumes_stream_table(yt_client, path, log_interval, data=None):
    yt_client.create(
        'table',
        path,
        attributes={
            LB_META_ATTR: log_interval.to_meta(),
            'schema': [{'name': 'ID', 'type': 'int64'}] + LOGFELLER_TABLE_SCHEMA,
        }
    )
    subinterval, _ = log_interval.subintervals.values()

    yt_client.write_table(
        path,
        [
            {
                'ID': id_,
                '_topic_cluster': subinterval.cluster,
                '_topic': subinterval.topic,
                '_partition': subinterval.partition,
                '_offset': subinterval.first_offset + idx,
                '_chunk_record_index': 1,
            }
            for idx, id_ in enumerate(data)
        ]
    )


def test_happy_path(yt_client, yql_client, consumes_path, consumes_stream_dir, tariffed_events_dir):
    old_tariffed_events_path = ypath_join(tariffed_events_dir, PREV_DAY_RUN_ID)
    create_tariffed_events_table(yt_client, old_tariffed_events_path, data=[6, 6, 6])
    set_log_meta(
        yt_client,
        old_tariffed_events_path,
        PREV_DAY_RUN_ID,
    )

    tariffed_events_path = ypath_join(tariffed_events_dir, PREV_RUN_ID)
    create_tariffed_events_table(yt_client, tariffed_events_path, data=[1, 2, 3])
    set_log_meta(
        yt_client,
        tariffed_events_path,
        PREV_RUN_ID,
        auto_overdraft_dt=PREV_RUN_DT,
        generate_auto_overdraft=IN_PROGRESS,
    )

    for interval, consumes_ids in zip([CURR_LOG_INTERVAL, NEXT_LOG_INTERVAL], [{1}, {4}]):
        consumes_stream_path = ypath_join(consumes_stream_dir, get_proper_run_id(interval))
        create_consumes_stream_table(yt_client,
                                     consumes_stream_path,
                                     interval,
                                     data=consumes_ids)

    create_consumes_table(yt_client, consumes_path, data={2, 3})
    set_log_meta(
        yt_client,
        consumes_path,
        PREV_RUN_ID,
        log_interval=PREV_LOG_INTERVAL

    )
    with yt_client.Transaction() as transaction:
        run_job(yt_client,
                yql_client,
                transaction.transaction_id,
                tariffed_events_dir,
                consumes_path,
                consumes_stream_dir,
                PREV_RUN_DT.strftime('%Y-%m-%d'),
                100000)


@pytest.mark.parametrize('tariffed_consumes, streamed_consumes, dyntable_consumes',
                         [
                             ([1, 2, 3, 4, 5], [{3}, {2, 6}], {4}),
                             ([1, 2, 3, 4, 5], [], set()),
                             ([1, 2, 3, 4, 5], [{6}, set()], {7}),
                             ([1, 2, 3], [{1, 2}, {3, 4}], set()),
                             ([1, 2, 3], [], {1, 2, 4}),
                             ([1, 2, 3], [{1}, {4}], {2, 3}),
                             ([1, 2, 3, None], [{1}, {4}], {2, 3})
                         ]
                         )
def test_unloaded_consumes_count(yt_client, yql_client, consumes_path, consumes_stream_dir, tariffed_events_dir,
                                 tariffed_consumes, streamed_consumes, dyntable_consumes):
    tariffed_events_path = ypath_join(tariffed_events_dir, PREV_RUN_ID)
    create_tariffed_events_table(yt_client, tariffed_events_path, data=tariffed_consumes)
    set_log_meta(
        yt_client,
        tariffed_events_path,
        PREV_RUN_ID,
        PREV_RUN_DT,
        IN_PROGRESS,
    )

    if streamed_consumes:
        for interval, consumes_ids in zip([CURR_LOG_INTERVAL, NEXT_LOG_INTERVAL], streamed_consumes):
            consumes_stream_path = ypath_join(consumes_stream_dir, get_proper_run_id(interval))
            create_consumes_stream_table(yt_client,
                                         consumes_stream_path,
                                         interval,
                                         data=consumes_ids)

    create_consumes_table(yt_client, consumes_path, data=dyntable_consumes)
    set_log_meta(
        yt_client,
        consumes_path,
        PREV_RUN_ID,
        log_interval=PREV_LOG_INTERVAL

    )

    with yt_client.Transaction() as transaction:
        query = get_query(yt_client, consumes_path, consumes_stream_dir, '$consumes_stream_query', 'balance')
        unloaded_consumes_count = run_query(yql_client,
                                            query,
                                            transaction.transaction_id,
                                            tariffed_events_path,
                                            consumes_path,
                                            )

    streamed_consumes_set = streamed_consumes[0].union(streamed_consumes[1]) if streamed_consumes else set()
    in_yt_consumes = streamed_consumes_set.union(dyntable_consumes)
    assert unloaded_consumes_count == len(set([c for c in tariffed_consumes if c]).difference(in_yt_consumes))
    return unloaded_consumes_count


@pytest.mark.parametrize('consumes_interval, consumes_stream_intervals, is_query_exists',
                         [
                             [CURR_LOG_INTERVAL, [CURR_LOG_INTERVAL], False],
                             [PREV_LOG_INTERVAL, [CURR_LOG_INTERVAL], True],
                             [PREV_LOG_INTERVAL, [CURR_LOG_INTERVAL, NEXT_LOG_INTERVAL], True],
                             [PREV_LOG_INTERVAL, [NEXT_LOG_INTERVAL], False],  # не должно случаться
                         ]
                         )
def test_get_consumes_query_empty_interval(yt_client,
                                           yql_client,
                                           consumes_path,
                                           consumes_stream_dir,
                                           tariffed_events_dir,
                                           consumes_interval,
                                           consumes_stream_intervals,
                                           is_query_exists
                                           ):
    create_consumes_table(yt_client, consumes_path, data=[1])
    set_log_meta(
        yt_client,
        consumes_path,
        get_proper_run_id(consumes_interval),
        log_interval=consumes_interval

    )
    for stream_interval in consumes_stream_intervals:
        consumes_stream_path = ypath_join(consumes_stream_dir, get_proper_run_id(stream_interval))
        create_consumes_stream_table(yt_client, consumes_stream_path, stream_interval, data=[3])

    if consumes_interval == PREV_LOG_INTERVAL and consumes_stream_intervals == [NEXT_LOG_INTERVAL]:
        error_message = 'Target interval is not covered by intersecting tables'
        with pytest.raises(AssertionError, match=error_message):
            get_consumes_stream_query(yt_client, consumes_path, consumes_stream_dir,
                                      query_name='$consumes_stream_query')

    else:
        assert bool(get_consumes_stream_query(yt_client, consumes_path, consumes_stream_dir,
                                              query_name='$consumes_stream_query')) == is_query_exists


def test_no_in_progress_tables(yt_client, tariffed_events_dir):
    tariffed_events_path = ypath_join(tariffed_events_dir, PREV_DAY_RUN_ID)
    create_tariffed_events_table(yt_client, tariffed_events_path)
    set_log_meta(
        yt_client,
        tariffed_events_path,
        PREV_DAY_RUN_ID,
        PREV_DAY_RUN_DT,
        IN_PROGRESS,
    )

    tariffed_events_path = ypath_join(tariffed_events_dir, CURR_RUN_ID)
    create_tariffed_events_table(yt_client, tariffed_events_path)
    set_log_meta(
        yt_client,
        tariffed_events_path,
        CURR_RUN_ID,
        CURR_RUN_DT,
        FINISHED,
    )
    error_message = 'no in_progress tables with auto_overdraft_dt == 2020-06-06'
    with pytest.raises(AssertionError, match=error_message):
        get_table_with_autooverdraft_consumes(yt_client,
                                              tariffed_events_dir,
                                              CURR_RUN_DT.strftime('%Y-%m-%d'))


def test_multiple_in_progress_tables(yt_client, tariffed_events_dir):
    for run_id in [PREV_RUN_ID, NEXT_RUN_ID]:
        tariffed_events_path = ypath_join(tariffed_events_dir, run_id)
        create_tariffed_events_table(yt_client, tariffed_events_path)
        set_log_meta(
            yt_client,
            tariffed_events_path,
            run_id,
            auto_overdraft_dt=CURR_RUN_DT,
            generate_auto_overdraft=IN_PROGRESS,
        )

    error_message = "more than 1 in_progress tables: ['{0}/2020-06-06T05:55:00', '{0}/2020-06-06T06:05:00']".format(
        str(tariffed_events_dir))
    with pytest.raises(AssertionError) as exc_info:
        get_table_with_autooverdraft_consumes(yt_client,
                                              tariffed_events_dir,
                                              CURR_RUN_DT.strftime('%Y-%m-%d'))
    assert exc_info.value.args[0] == error_message
