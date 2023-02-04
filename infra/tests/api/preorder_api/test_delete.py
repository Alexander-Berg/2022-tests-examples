"""Tests preorder deleting API."""

import pytest
import http.client

from infra.walle.server.tests.lib.util import BOT_PROJECT_ID, TestCase


@pytest.fixture
def test(request):
    test = TestCase.create(request)
    test.preorders.mock(
        dict(id=2, owner=test.api_user, project=test.default_project.id, prepare=False, processed=False)
    )
    return test


def test_unauthenticated(test, unauthenticated):
    result = test.api_client.delete("/v1/preorders/1")
    assert result.status_code == http.client.UNAUTHORIZED
    test.preorders.assert_equal()


def test_unauthorized(test):
    test.preorders.mock(
        dict(
            id=1,
            owner="some-other-user",
            project=test.default_project.id,
            prepare=False,
            processed=False,
            bot_project=BOT_PROJECT_ID,
        )
    )

    result = test.api_client.delete("/v1/preorders/1")
    assert result.status_code == http.client.FORBIDDEN
    assert result.json["message"] == "Authorization failure: You must be owner of #1 preorder to perform this request."

    test.preorders.assert_equal()


def test_delete(test):
    test.preorders.mock(
        dict(
            id=1,
            owner=test.api_user,
            project=test.default_project.id,
            prepare=False,
            processed=False,
            bot_project=BOT_PROJECT_ID,
        ),
        add=False,
    )

    result = test.api_client.delete("/v1/preorders/1")
    assert result.status_code == http.client.NO_CONTENT

    test.preorders.assert_equal()


def test_delete_missing(test):
    result = test.api_client.delete("/v1/preorders/1")
    assert result.status_code == http.client.NOT_FOUND
    assert result.json["message"] == "The specified preorder ID doesn't exist."
    test.preorders.assert_equal()
