"""Test scheduler stage."""
from unittest.mock import ANY

import pytest

from infra.walle.server.tests.lib.util import monkeypatch_locks, monkeypatch_method, mock_task
from infra.walle.server.tests.scenario.utils import (
    mock_scenario,
    make_mock_host_stage,
    get_mock_host_stage,
    make_mock_stage,
)
from sepelib.core.exceptions import LogicalError
from sepelib.mongo.mock import ObjectMocker
from walle.models import timestamp
from walle.scenario.constants import HostScenarioStatus, SchedulerName
from walle.scenario.host_stage_info import HostStageInfo
from walle.scenario.marker import MarkerStatus
from walle.scenario.scenario import ScenarioHostState, Scenario
from walle.scenario.stage.scheduler_stage import (
    HostSchedulerStage,
    GroupScheduler,
    AcquireHosts,
    ProcessHostGroup,
    host_scheduler_stage,
    noc_maintenance_scheduler_stage,
    HOST_IS_ASSIGNED_READY,
)
from walle.scenario.stage_info import StageAction
from walle.scenario.stages import ScenarioRootStage, HostRootStage


@pytest.fixture(params=(True, False))
def greedy(request):
    return request.param


class EvenVsOddScheduler:
    @staticmethod
    def schedule(hosts_info):
        for host_info in hosts_info.values():
            host_info.group = host_info.inv % 2
        return hosts_info


def mock_n_hosts(walle_test, n, **overrides):
    return {inv: walle_test.mock_host(overrides=dict({"inv": inv}, **overrides)) for inv in range(n)}


def mock_scheduled_scenario(script, hosts_info, **kwargs):
    stage_info = script.serialize()
    hosts_info = EvenVsOddScheduler.schedule(hosts_info)  # [[0, 2, 4], [1, 3]]
    return mock_scenario(stage_info=stage_info, hosts=hosts_info, **kwargs)


def mock_host_info(hosts):
    hosts_info = {
        host.uuid: ScenarioHostState(inv=host.inv, status=HostScenarioStatus.QUEUE, timestamp=timestamp())
        for host in hosts.values()
    }

    # [[0, 2, 4], [1, 3]]
    return EvenVsOddScheduler.schedule(hosts_info)


def even(i):
    return i % 2 == 0


def odd(i):
    return i % 2 == 1


