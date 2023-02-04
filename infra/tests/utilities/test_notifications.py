"""Tests email notifications."""

from unittest.mock import call, ANY, Mock

import pytest

from infra.walle.server.tests.lib.util import patch, patch_attr
from sepelib.core import config
from walle import audit_log
from walle.hosts import Host
from walle.models import timestamp
from walle.util import notifications


@pytest.fixture(autouse=True)
def monkeypatch_config(mp):
    mp.setitem(
        config.get(),
        "notifications",
        {
            "sender": "test-sender",
            "recipients_by_severity": {
                "warning": ["warning1", "warning2"],
                "error": ["error1", "error2"],
                "critical": ["critical1", "critical2"],
            },
        },
    )
    mp.setitem(config.get_value("stand"), "name", "UnitTesting")


@patch("sepelib.util.net.mail.Mail.send_message", autospec=False)
def test_send_info(send, mp):
    notifications._send(notifications.SEVERITY_INFO, "test-subject", "test-message")
    assert send.mock_calls == []

    mp.setitem(config.get_value("notifications.recipients_by_severity"), "info", ["info1", "info2"])
    notifications._send(notifications.SEVERITY_INFO, "test-subject", "test-message")
    assert send.mock_calls == [
        call(
            subject="[Wall-E] INFO: test-subject",
            body="test-message",
            sender="test-sender",
            recipients=sorted({"info1", "info2"}),
            extra_headers={"X-Wall-E-Severity": "info", "X-Wall-E-Stand": "UnitTesting", "X-Wall-E": "True"},
        )
    ]


@patch("sepelib.util.net.mail.Mail.send_message", autospec=False)
def test_send_warning(send):
    notifications._send(notifications.SEVERITY_WARNING, "test-subject", "test-message")
    assert send.mock_calls == [
        call(
            subject="[Wall-E] WARNING: test-subject",
            body="test-message",
            sender="test-sender",
            recipients=sorted({"warning1", "warning2"}),
            extra_headers={"X-Wall-E-Severity": "warning", "X-Wall-E-Stand": "UnitTesting", "X-Wall-E": "True"},
        )
    ]


@patch("sepelib.util.net.mail.Mail.send_message", autospec=False)
def test_send_error(send):
    notifications._send(notifications.SEVERITY_ERROR, "test-subject", "test-message")
    assert send.mock_calls == [
        call(
            subject="[Wall-E] ERROR: test-subject",
            body="test-message",
            sender="test-sender",
            recipients=sorted({"warning1", "warning2", "error1", "error2"}),
            extra_headers={"X-Wall-E-Severity": "error", "X-Wall-E-Stand": "UnitTesting", "X-Wall-E": "True"},
        )
    ]


@patch("sepelib.util.net.mail.Mail.send_message", autospec=False)
@patch("walle.util.notifications._get_recipients_for_project")
@patch(
    "walle.util.notifications._get_recipients_configs_by_query",
    return_value=[
        {
            "warning": ["project-warning"],
            "error": ["project-error"],
            "critical": ["project-critical"],
        }
    ],
)
def test_send_critical(get_configs_for_all_projects, get_recipients_for_project, send):
    notifications._send(notifications.SEVERITY_CRITICAL, "test-subject", "test-message")

    assert get_recipients_for_project.call_count == 0
    get_configs_for_all_projects.assert_called_once_with()

    assert send.mock_calls == [
        call(
            subject="[Wall-E] CRITICAL: test-subject",
            body="test-message",
            sender="test-sender",
            recipients=sorted(
                {
                    "warning1",
                    "warning2",
                    "error1",
                    "error2",
                    "critical1",
                    "critical2",
                    "project-warning",
                    "project-error",
                    "project-critical",
                }
            ),
            extra_headers={"X-Wall-E-Severity": "critical", "X-Wall-E-Stand": "UnitTesting", "X-Wall-E": "True"},
        )
    ]


