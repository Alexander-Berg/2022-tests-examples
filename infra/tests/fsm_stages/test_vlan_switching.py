"""Tests VLAN switching."""

from unittest.mock import call, Mock

import pytest

from infra.walle.server.tests.lib.util import (
    TestCase,
    patch,
    mock_task,
    handle_host,
    mock_fail_current_stage,
    mock_commit_stage_changes,
    mock_complete_current_stage,
    mock_skip_current_stage,
    check_stage_initialization,
    mock_retry_current_stage,
    monkeypatch_config,
    monkeypatch_request,
    mock_response,
)
from walle import network
from walle.clients import racktables, vlan_toggler
from walle.constants import NetworkTarget, YCNetworkState, VLAN_SCHEME_CLOUD
from walle.errors import NoInformationError, InvalidHostConfiguration
from walle.fsm_stages import vlan_switching, common
from walle.fsm_stages.common import get_current_stage, DEFAULT_RETRY_TIMEOUT
from walle.hosts import deploy_configuration, HostStatus
from walle.models import timestamp
from walle.network import HostNetworkLocationInfo
from walle.stages import Stages, Stage, StageTerminals
from walle.util.misc import drop_none

MTN_PRJ = "0x1388"
FOREIGN_MTN_PRJ = "0x604"


class ProjectMatch:
    def __init__(self, project):
        self._project = project

    def __eq__(self, other):
        if isinstance(other, self._project.__class__):
            return self._project.id == other.id

        return NotImplemented

    def __repr__(self):
        return "<ProjectMatch({!r})>".format(self._project)


def mtn_prj(val):
    return int(val, 16)


def mock_vlan_toggler_error_request(mp, status_code):
    return monkeypatch_request(mp, mock_response(status_code=status_code))


@pytest.fixture()
def test(request, monkeypatch_timestamp):
    return TestCase.create(request)


@pytest.fixture()
def mock_mtn_vlan_config(mp, request):
    return mp.function(network.get_host_expected_vlans, return_value=network._VlanConfig([1, 2], 1, MTN_PRJ))


@pytest.fixture()
def mock_mtn_vlan_config_for_parking_vlan(mp, request):
    return mp.function(network.get_host_expected_vlans, return_value=network._VlanConfig([999], 1, MTN_PRJ))


@pytest.fixture()
def mock_automated_vlan_config(mp):
    return mp.function(network.get_host_expected_vlans, return_value=network._VlanConfig([1, 2], 1, None))


@pytest.fixture()
def mock_no_vlan_config(mp):
    return mp.function(network.get_host_expected_vlans, return_value=None)


def test_stage_initialization(test):
    stage = Stage(name=Stages.SWITCH_VLANS, params={"network": NetworkTarget.PROJECT})
    check_stage_initialization(test, stage, status=vlan_switching._STATUS_PREPARING)


