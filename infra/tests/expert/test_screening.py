"""Tests host healing."""

import mock
import pytest

from infra.walle.server.tests.lib.util import (
    mock_status_reasons,
    mock_host_health_status,
    mock_location,
)
from sepelib.core.exceptions import Error
from walle.expert import screening, juggler, decisionmakers
from walle.expert.decision import Decision
from walle.expert.decisionmakers import get_decision_maker
from walle.expert.types import WalleAction, CheckType, CheckStatus, CheckSets, get_walle_check_type
from walle.hosts import HostState, HostStatus, HealthStatus, DecisionStatus, Decision as DecisionDocument, HostMessage
from walle.models import monkeypatch_timestamp, timestamp
from walle.operations_log.constants import Operation
from walle.util import mongo


class AnyHostBulkUpdater:
    def __eq__(self, other):
        return isinstance(other, screening.HostBulkUpdater)


def set_health_decision(host, decision, health_status, message, decision_timestamp=None):
    if message:
        host.messages["dmc"] = [HostMessage.info(message)]
    elif "dmc" in host.messages:
        del host.messages["dmc"]

    host.health.decision = DecisionDocument(**decision.to_dict())

    host.decision_status_timestamp = timestamp() if decision_timestamp is None else decision_timestamp

    if health_status == HealthStatus.STATUS_FAILURE:
        host.decision_status = DecisionStatus.FAILURE
    else:
        host.decision_status = DecisionStatus.HEALTHY


def receive_health():
    screening_processor = screening.ScreeningShardProcessor(1)
    screening_processor._receive_health(mongo.MongoPartitionerShard("0", mock.Mock()))


@pytest.mark.usefixtures("enable_basic_automation_plot")
def test_screening_fetches_all_enabled_checks_for_basic_automation_plot(monkeypatch_screening):
    enabled_checks = CheckSets.BASIC
    receive_health()
    assert set(monkeypatch_screening) == enabled_checks

    for check_type in enabled_checks:
        assert monkeypatch_screening[check_type].get("metadata") is not None, "Check {} must contain metadata".format(
            check_type
        )
        assert monkeypatch_screening[check_type]["status"] == CheckStatus.FAILED, "Check {} status changed".format(
            check_type
        )


@pytest.mark.usefixtures("enable_modern_automation_plot")
def test_screening_fetches_all_enabled_checks_for_modern_automation_plot(monkeypatch_screening):
    enabled_checks = CheckSets.FULL_FEATURED
    receive_health()

    assert set(monkeypatch_screening) == enabled_checks

    for check_type in enabled_checks:
        assert monkeypatch_screening[check_type].get("metadata") is not None, "Check {} must contain metadata".format(
            check_type
        )
        assert monkeypatch_screening[check_type]["status"] == CheckStatus.FAILED, "Check {} status changed".format(
            check_type
        )


@pytest.mark.usefixtures("fake_automation_plot")
def test_screening_fetches_all_enabled_checks_for_custom_automation_plot(monkeypatch_screening, custom_checks):
    enabled_checks = CheckSets.FULL_FEATURED | {custom_checks.CUSTOM_CHECK_NAME_1, custom_checks.CUSTOM_CHECK_NAME_2}

    # our mock data does not contain this check exactly to test this thing.
    missing_checks = {custom_checks.CUSTOM_CHECK_NAME_2}

    checks_with_metadata = enabled_checks.difference(
        CheckType.ALL_INFRASTRUCTURE, CheckType.ALL_ACTIVE, {CheckType.META}, missing_checks
    )

    got_checks = monkeypatch_screening
    receive_health()

    assert got_checks.keys() == enabled_checks

    for check_type in checks_with_metadata:
        assert got_checks[check_type].get("metadata") is not None, "Check {} must contain metadata".format(check_type)


