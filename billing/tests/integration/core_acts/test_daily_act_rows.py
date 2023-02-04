# -*- coding: utf-8 -*-

import contextlib
import re
import math
from unittest import mock

import pytest
import hamcrest

from yt.wrapper import (
    ypath_join,
)

from billing.library.python.logfeller_utils.log_interval import (
    LB_META_ATTR,
)
from billing.log_tariffication.py.lib.constants import (
    LOG_INTERVAL_KEY,
    ACT_SEQUENCE_POS_KEY,
    RUN_ID_KEY,
    ACT_DT_KEY,
    ACTED_SLICE_KEY,
    REF_INTERVAL_KEY_FORMATTER,
)
from billing.log_tariffication.py.jobs.core_acts import daily_act_rows

from billing.library.python.yql_utils import yql, query_metrics
from billing.library.python.yt_utils.test_utils.utils import (
    create_subdirectory,
)
from billing.library.python.logfeller_utils.tests.utils import (
    mk_interval,
)
from billing.log_tariffication.py.tests.constants import (
    PREV_RUN_ID,
    CURR_RUN_ID,
    NEXT_RUN_ID,
    LOGFELLER_TABLE_SCHEMA,
)
from billing.log_tariffication.py.tests.integration.core_acts.common import (
    create_interim_rows_table,
    create_daily_table,
    create_headers_table,
    get_result_meta,
    TODAY,
    PAST,
    VERY_PAST,
    FUTURE,
    VERY_FUTURE,
)
from billing.log_tariffication.py.tests.utils import (
    dict2matcher,
)

TAX_POLICY_PCT_1 = 1
TAX_POLICY_PCT_2 = 2


@pytest.fixture(name='rows_dir')
def rows_dir_fixture(yt_client, yt_root):
    return create_subdirectory(yt_client, yt_root, 'rows')


@pytest.fixture(name='unprocessed_dir')
def unprocessed_dir_fixture(yt_client, yt_root):
    return create_subdirectory(yt_client, yt_root, 'unprocessed')


@pytest.fixture(name='daily_rows_dir')
def daily_rows_dir_fixture(yt_client, yt_root):
    return create_subdirectory(yt_client, yt_root, 'daily_rows')


@pytest.fixture(name='act_requests_dir')
def act_requests_dir_fixture(yt_client, yt_root):
    return create_subdirectory(yt_client, yt_root, 'act_requests_dir')


@pytest.fixture(name='res_rows_dir')
def res_rows_dir_fixture(yt_client, yt_root):
    return create_subdirectory(yt_client, yt_root, 'res_rows')


@pytest.fixture(name='res_headers_dir')
def res_headers_dir_fixture(yt_client, yt_root):
    return create_subdirectory(yt_client, yt_root, 'res_headers')


@pytest.fixture(name='run_job')
def run_job_fixture(
    yql_client,
    rows_dir,
    unprocessed_dir,
    daily_rows_dir,
    act_requests_dir,
    res_rows_dir,
    res_headers_dir,
):
    def _wrapped(yt_client, transaction, meta, *args, **kwargs):
        return daily_act_rows.run_job(
            yt_client,
            yql_client,
            transaction.transaction_id,
            meta,
            rows_dir,
            unprocessed_dir,
            daily_rows_dir,
            act_requests_dir,
            res_rows_dir,
            res_headers_dir,
            *args,
            **kwargs
        )

    return _wrapped


@contextlib.contextmanager
def patch_run_query(yql_client, res_act_id):
    run_query_path = 'billing.log_tariffication.py.jobs.core_acts.daily_act_rows.utils.yql.run_query'
    extract_metrics_path = 'billing.log_tariffication.py.jobs.core_acts.daily_act_rows.query_metrics.extract_metrics'
    write_metrics_path = 'billing.log_tariffication.py.jobs.core_acts.daily_act_rows.query_metrics.write_metrics'

    query = f'SELECT {res_act_id} INTO RESULT `act_sequence_pos`;'
    request = yql.run_query(yql_client, query)

    with mock.patch(run_query_path, return_value=request) as mock_obj, \
            mock.patch(extract_metrics_path), \
            mock.patch(write_metrics_path):
        yield mock_obj


@contextlib.contextmanager
def patch_set_meta():
    path = 'billing.log_tariffication.py.jobs.core_acts.daily_act_rows.utils.meta.set_log_tariff_meta'
    with mock.patch(path) as mock_obj:
        yield mock_obj


