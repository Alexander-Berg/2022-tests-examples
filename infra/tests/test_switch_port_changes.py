#!/usr/bin/env python

import logging
import os
import json
import mock
from itertools import izip

from infra.netconfig.checks import switch_port_changes

logger = logging.getLogger('SwitchPortTest')

# full ethernet frames
eth_lldp_packet = (
    b'\x01\x80\xc2\x00\x00\x0e\xbc\x9c1\xaay\x81\x88\xcc\x02\x07\x04\xbc\x9c1\xaay\x81\x04\t\x05GE1/0/31\x06\x02'
    '\x00x\x08\x01\\\n\tsas1-s297\x0c\xbbHuawei Versatile Routing Platform Software\r\n'
    'VRP (R) software, Version 8.120 (CE5850HI V200R001C00SPC700)\r\nCopy'
    'right (C) 2012-2016 Huawei Technologies Co., Ltd.\r\nHUAWEI CE5850-48T4S2Q-HI\r\n\x0e\x04\x00\x14\x00\x14\x10'
    '\x1d\x05\x01_l\xcc\xc2\x02\x00\x00\x00\x04\x11\x06\x0f+\x06\x01\x04\x01\x8f[\x05\x19)\x01\x02\x01\x01\x01\xfe\x06\x00\x80\xc2\x01\x02\\\xfe\x07'
    '\x00\x80\xc2\x02\x00\x00\x00\xfe\x0e\x00\x80\xc2\x03\x02\\\x07VLAN604\xfe\t\x00\x12'
    '\x0f\x01\x03\x83\xa4\x00\x16\xfe\t\x00\x12\x0f\x03\x01\x00\x00\x00\x00\xfe\x06\x00\x12\x0f\x04%\xf0\x00\x00'
)

eth_lldp_packet_new = (
    b'\x01\x80\xc2\x00\x00\x0e\x04\x9f\xca\x0bu\xe1\x88\xcc\x02\x07\x04\x04\x9f\xca\x0bu\xe1'
    '\x04\t\x05GE1/0/23\x06\x02\x00x\x08\x01\\\n\tsas1-s225\x0c\xbbHuawei Versatile Routing Platform Software\r\n'
    'VRP (R) software, Version 8.120 (CE5850HI V200R001C00SPC700)\r\nCopy'
    'right (C) 2012-2016 Huawei Technologies '
    'Co., Ltd.\r\nHUAWEI CE5850-48T4S2Q-HI\r\n\x0e\x04\x00\x14\x00\x14\x10\x1d\x05\x01%\x8c\x92\xc7\x02\x00\x00\x00'
    '\x04\x11\x06\x0f+\x06\x01\x04\x01\x8f[\x05\x19)\x01\x02\x01\x01\x01\xfe\x06\x00\x80\xc2\x01\x02\\\xfe\x07\x00'
    '\x80\xc2\x02\x00\x00\x00\xfe\x0e\x00\x80\xc2\x03\x02\\\x07VLAN604\xfe\t\x00\x12\x0f\x01\x03\x83\xa4\x00\x16'
    '\xfe\t\x00\x12\x0f\x03\x01\x00\x00\x00\x00\xfe\x06\x00\x12\x0f\x04%\xf0\x00\x00'
)

mock_host64ifnames = {
    "GE1/0/22": {
        "index": 21,
        "link-local": "fe80::a:15"
    },
    "GE1/0/23": {
        "index": 22,
        "link-local": "fe80::a:16"
    },
    "GE1/0/24": {
        "index": 23,
        "link-local": "fe80::a:17"
    },
    "GE1/0/25": {
        "index": 24,
        "link-local": "fe80::a:18"
    },
    "GE1/0/26": {
        "index": 25,
        "link-local": "fe80::a:19"
    },
    "GE1/0/27": {
        "index": 26,
        "link-local": "fe80::a:1a"
    },
    "GE1/0/28": {
        "index": 27,
        "link-local": "fe80::a:1b"
    },
    "GE1/0/29": {
        "index": 28,
        "link-local": "fe80::a:1c"
    },
    "GE1/0/30": {
        "index": 29,
        "link-local": "fe80::a:1d"
    },
    "GE1/0/31": {
        "index": 30,
        "link-local": "fe80::a:1e"
    },
    "GE1/0/32": {
        "index": 31,
        "link-local": "fe80::a:1f"
    },
}
mock_host64ifnames = json.dumps(mock_host64ifnames)


def test_switch_port_changes(tmpdir):
    state = '1e'
    interface = 'eth0'
    state_file_postfix = 'switch_port'
    state_file = '.'.join((interface, state_file_postfix))

    tmpfile = tmpdir.join(state_file)
    path = str(tmpdir)

    host64_tmp_file = tmpdir.join('host64.tmp')
    host64_tmp_file.write(mock_host64ifnames)

    with mock.patch('infra.netconfig.lib.jugglerutil.push_local') as m:
        def ensure_invocation(call_args, need_service, need_status, predicate):
            logger.info('call args: {}'.format(call_args))
            (status, descr, service, tags), _ = call_args
            assert service == need_service
            assert status == need_status and predicate(descr), "Unexpected event generated: %s - %s" % (status, descr)

        def ensure_events(mock_util, need_list):
            logger.info('Mock.call_args_list: {}'.format(mock_util.call_args_list))
            assert len(mock_util.call_args_list) == len(need_list), "Unexpected number of invocations: %d vs %d" % (len(mock_util.call_args_list), len(need_list))

            for (c, n) in izip(mock_util.call_args_list, need_list):
                ensure_invocation(c, *n)

        # No state file => Push OK (RTCNETWORK-17)
        assert switch_port_changes.run(path)
        ensure_events(m, [
            ('netconfig_switch_port_changes', 'OK', lambda x: 'No switch_port files' in x),
        ])

        tmpfile.write(state)
        assert os.path.exists(path)

        # Can't open file to read
        m.reset_mock()
        os.chmod(str(tmpfile), 0000)
        assert not switch_port_changes.run(path)
        ensure_events(m, [
            ('netconfig_switch_port_changes', 'CRIT', lambda x: 'Got exception' in x)
        ])
        os.chmod(str(tmpfile), 0644)

        # New port
        m.reset_mock()
        with mock.patch('infra.netconfig.lib.lldputil.run_lldp_sniff', return_value=eth_lldp_packet_new[14:]):
            assert not switch_port_changes.run(path, str(host64_tmp_file))
            ensure_events(m, [
                ('netconfig_switch_port_changes', 'CRIT', lambda x: 'Switch/Port has been changed' in x)
            ])

        # OK
        m.reset_mock()
        with mock.patch('infra.netconfig.lib.lldputil.run_lldp_sniff', return_value=eth_lldp_packet[14:]):
            assert switch_port_changes.run(path, str(host64_tmp_file))
            ensure_events(m, [
                ('netconfig_switch_port_changes', 'OK', lambda x: x == '')
            ])
