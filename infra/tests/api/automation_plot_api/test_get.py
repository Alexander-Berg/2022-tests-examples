"""Tests for automation plot reading API."""
import pytest
import http.client

from infra.walle.server.tests.lib.util import TestCase
from walle.expert.automation_plot import AutomationPlot


@pytest.fixture
def test(request):
    return TestCase.create(request)


@pytest.mark.usefixtures("iterate_authentication")
def test_get_plot_by_id(test):
    plot = test.automation_plot.mock({"id": "plot-id"})

    result = test.api_client.get("/v1/automation-plot/" + plot.id)
    assert result.status_code == http.client.OK
    assert result.json == plot.to_api_obj()


def test_get_non_existing_plot_by_id(test):
    result = test.api_client.get("/v1/automation-plot/plot-id")
    assert result.status_code == http.client.NOT_FOUND


@pytest.mark.parametrize("fields", [["name"], ["id", "name"], AutomationPlot.api_fields])
def test_get_plot_with_fields(test, fields):
    plot = test.automation_plot.mock({"id": "plot-id"})

    result = test.api_client.get("/v1/automation-plot/" + plot.id, query_string={"fields": ",".join(fields)})
    assert result.status_code == http.client.OK
    assert result.json == plot.to_api_obj(fields)

    test.projects.assert_equal()


@pytest.mark.usefixtures("iterate_authentication")
def test_list_plots(test):
    plot_1 = test.automation_plot.mock({"id": "plot-rtc-id", "name": "RTC"})
    plot_2 = test.automation_plot.mock({"id": "plot-qloud-id", "name": "Qloud"})

    result = test.api_client.get("/v1/automation-plot/")
    assert result.status_code == http.client.OK
    assert result.json == {"result": [plot_1.to_api_obj(), plot_2.to_api_obj()]}


def test_list_plots_from_empty_db(test):

    result = test.api_client.get("/v1/automation-plot/")
    assert result.status_code == http.client.OK
    assert result.json == {"result": []}


@pytest.mark.parametrize("fields", [["name"], ["id", "name"], AutomationPlot.api_fields])
def test_list_plots_with_fields(test, fields):
    plot_1 = test.automation_plot.mock({"id": "plot-rtc-id", "name": "RTC"})
    plot_2 = test.automation_plot.mock({"id": "plot-qloud-id", "name": "Qloud"})

    result = test.api_client.get("/v1/automation-plot/", query_string={"fields": ",".join(fields)})
    assert result.status_code == http.client.OK
    assert result.json == {"result": [plot_1.to_api_obj(fields), plot_2.to_api_obj(fields)]}
