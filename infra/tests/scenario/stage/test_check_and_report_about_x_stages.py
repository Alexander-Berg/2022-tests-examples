import dataclasses
import typing
from unittest import mock

from sepelib.core import constants
from walle.hosts import HostState
from walle.maintenance_plot import constants as maintenance_plot_constants
from walle.maintenance_plot.model import MaintenancePlotMetaInfo, MaintenancePlotScenarioSettings
from walle.models import monkeypatch_timestamp
from walle.restrictions import REBOOT, AUTOMATED_REDEPLOY, AUTOMATION
from walle.scenario.constants import ScriptName
from walle.scenario.data_storage.types import HostGroupSource
from walle.scenario.definitions.base import get_data_storage
from walle.scenario.host_groups_builders.base import MaintenancePlotHostGroupSource
from walle.scenario.marker import Marker, MarkerStatus
from walle.scenario.mixins import BaseStage
from walle.scenario.scenario import Scenario
from walle.scenario.script_args import ItdcMaintenanceParams
from walle.scenario.stage_info import StageInfo
from walle.scenario.stages import (
    CheckAndReportAboutDeadlinesStage,
    StartrekCommentInfo,
    CheckAndReportAboutHostRestrictionsStage,
)

MOCK_MESSAGE = "test message"
MOCK_MAINTENANCE_PLOT_ID = "some-maintenance-plot-id"
MOCK_COMMENT_ID = "some-comment-id"
MOCK_TICKET_KEY = "mock-ticket-key"
MOCK_STARTREK_COMMENT_INFO = StartrekCommentInfo(MOCK_TICKET_KEY, MOCK_COMMENT_ID)


def _make_mock_stage(status):
    class MockStage(BaseStage):
        run = mock.Mock(return_value=Marker(status, message=MOCK_MESSAGE))

    return MockStage()


def _get_stage_and_stage_info_for_deadlines(status=MarkerStatus.SUCCESS):
    stage = CheckAndReportAboutDeadlinesStage([_make_mock_stage(status=status)])
    stage_info = StageInfo(stages=[StageInfo()], seq_num=0)
    return stage, stage_info


def _get_stage_and_stage_info_for_restrictions(restrictions_to_check=None):
    stage = CheckAndReportAboutHostRestrictionsStage(restrictions_to_check=restrictions_to_check)
    stage_info = StageInfo()
    return stage, stage_info


def _mock_scenario(
    walle_test,
    maintenance_start_time: typing.Optional[int],
    host_state: str = HostState.MAINTENANCE,
    by_start_time: int = True,
    by_x_seconds: typing.Optional[int] = 1,
    restrictions: typing.Optional[list[str]] = None,
) -> Scenario:
    itdc_maintenance_settings = MaintenancePlotScenarioSettings(
        scenario_type=ScriptName.ITDC_MAINTENANCE,
        settings=maintenance_plot_constants.SCENARIO_TYPES_SETTINGS_MAP.get(ScriptName.ITDC_MAINTENANCE)(
            get_approvers_to_ticket_if_hosts_not_in_maintenance_by_start_time=by_start_time,
            get_approvers_to_ticket_if_hosts_not_in_maintenance_by_x_seconds=by_x_seconds,
            enable_manual_approval_after_hosts_power_off=False,
            enable_redeploy_after_change_of_mac_address=True,
        ),
    )

    meta_info = MaintenancePlotMetaInfo(abc_service_slug="abc-test", name="test")

    walle_test.mock_maintenance_plot(
        dict(
            id=MOCK_MAINTENANCE_PLOT_ID,
            meta_info=meta_info.to_dict(),
            scenarios_settings=[itdc_maintenance_settings.to_dict()],
        )
    )

    project = walle_test.mock_project(dict(maintenance_plot_id=MOCK_MAINTENANCE_PLOT_ID, id="test-plot-options"))
    host = walle_test.mock_host(dict(inv=1, state=host_state, project=project.id, restrictions=restrictions))

    scenario = walle_test.mock_scenario(
        {"scenario_type": ScriptName.ITDC_MAINTENANCE, "hosts": [host.inv], "ticket_key": MOCK_TICKET_KEY}
    )
    data_storage = get_data_storage(scenario)

    host_groups_sources = [
        HostGroupSource(0, MaintenancePlotHostGroupSource(maintenance_plot_id=MOCK_MAINTENANCE_PLOT_ID)),
    ]
    data_storage.write_host_groups_sources(host_groups_sources)

    scenario_parameters = ItdcMaintenanceParams(maintenance_start_time=maintenance_start_time)
    data_storage.write_scenario_parameters(scenario_parameters)

    return scenario


