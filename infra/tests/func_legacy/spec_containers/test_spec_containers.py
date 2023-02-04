from __future__ import print_function
from __future__ import unicode_literals

import subprocess

import gevent
from sepelib.subprocess import util
from yp_proto.yp.client.hq.proto import hq_pb2
from yp_proto.yp.client.hq.proto import types_pb2
from conftest import porto_required

import utils


def test_start_containers_from_spec(ctl, request):
    env = utils.get_spec_env()

    resp = hq_pb2.GetInstanceRevResponse()
    # Container 1
    c = resp.revision.container.add()
    c.name = 'test_spec_containers_good'
    c.restart_policy.max_period_seconds = 1
    c.command.extend(['/bin/sh', 'good_binary'])
    c.readiness_probe.initial_delay_seconds = 3
    c.readiness_probe.max_period_seconds = 3
    h = c.readiness_probe.handlers.add()
    h.type = types_pb2.Handler.EXEC
    h.exec_action.command.extend(['/bin/sh', '-c', 'exit $(cat script_result.txt)'])
    h = c.readiness_probe.handlers.add()
    h.type = types_pb2.Handler.TCP_SOCKET
    # Container 2
    c = resp.revision.container.add()
    c.name = 'test_spec_containers_bad'
    c.restart_policy.max_period_seconds = 1
    c.command.extend(['/bin/sh', 'bad_binary'])
    c.readiness_probe.initial_delay_seconds = 3
    c.readiness_probe.max_period_seconds = 3
    port = unicode(utils.get_free_port())
    h = c.readiness_probe.handlers.add()
    h.type = types_pb2.Handler.HTTP_GET
    h.http_get.port = port

    hq = utils.start_hq_mock(ctl, env, request, resp)

    p = subprocess.Popen(['python', ctl.dirpath('pyserver.py').strpath, env['BSCONFIG_IPORT']])
    request.addfinalizer(lambda: util.terminate(p))
    assert p.poll() is None
    http_serv = subprocess.Popen(['python', ctl.dirpath('pyserver.py').strpath, port])
    request.addfinalizer(lambda: util.terminate(http_serv))
    assert http_serv.poll() is None
    ctl.dirpath('script_result.txt').write('0')

    # Start binaries both OK
    ctl.dirpath('sleep_bad.txt').write('20')
    utils.must_start_instancectl(ctl, request, ctl_environment=env, add_args=utils.make_hq_args(hq),
                                 console_logging=True)
    gevent.sleep(10)
    assert len(hq.app.processed_requests) > 0
    req = hq.app.processed_requests[-1]
    assert req.status.ready.status == 'True'
    assert req.status.ready.reason == 'RevisionReady'
    assert req.status.installed.status == 'True'
    assert req.status.installed.reason == 'RevisionInstalled'
    assert len(req.status.container) == 2
    for c in req.status.container:
        assert c.name in ('test_spec_containers_good', 'test_spec_containers_bad')
        if c.name == 'test_spec_containers_good':
            assert c.ready.status == 'True'
            assert c.ready.reason == 'CheckPortOk'
            assert c.installed.status == 'True'
            assert c.installed.reason == 'ContainerInstalled'
            assert c.restart_count == 1
        else:
            assert c.ready.status == 'True'
            assert c.ready.reason == 'HttpGetOk'
            assert c.installed.status == 'True'
            assert c.installed.reason == 'ContainerInstalled'
            assert c.restart_count == 1

    # FAIL binary
    ctl.dirpath('sleep_bad.txt').write('1')
    gevent.sleep(10)
    req = hq.app.processed_requests[-1]
    assert req.status.ready.status == 'False'
    assert req.status.ready.reason == 'RevisionNotReady'
    assert req.status.installed.status == 'True'
    assert req.status.installed.reason == 'RevisionInstalled'
    assert len(req.status.container) == 2
    for c in req.status.container:
        assert c.name in ('test_spec_containers_good', 'test_spec_containers_bad')
        if c.name == 'test_spec_containers_good':
            assert c.ready.status == 'True'
            assert c.ready.reason == 'CheckPortOk'
            assert c.installed.status == 'True'
            assert c.installed.reason == 'ContainerInstalled'
            assert c.restart_count == 1
        else:
            assert c.ready.status == 'False'
            assert c.ready.reason == 'ContainerNotReady'
            assert c.installed.status == 'True'
            assert c.installed.reason == 'ContainerInstalled'
            assert c.restart_count > 0
            assert c.last_termination_status.stdout_tail.split()[-1].strip() == 'STDOUT_CONTENT'

    # Make binary OK again
    ctl.dirpath('sleep_bad.txt').write('100')
    gevent.sleep(10)
    req = hq.app.processed_requests[-1]
    assert req.status.ready.status == 'True'
    assert req.status.ready.reason == 'RevisionReady'
    assert req.status.installed.status == 'True'
    assert req.status.installed.reason == 'RevisionInstalled'
    assert len(req.status.container) == 2
    for c in req.status.container:
        assert c.name in ('test_spec_containers_good', 'test_spec_containers_bad')
        if c.name == 'test_spec_containers_good':
            assert c.ready.status == 'True'
            assert c.ready.reason == 'CheckPortOk'
            assert c.installed.status == 'True'
            assert c.installed.reason == 'ContainerInstalled'
            assert c.restart_count == 1
        else:
            assert c.ready.status == 'True'
            assert c.ready.reason == 'HttpGetOk'
            assert c.installed.status == 'True'
            assert c.installed.reason == 'ContainerInstalled'
            assert c.restart_count > 1
            assert c.last_termination_status.stdout_tail.split()[-1].strip() == 'STDOUT_CONTENT'

    # Fail TCP check
    util.terminate(p)
    gevent.sleep(5)
    req = hq.app.processed_requests[-1]
    assert req.status.ready.status == 'False'
    assert req.status.ready.reason == 'RevisionNotReady'
    assert req.status.installed.status == 'True'
    assert req.status.installed.reason == 'RevisionInstalled'
    assert len(req.status.container) == 2
    for c in req.status.container:
        assert c.name in ('test_spec_containers_good', 'test_spec_containers_bad')
        if c.name == 'test_spec_containers_good':
            assert c.ready.status == 'False'
            assert c.ready.reason == 'CheckPortFail'
            assert c.installed.status == 'True'
            assert c.installed.reason == 'ContainerInstalled'
            assert c.restart_count == 1
        else:
            assert c.ready.status == 'True'
            assert c.ready.reason == 'HttpGetOk'
            assert c.installed.status == 'True'
            assert c.installed.reason == 'ContainerInstalled'
            assert c.restart_count > 1
            assert c.last_termination_status.stdout_tail.split()[-1].strip() == 'STDOUT_CONTENT'

    # Make TCP check OK again
    p = subprocess.Popen(['python', ctl.dirpath('pyserver.py').strpath, env['BSCONFIG_IPORT']])
    request.addfinalizer(lambda: util.terminate(p))
    assert p.poll() is None
    gevent.sleep(5)
    req = hq.app.processed_requests[-1]
    assert req.status.ready.status == 'True'
    assert req.status.ready.reason == 'RevisionReady'
    assert req.status.installed.status == 'True'
    assert req.status.installed.reason == 'RevisionInstalled'
    assert len(req.status.container) == 2
    for c in req.status.container:
        assert c.name in ('test_spec_containers_good', 'test_spec_containers_bad')
        if c.name == 'test_spec_containers_good':
            assert c.ready.status == 'True'
            assert c.ready.reason == 'CheckPortOk'
            assert c.installed.status == 'True'
            assert c.installed.reason == 'ContainerInstalled'
            assert c.restart_count == 1
        else:
            assert c.ready.status == 'True'
            assert c.ready.reason == 'HttpGetOk'
            assert c.installed.status == 'True'
            assert c.installed.reason == 'ContainerInstalled'
            assert c.restart_count > 1
            assert c.last_termination_status.stdout_tail.split()[-1].strip() == 'STDOUT_CONTENT'

    # Fail exec prober
    ctl.dirpath('script_result.txt').write('1')
    gevent.sleep(5)
    req = hq.app.processed_requests[-1]
    assert req.status.ready.status == 'False'
    assert req.status.ready.reason == 'RevisionNotReady'
    assert req.status.installed.status == 'True'
    assert req.status.installed.reason == 'RevisionInstalled'
    assert len(req.status.container) == 2
    for c in req.status.container:
        assert c.name in ('test_spec_containers_good', 'test_spec_containers_bad')
        if c.name == 'test_spec_containers_good':
            assert c.ready.status == 'False'
            assert c.ready.reason == 'StatusScriptFail'
            assert c.installed.status == 'True'
            assert c.installed.reason == 'ContainerInstalled'
            assert c.restart_count == 1
        else:
            assert c.ready.status == 'True'
            assert c.ready.reason == 'HttpGetOk'
            assert c.installed.status == 'True'
            assert c.installed.reason == 'ContainerInstalled'
            assert c.restart_count > 1
            assert c.last_termination_status.stdout_tail.split()[-1].strip() == 'STDOUT_CONTENT'

    # Make exec prober OK again
    ctl.dirpath('script_result.txt').write('0')
    gevent.sleep(5)
    req = hq.app.processed_requests[-1]
    assert req.status.ready.status == 'True'
    assert req.status.ready.reason == 'RevisionReady'
    assert req.status.installed.status == 'True'
    assert req.status.installed.reason == 'RevisionInstalled'
    assert len(req.status.container) == 2
    for c in req.status.container:
        assert c.name in ('test_spec_containers_good', 'test_spec_containers_bad')
        if c.name == 'test_spec_containers_good':
            assert c.ready.status == 'True'
            assert c.ready.reason == 'CheckPortOk'
            assert c.installed.status == 'True'
            assert c.installed.reason == 'ContainerInstalled'
            assert c.restart_count == 1
        else:
            assert c.ready.status == 'True'
            assert c.ready.reason == 'HttpGetOk'
            assert c.installed.status == 'True'
            assert c.installed.reason == 'ContainerInstalled'
            assert c.restart_count > 1
            assert c.last_termination_status.stdout_tail.split()[-1].strip() == 'STDOUT_CONTENT'

    # Fail HTTP GET check
    util.terminate(http_serv)
    gevent.sleep(5)
    req = hq.app.processed_requests[-1]
    assert req.status.ready.status == 'False'
    assert req.status.ready.reason == 'RevisionNotReady'
    assert req.status.installed.status == 'True'
    assert req.status.installed.reason == 'RevisionInstalled'
    assert len(req.status.container) == 2
    for c in req.status.container:
        assert c.name in ('test_spec_containers_good', 'test_spec_containers_bad')
        if c.name == 'test_spec_containers_good':
            assert c.ready.status == 'True'
            assert c.ready.reason == 'CheckPortOk'
            assert c.installed.status == 'True'
            assert c.installed.reason == 'ContainerInstalled'
            assert c.restart_count == 1
        else:
            assert c.ready.status == 'False'
            assert c.ready.reason == 'HttpGetFail'
            assert c.installed.status == 'True'
            assert c.installed.reason == 'ContainerInstalled'
            assert c.restart_count > 1
            assert c.last_termination_status.stdout_tail.split()[-1].strip() == 'STDOUT_CONTENT'

    # Make HTTP GET check OK again
    http_serv = subprocess.Popen(['python', ctl.dirpath('pyserver.py').strpath, port])
    request.addfinalizer(lambda: util.terminate(http_serv))
    assert http_serv.poll() is None
    gevent.sleep(5)
    req = hq.app.processed_requests[-1]
    assert req.status.ready.status == 'True'
    assert req.status.ready.reason == 'RevisionReady'
    assert req.status.installed.status == 'True'
    assert req.status.installed.reason == 'RevisionInstalled'
    assert len(req.status.container) == 2
    for c in req.status.container:
        assert c.name in ('test_spec_containers_good', 'test_spec_containers_bad')
        if c.name == 'test_spec_containers_good':
            assert c.ready.status == 'True'
            assert c.ready.reason == 'CheckPortOk'
            assert c.installed.status == 'True'
            assert c.installed.reason == 'ContainerInstalled'
            assert c.restart_count == 1
        else:
            assert c.ready.status == 'True'
            assert c.ready.reason == 'HttpGetOk'
            assert c.installed.status == 'True'
            assert c.installed.reason == 'ContainerInstalled'
            assert c.restart_count > 1
            assert c.last_termination_status.stdout_tail.split()[-1].strip() == 'STDOUT_CONTENT'

    utils.must_stop_instancectl(ctl, check_loop_err=False)


