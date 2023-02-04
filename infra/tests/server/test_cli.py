#!/usr/bin/env python

import json
import mmap
import os
import pytest
import logging
import requests
import socket
import subprocess
import time
from infra.diskmanager.lib.lvm import LVM
from infra.diskmanager.lib.targetcli import TargetCLI
from infra.diskmanager.lib import mount
from infra.diskmanager.lib import consts
from infra.diskmanager.lib import disk
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
BYTES_IN_GB = 1024 * 1024 * 1024
log = logging.getLogger(__name__)
io_sched = "mq-deadline"


def run(args, **kwargs):
    log.debug("run cmd: '{}'".format(" ".join(args)))
    return subprocess.check_call(args, **kwargs)


def add_check_list(c_string):
    global logfile_check_list

    logfile_check_list.append(c_string)


def align_up(a, b):
    return ((a + b-1) / b) * b


def setup_module(module):
    global dev
    global dev2
    global dev_id
    global dev2_id
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
        except Exception as e:
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
    config = """[SERVER]
stat_period=1
quota_sync_period=1000
fstrim_period=10
format_new_disk=1
#yasm_ur=XXX
"""
    conf_name = '/tmp/diskman-test_cli.conf'
    with open(conf_name, 'w') as f:
        f.write(config)
    cmd = [diskmanager_bin, '--config', conf_name, '--default-io-scheduler=' + io_sched,
           '--log_size_mb', '1', '-v', '-v', '-v', '-v']
    daemon = subprocess.Popen(cmd, stdout=subprocess.PIPE)
    time.sleep(2)
    daemon_ts = time.time()

    cmd = [dmctl_bin, '-J', 'disk-list']
    data = json.loads(subprocess.check_output([dmctl_bin, '-J', 'disk-list']))
    for d in data['disks']:
        if d['spec']['device_path'] == dev:
            dev_id = d['meta']['id']
            print 'use test_dev_id:', dev_id

        elif d['spec']['device_path'] == dev2:
            dev2_id = d['meta']['id']
            print 'use test_dev2_id:', dev2_id


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


def assert_disk_alloc(disk, min_bytes, max_bytes):
    capacity = long(disk['spec']['capacity_bytes'])
    pv_capacity = long(disk['spec']['vpool_capacity_bytes'])
    allocatable = long(disk['status']['allocatable_bytes'])

    assert capacity >= max_bytes
    assert max_bytes >= allocatable
    assert allocatable >= min_bytes

    assert capacity >= pv_capacity
    assert pv_capacity >= allocatable


def get_disk_info(disk_id):
    cmd = [dmctl_bin, '-J', 'disk-list']
    p = subprocess.Popen(cmd, stdout=subprocess.PIPE)
    data = json.load(p.stdout)
    p.wait()
    for d in data['disks']:
        if d['meta']['id'] == disk_id:
            return d
    return None


def get_vol_info(vid):
    cmd = [dmctl_bin, '-J', 'vol-list']
    p = subprocess.Popen(cmd, stdout=subprocess.PIPE)
    data = json.load(p.stdout)
    p.wait()
    for d in data['volumes']:
        if d['meta']['id'] == vid:
            return d
    return None


def test_01_list_disks():
    cmd = [dmctl_bin, '-J', 'disk-list']
    p = subprocess.Popen(cmd, stdout=subprocess.PIPE)
    data = json.load(p.stdout)
    p.wait()
    known_id = {}
    dev_id = None
    dev2_id = None
    for d in data['disks']:

        # Check id uniqueness
        assert known_id.get(d['meta']['id']) is None
        known_id[d['meta']['id']] = d

        if d['spec']['device_path'] == dev:
            assert d['spec']['storage_class'] == 'ssd'
            assert d['spec']['scheduler'] == io_sched
            dev_id = d['meta']['id']
            print 'use test_dev_id:', dev_id
            continue

        if d['spec']['device_path'] == dev2:
            assert d['spec']['storage_class'] == 'hdd'
            assert d['spec']['scheduler'] == io_sched
            dev2_id = d['meta']['id']

    assert dev_id is not None
    assert dev2_id is not None
    assert os.path.exists(logfile)
    add_check_list('Init DiskManager service build')
    add_check_list("fstrim-job: {} start job".format(dev_id))
    add_check_list('fstrim-job: {}, no candidated found'.format(dev_id))
    add_check_list('quotasync-job: start job, period:1000 sec')
    add_check_list("run cmd: '/opt/diskmanager/utils/dqsync'")