@pytest.mark.usefixtures("enable_modern_automation_plot")
def test_screening_keep_eine_code_from_hw_watcher(monkeypatch_screening):
    enabled_checks = CheckSets.FULL_FEATURED
    receive_health()

    assert set(monkeypatch_screening) == enabled_checks

    def get_metadata(reasons):
        for check_type_ in CheckType.ALL_HW_WATCHER:
            metadata = reasons[check_type_]["metadata"]
            if check_type_ == CheckType.MEMORY:
                # this check is composed of two
                for subcheck in ("mem", "ecc"):
                    check = metadata["results"][subcheck]
                    yield check_type_, check
            else:
                yield check_type_, metadata["result"]

    for check_type, check_metadata in get_metadata(monkeypatch_screening):
        eine_code = check_metadata.get("eine_code")
        assert eine_code is not None, "Check {} metadata must contain eine_code from hw_watcher".format(check_type)


def mock_host_health_timestamp(reasons=None):
    checks = CheckType.ALL
    cur_time = timestamp()

    if reasons is None:
        reasons = mock_status_reasons()

    check_statuses = {}
    for check_type, check in reasons.items():
        check_statuses[get_walle_check_type(check_type)] = check["status"]

    for check in checks:
        if get_walle_check_type(check) not in check_statuses:
            check_statuses[get_walle_check_type(check)] = CheckStatus.VOID

    return cur_time


@pytest.mark.usefixtures("monkeypatch_timestamp")
@pytest.mark.parametrize(
    ["old_status", "new_status"],
    [
        (None, HealthStatus.STATUS_OK),
        (None, HealthStatus.STATUS_FAILURE),
        (HealthStatus.STATUS_OK, HealthStatus.STATUS_FAILURE),
    ],
)
def test_screening_refreshes_health_status(walle_test, mp, old_status, new_status):
    # add ballast host
    host_1 = walle_test.mock_host({"inv": 0, "state": HostState.ASSIGNED, "status": HostStatus.READY})
    host_2 = walle_test.mock_host({"inv": 1, "state": HostState.ASSIGNED, "status": HostStatus.READY})

    if old_status:
        host_2.health = mock_host_health_status(status=old_status)

    new_reasons = mock_status_reasons(status=new_status)
    new_health = mock_host_health_status(status=new_status, reasons=new_reasons).to_mongo(use_db_field=False)
    host_health = HealthStatus(**new_health)

    get_hosts_statuses = mp.function(
        juggler.get_health_for_hosts,
        return_value={
            host_1.name: juggler.HostHealth(HealthStatus(**new_health), new_reasons, mock_host_health_timestamp(), []),
            host_2.name: juggler.HostHealth(HealthStatus(**new_health), new_reasons, mock_host_health_timestamp(), []),
        },
    )

    receive_health()

    decision_maker = get_decision_maker(walle_test.default_project)
    decision = decision_maker.make_decision(host_1, new_reasons)

    host_decision_makers = {
        host_1.name: decision_maker,
        host_2.name: decision_maker,
    }

    host_1.health = host_health
    host_2.health = host_health
    message = "Host is healthy." if new_status == HealthStatus.STATUS_OK else None
    set_health_decision(host_1, decision, new_status, message)
    set_health_decision(host_2, decision, new_status, message)

    assert get_hosts_statuses.mock_calls == [mock.call(host_decision_makers)]

    walle_test.hosts.assert_equal()


