"""Tests for API setup."""
import json
import zlib

import pytest

from infra.walle.server.tests.lib.util import TestCase, monkeypatch_config
from walle.application import app
from walle.util.api import api_response


def mock_api_handler():
    return api_response({"response": "mock api response"})


def mock_api_big_response_handler():
    return api_response({"response": "mock api response", "content": "Lorem ipsum " + ("0123456789" * 410)})


@pytest.fixture
def flask_app(request, mp):
    mp.setattr(app, "flask", None)
    mp.setattr(app, "_Application__services", None)
    mp.setattr(app, "_Application__logging_initialized", None)
    mp.setattr(app, "setup_logging", lambda **kw: None)

    # these are special kind of tests that need to create new flask app for every test.
    # stub blueprint creating to keep thing untouched after this test finishes.
    mp.method(app.setup_api_blueprint)
    mp.method(app.setup_cms_api_blueprint)
    mp.method(app.setup_metrics_blueprint)

    mp.setattr(TestCase, "_app_initialized", False)

    def client():
        app.init_flask()

        with app.init_blueprint("api", "/v1") as mock_api_blueprint:
            mp.setattr(app, "api_blueprint", mock_api_blueprint)
            mock_api_blueprint.add_url_rule("/mock", view_func=mock_api_handler)
            mock_api_blueprint.add_url_rule("/mock-heavy", view_func=mock_api_big_response_handler)

        return TestCase.create(request).api_client

    return client


@pytest.mark.parametrize(
    "allow_origin",
    [
        "walle.localhost.example.com",
        ["walle.localhost.example.com"],
        ["walle.localhost.example.com", "nanny.localhost.example.com"],
    ],
)
def test_cors_allow_matching_origin(flask_app, mp, allow_origin):
    monkeypatch_config(mp, "web.http.allow_origin", allow_origin)

    result = flask_app().get("/v1/mock", headers={"Origin": "http://walle.localhost.example.com"})
    assert result.headers.get("Access-Control-Allow-Origin") == "http://walle.localhost.example.com"
    assert result.headers.get("Access-Control-Allow-Credentials") == "true"


@pytest.mark.parametrize(
    "allow_origin",
    [
        "walle.localhost.example.com:9000",
        "walle.localhost.example.com:",
    ],
)
def test_cors_allow_origin_with_matching_port_number(flask_app, mp, allow_origin):
    monkeypatch_config(mp, "web.http.allow_origin", allow_origin)

    result = flask_app().get("/v1/mock", headers={"Origin": "https://walle.localhost.example.com:9000"})
    assert result.headers.get("Access-Control-Allow-Origin") == "https://walle.localhost.example.com:9000"
    assert result.headers.get("Access-Control-Allow-Credentials") == "true"


@pytest.mark.parametrize(
    "allow_origin",
    [
        None,
        [],
        "",
        "another.domain.example.com",
        "localhost.example.com",  # this is higher level domain which is not allowed
        ".walle.localhost.example.com",  # this only allows subdomains
        ["another.domain.example.com", "more.domains.example.com"],
    ],
)
def test_cors_not_allow_origin_that_does_not_match(flask_app, mp, allow_origin):
    monkeypatch_config(mp, "web.http.allow_origin", allow_origin)

    result = flask_app().get("/v1/mock", headers={"Origin": "http://walle.localhost.example.com"})
    assert "Access-Control-Allow-Origin" not in result.headers
    assert "Access-Control-Allow-Credentials" not in result.headers


@pytest.mark.parametrize(
    "allow_origin",
    [
        ["localhost.yandex-team.ru:9000", "walle.localhost.example.com"],
        ["localhost.yandex-team.ru:", "walle.localhost.example.com"],
        ["walle.localhost.example.com:8080"],
    ],
)
def test_cors_not_allow_origin_with_port_number_that_does_not_match(flask_app, mp, allow_origin):
    monkeypatch_config(mp, "web.http.allow_origin", allow_origin)

    result = flask_app().get("/v1/mock", headers={"Origin": "https://walle.localhost.example.com:9000"})
    assert "Access-Control-Allow-Origin" not in result.headers
    assert "Access-Control-Allow-Credentials" not in result.headers


@pytest.mark.parametrize(
    "allow_origin",
    [
        ".localhost.example.com",
        ".example.com",
        "..example.com",
    ],
)
def test_cors_allow_subdomain_origin_when_subdomains_allowed(flask_app, mp, allow_origin):
    monkeypatch_config(mp, "web.http.allow_origin", allow_origin)

    result = flask_app().get("/v1/mock", headers={"Origin": "http://walle.localhost.example.com"})
    assert result.headers.get("Access-Control-Allow-Origin") == "http://walle.localhost.example.com"
    assert result.headers.get("Access-Control-Allow-Credentials") == "true"


@pytest.mark.parametrize(
    "request_origin",
    [
        "wall-e.yandex-team.ru",
        "wall-e.n.yandex-team.ru",
        "wall-e-test.yandex-team.ru",
        "wall-e-dev.n.yandex-team.ru",
        "nanny.yandex-team.ru",
        "dev-nanny.yandex-team.ru",
        "bot.yandex-team.ru",
        "dev.bot.yandex-team.ru",
        "sandbox.yandex-team.ru",
        "juggler.yandex-team.ru",
        "juggler-prestable.n.yandex-team.ru",
        "juggler-testing.n.yandex-team.ru",
        "juggler-unstable.n.yandex-team.ru",
        "localhost.yandex-team.ru",
        "localhost.yandex-team.ru:9000",
    ],
)
@pytest.mark.parametrize("schema", ["http://", "https://", ""])
def test_default_cors_allows_walle(flask_app, request_origin, schema):
    result = flask_app().get("/v1/mock", headers={"Origin": schema + request_origin})

    assert result.headers.get("Access-Control-Allow-Origin") == schema + request_origin
    assert result.headers.get("Access-Control-Allow-Credentials") == "true"


@pytest.mark.parametrize("encoding_headers", ({}, {"Accept-Encoding": ""}))
def test_no_compression_api_produces_non_compressed_output(flask_app, encoding_headers):
    # NB: compression is only enabled for requests that are longer than 4096
    result = flask_app().get("/v1/mock-heavy", headers=encoding_headers)

    assert result.headers.get("Content-Encoding", None) is None


@pytest.mark.parametrize(
    "encoding_headers",
    (
        {"Accept-Encoding": "deflate,"},
        {"Accept-Encoding": "deflate ,gz-fake"},
        {"Accept-Encoding": "gz-fake, deflate"},
    ),
)
def test_compression_api_produces_compressed_output(flask_app, encoding_headers):
    # NB: only deflate encoding is currently enabled.
    # Feel free to modify this test if you need to enable gzip or add other encoders.

    # NB: compression is only enabled for requests that are longer than 4096
    result = flask_app().get("/v1/mock-heavy", headers=encoding_headers)

    assert result.headers.get("Content-Encoding", None) == "deflate"
    assert ["content", "response"] == sorted(json.loads(zlib.decompress(result.data)).keys())
