import http.client as httplib

import pytest

from infra.walle.server.tests.lib.util import TestCase, drop_none

_EXISTING_PLOT_ID = "rtc-maintenance-plot"
_PREVIOUS_PLOT_ID = "walle-test-maintenance-plot"
_NON_EXISTING_PLOT_ID = "cloud-maintenance-plot"


@pytest.fixture
def test(request, monkeypatch_production_env):
    test = TestCase.create(request)
    test.mock_projects()
    test.maintenance_plots.mock({"id": _EXISTING_PLOT_ID})
    return test


@pytest.mark.parametrize("method", ["POST", "PATCH"])
@pytest.mark.usefixtures("unauthenticated")
def test_unauthenticated(test, method):
    project = test.mock_project({"id": "some-id"})

    result = test.api_client.open(
        "/v1/projects/{}/maintenance_plot".format(project.id),
        method=method,
        data={"maintenance_plot_id": _EXISTING_PLOT_ID},
    )
    assert result.status_code == httplib.UNAUTHORIZED

    test.projects.assert_equal()


@pytest.mark.parametrize("method", ["POST", "PATCH"])
@pytest.mark.usefixtures("unauthorized_project")
def test_unauthorized(test, method):
    project = test.mock_project({"id": "some-id"})

    result = test.api_client.open(
        "/v1/projects/{}/maintenance_plot".format(project.id),
        method=method,
        data={"maintenance_plot_id": _EXISTING_PLOT_ID},
    )
    assert result.status_code == httplib.FORBIDDEN

    test.projects.assert_equal()


@pytest.mark.parametrize("method", ["POST", "PATCH"])
@pytest.mark.parametrize("reason", [None, "reason mock"])
@pytest.mark.parametrize("maintenance_plot_id", [None, _PREVIOUS_PLOT_ID])
@pytest.mark.usefixtures("monkeypatch_locks", "authorized_admin")
def test_set_maintenance_plot(test, method, reason, maintenance_plot_id):
    project = test.mock_project(
        drop_none({"id": "some-id", "name": "Some name", "maintenance_plot_id": maintenance_plot_id})
    )

    result = test.api_client.open(
        "/v1/projects/{}/maintenance_plot".format(project.id),
        method=method,
        data=drop_none({"maintenance_plot_id": _EXISTING_PLOT_ID, "reason": reason}),
    )
    assert result.status_code == httplib.NO_CONTENT

    project.maintenance_plot_id = _EXISTING_PLOT_ID
    test.projects.assert_equal()


@pytest.mark.parametrize("reason", [None, "reason mock"])
@pytest.mark.parametrize("maintenance_plot_id", [None, _PREVIOUS_PLOT_ID])
@pytest.mark.usefixtures("monkeypatch_locks", "authorized_admin")
def test_unset_maintenance_plot(test, reason, maintenance_plot_id):
    project = test.mock_project({"id": "some-id", "name": "Some name", "maintenance_plot_id": maintenance_plot_id})

    result = test.api_client.open(
        "/v1/projects/{}/maintenance_plot".format(project.id), method="DELETE", data=drop_none({"reason": reason})
    )
    assert result.status_code == httplib.NO_CONTENT

    del project.maintenance_plot_id
    test.projects.assert_equal()


@pytest.mark.usefixtures("monkeypatch_locks", "authorized_admin")
def test_not_existing_project_delete_plot(test):
    result = test.api_client.open("/v1/projects/project-id/maintenance_plot", method="DELETE", data={})
    assert result.status_code == httplib.NOT_FOUND

    test.projects.assert_equal()


@pytest.mark.parametrize("method", ["POST", "PATCH"])
@pytest.mark.usefixtures("monkeypatch_locks", "authorized_admin")
def test_set_plot_for_not_existing_project_(test, method):
    result = test.api_client.open(
        "/v1/projects/project-id/maintenance_plot", method=method, data={"maintenance_plot_id": _EXISTING_PLOT_ID}
    )
    assert result.status_code == httplib.NOT_FOUND

    test.projects.assert_equal()


@pytest.mark.parametrize("method", ["POST", "PATCH"])
@pytest.mark.usefixtures("monkeypatch_locks", "authorized_admin")
def test_set_not_existing_plot(test, method):
    project = test.mock_project({"id": "some-id", "name": "Some name"})

    result = test.api_client.open(
        "/v1/projects/{}/maintenance_plot".format(project.id),
        method=method,
        data={"maintenance_plot_id": _NON_EXISTING_PLOT_ID},
    )
    assert result.status_code == httplib.NOT_FOUND

    test.projects.assert_equal()
