import os
import mock
import pytest
from infra.qyp.vmagent.src import process, config, qemu_ctl
from infra.qyp.proto_lib import vmagent_pb2, vmset_pb2, qdm_pb2


@pytest.fixture
def workdir(tmpdir):
    workdir = tmpdir.mkdir('workdir')
    return workdir


@pytest.fixture
def main_storage(tmpdir):
    main_storage = tmpdir.mkdir('main_storage')
    return main_storage


@pytest.fixture
def vm_config(main_storage):
    _vm_config = vmagent_pb2.VMConfig()
    _vm_config.mem = 10
    _vm_config.vcpu = 2
    _vm_config.autorun = False
    main_volume = _vm_config.volumes.add()  # type: vmagent_pb2.VMVolume
    main_volume.name = config.VmagentContext.MAIN_QEMU_VOLUME_NAME
    main_volume.available_size = 100
    main_volume.mount_path = str(main_storage)
    main_volume.is_main = True
    main_volume.image_type = vmagent_pb2.VMVolume.DELTA
    main_volume.resource_url = 'any_url'
    return _vm_config


@pytest.fixture
def context(workdir, vm_config, main_storage):
    ctx = mock.Mock()  # type: config.VmagentContext
    ctx.VM = vmset_pb2.VM()
    ctx.VM_CONFIG = vm_config
    ctx.SSH_AUTHORIZED_KEYS = ['first_ssh_key', 'second_ssh_key']
    ctx.CLOUD_INIT_CONFIGS_FOLDER_NAME = 'cloud_init_configs'
    ctx.CLOUD_INIT_CONFIGS_FOLDER_PATH = str(workdir.mkdir(ctx.CLOUD_INIT_CONFIGS_FOLDER_NAME))
    ctx.VM_HOSTNAME = 'test.local.yandex.net'
    ctx.VM_AUX_IP = '2a02:6b8:c0a:36b6:10d:2fbc:4336:0'
    ctx.VM_IP = '2a02:6b8:c0a:36b6:10d:2fbc:4336:2'
    ctx.VM_MAC = 'test'
    ctx.TAP_DEV = 'tap7255'
    ctx.TAP_LL = 'test'
    ctx.MONITOR_PATH = str(workdir.join('mon.sock'))
    ctx.VNC_SOCKET_FILE_PATH = str(workdir.join('vnc.sock'))
    ctx.SERIAL_LOG_FILE_PATH = str(workdir.join('serial.log'))
    ctx.QEMU_LAUNCHER_FILE_NAME = 'qemu_launcher.sh'
    ctx.QEMU_LAUNCHER_FILE_PATH = str(workdir.join(ctx.QEMU_LAUNCHER_FILE_NAME))
    ctx.CURRENT_CONFIG_FILE_NAME = 'current.state'
    ctx.CURRENT_CONFIG_FILE_PATH = str(workdir.join(ctx.CURRENT_CONFIG_FILE_NAME))
    ctx.LAST_STATUS_FILE_NAME = 'last_status'
    ctx.LAST_STATUS_FILE_PATH = str(workdir.join(ctx.LAST_STATUS_FILE_NAME))
    return ctx


@pytest.fixture
def qemu_ctl_mock():
    _qemu_ctl = qemu_ctl.QemuCtl(mock.Mock(), None)
    _qemu_ctl._get_qemu_container = mock.Mock()
    _qemu_ctl._qemu_mon = mock.Mock()
    return _qemu_ctl


@pytest.fixture
def qemu_launcher_mock():
    return mock.Mock()


@pytest.fixture
def resource_manager_mock():
    return mock.Mock()


@pytest.fixture
def qemu_img_cmd_mock():
    return mock.Mock()


@pytest.fixture
def get_image_size_mock():
    with mock.patch('infra.qyp.vmagent.src.helpers.get_image_size') as _get_image_size_mock:
        yield _get_image_size_mock


@pytest.fixture(autouse=True)
def ping_vm_mock():
    with mock.patch('infra.qyp.vmagent.src.helpers.ping_vm') as _ping_vm_mock:
        yield _ping_vm_mock


@pytest.fixture(autouse=True)
def connect_vm_mock():
    with mock.patch('infra.qyp.vmagent.src.helpers.connect_vm') as _connect_vm_mock:
        yield _connect_vm_mock


