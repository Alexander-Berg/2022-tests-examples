"""Tests project modification API: set/change/remove report settings."""

import pytest
import http.client

from infra.walle.server.tests.lib.util import TestCase

DEFAULT_QUEUE = "BURNE"


@pytest.fixture
def test(request, monkeypatch_production_env):
    test = TestCase.create(request)
    test.mock_projects()
    return test


@pytest.mark.parametrize("method", ["POST", "PATCH"])
def test_unauthenticated(test, unauthenticated, method):
    project = test.mock_project({"id": "some-id"})

    result = test.api_client.open("/v1/projects/{}/reports".format(project.id), method=method, data={})
    assert result.status_code == http.client.UNAUTHORIZED

    test.projects.assert_equal()


@pytest.mark.parametrize("method", ["POST", "PATCH"])
def test_unauthorized(test, unauthorized_project, method):
    project = test.mock_project({"id": "some-id"})

    result = test.api_client.open(
        "/v1/projects/{}/reports".format(project.id), method=method, data={"queue": DEFAULT_QUEUE}
    )
    assert result.status_code == http.client.FORBIDDEN

    test.projects.assert_equal()


@pytest.mark.parametrize(
    "updated_fields",
    [
        {"enabled": True},
        {"enabled": False},
        {"summary": "RTC"},
    ],
)
@pytest.mark.usefixtures("monkeypatch_locks")
def test_set_requires_queue(test, updated_fields):
    reports_config = {"enabled": True, "queue": "WALLE", "summary": "YT", "extra": {"components": 35445}}
    project = test.mock_project({"id": "some-id", "reports": reports_config})
    result = test.api_client.put("/v1/projects/{}/reports".format(project.id), data=updated_fields)

    assert result.status_code == http.client.BAD_REQUEST
    assert result.json["message"].endswith("'queue' is a required property")

    test.projects.assert_equal()


@pytest.mark.parametrize("method", ["POST", "PATCH"])
@pytest.mark.usefixtures("monkeypatch_locks")
def test_new_setup_requires_queue_parameter(test, method):
    project = test.mock_project({"id": "some-id"})

    result = test.api_client.open("/v1/projects/{}/reports".format(project.id), method=method, data={})

    assert result.status_code == http.client.BAD_REQUEST
    assert result.json["message"].endswith("'queue' is a required property")

    test.projects.assert_equal()


@pytest.mark.parametrize("method", ["POST", "PATCH"])
@pytest.mark.usefixtures("monkeypatch_locks")
def test_enabled_for_set_by_default(test, method):
    project = test.mock_project({"id": "some-id", "reports": {"enabled": True, "queue": "WALLE"}})

    result = test.api_client.put(
        "/v1/projects/{}/reports".format(project.id), method=method, data={"queue": DEFAULT_QUEUE}
    )

    assert result.status_code == http.client.NO_CONTENT

    project.reports = {"queue": DEFAULT_QUEUE, "enabled": True}
    test.projects.assert_equal()


@pytest.mark.parametrize("method", ["POST", "PATCH"])
@pytest.mark.usefixtures("monkeypatch_locks")
def test_enabled_for_new_setup_by_default(test, method):
    project = test.mock_project({"id": "some-id"})

    result = test.api_client.open(
        "/v1/projects/{}/reports".format(project.id), method=method, data={"queue": DEFAULT_QUEUE}
    )

    assert result.status_code == http.client.NO_CONTENT

    project.reports = {"queue": DEFAULT_QUEUE, "enabled": True}
    test.projects.assert_equal()


@pytest.mark.parametrize("method", ["POST", "PATCH"])
@pytest.mark.usefixtures("monkeypatch_locks")
def test_save_passed_fields_for_new_setup(test, method):
    # "queue" is required for new setup, it is not a subject for test here
    # "enabled" is True by default, use False here for test purpose.
    project = test.mock_project({"id": "some-id"})

    result = test.api_client.open(
        "/v1/projects/{}/reports".format(project.id),
        method=method,
        data={"enabled": False, "queue": DEFAULT_QUEUE, "summary": "RTC", "extra": {"components": 35444}},
    )

    assert result.status_code == http.client.NO_CONTENT

    project.reports = {"queue": DEFAULT_QUEUE, "enabled": False, "summary": "RTC", "extra": {"components": 35444}}
    test.projects.assert_equal()


