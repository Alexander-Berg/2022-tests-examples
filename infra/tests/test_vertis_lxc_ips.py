#! /usr/bin/env python2

import mock
import unittest

from hbfagent.mod import vertis_lxc_ips
from hbfagent.util import join_lines


class TestVertisLxcIPs(unittest.TestCase):

    vertis_lxc_ips.LXCBASEDIR = "/"

    ls_reply = join_lines(
        "ya.ru",
        "non-existing-domain1.ru",
    )

    lxc_ips = {"2a02:6b8::2:242"}

    def test_run(self):
        with mock.patch("subprocess.check_output") as check_output:
            check_output.return_value = self.ls_reply
            ips = vertis_lxc_ips.run()

            self.assertSetEqual(self.lxc_ips, ips)


if __name__ == "__main__":
    unittest.main()
