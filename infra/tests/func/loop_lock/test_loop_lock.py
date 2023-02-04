from __future__ import unicode_literals

import time
import utils
import subprocess

from sepelib.subprocess import util

from instancectl import common


def test_loop_lock_acquiring(ctl, ctl_environment, request):

    # Start the first copy which must lock loop.lock file
    p_first = utils.must_start_instancectl(ctl, request, ctl_environment)

    # Then start the second which must die with appropriate exit code
    p = subprocess.Popen(ctl.strpath, env=ctl_environment)
    request.addfinalizer(lambda: util.terminate(p))

    s = time.time()
    while p.poll() is None and time.time() - s < 10.0:
        time.sleep(0.1)

    assert p.poll() == common.INSTANCE_CTL_CANNOT_ACQUIRE_LOCK_EXIT_CODE

    utils.must_stop_instancectl(ctl, check_loop_err=False, process=p_first)
