# coding: utf-8
__author__ = 'chihiro'

import pytest

from balance import balance_api as api
from balance import balance_db as db
from check import utils
from check.utils import relative_date, LAST_DAY_OF_MONTH
from btestlib import shared
from check import steps as check_steps
from check.shared import CheckSharedBefore
from check import shared_steps
from balance import balance_steps as steps

CHECK_CODE_NAME = 'ovi'

END_OF_MONTH = relative_date(months=-1, day=LAST_DAY_OF_MONTH)

DIFFS_COUNT = 1

def create_client_and_contract(order_data):
    client_id = check_steps.create_client()
    person_id = check_steps.create_person(client_id, person_category='ph')

    return check_steps.create_acted_orders(
        order_data, client_id, person_id, END_OF_MONTH)[1]


@pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_OVI)
def test_ovi_without_diff(shared_data):
    with CheckSharedBefore(shared_data=shared_data, cache_vars=['orders_map', 'order_invoice_id']) as before:
        before.validate()

        orders_map = create_client_and_contract(
            {1: {'shipment_info': {'Bucks': 50},
                 'consume_qty': 50}}
        )

        order_invoice_id = orders_map['invoice_ids'][0]

    cmp_data = shared_steps.SharedBlocks.run_ovi(shared_data, before, pytest.active_tests)

    cmp_data = cmp_data if cmp_data or cmp_data == [] else shared_data.cache.get('cmp_data')

    assert order_invoice_id not in [row['invoice_id'] for row in cmp_data]
    print('state = ' + '0' + ';   expected = ' + '0')


@pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_OVI)
def test_ovi_with_diff(shared_data):

    with CheckSharedBefore(shared_data=shared_data, cache_vars=['orders_map', 'order_invoice_id']) as before:
        before.validate()

        orders_map = create_client_and_contract(
            {1: {'shipment_info': {'Bucks': 50},
                 'consume_qty': 50}}
        )

        order_invoice_id = orders_map['invoice_ids'][0]

        query = """
                  update bo.t_consume
                  set act_qty = 55, act_sum = 1650
                  where invoice_id = :invoice_id
                """
        query_params = {'invoice_id': order_invoice_id}
        db.balance().execute(query, query_params)

    cmp_data = shared_steps.SharedBlocks.run_ovi(shared_data, before, pytest.active_tests)

    cmp_data = cmp_data or shared_data.cache.get('cmp_data')

    result = [(row['invoice_id'], row['state']) for row in cmp_data if
              row['invoice_id'] == order_invoice_id]
    assert len(result) == 1
    assert (order_invoice_id, 1) in result


@pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_OVI)
def test_ovi_check_diffs_count(shared_data):
    with CheckSharedBefore(shared_data=shared_data,
                             cache_vars=['cache_var']) as before:
        before.validate()
        cache_var = 'test'


    cmp_data = shared_steps.SharedBlocks.run_ovi(shared_data, before, pytest.active_tests)

    cmp_data = cmp_data if cmp_data or cmp_data == [] else shared_data.cache.get('cmp_data')

    assert len(cmp_data) == DIFFS_COUNT



