import pytest

from infra.walle.server.tests.lib.util import mock_uuid_for_inv, monkeypatch_locks, monkeypatch_method
from infra.walle.server.tests.scenario.utils import make_mock_host_stage, make_mock_stage
from sepelib.core.exceptions import LogicalError
from walle.hosts import Host
from walle.scenario.error_handlers import HssErrorHandler
from walle.scenario.host_stage_info import HostStageInfo
from walle.scenario.marker import Marker
from walle.scenario.scenario import Scenario, ScenarioHostState, HostScenarioStatus
from walle.scenario.stage.host_group_scheduler_stage import HostGroupSchedulerStage
from walle.scenario.stage_info import StageStatus
from walle.scenario.stages import HostRootStage


class TestStaticConstructors:
    def test_init(self):
        stage = HostGroupSchedulerStage([HostRootStage([make_mock_host_stage()])])
        stage_info = stage.serialize("0")
        deserialized_stage = stage_info.deserialize()
        assert stage == deserialized_stage

    @pytest.mark.parametrize("stage", (make_mock_stage(), make_mock_host_stage()))
    def test_init_without_host_root_stage_as_a_child(self, stage):
        with pytest.raises(LogicalError):
            HostGroupSchedulerStage([stage])


class TestAcquiringHosts:

    SCENARIO_ID = 1
    OTHER_SCENARIO_ID = 42
    ACQUIRED = True
    NOT_ACQUIRED = False

    @pytest.mark.parametrize(
        "test_params",
        [
            (  # Two hosts that can be acquired.
                [
                    ({"inv": 0}, SCENARIO_ID, ACQUIRED),
                    ({"inv": 1}, SCENARIO_ID, ACQUIRED),
                ],
                Marker.success(),
            ),
            (  # One host can be acquired, other not - it belongs to another scenario.
                [
                    ({"inv": 0}, SCENARIO_ID, ACQUIRED),
                    ({"inv": 1, "scenario_id": OTHER_SCENARIO_ID}, OTHER_SCENARIO_ID, NOT_ACQUIRED),
                ],
                Marker.in_progress(),
            ),
        ],
    )
    def test_acquire_hosts(self, walle_test, test_params):
        (hosts_params, expected_acquire_hosts_result_marker) = test_params

        hosts = []
        host_infos = {}
        for (test_host_dict, _, _) in hosts_params:
            uuid = mock_uuid_for_inv(test_host_dict["inv"])
            hosts.append(walle_test.mock_host(overrides={**test_host_dict, **{"uuid": uuid}}, save=True))
            host_infos[uuid] = ScenarioHostState(status=HostScenarioStatus.QUEUE)

        stage = HostGroupSchedulerStage([HostRootStage([make_mock_host_stage()])])
        stage_info = stage.serialize("0")

        scenario = Scenario(hosts=host_infos, stage_info=stage_info, scenario_id=self.SCENARIO_ID)

        assert stage._acquire_hosts(hosts, stage_info, scenario).status == expected_acquire_hosts_result_marker.status

        # Check mocked hosts changes.
        for (test_host_dict, expected_scenario_id, expected_to_be_acquired) in hosts_params:
            updated_host = Host.get_by_inv(test_host_dict["inv"])
            assert expected_scenario_id == updated_host.scenario_id

            uuid = updated_host.uuid
            if expected_to_be_acquired:
                assert scenario.hosts[uuid].status == HostScenarioStatus.ACQUIRED
                assert HostStageInfo.objects(host_uuid=uuid).count()
            else:
                assert not HostStageInfo.objects(host_uuid=uuid).count()

    @pytest.mark.parametrize(
        "test_params",
        [
            # While all hosts are not acquired, 'are_all_hosts_acquired' flag is not set.
            (Marker.in_progress(), None, Marker.in_progress()),
            # After all hosts are acquired, 'are_all_hosts_acquired' flag becomes set.
            (Marker.success(), True, Marker.success()),
        ],
    )
    def test_that_are_all_hosts_acquired_flag_sets_properly(self, mp, test_params):
        (acquire_hosts_result_marker, expected_are_all_hosts_acquired, expected_run_result_marker) = test_params

        monkeypatch_method(
            mp,
            method=HostGroupSchedulerStage._get_hosts_of_the_group_that_are_not_done,
            obj=HostGroupSchedulerStage,
            return_value=[],
        )
        monkeypatch_method(
            mp,
            method=HostGroupSchedulerStage._acquire_hosts,
            obj=HostGroupSchedulerStage,
            return_value=acquire_hosts_result_marker,
        )
        monkeypatch_method(
            mp, method=HostGroupSchedulerStage._execute_host_stages, obj=HostGroupSchedulerStage, return_value=True
        )
        monkeypatch_method(
            mp,
            method=HostGroupSchedulerStage._remove_host_stage_info_documents,
            obj=HostGroupSchedulerStage,
            return_value=None,
        )

        stage = HostGroupSchedulerStage([HostRootStage([make_mock_host_stage()])])
        stage_info = stage.serialize("0")
        scenario = Scenario()
        run_result = stage.run(stage_info, scenario, 0)

        assert run_result.status == expected_run_result_marker.status
        assert stage_info.get_data(HostGroupSchedulerStage.are_all_hosts_acquired) is expected_are_all_hosts_acquired


