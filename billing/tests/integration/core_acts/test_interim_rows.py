# -*- coding: utf-8 -*-

from enum import Enum
import os
import re
import math
import collections
import contextlib
import itertools
from unittest import mock

import pytest
import hamcrest

from yt.wrapper import (
    ypath_join,
)

from billing.log_tariffication.py.lib.constants import (
    LOG_TARIFF_META_ATTR,
    LOG_INTERVAL_KEY,
    CORRECTIONS_LOG_INTERVAL_KEY,
    DYN_TABLE_IS_UPDATING_KEY,
    RUN_ID_KEY,
    ACT_DT_KEY,
    ACTED_SLICE_KEY,
    REF_INTERVAL_KEY_FORMATTER,
)
from billing.log_tariffication.py.lib import utils
from billing.log_tariffication.py.jobs.core_acts.interim_rows.processor import InterimProcessor
from billing.library.python.logfeller_utils.log_interval import (
    LogInterval,
    Subinterval,
)
from billing.library.python.yql_utils import query_metrics
from billing.library.python.yt_utils.test_utils.utils import (
    create_subdirectory,
)
from billing.log_tariffication.py.tests.utils import (
    SENTINEL,
    dict2matcher,
)
from billing.log_tariffication.py.tests.constants import (
    PREV_RUN_ID,
    CURR_RUN_ID,
    NEXT_RUN_ID,
    PREV_LOG_INTERVAL,
    CURR_LOG_INTERVAL,
    NEXT_LOG_INTERVAL,
)
from billing.log_tariffication.py.tests.integration.core_acts.common import (
    TODAY,
    PAST,
    VERY_PAST,
    FUTURE,
    VERY_FUTURE,
    create_daily_table
)


class ConsumeType(Enum):
    NONE = 'paid'
    CERTIFICATE = 'certificate'
    COMPENSATION = 'compensation'
    ROLLBACK_COMPENSATION = 'rollback_compensation'

TAX_POLICY_1 = 1
TAX_POLICY_2 = 2

TAX_POLICY_PCT_1_1 = 1
TAX_POLICY_PCT_1_2 = 2
TAX_POLICY_PCT_1_3 = 4
TAX_POLICY_PCT_2_1 = 3

CasesContainer = collections.namedtuple(
    'CasesContainer',
    [
        'consumes',
        'orders',
        'events',
        'docs_params',
        'unprocessed',
        'rows',
        'corrections',
        'daily_rows',
        'products',
    ]
)


@pytest.fixture(name='consumes_table_path')
def consumes_table_path_fixture(yt_root):
    return ypath_join(yt_root, 'consumes')


@pytest.fixture(name='docs_params_dir')
def docs_params_dir_fixture(yt_client, yt_root):
    return create_subdirectory(yt_client, yt_root, 'client_docs_params')


@pytest.fixture(name='docs_params_table_path')
def docs_params_table_path_fixture(docs_params_dir):
    return ypath_join(docs_params_dir, 'client_docs_params')


@pytest.fixture(name='taxes_dir')
def taxes_dir_fixture(yt_client, yt_root):
    return create_subdirectory(yt_client, yt_root, 'taxes')


@pytest.fixture(name='taxes_table_path')
def taxes_table_path_fixture(taxes_dir):
    return ypath_join(taxes_dir, 'taxes')


@pytest.fixture(name='product_dir')
def product_dir_fixture(yt_client, yt_root):
    return create_subdirectory(yt_client, yt_root, 'product')


@pytest.fixture(name='product_table_path')
def product_table_path_fixture(product_dir):
    return ypath_join(product_dir, 'product')


@pytest.fixture(name='log_table_path')
def log_table_path_fixture(yt_root):
    return ypath_join(yt_root, 'log')


@pytest.fixture(name='unprocessed_dir')
def unprocessed_dir_fixture(yt_client, yt_root):
    return create_subdirectory(yt_client, yt_root, 'unprocessed')


@pytest.fixture(name='rows_dir')
def rows_dir_fixture(yt_client, yt_root):
    return create_subdirectory(yt_client, yt_root, 'rows')


@pytest.fixture(name='unprocessed_acts_rows_dir')
def unprocessed_acts_rows_dir_fixture(yt_client, yt_root):
    return create_subdirectory(yt_client, yt_root, 'unprocessed_acts_rows_dir')


@pytest.fixture(name='corrections_dir')
def corrections_dir_fixture(yt_client, yt_root):
    return create_subdirectory(yt_client, yt_root, 'corrections')


@pytest.fixture(name='rows_table_path')
def rows_table_path_fixture(rows_dir):
    return ypath_join(rows_dir, 'rows')


@pytest.fixture(name='res_unprocessed_dir')
def res_unprocessed_dir_fixture(yt_client, yt_root):
    return create_subdirectory(yt_client, yt_root, 'res_unprocessed')


@pytest.fixture(name='res_events_dir')
def res_events_dir_fixture(yt_client, yt_root):
    return create_subdirectory(yt_client, yt_root, 'res_events')


@pytest.fixture(name='res_rows_dir')
def res_rows_dir_fixture(yt_client, yt_root):
    return create_subdirectory(yt_client, yt_root, 'res_rows')


@pytest.fixture(name='daily_rows_dir')
def daily_rows_dir_fixture(yt_client, yt_root):
    return create_subdirectory(yt_client, yt_root, 'daily_rows')


@pytest.fixture(name='run_job')
def run_job_fixture(
    yql_client,
    consumes_table_path,
    taxes_dir,
    docs_params_dir,
    product_dir,
    log_table_path,
    unprocessed_dir,
    rows_dir,
    unprocessed_acts_rows_dir,
    corrections_dir,
    daily_rows_dir,
    res_unprocessed_dir,
    res_events_dir,
    res_rows_dir,
):

    def _wrapped(yt_client, transaction, *args, **kwargs):

        return InterimProcessor(
            yt_client,
            yql_client,
            transaction.transaction_id,
            consumes_table_path,
            taxes_dir,
            docs_params_dir,
            product_dir,
            log_table_path,
            unprocessed_dir,
            rows_dir,
            unprocessed_acts_rows_dir,
            corrections_dir,
            daily_rows_dir,
            res_unprocessed_dir,
            res_events_dir,
            res_rows_dir,
            *args,
            **kwargs
        ).do()

    return _wrapped


@contextlib.contextmanager
def patch_run_query():
    run_query_path = 'billing.log_tariffication.py.jobs.core_acts.interim_rows.processor.utils.yql.run_query'
    extract_metrics_path = 'billing.log_tariffication.py.jobs.core_acts.interim_rows.processor.query_metrics.extract_metrics'
    write_metrics_path = 'billing.log_tariffication.py.jobs.core_acts.interim_rows.processor.query_metrics.write_metrics'
    with mock.patch(run_query_path) as mock_obj, \
            mock.patch(extract_metrics_path), \
            mock.patch(write_metrics_path):
        yield mock_obj


@contextlib.contextmanager
def patch_run_sort():
    with mock.patch('billing.log_tariffication.py.jobs.core_acts.interim_rows.processor.YtClient.run_sort') as mock_obj:
        yield mock_obj


@contextlib.contextmanager
def patch_run_reduce():
    with mock.patch('billing.log_tariffication.py.jobs.core_acts.interim_rows.processor.YtClient.run_reduce') as mock_obj:
        yield mock_obj


@contextlib.contextmanager
def patch_set_meta():
    path = 'billing.log_tariffication.py.jobs.core_acts.interim_rows.processor.utils.meta.set_log_tariff_meta'
    with mock.patch(path) as mock_obj:
        yield mock_obj


@pytest.fixture(autouse=True)
def taxes_table_fixture(yt_client, taxes_table_path):
    create_taxes_table(
        yt_client,
        taxes_table_path,
        [
            # (idx, tpid, dt, nds_pct, hidden)
            (TAX_POLICY_PCT_1_1, TAX_POLICY_1, VERY_PAST.to('utc'), 18, 0),
            (TAX_POLICY_PCT_1_2, TAX_POLICY_1, PAST.to('utc'), 20, 0),
            (TAX_POLICY_PCT_1_3, TAX_POLICY_1, TODAY.to('utc'), 21, 1),  # hidden, must be ignored
            (TAX_POLICY_PCT_2_1, TAX_POLICY_2, VERY_PAST.to('utc'), 7, 0),
        ]
    )

    return taxes_table_path


