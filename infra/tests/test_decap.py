import unittest
import mock
import socket

from infra.shawshank.lib import decap
from infra.shawshank.tests import test_lib


class TestGetDecapIface(unittest.TestCase):
    def setUp(self):
        self.remote_ip_ok = '2a02:6b8:0:3400::aaaa'
        self.remote_ip_not_ok = '2001:db8::100'

        # Setup OK tunnel to decap
        self.link_obj = test_lib.setup_proper_ip6tnl_to_remote(self.remote_ip_ok)

        # Setup tunnel not to decap
        self.link_obj_not_ok = test_lib.setup_proper_ip6tnl_to_remote(self.remote_ip_not_ok)

    def test_no_links(self):
        with mock.patch('infra.shawshank.lib.netlink.get_links', return_value=[]):
            with mock.patch('infra.shawshank.lib.netlink.get_remote_ip_and_proto') as m:
                assert decap.get_decap_iface() is None
                m.call_count == 0

    def test_no_remote_tuns(self):
        with mock.patch('infra.shawshank.lib.netlink.get_links', return_value=[self.link_obj_not_ok]):
            assert decap.get_decap_iface() is None

    def test_ok(self):
        with mock.patch('infra.shawshank.lib.netlink.get_links', return_value=[self.link_obj_not_ok, self.link_obj]):
            assert decap.get_decap_iface() == 7


class TestGetAddressesOfDecapIface(unittest.TestCase):
    def setUp(self):
        self.netlink_addr1 = test_lib.form_pyroute_addr_answer('192.168.1.100', family=socket.AF_INET)
        self.netlink_addr2 = test_lib.form_pyroute_addr_answer('192.168.1.200', family=socket.AF_INET)

    def test_ok_1(self):
        with mock.patch('pyroute2.IPRoute.get_addr', return_value=(self.netlink_addr1,)):
            result = decap.get_addresses_of_decap_iface(2)  # this index id formed in form_pyroute_addr_answer()
            assert result == {'192.168.1.100'}

    def test_ok_2(self):
        with mock.patch('pyroute2.IPRoute.get_addr', return_value=(self.netlink_addr2,)):
            result = decap.get_addresses_of_decap_iface(2)  # this index id formed in form_pyroute_addr_answer()
            assert result == {'192.168.1.200'}

    def test_ok_3(self):
        with mock.patch('pyroute2.IPRoute.get_addr', return_value=(self.netlink_addr1, self.netlink_addr2)):
            result = decap.get_addresses_of_decap_iface(2)  # this index id formed in form_pyroute_addr_answer()
            assert result == {'192.168.1.200', '192.168.1.100'}
