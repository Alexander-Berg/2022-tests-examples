import mock
import mongoengine

from infra.walle.server.tests.expert.util import monkeypatch_health_data, mock_host, run_triage
from walle.expert import triage, juggler
from walle.expert.decision import Decision
from walle.expert.decisionmakers import AbstractDecisionMaker, get_decision_maker
from walle.expert.types import WalleAction
from walle.hbf_drills import HbfDrillsCollection
from walle.hosts import HostState, HostStatus, DecisionStatus, HostMessage
from walle.operations_log.constants import Operation


class AnyOfClass:
    def __init__(self, matching_class):
        self.matching_class = matching_class

    def __eq__(self, other):
        return isinstance(other, self.matching_class)

    def __ne__(self, other):
        return not isinstance(other, self.matching_class)


def test_only_hosts_with_failure_decisions_processed(mp, walle_test, mock_health_data):
    host = mock_host(walle_test, 0, Decision(WalleAction.REBOOT, "host is dead"), DecisionStatus.FAILURE)
    mock_host(walle_test, 1, Decision(WalleAction.HEALTHY, "host is healthy"), DecisionStatus.HEALTHY)
    monkeypatch_health_data(mp, [host], mock_health_data)

    processor = triage.TriageShardProcessor(1)
    mock_process = mp.method(processor._process_and_save_message)

    run_triage(processor)

    mock_process.assert_called_once_with(
        host, mock.ANY, mock.ANY, AnyOfClass(AbstractDecisionMaker), AnyOfClass(HbfDrillsCollection)
    )


def test_only_steady_and_assigned_hosts_processed(mp, walle_test, mock_health_data):
    # free host should not be processed
    inv = 1
    mock_host(
        walle_test,
        inv=inv,
        decision=Decision(WalleAction.REBOOT, "host is dead"),
        decision_status=DecisionStatus.FAILURE,
        state=HostState.FREE,
        status=HostStatus.READY,
    )

    # steady hosts should be processed
    steady_hosts = []
    for inv, status in enumerate(HostStatus.ALL_STEADY, start=inv + 1):
        steady_hosts.append(
            mock_host(
                walle_test,
                inv=inv,
                decision=Decision(WalleAction.REBOOT, "host is dead"),
                decision_status=DecisionStatus.FAILURE,
                state=HostState.ASSIGNED,
                status=status,
            )
        )

    excluded_statuses = [HostStatus.INVALID, Operation.REBOOT.host_status]
    for inv, status in enumerate(excluded_statuses, start=inv + 1):
        mock_host(
            walle_test,
            inv=inv,
            decision=Decision(WalleAction.REBOOT, "host is dead"),
            decision_status=DecisionStatus.FAILURE,
            state=HostState.ASSIGNED,
            status=status,
        )

    monkeypatch_health_data(mp, steady_hosts, mock_health_data)
    processed_hosts = []

    def mock_process_with_dmc(host, reasons, go_host_decision, decision_maker, hbf_drills):
        processed_hosts.append(host.inv)

    processor = triage.TriageShardProcessor(1)
    mp.method(processor._process_and_save_message, side_effect=mock_process_with_dmc)

    run_triage(processor)

    assert sorted(host.inv for host in steady_hosts) == sorted(processed_hosts)


def test_dead_hosts_with_healthy_decisions_are_processed(mp, walle_test, mock_health_data):
    # dead healthy hosts should be processed
    dead_host = None
    for inv, status in enumerate(HostStatus.ALL_STEADY, start=1):
        host = mock_host(
            walle_test,
            inv=inv,
            status=status,
            decision=Decision(WalleAction.HEALTHY, "host is healthy"),
            decision_status=DecisionStatus.HEALTHY,
            state=HostState.ASSIGNED,
        )
        if status == HostStatus.DEAD:
            dead_host = host

    monkeypatch_health_data(mp, [dead_host], mock_health_data)
    processed_hosts = []

    def mock_process_with_dmc(host, reasons, go_host_decision, decision_maker, hbf_drills):
        processed_hosts.append(host.inv)

    processor = triage.TriageShardProcessor(1)
    mp.method(processor._process_and_save_message, side_effect=mock_process_with_dmc)

    run_triage(processor)

    assert [dead_host.inv] == processed_hosts


