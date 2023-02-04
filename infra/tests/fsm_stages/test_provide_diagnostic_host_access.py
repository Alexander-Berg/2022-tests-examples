"""Tests for provide diagnostic host access stage."""
import pytest

from infra.walle.server.tests.lib.util import (
    check_stage_initialization,
    any_task_status,
    mock_task,
    handle_host,
    mock_complete_current_stage,
)
from walle.clients import staff, idm
from walle.hosts import HostState
from walle.stages import Stage, Stages

GROUP1 = "@group1"
GROUP2 = "@group2"
GROUP_TO_ID = {
    GROUP1: 1,
    GROUP2: 2,
}
ROLE_TTL = 10


@pytest.fixture(autouse=True)
def mock_groups(mp):
    mp.config("host_diagnostics_access.groups", [GROUP1, GROUP2])
    mp.config("host_diagnostics_access.ttl_days", ROLE_TTL)
    mp.function(staff.groups_to_ids, side_effect=lambda gs: GROUP_TO_ID)


@pytest.fixture()
def host_mock(walle_test):
    host = walle_test.mock_host(
        {
            "inv": 1,
            "name": "some-host.y-t.net",
            "state": HostState.ASSIGNED,
            "status": any_task_status(),
            "task": mock_task(stage=Stages.PROVIDE_DIAGNOSTIC_HOST_ACCESS),
        }
    )
    return host


def test_stage_initialization(walle_test):
    check_stage_initialization(walle_test, Stage(name=Stages.PROVIDE_DIAGNOSTIC_HOST_ACCESS))


def test_stage_completes_on_successful_request(walle_test, mp, host_mock, batch_request_execute_mock):
    handle_host(host_mock)

    assert batch_request_execute_mock.called
    subrequests = batch_request_execute_mock.call_args[0][0]._subrequests

    expected_br = idm.BatchRequest()
    for group in [GROUP1, GROUP2]:
        common_params = {"system": "cauth", "deprive_after_days": ROLE_TTL, "group": group}
        expected_br.request_role(
            path=["dst", host_mock.name, "role", "ssh"], fields_data={"root": False}, **common_params
        )
        expected_br.request_role(
            path=["dst", host_mock.name, "role", "sudo"], fields_data={'role': 'ALL=(ALL) ALL'}, **common_params
        )
    assert subrequests == expected_br._subrequests

    mock_complete_current_stage(host_mock)
    walle_test.hosts.assert_equal()