@pytest.mark.parametrize(
    "new_reports_config",
    [
        {"enabled": False, "queue": DEFAULT_QUEUE},
        {"queue": DEFAULT_QUEUE},
        {"summary": "RTC", "queue": DEFAULT_QUEUE},
    ],
)
@pytest.mark.usefixtures("monkeypatch_locks")
def test_removes_value_for_absent_fields_for_existing_setup_on_set(test, new_reports_config):
    # "queue" is required for new setup, it is not a subject for test here
    # "enabled" is True by default, use False here for test purpose.
    reports_config = {"enabled": True, "queue": "WALLE", "summary": "YT", "extra": {"components": 35445}}
    project = test.mock_project({"id": "some-id", "reports": reports_config})

    result = test.api_client.put("/v1/projects/{}/reports".format(project.id), data=new_reports_config)

    assert result.status_code == http.client.NO_CONTENT

    new_reports_config.setdefault("enabled", True)
    project.reports = new_reports_config
    test.projects.assert_equal()


@pytest.mark.parametrize(
    "updated_fields",
    [
        {"enabled": True},
        {"enabled": False},
        {"enabled": False, "queue": DEFAULT_QUEUE},
        {"queue": DEFAULT_QUEUE},
        {"summary": "RTC"},
        {"extra": {"components": 35444}},
    ],
)
@pytest.mark.usefixtures("monkeypatch_locks")
@pytest.mark.parametrize("update_method", ["POST", "PATCH"])
def test_updates_passed_fields_for_existing_setup_un_update(test, update_method, updated_fields):
    # "queue" is required for new setup, it is not a subject for test here
    # "enabled" is True by default, use False here for test purpose.
    reports_config = {"enabled": True, "queue": "WALLE", "summary": "YT", "extra": {"components": 35445}}
    project = test.mock_project({"id": "some-id", "reports": reports_config})

    result = test.api_client.open(
        "/v1/projects/{}/reports".format(project.id), method=update_method, data=updated_fields
    )

    assert result.status_code == http.client.NO_CONTENT

    reports_config.update(updated_fields)
    project.reports = reports_config
    test.projects.assert_equal()


@pytest.mark.parametrize(
    "updated_fields",
    [
        {"summary": ""},
        {"extra": {}},
    ],
)
@pytest.mark.usefixtures("monkeypatch_locks")
@pytest.mark.parametrize("update_method", ["POST", "PATCH"])
def test_updateremoves_optional_emty_fields(test, update_method, updated_fields):
    # "queue" is required for new setup, it is not a subject for test here
    # "enabled" is True by default, use False here for test purpose.
    reports_config = {"enabled": True, "queue": "WALLE", "summary": "YT", "extra": {"components": 35445}}
    project = test.mock_project({"id": "some-id", "reports": reports_config})

    result = test.api_client.open(
        "/v1/projects/{}/reports".format(project.id), method=update_method, data=updated_fields
    )

    assert result.status_code == http.client.NO_CONTENT

    for key in updated_fields:
        del reports_config[key]

    project.reports = reports_config
    test.projects.assert_equal()


@pytest.mark.parametrize("method", ["POST", "PATCH"])
@pytest.mark.usefixtures("monkeypatch_locks")
def test_passing_unknown_fields_is_error(test, method):
    # "queue" is required for new setup, it is not a subject for test here
    # "enabled" is True by default, use False here for test purpose.
    project = test.mock_project({"id": "some-id"})

    result = test.api_client.open(
        "/v1/projects/{}/reports".format(project.id), method=method, data={"queue": DEFAULT_QUEUE, "wiki": "RTC"}
    )

    assert result.status_code == http.client.BAD_REQUEST
    test.projects.assert_equal()


@pytest.mark.usefixtures("monkeypatch_locks")
def test_remove_report_params(test):
    project = test.mock_project({"id": "some-id", "reports": {"queue": DEFAULT_QUEUE, "enabled": False}})

    result = test.api_client.delete("/v1/projects/{}/reports".format(project.id))
    assert result.status_code == http.client.NO_CONTENT

    del project.reports
    test.projects.assert_equal()
