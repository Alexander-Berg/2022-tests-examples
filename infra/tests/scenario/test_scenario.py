import pytest

from infra.walle.server.tests.lib.util import mock_uuid_for_inv
from walle.models import timestamp
from walle.scenario.constants import HostScenarioStatus, FixedMaintenanceApproversLogins
from walle.scenario.data_storage.types import HostGroupSource, MaintenanceApproversGroup
from walle.scenario.host_groups_builders.base import (
    HostGroup,
    BotProjectIdHostGroupSource,
    MaintenancePlotHostGroupSource,
    SpecificProjectTagHostGroupSource,
)
from walle.scenario.host_groups_builders.hosts_list_splitters import BaseHostsListSplitter
from walle.scenario.scenario import Scenario, ScenarioHostState


def test_next_id(walle_test):
    for i in range(10):
        assert i == Scenario.next_id()


def test_edit_ability_of_hosts_field(walle_test):
    test_hosts = {str(inv): ScenarioHostState(inv=inv, timestamp=0) for inv in range(3)}
    scenario = walle_test.mock_scenario(dict(hosts=list(range(3))), resolve_uuids=False)

    for host_info, test_val in zip(scenario.hosts, test_hosts):
        assert host_info == test_val

    scenario.modify(**scenario.set_host_status_kwargs(test_hosts["1"], HostScenarioStatus.PROCESSING))

    for host_info in scenario.hosts.values():
        if host_info.inv == 1:
            assert host_info.status == HostScenarioStatus.PROCESSING
        else:
            assert host_info.status == HostScenarioStatus.QUEUE


class EvenOddHostsListSplitter(BaseHostsListSplitter):
    @classmethod
    def split(cls, host_inv_list):
        odd_group = HostGroup(hosts_invs=[], group_source=MaintenancePlotHostGroupSource(maintenance_plot_id="odd"))
        even_group = HostGroup(hosts_invs=[], group_source=MaintenancePlotHostGroupSource(maintenance_plot_id="even"))
        for inv in host_inv_list:
            if inv % 2:
                odd_group.hosts_invs.append(inv)
            else:
                even_group.hosts_invs.append(inv)
        return [odd_group, even_group]


@pytest.mark.parametrize("use_uuids", [True, False])
def test_split_scenario_hosts_to_groups(walle_test, use_uuids):
    host_inv_list = [1, 2, 3]
    for inv in host_inv_list:
        walle_test.mock_host(dict(inv=inv, uuid=mock_uuid_for_inv(inv)))

    if use_uuids:
        expected_hosts_states = {
            mock_uuid_for_inv(1): ScenarioHostState(
                inv=1, timestamp=timestamp(), status=HostScenarioStatus.QUEUE, group=0
            ),
            mock_uuid_for_inv(2): ScenarioHostState(
                inv=2, timestamp=timestamp(), status=HostScenarioStatus.QUEUE, group=1
            ),
            mock_uuid_for_inv(3): ScenarioHostState(
                inv=3, timestamp=timestamp(), status=HostScenarioStatus.QUEUE, group=0
            ),
        }
    else:
        expected_hosts_states = {
            "1": ScenarioHostState(inv=1, timestamp=timestamp(), status=HostScenarioStatus.QUEUE, group=0),
            "2": ScenarioHostState(inv=2, timestamp=timestamp(), status=HostScenarioStatus.QUEUE, group=1),
            "3": ScenarioHostState(inv=3, timestamp=timestamp(), status=HostScenarioStatus.QUEUE, group=0),
        }

    expected_host_groups_sources = [
        HostGroupSource(0, MaintenancePlotHostGroupSource(maintenance_plot_id="odd")),
        HostGroupSource(1, MaintenancePlotHostGroupSource(maintenance_plot_id="even")),
    ]

    actual_hosts_states, actual_host_groups_sources = Scenario.split_scenario_hosts_to_groups(
        host_inv_list=host_inv_list, uses_uuids=use_uuids, hosts_list_splitter=EvenOddHostsListSplitter()
    )

    for actual_hosts_state_inv, actual_hosts_state in actual_hosts_states.items():
        expected_hosts_state = expected_hosts_states[actual_hosts_state_inv]
        assert expected_hosts_state.inv == actual_hosts_state.inv
        assert expected_hosts_state.group == actual_hosts_state.group

    assert expected_host_groups_sources == actual_host_groups_sources


def test_get_maintenance_approvers_groups_from_host_groups_sources():
    host_groups_sources = [
        HostGroupSource(0, MaintenancePlotHostGroupSource(maintenance_plot_id="foo")),
        HostGroupSource(1, BotProjectIdHostGroupSource(bot_project_id=42, abc_service_slug="abc-42")),
        HostGroupSource(2, SpecificProjectTagHostGroupSource(specific_project_tag="yt")),
    ]
    expected_result = [
        MaintenanceApproversGroup(
            0, "maintenance-plot:foo", FixedMaintenanceApproversLogins.DEFAULT_MAINTENANCE_APPROVERS_LOGINS
        ),
        MaintenanceApproversGroup(
            1, "bot-project-id:42/abc-42", FixedMaintenanceApproversLogins.DEFAULT_MAINTENANCE_APPROVERS_LOGINS
        ),
        MaintenanceApproversGroup(
            2, "specific-project-tag:yt", FixedMaintenanceApproversLogins.DEFAULT_MAINTENANCE_APPROVERS_LOGINS
        ),
    ]
    assert expected_result == Scenario.get_maintenance_approvers_groups_from_host_groups_sources(host_groups_sources)