def create_taxes_table(yt_client, path, data):
    yt_client.create(
        'table',
        path,
        attributes={
            'schema': [
                {'name': 'id', 'type': 'int64'},
                {'name': 'tax_policy_id', 'type': 'int64'},
                {'name': 'dt', 'type': 'string'},
                {'name': 'nds_pct', 'type': 'double'},
                {'name': 'nsp_pct', 'type': 'double'},
                {'name': 'hidden', 'type': 'int64'},
            ],
        }
    )

    yt_client.write_table(
        path,
        [
            {
                'id': idx,
                'tax_policy_id': tpid,
                'dt': dt.strftime('%Y-%m-%d'),
                'nds_pct': float(nds_pct),
                'nsp_pct': 0.0,
                'hidden': hidden
            }
            for idx, tpid, dt, nds_pct, hidden in data
        ]
    )


def create_consumes_table(yt_client, path, data, log_interval):
    yt_client.create(
        'table',
        path,
        attributes={
            LOG_TARIFF_META_ATTR: {
                LOG_INTERVAL_KEY: log_interval.to_meta(),
                DYN_TABLE_IS_UPDATING_KEY: False,
            },
            'schema': [
                {'name': 'ID', 'type': 'uint64'},
                {'name': 'invoice_id', 'type': 'uint64'},
                {'name': 'tax_policy_pct_id', 'type': 'uint64'},
                {'name': 'order_client_id', 'type': 'uint64'},
                {'name': 'invoice_client_id', 'type': 'uint64'},

                {'name': 'consume_qty', 'type': 'double'},
                {'name': 'cashback_initial_qty', 'type': 'double'},
                {'name': 'promocode_initial_qty', 'type': 'double'},
                {'name': 'discount_initial_qty', 'type': 'double'},
                {'name': 'paid_initial_qty', 'type': 'double'},
                {'name': 'certificate_initial_qty', 'type': 'double'},
                {'name': 'compensation_initial_qty', 'type': 'double'},
                {'name': 'rollback_compensation_initial_qty', 'type': 'double'},
            ],
        }
    )

    yt_client.write_table(
        path,
        [
            {
                'ID': idx,
                'invoice_id': iid,
                'tax_policy_pct_id': tppid,
                'order_client_id': ocid,
                'invoice_client_id': icid,

                'consume_qty': float(iqty + cbqty + pcqty + dqty),
                'cashback_initial_qty': float(cbqty),
                'promocode_initial_qty': float(pcqty),
                'discount_initial_qty': float(dqty),
                'paid_initial_qty': float(iqty if ctype == ConsumeType.NONE.value else 0),
                'certificate_initial_qty': float(iqty if ctype == ConsumeType.CERTIFICATE.value else 0),
                'compensation_initial_qty': float(iqty if ctype == ConsumeType.COMPENSATION.value else 0),
                'rollback_compensation_initial_qty': float(iqty if ctype == ConsumeType.ROLLBACK_COMPENSATION.value else 0),
            }
            for idx, iid, tppid, ocid, icid, iqty, ctype, cbqty, pcqty, dqty in data
        ]
    )

    yt_client.run_sort(path, sort_by=['ID'])


def create_client_docs_params(yt_client, path, data):
    yt_client.create(
        'table',
        path,
        attributes={
            'schema': [
                {'name': 'client_id', 'type': 'uint64'},
                {'name': 'is_docs_detailed', 'type': 'uint64'},
                {'name': 'is_docs_separated', 'type': 'uint64'},
                {'name': 'agency_params', 'type': 'string'}
            ],
        }
    )

    yt_client.write_table(
        path,
        [
            {
                'client_id': cid,
                'is_docs_detailed': idd,
                'is_docs_separated': ids,
                'agency_params': ap
            }
            for cid, idd, ids, ap in data
        ]
    )


def create_log_table(yt_client, path, data):
    yt_client.create(
        'table',
        path,
        attributes={
            'schema': [
                {'name': 'UID', 'type': 'string'},
                {'name': 'ServiceID', 'type': 'int64'},
                {'name': 'EffectiveServiceOrderID', 'type': 'int64'},
                {'name': 'ServiceOrderID', 'type': 'int64'},
                {'name': 'consume_id', 'type': 'uint64'},
                {'name': 'tariffed_qty', 'type': 'double'},
                {'name': 'tariffed_sum', 'type': 'double'},
                {'name': 'tariff_dt', 'type': 'uint64'},
                {'name': 'coeff_qty', 'type': 'double'},
                {'name': 'coeff_sum', 'type': 'double'},
                {'name': 'ProductID', 'type': 'int64'}
            ]
        }
    )

    yt_client.write_table(
        path,
        [
            {
                'UID': str(idx),
                'ServiceID': sid,
                'EffectiveServiceOrderID': essid,
                'ServiceOrderID': ssid,
                'consume_id': cid,
                'tariffed_qty': float(t_qty),
                'tariffed_sum': float(t_sum) if t_sum is not None else None,
                'tariff_dt': dt.int_timestamp,
                'coeff_qty': float(c_qty) if c_qty is not None else None,
                'coeff_sum': float(c_sum) if c_sum is not None else None,
                'ProductID': int(product_id)
            }
            for idx, sid, essid, ssid, cid, t_qty, t_sum, dt, c_qty, c_sum, product_id, in data
        ]
    )


def create_product_table(yt_client, path, data):
    yt_client.create(
        'table',
        path,
        attributes={
            'schema': [
                {'name': 'id', 'type': 'int64'},
                {'name': 'commission_type', 'type': 'int64'},
                {'name': 'media_discount', 'type': 'int64'},
            ],
        }
    )

    yt_client.write_table(
        path,
        [
            {
                'id': pid,
                'commission_type': pct,
                'media_discount': pmd,
            }
            for pid, pct, pmd, in data
        ]
    )


def set_log_meta(
    yt_client,
    path,
    log_interval,
    run_id=None,
    act_dt=None,
    taxes_path=None,
    docs_params_path=None,
    product_params_path=None,
    consumes_slice=None,
    acted_slice=None,
    corrections=None,
):
    meta = {
        LOG_INTERVAL_KEY: log_interval.to_meta()
    }
    if run_id is not None:
        meta[RUN_ID_KEY] = run_id
    if act_dt is not None:
        meta[ACT_DT_KEY] = act_dt.strftime('%Y-%m-%d')
    if taxes_path is not None:
        meta[REF_INTERVAL_KEY_FORMATTER('taxes')] = os.path.basename(taxes_path)
    if docs_params_path:
        meta[REF_INTERVAL_KEY_FORMATTER('docs_params')] = os.path.basename(docs_params_path)
    if product_params_path:
        meta[REF_INTERVAL_KEY_FORMATTER('product')] = os.path.basename(product_params_path)

    if consumes_slice is not None:
        meta[REF_INTERVAL_KEY_FORMATTER('consumes')] = consumes_slice.to_meta()
    if acted_slice is not None:
        meta[ACTED_SLICE_KEY] = acted_slice.to_meta()
    if corrections is not None:
        meta[CORRECTIONS_LOG_INTERVAL_KEY] = corrections.to_meta()

    yt_client.set(ypath_join(path, '@' + LOG_TARIFF_META_ATTR), meta)


def create_rows_table(yt_client, path, data):

    yt_client.create(
        'table',
        path,
        attributes={
            'schema': [
                {'name': 'UID', 'type': 'string'},
                {'name': 'invoice_id', 'type': 'uint64'},
                {'name': 'consume_id', 'type': 'int64'},
                {'name': 'tax_policy_pct_id', 'type': 'uint64'},
                {'name': 'acted_sum', 'type': 'double'},
                {'name': 'acted_qty', 'type': 'double'},
                {'name': 'service_order_id', 'type': 'uint64'},
                {
                    'name': 'custom_header_key',
                    'type_v3': {
                        "type_name": "optional",
                        "item": {
                            "type_name": "struct",
                            "members": [
                                {
                                    "name": "group_docs",
                                    "required": False,
                                    "type": "int64",
                                },
                                {
                                    "name": "commission_type",
                                    "required": False,
                                    "type": "int64",
                                },
                                {
                                    "name": "media_discount",
                                    "required": False,
                                    "type": "int64",
                                },
                            ]

                        }
                    }
                },
            ],
        }
    )

    yt_client.write_table(
        path,
        [
            {
                'UID': uid,
                'invoice_id': iid,
                'consume_id': cid,
                'tax_policy_pct_id': tppid,
                'acted_sum': float(a_sum),
                'acted_qty': float(a_qty),
                'custom_header_key': cust,
                'service_order_id': sorder_id,
            }
            for uid, iid, cid, tppid, a_sum, a_qty, cust, sorder_id in data
        ]
    )


