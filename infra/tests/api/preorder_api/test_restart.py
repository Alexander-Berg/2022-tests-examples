"""Tests preorder restarting API."""

import pytest
import http.client

from infra.walle.server.tests.lib.util import BOT_PROJECT_ID, TestCase, AUDIT_LOG_ID
from walle import audit_log, preorders
from walle.models import timestamp


@pytest.fixture
def test(request):
    return TestCase.create(request)


def test_unauthenticated(test, unauthenticated):
    result = test.api_client.post("/v1/preorders/1/restart", data={})
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

    result = test.api_client.post("/v1/preorders/1/restart", data={})
    assert result.status_code == http.client.FORBIDDEN
    assert result.json["message"] == "Authorization failure: You must be owner of #1 preorder to perform this request."

    test.preorders.assert_equal()


@pytest.mark.usefixtures("monkeypatch_audit_log")
@pytest.mark.parametrize("processed", [True, False])
def test_restart_preorder(test, processed):
    # add extra preorder that should not be affected
    test.preorders.mock(dict(id=1, processed=True, audit_log_id=None))

    preorder = test.preorders.mock(
        dict(
            id=2,
            owner=test.api_user,
            project=test.default_project.id,
            audit_log_id=None,
            bot_project=BOT_PROJECT_ID,
            prepare=False,
            processed=processed,
            acquired_hosts=[1, 2],
            failed_hosts=[3, 4],
        )
    )

    preorder.audit_log_id = AUDIT_LOG_ID
    preorder.processed = False
    del preorder.failed_hosts

    result = test.api_client.post("/v1/preorders/2/restart", data={})
    assert result.status_code == http.client.OK
    assert result.mimetype == "application/json"
    assert result.json == preorder.to_api_obj()

    test.preorders.assert_equal()


@pytest.mark.usefixtures("monkeypatch_audit_log", "monkeypatch_timestamp")
def test_restart_preorder_cancels_existing_audit_log(test):
    old_entry = test.audit_log.mock(
        dict(id="old-audit-log-id-mock", status=audit_log.STATUS_ACCEPTED, type=audit_log.TYPE_PROCESS_PREORDER)
    )

    preorder = test.preorders.mock(
        dict(
            id=1,
            owner=test.api_user,
            project=test.default_project.id,
            bot_project=BOT_PROJECT_ID,
            audit_log_id=old_entry.id,
            processed=False,
        )
    )

    result = test.api_client.post("/v1/preorders/1/restart", data={})
    assert result.status_code == http.client.OK

    preorder.audit_log_id = AUDIT_LOG_ID
    preorder.processed = False
    del preorder.failed_hosts
    test.preorders.assert_equal()

    old_entry.status = audit_log.STATUS_CANCELLED
    old_entry.status_time = timestamp()
    test.preorders.assert_equal()


@pytest.mark.usefixtures("monkeypatch_audit_log")
def test_restart_preorder_conflict_already_finished(test, mp):
    """Test that restart works if preorder was finished."""

    restart_processing_orig = preorders.restart_processing

    def introduce_conflict(issuer, preorder, reason):
        preorders.Preorder.objects(id=preorder.id).update(
            unset__audit_log_id=True, set__processed=True, unset__messages=True
        )
        return restart_processing_orig(issuer, preorder, reason)

    mp.function(preorders.restart_processing, side_effect=introduce_conflict)
    preorder = test.preorders.mock(
        dict(id=1, owner=test.api_user, project=test.default_project.id, processed=False, bot_project=BOT_PROJECT_ID)
    )

    result = test.api_client.post("/v1/preorders/1/restart", data={})
    assert result.status_code == http.client.OK

    preorder.audit_log_id = AUDIT_LOG_ID
    preorder.processed = False
    test.preorders.assert_equal()


@pytest.mark.usefixtures("monkeypatch_audit_log")
def test_restart_preorder_conflict_already_restarted(test, mp):
    """Test that restart works if preorder was finished."""
    conflicted_audit_log_id = "changed-log-id"
    restart_processing_orig = preorders.restart_processing

    def introduce_conflict(issuer, preorder, reason):
        preorders.Preorder.objects(id=preorder.id).update(
            set__audit_log_id=conflicted_audit_log_id, set__processed=False
        )
        return restart_processing_orig(issuer, preorder, reason)

    mp.function(preorders.restart_processing, side_effect=introduce_conflict)

    preorder = test.preorders.mock(
        dict(id=1, owner=test.api_user, project=test.default_project.id, processed=True, bot_project=BOT_PROJECT_ID)
    )

    result = test.api_client.post("/v1/preorders/1/restart", data={})
    assert result.status_code == http.client.CONFLICT
    assert result.json["message"] == "Preorder 1 has changed it's state."

    preorder.audit_log_id = conflicted_audit_log_id
    preorder.processed = False
    test.preorders.assert_equal()


def test_restart_preorder_conflict_deleted(test, mp):
    """Test that restart works if preorder was finished."""
    restart_processing_orig = preorders.restart_processing

    def introduce_conflict(issuer, preorder, reason):
        preorders.Preorder.objects(id=preorder.id).delete()
        return restart_processing_orig(issuer, preorder, reason)

    mp.function(preorders.restart_processing, side_effect=introduce_conflict)

    test.preorders.mock(
        dict(id=1, owner=test.api_user, project=test.default_project.id, processed=True, bot_project=BOT_PROJECT_ID),
        add=False,
    )

    result = test.api_client.post("/v1/preorders/1/restart", data={})
    assert result.status_code == http.client.CONFLICT
    assert result.json["message"] == "Preorder 1 has changed it's state."

    test.preorders.assert_equal()


def test_restart_missing(test):
    test.preorders.mock(dict(id=1, owner="some-user", project=test.default_project.id, bot_project=BOT_PROJECT_ID))

    result = test.api_client.post("/v1/preorders/2/restart", data={})

    assert result.status_code == http.client.NOT_FOUND
    assert result.json["message"] == "The specified preorder ID doesn't exist."
    test.preorders.assert_equal()
