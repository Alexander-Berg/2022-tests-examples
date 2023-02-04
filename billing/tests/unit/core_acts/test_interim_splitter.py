# -*- coding: utf-8 -*-

import sys
import decimal
import itertools
from typing import (
    Dict,
    List,
    Optional,
)

import pytest
import hamcrest as hm

from billing.log_tariffication.py.lib.utils.numeric import (
    float2decimal,
    decimal2float,
)
from billing.log_tariffication.py.lib.logic.interim_splitter import (
    InterimSplitter,
)


def mk_split_key(
    invoice_id: int = 666,
    tax_policy_id: int = 667,
    client_id: int = 668,
    custom: Optional[dict] = None
) -> Dict:
    return {
        'invoice_id': invoice_id,
        'tax_policy_id': tax_policy_id,
        'client_id': client_id,
        'custom': custom if custom is not None else {'group_docs': 669},
    }


def mk_netting_key(tax_policy_pct_id: int) -> Dict:
    return {
        'tax_policy_pct_id': tax_policy_pct_id,
    }


def mk_header_key(tax_policy_pct_id: int, split_key: Optional[dict] = None) -> Dict:
    split_key = split_key or mk_split_key()
    return {
        'invoice_id': split_key['invoice_id'],
        'tax_policy_pct_id': tax_policy_pct_id,
        'custom': split_key['custom'],
    }


def mk_prev_group(key: dict, sum_: float) -> Dict:
    return {
        'netting_key': key,
        'acted_sum': sum_,
    }


def mk_log_row(
    key: Dict,
    qty: float,
    sum_: Optional[float] = None,
    k_qty: Optional[float] = None,
    k_sum: Optional[float] = None,
    consume_id=666,
    counter=itertools.count(1),
    ProductID=0,
) -> Dict:
    return {
        'UID': str(next(counter)),
        'ServiceID': next(counter),
        'EffectiveServiceOrderID': next(counter),
        'ServiceOrderID': next(counter),
        'netting_key': key,
        'row_key': next(counter),
        'consume_id': consume_id,
        'act_dt': next(counter),
        'tariff_dt': next(counter),
        'tariffed_qty': qty,
        'tariffed_sum': sum_ if sum_ is not None else qty,
        'coeff_qty': k_qty if k_qty is not None else qty,
        'coeff_sum': k_sum if k_sum is not None else qty,
        'ProductID': ProductID,
    }


def mk_cur_row(rows: List[Dict]) -> Dict:
    key = None
    total_sum = decimal.Decimal(0)
    max_sum = decimal.Decimal(-sys.maxsize)
    min_qty = decimal.Decimal(0)
    for row in rows:
        if key is None:
            key = row['netting_key']
        else:
            assert key == row['netting_key']
        total_sum += float2decimal(row['tariffed_sum'])
        max_sum = max(max_sum, float2decimal(row['tariffed_sum']))
        min_qty = min(min_qty, float2decimal(row['tariffed_qty']))

    return {
        'netting_key': key,
        'tariffed_sum': decimal2float(total_sum),
        'max_tariffed_sum': decimal2float(max_sum),
        'min_tariffed_qty': decimal2float(min_qty),
    }


def process_rows(splitter: InterimSplitter, rows: List[Dict]) -> List[Dict]:
    res = []
    for row in rows:
        res.extend(splitter.process_log(row))
    return res


def check_processed(tpp_id, qty, sum_):
    return hm.has_entries(
        header_key=mk_header_key(tpp_id),
        tariffed_qty=qty,
        tariffed_sum=sum_,
    )


def check_unprocessed(qty, sum_):
    return hm.all_of(
        hm.has_entries(
            tariffed_qty=qty,
            tariffed_sum=sum_,
        ),
        hm.not_(hm.has_entries(header_key=hm.anything())),
    )