class TestSchedulerStage:
    def make_mock_script(self):
        return ScenarioRootStage([host_scheduler_stage(HostRootStage([make_mock_host_stage()]))])

    def setup_scheduler_stage_info(self, scenario, action_type=StageAction.ACTION):
        scheduler_stage_info = scenario.stage_info.stages[0]
        scheduler_stage_info.action_type = action_type
        return scheduler_stage_info

    @staticmethod
    def assert_greedy_action_switch(scenario, previous_action, next_action):
        # non-greedy: same action
        # greedy: next action
        scheduler_stage_info = scenario.stage_info.stages[0]
        expected_action = next_action if scheduler_stage_info.params.get("greedy", False) else previous_action
        assert scheduler_stage_info.action_type == expected_action

    @pytest.mark.usefixtures("monkeypatch_locks")
    def test_transition_to_action_when_next_group_available(self, walle_test):
        """The shage should return from `check` to `action` for next group untill all groups processed."""

        script = self.make_mock_script()
        stage_info = script.serialize()
        scenario = mock_scenario(current_group=0, stage_info=stage_info)
        hosts = [walle_test.mock_host({"inv": i, "scenario_id": scenario.scenario_id}) for i in range(2)]
        hosts_info = {
            hosts[0].uuid: ScenarioHostState(inv=0, status=HostScenarioStatus.DONE, timestamp=timestamp(), group=0),
            hosts[1].uuid: ScenarioHostState(inv=1, status=HostScenarioStatus.QUEUE, timestamp=timestamp(), group=1),
        }
        scenario.hosts = hosts_info
        scheduler_stage = script.children[0]
        scheduler_stage_info = self.setup_scheduler_stage_info(scenario, StageAction.CHECK)

        result_marker = scheduler_stage.run(scheduler_stage_info, scenario)
        assert result_marker.status == MarkerStatus.IN_PROGRESS
        assert scheduler_stage_info.action_type == StageAction.ACTION

    def test_stage_finishes_when_last_group_processed(self, walle_test):
        script = self.make_mock_script()
        stage_info = script.serialize()
        scenario = mock_scenario(current_group=1, stage_info=stage_info)
        hosts = [walle_test.mock_host({"inv": i, "scenario_id": scenario.scenario_id}) for i in range(2)]
        hosts_info = {
            hosts[0].uuid: ScenarioHostState(inv=0, status=HostScenarioStatus.DONE, timestamp=timestamp(), group=0),
            hosts[1].uuid: ScenarioHostState(inv=1, status=HostScenarioStatus.DONE, timestamp=timestamp(), group=1),
        }
        scenario.hosts = hosts_info
        scheduler_stage = script.children[0]
        scheduler_stage_info = self.setup_scheduler_stage_info(scenario, StageAction.CHECK)

        result_marker = scheduler_stage.run(scheduler_stage_info, scenario)

        assert result_marker.status == MarkerStatus.SUCCESS

    def test_prepare_stage(self, walle_test):
        script = self.make_mock_script()
        stage_info = script.serialize()
        hosts_info = {
            host.uuid: ScenarioHostState(inv=host.inv, status=HostScenarioStatus.QUEUE, timestamp=timestamp())
            for host in [walle_test.mock_host(overrides={"inv": i}) for i in range(2)]
        }
        scenario = mock_scenario(stage_info=stage_info, hosts=hosts_info)
        scheduler_stage = script.children[0]
        scheduler_stage_info = self.setup_scheduler_stage_info(scenario, StageAction.PREPARE)

        assert scenario.current_group is None
        for host in scenario.hosts.values():
            assert host.group is None

        assert scheduler_stage.run(scheduler_stage_info, scenario).status == MarkerStatus.IN_PROGRESS
        assert scenario.current_group == 0
        for host in scenario.hosts.values():
            assert host.group is not None

    @staticmethod
    def _mock_scheduled_scenario(script, hosts_info, **kwargs):
        stage_info = script.serialize()
        hosts_info = EvenVsOddScheduler.schedule(hosts_info)  # [[0, 2, 4], [1, 3]]
        return mock_scenario(stage_info=stage_info, hosts=hosts_info, **kwargs)

    @staticmethod
    def mock_n_hosts(walle_test, n):
        return {inv: walle_test.mock_host(overrides={"inv": inv}) for inv in range(n)}

    @staticmethod
    def mock_host_info(hosts):
        hosts_info = {
            host.uuid: ScenarioHostState(inv=host.inv, status=HostScenarioStatus.QUEUE, timestamp=timestamp())
            for host in hosts.values()
        }
        # [[0, 2, 4], [1, 3]]
        return EvenVsOddScheduler.schedule(hosts_info)

    @staticmethod
    def _scheduler_stage(script):
        return script.children[0]

    def _host_root_stage_info(self, script):
        return self._scheduler_stage(script).host_root_stage.serialize(uid="0.0.0")

    def _execute_scheduler_stage(self, script, scenario, action_type):
        scheduler_stage = self._scheduler_stage(script)
        scheduler_stage_info = self.setup_scheduler_stage_info(scenario, action_type)

        return scheduler_stage.run(scheduler_stage_info, scenario)

    def test_stage_info_cleanup_on_group_finished(self, walle_test):
        current_group_idx = 0
        scenario_id = 1

        hsi = ObjectMocker(HostStageInfo)
        hsis = []
        hosts = []
        for i in range(5):
            host = walle_test.mock_host({"inv": i, "scenario_id": scenario_id})
            hosts.append(host)
            hsis.append(
                hsi.mock(
                    dict(
                        host_uuid=host.uuid,
                        scenario_id=scenario_id,
                        stage_info=HostRootStage([make_mock_host_stage()]).serialize(),
                    )
                )
            )

        hosts_info = {
            host.uuid: ScenarioHostState(
                inv=host.inv, status=HostScenarioStatus.DONE, timestamp=timestamp(), group=current_group_idx
            )
            for host in hosts
        }
        script = self.make_mock_script()
        scenario = mock_scenario(
            scenario_id=scenario_id, current_group=current_group_idx, stage_info=script.serialize(), hosts=hosts_info
        )
        scheduler_stage = script.children[0]
        scheduler_stage_info = self.setup_scheduler_stage_info(scenario, StageAction.CHECK)

        assert scheduler_stage.run(scheduler_stage_info, scenario).status == MarkerStatus.SUCCESS

        for _hsi in hsis:
            hsi.remove(_hsi)

        walle_test.hosts.assert_equal()
        hsi.assert_equal()

    def test_check_switch_while_hms_in_progress(self, walle_test, mp):
        monkeypatch_locks(mp)
        current_group_idx = 0
        scenario_id = 1

        host = walle_test.mock_host({"task": mock_task(), "scenario_id": scenario_id})
        hosts_info = {
            host.uuid: ScenarioHostState(
                inv=host.inv, status=HostScenarioStatus.PROCESSING, timestamp=timestamp(), group=current_group_idx
            )
        }
        script = self.make_mock_script()
        scenario = mock_scenario(
            scenario_id=scenario_id, current_group=current_group_idx, stage_info=script.serialize(), hosts=hosts_info
        )

        HostStageInfo(
            host_uuid=host.uuid, scenario_id=scenario.scenario_id, stage_info=self._host_root_stage_info(script)
        ).save()

        scheduler_stage = script.children[0]
        scheduler_stage_info = self.setup_scheduler_stage_info(scenario, action_type=StageAction.CHECK)

        scheduler_stage.run(scheduler_stage_info, scenario)

        self.assert_greedy_action_switch(scenario, StageAction.CHECK, StageAction.ACTION)

        assert get_mock_host_stage().run.called


