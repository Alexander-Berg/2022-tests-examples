# -*- coding: utf-8 -*-
import datetime
from decimal import Decimal as D

import pytest
import mock
from hamcrest import assert_that
import hamcrest

from balance import mapper, exc
from balance import constants as cst
from balance.actions.promocodes import reserve_promo_code

from tests import object_builder as ob
from tests.balance_tests.promocode.common import (
    NOW,
    create_promocode,
    create_order,
    create_invoice,
)

pytestmark = [
    pytest.mark.promo_code,
]


class BasePromocodeCalcTest(object):
    def do_test_turn_on(self, invoice, promocode, req_qty, req_sum, req_pct, req_base_pct, req_promo_pct):
        invoice.on_turn_on(None, 666, manual=True)
        invoice.session.expire(invoice)
        consume, = invoice.consumes

        assert invoice.promo_code == promocode
        assert_that(
            consume,
            hamcrest.has_properties(
                discount_pct=req_pct,
                discount_obj=mapper.DiscountObj(
                    req_base_pct,
                    req_promo_pct,
                    promocode,
                ),
                current_qty=req_qty,
                current_sum=req_sum,
            )
        )

    def do_test_create(self,
                       session, order, promocode,
                       qty, discount_pct, adjust_qty,
                       req_qty, req_sum, req_pct, req_base_pct, req_promo_pct,
                       paysys_cc='pc'):
        paysys = session.query(mapper.Paysys).getone(firm_id=1, cc=paysys_cc)
        person = ob.PersonBuilder(client=order.client, type=paysys.category).build(session).obj

        invoice = create_invoice(session, qty, order.client, order, paysys, person, discount_pct, adjust_qty)
        invoice.create_receipt(invoice.effective_sum)
        invoice.on_turn_on(None, 666, manual=True)
        io, = invoice.invoice_orders
        co, = invoice.consumes

        assert_that(
            invoice,
            hamcrest.has_properties(
                promo_code=promocode if req_pct is not None else None,
                total_sum=req_sum,
            )
        )
        assert_that(
            io,
            hamcrest.has_properties(
                discount_pct=req_pct if req_pct is not None else D('0'),
                discount_obj=mapper.DiscountObj(req_base_pct, req_promo_pct, promocode if req_pct is not None else None),
                amount=req_sum,
                quantity=req_qty,
                initial_quantity=qty,
            )
        )
        assert_that(
            co,
            hamcrest.has_properties(
                discount_pct=req_pct if req_pct is not None else D('0'),
                discount_obj=mapper.DiscountObj(req_base_pct, req_promo_pct, promocode if req_pct is not None else None),
                current_sum=req_sum,
                current_qty=req_qty,
            )
        )


class TestInvoiceLegacyPromocode(BasePromocodeCalcTest):
    @pytest.fixture
    def promocode(self, request, session):
        params = getattr(request, 'param', {})
        bonus_rub = params.get('bonus_rub', D('254.24'))
        bonus_fish = params.get('bonus_fish', 10)
        service_ids = params.get('service_ids')

        calc_params = {
            u"multicurrency_bonuses": {"RUB": {"bonus1": bonus_rub, "bonus2": bonus_rub}},
            u"bonus1": bonus_fish,
            u"bonus2": bonus_fish
        }
        if service_ids:
            calc_params['service_ids'] = list(service_ids)
        pc = create_promocode(session, dict(calc_params=calc_params, calc_class_name='LegacyPromoCodeGroup'))

        for arg in ['minimal_amounts', 'start_dt', 'end_dt']:
            val = params.get(arg)
            if val is not None:
                setattr(pc.group, arg, val)
        session.flush()

        return pc

    @pytest.mark.parametrize(
        'order, invoice, promocode, req_pct, req_base_pct, req_promo_pct, req_qty, req_sum',
        [
            pytest.param(
                cst.DIRECT_PRODUCT_RUB_ID, {'paysys_cc': 'ur', 'qty': 1000},
                {'bonus_rub': 250, 'bonus_fish': 10},
                D('23.08'), 0, D('23.08'), D('1300.052'), D('1000'),
                id='rur_resident'
            ),
            pytest.param(
                cst.DIRECT_PRODUCT_ID, {'paysys_cc': 'ur', 'qty': 10},
                {'bonus_rub': D('254.24'), 'bonus_fish': 10},
                D('50'), 0, D('50'), D('20'), D('300'),
                id='fish_resident'
            ),
            pytest.param(
                cst.DIRECT_PRODUCT_RUB_ID, {'paysys_cc': 'rur_wo_nds', 'qty': 1000},
                {'bonus_rub': D('254.24'), 'bonus_fish': 10},
                D('20.27'), 0, D('20.27'), D('1254.2330'), D('1000'),
                id='rur_nonresident'
            ),
            pytest.param(
                cst.DIRECT_PRODUCT_ID, {'paysys_cc': 'rur_wo_nds', 'qty': 10},
                {'bonus_rub': 250, 'bonus_fish': 10},
                D('50'), 0, D('50'), D('20'), D('250'),
                id='fish_nonresident'
            ),
            pytest.param(
                cst.DIRECT_PRODUCT_ID, {'paysys_cc': 'ur', 'qty': 30, 'discount_pct': 10, 'adjust_qty': False},
                {'bonus_rub': 250, 'bonus_fish': 10},
                D('32.5'), 10, 25, D('40'), D('810'),
                id='w_discount_wo_adj_qty'

            ),
            pytest.param(
                cst.DIRECT_PRODUCT_ID, {'paysys_cc': 'ur', 'qty': 30, 'discount_pct': 10, 'adjust_qty': True},
                {'bonus_rub': 250, 'bonus_fish': 10},
                D('32.5'), 10, 25, D('44.444444'), D('900'),
                id='w_discount_w_adj_qty'
            ),
        ],
        ['order', 'invoice', 'promocode'],
    )
    @pytest.mark.usefixtures('reservation')
    def test_turn_on(self, invoice, promocode, req_pct, req_base_pct, req_promo_pct, req_qty, req_sum):
        self.do_test_turn_on(invoice, promocode, req_qty, req_sum, req_pct, req_base_pct, req_promo_pct)

    @pytest.mark.parametrize(
        'order, invoice, promocode',
        [
            pytest.param(cst.DIRECT_PRODUCT_RUB_ID, {'qty': 42}, {'minimal_amounts': {'RUB': 666}}, id='^_^'),
        ],
        True
    )
    @pytest.mark.usefixtures('reservation')
    def test_minimal_qty(self, invoice):
        invoice.on_turn_on(None, 666, manual=True)

        assert invoice.promo_code is None
        assert 1 == len(invoice.consumes)
        consume, = invoice.consumes

        assert_that(
            consume,
            hamcrest.has_properties(
                current_qty=42,
                current_sum=42,
                discount_pct=0
            )
        )

    @pytest.mark.parametrize(
        'order, invoice, promocode',
        [
            pytest.param(
                cst.DIRECT_PRODUCT_RUB_ID, {'qty': 666},
                {'start_dt': NOW - datetime.timedelta(666), 'end_dt': NOW - datetime.timedelta(66)},
                id='^_^'
            ),
        ],
        True
    )
    @pytest.mark.usefixtures('reservation')
    def test_wrong_date(self, invoice):
        invoice.on_turn_on(None, 666, manual=True)

        assert invoice.promo_code is None
        assert 1 == len(invoice.consumes)
        consume, = invoice.consumes

        assert_that(
            consume,
            hamcrest.has_properties(
                current_qty=666,
                current_sum=666,
                discount_pct=0
            )
        )

    @pytest.mark.parametrize(
        'promocode',
        [pytest.param({'service_ids': [cst.ServiceId.DIRECT]}, id='^_^')],
        True
    )
    @pytest.mark.usefixtures('reservation')
    def test_multiple_services(self, session, client, promocode):
        orders = [
            ob.OrderBuilder.construct(
                session,
                client=client,
                product_id=cst.DIRECT_PRODUCT_RUB_ID,
                service_id=service_id
            )
            for service_id in [cst.ServiceId.DIRECT, cst.ServiceId.MKB]
        ]
        invoice = create_invoice(session, 600, client, orders)
        invoice.create_receipt(invoice.effective_sum)

        invoice.on_turn_on(None, 1200, manual=True)
        hamcrest.assert_that(
            invoice,
            hamcrest.has_properties(
                promo_code=promocode,
                consumes=hamcrest.contains_inanyorder(
                    hamcrest.has_properties(
                        order=hamcrest.has_properties(service_id=cst.ServiceId.DIRECT),
                        discount_obj=mapper.DiscountObj(0, D('33.71'), promocode),
                        current_sum=600,
                        current_qty=D('905.1139'),
                    ),
                    hamcrest.has_properties(
                        order=hamcrest.has_properties(service_id=cst.ServiceId.MKB),
                        discount_obj=mapper.DiscountObj(),
                        current_sum=600,
                        current_qty=600,
                    )
                )
            )
        )


