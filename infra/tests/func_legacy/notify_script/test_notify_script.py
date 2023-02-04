from __future__ import unicode_literals

import time
import subprocess

from sepelib.subprocess import util

from utils import must_stop_instancectl, must_start_instancectl


def test_notify(cwd, ctl, patch_loop_conf, ctl_environment, request):

    # Case 1: no instancectl running, hook must fail
    p = subprocess.Popen([str(ctl), '--console', 'notify'], cwd=ctl.dirname, env=ctl_environment,
                         stdout=subprocess.PIPE)
    assert p.wait() != 0

    # Case 2: instancectl running, hook must be OK
    must_start_instancectl(ctl, request, ctl_environment)

    cmd = [ctl.strpath,
           '--console',
           'notify',
           '--updates',
           '+some_added_resource',
           '-some_removed_resource',
           '!some_changed_resource']
    p = subprocess.Popen(cmd, cwd=ctl.dirname, env=ctl_environment, stdout=subprocess.PIPE)
    rc = p.wait()
    assert rc == 0

    must_stop_instancectl(ctl)
    result = cwd.join('notify_result.txt').read().strip()
    expected = 'notify_action +some_added_resource -some_removed_resource !some_changed_resource 3 HELLO'
    assert result == expected


def test_long_notify(cwd, ctl, patch_loop_conf, ctl_environment, request):
    ctl.dirpath('time_to_sleep.txt').write('3')

    must_start_instancectl(ctl, request, ctl_environment)

    cmd = [ctl.strpath,
           '--console',
           'notify',
           '--updates',
           '+some_added_resource',
           '-some_removed_resource',
           '!some_changed_resource']
    processes = []
    for i in xrange(5):
        p = subprocess.Popen(cmd, cwd=ctl.dirname, env=ctl_environment, stdout=subprocess.PIPE)
        request.addfinalizer(lambda: util.terminate(p))
        processes.append(p)
    time.sleep(15)
    for p in processes:
        assert p.poll() == 0
    must_stop_instancectl(ctl)
    result = cwd.join('notify_result.txt').readlines()
    assert len(result) == 2
    for l in result:
        assert l.strip() == 'notify_action +some_added_resource -some_removed_resource !some_changed_resource 3 HELLO'
