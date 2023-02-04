"""Tests host state and status garbage collecting."""

from unittest.mock import call

import pytest
from mongoengine import NotUniqueError

import walle.expert
import walle.host_status
from infra.walle.server.tests.lib.util import AUDIT_LOG_ID, mock_schedule_assigned, mock_host_health_status, TestCase
from walle import authorization
from walle.clients import startrek
from walle.clients.startrek import StartrekClientError
from walle.expert.automation_plot import Check, AutomationPlot
from walle.expert.decision import Decision
from walle.expert.decisionmakers import get_decision_maker
from walle.expert.types import WalleAction, CheckType, CheckStatus
from walle.hosts import HostState, HostStatus, HealthStatus, StateExpire
from walle.models import timestamp
from walle.util.misc import drop_none

MOCK_TICKET_KEY = "MOCK-1234"


@pytest.fixture
def test(mp, request, monkeypatch_timestamp):
    mp.function(
        walle.host_status.get_tickets_by_query, module=walle.host_status, return_value=[{"key": MOCK_TICKET_KEY}]
    )
    return TestCase.create(request)


@pytest.mark.usefixtures("monkeypatch_audit_log")
class BaseHostStateGcByTimeout:
    @staticmethod
    def _mock_maintenance_host(test, timeout_time, target_status, project_healing_automation_enabled=False):
        raise NotImplementedError

    @staticmethod
    def check_host_status_garbage_collected(test, host, target_status, reason, power_on=False):
        walle.host_status._gc_maintenance_timeout()
        mock_schedule_assigned(host, target_status, manual=False, reason=reason, power_on=power_on)

        test.hosts.assert_equal()

    @pytest.mark.parametrize("target_status", [HostStatus.READY, HostStatus.DEAD])
    @pytest.mark.parametrize(
        "project_healing_automation_enabled,expected_power_on_value", [(True, True), (False, False)]
    )
    def test_maintenance_with_timeout_is_garbage_collected(
        self, test, target_status, project_healing_automation_enabled, expected_power_on_value
    ):
        host = self._mock_maintenance_host(
            test,
            timeout_time=timestamp(),
            target_status=target_status,
            project_healing_automation_enabled=project_healing_automation_enabled,
        )

        self.check_host_status_garbage_collected(
            test,
            host,
            target_status,
            reason="The 'maintenance' state requested by An unauthorized API user has timed out.",
            power_on=expected_power_on_value,
        )

    @pytest.mark.parametrize("target_status", [HostStatus.READY, HostStatus.DEAD])
    def test_maintenance_with_ticket_and_no_timeout_is_not_garbage_collected__state_timeout(self, test, target_status):
        self._mock_maintenance_host(test, timeout_time=None, target_status=target_status)

        walle.host_status._gc_maintenance_timeout()
        test.hosts.assert_equal()


class TestHostStateGcByTimeoutStateExpire(BaseHostStateGcByTimeout):
    @staticmethod
    def _mock_maintenance_host(test, timeout_time, target_status, project_healing_automation_enabled=False):
        project = test.mock_project(
            {
                "id": "some-id",
                "healing_automation": {"enabled": project_healing_automation_enabled},
            }
        )
        host = test.mock_host(
            {
                "project": project.id,
            }
        )
        host.set_state(
            HostState.MAINTENANCE,
            authorization.ISSUER_WALLE,
            AUDIT_LOG_ID,
            expire=StateExpire(
                ticket="ticket-mock",
                time=timeout_time,
                status=target_status,
                issuer=authorization.ISSUER_ANONYMOUS_USER,
            ),
        )
        host.save()
        return host


