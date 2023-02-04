from walle.maintenance_plot.common_settings import MaintenanceApprovers
from walle.maintenance_plot.model import (
    MaintenancePlot,
    MaintenancePlotMetaInfo,
    MaintenancePlotCommonSettings,
    MaintenancePlotScenarioSettings,
    CommonScenarioSettings,
)
from walle.maintenance_plot.scenarios_settings.itdc_maintenance import ItdcMaintenanceMaintenancePlotSettings
from walle.maintenance_plot.scenarios_settings.noc_hard import NocHardMaintenancePlotSettings
from walle.scenario.constants import ScriptName

ID = "mock-id"
META_INFO = MaintenancePlotMetaInfo(name="Some maintenance plot", abc_service_slug="some-abc-service-slug")
COMMON_SETTINGS = MaintenancePlotCommonSettings(
    maintenance_approvers=MaintenanceApprovers(logins=["some-login"], abc_roles_codes=["some-abc-role-code"]),
    common_scenarios_settings=CommonScenarioSettings(),
)
MOCK_SCENARIO_SETTINGS = [
    MaintenancePlotScenarioSettings(
        scenario_type=ScriptName.ITDC_MAINTENANCE, settings=ItdcMaintenanceMaintenancePlotSettings()
    ),
    MaintenancePlotScenarioSettings(scenario_type=ScriptName.NOC_HARD, settings=NocHardMaintenancePlotSettings()),
]

MOCK_MAINTENANCE_PLOT_OBJ = MaintenancePlot(
    id=ID, meta_info=META_INFO, common_settings=COMMON_SETTINGS, scenarios_settings=MOCK_SCENARIO_SETTINGS
)