class TestInvoiceFixedDiscountPromocode(BasePromocodeCalcTest):
    @pytest.fixture
    def promocode(self, request, session):
        params = getattr(request, 'param', {})
        calc_params = {
            'discount_pct': params.get('pct', 66),
            'adjust_quantity': params.get('adjust_qty', False),
            'apply_on_create': params.get('on_create', False),
        }
        return create_promocode(session, dict(
            calc_class_name='FixedDiscountPromoCodeGroup',
            calc_params=calc_params,
            product_ids=[cst.DIRECT_PRODUCT_RUB_ID],
        ))

    @pytest.mark.parametrize(
        'order, invoice, promocode, req_qty, req_sum, req_pct, req_base_pct, req_promo_pct',
        [
            pytest.param(
                cst.DIRECT_PRODUCT_RUB_ID, {'qty': 666},
                {'pct': 10, 'adjust_qty': True, 'on_create': False},
                D('740'), D('666'), 10, 0, 10,
                id='w_adjust_qty_wo_discount'
            ),
            pytest.param(
                cst.DIRECT_PRODUCT_RUB_ID, {'qty': 666},
                {'pct': 10, 'adjust_qty': False, 'on_create': False},
                D('740'), D('666'), 10, 0, 10,
                id='wo_adjust_qty_wo_discount'
            ),
            pytest.param(
                cst.DIRECT_PRODUCT_RUB_ID,
                {'qty': 666, 'discount_pct': 10, 'adjust_qty': False},
                {'pct': 10, 'adjust_qty': True, 'on_create': False},
                D('740'), D('599.4'), 19, 10, 10,
                id='w_discount_wo_adj'
            ),
            pytest.param(
                cst.DIRECT_PRODUCT_RUB_ID,
                {'qty': 666, 'discount_pct': 10, 'adjust_qty': True},
                {'pct': 10, 'adjust_qty': True, 'on_create': False},
                D('822.2222'), D('666'), 19, 10, 10,
                id='w_discount_w_adj'
            ),
        ],
        ['order', 'invoice', 'promocode'],
    )
    @pytest.mark.usefixtures('reservation')
    def test_turn_on(self, invoice, promocode, req_qty, req_sum, req_pct, req_base_pct, req_promo_pct):
        self.do_test_turn_on(invoice, promocode, req_qty, req_sum, req_pct, req_base_pct, req_promo_pct)

    @pytest.mark.parametrize(
        'order, promocode, qty, discount_pct, adjust_qty, req_qty, req_sum, req_pct, req_base_pct, req_promo_pct',
        [
            pytest.param(
                cst.DIRECT_PRODUCT_RUB_ID,
                {'pct': 10, 'adjust_qty': True, 'on_create': True},
                D('666'), 0, False,
                D('740'), D('666'), 10, 0, 10,
                id='w_adjust_qty_wo_discount',
            ),
            pytest.param(
                cst.DIRECT_PRODUCT_RUB_ID,
                {'pct': 10, 'adjust_qty': False, 'on_create': True},
                D('666'), 0, False,
                D('666'), D('599.4'), 10, 0, 10,
                id='wo_adjust_qty_wo_discount'
            ),
            pytest.param(
                cst.DIRECT_PRODUCT_RUB_ID,
                {'pct': 10, 'adjust_qty': True, 'on_create': True},
                D('666'), 10, False,
                D('740'), D('599.4'), 19, 10, 10,
                id='w_adjust_qty_w_discount'
            ),
            pytest.param(
                cst.DIRECT_PRODUCT_RUB_ID,
                {'pct': 10, 'adjust_qty': False, 'on_create': True},
                D('666'), 10, False,
                D('666'), D('539.46'), 19, 10, 10,
                id='wo_adjust_qty_w_discount'
            ),
            pytest.param(
                cst.DIRECT_PRODUCT_RUB_ID,
                {'pct': 10, 'adjust_qty': True, 'on_create': True},
                D('666'), 10, True,
                D('822.2222'), D('666'), 19, 10, 10,
                id='w_adjust_qty_w_discount_adj'
            ),
            pytest.param(
                cst.DIRECT_PRODUCT_RUB_ID,
                {'pct': 10, 'adjust_qty': False, 'on_create': True},
                D('666'), 10, True,
                D('740'), D('599.4'), 19, 10, 10,
                id='wo_adjust_qty_w_discount_adj'
            ),
        ],
        ['order', 'promocode'],
    )
    @pytest.mark.usefixtures('reservation')
    def test_create(self, session, order, promocode, qty, discount_pct, adjust_qty,
                    req_qty, req_sum, req_pct, req_base_pct, req_promo_pct):
        self.do_test_create(
            session, order, promocode,
            qty, discount_pct, adjust_qty,
            req_qty, req_sum, req_pct, req_base_pct, req_promo_pct,
        )

    @pytest.mark.parametrize(
        'order, promocode',
        [
            pytest.param(
                cst.DIRECT_PRODUCT_ID,
                {'pct': 10, 'adjust_qty': False, 'on_create': True},
                id='invalid_product_id'
            ),
        ],
        ['order', 'promocode'],
    )
    @pytest.mark.parametrize(
        'func',
        ['create', 'turn_on'],
    )
    @pytest.mark.usefixtures('reservation')
    def test_product_ids(self, session, order, promocode, func):
        if func == 'create':
            self.do_test_create(session, order, None, D('666'), 10, True, D('740'), D('19980'), D('10'), D('10'), 0)
        else:
            invoice = create_invoice(session, 666, order.client, [order], discount_pct=10, adjust_qty=True)
            invoice.create_receipt(invoice.effective_sum)
            self.do_test_turn_on(invoice, None, D('740.000000'), D('19980.00'), D('10'), D('10'), 0)

    @pytest.mark.parametrize(
        'promocode',
        [pytest.param({'pct': 10, 'adjust_qty': True, 'on_create': True}, id='^_^')],
        True
    )
    @pytest.mark.usefixtures('reservation')
    def test_create_multiple_rows(self, session, client, promocode):
        order1 = create_order(session, client, cst.DIRECT_PRODUCT_RUB_ID)
        order2 = create_order(session, client, cst.DIRECT_PRODUCT_RUB_ID)

        invoice = create_invoice(session, [222, 444], client, [order1, order2])
        invoice.create_receipt(invoice.effective_sum)
        invoice.on_turn_on(None, 666, manual=True)

        assert promocode == invoice.promo_code
        assert 666 == invoice.total_sum
        assert 666 == invoice.consume_sum

        assert {222, 444} == {io.amount for io in invoice.invoice_orders}
        assert {D('246.6667'), D('493.3333')} == {io.quantity for io in invoice.invoice_orders}
        assert {10} == {io.discount_pct for io in invoice.invoice_orders}
        assert {mapper.DiscountObj(0, 10, promocode)} == {io.discount_obj for io in invoice.invoice_orders}

        assert {222, 444} == {q.current_sum for q in invoice.consumes}
        assert {D('246.6667'), D('493.3333')} == {q.current_qty for q in invoice.consumes}
        assert {10} == {q.discount_pct for q in invoice.consumes}
        assert {mapper.DiscountObj(0, 10, promocode)} == {q.discount_obj for q in invoice.consumes}

    @pytest.mark.parametrize(
        'promocode',
        [pytest.param({'pct': 10, 'on_create': True, 'service_ids': [cst.ServiceId.DIRECT]}, id='^_^')],
        True
    )
    @pytest.mark.usefixtures('reservation')
    def test_create_multiple_services(self, session, client, promocode):
        orders = [
            ob.OrderBuilder.construct(
                session,
                client=client,
                product_id=cst.DIRECT_PRODUCT_RUB_ID,
                service_id=service_id
            )
            for service_id in [cst.ServiceId.DIRECT, cst.ServiceId.MKB]
        ]
        invoice = create_invoice(session, 100, client, orders)

        hamcrest.assert_that(
            invoice,
            hamcrest.has_properties(
                promo_code=promocode,
                invoice_orders=hamcrest.contains_inanyorder(
                    hamcrest.has_properties(
                        order=hamcrest.has_properties(service_id=cst.ServiceId.DIRECT),
                        discount_obj=mapper.DiscountObj(0, 10, promocode),
                        amount=90,
                        quantity=100,
                    ),
                    hamcrest.has_properties(
                        order=hamcrest.has_properties(service_id=cst.ServiceId.MKB),
                        discount_obj=mapper.DiscountObj(),
                        amount=100,
                        quantity=100,
                    )
                )
            )
        )

    @pytest.mark.parametrize(
        'promocode',
        [pytest.param({'pct': 10, 'on_create': False, 'service_ids': [cst.ServiceId.DIRECT]}, id='^_^')],
        True
    )
    @pytest.mark.usefixtures('reservation')
    def test_turn_on_multiple_services(self, session, client, promocode):
        orders = [
            ob.OrderBuilder.construct(
                session,
                client=client,
                product_id=cst.DIRECT_PRODUCT_RUB_ID,
                service_id=service_id
            )
            for service_id in [cst.ServiceId.DIRECT, cst.ServiceId.MKB]
        ]
        invoice = create_invoice(session, 100, client, orders)
        invoice.create_receipt(invoice.effective_sum)
        invoice.on_turn_on(None, 200, manual=True)

        hamcrest.assert_that(
            invoice,
            hamcrest.has_properties(
                promo_code=promocode,
                consumes=hamcrest.contains_inanyorder(
                    hamcrest.has_properties(
                        order=hamcrest.has_properties(service_id=cst.ServiceId.DIRECT),
                        discount_obj=mapper.DiscountObj(0, 10, promocode),
                        current_sum=100,
                        current_qty=D('111.1111'),
                    ),
                    hamcrest.has_properties(
                        order=hamcrest.has_properties(service_id=cst.ServiceId.MKB),
                        discount_obj=mapper.DiscountObj(),
                        current_sum=100,
                        current_qty=100,
                    )
                )
            )
        )

    @pytest.mark.parametrize(
        'promocode',
        [
            pytest.param({'pct': 10, 'adjust_qty': True, 'on_create': True}, id='adjust_qty'),
            pytest.param({'pct': 10, 'adjust_qty': False, 'on_create': True}, id='adjust_sum'),
        ],
        True
    )
    @pytest.mark.usefixtures('reservation')
    def test_create_yamoney_fastpay(self, session, client, promocode):
        order = create_order(session, client, cst.DIRECT_PRODUCT_RUB_ID)

        request = ob.RequestBuilder.construct(
            session,
            basket=ob.BasketBuilder(
                client=client,
                rows=[ob.BasketItemBuilder(order=order, quantity=100)]
            )
        )
        invoice = ob.InvoiceBuilder.construct(
            session,
            request=request,
            paysys_id=1000,
            fast_payment=2
        )

        assert invoice.promo_code is None


