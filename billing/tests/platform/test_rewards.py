import unittest

from agency_rewards.rewards.platform.rewards import is_need_fin_docs_reward

from . import create_bunker


class TestPlatformRewards(unittest.TestCase):
    def test_is_need_fin_docs_reward(self):
        test_env = [
            {
                'name': 'fin_docs',
                'value': '//home/comdep/fin_docs',
            }
        ]
        self.assertTrue(is_need_fin_docs_reward(create_bunker({'scale': '2', 'freq': 'm', 'env': test_env})))
        self.assertFalse(is_need_fin_docs_reward(create_bunker({'scale': '1', 'freq': 'm', 'env': test_env})))
        self.assertFalse(is_need_fin_docs_reward(create_bunker({'scale': '2', 'freq': 'm', 'env': []})))
