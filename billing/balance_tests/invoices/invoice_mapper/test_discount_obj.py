# -*- coding: utf-8 -*-

from decimal import Decimal as D

import pytest
import hamcrest

from balance import mapper, muzzle_util as ut
from tests import object_builder as ob


@pytest.fixture
def invoice():
    return mapper.Invoice(
        paysys=mapper.Paysys(
            person_category=mapper.PersonCategory(),
        ),
        firm=mapper.Firm(),
        client=mapper.Client(),
    )


@pytest.fixture
def invoice_order(invoice):
    io = mapper.InvoiceOrder(mapper.BasketItem(
        666,
        order=mapper.Order(
            product=mapper.Product(
                unit=mapper.ProductUnit(
                    product_type=mapper.ProductType()
                ),
            )
        )
    ))
    io.invoice = invoice
    return io


@pytest.fixture
def consume(invoice):
    return mapper.Consume(None, invoice, 666, 666, mapper.PriceObject(1, 1), mapper.DiscountObj(), None)


@pytest.fixture
def promo_code():
    return mapper.PromoCode('666')


class TestInvoiceOrder(object):

    @pytest.mark.parametrize(
        'pct, base_pct, io_promo, i_promo, promo_pct, has_promo, discount_obj',
        [
            pytest.param(0, None, False, False, None, False, mapper.DiscountObj(), id='zero'),
            pytest.param(0, None, True, False, None, True, mapper.DiscountObj(None, None), id='zero_wo_base_w_io_promo'),
            pytest.param(0, None, False, True, None, False, mapper.DiscountObj(), id='zero_wo_base_w_i_promo'),
            pytest.param(0, 0, True, False, 0, True, mapper.DiscountObj(), id='zero_w_base_w_io_promo'),
            pytest.param(0, 0, False, True, None, False, mapper.DiscountObj(), id='zero_w_base_w_i_promo'),
            pytest.param(10, None, False, False, None, False, mapper.DiscountObj(10), id='wo_base'),
            pytest.param(10, None, True, False, 666, True, mapper.DiscountObj(None, 666), id='wo_base_w_io_promo'),
            pytest.param(10, None, False, True, 20, False, mapper.DiscountObj(10), id='wo_base_w_i_promo'),
            pytest.param(10, 10, False, False, None, False, mapper.DiscountObj(10), id='w_base'),
            pytest.param(10, 10, True, False, None, True, mapper.DiscountObj(10, None), id='w_base_i_promo'),
            pytest.param(10, 10, True, False, 10, True, mapper.DiscountObj(10, 10), id='w_base_io_promo'),
        ]
    )
    def test_get(self, invoice, invoice_order, promo_code,
                          pct, base_pct, io_promo, i_promo, promo_pct, has_promo, discount_obj):
        invoice_order.discount_pct = pct
        invoice_order.base_discount_pct = base_pct
        invoice_order.promo_code_discount_pct = promo_pct
        if io_promo:
            invoice_order.promo_code = promo_code
        if i_promo:
            invoice.promo_code = promo_code

        if has_promo:
            discount_obj = discount_obj.copy()
            discount_obj.promo_code = promo_code
        assert invoice_order.discount_obj == discount_obj

    @pytest.mark.parametrize(
        'discount_obj, has_promo, pct, base_pct, promo_pct, io_promo',
        [
            pytest.param(mapper.DiscountObj(), False, 0, None, None, False, id='zero'),
            pytest.param(mapper.DiscountObj(10), False, 10, None, None, False, id='base'),
            pytest.param(mapper.DiscountObj(10, 10), True, 19, 10, 10, True, id='promo'),
            pytest.param(mapper.DiscountObj(10, dynamic_pct=10), False, 19, None, None, False, id='dynamic'),
        ]
    )
    def test_set(self, invoice_order, promo_code, discount_obj, has_promo, pct, base_pct, promo_pct, io_promo):
        if has_promo:
            discount_obj = discount_obj.copy()
            discount_obj.promo_code = promo_code

        invoice_order.discount_obj = discount_obj

        hamcrest.assert_that(
            invoice_order,
            hamcrest.has_properties(
                discount_pct=pct,
                base_discount_pct=base_pct,
                promo_code_discount_pct=promo_pct,
                promo_code=promo_code if io_promo else None
            )
        )


