from __future__ import unicode_literals

import os
import signal
import time

import utils


def test_restart_script_env(cwd, ctl, ctl_environment, request):
    utils.must_start_instancectl(ctl, request, ctl_environment)
    time.sleep(10)
    expected = 'EXIT_STATUS=123 TERM_SIGNAL= COREDUMPED='
    assert cwd.join('exited.txt').readlines()[0].strip() == expected
    test_pid = ctl.dirpath('pids', 'test_restart_script_signaled').read()
    os.kill(int(test_pid), signal.SIGABRT)
    time.sleep(10)
    expected_coredumped = 'EXIT_STATUS= TERM_SIGNAL={} COREDUMPED=1'.format(signal.SIGABRT)
    expected_no_coredump = 'EXIT_STATUS= TERM_SIGNAL={} COREDUMPED='.format(signal.SIGABRT)
    actual = cwd.join('signaled.txt').readlines()[0].strip()
    assert actual in [expected_coredumped, expected_no_coredump]
    utils.must_stop_instancectl(ctl, check_loop_err=False)