def _expand_startrek_client(startrek_client):
    startrek_client.attach_mock(mock.Mock(return_value={"id": MOCK_COMMENT_ID}), "add_comment")
    return startrek_client


def test_scenario_without_start_time_but_with_hosts_not_in_maintenance(walle_test, mp, startrek_client):
    monkeypatch_timestamp(mp, 0)
    _expand_startrek_client(startrek_client)

    scenario = _mock_scenario(walle_test, None, HostState.ASSIGNED)
    stage, stage_info = _get_stage_and_stage_info_for_deadlines()

    assert stage.run(stage_info, scenario, 0).status == MarkerStatus.SUCCESS
    startrek_client.add_comment.assert_not_called()


def test_scenario_with_start_time_but_without_hosts_not_in_maintenance(walle_test, mp, startrek_client):
    monkeypatch_timestamp(mp, 0)
    _expand_startrek_client(startrek_client)

    scenario = _mock_scenario(walle_test, 0)
    stage, stage_info = _get_stage_and_stage_info_for_deadlines()

    assert stage.run(stage_info, scenario, 0).status == MarkerStatus.SUCCESS
    startrek_client.add_comment.assert_not_called()


def test_scenario_without_activated_options_in_maintenance_plot(walle_test, mp, startrek_client):
    monkeypatch_timestamp(mp, 0)
    _expand_startrek_client(startrek_client)

    scenario = _mock_scenario(walle_test, 0, HostState.ASSIGNED, by_start_time=False, by_x_seconds=None)
    stage, stage_info = _get_stage_and_stage_info_for_deadlines()

    assert stage.run(stage_info, scenario, 0).status == MarkerStatus.SUCCESS
    startrek_client.add_comment.assert_not_called()


def test_scenario_with_by_x_hours_option(walle_test, mp, startrek_client):
    monkeypatch_timestamp(mp, 1 * constants.HOUR_SECONDS + 1)
    _expand_startrek_client(startrek_client)

    scenario = _mock_scenario(
        walle_test,
        2 * constants.HOUR_SECONDS,
        HostState.ASSIGNED,
        by_start_time=False,
        by_x_seconds=constants.HOUR_SECONDS,
    )
    stage, stage_info = _get_stage_and_stage_info_for_deadlines()

    assert stage.run(stage_info, scenario, 0).status == MarkerStatus.SUCCESS
    startrek_client.add_comment.assert_called_once_with(
        issue_id=scenario.ticket_key, summonees=['login-1-mock', 'login-2-mock'], text=mock.ANY
    )
    assert stage._startrek_comment_id_for_comment_in_start_time not in stage_info.data
    assert stage_info.data[stage._startrek_comment_id_for_comment_in_X_hours] == dataclasses.asdict(
        MOCK_STARTREK_COMMENT_INFO
    )


def test_scenario_with_by_start_option(walle_test, mp, startrek_client):
    monkeypatch_timestamp(mp, 1 * constants.HOUR_SECONDS + 1)
    _expand_startrek_client(startrek_client)

    scenario = _mock_scenario(
        walle_test, 1 * constants.HOUR_SECONDS, HostState.ASSIGNED, by_start_time=True, by_x_seconds=None
    )
    stage, stage_info = _get_stage_and_stage_info_for_deadlines()

    assert stage.run(stage_info, scenario, 0).status == MarkerStatus.SUCCESS
    startrek_client.add_comment.assert_called_once_with(
        issue_id=scenario.ticket_key, summonees=['login-1-mock', 'login-2-mock'], text=mock.ANY
    )
    assert stage_info.data[stage._startrek_comment_id_for_comment_in_start_time] == dataclasses.asdict(
        MOCK_STARTREK_COMMENT_INFO
    )
    assert stage._startrek_comment_id_for_comment_in_X_hours not in stage_info.data


