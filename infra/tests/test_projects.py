"""Tests projects methods."""

from walle.expert.types import Failure, CheckType, get_walle_check_type
from walle.projects import get_default_automation_limits, DEFAULT_AUTOMATION_LIMIT, DEFAULT_RACK_FAILURE_LIMIT, LIMIT


def get_all_failures_with_default_limit():
    failures = [failure for failure in Failure.ALL if failure != CheckType.WALLE_MAPPING[CheckType.WALLE_RACK]]
    return failures


def test_get_default_automation_limits_with_default_automation_limit():
    failures = get_all_failures_with_default_limit()
    for failure in failures:
        result = get_default_automation_limits(failure)[0]
        assert result[LIMIT] == DEFAULT_AUTOMATION_LIMIT


def test_get_default_automation_limits_for_rack_failure():
    failure = get_walle_check_type(CheckType.WALLE_RACK)
    assert get_default_automation_limits(failure)[0][LIMIT] == DEFAULT_RACK_FAILURE_LIMIT
