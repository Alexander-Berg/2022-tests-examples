# coding=utf-8
from __future__ import unicode_literals

import subprocess
import time

import pytest
from sepelib.subprocess import util
import utils

from instancectl.jobs.job import JobStatusCheckResult


def status_must_return_code(ctl, request, expected, env, timeout):
    s = time.time()
    rc = None
    while time.time() - s < timeout:
        p = subprocess.Popen([ctl.strpath, 'status', '--max-tries', '1'], cwd=ctl.dirname, env=env)
        request.addfinalizer(lambda: util.terminate(p))
        rc = p.wait()
        if rc == expected:
            break
        time.sleep(0.5)
    assert rc == expected


@pytest.mark.parametrize('resources', ['resources', 'resources2'])
def test_status_simple(cwd, ctl, patch_loop_conf, resources, request, ctl_environment):
    """
    Проверяем вывод статусов (при незаинсталленом инстансе, при незапущенном ctl, при незапущенном инстансе)

    Логика описана в SWAT-1504

    Запускаем инстанс с 2 секциями в двух вариантах:
    * В одной секции status_script всегда возвращает 0, в другой результатом управляем через файлик
    * В одной секции нет status_script, в другой результатом управляем через файлик

    :type cwd: py._path.local.LocalPath

    """
    status_must_return_code(ctl, request, JobStatusCheckResult.PENDING, ctl_environment, 5)

    p = utils.must_start_instancectl(ctl, request, ctl_environment)
    control_file = cwd.join('long_test_rc.txt')
    control_file.write('0', 'w')
    status_must_return_code(ctl, request, JobStatusCheckResult.STARTED, ctl_environment, 5)

    control_file.write('1', 'w')
    status_must_return_code(ctl, request, JobStatusCheckResult.PENDING, ctl_environment, 5)

    control_file.write('0', 'w')
    status_must_return_code(ctl, request, JobStatusCheckResult.STARTED, ctl_environment, 5)

    utils.must_stop_instancectl(ctl, check_loop_err=False, process=p)
    status_must_return_code(ctl, request, JobStatusCheckResult.PENDING, ctl_environment, 5)


@pytest.mark.parametrize('resources,result', [('resources3', JobStatusCheckResult.PENDING),
                                              ('resources4', JobStatusCheckResult.STARTED)])
def test_status_check_port(ctl, patch_loop_conf, request, resources, result, ctl_environment):
    """
    Проверяем вывод статусов (при незаинсталленом инстансе, при незапущенном ctl, при незапущенном инстансе)

    * В обеих секциях нет status_script'а и порт не занят => результат PENDING
    * В обеих секциях нет status_script'а и порт занят => результат STARTED
    """
    ctl_environment['HOSTNAME'] = 'localhost'
    ctl_environment['BSCONFIG_IPORT'] = unicode(utils.get_free_port())
    status_must_return_code(ctl, request, JobStatusCheckResult.PENDING, ctl_environment, 5)

    p = utils.must_start_instancectl(ctl, request, ctl_environment=ctl_environment)

    status_must_return_code(ctl, request, result, ctl_environment, 5)

    utils.must_stop_instancectl(ctl, check_loop_err=False, process=p)

    status_must_return_code(ctl, request, JobStatusCheckResult.PENDING, ctl_environment, 10)


@pytest.mark.parametrize('resources', ('resources5',))
def test_status_multiple_attempts(cwd, ctl, patch_loop_conf, request, resources, ctl_environment):
    cwd.join('attempt.txt').write('1')
    p = utils.must_start_instancectl(ctl, request, ctl_environment=ctl_environment)
    status_process = subprocess.Popen([ctl.strpath, 'status', '--max-tries', '2', '--min-delay', '7'],
                                      cwd=ctl.dirname, env=ctl_environment)
    status_process.wait()
    assert status_process.poll() == JobStatusCheckResult.STARTED
    utils.must_stop_instancectl(ctl, check_loop_err=False, process=p)


