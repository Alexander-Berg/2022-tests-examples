import pytest

from infra.walle.server.tests.lib.util import monkeypatch_method
from infra.walle.server.tests.scenario.utils import get_scenario_params, make_mock_host_stage, mock_scenario
from walle.scenario.constants import ConditionFunction, StageName
from walle.scenario.marker import Marker
from walle.scenario.scenario import Scenario
from walle.scenario.stage.conditional_host_stage import ConditionalHostStage, ChoiceFuncRegistry
from walle.scenario.stage_info import StageInfo
from walle.scenario.stages import RebootHostStage, HostRootStage, ScenarioRootStage, StageRegistry


@pytest.fixture
def mock_registry(mp):
    mp.setattr(ChoiceFuncRegistry, "ITEMS", ChoiceFuncRegistry.ITEMS.copy())
    monkeypatch_method(mp, ChoiceFuncRegistry.get, obj=ChoiceFuncRegistry, return_value=func_return_true)
    mp.setattr(StageRegistry, "ITEMS", StageRegistry.ITEMS.copy())


def func_return_true(*args):
    return True


def func_return_false(*args):
    return False


class TestLambdaHostStage:
    TICKET_KEY = "TEST-1"

    def test_stage_serialize_and_deserialize(self):
        script = ScenarioRootStage(
            [ConditionalHostStage([RebootHostStage()], condition_func=ConditionFunction.ITDC_MAINTENANCE_REBOOT_NEEDED)]
        )
        serialized_script = script.serialize()

        assert serialized_script.stages[0].name == StageName.ConditionalHostStage
        assert serialized_script.stages[0].stages[0].name == StageName.RebootHostStage

        deserialized_script = serialized_script.deserialize()

        assert isinstance(deserialized_script.children[0], ConditionalHostStage)
        assert isinstance(deserialized_script.children[0].children[0], RebootHostStage)

    @pytest.mark.parametrize(["func", "result"], [(func_return_false, False), (func_return_true, True)])
    def test_check_condition(self, mp, func, result):
        monkeypatch_method(mp, ChoiceFuncRegistry.get, obj=ChoiceFuncRegistry, return_value=func)

        stage = ConditionalHostStage([make_mock_host_stage()], condition_func=str(func))
        scenario = Scenario(ticket_key=self.TICKET_KEY, **get_scenario_params())
        stage_info = StageInfo()
        assert stage._check_condition(stage_info, scenario, None) == result
        assert stage_info.data[stage.CONDITION_CHECKED] is True

    def test_simple_run(self, walle_test, mock_registry):
        host_script = HostRootStage(
            [
                ConditionalHostStage([make_mock_host_stage()], condition_func=func_return_true),
                ConditionalHostStage([make_mock_host_stage()], condition_func=func_return_true),
            ]
        )
        stage_info = host_script.serialize()
        scenario_stage_info = host_script.serialize()

        host = walle_test.mock_host()
        scenario = mock_scenario()

        assert host_script.run(stage_info, scenario, host, scenario_stage_info) == Marker.in_progress(
            message="Stage runs child stages"
        )
        assert host_script.run(stage_info, scenario, host, scenario_stage_info) == Marker.success(
            message="Stage has successfully executed all of its child stages"
        )