class TestPrepare:
    @staticmethod
    def _create_host_and_stage(
        test,
        stage_params=None,
        parent_stage_params=None,
        parent_stage_data=None,
        skip_on_switch_missing=False,
        tricky_platform=False,
    ):
        if skip_on_switch_missing:
            stage_terminators = {StageTerminals.SWITCH_MISSING: StageTerminals.SKIP}
        else:
            stage_terminators = None

        project = test.mock_project({"id": "some-id", "owned_vlans": [1, 2], "hbf_project_id": mtn_prj(MTN_PRJ)})

        if tricky_platform:
            host = test.mock_host(
                {
                    "project": project.id,
                    "platform": {"system": "PROLIANT DL160G6", "board": "PROLIANT DL160G6"},
                    "task": mock_task(
                        stage=Stages.SWITCH_VLANS,
                        stage_status=vlan_switching._STATUS_PREPARING,
                        stage_params=stage_params,
                        stage_terminators=stage_terminators,
                        parent_stage_params=parent_stage_params,
                        parent_stage_data=parent_stage_data,
                    ),
                }
            )
        else:
            host = test.mock_host(
                {
                    "project": project.id,
                    "task": mock_task(
                        stage=Stages.SWITCH_VLANS,
                        stage_status=vlan_switching._STATUS_PREPARING,
                        stage_params=stage_params,
                        stage_terminators=stage_terminators,
                        parent_stage_params=parent_stage_params,
                        parent_stage_data=parent_stage_data,
                    ),
                }
            )

        return project, host

    @staticmethod
    def _create_host_with_custom_vlan_scheme(test, stage_params=None, vlan_scheme=None):
        project = test.mock_project({"id": "some-id", "owned_vlans": [1, 2], "vlan_scheme": vlan_scheme})
        return test.mock_host(
            {
                "project": project.id,
                "task": mock_task(
                    stage=Stages.SWITCH_VLANS, stage_params=stage_params, stage_status=vlan_switching._STATUS_PREPARING
                ),
            }
        )

    # test transition phase: absent params is obsolete, only `network` should be processed
    @pytest.mark.parametrize("stage_params", [None, {"network": NetworkTarget.SERVICE}])
    def test_no_vlan_configuration(self, test, stage_params):
        host = self._create_host_with_custom_vlan_scheme(test, stage_params)

        handle_host(host)

        mock_complete_current_stage(host, expected_data={"vlan_success": True})
        test.hosts.assert_equal()

    @patch("walle.network.get_current_host_switch_port", side_effect=NoInformationError("Mocked error"))
    def test_no_switch_info_skip(self, get_current_host_switch_port, test):
        project, host = self._create_host_and_stage(
            test, stage_params={"network": NetworkTarget.PROJECT}, skip_on_switch_missing=True
        )

        handle_host(host)
        get_current_host_switch_port.assert_called_once_with(host)

        mock_skip_current_stage(host)

        test.hosts.assert_equal()

    @patch("walle.network.get_current_host_switch_port", side_effect=NoInformationError("Mocked error"))
    def test_no_switch_info_retry(self, get_current_host_switch_port, test):
        project, host = self._create_host_and_stage(test, stage_params={"network": NetworkTarget.PROJECT})

        handle_host(host)
        get_current_host_switch_port.assert_called_once_with(host)

        mock_retry_current_stage(
            host,
            Stages.SWITCH_VLANS,
            vlan_switching._STATUS_PREPARING,
            error="Can't switch vlans: host's switch is unknown.",
            check_after=DEFAULT_RETRY_TIMEOUT,
        )

        test.hosts.assert_equal()

    @patch(
        "walle.network.get_current_host_switch_port",
        return_value=HostNetworkLocationInfo(switch="switch-mock", port="port-mock", source="source-mock", timestamp=0),
    )
    def test_invalid_host_configuration_retry(self, get_current_host_switch_port, mp, test):
        mp.function(network.get_host_expected_vlans, side_effect=InvalidHostConfiguration("Mocked error"))

        project, host = self._create_host_and_stage(test, stage_params={"network": NetworkTarget.PROJECT})
        handle_host(host)

        mock_retry_current_stage(
            host,
            Stages.SWITCH_VLANS,
            vlan_switching._STATUS_PREPARING,
            error="Mocked error",
            check_after=DEFAULT_RETRY_TIMEOUT,
        )

        get_current_host_switch_port.assert_called_once_with(host)
        test.hosts.assert_equal()

    @patch(
        "walle.network.get_current_host_switch_port",
        return_value=HostNetworkLocationInfo(switch="switch-mock", port="port-mock", source="source-mock", timestamp=0),
    )
    @pytest.mark.parametrize(
        ["stage_params", "vlans", "native_vlan"],
        [
            ({"network": NetworkTarget.PROJECT}, [1, 2], 1),
            ({"network": NetworkTarget.SERVICE}, [542], 542),
            ({"network": NetworkTarget.PARKING}, [999], 999),
        ],
    )
    def test_prepare_switching_to_requested_target_no_mtn(
        self, get_current_host_switch_port, test, mock_automated_vlan_config, stage_params, vlans, native_vlan
    ):
        project, host = self._create_host_and_stage(test, stage_params=stage_params)

        handle_host(host)

        mock_automated_vlan_config.assert_called_once_with(host, project=ProjectMatch(project))
        get_current_host_switch_port.assert_called_once_with(host)

        get_current_stage(host).set_temp_data(
            "switched_port_info",
            {
                "switch": "switch-mock",
                "port": "port-mock",
                "vlans": vlans,
                "native_vlan": native_vlan,
                "mtn_project_id": None,
            },
        )
        mock_commit_stage_changes(host, status=vlan_switching._STATUS_CHECKING_VLANS, check_now=True)
        test.hosts.assert_equal()

    @patch(
        "walle.network.get_current_host_switch_port",
        return_value=HostNetworkLocationInfo(switch="switch-mock", port="port-mock", source="source-mock", timestamp=0),
    )
    @pytest.mark.parametrize("network", [NetworkTarget.PROJECT, NetworkTarget.SERVICE, NetworkTarget.PARKING])
    def test_prepare_switching_to_requested_target_with_mtn(
        self, get_current_host_switch_port, test, mock_mtn_vlan_config, network
    ):
        project, host = self._create_host_and_stage(test, stage_params={"network": network})

        handle_host(host)

        mock_mtn_vlan_config.assert_called_once_with(host, project=ProjectMatch(project))
        get_current_host_switch_port.assert_called_once_with(host)

        # we do not switch anywhere when project configured for mtn. Always use project config.
        get_current_stage(host).set_temp_data(
            "switched_port_info",
            {
                "switch": "switch-mock",
                "port": "port-mock",
                "vlans": [1, 2],
                "native_vlan": 1,
                "mtn_project_id": MTN_PRJ,
            },
        )
        mock_commit_stage_changes(host, status=vlan_switching._STATUS_CHECKING_PROJECT, check_now=True)
        test.hosts.assert_equal()

    @patch(
        "walle.network.get_current_host_switch_port",
        return_value=HostNetworkLocationInfo(switch="switch-mock", port="port-mock", source="source-mock", timestamp=0),
    )
    @pytest.mark.parametrize("network", [NetworkTarget.PROJECT, NetworkTarget.PARKING])
    def test_prepare_switching_to_requested_target_with_mtn_proliant(
        self, get_current_host_switch_port, test, mock_mtn_vlan_config, network
    ):
        project, host = self._create_host_and_stage(test, stage_params={"network": network}, tricky_platform=True)

        handle_host(host)

        mock_mtn_vlan_config.assert_called_once_with(host, project=ProjectMatch(project))
        get_current_host_switch_port.assert_called_once_with(host)

        # we do not switch anywhere when project configured for mtn. Always use project config.
        get_current_stage(host).set_temp_data(
            "switched_port_info",
            {
                "switch": "switch-mock",
                "port": "port-mock",
                "vlans": [1, 2],
                "native_vlan": 1,
                "mtn_project_id": MTN_PRJ,
            },
        )
        mock_commit_stage_changes(host, status=vlan_switching._STATUS_CHECKING_PROJECT, check_now=True)
        test.hosts.assert_equal()

    @patch(
        "walle.network.get_current_host_switch_port",
        return_value=HostNetworkLocationInfo(switch="switch-mock", port="port-mock", source="source-mock", timestamp=0),
    )
    @pytest.mark.parametrize("network", [NetworkTarget.SERVICE, NetworkTarget.DEPLOY])
    def test_prepare_switching_to_service_network_with_mtn_proliant(
        self, get_current_host_switch_port, test, mock_automated_vlan_config, network
    ):
        project, host = self._create_host_and_stage(test, stage_params={"network": network}, tricky_platform=True)

        handle_host(host)

        get_current_host_switch_port.assert_called_once_with(host)

        get_current_stage(host).set_temp_data(
            "switched_port_info",
            {
                "switch": "switch-mock",
                "port": "port-mock",
                "vlans": [542],
                "native_vlan": 542,
                "mtn_project_id": None,
            },
        )
        mock_commit_stage_changes(host, status=vlan_switching._STATUS_CHECKING_VLANS, check_now=True)
        test.hosts.assert_equal()

    @patch(
        "walle.network.get_current_host_switch_port",
        return_value=HostNetworkLocationInfo(switch="switch-mock", port="port-mock", source="source-mock", timestamp=0),
    )
    @pytest.mark.parametrize("network", [NetworkTarget.PROJECT, NetworkTarget.SERVICE, NetworkTarget.PARKING])
    def test_prepare_switching_to_parking_vlan_with_mtn(
        self, get_current_host_switch_port, test, network, mock_mtn_vlan_config_for_parking_vlan
    ):
        project, host = self._create_host_and_stage(test, stage_params={"network": network})

        handle_host(host)

        get_current_host_switch_port.assert_called_once_with(host)

        get_current_stage(host).set_temp_data(
            "switched_port_info",
            {"switch": "switch-mock", "port": "port-mock", "vlans": [999], "native_vlan": 1, "mtn_project_id": MTN_PRJ},
        )
        mock_commit_stage_changes(host, status=vlan_switching._STATUS_CHECKING_VLANS, check_now=True)
        test.hosts.assert_equal()

    @patch(
        "walle.network.get_current_host_switch_port",
        return_value=HostNetworkLocationInfo(switch="switch-mock", port="port-mock", source="source-mock", timestamp=0),
    )
    @pytest.mark.parametrize("native_vlan", (None, 2))
    def test_prepare_switching_to_custom_vlans(
        self, get_current_host_switch_port, mock_automated_vlan_config, test, native_vlan
    ):
        project, host = self._create_host_and_stage(
            test, stage_params=drop_none({"vlans": [1, 2], "native_vlan": native_vlan})
        )

        handle_host(host)

        get_current_host_switch_port.assert_called_once_with(host)

        get_current_stage(host).set_temp_data(
            "switched_port_info",
            {
                "switch": "switch-mock",
                "port": "port-mock",
                "vlans": [1, 2],
                "native_vlan": native_vlan,
                "mtn_project_id": None,
            },
        )
        mock_commit_stage_changes(host, status=vlan_switching._STATUS_CHECKING_VLANS, check_now=True)
        test.hosts.assert_equal()

    @patch(
        "walle.network.get_current_host_switch_port",
        return_value=HostNetworkLocationInfo(switch="switch-mock", port="port-mock", source="source-mock", timestamp=0),
    )
    @pytest.mark.parametrize("extra_vlans", (None, [123, 456], []))
    def test_prepare_with_extra_vlans(
        self, get_current_host_switch_port, mock_automated_vlan_config, test, extra_vlans
    ):

        stage_params = {"network": NetworkTarget.PROJECT}
        if extra_vlans:
            stage_params["extra_vlans"] = extra_vlans

        project, host = self._create_host_and_stage(test, stage_params=stage_params)

        expected_vlans = [1, 2]
        if extra_vlans:
            project.owned_vlans += [123, 456]
            project.save()
            expected_vlans += [123, 456]

        handle_host(host)

        mock_automated_vlan_config.assert_called_once_with(host, project=ProjectMatch(project))
        get_current_host_switch_port.assert_called_once_with(host)

        get_current_stage(host).set_temp_data(
            "switched_port_info",
            {
                "switch": "switch-mock",
                "port": "port-mock",
                "vlans": expected_vlans,
                "native_vlan": 1,
                "mtn_project_id": None,
            },
        )
        mock_commit_stage_changes(host, status=vlan_switching._STATUS_CHECKING_VLANS, check_now=True)
        test.hosts.assert_equal()

    @patch(
        "walle.network.get_current_host_switch_port",
        return_value=HostNetworkLocationInfo(switch="switch-mock", port="port-mock", source="source-mock", timestamp=0),
    )
    @pytest.mark.parametrize(
        ["parent_stage_params", "parent_stage_data"],
        [
            (None, {"config_override": deploy_configuration(None, None, network=NetworkTarget.SERVICE)}),
            ({"config": deploy_configuration(None, None, network=NetworkTarget.SERVICE)}, None),
            (
                {"config": deploy_configuration(None, None, network=NetworkTarget.PARKING)},
                {"config_override": deploy_configuration(None, None, network=NetworkTarget.SERVICE)},
            ),
        ],
    )
    def test_prepare_with_deploying_network(
        self, get_current_host_switch_port, mock_automated_vlan_config, test, parent_stage_params, parent_stage_data
    ):
        project, host = self._create_host_and_stage(
            test,
            stage_params={"network": NetworkTarget.DEPLOY},
            parent_stage_params=parent_stage_params,
            parent_stage_data=parent_stage_data,
        )

        handle_host(host)

        mock_automated_vlan_config.assert_called_once_with(host, project=ProjectMatch(project))
        get_current_host_switch_port.assert_called_once_with(host)

        get_current_stage(host).set_temp_data(
            "switched_port_info",
            {
                "switch": "switch-mock",
                "port": "port-mock",
                "vlans": [542],
                "native_vlan": 542,
                "mtn_project_id": None,
            },
        )
        mock_commit_stage_changes(host, status=vlan_switching._STATUS_CHECKING_VLANS, check_now=True)
        test.hosts.assert_equal()

    @patch(
        "walle.network.get_current_host_switch_port",
        return_value=HostNetworkLocationInfo(switch="switch-mock", port="port-mock", source="source-mock", timestamp=0),
    )
    @pytest.mark.parametrize(
        "network, yc_target",
        (
            (NetworkTarget.PROJECT, YCNetworkState.PROD),
            (NetworkTarget.SERVICE, YCNetworkState.SETUP),
            (NetworkTarget.PARKING, YCNetworkState.SETUP),
            (NetworkTarget.DEPLOY, YCNetworkState.PROD),
        ),
    )
    def test_prepare_with_yc_vlan_schema(self, get_current_host_switch_port, test, network, yc_target):
        host = self._create_host_with_custom_vlan_scheme(
            test, stage_params={"network": network}, vlan_scheme=VLAN_SCHEME_CLOUD
        )

        handle_host(host)

        get_current_host_switch_port.assert_called_once_with(host)

        get_current_stage(host).set_temp_data(
            "switched_port_info", {"switch": "switch-mock", "port": "port-mock", "target_yc_state": yc_target}
        )
        mock_commit_stage_changes(host, status=vlan_switching._STATUS_CHECKING_YC_STATE, check_now=True)
        test.hosts.assert_equal()


