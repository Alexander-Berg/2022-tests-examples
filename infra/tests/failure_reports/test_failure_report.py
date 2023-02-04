"""Tests for failure report functions."""
import datetime
import json
import random
import time
from collections import defaultdict
from textwrap import dedent
from unittest.mock import call, Mock

import pytest

import walle.clients.utils
import walle.util.misc
import walle.util.workdays
from infra.walle.server.tests.lib.util import (
    monkeypatch_function,
    TestCase,
    monkeypatch_config,
    AUDIT_LOG_ID,
    mock_host_health_status,
    monkeypatch_audit_log,
    mock_response,
    mock_task,
    any_task_status,
    mock_location,
    load_mock_data,
    get_mock_health_data,
)
from sepelib.core import constants
from sepelib.core.constants import HOUR_SECONDS, MINUTE_SECONDS
from sepelib.yandex.startrek import Relationship
from walle import audit_log
from walle.clients import startrek, staff
from walle.clients.startrek import StartrekClientRequestError
from walle.expert import juggler
from walle.expert.types import CheckType, CheckStatus
from walle.failure_reports import reports, base, startrek as startrek_report, project_reports, observers
from walle.hbf_drills import HbfDrillsCollection
from walle.hosts import HostStatus, HostState, HostOperationState, HostMessage, StateExpire
from walle.models import monkeypatch_timestamp, timestamp
from walle.operations_log.constants import Operation
from walle.projects import Project
from walle.stages import Stages
from walle.util import notifications
from walle.util.misc import drop_none


@pytest.fixture
def test(request):
    return TestCase.create(request)


@pytest.fixture
def mock_config(mp):
    monkeypatch_config(mp, "stand.ui_url", "http://localhost")


@pytest.fixture()
def bot_projects_raw_data():
    # Return stored mock data. Actual data can be found at
    # https://bot.yandex-team.ru/api/view.php?name=view_oebs_services&format=json
    return json.loads(load_mock_data("mocks/bot-oebs-projects.json"))


@pytest.fixture
def mock_health_data():
    return get_mock_health_data()


def monkeypatch_health_data(mp, mock_health_data):
    return mp.function(
        juggler._fetch_health_data,
        side_effect=lambda hostnames: [
            juggler.HostHealthStatus(
                hostname,
                [
                    {
                        "type": check["type"],
                        "metadata": check["metadata"],
                        "status": CheckStatus.FAILED,
                        "status_mtime": timestamp() - HOUR_SECONDS,
                        "timestamp": timestamp() - MINUTE_SECONDS,
                    }
                    for check in mock_health_data
                ],
            )
            for hostname in hostnames
        ],
    )


@pytest.fixture()
def monkeypatch_startrek_options(mp):
    monkeypatch_config(mp, "startrek.access_token", "startrek-access-token-mock")


def patch_current_time(mp, **now_time):
    if not now_time:
        # ensure we are always after 9 am by default, so that report's date doesn't slip to yesterday
        now_time = dict(hour=9, minute=0, second=0)

    now = datetime.datetime.now().replace(**now_time)
    monkeypatch_timestamp(mp, int(time.mktime(now.timetuple())))

    # use it for uniformity reasons, so that it compare equal after all conversions and roundings
    return base.now()


@pytest.fixture()
def patch_audit_log(mp):
    patch_current_time(mp)
    monkeypatch_audit_log(mp, time=timestamp(), patch_create=False)


def _mock_problem_host(**kwargs):
    """This host fetched from the hosts collection by report section"""
    defaults = {
        "name": "mock-host",
        "project": "mock-project",
        "status": "mock-status",
        "host_uuid": "mock-host-uuid",
        "report_timestamp": timestamp(),
    }
    defaults.update(kwargs)

    return base.ReportedHost(**defaults)


def _mock_solved_host(**kwargs):
    """This host fetched from the report by report processor"""
    defaults = {
        "inv": 1,
        "solved": True,
        "name": "mock-host",
        "host_uuid": "mock-host-uuid",
        "project": "mock-project",
        "status": "mock-status",
        "section": "ErrorReport",
        "reason": "reason mock",
        "report_timestamp": timestamp(),
        "solve_timestamp": timestamp(),
    }
    defaults.update(kwargs)

    return base.ReportedHost(**defaults)


def _host_to_problem_host(host, reason, section, solved=None):
    return _mock_problem_host(
        inv=host.inv,
        name=host.name,
        host_uuid=host.uuid,
        project=host.project,
        status=host.status,
        section=section,
        reason=reason,
        solved=solved,
    )


def _mock_maintenance_ticket_host(test, **kwargs):
    defaults = {
        "inv": 1,
        "state": HostState.MAINTENANCE,
        "state_time": timestamp() - 9 * constants.WEEK_SECONDS,
        "ticket": "MOCK-1234",
        "state_expire": StateExpire(time=None, ticket="MOCK-1234", status=HostStatus.READY, issuer=TestCase.api_issuer),
    }
    defaults.update(kwargs)

    return test.mock_host(defaults)


def _datetime(delta_days=None):
    # `timestamp` should be monkeypatched
    now = datetime.datetime.fromtimestamp(timestamp())
    if delta_days is not None:
        return now - datetime.timedelta(days=delta_days)
    else:
        return now


def _mock_previous_reports(test, last_offset=0, count=3):
    reports = []
    for index, offset_days in enumerate(reversed(range(0, count)), start=1):
        reports.append(
            test.failure_reports.mock(
                {
                    "report_key": "ticket-key-{}".format(index),
                    "report_date": _datetime(delta_days=offset_days + last_offset),
                    "hosts": [
                        {
                            "inv": 1,
                            "name": "hostname-mock",
                            "status": "status-mock",
                            "project": test.default_project.id,
                            "reason": "reason-mock",
                            "section": "MockReportSection",
                            "report_timestamp": timestamp(),
                        }
                    ],
                }
            )
        )

    return reports


@pytest.mark.usefixtures("mock_config")
class TestReportSection:
    @staticmethod
    def mk_section():
        return base.ReportSection("name", "title")

    def test_section_with_no_hosts_is_empty(self):
        section = self.mk_section()
        assert section.empty()

    def test_section_with_problem_hosts_is_not_empty(self):
        section = self.mk_section()
        section.add_problem_host(_mock_problem_host(inv=1, reason="reason mock"))
        assert not section.empty()

    def test_section_with_solved_hosts_is_not_empty(self):
        section = self.mk_section()
        section.add_solved_host(_mock_problem_host(inv=1, reason="reason mock"))
        assert not section.empty()

    def test_discards_new_hosts_on_merge(self):
        section = self.mk_section()
        section.add_problem_host(_mock_problem_host(inv=1, reason="reason mock"))
        section.add_problem_host(_mock_problem_host(inv=2, reason="reason mock"))

        previous_hosts = {}
        section.merge_hosts(previous_hosts)

        assert section.empty()

    def test_adds_solved_previous_hosts_on_merge(self):
        section = self.mk_section()

        previous_hosts = {
            1: _mock_problem_host(inv=1, solved=True, tickets=["BURNE-1"], section=section.name, reason="reason"),
            2: _mock_problem_host(inv=2, solved=True, tickets=["BURNE-1"], section="OtherSection", reason="reason"),
        }

        section.merge_hosts(previous_hosts)
        assert {1: previous_hosts[1]} == section.solved_hosts


@pytest.mark.usefixtures("mock_config")
class TestReportContents:
    @staticmethod
    def mk_section():
        return base.ReportSection("name", "title")

    def test_report_with_no_sections_is_empty(self):
        content = reports.ReportContents(base.ReportFormatter())

        assert content.empty()

    def test_report_with_non_empty_sections_is_not_empty(self):
        section = self.mk_section()
        section.add_problem_host(_mock_problem_host(inv=1, reason="reason mock"))

        content = reports.ReportContents(base.ReportFormatter())
        content.add_section(section)

        assert not content.empty()

    def test_empty_sections_ignored(self):
        content = reports.ReportContents(base.ReportFormatter())
        content.add_section(self.mk_section())

        assert content.empty()

    def test_merge_host_affects_sections(self):
        section1 = self.mk_section()
        section1.add_problem_host(_mock_problem_host(inv=1, reason="reason mock"))

        section2 = self.mk_section()
        section2.add_problem_host(_mock_problem_host(inv=2, reason="reason mock"))

        content = reports.ReportContents(base.ReportFormatter())
        content.add_section(section1)
        content.add_section(section2)

        # all sections should discard all hosts as "new" because we don't add new hosts to existing reports.
        content.merge_hosts({})
        assert content.empty()

    def test_solved_hosts_does_not_appear_in_hosts_list(self):
        section1 = self.mk_section()
        section2 = self.mk_section()

        section1.name = "SectionOne"
        section2.name = "SectionTwo"

        content = reports.ReportContents(base.ReportFormatter())
        content.add_section(section1)
        content.add_section(section2)

        previous_hosts = {
            1: _mock_solved_host(inv=1, tickets=["BURNE-1"], section=section1.name),
            2: _mock_solved_host(inv=2, tickets=["BURNE-1"], section=section2.name),
        }

        expected_report_hosts = [
            _mock_solved_host(inv=1, tickets=["BURNE-1"], section=section1.name, reason="reason mock"),
            _mock_solved_host(inv=2, tickets=["BURNE-1"], section=section2.name, reason="reason mock"),
        ]
        # all sections should add hosts from previous hosts list to their "solved hosts" lists, so that
        # their content is not empty but there are no hosts in report
        content.merge_hosts(previous_hosts)
        assert content.empty()
        assert content.report_hosts() == expected_report_hosts

    def test_contents_as_text(self):
        class SectionFormatterImpl(base.SectionFormatter):
            def __init__(self, index):
                self.index = index
                super().__init__("title mock")

            def format_section_hosts(self, section):
                return "section {} content".format(self.index)

            def format_title(self):
                return "section {} title".format(self.index)

        class ReportFormatterImpl(base.ReportFormatter):
            def format(self, sections):
                return "{}\n\n*formatted by FanCyFormatter 2.0".format(super().format(sections))

        content = reports.ReportContents(ReportFormatterImpl())
        section1 = base.ReportSection("name mock", SectionFormatterImpl(1))
        section1.add_problem_host(_mock_problem_host())
        content.add_section(section1)

        section2 = base.ReportSection("name mock", SectionFormatterImpl(2))
        section2.add_problem_host(_mock_problem_host())
        content.add_section(section2)

        content.add_section(base.ReportSection("name mock", SectionFormatterImpl(3)))

        assert content.text() == (
            "section 1 title\n\nsection 1 content\n\n"
            "section 2 title\n\nsection 2 content\n\n"
            "*formatted by FanCyFormatter 2.0"
        )


@pytest.mark.usefixtures("disable_caches")
@pytest.mark.online
class TestGroupProjects:
    @staticmethod
    @pytest.fixture(autouse=True)
    def bot_projects(mp, bot_projects_raw_data):
        response = mock_response(bot_projects_raw_data)
        mp.function(walle.clients.utils.request, return_value=response)

    def test_group_by_same_report_params(self):
        project1 = Project(id="1", reports={"queue": "BURNE", "extra": {"component": "project"}, "enabled": True})
        project2 = Project(id="2", reports={"queue": "BURNE", "extra": {"component": "project"}, "enabled": True})
        project3 = Project(id="3", reports={"queue": "BURNE", "extra": {"component": "garbage"}, "enabled": True})

        project_list = [project1, project2, project3]
        random.shuffle(project_list)

        expected_groups = {
            startrek_report.TicketParams(component="project", queue="BURNE"): [project1, project2],
            startrek_report.TicketParams(component="garbage", queue="BURNE"): [project3],
        }

        actual_groups = project_reports._group_projects(project_list, {})
        for group in actual_groups.values():
            group.sort(key=lambda p: p.id)

        assert expected_groups == actual_groups

    def test_group_by_bot_project(self):
        project1 = Project(id="1", reports={"queue": "BURNE", "extra": {"component": "project"}, "enabled": True})
        project2 = Project(id="2", bot_project_id=100000041)
        project3 = Project(id="3", bot_project_id=100000041)
        project4 = Project(id="4", bot_project_id=100000040)

        project_list = [project1, project2, project3, project4]
        random.shuffle(project_list)

        default_report_params = {"queue": "BURNE", "component": "garbage"}
        expected_groups = {
            startrek_report.TicketParams(component="project", queue="BURNE"): [project1],
            startrek_report.TicketParams(component="garbage", queue="BURNE", summary="Search"): [project2, project3],
            startrek_report.TicketParams(component="garbage", queue="BURNE", summary="Monetization"): [project4],
        }

        actual_groups = project_reports._group_projects(project_list, default_report_params)
        for group in actual_groups.values():
            group.sort(key=lambda p: p.id)

        assert expected_groups == actual_groups

    def test_group_by_word(self):
        project1 = Project(id="rtc", reports={"queue": "BURNE", "extra": {"component": "project"}, "enabled": True})
        project2 = Project(id="market")
        project3 = Project(id="yt-banach")
        project4 = Project(id="yt-hahn")

        project_list = [project1, project2, project3, project4]
        random.shuffle(project_list)

        default_report_params = {"queue": "BURNE", "component": "garbage"}
        expected_groups = {
            startrek_report.TicketParams(component="project", queue="BURNE"): [project1],
            startrek_report.TicketParams(component="garbage", queue="BURNE", summary="market"): [project2],
            startrek_report.TicketParams(component="garbage", queue="BURNE", summary="yt"): [project3, project4],
        }

        actual_groups = project_reports._group_projects(project_list, default_report_params)
        for group in actual_groups.values():
            group.sort(key=lambda p: p.id)

        assert expected_groups == actual_groups

    def test_ignores_disabled_projects(self):
        project1 = Project(id="rtc", reports={"enabled": False})

        assert {} == project_reports._group_projects([project1], {})


