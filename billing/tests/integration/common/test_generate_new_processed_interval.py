from yt.wrapper import ypath_join

from billing.library.python.logfeller_utils.log_interval import (
    LogInterval,
    Subinterval,
)
from billing.log_tariffication.py.jobs.common import generate_new_processed_interval
from billing.library.python.logfeller_utils.tests.utils import (
    generate_stream_log_table_name,
)

from billing.log_tariffication.py.lib.constants import (
    RUN_ID_KEY,
    PREVIOUS_RUN_ID_KEY,
    LOG_TARIFF_META_ATTR,
    LOG_INTERVAL_KEY,
)

from billing.log_tariffication.py.lib import utils
import arrow


def test_empty_directory(caplog, yt_client, yt_root):
    assert generate_new_processed_interval.run_job(
        yt_client=yt_client,
        last_meta=dict(
            log_interval=LogInterval([Subinterval('c1', 't1', 0, 0, 20)]).to_meta(),
        ),
        ref_dir=yt_root,
        ref_name='log_interval',
    ) is None
    assert "Reference directory is empty" in caplog.text


def test_reflog_is_behind(caplog, yt_client, yt_root):
    log_table_name = generate_stream_log_table_name()
    yt_client.create('table', ypath_join(yt_root, log_table_name), attributes={
        LOG_TARIFF_META_ATTR: {
            LOG_INTERVAL_KEY: LogInterval([Subinterval('c1', 't1', 1, 10, 20)]).to_meta()
        }
    })

    assert generate_new_processed_interval.run_job(
        yt_client=yt_client,
        last_meta=dict(
            log_interval=LogInterval([Subinterval('c1', 't1', 1, 20, 30)]).to_meta(),
        ),
        ref_dir=yt_root,
        ref_name='log_interval',
    ) is None
    assert "Reference data is outdated" in caplog.text


def test_reflog_has_no_new_data(yt_client, yt_root):
    log_table_name = generate_stream_log_table_name()
    yt_client.create('table', ypath_join(yt_root, log_table_name), attributes={
        LOG_TARIFF_META_ATTR: {
            LOG_INTERVAL_KEY: LogInterval([Subinterval('c1', 't1', 1, 10, 20)]).to_meta()
        }
    })

    res = generate_new_processed_interval.run_job(
        yt_client=yt_client,
        last_meta=dict(
            log_interval=LogInterval([Subinterval('c1', 't1', 1, 10, 20)]).to_meta(),
        ),
        ref_dir=yt_root,
        ref_name='log_interval',
    )
    assert res['log_interval'] == LogInterval([Subinterval('c1', 't1', 1, 20, 20)]).to_meta()


def test_reflog_new_interval(yt_client, yt_root):
    log_table_name = generate_stream_log_table_name()
    yt_client.create('table', ypath_join(yt_root, log_table_name), attributes={
        LOG_TARIFF_META_ATTR: {
            LOG_INTERVAL_KEY: LogInterval([Subinterval('c1', 't1', 1, 10, 20)]).to_meta()
        }
    })

    res = generate_new_processed_interval.run_job(
        yt_client=yt_client,
        last_meta=dict(
            log_interval=LogInterval([Subinterval('c1', 't1', 1, 0, 10)]).to_meta(),
            some_transparent_param='some transparent val',
        ),
        ref_dir=yt_root,
        ref_name='log_interval',
    )
    assert res['log_interval'] == LogInterval([Subinterval('c1', 't1', 1, 10, 20)]).to_meta()
    assert res['some_transparent_param'] == 'some transparent val'


def test_generate_new_run_id(yt_client, yt_root):
    log_table_name = generate_stream_log_table_name()

    yt_client.create('table', ypath_join(yt_root, log_table_name), attributes={
        LOG_TARIFF_META_ATTR: {
            LOG_INTERVAL_KEY: LogInterval([Subinterval('c1', 't1', 1, 10, 20)]).to_meta()
        }
    })

    run_key_id = utils.meta.generate_run_id(arrow.get('2021-08-18T12:30:45', 'YYYY-MM-DDTHH:mm:ss'))

    res = generate_new_processed_interval.run_job(
        yt_client=yt_client,
        last_meta=dict(
            log_interval=LogInterval([Subinterval('c1', 't1', 1, 0, 10)]).to_meta(),
            some_transparent_param='some transparent val',
            **{RUN_ID_KEY: run_key_id},
        ),
        ref_dir=yt_root,
        ref_name='log_interval',
    )

    assert res[PREVIOUS_RUN_ID_KEY] == run_key_id
    assert res[RUN_ID_KEY] > res[PREVIOUS_RUN_ID_KEY]

    assert res['log_interval'] == LogInterval([Subinterval('c1', 't1', 1, 10, 20)]).to_meta()
    assert res['some_transparent_param'] == 'some transparent val'