def test_02_autoformat_disk():
    global dev
    global dmctl_bin
    global dev_id

    # Force daemon cache update and let daemon format all disks
    cmd = [dmctl_bin, '-J', 'daemon-update-cache']
    run(cmd)
    # After this disk should becames configured
    d = get_disk_info(dev_id)
    assert d
    assert_status(d['status'], True)
    assert_disk_alloc(d, BYTES_IN_GB, 10 * BYTES_IN_GB)


def test_03_ioscheduler():
    global dev
    global dmctl_bin
    global dev_id

    # Screwup scheduler
    disk.write_sysfs_file('none', '/sys/class/block/{}/queue/scheduler'.format(os.path.basename(dev)))

    # Force daemon cache update and fix ioscheduler options
    cmd = [dmctl_bin, '-J', 'daemon-update-cache']
    run(cmd)

    d = get_disk_info(dev_id)
    assert d
    assert d['spec']['scheduler'] == io_sched
    add_check_list('Set ioscheduler for {} old:none, new:{}'.format(os.path.basename(dev), io_sched))


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
    run(cmd)


def create_vol_block(vol_name):
    global dev
    global dmctl_bin
    global dev_id

    cmd = [dmctl_bin, '-J', 'vol-create', vol_name, dev_id, '1G', 'block']
    print "creat_vol_block: %r" % (cmd, )
    p = subprocess.Popen(cmd, stdout=subprocess.PIPE)
    data = json.load(p.stdout)
    p.wait()

    v = data['volume']
    assert v['spec']['name'] == vol_name
    assert v['spec']['disk_id'] == dev_id
    assert v['spec']['capacity_bytes'] == str(1024 ** 3)
    assert v['spec']['block']['stub'] is True
    assert_status(v['status'], True)

    return v['meta']['id']


def create_vol_mnt(vol_name, fs_type="ext4", mnt_policy="default", mnt_opts='', label=''):
    global dev
    global dmctl_bin
    global dev_id

    cmd = [dmctl_bin, '-J', 'vol-create', vol_name, dev_id, '1G', 'mount']
    cmd += ['--fs_type', fs_type]
    cmd += ['--mount_policy',  mnt_policy]
    if mnt_opts:
        cmd += ['--mount_opts', mnt_opts]
    if label:
        cmd += ['--label', label]

    print "create_vol_mnt: %r" % (cmd, )
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
    assert v['spec']['mount']['root_owner']['uid'] == str(os.getuid())
    assert v['spec']['mount']['root_owner']['gid'] == str(consts.DEFAULT_GROUP_ID)

    if 'diskman.sys=true' in label:
        assert_status(v['status'], False)
    else:
        assert_status(v['status'], True)

    return v['meta']['id']


def test_03_create_vol_block01():
    create_vol_block('vol-b1')


def test_03_create_vol_block_retry():
    d1 = get_disk_info(dev_id)
    id01 = create_vol_block('vol-b1')
    id02 = create_vol_block('vol-b2')

    d2 = get_disk_info(dev_id)
    id11 = create_vol_block('vol-b1')
    id12 = create_vol_block('vol-b2')

    d3 = get_disk_info(dev_id)

    # Check idempotent feature
    assert id01 == id11
    assert id02 == id12
    # Check capacity updated
    assert long(d1['status']['allocatable_bytes']) > long(d2['status']['allocatable_bytes'])
    assert long(d2['status']['allocatable_bytes']) == long(d3['status']['allocatable_bytes'])
    # Check volume capacity not changed
    assert long(d1['spec']['vpool_capacity_bytes']) == long(d2['spec']['vpool_capacity_bytes'])
    assert long(d2['spec']['vpool_capacity_bytes']) == long(d3['spec']['vpool_capacity_bytes'])


