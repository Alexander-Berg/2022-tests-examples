# -*- coding: utf-8 -*-

import contextlib
import logging
import dataclasses
from typing import Any

from unittest import mock

import pytest
import hamcrest

from yt.wrapper import (
    ypath_join,
)

from billing.log_tariffication.py.lib.constants import (
    LOG_TARIFF_META_ATTR,
    LOG_INTERVAL_KEY,
    RUN_ID_KEY,
    MONTHLY_INTERVAL_KEY,
    DAILY_INTERVAL_KEY
)
from billing.library.python.logmeta_utils.meta import generate_run_id
from billing.log_tariffication.py.jobs.core_acts.oltp_upload import acts2oltp

from billing.library.python.yt_utils.test_utils.utils import (
    create_subdirectory,
)
from billing.library.python.logfeller_utils.tests.utils import (
    mk_interval,
)
from billing.log_tariffication.py.tests.integration.core_acts.common import (
    TODAY,
    PAST,
    FUTURE,
    VERY_FUTURE,
)

log = logging.getLogger()

PREV_RUN_ID = generate_run_id(PAST)
CURR_RUN_ID = generate_run_id(TODAY)
NEXT_RUN_ID = generate_run_id(FUTURE)
VERY_NEXT_RUN_ID = generate_run_id(VERY_FUTURE)


@pytest.fixture(name='monthly_headers_dir')
def monthly_headers_dir_fixture(yt_client, yt_root):
    return create_subdirectory(yt_client, yt_root, 'act_headers')


@pytest.fixture(name='monthly_rows_dir')
def monthly_rows_dir_fixture(yt_client, yt_root):
    return create_subdirectory(yt_client, yt_root, 'act_rows')


@pytest.fixture(name='daily_headers_dir')
def daily_headers_dir_fixture(yt_client, yt_root):
    return create_subdirectory(yt_client, yt_root, 'daily_act_headers')


@pytest.fixture(name='daily_rows_dir')
def daily_rows_dir_fixture(yt_client, yt_root):
    return create_subdirectory(yt_client, yt_root, 'daily_act_rows')


@pytest.fixture(name='res_dir')
def res_fixture(yt_client, yt_root):
    return create_subdirectory(yt_client, yt_root, 'res')


@pytest.fixture(name='run_job')
def run_job_fixture(
    yql_client,
    monthly_headers_dir,
    monthly_rows_dir,
    daily_headers_dir,
    daily_rows_dir,
    res_dir,
):
    def _wrapped(yt_client, transaction, meta, *args, **kwargs):
        return acts2oltp.run_job(
            yt_client,
            yql_client,
            transaction.transaction_id,
            meta,
            monthly_headers_dir,
            monthly_rows_dir,
            daily_headers_dir,
            daily_rows_dir,
            res_dir,
            *args,
            **kwargs
        )

    return _wrapped


@contextlib.contextmanager
def patch_run_query():
    path = 'billing.log_tariffication.py.jobs.core_acts.oltp_upload.acts2oltp.utils.yql.run_query'
    with mock.patch(path) as mock_obj:
        yield mock_obj


@contextlib.contextmanager
def patch_set_meta():
    path = 'billing.log_tariffication.py.jobs.core_acts.oltp_upload.acts2oltp.utils.meta.set_log_tariff_meta'
    with mock.patch(path) as mock_obj:
        yield mock_obj


def create_rows_table(yt_client, path, data, meta):
    yt_client.create(
        'table',
        path,
        attributes={
            LOG_TARIFF_META_ATTR: meta,
            'schema': [
                {'name': 'act_id', 'type': 'string'},
                {'name': 'consume_id', 'type': 'uint64'},
                {'name': 'service_order_id', 'type': 'uint64'},
                {'name': 'acted_qty', 'type': 'double'},
                {'name': 'acted_sum', 'type': 'double'},
            ],
        }
    )

    yt_client.write_table(
        path,
        [
            {
                'act_id': aid,
                'consume_id': cid,
                'service_order_id': ssid,
                'acted_qty': float(q),
                'acted_sum': float(s),
            }
            for aid, cid, ssid, q, s in data
        ]
    )


