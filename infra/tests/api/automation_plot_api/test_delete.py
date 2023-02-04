"""Test 'delete automation plot' method."""

import pytest
import http.client

from infra.walle.server.tests.lib.util import TestCase


@pytest.fixture
def test(request):
    return TestCase.create(request)


@pytest.mark.usefixtures("unauthenticated")
def test_unauthenticated_user_rejected(test):
    test.automation_plot.mock(_dummy_plot())
    result = test.api_client.delete("/v1/automation-plot/plot-id")

    assert result.status_code == http.client.UNAUTHORIZED
    test.automation_plot.assert_equal()


@pytest.mark.usefixtures("monkeypatch_locks")
def test_authenticated_user_allowed(test):
    test.automation_plot.mock(_dummy_plot(), add=False)

    result = test.api_client.delete("/v1/automation-plot/plot-id", data={"reason": "reason-mock"})

    assert result.status_code == http.client.NO_CONTENT
    test.automation_plot.assert_equal()


@pytest.mark.usefixtures("monkeypatch_locks")
def test_plot_is_used_by_a_project(test):
    plot = test.automation_plot.mock(_dummy_plot())
    test.mock_project({"id": "plot-user-project-mock", "name": "Plot User Project Mock", "automation_plot_id": plot.id})

    result = test.api_client.delete("/v1/automation-plot/plot-id", data={"reason": "reason-mock"})
    assert result.status_code == http.client.CONFLICT

    test.automation_plot.assert_equal()
    test.projects.assert_equal()


def _dummy_plot():
    return {
        "id": "plot-id",
        "name": "plot name",
        "owners": [TestCase.api_user],
        "checks": [
            {
                "name": "ssh",
                "reboot": True,
            }
        ],
    }
