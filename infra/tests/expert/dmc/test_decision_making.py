"""Test decision making, check that rules are combined properly."""
from collections import defaultdict

import pytest
from mock import Mock

from infra.walle.server.tests.expert.dmc.util import break_check, fail_check
from infra.walle.server.tests.lib.util import (
    mock_status_reasons,
    monkeypatch_expert,
    TestCase,
    monkeypatch_automation_plot_id,
    monkeypatch_config,
    monkeypatch_function,
    MockRule,
)
from walle.expert import decisionmakers
from walle.expert.automation_plot import AUTOMATION_PLOT_FULL_FEATURED_ID
from walle.expert.constants import NETWORK_CHECKS_REACTION_TIMEOUT
from walle.expert.decision import Decision
from walle.expert.decisionmakers import get_decision_maker, ModernDecisionMaker
from walle.expert.dmc import get_host_reasons
from walle.expert.rules import utils
from walle.expert.types import WalleAction, CheckType, CheckStatus, get_walle_check_type
from walle.hosts import HostState, HealthStatus, HostStatus, HostType
from walle.models import timestamp


@pytest.fixture
def test(request, monkeypatch, monkeypatch_timestamp, monkeypatch_check_percentage, mp_juggler_source):
    monkeypatch_automation_plot_id(monkeypatch, AUTOMATION_PLOT_FULL_FEATURED_ID)
    monkeypatch_expert(monkeypatch, enabled=True)
    return TestCase.create(request, healthdb=True)


def _make_decision(test, host, reasons, check_type, checks_for_use=None):
    decision_maker = get_decision_maker(test.default_project)
    return decision_maker.make_alternate_decision(host, reasons, {check_type}, checks_for_use=checks_for_use)


def _make_decision_trace(test, host, reasons):
    decision_maker = get_decision_maker(test.default_project)
    return decision_maker.make_decision_trace(host, reasons)


def test_modern_automation_plot_has_all_known_checks_enabled(test):
    decision_maker = get_decision_maker(test.default_project)

    # this automation plot uses our own WALLE_META check instead of the old META
    assert set(decision_maker.checks) == set(CheckType.ALL) - ({CheckType.META} | set(CheckType.ALL_IB))


