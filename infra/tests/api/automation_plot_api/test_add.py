"""Test 'new automation plot' method."""

import pytest
import http.client

from infra.walle.server.tests.lib.util import TestCase
from walle.util.misc import drop_none
from walle.views.helpers.constants import CheckFields


@pytest.fixture
def test(request, monkeypatch_timestamp):
    return TestCase.create(request)


@pytest.mark.usefixtures("unauthenticated")
def test_unauthenticated_user_rejected(test):
    request_data = _dummy_plot(reboot=True)

    result = test.api_client.post("/v1/automation-plot/", data=request_data)

    assert result.status_code == http.client.UNAUTHORIZED
    test.automation_plot.assert_equal()


def test_authenticated_user_allowed(test):
    request_data = _dummy_plot(reboot=True)
    result = test.api_client.post("/v1/automation-plot/", data=request_data)
    assert result.status_code == http.client.CREATED

    test.automation_plot.mock(request_data, save=False)
    test.automation_plot.assert_equal()


def test_create(test):
    from walle.expert.automation_plot import AutomationPlot
    from walle.util.mongo import MongoDocument

    AutomationPlot(id="plot-id", name="plot name").save()

    Plot = MongoDocument.for_model(AutomationPlot)
    plot = Plot(AutomationPlot._get_collection().find_one({"_id": "plot-id"}))
    assert plot.checks == []
    assert AutomationPlot.objects.get(id="plot-id").checks == []


def test_plot_empty_checks_list_allowed(test):
    request_data = _dummy_plot(reboot=None, redeploy=None)
    request_data["checks"] = []

    result = test.api_client.post("/v1/automation-plot/", data=request_data)
    assert result.status_code == http.client.CREATED

    test.automation_plot.mock(request_data, save=False)
    test.automation_plot.assert_equal()


def test_plot_without_checks_field_allowed(test):
    # Save plot with empty check list.
    request_data = _dummy_plot(wait=None, reboot=None, profile=None, redeploy=None)
    del request_data["checks"]

    result = test.api_client.post("/v1/automation-plot/", data=request_data)
    assert result.status_code == http.client.CREATED

    test.automation_plot.mock(dict(request_data, checks=[]), save=False)
    test.automation_plot.assert_equal()


def test_unique_names_required_for_checks(test):
    request_data = _dummy_plot(wait=None, reboot=None, profile=None, redeploy=None)
    request_data["checks"] = [
        _dummy_check("ssh", reboot=True),
        _dummy_check("ssh", enabled=False, redeploy=True),
    ]

    result = test.api_client.post("/v1/automation-plot/", data=request_data)
    assert result.status_code == http.client.BAD_REQUEST

    test.automation_plot.assert_equal()


def test_no_reaction_required_for_check(test):
    request_data = _dummy_plot()

    result = test.api_client.post("/v1/automation-plot/", data=request_data)
    assert result.status_code == http.client.CREATED

    test.automation_plot.mock(request_data, save=False)
    test.automation_plot.assert_equal()


def test_all_possible_reactions_can_be_enabled_for_check(test):
    request_data = _dummy_plot(wait=True, reboot=True, profile=True, redeploy=True)

    result = test.api_client.post("/v1/automation-plot/", data=request_data)
    assert result.status_code == http.client.CREATED

    test.automation_plot.mock(request_data, save=False)
    test.automation_plot.assert_equal()


def test_wait_action_can_be_enabled_for_check(test):
    request_data = _dummy_plot(wait=True)

    result = test.api_client.post("/v1/automation-plot/", data=request_data)
    assert result.status_code == http.client.CREATED

    test.automation_plot.mock(request_data, save=False)
    test.automation_plot.assert_equal()


@pytest.mark.parametrize("enabled", [True, False, None])
def test_check_can_be_disabled_from_start(test, enabled):
    request_data = _dummy_plot(reboot=True, enabled=enabled)

    result = test.api_client.post("/v1/automation-plot/", data=request_data)
    assert result.status_code == http.client.CREATED

    for check in request_data["checks"]:
        check["enabled"] = True if enabled is None else enabled

    test.automation_plot.mock(request_data, save=False)
    test.automation_plot.assert_equal()


def test_does_not_allow_to_update_existing_plots(test):
    request_data = _dummy_plot(reboot=True)
    test.automation_plot.mock(request_data)

    result = test.api_client.post("/v1/automation-plot/", data=request_data)
    assert result.status_code == http.client.CONFLICT

    test.automation_plot.assert_equal()


def _dummy_plot(enabled=True, wait=False, reboot=False, profile=False, redeploy=False, report=True):
    return {
        "id": "plot-id",
        "name": "plot name",
        "owners": ["robot-rtc"],
        "checks": [_dummy_check("ssh", enabled, wait, reboot, profile, redeploy, report)],
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
