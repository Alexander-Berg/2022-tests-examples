"""Tests project notifications modify API."""

import pytest
import http.client

from infra.walle.server.tests.lib.util import TestCase
from walle.projects import Notifications


@pytest.fixture
def test(request, monkeypatch_production_env):
    test = TestCase.create(request)
    test.mock_projects()
    return test


@pytest.mark.parametrize("method", ("PUT", "PATCH", "POST", "DELETE"))
def test_unauthenticated(test, unauthenticated, method):
    project = test.mock_project({"id": "some-id"})

    result = test.api_client.open("/v1/projects/" + project.id + "/notifications/recipients", method=method, data={})
    assert result.status_code == http.client.UNAUTHORIZED

    test.projects.assert_equal()


@pytest.mark.parametrize("method", ("PUT", "PATCH", "POST", "DELETE"))
def test_unauthorized(test, unauthorized_project, method):
    project = test.mock_project({"id": "some-id"})

    result = test.api_client.open(
        "/v1/projects/" + project.id + "/notifications/recipients",
        method=method,
        data={"info": ["info@yandex-team.ru"]},
    )
    assert result.status_code == http.client.FORBIDDEN

    test.projects.assert_equal()


@pytest.mark.parametrize("method", ("PUT", "PATCH", "POST", "DELETE"))
def test_set_invalid(test, method):
    project = test.mock_project({"id": "some-id"})

    result = test.api_client.open("/v1/projects/" + project.id + "/notifications/recipients", method=method, data={})
    assert result.status_code == http.client.BAD_REQUEST

    result = test.api_client.open(
        "/v1/projects/" + project.id + "/notifications/recipients",
        method=method,
        data={"info": ["--INVALID--@yandex-team.ru"]},
    )
    assert result.status_code == http.client.BAD_REQUEST

    test.projects.assert_equal()


def test_set(test):
    project = test.mock_project(
        {
            "id": "some-id",
            "notifications": Notifications(
                recipients={
                    "info": ["old-info@yandex-team.ru"],
                    "error": ["error@yandex-team.ru"],
                }
            ),
        }
    )

    recipients = {
        "info": ["info@yandex-team.ru"],
        "warning": ["warning@yandex-team.ru"],
    }
    result = test.api_client.put("/v1/projects/" + project.id + "/notifications/recipients", data=recipients)
    assert result.status_code == http.client.OK

    project.notifications.recipients.info = recipients["info"]
    project.notifications.recipients.warning = recipients["warning"]

    test.projects.assert_equal()


@pytest.mark.parametrize("method", ("PATCH", "POST"))
def test_add(test, method):
    project = test.mock_project(
        {
            "id": "some-id",
            "notifications": Notifications(
                recipients={
                    "info": ["info1@yandex-team.ru"],
                    "warning": ["warning@yandex-team.ru"],
                    "critical": ["critical1@yandex-team.ru"],
                }
            ),
        }
    )

    recipients = {
        "info": ["info2@yandex-team.ru", "info3@yandex-team.ru"],
        "error": ["error1@yandex-team.ru", "error2@yandex-team.ru"],
        "critical": ["critical1@yandex-team.ru", "critical1@yandex-team.ru", "critical2@yandex-team.ru"],
    }
    result = test.api_client.open(
        "/v1/projects/" + project.id + "/notifications/recipients", method=method, data=recipients
    )
    assert result.status_code == http.client.OK

    project.notifications.recipients.info.extend(recipients["info"])
    project.notifications.recipients.error = recipients["error"]
    project.notifications.recipients.critical.append("critical2@yandex-team.ru")

    test.projects.assert_equal()


def test_remove(test):
    project = test.mock_project(
        {
            "id": "some-id",
            "notifications": Notifications(
                recipients={
                    "info": ["info1@yandex-team.ru", "info2@yandex-team.ru", "info3@yandex-team.ru"],
                    "warning": ["warning@yandex-team.ru"],
                    "error": ["error1@yandex-team.ru", "error2@yandex-team.ru", "error3@yandex-team.ru"],
                }
            ),
        }
    )

    result = test.api_client.delete(
        "/v1/projects/" + project.id + "/notifications/recipients",
        data={
            "info": ["info1@yandex-team.ru", "info3@yandex-team.ru"],
            "error": ["error2@yandex-team.ru"],
        },
    )
    assert result.status_code == http.client.OK

    del project.notifications.recipients.info[2]
    del project.notifications.recipients.info[0]
    del project.notifications.recipients.error[1]

    test.projects.assert_equal()
