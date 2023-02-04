import pytest

from walle.clients import abc
from walle.hosts import HostLocation
from walle.models import timestamp
from walle.scenario.common import get_inv_to_tags_map
from walle.scenario.constants import FixedMaintenanceApproversLogins
from walle.scenario.data_storage.data_storage import DefaultDataStorage
from walle.scenario.data_storage.types import MaintenanceApproversGroup
from walle.scenario.scenario import ScenarioHostState
from walle.scenario.scheduler import (
    DatacenterScheduler,
    AllPreviouslyAcquiredHostsScheduler,
    MaintenanceApproversScheduler,
)


@pytest.mark.parametrize(
    ["datacenters", "initial_group_size", "result"],
    [
        [("IVA", "MYT", "SAS", "VLADIMIR"), 1, 4],
        [("IVA",), 1, 1],
        [tuple(), 0, 0],
        [("IVA", "IVA", "SAS", "SAS"), 1, 2],
    ],
)
def test_database_scheduler(walle_test, datacenters, initial_group_size, result):
    hosts_info = {}
    for idx, datacenter in enumerate(datacenters):
        country, city, queue, rack = "new-country", "new-city", "new-queue", "new-rack"
        walle_test.mock_host(
            dict(
                inv=idx,
                name=str(idx),
                location=HostLocation(
                    country=country,
                    city=city,
                    datacenter=datacenter,
                    queue=queue,
                    rack=rack,
                    physical_timestamp=timestamp(),
                ),
            )
        )
        hosts_info[str(idx)] = ScenarioHostState(inv=idx, group=0)

    assert len({item.group for item in hosts_info.values()}) == initial_group_size

    scheduled_hosts = DatacenterScheduler.schedule(hosts_info)

    assert len({item.group for item in scheduled_hosts.values()}) == result


def test_previously_scheduled_scheduler(walle_test):
    for i in range(5):
        walle_test.mock_host({"inv": i})
    hosts_info = {}
    for i in range(3):
        hosts_info[str(i)] = ScenarioHostState(inv=i, group=0, is_acquired=True)
    for i in range(3, 5):
        hosts_info[str(i)] = ScenarioHostState(inv=i, group=0)

    scheduled_hosts = AllPreviouslyAcquiredHostsScheduler.schedule(hosts_info)
    assert len([host for host in scheduled_hosts.values() if host.group >= 0]) == 3
    assert len([host for host in scheduled_hosts.values() if host.group < 0]) == 2


def test_maintenance_approvers_scheduler(mp, monkeypatch, walle_test):
    mp.function(abc.get_service_on_duty_logins, return_value=["yp-approver-login-1"])
    monkeypatch.setattr(FixedMaintenanceApproversLogins, "YABS_MAINTENANCE_APPROVERS_LOGINS", ["yabs-approver-login-1"])
    monkeypatch.setattr(
        FixedMaintenanceApproversLogins,
        "YT_MAINTENANCE_APPROVERS_LOGINS",
        ["yt-approver-login-1", "yt-approver-login-2"],
    )
    monkeypatch.setattr(
        FixedMaintenanceApproversLogins, "DEFAULT_MAINTENANCE_APPROVERS_LOGINS", ["default-approver-login-1"]
    )

    mock_project_invs_map = {
        "yt-project-1": {"tags": ["yt", "some-yt-tag"], "invs": [1]},
        "yt-project-2": {"tags": ["yt", "another-yt-tag"], "invs": [2, 3]},
        "yp-project": {"tags": ["yp", "some-yp-tag"], "invs": [4]},
        "yabs-project": {"tags": ["yabs", "yabs-foo", "yabs-bar"], "invs": [5, 6]},
        "other-project-1": {"tags": ["other-foo"], "invs": [7, 8, 9]},
        "other-project-2": {"tags": [], "invs": [10, 11, 12]},
        "yt-over-yp-project": {"tags": ["yp", "yt", "some-other-tags"], "invs": [13]},
        "yt-masters-project": {"tags": ["yp", "yt", "some-other-tag", "rtc.reboot_segment-yt_masters"], "invs": [14]},
    }
    number_of_hosts_in_mock_project_invs_map = 14

    for project, project_values in mock_project_invs_map.items():
        walle_test.mock_project({"id": project, "tags": project_values["tags"]})
        for inv in project_values["invs"]:
            walle_test.mock_host({"inv": inv, "project": project})

    data_storage_dict = {}
    data_storage = DefaultDataStorage(data_storage_dict)

    hosts_info = {}
    for idx in range(number_of_hosts_in_mock_project_invs_map):
        hosts_info[str(idx)] = ScenarioHostState(inv=idx + 1)

    scheduler = MaintenanceApproversScheduler(hosts_info, data_storage)
    hosts_info = scheduler.schedule()

    maintenance_approvers_groups = data_storage.read()

    expected_group_name_invs_map = {
        "yt": [1, 2, 3, 13],
        "yp": [4],
        "yabs": [5, 6],
        "other": [7, 8, 9, 10, 11, 12],
        "rtc.reboot_segment-yt_masters": [14],
    }

    for group in maintenance_approvers_groups:
        hosts_invs_of_given_group = [host.inv for host in hosts_info.values() if host.group == group.group_id]
        assert hosts_invs_of_given_group == expected_group_name_invs_map[group.name]

    def get_host_approvers(_inv):
        for host in hosts_info.values():
            if host.inv == _inv:
                host_group_id = host.group

                for _group in maintenance_approvers_groups:
                    if _group.group_id == host_group_id:
                        return _group.logins
        raise ValueError("Can not find host approvers for host '%s'" % _inv)

    assert get_host_approvers(1) == ["yt-approver-login-1", "yt-approver-login-2"]
    # WALLE-4021
    assert get_host_approvers(4) == ["default-approver-login-1"]  # ["yp-approver-login-1"]
    assert get_host_approvers(5) == ["yabs-approver-login-1"]
    assert get_host_approvers(7) == ["default-approver-login-1"]
    assert get_host_approvers(13) == ["yt-approver-login-1", "yt-approver-login-2"]
    assert get_host_approvers(14) == ["yt-approver-login-1", "yt-approver-login-2"]


