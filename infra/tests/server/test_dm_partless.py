#!/usr/bin/env python

# By historical means yandex has various generations of disk-layout configurations
# This test-suit aims to test that diskmanager works correctly on such configs
#
# Partitionless disks

import json
import mmap
import os
import pytest
import requests
import socket
import subprocess
import time
import uuid
from infra.diskmanager.lib.targetcli import TargetCLI
from infra.diskmanager.lib import consts
from infra.diskmanager.tests.server import common
import yatest.common  # arcadia speciffic

dev = None
dev_id = None
dev_mnt = '/mnt/m1'
dev_uuid = str(uuid.uuid4())
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
        do_cmd(['mkfs.ext4', '-F', '-U', dev_uuid, dev])
        if not os.path.exists(dev_mnt):
            os.makedirs(dev_mnt)
        do_cmd(['mount', dev, dev_mnt])
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


def test_01_fstrim_partitionless_disk():
    global dmctl_bin
    global dev_mnt
    global dev_uuid
    # Let fstrim worker to find this volume
    time.sleep(5)
    add_check_list(' Start volume: {}, mount at: {}'.format(dev_uuid, dev_mnt))


def test_980_yasm_stat():
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

    keys = ["push-nr_disks_tmmm",
            "push-nr_disks_ready_tmmm",
            "push-nr_volumes_tmmm",
            "push-nr_volumes_mounts_tmmm",
            "push-nr_volumes_ready_tmmm",
            "push-volumes_size_GB_tmmm"]

    for k in keys:
        print 'lookup', k
        assert k in values.keys()

    fstrim_data = get_data.get("diskmanager|ctype=prod;disk={}".format(os.path.basename(dev)), None)
    assert fstrim_data is not None
    fstrim_values = fstrim_data.get('values', None)
    assert fstrim_values is not None

    fstrim_keys = ["push-fstrim_bytes_tmmm",
                   "push-fstrim_errors_tmmm",
                   "push-fstrim_loops_tmmm",
                   "push-fstrim_scan_bytes_tmmm"]

    for k in fstrim_keys:
        print 'fstrim lookup', k
        assert k in fstrim_values.keys()

    fstrim_bytes = fstrim_values['push-fstrim_bytes_tmmm']
    print 'fstrim stats:', fstrim_bytes
    assert fstrim_bytes > 1024 ** 2


def test_981_daemon_stat():
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
    assert int(data['fstrim_bytes']) > 0
    assert int(data['fstrim_loops']) > 0
    assert int(data['fstrim_errors']) == 0


def test_999_check_logfile():
    global logfile
    global logfile_check_list
    f = open(logfile)
    s = mmap.mmap(f.fileno(), 0, access=mmap.ACCESS_READ)
    print('Log content:')
    print(s)
    for token in logfile_check_list:
        print 'lookup', token
        assert s.find(token) != -1