class CaseOrder:
    _order_id_counter = [1]
    _consume_id_counter = [1]
    _event_id_counter = [1]
    _invoice_id_counter = [1]
    _invoice_ids_map = {}
    _row_id_counter = [1]
    _daily_id_counter = [1]
    _product_id_counter = [1]

    def __init__(self, uid_prefix='', service_id=7, with_order=True, with_corrections=False):
        self._uid_prefix = uid_prefix
        self._service_id = service_id
        self._service_order_id, = self._order_id_counter
        self._order_id_counter[0] += 1

        self._consume_ids_map = {}
        self._row_id_map = {}
        self._product_id_map = {}

        self.events = []
        self.unprocessed = []
        self.consumes = []
        self.docs_params = []
        self.rows = []
        self.corrections = []
        self.daily_rows = []
        self.products = []

        if with_order:
            self.orders = [(self._service_id, self._service_order_id)]
        else:
            self.orders = []

        self.with_corrections = with_corrections
        self.corrections_counter = itertools.count(1)

    def _consume_id(self, idx):
        res = self._consume_ids_map.get(idx)
        if res is None:
            res, = self._consume_id_counter
            self._consume_id_counter[0] += 1
            self._consume_ids_map[idx] = res
        return res

    def _invoice_id(self, idx):
        if idx is SENTINEL:
            res, = self._invoice_id_counter
            self._invoice_id_counter[0] += 1
        else:
            case_invoice_ids_map = self._invoice_ids_map.setdefault(self._uid_prefix, {})
            res = case_invoice_ids_map.get(idx)
            if res is None:
                res, = self._invoice_id_counter
                self._invoice_id_counter[0] += 1
                case_invoice_ids_map[idx] = res
        return res

    def _row_id(self, idx):
        res = self._row_id_map.get(idx)

        if res is None:
            res = self._row_id_counter[0]
            self._row_id_map[idx] = res

            self._row_id_counter[0] += 1

        return res

    def _product_id(self, idx) -> int:
        res = self._product_id_map.get(idx)

        if res is None:
            res = self._product_id_counter[0]
            self._product_id_map[idx] = res

            self._product_id_counter[0] += 1

        return res

    def _fmt_event_uid(self, event_id):
        return '{}_{:05}'.format(self._uid_prefix, event_id)

    def _fmt_event(self, consume_id, t_qty, dt=TODAY, t_sum=SENTINEL, c_qty=1, c_sum=1, product_id=0, service_order_id=None):
        idx = self._fmt_event_uid(self._event_id_counter[0])
        self._event_id_counter[0] += 1

        product_uid = self._product_id(product_id) if product_id > 0 else int(product_id)

        return (
            idx,
            self._service_id,
            self._service_order_id,
            service_order_id or self._service_order_id,
            self._consume_id(consume_id) if consume_id is not None else None,
            t_qty,
            round(t_qty, 2) if t_sum is SENTINEL else t_sum,
            dt,
            c_qty,
            c_sum,
            product_uid
        )

    def _fmt_row_uid(self, row_id):
        return '{}_row_{:05}'.format(self._uid_prefix, row_id)

    def _fmt_daily_uid(self, daily_id):
        return '{}_row_{:05}'.format(self._uid_prefix, daily_id)

    def add_event(self, *args, **kwargs):
        self.events.append(self._fmt_event(*args, **kwargs))
        return self

    def add_unprocessed(self, *args, **kwargs):
        self.unprocessed.append(self._fmt_event(*args, **kwargs))
        return self

    def add_consume(
        self,
        consume_id,
        invoice_id=SENTINEL,
        tpp_id=TAX_POLICY_PCT_1_2,
        order_client_id=1000,
        invoice_client_id=None,
        initial_qty=1,
        consume_type=ConsumeType.NONE.value,
        cashback_initial_qty=0,
        promocode_initial_qty=0,
        discount_initial_qty=0,
    ):
        self.consumes.append((
            self._consume_id(consume_id),
            self._invoice_id(invoice_id),
            tpp_id,
            order_client_id,
            invoice_client_id if invoice_client_id else order_client_id,
            initial_qty,
            consume_type,
            cashback_initial_qty,
            promocode_initial_qty,
            discount_initial_qty,
        ))
        return self

    def add_row(self, invoice_id, consume_id, a_sum, row_id=None, tpp_id=TAX_POLICY_PCT_1_2, custom=None, a_qty=None, service_order_id=None):
        row_uid = self._fmt_row_uid(self._row_id(row_id or self._row_id_counter[0]))

        self.rows.append((
            row_uid,
            self._invoice_id(invoice_id),
            self._consume_id(consume_id),
            tpp_id,
            a_sum,
            a_qty if a_qty is not None else a_sum,
            custom,
            service_order_id or self._service_order_id,
        ))
        return self

    def add_corrections(self, *args, **kwargs):
        data = self._fmt_event(*args, **kwargs)
        self.corrections.append(data)
        return self

    def add_docs_params(self, client_id, is_docs_detailed=0, is_docs_separated=0, agency_params=None):
        self.docs_params.append((client_id,
                                 is_docs_detailed,
                                 is_docs_separated,
                                 agency_params))
        return self

    def add_daily_row(self, uid=None, row_id=None, event_id=None):
        daily_uid = uid or \
            (row_id and self._fmt_row_uid(self._row_id(row_id))) or \
            (event_id and self._fmt_event_uid(event_id))

        if daily_uid is None:
            daily_uid = self._fmt_daily_uid(self._daily_id_counter[0])
            self._daily_id_counter[0] += 1

        self.daily_rows.append((daily_uid,))
        return self

    def add_product(self, id=0, commission_type=7, media_discount=7):
        uid = self._product_id(id)
        self.products.append((uid, commission_type, media_discount))

        return self


def get_result(yt_client, res_paths, w_meta=True):
    uid_pattern = re.compile(r'(.*)_\d+')
    unprocessed, events, rows = res_paths

    cases = {}
    row2case = {}
    for row in sorted(yt_client.read_table(unprocessed), key=lambda r: r['UID']):
        case_name = uid_pattern.match(row['UID']).group(1)
        cases.setdefault(case_name, {}).setdefault('unprocessed', []).append(row)

    for row in sorted(yt_client.read_table(events), key=lambda r: r['UID']):
        case_name = uid_pattern.match(row['UID']).group(1)
        cases.setdefault(case_name, {}).setdefault('events', []).append(row)
        row2case[row['row_UID']] = case_name

    for row in sorted(yt_client.read_table(rows), key=lambda r: r['UID']):
        case_name = row2case[row['UID']]
        cases.setdefault(case_name, {}).setdefault('rows', []).append(row)

    meta = {}
    for tbl_name, tbl_path in zip(['unprocessed', 'events', 'rows'], [unprocessed, events, rows]):
        meta_attr = yt_client.get(ypath_join(tbl_path, '@' + LOG_TARIFF_META_ATTR))
        run_id = meta_attr.pop('run_id')
        assert run_id
        meta[tbl_name] = meta_attr

    res = {'cases': cases}
    if w_meta:
        res['meta'] = meta

    return res


def unpack_cases(cases):
    res = CasesContainer([], [], [], [], [], [], [], [], [])

    for case in cases:
        res.consumes.extend(case.consumes)
        res.orders.extend(case.orders)
        res.events.extend(case.events)
        res.docs_params.extend(case.docs_params)
        res.unprocessed.extend(case.unprocessed)
        res.rows.extend(case.rows)
        res.corrections.extend(case.corrections)
        res.daily_rows.extend(case.daily_rows)
        res.products.extend(case.products)

    return res


def _gen_rounding_case():
    case = CaseOrder('rounding').add_consume(1)
    qty = 0.001
    for idx in range(1, 101):
        case.add_event(1, qty, t_sum=round(round(qty * idx, 2) - round(qty * (idx - 1), 2), 2))
    return [case]


def _get_rounding_uncompleted_case():
    case = (
        CaseOrder('rounding_unprocessed')
        .add_consume(1)
        .add_event(1, 0.05)
    )
    qty = -0.001
    for idx in range(1, 101):
        case.add_event(1, qty, t_sum=round(round(qty * idx, 2) - round(qty * (idx - 1), 2), 2))
    return [case]


