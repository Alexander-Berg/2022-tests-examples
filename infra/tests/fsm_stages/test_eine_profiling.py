"""Tests host profiling via Einstellung."""

import random
from unittest.mock import call

import pytest

from infra.walle.server.tests.lib.util import (
    TestCase,
    handle_host,
    mock_task,
    monkeypatch_clients_for_host,
    cancel_stage,
    mock_commit_stage_changes,
    check_stage_initialization,
    mock_complete_current_stage,
    mock_retry_parent_stage,
    mock_fail_current_stage,
    monkeypatch_max_errors,
    mock_stage_internal_error,
    mock_schedule_host_redeployment,
)
from sepelib.core import config
from sepelib.core.constants import DAY_SECONDS
from walle import authorization
from walle.admin_requests.severity import EineTag
from walle.clients import eine
from walle.clients.eine import EineProfileStatus, ProfileMode, EineProfileTags
from walle.constants import EINE_NOP_PROFILE, EINE_PROFILES_WITH_DC_SUPPORT, PROVISIONER_EINE
from walle.fsm_stages import eine_profiling
from walle.fsm_stages.common import get_current_stage
from walle.fsm_stages.constants import EineProfileOperation
from walle.hosts import Host, TaskType
from walle.models import timestamp
from walle.stages import Stage, Stages, StageTerminals


@pytest.fixture
def test(request, monkeypatch_timestamp):
    del monkeypatch_timestamp  # make linter happy
    return TestCase.create(request)


def test_stage_initialization(test):
    check_stage_initialization(test, Stage(name=Stages.EINE_PROFILE), status=eine_profiling._STATUS_ASSIGN_PROFILE)