def test_03_create_vol_mount01():
    d1 = get_disk_info(dev_id)
    create_vol_mnt('vol-m1')
    d2 = get_disk_info(dev_id)

    assert long(d1['status']['allocatable_bytes']) > long(d2['status']['allocatable_bytes'])
    assert long(d1['spec']['vpool_capacity_bytes']) == long(d2['spec']['vpool_capacity_bytes'])


def test_03_create_vol_mount_retry():
    id01 = create_vol_mnt('vol-m1', fs_type='ext4', mnt_policy='default')
    id02 = create_vol_mnt('vol-m2', fs_type='ext4', mnt_policy='default')
    id11 = create_vol_mnt('vol-m1', fs_type='ext4', mnt_policy='default')
    id12 = create_vol_mnt('vol-m2', fs_type='ext4', mnt_policy='default')
    # Check idempotent feature
    assert id01 == id11
    assert id02 == id12


def test_03_create_vol_spec_conflict():
    vol_name = 'vol-b1'
    create_vol_block(vol_name)

    # Volume creation with conflict spec must fail
    # try different size
    cmd = [dmctl_bin, '-J', 'vol-create', vol_name, dev_id, '2G', 'block']
    ret = subprocess.call(cmd)
    assert not (ret == 0)
    # try different type
    cmd = [dmctl_bin, '-J', 'vol-create', vol_name, dev_id, '1G', 'mount', '--fs_type=ext4']
    ret = subprocess.call(cmd)
    assert not (ret == 0)


def test_03_create_vol_mnt_policy_conflict():
    vol_name = 'vol-m5'
    id1 = create_vol_mnt(vol_name, 'ext4', 'unsafe')

    # try different mnt_policy type
    cmd = [dmctl_bin, '-J', 'vol-create', vol_name, dev_id, '1G', 'mount', '--mount_policy', ' default']
    ret = subprocess.call(cmd)
    assert not (ret == 0)

    cmd = [dmctl_bin, '-J', 'vol-create', vol_name, dev_id, '1G', 'mount', '--mount_policy', 'custom', '--mount_opts', 'discard']
    ret = subprocess.call(cmd)
    assert not (ret == 0)

    delete_vol(id1)


def test_03_create_vol_mnt_uid_conflict():
    vol_name = 'vol-m5'
    id1 = create_vol_mnt(vol_name, 'ext4')

    # try different mnt_policy type
    cmd = [dmctl_bin, '-J', 'vol-create', vol_name, dev_id, '1G', 'mount', '--uid', '3']
    ret = subprocess.call(cmd)
    assert not (ret == 0)
    delete_vol(id1)


def test_03_create_vol_mnt_gid_conflict():
    vol_name = 'vol-m5'
    id1 = create_vol_mnt(vol_name, 'ext4')

    # try different mnt_policy type
    cmd = [dmctl_bin, '-J', 'vol-create', vol_name, dev_id, '1G', 'mount', '--gid', '1335']
    ret = subprocess.call(cmd)
    assert not (ret == 0)
    delete_vol(id1)