def create_headers_table(yt_client, path, data, meta):
    yt_client.create(
        'table',
        path,
        attributes={
            LOG_TARIFF_META_ATTR: meta,
            'schema': [
                {'name': 'act_id', 'type': 'string'},
                {'name': 'invoice_id', 'type': 'uint64'},
                {'name': 'tax_policy_pct_id', 'type': 'uint64'},
                {'name': 'act_dt', 'type': 'uint64'},
                {'name': 'hidden', 'type': 'boolean'},
                {'name': 'ticket_id', 'type': 'string'}
            ],
        }
    )

    yt_client.write_table(
        path,
        [
            {
                'act_id': aid,
                'invoice_id': iid,
                'tax_policy_pct_id': tpid,
                'act_dt': dt.int_timestamp,
                'hidden': hidden,
                'ticket_id': ticket_id
            }
            for aid, iid, tpid, dt, hidden, ticket_id in data
        ]
    )


def get_upload_meta(log_interval, monthly_interval, daily_interval, run_id=None, **kwargs):
    meta = {
        LOG_INTERVAL_KEY: log_interval.to_meta(),
        MONTHLY_INTERVAL_KEY: monthly_interval.to_meta(),
        DAILY_INTERVAL_KEY: daily_interval.to_meta(),
        **kwargs
    }

    if run_id:
        meta[RUN_ID_KEY] = run_id

    return meta


def get_table_meta(log_interval, run_id=None, **kwargs):
    meta = {
        LOG_INTERVAL_KEY: log_interval.to_meta(),
        **kwargs
    }
    if run_id:
        meta[RUN_ID_KEY] = run_id

    return meta


