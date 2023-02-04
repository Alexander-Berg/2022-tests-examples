"""Tests for fsm handbrake method."""

import pytest
import http.client

from infra.walle.server.tests.lib.util import TestCase, AUDIT_LOG_ID
from sepelib.core.constants import DAY_SECONDS, HOUR_SECONDS
from walle.models import timestamp
from walle.projects import FsmHandbrake
from walle.util.misc import drop_none


@pytest.fixture
def test(request, monkeypatch_audit_log, monkeypatch_timestamp):
    test = TestCase.create(request)
    request.addfinalizer(test.projects.assert_equal)
    test.mock_projects()
    return test


@pytest.mark.parametrize("method", ["POST", "PUT"])
def test_unauthenticated(test, unauthenticated, method):
    project = test.mock_project({"id": "some-id"})
    result = test.api_client.open("/v1/projects/" + project.id + "/fsm-handbrake", method=method, data={})
    assert result.status_code == http.client.UNAUTHORIZED


@pytest.mark.parametrize("method", ["POST", "PUT"])
def test_unauthorized(test, unauthorized_project, method):
    project = test.mock_project({"id": "some-id"})
    result = test.api_client.open("/v1/projects/" + project.id + "/fsm-handbrake", method=method, data={})
    assert result.status_code == http.client.FORBIDDEN


class TestSetFsmHandbrake:
    @pytest.mark.parametrize("method", ["POST", "PUT"])
    def test_set_fsm_handbrake_without_params(self, test, method):
        project = test.mock_project({"id": "some-id"})
        result = _set_fsm_with_api_call(test, project, method, data={})

        assert result.status_code == http.client.OK
        _assert_fsm_handbrake_matches(test, result, project, _make_fsm_handbrake())

    @pytest.mark.parametrize("method", ["POST", "PUT"])
    def test_set_fsm_handbrake_with_timeout(self, test, method):
        project = test.mock_project({"id": "some-id"})
        result = _set_fsm_with_api_call(test, project, method, data={"timeout": HOUR_SECONDS})

        assert result.status_code == http.client.OK
        _assert_fsm_handbrake_matches(
            test, result, project, _make_fsm_handbrake(timeout_time=timestamp() + HOUR_SECONDS)
        )

    @pytest.mark.parametrize("method", ["POST", "PUT"])
    def test_set_fsm_handbrake_with_timeout_time(self, test, method):
        project = test.mock_project({"id": "some-id"})
        result = _set_fsm_with_api_call(test, project, method, data={"timeout_time": timestamp() + HOUR_SECONDS + 15})

        assert result.status_code == http.client.OK
        _assert_fsm_handbrake_matches(
            test, result, project, _make_fsm_handbrake(timeout_time=timestamp() + HOUR_SECONDS + 15)
        )

    @pytest.mark.parametrize("method", ["POST", "PUT"])
    def test_set_fsm_handbrake_with_reason(self, test, method):
        project = test.mock_project({"id": "some-id"})
        result = _set_fsm_with_api_call(test, project, method, data={"reason": "reason-mock"})

        assert result.status_code == http.client.OK
        _assert_fsm_handbrake_matches(test, result, project, _make_fsm_handbrake(reason="reason-mock"))

    @pytest.mark.parametrize("method", ["POST", "PUT"])
    def test_set_fsm_handbrake_with_ticket_key(self, test, method):
        project = test.mock_project({"id": "some-id"})
        result = _set_fsm_with_api_call(test, project, method, data={"ticket_key": "BURNE-0000"})

        assert result.status_code == http.client.OK
        _assert_fsm_handbrake_matches(test, result, project, _make_fsm_handbrake(ticket_key="BURNE-0000"))


