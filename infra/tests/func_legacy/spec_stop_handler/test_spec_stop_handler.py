from __future__ import unicode_literals

import gevent
from flask import Flask
from flask import request as flask_request
from yp_proto.yp.client.hq.proto import hq_pb2
from yp_proto.yp.client.hq.proto import types_pb2
from sepelib.flask import server

import utils


def test_spec_stop_handler(ctl, request):
    env = utils.get_spec_env()
    resp = hq_pb2.GetInstanceRevResponse()
    c = resp.revision.container.add()
    c.name = 'test_spec_stop_handler'
    c.restart_policy.max_period_seconds = 1
    c.readiness_probe.initial_delay_seconds = 3
    c.readiness_probe.max_period_seconds = 3
    c.command.extend(['/bin/sleep', '100'])
    c.lifecycle.pre_stop.type = types_pb2.Handler.EXEC
    c.lifecycle.pre_stop.exec_action.command.extend(['/bin/sh', '-c', 'echo "SCRIPT_OK" > script_result.txt'])

    c = resp.revision.container.add()
    c.name = 'test_spec_stop_handler2'
    c.restart_policy.max_period_seconds = 1
    c.readiness_probe.initial_delay_seconds = 3
    c.readiness_probe.max_period_seconds = 3
    c.command.extend(['/bin/sleep', '100'])
    c.lifecycle.pre_stop.type = types_pb2.Handler.HTTP_GET
    c.lifecycle.pre_stop.http_get.path = '/test_path'
    h = c.lifecycle.pre_stop.http_get.http_headers.add()
    h.name = 'x-my-header'
    h.value = 'some-value'

    app = Flask('instancectl-test-fake-its')
    response_code = 200
    request_headers = []

    @app.route('/test_path', methods=['GET'])
    def main():
        try:
            request_headers.append(flask_request.headers)
            return '', response_code
        except Exception:
            import traceback
            traceback.print_exc()

    web_cfg = {'web': {'http': {
        'host': 'localhost',
        'port': int(env['BSCONFIG_IPORT']),
    }}}

    web_server = server.WebServer(web_cfg, app, version='test')
    web_thread = gevent.spawn(web_server.run)

    request.addfinalizer(web_server.stop)
    request.addfinalizer(web_thread.kill)

    hq = utils.start_hq_mock(ctl, env, request, resp)
    utils.must_start_instancectl(ctl, request, ctl_environment=env, add_args=utils.make_hq_args(hq))

    gevent.sleep(5)

    assert len(hq.app.processed_requests) > 0
    req = hq.app.processed_requests[-1]
    assert req.status.ready.status == 'True'
    assert req.status.ready.reason == 'RevisionReady'
    assert req.status.installed.status == 'True'
    assert req.status.installed.reason == 'RevisionInstalled'
    assert len(req.status.container) == 2
    for c in req.status.container:
        assert c.name in ('test_spec_stop_handler', 'test_spec_stop_handler2')
        assert c.ready.status == 'True'
        assert c.ready.reason == 'ContainerReady'
        assert c.installed.status == 'True'
        assert c.installed.reason == 'ContainerInstalled'

    utils.must_stop_instancectl(ctl, check_loop_err=False)
    hq.stop()

    assert ctl.dirpath('script_result.txt').read().strip() == 'SCRIPT_OK'
    assert len(request_headers) == 1
    assert request_headers[0]['x-my-header'] == 'some-value'

    c = resp.revision.container[0]
    c.lifecycle.pre_stop.exec_action.command[:] = ['/bin/sh', '-c', 'echo "SCRIPT_OK" > script_result2.txt; exit 1']

    p = utils.get_free_port()
    env['BSCONFIG_IPORT'] = unicode(p)
    env['BSCONFIG_INAME'] = '{}:{}'.format(env['BSCONFIG_IHOST'], p)
    response_code = 500

    hq = utils.start_hq_mock(ctl, env, request, resp)
    utils.must_start_instancectl(ctl, request, ctl_environment=env, add_args=utils.make_hq_args(hq))

    gevent.sleep(5)

    assert len(hq.app.processed_requests) > 0
    req = hq.app.processed_requests[-1]
    assert req.status.ready.status == 'True'
    assert req.status.ready.reason == 'RevisionReady'
    assert req.status.installed.status == 'True'
    assert req.status.installed.reason == 'RevisionInstalled'
    assert len(req.status.container) == 2
    for c in req.status.container:
        assert c.name in ('test_spec_stop_handler', 'test_spec_stop_handler2')
        assert c.ready.status == 'True'
        assert c.ready.reason == 'ContainerReady'
        assert c.installed.status == 'True'
        assert c.installed.reason == 'ContainerInstalled'

    utils.must_stop_instancectl(ctl, check_loop_err=False)
    hq.stop()
    assert ctl.dirpath('script_result2.txt').read().strip() == 'SCRIPT_OK'
    assert len(request_headers) == 1
    assert request_headers[0]['x-my-header'] == 'some-value'
