# -*- coding: utf-8 -*-

import pytest
import arrow
import contextlib
from unittest import mock

from billing.log_tariffication.py.jobs.core_acts.oltp_upload import generate_new_meta
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
    MONTHLY_INTERVAL_KEY,
    DAILY_INTERVAL_KEY,
    LOG_TARIFF_META_ATTR,
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


@contextlib.contextmanager
def yt_client_mock():
    with mock.patch('yt.wrapper.YtClient') as m:
        m.exists.return_value = True
        yield m


class Table:
    def __init__(self, name, attributes):
        self.name: str = name
        self.attributes: dict = attributes

    def __str__(self):
        return self.name


NOW = arrow.get('2020-06-06 12:00:00').replace(tzinfo=MSK_TZ)
TODAY = NOW.floor('day')


LOG_INTERVAL_1 = LogInterval([Subinterval('a', 'a', 0, 0, 10)])
LOG_INTERVAL_2 = LogInterval([Subinterval('a', 'a', 0, 10, 20)])
LOG_INTERVAL_3 = LogInterval([Subinterval('a', 'a', 0, 20, 30)])


MONTHLY_HEADERS_DIR = '//home/balance/prod/log_tariff/public/income/bs/act_headers'
DAILY_HEADERS_DIR = '//home/balance/prod/log_tariff/public/income/bs/daily_act_headers'


def upload_meta(log_interval, monthly_interval, daily_interval, run_id):
    return {
        LOG_INTERVAL_KEY: log_interval.to_meta(),
        MONTHLY_INTERVAL_KEY: monthly_interval.to_meta(),
        DAILY_INTERVAL_KEY: daily_interval.to_meta(),
        RUN_ID_KEY: run_id
    }


def acts_meta(log_interval):
    return {
        LOG_INTERVAL_KEY: log_interval.to_meta(),

        # не используется
        RUN_ID_KEY: CURR_RUN_ID,
        ACT_DT_KEY: TODAY,
        ACT_SEQUENCE_POS_KEY: 666,
        DAILY_ACTED_SLICE_KEY: log_interval.to_meta(),
    }


def daily_acts_meta(log_interval):
    return {
        LOG_INTERVAL_KEY: log_interval.to_meta(),

        # не используется
        RUN_ID_KEY: CURR_RUN_ID,
        ACT_DT_KEY: TODAY,
        ACT_SEQUENCE_POS_KEY: 666,
        ACTED_SLICE_KEY: log_interval.to_meta(),
        REF_INTERVAL_KEY_FORMATTER('act_requests'): None,
    }


def commands_meta(log_interval):
    return {
        LOG_INTERVAL_KEY: log_interval.to_meta(),

        # не используется
        RUN_ID_KEY: CURR_RUN_ID,
        ACT_DT_KEY: TODAY,
        ACTED_SLICE_KEY: log_interval.to_meta(),
        REF_INTERVAL_KEY_FORMATTER('taxes'): TODAY,
        REF_INTERVAL_KEY_FORMATTER('docs_params'): TODAY,
        CORRECTIONS_LOG_INTERVAL_KEY: None,
        PREV_COMMANDS_TABLE: CURR_RUN_ID,
        LAST_COMMANDS_TABLE: CURR_RUN_ID,
    }


def headers_table_meta(run_id, log_interval):
    return {
        RUN_ID_KEY: run_id,
        LOG_TARIFF_META_ATTR: {
            LOG_INTERVAL_KEY: log_interval.to_meta()
        }
    }