@pytest.mark.parametrize(
    ["hosts_info", "mocked_hosts_data", "mocked_projects_data", "result"],
    [
        [
            {
                "0": ScenarioHostState(inv=1),
                "1": ScenarioHostState(inv=2),
                "2": ScenarioHostState(inv=3),
                "3": ScenarioHostState(inv=4),
                "4": ScenarioHostState(inv=5),
            },
            [
                {"inv": 1, "project": "project-foo"},
                {"inv": 2, "project": "project-foo"},
                {"inv": 3, "project": "project-bar"},
                {"inv": 4, "project": "project-baz"},
                {"inv": 5, "project": "project-baz"},
            ],
            [
                {"id": "project-foo", "tags": ["tag-foo-1", "tag-foo-2"]},
                {"id": "project-bar", "tags": ["tag-bar"]},
                {"id": "project-baz", "tags": ["tag-baz"]},
            ],
            # {inv: project tags list}
            {
                1: ["tag-foo-1", "tag-foo-2"],
                2: ["tag-foo-1", "tag-foo-2"],
                3: ["tag-bar"],
                4: ["tag-baz"],
                5: ["tag-baz"],
            },
        ]
    ],
)
def test_maintenance_approvers_scheduler_get_inv_tags_map(
    walle_test, hosts_info, mocked_hosts_data, mocked_projects_data, result
):
    for host_data in mocked_hosts_data:
        walle_test.mock_host(host_data)
    for project_data in mocked_projects_data:
        walle_test.mock_project(project_data)

    assert get_inv_to_tags_map([host.inv for host in hosts_info.values()]) == result