@patch("walle.clients.racktables.switch_mtn_project")
@patch("walle.clients.racktables.delete_mtn_project")
class TestCheckingMtnProject:
    @staticmethod
    def _create_host_with_stage(test, mtn_project_id, temp_data=None):
        project = test.mock_project({"id": "some-id", "owned_vlans": [1, 2], "hbf_project_id": mtn_prj(MTN_PRJ)})

        temp_data = drop_none(temp_data or {})
        temp_data["switched_port_info"] = {
            "switch": "switch-mock",
            "port": "port-mock",
            "vlans": [1, 2],
            "native_vlan": 1,
            "mtn_project_id": mtn_project_id,
        }

        return test.mock_host(
            {
                "project": project.id,
                "task": mock_task(
                    stage=Stages.SWITCH_VLANS,
                    stage_params={"network": NetworkTarget.PROJECT},
                    stage_status=vlan_switching._STATUS_CHECKING_PROJECT,
                    stage_temp_data=temp_data,
                ),
            }
        )

    @pytest.mark.xfail(reason="ignoring this error, see https://st.yandex-team.ru/WALLE-1349#1509027383000")
    @patch(
        "walle.clients.racktables.get_port_project_status", side_effect=racktables.MtnNotSupportedForSwitchError("ERR")
    )
    def test_mtn_not_supported_but_needed(self, get_port_status, delete_mtn_project, switch_mtn_project, test):
        host = self._create_host_with_stage(test, MTN_PRJ)
        handle_host(host)

        get_port_status.assert_called_once_with("switch-mock", "port-mock")
        assert not switch_mtn_project.called
        assert not delete_mtn_project.called

        error = (
            "Can not switch 'port-mock' port of 'switch-mock' switch to '{}' mtn project: "
            "RackTables returned an error: ERR.".format(MTN_PRJ)
        )
        mock_retry_current_stage(
            host, Stages.SWITCH_VLANS, vlan_switching._STATUS_PREPARING, error=error, check_after=DEFAULT_RETRY_TIMEOUT
        )
        test.hosts.assert_equal()

    @patch("walle.clients.racktables.get_port_project_status", side_effect=racktables.MtnNotSupportedForSwitchError(""))
    @pytest.mark.parametrize("vlans_synced", [True, False, None])
    def test_mtn_not_supported_neither_needed(
        self, get_port_status, delete_mtn_project, switch_mtn_project, test, vlans_synced
    ):
        host = self._create_host_with_stage(test, mtn_project_id=None, temp_data={"vlans_synced": vlans_synced})
        handle_host(host)

        get_port_status.assert_called_once_with("switch-mock", "port-mock")
        assert not switch_mtn_project.called
        assert not delete_mtn_project.called

        if vlans_synced:
            mock_complete_current_stage(host, expected_data={"vlan_success": True})
        else:
            get_current_stage(host).set_temp_data("project_id_synced", True)
            mock_commit_stage_changes(host, status=vlan_switching._STATUS_CHECKING_VLANS, check_now=True)

        test.hosts.assert_equal()

    @patch("walle.clients.racktables.get_port_project_status", return_value=(None, True))
    @pytest.mark.parametrize("vlans_synced", [True, False, None])
    def test_no_project_on_port_and_should_not_be(
        self, get_port_status, delete_mtn_project, switch_mtn_project, test, vlans_synced
    ):
        host = self._create_host_with_stage(test, mtn_project_id=None, temp_data={"vlans_synced": vlans_synced})
        handle_host(host)

        get_port_status.assert_called_once_with("switch-mock", "port-mock")
        assert not switch_mtn_project.called
        assert not delete_mtn_project.called

        if vlans_synced:
            mock_complete_current_stage(host, expected_data={"vlan_success": True})
        else:
            get_current_stage(host).set_temp_data("project_id_synced", True)
            mock_commit_stage_changes(host, status=vlan_switching._STATUS_CHECKING_VLANS, check_now=True)

        test.hosts.assert_equal()

    @patch("walle.clients.racktables.get_port_project_status", return_value=(None, True))
    def test_no_project_on_port_but_should_be(self, get_port_status, delete_mtn_project, switch_mtn_project, test):
        host = self._create_host_with_stage(test, MTN_PRJ)
        handle_host(host)

        get_port_status.assert_called_once_with("switch-mock", "port-mock")
        assert not switch_mtn_project.called
        assert not delete_mtn_project.called

        get_current_stage(host).set_temp_data("current_project_id", None)
        mock_commit_stage_changes(host, status=vlan_switching._STATUS_SWITCHING_PROJECT, check_now=True)
        test.hosts.assert_equal()

    @pytest.mark.parametrize("synced", [True, False])
    def test_project_is_on_port_but_should_not(self, delete_mtn_project, switch_mtn_project, mp, test, synced):
        host = self._create_host_with_stage(test, mtn_project_id=None)
        get_port_status = mp.function(racktables.get_port_project_status, return_value=(FOREIGN_MTN_PRJ, synced))

        handle_host(host)

        get_port_status.assert_called_once_with("switch-mock", "port-mock")
        assert not switch_mtn_project.called  # made in other handler
        assert not delete_mtn_project.called

        get_current_stage(host).set_temp_data("current_project_id", FOREIGN_MTN_PRJ)
        mock_commit_stage_changes(host, status=vlan_switching._STATUS_SWITCHING_PROJECT, check_now=True)
        test.hosts.assert_equal()

    @pytest.mark.parametrize("synced", [True, False])
    def test_wrong_project_on_port(self, delete_mtn_project, switch_mtn_project, mp, test, synced):
        host = self._create_host_with_stage(test, MTN_PRJ)
        get_port_status = mp.function(racktables.get_port_project_status, return_value=(FOREIGN_MTN_PRJ, synced))

        handle_host(host)

        get_port_status.assert_called_once_with("switch-mock", "port-mock")
        assert not switch_mtn_project.called  # made in other handler
        assert not delete_mtn_project.called

        get_current_stage(host).set_temp_data("current_project_id", FOREIGN_MTN_PRJ)
        mock_commit_stage_changes(host, status=vlan_switching._STATUS_SWITCHING_PROJECT, check_now=True)
        test.hosts.assert_equal()

    @patch("walle.clients.racktables.get_port_project_status", return_value=(MTN_PRJ, False))
    def test_project_is_not_synced(self, get_port_status, delete_mtn_project, switch_mtn_project, test):
        host = self._create_host_with_stage(test, MTN_PRJ)
        handle_host(host)

        get_port_status.assert_called_once_with("switch-mock", "port-mock")
        assert not switch_mtn_project.called
        assert not delete_mtn_project.called

        mock_commit_stage_changes(
            host, status=vlan_switching._STATUS_WAITING_PROJECT_SYNC, check_after=vlan_switching._SYNC_POLLING_PERIOD
        )
        test.hosts.assert_equal()

    @patch("walle.clients.racktables.get_port_project_status", return_value=(MTN_PRJ, True))
    @pytest.mark.parametrize("vlans_synced", [True, False, None])
    def test_project_is_synced(self, get_port_status, delete_mtn_project, switch_mtn_project, test, vlans_synced):
        host = self._create_host_with_stage(test, MTN_PRJ, temp_data={"vlans_synced": vlans_synced})
        handle_host(host)

        get_port_status.assert_called_once_with("switch-mock", "port-mock")
        assert not switch_mtn_project.called
        assert not delete_mtn_project.called

        if vlans_synced:
            mock_complete_current_stage(host, expected_data={"vlan_success": True})
        else:
            get_current_stage(host).set_temp_data("project_id_synced", True)
            mock_commit_stage_changes(host, status=vlan_switching._STATUS_CHECKING_VLANS, check_now=True)

        test.hosts.assert_equal()

    @patch("walle.clients.racktables.get_port_project_status", side_effect=racktables.PersistentRacktablesError("ERR"))
    def test_error_getting_info(self, get_port_status, delete_mtn_project, switch_mtn_project, test):
        host = self._create_host_with_stage(test, MTN_PRJ)
        handle_host(host)

        get_port_status.assert_called_once_with("switch-mock", "port-mock")
        assert not switch_mtn_project.called
        assert not delete_mtn_project.called

        error = (
            "Can not switch 'port-mock' port of 'switch-mock' switch to '{}' mtn project: "
            "RackTables returned an error: ERR.".format(MTN_PRJ)
        )
        mock_retry_current_stage(
            host, Stages.SWITCH_VLANS, vlan_switching._STATUS_PREPARING, error=error, check_after=DEFAULT_RETRY_TIMEOUT
        )
        test.hosts.assert_equal()


class SwitchMtnProjectTestCase:
    def _create_host(self, test):
        raise NotImplementedError

    def _assert_racktables_client_called(self, delete_mtn_project, switch_mtn_project):
        raise NotImplementedError

    def _racktables_error_message(self):
        raise NotImplementedError

    @staticmethod
    def _create_host_with_stage(test, mtn_project_id):
        project = test.mock_project({"id": "some-id", "owned_vlans": [1, 2], "hbf_project_id": mtn_prj(MTN_PRJ)})
        stage_port_info = {
            "current_project_id": FOREIGN_MTN_PRJ,
            "switched_port_info": {
                "switch": "switch-mock",
                "port": "port-mock",
                "vlans": [1, 2],
                "native_vlan": 1,
                "mtn_project_id": mtn_project_id,
            },
        }

        return test.mock_host(
            {
                "project": project.id,
                "task": mock_task(
                    stage=Stages.SWITCH_VLANS,
                    stage_params={"network": NetworkTarget.PROJECT},
                    stage_status=vlan_switching._STATUS_SWITCHING_PROJECT,
                    stage_temp_data=stage_port_info,
                ),
            }
        )

    @patch("walle.clients.racktables.switch_mtn_project")
    @patch("walle.clients.racktables.delete_mtn_project")
    def test_switch_project_id(self, delete_mtn_project, switch_mtn_project, test):
        host = self._create_host(test)
        handle_host(host)

        self._assert_racktables_client_called(delete_mtn_project, switch_mtn_project)

        mock_commit_stage_changes(
            host, status=vlan_switching._STATUS_WAITING_PROJECT_SYNC, check_after=vlan_switching._SYNC_POLLING_PERIOD
        )
        test.hosts.assert_equal()

    @patch("walle.clients.racktables.switch_mtn_project", side_effect=racktables.PersistentRacktablesError("ERROR"))
    @patch("walle.clients.racktables.delete_mtn_project", side_effect=racktables.PersistentRacktablesError("ERROR"))
    def test_proceed_with_racktables_error(self, delete_mtn_project, switch_mtn_project, test):
        host = self._create_host(test)
        handle_host(host)

        mock_retry_current_stage(
            host,
            Stages.SWITCH_VLANS,
            vlan_switching._STATUS_PREPARING,
            error=self._racktables_error_message(),
            check_after=DEFAULT_RETRY_TIMEOUT,
        )
        test.hosts.assert_equal()


