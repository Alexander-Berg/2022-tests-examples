from datetime import datetime
from unittest.mock import Mock

import pytest

from infra.walle.server.tests.lib.util import monkeypatch_method, string_to_ts
from sepelib.core import constants
from walle.clients import ok, startrek
from walle.maintenance_plot.model import (
    MaintenanceApprovers,
    MaintenancePlotCommonSettings,
    MaintenancePlotMetaInfo,
    CommonScenarioSettings,
)
from walle.models import timestamp, monkeypatch_timestamp
from walle.scenario.constants import ScriptName
from walle.scenario.data_storage.base import HostGroupSource
from walle.scenario.data_storage.types import ApprovementDecision
from walle.scenario.definitions.base import get_data_storage
from walle.scenario.host_groups_builders.base import MaintenancePlotHostGroupSource
from walle.scenario.marker import Marker
from walle.scenario.script_args import ItdcMaintenanceParams
from walle.scenario.stage.host_group_approve_stage import ApprovementInfo, HostGroupApproveStage, RenewApproversStatus
from walle.scenario.stage.noc_hard_host_group_approve_stage import ManualConfirmationHostGroupApproveStage
from walle.scenario.stage_info import StageInfo
from walle.util.approvement_tools import ApproveClient

MOCK_CONFIG_OK_COORDINATORS_GROUP = ["mock-config-ok-coordinators-group"]
MOCK_CONFIG_STARTREK_QUEUE = "APPROVEMENTS"
MOCK_CONFIG_STARTREK_TICKET_SUMMARY_TEMPLATE = "Mocked {} Hosts maintenance approval for group {}"
MOCK_CONFIG_STARTREK_TICKET_TAGS = ["mock-config-startrek-ticket-tags"]
MOCK_MAINTENANCE_PLOT_ID = "some-maintenance-plot-id"


@pytest.fixture()
def mocks(mp, walle_test):
    mp.config("scenario.host_group_approve_stage.ok_coordinators_groups", MOCK_CONFIG_OK_COORDINATORS_GROUP)
    mp.config("scenario.host_group_approve_stage.startrek_queue", MOCK_CONFIG_STARTREK_QUEUE)
    mp.config(
        "scenario.host_group_approve_stage.startrek_ticket_summary_template",
        MOCK_CONFIG_STARTREK_TICKET_SUMMARY_TEMPLATE,
    )
    mp.config("scenario.host_group_approve_stage.startrek_ticket_tags", MOCK_CONFIG_STARTREK_TICKET_TAGS)
    monkeypatch_method(mp, method=ok.get_client, obj=ok, return_value=Mock())
    monkeypatch_method(mp, method=startrek.get_client, obj=startrek, return_value=Mock())