class TestGettingHostsOfGroup:
    @pytest.mark.parametrize(
        "test_params",
        [
            [  # Two groups of hosts. Several hosts from the second group won't be returned as a part of the group.
                {
                    "group_id": 1,
                    "hosts": [
                        (1, HostScenarioStatus.QUEUE),
                        (2, HostScenarioStatus.QUEUE),
                    ],
                    "expected_uuids": [
                        mock_uuid_for_inv(1),
                        mock_uuid_for_inv(2),
                    ],
                },
                {
                    "group_id": 2,
                    "hosts": [
                        (3, HostScenarioStatus.QUEUE),
                        (4, HostScenarioStatus.DONE),
                        (5, HostScenarioStatus.QUEUE),
                        (6, HostScenarioStatus.DONE),
                    ],
                    "expected_uuids": [
                        mock_uuid_for_inv(3),
                        mock_uuid_for_inv(5),
                    ],
                },
            ]
        ],
    )
    def test_get_current_hosts_group(self, walle_test, test_params):
        host_infos = {}

        # Mock hosts of all groups.
        for hosts_group_data in test_params:
            for (inv, host_scenario_status) in hosts_group_data["hosts"]:
                uuid = mock_uuid_for_inv(inv)
                walle_test.mock_host(overrides=dict({"inv": inv, "uuid": uuid}), save=True)
                host_infos[uuid] = ScenarioHostState(group=hosts_group_data["group_id"], status=host_scenario_status)

        children = [HostRootStage([make_mock_host_stage()])]

        # Get hosts of each group.
        for hosts_group_data in test_params:
            actual_hosts = HostGroupSchedulerStage(children=children)._get_hosts_of_the_group_that_are_not_done(
                host_infos, hosts_group_data["group_id"]
            )
            actual_uuids = [host.uuid for host in actual_hosts]
            assert hosts_group_data["expected_uuids"] == actual_uuids


