from unittest import mock

import pytest

import walle.clients.startrek
from infra.walle.server.tests.lib.util import monkeypatch_config, AUDIT_LOG_ID, timestamp
from walle import audit_log
from walle.failure_reports import cms_reports
from walle.util.misc import drop_none

CONFIG_MOCK = [
    {'url_match': r'.*clusterstate\.yandex-team\.ru.*', 'queue': 'GENCFG', 'responsible': 'okats'},
    {'url_match': r'.*cloud-adapter-.*\.yt\.yandex-team\.ru.*', 'queue': 'YTADMIN', 'responsible': 'alximik'},
    {'url_match': r'.*qloud-hfsm-.*\.n\.yandex-team\.ru.*', 'queue': 'QLOUD', 'responsible': 'amich'},
]


@pytest.fixture
def mock_config(mp):
    monkeypatch_config(mp, "startrek.access_token", "<nothing>")
    monkeypatch_config(mp, "stand.name", "mocked wall-e")
    monkeypatch_config(mp, "stand.ui_url", "ftp://non-existing-host/wall-e/index.html")


def mock_projects(walle_test, overrides, shift='one'):
    walle_test.mock_project({'id': "mocked-1{}".format(shift), "name": "Mocked 1{}".format(shift)})
    for i in range(10, 7, -1):
        update = {"id": "mocked-cms-{}{}".format(i, shift), "name": "Mocked {}{}".format(i, shift)}
        update.update(overrides)
        walle_test.mock_project(update)
    walle_test.mock_project({'id': "mocked-2{}".format(shift), "name": "Mocked 2{}".format(shift)})


@pytest.fixture
def report(walle_test):
    mock_projects(walle_test, {'cms': 'http://clusterstate.yandex-team.ru/api/v1', 'cms_api_version': 'v1.0'})
    return cms_reports.CmsReport(
        cms_reports.CmsReportParams(
            url_match=r'.*clusterstate\.yandex-team\.ru.*',
            queue='GENCFG',
            responsible='okats',
        )
    )


@pytest.mark.usefixtures('walle_test')
def test_create_reports():
    reports = cms_reports.create_reports(CONFIG_MOCK, None)
    assert len(reports) == len(CONFIG_MOCK)
    for report in reports:
        assert isinstance(report, cms_reports.CmsReport)
        assert isinstance(report.report_params, cms_reports.CmsReportParams)


class TestCmsReportParams:
    def test_init(self):
        params = cms_reports.CmsReportParams(url_match='umock', queue='qmock', responsible='rmock', dry_run=False)
        assert params.dry_run is False
        assert params.queue == 'qmock'
        assert params.responsible == 'rmock'
        assert params.url_match == 'umock'


def mock_section(text):
    m = mock.Mock()
    m.empty = mock.Mock(return_value=False)
    m.text = mock.Mock(return_value=text)
    return m


class TestCmsReportFormatter:
    def test_format(self):
        expected_result = "section 1\n\nsection 2" + cms_reports._CmsReportFormatter.REPORT_FOOTER

        mocked_sections = (mock_section("section 1"), mock_section("section 2"))
        formatter = cms_reports._CmsReportFormatter()
        result = formatter.format(mocked_sections)
        assert result == expected_result


class TestCmsReport:
    def test_create_instance(self, report):
        assert report.report_params.url_match == r'.*clusterstate\.yandex-team\.ru.*'
        # there are 3 projects matching the pattern
        assert len(report.projects) == 3

    def test_ticket_params(self, report):
        params = report.ticket_params()
        assert params['queue'] == 'GENCFG'
        assert params['assignee'] == 'okats'
        assert sorted(params['tags']) == sorted(['cms', 'wall-e', 'cms_error'])

    def test_create_report(self, mp, report):
        mp.function(walle.clients.startrek.get_client, return_value=mock.Mock())
        startrek_report = report._create_report()
        assert isinstance(startrek_report, cms_reports.ErrorHostsReport)
        assert (
            startrek_report._stream_key == 'startrek/failing_hosts@assignee:okats'
            '#queue:GENCFG#tags:cms,cms_error,wall-e'
        )

    def test_publish(self, walle_test, mp, report):
        def _create_ticket(ticket):
            ret = {"key": "GENCFG-1"}
            ret.update(ticket)
            return ret

        client_mock = mock.Mock()
        client_mock.get_issues = mock.Mock(return_value=[])
        client_mock.create_issue = mock.Mock(side_effect=_create_ticket)
        mp.function(walle.clients.startrek.get_client, return_value=client_mock)

        unique_salt = "2019-01-01-00"
        mp.method(report._unique_salt, obj=cms_reports.CmsReport, return_value=unique_salt)

        monkeypatch_config(mp, "stand.ui_url", "http://localhost")
        host = walle_test.mock_host(
            {
                "name": 'mocked',
                "status": "dead",
                "status_reason": "CMS has not allowed to process host yet",
                "project": "mocked-cms-10one",
                "status_audit_log_id": AUDIT_LOG_ID,
            }
        )
        walle_test.audit_log.mock(
            {
                "id": AUDIT_LOG_ID,
                "status": audit_log.STATUS_ACCEPTED,
                "type": audit_log.TYPE_REPORT_HOST_FAILURE,
                "host_inv": host.inv,
                "host_name": host.name,
                "project": host.project,
                "reason": "mocked-reason",
                "payload": drop_none({"mocked-payload": "mocked-value"}),
                "status_time": timestamp(),
                "time": timestamp(),
            }
        )

        report.publish()

        create_issue_args, _ = client_mock.create_issue.call_args_list.pop()
        create_issue_param = create_issue_args[0]
        assert client_mock.get_issues.mock_calls == [
            mock.call(
                filter={
                    "unique": "assignee:okats#queue:GENCFG#tags:cms,cms_error,wall-e#{}#Wall-e CMS errors".format(
                        unique_salt
                    )
                }
            )
        ]
        assert host.name in create_issue_param["description"]
        assert create_issue_param["summary"] == "Wall-e CMS errors"
        assert create_issue_param["queue"] == "GENCFG"
        assert create_issue_param["assignee"] == "okats"
