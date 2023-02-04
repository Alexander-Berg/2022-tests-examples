from __future__ import unicode_literals

import gevent
from yp_proto.yp.client.hq.proto import hq_pb2
from yp_proto.yp.client.hq.proto import types_pb2

import utils


def test_spec_term_barrier(ctl, request):
    env = utils.get_spec_env()
    resp = hq_pb2.GetInstanceRevResponse()
    c = resp.revision.container.add()
    c.name = 'test_spec_term_barrier1'
    c.restart_policy.max_period_seconds = 1
    c.readiness_probe.initial_delay_seconds = 3
    c.readiness_probe.max_period_seconds = 3
    c.command.extend(['python', 'pyscript.py', 'term1.txt'])
    c.lifecycle.pre_stop.type = types_pb2.Handler.EXEC
    c.lifecycle.pre_stop.exec_action.command.extend(['python', 'pre_stop.py'])
    c.lifecycle.stop_grace_period_seconds = 5

    c = resp.revision.container.add()
    c.name = 'test_spec_term_barrier2'
    c.restart_policy.max_period_seconds = 1
    c.readiness_probe.initial_delay_seconds = 3
    c.readiness_probe.max_period_seconds = 3
    c.command.extend(['python', 'pyscript.py', 'term2.txt'])
    c.lifecycle.pre_stop.type = types_pb2.Handler.EXEC
    c.lifecycle.pre_stop.exec_action.command.extend(['/bin/sh', '-c', 'echo OK > script2.txt'])
    c.lifecycle.term_barrier = types_pb2.Lifecycle.WAIT

    c = resp.revision.container.add()
    c.name = 'test_spec_term_barrier3'
    c.restart_policy.max_period_seconds = 1
    c.readiness_probe.initial_delay_seconds = 3
    c.readiness_probe.max_period_seconds = 3
    c.command.extend(['python', 'pyscript.py', 'term3.txt'])
    c.lifecycle.pre_stop.type = types_pb2.Handler.EXEC
    c.lifecycle.pre_stop.exec_action.command.extend(['/bin/sh', '-c', 'echo OK > script3.txt'])

    hq = utils.start_hq_mock(ctl, env, request, resp)
    utils.must_start_instancectl(ctl, request, ctl_environment=env, add_args=utils.make_hq_args(hq))

    gevent.sleep(10)

    assert len(hq.app.processed_requests) > 0
    req = hq.app.processed_requests[-1]
    assert req.status.ready.status == 'True'
    assert req.status.ready.reason == 'RevisionReady'
    assert req.status.installed.status == 'True'
    assert req.status.installed.reason == 'RevisionInstalled'
    assert len(req.status.container) == 3
    for c in req.status.container:
        assert c.name in ('test_spec_term_barrier1', 'test_spec_term_barrier2', 'test_spec_term_barrier3')
        assert c.ready.status == 'True'
        assert c.ready.reason == 'ContainerReady'
        assert c.installed.status == 'True'
        assert c.installed.reason == 'ContainerInstalled'

    ctl.dirpath('run.flag').remove()
    gevent.sleep(10)
    assert ctl.dirpath('script2.txt').read().strip() == 'OK'
    assert ctl.dirpath('script3.txt').read().strip() == 'OK'
    assert not ctl.dirpath('term1.txt').exists()
    assert not ctl.dirpath('term2.txt').exists()
    assert ctl.dirpath('term3.txt').exists()
    ctl.dirpath('stop.txt').ensure()
    lock_file = ctl.dirpath('state', 'loop.lock').strpath
    assert utils.wait_file_is_not_locked(lock_file, timeout=10), 'Control failed to stop!'

    pre_stop_exited = float(ctl.dirpath('stop_result.txt').read().strip())
    terminated1 = float(ctl.dirpath('term1.txt').read().strip())
    terminated2 = float(ctl.dirpath('term2.txt').read().strip())
    assert terminated1 - pre_stop_exited > 4
    assert terminated2 - pre_stop_exited > 4
