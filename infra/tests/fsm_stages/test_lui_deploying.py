"""Tests host deployment via LUI."""

import json
from unittest.mock import call

import pytest

from infra.walle.server.tests.lib.util import (
    TestCase,
    handle_host,
    mock_task,
    monkeypatch_clients_for_host,
    mock_commit_stage_changes,
    check_stage_initialization,
    mock_complete_current_stage,
    mock_retry_parent_stage,
    mock_host_deactivation,
    mock_fail_current_stage,
    monkeypatch_max_errors,
    mock_host_macs,
    mock_stage_internal_error,
    mock_schedule_host_redeployment,
    mock_retry_current_stage,
    monkeypatch_config,
    mock_config_content_dm,
)
from sepelib.core import config
from walle.audit_log import LogEntry
from walle.clients import bot, deploy as deploy_client, inventory
from walle.clients.eine import ProfileMode
from walle.constants import PROVISIONER_LUI, PROVISIONER_EINE, NetworkTarget
from walle.fsm_stages import lui_deploying
from walle.fsm_stages.common import get_current_stage, get_parent_stage, commit_stage_changes, DEFAULT_RETRY_TIMEOUT
from walle.hosts import HostState, DeployConfiguration
from walle.models import timestamp
from walle.operations_log.constants import Operation
from walle.stages import Stage, Stages, StageTerminals
from walle.util import deploy_config
from walle.util.deploy_config import DeployConfigPolicies


@pytest.fixture
def test(request, monkeypatch_timestamp, monkeypatch_audit_log):
    return TestCase.create(request)


@pytest.mark.parametrize("stage", (Stages.ASSIGN_LUI_CONFIG, Stages.LUI_INSTALL, Stages.LUI_REMOVE))
def test_stage_initialization(test, stage):
    check_stage_initialization(test, Stage(name=stage))


class TestGenerateDeployConfigContent:
    _config = "config-mock"

    @pytest.fixture()
    def mock_disk_configuration(self, mp):
        mp.function(bot.get_host_disk_configuration, return_value=bot.HostDiskConf(hdds=2, ssds=1, nvmes=0))

    @pytest.fixture
    def monkeypatch_check_deploy_conf(self, mp):
        mp.function(inventory.check_deploy_configuration)

    @classmethod
    def _get_host(cls, test, deploy_config_policy):
        project = test.mock_project({"id": "project-mock"})

        host = test.mock_host(
            {
                "macs": mock_host_macs(),
                "active_mac": mock_host_macs()[1],
                "state": HostState.ASSIGNED,
                "status": Operation.REDEPLOY.host_status,
                "provisioner": PROVISIONER_LUI,
                "config": cls._config,
                "deploy_network": None,
                "task": mock_task(stage=Stages.GENERATE_CUSTOM_DEPLOY_CONFIG),
                "location": {"switch": "switch-mock"},
                "project": project.id,
            }
        )

        deploy_conf = host.deduce_deploy_configuration(
            requested_config=cls._config, requested_deploy_config_policy=deploy_config_policy
        )[5]
        get_parent_stage(host).set_param("config", deploy_conf)

        return host.save()

    @pytest.mark.parametrize("policy_name", [None] + deploy_config.DeployConfigPolicies.get_all_names())
    @pytest.mark.usefixtures("monkeypatch_check_deploy_conf")
    def test_updates_parent_stage_params_with_policy_results(self, test, mp, policy_name):
        mutated_stage_params = {"config_content_json": "{}", "host_ticket": "SETUP-1"}
        self.mock_deploy_policy(mp, policy_results=mutated_stage_params)

        host = self._get_host(test, policy_name)
        test.audit_log.mock(overrides={"id": host.task.audit_log_id}, save=True)
        handle_host(host)

        mock_complete_current_stage(host, expected_parent_data={"deploy_policy_overrides": mutated_stage_params})

        audit_log_entry = LogEntry.objects.get(id=host.task.audit_log_id)
        assert audit_log_entry.payload == {"deploy_policy_overrides": mutated_stage_params}

        test.hosts.assert_equal()

    @staticmethod
    def mock_deploy_policy(mp, policy_results):
        class PolicyMock(deploy_config.DeployConfigStrategy):
            def generate(self, host, deploy_config_name):
                return policy_results

        mp.method(
            deploy_config.DeployConfigPolicies.get_policy_class,
            obj=deploy_config.DeployConfigPolicies,
            return_value=PolicyMock,
        )


