import os
import pwd
import sys
import random
import logging

import pytest
import paramiko
import yatest.common

from ya.skynet.services.portoshell import proxy
proxy.register_proxies()  # noqa

from ya.skynet.services.portoshell import portotools
from ya.skynet.services.portoshell.connserver import Server
from ya.skynet.util.misc import daemonthr


def make_name():
    return 'ut-%s-%s' % (os.getpid(), random.randint(0, sys.maxint))


@pytest.fixture(scope='session')
def log():
    log = logging.getLogger('paramiko')
    log.setLevel(logging.DEBUG)

    return log


@pytest.fixture(scope='session')
def ssh_server(log):
    conn = portotools.get_portoconn(main=True)
    name = make_name()
    user = pwd.getpwuid(os.getuid()).pw_name
    c = conn.CreateWeakContainer(name)
    c.SetProperty('isolate', True)
    c.SetProperty('memory_limit', 1 << 30)
    c.SetProperty('command', '/bin/sleep 1000')
    c.SetProperty('enable_porto', False)
    c.SetProperty('user', user)
    c.Start()

    server = Server(
        log=log,
        telnet_port=0,
        ssh_port=-1,
        check_auth=False,
        iss=False,
        host_keys_dir=yatest.common.test_source_path('.'),
    )
    daemonthr(server.serve_forever)

    port = server.host_ssh_socket.getsockname()[1]
    connstring = '//slot:CONT:{name}//user:{user}'.format(
        name=name,
        user=user,
    )

    try:
        yield (
            'localhost',
            port,
            connstring,
        )
    finally:
        server.shutdown()


@pytest.fixture(scope='session')
def ssh_client(ssh_server, log):
    host, port, user = ssh_server

    pkey = paramiko.RSAKey.generate(1024)

    client = paramiko.SSHClient()
    client.set_log_channel(log.name)
    client.set_missing_host_key_policy(paramiko.AutoAddPolicy())
    client.connect(host, port,
                   username=user,
                   pkey=pkey,
                   timeout=15,
                   allow_agent=False,
                   look_for_keys=False,
                   banner_timeout=15,
                   )
    try:
        yield client
    finally:
        client.close()


def test_simple_command(ssh_client, log):
    log.info("trying without pty")
    stdin, stdout, stderr = ssh_client.exec_command('echo 123', timeout=5, get_pty=False)
    assert stdout.channel.recv_exit_status() == 0
    assert stdout.read() == '123\n'
    assert stderr.read() == ''
    stdin.close()

    log.info("trying with pty")
    stdin, stdout, stderr = ssh_client.exec_command('echo 123', timeout=5, get_pty=True)
    assert stdout.channel.recv_exit_status() == 0
    assert stdout.read() == '123\r\n'
    assert stderr.read() == ''
    stdin.close()


def test_large_stdin(ssh_client):
    chan = ssh_client.get_transport().open_session()
    chan.exec_command('cat')
    chan.sendall('\x00' * 100000)
    chan.shutdown(1)
    assert chan.recv_exit_status() == 0
    assert chan.recv(100000) == '\x00' * 100000
