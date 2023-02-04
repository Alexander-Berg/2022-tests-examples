# coding: utf-8

from ConfigParser import SafeConfigParser
import time

from flask import Flask, request as flask_request
from sepelib.flask.server import WebServer


PARSED_TRACES = (
    'Program terminated with signal SIGSEGV\n\n'
    'Thread 1 (Thread 0x0)\n'
    '#0  0x0 in pthread_join () from /build/buildd/eglibc-2.15/nptl/pthread_join.c:89\n\n'
)


def create_web_server(received_traces):

    app = Flask('instancectl-test-fake-aggregator')

    @app.route('/submit/', methods=['POST'])
    def main():
        received_traces.append({
            'params': flask_request.args,
            'traces': flask_request.data,
        })
        return 'OK'

    web_cfg = {'web': {'http': {
        'host': 'localhost',
        'port': 0,
    }}}

    return WebServer(web_cfg, app, version='test')


def assert_directory_contains_files(expected, directory, timeout=120.0):
    # Renaming files may be very slow on sandbox environment disks,
    # so we have to wait very long
    expected = set(expected)
    deadline = time.time() + timeout
    while time.time() < deadline:
        time.sleep(0.5)
        if expected == {i.basename for i in directory.listdir()}:
            return
    assert expected == {i.basename for i in directory.listdir()}


def update_loop_conf(
    conf_file, port, timeout_before_sending, check_timeout,
    core_pattern=None,
    rename_binary=None,
):
    parser = SafeConfigParser()
    parser.read(conf_file)
    parser.set('test_coredumps_sending', 'minidumps_aggregator_url', 'http://localhost:{}/submit/'.format(port))
    parser.set('test_coredumps_sending', 'minidumps_timeout_before_sending', str(timeout_before_sending))
    parser.set('test_coredumps_sending', 'minidumps_check_timeout', str(check_timeout))
    parser.set('test_coredumps_sending', 'minidumps_clean_timeout', str(timeout_before_sending))
    if core_pattern:
        parser.set('test_coredumps_sending', 'core_pattern', core_pattern)

    if rename_binary:
        parser.set('test_coredumps_sending', 'rename_binary', rename_binary)

    with open(conf_file, 'w') as fd:
        parser.write(fd)
