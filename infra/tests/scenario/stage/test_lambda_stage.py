from infra.walle.server.tests.scenario.utils import get_scenario_params
from walle.scenario.constants import SharedDataKey, StageName
from walle.scenario.marker import MarkerStatus
from walle.scenario.scenario import Scenario
from walle.scenario.stage.lambda_stage import LambdaStage, FindActiveMacAddress
from walle.scenario.stage_info import StageInfo
from walle.scenario.stages import ScenarioRootStage


class TestLambdaHostStage:
    TICKET_KEY = "TEST-1"

    def test_stage_serialize_and_deserialize(self):
        lambda_stage = LambdaStage(SharedDataKey.HOST_NETWORK_INFO, FindActiveMacAddress.name)
        script = ScenarioRootStage([lambda_stage])
        serialized_script = script.serialize()

        assert serialized_script.stages[0].name == StageName.LambdaStage
        assert serialized_script.stages[0].params == dict(
            shared_data_key=SharedDataKey.HOST_NETWORK_INFO, operation_name=FindActiveMacAddress.name
        )

        deserialized_script = serialized_script.deserialize()

        assert isinstance(deserialized_script.children[0], LambdaStage)
        assert deserialized_script.children[0].operation_name == lambda_stage.operation_name
        assert deserialized_script.children[0].shared_data_key == lambda_stage.shared_data_key

    def test_lambda_stage_run_without_active_mac(self, walle_test):
        stage = LambdaStage(SharedDataKey.HOST_NETWORK_INFO, FindActiveMacAddress().name)
        scenario = Scenario(ticket_key=self.TICKET_KEY, **get_scenario_params())
        stage_info = StageInfo()
        host = walle_test.mock_host()

        result = stage.run(stage_info, scenario, host)
        assert {SharedDataKey.HOST_NETWORK_INFO: None} == result.data and result.status == MarkerStatus.SUCCESS

    def test_lambda_stage_run_with_active_mac(self, walle_test):
        stage = LambdaStage(SharedDataKey.HOST_NETWORK_INFO, FindActiveMacAddress().name)
        scenario = Scenario(ticket_key=self.TICKET_KEY, **get_scenario_params())
        stage_info = StageInfo()
        host = walle_test.mock_host({"uuid": str(1), "inv": 1})
        walle_test.mock_host_network(dict(uuid="1", active_mac="0"))

        result = stage.run(stage_info, scenario, host)
        assert {SharedDataKey.HOST_NETWORK_INFO: "0"} == result.data and result.status == MarkerStatus.SUCCESS
