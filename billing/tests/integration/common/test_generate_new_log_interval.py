# coding=utf-8

import pytest

from yt.wrapper import ypath_join

from billing.log_tariffication.py.jobs.common import generate_new_log_interval
from billing.library.python.logfeller_utils.log_interval import (
    LogInterval,
    Subinterval,
    LB_META_ATTR,
    get_next_stream_table_name,
)

from billing.library.python.logfeller_utils.tests.utils import (
    generate_stream_log_table_name,
)
from billing.log_tariffication.py.tests.constants import (
    PREV_RUN_ID,
    OLD_RUN_ID,
    CURR_RUN_ID,
    NEXT_RUN_ID,
)
from billing.log_tariffication.py.tests.utils import (
    patch_generate_run_id,
)


@pytest.mark.parametrize(
    ['last_generated_run_id', 'last_tariffed_run_id'],
    (
        (PREV_RUN_ID, CURR_RUN_ID),
        (PREV_RUN_ID, NEXT_RUN_ID),
        (CURR_RUN_ID, PREV_RUN_ID),
        (NEXT_RUN_ID, PREV_RUN_ID),
    )
)
def test_same_run_id(last_generated_run_id, last_tariffed_run_id):
    with patch_generate_run_id(return_value=CURR_RUN_ID):
        with pytest.raises(AssertionError,
                           match=r'new run id .+ last \w+ meta run id'):
            generate_new_log_interval.run_job(
                yt_client=None,
                last_generated_meta=dict(run_id=last_generated_run_id),
                last_tariffed_meta=dict(run_id=last_tariffed_run_id),
                log_dir=''
            )


def test_tariffed_is_behind(caplog):
    assert generate_new_log_interval.run_job(
        yt_client=None,
        last_generated_meta=dict(
            log_interval=LogInterval([Subinterval('c1', 't1', 0, 10, 20)]).to_meta(),
            run_id=PREV_RUN_ID
        ),
        last_tariffed_meta=dict(
            log_interval=LogInterval([Subinterval('c1', 't1', 0, 0, 10)]).to_meta(),
            run_id=OLD_RUN_ID
        ),
        log_dir=''
    ) is None
    assert "Last generated interval hasn't been processed yet" in caplog.text


def test_artifact_slices_mismatch():
    with pytest.raises(AssertionError, match='Artifact slices mismatch'):
        generate_new_log_interval.run_job(
            yt_client=None,
            last_generated_meta=dict(
                log_interval=LogInterval([Subinterval('c1', 't1', 0, 0, 10)]).to_meta(),
                run_id=PREV_RUN_ID
            ),
            last_tariffed_meta=dict(
                log_interval=LogInterval([Subinterval('c1', 't1', 0, 10, 20)]).to_meta(),
                run_id=PREV_RUN_ID
            ),
            log_dir=''
        )


def test_empty_directory(caplog, yt_client, yt_root):
    assert generate_new_log_interval.run_job(
        yt_client=yt_client,
        last_generated_meta=dict(
            log_interval=LogInterval([Subinterval('c1', 't1', 0, 0, 20)]).to_meta(),
            run_id=PREV_RUN_ID
        ),
        last_tariffed_meta=dict(
            log_interval=LogInterval([Subinterval('c1', 't1', 0, 10, 20)]).to_meta(),
            run_id=PREV_RUN_ID
        ),
        log_dir=yt_root
    ) is None
    assert "Log directory is empty" in caplog.text