def test_03_create_vol_size_alignment():
    d = get_disk_info(dev_id)
    e_sz = int(d['spec']['extent_size_bytes'])
    assert(e_sz > 1)
    assert(e_sz <= BYTES_IN_GB)

    # Volume creation with unaligned size must fail.
    cmd = [dmctl_bin, '-J', 'vol-create', '03-vol-under-size', dev_id, str(align_up(BYTES_IN_GB, e_sz) - 1), 'block']
    ret = subprocess.call(cmd)
    assert ret

    cmd = [dmctl_bin, '-J', 'vol-create', '03-vol-over-size', dev_id, str(align_up(BYTES_IN_GB, e_sz) + 1), 'block']
    ret = subprocess.call(cmd)
    assert ret

    # Check that we can create volume with minimal size
    cmd = [dmctl_bin, '-J', 'vol-create', '03-vol-minimal-size', dev_id, str(e_sz), 'block']
    p = subprocess.Popen(cmd, stdout=subprocess.PIPE)
    data = json.load(p.stdout)
    assert p.wait() == 0
    delete_vol(data['volume']['meta']['id'])


def test_04_del_vol():
    id1 = create_vol_block('vol-b1')
    id2 = create_vol_block('vol-b2')
    id3 = create_vol_mnt('vol-m1')
    id4 = create_vol_mnt('vol-m2')

    delete_vol(id1)
    delete_vol(id2)
    delete_vol(id3)
    delete_vol(id4)


def test_05_mount_vol():
    global dmctl_bin

    vol_id = create_vol_mnt('vol-m1', fs_type='ext4', mnt_policy='default')
    mpath = '/tmp/vol-m1'

    cmd = [dmctl_bin, '-J', 'vol-mount', vol_id, mpath]
    run(cmd)
    v = get_vol(vol_id)
    assert_status(v['status'], True)
    assert v['status']['mount_path'] == mpath

    # Retry should be NOOP, and must succeed
    run(cmd)
    v = get_vol(vol_id)
    assert_status(v['status'], True)
    assert v['status']['mount_path'] == mpath

    cmd = ["project_quota",  "list", mpath]
    run(cmd)

    # Try to mount to another path, fail expected
    cmd = [dmctl_bin, '-J', 'vol-mount', vol_id, mpath + '-duplicate']
    ret = subprocess.call(cmd)
    assert not ret == 0
    # Recheck that spec still points to original mount
    v = get_vol(vol_id)
    assert_status(v['status'], True)
    assert v['status']['mount_path'] == mpath

    # Let fstrim worker to find this volume
    time.sleep(2)
    add_check_list('fstrim-job: {} Start volume: {}, mount at: {}'.format(dev_id, vol_id, mpath))
    add_check_list('fstrim-job: {} Complete with volume: {}, mount at: {}'.format(dev_id, vol_id, mpath))

    # Umount vol
    cmd = [dmctl_bin, '-J', 'vol-umount', vol_id]
    run(cmd)

    # Second umount should be NOOP, and must succeed
    run(cmd)


def test_05_mount_vol_unsafe():
    global dmctl_bin

    vol_id = create_vol_mnt('vol-m2', fs_type='ext4', mnt_policy='unsafe')
    mpath = '/tmp/vol-m2'

    cmd = [dmctl_bin, '-J', 'vol-mount', vol_id, mpath]
    run(cmd)
    v = get_vol(vol_id)
    assert_status(v['status'], True)
    assert v['status']['mount_path'] == mpath

    # Umount vol
    cmd = [dmctl_bin, '-J', 'vol-umount', vol_id]
    run(cmd)

    delete_vol(vol_id)


def test_05_mount_vol_safe():
    global dmctl_bin

    vol_id = create_vol_mnt('vol-m2', fs_type='ext4', mnt_policy='safe')
    mpath = '/tmp/vol-m2'

    cmd = [dmctl_bin, '-J', 'vol-mount', vol_id, mpath]
    run(cmd)
    v = get_vol(vol_id)
    assert_status(v['status'], True)
    assert v['status']['mount_path'] == mpath

    # Umount vol
    cmd = [dmctl_bin, '-J', 'vol-umount', vol_id]
    run(cmd)

    delete_vol(vol_id)


