from __future__ import unicode_literals

import json
import signal
import socket
import subprocess
from ConfigParser import SafeConfigParser

import pytest
import gevent
from flask import Flask, request as flask_request
from sepelib.flask.server import WebServer
from sepelib.subprocess.util import terminate

from utils import must_start_instancectl, must_stop_instancectl
from yp_proto.yp.client.hq.proto import hq_pb2


def get_env(hq_report_version="1"):
    return {
        "BSCONFIG_IHOST": "sas1-1956.search.yandex.net",
        "BSCONFIG_INAME": "sas1-1956.search.yandex.net:17319",
        "BSCONFIG_IPORT": "17319",
        "BSCONFIG_SHARDDIR": "rlsfacts-000-1393251816",
        "BSCONFIG_SHARDNAME": "rlsfacts-000-1393251816",
        "BSCONFIG_ITAGS": "newstyle_upload a_dc_sas a_geo_sas parallel_autofacts a_ctype_isstest enable_hq_report",
        "tags": "newstyle_upload a_dc_sas a_geo_sas parallel_autofacts a_ctype_isstest enable_hq_report",
        "annotated_ports": "{\"main\": 8080, \"extra\": 8081}",
        "NANNY_SERVICE_ID": "parallel_rlsfacts_iss_test",
        "HQ_REPORT_VERSION": hq_report_version
    }



@pytest.fixture
def hq(request, ctl):

    with ctl.dirpath('dump.json').open() as fd:
        dump_json = json.load(fd)

    instance_port = dump_json['properties']['BSCONFIG_IPORT']
    service, conf = dump_json['configurationId'].rsplit('#')

    app = Flask('instancectl-test-fake-its')
    app.processed_requests = []
    app.node_name = socket.getfqdn()

    @app.route('/rpc/instances/ReportInstanceRevStatus/', methods=['POST'])
    def ReportInstanceRevStatus():
        expected_instance_id = '{}:{}@{}'.format(app.node_name, instance_port, service)
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

    @app.route('/rpc/instances/ReportInstanceRevStatusV2/', methods=['POST'])
    def ReportInstanceRevStatusV2():
        expected_instance_id = '{}:{}@{}'.format(app.node_name, instance_port, service)
        try:
            req = hq_pb2.ReportInstanceRevStatusV2Request()
            req.ParseFromString(flask_request.data)
            assert req.instance_id == expected_instance_id
            assert req.service_id == service
            assert req.status.id == conf
            app.processed_requests.append(req)
            return '', 200
        except Exception:
            import traceback
            traceback.print_exc()


    web_cfg = {'web': {'http': {
        'host': 'localhost',
        'port': 0,
    }}}

    web_server = WebServer(web_cfg, app, version='test')
    web_thread = gevent.spawn(web_server.run)

    request.addfinalizer(web_server.stop)
    request.addfinalizer(web_thread.kill)

    conf_file = ctl.dirpath('loop.conf').strpath
    parser = SafeConfigParser()
    parser.read(conf_file)

    return web_server


def _get_port(web_server):
    return web_server.wsgi.socket.getsockname()[1]


def test_report_to_hq(ctl, patch_loop_conf, request, hq):
    port = _get_port(hq)
    must_start_instancectl(ctl, request, ctl_environment=get_env(), console_logging=True,
                           add_args=['--hq-url', 'http://localhost:{}/'.format(port)])
    gevent.sleep(10)
    assert len(hq.app.processed_requests) > 0
    processed_request = hq.app.processed_requests[-1]
    assert isinstance(processed_request, hq_pb2.ReportInstanceRevStatusRequest)
    cont_status = processed_request.status.container[0]
    assert cont_status.ready.status == 'True'
    assert cont_status.installed.status == 'True'
    must_stop_instancectl(ctl, check_loop_err=False)


def test_report_to_hq_v2(ctl, patch_loop_conf, request, hq):
    port = _get_port(hq)
    must_start_instancectl(ctl, request, ctl_environment=get_env(hq_report_version="2"), console_logging=True,
                           add_args=['--hq-url', 'http://localhost:{}/'.format(port)])
    gevent.sleep(10)
    assert len(hq.app.processed_requests) > 0
    processed_request = hq.app.processed_requests[-1]  # type: hq_pb2.ReportInstanceRevStatusV2Request
    assert isinstance(processed_request, hq_pb2.ReportInstanceRevStatusV2Request)
    cont_status = processed_request.status.container[0]
    assert cont_status.ready.status == 'True'
    assert cont_status.installed.status == 'True'
    must_stop_instancectl(ctl, check_loop_err=False)