class TestExtendFsmHandbrake:
    @pytest.mark.parametrize("method", ["POST", "PUT"])
    def test_extend_fsm_handbrake_without_params(self, test, method):
        project = test.mock_project({"id": "some-id", "fsm_handbrake": _make_fsm_handbrake(timeout_time=timestamp())})
        result = _set_fsm_with_api_call(test, project, method, data={})

        assert result.status_code == http.client.OK
        _assert_fsm_handbrake_matches(test, result, project, _make_fsm_handbrake())

    @pytest.mark.parametrize("method", ["POST", "PUT"])
    @pytest.mark.parametrize("new_reason", ["new-reason", None])
    def test_extend_fsm_handbrake_keeps_reason(self, test, method, new_reason):
        project = test.mock_project({"id": "some-id", "fsm_handbrake": _make_fsm_handbrake(reason="old-reason")})
        result = _set_fsm_with_api_call(test, project, method, data=drop_none({"reason": new_reason}))

        assert result.status_code == http.client.OK
        _assert_fsm_handbrake_matches(test, result, project, _make_fsm_handbrake(reason="old-reason"))

    @pytest.mark.parametrize("method", ["POST", "PUT"])
    @pytest.mark.parametrize("new_reason", ["new-reason", None])
    def test_extend_fsm_handbrake_adds_reason_if_missing(self, test, method, new_reason):
        project = test.mock_project({"id": "some-id", "fsm_handbrake": _make_fsm_handbrake()})
        result = _set_fsm_with_api_call(test, project, method, data=drop_none({"reason": new_reason}))

        assert result.status_code == http.client.OK
        _assert_fsm_handbrake_matches(test, result, project, _make_fsm_handbrake(reason=new_reason))

    @pytest.mark.parametrize("method", ["POST", "PUT"])
    @pytest.mark.parametrize("new_ticket_key", ["NEW-000", None])
    def test_extend_fsm_handbrake_keeps_ticket_key(self, test, method, new_ticket_key):
        project = test.mock_project({"id": "some-id", "fsm_handbrake": _make_fsm_handbrake(ticket_key="OLD-000")})
        result = _set_fsm_with_api_call(test, project, method, data=drop_none({"ticket_key": new_ticket_key}))

        assert result.status_code == http.client.OK
        _assert_fsm_handbrake_matches(test, result, project, _make_fsm_handbrake(ticket_key="OLD-000"))

    @pytest.mark.parametrize("method", ["POST", "PUT"])
    @pytest.mark.parametrize("new_ticket_key", ["NEW-000", None])
    def test_extend_fsm_handbrake_adds_ticket_key_if_missing(self, test, method, new_ticket_key):
        project = test.mock_project({"id": "some-id", "fsm_handbrake": _make_fsm_handbrake()})
        result = _set_fsm_with_api_call(test, project, method, data=drop_none({"ticket_key": new_ticket_key}))

        assert result.status_code == http.client.OK
        _assert_fsm_handbrake_matches(test, result, project, _make_fsm_handbrake(ticket_key=new_ticket_key))

    @pytest.mark.parametrize("method", ["POST", "PUT"])
    def test_extend_fsm_handbrake_with_timeout_time(self, test, method):
        project = test.mock_project({"id": "some-id", "fsm_handbrake": _make_fsm_handbrake(timeout_time=timestamp())})
        result = _set_fsm_with_api_call(test, project, method, data={"timeout_time": timestamp() + HOUR_SECONDS + 15})

        assert result.status_code == http.client.OK
        _assert_fsm_handbrake_matches(
            test, result, project, _make_fsm_handbrake(timeout_time=timestamp() + HOUR_SECONDS + 15)
        )

    @pytest.mark.parametrize("method", ["POST", "PUT"])
    def test_extend_fsm_handbrake_with_timeout(self, test, method):
        project = test.mock_project({"id": "some-id", "fsm_handbrake": _make_fsm_handbrake(timeout_time=timestamp())})
        result = _set_fsm_with_api_call(test, project, method, data={"timeout": HOUR_SECONDS + 10})

        assert result.status_code == http.client.OK
        _assert_fsm_handbrake_matches(
            test, result, project, _make_fsm_handbrake(timeout_time=timestamp() + HOUR_SECONDS + 10)
        )


def test_unset_handbrake(test):
    project = test.mock_project({"id": "some-id", "fsm_handbrake": _make_fsm_handbrake()})
    result = test.api_client.open("/v1/projects/" + project.id + "/fsm-handbrake", method="DELETE")

    assert result.status_code == http.client.OK
    del project.fsm_handbrake

    test.projects.assert_equal()
    assert {"id": project.id, "name": project.name} == result.json


def _set_fsm_with_api_call(test, project, method, data):
    return test.api_client.open("/v1/projects/" + project.id + "/fsm-handbrake", method=method, data=data or {})


def _make_fsm_handbrake(timeout_time=None, reason=None, ticket_key=None):
    return FsmHandbrake(
        issuer=TestCase.api_issuer,
        timestamp=timestamp(),
        timeout_time=timeout_time or timestamp() + DAY_SECONDS,
        reason=reason,
        ticket_key=ticket_key,
        audit_log_id=AUDIT_LOG_ID,
    )


def _assert_fsm_handbrake_matches(test, result, project, expected_fsm_handbrake_obj):
    project.fsm_handbrake = expected_fsm_handbrake_obj

    test.projects.assert_equal()

    expected_project_json = {
        "id": project.id,
        "fsm_handbrake": expected_fsm_handbrake_obj.to_mongo(),
    }
    assert expected_project_json == result.json
