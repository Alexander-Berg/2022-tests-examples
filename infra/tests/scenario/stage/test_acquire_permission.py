from functools import partial

import pytest

from infra.walle.server.tests.lib.util import monkeypatch_config
from walle.maintenance_plot.model import MaintenanceApprovers, MaintenancePlotCommonSettings, CommonScenarioSettings
from walle.models import monkeypatch_timestamp
from walle.scenario.constants import (
    DATACENTER_LABEL_NAME,
    ScenarioFsmStatus,
    ScriptName,
    ScenarioWorkStatus,
    WORK_STATUS_LABEL_NAME,
)
from walle.scenario.data_storage.base import HostGroupSource
from walle.scenario.data_storage.types import ApprovementDecision
from walle.scenario.definitions.base import get_data_storage
from walle.scenario.host_groups_builders.base import MaintenancePlotHostGroupSource
from walle.scenario.marker import Marker, MarkerStatus
from walle.scenario.scenario import Scenario
from walle.scenario.stage.acquire_permission import AcquirePermission, _REASON
from walle.scenario.stage_info import StageInfo

_USER_LIMIT_NAME = "max_started_scenarios_per_user"
_GLOBAL_LIMIT_NAME = "max_started_scenarios"
MOCK_CONFIG_OK_COORDINATORS_GROUP = ["mock-config-ok-coordinators-group"]
MOCK_CONFIG_STARTREK_QUEUE = "APPROVEMENTS"
MOCK_CONFIG_STARTREK_TICKET_SUMMARY_TEMPLATE = "Mocked {} Hosts maintenance approval for group {}"
MOCK_CONFIG_STARTREK_TICKET_TAGS = ["mock-config-startrek-ticket-tags"]
MOCK_MAINTENANCE_PLOT_ID_1 = "some-maintenance-plot-id-1"
MOCK_MAINTENANCE_PLOT_ID_2 = "some-maintenance-plot-id-2"


@pytest.fixture(autouse=True)
def mp_scenario_limits(mp):
    # set big defaults to free up scenarios from these
    _mp_scenario_limit(mp, _GLOBAL_LIMIT_NAME, float("inf"))
    _mp_scenario_limit(mp, _USER_LIMIT_NAME, float("inf"))


def _mp_scenario_limit(mp, limit_name, limit_value):
    monkeypatch_config(mp, 'scenario.{}'.format(limit_name), limit_value)


@pytest.fixture()
def timestamp_patcher(mp):
    monkeypatch_timestamp(mp, 0)
    return partial(monkeypatch_timestamp, mp)


def run_scenario(walle_test, scenario_type=None, user=None, labels=None):
    scenario_id = Scenario.next_id()
    scenario = walle_test.mock_scenario(
        overrides={
            "scenario_id": scenario_id,
            "name": "scenario-{}".format(scenario_id),
            "issuer": "mock-user-{}".format(scenario_id) if not user else user,
            "scenario_type": ScriptName.NOOP if not scenario_type else scenario_type,
            "status": ScenarioFsmStatus.CREATED,
            "labels": labels,
        }
    )
    scenario.start()
    return scenario


class TestGlobalLimit:
    def setup_scenarios(self, walle_test, mp, timestamp_patcher, limit, user=None):
        _mp_scenario_limit(mp, _GLOBAL_LIMIT_NAME, limit)

        scenarios = []
        for i in range(limit + 1):
            timestamp_patcher(i)
            scenarios.append(run_scenario(walle_test, user=user))

        return scenarios

    def test_global_permission_success(self, walle_test, mp, timestamp_patcher):
        scenarios = self.setup_scenarios(walle_test, mp, timestamp_patcher, limit=2)

        permission_stage = AcquirePermission()
        assert permission_stage.run(None, scenarios[0]) == Marker.success(message="Permission granted")
        assert permission_stage.run(None, scenarios[1]) == Marker.success(message="Permission granted")

    def test_global_permission_wait(self, walle_test, mp, timestamp_patcher):
        scenarios = self.setup_scenarios(walle_test, mp, timestamp_patcher, limit=1)

        permission_stage = AcquirePermission()
        assert permission_stage.run(None, scenarios[0]) == Marker.success(message="Permission granted")
        assert permission_stage.run(None, scenarios[1]) == Marker.in_progress(
            message="Too many started scenarios: queue position: 1"
        )


