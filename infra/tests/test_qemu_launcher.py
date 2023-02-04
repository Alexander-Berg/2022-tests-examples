import os
import yaml
import mock
import pytest
from infra.qyp.vmagent.src import qemu_launcher
from infra.qyp.proto_lib import vmagent_pb2


@pytest.fixture
def regenerate_expected_results():
    return False


@pytest.fixture(autouse=True)
def uuid_mock():
    with mock.patch('uuid.uuid4', return_value='5f4f4a7d-139e-4f79-b763-1f3481d4f0b5') as m:
        yield m


@pytest.fixture
def compare_or_update_content(regenerate_expected_results):
    def update_content(source_content, target_file_path):
        with open(target_file_path, 'w') as exp_fp:
            exp_fp.write(source_content)

    def compare_content(source_content, target_file_path):
        with open(target_file_path) as exp_fp:
            assert source_content.strip() == exp_fp.read().strip()

    return update_content if regenerate_expected_results else compare_content


@pytest.fixture
def context(tmpdir):
    workdir = tmpdir.mkdir('workdir')

    ctx = mock.Mock()
    ctx.QEMU_LAUNCHER_FILE_PATH = str(workdir.join('qemu_launcher.sh'))
    ctx.SSH_AUTHORIZED_KEYS = ['first_ssh_key', 'second_ssh_key']
    ctx.CLOUD_INIT_CONFIGS_FOLDER_NAME = 'cloud_init_configs'
    ctx.CLOUD_INIT_CONFIGS_FOLDER_PATH = str(tmpdir.mkdir(ctx.CLOUD_INIT_CONFIGS_FOLDER_NAME))
    ctx.QEMU_LAUNCHER_FILE_NAME = 'qemu_launcher.sh'
    ctx.QEMU_LAUNCHER_FILE_PATH = str(tmpdir.join(ctx.QEMU_LAUNCHER_FILE_NAME))
    ctx.VM_HOSTNAME = 'test.local.yandex.net'
    ctx.VM_AUX_IP = '2a02:6b8:c0a:36b6:10d:2fbc:4336:0'
    ctx.VM_IP = '2a02:6b8:c0a:36b6:10d:2fbc:4336:2'
    ctx.VM_MAC = 'test'
    ctx.VM.spec.qemu.qemu_options.audio = 0
    ctx.TAP_DEV = 'test'
    ctx.TAP_LL = 'test'
    ctx.MONITOR_PATH = str(workdir.join('mon.sock'))
    ctx.VNC_SOCKET_FILE_PATH = str(workdir.join('vnc.sock'))
    ctx.SERIAL_LOG_FILE_PATH = str(workdir.join('serial.log'))
    ctx.VMAGENT_PORT = 7255
    ctx.VNC_PORT = 7256
    ctx.NODE_HOSTNAME = 'sas1-1234.search.yandex.net'
    ctx.NUMA_NODES = [0, 1, 2, 3]
    ctx.VFIO_DEVICES = ['1', '2', 'vfio']
    ctx.VFIO_NUMA_MAPPING = {0: ['03:00.0'], 1: ['04:00.0']}
    ctx.USE_NUMA = False
    return ctx


@pytest.fixture
def vm_config(tmpdir):
    config = vmagent_pb2.VMConfig()
    config.mem = 12 * 1024 ** 3
    config.vcpu = 10
    config.autorun = True

    main_volume = config.volumes.add()  # type: vmagent_pb2.VMVolume
    main_volume.name = '/qemu-persistent'
    main_volume.mount_path = str(tmpdir.mkdir('qemu-persistence'))
    main_volume.available_size = 100 * 1024 ** 3
    main_volume.image_type = vmagent_pb2.VMVolume.RAW
    main_volume.resource_url = 'rbtorrent:anyurl'
    main_volume.vm_mount_path = '/'
    main_volume.is_main = True
    main_volume.order = 0

    extra_volume = config.volumes.add()  # type: vmagent_pb2.VMVolume
    extra_volume.name = 'empty'
    extra_volume.mount_path = str(tmpdir.mkdir('qemu-empty'))
    extra_volume.available_size = 100 * 1024 ** 3
    extra_volume.image_type = vmagent_pb2.VMVolume.RAW
    extra_volume.is_main = False
    extra_volume.order = 1

    extra_volume = config.volumes.add()  # type: vmagent_pb2.VMVolume
    extra_volume.name = 'resource'
    extra_volume.mount_path = str(tmpdir.mkdir('qemu-resource'))
    extra_volume.available_size = 100 * 1024 ** 3
    extra_volume.resource_url = 'rbtorrent:anyurl'
    extra_volume.image_type = vmagent_pb2.VMVolume.RAW
    extra_volume.is_main = False
    extra_volume.order = 2

    return config