@pytest.mark.usefixtures("disable_caches")
class TestEnabledProjects:
    @staticmethod
    @pytest.fixture
    def group_projects_mock(mp):
        arguments = {"projects": None, "ticket_params": None}

        def _mock_group_projects(queryset, default_report):
            arguments["projects"] = list(queryset)
            arguments["ticket_params"] = default_report
            return {}

        mp.function(project_reports._group_projects, side_effect=_mock_group_projects)
        return arguments

    def test_explicitly_disabled_projects_excluded(self, test, group_projects_mock):
        other_project = test.mock_project({"id": "project-0"})
        test.mock_project({"id": "project-1", "reports": {"enabled": False, "queue": "BURNE"}})
        enabled_project = test.mock_project(
            {"id": "project-2", "reports": {"queue": "BURNE", "extra": {"component": "project"}, "enabled": True}}
        )

        list(project_reports.ticket_params_for_all_projects())
        group_projects_mock["projects"].sort(key=lambda p: p.id)

        assert group_projects_mock["projects"] == [test.default_project, other_project, enabled_project]


@pytest.mark.online
@pytest.mark.usefixtures("disable_caches")
class TestTicketAssignee:
    @staticmethod
    @pytest.fixture(autouse=True)
    def bot_projects(mp, bot_projects_raw_data):
        response = mock_response(bot_projects_raw_data)
        mp.function(walle.clients.utils.request, return_value=response)

    @staticmethod
    @pytest.fixture
    def mock_staff(mp):
        mp.function(staff.check_logins, side_effect=lambda logins, **kw: sorted(logins))
        mp.function(staff.check_groups, side_effect=lambda groups, **kw: sorted(groups))

    @staticmethod
    @pytest.fixture
    def _mock_fetch_projects(mp):
        return mp.function(project_reports._fetch_projects)

    @pytest.mark.usefixtures("mock_staff")
    def test_assignee_from_report_settings_used_as_is(self, _mock_fetch_projects):
        project = Project(id="project-1", reports={"queue": "RTC", "extra": {"assignee": "lenin"}, "enabled": True})

        _mock_fetch_projects.return_value = [project]
        [(issue_params, projects)] = list(project_reports.ticket_params_for_all_projects())

        assert dict(issue_params) == {"queue": "RTC", "assignee": "lenin"}
        assert projects == [project]

    @pytest.mark.usefixtures("mock_staff")
    def test_assignee_in_custom_queue_not_forced(self, _mock_fetch_projects):
        project = Project(id="project-1", reports={"queue": "RTC", "enabled": True})
        _mock_fetch_projects.return_value = [project]

        [(issue_params, projects)] = list(project_reports.ticket_params_for_all_projects())

        assert dict(issue_params) == {"queue": "RTC"}
        assert projects == [project]

    @pytest.mark.usefixtures("mock_staff")
    def test_projects_with_no_owners_skipped_if_in_default_queue(self, _mock_fetch_projects):
        project = Project(id="project-1", reports={"queue": "BURNE", "enabled": True})
        _mock_fetch_projects.return_value = [project]

        project_groups = list(project_reports.ticket_params_for_all_projects())
        assert project_groups == []

    @pytest.mark.usefixtures("mock_staff")
    @pytest.mark.parametrize("project_settings", [{"queue": "BURNE", "enabled": True}, None])
    def test_uses_owner_as_assignee_in_default_queue(self, _mock_fetch_projects, project_settings):
        project = Project(id="project", owners=["lenin"], reports=project_settings)
        _mock_fetch_projects.return_value = [project]

        [(issue_params, projects)] = list(project_reports.ticket_params_for_all_projects())
        assert issue_params["assignee"] == "lenin"

    @pytest.mark.usefixtures("mock_staff")
    @pytest.mark.parametrize("project_settings", [{"queue": "BURNE", "enabled": True}, None])
    def test_uses_owners_as_followers_in_default_queue(self, _mock_fetch_projects, project_settings):
        project = Project(id="project", owners=["lenin", "stalin"], reports=project_settings)
        _mock_fetch_projects.return_value = [project]

        [(issue_params, projects)] = list(project_reports.ticket_params_for_all_projects())
        assert issue_params["assignee"] == "lenin"
        assert issue_params["followers"] == ["stalin"]

    @pytest.mark.usefixtures("mock_staff")
    @pytest.mark.parametrize("project_settings", [{"queue": "BURNE", "enabled": True}, None])
    def test_resolves_groups_for_assignee_and_followers(self, mp, _mock_fetch_projects, project_settings):
        mp.function(staff.get_group_members, return_value=["lenin", "stalin"])

        project = Project(id="project", owners=["@ussr_leaders"], reports=project_settings)
        _mock_fetch_projects.return_value = [project]

        [(issue_params, projects)] = list(project_reports.ticket_params_for_all_projects())
        assert issue_params["assignee"] == "lenin"
        assert issue_params["followers"] == ["stalin"]

    def test_ignores_invalid_logins_and_groups(self, mp, _mock_fetch_projects):
        mock_staff_client = Mock()
        mock_staff_client.list_groups.return_value = {"result": [{"url": "ussr"}]}
        mock_staff_client.list_persons.return_value = {"result": [{"login": "lenin"}, {"login": "stalin"}]}

        mp.function(staff._get_staff_client, return_value=mock_staff_client)
        mp.function(staff.get_group_members, return_value=["lenin", "stalin"])

        project = Project(id="project", owners=["@ussr", "@usa", "@de", "lenin", "obama", "merkel"])
        _mock_fetch_projects.return_value = [project]

        [(issue_params, projects)] = list(project_reports.ticket_params_for_all_projects())
        assert issue_params["assignee"] == "lenin"
        assert issue_params["followers"] == ["stalin"]


class TestSectionFormatting:
    @staticmethod
    @pytest.fixture(autouse=True)
    def set_config(mp):
        monkeypatch_config(mp, "stand.ui_url", "stand-uri-mock")

    @staticmethod
    @pytest.fixture(params=(base.SectionFormatter, base.GroupingSectionFormatter))
    def formatter_cls(request):
        return request.param

    @staticmethod
    @pytest.fixture()
    def formatter(formatter_cls):
        return formatter_cls("mock section title")

    def test_error_hosts_title_formatting_for_non_prod_stand(self, formatter):
        assert formatter.format_title() == "====Wall-E stand-uri-mock mock section title:"

    def test_error_hosts_title_formatting_for_prod_stand(self, mp, formatter_cls):
        monkeypatch_config(mp, "stand.name", "Production")

        formatter = formatter_cls("mock section title")

        assert formatter.format_title() == "====Wall-E mock section title:"

    def test_error_hosts_failure_reason_formatting(self, formatter):
        assert formatter.host_failure_reason("reason-mock") == '""reason-mock""'

    def test_error_hosts_solved_reason_formatting(self, formatter):
        assert formatter.resolved_failure_reason('""reason-mock""') == '--""reason-mock""--'

    def test_host_line_formatting(self, formatter):
        expected = "  * **((stand-uri-mock/host/uuid-mock hostname-mock)) [status-mock]**: "
        # wtf is it:  ^1 ^2 ^3                                                  ^4
        # 1 - bullet-list
        # 2 - hostname is in bold text
        # 3 - hostname is a url that points to the host in wall-e UI
        # 4 - line also contains host status

        assert formatter.wiki_format_host("uuid-mock", "hostname-mock", "status-mock") == expected

    def test_error_hosts_subtitle_formatting_for_non_prod_stand(self):
        formatter = base.GroupingSectionFormatter("mock section title")

        assert formatter.subtitle("project-mock") == "=====((stand-uri-mock/project/project-mock project-mock))"

    def test_error_hosts_section_formatting(self):
        formatter = base.SectionFormatter("mock section title")
        section = base.ReportSection("mock section name", formatter)

        host_one_status = any_task_status()
        host_two_status = any_task_status()
        section.add_problem_host(
            _mock_problem_host(
                inv=1, name="host-mock-1", status=host_one_status, project="project-mock-1", reason="reason-mock"
            )
        )
        section.add_problem_host(
            _mock_problem_host(
                inv=2, name="host-mock-2", status=host_two_status, project="project-mock-2", reason="reason-mock"
            )
        )

        # these methods have already been tested above
        formatter.wiki_format_host = lambda i, h, s: "  " + h + ":" + s + ": "

        expected = "  host-mock-1:{}: reason-mock\n  host-mock-2:{}: reason-mock".format(
            host_one_status, host_two_status
        )
        assert expected == formatter.format_section_hosts(section)

    def test_error_hosts_section_formatting_with_grouping(self):
        formatter = base.GroupingSectionFormatter("mock section title")
        section = base.ReportSection("mock section name", formatter)

        host_one_status = any_task_status()
        host_two_status = any_task_status()
        section.add_problem_host(
            _mock_problem_host(
                inv=1, name="host-mock-1", status=host_one_status, project="project-mock-1", reason="reason-mock"
            )
        )
        section.add_problem_host(
            _mock_problem_host(
                inv=2, name="host-mock-2", status=host_two_status, project="project-mock-2", reason="reason-mock"
            )
        )

        # these methods have already been tested above
        formatter.subtitle = lambda p: "(subtitle) project: " + p
        formatter.wiki_format_host = lambda i, h, s: "  " + h + ":" + s + ": "

        expected = (
            dedent(
                """
        (subtitle) project: project-mock-1
          host-mock-1:{}: reason-mock
        (subtitle) project: project-mock-2
          host-mock-2:{}: reason-mock
        """
            )
            .strip("\n")
            .format(host_one_status, host_two_status)
        )
        assert expected == formatter.format_section_hosts(section)

    def test_formats_error_hosts(self, mp):
        formatter = base.SectionFormatter("mock section title")

        section = base.ReportSection("section-name", formatter)
        section.add_problem_host(_mock_problem_host(inv=1, reason="reason mock"))

        mp.method(formatter.wiki_format_host, return_value="formatted-error-host-mock: ")

        assert "formatted-error-host-mock: reason mock" == formatter.format_section_hosts(section)

    def test_formats_error_hosts_with_grouping(self, mp):
        formatter = base.GroupingSectionFormatter("mock section title")

        section = base.ReportSection("section-name", formatter)
        section.add_problem_host(_mock_problem_host(inv=1, reason="reason mock"))

        mp.method(formatter.subtitle, return_value="formatted-subtitle-mock: ")
        mp.method(formatter.wiki_format_host, return_value="formatted-error-host-mock: ")

        expected_content = "formatted-subtitle-mock: \nformatted-error-host-mock: reason mock"
        assert expected_content == formatter.format_section_hosts(section)

    def test_formats_solved_hosts(self, mp):
        formatter = base.SectionFormatter("mock section title")

        section = base.ReportSection("section-name", formatter)
        section.add_solved_host(
            _mock_problem_host(
                inv=1,
                name="host-mock",
                status="status-mock",
                project="project-mock",
                section=section.name,
                reason="reason mock",
            )
        )

        mp.method(formatter.wiki_format_host, return_value="formatted-error-host-mock: ")

        assert "formatted-error-host-mock: --reason mock--" == formatter.format_section_hosts(section)

    def test_formats_solved_hosts_with_grouping(self, mp):
        formatter = base.GroupingSectionFormatter("mock section title")

        section = base.ReportSection("section-name", formatter)
        section.add_solved_host(
            _mock_problem_host(
                inv=1,
                name="host-mock",
                status="status-mock",
                project="project-mock",
                section=section.name,
                reason="reason mock",
            )
        )

        mp.method(formatter.subtitle, return_value="formatted-subtitle-mock: ")
        mp.method(formatter.wiki_format_host, return_value="formatted-error-host-mock: ")

        expected_text = "formatted-subtitle-mock: \nformatted-error-host-mock: --reason mock--"
        assert expected_text == formatter.format_section_hosts(section)


