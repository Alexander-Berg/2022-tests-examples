import unittest

from agency_rewards.rewards.utils.const import Scale
from agency_rewards.rewards.scheme import (
    base_rewards,
    prof_rewards,
    kazakh_rewards,
    belarus_rewards,
)
from agency_rewards.rewards.platform.utils import get_target_table_by_scale


class TestPlatformUtils(unittest.TestCase):
    def test_get_table_by_scale(self):
        self.assertEqual(get_target_table_by_scale(Scale.BaseMsk.value), base_rewards)
        self.assertEqual(get_target_table_by_scale(Scale.Prof.value), prof_rewards)
        self.assertEqual(get_target_table_by_scale(Scale.Prof20.value), prof_rewards)
        self.assertEqual(get_target_table_by_scale(Scale.MarketRegions.value), base_rewards)
        self.assertEqual(get_target_table_by_scale(Scale.MarketMskSpb.value), base_rewards)
        self.assertEqual(get_target_table_by_scale(Scale.Kazakhstan.value), kazakh_rewards)
        self.assertEqual(get_target_table_by_scale(Scale.Belarus.value), belarus_rewards)
        self.assertEqual(get_target_table_by_scale(Scale.Prof22.value), prof_rewards)
        self.assertEqual(get_target_table_by_scale(Scale.SpecDAN.value), prof_rewards)
