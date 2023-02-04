#!/usr/bin/env python

import logging
import mmap
import os
import pytest
import subprocess
import time
from infra.diskmanager.lib.targetcli import TargetCLI
from infra.diskmanager.lib import consts
from infra.diskmanager.lib import mount
from infra.diskmanager.tests.server import common

import yatest.common  # arcadia speciffic

dev = None
dev_id = None
dev_mnt = '/ssd'
tgt = None
daemon = None
diskmanager_bin = yatest.common.build_path("infra/diskmanager/server/diskmanager")
dmctl_bin = yatest.common.build_path("infra/diskmanager/client/dmctl")
start_ts = 0
log = logging.getLogger(__name__)
logfile = consts.DEFAULT_SERVER_LOG
logfile_check_list = []


def add_check_list(c_string):
    global logfile_check_list

    logfile_check_list.append(c_string)


def do_cmd(cmd):
    print 'exec cmd :%s' % cmd
    subprocess.check_call(cmd)


def setup_module(module):
    global dev
    global dev_mnt
    global dev_uuid
    global tgt
    global diskmanager_bin
    global dmctl_bin
    global daemon
    global daemon_ts
    global logfile_check_list

    if not os.geteuid() == 0:
        pytest.skip('Not privileged user')
        return

    assert os.path.exists(diskmanager_bin)
    assert os.path.exists(dmctl_bin)
    common.prepare_utils(yatest.common.build_path)

    dev = os.getenv('DISKMAN_TEST_DEV')
    if not dev:
        try:
            tgt = TargetCLI()
            dev = tgt.create_fileio_dev('test_dmctl', '10G', rotational=False)
        except OSError as e:
            pytest.skip('Fail to create test devices, msg:%s' % str(e))

    try:
        do_cmd(['pvcreate', dev])
        do_cmd(['vgcreate', 'syspart_vg', dev])
        do_cmd(['lvcreate', '-l', '100%FREE', '-n', 'ssd', 'syspart_vg'])
        dev = '/dev/syspart_vg/ssd'
        do_cmd(['mkfs.ext4', '-F', dev])
        if not os.path.exists(dev_mnt):
            os.makedirs(dev_mnt)
        do_cmd(['mount', dev, dev_mnt, "-obarrier=0,discard"])
    except OSError as e:
        pytest.skip('Fail to prepare test device, msg:%s' % str(e))

    # Start diskman service
    if os.path.exists(logfile):
        os.remove(logfile)
    cmd = [diskmanager_bin, '--log_size_mb', '1', '-v', '-v', '-v', '-v',
           '--stat-period', '1', '--fstrim-period', '10']
    daemon = subprocess.Popen(cmd, stdout=subprocess.PIPE)
    time.sleep(2)
    daemon_ts = time.time()


def teardown_module(module):
    global tgt
    global dev_mnt

    do_cmd(['umount', dev_mnt])

    if tgt is not None:
        tgt.clear_all()
    if daemon is not None:
        daemon.terminate()
        print daemon.stdout.read()


def test_check_sysvol_remount():
    global dmctl_bin
    global dev_mnt

    # Force daemon cache update and let daemon fix mount options
    cmd = [dmctl_bin, '-J', 'daemon-update-cache']
    do_cmd(cmd)

    # Check that daemon fixed mount options
    mi = mount.find_by_path(dev_mnt)
    on_opt = ['relatime', 'rw', 'nosuid', 'nodev', 'lazytime']
    off_opt = ["nobarrier", "discard"]

    log.info("check opts :%s", str(mi.opts))
    for o in on_opt:
        assert o in mi.opts
    for o in off_opt:
        assert o not in mi.opts


def test_999_check_logfile():
    global logfile
    global logfile_check_list
    f = open(logfile)
    s = mmap.mmap(f.fileno(), 0, access=mmap.ACCESS_READ)
    print("logfile_data:  {}".format(f.read()))
    for token in logfile_check_list:
        print 'lookup', token
        assert s.find(token) != -1