@pytest.mark.parametrize(
    'prev_groups, new_rows, res_rows',
    [
        pytest.param([], [[(1, -10)], [(2, 100)]], [(2, -10), (2, 100)]),
        pytest.param([], [[(1, 100)], [(2, -10)]], [(1, 100), (1, -10)]),
        pytest.param([], [[(1, -100)], [(2, 10)]], [(2, -10), (None, -90), (2, 10)]),
        pytest.param([], [[(1, 10)], [(2, -100)]], [(1, 10), (1, -10), (None, -90)]),
        pytest.param([(1, 10)], [[(1, -100)], [(2, 10)]],
                     [(1, -10), (2, -10), (None, -80), (2, 10)]),
        pytest.param([(2, 10)], [[(1, 10)], [(2, -100)]],
                     [(1, 10), (2, -10), (1, -10), (None, -80)]),
        pytest.param([(1, 10)], [[(2, -10)]], [(1, -10)]),
        pytest.param([(1, 10), (2, 20)], [[(2, -30)]], [(1, -10), (2, -20)]),
        pytest.param([(2, 20)], [[(1, -10)]], [(2, -10)]),
        pytest.param([(1, 10), (2, 20)], [[(1, -15)]], [(1, -10), (2, -5)]),
        pytest.param([(1, 666)], [[(1, -100), (1, 99)]], [(1, -100), (1, 99)]),
        pytest.param([(1, 1)], [[(1, -100), (1, 99)]], [(1, -100), (1, 99)]),
        pytest.param([(1, 1), (2, 1)], [[(1, -100), (1, 98)]], [(1, -1), (2, -99), (2, 98)]),
        pytest.param([(1, 666)], [[(1, 99), (1, -100)]], [(1, 99), (1, -100)]),
        pytest.param([], [[(1, -10)], [(2, -20)], [(3, 100)]], [(3, -10), (3, -20), (3, 100)]),
        pytest.param([], [[(1, -10)], [(2, 20)], [(3, 100)]], [(2, -10), (2, 20), (3, 100)]),
        pytest.param([], [[(1, -10)], [(2, 30)], [(3, -5)]], [(2, -10), (2, 30), (2, -5)]),
        pytest.param(
            [],
            [[(1, -10), (1, -30), (1, -5)], [(2, 15), (2, 15)]],
            [(2, -10), (2, -20), (None, -10), (None, -5), (2, 15), (2, 15)]
        ),
        pytest.param(
            [],
            [[(1, -9), (1, 2), (1, -10)], [(2, 10)]],
            [(2, -9), (2, 2), (2, -3), (None, -7), (2, 10)]
        ),
        pytest.param(
            [],
            [[(1, 2), (1, -10), (1, -10)], [(2, 10)]],
            [(2, 2), (2, -10), (2, -2), (None, -8), (2, 10)]
        ),
        pytest.param(
            [],
            [[(1, 10), (1, -7), (1, -8)]],
            [(None, 10), (None, -7), (None, -8)]
        ),
        pytest.param(
            [],
            [[(1, -10), (1, -10), (1, 2)], [(2, 10)]],
            [(2, -10), (None, -8), (1, -2), (1, 2), (2, 10)]
        ),
        pytest.param(
            [],
            [[(1, -666), (1, 666)], [(2, 1)]],
            [(1, -666), (1, 666), (2, 1)]
        ),
        # Тесты на отрицательные предыдущие группы
        pytest.param([(1, -10)], [[(1, 9)]], [(1, 9)]),
        pytest.param([(1, -10)], [[(1, 10)]], [(1, 10)]),
        pytest.param([(1, -10)], [[(1, 3), (1, 8)]], [(1, 3), (1, 8)]),
        pytest.param([(1, -10)], [[(1, -1), (1, -2)]], [(None, -1), (None, -2)]),
        pytest.param([(1, -10)], [[(2, -1)]], [(None, -1)]),
        pytest.param([(1, -10)], [[(1, 9)], [(2, -1)]], [(1, 9), (None, -1)]),
        pytest.param([(1, -10)], [[(1, 11)], [(2, -1)]], [(1, 11), (1, -1)]),
        pytest.param([(1, -10)], [[(1, 11)], [(2, -2)]], [(1, 11), (1, -1), (None, -1)]),
        pytest.param([(1, -10), (2, 10)], [[(3, -11)]], [(2, -10), (None, -1)]),
        pytest.param([(1, -10)], [[(2, 3), (2, 5)], [(3, -6)]], [(2, 3), (2, 5), (2, -6)]),
    ]
)
def test_split(prev_groups, new_rows, res_rows):
    splitter = InterimSplitter(mk_split_key())

    for tpp_id, sum_ in prev_groups:
        splitter.add_prev_group(mk_prev_group(mk_netting_key(tpp_id), sum_))

    all_rows = []
    for rows_group in new_rows:
        rows = [
            mk_log_row(mk_netting_key(tpp_id), qty)
            for tpp_id, qty in rows_group
        ]
        splitter.add_group(mk_cur_row(rows))
        all_rows.extend(rows)

    splitter.calculate_split()

    res = process_rows(splitter, all_rows)

    hm.assert_that(
        res,
        hm.contains(*[
            check_processed(tpp_id, qty, qty)
            if tpp_id is not None else
            check_unprocessed(qty, qty)
            for tpp_id, qty in res_rows
        ])
    )