class TestAssignProfile:
    test = None
    profile = None
    host = None

    @pytest.fixture(autouse=True)
    def init(self, test):
        self.test = test
        self.profile = "profile-mock"

    def mock_host(self, vlan_success=False):
        host = self.test.mock_host(
            {
                "task": mock_task(
                    stage=Stages.EINE_PROFILE,
                    stage_params={
                        "operation": EineProfileOperation.PROFILE,
                        "profile": self.profile,
                        "vlans_stage": '1.1',
                        "type": TaskType.AUTOMATED_HEALING,
                    },
                    stage_status=eine_profiling._STATUS_ASSIGN_PROFILE,
                    previous_stage_data={"vlan_success": True} if vlan_success else None,
                )
            }
        )

        self.test.mock_host_network({"network_timestamp": timestamp()}, host=host)
        return host

    def test_internal_eine_error(self, monkeypatch):
        host = self.mock_host()
        clients = monkeypatch_clients_for_host(
            monkeypatch, host, mock_internal_profile_error=True, power_on=True, switch_info="switch-mock port-mock"
        )

        handle_host(host, suppress_internal_errors=(eine.EineInternalError,))

        assert clients.mock_calls == [call.eine.get_host_status(host.inv, profile=True, location=True)]
        mock_stage_internal_error(host, "Error in communication with Einstellung: Mocked internal Einstellung error.")

        self.test.hosts.assert_equal()
        self.test.host_network.assert_equal()

    def test_persistent_eine_error(self, monkeypatch):
        host = self.mock_host()
        clients = monkeypatch_clients_for_host(monkeypatch, host, mock_persistent_profile_error=True, power_on=True)

        handle_host(host)

        assert clients.mock_calls == [call.eine.get_host_status(host.inv, profile=True, location=True)]
        mock_commit_stage_changes(
            host, error="Failed to get host from eine.", check_after=eine_profiling._PERSISTENT_ERROR_RETRY_PERIOD
        )

        self.test.hosts.assert_equal()
        self.test.host_network.assert_equal()

    @pytest.mark.parametrize("timed_out", [True, False])
    def test_assign_in_process(self, monkeypatch, timed_out):
        host = self.mock_host()
        stage = get_current_stage(host)
        stage.status_time = timestamp() - eine_profiling._MAX_ASSIGN_DELAY + 1 - int(timed_out)
        host.save()

        clients = monkeypatch_clients_for_host(
            monkeypatch,
            host,
            profile_name="other",
            profile_status=EineProfileStatus.QUEUED,
            switch_info="switch-mock port-mock",
        )

        handle_host(host)
        assert clients.mock_calls == [
            call.eine.get_host_status(host.inv, profile=True, location=True),
        ]

        message = "eine is currently running 'other' profile on the host. Waiting for it to finish."
        message_kwarg = {"error" if timed_out else "status_message": message}
        mock_commit_stage_changes(host, check_after=eine_profiling._CHECK_PERIOD, **message_kwarg)

        self.test.hosts.assert_equal()
        self.test.host_network.assert_equal()

    @pytest.mark.parametrize("vlan_success", [True, False])
    @pytest.mark.parametrize("platform_supported_ipxe", (True, False))
    def test_assign_without_tags(self, mp, vlan_success, platform_supported_ipxe):
        host = self.mock_host(vlan_success)
        mp.method(Host.platform_ipxe_supported, return_value=platform_supported_ipxe, obj=Host)
        stage = get_current_stage(host)
        assert "profile_tags" not in stage.params
        clients = monkeypatch_clients_for_host(
            mp, host, profile_status=EineProfileStatus.FAILED, tags=["a"], switch_info="switch-mock port-mock"
        )

        handle_host(host)

        need_skip_vlan = vlan_success and platform_supported_ipxe
        assert clients.mock_calls == [
            call.eine.get_host_status(host.inv, profile=True, location=True),
            call.eine.get_tags(host.inv),
            call.eine.update_tags(
                host.inv, add=[EineProfileTags.SKIP_VLAN_SWITCH] if need_skip_vlan else [], remove=["a"]
            ),
            call.eine.assign_profile(
                host.inv, "profile-mock", local_tags=None, cc_logins=None, assigned_for=authorization.ISSUER_WALLE
            ),
        ]

        stage.set_temp_data("profile_assign_time", timestamp())
        mock_commit_stage_changes(
            host, status=eine_profiling._STATUS_PROFILING, check_after=eine_profiling._CHECK_PERIOD
        )

        self.test.hosts.assert_equal()
        self.test.host_network.assert_equal()

    @pytest.mark.parametrize("vlan_success", [True, False])
    @pytest.mark.parametrize("platform_supported_ipxe", (True, False))
    def test_assign_with_tags(self, mp, vlan_success, platform_supported_ipxe):
        host = self.mock_host(vlan_success)
        mp.method(Host.platform_ipxe_supported, return_value=platform_supported_ipxe, obj=Host)
        stage = get_current_stage(host)
        stage.params["profile_tags"] = [
            EineProfileTags.DANGEROUS_LOAD,
            EineProfileTags.FULL_PROFILING,
            "lan-is-flashed",
            "_set:do-not-use-snmp",
        ]
        host.save()

        clients = monkeypatch_clients_for_host(
            mp,
            host,
            profile_status=EineProfileStatus.COMPLETED,
            tags=[
                "lan-is-flashed",
                "_set:do-not-use-snmp",
                "PERF:321GFLOPS",
                EineProfileTags.SMART_RELAX,
                EineProfileTags.MEMORY_RELAX,
            ],
            switch_info="switch-mock port-mock",
        )

        handle_host(host)

        need_skip_vlan = vlan_success and platform_supported_ipxe
        assert clients.mock_calls == [
            call.eine.get_host_status(host.inv, profile=True, location=True),
            call.eine.get_tags(host.inv),
            call.eine.update_tags(
                host.inv,
                add=[EineProfileTags.DANGEROUS_LOAD, EineProfileTags.FULL_PROFILING]
                + ([EineProfileTags.SKIP_VLAN_SWITCH] if need_skip_vlan else []),
                remove=[EineProfileTags.MEMORY_RELAX, EineProfileTags.SMART_RELAX],
            ),
            call.eine.assign_profile(
                host.inv, "profile-mock", local_tags=None, cc_logins=None, assigned_for=authorization.ISSUER_WALLE
            ),
        ]

        stage.set_temp_data("profile_assign_time", timestamp())
        mock_commit_stage_changes(
            host, status=eine_profiling._STATUS_PROFILING, check_after=eine_profiling._CHECK_PERIOD
        )

        self.test.hosts.assert_equal()
        self.test.host_network.assert_equal()

    @pytest.mark.parametrize("disable_admin_requests", (True, False))
    @pytest.mark.parametrize("repair_request_severity", (EineTag.ALL))
    def test_assign_with_dc_support_with_severity_tags(
        self, monkeypatch, disable_admin_requests, repair_request_severity
    ):
        chosen_profile = random.choice(EINE_PROFILES_WITH_DC_SUPPORT)

        host = self.mock_host()
        stage = get_current_stage(host)
        stage.params["profile"] = chosen_profile
        stage.params["repair_request_severity"] = repair_request_severity
        host.task.disable_admin_requests = disable_admin_requests
        host.save()

        clients = monkeypatch_clients_for_host(
            monkeypatch, host, profile_status=EineProfileStatus.FAILED, switch_info="switch-mock port-mock"
        )

        handle_host(host)

        if disable_admin_requests:
            local_tags, cc_logins = None, None
        else:
            local_tags, cc_logins = ["eaas", repair_request_severity], ["robot-walle"]
            stage.set_temp_data("dc_support", True)

        stage.set_temp_data("profile_assign_time", timestamp())
        mock_commit_stage_changes(
            host, status=eine_profiling._STATUS_PROFILING, check_after=eine_profiling._CHECK_PERIOD
        )

        assert clients.mock_calls == [
            call.eine.get_host_status(host.inv, profile=True, location=True),
            call.eine.get_tags(host.inv),
            call.eine.assign_profile(
                host.inv,
                chosen_profile,
                local_tags=local_tags,
                cc_logins=cc_logins,
                assigned_for=authorization.ISSUER_WALLE,
            ),
        ]

        self.test.hosts.assert_equal()
        self.test.host_network.assert_equal()

    @pytest.mark.parametrize("disable_admin_requests", (True, False))
    def test_assign_with_dc_support(self, monkeypatch, disable_admin_requests):
        chosen_profile = random.choice(EINE_PROFILES_WITH_DC_SUPPORT)

        host = self.mock_host()
        stage = get_current_stage(host)
        stage.params["profile"] = chosen_profile
        host.task.disable_admin_requests = disable_admin_requests
        host.save()

        clients = monkeypatch_clients_for_host(
            monkeypatch, host, profile_status=EineProfileStatus.FAILED, switch_info="switch-mock port-mock"
        )

        handle_host(host)

        if disable_admin_requests:
            local_tags, cc_logins = None, None
        else:
            local_tags, cc_logins = ["eaas", EineTag.MEDIUM], ["robot-walle"]
            stage.set_temp_data("dc_support", True)

        stage.set_temp_data("profile_assign_time", timestamp())
        mock_commit_stage_changes(
            host, status=eine_profiling._STATUS_PROFILING, check_after=eine_profiling._CHECK_PERIOD
        )

        assert clients.mock_calls == [
            call.eine.get_host_status(host.inv, profile=True, location=True),
            call.eine.get_tags(host.inv),
            call.eine.assign_profile(
                host.inv,
                chosen_profile,
                local_tags=local_tags,
                cc_logins=cc_logins,
                assigned_for=authorization.ISSUER_WALLE,
            ),
        ]

        self.test.hosts.assert_equal()
        self.test.host_network.assert_equal()

    @pytest.mark.parametrize("vlan_success", [True, False])
    @pytest.mark.parametrize("switch_info", [None, "old-switch-mock old-port-mock"])
    def test_assign_and_set_host_location_info(self, monkeypatch, switch_info, vlan_success):
        host = self.mock_host(vlan_success)
        stage = get_current_stage(host)

        clients = monkeypatch_clients_for_host(
            monkeypatch, host, profile_status=EineProfileStatus.FAILED, switch_info=switch_info
        )

        handle_host(host)

        stage.set_temp_data("profile_assign_time", timestamp())
        mock_commit_stage_changes(
            host, status=eine_profiling._STATUS_PROFILING, check_after=eine_profiling._CHECK_PERIOD
        )

        assert clients.mock_calls == [
            call.eine.get_host_status(host.inv, profile=True, location=True),
            call.eine.set_host_location(host.inv, "switch-mock", "port-mock"),
            call.eine.get_tags(host.inv),
            call.eine.update_tags(
                host.inv,
                add=["_set:do-not-use-snmp", EineProfileTags.SKIP_VLAN_SWITCH]
                if vlan_success
                else ["_set:do-not-use-snmp"],
                remove=[],
            ),
            call.eine.assign_profile(
                host.inv, "profile-mock", local_tags=None, cc_logins=None, assigned_for=authorization.ISSUER_WALLE
            ),
        ]

        self.test.hosts.assert_equal()
        self.test.host_network.assert_equal()

    def test_assign_with_plaform_specific_tags(self, mp):
        host = self.mock_host(True)
        mp.method(Host.get_platform_eine_tags, return_value={EineProfileTags.SKIP_FW_FANTABLE}, obj=Host)
        stage = get_current_stage(host)
        stage.params["profile_tags"] = ["lan-is-flashed", "_set:do-not-use-snmp"]
        host.save()

        clients = monkeypatch_clients_for_host(
            mp,
            host,
            profile_status=EineProfileStatus.COMPLETED,
            tags=["lan-is-flashed", "_set:do-not-use-snmp", "PERF:321GFLOPS"],
            switch_info="switch-mock port-mock",
        )

        handle_host(host)

        assert clients.mock_calls == [
            call.eine.get_host_status(host.inv, profile=True, location=True),
            call.eine.get_tags(host.inv),
            call.eine.update_tags(
                host.inv, add=[EineProfileTags.SKIP_FW_FANTABLE, EineProfileTags.SKIP_VLAN_SWITCH], remove=[]
            ),
            call.eine.assign_profile(
                host.inv, "profile-mock", local_tags=None, cc_logins=None, assigned_for=authorization.ISSUER_WALLE
            ),
        ]

        stage.set_temp_data("profile_assign_time", timestamp())
        mock_commit_stage_changes(
            host, status=eine_profiling._STATUS_PROFILING, check_after=eine_profiling._CHECK_PERIOD
        )

        self.test.hosts.assert_equal()
        self.test.host_network.assert_equal()