def test_05_mount_vol_custom():
    global dmctl_bin

    vol_id = create_vol_mnt('vol-m3', fs_type='ext4', mnt_policy='custom', mnt_opts='discard')
    mpath = '/tmp/vol-m3'

    cmd = [dmctl_bin, '-J', 'vol-mount', vol_id, mpath]
    run(cmd)
    v = get_vol(vol_id)
    assert_status(v['status'], True)
    assert v['status']['mount_path'] == mpath

    # Umount vol
    cmd = [dmctl_bin, '-J', 'vol-umount', vol_id]
    run(cmd)

    delete_vol(vol_id)


def test_custom_vol_longopt():
    global dmctl_bin

    vol_id = create_vol_mnt('vol-m3', fs_type='ext4', mnt_policy='custom', mnt_opts='nodelalloc,barrier=0,discard')
    mpath = '/tmp/vol-m3'

    cmd = [dmctl_bin, '-J', 'vol-mount', vol_id, mpath]
    run(cmd)
    v = get_vol(vol_id)
    assert_status(v['status'], True)
    assert v['status']['mount_path'] == mpath

    # Umount vol
    cmd = [dmctl_bin, '-J', 'vol-umount', vol_id]
    run(cmd)

    delete_vol(vol_id)


def vol_mount_remount_check(name, fs_type, mnt_policy, new_opts=[], good_opts=[], bad_opts=[]):
    global dmctl_bin

    vol_id = create_vol_mnt(name, fs_type, mnt_policy)
    mpath = '/tmp/%s' % name

    cmd = [dmctl_bin, '-J', 'vol-mount', vol_id, mpath]
    run(cmd)
    v = get_vol(vol_id)
    assert_status(v['status'], True)
    assert v['status']['mount_path'] == mpath
    v_dev = mount.find_by_path(mpath).fs_dev
    cmd = [dmctl_bin, '-J', 'vol-umount', vol_id]
    run(cmd)

    # Manually mount volume with different mount options
    r_mops = ','.join(new_opts)
    cmd = ['mount', v_dev, mpath, '-o%s' % r_mops]
    run(cmd)

    # Force daemon cache update and let daemon fix mount options
    cmd = [dmctl_bin, '-J', 'daemon-update-cache']
    run(cmd)

    # Check that daemon fixed mount options
    m_info = mount.find_by_path(mpath)
    log.debug("new_opts: gen_opts: {}, fs_opts:{}, opts:{}".format(m_info.gen_opts, m_info.fs_opts, m_info.opts))
    assert m_info is not None
    for o in good_opts:
        assert o in m_info.opts
    for o in bad_opts:
        assert o not in m_info.opts

    cmd = [dmctl_bin, '-J', 'vol-umount', vol_id]
    run(cmd)
    delete_vol(vol_id)


def test_06_vol_remount_default_1():
    vol_mount_remount_check('remount01-default', 'ext4', 'default',
                            ['data=ordered', 'nobarrier', 'nolazytime'],
                            ['lazytime'],
                            ['nobarrier'])


def test_06_vol_remount_default_2():
    vol_mount_remount_check('remount02-default', 'ext4', 'default',
                            ['data=ordered', 'discard'],
                            ['lazytime'],
                            ['discard'])


def test_06_vol_remount_default_3():
    vol_mount_remount_check('remount03-default', 'ext4', 'default',
                            ['data=ordered', 'lazytime', 'discard', 'nobarrier'],
                            ['lazytime'],
                            ['nobarrier', 'discard'])


def test_06_vol_remount04_unsafe():
    vol_mount_remount_check('remount04-unsafe', 'ext4', 'unsafe',
                            ['norecovery', 'discard'],
                            ['norecovery'],
                            ['discard'])


def test_06_vol_remount04_safe():
    vol_mount_remount_check('remount03-safe', 'ext4', 'safe',
                            ['suid', 'dev', 'lazytime'],
                            ['lazytime', 'nosuid', 'nodev'],
                            ['suid', 'dev'])


