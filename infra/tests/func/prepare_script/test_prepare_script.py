from __future__ import unicode_literals
import subprocess
import time

from sepelib.subprocess.util import terminate


def test_install(cwd, ctl, ctl_environment, request):
    assert not cwd.join('state/short_test_prepare_script_install.flag').exists()
    assert not cwd.join('state/long_test_prepare_script_install.flag').exists()

    p = subprocess.Popen(ctl.strpath, cwd=ctl.dirname, env=ctl_environment)
    request.addfinalizer(lambda: terminate(p))
    time.sleep(10)

    assert cwd.join('state', 'loop.lock').exists()
    assert not cwd.join('short_install_script.txt').exists()
    assert not cwd.join('long_install_script.txt').exists()

    cwd.join('flag.txt').write('OK')

    time.sleep(10)
    assert cwd.join('state/prepare_short_test_prepare_script_install.flag').exists()
    assert cwd.join('state/prepare_long_test_prepare_script_install.flag').exists()
    assert cwd.join('short_install_script.txt').exists()
    assert cwd.join('long_install_script.txt').exists()
    assert cwd.join('order.txt').read().strip() == '1\n2'
