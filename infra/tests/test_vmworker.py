import mock
import freezegun
import time

from infra.qyp.proto_lib import vmagent_pb2
from infra.qyp.vmagent.src import process


def test_get_data_transfer_state_empty():
    vm_worker = process.VMWorker(
        context=mock.Mock(), qemu_ctl=mock.Mock(), qemu_launcher=mock.Mock(),
        resource_manager=mock.Mock(), qemu_img_cmd=mock.Mock,
    )

    def _compare_emty(state):
        assert not state.is_in_progress
        assert state.operation == vmagent_pb2.DataTransferState.OperationType.NONE
        assert state.progress == 0

    # case 1: dead worker
    _compare_emty(vm_worker.get_data_transfer_state())

    # case 2: empty action
    with mock.patch('infra.qyp.vmagent.src.process.VMWorker.is_alive', return_value=True):
        _compare_emty(vm_worker.get_data_transfer_state())

    # case 3: test action
    with mock.patch('infra.qyp.vmagent.src.process.VMWorker.is_alive', return_value=True):
        vm_worker._state.actions.append('test_action')
        _compare_emty(vm_worker.get_data_transfer_state())

    # case 4: action and state
    with mock.patch('infra.qyp.vmagent.src.process.VMWorker.is_alive', return_value=True):
        vm_worker._state.actions.append('config')
        vm_worker._state._state_pb.type = vmagent_pb2.VMState.RUNNING
        _compare_emty(vm_worker.get_data_transfer_state())


@freezegun.freeze_time()
def test_get_data_transfer_state():
    res_manager = mock.Mock()
    vm_worker = process.VMWorker(
        context=mock.Mock(), qemu_ctl=mock.Mock(), qemu_launcher=mock.Mock(),
        resource_manager=res_manager, qemu_img_cmd=mock.Mock,
    )

    # case 1: upload
    with mock.patch('infra.qyp.vmagent.src.process.VMWorker.is_alive', return_value=True):
        res_manager.get_backup_status.return_value = {'progress': 50, 'total_bytes': 1000, 'done_bytes': 500}
        vm_worker._state.actions.append('qdm_upload')
        vm_worker._state._state_pb.type = vmagent_pb2.VMState.BUSY
        state = vm_worker.get_data_transfer_state()
        assert state.is_in_progress
        assert state.operation == vmagent_pb2.DataTransferState.OperationType.UPLOAD
        assert state.progress == 50
        assert state.total_bytes == 1000
        assert state.done_bytes == 500
        assert state.eta == 0

    # case 2: upload
    with mock.patch('infra.qyp.vmagent.src.process.VMWorker.is_alive', return_value=True):
        res_manager.get_backup_status.return_value = {
            'progress': 50, 'total_bytes': 10, 'done_bytes': 5, 'start_time': time.time() - 5, 'duration': 100
        }
        vm_worker._state.actions.append('qdm_upload')
        vm_worker._state._state_pb.type = vmagent_pb2.VMState.BUSY
        state = vm_worker.get_data_transfer_state()
        assert state.is_in_progress
        assert state.operation == vmagent_pb2.DataTransferState.OperationType.UPLOAD
        assert state.progress == 50
        assert state.total_bytes == 10
        assert state.done_bytes == 5
        assert state.eta == 95

    # case 3: download
    with mock.patch('infra.qyp.vmagent.src.process.VMWorker.is_alive', return_value=True):
        res_manager.get_backup_status.return_value = {'progress': 75, 'total_bytes': 1000, 'done_bytes': 750}
        vm_worker._state.actions.append('config')
        vm_worker._state._state_pb.type = vmagent_pb2.VMState.PREPARING
        state = vm_worker.get_data_transfer_state()
        assert state.is_in_progress
        assert state.operation == vmagent_pb2.DataTransferState.OperationType.DOWNLOAD
        assert state.progress == 75
        assert state.total_bytes == 1000
        assert state.done_bytes == 750
