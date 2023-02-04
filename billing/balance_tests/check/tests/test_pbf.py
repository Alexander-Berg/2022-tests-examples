# -*- coding: utf-8 -*-

import decimal
import datetime
import itertools

import enum
import pytest
import hamcrest

from balance import balance_db
from btestlib import constants
from btestlib import utils as b_utils
from check import shared_steps, shared
from btestlib.data import partner_contexts
from balance.balance_steps import client_steps, partner_steps


# TODO: Это плохо так делать, но я не очень уверен в какие utils можно вынести данные функции
from balance.tests.payment import test_food_payments

CONTEXT = partner_contexts.FOOD_RESTAURANT_CONTEXT


class States(enum.Enum):
    missing_in_food = 1
    missing_in_billing = 2
    client_id_mismatch = 3
    currency_mismatch = 4
    amount_mismatch = 5
    payment_type_mismatch = 6
    transaction_type_mismatch = 7
    paysys_type_cc_mismatch = 8
    product_mismatch = 9
    service_order_id_mismatch = 10
    payment_id_mismatch = 11
    non_uniquie_rows_in_food = 12
    non_uniquie_rows_in_billing = 13


def create_client():
    return client_steps.ClientSteps.create()


def get_transaction_id():
    return partner_steps.CommonPartnerSteps.get_fake_food_transaction_id()


def get_fake_id():
    return partner_steps.CommonPartnerSteps.get_fake_trust_payment_id()


def create_balance_transaction(transaction_id, client_id,
                               service_id=CONTEXT.service.id,
                               payment_type=constants.PaymentType.CARD,
                               transaction_type=constants.TransactionType.PAYMENT.name,
                               paysys_type_cc=constants.PaysysType.PAYTURE,
                               product=constants.FoodProductType.GOODS,
                               service_order_id='1',
                               payment_id='1',
                               amount=decimal.Decimal('100'),
                               currency=CONTEXT.currency.iso_code,
                               dt=shared_steps.TRANSACTION_LOG_DT,
                               transaction_dt=shared_steps.TRANSACTION_LOG_DT):
    query = """
        insert into t_partner_payment_stat
          (id, transaction_id, service_id, client_id, payment_type, transaction_type, paysys_type_cc, extra_str_0, extra_str_1, extra_str_2, price, currency, dt, transaction_dt)
        values
          (s_partner_payment_stat_id.nextval, :transaction_id, :service_id, :client_id, :payment_type, :transaction_type, :paysys_type_cc, :product, :service_order_id, :payment_id, :amount, :currency, :dt, :transaction_dt)
    """

    query_params = {
        'transaction_id': transaction_id,
        'service_id': service_id,
        'client_id': client_id,
        'payment_type': payment_type,
        'transaction_type': transaction_type,
        'paysys_type_cc': paysys_type_cc,
        'product': product,
        'service_order_id': service_order_id,
        'payment_id': payment_id,
        'amount': amount,
        'currency': currency,
        'dt': dt,
        'transaction_dt': transaction_dt,
    }

    balance_db.balance().execute(query, query_params)
    return transaction_id


def format_yt_data(transaction_id, client_id,
                   service_id=CONTEXT.service.id,
                   payment_type=constants.PaymentType.CARD,
                   transaction_type=constants.TransactionType.PAYMENT.name,
                   paysys_type_cc=constants.PaysysType.PAYTURE,
                   product=constants.FoodProductType.GOODS,
                   service_order_id='1',
                   payment_id='1',
                   amount=decimal.Decimal('100'),
                   currency=CONTEXT.currency.iso_code,
                   dt=shared_steps.TRANSACTION_LOG_DT):
    return {
        'transaction_id': transaction_id,
        'service_id': service_id,
        'client_id': client_id,
        'payment_type': payment_type,
        'transaction_type': transaction_type,
        'paysys_type_cc': paysys_type_cc,
        'product': product,
        'service_order_id': service_order_id,
        'payment_id': payment_id,
        'amount': amount,
        'currency': currency,
        'dt': dt,
    }


def run_cmp(shared_data, before):
    cmp_data = shared_steps.SharedBlocks.run_pbf(shared_data, before, pytest.active_tests)
    return cmp_data or shared_data.cache and shared_data.cache.get('cmp_data') or []


@pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_PBF)
class TestPbf(object):
    @staticmethod
    def _format_cmp_data(cmp_data, transaction_id):
        if transaction_id:
            cmp_data = itertools.ifilter(
                lambda row: row['transaction_id'] == transaction_id, cmp_data)
        return map(lambda row: (row['transaction_id'], row['state']), cmp_data)

    def test_without_diff(self, shared_data):
        with shared.CheckSharedBefore(shared_data=shared_data,
                                      cache_vars=['transaction_id', 'yt_data']) as before:
            before.validate()

            client_id = create_client()
            transaction_id = get_transaction_id()

            service_order_id = get_fake_id()
            payment_id = get_fake_id()

            create_balance_transaction(transaction_id, client_id,
                                       payment_id=payment_id,
                                       service_order_id=service_order_id)
            yt_data = format_yt_data(transaction_id, client_id,
                                     payment_id=payment_id,
                                     service_order_id=service_order_id)

        cmp_data = run_cmp(shared_data, before)
        cmp_data = self._format_cmp_data(cmp_data, transaction_id)
        b_utils.check_that(cmp_data, hamcrest.empty())

    def test_missing_in_food(self, shared_data):
        with shared.CheckSharedBefore(shared_data=shared_data,
                                      cache_vars=['transaction_id']) as before:
            before.validate()

            client_id = create_client()
            transaction_id = get_transaction_id()

            service_order_id = get_fake_id()
            payment_id = get_fake_id()

            create_balance_transaction(transaction_id, client_id,
                                       payment_id=payment_id,
                                       service_order_id=service_order_id)

        cmp_data = run_cmp(shared_data, before)
        cmp_data = self._format_cmp_data(cmp_data, transaction_id)

        b_utils.check_that(cmp_data, hamcrest.has_length(1))
        b_utils.check_that(cmp_data, hamcrest.contains((transaction_id, States.missing_in_food.value)))

    def test_missing_in_billing(self, shared_data):
        with shared.CheckSharedBefore(shared_data=shared_data,
                                      cache_vars=['transaction_id', 'yt_data']) as before:
            before.validate()

            client_id = create_client()
            transaction_id = get_transaction_id()

            service_order_id = get_fake_id()
            payment_id = get_fake_id()

            yt_data = format_yt_data(transaction_id, client_id,
                                     payment_id=payment_id,
                                     service_order_id=service_order_id)

        cmp_data = run_cmp(shared_data, before)
        cmp_data = self._format_cmp_data(cmp_data, transaction_id)

        b_utils.check_that(cmp_data, hamcrest.has_length(1))
        b_utils.check_that(cmp_data, hamcrest.contains((transaction_id, States.missing_in_billing.value)))

    @pytest.mark.parametrize('key,invalid_value,state',
                             [
                                 ('client_id', -1, States.client_id_mismatch),
                                 ('currency', 'USD', States.currency_mismatch),
                                 ('amount', decimal.Decimal('666'), States.amount_mismatch),
                                 ('payment_type', constants.PaymentType.SUBSIDY, States.payment_type_mismatch),
                                 ('transaction_type', constants.TransactionType.REFUND.name, States.transaction_type_mismatch),
                                 ('paysys_type_cc', constants.PaysysType.DELIVERY, States.paysys_type_cc_mismatch),
                                 ('product', constants.FoodProductType.SUBSIDY, States.product_mismatch),
                                 ('service_order_id', -1, States.service_order_id_mismatch),
                                 ('payment_id', -1, States.payment_id_mismatch),
                             ])
    def test_not_converge(self, shared_data, key, invalid_value, state):
        with shared.CheckSharedBefore(shared_data=shared_data,
                                      cache_vars=['transaction_id', 'yt_data']) as before:
            before.validate()

            client_id = create_client()
            transaction_id = get_transaction_id()

            service_order_id = get_fake_id()
            payment_id = get_fake_id()

            create_balance_transaction(transaction_id, client_id,
                                       payment_id=payment_id,
                                       service_order_id=service_order_id)

            yt_data_params = {
                'transaction_id': transaction_id,
                'client_id': client_id,
                'payment_id': payment_id,
                'service_order_id': service_order_id,
                key: invalid_value,
            }
            yt_data = format_yt_data(**yt_data_params)

        cmp_data = run_cmp(shared_data, before)
        cmp_data = self._format_cmp_data(cmp_data, transaction_id)

        b_utils.check_that(cmp_data, hamcrest.has_length(1))
        b_utils.check_that(cmp_data, hamcrest.contains((transaction_id, state.value)))

    def test_non_unique_rows_in_food(self, shared_data):
        with shared.CheckSharedBefore(shared_data=shared_data,
                                      cache_vars=['transaction_id', 'yt_data']) as before:
            before.validate()

            client_id = create_client()
            transaction_id = get_transaction_id()

            service_order_id = get_fake_id()
            payment_id = get_fake_id()

            create_balance_transaction(transaction_id, client_id,
                                       payment_id=payment_id,
                                       service_order_id=service_order_id)

            yt_data = [format_yt_data(transaction_id, client_id,
                                      payment_id=payment_id,
                                      service_order_id=service_order_id)
                       for _ in xrange(2)]

        cmp_data = run_cmp(shared_data, before)
        cmp_data = self._format_cmp_data(cmp_data, transaction_id)

        state_id = States.non_uniquie_rows_in_food.value
        b_utils.check_that(cmp_data, hamcrest.has_length(1))
        b_utils.check_that(cmp_data, hamcrest.contains((transaction_id, state_id)))

    def test_non_uniquie_rows_in_billing(self, shared_data):
        with shared.CheckSharedBefore(shared_data=shared_data,
                                      cache_vars=['transaction_id', 'yt_data']) as before:
            before.validate()

            client_id = create_client()
            transaction_id = get_transaction_id()

            service_order_id = get_fake_id()
            payment_id = get_fake_id()

            # Дубликаты обязательно должны быть в разных партициях
            create_balance_transaction(transaction_id, client_id,
                                       payment_id=payment_id,
                                       service_order_id=service_order_id,
                                       transaction_dt=shared_steps.TRANSACTION_LOG_DT)
            create_balance_transaction(transaction_id, client_id,
                                       payment_id=payment_id,
                                       service_order_id=service_order_id,
                                       transaction_dt=shared_steps.ACT_DT)
            yt_data = format_yt_data(transaction_id, client_id,
                                     payment_id=payment_id,
                                     service_order_id=service_order_id)

        cmp_data = run_cmp(shared_data, before)
        cmp_data = self._format_cmp_data(cmp_data, transaction_id)

        state_id = States.non_uniquie_rows_in_billing.value
        b_utils.check_that(cmp_data, hamcrest.has_length(1))
        b_utils.check_that(cmp_data, hamcrest.contains((transaction_id, state_id)))

# vim:ts=4:sts=4:sw=4:tw=79:et:
