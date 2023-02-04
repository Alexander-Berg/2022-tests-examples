import pytest
import http.client

from infra.walle.server.tests.lib.util import (
    TestCase,
    mock_startrek_client,
    monkeypatch_locks,
    find_host_scheduler_stage,
)
from sepelib.core.constants import DAY_SECONDS
from tests.api.scenario_api.utils import get_scenario_json
from walle.models import monkeypatch_timestamp
from walle.scenario.constants import ScriptName
from walle.scenario.handlers import ScenarioModifyAction
from walle.scenario.host_stage_info import HostStageInfo
from walle.scenario.scenario import Scenario
from walle.scenario.script import hosts_transfer_script
from walle.scenario.stage_info import StageStatus
from walle.util.misc import drop_none


@pytest.fixture
def test(request, mp):
    mock_startrek_client(mp)
    monkeypatch_timestamp(mp, cur_time=0)
    return TestCase.create(request)


def get_host_add_rtc_scenario_json(
    name="test",
    ticket_key=None,
    issuer=None,
    labels=None,
    autostart=False,
    hosts_invs=None,
    target_project_id=None,
    target_hardware_segment=None,
):
    data = {
        "name": name,
        "ticket_key": ticket_key,
        "hosts": hosts_invs,
        "issuer": issuer,
        "labels": labels,
        "autostart": autostart,
        "target_project_id": target_project_id,
        "target_hardware_segment": target_hardware_segment,
    }
    return drop_none(data)


class TestAddRtcHostsScenario:

    PROJECT_ID = "mock-project"
    SEGMENT = "pre.cocs"
    INVS = [1, 2, 3]
    TICKET_KEY = "WALLE-3282"

    def test_create_add_rtc_hosts_scenario_authorized(self, test, authorized_scenario_user, authorized_admin):
        project = test.mock_project({"id": self.PROJECT_ID})

        for inv in self.INVS:
            test.mock_host({"inv": inv})

        data = get_host_add_rtc_scenario_json(
            target_project_id=project.id, hosts_invs=self.INVS, ticket_key=self.TICKET_KEY
        )
        result = test.api_client.post("v1/scenarios/hosts_add_rtc", data=data)

        assert result.status_code == http.client.CREATED
        assert result.json["scenario_type"] == ScriptName.HOSTS_TRANSFER

        script_args = dict(target_project_id=project.id, workdays_only=True, idle_time=DAY_SECONDS)
        scenario_parameters = dict(
            target_hardware_segment=None,
            intermediate_project="search-delete",
            delete=False,
            abc_service_id=None,
            **script_args
        )

        test.mock_scenario(
            get_scenario_json(
                script=hosts_transfer_script,
                script_args=script_args,
                hosts=self.INVS,
                next_check_time=Scenario.get_new_next_check_time(),
                ticket_key=self.TICKET_KEY,
                data_storage={"scenario_parameters": scenario_parameters},
            ),
            save=False,
            resolve_uuids=False,
        )
        test.scenarios.assert_equal()

    def test_create_add_rtc_hosts_scenario_unauthorized(self, test):
        result = test.api_client.post(
            "v1/scenarios/hosts_add_rtc",
            data=get_host_add_rtc_scenario_json(hosts_invs=self.INVS, ticket_key=self.TICKET_KEY),
        )
        assert result.status_code == http.client.FORBIDDEN

    @pytest.mark.usefixtures("authorized_scenario_user")
    def test_add_hosts_add_rtc_scenario_and_add_scenario_have_the_same_result(self, test, authorized_admin):
        def drop_name_and_id(json):
            return {key: value for key, value in json.items() if key not in ["name", "scenario_id"]}

        project = test.mock_project({"id": self.PROJECT_ID})

        for inv in self.INVS:
            test.mock_host({"inv": inv})

        data = get_host_add_rtc_scenario_json(
            target_project_id=project.id, hosts_invs=self.INVS, ticket_key=self.TICKET_KEY
        )
        result_from_hosts_add_rtc = test.api_client.post("v1/scenarios/hosts_add_rtc", data=data)

        assert result_from_hosts_add_rtc.status_code == http.client.CREATED

        script_args = dict(target_project_id=project.id, workdays_only=True, idle_time=DAY_SECONDS)
        result_add_scenario = test.api_client.post(
            "/v1/scenarios",
            data=get_scenario_json(
                name="some",
                hosts=self.INVS,
                script_name=hosts_transfer_script.name,
                script_args=script_args,
                ticket_key=self.TICKET_KEY,
            ),
        )

        assert result_add_scenario.status_code == http.client.CREATED
        assert drop_name_and_id(result_from_hosts_add_rtc.json) == drop_name_and_id(result_add_scenario.json)


@pytest.mark.usefixtures("authorized_admin")
def test_skip_wait_stage_for_hosts_transfer(test, mp):
    monkeypatch_locks(mp)
    scenario = Scenario(**get_scenario_json(script=hosts_transfer_script, script_args={"delete": True}))
    scenario.scenario_id = 1
    scenario.name = "test"
    scenario.creation_time = 0
    scenario.issuer = "test-issuer"
    scenario.save()

    host = test.mock_host()
    host_scheduler_stage = find_host_scheduler_stage(scenario.stage_info.stages)
    hsi = HostStageInfo(
        host_uuid=host.uuid, scenario_id=scenario.scenario_id, stage_info=host_scheduler_stage.stages[0]
    )
    hsi.save()

    assert host_scheduler_stage.stages[0].stages[2].status == StageStatus.QUEUE
    assert hsi.stage_info.stages[2].status == StageStatus.QUEUE

    result_add_scenario = test.api_client.post(
        "/v1/scenarios/{}/apply-action".format(scenario.scenario_id),
        data=dict(action=ScenarioModifyAction.SKIP_WAIT_STAGE_FOR_HOSTS_TRANSFER),
    )

    assert result_add_scenario.status_code == http.client.ACCEPTED

    scenario = Scenario.objects.get(scenario_id=scenario.scenario_id)
    host_scheduler_stage = find_host_scheduler_stage(scenario.stage_info.stages)
    assert host_scheduler_stage.stages[0].stages[2].status == StageStatus.FINISHED

    hsi = HostStageInfo.objects.get(host_uuid=host.uuid)
    assert hsi.stage_info.stages[2].status == StageStatus.FINISHED
