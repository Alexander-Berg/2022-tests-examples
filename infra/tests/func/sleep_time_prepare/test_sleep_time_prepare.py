from __future__ import unicode_literals

import time

import utils
from instancectl import common


def test_restart_prepare_script_crash(cwd, ctl, ctl_environment, request):
    ctl_environment["INSTANCECTL_MOCK_RETRY_SLEEPER_OUTPUT"] = 'tmp.txt'
    ctl_environment["PREPARE_SCRIPT_MAX_JITTER"] = '0'
    ctl_environment["PREPARE_SCRIPT_MAX_TRIES"] = '4'
    ctl_environment["PREPARE_SCRIPT_MAX_DELAY"] = '5'

    p = utils.must_start_instancectl(ctl, request, ctl_environment)
    expected = ("retry_sleep prepare_test_sleep_time 1.0\n"
                "retry_sleep prepare_test_sleep_time 2.0\n"
                "retry_sleep prepare_test_sleep_time 4.0\n"
                "retry_sleep prepare_test_sleep_time 5.0\n")
    while True:
        time.sleep(0.5)
        if p.poll() is None:
            continue
        res = ctl.dirpath('tmp.txt').read()
        assert p.poll() == common.INSTANCE_CTL_CANNOT_INIT
        assert res == expected
        break