def get_meta(log_interval, run_id=None, act_dt=None, act_seq_pos=None, act_slice=None, act_requests_interval=None):
    meta = {
        LOG_INTERVAL_KEY: log_interval.to_meta(),
    }
    if run_id is not None:
        meta[RUN_ID_KEY] = run_id
    if act_dt is not None:
        meta[ACT_DT_KEY] = act_dt.strftime('%Y-%m-%d')
    if act_seq_pos is not None:
        meta[ACT_SEQUENCE_POS_KEY] = act_seq_pos
    if act_slice is not None:
        meta[ACTED_SLICE_KEY] = act_slice.to_meta()
    if act_requests_interval:
        meta[REF_INTERVAL_KEY_FORMATTER('act_requests')] = act_requests_interval.to_meta()
    return meta


def create_requests_table(yt_client, path, data, log_interval):
    yt_client.create(
        'table',
        path,
        attributes={
            LB_META_ATTR: log_interval.to_meta(),
            'schema': [
                {'name': 'invoice_id', 'type': 'uint64'},
                {'name': 'amount', 'type': 'double'},
                {'name': 'Version', 'type': 'uint64'},
            ] + LOGFELLER_TABLE_SCHEMA,
        }
    )

    subinterval, = log_interval.subintervals.values()
    num_offsets = subinterval.next_offset - subinterval.first_offset
    offset_size = int(math.ceil(float(len(data)) / num_offsets))

    yt_client.write_table(
        path,
        [
            {
                'invoice_id': invoice_id,
                'amount': float(amount),
                'Version': version_id,
                '_topic_cluster': subinterval.cluster,
                '_topic': subinterval.topic,
                '_partition': subinterval.partition,
                '_offset': subinterval.first_offset + idx // offset_size,
                '_chunk_record_index': 1 + idx % offset_size,
            }
            for idx, (invoice_id, amount, version_id) in enumerate(data)
        ]
    )


def get_result(yt_client, res):
    uid_pattern = re.compile(r'(.*)_\d+')

    res_meta, rows_path, headers_path = res
    cases = {}
    act2case = {}
    for row in sorted(yt_client.read_table(rows_path), key=lambda r: r['UID']):
        case_name = uid_pattern.match(row['UID']).group(1)
        cases.setdefault(case_name, {}).setdefault('rows', []).append(row)
        act2case[row['act_id']] = case_name

    for row in sorted(yt_client.read_table(headers_path), key=lambda r: r['act_id']):
        case_name = act2case.get(row['act_id'], '_unknown_header')
        cases.setdefault(case_name, {}).setdefault('headers', []).append(row)

    meta = {
        'rows': get_result_meta(yt_client, rows_path),
        'headers': get_result_meta(yt_client, headers_path),
    }

    res_meta = res_meta.copy()
    res_run_id = res_meta.pop('run_id')
    assert res_run_id

    return {'cases': cases, 'table_meta': meta, 'res_meta': res_meta}


def unpack_cases(cases):
    rows = []
    unprocessed = []
    daily = []
    requests = []

    for case in cases:
        rows.extend(case.rows)
        unprocessed.extend(case.unprocessed)
        daily.extend(case.daily)
        requests.extend(case.requests)

    return rows, unprocessed, daily, requests


class Case:
    def __init__(self, uid_prefix=''):
        self._uid_prefix = uid_prefix
        self._row_id = 1
        self._service_order_id = 1

        self.rows = []
        self.unprocessed = []
        self.daily = []
        self.requests = []

    def _fmt_rows(self, invoice_id, act_sum, dt=TODAY, tpp_id=TAX_POLICY_PCT_1):
        idx = '{}_{:05}'.format(self._uid_prefix, self._row_id)
        self._row_id += 1
        return (
            idx,
            dt,
            invoice_id,
            tpp_id,
            act_sum,
            self._service_order_id
        )

    def add_row(self, *args, is_daily=False, **kwargs):
        row = self._fmt_rows(*args, **kwargs)
        self.rows.append(row)
        if is_daily:
            self.daily.append(row[:1])
        return self

    def add_unprocessed(self, *args, is_daily=False, **kwargs):
        row = self._fmt_rows(*args, **kwargs)
        self.unprocessed.append(row)
        if is_daily:
            self.daily.append(row[:1])
        return self

    def add_request(self, invoice_id, amount, version_id=0):
        self.requests.append((invoice_id, amount, version_id))
        return self


