import contextlib
import errno
import os
import shutil

from infra.ya_salt.lib import kernel_man


@contextlib.contextmanager
def remove_after_done(path):
    yield
    try:
        shutil.rmtree(path)
    except EnvironmentError as e:
        if e.errno == errno.ENOENT:
            pass
        else:
            raise


def touch(prefix, path):
    open(os.path.join(prefix, path), 'w').close()


def test_get_version():
    assert kernel_man.Manager.get_version() == os.uname()[2]


def test_sort_versions():
    versions = [
        'vmlinuz-4.4.0-21-generic',
        'vmlinuz-4.14.78-29',
        'vmlinuz-4.4.114-50',
    ]
    expected = [
        'vmlinuz-4.14.78-29',
        'vmlinuz-4.4.114-50',
        'vmlinuz-4.4.0-21-generic',
    ]
    assert kernel_man.sort_versions(versions) == expected


def test_get_boot_version():
    boot_dir = './boot'
    # Test no boot directory
    v, err = kernel_man.Manager.get_latest_boot_version(boot_dir)
    assert v is None
    assert err is not None
    os.mkdir(boot_dir)
    # Test no kernels
    v, err = kernel_man.Manager.get_latest_boot_version(boot_dir)
    assert v is None
    assert err is not None
    # Test good case
    with remove_after_done(boot_dir):
        touch(boot_dir, 'initramfs-0-rescue-2fc192c2d3524bceb50f4a32aa687e24.img')
        touch(boot_dir, 'config-4.4.0-21-generic')
        touch(boot_dir, 'vmlinuz-4.4.0-21-generic')
        touch(boot_dir, 'vmlinuz-4.14.78-29')
        touch(boot_dir, 'config-4.14.78-29')
        v, err = kernel_man.Manager.get_latest_boot_version(boot_dir)
    assert err is None
    assert v == '4.14.78-29'