@pytest.mark.parametrize(
    ['last_log_table_interval', 'log_message'],
    [
        pytest.param(
            LogInterval([Subinterval('c1', 't1', 0, 10, 20),
                         Subinterval('c1', 't1', 1, 0, 0)]),
            'Empty result interval',
            id='Empty result interval'
        ),
        pytest.param(
            LogInterval([Subinterval('c1', 't1', 0, 10, 20)]),
            'No new logs',
            id='No new logs',
        ),
        pytest.param(
            LogInterval([Subinterval('c1', 't1', 1, 10, 20)]),
            'The latest log slice is not comparable',
            id='The latest log slice is not comparable',
        ),
    ]
)
def test_problems_with_logs(caplog, yt_root, yt_client,
                            last_log_table_interval, log_message):
    log_table_name = generate_stream_log_table_name()
    yt_client.create('table', ypath_join(yt_root, log_table_name), attributes={
        LB_META_ATTR: last_log_table_interval.to_meta()
    })

    assert generate_new_log_interval.run_job(
        yt_client=yt_client,
        last_generated_meta=dict(
            log_interval=LogInterval([Subinterval('c1', 't1', 0, 10, 20)]).to_meta(),
            run_id=PREV_RUN_ID
        ),
        last_tariffed_meta=dict(
            log_interval=LogInterval([Subinterval('c1', 't1', 0, 10, 20)]).to_meta(),
            run_id=PREV_RUN_ID
        ),
        log_dir=yt_root
    ) is None
    assert log_message in caplog.text


def test_generated(yt_root, yt_client):
    log_table_name = generate_stream_log_table_name()
    yt_client.create('table', ypath_join(yt_root, log_table_name), attributes={
        LB_META_ATTR: LogInterval([
            Subinterval('c1', 't1', 0, 10, 20),
            Subinterval('c1', 't1', 4, 20, 20),
        ]).to_meta()
    })
    log_table_name = get_next_stream_table_name(log_table_name)
    yt_client.create('table', ypath_join(yt_root, log_table_name), attributes={
        LB_META_ATTR: LogInterval([
            Subinterval('c1', 't1', 0, 20, 30),
            Subinterval('c1', 't1', 1, 5, 10),
            Subinterval('c1', 't1', 4, 20, 20),
        ]).to_meta()
    })
    yt_client.create('table', ypath_join(yt_root, get_next_stream_table_name(log_table_name)), attributes={
        LB_META_ATTR: LogInterval([
            Subinterval('c1', 't1', 0, 30, 30),
            Subinterval('c1', 't1', 1, 10, 15),
            Subinterval('c1', 't1', 2, 0, 0),
            Subinterval('c1', 't1', 4, 20, 20),
        ]).to_meta()
    })

    with patch_generate_run_id(return_value=PREV_RUN_ID):
        res = generate_new_log_interval.run_job(
            yt_client=yt_client,
            last_generated_meta=dict(
                log_interval=LogInterval([
                    Subinterval('c1', 't1', 0, 10, 25),
                    Subinterval('c1', 't1', 4, 20, 20),
                ]).to_meta(),
                run_id=OLD_RUN_ID,
                some_transparent_param='some transparent val',
            ),
            last_tariffed_meta=dict(
                log_interval=LogInterval([
                    Subinterval('c1', 't1', 0, 10, 25),
                    Subinterval('c1', 't1', 4, 20, 20),
                ]).to_meta(),
                run_id=OLD_RUN_ID
            ),
            log_dir=yt_root
        )

    assert res['run_id'] == PREV_RUN_ID
    assert LogInterval.from_meta(res['log_interval']) == LogInterval([
        Subinterval('c1', 't1', 0, 25, 30),
        Subinterval('c1', 't1', 1, 0, 15),
        Subinterval('c1', 't1', 2, 0, 0),
        Subinterval('c1', 't1', 4, 20, 20),
    ])
    assert res['some_transparent_param'] == 'some transparent val'


def test_not_comparable_intervals(caplog, yt_root, yt_client):
    log_table_name = generate_stream_log_table_name()
    yt_client.create('table', ypath_join(yt_root, log_table_name), attributes={
        LB_META_ATTR: LogInterval([
            Subinterval('c1', 't1', 1, 10, 20),
        ]).to_meta()
    })

    assert generate_new_log_interval.run_job(
        yt_client=yt_client,
        last_generated_meta=dict(
            log_interval=LogInterval([Subinterval('c1', 't1', 0, 10, 20)]).to_meta(),
            run_id=PREV_RUN_ID
        ),
        last_tariffed_meta=dict(
            log_interval=LogInterval([Subinterval('c1', 't1', 0, 10, 20)]).to_meta(),
            run_id=PREV_RUN_ID
        ),
        log_dir=yt_root
    ) is None
    assert 'The latest log slice is not comparable' in caplog.text
