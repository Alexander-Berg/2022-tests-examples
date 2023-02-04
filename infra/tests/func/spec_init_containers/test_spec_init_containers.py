from __future__ import unicode_literals

import gevent
from yp_proto.yp.client.hq.proto import hq_pb2
from yp_proto.yp.client.hq.proto import types_pb2

import utils


def test_spec_init_containers(ctl, request):
    env = utils.get_spec_env()
    env["PREPARE_SCRIPT_MAX_DELAY"] = "1"
    resp = hq_pb2.GetInstanceRevResponse()
    c = resp.revision.container.add()
    c.name = 'test_spec_init_containers'
    c.restart_policy.max_period_seconds = 1
    c.readiness_probe.initial_delay_seconds = 3
    c.readiness_probe.max_period_seconds = 3
    c.command.extend(['/bin/sleep', '100'])

    c = resp.revision.init_containers.add()
    c.name = 'prepare1'
    c.restart_policy.type = types_pb2.RestartPolicy.ON_FAILURE
    c.restart_policy.max_period_seconds = 1
    c.command.extend(['python', 'prepare1.py'])

    c = resp.revision.init_containers.add()
    c.name = 'prepare2'
    c.restart_policy.type = types_pb2.RestartPolicy.ON_FAILURE
    c.restart_policy.max_period_seconds = 1
    c.command.extend(['python', 'prepare2.py'])

    hq = utils.start_hq_mock(ctl, env, request, resp)

    utils.must_start_instancectl(ctl, request, ctl_environment=env, add_args=utils.make_hq_args(hq))
    gevent.sleep(5)
    assert ctl.dirpath('state', 'loop.lock').exists()
    assert not ctl.dirpath('state', 'install.flag').exists()
    assert not ctl.dirpath('state', 'prepare1_install.flag').exists()
    assert not ctl.dirpath('state', 'prepare2_install.flag').exists()

    ctl.dirpath('prepare1.txt').write('OK')
    gevent.sleep(3)
    assert not ctl.dirpath('state', 'install.flag').exists()
    assert ctl.dirpath('state', 'prepare1_install.flag').exists()
    assert not ctl.dirpath('state', 'prepare2_install.flag').exists()

    ctl.dirpath('prepare2.txt').write('OK')
    gevent.sleep(3)
    assert ctl.dirpath('state', 'install.flag').exists()
    assert ctl.dirpath('state', 'prepare1_install.flag').exists()
    assert ctl.dirpath('state', 'prepare2_install.flag').exists()
    utils.must_stop_instancectl(ctl, check_loop_err=False)


def test_run_containers_before_prepare(ctl, request):
    env = utils.get_spec_env()
    env['BSCONFIG_ITAGS'] = "a_dc_sas a_geo_sas a_ctype_isstest a_itype_fake_type enable_hq_report enable_hq_poll"

    resp = hq_pb2.GetInstanceRevResponse()
    c = resp.revision.init_containers.add()
    c.name = 'prepare'
    c.restart_policy.type = types_pb2.RestartPolicy.ON_FAILURE
    c.restart_policy.max_period_seconds = 1
    c.command.extend(['mkdir', 'init'])

    hq = utils.start_hq_mock(ctl, env, request, resp)

    utils.must_start_instancectl(ctl, request, ctl_environment=env, add_args=utils.make_hq_args(hq))
    gevent.sleep(5)
    assert ctl.dirpath('init', 'test.txt').exists()
    utils.must_stop_instancectl(ctl, check_loop_err=False)