class TestProfiling:
    @pytest.fixture(autouse=True)
    def init(self, test):
        self.test = test
        self.profile = "profile-mock"

    def mock_host(self, vlan_success=False):
        host = self.test.mock_host(
            {
                "task": mock_task(
                    stage=Stages.EINE_PROFILE,
                    stage_params={
                        "operation": EineProfileOperation.PROFILE,
                        "profile": self.profile,
                    },
                    stage_status=eine_profiling._STATUS_PROFILING,
                    stage_temp_data={"profile_assign_time": timestamp()},
                    previous_stage_data={"vlan_success": True} if vlan_success else None,
                )
            }
        )
        self.test.mock_host_network({"network_timestamp": timestamp()}, host=host)
        return host

    @pytest.mark.parametrize("dc_support", [True, False])
    def test_pending(self, monkeypatch, dc_support):
        host = self.mock_host()
        stage = get_current_stage(host)
        stage.set_temp_data("dc_support", dc_support)
        host.save()

        clients = monkeypatch_clients_for_host(
            monkeypatch, host, profile_status=EineProfileStatus.QUEUED, profile_name=self.profile
        )
        handle_host(host)

        assert clients.mock_calls == [call.eine.get_host_status(host.inv, profile=True, location=True)]
        host.task.status = Stages.EINE_PROFILE + ":stage-mock:running"
        if dc_support:
            stage.set_temp_data("profile_status_time", timestamp())
            stage.set_temp_data("profile_status", EineProfileStatus.QUEUED)
        mock_commit_stage_changes(host, check_after=eine_profiling._CHECK_PERIOD)

        self.test.hosts.assert_equal()
        self.test.host_network.assert_equal()

    def run_on_service_test(self, monkeypatch, eine_status, service_profile):
        host = self.mock_host()
        stage = get_current_stage(host)
        stage.set_temp_data("dc_support", True)
        host.save()

        clients = monkeypatch_clients_for_host(
            monkeypatch, host, profile_name=service_profile, profile_status=eine_status
        )
        message = "Someone has switched the host to '{}' service profile. Waiting for it to complete.".format(
            service_profile
        )

        handle_host(host)
        assert clients.mock_calls == [call.eine.get_host_status(host.inv, profile=True, location=True)]
        stage.set_temp_data("profile_status_time", timestamp())
        stage.set_temp_data("profile_status", "{}:{}".format(service_profile, eine_status))
        host.task.status = Stages.EINE_PROFILE + ":stage-mock:running"
        mock_commit_stage_changes(host, status_message=message, check_after=eine_profiling._DC_SUPPORT_CHECK_PERIOD)
        self.test.hosts.assert_equal()
        self.test.host_network.assert_equal()

        clients.reset_mock()
        stage.set_temp_data("profile_status_time", timestamp() - eine_profiling._SERVICE_TIMEOUT + 1)
        host.save()

        handle_host(host)
        assert clients.mock_calls == [call.eine.get_host_status(host.inv, profile=True, location=True)]
        mock_commit_stage_changes(host, status_message=message, check_after=eine_profiling._DC_SUPPORT_CHECK_PERIOD)
        self.test.hosts.assert_equal()
        self.test.host_network.assert_equal()

        clients.reset_mock()
        stage.set_temp_data("profile_status_time", timestamp() - eine_profiling._SERVICE_TIMEOUT)
        host.save()

        if eine_status == EineProfileStatus.QUEUED:
            handle_host(host)
            mock_commit_stage_changes(host, status_message=message, check_after=eine_profiling._DC_SUPPORT_CHECK_PERIOD)
            self.test.hosts.assert_equal()
        else:
            self._mock_retry_or_fail(host, monkeypatch, clients, exceed_max_error_count=False)

    @pytest.mark.parametrize("eine_status", EineProfileStatus.ALL)
    def test_on_service_all_statuses(self, monkeypatch, eine_status):
        # this test verifies status handling, it uses one random service profile to test status.
        [service_profile] = random.sample(eine.EINE_SERVICE_PROFILES, 1)
        self.run_on_service_test(monkeypatch, eine_status, service_profile)

    def test_failed(self, monkeypatch, exceed_max_error_count):
        host = self.mock_host()
        clients = monkeypatch_clients_for_host(
            monkeypatch, host, profile_status=EineProfileStatus.FAILED, profile_name=self.profile
        )

        self._mock_retry_or_fail(host, monkeypatch, clients, exceed_max_error_count)

    @pytest.mark.parametrize("ticket", [True, False])
    def test_failed_with_dc_support(self, monkeypatch, ticket):
        host = self.mock_host()
        current_stage = get_current_stage(host)
        current_stage.set_temp_data("dc_support", True)
        host.save()

        clients = monkeypatch_clients_for_host(
            monkeypatch, host, profile_status=EineProfileStatus.FAILED, profile_name=self.profile, eaas_ticket=ticket
        )

        handle_host(host)

        assert clients.mock_calls == [call.eine.get_host_status(host.inv, profile=True, location=True)]
        host.task.status = Stages.EINE_PROFILE + ":stage-mock:running"
        if ticket:
            host.ticket = "ITDC-0"
            current_stage.set_data("tickets", ["ITDC-0"])
            current_stage.set_data("failure_detected", True)

        current_stage.set_temp_data("profile_status", EineProfileStatus.FAILED)
        current_stage.set_temp_data("profile_status_time", timestamp())
        mock_commit_stage_changes(
            host,
            check_after=eine_profiling._DC_SUPPORT_CHECK_PERIOD,
            status_message="Profile failed - it got {status}/stage-mock:running status: <no message>.{ticket}".format(
                status=EineProfileStatus.FAILED, ticket=" (see https://st.yandex-team.ru/ITDC-0)" if ticket else ""
            ),
        )

        self.test.hosts.assert_equal()
        self.test.host_network.assert_equal()

    def test_failed_with_dc_support_but_no_ticket(self, monkeypatch):
        host = self.mock_host()
        current_stage = get_current_stage(host)
        current_stage.set_temp_data("dc_support", True)
        current_stage.set_temp_data("profile_status", EineProfileStatus.FAILED)
        current_stage.set_temp_data("profile_status_time", timestamp() - eine_profiling._WAIT_FOR_TICKET_TIMEOUT - 1)
        host.save()

        clients = monkeypatch_clients_for_host(
            monkeypatch, host, profile_status=EineProfileStatus.FAILED, profile_name=self.profile
        )

        handle_host(host)

        assert clients.mock_calls == [call.eine.get_host_status(host.inv, profile=True, location=True)]
        host.task.status = Stages.EINE_PROFILE + ":stage-mock:running"

        mock_commit_stage_changes(
            host,
            check_after=eine_profiling._DC_SUPPORT_CHECK_PERIOD,
            error=(
                "Profile failed - it got {status}/stage-mock:running status: <no message>."
                " but no eaas ticket created.".format(status=EineProfileStatus.FAILED)
            ),
        )

        self.test.hosts.assert_equal()
        self.test.host_network.assert_equal()

    def test_failed_with_dc_support_but_no_ticket_eaas_tag_missing(self, monkeypatch):
        host = self.mock_host()
        current_stage = get_current_stage(host)
        current_stage.set_temp_data("dc_support", True)
        current_stage.set_temp_data("profile_status", EineProfileStatus.FAILED)
        current_stage.set_temp_data("profile_status_time", timestamp() - eine_profiling._WAIT_FOR_TICKET_TIMEOUT - 1)
        host.save()

        clients = monkeypatch_clients_for_host(
            monkeypatch, host, profile_status=EineProfileStatus.FAILED, profile_name=self.profile, profile_local_tags=[]
        )

        handle_host(host)

        assert clients.mock_calls == [call.eine.get_host_status(host.inv, profile=True, location=True)]
        host.task.status = Stages.EINE_PROFILE + ":stage-mock:running"

        current_stage.set_data("eine_errors", 1)
        mock_retry_parent_stage(host, check_after=eine_profiling._ERROR_RETRY_DELAY)

        self.test.hosts.assert_equal()
        self.test.host_network.assert_equal()

    @pytest.mark.parametrize(
        ["tags", "eine_status", "timeout"],
        [
            ([], EineProfileStatus.QUEUED, eine_profiling._IN_PROCESS_TIMEOUT),
            ([EineProfileTags.EXTRA_LOAD], EineProfileStatus.QUEUED, eine_profiling._IN_PROCESS_EXTENDED_TIMEOUT),
            (
                [EineProfileTags.FULL_PROFILING, EineProfileTags.ADVANCED_LOAD],
                EineProfileStatus.STOPPED,
                eine_profiling._STOPPED_TIMEOUT,
            ),
        ],
    )
    def test_no_dc_support_timeout_time_has_not_come_yet(self, monkeypatch, tags, eine_status, timeout):
        host = self.mock_host()

        stage = get_current_stage(host)
        stage.status_time = timestamp() - timeout + 1
        stage.set_param("profile_tags", tags)
        host.save()

        monkeypatch_clients_for_host(monkeypatch, host, profile_status=eine_status, profile_name=self.profile)

        handle_host(host)

        host.task.status = Stages.EINE_PROFILE + ":stage-mock:running"
        mock_commit_stage_changes(host, check_after=eine_profiling._CHECK_PERIOD)

        self.test.hosts.assert_equal()
        self.test.host_network.assert_equal()

    @pytest.mark.parametrize(
        ["tags", "eine_status", "timeout"],
        [
            (
                [EineProfileTags.FULL_PROFILING, EineProfileTags.ADVANCED_LOAD],
                EineProfileStatus.QUEUED,
                eine_profiling._IN_PROCESS_TIMEOUT,
            ),
            ([EineProfileTags.EXTRA_LOAD], EineProfileStatus.QUEUED, eine_profiling._IN_PROCESS_EXTENDED_TIMEOUT),
            (
                [EineProfileTags.FULL_PROFILING, EineProfileTags.ADVANCED_LOAD],
                EineProfileStatus.STOPPED,
                eine_profiling._STOPPED_TIMEOUT,
            ),
        ],
    )
    def test_no_dc_support_timeout_time_has_come(self, monkeypatch, tags, eine_status, timeout, exceed_max_error_count):
        host = self.mock_host()

        stage = get_current_stage(host)
        stage.status_time = timestamp() - timeout
        stage.set_param("profile_tags", tags)
        host.task.status = "eine-profile:stage-mock:running"
        host.save()

        monkeypatch_clients_for_host(monkeypatch, host, profile_status=eine_status, profile_name=self.profile)
        monkeypatch_max_errors(monkeypatch, "profiling.max_errors", exceed_max_error_count)
        handle_host(host)

        if eine_status == EineProfileStatus.QUEUED:
            error = "Profiling process has timed out."
            mock_commit_stage_changes(host, error=error, check_after=eine_profiling._CHECK_PERIOD)
        else:
            get_current_stage(host).set_data("eine_errors", 1)
            if exceed_max_error_count:
                error = (
                    "Waiting until somebody run profile 'profile-mock' and it completes successfully. "
                    "Too many errors occurred during processing 'eine-profile' stage of 'ready' task. "
                    "Last error: Profiling process has been stopped."
                )
                mock_commit_stage_changes(host, error=error, check_after=eine_profiling._PERSISTENT_ERROR_RETRY_PERIOD)
            else:
                mock_retry_parent_stage(host, check_after=eine_profiling._ERROR_RETRY_DELAY)

        self.test.hosts.assert_equal()
        self.test.host_network.assert_equal()

    def test_no_dc_support_timeout_time_has_come_for_daily_retry(self, monkeypatch):
        host = self.mock_host()

        stage = get_current_stage(host)
        stage.status_time = timestamp() - eine_profiling._STOPPED_TIMEOUT - DAY_SECONDS
        host.task.status = "eine-profile:stage-mock:running"
        host.save()

        monkeypatch_clients_for_host(
            monkeypatch, host, profile_status=EineProfileStatus.STOPPED, profile_name=self.profile
        )
        monkeypatch_max_errors(monkeypatch, "profiling.max_errors", 1)
        handle_host(host)

        get_current_stage(host).set_data("eine_errors", 1)
        error = (
            "Too many errors occurred during processing 'eine-profile' stage of 'ready' task. "
            "Last error: Profiling process has been stopped."
        )
        mock_retry_parent_stage(host, error=error, check_after=eine_profiling._ERROR_RETRY_DELAY)
        self.test.hosts.assert_equal()
        self.test.host_network.assert_equal()

    def test_dc_support_overall_timeout(self, monkeypatch):
        host = self.mock_host()

        stage = get_current_stage(host)
        stage.set_temp_data("dc_support", True)
        stage.status_time = timestamp() - eine_profiling._TOTAL_TIMEOUT
        host.save()

        monkeypatch_clients_for_host(
            monkeypatch, host, profile_status=EineProfileStatus.QUEUED, profile_name=self.profile
        )

        handle_host(host)

        mock_fail_current_stage(host, reason="Profiling process has timed out.")
        self.test.hosts.assert_equal()
        self.test.host_network.assert_equal()

    @pytest.mark.parametrize(
        ["tags", "eine_status", "timeout"],
        [
            (
                [EineProfileTags.FULL_PROFILING, EineProfileTags.ADVANCED_LOAD],
                EineProfileStatus.QUEUED,
                eine_profiling._IN_PROCESS_TIMEOUT,
            ),
            (
                [EineProfileTags.FULL_PROFILING, EineProfileTags.EXTRA_LOAD],
                EineProfileStatus.QUEUED,
                eine_profiling._IN_PROCESS_EXTENDED_TIMEOUT,
            ),
            (
                [EineProfileTags.FULL_PROFILING, EineProfileTags.ADVANCED_LOAD],
                EineProfileStatus.STOPPED,
                eine_profiling._STOPPED_TIMEOUT,
            ),
        ],
    )
    def test_dc_support_timeout_without_ticket(self, monkeypatch, tags, eine_status, timeout, exceed_max_error_count):
        host = self.mock_host()

        stage = get_current_stage(host)
        stage.set_temp_data("dc_support", True)
        stage.set_param("profile_tags", tags)

        stage.set_temp_data("profile_status", eine_status)
        stage.set_temp_data("profile_status_time", timestamp() - timeout)
        stage.status_time = timestamp() - timeout

        host.task.status = "eine-profile:stage-mock:running"
        host.save()

        monkeypatch_clients_for_host(monkeypatch, host, profile_status=eine_status, profile_name=self.profile)
        handle_host(host)

        if eine_status == EineProfileStatus.QUEUED:
            error = "Profiling process has timed out."
            mock_commit_stage_changes(host, error=error, check_after=eine_profiling._CHECK_PERIOD)
        else:
            get_current_stage(host).set_data("eine_errors", 1)
            mock_retry_parent_stage(host, check_after=eine_profiling._ERROR_RETRY_DELAY)
        self.test.hosts.assert_equal()
        self.test.host_network.assert_equal()

    @pytest.mark.parametrize(
        ["tags", "eine_status", "timeout"],
        [
            (
                [EineProfileTags.FULL_PROFILING, EineProfileTags.ADVANCED_LOAD],
                EineProfileStatus.QUEUED,
                eine_profiling._IN_PROCESS_TIMEOUT,
            ),
            (
                [EineProfileTags.FULL_PROFILING, EineProfileTags.EXTRA_LOAD],
                EineProfileStatus.QUEUED,
                eine_profiling._IN_PROCESS_EXTENDED_TIMEOUT,
            ),
            (
                [EineProfileTags.FULL_PROFILING, EineProfileTags.ADVANCED_LOAD],
                EineProfileStatus.STOPPED,
                eine_profiling._STOPPED_TIMEOUT,
            ),
        ],
    )
    def test_dc_support_timeout_with_ticket(self, monkeypatch, tags, eine_status, timeout):
        host = self.mock_host()

        stage = get_current_stage(host)
        stage.set_temp_data("dc_support", True)
        stage.set_param("profile_tags", tags)

        stage.set_temp_data("profile_status_time", timestamp() - timeout)
        stage.set_temp_data("profile_status", eine_status)
        stage.status_time = timestamp() - timeout

        host.save()

        monkeypatch_clients_for_host(
            monkeypatch, host, profile_status=eine_status, profile_name=self.profile, eaas_ticket=True
        )

        handle_host(host)

        stage.set_data("tickets", ["ITDC-0"])
        host.ticket = "ITDC-0"
        host.task.status = Stages.EINE_PROFILE + ":stage-mock:running"
        mock_commit_stage_changes(host, check_after=eine_profiling._CHECK_PERIOD)

        self.test.hosts.assert_equal()
        self.test.host_network.assert_equal()

    def test_changed_profile_is_running(self, monkeypatch):
        host = self.mock_host()
        eine_status = EineProfileStatus.QUEUED

        monkeypatch_clients_for_host(monkeypatch, host, profile_status=eine_status, profile_name="invalid-profile")
        mock_commit_stage_changes(
            host,
            status_message="Someone changed current host profile in Eine "
            "from 'flexy' to 'invalid-profile'. "
            "Waiting for it to complete.",
        )

    @pytest.mark.parametrize("eine_status", set(EineProfileStatus.ALL) - {EineProfileStatus.QUEUED})
    def test_changed_profile_is_not_running__giving_timeout(self, monkeypatch, eine_status):
        host = self.mock_host()
        monkeypatch_clients_for_host(monkeypatch, host, profile_status=eine_status, profile_name="invalid-profile")
        mock_commit_stage_changes(
            host,
            status_message="Someone changed current host profile in Eine "
            "from 'flexy' to 'invalid-profile'. "
            "Waiting for it to complete.",
        )

    @pytest.mark.parametrize("eine_status", set(EineProfileStatus.ALL) - {EineProfileStatus.QUEUED})
    def test_changed_profile_is_not_running__force_recover(self, monkeypatch, eine_status, exceed_max_error_count):
        host = self.mock_host()
        stage = get_current_stage(host)
        stage.status_time = timestamp() - eine_profiling._STOPPED_TIMEOUT
        host.task.status = "eine-profile:stage-mock:running"
        host.save()

        clients = monkeypatch_clients_for_host(
            monkeypatch, host, profile_status=eine_status, profile_name="invalid-profile"
        )

        error_message = (
            "Waiting until somebody run profile 'profile-mock' and it completes successfully. "
            "Too many errors occurred during processing 'eine-profile' stage of 'ready' task. "
            "Last error: Someone changed current host profile in Eine "
            "from profile-mock to invalid-profile. Switching back."
        )

        self._mock_retry_or_wait(
            host,
            monkeypatch,
            clients,
            exceed_max_error_count,
            on_exceed=lambda: mock_commit_stage_changes(
                host, error=error_message, check_after=eine_profiling._PERSISTENT_ERROR_RETRY_PERIOD
            ),
        )

    def test_internal_eine_error(self, monkeypatch):
        host = self.mock_host()
        clients = monkeypatch_clients_for_host(monkeypatch, host, mock_internal_profile_error=True)

        handle_host(host, suppress_internal_errors=(eine.EineInternalError,))

        assert clients.mock_calls == [call.eine.get_host_status(host.inv, profile=True, location=True)]
        mock_stage_internal_error(host, "Error in communication with Einstellung: Mocked internal Einstellung error.")

        self.test.hosts.assert_equal()
        self.test.host_network.assert_equal()

    def test_missing_host_in_eine(self, monkeypatch):
        host = self.mock_host()
        clients = monkeypatch_clients_for_host(monkeypatch, host, profile_status=None)

        handle_host(host)

        assert clients.mock_calls == [call.eine.get_host_status(host.inv, profile=True, location=True)]
        mock_commit_stage_changes(
            host,
            error="Einstellung returned an error: Mock persistent error",
            check_after=eine_profiling._PERSISTENT_ERROR_RETRY_PERIOD,
        )

        self.test.hosts.assert_equal()
        self.test.host_network.assert_equal()

    @pytest.mark.parametrize("with_timeout", (False, True))
    @pytest.mark.parametrize("with_dc_support", (False, True))
    @pytest.mark.parametrize("failure_detected", (True, False))
    def test_completed(self, monkeypatch, with_timeout, with_dc_support, failure_detected):
        host = self.mock_host()
        host.ticket = "ITDC-0"

        stage = get_current_stage(host)
        stage.set_data("tickets", ["ITDC-0"])
        stage.set_param("failure_detected", failure_detected)

        if with_dc_support:
            stage.set_temp_data("dc_support", with_dc_support)

        # Profile timeout mustn't affect anything if host has completed the profiling
        if with_timeout:
            stage.status_time = timestamp() - eine_profiling._IN_PROCESS_TIMEOUT

        host.save()

        clients = monkeypatch_clients_for_host(
            monkeypatch, host, profile_status=EineProfileStatus.COMPLETED, profile_name=self.profile, eaas_ticket=False
        )
        handle_host(host)

        assert clients.mock_calls == [call.eine.get_host_status(host.inv, profile=True, location=True)]

        del host.ticket
        mock_complete_current_stage(host)
        self.test.hosts.assert_equal()
        self.test.host_network.assert_equal()

    @pytest.mark.parametrize("with_timeout", (False, True))
    def test_completed_with_ticket(self, monkeypatch, with_timeout):
        host = self.mock_host()
        host.ticket = "ITDC-0"

        stage = get_current_stage(host)
        stage.set_data("tickets", ["ITDC-0"])

        stage.set_temp_data("dc_support", True)

        status_time = timestamp() - eine_profiling._TICKET_CLOSING_TIMEOUT + int(not with_timeout)
        stage.set_temp_data("profile_status_time", status_time)
        stage.set_temp_data("profile_status", EineProfileStatus.COMPLETED)
        stage.status_time = status_time

        host.save()

        clients = monkeypatch_clients_for_host(
            monkeypatch, host, profile_status=EineProfileStatus.COMPLETED, profile_name=self.profile, eaas_ticket=True
        )
        handle_host(host)

        assert clients.mock_calls == [call.eine.get_host_status(host.inv, profile=True, location=True)]

        if with_timeout:
            del host.ticket
            mock_complete_current_stage(host)
        else:
            host.task.status = Stages.EINE_PROFILE + ":stage-mock:running"
            status_message = "EaaS profile completed. Waiting for ticket to close."
            mock_commit_stage_changes(host, status_message=status_message, check_after=eine_profiling._CHECK_PERIOD)
        self.test.hosts.assert_equal()
        self.test.host_network.assert_equal()

    def _mock_retry_or_fail(self, host, monkeypatch, clients, exceed_max_error_count):
        monkeypatch_max_errors(monkeypatch, "profiling.max_errors", exceed_max_error_count)

        handle_host(host)

        assert clients.mock_calls == [call.eine.get_host_status(host.inv, profile=True, location=True)]

        if exceed_max_error_count:
            reason = (
                "Too many errors occurred during processing 'eine-profile' stage of 'ready' task."
                " Last error: Profile failed - it got failed/stage-mock:running status: <no message>."
            )
            mock_fail_current_stage(host, reason=reason)
        else:
            get_current_stage(host).set_data("eine_errors", 1)
            mock_retry_parent_stage(host, check_after=eine_profiling._ERROR_RETRY_DELAY)

        self.test.hosts.assert_equal()
        self.test.host_network.assert_equal()

    def _mock_retry_or_wait(self, host, monkeypatch, clients, exceed_max_error_count, on_exceed):
        monkeypatch_max_errors(monkeypatch, "profiling.max_errors", exceed_max_error_count)

        handle_host(host)

        assert clients.mock_calls == [call.eine.get_host_status(host.inv, profile=True, location=True)]

        if exceed_max_error_count:
            get_current_stage(host).set_data("eine_errors", 1)
            on_exceed()
        else:
            get_current_stage(host).set_data("eine_errors", 1)
            mock_retry_parent_stage(host, check_after=eine_profiling._ERROR_RETRY_DELAY)

        self.test.hosts.assert_equal()
        self.test.host_network.assert_equal()