def test_report_instance_installed_to_hq(ctl, patch_loop_conf, request, hq):
    port = _get_port(hq)
    p = subprocess.Popen([ctl.strpath, 'install', '--hq-url', 'http://localhost:{}/'.format(port)],
                         cwd=ctl.dirname, env=get_env())
    request.addfinalizer(lambda: terminate(p))
    while p.poll() is None:
        gevent.sleep(0.1)
    assert len(hq.app.processed_requests) > 0


def test_report_instance_stopped_to_hq(ctl, patch_loop_conf, request, hq):
    port = _get_port(hq)
    must_start_instancectl(ctl, request, ctl_environment=get_env(), console_logging=True,
                           add_args=['--hq-url', 'http://localhost:{}/'.format(port)])
    gevent.sleep(5)
    assert len(hq.app.processed_requests) > 0
    must_stop_instancectl(ctl, check_loop_err=False)
    assert len(hq.app.processed_requests) > 1
    for cont_status in hq.app.processed_requests[-1].status.container:
        assert cont_status.ready.status == 'False'
        assert cont_status.installed.status == 'True'
        assert cont_status.last_termination_status.exit_status.if_signaled is True
        assert cont_status.last_termination_status.exit_status.term_signal == signal.SIGTERM
        err_tail = cont_status.last_termination_status.stderr_tail.rstrip().splitlines()
        assert err_tail == ['SOME_STDERR_CONTENT'] * len(err_tail)
        out_tail = cont_status.last_termination_status.stdout_tail.rstrip().splitlines()
        assert out_tail == ['SOME_STDOUT_CONTENT'] * len(out_tail)


def test_report_instance_exited_to_hq(cwd, ctl, patch_loop_conf, request, hq):
    cwd.join('time_to_sleep.txt').write('1')
    cwd.join('daemon').write("""#!/bin/sh

echo SOME_STDOUT_CONTENT
echo 1>&2 SOME_STDERR_CONTENT

sleep $(cat time_to_sleep.txt);

exit 0""")
    port = _get_port(hq)
    must_start_instancectl(ctl, request, ctl_environment=get_env(), console_logging=True,
                           add_args=['--hq-url', 'http://localhost:{}/'.format(port)])
    gevent.sleep(10)
    assert len(hq.app.processed_requests) > 0
    for cont_status in hq.app.processed_requests[-1].status.container:
        assert cont_status.installed.status == 'True'
        assert cont_status.last_termination_status.exit_status.if_exited is True
        assert cont_status.last_termination_status.exit_status.exit_status == 0
        err_tail = cont_status.last_termination_status.stderr_tail.rstrip().splitlines()
        assert err_tail == ['SOME_STDERR_CONTENT'] * len(err_tail)
        out_tail = cont_status.last_termination_status.stdout_tail.rstrip().splitlines()
        assert out_tail == ['SOME_STDOUT_CONTENT'] * len(out_tail)
    must_stop_instancectl(ctl, check_loop_err=False)


def test_report_instance_crashed_to_hq(cwd, ctl, patch_loop_conf, request, hq):
    cwd.join('time_to_sleep.txt').write('1')
    port = _get_port(hq)
    must_start_instancectl(ctl, request, ctl_environment=get_env(), console_logging=True,
                           add_args=['--hq-url', 'http://localhost:{}/'.format(port)])
    gevent.sleep(10)
    assert len(hq.app.processed_requests) > 0
    for cont_status in hq.app.processed_requests[-1].status.container:
        assert cont_status.installed.status == 'True'
        assert cont_status.last_termination_status.exit_status.if_exited is True
        assert cont_status.last_termination_status.exit_status.exit_status == 1
        err_tail = cont_status.last_termination_status.stderr_tail.rstrip().splitlines()
        assert err_tail == ['SOME_STDERR_CONTENT'] * len(err_tail)
        out_tail = cont_status.last_termination_status.stdout_tail.rstrip().splitlines()
        assert out_tail == ['SOME_STDOUT_CONTENT'] * len(out_tail)
    must_stop_instancectl(ctl, check_loop_err=False)


def test_report_to_hq_node_name(ctl, patch_loop_conf, request, hq):
    hq.app.node_name = 'some-fake-node-name'
    env = get_env()
    env['NODE_NAME'] = hq.app.node_name
    port = _get_port(hq)
    must_start_instancectl(ctl, request, ctl_environment=env, console_logging=True,
                           add_args=['--hq-url', 'http://localhost:{}/'.format(port)])
    gevent.sleep(10)
    assert len(hq.app.processed_requests) > 0
    cont_status = hq.app.processed_requests[-1].status.container[0]
    assert cont_status.ready.status == 'True'
    assert cont_status.installed.status == 'True'
    must_stop_instancectl(ctl, check_loop_err=False)
