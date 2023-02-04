#!/usr/bin/env python

import mock
# import __builtin__
import unittest
import logging
import socket
from infra.netconfig.lib import master
from infra.netconfig.lib import fixutil
from pyroute2.netlink.rtnl.fibmsg import fibmsg

logger = logging.getLogger()


class TestGetHost64Address(unittest.TestCase):
    def setUp(self):
        self.iface = 'eth0'
        self.project_id = '604'
        self.port_index = '9'
        self.port_none = None
        self.mocked_prefix_ok = '2a02:6b8:c08:5e00::/57'
        self.mocked_prefix_len_not_ok = '2a02:6b8:c08:5e00::/64'
        self.mocked_prefix_not_ok = 'zzzz::/64'
        self.want_result = {
            'mtn_prefix': self.mocked_prefix_ok,
            'mtn_global': '2a02:6b8:c08:5e09::badc:ab1e/64',
            'mtn_local': 'fe80::a:9/64',
            'mtn_net': '2a02:6b8:c08:5e09::/64',
        }

    def test_prefix_not_ok(self):
        with mock.patch('infra.netconfig.lib.master.get_ra_prefix', return_value=self.mocked_prefix_not_ok):
            with self.assertRaises(master.NetconfigError):
                master.get_host64_address(self.iface, self.port_index)

    def test_none_port(self):
        with mock.patch('infra.netconfig.lib.master.get_ra_prefix', return_value=self.mocked_prefix_ok):
            self.assertIs(None, master.get_host64_address(self.iface, self.port_none))

    def test_prefix_len_not_ok(self):
        with mock.patch('infra.netconfig.lib.master.get_ra_prefix', return_value=self.mocked_prefix_len_not_ok):
            with self.assertRaises(master.NetconfigError):
                master.get_host64_address(self.iface, self.port_index)

    def test_ok_result(self):
        with mock.patch('infra.netconfig.lib.master.get_ra_prefix', return_value=self.mocked_prefix_ok):
            got_result = master.get_host64_address(self.iface, self.port_index)
            for k, v in got_result.items():
                self.assertEqual(self.want_result[k], v)


class TestMacHostids(unittest.TestCase):
    def setUp(self):
        self.macs_hostids = (
            ('8c:16:45:f4:37:1e', int('45f4371e', 16)),
            ('90:2b:34:c1:d0:6a', int('34c1d06a', 16)),
            ('90:2b:34:cf:41:60', int('34cf4160', 16)),
        )

    def test_get_mac_hostid_ok(self):
        for mac_hostid in self.macs_hostids:
            hostid = master.get_mac_hostid(mac_hostid[0])
            self.assertEqual(mac_hostid[1], hostid)

    def test_get_mac_hostid_len(self):
        with self.assertRaises(Exception):
            master.get_mac_hostid('')
            master.get_mac_hostid('90:2b:34:cf:41:600')


def create_interface(tmpdir, content=None):
    if not content:
        content = """
auto lo
iface lo inet loopback

auto eth0
iface eth0 inet6 auto
  privext 0
  ya-netconfig-networks-url https://noc-export.yandex.net/rt/l3-segments2.json
  mtu 9000
  debug yes
  ya-netconfig-project-id-host-method mac
  ya-netconfig-set-group yes
  project-id 604
  pre-up /sbin/ethtool -K $IFACE lro off tso off
"""

    network_interfaces = str(tmpdir.join('interfaces'))
    with open(network_interfaces, 'w') as f:
        f.write(content)

    interfaces = fixutil.get_interfaces(network_interfaces)
    interfaces = [iface for iface in interfaces if iface.name != 'lo']
    assert len(interfaces) == 1
    iface = interfaces[0]
    return iface


def test_get_project_id(tmpdir):
    iface_obj = create_interface(tmpdir)
    logging.info('IFACE: {}'.format(iface_obj.show()))
    assert master.get_project_id(iface_obj) == '604'

    bad_content = """
iface eth0 inet6 auto
  privext 0
  ya-netconfig-networks-url https://noc-export.yandex.net/rt/l3-segments2.json
"""

    iface_obj = create_interface(tmpdir, content=bad_content)
    assert not iface_obj.option('project-id')

    master.NETWORK_CONFIG_PROJECT_ID = str(tmpdir.join('project_id'))
    with open(master.NETWORK_CONFIG_PROJECT_ID, 'w') as f:
        f.write('604')

    assert master.get_project_id(iface_obj) == '604'


def test_get_hostname_hostid():
    hostname_hostids = (
        ('sas1-8555.search.yandex.net', int('db00e07d', 16)),
        ('sas1-8487.search.yandex.net', int('6a27edd9', 16)),
        ('vla1-2786.search.yandex.net', int('28e41643', 16)),
    )

    for hostname_hostid in hostname_hostids:
        assert master.get_hostname_hostid(hostname_hostid[0]) == hostname_hostid[1]