class TestSettingMtnProject(SwitchMtnProjectTestCase):
    @classmethod
    def _create_host(cls, test):
        return cls._create_host_with_stage(test, mtn_project_id=MTN_PRJ)

    def _assert_racktables_client_called(self, delete_mtn_project, switch_mtn_project):
        switch_mtn_project.assert_called_once_with("switch-mock", "port-mock", MTN_PRJ)
        assert not delete_mtn_project.called

    def _racktables_error_message(self):
        return (
            "Can not switch 'port-mock' port of 'switch-mock' switch "
            "to '{}' mtn project: "
            "RackTables returned an error: ERROR.".format(MTN_PRJ)
        )


class TestUnsettingMtnProject(SwitchMtnProjectTestCase):
    @classmethod
    def _create_host(cls, test):
        return cls._create_host_with_stage(test, mtn_project_id=None)

    def _assert_racktables_client_called(self, delete_mtn_project, switch_mtn_project):
        delete_mtn_project.assert_called_once_with("switch-mock", "port-mock", FOREIGN_MTN_PRJ)
        assert not switch_mtn_project.called

    def _racktables_error_message(self):
        return (
            "Can not switch 'port-mock' port of 'switch-mock' switch "
            "to 1,2 VLANs with native VLAN set to 1: "
            "RackTables returned an error: ERROR."
        )


class TestWaitingMtnProjectSync:
    @staticmethod
    def _create_host_with_stage(test, mtn_project_id, stage_age=None, retry_count=None, temp_data=None):
        project = test.mock_project({"id": "some-id", "owned_vlans": [1, 2], "hbf_project_id": mtn_prj(MTN_PRJ)})
        temp_data = drop_none(temp_data or {})

        temp_data["current_project_id"] = (FOREIGN_MTN_PRJ,)
        temp_data["switched_port_info"] = {
            "switch": "switch-mock",
            "port": "port-mock",
            "vlans": [1, 2],
            "native_vlan": 1,
            "mtn_project_id": mtn_project_id,
        }

        if stage_age:
            stage_status_time = timestamp() - stage_age
        else:
            stage_status_time = None

        stage_data = None
        if retry_count is not None:
            stage_data = {"retries": retry_count}

        return test.mock_host(
            {
                "project": project.id,
                "task": mock_task(
                    stage=Stages.SWITCH_VLANS,
                    stage_status=vlan_switching._STATUS_WAITING_PROJECT_SYNC,
                    stage_params={"network": NetworkTarget.PROJECT},
                    stage_temp_data=temp_data,
                    stage_data=stage_data,
                    stage_status_time=stage_status_time,
                ),
            }
        )

    @patch("walle.clients.racktables.get_port_project_status", return_value=(None, True))
    @pytest.mark.parametrize("vlans_synced", [True, False, None])
    def test_project_unset(self, get_port_status, test, vlans_synced):
        host = self._create_host_with_stage(test, mtn_project_id=None, temp_data={"vlans_synced": vlans_synced})
        handle_host(host)

        get_port_status.assert_called_once_with("switch-mock", "port-mock")

        if vlans_synced:
            mock_complete_current_stage(host, expected_data={"vlan_success": True})
        else:
            get_current_stage(host).set_temp_data("project_id_synced", True)
            mock_commit_stage_changes(host, status=vlan_switching._STATUS_CHECKING_VLANS, check_now=True)
        test.hosts.assert_equal()

    @patch("walle.clients.racktables.get_port_project_status", return_value=(MTN_PRJ, False))
    def test_project_set_not_yet_synced(self, get_port_status, test):
        host = self._create_host_with_stage(test, mtn_project_id=MTN_PRJ)
        handle_host(host)

        get_port_status.assert_called_once_with("switch-mock", "port-mock")

        mock_commit_stage_changes(host, check_after=vlan_switching._SYNC_POLLING_PERIOD)
        test.hosts.assert_equal()

    @patch("walle.clients.racktables.get_port_project_status", return_value=(MTN_PRJ, True))
    @pytest.mark.parametrize("vlans_synced", [True, False, None])
    def test_project_set_and_synced(self, get_port_status, test, vlans_synced):
        host = self._create_host_with_stage(test, mtn_project_id=MTN_PRJ, temp_data={"vlans_synced": vlans_synced})
        handle_host(host)

        get_port_status.assert_called_once_with("switch-mock", "port-mock")

        if vlans_synced:
            mock_complete_current_stage(host, expected_data={"vlan_success": True})
        else:
            get_current_stage(host).set_temp_data("project_id_synced", True)
            mock_commit_stage_changes(host, status=vlan_switching._STATUS_CHECKING_VLANS, check_now=True)

        test.hosts.assert_equal()

    @patch("walle.clients.racktables.get_port_project_status", return_value=(FOREIGN_MTN_PRJ, False))
    def test_project_switched_suddenly_retry(self, get_port_status, test):
        host = self._create_host_with_stage(test, mtn_project_id=MTN_PRJ)
        handle_host(host)

        get_port_status.assert_called_once_with("switch-mock", "port-mock")

        get_current_stage(host).set_data("retries", 1)
        error = (
            "Failed to switch 'port-mock' port of 'switch-mock' switch to '{}' mtn project: "
            "port changed it's project to unexpected value '{}'. Retrying.".format(MTN_PRJ, FOREIGN_MTN_PRJ)
        )
        mock_retry_current_stage(
            host,
            Stages.SWITCH_VLANS,
            check_after=vlan_switching.DEFAULT_RETRY_TIMEOUT,
            expected_status=vlan_switching._STATUS_PREPARING,
            error=error,
        )
        test.hosts.assert_equal()

    @patch("walle.clients.racktables.get_port_project_status", return_value=(FOREIGN_MTN_PRJ, False))
    def test_project_switched_suddenly_fail(self, get_port_status, test, mp):
        monkeypatch_config(mp, "vlan_switching.max_errors", 1)
        host = self._create_host_with_stage(test, mtn_project_id=MTN_PRJ, retry_count=1)
        handle_host(host)

        get_port_status.assert_called_once_with("switch-mock", "port-mock")

        error = (
            "Too many errors occurred during processing 'switch-vlans' stage of 'ready' task."
            " Last error: Failed to switch 'port-mock' port of 'switch-mock' switch to '{}' mtn project:"
            " port changed it's project to unexpected value '{}'.".format(MTN_PRJ, FOREIGN_MTN_PRJ)
        )
        mock_fail_current_stage(host, reason=error)
        test.hosts.assert_equal()

    @patch("walle.clients.racktables.get_port_project_status", return_value=(MTN_PRJ, False))
    def test_project_sync_wait_timeout_report_error(self, get_port_status, test):
        host = self._create_host_with_stage(test, mtn_project_id=MTN_PRJ, stage_age=vlan_switching._SWITCHING_TIMEOUT)
        handle_host(host)

        error = (
            "MTN project switching process is taking too long. "
            "It is probably a failure on the racktables's side. "
            "Please, contact with nocdev-bugs@yandex-team.ru. "
            "Operation is switching 'port-mock' port of 'switch-mock' switch to '{}' mtn project".format(MTN_PRJ)
        )
        mock_commit_stage_changes(host, error=error, check_after=vlan_switching._INCREASED_SYNC_POLLING_PERIOD)

        get_port_status.assert_called_once_with("switch-mock", "port-mock")
        test.hosts.assert_equal()

    @patch("walle.clients.racktables.get_port_project_status", return_value=(MTN_PRJ, False))
    def test_project_sync_wait_timeout_fail_task(self, get_port_status, test):
        host = self._create_host_with_stage(
            test, mtn_project_id=MTN_PRJ, stage_age=vlan_switching._ULTIMATE_WAIT_TIMEOUT
        )
        handle_host(host)

        error = (
            "MTN project switching process is taking too long."
            " It is probably a failure on the racktables's side."
            " Please, contact with nocdev-bugs@yandex-team.ru."
            " Operation is switching 'port-mock' port of 'switch-mock' switch"
            " to '{}' mtn project".format(MTN_PRJ)
        )
        mock_fail_current_stage(host, reason=error)

        get_port_status.assert_called_once_with("switch-mock", "port-mock")
        test.hosts.assert_equal()

    @patch("walle.clients.racktables.get_port_project_status", side_effect=racktables.PersistentRacktablesError("ERR"))
    def test_racktables_persistent_error(self, get_port_status, test):
        host = self._create_host_with_stage(test, mtn_project_id=MTN_PRJ, stage_age=vlan_switching._SWITCHING_TIMEOUT)
        handle_host(host)

        get_port_status.assert_called_once_with("switch-mock", "port-mock")

        error = (
            "Failed to switch 'port-mock' port of 'switch-mock' switch to '{}' mtn project: "
            "RackTables returned an error: ERR.".format(MTN_PRJ)
        )
        mock_retry_current_stage(
            host, Stages.SWITCH_VLANS, vlan_switching._STATUS_PREPARING, error=error, check_after=DEFAULT_RETRY_TIMEOUT
        )
        test.hosts.assert_equal()


