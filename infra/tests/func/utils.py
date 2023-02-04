# coding=utf-8
from __future__ import unicode_literals
from __future__ import print_function

import os
import json
import textwrap
import fcntl
import socket
import time
import subprocess
import ConfigParser

import gevent
from flask import Flask
from flask import request as flask_request
from yp_proto.yp.client.hq.proto import hq_pb2
from sepelib.subprocess.util import terminate
from sepelib.flask import server

from instancectl import utils as instancectl_utils


def lock(fd):
    try:
        fcntl.lockf(fd, fcntl.LOCK_EX | fcntl.LOCK_NB)
    except (IOError, OSError) as e:
        return False

    return True


def is_locked(file_path):
    """
    Проверить, залочен ли файл

    :param file_path: путь до файла
    :return: True, если залочен; False в противном случае
    :rtype: bool
    """
    if not os.path.exists(file_path):
        return False
    with open(file_path, b'a') as file_descriptor:
        return not lock(file_descriptor)


def wait_condition_is_true(condition_function, timeout, check_timeout, *args, **kwargs):
    """
        Подождать выполнения условия-функции

        :param condition_function: функция условия
        :param timeout: таймаут для условия
        :param check_timeout: промежуток между проверками

        :return: True, если условие выполнилось; False в противном случае
        :rtype: bool
    """
    stop_time = time.time() + timeout
    while time.time() < stop_time:
        if condition_function(*args, **kwargs):
            return True
        gevent.sleep(check_timeout)
    return False


def wait_file_is_locked(file_path, timeout=5, check_timeout=0.1):
    """
        Подождать, когда будет залочен файл

        :param file_path: путь до файла
        :param timeout: таймаут для условия
        :param check_timeout: промежуток между проверками

        :return:
        :rtype: bool
    """
    return wait_condition_is_true(is_locked, timeout, check_timeout, file_path)


def wait_file_is_not_locked(file_path, timeout=5, check_timeout=1):
    """
        Подождать, когда будет разлочен файл

        :param file_path: путь до файла
        :param timeout: таймаут для условия
        :param check_timeout: промежуток между проверками

        :return:
        :rtype: bool
    """
    return wait_condition_is_true(lambda x: not is_locked(x), timeout, check_timeout, file_path)


def wait_for_unixsocket(ctl, request, timeout=5.0):
    stop_time = time.time() + timeout

    unix_socket_checker = textwrap.dedent("""
        #!/usr/bin/env python

        import time
        import socket


        if __name__ == '__main___':
            s = socket.socket(socket.AF_UNIX)
            while True:
                try:
                    s.connect('instancectl.sock')
                except socket.error:
                    time.sleep(0.1)
                else:
                    break
            s.close()
    """)

    checker = ctl.dirpath('socket_checker.py')
    checker.write(unix_socket_checker)

    p = subprocess.Popen(['python', checker.strpath])
    request.addfinalizer(lambda: terminate(p))

    while time.time() < stop_time and p.poll() is None:
        gevent.sleep(0.1)
    return p.poll() == 0


def must_start_instancectl(ctl, request, ctl_environment=None, console_logging=False, add_args=()):
    """
    Creates instancectl process and ensures that:
      * loop.lock is acquired by instancectl
      * instancectl listens on unixsocket

    :type request: _pytest.python.FixtureRequest
    :type ctl: py._path.local.LocalPath
    :type ctl_environment: dict
    :type console_logging: bool
    :rtype: subprocess.Popen
    """
    lock_file = ctl.dirpath('state', 'loop.lock').strpath
    cmd = [ctl.strpath]
    if console_logging:
        cmd += ['--console', 'start', '--no-detach']
    else:
        cmd += ['start']
    cmd += list(add_args)
    env = ctl_environment or {}
    env.setdefault('SKIP_FDATASYNC_CONFIG', '1')
    process = subprocess.Popen(cmd, cwd=ctl.dirname, env=env)
    request.addfinalizer(lambda: terminate(process))
    assert wait_file_is_locked(lock_file), 'Control failed to start!'
    assert wait_for_unixsocket(ctl, request), 'Control failed to listen to unixsocket'
    gevent.sleep(1)
    return process