class TestAssignConfig:
    _config = "config-mock"

    @classmethod
    def _get_host(
        cls,
        test,
        hbf_project_id=None,
        config_forced=None,
        config_parent_stage=_config,
        override_config=None,
        config_provisioner=PROVISIONER_LUI,
        host_provisioner=PROVISIONER_LUI,
        host_config=_config,
        host_deploy_network=None,
        certificate_deploy=False,
        config_content=None,
    ):
        project = test.mock_project(
            {"id": "project-mock", "hbf_project_id": hbf_project_id, "certificate_deploy": certificate_deploy}
        )

        host = test.mock_host(
            {
                "macs": mock_host_macs(),
                "state": HostState.ASSIGNED,
                "status": Operation.REDEPLOY.host_status,
                "provisioner": host_provisioner,
                "config": host_config,
                "deploy_network": host_deploy_network,
                "task": mock_task(stage=Stages.ASSIGN_LUI_CONFIG),
                "location": {"switch": "switch-mock"},
                "project": project.id,
            }
        )

        if override_config:
            config = DeployConfiguration(
                config=override_config,
                provisioner=config_provisioner,
                tags=None,
                certificate=False,
                network=NetworkTarget.PROJECT,
                ipxe=True,
                deploy_config_policy=None,
            )
            get_parent_stage(host).set_data("config_override", config)

        if config_parent_stage:
            config = DeployConfiguration(
                config=config_parent_stage,
                provisioner=config_provisioner,
                tags=None,
                certificate=False,
                network=NetworkTarget.PROJECT,
                ipxe=True,
                deploy_config_policy=None,
            )
            get_parent_stage(host).set_param("config", config)

        if config_forced is not None:
            get_parent_stage(host).set_param("config_forced", config_forced)

        if config_content is not None:
            overrides = {"config_content_json": json.dumps(config_content), "config_name": "external"}
            get_parent_stage(host).set_data("deploy_policy_overrides", overrides)

        return host.save()

    def _get_schedule_kwargs(self, hbf_project_id=None, config_content=None):
        schedule_kwargs = {}

        if hbf_project_id is not None:
            schedule_kwargs["project_id"] = hex(hbf_project_id)[2:]

        if config_content is not None:
            schedule_kwargs["config_name"] = "external"
            schedule_kwargs["config_content"] = config_content
        else:
            schedule_kwargs["config_name"] = self._config

        return schedule_kwargs

    @pytest.mark.parametrize("persistent_error", [True, False])
    def test_deploy_client_deploy_status_fails(self, test, monkeypatch, persistent_error):
        host = self._get_host(test)
        clients = monkeypatch_clients_for_host(
            monkeypatch,
            host,
            mock_internal_deploy_error=not persistent_error,
            mock_persistent_deploy_error=persistent_error,
        )

        handle_host(host, suppress_internal_errors=(deploy_client.DeployClientError,))

        assert clients.mock_calls == [call.deploy.get_deploy_status(host.name)]

        mock_stage_internal_error(host, "Mocked {} LUI error.".format("persistent" if persistent_error else "internal"))
        test.hosts.assert_equal()

    def test_deploy_client_internal_error(self, test, monkeypatch):
        host = self._get_host(test)
        clients = monkeypatch_clients_for_host(monkeypatch, host, mock_internal_deploy_error=True)
        clients.deploy.get_deploy_status.side_effect = deploy_client.HostDoesntExistError("mock host doesn't exist")

        handle_host(host, suppress_internal_errors=(deploy_client.DeployInternalError,))

        assert clients.mock_calls == [
            call.deploy.get_deploy_status(host.name),
            call.deploy.schedule_redeploy(host.name, mock_host_macs(), **self._get_schedule_kwargs()),
        ]

        mock_stage_internal_error(host, "Mocked internal LUI error.")
        test.hosts.assert_equal()

    def test_deploy_client_persistent_error(self, test, monkeypatch):
        host = self._get_host(test)
        clients = monkeypatch_clients_for_host(monkeypatch, host, mock_persistent_deploy_error=True)

        # this should not affect the stage
        clients.deploy.get_deploy_status.side_effect = deploy_client.HostDoesntExistError("mock host doesn't exist")

        handle_host(host)

        assert clients.mock_calls == [
            call.deploy.get_deploy_status(host.name),
            call.deploy.schedule_redeploy(host.name, mock_host_macs(), **self._get_schedule_kwargs()),
        ]

        get_parent_stage(host).set_temp_data("start_fail_count", 0)
        mock_retry_current_stage(
            host,
            expected_name=Stages.ASSIGN_LUI_CONFIG,
            error="Mocked persistent LUI error.",
            check_after=lui_deploying._ERROR_RETRY_PERIOD,
        )
        test.hosts.assert_equal()

    def test_deploy_config_does_not_exist(self, test, monkeypatch):
        host = self._get_host(test)
        clients = monkeypatch_clients_for_host(monkeypatch, host, deploy_config_list=[])

        clients.deploy.get_deploy_status.side_effect = deploy_client.HostDoesntExistError("mock host doesn't exist")

        handle_host(host)

        assert clients.mock_calls == []

        error_message = "Deploy config config-mock does not exist in Setup/LUI."
        mock_retry_current_stage(
            host, Stages.ASSIGN_LUI_CONFIG, error=error_message, check_after=lui_deploying._ERROR_RETRY_PERIOD
        )
        test.hosts.assert_equal()

    @pytest.mark.parametrize("config_forced_value", [None, False])
    def test_deploy_configuration_refreshed_after_config_not_exist_error(self, test, monkeypatch, config_forced_value):
        host = self._get_host(test, host_config="host-config-mock", config_forced=config_forced_value)
        clients = monkeypatch_clients_for_host(monkeypatch, host, deploy_config_list=[])

        clients.deploy.get_deploy_status.side_effect = deploy_client.HostDoesntExistError("mock host doesn't exist")

        handle_host(host)

        assert clients.mock_calls == []

        get_parent_stage(host).set_data("config_override", host.get_deploy_configuration())
        error_message = "Deploy config config-mock does not exist in Setup/LUI."
        mock_retry_current_stage(
            host, Stages.ASSIGN_LUI_CONFIG, error=error_message, check_after=lui_deploying._ERROR_RETRY_PERIOD
        )
        test.hosts.assert_equal()

    def test_deploy_configuration_not_refreshed_after_error_if_config_forced(self, test, monkeypatch):
        host = self._get_host(test, host_config="host-config-mock", config_forced=True)
        clients = monkeypatch_clients_for_host(monkeypatch, host, deploy_config_list=[])

        clients.deploy.get_deploy_status.side_effect = deploy_client.HostDoesntExistError("mock host doesn't exist")

        handle_host(host)

        assert clients.mock_calls == []

        error_message = "Deploy config config-mock does not exist in Setup/LUI."
        mock_retry_current_stage(
            host, Stages.ASSIGN_LUI_CONFIG, error=error_message, check_after=lui_deploying._ERROR_RETRY_PERIOD
        )
        test.hosts.assert_equal()

    def test_new_deploy_provisioner_checked_after_config_refreshing(self, test, monkeypatch):
        host = self._get_host(test, host_config="host-config-mock", host_provisioner=PROVISIONER_EINE)
        clients = monkeypatch_clients_for_host(monkeypatch, host, deploy_config_list=[])

        clients.deploy.get_deploy_status.side_effect = deploy_client.HostDoesntExistError("mock host doesn't exist")

        handle_host(host)

        assert clients.mock_calls == []

        reason = (
            "Redeploy task failed. Host deploy provisioner has changed from lui to eine during the task."
            " Failing automated deploying."
        )
        mock_fail_current_stage(host, reason=reason)
        test.hosts.assert_equal()

    @pytest.mark.parametrize("param", ["certificate", "network"])
    def test_new_deploy_config_major_changes(self, test, monkeypatch, param):
        if param == "network":
            host = self._get_host(test, host_deploy_network=NetworkTarget.SERVICE)
        else:
            host = self._get_host(test, certificate_deploy=True)

        clients = monkeypatch_clients_for_host(monkeypatch, host, deploy_config_list=[])

        clients.deploy.get_deploy_status.side_effect = deploy_client.HostDoesntExistError("mock host doesn't exist")

        handle_host(host)

        assert clients.mock_calls == []

        get_parent_stage(host).set_data("config_override", host.get_deploy_configuration())
        error_message = "Deploy config config-mock does not exist in Setup/LUI."
        mock_retry_parent_stage(host, error=error_message, check_after=lui_deploying._ERROR_RETRY_PERIOD)
        test.hosts.assert_equal()

    def test_deploy_provisioner_mismatch(self, test, monkeypatch):
        host = self._get_host(test, config_provisioner=PROVISIONER_EINE)
        clients = monkeypatch_clients_for_host(monkeypatch, host)

        handle_host(host)

        assert clients.mock_calls == []

        reason = (
            "Redeploy task failed. Host deploy provisioner has changed from lui to eine during the task."
            " Failing automated deploying."
        )
        mock_fail_current_stage(host, reason=reason)
        test.hosts.assert_equal()

    def _successfully_handle(self, test, monkeypatch, host, config_content=None):
        clients = monkeypatch_clients_for_host(monkeypatch, host, deploy_config=self._config)

        handle_host(host, suppress_internal_errors=(deploy_client.DeployInternalError,))

        assert clients.mock_calls == [
            call.deploy.get_deploy_status(host.name),
            call.deploy.schedule_redeploy(
                host.name, mock_host_macs(), **self._get_schedule_kwargs(config_content=config_content)
            ),
        ]

        parent_stage = get_parent_stage(host)
        parent_stage.set_temp_data("deploy_macs", host.macs)
        parent_stage.set_temp_data("start_fail_count", 0)

        mock_complete_current_stage(host)
        test.hosts.assert_equal()

    def test_deploy_configured_with_parent_stage_params(self, test, monkeypatch):
        host = self._get_host(test, config_parent_stage=self._config, override_config=None)
        self._successfully_handle(test, monkeypatch, host)

    def test_deploy_configured_config_content(self, test, monkeypatch):
        config_content = mock_config_content_dm()
        host = self._get_host(
            test, config_parent_stage=self._config, override_config=None, config_content=config_content
        )
        self._successfully_handle(test, monkeypatch, host, config_content)

    @pytest.mark.parametrize("config_parent_stage", ["fake", None])
    def test_deploy_configured_with_parent_stage_data(self, test, monkeypatch, config_parent_stage):
        host = self._get_host(test, config_parent_stage=config_parent_stage, override_config=self._config)

        self._successfully_handle(test, monkeypatch, host)

    @pytest.mark.parametrize("certificate_owner", ["root:root", None])
    @pytest.mark.parametrize("hbf_project_id", [int("604", 16), None])
    def test_deploy_with_certificate(self, test, monkeypatch, certificate_owner, hbf_project_id):
        host = self._get_host(test, hbf_project_id=hbf_project_id)
        clients = monkeypatch_clients_for_host(monkeypatch, host)

        monkeypatch_config(monkeypatch, "certificator.host_certificate_path", "/etc/certs/iss.pem")
        monkeypatch_config(monkeypatch, "certificator.host_certificate_owner", certificate_owner)

        parent_stage = get_parent_stage(host)
        parent_stage.set_data("certificate", "-- BEGIN CERTIFICATE-MOCK --")
        parent_stage.set_temp_data("deploy_macs", host.macs)
        parent_stage.set_temp_data("start_fail_count", 0)
        commit_stage_changes(host)

        handle_host(host)
        mock_complete_current_stage(host)

        mock_certificate_data = [
            {"content": "-- BEGIN CERTIFICATE-MOCK --", "path": config.get_value("certificator.host_certificate_path")}
        ]

        if certificate_owner is not None:
            mock_certificate_data[0]["owner"] = certificate_owner

        assert clients.mock_calls == [
            call.deploy.get_deploy_status(host.name),
            call.deploy.schedule_redeploy(
                host.name,
                mock_host_macs(),
                private_data=mock_certificate_data,
                **self._get_schedule_kwargs(hbf_project_id=hbf_project_id)
            ),
        ]

        test.hosts.assert_equal()


