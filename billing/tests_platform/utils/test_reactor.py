import argparse
import datetime

import reactor_client.reactor_objects as r_objs

from agency_rewards.rewards.config import Config
from agency_rewards.rewards.calculate import refresh_reports_mvs
from billing.agency_rewards.tests_platform.common import TestBase


class TestReactor(TestBase):
    def test_refresh_reports_mvs(self):
        """
        Проставляет артефакт в реакторе и проверяет, что вызванная им реакция запустилась и успешно отработала.
        """
        self.assertIsNotNone(Config.reactor_client)
        self.assertEqual("/billing/yb-ar/dev/fake_composite_reaction", Config.reactor_client.COMPOSITE_REACTION_PATH)
        self.assertEqual("/billing/yb-ar/dev/fake_composite_artifact", Config.reactor_client.COMPOSITE_ARTIFACT_PATH)
        opt = argparse.Namespace(refresh_with_local_dwh=False, no_mv_refresh=False)
        refresh_reports_mvs(None, opt, timeout=60 * 15, sleep_seconds=10)
        last_instance = Config.reactor_client.get_last_reaction_instance(Config.reactor_client.COMPOSITE_REACTION_PATH)
        self.assertIsNotNone(last_instance.completion_time)
        self.assertTrue(last_instance.completion_time > datetime.datetime.now() - datetime.timedelta(minutes=3))
        self.assertEqual(r_objs.ReactionInstanceStatus.COMPLETED, last_instance.status)
