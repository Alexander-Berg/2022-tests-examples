import os
import json
from datetime import timedelta
import unittest

from agency_rewards.rewards.config import Config
from billing.agency_rewards.tests_platform.common import TestBase

notification_test_is_not_enabled = os.getenv('YA_AR_TEST_NOTIFICATIONS', '1') != '1'


class TestReportDifferences(TestBase):
    @classmethod
    def get_prev_month_range(cls, now):
        prev_month_end = (now.replace(day=1, hour=0, minute=0, second=0, microsecond=0) - timedelta(seconds=1)).replace(
            microsecond=0
        )

        prev_month_start = prev_month_end.replace(day=1, hour=0, minute=0, second=0, microsecond=0)
        return prev_month_start, prev_month_end

    @unittest.skipIf(notification_test_is_not_enabled, "Notifications tests are disabled")
    def test_sent_emails(self):
        with open(f'{Config.regression_email_path}differences.txt', 'r') as emails_data:
            emails = json.loads(emails_data.read())

        def fmt(d: dict) -> dict:
            return {
                'contract_id': d['contract_id'],
                'discount_type': d['discount_type'],
                'reward_type': d['reward_type'],
            }

        emails_formatted = [fmt(e) for e in emails]
        expected_result = [
            {'contract_id': 1, 'reward_type': 312, 'discount_type': 1},
            {'contract_id': 1, 'reward_type': 312, 'discount_type': 3},
            {'contract_id': 1, 'reward_type': 312, 'discount_type': None},
            {'contract_id': 2, 'reward_type': 312, 'discount_type': 3},
            # disappeared records
            {'contract_id': 10, 'reward_type': 314, 'discount_type': 10},
            {'contract_id': 11, 'reward_type': 314, 'discount_type': None},
            # new records
            {'contract_id': 12, 'reward_type': 315, 'discount_type': 12},
            {'contract_id': 13, 'reward_type': 315, 'discount_type': None},
            # prev q record
            {'contract_id': 15, 'reward_type': 400, 'discount_type': 12},
            # prev q new record
            {'contract_id': 16, 'reward_type': 400, 'discount_type': 12},
            # prev q disappeared record
            {'contract_id': 17, 'reward_type': 500, 'discount_type': None},
            {'contract_id': 20, 'reward_type': 310, 'discount_type': 7},
            {'contract_id': 21, 'reward_type': 310, 'discount_type': 7, 'absent': True},
        ]

        for dct in expected_result:
            if 'absent' in dct:
                assert dct not in emails_formatted, dct
            else:
                assert dct in emails_formatted, dct