@patch("walle.clients.racktables.switch_vlans")
class TestCheckingVlans:
    @staticmethod
    def _create_host_and_stage(test, mtn_project_id=None, temp_data=None):
        project = test.mock_project({"id": "some-id", "owned_vlans": [1, 2]})

        temp_data = drop_none(temp_data or {})
        temp_data["switched_port_info"] = {
            "switch": "switch-mock",
            "port": "port-mock",
            "vlans": [1, 2],
            "native_vlan": 1,
            "mtn_project_id": mtn_project_id,
        }

        return test.mock_host(
            {
                "project": project.id,
                "task": mock_task(
                    stage=Stages.SWITCH_VLANS,
                    stage_params={"network": NetworkTarget.PROJECT},
                    stage_status=vlan_switching._STATUS_CHECKING_VLANS,
                    stage_temp_data=temp_data,
                ),
            }
        )

    @patch("walle.clients.racktables.get_port_vlan_status", return_value=(None, None, None))
    def test_checking_not_applied(self, get_port_status, switch_vlans, test):
        host = self._create_host_and_stage(test)
        handle_host(host)

        get_port_status.assert_called_once_with("switch-mock", "port-mock")
        assert not switch_vlans.called

        mock_commit_stage_changes(host, status=vlan_switching._STATUS_VLAN_AVAILABILITY, check_now=True)
        test.hosts.assert_equal()

    @patch("walle.clients.racktables.get_port_vlan_status", return_value=([1, 2], 1, False))
    def test_checking_already_applied(self, get_port_status, switch_vlans, test):
        host = self._create_host_and_stage(test)
        handle_host(host)

        get_port_status.assert_called_once_with("switch-mock", "port-mock")
        assert not switch_vlans.called

        mock_commit_stage_changes(
            host, status=vlan_switching._STATUS_WAITING_VLANS_SYNC, check_after=vlan_switching._SYNC_POLLING_PERIOD
        )
        test.hosts.assert_equal()

    @patch("walle.clients.racktables.get_port_vlan_status", return_value=([1, 2], 1, True))
    def test_checking_already_applied_and_synced(self, get_port_status, switch_vlans, test):
        host = self._create_host_and_stage(test)
        handle_host(host)

        get_port_status.assert_called_once_with("switch-mock", "port-mock")
        assert not switch_vlans.called

        mock_commit_stage_changes(host, status=vlan_switching._STATUS_WAITING_NETWORKS)

        test.hosts.assert_equal()

    @patch(
        "walle.clients.racktables.get_port_vlan_status",
        side_effect=racktables.PersistentRacktablesError("Mocked error"),
    )
    def test_checking_with_racktables_error(self, get_port_status, switch_vlans, test):
        host = self._create_host_and_stage(test)
        handle_host(host)

        get_port_status.assert_called_once_with("switch-mock", "port-mock")
        assert not switch_vlans.called

        error = (
            "Can not switch 'port-mock' port of 'switch-mock' switch "
            "to 1,2 VLANs with native VLAN set to 1: "
            "RackTables returned an error: Mocked error."
        )
        mock_retry_current_stage(
            host, Stages.SWITCH_VLANS, vlan_switching._STATUS_PREPARING, error=error, check_after=DEFAULT_RETRY_TIMEOUT
        )
        test.hosts.assert_equal()


class TestVlanAvailability:
    @staticmethod
    def _create_host_and_stage(test, network=NetworkTarget.PROJECT):
        return test.mock_host(
            {
                "task": mock_task(
                    stage=Stages.SWITCH_VLANS,
                    stage_params={"network": network},
                    stage_status=vlan_switching._STATUS_VLAN_AVAILABILITY,
                    stage_temp_data={
                        "switched_port_info": {
                            "switch": "switch-mock",
                            "port": "port-mock",
                            "vlans": [1, 2],
                            "native_vlan": 1,
                        }
                    },
                )
            }
        )

    @patch("walle.clients.racktables.is_vlan_available", side_effect=[True, False])
    def test_vlan_not_available(self, is_vlan_available, test):
        host = self._create_host_and_stage(test)
        handle_host(host)
        assert is_vlan_available.mock_calls == [call("switch-mock", 1), call("switch-mock", 2)]

        expected_error_message = (
            "VLAN {} is not available in the domain of {} switch. "
            "This is most probably an error in network configuration. "
            "Please, contact with nocrequests@yandex-team.ru to find out "
            "why VLAN is not available on the switch "
            "(vlan is not present in net-layout.xml).".format(2, "switch-mock")
        )

        mock_retry_current_stage(
            host,
            Stages.SWITCH_VLANS,
            vlan_switching._STATUS_PREPARING,
            error=expected_error_message,
            check_after=DEFAULT_RETRY_TIMEOUT,
        )
        test.hosts.assert_equal()

    @patch("walle.clients.racktables.is_vlan_available", return_value=True)
    def test_vlan_available(self, is_vlan_available, test):
        host = self._create_host_and_stage(test)
        handle_host(host)
        assert is_vlan_available.mock_calls == [call("switch-mock", 1), call("switch-mock", 2)]

        mock_commit_stage_changes(host, status=vlan_switching._STATUS_SWITCHING_VLANS, check_now=True)
        test.hosts.assert_equal()

    @patch("walle.clients.racktables.is_vlan_available", return_value=True)
    @pytest.mark.parametrize("target_network", NetworkTarget.AVAILABLE)
    def test_skip(self, is_vlan_available, test, target_network):
        host = self._create_host_and_stage(test, network=target_network)

        handle_host(host)
        assert is_vlan_available.mock_calls == []

        mock_commit_stage_changes(host, status=vlan_switching._STATUS_SWITCHING_VLANS, check_now=True)
        test.hosts.assert_equal()


class TestVlanSwitching:
    @staticmethod
    def _create_host_and_stage(test, skip_on_switch_missing=False):
        stage_terminators = {StageTerminals.SWITCH_MISSING: StageTerminals.SKIP} if skip_on_switch_missing else None

        return test.mock_host(
            {
                "task": mock_task(
                    stage=Stages.SWITCH_VLANS,
                    stage_status=vlan_switching._STATUS_SWITCHING_VLANS,
                    stage_params={"network": NetworkTarget.PROJECT},
                    stage_terminators=stage_terminators,
                    stage_temp_data={
                        "switched_port_info": {
                            "switch": "switch-mock",
                            "port": "port-mock",
                            "vlans": [1, 2],
                            "native_vlan": 1,
                        }
                    },
                )
            }
        )

    @patch("walle.clients.racktables.switch_vlans", side_effect=racktables.PersistentRacktablesError("Mocked error"))
    def test_switching_with_racktables_error_fail(self, switch_vlans, test):
        host = self._create_host_and_stage(test)
        handle_host(host)
        switch_vlans.assert_called_once_with("switch-mock", "port-mock", [1, 2], 1)

        error = (
            "Can not switch 'port-mock' port of 'switch-mock' switch "
            "to 1,2 VLANs with native VLAN set to 1: "
            "RackTables returned an error: Mocked error."
        )
        mock_retry_current_stage(
            host, Stages.SWITCH_VLANS, vlan_switching._STATUS_PREPARING, error=error, check_after=DEFAULT_RETRY_TIMEOUT
        )

        test.hosts.assert_equal()

    @patch("walle.clients.racktables.switch_vlans", side_effect=racktables.PersistentRacktablesError("Mocked error"))
    def test_switching_with_racktables_error_skip(self, switch_vlans, test):
        host = self._create_host_and_stage(test, skip_on_switch_missing=True)
        handle_host(host)
        switch_vlans.assert_called_once_with("switch-mock", "port-mock", [1, 2], 1)

        mock_skip_current_stage(host)
        test.hosts.assert_equal()

    @patch("walle.clients.racktables.switch_vlans")
    def test_switching(self, switch_vlans, test):
        host = self._create_host_and_stage(test)
        handle_host(host)
        switch_vlans.assert_called_once_with("switch-mock", "port-mock", [1, 2], 1)

        mock_commit_stage_changes(
            host, status=vlan_switching._STATUS_WAITING_VLANS_SYNC, check_after=vlan_switching._MINIMUM_SYNC_DELAY
        )
        test.hosts.assert_equal()


