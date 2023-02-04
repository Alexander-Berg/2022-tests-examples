"""Test 'update automation plot' methods."""

import pytest
import http.client

from infra.walle.server.tests.lib.util import TestCase
from walle.util.misc import drop_none
from walle.views.helpers.constants import CheckFields


@pytest.fixture
def test(request, monkeypatch_timestamp):
    return TestCase.create(request)


@pytest.fixture(params=("POST", "PATCH"))
def method(request):
    return request.param


@pytest.mark.usefixtures("unauthenticated")
def test_unauthenticated_user_rejected(test, method):
    request_data = {"name": "some name", "owners": ["robot-rtc"]}
    result = test.api_client.open("/v1/automation-plot/plot-id", method=method, data=request_data)

    assert result.status_code == http.client.UNAUTHORIZED
    test.automation_plot.assert_equal()


def test_authenticated_user_allowed(test, method):
    test.automation_plot.mock(dict(_dummy_plot(reboot=True), id="plot-id"))

    request_data = _dummy_plot(reboot=True)

    result = test.api_client.open("/v1/automation-plot/plot-id", method=method, data=request_data)
    assert result.status_code == http.client.OK

    test.automation_plot.assert_equal()


@pytest.mark.parametrize(
    "update",
    [
        # set new plot name
        {"name": "new plot name"},
        # set new plot owners
        {"owners": ["new-owner"]},
        # set new plot name and owners
        {"owners": ["new-owner"], "name": "new plot name"},
        # set new plot checks
        {
            "checks": [
                {
                    CheckFields.NAME: "walled_disk",
                    CheckFields.REBOOT: True,
                    CheckFields.PROFILE: False,
                    CheckFields.REDEPLOY: False,
                    CheckFields.REPORT_FAILURE: True,
                }
            ]
        },
        # set new plot checks with wait
        {
            "checks": [
                {
                    CheckFields.NAME: "walled_disk",
                    CheckFields.WAIT: True,
                    CheckFields.REBOOT: False,
                    CheckFields.PROFILE: False,
                    CheckFields.REDEPLOY: False,
                    CheckFields.REPORT_FAILURE: False,
                }
            ]
        },
        # unset plot checks
        {"checks": []},
    ],
)
def test_update_fields(test, method, update):
    # this is db version, we don't add it to objects list
    test.automation_plot.mock(dict(_dummy_plot(reboot=True), id="plot-id"), add=False)

    result = test.api_client.open("/v1/automation-plot/plot-id", method=method, data=update)
    assert result.status_code == http.client.OK

    # this is our expected version of updated plot, we don't save it to database
    test.automation_plot.mock(dict(_dummy_plot(reboot=True), id="plot-id", **update), save=False)
    test.automation_plot.assert_equal()


def test_unique_check_names_required(test, method):
    test.automation_plot.mock(dict(_dummy_plot(reboot=True, check_name="ssh"), id="plot-id"))

    update = {
        "checks": [
            _dummy_check("ssh", reboot=True),
            _dummy_check("ssh", enabled=False, redeploy=True),
        ]
    }

    result = test.api_client.open("/v1/automation-plot/plot-id", method=method, data=update)

    assert result.status_code == http.client.BAD_REQUEST
    test.automation_plot.assert_equal()


def test_no_reaction_required_for_check(test, method):
    test.automation_plot.mock(dict(_dummy_plot(reboot=True, check_name="ssh"), id="plot-id"), add=False)

    update = {"checks": [_dummy_check("ssh")]}
    result = test.api_client.open("/v1/automation-plot/plot-id", method=method, data=update)

    assert result.status_code == http.client.OK

    # this is our expected version of updated plot, we don't save it to database
    test.automation_plot.mock(dict(_dummy_plot(), id="plot-id", **update), save=False)
    test.automation_plot.assert_equal()


def _dummy_plot(
    enabled=True,
    wait=False,
    reboot=False,
    profile=False,
    redeploy=False,
    report=True,
    name=None,
    check_name="ssh",
    owners=None,
):
    return {
        "name": name or "plot name",
        "owners": owners or [TestCase.api_user],
        "checks": [_dummy_check(check_name, enabled, wait, reboot, profile, redeploy, report)],
    }


def _dummy_check(name, enabled=True, wait=False, reboot=False, profile=False, redeploy=False, report=True):
    return drop_none(
        {
            CheckFields.NAME: name,
            CheckFields.ENABLED: enabled,
            CheckFields.WAIT: wait,
            CheckFields.REBOOT: reboot,
            CheckFields.PROFILE: profile,
            CheckFields.REDEPLOY: redeploy,
            CheckFields.REPORT_FAILURE: report,
        }
    )