class TestInstallation:
    _macs = mock_host_macs()
    _config = "config-mock"
    _stage_status = "provisioner-status-mock"

    @classmethod
    def _get_host(
        cls,
        test,
        stage_status=_stage_status,
        stage_status_time=None,
        stage_temp_data=None,
        stage_terminators=None,
        stage_data=None,
        mock_task_kwargs={},
        mock_host_kwargs={},
    ):
        stage = Stage(
            name=Stages.LUI_INSTALL,
            temp_data=stage_temp_data,
            terminators=stage_terminators,
            data=stage_data,
        )

        host = test.mock_host(
            dict(
                macs=cls._macs,
                task=mock_task(stage=stage, **mock_task_kwargs),
                provisioner=PROVISIONER_LUI,
                config=cls._config,
                **mock_host_kwargs
            )
        )

        parent_stage = get_parent_stage(host)
        parent_stage.set_param("config", host.get_deploy_configuration())
        parent_stage.set_temp_data("deploy_macs", cls._macs)
        parent_stage.set_temp_data("start_fail_count", 0)
        cls._set_stage_status(host, stage_status, status_time=stage_status_time)

        return host.save()

    @pytest.mark.parametrize(
        "fixture",
        (
            lambda: (None, timestamp(), "installing", timestamp(), True),
            lambda: ("pending", timestamp(), "installing", timestamp(), True),
            lambda: ("booting", timestamp() - 1, "booting", timestamp(), True),
            lambda: ("booting", timestamp(), "booting", timestamp(), False),
            lambda: ("booting", timestamp(), "booting", timestamp() - 1, False),
        ),
    )
    def test_updates_task_status_only_when_changed(self, test, monkeypatch, fixture):
        current_status, current_status_time, new_status, new_status_time, update = fixture()
        host = self._get_host(test, stage_status=current_status, stage_status_time=current_status_time)

        monkeypatch_clients_for_host(
            monkeypatch,
            host,
            deploy_status=deploy_client.STATUS_BOOTING,
            lui_deploy_status=new_status,
            status_time=new_status_time,
        )
        handle_host(host)
        mock_commit_stage_changes(
            host,
            status=new_status if update else None,
            check_after=lui_deploying._INSTALLATION_CHECK_PERIOD,
            inc_revision=int(update),
        )

        test.hosts.assert_equal()

    @pytest.mark.parametrize("first_time", (True, False))
    def test_booting(self, test, monkeypatch, first_time):
        host = self._get_host(test, stage_status=None if first_time else self._stage_status)
        clients = monkeypatch_clients_for_host(monkeypatch, host, deploy_status=deploy_client.STATUS_BOOTING)

        handle_host(host)

        mock_commit_stage_changes(
            host,
            status="provisioner-status-mock",
            check_after=lui_deploying._INSTALLATION_CHECK_PERIOD,
            inc_revision=int(first_time),
        )

        assert clients.mock_calls == [call.deploy.get_deploy_status(host.name)]
        test.hosts.assert_equal()

    def test_boot_timeout(self, test, monkeypatch, exceed_max_error_count):
        host = self._get_host(test, stage_status_time=timestamp() - lui_deploying._PXE_BOOT_TIMEOUT)
        stage = get_current_stage(host)
        assert not stage.has_data("lui_errors")
        clients = monkeypatch_clients_for_host(
            monkeypatch,
            host,
            deploy_status=deploy_client.STATUS_BOOTING,
            status_time=timestamp() - lui_deploying._PXE_BOOT_TIMEOUT,
        )

        self._mock_handle_installation_error(
            host,
            test,
            monkeypatch,
            clients,
            exceed_max_error_count,
            with_power_check=True,
            error="Host failed to boot from PXE.",
        )

    def test_pending_timeout(self, test, exceed_max_error_count, monkeypatch):
        host = self._get_host(test, stage_status_time=timestamp() - lui_deploying._PENDING_STATUS_TIMEOUT)
        stage = get_current_stage(host)
        assert not stage.has_data("pxe_errors")
        clients = monkeypatch_clients_for_host(
            monkeypatch,
            host,
            deploy_status=deploy_client.STATUS_PENDING,
            status_time=timestamp() - lui_deploying._PENDING_STATUS_TIMEOUT,
        )

        self._mock_handle_installation_error(
            host,
            test,
            monkeypatch,
            clients,
            exceed_max_error_count,
            with_power_check=True,
            error="Host failed to boot from PXE:"
            " either it's plugged into an invalid VLAN or there is a hardware problem.",
        )

    @pytest.mark.parametrize("deploy_status", (deploy_client.STATUS_PREPARING, deploy_client.STATUS_DEPLOYING))
    def test_installation_first_run(self, test, monkeypatch, deploy_status):
        lui_deploy_status = "INSTALL/{}".format(deploy_status.upper())
        host = self._get_host(test)

        stage = get_current_stage(host)
        assert not stage.has_temp_data("install_time")

        clients = monkeypatch_clients_for_host(
            monkeypatch,
            host,
            deploy_status=deploy_status,
            status_time=timestamp() - 1,
            lui_deploy_status=lui_deploy_status,
        )

        handle_host(host)

        assert clients.mock_calls == [call.deploy.get_deploy_status(host.name)]

        stage.set_temp_data("install_time", timestamp())
        mock_commit_stage_changes(
            host, status=lui_deploy_status.lower(), check_after=lui_deploying._INSTALLATION_CHECK_PERIOD, inc_revision=1
        )

        test.hosts.assert_equal()

    @pytest.mark.parametrize("deploy_status", (deploy_client.STATUS_PREPARING, deploy_client.STATUS_DEPLOYING))
    def test_installation_general_run(self, test, monkeypatch, deploy_status):
        lui_deploy_status = "INSTALL/{}".format(deploy_status.upper())
        host = self._get_host(
            test,
            stage_status=lui_deploy_status,
            stage_status_time=timestamp() - 1,
            stage_temp_data={"install_time": timestamp() - 1},
        )

        clients = monkeypatch_clients_for_host(
            monkeypatch,
            host,
            deploy_status=deploy_status,
            status_time=timestamp() - 1,
            lui_deploy_status=lui_deploy_status,
        )

        handle_host(host)

        assert clients.mock_calls == [call.deploy.get_deploy_status(host.name)]

        mock_commit_stage_changes(host, check_after=lui_deploying._INSTALLATION_CHECK_PERIOD)

        test.hosts.assert_equal()

    @pytest.mark.parametrize("deploy_status", deploy_client.STATUSES)
    @pytest.mark.parametrize(
        "change_kwargs", ({"deploy_config": "other-config"}, {"deploy_macs": ["ff:ff:ff:ff:ff:ff"]})
    )
    def test_unexpected_config_change(self, test, monkeypatch, deploy_status, exceed_max_error_count, change_kwargs):
        host = self._get_host(test)
        redeploy_fail_count = 99
        clients = monkeypatch_clients_for_host(
            monkeypatch, host, deploy_status=deploy_status, redeploy_fail_count=redeploy_fail_count, **change_kwargs
        )

        if "deploy_config" in change_kwargs:
            error_message = "Host has suddenly changed it's deployment config from config-mock to other-config."
        else:
            error_message = "Host has suddenly changed it's deployment MAC address from {} to {}.".format(
                ['00:00:00:00:00:10', '00:00:00:00:00:00', '00:00:00:00:00:01'], ['ff:ff:ff:ff:ff:ff']
            )

        self._mock_handle_installation_error(
            host, test, monkeypatch, clients, exceed_max_error_count, error=error_message
        )

    def test_deploy_client_error(self, test, monkeypatch):
        host = self._get_host(test)
        monkeypatch_clients_for_host(monkeypatch, host, mock_internal_deploy_error=True)

        handle_host(host, suppress_internal_errors=(deploy_client.DeployInternalError,))
        mock_stage_internal_error(host, "Mocked internal LUI error.")

        test.hosts.assert_equal()

    @pytest.mark.parametrize(
        "host_kwargs, expected_config_str",
        [
            ({}, "config-mock"),
            ({"deploy_config_policy": DeployConfigPolicies.PASSTHROUGH}, "config-mock"),
            (
                {"deploy_config_policy": DeployConfigPolicies.DISKMANAGER},
                "config-mock (config policy: {})".format(DeployConfigPolicies.DISKMANAGER),
            ),
        ],
    )
    def test_config_inappropriate(self, test, mp, host_kwargs, expected_config_str):
        host = self._get_host(test, mock_host_kwargs=host_kwargs)

        clients = monkeypatch_clients_for_host(mp, host, deploy_status=deploy_client.STATUS_CONFIG_INAPPROPRIATE)
        disk_conf = bot.HostDiskConf(hdds=1, ssds=2, nvmes=3)
        mp.function(bot.get_host_disk_configuration, return_value=disk_conf)

        handle_host(host)

        expected_error = """LUI encountered wrong deploy config ({}) and will try to reload it every day
Usually it means that config doesn't support host's disk configuration ({})""".format(
            expected_config_str, disk_conf
        )
        mock_commit_stage_changes(
            host,
            status="provisioner-status-mock",
            check_after=lui_deploying._CONFIG_INAPPROPRIATE_CHECK_PERIOD,
            error=expected_error,
        )

        assert clients.mock_calls == [call.deploy.get_deploy_status(host.name)]
        test.hosts.assert_equal()

    def test_config_inappropriate_timeout(self, test, mp, exceed_max_error_count):
        host = self._get_host(test, stage_status_time=timestamp() - lui_deploying._INSTALLATION_TIMEOUT)
        stage = get_current_stage(host)
        assert not stage.has_data("lui_errors")
        clients = monkeypatch_clients_for_host(
            mp,
            host,
            deploy_status=deploy_client.STATUS_CONFIG_INAPPROPRIATE,
            status_time=timestamp() - lui_deploying._INSTALLATION_TIMEOUT,
        )
        disk_conf = bot.HostDiskConf(hdds=1, ssds=2, nvmes=3)
        mp.function(bot.get_host_disk_configuration, return_value=disk_conf)

        self._mock_handle_installation_error(
            host,
            test,
            mp,
            clients,
            exceed_max_error_count,
            with_power_check=True,
            error="Deploying process has timed out.",
        )

    @pytest.mark.parametrize(
        "deploy_status", (deploy_client.STATUS_PREPARING, deploy_client.STATUS_DEPLOYING, deploy_client.STATUS_FAILED)
    )
    @pytest.mark.parametrize("exceed_max_failure_count", (False, True))
    def test_installation_timeout(self, test, monkeypatch, deploy_status, exceed_max_failure_count):
        host = self._get_host(
            test,
            stage_data={"lui_retries": 0},
            stage_temp_data={"install_time": timestamp() - lui_deploying._INSTALLATION_TIMEOUT},
        )
        host.save()
        clients = monkeypatch_clients_for_host(monkeypatch, host, deploy_status=deploy_status)
        error_message = "Deploying process has timed out."
        self._mock_handle_installation_error(
            host, test, monkeypatch, clients, exceed_max_failure_count, error=error_message, with_power_check=True
        )

    @pytest.mark.parametrize("exceed_max_failure_count", (False, True))
    def test_installation_max_failures_exceeded(self, test, monkeypatch, exceed_max_failure_count):
        max_failures = 1
        host = self._get_host(
            test,
            stage_temp_data={"install_time": timestamp()},
            stage_data={"lui_errors": 1 if exceed_max_failure_count else 0, "lui_retries": 0},
        )

        monkeypatch.setitem(config.get_value("deployment"), "max_failures", max_failures)
        monkeypatch.setitem(config.get_value("deployment"), "max_errors", 1)
        clients = monkeypatch_clients_for_host(
            monkeypatch,
            host,
            deploy_status=deploy_client.STATUS_FAILED,
            deploy_fail_count=max_failures if exceed_max_failure_count else max_failures - 1,
        )

        handle_host(host)

        expected = [call.deploy.get_deploy_status(host.name)]

        if exceed_max_failure_count:
            get_current_stage(host).set_data("lui_errors", 2)
            get_current_stage(host).set_data("lui_retries", 1)
            mock_retry_parent_stage(host, error="Setup failed 1 times.", check_after=lui_deploying._ERROR_RETRY_PERIOD)
        else:
            mock_commit_stage_changes(
                host, status="provisioner-status-mock", check_after=lui_deploying._INSTALLATION_CHECK_PERIOD
            )

        assert clients.mock_calls == expected
        test.hosts.assert_equal()

    def test_retry_limit_exceeded(self, test, monkeypatch):
        host = self._get_host(
            test,
            stage_temp_data={"install_time": timestamp()},
            stage_data={"lui_errors": 1, "lui_retries": 1},
        )
        monkeypatch.setitem(config.get_value("deployment"), "max_failures", 1)
        monkeypatch.setitem(config.get_value("deployment"), "max_errors", 1)
        monkeypatch.setitem(config.get_value("deployment"), "max_retries", 1)

        clients = monkeypatch_clients_for_host(
            monkeypatch, host, deploy_status=deploy_client.STATUS_FAILED, deploy_fail_count=1
        )

        handle_host(host)

        assert clients.mock_calls == [call.deploy.get_deploy_status(host.name), call.deploy.deactivate(host.name)]

        mock_host_deactivation(host, "Setup failed 1 times.")

        test.hosts.assert_equal()

    def test_installation_failure_upgrade_task(self, test, monkeypatch):
        max_failures = 1
        host = self._get_host(
            test,
            stage_temp_data={"install_time": timestamp()},
            stage_terminators={StageTerminals.DEPLOY_FAILED: StageTerminals.DISK_RW_AND_REDEPLOY},
            stage_data={"lui_errors": 1},
        )

        monkeypatch.setitem(config.get_value("deployment"), "max_failures", max_failures)
        monkeypatch.setitem(config.get_value("deployment"), "max_errors", 1)
        clients = monkeypatch_clients_for_host(
            monkeypatch, host, deploy_status=deploy_client.STATUS_FAILED, deploy_fail_count=max_failures
        )

        handle_host(host)

        expected = [call.deploy.get_deploy_status(host.name)]
        mock_schedule_host_redeployment(
            host,
            manual=False,
            provisioner=PROVISIONER_LUI,
            custom_profile_mode=ProfileMode.DISK_RW_TEST,
            check=True,
            reason='Upgrading the task to run highload profile: Setup failed 1 times.',
        )

        assert clients.mock_calls == expected
        test.hosts.assert_equal()

    def test_upgraded_task_inherits_ignore_cms_value(self, test, monkeypatch):
        host = self._get_host(
            test,
            stage_temp_data={"install_time": timestamp()},
            stage_terminators={StageTerminals.DEPLOY_FAILED: StageTerminals.DISK_RW_AND_REDEPLOY},
            mock_task_kwargs={"ignore_cms": True},
            stage_data={"lui_errors": 1},
        )

        max_failures = 1
        monkeypatch.setitem(config.get_value("deployment"), "max_failures", max_failures)
        monkeypatch.setitem(config.get_value("deployment"), "max_errors", 1)
        monkeypatch_clients_for_host(
            monkeypatch, host, deploy_status=deploy_client.STATUS_FAILED, deploy_fail_count=max_failures
        )

        handle_host(host)

        mock_schedule_host_redeployment(
            host,
            manual=False,
            provisioner=PROVISIONER_LUI,
            custom_profile_mode=ProfileMode.DISK_RW_TEST,
            check=True,
            ignore_cms=True,
            reason='Upgrading the task to run highload profile: Setup failed 1 times.',
        )

        test.hosts.assert_equal()

    def test_installation_invalid_status(self, test, monkeypatch):
        host = self._get_host(test)
        monkeypatch_clients_for_host(monkeypatch, host, redeploy_fail_count=99)
        handle_host(host)

        error = "Deploying process got an unexpected status 'invalid' ( LUI status)."
        mock_commit_stage_changes(host, error=error, check_after=lui_deploying._ERROR_CHECK_INTERVAL)

        test.hosts.assert_equal()

    def test_bmc_reset(self, test, monkeypatch):
        host = self._get_host(test, stage_status=lui_deploying._STATUS_RESETTING_BMC)
        clients = monkeypatch_clients_for_host(monkeypatch, host, redeploy_fail_count=99)

        handle_host(host)
        mock_retry_parent_stage(host, error=None, check_after=DEFAULT_RETRY_TIMEOUT)

        test.hosts.assert_equal()
        clients.hardware.bmc_reset.assert_called_once_with()

    def test_retry_stage(self, test, monkeypatch):
        host = self._get_host(test)
        clients = monkeypatch_clients_for_host(monkeypatch, host, deploy_status=deploy_client.STATUS_RETRY)
        expected = [call.deploy.get_deploy_status(host.name)]

        handle_host(host)

        assert clients.mock_calls == expected

        mock_retry_parent_stage(host, check_after=DEFAULT_RETRY_TIMEOUT)

        test.hosts.assert_equal()

    @pytest.mark.parametrize("with_timeout", (False, True))
    def test_completion(self, test, monkeypatch, with_timeout):
        stage_temp_data = {"boot_time": 666}
        if with_timeout:
            # Installation timeout mustn't affect anything if host has completed the installation
            stage_temp_data["reboot_time"] = timestamp() - lui_deploying._INSTALLATION_TIMEOUT

        host = self._get_host(test, stage_temp_data=stage_temp_data)
        clients = monkeypatch_clients_for_host(
            monkeypatch, host, deploy_status=deploy_client.STATUS_COMPLETED, deploy_macs=self._macs
        )

        handle_host(host)

        assert clients.mock_calls == [call.deploy.get_deploy_status(host.name)]
        mock_complete_current_stage(host)

        assert host.active_mac is None
        test.hosts.assert_equal()

    @staticmethod
    def _set_stage_status(host, status, status_time=None):
        stage = get_current_stage(host)
        stage.status_time = timestamp() if status_time is None else status_time

        if status is None:
            del stage.status
            host.task.status = stage.name
        else:
            stage.status = status.lower()
            host.task.status = ":".join([stage.name, stage.status])

    @staticmethod
    def _mock_handle_installation_error(
        host, test, monkeypatch, clients, exceed_max_error_count, error=None, with_power_check=False
    ):
        monkeypatch_max_errors(monkeypatch, "deployment.max_errors", exceed_max_error_count)

        handle_host(host)

        expected_client_calls = [call.deploy.get_deploy_status(host.name)]
        if with_power_check:
            expected_client_calls.append(call.hardware.is_power_on())
        assert clients.mock_calls == expected_client_calls

        get_current_stage(host).set_data("lui_errors", 1)
        if exceed_max_error_count:
            get_current_stage(host).set_data("lui_retries", 1)
            mock_retry_parent_stage(host, error=error, check_after=lui_deploying._ERROR_RETRY_PERIOD)
        else:
            mock_commit_stage_changes(host, status=lui_deploying._STATUS_RESETTING_BMC, check_now=True, error=error)

        test.hosts.assert_equal()