class TestGreedyHostScheduler(TestSchedulerStage):
    def make_mock_script(self):
        return ScenarioRootStage([host_scheduler_stage([HostRootStage([make_mock_host_stage()])], greedy=True)])

    def setup_scheduler_stage_info(self, scenario, action_type=StageAction.ACTION):
        scheduler_stage_info = scenario.stage_info.stages[0]
        scheduler_stage_info.action_type = action_type
        return scheduler_stage_info

    def test_action_check_loop(self, walle_test, mp):
        """Greedy scheduler stage will loop between action and check untill all hosts finished."""
        monkeypatch_locks(mp)

        current_group_idx = 0
        scenario_id = 1
        hosts = [walle_test.mock_host({"inv": i, "scenario_id": scenario_id}) for i in range(5)]
        hosts_info = {
            host.uuid: ScenarioHostState(
                inv=host.inv, status=HostScenarioStatus.PROCESSING, timestamp=timestamp(), group=current_group_idx
            )
            for host in hosts
        }
        script = self.make_mock_script()
        scenario = mock_scenario(
            scenario_id=scenario_id, current_group=current_group_idx, stage_info=script.serialize(), hosts=hosts_info
        )
        scheduler_stage = script.children[0]
        scheduler_stage_info = self.setup_scheduler_stage_info(scenario)
        monkeypatch_method(mp, ProcessHostGroup._execute_host_stages, ProcessHostGroup)

        scheduler_stage.run(scheduler_stage_info, scenario)
        self.assert_greedy_action_switch(scenario, StageAction.ACTION, StageAction.CHECK)

        scheduler_stage.run(scheduler_stage_info, scenario)
        self.assert_greedy_action_switch(scenario, StageAction.CHECK, StageAction.ACTION)


class TestTimedHostSchedulerStage(TestSchedulerStage):
    def make_mock_script(self, greedy=False):
        return ScenarioRootStage(
            [
                host_scheduler_stage(
                    [HostRootStage([make_mock_host_stage()])],
                    execution_time=10000,
                    greedy=greedy,
                )
            ]
        )

    def setup_scheduler_stage_info(self, scenario, action_type=StageAction.ACTION):
        scheduler_stage_info = scenario.stage_info.stages[0]
        scheduler_stage_info.action_type = action_type
        scheduler_stage_info.data = {HostSchedulerStage.STAGE_END_TIME: timestamp() + 10000}
        return scheduler_stage_info

    def test_prepare_sets_initial_time(self, walle_test, monkeypatch_timestamp):
        script = self.make_mock_script()
        scenario = mock_scenario(stage_info=script.serialize())
        scheduler_stage = script.children[0]
        scheduler_stage_info = scenario.stage_info.stages[0]

        assert scheduler_stage_info.data == {}
        assert scheduler_stage.prepare(scheduler_stage_info, scenario).status == MarkerStatus.SUCCESS
        assert scheduler_stage_info.data == {
            HostSchedulerStage.STAGE_END_TIME: timestamp() + scheduler_stage.execution_time
        }

    def test_stage_success_on_timeout(self, walle_test):
        """Current behaviour: stage finishes on ttl"""

        script = self.make_mock_script()
        scenario = mock_scenario(stage_info=script.serialize())
        scheduler_stage = script.children[0]
        scheduler_stage_info = self.setup_scheduler_stage_info(scenario)
        scheduler_stage_info.data[HostSchedulerStage.STAGE_END_TIME] = timestamp() - 10

        assert scheduler_stage.run(scheduler_stage_info, scenario).status == MarkerStatus.SUCCESS


