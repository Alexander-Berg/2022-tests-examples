"""Test report stage handler."""
from contextlib import contextmanager
from unittest.mock import call, Mock

import pytest

from infra.walle.server.tests.lib.util import (
    TestCase,
    check_stage_initialization,
    mock_status_reasons,
    monkeypatch_function,
    mock_task,
    handle_host,
    mock_commit_stage_changes,
    monkeypatch_config,
    mock_fail_current_stage,
    mock_complete_current_stage,
    any_task_status,
    mock_decision,
)
from walle.expert import juggler, decisionmakers
from walle.expert.decision import Decision
from walle.expert.types import WalleAction, CheckType, CheckStatus
from walle.failure_reports.base import ReportPublisher
from walle.failure_reports.startrek import TicketParams
from walle.fsm_stages import startrek_report, report_check_failure
from walle.fsm_stages.common import get_current_stage
from walle.hosts import HostState
from walle.models import timestamp
from walle.stages import Stage, Stages
from walle.clients import dmc

MOCK_REPORT_KEY = "mock-report-key"


@pytest.fixture()
def test(request, monkeypatch_timestamp):
    return TestCase.create(request)


@pytest.fixture
def mock_config(mp):
    monkeypatch_config(mp, "stand.ui_url", "http://localhost")


def test_stage_initialization(test):
    check_stage_initialization(test, Stage(name=Stages.REPORT))


class MockReportPublisher(ReportPublisher):
    name = "mock"

    def get_stream_key(self):
        return "mock-stream-key"

    def close_old_report(self, report_key):
        pass

    def create_new_report(self, summary, report_text, report_hosts, previous_report=None):
        self.summary = summary
        self.report_text = report_text
        self.report_hosts = report_hosts

        return MOCK_REPORT_KEY

    def update_existing_report(self, report, report_text, report_hosts, close=False):
        self.report = report
        self.report_text = report_text
        self.report_hosts = report_hosts


def _monkeypatch_report(mp, mock_report_facade=None):
    if mock_report_facade is None:
        mock_report_facade = Mock(**{"get_report_key.return_value": MOCK_REPORT_KEY})

    @contextmanager
    def mock_report_context(handler):
        yield mock_report_facade

    mp.function(report_check_failure._report_getter, side_effect=mock_report_context)

    return mock_report_facade


def _monkeypatch_health(mp, enabled_checks=CheckType.ALL, check_status=None):
    reasons = mock_status_reasons(enabled_checks=enabled_checks, check_status=check_status)
    mp.function(juggler.get_host_health_reasons, return_value=reasons)


def _mock_decision_makers(mp, action, reason):
    return mp.method(
        decisionmakers.BasicDecisionMaker.make_decision,
        obj=decisionmakers.BasicDecisionMaker,
        return_value=Decision(action, reason),
    )


def test_adds_host_to_report_when_check_is_failing(test, mp):
    # this test patches ErrorHostReports, it only tests that host is added and does not test that report is created.
    _monkeypatch_health(mp)
    _mock_decision_makers(mp, WalleAction.REPORT_FAILURE, "check is failing")
    monkeypatch_function(
        mp,
        dmc.get_decisions_from_handler,
        module=dmc,
        return_value=(None, mock_decision(WalleAction.REPORT_FAILURE)),
    )

    mock_report = _monkeypatch_report(mp)

    host = test.mock_host(
        dict(
            state=HostState.ASSIGNED,
            status=any_task_status(),
            task=mock_task(stage=Stages.REPORT, stage_params={"checks": [CheckType.META], "reason": "Reason mock."}),
        )
    )

    handle_host(host)

    mock_commit_stage_changes(
        host,
        status=startrek_report._STATUS_WAITING_TICKET,
        status_message="Waiting for ticket to be processed.",
        check_after=startrek_report._CHECK_POLLING_PERIOD,
    )

    host.ticket = MOCK_REPORT_KEY
    get_current_stage(host).set_data("tickets", [MOCK_REPORT_KEY])

    assert mock_report.mock_calls == [call.add_host(host, "Reason mock."), call.get_report_key()]
    test.hosts.assert_equal()