class TestServerSetup:
    @pytest.fixture(autouse=True)
    def init(self, test):
        self.test = test
        stage = Stage(name=Stages.LUI_SETUP, params={"config": "config-mock"})
        self.host = test.mock_host(
            {
                "task": mock_task(stage=stage),
            }
        )

    def test_completion_with_hostname(self, monkeypatch):
        host = self.host
        clients = monkeypatch_clients_for_host(monkeypatch, host)
        handle_host(host)
        mock_complete_current_stage(host)

        assert clients.mock_calls == [call.deploy.setup(host.name, host.macs, "config-mock")]
        self.test.hosts.assert_equal()

    def test_completion_without_hostname(self, monkeypatch):
        # check that stage does nothing in this case.
        # Because hostname is a required parameter.
        # as a reliable host identity.
        host = self.host
        del host.name
        host.save()

        clients = monkeypatch_clients_for_host(monkeypatch, host)
        handle_host(host)
        mock_fail_current_stage(host, reason="Host name is required. This is a bug in Wall-E.")

        assert clients.mock_calls == []
        self.test.hosts.assert_equal()

    def test_lui_api_error(self, monkeypatch):
        host = self.host

        monkeypatch_clients_for_host(monkeypatch, host, mock_internal_deploy_error=True)
        handle_host(host, suppress_internal_errors=(deploy_client.DeployInternalError,))
        mock_stage_internal_error(host, "Mocked internal LUI error.")

        self.test.hosts.assert_equal()


