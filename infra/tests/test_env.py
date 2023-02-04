import os
import json
import subprocess
import socket
import unittest
import netaddr

from pyroute2 import IPRoute

from infra.shawshank.lib import netlink

import yatest.common as ytc


def run(args, **kwargs):
    print("+ '" + "' '".join(args) + "'")
    return subprocess.check_output(args, **kwargs).decode('utf-8')


def check_tun_attrs(out_tuns):
    for t in out_tuns:
        with IPRoute() as ipr:
            idx = ipr.link_lookup(ifname=t['id'])[0]
            link = ipr.get_links(idx)[0]
            print(link)
            expected_mtu = t.get('mtu') or 1450
            assert link.get_attr('IFLA_MTU') == expected_mtu
            link_info = link.get_attr('IFLA_LINKINFO')
            assert link_info.get_attr('IFLA_INFO_KIND') == 'ip6tnl'
            info_data = link_info.get_attr('IFLA_INFO_DATA')
            remote_ip_val = netaddr.IPAddress(info_data.get_attr('IFLA_IP6TNL_REMOTE')).value
            expected_remote_ip_val = netaddr.IPAddress(t['remoteIp']).value
            assert remote_ip_val == expected_remote_ip_val
            assert info_data.get_attr('IFLA_IP6TNL_TTL') == 64
            assert info_data.get_attr('IFLA_IP6TNL_PROTO') == 4  # ipip6
            expected_encap = t.get('encap')
            if expected_encap and expected_encap['type'] != 0:
                assert info_data.get_attr('IFLA_IP6TNL_ENCAP_TYPE') == 1  # mpls
                expected_encap_sport = expected_encap.get('sport')
                if expected_encap_sport:
                    assert info_data.get_attr('IFLA_IP6TNL_ENCAP_SPORT') == expected_encap_sport
                expected_encap_dport = expected_encap.get('dport')
                if expected_encap_dport:
                    assert info_data.get_attr('IFLA_IP6TNL_ENCAP_DPORT') == expected_encap_dport
                expected_encap_limit = expected_encap.get('limit')
                if expected_encap_limit:
                    assert info_data.get_attr('IFLA_IP6TNL_ENCAP_LIMIT') == expected_encap_limit
                else:
                    assert info_data.get_attr('IFLA_IP6TNL_FLAGS') & netlink.IP6_TNL_F_IGN_ENCAP_LIMIT == netlink.IP6_TNL_F_IGN_ENCAP_LIMIT
            else:
                assert info_data.get_attr('IFLA_IP6TNL_ENCAP_LIMIT') == netlink.V6_DEF_ENCAPLIMIT


def check_route(ipr, route, ifname, mtu=None, advmss=None, mpls_labels=None):
    out_dev_index = route.get_attr('RTA_OIF')
    link = ipr.get_links(out_dev_index)[0]
    assert link.get_attr('IFLA_IFNAME') == ifname
    if mtu or advmss:
        metrics = route.get_attr('RTA_METRICS')
        if mtu:
            route_mtu = metrics.get_attr('RTAX_MTU')
            assert route_mtu == mtu
        if advmss:
            route_advmss = metrics.get_attr('RTAX_ADVMSS')
            assert route_advmss == advmss
    if mpls_labels:
        assert route.get_attr('RTA_ENCAP_TYPE') == netlink.LWTUNNEL_ENCAP_MPLS
        route_labels = netlink.get_mpls_labels_from_encap(route.get_attr('RTA_ENCAP'))
        assert route_labels == mpls_labels


def check_default_route(family, route_config):
    if not route_config:
        return

    iface = route_config['iface']
    mtu = route_config['mtu'] if 'mtu' in route_config else None
    advmss = route_config['advmss'] if 'advmss' in route_config else None
    mpls_labels = route_config['mplsLabels'] if 'mplsLabels' in route_config else None

    with IPRoute() as ipr:
        route = ipr.get_default_routes(family=family)[0]
        check_route(ipr, route, iface, mtu, advmss, mpls_labels)


def check_default_routes(config):
    check_default_route(socket.AF_INET, config.get('defaultV4Route'))
    check_default_route(socket.AF_INET6, config.get('defaultV6Route'))


