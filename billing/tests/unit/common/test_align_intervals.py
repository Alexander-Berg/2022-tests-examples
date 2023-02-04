# -*- coding: utf-8 -*-

import arrow
import pytest

from contextlib import contextmanager
from unittest import mock

from billing.log_tariffication.py.jobs.common import align_intervals

from billing.log_tariffication.py.lib.constants import (
    RUN_ID_KEY,
    LOG_INTERVAL_KEY,
    LOG_TARIFF_META_ATTR,
    CORRECTIONS_LOG_INTERVAL_KEY,
    ACT_DT_KEY,
    ACTED_SLICE_KEY,
    ACT_SEQUENCE_POS_KEY,
    MSK_TZ,
    REF_INTERVAL_KEY_FORMATTER,
)

from billing.library.python.logfeller_utils.log_interval import (
    LogInterval,
    Subinterval,
)

from billing.log_tariffication.py.tests.constants import (
    PREV_RUN_ID,
    CURR_RUN_ID,
    NEXT_RUN_ID,
)
from billing.log_tariffication.py.tests.utils import patch_generate_run_id


TAXES_DML_DT = '2020-01-01'

NOW = arrow.get('2020-06-06 12:00:00').replace(tzinfo=MSK_TZ)


LOG_INTERVAL_TARGET = LogInterval([Subinterval('a', 'a', 0, 0, 20)])
LOG_INTERVAL_TARGET_LAG = LogInterval([Subinterval('a', 'a', 0, 0, 15)])
LOG_INTERVAL_SOURCE = LogInterval([Subinterval('a', 'a', 0, 0, 10)])
LOG_INTERVAL_SOURCE_LAG = LogInterval([Subinterval('a', 'a', 0, 0, 5)])
LOG_INTERVAL_ALIGNMENT = LogInterval([Subinterval('a', 'a', 0, 10, 20)])


@contextmanager
def patch_check_last_table_path(return_value=None):
    patch_path = 'billing.library.python.logmeta_utils.meta.check_last_table_path'
    with mock.patch(patch_path) as check_last_table_path_mock:
        if return_value:
            check_last_table_path_mock.return_value = return_value
        yield check_last_table_path_mock


@pytest.fixture
def yt_client_mock():
    with mock.patch('yt.wrapper.YtClient') as yt_client:
        yield yt_client.return_value


def target_meta(log_interval, run_id, act_dt, act_slice, corrections_interval=None):
    return {
        LOG_INTERVAL_KEY: log_interval.to_meta(),
        RUN_ID_KEY: run_id,
        ACT_DT_KEY: act_dt.strftime('%Y-%m-%d'),
        ACTED_SLICE_KEY: act_slice.to_meta(),
        REF_INTERVAL_KEY_FORMATTER('taxes'): TAXES_DML_DT,
        CORRECTIONS_LOG_INTERVAL_KEY: corrections_interval and corrections_interval.to_meta(),
    }


def source_meta(log_interval, run_id, act_dt, seq_pos=666):
    res = {
        LOG_INTERVAL_KEY: log_interval.to_meta(),
        RUN_ID_KEY: run_id,
        ACT_DT_KEY: act_dt.strftime('%Y-%m-%d'),
        ACT_SEQUENCE_POS_KEY: seq_pos,
    }
    return res


def test_need_align(yt_client_mock):
    with patch_generate_run_id(NEXT_RUN_ID):
        new_target_meta, new_source_meta, alignment_interval, target_slice = align_intervals.run_job(
            yt_client_mock,
            target_meta(LOG_INTERVAL_TARGET, CURR_RUN_ID, NOW, LOG_INTERVAL_SOURCE.end),
            target_meta(LOG_INTERVAL_TARGET, CURR_RUN_ID, NOW, LOG_INTERVAL_SOURCE.end),
            source_meta(LOG_INTERVAL_SOURCE, PREV_RUN_ID, NOW),
            source_meta(LOG_INTERVAL_SOURCE, PREV_RUN_ID, NOW),
        )

    assert new_target_meta == target_meta(LOG_INTERVAL_TARGET, NEXT_RUN_ID, NOW, LOG_INTERVAL_SOURCE.end)
    assert new_source_meta == source_meta(LOG_INTERVAL_ALIGNMENT, NEXT_RUN_ID, NOW)
    assert alignment_interval == LOG_INTERVAL_ALIGNMENT.to_meta()
    assert target_slice == LOG_INTERVAL_ALIGNMENT.end.to_meta()
    assert not yt_client_mock.create.called


