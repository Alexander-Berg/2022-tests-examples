#!/usr/bin/env python

# Simplified version of test_cli
# Run daemon with default loglevel and check that client's operations are logged properly
import json
import mmap
import os
import pytest
import subprocess
import time
from infra.diskmanager.lib.targetcli import TargetCLI
from infra.diskmanager.lib import consts
from infra.diskmanager.tests.server import common
import yatest.common  # arcadia speciffic

dev = None
dev_id = None
dev2 = None
dev2_id = None
tgt = None
daemon = None
diskmanager_bin = yatest.common.build_path("infra/diskmanager/server/diskmanager")
dmctl_bin = yatest.common.build_path("infra/diskmanager/client/dmctl")
start_ts = 0
logfile = consts.DEFAULT_SERVER_LOG
logfile_check_list = []


def add_check_list(c_string):
    global logfile_check_list

    logfile_check_list.append(c_string)


def setup_module(module):
    global dev
    global dev2
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
    dev2 = os.getenv('DISKMAN_TEST_DEV2')
    if not dev2:
        try:
            if tgt is None:
                tgt = TargetCLI()
            dev2 = tgt.create_fileio_dev('test_dmctl2', '10G', rotational=True)
        except OSError as e:
            pytest.skip('Fail to create test devices, msg:%s' % str(e))

    # Start diskman service
    if os.path.exists(logfile):
        os.remove(logfile)
        cmd = [diskmanager_bin, '--log_size_mb', '1',
               '--stat-period', '1', '--fstrim-period', '10']
    daemon = subprocess.Popen(cmd, stdout=subprocess.PIPE)
    time.sleep(2)
    daemon_ts = time.time()


def teardown_module(module):
    global tgt
    if dev_id is not None:
        vg_name = 'diskman-vg-' + dev_id
        cmd = ['vgremove', '-y', vg_name]
        subprocess.call(cmd)

    if tgt is not None:
        tgt.clear_all()
    if daemon is not None:
        daemon.terminate()
        print daemon.stdout.read()


def assert_status(status, configured):
    if configured:
        expect_val = 'True'
    else:
        expect_val = 'False'
    assert status['absent']['status'] == 'False'
    assert status['error']['status'] == 'False'
    assert status['configured']['status'] == expect_val
    assert status['ready']['status'] == expect_val


def test_01_list_disks():
    global dev
    global dmctl_bin
    global dev_id

    cmd = [dmctl_bin, '-J', 'disk-list']
    p = subprocess.Popen(cmd, stdout=subprocess.PIPE)
    data = json.load(p.stdout)
    assert p.wait() == 0
    known_id = {}
    for d in data['disks']:

        # Check id uniqueness
        assert known_id.get(d['meta']['id']) is None
        known_id[d['meta']['id']] = d

        if d['spec']['device_path'] == dev:
            assert d['spec']['storage_class'] == 'ssd'
            assert_status(d['status'], False)
            dev_id = d['meta']['id']
            print 'use test_dev_id:', dev_id
            continue

        if d['spec']['device_path'] == dev2:
            assert d['spec']['storage_class'] == 'hdd'
            dev2_id = d['meta']['id']

    assert dev_id is not None
    assert dev2_id is not None
    assert os.path.exists(logfile)
    add_check_list("fstrim-job: {} start job".format(dev_id))


def test_02_format_disk():
    global dev
    global dmctl_bin
    global dev_id

    cmd = [dmctl_bin, '-J', 'disk-format', '--force', dev_id]
    subprocess.check_call(cmd)

    # After this disk should becames configured
    cmd = [dmctl_bin, '-J', 'disk-list']
    p = subprocess.Popen(cmd, stdout=subprocess.PIPE)
    data = json.load(p.stdout)
    assert p.wait() == 0
    add_check_list('FormadDisk %s(%s)' % (dev, dev_id))
    for d in data['disks']:
        if not d['meta']['id'] == dev_id:
            continue
        assert_status(d['status'], True)
        return
    # This should never happen
    pytest.fail('fail to find disk with dev_id:%s' % dev_id)