def must_stop_instancectl(ctl, check_loop_err=True, raise_if_not_started=True, process=None):
    """
    Корректно останавливает запущенный instancectl

    * Убеждается, что instancectl запущен (занят loop.lock)
    * Удаляет файл-флажок run.flag
    * Убеждается, что instancectl завершился (loop.lock свободен)

    :type ctl: py._path.local.LocalPath
    :type check_loop_err: bool
    :type raise_if_not_started: bool
    :type process: subprocess.Popen
    """
    lock_file = ctl.dirpath('state', 'loop.lock').strpath
    if raise_if_not_started:
        assert is_locked(lock_file), 'Control crashed!'
    ctl.dirpath('run.flag').remove()
    assert wait_file_is_not_locked(lock_file, timeout=30), 'Control failed to stop!'
    if check_loop_err:
        stderr_file = instancectl_utils.STDERR_FILENAME
        assert ctl.dirpath(stderr_file).stat().size == 0, ctl.dirpath(stderr_file).read()


def set_loop_conf_parameter(conf_file, section, key, value):
    """
    Выставляет значение параметра в конфиге instancectl.

    :type conf_file: unicode
    :type section: unicode
    :type key: unicode
    :type value: unicode
    """
    parser = ConfigParser.SafeConfigParser()
    parser.read(conf_file)
    parser.set(section, key, value)
    with open(conf_file, 'w') as fd:
        parser.write(fd)


def get_free_port():
    sock = socket.socket(socket.AF_INET6)
    sock.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
    sock.bind(('::', 0))
    port = sock.getsockname()[1]
    return port


def get_current_status(ctl, request):
    p = subprocess.Popen([ctl.strpath, 'status', '--max-tries', '1'], cwd=ctl.dirname)
    request.addfinalizer(lambda: terminate(p))
    p.wait()
    return p.poll()


def make_hq_args(hq_server):
    port = hq_server.wsgi.socket.getsockname()[1]
    return ['--hq-url', 'http://localhost:{}/'.format(port)]


def start_hq_mock(ctl, env, request, get_rev_resp):
    with ctl.dirpath('dump.json').open() as fd:
        dump_json = json.load(fd)

    service, conf = dump_json['configurationId'].rsplit('#')

    app = Flask('instancectl-test-fake-its')
    app.processed_requests = []
    expected_instance_id = '{}:{}@{}'.format(env["NODE_NAME"], env['BSCONFIG_IPORT'], service)

    @app.route('/rpc/instances/ReportInstanceRevStatus/', methods=['POST'])
    def main():
        try:
            req = hq_pb2.ReportInstanceRevStatusRequest()
            req.ParseFromString(flask_request.data)
            assert req.instance_id == expected_instance_id
            assert req.status.id == conf
            app.processed_requests.append(req)
            return '', 200
        except Exception:
            import traceback
            traceback.print_exc()

    @app.route('/rpc/instances/GetInstanceRev/', methods=['POST'])
    def get_instance():
        try:
            req = hq_pb2.GetInstanceRevRequest()
            req.ParseFromString(flask_request.data)
            assert req.id == expected_instance_id
            assert req.rev == conf
            resp = hq_pb2.GetInstanceRevResponse()
            resp.CopyFrom(get_rev_resp)
            resp.revision.id = conf
            return resp.SerializeToString(), 200
        except Exception:
            import traceback
            traceback.print_exc()

    web_cfg = {'web': {'http': {
        'host': 'localhost',
        'port': 0,
    }}}

    web_server = server.WebServer(web_cfg, app, version='test')
    web_thread = gevent.spawn(web_server.run)

    request.addfinalizer(web_server.stop)
    request.addfinalizer(web_thread.kill)

    return web_server


def get_spec_env():
    p = get_free_port()
    return {
        "BSCONFIG_IHOST": "fake-host.search.yandex.net",
        "BSCONFIG_INAME": "fake-host.search.yandex.net:{}".format(p),
        "BSCONFIG_IPORT": unicode(p),
        "BSCONFIG_ITAGS": "a_dc_sas a_geo_sas a_ctype_isstest a_itype_fake_type enable_hq_report use_hq_spec",
        "NANNY_SERVICE_ID": "fake_instancectl_service",
        "HOSTNAME": "localhost",
        "NODE_NAME": "fake-node.search.yandex.net"
    }


def must_start_sd_server(sd_bin, pytest_request, bind_addr, endpoints_responses=None, pods_responses=None):
    cmd = [sd_bin.strpath, bind_addr]
    if endpoints_responses:
        cmd.extend(['--endpoints-responses', json.dumps(endpoints_responses)])

    if pods_responses:
        cmd.extend(['--pods-responses', json.dumps(pods_responses)])

    process = subprocess.Popen(cmd, cwd=sd_bin.dirname)
    pytest_request.addfinalizer(lambda: terminate(process))
    gevent.sleep(1)
    return process
