#! /usr/bin/env python2

import ipaddr
import mock
import unittest

from hbfagent.mod import vertis_docker


class TestVertisDockerNets(unittest.TestCase):
    config = """
    2a02:6b8:c08:c717:0:1459:f:2/112
    2a02:
    2a02:6b8:c08:c717:0:d:0::/112
    """

    def test_run(self):
        with mock.patch("__builtin__.open", mock.mock_open(read_data=self.config)) as mock_file:
            ips = vertis_docker.run()
            mock_file.assert_called_with(vertis_docker.NET_FILE)

            expected_ips = {
                ipaddr.IPv6Network("2a02:6b8:c08:c717:0:1459:f::/112"),
                ipaddr.IPv6Network("2a02:6b8:c08:c717:0:d:0:0/112")
            }

            self.assertEqual(ips, expected_ips)
            self.assertEqual(len(ips), 2)


if __name__ == "__main__":
    unittest.main()