@pytest.mark.parametrize(
    'oltp_upload_meta,'
    'processed_oltp_upload_meta,'
    'monthly_tables,'
    'daily_tables,'
    'res_meta',
    [
        pytest.param(
            upload_meta(
                LogInterval([Subinterval('a', 'a', 0, 0, 5)]),
                LogInterval([Subinterval('a', 'a', 0, 0, 5)]),
                LogInterval([Subinterval('a', 'a', 0, 0, 0)]),
                PREV_RUN_ID
            ),
            upload_meta(
                LogInterval([Subinterval('a', 'a', 0, 0, 5)]),
                LogInterval([Subinterval('a', 'a', 0, 0, 5)]),
                LogInterval([Subinterval('a', 'a', 0, 0, 0)]),
                PREV_RUN_ID
            ),
            [
                Table(PREV_RUN_ID, headers_table_meta(PREV_RUN_ID, LogInterval([Subinterval('a', 'a', 0, 5, 10)]))),
                Table(PREV_RUN_ID, headers_table_meta(PREV_RUN_ID, LogInterval([Subinterval('a', 'a', 0, 10, 15)])))
            ],
            [],
            upload_meta(
                LogInterval([Subinterval('a', 'a', 0, 5, 15)]),
                LogInterval([Subinterval('a', 'a', 0, 5, 15)]),
                LogInterval([Subinterval('a', 'a', 0, 0, 0)]),
                NEXT_RUN_ID
            ),
            id='only_monthly_acts'
        ),
        pytest.param(
            upload_meta(
                LogInterval([Subinterval('a', 'a', 0, 0, 5)]),
                LogInterval([Subinterval('a', 'a', 0, 1, 1)]),
                LogInterval([Subinterval('a', 'a', 0, 0, 5)]),
                PREV_RUN_ID
            ),
            upload_meta(
                LogInterval([Subinterval('a', 'a', 0, 0, 5)]),
                LogInterval([Subinterval('a', 'a', 0, 1, 1)]),
                LogInterval([Subinterval('a', 'a', 0, 0, 5)]),
                PREV_RUN_ID
            ),
            [],
            [
                Table(PREV_RUN_ID, headers_table_meta(PREV_RUN_ID, LogInterval([Subinterval('a', 'a', 0, 5, 10)]))),
                Table(PREV_RUN_ID, headers_table_meta(PREV_RUN_ID, LogInterval([Subinterval('a', 'a', 0, 10, 15)])))
            ],
            upload_meta(
                LogInterval([Subinterval('a', 'a', 0, 5, 15)]),
                LogInterval([Subinterval('a', 'a', 0, 1, 1)]),
                LogInterval([Subinterval('a', 'a', 0, 5, 15)]),
                NEXT_RUN_ID
            ),
            id='only_daily_acts'
        ),
        pytest.param(
            upload_meta(
                LogInterval([Subinterval('a', 'a', 0, 0, 5)]),
                LogInterval([Subinterval('a', 'a', 0, 1, 2)]),
                LogInterval([Subinterval('a', 'a', 0, 0, 5)]),
                PREV_RUN_ID
            ),
            upload_meta(
                LogInterval([Subinterval('a', 'a', 0, 0, 5)]),
                LogInterval([Subinterval('a', 'a', 0, 1, 2)]),
                LogInterval([Subinterval('a', 'a', 0, 0, 5)]),
                PREV_RUN_ID
            ),
            [
                Table(PREV_RUN_ID, headers_table_meta(PREV_RUN_ID, LogInterval([Subinterval('a', 'a', 0, 0, 1)]))),
                Table(PREV_RUN_ID, headers_table_meta(PREV_RUN_ID, LogInterval([Subinterval('a', 'a', 0, 1, 2)]))),
                Table(PREV_RUN_ID, headers_table_meta(PREV_RUN_ID, LogInterval([Subinterval('a', 'a', 0, 15, 17)])))
            ],
            [
                Table(PREV_RUN_ID, headers_table_meta(PREV_RUN_ID, LogInterval([Subinterval('a', 'a', 0, 0, 5)]))),
                Table(PREV_RUN_ID, headers_table_meta(PREV_RUN_ID, LogInterval([Subinterval('a', 'a', 0, 5, 15)])))
            ],
            upload_meta(
                LogInterval([Subinterval('a', 'a', 0, 5, 17)]),
                LogInterval([Subinterval('a', 'a', 0, 2, 17)]),
                LogInterval([Subinterval('a', 'a', 0, 5, 15)]),
                NEXT_RUN_ID
            ),
            id='monthly_and_daily'
        )
    ]
)
def test_base(
    oltp_upload_meta,
    processed_oltp_upload_meta,
    monthly_tables,
    daily_tables,
    res_meta
):
    with patch_generate_run_id(NEXT_RUN_ID), yt_client_mock() as yt_client:
        def side_effect_func(log_dir, **kwargs):
            if log_dir == MONTHLY_HEADERS_DIR:
                return monthly_tables
            elif log_dir == DAILY_HEADERS_DIR:
                return daily_tables
            else:
                raise Exception('Unknown log_did %s' % log_dir)

        yt_client.list.side_effect = side_effect_func

        new_oltp_upload_meta = generate_new_meta.OltpUploadMetaGenerator(
            yt_client=yt_client,
            oltp_upload_meta=oltp_upload_meta,
            processed_oltp_upload_meta=processed_oltp_upload_meta,
            monthly_headers_dir=MONTHLY_HEADERS_DIR,
            daily_headers_dir=DAILY_HEADERS_DIR
        ).generate()

    assert new_oltp_upload_meta == res_meta