@pytest.mark.usefixtures("monkeypatch_timestamp")
@pytest.mark.parametrize(
    ["prev_action", "new_action", "health_status"],
    [
        (WalleAction.HEALTHY, WalleAction.HEALTHY, HealthStatus.STATUS_OK),
        (WalleAction.HEALTHY, WalleAction.WAIT, HealthStatus.STATUS_OK),
        (WalleAction.WAIT, WalleAction.HEALTHY, HealthStatus.STATUS_OK),
        (WalleAction.DEACTIVATE, WalleAction.WAIT, HealthStatus.STATUS_FAILURE),
        (WalleAction.WAIT, WalleAction.DEACTIVATE, HealthStatus.STATUS_FAILURE),
        (WalleAction.REBOOT, WalleAction.DEACTIVATE, HealthStatus.STATUS_FAILURE),
        (WalleAction.REDEPLOY, WalleAction.REDEPLOY, HealthStatus.STATUS_FAILURE),
    ],
)
def test_keeps_decision_timestamp_when_decision_action_does_not_change(
    walle_test, mp, prev_action, new_action, health_status
):
    host = walle_test.mock_host(
        {
            "inv": 0,
            "state": HostState.ASSIGNED,
            "status": HostStatus.READY,
            "health": mock_host_health_status().to_mongo(),
        }
    )
    set_health_decision(
        host,
        Decision(prev_action, reason="reason mock"),
        health_status,
        "DMC message mock.",
        decision_timestamp=timestamp() - 3600,
    )
    host.save()

    new_reasons = mock_status_reasons(status=health_status)
    new_health_kwargs = mock_host_health_status(status=health_status, reasons=new_reasons).to_mongo(use_db_field=False)
    mp.function(
        juggler.get_health_for_hosts,
        return_value={
            host.name: juggler.HostHealth(
                HealthStatus(**new_health_kwargs), new_reasons, mock_host_health_timestamp(reasons=new_reasons), []
            ),
        },
    )

    decision_maker = get_decision_maker(walle_test.default_project)
    decision = Decision(new_action, reason="reason mock")
    mp.method(decision_maker.make_decision, return_value=decision, obj=type(decision_maker))

    receive_health()

    host.health = HealthStatus(**new_health_kwargs)

    # either keep old message or set a "healthy" message
    message = "Host is healthy." if health_status == HealthStatus.STATUS_OK else "DMC message mock."
    set_health_decision(host, decision, health_status, message, decision_timestamp=timestamp() - 3600)

    walle_test.hosts.assert_equal()


@pytest.mark.usefixtures("monkeypatch_timestamp")
@pytest.mark.parametrize(
    ["prev_action", "new_action", "prev_status", "new_status"],
    [
        (WalleAction.HEALTHY, WalleAction.REBOOT, HealthStatus.STATUS_OK, HealthStatus.STATUS_FAILURE),
        (WalleAction.REBOOT, WalleAction.HEALTHY, HealthStatus.STATUS_FAILURE, HealthStatus.STATUS_OK),
    ],
)
def test_bumps_decision_timestamp_when_decision_action_changes(
    walle_test, mp, prev_action, new_action, prev_status, new_status
):
    host = walle_test.mock_host(
        {"inv": 0, "state": HostState.ASSIGNED, "status": HostStatus.READY, "health": mock_host_health_status()}
    )
    set_health_decision(
        host,
        Decision(prev_action, reason="reason mock"),
        prev_status,
        "DMC message mock.",
        decision_timestamp=timestamp() - 3600,
    )
    host.save()

    new_reasons = mock_status_reasons(status=new_status)
    new_health_kwargs = mock_host_health_status(status=new_status, reasons=new_reasons).to_mongo(use_db_field=False)
    mp.function(
        juggler.get_health_for_hosts,
        return_value={
            host.name: juggler.HostHealth(
                HealthStatus(**new_health_kwargs),
                new_reasons,
                mock_host_health_timestamp(reasons=new_reasons),
                [],
            ),
        },
    )

    decision_maker = get_decision_maker(walle_test.default_project)
    decision = Decision(new_action, reason="reason mock")
    mp.method(decision_maker.make_decision, return_value=decision, obj=type(decision_maker))

    receive_health()

    host.health = HealthStatus(**new_health_kwargs)
    # either keep old message or set a "healthy" message
    message = "Host is healthy." if new_status == HealthStatus.STATUS_OK else "DMC message mock."
    set_health_decision(host, decision, new_status, message, decision_timestamp=timestamp())

    walle_test.hosts.assert_equal()