class TestMakeAlternateDecision:
    @pytest.mark.parametrize(
        "check_type",
        set(CheckType.ALL_ACTIVE + CheckType.ALL_PASSIVE) - (set(CheckType.ALL_META) | set(CheckType.ALL_IB)),
    )
    @pytest.mark.parametrize(
        "broken_check_status",
        sorted(set(CheckStatus.ALL_MISSING) | set(CheckStatus.ALL_IGNORED) | {CheckStatus.INVALID, CheckStatus.UNSURE}),
    )
    def test_required_checks_missing_and_active_checks_passing(self, test, check_type, broken_check_status):
        """Required check is not available but host is alive. Wait for required check."""
        host = test.mock_host({"state": HostState.ASSIGNED}, save=False)

        reasons = mock_status_reasons(effective_timestamp=timestamp(), check_min_time=timestamp() - 1)
        break_check(reasons, check_type, broken_check_status)

        decision = _make_decision(test, host, reasons, check_type)
        assert decision.action == WalleAction.WAIT
        assert decision.checks == [check_type]
        assert decision.reason in {
            msg.format(check_name=get_walle_check_type(check_type), status=broken_check_status)
            for msg in (
                "No data for {check_name} check result.",
                "Wall-E has not received {check_name} check.",
                "{check_name} check is not fresh enough.",
                "{check_name} check has invalid metadata.",
                "{check_name} check is {status}.",
                "Host is suspected to be unavailable: {check_name}.",
            )
        }

    @pytest.mark.parametrize(
        "check_type",
        set(CheckType.ALL_ACTIVE + CheckType.ALL_PASSIVE) - (set(CheckType.ALL_META) | set(CheckType.ALL_IB)),
    )
    def test_required_checks_missing_and_active_checks_are_unsure(self, test, check_type):
        """Required check is not available host state is unknown. Wait for active checks first."""
        host = test.mock_host(save=False)

        check_min_time = timestamp()
        reasons = mock_status_reasons(effective_timestamp=check_min_time - 1, check_min_time=check_min_time)
        del reasons[check_type]

        if check_type == CheckType.SSH:
            expected_message = "Wall-E has not received {} check.".format(get_walle_check_type(CheckType.SSH))
        elif check_type in CheckType.ALL_ACTIVE:
            expected_message = "{} check is not fresh enough.".format(get_walle_check_type(CheckType.SSH))
        else:
            expected_message = (
                "Wall-E has not received {} check. "
                "But active checks are not passed yet: "
                "{} check is not fresh enough.".format(
                    get_walle_check_type(check_type), get_walle_check_type(CheckType.SSH)
                )
            )

        decision = _make_decision(test, host, reasons, check_type)
        assert decision.action == WalleAction.WAIT
        assert decision.checks == [CheckType.SSH]
        assert decision.reason == expected_message

    @pytest.mark.parametrize(
        "check_type",
        set(CheckType.ALL_ACTIVE + CheckType.ALL_PASSIVE) - (set(CheckType.ALL_META) | set(CheckType.ALL_IB)),
    )
    def test_required_checks_staled_and_active_checks_are_unsure(self, test, check_type):
        """Required check is not available host state is unknown. Wait for active checks first."""
        host = test.mock_host(save=False)

        check_min_time = timestamp()
        reasons = mock_status_reasons(effective_timestamp=check_min_time - 1, check_min_time=check_min_time)
        reasons[check_type]["status"] = CheckStatus.STALED

        if check_type == CheckType.SSH:
            expected_message = "{} check is staled.".format(get_walle_check_type(CheckType.SSH))
        elif check_type in CheckType.ALL_ACTIVE:
            expected_message = "{} check is not fresh enough.".format(get_walle_check_type(CheckType.SSH))
        else:
            expected_message = (
                "{} check is staled. "
                "But active checks are not passed yet: "
                "{} check is not fresh enough.".format(
                    get_walle_check_type(check_type), get_walle_check_type(CheckType.SSH)
                )
            )

        decision = _make_decision(test, host, reasons, check_type)
        assert decision.action == WalleAction.WAIT
        assert decision.checks == [CheckType.SSH]
        assert decision.reason == expected_message

    @pytest.mark.parametrize(
        "check_type",
        set(CheckType.ALL_ACTIVE + CheckType.ALL_PASSIVE) - (set(CheckType.ALL_META) | set(CheckType.ALL_IB)),
    )
    def test_required_checks_unsure_and_active_checks_are_unsure(self, test, check_type):
        """Required check is not available host state is unknown. Wait for active checks first."""
        host = test.mock_host(save=False)

        check_min_time = timestamp()
        reasons = mock_status_reasons(effective_timestamp=check_min_time - 1, check_min_time=check_min_time)

        if check_type in CheckType.ALL_ACTIVE:
            expected_message = "{} check is not fresh enough.".format(get_walle_check_type(CheckType.SSH))
        else:
            expected_message = (
                "{} check is not fresh enough. "
                "But active checks are not passed yet: "
                "{} check is not fresh enough.".format(
                    get_walle_check_type(check_type), get_walle_check_type(CheckType.SSH)
                )
            )

        decision = _make_decision(test, host, reasons, check_type)
        assert decision.action == WalleAction.WAIT
        assert decision.checks == [CheckType.SSH]
        assert decision.reason == expected_message

    @pytest.mark.parametrize(
        "check_type",
        set(CheckType.ALL_ACTIVE + CheckType.ALL_PASSIVE)
        - (set(CheckType.ALL_META) | {CheckType.GPU} | set(CheckType.ALL_IB)),
    )
    @pytest.mark.parametrize("failing_check", CheckType.ALL_ACTIVE)
    @pytest.mark.parametrize(
        "broken_check_status",
        sorted(set(CheckStatus.ALL_MISSING) | set(CheckStatus.ALL_IGNORED) | {CheckStatus.INVALID, CheckStatus.UNSURE}),
    )
    def test_required_checks_missing_and_active_check_failing(
        self, test, check_type, failing_check, broken_check_status
    ):
        """Required check is not available host state is down. Fix it."""
        if check_type == failing_check:
            return

        host = test.mock_host(save=False)

        # need to account for network checks interdependencies
        check_min_time = timestamp() - NETWORK_CHECKS_REACTION_TIMEOUT
        reasons = mock_status_reasons(
            effective_timestamp=check_min_time,
            status_mtime=check_min_time - 1,
            check_min_time=check_min_time,
        )

        fail_check(reasons, failing_check)
        break_check(reasons, check_type, broken_check_status)

        decision = _make_decision(test, host, reasons, check_type)

        assert decision.action == WalleAction.REBOOT
        assert decision.checks == [failing_check]
        assert decision.reason in {
            "Host is not available: {}.".format(get_walle_check_type(failing_check)),
            "Host is not available: {}, {} suspected.".format(
                get_walle_check_type(failing_check), get_walle_check_type(check_type)
            ),
        }

    @pytest.mark.parametrize(
        "check_type",
        list(set(CheckType.ALL_PASSIVE) - (set(CheckType.ALL_META) | {CheckType.GPU} | set(CheckType.ALL_IB))),
    )
    @pytest.mark.parametrize("infrastructure_check_type", CheckType.ALL_INFRASTRUCTURE)
    def test_required_checks_missing_and_active_check_failing_and_netmon_missing(
        self, test, check_type, infrastructure_check_type
    ):
        """Required check is not available. Host seems down, but netmon/rack check is failing too. Wait more."""
        host = test.mock_host(save=False)

        check_min_time = timestamp() - NETWORK_CHECKS_REACTION_TIMEOUT
        reasons = mock_status_reasons(effective_timestamp=check_min_time + 1, check_min_time=check_min_time)

        break_check(reasons, check_type)
        break_check(reasons, infrastructure_check_type)
        fail_check(reasons, CheckType.UNREACHABLE)
        fail_check(reasons, CheckType.SSH)

        expected_reason = (
            "No data for {} check result. "
            "But active checks are not passed yet: "
            "Host is suspected to be unavailable: "
            "unreachable, ssh, which might be due to an infrastructure issue: "
            "{} missing.".format(get_walle_check_type(check_type), get_walle_check_type(infrastructure_check_type))
        )

        decision = _make_decision(test, host, reasons, check_type)

        assert decision.action == WalleAction.WAIT
        assert set(decision.checks) == set(CheckType.ALL_ACTIVE)
        assert decision.reason == expected_reason

    @pytest.mark.parametrize(
        "check_type",
        list(set(CheckType.ALL_PASSIVE) - (set(CheckType.ALL_META) | set(CheckType.ALL_IB))),
    )
    @pytest.mark.parametrize("infrastructure_check_type", CheckType.ALL_INFRASTRUCTURE)
    @pytest.mark.parametrize("check_breaker", [break_check, fail_check])
    def test_required_checks_missing_ignores_infrastructure_checks(
        self, test, check_type, infrastructure_check_type, check_breaker
    ):
        """Required check is not available but host is alive.
        Netmon check is failing, ignore it, wait for required check.
        """
        host = test.mock_host({"state": HostState.ASSIGNED}, save=False)

        reasons = mock_status_reasons(checks_min_time=timestamp() - 1)
        break_check(reasons, check_type)
        check_breaker(reasons, infrastructure_check_type)

        if check_type in CheckType.ALL_ACTIVE:
            expected_checks = CheckType.ALL_ACTIVE + CheckType.ALL_INFRASTRUCTURE
        else:
            expected_checks = [check_type]

        decision = _make_decision(test, host, reasons, check_type)

        assert decision.action == WalleAction.WAIT
        assert set(decision.checks) == set(expected_checks)
        assert decision.reason == "No data for {} check result.".format(get_walle_check_type(check_type))

    @pytest.mark.parametrize(
        "check_type",
        (set(CheckType.ALL_JUGGLER) - (set(CheckType.ALL_META) | set(CheckType.ALL_IB))),
    )
    def test_required_passive_checks_failing(self, test, check_type):
        """Required check is failing, host is alive. Fix failure of the required check."""
        host = test.mock_host({"state": HostState.ASSIGNED}, save=False)

        reasons = mock_status_reasons(checks_min_time=timestamp() - 1)
        fail_check(reasons, check_type)

        decision = _make_decision(test, host, reasons, check_type)

        assert decision.action not in {WalleAction.WAIT, WalleAction.HEALTHY}
        assert decision.checks == [check_type]
        # don't check reason messages here, too big of a variety.

    @pytest.mark.parametrize("check_type", CheckType.ALL_ACTIVE)
    def test_required_active_checks_failing(self, test, check_type):
        """Required check is failing, host is alive. Fix failure of the required check."""
        host = test.mock_host({"state": HostState.ASSIGNED}, save=False)

        check_min_time = timestamp() - NETWORK_CHECKS_REACTION_TIMEOUT
        reasons = mock_status_reasons(
            effective_timestamp=check_min_time, status_mtime=check_min_time - 1, check_min_time=check_min_time
        )
        fail_check(reasons, check_type)

        decision = _make_decision(test, host, reasons, check_type)
        assert decision.action == WalleAction.REBOOT
        assert decision.checks == [check_type]
        # don't check reason messages here, too big of a variety.

    @pytest.mark.parametrize("check_type", set(CheckType.ALL_ACTIVE + CheckType.ALL_PASSIVE) - set(CheckType.ALL_META))
    def test_required_checks_passing(self, test, check_type):
        """Required check is passing, we are done here."""
        host = test.mock_host({"state": HostState.ASSIGNED}, save=False)

        reasons = mock_status_reasons(checks_min_time=timestamp() - 1)

        decision = _make_decision(test, host, reasons, check_type)
        assert decision.action == WalleAction.HEALTHY

    @pytest.mark.parametrize("check_type", list(set(CheckType.ALL_PASSIVE) - set(CheckType.ALL_META)))
    @pytest.mark.parametrize("infrastructure_check_type", CheckType.ALL_INFRASTRUCTURE)
    @pytest.mark.parametrize("failing_active_check", CheckType.ALL_ACTIVE)
    @pytest.mark.parametrize("check_breaker", [break_check, fail_check])
    def test_required_checks_passing_ignores_availability_checks(
        self, test, check_type, failing_active_check, infrastructure_check_type, check_breaker
    ):
        """Required check is passing, infrastructure is broken, we are done here."""
        host = test.mock_host({"state": HostState.ASSIGNED}, save=False)

        reasons = mock_status_reasons(checks_min_time=timestamp() - 1)
        check_breaker(reasons, infrastructure_check_type)
        check_breaker(reasons, failing_active_check)

        decision = _make_decision(test, host, reasons, check_type)
        assert decision.action == WalleAction.HEALTHY

    def test_checks_for_use(self, test):
        """Required check is failing, but if we don't look at it, it's ok and host is healthy."""

        def _make_decision_for_modern_dmc(test, host, reasons, checks, checks_for_use=None):
            decision_maker_cls = ModernDecisionMaker.for_modern_automation_plot(test.default_project)
            decision_maker = decision_maker_cls(test.default_project)
            return decision_maker.make_alternate_decision(host, reasons, checks, checks_for_use=checks_for_use)

        CHECK_FOR_USE = CheckType.SSH
        FAILED_CHECK = CheckType.MEMORY
        ALL_CHECKS = {CHECK_FOR_USE, FAILED_CHECK}

        host = test.mock_host({"state": HostState.ASSIGNED}, save=False)

        check_min_time = timestamp() - NETWORK_CHECKS_REACTION_TIMEOUT
        reasons = mock_status_reasons(
            effective_timestamp=check_min_time, status_mtime=check_min_time - 1, check_min_time=check_min_time
        )
        fail_check(reasons, FAILED_CHECK)

        decision = _make_decision_for_modern_dmc(test, host, reasons, ALL_CHECKS)
        assert decision.action == WalleAction.REPAIR_HARDWARE
        assert decision.checks == [FAILED_CHECK]

        decision = _make_decision_for_modern_dmc(test, host, reasons, ALL_CHECKS, checks_for_use={CHECK_FOR_USE})
        assert decision.action == WalleAction.HEALTHY

    @pytest.mark.parametrize("infrastructure_check_type", CheckType.ALL_INFRASTRUCTURE)
    @pytest.mark.parametrize("infrastructure_status", CheckStatus.ALL_MISSING)
    @pytest.mark.parametrize("host_type", HostType.get_choices())
    def test_active_checks_failing_and_infrastructure_missing(
        self, mp, test, infrastructure_check_type, infrastructure_status, host_type
    ):
        host = test.mock_host(dict(type=host_type), save=False)

        check_min_time = timestamp() - NETWORK_CHECKS_REACTION_TIMEOUT
        reasons = mock_status_reasons(effective_timestamp=check_min_time + 1, check_min_time=check_min_time)

        break_check(reasons, infrastructure_check_type, infrastructure_status)
        fail_check(reasons, CheckType.UNREACHABLE)
        fail_check(reasons, CheckType.SSH)

        decision = _make_decision(test, host, reasons, CheckType.UNREACHABLE)

        if host_type == HostType.SERVER:
            assert decision.action == WalleAction.WAIT
        else:
            assert decision.action == WalleAction.REBOOT
            assert decision.reason == "Host is not available: unreachable, ssh."
        assert set(decision.checks) == set(CheckType.ALL_ACTIVE)


