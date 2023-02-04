from unittest.mock import Mock

import pytest

from walle.fsm_stages import report_rack_unavailable, startrek_report
from walle.hosts import Host, HostLocation
from walle.projects import Project


def test_location_is_a_component(mp):
    host = Host(
        name="mock-host-name",
        location=HostLocation(country="RU", city="MOW", datacenter="UGRB", queue="UGRB-1.2", rack="1"),
    )

    def verify(ticket_params, summary, formatter):
        assert ticket_params.get("components") == ["UGR"]

    mp.function(startrek_report.get_report, side_effect=verify)

    report_rack_unavailable._report_getter(Mock(host=host, project=Project(), checks=[]))


def test_rack_location_is_a_summary(mp):
    host = Host(
        name="mock-host-name",
        location=HostLocation(country="RU", city="MOW", datacenter="UGRB", queue="UGRB-1.2", rack="1"),
    )

    def verify(ticket_params, summary, formatter):
        assert "UGRB-1.2 rack 1" in summary

    mp.function(startrek_report.get_report, side_effect=verify)

    report_rack_unavailable._report_getter(Mock(host=host, project=Project(), checks=[]))


def test_grafana_link_in_report_body(mp):
    host = Host(
        name="mock-host-name",
        location=HostLocation(country="FI", city="MANTSALA", datacenter="B", queue="MAN-1#B.1.06", rack="1D9"),
    )

    class _MockSection:
        def empty(self):
            return False

        def text(self):
            return "section body"

    def verify(ticket_params, summary, formatter):
        expected_link = (
            "https://grafana.yandex-team.ru/d/f_EZJFaiz/itdc-alldc-rack-detail?orgId=1&refresh=1m"
            "&var-CITY=MANTSALA&var-SITE=B&var-MODULE=MAN-1-B-1-06&var-RACK=1D9&from=now-2d&to=now"
        )
        assert expected_link in formatter.format([_MockSection()])

    mp.function(startrek_report.get_report, side_effect=verify)

    report_rack_unavailable._report_getter(Mock(host=host, project=Project(), checks=[]))


def test_can_not_create_reports_if_location_is_unknown(mp):
    host = Host(
        name="mock-host-name",
        location=HostLocation(country="CN", city="Huangow", datacenter="UGRB", queue="UGRB-1.2", rack="1"),
    )

    mp.function(startrek_report.get_report)

    with pytest.raises(startrek_report.CanNotCreateReports):
        report_rack_unavailable._report_getter(Mock(host=host, project=Project(), checks=[]))


def test_reports_to_itdc_queue(mp):
    host = Host(
        name="mock-host-name",
        location=HostLocation(country="RU", city="MOW", datacenter="UGRB", queue="UGRB-1.2", rack="1"),
    )

    def verify(ticket_params, summary, formatter):
        assert ticket_params.get("queue") == "ITDC"

    mp.function(startrek_report.get_report, side_effect=verify)

    report_rack_unavailable._report_getter(Mock(host=host, project=Project(), checks=[]))


def test_reports_has_location_tags(mp):
    host = Host(
        name="mock-host-name",
        location=HostLocation(country="RU", city="MOW", datacenter="UGRB", queue="UGRB-1.2", rack="1"),
    )

    def verify(ticket_params, summary, formatter):
        assert ticket_params.get("tags") == sorted(["UGR", "UGRB-1.2", "rack-1"])

    mp.function(startrek_report.get_report, side_effect=verify)

    report_rack_unavailable._report_getter(Mock(host=host, project=Project(), checks=[]))