class TestInvoiceFixedSumBonusPromocode(BasePromocodeCalcTest):
    @pytest.fixture
    def promocode(self, request, session):
        params = getattr(request, 'param', {})
        calc_params = {
            'currency_bonuses': params.get('bonuses', {'RUB': 100}),
            'adjust_quantity': params.get('adjust_qty', False),
            'apply_on_create': params.get('on_create', False),
        }
        reference_currency = params.get('reference_currency')
        if reference_currency:
            calc_params['reference_currency'] = reference_currency

        return create_promocode(session, dict(
            calc_class_name='FixedSumBonusPromoCodeGroup',
            calc_params=calc_params
        ))

    @pytest.mark.parametrize(
        'order, invoice, promocode, req_qty, req_sum, req_pct, req_base_pct, req_promo_pct',
        [
            pytest.param(
                cst.DIRECT_PRODUCT_RUB_ID, {'qty': 100},
                {'bonuses': {'RUB': D('83.33')}, 'adjust_qty': True, 'on_create': False},
                D('199.9960'), D('100'), D('49.9990'), 0, D('49.9990'),
                id='w_adjust_qty_wo_discount'
            ),
            pytest.param(
                cst.DIRECT_PRODUCT_RUB_ID, {'qty': 100},
                {'bonuses': {'RUB': D('83.33')}, 'adjust_qty': False, 'on_create': False},
                D('199.9960'), D('100'), D('49.9990'), 0, D('49.9990'),
                id='wo_adjust_qty_wo_discount'
            ),
            pytest.param(
                cst.DIRECT_PRODUCT_RUB_ID,
                {'qty': 100, 'discount_pct': 10, 'adjust_qty': False},
                {'bonuses': {'RUB': D('83.33')}, 'adjust_qty': True, 'on_create': False},
                D('199.9960'), D('90'), D('54.9991'), 10, D('49.9990'),
                id='w_discount_wo_adj_qty'
            ),
            pytest.param(
                cst.DIRECT_PRODUCT_RUB_ID,
                {'qty': 100, 'discount_pct': 10, 'adjust_qty': True},
                {'bonuses': {'RUB': D('83.33')}, 'adjust_qty': True, 'on_create': False},
                D('222.2178'), D('100'), D('54.9991'), 10, D('49.9990'),
                id='w_discount_w_adj_qty'
            ),
            pytest.param(
                cst.DIRECT_PRODUCT_ID, {'qty': 10},
                {'bonuses': {'RUB': 25}, 'adjust_qty': True, 'on_create': False},
                D('10.999999'), D('300'), D('9.0909'), 0, D('9.0909'),
                id='fish'
            ),
            pytest.param(
                cst.DIRECT_PRODUCT_RUB_ID, {'qty': 100},
                {'bonuses': {'USD': 1}, 'adjust_qty': True, 'on_create': False, 'reference_currency': 'USD'},
                D('184.0001'), D('100'), D('45.6522'), 0, D('45.6522'),
                id='cross_rate'
            ),
            pytest.param(
                cst.DIRECT_PRODUCT_RUB_ID, {'qty': 100, 'paysys_cc': 'rur_wo_nds'},
                {'bonuses': {'RUB': D('83.33')}, 'adjust_qty': True, 'on_create': False},
                D('183.3302'), D('100'), D('45.4536'), 0, D('45.4536'),
                id='rur_nonres'
            ),
            pytest.param(
                cst.DIRECT_PRODUCT_ID, {'qty': 10, 'paysys_cc': 'rur_wo_nds'},
                {'bonuses': {'RUB': D('83.33')}, 'adjust_qty': True, 'on_create': False},
                D('13.333191'), D('250'), D('24.9992'), 0, D('24.9992'),
                id='fish_nonres'
            ),
        ],
        ['order', 'invoice', 'promocode'],
    )
    @pytest.mark.usefixtures('reservation')
    def test_turn_on(self, invoice, promocode, req_qty, req_sum, req_pct, req_base_pct, req_promo_pct):
        self.do_test_turn_on(invoice, promocode, req_qty, req_sum, req_pct, req_base_pct, req_promo_pct)

    @pytest.mark.parametrize(
        'order, paysys_cc, promocode, qty, discount_pct, adjust_qty, req_qty, req_sum,'
        'req_pct, req_base_pct, req_promo_pct',
        [
            pytest.param(
                cst.DIRECT_PRODUCT_RUB_ID, 'ur',
                {'bonuses': {'RUB': D('83.33')}, 'adjust_qty': True, 'on_create': True},
                D('100'), 0, False,
                D('199.996'), D('100'), D('49.9990'), 0, D('49.9990'),
                id='rub_w_adj_qty'
            ),
            pytest.param(
                cst.DIRECT_PRODUCT_RUB_ID, 'ur',
                {'bonuses': {'RUB': D('83.33')}, 'adjust_qty': False, 'on_create': True},
                D('150'), 0, False,
                D('150'), D('50.00'), D('66.664'), 0, D('66.664'),
                id='rub_wo_adj_qty'
            ),
            pytest.param(
                cst.DIRECT_PRODUCT_ID, 'ur',
                {'bonuses': {'RUB': 25}, 'adjust_qty': True, 'on_create': True},
                D('10'), 0, False,
                D('10.999999'), D('300'), D('9.0909'), 0, D('9.0909'),
                id='fish_w_adj_qty'
            ),
            pytest.param(
                cst.DIRECT_PRODUCT_ID, 'ur',
                {'bonuses': {'RUB': 25}, 'adjust_qty': False, 'on_create': True},
                D('10'), 0, False,
                D('10'), D('270'), D('10'), 0, 10,
                id='fish_wo_adj_qty'
            ),
            pytest.param(
                cst.DIRECT_PRODUCT_ID, 'ur',
                {'bonuses': {'RUB': 25}, 'adjust_qty': False, 'on_create': True},
                D('10'), 10, False,
                D('10'), D('243'), D('19'), 10, 10,
                id='wo_adj_qty_w_discount_wo_adj'
            ),
            pytest.param(
                cst.DIRECT_PRODUCT_ID, 'ur',
                {'bonuses': {'RUB': 25}, 'adjust_qty': False, 'on_create': True},
                D('10'), 10, True,
                D('11.111111'), D('270'), D('19'), 10, 10,
                id='wo_adj_qty_w_discount_w_adj'
            ),
            pytest.param(
                cst.DIRECT_PRODUCT_ID, 'ur',
                {'bonuses': {'RUB': 25}, 'adjust_qty': True, 'on_create': True},
                D('10'), 10, False,
                D('10.999999'), D('270'), D('18.1818'), 10, D('9.0909'),
                id='w_adj_qty_w_discount_wo_adj'
            ),
            pytest.param(
                cst.DIRECT_PRODUCT_ID, 'ur',
                {'bonuses': {'RUB': 25}, 'adjust_qty': True, 'on_create': True},
                D('10'), 10, True,
                D('12.22222'), D('300'), D('18.1818'), 10, D('9.0909'),
                id='w_adj_qty_w_discount_w_adj'
            ),
            pytest.param(
                cst.DIRECT_PRODUCT_RUB_ID, 'ur',
                {'bonuses': {'USD': 1}, 'adjust_qty': True, 'on_create': True, 'reference_currency': 'USD'},
                D('666'), 0, False,
                D('750'), D('666'), D('11.2'), 0, D('11.2'),
                id='cross_rate'
            ),
            pytest.param(
                cst.DIRECT_PRODUCT_ID, 'rur_wo_nds',
                {'bonuses': {'RUB': D('66.66')}, 'adjust_qty': True, 'on_create': True},
                D('10'), 0, False,
                D('12.666405'), D('250'), D('21.0510'), 0, D('21.0510'),
                id='nonres_w_adj_qty'
            ),
            pytest.param(
                cst.DIRECT_PRODUCT_ID, 'rur_wo_nds',
                {'bonuses': {'RUB': D('66.66')}, 'adjust_qty': False, 'on_create': True},
                D('10'), 0, False,
                D('10'), D('183.34'), D('26.664'), 0, D('26.664'),
                id='nonres_wo_adj_qty'
            ),
        ],
        ['order', 'promocode'],
    )
    @pytest.mark.usefixtures('reservation')
    def test_create(self,
                    session, order, paysys_cc, promocode,
                    qty, discount_pct, adjust_qty,
                    req_qty, req_sum, req_pct, req_base_pct, req_promo_pct):
        self.do_test_create(
            session, order, promocode,
            qty, discount_pct, adjust_qty,
            req_qty, req_sum, req_pct, req_base_pct, req_promo_pct, paysys_cc,
        )

    @pytest.mark.parametrize(
        'promocode, qtys, req_qtys, req_sums, req_pct, req_base_pct, req_promo_pct',
        [
            pytest.param(
                {'bonuses': {'RUB': 55}, 'adjust_qty': False, 'on_create': True}, [222, 444],
                {D('222'), D('444')}, {200, 400}, D('9.9099'), 0, D('9.9099'),
                id='wo_adjust_qty'
            ),
            pytest.param(
                {'bonuses': {'RUB': 55}, 'adjust_qty': True, 'on_create': True}, [222, 444],
                {D('244'), D('488')}, {222, 444}, D('9.0164'), 0, D('9.0164'),
                id='w_adjust_qty'
            ),
        ],
        ['promocode'],
    )
    @pytest.mark.usefixtures('reservation')
    def test_create_multiple_rows(self, session, client, promocode, qtys,
                                  req_qtys, req_sums, req_pct, req_base_pct, req_promo_pct):
        order1 = create_order(session, client, cst.DIRECT_PRODUCT_RUB_ID)
        order2 = create_order(session, client, cst.DIRECT_PRODUCT_RUB_ID)

        invoice = create_invoice(session, qtys, client, [order1, order2])
        invoice.create_receipt(invoice.effective_sum)
        invoice.on_turn_on(None, 6666, manual=True)

        assert promocode == invoice.promo_code
        assert sum(req_sums) == invoice.total_sum
        assert sum(req_sums) == invoice.consume_sum

        req_discount_obj = mapper.DiscountObj(req_base_pct, req_promo_pct, promocode)
        assert req_sums == {io.amount for io in invoice.invoice_orders}
        assert req_qtys == {io.quantity for io in invoice.invoice_orders}
        assert {req_pct} == {io.discount_pct for io in invoice.invoice_orders}
        assert {req_discount_obj} == {io.discount_obj for io in invoice.invoice_orders}

        assert req_sums == {q.current_sum for q in invoice.consumes}
        assert req_qtys == {q.current_qty for q in invoice.consumes}
        assert {req_pct} == {q.discount_pct for q in invoice.consumes}
        assert {req_discount_obj} == {q.discount_obj for q in invoice.invoice_orders}

    @pytest.mark.parametrize(
        'promocode',
        [pytest.param({'on_create': True, 'service_ids': [cst.ServiceId.DIRECT]}, id='^_^')],
        True
    )
    @pytest.mark.usefixtures('reservation')
    def test_create_multiple_services(self, session, client, promocode):
        orders = [
            ob.OrderBuilder.construct(
                session,
                client=client,
                product_id=cst.DIRECT_PRODUCT_RUB_ID,
                service_id=service_id
            )
            for service_id in [cst.ServiceId.DIRECT, cst.ServiceId.MKB, cst.ServiceId.DIRECT]
        ]
        invoice = create_invoice(session, [150, 200, 250], client, orders)

        hamcrest.assert_that(
            invoice,
            hamcrest.has_properties(
                promo_code=promocode,
                invoice_orders=hamcrest.contains_inanyorder(
                    hamcrest.has_properties(
                        order=hamcrest.has_properties(service_id=cst.ServiceId.DIRECT),
                        discount_obj=mapper.DiscountObj(0, 30, promocode),
                        amount=105,
                        quantity=150,
                    ),
                    hamcrest.has_properties(
                        order=hamcrest.has_properties(service_id=cst.ServiceId.DIRECT),
                        discount_obj=mapper.DiscountObj(0, 30, promocode),
                        amount=175,
                        quantity=250,
                    ),
                    hamcrest.has_properties(
                        order=hamcrest.has_properties(service_id=cst.ServiceId.MKB),
                        discount_obj=mapper.DiscountObj(),
                        amount=200,
                        quantity=200,
                    )
                )
            )
        )

    @pytest.mark.parametrize(
        'promocode',
        [pytest.param({'on_create': False, 'service_ids': [cst.ServiceId.DIRECT]}, id='^_^')],
        True
    )
    @pytest.mark.usefixtures('reservation')
    def test_turn_on_multiple_services(self, session, client, promocode):
        orders = [
            ob.OrderBuilder.construct(
                session,
                client=client,
                product_id=cst.DIRECT_PRODUCT_RUB_ID,
                service_id=service_id
            )
            for service_id in [cst.ServiceId.DIRECT, cst.ServiceId.DIRECT, cst.ServiceId.MKB]
        ]
        invoice = create_invoice(session, [150, 250, 200], client, orders)
        invoice.create_receipt(invoice.effective_sum)
        invoice.on_turn_on(None, invoice.effective_sum, manual=True)

        hamcrest.assert_that(
            invoice,
            hamcrest.has_properties(
                promo_code=promocode,
                consumes=hamcrest.contains_inanyorder(
                    hamcrest.has_properties(
                        order=hamcrest.has_properties(service_id=cst.ServiceId.DIRECT),
                        discount_obj=mapper.DiscountObj(0, D('23.0769'), promocode),
                        current_sum=150,
                        current_qty=D('194.9999'),
                    ),
                    hamcrest.has_properties(
                        order=hamcrest.has_properties(service_id=cst.ServiceId.DIRECT),
                        discount_obj=mapper.DiscountObj(0, D('23.0769'), promocode),
                        current_sum=250,
                        current_qty=D('324.9999'),
                    ),
                    hamcrest.has_properties(
                        order=hamcrest.has_properties(service_id=cst.ServiceId.MKB),
                        discount_obj=mapper.DiscountObj(),
                        current_sum=200,
                        current_qty=200,
                    )
                )
            )
        )


    @pytest.mark.parametrize(
        'flag_value, req_qty, req_sum, req_pct, req_base_pct, req_promo_pct',
        [
            [1,    D('600000'), D('300'), D('99.95'), 0, D('99.95')],
            [0,    D('582524.2718'), D('300'), D('99.9485'), 0, D('99.9485')],
            [None, D('582524.2718'), D('300'), D('99.9485'), 0, D('99.9485')]
        ]
    )
    @pytest.mark.parametrize(
        'order, invoice, promocode',
        [
            pytest.param(
                cst.DIRECT_PRODUCT_RUB_ID, {'qty': 300},
                {'bonuses': {'RUB': D('485000')}, 'adjust_qty': False, 'on_create': False, 'minimal_amounts': {'RUB': 300}},
                id='large_bonus'
            ),
        ],
        ['order', 'invoice', 'promocode'],
    )
    @pytest.mark.usefixtures('reservation')
    def test_turn_on_precision(self, session, invoice, promocode, req_qty, req_sum, req_pct, req_base_pct, req_promo_pct, flag_value):
        session.config.__dict__["UNDO_INCREASING_FIXED_SUM_PROMOCODES_PRECISION"] = flag_value
        self.do_test_turn_on(invoice, promocode, req_qty, req_sum, req_pct, req_base_pct, req_promo_pct)

        consume, = invoice.consumes
        session.refresh(invoice)

        assert invoice.promo_code == promocode
        assert_that(
            consume,
            hamcrest.has_properties(
                discount_pct=req_pct,
                discount_obj=mapper.DiscountObj(
                    req_base_pct,
                    req_promo_pct,
                    promocode,
                ),
                current_qty=req_qty,
                current_sum=req_sum,
            )
        )


