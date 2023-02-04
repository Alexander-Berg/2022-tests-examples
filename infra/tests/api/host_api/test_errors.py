import pytest

from infra.walle.server.tests.lib.util import TestCase
from walle.errors import InvalidHostStateError
from walle.hosts import HostState, HostStatus


@pytest.fixture
def test(monkeypatch_timestamp, monkeypatch_audit_log, request):
    return TestCase.create(request)


def test_string_arg(test):
    host = test.mock_host({"state": HostState.FREE, "status": HostStatus.READY})
    allowed_states = HostState.MAINTENANCE
    allowed_statuses = HostStatus.ALL_STEADY

    with_str_arg = InvalidHostStateError(host, allowed_states=allowed_states, allowed_statuses=allowed_statuses)
    without_str_arg = InvalidHostStateError(host, allowed_states=[allowed_states], allowed_statuses=allowed_statuses)

    assert str(with_str_arg) == str(without_str_arg)


@pytest.mark.parametrize(
    "state, status", ((HostState.ASSIGNED, HostStatus.READY), (HostState.FREE, HostStatus.INVALID))
)
@pytest.mark.parametrize("allowed_states", [[HostState.FREE], [HostState.MAINTENANCE], HostState.ALL])
@pytest.mark.parametrize(
    "allowed_statuses, forbidden_statuses",
    ((None, [HostStatus.DEAD, HostStatus.INVALID]), (HostStatus.ALL_STEADY, None)),
)
def test_invalid_host_state_error(test, state, status, allowed_states, allowed_statuses, forbidden_statuses):
    host = test.mock_host({"inv": 0, "state": state, "status": status})

    expected_message = [
        "The host has an invalid state for this operation.\n"
        "Host's state: {state}.\n"
        "Host's status: {status}.".format(state=host.state, status=host.status)
    ]

    if allowed_states is not None:
        expected_message.append("Allowed states: {states}.".format(states=", ".join(allowed_states)))

    if allowed_statuses is not None:
        expected_message.append("Allowed statuses: {statuses}.".format(statuses=", ".join(allowed_statuses)))

    if forbidden_statuses is not None:
        expected_message.append("Forbidden statuses: {statuses}.".format(statuses=", ".join(forbidden_statuses)))

    if (
        allowed_statuses
        and host.status not in allowed_statuses
        or forbidden_statuses
        and host.status in forbidden_statuses
    ):
        expected_message.append("You can try to change host's status to allowed one.")

    if host.status == HostStatus.INVALID:
        expected_message.append(
            "For next actions you can look here: https://docs.yandex-team.ru/wall-e/faq#faq-invalid."
        )

    error_message = InvalidHostStateError(
        host, allowed_states=allowed_states, allowed_statuses=allowed_statuses, forbidden_statuses=forbidden_statuses
    )
    assert str(error_message) == "\n".join(expected_message)