def _gen_rounding_case(invoice_id):
    case = Case('rounding')
    for idx in range(100):
        case.add_row(invoice_id, 0.01)
    for idx in range(99):
        case.add_unprocessed(invoice_id, -0.01)
    case.add_request(invoice_id, 0.01)
    return case


CASES = [
    Case('skipped_prev')
    .add_row(1, 666, TODAY)
    .add_row(2, 666, TODAY)
    .add_request(1, 666)
    .add_request(2, 666),
    Case('grouping')
    .add_row(3, 10.66, tpp_id=TAX_POLICY_PCT_1)
    .add_unprocessed(3, 5.44, tpp_id=TAX_POLICY_PCT_1)
    .add_row(3, -13.42, tpp_id=TAX_POLICY_PCT_1)
    .add_row(3, 666, tpp_id=TAX_POLICY_PCT_2)
    .add_unprocessed(4, 13, tpp_id=TAX_POLICY_PCT_2)
    .add_request(3, 668.68)
    .add_request(4, 13),
    Case('daily')
    .add_row(5, 10, TODAY)
    .add_row(5, 20, TODAY, is_daily=True)
    .add_row(5, 30, TODAY)
    .add_unprocessed(5, 40, TODAY, is_daily=True)
    .add_unprocessed(5, 50, TODAY)
    .add_request(5, 90),
    Case('dt_filter')
    .add_row(6, 4, VERY_PAST)
    .add_row(6, 5, PAST)
    .add_row(6, 6, TODAY)
    .add_row(6, 7, FUTURE)
    .add_row(6, 8, VERY_FUTURE)
    .add_request(6, 15),
    Case('zero_sum')
    .add_row(7, 6.66)
    .add_row(7, -3.21)
    .add_row(7, -3.45)
    .add_request(7, 0),
    Case('neg_sum')
    .add_row(8, 6.66)
    .add_row(8, -3.22)
    .add_row(8, -3.45)
    .add_request(8, -0.01),
    _gen_rounding_case(9),
    Case('dt_neg_sum')
    .add_row(10, -100, VERY_PAST)
    .add_row(10, 66, PAST)
    .add_row(10, 10, TODAY)
    .add_row(10, 666, FUTURE)
    .add_request(10, -24),
    Case('sum_filter')
    .add_row(13, 10, TODAY)
    .add_request(13, 11)
    .add_row(14, 10, TODAY)
    .add_request(14, 9),
    Case('skipped_after')
    .add_row(11, 666, TODAY)
    .add_request(11, 666)
    .add_row(12, 666, TODAY)
    .add_request(12, 666),
]