@pytest.mark.parametrize(
    'oltp_upload_interval, monthly_interval, daily_interval',
    [
        pytest.param((3, 8), (0, 0), (3, 8), id='daily'),
        pytest.param((5, 15), (5, 15), (0, 0), id='monthly'),
        pytest.param((6, 15), (10, 15), (6, 8), id='daily_and_monthly')
    ]
)
def test_calculations(
    yt_client,
    run_job,
    monthly_headers_dir,
    monthly_rows_dir,
    daily_headers_dir,
    daily_rows_dir,
    oltp_upload_interval,
    monthly_interval,
    daily_interval
):
    @dataclasses.dataclass
    class ActInfo:
        act_id: Any = None
        invoice_id: Any = None
        tax_policy_pct_id: Any = None
        act_dt: Any = None
        hidden: Any = None
        ticket_id: Any = None

        @property
        def header_data_for_insert(self):
            return [
                self.act_id, self.invoice_id, self.tax_policy_pct_id,
                self.act_dt, self.hidden, self.ticket_id
            ]

        @property
        def rows_data_for_insert(self):
            rows = []
            qty = 0.01 if not self.hidden else -0.01
            sum = 0.01 if not self.hidden else -0.01
            for consume_id in range(3):
                for _ in range(100):
                    rows.append((self.act_id, consume_id, 14, qty, sum))

            return rows

    class Table:
        def __init__(self, period_type, acts_info, run_id, interval):
            if period_type == 'monthly':
                headers_dir = monthly_headers_dir
                rows_dir = monthly_rows_dir
            else:
                headers_dir = daily_headers_dir
                rows_dir = daily_rows_dir

            meta = get_table_meta(interval)

            headers_data = [ai.header_data_for_insert for ai in acts_info]
            create_headers_table(yt_client, ypath_join(headers_dir, run_id), headers_data, meta)

            all_rows_data = []
            for ai in acts_info:
                all_rows_data.extend(ai.rows_data_for_insert)

            create_rows_table(yt_client, ypath_join(rows_dir, run_id), all_rows_data, meta)

    Table(
        period_type='monthly',
        acts_info=[
            ActInfo(act_id='YB-1-M', invoice_id=2, tax_policy_pct_id=100, act_dt=TODAY, hidden=True, ticket_id='PAYSUP-111'),
            ActInfo(act_id='YB-2-M', invoice_id=2, tax_policy_pct_id=100, act_dt=PAST, hidden=False, ticket_id=None),
            ActInfo(act_id='YB-3-M', invoice_id=3, tax_policy_pct_id=101, act_dt=PAST, hidden=False, ticket_id=None),
        ],
        run_id=PREV_RUN_ID,
        interval=mk_interval(0, 5)
    )

    Table(
        period_type='monthly',
        acts_info=[

            ActInfo(act_id='YB-4-M', invoice_id=1, tax_policy_pct_id=101, act_dt=TODAY, hidden=False, ticket_id=None),
            ActInfo(act_id='YB-5-M', invoice_id=2, tax_policy_pct_id=100, act_dt=PAST, hidden=False, ticket_id=None),
            ActInfo(act_id='YB-6-M', invoice_id=2, tax_policy_pct_id=100, act_dt=TODAY, hidden=False, ticket_id=None),
            ActInfo(act_id='YB-7-M', invoice_id=1, tax_policy_pct_id=102, act_dt=PAST, hidden=False, ticket_id=None),
            ActInfo(act_id='YB-8-M', invoice_id=1, tax_policy_pct_id=100, act_dt=FUTURE, hidden=False, ticket_id=None),
            ActInfo(act_id='YB-7-M', invoice_id=1, tax_policy_pct_id=100, act_dt=PAST, hidden=True, ticket_id='PAYSUP-666'),
            ActInfo(act_id='YB-10-M', invoice_id=1, tax_policy_pct_id=100, act_dt=VERY_FUTURE, hidden=True, ticket_id='PAYSUP-777')
        ],
        run_id=CURR_RUN_ID,
        interval=mk_interval(5, 10)
    ),

    Table(
        period_type='monthly',
        acts_info=[
            ActInfo(act_id='YB-4-M', invoice_id=2, tax_policy_pct_id=100, act_dt=TODAY, hidden=True, ticket_id='PAYSUP-111'),
            ActInfo(act_id='YB-4-M', invoice_id=2, tax_policy_pct_id=100, act_dt=TODAY, hidden=False, ticket_id=None),
            ActInfo(act_id='YB-5-M', invoice_id=2, tax_policy_pct_id=100, act_dt=PAST, hidden=False, ticket_id=None),
        ],
        run_id=NEXT_RUN_ID,
        interval=mk_interval(10, 15)
    )

    Table(
        period_type='daily',
        acts_info=[
            ActInfo(act_id='YB-1-D', invoice_id=1, tax_policy_pct_id=101, act_dt=PAST, hidden=False, ticket_id=None),
            ActInfo(act_id='YB-2-D', invoice_id=2, tax_policy_pct_id=100, act_dt=PAST, hidden=False, ticket_id=None),
            ActInfo(act_id='YB-3-D', invoice_id=2, tax_policy_pct_id=100, act_dt=TODAY, hidden=False, ticket_id=None),
            ActInfo(act_id='YB-4-D', invoice_id=1, tax_policy_pct_id=102, act_dt=PAST, hidden=False, ticket_id=None),
            ActInfo(act_id='YB-5-D', invoice_id=5, tax_policy_pct_id=100, act_dt=FUTURE, hidden=False, ticket_id=None),
            ActInfo(act_id='YB-5-D', invoice_id=6, tax_policy_pct_id=100, act_dt=FUTURE, hidden=True, ticket_id='PAYSUP-666'),
            ActInfo(act_id='YB-6-D', invoice_id=1, tax_policy_pct_id=100, act_dt=VERY_FUTURE, hidden=True, ticket_id='PAYSUP-777')
        ],
        run_id=PREV_RUN_ID,
        interval=mk_interval(3, 6)
    )

    Table(
        period_type='daily',
        acts_info=[
            ActInfo(act_id='YB-3-D', invoice_id=2, tax_policy_pct_id=100, act_dt=TODAY, hidden=True, ticket_id='PAYSUP-111'),
            ActInfo(act_id='YB-4-D', invoice_id=2, tax_policy_pct_id=100, act_dt=PAST, hidden=False, ticket_id=None),
            ActInfo(act_id='YB-7-D', invoice_id=2, tax_policy_pct_id=100, act_dt=PAST, hidden=False, ticket_id=None),
        ],
        run_id=CURR_RUN_ID,
        interval=mk_interval(6, 8)
    ),

    Table(
        period_type='daily',
        acts_info=[
            ActInfo(act_id='YB-8-D', invoice_id=2, tax_policy_pct_id=100, act_dt=TODAY, hidden=True, ticket_id='PAYSUP-111'),
            ActInfo(act_id='YB-9-D', invoice_id=2, tax_policy_pct_id=100, act_dt=PAST, hidden=False, ticket_id=None),
            ActInfo(act_id='YB-10-D', invoice_id=2, tax_policy_pct_id=100, act_dt=PAST, hidden=False, ticket_id=None),
        ],
        run_id=NEXT_RUN_ID,
        interval=mk_interval(8, 13)
    )

    with yt_client.Transaction() as transaction:
        res_path = run_job(
            yt_client,
            transaction,
            get_upload_meta(
                mk_interval(*oltp_upload_interval),
                mk_interval(*monthly_interval),
                mk_interval(*daily_interval),
                PREV_RUN_ID,
                some='random crap'
            )
        )
    rows = list(yt_client.read_table(res_path))
    log.info('rows = {}'.format(rows))

    return {
        'rows': rows,
        'tbl_meta': yt_client.get(ypath_join(res_path, '@' + LOG_TARIFF_META_ATTR)),
    }


