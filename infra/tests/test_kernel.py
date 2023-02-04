import mock

from infra.ya_salt.lib.components import kernel
from infra.ya_salt.proto import ya_salt_pb2


def test_update_boot_version():
    status = ya_salt_pb2.BootVersion()
    error = 'Failed to determine'
    kernel.update_boot_version(status, lambda: (None, error))
    assert status.version == 'unknown'
    assert status.error == error
    assert status.transition_time.seconds
    # Good case
    status.Clear()
    status.version = '4.4.114-50'
    version = '4.14.68-14'
    kernel.update_boot_version(status, lambda: (version, None))
    assert status.version == version
    assert status.error == ''
    assert status.transition_time.seconds


def test_run_versions_equal(monkeypatch):
    """
    Checks that we do nothing if kernel version is the one we are running
    """
    k_manager = mock.Mock()
    version = '4.4.114-50'
    k_manager.get_version.return_value = version
    k_manager.get_boot_version.return_value = (version, None)
    k = kernel.Kernel(k_manager)
    spec_a = ya_salt_pb2.KernelSpec()
    spec_a.version = version
    spec_b = ya_salt_pb2.KernelSpec()
    monkeypatch.setattr(kernel, 'get_kernel_version_from_hostctl_spec', lambda *args, **kwargs: (version, None))
    status = ya_salt_pb2.KernelStatus()
    k.run(spec_a, spec_b, status)
    assert spec_b.version == version
    assert k_manager.get_version.call_count == 1
    assert status.version == version

    assert status.need_reboot.status == 'False'
    assert status.need_reboot.message == 'Spec and current versions equal'
    assert status.need_reboot.transition_time.seconds

    assert status.ready.status == 'True'
    assert status.ready.message == 'OK'
    assert status.ready.transition_time.seconds


def test_run_no_version_a_and_version_b_equal(monkeypatch):
    """
    Checks that we do nothing if no version in spec.
    """
    k_manager = mock.Mock()
    version = '4.4.114-50'
    k_manager.get_version.return_value = version
    k_manager.get_boot_version.return_value = (version, None)
    k = kernel.Kernel(k_manager)
    spec_a = ya_salt_pb2.KernelSpec()
    spec_b = ya_salt_pb2.KernelSpec()
    monkeypatch.setattr(kernel, 'get_kernel_version_from_hostctl_spec', lambda *args, **kwargs: (version, None))
    status = ya_salt_pb2.KernelStatus()
    k.run(spec_a, spec_b, status)
    assert spec_b.version == version
    assert k_manager.get_version.call_count == 1
    assert status.version == version

    assert status.need_reboot.status == 'False'
    assert status.need_reboot.message == 'Spec and current versions equal'
    assert status.need_reboot.transition_time.seconds

    assert status.ready.status == 'True'
    assert status.ready.message == 'OK'
    assert status.ready.transition_time.seconds


def test_run_no_version_a_and_b_version_mismatch(monkeypatch):
    """
    Checks that we do nothing if boot version does not match required.
    """
    k_manager = mock.Mock()
    version = '4.4.114-50'
    k_manager.get_version.return_value = '4.3.44-29'
    k_manager.get_boot_version.return_value = ('4.3.44-29', None)
    k = kernel.Kernel(k_manager)
    spec_a = ya_salt_pb2.KernelSpec()
    spec_b = ya_salt_pb2.KernelSpec()
    monkeypatch.setattr(kernel, 'get_kernel_version_from_hostctl_spec', lambda *args, **kwargs: (version, None))
    status = ya_salt_pb2.KernelStatus()
    k.run(spec_a, spec_b, status)
    assert spec_b.version == version
    assert k_manager.get_version.call_count == 1

    assert status.need_reboot.status == 'False'
    assert status.need_reboot.message.startswith('Invalid boot version')
    assert status.need_reboot.transition_time.seconds

    assert status.ready.status == 'False'
    assert status.ready.message == "spec.version='4.4.114-50' != status.version='4.3.44-29'"
    assert status.ready.transition_time.seconds


def test_run_no_version_b_and_a_version_equal(monkeypatch):
    """
    Checks that we do nothing if no version in spec.
    """
    k_manager = mock.Mock()
    version = '4.4.114-50'
    k_manager.get_version.return_value = version
    k_manager.get_boot_version.return_value = (version, None)
    k = kernel.Kernel(k_manager)
    spec_a = ya_salt_pb2.KernelSpec()
    spec_a.version = version
    spec_b = ya_salt_pb2.KernelSpec()
    monkeypatch.setattr(kernel, 'get_kernel_version_from_hostctl_spec', lambda *args, **kwargs: ('', 'mock-hostctl-version-err'))
    status = ya_salt_pb2.KernelStatus()
    k.run(spec_a, spec_b, status)
    assert k_manager.get_version.call_count == 1
    assert status.version == version

    assert status.need_reboot.status == 'False'
    assert status.need_reboot.message == 'Spec and current versions equal'
    assert status.need_reboot.transition_time.seconds

    assert status.ready.status == 'True'
    assert status.ready.message == 'OK'
    assert status.ready.transition_time.seconds


