import datetime
import os
import sys
from pprint import pprint

from library.python import resource


def from_file(fn):
    return resource.find("/" + fn)


def test_isoformat():
    from ya.infra.oops.agent.modules.abstract import iso_date

    a = iso_date()
    assert a.endswith('Z')
    a = iso_date(datetime.datetime(2013, 12, 12, 10, 9, 8))
    assert a == '2013-12-12T10:09:08Z'
    a = iso_date(1394198254.0)
    assert a == '2014-03-07T13:17:34Z'
    a = iso_date(-10)
    assert a.endswith('Z')


def test_cpuinfo_none():
    from ya.infra.oops.agent.modules.cpuinfo import AgentModule

    res = AgentModule('aaa').get_value()
    assert res is None, "result is %s" % res


def test_loadavg_none():
    from ya.infra.oops.agent.modules.loadavg import AgentModule

    res = AgentModule('aaa').get_value()
    assert res is None, "result is %s" % res


def test_mapreduce_none():
    from ya.infra.oops.agent.modules.mapreduce import AgentModule

    res = AgentModule('aaa').get_value()
    assert res is None, "result is %s" % res


def test_sensors_none():
    from ya.infra.oops.agent.modules.sensors import AgentModule

    res = AgentModule('aaa').get_value()
    assert res is None, "result is %s" % res


def test_sysinfo_none():
    from ya.infra.oops.agent.modules.sysinfo import AgentModule

    res = AgentModule('aaa').get_value()
    assert res == [], "result is %s" % res


def test_disk_usage_df():
    from ya.infra.oops.agent.modules.disk_usage import get_df

    res = get_df(lines=from_file('df1.out').split('\n'))
    assert len(res) == 4, len(res)

    res = get_df(lines=from_file('df2.out').split('\n'))
    assert len(res) == 2, res

    res = get_df(lines=from_file('df_tsuga.out').split('\n'))
    assert len(res) == 2, res


def test_disk_usage_du():
    from ya.infra.oops.agent.modules.disk_usage import AgentModule

    m = AgentModule('aaa')
    os.environ['HOME'] = '/home/users/user-dummy'
    mp, res = m.read_du_file(lines=from_file('du.out').split('\n'))
    assert mp == '/home/users'
    assert len(res) == 2
    assert 'user1' in res.keys()
    assert res['user1'] == 77797 * 1024 * 1024


def test_disks_rtc():
    from ya.infra.oops.agent.modules.disks_rtc import AgentModule

    m = AgentModule(sys.platform)
    data = m.get_value()
    for d in data['data']['disks']:
        pprint("validate disk: {}".format(d))
        for k in ['name', 'hwInfo', 'raidLevel', 'partitionUuid', 'fsSize', 'deviceNumber', 'slaves', 'device', 'mountPoint', 'type', 'size']:
            assert k in d


def test_dmi():
    from ya.infra.oops.agent.modules.dmidecode import parse_dmi

    d = parse_dmi(from_file('dmidecode.out').split('\n'))
    pprint(d)
    assert 'System Information' in d
    assert 'Product Name' in d['System Information']


def test_bus():
    from ya.infra.oops.agent.agent.bus import Bus

    bus = Bus(None)
    bus.last_get = {'a': 100, 'b': 99, 'c': 101}
    bus.data = {'a': {}, 'b': {}, 'c': {}}

    d = bus.get_data(100)
    assert len(d.keys()) == 2, d.keys()

    d = bus.get_data(101.1)
    assert len(d.keys()) == 0, d.keys()
