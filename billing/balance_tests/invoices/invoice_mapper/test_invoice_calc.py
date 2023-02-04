# -*- coding: utf-8 -*-

import decimal

import pytest

from balance.mapper import (
    PriceObject,
    TaxSimple,
)
from balance import muzzle_util as ut
from balance.invoice_calc import calc
from balance.invoice_calc import calc_funcs
from balance.invoice_calc import utils as calc_utils
from balance.invoice_calc import discount as calc_discount
from balance.invoice_calc import invoice as calc_invoice

from butils import decimal_unit

DU = decimal_unit.DecimalUnit
D = decimal.Decimal

base_params = [
    calc_utils.constant_param('amount_precision', 2),
    calc_utils.constant_param('quantity_precision', 6),
    calc_utils.constant_param('taxes_alignment_required', False),
]


@calc_utils.calc_parameter('discounts')
def forced_test_discounts(forced_sum_pct, forced_qty_pct):
    res = []
    if forced_sum_pct:
        res.append(calc_discount.DiscountInfo(forced_sum_pct, False))

    if forced_qty_pct:
        res.append(calc_discount.DiscountInfo(forced_qty_pct, True))
    return res


@pytest.fixture
def type_rate(request):
    return DU(request.param, [], request.param)


class TestRowAmountCalculator(object):
    AmountsCalcTest = calc.create_calc(
        'CalcTest',
        [
            base_params,
            calc_utils.input_param('quantity'),
            calc_utils.input_param('price_obj'),
            calc_utils.input_param('forced_qty_pct'),
            calc_utils.input_param('forced_sum_pct'),
            forced_test_discounts,
            calc_funcs.tax_policy_pct,
            calc_funcs.amount_discount_pct,
            calc_funcs.quantity_discount_pct,
            calc_funcs.price_by_piece,
            calc_funcs.adjusted_quantity,
            calc_funcs.amount_nds,
            calc_funcs.amount_nsp,
            calc_funcs.amount,
            calc_funcs.amount_without_discount,
            calc_funcs.amount_without_tax,
            calc_funcs.initial_amount,
            calc_funcs.initial_amount_without_tax,
        ],
        output=[
            'adjusted_quantity', 'amount_nds', 'amount_nsp', 'amount', 'initial_amount',
            'initial_amount_without_tax', 'amount_without_discount', 'amount_without_tax',
        ]
    )

    @staticmethod
    def _do_test_calc(*args, **kwargs):
        inputs = kwargs.items()
        input_params = [calc_utils.input_param(i[0]) for i in inputs]

        output = [calc_utils.extract_param(f).name for f in args]

        calc_cls = calc.create_calc(
            'CalcTest',
            [
                base_params,
                input_params,
                args,
            ],
            output=output
        )
        return calc_cls(*(i[1] for i in inputs)).calc()

    @classmethod
    def _do_test_amounts_calc_from_params(cls, qty, price, type_rate, nds_pct, nsp_pct, qty_pct, sum_pct, **kwargs):
        tpp = TaxSimple(nds_pct=DU(nds_pct, '%'), nsp_pct=DU(nsp_pct, '%'))
        price_obj = PriceObject(price=price, type_rate=type_rate, tax_policy_pct=tpp)
        return cls.AmountsCalcTest(qty, price_obj, qty_pct, sum_pct).calc()

    @pytest.mark.parametrize(
        'price, type_rate',
        [
            [DU(1, 'FISH', [1, 'QTY']), 1],
            [DU('1.0000001', 'RUB', [100, 'QTY']), 100],
        ],
        ['type_rate'],
        ids=lambda o: str(o)
    )
    def test_price(self, price, type_rate):
        res = self._do_test_calc(calc_funcs.price, price_obj=PriceObject(price=price, type_rate=type_rate))
        assert price == res.price

    @pytest.mark.parametrize(
        'price, type_rate, req_price',
        [
            [DU(1, 'A', [1, 'B']), 1, DU(1, 'A', 'B')],
            [DU(66, 'A', [100, 'B']), 100, DU('0.66', 'A', 'B')],
            [DU(1, 'A', [D('0.1'), 'B']), D('0.1'), DU(10, 'A', 'B')],
            [DU(7, 'A', [66, 'B']), 66, DU('0.1060606061', 'A', 'B')],
        ],
        ['type_rate'],
        ids=lambda o: str(o)
    )
    def test_price_by_piece(self, price, type_rate, req_price):
        res = self._do_test_calc(calc_funcs.price_by_piece, price_obj=PriceObject(price=price, type_rate=type_rate))
        assert req_price == ut.round(res.price_by_piece, 10)

    @pytest.mark.parametrize(
        'price, type_rate, nds_pct, nsp_pct, req_price',
        [
            [DU(10, 'A', [1, 'B']), 1, 0, 0, DU(10, 'A', [1, 'B'])],
            [DU(10, 'A', [1, 'B']), 1, 18, 0, DU('8.4745762712', 'A', [1, 'B'])],
            [DU(10, 'A', [1, 'B']), 1, 0, 66, DU('6.0240963855', 'A', [1, 'B'])],
            [DU(10, 'A', [100, 'B']), 100, 0, 66, DU('6.0240963855', 'A', [100, 'B'])],
            [DU(10, 'A', [100, 'B']), 100, 10, 10, DU('8.3333333333', 'A', [100, 'B'])],
        ],
        ['type_rate'],
        ids=lambda o: str(o)
    )
    def test_price_wo_tax(self, price, type_rate, nds_pct, nsp_pct, req_price):
        tpp = TaxSimple(nds_pct=DU(nds_pct, '%'), nsp_pct=DU(nsp_pct, '%'))
        price_obj = PriceObject(price=price, type_rate=type_rate, tax_policy_pct=tpp)
        res = self._do_test_calc(calc_funcs.price, calc_funcs.price_wo_tax, price_obj=price_obj)
        assert price == res.price
        assert req_price == ut.round(res.price_wo_tax, 10)

    @pytest.mark.parametrize(
        'price, type_rate, nds_pct, nsp_pct, req_price',
        [
            [DU(10, 'A', [1, 'B']), 1, 0, 66, DU('6.0240963855', 'A', 'B')],
            [DU(10, 'A', [100, 'B']), 100, 0, 66, DU('0.0602409639', 'A', 'B')],
            [DU(10, 'A', [100, 'B']), 100, 10, 10, DU('0.0833333333', 'A', 'B')],
        ],
        ['type_rate'],
        ids=lambda o: str(o)
    )
    def test_price_wo_tax_by_piece(self, price, type_rate, nds_pct, nsp_pct, req_price):
        tpp = TaxSimple(nds_pct=DU(nds_pct, '%'), nsp_pct=DU(nsp_pct, '%'))
        price_obj = PriceObject(price=price, type_rate=type_rate, tax_policy_pct=tpp)
        res = self._do_test_calc(calc_funcs.price_wo_tax_by_piece, price_obj=price_obj)
        assert req_price == ut.round(res.price_wo_tax_by_piece, 10)

    @pytest.mark.parametrize(
        'nds_pct, nsp_pct',
        [
            [0, 0],
            [0, 10],
            [10, 0],
            [10, 10],
        ],
        ids=lambda o: str(o)
    )
    def test_nds_pct(self, nds_pct, nsp_pct):
        tpp = TaxSimple(nds_pct=DU(nds_pct, '%'), nsp_pct=DU(nsp_pct, '%'))
        price_obj = PriceObject(price=1, type_rate=1, tax_policy_pct=tpp)
        res = self._do_test_calc(calc_funcs.nds_pct, calc_funcs.tax_policy_pct, price_obj=price_obj)
        assert nds_pct == res.nds_pct

    @pytest.mark.parametrize(
        'nds_pct, nsp_pct',
        [
            [0, 0],
            [0, 10],
            [10, 0],
            [10, 10],
        ],
        ids=lambda o: str(o)
    )
    def test_nsp_pct(self, nds_pct, nsp_pct):
        tpp = TaxSimple(nds_pct=DU(nds_pct, '%'), nsp_pct=DU(nsp_pct, '%'))
        price_obj = PriceObject(price=1, type_rate=1, tax_policy_pct=tpp)
        res = self._do_test_calc(calc_funcs.nsp_pct, calc_funcs.tax_policy_pct, price_obj=price_obj)
        assert nsp_pct == res.nsp_pct

    @pytest.mark.parametrize(
        'nds_pct, nsp_pct, taxes_pct',
        [
            [0, 0, 0],
            [0, 66, 66],
            [66, 0, 66],
            [33, 33, 66],
        ],
        ids=lambda o: str(o)
    )
    def test_taxes_pct(self, nds_pct, nsp_pct, taxes_pct):
        tpp = TaxSimple(nds_pct=DU(nds_pct, '%'), nsp_pct=DU(nsp_pct, '%'))
        price_obj = PriceObject(price=1, type_rate=1, tax_policy_pct=tpp)
        res = self._do_test_calc(
            calc_funcs.taxes_pct,
            calc_funcs.nds_pct,
            calc_funcs.nsp_pct,
            calc_funcs.tax_policy_pct,
            price_obj=price_obj
        )
        assert taxes_pct == res.taxes_pct

    @pytest.mark.parametrize(
        'qty_pct, sum_pct',
        [
            [0, 0],
            [10, 0],
            [0, 10],
            [10, 10],
        ],
        ids=lambda o: str(o)
    )
    def test_qty_discount_pct(self, qty_pct, sum_pct):
        res = self._do_test_calc(
            forced_test_discounts,
            calc_funcs.quantity_discount_pct,
            forced_sum_pct=sum_pct,
            forced_qty_pct=qty_pct,
        )
        assert qty_pct == res.quantity_discount_pct

    @pytest.mark.parametrize(
        'qty_pct, sum_pct',
        [
            [0, 0],
            [10, 0],
            [0, 10],
            [10, 10],
        ],
        ids=lambda o: str(o)
    )
    def test_sum_discount_pct(self, qty_pct, sum_pct):
        res = self._do_test_calc(
            forced_test_discounts,
            calc_funcs.amount_discount_pct,
            forced_sum_pct=sum_pct,
            forced_qty_pct=qty_pct,
        )
        assert sum_pct == res.amount_discount_pct

    @pytest.mark.parametrize(
        'qty_pct, sum_pct, total_pct',
        [
            [0, 0, 0],
            [10, 0, 10],
            [0, 10, 10],
            [10, 10, 19],
        ],
        ids=lambda o: str(o)
    )
    def test_discount_pct(self, qty_pct, sum_pct, total_pct):
        res = self._do_test_calc(
            calc_discount.simple_discounts,
            calc_funcs.discount_pct,
            amount_discount_pct=sum_pct,
            quantity_discount_pct=qty_pct,
        )
        assert total_pct == res.discount_pct

    @pytest.mark.parametrize(
        'qty, price, type_rate, nds_pct, nsp_pct, qty_pct, sum_pct, req_sum',
        [
            [DU(10, 'Q'), DU('0.5', 'R', [1, 'Q']), 1, 18, 0, 0, 0, DU(5, 'R')],
            [DU(10, 'Q'), DU('0.5', 'R', [1, 'Q']), 1, 18, 18, 0, 0, DU(5, 'R')],
            [DU(10, 'Q'), DU('0.5', 'R', [1, 'Q']), 1, 0, 0, 0, 0, DU(5, 'R')],
            [DU(10, 'Q'), DU('0.5', 'R', [1, 'Q']), 1, 0, 0, 10, 0, DU(5, 'R')],
            [DU(10, 'Q'), DU('0.5', 'R', [1, 'Q']), 1, 0, 0, 0, 10, DU('4.5', 'R')],
            [DU(10, 'Q'), DU('0.5', 'R', [1, 'Q']), 1, 66, 0, 66, 66, DU('1.70', 'R')],
            [DU(10, 'Q'), DU('0.5', 'R', [10, 'Q']), 10, 66, 0, 66, 66, DU('0.17', 'R')],
            [DU(1000, 'Q'), DU('0.1', 'R', [1000, 'Q']), 1000, 66, 0, 10, 10, DU('0.09', 'R')],
        ],
        ['type_rate'],
        ids=lambda o: str(o)
    )
    def test_amount(self, qty, price, type_rate, nds_pct, nsp_pct, qty_pct, sum_pct, req_sum):
        res = self._do_test_amounts_calc_from_params(**locals())
        assert req_sum == ut.round(res.amount, 10)

    @pytest.mark.parametrize(
        'qty, price, type_rate, nds_pct, nsp_pct, qty_pct, sum_pct, req_sum',
        [
            [DU(10, 'Q'), DU('0.5', 'R', [1, 'Q']), 1, 0, 0, 0, 0, DU('0', 'R')],
            [DU(10, 'Q'), DU('0.5', 'R', [1, 'Q']), 1, 10, 0, 0, 0, DU('0.45', 'R')],
            [DU(10, 'Q'), DU('0.5', 'R', [1, 'Q']), 1, 0, 10, 0, 0, DU('0', 'R')],
            [DU(10, 'Q'), DU('0.5', 'R', [1, 'Q']), 1, 10, 10, 0, 0, DU('0.42', 'R')],
            [DU(10, 'Q'), DU('0.5', 'R', [1, 'Q']), 1, 10, 0, 66, 66, DU('0.15', 'R')],
            [DU(10, 'Q'), DU('0.5', 'R', [10, 'Q']), 10, 10, 0, 66, 66, DU('0.02', 'R')],
        ],
        ['type_rate'],
        ids=lambda o: str(o)
    )
    def test_amount_nds(self, qty, price, type_rate, nds_pct, nsp_pct, qty_pct, sum_pct, req_sum):
        res = self._do_test_amounts_calc_from_params(**locals())
        assert req_sum == ut.round(res.amount_nds, 10)

    @pytest.mark.parametrize(
        'qty, price, type_rate, nds_pct, nsp_pct, qty_pct, sum_pct, req_sum',
        [
            [DU(10, 'Q'), DU('0.5', 'R', [1, 'Q']), 1, 0, 0, 0, 0, DU('0', 'R')],
            [DU(10, 'Q'), DU('0.5', 'R', [1, 'Q']), 1, 10, 0, 0, 0, DU('0', 'R')],
            [DU(10, 'Q'), DU('0.5', 'R', [1, 'Q']), 1, 0, 10, 0, 0, DU('0.45', 'R')],
            [DU(10, 'Q'), DU('0.5', 'R', [1, 'Q']), 1, 10, 10, 0, 0, DU('0.42', 'R')],
            [DU(10, 'Q'), DU('0.5', 'R', [1, 'Q']), 1, 0, 10, 0, 0, DU('0.45', 'R')],
            [DU(10, 'Q'), DU('0.5', 'R', [1, 'Q']), 1, 0, 10, 66, 66, DU('0.15', 'R')],
            [DU(10, 'Q'), DU('0.5', 'R', [10, 'Q']), 10, 0, 10, 66, 66, DU('0.02', 'R')],
        ],
        ['type_rate'],
        ids=lambda o: str(o)
    )
    def test_amount_nsp(self, qty, price, type_rate, nds_pct, nsp_pct, qty_pct, sum_pct, req_sum):
        res = self._do_test_amounts_calc_from_params(**locals())
        assert req_sum == ut.round(res.amount_nsp, 10)

    @pytest.mark.parametrize(
        'qty, price, type_rate, nds_pct, nsp_pct, qty_pct, sum_pct, req_sum',
        [
            [DU(100, 'Q'), DU('0.5', 'R', [1, 'Q']), 1, 0, 0, 0, 0, DU('50', 'R')],
            [DU(100, 'Q'), DU('0.5', 'R', [1, 'Q']), 1, 10, 0, 0, 0, DU('45.45', 'R')],
            [DU(100, 'Q'), DU('0.5', 'R', [1, 'Q']), 1, 0, 10, 0, 0, DU('45.45', 'R')],
            [DU(100, 'Q'), DU('0.5', 'R', [1, 'Q']), 1, 10, 10, 0, 0, DU('41.66', 'R')],
            [DU(100, 'Q'), DU('0.5', 'R', [1, 'Q']), 1, 10, 10, 10, 0, DU('41.66', 'R')],
            [DU(100, 'Q'), DU('0.5', 'R', [1, 'Q']), 1, 10, 10, 0, 10, DU('37.5', 'R')],
            [DU(100, 'Q'), DU('0.5', 'R', [1, 'Q']), 1, 10, 10, 10, 10, DU('37.5', 'R')],
        ],
        ['type_rate'],
        ids=lambda o: str(o)
    )
    def test_amount_wo_tax(self, qty, price, type_rate, nds_pct, nsp_pct, qty_pct, sum_pct, req_sum):
        res = self._do_test_amounts_calc_from_params(**locals())
        assert req_sum == ut.round(res.amount_without_tax, 10)

    @pytest.mark.parametrize(
        'qty, price, type_rate, nds_pct, nsp_pct, qty_pct, sum_pct, req_sum',
        [
            [DU(10, 'Q'), DU('0.5', 'R', [1, 'Q']), 1, 0, 0, 0, 0, DU(5, 'R')],
            [DU(10, 'Q'), DU('0.5', 'R', [1, 'Q']), 1, 18, 0, 0, 0, DU(5, 'R')],
            [DU(10, 'Q'), DU('0.5', 'R', [1, 'Q']), 1, 18, 18, 0, 0, DU(5, 'R')],
            [DU(10, 'Q'), DU('0.5', 'R', [1, 'Q']), 1, 0, 0, 10, 0, DU('5.56', 'R')],
            [DU(10, 'Q'), DU('0.5', 'R', [1, 'Q']), 1, 0, 0, 0, 10, DU(5, 'R')],
            [DU(10, 'Q'), DU('0.5', 'R', [1, 'Q']), 1, 66, 0, 66, 66, DU('14.71', 'R')],
            [DU(10, 'Q'), DU('0.5', 'R', [10, 'Q']), 10, 66, 0, 66, 66, DU('1.47', 'R')],
        ],
        ['type_rate'],
        ids=lambda o: str(o)
    )
    def test_amount_wo_discount(self, qty, price, type_rate, nds_pct, nsp_pct, qty_pct, sum_pct, req_sum):
        res = self._do_test_amounts_calc_from_params(**locals())
        assert req_sum == ut.round(res.amount_without_discount, 10)

    @pytest.mark.parametrize(
        'qty, qty_pct, sum_pct, req_qty',
        [
            [DU(100, 'Q'), 0, 0, DU(100, 'Q')],
            [DU(100, 'Q'), 0, 10, DU(100, 'Q')],
            [DU(100, 'Q'), 10, 0, DU('111.111111', 'Q')],
            [DU(100, 'Q'), 10, 10, DU('111.111111', 'Q')],
        ],
        ids=lambda o: str(o)
    )
    def test_adjusted_qty(self, qty, qty_pct, sum_pct, req_qty):
        res = self._do_test_amounts_calc_from_params(
            nds_pct=66,
            nsp_pct=66,
            price=DU(100, 'R', 'Q'),
            type_rate=6,
            **locals()
        )
        assert req_qty == ut.round(res.adjusted_quantity, 10)

    @pytest.mark.parametrize(
        'qty, price, type_rate, nds_pct, nsp_pct, qty_pct, sum_pct, req_sum',
        [
            [DU(10, 'Q'), DU('0.5', 'R', [1, 'Q']), 1, 18, 0, 0, 0, DU(5, 'R')],
            [DU(10, 'Q'), DU('0.5', 'R', [1, 'Q']), 1, 18, 18, 0, 0, DU(5, 'R')],
            [DU(10, 'Q'), DU('0.5', 'R', [1, 'Q']), 1, 0, 0, 0, 0, DU(5, 'R')],
            [DU(10, 'Q'), DU('0.5', 'R', [1, 'Q']), 1, 0, 0, 10, 0, DU(5, 'R')],
            [DU(10, 'Q'), DU('0.5', 'R', [1, 'Q']), 1, 0, 0, 0, 10, DU(5, 'R')],
            [DU(10, 'Q'), DU('0.5', 'R', [1, 'Q']), 1, 66, 0, 66, 66, DU(5, 'R')],
            [DU(10, 'Q'), DU('0.5', 'R', [10, 'Q']), 10, 66, 0, 66, 66, DU('0.5', 'R')],
            [DU(1000, 'Q'), DU('0.1', 'R', [1000, 'Q']), 1000, 66, 0, 10, 10, DU('0.1', 'R')],
        ],
        ['type_rate'],
        ids=lambda o: str(o)
    )
    def test_initial_amount(self, qty, price, type_rate, nds_pct, nsp_pct, qty_pct, sum_pct, req_sum):
        res = self._do_test_amounts_calc_from_params(**locals())
        assert req_sum == ut.round(res.initial_amount, 10)

    @pytest.mark.parametrize(
        'qty, price, type_rate, nds_pct, nsp_pct, qty_pct, sum_pct, req_sum',
        [
            [DU(100, 'Q'), DU('0.5', 'R', [1, 'Q']), 1, 0, 0, 0, 0, DU('50', 'R')],
            [DU(100, 'Q'), DU('0.5', 'R', [1, 'Q']), 1, 10, 0, 0, 0, DU('45.45', 'R')],
            [DU(100, 'Q'), DU('0.5', 'R', [1, 'Q']), 1, 0, 10, 0, 0, DU('45.45', 'R')],
            [DU(100, 'Q'), DU('0.5', 'R', [1, 'Q']), 1, 10, 10, 0, 0, DU('41.66', 'R')],
            [DU(100, 'Q'), DU('0.5', 'R', [1, 'Q']), 1, 10, 10, 10, 0, DU('41.66', 'R')],
            [DU(100, 'Q'), DU('0.5', 'R', [1, 'Q']), 1, 10, 10, 0, 10, DU('41.66', 'R')],
            [DU(100, 'Q'), DU('0.5', 'R', [1, 'Q']), 1, 10, 10, 10, 10, DU('41.66', 'R')],
        ],
        ['type_rate'],
        ids=lambda o: str(o)
    )
    def test_initial_amount_wo_tax(self, qty, price, type_rate, nds_pct, nsp_pct, qty_pct, sum_pct, req_sum):
        res = self._do_test_amounts_calc_from_params(**locals())
        assert req_sum == ut.round(res.initial_amount_without_tax, 10)


