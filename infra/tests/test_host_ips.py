#! /usr/bin/env python2

import mock
import unittest

from hbfagent.mod import host_ips
from hbfagent.util import join_lines


class TestHostIPs(unittest.TestCase):

    ip_o_addr = join_lines(
        "1: lo    inet 127.0.0.1/8 scope host lo\\       valid_lft forever preferred_lft forever",
        "1: lo    inet6 ::1/128 scope host \\       valid_lft forever preferred_lft forever",
        "2: eth0    inet6 2a02:6b8:b000:672:feaa:14ff:fea9:7920/64 scope global dynamic \\       valid_lft 2591996sec preferred_lft 604796sec",
        "2: eth0    inet6 fe80::feaa:14ff:fea9:7920/64 scope link \\       valid_lft forever preferred_lft forever",
        "8: L3-0    inet6 fe80::d8a5:16ff:fe9a:ac8/64 scope link \\       valid_lft forever preferred_lft forever",
        "9: L3-1    inet6 fe80::6c8f:bfff:fef7:74dc/64 scope link \\       valid_lft forever preferred_lft forever",
        "34: vlan688    inet6 2a02:6b8:c08:6a08:0:604:0:1/64 scope global \\       valid_lft forever preferred_lft forever",
        "34: vlan688    inet6 fe80::a:8/64 scope link \\       valid_lft forever preferred_lft forever",
        "34: vlan688    inet6 fe80::feaa:14ff:fea9:7920/64 scope link \\       valid_lft forever preferred_lft forever",
        "35: vlan761    inet6 2a02:6b8:f000:13:feaa:14ff:fea9:7920/64 scope global dynamic \\       valid_lft 2591996sec preferred_lft 604796sec",
        "35: vlan761    inet6 fe80::feaa:14ff:fea9:7920/64 scope link \\       valid_lft forever preferred_lft forever",
        "36: vlan788    inet6 2a02:6b8:fc00:6a08:0:604:0:1/64 scope global \\       valid_lft forever preferred_lft forever",
        "36: vlan788    inet6 fe80::a:8/64 scope link \\       valid_lft forever preferred_lft forever",
        "36: vlan788    inet6 fe80::feaa:14ff:fea9:7920/64 scope link \\       valid_lft forever preferred_lft forever",
        "37: vlan711    inet6 fe80::feaa:14ff:fea9:7920/64 scope link \\       valid_lft forever preferred_lft forever",
        "88: L3-37    inet6 fe80::8e3:4dff:fec8:52d2/64 scope link \\       valid_lft forever preferred_lft forever",
        "89: L3-38    inet6 fe80::24b4:ffff:fe0a:63e2/64 scope link \\       valid_lft forever preferred_lft forever",
    )

    ips = {
        "2a02:6b8:f000:13:feaa:14ff:fea9:7920",
        "2a02:6b8:fc00:6a08:0:604:0:1",
        "2a02:6b8:b000:672:feaa:14ff:fea9:7920",
        "2a02:6b8:c08:6a08:0:604:0:1"
    }

    def test_run(self):
        with mock.patch("subprocess.check_output") as check_output:
            check_output.return_value = self.ip_o_addr
            ips = host_ips.run()
            self.assertEqual(ips, self.ips)


if __name__ == "__main__":
    unittest.main()