def test_run_no_version_b_and_a_version_mismatch(monkeypatch):
    """
    Checks that we do nothing if no version in spec.
    """
    k_manager = mock.Mock()
    version = '4.4.114-50'
    k_manager.get_version.return_value = '4.3.44-29'
    k_manager.get_boot_version.return_value = ('4.3.44-29', None)
    k = kernel.Kernel(k_manager)
    spec_a = ya_salt_pb2.KernelSpec()
    spec_a.version = version
    spec_b = ya_salt_pb2.KernelSpec()
    monkeypatch.setattr(kernel, 'get_kernel_version_from_hostctl_spec',
                        lambda *args, **kwargs: ('', 'mock-hostctl-version-err'))
    status = ya_salt_pb2.KernelStatus()
    k.run(spec_a, spec_b, status)
    assert k_manager.get_version.call_count == 1
    assert status.version == '4.3.44-29'

    assert status.need_reboot.status == 'False'
    assert status.need_reboot.message.startswith('Invalid boot version')
    assert status.need_reboot.transition_time.seconds

    assert status.ready.status == 'False'
    assert status.ready.message == "spec.version='4.4.114-50' != status.version='4.3.44-29'"
    assert status.ready.transition_time.seconds


def test_run_no_versions(monkeypatch):
    """
    Checks that we do nothing if no version in spec.
    """
    k_manager = mock.Mock()
    version = '4.4.114-50'
    k_manager.get_version.return_value = version
    k_manager.get_boot_version.return_value = (version, None)
    k = kernel.Kernel(k_manager)
    spec_a = ya_salt_pb2.KernelSpec()
    spec_b = ya_salt_pb2.KernelSpec()
    monkeypatch.setattr(kernel, 'get_kernel_version_from_hostctl_spec',
                        lambda *args, **kwargs: ('', 'mock-hostctl-version-err'))
    status = ya_salt_pb2.KernelStatus()
    k.run(spec_a, spec_b, status)
    assert k_manager.get_version.call_count == 1
    assert status.version == version

    assert status.need_reboot.status == 'False'
    assert status.need_reboot.message == 'No version in spec'
    assert status.need_reboot.transition_time.seconds

    assert status.ready.status == 'Unknown'
    assert status.ready.message == 'No version in spec'
    assert status.ready.transition_time.seconds


def test_run_set_need_reboot(monkeypatch):
    """
    Checks that we set flag for reboot.
    """
    k_manager = mock.Mock()
    version = '4.4.114-50'
    k_manager.get_version.return_value = '4.3.44-29'
    k_manager.get_boot_version.return_value = (version, None)
    k = kernel.Kernel(k_manager)
    spec_a = ya_salt_pb2.KernelSpec()
    spec_a.version = version
    spec_b = ya_salt_pb2.KernelSpec()
    monkeypatch.setattr(kernel, 'get_kernel_version_from_hostctl_spec',
                        lambda *args, **kwargs: (version, None))
    status = ya_salt_pb2.KernelStatus()
    k.run(spec_a, spec_b, status)
    assert k_manager.get_version.call_count == 1
    assert status.version == k_manager.get_version.return_value

    assert status.need_reboot.status == 'True'
    assert status.need_reboot.message == 'Ready to reboot into 4.4.114-50'
    assert status.need_reboot.transition_time.seconds

    assert status.ready.status == 'False'
    assert status.ready.message == "spec.version='4.4.114-50' != status.version='4.3.44-29'"
    assert status.ready.transition_time.seconds


def test_get_kernel_version_from_hostctl_spec_ok():
    m = mock.mock_open(read_data='version: "5.13.1"\n')
    ver, err = kernel.get_kernel_version_from_hostctl_spec('spec.yaml', m)
    m.assert_called_once_with("spec.yaml")
    assert err is None
    assert ver == '5.13.1'


def test_get_kernel_version_from_hostctl_spec_os_error():
    m = mock.Mock(side_effect=OSError('os'))
    ver, err = kernel.get_kernel_version_from_hostctl_spec('spec.yaml', m)
    m.assert_called_once_with("spec.yaml")
    assert err == 'failed to open spec.yaml: os'
    assert not ver


def test_get_kernel_version_from_hostctl_spec_key_error():
    m = mock.mock_open(read_data='vers1on: "5.13.1"\n')
    ver, err = kernel.get_kernel_version_from_hostctl_spec('spec.yaml', m)
    m.assert_called_once_with("spec.yaml")
    assert err == 'kernel spec has no "version" field'
    assert not ver


def test_get_kernel_version_from_hostctl_spec_unknown_error():
    m = mock.Mock(side_effect=Exception('mock'))
    ver, err = kernel.get_kernel_version_from_hostctl_spec('spec.yaml', m)
    m.assert_called_once_with("spec.yaml")
    assert err == 'unknown error loading spec.yaml: mock'
    assert not ver
