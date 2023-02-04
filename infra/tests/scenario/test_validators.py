import http.client

import attr
import pytest

from infra.walle.server.tests.lib.util import mock_response
from sepelib.yandex.startrek import StartrekRequestError, StartrekConnectionError
from walle.clients import startrek
from walle.errors import RequestValidationError
from walle.scenario.errors import ScenarioValidationError
from walle.scenario.script_args import check_project_exists_and_automation_enabled, check_between
from walle.scenario.validators import validate_labels_fn, validate_ticket_key_fn, validate_scenario_params

TICKET = "WALLE-1"


def test_validate_labels_with_valid_labels():
    labels = {"a": "a", "b": 0, 1: "c", 2: 3}
    try:
        validate_labels_fn(labels)
    except ScenarioValidationError:
        assert False


@pytest.mark.parametrize("key", [True, None, ("a", "b"), frozenset([1, 2, 3]), int, isinstance])
def test_validate_labels_raise_error_when_get_not_str_or_int_key(key):
    labels = {key: "a"}
    with pytest.raises(ScenarioValidationError):
        validate_labels_fn(labels)


@pytest.mark.parametrize(
    "value", [True, None, ("a", "b"), frozenset([1, 2, 3]), int, isinstance, [1, 2, 3], {"a", "b"}, {1: 2}]
)
def test_validate_labels_raise_error_when_get_not_str_or_int_value(value):
    labels = {"a": value}
    with pytest.raises(ScenarioValidationError):
        validate_labels_fn(labels)


def test_validate_ticket_key_connection_error(mp):
    mock_startrek = mp.function(startrek.get_client)
    mock_startrek().get_issue.side_effect = StartrekConnectionError("mock-error")

    with pytest.raises(ScenarioValidationError) as e:
        validate_ticket_key_fn(TICKET)
    assert str(e.value) == "Ticket key validation error: Connection with Startrek failed"


@pytest.mark.parametrize(
    "status_code, message",
    (
        (http.client.NOT_FOUND, "Ticket key validation error: Startrek ticket {} does not exist".format(TICKET)),
        (
            http.client.BAD_REQUEST,
            "Ticket key validation error: Failed to get status for startrek ticket {}: "
            "Error in communication with Startrek: mock-error".format(TICKET),
        ),
    ),
)
def test_validate_ticket_key_not_exist(mp, status_code, message):
    response = mock_response({}, status_code=status_code)
    mock_startrek = mp.function(startrek.get_client)
    mock_startrek().get_issue.side_effect = StartrekRequestError(response, "mock-error")
    with pytest.raises(ScenarioValidationError) as e:
        validate_ticket_key_fn(TICKET)
    assert str(e.value) == "{}".format(message)


def test_validate_ticket_key(mp):
    mp.function(startrek.get_client)
    try:
        validate_ticket_key_fn(TICKET)
    except ScenarioValidationError:
        assert False


def test_check_project_id_not_exist(walle_test):
    project_id = "test"
    with pytest.raises(ScenarioValidationError) as e:
        check_project_exists_and_automation_enabled(None, None, project_id)
    assert str(e.value) == "Project id '{}' does not exist".format(project_id)


def test_check_project_id_exist(walle_test):
    project = walle_test.mock_project({"id": "test"})
    try:
        check_project_exists_and_automation_enabled(None, None, project.id)
    except ScenarioValidationError:
        assert False


@pytest.mark.parametrize("healing_automation, dns_automation", [(True, False), (False, True), (False, False)])
def test_check_project_has_disabled_automation(walle_test, healing_automation, dns_automation):
    project = walle_test.mock_project(
        {
            "id": "test",
            "healing_automation": {"enabled": healing_automation},
            "dns_automation": {"enabled": dns_automation},
        }
    )
    with pytest.raises(ScenarioValidationError):
        check_project_exists_and_automation_enabled(None, None, project.id)


@attr.s
class MockAttrs:
    a = attr.ib(validator=check_between(30, 1209600))


@pytest.mark.parametrize("value", [0, 30, -100, 1209600])
def test_check_idle_time_invalid(value):
    with pytest.raises(ScenarioValidationError) as e:
        MockAttrs(value)
        assert str(e.value) == "Parameter 'a' must be in range of 30 and 1209600 (got {})".format(value)


@pytest.mark.parametrize("value", [31, 1000, 86399])
def test_check_idle_time_valid(value):
    MockAttrs(value)


@pytest.mark.parametrize(
    ["target_project_id", "ticket_key", "labels", "error_message"],
    [
        (
            "test-2",
            None,
            {"a": [1, 2]},
            "Request validation error: Ticket key validation error: "
            "no ticket key specified; Labels validation errors: "
            "Key or value isn't [String]/[Integer], wrong pair - a:[1, 2]",
        ),
        (
            "test-2",
            "TEST-1",
            {"a": [1, 2]},
            "Request validation error: "
            "Labels validation errors: Key or value isn't [String]/[Integer], wrong pair - a:[1, 2]",
        ),
        ("test-2", None, {"a": 1}, "Request validation error: Ticket key validation error: no ticket key specified"),
        ("test-2", "", {"a": 1}, "Request validation error: Ticket key validation error: no ticket key specified"),
        ("test-2", "TEST", {"a": 1}, "Request validation error: Ticket key validation error: invalid ticket format"),
    ],
)
def test_validate_scenario_params_invalid(mp, walle_test, target_project_id, ticket_key, labels, error_message):
    if ticket_key:
        mp.function(startrek.get_client)
    else:
        response = mock_response({}, status_code=http.client.NOT_FOUND)
        mock_startrek = mp.function(startrek.get_client)
        mock_startrek().get_issue.side_effect = StartrekRequestError(response, "mock-error")
    walle_test.mock_project({"id": "test-2"})

    with pytest.raises(RequestValidationError) as e:
        validate_scenario_params(dict(ticket_key=ticket_key, labels=labels))
    assert str(e.value) == error_message


def test_validate_scenario_params_successfully(mp, walle_test):
    ticket_key, target_project_id, timeout, labels = "TEST-1", "test-1", 1, {1: 1}

    mp.function(startrek.get_client)
    walle_test.mock_project({"id": target_project_id})

    try:
        validate_scenario_params(
            dict(
                ticket_key=ticket_key,
                labels=labels,
                script_args=dict(target_project_id=target_project_id, timeout=timeout),
            )
        )
    except RequestValidationError:
        assert False
