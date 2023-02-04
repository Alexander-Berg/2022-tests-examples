import pytest

from sepelib.core import constants
from walle.maintenance_plot import constants as maintenance_plot_constants
from walle.maintenance_plot.model import MaintenancePlotMetaInfo, MaintenancePlotScenarioSettings
from walle.models import timestamp, monkeypatch_timestamp
from walle.scenario.constants import ScriptName
from walle.scenario.data_storage.types import HostGroupSource
from walle.scenario.definitions.base import get_data_storage
from walle.scenario.host_groups_builders.base import MaintenancePlotHostGroupSource
from walle.scenario.maintenance_plot_checks import (
    MaintenancePlotCheckResult,
    check_if_maintenance_start_time_is_too_soon,
)
from walle.scenario.script_args import ItdcMaintenanceParams


class TestMaintenancePlotChecks:
    @pytest.mark.parametrize(
        (
            "maintenance_start_time_offset, "
            "foo_plot_reject_value_hours, "
            "bar_plot_reject_value_hours, "
            "expected_result"
        ),
        [
            (constants.HOUR_SECONDS * 50, 10, 20, MaintenancePlotCheckResult(check_passed=True, messages=[])),
            (
                constants.HOUR_SECONDS * 15,
                constants.HOUR_SECONDS * 10,
                constants.HOUR_SECONDS * 20,
                MaintenancePlotCheckResult(
                    check_passed=False,
                    messages=[
                        (
                            "Maintenance start time is too soon for hosts belonging to ABC service abc-bar, "
                            "required offset is {} seconds".format(constants.HOUR_SECONDS * 20)
                        ),
                    ],
                ),
            ),
            (
                constants.HOUR_SECONDS * 5,
                constants.HOUR_SECONDS * 10,
                constants.HOUR_SECONDS * 20,
                MaintenancePlotCheckResult(
                    check_passed=False,
                    messages=[
                        (
                            "Maintenance start time is too soon for hosts belonging to ABC service abc-foo, "
                            "required offset is {} seconds".format(constants.HOUR_SECONDS * 10)
                        ),
                        (
                            "Maintenance start time is too soon for hosts belonging to ABC service abc-bar, "
                            "required offset is {} seconds".format(constants.HOUR_SECONDS * 20)
                        ),
                    ],
                ),
            ),
            (constants.HOUR_SECONDS * 5, None, None, MaintenancePlotCheckResult(check_passed=True, messages=[])),
        ],
    )
    def test_check_if_maintenance_start_time_is_too_soon(
        self,
        walle_test,
        mp,
        maintenance_start_time_offset,
        foo_plot_reject_value_hours,
        bar_plot_reject_value_hours,
        expected_result,
    ):
        monkeypatch_timestamp(mp)
        maintenance_start_time = timestamp() + maintenance_start_time_offset

        host_groups_sources = [
            HostGroupSource(0, MaintenancePlotHostGroupSource("foo")),
            HostGroupSource(1, MaintenancePlotHostGroupSource("bar")),
        ]
        scenario_parameters = ItdcMaintenanceParams(maintenance_start_time=maintenance_start_time)

        scenario = walle_test.mock_scenario(dict(scenario_type=ScriptName.ITDC_MAINTENANCE))
        data_storage = get_data_storage(scenario)
        data_storage.write_host_groups_sources(host_groups_sources)
        data_storage.write_scenario_parameters(scenario_parameters)

        self._mock_maintenance_plot(walle_test, "foo", foo_plot_reject_value_hours)
        self._mock_maintenance_plot(walle_test, "bar", bar_plot_reject_value_hours)

        actual_result = check_if_maintenance_start_time_is_too_soon(scenario)
        assert expected_result.check_passed == actual_result.check_passed
        assert sorted(expected_result.messages) == sorted(actual_result.messages)

    @staticmethod
    def _mock_maintenance_plot(walle_test, _id, reject_value):
        meta_info = MaintenancePlotMetaInfo(abc_service_slug=f"abc-{_id}", name=_id)
        itdc_maintenance_settings = MaintenancePlotScenarioSettings(
            scenario_type=ScriptName.ITDC_MAINTENANCE,
            settings=maintenance_plot_constants.SCENARIO_TYPES_SETTINGS_MAP.get(ScriptName.ITDC_MAINTENANCE)(
                approval_sla=reject_value,
            ),
        )

        walle_test.mock_maintenance_plot(
            dict(id=_id, meta_info=meta_info.to_dict(), scenarios_settings=[itdc_maintenance_settings.to_dict()])
        )