class TestEnvironment(unittest.TestCase):
    def setUp(self):
        print(run(['modprobe', 'fou6']))

        self.shawshank_bin = ytc.binary_path('infra/shawshank/bin/shawshank')
        self.conf_dir = ytc.source_path('infra/shawshank/tests/env_data')
        self.config_1_file = os.path.join(self.conf_dir, 'config.1.json')
        self.config_2_file = os.path.join(self.conf_dir, 'config.2.json')
        self.config_3_file = os.path.join(self.conf_dir, 'config.3.json')
        self.config_4_file = os.path.join(self.conf_dir, 'config.4.json')

        with open(self.config_1_file) as f:
            self.config_1 = json.load(f)
        with open(self.config_2_file) as f:
            self.config_2 = json.load(f)
        with open(self.config_3_file) as f:
            self.config_3 = json.load(f)
        with open(self.config_4_file) as f:
            self.config_4 = json.load(f)

        print(run([self.shawshank_bin, '-i', 'eth0', '--noporto', '-m', '-c', self.config_1_file]))

    def test_addresses_are_present(self):
        expexted_addresses = {netaddr.IPAddress(i['ip']).value for i in self.config_1['virtualIps']}
        with IPRoute() as ipr:
            addrs = ipr.get_addr()

        current_addresses = set()
        for a in addrs:
            current_addresses.add(netaddr.IPAddress(a.get_attr('IFA_ADDRESS')).value)
        assert expexted_addresses - current_addresses == set()

    def test_link_attrs(self):
        with IPRoute() as ipr:
            ipr.link_lookup(ifname='ip6tnl0')[0]  # will raise IndexError if no ip6tnl0 device found

        out_tuns = self.config_1['outboundTunnels']
        check_tun_attrs(out_tuns)

    def test_rules_and_routes(self):
        with IPRoute() as ipr:
            rules = ipr.get_rules(family=socket.AF_INET)

        current_addr_table_map = {}  # src_addr:route_table_number
        for r in rules:
            try:
                current_addr_table_map[netaddr.IPAddress(r.get_attr('FRA_SRC')).value] = r.get_attr('FRA_TABLE')
            except netaddr.AddrFormatError:
                continue

        expected_rules_srcs = {}  # rule_src_addr:id
        out_tuns = self.config_1['outboundTunnels']
        for ot in out_tuns:
            for r in ot['rules']:
                src_addr_val = netaddr.IPAddress(r['fromIp']).value
                expected_rules_srcs[src_addr_val] = ot['id']
                assert src_addr_val in current_addr_table_map.keys()

        with IPRoute() as ipr:
            for expected_src_addr, expected_dev_name in expected_rules_srcs.items():
                table_number = current_addr_table_map[expected_src_addr]
                routes = ipr.get_routes(family=socket.AF_INET, table=table_number)
                for r in routes:
                    check_route(ipr, r, expected_dev_name)

        check_default_routes(self.config_1)

    def test_full_change(self):
        print(run([self.shawshank_bin, '-i', 'eth0', '--noporto', '-m', '-c', self.config_2_file]))

        with IPRoute() as ipr:
            links = ipr.get_links()
        cur_link_names = {link.get_attr('IFLA_IFNAME') for link in links}
        for ot in self.config_1['outboundTunnels']:
            assert ot['id'] not in cur_link_names

        out_tuns = self.config_2['outboundTunnels']
        check_tun_attrs(out_tuns)

        expexted_addresses = {netaddr.IPAddress(i['ip']).value for i in self.config_2['virtualIps']}
        with IPRoute() as ipr:
            addrs = ipr.get_addr()

        current_addresses = set()
        for a in addrs:
            current_addresses.add(netaddr.IPAddress(a.get_attr('IFA_ADDRESS')).value)
        assert expexted_addresses - current_addresses == set()

        conf_1_addresses = {netaddr.IPAddress(i['ip']).value for i in self.config_1['virtualIps']}
        assert conf_1_addresses - current_addresses == conf_1_addresses

        with IPRoute() as ipr:
            rules = ipr.get_rules(family=socket.AF_INET)
        current_from_addrs = set()
        for r in rules:
            try:
                current_from_addrs.add(netaddr.IPAddress(r.get_attr('FRA_SRC')).value)
            except netaddr.AddrFormatError:
                continue
        assert conf_1_addresses - current_from_addrs == conf_1_addresses

    def test_add_vip(self):
        print(run([self.shawshank_bin, '-i', 'eth0', '--noporto', '-m', '-c', self.config_3_file]))

        expexted_addresses = {netaddr.IPAddress(i['ip']).value for i in self.config_3['virtualIps']}
        with IPRoute() as ipr:
            addrs = ipr.get_addr()

        current_addresses = set()
        for a in addrs:
            current_addresses.add(netaddr.IPAddress(a.get_attr('IFA_ADDRESS')).value)
        assert expexted_addresses - current_addresses == set()
        assert netaddr.IPAddress('2a02:6b8:0::300').value in current_addresses

    def test_del_vip(self):
        print(run([self.shawshank_bin, '-i', 'eth0', '--noporto', '-m', '-c', self.config_2_file]))

        expected_to_be_deleted = netaddr.IPAddress('2a02:6b8:0::300').value

        with IPRoute() as ipr:
            addrs = ipr.get_addr()
        current_addresses = set()
        for a in addrs:
            current_addresses.add(netaddr.IPAddress(a.get_attr('IFA_ADDRESS')).value)
        assert expected_to_be_deleted not in current_addresses

        expected = netaddr.IPAddress('2a02:6b8:0::100').value
        assert expected in current_addresses
        expected = netaddr.IPAddress('2a02:6b8:0::200').value
        assert expected in current_addresses

    def test_add_rule(self):
        # config.4 based on config.2, so set config.2 first
        print(run([self.shawshank_bin, '-i', 'eth0', '--noporto', '-m', '-c', self.config_2_file]))
        check_default_routes(self.config_2)

        print(run([self.shawshank_bin, '-i', 'eth0', '--noporto', '-m', '-c', self.config_4_file]))
        check_default_routes(self.config_4)

        expected_to_be_added = netaddr.IPAddress('7.7.7.7').value

        # check, that new addr added
        with IPRoute() as ipr:
            addrs = ipr.get_addr()
        current_addresses = set()
        for a in addrs:
            current_addresses.add(netaddr.IPAddress(a.get_attr('IFA_ADDRESS')).value)
        assert expected_to_be_added in current_addresses

        with IPRoute() as ipr:
            rules = ipr.get_rules()
        rules_srcs = set()
        for ru in rules:
            ru = ru.get_attr('FRA_SRC')
            if ru:
                rules_srcs.add(netaddr.IPAddress(ru).value)
        assert expected_to_be_added in rules_srcs
