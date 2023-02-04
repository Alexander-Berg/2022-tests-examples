from __future__ import unicode_literals

import os

from utils import must_start_instancectl, must_stop_instancectl


def test_renaming(cwd, ctl, ctl_environment, request):
    must_start_instancectl(ctl, request, ctl_environment)

    assert os.path.isfile(str(cwd.join('srch-base-{}'.format(ctl_environment['BSCONFIG_IPORT']))))

    must_stop_instancectl(ctl)


def test_kill(cwd, ctl, ctl_environment, request):
    must_start_instancectl(ctl, request, ctl_environment)

    pid = int(cwd.join('pids', 'test_minor_features').read())

    os.kill(pid, 9)

    # We have delay == 100 in conf file, instance must not be restarted

    new_pid = int(cwd.join('pids', 'test_minor_features').read())

    assert new_pid == pid

    must_stop_instancectl(ctl)

    pids = []
    with cwd.join('result.txt').open() as fd:
        for line in fd:
            pids.append(line.split()[1])

    assert len(pids) == 1