class TestDeactivate:
    @pytest.fixture(autouse=True)
    def init(self, test):
        self.test = test
        stage = Stage(name=Stages.LUI_DEACTIVATE)
        self.host = test.mock_host(
            {
                "task": mock_task(stage=stage),
            }
        )

    def test_completion_with_hostname(self, monkeypatch):
        host = self.host
        clients = monkeypatch_clients_for_host(monkeypatch, host)
        handle_host(host)
        mock_complete_current_stage(host)

        assert clients.mock_calls == [call.deploy.deactivate(host.name)]
        self.test.hosts.assert_equal()

    def test_completion_without_hostname(self, monkeypatch):
        # check that stage does nothing in this case.
        # Because MAC is out of our control, we can not use it
        # as a reliable host identity.
        host = self.host
        host.name = None
        host.save()

        clients = monkeypatch_clients_for_host(monkeypatch, host)
        handle_host(host)
        mock_complete_current_stage(host)

        assert clients.mock_calls == []
        self.test.hosts.assert_equal()

    def test_lui_api_error(self, monkeypatch):
        host = self.host

        monkeypatch_clients_for_host(monkeypatch, host, mock_internal_deploy_error=True)
        handle_host(host, suppress_internal_errors=(deploy_client.DeployInternalError,))
        mock_stage_internal_error(host, "Mocked internal LUI error.")

        self.test.hosts.assert_equal()


