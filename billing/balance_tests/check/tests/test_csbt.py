# coding: utf-8

import pytest

from btestlib import constants

from check import shared_steps
from check.common.taxi import expenses


@pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_CSBT)
class TestCsbt(expenses.BaseTaxiExpensesTest):
    check_code_name = 'csbt'
    service_id = constants.Services.TAXI_CORP_PARTNERS.id

    order_dt_shift_in_days = 2

    def test_without_diff(self, shared_data):
        self.do_test_without_diff(shared_data)

    def test_not_found_in_taxi(self, shared_data):
        self.do_test_not_found_in_taxi(shared_data)

    def test_not_found_in_balance(self, shared_data):
        self.do_test_not_found_in_balance(shared_data)

    def test_partner_id_not_converge(self, shared_data):
        self.do_test_partner_id_not_converge(shared_data)

    def test_currency_not_converge(self, shared_data):
        self.do_test_currency_not_converge(shared_data)

    def test_amount_not_converge(self, shared_data):
        self.do_test_amount_not_converge(shared_data)

    def test_payment_type_not_converge(self, shared_data):
        self.do_test_payment_type_not_converge(shared_data)

    def test_transaction_type_not_converge(self, shared_data):
        self.do_test_transaction_type_not_converge(shared_data)

    def test_ignore_in_balance(self, shared_data):
        self.do_test_ignore_in_balance(shared_data)

    def test_diffs_count(self, shared_data):
        self.do_test_diffs_count(shared_data)

    def test_auto_analyzer_not_found_in_taxi(self, shared_data):
        self.do_test_auto_analyzer_not_found_in_taxi(shared_data)

    def test_auto_analyzer_not_found_in_balance(self, shared_data):
        self.do_test_auto_analyzer_not_found_in_balance(shared_data)

    def test_auto_analyzer_partner_id_not_converge(self, shared_data):
        self.do_test_auto_analyzer_partner_id_not_converge(shared_data)

    def test_auto_analyzer_currency_not_converge(self, shared_data):
        self.do_test_auto_analyzer_currency_not_converge(shared_data)

    def test_auto_analyzer_amount_not_converge(self, shared_data):
        self.do_test_auto_analyzer_amount_not_converge(shared_data)

    def test_auto_analyzer_payment_type_not_converge(self, shared_data):
        self.do_test_auto_analyzer_payment_type_not_converge(shared_data)

    def test_auto_analyzer_transaction_type_not_converge(self, shared_data):
        self.do_test_auto_analyzer_transaction_type_not_converge(shared_data)
