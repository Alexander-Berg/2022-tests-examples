"""Test startrek reports facade used in stage handlers."""

from unittest.mock import Mock

import pytest

from infra.walle.server.tests.lib.util import monkeypatch_config, mock_task, AUDIT_LOG_ID, any_task_status
from walle.failure_reports import observers
from walle.failure_reports.base import (
    ErrorHostsReport,
    RotationStrategy,
    ExistingReports,
    ReportPublisher,
    ErrorReportModel,
    now,
    ReportedHost,
)
from walle.fsm_stages.startrek_report import ReportFacade
from walle.hosts import Host
from walle.models import timestamp
from walle.stages import Stages

MOCK_REPORT_KEY = "report-key-mock"
MOCK_STREAM_KEY = "mock-stream-key"
SECTION_NAME = "CheckFailureSection"


@pytest.fixture
def mock_config(mp):
    monkeypatch_config(mp, "stand.ui_url", "http://localhost")


@pytest.mark.usefixtures("mock_config", "monkeypatch_timestamp")
class TestReportsFacade:
    class MockReport(ErrorHostsReport):
        def __init__(self):
            pass

        def set_content(self, summary, content):
            self.summary = summary
            self.content = content

        def get_report_key(self):
            return MOCK_REPORT_KEY

    def test_adds_failed_host_to_report_content(self):
        report = self.MockReport()
        report_facade = ReportFacade("dummy-summy", report)

        host = Host(
            inv=999,
            name="mock-host-name",
            project="project-id-mock",
            status=any_task_status(),
            task=mock_task(stage=Stages.REPORT),
        )
        report_facade.add_host(host, "host has failed spectacularly")

        content = report.content.text()
        assert "mock-host-name" in content
        assert ": host has failed spectacularly" in content

    def test_adds_solved_host_to_report_content(self):
        report = self.MockReport()
        report_facade = ReportFacade("dummy-summy", report)

        host = Host(
            inv=999,
            name="mock-host-name",
            project="project-id-mock",
            status=any_task_status(),
            task=mock_task(stage=Stages.REPORT),
        )
        report_facade.remove_host(host, "host has failed spectacularly")

        content = report.content.text()
        assert "mock-host-name" in content
        assert ": --host has failed spectacularly--" in content

    def test_sets_required_summary_to_the_report(self):
        report = self.MockReport()
        ReportFacade("summary that we set earlier", report)

        assert report.summary == "summary that we set earlier"

    def test_provides_report_key(self):
        report = self.MockReport()
        report_facade = ReportFacade("dummy-summy", report)

        assert MOCK_REPORT_KEY == report_facade.get_report_key()


@pytest.mark.usefixtures("mock_config", "monkeypatch_audit_log")
class TestFacadeReportIntegration:
    class MockRotationStrategy(RotationStrategy):
        name = "mock"

        def __init__(self, current_report=None, prev_report=None, old_reports=None):
            if old_reports is None:
                old_reports = []
            self.current_report = current_report
            self.prev_report = prev_report
            self.old_reports = old_reports

        def find_reports(self, stream_key):
            return ExistingReports(self.current_report, self.prev_report, self.old_reports)

    class MockPublisher(ReportPublisher):
        create_new_report = Mock(return_value=MOCK_REPORT_KEY)
        verify_report_published = Mock(return_value=True)
        update_existing_report = Mock()
        close_old_report = Mock()

    @staticmethod
    def _to_report_host(host, audit_log_id=None):
        return ReportedHost(
            inv=host.inv,
            name=host.name,
            host_uuid=host.uuid,
            project=host.project,
            status=host.status,
            reason="host has failed spectacularly",
            section=SECTION_NAME,
            audit_log_id=audit_log_id,
            report_timestamp=timestamp(),
        )

    def test_creates_report_which_can_be_saved(self, mp, monkeypatch_timestamp):
        # integration test
        mock_report_save = mp.method(ErrorReportModel.create, obj=ErrorReportModel)

        host = Host(
            inv=999,
            name="mock-host-name",
            project="project-id-mock",
            status=any_task_status(),
            task=mock_task(stage=Stages.REPORT),
        )

        report = ErrorHostsReport(
            stream_key=MOCK_STREAM_KEY,
            rotation_strategy=self.MockRotationStrategy(),
            publisher=self.MockPublisher(),
            observer=observers.collect_group(Mock(**{"wrapped_key.return_value": MOCK_STREAM_KEY})),
        )

        with report:
            report_facade = ReportFacade("dummy-summy", report)
            report_facade.add_host(host, "host has failed spectacularly")

        mock_report_save.assert_called_once_with(
            ErrorReportModel,
            report_key=MOCK_REPORT_KEY,
            stream_key=MOCK_STREAM_KEY,
            report_date=now(),
            hosts=[self._to_report_host(host, audit_log_id=AUDIT_LOG_ID)],
        )

    def test_closes_existing_report(self, mp):
        # integration test
        mock_report_update = mp.method(ErrorReportModel.update, obj=ErrorReportModel)

        host = Host(
            inv=999,
            name="mock-host-name",
            project="project-id-mock",
            status=any_task_status(),
            task=mock_task(stage=Stages.REPORT),
        )

        report_hosts = [self._to_report_host(host)]
        mock_old_report = ErrorReportModel(
            report_key=MOCK_REPORT_KEY, stream_key=MOCK_STREAM_KEY, report_date=now(), hosts=report_hosts
        )

        rotation_strategy = self.MockRotationStrategy(current_report=mock_old_report)

        publisher = self.MockPublisher()

        report = ErrorHostsReport(
            stream_key=MOCK_STREAM_KEY,
            rotation_strategy=rotation_strategy,
            publisher=publisher,
            observer=observers.collect_group(Mock(**{"wrapped_key.return_value": MOCK_STREAM_KEY})),
        )

        with report:
            report_facade = ReportFacade("dummy-summy", report)
            report_facade.remove_host(host, "host has failed spectacularly")

        # solve all hosts
        for host in report_hosts:
            host.update({"solved": True, "solve_timestamp": timestamp()})

        assert publisher.close_old_report.called
        mock_report_update.assert_called_once_with(ErrorReportModel, MOCK_REPORT_KEY, closed=True, hosts=report_hosts)