@pytest.mark.usefixtures("mock_config")
def test_saves_created_report_key(test, mp):
    # this test verifies that report is actually created with a correct usage pattern
    _monkeypatch_health(mp)
    _mock_decision_makers(mp, WalleAction.REPORT_FAILURE, "check is failing")
    monkeypatch_function(
        mp,
        dmc.get_decisions_from_handler,
        module=dmc,
        return_value=(None, mock_decision(WalleAction.REPORT_FAILURE)),
    )

    mock_publisher = MockReportPublisher()
    monkeypatch_function(
        mp, startrek_report.StarTrekReportPublisher, module=startrek_report, return_value=mock_publisher
    )
    ticket_params_mock = TicketParams()
    mp.function(report_check_failure._ticket_params, return_value=ticket_params_mock)
    mp.function(report_check_failure._create_summary, return_value="mock summary")

    host = test.mock_host(
        dict(
            state=HostState.ASSIGNED,
            status=any_task_status(),
            task=mock_task(stage=Stages.REPORT, stage_params={"checks": [CheckType.META], "reason": "Reason mock."}),
        )
    )

    handle_host(host)

    mock_commit_stage_changes(
        host,
        status=startrek_report._STATUS_WAITING_TICKET,
        status_message="Waiting for ticket to be processed.",
        check_after=startrek_report._CHECK_POLLING_PERIOD,
        temp_data={'ticket_params': ticket_params_mock.to_dict(), 'report_summary': "mock summary"},
    )

    host.ticket = MOCK_REPORT_KEY
    get_current_stage(host).set_data("tickets", [MOCK_REPORT_KEY])

    test.hosts.assert_equal()


def test_removes_host_from_report_when_check_is_passing(test, mp):
    _monkeypatch_health(mp)
    _mock_decision_makers(mp, WalleAction.HEALTHY, "host is healthy")
    monkeypatch_function(
        mp,
        dmc.get_decisions_from_handler,
        module=dmc,
        return_value=(None, mock_decision(WalleAction.HEALTHY)),
    )

    mock_report = _monkeypatch_report(mp)

    host = test.mock_host(
        dict(
            state=HostState.ASSIGNED,
            status=any_task_status(),
            task=mock_task(stage=Stages.REPORT, stage_params={"checks": [CheckType.META], "reason": "Reason mock."}),
        )
    )

    handle_host(host)

    mock_complete_current_stage(host)

    assert mock_report.mock_calls == [call.remove_host(host, "Reason mock.")]
    test.hosts.assert_equal()


def test_keeps_failure_reason_when_host_becomes_healthy(test, mp):
    _monkeypatch_health(mp)
    mock_report = _monkeypatch_report(mp)

    host = test.mock_host(
        dict(
            state=HostState.ASSIGNED,
            status=any_task_status(),
            task=mock_task(stage=Stages.REPORT, stage_params={"checks": [CheckType.META], "reason": "Reason mock."}),
        )
    )

    mock_make_decision = _mock_decision_makers(mp, WalleAction.REPORT_FAILURE, "check is failing")
    decision_handler = monkeypatch_function(
        mp,
        dmc.get_decisions_from_handler,
        module=dmc,
        return_value=(None, mock_decision(WalleAction.REPORT_FAILURE)),
    )

    handle_host(host)
    get_current_stage(host).set_data("tickets", [MOCK_REPORT_KEY])
    mock_commit_stage_changes(
        host, status_message="Waiting for ticket to be processed.", check_after=startrek_report._CHECK_POLLING_PERIOD
    )

    mock_make_decision.return_value = Decision(WalleAction.HEALTHY, "host is healthy")
    decision_handler.return_value = (None, mock_decision(WalleAction.HEALTHY))

    handle_host(host)
    mock_complete_current_stage(host)

    assert mock_report.mock_calls == [
        call.add_host(host, "Reason mock."),
        call.get_report_key(),
        call.remove_host(host, "Reason mock."),
    ]

    test.hosts.assert_equal()