class TestInvoiceFixedQtyBonusPromocode(BasePromocodeCalcTest):
    @pytest.fixture
    def promocode(self, request, session):
        params = getattr(request, 'param', {})
        calc_params = {
            'product_bonuses': params.get('bonuses', {cst.DIRECT_PRODUCT_ID: 1, cst.DIRECT_PRODUCT_RUB_ID: 30}),
            'adjust_quantity': params.get('adjust_qty', False),
            'apply_on_create': params.get('on_create', False),
        }
        return create_promocode(session, dict(
            calc_class_name='FixedQtyBonusPromoCodeGroup',
            calc_params=calc_params
        ))

    @pytest.mark.parametrize(
        'order, invoice, promocode, req_qty, req_sum, req_pct, req_base_pct, req_promo_pct',
        [
            pytest.param(
                cst.DIRECT_PRODUCT_RUB_ID, {'qty': 666},
                {'bonuses': {cst.DIRECT_PRODUCT_RUB_ID: 30}, 'adjust_qty': True, 'on_create': False},
                D('695.9975'), D('666'), D('4.31'), 0, D('4.31'),
                id='rur'
            ),
            pytest.param(
                cst.DIRECT_PRODUCT_ID, {'qty': 10},
                {'bonuses': {cst.DIRECT_PRODUCT_ID: D('1.234')}, 'adjust_qty': True, 'on_create': False},
                D('11.233431'), D('300'), D('10.98'), 0, D('10.98'),
                id='fish'
            ),
            pytest.param(
                cst.DIRECT_PRODUCT_ID,
                {'qty': 10, 'discount_pct': 10, 'adjust_qty': False},
                {'bonuses': {cst.DIRECT_PRODUCT_ID: D('1.234')}, 'adjust_qty': True, 'on_create': False},
                D('11.233431'), D('270'), D('19.88'), 10, D('10.98'),
                id='w_discount_wo_adj'
            ),
            pytest.param(
                cst.DIRECT_PRODUCT_ID,
                {'qty': 10, 'discount_pct': 10, 'adjust_qty': True},
                {'bonuses': {cst.DIRECT_PRODUCT_ID: D('1.234')}, 'adjust_qty': True, 'on_create': False},
                D('12.48159'), D('300'), D('19.88'), 10, D('10.98'),
                id='w_discount_w_adj'
            ),
        ],
        ['order', 'invoice', 'promocode'],
    )
    @pytest.mark.usefixtures('reservation')
    def test_turn_on(self, invoice, promocode, req_qty, req_sum, req_pct, req_base_pct, req_promo_pct):
        self.do_test_turn_on(invoice, promocode, req_qty, req_sum, req_pct, req_base_pct, req_promo_pct)

    @pytest.mark.parametrize(
        'order, promocode, qty, discount_pct, adjust_qty, req_qty, req_sum, req_pct, req_base_pct, req_promo_pct',
        [
            pytest.param(
                cst.DIRECT_PRODUCT_RUB_ID,
                {'bonuses': {cst.DIRECT_PRODUCT_RUB_ID: 666}, 'adjust_qty': True, 'on_create': True},
                D('100'), 0, False,
                D('766.2835'), D('100'), D('86.95'), 0, D('86.95'),
                id='rub_w_adj_qty',
            ),
            pytest.param(
                cst.DIRECT_PRODUCT_RUB_ID,
                {'bonuses': {cst.DIRECT_PRODUCT_RUB_ID: 100}, 'adjust_qty': False, 'on_create': True},
                D('666'), 0, False,
                D('666'), D('565.97'), D('15.02'), 0, D('15.02'),
                id='rub_wo_adj_qty',
            ),
            pytest.param(
                cst.DIRECT_PRODUCT_ID,
                {'bonuses': {cst.DIRECT_PRODUCT_ID: D('6.66')}, 'adjust_qty': True, 'on_create': True},
                D('10'), 0, False,
                D('16.661113'), D('300'), D('39.98'), 0, D('39.98'),
                id='fish_w_adj_qty',
            ),
            pytest.param(
                cst.DIRECT_PRODUCT_ID,
                {'bonuses': {cst.DIRECT_PRODUCT_ID: D('6.66')}, 'adjust_qty': False, 'on_create': True},
                D('10'), 0, False,
                D('10'), D('100.2'), D('66.6'), 0, D('66.6'),
                id='fish_wo_adj_qty',
            ),
            pytest.param(
                cst.DIRECT_PRODUCT_ID,
                {'bonuses': {cst.DIRECT_PRODUCT_ID: D('6.66')}, 'adjust_qty': False, 'on_create': True},
                D('10'), 10, False,
                D('10'), D('90.18'), D('69.94'), 10, D('66.6'),
                id='wo_adj_qty_w_discount_wo_adj'
            ),
            pytest.param(
                cst.DIRECT_PRODUCT_ID,
                {'bonuses': {cst.DIRECT_PRODUCT_ID: D('6.66')}, 'adjust_qty': False, 'on_create': True},
                D('10'), 10, True,
                D('11.111111'), D('100.2'), D('69.94'), 10, D('66.6'),
                id='wo_adj_qty_w_discount_w_adj',
            ),
            pytest.param(
                cst.DIRECT_PRODUCT_ID,
                {'bonuses': {cst.DIRECT_PRODUCT_ID: 1}, 'adjust_qty': True, 'on_create': True},
                D('10'), 10, False,
                D('10.999890'), D('270'), D('18.18'), 10, D('9.09'),
                id='w_adj_qty_w_discount_wo_adj',
            ),
            pytest.param(
                cst.DIRECT_PRODUCT_ID,
                {'bonuses': {cst.DIRECT_PRODUCT_ID: D('6.66')}, 'adjust_qty': True, 'on_create': True},
                D('10'), 10, True,
                D('18.511662'), D('300'), D('45.98'), 10, D('39.98'),
                id='w_adj_qty_w_discount_w_adj',
            ),
        ],
        ['order', 'promocode'],
    )
    @pytest.mark.usefixtures('reservation')
    def test_create(self,
                    session, order, promocode,
                    qty, discount_pct, adjust_qty,
                    req_qty, req_sum, req_pct, req_base_pct, req_promo_pct):
        self.do_test_create(
            session, order, promocode,
            qty, discount_pct, adjust_qty,
            req_qty, req_sum, req_pct, req_base_pct, req_promo_pct,
        )

    @pytest.mark.parametrize(
        'promocode',
        [
            {'bonuses': {cst.DIRECT_PRODUCT_RUB_ID: D('300'), cst.DIRECT_PRODUCT_ID: 11},
             'adjust_qty': True, 'on_create': True}
        ],
        ['promocode'],
        ['^_^']
    )
    @pytest.mark.parametrize(
        'product_ids, res_discounts',
        [
            pytest.param(
                [cst.DIRECT_PRODUCT_RUB_ID, cst.DIRECT_PRODUCT_RUB_ID, cst.DIRECT_MEDIA_PRODUCT_RUB_ID],
                [D('18.38'), D('18.38'), 0],
                id='multiple_orders'
            ),
            pytest.param(
                [cst.DIRECT_PRODUCT_RUB_ID, cst.DIRECT_PRODUCT_ID, cst.DIRECT_MEDIA_PRODUCT_RUB_ID],
                [0, D('1.62'), 0],
                id='multiple_matching_products'
            ),
        ],
    )
    @pytest.mark.usefixtures('reservation')
    def test_create_multiple_products(self, session, client, product_ids, promocode, res_discounts):
        orders = [create_order(session, client, product_id) for product_id in product_ids]

        with mock.patch('balance.mapper.invoices.Invoice.need_strict_uniq_check', False):
            invoice = create_invoice(session, 666, client, orders)

        assert invoice.promo_code == promocode
        assert [io.promo_code for io in invoice.invoice_orders] == [promocode if d else None for d in res_discounts]
        assert [io.promo_code_discount_pct for io in invoice.invoice_orders] == [d or None for d in res_discounts]

    @pytest.mark.parametrize(
        'promocode',
        [
            {
                'bonuses': {cst.DIRECT_PRODUCT_RUB_ID: D('300')},
                'service_ids': [cst.ServiceId.DIRECT],
                'on_create': True,
                'adjust_qty': True,
            }
        ],
        True,
        ['^_^']
    )
    @pytest.mark.usefixtures('reservation')
    def test_create_products_services(self, session, client, promocode):
        combos = [
            (cst.DIRECT_PRODUCT_RUB_ID, cst.ServiceId.DIRECT),
            (cst.DIRECT_PRODUCT_RUB_ID, cst.ServiceId.MKB),
            (cst.DIRECT_PRODUCT_ID, cst.ServiceId.DIRECT),
        ]
        orders = [
            ob.OrderBuilder.construct(
                session,
                client=client,
                product_id=p_id,
                service_id=s_id
            )
            for p_id, s_id in combos
        ]
        invoice = create_invoice(session, 300, client, orders)

        hamcrest.assert_that(
            invoice,
            hamcrest.has_properties(
                promo_code=promocode,
                invoice_orders=hamcrest.contains_inanyorder(
                    hamcrest.has_properties(
                        order=hamcrest.has_properties(
                            service_id=cst.ServiceId.DIRECT,
                            service_code=cst.DIRECT_PRODUCT_RUB_ID,
                        ),
                        discount_obj=mapper.DiscountObj(0, D('50'), promocode),
                        amount=300,
                        quantity=D('600'),
                    ),
                    hamcrest.has_properties(
                        order=hamcrest.has_properties(
                            service_id=cst.ServiceId.DIRECT,
                            service_code=cst.DIRECT_PRODUCT_ID,
                        ),
                        discount_obj=mapper.DiscountObj(),
                        amount=9000,
                        quantity=300,
                    ),
                    hamcrest.has_properties(
                        order=hamcrest.has_properties(
                            service_id=cst.ServiceId.MKB,
                            service_code=cst.DIRECT_PRODUCT_RUB_ID,
                        ),
                        discount_obj=mapper.DiscountObj(),
                        amount=300,
                        quantity=300,
                    )
                )
            )
        )

    @pytest.mark.parametrize(
        'promocode',
        [
            {
                'bonuses': {cst.DIRECT_PRODUCT_RUB_ID: D('300')},
                'service_ids': [cst.ServiceId.DIRECT],
                'on_create': False
            }
        ],
        True,
        ['^_^']
    )
    @pytest.mark.usefixtures('reservation')
    def test_turn_on_products_services(self, session, client, promocode):
        combos = [
            (cst.DIRECT_PRODUCT_RUB_ID, cst.ServiceId.DIRECT),
            (cst.DIRECT_PRODUCT_RUB_ID, cst.ServiceId.MKB),
            (cst.DIRECT_PRODUCT_ID, cst.ServiceId.DIRECT),
        ]
        orders = [
            ob.OrderBuilder.construct(
                session,
                client=client,
                product_id=p_id,
                service_id=s_id
            )
            for p_id, s_id in combos
        ]
        invoice = create_invoice(session, 300, client, orders)
        invoice.create_receipt(invoice.effective_sum)
        invoice.on_turn_on(None, invoice.effective_sum, manual=True)

        hamcrest.assert_that(
            invoice,
            hamcrest.has_properties(
                promo_code=promocode,
                consumes=hamcrest.contains_inanyorder(
                    hamcrest.has_properties(
                        order=hamcrest.has_properties(
                            service_id=cst.ServiceId.DIRECT,
                            service_code=cst.DIRECT_PRODUCT_RUB_ID,
                        ),
                        discount_obj=mapper.DiscountObj(0, D('50'), promocode),
                        current_sum=300,
                        current_qty=D('600'),
                    ),
                    hamcrest.has_properties(
                        order=hamcrest.has_properties(
                            service_id=cst.ServiceId.DIRECT,
                            service_code=cst.DIRECT_PRODUCT_ID,
                        ),
                        discount_obj=mapper.DiscountObj(),
                        current_sum=9000,
                        current_qty=300,
                    ),
                    hamcrest.has_properties(
                        order=hamcrest.has_properties(
                            service_id=cst.ServiceId.MKB,
                            service_code=cst.DIRECT_PRODUCT_RUB_ID,
                        ),
                        discount_obj=mapper.DiscountObj(),
                        current_sum=300,
                        current_qty=300,
                    )
                )
            )
        )