@pytest.mark.usefixtures("disable_caches", "mock_config")
class TestErrorHostsReportSection:
    @staticmethod
    def _problem_host(host, tickets=None):
        return _mock_problem_host(
            inv=host.inv,
            name=host.name,
            host_uuid=host.uuid,
            project=host.project,
            status=host.status,
            reason='""{}""'.format(host.task.error),
            section=reports.ErrorHosts.__name__,
            tickets=tickets,
        )

    def test_host_data_to_reported_host_produces_reported_host(self):
        data_collector = reports.ErrorHosts()
        host_data = {"name": "mock-host", "_id": "mock-host-uuid", "project": "mock-project", "status": "mock-status"}
        reported_host = data_collector.init_reported_host(1, host_data, reason="reason mock")

        expected_reported_host = _mock_problem_host(inv=1, reason="reason mock")
        assert expected_reported_host == reported_host

    def test_fetches_error_hosts(self, test):
        failed_host = test.mock_host({"inv": 1, "status": any_task_status()}, task_kwargs={"error": "error-mock"})
        test.mock_host({"inv": 2, "health": mock_host_health_status(check_status=CheckStatus.PASSED)})

        expected = {failed_host.inv: self._problem_host(failed_host)}

        data_collector = reports.ErrorHosts()
        section = data_collector.gather_data([test.default_project])

        assert expected == section.problem_hosts

    def test_fetches_stage_tickets(self, test):
        failed_host = test.mock_host(
            {"inv": 1, "status": Operation.POWER_OFF.host_status},
            task_kwargs={"error": "error-mock", "stage": Stages.POWER_OFF, "stage_data": {"tickets": ["ITDC-100001"]}},
        )

        expected = {failed_host.inv: self._problem_host(failed_host, tickets=["ITDC-100001"])}

        data_collector = reports.ErrorHosts()
        section = data_collector.gather_data([test.default_project])

        assert expected == section.problem_hosts

    def test_error_hosts_reason(self, test, mp):
        failed_host = test.mock_host(
            {"inv": 1, "status": any_task_status()}, task_kwargs={"error": "task error mock"}, save=False
        )

        fields = ("inv", "name", "uuid", "project", "status", "task.error")
        found_hosts = [dict(failed_host.to_mongo(fields=fields))]

        mp.function(reports.ErrorHosts.find_hosts, module=reports.ErrorHosts, return_value=found_hosts)

        data_collector = reports.ErrorHosts()
        section = data_collector.gather_data([test.default_project])

        for host in section.problem_hosts.values():
            assert host["reason"] == '""task error mock""'


@pytest.mark.usefixtures("disable_caches", "mock_config")
class TestUnreachableHostsReportSection:
    fetched_fields = ("uuid", "inv", "name", "project", "status", "health.reasons")

    def test_error_hosts_failure_reason_formatting(self):
        data_collector = reports.UnreachableHosts()

        actual_reason = data_collector.host_failure_reason(["unreachable", "ssh"], base.SectionFormatter)
        assert '""The host is unreachable: unreachable, ssh.""' == actual_reason

    def test_fetches_unreachable_hosts(self, test):
        failed_host = test.mock_host({"inv": 1, "health": mock_host_health_status(check_status=CheckStatus.FAILED)})
        test.mock_host({"inv": 2, "health": mock_host_health_status(check_status=CheckStatus.PASSED)})

        expected = [dict(failed_host.to_mongo(fields=self.fetched_fields))]

        data_collector = reports.UnreachableHosts()
        hosts = list(data_collector.find_hosts(projects=[test.default_project]))

        assert hosts == expected

    def test_unreachable_hosts_reason(self, test, mp):
        failed_host = test.mock_host(
            {"inv": 1, "health": mock_host_health_status(check_status=CheckStatus.FAILED)}, save=False
        )

        found_hosts = [dict(failed_host.to_mongo(fields=self.fetched_fields))]

        mp.function(reports.UnreachableHosts.find_hosts, module=reports.UnreachableHosts, return_value=found_hosts)

        data_collector = reports.UnreachableHosts()
        section = data_collector.gather_data([test.default_project])

        for host in section.problem_hosts.values():
            assert host["reason"].startswith('""The host is unreachable: ')


@pytest.mark.usefixtures("disable_caches", "mock_config")
class TestBrokenDNSHostsReportSection:
    fetched_fields = ("uuid", "inv", "name", "project", "status", "messages")

    def test_error_hosts_failure_reason_formatting(self):
        data_collector = reports.BrokenDnsHosts()

        actual_reason = data_collector.host_failure_reason(["ips mismatch"], base.SectionFormatter)
        assert '""The host has DNS problems: ips mismatch.""' == actual_reason

    def test_fetches_hosts_with_broken_dns(self, test):
        failed_host = test.mock_host({"inv": 1, "messages": {"dns_fixer": [HostMessage.error("ips mismatch")]}})
        test.mock_host({"inv": 2, "messages": {}})

        expected = [dict(failed_host.to_mongo(fields=self.fetched_fields))]

        data_collector = reports.BrokenDnsHosts()
        hosts = list(data_collector.find_hosts(projects=[test.default_project]))

        assert hosts == expected

    def test_broken_dns_hosts_reason(self, test, mp):
        failed_host = test.mock_host(
            {"inv": 1, "messages": {"dns_fixer": [{"message": "ips mismatch", "severity": "error"}]}}, save=False
        )

        found_hosts = [dict(failed_host.to_mongo(fields=self.fetched_fields))]

        mp.function(reports.BrokenDnsHosts.find_hosts, module=reports.BrokenDnsHosts, return_value=found_hosts)

        data_collector = reports.BrokenDnsHosts()
        section = data_collector.gather_data([test.default_project])

        for host in section.problem_hosts.values():
            assert host["reason"].startswith('""The host has DNS problems: ')


@pytest.mark.usefixtures("disable_caches", "mock_config")
class TestDeadHostsReportSection:
    fetched_fields = ("uuid", "inv", "name", "project", "status", "status_audit_log_id")

    def test_fetches_dead_and_invalid_hosts(self, test):
        dead_host = test.mock_host({"inv": 1, "status": HostStatus.DEAD})
        invalid_host = test.mock_host({"inv": 2, "status": HostStatus.INVALID})
        test.mock_host({"inv": 3, "status": HostStatus.READY})

        expected_hosts = [dict(host.to_mongo(fields=self.fetched_fields)) for host in (dead_host, invalid_host)]

        dead_collector = reports.DeadHosts()
        invalid_collector = reports.InvalidHosts()
        hosts = list(
            dead_collector.find_hosts(projects=[test.default_project])
            + invalid_collector.find_hosts(projects=[test.default_project])
        )

        assert hosts == expected_hosts

    def test_dont_fetch_invalid_hosts_in_maintenance_with_decommission(self, test):
        host_in_maintenance_operation = test.mock_host(
            {
                "inv": 1,
                "status": HostStatus.INVALID,
                "state": HostState.MAINTENANCE,
                "operation_state": HostOperationState.OPERATION,
            }
        )

        test.mock_host(
            {
                "inv": 2,
                "status": HostStatus.INVALID,
                "state": HostState.MAINTENANCE,
                "operation_state": HostOperationState.DECOMMISSIONED,
            }
        )

        host_in_assigned_decommisioned = test.mock_host(
            {
                "inv": 3,
                "status": HostStatus.INVALID,
                "state": HostState.ASSIGNED,
                "operation_state": HostOperationState.DECOMMISSIONED,
            }
        )

        expected_hosts = [
            dict(host.to_mongo(fields=self.fetched_fields))
            for host in (host_in_maintenance_operation, host_in_assigned_decommisioned)
        ]

        invalid_collector = reports.InvalidHosts()
        hosts = list(invalid_collector.find_hosts(projects=[test.default_project]))

        assert hosts == expected_hosts

    def test_fetches_dead_hosts_without_status_reason(self, test):
        dead_host = test.mock_host({"inv": 1, "status": HostStatus.DEAD})
        del dead_host.status_reason
        dead_host.save()
        test.mock_host({"inv": 3, "status": HostStatus.READY})

        expected_hosts = [dead_host.to_mongo(fields=self.fetched_fields)]

        dead_collector = reports.DeadHosts()
        hosts = list(dead_collector.find_hosts(projects=[test.default_project]))

        assert hosts == expected_hosts

    @pytest.mark.usefixtures("patch_audit_log")
    def test_failed_hosts_reason(self, test, mp):
        failed_host = test.mock_host(
            {"inv": 1, "status": HostStatus.DEAD, "status_audit_log_id": AUDIT_LOG_ID}, save=False
        )

        audit_log.on_deactivate_host(
            issuer="wall-e-unittest@",
            project_id=test.default_project.id,
            inv=failed_host.inv,
            name=failed_host.name,
            host_uuid=failed_host.uuid,
            reason="deactivate reason mock",
        )

        found_hosts = [dict(failed_host.to_mongo(fields=self.fetched_fields))]
        mp.function(reports.DeadHosts.find_hosts, module=reports.DeadHosts, return_value=found_hosts)

        data_collector = reports.DeadHosts()
        section = data_collector.gather_data([test.default_project])

        for host in section.problem_hosts.values():
            assert host["reason"].endswith('deactivate reason mock""')


@pytest.mark.usefixtures("disable_caches", "mock_config", "monkeypatch_timestamp")
class TestInfrastructureReportSection:
    def test_error_hosts_failure_reason_formatting(self):
        data_collector = reports.InfrastructureProblems()

        expected = '!!(red)**""rack failed""**!!, !!(red)**""switch failed""**!!, ""netmon failed""'

        failure_reasons = ["switch failed", "netmon failed", "rack failed"]
        actual = data_collector.host_failure_reason(failure_reasons, formatter=base.SectionFormatter)

        assert actual == expected

    def test_fetches_hosts_with_infrastructure_failure(self, test, mp, mock_health_data):
        monkeypatch_health_data(mp, mock_health_data)

        test.mock_host(
            {
                "inv": 1,
                "health": mock_host_health_status(check_status=CheckStatus.PASSED),
                "location": mock_location(short_datacenter_name="man"),
            }
        )

        failed_hosts = self._gen_failed_hosts(test)

        data_collector = reports.InfrastructureProblems()
        section = data_collector.gather_data(projects=[test.default_project])

        expected = {
            h.inv: _host_to_problem_host(
                h,
                section=section.name,
                reason=data_collector.host_failure_reason(h.health.reasons, base.SectionFormatter),
            )
            for h in failed_hosts
        }
        assert expected == section.problem_hosts

    def test_excludes_hosts_in_hbf_drill(self, test, mp, mock_health_data):
        monkeypatch_health_data(mp, mock_health_data)

        test.mock_host(
            {
                "inv": 1,
                "health": mock_host_health_status(check_status=CheckStatus.PASSED),
                "location": mock_location(short_datacenter_name="man"),
            }
        )

        self._gen_failed_hosts(test)
        mp.method(HbfDrillsCollection.get_host_inclusion_reason, return_value="mock-reason", obj=HbfDrillsCollection)

        data_collector = reports.InfrastructureProblems()
        section = data_collector.gather_data(projects=[test.default_project])

        assert section.problem_hosts == {}

    def _gen_failed_hosts(self, test):
        failed_hosts = []
        host_inv = 2
        for check in CheckType.ALL_INFRASTRUCTURE:
            for status in (CheckStatus.FAILED, CheckStatus.SUSPECTED):
                # only report failures, for netmon checks - only switch failures
                if check == CheckType.WALLE_RACK and status == CheckStatus.FAILED:
                    failed_hosts.append(
                        test.mock_host(
                            {
                                "inv": host_inv,
                                "health": mock_host_health_status(
                                    check_status=CheckStatus.FAILED,
                                    check_overrides=[
                                        {
                                            "type": check,
                                            "status": status,
                                            "stale_timestamp": timestamp() + 100,
                                        }
                                    ],
                                ),
                                "location": mock_location(short_datacenter_name="man"),
                            }
                        )
                    )
                host_inv += 1
        return failed_hosts


