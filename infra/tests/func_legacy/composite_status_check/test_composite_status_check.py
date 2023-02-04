from __future__ import unicode_literals

import os
import signal
import subprocess
import time

import pytest
import utils
from sepelib.subprocess import util

from instancectl.jobs.job import JobStatusCheckResult


def get_current_status(ctl, request):
    p = subprocess.Popen([ctl.strpath, 'status', '--max-tries', '1'], cwd=ctl.dirname)
    request.addfinalizer(lambda: util.terminate(p))
    p.wait()
    return p.poll()


@pytest.mark.parametrize('resources', ['resources_composite_check'])
def test_composite_check(ctl, patch_loop_conf, request, resources, ctl_environment):

    ctl_environment['BSCONFIG_IPORT'] = str(utils.get_free_port())
    ctl_environment['HOSTNAME'] = 'localhost'

    p = subprocess.Popen(['python', ctl.dirpath('pyserver.py').strpath, ctl_environment['BSCONFIG_IPORT']])
    request.addfinalizer(lambda: util.terminate(p))
    assert p.poll() is None

    ctl_process = utils.must_start_instancectl(ctl, request, ctl_environment)

    time.sleep(10)
    assert get_current_status(ctl, request) == JobStatusCheckResult.STARTED

    # Fail status script
    ctl.dirpath('test1.txt').write('1')

    time.sleep(5)
    assert get_current_status(ctl, request) == JobStatusCheckResult.PENDING

    # Make status script OK again
    ctl.dirpath('test1.txt').write('0')

    time.sleep(5)
    assert get_current_status(ctl, request) == JobStatusCheckResult.STARTED

    # Fail TCP check
    util.terminate(p)
    time.sleep(3)
    assert get_current_status(ctl, request) == JobStatusCheckResult.PENDING

    # Make TCP check OK again
    p = subprocess.Popen(['python', ctl.dirpath('pyserver.py').strpath, ctl_environment['BSCONFIG_IPORT']])
    request.addfinalizer(lambda: util.terminate(p))
    time.sleep(3)
    assert get_current_status(ctl, request) == JobStatusCheckResult.STARTED

    # Kill the process
    test_pid = ctl.dirpath('pids', 'test_composite_check').read()
    os.kill(int(test_pid), signal.SIGTERM)

    time.sleep(3)
    assert get_current_status(ctl, request) == JobStatusCheckResult.PENDING

    time.sleep(5)
    assert get_current_status(ctl, request) == JobStatusCheckResult.STARTED

    utils.must_stop_instancectl(ctl, check_loop_err=False, process=ctl_process)