@pytest.mark.parametrize(
    'pos_sum, req_qty_acted, req_sum_acted, req_qty_tariffed, req_sum_tariffed',
    [
        (0.3, -2.1, -0.3, -4.9, -0.7),
        (0.14, -0.98, -0.14, -6.02, -0.86),
    ]
)
def test_rounding(pos_sum, req_qty_acted, req_sum_acted, req_qty_tariffed, req_sum_tariffed):  # test_crawfish
    splitter = InterimSplitter(mk_split_key())

    neg_rows = [mk_log_row(mk_netting_key(1), -7, -1, 7, 1)]
    pos_rows = [mk_log_row(mk_netting_key(2), pos_sum)]

    splitter.add_group(mk_cur_row(neg_rows))
    splitter.add_group(mk_cur_row(pos_rows))

    splitter.calculate_split()

    res = process_rows(splitter, pos_rows + neg_rows)

    hm.assert_that(
        res,
        hm.contains(
            check_processed(2, pos_sum, pos_sum),
            check_processed(2, req_qty_acted, req_sum_acted),
            check_unprocessed(req_qty_tariffed, req_sum_tariffed),
        )
    )


def test_rounding_precision():
    splitter = InterimSplitter(mk_split_key())

    rows = [
        mk_log_row(mk_netting_key(1), -1000.01, -1000.01),
        mk_log_row(mk_netting_key(1), 1000.001, 1000),
    ]
    splitter.add_group(mk_cur_row(rows))

    splitter.calculate_split()

    res = process_rows(splitter, rows)

    hm.assert_that(
        res,
        hm.contains(
            check_unprocessed(-0.01, -0.01),
            check_processed(1, -1000, -1000),
            check_processed(1, 1000.001, 1000),
        )
    )


def test_multiple_consumes():
    splitter = InterimSplitter(mk_split_key())

    pos_rows = [
        mk_log_row(mk_netting_key(1), 3, consume_id=1),
        mk_log_row(mk_netting_key(1), -1.5, consume_id=2),
    ]
    neg_rows = [
        mk_log_row(mk_netting_key(2), -1.4, consume_id=3),
    ]
    splitter.add_group(mk_cur_row(pos_rows))
    splitter.add_group(mk_cur_row(neg_rows))

    splitter.calculate_split()

    res = process_rows(splitter, pos_rows + neg_rows)

    hm.assert_that(
        res,
        hm.contains(
            hm.has_entries(
                header_key=mk_header_key(1),
                tariffed_qty=3,
                tariffed_sum=3,
                consume_id=1,
            ),
            hm.has_entries(
                header_key=mk_header_key(1),
                tariffed_qty=-1.5,
                tariffed_sum=-1.5,
                consume_id=2,
            ),
            hm.has_entries(
                header_key=mk_header_key(1),
                tariffed_qty=-1.4,
                tariffed_sum=-1.4,
                consume_id=3,
            ),
        )
    )