@pytest.mark.usefixtures("disable_caches", "mock_config")
class TestTopProblematicHostsReportSection:
    def test_error_hosts_failure_reason_formatting(self):
        report = reports.TopProblematicHosts()

        actions_counter = (("reboot", 10), ("redeploy", 5))
        actual = report.host_failure_reason(actions_counter, base.SectionFormatter)
        assert '""reboot: 10 times, redeploy: 5 times""' == actual

    def test_filters_top_failure_hosts_hosts(self, test):
        host_1 = test.mock_host({"inv": 1})
        host_2 = test.mock_host({"inv": 2, "status": any_task_status()})
        host_3 = test.mock_host({"inv": 3, "status": any_task_status()}, task_kwargs={"owner": "user@"})

        for host in [host_1, host_2, host_3]:
            for x in range(4):
                log_id = str(host.inv * 10 + x)
                test.audit_log.mock(
                    {
                        "id": log_id,
                        "host_inv": host.inv,
                        "type": audit_log.TYPE_REBOOT_HOST,
                        "time": walle.util.workdays.to_timestamp(
                            datetime.datetime.now().replace(hour=5, minute=10 + 2 * x)
                        ),
                    }
                )

        data_collector = reports.TopProblematicHosts()
        section = data_collector.gather_data(projects=[test.default_project])

        actions = [[audit_log.TYPE_REBOOT_HOST, 4]]
        expected = {
            host_1.inv: _host_to_problem_host(
                host_1,
                section=section.name,
                reason=data_collector.host_failure_reason(actions, base.SectionFormatter),
            ),
            host_2.inv: _host_to_problem_host(
                host_2,
                section=section.name,
                reason=data_collector.host_failure_reason(actions, base.SectionFormatter),
            ),
        }

        assert section.problem_hosts == expected


class TestReportModel:
    def test_create_report(self, test, mp):
        monkeypatch_timestamp(mp)
        report = test.failure_reports.mock(save=False)

        base.ErrorReportModel.create(
            report_key=report.report_key,
            stream_key=report.stream_key,
            report_date=report.report_date,
            hosts=report.hosts,
        )

        test.failure_reports.assert_equal()

    def test_update_report_bumps_timestamp(self, test, mp):
        report = test.failure_reports.mock()

        monkeypatch_timestamp(mp, int(time.time() + 1))
        report.last_update_time = timestamp()

        base.ErrorReportModel.update(report.report_key)

        test.failure_reports.assert_equal()

    def test_update_applies_kwargs(self, test, mp):
        monkeypatch_timestamp(mp)
        report = test.failure_reports.mock()

        report.hosts.append({"inv": 2})

        base.ErrorReportModel.update(report.report_key, add_to_set__hosts={"inv": 2})

        test.failure_reports.assert_equal()

    def test_close_report_bumps_timestamp_and_closes(self, test, mp):
        report = test.failure_reports.mock()

        monkeypatch_timestamp(mp, int(time.time() + 1))
        report.last_update_time = timestamp()
        report.closed = True

        base.ErrorReportModel.close_report(report.report_key)
        test.failure_reports.assert_equal()

    def test_find_returns_only_opened_reports(self, test):
        stream_key = "mock-stream-key"
        report1 = test.failure_reports.mock({"report_key": "BURNE-1001", "stream_key": stream_key})
        test.failure_reports.mock({"report_key": "BURNE-1002", "stream_key": stream_key, "closed": True})

        assert [json.loads(report.to_json()) for report in base.ErrorReportModel.find(stream_key)] == [
            json.loads(report1.to_json())
        ]

    def test_find_oldest_report_first(self, test):
        stream_key = "mock-stream-key"
        test.failure_reports.mock({"report_key": "BURNE-1000", "stream_key": stream_key, "closed": True})

        d = datetime.date.today() - datetime.timedelta(days=2)
        # Try to mess with order of documents in collection.
        report2 = test.failure_reports.mock(
            {
                "report_key": "BURNE-1002",
                "stream_key": stream_key,
                "report_date": d + datetime.timedelta(days=2),
                "create_time": int(time.time() + 3),
            }
        )
        report3 = test.failure_reports.mock(
            {
                "report_key": "BURNE-1003",
                "stream_key": stream_key,
                "report_date": d + datetime.timedelta(days=3),
                "create_time": int(time.time() + 1),
            }
        )
        report1 = test.failure_reports.mock(
            {
                "report_key": "BURNE-1001",
                "stream_key": stream_key,
                "report_date": d + datetime.timedelta(days=1),
                "create_time": int(time.time() + 2),
            }
        )

        expected_reports = [json.loads(report.to_json()) for report in (report1, report2, report3)]
        assert [json.loads(report.to_json()) for report in base.ErrorReportModel.find(stream_key)] == expected_reports


class RotationStrategyTestCase:
    @staticmethod
    def get_rotation_strategy(*args, **kwargs):
        # type: (*args, **kwargs) -> base.RotationStrategy(*args, **kwargs)
        raise NotImplementedError

    def test_can_be_found_by_name(self):
        original_rotation_strategy = self.get_rotation_strategy()
        restored_rotation_strategy = base.RotationStrategy.by_name(original_rotation_strategy.name)

        assert type(restored_rotation_strategy) == type(original_rotation_strategy)


@pytest.mark.usefixtures("mock_config", "test")
class TestDailyReportRotationStrategy(RotationStrategyTestCase):
    @staticmethod
    def get_rotation_strategy(*args, **kwargs):
        return base.DailyReportRotationStrategy()

    def test_uses_existing_yesterday_ticket_before_nine_am(self, test, mp):
        # no ticket for today's date, but there is a ticket for yesterday, and it should be used.
        patch_current_time(mp, hour=8, minute=59, second=59).date()
        previous_reports = _mock_previous_reports(test, last_offset=1)

        rotation_strategy = self.get_rotation_strategy()
        today_report, previous_report, old_reports = rotation_strategy.find_reports("mock-stream-key")

        assert today_report == previous_reports[-1]
        assert previous_report == previous_reports[-2]
        assert old_reports == previous_reports[:-1]

    def test_uses_existing_today_ticket_before_nine_am(self, test, mp):
        # got both ticket for today's date and for "relative today"
        # NB: this is an error case, two one of the tickets should have been created by mistake.
        # but it wouldn't be an error if there were no previous tickets:
        # today's ticket may have been created as a normal report.

        patch_current_time(mp, hour=8, minute=59, second=59).date()
        previous_reports = _mock_previous_reports(test)

        rotation_strategy = self.get_rotation_strategy()
        today_report, previous_report, old_reports = rotation_strategy.find_reports("mock-stream-key")

        assert today_report == previous_reports[-1]
        assert previous_report == previous_reports[-2]
        assert old_reports == previous_reports[:-1]

    def test_does_not_use_existing_ticket_after_nine_am(self, test, mp):
        # no ticket for today's date, new ticket should be created
        patch_current_time(mp, hour=9, minute=0, second=0)
        previous_reports = _mock_previous_reports(test, last_offset=1)

        rotation_strategy = self.get_rotation_strategy()
        today_report, previous_report, old_reports = rotation_strategy.find_reports("mock-stream-key")

        assert today_report is None
        assert previous_report == previous_reports[-1]
        assert old_reports == previous_reports

    @pytest.mark.parametrize(
        "now_time",
        [
            dict(hour=9, minute=0, second=0),
            dict(hour=9, minute=1, second=0),
            dict(hour=9, minute=0, second=1),
        ],
    )
    def test_uses_existing_today_ticket_if_exists(self, test, mp, now_time):
        patch_current_time(mp, **now_time)
        previous_reports = _mock_previous_reports(test)

        rotation_strategy = self.get_rotation_strategy()
        today_report, previous_report, old_reports = rotation_strategy.find_reports("mock-stream-key")

        assert today_report == previous_reports[-1]
        assert previous_report == previous_reports[-2]
        assert old_reports == previous_reports[:-1]


@pytest.mark.usefixtures("mock_config", "test")
class TestHostFailureRotationStrategy(RotationStrategyTestCase):
    @staticmethod
    def get_rotation_strategy(rotate=False, *args, **kwargs):
        return base.HostFailureRotationStrategy(rotate=rotate)

    def test_finds_report_in_stream(self, test):
        report = test.failure_reports.mock(
            {
                "report_key": "BURNE-1001",
            }
        )
        rotation_strategy = self.get_rotation_strategy(rotate=False)
        found_reports = rotation_strategy.find_reports("mock-stream-key")

        assert (report, None, []) == found_reports

    def test_finds_and_closes_duplicate_reports(self, test):
        previous_report = test.failure_reports.mock(
            {
                "report_key": "BURNE-1001",
                "create_time": time.time() - 1,
            }
        )

        current_report = test.failure_reports.mock(
            {
                "report_key": "BURNE-1002",
            }
        )

        rotation_strategy = self.get_rotation_strategy(rotate=False)
        found_reports = rotation_strategy.find_reports("mock-stream-key")

        assert (current_report, previous_report, [previous_report]) == found_reports

    def test_finds_and_rotates_reports_without_failed_hosts(self, test):
        test.mock_host({"inv": 1})
        report = test.failure_reports.mock(
            {
                "report_key": "BURNE-1001",
            }
        )

        rotation_strategy = self.get_rotation_strategy(rotate=True)
        found_reports = rotation_strategy.find_reports("mock-stream-key")

        assert (None, report, [report]) == found_reports

    def test_finds_and_keeps_reports_with_failed_hosts(self, test):
        test.mock_host({"inv": 1, "task": mock_task()})
        report = test.failure_reports.mock(
            {
                "report_key": "BURNE-1001",
            }
        )

        rotation_strategy = self.get_rotation_strategy(rotate=True)
        found_reports = rotation_strategy.find_reports("mock-stream-key")

        assert (report, None, []) == found_reports

    def test_finds_and_rotates_duplicates_even_with_failed_hosts(self, test):
        test.mock_host({"inv": 1, "task": mock_task()})
        prev_report = test.failure_reports.mock(
            {
                "report_key": "BURNE-1001",
                "create_time": time.time() - 1,
            }
        )
        curr_report = test.failure_reports.mock(
            {
                "report_key": "BURNE-1002",
            }
        )

        rotation_strategy = self.get_rotation_strategy(rotate=True)
        found_reports = rotation_strategy.find_reports("mock-stream-key")

        assert (curr_report, prev_report, [prev_report]) == found_reports


