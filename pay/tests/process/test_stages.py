from hamcrest import assert_that, equal_to
from mock import patch, Mock, PropertyMock

from tests.processes_testing_instances import TestMaintenanceAction, TestSwitchingAction, TestProcess
from yb_darkspirit.process.stages import SwitchingStage, Stage


@patch.object(TestSwitchingAction, 'apply', Mock())
@patch.object(TestProcess, '_stages_list', PropertyMock())
def test_switching_stage_applies_action_and_returns_stage_to_switch(session, cr_wrapper_with_test_process):
    init_stage = SwitchingStage(TestSwitchingAction)
    middle_stage = Stage(TestMaintenanceAction, name='middle')
    goal_stage = Stage(TestMaintenanceAction, name='final')
    TestProcess._stages_list = [init_stage, middle_stage, goal_stage]

    assert_that(
        TestProcess._find_next_stage_in_stages_list(init_stage),
        equal_to(middle_stage)
    )
    assert_that(
        init_stage.run(session, cr_wrapper_with_test_process.current_process),
        equal_to(goal_stage.name)
    )
    TestSwitchingAction.apply.assert_called_once_with(session, cr_wrapper_with_test_process.current_process)