class TestWaitingSyncVlans:
    @staticmethod
    def _create_host_and_stage(test, stage_status_time=None, retry_count=None):
        stage_data = None
        if retry_count is not None:
            stage_data = {"retries": retry_count}

        return test.mock_host(
            {
                "task": mock_task(
                    stage=Stages.SWITCH_VLANS,
                    stage_status=vlan_switching._STATUS_WAITING_VLANS_SYNC,
                    stage_params={"network": NetworkTarget.PROJECT},
                    stage_status_time=stage_status_time,
                    stage_data=stage_data,
                    stage_temp_data={
                        "switched_port_info": {
                            "switch": "switch-mock",
                            "port": "port-mock",
                            "vlans": [1, 2],
                            "native_vlan": 1,
                        }
                    },
                )
            }
        )

    @patch(
        "walle.clients.racktables.get_port_vlan_status",
        side_effect=racktables.PersistentRacktablesError("Mocked error"),
    )
    def test_racktables_error(self, get_port_vlan_status, test):
        host = self._create_host_and_stage(test)

        handle_host(host)

        error = (
            "Failed to switch 'port-mock' port of 'switch-mock' switch "
            "to 1,2 VLANs with native VLAN set to 1: "
            "RackTables returned an error: Mocked error."
        )
        mock_retry_current_stage(
            host, Stages.SWITCH_VLANS, vlan_switching._STATUS_PREPARING, error=error, check_after=DEFAULT_RETRY_TIMEOUT
        )
        get_port_vlan_status.assert_called_once_with("switch-mock", "port-mock")
        test.hosts.assert_equal()

    @patch("walle.clients.racktables.get_port_vlan_status", return_value=([1, 2], 3, True))
    def test_unexpected_vlan_switch_retry(self, get_port_vlan_status, test):
        host = self._create_host_and_stage(test)

        handle_host(host)

        get_port_vlan_status.assert_called_once_with("switch-mock", "port-mock")

        get_current_stage(host).set_data("retries", 1)
        error_msg = (
            "Failed to switch 'port-mock' port of 'switch-mock' switch to 1,2 VLANs with native VLAN set to 1:"
            " the port has suddenly changed it's assigned VLANs to 1,2 VLANs with native VLAN set to 3. "
            "Retrying."
        )
        mock_retry_current_stage(
            host,
            Stages.SWITCH_VLANS,
            check_after=vlan_switching.DEFAULT_RETRY_TIMEOUT,
            expected_status=vlan_switching._STATUS_PREPARING,
            error=error_msg,
        )
        test.hosts.assert_equal()

    @patch("walle.clients.racktables.get_port_vlan_status", return_value=([1, 2], 3, True))
    def test_unexpected_vlan_switch_fail(self, get_port_vlan_status, test, mp):
        monkeypatch_config(mp, "vlan_switching.max_errors", 2)
        host = self._create_host_and_stage(test, retry_count=2)

        handle_host(host)

        get_port_vlan_status.assert_called_once_with("switch-mock", "port-mock")
        error = (
            "Too many errors occurred during processing 'switch-vlans' stage of 'ready' task."
            " Last error: Failed to switch 'port-mock' port of 'switch-mock' switch"
            " to 1,2 VLANs with native VLAN set to 1:"
            " the port has suddenly changed it's assigned VLANs to 1,2 VLANs with native VLAN set to 3."
        )
        mock_fail_current_stage(host, reason=error)
        test.hosts.assert_equal()

    @patch("walle.clients.racktables.get_port_vlan_status", return_value=([1, 2], 1, False))
    def test_in_process(self, get_port_vlan_status, test):
        host = self._create_host_and_stage(test)

        handle_host(host)

        get_port_vlan_status.assert_called_once_with("switch-mock", "port-mock")
        mock_commit_stage_changes(host, check_after=vlan_switching._SYNC_POLLING_PERIOD)
        test.hosts.assert_equal()

    @patch("walle.clients.racktables.get_port_vlan_status", return_value=([1, 2], 1, False))
    def test_timed_out_report_error(self, get_port_vlan_status, test):
        host = self._create_host_and_stage(test, stage_status_time=timestamp() - vlan_switching._SWITCHING_TIMEOUT)

        handle_host(host)

        error = (
            "VLAN switching process is taking too long. "
            "It is probably a failure on the racktables's side. "
            "Please, contact with nocdev-bugs@yandex-team.ru. "
            "Operation is switching 'port-mock' port of 'switch-mock' switch to 1,2 "
            "VLANs with native VLAN set to 1"
        )
        mock_commit_stage_changes(host, error=error, check_after=vlan_switching._INCREASED_SYNC_POLLING_PERIOD)

        get_port_vlan_status.assert_called_once_with("switch-mock", "port-mock")
        test.hosts.assert_equal()

    @patch("walle.clients.racktables.get_port_vlan_status", return_value=([1, 2], 1, False))
    def test_timed_out_fail_task(self, get_port_vlan_status, test):
        host = self._create_host_and_stage(test, stage_status_time=timestamp() - vlan_switching._ULTIMATE_WAIT_TIMEOUT)

        handle_host(host)

        error = (
            "VLAN switching process is taking too long."
            " It is probably a failure on the racktables's side."
            " Please, contact with nocdev-bugs@yandex-team.ru."
            " Operation is switching 'port-mock' port of 'switch-mock' switch"
            " to 1,2 VLANs with native VLAN set to 1"
        )
        mock_fail_current_stage(host, reason=error)

        get_port_vlan_status.assert_called_once_with("switch-mock", "port-mock")
        test.hosts.assert_equal()

    @patch("walle.clients.racktables.get_port_vlan_status", return_value=([1, 2], 1, True))
    def test_success(self, get_port_vlan_status, test):
        host = self._create_host_and_stage(test)

        handle_host(host)

        get_port_vlan_status.assert_called_once_with("switch-mock", "port-mock")
        mock_commit_stage_changes(host, status=vlan_switching._STATUS_WAITING_NETWORKS, check_now=True)
        test.hosts.assert_equal()


class TestWaitingNetworks:
    @staticmethod
    def _create_host_and_stage(test, stage_status_time=None, network=NetworkTarget.PROJECT, temp_data=None):
        temp_data = drop_none(temp_data or {})
        temp_data["switched_port_info"] = {
            "switch": "switch-mock",
            "port": "port-mock",
            "vlans": [1, 2, 3],
            "native_vlan": 1,
        }

        return test.mock_host(
            {
                "task": mock_task(
                    stage=Stages.SWITCH_VLANS,
                    stage_status=vlan_switching._STATUS_WAITING_NETWORKS,
                    stage_status_time=stage_status_time,
                    stage_params={"network": network},
                    stage_temp_data=temp_data,
                )
            }
        )

    @staticmethod
    def network_for_vlan(assigned_vlan):
        def _side_effect(switch, vlan):
            if vlan == assigned_vlan:
                return ["network-mock"]

        return _side_effect

    @staticmethod
    def polling_period(is_l3_switch):
        if is_l3_switch:
            return vlan_switching._L3_NETWORKS_POLLING_PERIOD
        else:
            return vlan_switching._L2_NETWORKS_POLLING_PERIOD

    @pytest.mark.parametrize("is_l3_switch", [True, False])
    @patch("walle.clients.racktables.is_l3_switch")
    def test_in_process(self, is_l3_switch_mock, test, is_l3_switch):
        host = self._create_host_and_stage(test)

        is_l3_switch_mock.return_value = is_l3_switch
        polling_period = self.polling_period(is_l3_switch)

        with patch(
            "walle.clients.racktables.get_vlan_networks", side_effect=self.network_for_vlan(2)
        ) as get_vlan_network:
            handle_host(host)
        assert get_vlan_network.mock_calls == [call("switch-mock", 1), call("switch-mock", 2), call("switch-mock", 3)]
        get_current_stage(host).set_temp_data("assigned_vlans", [2])
        mock_commit_stage_changes(host, check_after=polling_period)
        test.hosts.assert_equal()

        with patch(
            "walle.clients.racktables.get_vlan_networks", side_effect=self.network_for_vlan(1)
        ) as get_vlan_network:
            handle_host(host)
        assert get_vlan_network.mock_calls == [call("switch-mock", 1), call("switch-mock", 3)]
        get_current_stage(host).get_temp_data("assigned_vlans").append(1)
        mock_commit_stage_changes(host, check_after=polling_period)
        test.hosts.assert_equal()

    @pytest.mark.parametrize("is_l3_switch", [True, False])
    @patch("walle.clients.racktables.is_l3_switch")
    @patch("walle.clients.racktables.get_vlan_networks", return_value=None)
    def test_timeout_report_error(self, get_vlan_network, is_l3_switch_mock, test, is_l3_switch):
        is_l3_switch_mock.return_value = is_l3_switch
        polling_period = self.polling_period(is_l3_switch)

        stage_status_time = timestamp() - (vlan_switching._NETWORK_ASSIGNING_TIMEOUT + polling_period)
        host = self._create_host_and_stage(test, stage_status_time=stage_status_time)

        handle_host(host)

        error = (
            "Network assigning for 1, 2, 3 VLANs is taking too long. "
            "It is probably a failure on the racktables's side. "
            "Please, contact with nocdev-bugs@yandex-team.ru. "
            "Operation is switching 'port-mock' port of 'switch-mock' switch to 1,2,3 "
            "VLANs with native VLAN set to 1"
        )
        get_current_stage(host).set_temp_data("assigned_vlans", [])
        mock_commit_stage_changes(host, error=error, check_after=polling_period)

        assert get_vlan_network.mock_calls == [call("switch-mock", 1), call("switch-mock", 2), call("switch-mock", 3)]
        test.hosts.assert_equal()

    @pytest.mark.parametrize("is_l3_switch", [True, False])
    @patch("walle.clients.racktables.is_l3_switch")
    @patch("walle.clients.racktables.get_vlan_networks", return_value=None)
    def test_timeout_fail_task(self, get_vlan_network, is_l3_switch_mock, test, is_l3_switch):
        is_l3_switch_mock.return_value = is_l3_switch
        polling_period = self.polling_period(is_l3_switch)

        stage_status_time = timestamp() - (vlan_switching._ULTIMATE_WAIT_TIMEOUT + polling_period)
        host = self._create_host_and_stage(test, stage_status_time=stage_status_time)

        handle_host(host)

        error = (
            "Network assigning for 1, 2, 3 VLANs is taking too long."
            " It is probably a failure on the racktables's side."
            " Please, contact with nocdev-bugs@yandex-team.ru."
            " Operation is switching 'port-mock' port of 'switch-mock' switch"
            " to 1,2,3 VLANs with native VLAN set to 1"
        )
        get_current_stage(host).set_temp_data("assigned_vlans", [])
        mock_fail_current_stage(host, reason=error)

        assert get_vlan_network.mock_calls == [call("switch-mock", 1), call("switch-mock", 2), call("switch-mock", 3)]
        test.hosts.assert_equal()

    @patch("walle.clients.racktables.is_l3_switch")
    @patch("walle.clients.racktables.get_vlan_networks", return_value=["network-mock"])
    @pytest.mark.parametrize("project_id_synced", [True, False, None])
    def test_success(self, get_vlan_network, is_l3_switch_mock, test, project_id_synced):
        stage_status_time = timestamp() - vlan_switching._NETWORK_ASSIGNING_TIMEOUT
        host = self._create_host_and_stage(
            test, stage_status_time=stage_status_time, temp_data={"project_id_synced": project_id_synced}
        )

        handle_host(host)

        assert get_vlan_network.mock_calls == [call("switch-mock", 1), call("switch-mock", 2), call("switch-mock", 3)]
        assert not is_l3_switch_mock.called

        stage = get_current_stage(host)

        if project_id_synced:
            mock_complete_current_stage(host, expected_data={"vlan_success": True})
        else:
            stage.set_temp_data("vlans_synced", True)
            stage.set_temp_data("assigned_vlans", [1, 2, 3])
            mock_commit_stage_changes(host, status=vlan_switching._STATUS_CHECKING_PROJECT)

        test.hosts.assert_equal()

    @patch("walle.clients.racktables.is_l3_switch")
    @patch("walle.clients.racktables.get_vlan_networks", return_value=["network-mock"])
    @pytest.mark.parametrize("target_network", NetworkTarget.AVAILABLE)
    @pytest.mark.parametrize("project_id_synced", [True, False, None])
    def test_skip(self, get_vlan_network, is_l3_switch_mock, test, target_network, project_id_synced):
        stage_status_time = timestamp() - vlan_switching._NETWORK_ASSIGNING_TIMEOUT
        host = self._create_host_and_stage(
            test,
            stage_status_time=stage_status_time,
            network=target_network,
            temp_data={"project_id_synced": project_id_synced},
        )

        handle_host(host)

        if project_id_synced:
            mock_complete_current_stage(host, expected_data={"vlan_success": True})
        else:
            get_current_stage(host).set_temp_data("vlans_synced", True)
            mock_commit_stage_changes(host, status=vlan_switching._STATUS_CHECKING_PROJECT)

        assert get_vlan_network.mock_calls == []
        assert not is_l3_switch_mock.called

        test.hosts.assert_equal()


