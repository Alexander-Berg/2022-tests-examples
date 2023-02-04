# coding=utf-8
import os
import subprocess
from ConfigParser import SafeConfigParser

from utils import must_start_instancectl, must_stop_instancectl
from utils import wait_file_is_not_locked


def test_terminate_timeout(cwd, ctl, patch_loop_conf, ctl_environment, request):
    lock_file = os.path.join(cwd.strpath, 'state', 'loop.lock')

    must_start_instancectl(ctl, request, ctl_environment)
    stop_process = subprocess.Popen([str(ctl), 'stop'], cwd=ctl.dirname)
    stop_process.wait()

    assert wait_file_is_not_locked(lock_file), 'Control failed to stop!'

    assert not cwd.join('signals.txt').exists()

    must_stop_instancectl(ctl, raise_if_not_started=False)


def test_terminate_timeout_without_stop_script(cwd, ctl, patch_loop_conf, ctl_environment, request):
    lock_file = os.path.join(cwd.strpath, 'state', 'loop.lock')

    conf_file = cwd.join('loop.conf').strpath
    parser = SafeConfigParser()
    parser.read(conf_file)
    parser.set('test_terminate_timeout', 'stop_script', '')
    with open(conf_file, 'w') as fd:
        parser.write(fd)

    # Проверяем, что при отсутствии stop_script'а мы сразу отправляем
    # демону TERM, без ожидания terminate_timeout

    must_start_instancectl(ctl, request, ctl_environment)

    stop_process = subprocess.Popen([str(ctl), 'stop'], cwd=ctl.dirname)
    stop_process.wait()

    assert wait_file_is_not_locked(lock_file), 'Control failed to stop!'

    assert cwd.join('signals.txt').read().strip() == '15'

    must_stop_instancectl(ctl, raise_if_not_started=False)

