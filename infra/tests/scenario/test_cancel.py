"""Test cancelling scenarios."""
import http.client
from unittest import mock

import pytest

from infra.walle.server.tests.lib.scenario_util import launch_scenario
from walle.scenario.constants import ScenarioFsmStatus, ScenarioWorkStatus, ScriptName
from walle.scenario.host_stage_info import HostStageInfo
from walle.scenario.scenario import Scenario
from walle.scenario.script import ScriptRegistry
from walle.scenario.script_args import EmptyParams
from walle.scenario.stage_info import StageInfo
from walle.scenario.stages import ScenarioRootStage, NoopStage


@pytest.fixture()
def mock_config(mp):
    mp.config("scenario.stages.scenario_root_stage.startrek_scenario_canceled_summonees", ["foo", "bar"])


def get_noop_script(_params):
    return ScenarioRootStage(
        [
            NoopStage(),
            NoopStage(),
            NoopStage(),
        ]
    )


def set_work_status_rejected(scenario):
    scenario.set_works_status_label(ScenarioWorkStatus.REJECTED)


def cancel_scenario(scenario):
    scenario.cancel()


@pytest.mark.slow
@pytest.mark.usefixtures("mock_config")
@pytest.mark.parametrize("cancel_func", [set_work_status_rejected, cancel_scenario])
def test_cancel_execution(mp, walle_test, authorized_scenario_user, startrek_client, cancel_func):
    def _get_noop_script():
        return get_noop_script

    mocked_noop_script = _get_noop_script()
    mocked_noop_script.name = "HEY"
    mocked_noop_script.uses_uuids = False
    mocked_noop_script.attrs_cls = EmptyParams
    ScriptRegistry.ITEMS["noop"] = get_noop_script

    host_inv = 42
    host = walle_test.mock_host(dict(inv=host_inv))

    scenario_params = dict(
        name="mocked-noop-scenario-execution-test",
        reason="Test Mocked NOOP scenario",
        ticket_key="WALLE-1",
        scenario_type=ScriptName.NOOP,
        hosts=[host_inv],
    )
    response = walle_test.api_client.post("v1/scenarios", data=scenario_params)
    assert response.status_code == http.client.CREATED
    scenario = Scenario.objects.get(scenario_id=response.json["scenario_id"])

    host.scenario_id = scenario.scenario_id
    HostStageInfo(host_uuid=host.uuid, scenario_id=scenario.scenario_id, stage_info=StageInfo(uid="0")).save()

    scenario.start()
    assert scenario.status == ScenarioFsmStatus.STARTED

    scenario = launch_scenario(scenario)
    scenario = launch_scenario(scenario)

    cancel_func(scenario)
    scenario.save()

    assert startrek_client.add_comment.mock_calls == []

    scenario = launch_scenario(scenario)

    startrek_client.add_comment.assert_has_calls(
        [
            mock.call(issue_id=scenario.ticket_key, summonees=["mocked-user@".strip("@"), "foo", "bar"], text=mock.ANY),
        ]
    )
    assert scenario.status == ScenarioFsmStatus.CANCELED
    assert scenario.get_works_status() == ScenarioWorkStatus.CANCELED
    assert scenario.message == ScenarioRootStage._cancel_message_template.format("WALLE-1")

    assert HostStageInfo.objects.count() == 0
    host.scenario_id = None
    walle_test.hosts.assert_equal()
