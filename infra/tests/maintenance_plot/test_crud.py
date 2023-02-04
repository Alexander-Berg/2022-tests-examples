import pytest

from infra.walle.server.tests.lib.maintenance_plot_util import NoopScenarioSettings
from walle.maintenance_plot import constants
from walle.maintenance_plot.crud import (
    add_maintenance_plot_orm_object,
    delete_maintenance_plot,
    get_maintenance_plot_orm_object,
    list_all_maintenance_plots_meta_infos,
)
from walle.maintenance_plot.exceptions import (
    MaintenancePlotAlreadyExistError,
    MaintenancePlotNotFoundError,
    MaintenancePlotScenarioSettingsAlreadyExistError,
    MaintenancePlotScenarioSettingsNotFoundError,
)
from walle.maintenance_plot.model import (
    MaintenancePlotCommonSettings,
    MaintenancePlotMetaInfo,
    MaintenancePlotScenarioSettings,
)
from walle.maintenance_plot.scenarios_settings.base import BaseScenarioMaintenancePlotSettings
from walle.scenario.constants import ScriptName


class TestMaintenancePlotCrud:
    def test_add_maintenance_plot(self, walle_test, monkeypatch):
        monkeypatch.setattr(
            constants,
            "SCENARIO_TYPES_SETTINGS_MAP",
            {
                ScriptName.NOOP: NoopScenarioSettings,
            },
        )

        plot_id = "some-mocked-id"

        with pytest.raises(MaintenancePlotNotFoundError):
            get_maintenance_plot_orm_object(plot_id)

        mocked_maintenance_plot = walle_test.mock_maintenance_plot(dict(id=plot_id), save=False)
        mocked_maintenance_plot_dataclass = mocked_maintenance_plot.as_dataclass()

        add_maintenance_plot_orm_object(mocked_maintenance_plot_dataclass)
        assert mocked_maintenance_plot_dataclass == get_maintenance_plot_orm_object(plot_id).as_dataclass()

        with pytest.raises(MaintenancePlotAlreadyExistError):
            add_maintenance_plot_orm_object(mocked_maintenance_plot_dataclass)

    def test_delete_maintenance_plot(self, walle_test, monkeypatch):
        monkeypatch.setattr(
            constants,
            "SCENARIO_TYPES_SETTINGS_MAP",
            {
                ScriptName.NOOP: NoopScenarioSettings,
            },
        )

        mocked_maintenance_plot = walle_test.mock_maintenance_plot()

        delete_maintenance_plot(mocked_maintenance_plot.id)
        with pytest.raises(MaintenancePlotNotFoundError):
            get_maintenance_plot_orm_object(mocked_maintenance_plot.id)

        with pytest.raises(MaintenancePlotNotFoundError):
            delete_maintenance_plot(mocked_maintenance_plot.id)

        walle_test.maintenance_plots.remove(mocked_maintenance_plot)

        walle_test.maintenance_plots.assert_equal()

    def test_list_all_maintenance_plots_meta_infos(self, walle_test):
        for _id in [
            "maintenance-plot-mock-1",
            "maintenance-plot-mock-2",
            "maintenance-plot-mock-3",
        ]:
            walle_test.mock_maintenance_plot(dict(id=_id))
        assert len(list_all_maintenance_plots_meta_infos()) == 3
        walle_test.maintenance_plots.assert_equal()

    def test_modify_common_settings(self, walle_test):
        new_common_settings_dict = {
            "maintenance_approvers": {
                "logins": ["my-login"],
                "abc_roles_codes": ["my-abc-role-code"],
                "abc_role_scope_slugs": ["my-abc-role-scope-slug"],
                "abc_duty_schedule_slugs": ["my-abc-duty-schedule-slug"],
            },
            "common_scenario_settings": {
                "total_number_of_active_hosts": 10,
                "dont_allow_start_scenario_if_total_number_of_active_hosts_more_than": 20,
            },
        }
        new_common_settings = MaintenancePlotCommonSettings.from_dict(new_common_settings_dict)

        maintenance_plot = walle_test.mock_maintenance_plot()
        maintenance_plot.set_common_settings(new_common_settings)

        assert maintenance_plot.common_settings == new_common_settings_dict
        walle_test.maintenance_plots.assert_equal()

    def test_modify_meta_info(self, walle_test):
        new_meta_info_dict = {"name": "my-name", "abc_service_slug": "my-abc-service-slug"}
        new_meta_info = MaintenancePlotMetaInfo.from_dict(new_meta_info_dict)

        maintenance_plot_mock = walle_test.mock_maintenance_plot()
        maintenance_plot_mock.set_meta_info(new_meta_info)

        assert maintenance_plot_mock.meta_info == new_meta_info_dict
        walle_test.maintenance_plots.assert_equal()


