from __future__ import unicode_literals

import time

from utils import must_start_instancectl, must_stop_instancectl


def test_iss_arguments(cwd, ctl, request, ctl_environment):

    must_start_instancectl(ctl, request, ctl_environment)
    time.sleep(10)

    expected_number = 1543 + 1 + 1543
    output = 'test_one.txt {} ZZZ 27 --main 8080 --extra 8081 --bsconfig-iport-plus-1 1544'.format(expected_number)

    lines = cwd.join('test_one.txt').readlines()
    assert len(lines) == 1
    assert ' '.join(lines[0].split()[2:]) == output
    assert cwd.join('port_plus_1.txt').read().strip() == '1544'

    must_stop_instancectl(ctl, check_loop_err=False)
