import pytest
import http.client

from infra.walle.server.tests.lib.util import TestCase, drop_none
from walle.projects import RepairRequestSeverity


@pytest.fixture
def test(request, monkeypatch_production_env):
    test = TestCase.create(request)
    test.mock_projects()
    return test


@pytest.mark.parametrize("method", ["POST", "PATCH"])
def test_unauthenticated(test, unauthenticated, method):
    project = test.mock_project({"id": "some-id"})

    result = test.api_client.open(
        "/v1/projects/{}/repair_request_severity".format(project.id),
        method=method,
        data={"repair_request_severity": RepairRequestSeverity.LOW},
    )
    assert result.status_code == http.client.UNAUTHORIZED

    test.projects.assert_equal()


@pytest.mark.parametrize("method", ["POST", "PATCH"])
def test_unauthorized_admin(test, method):
    project = test.mock_project({"id": "some-id"})

    result = test.api_client.open(
        "/v1/projects/{}/repair_request_severity".format(project.id),
        method=method,
        data={"repair_request_severity": RepairRequestSeverity.LOW},
    )
    assert result.status_code == http.client.FORBIDDEN

    test.projects.assert_equal()


@pytest.mark.parametrize("method", ["POST", "PATCH"])
@pytest.mark.parametrize("reason", ["reason mock 1", "reason mock 2"])
@pytest.mark.parametrize("repair_request_severity_default", [None] + RepairRequestSeverity.ALL)
@pytest.mark.parametrize("repair_request_severity_for_set", RepairRequestSeverity.ALL)
@pytest.mark.usefixtures("monkeypatch_locks", "authorized_admin")
def test_set_repair_request_severity(
    test, method, reason, repair_request_severity_default, repair_request_severity_for_set
):
    project = test.mock_project(
        drop_none({"id": "some-id", "name": "Some name", "repair_request_severity": repair_request_severity_default})
    )
    result = test.api_client.open(
        "/v1/projects/{}/repair_request_severity".format(project.id),
        method=method,
        data={"repair_request_severity": repair_request_severity_for_set, "reason": reason},
    )
    assert result.status_code == http.client.NO_CONTENT

    project.repair_request_severity = repair_request_severity_for_set
    test.projects.assert_equal()


@pytest.mark.parametrize("method", ["POST", "PATCH"])
@pytest.mark.usefixtures("monkeypatch_locks", "authorized_admin")
def test_set_repair_request_severity_for_unexisting_project(test, method):
    result = test.api_client.open(
        "/v1/projects/{}/repair_request_severity".format("test"),
        method=method,
        data={"repair_request_severity": RepairRequestSeverity.HIGH},
    )
    assert result.status_code == http.client.NOT_FOUND

    test.projects.assert_equal()


@pytest.mark.parametrize("method", ["POST", "PATCH"])
@pytest.mark.parametrize("severity", [None, "test", 123])
@pytest.mark.usefixtures("monkeypatch_locks", "authorized_admin")
def test_set_wrong_repair_request_severity(test, method, severity):
    project = test.mock_project(drop_none({"id": "some-id"}))
    result = test.api_client.open(
        "/v1/projects/{}/repair_request_severity".format(project.id),
        method=method,
        data={"repair_request_severity": severity},
    )
    assert result.status_code == http.client.BAD_REQUEST

    test.projects.assert_equal()
