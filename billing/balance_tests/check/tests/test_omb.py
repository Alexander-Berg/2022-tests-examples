# coding: utf-8
__author__ = 'chihiro'

from decimal import Decimal

import pytest

from balance import balance_steps as steps
from balance import balance_api as api
from btestlib.constants import Products, PersonTypes, Paysyses, Firms
from check import steps as check_steps
from check import shared_steps
from check.shared import CheckSharedBefore
from check.defaults import LAST_DAY_OF_PREVIOUS_MONTH
from check.db import _create_data_in_market
from check.utils import run_check_new


class TestOMB(object):
    DIFFS_COUNT = 4

    @staticmethod
    def create_order(qty_bucks=Decimal("22")):
        client_id = check_steps.create_client()
        person_id = check_steps.create_person(
            client_id, person_category=PersonTypes.UR.code
        )
        order_map = check_steps.create_act_map({1: {'paysys_id': Paysyses.BANK_UR_RUB.id,
                                                    'service_id': Products.MARKET.service.id,
                                                    'product_id': Products.MARKET.id,
                                                    'shipment_info': {'Bucks': qty_bucks}}
                                                }, client_id, person_id)
        return {
            'service_order_id': order_map[0]['service_order_id'],
            'service_id': order_map[0]['service_id'],
            'completion_qty': qty_bucks,
            'client_id': client_id,
            'person_id': person_id,
            'id': order_map[0]['id']
        }

    @pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_OMB)
    def test_omb_without_diff(self, shared_data):
        with CheckSharedBefore(shared_data=shared_data,
                               cache_vars=['order_data']) as before:
            before.validate()
            order_data = self.create_order()
            _create_data_in_market([
                order_data['service_id'], order_data['service_order_id'], order_data['completion_qty']
            ])

        cmp_data = shared_steps.SharedBlocks.run_omb(shared_data, before, pytest.active_tests)

        cmp_data = cmp_data or shared_data.cache.get('cmp_data')

        assert order_data['service_order_id'] not in [row['order_id'] for row in cmp_data]

    @pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_OMB)
    def test_omb_not_found_in_market(self, shared_data):
        with CheckSharedBefore(shared_data=shared_data,
                               cache_vars=['order_data']) as before:
            before.validate()
            order_data = self.create_order()

        cmp_data = shared_steps.SharedBlocks.run_omb(shared_data, before, pytest.active_tests)

        cmp_data = cmp_data or shared_data.cache.get('cmp_data')

        result = [(row['order_id'], row['state']) for row in cmp_data if
                  row['order_id'] == order_data['service_order_id']]
        assert len(result) == 1
        assert (order_data['service_order_id'], 1) in result

    @pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_OMB)
    def test_omb_not_found_in_billing(self, shared_data):
        with CheckSharedBefore(shared_data=shared_data,
                               cache_vars=['order_data']) as before:
            before.validate()
            client_id = check_steps.create_client()
            person_id = check_steps.create_person(
                client_id, person_category=PersonTypes.UR.code
            )
            order_id, service_order_id = check_steps.create_order(client_id, Products.MARKET.service.id,
                                                                  Products.MARKET.id)
            invoice_id, _ = check_steps.create_invoice(client_id, person_id,
                                                       orders_list=[{'ServiceID': Products.MARKET.service.id,
                                                                     'ServiceOrderID': service_order_id,
                                                                     'Qty': Decimal(20),
                                                                     'BeginDT': LAST_DAY_OF_PREVIOUS_MONTH}],
                                                       paysys_id=Paysyses.BANK_UR_RUB.id)
            steps.InvoiceSteps.pay(invoice_id, payment_dt=LAST_DAY_OF_PREVIOUS_MONTH)
            order_data = {
                'service_id': Products.MARKET.service.id, 'service_order_id': service_order_id
            }
            _create_data_in_market([
                order_data['service_id'], order_data['service_order_id'], 121
            ])

        cmp_data = shared_steps.SharedBlocks.run_omb(shared_data, before, pytest.active_tests)

        cmp_data = cmp_data or shared_data.cache.get('cmp_data')

        result = [(row['order_id'], row['state']) for row in cmp_data if
                  row['order_id'] == order_data['service_order_id']]
        assert len(result) == 1
        assert (order_data['service_order_id'], 2) in result

    @pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_OMB)
    def test_omb_changed_sum(self, shared_data):
        with CheckSharedBefore(shared_data=shared_data,
                               cache_vars=['order_data']) as before:
            before.validate()
            order_data = self.create_order()
            _create_data_in_market([
                order_data['service_id'], order_data['service_order_id'], order_data['completion_qty'] + 1
            ])

        cmp_data = shared_steps.SharedBlocks.run_omb(shared_data, before, pytest.active_tests)

        cmp_data = cmp_data or shared_data.cache.get('cmp_data')

        result = [(row['order_id'], row['state']) for row in cmp_data if
                  row['order_id'] == order_data['service_order_id']]
        assert len(result) == 1
        assert (order_data['service_order_id'], 3) in result

    @pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_OMB)
    def test_omb_mismatch_was_found_in_previous_check(self, shared_data):
        with CheckSharedBefore(shared_data=shared_data,
                               cache_vars=['order_data']) as before:
            before.validate()
            order_data = self.create_order()
            _create_data_in_market([
                order_data['service_id'], order_data['service_order_id'], order_data['completion_qty'] + 1
            ])
            cmp_id = run_check_new('omb', str(order_data['service_order_id']))
            query = 'update (select * from cmp.omb_cmp_data where cmp_id = {0} and order_id = {1}) set jira_id = \'test_omb\''.format(
                cmp_id, order_data['service_order_id'])
            api.test_balance().ExecuteSQL('cmp', query)

        cmp_data = shared_steps.SharedBlocks.run_omb(shared_data, before, pytest.active_tests)

        cmp_data = cmp_data or shared_data.cache.get('cmp_data')

        result = [(row['order_id'], row['state'], row['jira_id']) for row in cmp_data if
                  row['order_id'] == order_data['service_order_id']]
        assert len(result) == 1
        assert (order_data['service_order_id'], 3, 'test_omb') in result

    @pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_OMB)
    def test_omb_check_diffs_count(self, shared_data):
        with CheckSharedBefore(shared_data=shared_data,
                               cache_vars=['cache_var']) as before:
            before.validate()
            cache_var = 'test'

        cmp_data = shared_steps.SharedBlocks.run_omb(shared_data, before, pytest.active_tests)

        cmp_data = cmp_data or shared_data.cache.get('cmp_data')

        assert len(cmp_data) == self.DIFFS_COUNT