def test_scenario_with_both_options(walle_test, mp, startrek_client):
    monkeypatch_timestamp(mp, 0)
    _expand_startrek_client(startrek_client)
    scenario = _mock_scenario(
        walle_test,
        2 * constants.HOUR_SECONDS,
        HostState.ASSIGNED,
        by_start_time=True,
        by_x_seconds=constants.HOUR_SECONDS,
    )

    stage, stage_info = _get_stage_and_stage_info_for_deadlines()
    assert stage.run(stage_info, scenario, 0).status == MarkerStatus.SUCCESS
    startrek_client.add_comment.assert_not_called()
    assert stage._startrek_comment_id_for_comment_in_start_time not in stage_info.data
    assert stage._startrek_comment_id_for_comment_in_X_hours not in stage_info.data

    monkeypatch_timestamp(mp, 1 * constants.HOUR_SECONDS + 1)
    assert stage.run(stage_info, scenario, 0).status == MarkerStatus.SUCCESS
    startrek_client.add_comment.assert_called_once_with(
        issue_id=scenario.ticket_key, summonees=['login-1-mock', 'login-2-mock'], text=mock.ANY
    )
    assert stage_info.data[stage._startrek_comment_id_for_comment_in_X_hours] == dataclasses.asdict(
        MOCK_STARTREK_COMMENT_INFO
    )
    assert stage._startrek_comment_id_for_comment_in_start_time not in stage_info.data

    monkeypatch_timestamp(mp, 2 * constants.HOUR_SECONDS + 1)
    assert stage.run(stage_info, scenario, 0).status == MarkerStatus.SUCCESS
    startrek_client.add_comment.assert_has_calls(
        [
            mock.call(issue_id=scenario.ticket_key, summonees=['login-1-mock', 'login-2-mock'], text=mock.ANY),
            mock.call(issue_id=scenario.ticket_key, summonees=['login-1-mock', 'login-2-mock'], text=mock.ANY),
        ]
    )
    assert stage_info.data[stage._startrek_comment_id_for_comment_in_X_hours] == dataclasses.asdict(
        MOCK_STARTREK_COMMENT_INFO
    )
    assert stage_info.data[stage._startrek_comment_id_for_comment_in_start_time] == dataclasses.asdict(
        MOCK_STARTREK_COMMENT_INFO
    )


def test_report_about_host_restrictions(walle_test, mp, startrek_client):
    scenario = _mock_scenario(walle_test, 0, restrictions=[REBOOT])
    stage, stage_info = _get_stage_and_stage_info_for_restrictions(restrictions_to_check=[REBOOT])

    assert stage.run(stage_info, scenario, 0).status == MarkerStatus.SUCCESS
    startrek_client.add_comment.assert_called_once_with(
        issue_id=scenario.ticket_key, summonees=['login-1-mock', 'login-2-mock'], text=mock.ANY
    )


def test_report_about_automated_redeploy_if_automation_restriction_set(walle_test, mp, startrek_client):
    scenario = _mock_scenario(walle_test, 0, restrictions=[AUTOMATION])
    stage, stage_info = _get_stage_and_stage_info_for_restrictions(restrictions_to_check=[AUTOMATED_REDEPLOY])

    assert stage.run(stage_info, scenario, 0).status == MarkerStatus.SUCCESS
    startrek_client.add_comment.assert_called_once_with(
        issue_id=scenario.ticket_key, summonees=['login-1-mock', 'login-2-mock'], text=mock.ANY
    )


def test_dont_report_about_host_restrictions(walle_test, mp, startrek_client):
    scenario = _mock_scenario(walle_test, 0)
    stage, stage_info = _get_stage_and_stage_info_for_restrictions(restrictions_to_check=[AUTOMATION])

    assert stage.run(stage_info, scenario, 0).status == MarkerStatus.SUCCESS
    startrek_client.add_comment.assert_not_called()