class TestConsume(object):
    @pytest.mark.parametrize(
        'pct, static_pct, base_pct, q_promo, i_promo, promo_pct, has_promo, cashback, discount_obj',
        [
            pytest.param(
                0, 0, None, False, False, None, False, None,
                mapper.DiscountObj(),
                id='zero'
            ),
            pytest.param(
                0, 0, None, True, False, None, True, None,
                mapper.DiscountObj(None, None),
                id='zero_wo_base_w_q_promo'
            ),
            pytest.param(
                0, 0, None, False, True, None, False, None,
                mapper.DiscountObj(),
                id='zero_wo_base_w_i_promo'
            ),
            pytest.param(
                0, 0, 0, True, False, 0, True, None,
                mapper.DiscountObj(),
                id='zero_w_base_w_q_promo'
            ),
            pytest.param(
                0, 0, 0, False, True, None, False, None,
                mapper.DiscountObj(),
                id='zero_w_base_w_i_promo'
            ),
            pytest.param(
                10, 10, None, False, False, None, False, None,
                mapper.DiscountObj(10),
                id='wo_base'
            ),
            pytest.param(
                10, 10, None, True, False, 666, True, None,
                mapper.DiscountObj(None, 666),
                id='wo_base_w_q_promo'
            ),
            pytest.param(
                10, 10, None, False, True, 20, False, None,
                mapper.DiscountObj(10, 0),
                id='wo_base_w_i_promo'
            ),
            pytest.param(
                19, 10, None, False, False, None, False, None,
                mapper.DiscountObj(10, dynamic_pct=10),
                id='wo_base_dynamic'
            ),
            pytest.param(
                10, 10, 10, False, False, None, False, None,
                mapper.DiscountObj(10),
                id='w_base'
            ),
            pytest.param(
                10, 10, 10, True, False, None, True, None,
                mapper.DiscountObj(10, None),
                id='w_base_i_promo'
            ),
            pytest.param(
                19, 19, 10, True, False, 10, True, None,
                mapper.DiscountObj(10, 10),
                id='w_base_q_promo'
            ),
            pytest.param(
                19, 10, 10, False, False, None, False, None,
                mapper.DiscountObj(10, dynamic_pct=10),
                id='w_base_dynamic'
            ),
            pytest.param(
                10, 10, 10, False, False, None, False, (10, 5, None),
                mapper.DiscountObj(10, cashback_base=10, cashback_bonus=5),
                id='w_cashback_old'
            ),
            pytest.param(
                10, 10, 10, False, False, None, False, (10, 5, 666),
                mapper.DiscountObj(10, cashback_base=10, cashback_bonus=5, cashback_usage_id=666),
                id='w_cashback'
            ),
            pytest.param(
                10, 10, 10, True, False, 666, True, (10, 5, 666),
                mapper.DiscountObj(10, 666, cashback_base=10, cashback_bonus=5, cashback_usage_id=666),
                id='w_promo_w_cashback'
            ),
        ]
    )
    def test_get(self, invoice, consume, promo_code,
                 pct, static_pct, base_pct, q_promo, i_promo, promo_pct, has_promo, cashback, discount_obj):
        consume.discount_pct = pct
        consume.static_discount_pct = static_pct
        consume.base_discount_pct = base_pct
        consume.promo_code_discount_pct = promo_pct
        if q_promo:
            consume.promo_code = promo_code
        if i_promo:
            invoice.promo_code = promo_code

        if has_promo:
            discount_obj = discount_obj.copy()
            discount_obj.promo_code = promo_code

        if cashback is not None:
            consume.cashback_base, consume.cashback_bonus, consume.cashback_usage_id = cashback

        assert consume.discount_obj == discount_obj

    @pytest.mark.parametrize(
        'discount_obj, has_promo, pct, static_pct, base_pct, promo_pct, io_promo, cashback',
        [
            pytest.param(mapper.DiscountObj(), False, 0, 0, None, None, False, None, id='zero'),
            pytest.param(mapper.DiscountObj(10), False, 10, 10, None, None, False, None, id='base'),
            pytest.param(mapper.DiscountObj(10, 10), True, 19, 19, 10, 10, True, None, id='promo'),
            pytest.param(mapper.DiscountObj(10, dynamic_pct=10), False, 19, 10, None, None, False, None, id='dynamic'),
            pytest.param(
                mapper.DiscountObj(10, cashback_base=9, cashback_bonus=1),
                False, 19, 19, 10, None, False, (9, 1, None),
                id='cashback_old'
            ),
            pytest.param(
                mapper.DiscountObj(10, cashback_base=9, cashback_bonus=1, cashback_usage_id=666),
                False, 19, 19, 10, None, False, (9, 1, 666),
                id='cashback'
            ),
            pytest.param(
                mapper.DiscountObj(10, 10, cashback_base=9, cashback_bonus=1, cashback_usage_id=666),
                True, D('27.1'), D('27.1'), 10, 10, True, (9, 1, 666),
                id='cashback + promo'
            ),
            pytest.param(
                mapper.DiscountObj(10, dynamic_pct=10, cashback_base=9, cashback_bonus=1, cashback_usage_id=666),
                False, D('27.1'), D('19'), 10, None, False, (9, 1, 666),
                id='cashback + dynamic'
            ),
            pytest.param(
                mapper.DiscountObj(10, 10, dynamic_pct=10, cashback_base=9, cashback_bonus=1, cashback_usage_id=666),
                True, D('34.39'), D('27.1'), D('10'), D('10'), True, (9, 1, 666),
                id='cashback + promo + dynamic'
            ),
        ]
    )
    def test_set(self, consume, promo_code, discount_obj, has_promo, pct, static_pct, base_pct, promo_pct, io_promo, cashback):
        if has_promo:
            discount_obj = discount_obj.copy()
            discount_obj.promo_code = promo_code

        consume.discount_obj = discount_obj

        hamcrest.assert_that(
            consume,
            hamcrest.has_properties(
                discount_pct=pct,
                static_discount_pct=static_pct,
                base_discount_pct=base_pct,
                promo_code_discount_pct=promo_pct,
                promo_code=promo_code if io_promo else None,
                cashback_base=cashback[0] if cashback else None,
                cashback_bonus=cashback[1] if cashback else None,
                cashback_usage_id=cashback[2] if cashback else None,
            )
        )


