"""Test iteration strategy and utilities."""
import pytest

from walle.scenario.iteration_strategy import (
    ActionsMap,
    SequentialIterationActionStrategy,
    SequentialActionsStrategyWithRepeat,
)
from walle.scenario.marker import Marker, MarkerStatus
from walle.scenario.scenario import Scenario
from walle.scenario.stage_info import StageInfo, StageAction


def prepare():
    pass


def action():
    pass


def check():
    pass


def completion():
    pass


def failure():
    pass


class TestActionsMap:
    def test_contains_by_name(self):
        am = ActionsMap([action, check])
        assert "action" in am
        assert "check" in am

    def test_get_by_names(self):
        am = ActionsMap([action, check])
        assert am["action"] is action
        assert am["check"] is check

    def test_get_next_key_returns_next(self):
        am = ActionsMap([action, check, completion])
        assert am.get_next_key("action") == "check"
        assert am.get_next_key("check") == "completion"

    def test_returns_none_for_last_actions_next_key(self):
        am = ActionsMap([action, check, completion])
        assert am.get_next_key("completion") is None

    def test_get_first_action_returns_first_action(self):
        am = ActionsMap([action, check])
        assert am.get_first_action_key() == "action"

    def test_iterates_over_actions_in_direct_order(self):
        am = ActionsMap([action, check, completion])
        assert list(am) == ["action", "check", "completion"]


class TestSequentialIterationStrategyBase:
    @pytest.mark.parametrize(
        ["action_name", "action_function"],
        [
            (StageAction.ACTION, action),
            (StageAction.CHECK, check),
            (StageAction.COMPLETION, completion),
        ],
    )
    def test_get_current_function_returns_current_action(self, action_name, action_function):
        sis = SequentialIterationActionStrategy([action, check, completion])
        stage_info = StageInfo(action_type=action_name)

        assert sis.get_current_func(stage_info) is action_function

    @pytest.mark.parametrize(
        ["current_action", "next_action"],
        [
            (StageAction.ACTION, StageAction.CHECK),
            (StageAction.CHECK, StageAction.COMPLETION),
        ],
    )
    def test_get_next_action_returns_next_action(self, current_action, next_action):
        # this looks like an internal method to me
        sis = SequentialIterationActionStrategy([action, check, completion])
        stage_info = StageInfo(action_type=current_action)

        assert sis.get_next_action(stage_info) == next_action

    @pytest.mark.parametrize("current_action", (StageAction.ACTION, StageAction.CHECK, StageAction.COMPLETION))
    @pytest.mark.parametrize("marker_status", (MarkerStatus.IN_PROGRESS, MarkerStatus.FAILURE))
    def test_make_transition_does_nothing_if_action_didnt_return_success(self, current_action, marker_status):
        sis = SequentialIterationActionStrategy([action, check, completion])
        stage_info = StageInfo(action_type=current_action)

        action_marker = Marker(marker_status)
        res_marker = sis.make_transition(action_marker, stage_info, Scenario())

        # same action, no transition
        assert stage_info.action_type == current_action
        assert res_marker == action_marker

    @pytest.mark.parametrize(
        ["current_action", "next_action"],
        [
            (StageAction.ACTION, StageAction.CHECK),
            (StageAction.CHECK, StageAction.COMPLETION),
        ],
    )
    def test_make_transition_transits_to_next_action_on_success(self, current_action, next_action):
        sis = SequentialIterationActionStrategy([action, check, completion])
        stage_info = StageInfo(action_type=current_action)

        res_marker = sis.make_transition(Marker.success(), stage_info, Scenario())

        # next action, transition happened
        assert stage_info.action_type == next_action
        assert res_marker == Marker.in_progress()

    def test_make_transition_finishes_stage(self):
        # stage_info is a MongoEngine embedded document.
        # stage_info.action_type has default value.
        # Which means when you set it to None, it actually resets to the default value.
        # Please, do not try to depend on this implementation detail.
        sis = SequentialIterationActionStrategy([action, check, completion])
        stage_info = StageInfo(action_type=StageAction.COMPLETION)

        res_marker = sis.make_transition(Marker.success(), stage_info, Scenario())

        # Please, do not try to depend on this implementation detail.
        assert stage_info.action_type == StageAction.ACTION

        assert res_marker == Marker.success()

    @pytest.mark.parametrize("current_action", (StageAction.ACTION, StageAction.CHECK, StageAction.COMPLETION))
    def test_restart_strategy_starts_with_fisrt_action(self, current_action):
        sis = SequentialIterationActionStrategy([action, check, completion])
        stage_info = StageInfo(action_type=current_action)

        sis.restart(stage_info)

        assert stage_info.action_type == StageAction.ACTION