def test_rows_range(yt_client, run_job, monthly_headers_dir, monthly_rows_dir, daily_headers_dir, daily_rows_dir):
    create_rows_table(yt_client, ypath_join(monthly_rows_dir, 't00'), [], get_table_meta(mk_interval(0, 10)))
    create_rows_table(yt_client, ypath_join(monthly_rows_dir, 't10'), [], get_table_meta(mk_interval(10, 15)))
    create_rows_table(yt_client, ypath_join(monthly_rows_dir, 't15'), [], get_table_meta(mk_interval(15, 25)))
    create_rows_table(yt_client, ypath_join(monthly_rows_dir, 't30'), [], get_table_meta(mk_interval(25, 35)))
    create_rows_table(yt_client, ypath_join(daily_rows_dir, 't20'), [], get_table_meta(mk_interval(20, 25)))
    create_rows_table(yt_client, ypath_join(daily_rows_dir, 't25'), [], get_table_meta(mk_interval(25, 30)))
    create_rows_table(yt_client, ypath_join(daily_rows_dir, 't30'), [], get_table_meta(mk_interval(30, 40)))

    create_headers_table(yt_client, ypath_join(monthly_headers_dir, 't066'), [], get_table_meta(mk_interval(0, 10)))
    create_headers_table(yt_client, ypath_join(monthly_headers_dir, 't166'), [], get_table_meta(mk_interval(10, 15)))
    create_headers_table(yt_client, ypath_join(monthly_headers_dir, 't167'), [], get_table_meta(mk_interval(15, 25)))
    create_headers_table(yt_client, ypath_join(monthly_headers_dir, 't168'), [], get_table_meta(mk_interval(25, 30)))
    create_headers_table(yt_client, ypath_join(daily_headers_dir, 't266'), [], get_table_meta(mk_interval(10, 20)))
    create_headers_table(yt_client, ypath_join(daily_headers_dir, 't267'), [], get_table_meta(mk_interval(20, 30)))
    create_headers_table(yt_client, ypath_join(daily_headers_dir, 't366'), [], get_table_meta(mk_interval(30, 666)))

    with patch_run_query() as mock_obj, patch_set_meta():
        with yt_client.Transaction() as transaction:
            run_job(
                yt_client,
                transaction,
                get_upload_meta(mk_interval(10, 40), mk_interval(10, 25), mk_interval(20, 30), 't10')
            )

    hamcrest.assert_that(
        mock_obj.call_args[0][2],
        hamcrest.has_entries({
            '$monthly_headers_dir': monthly_headers_dir,
            '$monthly_rows_dir': monthly_rows_dir,
            '$daily_headers_dir': daily_headers_dir,
            '$daily_rows_dir': daily_rows_dir,
            '$monthly_headers_first_table': 't166',
            '$monthly_headers_last_table': 't167',
            '$monthly_rows_first_table': 't10',
            '$monthly_rows_last_table': 't15',
            '$daily_headers_first_table': 't267',
            '$daily_headers_last_table': 't267',
            '$daily_rows_first_table': 't20',
            '$daily_rows_last_table': 't25'
        })
    )


