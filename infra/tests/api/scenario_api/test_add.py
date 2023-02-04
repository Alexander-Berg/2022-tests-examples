import http.client

import pytest

from infra.walle.server.tests.lib.util import (
    mock_startrek_client,
    monkeypatch_function,
    mock_response,
    mock_uuid_for_inv,
    TestCase,
)
from sepelib.yandex.startrek import StartrekRequestError
from tests.api.scenario_api.utils import get_scenario_json
from walle.models import monkeypatch_timestamp
from walle.scenario.host_groups_builders import by_bot_project_id
from walle.scenario.scenario import Scenario
from walle.scenario.script import (
    noop_script,
    switch_to_maintenance_script,
    approve_hosts_script,
    hosts_add_script,
    wait_script,
    hosts_transfer_script,
    itdc_maintenance_script,
    noc_hard_script,
)
from walle.scenario.script_args import check_qloud_segment_exists


@pytest.fixture
def test(request, mp):
    monkeypatch_timestamp(mp, cur_time=0)
    return TestCase.create(request)


@pytest.fixture(autouse=True)
def startrek_client(mp):
    return mock_startrek_client(mp)


@pytest.fixture
def mock_get_abc_project_slug_from_bot_project_id(monkeypatch):
    def _mocked_get_abc_project_slug_from_bot_project_id(_bot_project_id: int) -> str:
        return str(_bot_project_id)

    monkeypatch.setattr(
        by_bot_project_id, "_get_abc_project_slug_from_bot_project_id", _mocked_get_abc_project_slug_from_bot_project_id
    )


def test_add_scenario_by_non_admin(test):
    result = test.api_client.post("v1/scenarios", data=get_scenario_json(script_name=noop_script.name))
    assert result.status_code == http.client.FORBIDDEN
    assert not Scenario.objects.count()


@pytest.mark.usefixtures("authorized_scenario_user")
def test_add_scenario_by_admin(test):
    result = test.api_client.post("v1/scenarios", data=get_scenario_json(script_name=noop_script.name))
    assert result.status_code == http.client.CREATED
    assert Scenario.objects.count() == 1


def test_add_scenario_by_authorized_user(test, authorized_scenario_user):
    result = test.api_client.post("v1/scenarios", data=get_scenario_json(script_name=noop_script.name))
    assert result.status_code == http.client.CREATED
    assert Scenario.objects.count() == 1


@pytest.mark.usefixtures("authorized_scenario_user")
def test_add_scenario_failed_validation_of_ticket_key(test, mp, startrek_client):
    data = get_scenario_json(script_name=noop_script.name)
    response = mock_response({}, status_code=http.client.NOT_FOUND)
    startrek_client.get_issue.side_effect = StartrekRequestError(response, "mock-error")

    result = test.api_client.post("/v1/scenarios", data=data)

    assert result.status_code == http.client.BAD_REQUEST
    assert (
        result.json["message"] == "Request validation error: Ticket key validation error: "
        "Startrek ticket WALLE-2413 does not exist"
    )
    assert not Scenario.objects.count()


@pytest.mark.usefixtures("authorized_scenario_user")
def test_add_scenario_failed_validation_of_labels(test):
    data = get_scenario_json(labels={1: [1, 2]}, script_name=noop_script.name)
    result = test.api_client.post("/v1/scenarios", data=data)
    assert result.status_code == http.client.BAD_REQUEST
    assert (
        result.json["message"] == "Request validation error: Labels validation errors: "
        "Key or value isn't [String]/[Integer], wrong pair - 1:[1, 2]"
    )

    assert not Scenario.objects.count()


@pytest.mark.usefixtures("authorized_scenario_user")
@pytest.mark.parametrize(
    ["script_args", "error_message"],
    [({"target_project_id": "1"}, "Invalid params for scenario: Project id '1' does not exist")],
)
def test_add_scenario_failed_validation_of_script_args(test, script_args, error_message):
    data = get_scenario_json(script_args=script_args, script_name=hosts_add_script.name)
    result = test.api_client.post("/v1/scenarios", data=data)
    assert result.status_code == http.client.BAD_REQUEST
    assert result.json["message"] == error_message
    assert not Scenario.objects.count()


