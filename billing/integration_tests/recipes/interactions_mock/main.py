import logging
import os
import signal
from time import sleep

import requests
import yatest.common
from library.python.testing.recipe import declare_recipe
from yatest.common import network

logger = logging.getLogger('yandexpay-interaction-mock-recipe')

PID_FILE = 'yandexpay-interaction-mock.pid'
PORT_FILE = 'yandexpay-interaction-mock.port'


def ping():
    with open(PORT_FILE) as f:
        port = int(f.read().strip())

    try:
        response = requests.get(f'http://localhost:{port}/ping')
    except Exception:
        logger.exception('Unable to ping interaction mock server.')
        return False

    if response.status_code >= 400:
        logger.error(
            'Unable to ping interaction mock server:\nStatus: %(status)s\nBody: %(body)s',
            dict(status=response.status_code, body=response.content),
        )
        return False

    return True


def start(argv):
    logger.info('Start.')

    mocks_bin = yatest.common.build_path(
        'billing/yandex_pay/tools/load/interactions_mock/bin/interactions_mock'
    )

    pm = network.PortManager()
    port = pm.get_tcp_port()
    with open(PORT_FILE, 'w') as f:
        f.write(str(port))

    cmd = [mocks_bin, '--host', 'localhost', '--port', str(port)]
    execution = yatest.common.execute(cmd, wait=False)

    with open(PID_FILE, 'w') as f:
        f.write(str(execution.process.pid))

    for _ in range(10):
        if ping():
            return
        sleep(1)

    raise RuntimeError('Unable to start interaction mock server.')


def stop(argv):
    logger.info('Stop.')

    try:
        with open(PID_FILE) as f:
            pid = int(f.read().strip())

        os.kill(pid, signal.SIGKILL)
        logger.info('Sent termination signal to interaction mock server.')
    except (FileNotFoundError, ProcessLookupError):
        logger.error('Unable to terminate interaction mock server. Is it running?')


def main():
    declare_recipe(start, stop)
