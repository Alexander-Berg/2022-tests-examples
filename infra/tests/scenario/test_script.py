import pytest

from walle import projects
from walle.scenario.constants import ScriptArgs, ScriptName
from walle.scenario.errors import ScenarioValidationError
from walle.scenario.mixins import Stage, ParentStageHandler
from walle.scenario.script import ScriptRegistry
from walle.scenario.script_args import AddHostsParams


class MockStage(Stage):
    pass


class MockRootStage(ParentStageHandler, Stage):
    pass


def collect_stage_names(stage_info):
    composite_stage = dict()
    composite_stage[stage_info.name] = []
    if stage_info.stages:
        for stage in stage_info.stages:
            result = collect_stage_names(stage)
            composite_stage[stage_info.name].append(result)
    return composite_stage


def get_serialized_stages(root_stage):
    serialized_stages = root_stage.serialize()
    stage_names = collect_stage_names(serialized_stages)
    return stage_names


def test_script_serialize_one_child_stage():
    def example_script():
        return MockRootStage([MockStage()])

    test_value = {"MockRootStage": [{"MockStage": []}]}

    stage_names = get_serialized_stages(example_script())
    assert stage_names == test_value


def test_script_serialize_few_child_stage():
    def example_script():
        return MockRootStage([MockStage() for _ in range(3)])

    test_value = {"MockRootStage": [{"MockStage": []}, {"MockStage": []}, {"MockStage": []}]}

    stage_names = get_serialized_stages(example_script())
    assert stage_names == test_value


def test_script_serialize_with_many_composite_stages():
    def example_script():
        return MockRootStage(
            [MockRootStage([MockRootStage([MockStage()]), MockStage()]), MockRootStage([MockRootStage([MockStage()])])]
        )

    test_value = {
        "MockRootStage": [
            {"MockRootStage": [{"MockRootStage": [{"MockStage": []}]}, {"MockStage": []}]},
            {"MockRootStage": [{"MockRootStage": [{"MockStage": []}]}]},
        ]
    }

    stage_names = get_serialized_stages(example_script())
    assert stage_names == test_value


def test_script_serialize_mixed_stages():
    def example_script():
        return MockRootStage([MockStage(), MockStage(), MockRootStage([MockStage(), MockStage()]), MockStage()])

    test_value = {
        "MockRootStage": [
            {"MockStage": []},
            {"MockStage": []},
            {"MockRootStage": [{"MockStage": []}, {"MockStage": []}]},
            {"MockStage": []},
        ]
    }

    stage_names = get_serialized_stages(example_script())
    assert stage_names == test_value


def test_wait_script(walle_test):
    project_id = "mock-project"
    walle_test.mock_project({"id": project_id})
    script = ScriptRegistry.get("wait")
    stage_names = get_serialized_stages(script({ScriptArgs.TARGET_PROJECT: project_id}))
    test_value = {
        "ScenarioRootStage": [
            {"AcquirePermission": []},
            {"HostSchedulerStage": [{"HostRootStage": [{"WaitStateStatusHostStage": []}]}]},
        ]
    }
    assert stage_names == test_value


def check_uid(stage_info, val):
    assert stage_info.uid == val
    for idx, child in enumerate(stage_info.stages):
        check_uid(child, "{}.{}".format(val, idx))


def test_set_uid_for_stage_info_in_right_order(walle_test):
    def example_script():
        return MockRootStage([MockStage(), MockStage(), MockRootStage([MockStage(), MockStage()]), MockStage()])

    root_stage_info = example_script().serialize()
    check_uid(root_stage_info, "0")


@pytest.mark.parametrize(
    "target_project_id,target_hardware_segment", [(None, "ext.mock"), ("mock", None), ("mock", "ext.mock")]
)
def test_hosts_add_params_pass(mp, walle_test, target_project_id, target_hardware_segment):
    if target_project_id is not None:
        walle_test.mock_project({"id": target_project_id})
    AddHostsParams(target_project_id=target_project_id, target_hardware_segment=target_hardware_segment)


def test_hosts_add_params_insufficient(mp, walle_test):
    with pytest.raises(ScenarioValidationError) as e:
        AddHostsParams(target_project_id=None, target_hardware_segment=None)
    assert str(e.value) == "You must specify target_project_id or target_hardware_segment"


@pytest.mark.parametrize(
    "target_project_id,target_hardware_segment,message",
    [
        (None, "mock", "Invalid Qloud segment. Got 'mock'"),
        ("bad_mock", None, "Project id 'bad_mock' does not exist"),
        ("bad_mock", "ext.mock", "Project id 'bad_mock' does not exist"),
        ("mock", "mock", "Invalid Qloud segment. Got 'mock'"),
    ],
)
def test_hosts_add_params_bad_parameters(mp, walle_test, target_project_id, target_hardware_segment, message):
    walle_test.mock_project({"id": "mock"})
    with pytest.raises(ScenarioValidationError) as e:
        AddHostsParams(target_project_id=target_project_id, target_hardware_segment=target_hardware_segment)
    assert str(e.value) == message


_MOCK_SCRIPT_PARAMS = {
    "wait": [{"target_project_id": "mock"}],
    "hosts-add": [{"target_project_id": "mock"}],
    "hosts-transfer": [{"target_project_id": "mock"}, {"delete": True}],
    "reserved-hosts-transfer": [{"target_project_id": "mock"}],
    "wait-time": [{"target_project_id": "mock"}],
    ScriptName.NOC_SOFT: [{"switch": "some-switch"}],
}


@pytest.mark.parametrize("scenario_type", ScriptRegistry.get_keys())
def test_all_scenarios_can_be_deserialized(mp, scenario_type):
    mp.function(projects.get_by_id)  # stub project id validator
    script_func = ScriptRegistry.get(scenario_type)

    for params in _MOCK_SCRIPT_PARAMS.get(scenario_type, [{}]):
        script = script_func(params)

        root_stage_info = script.serialize()
        deserialized_script = root_stage_info.deserialize()

        assert deserialized_script == script