BASE_CASES = [
    CaseOrder('part_rollback_unprocessed')
    .add_consume(1)
    .add_event(1, 1.666666)
    .add_event(1, 1.333334)
    .add_unprocessed(1, -1.012345),
    CaseOrder('part_rollback_completed')
    .add_consume(1)
    .add_event(1, 66)
    .add_event(1, -42.424242),
    CaseOrder('unprocessed_positive')
    .add_consume(1)
    .add_event(1, 14)
    .add_event(1, -15)
    .add_unprocessed(1, 2),
    CaseOrder('rollback_to_unprocessed_equal')
    .add_consume(1)
    .add_event(1, 42.111111)
    .add_event(1, 23.888889)
    .add_event(1, -66)
    .add_event(1, -166.000001),
    CaseOrder('rollback_to_unprocessed_split')
    .add_consume(1)
    .add_event(1, 10)
    .add_event(1, -20)
    .add_event(1, -4),
    CaseOrder('rollback_to_unprocessed_split_price')
    .add_consume(1)
    .add_event(1, 3.5, t_sum=1.5, c_qty=7, c_sum=3)
    .add_event(1, 1.1666667, t_sum=0.5, c_qty=7, c_sum=3)
    .add_event(1, -7, t_sum=-3, c_qty=7, c_sum=3),
    CaseOrder('rollback_to_unprocessed_zero_event')
    .add_consume(1)
    .add_event(1, 2)
    .add_event(1, -0.001)
    .add_event(1, -0.001)
    .add_event(1, -0.001)
    .add_event(1, -3),
    CaseOrder('only_rollbacks')
    .add_consume(1)
    .add_event(1, -1)
    .add_unprocessed(1, -1),
    CaseOrder('zero_sum')
    .add_consume(1)
    .add_event(1, 0.000666, c_qty=1, c_sum=0)
    .add_unprocessed(1, 0.000666, c_qty=1, c_sum=0)
    .add_unprocessed(1, 1, t_sum=0.00042, c_qty=1, c_sum=0),
    CaseOrder('null_tariff_info')
    .add_consume(1)
    .add_event(1, 1)
    .add_event(None, 666, t_sum=None)
    .add_unprocessed(None, 667, t_sum=None),
    CaseOrder('dt_filter')
    .add_consume(1)
    .add_event(1, 1, VERY_PAST)
    .add_event(1, 1, PAST)
    .add_unprocessed(1, 1, TODAY)
    .add_unprocessed(1, 1, FUTURE)
    .add_event(1, 1, VERY_FUTURE),
    CaseOrder('rollback_tax_dt_adjust')
    .add_consume(1, 1, TAX_POLICY_PCT_1_1)
    .add_event(1, -1)
    .add_event(1, 2)
    .add_consume(2, 1, TAX_POLICY_PCT_1_2)
    .add_event(2, 2),
]

GROUP_PART_ROLLBACK_CASES = [
    CaseOrder('group_part_rollback')
    .add_consume(1, 1, TAX_POLICY_PCT_2_1)
    .add_event(1, 1)
    .add_event(1, -0.5)
    .add_consume(2, 1)
    .add_event(2, 2)
    .add_event(2, -1)
    .add_consume(3, 2)
    .add_event(3, 3)
    .add_unprocessed(3, -1.5),
    CaseOrder('group_part_rollback')
    .add_consume(1, 1, TAX_POLICY_PCT_2_1)
    .add_event(1, 1)
    .add_consume(2, 1, TAX_POLICY_PCT_2_1)
    .add_unprocessed(2, -1.1)
    .add_consume(3, 2)
    .add_event(3, 3)
    .add_consume(4, 2)
    .add_unprocessed(4, -3.3)
]

GROUP_ROLLBACK_TO_UNCOMPLETED_CASES = [
    CaseOrder('group_rollback_to_unprocessed')
    .add_consume(1, 1)
    .add_event(1, 10)
    .add_consume(2, 1)
    .add_event(2, -7),
    CaseOrder('group_rollback_to_unprocessed')
    .add_consume(1, 1)
    .add_unprocessed(1, -8)
]

ROUNDING_CASE = _gen_rounding_case()
ROUNDING_UNCOMPLETED_CASE = _get_rounding_uncompleted_case()

BASE_CASES_2 = [
    CaseOrder('no_consume')
    .add_consume(1, 1)
    .add_event(1, 1)
    .add_unprocessed(1, 2)
    .add_event(2, -0.2)
    .add_event(2, 3),
    CaseOrder('rollback_to_unprocessed_zero_total_qty')
    .add_consume(1)
    .add_event(1, 2.22)
    .add_event(1, 4.44)
    .add_event(1, -3.33)
    .add_event(1, -3.33),
    CaseOrder('rollback_to_unprocessed_zero_total_sum')
    .add_consume(1)
    .add_event(1, 2)
    .add_event(1, -0.3)
    .add_event(1, -1.701),
    CaseOrder('rollback_to_unprocessed_proportion')
    .add_consume(1)
    .add_event(1, 1000.001)
    .add_event(1, -1000.01),
    CaseOrder('split_rows_by_service_order_id')
    .add_consume(1)
    .add_event(1, 1.666666, service_order_id=3)
    .add_event(1, 1.333334, service_order_id=45)
    .add_unprocessed(1, -1.012345),
]

CORRECTIONS_FILTERING_CASES = [
    CaseOrder('corrections_prev')
    .add_corrections(1, 1),
    CaseOrder('corrections_curr')
    .add_corrections(2, 10),
    CaseOrder('corrections_next')
    .add_corrections(3, 100),
]

CORRECTION_CASES = [
    CaseOrder('correction_group')
    .add_consume(1)
    .add_consume(2)
    .add_event(1, 1, service_order_id=1)
    .add_event(1, 2, service_order_id=2)
    .add_corrections(1, 1, service_order_id=1)
    .add_corrections(2, 1, service_order_id=2),
]

GROUP_DOCS_CASES = [
    # group_docs 1
    CaseOrder('client_wo_docs_params')
    .add_consume(1, order_client_id=6665)
    .add_event(1, 1),
    # 6666
    CaseOrder('client_w_dd_w_ds')
    .add_consume(2, order_client_id=6666)
    .add_docs_params(client_id=6666, is_docs_detailed=1, is_docs_separated=1)
    .add_event(2, 1),
    # 1
    CaseOrder('client_w_dd_wo_ds')
    .add_consume(3, order_client_id=6667)
    .add_docs_params(client_id=6667, is_docs_detailed=1, is_docs_separated=0)
    .add_event(3, 1),
    # 6668
    CaseOrder('client_wo_dd_w_ds')
    .add_consume(4, order_client_id=6668)
    .add_docs_params(client_id=6668, is_docs_detailed=0, is_docs_separated=1)
    .add_event(4, 1),
    # 0
    CaseOrder('client_wo_dd_wo_ds')
    .add_consume(5, order_client_id=6669)
    .add_docs_params(client_id=6669, is_docs_detailed=0, is_docs_separated=0)
    .add_event(5, 1),
    # 0
    CaseOrder('agency_empty_params')
    .add_consume(6, order_client_id=6670, invoice_client_id=6671)
    .add_docs_params(client_id=6670, agency_params="{\"6672\": [true, true]}")
    .add_event(6, 1),
    # 6673
    CaseOrder('agency_w_dd_w_ds')
    .add_consume(7, order_client_id=6673, invoice_client_id=6674)
    .add_docs_params(client_id=6673, agency_params="{\"6674\": [true, true]}")
    .add_docs_params(client_id=6674, is_docs_detailed=0, is_docs_separated=0)
    .add_event(7, 1),
    # 6675
    CaseOrder('agency_wo_dd_w_ds')
    .add_consume(8, order_client_id=6675, invoice_client_id=6676)
    .add_docs_params(client_id=6675, agency_params="{\"6676\": [false, true]}")
    .add_docs_params(client_id=6676, is_docs_detailed=1, is_docs_separated=0)
    .add_event(8, 1),
    # 1
    CaseOrder('agency_w_dd_wo_ds')
    .add_consume(9, order_client_id=6677, invoice_client_id=6678)
    .add_docs_params(client_id=6677, agency_params="{\"6678\": [true, false]}")
    .add_docs_params(client_id=6678, is_docs_detailed=0, is_docs_separated=1)
    .add_event(9, 1)
]

ZERO_MAX_SUM_CASES = [
    CaseOrder('zero_max_sum_neg')
    .add_consume(1)
    .add_event(1, t_qty=-0.001, t_sum=0)
    .add_event(1, t_qty=0.002, t_sum=0)
    .add_event(1, t_qty=0.003, t_sum=0),
    CaseOrder('zero_max_sum_pos')
    .add_consume(1)
    .add_event(1, t_qty=0.001, t_sum=0)
    .add_event(1, t_qty=0.002, t_sum=0)
    .add_event(1, t_qty=0.003, t_sum=0),
    CaseOrder('zero_max_sum_prev_neg')
    .add_consume(1, 1)
    .add_row(1, 1, a_qty=0.002, a_sum=0)
    .add_row(1, 1, a_qty=-0.001, a_sum=0)
    .add_event(1, t_qty=0.001, t_sum=0),
    CaseOrder('zero_max_sum_prev_pos')
    .add_consume(1, 1)
    .add_row(1, 1, a_qty=0.002, a_sum=0)
    .add_row(1, 1, a_qty=0.001, a_sum=0)
    .add_event(1, t_qty=0.001, t_sum=0),
    CaseOrder('zero_max_sum_prev_nonzero')
    .add_consume(1, 1)
    .add_row(1, 1, a_qty=0.01, a_sum=0.01)
    .add_event(1, t_qty=-0.001, t_sum=0)
    .add_event(1, t_qty=0.002, t_sum=0)
]