class TestAcquireHostsAction:
    @staticmethod
    def greedy_marker_status(greedy):
        return MarkerStatus.SUCCESS if greedy else MarkerStatus.IN_PROGRESS

    @staticmethod
    def make_mock_script(greedy=False):
        return ScenarioRootStage(
            [
                host_scheduler_stage(
                    [HostRootStage([make_mock_host_stage()])],
                    greedy=greedy,
                )
            ]
        )

    @staticmethod
    def mock_scheduled_scenario(test, script, hosts_info, **kwargs):
        scenario = mock_scheduled_scenario(script, hosts_info=hosts_info, **kwargs)

        scenario.save()
        test.scenarios.add(scenario)

        return scenario

    @staticmethod
    def _get_scheduler_stage(script):
        return script.children[0]

    @staticmethod
    def _get_scheduler_stage_info(script):
        return script.serialize().stages[0]

    @classmethod
    def make_host_root_stage_info(cls, script):
        return cls._get_scheduler_stage(script).host_root_stage.serialize(uid="0.0.0")

    def mock_acquire_host(self, walle_test, host, scenario, script, stage_info_data=None, save=False):
        stage_info = self.make_host_root_stage_info(script)
        if stage_info_data is not None:
            stage_info.data = stage_info_data

        host.scenario_id = scenario.scenario_id
        scenario.hosts[host.uuid].status = HostScenarioStatus.ACQUIRED
        scenario.hosts[host.uuid].is_acquired = True

        walle_test.hosts_stage_info.mock(
            {
                "host_uuid": host.uuid,
                "scenario_id": scenario.scenario_id,
                "stage_info": stage_info,
            },
            save=save,
        )

        if save:
            scenario.save()
            host.save()

    @classmethod
    def execute_acquire_hosts_action(cls, script, scenario, greedy, host_readiness_str=None):
        scheduler_stage = cls._get_scheduler_stage(script)
        db_scenario = Scenario.objects.get(scenario_id=scenario.scenario_id)

        acquire_hosts_action = AcquireHosts(
            greedy,
            scheduler_stage.host_root_stage,
            scheduler_stage.group_scheduler,
            host_readiness_str=host_readiness_str,
        )

        try:
            return acquire_hosts_action(cls._get_scheduler_stage_info(script), db_scenario)
        finally:
            db_scenario.save()

    def test_available_hosts_acquired_immediately(self, walle_test, greedy):
        """Hosts that can be acquired should be acquired.
        Any host can be acquired, except for already acquired hosts, see next tests.
        """
        script = self.make_mock_script(greedy=greedy)

        hosts = mock_n_hosts(walle_test, 5)
        hosts_info = mock_host_info(hosts)

        scenario = self.mock_scheduled_scenario(walle_test, script, hosts_info=hosts_info, current_group=1)

        result_marker = self.execute_acquire_hosts_action(script, scenario, greedy)

        for inv in filter(odd, range(5)):
            self.mock_acquire_host(walle_test, hosts[inv], scenario, script)

        assert result_marker.status == MarkerStatus.SUCCESS
        walle_test.hosts.assert_equal()
        walle_test.hosts_stage_info.assert_equal()
        walle_test.scenarios.assert_equal()

    def test_busy_hosts_not_acquired(self, walle_test, greedy):
        """Hosts that acquired by another scenario should not be acquired"""
        script = self.make_mock_script(greedy=greedy)

        hosts = mock_n_hosts(walle_test, 5, scenario_id=1)
        hosts_info = mock_host_info(hosts)

        scenario = self.mock_scheduled_scenario(
            walle_test, script, scenario_id=2, hosts_info=hosts_info, current_group=1
        )

        result_marker = self.execute_acquire_hosts_action(script, scenario, greedy)

        assert result_marker.status == self.greedy_marker_status(greedy)
        walle_test.hosts.assert_equal()
        walle_test.hosts_stage_info.assert_equal()
        walle_test.scenarios.assert_equal()

    def test_scheduled_hosts_not_acquired_repeatedly(self, walle_test, greedy):
        """Hosts that already acquired should not be acquired again.
        Repeated acquire can corrupt host state."""

        script = self.make_mock_script(greedy=greedy)

        hosts = mock_n_hosts(walle_test, 5, scenario_id=2)
        hosts_info = mock_host_info(hosts)

        scenario = self.mock_scheduled_scenario(
            walle_test, script, scenario_id=2, hosts_info=hosts_info, current_group=1
        )

        for inv in filter(odd, range(5)):
            host = hosts[inv]
            self.mock_acquire_host(
                walle_test, host, scenario, script, stage_info_data={"mock_data": "mock_value"}, save=True
            )

        result_marker = self.execute_acquire_hosts_action(script, scenario, greedy)

        assert result_marker.status == MarkerStatus.SUCCESS  # success because all hosts acquired
        walle_test.hosts.assert_equal()
        walle_test.hosts_stage_info.assert_equal()
        walle_test.scenarios.assert_equal()

    def test_host_acquire_should_be_transactional(self, mp, walle_test, greedy):
        """Host can be changed between read and write. That should not result in data corruption."""

        script = self.make_mock_script(greedy=greedy)

        hosts = mock_n_hosts(walle_test, 5, scenario_id=2)
        hosts_info = mock_host_info(hosts)

        scenario = self.mock_scheduled_scenario(
            walle_test, script, scenario_id=2, hosts_info=hosts_info, current_group=1
        )

        get_current_hosts_group_orig = AcquireHosts._get_current_hosts_group

        def mock_acquire_hosts_by_another_scenario(self, *args, **kwargs):
            res = get_current_hosts_group_orig(self, *args, **kwargs)
            for inv in filter(odd, range(5)):
                host = hosts[inv]
                host.scenario_id = 3
                host.save()

            return res

        mp.method(
            AcquireHosts._get_current_hosts_group, side_effect=mock_acquire_hosts_by_another_scenario, obj=AcquireHosts
        )
        result_marker = self.execute_acquire_hosts_action(script, scenario, greedy)

        assert result_marker.status == self.greedy_marker_status(greedy)
        walle_test.hosts.assert_equal()
        walle_test.hosts_stage_info.assert_equal()
        walle_test.scenarios.assert_equal()


