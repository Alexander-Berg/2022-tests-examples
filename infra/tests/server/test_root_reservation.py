#!/usr/bin/env python

import logging
import mmap
import os
import pytest
import subprocess
import time
from infra.diskmanager.lib.targetcli import TargetCLI
from infra.diskmanager.lib import consts
from infra.diskmanager.tests.server import common

import yatest.common  # arcadia speciffic

tgt = None
daemon = None
diskmanager_bin = yatest.common.build_path("infra/diskmanager/server/diskmanager")
dmctl_bin = yatest.common.build_path("infra/diskmanager/client/dmctl")
log = logging.getLogger(__name__)
logfile = consts.DEFAULT_SERVER_LOG
logfile_check_list = []


class Volume(object):
    def __init__(self, name, rotational, vg_tags, vol_tags, wrong_reserve_perc, proper_reserve_blks):
        self.name = name
        self.rotational = rotational
        self.dev_name = 'hdd_'+self.name if rotational else 'ssd_'+self.name
        self.dev = None
        self.vg = 'vg_' + self.name
        self.vg_tags = vg_tags
        self.vol_tags = vol_tags
        self.vol_path = os.path.join("/dev", self.vg, name)
        self.vol_mnt_path = '/' + name
        self.wrong_reserve_perc = str(wrong_reserve_perc)
        self.proper_reserve_blks = str(proper_reserve_blks)


# hard to calculate exact nr of blocks here (if other than 0), but aproximately (actually slightly less than): vg_size / block_size * reserved_perc
vols = [Volume('ssd', False, None, None, 1, 0),
        # Volume('place', True, 'diskman=true', 'diskman.sys=true', 1, 0),  # /place gets remounted inside test vm and test fails to find diskman and others binaries
        Volume('home', True, ['diskman=true'], ['diskman.sys=true'], 10, 12595),  # (1 * 2^30) / 4096 * 0.05
        Volume('mnt', True, ['diskman=true'], ['diskman.access_type=mount', 'diskman.fstrim=0', 'diskman.mount_policy=DEFAULT', 'diskman=true'], 3, 12595)]  # maybe add vol_tag 'diskman.fs_type=ext4'


def add_check_list(c_string):
    global logfile_check_list

    logfile_check_list.append(c_string)


def do_cmd(cmd):
    print 'exec cmd :%s' % cmd
    subprocess.check_call(cmd)


def do_cmd_output(cmd):
    print 'exec cmd :%s' % cmd
    return subprocess.check_output(cmd)


def setup_module(module):
    global vols
    global tgt
    global diskmanager_bin
    global dmctl_bin
    global daemon
    global logfile_check_list

    if not os.geteuid() == 0:
        pytest.skip('Not privileged user')
        return

    assert os.path.exists(diskmanager_bin)
    assert os.path.exists(dmctl_bin)
    common.prepare_utils(yatest.common.build_path)

    try:
        tgt = TargetCLI()
        for vol in vols:
            vol.dev = tgt.create_fileio_dev(vol.dev_name, '1G', rotational=vol.rotational)
    except OSError as e:
        pytest.skip('Failed to create test device, msg:%s' % str(e))

    # create vg and lv without diskman tags for system volume /ssd
    # create vg and lv with diskman tags for system volumes /home, /place and non-system /mnt
    try:
        for vol in vols:
            do_cmd(['pvcreate', vol.dev])

            cmd = ['vgcreate', vol.vg, vol.dev]
            if vol.vg_tags:
                for tag in vol.vg_tags:
                    cmd.extend(['--addtag', tag])
            do_cmd(cmd)

            cmd = ['lvcreate', '-l', '100%FREE', '-n', vol.name, vol.vg]
            if vol.vol_tags:
                for tag in vol.vol_tags:
                    cmd.extend(['--addtag', tag])
            do_cmd(cmd)

            do_cmd(['mkfs.ext4', '-F', vol.vol_path])

            # mount, so that even system volumes w/o tags will be discovered and fixed
            if not os.path.exists(vol.vol_mnt_path):
                os.makedirs(vol.vol_mnt_path)
            do_cmd(['mount', vol.vol_path, vol.vol_mnt_path])
    except OSError as e:
        pytest.skip('Failed to prepare test device, msg:%s' % str(e))

    # set wrong root reservation
    for vol in vols:
        do_cmd(['tune2fs', '-m', vol.wrong_reserve_perc, vol.vol_path])
        add_check_list("fixing reserved blocks on '%s' to %s" % (vol.vol_path, vol.proper_reserve_blks))

    # start diskman service
    if os.path.exists(logfile):
        os.remove(logfile)
    cmd = [diskmanager_bin, '--log_size_mb', '1', '-v', '-v', '-v', '-v',
           '--stat-period', '1', '--fstrim-period', '10']
    daemon = subprocess.Popen(cmd, stdout=subprocess.PIPE)
    time.sleep(2)


def teardown_module(module):
    global vols
    global tgt

    for vol in vols:
        do_cmd(['umount', vol.vol_mnt_path])

    if tgt is not None:
        tgt.clear_all()
    if daemon is not None:
        daemon.terminate()
        print daemon.stdout.read()


def test_check_root_reservation():
    global vols
    global dmctl_bin

    # even though, daemon on start should already fix reservation,
    # force daemon cache update just to be sure
    cmd = [dmctl_bin, '-J', 'daemon-update-cache']
    do_cmd(cmd)

    # check that root reservation is correct
    for vol in vols:
        log.info("checking reservation on {}".format(vol.vol_path))
        str_found = False
        # try to found and parse str like: "Reserved block count:     514423\n"
        for s in do_cmd_output(['tune2fs', '-l', vol.vol_path]).split('\n'):
            if "Reserved block count:" in s:
                str_found = True
                assert s.split()[3] == vol.proper_reserve_blks
                break
        assert str_found is True


def test_999_check_logfile():
    global logfile
    global logfile_check_list
    f = open(logfile)
    s = mmap.mmap(f.fileno(), 0, access=mmap.ACCESS_READ)
    print("logfile_data:  {}".format(f.read()))
    for token in logfile_check_list:
        print 'lookup', token
        assert s.find(token) != -1