@pytest.mark.usefixtures("authorized_scenario_user")
@pytest.mark.parametrize(
    "data",
    [
        {"name": "a", "scenario_type": noop_script.name},
        {"name": "a", "ticket_key": "WALLE-2807"},
        {"scenario_type": noop_script.name, "ticket_key": "WALLE-2807"},
        {"name": "a"},
        {"scenario_type": noop_script.name},
        {"ticket_key": "WALLE-2807"},
    ],
)
def test_add_scenario_without_required_fields(test, data):
    result = test.api_client.post("/v1/scenarios", data=data)
    assert result.status_code == http.client.BAD_REQUEST
    assert not Scenario.objects.count()


@pytest.mark.usefixtures("authorized_scenario_user")
@pytest.mark.parametrize(
    ["mock_invs", "hosts_string_tokens_from_request", "assumed_hosts_tokens_type"],
    [
        ([1, 2, 3], ["1", "2", "3"], "inv"),
        ([4, 5, 6], ["server-4", "server-5", "server-6"], "name"),
        ([7, 8, 9], [mock_uuid_for_inv(7), mock_uuid_for_inv(8), mock_uuid_for_inv(9)], "uuid"),
    ],
)
def test_add_scenario_with_hosts(test, mock_invs, hosts_string_tokens_from_request, assumed_hosts_tokens_type):
    for inv, host_string_token in dict(zip(mock_invs, hosts_string_tokens_from_request)).items():
        if assumed_hosts_tokens_type == "inv":
            test.mock_host(
                {
                    "inv": inv,
                }
            )
        else:
            test.mock_host(
                {
                    "inv": inv,
                    assumed_hosts_tokens_type: host_string_token,
                }
            )

    result = test.api_client.post(
        "/v1/scenarios", data=get_scenario_json(hosts=hosts_string_tokens_from_request, script_name=noop_script.name)
    )

    assert result.status_code == http.client.CREATED
    assert {h.inv for h in Scenario.objects.get(result.json["scenario_id"]).hosts.values()} == set(mock_invs)


@pytest.mark.usefixtures("authorized_scenario_user")
def test_add_scenario_in_status_started(test):
    result = test.api_client.post("/v1/scenarios", data=get_scenario_json(autostart=True, script_name=noop_script.name))
    assert result.status_code == http.client.CREATED
    assert Scenario.objects.count() == 1


@pytest.mark.usefixtures("authorized_scenario_user")
def test_add_scenario_with_script_args_and_hosts(test):
    test.mock_project(dict(id="q"))
    script_args = {"target_project_id": "q"}

    result = test.api_client.post(
        "/v1/scenarios", data=get_scenario_json(script_args=script_args, script_name=noop_script.name)
    )
    assert result.status_code == http.client.CREATED
    assert Scenario.objects.count() == 1


@pytest.mark.usefixtures("authorized_scenario_user")
def test_add_scenario_with_labels(test):
    labels = {"a": 1}

    result = test.api_client.post("/v1/scenarios", data=get_scenario_json(labels=labels, script_name=noop_script.name))
    assert result.status_code == http.client.CREATED
    assert Scenario.objects.count() == 1


@pytest.mark.usefixtures("authorized_scenario_user")
def test_add_scenario_with_not_unique_name(test):
    name = "test"
    test.mock_scenario(dict(name=name, ticket_key="EXISTING"))

    result = test.api_client.post(
        "v1/scenarios", data=dict(name=name, scenario_type=noop_script.name, ticket_key="PROHIBITED-1")
    )
    assert result.status_code == http.client.CONFLICT
    assert Scenario.objects.count() == 1


@pytest.mark.usefixtures("authorized_scenario_user")
@pytest.mark.parametrize(
    ["script_args", "error_msg"],
    [
        [
            {"unknown": 1, "target_project_id": "q"},
            "Invalid params for scenario: Unknown parameters were sent: {}; ".format(['unknown']),
        ],
        [{}, "Invalid params for scenario: You must specify target_project_id or target_hardware_segment"],
        [{"unknown": 1}, "Invalid params for scenario: Unknown parameters were sent: {}; ".format(['unknown'])],
    ],
)
def test_add_scenario_with_bad_script_arg(test, script_args, error_msg):
    test.mock_project(dict(id="q"))

    result = test.api_client.post(
        "/v1/scenarios", data=get_scenario_json(script_args=script_args, script_name=hosts_add_script.name)
    )
    assert result.status_code == http.client.BAD_REQUEST
    assert result.json["message"] == error_msg
    assert not Scenario.objects.count()