class TestRowQuantityCalculator(object):
    AmountsCalcTest = calc.create_calc(
        'CalcTest',
        [
            base_params,
            calc_utils.input_param('amount'),
            calc_utils.input_param('price_obj'),
            calc_utils.input_param('forced_qty_pct'),
            calc_utils.input_param('forced_sum_pct'),
            forced_test_discounts,
            calc_funcs.discount_pct,
            calc_funcs.tax_policy_pct,
            calc_funcs.amount_discount_pct,
            calc_funcs.quantity_discount_pct,
            calc_funcs.price_by_piece,
            calc_funcs.adjusted_quantity,
            calc_funcs.quantity_from_amount,
            calc_funcs.amount_without_discount_from_amount,
        ],
        output=['adjusted_quantity', 'quantity', 'amount_without_discount']
    )

    @classmethod
    def _do_test_qty_calc_from_params(cls, amount, price, type_rate, nds_pct, nsp_pct, qty_pct, sum_pct, **kwargs):
        tpp = TaxSimple(nds_pct=DU(nds_pct, '%'), nsp_pct=DU(nsp_pct, '%'))
        price_obj = PriceObject(price=price, type_rate=type_rate, tax_policy_pct=tpp)
        return cls.AmountsCalcTest(amount, price_obj, qty_pct, sum_pct).calc()

    @pytest.mark.parametrize(
        'amount, price, type_rate, nds_pct, nsp_pct, qty_pct, sum_pct, req_qty',
        [
            [DU(100, 'R'), DU('2', 'R', [1, 'Q']), 1, 0, 0, 0, 0, DU(50, 'Q')],
            [DU(100, 'R'), DU('2', 'R', [10, 'Q']), 10, 0, 0, 0, 0, DU(500, 'Q')],
            [DU(100, 'R'), DU('2', 'R', [1, 'Q']), 1, 10, 0, 0, 0, DU(50, 'Q')],
            [DU(100, 'R'), DU('2', 'R', [1, 'Q']), 1, 0, 10, 0, 0, DU(50, 'Q')],
            [DU(100, 'R'), DU('2', 'R', [1, 'Q']), 1, 10, 10, 0, 0, DU(50, 'Q')],
            [DU(100, 'R'), DU('2', 'R', [1, 'Q']), 1, 10, 10, 10, 0, DU(50, 'Q')],
            [DU(100, 'R'), DU('2', 'R', [1, 'Q']), 1, 10, 10, 0, 10, DU('55.555556', 'Q')],
            [DU(100, 'R'), DU('2', 'R', [1, 'Q']), 1, 10, 10, 10, 10, DU('55.555556', 'Q')],
        ],
        ['type_rate'],
        ids=lambda o: str(o)
    )
    def test_quantity(self, amount, price, type_rate, nds_pct, nsp_pct, qty_pct, sum_pct, req_qty):
        res = self._do_test_qty_calc_from_params(**locals())
        assert req_qty == ut.round(res.quantity, 10)

    @pytest.mark.parametrize(
        'amount, price, type_rate, nds_pct, nsp_pct, qty_pct, sum_pct, req_qty',
        [
            [DU(100, 'R'), DU('2', 'R', [1, 'Q']), 1, 0, 0, 0, 0, DU(50, 'Q')],
            [DU(100, 'R'), DU('2', 'R', [10, 'Q']), 10, 0, 0, 0, 0, DU(500, 'Q')],
            [DU(100, 'R'), DU('2', 'R', [1, 'Q']), 1, 10, 0, 0, 0, DU(50, 'Q')],
            [DU(100, 'R'), DU('2', 'R', [1, 'Q']), 1, 0, 10, 0, 0, DU(50, 'Q')],
            [DU(100, 'R'), DU('2', 'R', [1, 'Q']), 1, 10, 10, 0, 0, DU(50, 'Q')],
            [DU(100, 'R'), DU('2', 'R', [1, 'Q']), 1, 10, 10, 10, 0, DU('55.555556', 'Q')],
            [DU(100, 'R'), DU('2', 'R', [1, 'Q']), 1, 10, 10, 0, 10, DU('55.555556', 'Q')],
            [DU(100, 'R'), DU('2', 'R', [1, 'Q']), 1, 10, 10, 10, 10, DU('61.728396', 'Q')],
        ],
        ['type_rate'],
        ids=lambda o: str(o)
    )
    def test_adjusted_qty(self, amount, price, type_rate, nds_pct, nsp_pct, qty_pct, sum_pct, req_qty):
        res = self._do_test_qty_calc_from_params(**locals())
        assert req_qty == ut.round(res.adjusted_quantity, 10)

    @pytest.mark.parametrize(
        'amount, qty_pct, sum_pct, req_sum',
        [
            [DU(100, 'R'), 0, 0, DU(100, 'R')],
            [DU(100, 'R'), 10, 0, DU('111.11', 'R')],
            [DU(100, 'R'), 0, 10, DU('111.11', 'R')],
            [DU(100, 'R'), 10, 10, DU('123.46', 'R')],
        ],
        ids=lambda o: str(o)
    )
    def test_amount_wo_discount(self, amount, qty_pct, sum_pct, req_sum):
        res = self._do_test_qty_calc_from_params(amount, 10, 10, 66, 66, qty_pct, sum_pct)
        assert req_sum == ut.round(res.amount_without_discount, 10)


