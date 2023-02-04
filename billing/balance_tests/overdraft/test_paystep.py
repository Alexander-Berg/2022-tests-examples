# -*- coding: utf-8 -*-

import decimal
import datetime

import pytest
import hamcrest
import mock

from balance import mapper
from balance import paystep
from balance import muzzle_util as ut
from balance.constants import (
    ServiceId,
    CONVERT_TYPE_MODIFY,
    DIRECT_PRODUCT_RUB_ID,
    DIRECT_PRODUCT_ID,
)

from common import patch_sfop

from tests import object_builder as ob

pytestmark = [
    pytest.mark.paystep,
    pytest.mark.usefixtures('switch_new_paystep_flag'),
]

D = decimal.Decimal


class TestPaystepOverdraftsInfo(object):
    def _create_request(self, session, client, product_info, qty, dt=None):
        if isinstance(product_info, dict):
            product = ob.ProductBuilder(**product_info)
        else:
            product = ob.Getter(mapper.Product, product_info)

        order = ob.OrderBuilder(
            product=product,
            client=client,
            service_id=ServiceId.DIRECT,
        ).build(session).obj

        return ob.RequestBuilder(
            basket=ob.BasketBuilder(
                dt=dt,
                client=client,
                rows=[ob.BasketItemBuilder(order=order, quantity=qty)]
            )
        ).build(session).obj

    def _create_invoice(self, session, person, product_id, qty, dt=None):
        return ob.InvoiceBuilder(
            request=self._create_request(session, person.client, product_id, qty, dt),
            person=person,
            overdraft=1
        ).build(session).obj

    @pytest.fixture(autouse=True)
    def _patch_config(self, session):
        session.config.__dict__['OVERDRAFT_EXCEEDED_DELAY'] = 10

    @pytest.fixture
    def client(self, request, session):
        currency, overdraft_limit, is_agency = getattr(request, 'param', ('RUB', 66666666666, 0))
        client = ob.ClientBuilder(is_agency=is_agency).build(session).obj
        if currency:
            client.set_currency(ServiceId.DIRECT, currency, datetime.datetime(2000, 1, 1), CONVERT_TYPE_MODIFY)
        if overdraft_limit is not None:
            client.set_overdraft_limit(ServiceId.DIRECT, 1, overdraft_limit, currency)

        session.flush()
        return client

    @pytest.fixture
    def person(self, request, session, client):
        category = getattr(request, 'param', 'ph')
        return ob.PersonBuilder(type=category, client=client).build(session).obj

    @pytest.fixture
    def spent_sum(self, request, person, session):
        if not hasattr(request, 'param'):
            return
        if isinstance(request.param, list):
            params = request.param
        else:
            params = [request.param]

        for param in params:
            if len(param) == 3:
                product_id, qty, term_delta = param
            elif len(param) == 2:
                product_id, qty = param
                term_delta = 666
            else:
                raise ValueError

            invoice = self._create_invoice(session, person, product_id, qty)
            invoice.payment_term_dt = datetime.datetime.now() + datetime.timedelta(term_delta)
            invoice.turn_on_rows()

    @pytest.fixture
    def invoice(self, request, session, person):
        product_id, qty = request.param
        return self._create_invoice(session, person, product_id, qty)

    @pytest.fixture
    def request_obj(self, request, session, client):
        product_id, qty = request.param
        return self._create_request(session, client, product_id, qty)

    INVOICE_CASES = [
        (
            'cur_ok',
            [
                ('RUB', 666, 0),
                (DIRECT_PRODUCT_RUB_ID, 100),
                (DIRECT_PRODUCT_RUB_ID, 300),
                {
                    'is_present': True,
                    'is_available': True,
                    'available_sum': 366,
                    'available_sum_ue': 366,
                    'spent_sum': 300,
                    'spent_sum_ue': 300,
                    'expired_sum': 0,
                    'expired_sum_ue': 0,
                    'nearly_expired_sum': 0,
                    'nearly_expired_sum_ue': 0,
                }
            ],
        ),
        (
            'cur_nearly_expired',
            [
                ('RUB', 666, 0),
                (DIRECT_PRODUCT_RUB_ID, 100),
                [(DIRECT_PRODUCT_RUB_ID, 300), (DIRECT_PRODUCT_ID, 6, 2)],
                {
                    'is_present': True,
                    'is_available': True,
                    'available_sum': 186,
                    'available_sum_ue': 186,
                    'spent_sum': 480,
                    'spent_sum_ue': 480,
                    'expired_sum': 0,
                    'expired_sum_ue': 0,
                    'nearly_expired_sum': 180,
                    'nearly_expired_sum_ue': 180,
                }
            ],
        ),
        (
            'cur_expired',
            [
                ('RUB', 666, 0),
                (DIRECT_PRODUCT_RUB_ID, 100),
                [(DIRECT_PRODUCT_RUB_ID, 300), (DIRECT_PRODUCT_ID, 6, -1)],
                {
                    'is_present': True,
                    'is_available': False,
                    'available_sum': 186,
                    'available_sum_ue': 186,
                    'spent_sum': 480,
                    'spent_sum_ue': 480,
                    'expired_sum': 180,
                    'expired_sum_ue': 180,
                    'nearly_expired_sum': 0,
                    'nearly_expired_sum_ue': 0,
                }
            ],
        ),
        (
            'cur_excess',
            [
                ('RUB', 666, 0),
                (DIRECT_PRODUCT_RUB_ID, D('367.1')),
                (DIRECT_PRODUCT_RUB_ID, 300),
                {
                    'is_present': True,
                    'is_available': False,
                    'available_sum': 366,
                    'available_sum_ue': 366,
                    'spent_sum': 300,
                    'spent_sum_ue': 300,
                    'expired_sum': 0,
                    'expired_sum_ue': 0,
                    'nearly_expired_sum': 0,
                    'nearly_expired_sum_ue': 0,
                }
            ],
        ),
        (
            'cur_alt_product_ok',
            [
                ('RUB', 666, 0),
                ({'price': D('123.45')}, 4),
                ({'price': 42}, D('3.666666')),
                {
                    'is_present': True,
                    'is_available': True,
                    'available_sum': 512,
                    'available_sum_ue': 512,
                    'spent_sum': 154,
                    'spent_sum_ue': 154,
                    'expired_sum': 0,
                    'expired_sum_ue': 0,
                    'nearly_expired_sum': 0,
                    'nearly_expired_sum_ue': 0,
                }
            ],
        ),
        (
            'cur_alt_product_excess',
            [
                ('RUB', 666, 0),
                ({'price': D('123.45')}, D('4.16')),
                [({'price': 42}, D('3.516666')), ({'price': 42}, D('0.15'), 1)],
                {
                    'is_present': True,
                    'is_available': False,
                    'available_sum': 512,
                    'available_sum_ue': 512,
                    'spent_sum': 154,
                    'spent_sum_ue': 154,
                    'expired_sum': 0,
                    'expired_sum_ue': 0,
                    'nearly_expired_sum': D('6.3'),
                    'nearly_expired_sum_ue': D('6.3'),
                }
            ],
        ),
        (
            'fish_ok',
            [
                (None, 10, 0),
                (DIRECT_PRODUCT_RUB_ID, 150),
                (DIRECT_PRODUCT_ID, 5),
                {
                    'is_present': True,
                    'is_available': True,
                    'available_sum': 150,
                    'available_sum_ue': 5,
                    'spent_sum': 150,
                    'spent_sum_ue': 5,
                    'expired_sum': 0,
                    'expired_sum_ue': 0,
                    'nearly_expired_sum': 0,
                    'nearly_expired_sum_ue': 0,
                }
            ],
        ),
        (
            'fish_expired',
            [
                (None, 10, 0),
                (DIRECT_PRODUCT_RUB_ID, 150),
                [(DIRECT_PRODUCT_ID, 1), (DIRECT_PRODUCT_RUB_ID, 30, 1), (DIRECT_PRODUCT_ID, 1, -1)],
                {
                    'is_present': True,
                    'is_available': False,
                    'available_sum': 210,
                    'available_sum_ue': 7,
                    'spent_sum': 90,
                    'spent_sum_ue': 3,
                    'expired_sum': 30,
                    'expired_sum_ue': 1,
                    'nearly_expired_sum': 30,
                    'nearly_expired_sum_ue': 1,
                }
            ],
        ),
        (
            'fish_excess',
            [
                (None, 10, 0),
                (DIRECT_PRODUCT_ID, 3),
                (DIRECT_PRODUCT_ID, 9),
                {
                    'is_present': True,
                    'is_available': False,
                    'available_sum': 30,
                    'available_sum_ue': 1,
                    'spent_sum': 270,
                    'spent_sum_ue': 9,
                    'expired_sum': 0,
                    'expired_sum_ue': 0,
                    'nearly_expired_sum': 0,
                    'nearly_expired_sum_ue': 0,
                }
            ],
        ),
        (
            'fish_slack',
            [
                (None, 10, 0),
                (DIRECT_PRODUCT_RUB_ID, 59),
                (DIRECT_PRODUCT_ID, 9),
                {
                    'is_present': True,
                    'is_available': True,
                    'available_sum': 30,
                    'available_sum_ue': 1,
                    'spent_sum': 270,
                    'spent_sum_ue': 9,
                    'expired_sum': 0,
                    'expired_sum_ue': 0,
                    'nearly_expired_sum': 0,
                    'nearly_expired_sum_ue': 0,
                }
            ],
        ),
        (
            'fish_alt_product_ok',
            [
                (None, 100, 0),
                ({'price': D('123.45')}, D('3.888')),
                ({'price': 42}, D('60')),
                {
                    'is_present': True,
                    'is_available': True,
                    'available_sum': 480,
                    'available_sum_ue': 16,
                    'spent_sum': 2520,
                    'spent_sum_ue': 84,
                    'expired_sum': 0,
                    'expired_sum_ue': 0,
                    'nearly_expired_sum': 0,
                    'nearly_expired_sum_ue': 0,
                }
            ],
        ),
        (
            'fish_alt_product_excess',  # Пересчет в фишки через округленные суммы.
            [                           # Копейки летят во все стороны
                (None, 100, 0),
                ({'price': D('123.45')}, D('4.14')),
                [({'price': 42}, D('34.567')), ({'price': 42}, D('25.433'), 1)],
                {
                    'is_present': True,
                    'is_available': False,
                    'available_sum': D('480'),
                    'available_sum_ue': D('16'),
                    'spent_sum': D('2520'),
                    'spent_sum_ue': D('84'),
                    'expired_sum': 0,
                    'expired_sum_ue': 0,
                    'nearly_expired_sum': D('1068.19'),
                    'nearly_expired_sum_ue': D('35.60'),
                }
            ],
        ),
    ]

    @pytest.mark.parametrize(
        'client, invoice, spent_sum, required_res',
        [c[1] for c in INVOICE_CASES],
        ['client', 'invoice', 'spent_sum'],
        [c[0] for c in INVOICE_CASES]
    )
    @pytest.mark.usefixtures('spent_sum')
    @pytest.mark.usefixtures('client')
    def test_spent_w_invoice(self, invoice, required_res):
        res = paystep.get_overdrafts_info(invoice.request, invoice, None)
        hamcrest.assert_that(res, hamcrest.has_entries(required_res))

    REQUEST_CASES = [
        (
            'cur_ok',
            [
                ('RUB', 666, 0),
                (DIRECT_PRODUCT_RUB_ID, 100),
                (DIRECT_PRODUCT_RUB_ID, 300),
                {
                    'is_present': True,
                    'is_available': True,
                    'available_sum': 366,
                    'available_sum_ue': 366,
                    'spent_sum': 300,
                    'spent_sum_ue': 300,
                    'expired_sum': 0,
                    'expired_sum_ue': 0,
                    'nearly_expired_sum': 0,
                    'nearly_expired_sum_ue': 0,
                }
            ],
        ),
        (
            'cur_ok_agency',
            [
                ('RUB', 666, 1),
                (DIRECT_PRODUCT_RUB_ID, 100),
                (DIRECT_PRODUCT_RUB_ID, 300),
                {
                    'is_present': True,
                    'is_available': True,
                    'available_sum': 366,
                    'available_sum_ue': 366,
                    'spent_sum': 300,
                    'spent_sum_ue': 300,
                    'expired_sum': 0,
                    'expired_sum_ue': 0,
                    'nearly_expired_sum': 0,
                    'nearly_expired_sum_ue': 0,
                }
            ],
        ),
        (
            'cur_expired',
            [
                ('RUB', 666, 0),
                (DIRECT_PRODUCT_RUB_ID, 100),
                [(DIRECT_PRODUCT_RUB_ID, 100), (DIRECT_PRODUCT_ID, D('3.3'), -1), (DIRECT_PRODUCT_RUB_ID, 100, 1)],
                {
                    'is_present': True,
                    'is_available': False,
                    'available_sum': 367,
                    'available_sum_ue': 367,
                    'spent_sum': 299,
                    'spent_sum_ue': 299,
                    'expired_sum': 99,
                    'expired_sum_ue': 99,
                    'nearly_expired_sum': 100,
                    'nearly_expired_sum_ue': 100,
                }
            ],
        ),
        (
            'cur_expired_agency',
            [
                ('RUB', 666, 1),
                (DIRECT_PRODUCT_RUB_ID, 100),
                [(DIRECT_PRODUCT_RUB_ID, 100), (DIRECT_PRODUCT_ID, D('3.3'), -1),
                 (DIRECT_PRODUCT_RUB_ID, 100, 1)],
                {
                    'is_present': True,
                    'is_available': False,
                    'available_sum': 367,
                    'available_sum_ue': 367,
                    'spent_sum': 299,
                    'spent_sum_ue': 299,
                    'expired_sum': 99,
                    'expired_sum_ue': 99,
                    'nearly_expired_sum': 100,
                    'nearly_expired_sum_ue': 100,
                }
            ],
        ),
        (
            'cur_excess',
            [
                ('RUB', 666, 0),
                (DIRECT_PRODUCT_RUB_ID, 500),
                [(DIRECT_PRODUCT_RUB_ID, 100), (DIRECT_PRODUCT_ID, 3, 1)],
                {
                    'is_present': True,
                    'is_available': False,
                    'available_sum': 476,
                    'available_sum_ue': 476,
                    'spent_sum': 190,
                    'spent_sum_ue': 190,
                    'expired_sum': 0,
                    'expired_sum_ue': 0,
                    'nearly_expired_sum': 90,
                    'nearly_expired_sum_ue': 90,
                }
            ],
        ),
        (
            'cur_excess_agency',
            [
                ('RUB', 666, 1),
                (DIRECT_PRODUCT_RUB_ID, 500),
                [(DIRECT_PRODUCT_RUB_ID, 100), (DIRECT_PRODUCT_ID, 3, 1)],
                {
                    'is_present': True,
                    'is_available': False,
                    'available_sum': 476,
                    'available_sum_ue': 476,
                    'spent_sum': 190,
                    'spent_sum_ue': 190,
                    'expired_sum': 0,
                    'expired_sum_ue': 0,
                    'nearly_expired_sum': 90,
                    'nearly_expired_sum_ue': 90,
                }
            ],
        ),
        (
            'fish_ok',
            [
                (None, 666, 0),
                (DIRECT_PRODUCT_ID, 67),
                (DIRECT_PRODUCT_ID, 600),
                {
                    'is_present': True,
                    'is_available': True,
                    'available_sum': 1980,
                    'available_sum_ue': 66,
                    'spent_sum': 18000,
                    'spent_sum_ue': 600,
                    'expired_sum': 0,
                    'expired_sum_ue': 0,
                    'nearly_expired_sum': 0,
                    'nearly_expired_sum_ue': 0,
                }
            ],
        ),
        (
            'fish_ok_agency',
            [
                (None, 666, 1),
                (DIRECT_PRODUCT_ID, 67),
                (DIRECT_PRODUCT_ID, 600),
                {
                    'is_present': True,
                    'is_available': True,
                    'available_sum': 1980,
                    'available_sum_ue': 66,
                    'spent_sum': 18000,
                    'spent_sum_ue': 600,
                    'expired_sum': 0,
                    'expired_sum_ue': 0,
                    'nearly_expired_sum': 0,
                    'nearly_expired_sum_ue': 0,
                }
            ],
        ),
        (
            'fish_expired',
            [
                (None, 666, 0),
                (DIRECT_PRODUCT_ID, 67),
                [(DIRECT_PRODUCT_ID, 100), (DIRECT_PRODUCT_ID, 100, -1), (DIRECT_PRODUCT_ID, 200, 1)],
                {
                    'is_present': True,
                    'is_available': False,
                    'available_sum': 7980,
                    'available_sum_ue': 266,
                    'spent_sum': 12000,
                    'spent_sum_ue': 400,
                    'expired_sum': 3000,
                    'expired_sum_ue': 100,
                    'nearly_expired_sum': 6000,
                    'nearly_expired_sum_ue': 200,
                }
            ],
        ),
        (
            'fish_expired_agency',
            [
                (None, 666, 1),
                (DIRECT_PRODUCT_ID, 67),
                [(DIRECT_PRODUCT_ID, 100), (DIRECT_PRODUCT_ID, 100, -1),
                 (DIRECT_PRODUCT_ID, 200, 1)],
                {
                    'is_present': True,
                    'is_available': False,
                    'available_sum': 7980,
                    'available_sum_ue': 266,
                    'spent_sum': 12000,
                    'spent_sum_ue': 400,
                    'expired_sum': 3000,
                    'expired_sum_ue': 100,
                    'nearly_expired_sum': 6000,
                    'nearly_expired_sum_ue': 200,
                }
            ],
        ),
        (
            'fish_excess',
            [
                (None, 666, 0),
                (DIRECT_PRODUCT_ID, 68),
                (DIRECT_PRODUCT_ID, 600),
                {
                    'is_present': True,
                    'is_available': False,
                    'available_sum': 1980,
                    'available_sum_ue': 66,
                    'spent_sum': 18000,
                    'spent_sum_ue': 600,
                    'expired_sum': 0,
                    'expired_sum_ue': 0,
                    'nearly_expired_sum': 0,
                    'nearly_expired_sum_ue': 0,
                }
            ],
        ),
        (
            'fish_excess_agency',
            [
                (None, 666, 1),
                (DIRECT_PRODUCT_ID, 68),
                (DIRECT_PRODUCT_ID, 600),
                {
                    'is_present': True,
                    'is_available': False,
                    'available_sum': 1980,
                    'available_sum_ue': 66,
                    'spent_sum': 18000,
                    'spent_sum_ue': 600,
                    'expired_sum': 0,
                    'expired_sum_ue': 0,
                    'nearly_expired_sum': 0,
                    'nearly_expired_sum_ue': 0,
                }
            ],
        )
    ]

    @mock.patch('balance.mapper.ServiceFirmOverdraftParams.get',
                new=mock.Mock(return_value=[patch_sfop()]))
    @pytest.mark.parametrize(
        'client, request_obj, spent_sum, required_res',
        [c[1] for c in REQUEST_CASES],
        ['client', 'request_obj', 'spent_sum'],
        [c[0] for c in REQUEST_CASES]
    )
    @pytest.mark.usefixtures('spent_sum')
    @pytest.mark.usefixtures('client')
    def test_spent_request(self, session, request_obj, person, required_res):
        res = paystep._do_get_overdrafts_info(
            session, request=request_obj,
            currency_rate=D('1'),
            person_category=person.person_category
        )
        hamcrest.assert_that(res, hamcrest.has_entries(required_res))

    @pytest.mark.taxes_update
    def test_spent_w_invoice_fishes_w_price_conversion(self, session):
        tax_change_dt = ut.trunc_date(datetime.datetime.now())
        past = datetime.datetime(2000, 1, 1)

        # client
        client = ob.ClientBuilder().build(session).obj
        person = ob.PersonBuilder(client=client).build(session).obj
        client.set_overdraft_limit(ServiceId.DIRECT, 1, 100, None)
        client.set_currency(ServiceId.DIRECT, 'RUB', tax_change_dt, CONVERT_TYPE_MODIFY, force=1)

        # product
        tax_policy = ob.TaxPolicyBuilder(
            tax_pcts=[
                (past, 18),
                (tax_change_dt, 20)
            ]
        ).build(session).obj

        product = ob.ProductBuilder(
            taxes=tax_policy,
            prices=[(past, 'RUR', 42)]
        ).build(session).obj

        # spent
        prev_invoice = self._create_invoice(
            session, person, product.id,
            qty=10, dt=tax_change_dt - datetime.timedelta(2)
        )
        prev_invoice.turn_on_rows(on_dt=prev_invoice.dt)
        new_invoice = self._create_invoice(session, person, product.id, 7)

        # мокаем дефолтную политику региона и фишек
        with mock.patch('balance.mapper.products.TAX_POLICY_RUSSIA_RESIDENT', tax_policy.id):
            with mock.patch('balance.mapper.products.TaxPolicy.from_country_resident', return_value=[tax_policy]):
                res = paystep.get_overdrafts_info(new_invoice.request, new_invoice, None)

        hamcrest.assert_that(
            res,
            hamcrest.has_entries({
                'is_present': True,
                'is_available': True,
                'available_sum': D('2504.4'),
                'available_sum_ue': D('2504.4'),
                'spent_sum': D('495.6'),
                'spent_sum_ue': D('495.6'),
            })
        )
