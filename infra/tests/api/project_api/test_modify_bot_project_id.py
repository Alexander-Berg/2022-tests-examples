"""Tests project modification API: set/change/remove bot_project_id."""

import pytest
import http.client

from infra.walle.server.tests.lib.util import TestCase, drop_none, BOT_PROJECT_ID, monkeypatch_function
from walle.clients import abc, bot

TEST_DATA = {"abc_service_slug": "some_service", "bot_project_id": BOT_PROJECT_ID}


@pytest.fixture
def test(request, monkeypatch_production_env, mp):
    test = TestCase.create(request)
    test.mock_projects()
    monkeypatch_function(mp, bot.get_bot_project_id_by_planner_id, module=bot, return_value=BOT_PROJECT_ID)
    monkeypatch_function(
        mp, abc.get_service_by_slug, module=abc, return_value={"id": 37, "slug": TEST_DATA["abc_service_slug"]}
    )
    return test


@pytest.mark.parametrize("method", ["POST", "PATCH"])
@pytest.mark.parametrize("data_field", ["bot_project_id", "abc_service_slug"])
def test_unauthenticated(test, unauthenticated, method, data_field):
    project = test.mock_project({"id": "some-id"})

    result = test.api_client.open(
        "/v1/projects/{}/bot_project_id".format(project.id), method=method, data={data_field: TEST_DATA[data_field]}
    )
    assert result.status_code == http.client.UNAUTHORIZED

    test.projects.assert_equal()


@pytest.mark.parametrize("method", ["POST", "PATCH"])
@pytest.mark.parametrize("data_field", ["bot_project_id", "abc_service_slug"])
def test_unauthorized(test, unauthorized_project, method, data_field):
    project = test.mock_project({"id": "some-id"})

    result = test.api_client.open(
        "/v1/projects/{}/bot_project_id".format(project.id), method=method, data={data_field: TEST_DATA[data_field]}
    )
    assert result.status_code == http.client.FORBIDDEN

    test.projects.assert_equal()


@pytest.mark.parametrize("method", ["POST", "PATCH"])
@pytest.mark.parametrize("data_field", ["bot_project_id", "abc_service_slug"])
@pytest.mark.parametrize("reason", [None, "reason mock"])
@pytest.mark.parametrize("bot_project_id", [None, 600009])
@pytest.mark.usefixtures("authorized_admin", "monkeypatch_locks", "monkeypatch_bot_projects")
def test_set_bot_project_id(test, method, data_field, reason, bot_project_id):
    project = test.mock_project(drop_none({"id": "some-id", "name": "Some name", "bot_project_id": bot_project_id}))
    result = test.api_client.open(
        "/v1/projects/{}/bot_project_id".format(project.id),
        method=method,
        data=drop_none({data_field: TEST_DATA[data_field], "reason": reason}),
    )
    assert result.status_code == http.client.NO_CONTENT

    project.bot_project_id = BOT_PROJECT_ID
    test.projects.assert_equal()
