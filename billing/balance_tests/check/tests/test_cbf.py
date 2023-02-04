# -*- coding: utf-8 -*-

import decimal

import pytest
import hamcrest

from btestlib import utils
from btestlib import constants
from balance import balance_steps
from check import shared_steps, shared
from btestlib.data import partner_contexts


CONTEXT = partner_contexts.FOOD_RESTAURANT_CONTEXT
RESTAURANT_COMPLETION_SERVICE = constants.Services.FOOD_SERVICES


class States(object):
    missing_in_food = 1
    missing_in_billing = 2
    amount_mismatch = 3


def create_client():
    client_id, person_id, contract_id, _ = balance_steps.ContractSteps.create_partner_contract(
        CONTEXT, is_offer=1, additional_params={'start_dt': shared_steps.COMPLETION_DT})
    return client_id


def create_balance_completions(client_id, commission_sum='100',
                               service_id=RESTAURANT_COMPLETION_SERVICE.id):
    balance_steps.PartnerSteps.create_fake_product_completion(
        shared_steps.COMPLETION_DT, client_id=client_id,
        service_id=service_id, service_order_id=0,
        commission_sum=decimal.Decimal(commission_sum),
        currency=CONTEXT.currency.iso_code,
        type=constants.FoodProductType.GOODS,
        transaction_dt=shared_steps.COMPLETION_DT,
    )


def format_yt_data(client_id, commission_sum='100',
                   service_id=RESTAURANT_COMPLETION_SERVICE.id):
    return {
        'client_id': client_id,
        'commission_sum': commission_sum,
        'dt': shared_steps.COMPLETION_DT,
        'service_id': service_id,
        'currency': CONTEXT.currency.iso_code,
        'type': constants.FoodProductType.GOODS,
    }


@pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_CBF)
class TestCbf(object):
    def run_cmp(self, shared_data, before):
        cmp_data = shared_steps.SharedBlocks.run_cbf(shared_data, before, pytest.active_tests)
        return cmp_data or shared_data.cache and shared_data.cache.get('cmp_data') or []

    def test_without_diff(self, shared_data):
        with shared.CheckSharedBefore(shared_data=shared_data,
                                      cache_vars=['client_id', 'yt_data']) as before:
            before.validate()

            client_id = create_client()
            create_balance_completions(client_id)
            yt_data = format_yt_data(client_id)

        cmp_data = self.run_cmp(shared_data, before)

        clients = set(row['client_id'] for row in cmp_data)
        utils.check_that(clients, hamcrest.not_(hamcrest.contains(client_id)))

    def test_skip_9999_service(self, shared_data):
        with shared.CheckSharedBefore(shared_data=shared_data,
                                      cache_vars=['client_id', 'yt_data']) as before:
            before.validate()

            client_id = create_client()

            inner_service_id = 9999
            create_balance_completions(client_id, service_id=inner_service_id)
            yt_data = format_yt_data(client_id, service_id=inner_service_id)

        cmp_data = self.run_cmp(shared_data, before)

        clients = set(row['client_id'] for row in cmp_data)
        utils.check_that(clients, hamcrest.not_(hamcrest.contains(client_id)))

    def test_missing_in_food(self, shared_data):
        with shared.CheckSharedBefore(shared_data=shared_data,
                                      cache_vars=['client_id']) as before:
            before.validate()

            client_id = create_client()
            create_balance_completions(client_id)

        cmp_data = self.run_cmp(shared_data, before)
        cmp_data = [(row['client_id'], row['state']) for row in cmp_data
                    if row['state'] == States.missing_in_food]

        utils.check_that(cmp_data, hamcrest.has_length(1))

        expected = [(client_id, States.missing_in_food)]
        utils.check_that(cmp_data, hamcrest.equal_to(expected))

    def test_missing_in_billing(self, shared_data):
        with shared.CheckSharedBefore(shared_data=shared_data,
                                      cache_vars=['client_id', 'yt_data']) as before:
            before.validate()

            client_id = create_client()
            yt_data = format_yt_data(client_id)

        cmp_data = self.run_cmp(shared_data, before)
        cmp_data = [(row['client_id'], row['state']) for row in cmp_data
                    if row['state'] == States.missing_in_billing]

        utils.check_that(cmp_data, hamcrest.has_length(1))

        expected = [(client_id, States.missing_in_billing)]
        utils.check_that(cmp_data, hamcrest.equal_to(expected))

    def test_amount_mismatch(self, shared_data):
        with shared.CheckSharedBefore(shared_data=shared_data,
                                      cache_vars=['client_id', 'yt_data']) as before:
            before.validate()

            client_id = create_client()
            create_balance_completions(client_id, commission_sum='100')
            yt_data = format_yt_data(client_id, commission_sum='111')

        cmp_data = self.run_cmp(shared_data, before)
        cmp_data = [(row['client_id'], row['food_amount'], row['billing_amount'], row['state'])
                    for row in cmp_data if row['state'] == States.amount_mismatch]

        utils.check_that(cmp_data, hamcrest.has_length(1))

        expected = [(client_id, 111, 100, States.amount_mismatch)]
        utils.check_that(cmp_data, hamcrest.equal_to(expected))

# vim:ts=4:sts=4:sw=4:tw=79:et:
