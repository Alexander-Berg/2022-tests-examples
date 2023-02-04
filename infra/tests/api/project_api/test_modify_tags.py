"""Tests project modification API: set/change/remove tags."""

import pytest
import http.client

from infra.walle.server.tests.lib.util import TestCase, drop_none


@pytest.fixture
def test(request, monkeypatch_production_env):
    test = TestCase.create(request)
    test.mock_projects()
    return test


@pytest.mark.parametrize("method", ["PUT", "POST", "PATCH", "DELETE"])
def test_unauthenticated(test, unauthenticated, method):
    project = test.mock_project({"id": "some-id"})

    result = test.api_client.open("/v1/projects/{}/tags".format(project.id), method=method, data={})
    assert result.status_code == http.client.UNAUTHORIZED

    test.projects.assert_equal()


@pytest.mark.parametrize("method", ["PUT", "POST", "PATCH", "DELETE"])
def test_unauthorized(test, unauthorized_project, method):
    project = test.mock_project({"id": "some-id"})

    result = test.api_client.open("/v1/projects/{}/tags".format(project.id), method=method, data={"tags": []})
    assert result.status_code == http.client.FORBIDDEN

    test.projects.assert_equal()


@pytest.mark.parametrize("old_tags", [None, [], ["test"], ["rtc", "test"]])
@pytest.mark.usefixtures("monkeypatch_locks")
def test_set_tags(test, old_tags):
    project = test.mock_project(drop_none({"id": "some-id", "name": "Some name", "tags": old_tags}))
    result = test.api_client.put("/v1/projects/{}/tags".format(project.id), data={"tags": ["#yt", "hahn"]})

    project.tags = ["hahn", "yt"]
    assert result.status_code == http.client.NO_CONTENT
    test.projects.assert_equal()


@pytest.mark.parametrize("add_method", ["POST", "PATCH"])
@pytest.mark.parametrize("old_tags", [None, [], ["test"], ["rtc", "test"]])
@pytest.mark.usefixtures("monkeypatch_locks")
def test_add_tags(test, old_tags, add_method):
    project = test.mock_project(drop_none({"id": "some-id", "name": "Some name", "tags": old_tags}))
    result = test.api_client.open(
        "/v1/projects/{}/tags".format(project.id), method=add_method, data={"tags": ["#test", "yt"]}
    )

    project.tags = sorted(set(project.tags or []) | {"test", "yt"})
    assert result.status_code == http.client.NO_CONTENT
    test.projects.assert_equal()


@pytest.mark.parametrize("add_method", ["POST", "PATCH"])
@pytest.mark.parametrize("old_tags", [None, [], ["test"], ["rtc", "test"]])
@pytest.mark.parametrize("request_data", [{"tags": []}])
@pytest.mark.usefixtures("monkeypatch_locks")
def test_add_empty_list_of_tags_does_nothing(test, old_tags, add_method, request_data):
    project = test.mock_project(drop_none({"id": "some-id", "name": "Some name", "tags": old_tags}))
    result = test.api_client.open("/v1/projects/{}/tags".format(project.id), method=add_method, data=request_data)

    assert result.status_code == http.client.NO_CONTENT
    test.projects.assert_equal()


@pytest.mark.parametrize("old_tags", [None, [], ["test"], ["rtc", "test"]])
@pytest.mark.usefixtures("monkeypatch_locks")
def test_remove_tags(test, old_tags):
    project = test.mock_project(drop_none({"id": "some-id", "name": "Some name", "tags": old_tags}))
    result = test.api_client.delete("/v1/projects/{}/tags".format(project.id), data={"tags": ["test"]})

    if project.tags:
        project.tags = sorted(set(project.tags or []) - {"test"})

    assert result.status_code == http.client.NO_CONTENT
    test.projects.assert_equal()


@pytest.mark.parametrize("old_tags", [None, [], ["test"], ["rtc", "test"]])
@pytest.mark.parametrize(
    ["request_method", "request_data"],
    (
        ("PUT", {"tags": []}),
        ("DELETE", {"tags": []}),
        ("DELETE", None),
    ),
)
@pytest.mark.usefixtures("monkeypatch_locks")
def test_clear_tags(test, old_tags, request_data, request_method):
    project = test.mock_project(drop_none({"id": "some-id", "name": "Some name", "tags": old_tags}))
    result = test.api_client.open("/v1/projects/{}/tags".format(project.id), method=request_method, data=request_data)

    del project.tags

    assert result.status_code == http.client.NO_CONTENT
    test.projects.assert_equal()
