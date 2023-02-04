from infra.walle.server.tests.lib.util import monkeypatch_locks, find_host_scheduler_stage
from infra.walle.server.tests.scenario.utils import make_mock_stage, mock_scenario, make_mock_host_stage
from walle.scenario.handlers import modify_stage_info_by_uid, skip_wait_stage_for_hosts_transfer
from walle.scenario.host_stage_info import HostStageInfo
from walle.scenario.scenario import Scenario
from walle.scenario.script import hosts_transfer_script
from walle.scenario.stage_info import StageStatus, StageInfo
from walle.scenario.stages import ScenarioRootStage, HostRootStage


def modify_function(stage_info: StageInfo):
    stage_info.status = StageStatus.FINISHED


class TestModifyStageInfo:
    def test_simple_modify_child_of_srs(self, walle_test, mp):
        monkeypatch_locks(mp)
        script = ScenarioRootStage([make_mock_stage(), make_mock_stage()])

        scenario = mock_scenario(stage_info=script.serialize())
        scenario.save()

        assert scenario.stage_info.stages[0].status == StageStatus.QUEUE

        uid = scenario.stage_info.stages[0].uid
        modify_stage_info_by_uid(scenario, uid, modify_function=modify_function)

        scenario = Scenario.objects.get(scenario_id=scenario.scenario_id)
        assert scenario.stage_info.stages[0].status == StageStatus.FINISHED

    def test_complex_modify_child_of_srs(self, walle_test, mp):
        monkeypatch_locks(mp)
        script = ScenarioRootStage([ScenarioRootStage([make_mock_stage()]), make_mock_stage()])
        scenario = mock_scenario(stage_info=script.serialize())
        scenario.save()

        assert scenario.stage_info.stages[0].stages[0].status == StageStatus.QUEUE

        uid = scenario.stage_info.stages[0].stages[0].uid
        modify_stage_info_by_uid(scenario, uid, modify_function=modify_function)

        scenario = Scenario.objects.get(scenario_id=scenario.scenario_id)
        assert scenario.stage_info.stages[0].stages[0].status == StageStatus.FINISHED

    def test_try_to_modify_root_stage_with_id_non_equal_zero(self, walle_test, mp):
        monkeypatch_locks(mp)
        script = ScenarioRootStage([make_mock_stage()])
        scenario = mock_scenario(stage_info=script.serialize())
        scenario.save()

        uid = "1"
        assert (
            modify_stage_info_by_uid(scenario, uid, modify_function=modify_function)
            == "Root level has only stage with id 0"
        )

    def test_hrs(self, walle_test, mp):
        monkeypatch_locks(mp)
        script = ScenarioRootStage(
            [
                HostRootStage([make_mock_host_stage()]),
            ]
        )
        scenario = mock_scenario(stage_info=script.serialize())
        scenario.save()

        host = walle_test.mock_host()
        hsi = HostStageInfo(
            host_uuid=host.uuid, scenario_id=scenario.scenario_id, stage_info=scenario.stage_info.stages[0]
        )
        hsi.save()

        assert scenario.stage_info.stages[0].stages[0].status == StageStatus.QUEUE
        assert hsi.stage_info.stages[0].status == StageStatus.QUEUE

        uid = scenario.stage_info.stages[0].stages[0].uid
        modify_stage_info_by_uid(scenario, uid, modify_function=modify_function)

        scenario = Scenario.objects.get(scenario_id=scenario.scenario_id)
        assert scenario.stage_info.stages[0].stages[0].status == StageStatus.FINISHED

        hsi = HostStageInfo.objects.get(host_uuid=host.uuid)
        assert hsi.stage_info.stages[0].status == StageStatus.FINISHED


def test_skip_wait_stage_for_hosts_transfer(walle_test, mp):
    monkeypatch_locks(mp)
    scenario = mock_scenario(stage_info=hosts_transfer_script({"delete": True}).serialize())
    scenario.save()

    host_scheduler_stage = find_host_scheduler_stage(scenario.stage_info.stages)

    host = walle_test.mock_host()
    hsi = HostStageInfo(
        host_uuid=host.uuid, scenario_id=scenario.scenario_id, stage_info=host_scheduler_stage.stages[0]
    )
    hsi.save()

    assert host_scheduler_stage.stages[0].stages[2].status == StageStatus.QUEUE
    assert hsi.stage_info.stages[2].status == StageStatus.QUEUE

    skip_wait_stage_for_hosts_transfer(scenario)

    scenario = Scenario.objects.get(scenario_id=scenario.scenario_id)
    host_scheduler_stage = find_host_scheduler_stage(scenario.stage_info.stages)
    assert host_scheduler_stage.stages[0].stages[2].status == StageStatus.FINISHED

    hsi = HostStageInfo.objects.get(host_uuid=host.uuid)
    assert hsi.stage_info.stages[2].status == StageStatus.FINISHED
