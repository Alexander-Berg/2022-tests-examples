from infra.walle.server.tests.scenario.utils import get_test_scenario_document_with_stage_info
from walle.scenario.mixins import ParentStageHandler, BaseStage
from walle.scenario.stage_info import StageStatus, StageInfo, StageRegistry


class TestStageInfo:
    def test_set_stage_finished(self, walle_test):
        document = get_test_scenario_document_with_stage_info()
        stage_info = document.stage_info.stages[0]
        assert stage_info.status != StageStatus.FINISHED
        stage_info.set_stage_finished()
        assert document.stage_info.stages[0].status == StageStatus.FINISHED

    def test_set_stage_is_done(self, walle_test):
        document = get_test_scenario_document_with_stage_info()
        assert document.stage_info.status != StageStatus.FINISHED

        document.stage_info.set_stage_finished()
        assert document.stage_info.status == StageStatus.FINISHED

    def test_is_on_last_stage(self):
        size = 3
        scenario = get_test_scenario_document_with_stage_info(size)
        scenario.stage_info.seq_num = size - 1
        assert scenario.stage_info.is_on_last_stage()

    def test_is_not_on_last_stage(self):
        scenario = get_test_scenario_document_with_stage_info(3)
        assert scenario.stage_info.seq_num == 0
        assert not scenario.stage_info.is_on_last_stage()

    def test_deserialize(self, mock_stage_registry):
        class MockHandlerStage(ParentStageHandler, BaseStage):
            pass

        class TestMockStage(BaseStage):
            pass

        StageRegistry.register(MockHandlerStage.__name__)(MockHandlerStage)
        StageRegistry.register(TestMockStage.__name__)(TestMockStage)

        stage_info = StageInfo(
            name="MockHandlerStage",
            seq_num=1,
            params={"a": 1},
            stages=[
                StageInfo(name="TestMockStage", params={"b": 2}),
                StageInfo(name="TestMockStage", params={"b": 3}),
            ],
        )
        actual = stage_info.deserialize()
        expected = MockHandlerStage([TestMockStage(**{"b": 2}), TestMockStage(**{"b": 3})], **{"a": 1})

        assert isinstance(actual, type(expected))
        assert actual.params == expected.params

        for i in range(len(expected.children)):
            assert isinstance(actual.children[i], type(expected.children[i]))
            assert actual.children[i].params == expected.children[i].params