def test_build_linux(context, vm_config, source_path, compare_or_update_content, tmpdir):
    qemu_launcher.QEMUSystemCmdBuilder.check_dev_kvm = mock.Mock()
    qemu_launcher.QEMUSystemCmdBuilder.check_dev_kvm.return_value = True
    vm_config.type = vmagent_pb2.VMConfig.LINUX

    _qemu_launcher = qemu_launcher.QEMULauncher()
    assert _qemu_launcher._get_tpl_names('linux_') == [
        'linux_meta-data.jinja2',
        'linux_network-config.jinja2',
        'linux_user-data.jinja2']

    _qemu_launcher.build(context, vm_config, rescue=False)

    expected_results_dir = source_path('infra/qyp/vmagent/tests/qemu_launcher_expected/linux')

    assert os.path.exists(context.CLOUD_INIT_CONFIGS_FOLDER_PATH)
    cloud_init_files = ('user-data', 'network-config', 'meta-data')

    for cloud_init_file_name in cloud_init_files:
        file_path = os.path.join(context.CLOUD_INIT_CONFIGS_FOLDER_PATH, cloud_init_file_name)
        assert os.path.exists(file_path), 'Cloud Init Config File' \
                                          ' does not exists: {}'.format(cloud_init_file_name)
        with open(file_path) as fp:
            file_content = fp.read()
            expected_file_path = os.path.join(expected_results_dir, context.CLOUD_INIT_CONFIGS_FOLDER_NAME,
                                              cloud_init_file_name)
            compare_or_update_content(file_content, expected_file_path)

            fp.seek(0)
            file_data = yaml.safe_load(fp)
            assert file_data

    expected_file_path = os.path.join(expected_results_dir, context.QEMU_LAUNCHER_FILE_NAME)
    assert os.path.exists(context.QEMU_LAUNCHER_FILE_PATH)
    with open(context.QEMU_LAUNCHER_FILE_PATH) as fp:
        file_content = fp.read()
        compare_or_update_content(file_content.replace(str(tmpdir), ''), expected_file_path)


def test_build_linux_rescue(context, vm_config, source_path, compare_or_update_content, tmpdir):
    qemu_launcher.QEMUSystemCmdBuilder.check_dev_kvm = mock.Mock()
    qemu_launcher.QEMUSystemCmdBuilder.check_dev_kvm.return_value = True

    vm_config.type = vmagent_pb2.VMConfig.LINUX
    _qemu_launcher = qemu_launcher.QEMULauncher()
    _qemu_launcher.build(context, vm_config, rescue=True)

    expected_results_dir = source_path('infra/qyp/vmagent/tests/qemu_launcher_expected/linux_rescue')

    expected_file_path = os.path.join(expected_results_dir, context.QEMU_LAUNCHER_FILE_NAME)
    assert os.path.exists(context.QEMU_LAUNCHER_FILE_PATH)
    with open(context.QEMU_LAUNCHER_FILE_PATH) as fp:
        file_content = fp.read()
        compare_or_update_content(file_content.replace(str(tmpdir), ''), expected_file_path)


def test_build_windows(context, vm_config, source_path, compare_or_update_content, tmpdir):
    qemu_launcher.QEMUSystemCmdBuilder.check_dev_kvm = mock.Mock()
    qemu_launcher.QEMUSystemCmdBuilder.check_dev_kvm.return_value = True

    vm_config.type = vmagent_pb2.VMConfig.WINDOWS

    windows_source_dir = tmpdir.mkdir('windows_source_dir')
    assert qemu_launcher.QEMULauncher.WINDOWS_READY_FILE_NAMES == [
        'CloudbaseInitSetup_0_9_11_x64.msi',
        'CloudbaseInitSetup_0_9_11_x86.msi',
        'cloudbase-init.conf',
        'cloudbase-init-unattend.conf'
    ]

    for file_name in qemu_launcher.QEMULauncher.WINDOWS_READY_FILE_NAMES:
        windows_source_dir.join(file_name).write('test')

    _qemu_launcher = qemu_launcher.QEMULauncher(windows_source_dir=str(windows_source_dir))
    assert _qemu_launcher._get_tpl_names('win_') == [
        'win_openstack__content__network_config.jinja2',
        'win_openstack__latest__meta_data.json.jinja2',
        'win_openstack__latest__user-data.jinja2']

    _qemu_launcher.build(context, vm_config, rescue=False)

    for file_name in qemu_launcher.QEMULauncher.WINDOWS_READY_FILE_NAMES:
        assert os.path.exists(os.path.join(context.CLOUD_INIT_CONFIGS_FOLDER_PATH, file_name))

    expected_results_dir = source_path('infra/qyp/vmagent/tests/qemu_launcher_expected/windows')

    assert os.path.exists(context.CLOUD_INIT_CONFIGS_FOLDER_PATH)

    cloud_init_files = ('openstack/latest/user-data',
                        'openstack/content/network_config',
                        'openstack/latest/meta_data.json')
    for cloud_init_file_name in cloud_init_files:
        file_path = os.path.join(context.CLOUD_INIT_CONFIGS_FOLDER_PATH, cloud_init_file_name)
        assert os.path.exists(file_path), 'Cloud Init Config File' \
                                          ' does not exists: {}'.format(cloud_init_file_name)
        with open(file_path) as fp:
            file_content = fp.read()
            expected_file_path = os.path.join(expected_results_dir, context.CLOUD_INIT_CONFIGS_FOLDER_NAME,
                                              cloud_init_file_name)
            compare_or_update_content(file_content, expected_file_path)

    expected_file_path = os.path.join(expected_results_dir, context.QEMU_LAUNCHER_FILE_NAME)
    assert os.path.exists(context.QEMU_LAUNCHER_FILE_PATH)
    with open(context.QEMU_LAUNCHER_FILE_PATH) as fp:
        file_content = fp.read()
        compare_or_update_content(file_content.replace(str(tmpdir), ''), expected_file_path)