class TestDropProfile:
    test = None
    host = None

    @pytest.fixture(autouse=True)
    def init(self, test):
        self.test = test
        self.host = self.test.mock_host({"task": mock_task(stage=Stages.DROP_EINE_PROFILE)})

    def test_drop(self, monkeypatch):
        host = self.host
        clients = monkeypatch_clients_for_host(
            monkeypatch, host, profile_status=EineProfileStatus.QUEUED, profile_name="profile-mock"
        )
        handle_host(host)

        assert clients.mock_calls == [
            call.eine.get_host_status(host.inv, profile=True),
            call.eine.assign_profile(host.inv, EINE_NOP_PROFILE, assigned_for=authorization.ISSUER_WALLE),
        ]
        mock_complete_current_stage(host)
        self.test.hosts.assert_equal()

    @pytest.mark.parametrize(
        "profile_status", [EineProfileStatus.FAILED, EineProfileStatus.STOPPED, EineProfileStatus.COMPLETED]
    )
    def test_no_running_profile(self, monkeypatch, profile_status):
        host = self.host
        clients = monkeypatch_clients_for_host(
            monkeypatch, host, profile_status=profile_status, profile_name="profile-mock"
        )
        handle_host(host)

        assert clients.mock_calls == [
            call.eine.get_host_status(host.inv, profile=True),
        ]
        mock_complete_current_stage(host)
        self.test.hosts.assert_equal()

    def test_temporary_failure(self, monkeypatch):
        host = self.host
        clients = monkeypatch_clients_for_host(monkeypatch, host, mock_internal_profile_error=True)

        handle_host(host, suppress_internal_errors=(eine.EineInternalError,))

        assert clients.mock_calls == [call.eine.get_host_status(host.inv, profile=True)]

        mock_stage_internal_error(
            host, error="Error in communication with Einstellung: Mocked internal Einstellung error."
        )
        self.test.hosts.assert_equal()

    def test_persistent_failure(self, monkeypatch):
        host = self.host
        clients = monkeypatch_clients_for_host(monkeypatch, host, mock_persistent_profile_error=True)

        handle_host(host)

        assert clients.mock_calls == [call.eine.get_host_status(host.inv, profile=True)]

        mock_fail_current_stage(host, reason="Einstellung returned an error: Mocked persistent Einstellung error.")
        self.test.hosts.assert_equal()