class TestProcessHostGroup:
    @pytest.fixture(autouse=True)
    def stub_locks(self, mp):
        monkeypatch_locks(mp)

    @staticmethod
    def greedy_marker_status(greedy):
        return MarkerStatus.SUCCESS if greedy else MarkerStatus.IN_PROGRESS

    @staticmethod
    def make_mock_script(greedy=False, host_stage_status=MarkerStatus.SUCCESS):
        return ScenarioRootStage(
            [
                host_scheduler_stage(
                    [HostRootStage([make_mock_host_stage(marker_status=host_stage_status)])],
                    greedy=greedy,
                )
            ]
        )

    @staticmethod
    def _get_scheduler_stage(script):
        return script.children[0]

    @staticmethod
    def _get_scheduler_stage_info(script):
        return script.serialize().stages[0]

    @classmethod
    def make_host_root_stage_info(cls, script):
        return cls._get_scheduler_stage(script).host_root_stage.serialize(uid="0.0.0")

    @classmethod
    def acquire_hosts(cls, hosts, scenario, script):
        for host in hosts:
            hsi = HostStageInfo(
                host_uuid=host.uuid, scenario_id=scenario.scenario_id, stage_info=cls.make_host_root_stage_info(script)
            )
            hsi.save()

            host.scenario_id = scenario.scenario_id
            host.save()

    @classmethod
    def execute_process_host_group_action(cls, script, scenario, greedy=None):
        scheduler_stage = cls._get_scheduler_stage(script)
        process_host_group_action = ProcessHostGroup(greedy, scheduler_stage.group_scheduler)
        return process_host_group_action(cls._get_scheduler_stage_info(script), scenario)

    @pytest.mark.parametrize("current_group", [0, 1])
    def test_finished_when_group_finished(self, walle_test, greedy, current_group):
        script = self.make_mock_script(greedy)
        hosts = [walle_test.mock_host({"inv": i, "scenario_id": 1}) for i in range(3)]
        hosts_info = {
            hosts[0].uuid: ScenarioHostState(inv=0, status=HostScenarioStatus.DONE, timestamp=timestamp(), group=0),
            hosts[1].uuid: ScenarioHostState(inv=1, status=HostScenarioStatus.DONE, timestamp=timestamp(), group=0),
            hosts[2].uuid: ScenarioHostState(inv=2, status=HostScenarioStatus.DONE, timestamp=timestamp(), group=1),
        }

        scenario = mock_scenario(
            scenario_id=1, current_group=current_group, stage_info=script.serialize(), hosts=hosts_info
        )

        self.acquire_hosts(hosts, scenario, script)

        result_marker = self.execute_process_host_group_action(script, scenario, greedy)
        assert result_marker.status == MarkerStatus.SUCCESS

    @pytest.mark.parametrize("host_processing_status", [HostScenarioStatus.PROCESSING, HostScenarioStatus.QUEUE])
    def test_in_progress_when_group_not_finished(self, walle_test, greedy, host_processing_status):
        script = self.make_mock_script(greedy)
        hosts = [walle_test.mock_host({"inv": i, "scenario_id": 1}) for i in range(3)]

        hosts_info = {
            hosts[0].uuid: ScenarioHostState(inv=0, status=HostScenarioStatus.DONE, timestamp=timestamp(), group=0),
            hosts[1].uuid: ScenarioHostState(inv=1, status=host_processing_status, timestamp=timestamp(), group=1),
            hosts[2].uuid: ScenarioHostState(inv=2, status=HostScenarioStatus.DONE, timestamp=timestamp(), group=1),
        }

        scenario = mock_scenario(scenario_id=1, current_group=1, stage_info=script.serialize(), hosts=hosts_info)

        self.acquire_hosts(hosts, scenario, script)

        result_marker = self.execute_process_host_group_action(script, scenario, greedy)
        assert result_marker.status == self.greedy_marker_status(greedy)

    @pytest.mark.parametrize("host_processing_status", [HostScenarioStatus.PROCESSING, HostScenarioStatus.QUEUE])
    def test_executes_host_root_stage_for_every_processing_host(self, walle_test, greedy, host_processing_status):
        script = self.make_mock_script(greedy)
        hosts = [walle_test.mock_host({"inv": i, "scenario_id": 1}) for i in range(2)]

        hosts_info = {
            hosts[0].uuid: ScenarioHostState(inv=0, status=HostScenarioStatus.DONE, timestamp=timestamp(), group=1),
            hosts[1].uuid: ScenarioHostState(inv=1, status=host_processing_status, timestamp=timestamp(), group=1),
        }

        scenario = mock_scenario(scenario_id=1, current_group=1, stage_info=script.serialize(), hosts=hosts_info)
        self.acquire_hosts(hosts, scenario, script)

        result_marker = self.execute_process_host_group_action(script, scenario, greedy)

        host_stage = get_mock_host_stage()
        # stage_info changes during the call. the stage asserts correctness of stage_info
        host_stage.run.assert_called_once_with(ANY, scenario, hosts[1], ANY)
        assert result_marker.status == self.greedy_marker_status(greedy)  # does not depend on host stage result

    @pytest.mark.parametrize("host_processing_status", [HostScenarioStatus.PROCESSING, HostScenarioStatus.QUEUE])
    def test_changes_host_processing_status_to_processing_when_processing(self, walle_test, host_processing_status):
        script = self.make_mock_script(host_stage_status=MarkerStatus.IN_PROGRESS)
        hosts = [walle_test.mock_host({"inv": i, "scenario_id": 1}) for i in range(3)]

        hosts_info = {
            hosts[0].uuid: ScenarioHostState(inv=0, status=host_processing_status, timestamp=timestamp(), group=1),
            hosts[1].uuid: ScenarioHostState(inv=1, status=HostScenarioStatus.DONE, timestamp=timestamp(), group=1),
            hosts[2].uuid: ScenarioHostState(inv=2, status=HostScenarioStatus.QUEUE, timestamp=timestamp(), group=2),
        }

        scenario = mock_scenario(scenario_id=1, current_group=1, stage_info=script.serialize(), hosts=hosts_info)
        scenario.save()

        self.acquire_hosts(hosts, scenario, script)

        self.execute_process_host_group_action(script, scenario)

        assert scenario.hosts[hosts[0].uuid].status == HostScenarioStatus.PROCESSING

    @pytest.mark.parametrize("host_processing_status", [HostScenarioStatus.PROCESSING, HostScenarioStatus.QUEUE])
    def test_changes_host_processing_status_to_done_when_host_finished(self, walle_test, host_processing_status):
        script = self.make_mock_script(host_stage_status=MarkerStatus.SUCCESS)
        hosts = [walle_test.mock_host({"inv": i, "scenario_id": 1}) for i in range(3)]

        hosts_info = {
            hosts[0].uuid: ScenarioHostState(inv=0, status=host_processing_status, timestamp=timestamp(), group=1),
            hosts[1].uuid: ScenarioHostState(inv=1, status=HostScenarioStatus.DONE, timestamp=timestamp(), group=1),
            hosts[2].uuid: ScenarioHostState(inv=2, status=HostScenarioStatus.QUEUE, timestamp=timestamp(), group=2),
        }

        scenario = mock_scenario(scenario_id=1, current_group=1, stage_info=script.serialize(), hosts=hosts_info)
        scenario.save()

        self.acquire_hosts(hosts, scenario, script)

        self.execute_process_host_group_action(script, scenario)

        assert scenario.hosts[hosts[0].uuid].status == HostScenarioStatus.DONE

    def test_does_not_process_hosts_from_other_groups(self, walle_test):
        script = self.make_mock_script()
        hosts = [walle_test.mock_host({"inv": i, "scenario_id": 1}) for i in range(3)]

        hosts_info = {
            hosts[0].uuid: ScenarioHostState(
                inv=0, status=HostScenarioStatus.PROCESSING, timestamp=timestamp(), group=1
            ),
            hosts[1].uuid: ScenarioHostState(inv=1, status=HostScenarioStatus.DONE, timestamp=timestamp(), group=1),
            hosts[2].uuid: ScenarioHostState(inv=2, status=HostScenarioStatus.QUEUE, timestamp=timestamp(), group=2),
        }

        scenario = mock_scenario(scenario_id=1, current_group=0, stage_info=script.serialize(), hosts=hosts_info)
        scenario.save()
        self.acquire_hosts(hosts, scenario, script)

        self.execute_process_host_group_action(script, scenario)

        host_stage = get_mock_host_stage()
        # stage_info changes during the call. the stage asserts correctness of stage_info
        assert not host_stage.run.called
        assert scenario.hosts[hosts[0].uuid].status == HostScenarioStatus.PROCESSING
        assert scenario.hosts[hosts[1].uuid].status == HostScenarioStatus.DONE
        assert scenario.hosts[hosts[2].uuid].status == HostScenarioStatus.QUEUE

    def test_does_not_process_finished_hosts(self, walle_test):
        script = self.make_mock_script()
        host = walle_test.mock_host({"inv": 1, "scenario_id": 1})

        hosts_info = {
            host.uuid: ScenarioHostState(inv=1, status=HostScenarioStatus.DONE, timestamp=timestamp(), group=1),
        }

        scenario = mock_scenario(scenario_id=1, current_group=1, stage_info=script.serialize(), hosts=hosts_info)
        scenario.save()
        self.acquire_hosts([host], scenario, script)

        self.execute_process_host_group_action(script, scenario)

        host_stage = get_mock_host_stage()
        # stage_info changes during the call. the stage asserts correctness of stage_info
        assert not host_stage.run.called
        assert scenario.hosts[host.uuid].status == HostScenarioStatus.DONE


