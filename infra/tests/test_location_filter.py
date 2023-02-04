import yaml
from google.protobuf import json_format
from infra.rtc.rebootctl.proto import reboot_pb2
from infra.rtc.rebootctl.lib import script
from hosts_data_source import hostlist


def test_get_location_filter():
    yml = '''---
    spec:
      limit:
        rack: '1R2'
    '''
    fltr = "host[\'location\'][\'rack\'] == \'1R2\'"

    m = reboot_pb2.Script()
    json_format.ParseDict(yaml.load(yml, Loader=yaml.CSafeLoader), m)
    assert script.get_location_filter(m.spec.limit) == fltr

    yml = '''---
    spec:
      limit:
        rack: '1D7'
        city: 'MANTSALA'
        switch: 'man1-s10'
    '''
    fltr = '''all((host['location']['city'] == 'MANTSALA', host['location']['rack'] == '1D7', host['location']['switch'] == 'man1-s10'))'''

    m = reboot_pb2.Script()
    json_format.ParseDict(yaml.load(yml, Loader=yaml.CSafeLoader), m)
    assert script.get_location_filter(m.spec.limit) == fltr


def test_make_location_filter():
    yml = '''---
    spec:
      limit:
        rack: '12'
    '''
    fltr = "host[\'location\'][\'rack\'] == \'12\'"

    m = reboot_pb2.Script()
    json_format.ParseDict(yaml.load(yml, Loader=yaml.CSafeLoader), m)
    assert script.get_location_filter(m.spec.limit) == fltr  # filter generated correctly
    assert script.make_location_filter(m.spec.limit)(hostlist[0])  # host 0 from data must pass filter
    assert not script.make_location_filter(m.spec.limit)(hostlist[3])  # host 3 from data must not pass filter

    yml = '''---
    spec:
      limit:
        rack: '12'
        city: 'Aboo'
        switch: 'aboodc1a-12'
    '''

    m = reboot_pb2.Script()
    json_format.ParseDict(yaml.load(yml, Loader=yaml.CSafeLoader), m)
    f = script.make_location_filter(m.spec.limit)
    assert f(hostlist[0])
    assert not f(hostlist[3])
    assert filter(f, hostlist) == [h for h in hostlist if h['location']['rack'] == '12' and h['location']['city'] == 'Aboo' and h['location']['switch'] == 'aboodc1a-12']

    yml = '''---
    spec:
      limit:
        rack: 'zzzz'
        city: 'Aboo'
        switch: 'aboodc1a-12'
    '''
    m = reboot_pb2.Script()
    json_format.ParseDict(yaml.load(yml, Loader=yaml.CSafeLoader), m)
    f = script.make_location_filter(m.spec.limit)
    assert filter(f, hostlist) == []


def test_location_filter_fm():
    yml = '''---
    spec:
      limit:
        rack: '12'
        city: 'Aboo'
        switch: 'aboodc1a-12'
    '''

    m = reboot_pb2.Script()
    json_format.ParseDict(yaml.load(yml, Loader=yaml.CSafeLoader), m)
    f = script.location_filter_fm(m.spec.limit)
    assert f(hostlist[0])
    assert not f(hostlist[3])
    assert filter(f, hostlist) == [h for h in hostlist if h['location']['rack'] == '12' and h['location']['city'] == 'Aboo' and h['location']['switch'] == 'aboodc1a-12']

    yml = '''---
    spec:
      limit:
        rack: 'zzzz'
        city: 'Aboo'
        switch: 'aboodc1a-12'
    '''
    m = reboot_pb2.Script()
    json_format.ParseDict(yaml.load(yml, Loader=yaml.CSafeLoader), m)
    f = script.location_filter_fm(m.spec.limit)
    assert filter(f, hostlist) == []