class TestMakeDecisionTrace:
    def test_make_decision_trace(self, test, monkeypatch):
        """Test all rule decisions return."""
        host = test.mock_host(save=False)

        mocked_rules = [MockRule(), MockRule(fail=True), MockRule(), MockRule(wait=True)]
        expected_decisions = [
            Decision.healthy("Mock healthy reason"),
            Decision.failure("Mock fail reason"),
            Decision.healthy("Mock healthy reason"),
            Decision.wait("Mock wait reason"),
        ]

        # Monkeypatch check_rules
        monkeypatch_function(monkeypatch, decisionmakers._init_check_rules, return_value=mocked_rules)

        # Mock reasons, does not affect on anything
        mock_reasons = mock_status_reasons()
        decisions = _make_decision_trace(test, host, reasons=mock_reasons)
        for expected_decisions in expected_decisions:
            assert next(decisions).action == expected_decisions.action

        # Make sure no decisions left
        with pytest.raises(StopIteration):
            next(decisions)

    def test_make_decision_trace_only_healthy(self, test, monkeypatch):
        """Test trailing 'Host is healthy.' Decision."""
        host = test.mock_host(save=False)

        mocked_rules = [MockRule(), MockRule(), MockRule()]
        expected_decisions = [
            Decision.healthy("Mock healthy reason"),
            Decision.healthy("Mock healthy reason"),
            Decision.healthy("Mock healthy reason"),
        ]

        # Monkeypatch check_rules
        monkeypatch_function(monkeypatch, decisionmakers._init_check_rules, return_value=mocked_rules)

        # Mock reasons, does not affect on anything
        mock_reasons = mock_status_reasons()
        decisions = _make_decision_trace(test, host, reasons=mock_reasons)
        for expected_decisions in expected_decisions:
            assert next(decisions).action == expected_decisions.action

        # Trailing HEALTHY decision
        assert next(decisions).reason == "Host is healthy."

        # Make sure no decisions left
        with pytest.raises(StopIteration):
            next(decisions)

    @pytest.mark.parametrize("incorrect_metadata", ["timed out after 1 year", "exited with code 1"])
    def test_check_metadata_invalid(self, test, incorrect_metadata):
        curr_time = timestamp()
        host = test.mock_host(save=True)

        test.health_checks.mock(
            {
                "id": "1",
                "status": CheckStatus.FAILED,
                "type": CheckType.SSH,
                "timestamp": curr_time,
                "status_mtime": curr_time,
                "metadata": incorrect_metadata,
                "fqdn": host.name,
            },
            save=True,
        )

        decision_maker = get_decision_maker(host.get_project())
        host_reasons = get_host_reasons(host, decision_maker=decision_maker)

        assert host_reasons['ssh']['status'] == CheckStatus.INVALID
        assert host_reasons['ssh']['invalid_reason'] == "{}: check ssh invalid: {}".format(
            host.name, incorrect_metadata
        )


