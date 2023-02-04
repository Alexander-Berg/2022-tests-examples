# coding: utf-8
__author__ = 'atkaya'

import json
import time

import pytest
from dateutil.relativedelta import relativedelta
from hamcrest import anything

from balance import balance_steps as steps
from btestlib.data.partner_contexts import *
from btestlib.data.simpleapi_defaults import TrustPaymentCases, DEFAULT_PAYMENT_JSON_TEMPLATE
from btestlib.matchers import contains_dicts_equal_to

case = TrustPaymentCases.TAXI_RU_124
order_dt = utils.Date.moscow_offset_dt() - relativedelta(hours=10)


def get_t_payment_data(payment_id):
    query = "select p.amount, p.currency, p.service_id, p.postauth_amount," \
            "cbp.payment_method, cbp.postauth_amount as postauth_amount_cbp from t_payment p " \
            "join t_ccard_bound_payment cbp " \
            "on p.id = cbp.id " \
            "where p.id = :payment_id"
    query_rows = "select payment_rows from t_payment where id = :payment_id"
    params = {'payment_id': payment_id}
    patment_data = db.balance().execute(query, params)[0]
    rows_data = json.loads(db.balance().execute(query_rows, params)[0]['payment_rows'])
    rows_data_wo_order = rows_data[0].copy()
    rows_data_wo_order.pop('order')
    return [patment_data, rows_data[0]['order'].copy(), rows_data_wo_order]


def get_expected_data(case, order_dt):

    expected_payment_data = {
        'amount': case.amount,
        'currency': case.currency.char_code,
        'service_id': case.service.id,
        'postauth_amount': case.amount,
        'postauth_amount_cbp': case.amount,
        'payment_method': anything()
    }

    expected_rows_data = DEFAULT_PAYMENT_JSON_TEMPLATE[0].copy()
    expected_rows_data.update({'price': case.price,'amount': case.amount})
    expected_rows_data.pop('order')

    expected_order_data = DEFAULT_PAYMENT_JSON_TEMPLATE[0]['order'].copy()
    expected_order_data.update({
        'region_id': case.region_id.id,
        'start_dt_utc': anything(),
        'service_product_id': anything(),
        'start_dt_offset': case.dt_offset,
        'service_id': case.service.id,
        'commission_category': case.commission_category
    })
    return [expected_payment_data, expected_rows_data, expected_order_data]


@pytest.fixture(autouse=True)
def mock_trust(mock_simple_api):
    pass


def test_taxi_payment(switch_to_trust):
    switch_to_trust(service=case.service)
    client_id, service_product_id = steps.SimpleApi.create_partner_and_product(case.service)
    service_order_id, trust_payment_id, purchase_token, payment_id = \
        steps.SimpleApi.create_trust_payment(case.service, service_product_id, currency=case.currency,
                                             order_dt=order_dt, price=case.price,
                                             commission_category=case.commission_category)
    patment_data = get_t_payment_data(payment_id)
    expected_data = get_expected_data(case, order_dt)
    utils.check_that(patment_data, contains_dicts_equal_to(expected_data), u'Сравниваем данные в t_payment')