def test_does_nothing_if_health_is_not_available(test, mp):
    _monkeypatch_health(mp, enabled_checks=[])
    mock_report = _monkeypatch_report(mp)

    host = test.mock_host(
        dict(
            state=HostState.ASSIGNED,
            status=any_task_status(),
            task=mock_task(stage=Stages.REPORT, stage_params={"checks": [CheckType.META], "reason": "Reason mock."}),
        )
    )

    handle_host(host)
    mock_commit_stage_changes(
        host,
        status=startrek_report._STATUS_WAITING_HEALTH,
        status_message="Health information is not available.",
        check_after=startrek_report._CHECK_POLLING_PERIOD,
    )

    assert not mock_report.called
    test.hosts.assert_equal()


def test_does_nothing_if_check_is_missing(test, mp):
    _monkeypatch_health(mp, enabled_checks=[CheckType.DISK], check_status=CheckStatus.MISSING)
    mock_report = _monkeypatch_report(mp)
    monkeypatch_function(
        mp,
        dmc.get_decisions_from_handler,
        module=dmc,
        return_value=(None, mock_decision(WalleAction.WAIT)),
    )

    host = test.mock_host(
        dict(
            state=HostState.ASSIGNED,
            status=any_task_status(),
            task=mock_task(stage=Stages.REPORT, stage_params={"checks": [CheckType.DISK], "reason": "Reason mock."}),
        )
    )

    handle_host(host)
    mock_commit_stage_changes(
        host,
        status=startrek_report._STATUS_WAITING_HEALTH,
        status_message="Health information is not complete.",
        check_after=startrek_report._CHECK_POLLING_PERIOD,
    )

    assert not mock_report.called
    test.hosts.assert_equal()


@pytest.mark.usefixtures("monkeypatch_audit_log", "monkeypatch_timestamp")
def test_fails_task_on_timeout(test, mp):
    _monkeypatch_health(mp)
    _mock_decision_makers(mp, WalleAction.REPORT_FAILURE, "host needs a bear")
    decision_handler = monkeypatch_function(
        mp,
        dmc.get_decisions_from_handler,
        module=dmc,
        return_value=(None, mock_decision(WalleAction.REPORT_FAILURE)),
    )

    mock_report = _monkeypatch_report(mp)

    host = test.mock_host(
        dict(
            state=HostState.ASSIGNED,
            status=any_task_status(),
            task=mock_task(
                stage=Stages.REPORT,
                stage_status=startrek_report._STATUS_WAITING_TICKET,
                stage_status_time=timestamp() - startrek_report._TICKET_WAIT_TIMEOUT,
                stage_params={"checks": [CheckType.DISK], "reason": "Reason mock."},
            ),
        )
    )

    handle_host(host)
    mock_fail_current_stage(host, reason="Have been waiting too long for ticket to be processed. Timed out.")

    assert not mock_report.called
    test.hosts.assert_equal()


@pytest.mark.usefixtures("mock_config")
def test_shows_error_when_can_not_create_reports(test, mp):
    # this test verifies that report is actually created with a correct usage pattern
    _monkeypatch_health(mp)
    _mock_decision_makers(mp, WalleAction.REPORT_FAILURE, "check is failing")
    decision_handler = monkeypatch_function(
        mp,
        dmc.get_decisions_from_handler,
        module=dmc,
        return_value=(None, mock_decision(WalleAction.REPORT_FAILURE)),
    )

    exception = startrek_report.CanNotCreateReports("project-mock", "invalid project configuration.")
    mp.function(report_check_failure._ticket_params, side_effect=exception)

    host = test.mock_host(
        dict(
            state=HostState.ASSIGNED,
            status=any_task_status(),
            task=mock_task(
                stage=Stages.REPORT,
                stage_params={"checks": [CheckType.META], "reason": "Reason mock."},
                status=Stages.REPORT + ":" + startrek_report._STATUS_ERROR,
                stage_status=startrek_report._STATUS_ERROR,
                stage_status_time=timestamp() - 100,
            ),
        )
    )
    handle_host(host)
    mock_commit_stage_changes(host, error=str(exception), check_after=startrek_report._CHECK_POLLING_PERIOD)
    test.hosts.assert_equal()
