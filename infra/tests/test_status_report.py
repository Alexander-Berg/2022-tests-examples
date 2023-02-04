from unittest.mock import call, ANY

import pytest

from infra.walle.server.tests.lib.util import monkeypatch_request, mock_response, monkeypatch_config
from walle import status_report, audit_log
from walle.application import app
from walle.clients import juggler
from walle.expert.automation_plot import AutomationPlot, Check
from walle.projects import Project

STATUS_OK = juggler.JugglerCheckStatus.OK
STATUS_CRIT = juggler.JugglerCheckStatus.CRIT


@pytest.fixture()
def enable_juggler_events(mp):
    monkeypatch_config(mp, "juggler.events_enabled", True)
    monkeypatch_config(mp, "juggler.agent_host_port", "localhost:9090")


@pytest.mark.usefixtures("walle_test", "enable_juggler_events")
class TestGlobalAutomationStatusReport:
    @staticmethod
    def _set_automation(healing_enabled, dns_enabled):
        settings = app.settings()
        settings.disable_healing_automation = not healing_enabled
        settings.disable_dns_automation = not dns_enabled
        settings.save()

    @pytest.mark.parametrize("healing_enabled", [True, False])
    @pytest.mark.parametrize("dns_enabled", [True, False])
    def test_automation_status_reported(self, mp, healing_enabled, dns_enabled):
        self._set_automation(healing_enabled, dns_enabled)
        mock_send_events = mp.function(juggler.send_event)

        status_report.report_global_automation_status()

        expected_calls = [
            call(
                "healing_enabled",
                STATUS_OK if healing_enabled else STATUS_CRIT,
                "Have never been switched.",
                ["wall-e.healing_enabled"],
            ),
            call(
                "dns_enabled",
                STATUS_OK if dns_enabled else STATUS_CRIT,
                "Have never been switched.",
                ["wall-e.dns_enabled"],
            ),
        ]

        assert expected_calls == mock_send_events.mock_calls

    def test_failures_logged(self, mp):
        self._set_automation(healing_enabled=True, dns_enabled=True)

        response = mock_response(
            {"message": "Expecting value: line 1 column 1 (char 0)", "success": False}, status_code=400
        )
        monkeypatch_request(mp, return_value=response)
        mock_error_logger = mp.method(juggler.log.error, obj=juggler.log)

        status_report.report_global_automation_status()
        expected_logger_calls = [
            call(
                "Error in communication with juggler agent: %s: %s. Events data was %s",
                "400 Client Error: None for url: None",
                "Expecting value: line 1 column 1 (char 0)",
                ANY,  # events data
            ),
            call(
                "Error in communication with juggler agent: %s: %s. Events data was %s",
                "400 Client Error: None for url: None",
                "Expecting value: line 1 column 1 (char 0)",
                ANY,  # events data
            ),
        ]
        assert expected_logger_calls == mock_error_logger.mock_calls

    @pytest.mark.parametrize("enabled", [True, False])
    def test_audit_log_usage(self, mp, enabled):
        self._set_automation(healing_enabled=enabled, dns_enabled=enabled)
        mock_send_events = mp.function(juggler.send_event)

        # older messages should be ignored
        audit_log.on_change_healing_status("issued", enable=True, reason="older message mock")
        audit_log.on_change_healing_status("issued", enable=False, reason="older message mock")
        audit_log.on_change_dns_automation_status("issued", enable=True, reason="older message mock")
        audit_log.on_change_dns_automation_status("issued", enable=False, reason="older message mock")

        # current messages, should be used
        audit_log.on_change_healing_status("issued", enable=True, reason="healing enable reason mock")
        audit_log.on_change_healing_status("issued", enable=False, reason="healing disable reason mock")
        audit_log.on_change_dns_automation_status("issued", enable=True, reason="dns enable reason mock")
        audit_log.on_change_dns_automation_status("issued", enable=False, reason="dns disable reason mock")

        status_report.report_global_automation_status()

        expected_calls = [
            call(
                "healing_enabled",
                STATUS_OK if enabled else STATUS_CRIT,
                "healing {} reason mock".format("enable" if enabled else "disable"),
                ["wall-e.healing_enabled"],
            ),
            call(
                "dns_enabled",
                STATUS_OK if enabled else STATUS_CRIT,
                "dns {} reason mock".format("enable" if enabled else "disable"),
                ["wall-e.dns_enabled"],
            ),
        ]

        assert expected_calls == mock_send_events.mock_calls


