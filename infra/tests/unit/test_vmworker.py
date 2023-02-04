import uuid

import mock
import pytest
import os
import tempfile

from infra.vmagent.src.vmagent import process
from infra.vmagent.src.vmagent_pb import vmagent_pb2
from infra.vmagent.src.vmagent_pb import vmagent_api_pb2


def test_process():
    class TestError(BaseException):
        pass

    worker = process.VMWorker(mode='yp')
    worker._handle_config = mock.Mock(side_effect=TestError)
    worker._check_qemu = mock.Mock()
    worker._set_state = mock.Mock()

    # Push task first
    task = vmagent_api_pb2.VMActionRequest()
    task.action = vmagent_api_pb2.VMActionRequest.PUSH_CONFIG
    assert worker.push_task(task) is None
    assert worker.push_task(task) is not None  # overcommit is ok, task just will be dropped

    # Then get it from queue and run command
    with mock.patch('infra.vmagent.src.vmagent.helpers.get_qemu_container', mock.Mock()):
        with pytest.raises(TestError):
            worker.process()

    worker._handle_config.assert_called_once()


def prepare_task():
    task = vmagent_api_pb2.VMActionRequest()
    task.action = vmagent_api_pb2.VMActionRequest.PUSH_CONFIG
    task.timeout = 1
    task.config.disk.resource.rb_torrent = 'rbtorrent:{}'.format(str(uuid.uuid4()))
    return task


def test_handle_config(tmpdir):
    tmpdir_path = str(tmpdir)
    image_path = os.path.join(tmpdir_path, 'image')
    image_folder_path = os.path.join(tmpdir_path, 'image_folder')
    worker = process.VMWorker()
    worker._revert_qemu = mock.Mock()

    def store_config(config):
        worker._config = config

    def set_state(state):
        worker._state.CopyFrom(state)

    def get_resource(rbtorrent, path):
        tempfile.mkstemp(dir=image_folder_path)

    size_for_alloc = 0
    size_for_image = 0
    size_for_virtual = 0

    def storage_size(path):
        return size_for_alloc

    def virtual_size(path):
        return size_for_virtual

    def image_size(path):
        return size_for_image

    worker._store_config = mock.Mock(side_effect=store_config)
    worker._set_state = mock.Mock(side_effect=set_state)
    patchers = [
        mock.patch('infra.vmagent.src.vmagent.config.STORAGE_PATH', tmpdir_path),
        mock.patch('infra.vmagent.src.vmagent.config.IMAGE_PATH', image_path),
        mock.patch('infra.vmagent.src.vmagent.config.IMAGE_FOLDER_PATH', image_folder_path),
        mock.patch('infra.vmagent.src.vmagent.helpers.get_resource', mock.Mock(
            side_effect=get_resource
        )),
        mock.patch('infra.vmagent.src.vmagent.helpers.get_virtual_size', virtual_size),
        mock.patch('infra.vmagent.src.vmagent.helpers.get_storage_size', storage_size),
        mock.patch('infra.vmagent.src.vmagent.helpers.get_image_size', image_size),
    ]
    for p in patchers:
        p.start()

    giga = 1024**3
    # successful configured
    size_for_alloc = 4 * giga
    size_for_image = 1 * giga
    size_for_virtual = 1 * giga
    worker._handle_cmd(prepare_task())
    assert worker._config.id
    assert worker._state.type == vmagent_pb2.VMState.CONFIGURED

    # allocated_size(3g) - image_size(2g) - 1g = 0g < virtual_size(2g)
    size_for_image = 2 * giga
    size_for_alloc = 3 * giga
    size_for_virtual = 2 * giga
    worker._handle_cmd(prepare_task())
    assert worker._state.type == vmagent_pb2.VMState.INVALID

    # successful configured
    size_for_alloc = 4 * giga
    size_for_image = 1 * giga
    size_for_virtual = 1 * giga
    worker._handle_cmd(prepare_task())
    assert worker._config.id
    assert worker._state.type == vmagent_pb2.VMState.CONFIGURED

    for p in patchers:
        p.stop()


