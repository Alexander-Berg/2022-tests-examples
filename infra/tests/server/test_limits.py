#!/usr/bin/env python

from mock import mock_open
from infra.diskmanager.lib.limit import IOLimitsForYPExport
from infra.diskmanager.lib import consts

DEFAULT_MOCK = mock_open(read_data='''\
    {
        "DISK1": {
            "rr_iops": 111,
            "rw_iops": 222,
            "sr_bandwidth": 333,
            "sw_bandwidth": 444
        }
    }
    ''')

DEFAULT_UPDATE_MOCK = mock_open(read_data='''\
    {
        "DISK1": {
            "rr_iops": 555,
            "rw_iops": 666,
            "sr_bandwidth": 777,
            "sw_bandwidth": 888
        }
    }
    ''')


class DiskMock(object):
    def __init__(self, storage_class=consts.STORAGE_NVME, model='DISK_MODEL', serial='DISK_SERIAL'):
        self.storage_class = storage_class
        self.model = model
        self.serial = serial


def _assert_valid_values(o, _type='default_mock_values'):
    assert o.use_override
    if _type == 'default_mock_values':
        assert o.rr_iops == 111
        assert o.rw_iops == 222
        assert o.sr_bandwidth == 333
        assert o.sw_bandwidth == 444
    elif _type == 'default_update_mock_values':
        assert o.rr_iops == 555
        assert o.rw_iops == 666
        assert o.sr_bandwidth == 777
        assert o.sw_bandwidth == 888


def test_run_with_no_config():
    disk = DiskMock(storage_class=consts.STORAGE_HDD, model='DISK1')
    # start without limits
    io_limits = IOLimitsForYPExport(config_file='UnExistantFile.json')
    assert io_limits.get(disk) is None

    # then it's appearing
    io_limits.update(open_func=DEFAULT_MOCK)
    _assert_valid_values(io_limits.get(disk))


def test_run_with_invalid_json():
    disk = DiskMock(storage_class=consts.STORAGE_HDD, model='DISK1')
    open_mock = mock_open(read_data='{\n')
    io_limits = IOLimitsForYPExport(open_func=open_mock)
    assert io_limits.get(disk) is None


def test_run_with_invalid_schema():
    disk1 = DiskMock(storage_class=consts.STORAGE_HDD, model='DISK1')
    disk2 = DiskMock(storage_class=consts.STORAGE_HDD, model='DISK2')
    open_mock = mock_open(read_data='''\
    {
        "DISK1": {
            "rr_iops": 111,
            "rw_iops": 222,
            "sr_bandwidth": 333,
            "sw_bandwidth": 444
        },
        "DISK2": {
            "rr_iops": 111,
            "rw_iops": 222,
            "sr_bandwidth": 333,
            "sw_bandwidth": 444,
            "invalid_field": "lol-kek"
        }
    }
    ''')
    io_limits = IOLimitsForYPExport(open_func=open_mock)
    _assert_valid_values(io_limits.get(disk1))
    assert io_limits.get(disk2) is None


def test_reuse_cache():
    disk = DiskMock(storage_class=consts.STORAGE_HDD, model='DISK1')
    io_limits = IOLimitsForYPExport(config_file='some_awesome.json', open_func=DEFAULT_MOCK)
    _assert_valid_values(io_limits.get(disk))
    assert io_limits._config_file == 'some_awesome.json'

    new_io_limits = IOLimitsForYPExport(cache=io_limits)
    _assert_valid_values(new_io_limits.get(disk))
    assert new_io_limits._config_file == 'some_awesome.json'


def test_substr_model_name():
    disk1 = DiskMock(storage_class=consts.STORAGE_HDD, model='DISK')
    disk2 = DiskMock(storage_class=consts.STORAGE_HDD, model='LONG-LONG DISK1 rev123 model')
    io_limits = IOLimitsForYPExport(open_func=DEFAULT_MOCK)
    # allowing substr as a key in config (same as yp does, see comments is YP-631)
    _assert_valid_values(io_limits.get(disk2))
    # but not vice versa
    assert io_limits.get(disk1) is None


def test_valid_limits_update():
    disk1 = DiskMock(storage_class=consts.STORAGE_HDD, model='DISK1')
    disk2 = DiskMock(storage_class=consts.STORAGE_HDD, model='DISK2')
    # start with valid data
    io_limits = IOLimitsForYPExport(open_func=DEFAULT_MOCK)
    _assert_valid_values(io_limits.get(disk1))
    assert io_limits.get(disk2) is None

    # update with valid data
    io_limits.update(open_func=DEFAULT_UPDATE_MOCK)
    _assert_valid_values(io_limits.get(disk1), 'default_update_mock_values')


def test_valid_limits_bad_update():
    disk = DiskMock(storage_class=consts.STORAGE_HDD, model='DISK1')
    # start with valid data
    io_limits = IOLimitsForYPExport(open_func=DEFAULT_MOCK)
    _assert_valid_values(io_limits.get(disk))

    # bad update is coming
    open_mock = mock_open(read_data='{\n')
    io_limits.update(open_func=open_mock)
    _assert_valid_values(io_limits.get(disk))

    # good fix is coming
    io_limits.update(open_func=DEFAULT_UPDATE_MOCK)
    _assert_valid_values(io_limits.get(disk), 'default_update_mock_values')


def test_valid_limits_deletion():
    disk = DiskMock(storage_class=consts.STORAGE_HDD, model='DISK1')
    # start with valid data
    io_limits = IOLimitsForYPExport(open_func=DEFAULT_MOCK)
    _assert_valid_values(io_limits.get(disk))

    # no data for DISK1 in update
    open_mock = mock_open(read_data='{}')
    io_limits.update(open_func=open_mock)
    assert io_limits.get(disk) is None


def test_model_name_matching_for_storage_class():
    # using ID_SERIAL for nvme and ID_MODEL for others
    disk_hdd = DiskMock(storage_class=consts.STORAGE_HDD, model='MODEL_DISK1', serial='SERIAL_DISK1')
    disk_ssd = DiskMock(storage_class=consts.STORAGE_SSD, model='MODEL_DISK2', serial='SERIAL_DISK2')
    disk_nvme = DiskMock(storage_class=consts.STORAGE_NVME, model='MODEL_DISK3', serial='SERIAL_DISK3')

    open_mock = mock_open(read_data='''\
    {
        "MODEL_DISK1": {
            "rr_iops": 111,
            "rw_iops": 222,
            "sr_bandwidth": 333,
            "sw_bandwidth": 444
        },
        "MODEL_DISK2": {
            "rr_iops": 111,
            "rw_iops": 222,
            "sr_bandwidth": 333,
            "sw_bandwidth": 444
        },
        "SERIAL_DISK3": {
            "rr_iops": 111,
            "rw_iops": 222,
            "sr_bandwidth": 333,
            "sw_bandwidth": 444
        }
    }
    ''')
    io_limits = IOLimitsForYPExport(open_func=open_mock)

    hdd_limits = io_limits.get(disk_hdd)
    _assert_valid_values(hdd_limits)

    ssd_limits = io_limits.get(disk_ssd)
    _assert_valid_values(ssd_limits)

    nvme_limits = io_limits.get(disk_nvme)
    _assert_valid_values(nvme_limits)
