import unittest
import mock
import socket

from infra.shawshank.lib import tun64
from infra.shawshank.tests import test_lib


class TestTryFindTUN64ViaMap(unittest.TestCase):
    def setUp(self):
        self.map64 = {
            "5.45.201.1": {
                "net_loc": "SAS",
                "scheme": "tun64",
                "fqdn": "sas2-3729-sas-market-prod-check-a93-8425.gencfg-c.yandex.net",
                "addr6": "2a02:6b8:c08:cd26:10d:c37f:0:20e9"
            },
            "5.45.201.2": {
                "net_loc": "SAS",
                "scheme": "tun64",
                "fqdn": "sas1-1943-sas-mssngr-router-balancers-31925.gencfg-c.yandex.net",
                "addr6": "2a02:6b8:c08:2e04:10d:6a66:0:7cb5"
            }
        }

        self.netlink_msg_ok = test_lib.form_pyroute_addr_answer('5.45.201.2', family=socket.AF_INET)
        self.netlink_msg_not_ok = test_lib.form_pyroute_addr_answer('5.45.201.100', family=socket.AF_INET)

    def test_empty_map(self):
        with mock.patch('infra.shawshank.lib.tun64.get_tun64_map', return_value=None) as n:
            assert tun64.try_find_tun64_via_map() is None
            assert n.call_count == 1

    def test_not_empty_map_but_empty_links(self):
        with mock.patch('infra.shawshank.lib.tun64.get_tun64_map', return_value=self.map64) as n:
            with mock.patch('pyroute2.IPRoute.get_addr', return_value=[]) as m:
                assert tun64.try_find_tun64_via_map() is None
                assert n.call_count == 1
                assert m.call_count == 1

    def test_not_ok_msg(self):
        with mock.patch('infra.shawshank.lib.tun64.get_tun64_map', return_value=self.map64) as n:
            with mock.patch('pyroute2.IPRoute.get_addr', return_value=(self.netlink_msg_not_ok,)) as m:
                assert tun64.try_find_tun64_via_map() is None
                assert n.call_count == 1
                assert m.call_count == 1

    def test_ok_msg(self):
        with mock.patch('infra.shawshank.lib.tun64.get_tun64_map', return_value=self.map64) as n:
            with mock.patch('pyroute2.IPRoute.get_addr', return_value=(self.netlink_msg_ok,)) as m:
                assert tun64.try_find_tun64_via_map() == 2
                assert n.call_count == 1
                assert m.call_count == 1

    def test_transponse_64(self):
        expect = {
            "2a02:6b8:c08:cd26:10d:c37f:0:20e9": "5.45.201.1",
            "2a02:6b8:c08:2e04:10d:6a66:0:7cb5": "5.45.201.2",
        }
        assert tun64.transponse_map64(self.map64) == expect


class TestFindTUN64Iface(unittest.TestCase):
    def setUp(self):
        self.link_ok = test_lib.form_pyroute_link_answer(2, 'ip_ext_tun0')

    def test_ok(self):
        with mock.patch('pyroute2.IPRoute.get_links', return_value=(self.link_ok,)) as l:
            with mock.patch('infra.shawshank.lib.tun64.try_find_tun64_via_map') as m:
                assert tun64.find_tun64_iface() == 2
                assert l.call_count == 1
                assert m.call_count == 0

    def test_find_via_map_failed(self):
        with mock.patch('pyroute2.IPRoute.get_links', return_value=[]) as l:
            with mock.patch('infra.shawshank.lib.tun64.try_find_tun64_via_map', return_value=None) as m:
                assert tun64.find_tun64_iface() is None
                assert m.call_count == 1
                assert l.call_count == 1

    def test_find_via_map_ok(self):
        with mock.patch('pyroute2.IPRoute.get_links', return_value=[]) as l:
            with mock.patch('infra.shawshank.lib.tun64.try_find_tun64_via_map', return_value=3) as m:
                assert tun64.find_tun64_iface() == 3
                assert m.call_count == 1
                assert l.call_count == 1


