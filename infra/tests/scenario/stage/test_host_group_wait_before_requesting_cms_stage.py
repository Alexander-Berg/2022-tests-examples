import typing

import pytest

from infra.walle.server.tests.lib.util import string_to_ts
from walle.models import monkeypatch_timestamp
from walle.scenario.constants import ScriptName
from walle.scenario.data_storage.types import HostGroupSource
from walle.scenario.definitions.base import get_data_storage
from walle.scenario.host_groups_builders.base import MaintenancePlotHostGroupSource
from walle.scenario.marker import Marker
from walle.scenario.scenario import Scenario
from walle.scenario.script_args import ItdcMaintenanceParams
from walle.scenario.stage.host_group_wait_before_requesting_cms_stage import (
    HostGroupWaitBeforeRequestingCmsStage,
    CommonWaitBeforeRequestingCms,
)

MOCK_MAINTENANCE_PLOT_ID = "some-maintenance-plot-id"


class TestHostGroupWaitBeforeRequestingCmsStageExecution:

    scenario = None
    stage = None
    stage_info = None
    common = CommonWaitBeforeRequestingCms()

    def test_wait(self, mp, walle_test):
        self.stage = HostGroupWaitBeforeRequestingCmsStage()
        self.stage_info = self.stage.serialize("0")

        required_offset_seconds = 1 * 60 * 60
        self._mock_maintenance_plot(
            walle_test, request_cms_x_seconds_before_maintenance_start_time=required_offset_seconds
        )
        self.scenario = self._mock_scenario(walle_test, maintenance_start_time=string_to_ts("01.01.2000 12:00"))

        monkeypatch_timestamp(mp, string_to_ts("01.01.2000 00:00"))
        self._run_stage_and_check_result_marker(Marker.in_progress(message="Waiting until '%s'." % "01.01.2000 11:00"))

        monkeypatch_timestamp(mp, string_to_ts("01.01.2000 06:00"))
        self._run_stage_and_check_result_marker(Marker.in_progress(message="Waiting until '%s'." % "01.01.2000 11:00"))

        monkeypatch_timestamp(mp, string_to_ts("01.01.2000 11:00"))
        self._run_stage_and_check_result_marker(Marker.success(message="Had waited until '%s'." % "01.01.2000 11:00"))

    @pytest.mark.parametrize(
        ["maintenance_start_time_string", "request_cms_x_seconds_before_maintenance_start_time", "expected_marker"],
        [
            (None, None, Marker.success(message="Maintenance start time is not set in scenario parameters.")),
            (None, 42 * 60 * 60, Marker.success(message="Maintenance start time is not set in scenario parameters.")),
            (
                "01.01.2000 12:00",
                None,
                Marker.success(
                    message="Waiting before requesting CMS is not configured in maintenance plot '%s'."
                    % MOCK_MAINTENANCE_PLOT_ID
                ),
            ),
        ],
    )
    def test_corner_cases(
        self,
        walle_test,
        maintenance_start_time_string,
        request_cms_x_seconds_before_maintenance_start_time,
        expected_marker,
    ):
        self.stage = HostGroupWaitBeforeRequestingCmsStage()
        self.stage_info = self.stage.serialize("0")

        self.scenario = self._mock_scenario(
            walle_test,
            string_to_ts(maintenance_start_time_string) if maintenance_start_time_string is not None else None,
        )
        self._mock_maintenance_plot(walle_test, request_cms_x_seconds_before_maintenance_start_time)
        self._run_stage_and_check_result_marker(expected_marker)  # noqa

    def test_cache(self, mp, walle_test):
        self.stage = HostGroupWaitBeforeRequestingCmsStage()
        self.stage_info = self.stage.serialize("0")

        required_offset_seconds = 1 * 60 * 60
        self._mock_maintenance_plot(
            walle_test, request_cms_x_seconds_before_maintenance_start_time=required_offset_seconds
        )
        self.scenario = self._mock_scenario(walle_test, maintenance_start_time=string_to_ts("01.01.2000 12:00"))

        monkeypatch_timestamp(mp, string_to_ts("01.01.2000 00:00"))
        self.stage.run(self.stage_info, self.scenario, 0)
        self._assert_stage_cache_content(
            {
                "maintenance_plot_id": MOCK_MAINTENANCE_PLOT_ID,
                "required_offset": required_offset_seconds,
                "valid_until": string_to_ts("01.01.2000 00:15"),
            }
        )

        monkeypatch_timestamp(mp, string_to_ts("01.01.2000 00:05"))
        self.stage.run(self.stage_info, self.scenario, 0)
        self._assert_stage_cache_content(
            {
                "maintenance_plot_id": MOCK_MAINTENANCE_PLOT_ID,
                "required_offset": required_offset_seconds,
                "valid_until": string_to_ts("01.01.2000 00:15"),
            }
        )

        monkeypatch_timestamp(mp, string_to_ts("01.01.2000 00:20"))
        self.stage.run(self.stage_info, self.scenario, 0)
        self._assert_stage_cache_content(
            {
                "maintenance_plot_id": MOCK_MAINTENANCE_PLOT_ID,
                "required_offset": required_offset_seconds,
                "valid_until": string_to_ts("01.01.2000 00:35"),
            }
        )

    def _assert_stage_cache_content(self, expected_cache_content: typing.Optional[dict]):
        assert self.stage_info.get_data(self.common._cache_field_name) == expected_cache_content

    def _run_stage_and_check_result_marker(self, expected_marker: Marker):
        actual_marker = self.stage.run(self.stage_info, self.scenario, 0)
        assert actual_marker.status == expected_marker.status
        assert actual_marker.message == expected_marker.message

    @staticmethod
    def _mock_maintenance_plot(walle_test, request_cms_x_seconds_before_maintenance_start_time: typing.Optional[int]):
        walle_test.mock_maintenance_plot(
            dict(
                id=MOCK_MAINTENANCE_PLOT_ID,
                scenarios_settings=[
                    {
                        "scenario_type": ScriptName.ITDC_MAINTENANCE,
                        "settings": {
                            "request_cms_x_seconds_before_maintenance_start_time": request_cms_x_seconds_before_maintenance_start_time,
                        },
                    }
                ],
            )
        )

    @staticmethod
    def _mock_scenario(walle_test, maintenance_start_time: typing.Optional[int]) -> Scenario:
        scenario = walle_test.mock_scenario(
            {
                "scenario_type": ScriptName.ITDC_MAINTENANCE,
            }
        )
        data_storage = get_data_storage(scenario)

        host_groups_sources = [
            HostGroupSource(0, MaintenancePlotHostGroupSource(maintenance_plot_id=MOCK_MAINTENANCE_PLOT_ID)),
            HostGroupSource(1, MaintenancePlotHostGroupSource(maintenance_plot_id="unused")),
        ]
        data_storage.write_host_groups_sources(host_groups_sources)

        scenario_parameters = ItdcMaintenanceParams(maintenance_start_time=maintenance_start_time)
        data_storage.write_scenario_parameters(scenario_parameters)

        return scenario