@pytest.mark.usefixtures("enable_juggler_events")
class TestProjectAutomationStatusReport:
    def _mock_projects(self):
        return [Project()]

    @pytest.mark.parametrize("healing_enabled", [True, False])
    @pytest.mark.parametrize("dns_enabled", [True, False])
    def test_events_for_project_automation(self, mp, healing_enabled, dns_enabled):
        project = Project(
            id="mock-id",
            healing_automation={"enabled": healing_enabled},
            dns_automation={"enabled": dns_enabled},
        )
        mp.function(status_report._fetch_projects, return_value=[project])
        mock_send_events_batch = mp.function(juggler.send_batch)
        mp.function(juggler.send_event)

        status_report.report_project_automation_status()

        healing_message = "Healing automation is {} in project mock-id.".format(
            "enabled" if healing_enabled else "disabled"
        )
        dns_message = "DNS automation is {} in project mock-id.".format("enabled" if dns_enabled else "disabled")

        mock_send_events_batch.assert_called_once_with(
            [
                {
                    "host": "wall-e.project.mock-id",
                    "service": "healing_enabled",
                    "status": STATUS_OK if healing_enabled else STATUS_CRIT,
                    "description": healing_message,
                    "tags": ["wall-e.healing_enabled", "wall-e.project.mock-id"],
                },
                {
                    "host": "wall-e.project.mock-id",
                    "service": "dns_enabled",
                    "status": STATUS_OK if dns_enabled else STATUS_CRIT,
                    "description": dns_message,
                    "tags": ["wall-e.dns_enabled", "wall-e.project.mock-id"],
                },
            ]
        )

    def test_uses_automation_status_message(self, mp):
        project = Project(
            id="mock-id",
            healing_automation={"enabled": True, "status_message": "healing enabled mock"},
            dns_automation={"enabled": False, "status_message": "dns disabled mock"},
        )
        mp.function(status_report._fetch_projects, return_value=[project])
        mock_send_events_batch = mp.function(juggler.send_batch)
        mp.function(juggler.send_event)

        status_report.report_project_automation_status()

        mock_send_events_batch.assert_called_once_with(
            [
                {
                    "host": "wall-e.project.mock-id",
                    "service": "healing_enabled",
                    "status": STATUS_OK,
                    "description": "healing enabled mock",
                    "tags": ["wall-e.healing_enabled", "wall-e.project.mock-id"],
                },
                {
                    "host": "wall-e.project.mock-id",
                    "service": "dns_enabled",
                    "status": STATUS_CRIT,
                    "description": "dns disabled mock",
                    "tags": ["wall-e.dns_enabled", "wall-e.project.mock-id"],
                },
            ]
        )

    def test_uses_project_tags_for_event_tags(self, mp):
        project = Project(
            id="mock-id",
            tags=["tag1", "tag2"],
            healing_automation={"enabled": True, "status_message": "healing enabled mock"},
            dns_automation={"enabled": False, "status_message": "dns disabled mock"},
        )
        mp.function(status_report._fetch_projects, return_value=[project])
        mock_send_events_batch = mp.function(juggler.send_batch)
        mp.function(juggler.send_event)

        status_report.report_project_automation_status()

        mock_send_events_batch.assert_called_once_with(
            [
                {
                    "host": "wall-e.project.mock-id",
                    "service": "healing_enabled",
                    "status": STATUS_OK,
                    "description": "healing enabled mock",
                    "tags": ["wall-e.healing_enabled", "wall-e.project.mock-id", "wall-e.tag.tag1", "wall-e.tag.tag2"],
                },
                {
                    "host": "wall-e.project.mock-id",
                    "service": "dns_enabled",
                    "status": STATUS_CRIT,
                    "description": "dns disabled mock",
                    "tags": ["wall-e.dns_enabled", "wall-e.project.mock-id", "wall-e.tag.tag1", "wall-e.tag.tag2"],
                },
            ]
        )

    def test_reports_success_to_juggler(self, mp):
        project = Project(
            id="mock-id",
            healing_automation={"enabled": True, "status_message": "healing enabled mock"},
            dns_automation={"enabled": False, "status_message": "dns disabled mock"},
        )
        mp.function(status_report._fetch_projects, return_value=[project])
        mp.function(juggler.send_batch)
        mock_send_event = mp.function(juggler.send_event)

        status_report.report_project_automation_status()

        mock_send_event.assert_called_once_with(
            "wall-e.projects-automation-report", STATUS_OK, "Report for projects automation status sent to juggler."
        )

    def test_reports_failure_to_juggler(self, mp):
        project = Project(
            id="mock-id",
            healing_automation={"enabled": True, "status_message": "healing enabled mock"},
            dns_automation={"enabled": False, "status_message": "dns disabled mock"},
        )
        mp.function(status_report._fetch_projects, return_value=[project])
        mp.function(juggler.send_batch, return_value=["juggler error mock"])
        mock_send_event = mp.function(juggler.send_event)

        status_report.report_project_automation_status()

        mock_send_event.assert_called_once_with(
            "wall-e.projects-automation-report",
            STATUS_CRIT,
            "Failure while sending report for projects automation status to juggler: juggler error mock.",
        )