class TestCheckDefaultTUN64Route(unittest.TestCase):
    def setUp(self):
        self.proper_iface_id = 2
        self.netlink_msg_not_ok = test_lib.form_pyroute_route_answer('0.0.0.0/0', 3, family=socket.AF_INET)
        self.netlink_msg_ok = test_lib.form_pyroute_route_answer('0.0.0.0/0', 2, family=socket.AF_INET)

    def test_empty_default_routes(self):
        with mock.patch('pyroute2.IPRoute.get_default_routes', return_value=[]):
            with mock.patch('infra.shawshank.lib.netlink.set_default_route') as m:
                tun64.check_default_tun64_route(self.proper_iface_id)
                assert m.call_count == 1
                call_args, kw_call_args = m.call_args
                assert 2 in call_args
                assert 'add' in call_args

    def test_need_to_change_route(self):
        with mock.patch('pyroute2.IPRoute.get_default_routes', return_value=(self.netlink_msg_not_ok,)):
            with mock.patch('infra.shawshank.lib.netlink.set_default_route') as m:
                tun64.check_default_tun64_route(self.proper_iface_id)
                assert m.call_count == 1
                call_args, kw_call_args = m.call_args
                assert 2 in call_args
                assert 'replace' in call_args

    def test_no_need_to_change_anything(self):
        with mock.patch('pyroute2.IPRoute.get_default_routes', return_value=(self.netlink_msg_ok,)):
            with mock.patch('infra.shawshank.lib.netlink.set_default_route') as m:
                tun64.check_default_tun64_route(self.proper_iface_id)
                assert m.call_count == 0


class TestCreateTun64Iface(unittest.TestCase):
    def setUp(self):
        self.map64 = {
            "5.45.201.1": {
                "net_loc": "SAS",
                "scheme": "tun64",
                "fqdn": "sas2-3729-sas-market-prod-check-a93-8425.gencfg-c.yandex.net",
                "addr6": "2a02:6b8:c08:cd26:10d:c37f:0:20e9"
            },
            "5.45.201.2": {
                "net_loc": "SAS",
                "scheme": "tun64",
                "fqdn": "sas1-1943-sas-mssngr-router-balancers-31925.gencfg-c.yandex.net",
                "addr6": "2a02:6b8:c08:2e04:10d:6a66:0:7cb5"
            }
        }
        self.not_mtn_ip = '2001:db8::1'
        self.ok_ip = "2a02:6b8:c08:2e04:10d:6a66:0:7cb5"

    def test_found_iface(self):
        with mock.patch('infra.shawshank.lib.tun64.find_tun64_iface', return_value=2) as m:
            result = tun64.create_tun64_iface('xxx', False)
            assert m.call_count == 1
            assert result == 2

    def test_empty_map(self):
        with mock.patch('infra.shawshank.lib.tun64.get_tun64_map', return_value={}) as n:
            with self.assertRaises(Exception) as e:
                tun64.create_tun64_iface()
                assert str(e.exception) == 'tun64 map is empty'
                assert n.call_count == 2

    def test_not_mtn_ip(self):
        with mock.patch('infra.shawshank.lib.tun64.find_tun64_iface', return_value=None):
            with mock.patch('infra.shawshank.lib.tun64.get_tun64_map', return_value=self.map64):
                with mock.patch('infra.shawshank.lib.netlink.get_local_bb_ip', return_value=self.not_mtn_ip):
                    with self.assertRaises(Exception) as e:
                        tun64.create_tun64_iface()
                        assert str(e.exception) == 'Not found any v6 address to use as local address for tun64 tunnel'

    def test_ok(self):
        with mock.patch('infra.shawshank.lib.tun64.find_tun64_iface', return_value=None):
            with mock.patch('infra.shawshank.lib.tun64.get_tun64_map', return_value=self.map64):
                with mock.patch('infra.shawshank.lib.netlink.get_local_bb_ip', return_value=self.ok_ip):
                    with mock.patch('pyroute2.IPRoute.link_lookup'):
                        with mock.patch('pyroute2.IPRoute.link') as l:
                            with mock.patch('pyroute2.IPRoute.addr') as a:
                                tun64.create_tun64_iface('xxx', True)
                                assert a.call_count == 1
                                assert 'add' in a.call_args.args
                                assert l.call_count == 3