def test_07_vol_min_limits():
    global dmctl_bin

    lims = [str(consts.DEFAULT_IOLIM_READ_IOPS_MIN), str(consts.DEFAULT_IOLIM_READ_BPS_MIN),
            str(consts.DEFAULT_IOLIM_WRITE_IOPS_MIN), str(consts.DEFAULT_IOLIM_WRITE_BPS_MIN)]

    vol_id = create_vol_mnt('vol-lim')

    # check that volume limits are unset
    vol_info = get_vol_info(vol_id)
    for rw in ['read', 'write']:
        for stat in ['ops_per_second', 'bytes_per_second']:
            assert vol_info['status']['iolimit'][rw][stat] == '0'

    # set lowest possible limits
    cmd = [dmctl_bin, '-J', 'vol-set-iolimit', vol_id] + lims
    ret = subprocess.call(cmd)
    assert ret == 0

    # check that limits have expected values
    vol_info = get_vol_info(vol_id)
    i = 0
    for rw in ['read', 'write']:
        for stat in ['ops_per_second', 'bytes_per_second']:
            assert vol_info['status']['iolimit'][rw][stat] == lims[i]
            i = i + 1

    # check that no limit can have value lower than minimum
    for i in xrange(0, 4):
        lims_tmp = list(lims)
        lims_tmp[i] = str(int(lims_tmp[i]) - 1)

        cmd = [dmctl_bin, '-J', 'vol-set-iolimit', vol_id] + lims_tmp
        ret = subprocess.call(cmd)
        assert ret != 0

    delete_vol(vol_id)


def test_08_check_vpool_accounts_sys_vols():
    vg_name = 'diskman-vg-' + dev_id
    di1 = get_disk_info(dev_id)

    # Create non-system volume
    _ = create_vol_mnt('vol')
    di2 = get_disk_info(dev_id)

    # Check that capacity updated
    assert long(di1['status']['allocatable_bytes']) > long(di2['status']['allocatable_bytes'])
    # Check that vpool_capacity_bytes remains the same as volume is non-system
    assert long(di1['spec']['vpool_capacity_bytes']) == long(di2['spec']['vpool_capacity_bytes'])

    # Create system volume via lvm directly (without diskmanager knowing)
    LVM.create_lv('vol_sys', 1024 ** 3, vg_name, tags=['diskman.sys=true'])
    di3 = get_disk_info(dev_id)
    # Check that capacity and vpool_capacity_bytes haven't updated yet
    assert long(di2['status']['allocatable_bytes']) == long(di3['status']['allocatable_bytes'])
    assert long(di2['spec']['vpool_capacity_bytes']) == long(di3['spec']['vpool_capacity_bytes'])

    # Check that vpool_capacity_bytes accounts system volume size after cache update
    cmd = [dmctl_bin, '-J', 'daemon-update-cache']
    run(cmd)
    di3_2 = get_disk_info(dev_id)
    # Check that capacity updated
    assert long(di2['status']['allocatable_bytes']) > long(di3_2['status']['allocatable_bytes'])
    # Check that size of system volume is substracted from vpool_capacity_bytes
    assert long(di2['spec']['vpool_capacity_bytes']) > long(di3_2['spec']['vpool_capacity_bytes'])

    # Create second system volume via diskmanager
    _ = create_vol_mnt('vol_sys_2', label='diskman.sys=true')
    di4 = get_disk_info(dev_id)
    assert long(di3_2['status']['allocatable_bytes']) > long(di4['status']['allocatable_bytes'])
    assert long(di3_2['spec']['vpool_capacity_bytes']) > long(di4['spec']['vpool_capacity_bytes'])

    # Delete system volume
    # System volume should be deleted manually via LVM, because it's not manageable by diskman
    LVM.delete_lv('vol_sys_2', vg_name)
    di5 = get_disk_info(dev_id)
    # Check that vpool_capacity_bytes still accounts system volume size
    assert long(di5['status']['allocatable_bytes']) == long(di4['status']['allocatable_bytes'])
    assert long(di5['spec']['vpool_capacity_bytes']) == long(di4['spec']['vpool_capacity_bytes'])
    # Check that vpool_capacity_bytes stops accounting system volume size after cache update
    cmd = [dmctl_bin, '-J', 'daemon-update-cache']
    run(cmd)
    di5_2 = get_disk_info(dev_id)
    assert long(di5_2['status']['allocatable_bytes']) > long(di4['status']['allocatable_bytes'])
    assert long(di5_2['spec']['vpool_capacity_bytes']) > long(di4['spec']['vpool_capacity_bytes'])
    assert long(di5_2['status']['allocatable_bytes']) == long(di3_2['status']['allocatable_bytes'])
    assert long(di5_2['spec']['vpool_capacity_bytes']) == long(di3_2['spec']['vpool_capacity_bytes'])

    # Do/check all the same for another system volume
    LVM.delete_lv('vol_sys', vg_name)
    cmd = [dmctl_bin, '-J', 'daemon-update-cache']
    run(cmd)
    di6 = get_disk_info(dev_id)
    assert long(di6['status']['allocatable_bytes']) > long(di5_2['status']['allocatable_bytes'])
    assert long(di6['spec']['vpool_capacity_bytes']) > long(di5_2['spec']['vpool_capacity_bytes'])
    assert long(di6['status']['allocatable_bytes']) == long(di2['status']['allocatable_bytes'])
    assert long(di6['spec']['vpool_capacity_bytes']) == long(di2['spec']['vpool_capacity_bytes'])