@pytest.mark.usefixtures("monkeypatch_timestamp")
@pytest.mark.parametrize(
    ["new_action", "new_status"],
    [
        (WalleAction.REBOOT, HealthStatus.STATUS_FAILURE),
        (WalleAction.HEALTHY, HealthStatus.STATUS_OK),
        (WalleAction.WAIT, HealthStatus.STATUS_OK),
    ],
)
@pytest.mark.parametrize("prev_health", [True, False])
def test_sets_decision_when_no_prev_decision_existed(walle_test, mp, prev_health, new_action, new_status):
    if prev_health:
        old_host_health = mock_host_health_status().to_mongo()
    else:
        old_host_health = None

    host = walle_test.mock_host(
        {"inv": 0, "state": HostState.ASSIGNED, "status": HostStatus.READY, "health": old_host_health}
    )

    new_reasons = mock_status_reasons(status=new_status)
    new_health_kwargs = mock_host_health_status(status=new_status, reasons=new_reasons).to_mongo(use_db_field=False)
    mp.function(
        juggler.get_health_for_hosts,
        return_value={
            host.name: juggler.HostHealth(
                HealthStatus(**new_health_kwargs),
                new_reasons,
                mock_host_health_timestamp(reasons=new_reasons),
                [],
            ),
        },
    )

    decision_maker = get_decision_maker(walle_test.default_project)
    decision = Decision(new_action, reason="reason mock")
    mp.method(decision_maker.make_decision, return_value=decision, obj=type(decision_maker))

    receive_health()

    host.health = HealthStatus(**new_health_kwargs)
    # either keep old message (no message) or set a "healthy" message
    message = "Host is healthy." if new_status == HealthStatus.STATUS_OK else None
    set_health_decision(host, decision, new_status, message, decision_timestamp=timestamp())

    walle_test.hosts.assert_equal()


@pytest.mark.usefixtures("monkeypatch_timestamp")
@pytest.mark.parametrize(
    ["prev_action", "prev_status", "new_health_status"],
    [
        (WalleAction.REBOOT, HealthStatus.STATUS_FAILURE, HealthStatus.STATUS_FAILURE),
        (WalleAction.REBOOT, HealthStatus.STATUS_FAILURE, HealthStatus.STATUS_OK),
        (WalleAction.HEALTHY, HealthStatus.STATUS_OK, HealthStatus.STATUS_FAILURE),
        (WalleAction.WAIT, HealthStatus.STATUS_OK, HealthStatus.STATUS_OK),
        (WalleAction.WAIT, HealthStatus.STATUS_OK, HealthStatus.STATUS_FAILURE),
    ],
)
def test_keeps_previous_decision_when_no_new_decision_available(
    walle_test, mp, prev_action, prev_status, new_health_status
):
    host = walle_test.mock_host(
        {
            "inv": 0,
            "state": HostState.ASSIGNED,
            "status": Operation.REBOOT.host_status,
            "health": mock_host_health_status(),
        }
    )
    prev_decision = Decision(prev_action, reason="reason mock")
    set_health_decision(host, prev_decision, prev_status, "DMC message mock.", decision_timestamp=timestamp() - 3600)
    host.save()

    new_reasons = mock_status_reasons(status=new_health_status)
    new_health_kwargs = mock_host_health_status(status=new_health_status, reasons=new_reasons).to_mongo(
        use_db_field=False
    )
    mp.function(
        juggler.get_health_for_hosts,
        return_value={
            host.name: juggler.HostHealth(
                HealthStatus(**new_health_kwargs),
                new_reasons,
                mock_host_health_timestamp(reasons=new_reasons),
                [],
            ),
        },
    )

    receive_health()

    host.health = HealthStatus(**new_health_kwargs)
    # either keep old message or set a "healthy" message
    message = "Host is healthy." if prev_status == HealthStatus.STATUS_OK else "DMC message mock."
    set_health_decision(host, prev_decision, prev_status, message, decision_timestamp=timestamp() - 3600)

    walle_test.hosts.assert_equal()


@pytest.mark.usefixtures("monkeypatch_timestamp")
def test_screening_clears_old_data_when_no_new_data_available(walle_test, mp):
    # add ballast host
    host_1 = walle_test.mock_host({"inv": 0, "state": HostState.ASSIGNED, "status": HostStatus.READY})
    host_2 = walle_test.mock_host(
        {"inv": 1, "state": HostState.ASSIGNED, "status": HostStatus.READY, "health": mock_host_health_status()}
    )

    new_reasons = mock_status_reasons(status=HealthStatus.STATUS_FAILURE)
    new_health_kwargs = mock_host_health_status(reasons=new_reasons).to_mongo()
    get_hosts_statuses = mp.function(
        juggler.get_health_for_hosts,
        return_value={
            host_1.name: juggler.HostHealth(
                HealthStatus(**new_health_kwargs), new_reasons, mock_host_health_timestamp(reasons=new_reasons), []
            ),
        },
    )

    receive_health()

    decision_maker = get_decision_maker(walle_test.default_project)
    decision = decision_maker.make_decision(host_1, new_reasons)
    host_decision_makers = {host_1.name: decision_maker, host_2.name: decision_maker}

    host_status = HealthStatus(**new_health_kwargs)
    host_1.health = host_status
    set_health_decision(host_1, decision, HealthStatus.STATUS_FAILURE, message=None)
    del host_2.health

    assert get_hosts_statuses.mock_calls == [mock.call(host_decision_makers)]

    walle_test.hosts.assert_equal()