class TestServerDelete:
    @pytest.fixture(autouse=True)
    def init(self, test):
        self.test = test
        stage = Stage(name=Stages.LUI_REMOVE)
        self.host = test.mock_host(
            {
                "task": mock_task(stage=stage),
            }
        )

    def test_completion_with_hostname(self, monkeypatch):
        host = self.host
        clients = monkeypatch_clients_for_host(monkeypatch, host)
        handle_host(host)
        mock_complete_current_stage(host)

        assert clients.mock_calls == [call.deploy.remove(host.name)]
        self.test.hosts.assert_equal()

    def test_completion_without_hostname(self, monkeypatch):
        # check that stage does nothing in this case.
        # Because MAC is out of our control, we can not use it
        # as a reliable host identity.
        host = self.host
        host.name = None
        host.save()

        clients = monkeypatch_clients_for_host(monkeypatch, host)
        handle_host(host)
        mock_complete_current_stage(host)

        assert clients.mock_calls == []
        self.test.hosts.assert_equal()

    def test_lui_api_error(self, monkeypatch):
        host = self.host

        monkeypatch_clients_for_host(monkeypatch, host, mock_internal_deploy_error=True)
        handle_host(host, suppress_internal_errors=(deploy_client.DeployInternalError,))
        mock_stage_internal_error(host, "Mocked internal LUI error.")

        self.test.hosts.assert_equal()