@porto_required
def test_start_app_container(ctl, request):
    env = utils.get_spec_env()

    resp = hq_pb2.GetInstanceRevResponse()
    resp.revision.type = types_pb2.InstanceRevision.APP_CONTAINER
    # Container 1
    c = resp.revision.container.add()
    c.name = 'test_spec_containers_good'
    c.restart_policy.max_period_seconds = 1
    c.command.extend(['/bin/sh', 'good_binary'])
    c.readiness_probe.initial_delay_seconds = 3
    c.readiness_probe.max_period_seconds = 3
    h = c.readiness_probe.handlers.add()
    h.type = types_pb2.Handler.EXEC
    h.exec_action.command.extend(['/bin/sh', '-c', 'exit $(cat script_result.txt)'])
    h = c.readiness_probe.handlers.add()
    h.type = types_pb2.Handler.TCP_SOCKET
    # Container 2
    c = resp.revision.container.add()
    c.name = 'test_spec_containers_bad'
    c.restart_policy.max_period_seconds = 1
    c.command.extend(['/bin/sh', 'bad_binary'])
    c.readiness_probe.initial_delay_seconds = 3
    c.readiness_probe.max_period_seconds = 3
    port = unicode(utils.get_free_port())
    h = c.readiness_probe.handlers.add()
    h.type = types_pb2.Handler.HTTP_GET
    h.http_get.port = port

    hq = utils.start_hq_mock(ctl, env, request, resp)

    p = subprocess.Popen(['python', ctl.dirpath('pyserver.py').strpath, env['BSCONFIG_IPORT']])
    request.addfinalizer(lambda: util.terminate(p))
    assert p.poll() is None
    http_serv = subprocess.Popen(['python', ctl.dirpath('pyserver.py').strpath, port])
    request.addfinalizer(lambda: util.terminate(http_serv))
    assert http_serv.poll() is None
    ctl.dirpath('script_result.txt').write('0')

    # Start binaries both OK
    ctl.dirpath('sleep_bad.txt').write('20')
    utils.must_start_instancectl(ctl, request, ctl_environment=env, add_args=utils.make_hq_args(hq))
    gevent.sleep(10)
    assert len(hq.app.processed_requests) > 0
    req = hq.app.processed_requests[-1]
    assert req.status.ready.status == 'True'
    assert req.status.ready.reason == 'RevisionReady'
    assert req.status.installed.status == 'True'
    assert req.status.installed.reason == 'RevisionInstalled'
    assert len(req.status.container) == 2
    for c in req.status.container:
        assert c.name in ('test_spec_containers_good', 'test_spec_containers_bad')
        if c.name == 'test_spec_containers_good':
            assert c.ready.status == 'True'
            assert c.ready.reason == 'CheckPortOk'
            assert c.installed.status == 'True'
            assert c.installed.reason == 'ContainerInstalled'
            assert c.restart_count == 1
        else:
            assert c.ready.status == 'True'
            assert c.ready.reason == 'HttpGetOk'
            assert c.installed.status == 'True'
            assert c.installed.reason == 'ContainerInstalled'
            assert c.restart_count == 1

    # FAIL binary
    ctl.dirpath('sleep_bad.txt').write('1')
    gevent.sleep(10)
    req = hq.app.processed_requests[-1]
    assert req.status.ready.status == 'False'
    assert req.status.ready.reason == 'RevisionNotReady'
    assert req.status.installed.status == 'True'
    assert req.status.installed.reason == 'RevisionInstalled'
    assert len(req.status.container) == 2
    for c in req.status.container:
        assert c.name in ('test_spec_containers_good', 'test_spec_containers_bad')
        if c.name == 'test_spec_containers_good':
            assert c.ready.status == 'True'
            assert c.ready.reason == 'CheckPortOk'
            assert c.installed.status == 'True'
            assert c.installed.reason == 'ContainerInstalled'
            assert c.restart_count == 1
        else:
            assert c.ready.status == 'False'
            assert c.ready.reason == 'ContainerNotReady'
            assert c.installed.status == 'True'
            assert c.installed.reason == 'ContainerInstalled'
            assert c.restart_count > 0
            assert c.last_termination_status.stdout_tail.split()[-1].strip() == 'STDOUT_CONTENT'

    # Make binary OK again
    ctl.dirpath('sleep_bad.txt').write('100')
    gevent.sleep(10)
    req = hq.app.processed_requests[-1]
    assert req.status.ready.status == 'True'
    assert req.status.ready.reason == 'RevisionReady'
    assert req.status.installed.status == 'True'
    assert req.status.installed.reason == 'RevisionInstalled'
    assert len(req.status.container) == 2
    for c in req.status.container:
        assert c.name in ('test_spec_containers_good', 'test_spec_containers_bad')
        if c.name == 'test_spec_containers_good':
            assert c.ready.status == 'True'
            assert c.ready.reason == 'CheckPortOk'
            assert c.installed.status == 'True'
            assert c.installed.reason == 'ContainerInstalled'
            assert c.restart_count == 1
        else:
            assert c.ready.status == 'True'
            assert c.ready.reason == 'HttpGetOk'
            assert c.installed.status == 'True'
            assert c.installed.reason == 'ContainerInstalled'
            assert c.restart_count > 1
            assert c.last_termination_status.stdout_tail.split()[-1].strip() == 'STDOUT_CONTENT'

    # Fail TCP check
    util.terminate(p)
    gevent.sleep(5)
    req = hq.app.processed_requests[-1]
    assert req.status.ready.status == 'False'
    assert req.status.ready.reason == 'RevisionNotReady'
    assert req.status.installed.status == 'True'
    assert req.status.installed.reason == 'RevisionInstalled'
    assert len(req.status.container) == 2
    for c in req.status.container:
        assert c.name in ('test_spec_containers_good', 'test_spec_containers_bad')
        if c.name == 'test_spec_containers_good':
            assert c.ready.status == 'False'
            assert c.ready.reason == 'CheckPortFail'
            assert c.installed.status == 'True'
            assert c.installed.reason == 'ContainerInstalled'
            assert c.restart_count == 1
        else:
            assert c.ready.status == 'True'
            assert c.ready.reason == 'HttpGetOk'
            assert c.installed.status == 'True'
            assert c.installed.reason == 'ContainerInstalled'
            assert c.restart_count > 1
            assert c.last_termination_status.stdout_tail.split()[-1].strip() == 'STDOUT_CONTENT'

    # Make TCP check OK again
    p = subprocess.Popen(['python', ctl.dirpath('pyserver.py').strpath, env['BSCONFIG_IPORT']])
    request.addfinalizer(lambda: util.terminate(p))
    assert p.poll() is None
    gevent.sleep(5)
    req = hq.app.processed_requests[-1]
    assert req.status.ready.status == 'True'
    assert req.status.ready.reason == 'RevisionReady'
    assert req.status.installed.status == 'True'
    assert req.status.installed.reason == 'RevisionInstalled'
    assert len(req.status.container) == 2
    for c in req.status.container:
        assert c.name in ('test_spec_containers_good', 'test_spec_containers_bad')
        if c.name == 'test_spec_containers_good':
            assert c.ready.status == 'True'
            assert c.ready.reason == 'CheckPortOk'
            assert c.installed.status == 'True'
            assert c.installed.reason == 'ContainerInstalled'
            assert c.restart_count == 1
        else:
            assert c.ready.status == 'True'
            assert c.ready.reason == 'HttpGetOk'
            assert c.installed.status == 'True'
            assert c.installed.reason == 'ContainerInstalled'
            assert c.restart_count > 1
            assert c.last_termination_status.stdout_tail.split()[-1].strip() == 'STDOUT_CONTENT'

    # Fail exec prober
    ctl.dirpath('script_result.txt').write('1')
    gevent.sleep(5)
    req = hq.app.processed_requests[-1]
    assert req.status.ready.status == 'False'
    assert req.status.ready.reason == 'RevisionNotReady'
    assert req.status.installed.status == 'True'
    assert req.status.installed.reason == 'RevisionInstalled'
    assert len(req.status.container) == 2
    for c in req.status.container:
        assert c.name in ('test_spec_containers_good', 'test_spec_containers_bad')
        if c.name == 'test_spec_containers_good':
            assert c.ready.status == 'False'
            assert c.ready.reason == 'StatusScriptFail'
            assert c.installed.status == 'True'
            assert c.installed.reason == 'ContainerInstalled'
            assert c.restart_count == 1
        else:
            assert c.ready.status == 'True'
            assert c.ready.reason == 'HttpGetOk'
            assert c.installed.status == 'True'
            assert c.installed.reason == 'ContainerInstalled'
            assert c.restart_count > 1
            assert c.last_termination_status.stdout_tail.split()[-1].strip() == 'STDOUT_CONTENT'

    # Make exec prober OK again
    ctl.dirpath('script_result.txt').write('0')
    gevent.sleep(5)
    req = hq.app.processed_requests[-1]
    assert req.status.ready.status == 'True'
    assert req.status.ready.reason == 'RevisionReady'
    assert req.status.installed.status == 'True'
    assert req.status.installed.reason == 'RevisionInstalled'
    assert len(req.status.container) == 2
    for c in req.status.container:
        assert c.name in ('test_spec_containers_good', 'test_spec_containers_bad')
        if c.name == 'test_spec_containers_good':
            assert c.ready.status == 'True'
            assert c.ready.reason == 'CheckPortOk'
            assert c.installed.status == 'True'
            assert c.installed.reason == 'ContainerInstalled'
            assert c.restart_count == 1
        else:
            assert c.ready.status == 'True'
            assert c.ready.reason == 'HttpGetOk'
            assert c.installed.status == 'True'
            assert c.installed.reason == 'ContainerInstalled'
            assert c.restart_count > 1
            assert c.last_termination_status.stdout_tail.split()[-1].strip() == 'STDOUT_CONTENT'

    # Fail HTTP GET check
    util.terminate(http_serv)
    gevent.sleep(5)
    req = hq.app.processed_requests[-1]
    assert req.status.ready.status == 'False'
    assert req.status.ready.reason == 'RevisionNotReady'
    assert req.status.installed.status == 'True'
    assert req.status.installed.reason == 'RevisionInstalled'
    assert len(req.status.container) == 2
    for c in req.status.container:
        assert c.name in ('test_spec_containers_good', 'test_spec_containers_bad')
        if c.name == 'test_spec_containers_good':
            assert c.ready.status == 'True'
            assert c.ready.reason == 'CheckPortOk'
            assert c.installed.status == 'True'
            assert c.installed.reason == 'ContainerInstalled'
            assert c.restart_count == 1
        else:
            assert c.ready.status == 'False'
            assert c.ready.reason == 'HttpGetFail'
            assert c.installed.status == 'True'
            assert c.installed.reason == 'ContainerInstalled'
            assert c.restart_count > 1
            assert c.last_termination_status.stdout_tail.split()[-1].strip() == 'STDOUT_CONTENT'

    # Make HTTP GET check OK again
    http_serv = subprocess.Popen(['python', ctl.dirpath('pyserver.py').strpath, port])
    request.addfinalizer(lambda: util.terminate(http_serv))
    assert http_serv.poll() is None
    gevent.sleep(5)
    req = hq.app.processed_requests[-1]
    assert req.status.ready.status == 'True'
    assert req.status.ready.reason == 'RevisionReady'
    assert req.status.installed.status == 'True'
    assert req.status.installed.reason == 'RevisionInstalled'
    assert len(req.status.container) == 2
    for c in req.status.container:
        assert c.name in ('test_spec_containers_good', 'test_spec_containers_bad')
        if c.name == 'test_spec_containers_good':
            assert c.ready.status == 'True'
            assert c.ready.reason == 'CheckPortOk'
            assert c.installed.status == 'True'
            assert c.installed.reason == 'ContainerInstalled'
            assert c.restart_count == 1
        else:
            assert c.ready.status == 'True'
            assert c.ready.reason == 'HttpGetOk'
            assert c.installed.status == 'True'
            assert c.installed.reason == 'ContainerInstalled'
            assert c.restart_count > 1
            assert c.last_termination_status.stdout_tail.split()[-1].strip() == 'STDOUT_CONTENT'

    utils.must_stop_instancectl(ctl, check_loop_err=False)
