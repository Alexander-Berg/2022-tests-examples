# -*- coding: utf-8 -*-

import abc
import decimal
import datetime
import itertools

import pytest
import hamcrest

from balance import balance_db
from btestlib import constants
from btestlib import utils as b_utils
from btestlib.data import partner_contexts
from balance.balance_steps import client_steps
from balance.balance_steps import partner_steps

from check import shared
from check import utils as c_utils
from check import steps as check_steps, shared_steps


class BaseTaxiExpensesTest(object):
    __metaclass__ = abc.ABCMeta

    CONTEXT = partner_contexts.TAXI_RU_CONTEXT_SPENDABLE

    PAYMENT_TYPE = constants.PaymentType.SUBSIDY
    TRANSACTION_TYPE = constants.TransactionType.PAYMENT.name

    AMOUNT = decimal.Decimal('1')
    CURRENCY = CONTEXT.currency.iso_code

    DIFF_COUNT = 7

    YT_TABLES_PATH = '//home/balance_reports/dcs/test/test_data/taxi/tlog/expenses'

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
    def transaction_dt(self):
        return check_steps.TRANSACTION_LOG_DT + datetime.timedelta(days=self.order_dt_shift_in_days)

    @staticmethod
    def create_client():
        return client_steps.ClientSteps.create()

    @staticmethod
    def get_transaction_id():
        (transaction_id,) = partner_steps.PartnerSteps.generate_subvention_transaction_log_ids(n=1)
        return str(transaction_id)

    def create_balance_transaction(self, transaction_id, client_id, amount=AMOUNT):
        query = """
            insert into t_partner_payment_stat
              (id, transaction_id, service_id, client_id, payment_type, transaction_type, price, currency, dt, transaction_dt)
            values
              (s_partner_payment_stat_id.nextval, :transaction_id, :service_id, :client_id, :payment_type, :transaction_type, :amount, :currency, :dt, :transaction_dt)
        """

        query_params = {
            'client_id': client_id,
            'transaction_id': transaction_id,
            'service_id': self.service_id,
            'payment_type': self.PAYMENT_TYPE,
            'transaction_type': self.TRANSACTION_TYPE,
            'amount': amount,
            'currency': self.CURRENCY,
            'dt': self.transaction_dt,
            'transaction_dt': self.transaction_dt,
        }

        balance_db.balance().execute(query, query_params)
        return transaction_id

    def format_yt_data(self, transaction_id, client_id,
                       payment_type=PAYMENT_TYPE,
                       transaction_type=TRANSACTION_TYPE,
                       amount=AMOUNT, currency=CURRENCY,
                       ignore_in_balance=False):
        return {
            'transaction_id': transaction_id,
            'service_id': self.service_id,
            'client_id': client_id,
            'payment_type': payment_type,
            'transaction_type': transaction_type,
            'amount': amount,
            'currency': currency,
            'dt': self.transaction_dt,
            'transaction_dt': self.transaction_dt,
            'ignore_in_balance': ignore_in_balance,
        }

    def run_cmp(self, shared_data, before):
        cmp_data = shared_steps.SharedBlocks.run_taxi_expenses_check(
            self.__class__.__name__, self.check_code_name, self.YT_TABLES_PATH,
            shared_data, before, pytest.active_tests,
        )
        cmp_data = cmp_data if cmp_data or cmp_data == [] else shared_data.cache.get('cmp_data')
        return cmp_data

    @staticmethod
    def _format_cmp_data(cmp_data, transaction_id):
        if transaction_id:
            cmp_data = itertools.ifilter(
                lambda row: row['transaction_id'] == transaction_id, cmp_data)
        return map(lambda row: (row['transaction_id'], row['state']), cmp_data)

    def do_test_without_diff(self, shared_data):
        with shared.CheckSharedBefore(shared_data=shared_data,
                                      cache_vars=['transaction_id', 'client_id', 'yt_data']) as before:
            before.validate()

            client_id = self.create_client()
            transaction_id = self.get_transaction_id()

            self.create_balance_transaction(transaction_id, client_id)
            yt_data = self.format_yt_data(transaction_id, client_id)

        cmp_data = self.run_cmp(shared_data, before)
        cmp_data = self._format_cmp_data(cmp_data, transaction_id)
        b_utils.check_that(cmp_data, hamcrest.empty())

    def do_test_not_found_in_taxi(self, shared_data):
        with shared.CheckSharedBefore(shared_data=shared_data,
                                      cache_vars=['transaction_id', 'client_id']) as before:
            before.validate()

            client_id = self.create_client()
            transaction_id = self.get_transaction_id()

            self.create_balance_transaction(transaction_id, client_id)

        cmp_data = self.run_cmp(shared_data, before)
        cmp_data = self._format_cmp_data(cmp_data, transaction_id)

        b_utils.check_that(cmp_data, hamcrest.has_length(1))
        b_utils.check_that(cmp_data, hamcrest.contains((transaction_id, 1)))

    def do_test_not_found_in_balance(self, shared_data):
        with shared.CheckSharedBefore(shared_data=shared_data,
                                      cache_vars=['transaction_id', 'client_id', 'yt_data']) as before:
            before.validate()

            client_id = self.create_client()
            transaction_id = self.get_transaction_id()

            yt_data = self.format_yt_data(transaction_id, client_id)

        cmp_data = self.run_cmp(shared_data, before)
        cmp_data = self._format_cmp_data(cmp_data, transaction_id)

        b_utils.check_that(cmp_data, hamcrest.has_length(1))
        b_utils.check_that(cmp_data, hamcrest.contains((transaction_id, 2)))

    def do_test_partner_id_not_converge(self, shared_data):
        with shared.CheckSharedBefore(shared_data=shared_data,
                                      cache_vars=['transaction_id', 'client_id', 'taxi_client_id', 'yt_data']) as before:
            before.validate()

            client_id = self.create_client()
            taxi_client_id = self.create_client()

            transaction_id = self.get_transaction_id()

            self.create_balance_transaction(transaction_id, client_id)
            yt_data = self.format_yt_data(transaction_id, taxi_client_id)

        cmp_data = self.run_cmp(shared_data, before)
        cmp_data = self._format_cmp_data(cmp_data, transaction_id)

        b_utils.check_that(cmp_data, hamcrest.has_length(1))
        b_utils.check_that(cmp_data, hamcrest.contains((transaction_id, 3)))

    def do_test_currency_not_converge(self, shared_data):
        with shared.CheckSharedBefore(shared_data=shared_data,
                                      cache_vars=['transaction_id', 'client_id', 'yt_data']) as before:
            before.validate()

            client_id = self.create_client()
            transaction_id = self.get_transaction_id()

            self.create_balance_transaction(transaction_id, client_id)
            yt_data = self.format_yt_data(transaction_id, client_id,
                                          currency=constants.Currencies.UAH.iso_code)

        cmp_data = self.run_cmp(shared_data, before)
        cmp_data = self._format_cmp_data(cmp_data, transaction_id)

        b_utils.check_that(cmp_data, hamcrest.has_length(1))
        b_utils.check_that(cmp_data, hamcrest.contains((transaction_id, 4)))

    def do_test_amount_not_converge(self, shared_data):
        with shared.CheckSharedBefore(shared_data=shared_data,
                                      cache_vars=['transaction_id', 'client_id', 'yt_data']) as before:
            before.validate()

            client_id = self.create_client()
            transaction_id = self.get_transaction_id()

            self.create_balance_transaction(transaction_id, client_id)
            yt_data = self.format_yt_data(transaction_id, client_id,
                                          amount=decimal.Decimal('100'))

        cmp_data = self.run_cmp(shared_data, before)
        cmp_data = self._format_cmp_data(cmp_data, transaction_id)

        b_utils.check_that(cmp_data, hamcrest.has_length(1))
        b_utils.check_that(cmp_data, hamcrest.contains((transaction_id, 5)))

    def do_test_payment_type_not_converge(self, shared_data):
        with shared.CheckSharedBefore(shared_data=shared_data,
                                      cache_vars=['transaction_id', 'client_id', 'yt_data']) as before:
            before.validate()

            client_id = self.create_client()
            transaction_id = self.get_transaction_id()

            self.create_balance_transaction(transaction_id, client_id)
            yt_data = self.format_yt_data(transaction_id, client_id,
                                          payment_type=constants.PaymentType.COUPON)

        cmp_data = self.run_cmp(shared_data, before)
        cmp_data = self._format_cmp_data(cmp_data, transaction_id)

        b_utils.check_that(cmp_data, hamcrest.has_length(1))
        b_utils.check_that(cmp_data, hamcrest.contains((transaction_id, 6)))

    def do_test_transaction_type_not_converge(self, shared_data):
        with shared.CheckSharedBefore(shared_data=shared_data,
                                      cache_vars=['transaction_id', 'client_id', 'yt_data']) as before:
            before.validate()

            client_id = self.create_client()
            transaction_id = self.get_transaction_id()

            self.create_balance_transaction(transaction_id, client_id)
            yt_data = self.format_yt_data(transaction_id, client_id,
                                          transaction_type=constants.TransactionType.REFUND.name)

        cmp_data = self.run_cmp(shared_data, before)
        cmp_data = self._format_cmp_data(cmp_data, transaction_id)

        b_utils.check_that(cmp_data, hamcrest.has_length(1))
        b_utils.check_that(cmp_data, hamcrest.contains((transaction_id, 7)))

    def do_test_ignore_in_balance(self, shared_data):
        with shared.CheckSharedBefore(shared_data=shared_data,
                                      cache_vars=['transaction_id', 'client_id', 'yt_data']) as before:
            before.validate()

            client_id = self.create_client()
            transaction_id = self.get_transaction_id()

            yt_data = self.format_yt_data(transaction_id, client_id, ignore_in_balance=True)

        cmp_data = self.run_cmp(shared_data, before)
        cmp_data = self._format_cmp_data(cmp_data, transaction_id)
        b_utils.check_that(cmp_data, hamcrest.empty())

    def do_test_diffs_count(self, shared_data):
        # Тест не работает вне shared-запуска
        if not pytest.config.getoption('shared'):
            pytest.skip('cannot perfom test without shared')

        with shared.CheckSharedBefore(shared_data=shared_data,
                                      cache_vars=[]) as before:
            before.validate()

        cmp_data = self.run_cmp(shared_data, before)
        b_utils.check_that(cmp_data, hamcrest.has_length(self.DIFF_COUNT))

    def do_separate_issue_auto_analyzer_test(self, state_id, comment_text, shared_data):
        # Тест не работает вне shared-запуска
        if not pytest.config.getoption('shared'):
            pytest.skip('cannot perfom test without shared')

        with shared.CheckSharedBefore(shared_data=shared_data,
                                      cache_vars=[]) as before:
            before.validate()

        cmp_data = shared_data.cache.get('cmp_data') or []
        b_utils.check_that(cmp_data, hamcrest.not_(hamcrest.empty()))

        transactions = []
        for row in cmp_data:
            if row['state'] == state_id:
                transaction_id = row.get('transaction_id')

                if transaction_id is None:
                    raise Exception('No data in cmp_data')

                transactions.append(str(transaction_id))

        b_utils.check_that(transactions, hamcrest.not_(hamcrest.empty()))

        cmp_id = cmp_data[0]['cmp_id']
        ticket = c_utils.get_check_ticket(self.check_code_name, cmp_id)

        for comment in ticket.comments.get_all():
            if comment_text not in comment.text:
                continue

            for transaction_id in transactions:
                b_utils.check_that(comment.text, hamcrest.contains_string(transaction_id))
            else:
                break
        else:
            assert False, u'Комментраий авторазбора для случая #{} не найден'. \
                format(state_id)

    def do_test_auto_analyzer_not_found_in_taxi(self, shared_data):
        self.do_separate_issue_auto_analyzer_test(
            1, u'Субсидии отсутствуют в такси', shared_data)

    def do_test_auto_analyzer_not_found_in_balance(self, shared_data):
        self.do_separate_issue_auto_analyzer_test(
            2, u'Субсидии отсутствуют в Биллинге', shared_data)

    def do_test_auto_analyzer_partner_id_not_converge(self, shared_data):
        self.do_separate_issue_auto_analyzer_test(
            3, u'У субсидий расходится ID партнера', shared_data)

    def do_test_auto_analyzer_currency_not_converge(self, shared_data):
        self.do_separate_issue_auto_analyzer_test(
            4, u'У субсидий расходится валюта', shared_data)

    def do_test_auto_analyzer_amount_not_converge(self, shared_data):
        self.do_separate_issue_auto_analyzer_test(
            5, u'У субсидий расходится сумма', shared_data)

    def do_test_auto_analyzer_payment_type_not_converge(self, shared_data):
        self.do_separate_issue_auto_analyzer_test(
            6, u'У субсидий расходится тип выплаты', shared_data)

    def do_test_auto_analyzer_transaction_type_not_converge(self, shared_data):
        self.do_separate_issue_auto_analyzer_test(
            7, u'У субсидий расходится тип транзакции', shared_data)

# vim:ts=4:sts=4:sw=4:tw=79:et:
