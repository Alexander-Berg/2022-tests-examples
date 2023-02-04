"""Tests authorization."""

import http.client

import pytest

from infra.walle.server.tests.lib.util import MonkeyPatch, patch, monkeypatch_config, monkeypatch_method, TestCase
from sepelib.core import config
from walle.application import app
from walle.authorization import csrf, blackbox
from walle.clients import staff
from walle.errors import UnauthorizedError
from walle.hosts import HostUnderMaintenanceError, HostState
from walle.util.api import api_handler, admin_request, api_response


@pytest.fixture(autouse=True, scope="module")
def test(request):
    monkeypatch = MonkeyPatch()
    request.addfinalizer(monkeypatch.undo)

    # Required by api_handler decorator
    monkeypatch.setattr(app, "flask", None)
    monkeypatch.setattr(app, "_Application__services", None)
    monkeypatch.setattr(app, "_Application__logging_initialized", None)
    monkeypatch.setattr(TestCase, "_app_initialized", False)
    app.init_flask()

    with app.init_blueprint("api", "/v1") as mock_api_blueprint:
        app.api_blueprint = mock_api_blueprint
        create_handlers()

    # these are special kind of tests that test on handlers instead of those registered in api.
    # stub blueprint creating to keep thing untouched after this test finishes.
    monkeypatch_method(monkeypatch, app.setup_api_blueprint)
    monkeypatch_method(monkeypatch, app.setup_cms_api_blueprint)
    monkeypatch_method(monkeypatch, app.setup_metrics_blueprint)


def create_handlers():
    @api_handler("/test/authentication", "GET")
    def unauthorized_handler():
        return api_response({"status": "unauthenticated"})

    @api_handler(
        "/test/authentication",
        ("OPTION", "POST"),
        {
            "type": "object",
            "properties": {},
            "additionalProperties": False,
        },
        authenticate=True,
    )
    def authenticated_handler(issuer, request=None):
        return api_response({"status": "authenticated", "issuer": issuer})

    @api_handler(
        "/test/admin_authorization",
        "POST",
        {
            "type": "object",
            "properties": {},
            "additionalProperties": False,
        },
        authenticate=True,
    )
    @admin_request
    def authorized_admin_handler(issuer, request):
        return api_response({"status": "authorized_admin", "issuer": issuer})


@pytest.fixture
def clear_auth_cache():
    blackbox._AUTHENTICATION_FALLBACK_CACHE.clear()
    blackbox._AUTHENTICATION_HOT_CACHE.clear()


def test_unauthenticated(walle_test, iterate_authentication):
    result = walle_test.api_client.get("/v1/test/authentication")
    assert result.status_code == http.client.OK
    assert result.json == {"status": "unauthenticated"}


@patch(
    "walle.authorization.blackbox.authenticate", return_value=blackbox.AuthInfo(issuer="issuer-mock", session_id=None)
)
def test_authenticated_ok(authenticate, walle_test):
    result = walle_test.api_client.post("/v1/test/authentication", data={})
    assert result.status_code == http.client.OK
    assert result.json == {"status": "authenticated", "issuer": "issuer-mock"}
    authenticate.assert_called_once_with(config.get_value("oauth.client_id"))


def test_authentication_failure(walle_test, unauthenticated):
    result = walle_test.api_client.post("/v1/test/authentication", data={})
    assert result.status_code == http.client.UNAUTHORIZED
    unauthenticated.assert_called_once_with(config.get_value("oauth.client_id"))


@patch("walle.authorization.blackbox.authenticate", side_effect=blackbox._authenticate_cached)
def test_authentication_blackbox_request_failure(authenticate, walle_test, clear_auth_cache):
    with patch(
        "walle.authorization.blackbox._authenticate", side_effect=blackbox.BlackBoxCommunicationError("Mocked Error")
    ):
        result = walle_test.api_client.post("/v1/test/authentication", data={}, headers={"Authorization": ""})
        assert result.status_code == http.client.INTERNAL_SERVER_ERROR
        assert result.json["message"] == "Internal error occurred: Error in communication with blackbox: Mocked Error"
        assert result.json["result"] == "FAIL"