# noinspection DuplicatedCode
class TestProcessingHosts:

    SCENARIO_ID = 1
    STAGE = HostGroupSchedulerStage(
        [HostRootStage([make_mock_host_stage(), make_mock_host_stage(), make_mock_host_stage()])]
    )

    def test_process_single_host(self, mp, walle_test):
        """Shows how _process_single_host() method changes 'HostStageInfo' documents in database."""
        monkeypatch_locks(mp)

        stage_info = self.STAGE.serialize("0")

        uuid = mock_uuid_for_inv(0)
        host = walle_test.mock_host(overrides={"inv": 0, "uuid": uuid}, save=True)
        scenario = Scenario(
            hosts={uuid: ScenarioHostState(status=HostScenarioStatus.QUEUE)},
            stage_info=stage_info,
            scenario_id=self.SCENARIO_ID,
        )

        host_root_stage = stage_info.stages[0]

        # Being verbose to illustrate what's happening.
        # Acquire host to create its 'HostStageInfo' in database.
        self.STAGE._acquire_hosts([host], host_root_stage, scenario)

        # Before executing host stages.
        host_stage_info_document = HostStageInfo.objects(
            **dict(scenario_id=scenario.scenario_id, host_uuid=uuid)
        ).first()
        assert host_stage_info_document.revision == 0
        assert host_stage_info_document.stage_info.stages[0].status == StageStatus.QUEUE
        assert host_stage_info_document.stage_info.stages[1].status == StageStatus.QUEUE
        assert host_stage_info_document.stage_info.stages[2].status == StageStatus.QUEUE
        assert host_stage_info_document.stage_info.status == StageStatus.QUEUE
        assert scenario.hosts[uuid].status == HostScenarioStatus.ACQUIRED

        error_handler = HssErrorHandler()

        # First host stage executed.
        self.STAGE._process_single_host(scenario, host, error_handler, host_root_stage)
        host_stage_info_document = HostStageInfo.objects(
            **dict(scenario_id=scenario.scenario_id, host_uuid=uuid)
        ).first()
        assert host_stage_info_document.revision == 1
        assert host_stage_info_document.stage_info.stages[0].status == StageStatus.FINISHED
        assert host_stage_info_document.stage_info.stages[1].status == StageStatus.QUEUE
        assert host_stage_info_document.stage_info.stages[2].status == StageStatus.QUEUE
        assert host_stage_info_document.stage_info.status == StageStatus.QUEUE
        assert scenario.hosts[uuid].status == HostScenarioStatus.PROCESSING

        # Second host stage executed.
        self.STAGE._process_single_host(scenario, host, error_handler, host_root_stage)
        host_stage_info_document = HostStageInfo.objects(
            **dict(scenario_id=scenario.scenario_id, host_uuid=uuid)
        ).first()
        assert host_stage_info_document.revision == 2
        assert host_stage_info_document.stage_info.stages[0].status == StageStatus.FINISHED
        assert host_stage_info_document.stage_info.stages[1].status == StageStatus.FINISHED
        assert host_stage_info_document.stage_info.stages[2].status == StageStatus.QUEUE
        assert host_stage_info_document.stage_info.status == StageStatus.QUEUE
        assert scenario.hosts[uuid].status == HostScenarioStatus.PROCESSING

        # Third (last) host stage executed.
        self.STAGE._process_single_host(scenario, host, error_handler, host_root_stage)
        host_stage_info_document = HostStageInfo.objects(
            **dict(scenario_id=scenario.scenario_id, host_uuid=uuid)
        ).first()
        assert host_stage_info_document.revision == 3
        assert host_stage_info_document.stage_info.stages[0].status == StageStatus.FINISHED
        assert host_stage_info_document.stage_info.stages[1].status == StageStatus.FINISHED
        assert host_stage_info_document.stage_info.stages[2].status == StageStatus.FINISHED
        assert host_stage_info_document.stage_info.status == StageStatus.FINISHED
        assert scenario.hosts[uuid].status == HostScenarioStatus.DONE

    def test_execute_host_stages(self, mp, walle_test):
        """
        Shows how _execute_host_stages() changes child stages' statuses and issues stage result
        when all child stages finshed.
        """
        monkeypatch_locks(mp)

        # Create five hosts.
        hosts = [
            walle_test.mock_host(overrides={"inv": inv, "uuid": mock_uuid_for_inv(inv)}, save=True)
            for inv in range(0, 5)
        ]

        scenario_hosts = {host.uuid: ScenarioHostState(status=HostScenarioStatus.QUEUE) for host in hosts}
        stage_info = self.STAGE.serialize("0")
        scenario = Scenario(hosts=scenario_hosts, stage_info=stage_info, scenario_id=self.SCENARIO_ID)

        host_root_stage = stage_info.stages[0]

        # Being verbose to illustrate what's happening.
        # Check that there are no 'HostStageInfo' objects in database before acquiring hosts.
        assert not HostStageInfo.objects(scenario_id=self.SCENARIO_ID).count()

        # Acquire hosts to create their 'HostStageInfo' in database.
        self.STAGE._acquire_hosts(hosts, host_root_stage, scenario)

        # Check that 'HostStageInfo' objects for each host were created in database.
        assert HostStageInfo.objects(scenario_id=self.SCENARIO_ID).count() == 5

        # Check that host stages's status were not modified yet.
        assert host_root_stage.status == StageStatus.QUEUE

        # First run.
        assert not self.STAGE._execute_host_stages(hosts, host_root_stage, scenario)
        scenario.update_stage_info_hosts_for_all_child_stages()
        assert host_root_stage.status == StageStatus.PROCESSING
        # assert host_root_stage.stages[0].status == StageStatus.FINISHED
        assert host_root_stage.stages[1].status == StageStatus.QUEUE
        assert host_root_stage.stages[2].status == StageStatus.QUEUE

        # Second run.
        assert not self.STAGE._execute_host_stages(hosts, host_root_stage, scenario)
        scenario.update_stage_info_hosts_for_all_child_stages()
        assert host_root_stage.status == StageStatus.PROCESSING
        assert host_root_stage.stages[0].status == StageStatus.FINISHED
        # assert host_root_stage.stages[1].status == StageStatus.FINISHED
        assert host_root_stage.stages[2].status == StageStatus.QUEUE

        # Third run.
        assert self.STAGE._execute_host_stages(hosts, host_root_stage, scenario)
        scenario.update_stage_info_hosts_for_all_child_stages()
        assert host_root_stage.status == StageStatus.FINISHED
        assert host_root_stage.stages[0].status == StageStatus.FINISHED
        assert host_root_stage.stages[1].status == StageStatus.FINISHED
        # assert host_root_stage.stages[2].status == StageStatus.FINISHED

        # Check that 'HostStageInfo' objects for each host still present in database.
        assert HostStageInfo.objects(scenario_id=self.SCENARIO_ID).count() == 5