class TestCheckDisabler:
    @staticmethod
    def _mock_host(hostname_hash):
        mock_project = Mock()
        mock_project.return_value.tags = []

        class MockStr(str):
            def __hash__(self):
                return hostname_hash

        class MockHost:
            inv = 999
            name = MockStr("mock-host-name")
            project = "project-mock"
            ipmi_mac = "ipmi_mac"
            status = HostStatus.READY
            dns = None
            get_project = mock_project
            location = Mock()
            platform = Mock()
            type = HostType.SERVER
            applied_restrictions = Mock(return_value=[])

            def human_id(self):
                return self.inv

        return MockHost()

    @pytest.fixture(autouse=True)
    def _mock_hash(self, mp):
        mp.function(utils._get_host_hash, side_effect=lambda host: hash(host.name) % 100)

    @pytest.fixture(autouse=True)
    def _mock_config(self, mp):
        monkeypatch_config(mp, "infiniband.involvement_tag", "infiniband-project-tag")

    @pytest.mark.parametrize(
        "check_type", (set(CheckType.ALL_JUGGLER) - set(CheckType.ALL_META) - set(CheckType.ALL_IB))
    )
    @pytest.mark.parametrize("hostname_hash", [0, 10, 90, 100, 101])
    def test_check_enabled_when_no_limit_exist(self, test, monkeypatch, check_type, hostname_hash):
        monkeypatch_config(monkeypatch, "automation.checks_percentage", {})

        reasons = mock_status_reasons(status=HealthStatus.STATUS_FAILURE)
        host = self._mock_host(hostname_hash)

        decision_maker = get_decision_maker(test.default_project, enabled_checks={check_type})
        assert decision_maker.make_decision(host, reasons).action not in {WalleAction.HEALTHY, WalleAction.WAIT}

    @pytest.mark.parametrize(
        "check_type",
        (set(CheckType.ALL_JUGGLER) - (set(CheckType.ALL_META) | set(CheckType.ALL_IB))),
    )
    @pytest.mark.parametrize("hostname_hash", [0, 99, 100, 101])
    def test_check_always_enabled_if_limit_is_100(self, test, monkeypatch, check_type, hostname_hash):
        monkeypatch_config(monkeypatch, "automation.checks_percentage", defaultdict(lambda: 100))

        reasons = mock_status_reasons(status=HealthStatus.STATUS_FAILURE)
        host = self._mock_host(hostname_hash)

        decision_maker = get_decision_maker(test.default_project, enabled_checks={check_type})
        assert decision_maker.make_decision(host, reasons).action not in {WalleAction.HEALTHY, WalleAction.WAIT}

    @pytest.mark.parametrize("check_type", set(CheckType.ALL_JUGGLER) - set(CheckType.ALL_META))
    @pytest.mark.parametrize("hostname_hash", [0, 10, 90, 100])
    def test_check_disabled_when_limit_is_zero(self, test, monkeypatch, check_type, hostname_hash):
        monkeypatch_config(monkeypatch, "automation.checks_percentage", defaultdict(int))
        reasons = mock_status_reasons(status=HealthStatus.STATUS_FAILURE)
        host = self._mock_host(hostname_hash)

        decision_maker = get_decision_maker(test.default_project, enabled_checks={check_type})
        assert decision_maker.make_decision(host, reasons).action == WalleAction.HEALTHY

    @pytest.mark.parametrize(
        "check_type",
        (set(CheckType.ALL_JUGGLER) - (set(CheckType.ALL_META) | set(CheckType.ALL_IB))),
    )
    def test_check_enabled_when_host_matches_percent(self, test, monkeypatch, check_type):
        threshold = 80
        monkeypatch_config(monkeypatch, "automation.checks_percentage", defaultdict(lambda: threshold))
        reasons = mock_status_reasons(status=HealthStatus.STATUS_FAILURE)

        enabled_hosts = [self._mock_host(0), self._mock_host(threshold - 1)]
        disabled_hosts = [self._mock_host(threshold), self._mock_host(threshold + 1)]

        decision_maker = get_decision_maker(test.default_project, enabled_checks={check_type})

        for host in disabled_hosts:
            assert decision_maker.make_decision(host, reasons).action == WalleAction.HEALTHY

        for host in enabled_hosts:
            assert decision_maker.make_decision(host, reasons).action not in {WalleAction.HEALTHY, WalleAction.WAIT}
