import http.client

import pytest

from infra.walle.server.tests.lib.util import mock_startrek_client, monkeypatch_config
from sepelib.core import constants
from tests.api.scenario_api.utils import get_scenario_json
from walle.models import timestamp
from walle.scenario.constants import ScriptName


@pytest.fixture(autouse=True)
def startrek_client(mp):
    return mock_startrek_client(mp)


class TestAddWithMaintenancePlotChecks:

    now = timestamp()

    @pytest.mark.usefixtures("authorized_scenario_user")
    @pytest.mark.usefixtures("monkeypatches_for_noop_scenario")
    @pytest.mark.parametrize(
        ["request_dict", "maintenance_plot_scenario_settings_dict", "expected_status_code"],
        [
            (
                {
                    "maintenance_start_time": now + constants.HOUR_SECONDS * 50,
                },
                {
                    "approval_sla": 10 * constants.HOUR_SECONDS,
                },
                http.client.CREATED,
            ),
            (
                {
                    "maintenance_start_time": now + constants.HOUR_SECONDS * 5,
                },
                {
                    "approval_sla": 10 * constants.HOUR_SECONDS,
                },
                http.client.BAD_REQUEST,
            ),
        ],
    )
    def test_create_itdc_maintenance_scenario_with_maintenance_plot_checks(
        self, walle_test, mp, request_dict, maintenance_plot_scenario_settings_dict, expected_status_code
    ):
        monkeypatch_config(mp, "scenario.itdc_maintenance_whitelist_tags", ["rtc"])
        # TODO: Get scenarios with enabled feature from definitions.
        walle_test.mock_project(dict(id="test", tags=["rtc"]))
        walle_test.mock_host(dict(inv=42, project="test"))
        walle_test.mock_maintenance_plot(
            dict(
                scenarios_settings=[
                    {"scenario_type": ScriptName.ITDC_MAINTENANCE, "settings": maintenance_plot_scenario_settings_dict}
                ]
            )
        )
        result = walle_test.api_client.post(
            "/v1/scenarios",
            data=get_scenario_json(script_name=ScriptName.ITDC_MAINTENANCE, script_args=request_dict, hosts=[42]),
        )

        assert result.status_code == expected_status_code

        if expected_status_code != http.client.CREATED:
            walle_test.scenarios.assert_equal()

    @pytest.mark.usefixtures("authorized_scenario_user")
    @pytest.mark.usefixtures("monkeypatches_for_noop_scenario")
    @pytest.mark.parametrize(
        ["request_dict", "maintenance_plot_scenario_settings_dict", "expected_status_code"],
        [
            (
                {
                    "maintenance_start_time": now + constants.HOUR_SECONDS * 50,
                    "switch": "test-switch-s01",
                },
                {
                    "approval_sla": 10 * constants.HOUR_SECONDS,
                },
                http.client.CREATED,
            ),
            (
                {
                    "maintenance_start_time": now + constants.HOUR_SECONDS * 5,
                    "switch": "test-switch-s01",
                },
                {
                    "approval_sla": 10 * constants.HOUR_SECONDS,
                },
                http.client.BAD_REQUEST,
            ),
        ],
    )
    def test_create_noc_hard_scenario_with_maintenance_plot_checks(
        self, walle_test, request_dict, maintenance_plot_scenario_settings_dict, expected_status_code
    ):
        walle_test.mock_project(
            {
                "id": "test-project",
                "tags": ["rtc"],
            }
        )
        walle_test.mock_host(
            {
                "inv": 1,
                "location": {
                    "switch": "test-switch-s01",
                },
                "project": "test-project",
            }
        )
        walle_test.mock_host(dict(inv=42))
        walle_test.mock_maintenance_plot(
            dict(
                scenarios_settings=[
                    {"scenario_type": ScriptName.NOC_HARD, "settings": maintenance_plot_scenario_settings_dict}
                ]
            )
        )
        result = walle_test.api_client.post(
            "/v1/scenarios",
            data=get_scenario_json(script_name=ScriptName.NOC_HARD, script_args=request_dict, hosts=[1]),
        )

        assert result.status_code == expected_status_code

        if expected_status_code != http.client.CREATED:
            walle_test.scenarios.assert_equal()
