"""Tests token API."""

import http.client

import pytest

from infra.walle.server.tests.lib.util import TestCase, patch, monkeypatch_config
from walle.authorization import csrf, blackbox
from walle.clients import oauth


@pytest.fixture
def test(request):
    return TestCase.create(request)


def test_csrf_token_unauthenticated(test, monkeypatch, unauthenticated):
    monkeypatch_config(monkeypatch, "oauth.client_id", "")

    result = test.api_client.get("/v1/csrf-token")
    assert result.status_code == http.client.UNAUTHORIZED


@patch(
    "walle.authorization.blackbox.authenticate",
    return_value=blackbox.AuthInfo(issuer=TestCase.api_issuer, session_id="session-id-mock"),
)
def test_csrf_token(authenticate, test, monkeypatch):
    monkeypatch_config(monkeypatch, "authorization.csrf_key", "csrf-key-mock")
    monkeypatch_config(monkeypatch, "oauth.client_id", "")

    result = test.api_client.get("/v1/csrf-token")
    assert result.status_code == http.client.OK
    assert result.json == {"csrf_token": csrf.get_csrf_token("session-id-mock")}


@patch("walle.clients.oauth.get_authorization_url", return_value="url-mock")
def test_get_authorization_url(get_authorization_url, test, iterate_authentication):
    result = test.api_client.get("/v1/access-token")
    assert result.status_code == http.client.OK
    get_authorization_url.assert_called_once_with()
    assert result.json == {"authorization_url": "url-mock"}


@patch("walle.clients.oauth.get_token", return_value="token-mock")
def test_obtain_access_token(get_token, test, iterate_authentication):
    authorization_code = "code-mock"
    result = test.api_client.post("/v1/access-token", data={"authorization_code": authorization_code})
    assert result.status_code == http.client.OK
    get_token.assert_called_once_with(authorization_code)
    assert result.json == {"access_token": "token-mock"}


@patch("walle.clients.oauth.get_token", side_effect=oauth.OauthError("Mocked error"))
def test_obtain_access_token_with_invalid_code(get_token_mock, test, iterate_authentication):
    authorization_code = "code-mock"
    result = test.api_client.post("/v1/access-token", data={"authorization_code": authorization_code})
    assert result.status_code == http.client.BAD_REQUEST
    get_token_mock.assert_called_once_with(authorization_code)