class TestGroupScheduler:
    @staticmethod
    def make_mock_script():
        return ScenarioRootStage(
            [
                host_scheduler_stage(
                    [HostRootStage([make_mock_host_stage()])],
                )
            ]
        )

    @pytest.mark.parametrize("current_group,expected_availability", ((0, True), (1, True), (2, False)))
    def test_group_available(self, current_group, expected_availability):
        hosts_info = {
            "0": ScenarioHostState(inv=0, status=HostScenarioStatus.DONE, timestamp=timestamp(), group=0),
            "1": ScenarioHostState(inv=1, status=HostScenarioStatus.QUEUE, timestamp=timestamp(), group=1),
        }
        script = self.make_mock_script()
        scenario = mock_scenario(stage_info=script.serialize(), hosts=hosts_info, current_group=current_group)
        assert GroupScheduler(None).is_group_available(scenario) == expected_availability

    @pytest.mark.parametrize("host_processing_status", [HostScenarioStatus.PROCESSING, HostScenarioStatus.QUEUE])
    def test_hosts_group_returns_only_processing_hosts(self, walle_test, host_processing_status):
        hosts = mock_n_hosts(walle_test, 2)
        current_group_idx = 1
        hosts_info = {
            hosts[0].uuid: ScenarioHostState(
                inv=0, status=HostScenarioStatus.DONE, timestamp=timestamp(), group=current_group_idx
            ),
            hosts[1].uuid: ScenarioHostState(
                inv=1, status=host_processing_status, timestamp=timestamp(), group=current_group_idx
            ),
        }
        scenario = mock_scenario(current_group=current_group_idx, hosts=hosts_info)

        current_group = GroupScheduler(None).get_current_hosts_group(scenario.hosts, scenario.current_group)
        assert [host.inv for host in current_group] == [1]

    @pytest.mark.parametrize("host_processing_status", [HostScenarioStatus.PROCESSING, HostScenarioStatus.QUEUE])
    def test_hosts_group_returns_only_hosts_from_current_group(self, walle_test, host_processing_status):
        hosts = mock_n_hosts(walle_test, 2)
        current_group_idx = 1
        hosts_info = {
            hosts[0].uuid: ScenarioHostState(
                inv=0, group=current_group_idx + 1, status=host_processing_status, timestamp=timestamp()
            ),
            hosts[1].uuid: ScenarioHostState(
                inv=1, group=current_group_idx, status=host_processing_status, timestamp=timestamp()
            ),
        }
        scenario = mock_scenario(current_group=current_group_idx, hosts=hosts_info)

        current_group = GroupScheduler(None).get_current_hosts_group(scenario.hosts, scenario.current_group)
        assert [host.inv for host in current_group] == [1]

    def test_hosts_group_returns_empty_list_when_all_hosts_done(self, walle_test):
        hosts = mock_n_hosts(walle_test, 2)
        current_group_idx = 1
        hosts_info = {
            hosts[0].uuid: ScenarioHostState(
                inv=0, group=current_group_idx, status=HostScenarioStatus.DONE, timestamp=timestamp()
            ),
        }
        scenario = mock_scenario(current_group=current_group_idx, hosts=hosts_info)

        current_group = GroupScheduler(None).get_current_hosts_group(scenario.hosts, scenario.current_group)
        assert [host.inv for host in current_group] == []

    def test_hosts_group_returns_empty_list_when_no_hosts_in_group(self, walle_test):
        hosts = mock_n_hosts(walle_test, 2)
        current_group_idx = 1
        hosts_info = {
            hosts[0].uuid: ScenarioHostState(
                inv=0, group=current_group_idx + 1, status=HostScenarioStatus.DONE, timestamp=timestamp()
            ),
        }
        scenario = mock_scenario(current_group=current_group_idx, hosts=hosts_info)

        current_group = GroupScheduler(None).get_current_hosts_group(scenario.hosts, scenario.current_group)
        assert [host.inv for host in current_group] == []


