# coding: utf-8

__author__ = 'nurlykhan'

import logging

import allure

from balance.tests.integrations.act_data import *
from balance.tests.integrations.contract_data import *
from balance.tests.integrations.config_fetch import get_config_and_id_list
from balance.tests.integrations.transaction_pipeline import *
from btestlib import reporter

log = reporter.logger()


def pytest_generate_tests(metafunc):
    config_name, config_list, ids, mock_trust = get_config_and_id_list(metafunc)
    metafunc.parametrize(config_name, config_list, ids=ids)
    if config_name == 'payment_config':
        metafunc.parametrize('mock_trust', [mock_trust], ids=['mock_trust' if mock_trust else ''])


class TestActs(object):
    @allure.suite('acts')
    def test_acts(self, monkeypatch, act_config):
        act_tests_provider = ActTestsProvider(act_config)
        for case in act_tests_provider.provide_tests():
            contract = ContractDataProvider(case)

            act_processor = ActProcessor(case, contract)
            for act_data in act_processor.generate_act_data():
                act_processor.generate_rows(act_data)  # todo: function return rows and compls_dicts and pass for export?
                act_processor.export_rows(act_data)
                act_processor.create_expected_and_real_data(act_data)
                act_processor.compare()


class TestPayments(object):
    @allure.suite('payments')
    def test_payments(self, monkeypatch, payment_config, mock_trust):
        payment_tests_provider = PaymentTestsProvider(payment_config)
        for case in payment_tests_provider.provide_tests():
            with monkeypatch.context() as m:
                if case.test_input.processing == 'emulator' or mock_trust:
                    FakeTrustActivator.apply_mock_simple_api(m)

                contract = ContractDataProvider(case)

                payment_processor = PaymentProcessor(case, contract)
                payment_processor.generate_rows()  # todo: function return rows and compls_dicts and pass for export?
                payment_processor.export_rows()
                payment_processor.create_expected_and_real_data()
                payment_processor.compare()