class TestInvoiceScaleAmountsBonusPromocode(BasePromocodeCalcTest):
    @pytest.fixture
    def promocode(self, request, session):
        params = getattr(request, 'param', {})
        calc_params = {
            'scale_points': params.get('scale', [(100, 50), (200, 150), (300, 300)]),
            'currency': params.get('currency', 'RUB'),
            'convert_currency': params.get('convert', False),
            'adjust_quantity': params.get('adjust_qty', False),
            'apply_on_create': params.get('on_create', False),
        }
        return create_promocode(session, dict(
            calc_class_name='ScaleAmountsBonusPromoCodeGroup',
            calc_params=calc_params
        ))

    @pytest.mark.parametrize(
        'order, invoice, promocode, req_qty, req_sum, req_pct, req_base_pct, req_promo_pct',
        [
            pytest.param(
                cst.DIRECT_PRODUCT_RUB_ID, {'qty': 150},
                {'scale': [(100, 50), (200, 150)], 'adjust_qty': True, 'on_create': False},
                D('209.9958'), D('150'), D('28.57'), 0, D('28.57'),
                id='first_point'
            ),
            pytest.param(
                cst.DIRECT_PRODUCT_RUB_ID, {'qty': 500},
                {'scale': [(100, 50), (200, 150)], 'adjust_qty': True, 'on_create': False},
                D('679.9946'), D('500'), D('26.47'), 0, D('26.47'),
                id='second_point'
            ),
            pytest.param(
                cst.DIRECT_PRODUCT_ID, {'qty': 10},
                {'scale': [(100, 50), (200, 150)], 'adjust_qty': True, 'on_create': False},
                D('16'), D('300'), D('37.5'), 0, D('37.5'),
                id='fish'
            ),
            pytest.param(
                cst.DIRECT_PRODUCT_RUB_ID, {'qty': 200},
                {'scale': [(2, 1), (4, 3)], 'currency': 'USD', 'convert': True,
                 'adjust_qty': True, 'on_create': False},
                D('284.0102'), D('200'), D('29.58'), 0, D('29.58'),
                id='cross_currency'
            ),
        ],
        ['order', 'invoice', 'promocode'],
    )
    @pytest.mark.usefixtures('reservation')
    def test_turn_on(self, invoice, promocode, req_qty, req_sum, req_pct, req_base_pct, req_promo_pct):
        self.do_test_turn_on(invoice, promocode, req_qty, req_sum, req_pct, req_base_pct, req_promo_pct)

    @pytest.mark.parametrize(
        'order, promocode, qty, req_qty, req_sum, req_pct, req_base_pct, req_promo_pct',
        [
            pytest.param(
                cst.DIRECT_PRODUCT_RUB_ID,
                {'scale': [(100, 50), (200, 150)], 'adjust_qty': True, 'on_create': True},
                D('120'), D('179.9910'), D('120'), D('33.33'), 0, D('33.33'),
                id='w_adj_qty'
            ),
            pytest.param(
                cst.DIRECT_PRODUCT_RUB_ID,
                {'scale': [(D('84.74'), 50), (200, 125)], 'adjust_qty': False, 'on_create': True},
                D('250'), D('250'), D('100'), D('60'), 0, D('60'),
                id='wo_adj_qty'
            ),
        ],
        ['order', 'promocode'],
    )
    @pytest.mark.usefixtures('reservation')
    def test_create(self, session, order, promocode, qty, req_qty, req_sum, req_pct, req_base_pct, req_promo_pct):
        self.do_test_create(session, order, promocode, qty, 0, False, req_qty, req_sum,
                            req_pct, req_base_pct, req_promo_pct)

    @pytest.mark.parametrize(
        'promocode',
        [pytest.param({'on_create': True, 'service_ids': [cst.ServiceId.DIRECT]}, id='^_^')],
        True
    )
    @pytest.mark.usefixtures('reservation')
    def test_create_multiple_services(self, session, client, promocode):
        orders = [
            ob.OrderBuilder.construct(
                session,
                client=client,
                product_id=cst.DIRECT_PRODUCT_RUB_ID,
                service_id=service_id
            )
            for service_id in [cst.ServiceId.DIRECT, cst.ServiceId.DIRECT, cst.ServiceId.MKB]
        ]
        invoice = create_invoice(session, [50, 100, 150], client, orders)

        hamcrest.assert_that(
            invoice,
            hamcrest.has_properties(
                promo_code=promocode,
                invoice_orders=hamcrest.contains_inanyorder(
                    hamcrest.has_properties(
                        order=hamcrest.has_properties(service_id=cst.ServiceId.DIRECT),
                        discount_obj=mapper.DiscountObj(0, 40, promocode),
                        amount=30,
                        quantity=50,
                    ),
                    hamcrest.has_properties(
                        order=hamcrest.has_properties(service_id=cst.ServiceId.DIRECT),
                        discount_obj=mapper.DiscountObj(0, 40, promocode),
                        amount=60,
                        quantity=100,
                    ),
                    hamcrest.has_properties(
                        order=hamcrest.has_properties(service_id=cst.ServiceId.MKB),
                        discount_obj=mapper.DiscountObj(),
                        amount=150,
                        quantity=150,
                    )
                )
            )
        )

    @pytest.mark.parametrize(
        'promocode',
        [pytest.param({'on_create': False, 'service_ids': [cst.ServiceId.DIRECT]}, id='^_^')],
        True
    )
    @pytest.mark.usefixtures('reservation')
    def test_turn_on_multiple_services(self, session, client, promocode):
        orders = [
            ob.OrderBuilder.construct(
                session,
                client=client,
                product_id=cst.DIRECT_PRODUCT_RUB_ID,
                service_id=service_id
            )
            for service_id in [cst.ServiceId.DIRECT, cst.ServiceId.DIRECT, cst.ServiceId.MKB]
        ]
        invoice = create_invoice(session, [150, 200, 250], client, orders)
        invoice.create_receipt(invoice.effective_sum)
        invoice.on_turn_on(None, invoice.effective_sum, manual=True)

        hamcrest.assert_that(
            invoice,
            hamcrest.has_properties(
                promo_code=promocode,
                consumes=hamcrest.contains_inanyorder(
                    hamcrest.has_properties(
                        order=hamcrest.has_properties(service_id=cst.ServiceId.DIRECT),
                        discount_obj=mapper.DiscountObj(0, D('33.96'), promocode),
                        current_sum=150,
                        current_qty=D('227.1351'),
                    ),
                    hamcrest.has_properties(
                        order=hamcrest.has_properties(service_id=cst.ServiceId.DIRECT),
                        discount_obj=mapper.DiscountObj(0, D('33.96'), promocode),
                        current_sum=200,
                        current_qty=D('302.8468'),
                    ),
                    hamcrest.has_properties(
                        order=hamcrest.has_properties(service_id=cst.ServiceId.MKB),
                        discount_obj=mapper.DiscountObj(),
                        current_sum=250,
                        current_qty=250,
                    )
                )
            )
        )