class TestRowsSetCalculator(object):
    AmountCalcTest = calc.create_calc(
        'CalcTest',
        [
            calc_utils.constant_param('amount_precision', 2),
            calc_utils.constant_param('quantity_precision', 6),
            calc_utils.input_rows(
                'input_rows',
                [
                    'quantity',
                    'price_obj',
                    'forced_qty_pct',
                    'forced_sum_pct',
                ]
            ),
            calc_utils.input_param('taxes_alignment_required'),
            forced_test_discounts,
            calc_funcs.tax_policy_pct,
            calc_funcs.amount_discount_pct,
            calc_funcs.quantity_discount_pct,
            calc_funcs.adjusted_quantity,
            calc_funcs.price_by_piece,
            calc_invoice.invoice_taxes,
            calc_funcs.amount,
            calc_funcs.amount_without_discount,
            calc_funcs.amount_without_tax,
            calc_funcs.nds_pct,
            calc_funcs.nsp_pct,
            calc_funcs.header_amount,
            calc_funcs.header_amount_nds,
            calc_funcs.header_amount_nsp,
            calc_funcs.header_amount_without_tax,
            calc_funcs.header_nds_pct,
            calc_funcs.header_nsp_pct,
            calc_funcs.header_tax_policy_pct,
            calc_utils.aggregate_rows(
                'rows',
                [
                    'amount_nds',
                    'amount_nsp',
                ]
            )
        ],
        output=[
            'rows', 'header_amount', 'header_amount_nds', 'header_amount_nsp',
            'header_amount_without_tax', 'header_nds_pct', 'header_nsp_pct', 'header_tax_policy_pct'
        ]
    )

    def test_amount(self):
        tpp = TaxSimple(0, 0)
        rows = [
            (
                DU('6.666666', 'Q'),
                PriceObject(price=DU('16.66', 'S', [1, 'Q']), type_rate=DU(1, [], 1), tax_policy_pct=tpp),
                0, 0
            ),
            (
                DU('1555', 'Q'),
                PriceObject(price=DU('42', 'S', [100, 'Q']), type_rate=DU(100, [], 100), tax_policy_pct=tpp),
                0, 24
            ),
            (
                DU('71.71', 'Q'),
                PriceObject(price=DU('0.13', 'S', [9, 'Q']), type_rate=DU(9, [], 9), tax_policy_pct=tpp),
                0, 0
            ),
        ]
        res = self.AmountCalcTest(rows, 0).calc()
        assert DU('608.47', 'S') == res.header_amount

    @pytest.mark.parametrize(
        'do_align, total_sum, rows_sums',
        [
            [False, DU('1.68', 'S'), (DU('0.56', 'S'), DU('0.56', 'S'), DU('0.56', 'S'))],
            [True, DU('1.67', 'S'), (DU('0.56', 'S'), DU('0.55', 'S'), DU('0.56', 'S'))],
        ],
        ids=lambda x: str(x)
    )
    def test_amount_nds(self, do_align, total_sum, rows_sums):
        tax = TaxSimple(nds_pct=DU(20, '%'), nsp_pct=0)

        rows = [
            (
                DU('3.333333', 'Q'),
                PriceObject(price=DU('1', 'S', [1, 'Q']), type_rate=DU(1, [], 1), tax_policy_pct=tax),
                0, 0
            ),
            (
                DU('3.333333', 'Q'),
                PriceObject(price=DU('1', 'S', [1, 'Q']), type_rate=DU(1, [], 1), tax_policy_pct=tax),
                0, 0
            ),
            (
                DU('3.333333', 'Q'),
                PriceObject(price=DU('1', 'S', [1, 'Q']), type_rate=DU(1, [], 1), tax_policy_pct=tax),
                0, 0
            )
        ]
        res = self.AmountCalcTest(rows, do_align).calc()
        assert total_sum == res.header_amount_nds
        assert rows_sums == tuple(row.amount_nds for row in res.rows)

    @pytest.mark.parametrize(
        'do_align, total_sum, rows_sums',
        [
            [False, DU('1.68', 'S'), (DU('0.56', 'S'), DU('0.56', 'S'), DU('0.56', 'S'))],
            [True, DU('1.67', 'S'), (DU('0.56', 'S'), DU('0.55', 'S'), DU('0.56', 'S'))],
        ],
        ids=lambda x: str(x)
    )
    def test_amount_nsp(self, do_align, total_sum, rows_sums):
        tax = TaxSimple(nds_pct=0, nsp_pct=DU(20, '%'))

        rows = [
            (
                DU('3.333333', 'Q'),
                PriceObject(price=DU('1', 'S', [1, 'Q']), type_rate=DU(1, [], 1), tax_policy_pct=tax),
                0, 0
            ),
            (
                DU('3.333333', 'Q'),
                PriceObject(price=DU('1', 'S', [1, 'Q']), type_rate=DU(1, [], 1), tax_policy_pct=tax),
                0, 0
            ),
            (
                DU('3.333333', 'Q'),
                PriceObject(price=DU('1', 'S', [1, 'Q']), type_rate=DU(1, [], 1), tax_policy_pct=tax),
                0, 0
            )
        ]
        res = self.AmountCalcTest(rows, do_align).calc()
        assert total_sum == res.header_amount_nsp
        assert rows_sums == tuple(row.amount_nsp for row in res.rows)

    @pytest.mark.parametrize(
        'do_align, total_sum, nds_sum, nsp_sum',
        [
            [False, DU('8.01', 'S'), DU('1.59', 'S'), DU('0.39', 'S')],
            [True, DU('7.99', 'S'), DU('1.60', 'S'), DU('0.40', 'S')],
        ],
        ids=lambda x: str(x)
    )
    def test_all_tax_amounts(self, do_align, total_sum, nds_sum, nsp_sum):
        tax = TaxSimple(nds_pct=DU(20, '%'), nsp_pct=DU(5, '%'))

        rows = [
            (
                DU('3.333333', 'Q'),
                PriceObject(price=DU('1', 'S', [1, 'Q']), type_rate=DU(1, [], 1), tax_policy_pct=tax),
                0, 0
            ),
            (
                DU('3.333333', 'Q'),
                PriceObject(price=DU('1', 'S', [1, 'Q']), type_rate=DU(1, [], 1), tax_policy_pct=tax),
                0, 0
            ),
            (
                DU('3.333333', 'Q'),
                PriceObject(price=DU('1', 'S', [1, 'Q']), type_rate=DU(1, [], 1), tax_policy_pct=tax),
                0, 0
            )
        ]
        res = self.AmountCalcTest(rows, do_align).calc()

        assert total_sum == res.header_amount_without_tax
        assert nds_sum == res.header_amount_nds
        assert nsp_sum == res.header_amount_nsp
        assert res.header_amount == res.header_amount_without_tax + res.header_amount_nds + res.header_amount_nsp

    def test_tax_align_recalc(self):
        tax = TaxSimple(nds_pct=DU(20, '%'), nsp_pct=0)

        rows = [
            (
                DU('3.333333', 'Q'),
                PriceObject(price=DU('1', 'S', [1, 'Q']), type_rate=DU(1, [], 1), tax_policy_pct=tax),
                0, 0
            ),
            (
                DU('3.333333', 'Q'),
                PriceObject(price=DU('1', 'S', [1, 'Q']), type_rate=DU(1, [], 1), tax_policy_pct=tax),
                0, 0
            ),
            (
                DU('3.333333', 'Q'),
                PriceObject(price=DU('1', 'S', [1, 'Q']), type_rate=DU(1, [], 1), tax_policy_pct=tax),
                0, 0
            )
        ]
        res = self.AmountCalcTest(rows, True).calc()

        assert DU('0.55', 'S') == res.rows[1].amount_nds
        assert DU('1.67', 'S') == res.header_amount_nds

    def test_tax_pct(self):
        tax = TaxSimple(nds_pct=DU(20, '%'), nsp_pct=DU(5, '%'))

        rows = [
            (1, PriceObject(price=1, type_rate=DU(1, [], 1), tax_policy_pct=tax), 0, 0),
            (1, PriceObject(price=1, type_rate=DU(1, [], 1), tax_policy_pct=tax), 0, 0),
            (1, PriceObject(price=1, type_rate=DU(1, [], 1), tax_policy_pct=tax), 0, 0)
        ]
        res = self.AmountCalcTest(rows, True).calc()

        assert DU(20, '%') == res.header_nds_pct
        assert DU(5, '%') == res.header_nsp_pct
        assert tax == res.header_tax_policy_pct

    def test_tax_pct_multiple_nds(self):
        tax1 = TaxSimple(nds_pct=DU(20, '%'), nsp_pct=DU(5, '%'))
        tax2 = TaxSimple(nds_pct=DU(18, '%'), nsp_pct=DU(5, '%'))

        rows = [
            (1, PriceObject(price=1, type_rate=DU(1, [], 1), tax_policy_pct=tax1), 0, 0),
            (1, PriceObject(price=1, type_rate=DU(1, [], 1), tax_policy_pct=tax1), 0, 0),
            (1, PriceObject(price=1, type_rate=DU(1, [], 1), tax_policy_pct=tax2), 0, 0)
        ]
        res = self.AmountCalcTest(rows, True).calc()

        assert res.header_nds_pct is None
        assert DU(5, '%') == res.header_nsp_pct
        assert res.header_tax_policy_pct is None

    def test_tax_pct_multiple_nsp(self):
        tax1 = TaxSimple(nds_pct=DU(20, '%'), nsp_pct=DU(5, '%'))
        tax2 = TaxSimple(nds_pct=DU(20, '%'), nsp_pct=DU(6, '%'))

        rows = [
            (1, PriceObject(price=1, type_rate=DU(1, [], 1), tax_policy_pct=tax1), 0, 0),
            (1, PriceObject(price=1, type_rate=DU(1, [], 1), tax_policy_pct=tax1), 0, 0),
            (1, PriceObject(price=1, type_rate=DU(1, [], 1), tax_policy_pct=tax2), 0, 0)
        ]
        res = self.AmountCalcTest(rows, True).calc()

        assert DU(20, '%') == res.header_nds_pct
        assert res.header_nsp_pct is None
        assert res.header_tax_policy_pct is None