@pytest.mark.usefixtures("mock_config", "test")
class TestErrorHostsReport:
    @staticmethod
    def _mock_report_content(hosts=None):
        class MockSectionFormatter(base.SectionFormatter):
            def format_title(self):
                return self.title

            def format_section_hosts(self, section):
                return "section body"

        section = base.ReportSection("MockReportSection", MockSectionFormatter("section title"))

        if hosts is None:
            section.add_problem_host(
                _mock_problem_host(
                    inv=1, name="host-mock", status="status-mock", project="project-mock", reason="reason mock"
                ),
            )
        else:
            for host in hosts:
                section.add_problem_host(_mock_problem_host(reason="reason mock", **host))

        content = reports.ReportContents(base.ReportFormatter())
        content.add_section(section)
        return content

    @staticmethod
    def _mock_report_publisher(stream_key, new_report_key=None, side_effect=None):
        class MockReportPublisher(base.ReportPublisher):
            def get_stream_key(self):
                return stream_key

            close_old_report = Mock(side_effect=side_effect)
            create_new_report = Mock(return_value=new_report_key, side_effect=side_effect)
            update_existing_report = Mock(side_effect=side_effect)
            verify_report_published = Mock(side_effect=side_effect)
            from_stream_key = Mock(side_effect=side_effect)

        return MockReportPublisher()

    @staticmethod
    def _mock_rotation_strategy(today_report=None, previous_report=None, old_reports=None):
        class MockRotationStrategy(base.RotationStrategy):
            name = "mock"

            def find_reports(self, stream_key):
                return base.ExistingReports(today_report, previous_report, old_reports or [])

        return MockRotationStrategy()

    @staticmethod
    def _mock_report_observer(side_effect=None):
        class MockObserver(base.ReportObserver):
            report_created = Mock(side_effect=side_effect)
            report_failed_to_create = Mock(side_effect=side_effect)
            report_updated = Mock(side_effect=side_effect)
            report_closed = Mock(side_effect=side_effect)

        return MockObserver()

    @pytest.mark.parametrize("is_new_report", [True, False])
    def test_does_nothing_when_description_not_set(self, test, is_new_report):
        _mock_previous_reports(test, last_offset=int(is_new_report))
        mock_publisher = self._mock_report_publisher(stream_key="mock-stream-key", side_effect=RuntimeError)
        report_observer = self._mock_report_observer(side_effect=RuntimeError)

        with base.ErrorHostsReport("mock-stream-key", mock_publisher, self._mock_rotation_strategy(), report_observer):
            pass

        assert not mock_publisher.create_new_report.called

    def test_updates_report_hosts_when_description_set(self, test, mp):
        patch_current_time(mp)

        _mock_previous_reports(test, count=1)
        today_report = test.failure_reports.objects[-1]  # the last one should be the most recent

        rotation_strategy = self._mock_rotation_strategy(today_report)
        mock_publisher = self._mock_report_publisher(stream_key="mock-stream-key")
        report_observer = self._mock_report_observer()

        report_content = self._mock_report_content(
            [
                {
                    "inv": 1,
                    "name": "hostname-mock",
                    "status": "status-mock",
                    "project": "project-mock",
                    "tickets": ["BURNE-1001"],
                }
            ]
        )

        with base.ErrorHostsReport("mock-stream-key", mock_publisher, rotation_strategy, report_observer) as report:
            report.set_content("dummy summary", report_content)

        expected_report_hosts = [
            base.ReportedHost(
                inv=1,
                tickets=["BURNE-1001"],
                reason="reason mock",
                section="MockReportSection",
                name="hostname-mock",
                status="status-mock",
                project="project-mock",
                report_timestamp=timestamp(),
            )
        ]

        mock_publisher.update_existing_report.assert_called_once_with(
            today_report, report_content.text(), expected_report_hosts
        )

        assert not report_observer.report_created.called
        assert not report_observer.report_closed.called
        report_observer.report_updated.assert_called_once_with(
            today_report.report_key, report_hosts=expected_report_hosts
        )

    def test_closes_report_when_content_becomes_empty(self, test, mp):
        patch_current_time(mp)

        _mock_previous_reports(test, count=1)
        today_report = test.failure_reports.objects[-1]  # the last one should be the most recent

        rotation_strategy = self._mock_rotation_strategy(today_report)
        mock_publisher = self._mock_report_publisher(stream_key="mock-stream-key")
        report_observer = self._mock_report_observer()

        report_content = self._mock_report_content([])

        with base.ErrorHostsReport("mock-stream-key", mock_publisher, rotation_strategy, report_observer) as report:
            report.set_content("dummy summary", report_content)

        expected_report_hosts = [
            base.ReportedHost(
                inv=1,
                solved=True,
                reason="reason-mock",
                section="MockReportSection",
                name="hostname-mock",
                status="status-mock",
                project=test.default_project.id,
                report_timestamp=timestamp(),
                solve_timestamp=timestamp(),
            )
        ]

        mock_publisher.update_existing_report.assert_called_once_with(
            today_report, report_content.text(), report_content.report_hosts()
        )
        mock_publisher.close_old_report.assert_called_once_with(today_report.report_key)

        assert not report_observer.report_created.called
        assert not report_observer.report_updated.called
        report_observer.report_closed.assert_called_once_with(
            today_report.report_key, report_hosts=expected_report_hosts
        )

    def test_merges_hosts_from_previous_run_with_current_list(self, test, mp):
        """Report section find new error hosts, but they should be discarded because
        we do not add hosts to report on updates. Report should be closed."""
        patch_current_time(mp)

        # previous report contain a host with inv=1
        _mock_previous_reports(test, count=1)
        today_report = test.failure_reports.objects[-1]  # the last one should be the most recent

        rotation_strategy = self._mock_rotation_strategy(today_report)
        mock_publisher = self._mock_report_publisher(stream_key="mock-stream-key")
        report_observer = self._mock_report_observer()

        report_content = self._mock_report_content(
            [
                {
                    "inv": 2,
                    "name": "hostname-mock",
                    "status": "status-mock",
                    "project": "project-mock",
                    "tickets": ["BURNE-1001"],
                }
            ]
        )

        with base.ErrorHostsReport("mock-stream-key", mock_publisher, rotation_strategy, report_observer) as report:
            report.set_content("dummy summary", report_content)

        # this host comes from default mock args from TestCase
        expected_report_hosts = [
            base.ReportedHost(
                inv=1,
                solved=True,
                reason="reason-mock",
                section="MockReportSection",
                name="hostname-mock",
                status="status-mock",
                project=test.default_project.id,
                solve_timestamp=timestamp(),
                report_timestamp=timestamp(),
            )
        ]
        mock_publisher.update_existing_report.assert_called_once_with(
            today_report, report_content.text(), report_content.report_hosts()
        )
        mock_publisher.close_old_report.assert_called_once_with(today_report.report_key)

        assert not report_observer.report_created.called
        assert not report_observer.report_updated.called
        report_observer.report_closed.assert_called_once_with(
            today_report.report_key, report_hosts=expected_report_hosts
        )

    @pytest.mark.usefixtures("monkeypatch_audit_log")
    @pytest.mark.parametrize("has_previous_report", [True, False])
    def test_creates_new_report_when_description_set(self, test, mp, has_previous_report):
        report_key = "new-report-key"
        patch_current_time(mp)

        if has_previous_report:
            _mock_previous_reports(test, last_offset=1, count=1)
            previous_report = test.failure_reports.objects[-1]  # the last one should be the most recent
        else:
            previous_report = None

        report_content = self._mock_report_content(
            hosts=[
                {
                    "inv": 1,
                    "name": "hostname-mock",
                    "status": "status-mock",
                    "project": "project-mock",
                    "tickets": ["BURNE-1001"],
                }
            ]
        )
        rotation_strategy = self._mock_rotation_strategy(previous_report=previous_report)
        mock_publisher = self._mock_report_publisher(stream_key="mock-stream-key", new_report_key=report_key)
        report_observer = self._mock_report_observer()

        summary = "dummy summary"

        with base.ErrorHostsReport("mock-stream-key", mock_publisher, rotation_strategy, report_observer) as report:
            report.set_content(summary, report_content)

        expected_report_hosts = [
            base.ReportedHost(
                inv=1,
                name="hostname-mock",
                host_uuid="mock-host-uuid",
                tickets=["BURNE-1001"],
                reason="reason mock",
                section="MockReportSection",
                status="status-mock",
                project="project-mock",
                audit_log_id=AUDIT_LOG_ID,
                report_timestamp=timestamp(),
            )
        ]

        # creates audit lof entry for every host in report
        report_hosts = report_content.report_hosts()
        for host in report_hosts:
            host["audit_log_id"] = AUDIT_LOG_ID

        mock_publisher.create_new_report.assert_called_once_with(
            summary, report_content.text(), expected_report_hosts, previous_report
        )

        assert not report_observer.report_updated.called
        assert not report_observer.report_closed.called
        report_observer.report_created.assert_called_once_with(
            report_key,
            report_hosts=expected_report_hosts,
            previous_report_key=previous_report.report_key if previous_report else None,
        )

    def test_notifies_observers_when_report_creation_failed(self, test):
        rotation_strategy = self._mock_rotation_strategy()
        mock_publisher = self._mock_report_publisher(
            stream_key="mock-stream-key", side_effect=base.ReportPublisherFailure("publisher error mock")
        )
        report_observer = self._mock_report_observer()

        with pytest.raises(base.ReportPublisherFailure):
            with base.ErrorHostsReport("mock-stream-key", mock_publisher, rotation_strategy, report_observer) as report:
                report.set_content("dummy summary", self._mock_report_content())

        expected_report_hosts = [
            base.ReportedHost(
                inv=1,
                name="host-mock",
                host_uuid="mock-host-uuid",
                section="MockReportSection",
                status="status-mock",
                project="project-mock",
                reason="reason mock",
                report_timestamp=timestamp(),
            )
        ]

        report_observer.report_failed_to_create.assert_called_once_with(
            "publisher error mock", report_hosts=expected_report_hosts
        )

    def test_silences_publisher_error_if_silenced(self, test):
        rotation_strategy = self._mock_rotation_strategy()
        mock_publisher = self._mock_report_publisher(
            stream_key="mock-stream-key", side_effect=base.ReportPublisherFailure("publisher error mock")
        )
        report_observer = self._mock_report_observer()

        hosts_report = base.ErrorHostsReport(
            "mock-stream-key", mock_publisher, rotation_strategy, report_observer, raise_on_failure=False
        )
        with hosts_report as report:
            report.set_content("dummy summary", self._mock_report_content())

        expected_report_hosts = [
            base.ReportedHost(
                inv=1,
                name="host-mock",
                host_uuid="mock-host-uuid",
                section="MockReportSection",
                status="status-mock",
                project="project-mock",
                reason="reason mock",
                report_timestamp=timestamp(),
            )
        ]

        report_observer.report_failed_to_create.assert_called_once_with(
            "publisher error mock", report_hosts=expected_report_hosts
        )

    @pytest.mark.parametrize("has_previous_report", [True, False])
    def test_does_not_create_new_when_description_is_empty(self, test, mp, has_previous_report):
        patch_current_time(mp)

        if has_previous_report:
            _mock_previous_reports(test, last_offset=1, count=1)
            previous_report = test.failure_reports.objects[-1]  # the last one should be the most recent
        else:
            previous_report = None

        report_content = self._mock_report_content(hosts=[])
        rotation_strategy = self._mock_rotation_strategy(previous_report=previous_report)
        mock_publisher = self._mock_report_publisher(stream_key="mock-stream-key", new_report_key="new-report-key")
        report_observer = self._mock_report_observer()

        with base.ErrorHostsReport("mock-stream-key", mock_publisher, rotation_strategy, report_observer) as report:
            report.set_content("dummy summary", report_content)

        assert not mock_publisher.create_new_report.called

        assert not report_observer.report_created.called
        assert not report_observer.report_updated.called
        assert not report_observer.report_closed.called

    @pytest.mark.parametrize("is_new_report", [True, False])
    def test_closes_old_reports(self, test, mp, is_new_report):
        # NB: this exact behaviour is a garbage collector logic.
        # Don't drop it without corresponding changes to garbage collector.
        patch_current_time(mp)

        if is_new_report:
            # no today report
            previous_reports = _mock_previous_reports(test, last_offset=1)
            last_report_key = previous_reports.pop()
        else:
            previous_reports = _mock_previous_reports(test)
            last_report_key = None

        mock_publisher = self._mock_report_publisher(stream_key="mock-stream-key")
        rotation_strategy = self._mock_rotation_strategy(today_report=last_report_key, old_reports=previous_reports)
        report_observer = self._mock_report_observer()

        with base.ErrorHostsReport("mock-stream-key", mock_publisher, rotation_strategy, report_observer):
            pass

        assert mock_publisher.close_old_report.mock_calls == [call(report.report_key) for report in previous_reports]

        assert not report_observer.report_created.called
        assert not report_observer.report_updated.called

        expected_calls = [call(report.report_key, report_hosts=report.hosts) for report in previous_reports]
        assert expected_calls == report_observer.report_closed.mock_calls

    def test_creates_new_report_if_old_report_is_not_published(self, test, mp):
        patch_current_time(mp)

        _mock_previous_reports(test, count=1)
        today_report = test.failure_reports.objects[-1]  # the last one should be the most recent

        new_report_key = "new-report-key"
        rotation_strategy = self._mock_rotation_strategy(today_report)
        mock_publisher = self._mock_report_publisher(stream_key="mock-stream-key", new_report_key=new_report_key)
        mock_publisher.verify_report_published.return_value = False
        report_observer = self._mock_report_observer()

        report_content = self._mock_report_content(
            [
                {
                    "inv": 1,
                    "name": "hostname-mock",
                    "status": "status-mock",
                    "project": "project-mock",
                    "tickets": ["BURNE-1001"],
                }
            ]
        )

        summary = "dummy summary"
        with base.ErrorHostsReport("mock-stream-key", mock_publisher, rotation_strategy, report_observer) as report:
            report.set_content(summary, report_content)

        expected_report_hosts = [
            base.ReportedHost(
                inv=1,
                tickets=["BURNE-1001"],
                reason="reason mock",
                section="MockReportSection",
                name="hostname-mock",
                status="status-mock",
                project="project-mock",
                report_timestamp=timestamp(),
            )
        ]

        mock_publisher.verify_report_published.assert_called_once_with(today_report.report_key)
        mock_publisher.close_old_report.assert_called_once_with(today_report.report_key)
        mock_publisher.create_new_report.assert_called_once_with(
            summary, report_content.text(), report_content.report_hosts(), today_report
        )

        mock_publisher.create_new_report.assert_called_once_with(
            summary, report_content.text(), report_content.report_hosts(), today_report
        )

        assert not mock_publisher.update_existing_report.called

        assert not report_observer.report_updated.called
        report_observer.report_closed.assert_called_once_with(today_report.report_key, report_hosts=today_report.hosts)
        report_observer.report_created.assert_called_once_with(
            new_report_key, report_hosts=expected_report_hosts, previous_report_key=today_report.report_key
        )


