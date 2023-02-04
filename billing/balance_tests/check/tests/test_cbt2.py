# -*- coding: utf-8 -*-

import pytest

from btestlib import constants

from check import shared_steps
from check.common.taxi import revenues


@pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_CBT2)
class TestCbt2(revenues.BaseTaxiRevenuesTest):
    check_code_name = 'cbt2'
    service_id = constants.Services.TAXI_128.id

    order_dt_shift_in_days = 1

    def test_without_diff(self, shared_data):
        self.do_test_without_diff(shared_data)

    def test_not_found_in_yt(self, shared_data):
        self.do_test_not_found_in_yt(shared_data)

    def test_not_found_in_billing(self, shared_data):
        self.do_test_not_found_in_billing(shared_data)

    def test_commission_sum_mismatch(self, shared_data):
        self.do_test_commission_sum_mismatch(shared_data)

    def test_ignore_in_balance(self, shared_data):
        self.do_test_ignore_in_balance(shared_data)

    def test_aggregation_sign(self, shared_data):
        self.do_test_aggregation_sign(shared_data)

# vim:ts=4:sts=4:sw=4:tw=79:et:
