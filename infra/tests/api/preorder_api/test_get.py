"""Tests preorder querying API."""

import pytest
import http.client

from infra.walle.server.tests.lib.util import BOT_PROJECT_ID, TestCase


@pytest.fixture
def test(request, unauthenticated):
    test = TestCase.create(request)

    for i in range(1, 4):
        test.preorders.mock(
            dict(
                id=i,
                owner=test.api_user,
                project=test.default_project.id,
                prepare=False,
                processed=False,
                bot_project=BOT_PROJECT_ID + i,
            )
        )

    return test


def test_list(test):
    result = test.api_client.get("/v1/preorders")
    assert result.status_code == http.client.OK
    assert result.json == {"result": [preorder.to_api_obj() for preorder in test.preorders.objects]}
    test.preorders.assert_equal()


def test_get(test):
    result = test.api_client.get("/v1/preorders/2")
    assert result.status_code == http.client.OK
    assert result.json == test.preorders.objects[1].to_api_obj()
    test.preorders.assert_equal()


def test_get_missing(test):
    result = test.api_client.get("/v1/preorders/0")
    assert result.status_code == http.client.NOT_FOUND
    assert result.json["message"] == "The specified preorder ID doesn't exist."
    test.preorders.assert_equal()