class TestScenarioSettingsCrud:

    SCENARIO_TYPE = ScriptName.NOOP
    SCENARIO_SETTINGS = NoopScenarioSettings
    SCENARIO_SETTINGS_DICT = {"foo": 42}
    NEW_SCENARIO_SETTINGS_DICT = {"foo": 84}

    SOME_SCENARIO_TYPE = "some-scenario-type"

    def test_scenario_settings_crud(self, walle_test, monkeypatch):
        monkeypatch.setattr(
            constants,
            "SCENARIO_TYPES_SETTINGS_MAP",
            {
                self.SCENARIO_TYPE: self.SCENARIO_SETTINGS,
                self.SOME_SCENARIO_TYPE: BaseScenarioMaintenancePlotSettings,
            },
        )

        maintenance_plot = walle_test.maintenance_plots.mock()

        # Get settings for unexistent scenario type results in exception.
        with pytest.raises(MaintenancePlotScenarioSettingsNotFoundError):
            maintenance_plot.get_scenario_settings("unexistent")

        noop_scenario_settings = MaintenancePlotScenarioSettings(
            scenario_type=self.SCENARIO_TYPE,
            settings=constants.SCENARIO_TYPES_SETTINGS_MAP.get(self.SCENARIO_TYPE).from_dict(
                self.SCENARIO_SETTINGS_DICT
            ),
        )

        # Get settings for scenario type.
        actual_noop_scenario_settings = maintenance_plot.get_scenario_settings(self.SCENARIO_TYPE)
        assert actual_noop_scenario_settings == noop_scenario_settings

        new_noop_scenario_settings = MaintenancePlotScenarioSettings(
            scenario_type=self.SCENARIO_TYPE,
            settings=constants.SCENARIO_TYPES_SETTINGS_MAP.get(self.SCENARIO_TYPE).from_dict(
                self.NEW_SCENARIO_SETTINGS_DICT
            ),
        )

        # Add settings for scenario type that already present in maintenance plot.
        with pytest.raises(MaintenancePlotScenarioSettingsAlreadyExistError):
            maintenance_plot.add_scenario_settings(new_noop_scenario_settings)

        # Delete settings for scenario type and check if they are deleted.
        maintenance_plot.delete_scenario_settings(self.SCENARIO_TYPE)
        with pytest.raises(MaintenancePlotScenarioSettingsNotFoundError):
            maintenance_plot.get_scenario_settings(self.SCENARIO_TYPE)

        # Add settings for scenario type.
        maintenance_plot.add_scenario_settings(new_noop_scenario_settings)
        actual_noop_scenario_settings = maintenance_plot.get_scenario_settings(self.SCENARIO_TYPE)
        assert actual_noop_scenario_settings == new_noop_scenario_settings

        # Modify settings for unexistent scenario type.
        some_scenario_type_settings = MaintenancePlotScenarioSettings(
            scenario_type=self.SOME_SCENARIO_TYPE, settings=BaseScenarioMaintenancePlotSettings
        )
        with pytest.raises(MaintenancePlotScenarioSettingsNotFoundError):
            maintenance_plot.modify_scenario_settings(some_scenario_type_settings)

        # Modify existent scenario settings.
        maintenance_plot.modify_scenario_settings(noop_scenario_settings)
        actual_noop_scenario_settings = maintenance_plot.get_scenario_settings(self.SCENARIO_TYPE)
        assert actual_noop_scenario_settings == noop_scenario_settings

        walle_test.maintenance_plots.assert_equal()
