#! /usr/bin/env python2

import mock
import unittest

from ipaddr import IPAddress

import hbfagent.iptables as ipt
from hbfagent.mod import runtime_mtn_nat
from hbfagent.mod.runtime_mtn_nat import PortRange
from hbfagent.util import ISSIPv6Address
from hbfagent.util import join_lines


class TestRuntimeMTNNat(unittest.TestCase):

    local_range = (32768, 60999)
    default_local_range = (32768, 65535)

    local_reserved_ports_str = "1,2-4,10-10"
    local_reserved_ports = set([1, 2, 3, 4, 10])

    mtn_nat_rules = join_lines(
        "*raw",
        ":OUTPUT -",
        ":PREROUTING_MTN_NAT -",
        ":PREROUTING -",
        ":OUTPUT_MTN_NAT -",
        "-A OUTPUT -j OUTPUT_MTN_NAT",
        "-I PREROUTING_MTN_NAT -p tcp -d 2a02:6b8:fc00:6a08:0:604:0:1 --dport 18534:18541 -j ACCEPT",
        "-I PREROUTING_MTN_NAT -p tcp -d 2a02:6b8:c08:6a08:0:604:0:1 --dport 18534:18541 -j ACCEPT",
        "-I PREROUTING_MTN_NAT -p tcp -s 2a02:6b8:c08:6a08:10b:6792:9:4866 --sport 18534:18541 -j ACCEPT",
        "-I PREROUTING_MTN_NAT -p tcp -i lo --sport 18534:18541 -j ACCEPT",
        "-I PREROUTING_MTN_NAT -p tcp -d 2a02:6b8:fc00:6a08:0:604:0:1 --dport 29217:29224 -j ACCEPT",
        "-I PREROUTING_MTN_NAT -p tcp -d 2a02:6b8:c08:6a08:0:604:0:1 --dport 29217:29224 -j ACCEPT",
        "-I PREROUTING_MTN_NAT -p tcp -s 2a02:6b8:c08:6a08:10b:6791:15:7221 --sport 29217:29224 -j ACCEPT",
        "-I PREROUTING_MTN_NAT -p tcp -i lo --sport 29217:29224 -j ACCEPT",
        "-A PREROUTING -j PREROUTING_MTN_NAT",
        "-I OUTPUT_MTN_NAT -p tcp -o lo --dport 18534:18541 -j ACCEPT",
        "-I OUTPUT_MTN_NAT -p tcp -o L3+ --dport 18534:18541 -j ACCEPT",
        "-I OUTPUT_MTN_NAT -p tcp -o lo --dport 29217:29224 -j ACCEPT",
        "-I OUTPUT_MTN_NAT -p tcp -o L3+ --dport 29217:29224 -j ACCEPT",
        "COMMIT",
        "*nat",
        ":OUTPUT -",
        ":PREROUTING_MTN_NAT -",
        ":PREROUTING -",
        ":OUTPUT_MTN_NAT -",
        "-A OUTPUT -j OUTPUT_MTN_NAT",
        "-A PREROUTING_MTN_NAT -p tcp -d 2a02:6b8:fc00:6a08:0:604:0:1 --dport 18534:18541 -j DNAT --to-destination 2a02:6b8:c08:6a08:10b:6792:9:4866",
        "-A PREROUTING_MTN_NAT -p udp -d 2a02:6b8:fc00:6a08:0:604:0:1 --dport 18534:18541 -j DNAT --to-destination 2a02:6b8:c08:6a08:10b:6792:9:4866",
        "-A PREROUTING_MTN_NAT -p tcp -d 2a02:6b8:c08:6a08:0:604:0:1 --dport 18534:18541 -j DNAT --to-destination 2a02:6b8:c08:6a08:10b:6792:9:4866",
        "-A PREROUTING_MTN_NAT -p udp -d 2a02:6b8:c08:6a08:0:604:0:1 --dport 18534:18541 -j DNAT --to-destination 2a02:6b8:c08:6a08:10b:6792:9:4866",
        "-A PREROUTING_MTN_NAT -p tcp -d 2a02:6b8:fc00:6a08:0:604:0:1 --dport 29217:29224 -j DNAT --to-destination 2a02:6b8:c08:6a08:10b:6791:15:7221",
        "-A PREROUTING_MTN_NAT -p udp -d 2a02:6b8:fc00:6a08:0:604:0:1 --dport 29217:29224 -j DNAT --to-destination 2a02:6b8:c08:6a08:10b:6791:15:7221",
        "-A PREROUTING_MTN_NAT -p tcp -d 2a02:6b8:c08:6a08:0:604:0:1 --dport 29217:29224 -j DNAT --to-destination 2a02:6b8:c08:6a08:10b:6791:15:7221",
        "-A PREROUTING_MTN_NAT -p udp -d 2a02:6b8:c08:6a08:0:604:0:1 --dport 29217:29224 -j DNAT --to-destination 2a02:6b8:c08:6a08:10b:6791:15:7221",
        "-A PREROUTING -j PREROUTING_MTN_NAT",
        "-A OUTPUT_MTN_NAT -p tcp -o lo --dport 18534:18541 -j DNAT --to-destination 2a02:6b8:c08:6a08:10b:6792:9:4866",
        "-A OUTPUT_MTN_NAT -p udp -o lo --dport 18534:18541 -j DNAT --to-destination 2a02:6b8:c08:6a08:10b:6792:9:4866",
        "-A OUTPUT_MTN_NAT -p tcp -o lo --dport 29217:29224 -j DNAT --to-destination 2a02:6b8:c08:6a08:10b:6791:15:7221",
        "-A OUTPUT_MTN_NAT -p udp -o lo --dport 29217:29224 -j DNAT --to-destination 2a02:6b8:c08:6a08:10b:6791:15:7221",
        "COMMIT"
    )

    mtn_nat_tables = ipt.IPTables("v6", mtn_nat_rules)

    host_ips = [
        IPAddress("2a02:6b8:fc00:6a08:0:604:0:1"),
        IPAddress("2a02:6b8:c08:6a08:0:604:0:1")
    ]

    guest_ips = [
        # Non-ISS IP address.
        IPAddress("2a02:6b8:c08:6a08:10b:6792:9:4866"),
        # ISS IP address with property 'HBF_NAT = disabled'.
        ISSIPv6Address("2a02:6b8:c08:6a08:10b:6792:9:4866", iss_hbf_nat="disabled"),
        # Outside of "project id" network.
        ISSIPv6Address("2a02:6b8:d08:6a08:10b:6792:9:4866"),
        # Outside of runtime project id range.
        ISSIPv6Address("2a02:6b8:c08:6a08:20b:6791:15:7221"),
        # Prohibited port range.
        ISSIPv6Address("2a02:6b8:c08:6a08:10b:6792:9:80e8"),
        # Normal.
        ISSIPv6Address("2a02:6b8:c08:6a08:10b:6792:9:4866"),
        ISSIPv6Address("2a02:6b8:c08:6a08:10b:6791:15:7221"),
    ]

    project_id_ips = [
        (IPAddress("2a02:6b8:c08:6a08:10b:6792:9:4866"),
         PortRange(18534, 18541)),
        (IPAddress("2a02:6b8:c08:6a08:10b:6791:15:7221"),
         PortRange(29217, 29224))
    ]

    def test_runtime_project_id(self):
        self.assertFalse(runtime_mtn_nat.runtime_project_id(0x0FFFFFF))
        self.assertTrue(runtime_mtn_nat.runtime_project_id(0x1111111))
        self.assertFalse(runtime_mtn_nat.runtime_project_id(0x2000000))

    def test_run(self):
        with mock.patch("__builtin__.open", mock.mock_open(), create=True) as open:
            open.return_value.readline.side_effect = [
                "{}\t{}\n".format(*self.local_range),
                ""
            ]
            tables = runtime_mtn_nat.run("v6", self.host_ips, self.guest_ips)
            print tables
            self.assertEqual(tables, self.mtn_nat_tables)

    def test_run_v4(self):
        tables = runtime_mtn_nat.run("v4", None, None)
        self.assertEqual(tables, ipt.IPTables("v4"))

    def test_get_local_port_range(self):
        with mock.patch("__builtin__.open", mock.mock_open(), create=True) as open:
            open.return_value.readline.return_value = \
                "{}\t{}\n".format(*self.local_range)
            range = runtime_mtn_nat.get_local_port_range()
            self.assertEqual(range, self.local_range)

    def test_get_local_port_range_exception(self):
        with mock.patch("__builtin__.open", mock.mock_open(), create=True) as open:
            open.return_value.readline.side_effect = IOError
            range = runtime_mtn_nat.get_local_port_range()
            self.assertEqual(range, self.default_local_range)

    def test_get_local_reserved_ports(self):
        with mock.patch("__builtin__.open", mock.mock_open(), create=True) as open:
            open.return_value.readline.return_value = self.local_reserved_ports_str
            reserved_ports = runtime_mtn_nat.get_local_reserved_ports()
            self.assertEqual(reserved_ports, self.local_reserved_ports)

    def test_get_local_reserved_ports_empty(self):
        with mock.patch("__builtin__.open", mock.mock_open(), create=True) as open:
            open.return_value.readline.return_value = ""
            reserved_ports = runtime_mtn_nat.get_local_reserved_ports()
            self.assertEqual(reserved_ports, set())

    def test_ok_for_nat(self):
        ok_for_nat = runtime_mtn_nat.ok_for_nat
        PortRange = runtime_mtn_nat.PortRange
        local = 32768
        reserved = set(range(8999, 9002))
        self.assertFalse(ok_for_nat(PortRange(1023, 1030), local, reserved))
        self.assertFalse(ok_for_nat(PortRange(33333, 33340), local, reserved))
        self.assertFalse(ok_for_nat(PortRange(9000, 9007), local, reserved))
        self.assertTrue(ok_for_nat(PortRange(31337, 31344), local, reserved))

    def test_gen_mtn_nat_rules(self):
        tables = runtime_mtn_nat.gen_mtn_nat_rules(self.host_ips,
                                                   self.project_id_ips)
        self.assertEqual(tables, self.mtn_nat_tables)


if __name__ == "__main__":
    unittest.main()
