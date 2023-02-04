from __future__ import unicode_literals

import time
import subprocess
import ConfigParser

from sepelib.subprocess import util

import utils


def test_bad_names(cwd, ctl, ctl_environment, request):
    # Start instancectl without forbidden section names
    p = utils.must_start_instancectl(ctl, request, ctl_environment)
    utils.must_stop_instancectl(ctl, check_loop_err=False, process=p)

    # Add forbidden section name
    conf_file = cwd.join('loop.conf').strpath
    parser = ConfigParser.SafeConfigParser()
    parser.read(conf_file)
    parser.add_section('instancectl')
    parser.set('instancectl', 'binary', '/bin/sleep')
    parser.set('instancectl', 'arguments', '10')
    with open(conf_file, 'w') as fd:
        parser.write(fd)

    # Then start the second which must die
    p = subprocess.Popen(ctl.strpath, env=ctl_environment)
    request.addfinalizer(lambda: util.terminate(p))

    s = time.time()
    while p.poll() is None and time.time() - s < 10.0:
        time.sleep(0.1)

    assert p.poll() != 0
