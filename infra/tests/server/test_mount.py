#!/usr/bin/env python

import os
import pytest
import subprocess
import logging
from infra.diskmanager.lib import mount
from infra.diskmanager.lib import kernel
from infra.diskmanager.lib import diskmanager

dev = None
tgt = None
TS_NAME = 'test_mount'
log = logging.getLogger(__name__)


def test_01_mountinfo():
    m_info = mount.find_by_path('/proc')
    assert m_info.fs_type == 'proc'
    assert m_info.fs_dev == 'proc'


def test_02_mountinfo_opt_fields():
    if not os.geteuid() == 0:
        pytest.skip('Not privileged user')
        return

    mpath = os.path.join('/tmp', TS_NAME, 'mnt')
    os.makedirs(mpath)

    stages = []
    stages.append(['mount', '/proc', mpath, '-obind'])
    stages.append(['mount', '--make-slave', mpath])
    stages.append(['mount', '--make-shared', mpath])
    for cmd in stages:
        subprocess.check_call(cmd)

    m_info = mount.find_by_path(mpath)
    assert m_info.fs_type == 'proc'
    assert m_info.fs_dev == 'proc'
    subprocess.check_call(['umount', mpath])
    os.rmdir(mpath)


@pytest.mark.parametrize("uname, need_workaround", [
    ("4.4.171-15", True),
    ("4.19.73-15", True),
    ("4.19.100-23", False),
    ("4.19.119-30.2", False),
    ("5.1", False)])
def test_diskman_78(uname, need_workaround):
    kernel_release = kernel.kernel_version(uname)
    mp = diskmanager.get_mpolicy(kernel_release)

    assert mp["ext4"]["SAFE"] == mp["ext4"]["SAFE_LAZYTIME"]
    if need_workaround:
        assert mp["ext4"]["YT"] == mp["ext4"]["SAFE_NO_LAZYTIME"]
    else:
        assert mp["ext4"]["YT"] == mp["ext4"]["SAFE_LAZYTIME"]