class TestUserLimit:
    def setup_scenarios(self, walle_test, mp, timestamp_patcher, limit, user=None):
        _mp_scenario_limit(mp, _USER_LIMIT_NAME, limit)

        scenarios = []
        for i in range(limit + 1):
            timestamp_patcher(i)
            scenarios.append(run_scenario(walle_test, user=user))

        return scenarios

    def test_user_permission_success(self, walle_test, mp, timestamp_patcher):
        scenarios = self.setup_scenarios(walle_test, mp, timestamp_patcher, limit=2, user="mock-user")

        permission_stage = AcquirePermission()
        assert permission_stage.run(None, scenarios[0]) == Marker.success(message="Permission granted")
        assert permission_stage.run(None, scenarios[1]) == Marker.success(message="Permission granted")

    def test_user_permission_wait(self, walle_test, mp, timestamp_patcher):
        scenarios = self.setup_scenarios(walle_test, mp, timestamp_patcher, limit=1, user="mock-user")

        permission_stage = AcquirePermission()
        assert permission_stage.run(None, scenarios[0]) == Marker.success(message="Permission granted")
        assert permission_stage.run(None, scenarios[1]) == Marker.in_progress(
            message="Too many scenarios are started by mock-user: queue position: 1"
        )


def test_check_scenario_type_if_custom_limit(walle_test, mp, timestamp_patcher):
    type_limit = 1
    custom_type_limit_name = "custom_limit"
    user = "mock-user"
    _mp_scenario_limit(mp, custom_type_limit_name, type_limit)

    scenarios_other_type = []
    for i in range(type_limit + 1):
        timestamp_patcher(i)
        scenarios_other_type.append(run_scenario(walle_test, user=user, scenario_type=ScriptName.NOC_SOFT))

    scenario = run_scenario(walle_test, user=user)

    permission_stage = AcquirePermission(custom_type_limit_name)
    assert permission_stage.run(None, scenario) == Marker.success(message="Permission granted")
    assert permission_stage.run(None, scenarios_other_type[0]) == Marker.success(message="Permission granted")
    assert permission_stage.run(None, scenarios_other_type[1]) == Marker.in_progress(
        message="Too many started scenarios of type {}: queue position: 1".format(ScriptName.NOC_SOFT)
    )


class TestPerDcLimit:
    dc_limit_name = "dc_limit"

    def setup_scenarios(self, walle_test, mp, timestamp_patcher, limit, user=None, labels=None):
        _mp_scenario_limit(mp, self.dc_limit_name, limit)

        scenarios = []
        for i in range(limit + 1):
            timestamp_patcher(i)
            scenarios.append(run_scenario(walle_test, user=user, labels=labels))

        return scenarios

    @classmethod
    def _mk_stage(cls, urgently=False):
        return AcquirePermission(dc_started_scenarios_limit=cls.dc_limit_name, urgently=urgently)

    def test_limit_wait(self, walle_test, mp, timestamp_patcher):
        scenarios = self.setup_scenarios(
            walle_test, mp, timestamp_patcher, limit=1, labels={DATACENTER_LABEL_NAME: "mock-dc-1"}
        )

        permission_stage = self._mk_stage()

        assert permission_stage.run(None, scenarios[0]) == Marker.success(message="Permission granted")
        assert permission_stage.run(None, scenarios[1]) == Marker.in_progress(
            message="Too many started scenarios of type noop in mock-dc-1: queue position: 1"
        )

    def test_limit_urgently(self, walle_test, mp, timestamp_patcher):
        scenarios = self.setup_scenarios(
            walle_test, mp, timestamp_patcher, limit=1, labels={DATACENTER_LABEL_NAME: "mock-dc-1"}
        )

        permission_stage = self._mk_stage(urgently=True)

        assert permission_stage.run(None, scenarios[0]) == Marker.success(message="Permission granted")
        assert permission_stage.run(None, scenarios[1]) == Marker.success(message="Permission granted")

    def test_success(self, walle_test, mp, timestamp_patcher):
        scenarios = self.setup_scenarios(
            walle_test, mp, timestamp_patcher, limit=2, labels={DATACENTER_LABEL_NAME: "mock-dc-1"}
        )
        permission_stage = self._mk_stage()

        assert permission_stage.run(None, scenarios[0]) == Marker.success(message="Permission granted")
        assert permission_stage.run(None, scenarios[1]) == Marker.success(message="Permission granted")

    def test_different_dc(self, walle_test, mp, timestamp_patcher):
        self.setup_scenarios(walle_test, mp, timestamp_patcher, limit=2, labels={DATACENTER_LABEL_NAME: "mock-dc-1"})
        permission_stage = self._mk_stage()

        other_dc_scenario = run_scenario(walle_test, labels={DATACENTER_LABEL_NAME: "mock-dc-2"})
        assert permission_stage.run(None, other_dc_scenario) == Marker.success(message="Permission granted")

    def test_no_label(self, walle_test, mp, timestamp_patcher):
        self.setup_scenarios(walle_test, mp, timestamp_patcher, limit=2, labels={DATACENTER_LABEL_NAME: "mock-dc-1"})
        permission_stage = self._mk_stage()

        no_dc_scenario = run_scenario(walle_test, labels=None)
        assert permission_stage.run(None, no_dc_scenario) == Marker.success(message="Permission granted")


