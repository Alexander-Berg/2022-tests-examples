#!/usr/bin/env python

import json
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
    cmd = [diskmanager_bin, '--log_size_mb', '1', '-v', '-v', '-v', '-v',
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
    p.wait()
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


def assert_labels(spec, labels):
    ret_labels = spec['labels']
    for k, v in labels.iteritems():
        assert k in ret_labels
        assert ret_labels[k] == v
    assert len(ret_labels) == len(labels)


def test_02_format_disk():
    global dev
    global dmctl_bin
    global dev_id

    labels = {'key1': 'val1', 'key2': 'v2'}
    cmd = [dmctl_bin, '-J', 'disk-format', '--force', dev_id]
    for k, v in labels.iteritems():
        cmd += ["--label", "%s=%s" % (k, v)]

    subprocess.check_call(cmd)

    # After this disk should becames configured
    cmd = [dmctl_bin, '-J', 'disk-list']
    p = subprocess.Popen(cmd, stdout=subprocess.PIPE)
    data = json.load(p.stdout)
    p.wait()
    for d in data['disks']:
        if not d['meta']['id'] == dev_id:
            continue
        assert_status(d['status'], True)
        assert_labels(d['spec'], labels)
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
    p.wait()
    known_id = {}
    for v in data['volumes']:
        # Check id uniqueness
        assert known_id.get(v['meta']['id']) is None
        known_id[v['meta']['id']] = v

    return known_id[vol_id]


def delete_vol(vol_id):
    global dmctl_bin

    cmd = [dmctl_bin, '-J', 'vol-delete', vol_id]
    subprocess.check_call(cmd)


def create_vol_block(vol_name, labels={}):
    global dev
    global dmctl_bin
    global dev_id

    cmd = [dmctl_bin, '-J', 'vol-create', vol_name, dev_id, '1G', 'block']
    for k, v in labels.iteritems():
        cmd += ["--label", "%s=%s" % (k, v)]

    p = subprocess.Popen(cmd, stdout=subprocess.PIPE)
    data = json.load(p.stdout)
    p.wait()

    v = data['volume']
    assert v['spec']['name'] == vol_name
    assert v['spec']['disk_id'] == dev_id
    assert v['spec']['capacity_bytes'] == str(1024 ** 3)
    assert v['spec']['block']['stub'] is True
    assert_status(v['status'], True)
    assert_labels(v['spec'], labels)
    return v['meta']['id']


def create_vol_mnt(vol_name, fs_type="ext4", mnt_policy="default", mnt_opts='', labels={}):
    global dev
    global dmctl_bin
    global dev_id

    cmd = [dmctl_bin, '-J', 'vol-create', vol_name, dev_id, '1G', 'mount']
    cmd += ['--fs_type', fs_type]
    cmd += ['--mount_policy',  mnt_policy]
    if mnt_opts:
        cmd += ['--mount_opts', mnt_opts]
    for k, v in labels.iteritems():
        cmd += ["--label", "%s=%s" % (k, v)]

    p = subprocess.Popen(cmd, stdout=subprocess.PIPE)
    data = json.load(p.stdout)
    assert p.wait() == 0

    v = data['volume']
    if not mnt_policy.upper() == 'CUSTOM':
        mnt_opts = ''

    assert v['spec']['name'] == vol_name
    assert v['spec']['disk_id'] == dev_id
    assert v['spec']['capacity_bytes'] == str(1024 ** 3)
    assert v['spec']['mount']['fs_type'] == fs_type
    assert v['spec']['mount']['mount_policy'] == mnt_policy.upper()
    assert v['spec']['mount']['mount_flags'] == mnt_opts
    assert_status(v['status'], True)
    assert_labels(v['spec'], labels)

    return v['meta']['id']


def test_03_create_vol_block01():
    create_vol_block('vol-b1', {'key1': 'value1'})


def test_03_create_vol_block_retry():
    id01 = create_vol_block('vol-b2', {'key1': 'value1'})
    id02 = create_vol_block('vol-b3', {'key2': 'value2'})
    id11 = create_vol_block('vol-b2', {'key1': 'value1'})
    id12 = create_vol_block('vol-b3', {'key2': 'value2'})
    # Check idempotent feature
    assert id01 == id11
    assert id02 == id12


def test_03_create_vol_mount01():
    create_vol_mnt('vol-m1', labels={'key1': 'value1'})


def test_03_create_vol_mount_retry():
    id01 = create_vol_mnt('vol-m2', fs_type='ext4', mnt_policy='default', labels={'key1': 'value1'})
    id02 = create_vol_mnt('vol-m3', fs_type='ext4', mnt_policy='default', labels={'key2': 'value2'})
    id11 = create_vol_mnt('vol-m2', fs_type='ext4', mnt_policy='default', labels={'key1': 'value1'})
    id12 = create_vol_mnt('vol-m3', fs_type='ext4', mnt_policy='default', labels={'key2': 'value2'})
    # Check idempotent feature
    assert id01 == id11
    assert id02 == id12


def test_03_create_vol_spec_conflict():
    vol_name = 'vol-b4'
    create_vol_block(vol_name)

    # Volume creation with conflict spec must fail
    # try diferent label
    cmd = [dmctl_bin, '-J', 'vol-create', vol_name, dev_id, '1G', 'block', '--label', 'newKey=newVal']
    ret = subprocess.call(cmd)
    assert not (ret == 0)
