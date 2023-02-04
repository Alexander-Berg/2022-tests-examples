from agency_rewards.rewards.config import Config
from billing.agency_rewards.tests_platform.common import TestBase, get_bunker_calc

import reactor_client.reactor_objects as r_objs

bunker_calc_path = '/agency-rewards/dev/regression/tasks/balance-37566'


class TestArtifactPresenceInCalcWithoutTurnoverControl(TestBase):
    """
    Проверяет наличие артефакта в реакторе по окончании рег. тестов
    """

    def test_artifact_presence(self):
        bunker_calc = get_bunker_calc(bunker_calc_path)

        for paths in bunker_calc.artifact_paths:
            namespace_identifier = r_objs.NamespaceIdentifier(namespace_path=paths.artifact_path)
            self.assertTrue(
                Config.reactor_client.client.artifact.check_exists(namespace_identifier=namespace_identifier)
            )