def test_unprocessed_source(yt_client_mock):
    new_target_meta, new_source_meta, alignment_interval, target_slice = align_intervals.run_job(
        yt_client_mock,
        target_meta(LOG_INTERVAL_TARGET, CURR_RUN_ID, NOW, LOG_INTERVAL_SOURCE.end),
        target_meta(LOG_INTERVAL_TARGET, CURR_RUN_ID, NOW, LOG_INTERVAL_SOURCE.end),
        source_meta(LOG_INTERVAL_SOURCE_LAG, PREV_RUN_ID, NOW),
        source_meta(LOG_INTERVAL_SOURCE, PREV_RUN_ID, NOW),
    )

    assert new_target_meta is None
    assert new_source_meta is None
    assert alignment_interval is None


def test_unprocessed_target(yt_client_mock):
    new_target_meta, new_source_meta, alignment_interval, target_slice = align_intervals.run_job(
        yt_client_mock,
        target_meta(LOG_INTERVAL_TARGET_LAG, CURR_RUN_ID, NOW, LOG_INTERVAL_SOURCE.end),
        target_meta(LOG_INTERVAL_TARGET, CURR_RUN_ID, NOW, LOG_INTERVAL_SOURCE.end),
        source_meta(LOG_INTERVAL_SOURCE, PREV_RUN_ID, NOW),
        source_meta(LOG_INTERVAL_SOURCE, PREV_RUN_ID, NOW),
    )

    assert new_target_meta is None
    assert new_source_meta is None
    assert alignment_interval is None


def test_mismatch_source(yt_client_mock):
    with pytest.raises(AssertionError) as exc_info:
        align_intervals.run_job(
            yt_client_mock,
            target_meta(LOG_INTERVAL_TARGET, CURR_RUN_ID, NOW, LOG_INTERVAL_SOURCE.end),
            target_meta(LOG_INTERVAL_TARGET, CURR_RUN_ID, NOW, LOG_INTERVAL_SOURCE.end),
            source_meta(LOG_INTERVAL_SOURCE, PREV_RUN_ID, NOW),
            source_meta(LOG_INTERVAL_SOURCE_LAG, PREV_RUN_ID, NOW),
        )

    assert 'intervals mismatch' in exc_info.value.args[0]


def test_mismatch_target(yt_client_mock):
    with pytest.raises(AssertionError) as exc_info:
        align_intervals.run_job(
            yt_client_mock,
            target_meta(LOG_INTERVAL_TARGET, CURR_RUN_ID, NOW, LOG_INTERVAL_SOURCE.end),
            target_meta(LOG_INTERVAL_TARGET_LAG, CURR_RUN_ID, NOW, LOG_INTERVAL_SOURCE.end),
            source_meta(LOG_INTERVAL_SOURCE, PREV_RUN_ID, NOW),
            source_meta(LOG_INTERVAL_SOURCE, PREV_RUN_ID, NOW),
        )

    assert 'intervals mismatch' in exc_info.value.args[0]


def test_already_aligned(yt_client_mock):
    new_target_meta, new_source_meta, alignment_interval, target_slice = align_intervals.run_job(
        yt_client_mock,
        target_meta(LOG_INTERVAL_TARGET, CURR_RUN_ID, NOW, LOG_INTERVAL_SOURCE.end),
        target_meta(LOG_INTERVAL_TARGET, CURR_RUN_ID, NOW, LOG_INTERVAL_SOURCE.end),
        source_meta(LOG_INTERVAL_TARGET, PREV_RUN_ID, NOW),
        source_meta(LOG_INTERVAL_TARGET, PREV_RUN_ID, NOW),
    )

    assert new_target_meta is None
    assert new_source_meta is None
    assert alignment_interval is None