ZERO_QTY_NONZERO_SUM_CASES = [
    CaseOrder('zero_qty_nonzero_sum')
    .add_consume(1)
    .add_event(1, t_qty=10, t_sum=10.01)
    .add_event(1, t_qty=-10, t_sum=-10)
    .add_consume(2)
    .add_event(2, 666),
]

TAXES_SPLIT_CASES = [
    CaseOrder('taxes_split_curr')
    .add_consume(1, 1, tpp_id=TAX_POLICY_PCT_1_2)
    .add_event(1, 10)
    .add_consume(2, 1, tpp_id=TAX_POLICY_PCT_1_1)
    .add_event(2, -5)
    .add_event(2, 1)
    .add_event(2, -2)
    .add_consume(3, 1, tpp_id=TAX_POLICY_PCT_1_1)
    .add_event(3, -7),
    CaseOrder('taxes_split_prev')
    .add_consume(1, 1, tpp_id=TAX_POLICY_PCT_1_1)
    .add_event(1, -11)
    .add_consume(2, 1, tpp_id=TAX_POLICY_PCT_1_2)
    .add_row(1, 2, 10, tpp_id=TAX_POLICY_PCT_1_2),
    CaseOrder('taxes_split_tax_policy_group')
    .add_consume(1, 1, tpp_id=TAX_POLICY_PCT_1_1)
    .add_event(1, -1)
    .add_consume(2, 1, tpp_id=TAX_POLICY_PCT_2_1)
    .add_event(2, 666),
]

SPLIT_CASES = [
    CaseOrder('groups_split_zero_sum')
    .add_consume(1, 1, tpp_id=TAX_POLICY_PCT_1_1)
    .add_event(1, -99.998, t_sum=-100)
    .add_event(1, -0.001, t_sum=0)
    .add_event(1, -0.001, t_sum=0)
    .add_consume(2, 1, tpp_id=TAX_POLICY_PCT_1_2)
    .add_event(2, 100),
    CaseOrder('groups_split_plusminus_multiple_prev')
    .add_consume(1, 1, tpp_id=TAX_POLICY_PCT_1_2)
    .add_event(1, -100)
    .add_event(1, 98)
    .add_row(1, 1, 1, tpp_id=TAX_POLICY_PCT_1_2)
    .add_consume(2, 1, tpp_id=TAX_POLICY_PCT_1_1)
    .add_row(1, 2, 1, tpp_id=TAX_POLICY_PCT_1_1),
    CaseOrder('groups_split_neg_prev_row_regroup')
    .add_consume(1, 1, tpp_id=TAX_POLICY_PCT_1_1)
    .add_row(1, 1, -10, tpp_id=TAX_POLICY_PCT_1_1)
    .add_event(1, 2)
    .add_event(1, -1)
    .add_event(1, -4)
    .add_consume(2, 1, tpp_id=TAX_POLICY_PCT_1_2)
    .add_row(1, 2, -3, tpp_id=TAX_POLICY_PCT_1_2)
    .add_event(2, 3)
    .add_event(2, -1)
    .add_event(2, 8),
    CaseOrder('groups_split_neg_prev_row_split')
    .add_consume(1, 1, tpp_id=TAX_POLICY_PCT_1_1)
    .add_row(1, 1, -10, tpp_id=TAX_POLICY_PCT_1_1)
    .add_event(1, 4)
    .add_event(1, 9)
    .add_consume(2, 1, tpp_id=TAX_POLICY_PCT_1_2)
    .add_event(2, -5),
    CaseOrder('group_split_proportion_zero')
    .add_consume(1, 1, tpp_id=TAX_POLICY_PCT_1_1)
    .add_event(1, t_qty=0.00003, t_sum=0, c_qty=13.806, c_sum=13.81)
    .add_consume(2, 1, tpp_id=TAX_POLICY_PCT_1_2)
    .add_event(2, t_qty=-13.806, t_sum=-13.81, c_qty=100.06401, c_sum=100.06),
    CaseOrder('group_split_proportion')
    .add_consume(1, 1, tpp_id=TAX_POLICY_PCT_1_1)
    .add_event(1, t_qty=-13.806, t_sum=-13.81, c_qty=100.06401, c_sum=100.06)
    .add_consume(2, 1, tpp_id=TAX_POLICY_PCT_1_2)
    .add_row(1, 2, 0.01, TAX_POLICY_PCT_1_2),
    CaseOrder('group_split_pos_single_consumption')
    .add_consume(1, 1, tpp_id=TAX_POLICY_PCT_1_1)
    .add_consume(2, 1, tpp_id=TAX_POLICY_PCT_1_2)
    .add_row(1, 1, 0.5, tpp_id=TAX_POLICY_PCT_1_1)
    .add_event(1, -0.4)
    .add_event(2, -0.3),
]

CLIENT_SPLIT_CASES = [
    CaseOrder('split_by_client')
    .add_consume(1, 1, order_client_id=1)
    .add_event(1, 10)
    .add_event(1, -5)
    .add_consume(2, 1, order_client_id=2)
    .add_row(1, 2, 11)
    .add_event(2, -4)
    .add_consume(3, 1, order_client_id=3)
    .add_row(1, 3, 3)
    .add_event(3, 2)
    .add_event(3, -6),
    CaseOrder('split_by_client_taxes')
    .add_consume(1, 1, tpp_id=TAX_POLICY_PCT_1_1, order_client_id=1)
    .add_event(1, 10)
    .add_consume(2, 1, tpp_id=TAX_POLICY_PCT_1_1, order_client_id=2)
    .add_event(2, 666)
    .add_consume(3, 1, tpp_id=TAX_POLICY_PCT_1_2, order_client_id=1)
    .add_event(3, -14)
]

WITH_DAILY_ROWS_CASES = [
    CaseOrder('exclude_daily_from_interim_month_positive')
    .add_consume(1, 1, order_client_id=1)
    .add_row(1, 1, 100, row_id=1)
    .add_row(1, 1, 50)
    .add_daily_row(row_id=1)
    .add_event(1, -20),
    CaseOrder('exclude_daily_from_interim_month_negative')
    .add_consume(1, 1, order_client_id=1)
    .add_row(1, 1, 100, row_id=1)
    .add_row(1, 1, 20, row_id=2)
    .add_daily_row(row_id=1)
    .add_event(1, -50),
]

PRODUCT_SPLIT_CASES = [
    CaseOrder('split_by_product')
    .add_consume(1, 1)
    .add_product(id=1, commission_type=6, media_discount=5)
    .add_product(id=2, commission_type=8, media_discount=9)
    .add_event(1, 1, product_id=1)
    .add_event(1, 2, product_id=1)
    .add_event(1, 5, product_id=2)
]

PEREACT_BY_ORDER = [
    CaseOrder('pereact_by_order')
    .add_consume(1, 1)
    .add_row(1, 1, 3)
    .add_event(2, -3),
    CaseOrder('pereact_by_order')
    .add_consume(2, 1)
    .add_event(2, -2),
    CaseOrder('pereact_by_order')
    .add_consume(3, 1)
    .add_event(3, 1)
    .add_event(3, -1)
]

QUANTITIES_DISPOSITION_CASES = [
    CaseOrder('disposition_paid')
    .add_consume(1, initial_qty=1)
    .add_event(1, 1.2345),
    CaseOrder('disposition_certificate_cashback')
    .add_consume(1, initial_qty=271.314, consume_type=ConsumeType.CERTIFICATE.value, cashback_initial_qty=33.33)
    .add_event(1, -1.2345)
    .add_event(1, 2.3456),
    CaseOrder('disposition_compensation_promocode')
    .add_consume(1, initial_qty=828.159, consume_type=ConsumeType.COMPENSATION.value, promocode_initial_qty=25.2525)
    .add_event(1, -1.2345)
    .add_event(1, 2.3456)
    .add_event(1, 3.4567),
    CaseOrder('disposition_rollback_compensation_discount')
    .add_consume(1, initial_qty=182.265, consume_type=ConsumeType.ROLLBACK_COMPENSATION.value, discount_initial_qty=10)
    .add_event(1, 1.2345)
    .add_event(1, -2.3456)
    .add_event(1, 3.4567)
    .add_event(1, 4.5678),
    CaseOrder('disposition_paid_cashback_promocode_discount')
    .add_consume(1, initial_qty=845.359, consume_type=ConsumeType.NONE.value,
                 cashback_initial_qty=33.33, promocode_initial_qty=25.2525, discount_initial_qty=10)
    .add_event(1, 1.2345)
    .add_event(1, 2.3456)
    .add_event(1, -3.4567)
    .add_event(1, 4.5678)
    .add_event(1, 5.6789),
]