def get_vol(vol_id):
    global dev
    global dmctl_bin
    global dev_id

    cmd = [dmctl_bin, '-J', 'vol-list']
    p = subprocess.Popen(cmd, stdout=subprocess.PIPE)
    data = json.load(p.stdout)
    assert p.wait() == 0
    known_id = {}
    for v in data['volumes']:
        # Check id uniqueness
        assert known_id.get(v['meta']['id']) is None
        known_id[v['meta']['id']] = v

    return known_id[vol_id]


def delete_vol(vol_name, vol_id):
    global dmctl_bin

    cmd = [dmctl_bin, '-J', 'vol-delete', vol_id]
    subprocess.check_call(cmd)
    add_check_list('DeleteVolume %s(%s)' % (vol_name, vol_id))


def create_vol(vol_name, block):
    global dev
    global dmctl_bin
    global dev_id

    if block:
        cmd = [dmctl_bin, '-J', 'vol-create', vol_name, dev_id, '1G', 'block']
    else:
        cmd = [dmctl_bin, '-J', 'vol-create', vol_name, dev_id, '1G', 'mount', '--fs_type=ext4']

    p = subprocess.Popen(cmd, stdout=subprocess.PIPE)
    data = json.load(p.stdout)
    assert p.wait() == 0

    v = data['volume']
    assert v['spec']['name'] == vol_name
    assert v['spec']['disk_id'] == dev_id
    assert v['spec']['capacity_bytes'] == str(1024 ** 3)
    if block:
        assert v['spec']['block']['stub'] is True
    else:
        assert v['spec']['mount']['fs_type'] == 'ext4'
        assert v['spec']['mount']['mount_policy'] == 'DEFAULT'
    assert_status(v['status'], True)
    vol_id = v['meta']['id']
    add_check_list('CreateVolume %s on %s(%s)' % (vol_name, dev, dev_id))
    return vol_id


def test_03_create_vol_mount01():
    create_vol('vol-m0', block=False)


def test_04_del_vol():
    id1 = create_vol('vol-b1', block=True)
    id2 = create_vol('vol-b2', block=True)
    id3 = create_vol('vol-m1', block=False)
    id4 = create_vol('vol-m2', block=False)

    delete_vol('vol-b1', id1)
    delete_vol('vol-b2', id2)
    delete_vol('vol-m1', id3)
    delete_vol('vol-m2', id4)


def test_05_mount_vol():
    global dmctl_bin

    vol_name = 'vol-m1'
    vol_id = create_vol(vol_name, block=False)
    mpath = '/tmp/vol-m1'

    cmd = [dmctl_bin, '-J', 'vol-mount', vol_id, mpath]
    subprocess.check_call(cmd)
    v = get_vol(vol_id)
    assert_status(v['status'], True)
    assert v['status']['mount_path'] == mpath

    # Retry should be NOOP, and must succeed
    subprocess.check_call(cmd)
    v = get_vol(vol_id)
    assert_status(v['status'], True)
    assert v['status']['mount_path'] == mpath
    add_check_list('MountVolume %s(%s) at %s' % (vol_name, vol_id, mpath))

    # Umount vol
    cmd = [dmctl_bin, '-J', 'vol-umount', vol_id]
    subprocess.check_call(cmd)
    add_check_list('MountVolume %s(%s) at %s' % (vol_name, vol_id, mpath))

    # Second umount should be NOOP, and must succeed
    subprocess.check_call(cmd)


def test_99_check_logfile():
    global logfile
    global logfile_check_list

    log_wait = 5
    # Ensure that daemon flush logfile
    print 'wait %d seconds for logs' % log_wait
    time.sleep(log_wait)

    f = open(logfile)
    s = mmap.mmap(f.fileno(), 0, access=mmap.ACCESS_READ)

    for token in logfile_check_list:
        print 'lookup', token
        assert s.find(token) != -1