def test_saves_dmc_message_when_not_performing_action(mp, walle_test, mock_health_data, monkeypatch_locks):
    project = walle_test.default_project
    project.healing_automation.enabled = False
    project.save()

    host = mock_host(walle_test, 0, Decision(WalleAction.REBOOT, "host is dead"), DecisionStatus.FAILURE)
    monkeypatch_health_data(mp, [host], mock_health_data)

    decision_maker = get_decision_maker(walle_test.default_project)
    decision = Decision(WalleAction.REBOOT, reason="host is dead")
    mp.method(decision_maker.make_decision, return_value=decision, obj=type(decision_maker))

    run_triage()

    host.messages["dmc"] = [
        HostMessage.info(
            "Host failure has not been processed: automated healing is disabled for mocked-default-project project."
        )
    ]
    walle_test.hosts.assert_equal()


# class TestHBFDrillHostsExclusion:
#     def test_dmc_is_not_called_if_host_is_excluded(self, mp, walle_test, mock_health_data, monkeypatch_locks):
#         host = mock_host(walle_test, 0, Decision(WalleAction.REBOOT, "host is dead"), DecisionStatus.FAILURE)
#         monkeypatch_health_data(mp, [host], mock_health_data)
#         decision_maker = get_decision_maker(walle_test.default_project)
#         decision = Decision(WalleAction.REBOOT, reason="host is dead")
#         mp.method(decision_maker.make_decision, return_value=decision, obj=type(decision_maker))
#
#         exclusion_reason = "host is excluded because of HBF drill"
#         mp.method(HbfDrillsCollection.get_host_inclusion_reason, return_value=exclusion_reason, obj=HbfDrillsCollection)
#         process_with_dmc_mock = mp.function(triage._process_with_dmc)
#
#         run_triage()
#
#         host.messages["dmc"] = [
#             HostMessage.info(
#                 "Host mocked-0.mock is temporarily excluded from health processing: {}".format(exclusion_reason)
#             )
#         ]
#         assert not process_with_dmc_mock.called
#         walle_test.hosts.assert_equal()


class MockGetHostIterator:
    is_reset = False

    def __call__(self, *args, **kwargs):
        hosts = list(triage._get_hosts_by_query(mongoengine.Q(), full=False))
        yield hosts[0]
        if not self.is_reset:
            self.is_reset = True
            yield hosts[1]


def run_test_triage():
    decision_makers = triage._fetch_host_decision_makers(mongoengine.Q())
    return list(triage._fetch_hosts_and_health_for_decision_makers(decision_makers))


def create_mock_hosts(walle_test):
    project = walle_test.default_project
    project.healing_automation.enabled = False
    project.save()

    for inv, status in enumerate(HostStatus.ALL_STEADY, start=1):
        (
            mock_host(
                walle_test,
                inv=inv,
                decision=Decision(WalleAction.REBOOT, "host is dead"),
                decision_status=DecisionStatus.FAILURE,
                state=HostState.ASSIGNED,
                status=status,
            )
        )


def patch_functions_with_health_data(mp, monkeypatch):
    monkeypatch.setattr(triage, '_get_host_iterator', MockGetHostIterator())

    mp.function(juggler.get_health_status_reasons, return_value=None)

    mp.function(juggler._fetch_health_data, side_effect=lambda keys: [{key: None} for key in keys])
    mp.function(
        juggler._decode_host_health_data, side_effect=lambda host_status, _: (next(iter(host_status.keys())), [])
    )


def test_fetch_hosts_and_health_for_decision_makers_with_updated_hosts(mp, monkeypatch, walle_test, mock_health_data):
    create_mock_hosts(walle_test)
    patch_functions_with_health_data(mp, monkeypatch)

    result = run_test_triage()

    assert {element[0].name for element in result} == {"mocked-1.mock"}
