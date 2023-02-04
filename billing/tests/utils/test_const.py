import unittest

from agency_rewards.rewards.utils.const import (
    RewardType,
    ARCalcType,
    CalcFreq,
)


class TestConstRewardType(unittest.TestCase):
    def test_get_acts_reward_type(self):
        tests = (
            dict(src=(ARCalcType.Rewards, CalcFreq.monthly), res=RewardType.MonthActs),
            dict(src=(ARCalcType.Rewards, CalcFreq.quarterly), res=RewardType.Quarter),
            dict(src=(ARCalcType.Rewards, CalcFreq.half_yearly), res=RewardType.HalfYear),
            dict(src=(ARCalcType.Commissions, CalcFreq.monthly), res=RewardType.CommissionActs),
            dict(src=(ARCalcType.Commissions, CalcFreq.quarterly), res=RewardType.CommissionQuarter),
            dict(src=(ARCalcType.Commissions, CalcFreq.half_yearly), res=RewardType.CommissionHalfYear),
        )

        for i, t in enumerate(tests):
            self.assertEqual(RewardType.get_acts_reward_type(*t['src']), t['res'], f'Error ar {i}')
