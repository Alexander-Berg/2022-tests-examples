from unittest import mock

import pytest

from infra.walle.server.tests.lib.util import mock_schedule_host_reboot
from infra.walle.server.tests.scenario.utils import get_scenario_params, mock_scenario, make_mock_stage
from walle.constants import PROVISIONER_LUI
from walle.hosts import HostState, HostStatus, TaskType
from walle.scenario.constants import DEFAULT_STAGE_DESCRIPTION
from walle.scenario.constants import ScriptName
from walle.scenario.marker import Marker, MarkerStatus
from walle.scenario.mixins import BaseStage
from walle.scenario.mixins import ParentStageHandler
from walle.scenario.scenario import Scenario
from walle.scenario.stage_info import StageInfo
from walle.scenario.stage_info import StageStatus, StageRegistry
from walle.scenario.stages import RebootHostStage


class MockHandlerStage(ParentStageHandler):
    def run(self, stage_info, scenario, *args, **kwargs):
        return self.execute_current_stage(stage_info, scenario, *args, **kwargs)

    def cleanup(self, stage_info, scenario):
        raise ValueError("MockHandlerStage cleanup launched")


class StageHandlerMixinTest:
    def test_make_transition_for_last_finishes_handler(self, mock_stage_registry):
        StageRegistry.register(MockHandlerStage)
        script = MockHandlerStage([make_mock_stage(), make_mock_stage()])
        scenario = mock_scenario(stage_info=script.serialize())
        scenario.stage_info.seq_num = 1
        assert script.run(scenario.stage_info, scenario).status == MarkerStatus.SUCCESS

    @pytest.mark.parametrize(["seq_num", "marker_status"], [(0, MarkerStatus.IN_PROGRESS), (1, MarkerStatus.SUCCESS)])
    def test_make_transition_after_manually_changed_stage_status(self, mock_stage_registry, seq_num, marker_status):
        StageRegistry.register(MockHandlerStage)
        script = MockHandlerStage([make_mock_stage(), make_mock_stage()])
        scenario = mock_scenario(stage_info=script.serialize())
        scenario.stage_info.seq_num = seq_num
        scenario.stage_info.stages[seq_num].status = StageStatus.FINISHED
        assert script.run(scenario.stage_info, scenario).status == marker_status

    def test_make_transition_sets_next_stage(self):
        StageRegistry.register(MockHandlerStage)
        script = MockHandlerStage([make_mock_stage(), make_mock_stage(), make_mock_stage()])
        scenario = mock_scenario(stage_info=script.serialize())
        scenario.stage_info.seq_num = 1
        assert script.run(scenario.stage_info, scenario).status == MarkerStatus.IN_PROGRESS

    def test_process_marker_success(self):
        pass

    @pytest.mark.parametrize("marker_status", set(MarkerStatus) - {MarkerStatus.SUCCESS})
    def test_process_marker_no_success(self, marker_status):
        pass

    def test_serialize(self):
        mock_stage = make_mock_stage()

        script = MockHandlerStage(mock_stage, mock_stage)
        expected = StageInfo(
            name="MockHandlerStage", seq_num=0, stages=[mock_stage.serialize(), mock_stage.serialize()]
        )
        result = script.serialize()
        assert result == expected
        assert result.description == DEFAULT_STAGE_DESCRIPTION

    def test_custom_description_from_stage_falls_into_stage_info(self):
        class MockStage(BaseStage):
            """test description"""

            run = mock.Mock()

        mock_stage = MockStage()

        script = MockHandlerStage(mock_stage, mock_stage)
        expected = StageInfo(
            name="MockHandlerStage",
            description="test description",
            seq_num=0,
            stages=[mock_stage.serialize(), mock_stage.serialize()],
        )
        result = script.serialize()
        assert result == expected


class TestIgnoreCmsOption:

    TICKET_KEY = "TEST-1"

    def _prepare_stage_scenario_and_stage_info(self, ignore_cms=False):
        stage = RebootHostStage(ignore_cms=ignore_cms)
        scenario = Scenario(ticket_key=self.TICKET_KEY, **get_scenario_params())
        stage_info = StageInfo()
        return stage, scenario, stage_info

    def test_run_for_implicitly_given_ignore_value(self, walle_test, monkeypatch_audit_log):
        stage, scenario, stage_info = self._prepare_stage_scenario_and_stage_info(ignore_cms=True)
        host = walle_test.mock_host(
            dict(status=HostStatus.READY, state=HostState.ASSIGNED, provisioner=PROVISIONER_LUI, config="mock-config")
        )

        result = stage.run(stage_info, scenario, host)

        mock_schedule_host_reboot(
            host,
            manual=False,
            reason=scenario.ticket_key,
            with_auto_healing=True,
            task_type=TaskType.AUTOMATED_ACTION,
            ignore_cms=True,
        )

        assert result == Marker.in_progress(message="Launched a task that reboots the host")
        walle_test.hosts.assert_equal()

    def test_run_for_host_with_default_cms(self, walle_test, monkeypatch_audit_log):
        stage, scenario, stage_info = self._prepare_stage_scenario_and_stage_info(ignore_cms=False)
        host = walle_test.mock_host(
            dict(status=HostStatus.READY, state=HostState.ASSIGNED, provisioner=PROVISIONER_LUI, config="mock-config")
        )

        result = stage.run(stage_info, scenario, host)

        mock_schedule_host_reboot(
            host,
            manual=False,
            reason=scenario.ticket_key,
            with_auto_healing=True,
            task_type=TaskType.AUTOMATED_ACTION,
            ignore_cms=True,
        )

        assert result == Marker.in_progress(message="Launched a task that reboots the host")
        walle_test.hosts.assert_equal()

    @pytest.mark.parametrize("ignore_cms", [True, False])
    def test_run_with_option_from_maintenance_plot(self, walle_test, ignore_cms, monkeypatch_audit_log):
        plot = walle_test.mock_maintenance_plot(
            dict(
                scenarios_settings=[
                    {
                        "scenario_type": ScriptName.ITDC_MAINTENANCE,
                        "settings": {"ignore_cms_on_host_operations": ignore_cms},
                    }
                ]
            )
        )

        stage = RebootHostStage(ignore_cms=False)
        scenario = Scenario(ticket_key=self.TICKET_KEY, scenario_type=ScriptName.ITDC_MAINTENANCE)
        stage_info = StageInfo()

        scenario.save(validate=False)

        project = walle_test.mock_project(
            dict(maintenance_plot_id=plot.id, id="test-cms-option", cms_settings=[{"cms": "non-default"}])
        )
        host = walle_test.mock_host(
            dict(
                status=HostStatus.READY,
                state=HostState.ASSIGNED,
                provisioner=PROVISIONER_LUI,
                config="mock-config",
                project=project.id,
            )
        )

        result = stage.run(stage_info, scenario, host)

        mock_schedule_host_reboot(
            host,
            manual=False,
            reason=scenario.ticket_key,
            with_auto_healing=True,
            task_type=TaskType.AUTOMATED_ACTION,
            ignore_cms=ignore_cms,
        )

        assert result == Marker.in_progress(message="Launched a task that reboots the host")
        walle_test.hosts.assert_equal()
