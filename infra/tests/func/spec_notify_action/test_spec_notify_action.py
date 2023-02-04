from __future__ import unicode_literals

import subprocess

import gevent
from yp_proto.yp.client.hq.proto import hq_pb2
from yp_proto.yp.client.hq.proto import types_pb2

import utils


def test_spec_notify_action(ctl, request, cwd):
    port = utils.get_free_port() - 5
    env = utils.get_spec_env()
    env["BSCONFIG_IPORT"] = unicode(port)
    env["BSCONFIG_INAME"] = '{}:{}'.format(env['BSCONFIG_IHOST'], port)

    resp = hq_pb2.GetInstanceRevResponse()
    c = resp.revision.container.add()
    c.name = 'test_notify_actions'
    c.restart_policy.max_period_seconds = 1
    c.readiness_probe.initial_delay_seconds = 3
    c.readiness_probe.max_period_seconds = 3
    c.command.extend(['/bin/sleep', '1000'])
    h = resp.revision.notify_action.handlers.add()
    h.type = types_pb2.Handler.EXEC
    h.exec_action.command.extend(['/bin/sh', '-c', 'echo STDOUT_CONTENT'])
    h = resp.revision.notify_action.handlers.add()
    h.type = types_pb2.Handler.EXEC
    h.exec_action.command.extend(['/bin/sh', '-c', 'echo STDERR_CONTENT >&2'])
    h = resp.revision.notify_action.handlers.add()
    h.type = types_pb2.Handler.EXEC
    h.exec_action.command.extend(['/bin/sh', '-c', 'echo "$0 $@ $# {BSCONFIG_IPORT}" > notify_result.txt'])
    h = resp.revision.notify_action.handlers.add()
    h.type = types_pb2.Handler.EXEC
    h.exec_action.command.extend(['/bin/sh', '-c', 'exit 1'])
    h = resp.revision.notify_action.handlers.add()
    h.type = types_pb2.Handler.EXEC
    h.exec_action.command.extend(['/bin/sh', '-c', 'touch MUST_NOT_EXIST'])

    hq = utils.start_hq_mock(ctl, env, request, resp)

    utils.must_start_instancectl(ctl, request, ctl_environment=env, add_args=utils.make_hq_args(hq))
    gevent.sleep(5)
    assert len(hq.app.processed_requests) > 0
    req = hq.app.processed_requests[-1]

    assert req.status.ready.status == 'True'
    assert req.status.ready.reason == 'RevisionReady'
    assert req.status.installed.status == 'True'
    assert req.status.installed.reason == 'RevisionInstalled'

    cmd = [ctl.strpath,
           '--console',
           'notify',
           '--updates',
           '+some_added_resource',
           '-some_removed_resource',
           '!some_changed_resource']
    p = subprocess.Popen(cmd, cwd=ctl.dirname, env=env, stdout=subprocess.PIPE)
    rc = p.wait()
    assert rc == 40
    result = cwd.join('notify_result.txt').read().strip()
    assert result == 'notify_action +some_added_resource -some_removed_resource !some_changed_resource 3 {}'.format(
        port)
    assert not cwd.join('MUST_NOT_EXIST').exists()
    assert cwd.join('notify.out').read().split()[0].strip() == 'STDOUT_CONTENT'
    assert cwd.join('notify.err').read().split()[0].strip() == 'STDERR_CONTENT'

    cmd = [ctl.strpath,
           '--console',
           'notify',
           '--updates',
           '+some_added_resource',
           '-some_other_resource']
    p = subprocess.Popen(cmd, cwd=ctl.dirname, env=env, stdout=subprocess.PIPE)
    rc = p.wait()
    assert rc == 40
    result = cwd.join('notify_result.txt').read().strip()
    assert result == 'notify_action +some_added_resource -some_other_resource 2 {}'.format(port)
    assert not cwd.join('MUST_NOT_EXIST').exists()
    utils.must_stop_instancectl(ctl, check_loop_err=False)
