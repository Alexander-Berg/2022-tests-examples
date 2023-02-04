from __future__ import unicode_literals

import time

import utils


def test_restart_install_script(cwd, ctl, ctl_environment, request):
    ctl_environment["INSTANCECTL_MOCK_RETRY_SLEEPER_OUTPUT"] = 'tmp.txt'
    ctl_environment["INSTALL_SCRIPT_MAX_JITTER"] = '0'
    ctl_environment["INSTALL_SCRIPT_MAX_TRIES"] = '5'
    p = utils.must_start_instancectl(ctl, request, ctl_environment)

    expected = ("retry_sleep install_script 1.0\n"
                "retry_sleep install_script 1.05\n"
                "retry_sleep install_script 1.1025\n"
                "retry_sleep install_script 1.157625\n"
                "retry_sleep install_script 1.21550625\n")
    while True:
        time.sleep(0.5)
        if p.poll() is None:
            continue
        res = ctl.dirpath('tmp.txt').read()
        assert p.poll() == 0
        assert res == expected
        break