def test_target_behind(yt_client_mock):
    with pytest.raises(AssertionError) as exc_info:
        align_intervals.run_job(
            yt_client_mock,
            target_meta(LOG_INTERVAL_SOURCE, CURR_RUN_ID, NOW, LOG_INTERVAL_SOURCE.end),
            target_meta(LOG_INTERVAL_SOURCE, CURR_RUN_ID, NOW, LOG_INTERVAL_SOURCE.end),
            source_meta(LOG_INTERVAL_TARGET, PREV_RUN_ID, NOW),
            source_meta(LOG_INTERVAL_TARGET, PREV_RUN_ID, NOW),
        )

    assert 'Trying to align to a preceeding interval' in exc_info.value.args[0]


@pytest.mark.parametrize(
    'row_spec_exists', [True, False], ids=['w_row_spec', 'wo_row_spec']
)
@pytest.mark.parametrize(
    'run_id_prefix', [None, '2021-01_1'], ids=['wo_prefix', 'w_prefix']
)
def test_fill_table(yt_client_mock, row_spec_exists, run_id_prefix):
    fill_dir = '//fill/my/hole'
    run_id = f'{run_id_prefix}-{NEXT_RUN_ID}' if run_id_prefix else NEXT_RUN_ID
    fill_path = f'{fill_dir}/{run_id}'

    yt_client_mock.exists.return_value = row_spec_exists
    get_returns = [{'mock': 'schema'}, {'mock': 'meta', ACTED_SLICE_KEY: LOG_INTERVAL_SOURCE.end.to_meta()}]
    if row_spec_exists:
        get_returns.append({'row': 'spec'})
    yt_client_mock.get.side_effect = get_returns

    with patch_generate_run_id(NEXT_RUN_ID), patch_check_last_table_path(fill_path):
        new_target_meta, new_source_meta, alignment_interval, target_slice = align_intervals.run_job(
            yt_client_mock,
            target_meta(LOG_INTERVAL_TARGET, CURR_RUN_ID, NOW, LOG_INTERVAL_SOURCE.end),
            target_meta(LOG_INTERVAL_TARGET, CURR_RUN_ID, NOW, LOG_INTERVAL_SOURCE.end),
            source_meta(LOG_INTERVAL_SOURCE, PREV_RUN_ID, NOW),
            source_meta(LOG_INTERVAL_SOURCE, PREV_RUN_ID, NOW),
            [fill_dir],
            run_id_prefix,
        )

    assert new_target_meta == target_meta(LOG_INTERVAL_TARGET, run_id, NOW, LOG_INTERVAL_SOURCE.end)
    assert new_source_meta == source_meta(LOG_INTERVAL_ALIGNMENT, run_id, NOW)
    assert alignment_interval == LOG_INTERVAL_ALIGNMENT.to_meta()
    assert target_slice == LOG_INTERVAL_ALIGNMENT.end.to_meta()

    required_attributes = {
        'schema': {'mock': 'schema'},
        'optimize_for': 'scan',
        LOG_TARIFF_META_ATTR: {
            'mock': 'meta',
            LOG_INTERVAL_KEY: LOG_INTERVAL_ALIGNMENT.to_meta(),
            RUN_ID_KEY: run_id,
            ACTED_SLICE_KEY: LOG_INTERVAL_SOURCE.end.to_meta(),
        }
    }
    if row_spec_exists:
        required_attributes.update(_yql_row_spec={'row': 'spec'}),
    assert yt_client_mock.create.called_once
    assert yt_client_mock.create.call_args.args == ('table', fill_path)
    assert yt_client_mock.create.call_args.kwargs == {'attributes': required_attributes}
