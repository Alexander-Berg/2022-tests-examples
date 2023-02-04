from __future__ import unicode_literals

import subprocess


def test_uninstall(cwd, ctl, ctl_environment):
    assert not cwd.join('uninstall_result.txt').exists()

    p = subprocess.Popen([str(ctl), 'uninstall'], cwd=ctl.dirname, env=ctl_environment)
    p.wait()

    assert p.poll() == 0
    assert cwd.join('uninstall_result.txt').exists()
