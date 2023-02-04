# coding: utf-8
__author__ = 'chihiro'

from decimal import Decimal

import pytest

from balance import balance_steps as steps
from balance.balance_steps import new_taxi_steps as taxi_steps
from btestlib.constants import Currencies, PaymentType, TaxiOrderType, Services
from btestlib.data.defaults import Taxi
from btestlib.data.partner_contexts import TAXI_BV_LAT_EUR_CONTEXT
from check import shared_steps, utils
from check import steps as check_steps
from check.shared import CheckSharedBefore

DEFAULT_SERVICE_MIN_COST = Decimal('100')

context_bv_eur = TAXI_BV_LAT_EUR_CONTEXT

SUM_NOT_CONVERGE = 4


def setup_module():
    # Сдвигаем дату перехода на транзакционный лог
    check_steps.update_partner_transaction_log_dt()


class BaseTestCbt(object):
    CURRENCY = Currencies.EUR
    PAYMENT_TYPE = PaymentType.CARD
    SERVICE_ID = Services.TAXI_128.id

    ORDER_TYPE = TaxiOrderType.commission

    COMMISSION_SUM = Taxi.order_commission_card
    PROMOCODE_SUM = 0

    @staticmethod
    def create_contract():
        client_id, person_id, contract_id, _ = steps.ContractSteps.create_partner_contract(
            context_bv_eur, additional_params={'start_dt': check_steps.CONTRACT_START_DT})
        return client_id, contract_id

    @staticmethod
    def check_auto_analyzer(client_id, state, cmp_id, comment_number=0):
        state_comment_dict = {
            1: u'Данные отсутствуют в такси',
            3: u'Расходится сумма комиссии',
            4: u'Расходится сумма промокода'
        }

        ticket = utils.get_check_ticket('cbt', cmp_id)

        comments = list(ticket.comments.get_all())

        # Переделал проверку наличия нужного комментария в комментариях.
        # Сейчас просто проверяем есть ли нужный текст в комментариях вообще(Все комментарии вобъединяю в одну строку).
        # Раньше привязывались к конкретному порядку комментария
        comments_for_check = '\t'.join([row['text'] for row in comments])
        assert u'Разбираем в TAXIDUTY-' in comments_for_check
        assert state_comment_dict[state] in comments_for_check
        assert str(client_id) in comments_for_check

    def _format_yt_data_before_tlog(self, order_dt, client_id, commission_sum=None, promocode_sum=None):
        return {
            "dt": order_dt,
            "client_id": str(client_id),
            "commission_currency": self.CURRENCY.iso_code,
            "commission_value": str(commission_sum or self.COMMISSION_SUM),
            "type": self.ORDER_TYPE,
            "coupon_value": str(promocode_sum or self.PROMOCODE_SUM),
            "payment_method": self.PAYMENT_TYPE,
            'order_cost': str(promocode_sum or self.PROMOCODE_SUM)
        }

    def _format_yt_data_after_tlog(self, order_dt, client_id, commission_sum=None,
                                   ignore_in_balance=None, aggregation_sign=None):
        return {
            "dt": order_dt,
            "client_id": client_id,
            "currency": self.CURRENCY.iso_code,
            "amount": str(commission_sum or self.COMMISSION_SUM),
            "transaction_type": 'payment',
            "product": self.ORDER_TYPE,
            "event_time": '{}+00:00'.format(order_dt.isoformat()),
            "service_id": self.SERVICE_ID,
            "aggregation_sign": aggregation_sign,
            "ignore_in_balance": ignore_in_balance,
        }

    def format_yt_data(self, order_dt, client_id, commission_sum=None, promocode_sum=None,
                       ignore_in_balance=False, aggregation_sign=None):
        if order_dt < check_steps.TRANSACTION_LOG_DT:
            return self._format_yt_data_before_tlog(order_dt, client_id,
                                                    commission_sum=commission_sum,
                                                    promocode_sum=promocode_sum)
        else:
            return self._format_yt_data_after_tlog(order_dt, client_id,
                                                   commission_sum=commission_sum,
                                                   aggregation_sign=aggregation_sign,
                                                   ignore_in_balance=ignore_in_balance)

    def _create_taxi_order_before_tlog(self, order_dt, client_id, commission_sum=None):
        # TODO: перейти на taxi_steps
        return steps.TaxiSteps.create_order(
            client_id, order_dt, self.PAYMENT_TYPE,
            commission_sum=commission_sum, currency=self.CURRENCY.iso_code,
            order_type=self.ORDER_TYPE, promocode_sum=self.PROMOCODE_SUM
        )

    def _create_taxi_order_after_tlog(self, order_dt, client_id, commission_sum=None):
        return taxi_steps.TaxiSteps.create_order_tlog(
            client_id, dt=order_dt, transaction_dt=order_dt, currency=self.CURRENCY.iso_code,
            service_id=self.SERVICE_ID, type=self.ORDER_TYPE, amount=commission_sum,
        )

    def create_taxi_order(self, order_dt, client_id, commission_sum=None):
        if commission_sum is None:
            commission_sum = self.COMMISSION_SUM
        if order_dt < check_steps.TRANSACTION_LOG_DT:
            return self._create_taxi_order_before_tlog(order_dt, client_id, commission_sum)
        else:
            return self._create_taxi_order_after_tlog(order_dt, client_id, commission_sum)


