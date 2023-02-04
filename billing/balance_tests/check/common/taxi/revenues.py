# -*- coding: utf-8 -*-

import abc
import datetime

import pytest

from btestlib.data.defaults import Taxi
from balance import balance_steps as steps
from balance.balance_steps import new_taxi_steps as taxi_steps
from btestlib.constants import Currencies, TaxiOrderType
from btestlib.data.partner_contexts import TAXI_BV_LAT_EUR_CONTEXT

from check.shared import CheckSharedBefore
from check import steps as check_steps, shared_steps


class BaseTaxiRevenuesTest(object):
    # TODO: Пока без тестов на separate issues, кажется их нужно выделить
    #  в отдельный тест, по аналогии с тем как вынесен этот авторазбор в сверках

    __metaclass__ = abc.ABCMeta

    CONTEXT = TAXI_BV_LAT_EUR_CONTEXT

    CURRENCY = Currencies.EUR
    ORDER_TYPE = TaxiOrderType.commission
    COMMISSION_SUM = Taxi.order_commission_card

    YT_TABLES_PATH = '//home/balance_reports/dcs/test/test_data/taxi/tlog/revenues'

    @abc.abstractproperty
    def service_id(self):
        pass

    @abc.abstractproperty
    def check_code_name(self):
        pass

    @abc.abstractproperty
    def order_dt_shift_in_days(self):
        """
        Для разных сверок нужно выделять разные дни, чтобы не пересекаться по файлам.
        Чтобы не копировать даты, указываем здесь смещение от даты начала работы транзакционного лога
        """
        pass

    @property
    def order_dt(self):
        return check_steps.TRANSACTION_LOG_DT + datetime.timedelta(days=self.order_dt_shift_in_days)

    @classmethod
    def create_contract(cls):
        client_id, person_id, contract_id, _ = steps.ContractSteps.create_partner_contract(
            cls.CONTEXT, additional_params={'start_dt': check_steps.CONTRACT_START_DT})
        return client_id, contract_id

    def format_yt_data(self, client_id, commission_sum=None,
                       ignore_in_balance=False, aggregation_sign=1):
        return {
            "dt": self.order_dt,
            "client_id": client_id,
            "currency": self.CURRENCY.iso_code,
            "amount": str(commission_sum or self.COMMISSION_SUM),
            "transaction_type": 'payment',
            "product": self.ORDER_TYPE,
            "event_time": '{}+00:00'.format(self.order_dt.isoformat()),
            "service_id": self.service_id,
            "aggregation_sign": aggregation_sign,
            "ignore_in_balance": ignore_in_balance,
        }

    def create_taxi_order(self, client_id, commission_sum=None):
        return taxi_steps.TaxiSteps.create_order_tlog(
            client_id, dt=self.order_dt, transaction_dt=self.order_dt, currency=self.CURRENCY.iso_code,
            service_id=self.service_id, type=self.ORDER_TYPE, amount=commission_sum or self.COMMISSION_SUM,
        )

    def run_cmp(self, shared_data, before):
        cmp_data = shared_steps.SharedBlocks.run_taxi_revenues_check(
            self.__class__.__name__, self.check_code_name, self.YT_TABLES_PATH,
            shared_data, before, pytest.active_tests,
        )
        cmp_data = cmp_data if cmp_data or cmp_data == [] else shared_data.cache.get('cmp_data')
        return cmp_data

    def do_test_without_diff(self, shared_data):
        with CheckSharedBefore(shared_data=shared_data, cache_vars=['client_id', 'yt_data']) as before:
            before.validate()
            client_id, _ = self.create_contract()

            self.create_taxi_order(client_id)
            yt_data = self.format_yt_data(client_id)

        cmp_data = self.run_cmp(shared_data, before)
        assert client_id not in [row['client_id'] for row in cmp_data]

    def do_test_not_found_in_yt(self, shared_data):
        with CheckSharedBefore(shared_data=shared_data, cache_vars=['client_id']) as before:
            before.validate()
            client_id, _ = self.create_contract()

            self.create_taxi_order(client_id)

        cmp_data = self.run_cmp(shared_data, before)

        result = [(row['client_id'], row['state']) for row in cmp_data if row['client_id'] == client_id]
        assert len(result) == 1
        assert (client_id, 1) in result

    def do_test_not_found_in_billing(self, shared_data):
        with CheckSharedBefore(shared_data=shared_data, cache_vars=['client_id', 'yt_data']) as before:
            before.validate()
            client_id, _ = self.create_contract()

            yt_data = self.format_yt_data(client_id)

        cmp_data = self.run_cmp(shared_data, before)

        result = [(row['client_id'], row['state']) for row in cmp_data if row['client_id'] == client_id]
        assert len(result) == 1
        assert (client_id, 2) in result

    def do_test_commission_sum_mismatch(self, shared_data):
        with CheckSharedBefore(shared_data=shared_data, cache_vars=['client_id', 'yt_data']) as before:
            before.validate()
            client_id, _ = self.create_contract()

            self.create_taxi_order(client_id)
            yt_data = self.format_yt_data(client_id, self.COMMISSION_SUM + 10)

        cmp_data = self.run_cmp(shared_data, before)
        result = [(row['client_id'], row['state']) for row in cmp_data if row['client_id'] == client_id]

        assert len(result) == 1
        assert (client_id, 3) in result

    def do_test_ignore_in_balance(self, shared_data):
        with CheckSharedBefore(shared_data=shared_data, cache_vars=['client_id', 'yt_data']) as before:
            before.validate()

            client_id, _ = self.create_contract()
            yt_data = self.format_yt_data(client_id, ignore_in_balance=True)

        cmp_data = self.run_cmp(shared_data, before)
        assert client_id not in [row['client_id'] for row in cmp_data]

    def do_test_aggregation_sign(self, shared_data):
        with CheckSharedBefore(shared_data=shared_data, cache_vars=['client_id', 'yt_data']) as before:
            before.validate()

            client_id, _ = self.create_contract()

            self.create_taxi_order(client_id, commission_sum=-self.COMMISSION_SUM)
            yt_data = self.format_yt_data(client_id, aggregation_sign=-1)

        cmp_data = self.run_cmp(shared_data, before)
        assert client_id not in [row['client_id'] for row in cmp_data]


# vim:ts=4:sts=4:sw=4:tw=79:et:
