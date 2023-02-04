import mock
import unittest
import netaddr
import socket

import porto

from google.protobuf.json_format import MessageToJson

from infra.shawshank.proto import tuns_pb2
from infra.awacs.proto import model_pb2 as awacs_model_pb2
from infra.shawshank.lib import master
from infra.shawshank.lib import util

from infra.shawshank.tests import test_lib

CONFIG_NAME = 'test.msg'


class TestCheckVipAddrsExistance(unittest.TestCase):
    def setUp(self):
        self.spec = gen_example_proto('xxx', write=False)
        self.address_ok = '2001:db8::100'
        self.address_ok2 = '2001:db8::200'
        self.address_ok3 = '192.0.2.1'
        self.address_ok4 = '2001:db8::400'

        self.netaddr_ok = netaddr.IPAddress(self.address_ok).value
        self.netaddr_ok2 = netaddr.IPAddress(self.address_ok2).value
        self.netaddr_ok3 = netaddr.IPAddress(self.address_ok3).value
        self.netaddr_ok4 = netaddr.IPAddress(self.address_ok4).value

        self.address_not_ok = '2001:DB8::300'

        self.netlink_msg_ok = test_lib.form_pyroute_addr_answer(self.address_ok)
        self.netlink_msg_ok2 = test_lib.form_pyroute_addr_answer(self.address_ok2)
        self.netlink_msg_ok3 = test_lib.form_pyroute_addr_answer(self.address_ok3, family=socket.AF_INET)
        self.netlink_msg_ok4 = test_lib.form_pyroute_addr_answer(self.address_ok4)

        self.netlink_msg_not_ok = test_lib.form_pyroute_addr_answer(self.address_not_ok)

    def test_ok(self):
        with mock.patch(
            'pyroute2.IPRoute.get_addr',
            return_value=(self.netlink_msg_ok, self.netlink_msg_ok2, self.netlink_msg_ok3, self.netlink_msg_ok4),
        ):
            assert master.check_vip_addrs_existence(self.spec) == set()

        with mock.patch(
            'pyroute2.IPRoute.get_addr',
            return_value=(
                self.netlink_msg_ok,
                self.netlink_msg_ok2,
                self.netlink_msg_ok3,
                self.netlink_msg_ok4,
                self.netlink_msg_not_ok,
                )):
            assert master.check_vip_addrs_existence(self.spec) == set()

    def test_not_ok(self):
        with mock.patch('pyroute2.IPRoute.get_addr', return_value=(self.netlink_msg_not_ok,)):
            assert master.check_vip_addrs_existence(self.spec) == {
                self.netaddr_ok, self.netaddr_ok2, self.netaddr_ok3, self.netaddr_ok4
            }

        with mock.patch('pyroute2.IPRoute.get_addr', return_value=(self.netlink_msg_ok, self.netlink_msg_not_ok,)):
            assert master.check_vip_addrs_existence(self.spec) == {
                self.netaddr_ok2, self.netaddr_ok3, self.netaddr_ok4
            }

        with mock.patch('pyroute2.IPRoute.get_addr', return_value=(
            self.netlink_msg_ok2, self.netlink_msg_ok4, self.netlink_msg_not_ok,)
        ):
            assert master.check_vip_addrs_existence(self.spec) == {
                self.netaddr_ok, self.netaddr_ok3
            }


class TestValidateV4VipsAndFromIps(unittest.TestCase):
    def setUp(self):
        self.msg = tuns_pb2.ContainerSpec()
        self.tun = tuns_pb2.Tunnel()
        self.tun.name = 'test-tun'
        self.tun.mtu = 1234
        self.tun.remote_ip = '2001:db8::123'
        self.tun.mode = 1
        self.tun.rules.add(from_ip='192.0.2.128')
        self.msg.tunnels.extend([self.tun])

        self.vip1 = tuns_pb2.VirtualIp()
        self.vip1.ip = '192.0.2.128'
        self.msg.virtual_ips.extend([self.vip1])

    def test_ok(self):
        assert master.validate_v4_vips_and_from_ips(self.msg) is True

    def test_not_ok(self):
        self.msg.virtual_ips.add(ip='192.0.2.200')
        assert master.validate_v4_vips_and_from_ips(self.msg) is False

        # cleanups
        self.msg.virtual_ips.pop()  # delete '192.0.2.200' ip
        assert len(self.msg.virtual_ips) == 1
        assert self.msg.virtual_ips[0].ip == '192.0.2.128'
        self.msg.tunnels.pop()
        assert len(self.msg.tunnels) == 0

        self.tun.rules.add(from_ip='192.0.2.200')
        self.msg.tunnels.extend([self.tun])
        assert len(self.msg.tunnels) == 1
        assert master.validate_v4_vips_and_from_ips(self.msg) is False


class TestValidatePortoIpsAndVip(unittest.TestCase):
    def setUp(self):
        self.msg = tuns_pb2.ContainerSpec()
        self.vip = tuns_pb2.VirtualIp()
        self.vip.ip = '192.0.2.128'
        self.msg.virtual_ips.extend([self.vip])

    def test_empty_porto_ips(self):
        with mock.patch('infra.shawshank.lib.master.get_porto_tun_ips', return_value=set()):
            assert master.validate_porto_ips_and_vips(self.msg) is True

    def test_ok(self):
        val = netaddr.IPAddress('192.0.2.128').value
        with mock.patch('infra.shawshank.lib.master.get_porto_tun_ips', return_value={val, }):
            assert master.validate_porto_ips_and_vips(self.msg) is True

    def test_not_ok(self):
        netaddr.IPAddress('192.0.2.128').value
        with mock.patch(
            'infra.shawshank.lib.master.get_porto_tun_ips',
            return_value={
                netaddr.IPAddress('192.0.2.128').value,
                netaddr.IPAddress('2001:db8::111').value,
        }):
            assert master.validate_porto_ips_and_vips(self.msg) is False


class TestGetPortoTunIps(unittest.TestCase):
    def setUp(self):
        pass

    def test_catch_exception_permission(self):
        with mock.patch('porto.Connection.GetProperty', side_effect=porto.exceptions.Permission):
            assert master.get_porto_tun_ips() == set()

    def test_ok(self):
        expected = {
            netaddr.IPAddress('192.0.2.42').value,
            netaddr.IPAddress('2001:db8::42').value,
        }
        with mock.patch('porto.Connection.GetProperty', side_effect=[
            'L3 veth;ipip6 tun0 2001:db8::aaaa',
            'veth 192.0.2.1;tun0 192.0.2.42;ip6tnl0 2001:db8::42',
        ]):
            assert master.get_porto_tun_ips() == expected

        expected.add(netaddr.IPAddress('2001:db8::1').value)
        with mock.patch('porto.Connection.GetProperty', side_effect=[
            'L3 veth;ipip6 tun0 2001:db8::aaaa',
            'veth 192.0.2.1;tun0 192.0.2.42;ip6tnl0 2001:db8::42;veth 192.0.2.100;veth 192.0.2.321;tun0 2001:db8::1',
        ]):
            assert master.get_porto_tun_ips() == expected


class TestExtendWithRulesAndRoutes(unittest.TestCase):
    def setUp(self):
        self.msg = tuns_pb2.ContainerSpec()
        self.tun = tuns_pb2.Tunnel()
        self.tun.name = 'tun0'
        self.tun.mtu = 1234
        self.tun.remote_ip = '2001:db8::123'
        self.tun.mode = 1

        self.msg.tunnels.extend([self.tun])

    def test_ok(self):
        mock_table_id = 400
        mock_prio = 1234
        mock_src = '2001:db8::123'
        with mock.patch('pyroute2.IPRoute.link_lookup'):
            with mock.patch(
                'pyroute2.IPRoute.get_routes',
                return_value=[test_lib.form_pyroute_route_answer('2001:db8::400', 1, table=mock_table_id)]
            ):
                with mock.patch(
                    'pyroute2.IPRoute.get_rules',
                    return_value=[test_lib.form_pyroute_rule_answer(mock_src, priority=mock_prio)]
                ):
                    msg = master.extend_with_rules_and_routes(self.msg)

        expected = tuns_pb2.ContainerSpec()
        tun = self.tun
        rule = tuns_pb2.Rule()
        rule.table_id = mock_table_id
        rule.from_ip = mock_src
        rule.priority = mock_prio
        tun.rules.extend([rule])
        expected.tunnels.extend([tun])

        for et in expected.tunnels:
            for mt in msg.tunnels:
                assert mt.name == et.name
                assert mt.mtu == et.mtu
                assert mt.remote_ip == et.remote_ip
                assert mt.mode == et.mode
                for etr in et.rules:
                    for mtr in mt.rules:
                        assert mtr.table_id == etr.table_id
                        assert mtr.from_ip == etr.from_ip
                        assert mtr.priority == etr.priority

    def test_no_table_id(self):
        mock_table_id = 253
        with mock.patch('pyroute2.IPRoute.link_lookup'):
            with mock.patch(
                'pyroute2.IPRoute.get_routes',
                return_value=[test_lib.form_pyroute_route_answer('2001:db8::400', 1, table=mock_table_id)]
            ):
                msg = master.extend_with_rules_and_routes(self.msg)

        expected = self.msg

        for et in expected.tunnels:
            for mt in msg.tunnels:
                assert mt.name == et.name
                assert mt.mtu == et.mtu
                assert mt.remote_ip == et.remote_ip
                assert mt.mode == et.mode
                for etr in et.rules:
                    for mtr in mt.rules:
                        assert mtr.table_id == etr.table_id
                        assert mtr.from_ip == etr.from_ip
                        assert mtr.priority == etr.priority