@pytest.fixture
def vm_worker(context, qemu_ctl_mock, qemu_launcher_mock, resource_manager_mock, qemu_img_cmd_mock):
    return process.VMWorker(
        context=context,
        qemu_ctl=qemu_ctl_mock,
        qemu_launcher=qemu_launcher_mock,
        resource_manager=resource_manager_mock,
        qemu_img_cmd=qemu_img_cmd_mock
    )


def test_handle_config_without_prev_config(vm_worker, vm_config, qemu_ctl_mock, resource_manager_mock,
                                           qemu_img_cmd_mock, get_image_size_mock):
    qemu_ctl_mock._get_qemu_container.side_effect = qemu_ctl.QemuCtl.QemuContainerDoesNotExists()
    qemu_img_cmd_mock.create.return_value = (True, None)
    qemu_img_cmd_mock.get_virtual_disk_size.return_value = 97
    get_image_size_mock.return_value = 3

    def get_resource_sd(resid, path):
        with open(os.path.join(path, 'layer.img'), 'w') as fp:
            fp.write('1')

    resource_manager_mock.get_resource.side_effect = get_resource_sd
    vm_worker._handle_config(vm_config)
    assert resource_manager_mock.clear_data_transfer_progress.called
    assert vm_worker.get_state().type == process.CONFIGURED


def test_handle_config_without_prev_config_with_autorun(vm_worker, vm_config, qemu_ctl_mock, resource_manager_mock,
                                                        qemu_img_cmd_mock, get_image_size_mock):
    qemu_ctl_mock._get_qemu_container.side_effect = [qemu_ctl.QemuCtl.QemuContainerDoesNotExists(), None, mock.Mock()]
    qemu_img_cmd_mock.create.return_value = (True, None)
    qemu_img_cmd_mock.get_virtual_disk_size.return_value = 97
    get_image_size_mock.return_value = 3
    vm_config.autorun = True

    def get_resource_sd(resid, path):
        with open(os.path.join(path, 'layer.img'), 'w') as fp:
            fp.write('1')

    resource_manager_mock.get_resource.side_effect = get_resource_sd
    vm_worker._handle_config(vm_config)
    assert resource_manager_mock.clear_data_transfer_progress.called
    assert vm_worker.get_state().type == vmagent_pb2.VMState.RUNNING


def test_handle_config_with_same_prev_config(vm_worker, vm_config, qemu_ctl_mock, resource_manager_mock,
                                             qemu_img_cmd_mock, get_image_size_mock):
    qemu_ctl_mock._get_qemu_container.side_effect = qemu_ctl.QemuCtl.QemuContainerDoesNotExists()
    qemu_img_cmd_mock.create.return_value = (True, None)
    qemu_img_cmd_mock.get_virtual_disk_size.return_value = 97
    get_image_size_mock.return_value = 3

    def get_resource_sd(resid, path):
        with open(os.path.join(path, 'layer.img'), 'w') as fp:
            fp.write('1')

    resource_manager_mock.get_resource.side_effect = get_resource_sd
    vm_worker._store_config(vm_config)
    vm_worker._state.set(vmagent_pb2.VMState.RUNNING)

    vm_worker._handle_config(vm_config)
    assert not resource_manager_mock.clear_data_transfer_progress.called
    assert vm_worker.get_state().type == vmagent_pb2.VMState.RUNNING


def test_vm_worker_init_first_time_with_wrong_config(vm_worker, vm_config, qemu_ctl_mock):
    qemu_ctl_mock._get_qemu_container.side_effect = qemu_ctl.QemuCtl.QemuContainerDoesNotExists()
    vm_config.Clear()
    # case: empty volumes
    vm_worker.init()
    state = vm_worker.get_state()
    assert state.type == vmagent_pb2.VMState.INVALID
    assert state.info == 'Config error: Invalid volumes, is empty'

    # case: wrong main volume resource url
    main_volume = vm_config.volumes.add()  # type: vmagent_pb2.VMVolume
    main_volume.name = config.VmagentContext.MAIN_QEMU_VOLUME_NAME
    main_volume.is_main = True
    vm_worker.init()
    state = vm_worker.get_state()
    assert state.type == vmagent_pb2.VMState.INVALID
    assert state.info == 'Config error: Invalid main image resource_url'

    # case: invalid vcpu count
    vm_config.vcpu = 0
    main_volume.resource_url = 'any_url'
    vm_worker.init()
    state = vm_worker.get_state()
    assert state.type == vmagent_pb2.VMState.INVALID
    assert state.info == 'Config error: Invalid vcpu count'

    # case: invalid memory
    vm_config.vcpu = 2
    vm_config.mem = 0
    vm_worker.init()
    state = vm_worker.get_state()
    assert state.type == vmagent_pb2.VMState.INVALID
    assert state.info == 'Config error: Invalid mem demand'