def handle_command(worker):
    vnc_pass = '1'

    def set_state(state):
        worker._state.CopyFrom(state)

    worker._set_state = mock.Mock(side_effect=set_state)
    qemu_container = mock.Mock()
    mock_qemu_mon = mock.Mock()
    mock_get_qemu_container = mock.Mock(return_value=qemu_container)
    mock_drop_delta = mock.Mock()
    patchers = [
        mock.patch('infra.vmagent.src.vmagent.helpers.qemu_mon', mock_qemu_mon),
        mock.patch('infra.vmagent.src.vmagent.helpers.get_qemu_container', mock_get_qemu_container),
        mock.patch('infra.vmagent.src.vmagent.helpers.drop_delta', mock_drop_delta)
    ]
    for p in patchers:
        p.start()

    task = vmagent_api_pb2.VMActionRequest()
    task.timeout = 1

    # Start
    qemu_container.reset_mock()
    worker._qemu = None
    worker._state.type = vmagent_pb2.VMState.CONFIGURED
    task.action = vmagent_api_pb2.VMActionRequest.START
    worker._handle_cmd(task)
    assert worker._qemu
    worker._qemu.Start.assert_called_once()
    mock_qemu_mon.assert_called_with("set_password vnc {}".format(vnc_pass))
    assert worker._state.type == vmagent_pb2.VMState.RUNNING

    # Exception while starting
    qemu_container.reset_mock()
    worker._qemu = None
    qemu_container.Start.side_effect = Exception('crash')
    qemu_container.GetProperty.return_value = 'stderr'
    worker._state.type = vmagent_pb2.VMState.CONFIGURED
    task.action = vmagent_api_pb2.VMActionRequest.START
    worker._handle_cmd(task)
    qemu_container.Start.assert_called_once()
    qemu_container.Start.side_effect = None
    assert worker._qemu is None
    assert worker._state.type == vmagent_pb2.VMState.CRASHED
    assert worker._state.info == 'Failed to start qemu: crash, stderr: stderr'

    # Shutdown
    worker._qemu = qemu_container
    worker._state.type = vmagent_pb2.VMState.RUNNING
    task.action = vmagent_api_pb2.VMActionRequest.SHUTDOWN
    worker._handle_cmd(task)
    mock_qemu_mon.assert_called_with('system_powerdown')
    # status will be changed on next worker iteration
    assert worker._state.type == vmagent_pb2.VMState.RUNNING

    # Reset
    worker._qemu = qemu_container
    worker._state.type = vmagent_pb2.VMState.RUNNING
    task.action = vmagent_api_pb2.VMActionRequest.RESET
    worker._handle_cmd(task)
    mock_qemu_mon.assert_called_with('system_reset')
    assert worker._state.type == vmagent_pb2.VMState.RUNNING

    # Poweroff
    worker._qemu = qemu_container
    worker._state.type = vmagent_pb2.VMState.RUNNING
    # imitate correct shutdown
    worker._qemu.GetProperty.return_value = '0'
    worker._qemu.Wait.return_value = 'qemu'
    task.action = vmagent_api_pb2.VMActionRequest.POWEROFF
    worker._handle_cmd(task)
    mock_qemu_mon.assert_called_with('quit')
    assert worker._state.type == vmagent_pb2.VMState.STOPPED

    # Poweroff with kill 9
    worker._qemu = qemu_container
    worker._state.type = vmagent_pb2.VMState.RUNNING
    # imitate correct shutdown
    worker._qemu.GetProperty.return_value = None
    worker._qemu.Wait.return_value = 'qemu'
    task.action = vmagent_api_pb2.VMActionRequest.POWEROFF
    worker._handle_cmd(task)
    mock_qemu_mon.assert_called_with('quit')
    assert worker._state.type == vmagent_pb2.VMState.CRASHED
    assert worker._state.info == 'Qemu exited with code: 9'

    # Hard reset
    worker._qemu = qemu_container
    worker._state.type = vmagent_pb2.VMState.RUNNING
    task.action = vmagent_api_pb2.VMActionRequest.HARD_RESET
    if worker._config.disk.type == vmagent_pb2.VMDisk.RAW:
        def configured(config, timeout):
            worker._state.type = vmagent_pb2.VMState.CONFIGURED
        new_patch = mock.patch("infra.vmagent.src.vmagent.process.VMWorker._handle_config",
                               mock.Mock(side_effect=configured))
        new_patch.start()
        worker._handle_cmd(task)
        mock_qemu_mon.assert_called_with('quit')
        new_patch.stop()
    else:
        worker._handle_cmd(task)
        mock_qemu_mon.assert_called_with('quit')
        mock_drop_delta.assert_called_once()
    assert worker._state.type == vmagent_pb2.VMState.CONFIGURED

    # Rescue
    qemu_container.reset_mock()
    worker._qemu = None
    worker._state.type = vmagent_pb2.VMState.CONFIGURED
    task.action = vmagent_api_pb2.VMActionRequest.RESCUE
    worker._handle_cmd(task)
    assert worker._qemu
    worker._qemu.Start.assert_called_once()
    mock_qemu_mon.assert_called_with("set_password vnc {}".format(vnc_pass))
    assert worker._state.type == vmagent_pb2.VMState.RUNNING

    # Fail unsupportable yet
    worker._state.type = vmagent_pb2.VMState.RUNNING
    task.action = vmagent_api_pb2.VMActionRequest.SNAPSHOT
    worker._handle_cmd(task)
    assert worker._state.type == vmagent_pb2.VMState.RUNNING

    for p in patchers:
        p.stop()


def test_handle_command(tmpdir):
    tmpdir_path = str(tmpdir)
    os.makedirs(os.path.join(tmpdir_path, 'image'))
    p = mock.patch('infra.vmagent.src.vmagent.config.IMAGE_PATH', os.path.join(tmpdir_path, 'image'))
    p.start()
    vnc_pass = '1'
    for image_type in (vmagent_pb2.VMDisk.DELTA, vmagent_pb2.VMDisk.RAW):
        worker = process.VMWorker()
        worker._config.access_info.vnc_password = vnc_pass
        worker._config.id = str(uuid.uuid4())
        worker._config.disk.type = image_type
        handle_command(worker)
    p.stop()


