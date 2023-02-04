from __future__ import unicode_literals

import gevent
from yp_proto.yp.client.hq.proto import hq_pb2
from yp_proto.yp.client.hq.proto import types_pb2

import utils


def test_spec_expanding(ctl, request):
    p = utils.get_free_port() - 5
    env = utils.get_spec_env()
    env["BSCONFIG_IPORT"] = unicode(p)
    env["BSCONFIG_INAME"] = '{}:{}'.format(env['BSCONFIG_IHOST'], p)

    resp = hq_pb2.GetInstanceRevResponse()
    c = resp.revision.container.add()
    c.name = 'test_spec_expanding'
    c.restart_policy.max_period_seconds = 1
    c.readiness_probe.initial_delay_seconds = 3
    c.readiness_probe.max_period_seconds = 3
    c.command.extend(['python', '{BSCONFIG_IDIR}/pyserver.py', '{BSCONFIG_IPORT_PLUS_5}'])

    e = c.env.add()
    e.name = 'FILE_PATH'
    e.value_from.type = types_pb2.EnvVarSource.LITERAL_ENV
    e.value_from.literal_env.value = 'script_result.txt'

    h = c.readiness_probe.handlers.add()
    h.type = types_pb2.Handler.EXEC
    h.exec_action.command.extend(['/bin/sh', '-c', 'exit $(cat {BSCONFIG_IDIR}/{FILE_PATH})'])

    h = c.readiness_probe.handlers.add()
    h.type = types_pb2.Handler.TCP_SOCKET
    h.tcp_socket.port = '{BSCONFIG_IPORT_PLUS_5}'

    c = resp.revision.container.add()
    c.name = 'test_spec_expanding2'
    c.restart_policy.max_period_seconds = 1
    c.readiness_probe.initial_delay_seconds = 3
    c.readiness_probe.max_period_seconds = 3
    c.command.extend(['/bin/sh', '-c', 'env > env.txt; /bin/sleep 100'])
    e = c.env.add()
    e.name = 'FILE_PATH_SECOND'
    e.value_from.type = types_pb2.EnvVarSource.LITERAL_ENV
    e.value_from.literal_env.value = 'SECOND_VALUE'

    hq = utils.start_hq_mock(ctl, env, request, resp)

    ctl.dirpath('script_result.txt').write('0')

    utils.must_start_instancectl(ctl, request, ctl_environment=env, add_args=utils.make_hq_args(hq),
                                 console_logging=True)
    gevent.sleep(5)
    assert len(hq.app.processed_requests) > 0
    req = hq.app.processed_requests[-1]

    assert req.status.ready.status == 'True'
    assert req.status.ready.reason == 'RevisionReady'
    assert req.status.installed.status == 'True'
    assert req.status.installed.reason == 'RevisionInstalled'
    assert len(req.status.container) == 2
    for c in req.status.container:
        assert c.name in ('test_spec_expanding', 'test_spec_expanding2')
        if c.name == 'test_spec_expanding':
            assert c.ready.status == 'True'
            assert c.ready.reason == 'CheckPortOk'
            assert c.installed.status == 'True'
            assert c.installed.reason == 'ContainerInstalled'
        else:
            assert c.ready.status == 'True'
            assert c.ready.reason == 'ContainerReady'
            assert c.installed.status == 'True'
            assert c.installed.reason == 'ContainerInstalled'

    r = ctl.dirpath('env.txt').readlines()
    d = {}
    for i in r:
        k, v = i.split('=', 1)
        d[k.strip()] = v.strip()
    assert d['a_prj'] == 'undefined'
    assert d['a_dc'] == 'sas'
    assert d['a_tier'] == 'undefined'
    assert d['a_itype'] == 'fake_type'
    assert d['a_geo'] == 'sas'
    assert d['a_ctype'] == 'isstest'
    assert d['FILE_PATH_SECOND'] == 'SECOND_VALUE'
    assert d['HOSTNAME'] == 'localhost'
    assert d['NANNY_SERVICE_ID'] == 'fake_instancectl_service'
    assert d['NODE_NAME'] == 'fake-node.search.yandex.net'
    assert d['BSCONFIG_IDIR'] == ctl.dirname.rstrip('/')
    assert d['BSCONFIG_ITAGS'] == 'a_dc_sas a_geo_sas a_ctype_isstest a_itype_fake_type enable_hq_report use_hq_spec'
    assert d['BSCONFIG_INAME'] == 'fake-host.search.yandex.net:{}'.format(p)
    assert d['BSCONFIG_IHOST'] == 'fake-host.search.yandex.net'
    assert d['BSCONFIG_IPORT'] == unicode(p)
    assert d['INSTANCECTL_CONTAINER'] == 'test_spec_expanding2'
    for i in xrange(1, 21):
        assert d['BSCONFIG_IPORT_PLUS_{}'.format(i)] == unicode(p + i)
    assert 'FILE_PATH' not in d
    utils.must_stop_instancectl(ctl, check_loop_err=False)