@pytest.mark.usefixtures("enable_juggler_events")
class TestPlotAutomationStatusReport:
    def _mock_plot(self):
        return [AutomationPlot()]

    @pytest.mark.parametrize("healing_enabled", [True, False])
    def test_events_for_project_automation(self, mp, healing_enabled):
        plot = AutomationPlot(id="mock-id", checks=[Check(name="mock_check", enabled=healing_enabled)])
        mp.function(status_report._fetch_automation_plots, return_value=[plot])
        mock_send_events_batch = mp.function(juggler.send_batch)
        mp.function(juggler.send_event)

        status_report.report_plot_automation_status()

        healing_message = "Healing for check mock_check in automation plot mock-id is {}".format(
            "enabled" if healing_enabled else "disabled"
        )

        mock_send_events_batch.assert_called_once_with(
            [
                {
                    "host": "wall-e.automation-plot.mock-id.mock_check",
                    "service": "healing_enabled",
                    "status": STATUS_OK if healing_enabled else STATUS_CRIT,
                    "description": healing_message,
                    "tags": ["wall-e.healing_enabled", "wall-e.automation-plot.mock-id"],
                }
            ]
        )

    def test_reports_success_to_juggler(self, mp):
        plot = AutomationPlot(id="mock-id", checks=[Check(name="mock_check", enabled=True)])
        mp.function(status_report._fetch_automation_plots, return_value=[plot])
        mp.function(juggler.send_batch)
        mock_send_event = mp.function(juggler.send_event)

        status_report.report_plot_automation_status()

        mock_send_event.assert_called_once_with(
            "wall-e.automation-plot-report", STATUS_OK, "Report for automation-plot status sent to juggler."
        )

    def test_reports_failure_to_juggler(self, mp):
        plot = AutomationPlot(id="mock-id", checks=[Check(name="mock_check", enabled=True)])
        mp.function(status_report._fetch_automation_plots, return_value=[plot])
        mp.function(juggler.send_batch, return_value=["juggler error mock"])
        mock_send_event = mp.function(juggler.send_event)

        status_report.report_plot_automation_status()

        mock_send_event.assert_called_once_with(
            "wall-e.automation-plot-report",
            STATUS_CRIT,
            "Failure while sending report for projects automation status to juggler: juggler error mock.",
        )
