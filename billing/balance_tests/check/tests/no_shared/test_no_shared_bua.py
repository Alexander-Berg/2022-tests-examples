# coding: utf-8

import requests
import hamcrest
import pytest

from btestlib import constants
from btestlib import utils as b_utils

from check import utils, db
from check import steps as check_steps

CHECK_CODE_NAME = 'bua'


def run_bua(objects=None, on_dt=None):
    if objects is not None:
        objects = ','.join(map(str, objects))

    if on_dt is None:
        on_dt = check_steps.ACT_DT

    on_dt_str = on_dt.strftime('%Y-%m-%d')
    params = {
        'completions-dt': on_dt_str,
        'acts-dt': on_dt_str,
    }
    cmp_id = utils.run_check_new(CHECK_CODE_NAME, objects, params)
    return cmp_id, db.get_cmp_diff(cmp_id, CHECK_CODE_NAME)


def test_no_shared_bua_check_query_hash():
    url = utils.dcs_api_url()
    response = requests.get(url + '/testing/checks/bua/hash')
    response.raise_for_status()

    d = response.json() or {}
    assert d.get('status') == 'success'

    value = d.get('value')
    expected = '74b6661a29cd1ee2dba9330b20814770dd8fb233'
    assert value == expected


@pytest.mark.no_parallel('vendor')
def test_no_shared_bua_ofd_significant_overshipment_auto_analyzers(shared_data):
    client_id = check_steps.create_client()
    person_id = check_steps.create_person(client_id, person_category='ur')

    key = 'bua_ofd_overshipment_approved_threshold'
    significant_overshipment = int(utils.get_db_config_value(key)) + 1

    product = constants.Products.OFD_YEAR
    orders_map = check_steps.create_acted_orders(
        {'order': {
            'service_id': product.service.id,
            'product_id': product.id,
            'paysys_id': 1003,
            'shipment_info': {product.type.code: 1000},
            'consume_qty': 1000,
        }}, client_id, person_id
    )

    orders_map['order']['shipment_info'][product.type.code] += significant_overshipment
    check_steps.do_campaign(orders_map['order'])
    check_steps.update_shipment_date(client_id, check_steps.ACT_DT)

    order_id = orders_map['order']['id']
    cmp_id, _ = run_bua([order_id, ])

    b_utils.wait_until(
        lambda: list(utils.get_check_ticket(CHECK_CODE_NAME, cmp_id).comments),
        hamcrest.has_length(hamcrest.greater_than_or_equal_to(2)),
        sleep_time=10,
    )

    ticket = utils.get_check_ticket(CHECK_CODE_NAME, cmp_id)

    rationale_template = utils.get_db_config_value('bua_ofd_overshipment_approved_threshold_excess_rationale')
    rationale = rationale_template.format(significant_overshipment)

    for comment in ticket.comments.get_all():
        if rationale in comment.text:
            b_utils.check_that(
                comment.text,
                hamcrest.contains_string(str(order_id))
            )
            break
    else:
        assert False, u'Комментарий авторазбора не найден'


@pytest.mark.no_parallel('vendor')
@pytest.mark.parametrize('p', [
    {'product': constants.Products.VENDOR,
     'name': 'Vendor'},
    {'product': constants.Products.MARKET,
     'name': 'Market'},
], ids=lambda p: 'overshipment-for-' + p['name'])
def test_no_shared_bua_market_vendor_significant_overshipment_auto_analyzers(shared_data, p):
    client_id = check_steps.create_client()
    person_id = check_steps.create_person(client_id, person_category='ur')

    key = 'bua_market_overshipment_approved_threshold'
    significant_overshipment = int(utils.get_db_config_value(key)) + 1

    product = p['product']
    orders_map = check_steps.create_acted_orders(
        {'order': {
            'service_id': product.service.id,
            'product_id': product.id,
            'paysys_id': 1003,
            'shipment_info': {product.type.code: 1000},
            'consume_qty': 1000,
        }}, client_id, person_id
    )

    orders_map['order']['shipment_info'][product.type.code] += significant_overshipment
    check_steps.do_campaign(orders_map['order'])
    check_steps.update_shipment_date(client_id, check_steps.ACT_DT)

    order_id = orders_map['order']['id']
    cmp_id, _ = run_bua([order_id, ])

    b_utils.wait_until(
        lambda: list(utils.get_check_ticket(CHECK_CODE_NAME, cmp_id).comments),
        hamcrest.has_length(hamcrest.greater_than_or_equal_to(2)),
        sleep_time=10,
    )

    ticket = utils.get_check_ticket(CHECK_CODE_NAME, cmp_id)

    rationale_template = utils.get_db_config_value('bua_market_overshipment_approved_threshold_excess_rationale')
    rationale = rationale_template.format(significant_overshipment)

    for comment in ticket.comments.get_all():
        if rationale in comment.text:
            b_utils.check_that(
                comment.text,
                hamcrest.contains_string(str(order_id))
            )
            break
    else:
        assert False, u'Комментарий авторазбора не найден'