def _gen_rounding_case_w_disposition(
    paid_initial_qty=1, cashback_initial_qty=0, promocode_initial_qty=0, discount_initial_qty=0,
):
    case = (
        CaseOrder('rounding_w_disposition')
        .add_consume(1, initial_qty=paid_initial_qty, consume_type=ConsumeType.NONE.value,
                     cashback_initial_qty=cashback_initial_qty, promocode_initial_qty=promocode_initial_qty,
                     discount_initial_qty=discount_initial_qty)
    )
    qty = 0.001
    for idx in range(1, 1984):
        case.add_event(1, qty, t_sum=round(round(qty * idx, 2) - round(qty * (idx - 1), 2), 2))
    return [case]


def _get_rounding_uncompleted_case_w_disposition(
    paid_initial_qty=1, cashback_initial_qty=0, promocode_initial_qty=0, discount_initial_qty=0,
):
    case = (
        CaseOrder('rounding_unprocessed_w_disposition')
        .add_consume(1, initial_qty=paid_initial_qty, consume_type=ConsumeType.NONE.value,
                     cashback_initial_qty=cashback_initial_qty, promocode_initial_qty=promocode_initial_qty,
                     discount_initial_qty=discount_initial_qty)
        .add_event(1, 1.5)
    )
    qty = -0.001
    for idx in range(1, 1337):
        case.add_event(1, qty, t_sum=round(round(qty * idx, 2) - round(qty * (idx - 1), 2), 2))
    return [case]


ROUNDING_CASES_DISPOSITION_PARAMS = dict(
    paid_initial_qty=845.359, cashback_initial_qty=34.56, promocode_initial_qty=45.67, discount_initial_qty=5.678
)

DISPOSITION_ROUNDING_CASES = (
    _gen_rounding_case_w_disposition(**ROUNDING_CASES_DISPOSITION_PARAMS)
    + _get_rounding_uncompleted_case_w_disposition(**ROUNDING_CASES_DISPOSITION_PARAMS)
)

CASES = (
    BASE_CASES
    + GROUP_PART_ROLLBACK_CASES
    + GROUP_ROLLBACK_TO_UNCOMPLETED_CASES
    + ROUNDING_CASE
    + ROUNDING_UNCOMPLETED_CASE
    + BASE_CASES_2
    + CORRECTIONS_FILTERING_CASES
    + CORRECTION_CASES
    + GROUP_DOCS_CASES
    + ZERO_MAX_SUM_CASES
    + ZERO_QTY_NONZERO_SUM_CASES
    + TAXES_SPLIT_CASES
    + SPLIT_CASES
    + CLIENT_SPLIT_CASES
    + WITH_DAILY_ROWS_CASES
    + PRODUCT_SPLIT_CASES
    + PEREACT_BY_ORDER
    + QUANTITIES_DISPOSITION_CASES
    + DISPOSITION_ROUNDING_CASES
)


def test_calculations(yt_client, run_job, consumes_table_path, log_table_path, unprocessed_dir,
                      rows_dir, unprocessed_acts_rows_dir, corrections_dir, taxes_table_path,
                      docs_params_table_path, daily_rows_dir, product_table_path):
    cases = unpack_cases(CASES)
    # raise Exception(f'length={len(cases.rows)}, rows={cases.rows[-2:]}')

    create_consumes_table(yt_client, consumes_table_path, cases.consumes, PREV_LOG_INTERVAL)
    create_client_docs_params(yt_client, docs_params_table_path, cases.docs_params)
    create_log_table(yt_client, log_table_path, cases.events)
    set_log_meta(
        yt_client,
        log_table_path,
        NEXT_LOG_INTERVAL,
        NEXT_RUN_ID,
        TODAY,
        taxes_table_path,
        docs_params_table_path,
        product_table_path,
        PREV_LOG_INTERVAL,
        PREV_LOG_INTERVAL.beginning,
        LogInterval([Subinterval('cor_c1', 'cor_t1', 0, 1, 4)]),
    )

    unprocessed_path = ypath_join(unprocessed_dir, CURR_RUN_ID)
    create_log_table(yt_client, unprocessed_path, cases.unprocessed)
    set_log_meta(yt_client, unprocessed_path, CURR_LOG_INTERVAL)

    rows_batch_size = math.ceil(len(cases.rows) / 3)

    create_rows_table(yt_client, ypath_join(rows_dir, PREV_RUN_ID), cases.rows[:rows_batch_size])
    set_log_meta(yt_client, ypath_join(rows_dir, PREV_RUN_ID), PREV_LOG_INTERVAL)

    create_rows_table(yt_client, ypath_join(rows_dir, CURR_RUN_ID), cases.rows[rows_batch_size:-rows_batch_size])
    set_log_meta(yt_client, ypath_join(rows_dir, CURR_RUN_ID), CURR_LOG_INTERVAL)

    if len(cases.rows) < 2:
        rows = []
    else:
        rows = cases.rows[-rows_batch_size:]
    create_rows_table(yt_client, ypath_join(unprocessed_acts_rows_dir, PREV_RUN_ID), rows)
    set_log_meta(
        yt_client,
        ypath_join(unprocessed_acts_rows_dir, PREV_RUN_ID),
        LogInterval.from_slices(PREV_LOG_INTERVAL.beginning, PREV_LOG_INTERVAL.beginning)
    )

    create_daily_table(
        yt_client,
        ypath_join(daily_rows_dir, PREV_RUN_ID),
        cases.daily_rows,
        {LOG_INTERVAL_KEY: PREV_LOG_INTERVAL.to_meta()}
    )

    corrections_data = zip(
        [PREV_RUN_ID, CURR_RUN_ID, NEXT_RUN_ID, CURR_RUN_ID + '_2'],
        [LogInterval([Subinterval('cor_c1', 'cor_t1', 0, 1, 2)]), LogInterval([Subinterval('cor_c1', 'cor_t1', 0, 2, 3)]),
         LogInterval([Subinterval('cor_c1', 'cor_t1', 0, 4, 5)]), LogInterval([Subinterval('cor_c1', 'cor_t1', 0, 3, 4)])],
        itertools.chain(CORRECTIONS_FILTERING_CASES, [CORRECTION_CASES]),
    )
    for run_id, cor_interval, cor_case in corrections_data:
        if isinstance(cor_case, list):
            cor_case = unpack_cases(cor_case)
        corrections_path = ypath_join(corrections_dir, run_id)
        create_log_table(yt_client, corrections_path, cor_case.corrections)
        set_log_meta(yt_client, corrections_path, PREV_LOG_INTERVAL, corrections=cor_interval)

    create_product_table(yt_client, product_table_path, cases.products)

    with yt_client.Transaction() as transaction:
        res = run_job(yt_client, transaction, split_by_client_start='2000-01-06T00:00:00+0000',
                      split_by_product_start='2000-01-06T00:00:00+0000')

    res_unprocessed_path, res_events_path, res_rows_path = res

    acted_metrics = query_metrics.get_table_metrics_data(
        yt_client, res_events_path
    )

    acted_metrics_matchers = map(dict2matcher, [
        {'labels': {'service_id': 'TOTAL'}, 'name': 'acted_sum',
         'type': 'float', 'value': 2048.11042},
        {'labels': {'service_id': 'TOTAL'}, 'name': 'acted_qty',
         'type': 'float', 'value': 2049.096882999983},
        {'labels': {'service_id': 'TOTAL'}, 'name': 'acted_sum_pos',
         'type': 'float', 'value': 3531.78042},
        {'labels': {'service_id': 'TOTAL'}, 'name': 'acted_qty_pos',
         'type': 'float', 'value': 3535.4452290004047},
        {'labels': {'service_id': 'TOTAL'}, 'name': 'acted_sum_neg',
         'type': 'float', 'value': -1483.67},
        {'labels': {'service_id': 'TOTAL'}, 'name': 'acted_qty_neg',
         'type': 'float', 'value': -1486.3483459999688},
        {'labels': {'service_id': '7'}, 'name': 'acted_sum',
         'type': 'float', 'value': 2048.11042},
        {'labels': {'service_id': '7'}, 'name': 'acted_qty',
         'type': 'float', 'value': 2049.096882999983},
        {'labels': {'service_id': '7'}, 'name': 'acted_sum_pos',
         'type': 'float', 'value': 3531.78042},
        {'labels': {'service_id': '7'}, 'name': 'acted_qty_pos',
         'type': 'float', 'value': 3535.4452290004047},
        {'labels': {'service_id': '7'}, 'name': 'acted_sum_neg',
         'type': 'float', 'value': -1483.67},
        {'labels': {'service_id': '7'}, 'name': 'acted_qty_neg',
         'type': 'float', 'value': -1486.3483459999688}])

    hamcrest.assert_that(
        acted_metrics,
        hamcrest.contains_inanyorder(*acted_metrics_matchers),
    )

    unprocessed_metrics = query_metrics.get_table_metrics_data(
        yt_client, res_unprocessed_path
    )

    unprocessed_metrics_matchers = map(dict2matcher, [
        {'labels': {'service_id': 'TOTAL'}, 'name': 'tariffed_sum',
         'type': 'float', 'value': -246.06999999999994},
        {'labels': {'service_id': 'TOTAL'}, 'name': 'tariffed_qty',
         'type': 'float', 'value': -247.3958870000002},
        {'labels': {'service_id': '7'}, 'name': 'tariffed_sum',
         'type': 'float', 'value': -246.06999999999994},
        {'labels': {'service_id': '7'}, 'name': 'tariffed_qty',
         'type': 'float', 'value': -247.3958870000002}
    ])

    hamcrest.assert_that(
        unprocessed_metrics,
        hamcrest.contains_inanyorder(*unprocessed_metrics_matchers),
    )

    return get_result(yt_client, res)


