import http.client

from walle.scenario.stage_info import StageInfo, StageStatus


def test_get_scenario_snapshot(walle_test):
    complex_structures = dict(params={"key1": {"key2": "val2", "key3": "val3"}}, data={"datablock": "val"})
    stage_info_inner = StageInfo(uid="2", name="2", status=StageStatus.QUEUE, **complex_structures)
    stage_info_middle = StageInfo(uid="1", stages=[stage_info_inner], name="1", **complex_structures)
    stage_info = StageInfo(uid="0", stages=[stage_info_middle], **complex_structures)

    scenario = walle_test.mock_scenario(dict(stage_info=stage_info))

    result = walle_test.api_client.get("/v1/scenarios/0/snapshot")
    assert result.status_code == http.client.OK
    assert scenario.stage_info.to_mongo().to_dict() == result.json


def test_get_state_of_not_existing_scenario(walle_test):
    result = walle_test.api_client.get("/v1/scenarios/0/snapshot")
    assert result.status_code == http.client.NOT_FOUND