class TestRulesChanged(unittest.TestCase):
    def setUp(self):
        self.cur_rule = tuns_pb2.Rule()
        self.cur_rule.from_ip = '1.1.1.1'
        self.cur_rule.table_id = 200
        self.cur_rule.priority = 256

    def test_found_match(self):
        t_rule = tuns_pb2.Rule()
        t_rule.from_ip = '1.1.1.1'
        t_rule.table_id = 200
        t_rule.priority = 256

        assert master.rules_changed([self.cur_rule], [t_rule]) is False

    def test_not_found_match(self):
        t_rule2 = tuns_pb2.Rule()
        t_rule2.from_ip = '2.2.2.2'
        t_rule2.table_id = 201
        t_rule2.priority = 257
        assert master.rules_changed([self.cur_rule], [self.cur_rule, t_rule2]) is True


class TestCompareTunsMatchedByName(unittest.TestCase):
    def setUp(self):
        self.spec = gen_example_proto('xxx', write=False)

    def test_match(self):
        msg = tuns_pb2.ContainerSpec()
        tun = tuns_pb2.Tunnel()
        tun.name = 'test_tun'
        tun.mtu = 1450
        tun.advmss = 1390
        tun.remote_ip = '2001:DB8::515'
        tun.mode = tun.IP6IP6
        tun.rules.add(from_ip='2001:DB8::100', table_id=200, priority=256)
        tun.rules.add(from_ip='2001:DB8::200', table_id=200, priority=257)
        tun.rules.add(from_ip='2001:DB8::400', table_id=200, priority=258)
        msg.tunnels.extend([tun])

        assert len(msg.tunnels) == 1
        assert len(self.spec.tunnels) == 1

        assert msg.tunnels[0].name == self.spec.tunnels[0].name
        assert msg.tunnels[0].mtu == self.spec.tunnels[0].mtu
        assert msg.tunnels[0].advmss == self.spec.tunnels[0].advmss
        assert msg.tunnels[0].mode == self.spec.tunnels[0].mode
        assert netaddr.IPAddress(msg.tunnels[0].remote_ip).value == netaddr.IPAddress(self.spec.tunnels[0].remote_ip).value
        assert master.rules_changed(msg.tunnels[0].rules, self.spec.tunnels[0].rules) is False

        new, old = master.compare_tuns_matched_by_name(self.spec, msg)
        assert len(new.tunnels) == 0
        assert len(old.tunnels) == 0

    def test_missmatch(self):
        msg = tuns_pb2.ContainerSpec()
        tun = tuns_pb2.Tunnel()
        tun.name = 'test_tun'
        tun.mtu = 1450
        tun.advmss = 1390
        tun.remote_ip = '2001:DB8::515'
        tun.mode = tun.IP6IP6
        tun.rules.add(from_ip='2001:DB8::200', table_id=200, priority=257)
        tun.rules.add(from_ip='2001:DB8::400', table_id=200, priority=258)
        msg.tunnels.extend([tun])

        new, old = master.compare_tuns_matched_by_name(self.spec, msg)
        assert len(new.tunnels) != 0
        assert len(old.tunnels) != 0


def config_path(tmpdir):
    return str(tmpdir.join(CONFIG_NAME))


def gen_example_proto(tmpdir, write=True):
    awacs_spec_pb = awacs_model_pb2.BalancerContainerSpec()
    awacs_tunnel_pb = awacs_spec_pb.outbound_tunnels.add()

    awacs_tunnel_pb.id = "test_tun"
    awacs_tunnel_pb.mode = awacs_model_pb2.BalancerContainerSpec.OutboundTunnel.IP6IP6
    awacs_tunnel_pb.remote_ip = "2001:DB8::515"
    awacs_tunnel_pb.mtu = 1450
    awacs_tunnel_pb.advmss = 1390
    awacs_tunnel_pb.rules.add(from_ip='2001:DB8::100')
    awacs_tunnel_pb.rules.add(from_ip='2001:DB8::200')
    awacs_tunnel_pb.rules.add(from_ip='2001:DB8::400')

    vips = ('2001:DB8::100', '2001:DB8::200', '192.0.2.1')
    for v in vips:
        awacs_vip_pb = awacs_spec_pb.virtual_ips.add()
        awacs_vip_pb.ip = v
        awacs_vip_pb.description = "XXX.ya.ru"

    msg_json = MessageToJson(awacs_spec_pb, including_default_value_fields=True)

    if write:
        with open(config_path(tmpdir), 'w') as f:
            f.write(msg_json)

    msg = util.awacs_instance_spec_pb_to_internal_pb(awacs_spec_pb)
    return msg


def test_read_spec(tmpdir):
    generated_msg = gen_example_proto(tmpdir)
    readed_msg = master.read_spec(config_path(tmpdir))
    assert readed_msg == generated_msg
    generated_tuns = generated_msg.tunnels
    readed_msg_tunnels = readed_msg.tunnels
    assert len(readed_msg_tunnels) == len(generated_tuns) == 1
    assert readed_msg_tunnels[0].name == generated_tuns[0].name
    assert readed_msg_tunnels[0].mode == generated_tuns[0].mode
    assert readed_msg_tunnels[0].remote_ip == generated_tuns[0].remote_ip
    assert readed_msg_tunnels[0].mtu == generated_tuns[0].mtu