def test_init_first_time_with_valid_config(vm_worker, vm_config, qemu_ctl_mock, tmpdir):
    qemu_ctl_mock._get_qemu_container.side_effect = qemu_ctl.QemuCtl.QemuContainerDoesNotExists()

    vm_worker.init()
    state = vm_worker.get_state()
    assert state.type == vmagent_pb2.VMState.EMPTY
    assert state.info == ''
    assert not vm_worker._queue.empty()


def test_handle_qdm_upload(vm_worker, vm_config, resource_manager_mock, qemu_ctl_mock, tmpdir):
    qemu_ctl_mock._get_qemu_container.side_effect = qemu_ctl.QemuCtl.QemuContainerDoesNotExists()
    resource_manager_mock.upload_resource.return_value = 'qdm:testing'
    qdm_req = qdm_pb2.QDMCliUploadRequest()
    qdm_req.key = 'test'
    extra_volume = vm_config.volumes.add()  # type: vmagent_pb2.VMVolume
    extra_volume.name = 'test-raw'
    extra_volume.mount_path = str(tmpdir.join('extra_volumes').join(extra_volume.name))
    extra_volume.resource_url = 'rbtorrent:test'
    extra_volume.order = len(vm_config.volumes) - 1
    extra_volume.image_type = vmagent_pb2.VMVolume.RAW

    vm_worker._store_config(vm_config)
    vm_worker._handle_qdm_upload(qdm_req)
    expected_qdm_spec = qdm_pb2.QDMBackupSpec()

    expected_qdm_spec.qdm_spec_version = 1
    expected_qdm_spec.vmspec.CopyFrom(vmset_pb2.VM())

    expected_file = expected_qdm_spec.filemap.add()  # type: qdm_pb2.QDMBackupFileSpec
    expected_file.source = os.path.join(vm_config.volumes[0].mount_path, 'image')
    expected_file.path = 'qemu-persistent/layer.img'
    expected_file.meta.volume_index = 0
    expected_file.meta.volume_name = vm_config.volumes[0].name

    expected_file = expected_qdm_spec.filemap.add()  # type: qdm_pb2.QDMBackupFileSpec
    expected_file.source = os.path.join(vm_config.volumes[0].mount_path, 'current.qcow2')
    expected_file.path = 'qemu-persistent/current.qcow2'
    expected_file.meta.volume_index = 0
    expected_file.meta.volume_name = vm_config.volumes[0].name

    expected_file = expected_qdm_spec.filemap.add()  # type: qdm_pb2.QDMBackupFileSpec
    expected_file.source = os.path.join(vm_config.volumes[1].mount_path, 'image')
    expected_file.path = 'test-raw/layer.img'
    expected_file.meta.volume_index = 1
    expected_file.meta.volume_name = vm_config.volumes[1].name

    assert resource_manager_mock.clear_data_transfer_progress.called
    resource_manager_mock.upload_resource.assert_called_with(qdm_req.key, expected_qdm_spec)

    current_state = vm_worker.get_state()

    assert current_state.type == vmagent_pb2.VMState.STOPPED
    assert current_state.backup.image_url == 'qdm:testing'


def test_share_image(vm_worker, vm_config, resource_manager_mock, qemu_ctl_mock, tmpdir):
    qemu_ctl_mock._get_qemu_container.side_effect = qemu_ctl.QemuCtl.QemuContainerDoesNotExists()
    resource_manager_mock.share_resource.return_value = 'qdm:testing'
    qdm_req = qdm_pb2.QDMCliUploadRequest()
    qdm_req.key = 'test'
    vm_worker._store_config(vm_config)
    vm_worker._share_image()
    resource_manager_mock.share_resource.assert_called_with(os.path.join(vm_config.volumes[0].mount_path, 'image'))
    current_state = vm_worker.get_state()
    assert current_state.type == vmagent_pb2.VMState.STOPPED
    assert current_state.shared_image.image_url == 'qdm:testing'