@patch("walle.clients.vlan_toggler.switch_port_state")
class TestCheckingYcState:
    @staticmethod
    def _create_host_and_stage(test):
        project = test.mock_project({"id": "some-id", "vlan_scheme": VLAN_SCHEME_CLOUD})

        temp_data = {
            "switched_port_info": {"switch": "switch-mock", "port": "port-mock", "target_yc_state": YCNetworkState.PROD}
        }

        return test.mock_host(
            {
                "project": project.id,
                "task": mock_task(
                    stage=Stages.SWITCH_VLANS,
                    stage_params={"network": NetworkTarget.PROJECT},
                    stage_status=vlan_switching._STATUS_CHECKING_YC_STATE,
                    stage_temp_data=temp_data,
                ),
            }
        )

    @patch(
        "walle.clients.vlan_toggler.get_port_state",
        return_value=vlan_toggler.Response(tor="", interface="", state=YCNetworkState.SETUP),
    )
    def test_checking_not_applied(self, get_port_state, switch_port_state, test):
        host = self._create_host_and_stage(test)
        handle_host(host)

        get_port_state.assert_called_once_with("switch-mock", "port-mock")
        assert not switch_port_state.called

        mock_commit_stage_changes(host, status=vlan_switching._STATUS_SWITCHING_YC_STATE, check_now=True)
        test.hosts.assert_equal()

    @patch(
        "walle.clients.vlan_toggler.get_port_state",
        return_value=vlan_toggler.Response(tor="", interface="", state=YCNetworkState.PROD),
    )
    def test_checking_already_applied(self, get_port_state, switch_port_state, test):
        host = self._create_host_and_stage(test)
        handle_host(host)

        get_port_state.assert_called_once_with("switch-mock", "port-mock")
        assert not switch_port_state.called

        mock_complete_current_stage(host, expected_data={"vlan_success": True})
        test.hosts.assert_equal()

    @pytest.mark.parametrize("return_status_code", vlan_toggler.VLAN_TOGGLER_HTTP_NON_RETRY_STATUSES)
    def test_checking_with_vlan_toggler_non_retriable_error(self, switch_port_state, mp, test, return_status_code):
        mock_vlan_toggler_error_request(mp, return_status_code)
        origin_get_port_state = vlan_toggler.get_port_state

        def mocked_get_port_state(*args, **kwargs):
            return origin_get_port_state(*args, **kwargs)

        with patch("walle.clients.vlan_toggler.get_port_state", side_effect=mocked_get_port_state) as get_port_state:
            host = self._create_host_and_stage(test)
            handle_host(host)

            get_port_state.assert_called_once_with("switch-mock", "port-mock")

        assert not switch_port_state.called

        error = (
            "Can not switch 'port-mock' port of 'switch-mock' switch "
            "to 'prod' state: VLAN Toggler HTTP code {}.".format(return_status_code)
        )
        mock_fail_current_stage(host, reason=error)
        test.hosts.assert_equal()

    @pytest.mark.parametrize("return_status_code", vlan_toggler.VLAN_TOGGLER_HTTP_RETRY_STATUSES)
    def test_checking_with_vlan_toggler_retriable_error(self, switch_port_state, mp, test, return_status_code):
        mock_vlan_toggler_error_request(mp, return_status_code)

        host = self._create_host_and_stage(test)
        handle_host(host)

        assert not switch_port_state.called

        error = (
            "Can not switch 'port-mock' port of 'switch-mock' switch "
            "to 'prod' state: VLAN Toggler HTTP code {}. Retrying.".format(return_status_code)
        )
        mock_retry_current_stage(
            host,
            Stages.SWITCH_VLANS,
            vlan_switching._STATUS_PREPARING,
            error=error,
            check_after=vlan_switching._VLAN_TOGGLER_RETRY_DELAY,
        )
        test.hosts.assert_equal()


class TestYcStateSwitching:
    @staticmethod
    def _create_host_and_stage(test, skip_on_switch_missing=False):
        stage_terminators = {StageTerminals.SWITCH_MISSING: StageTerminals.SKIP} if skip_on_switch_missing else None

        return test.mock_host(
            {
                "task": mock_task(
                    stage=Stages.SWITCH_VLANS,
                    stage_status=vlan_switching._STATUS_SWITCHING_YC_STATE,
                    stage_params={"network": NetworkTarget.PROJECT},
                    stage_terminators=stage_terminators,
                    stage_temp_data={
                        "switched_port_info": {
                            "switch": "switch-mock",
                            "port": "port-mock",
                            "target_yc_state": YCNetworkState.PROD,
                        }
                    },
                )
            }
        )

    @patch(
        "walle.clients.vlan_toggler.switch_port_state",
        side_effect=vlan_toggler.VlanTogglerPersistentError("Mocked error"),
    )
    @pytest.mark.parametrize("skip_on_switch_missing", (True, False))
    def test_switching_with_vlan_toggler_error_fail(self, switch_port_state, test, skip_on_switch_missing):
        host = self._create_host_and_stage(test, skip_on_switch_missing)
        handle_host(host)
        switch_port_state.assert_called_once_with("switch-mock", "port-mock", YCNetworkState.PROD)

        error = "Can not switch 'port-mock' port of 'switch-mock' switch to 'prod' state: Mocked error"
        mock_fail_current_stage(host, reason=error)

        test.hosts.assert_equal()

    @patch(
        "walle.clients.vlan_toggler.switch_port_state",
        return_value=vlan_toggler.Response(tor="", interface="", state=YCNetworkState.PROD),
    )
    def test_switching(self, switch_port_state, test):
        host = self._create_host_and_stage(test)
        handle_host(host)
        switch_port_state.assert_called_once_with("switch-mock", "port-mock", YCNetworkState.PROD)

        mock_commit_stage_changes(
            host, status=vlan_switching._STATUS_WAITING_YC_STATE, check_after=vlan_switching._MINIMUM_SYNC_DELAY
        )
        test.hosts.assert_equal()

    @patch(
        "walle.clients.vlan_toggler.switch_port_state",
        return_value=vlan_toggler.Response(tor="", interface="", state="unknown"),
    )
    def test_switching_to_unknown_state(self, switch_port_state, test):
        host = self._create_host_and_stage(test)
        handle_host(host)
        switch_port_state.assert_called_once_with("switch-mock", "port-mock", YCNetworkState.PROD)

        error = "Can not switch 'port-mock' port of 'switch-mock' switch to 'prod' state: Returned state 'unknown'"
        mock_fail_current_stage(host, reason=error)

        test.hosts.assert_equal()