@pytest.mark.parametrize(
    ['acts_interval', 'have_prev_rows'],
    [
        pytest.param(PREV_LOG_INTERVAL, True, id='has prev rows'),
        pytest.param(CURR_LOG_INTERVAL, False, id='no prev rows'),
    ]
)
def test_prev_rows_is_optional(yt_client, run_job, consumes_table_path, log_table_path,
                               unprocessed_dir, unprocessed_acts_rows_dir, taxes_table_path,
                               acts_interval, have_prev_rows, rows_dir, docs_params_table_path, product_table_path):
    case = (
        CaseOrder()
        .add_consume(1, 1)
        .add_event(1, 1)
        .add_row(1, 1, 1)
    )

    create_consumes_table(yt_client, consumes_table_path, case.consumes, PREV_LOG_INTERVAL)
    create_client_docs_params(yt_client, docs_params_table_path, case.docs_params)
    create_product_table(yt_client, product_table_path, case.products)

    create_log_table(yt_client, log_table_path, case.events)
    set_log_meta(
        yt_client,
        log_table_path,
        NEXT_LOG_INTERVAL,
        NEXT_RUN_ID,
        TODAY,
        taxes_table_path,
        docs_params_table_path,
        product_table_path,
        PREV_LOG_INTERVAL,
        acts_interval.end,
    )

    unprocessed_path = ypath_join(unprocessed_dir, CURR_RUN_ID)
    create_log_table(yt_client, unprocessed_path, [])
    set_log_meta(yt_client, unprocessed_path, CURR_LOG_INTERVAL)

    create_rows_table(yt_client, ypath_join(unprocessed_acts_rows_dir, CURR_RUN_ID), [])
    set_log_meta(
        yt_client,
        ypath_join(unprocessed_acts_rows_dir, CURR_RUN_ID),
        acts_interval
    )

    create_rows_table(yt_client, ypath_join(rows_dir, CURR_RUN_ID), case.rows)
    set_log_meta(yt_client, ypath_join(rows_dir, CURR_RUN_ID), CURR_LOG_INTERVAL)

    with yt_client.Transaction() as transaction:
        with patch_run_query() as run_query_mock, patch_set_meta(), patch_run_sort(), patch_run_reduce():
            run_job(yt_client, transaction)

    assert len(run_query_mock.call_args_list) == 2
    prev_rows_query_part = 'range($rows_dir, $rows_first_table, $rows_last_table)'
    query = run_query_mock.call_args_list[0][0][1]
    if have_prev_rows:
        assert prev_rows_query_part in query
    else:
        assert prev_rows_query_part not in query


@pytest.fixture
def base_tables_metadata(yt_client, consumes_table_path, log_table_path, unprocessed_dir, rows_dir,
                         unprocessed_acts_rows_dir, taxes_table_path, docs_params_table_path, product_table_path):
    create_consumes_table(yt_client, consumes_table_path, [], PREV_LOG_INTERVAL)

    create_log_table(yt_client, log_table_path, [])
    set_log_meta(
        yt_client,
        log_table_path,
        NEXT_LOG_INTERVAL,
        NEXT_RUN_ID,
        TODAY,
        taxes_table_path,
        docs_params_table_path,
        product_table_path,
        PREV_LOG_INTERVAL,
        CURR_LOG_INTERVAL.beginning
    )

    unprocessed_path = ypath_join(unprocessed_dir, CURR_RUN_ID)
    create_log_table(yt_client, unprocessed_path, [])
    set_log_meta(yt_client, unprocessed_path, CURR_LOG_INTERVAL)

    create_rows_table(yt_client, ypath_join(rows_dir, CURR_RUN_ID), [])
    set_log_meta(yt_client, ypath_join(rows_dir, CURR_RUN_ID), CURR_LOG_INTERVAL)

    unprocessed_acts_path = ypath_join(unprocessed_acts_rows_dir, PREV_RUN_ID)
    create_rows_table(yt_client, unprocessed_acts_path, [])
    set_log_meta(yt_client, unprocessed_acts_path, PREV_LOG_INTERVAL)


@pytest.mark.usefixtures('base_tables_metadata')
@pytest.mark.parametrize(
    'meta, error_msg',
    [
        pytest.param(
            {LOG_INTERVAL_KEY: NEXT_LOG_INTERVAL.to_meta(), DYN_TABLE_IS_UPDATING_KEY: False},
            'consumes have wrong log interval',
            id='interval'
        ),
        pytest.param(
            {LOG_INTERVAL_KEY: PREV_LOG_INTERVAL.to_meta(), DYN_TABLE_IS_UPDATING_KEY: True},
            'is updating!',
            id='dyntable_is_updating'
        )
    ]
)
def test_consumes_check(yt_client, run_job, consumes_table_path, meta, error_msg):
    utils.meta.set_log_tariff_meta(yt_client, consumes_table_path, meta)

    with pytest.raises(AssertionError) as exc_info:
        with yt_client.Transaction() as transaction:
            with patch_run_query() as mock_obj:
                run_job(yt_client, transaction)

    assert error_msg in exc_info.value.args[0]
    assert not mock_obj.called


@pytest.mark.usefixtures('base_tables_metadata')
def test_no_taxes(yt_client, run_job, taxes_dir, taxes_table_path):
    yt_client.move(taxes_table_path, ypath_join(taxes_dir, 'some_random_other_taxes_table'))

    with pytest.raises(AssertionError) as exc_info:
        with yt_client.Transaction() as transaction:
            with patch_run_query() as mock_obj:
                run_job(yt_client, transaction)

    assert 'No such taxes table' in exc_info.value.args[0]
    assert not mock_obj.called


def test_rows_range(yt_client, run_job, consumes_table_path, log_table_path, unprocessed_dir,
                    rows_dir, unprocessed_acts_rows_dir, taxes_table_path, docs_params_table_path, product_table_path):
    create_consumes_table(yt_client, consumes_table_path, [], PREV_LOG_INTERVAL)

    create_client_docs_params(yt_client, docs_params_table_path, [])
    create_product_table(yt_client, product_table_path, [])
    create_log_table(yt_client, log_table_path, [])
    set_log_meta(
        yt_client,
        log_table_path,
        LogInterval([Subinterval('a', 'a', 0, 50, 100)]),
        't50',
        TODAY,
        taxes_table_path,
        docs_params_table_path,
        product_table_path,
        PREV_LOG_INTERVAL,
        LogInterval([Subinterval('a', 'a', 0, 10, 10)]).beginning,
    )

    unprocessed_path = ypath_join(unprocessed_dir, 't40')
    create_log_table(yt_client, unprocessed_path, [])
    set_log_meta(yt_client, unprocessed_path, LogInterval([Subinterval('a', 'a', 0, 40, 50)]))

    create_rows_table(yt_client, ypath_join(rows_dir, 't00'), [])
    set_log_meta(yt_client, ypath_join(rows_dir, 't00'), LogInterval([Subinterval('a', 'a', 0, 0, 10)]))

    create_rows_table(yt_client, ypath_join(rows_dir, 't10'), [])
    set_log_meta(yt_client, ypath_join(rows_dir, 't10'), LogInterval([Subinterval('a', 'a', 0, 10, 20)]))

    create_rows_table(yt_client, ypath_join(rows_dir, 't20'), [])
    set_log_meta(yt_client, ypath_join(rows_dir, 't20'), LogInterval([Subinterval('a', 'a', 0, 20, 30)]))

    create_rows_table(yt_client, ypath_join(rows_dir, 't30'), [])
    set_log_meta(yt_client, ypath_join(rows_dir, 't30'), LogInterval([Subinterval('a', 'a', 0, 30, 40)]))

    create_rows_table(yt_client, ypath_join(rows_dir, 't40'), [])
    set_log_meta(yt_client, ypath_join(rows_dir, 't40'), LogInterval([Subinterval('a', 'a', 0, 40, 50)]))

    unprocessed_acts_path = ypath_join(unprocessed_acts_rows_dir, 't00')
    create_rows_table(yt_client, unprocessed_acts_path, [])
    set_log_meta(
        yt_client,
        unprocessed_acts_path,
        LogInterval([Subinterval('a', 'a', 0, 0, 10)]),
    )

    with yt_client.Transaction() as transaction:
        with patch_run_query() as mock_obj, patch_set_meta(), patch_run_sort(), patch_run_reduce():
            run_job(yt_client, transaction)

    hamcrest.assert_that(
        mock_obj.call_args_list[0][0][2],
        hamcrest.has_entries({
            '$rows_dir': rows_dir,
            '$rows_first_table': 't10',
            '$rows_last_table': 't40',
        })
    )


