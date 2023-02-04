import decimal

import pytest

from kkt_srv.cashmachine.calculator import calc_rows
from kkt_srv.cashmachine.global_constants import TaxCalculationMethod
from kkt_srv.cashmachine.kktproto.starrus.kkt_data import TaxTypeData


test_data_rows = [
    {
        'payment_type_type': 'full_prepayment_wo_delivery',
        'price': decimal.Decimal('49.08'),
        'qty': decimal.Decimal('1.970'),
        'tax_type': TaxTypeData.nds_20_120,
        'text': 'Goods_20_1'
    },
    {
        'payment_type_type': 'full_prepayment_wo_delivery',
        'price': decimal.Decimal('2.97'),
        'qty': decimal.Decimal('1.000'),
        'tax_type': TaxTypeData.nds_20_120,
        'text': 'Goods_20_2'
    },
    {
        'payment_type_type': 'full_prepayment_wo_delivery',
        'price': decimal.Decimal('57.08'),
        'qty': decimal.Decimal('1.972'),
        'tax_type': TaxTypeData.nds_10,
        'text': 'Goods_10_1'
    },
    {
        'payment_type_type': 'full_prepayment_wo_delivery',
        'price': decimal.Decimal('2.97'),
        'qty': decimal.Decimal('1.123'),
        'tax_type': TaxTypeData.nds_10,
        'text': 'Goods_10_2'
    }
]


@pytest.mark.parametrize('rows, tax_calc_method, desired_taxes', (
    (test_data_rows[:], TaxCalculationMethod.total, {TaxTypeData.nds_20_120: decimal.Decimal('16.61'), TaxTypeData.nds_10: decimal.Decimal('10.54')}),
    (test_data_rows[:], TaxCalculationMethod.position, {TaxTypeData.nds_20_120: decimal.Decimal('16.62'), TaxTypeData.nds_10: decimal.Decimal('10.53')}),
))
def test_calc_rows(rows, tax_calc_method, desired_taxes):
    rows, taxes = calc_rows(rows, tax_calc_method=tax_calc_method)
    print(taxes)
    assert taxes == desired_taxes