@pytest.mark.parametrize(
    'prev_sum, new_row',
    [
        pytest.param(0, (0.001, 0)),
        pytest.param(0, (-0.001, 0)),
        pytest.param(1, (-0.001, 0)),
    ]
)
def test_zero_sum_neg_qty(prev_sum, new_row):
    splitter = InterimSplitter(mk_split_key())

    splitter.add_prev_group(mk_prev_group(mk_netting_key(1), prev_sum))

    qty, sum_ = new_row
    rows = [mk_log_row(mk_netting_key(1), qty, sum_, 1, 1)]
    splitter.add_group(mk_cur_row(rows))

    splitter.calculate_split()

    res = process_rows(splitter, rows)

    hm.assert_that(
        res,
        hm.contains(check_processed(1, qty, sum_))
    )


def test_zero_total_qty():
    splitter = InterimSplitter(mk_split_key())

    neg_rows = [
        mk_log_row(mk_netting_key(1), -99.998, -100),
        mk_log_row(mk_netting_key(1), -0.001, 0),
        mk_log_row(mk_netting_key(1), -0.001, 0),
    ]
    pos_rows = [
        mk_log_row(mk_netting_key(2), 100),
    ]

    splitter.add_group(mk_cur_row(neg_rows))
    splitter.add_group(mk_cur_row(pos_rows))

    splitter.calculate_split()

    res = process_rows(splitter, neg_rows + pos_rows)

    hm.assert_that(
        res,
        hm.contains(
            check_processed(2, -99.998, -100),
            check_processed(2, -0.001, 0),
            check_processed(2, -0.001, 0),
            check_processed(2, 100, 100),
        )
    )


def test_plusminus_neg_row():
    splitter = InterimSplitter(mk_split_key())

    rows = [
        mk_log_row(mk_netting_key(1), -166.86, -166.86),
        mk_log_row(mk_netting_key(1), 166.86, 166.85),
    ]
    splitter.add_group(mk_cur_row(rows))
    splitter.add_prev_group(mk_prev_group(mk_netting_key(1), 24830.19))
    splitter.calculate_split()

    res = process_rows(splitter, rows)

    hm.assert_that(
        res,
        hm.contains(
            check_processed(1, -166.86, -166.86),
            check_processed(1, 166.86, 166.85),
        )
    )


def test_proportion_zero_split():
    splitter = InterimSplitter(mk_split_key())

    neg_rows = [mk_log_row(mk_netting_key(1), -13.806, -13.81, k_qty=100.06401, k_sum=100.06)]
    pos_rows = [mk_log_row(mk_netting_key(2), 0.00003, 0, k_qty=13.806, k_sum=13.81)]
    splitter.add_group(mk_cur_row(neg_rows))
    splitter.add_group(mk_cur_row(pos_rows))
    splitter.calculate_split()

    res = process_rows(splitter, neg_rows + pos_rows)
    hm.assert_that(
        res,
        hm.contains(
            check_unprocessed(-13.806, -13.81),
            check_processed(2, 0.00003, 0),
        )
    )


def test_proportion():
    splitter = InterimSplitter(mk_split_key())

    rows = [mk_log_row(mk_netting_key(1), -13.806, -13.81, k_qty=100.06401, k_sum=100.06)]
    splitter.add_group(mk_cur_row(rows))
    splitter.add_prev_group(mk_prev_group(mk_netting_key(2), 0.01))
    splitter.calculate_split()

    res = process_rows(splitter, rows)
    hm.assert_that(
        res,
        hm.contains(
            check_processed(2, -0.005447, -0.01),
            check_unprocessed(-13.800553, -13.8),
        )
    )
