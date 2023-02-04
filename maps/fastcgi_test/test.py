import contextlib
import logging
import multiprocessing
import os
import requests
import socket
import tempfile
import time
import yatest

from yatest.common import network

logger = logging.getLogger("test_logger")


@contextlib.contextmanager
def run_testapp(mode, output_stream):
    environment = os.environ.copy()
    environment['LD_LIBRARY_PATH'] = yatest.common.binary_path('maps/infra/yacare')
    environment['YCR_MODE'] = mode

    try:
        process = yatest.common.execute(
            [yatest.common.binary_path('maps/infra/yacare/testapp/bin/yacare-testapp')],
            stdin=None,
            stdout=output_stream,
            stderr=output_stream,
            wait=False,
            env=environment)
        yield process
    finally:
        process.kill()


def run_test_tool(testcase_path):
    yatest.common.execute([
        yatest.common.binary_path('maps/infra/yacare/scripts/yacare_test_tool/yacare_test_tool'),
        '-a', yatest.common.binary_path('maps/infra/yacare/testapp/bin/yacare-testapp'),
        '-p', testcase_path,
        '-v'
    ])


def test_fastcgi():
    socket_dir = tempfile.mkdtemp()
    logger.info(socket_dir)
    socket_file = os.path.join(socket_dir, 'testapp.sock')
    log_file = open(yatest.common.output_path('testapp.log'), 'w+')
    with run_testapp(f'fastcgi:{socket_file}', log_file) as testapp_process:
        time.sleep(2)
        logger.info(f'Testapp is running with pid {testapp_process.process.pid}')

        try:
            sock = socket.socket(socket.AF_UNIX, socket.SOCK_STREAM)
            sock.connect(socket_file)
        except socket.error as msg:
            logger.error(msg)
            raise
        finally:
            sock.close()

        pids = []
        for i in range(multiprocessing.cpu_count()):
            pids.append(yatest.common.execute(
                [yatest.common.binary_path('maps/infra/yacare/ut/fastcgi_test/bin/fastcgi_test_bin')],
                cwd=socket_dir,
                wait=False))

        for pid in pids:
            pid.wait()

        log_file.seek(0)
        log_lines = log_file.readlines()
        assert len(log_lines) > 1000
        if any("protocol violation" in l for l in log_lines):
            raise RuntimeError("fcgi-test: race in fastcgi frontend detected")


def test_queries():
    run_test_tool(yatest.common.test_source_path('test.queries'))


def test_http_mode():
    with network.PortManager() as pm:
        port = pm.get_port()
        with run_testapp(f'http:{port}', None):
            time.sleep(2)

            def run_test(method, request, expected):
                response = requests.request(method, f'http://localhost:{port}/{request}')
                if response.status_code != expected:
                    raise RuntimeError('Failed request: ' + request)

            run_test('GET', "mtroute/ping", 200)
            run_test('GET', "mtroute/update", 405)
            run_test('POST', "mtroute/update", 200)
