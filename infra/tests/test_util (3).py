#! /usr/bin/env python2

import unittest

from ipaddr import IPv4Address, IPv6Address

from hbfagent import util


class TestISSIPv6Address(unittest.TestCase):

    def setUp(self):
        self.address = util.ISSIPv6Address(
            "2a02:6b8::1", iss_hbf_nat=None
        )

    def test_init(self):
        self.assertEqual(self.address.iss_hbf_nat, None)

    def test_eq(self):
        other = util.ISSIPv6Address(
            "2a02:6b8::1", iss_hbf_nat=None
        )
        self.assertEqual(self.address, other)

        other = util.ISSIPv6Address(
            "2a02:6b8::1", iss_hbf_nat="disabled"
        )
        self.assertNotEqual(self.address, other)


class TestUtil(unittest.TestCase):

    ipv4_addr = IPv4Address("77.88.8.8")
    pid_addr = IPv6Address("2a02:6b8:c00:0:7e57:1d:0:7a69")
    non_pid_addr = IPv6Address("2a02:6b8:d00:0:7e57:1d:0:7a69")
    lines = (
        "Ras.",
        "Dva.",
        "Tri."
    )

    def test_get_project_id(self):
        self.assertIsNone(util.get_project_id(self.ipv4_addr))
        self.assertIsNone(util.get_project_id(self.non_pid_addr))
        self.assertEqual(util.get_project_id(self.pid_addr), 0x7e57001d)

    def test_get_port(self):
        self.assertEqual(util.get_port(self.non_pid_addr), 31337)

    def test_enumerate_lines(self):
        string = util.join_lines(*self.lines)
        enumerated = util.enumerate_lines(string)
        for line in self.lines:
            self.assertIn(line, enumerated)

    def test_join_lines(self):
        ugly = (
            "Ras.\n"
            "Dva.\n"
            "Tri.\n"
        )
        bad = util.join_lines(*self.lines)
        self.assertEqual(ugly, bad)


if __name__ == "__main__":
    unittest.main()