@pytest.mark.parametrize("status", EineProfileStatus.ALL)
def test_stage_cancellation(monkeypatch, test, status):
    host = test.mock_host({"task": mock_task(stage=Stages.EINE_PROFILE)})

    clients = monkeypatch_clients_for_host(monkeypatch, host, profile_status=status, profile_name="profile-mock")
    cancel_stage(host)

    if status == EineProfileStatus.QUEUED:
        assert clients.mock_calls == [
            call.eine.get_host_status(host.inv, profile=True),
            call.eine.assign_profile(host.inv, EINE_NOP_PROFILE, assigned_for=authorization.ISSUER_WALLE),
        ]
    else:
        assert clients.mock_calls == [call.eine.get_host_status(host.inv, profile=True)]

    test.hosts.assert_equal()


@pytest.mark.usefixtures("monkeypatch_audit_log")
def test_deploy_failed(test, monkeypatch):
    host = test.mock_host(
        {
            "provisioner": PROVISIONER_EINE,
            "config": "config-mock",
            "task": mock_task(
                stage=Stages.EINE_PROFILE,
                stage_params={
                    "operation": EineProfileOperation.DEPLOY,
                    "profile": "config-mock",
                },
                stage_status=eine_profiling._STATUS_PROFILING,
                stage_temp_data={"profile_assign_time": timestamp()},
                stage_terminators={StageTerminals.DEPLOY_FAILED: StageTerminals.DISK_RW_AND_REDEPLOY},
            ),
        }
    )

    clients = monkeypatch_clients_for_host(
        monkeypatch, host, profile_status=EineProfileStatus.FAILED, profile_name="config-mock"
    )
    monkeypatch.setitem(config.get_value("profiling"), "max_errors", 1)
    handle_host(host)

    assert clients.mock_calls == [call.eine.get_host_status(host.inv, profile=True, location=True)]
    mock_schedule_host_redeployment(
        host,
        manual=False,
        config="config-mock",
        provisioner=PROVISIONER_EINE,
        custom_profile_mode=ProfileMode.DISK_RW_TEST,
        check=True,
        reason="Upgrading the task to run highload profile: "
        + "Too many errors occurred during processing 'eine-profile' stage of "
        + "'ready' task. Last error: Profile failed - it got failed/stage-mock:"
        + "running status: <no message>.",
    )

    test.hosts.assert_equal()