@pytest.mark.usefixtures("monkeypatch_audit_log")
class HostStateLeaveMaintenanceByTicketGc:
    @staticmethod
    def make_maintenance_host_with_ticket(
        test,
        target_status,
        inv=1,
        timeout_time=None,
        ticket_key=MOCK_TICKET_KEY,
        scenario_id=None,
        project_healing_automation_enabled=False,
    ):
        raise NotImplementedError

    @staticmethod
    def check_host_garbage_collected(test, host, target_status, reason, power_on=False):
        walle.host_status._gc_maintenance_ticket_closed()
        mock_schedule_assigned(host, target_status, manual=False, reason=reason, unset_ticket=True, power_on=power_on)

        test.hosts.assert_equal()

    @pytest.mark.parametrize("target_status", [HostStatus.READY, HostStatus.DEAD])
    @pytest.mark.parametrize(
        "project_healing_automation_enabled,expected_power_on_value", [(True, True), (False, False)]
    )
    def test_host_in_maintenance_with_ticket_and_no_timeout_is_garbage_collected(
        self, test, target_status, project_healing_automation_enabled, expected_power_on_value
    ):
        host = self.make_maintenance_host_with_ticket(
            test, target_status, project_healing_automation_enabled=project_healing_automation_enabled
        )
        expected_reason = (
            "The 'maintenance' state requested by An unauthorized API user "
            "is expired because the ticket {} has been closed.".format(MOCK_TICKET_KEY)
        )
        self.check_host_garbage_collected(test, host, target_status, expected_reason, power_on=expected_power_on_value)

    def test_host_in_maintenance_with_open_ticket_and_no_timeout_is_not_garbage_collected(self, walle_test, mp):
        mp.function(walle.host_status.get_tickets_by_query, module=walle.host_status, return_value=[])
        self.make_maintenance_host_with_ticket(walle_test, HostStatus.READY)
        walle.host_status._gc_maintenance_ticket_closed()

        walle_test.hosts.assert_equal()

    def test_host_in_maintenance_with_ticket_and_timeout_is_not_garbage_collected(self, test):
        self.make_maintenance_host_with_ticket(test, HostStatus.READY, timeout_time=timestamp() - 1000)
        walle.host_status._gc_maintenance_ticket_closed()

        test.hosts.assert_equal()

    def test_startrek_connection_failed(self, test, mp):
        get_tickets = mp.function(
            startrek.get_tickets_by_query, module=walle.host_status, side_effect=StartrekClientError("Some error.")
        )
        st_query = walle.host_status.STARTREK_CLOSED_TICKETS_QUERY_TEMPLATE.format(ticket_keys=MOCK_TICKET_KEY)
        self.make_maintenance_host_with_ticket(test, HostStatus.READY)

        walle.host_status._gc_maintenance_ticket_closed()

        get_tickets.assert_called_once_with(st_query)
        test.hosts.assert_equal()

    def test_startrek_schema_changed(self, test, mp):
        get_tickets = mp.function(
            startrek.get_tickets_by_query, module=walle.host_status, return_value=[{"ticket": {"key": MOCK_TICKET_KEY}}]
        )
        st_query = walle.host_status.STARTREK_CLOSED_TICKETS_QUERY_TEMPLATE.format(ticket_keys=MOCK_TICKET_KEY)
        self.make_maintenance_host_with_ticket(test, HostStatus.READY)

        walle.host_status._gc_maintenance_ticket_closed()

        get_tickets.assert_called_once_with(st_query)
        test.hosts.assert_equal()

    @pytest.mark.slow
    def test_limit_tickets_processed_at_once(self, test, mp):
        get_tickets = mp.function(
            startrek.get_tickets_by_query, module=walle.host_status, return_value=[{"key": MOCK_TICKET_KEY}]
        )
        hosts = [
            self.make_maintenance_host_with_ticket(test, HostStatus.READY, inv=i, ticket_key="MOCK-{}".format(i))
            for i in range(1500)
        ]
        ticket_keys = sorted(["MOCK-{}".format(i) for i in range(1500)])
        page_size = walle.host_status.MAX_STARTREK_QUERY_TICKETS_COUNT
        st_queries = [
            walle.host_status.STARTREK_CLOSED_TICKETS_QUERY_TEMPLATE.format(
                ticket_keys=','.join(ticket_keys[:page_size])
            ),
            walle.host_status.STARTREK_CLOSED_TICKETS_QUERY_TEMPLATE.format(
                ticket_keys=','.join(ticket_keys[page_size:])
            ),
        ]
        expected_reason = (
            "The 'maintenance' state requested by An unauthorized API user "
            "is expired because the ticket {} has been closed.".format(MOCK_TICKET_KEY)
        )

        walle.host_status._gc_maintenance_ticket_closed()

        get_tickets.assert_has_calls([call(st_query) for st_query in st_queries])
        mock_schedule_assigned(hosts[1234], HostStatus.READY, manual=False, reason=expected_reason, unset_ticket=True)
        test.hosts.assert_equal()

    def test_host_in_scenario_is_not_garbage_collected(self, walle_test):
        self.make_maintenance_host_with_ticket(walle_test, HostStatus.READY, scenario_id=1)
        walle.host_status._gc_maintenance_ticket_closed()

        walle_test.hosts.assert_equal()

    def test_aliased_tickets_collection(self, test, mp):
        old_ticket_key = "OLD-1"
        new_ticket_key = "NEW-1"
        previous_alias = "ALIAS-1"
        target_status = HostStatus.READY

        get_tickets = mp.function(
            startrek.get_tickets_by_query,
            module=walle.host_status,
            return_value=[
                {"key": MOCK_TICKET_KEY},
                {"key": new_ticket_key, "aliases": [old_ticket_key, previous_alias]},
            ],
        )
        hosts = [
            self.make_maintenance_host_with_ticket(test, target_status, inv=1, ticket_key=old_ticket_key),
            self.make_maintenance_host_with_ticket(test, target_status, inv=2, ticket_key=new_ticket_key),
            self.make_maintenance_host_with_ticket(test, target_status, inv=3, ticket_key=previous_alias),
            self.make_maintenance_host_with_ticket(test, target_status, inv=4, ticket_key=MOCK_TICKET_KEY),
        ]
        walle.host_status._gc_maintenance_ticket_closed()
        get_tickets.assert_called_once_with(
            walle.host_status.STARTREK_CLOSED_TICKETS_QUERY_TEMPLATE.format(
                ticket_keys=','.join(sorted([previous_alias, MOCK_TICKET_KEY, new_ticket_key, old_ticket_key]))
            )
        )

        for host in hosts:
            ticket_key = host.state_expire.ticket if host.state_expire else host.ticket
            expected_reason = (
                "The 'maintenance' state requested by An unauthorized API user "
                "is expired because the ticket {} has been closed.".format(ticket_key)
            )
            mock_schedule_assigned(host, target_status, manual=False, reason=expected_reason, unset_ticket=True)
            host.task.task_id -= len(hosts) - 1

        test.hosts.assert_equal()