@patch("sepelib.util.net.mail.Mail.send_message", autospec=False)
@patch(
    "walle.util.notifications._get_recipients_for_project",
    return_value={
        "warning": ["project-warning"],
        "error": ["project-error"],
        "critical": ["project-critical"],
    },
)
@patch("walle.util.notifications._get_recipients_configs_for_all_projects")
def test_send_critical_project(get_configs_for_all_projects, get_recipients_for_project, send):
    notifications._send(
        notifications.SEVERITY_CRITICAL,
        "test_send_critical_project",
        "test-message:test_send_critical_project",
        project_id="project-mock",
    )

    get_recipients_for_project.assert_called_once_with("project-mock", True)
    assert get_configs_for_all_projects.call_count == 0
    assert send.mock_calls == [
        call(
            subject="[Wall-E] CRITICAL: test_send_critical_project",
            body="test-message:test_send_critical_project",
            sender="test-sender",
            recipients=sorted(
                {
                    "warning1",
                    "warning2",
                    "error1",
                    "error2",
                    "critical1",
                    "critical2",
                    "project-warning",
                    "project-error",
                    "project-critical",
                }
            ),
            extra_headers={
                "X-Wall-E-Severity": "critical",
                "X-Wall-E-Project": "project-mock",
                "X-Wall-E-Stand": "UnitTesting",
                "X-Wall-E": "True",
            },
        )
    ]


@patch("sepelib.util.net.mail.Mail.send_message", autospec=False)
def test_send_log_entry(send, mp):
    entry = audit_log.LogEntry(
        time=timestamp(),
        issuer="wall-e",
        type=audit_log.TYPE_REBOOT_HOST,
        project="project-mock",
        host_inv=101,
        host_name="hostname-mock",
        reason="reason-mock",
        payload={"reboot": True},
        status=audit_log.STATUS_ACCEPTED,
        status_time=timestamp(),
        error="error-mock",
    )

    mp.setitem(config.get_value("notifications.recipients_by_severity"), "audit", ["info1", "info2"])
    notifications.on_event(entry)

    subject = "[Wall-E] AUDIT: hostname-mock (project-mock): reboot-host by Wall-E -> accepted"
    assert send.mock_calls == [
        call(
            subject=subject,
            body=ANY,
            sender="test-sender",
            recipients=sorted(("info1", "info2")),
            extra_headers={
                "X-Wall-E-Severity": "audit",
                "X-Wall-E-Status": "accepted",
                "X-Wall-E-Project": "project-mock",
                "X-Wall-E-Stand": "UnitTesting",
                "X-Wall-E-Event": "reboot-host",
                "X-Wall-E": "True",
            },
        )
    ]


@patch("sepelib.util.net.mail.Mail.send_message", autospec=False)
def test_send_rate_limited(send):
    notifications._send(
        notifications.SEVERITY_WARNING,
        "test_send_rate_limited",
        "test-message:test_send_rate_limited",
        rate_limited=True,
    )
    notifications._send(
        notifications.SEVERITY_WARNING,
        "test_send_rate_limited",
        "test-message:test_send_rate_limited",
        rate_limited=True,
    )

    assert send.mock_calls == [
        call(
            subject="[Wall-E] WARNING: test_send_rate_limited",
            body="test-message:test_send_rate_limited",
            sender="test-sender",
            recipients=sorted({"warning1", "warning2"}),
            extra_headers={"X-Wall-E-Severity": "warning", "X-Wall-E-Stand": "UnitTesting", "X-Wall-E": "True"},
        )
    ]


@patch("sepelib.util.net.mail.Mail.send_message", autospec=False)
def test_on_cms_error_is_rate_limited(send, mp):
    mp.function(notifications._dump_response, return_value="mock response-dump")

    notifications.on_cms_api_error("CMS API gone mental", "cms-api-url-mock", "project-mock", Mock())
    notifications.on_cms_api_error("CMS API gone mental", "cms-api-url-mock", "project-mock", Mock())

    assert send.mock_calls == [
        call(
            subject="[Wall-E] ERROR: CMS api for your project project-mock is misbehaving.",
            body=ANY,
            sender="test-sender",
            recipients=sorted(("error1", "error2", "warning1", "warning2")),
            extra_headers={
                "X-Wall-E-Severity": "error",
                "X-Wall-E-Stand": "UnitTesting",
                "X-Wall-E-Event": "cms-api-error",
                "X-Wall-E-Project": "project-mock",
                "X-Wall-E": "True",
            },
        )
    ]


def test_host_link(mp):
    ui_url = "http://unittest-stand.n.yandex.net/walle/ui/"
    mp.setitem(config.get_value("stand"), "ui_url", ui_url)

    host = Host(inv=10001)
    expected_message = "some text\n\nView host in Wall-E: {url}?hosts={host}".format(url=ui_url, host=host.uuid)

    message = notifications._add_host_link("some text", host)
    assert message == expected_message


def test_host_link_misconfigured(mp):
    mock_log_error = patch_attr(mp, notifications.log, "error")

    message = notifications._add_host_link("some text", Host(inv=10001))

    assert message == "some text"
    mock_log_error.assert_called_once_with("Incomplete configuration, value for stand.ui_url is not set")
