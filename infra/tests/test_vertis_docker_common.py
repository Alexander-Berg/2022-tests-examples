#! /usr/bin/env python2

import mock
import unittest

from hbfagent.mod import vertis_docker_common


class TestVertisDockerCommonIPs(unittest.TestCase):

    config = """
        {
            "ipv6": true,
            "fixed-cidr-v6": "2a02:6b8:c08:c717:0:1459:0:2/112",
            "fixed-cidr": "172.18.39.0/24",
            "mtu": 8950,
            "default-gateway-v6": "2a02:6b8:c08:c717:0:1459:0:2",
            "storage-driver": "overlay2",
            "dns": ["2a02:6b8:c02:55b:0:1459:da69:595e"],
            "dns-opts": ["timeout:1", "attempts:2"],
            "debug": true
        }
    """

    correct_ip = {
        "2a02:6b8:c08:c717:0:1459:0:3",
        "2a02:6b8:c08:c717:0:1459:0:4",
        "2a02:6b8:c08:c717:0:1459:0:c7",
    }

    wrong_ip_1 = {"2a02:6b8:c08:c717:0:1459:1:0"}
    wrong_ip_2 = {"2a02:6b8:c08:c717:0:1459:1:1"}

    def test_run(self):

        with mock.patch("os.path.isfile") as isfile:
            with mock.patch("os.access") as access:
                with mock.patch("__builtin__.open", mock.mock_open(read_data=self.config)) as mock_file:
                    assert open(vertis_docker_common.DOCKER_CONFIG).read() == self.config
                    mock_file.assert_called_with(vertis_docker_common.DOCKER_CONFIG)

                    isfile.return_value = True
                    access.return_value = True

                    ips = vertis_docker_common.run()

                    self.assertTrue(ips)

                    self.assertTrue(self.correct_ip.issubset(ips), "test correct_ip")
                    self.assertFalse(self.wrong_ip_1.issubset(ips), "test wrong_ip_1")
                    self.assertFalse(self.wrong_ip_2.issubset(ips), "test wrong_ip_2")
                    self.assertEqual(len(ips), 200)


if __name__ == "__main__":
    unittest.main()
