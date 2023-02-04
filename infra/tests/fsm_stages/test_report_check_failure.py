from unittest.mock import Mock

from infra.walle.server.tests.lib.util import mock_task
from walle.expert.types import CheckType
from walle.failure_reports.startrek import TicketParams
from walle.fsm_stages import startrek_report, report_check_failure
from walle.hosts import Host
from walle.projects import Project
from walle.stages import Stages


def test_hostname_is_tag(mp):
    # start with empty parameters
    mp.function(
        report_check_failure.ticket_params_for_project, module=report_check_failure, return_value=TicketParams()
    )

    host = Host(name="mock-host-name", task=mock_task(stage=Stages.REPORT))

    def verify(ticket_params, summary):
        assert ticket_params.get("tags") == [host.name]

    mp.function(startrek_report.get_report, side_effect=verify)

    report_check_failure._report_getter(Mock(host=host, project=Project(), checks=[]))


def test_checks_are_tags(mp):
    # start with empty parameters
    mp.function(
        report_check_failure.ticket_params_for_project, module=report_check_failure, return_value=TicketParams()
    )

    host = Host(name="mock-host-name", task=mock_task(stage=Stages.REPORT))
    project = Project()

    def verify(ticket_params, summary):
        assert sorted(ticket_params.get("tags")) == sorted([host.name, CheckType.SSH, CheckType.UNREACHABLE])

    mp.function(startrek_report.get_report, side_effect=verify)

    report_check_failure._report_getter(Mock(host=host, project=project, checks=[CheckType.SSH, CheckType.UNREACHABLE]))


def test_default_tags_preserved(mp):
    # start with empty parameters
    mp.function(
        report_check_failure.ticket_params_for_project,
        module=report_check_failure,
        return_value=TicketParams(tags=["default-tag"]),
    )

    host = Host(name="mock-host-name", task=mock_task(stage=Stages.REPORT))
    project = Project()

    def verify(ticket_params, summary):
        assert sorted(ticket_params.get("tags")) == sorted(["default-tag", host.name, CheckType.SSH])

    mp.function(startrek_report.get_report, side_effect=verify)

    report_check_failure._report_getter(Mock(host=host, project=project, checks=[CheckType.SSH]))


def test_summary_contains_check_name_and_host_name(mp):
    # start with empty parameters
    mp.function(
        report_check_failure.ticket_params_for_project, module=report_check_failure, return_value=TicketParams()
    )

    host = Host(name="mock-host-name", task=mock_task(stage=Stages.REPORT))
    project = Project()

    def verify(ticket_params, summary):
        assert host.name in summary
        assert CheckType.CPU_CAPPING in summary

    mp.function(startrek_report.get_report, side_effect=verify)

    report_check_failure._report_getter(
        Mock(host=host, project=project, checks=[CheckType.CPU_CAPPING, CheckType.SSH, CheckType.META])
    )