def test_worker_init(tmpdir):
    """
    Poor man's functional testing of some VMWorker.init behaviours
    """
    worker = process.VMWorker(mode='yp')
    last_config = None
    payload_config = None
    last_state = None
    image_path = os.path.join(str(tmpdir), 'image')

    def get_last_config():
        return last_config

    def get_payload_config():
        return payload_config

    def store_config(config):
        worker._config = config

    def set_state(state):
        worker._state = state

    def replace_disk_image(config):
        with open(image_path, 'w'):
            pass

    def get_init_state():
        if last_state.type in (vmagent_pb2.VMState.RUNNING, vmagent_pb2.VMState.EMPTY):
            return vmagent_pb2.VMState(type=vmagent_pb2.VMState.CRASHED)
        else:
            return last_state

    worker._get_last_config = mock.Mock(side_effect=get_last_config)
    worker._get_payload_config = mock.Mock(side_effect=get_payload_config)
    worker._store_config = mock.Mock(side_effect=store_config)
    worker._set_state = mock.Mock(side_effect=set_state)
    worker._replace_disk_image = mock.Mock(side_effect=replace_disk_image)
    worker._get_init_state = mock.Mock(side_effect=get_init_state)
    worker._check_qemu = mock.Mock()
    patchers = [
        mock.patch('infra.vmagent.src.vmagent.config.IMAGE_PATH', image_path),
        mock.patch('infra.vmagent.src.vmagent.helpers.get_qemu_container', mock.Mock()),
    ]
    for p in patchers:
        p.start()

    def reset_worker():
        worker._config = vmagent_pb2.VMConfig()
        worker._state = vmagent_pb2.VMState()
        worker._replace_disk_image.reset_mock()

    # Case 1: no config at all (seems like disorder)
    last_config = None
    payload_config = None
    worker.init()
    assert worker._state.type == vmagent_pb2.VMState.EMPTY
    # Case 2: create operation, only default config present
    reset_worker()
    payload_config = vmagent_pb2.VMConfig()
    payload_config.id = '1'
    payload_config.disk.resource.rb_torrent = 'rbtorrent:fake'
    worker.init()
    assert worker._state.type == vmagent_pb2.VMState.CONFIGURED
    assert worker._config.id == payload_config.id
    worker._replace_disk_image.assert_called_once_with(payload_config)
    # Case 3: config, change disk image
    reset_worker()
    last_config = vmagent_pb2.VMConfig()
    last_config.id = '1'
    last_config.disk.resource.rb_torrent = 'rbtorrent:fake'
    payload_config = vmagent_pb2.VMConfig()
    payload_config.id = '2'
    payload_config.disk.resource.rb_torrent = 'rbtorrent:fake1'
    worker.init()
    assert worker._state.type == vmagent_pb2.VMState.CONFIGURED
    assert worker._config.id == payload_config.id
    worker._replace_disk_image.assert_called_once_with(payload_config)
    # Case 4: config, disk image has not been changed
    reset_worker()
    last_config = vmagent_pb2.VMConfig()
    last_config.id = '2'
    last_config.disk.resource.rb_torrent = 'rbtorrent:fake1'
    payload_config = vmagent_pb2.VMConfig()
    payload_config.id = '3'
    payload_config.disk.resource.rb_torrent = 'rbtorrent:fake1'
    worker.init()
    assert worker._state.type == vmagent_pb2.VMState.CONFIGURED
    assert worker._config.id == payload_config.id
    worker._replace_disk_image.assert_not_called()
    # Case 5: config action failed, retry
    reset_worker()
    os.unlink(image_path)
    last_config = vmagent_pb2.VMConfig()
    last_config.id = '3'
    last_config.disk.resource.rb_torrent = 'rbtorrent:fake1'
    payload_config = vmagent_pb2.VMConfig()
    payload_config.CopyFrom(last_config)
    worker.init()
    assert worker._state.type == vmagent_pb2.VMState.CONFIGURED
    assert worker._config.id == last_config.id
    worker._replace_disk_image.assert_called_once_with(last_config)
    # Case 6: worker restart, no need for config
    reset_worker()
    last_state = vmagent_pb2.VMState(type=vmagent_pb2.VMState.RUNNING)
    last_config = vmagent_pb2.VMConfig()
    last_config.id = '3'
    last_config.disk.resource.rb_torrent = 'rbtorrent:fake1'
    payload_config = vmagent_pb2.VMConfig()
    payload_config.CopyFrom(last_config)
    worker.init()
    assert worker._state.type == vmagent_pb2.VMState.CRASHED
    assert worker._config.id == last_config.id
    worker._replace_disk_image.assert_not_called()
    worker._check_qemu.assert_called_once()

    for p in patchers:
        p.stop()