class TestDiscountObj(object):
    @pytest.mark.parametrize(
        'pct, static_pct, has_promo, res_promo, discount_obj',
        [
            pytest.param(10, 10, False, False, mapper.DiscountObj(10), id='base'),
            pytest.param(19, 10, False, False, mapper.DiscountObj(10, dynamic_pct=10), id='dynamic'),
            pytest.param(10, 10, True, True, mapper.DiscountObj(0, 10), id='promo'),
        ]
    )
    def test_from_legacy(self, promo_code, pct, static_pct, has_promo, res_promo, discount_obj):
        if res_promo:
            discount_obj = discount_obj.copy()
            discount_obj.promo_code = promo_code

        assert mapper.DiscountObj.from_legacy(pct, static_pct, promo_code if has_promo else None) == discount_obj

    def test_without_dynamic(self, promo_code):
        base = mapper.DiscountObj(10, 20, promo_code, 30)
        assert base.without_dynamic() == mapper.DiscountObj(10, 20, promo_code)

    def test_with_promo_code(self, promo_code):
        base = mapper.DiscountObj(10, dynamic_pct=30)
        assert base.with_promo_code(20, promo_code) == mapper.DiscountObj(10, 20, promo_code, 30)

    def test_without_promo_code(self, promo_code):
        base = mapper.DiscountObj(10, 20, promo_code, 30)
        assert base.without_promo_code() == mapper.DiscountObj(10, dynamic_pct=30)

    def test_with_base(self, promo_code):
        base = mapper.DiscountObj(10, 20, promo_code, 30)
        assert base.with_base_discount(5) == mapper.DiscountObj(5, 20, promo_code, 30)

    def test_without_base(self, promo_code):
        base = mapper.DiscountObj(10, 20, promo_code, 30)
        assert base.without_base_discount() == mapper.DiscountObj(0, 20, promo_code, 30)

    def test_pct(self, promo_code):
        assert mapper.DiscountObj(10, 20, promo_code, D('33.03')).pct == D('51.78')

    def test_raw_pct(self, promo_code):
        assert mapper.DiscountObj(10, 20, promo_code, D('33.03')).raw_pct == D('51.7816')

    def test_static_pct(self, promo_code):
        assert mapper.DiscountObj(10, D('33.03'), promo_code, 30).static_pct == D('39.73')

    def test_dynamic_pct(self):
        assert mapper.DiscountObj(10, dynamic_pct=D('66.666667')).dynamic_pct == D('66.6667')

    def test_pct_precision(self, promo_code):
        assert mapper.DiscountObj(10, D('22.002'), promo_code, 30).pct == D('50.861')

    def test_with_cashback(self, promo_code):
        base = mapper.DiscountObj(10, 20, promo_code, 30)
        assert base.with_cashback(10, 5, 666) == mapper.DiscountObj(10, 20, promo_code, 30, 10, 5, 666)

    def test_without_cashback(self, promo_code):
        base = mapper.DiscountObj(10, 20, promo_code, 30, 10, 5, 666)
        assert base.without_cashback() == mapper.DiscountObj(10, 20, promo_code, 30)

    @pytest.mark.parametrize(
        'cashback, relation, pct',
        [
            ((D('10'), D('5')), D('0.666667'), D('33.33')),
            ((D('9'), D('1')), D('0.9'), D('10')),
            ((D('0'), D('10')), D('0'), D('100')),
            ((D('10'), D('10')), D('0.5'), D('50')),
            ((D('10'), D('1500')), D('0.006623'), D('99.34')),
        ],
    )
    def test_cashback_relation(self, cashback, relation, pct):
        base, bonus = cashback
        discount_obj = mapper.DiscountObj(cashback_base=base, cashback_bonus=bonus)
        assert ut.round(discount_obj.cashback_relation, 6) == relation
        assert ut.round(discount_obj.cashback_pct, 2) == pct

    def test_apply(self, promo_code):
        """Apply обеспечивает большую точность
        """
        discount_obj = mapper.DiscountObj(0, 50, promo_code, cashback_base=D('1'), cashback_bonus=D('2'))
        assert ut.round(discount_obj.cashback_relation, 6) == D('0.333333')
        assert ut.add_percent(D('100'), -discount_obj.pct) == D('16.67')
        assert ut.round(discount_obj.apply(D('100')), 6) == D('16.666667')