class TestCommon(object):

    @pytest.mark.parametrize(
        'promocode, is_ok',
        [
            pytest.param(
                dict(
                    calc_class_name='FixedSumBonusPromoCodeGroup',
                    calc_params={'currency_bonuses': {'RUB': 100}, 'apply_on_create': True, 'adjust_quantity': True}
                ),
                True,
                id='sum_adjust_qty'
            ),
            pytest.param(
                dict(
                    calc_class_name='FixedSumBonusPromoCodeGroup',
                    calc_params={'currency_bonuses': {'RUB': 100}, 'apply_on_create': True, 'adjust_quantity': False}
                ),
                False,
                id='sum_adjust_sum'
            ),
            pytest.param(
                dict(
                    calc_class_name='FixedQtyBonusPromoCodeGroup',
                    calc_params={
                        'product_bonuses': {cst.DIRECT_PRODUCT_RUB_ID: 101},
                        'apply_on_create': True,
                        'adjust_quantity': True
                    }
                ),
                True,
                id='qty_adjust_qty'
            ),
            pytest.param(
                dict(
                    calc_class_name='FixedQtyBonusPromoCodeGroup',
                    calc_params={
                        'product_bonuses': {cst.DIRECT_PRODUCT_RUB_ID: 101},
                        'apply_on_create': True,
                        'adjust_quantity': False
                    }
                ),
                False,
                id='qty_adjust_sum'
            ),
            pytest.param(
                dict(
                    calc_class_name='ScaleAmountsBonusPromoCodeGroup',
                    calc_params={
                        'scale_points': [(0, 100)],
                        'currency': 'RUB',
                        'apply_on_create': True,
                        'adjust_quantity': True
                    }
                ),
                True,
                id='scale_adjust_qty'
            ),
            pytest.param(
                dict(
                    calc_class_name='ScaleAmountsBonusPromoCodeGroup',
                    calc_params={
                        'scale_points': [(0, 100)],
                        'currency': 'RUB',
                        'apply_on_create': True,
                        'adjust_quantity': False
                    }
                ),
                False,
                id='scale_adjust_sum'
            )
        ],
        ['promocode']
    )
    @pytest.mark.usefixtures('reservation')
    def test_bonus_greater_than_sum(self, session, order, promocode, is_ok):
        invoice = create_invoice(session, 100, order.client, order)
        assert invoice.promo_code == (promocode if is_ok else None)
        assert invoice.effective_sum == 100