def test_already_processed(yt_client, run_job, monthly_headers_dir, monthly_rows_dir, res_dir):
    create_headers_table(yt_client, ypath_join(monthly_headers_dir, 't066'), [], get_table_meta(mk_interval(0, 666)))
    create_rows_table(yt_client, ypath_join(monthly_rows_dir, 't40'), [], get_table_meta(mk_interval(0, 666)))

    meta = get_upload_meta(mk_interval(0, 666), mk_interval(0, 666), mk_interval(0, 0), 't066', crap='some')
    create_headers_table(yt_client, ypath_join(res_dir, 't066'), [], meta)

    with yt_client.Transaction() as transaction:
        res_path = run_job(yt_client, transaction, meta)

    assert res_path == ypath_join(res_dir, 't066')


def test_result_wrong_meta(
    yt_client,
    run_job,
    daily_headers_dir,
    daily_rows_dir,
    res_dir
):
    create_headers_table(yt_client, ypath_join(daily_headers_dir, 't066'), [], get_table_meta(mk_interval(0, 666)))
    create_rows_table(yt_client, ypath_join(daily_rows_dir, 't40'), [], get_table_meta(mk_interval(0, 666)))

    create_headers_table(yt_client, ypath_join(res_dir, 't066'), [], get_table_meta(mk_interval(0, 666), NEXT_RUN_ID))

    with pytest.raises(AssertionError) as exc_info:
        with patch_run_query() as mock_obj, yt_client.Transaction() as transaction:
            run_job(
                yt_client,
                transaction,
                get_upload_meta(mk_interval(0, 666), mk_interval(0, 0), mk_interval(0, 666), 't066', crap='some')
            )

    assert not mock_obj.called
    assert 'Bad meta in current table for {}'.format(res_dir) in exc_info.value.args[0]


def test_result_sequence_break(
    yt_client,
    run_job,
    monthly_headers_dir,
    monthly_rows_dir,
    res_dir
):
    create_rows_table(yt_client, ypath_join(monthly_rows_dir, 't00'), [], get_table_meta(mk_interval(10, 30)))
    create_headers_table(yt_client, ypath_join(monthly_headers_dir, 't066'), [], get_table_meta(mk_interval(10, 30)))

    create_headers_table(yt_client, ypath_join(res_dir, 't066'), [], get_table_meta(mk_interval(0, 1)))

    with patch_run_query() as mock_obj, patch_set_meta():
        with yt_client.Transaction() as transaction:
            run_job(
                yt_client,
                transaction,
                get_upload_meta(mk_interval(10, 20), mk_interval(10, 30), mk_interval(5, 5), 't10'),
            )

    assert mock_obj.called
