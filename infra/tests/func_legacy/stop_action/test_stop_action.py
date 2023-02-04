# coding=utf-8
import time
import multiprocessing
import os
import socket
import subprocess
from ConfigParser import SafeConfigParser

from sepelib.subprocess.util import terminate
from utils import must_start_instancectl, wait_for_unixsocket, must_stop_instancectl


def test_action_stop_timeout_good_case(ctl, patch_loop_conf, ctl_environment, request):
    must_start_instancectl(ctl, request, ctl_environment)
    stop_process = subprocess.Popen([ctl.strpath, '--console', 'stop'], cwd=ctl.dirname)
    stop_process.wait()
    assert stop_process.returncode == 0
    must_stop_instancectl(ctl, check_loop_err=False, raise_if_not_started=False)


def test_action_stop_timeout_bad_case(cwd, ctl, patch_loop_conf, ctl_environment, request):
    conf_file = cwd.join('loop.conf').strpath
    parser = SafeConfigParser()
    parser.read(conf_file)
    parser.set('defaults', 'action_stop_timeout', '0')
    with open(conf_file, 'w') as fd:
        parser.write(fd)

    # проверяем ненулевой код возврата из команды ./instancectl stop если инстанс
    # завершается дольше, чем action_stop_timeout
    must_start_instancectl(ctl, request, ctl_environment)

    stop_process = subprocess.Popen([ctl.strpath, 'stop'], cwd=ctl.dirname)
    stop_process.wait()
    assert stop_process.returncode != 0
    time.sleep(10)
    must_stop_instancectl(ctl, check_loop_err=False, raise_if_not_started=False)


def test_stop_uninstalled(cwd, ctl, patch_loop_conf, ctl_environment):
    assert not cwd.join('state', 'instance.conf').exists()

    p = subprocess.Popen([str(ctl), 'stop'], cwd=ctl.dirname, env=ctl_environment)
    p.wait()

    assert p.poll() == 0
    assert not cwd.join('state', 'instance.conf').exists()


def test_stop_failed(cwd, ctl, request, ctl_environment):

    def socket_bind_process():
        os.chdir(ctl.dirname)
        s = socket.socket(socket.AF_UNIX)
        s.bind('instancectl.sock')
        s.listen(1)
        try:
            while True:
                conn, addr = s.accept()
                conn.close()
        finally:
            s.close()

    fake_process = multiprocessing.Process(target=socket_bind_process)
    fake_process.start()
    request.addfinalizer(fake_process.terminate)

    assert wait_for_unixsocket(ctl, request)

    with open(os.devnull, 'w') as FNULL:
        # stacktrace to devnull
        stop_process = subprocess.Popen([ctl.strpath, 'stop'], cwd=ctl.dirname, env=ctl_environment,
                                        stderr=FNULL)
        stop_process.wait()
        assert stop_process.returncode != 0


def test_socket_conn_refused(cwd, ctl, ctl_environment, request):
    binder = 'import socket, time; socket.socket(socket.AF_UNIX).bind("instancectl.sock"); time.sleep(30)'
    fake_ctl = subprocess.Popen(['python', '-c', binder], cwd=ctl.dirname, env=ctl_environment,
                                stderr=None)
    request.addfinalizer(lambda: terminate(fake_ctl))

    stop_process = subprocess.Popen([ctl.strpath, 'stop'], cwd=ctl.dirname, env=ctl_environment,
                                    stderr=None)
    stop_process.wait()
    assert stop_process.returncode == 0
