import mock
import ipaddr
import unittest
import logging

from pyroute2.netlink.rtnl.ifinfmsg import ifinfmsg

from infra.shawshank.lib import netlink
from infra.shawshank.tests import test_lib


class TestGetLocalBBIp(unittest.TestCase):
    def setUp(self):
        self.dummy_address = '2001:DB8::300'
        self.mtn_address = '2a02:6b8:c08:6506::1'
        self.netlink_msg = test_lib.form_pyroute_addr_answer(self.dummy_address)
        self.netlink_msg_mtn = test_lib.form_pyroute_addr_answer(self.mtn_address)

    def test_just_get_addr(self):
        with mock.patch('pyroute2.IPRoute.link_lookup'):
            with mock.patch('pyroute2.IPRoute.get_addr', return_value=(self.netlink_msg,)):
                bb = netlink.get_local_bb_ip('ethXXX', False)
                assert bb == ipaddr.IPAddress(self.dummy_address)

    def test_not_found_on_mtn_check(self):
        with mock.patch('pyroute2.IPRoute.link_lookup'):
            with mock.patch('pyroute2.IPRoute.get_addr', return_value=(self.netlink_msg,)):
                bb = netlink.get_local_bb_ip('ethXXX', True)
                assert bb is None

    def test_select_mtn_address(self):
        with mock.patch('pyroute2.IPRoute.link_lookup'):
            with mock.patch('pyroute2.IPRoute.get_addr', return_value=(self.netlink_msg, self.netlink_msg_mtn)):
                bb = netlink.get_local_bb_ip('ethXXX', True)
                assert bb == ipaddr.IPAddress(self.mtn_address)


class TestGetRemoteIPandProto(unittest.TestCase):
    def setUp(self):

        self.remote_ip = '2001:db8::200'
        # Setup OK tunnel
        self.link_obj = test_lib.setup_proper_ip6tnl_to_remote(self.remote_ip)

        # Setup link without IFLA_LINK_INFO
        self.no_info_link_obj_description = {
            'index': 13,
            'attrs': [
                ('IFLA_IFNAME', 'no_info_iface0'),
            ],
        }
        self.no_info_link_obj = ifinfmsg()
        self.no_info_link_obj.load(self.no_info_link_obj_description)

        # Setup not ip6tnl tunnel link
        self.not_ip6tnl_info_desc = {
            'attrs': [
                ('IFLA_INFO_KIND', 'ipip'),
            ]
        }
        self.not_ip6tnl_info = ifinfmsg()
        self.not_ip6tnl_info.load(self.not_ip6tnl_info_desc)

        self.not_ip6tnl_link_description = {
            'index': 666,
            'attrs': [
                ('IFLA_IFNAME', 'not_ip6tnl_iface0'),
                ('IFLA_LINKINFO', self.not_ip6tnl_info),
            ],
        }
        self.not_ip6tnl_link = ifinfmsg()
        self.not_ip6tnl_link.load(self.not_ip6tnl_link_description)

    def test_ok(self):
        r_ip, r_proto = netlink.get_remote_ip_and_proto(self.link_obj)
        assert r_ip == self.remote_ip
        assert r_proto == 41

    def test_no_info(self):
        r_ip, r_proto = netlink.get_remote_ip_and_proto(self.no_info_link_obj)
        assert r_ip is None
        assert r_proto is None

    def test_not_ip6tn(self):
        r_ip, r_proto = netlink.get_remote_ip_and_proto(self.not_ip6tnl_link)
        assert r_ip is None
        assert r_proto is None


class TestAddrExists(unittest.TestCase):
    logger = logging.getLogger(__name__)
    logger.setLevel(logging.INFO)

    def setUp(self):
        self.mocked_all_addrs = {
            '10.0.0.1',
            '127.0.0.1',
            '2001:db8:c07:82d:0:696:cad0:0',
            '2001:db8:fc0f:82d:0:696:7351:0',
            '::1',
            'fe80::5054:ff:fe12:3456',
        }

    def test_ok(self):
        with mock.patch('infra.shawshank.lib.netlink.get_all_addresses', return_value=self.mocked_all_addrs):
            for a in self.mocked_all_addrs:
                self.logger.info("Address: {}".format(a))
                assert netlink.addr_exists(a) is True

    def test_not_ok(self):
        with mock.patch('infra.shawshank.lib.netlink.get_all_addresses', return_value=self.mocked_all_addrs):
            assert netlink.addr_exists('2001:db8::1') is False
            assert netlink.addr_exists('192.168.0.1') is False