@pytest.mark.usefixtures("mocks")
class TestHostGroupApproveStageExecution:

    scenario_ticket_id = "SCENARIO-1"
    host_groups_sources = [
        HostGroupSource(0, MaintenancePlotHostGroupSource(maintenance_plot_id=MOCK_MAINTENANCE_PLOT_ID)),
    ]

    startrek_ticket_key = "APPROVEMENTS-1"
    ok_uuid = "some-approvement-uuid"
    call_approvers_startrek_comment_id = "42"
    call_comment_about_approvement_in_parent_ticket = "63"
    scenario_startrek_ticket_resolution_comment_id = "84"
    initial_approvers_logins = ["foo"]

    new_approvers_logins = ["bar", "baz"]
    ok_uuid_for_new_approvers = "new-approvement-uuid"
    call_approvers_startrek_comment_id_for_new_approvers = "43"

    @pytest.mark.usefixtures("monkeypatches_for_noop_scenario")
    def test_host_group_aprove_stage_execution(self, mp, walle_test):
        now = "01.01.2000 00:00"
        monkeypatch_timestamp(mp, string_to_ts(now))

        self._get_mocked_maintenance_plot(walle_test, self.initial_approvers_logins)
        scenario = walle_test.mock_scenario(
            {
                "scenario_type": ScriptName.NOOP,
                "ticket_key": self.scenario_ticket_id,
            }
        )
        get_data_storage(scenario).write_host_groups_sources(self.host_groups_sources)
        # Example of passing a config into the stage.
        stage = HostGroupApproveStage()
        stage_info = stage.serialize("0")

        # Ensure that there is no 'ApprovementInfo' stored.
        assert stage._read_approvement_info(stage_info) is None

        def _run_stage_and_check_actual_approvement_info(
            _expected_approvement_info, _expected_marker=Marker.in_progress()
        ):
            assert stage.run(stage_info, scenario, 0).status == _expected_marker.status
            try:
                assert stage._read_approvement_info(stage_info) == _expected_approvement_info
            except AssertionError:
                raise AssertionError(
                    "\nExpected approvement info:\n{}\nActual approvement info:\n{}\n".format(
                        _expected_approvement_info, stage._read_approvement_info(stage_info)
                    )
                )

        # Run: create Statrek ticket.
        monkeypatch_method(
            mp, method=ApproveClient.create_startrek_ticket, obj=ApproveClient, return_value=self.startrek_ticket_key
        )
        expected_approvement_info = ApprovementInfo(startrek_ticket_key=self.startrek_ticket_key)
        _run_stage_and_check_actual_approvement_info(expected_approvement_info)

        # Run: create approvement in Ok.
        create_ok_approvement_patch_attr = monkeypatch_method(
            mp, method=ApproveClient.create_ok_approvement, obj=ApproveClient, return_value=self.ok_uuid
        )
        expected_approvement_info.ok_uuid = self.ok_uuid
        expected_approvement_info.approvers_logins_in_ok_approvement = self.initial_approvers_logins
        expected_approvement_info.approvers_renew_last_ts = string_to_ts(now)
        _run_stage_and_check_actual_approvement_info(expected_approvement_info)

        # Run: put a comment into Startrek ticket.
        create_startrek_comment_patch_attr = monkeypatch_method(
            mp,
            method=ApproveClient.create_startrek_comment,
            obj=ApproveClient,
            return_value=self.call_approvers_startrek_comment_id,
        )
        expected_approvement_info.call_approvers_startrek_comment_id = self.call_approvers_startrek_comment_id
        _run_stage_and_check_actual_approvement_info(expected_approvement_info)

        # Run: get Ok approvement 'IN_PROGRESS' status and 'NO_RESOLUTION' resolution.
        get_ok_approvement_status_resolution_patch_attr = monkeypatch_method(
            mp,
            method=ApproveClient.get_ok_approvement_status_resolution,
            obj=ApproveClient,
            return_value=(ok.ApprovementStatus.IN_PROGRESS, ok.ApprovementResolution.NO_RESOLUTION),
        )
        expected_approvement_info.approvement_status = ok.ApprovementStatus.IN_PROGRESS
        expected_approvement_info.approvement_resolution = ok.ApprovementResolution.NO_RESOLUTION
        _run_stage_and_check_actual_approvement_info(expected_approvement_info)

        # Run: no changes in Ok approvement status - no changes in ApproveInfo.
        _run_stage_and_check_actual_approvement_info(expected_approvement_info)

        # Run: Some time passes, still no changes, wait for approvers cache to expire.
        now = "01.01.2000 00:05"
        monkeypatch_timestamp(mp, string_to_ts(now))
        _run_stage_and_check_actual_approvement_info(expected_approvement_info)

        # Run: Cache expires and is renewed (no changes in approvers).
        now = "01.01.2000 00:31"
        monkeypatch_timestamp(mp, string_to_ts(now))
        expected_approvement_info.approvers_renew_last_ts = string_to_ts(now)
        _run_stage_and_check_actual_approvement_info(expected_approvement_info)

        # Test stage behavior when maintenance approvers in maintenance plot has changed.
        # Run: reset OK UUID, StarTrek comment ID and saved approvement status and resolution.
        now = "01.01.2000 01:00"
        monkeypatch_timestamp(mp, string_to_ts(now))
        monkeypatch_method(
            mp,
            method=MaintenanceApprovers.get_approvers,
            obj=MaintenanceApprovers,
            return_value=self.new_approvers_logins,
        )

        monkeypatch_method(mp, method=ApproveClient.edit_approvers, obj=ApproveClient, side_effect=ok.OKBadRequest)
        monkeypatch_method(mp, method=ApproveClient.close_ok_approvement, obj=ApproveClient, return_value=True)
        expected_approvement_info.ok_uuid = None
        expected_approvement_info.call_approvers_startrek_comment_id = None
        expected_approvement_info.approvement_status = None
        expected_approvement_info.approvement_resolution = None
        expected_approvement_info.approvers_renew_last_ts = string_to_ts(now)
        expected_approvement_info.renew_approvers_status = RenewApproversStatus.NOT_STARTED
        _run_stage_and_check_actual_approvement_info(expected_approvement_info)

        # Run: Create OK approvement again.
        create_ok_approvement_patch_attr.return_value = self.ok_uuid_for_new_approvers
        expected_approvement_info.ok_uuid = self.ok_uuid_for_new_approvers
        expected_approvement_info.approvers_logins_in_ok_approvement = self.new_approvers_logins
        _run_stage_and_check_actual_approvement_info(expected_approvement_info)

        # Run: Write new comment to StarTrek approvement ticket again.
        create_startrek_comment_patch_attr.return_value = self.call_approvers_startrek_comment_id_for_new_approvers
        expected_approvement_info.call_approvers_startrek_comment_id = (
            self.call_approvers_startrek_comment_id_for_new_approvers
        )
        _run_stage_and_check_actual_approvement_info(expected_approvement_info)

        # Run: Wait for an approvement.
        now = "01.01.2000 01:01"
        monkeypatch_timestamp(mp, string_to_ts(now))
        expected_approvement_info.approvement_status = ok.ApprovementStatus.IN_PROGRESS
        expected_approvement_info.approvement_resolution = ok.ApprovementResolution.NO_RESOLUTION
        _run_stage_and_check_actual_approvement_info(expected_approvement_info)

        # Last run: we received an approve. Handle given approve and finish stage with Marker.success().
        get_ok_approvement_status_resolution_patch_attr.return_value = (
            ok.ApprovementStatus.CLOSED,
            ok.ApprovementResolution.APPROVED,
        )
        expected_approvement_info.approvement_status = ok.ApprovementStatus.CLOSED
        expected_approvement_info.approvement_resolution = ok.ApprovementResolution.APPROVED
        monkeypatch_method(mp, method=ApproveClient.close_startrek_ticket, obj=ApproveClient, return_value=None)
        expected_approvement_info.startrek_ticket_is_closed = True
        create_startrek_comment_patch_attr.return_value = self.scenario_startrek_ticket_resolution_comment_id
        expected_approvement_info.scenario_startrek_ticket_resolution_comment_id = (
            self.scenario_startrek_ticket_resolution_comment_id
        )
        marker_message = HostGroupApproveStage._marker_message_approvement_received
        _run_stage_and_check_actual_approvement_info(expected_approvement_info, Marker.success(message=marker_message))

    @pytest.mark.usefixtures("monkeypatches_for_noop_scenario")
    def test_host_group_aprove_stage_editing(self, mp, walle_test):
        now = "01.01.2000 00:00"
        monkeypatch_timestamp(mp, string_to_ts(now))

        self._get_mocked_maintenance_plot(walle_test, self.initial_approvers_logins)
        scenario = walle_test.mock_scenario(
            {
                "scenario_type": ScriptName.NOOP,
                "ticket_key": self.scenario_ticket_id,
            }
        )
        get_data_storage(scenario).write_host_groups_sources(self.host_groups_sources)

        stage = HostGroupApproveStage()

        info = ApprovementInfo()

        info.ok_uuid = "some-approvement-uuid"
        info.approvers_logins_in_ok_approvement = ["zivot", "sizoff"]

        now = "01.01.2030 00:00"
        monkeypatch_timestamp(mp, string_to_ts(now))

        mock_edit_approvers = monkeypatch_method(
            mp, method=ApproveClient.edit_approvers, obj=ApproveClient, side_effect=ok.OKBadRequest
        )

        mock_close_approvement = monkeypatch_method(mp, method=ApproveClient.close_ok_approvement, obj=ApproveClient)

        # Do we also need to mock & check create_ok_approvement?
        # Run: Change the approvers with expected 400 error output
        stage.handle_change_of_approvers(info, ["zivot", "sizoff", "alexsmirnov"])
        assert mock_edit_approvers.call_count == 1
        assert mock_close_approvement.call_count == 1

        # Run: Change the approvers with 200 output
        mock_edit_approvers.return_value = {"key": "value"}
        mock_edit_approvers.side_effect = None
        stage.handle_change_of_approvers(info, ["zivot", "sizoff", "alexsmirnov"])
        assert mock_edit_approvers.call_count == 2
        assert mock_close_approvement.call_count == 1

        # Run: While changing approvers connection to OK failed, additional 15 minutes of waiting

        del mock_edit_approvers
        del mock_close_approvement

        stage = HostGroupApproveStage()
        info = ApprovementInfo()

        info.ok_uuid = "some-approvement-uuid"
        info.approvers_logins_in_ok_approvement = ["zivot", "sizoff"]

        now = "01.01.2030 00:00"
        monkeypatch_timestamp(mp, string_to_ts(now))

        mock_handle_change = monkeypatch_method(
            mp,
            method=HostGroupApproveStage.handle_change_of_approvers,
            obj=HostGroupApproveStage,
            side_effect=ok.OKConnectionError,
        )
        maintenance_start_time = timestamp() + constants.HOUR_SECONDS * 50
        host_groups_sources = [
            HostGroupSource(0, MaintenancePlotHostGroupSource("mocked-maintenance-plot-id")),
        ]
        scenario_parameters = ItdcMaintenanceParams(maintenance_start_time=maintenance_start_time)

        walle_test.mock_maintenance_plot()
        scenario = walle_test.mock_scenario(
            dict(scenario_id=2, name="mocked-scenario-2", scenario_type=ScriptName.ITDC_MAINTENANCE)
        )
        data_storage = get_data_storage(scenario)
        data_storage.write_host_groups_sources(host_groups_sources)
        data_storage.write_scenario_parameters(scenario_parameters)

        mock_add_comment = monkeypatch_method(
            mp, obj=HostGroupApproveStage, method=HostGroupApproveStage._add_comment_about_changed_approvers
        )

        info.ok_uuid = "some-approvement-uuid"
        info.call_approvers_startrek_comment_id = "some-startrek-comment-id"
        info.approvement_status = ok.ApprovementStatus.IN_PROGRESS
        info.approvers_renew_last_ts = 0
        info.approvement_resolution = None
        stage_info = StageInfo()
        stage_info.data["approvement_info"] = info.to_dict()
        stage.run(stage_info, scenario, host_group_id=0)
        renew_last_ts = stage._read_approvement_info(stage_info).approvers_renew_last_ts
        assert renew_last_ts == timestamp() + 15 * constants.MINUTE_SECONDS

        # Run: While changing approvers unknown exception occured, additional 30 minutes of waiting
        mock_handle_change.side_effect = Exception
        stage = HostGroupApproveStage()
        stage_info = StageInfo()
        stage_info.data["approvement_info"] = info.to_dict()
        stage.run(stage_info, scenario, host_group_id=0)
        renew_last_ts = stage._read_approvement_info(stage_info).approvers_renew_last_ts
        assert renew_last_ts == timestamp() + 30 * constants.MINUTE_SECONDS

        # Run: While making comment connection to OK failed, additional 15 minutes of waiting
        del mock_handle_change
        mock_add_comment.side_effect = startrek.StartrekError("message")
        stage = HostGroupApproveStage()
        stage_info = StageInfo()
        stage_info.data["approvement_info"] = info.to_dict()
        stage.run(stage_info, scenario, host_group_id=0)
        renew_last_ts = stage._read_approvement_info(stage_info).approvers_renew_last_ts
        assert renew_last_ts == timestamp() + 15 * constants.MINUTE_SECONDS

        # Run: While making comment unknown exception occured, additional 30 minutes of waiting
        mock_add_comment.side_effect = Exception
        stage = HostGroupApproveStage()
        stage_info = StageInfo()
        stage_info.data["approvement_info"] = info.to_dict()
        stage.run(stage_info, scenario, host_group_id=0)
        renew_last_ts = stage._read_approvement_info(stage_info).approvers_renew_last_ts
        assert renew_last_ts == timestamp() + 30 * constants.MINUTE_SECONDS

        # Run: Both comment and changer failed with unknown error, check if waiting time added only once
        mock_handle_change = monkeypatch_method(
            mp,
            method=HostGroupApproveStage.handle_change_of_approvers,
            obj=HostGroupApproveStage,
            side_effect=Exception,
        )
        stage = HostGroupApproveStage()
        stage_info = StageInfo()
        stage_info.data["approvement_info"] = info.to_dict()
        stage.run(stage_info, scenario, host_group_id=0)
        renew_last_ts = stage._read_approvement_info(stage_info).approvers_renew_last_ts
        assert renew_last_ts == timestamp() + 30 * constants.MINUTE_SECONDS

    @pytest.mark.usefixtures("monkeypatches_for_noop_scenario")
    def test_host_group_aprove_stage_for_approved_host_group_source(self, walle_test, mp):
        monkeypatch_method(
            mp,
            method=ApproveClient.create_startrek_comment,
            obj=ApproveClient,
            return_value=self.call_approvers_startrek_comment_id,
        )
        walle_test.mock_maintenance_plot({"id": MOCK_MAINTENANCE_PLOT_ID})
        host_groups_sources = [
            HostGroupSource(
                0,
                MaintenancePlotHostGroupSource(maintenance_plot_id=MOCK_MAINTENANCE_PLOT_ID),
                ApprovementDecision(True, "mock-reason"),
            ),
        ]

        scenario = walle_test.mock_scenario(
            {
                "scenario_type": ScriptName.NOOP,
                "ticket_key": self.scenario_ticket_id,
            }
        )
        get_data_storage(scenario).write_host_groups_sources(host_groups_sources)
        # Example of passing a config into the stage.
        stage = HostGroupApproveStage()
        stage_info = stage.serialize("0")

        assert stage.run(stage_info, scenario, 0) == Marker.success(
            message=HostGroupApproveStage._marker_message_auto_approve_by_maintenance_plot
        )

    def test_get_maintenance_start_time_and_power_off_start_time(self, walle_test):
        host_group_id = 0
        walle_test.mock_maintenance_plot(
            dict(
                id=MOCK_MAINTENANCE_PLOT_ID,
                scenarios_settings=[
                    {
                        "scenario_type": ScriptName.ITDC_MAINTENANCE,
                        "settings": {
                            "request_cms_x_seconds_before_maintenance_start_time": 2 * 60 * 60,
                            "start_power_off_x_seconds_before_maintenance_start_time": 1 * 60 * 60,
                        },
                    }
                ],
            )
        )
        scenario = walle_test.mock_scenario(
            {
                "scenario_type": ScriptName.ITDC_MAINTENANCE,
                "ticket_key": self.scenario_ticket_id,
            }
        )
        host_groups_sources = [
            HostGroupSource(
                host_group_id,
                MaintenancePlotHostGroupSource(maintenance_plot_id=MOCK_MAINTENANCE_PLOT_ID),
                ApprovementDecision(False, "mock-reason"),
            ),
        ]
        scenario_parameters = ItdcMaintenanceParams(maintenance_start_time=3 * constants.HOUR_SECONDS)
        data_storage = get_data_storage(scenario)
        data_storage.write_host_groups_sources(host_groups_sources)
        data_storage.write_scenario_parameters(scenario_parameters)

        stage = HostGroupApproveStage()

        host_task_start_times = stage._get_maintenance_start_time_and_power_off_start_time(
            scenario, data_storage, host_group_id
        )

        assert host_task_start_times.start_time_of_power_off == datetime.fromtimestamp(
            2 * constants.HOUR_SECONDS
        ).strftime("%H:%M %d.%m.%Y")
        assert host_task_start_times.start_time_of_set_to_maintenance == datetime.fromtimestamp(
            constants.HOUR_SECONDS
        ).strftime("%H:%M %d.%m.%Y")

    @pytest.mark.parametrize("request_cms_time_is_set", (True, False))
    @pytest.mark.parametrize("power_off_time_is_set", (True, False))
    @pytest.mark.parametrize("scenario_type", (ScriptName.NOC_HARD, ScriptName.ITDC_MAINTENANCE))
    def test_maintenance_start_time_and_power_off_start_time_is_present_in_approvement_ticket(
        self, walle_test, scenario_type, power_off_time_is_set, request_cms_time_is_set
    ):
        host_group_id = 0
        scenario_settings = {"scenario_type": scenario_type}
        scenario_time_settings = {}
        if request_cms_time_is_set:
            scenario_time_settings.update({"request_cms_x_seconds_before_maintenance_start_time": 2 * 60 * 60})
        if power_off_time_is_set and scenario_type == ScriptName.ITDC_MAINTENANCE:
            scenario_time_settings.update({"start_power_off_x_seconds_before_maintenance_start_time": 1 * 60 * 60})
        if scenario_time_settings:
            scenario_settings["settings"] = scenario_time_settings

        walle_test.mock_maintenance_plot(
            {
                "id": MOCK_MAINTENANCE_PLOT_ID,
                "scenarios_settings": [scenario_settings],
            }
        )
        scenario = walle_test.mock_scenario(
            {
                "name": "mocked_scenario",
                "scenario_type": scenario_type,
                "ticket_key": self.scenario_ticket_id,
            }
        )
        host_groups_sources = [
            HostGroupSource(
                host_group_id,
                MaintenancePlotHostGroupSource(maintenance_plot_id=MOCK_MAINTENANCE_PLOT_ID),
                ApprovementDecision(False, "mock-reason"),
            ),
        ]

        data_storage = get_data_storage(scenario)
        scenario_parameters = data_storage.scenario_parameters_class(maintenance_start_time=3 * constants.HOUR_SECONDS)
        data_storage.write_host_groups_sources(host_groups_sources)
        data_storage.write_scenario_parameters(scenario_parameters)

        expected_lines = [
            '**Краткое описание работ:** "mocked_scenario" (подробная информация в корневом тикете - SCENARIO-1)',
            "",
        ]
        if request_cms_time_is_set:
            start_time_of_set_to_maintenance = datetime.fromtimestamp(constants.HOUR_SECONDS).strftime("%H:%M %d.%m.%Y")
            expected_lines.append(
                f"**Плановая дата начала переключения хостов в maintenance:** {start_time_of_set_to_maintenance}"
            )
        if power_off_time_is_set and scenario_type == ScriptName.ITDC_MAINTENANCE:
            start_time_of_power_off = datetime.fromtimestamp(2 * constants.HOUR_SECONDS).strftime("%H:%M %d.%m.%Y")
            expected_lines.append(f"**Плановая дата начала выключения хостов:** {start_time_of_power_off}")

        maintenance_start_time = datetime.fromtimestamp(3 * constants.HOUR_SECONDS).strftime("%d.%m.%Y %H:%M:%S")
        expected_lines.append(f"**Плановая дата начала работ в ДЦ (NOC/ITDC):** {maintenance_start_time}")
        expected_text = "\n".join(expected_lines)

        stage = HostGroupApproveStage()
        ticket_body = stage._get_startrek_approvement_ticket_text(scenario, data_storage, host_group_id)
        assert expected_text in ticket_body

        stage = ManualConfirmationHostGroupApproveStage(option_name="enable_manual_approval_after_hosts_power_off")
        ticket_body = stage._get_startrek_approvement_ticket_text(scenario, data_storage, host_group_id)
        assert expected_text in ticket_body

    @staticmethod
    def _get_mocked_maintenance_plot(walle_test, logins):
        return walle_test.mock_maintenance_plot(
            {
                "id": MOCK_MAINTENANCE_PLOT_ID,
                "meta_info": MaintenancePlotMetaInfo(
                    name="Some maintenance plot", abc_service_slug="some-abc-service-slug"
                ).to_dict(),
                "common_settings": MaintenancePlotCommonSettings(
                    maintenance_approvers=MaintenanceApprovers(
                        logins=logins,
                    ),
                    common_scenarios_settings=CommonScenarioSettings(),
                ).to_dict(),
            }
        )