@pytest.mark.usefixtures("mock_config", "test", "monkeypatch_startrek_options")
class TestStreamKey:
    def test_can_find_default_publisher_by_stream_key(self):
        ticket_params = startrek_report.TicketParams(queue="BURNE", type="serviceRequest")
        publisher = startrek_report.StarTrekReportPublisher(ticket_params)
        stream_key = base.StreamKey.from_wrapped_key(publisher.get_stream_key())

        found = base.ReportPublisher.by_stream_key(stream_key)

        assert type(found) == type(publisher)
        assert found.get_stream_key() == stream_key.wrapped_key()  # stream key must keep original value
        assert found.get_stream_key() == stream_key.unwrapped_key()

    def test_can_find_named_publisher_by_stream_key(self):
        ticket_params = startrek_report.TicketParams(queue="BURNE", type="serviceRequest")
        publisher = startrek_report.StarTrekReportPublisher(ticket_params)

        wrapped_stream_key = base.StreamKey(publisher.get_stream_key(), publisher.name).wrapped_key()
        stream_key = base.StreamKey.from_wrapped_key(wrapped_stream_key)

        found = base.ReportPublisher.by_stream_key(stream_key)

        assert type(found) == type(publisher)
        assert found.get_stream_key() == stream_key.unwrapped_key()

    def test_can_find_default_rotation_strategy_by_stream_key(self):
        stream_key = base.StreamKey.from_wrapped_key("mock-stream-key")
        found = base.RotationStrategy.by_name(stream_key.rotation_strategy_name)

        assert type(found) == base.DailyReportRotationStrategy

    @pytest.mark.parametrize("rotation_strategy", base.RotationStrategy.__subclasses__())
    def test_can_find_named_rotation_strategy_by_stream_key(self, rotation_strategy):
        if rotation_strategy.name == "mock":
            return  # NOTE(rocco66): subclass for tests
        publisher_name = startrek_report.StarTrekReportPublisher.name

        wrapped_stream_key = base.StreamKey("mock-stream-key", publisher_name, rotation_strategy.name).wrapped_key()
        stream_key = base.StreamKey.from_wrapped_key(wrapped_stream_key)

        found = base.RotationStrategy.by_name(stream_key.rotation_strategy_name)

        assert type(found) == rotation_strategy

    def test_generates_same_stream_key_as_from_which_was_constructed(self):
        old_stream_key = "z:1#a:2"
        stream_key = base.StreamKey.from_wrapped_key(old_stream_key)  # no prefix, wrong sorting.

        assert old_stream_key == stream_key.wrapped_key()


class TestGroupObserver:
    def _mock_observer(self, side_effect=None):
        class MockObserver(base.ReportObserver):
            report_created = Mock(side_effect=side_effect)
            report_failed_to_create = Mock(side_effect=side_effect)
            report_updated = Mock(side_effect=side_effect)
            report_closed = Mock(side_effect=side_effect)

        return MockObserver()

    def assert_event_propagated_to_all_observers(self, group_observer_method, observer_side_effect, args):
        observers_list = [
            self._mock_observer(side_effect=observer_side_effect),
            self._mock_observer(side_effect=observer_side_effect),
        ]
        group_observer = observers.ObserverGroup(observers_list)

        group_observer_method(group_observer, *args)

        method_name = group_observer_method.__name__
        for observer in observers_list:
            getattr(observer, method_name).assert_called_once_with(*args)

    @pytest.mark.parametrize("prev_report_key", [None, "mock-previous-report"])
    def test_report_create_propagated_to_all_observers(self, prev_report_key):
        self.assert_event_propagated_to_all_observers(
            observers.ObserverGroup.report_created,
            observer_side_effect=None,
            args=("report-mock", [_mock_problem_host()], prev_report_key),
        )

    def test_report_create_propagated_to_all_observers_even_when_they_raise_exception(self):
        self.assert_event_propagated_to_all_observers(
            observers.ObserverGroup.report_created,
            observer_side_effect=Exception("this is an error"),
            args=("report-mock", [_mock_problem_host()], "mock-previous-report"),
        )

    def test_report_creation_failure_propagated_to_all_observers(self):
        self.assert_event_propagated_to_all_observers(
            observers.ObserverGroup.report_failed_to_create,
            observer_side_effect=None,
            args=("failed to create report, wtf", [_mock_problem_host()]),
        )

    def test_report_creation_failure_propagated_to_all_observers_even_when_they_raise_exception(self):
        self.assert_event_propagated_to_all_observers(
            observers.ObserverGroup.report_failed_to_create,
            observer_side_effect=Exception("this is an error"),
            args=("failed to create report, wtf", [_mock_problem_host()]),
        )

    def test_report_update_propagated_to_all_observers(self):
        self.assert_event_propagated_to_all_observers(
            observers.ObserverGroup.report_updated,
            observer_side_effect=None,
            args=("mock-report-key", [_mock_problem_host()]),
        )

    def test_report_update_propagated_to_all_observers_even_when_they_raise_exception(self):
        self.assert_event_propagated_to_all_observers(
            observers.ObserverGroup.report_updated,
            observer_side_effect=Exception("this is an error"),
            args=("mock-report-key", [_mock_problem_host()]),
        )

    def test_report_closing_propagated_to_all_observers(self):
        self.assert_event_propagated_to_all_observers(
            observers.ObserverGroup.report_closed,
            observer_side_effect=None,
            args=("mock-report-key", [_mock_problem_host()]),
        )

    def test_report_closing_propagated_to_all_observers_even_when_they_raise_exception(self):
        self.assert_event_propagated_to_all_observers(
            observers.ObserverGroup.report_closed,
            observer_side_effect=Exception("this is an error"),
            args=("mock-report-key", [_mock_problem_host()]),
        )


@pytest.mark.usefixtures("mock_config", "test")
class TestHostTicketReportObserver:
    def test_when_report_created_sets_report_key_as_a_host_ticket(self, test):
        report_key = "today-ticket-key"
        # should not be changed
        host1 = test.mock_host({"inv": 1, "name": "default-1", "ticket": report_key})
        # should be updated
        host2 = test.mock_host({"inv": 2, "name": "default-2"})

        report_hosts = [_host_to_problem_host(host, "reason-mock", "section-mock") for host in (host1, host2)]
        observer = observers.HostTicketForReport()
        observer.report_created(report_key, report_hosts)

        host2.ticket = report_key
        test.hosts.assert_equal()

    def test_when_report_rolled_over_updates_host_ticket(self, test):
        old_report_key = "prev-ticket-key"
        new_report_key = "today-ticket-key"

        host = test.mock_host({"inv": 1, "name": "default-1", "ticket": old_report_key})

        report_hosts = [_host_to_problem_host(host, "reason-mock", "section-mock")]
        observer = observers.HostTicketForReport()
        observer.report_created(new_report_key, report_hosts, old_report_key)

        host.ticket = new_report_key
        test.hosts.assert_equal()

    def test_when_report_created_does_not_update_ticket_if_some_other_ticket_is_set(self, test):
        report_key = "today-ticket-key"

        host = test.mock_host({"inv": 1, "name": "default-1", "ticket": "some-other-ticket"})

        report_hosts = [_host_to_problem_host(host, "reason-mock", "section-mock")]
        observer = observers.HostTicketForReport()
        observer.report_created(report_key, report_hosts)

        test.hosts.assert_equal()

    def test_when_report_created_updates_only_report_hosts(self, test):
        test.mock_host({"inv": 1, "name": "default-1"})

        observer = observers.HostTicketForReport()
        observer.report_created("today-ticket-key", [], "prev-ticket-key")

        test.hosts.assert_equal()

    def test_when_report_updated_sets_report_key_as_a_host_ticket_if_not_yet_set(self, test):
        report_key = "today-ticket-key"
        # should not be changed
        host1 = test.mock_host({"inv": 1, "name": "default-1", "ticket": report_key})
        # should be updated
        host2 = test.mock_host({"inv": 2, "name": "default-2"})

        report_hosts = [_host_to_problem_host(host, "reason-mock", "section-mock") for host in (host1, host2)]
        observer = observers.HostTicketForReport()
        observer.report_updated(report_key, report_hosts)

        host2.ticket = report_key
        test.hosts.assert_equal()

    def test_when_report_updated_does_not_set_host_ticket_if_other_ticket_is_set(self, test):
        report_key = "today-ticket-key"
        # should not be changed
        host = test.mock_host({"inv": 1, "name": "default-1", "ticket": "some-other-ticket"})

        report_hosts = [_host_to_problem_host(host, "reason-mock", "section-mock")]
        observer = observers.HostTicketForReport()
        observer.report_updated(report_key, report_hosts)

        test.hosts.assert_equal()

    def test_when_report_updated_unsets_host_ticket_for_solved_hosts(self, test):
        report_key = "today-ticket-key"
        host = test.mock_host({"inv": 1, "name": "default-1", "ticket": report_key})

        report_hosts = [_host_to_problem_host(host, "reason-mock", "section-mock", solved=True)]
        observer = observers.HostTicketForReport()
        observer.report_updated(report_key, report_hosts)

        del host.ticket
        test.hosts.assert_equal()

    def test_when_report_updated_does_not_unset_host_ticket_for_solved_host_if_set_to_other_tickets(self, test):
        report_key = "today-ticket-key"
        host = test.mock_host({"inv": 1, "name": "default-1", "ticket": "some-other-ticket"})

        report_hosts = [_host_to_problem_host(host, "reason-mock", "section-mock", solved=True)]
        observer = observers.HostTicketForReport()
        observer.report_updated(report_key, report_hosts)

        test.hosts.assert_equal()

    def test_when_report_updated_updates_only_report_hosts(self, test):
        test.mock_host({"inv": 1, "name": "default-1"})

        observer = observers.HostTicketForReport()
        observer.report_updated("today-ticket-key", [])

        test.hosts.assert_equal()

    def test_when_report_closed_removes_report_key_from_host_ticket(self, test):
        report_key = "today-ticket-key"
        host1 = test.mock_host({"inv": 1, "name": "default-1", "ticket": report_key})
        host2 = test.mock_host({"inv": 2, "name": "default-2"})

        report_hosts = [
            _host_to_problem_host(host1, "reason-mock", "section-mock"),
            _host_to_problem_host(host2, "reason-mock", "section-mock"),
        ]

        observer = observers.HostTicketForReport()
        observer.report_closed(report_key, report_hosts)

        del host1.ticket
        test.hosts.assert_equal()

    def test_when_report_closed_does_not_remove_host_ticket_if_some_other_ticket_is_set(self, test):
        report_key = "today-ticket-key"
        host = test.mock_host({"inv": 1, "name": "default-1", "ticket": "some-other-ticket"})

        report_hosts = [_host_to_problem_host(host, "reason-mock", "section-mock")]

        observer = observers.HostTicketForReport()
        observer.report_closed(report_key, report_hosts)

        test.hosts.assert_equal()

    def test_when_report_closed_updates_only_hosts_in_report(self, test):
        report_key = "today-ticket-key"
        test.mock_host({"inv": 1, "name": "default-1", "ticket": report_key})

        observer = observers.HostTicketForReport()
        observer.report_closed(report_key, [])

        test.hosts.assert_equal()


@pytest.mark.usefixtures("mock_config", "test", "patch_audit_log")
class TestAuditLogObserver:
    report_key = "today-ticket-key"

    @staticmethod
    def _mock_audit_record(test, report_key, host, save=False):
        return test.audit_log.mock(
            {
                "id": AUDIT_LOG_ID,
                "status": audit_log.STATUS_ACCEPTED,
                "type": audit_log.TYPE_REPORT,
                "host_inv": host.inv,
                "host_name": host.name,
                "host_uuid": host.host_uuid,
                "project": host.project,
                "reason": host.reason,
                "payload": walle.util.misc.drop_none({"ticket": report_key}),
                "status_time": timestamp(),
                "time": timestamp(),
            },
            save=save,
        )

    def test_creates_audit_log_when_report_created(self, test):
        host = _mock_problem_host(inv=1, name="default-1", reason="reason-mock")
        self._mock_audit_record(test, self.report_key, host)

        observer = observers.AuditLogForReport()
        observer.report_created(self.report_key, [host], previous_report_key=None)

        test.audit_log.assert_equal()

    def test_creates_audit_log_when_report_failed_to_create(self, test):
        host = _mock_problem_host(inv=1, name="default-1", reason="reason-mock")

        log_record = self._mock_audit_record(test, report_key=None, host=host)
        log_record.status = audit_log.STATUS_FAILED
        log_record.error = "error message mock"

        observer = observers.AuditLogForReport()
        observer.report_failed_to_create("error message mock", [host])

        test.audit_log.assert_equal()

    def test_creates_audit_log_for_added_hosts_when_report_updated(self, test):
        host = _mock_problem_host(inv=1, name="default-1", reason="reason-mock")
        self._mock_audit_record(test, self.report_key, host)

        observer = observers.AuditLogForReport()
        observer.report_updated(self.report_key, [host])

        test.audit_log.assert_equal()

    def test_closes_audit_log_for_solved_hosts_when_report_updated(self, test):
        host = _mock_problem_host(inv=1, name="default-1", solved=True, audit_log_id=AUDIT_LOG_ID, reason="reason-mock")
        log_record = self._mock_audit_record(test, self.report_key, host, save=True)

        observer = observers.AuditLogForReport()
        observer.report_updated(self.report_key, [host])

        log_record.status = audit_log.STATUS_COMPLETED
        test.audit_log.assert_equal()

    def test_does_not_create_duplicate_audit_log_for_hosts_when_report_updated(self, test):
        host = _mock_problem_host(inv=1, name="default-1", audit_log_id=AUDIT_LOG_ID, reason="reason-mock")

        observer = observers.AuditLogForReport()
        observer.report_updated(self.report_key, [host])

        test.audit_log.assert_equal()

    def test_does_not_create_audit_log_for_solved_hosts_when_report_updated(self, test):
        host = _mock_problem_host(inv=1, name="default-1", solved=True, reason="reason-mock")

        observer = observers.AuditLogForReport()
        observer.report_updated(self.report_key, [host])

        test.audit_log.assert_equal()

    def test_closes_audit_log_for_solved_hosts_when_report_closed(self, test):
        host = _mock_problem_host(inv=1, name="default-1", solved=True, audit_log_id=AUDIT_LOG_ID, reason="reason-mock")
        log_record = self._mock_audit_record(test, self.report_key, host, save=True)

        observer = observers.AuditLogForReport()
        observer.report_closed(self.report_key, [host])

        log_record.status = audit_log.STATUS_COMPLETED
        test.audit_log.assert_equal()

    def test_cancels_audit_log_for_not_solved_hosts_when_report_closed(self, test):
        host = _mock_problem_host(inv=1, name="default-1", audit_log_id=AUDIT_LOG_ID, reason="reason-mock")
        log_record = self._mock_audit_record(test, self.report_key, host, save=True)

        observer = observers.AuditLogForReport()
        observer.report_closed(self.report_key, [host])

        log_record.status = audit_log.STATUS_CANCELLED
        test.audit_log.assert_equal()

    def test_does_nothing_if_no_audit_log_existed_for_host_when_report_closed(self, test):
        hosts = [
            _mock_problem_host(inv=1, name="default-1", solved=False, reason="reason-mock"),
            _mock_problem_host(inv=2, name="default-2", solved=True, reason="reason-mock"),
        ]

        observer = observers.AuditLogForReport()
        observer.report_closed(self.report_key, hosts)

        test.audit_log.assert_equal()