def test_calculations(yt_client, run_job, rows_dir, daily_rows_dir, unprocessed_dir, act_requests_dir):
    rows, unprocessed, daily, requests = unpack_cases(CASES)

    create_interim_rows_table(
        yt_client,
        ypath_join(unprocessed_dir, PREV_RUN_ID),
        unprocessed,
        get_meta(mk_interval(0, 10))
    )
    create_interim_rows_table(
        yt_client,
        ypath_join(rows_dir, PREV_RUN_ID),
        rows[:len(rows) // 2],
        get_meta(mk_interval(10, 20))
    )
    create_interim_rows_table(
        yt_client,
        ypath_join(rows_dir, CURR_RUN_ID),
        rows[len(rows) // 2:],
        get_meta(mk_interval(20, 30))
    )

    create_daily_table(
        yt_client,
        ypath_join(daily_rows_dir, PREV_RUN_ID),
        daily,
        get_meta(mk_interval(10, 20))
    )

    create_requests_table(
        yt_client,
        ypath_join(act_requests_dir, '1990-01-01T00:00:00'),
        requests[0:2],
        mk_interval(0, 2)
    )
    create_requests_table(
        yt_client,
        ypath_join(act_requests_dir, '1990-01-01T00:05:00'),
        requests[2:6],
        mk_interval(2, 6)
    )
    create_requests_table(
        yt_client,
        ypath_join(act_requests_dir, '1990-01-01T00:10:00'),
        requests[6:-2],
        mk_interval(6, 10)
    )
    create_requests_table(
        yt_client,
        ypath_join(act_requests_dir, '1990-01-01T00:15:00'),
        requests[-2:],
        mk_interval(10, 12)
    )

    with yt_client.Transaction() as transaction:
        res = run_job(
            yt_client,
            transaction,
            get_meta(
                mk_interval(20, 30),
                NEXT_RUN_ID,
                TODAY,
                666,
                mk_interval(10, 20).beginning,
                mk_interval(1, 11)
            )
        )

    res_meta, res_rows_path, res_headers_path = res

    rows_metrics = query_metrics.get_table_metrics_data(
        yt_client, res_rows_path
    )

    rows_metrics_matchers = map(dict2matcher, [
        {'name': 'acted_sum', 'type': 'float', 'value': 2118.69},
        {'name': 'acted_qty', 'type': 'float', 'value': 4237.38},
        {'name': 'acted_sum_pos', 'type': 'float', 'value': 2139.76},
        {'name': 'acted_qty_pos', 'type': 'float', 'value': 4279.52},
        {'name': 'acted_sum_neg', 'type': 'float', 'value': -21.07},
        {'name': 'acted_qty_neg', 'type': 'float', 'value': -42.14}
    ])

    hamcrest.assert_that(
        rows_metrics,
        hamcrest.contains_inanyorder(*rows_metrics_matchers),
    )

    return get_result(yt_client, res)


@pytest.mark.parametrize(
    'main_interval, act_interval, rows_first_table, rows_last_table, daily_rows_first_table, daily_rows_last_table',
    [
        pytest.param(mk_interval(40, 50), mk_interval(0, 10), 't10', 't40', 't10', 't30', id='base'),
        pytest.param(mk_interval(10, 50), mk_interval(0, 10), 't10', 't40', '', '', id='from_start'),
        pytest.param(mk_interval(10, 50), mk_interval(0, 30), 't30', 't40', '', '', id='intersect'),
    ]
)
def test_rows_range(
    yt_client, yql_client, run_job, rows_dir, daily_rows_dir, unprocessed_dir, act_requests_dir,
    main_interval, act_interval, rows_first_table, rows_last_table, daily_rows_first_table, daily_rows_last_table
):
    create_interim_rows_table(yt_client, ypath_join(unprocessed_dir, 't00'), [], get_meta(act_interval))

    create_interim_rows_table(yt_client, ypath_join(rows_dir, 't00'), [], get_meta(mk_interval(0, 10)))
    create_interim_rows_table(yt_client, ypath_join(rows_dir, 't10'), [], get_meta(mk_interval(10, 20)))
    create_interim_rows_table(yt_client, ypath_join(rows_dir, 't20'), [], get_meta(mk_interval(20, 30)))
    create_interim_rows_table(yt_client, ypath_join(rows_dir, 't30'), [], get_meta(mk_interval(30, 40)))
    create_interim_rows_table(yt_client, ypath_join(rows_dir, 't40'), [], get_meta(mk_interval(40, 50)))

    create_daily_table(yt_client, ypath_join(daily_rows_dir, 't00'), [], get_meta(mk_interval(0, 10)))
    create_daily_table(yt_client, ypath_join(daily_rows_dir, 't10'), [], get_meta(mk_interval(10, 20)))
    create_daily_table(yt_client, ypath_join(daily_rows_dir, 't20'), [], get_meta(mk_interval(20, 30)))
    create_daily_table(yt_client, ypath_join(daily_rows_dir, 't30'), [], get_meta(mk_interval(30, 40)))
    create_daily_table(yt_client, ypath_join(daily_rows_dir, 't40'), [], get_meta(mk_interval(40, 50)))

    create_requests_table(yt_client, ypath_join(act_requests_dir, '1990-01-01T00:15:00'), [], mk_interval(1, 2))

    with patch_run_query(yql_client, '7') as mock_obj, patch_set_meta():
        with yt_client.Transaction() as transaction:
            run_job(
                yt_client,
                transaction,
                get_meta(
                    main_interval,
                    NEXT_RUN_ID,
                    TODAY,
                    666,
                    act_interval.end,
                    mk_interval(1, 2)
                )
            )

    hamcrest.assert_that(
        mock_obj.call_args[0][2],
        hamcrest.has_entries({
            '$rows_dir': rows_dir,
            '$rows_first_table': rows_first_table,
            '$rows_last_table': rows_last_table,
            '$daily_rows_dir': daily_rows_dir,
            '$daily_rows_first_table': daily_rows_first_table,
            '$daily_rows_last_table': daily_rows_last_table,
        })
    )


def test_daily_rows_interval_split(yt_client, yql_client, run_job, rows_dir,
                                   daily_rows_dir, unprocessed_dir, act_requests_dir):
    create_interim_rows_table(yt_client, ypath_join(unprocessed_dir, 't00'), [], get_meta(mk_interval(0, 20)))

    create_interim_rows_table(yt_client, ypath_join(rows_dir, 't00'), [], get_meta(mk_interval(20, 50)))

    create_daily_table(yt_client, ypath_join(daily_rows_dir, 't00'), [], get_meta(mk_interval(0, 10)))
    create_daily_table(yt_client, ypath_join(daily_rows_dir, 't10'), [], get_meta(mk_interval(10, 30)))
    create_daily_table(yt_client, ypath_join(daily_rows_dir, 't20'), [], get_meta(mk_interval(30, 40)))

    create_requests_table(yt_client, ypath_join(act_requests_dir, '1990-01-01T00:15:00'), [], mk_interval(1, 2))

    with patch_run_query(yql_client, '7') as mock_obj, patch_set_meta():
        with yt_client.Transaction() as transaction:
            run_job(
                yt_client,
                transaction,
                get_meta(
                    mk_interval(40, 50),
                    't30',
                    TODAY,
                    666,
                    mk_interval(0, 20).end,
                    mk_interval(1, 2)
                )
            )

    hamcrest.assert_that(
        mock_obj.call_args[0][2],
        hamcrest.has_entries({
            '$rows_dir': rows_dir,
            '$rows_first_table': 't00',
            '$rows_last_table': 't00',
            '$daily_rows_dir': daily_rows_dir,
            '$daily_rows_first_table': 't10',
            '$daily_rows_last_table': 't20',
        })
    )


def test_unprocessed_rows_check(yt_client, yql_client, run_job, rows_dir,
                                unprocessed_dir, act_requests_dir):
    create_interim_rows_table(yt_client, ypath_join(unprocessed_dir, 't00'), [], get_meta(mk_interval(0, 9)))
    create_interim_rows_table(yt_client, ypath_join(rows_dir, 't00'), [], get_meta(mk_interval(10, 20)))
    create_requests_table(yt_client, ypath_join(act_requests_dir, '1990-01-01T00:15:00'), [], mk_interval(1, 2))

    with pytest.raises(AssertionError) as exc_info:
        with patch_run_query(yql_client, '7') as mock_obj, patch_set_meta():
            with yt_client.Transaction() as transaction:
                run_job(
                    yt_client,
                    transaction,
                    get_meta(
                        mk_interval(10, 20),
                        NEXT_RUN_ID,
                        TODAY,
                        666,
                        mk_interval(0, 10).end,
                        mk_interval(1, 2)
                    )
                )

    assert not mock_obj.called
    assert 'interval mismatch for {}'.format(unprocessed_dir) in exc_info.value.args[0]


def test_already_processed(
    yt_client, run_job, rows_dir, daily_rows_dir, unprocessed_dir, act_requests_dir,
    res_rows_dir, res_headers_dir
):
    create_interim_rows_table(yt_client, ypath_join(unprocessed_dir, 't00'), [], get_meta(mk_interval(0, 10)))
    create_interim_rows_table(yt_client, ypath_join(rows_dir, 't00'), [], get_meta(mk_interval(10, 20)))
    create_interim_rows_table(yt_client, ypath_join(rows_dir, 't10'), [], get_meta(mk_interval(20, 30)))
    create_daily_table(yt_client, ypath_join(daily_rows_dir, 't10'), [], get_meta(mk_interval(10, 20)))
    create_requests_table(yt_client, ypath_join(act_requests_dir, '1990-01-01T00:15:00'), [], mk_interval(1, 2))

    meta = get_meta(
        mk_interval(20, 30),
        't20',
        TODAY,
        666,
        mk_interval(0, 10).end,
        mk_interval(1, 2)
    )

    create_interim_rows_table(yt_client, ypath_join(res_rows_dir, 't20'), [], meta)
    create_headers_table(
        yt_client,
        ypath_join(res_headers_dir, 't20'),
        [
            ('YB-10',),
            ('YB-20',),
            ('YB-42',),
        ],
        meta
    )

    with yt_client.Transaction() as transaction:
        res_meta, res_rows_path, res_headers_path = run_job(yt_client, transaction, meta)

    assert res_meta == {**meta, ACT_SEQUENCE_POS_KEY: 42}
    assert res_rows_path == ypath_join(res_rows_dir, 't20')
    assert res_headers_path == ypath_join(res_headers_dir, 't20')


def test_already_processed_partial(yt_client, yql_client, run_job, rows_dir,
                                   unprocessed_dir, act_requests_dir, res_rows_dir):
    create_interim_rows_table(yt_client, ypath_join(unprocessed_dir, 't00'), [], get_meta(mk_interval(0, 10)))
    create_interim_rows_table(yt_client, ypath_join(rows_dir, 't00'), [], get_meta(mk_interval(10, 20)))
    create_requests_table(yt_client, ypath_join(act_requests_dir, '1990-01-01T00:15:00'), [], mk_interval(1, 2))

    meta = get_meta(
        mk_interval(10, 20),
        't10',
        TODAY,
        666,
        mk_interval(0, 10).end,
        mk_interval(1, 2)
    )

    create_interim_rows_table(yt_client, ypath_join(res_rows_dir, 't10'), [], meta)

    with pytest.raises(AssertionError) as exc_info:
        with patch_run_query(yql_client, '1') as mock_obj:
            with yt_client.Transaction() as transaction:
                run_job(yt_client, transaction, meta)

    assert not mock_obj.called
    assert 'Partially formed result!' in exc_info.value.args[0]


def test_already_processed_wrong_meta(
    yt_client, yql_client, run_job, rows_dir, unprocessed_dir, act_requests_dir,
    res_rows_dir, res_headers_dir
):
    create_interim_rows_table(yt_client, ypath_join(unprocessed_dir, 't00'), [], get_meta(mk_interval(0, 10)))
    create_interim_rows_table(yt_client, ypath_join(rows_dir, 't00'), [], get_meta(mk_interval(10, 20)))
    create_requests_table(yt_client, ypath_join(act_requests_dir, '1990-01-01T00:15:00'), [], mk_interval(1, 2))

    meta = get_meta(
        mk_interval(10, 20),
        't10',
        TODAY,
        666,
        mk_interval(0, 10).end,
        mk_interval(1, 2)
    )

    create_interim_rows_table(
        yt_client,
        ypath_join(res_rows_dir, 't10'),
        [],
        get_meta(
            mk_interval(10, 20),
            't10',
            TODAY,
            666,
            mk_interval(0, 10).end,
            mk_interval(1, 3)
        )
    )
    create_headers_table(yt_client, ypath_join(res_headers_dir, 't10'), [], meta)

    with pytest.raises(AssertionError) as exc_info:
        with patch_run_query(yql_client, '22') as mock_obj:
            with yt_client.Transaction() as transaction:
                run_job(yt_client, transaction, meta)

    assert not mock_obj.called
    assert 'Bad meta in current table for {}'.format(res_rows_dir) in exc_info.value.args[0]


def test_no_rows(yt_client, run_job, rows_dir, daily_rows_dir, unprocessed_dir, act_requests_dir):
    create_interim_rows_table(yt_client, ypath_join(unprocessed_dir, 't00'), [], get_meta(mk_interval(0, 10)))
    create_interim_rows_table(yt_client, ypath_join(rows_dir, 't00'), [], get_meta(mk_interval(10, 20)))
    create_interim_rows_table(yt_client, ypath_join(rows_dir, 't10'), [], get_meta(mk_interval(20, 30)))
    create_daily_table(yt_client, ypath_join(daily_rows_dir, 't10'), [], get_meta(mk_interval(10, 20)))
    create_requests_table(yt_client, ypath_join(act_requests_dir, '1990-01-01T00:15:00'), [], mk_interval(1, 2))

    with yt_client.Transaction() as transaction:
        res = run_job(
            yt_client,
            transaction,
            get_meta(
                mk_interval(20, 30),
                't20',
                TODAY,
                666,
                mk_interval(0, 10).end,
                mk_interval(1, 2)
            )
        )

    return get_result(yt_client, res)