class TestActBonusPromoCodeGroup(BasePromocodeCalcTest):
    qty = D('1000')
    reserv_date = datetime.datetime(2020, 01, 25, 23, 24, 05)
    act_date = datetime.datetime(2020, 02, 20, 03, 29, 53)
    start_date = datetime.datetime(2020, 03, 10, 0, 0, 0)

    @pytest.fixture
    def independent_acts(self, request, session, client):
        # акты по которым будет считаться размер скидки
        default_params = {
            'date': self.act_date,
            'product_id': cst.DIRECT_PRODUCT_RUB_ID,
            'shipment_qty': self.qty,
            'firm_id': cst.FirmId.YANDEX_OOO,
            'currency': 'RUR',
            'service_id': cst.ServiceId.DIRECT,
        }
        params = getattr(request, 'param', None)
        if params is None:
            params = [{}]
        for item in params:
            map(lambda i: item.setdefault(i[0], i[1]), default_params.iteritems())

        acts = []
        for param_dict in params:
            order = create_order(session, client,
                                 param_dict['product_id'], param_dict['date'], param_dict['service_id'])
            paysys = session.query(mapper.Paysys).filter_by(
                firm_id=param_dict['firm_id'], currency=param_dict['currency']).first()
            invoice = create_invoice(
                session=session,
                qty=param_dict['shipment_qty'],
                client=client,
                orders=[order],
                paysys=paysys,
                person=ob.PersonBuilder(client=client, type=paysys.category),
                dt=param_dict['date'],
                firm_id=param_dict['firm_id'],
            )
            invoice.turn_on_rows()
            order.calculate_consumption(param_dict['date'], {order.shipment_type: param_dict['shipment_qty']})
            act = invoice.generate_act(force=True)[0]
            act.dt = param_dict['date']
            acts.append(act)
        session.flush()
        return acts

    @pytest.fixture
    def promocode(self, request, session):
        pc_params = getattr(request, 'param', {})
        calc_params = {
            'act_bonus_pct': pc_params.get('act_bonus_pct', D('30')),
            'min_act_amount': pc_params.get('min_act_amount', D('10')),
            'max_bonus_amount': pc_params.get('max_bonus_amount', None),
            'max_discount_pct': pc_params.get('max_discount_pct'),
            'currency': pc_params.get('currency', 'RUB'),
            'act_month_count': pc_params.get('act_month_count', 1),
            'adjust_quantity': pc_params.get('adjust_quantity', False),
            'apply_on_create': pc_params.get('on_create', True),
            'act_product_ids': pc_params.get('act_product_ids'),
        }
        return create_promocode(session, dict(
            calc_class_name='ActBonusPromoCodeGroup',
            calc_params={k: v for k, v in calc_params.iteritems() if v is not None},
        ))

    @pytest.mark.parametrize(
        'order,'
        ' independent_acts,'
        ' promocode,'
        ' qty, req_qty, req_sum, req_pct,'
        ' req_base_pct, req_promo_pct',
        [
            pytest.param(
                cst.DIRECT_PRODUCT_RUB_ID,
                None, {'adjust_quantity': False, 'max_discount_pct': D('100')},
                D('1000'), D('1000'), D('700'), D('30'), D('0'), D('30'),
                id='w_adj_qty',
            ),
            pytest.param(
                cst.DIRECT_PRODUCT_RUB_ID,
                None, {'adjust_quantity': True},
                D('1000'), D('1300.052'), D('1000'), D('23.08'), D('0'), D('23.08'),
                id='wo_adj_qty',
            ),
            pytest.param(
                cst.DIRECT_PRODUCT_RUB_ID,
                [{'shipment_qty': D('200')}, {'shipment_qty': D('300')}, {'shipment_qty': D('2000')}],
                {'act_bonus_pct': D('10'), 'adjust_quantity': True},
                D('1000'), D('1250'), D('1000'), D('20'), D('0'), D('20'),
                id='3 acts are available for discount calculation',
            ),
            pytest.param(
                cst.DIRECT_PRODUCT_RUB_ID,
                [{'shipment_qty': D('2000')}, {'shipment_qty': D('500')},
                 {'shipment_qty': D('4000'), 'date': datetime.datetime(2020, 01, 31, 03, 29, 53)}],
                {'act_bonus_pct': D('10'), 'adjust_quantity': True, 'act_month_count': 1},
                D('1000'), D('1250'), D('1000'), D('20'), D('0'), D('20'),
                id='1 act is not in the diapason',
            ),
            pytest.param(
                cst.DIRECT_PRODUCT_RUB_ID,
                [{'shipment_qty': D('200'), 'product_id': cst.DIRECT_PRODUCT_USD_ID, 'currency': 'USD'},
                 {'shipment_qty': D('600'), 'product_id': cst.DIRECT_PRODUCT_RUB_ID}],
                {'act_bonus_pct': D('20'), 'adjust_quantity': True},
                D('2000'), D('2199.978'), D('2000'), D('9.09'), D('0'), D('9.09'),
                id='there are different currencies in acts',
            ),
            pytest.param(
                cst.DIRECT_PRODUCT_RUB_ID,
                [{'shipment_qty': D('600')}],
                {'min_act_amount': D('1000'), 'adjust_quantity': True},
                D('1000'), D('1000'), D('1000'), None, D('0'), D('0'),
                id='act amount is not enough',
            ),
            pytest.param(
                cst.DIRECT_PRODUCT_RUB_ID,
                [{'shipment_qty': D('10000')}],
                {'act_bonus_pct': D('50'), 'max_bonus_amount': D('4000'), 'adjust_quantity': True},
                D('8000'), D('11999.4'), D('8000'), D('33.33'), D('0'), D('33.33'),
                id='act amount is greater than max_bonus_amount',
            ),
            pytest.param(
                cst.DIRECT_PRODUCT_RUB_ID,
                [{'shipment_qty': D('6000')}],
                {'min_act_amount': D('1000'), 'currency': 'USD', 'adjust_quantity': True},
                D('1000'), D('1000'), D('1000'), None, D('0'), D('0'),
                id='min_act_amount is in a different currency',
            ),
            pytest.param(
                cst.DIRECT_PRODUCT_RUB_ID,
                [{'shipment_qty': D('10000')}],
                {'act_bonus_pct': D('50'), 'max_bonus_amount': D('4000'),
                 'max_discount_pct': D('90'), 'adjust_quantity': False},
                D('1000'), D('1000'), D('100'), D('90'), D('0'), D('90'),
                id='bonus is greater than invoice amount',
            ),
            pytest.param(
                cst.DIRECT_PRODUCT_RUB_ID,
                [{'shipment_qty': D('600'), 'service_id': cst.ServiceId.DIRECT},
                 {'shipment_qty': D('400'), 'service_id': cst.ServiceId.MARKET}],
                {'act_bonus_pct': D('10'), 'adjust_quantity': True},
                D('1000'), D('1059.9958'), D('1000'), D('5.66'), D('0'), D('5.66'),
                id='filter acts by service',
            ),
            pytest.param(
                cst.DIRECT_PRODUCT_RUB_ID,
                [{'shipment_qty': D('600'), 'firm_id': cst.FirmId.YANDEX_OOO},
                 {'shipment_qty': D('400'), 'firm_id': cst.FirmId.DRIVE}],
                {'act_bonus_pct': D('10'), 'adjust_quantity': True},
                D('1000'), D('1059.9958'), D('1000'), D('5.66'), D('0'), D('5.66'),
                id='filter acts by firm',
            ),
            pytest.param(
                cst.DIRECT_PRODUCT_RUB_ID,
                [{'shipment_qty': D('600'), 'product_id': cst.DIRECT_PRODUCT_RUB_ID},
                 {'shipment_qty': D('400'), 'product_id': cst.DIRECT_PRODUCT_USD_ID}],
                {'act_bonus_pct': D('10'), 'adjust_quantity': True, 'act_product_ids': [cst.DIRECT_PRODUCT_RUB_ID]},
                D('1000'), D('1059.9958'), D('1000'), D('5.66'), D('0'), D('5.66'),
                id='filter acts by product_id',
            ),
        ],
        ['order', 'independent_acts', 'promocode'],
    )
    @pytest.mark.usefixtures('independent_acts')
    @mock.patch('balance.mapper.promos.ActBonusPromoCodeGroup._get_start_time', return_value=NOW)
    def test_create(self, _mock_meth, session, client, order, promocode, qty, req_qty, req_sum, req_pct,
                    req_base_pct, req_promo_pct):
        reserve_promo_code(client, promocode, self.reserv_date)
        self.do_test_create(session, order, promocode, qty, 0, False, req_qty, req_sum, req_pct,
                            req_base_pct, req_promo_pct)

    @pytest.mark.parametrize(
        'order,'
        ' independent_acts,'
        ' promocode,'
        ' invoice_qty,'
        ' req_qty, req_sum, req_pct,'
        ' req_base_pct, req_promo_pct',
        [
            pytest.param(
                cst.DIRECT_PRODUCT_RUB_ID,
                [{'shipment_qty': D('200')}, {'shipment_qty': D('300')}, {'shipment_qty': D('2000')}],
                {'act_bonus_pct': D('10'), 'adjust_quantity': True},
                D('1000'), D('1250'), D('1000'), D('20'), D('0'), D('20'),
                id='3 acts are available for discount calculation',
            ),
            pytest.param(
                cst.DIRECT_PRODUCT_RUB_ID,
                [{'shipment_qty': D('2000')}, {'shipment_qty': D('500')},
                 {'shipment_qty': D('4000'), 'date': datetime.datetime(2020, 01, 31, 03, 29, 53)}],
                {'act_bonus_pct': D('10'), 'adjust_quantity': True, 'act_month_count': 1},
                D('1000'), D('1250'), D('1000'), D('20'), D('0'), D('20'),
                id='1 act is not in the diapason',
            ),
        ],
        ['order', 'independent_acts', 'promocode'],
    )
    @pytest.mark.usefixtures('independent_acts')
    def test_turn_on(self, session, client, order, promocode, invoice_qty, req_qty, req_sum, req_pct,
                     req_base_pct, req_promo_pct):
        reserve_promo_code(client, promocode, self.reserv_date)
        with mock.patch('balance.mapper.promos.ActBonusPromoCodeGroup._get_start_time', return_value=NOW):
            invoice = create_invoice(session, invoice_qty, client, [order], adjust_qty=True)
            invoice.create_receipt(invoice.effective_sum)
            self.do_test_turn_on(invoice, promocode, req_qty, req_sum, req_pct,
                                 req_base_pct, req_promo_pct)

    @pytest.mark.parametrize(
        'del_param, error_text',
        [
            ('act_bonus_pct', "Promo_code validation: hash sitem 'act_bonus_pct' is mandatory"),
            ('max_discount_pct', 'Set max_discount_pct for not adjust_quantity promocode'),
        ],
    )
    def test_create_promocode(self, promocode, del_param, error_text):
        calc_params = {
            'act_bonus_pct': D('30'),
            'min_act_amount': D('10'),
            'max_bonus_amount': D('666'),
            'max_discount_pct': D('99'),
            'currency': 'RUB',
            'act_month_count': 1,
            'adjust_quantity': False,
            'apply_on_create': True,
            'act_product_ids': [cst.DIRECT_PRODUCT_USD_ID, cst.DIRECT_PRODUCT_RUB_ID],
        }
        del calc_params[del_param]
        promocode.group.calc_params = calc_params
        with pytest.raises(exc.INVALID_PROMOCODE_PARAMS) as exc_info:
            promocode.group.validate()
        assert exc_info.value.msg == error_text

    @pytest.mark.parametrize(
        'independent_acts, promocode, bonus, reservation_date, start_dt, time_boundaries',
        [
            pytest.param([{'shipment_qty': D('111'), 'date': datetime.datetime(2019, 06, 01, 0, 0, 0)},
                          {'shipment_qty': D('333'), 'date': datetime.datetime(2019, 06, 20, 0, 0 , 0)},
                          {'shipment_qty': D('666'), 'date': datetime.datetime(2019, 07, 01, 0, 0, 0)}],
                         {'act_month_count': 1}, D('444'),
                         datetime.datetime(2019, 05, 01, 0, 0, 0), datetime.datetime(2019, 07, 10, 0, 0, 0),
                         (datetime.datetime(2019, 06, 01, 0, 0, 0), datetime.datetime(2019, 07, 01, 0, 0, 0)),
                         id='calc inside 1 month'),
            pytest.param([{'shipment_qty': D('111'), 'date': datetime.datetime(2019, 05, 31, 23, 59, 59)},
                          {'shipment_qty': D('333'), 'date': datetime.datetime(2019, 06, 01, 0, 0, 0)},
                          {'shipment_qty': D('666'), 'date': datetime.datetime(2019, 07, 10, 23, 56, 50)}],
                         {'act_month_count': 1}, D('333'),
                         datetime.datetime(2019, 05, 31, 23, 59, 59), datetime.datetime(2019, 07, 10, 0, 0, 0),
                         (datetime.datetime(2019, 06, 01, 0, 0, 0), datetime.datetime(2019, 7, 01, 0, 0, 0)),
                         id='calc inside 1 month wo month of reservation'),
            pytest.param([{'shipment_qty': D('111'), 'date': datetime.datetime(2019, 05, 31, 23, 59, 59)},
                          {'shipment_qty': D('333'), 'date': datetime.datetime(2019, 06, 01, 0, 0, 0)},
                          {'shipment_qty': D('666'), 'date': datetime.datetime(2019, 07, 10, 04, 56, 45)},
                          {'shipment_qty': D('111.01'), 'date': datetime.datetime(2019, 07, 31, 23, 59, 59)},
                          {'shipment_qty': D('333.03'), 'date': datetime.datetime(2019, 8, 1, 0, 0, 0)},
                          {'shipment_qty': D('666.06'), 'date': datetime.datetime(2019, 8, 10, 0, 0, 0)}],
                         {'act_month_count': 2}, D('1110.01'),
                         datetime.datetime(2019, 05, 10, 0, 0, 0), datetime.datetime(2019, 8, 10, 0, 0, 0),
                         (datetime.datetime(2019, 06, 01, 0, 0, 0), datetime.datetime(2019, 8, 01, 0, 0, 0)),
                         id='calc inside 2 month'),
            pytest.param([{'shipment_qty': D('111'), 'date': datetime.datetime(2019, 12, 31, 23, 59, 59)},
                          {'shipment_qty': D('333'), 'date': datetime.datetime(2020, 01, 01, 0, 0, 0)},
                          {'shipment_qty': D('666'), 'date': datetime.datetime(2020, 02, 29, 23, 59, 59)}],
                         {'act_month_count': 2}, D('999'),
                         datetime.datetime(2019, 12, 30, 0, 0, 0), datetime.datetime(2020, 03, 10, 0, 0, 0),
                         (datetime.datetime(2020, 01, 01, 0, 0, 0), datetime.datetime(2020, 03, 01, 0, 0, 0)),
                         id='calc inside NY'),
        ],
        ['independent_acts', 'promocode'],
    )
    @pytest.mark.usefixtures('independent_acts')
    def test_time_marks(self, session, client, promocode, bonus, reservation_date, start_dt, time_boundaries):
        """Тестируем временные рамки, в которых считается скидка.
        Число, после которого можно приступать к расчетам = 10"""
        currency = 'RUB'
        # относительно этой точки надо рассматривать остальные даты
        reserv_date = reservation_date
        reserve_promo_code(client, promocode, reserv_date)
        session.flush()
        reservation = (
            session
            .query(mapper.PromoCodeReservation)
            .filter_by(client=client.class_, promocode=promocode)
            .one()
        )

        assert promocode.group._get_time_boundaries(reservation) == time_boundaries
        assert promocode.group._get_start_time(reservation) == start_dt
        assert promocode.group.get_bonus(session, currency, client.id, reservation) == bonus
