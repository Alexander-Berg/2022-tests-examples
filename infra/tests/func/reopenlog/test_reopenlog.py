from __future__ import unicode_literals

import subprocess

from utils import must_stop_instancectl, must_start_instancectl


def test_reopenlog(cwd, ctl, ctl_environment, request):

    # Case 1: no instancectl running, hook must fail
    p = subprocess.Popen([str(ctl), '--console', 'reopenlog'], cwd=ctl.dirname, env=ctl_environment,
                         stdout=subprocess.PIPE)
    assert p.wait() != 0

    # Case 2: instancectl running, hook must be OK
    must_start_instancectl(ctl, request, ctl_environment)

    p = subprocess.Popen([str(ctl), 'reopenlog'], cwd=ctl.dirname, env=ctl_environment, stdout=subprocess.PIPE)
    rc = p.wait()
    assert rc == 0

    must_stop_instancectl(ctl)
    result = cwd.join('reopenlog.txt').readlines()
    assert ['REOPENLOG CALLED1', 'REOPENLOG CALLED2'] == sorted(x.strip() for x in result)
