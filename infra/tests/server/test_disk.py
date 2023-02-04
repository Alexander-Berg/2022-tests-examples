#!/usr/bin/env python

import os
import pytest
from infra.diskmanager.lib.disk import Disk
from infra.diskmanager.lib.targetcli import TargetCLI
from infra.diskmanager.lib import logger

dev = None
tgt = None
log = None


def setup_module(module):
    global dev
    global tgt
    global log

    if not os.geteuid() == 0:
        pytest.skip('Not privileged user')
        return
    log = logger.setup_logger('', 7)
    dev = os.getenv('DISKMAN_TEST_DEV')
    if not dev:
        try:
            tgt = TargetCLI()
            dev = tgt.create_fileio_dev('test_lvm', '10G')
        except OSError as e:
            pytest.skip('Fail to create test devices, msg:%s' % str(e))


def teardown_module(module):
    global tgt
    if tgt is not None:
        tgt.clear_all()


def test_list_disks():
    global dev

    disk_list = Disk.list_disks()
    assert len(disk_list) > 0

    found = False
    for d in disk_list:
        dpath = '/dev/' + d
        if dpath == dev:
            found = True

    assert found is True


def test_list_disks2():
    di_list = []
    for d in Disk.list_disks():
        di_list.append(Disk(d))

    assert len(di_list) > 0


def test_disk_fields():
    global dev

    d = Disk(os.path.basename(dev))
    keys = ['ID_TYPE', 'ID_MODEL', 'ID_SERIAL']
    for k in keys:
        log.info("lookup {}".format(k))
        assert k in d._udev_info


def test_disk_id():
    nvme = Disk("nvme0n1")
    log.debug("nvme_id: {}".format(nvme.id))
    assert nvme.id == "nvme-serial"

    global dev
    d = Disk(os.path.basename(dev))
    log.debug("disk_id: {}".format(d.id))

    d.hw_uuid = 'uuid-34434130-4e30-8142-0025-384100000004'
    d.wwid = 'eui.344341304e3081420025384100000004'
    d.wwn = '0x5000cca39ad1fcfc'
    d.serial = 'Hitachi_HUA722010CLA330_JPW9H0N018KR8V'

    d.infer_id()
    log.debug("disk_id: {}".format(d.id))
    assert d.id == d.hw_uuid

    d.hw_uuid = None
    d.infer_id()
    log.debug("disk_id: {}".format(d.id))
    assert d.id == d.wwid

    d.wwid = None
    d.infer_id()
    log.debug("disk_id: {}".format(d.id))
    assert d.id == 'wwn-' + d.wwn

    d.wwn = None
    d.infer_id()
    log.debug("disk_id: {}".format(d.id))
    assert d.id == d.serial

    d.serial = None
    d.infer_id()
    log.debug("disk_id: {}".format(d.id))
    assert d.id == 'virt-' + d.name + '_t' + str(d.usec_init)