def test_80_yasm_stat():
    global dmctl_bin
    global daemon_ts
    stat_wait = 5

    # Check yasmagent is available
    try:
        r = requests.get('http://%s:11003/json/' % socket.gethostname())
        r.raise_for_status()
    except Exception as ex:
        pytest.skip('Yasm agaent is not available, err:%s' % (str(ex), ))
        return

    delta = time.time() - daemon_ts
    # Ensure that daemon have sent some data already
    if delta < stat_wait:
        print 'wait %d seconds for stats' % (stat_wait - delta)
        time.sleep(stat_wait - delta)

    r = requests.get('http://%s:11003/json/' % socket.gethostname())
    assert r.status_code == requests.codes.ok
    data = r.json()
    print("Yasm response: ")
    print(json.dumps(data, indent=4))
    get_data = data["get"]

    diskman_data = get_data.get("diskmanager|ctype=prod", None)
    assert diskman_data is not None
    values = diskman_data.get('values', None)
    assert values is not None

    keys = ["push-remount_tmmm",
            "push-remount_errors_tmmm",
            "push-nr_disks_tmmm",
            "push-nr_disks_ready_tmmm",
            "push-nr_volumes_tmmm",
            "push-nr_volumes_mounts_tmmm",
            "push-nr_volumes_ready_tmmm",
            "push-volumes_size_GB_tmmm"]

    for k in keys:
        print 'lookup', k
        assert k in values.keys()


def test_81_daemon_stat():
    global dmctl_bin
    global daemon_ts
    stat_wait = 5
    delta = time.time() - daemon_ts

    # Ensure that daemon have sent some data already
    if delta < stat_wait:
        print 'wait %d seconds for stats' % (stat_wait - delta)
        time.sleep(stat_wait - delta)

    cmd = [dmctl_bin, '-J', 'daemon-stat']
    p = subprocess.Popen(cmd, stdout=subprocess.PIPE)
    data = json.load(p.stdout)
    print data
    assert p.wait() == 0
    assert int(data['fstrim_errors']) == 0
    assert int(data['remount_errors']) == 0
    assert int(data['remounts']) > 1


def test_99_check_logfile():
    global logfile
    global logfile_check_list
    f = open(logfile)
    s = mmap.mmap(f.fileno(), 0, access=mmap.ACCESS_READ)

    for token in logfile_check_list:
        print 'lookup', token
        assert s.find(token) != -1