def test_screening_with_error_clears_old_health_statuses(walle_test, mp):
    monkeypatch_timestamp(mp)
    mp.function(juggler.get_health_for_hosts, side_effect=Error("Mocked Expert error"))

    outdated_host = walle_test.mock_host(
        {"inv": 2, "state": HostState.ASSIGNED, "status": HostStatus.READY, "health": mock_host_health_status()}
    )

    receive_health()
    del outdated_host.health
    walle_test.hosts.assert_equal()


@pytest.mark.usefixtures("monkeypatch_timestamp")
@pytest.mark.parametrize(
    ["prev_action", "new_action", "prev_status", "new_status"],
    [
        (WalleAction.HEALTHY, WalleAction.REBOOT, HealthStatus.STATUS_OK, HealthStatus.STATUS_FAILURE),
        (WalleAction.REBOOT, WalleAction.HEALTHY, HealthStatus.STATUS_FAILURE, HealthStatus.STATUS_OK),
    ],
)
def test_screening_refreshes_health_status_according_dmc_rule(
    walle_test, mp, prev_action, new_action, prev_status, new_status
):
    location1 = dict(country="COUNTRY", city="CITY", datacenter="DATACENTER", queue="QUEUE", rack="RACK1")

    location2 = dict(country="COUNTRY", city="CITY", datacenter="DATACENTER", queue="QUEUE", rack="RACK2")

    walle_test.dmc_rules.mock({"rule_query": {"physical_locations_excluded": ["COUNTRY|CITY|DATACENTER|QUEUE|RACK2"]}})

    host1 = walle_test.mock_host(
        {
            "inv": 0,
            "state": HostState.ASSIGNED,
            "status": HostStatus.READY,
            "health": mock_host_health_status(),
            "location": mock_location(**location1),
        }
    )
    set_health_decision(
        host1,
        Decision(prev_action, reason="reason mock"),
        prev_status,
        "DMC message mock.",
        decision_timestamp=timestamp() - 3600,
    )
    host1.save()

    host2 = walle_test.mock_host(
        {
            "inv": 1,
            "state": HostState.ASSIGNED,
            "status": HostStatus.READY,
            "health": mock_host_health_status(),
            "location": mock_location(**location2),
        }
    )

    set_health_decision(
        host2,
        Decision(prev_action, reason="reason mock"),
        prev_status,
        "DMC message mock.",
        decision_timestamp=timestamp() - 3600,
    )
    host2.save()

    new_reasons = mock_status_reasons(status=new_status)
    new_health_kwargs = mock_host_health_status(status=new_status, reasons=new_reasons).to_mongo(use_db_field=False)
    mp.function(
        juggler.get_health_for_hosts,
        return_value={
            host1.name: juggler.HostHealth(
                HealthStatus(**new_health_kwargs),
                new_reasons,
                mock_host_health_timestamp(reasons=new_reasons),
                [],
            ),
        },
    )

    decision_maker = get_decision_maker(walle_test.default_project)
    decision = Decision(new_action, reason="reason mock")
    mp.method(decision_maker.make_decision, return_value=decision, obj=type(decision_maker))

    receive_health()

    host1.health = HealthStatus(**new_health_kwargs)
    # either keep old message or set a "healthy" message
    message = "Host is healthy." if new_status == HealthStatus.STATUS_OK else "DMC message mock."
    set_health_decision(host1, decision, new_status, message, decision_timestamp=timestamp())

    walle_test.hosts.assert_equal()