class TestScenarioLimitsFromMaintenancePlots:
    @pytest.mark.usefixtures("monkeypatches_for_noop_scenario")
    def test_set_autoapprove_simple(self, walle_test):
        walle_test.mock_maintenance_plot(
            {
                "id": MOCK_MAINTENANCE_PLOT_ID_1,
                "common_settings": MaintenancePlotCommonSettings(
                    maintenance_approvers=MaintenanceApprovers(),
                    common_scenarios_settings=CommonScenarioSettings(total_number_of_active_hosts=5),
                ).to_dict(),
            }
        )

        project = walle_test.mock_project(dict(maintenance_plot_id=MOCK_MAINTENANCE_PLOT_ID_1, id="test-plot-options"))

        for inv in range(4):
            walle_test.mock_host(dict(inv=inv, project=project.id))
        for _id in range(2):
            walle_test.mock_scenario(
                {
                    "scenario_id": _id,
                    "name": "mock-{}".format(_id),
                    "scenario_type": ScriptName.ITDC_MAINTENANCE,
                    "hosts": [_id],
                    "labels": {WORK_STATUS_LABEL_NAME: ScenarioWorkStatus.FINISHING},
                    "status": ScenarioFsmStatus.STARTED,
                }
            )
            walle_test.mock_scenario(
                {
                    "scenario_id": 100 + _id,
                    "name": "mock-{}".format(100 + _id),
                    "scenario_type": ScriptName.ITDC_MAINTENANCE,
                    "hosts": [100 + _id],
                    "labels": {WORK_STATUS_LABEL_NAME: ScenarioWorkStatus.FINISHED},
                    "status": ScenarioFsmStatus.FINISHED,
                }
            )

        tested_scenario = walle_test.mock_scenario(
            {
                "scenario_id": 3,
                "name": "tested-scenario",
                "scenario_type": ScriptName.ITDC_MAINTENANCE,
                "hosts": list(range(2, 4)),
                "labels": {WORK_STATUS_LABEL_NAME: ScenarioWorkStatus.ACQUIRING_PERMISSION},
                "status": ScenarioFsmStatus.STARTED,
            }
        )
        data_storage = get_data_storage(tested_scenario)
        host_groups_sources = [
            HostGroupSource(
                0,
                MaintenancePlotHostGroupSource(maintenance_plot_id=MOCK_MAINTENANCE_PLOT_ID_1),
                ApprovementDecision(False, None),
            ),
        ]
        data_storage.write_host_groups_sources(host_groups_sources)

        stage_info = StageInfo()
        stage = AcquirePermission(with_check_limits_by_maintenance_plots=True)
        assert stage.run(stage_info, tested_scenario).status == MarkerStatus.SUCCESS

        data_storage = get_data_storage(tested_scenario)
        for host_group_source in data_storage.read_host_groups_sources():
            assert host_group_source.approvement_decision.skip_approvement is True
            reason = _REASON.format(MOCK_MAINTENANCE_PLOT_ID_1, 5, 2, 2, 4)
            assert host_group_source.approvement_decision.reason == reason

    @pytest.mark.usefixtures("monkeypatches_for_noop_scenario")
    def test_set_approve_blocked_by_limit(self, walle_test):
        walle_test.mock_maintenance_plot(
            {
                "id": MOCK_MAINTENANCE_PLOT_ID_1,
                "common_settings": MaintenancePlotCommonSettings(
                    maintenance_approvers=MaintenanceApprovers(),
                    common_scenarios_settings=CommonScenarioSettings(
                        total_number_of_active_hosts=5,
                        dont_allow_start_scenario_if_total_number_of_active_hosts_more_than=3,
                    ),
                ).to_dict(),
            }
        )

        project = walle_test.mock_project(dict(maintenance_plot_id=MOCK_MAINTENANCE_PLOT_ID_1, id="test-plot-options"))

        for inv in range(4):
            walle_test.mock_host(dict(inv=inv, project=project.id))
        for _id in range(2):
            walle_test.mock_scenario(
                {
                    "scenario_id": _id,
                    "name": "mock-{}".format(_id),
                    "scenario_type": ScriptName.ITDC_MAINTENANCE,
                    "hosts": [_id],
                    "labels": {WORK_STATUS_LABEL_NAME: ScenarioWorkStatus.FINISHING},
                    "status": ScenarioFsmStatus.STARTED,
                }
            )
            walle_test.mock_scenario(
                {
                    "scenario_id": 100 + _id,
                    "name": "mock-{}".format(100 + _id),
                    "scenario_type": ScriptName.ITDC_MAINTENANCE,
                    "hosts": [100 + _id],
                    "labels": {WORK_STATUS_LABEL_NAME: ScenarioWorkStatus.FINISHED},
                    "status": ScenarioFsmStatus.FINISHED,
                }
            )

        tested_scenario = walle_test.mock_scenario(
            {
                "scenario_id": 10,
                "name": "tested-scenario",
                "scenario_type": ScriptName.ITDC_MAINTENANCE,
                "hosts": list(range(2, 4)),
                "labels": {WORK_STATUS_LABEL_NAME: ScenarioWorkStatus.ACQUIRING_PERMISSION},
                "status": ScenarioFsmStatus.STARTED,
            }
        )
        data_storage = get_data_storage(tested_scenario)
        host_groups_sources = [
            HostGroupSource(
                0,
                MaintenancePlotHostGroupSource(maintenance_plot_id=MOCK_MAINTENANCE_PLOT_ID_1),
                ApprovementDecision(False, None),
            ),
        ]
        data_storage.write_host_groups_sources(host_groups_sources)

        stage_info = StageInfo()
        stage = AcquirePermission(with_check_limits_by_maintenance_plots=True)
        assert stage.run(stage_info, tested_scenario).status == MarkerStatus.IN_PROGRESS

    @pytest.mark.usefixtures("monkeypatches_for_noop_scenario")
    def test_set_autoapprove_by_for_few_maintenance_plots(self, walle_test):
        walle_test.mock_maintenance_plot(
            {
                "id": MOCK_MAINTENANCE_PLOT_ID_1,
                "common_settings": MaintenancePlotCommonSettings(
                    maintenance_approvers=MaintenanceApprovers(),
                    common_scenarios_settings=CommonScenarioSettings(
                        total_number_of_active_hosts=7,
                        dont_allow_start_scenario_if_total_number_of_active_hosts_more_than=10,
                    ),
                ).to_dict(),
            }
        )
        walle_test.mock_maintenance_plot(
            {
                "id": MOCK_MAINTENANCE_PLOT_ID_2,
                "common_settings": MaintenancePlotCommonSettings(
                    maintenance_approvers=MaintenanceApprovers(),
                    common_scenarios_settings=CommonScenarioSettings(
                        total_number_of_active_hosts=7,
                        dont_allow_start_scenario_if_total_number_of_active_hosts_more_than=10,
                    ),
                ).to_dict(),
            }
        )

        project_1 = walle_test.mock_project(
            dict(maintenance_plot_id=MOCK_MAINTENANCE_PLOT_ID_1, id="test-plot-options-1")
        )
        project_2 = walle_test.mock_project(
            dict(maintenance_plot_id=MOCK_MAINTENANCE_PLOT_ID_2, id="test-plot-options-2")
        )

        for inv in range(6):
            walle_test.mock_host(dict(inv=inv, project=project_1.id))

        for inv in range(6, 12):
            walle_test.mock_host(dict(inv=inv, project=project_2.id))

        for _id in list(range(3)) + list(range(6, 9)):
            walle_test.mock_scenario(
                {
                    "scenario_id": _id,
                    "name": "mock-{}".format(_id),
                    "scenario_type": ScriptName.ITDC_MAINTENANCE,
                    "hosts": [_id],
                    "labels": {WORK_STATUS_LABEL_NAME: ScenarioWorkStatus.FINISHING},
                    "status": ScenarioFsmStatus.STARTED,
                }
            )
            walle_test.mock_scenario(
                {
                    "scenario_id": 100 + _id,
                    "name": "mock-{}".format(100 + _id),
                    "scenario_type": ScriptName.ITDC_MAINTENANCE,
                    "hosts": [100 + _id],
                    "labels": {WORK_STATUS_LABEL_NAME: ScenarioWorkStatus.FINISHED},
                    "status": ScenarioFsmStatus.FINISHED,
                }
            )

        tested_scenario = walle_test.mock_scenario(
            {
                "scenario_id": 5,
                "name": "tested-scenario",
                "scenario_type": ScriptName.ITDC_MAINTENANCE,
                "hosts": list(range(3, 6)) + list(range(9, 12)),
                "labels": {WORK_STATUS_LABEL_NAME: ScenarioWorkStatus.ACQUIRING_PERMISSION},
                "status": ScenarioFsmStatus.STARTED,
            }
        )
        data_storage = get_data_storage(tested_scenario)
        host_groups_sources = [
            HostGroupSource(
                0,
                MaintenancePlotHostGroupSource(maintenance_plot_id=MOCK_MAINTENANCE_PLOT_ID_1),
                ApprovementDecision(False, None),
            ),
            HostGroupSource(
                1,
                MaintenancePlotHostGroupSource(maintenance_plot_id=MOCK_MAINTENANCE_PLOT_ID_2),
                ApprovementDecision(False, None),
            ),
        ]
        data_storage.write_host_groups_sources(host_groups_sources)

        stage_info = StageInfo()
        stage = AcquirePermission(with_check_limits_by_maintenance_plots=True)
        assert stage.run(stage_info, tested_scenario).status == MarkerStatus.SUCCESS

        data_storage = get_data_storage(tested_scenario)
        for host_group_source in data_storage.read_host_groups_sources():
            assert host_group_source.approvement_decision.skip_approvement is True
            reason = _REASON.format(host_group_source.source.maintenance_plot_id, 7, 3, 3, 6)
            assert host_group_source.approvement_decision.reason == reason

    @pytest.mark.usefixtures("monkeypatches_for_noop_scenario")
    def test_set_autoapprove_limit_exceeded(self, walle_test):
        walle_test.mock_maintenance_plot(
            {
                "id": MOCK_MAINTENANCE_PLOT_ID_1,
                "common_settings": MaintenancePlotCommonSettings(
                    maintenance_approvers=MaintenanceApprovers(),
                    common_scenarios_settings=CommonScenarioSettings(total_number_of_active_hosts=4),
                ).to_dict(),
            }
        )

        project = walle_test.mock_project(dict(maintenance_plot_id=MOCK_MAINTENANCE_PLOT_ID_1, id="test-plot-options"))

        for inv in range(4):
            walle_test.mock_host(dict(inv=inv, project=project.id))
        for _id in range(2):
            walle_test.mock_scenario(
                {
                    "scenario_id": _id,
                    "name": "mock-{}".format(_id),
                    "scenario_type": ScriptName.ITDC_MAINTENANCE,
                    "hosts": [_id],
                    "labels": {WORK_STATUS_LABEL_NAME: ScenarioWorkStatus.FINISHING},
                    "status": ScenarioFsmStatus.STARTED,
                }
            )

        tested_scenario = walle_test.mock_scenario(
            {
                "scenario_id": 3,
                "name": "tested-scenario",
                "scenario_type": ScriptName.ITDC_MAINTENANCE,
                "hosts": list(range(2, 4)),
                "labels": {WORK_STATUS_LABEL_NAME: ScenarioWorkStatus.ACQUIRING_PERMISSION},
                "status": ScenarioFsmStatus.STARTED,
            }
        )
        data_storage = get_data_storage(tested_scenario)
        host_groups_sources = [
            HostGroupSource(
                0,
                MaintenancePlotHostGroupSource(maintenance_plot_id=MOCK_MAINTENANCE_PLOT_ID_1),
                ApprovementDecision(False, None),
            ),
        ]
        data_storage.write_host_groups_sources(host_groups_sources)

        stage_info = StageInfo()
        stage = AcquirePermission(with_check_limits_by_maintenance_plots=True)
        assert stage.run(stage_info, tested_scenario).status == MarkerStatus.SUCCESS

        data_storage = get_data_storage(tested_scenario)
        for host_group_source in data_storage.read_host_groups_sources():
            assert host_group_source.approvement_decision.skip_approvement is False
            reason = _REASON.format(MOCK_MAINTENANCE_PLOT_ID_1, 4, 2, 2, 4)
            assert host_group_source.approvement_decision.reason == reason
