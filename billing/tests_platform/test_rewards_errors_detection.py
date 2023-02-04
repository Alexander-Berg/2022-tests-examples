import os
import json
from datetime import timedelta
import unittest

from agency_rewards.rewards.config import Config
from billing.agency_rewards.tests_platform.common import TestBase

notification_test_is_not_enabled = os.getenv('YA_AR_TEST_NOTIFICATIONS', '1') != '1'


class TestRewardsErrorsDetection(TestBase):
    @classmethod
    def get_prev_month_range(cls, now):
        prev_month_end = (now.replace(day=1, hour=0, minute=0, second=0, microsecond=0) - timedelta(seconds=1)).replace(
            microsecond=0
        )

        prev_month_start = prev_month_end.replace(day=1, hour=0, minute=0, second=0, microsecond=0)
        return prev_month_start, prev_month_end

    @unittest.skipIf(notification_test_is_not_enabled, "Notifications tests are disabled")
    def test_rewards_errors(self):
        with open(f'{Config.regression_email_path}rewards_errors.txt') as rewards_errors_data:
            rewards_errors = json.loads(rewards_errors_data.read())

        assert any(rewards_errors['month_contract_count_mismatch'])
        assert not all(rewards_errors['quarter_contract_count_mismatch'])
        assert not all(rewards_errors['quarter_turnover_mismatch'])
        assert rewards_errors['month_negative_rewards_to_charge'] == [{'contract_id': 44}]

        assert rewards_errors['quarter_negative_rewards_to_charge'] == [{'contract_id': 42}]
        assert rewards_errors['quarter_total_sum_mismatches'] == []
