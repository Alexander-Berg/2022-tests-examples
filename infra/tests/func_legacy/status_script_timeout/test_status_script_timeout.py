from __future__ import unicode_literals

import subprocess
import time

from utils import must_start_instancectl

from instancectl.jobs.job import JobStatusCheckResult


def test_status_script_timeout(ctl, patch_loop_conf, request, ctl_environment):
    must_start_instancectl(ctl, request, ctl_environment=ctl_environment)
    time.sleep(5)
    status_process = subprocess.Popen([ctl.strpath, 'status', '--max-tries', '1', '--req-timeout', '1'], cwd=ctl.dirname, env=ctl_environment)
    status_process.wait()
    assert status_process.poll() == JobStatusCheckResult.PENDING