@pytest.mark.parametrize(
    ["hosts_info", "inv_tags_map", "assertions"],
    [
        [
            {
                "0": ScenarioHostState(inv=1),
                "1": ScenarioHostState(inv=2),
                "2": ScenarioHostState(inv=3),
                "3": ScenarioHostState(inv=4),
                "4": ScenarioHostState(inv=5),
                "5": ScenarioHostState(inv=6),
                "6": ScenarioHostState(inv=7),
                "7": ScenarioHostState(inv=8),
            },
            {
                1: ["yp", "tag-foo"],
                2: ["yp", "tag-foo-2"],
                3: ["yt", "tag-bar"],
                4: ["no-known-tags"],
                5: ["no-known-tags"],
                6: ["yabs"],
                7: ["yp", "yt"],
                8: ["rtc.reboot_segment-yt_masters"],
            },
            [
                # At the beginning.
                (
                    [],  # MaintenanceApproversScheduler._groups
                    0,  # MaintenanceApproversScheduler._next_group_id
                    {},  # MaintenanceApproversScheduler._groups_ids_created_from_known_project_tag
                    None,  # MaintenanceApproversScheduler._default_approvers_group_id
                ),
                # After processing hosts_info["0"].
                # Added "yp" approvers group with group_id 0.
                (
                    [
                        MaintenanceApproversGroup(0, "yp", ["yp", "approvers"]),
                    ],
                    1,
                    {"yp": 0},
                    None,
                ),
                # After processing hosts_info["1"].
                # No changes.
                (
                    [
                        MaintenanceApproversGroup(0, "yp", ["yp", "approvers"]),
                    ],
                    1,
                    {"yp": 0},
                    None,
                ),
                # After processing hosts_info["2"].
                # Added "yt" approvers group with group_id 1.
                (
                    [
                        MaintenanceApproversGroup(0, "yp", ["yp", "approvers"]),
                        MaintenanceApproversGroup(1, "yt", ["yt", "approvers"]),
                    ],
                    2,
                    {"yp": 0, "yt": 1},
                    None,
                ),
                # After processing hosts_info["3"].
                # Added "other" approvers group with group_id 2.
                (
                    [
                        MaintenanceApproversGroup(0, "yp", ["yp", "approvers"]),
                        MaintenanceApproversGroup(1, "yt", ["yt", "approvers"]),
                        MaintenanceApproversGroup(2, "other", ["default-approver-logins"]),
                    ],
                    3,
                    {"yp": 0, "yt": 1},
                    2,
                ),
                # After processing hosts_info["4"].
                # No changes.
                (
                    [
                        MaintenanceApproversGroup(0, "yp", ["yp", "approvers"]),
                        MaintenanceApproversGroup(1, "yt", ["yt", "approvers"]),
                        MaintenanceApproversGroup(2, "other", ["default-approver-logins"]),
                    ],
                    3,
                    {"yp": 0, "yt": 1},
                    2,
                ),
                # After processing hosts_info["5"].
                # Added "yabs" approvers group with group_id 3.
                (
                    [
                        MaintenanceApproversGroup(0, "yp", ["yp", "approvers"]),
                        MaintenanceApproversGroup(1, "yt", ["yt", "approvers"]),
                        MaintenanceApproversGroup(2, "other", ["default-approver-logins"]),
                        MaintenanceApproversGroup(3, "yabs", ["yabs", "approvers"]),
                    ],
                    4,
                    {
                        "yp": 0,
                        "yt": 1,
                        "yabs": 3,
                    },
                    2,
                ),
                # After processing hosts_info["6"].
                # No changes.
                (
                    [
                        MaintenanceApproversGroup(0, "yp", ["yp", "approvers"]),
                        MaintenanceApproversGroup(1, "yt", ["yt", "approvers"]),
                        MaintenanceApproversGroup(2, "other", ["default-approver-logins"]),
                        MaintenanceApproversGroup(3, "yabs", ["yabs", "approvers"]),
                    ],
                    4,
                    {
                        "yp": 0,
                        "yt": 1,
                        "yabs": 3,
                    },
                    2,
                ),
                # After processing hosts_info["7"].
                # Added "rtc.reboot_segment-yt_masters" approvers group with group_id 4.
                (
                    [
                        MaintenanceApproversGroup(0, "yp", ["yp", "approvers"]),
                        MaintenanceApproversGroup(1, "yt", ["yt", "approvers"]),
                        MaintenanceApproversGroup(2, "other", ["default-approver-logins"]),
                        MaintenanceApproversGroup(3, "yabs", ["yabs", "approvers"]),
                        MaintenanceApproversGroup(
                            4, "rtc.reboot_segment-yt_masters", ["rtc.reboot_segment-yt_masters", "approvers"]
                        ),
                    ],
                    5,
                    {
                        "yp": 0,
                        "yt": 1,
                        "yabs": 3,
                        "rtc.reboot_segment-yt_masters": 4,
                    },
                    2,
                ),
            ],
        ]
    ],
)
def test_maintenance_approvers_scheduler_get_host_group_id(
    mp, monkeypatch, walle_test, hosts_info, inv_tags_map, assertions
):
    def mock_get_approvers_logins_by_known_project_tag(self, tag):
        return [tag, "approvers"]

    monkeypatch.setattr(
        MaintenanceApproversScheduler,
        "_get_approvers_logins_by_known_project_tag",
        mock_get_approvers_logins_by_known_project_tag,
    )

    monkeypatch.setattr(
        FixedMaintenanceApproversLogins, "DEFAULT_MAINTENANCE_APPROVERS_LOGINS", ["default-approver-logins"]
    )

    data_storage_dict = {}
    data_storage = DefaultDataStorage(data_storage_dict)
    scheduler = MaintenanceApproversScheduler(hosts_info, data_storage)
    scheduler._inv_tags_map = inv_tags_map

    def check_assertions(step):
        _groups, _next_group_id, _groups_ids_created_from_known_project_tag, _default_approvers_group_id = assertions[
            step
        ]
        assert _groups == scheduler._groups
        assert _next_group_id == scheduler._next_group_id
        assert _groups_ids_created_from_known_project_tag == scheduler._groups_ids_created_from_known_project_tag
        if _default_approvers_group_id is None:
            assert scheduler._default_approvers_group_id is None
        else:
            assert _default_approvers_group_id == scheduler._default_approvers_group_id

    check_assertions(0)

    # Emulate MaintenanceApproversScheduler.schedule() method.
    # Check generated group ids for all ScenarioHostState objects in 'hosts_state' one by one.
    assert 0 == scheduler._get_host_group_id(hosts_info["0"].inv)
    check_assertions(1)

    assert 0 == scheduler._get_host_group_id(hosts_info["1"].inv)
    check_assertions(2)

    assert 1 == scheduler._get_host_group_id(hosts_info["2"].inv)
    check_assertions(3)

    assert 2 == scheduler._get_host_group_id(hosts_info["3"].inv)
    check_assertions(4)

    assert 2 == scheduler._get_host_group_id(hosts_info["4"].inv)
    check_assertions(5)

    assert 3 == scheduler._get_host_group_id(hosts_info["5"].inv)
    check_assertions(6)

    assert 1 == scheduler._get_host_group_id(hosts_info["6"].inv)
    check_assertions(7)

    assert 4 == scheduler._get_host_group_id(hosts_info["7"].inv)
    check_assertions(8)
