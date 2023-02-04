import unittest
import mock

from infra.shawshank.lib import vip
from infra.shawshank.proto import tuns_pb2
from pyroute2.netlink.exceptions import NetlinkError


class TestAddVips(unittest.TestCase):
    def setUp(self):
        self.container_spec = tuns_pb2.ContainerSpec()
        self.container_spec = tuns_pb2.ContainerSpec()
        self.container_spec.virtual_ips.add(ip='2001:db8::100', desc='test-XXXX.service')
        self.container_spec.virtual_ips.add(ip='2001:db8::200', desc='test-XXXX.service')
        self.container_spec.virtual_ips.add(ip='192.0.2.1', desc='test-XXXX.service')

    def test_ok(self):
        with mock.patch('pyroute2.IPRoute.flush_addr') as f:
            with mock.patch('pyroute2.IPRoute.link_lookup', side_effect=[[10], [11]]) as ll:
                with mock.patch('pyroute2.IPRoute.addr') as a:
                    vip.add_vips(self.container_spec)
                    assert f.call_count == 2
                    assert ll.call_count == 2
                    assert a.call_count == 3
                    assert a.mock_calls[0].kwargs == {'address': '2001:db8::100', 'index': 11, 'mask': 128}
                    assert a.mock_calls[1].kwargs == {'address': '2001:db8::200', 'index': 11, 'mask': 128}
                    assert a.mock_calls[2].kwargs == {'address': '192.0.2.1', 'index': 10, 'mask': 32}

    def test_no_device(self):
        with mock.patch('pyroute2.IPRoute.flush_addr') as f:
            with mock.patch('pyroute2.IPRoute.link_lookup', side_effect=[[10], []]) as ll:
                with mock.patch('pyroute2.IPRoute.addr') as a:
                    with self.assertRaises(Exception) as e:
                        vip.add_vips(self.container_spec)
                        assert f.call_count == 0
                        assert ll.call_count == 2
                        assert a.call_count == 0
                        assert str(e.exception) == 'No such device: ip6tnl0'

    def test_same_ips(self):
        with mock.patch('pyroute2.IPRoute.flush_addr') as f:
            with mock.patch('pyroute2.IPRoute.link_lookup', side_effect=[[10], [11]]) as ll:
                with mock.patch('pyroute2.IPRoute.addr', side_effect=NetlinkError(17)) as a:
                    vip.add_vips(self.container_spec)
                    assert f.call_count == 2
                    assert ll.call_count == 2
                    assert a.call_count == 3

    def test_raise_on_add(self):
        with mock.patch('pyroute2.IPRoute.flush_addr') as f:
            with mock.patch('pyroute2.IPRoute.link_lookup', side_effect=[[10], [11]]) as ll:
                with mock.patch('pyroute2.IPRoute.addr', side_effect=NetlinkError(9000)) as a:
                    with self.assertRaises(NetlinkError) as e:
                        vip.add_vips(self.container_spec)
                        assert f.call_count == 2
                        assert ll.call_count == 2
                        assert a.call_count == 1
                        assert e.code == 9000
