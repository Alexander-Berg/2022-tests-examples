from walle.scenario.host_stage_info import HostStageInfo
from walle.scenario.hsi_gc import _gc_old_host_stage_info
from walle.scenario.scenario import ScenarioFsmStatus


def test_gc_old_host_stage_info(walle_test):
    walle_test.mock_host({"uuid": "1"})
    walle_test.mock_host({"uuid": "2", "inv": 1, "scenario_id": 2})
    walle_test.mock_scenario({"scenario_id": 1, "status": ScenarioFsmStatus.CANCELED})
    walle_test.mock_scenario({"scenario_id": 2, "status": ScenarioFsmStatus.PAUSED, "name": "2"})

    HostStageInfo(host_uuid="1", scenario_id=1).save()
    HostStageInfo(host_uuid="2", scenario_id=2).save()
    HostStageInfo(host_uuid="3", scenario_id=1).save()
    HostStageInfo(host_uuid="4", scenario_id=2).save()

    assert HostStageInfo.objects.count() == 4

    _gc_old_host_stage_info()

    assert HostStageInfo.objects.count() == 1
    assert HostStageInfo.objects.get(host_uuid="2")
