import pytest

from walle import maintenance_plot
from walle import scenario
from walle.clients import abc
from walle.maintenance_plot.model import (
    MaintenancePlot,
    MaintenancePlotMetaInfo,
    MaintenancePlotCommonSettings,
    MaintenancePlotScenarioSettings,
    MaintenanceApprovers,
    CommonScenarioSettings,
)
from walle.maintenance_plot.scenarios_settings.itdc_maintenance import ItdcMaintenanceMaintenancePlotSettings
from walle.maintenance_plot.scenarios_settings.noc_hard import NocHardMaintenancePlotSettings
from walle.scenario import host_groups_builders
from walle.scenario.constants import FixedMaintenanceApproversLogins, ScriptName
from walle.scenario.host_groups_builders.base import (
    MaintenancePlotHostGroupSource,
    BotProjectIdHostGroupSource,
    SpecificProjectTagHostGroupSource,
)

_ID = "some-maintenance-plot-id"


class MockHostGroupsBuildersSpecificProjectTags:
    NOOP_PROJECT_TAG = "noop"


class NoopProjectTagMaintenancePlotData:
    ID = "noop-id"
    META_INFO = {
        "name": (
            f"Hard-coded maintenance plot for projects with "
            f"'{MockHostGroupsBuildersSpecificProjectTags.NOOP_PROJECT_TAG}' tag"
        )
    }
    COMMON_SETTINGS = {}
    SCENARIOS_SETTINGS = []


@pytest.fixture
def monkeypatches_for_noop_specific_project_tag(mp):
    mp.setattr(scenario.constants, "HostGroupsBuildersSpecificProjectTags", MockHostGroupsBuildersSpecificProjectTags)

    mp.setattr(
        host_groups_builders.by_specific_project_tag,
        "SPECIFIC_PROJECT_TAGS_LIST",
        [
            MockHostGroupsBuildersSpecificProjectTags.NOOP_PROJECT_TAG,
        ],
    )

    mp.setattr(
        scenario.constants,
        "SPECIFIC_PROJECT_TAG_TO_MAINTENANCE_PLOT_DATA_MAP",
        {
            MockHostGroupsBuildersSpecificProjectTags.NOOP_PROJECT_TAG: NoopProjectTagMaintenancePlotData,
        },
    )


@pytest.mark.usefixtures("monkeypatches_for_noop_scenario")
class TestMaintenancePlotGetters:
    def test_by_maintenance_plot_id(self, walle_test):

        meta_info = MaintenancePlotMetaInfo(name="Some maintenance plot", abc_service_slug="some-abc-service-slug")
        common_settings = MaintenancePlotCommonSettings(
            maintenance_approvers=MaintenanceApprovers(logins=["some-login"], abc_roles_codes=["some-abc-role-code"]),
            common_scenarios_settings=CommonScenarioSettings(),
        )

        walle_test.mock_maintenance_plot(
            {
                "id": _ID,
                "meta_info": meta_info.to_dict(),
                "common_settings": common_settings.to_dict(),
            }
        )

        expected_maintenance_plot = MaintenancePlot(
            id=_ID,
            meta_info=meta_info,
            common_settings=common_settings,
            scenarios_settings=[
                MaintenancePlotScenarioSettings(
                    scenario_type=ScriptName.NOOP,
                    settings=maintenance_plot.constants.SCENARIO_TYPES_SETTINGS_MAP.get(ScriptName.NOOP)(foo=42),
                )
            ],
        )

        host_group_source = MaintenancePlotHostGroupSource(maintenance_plot_id=_ID)
        assert host_group_source.get_maintenance_plot() == expected_maintenance_plot

    def test_by_abc_service_slug(self):
        abc_service_slug = "some-abc-service-slug"

        expected_maintenance_plot = MaintenancePlot(
            id=abc_service_slug,
            meta_info=MaintenancePlotMetaInfo(
                abc_service_slug=abc_service_slug,
                name="Auto-generated maintenance plot for ABC service 'some-abc-service-slug'",
            ),
            common_settings=self._get_default_maintenance_plot_common_settings(),
            scenarios_settings=self._get_default_maintenance_plot_scenarios_settings(),
        )

        host_group_source = BotProjectIdHostGroupSource(bot_project_id=42, abc_service_slug=abc_service_slug)
        actual_maintenance_plot = host_group_source.get_maintenance_plot()
        assert actual_maintenance_plot == expected_maintenance_plot

    @pytest.mark.usefixtures("monkeypatches_for_noop_specific_project_tag")
    def test_by_specific_project_tag(self):
        # Checking only resulting maintenance plot's name actually.
        for specific_project_tag in host_groups_builders.by_specific_project_tag.SPECIFIC_PROJECT_TAGS_LIST:
            expected_maintenance_plot_name = scenario.constants.SPECIFIC_PROJECT_TAG_TO_MAINTENANCE_PLOT_DATA_MAP.get(
                specific_project_tag
            ).META_INFO.get("name")

            host_group_source = SpecificProjectTagHostGroupSource(specific_project_tag=specific_project_tag)
            actual_maintenance_plot = host_group_source.get_maintenance_plot()

            assert actual_maintenance_plot.meta_info.name == expected_maintenance_plot_name

    @staticmethod
    def _get_default_maintenance_plot_common_settings():
        return MaintenancePlotCommonSettings(
            maintenance_approvers=MaintenanceApprovers(
                abc_roles_codes=[abc.Role.PRODUCT_HEAD, abc.Role.HARDWARE_RESOURCES_MANAGER],
            ),
            common_scenarios_settings=CommonScenarioSettings(),
        )

    @staticmethod
    def _get_default_maintenance_plot_scenarios_settings():
        return [
            MaintenancePlotScenarioSettings(
                scenario_type=ScriptName.ITDC_MAINTENANCE,
                settings=ItdcMaintenanceMaintenancePlotSettings(
                    enable_manual_approval_after_hosts_power_off=True,
                    approval_sla=1 * 24,
                ),
            ),
            MaintenancePlotScenarioSettings(
                scenario_type=ScriptName.NOC_HARD,
                settings=NocHardMaintenancePlotSettings(
                    enable_manual_approval_after_hosts_power_off=True,
                    approval_sla=1 * 24,
                ),
            ),
        ]