def test_get_manual_hostid():
    manual_hostids = (
        ('deadbeef', int('deadbeef', 16)),
        ('vla1-2786.search.yandex.net', int('28e41643', 16)),
    )

    for manual_hostid in manual_hostids:
        assert master.get_manual_hostid(manual_hostid[0]) == manual_hostid[1]


def test_get_projectid_address():

    mock_ra_prefix = 'aaaa:bbb:ccc:dd::/64'
    mock_mac_address = '00:11:22:33:44:55'
    mock_hostname = 'sas1-8555.search.yandex.net'

    params_results = {
        ('eth0', '604', 'mac'): 'aaaa:bbb:ccc:dd:0:604:2233:4455/64',
        ('eth0', '604', 'mac', '2a02:6b8:c0e:32::/64'): '2a02:6b8:c0e:32:0:604:2233:4455/64',
        ('eth0', '604', 'hostname'): 'aaaa:bbb:ccc:dd:0:604:db00:e07d/64',
        ('eth0', '604', 'ffffffff'): 'aaaa:bbb:ccc:dd:0:604:ffff:ffff/64',
        ('eth0', '604', 'some_string', '2a02:6b8:c0e:32::/64'): '2a02:6b8:c0e:32:0:604:31ee:7626/64',
        ('eth0', '80000604', 'mac', '2a02:6b8:c0e:32::/64'): '2a02:6b8:c0e:32:8000:604:2233:4455/64',
    }
    with mock.patch('infra.netconfig.lib.master.get_my_hostname', return_value=mock_hostname):
        with mock.patch('infra.netconfig.lib.master.get_ra_prefix', return_value=mock_ra_prefix):
            with mock.patch('infra.netconfig.lib.master.get_mac_address', return_value=mock_mac_address):
                for params, result in params_results.items():
                    assert master.get_projectid_address(*params) == result


class TestSetSysctl(unittest.TestCase):
    def setUp(self):
        self.iface_ok = 'dummy0'
        self.iface_with_points = 'dummy.with.points0'

    def test_iface_ok(self):
        m = mock.mock_open()
        with mock.patch('__builtin__.open', m) as o:
            master.set_sysctl('net.ipv6.conf.' + self.iface_ok + '.accept_ra', 1)
            o.assert_called_once_with('/proc/sys/net/ipv6/conf/dummy0/accept_ra', 'wb')

        m().write.assert_called_once_with('1\n')

    def test_iface_with_dots(self):
        m = mock.mock_open()
        with mock.patch('__builtin__.open', m) as o:
            # Doing the same in lib/master
            master.set_sysctl('net.ipv6.conf.' + self.iface_with_points.replace('.', '/') + '.accept_ra', 1)
            o.assert_called_once_with('/proc/sys/net/ipv6/conf/dummy.with.points0/accept_ra', 'wb')
        m().write.assert_called_once_with('1\n')


