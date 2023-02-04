import mock
import pytest

from balance import exc
from balance.actions.nirvana.block import nirvana_block_processor
from balance.actions.nirvana import operations
from balance.actions.nirvana.status import NirvanaBlockStatus
from tests import object_builder as ob


@pytest.fixture()
def process_mock():
    with mock.patch('balance.actions.nirvana.operations.process') as m:
        yield m


@pytest.fixture()
def push_status_to_nirvana_mock():
    with mock.patch('balance.actions.nirvana.block.push_status_to_nirvana') as m:
        yield m


def test_running_status_raises_deferred_error(session, process_mock, nirvana_block, export):
    process_mock.return_value = NirvanaBlockStatus.running(2)
    with pytest.raises(exc.DEFERRED_ERROR):
        nirvana_block_processor(nirvana_block)


def test_waiting_status_raises_deferred_error(session, process_mock, nirvana_block, export):
    process_mock.return_value = NirvanaBlockStatus.waiting(2)
    with pytest.raises(exc.DEFERRED_ERROR):
        nirvana_block_processor(nirvana_block)


def test_finished_status_does_not_raise_deferred_error(session, process_mock, nirvana_block, export):
    process_mock.return_value = NirvanaBlockStatus.finished()
    nirvana_block_processor(nirvana_block)


def test_status_is_running_after_first_error(session, process_mock, nirvana_block, export):
    process_mock.side_effect = ValueError()
    with pytest.raises(ValueError):
        nirvana_block_processor(nirvana_block)
    assert nirvana_block.status == nirvana_block.Status.RUNNING


def test_failed_because_of_errors_rate(session, process_mock, nirvana_block, export):
    export.rate = export.max_rate - 1
    process_mock.side_effect = ValueError()
    with pytest.raises(ValueError):
        nirvana_block_processor(nirvana_block)
    assert nirvana_block.status == nirvana_block.Status.FAILED


def test_failed_because_of_critical_error(session, process_mock, nirvana_block, export):
    process_mock.side_effect = exc.CRITICAL_ERROR()
    with pytest.raises(exc.CRITICAL_ERROR):
        nirvana_block_processor(nirvana_block)
    assert nirvana_block.status == nirvana_block.Status.FAILED


def test_terminate_1_sets_status_to_cancelled(session, process_mock, nirvana_block, export):
    nirvana_block.terminate = 1
    session.flush()

    nirvana_block_processor(nirvana_block)

    assert nirvana_block.status == nirvana_block.Status.CANCELLED
    process_mock.assert_not_called()


@pytest.mark.parametrize('process_status', [
    NirvanaBlockStatus.running(2),
    NirvanaBlockStatus.waiting(3),
    NirvanaBlockStatus.finished(),
])
def test_any_status_pushes_status_to_nirvana(
        session, process_mock, push_status_to_nirvana_mock, nirvana_block, process_status,
):
    process_mock.return_value = process_status
    try:
        nirvana_block_processor(nirvana_block)
    except exc.DEFERRED_ERROR:
        pass
    push_status_to_nirvana_mock.assert_called_once_with(nirvana_block)


def test_terminated_block_pushes_status_to_nirvana(
        session, push_status_to_nirvana_mock, nirvana_block,
):
    nirvana_block.terminate = 1
    session.flush()
    nirvana_block_processor(nirvana_block)
    push_status_to_nirvana_mock.assert_called_once_with(nirvana_block)


def test_universal_operation(session, push_status_to_nirvana_mock):
    nirvana_block = ob.NirvanaBlockBuilder.construct(
        session, operation='universal',
        request={
            'data': {'options': {'opcode': 'test_universal_operation'}},
            'operation': {'logs': {}}
        }
    )
    op_mock = mock.Mock(
        return_value=NirvanaBlockStatus.finished()
    )
    operations.TEST_OP_MAP['test_universal_operation'] = op_mock
    nirvana_block_processor(nirvana_block)
    op_mock.assert_called_once_with(nirvana_block)


def test_universal_opcode(session):
    nirvana_block = ob.NirvanaBlockBuilder.construct(
        session, operation='universal',
        request={
            'data': {'options': {'opcode': 'universal'}},
            'operation': {'logs': {}}
        }
    )

    with pytest.raises(AssertionError):
        nirvana_block.enqueue('NIRVANA_BLOCK')
        nirvana_block_processor(nirvana_block)


def test_pid_save(session, nirvana_block, process_mock):
    process_mock.return_value = NirvanaBlockStatus.finished()

    with mock.patch('os.getpid') as m:
        m.return_value = 12345
        nirvana_block_processor(nirvana_block)

    session.refresh(nirvana_block)
    assert nirvana_block.pid == m.return_value