@pytest.mark.usefixtures("authorized_scenario_user")
@pytest.mark.parametrize(
    ["script", "script_args"],
    [
        [wait_script, {"target_project_id": "q"}],
        [noop_script, None],
        [hosts_add_script, {"target_project_id": "q"}],
        [switch_to_maintenance_script, None],
        [approve_hosts_script, None],
        [wait_script, {"target_project_id": "q"}],
        [hosts_add_script, {"target_project_id": "q"}],
        [hosts_add_script, {"target_hardware_segment": "ext.mock"}],
        [hosts_add_script, {"target_project_id": "q", "target_hardware_segment": "ext.mock"}],
        [hosts_transfer_script, {"target_project_id": "q", "abc_service_id": 1}],
    ],
)
def test_add_scenario_of_all_types_with_minimum_script_args(test, mp, script, script_args):
    test.mock_project(dict(id="q"))
    monkeypatch_function(mp, check_qloud_segment_exists, return_value=None)

    result = test.api_client.post(
        "/v1/scenarios", data=get_scenario_json(script_args=script_args, script_name=script.name)
    )
    assert result.status_code == http.client.CREATED
    assert Scenario.objects.count() == 1


@pytest.mark.usefixtures("authorized_scenario_user")
@pytest.mark.parametrize(
    "script_args",
    [
        {"idle_time": 31, "workdays_only": False},
        {"idle_time": 31},
        {"workdays_only": False},
    ],
)
def test_add_scenario_with_admin_params_for_non_admin(test, script_args):
    result = test.api_client.post(
        "/v1/scenarios", data=get_scenario_json(script_args=script_args, script_name=hosts_transfer_script.name)
    )
    assert result.status_code == http.client.FORBIDDEN


@pytest.mark.usefixtures("authorized_scenario_user")
@pytest.mark.parametrize(
    "script_args",
    [
        {"idle_time": 31, "workdays_only": False},
        {"idle_time": 31},
        {"workdays_only": False},
    ],
)
def test_add_scenario_with_admin_params_for_admin(test, authorized_admin, script_args):
    test.mock_project(dict(id="q"))
    script_args.update({"target_project_id": "q"})

    result = test.api_client.post(
        "/v1/scenarios", data=get_scenario_json(script_args=script_args, script_name=hosts_transfer_script.name)
    )
    assert result.status_code == http.client.CREATED
    assert Scenario.objects.count() == 1


@pytest.mark.usefixtures("authorized_scenario_user")
@pytest.mark.parametrize("host_count", [1001, 2000])
def test_forbidden_add_scenario_with_gt_1000_hosts(test, host_count):
    hosts = [i for i in range(host_count)]

    result = test.api_client.post("/v1/scenarios", data=get_scenario_json(hosts=hosts, script_name=noop_script.name))

    assert result.status_code == http.client.BAD_REQUEST
    assert not Scenario.objects.count()


@pytest.mark.slow
@pytest.mark.usefixtures("authorized_scenario_user")
@pytest.mark.parametrize("host_count", [100, 5, 0])
def test_allowed_add_scenario_with_lte_1000_hosts(test, host_count):
    invs = []
    for i in range(host_count):
        test.mock_host(dict(inv=i))
        invs.append(i)

    result = test.api_client.post("/v1/scenarios", data=get_scenario_json(hosts=invs, script_name=noop_script.name))

    assert result.status_code == http.client.CREATED
    assert Scenario.objects.count() == 1


@pytest.mark.usefixtures("authorized_scenario_user")
@pytest.mark.parametrize(
    ["hosts_from_request", "expected_scenario_invs", "expected_status_code"],
    [
        ([1, 2, 3], [1, 2, 3], http.client.CREATED),
        (["4", "5", "6"], [4, 5, 6], http.client.CREATED),
        (["server-7", "server-8", "server-9"], [], http.client.BAD_REQUEST),
    ],
)
def test_create_hosts_transfer_scenario(test, hosts_from_request, expected_scenario_invs, expected_status_code):

    test.mock_project(dict(id="some-project-id"))
    script_args = {"target_project_id": "some-project-id"}

    result = test.api_client.post(
        "/v1/scenarios",
        data=get_scenario_json(
            hosts=hosts_from_request,
            script_name=hosts_transfer_script.name,
            script_args=script_args,
        ),
    )
    assert result.status_code == expected_status_code

    if expected_status_code == http.client.CREATED:
        actual_scenario_invs = [host["inv"] for host in result.json["hosts"]]
        assert sorted(actual_scenario_invs) == expected_scenario_invs
    elif expected_status_code == http.client.BAD_REQUEST:
        msg = (
            "Hosts-transfer scenario can use only invs. "
            "If next values are invs, please report to Wall-e Devs: {}".format(", ".join(hosts_from_request))
        )
        assert result.json["message"] == msg
        test.scenarios.assert_equal()
    else:
        assert False