class TestAddPBRRules(unittest.TestCase):
    def setUp(self):
        self.from_net = '2001:db8:515:6980::/57'
        self.table_main = 'main'
        self.table_main_int = 254
        self.table_vlan = '688'
        self.to_net = '2001:db8:515:69a2::/64'
        self.attrs_for_main = [
            ('FRA_TABLE', 254),
            ('FRA_PRIORITY', master.PBR_PRIORITY),
            ('FRA_DST', '2001:db8:515:69a2::'),
            ('FRA_SRC', '2001:db8:515:6980::'),
        ]
        self.attrs_for_vlan = [
            ('FRA_TABLE', 688),
            ('FRA_SRC', '2001:db8:515:6980::'),
            ('FRA_PRIORITY', master.PBR_PRIORITY+5),
        ]
        self.attrs_wrong_vlan_priority = [
            ('FRA_TABLE', 688),
            ('FRA_SRC', '2001:db8:515:6980::'),
            ('FRA_PRIORITY', master.PBR_PRIORITY+66),
        ]
        self.attrs_wrong_main_priority = [
            ('FRA_TABLE', 254),
            ('FRA_PRIORITY', master.PBR_PRIORITY-100),
            ('FRA_DST', '2001:db8:515:69a2::'),
            ('FRA_SRC', '2001:db8:515:6980::'),
        ]

    def form_pyroute_rule_answer(self, attrs, table):
        rule_msg = fibmsg()
        rule_msg_description = {
            'dst_len': 64,
            'family': 10,
            'src_len': 57,
            'table': int(table),
            'attrs': attrs,
        }
        return rule_msg.load(rule_msg_description)

    def test_main_exists(self):
        with mock.patch(
            'pyroute2.IPRoute.get_rules',
            return_value=(
                self.form_pyroute_rule_answer(self.attrs_for_main, self.table_main_int),
                self.form_pyroute_rule_answer(self.attrs_for_vlan, self.table_vlan),
            )
        ):
            with mock.patch('pyroute2.IPRoute.rule') as m:
                master.add_pbr_rule(self.from_net, self.table_main, self.to_net)
                assert m.call_count == 0

    def test_vlan_exists(self):
        with mock.patch(
            'pyroute2.IPRoute.get_rules',
            return_value=(
                self.form_pyroute_rule_answer(self.attrs_for_main, self.table_main_int),
                self.form_pyroute_rule_answer(self.attrs_for_vlan, self.table_vlan),
            )
        ):
            with mock.patch('pyroute2.IPRoute.rule') as m:
                master.add_pbr_rule(self.from_net, self.table_vlan)
                assert m.call_count == 0

    def test_need_to_add_vlan(self):
        with mock.patch(
            'pyroute2.IPRoute.get_rules',
            return_value=(
                self.form_pyroute_rule_answer(self.attrs_for_main, self.table_main_int),
            )
        ):
            with mock.patch('pyroute2.IPRoute.rule') as m:
                master.add_pbr_rule(self.from_net, self.table_vlan)
                assert m.call_count == 1
                call_args, kw_call_args = m.call_args
                assert 'add' in call_args
                assert kw_call_args['family'] == socket.AF_INET6
                assert kw_call_args['table'] == 688
                assert kw_call_args['priority'] == master.PBR_PRIORITY+5
                assert kw_call_args['src'] == self.from_net

    def test_need_to_add_main(self):
        with mock.patch(
            'pyroute2.IPRoute.get_rules',
            return_value=(
                self.form_pyroute_rule_answer(self.attrs_for_vlan, self.table_vlan),
            )
        ):
            with mock.patch('pyroute2.IPRoute.rule') as m:
                master.add_pbr_rule(self.from_net, self.table_main, self.to_net)
                assert m.call_count == 1
                call_args, kw_call_args = m.call_args
                assert 'add' in call_args
                assert kw_call_args['family'] == socket.AF_INET6
                assert kw_call_args['table'] == self.table_main_int
                assert kw_call_args['priority'] == master.PBR_PRIORITY
                assert kw_call_args['src'] == self.from_net
                assert kw_call_args['dst'] == self.to_net

    def test_wrong_vlan_priority(self):
        with mock.patch(
            'pyroute2.IPRoute.get_rules',
            return_value=(
                self.form_pyroute_rule_answer(self.attrs_for_main, self.table_main_int),
                self.form_pyroute_rule_answer(self.attrs_wrong_vlan_priority, self.table_vlan),
            )
        ):
            with mock.patch('pyroute2.IPRoute.rule') as m:
                master.add_pbr_rule(self.from_net, self.table_vlan)
                assert m.call_count == 1
                call_args, kw_call_args = m.call_args
                assert 'add' in call_args
                assert kw_call_args['family'] == socket.AF_INET6
                assert kw_call_args['table'] == 688
                assert kw_call_args['priority'] == master.PBR_PRIORITY+5
                assert kw_call_args['src'] == self.from_net

    def test_wrong_main_priority(self):
        with mock.patch(
            'pyroute2.IPRoute.get_rules',
            return_value=(
                self.form_pyroute_rule_answer(self.attrs_wrong_main_priority, self.table_main_int),
                self.form_pyroute_rule_answer(self.attrs_for_vlan, self.table_vlan),
            )
        ):
            with mock.patch('pyroute2.IPRoute.rule') as m:
                master.add_pbr_rule(self.from_net, self.table_main, self.to_net)
                assert m.call_count == 1
                call_args, kw_call_args = m.call_args
                assert 'add' in call_args
                assert kw_call_args['family'] == socket.AF_INET6
                assert kw_call_args['table'] == self.table_main_int
                assert kw_call_args['priority'] == master.PBR_PRIORITY
                assert kw_call_args['src'] == self.from_net
                assert kw_call_args['dst'] == self.to_net


def test_switch_symbols():
    input = 'net.conf.ipv6.dummy0.accept_ra'
    assert master.switch_symbols(input, '.', '/') == 'net/conf/ipv6/dummy0/accept_ra'

    # emultate netconfig behavior
    iface = 'dum.my0'
    input = 'net.conf.ipv6.' + iface.replace('.', '/') + '.accept_ra'
    assert master.switch_symbols(input, '.', '/') == 'net/conf/ipv6/dum.my0/accept_ra'
