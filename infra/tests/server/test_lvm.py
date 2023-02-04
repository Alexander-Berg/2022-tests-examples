#!/usr/bin/env python

import os
import pytest
from infra.diskmanager.lib.lvm import LVM
from infra.diskmanager.lib.targetcli import TargetCLI
from infra.diskmanager.lib import logger

dev = None
tgt = None


def setup_module(module):
    global dev
    global tgt

    if not os.geteuid() == 0:
        pytest.skip('Not privileged user')
        return
    logger.setup_logger('', 7)
    dev = os.getenv('DISKMAN_TEST_DEV')
    if not dev:
        tgt = TargetCLI()
        dev = tgt.create_fileio_dev('test_lvm', '10G')


def teardown_module(module):
    global tgt
    if tgt is not None:
        tgt.clear_all()


def test_01_create_vg():
    LVM.create_vg('fake-2', pv_list=[dev], tags=['tag1=val', 'tag2=val'])
    LVM.create_lv('fake-2-lv1', 1024 * 1024 * 1024, 'fake-2', tags=['tag3=val3', 'tag4=val3'])


def test_02_list_vg():
    keys = ['vg_name', 'vg_tags', 'vg_size']
    vg_list = LVM.list_vg()
    for vg in vg_list:
        for k in keys:
            print 'lookup', k
            assert k in vg.keys()


def test_02_list_lv():
    keys = ['lv_name', 'lv_tags', 'lv_size']
    lv_list = LVM.list_lv()
    for lv in lv_list:
        for k in keys:
            print 'lookup', k
            assert k in lv.keys()


def test_02_list_pv():
    keys = ['pv_name', 'pv_tags', 'pv_size']
    pv_list = LVM.list_pv()
    for pv in pv_list:
        for k in keys:
            print 'lookup', k
            assert k in pv.keys()


def test_03_delete_vg():
    LVM.delete_lv('fake-2-lv1', 'fake-2', force=True)
    LVM.delete_vg('fake-2')


def test_10_change_tags():
    LVM.create_vg('testvg-2', pv_list=[dev], tags=['tag1=val1', 'tag2=val2'])
    LVM.create_lv('v1', 1024 * 1024 * 1024, 'testvg-2', tags=['tag3=val3', 'tag4=val4'])

    LVM.change_vg_tags('testvg-2', add_tags=['tag1=val11'],  del_tags=['tag1=val1'])
    LVM.change_lv_tags('v1', 'testvg-2', add_tags=['tag3=val33'],  del_tags=['tag3=val3'])

    vg_list = LVM.list_vg('testvg-2')
    assert (len(vg_list) == 1)
    vg_info = vg_list[0]
    assert vg_info['vg_tags'].get('tag1') == 'val11'
    assert vg_info['vg_tags'].get('tag2') == 'val2'

    lv_list = LVM.list_lv('testvg-2/v1')
    assert (len(lv_list) == 1)
    lv_info = lv_list[0]
    assert lv_info['lv_tags'].get('tag3') == 'val33'
    assert lv_info['lv_tags'].get('tag4') == 'val4'

    LVM.delete_lv('v1', 'testvg-2')
    LVM.delete_vg('testvg-2')