@pytest.mark.usefixtures("monkeypatches_for_noop_scenario")
@pytest.mark.usefixtures("authorized_scenario_user")
@pytest.mark.parametrize(
    ["request_dict", "project_tags", "expected_status_code", "maintenance_plot_id"],
    [
        (
            {
                "maintenance_start_time": "1970-01-01 00:00:00",
                "maintenance_end_time": 42,
            },
            ["rtc"],
            http.client.BAD_REQUEST,
            "mocked-maintenance-plot-id",
        ),
        (
            {
                "maintenance_start_time": 1000,
                "maintenance_end_time": 1060,
            },
            ["rtc"],
            http.client.CREATED,
            "mocked-maintenance-plot-id",
        ),
        ({}, ["rtc"], http.client.CREATED, "mocked-maintenance-plot-id"),
    ],
)
@pytest.mark.usefixtures("mock_get_abc_project_slug_from_bot_project_id")
def test_create_itdc_maintenance_scenario(
    test, mp, request_dict, project_tags, expected_status_code, maintenance_plot_id
):
    test.mock_project({"id": "some-rtc-project", "tags": project_tags, "maintenance_plot_id": maintenance_plot_id})
    test.mock_host({"inv": 42, "project": "some-rtc-project"})
    test.mock_maintenance_plot()
    result = test.api_client.post(
        "/v1/scenarios",
        data=get_scenario_json(script_name=itdc_maintenance_script.name, script_args=request_dict, hosts=[42]),
    )

    assert result.status_code == expected_status_code

    if expected_status_code != http.client.CREATED:
        test.scenarios.assert_equal()


@pytest.mark.usefixtures("monkeypatches_for_noop_scenario")
@pytest.mark.usefixtures("authorized_scenario_user")
@pytest.mark.parametrize(
    ["request_dict", "expected_status_code"],
    [
        (
            {
                "maintenance_start_time": 1000,
                "maintenance_end_time": 1060,
                "switch": "test-switch-s01",
            },
            http.client.CREATED,
        ),
        (
            {
                "maintenance_start_time": None,
                "maintenance_end_time": None,
                "switch": "test-switch-s01",
            },
            http.client.CREATED,
        ),
        (
            {
                "switch": "test-switch-s01",
            },
            http.client.CREATED,
        ),
        (
            {
                "switch": "unexistent-switch",
            },
            http.client.BAD_REQUEST,
        ),
        (
            {
                "maintenance_start_time": "foo",
                "maintenance_end_time": "bar",
                "switch": "test-switch-s01",
            },
            http.client.BAD_REQUEST,
        ),
        (
            {
                "switch": 42,
            },
            http.client.BAD_REQUEST,
        ),
        (
            {
                "maintenance_start_time": "1970-01-01 00:00:00",
                "maintenance_end_time": "1970-01-01 01:00:00",
            },
            http.client.BAD_REQUEST,
        ),
        (
            {},
            http.client.BAD_REQUEST,
        ),
    ],
)
def test_create_noc_hard_scenario(test, request_dict, expected_status_code):
    test.mock_project(
        {
            "id": "test-project",
            "tags": ["rtc"],
        }
    )
    test.mock_host(
        {
            "inv": 1,
            "location": {
                "switch": "test-switch-s01",
            },
            "project": "test-project",
        }
    )
    test.mock_maintenance_plot()

    result = test.api_client.post(
        "/v1/scenarios", data=get_scenario_json(script_name=noc_hard_script.name, script_args=request_dict, hosts=[])
    )

    assert result.status_code == expected_status_code

    if expected_status_code != http.client.CREATED:
        test.scenarios.assert_equal()
