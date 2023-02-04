from unittest import mock

import pytest

from infra.walle.server.tests.scenario.utils import mock_scenario
from walle.models import monkeypatch_timestamp
from walle.scenario.marker import Marker, MarkerStatus
from walle.scenario.mixins import BaseStage
from walle.scenario.stages import ScenarioRootStage

MOCK_MESSAGE = "test message"


def make_mock_stage(status):
    class MockStage(BaseStage):
        run = mock.Mock(return_value=Marker(status, message=MOCK_MESSAGE))

    return MockStage()


@pytest.mark.parametrize("status", [MarkerStatus.SUCCESS, MarkerStatus.IN_PROGRESS])
def test_message(mock_stage_registry, mp, status):
    monkeypatch_timestamp(mp, cur_time=0)
    script = ScenarioRootStage([make_mock_stage(status)])

    scenario = mock_scenario(stage_info=script.serialize())
    if status == MarkerStatus.SUCCESS:
        assert script.run(scenario.stage_info, scenario) == Marker(
            status, message="Stage has successfully executed all of its child stages"
        )
    else:
        assert script.run(scenario.stage_info, scenario) == Marker(status, message=MOCK_MESSAGE)