@patch("walle.authorization.blackbox.authenticate", side_effect=blackbox._authenticate_cached)
@pytest.mark.parametrize("header_value", ["OAuth", "OAuth "])
def test_authentication_blackbox_request_invalid_header(authenticate, walle_test, clear_auth_cache, header_value):
    result = walle_test.api_client.post("/v1/test/authentication", data={}, headers={"Authorization": header_value})
    assert result.status_code == http.client.UNAUTHORIZED
    assert result.json["message"] == "Authentication failed: Authentication failed: Authorization token is empty"
    assert result.json["result"] == "FAIL"


@patch("walle.authorization.blackbox.authenticate", side_effect=blackbox._authenticate_cached)
def test_authentication_blackbox_request_cache_fallback(authenticate, walle_test, clear_auth_cache):
    side_effects = [
        blackbox.AuthInfo(issuer=walle_test.api_issuer, session_id=None),
        blackbox.BlackBoxCommunicationError("Mocked Error"),
    ]

    with patch("walle.authorization.blackbox._authenticate", side_effect=side_effects):
        for _ in (1, 2):
            result = walle_test.api_client.post("/v1/test/authentication", data={}, headers={"Authorization": ""})
            assert result.status_code == http.client.OK
            assert result.json == {"status": "authenticated", "issuer": walle_test.api_issuer}


@patch("walle.authorization.blackbox.authenticate", side_effect=blackbox._authenticate_cached)
def test_csrf_ok(authenticate, walle_test, mp, clear_auth_cache):
    monkeypatch_config(mp, "authorization.csrf_key", "0000")
    session_id = "6a37bb251c8590267ec03770125ae5271a8c74c9"
    valid_csrf_token = csrf.get_csrf_token(session_id)
    session_header = "Session_id={}; Domain=localhost; Path=/".format(session_id)

    with patch("walle.authorization.blackbox._authenticate", return_value=(walle_test.api_issuer, session_id)):
        for method in ("GET", "OPTION", "POST"):
            result = walle_test.api_client.open(
                "/v1/test/authentication",
                method=method,
                data={},
                headers={"X-CSRF-TOKEN": valid_csrf_token, "Cookie": session_header},
            )
            assert result.status_code == http.client.OK


@patch("walle.authorization.blackbox.authenticate", side_effect=blackbox._authenticate_cached)
def test_csrf_fail(authenticate, walle_test, mp, clear_auth_cache):
    monkeypatch_config(mp, "authorization.csrf_key", "0000")
    session_id = "6a37bb251c8590267ec03770125ae5271a8c74c9"
    session_header = "Session_id={}; Domain=localhost; Path=/".format(session_id)

    with patch(
        "walle.authorization.blackbox._authenticate",
        side_effect=[
            (walle_test.api_issuer, session_id),
            # these does not affect the result, auth was cached after the first GET request.
            blackbox.UnauthenticatedError("Mock CSRF token error"),
            blackbox.UnauthenticatedError("Mock CSRF token error"),
        ],
    ):
        for method, code in (("GET", http.client.OK), ("OPTION", http.client.OK), ("POST", http.client.UNAUTHORIZED)):
            result = walle_test.api_client.open(
                "/v1/test/authentication",
                method=method,
                data={},
                headers={"X-CSRF-TOKEN": "INVALID TOKEN", "Cookie": session_header},
            )
            assert result.status_code == code


def test_admin_authorization_ok(walle_test, authorized_admin):
    result = walle_test.api_client.post("/v1/test/admin_authorization", data={})
    assert result.status_code == http.client.OK
    assert result.json == {"status": "authorized_admin", "issuer": walle_test.api_issuer}


def test_admin_authorization_failure(walle_test):
    result = walle_test.api_client.post("/v1/test/admin_authorization", data={})
    assert result.status_code == http.client.FORBIDDEN


@patch("walle.clients.staff.get_user_groups", return_value={"@group1", "@group2"})
def test_project_authorization(get_user_groups, walle_test, mp):
    project = walle_test.mock_project({"id": "some-id", "owners": ["owner0", "owner1"]})
    project.authorize("owner1@")

    with pytest.raises(UnauthorizedError):
        project.authorize("owner2@")

    monkeypatch_config(mp, "authorization.admins", ["owner2"])
    project.authorize("owner2@")


