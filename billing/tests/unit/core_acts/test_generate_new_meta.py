# -*- coding: utf-8 -*-

import datetime
from unittest import mock
import contextlib

import pytest
import arrow

from billing.log_tariffication.py.jobs.core_acts import generate_new_meta
from billing.log_tariffication.py.lib.constants import (
    RUN_ID_KEY,
    LOG_INTERVAL_KEY,
    CORRECTIONS_LOG_INTERVAL_KEY,
    ACT_DT_KEY,
    ACTED_SLICE_KEY,
    ACT_SEQUENCE_POS_KEY,
    DAILY_ACTED_SLICE_KEY,
    MSK_TZ,
    REF_INTERVAL_KEY_FORMATTER,
    PREV_COMMANDS_TABLE,
    LAST_COMMANDS_TABLE,
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
from billing.log_tariffication.py.tests.utils import (
    patch_generate_run_id,
)


NOW = arrow.get('2020-06-06 12:00:00').replace(tzinfo=MSK_TZ)
TODAY = NOW.floor('day')
YESTERDAY = TODAY.shift(days=-1)
PAST_MONTH = NOW.shift(months=-1)
OLD_MONTH = NOW.shift(months=-2)

LOG_INTERVAL_EMPTY = LogInterval([Subinterval('a', 'a', 0, 0, 0)])
LOG_INTERVAL_1 = LogInterval([Subinterval('a', 'a', 0, 0, 10)])
LOG_INTERVAL_2 = LogInterval([Subinterval('a', 'a', 0, 10, 20)])
LOG_INTERVAL_3 = LogInterval([Subinterval('a', 'a', 0, 20, 30)])

TAXES_DML_DT = '2020-01-01'
DOCS_PARAMS_DML_DT = '2020-01-01'
PRODUCT_DML_DT = '2020-01-01'


@contextlib.contextmanager
def patch_now(dt):
    with mock.patch('arrow.now', lambda: dt):
        yield


def mnclose_state(dt, state, run_id=None):
    res = {
        'dt': dt.strftime('%Y-%m'),
        'status': state,
    }
    if run_id is not None:
        res['id'] = run_id
    return res


def tariffed_meta(log_interval, corrections_interval=None):
    meta = {LOG_INTERVAL_KEY: log_interval.to_meta()}
    if corrections_interval:
        meta[CORRECTIONS_LOG_INTERVAL_KEY] = corrections_interval.to_meta()
    return meta


def interim_meta(log_interval, run_id, act_dt, act_slice, corrections_interval=None):
    return {
        LOG_INTERVAL_KEY: log_interval.to_meta(),
        RUN_ID_KEY: run_id,
        ACT_DT_KEY: act_dt.strftime('%Y-%m-%d'),
        ACTED_SLICE_KEY: act_slice.to_meta(),
        REF_INTERVAL_KEY_FORMATTER('taxes'): TAXES_DML_DT,
        REF_INTERVAL_KEY_FORMATTER('docs_params'): DOCS_PARAMS_DML_DT,
        REF_INTERVAL_KEY_FORMATTER('product'): PRODUCT_DML_DT,
        CORRECTIONS_LOG_INTERVAL_KEY: corrections_interval and corrections_interval.to_meta(),
    }


def commands_meta(log_interval, prev_run_id, last_run_id, act_dt, act_slice, daily_act_slice, corrections_interval=None):
    return {
        LOG_INTERVAL_KEY: log_interval.to_meta(),
        RUN_ID_KEY: last_run_id,
        ACT_DT_KEY: act_dt.strftime('%Y-%m-%d'),
        ACTED_SLICE_KEY: act_slice.to_meta(),
        DAILY_ACTED_SLICE_KEY: daily_act_slice.to_meta(),
        REF_INTERVAL_KEY_FORMATTER('taxes'): TAXES_DML_DT,
        REF_INTERVAL_KEY_FORMATTER('docs_params'): DOCS_PARAMS_DML_DT,
        REF_INTERVAL_KEY_FORMATTER('product'): PRODUCT_DML_DT,
        CORRECTIONS_LOG_INTERVAL_KEY: corrections_interval and corrections_interval.to_meta(),
        PREV_COMMANDS_TABLE: prev_run_id,
        LAST_COMMANDS_TABLE: last_run_id,
    }


def enqueued_commands(run_id):
    return {
        RUN_ID_KEY: run_id
    }


def act_meta(log_interval, daily_slice, run_id, act_dt, seq_pos=666):
    return {
        LOG_INTERVAL_KEY: log_interval.to_meta(),
        RUN_ID_KEY: run_id,
        ACT_DT_KEY: act_dt.strftime('%Y-%m-%d'),
        ACT_SEQUENCE_POS_KEY: seq_pos,
        DAILY_ACTED_SLICE_KEY: daily_slice.to_meta(),
    }


def daily_meta(log_interval, act_slice, run_id, act_dt, seq_pos=666, req_interval=None):
    return {
        LOG_INTERVAL_KEY: log_interval.to_meta(),
        RUN_ID_KEY: run_id,
        ACT_DT_KEY: act_dt.strftime('%Y-%m-%d'),
        ACT_SEQUENCE_POS_KEY: seq_pos,
        ACTED_SLICE_KEY: act_slice.to_meta(),
        REF_INTERVAL_KEY_FORMATTER('act_requests'): req_interval and req_interval.to_meta(),
    }


def everything_is_empty(new_meta_res):
    if any(
        [new_meta_res.new_commands_meta,
         new_meta_res.new_interim_meta,
         new_meta_res.new_acts_meta,
         new_meta_res.new_daily_acts_meta]
    ):
        return False
    return True


class TestInterim:
    def test_unprocessed(self):
        new_meta_res = generate_new_meta.ActMetaGen(
            mnclose_state(PAST_MONTH, 'closed'),
            tariffed_meta(LOG_INTERVAL_3),
            interim_meta(LOG_INTERVAL_3, CURR_RUN_ID, NOW, LOG_INTERVAL_1.end),
            interim_meta(LOG_INTERVAL_2, PREV_RUN_ID, NOW, LOG_INTERVAL_1.end),
            act_meta(LOG_INTERVAL_1, LOG_INTERVAL_1.beginning, PREV_RUN_ID, NOW),
            act_meta(LOG_INTERVAL_1, LOG_INTERVAL_1.beginning, PREV_RUN_ID, NOW),
            daily_meta(LOG_INTERVAL_1, LOG_INTERVAL_1.beginning, PREV_RUN_ID, NOW),
            daily_meta(LOG_INTERVAL_1, LOG_INTERVAL_1.beginning, PREV_RUN_ID, NOW, req_interval=LOG_INTERVAL_1),
            commands_meta(LOG_INTERVAL_1, PREV_RUN_ID, PREV_RUN_ID, NOW, LOG_INTERVAL_1.end, LOG_INTERVAL_1.end),
            commands_meta(LOG_INTERVAL_1, PREV_RUN_ID, PREV_RUN_ID, NOW, LOG_INTERVAL_1.end, LOG_INTERVAL_1.end),
            enqueued_commands(PREV_RUN_ID),
            datetime.time(11, 0),
            datetime.time(20, 0),
            TAXES_DML_DT,
            DOCS_PARAMS_DML_DT,
            PRODUCT_DML_DT,
        ).do()

        assert everything_is_empty(new_meta_res)

    def test_mismatched_prev(self):
        with pytest.raises(AssertionError) as exc_info:
            generate_new_meta.ActMetaGen(
                mnclose_state(PAST_MONTH, 'closed'),
                tariffed_meta(LOG_INTERVAL_3),
                interim_meta(LOG_INTERVAL_3, CURR_RUN_ID, NOW, LOG_INTERVAL_1.end),
                interim_meta(LogInterval([Subinterval('a', 'a', 0, 20, 31)]), PREV_RUN_ID, NOW, LOG_INTERVAL_1.end),
                act_meta(LOG_INTERVAL_1, LOG_INTERVAL_1.beginning, PREV_RUN_ID, NOW),
                act_meta(LOG_INTERVAL_1, LOG_INTERVAL_1.beginning, PREV_RUN_ID, NOW),
                daily_meta(LOG_INTERVAL_1, LOG_INTERVAL_1.beginning, PREV_RUN_ID, NOW),
                daily_meta(LOG_INTERVAL_1, LOG_INTERVAL_1.beginning, PREV_RUN_ID, NOW, req_interval=LOG_INTERVAL_1),
                commands_meta(LOG_INTERVAL_1, PREV_RUN_ID, PREV_RUN_ID, NOW, LOG_INTERVAL_1.end, LOG_INTERVAL_1.end),
                commands_meta(LOG_INTERVAL_1, PREV_RUN_ID, PREV_RUN_ID, NOW, LOG_INTERVAL_1.end, LOG_INTERVAL_1.end),
                enqueued_commands(PREV_RUN_ID),
                datetime.time(11, 0),
                datetime.time(20, 0),
                TAXES_DML_DT,
                DOCS_PARAMS_DML_DT,
                PRODUCT_DML_DT,
            ).do()
        assert 'intervals mismatch' in exc_info.value.args[0]

    @pytest.mark.parametrize(
        'now_dt, switch_hour, res_dt',
        [
            pytest.param(NOW, 13, YESTERDAY, id='yesterday'),
            pytest.param(NOW, 12, TODAY, id='today'),
        ]
    )
    def test_day_switch(self, now_dt, switch_hour, res_dt):
        with patch_now(now_dt), patch_generate_run_id(NEXT_RUN_ID):
            new_meta_res = generate_new_meta.ActMetaGen(
                mnclose_state(PAST_MONTH, 'closed'),
                tariffed_meta(LOG_INTERVAL_3),
                interim_meta(LOG_INTERVAL_2, CURR_RUN_ID, NOW, LOG_INTERVAL_1.end),
                interim_meta(LOG_INTERVAL_2, PREV_RUN_ID, NOW, LOG_INTERVAL_1.end),
                act_meta(LOG_INTERVAL_2, LOG_INTERVAL_2.beginning, PREV_RUN_ID, NOW),
                act_meta(LOG_INTERVAL_2, LOG_INTERVAL_2.beginning, PREV_RUN_ID, NOW),
                daily_meta(LOG_INTERVAL_1, LOG_INTERVAL_1.beginning, PREV_RUN_ID, NOW),
                daily_meta(LOG_INTERVAL_1, LOG_INTERVAL_1.beginning, PREV_RUN_ID, NOW, req_interval=LOG_INTERVAL_1),
                commands_meta(LOG_INTERVAL_1, PREV_RUN_ID, PREV_RUN_ID, NOW, LOG_INTERVAL_1.end, LOG_INTERVAL_1.end),
                commands_meta(LOG_INTERVAL_1, PREV_RUN_ID, PREV_RUN_ID, NOW, LOG_INTERVAL_1.end, LOG_INTERVAL_1.end),
                enqueued_commands(PREV_RUN_ID),
                datetime.time(switch_hour, 0),
                datetime.time(20, 0),
                TAXES_DML_DT,
                DOCS_PARAMS_DML_DT,
                PRODUCT_DML_DT,
            ).do()

        assert new_meta_res.new_interim_meta == interim_meta(
            LOG_INTERVAL_3,
            NEXT_RUN_ID,
            res_dt,
            LOG_INTERVAL_2.end,
        )
        assert new_meta_res.new_acts_meta is None
        assert new_meta_res.new_daily_acts_meta is None
        assert new_meta_res.new_commands_meta is None

    @pytest.mark.parametrize(
        'now_dt, switch_hour',
        [
            pytest.param(NOW, 13, id='before_switch'),
            pytest.param(NOW, 12, id='after_switch'),
        ]
    )
    def test_forced_by_acts(self, now_dt, switch_hour):
        with patch_now(now_dt), patch_generate_run_id(NEXT_RUN_ID):
            new_meta_res = generate_new_meta.ActMetaGen(
                mnclose_state(OLD_MONTH, 'closed'),
                tariffed_meta(LOG_INTERVAL_3),
                interim_meta(LOG_INTERVAL_2, CURR_RUN_ID, NOW, LOG_INTERVAL_1.end),
                interim_meta(LOG_INTERVAL_2, PREV_RUN_ID, NOW, LOG_INTERVAL_1.end),
                act_meta(LOG_INTERVAL_2, LOG_INTERVAL_2.beginning, OLD_MONTH.strftime('%Y-%m'), NOW),
                act_meta(LOG_INTERVAL_2, LOG_INTERVAL_2.beginning, OLD_MONTH.strftime('%Y-%m'), NOW),
                daily_meta(LOG_INTERVAL_1, LOG_INTERVAL_1.beginning, PREV_RUN_ID, NOW),
                daily_meta(LOG_INTERVAL_1, LOG_INTERVAL_1.beginning, PREV_RUN_ID, NOW, req_interval=LOG_INTERVAL_1),
                commands_meta(LOG_INTERVAL_1, PREV_RUN_ID, PREV_RUN_ID, NOW, LOG_INTERVAL_1.end, LOG_INTERVAL_1.end),
                commands_meta(LOG_INTERVAL_1, PREV_RUN_ID, PREV_RUN_ID, NOW, LOG_INTERVAL_1.end, LOG_INTERVAL_1.end),
                enqueued_commands(PREV_RUN_ID),
                datetime.time(switch_hour, 0),
                datetime.time(20, 0),
                TAXES_DML_DT,
                DOCS_PARAMS_DML_DT,
                PRODUCT_DML_DT,
            ).do()

        assert new_meta_res.new_interim_meta == interim_meta(
            LOG_INTERVAL_3,
            NEXT_RUN_ID,
            PAST_MONTH.ceil('month'),
            LOG_INTERVAL_2.end,
        )
        assert new_meta_res.new_acts_meta is None
        assert new_meta_res.new_daily_acts_meta is None
        assert new_meta_res.new_commands_meta is None

    def test_forced_by_acts_unprocessed_acts(self):
        with pytest.raises(AssertionError) as exc_info:
            with patch_now(NOW), patch_generate_run_id(NEXT_RUN_ID):
                generate_new_meta.ActMetaGen(
                    mnclose_state(OLD_MONTH, 'closed'),
                    tariffed_meta(LOG_INTERVAL_3),
                    interim_meta(LOG_INTERVAL_2, CURR_RUN_ID, NOW, LOG_INTERVAL_1.end),
                    interim_meta(LOG_INTERVAL_2, PREV_RUN_ID, NOW, LOG_INTERVAL_1.end),
                    act_meta(LOG_INTERVAL_2, LOG_INTERVAL_2.beginning, PAST_MONTH.strftime('%Y-%m'), NOW),
                    act_meta(LOG_INTERVAL_2, LOG_INTERVAL_2.beginning, PAST_MONTH.strftime('%Y-%m'), NOW),
                    daily_meta(LOG_INTERVAL_1, LOG_INTERVAL_1.beginning, PREV_RUN_ID, NOW),
                    daily_meta(LOG_INTERVAL_1, LOG_INTERVAL_1.beginning, PREV_RUN_ID, NOW, req_interval=LOG_INTERVAL_1),
                    commands_meta(LOG_INTERVAL_1, PREV_RUN_ID, PREV_RUN_ID, NOW, LOG_INTERVAL_1.end, LOG_INTERVAL_1.end),
                    commands_meta(LOG_INTERVAL_1, PREV_RUN_ID, PREV_RUN_ID, NOW, LOG_INTERVAL_1.end, LOG_INTERVAL_1.end),
                    enqueued_commands(PREV_RUN_ID),
                    datetime.time(11, 0),
                    datetime.time(20, 0),
                    TAXES_DML_DT,
                    DOCS_PARAMS_DML_DT,
                    PRODUCT_DML_DT,
                ).do()

        assert 'acts not processed with finished generation' in exc_info.value.args[0]

    def test_no_new_tariffed(self):
        with patch_now(NOW):
            new_meta_res = generate_new_meta.ActMetaGen(
                mnclose_state(PAST_MONTH, 'closed'),
                tariffed_meta(LOG_INTERVAL_2),
                interim_meta(LOG_INTERVAL_2, CURR_RUN_ID, NOW, LOG_INTERVAL_1.end),
                interim_meta(LOG_INTERVAL_2, PREV_RUN_ID, NOW, LOG_INTERVAL_1.end),
                act_meta(LOG_INTERVAL_2, LOG_INTERVAL_2.beginning, PREV_RUN_ID, NOW),
                act_meta(LOG_INTERVAL_2, LOG_INTERVAL_2.beginning, PREV_RUN_ID, NOW),
                daily_meta(LOG_INTERVAL_1, LOG_INTERVAL_1.beginning, PREV_RUN_ID, NOW),
                daily_meta(LOG_INTERVAL_1, LOG_INTERVAL_1.beginning, PREV_RUN_ID, NOW, req_interval=LOG_INTERVAL_1),
                commands_meta(LOG_INTERVAL_1, PREV_RUN_ID, PREV_RUN_ID, NOW, LOG_INTERVAL_1.end, LOG_INTERVAL_1.end),
                commands_meta(LOG_INTERVAL_1, PREV_RUN_ID, PREV_RUN_ID, NOW, LOG_INTERVAL_1.end, LOG_INTERVAL_1.end),
                enqueued_commands(PREV_RUN_ID),
                datetime.time(13, 0),
                datetime.time(20, 0),
                TAXES_DML_DT,
                DOCS_PARAMS_DML_DT,
                PRODUCT_DML_DT,
            ).do()

        assert everything_is_empty(new_meta_res)

    def test_mismatched_tariffed(self):
        with patch_now(NOW):
            new_meta_res = generate_new_meta.ActMetaGen(
                mnclose_state(PAST_MONTH, 'closed'),
                tariffed_meta(LogInterval([Subinterval('a', 'a', 1, 0, 10)])),
                interim_meta(LOG_INTERVAL_2, CURR_RUN_ID, NOW, LOG_INTERVAL_1.end),
                interim_meta(LOG_INTERVAL_2, PREV_RUN_ID, NOW, LOG_INTERVAL_1.end),
                act_meta(LOG_INTERVAL_2, LOG_INTERVAL_2.beginning, PREV_RUN_ID, NOW),
                act_meta(LOG_INTERVAL_2, LOG_INTERVAL_2.beginning, PREV_RUN_ID, NOW),
                daily_meta(LOG_INTERVAL_1, LOG_INTERVAL_1.beginning, PREV_RUN_ID, NOW),
                daily_meta(LOG_INTERVAL_1, LOG_INTERVAL_1.beginning, PREV_RUN_ID, NOW, req_interval=LOG_INTERVAL_1),
                commands_meta(LOG_INTERVAL_1, PREV_RUN_ID, PREV_RUN_ID, NOW, LOG_INTERVAL_1.end, LOG_INTERVAL_1.end),
                commands_meta(LOG_INTERVAL_1, PREV_RUN_ID, PREV_RUN_ID, NOW, LOG_INTERVAL_1.end, LOG_INTERVAL_1.end),
                enqueued_commands(PREV_RUN_ID),
                datetime.time(13, 0),
                datetime.time(20, 0),
                TAXES_DML_DT,
                DOCS_PARAMS_DML_DT,
                PRODUCT_DML_DT,
            ).do()

        assert everything_is_empty(new_meta_res)

    def test_empty_tariffed(self):
        with patch_now(NOW):
            new_meta_res = generate_new_meta.ActMetaGen(
                mnclose_state(PAST_MONTH, 'closed'),
                tariffed_meta(LogInterval([
                    Subinterval('a', 'a', 0, 10, 20),
                    Subinterval('a', 'a', 1, 0, 0),
                ])),
                interim_meta(LOG_INTERVAL_2, CURR_RUN_ID, NOW, LOG_INTERVAL_1.end),
                interim_meta(LOG_INTERVAL_2, PREV_RUN_ID, NOW, LOG_INTERVAL_1.end),
                act_meta(LOG_INTERVAL_2, LOG_INTERVAL_2.beginning, PREV_RUN_ID, NOW),
                act_meta(LOG_INTERVAL_2, LOG_INTERVAL_2.beginning, PREV_RUN_ID, NOW),
                daily_meta(LOG_INTERVAL_1, LOG_INTERVAL_1.beginning, PREV_RUN_ID, NOW),
                daily_meta(LOG_INTERVAL_1, LOG_INTERVAL_1.beginning, PREV_RUN_ID, NOW, req_interval=LOG_INTERVAL_1),
                commands_meta(LOG_INTERVAL_1, PREV_RUN_ID, PREV_RUN_ID, NOW, LOG_INTERVAL_1.end, LOG_INTERVAL_1.end),
                commands_meta(LOG_INTERVAL_1, PREV_RUN_ID, PREV_RUN_ID, NOW, LOG_INTERVAL_1.end, LOG_INTERVAL_1.end),
                enqueued_commands(PREV_RUN_ID),
                datetime.time(13, 0),
                datetime.time(20, 0),
                TAXES_DML_DT,
                DOCS_PARAMS_DML_DT,
                PRODUCT_DML_DT,
            ).do()

        assert everything_is_empty(new_meta_res)

    def test_inconsistent_run_id(self):
        with pytest.raises(AssertionError) as exc_info:
            with patch_now(NOW), patch_generate_run_id(PREV_RUN_ID):
                generate_new_meta.ActMetaGen(
                    mnclose_state(PAST_MONTH, 'closed'),
                    tariffed_meta(LOG_INTERVAL_3),
                    interim_meta(LOG_INTERVAL_2, CURR_RUN_ID, NOW, LOG_INTERVAL_1.end),
                    interim_meta(LOG_INTERVAL_2, CURR_RUN_ID, NOW, LOG_INTERVAL_1.end),
                    act_meta(LOG_INTERVAL_2, LOG_INTERVAL_2.beginning, PREV_RUN_ID, NOW),
                    act_meta(LOG_INTERVAL_2, LOG_INTERVAL_2.beginning, PREV_RUN_ID, NOW),
                    daily_meta(LOG_INTERVAL_1, LOG_INTERVAL_1.beginning, PREV_RUN_ID, NOW),
                    daily_meta(LOG_INTERVAL_1, LOG_INTERVAL_1.beginning, PREV_RUN_ID, NOW, req_interval=LOG_INTERVAL_1),
                    commands_meta(LOG_INTERVAL_1, PREV_RUN_ID, PREV_RUN_ID, NOW, LOG_INTERVAL_1.end, LOG_INTERVAL_1.end),
                    commands_meta(LOG_INTERVAL_1, PREV_RUN_ID, PREV_RUN_ID, NOW, LOG_INTERVAL_1.end, LOG_INTERVAL_1.end),
                    enqueued_commands(PREV_RUN_ID),
                    datetime.time(11, 0),
                    datetime.time(20, 0),
                    TAXES_DML_DT,
                    DOCS_PARAMS_DML_DT,
                    PRODUCT_DML_DT,
                ).do()

        assert 'inconsequential run_id for interim rows' in exc_info.value.args[0]

    @pytest.mark.parametrize(
        'act_interval, tariffed_interval, res_interval',
        [
            pytest.param(
                LogInterval([Subinterval('a', 'a', 0, 0, 0)]),
                LogInterval([Subinterval('a', 'a', 0, 0, 0)]),
                LogInterval([Subinterval('a', 'a', 0, 0, 0)]),
                id='initial conditions',
            ),
            pytest.param(
                LogInterval([Subinterval('a', 'a', 0, 0, 1)]),
                LogInterval([Subinterval('a', 'a', 0, 1, 2)]),
                LogInterval([Subinterval('a', 'a', 0, 1, 2)]),
                id='new interval',
            ),
            pytest.param(
                LogInterval([Subinterval('a', 'a', 0, 0, 1)]),
                LogInterval([Subinterval('a', 'a', 0, 3, 5)]),
                LogInterval([Subinterval('a', 'a', 0, 1, 5)]),
                id='gap in intervals',
            ),
            pytest.param(
                LogInterval([Subinterval('a', 'a', 0, 0, 1)]),
                LogInterval([Subinterval('a', 'a', 0, 0, 1)]),
                LogInterval([Subinterval('a', 'a', 0, 1, 1)]),
                id='tariffed interval is processed',
            ),
            pytest.param(
                LogInterval([Subinterval('a', 'a', 0, 0, 1)]),
                LogInterval([Subinterval('a', 'a', 0, 1, 1)]),
                LogInterval([Subinterval('a', 'a', 0, 1, 1)]),
                id='empty interval',
            ),
            pytest.param(
                LogInterval([Subinterval('a', 'a', 0, 0, 1)]),
                LogInterval([Subinterval('a', 'a', 0, 3, 3)]),
                LogInterval([Subinterval('a', 'a', 0, 1, 3)]),
                id='empty interval w gap',
            ),
            pytest.param(
                LogInterval([Subinterval('a', 'a', 0, 1, 1)]),
                LogInterval([Subinterval('a', 'a', 0, 5, 5)]),
                LogInterval([Subinterval('a', 'a', 0, 1, 5)]),
                id='new interval w gap',
            ),
            pytest.param(
                LogInterval([Subinterval('a', 'a', 0, 0, 0)]),
                LogInterval([Subinterval('a', 'a', 0, 0, 1)]),
                LogInterval([Subinterval('a', 'a', 0, 0, 1)]),
                id='start',
            ),
            pytest.param(
                LogInterval([Subinterval('a', 'a', 0, 0, 0)]),
                None,
                LogInterval([Subinterval('a', 'a', 0, 0, 0)]),
                id='not tariffed',
            ),
        ],
    )
    def test_corrections_interval(self, act_interval, tariffed_interval, res_interval):
        with patch_now(NOW), patch_generate_run_id(NEXT_RUN_ID):
            new_meta_res = generate_new_meta.ActMetaGen(
                mnclose_state(OLD_MONTH, 'closed'),
                tariffed_meta(LOG_INTERVAL_3, tariffed_interval),
                interim_meta(LOG_INTERVAL_2, CURR_RUN_ID, NOW, LOG_INTERVAL_1.end),
                interim_meta(LOG_INTERVAL_2, PREV_RUN_ID, NOW, LOG_INTERVAL_1.end, act_interval),
                act_meta(LOG_INTERVAL_2, LOG_INTERVAL_2.beginning, OLD_MONTH.strftime('%Y-%m'), NOW),
                act_meta(LOG_INTERVAL_2, LOG_INTERVAL_2.beginning, OLD_MONTH.strftime('%Y-%m'), NOW),
                daily_meta(LOG_INTERVAL_1, LOG_INTERVAL_1.beginning, PREV_RUN_ID, NOW),
                daily_meta(LOG_INTERVAL_1, LOG_INTERVAL_1.beginning, PREV_RUN_ID, NOW, req_interval=LOG_INTERVAL_1),
                commands_meta(LOG_INTERVAL_1, PREV_RUN_ID, PREV_RUN_ID, NOW, LOG_INTERVAL_1.end, LOG_INTERVAL_1.end),
                commands_meta(LOG_INTERVAL_1, PREV_RUN_ID, PREV_RUN_ID, NOW, LOG_INTERVAL_1.end, LOG_INTERVAL_1.end),
                enqueued_commands(PREV_RUN_ID),
                datetime.time(12, 0),
                datetime.time(20, 0),
                TAXES_DML_DT,
                DOCS_PARAMS_DML_DT,
                PRODUCT_DML_DT,
            ).do()

        assert new_meta_res.new_interim_meta == interim_meta(
            LOG_INTERVAL_3,
            NEXT_RUN_ID,
            PAST_MONTH.ceil('month'),
            LOG_INTERVAL_2.end,
            res_interval,
        )
        assert new_meta_res.new_acts_meta is None
        assert new_meta_res.new_daily_acts_meta is None
        assert new_meta_res.new_commands_meta is None

    def test_corrections_interval_error(self):
        with pytest.raises(AssertionError) as exc_info:
            with patch_now(NOW), patch_generate_run_id(NEXT_RUN_ID):
                generate_new_meta.ActMetaGen(
                    mnclose_state(OLD_MONTH, 'closed'),
                    tariffed_meta(LOG_INTERVAL_3, LogInterval([Subinterval('a', 'a', 0, 0, 1)])),
                    interim_meta(LOG_INTERVAL_2, CURR_RUN_ID, NOW, LOG_INTERVAL_1.end),
                    interim_meta(LOG_INTERVAL_2, PREV_RUN_ID, NOW, LOG_INTERVAL_1.end, None),
                    act_meta(LOG_INTERVAL_2, LOG_INTERVAL_2.beginning, OLD_MONTH.strftime('%Y-%m'), NOW),
                    act_meta(LOG_INTERVAL_2, LOG_INTERVAL_2.beginning, OLD_MONTH.strftime('%Y-%m'), NOW),
                    daily_meta(LOG_INTERVAL_1, LOG_INTERVAL_1.beginning, PREV_RUN_ID, NOW),
                    daily_meta(LOG_INTERVAL_1, LOG_INTERVAL_1.beginning, PREV_RUN_ID, NOW, req_interval=LOG_INTERVAL_1),
                    commands_meta(LOG_INTERVAL_1, PREV_RUN_ID, PREV_RUN_ID, NOW, LOG_INTERVAL_1.end, LOG_INTERVAL_1.end),
                    commands_meta(LOG_INTERVAL_1, PREV_RUN_ID, PREV_RUN_ID, NOW, LOG_INTERVAL_1.end, LOG_INTERVAL_1.end),
                    enqueued_commands(PREV_RUN_ID),
                    datetime.time(12, 0),
                    datetime.time(20, 0),
                    TAXES_DML_DT,
                    DOCS_PARAMS_DML_DT,
                    PRODUCT_DML_DT,
                ).do()
        assert 'Define default corrections interval for acts.' in exc_info.value.args[0]

    def test_broken_corrections_interval(self):
        """Заакчено больше чем затарифицированно"""
        with pytest.raises(AssertionError) as exc_info:
            with patch_now(NOW), patch_generate_run_id(NEXT_RUN_ID):
                generate_new_meta.ActMetaGen(
                    mnclose_state(OLD_MONTH, 'closed'),
                    tariffed_meta(LOG_INTERVAL_3, LogInterval([Subinterval('a', 'a', 0, 2, 3)])),
                    interim_meta(LOG_INTERVAL_2, CURR_RUN_ID, NOW, LOG_INTERVAL_1.end),
                    interim_meta(LOG_INTERVAL_2, PREV_RUN_ID, NOW, LOG_INTERVAL_1.end,
                                 LogInterval([Subinterval('a', 'a', 0, 3, 4)])),
                    act_meta(LOG_INTERVAL_2, LOG_INTERVAL_2.beginning, OLD_MONTH.strftime('%Y-%m'), NOW),
                    act_meta(LOG_INTERVAL_2, LOG_INTERVAL_2.beginning, OLD_MONTH.strftime('%Y-%m'), NOW),
                    daily_meta(LOG_INTERVAL_1, LOG_INTERVAL_1.beginning, PREV_RUN_ID, NOW),
                    daily_meta(LOG_INTERVAL_1, LOG_INTERVAL_1.beginning, PREV_RUN_ID, NOW, req_interval=LOG_INTERVAL_1),
                    commands_meta(LOG_INTERVAL_1, PREV_RUN_ID, PREV_RUN_ID, NOW, LOG_INTERVAL_1.end, LOG_INTERVAL_1.end),
                    commands_meta(LOG_INTERVAL_1, PREV_RUN_ID, PREV_RUN_ID, NOW, LOG_INTERVAL_1.end, LOG_INTERVAL_1.end),
                    enqueued_commands(PREV_RUN_ID),
                    datetime.time(12, 0),
                    datetime.time(13, 0),
                    TAXES_DML_DT,
                    DOCS_PARAMS_DML_DT,
                    PRODUCT_DML_DT,
                ).do()
        assert 'Broken corrections interval.' in exc_info.value.args[0]

    def test_with_bigger_processed_commands_meta(self):
        """
        Есть новое тарифицированное по отношению к мете интеримов,
        но относительно меты команд все уже обработано
        """
        with patch_now(NOW), patch_generate_run_id(NEXT_RUN_ID):
            new_meta_res = generate_new_meta.ActMetaGen(
                mnclose_state(PAST_MONTH, 'closed'),
                tariffed_meta(LOG_INTERVAL_2),
                interim_meta(LOG_INTERVAL_1, CURR_RUN_ID, NOW, LOG_INTERVAL_1.end),
                interim_meta(LOG_INTERVAL_1, PREV_RUN_ID, NOW, LOG_INTERVAL_1.end),
                act_meta(LOG_INTERVAL_1, LOG_INTERVAL_2.beginning, PREV_RUN_ID, NOW),
                act_meta(LOG_INTERVAL_1, LOG_INTERVAL_2.beginning, PREV_RUN_ID, NOW),
                daily_meta(LOG_INTERVAL_1, LOG_INTERVAL_1.beginning, PREV_RUN_ID, NOW),
                daily_meta(LOG_INTERVAL_1, LOG_INTERVAL_1.beginning, PREV_RUN_ID, NOW, req_interval=LOG_INTERVAL_1),
                commands_meta(LOG_INTERVAL_2, PREV_RUN_ID, PREV_RUN_ID, NOW, LOG_INTERVAL_1.end, LOG_INTERVAL_1.end),
                commands_meta(LOG_INTERVAL_2, PREV_RUN_ID, PREV_RUN_ID, NOW, LOG_INTERVAL_1.end, LOG_INTERVAL_1.end),
                enqueued_commands(PREV_RUN_ID),
                datetime.time(13, 0),
                datetime.time(0, 0),
                TAXES_DML_DT,
                DOCS_PARAMS_DML_DT,
                PRODUCT_DML_DT,
            ).do()

        assert everything_is_empty(new_meta_res)


class TestActs:
    def test_unprocessed(self):
        new_meta_tes = generate_new_meta.ActMetaGen(
            mnclose_state(PAST_MONTH, 'closed'),
            tariffed_meta(LOG_INTERVAL_3),
            interim_meta(LOG_INTERVAL_2, CURR_RUN_ID, NOW, LOG_INTERVAL_1.end),
            interim_meta(LOG_INTERVAL_2, CURR_RUN_ID, NOW, LOG_INTERVAL_1.end),
            act_meta(LOG_INTERVAL_2, LOG_INTERVAL_1.end, CURR_RUN_ID, PAST_MONTH),
            act_meta(LOG_INTERVAL_1, LOG_INTERVAL_1.end, PREV_RUN_ID, PAST_MONTH),
            daily_meta(LOG_INTERVAL_1, LOG_INTERVAL_1.beginning, PREV_RUN_ID, NOW),
            daily_meta(LOG_INTERVAL_1, LOG_INTERVAL_1.beginning, PREV_RUN_ID, NOW, req_interval=LOG_INTERVAL_1),
            commands_meta(LOG_INTERVAL_1, PREV_RUN_ID, PREV_RUN_ID, NOW, LOG_INTERVAL_1.end, LOG_INTERVAL_1.end),
            commands_meta(LOG_INTERVAL_1, PREV_RUN_ID, PREV_RUN_ID, NOW, LOG_INTERVAL_1.end, LOG_INTERVAL_1.end),
            enqueued_commands(PREV_RUN_ID),
            datetime.time(11, 0),
            datetime.time(0, 0),
            TAXES_DML_DT,
            DOCS_PARAMS_DML_DT,
            PRODUCT_DML_DT,
        ).do()

        assert everything_is_empty(new_meta_tes)

    def test_mismatched_prev(self):
        with pytest.raises(AssertionError) as exc_info:
            generate_new_meta.ActMetaGen(
                mnclose_state(PAST_MONTH, 'closed'),
                tariffed_meta(LOG_INTERVAL_3),
                interim_meta(LOG_INTERVAL_2, CURR_RUN_ID, NOW, LOG_INTERVAL_1.end),
                interim_meta(LOG_INTERVAL_2, CURR_RUN_ID, NOW, LOG_INTERVAL_1.end),
                act_meta(LOG_INTERVAL_2, LOG_INTERVAL_1.end, CURR_RUN_ID, PAST_MONTH),
                act_meta(LogInterval([Subinterval('a', 'a', 0, 11, 21)]), LOG_INTERVAL_1.end, PREV_RUN_ID, PAST_MONTH),
                daily_meta(LOG_INTERVAL_1, LOG_INTERVAL_1.beginning, PREV_RUN_ID, NOW),
                daily_meta(LOG_INTERVAL_1, LOG_INTERVAL_1.beginning, PREV_RUN_ID, NOW, req_interval=LOG_INTERVAL_1),
                commands_meta(LOG_INTERVAL_1, PREV_RUN_ID, PREV_RUN_ID, NOW, LOG_INTERVAL_1.end, LOG_INTERVAL_1.end),
                commands_meta(LOG_INTERVAL_1, PREV_RUN_ID, PREV_RUN_ID, NOW, LOG_INTERVAL_1.end, LOG_INTERVAL_1.end),
                enqueued_commands(PREV_RUN_ID),
                datetime.time(11, 0),
                datetime.time(0, 0),
                TAXES_DML_DT,
                DOCS_PARAMS_DML_DT,
                PRODUCT_DML_DT,
            ).do()

        assert 'intervals mismatch' in exc_info.value.args[0]

    def test_mnclose_running_completed(self):
        with patch_now(NOW):
            new_meta_res = generate_new_meta.ActMetaGen(
                mnclose_state(PAST_MONTH, 'open'),
                tariffed_meta(LOG_INTERVAL_3),
                interim_meta(LOG_INTERVAL_2, CURR_RUN_ID, NOW, LOG_INTERVAL_1.end),
                interim_meta(LOG_INTERVAL_2, CURR_RUN_ID, NOW, LOG_INTERVAL_1.end),
                act_meta(LOG_INTERVAL_1, LOG_INTERVAL_1.end, PAST_MONTH.strftime('%Y-%m'), PAST_MONTH),
                act_meta(LOG_INTERVAL_1, LOG_INTERVAL_1.end, PAST_MONTH.strftime('%Y-%m'), PAST_MONTH),
                daily_meta(LOG_INTERVAL_1, LOG_INTERVAL_1.beginning, PREV_RUN_ID, NOW),
                daily_meta(LOG_INTERVAL_1, LOG_INTERVAL_1.beginning, PREV_RUN_ID, NOW, req_interval=LOG_INTERVAL_1),
                commands_meta(LOG_INTERVAL_1, PREV_RUN_ID, PREV_RUN_ID, NOW, LOG_INTERVAL_1.end, LOG_INTERVAL_1.end),
                commands_meta(LOG_INTERVAL_1, PREV_RUN_ID, PREV_RUN_ID, NOW, LOG_INTERVAL_1.end, LOG_INTERVAL_1.end),
                enqueued_commands(PREV_RUN_ID),
                datetime.time(11, 0),
                datetime.time(0, 0),
                TAXES_DML_DT,
                DOCS_PARAMS_DML_DT,
                PRODUCT_DML_DT,
            ).do()

        assert everything_is_empty(new_meta_res)

    def test_mnclose_running_invalid_run_in(self):
        with pytest.raises(AssertionError) as exc_info:
            with patch_now(NOW):
                generate_new_meta.ActMetaGen(
                    mnclose_state(PAST_MONTH, 'open', '0'),
                    tariffed_meta(LOG_INTERVAL_3),
                    interim_meta(LOG_INTERVAL_2, CURR_RUN_ID, NOW, LOG_INTERVAL_1.end),
                    interim_meta(LOG_INTERVAL_2, CURR_RUN_ID, NOW, LOG_INTERVAL_1.end),
                    act_meta(LOG_INTERVAL_1, LOG_INTERVAL_1.end, OLD_MONTH.strftime('%Y-%m'), OLD_MONTH, 666),
                    act_meta(LOG_INTERVAL_1, LOG_INTERVAL_1.end, OLD_MONTH.strftime('%Y-%m'), OLD_MONTH, 666),
                    daily_meta(LOG_INTERVAL_1, LOG_INTERVAL_1.beginning, PREV_RUN_ID, NOW),
                    daily_meta(LOG_INTERVAL_1, LOG_INTERVAL_1.beginning, PREV_RUN_ID, NOW, req_interval=LOG_INTERVAL_1),
                    commands_meta(LOG_INTERVAL_1, PREV_RUN_ID, PREV_RUN_ID, NOW, LOG_INTERVAL_1.end, LOG_INTERVAL_1.end),
                    commands_meta(LOG_INTERVAL_1, PREV_RUN_ID, PREV_RUN_ID, NOW, LOG_INTERVAL_1.end, LOG_INTERVAL_1.end),
                    enqueued_commands(PREV_RUN_ID),
                    datetime.time(11, 0),
                    datetime.time(0, 0),
                    TAXES_DML_DT,
                    DOCS_PARAMS_DML_DT,
                    PRODUCT_DML_DT,
                ).do()

        assert 'inconsequential run_id for acts' in exc_info.value.args[0]

    def test_old_month(self):
        with pytest.raises(AssertionError) as exc_info:
            with patch_now(NOW):
                generate_new_meta.ActMetaGen(
                    mnclose_state(OLD_MONTH, 'open'),
                    tariffed_meta(LOG_INTERVAL_3),
                    interim_meta(LOG_INTERVAL_2, CURR_RUN_ID, NOW, LOG_INTERVAL_1.end),
                    interim_meta(LOG_INTERVAL_2, CURR_RUN_ID, NOW, LOG_INTERVAL_1.end),
                    act_meta(LOG_INTERVAL_1, LOG_INTERVAL_1.end, PAST_MONTH.strftime('%Y-%m'), OLD_MONTH, 666),
                    act_meta(LOG_INTERVAL_1, LOG_INTERVAL_1.end, PAST_MONTH.strftime('%Y-%m'), OLD_MONTH, 666),
                    daily_meta(LOG_INTERVAL_2, LOG_INTERVAL_1.end, PREV_RUN_ID, NOW),
                    daily_meta(LOG_INTERVAL_2, LOG_INTERVAL_1.end, PREV_RUN_ID, NOW, req_interval=LOG_INTERVAL_1),
                    commands_meta(LOG_INTERVAL_1, PREV_RUN_ID, PREV_RUN_ID, NOW, LOG_INTERVAL_1.end, LOG_INTERVAL_1.end),
                    commands_meta(LOG_INTERVAL_1, PREV_RUN_ID, PREV_RUN_ID, NOW, LOG_INTERVAL_1.end, LOG_INTERVAL_1.end),
                    enqueued_commands(PREV_RUN_ID),
                    datetime.time(11, 0),
                    datetime.time(0, 0),
                    TAXES_DML_DT,
                    DOCS_PARAMS_DML_DT,
                    PRODUCT_DML_DT,
                ).do()

        assert 'generating acts for old month' in exc_info.value.args[0]

    def test_no_new_interim(self):
        with patch_now(NOW):
            new_meta_res = generate_new_meta.ActMetaGen(
                mnclose_state(PAST_MONTH, 'open'),
                tariffed_meta(LOG_INTERVAL_3),
                interim_meta(LOG_INTERVAL_2, CURR_RUN_ID, NOW, LOG_INTERVAL_1.end),
                interim_meta(LOG_INTERVAL_2, CURR_RUN_ID, NOW, LOG_INTERVAL_1.end),
                act_meta(LOG_INTERVAL_2, LOG_INTERVAL_1.end, OLD_MONTH.strftime('%Y-%m'), OLD_MONTH),
                act_meta(LOG_INTERVAL_2, LOG_INTERVAL_1.end, OLD_MONTH.strftime('%Y-%m'), OLD_MONTH),
                daily_meta(LOG_INTERVAL_2, LOG_INTERVAL_1.end, PREV_RUN_ID, NOW),
                daily_meta(LOG_INTERVAL_2, LOG_INTERVAL_1.end, PREV_RUN_ID, NOW, req_interval=LOG_INTERVAL_1),
                commands_meta(LOG_INTERVAL_1, PREV_RUN_ID, PREV_RUN_ID, NOW, LOG_INTERVAL_1.end, LOG_INTERVAL_1.end),
                commands_meta(LOG_INTERVAL_1, PREV_RUN_ID, PREV_RUN_ID, NOW, LOG_INTERVAL_1.end, LOG_INTERVAL_1.end),
                enqueued_commands(PREV_RUN_ID),
                datetime.time(11, 0),
                datetime.time(0, 0),
                TAXES_DML_DT,
                DOCS_PARAMS_DML_DT,
                PRODUCT_DML_DT,
            ).do()

        assert everything_is_empty(new_meta_res)

    @pytest.mark.parametrize(
        'mnclose_id, res_run_id',
        [
            pytest.param(None, PAST_MONTH.strftime('%Y-%m'), id='default_run_id'),
            pytest.param('6666', '6666', id='custom_run_id'),
        ]
    )
    def test_mnclose_running_new(self, mnclose_id, res_run_id):
        with patch_now(NOW):
            new_meta_res = generate_new_meta.ActMetaGen(
                mnclose_state(PAST_MONTH, 'open', mnclose_id),
                tariffed_meta(LOG_INTERVAL_3),
                interim_meta(LOG_INTERVAL_2, CURR_RUN_ID, NOW, LOG_INTERVAL_1.end),
                interim_meta(LOG_INTERVAL_2, CURR_RUN_ID, NOW, LOG_INTERVAL_1.end),
                act_meta(LOG_INTERVAL_1, LOG_INTERVAL_1.end, OLD_MONTH.strftime('%Y-%m'), OLD_MONTH, 666),
                act_meta(LOG_INTERVAL_1, LOG_INTERVAL_1.end, OLD_MONTH.strftime('%Y-%m'), OLD_MONTH, 666),
                daily_meta(LOG_INTERVAL_2, LOG_INTERVAL_1.end, PREV_RUN_ID, NOW),
                daily_meta(LOG_INTERVAL_2, LOG_INTERVAL_1.end, PREV_RUN_ID, NOW, req_interval=LOG_INTERVAL_1),
                commands_meta(LOG_INTERVAL_1, PREV_RUN_ID, PREV_RUN_ID, NOW, LOG_INTERVAL_1.end, LOG_INTERVAL_1.end),
                commands_meta(LOG_INTERVAL_1, PREV_RUN_ID, PREV_RUN_ID, NOW, LOG_INTERVAL_1.end, LOG_INTERVAL_1.end),
                enqueued_commands(PREV_RUN_ID),
                datetime.time(11, 0),
                datetime.time(0, 0),
                TAXES_DML_DT,
                DOCS_PARAMS_DML_DT,
                PRODUCT_DML_DT,
            ).do()

        assert new_meta_res.new_interim_meta is None
        assert new_meta_res.new_acts_meta == act_meta(
            LOG_INTERVAL_2,
            LOG_INTERVAL_2.end,
            res_run_id,
            PAST_MONTH.ceil('month'),
            666
        )
        assert new_meta_res.new_daily_acts_meta is None
        assert new_meta_res.new_commands_meta is None

    @pytest.mark.parametrize(
        'cur_act_slice, daily_interval, res_slice',
        [
            pytest.param(LOG_INTERVAL_1.end, LOG_INTERVAL_2, LOG_INTERVAL_2.end, id='before'),
            pytest.param(LOG_INTERVAL_2.end, LOG_INTERVAL_2, LOG_INTERVAL_2.end, id='equal'),
            pytest.param(LOG_INTERVAL_2.end, LOG_INTERVAL_3, LOG_INTERVAL_3.end, id='after'),
        ]
    )
    def test_daily_pos(self, cur_act_slice, daily_interval, res_slice):
        with patch_now(NOW):
            new_meta_res = generate_new_meta.ActMetaGen(
                mnclose_state(PAST_MONTH, 'open', '666'),
                tariffed_meta(LOG_INTERVAL_3),
                interim_meta(LOG_INTERVAL_3, CURR_RUN_ID, NOW, LOG_INTERVAL_1.end),
                interim_meta(LOG_INTERVAL_3, CURR_RUN_ID, NOW, LOG_INTERVAL_1.end),
                act_meta(LOG_INTERVAL_2, cur_act_slice, OLD_MONTH.strftime('%Y-%m'), OLD_MONTH, 666),
                act_meta(LOG_INTERVAL_2, cur_act_slice, OLD_MONTH.strftime('%Y-%m'), OLD_MONTH, 666),
                daily_meta(daily_interval, LOG_INTERVAL_2.end, PREV_RUN_ID, NOW),
                daily_meta(daily_interval, LOG_INTERVAL_2.end, PREV_RUN_ID, NOW, req_interval=LOG_INTERVAL_1),
                commands_meta(LOG_INTERVAL_1, PREV_RUN_ID, PREV_RUN_ID, NOW, LOG_INTERVAL_1.end, LOG_INTERVAL_1.end),
                commands_meta(LOG_INTERVAL_1, PREV_RUN_ID, PREV_RUN_ID, NOW, LOG_INTERVAL_1.end, LOG_INTERVAL_1.end),
                enqueued_commands(PREV_RUN_ID),
                datetime.time(11, 0),
                datetime.time(0, 0),
                TAXES_DML_DT,
                DOCS_PARAMS_DML_DT,
                PRODUCT_DML_DT,
            ).do()

        assert new_meta_res.new_interim_meta is None
        assert new_meta_res.new_acts_meta == act_meta(
            LOG_INTERVAL_3,
            res_slice,
            '666',
            PAST_MONTH.ceil('month'),
            666
        )
        assert new_meta_res.new_daily_acts_meta is None
        assert new_meta_res.new_commands_meta is None

    @pytest.mark.parametrize(
        'monthly_pos, daily_pos, res_pos',
        [
            pytest.param(7, 42, 42, id='daily'),
            pytest.param(666, 42, 666, id='monthly'),
        ]
    )
    def test_act_seq(self, monthly_pos, daily_pos, res_pos):
        with patch_now(NOW):
            new_meta_res = generate_new_meta.ActMetaGen(
                mnclose_state(PAST_MONTH, 'open', '666'),
                tariffed_meta(LOG_INTERVAL_3),
                interim_meta(LOG_INTERVAL_2, CURR_RUN_ID, NOW, LOG_INTERVAL_1.end),
                interim_meta(LOG_INTERVAL_2, CURR_RUN_ID, NOW, LOG_INTERVAL_1.end),
                act_meta(LOG_INTERVAL_1, LOG_INTERVAL_1.end, OLD_MONTH.strftime('%Y-%m'), OLD_MONTH, monthly_pos),
                act_meta(LOG_INTERVAL_1, LOG_INTERVAL_1.end, OLD_MONTH.strftime('%Y-%m'), OLD_MONTH, monthly_pos),
                daily_meta(LOG_INTERVAL_2, LOG_INTERVAL_1.end, PREV_RUN_ID, NOW, daily_pos),
                daily_meta(LOG_INTERVAL_2, LOG_INTERVAL_1.end, PREV_RUN_ID, NOW, daily_pos,
                           req_interval=LOG_INTERVAL_1),
                commands_meta(LOG_INTERVAL_1, PREV_RUN_ID, PREV_RUN_ID, NOW, LOG_INTERVAL_1.end, LOG_INTERVAL_1.end),
                commands_meta(LOG_INTERVAL_1, PREV_RUN_ID, PREV_RUN_ID, NOW, LOG_INTERVAL_1.end, LOG_INTERVAL_1.end),
                enqueued_commands(PREV_RUN_ID),
                datetime.time(11, 0),
                datetime.time(0, 0),
                TAXES_DML_DT,
                DOCS_PARAMS_DML_DT,
                PRODUCT_DML_DT,
            ).do()

        assert new_meta_res.new_interim_meta is None
        assert new_meta_res.new_acts_meta == act_meta(
            LOG_INTERVAL_2,
            LOG_INTERVAL_2.end,
            '666',
            PAST_MONTH.ceil('month'),
            res_pos
        )
        assert new_meta_res.new_daily_acts_meta is None
        assert new_meta_res.new_commands_meta is None

    def test_with_bigger_processed_commands_meta(self):
        """
        Мета интеримов опережает мету актов, но не опережает мету команд.
        Проверяем, что генерация актов не начнется
        """
        with patch_now(NOW):
            new_meta_res = generate_new_meta.ActMetaGen(
                mnclose_state(PAST_MONTH, 'open', None),
                tariffed_meta(LOG_INTERVAL_3),
                interim_meta(LOG_INTERVAL_3, CURR_RUN_ID, NOW, LOG_INTERVAL_1.end),
                interim_meta(LOG_INTERVAL_3, CURR_RUN_ID, NOW, LOG_INTERVAL_1.end),
                act_meta(LOG_INTERVAL_1, LOG_INTERVAL_1.end, OLD_MONTH.strftime('%Y-%m'), OLD_MONTH, 666),
                act_meta(LOG_INTERVAL_1, LOG_INTERVAL_1.end, OLD_MONTH.strftime('%Y-%m'), OLD_MONTH, 666),
                daily_meta(LOG_INTERVAL_2, LOG_INTERVAL_1.end, PREV_RUN_ID, NOW),
                daily_meta(LOG_INTERVAL_2, LOG_INTERVAL_1.end, PREV_RUN_ID, NOW, req_interval=LOG_INTERVAL_1),
                commands_meta(LOG_INTERVAL_3, PREV_RUN_ID, PREV_RUN_ID, NOW, LOG_INTERVAL_1.end, LOG_INTERVAL_1.end),
                commands_meta(LOG_INTERVAL_3, PREV_RUN_ID, PREV_RUN_ID, NOW, LOG_INTERVAL_1.end, LOG_INTERVAL_1.end),
                enqueued_commands(PREV_RUN_ID),
                datetime.time(11, 0),
                datetime.time(0, 0),
                TAXES_DML_DT,
                DOCS_PARAMS_DML_DT,
                PRODUCT_DML_DT,
            ).do()

        assert everything_is_empty(new_meta_res)

    @pytest.mark.parametrize(
        'mnclose_id, res_run_id',
        [
            pytest.param(None, PAST_MONTH.strftime('%Y-%m'), id='default_run_id'),
            pytest.param('6666', '6666', id='custom_run_id'),
        ]
    )
    def test_mnclose_running_after_commands(self, mnclose_id, res_run_id):
        with patch_now(NOW):
            new_meta_res = generate_new_meta.ActMetaGen(
                mnclose_state(PAST_MONTH, 'open', mnclose_id),
                tariffed_meta(LOG_INTERVAL_3),
                interim_meta(LOG_INTERVAL_2, CURR_RUN_ID, NOW, LOG_INTERVAL_1.end),
                interim_meta(LOG_INTERVAL_2, CURR_RUN_ID, NOW, LOG_INTERVAL_1.end),
                act_meta(LOG_INTERVAL_1, LOG_INTERVAL_1.end, OLD_MONTH.strftime('%Y-%m'), OLD_MONTH, 666),
                act_meta(LOG_INTERVAL_1, LOG_INTERVAL_1.end, OLD_MONTH.strftime('%Y-%m'), OLD_MONTH, 666),
                daily_meta(LOG_INTERVAL_2, LOG_INTERVAL_1.end, PREV_RUN_ID, NOW),
                daily_meta(LOG_INTERVAL_2, LOG_INTERVAL_1.end, PREV_RUN_ID, NOW, req_interval=LOG_INTERVAL_1),
                commands_meta(LOG_INTERVAL_3, PREV_RUN_ID, PREV_RUN_ID, NOW, LOG_INTERVAL_1.end, LOG_INTERVAL_1.end),
                commands_meta(LOG_INTERVAL_3, PREV_RUN_ID, PREV_RUN_ID, NOW, LOG_INTERVAL_1.end, LOG_INTERVAL_1.end),
                enqueued_commands(PREV_RUN_ID),
                datetime.time(11, 0),
                datetime.time(0, 0),
                TAXES_DML_DT,
                DOCS_PARAMS_DML_DT,
                PRODUCT_DML_DT,
            ).do()

        assert everything_is_empty(new_meta_res)


class TestDailyActs:
    def test_base(self):
        with patch_now(NOW), patch_generate_run_id(NEXT_RUN_ID):
            new_meta_res = generate_new_meta.ActMetaGen(
                mnclose_state(PAST_MONTH, 'closed'),
                tariffed_meta(LOG_INTERVAL_3),
                interim_meta(LOG_INTERVAL_2, CURR_RUN_ID, NOW, LOG_INTERVAL_1.end),
                interim_meta(LOG_INTERVAL_2, CURR_RUN_ID, NOW, LOG_INTERVAL_1.end),
                act_meta(LOG_INTERVAL_1, LOG_INTERVAL_1.end, OLD_MONTH.strftime('%Y-%m'), OLD_MONTH),
                act_meta(LOG_INTERVAL_1, LOG_INTERVAL_1.end, OLD_MONTH.strftime('%Y-%m'), OLD_MONTH),
                daily_meta(LOG_INTERVAL_1, LOG_INTERVAL_1.end, PREV_RUN_ID, YESTERDAY),
                daily_meta(LOG_INTERVAL_1, LOG_INTERVAL_1.end, PREV_RUN_ID, YESTERDAY, req_interval=LOG_INTERVAL_1),
                commands_meta(LOG_INTERVAL_1, PREV_RUN_ID, PREV_RUN_ID, NOW, LOG_INTERVAL_1.end, LOG_INTERVAL_1.end),
                commands_meta(LOG_INTERVAL_1, PREV_RUN_ID, PREV_RUN_ID, NOW, LOG_INTERVAL_1.end, LOG_INTERVAL_1.end),
                enqueued_commands(PREV_RUN_ID),
                datetime.time(11, 0),
                datetime.time(11, 0),
                TAXES_DML_DT,
                DOCS_PARAMS_DML_DT,
                PRODUCT_DML_DT,
            ).do()

        assert new_meta_res.new_interim_meta is None
        assert new_meta_res.new_acts_meta is None
        assert new_meta_res.new_commands_meta is None
        assert new_meta_res.new_daily_acts_meta == daily_meta(
            LOG_INTERVAL_2,
            LOG_INTERVAL_1.end,
            NEXT_RUN_ID,
            TODAY,
            666,
            LOG_INTERVAL_1
        )

    @pytest.mark.parametrize(
        'act_interval, res_slice',
        [
            pytest.param(LOG_INTERVAL_2, LOG_INTERVAL_2.end, id='start'),
            pytest.param(LOG_INTERVAL_1, LOG_INTERVAL_1.end, id='old'),
        ]
    )
    def test_act_seq_pos(self, act_interval, res_slice):
        with patch_now(NOW), patch_generate_run_id(NEXT_RUN_ID):
            new_meta_res = generate_new_meta.ActMetaGen(
                mnclose_state(PAST_MONTH, 'closed'),
                tariffed_meta(LOG_INTERVAL_3),
                interim_meta(LOG_INTERVAL_3, CURR_RUN_ID, NOW, LOG_INTERVAL_1.end),
                interim_meta(LOG_INTERVAL_3, CURR_RUN_ID, NOW, LOG_INTERVAL_1.end),
                act_meta(act_interval, LOG_INTERVAL_1.end, OLD_MONTH.strftime('%Y-%m'), OLD_MONTH),
                act_meta(act_interval, LOG_INTERVAL_1.end, OLD_MONTH.strftime('%Y-%m'), OLD_MONTH),
                daily_meta(LOG_INTERVAL_2, LOG_INTERVAL_1.end, PREV_RUN_ID, YESTERDAY),
                daily_meta(LOG_INTERVAL_2, LOG_INTERVAL_1.end, PREV_RUN_ID, YESTERDAY, req_interval=LOG_INTERVAL_1),
                commands_meta(LOG_INTERVAL_1, PREV_RUN_ID, PREV_RUN_ID, NOW, LOG_INTERVAL_1.end, LOG_INTERVAL_1.end),
                commands_meta(LOG_INTERVAL_1, PREV_RUN_ID, PREV_RUN_ID, NOW, LOG_INTERVAL_1.end, LOG_INTERVAL_1.end),
                enqueued_commands(PREV_RUN_ID),
                datetime.time(11, 0),
                datetime.time(11, 0),
                TAXES_DML_DT,
                DOCS_PARAMS_DML_DT,
                PRODUCT_DML_DT,
            ).do()

        assert new_meta_res.new_interim_meta is None
        assert new_meta_res.new_acts_meta is None
        assert new_meta_res.new_commands_meta is None
        assert new_meta_res.new_daily_acts_meta == daily_meta(
            LOG_INTERVAL_3,
            res_slice,
            NEXT_RUN_ID,
            TODAY,
            666,
            LOG_INTERVAL_1
        )

    def test_unprocessed(self):
        with patch_now(NOW):
            new_meta_res = generate_new_meta.ActMetaGen(
                mnclose_state(PAST_MONTH, 'closed'),
                tariffed_meta(LOG_INTERVAL_3),
                interim_meta(LOG_INTERVAL_3, CURR_RUN_ID, NOW, LOG_INTERVAL_1.end),
                interim_meta(LOG_INTERVAL_3, CURR_RUN_ID, NOW, LOG_INTERVAL_1.end),
                act_meta(LOG_INTERVAL_2, LOG_INTERVAL_1.end, OLD_MONTH.strftime('%Y-%m'), OLD_MONTH),
                act_meta(LOG_INTERVAL_2, LOG_INTERVAL_1.end, OLD_MONTH.strftime('%Y-%m'), OLD_MONTH),
                daily_meta(LOG_INTERVAL_2, LOG_INTERVAL_1.end, CURR_RUN_ID, YESTERDAY),
                daily_meta(LOG_INTERVAL_1, LOG_INTERVAL_1.end, PREV_RUN_ID, YESTERDAY, req_interval=LOG_INTERVAL_1),
                commands_meta(LOG_INTERVAL_1, PREV_RUN_ID, PREV_RUN_ID, NOW, LOG_INTERVAL_1.end, LOG_INTERVAL_1.end),
                commands_meta(LOG_INTERVAL_1, PREV_RUN_ID, PREV_RUN_ID, NOW, LOG_INTERVAL_1.end, LOG_INTERVAL_1.end),
                enqueued_commands(PREV_RUN_ID),
                datetime.time(11, 0),
                datetime.time(11, 0),
                TAXES_DML_DT,
                DOCS_PARAMS_DML_DT,
                PRODUCT_DML_DT,
            ).do()

        assert everything_is_empty(new_meta_res)

    def test_mismatched_prev(self):
        with pytest.raises(AssertionError) as exc_info:
            generate_new_meta.ActMetaGen(
                mnclose_state(PAST_MONTH, 'closed'),
                tariffed_meta(LOG_INTERVAL_3),
                interim_meta(LOG_INTERVAL_3, CURR_RUN_ID, NOW, LOG_INTERVAL_1.end),
                interim_meta(LOG_INTERVAL_3, CURR_RUN_ID, NOW, LOG_INTERVAL_1.end),
                act_meta(LOG_INTERVAL_2, LOG_INTERVAL_1.end, OLD_MONTH.strftime('%Y-%m'), OLD_MONTH),
                act_meta(LOG_INTERVAL_2, LOG_INTERVAL_1.end, OLD_MONTH.strftime('%Y-%m'), OLD_MONTH),
                daily_meta(LOG_INTERVAL_2, LOG_INTERVAL_1.end, CURR_RUN_ID, YESTERDAY),
                daily_meta(LogInterval([Subinterval('a', 'a', 0, 11, 21)]), LOG_INTERVAL_1.end, PREV_RUN_ID, YESTERDAY,
                           req_interval=LOG_INTERVAL_1),
                commands_meta(LOG_INTERVAL_1, PREV_RUN_ID, PREV_RUN_ID, NOW, LOG_INTERVAL_1.end, LOG_INTERVAL_1.end),
                commands_meta(LOG_INTERVAL_1, PREV_RUN_ID, PREV_RUN_ID, NOW, LOG_INTERVAL_1.end, LOG_INTERVAL_1.end),
                enqueued_commands(PREV_RUN_ID),
                datetime.time(11, 0),
                datetime.time(11, 0),
                TAXES_DML_DT,
                DOCS_PARAMS_DML_DT,
                PRODUCT_DML_DT,
            ).do()

        assert 'intervals mismatch' in exc_info.value.args[0]

    @pytest.mark.parametrize(
        'daily_interval',
        [
            pytest.param(LOG_INTERVAL_2, id='from_daily'),
            pytest.param(LOG_INTERVAL_1, id='from_monthly'),
        ]
    )
    def test_no_interim(self, daily_interval):
        with patch_now(NOW), patch_generate_run_id(NEXT_RUN_ID):
            new_meta_res = generate_new_meta.ActMetaGen(
                mnclose_state(PAST_MONTH, 'closed'),
                tariffed_meta(LOG_INTERVAL_3),
                interim_meta(LOG_INTERVAL_2, CURR_RUN_ID, NOW, LOG_INTERVAL_1.end),
                interim_meta(LOG_INTERVAL_2, CURR_RUN_ID, NOW, LOG_INTERVAL_1.end),
                act_meta(LOG_INTERVAL_2, LOG_INTERVAL_1.end, OLD_MONTH.strftime('%Y-%m'), OLD_MONTH),
                act_meta(LOG_INTERVAL_2, LOG_INTERVAL_1.end, OLD_MONTH.strftime('%Y-%m'), OLD_MONTH),
                daily_meta(daily_interval, LOG_INTERVAL_1.end, PREV_RUN_ID, YESTERDAY),
                daily_meta(daily_interval, LOG_INTERVAL_1.end, PREV_RUN_ID, YESTERDAY, req_interval=LOG_INTERVAL_1),
                commands_meta(LOG_INTERVAL_1, PREV_RUN_ID, PREV_RUN_ID, NOW, LOG_INTERVAL_1.end, LOG_INTERVAL_1.end),
                commands_meta(LOG_INTERVAL_1, PREV_RUN_ID, PREV_RUN_ID, NOW, LOG_INTERVAL_1.end, LOG_INTERVAL_1.end),
                enqueued_commands(PREV_RUN_ID),
                datetime.time(11, 0),
                datetime.time(11, 0),
                TAXES_DML_DT,
                DOCS_PARAMS_DML_DT,
                PRODUCT_DML_DT,
            ).do()

        assert new_meta_res.new_interim_meta == interim_meta(
            LOG_INTERVAL_3,
            NEXT_RUN_ID,
            TODAY,
            LOG_INTERVAL_2.end
        )
        assert new_meta_res.new_acts_meta is None
        assert new_meta_res.new_daily_acts_meta is None
        assert new_meta_res.new_commands_meta is None

    def test_no_requests(self):
        with patch_now(NOW), patch_generate_run_id(NEXT_RUN_ID):
            new_meta_res = generate_new_meta.ActMetaGen(
                mnclose_state(PAST_MONTH, 'closed'),
                tariffed_meta(LOG_INTERVAL_3),
                interim_meta(LOG_INTERVAL_2, CURR_RUN_ID, NOW, LOG_INTERVAL_1.end),
                interim_meta(LOG_INTERVAL_2, CURR_RUN_ID, NOW, LOG_INTERVAL_1.end),
                act_meta(LOG_INTERVAL_1, LOG_INTERVAL_1.end, OLD_MONTH.strftime('%Y-%m'), OLD_MONTH),
                act_meta(LOG_INTERVAL_1, LOG_INTERVAL_1.end, OLD_MONTH.strftime('%Y-%m'), OLD_MONTH),
                daily_meta(LOG_INTERVAL_1, LOG_INTERVAL_1.end, PREV_RUN_ID, YESTERDAY),
                daily_meta(LOG_INTERVAL_1, LOG_INTERVAL_1.end, PREV_RUN_ID, YESTERDAY, req_interval=LOG_INTERVAL_EMPTY),
                commands_meta(LOG_INTERVAL_1, PREV_RUN_ID, PREV_RUN_ID, NOW, LOG_INTERVAL_1.end, LOG_INTERVAL_1.end),
                commands_meta(LOG_INTERVAL_1, PREV_RUN_ID, PREV_RUN_ID, NOW, LOG_INTERVAL_1.end, LOG_INTERVAL_1.end),
                enqueued_commands(PREV_RUN_ID),
                datetime.time(11, 0),
                datetime.time(11, 0),
                TAXES_DML_DT,
                DOCS_PARAMS_DML_DT,
                PRODUCT_DML_DT,
            ).do()

        assert new_meta_res.new_interim_meta == interim_meta(
            LOG_INTERVAL_3,
            NEXT_RUN_ID,
            TODAY,
            LOG_INTERVAL_1.end
        )
        assert new_meta_res.new_acts_meta is None
        assert new_meta_res.new_daily_acts_meta is None
        assert new_meta_res.new_commands_meta is None

    def test_time(self):
        with patch_now(NOW), patch_generate_run_id(NEXT_RUN_ID):
            new_meta_res = generate_new_meta.ActMetaGen(
                mnclose_state(PAST_MONTH, 'closed'),
                tariffed_meta(LOG_INTERVAL_3),
                interim_meta(LOG_INTERVAL_2, CURR_RUN_ID, NOW, LOG_INTERVAL_1.end),
                interim_meta(LOG_INTERVAL_2, CURR_RUN_ID, NOW, LOG_INTERVAL_1.end),
                act_meta(LOG_INTERVAL_1, LOG_INTERVAL_1.end, OLD_MONTH.strftime('%Y-%m'), OLD_MONTH),
                act_meta(LOG_INTERVAL_1, LOG_INTERVAL_1.end, OLD_MONTH.strftime('%Y-%m'), OLD_MONTH),
                daily_meta(LOG_INTERVAL_1, LOG_INTERVAL_1.end, PREV_RUN_ID, YESTERDAY),
                daily_meta(LOG_INTERVAL_1, LOG_INTERVAL_1.end, PREV_RUN_ID, YESTERDAY, req_interval=LOG_INTERVAL_1),
                commands_meta(LOG_INTERVAL_1, PREV_RUN_ID, PREV_RUN_ID, NOW, LOG_INTERVAL_1.end, LOG_INTERVAL_1.end),
                commands_meta(LOG_INTERVAL_1, PREV_RUN_ID, PREV_RUN_ID, NOW, LOG_INTERVAL_1.end, LOG_INTERVAL_1.end),
                enqueued_commands(PREV_RUN_ID),
                datetime.time(11, 0),
                datetime.time(13, 0),
                TAXES_DML_DT,
                DOCS_PARAMS_DML_DT,
                PRODUCT_DML_DT,
            ).do()

        assert new_meta_res.new_interim_meta == interim_meta(
            LOG_INTERVAL_3,
            NEXT_RUN_ID,
            TODAY,
            LOG_INTERVAL_1.end
        )
        assert new_meta_res.new_acts_meta is None
        assert new_meta_res.new_daily_acts_meta is None
        assert new_meta_res.new_commands_meta is None

    def test_processed_today(self):
        with patch_now(NOW), patch_generate_run_id(NEXT_RUN_ID):
            new_meta_res = generate_new_meta.ActMetaGen(
                mnclose_state(PAST_MONTH, 'closed'),
                tariffed_meta(LOG_INTERVAL_3),
                interim_meta(LOG_INTERVAL_2, CURR_RUN_ID, NOW, LOG_INTERVAL_1.end),
                interim_meta(LOG_INTERVAL_2, CURR_RUN_ID, NOW, LOG_INTERVAL_1.end),
                act_meta(LOG_INTERVAL_1, LOG_INTERVAL_1.end, OLD_MONTH.strftime('%Y-%m'), OLD_MONTH),
                act_meta(LOG_INTERVAL_1, LOG_INTERVAL_1.end, OLD_MONTH.strftime('%Y-%m'), OLD_MONTH),
                daily_meta(LOG_INTERVAL_1, LOG_INTERVAL_1.end, PREV_RUN_ID, TODAY),
                daily_meta(LOG_INTERVAL_1, LOG_INTERVAL_1.end, PREV_RUN_ID, TODAY, req_interval=LOG_INTERVAL_1),
                commands_meta(LOG_INTERVAL_1, PREV_RUN_ID, PREV_RUN_ID, NOW, LOG_INTERVAL_1.end, LOG_INTERVAL_1.end),
                commands_meta(LOG_INTERVAL_1, PREV_RUN_ID, PREV_RUN_ID, NOW, LOG_INTERVAL_1.end, LOG_INTERVAL_1.end),
                enqueued_commands(PREV_RUN_ID),
                datetime.time(11, 0),
                datetime.time(11, 0),
                TAXES_DML_DT,
                DOCS_PARAMS_DML_DT,
                PRODUCT_DML_DT,
            ).do()

        assert new_meta_res.new_interim_meta == interim_meta(
            LOG_INTERVAL_3,
            NEXT_RUN_ID,
            TODAY,
            LOG_INTERVAL_1.end
        )
        assert new_meta_res.new_acts_meta is None
        assert new_meta_res.new_daily_acts_meta is None
        assert new_meta_res.new_commands_meta is None

    @pytest.mark.parametrize(
        'on_dt, is_daily',
        [
            pytest.param(arrow.get('2020-06-30 12:00:00').replace(tzinfo=MSK_TZ), False, id='last'),
            pytest.param(arrow.get('2020-06-29 12:00:00').replace(tzinfo=MSK_TZ), True, id='not_last'),
        ]
    )
    def test_last_day_of_month(self, on_dt, is_daily):
        with patch_now(on_dt), patch_generate_run_id(NEXT_RUN_ID):
            new_meta_res = generate_new_meta.ActMetaGen(
                mnclose_state(PAST_MONTH, 'closed'),
                tariffed_meta(LOG_INTERVAL_3),
                interim_meta(LOG_INTERVAL_2, CURR_RUN_ID, NOW, LOG_INTERVAL_1.end),
                interim_meta(LOG_INTERVAL_2, CURR_RUN_ID, NOW, LOG_INTERVAL_1.end),
                act_meta(LOG_INTERVAL_1, LOG_INTERVAL_1.end, OLD_MONTH.strftime('%Y-%m'), OLD_MONTH),
                act_meta(LOG_INTERVAL_1, LOG_INTERVAL_1.end, OLD_MONTH.strftime('%Y-%m'), OLD_MONTH),
                daily_meta(LOG_INTERVAL_1, LOG_INTERVAL_1.end, PREV_RUN_ID, YESTERDAY),
                daily_meta(LOG_INTERVAL_1, LOG_INTERVAL_1.end, PREV_RUN_ID, YESTERDAY, req_interval=LOG_INTERVAL_1),
                commands_meta(LOG_INTERVAL_1, PREV_RUN_ID, PREV_RUN_ID, NOW, LOG_INTERVAL_1.end, LOG_INTERVAL_1.end),
                commands_meta(LOG_INTERVAL_1, PREV_RUN_ID, PREV_RUN_ID, NOW, LOG_INTERVAL_1.end, LOG_INTERVAL_1.end),
                enqueued_commands(PREV_RUN_ID),
                datetime.time(11, 0),
                datetime.time(0, 0),
                TAXES_DML_DT,
                DOCS_PARAMS_DML_DT,
                PRODUCT_DML_DT,
            ).do()

        if is_daily:
            assert new_meta_res.new_interim_meta is None
            assert new_meta_res.new_daily_acts_meta == daily_meta(
                LOG_INTERVAL_2,
                LOG_INTERVAL_1.end,
                NEXT_RUN_ID,
                on_dt,
                666,
                LOG_INTERVAL_1
            )
        else:
            assert new_meta_res.new_interim_meta == interim_meta(
                LOG_INTERVAL_3,
                NEXT_RUN_ID,
                on_dt,
                LOG_INTERVAL_1.end
            )
            assert new_meta_res.new_daily_acts_meta is None

        assert new_meta_res.new_acts_meta is None
        assert new_meta_res.new_commands_meta is None

    def test_with_bigger_processed_commands_meta(self):
        with patch_now(NOW), patch_generate_run_id(NEXT_RUN_ID):
            new_meta_res = generate_new_meta.ActMetaGen(
                mnclose_state(PAST_MONTH, 'closed'),
                tariffed_meta(LOG_INTERVAL_2),
                interim_meta(LOG_INTERVAL_2, CURR_RUN_ID, NOW, LOG_INTERVAL_1.end),
                interim_meta(LOG_INTERVAL_2, CURR_RUN_ID, NOW, LOG_INTERVAL_1.end),
                act_meta(LOG_INTERVAL_1, LOG_INTERVAL_1.end, OLD_MONTH.strftime('%Y-%m'), OLD_MONTH),
                act_meta(LOG_INTERVAL_1, LOG_INTERVAL_1.end, OLD_MONTH.strftime('%Y-%m'), OLD_MONTH),
                daily_meta(LOG_INTERVAL_1, LOG_INTERVAL_1.end, PREV_RUN_ID, YESTERDAY),
                daily_meta(LOG_INTERVAL_1, LOG_INTERVAL_1.end, PREV_RUN_ID, YESTERDAY, req_interval=LOG_INTERVAL_1),
                commands_meta(LOG_INTERVAL_2, PREV_RUN_ID, PREV_RUN_ID, NOW, LOG_INTERVAL_1.end, LOG_INTERVAL_1.end),
                commands_meta(LOG_INTERVAL_2, PREV_RUN_ID, PREV_RUN_ID, NOW, LOG_INTERVAL_1.end, LOG_INTERVAL_1.end),
                enqueued_commands(PREV_RUN_ID),
                datetime.time(11, 0),
                datetime.time(11, 0),
                TAXES_DML_DT,
                DOCS_PARAMS_DML_DT,
                PRODUCT_DML_DT,
            ).do()

        assert everything_is_empty(new_meta_res)

    def test_daily_after_commands(self):
        with patch_now(NOW), patch_generate_run_id(NEXT_RUN_ID):
            new_meta_res = generate_new_meta.ActMetaGen(
                mnclose_state(PAST_MONTH, 'closed'),
                tariffed_meta(LOG_INTERVAL_3),
                interim_meta(LOG_INTERVAL_2, CURR_RUN_ID, NOW, LOG_INTERVAL_1.end),
                interim_meta(LOG_INTERVAL_2, CURR_RUN_ID, NOW, LOG_INTERVAL_1.end),
                act_meta(LOG_INTERVAL_1, LOG_INTERVAL_1.end, OLD_MONTH.strftime('%Y-%m'), OLD_MONTH),
                act_meta(LOG_INTERVAL_1, LOG_INTERVAL_1.end, OLD_MONTH.strftime('%Y-%m'), OLD_MONTH),
                daily_meta(LOG_INTERVAL_1, LOG_INTERVAL_1.end, PREV_RUN_ID, YESTERDAY),
                daily_meta(LOG_INTERVAL_1, LOG_INTERVAL_1.end, PREV_RUN_ID, YESTERDAY, req_interval=LOG_INTERVAL_1),
                commands_meta(LOG_INTERVAL_3, PREV_RUN_ID, PREV_RUN_ID, NOW, LOG_INTERVAL_1.end, LOG_INTERVAL_1.end),
                commands_meta(LOG_INTERVAL_3, PREV_RUN_ID, PREV_RUN_ID, NOW, LOG_INTERVAL_1.end, LOG_INTERVAL_1.end),
                enqueued_commands(PREV_RUN_ID),
                datetime.time(11, 0),
                datetime.time(11, 0),
                TAXES_DML_DT,
                DOCS_PARAMS_DML_DT,
                PRODUCT_DML_DT,
            ).do()

        assert everything_is_empty(new_meta_res)


class TestCommands:
    def test_unprocessed(self):
        """
        Команды еще не обработаны, появилось тарифицированное.
        Проверяем, что никакие артефакты не генерим
        """
        new_meta_res = generate_new_meta.ActMetaGen(
            mnclose_state(PAST_MONTH, 'closed'),
            tariffed_meta(LOG_INTERVAL_3),
            interim_meta(LOG_INTERVAL_2, CURR_RUN_ID, NOW, LOG_INTERVAL_1.end),
            interim_meta(LOG_INTERVAL_2, PREV_RUN_ID, NOW, LOG_INTERVAL_1.end),
            act_meta(LOG_INTERVAL_1, LOG_INTERVAL_1.beginning, PREV_RUN_ID, NOW),
            act_meta(LOG_INTERVAL_1, LOG_INTERVAL_1.beginning, PREV_RUN_ID, NOW),
            daily_meta(LOG_INTERVAL_1, LOG_INTERVAL_1.beginning, PREV_RUN_ID, NOW),
            daily_meta(LOG_INTERVAL_1, LOG_INTERVAL_1.beginning, PREV_RUN_ID, NOW, req_interval=LOG_INTERVAL_1),
            commands_meta(LOG_INTERVAL_3, PREV_RUN_ID, PREV_RUN_ID, NOW, LOG_INTERVAL_1.end, LOG_INTERVAL_1.end),
            commands_meta(LOG_INTERVAL_2, PREV_RUN_ID, PREV_RUN_ID, NOW, LOG_INTERVAL_1.end, LOG_INTERVAL_1.end),
            enqueued_commands(PREV_RUN_ID),
            datetime.time(11, 0),
            datetime.time(20, 0),
            TAXES_DML_DT,
            DOCS_PARAMS_DML_DT,
            PRODUCT_DML_DT,
        ).do()

        assert everything_is_empty(new_meta_res)

    def test_mismatched_prev(self):
        """
        Все команды обработаны, но интервалы почему-то кривые.
        Проверяем отлавливание такого рассинхрона
        """
        with pytest.raises(AssertionError) as exc_info:
            generate_new_meta.ActMetaGen(
                mnclose_state(PAST_MONTH, 'closed'),
                tariffed_meta(LOG_INTERVAL_3),
                interim_meta(LOG_INTERVAL_2, CURR_RUN_ID, NOW, LOG_INTERVAL_1.end),
                interim_meta(LOG_INTERVAL_2, PREV_RUN_ID, NOW, LOG_INTERVAL_1.end),
                act_meta(LOG_INTERVAL_1, LOG_INTERVAL_1.beginning, PREV_RUN_ID, NOW),
                act_meta(LOG_INTERVAL_1, LOG_INTERVAL_1.beginning, PREV_RUN_ID, NOW),
                daily_meta(LOG_INTERVAL_1, LOG_INTERVAL_1.beginning, PREV_RUN_ID, NOW),
                daily_meta(LOG_INTERVAL_1, LOG_INTERVAL_1.beginning, PREV_RUN_ID, NOW, req_interval=LOG_INTERVAL_1),
                commands_meta(LOG_INTERVAL_3, PREV_RUN_ID, PREV_RUN_ID, NOW, LOG_INTERVAL_1.end, LOG_INTERVAL_1.end),
                commands_meta(LogInterval([Subinterval('a', 'a', 0, 20, 31)]), PREV_RUN_ID, PREV_RUN_ID, NOW,
                              LOG_INTERVAL_1.end, LOG_INTERVAL_1.end),
                enqueued_commands(PREV_RUN_ID),
                datetime.time(11, 0),
                datetime.time(20, 0),
                TAXES_DML_DT,
                DOCS_PARAMS_DML_DT,
                PRODUCT_DML_DT,
            ).do()

        assert 'intervals mismatch' in exc_info.value.args[0]

    def test_no_new_commands(self):
        """
        Нет новых команд, но есть тарифицированное.
        Проверяем, что появится только new_interim_meta
        """
        with patch_now(NOW), patch_generate_run_id(NEXT_RUN_ID):
            new_meta_res = generate_new_meta.ActMetaGen(
                mnclose_state(PAST_MONTH, 'closed'),
                tariffed_meta(LOG_INTERVAL_3),
                interim_meta(LOG_INTERVAL_2, CURR_RUN_ID, NOW, LOG_INTERVAL_1.end),
                interim_meta(LOG_INTERVAL_2, PREV_RUN_ID, NOW, LOG_INTERVAL_1.end),
                act_meta(LOG_INTERVAL_2, LOG_INTERVAL_2.beginning, PREV_RUN_ID, NOW),
                act_meta(LOG_INTERVAL_2, LOG_INTERVAL_2.beginning, PREV_RUN_ID, NOW),
                daily_meta(LOG_INTERVAL_1, LOG_INTERVAL_1.beginning, PREV_RUN_ID, NOW),
                daily_meta(LOG_INTERVAL_1, LOG_INTERVAL_1.beginning, PREV_RUN_ID, NOW, req_interval=LOG_INTERVAL_1),
                commands_meta(LOG_INTERVAL_1, PREV_RUN_ID, PREV_RUN_ID, NOW, LOG_INTERVAL_1.end, LOG_INTERVAL_1.end),
                commands_meta(LOG_INTERVAL_1, PREV_RUN_ID, PREV_RUN_ID, NOW, LOG_INTERVAL_1.end, LOG_INTERVAL_1.end),
                enqueued_commands(PREV_RUN_ID),
                datetime.time(11, 0),
                datetime.time(11, 0),
                TAXES_DML_DT,
                DOCS_PARAMS_DML_DT,
                PRODUCT_DML_DT,
            ).do()

        assert new_meta_res.new_acts_meta is None
        assert new_meta_res.new_daily_acts_meta is None
        assert new_meta_res.new_commands_meta is None

        assert new_meta_res.new_interim_meta == interim_meta(
            LOG_INTERVAL_3,
            NEXT_RUN_ID,
            TODAY,
            LOG_INTERVAL_2.end
        )

    def test_new_commands_w_no_new_tariffed(self):
        """
        Есть новые команды, но нет тарифицированного.
        Проверяем, что никакие артефакты не генерим
        """
        with patch_now(NOW):
            new_meta_res = generate_new_meta.ActMetaGen(
                mnclose_state(PAST_MONTH, 'closed'),
                tariffed_meta(LOG_INTERVAL_2),
                interim_meta(LOG_INTERVAL_2, CURR_RUN_ID, NOW, LOG_INTERVAL_1.end),
                interim_meta(LOG_INTERVAL_2, PREV_RUN_ID, NOW, LOG_INTERVAL_1.end),
                act_meta(LOG_INTERVAL_2, LOG_INTERVAL_2.beginning, PREV_RUN_ID, NOW),
                act_meta(LOG_INTERVAL_2, LOG_INTERVAL_2.beginning, PREV_RUN_ID, NOW),
                daily_meta(LOG_INTERVAL_1, LOG_INTERVAL_1.beginning, PREV_RUN_ID, NOW),
                daily_meta(LOG_INTERVAL_1, LOG_INTERVAL_1.beginning, PREV_RUN_ID, NOW, req_interval=LOG_INTERVAL_1),
                commands_meta(LOG_INTERVAL_1, PREV_RUN_ID, PREV_RUN_ID, NOW, LOG_INTERVAL_1.end, LOG_INTERVAL_1.end),
                commands_meta(LOG_INTERVAL_1, PREV_RUN_ID, PREV_RUN_ID, NOW, LOG_INTERVAL_1.end, LOG_INTERVAL_1.end),
                enqueued_commands(NEXT_RUN_ID),
                datetime.time(13, 0),
                datetime.time(20, 0),
                TAXES_DML_DT,
                DOCS_PARAMS_DML_DT,
                PRODUCT_DML_DT,
            ).do()

        assert everything_is_empty(new_meta_res)

    @pytest.mark.parametrize(
        'now_dt, switch_hour, res_dt',
        [
            pytest.param(NOW, 13, YESTERDAY, id='yesterday'),
            pytest.param(NOW, 12, TODAY, id='today'),
        ]
    )
    def test_day_switch(self, now_dt, switch_hour, res_dt):
        """
        Есть новые команды.
        Проверяем, что цикл интеримов подменен циклом команд
        """
        with patch_now(now_dt), patch_generate_run_id(NEXT_RUN_ID):
            new_meta_res = generate_new_meta.ActMetaGen(
                mnclose_state(PAST_MONTH, 'closed'),
                tariffed_meta(LOG_INTERVAL_3),
                interim_meta(LOG_INTERVAL_2, CURR_RUN_ID, NOW, LOG_INTERVAL_1.end),
                interim_meta(LOG_INTERVAL_2, PREV_RUN_ID, NOW, LOG_INTERVAL_1.end),
                act_meta(LOG_INTERVAL_2, LOG_INTERVAL_2.beginning, PREV_RUN_ID, NOW),
                act_meta(LOG_INTERVAL_2, LOG_INTERVAL_2.beginning, PREV_RUN_ID, NOW),
                daily_meta(LOG_INTERVAL_1, LOG_INTERVAL_1.beginning, PREV_RUN_ID, NOW),
                daily_meta(LOG_INTERVAL_1, LOG_INTERVAL_1.beginning, PREV_RUN_ID, NOW, req_interval=LOG_INTERVAL_1),
                commands_meta(LOG_INTERVAL_1, PREV_RUN_ID, PREV_RUN_ID, NOW, LOG_INTERVAL_1.end, LOG_INTERVAL_1.end),
                commands_meta(LOG_INTERVAL_1, PREV_RUN_ID, PREV_RUN_ID, NOW, LOG_INTERVAL_1.end, LOG_INTERVAL_1.end),
                enqueued_commands(NEXT_RUN_ID),
                datetime.time(switch_hour, 0),
                datetime.time(11, 0),
                TAXES_DML_DT,
                DOCS_PARAMS_DML_DT,
                PRODUCT_DML_DT,
            ).do()

        assert new_meta_res.new_acts_meta is None
        assert new_meta_res.new_daily_acts_meta is None
        assert new_meta_res.new_interim_meta is None

        assert new_meta_res.new_commands_meta == commands_meta(
            LOG_INTERVAL_3,
            PREV_RUN_ID,
            NEXT_RUN_ID,
            res_dt,
            LOG_INTERVAL_2.end,
            LOG_INTERVAL_1.end
        )
