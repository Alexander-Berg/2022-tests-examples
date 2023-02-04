"""Test '_validate_checks_list' method."""

import pytest

from walle.errors import BadRequestError
from walle.util.validate_checks import validate_checks_list
from walle.views.helpers.constants import CheckFields


@pytest.mark.parametrize(
    ["reboot", "profile", "redeploy"],
    [
        (True, True, True),
        (True, True, False),
        (True, False, True),
        (False, True, True),
        (False, False, True),
        (True, False, False),
        (False, True, False),
    ],
)
def test_report_is_required_if_any_other_action_enabled(reboot, profile, redeploy):
    checks = _get_dummy_check(reboot, profile, redeploy)
    with pytest.raises(BadRequestError):
        validate_checks_list(checks)


@pytest.mark.parametrize(
    ["reboot", "profile", "redeploy"],
    [
        (True, True, True),
        (True, True, False),
        (True, False, True),
        (False, True, True),
        (False, False, True),
        (True, False, False),
        (False, True, False),
    ],
)
def test_success_validation_if_any_action_and_report_enabled(reboot, profile, redeploy):
    checks = _get_dummy_check(reboot, profile, redeploy, report=True)
    assert validate_checks_list(checks) is None


@pytest.mark.parametrize(["reboot", "profile", "redeploy"], [(False, False, False)])
def test_success_validation_if_all_actions_disabled(reboot, profile, redeploy):
    checks = _get_dummy_check(reboot, profile, redeploy, report=False)
    assert validate_checks_list(checks) is None


def _get_dummy_check(reboot, profile, redeploy, wait=False, report=False):
    return [
        {
            CheckFields.NAME: None,
            CheckFields.REBOOT: reboot,
            CheckFields.PROFILE: profile,
            CheckFields.REDEPLOY: redeploy,
            CheckFields.REPORT_FAILURE: report,
            CheckFields.WAIT: wait,
        }
    ]
