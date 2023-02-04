from walle.models import timestamp
from walle.scenario.data_storage.types import HostGroupSource, MaintenanceApproversGroup, ApprovementDecision
from walle.scenario.definitions.base import get_data_storage
from walle.scenario.host_groups_builders.base import (
    BotProjectIdHostGroupSource,
    MaintenancePlotHostGroupSource,
    SpecificProjectTagHostGroupSource,
)
from walle.scenario.host_groups_builders.constants import HostGroupsSourcesTypes
from walle.scenario.script import itdc_maintenance_script
from walle.scenario.script_args import ItdcMaintenanceParams


def test_read_write_maintenance_approvers_groups(walle_test):
    scenario = walle_test.mock_scenario(
        {
            "scenario_type": itdc_maintenance_script.name,
        }
    )

    data_storage = get_data_storage(scenario)
    assert data_storage.read() == []

    maintenance_approvers_groups_example = [
        group
        for group in [
            MaintenanceApproversGroup(group_id=0, name="foo", logins=["login1", "login2"]),
            MaintenanceApproversGroup(group_id=1, name="bar", logins=["login3"]),
        ]
    ]
    data_storage.write(maintenance_approvers_groups_example)

    assert scenario.data_storage["maintenance_approvers_groups"] == [
        {"group_id": 0, "name": "foo", "logins": ["login1", "login2"]},
        {"group_id": 1, "name": "bar", "logins": ["login3"]},
    ]
    assert data_storage.read() == maintenance_approvers_groups_example

    proto_message = data_storage.to_protobuf()
    all_logins = []
    for group in proto_message.itdc_maintenance_data_storage.maintenance_approvers_groups:
        for login in group.logins:
            all_logins.append(login)

    assert all_logins == ["login1", "login2", "login3"]


def test_read_write_host_groups_sources_map(walle_test):
    scenario = walle_test.mock_scenario(
        {
            "scenario_type": itdc_maintenance_script.name,
        }
    )

    data_storage = get_data_storage(scenario)
    assert data_storage.read_host_groups_sources() == []

    host_groups_sources_example = [
        HostGroupSource(
            0,
            BotProjectIdHostGroupSource(bot_project_id=42, abc_service_slug="42"),
            ApprovementDecision(skip_approvement=False, reason="reason for 0 group"),
        ),
        HostGroupSource(
            1,
            MaintenancePlotHostGroupSource(maintenance_plot_id="some-maintenance-plot-id"),
            ApprovementDecision(skip_approvement=True, reason="reason for 1 group"),
        ),
        HostGroupSource(
            2,
            SpecificProjectTagHostGroupSource("yt"),
            ApprovementDecision(skip_approvement=True, reason="reason for 2 group"),
        ),
    ]

    data_storage.write_host_groups_sources(host_groups_sources_example)

    host_groups_sources_expected_content = [
        {
            "group_id": 0,
            "source": {
                "bot_project_id": 42,
                "abc_service_slug": "42",
                "group_source_type": HostGroupsSourcesTypes.BOT_PROJECT_ID,
            },
            "approvement_decision": {"skip_approvement": False, "reason": "reason for 0 group"},
        },
        {
            "group_id": 1,
            "source": {
                "maintenance_plot_id": "some-maintenance-plot-id",
                "group_source_type": HostGroupsSourcesTypes.MAINTENANCE_PLOT,
            },
            "approvement_decision": {"skip_approvement": True, "reason": "reason for 1 group"},
        },
        {
            "group_id": 2,
            "source": {"specific_project_tag": "yt", "group_source_type": HostGroupsSourcesTypes.SPECIFIC_PROJECT_TAG},
            "approvement_decision": {"skip_approvement": True, "reason": "reason for 2 group"},
        },
    ]

    assert scenario.data_storage["host_groups_sources"] == host_groups_sources_expected_content
    assert host_groups_sources_example == data_storage.read_host_groups_sources()


def test_read_write_scenario_parameters(walle_test):
    scenario = walle_test.mock_scenario(
        {
            "scenario_type": itdc_maintenance_script.name,
        }
    )

    data_storage = get_data_storage(scenario)
    assert data_storage.read_scenario_parameters() == ItdcMaintenanceParams()

    current_time = timestamp()
    scenario_parameters = ItdcMaintenanceParams(maintenance_start_time=current_time, rack="some|path|to|rack")

    data_storage.write_scenario_parameters(scenario_parameters)

    scenario_parameters_expected_content = {
        "maintenance_start_time": current_time,
        "maintenance_end_time": None,
        "rack": "some|path|to|rack",
    }

    assert scenario.data_storage["scenario_parameters"] == scenario_parameters_expected_content
    assert scenario_parameters == data_storage.read_scenario_parameters()
