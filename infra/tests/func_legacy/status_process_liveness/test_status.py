from __future__ import unicode_literals

import subprocess
import time

from utils import must_start_instancectl, must_stop_instancectl

from instancectl.jobs.job import JobStatusCheckResult


def test_status_check_process(ctl, patch_loop_conf, request, ctl_environment):
    p = must_start_instancectl(ctl, request, ctl_environment=ctl_environment)

    status_process = subprocess.Popen([ctl.strpath, 'status', '--max-tries', '1'], cwd=ctl.dirname, env=ctl_environment)
    status_process.wait()
    assert status_process.poll() == JobStatusCheckResult.PENDING

    time.sleep(10)

    status_process = subprocess.Popen([ctl.strpath, 'status', '--max-tries', '1'],
                                      cwd=ctl.dirname, env=ctl_environment)
    status_process.wait()
    assert status_process.poll() == JobStatusCheckResult.STARTED

    must_stop_instancectl(ctl, check_loop_err=False, process=p)
