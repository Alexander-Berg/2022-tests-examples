from __future__ import unicode_literals

import subprocess

from utils import must_start_instancectl, must_stop_instancectl


def test_install(cwd, ctl, ctl_environment, request):
    """
    Test install script
    """
    assert not cwd.join('short_test_install_action.txt').exists()
    assert not cwd.join('long_test_install_action.txt').exists()

    p = subprocess.Popen([str(ctl), 'install'], cwd=ctl.dirname, env=ctl_environment)
    p.wait()

    assert p.poll() == 0
    assert cwd.join('state', 'instance.conf').exists()

    assert cwd.join('short_test_install_action.txt').read().strip() == 'short_test_install_action'
    assert cwd.join('long_test_install_action.txt').read().strip() == 'long_test_install_action'
    assert cwd.join('order.txt').read().strip() == '1\n2'

    p = subprocess.Popen([str(ctl), 'install'], cwd=ctl.dirname, env=ctl_environment)
    p.wait()
    assert p.poll() == 0

    # Check that prepare_script has been executed exactly once
    assert cwd.join('short_test_install_action.txt').read().strip() == 'short_test_install_action'
    assert cwd.join('long_test_install_action.txt').read().strip() == 'long_test_install_action'
    assert cwd.join('order.txt').read().strip() == '1\n2'

    p = must_start_instancectl(ctl, request, ctl_environment)
    must_stop_instancectl(ctl, process=p)

    # Check that prepare_script has been executed exactly once
    assert cwd.join('short_test_install_action.txt').read().strip() == 'short_test_install_action'
    assert cwd.join('long_test_install_action.txt').read().strip() == 'long_test_install_action'
    assert cwd.join('order.txt').read().strip() == '1\n2'


def test_stderr(cwd, ctl, ctl_environment):
    """
    Check that error will be printed to stderr if install action fails.
    """
    cwd.join('loop.conf').write('No loop.conf, just trash!')

    p = subprocess.Popen([str(ctl), 'install'], cwd=ctl.dirname, env=ctl_environment, stderr=subprocess.PIPE,
                         stdout=subprocess.PIPE)
    stdout, stderr = p.communicate()

    assert stdout == ''
    assert stderr != ''