@pytest.mark.usefixtures('base_tables_metadata')
def test_unprocessed_rows_check(yt_client, run_job, unprocessed_dir, docs_params_table_path, product_table_path):
    create_client_docs_params(yt_client, docs_params_table_path, [])
    create_product_table(yt_client, product_table_path, [])
    unprocessed_path = ypath_join(unprocessed_dir, CURR_RUN_ID)
    set_log_meta(yt_client, unprocessed_path, PREV_LOG_INTERVAL)

    with pytest.raises(AssertionError) as exc_info:
        with yt_client.Transaction() as transaction:
            with patch_run_query() as mock_obj:
                run_job(yt_client, transaction)

    assert 'interval mismatch for {}'.format(unprocessed_dir) in exc_info.value.args[0]
    assert not mock_obj.called


@pytest.mark.usefixtures('base_tables_metadata')
def test_unprocessed_acts_check(yt_client, run_job, unprocessed_acts_rows_dir, docs_params_table_path, product_table_path):
    create_client_docs_params(yt_client, docs_params_table_path, [])
    create_product_table(yt_client, product_table_path, [])
    unprocessed_acts_path = ypath_join(unprocessed_acts_rows_dir, PREV_RUN_ID)
    set_log_meta(yt_client, unprocessed_acts_path, CURR_LOG_INTERVAL)

    with pytest.raises(AssertionError) as exc_info:
        with yt_client.Transaction() as transaction:
            with patch_run_query() as mock_obj:
                run_job(yt_client, transaction)

    assert 'interval mismatch for {}'.format(unprocessed_acts_rows_dir) in exc_info.value.args[0]
    assert not mock_obj.called


@pytest.mark.usefixtures('base_tables_metadata')
def test_already_processed(yt_client, run_job, log_table_path, res_unprocessed_dir, res_events_dir, res_rows_dir,
                           docs_params_table_path, product_table_path):
    meta = utils.meta.get_log_tariff_meta(yt_client, log_table_path)
    create_client_docs_params(yt_client, docs_params_table_path, [])
    create_product_table(yt_client, product_table_path, [])

    create_log_table(yt_client, ypath_join(res_unprocessed_dir, NEXT_RUN_ID), [])
    utils.meta.set_log_tariff_meta(yt_client, ypath_join(res_unprocessed_dir, NEXT_RUN_ID), meta)

    create_log_table(yt_client, ypath_join(res_events_dir, NEXT_RUN_ID), [])
    utils.meta.set_log_tariff_meta(yt_client, ypath_join(res_events_dir, NEXT_RUN_ID), meta)

    create_rows_table(yt_client, ypath_join(res_rows_dir, NEXT_RUN_ID), [])
    utils.meta.set_log_tariff_meta(yt_client, ypath_join(res_rows_dir, NEXT_RUN_ID), meta)

    with yt_client.Transaction() as transaction:
        with patch_run_query() as mock_obj:
            res_unprocessed_path, res_events_path, res_rows_path = run_job(yt_client, transaction)

    assert not mock_obj.called
    assert res_unprocessed_path == ypath_join(res_unprocessed_dir, NEXT_RUN_ID)
    assert res_events_path == ypath_join(res_events_dir, NEXT_RUN_ID)
    assert res_rows_path == ypath_join(res_rows_dir, NEXT_RUN_ID)


@pytest.mark.usefixtures('base_tables_metadata')
def test_partial_already_processed(yt_client, run_job, log_table_path, res_events_dir, res_rows_dir,
                                   docs_params_table_path, product_table_path):
    meta = utils.meta.get_log_tariff_meta(yt_client, log_table_path)

    create_client_docs_params(yt_client, docs_params_table_path, [])
    create_product_table(yt_client, product_table_path, [])
    create_log_table(yt_client, ypath_join(res_events_dir, NEXT_RUN_ID), [])
    utils.meta.set_log_tariff_meta(yt_client, ypath_join(res_events_dir, NEXT_RUN_ID), meta)

    create_rows_table(yt_client, ypath_join(res_rows_dir, NEXT_RUN_ID), [])
    utils.meta.set_log_tariff_meta(yt_client, ypath_join(res_rows_dir, NEXT_RUN_ID), meta)

    with pytest.raises(AssertionError) as exc_info:
        with yt_client.Transaction() as transaction:
            with patch_run_query() as mock_obj:
                run_job(yt_client, transaction)

    assert not mock_obj.called
    assert 'Partially formed result!' in exc_info.value.args[0]


@pytest.mark.usefixtures('base_tables_metadata')
def test_result_wrong_meta(yt_client, run_job, log_table_path, res_unprocessed_dir, res_events_dir, res_rows_dir,
                           docs_params_table_path, product_table_path):
    meta = utils.meta.get_log_tariff_meta(yt_client, log_table_path)

    create_client_docs_params(yt_client, docs_params_table_path, [])
    create_product_table(yt_client, product_table_path, [])

    create_log_table(yt_client, ypath_join(res_unprocessed_dir, NEXT_RUN_ID), [])
    utils.meta.set_log_tariff_meta(
        yt_client,
        ypath_join(res_unprocessed_dir, NEXT_RUN_ID),
        {'some_crappy_key': 'some_crappy_value', **meta}
    )

    create_log_table(yt_client, ypath_join(res_events_dir, NEXT_RUN_ID), [])
    utils.meta.set_log_tariff_meta(yt_client, ypath_join(res_events_dir, NEXT_RUN_ID), meta)

    create_rows_table(yt_client, ypath_join(res_rows_dir, NEXT_RUN_ID), [])
    utils.meta.set_log_tariff_meta(yt_client, ypath_join(res_rows_dir, NEXT_RUN_ID), meta)

    with pytest.raises(AssertionError) as exc_info:
        with yt_client.Transaction() as transaction:
            with patch_run_query() as mock_obj:
                run_job(yt_client, transaction)

    assert not mock_obj.called
    assert 'Bad meta in current table for {}'.format(res_unprocessed_dir) in exc_info.value.args[0]


@pytest.mark.usefixtures('base_tables_metadata')
@pytest.mark.parametrize(
    'split_by_client_start, split_by_client',
    [
        pytest.param('2020-07-07T00:00:01+0300', 0, id='before'),
        pytest.param('2020-07-06T21:00:01+0000', 0, id='before_tz'),
        pytest.param('2020-07-07T00:00:00+0300', 1, id='eq'),
        pytest.param('2020-07-06T23:59:59+0300', 1, id='after'),
    ]
)
def test_split_by_client(yt_client, run_job, log_table_path, res_unprocessed_dir, res_events_dir, res_rows_dir,
                         docs_params_table_path, split_by_client_start, split_by_client, daily_rows_dir, product_table_path):
    create_client_docs_params(yt_client, docs_params_table_path, [])
    create_product_table(yt_client, product_table_path, [])

    create_daily_table(yt_client, ypath_join(daily_rows_dir, PREV_RUN_ID), [], {
        LOG_INTERVAL_KEY: PREV_LOG_INTERVAL.to_meta()
    })

    with yt_client.Transaction() as transaction:
        with patch_run_query() as mock_obj:
            run_job(yt_client, transaction, split_by_client_start=split_by_client_start)

    hamcrest.assert_that(
        mock_obj.call_args_list[0][0][2],
        hamcrest.has_entries({
            '$split_by_client': split_by_client,
        })
    )