class TestSequentialIterationStrategy:
    @pytest.mark.parametrize(
        ["action_name", "action_function"],
        [
            (StageAction.ACTION, action),
            (StageAction.CHECK, check),
            (StageAction.COMPLETION, completion),
        ],
    )
    def test_get_current_function_returns_current_action(self, action_name, action_function):
        sis = SequentialIterationActionStrategy([action, check, completion])
        stage_info = StageInfo(action_type=action_name)

        assert sis.get_current_func(stage_info) is action_function

    @pytest.mark.parametrize(
        ["current_action", "next_action"],
        [
            (StageAction.ACTION, StageAction.CHECK),
            (StageAction.CHECK, StageAction.COMPLETION),
        ],
    )
    def test_get_next_action_returns_next_action(self, current_action, next_action):
        # this looks like an internal method to me
        sis = SequentialIterationActionStrategy([action, check, completion])
        stage_info = StageInfo(action_type=current_action)

        assert sis.get_next_action(stage_info) == next_action

    @pytest.mark.parametrize("current_action", (StageAction.ACTION, StageAction.CHECK, StageAction.COMPLETION))
    @pytest.mark.parametrize("marker_status", (MarkerStatus.IN_PROGRESS, MarkerStatus.FAILURE))
    def test_make_transition_does_nothing_if_action_didnt_return_success(self, current_action, marker_status):
        sis = SequentialIterationActionStrategy([action, check, completion])
        stage_info = StageInfo(action_type=current_action)

        action_marker = Marker(marker_status)
        res_marker = sis.make_transition(action_marker, stage_info, Scenario())

        # same action, no transition
        assert stage_info.action_type == current_action
        assert res_marker == action_marker

    @pytest.mark.parametrize(
        ["current_action", "next_action"],
        [
            (StageAction.ACTION, StageAction.CHECK),
            (StageAction.CHECK, StageAction.COMPLETION),
        ],
    )
    def test_make_transition_transits_to_next_action_on_success(self, current_action, next_action):
        sis = SequentialIterationActionStrategy([action, check, completion])
        stage_info = StageInfo(action_type=current_action)

        res_marker = sis.make_transition(Marker.success(), stage_info, Scenario())

        # next action, transition happened
        assert stage_info.action_type == next_action
        assert res_marker == Marker.in_progress()

    def test_make_transition_finishes_stage(self):
        # stage_info is a MongoEngine embedded document.
        # stage_info.action_type has default value.
        # Which means when you set it to None, it actually resets to the default value.
        # Please, do not try to depend on this implementation detail.
        sis = SequentialIterationActionStrategy([action, check, completion])
        stage_info = StageInfo(action_type=StageAction.COMPLETION)

        res_marker = sis.make_transition(Marker.success(), stage_info, Scenario())

        # Please, do not try to depend on this implementation detail.
        assert stage_info.action_type == StageAction.ACTION

        assert res_marker == Marker.success()

    @pytest.mark.parametrize("current_action", (StageAction.ACTION, StageAction.CHECK, StageAction.COMPLETION))
    def test_restart_strategy_starts_with_fisrt_action(self, current_action):
        sis = SequentialIterationActionStrategy([action, check, completion])
        stage_info = StageInfo(action_type=current_action)

        sis.restart(stage_info)

        assert stage_info.action_type == StageAction.ACTION


