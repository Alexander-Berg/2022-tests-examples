from __future__ import unicode_literals

import time

import utils

from instancectl.jobs.job import JobStatusCheckResult


def test_liveness(ctl, request, ctl_environment):
    utils.must_start_instancectl(ctl, request, ctl_environment)

    time.sleep(10)
    assert utils.get_current_status(ctl, request) == JobStatusCheckResult.STARTED

    ctl.dirpath('test1.txt').write('1')

    time.sleep(5)
    assert utils.get_current_status(ctl, request) == JobStatusCheckResult.PENDING

    ctl.dirpath('test1.txt').write('0')

    time.sleep(5)
    assert utils.get_current_status(ctl, request) == JobStatusCheckResult.STARTED

    utils.must_stop_instancectl(ctl, check_loop_err=False)