class TestWaitingYcState:
    @staticmethod
    def _create_host_and_stage(test, stage_status_time=None):
        return test.mock_host(
            {
                "task": mock_task(
                    stage=Stages.SWITCH_VLANS,
                    stage_status=vlan_switching._STATUS_WAITING_YC_STATE,
                    stage_params={"network": NetworkTarget.PROJECT},
                    stage_status_time=stage_status_time,
                    stage_temp_data={
                        "switched_port_info": {
                            "switch": "switch-mock",
                            "port": "port-mock",
                            "target_yc_state": YCNetworkState.PROD,
                        }
                    },
                )
            }
        )

    @patch(
        "walle.clients.vlan_toggler.get_port_state", side_effect=vlan_toggler.VlanTogglerPersistentError("Mocked error")
    )
    def test_vlan_toggler_error(self, get_port_state, test):
        host = self._create_host_and_stage(test)

        handle_host(host)

        error = "Can not switch 'port-mock' port of 'switch-mock' switch to 'prod' state: Mocked error"
        mock_retry_current_stage(
            host, Stages.SWITCH_VLANS, vlan_switching._STATUS_PREPARING, error=error, check_after=DEFAULT_RETRY_TIMEOUT
        )
        get_port_state.assert_called_once_with("switch-mock", "port-mock")
        host.reload()
        assert host.status == HostStatus.DEAD

    @patch(
        "walle.clients.vlan_toggler.get_port_state",
        return_value=vlan_toggler.Response(tor="", interface="", state=YCNetworkState.SETUP),
    )
    def test_in_process(self, get_port_state, test):
        host = self._create_host_and_stage(test)

        handle_host(host)

        get_port_state.assert_called_once_with("switch-mock", "port-mock")
        mock_commit_stage_changes(host, check_after=vlan_switching._SYNC_POLLING_PERIOD)
        test.hosts.assert_equal()

    @patch(
        "walle.clients.vlan_toggler.get_port_state",
        return_value=vlan_toggler.Response(tor="", interface="", state=YCNetworkState.SETUP),
    )
    def test_timed_out_report_error(self, get_port_state, test):
        host = self._create_host_and_stage(test, stage_status_time=timestamp() - vlan_switching._SWITCHING_TIMEOUT)

        handle_host(host)

        error = (
            "YC state switching process is taking too long. "
            "It is probably a failure on the vlan toggler's side. "
            "Please, contact with cloud-netinfra@yandex-team.ru. "
            "Operation is switching 'port-mock' port of 'switch-mock' switch to 'prod' state"
        )
        mock_commit_stage_changes(host, error=error, check_after=vlan_switching._INCREASED_SYNC_POLLING_PERIOD)

        get_port_state.assert_called_once_with("switch-mock", "port-mock")
        test.hosts.assert_equal()

    @patch(
        "walle.clients.vlan_toggler.get_port_state",
        return_value=vlan_toggler.Response(tor="", interface="", state=YCNetworkState.SETUP),
    )
    def test_timed_out_fail_task(self, get_port_state, test):
        host = self._create_host_and_stage(test, stage_status_time=timestamp() - vlan_switching._ULTIMATE_WAIT_TIMEOUT)

        handle_host(host)

        error = (
            "YC state switching process is taking too long. "
            "It is probably a failure on the vlan toggler's side. "
            "Please, contact with cloud-netinfra@yandex-team.ru. "
            "Operation is switching 'port-mock' port of 'switch-mock' switch to 'prod' state"
        )
        mock_fail_current_stage(host, reason=error)

        get_port_state.assert_called_once_with("switch-mock", "port-mock")
        test.hosts.assert_equal()

    @patch(
        "walle.clients.vlan_toggler.get_port_state",
        return_value=vlan_toggler.Response(tor="", interface="", state=YCNetworkState.PROD),
    )
    def test_success(self, get_port_state, test):
        host = self._create_host_and_stage(test)

        handle_host(host)

        get_port_state.assert_called_once_with("switch-mock", "port-mock")
        mock_complete_current_stage(host, expected_data={"vlan_success": True})
        test.hosts.assert_equal()


class TestErrorHandler:
    _POSSIBLE_STAGE_STATUSES = [
        # not checking "prepare" status here, it have custom procedure
        vlan_switching._STATUS_CHECKING_VLANS,
        vlan_switching._STATUS_CHECKING_PROJECT,
        vlan_switching._STATUS_VLAN_AVAILABILITY,
        vlan_switching._STATUS_SWITCHING_VLANS,
        vlan_switching._STATUS_SWITCHING_PROJECT,
        vlan_switching._STATUS_WAITING_VLANS_SYNC,
        vlan_switching._STATUS_WAITING_PROJECT_SYNC,
        vlan_switching._STATUS_WAITING_NETWORKS,
    ]

    @staticmethod
    def _create_host_with_error(test, stage_status, info=True):
        project = test.mock_project({"id": "some-id", "owned_vlans": [1, 2], "hbf_project_id": mtn_prj(MTN_PRJ)})
        if info:
            switched_port_info = {
                "switch": "switch-mock",
                "port": "port-mock",
                "vlans": [1, 2],
                "native_vlan": 1,
                "mtn_project_id": None,
            }
        else:
            switched_port_info = None

        stage_temp_data = drop_none(
            {
                "switched_port_info": switched_port_info,
            }
        )

        return test.mock_host(
            {
                "project": project.id,
                "task": mock_task(
                    error="mock-task-error",
                    stage=Stages.SWITCH_VLANS,
                    stage_params={"network": NetworkTarget.PROJECT},
                    stage_status=stage_status,
                    stage_temp_data=stage_temp_data,
                ),
            }
        )

    @patch("walle.clients.racktables.get_port_project_status", return_value=(None, True))
    @patch(
        "walle.network.get_current_host_switch_port",
        return_value=HostNetworkLocationInfo(switch="switch-mock", port="port-mock", source="source-mock", timestamp=0),
    )
    def test_error_on_prepare(self, get_current_host_switch_port, get_port_status, mock_automated_vlan_config, test):
        # all possible cases are RT returned error. We should proceed with the default handler here.

        host = self._create_host_with_error(test, stage_status=vlan_switching._STATUS_PREPARING, info=False)
        handle_host(host)

        get_current_stage(host).set_temp_data(
            "switched_port_info",
            {
                "switch": "switch-mock",
                "port": "port-mock",
                "vlans": [1, 2],
                "native_vlan": 1,
                "mtn_project_id": None,
            },
        )
        mock_commit_stage_changes(host, status=vlan_switching._STATUS_CHECKING_VLANS, check_now=True)
        test.hosts.assert_equal()

    @patch(
        "walle.network.get_current_host_switch_port",
        return_value=HostNetworkLocationInfo(switch="switch-mock", port="port-mock", source="source-mock", timestamp=0),
    )
    @pytest.mark.parametrize("status", _POSSIBLE_STAGE_STATUSES)
    def test_error_on_other_statuses_proceed(
        self, get_current_host_switch_port, mock_automated_vlan_config, mp, status, test
    ):
        # example case: RT returned error, we should proceed with the default handler

        mock_handler = Mock()
        mp.function(common.get_stage_handler, return_value=mock_handler)

        host = self._create_host_with_error(test, stage_status=status)
        handle_host(host)

        mock_handler.assert_called_once_with(host)
        test.hosts.assert_equal()

    @patch(
        "walle.network.get_current_host_switch_port",
        return_value=HostNetworkLocationInfo(switch="switch-mock", port="port-mock", source="source-mock", timestamp=0),
    )
    @pytest.mark.parametrize("status", _POSSIBLE_STAGE_STATUSES)
    def test_error_on_other_statuses_retry(self, get_current_host_switch_port, mock_mtn_vlan_config, mp, status, test):
        # example case: user changed vlan scheme for the project

        mock_handler = Mock()
        mp.function(common.get_stage_handler, return_value=mock_handler)

        host = self._create_host_with_error(test, stage_status=status)
        handle_host(host)

        del host.task.error
        mock_retry_current_stage(host, Stages.SWITCH_VLANS, vlan_switching._STATUS_PREPARING)

        assert not mock_handler.called
        test.hosts.assert_equal()

    @patch(
        "walle.network.get_current_host_switch_port",
        return_value=HostNetworkLocationInfo(switch="switch-mock", port="port-mock", source="source-mock", timestamp=0),
    )
    @pytest.mark.parametrize("status", _POSSIBLE_STAGE_STATUSES)
    def test_error_on_other_statuses_abandon(self, get_current_host_switch_port, mock_no_vlan_config, mp, status, test):
        # example case: user disabled vlan config for the project.
        # Other cases are "switch unknown", invalid config, etc.

        mock_handler = Mock()
        mp.function(common.get_stage_handler, return_value=mock_handler)

        host = self._create_host_with_error(test, stage_status=status)
        handle_host(host)

        mock_complete_current_stage(host, expected_data={"vlan_success": True})

        assert not mock_handler.called
        test.hosts.assert_equal()
