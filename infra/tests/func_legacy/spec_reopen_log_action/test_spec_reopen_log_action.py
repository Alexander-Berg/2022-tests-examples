from __future__ import unicode_literals

import subprocess

import gevent
from yp_proto.yp.client.hq.proto import hq_pb2
from yp_proto.yp.client.hq.proto import types_pb2
from sepelib.subprocess import util

import utils


def test_spec_reopen_log_action(ctl, request, cwd):
    port = utils.get_free_port() - 5
    env = utils.get_spec_env()
    env["BSCONFIG_IPORT"] = unicode(port)
    env["BSCONFIG_INAME"] = '{}:{}'.format(env['BSCONFIG_IHOST'], port)

    resp = hq_pb2.GetInstanceRevResponse()
    c = resp.revision.container.add()
    c.name = 'test_reopen_log_actions'
    c.restart_policy.max_period_seconds = 1
    c.readiness_probe.initial_delay_seconds = 3
    c.readiness_probe.max_period_seconds = 3
    c.command.extend(['/bin/sleep', '1000'])
    h = c.reopen_log_action.handler
    h.type = types_pb2.Handler.EXEC
    h.exec_action.command.extend(['/bin/sh', '-c', 'echo "{BSCONFIG_IPORT}" > reopenlog_result.txt'])

    hq = utils.start_hq_mock(ctl, env, request, resp)

    utils.must_start_instancectl(ctl, request, ctl_environment=env, add_args=utils.make_hq_args(hq))
    gevent.sleep(5)
    assert len(hq.app.processed_requests) > 0
    req = hq.app.processed_requests[-1]

    assert req.status.ready.status == 'True'
    assert req.status.ready.reason == 'RevisionReady'
    assert req.status.installed.status == 'True'
    assert req.status.installed.reason == 'RevisionInstalled'

    p = subprocess.Popen([str(ctl), 'reopenlog'], cwd=ctl.dirname, env=env, stdout=subprocess.PIPE)
    rc = p.wait()
    assert rc == 0
    result = cwd.join('reopenlog_result.txt').read().strip()
    assert result == str(port)

    utils.must_stop_instancectl(ctl, check_loop_err=False)


def test_spec_reopen_log_action_http_get(ctl, request, cwd):
    port = utils.get_free_port()
    env = utils.get_spec_env()
    env["BSCONFIG_IPORT"] = unicode(port)
    env["BSCONFIG_INAME"] = '{}:{}'.format(env['BSCONFIG_IHOST'], port)

    resp = hq_pb2.GetInstanceRevResponse()
    c = resp.revision.container.add()
    c.name = 'test_reopen_log_actions_http_get'
    c.restart_policy.max_period_seconds = 1
    c.readiness_probe.initial_delay_seconds = 3
    c.readiness_probe.max_period_seconds = 3
    c.command.extend(['/bin/sleep', '1000'])
    h = c.reopen_log_action.handler
    h.type = types_pb2.Handler.HTTP_GET
    h.http_get.path = '/test'

    p = subprocess.Popen(['python', ctl.dirpath('pyserver.py').strpath, env['BSCONFIG_IPORT']],
                         cwd=cwd.strpath)
    request.addfinalizer(lambda: util.terminate(p))
    assert p.poll() is None

    hq = utils.start_hq_mock(ctl, env, request, resp)

    utils.must_start_instancectl(ctl, request, ctl_environment=env, add_args=utils.make_hq_args(hq))
    gevent.sleep(5)
    assert len(hq.app.processed_requests) > 0
    req = hq.app.processed_requests[-1]

    assert req.status.ready.status == 'True'
    assert req.status.ready.reason == 'RevisionReady'
    assert req.status.installed.status == 'True'
    assert req.status.installed.reason == 'RevisionInstalled'

    p = subprocess.Popen([str(ctl), 'reopenlog'], cwd=ctl.dirname, env=env, stdout=subprocess.PIPE)
    rc = p.wait()
    assert rc == 0
    assert cwd.join('reopened.txt').exists()

    utils.must_stop_instancectl(ctl, check_loop_err=False)
