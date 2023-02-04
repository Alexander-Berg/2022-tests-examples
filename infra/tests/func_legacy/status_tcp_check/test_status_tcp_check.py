from __future__ import unicode_literals

import subprocess
import time
import socket


from instancectl.jobs.job import JobStatusCheckResult
import utils


def test_status_tcp_check_timeout(ctl, patch_loop_conf, request, ctl_environment):
    env = ctl_environment.copy()

    # Bind on port but don't listen: tcp check will be timed out
    s = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    s.bind(('', 0))
    request.addfinalizer(s.close)
    env['BSONFIG_IPORT'] = unicode(s.getsockname()[1])

    utils.must_start_instancectl(ctl, request, ctl_environment=ctl_environment)
    time.sleep(5)
    status_process = subprocess.Popen([ctl.strpath, 'status', '--max-tries', '1', '--req-timeout', '1'],
                                      cwd=ctl.dirname, env=ctl_environment)
    s = time.time()
    while time.time() < s + 10 and status_process.poll() is None:
        time.sleep(1.0)
    assert status_process.poll() == JobStatusCheckResult.PENDING
    utils.must_stop_instancectl(ctl)