@pytest.mark.usefixtures("mock_config", "test")
class TestNotifyOwnersObserver:
    def test_notifies_project_owners_when_report_failed_to_create(self, test, mp):
        hosts = [_mock_problem_host(), _mock_problem_host()]
        mock_notification = mp.function(notifications.on_failed_to_create_report)

        error_message = "error message mock"
        observer = observers.NotifyProjectOwners()
        observer.report_failed_to_create(error_message, hosts)

        mock_notification.assert_called_once_with(hosts[0].project, error_message)

    def test_notifies_owners_of_all_projects_when_report_failed_to_create(self, test, mp):
        expected_projects = ["mock-project-1", "mock-project-2"]
        report_hosts = [_mock_problem_host(project=project_id) for project_id in expected_projects]

        called_projects = set()
        monkeypatch_function(
            mp,
            notifications.on_failed_to_create_report,
            side_effect=lambda project_id, *a, **k: called_projects.add(project_id),
        )

        error_message = "error message mock"
        observer = observers.NotifyProjectOwners()
        observer.report_failed_to_create(error_message, report_hosts)

        assert expected_projects == sorted(called_projects)


@pytest.mark.usefixtures("mock_config", "test")
class TestReportStorageObserver:
    def test_saves_report_when_report_created(self, mp, test):
        now = patch_current_time(mp)
        stream_key = "mock-stream-key"
        report_key = "today-ticket-key"

        report_hosts = [_mock_problem_host(inv=1, name="default-1")]
        test.failure_reports.mock(
            {
                "report_key": report_key,
                "report_date": now,
                "stream_key": stream_key,
                "create_time": timestamp(),
                "last_update_time": timestamp(),
                "hosts": report_hosts,
            },
            save=False,
        )

        observer = observers.StoreReport(stream_key=stream_key)
        observer.report_created(report_key, report_hosts, previous_report_key=None)

        test.failure_reports.assert_equal()

    def test_updates_report_when_report_updated(self, mp, test):
        now = patch_current_time(mp)
        stream_key = "mock-stream-key"
        report_key = "today-ticket-key"

        report = test.failure_reports.mock(
            {
                "report_key": report_key,
                "report_date": now,
                "stream_key": stream_key,
                "create_time": timestamp() - 10,
                "last_update_time": timestamp() - 10,
                "hosts": [_mock_problem_host(inv=1, name="default-1")],
            }
        )

        report_hosts = [_mock_problem_host(inv=1, name="default-1"), _mock_problem_host(inv=2, name="default-2")]

        observer = observers.StoreReport(stream_key=stream_key)
        observer.report_updated(report_key, report_hosts)

        report.hosts = report_hosts
        report.last_update_time = timestamp()
        test.failure_reports.assert_equal()

    def test_closes_report_when_report_closed(self, mp, test):
        now = patch_current_time(mp)
        stream_key = "mock-stream-key"
        report_key = "today-ticket-key"

        report = test.failure_reports.mock(
            {
                "report_key": report_key,
                "report_date": now,
                "stream_key": stream_key,
                "create_time": timestamp() - 10,
                "last_update_time": timestamp() - 10,
                "hosts": [_mock_problem_host(inv=1, name="default-1")],
            }
        )

        report_hosts = [_mock_problem_host(inv=1, name="default-1"), _mock_problem_host(inv=2, name="default-2")]

        observer = observers.StoreReport(stream_key=stream_key)
        observer.report_closed(report_key, report_hosts)

        report.hosts = report_hosts
        report.last_update_time = timestamp()
        report.closed = True
        test.failure_reports.assert_equal()


@pytest.mark.usefixtures("mock_config", "test", "monkeypatch_startrek_options")
class TestStarTrekReportPublisher:
    class MockSectionFormatter(base.SectionFormatter):
        def format_title(self):
            return "title"

        def format_section_hosts(self, section):
            return "description"

    @classmethod
    def _mock_startrek_client(cls, mp):
        return mp.function(startrek.get_client, module=startrek)

    @classmethod
    def _mock_report_content(cls, report_hosts=None):
        content = reports.ReportContents(base.ReportFormatter())
        section = base.ReportSection("MockReportSection", cls.MockSectionFormatter("title"))

        if report_hosts:
            for host in report_hosts:
                tickets = [host.ticket] if host.ticket else None
                section.add_problem_host(
                    _mock_problem_host(inv=host.inv, name=host.name, tickets=tickets, reason="reason mock"),
                )
        else:
            section.add_problem_host(host=_mock_problem_host(inv=1, reason="reason mock"))

        content.add_section(section)
        return content

    def test_ticket_params_compose_a_stream_key(self):
        ticket_params = startrek_report.TicketParams(queue="BURNE", type="serviceRequest")
        publisher = startrek_report.StarTrekReportPublisher(ticket_params)

        assert publisher.get_stream_key() == "queue:BURNE#type:serviceRequest"

    def test_can_build_publisher_with_lists_in_stream_key(self):
        ticket_params = startrek_report.TicketParams(queue="BURNE", tags=["BOT", "DISK"])
        publisher = startrek_report.StarTrekReportPublisher(ticket_params)

        stream_key = base.StreamKey.from_wrapped_key(publisher.get_stream_key())

        found = base.ReportPublisher.by_stream_key(stream_key)

        assert type(found) == type(publisher)
        assert found.get_stream_key() == stream_key.wrapped_key()
        assert found.get_stream_key() == stream_key.unwrapped_key()

    def test_updates_existing_ticket_with_given_content(self, test, mp):
        test.mock_host({"inv": 1, "name": "default-1"})
        test.mock_host({"inv": 2, "name": "default-2", "ticket": "BURNE-10001"})

        mock_startrek = self._mock_startrek_client(mp)

        existing_report = base.ErrorReportModel(report_key="BURNE-10001")
        report_content = self._mock_report_content(test.hosts.objects)

        publisher = startrek_report.StarTrekReportPublisher(startrek_report.TicketParams(queue="BURNE"))
        publisher.update_existing_report(existing_report, report_content.text(), report_content.report_hosts())

        mock_startrek().modify_issue.assert_called_once_with(
            "BURNE-10001",
            {
                "description": report_content.text(),
                "links": [{"relationship": Relationship.RELATES, "issue": "BURNE-10001"}],
            },
        )

    def test_closes_existing_ticket(self, mp):
        mock_startrek = self._mock_startrek_client(mp)

        publisher = startrek_report.StarTrekReportPublisher(startrek_report.TicketParams(queue="BURNE"))
        publisher.close_old_report("BURNE-0001")

        mock_startrek().close_issue.assert_called_once_with("BURNE-0001")

    def test_creates_new_ticket(self, mp):
        mock_startrek = self._mock_startrek_client(mp)
        mock_startrek().get_issues.return_value = []
        mock_startrek().create_issue.return_value = {"key": "BURNE-MOCK"}

        summary = "Summary mock"
        report_content = self._mock_report_content()
        ticket_params = startrek_report.TicketParams(queue="BURNE")
        publisher = startrek_report.StarTrekReportPublisher(ticket_params)

        publisher.create_new_report(summary, report_content.text(), report_content.report_hosts())

        mock_startrek().create_issue.assert_called_once_with(
            {
                "queue": "BURNE",
                "description": report_content.text(),
                "summary": summary,
                "links": [],
                "unique": ticket_params.unique_field(summary),
            }
        )

    def test_checks_ticket_by_unique_field_before_create_new_one(self, mp):
        mock_startrek = self._mock_startrek_client(mp)
        mock_startrek().get_issues.return_value = [{"key": "BURNE-MOCK"}]
        mock_startrek().create_issue.side_effect = RuntimeError

        summary = "Summary mock"
        report_content = self._mock_report_content()
        ticket_params = startrek_report.TicketParams(queue="BURNE")
        publisher = startrek_report.StarTrekReportPublisher(ticket_params)

        publisher.create_new_report(summary, report_content.text(), report_content.report_hosts())

        mock_startrek().get_issues.assert_called_once_with(filter={"unique": ticket_params.unique_field(summary)})

    def test_checks_ticket_by_unique_field_when_create_fails_with_error_code(self, mp):
        mock_startrek = self._mock_startrek_client(mp)
        mock_startrek().get_issues.side_effect = ([], [{"key": "BURNE-MOCK"}])
        mock_startrek().create_issue.side_effect = StartrekClientRequestError(Mock(), "reason-mock")

        summary = "Summary mock"
        report_content = self._mock_report_content()
        ticket_params = startrek_report.TicketParams(queue="BURNE")
        publisher = startrek_report.StarTrekReportPublisher(ticket_params)

        publisher.create_new_report(summary, report_content.text(), report_content.report_hosts())

        assert mock_startrek().get_issues.mock_calls == [
            # two times same call
            call(filter={"unique": ticket_params.unique_field(summary)}),
            call(filter={"unique": ticket_params.unique_field(summary)}),
        ]

    def test_raises_ticket_create_error_when_can_not_create_ticket(self, mp):
        mock_startrek = self._mock_startrek_client(mp)
        mock_startrek().get_issues.return_value = []
        mock_startrek().create_issue.side_effect = StartrekClientRequestError(Mock(), "reason-mock")

        summary = "Summary mock"
        report_content = self._mock_report_content()
        ticket_params = startrek_report.TicketParams(queue="BURNE")
        publisher = startrek_report.StarTrekReportPublisher(ticket_params)

        with pytest.raises(base.ReportPublisherFailure):
            publisher.create_new_report(summary, report_content.text(), report_content.report_hosts())

    def test_links_previous_ticket_when_creating_new(self, mp):
        old_ticket_key = "BURNE-OLD"
        new_ticket_key = "BURNE-NEW"
        mock_startrek = self._mock_startrek_client(mp)
        mock_startrek().get_issues.return_value = []
        mock_startrek().create_issue.return_value = {"key": new_ticket_key}

        previous_report = base.ErrorReportModel(report_key=old_ticket_key)

        summary = "Summary mock"
        report_content = self._mock_report_content()
        ticket_params = startrek_report.TicketParams(queue="BURNE")
        publisher = startrek_report.StarTrekReportPublisher(ticket_params)

        publisher.create_new_report(summary, report_content.text(), report_content.report_hosts(), previous_report)

        mock_startrek().create_issue.assert_called_once_with(
            {
                "queue": "BURNE",
                "description": report_content.text(),
                "summary": summary,
                "links": [{"relationship": Relationship.RELATES, "issue": old_ticket_key}],
                "unique": ticket_params.unique_field(summary),
            }
        )

    def test_links_host_tickets_when_creating_new_report_ticket(self, test, mp):
        test.mock_host({"inv": 1, "name": "default-1", "ticket": "TICKET-KEY-MOCK-1"})
        test.mock_host({"inv": 2, "name": "default-2", "ticket": "TICKET-KEY-MOCK-2"})
        # another host with the same ticket. eine does this.
        test.mock_host({"inv": 3, "name": "default-3", "ticket": "TICKET-KEY-MOCK-2"})

        previous_report = base.ErrorReportModel(
            report_key="MOCK-OLD-TICKET",
            hosts=[
                {"inv": 1, "tickets": ["TICKET-KEY-MOCK-3"]},  # this one exists in current report and should be aded
                {
                    "inv": 0,
                    "tickets": ["TICKET-KEY-MOCK-4"],
                },  # this one does not exist in current report and should not
            ],
        )

        new_ticket_key = "BURNE-MOCK"
        mock_startrek = self._mock_startrek_client(mp)
        mock_startrek().get_issues.return_value = []
        mock_startrek().create_issue.return_value = {"key": new_ticket_key}

        summary = "Summary mock"
        report_content = self._mock_report_content(test.hosts.objects)
        ticket_params = startrek_report.TicketParams(queue="BURNE")
        publisher = startrek_report.StarTrekReportPublisher(ticket_params)

        publisher.create_new_report(summary, report_content.text(), report_content.report_hosts(), previous_report)

        mock_startrek().create_issue.assert_called_once_with(
            {
                "queue": "BURNE",
                "description": report_content.text(),
                "summary": summary,
                "links": [
                    {"relationship": Relationship.RELATES, "issue": "TICKET-KEY-MOCK-1"},
                    {"relationship": Relationship.RELATES, "issue": "TICKET-KEY-MOCK-2"},
                    {"relationship": Relationship.RELATES, "issue": previous_report.report_key},
                    {"relationship": Relationship.RELATES, "issue": "TICKET-KEY-MOCK-3"},
                ],
                "unique": ticket_params.unique_field(summary),
            }
        )

    def test_open_ticket_is_published(self, mp):
        mock_startrek = self._mock_startrek_client(mp)
        mock_startrek().get_issue.return_value = {
            "key": "BURNE-0001",
            "status": {"id": "4", "key": "inProgress"},
        }
        publisher = startrek_report.StarTrekReportPublisher(startrek_report.TicketParams(queue="BURNE"))

        assert publisher.verify_report_published("BURNE-0001")
        mock_startrek().get_issue.assert_called_once_with("BURNE-0001")

    def test_ticket_with_resolution_is_not_published(self, mp):
        mock_startrek = self._mock_startrek_client(mp)
        mock_startrek().get_issue.return_value = {
            "key": "BURNE-0001",
            "status": {"id": "3", "key": "closed"},
            "resolution": {"id": "2", "key": "won'tFix"},
        }

        publisher = startrek_report.StarTrekReportPublisher(startrek_report.TicketParams(queue="BURNE"))

        assert not publisher.verify_report_published("BURNE-0001")
        mock_startrek().get_issue.assert_called_once_with("BURNE-0001")

    def test_missing_ticket_is_not_published(self, mp):
        mock_startrek = self._mock_startrek_client(mp)
        mock_response = Mock(status_code=404)
        mock_startrek().get_issue.side_effect = StartrekClientRequestError(mock_response, "blah-blah-can-not-do-this")

        publisher = startrek_report.StarTrekReportPublisher(startrek_report.TicketParams(queue="BURNE"))

        assert not publisher.verify_report_published("BURNE-0001")
        mock_startrek().get_issue.assert_called_once_with("BURNE-0001")


