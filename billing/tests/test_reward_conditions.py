import unittest
from decimal import Decimal

from agency_rewards.rewards.common.reward_condition import (
    StepRewardCondition,
    RewardCondition,
)


def get_test_cls():
    class Test(object):
        def __init__(self, result, test_value, lower, upper, pct, base_amt=0, scale=None):
            self.result = result
            self.lower = lower
            self.upper = upper
            self.pct = pct
            self.test_value = test_value
            self.scale = scale
            self.base_amt = base_amt

        def __repr__(self):
            return "value={}, lower={}, upper={}, pct={}".format(self.test_value, self.lower, self.upper, self.pct)

    return Test


class TestRewardCondition(unittest.TestCase):
    def test_scale(self):
        r = RewardCondition(0.3, 5, 8)
        self.assertEqual(300000, r.lower)
        self.assertEqual(5000000, r.upper)
        r1 = RewardCondition(3, float("inf"), 8, scale=RewardCondition.K)
        self.assertEqual(3000, r1.lower)
        self.assertEqual(float("inf"), r1.upper)

    def test_pct_decimal(self):
        r = RewardCondition(3000, float("inf"), 8)
        self.assertEqual(Decimal('0.08'), r.pct)

    def test_is_in(self):
        cls = get_test_cls()

        tests = (
            cls(True, 1000001, 1, float("inf"), 8),
            cls(False, 999999, 1, float("inf"), 8),
            cls(True, 1000000, 1, 2, 8),
            cls(False, 2000000, 1, 2, 8),
        )
        for idx, test in enumerate(tests):
            r = RewardCondition(test.lower, test.upper, test.pct)
            self.assertEqual(
                test.result, r.is_in(test.test_value), "{}. Did not get {} for: {}".format(idx, test.result, test)
            )


class TestStepRewardCondition(unittest.TestCase):
    def test_scale(self):
        r = StepRewardCondition(0.3, 5, 8)
        self.assertEqual(300000, r.lower)
        self.assertEqual(5000000, r.upper)
        r1 = StepRewardCondition(3, float("inf"), 8, scale=RewardCondition.K)
        self.assertEqual(3000, r1.lower)
        self.assertEqual(float("inf"), r1.upper)

    def test_is_in(self):
        cls = get_test_cls()

        tests = (
            cls(True, 1000001, 1, float("inf"), 8),
            cls(False, 999999, 1, float("inf"), 8),
            cls(True, 1000000, 1, 2, 8),
            cls(False, 2000000, 1, 2, 8),
        )
        for idx, test in enumerate(tests):
            r = StepRewardCondition(test.lower, test.upper, test.pct)
            self.assertEqual(
                test.result, r.is_in(test.test_value), "{}. Did not get {} for: {}".format(idx, test.result, test)
            )

    def test_calc(self):
        cls = get_test_cls()

        tests = (
            cls(Decimal('40000'), 1500000, 1, 2, 8),
            cls(0, 2000000, 1, 2, 8),
            # 8000 (base_amt) + (100k - 100k)*8.25%
            cls(8000, 100000, 100, 150, 8.25, 8000, StepRewardCondition.K),
            # 8000 (base_amt) + (120k - 100k)*8.25%
            cls(9650, 120000, 100, 150, 8.25, 8000, StepRewardCondition.K),
        )
        for idx, test in enumerate(tests):
            r = StepRewardCondition(test.lower, test.upper, test.pct, base_amt=test.base_amt, scale=test.scale)
            res = r.calc(test.test_value)
            self.assertEqual(test.result, res, "{}: got={}, want={}. for: {}/{}".format(idx, res, test.result, test, r))
