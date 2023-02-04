from yt.wrapper import ypath_join

from billing.library.python.logfeller_utils.log_interval import (
    LogInterval,
    Subinterval,
    LB_META_ATTR,
)
from billing.log_tariffication.py.jobs.common import generate_new_reference_interval
from billing.library.python.logfeller_utils.tests.utils import (
    generate_stream_log_table_name,
)


def test_empty_directory(caplog, yt_client, yt_root):
    assert generate_new_reference_interval.run_job(
        yt_client=yt_client,
        last_meta=dict(
            ref_my_interval=LogInterval([Subinterval('c1', 't1', 0, 0, 20)]).to_meta(),
        ),
        ref_dir=yt_root,
        ref_name='my',
    ) is None
    assert "Reference directory is empty" in caplog.text


def test_reflog_is_behind(caplog, yt_client, yt_root):
    log_table_name = generate_stream_log_table_name()
    yt_client.create('table', ypath_join(yt_root, log_table_name), attributes={
        LB_META_ATTR: LogInterval([Subinterval('c1', 't1', 1, 10, 20)]).to_meta()
    })

    assert generate_new_reference_interval.run_job(
        yt_client=yt_client,
        last_meta=dict(
            ref_my_interval=LogInterval([Subinterval('c1', 't1', 1, 20, 30)]).to_meta(),
        ),
        ref_dir=yt_root,
        ref_name='my',
    ) is None
    assert "Reference data is outdated" in caplog.text


def test_reflog_has_no_new_data(yt_client, yt_root):
    log_table_name = generate_stream_log_table_name()
    yt_client.create('table', ypath_join(yt_root, log_table_name), attributes={
        LB_META_ATTR: LogInterval([Subinterval('c1', 't1', 1, 10, 20)]).to_meta()
    })

    res = generate_new_reference_interval.run_job(
        yt_client=yt_client,
        last_meta=dict(
            ref_my_interval=LogInterval([Subinterval('c1', 't1', 1, 10, 20)]).to_meta(),
        ),
        ref_dir=yt_root,
        ref_name='my',
    )
    assert res['ref_my_interval'] == LogInterval([Subinterval('c1', 't1', 1, 20, 20)]).to_meta()


def test_reflog_new_interval(yt_client, yt_root):
    log_table_name = generate_stream_log_table_name()
    yt_client.create('table', ypath_join(yt_root, log_table_name), attributes={
        LB_META_ATTR: LogInterval([Subinterval('c1', 't1', 1, 10, 20)]).to_meta()
    })

    res = generate_new_reference_interval.run_job(
        yt_client=yt_client,
        last_meta=dict(
            ref_my_interval=LogInterval([Subinterval('c1', 't1', 1, 0, 10)]).to_meta(),
            some_transparent_param='some transparent val',
        ),
        ref_dir=yt_root,
        ref_name='my',
    )
    assert res['ref_my_interval'] == LogInterval([Subinterval('c1', 't1', 1, 10, 20)]).to_meta()
    assert res['some_transparent_param'] == 'some transparent val'
