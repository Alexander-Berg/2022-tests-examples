#! /usr/bin/env python2

import mock
import unittest

from hbfagent.mod import vertis_docker_yacloud
from hbfagent.util import join_lines


class TestVertisDockerYACloudIPs(unittest.TestCase):

    ip_o_addr = join_lines(
        "2: eth0: <BROADCAST,MULTICAST,UP,LOWER_UP> mtu 1500 qdisc mq state UP group default qlen 1000",
        "    link/ether d0:0d:33:71:48:2d brd ff:ff:ff:ff:ff:ff",
        "    inet 172.17.0.36/16 brd 172.17.255.255 scope global eth0",
        "       valid_lft forever preferred_lft forever",
        "    inet6 2a02:6b8:c02:900:0:4d74:0:a01/128 scope global",
        "       valid_lft forever preferred_lft forever",
        "    inet6 fe80::d20d:33ff:fe71:482d/64 scope link",
        "       valid_lft forever preferred_lft forever",
    )

    correct_ip = {
        "2a02:6b8:c02:900:0:4d74:a01:1",
        "2a02:6b8:c02:900:0:4d74:a01:2",
        "2a02:6b8:c02:900:0:4d74:a01:c7",
    }

    wrong_ip_1 = {"2a02:6b8:c02:900:0:4d74:0:a02"}
    wrong_ip_2 = {"2a02:6b8:c02:900:0:4d74:1:a01"}

    def test_run(self):

        with mock.patch("subprocess.check_output") as check_output:
            check_output.return_value = self.ip_o_addr
            ips = vertis_docker_yacloud.run()

            self.assertTrue(self.correct_ip.issubset(ips), "test correct_ip")
            self.assertFalse(self.wrong_ip_1.issubset(ips), "test wrong_ip_1")
            self.assertFalse(self.wrong_ip_2.issubset(ips), "test wrong_ip_2")
            self.assertEqual(len(ips), 200)


if __name__ == "__main__":
    unittest.main()