class TestHostStateLeaveMaintenanceByTicketGcWithStateExpire(HostStateLeaveMaintenanceByTicketGc):
    @staticmethod
    def make_maintenance_host_with_ticket(
        test,
        target_status,
        inv=1,
        timeout_time=None,
        ticket_key=MOCK_TICKET_KEY,
        scenario_id=None,
        project_healing_automation_enabled=False,
    ):
        project_id = "some-id"
        try:
            test.mock_project(
                {
                    "id": project_id,
                    "healing_automation": {"enabled": project_healing_automation_enabled},
                }
            )
        except NotUniqueError:
            pass

        return test.mock_host(
            drop_none(
                {
                    "inv": inv,
                    "project": "some-id",
                    "scenario_id": scenario_id,
                    "state": HostState.MAINTENANCE,
                    "status": HostStatus.MANUAL,
                    "state_time": timestamp(),
                    "state_author": authorization.ISSUER_WALLE,
                    "state_audit_log_id": AUDIT_LOG_ID,
                    "state_expire": StateExpire(
                        time=timeout_time,
                        issuer=authorization.ISSUER_ANONYMOUS_USER,
                        ticket=ticket_key,
                        status=target_status,
                    ),
                }
            )
        )


@pytest.mark.usefixtures("monkeypatch_audit_log")
class TestHostStateLeaveProbationIfSuccessfullyPrepared:
    @staticmethod
    def make_probation_host(test, mp, is_healthy_plot_check=True):
        plot = AutomationPlot(id="mock-id", name="plot", checks=[Check(name="mock_check", enabled=True)])
        plot.save()
        project = test.mock_project({"id": "mock-project", "automation_plot_id": plot.id})
        host = test.mock_host(
            {
                "status": HostStatus.READY,
                "state": HostState.PROBATION,
                "health": mock_host_health_status(),
                "project": project.id,
            }
        )

        decision_maker = get_decision_maker(project)
        decision = Decision(
            action=WalleAction.HEALTHY if is_healthy_plot_check else WalleAction.WAIT,
            reason="{}OK".format("" if is_healthy_plot_check else "Not "),
        )

        mp.method(decision_maker.make_decision, return_value=decision, obj=type(decision_maker))
        mp.function(walle.expert.dmc.get_decision_maker, return_value=decision_maker)
        mp.function(walle.expert.dmc.get_host_reasons, return_value=["reason"])

        return host

    @staticmethod
    def _make_health_status(check_type, check_status=CheckStatus.FAILED):
        return mock_host_health_status(
            status=HealthStatus.STATUS_FAILURE,
            check_status=CheckStatus.PASSED,
            check_overrides=[
                {
                    "type": check_type,
                    "status": check_status,
                    "stale_timestamp": timestamp() + 100,
                }
            ],
        )

    def test_probation_ready_healthy_is_collected(self, test, mp):
        host = self.make_probation_host(test, mp)

        walle.host_status._gc_complete_probation()

        expected_reason = "Host {} has successfully finished preparing.".format(host.name)
        mock_schedule_assigned(host, HostStatus.READY, manual=False, reason=expected_reason)

        test.hosts.assert_equal()

    def test_probation_ready_unhealthy_decision_is_not_collected(self, mp, test):
        self.make_probation_host(test, mp, is_healthy_plot_check=False)
        walle.host_status._gc_complete_probation()
        test.hosts.assert_equal()

    @pytest.mark.parametrize("health_status", [HealthStatus.STATUS_BROKEN, HealthStatus.STATUS_FAILURE])
    def test_probation_ready_unhealthy_is_not_collected(self, test, health_status):
        test.mock_host(
            {
                "status": HostStatus.READY,
                "state": HostState.PROBATION,
                "health": mock_host_health_status(status=health_status),
            }
        )
        walle.host_status._gc_complete_probation()
        test.hosts.assert_equal()

    @pytest.mark.parametrize("status", set(HostStatus.ALL_STEADY + [HostStatus.ALL_TASK[0]]) - {HostStatus.READY})
    def test_probation_not_ready_is_not_collected(self, test, status):
        test.mock_host({"status": status, "state": HostState.PROBATION})
        walle.host_status._gc_complete_probation()
        test.hosts.assert_equal()

    @pytest.mark.parametrize("state", set(HostState.ALL) - {HostState.PROBATION})
    @pytest.mark.parametrize("status", HostStatus.ALL_STEADY + [HostStatus.ALL_TASK[0]])
    def test_different_state_is_not_collected(self, test, state, status):
        test.mock_host({"status": status, "state": state})
        walle.host_status._gc_complete_probation()
        test.hosts.assert_equal()

    @pytest.mark.parametrize("check_type", walle.host_status.IGNORED_CHECKS)
    @pytest.mark.parametrize("check_status", walle.host_status.IGNORED_CHECK_STATUSES)
    def test_host_with_ignored_check_are_collected(self, walle_test, check_type, check_status):
        host_with_ignored_check = walle_test.mock_host(
            {
                "state": HostState.PROBATION,
                "status": HostStatus.READY,
                "health": self._make_health_status(check_type, check_status),
            }
        )
        walle.host_status._gc_complete_probation()
        mock_schedule_assigned(
            host_with_ignored_check,
            HostStatus.READY,
            manual=False,
            reason="Host {} has successfully finished preparing.".format(host_with_ignored_check.name),
        )
        walle_test.hosts.assert_equal()

    def test_host_with_meta_missing_is_collected(self, walle_test):
        host_with_ignored_check = walle_test.mock_host(
            {
                "state": HostState.PROBATION,
                "status": HostStatus.READY,
                "health": self._make_health_status(CheckType.META, CheckStatus.MISSING),
            }
        )
        walle.host_status._gc_complete_probation()
        mock_schedule_assigned(
            host_with_ignored_check,
            HostStatus.READY,
            manual=False,
            reason="Host {} has successfully finished preparing.".format(host_with_ignored_check.name),
        )
        walle_test.hosts.assert_equal()

    @pytest.mark.parametrize("check_status", set(CheckStatus.ALL) - {CheckStatus.MISSING})
    def test_host_with_meta_not_missing_is_not_collected(self, walle_test, check_status):
        walle_test.mock_host(
            {
                "state": HostState.PROBATION,
                "status": HostStatus.READY,
                "health": self._make_health_status(CheckType.META, check_status),
            }
        )
        walle.host_status._gc_complete_probation()
        walle_test.hosts.assert_equal()

    def test_hosts_with_ignored_checks_are_collected_and_hosts_with_not_ignored_checks_stay_put(self, walle_test):
        only_ignored = walle_test.mock_host(
            {
                "inv": 1,
                "state": HostState.PROBATION,
                "status": HostStatus.READY,
                "health": self._make_health_status(CheckType.NETMON),
            }
        )
        walle_test.mock_host(
            {
                "inv": 3,
                "state": HostState.PROBATION,
                "status": HostStatus.READY,
                "health": self._make_health_status(CheckType.DISK),
            }
        )
        walle_test.mock_host(
            {
                "inv": 4,
                "state": HostState.PROBATION,
                "status": HostStatus.READY,
                "health": mock_host_health_status(
                    status=HealthStatus.STATUS_FAILURE,
                    check_status=CheckStatus.PASSED,
                    check_overrides=[
                        {
                            "type": CheckType.NETMON,
                            "status": CheckStatus.FAILED,
                            "stale_timestamp": timestamp() + 100,
                        },
                        {
                            "type": CheckType.DISK,
                            "status": CheckStatus.FAILED,
                            "stale_timestamp": timestamp() + 100,
                        },
                    ],
                ),
            }
        )
        walle.host_status._gc_complete_probation()

        mock_schedule_assigned(
            only_ignored,
            HostStatus.READY,
            manual=False,
            reason="Host {} has successfully finished preparing.".format(only_ignored.name),
        )

        walle_test.hosts.assert_equal()