class TestStaticConstructors:
    def test_host_scheduler_stage(self):
        stage = host_scheduler_stage([HostRootStage([make_mock_host_stage()])])
        stage_info = stage.serialize("0")

        deserialized_stage = stage_info.deserialize()
        assert stage == deserialized_stage

    def test_host_scheduler_stage_defaults(self):
        stage = host_scheduler_stage([HostRootStage([make_mock_host_stage()])])

        stage_info = stage.serialize("0")
        deserialized_stage = stage_info.deserialize()

        assert not deserialized_stage.params["greedy"]
        assert not deserialized_stage.params["execution_time"]
        assert deserialized_stage.params["schedule_type"] == SchedulerName.DATACENTER

    def test_noc_maintenance_host_scheduler_stage(self):
        stage = noc_maintenance_scheduler_stage(
            [HostRootStage([make_mock_host_stage()])], host_readiness_str=HOST_IS_ASSIGNED_READY
        )
        stage_info = stage.serialize("0")

        deserialized_stage = stage_info.deserialize()
        assert stage == deserialized_stage

    def test_noc_maintenance_host_scheduler_stage_defaults(self):
        stage = noc_maintenance_scheduler_stage(
            [HostRootStage([make_mock_host_stage()])], host_readiness_str=HOST_IS_ASSIGNED_READY
        )
        stage_info = stage.serialize("0")

        deserialized_stage = stage_info.deserialize()

        assert deserialized_stage.params["greedy"]
        assert not deserialized_stage.params["execution_time"]
        assert deserialized_stage.params["schedule_type"] == SchedulerName.ALL

    @pytest.mark.parametrize("stage", (make_mock_stage(), make_mock_host_stage()))
    def test_init_without_root_stage_for_host_scheduler_stage(self, stage):
        with pytest.raises(LogicalError):
            host_scheduler_stage([stage])

    @pytest.mark.parametrize("stage", (make_mock_stage(), make_mock_host_stage()))
    def test_init_without_root_stage_for_noc_maintenance_scheduler_stage(self, stage):
        with pytest.raises(LogicalError):
            noc_maintenance_scheduler_stage([stage], host_readiness_str=HOST_IS_ASSIGNED_READY)
