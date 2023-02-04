import pytest

import walle.util.tasks
from walle import restrictions
from walle.tasks import TaskType


@pytest.mark.parametrize("restrictions", [restrictions.AUTOMATED_PROFILE, restrictions.PROFILE])
@pytest.mark.parametrize("task_type", [TaskType.MANUAL, TaskType.AUTOMATED_HEALING])
def test_check_post_code_allowed_with_restrictions_without_healing(walle_test, restrictions, task_type):
    host = walle_test.mock_host({"restrictions": [restrictions]})
    decision, reason = walle.util.tasks.check_post_code_allowed(host, task_type, None)
    assert decision is False
    if task_type == TaskType.MANUAL:
        assert reason.startswith("Automated healing disabled; Operation restricted")
    else:
        assert reason.startswith("Operation restricted")


@pytest.mark.parametrize("restrictions", [restrictions.AUTOMATED_PROFILE, restrictions.PROFILE])
@pytest.mark.parametrize("task_type", [TaskType.MANUAL, TaskType.AUTOMATED_HEALING])
def test_check_post_code_allowed_with_restrictions_with_healing(walle_test, restrictions, task_type):
    host = walle_test.mock_host({"restrictions": [restrictions]})
    decision, reason = walle.util.tasks.check_post_code_allowed(host, task_type, True)
    assert decision is False
    assert reason.startswith("Operation restricted")


@pytest.mark.parametrize("restrictions", [restrictions.AUTOMATED_PROFILE, restrictions.PROFILE])
@pytest.mark.parametrize("task_type", [TaskType.MANUAL])
def test_check_post_code_allowed_with_restrictions_with_healing_disabled(walle_test, restrictions, task_type):
    host = walle_test.mock_host({"restrictions": [restrictions]})
    decision, reason = walle.util.tasks.check_post_code_allowed(host, task_type, False)
    assert decision is False
    assert reason.startswith("Automated healing disabled; Operation restricted")


@pytest.mark.parametrize("task_type", [TaskType.MANUAL, TaskType.AUTOMATED_HEALING])
def test_check_post_code_allowed_without_restrictions_without_healing(walle_test, task_type):
    host = walle_test.mock_host()
    decision, reason = walle.util.tasks.check_post_code_allowed(host, task_type, None)
    if task_type == TaskType.MANUAL:
        assert decision is False
        assert reason.startswith("Automated healing disabled")
    else:
        assert decision is True
        assert reason is None


@pytest.mark.parametrize("task_type", [TaskType.MANUAL, TaskType.AUTOMATED_HEALING])
def test_check_post_code_allowed_without_restrictions_with_healing(walle_test, task_type):
    host = walle_test.mock_host()
    decision, reason = walle.util.tasks.check_post_code_allowed(host, task_type, True)
    assert decision is True
    assert reason is None


@pytest.mark.parametrize("task_type", [TaskType.MANUAL])
def test_check_post_code_allowed_without_restrictions_with_healing_disabled(walle_test, task_type):
    host = walle_test.mock_host()
    decision, reason = walle.util.tasks.check_post_code_allowed(host, task_type, False)
    assert decision is False
    assert reason.startswith("Automated healing disabled")