@patch("walle.clients.staff.get_user_groups", return_value={"@group1", "@group2"})
def test_project_authorization_by_group(get_user_groups, walle_test):
    project1 = walle_test.mock_project({"id": "some-id-1", "owners": ["owner1", "@group1"]})
    project2 = walle_test.mock_project({"id": "some-id-2", "owners": ["owner1", "@group3"]})

    project1.authorize("owner1@")
    project1.authorize("owner2@")

    project2.authorize("owner1@")
    with pytest.raises(UnauthorizedError):
        project2.authorize("owner2@")


class TestHostAuthorization:
    @pytest.fixture(params=["owner1@", "owner2@", None])
    def host(self, request, walle_test, mp):
        mp.function(staff.get_user_groups, return_value={"@group1", "@group2"})
        project = walle_test.mock_project({"id": "some-id", "owners": ["owner0", "owner1"]})
        host = walle_test.mock_host({"project": project.id, "status_author": request.param})
        return host

    @pytest.mark.parametrize("ignore_maintenance", [True, False])
    def test_allows_explicit_owners(self, host, ignore_maintenance):
        host.authorize("owner1@", ignore_maintenance=ignore_maintenance)

    @pytest.mark.parametrize("ignore_maintenance", [True, False])
    def test_raises_on_member_not_in_owners(self, host, ignore_maintenance):
        with pytest.raises(UnauthorizedError):
            host.authorize("owner2@", ignore_maintenance=ignore_maintenance)

    @pytest.mark.parametrize("ignore_maintenance", [True, False])
    def test_raises_on_admins_not_in_owners(self, mp, host, ignore_maintenance):
        monkeypatch_config(mp, "authorization.admins", ["owner2"])
        with pytest.raises(UnauthorizedError):
            host.authorize("owner2@", ignore_maintenance=ignore_maintenance)


@patch("walle.clients.staff.get_user_groups", return_value={"@group1", "@group2"})
def test_host_under_maintenance(get_user_groups, walle_test, mp):
    project = walle_test.mock_project({"id": "some-id", "owners": ["owner0", "owner1"]})
    host1 = walle_test.mock_host(
        {"inv": 1, "project": project.id, "state": HostState.MAINTENANCE, "state_author": "owner0@"}
    )
    host2 = walle_test.mock_host(
        {"inv": 2, "project": project.id, "state": HostState.MAINTENANCE, "state_author": None}
    )

    monkeypatch_config(mp, "authorization.admins", ["owner0", "owner1"])

    # Host is under maintenance, authorizing it's owner
    host1.authorize("owner0@", ignore_maintenance=False)
    host1.authorize("owner0@", ignore_maintenance=True)

    # Host is not under maintenance, no state owner
    host2.authorize("owner0@", ignore_maintenance=False)
    host2.authorize("owner0@", ignore_maintenance=True)

    with pytest.raises(HostUnderMaintenanceError):
        host1.authorize("owner1@", ignore_maintenance=False)

    # Host is under maintenance, authorizing with force
    host1.authorize("owner1@", ignore_maintenance=True)

    # Host is not under maintenance, no state owner
    host2.authorize("owner1@", ignore_maintenance=True)
    host2.authorize("owner1@", ignore_maintenance=False)


@patch("walle.clients.staff.get_user_groups", return_value={"@group1", "@group2"})
def test_host_authorization_by_group(get_user_groups, walle_test):
    project1 = walle_test.mock_project({"id": "some-id-1", "owners": ["owner1", "@group1"]})
    host1 = walle_test.mock_host({"project": project1.id, "inv": 1})

    project2 = walle_test.mock_project({"id": "some-id-2", "owners": ["owner1", "@group3"]})
    host2 = walle_test.mock_host({"project": project2.id, "inv": 2})

    host1.authorize("owner1@", ignore_maintenance=False)
    host1.authorize("owner2@", ignore_maintenance=False)

    host2.authorize("owner1@", ignore_maintenance=False)
    with pytest.raises(UnauthorizedError):
        host2.authorize("owner2@", ignore_maintenance=False)