@pytest.mark.usefixtures("monkeypatch_audit_log")
def test_upgraded_task_inherits_cms_ignore(test, monkeypatch):
    host = test.mock_host(
        {
            "provisioner": PROVISIONER_EINE,
            "config": "config-mock",
            "task": mock_task(
                stage=Stages.EINE_PROFILE,
                stage_params={
                    "operation": EineProfileOperation.DEPLOY,
                    "profile": "config-mock",
                },
                stage_status=eine_profiling._STATUS_PROFILING,
                stage_temp_data={"profile_assign_time": timestamp()},
                stage_terminators={StageTerminals.DEPLOY_FAILED: StageTerminals.DISK_RW_AND_REDEPLOY},
                ignore_cms=True,
            ),
        }
    )

    clients = monkeypatch_clients_for_host(
        monkeypatch, host, profile_status=EineProfileStatus.FAILED, profile_name="config-mock"
    )
    monkeypatch.setitem(config.get_value("profiling"), "max_errors", 1)
    handle_host(host)

    assert clients.mock_calls == [call.eine.get_host_status(host.inv, profile=True, location=True)]
    mock_schedule_host_redeployment(
        host,
        manual=False,
        config="config-mock",
        provisioner=PROVISIONER_EINE,
        custom_profile_mode=ProfileMode.DISK_RW_TEST,
        check=True,
        ignore_cms=True,
        reason="Upgrading the task to run highload profile: "
        + "Too many errors occurred during processing 'eine-profile' stage of "
        + "'ready' task. Last error: Profile failed - it got failed/stage-mock:"
        + "running status: <no message>.",
    )

    test.hosts.assert_equal()
