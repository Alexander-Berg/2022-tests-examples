import http.client

import pytest

from infra.walle.server.tests.lib.util import TestCase
from walle.scenario.constants import ScriptName


@pytest.fixture
def test(request):
    return TestCase.create(request)


@pytest.mark.usefixtures("monkeypatches_for_noop_scenario")
@pytest.mark.parametrize(
    "fields",
    [
        ("id", "meta_info", "common_settings", "scenarios_settings"),
        ("id", "scenarios_settings"),
        ("meta_info", "common_settings"),
    ],
)
def test_get_maintenance_plot_by_id(test, fields):
    plot = test.maintenance_plots.mock({"id": "plot-id"})

    result = test.api_client.get("/v1/maintenance-plots/{}".format(plot.id), query_string={"fields": ",".join(fields)})
    assert result.status_code == http.client.OK
    assert result.json == plot.to_api_obj(fields)


def test_get_non_existing_maintenance_plot_by_id(test):
    result = test.api_client.get("/v1/maintenance-plots/nonexistent-plot-id")
    assert result.status_code == http.client.NOT_FOUND


@pytest.mark.usefixtures("monkeypatches_for_noop_scenario")
@pytest.mark.parametrize(
    "fields",
    [
        ("id", "meta_info", "common_settings", "scenarios_settings"),
        ("id", "scenarios_settings"),
        ("meta_info", "common_settings"),
    ],
)
def test_list_maintenance_plots(test, fields):
    plot_1 = test.maintenance_plots.mock({"id": "plot-rtc-id"})
    plot_2 = test.maintenance_plots.mock({"id": "plot-qloud-id"})

    result = test.api_client.get("/v1/maintenance-plots", query_string={"fields": ",".join(fields)})
    assert result.status_code == http.client.OK
    assert result.json == {"result": [plot_1.to_api_obj(fields), plot_2.to_api_obj(fields)]}


def project_exists_in_response(response, project_id):
    for proj in response["result"]:
        if proj["id"] == project_id:
            return True
    return False


def test_get_projects_of_maintenance_plot_by_id(test):
    for i in range(2):
        test.mock_maintenance_plot(
            dict(
                id=f"mocked-maintenance-plot-{i}",
                scenarios_settings=[
                    {
                        "scenario_type": ScriptName.ITDC_MAINTENANCE,
                        "settings": {
                            "reject_scenario_creation_if_maintenance_start_time_is_closer_than_x_hours": 10,
                        },
                    }
                ],
            )
        )

    for i in range(3):
        test.mock_project({"id": f"mock-project-{i}", "maintenance_plot_id": f"mocked-maintenance-plot-0"})

    for i in range(3, 6):
        test.mock_project({"id": f"mock-project-{i}", "maintenance_plot_id": f"mocked-maintenance-plot-1"})

    # Run: check if request status is 200 and request output is correct
    first_req = test.api_client.get(
        "/v1/maintenance-plots/{}/projects".format("mocked-maintenance-plot-0"), query_string=dict(limit=1)
    )
    assert first_req.status_code == 200
    assert len(first_req.json["result"]) == 1
    assert all("id" in item and "name" in item for item in first_req.json["result"])
    assert not project_exists_in_response(first_req.json, "mock-project-3")

    # Run: check if cursor works for shifting the output
    first_req_cursor = first_req.json["next_cursor"]
    cursor_req = test.api_client.get(
        "/v1/maintenance-plots/{}/projects".format("mocked-maintenance-plot-0"),
        query_string=dict(cursor=first_req_cursor, limit=1),
    )
    cursor_req_repeated = test.api_client.get(
        "/v1/maintenance-plots/{}/projects".format("mocked-maintenance-plot-0"),
        query_string=dict(cursor=first_req_cursor, limit=1),
    )
    assert first_req.json != cursor_req.json and cursor_req.json == cursor_req_repeated.json
    assert not project_exists_in_response(cursor_req_repeated.json, "mock-project-3")

    cursor = cursor_req_repeated.json["next_cursor"]
    while cursor:
        req = test.api_client.get(
            "/v1/maintenance-plots/{}/projects".format("mocked-maintenance-plot-0"),
            query_string=dict(cursor=cursor, limit=1),
        )
        cursor = req.json["next_cursor"] if "next_cursor" in req.json else None
        assert not project_exists_in_response(first_req.json, "mock-project-3")

    # Run: check if query selects only projects of the requested maintenance plot
    other_plot_req = test.api_client.get(
        "/v1/maintenance-plots/{}/projects".format("mocked-maintenance-plot-1"), query_string=dict(limit=1)
    )
    assert other_plot_req.status_code == 200
    assert project_exists_in_response(other_plot_req.json, "mock-project-3")
    assert len(other_plot_req.json["result"]) == 1