class TestSequentialActionStrategyWithRepeat:
    @pytest.mark.parametrize(
        ["action_name", "action_function"],
        [
            (StageAction.ACTION, action),
            (StageAction.CHECK, check),
            (StageAction.COMPLETION, completion),
        ],
    )
    def test_get_current_function_returns_current_action(self, action_name, action_function):
        sais = SequentialActionsStrategyWithRepeat([action, check, completion])
        stage_info = StageInfo(action_type=action_name)

        assert sais.get_current_func(stage_info) is action_function

    @pytest.mark.parametrize(
        ["current_action", "next_action"],
        [
            (StageAction.ACTION, StageAction.CHECK),
            (StageAction.CHECK, StageAction.COMPLETION),
        ],
    )
    def test_get_next_action_returns_next_action(self, current_action, next_action):
        # this looks like an internal method to me
        sais = SequentialActionsStrategyWithRepeat([action, check, completion])
        stage_info = StageInfo(action_type=current_action)

        assert sais.get_next_action(stage_info) == next_action

    @pytest.mark.parametrize("current_action", (StageAction.ACTION, StageAction.CHECK, StageAction.COMPLETION))
    @pytest.mark.parametrize("marker_status", (MarkerStatus.IN_PROGRESS, MarkerStatus.FAILURE))
    def test_make_transition_does_nothing_if_action_didnt_return_success(self, current_action, marker_status):
        sais = SequentialActionsStrategyWithRepeat([action, check, completion])
        stage_info = StageInfo(action_type=current_action)

        action_marker = Marker(marker_status)
        res_marker = sais.make_transition(action_marker, stage_info, Scenario())

        # same action, no transition
        assert stage_info.action_type == current_action
        assert res_marker == action_marker

    @pytest.mark.parametrize(
        ["current_action", "next_action"],
        [
            (StageAction.ACTION, StageAction.CHECK),
            (StageAction.CHECK, StageAction.COMPLETION),
        ],
    )
    def test_make_transition_transits_to_next_action_on_success(self, current_action, next_action):
        sais = SequentialActionsStrategyWithRepeat([action, check, completion])
        stage_info = StageInfo(action_type=current_action)

        res_marker = sais.make_transition(Marker.success(), stage_info, Scenario())

        # next action, transition happened
        assert stage_info.action_type == next_action
        assert res_marker == Marker.in_progress()

    def test_make_transition_finishes_stage(self):
        # stage_info is a MongoEngine embedded document.
        # stage_info.action_type has default value.
        # Which means when you set it to None, it actually resets to the default value.
        # Please, do not try to depend on this implementation detail.
        sais = SequentialActionsStrategyWithRepeat([action, check, completion])
        stage_info = StageInfo(action_type=StageAction.COMPLETION)

        res_marker = sais.make_transition(Marker.success(), stage_info, Scenario())

        # Please, do not try to depend on this implementation detail.
        assert stage_info.action_type == StageAction.ACTION

        assert res_marker == Marker.success()

    @pytest.mark.parametrize("current_action", (StageAction.ACTION, StageAction.CHECK, StageAction.COMPLETION))
    def test_restart_strategy_starts_with_fisrt_action(self, current_action):
        sais = SequentialActionsStrategyWithRepeat([action, check, completion])
        stage_info = StageInfo(action_type=current_action)

        sais.restart(stage_info)

        assert stage_info.action_type == StageAction.ACTION

    def test_prepare_action_uses_provided_prepare_function(self):
        sais = SequentialActionsStrategyWithRepeat([action, check, completion], stage_prepare=prepare)
        stage_info = StageInfo(action_type=StageAction.PREPARE)

        assert sais.get_current_func(stage_info) is prepare

    def test_failure_action_uses_provided_failure_function(self):
        sais = SequentialActionsStrategyWithRepeat([action, check, completion], stage_failure=failure)
        stage_info = StageInfo(action_type=StageAction.FAILURE)

        assert sais.get_current_func(stage_info) is failure

    def test_restarts_actions_when_stage_has_more(self):
        sais = SequentialActionsStrategyWithRepeat(
            actions=[action, check, completion], stage_has_more=lambda *args: True
        )

        stage_info = StageInfo(action_type=StageAction.COMPLETION)
        result = sais.make_transition(Marker.success(), stage_info, Scenario())

        assert stage_info.action_type == StageAction.ACTION
        assert result.status == MarkerStatus.IN_PROGRESS

    def test_finishes_earlier_when_stage_completed_returns_success(self):
        sais = SequentialActionsStrategyWithRepeat(
            actions=[action, check, completion], stage_completed=lambda *args: Marker.success()
        )

        stage_info = StageInfo(action_type=StageAction.CHECK)
        result = sais.make_transition(Marker.in_progress(), stage_info, Scenario())

        assert stage_info.action_type == StageAction.ACTION  # dropped to None but MongoEngined replaces with default
        assert result.status == MarkerStatus.SUCCESS

    def test_finishes_with_failure_when_stage_completed_returns_failure(self):
        sais = SequentialActionsStrategyWithRepeat(
            actions=[action, check, completion], stage_completed=lambda *args: Marker.failure()
        )

        stage_info = StageInfo(action_type=StageAction.CHECK)
        result = sais.make_transition(Marker.in_progress(), stage_info, Scenario())

        assert stage_info.action_type == StageAction.FAILURE
        assert result.status == MarkerStatus.FAILURE

    def test_continues_when_stage_completed_returns_in_progress(self):
        sais = SequentialActionsStrategyWithRepeat(
            actions=[action, check, completion], stage_completed=lambda *args: Marker.in_progress()
        )

        stage_info = StageInfo(action_type=StageAction.CHECK)
        result = sais.make_transition(Marker.in_progress(), stage_info, Scenario())

        assert stage_info.action_type == StageAction.CHECK  # not changed
        assert result.status == MarkerStatus.IN_PROGRESS  # current implementation quirk, should be fixed

    @pytest.mark.parametrize("marker", [Marker.success(), Marker.failure(), Marker.in_progress()])
    def test_returns_same_failure_marker_that_failure_action_produces(self, marker):
        sais = SequentialActionsStrategyWithRepeat(actions=[])

        stage_info = StageInfo(action_type=StageAction.FAILURE)
        result = sais.make_transition(marker, stage_info, Scenario())

        assert result is marker
