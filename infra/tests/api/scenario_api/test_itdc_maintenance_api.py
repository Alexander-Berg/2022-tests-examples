import pytest
import http.client

from infra.walle.server.tests.lib.util import mock_startrek_client, TestCase
from tests.api.scenario_api.utils import get_scenario_json
from walle.models import monkeypatch_timestamp
from walle.scenario.script import itdc_maintenance_script


@pytest.fixture
def test(request, mp):
    mock_startrek_client(mp)
    monkeypatch_timestamp(mp, cur_time=0)
    return TestCase.create(request)


def scenario_start(test, scenario_id):
    return test.api_client.open(
        f"/v1/scenarios/{scenario_id}",
        method="PATCH",
        data={"reason": "reason-mock", "labels": {"WORK_COMPLETED": "true"}},
    )


@pytest.mark.usefixtures("unauthenticated")
def test_start_itdc_scenario_unauthenticated(test):
    test.mock_scenario(get_scenario_json(name="existed", script_name=itdc_maintenance_script.name))
    result = scenario_start(test, 0)
    assert result.status_code == http.client.UNAUTHORIZED


def test_start_itdc_scenario_unauthrorized(test):
    test.mock_scenario(get_scenario_json(name="existed", script_name=itdc_maintenance_script.name))
    result = scenario_start(test, 0)
    assert result.status_code == http.client.FORBIDDEN


def test_start_non_existing_itdc_scenario(test, authorized_scenario_user):
    result = scenario_start(test, 0)
    assert result.status_code == http.client.NOT_FOUND
