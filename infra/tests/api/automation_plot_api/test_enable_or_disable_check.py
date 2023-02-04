"""Test 'enable check' method for automation plot."""

import pytest
import http.client

from infra.walle.server.tests.lib.util import TestCase
from walle.models import timestamp, monkeypatch_timestamp
from walle.util.misc import drop_none


@pytest.fixture
def test(request, monkeypatch_timestamp):
    return TestCase.create(request)


@pytest.mark.usefixtures("unauthenticated")
@pytest.mark.parametrize(
    "path",
    [
        "/v1/automation-plot/plot-id/some-check/enable",
        "/v1/automation-plot/plot-id/some-check/disable",
    ],
)
def test_unauthenticated_user_rejected(test, path):
    result = test.api_client.post(path, data={})

    assert result.status_code == http.client.UNAUTHORIZED
    test.automation_plot.assert_equal()


@pytest.mark.parametrize(
    ["path", "enabled"],
    [
        ("/v1/automation-plot/plot-id/some-check/enable", True),
        ("/v1/automation-plot/plot-id/some-check/disable", False),
    ],
)
def test_authenticated_user_allowed_to_toggle(test, path, enabled):
    plot = test.automation_plot.mock(_dummy_plot(enabled=not enabled, check_name="some-check"))

    result = test.api_client.post(path, data={})

    for check in plot.checks:
        check.enabled = enabled

    assert result.status_code == http.client.OK
    test.automation_plot.assert_equal()


@pytest.mark.parametrize(
    ["path", "enabled"],
    [
        ("/v1/automation-plot/plot-id/some-check/enable", True),
        ("/v1/automation-plot/plot-id/some-check/disable", False),
    ],
)
def test_toggles_only_one_requested_check(test, path, enabled):
    check_name = "some-check"
    plot_data = _dummy_plot()
    plot_data["checks"] = [
        _dummy_check(name="some-other-check", enabled=not enabled),
        _dummy_check(name=check_name, enabled=not enabled),
        _dummy_check(name="some-third-check", enabled=not enabled),
    ]
    plot = test.automation_plot.mock(plot_data)

    result = test.api_client.post(path, data={})

    for check in plot.checks:
        if check["name"] == check_name:
            check.enabled = enabled

    assert result.status_code == http.client.OK
    test.automation_plot.assert_equal()


@pytest.mark.parametrize(
    ["path", "enabled"],
    [
        ("/v1/automation-plot/plot-id/some-check/enable", True),
        ("/v1/automation-plot/plot-id/some-check/disable", False),
    ],
)
def test_enabling_check_bumps_start_time(test, monkeypatch, path, enabled):
    check_name = "some-check"
    plot_data = _dummy_plot()
    plot_data["checks"] = [
        _dummy_check(name="some-other-check", enabled=not enabled),
        _dummy_check(name=check_name, enabled=not enabled),
        _dummy_check(name="some-third-check", enabled=not enabled),
    ]
    plot = test.automation_plot.mock(plot_data)

    monkeypatch_timestamp(monkeypatch, cur_time=timestamp() + 1)
    result = test.api_client.post(path, data={})

    for check in plot.checks:
        if check["name"] == check_name:
            check.enabled = enabled
            if enabled:
                check.start_time = timestamp()

    assert result.status_code == http.client.OK
    test.automation_plot.assert_equal()


def _dummy_check(enabled=None, reboot=None, redeploy=None, name=None):
    return drop_none(
        {
            "name": name or "ssh",
            "enabled": enabled,
            "reboot": reboot,
            "redeploy": redeploy,
        }
    )


def _dummy_plot(enabled=None, check_name=None):
    return {
        "id": "plot-id",
        "name": "plot name",
        "owners": [TestCase.api_user],
        "checks": [_dummy_check(name=check_name, enabled=enabled)],
    }
