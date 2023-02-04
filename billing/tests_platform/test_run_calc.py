import sqlalchemy as sa
from datetime import datetime

from agency_rewards.rewards.scheme import run_calc

from billing.agency_rewards.tests_platform.common import TestBase
from billing.agency_rewards.tests_platform.common import get_bunker_calc
from agency_rewards.rewards.config import Config


class TestRunCalcInvariants(TestBase):
    def test_invariants(self):
        run_id = self.get_last_run_id()

        run_calcs = self.session.execute(
            sa.select(
                [
                    run_calc.c.start_dt,
                    run_calc.c.finish_dt,
                    run_calc.c.shared_yql,
                    run_calc.c.error,
                    run_calc.c.cluster_name,
                    run_calc.c.is_origin,
                ]
            ).where(run_calc.c.run_id == run_id)
        ).fetchall()

        for calc in run_calcs:
            self.assertIsNotNone(calc.start_dt, f"{run_id=}")
            self.assertIsNotNone(calc.finish_dt, f"{run_id=}")
            self.assertLessEqual(calc.start_dt, calc.finish_dt, f"{run_id=}")

            if calc.error is not None:
                self.assertFalse(calc.is_origin, f"{run_id=}")

            if calc.shared_yql is None:
                self.assertFalse(calc.is_origin, f"{run_id=}")
                self.assertTrue(calc.error is not None or calc.cluster_name is None, f"{run_id=}")

            if calc.cluster_name is None:
                self.assertFalse(calc.is_origin, f"{run_id=}")
                self.assertIsNone(calc.shared_yql, f"{run_id=}")
            else:
                self.assertEqual(calc.cluster_name, Config.clusters[0].name, f"{run_id=}")


class TestArtifact(TestBase):
    def test_artifact_presence(self):
        import reactor_client.reactor_objects as r_objs

        bunker_calc = get_bunker_calc('/agency-rewards/dev/regression/market/monthly_agency_rewards')
        for paths in bunker_calc.artifact_paths:
            for cluster in Config.clusters:
                namespace_identifier = r_objs.NamespaceIdentifier(namespace_path=paths.artifact_path)
                response = Config.reactor_client.instantiate_artifact_yt_path(
                    paths.yt_path, paths.artifact_path, datetime.now(), cluster=cluster.name
                )
                self.assertEqual(response, '', msg=response)
                self.assertTrue(
                    Config.reactor_client.client.artifact.check_exists(namespace_identifier=namespace_identifier)
                )
