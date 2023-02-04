from __future__ import unicode_literals

import os
import signal
import time

import pytest
import utils

from instancectl.jobs.job import JobStatusCheckResult


@pytest.mark.parametrize('resources', ['resources_criterion_list', 'resources_criterion_all'])
def test_liveness_criterion(ctl, patch_loop_conf, request, resources, ctl_environment):
    p = utils.must_start_instancectl(ctl, request, ctl_environment, console_logging=True)

    time.sleep(10)
    assert utils.get_current_status(ctl, request) == JobStatusCheckResult.STARTED

    test_pid = ctl.dirpath('pids', 'test1_liveness_criterion').read()
    os.kill(int(test_pid), signal.SIGTERM)

    time.sleep(3)
    assert utils.get_current_status(ctl, request) == JobStatusCheckResult.PENDING

    time.sleep(5)
    assert utils.get_current_status(ctl, request) == JobStatusCheckResult.STARTED

    utils.must_stop_instancectl(ctl, check_loop_err=False, process=p)