def test_unprocessed():
    with yt_client_mock() as yt_client:
        new_oltp_upload_meta = generate_new_meta.OltpUploadMetaGenerator(
            yt_client=yt_client,
            oltp_upload_meta=upload_meta(LOG_INTERVAL_2, LOG_INTERVAL_2, LOG_INTERVAL_1, CURR_RUN_ID),
            processed_oltp_upload_meta=upload_meta(LOG_INTERVAL_1, LOG_INTERVAL_1, LOG_INTERVAL_1, PREV_RUN_ID),
            monthly_headers_dir=MONTHLY_HEADERS_DIR,
            daily_headers_dir=DAILY_HEADERS_DIR
        ).generate()

    assert new_oltp_upload_meta is None


def test_mismatched_prev():
    with pytest.raises(AssertionError) as exc_info:
        with yt_client_mock() as yt_client:
            generate_new_meta.OltpUploadMetaGenerator(
                yt_client=yt_client,
                oltp_upload_meta=upload_meta(LOG_INTERVAL_2, LOG_INTERVAL_1, LOG_INTERVAL_1, CURR_RUN_ID),
                processed_oltp_upload_meta=upload_meta(LOG_INTERVAL_3, LOG_INTERVAL_1, LOG_INTERVAL_1, PREV_RUN_ID),
                monthly_headers_dir=MONTHLY_HEADERS_DIR,
                daily_headers_dir=DAILY_HEADERS_DIR
            ).generate()

    assert 'intervals mismatch' in exc_info.value.args[0]


def test_inconsistent_run_id():
    with pytest.raises(AssertionError) as exc_info:
        with patch_generate_run_id(PREV_RUN_ID), yt_client_mock() as yt_client:
            yt_client.list.return_value = [
                Table(CURR_RUN_ID, headers_table_meta(CURR_RUN_ID, LOG_INTERVAL_1)),
                Table(NEXT_RUN_ID, headers_table_meta(NEXT_RUN_ID, LOG_INTERVAL_2)),
            ]

            generate_new_meta.OltpUploadMetaGenerator(
                yt_client=yt_client,
                oltp_upload_meta=upload_meta(LOG_INTERVAL_1, LOG_INTERVAL_1, LOG_INTERVAL_1, CURR_RUN_ID),
                processed_oltp_upload_meta=upload_meta(LOG_INTERVAL_1, LOG_INTERVAL_1, LOG_INTERVAL_1, CURR_RUN_ID),
                monthly_headers_dir=MONTHLY_HEADERS_DIR,
                daily_headers_dir=DAILY_HEADERS_DIR
            ).generate()

    assert 'inconsequential run_id for oltp upload' in exc_info.value.args[0]


def test_no_new_acts():
    with yt_client_mock() as yt_client:
        yt_client.list.return_value = [
            Table(CURR_RUN_ID, headers_table_meta(CURR_RUN_ID, LOG_INTERVAL_1)),
        ]
        new_oltp_upload_meta = generate_new_meta.OltpUploadMetaGenerator(
            yt_client=yt_client,
            oltp_upload_meta=upload_meta(LOG_INTERVAL_1, LOG_INTERVAL_1, LOG_INTERVAL_1, CURR_RUN_ID),
            processed_oltp_upload_meta=upload_meta(LOG_INTERVAL_1, LOG_INTERVAL_1, LOG_INTERVAL_1, CURR_RUN_ID),
            monthly_headers_dir=MONTHLY_HEADERS_DIR,
            daily_headers_dir=DAILY_HEADERS_DIR
        ).generate()

    assert new_oltp_upload_meta is None
