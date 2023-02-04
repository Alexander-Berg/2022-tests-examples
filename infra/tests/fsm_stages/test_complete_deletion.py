"""Test complete deletion stage of a host deletion task."""
import pytest

from infra.walle.server.tests.lib.util import (
    TestCase,
    check_stage_initialization,
    mock_task,
    handle_host,
    mock_task_cancellation,
    mock_commit_stage_changes,
)
from sepelib.core.exceptions import Error
from walle.fsm_stages import complete_deletion, common as fsm_common
from walle.hosts import HostState
from walle.network import BlockedHostName
from walle.operations_log.constants import Operation
from walle.stages import Stages, Stage


@pytest.fixture()
def test(request, monkeypatch_timestamp, monkeypatch_audit_log):
    return TestCase.create(request)


def test_stage_initialization(test):
    check_stage_initialization(test, Stage(name=Stages.COMPLETE_DELETION))


@pytest.mark.parametrize("state", HostState.ALL)
@pytest.mark.parametrize("transition", [True, False])
def test_block_host_name(mp, test, state, transition):
    mock_block_hostname = mp.method(BlockedHostName.store, BlockedHostName)

    host = test.mock_host(
        {
            "state": state,
            "status": Operation.DELETE.host_status,
            "task": mock_task(
                stage=Stages.COMPLETE_DELETION,
                stage_status=None if transition else complete_deletion._STATUS_BLOCKING_HOST_NAME,
            ),
        }
    )

    handle_host(host)

    mock_commit_stage_changes(host, status=complete_deletion._STATUS_DELETING_HOST, check_now=True)

    mock_block_hostname.assert_called_once_with(BlockedHostName, host.name)
    test.hosts.assert_equal()


@pytest.mark.parametrize("state", HostState.ALL)
def test_complete(test, state):
    host = test.mock_host(
        {
            "state": state,
            "status": Operation.DELETE.host_status,
            "task": mock_task(stage=Stages.COMPLETE_DELETION, stage_status=complete_deletion._STATUS_DELETING_HOST),
        },
        add=False,
    )

    # I'm gonna make this pencil disappear.
    handle_host(host)
    test.hosts.assert_equal()


@pytest.mark.parametrize("state", HostState.ALL)
def test_deletion_canceled_race(test, mp, state):
    host = test.mock_host(
        {"state": state, "status": Operation.DELETE.host_status, "task": mock_task(stage=Stages.COMPLETE_DELETION)}
    )

    def simulate_race(fsm_host):
        db_host = fsm_host.copy()
        mock_task_cancellation(db_host)
        db_host.save()
        complete_deletion._handle_delete_host(fsm_host)

    mock_stage_config = fsm_common._StageConfig(simulate_race, None, None, None)
    mp.setitem(fsm_common._STAGES, Stages.COMPLETE_DELETION, mock_stage_config)

    with pytest.raises(Error) as exc:
        handle_host(host)
    assert "Unable to delete host: it's state have changed." == str(exc.value)

    mock_task_cancellation(host)
    test.hosts.assert_equal()
