import json
import traceback
from unittest import mock
from unittest.mock import patch

from infra.walle.server.tests.lib.util import monkeypatch_function
from infra.walle.server.tests.scenario.utils import mock_scenario
from walle.errors import ErrorType
from walle.errors import WalleError
from walle.models import monkeypatch_timestamp
from walle.scenario.error_handlers import scenario_root_stage_error_handler
from walle.scenario.mixins import BaseStage
from walle.scenario.scenario import StageError
from walle.scenario.stages import ScenarioRootStage

MOCK_STACKTRACE_STRING = ["some stacktrace here"]


def make_mock_stage_always_exc():
    class MockStage(BaseStage):
        run = mock.Mock(side_effect=WalleError)

    return MockStage()


class TestScenarioRootStageErrorHandling:
    def test_get_exception_from_first_level(self, mock_stage_registry, mp):
        monkeypatch_timestamp(mp, cur_time=0)
        monkeypatch_function(mp, traceback.format_exception, module=traceback, return_value=MOCK_STACKTRACE_STRING)

        script = ScenarioRootStage([make_mock_stage_always_exc()])

        scenario = mock_scenario(stage_info=script.serialize())

        with scenario_root_stage_error_handler(scenario.stage_info, scenario):
            script.run(scenario.stage_info, scenario)

        assert len(scenario.errors) == 1

        stage_error = next(iter(scenario.errors.values()))
        waited_stage_error = StageError(
            id='0_0 <-> None',
            repeats=1,
            type=ErrorType.FIXABLE,
            is_visible=True,
            retry_period=100,
            exc_info={
                "stage_name": "MockStage",
                "stage_action": "action",
                "exc_id": "0_0 <-> None",
                "timestamp": 0,
                "error_message": "None",
                "stage_uid": "0.0",
            },
            stacktrace=MOCK_STACKTRACE_STRING,
        )

        assert json.loads(waited_stage_error.to_json()) == json.loads(stage_error.to_json())

    def test_get_exception_from_second_level(self, mock_stage_registry, mp):
        monkeypatch_timestamp(mp, cur_time=0)
        monkeypatch_function(mp, traceback.format_exception, module=traceback, return_value=MOCK_STACKTRACE_STRING)

        script = ScenarioRootStage([ScenarioRootStage([make_mock_stage_always_exc()])])

        scenario = mock_scenario(stage_info=script.serialize())

        with scenario_root_stage_error_handler(scenario.stage_info, scenario):
            script.run(scenario.stage_info, scenario)

        assert len(scenario.errors) == 1

        stage_error = next(iter(scenario.errors.values()))
        waited_stage_error = StageError(
            id='0_0_0 <-> None',
            repeats=1,
            type=ErrorType.FIXABLE,
            is_visible=True,
            retry_period=100,
            exc_info={
                "stage_name": "MockStage",
                "stage_action": "action",
                "exc_id": "0_0_0 <-> None",
                "timestamp": 0,
                "error_message": "None",
                "stage_uid": "0.0.0",
            },
            stacktrace=MOCK_STACKTRACE_STRING,
        )

        assert json.loads(waited_stage_error.to_json()) == json.loads(stage_error.to_json())


class TestErrorCleanup:
    class MockStage(BaseStage):
        def run(self, *args, **kwargs):
            pass

    def test_error_cleanup(self, mock_stage_registry, mp):
        monkeypatch_timestamp(mp, cur_time=0)
        monkeypatch_function(mp, traceback.format_exception, module=traceback, return_value=MOCK_STACKTRACE_STRING)

        script = ScenarioRootStage([self.MockStage()])

        scenario = mock_scenario(stage_info=script.serialize())

        with patch.object(self.MockStage, 'run', side_effect=WalleError):
            with scenario_root_stage_error_handler(scenario.stage_info, scenario):
                script.run(scenario.stage_info, scenario)

            assert len(scenario.errors) == 1

            stage_error = next(iter(scenario.errors.values()))
            waited_stage_error = StageError(
                id='0_0 <-> None',
                repeats=1,
                type=ErrorType.FIXABLE,
                is_visible=True,
                retry_period=100,
                exc_info={
                    "stage_name": "MockStage",
                    "stage_action": "action",
                    "exc_id": "0_0 <-> None",
                    "timestamp": 0,
                    "error_message": "None",
                    "stage_uid": "0.0",
                },
                stacktrace=MOCK_STACKTRACE_STRING,
            )

            assert json.loads(waited_stage_error.to_json()) == json.loads(stage_error.to_json())

        with patch.object(self.MockStage, 'run'):
            with scenario_root_stage_error_handler(scenario.stage_info, scenario):
                script.run(scenario.stage_info, scenario)

            assert len(scenario.errors) == 0
