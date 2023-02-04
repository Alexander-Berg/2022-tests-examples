from typing import List, Optional
from unittest import mock

import pytest

from infra.walle.server.tests.scenario.utils import mock_scenario
from sepelib.core.exceptions import LogicalError
from walle.clients import juggler as juggler_client
from walle.scenario.marker import Marker, MarkerStatus
from walle.scenario.mixins import BaseStage
from walle.scenario.stage.timeout_stage import TimeoutStage, _timeout_message
from walle.scenario.stage_info import StageInfo


class FixedTimedOutMock:
    def __init__(self, return_values: List[bool]) -> None:
        self.return_values = return_values
        self.return_value_num = 0

    def __call__(self, stage_info: StageInfo) -> bool:
        result = self.return_values[self.return_value_num]
        self.return_value_num += 1
        return result


def monkeypatch_timestamp_seq(monkeypatch, step: int, current_time: Optional[int] = None):
    class SequenceTimeMocker:
        def __init__(self, step: int, cur_time: Optional[int] = None) -> None:
            self.step = step
            if cur_time:
                self.acc = cur_time
            else:
                self.acc = 0

        def __call__(self, *args, **kwargs):
            result = self.acc
            self.acc += self.step
            return result

    time_mocker = SequenceTimeMocker(step, current_time)
    import walle.models

    monkeypatch.setattr(walle.models, "_timestamp", time_mocker)

    return time_mocker


def make_mock_stage_with_status(status=Marker.success()):
    class MockStageWithStatus(BaseStage):
        run = mock.Mock(return_value=status)

    return MockStageWithStatus()


def test_fails_on_empty_stage_list():
    with pytest.raises(LogicalError):
        stage = TimeoutStage([], 0, "")  # noqa


def test_success_stage_after_timeout(mp, send_event_mock):
    script = TimeoutStage(
        [make_mock_stage_with_status()], timeout=2, juggler_service_name="One host task", host_name="host"
    )
    script._timed_out = FixedTimedOutMock([True])
    scenario = mock_scenario(stage_info=script.serialize())
    assert script.run(scenario.stage_info, scenario).status == MarkerStatus.SUCCESS
    expected_msg = 'Timeout stage for scenario https://wall-e.yandex-team.ru/scenarios/{} finished successfully'.format(
        scenario.scenario_id
    )
    send_event_mock.assert_called_once_with(
        'One host task', juggler_client.JugglerCheckStatus.OK, expected_msg, host_name="host"
    )


def test_timeout_in_progress(send_event_mock):
    script = TimeoutStage(
        [make_mock_stage_with_status(status=Marker.in_progress())],
        timeout=2,
        juggler_service_name="One host task",
        host_name="host",
    )
    script._timed_out = FixedTimedOutMock([True])
    scenario = mock_scenario(stage_info=script.serialize())
    assert script.run(scenario.stage_info, scenario).status == MarkerStatus.IN_PROGRESS
    expected_msg = _timeout_message(script.start_time, script.timeout, scenario)
    send_event_mock.assert_called_once_with(
        'One host task', juggler_client.JugglerCheckStatus.CRIT, expected_msg, host_name="host"
    )


def test_keep_notifications_after_timeout(send_event_mock):
    script = TimeoutStage(
        [make_mock_stage_with_status(status=Marker.in_progress())],
        timeout=2,
        juggler_service_name="One host task",
        host_name="host",
    )
    script._timed_out = FixedTimedOutMock([True, True])
    scenario = mock_scenario(stage_info=script.serialize())
    for _i in range(2):
        assert script.run(scenario.stage_info, scenario).status == MarkerStatus.IN_PROGRESS
        expected_msg = _timeout_message(script.start_time, script.timeout, scenario)
        send_event_mock.assert_called_once_with(
            'One host task', juggler_client.JugglerCheckStatus.CRIT, expected_msg, host_name="host"
        )
        send_event_mock.reset_mock()


def test_keep_timer_between_runs(mp, send_event_mock):
    script = TimeoutStage(
        [make_mock_stage_with_status(status=Marker.in_progress())],
        timeout=5,
        juggler_service_name="One host task",
        host_name="host",
    )
    scenario = mock_scenario(stage_info=script.serialize())
    # timestamp() function are used in _timed_out() and inside of get_active_stage
    monkeypatch_timestamp_seq(mp, 2)
    assert script.run(scenario.stage_info, scenario).status == MarkerStatus.IN_PROGRESS
    send_event_mock.assert_not_called()
    assert script.run(scenario.stage_info, scenario).status == MarkerStatus.IN_PROGRESS
    expected_msg = _timeout_message(script.start_time, script.timeout, scenario)
    send_event_mock.assert_called_once_with(
        'One host task', juggler_client.JugglerCheckStatus.CRIT, expected_msg, host_name="host"
    )


def test_keep_timer_between_stages(mp, send_event_mock):
    script = TimeoutStage(
        [make_mock_stage_with_status(status=Marker.success()), make_mock_stage_with_status(status=Marker.success())],
        timeout=5,
        juggler_service_name="One host task",
        host_name="host",
    )
    scenario = mock_scenario(stage_info=script.serialize())
    # timestamp() function are used in _timed_out() and inside of get_active_stage
    monkeypatch_timestamp_seq(mp, 2)
    assert script.run(scenario.stage_info, scenario).status == MarkerStatus.IN_PROGRESS
    expected_msg = _timeout_message(script.start_time, script.timeout, scenario)
    send_event_mock.assert_called_once_with(
        'One host task', juggler_client.JugglerCheckStatus.CRIT, expected_msg, host_name="host"
    )
