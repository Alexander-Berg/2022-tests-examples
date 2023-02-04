# -*- coding: utf-8 -*-

import datetime
import decimal

import pytest
import mock

import balance.muzzle_util as ut
from balance import mapper
import balance.constants as cst
from butils import decimal_unit

from tests import object_builder as ob

D = decimal.Decimal
DU = decimal_unit.DecimalUnit


class TestQuantityToLimitConversion(object):
    @pytest.fixture(autouse=True)
    def patch_currency(self):

        dt_rates = [
            {
                'RUR': DU(1),
                'RUB': DU(1),
                'EUR': DU('70', 'RUB', 'EUR'),
                'USD': DU('60', 'RUB', 'USD'),
            }
        ]
        with ob.patched_currency(dt_rates):
            yield

    @pytest.fixture
    def case_info(self, request, session):
        service_id, product_id, region_id, client_currency, convert_type, category_cc, qty, req_qty = request.param
        if isinstance(product_id, dict):
            product = ob.ProductBuilder(**product_id).build(session).obj
        else:
            product = ob.Getter(mapper.Product, product_id).build(session).obj

        client = ob.ClientBuilder(region_id=region_id).build(session).obj
        if client_currency:
            client.set_currency(cst.ServiceId.DIRECT, client_currency, datetime.datetime(2000, 1, 1), convert_type)

        order = ob.OrderBuilder(product=product, client=client, service_id=service_id).build(session).obj
        person = ob.PersonBuilder(client=client, type=category_cc).build(session).obj
        return order, person, qty, req_qty

    CASES = {
        'fish_no_migr': (cst.ServiceId.DIRECT, cst.DIRECT_PRODUCT_ID, cst.RegionId.RUSSIA, None, None, 'ph', DU(10, 'QTY'), DU(10, 'QTY')),
        'fish_rur_modify': (cst.ServiceId.DIRECT, cst.DIRECT_PRODUCT_ID, cst.RegionId.RUSSIA, 'RUB', cst.CONVERT_TYPE_MODIFY, 'ph', DU(10, 'QTY'), DU(300, 'RUB')),
        'fish_rur_copy': (cst.ServiceId.DIRECT, cst.DIRECT_PRODUCT_ID, cst.RegionId.RUSSIA, 'RUB', cst.CONVERT_TYPE_COPY, 'ph', DU(10, 'QTY'), DU(300, 'RUB')),
        'rur_modify': (cst.ServiceId.DIRECT, cst.DIRECT_PRODUCT_RUB_ID, cst.RegionId.RUSSIA, 'RUB', cst.CONVERT_TYPE_MODIFY, 'ph', DU(666, 'QTY'), DU(666, 'RUB')),
        'rur_copy': (cst.ServiceId.DIRECT, cst.DIRECT_PRODUCT_RUB_ID, cst.RegionId.RUSSIA, 'RUB', cst.CONVERT_TYPE_COPY, 'ph', DU(666, 'QTY'), DU(666, 'RUB')),
        'usd_copy': (cst.ServiceId.DIRECT, cst.DIRECT_PRODUCT_USD_ID, cst.RegionId.SWITZERLAND, 'USD', cst.CONVERT_TYPE_COPY, 'sw_ph', DU(666, 'QTY'), DU(666, 'USD')),
        'rur_no_migr': (cst.ServiceId.DIRECT, cst.DIRECT_PRODUCT_RUB_ID, cst.RegionId.RUSSIA, None, None, 'ph', DU(666, 'QTY'), DU('22.2', 'QTY')),
        'fish_eur_copy': (cst.ServiceId.DIRECT, cst.DIRECT_PRODUCT_ID, cst.RegionId.SWITZERLAND, 'EUR', cst.CONVERT_TYPE_COPY, 'sw_ph', DU(10, 'QTY'), DU('3.846429', 'EUR')),
        'market_fish': (cst.ServiceId.MARKET, cst.MARKET_FISH_PRODUCT_ID, cst.RegionId.RUSSIA, None, None, 'ph', DU(10, 'QTY'), DU('10', 'QTY')),
        'direct_nonfish': (cst.ServiceId.DIRECT, {'price': 666}, cst.RegionId.RUSSIA, None, None, 'ph', DU(10, 'QTY'), DU('222', 'QTY')),
        'market_nonfish': (cst.ServiceId.MARKET, {'price': 666}, cst.RegionId.RUSSIA, None, None, 'ph', DU(10, 'QTY'), DU('222', 'QTY')),
    }

    @pytest.mark.parametrize(
        'case_info',
        CASES.values(),
        ids=CASES.keys(),
        indirect=True
    )
    def test_invoice_overdraft_sum(self, session, case_info):
        order, person, qty, required_qty = case_info

        with mock.patch('balance.mapper.OverdraftInvoice.update_payment_term', lambda *args, **kwargs: True):
            invoice = ob.InvoiceBuilder(
                request=ob.RequestBuilder(
                    basket=ob.BasketBuilder(
                        client=person.client,
                        rows=[ob.BasketItemBuilder(order=order, quantity=qty)]
                    )
                ),
                person=person,
                overdraft=1
            ).build(session).obj
        invoice.turn_on_rows()

        qty_consumes = invoice.get_overdraft_sum()
        qty_ios = invoice.get_overdraft_sum(from_consumes=False)
        assert required_qty == qty_consumes
        assert required_qty == qty_ios
        assert isinstance(qty_consumes, DU)
        assert isinstance(qty_ios, DU)

    @pytest.mark.taxes_update
    @pytest.mark.parametrize(
        'dt_shift, req_sum',
        [
            (3, D('1100.232')),
            (0, D('1118.88')),
        ],
        ids=['before_tax_update', 'after_tax_update']
    )
    def test_fishes_w_price_conversion(self, session, dt_shift, req_sum):
        tax_change_dt = ut.trunc_date(datetime.datetime.now())
        past = datetime.datetime(2000, 1, 1)

        tax_policy = ob.TaxPolicyBuilder(
            tax_pcts=[
                (past, 18),
                (tax_change_dt, 20)
            ]
        ).build(session).obj

        product = ob.ProductBuilder(
            taxes=tax_policy,
            prices=[(past, 'RUR', 666)]
        ).build(session).obj

        invoice = ob.InvoiceBuilder(
            request=ob.RequestBuilder(
                basket=ob.BasketBuilder(
                    dt=datetime.datetime.now() - datetime.timedelta(dt_shift),
                    rows=[ob.BasketItemBuilder(
                        order=ob.OrderBuilder(product=product),
                        quantity=42
                    )]
                )
            ),
            overdraft=1
        ).build(session).obj

        with mock.patch('balance.mapper.products.TAX_POLICY_RUSSIA_RESIDENT', tax_policy.id):
            sum_ = invoice.get_overdraft_sum(from_consumes=False)

        assert sum_ == req_sum
