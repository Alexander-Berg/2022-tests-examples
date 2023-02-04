"""Tests audit log."""

import time
from unittest.mock import Mock

import pytest

from infra.walle.server.tests.lib.util import monkeypatch_audit_log, AUDIT_LOG_ID
from sepelib.mongo.mock import ObjectMocker
from walle import audit_log
from walle import authorization
from walle.errors import ApiError


@pytest.fixture
def log(database):
    return ObjectMocker(
        audit_log.LogEntry,
        {
            "id": "uuid",
            "time": 999.99,
            "issuer": authorization.ISSUER_WALLE,
            "type": audit_log.TYPES[0],
            "status": audit_log.STATUS_UNKNOWN,
            "status_time": 999.99,
        },
    )


@pytest.fixture(autouse=True)
def patch_audit_log_time(monkeypatch):
    cur_time = time.time()
    monkeypatch_audit_log(monkeypatch, uuid=None, time=cur_time, patch_create=False)

    return cur_time


def test_create_entry(monkeypatch, log):
    cur_time = time.time()
    monkeypatch_audit_log(monkeypatch, uuid=AUDIT_LOG_ID, time=cur_time, patch_create=False)

    entry = log.mock(
        {
            "id": AUDIT_LOG_ID,
            "time": cur_time,
            "issuer": authorization.ISSUER_WALLE,
            "type": audit_log.TYPES[0],
            "status": audit_log.STATUS_UNKNOWN,
            "status_time": cur_time,
        },
        save=False,
    )

    created_entry = audit_log.create(issuer=authorization.ISSUER_WALLE, type=audit_log.TYPES[0])
    assert created_entry.to_mongo() == entry.to_mongo()

    log.assert_equal()


@pytest.mark.parametrize("status", audit_log.STATUSES)
def test_complete_task(patch_audit_log_time, log, status):
    for i in range(2):
        entry = log.mock({"id": audit_log._uuid(), "status": status})

    task = Mock(audit_log_id=entry.id)
    audit_log.complete_task(task)
    if status in (audit_log.STATUS_UNKNOWN, audit_log.STATUS_ACCEPTED):
        entry.status = audit_log.STATUS_COMPLETED
        entry.status_time = patch_audit_log_time

    log.assert_equal()


@pytest.mark.parametrize("payload", (None, {"some-key": "some-value"}))
def test_complete_with_payload(patch_audit_log_time, log, payload):
    entry = log.mock({"id": audit_log._uuid(), "status": audit_log.STATUS_UNKNOWN, "payload": payload})

    extra_payload = {
        "some-extra-key": "some-extra-value",
        "some-extra-hash": {
            "a": 1,
            "b": 2,
        },
    }

    audit_log.complete_request(entry.copy(), extra_payload=extra_payload)

    entry.status = audit_log.STATUS_COMPLETED
    entry.status_time = patch_audit_log_time
    entry.payload = dict(payload or {}, **extra_payload)

    log.assert_equal()


@pytest.mark.parametrize("status", audit_log.STATUSES)
def test_fail_task(patch_audit_log_time, log, status):
    for i in range(2):
        entry = log.mock({"id": audit_log._uuid(), "status": status})

    task = Mock(audit_log_id=entry.id)
    audit_log.fail_task(task, "test error")
    if status in (audit_log.STATUS_UNKNOWN, audit_log.STATUS_ACCEPTED):
        entry.status = audit_log.STATUS_FAILED
        entry.status_time = patch_audit_log_time
        entry.error = "test error"

    log.assert_equal()


@pytest.mark.parametrize("type", audit_log.TYPES)
def test_context_manager_accepted(patch_audit_log_time, log, type):
    with log.mock({"type": type}) as entry:
        log.assert_equal()

    entry.status = audit_log.STATUS_COMPLETED if type in audit_log.INSTANT_TYPES else audit_log.STATUS_ACCEPTED
    entry.status_time = patch_audit_log_time
    log.assert_equal()


def test_context_manager_rejected(patch_audit_log_time, log):
    with pytest.raises(ApiError):
        with log.mock() as entry:
            raise ApiError(0, "test error")

    entry.status = audit_log.STATUS_REJECTED
    entry.status_time = patch_audit_log_time
    entry.error = "test error"
    log.assert_equal()


def test_context_manager_failed(patch_audit_log_time, log):
    class SomeException(Exception):
        pass

    with pytest.raises(SomeException):
        with log.mock() as entry:
            raise SomeException("test error")

    entry.status = audit_log.STATUS_FAILED
    entry.status_time = patch_audit_log_time
    entry.error = "test error"
    log.assert_equal()