@pytest.mark.parametrize('order_dt', [check_steps.CONTRACT_START_DT, ])
@pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_CBT)
class TestCbt(BaseTestCbt):
    """
    Тесты до введения транзакционного лога
    """

    def test_cbt_without_diff(self, order_dt, shared_data):
        with CheckSharedBefore(shared_data=shared_data, cache_vars=['client_id', 'yt_data']) as before:
            before.validate()
            client_id, _ = self.create_contract()

            self.create_taxi_order(order_dt, client_id)
            yt_data = self.format_yt_data(order_dt, client_id)

        cmp_data = shared_steps.SharedBlocks.run_cbt(shared_data, before, pytest.active_tests)
        cmp_data = cmp_data if cmp_data or cmp_data == [] else shared_data.cache.get('cmp_data')

        assert client_id not in [row['client_id'] for row in cmp_data]

    def test_cbt_not_found_in_yt(self, order_dt, shared_data):
        with CheckSharedBefore(shared_data=shared_data, cache_vars=['client_id']) as before:
            before.validate()
            client_id, _ = self.create_contract()

            self.create_taxi_order(order_dt, client_id)

        cmp_data = shared_steps.SharedBlocks.run_cbt(shared_data, before, pytest.active_tests)
        cmp_data = cmp_data if cmp_data or cmp_data == [] else shared_data.cache.get('cmp_data')

        result = [(row['client_id'], row['state']) for row in cmp_data if row['client_id'] == client_id]
        assert len(result) == 1
        assert (client_id, 1) in result

        cmp_id = cmp_data[0]['cmp_id']

        self.check_auto_analyzer(client_id, 1, cmp_id)

    def test_cbt_not_found_in_billing(self, order_dt, shared_data):
        with CheckSharedBefore(shared_data=shared_data, cache_vars=['client_id', 'yt_data']) as before:
            before.validate()
            client_id, _ = self.create_contract()

            yt_data = self.format_yt_data(order_dt, client_id)

        cmp_data = shared_steps.SharedBlocks.run_cbt(shared_data, before, pytest.active_tests)
        cmp_data = cmp_data if cmp_data or cmp_data == [] else shared_data.cache.get('cmp_data')

        result = [(row['client_id'], row['state']) for row in cmp_data if row['client_id'] == client_id]
        assert len(result) == 1
        assert (client_id, 2) in result

    def test_cbt_commission_sum_mismatch(self, order_dt, shared_data):
        with CheckSharedBefore(shared_data=shared_data, cache_vars=['client_id', 'yt_data']) as before:
            before.validate()
            client_id, _ = self.create_contract()

            self.create_taxi_order(order_dt, client_id)
            yt_data = self.format_yt_data(order_dt, client_id, self.COMMISSION_SUM + 10)

        cmp_data = shared_steps.SharedBlocks.run_cbt(shared_data, before, pytest.active_tests)
        cmp_data = cmp_data if cmp_data or cmp_data == [] else shared_data.cache.get('cmp_data')

        result = [(row['client_id'], row['state']) for row in cmp_data if row['client_id'] == client_id]

        assert len(result) == 1
        assert (client_id, 3) in result

        cmp_id = cmp_data[0]['cmp_id']

        self.check_auto_analyzer(client_id, 3, cmp_id, 0)


@pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_CBT)
class TestCbtSpecific(BaseTestCbt):
    """
    Специфичные тесты и просто микро-тесты.
    """

    DIFFS_COUNT = 4

    def test_cbt_promocode_sum_mismatch(self, shared_data):
        with CheckSharedBefore(shared_data=shared_data, cache_vars=['client_id', 'yt_data']) as before:
            before.validate()

            client_id, _ = self.create_contract()
            order_dt = check_steps.CONTRACT_START_DT

            self.create_taxi_order(order_dt, client_id)
            yt_data = self.format_yt_data(
                order_dt, client_id, promocode_sum=self.PROMOCODE_SUM + 10)

        cmp_data = shared_steps.SharedBlocks.run_cbt(shared_data, before, pytest.active_tests)

        print '>>>>>>>>> [CMP_DATA]: {}'.format(cmp_data)
        print '>>>>>>>>> [CACHE.CMP_DATA]: {}'.format(shared_data.cache.get('cmp_data'))

        cmp_data = cmp_data if cmp_data or cmp_data == [] else shared_data.cache.get('cmp_data')

        result = [(row['client_id'], row['state']) for row in cmp_data if row['client_id'] == client_id]

        assert len(result) == 1

        clid, diff_type = result[0]
        assert clid == client_id
        assert diff_type == SUM_NOT_CONVERGE

        cmp_id = cmp_data[0]['cmp_id']

        self.check_auto_analyzer(client_id, SUM_NOT_CONVERGE, cmp_id)

    def test_cbt_check_diffs_count(self, shared_data):
        with CheckSharedBefore(shared_data=shared_data,
                               cache_vars=['cache_var']) as before:
            before.validate()
            cache_var = 'test'

        cmp_data = shared_steps.SharedBlocks.run_cbt(shared_data, before, pytest.active_tests)
        cmp_data = cmp_data if cmp_data or cmp_data == [] else shared_data.cache.get('cmp_data')

        assert len(cmp_data) == self.DIFFS_COUNT