class TestStartrekTicketParams:
    def test_from_stream_key_creates_normalized_object(self):
        stream_key = "queue:BURNE#type:serviceRequest"
        expected_params = {"queue": "BURNE", "type": "serviceRequest"}

        ticket_params = startrek_report.TicketParams.from_stream_key(stream_key)
        assert dict(ticket_params) == expected_params

    def test_from_stream_key_normalizes_tags(self):
        stream_key = "queue:BURNE#tags:BOT,MEMORY,dcops"
        expected_params = {"queue": "BURNE", "tags": ["BOT", "MEMORY", "dcops"]}

        ticket_params = startrek_report.TicketParams.from_stream_key(stream_key)
        assert dict(ticket_params) == expected_params

    @pytest.mark.parametrize(
        ["original", "expected"],
        [
            ("13111", [13111]),
            ("Wall-E", ["Wall-E"]),
            ("Wall-E,13111", [13111, "Wall-E"]),
        ],
    )
    def test_from_stream_key_normalizes_components(self, original, expected):
        stream_key = "queue:BURNE#components:" + original
        expected_params = {"queue": "BURNE", "components": expected}

        ticket_params = startrek_report.TicketParams.from_stream_key(stream_key)
        assert dict(ticket_params) == expected_params

    @pytest.mark.parametrize(["original", "expected"], [("Wall-E", "Wall-E"), ("13111", 13111)])
    def test_from_stream_key_normalizes_projects(self, original, expected):
        stream_key = "queue:BURNE#project:" + original
        expected_params = {"queue": "BURNE", "project": expected}

        ticket_params = startrek_report.TicketParams.from_stream_key(stream_key)
        assert dict(ticket_params) == expected_params

    def test_from_params_normalizes_tags(self):
        ticket_params = {"queue": "BURNE", "tags": "BOT,MEMORY,dcops"}
        expected_params = {"queue": "BURNE", "tags": ["BOT", "MEMORY", "dcops"]}

        params = startrek_report.TicketParams.from_ticket_params(ticket_params)
        assert dict(params) == expected_params

    @pytest.mark.parametrize(
        ["original", "expected"],
        [
            (13111, [13111]),
            ([13111], [13111]),
            ("13111", [13111]),
            (["13111"], [13111]),
            ("Wall-E", ["Wall-E"]),
            (["Wall-E"], ["Wall-E"]),
            ("Wall-E,13111", [13111, "Wall-E"]),
            (["Wall-E", "13111"], [13111, "Wall-E"]),
        ],
    )
    def test_from_params_normalizes_components(self, original, expected):
        ticket_params = {"queue": "BURNE", "components": original}
        expected_params = {"queue": "BURNE", "components": expected}

        params = startrek_report.TicketParams.from_ticket_params(ticket_params)
        assert dict(params) == expected_params

    @pytest.mark.parametrize(
        ["original", "expected"],
        [
            (13111, 13111),
            ("13111", 13111),
            ("Wall-E", "Wall-E"),
        ],
    )
    def test_from_params_normalizes_projects(self, original, expected):
        ticket_params = {"queue": "BURNE", "project": original}
        expected_params = {"queue": "BURNE", "project": expected}

        params = startrek_report.TicketParams.from_ticket_params(ticket_params)
        assert dict(params) == expected_params

    def test_to_stream_key_normalizes_tags(self):
        ticket_params = {"queue": "BURNE", "tags": ["dcops", "MEMORY", "BOT"]}
        expected_stream_key = "queue:BURNE#tags:BOT,MEMORY,dcops"

        assert startrek_report.TicketParams(**ticket_params).stream_key() == expected_stream_key

    @pytest.mark.parametrize(
        ["original", "expected"],
        [
            (["Wall-E", 13111], "13111,Wall-E"),
            ([13111], "13111"),
            (["Wall-E"], "Wall-E"),
        ],
    )
    def test_to_stream_key_normalizes_components(self, original, expected):
        ticket_params = {"queue": "BURNE", "components": original}
        expected_stream_key = "components:" + expected + "#queue:BURNE"

        assert startrek_report.TicketParams(**ticket_params).stream_key() == expected_stream_key

    @pytest.mark.parametrize(
        ["original", "expected"],
        [
            (13111, "13111"),
            ("Wall-E", "Wall-E"),
        ],
    )
    def test_to_stream_key_normalizes_projects(self, original, expected):
        ticket_params = {"queue": "BURNE", "project": original}
        expected_stream_key = "project:" + expected + "#queue:BURNE"

        assert startrek_report.TicketParams(**ticket_params).stream_key() == expected_stream_key

    def test_to_stream_key_drops_followers(self):
        ticket_params = {"queue": "BURNE", "followers": ["robot-walle"]}
        expected_stream_key = "queue:BURNE"

        assert startrek_report.TicketParams(**ticket_params).stream_key() == expected_stream_key

    def test_to_stream_drops_linked_tickets(self):
        ticket_params = {"queue": "BURNE", "links": ["TICKET-1", "TICKET-2"]}
        expected_stream_key = "queue:BURNE"

        assert startrek_report.TicketParams(**ticket_params).stream_key() == expected_stream_key

    def test_to_stream_drops_unique(self):
        ticket_params = {"queue": "BURNE", "unique_salt": "look, ma, I'm so unique"}
        expected_stream_key = "queue:BURNE"

        assert startrek_report.TicketParams(**ticket_params).stream_key() == expected_stream_key

    def test_usable_as_dict_key(self):
        params = {"queue": "BURNE", "project": "Wall-E"}
        params = startrek_report.TicketParams.from_ticket_params(params)

        d = dict({params: ["project-1", "project-2"]})

        assert list(d.keys()) == [params]

    def test_normalized_params_compare_equal_for_dict(self):
        params_1 = startrek_report.TicketParams.from_ticket_params(dict(queue="BURNE", components="13111"))
        params_2 = startrek_report.TicketParams.from_ticket_params(dict(queue="BURNE", components=[13111]))
        params_3 = startrek_report.TicketParams.from_ticket_params(
            dict(queue="BURNE", components=["13111"], followers=["robot-walle"])
        )

        d = defaultdict(list)
        d[params_1].append("project-1")
        d[params_2].append("project-2")
        d[params_3].append("project-3")

        assert d == {params_1: ["project-1", "project-2", "project-3"]}
        assert d == {params_2: ["project-1", "project-2", "project-3"]}
        assert d == {params_3: ["project-1", "project-2", "project-3"]}

    def test_unique_does_not_affect_comparison(self):
        params_1 = startrek_report.TicketParams.from_ticket_params(dict(queue="BURNE", unique_salt="uniq-1"))
        params_2 = startrek_report.TicketParams.from_ticket_params(dict(queue="BURNE", unique_salt="uniq-2"))

        d = {}
        d.setdefault(params_1, []).append("project-1")
        d.setdefault(params_2, []).append("project-2")

        assert d == {params_1: ["project-1", "project-2"]}
        assert d == {params_2: ["project-1", "project-2"]}

    def test_extendable(self):
        params = dict(queue="BURNE", components="13111")

        original = dict(queue="BURNE", components=[13111])
        amended = dict(queue="BURNE", components=[13111], summary="BURNE")

        params_1 = startrek_report.TicketParams.from_ticket_params(params)
        params_2 = params_1.append(summary="BURNE")

        assert dict(params_1) == original  # check params_1 did not amend themselves
        assert dict(params_2) == amended  # check params_2 contain new keys

    def test_unique_key_contains_stream_key_and_provided_summary(self):
        params = {"queue": "BURNE", "project": "Wall-E"}
        params = startrek_report.TicketParams.from_ticket_params(params)

        unique_field = params.unique_field("unique summary")

        expected_unique_field = "project:Wall-E#queue:BURNE##unique summary"
        assert expected_unique_field == unique_field

    def test_appended_unique_appears_in_unique_field(self):
        params = {"queue": "BURNE", "project": "Wall-E"}
        params = startrek_report.TicketParams.from_ticket_params(params).append(unique_salt="uniq-e")

        unique_field = params.unique_field("unique summary")

        expected_unique_field = "project:Wall-E#queue:BURNE#uniq-e#unique summary"
        assert expected_unique_field == unique_field

    def test_unique_from_params_appers_in_unique_field(self):
        params = {"queue": "BURNE", "project": "Wall-E", "unique_salt": "uniq-e"}
        params = startrek_report.TicketParams.from_ticket_params(params)

        unique_field = params.unique_field("unique summary")

        expected_unique_field = "project:Wall-E#queue:BURNE#uniq-e#unique summary"
        assert expected_unique_field == unique_field

    def test_replace_pound_sign_to_underscore(self):
        ticket_params = {"queue": "MAN-4#B.1.09"}

        expected_stream_key = "queue:MAN-4_B.1.09"

        assert startrek_report.TicketParams.from_ticket_params(ticket_params).stream_key() == expected_stream_key


class TestTicketParams:
    def test_to_dict_drops_none(self):
        original_dict = {
            "close_transition": None,
            "tags": ["man1-0784.search.yandex.net", "rtc", "walle_clocksource"],
            "summary": "RTC",
            "queue": "RUNTIMECLOUD",
            "components": ["wall-e"],
            "unique_salt": 2422532,
        }

        ticket_params = startrek_report.TicketParams(**original_dict)
        assert ticket_params.to_dict() == drop_none(original_dict)
