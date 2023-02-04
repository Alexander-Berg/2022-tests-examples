import pytest
import http.client

from infra.walle.server.tests.lib.util import TestCase, drop_none

DEFAULT_TIER = 2
VALID_NOT_DEFAULT_TIER = 1


@pytest.fixture
def test(request, monkeypatch_production_env):
    test = TestCase.create(request)
    test.mock_projects()
    return test


@pytest.mark.parametrize("method", ["POST", "PATCH"])
def test_unauthenticated(test, unauthenticated, method):
    project = test.mock_project({"id": "some-id", "tier": DEFAULT_TIER})

    result = test.api_client.open(
        "/v1/projects/{}/tier".format(project.id), method=method, data={"tier": VALID_NOT_DEFAULT_TIER}
    )
    assert result.status_code == http.client.UNAUTHORIZED

    test.projects.assert_equal()


@pytest.mark.parametrize("method", ["POST", "PATCH"])
def test_unauthorized_admin(test, method):
    project = test.mock_project({"id": "some-id", "tier": DEFAULT_TIER})

    result = test.api_client.open(
        "/v1/projects/{}/tier".format(project.id), method=method, data={"tier": VALID_NOT_DEFAULT_TIER}
    )
    assert result.status_code == http.client.FORBIDDEN

    test.projects.assert_equal()


@pytest.mark.parametrize("method", ["POST", "PATCH"])
@pytest.mark.parametrize("reason", ["reason mock 1", "reason mock 2"])
@pytest.mark.parametrize("existing_tier", [None] + [1, DEFAULT_TIER])
@pytest.mark.usefixtures("monkeypatch_locks", "authorized_admin")
def test_set_tier_successfully(test, method, reason, existing_tier):
    project = test.mock_project(drop_none({"id": "some-id", "name": "Some name", "tier": existing_tier}))
    result = test.api_client.open(
        "/v1/projects/{}/tier".format(project.id),
        method=method,
        data={"tier": VALID_NOT_DEFAULT_TIER, "reason": reason},
    )
    assert result.status_code == http.client.NO_CONTENT

    project.tier = VALID_NOT_DEFAULT_TIER
    test.projects.assert_equal()


@pytest.mark.parametrize("method", ["POST", "PATCH"])
@pytest.mark.usefixtures("monkeypatch_locks", "authorized_admin")
def test_set_tier_for_unexisting_project(test, method):
    result = test.api_client.open(
        "/v1/projects/{}/tier".format("test"), method=method, data={"tier": VALID_NOT_DEFAULT_TIER}
    )
    assert result.status_code == http.client.NOT_FOUND

    test.projects.assert_equal()


@pytest.mark.parametrize("method", ["POST", "PATCH"])
@pytest.mark.parametrize("tier", [None, "test", -1, 5])
@pytest.mark.usefixtures("monkeypatch_locks", "authorized_admin")
def test_set_wrong_tier(test, method, tier):
    project = test.mock_project(drop_none({"id": "some-id"}))
    result = test.api_client.open("/v1/projects/{}/tier".format(project.id), method=method, data={"tier": tier})
    assert result.status_code == http.client.BAD_REQUEST

    test.projects.assert_equal()
