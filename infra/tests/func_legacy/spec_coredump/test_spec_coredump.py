from __future__ import print_function
from __future__ import unicode_literals

import os
import re
import textwrap
import gevent

from yp_proto.yp.client.hq.proto import hq_pb2
from yp_proto.yp.client.hq.proto import types_pb2
from conftest import porto_required
from flask import Flask, request as flask_request
from sepelib.flask.server import WebServer

import utils


@porto_required
def test_spec_coredump_custom_command(ctl, request):
    env = utils.get_spec_env()

    resp = hq_pb2.GetInstanceRevResponse()
    resp.revision.type = types_pb2.InstanceRevision.APP_CONTAINER
    c = resp.revision.container.add()
    c.name = 'test_spec_coredump'
    c.restart_policy.max_period_seconds = 1
    c.command.extend([ctl.dirpath('mem_eater.py').strpath, '64', '1'])
    c.readiness_probe.initial_delay_seconds = 3
    c.readiness_probe.max_period_seconds = 3
    c.coredump_policy.type = types_pb2.CoredumpPolicy.CUSTOM_CORE_COMMAND
    c.coredump_policy.custom_processor.command =\
        " ".join(["tee", ctl.dirpath("coredump.bin").strpath])
    h = c.readiness_probe.handlers.add()
    h.type = types_pb2.Handler.EXEC
    h.exec_action.command.extend(['/bin/sh', '-c', 'exit 0'])

    hq = utils.start_hq_mock(ctl, env, request, resp)

    utils.must_start_instancectl(
        ctl, request, ctl_environment=env, add_args=utils.make_hq_args(hq), console_logging=True
    )

    gevent.sleep(10)

    assert len(hq.app.processed_requests) > 0
    utils.must_stop_instancectl(ctl, check_loop_err=False)

    assert os.path.exists(ctl.dirpath("coredump.bin").strpath)
    os.remove(ctl.dirpath("coredump.bin").strpath)


def prepare_iteration(ctl, request, count=5, total_size=1024 * (1<< 20), ttl=0, prob=100):
    env = utils.get_spec_env()

    resp = hq_pb2.GetInstanceRevResponse()
    resp.revision.type = types_pb2.InstanceRevision.APP_CONTAINER
    c = resp.revision.container.add()
    c.name = 'test_spec_coredump'
    c.restart_policy.max_period_seconds = 1
    c.command.extend([ctl.dirpath('mem_eater.py').strpath, '64', '16'])
    c.readiness_probe.initial_delay_seconds = 3
    c.readiness_probe.max_period_seconds = 3

    core_dir = ctl.dirpath("coredumps").strpath
    os.mkdir(core_dir)

    if prob:
        c.coredump_policy.type = types_pb2.CoredumpPolicy.COREDUMP
        c.coredump_policy.coredump_processor.probability = prob
        c.coredump_policy.coredump_processor.path = core_dir

        if count > 0:
            c.coredump_policy.coredump_processor.count_limit = count

        if total_size > 0:
            c.coredump_policy.coredump_processor.total_size_limit = total_size

        if ttl > 0:
            c.coredump_policy.coredump_processor.cleanup_policy.type = types_pb2.CoredumpCleanupPolicy.TTL
            c.coredump_policy.coredump_processor.cleanup_policy.ttl.seconds = ttl

        c.coredump_policy.coredump_processor.aggregator.type = types_pb2.CoredumpAggregator.DISABLED

    else:
        c.coredump_policy.type = types_pb2.CoredumpPolicy.NONE

    h = c.readiness_probe.handlers.add()
    h.type = types_pb2.Handler.EXEC
    h.exec_action.command.extend(['/bin/sh', '-c', 'exit 0'])

    hq = utils.start_hq_mock(ctl, env, request, resp)

    return core_dir, hq, env


def run_iteration(ctl, request, core_dir, hq, env, wait_time=10, count=5, total_size=1024 * (1 << 20)):
    utils.must_start_instancectl(
        ctl, request, ctl_environment=env, add_args=utils.make_hq_args(hq), console_logging=True
    )

    gevent.sleep(wait_time)
    assert len(hq.app.processed_requests) > 0

    utils.must_stop_instancectl(ctl, check_loop_err=False)

    assert len(os.listdir(core_dir)) == count

    size = 0
    oldest = (0, "")

    for de in os.listdir(core_dir):
        stat = os.stat(os.path.join(core_dir, de))
        size += stat.st_size
        if oldest[0] == 0 or oldest[0] > stat.st_mtime:
            oldest = (stat.st_mtime, de)

    assert size <= total_size

    return oldest


@porto_required
def test_spec_coredump_limits(ctl, request):
    core_dir, hq, env = prepare_iteration(ctl, request)

    assert len(os.listdir(core_dir)) == 0

    oldest = run_iteration(ctl, request, core_dir, hq, env)
    oldest2 = run_iteration(ctl, request, core_dir, hq, env)

    # We fill coredumps with first run, the second run shouldn't change anything

    assert oldest == oldest2


@porto_required
def test_spec_coredump_disabled(ctl, request):
    core_dir, hq, env = prepare_iteration(ctl, request, prob=0)

    assert len(os.listdir(core_dir)) == 0

    assert run_iteration(ctl, request, core_dir, hq, env, count=0) == (0, "")


@porto_required
def test_spec_coredump_ttl(ctl, request):
    core_dir, hq, env = prepare_iteration(ctl, request, ttl=5)

    assert len(os.listdir(core_dir)) == 0

    oldest = run_iteration(ctl, request, core_dir, hq, env)
    oldest2 = run_iteration(ctl, request, core_dir, hq, env)

    # We have ttl set, so the oldest coredump should change between runs

    assert oldest != oldest2


@porto_required
def test_spec_coredump_aggr(ctl, request):
    env = utils.get_spec_env()

    resp = hq_pb2.GetInstanceRevResponse()
    resp.revision.type = types_pb2.InstanceRevision.APP_CONTAINER
    resp.revision.tags.append("a_ctype_isstest")

    c = resp.revision.container.add()

    c.name = 'test_spec_coredump'
    c.restart_policy.max_period_seconds = 1
    c.command.extend([ctl.dirpath('mem_eater.py').strpath, '64', '16'])
    c.readiness_probe.initial_delay_seconds = 3
    c.readiness_probe.max_period_seconds = 3

    core_dir = ctl.dirpath("coredumps").strpath
    os.mkdir(core_dir)

    received_traces = []

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

    web_server = WebServer(web_cfg, app, version='test')
    web_thread = gevent.spawn(web_server.run)
    request.addfinalizer(web_server.stop)
    request.addfinalizer(web_thread.kill)

    port = web_server.wsgi.socket.getsockname()[1]

    c.coredump_policy.type = types_pb2.CoredumpPolicy.COREDUMP
    c.coredump_policy.coredump_processor.path = core_dir
    c.coredump_policy.coredump_processor.cleanup_policy.type = types_pb2.CoredumpCleanupPolicy.TTL
    c.coredump_policy.coredump_processor.cleanup_policy.ttl.seconds = 5
    c.coredump_policy.coredump_processor.count_limit = 5
    c.coredump_policy.coredump_processor.total_size_limit = 1024 * (1 << 20)
    c.coredump_policy.coredump_processor.aggregator.type = types_pb2.CoredumpAggregator.SAAS_AGGREGATOR
    c.coredump_policy.coredump_processor.aggregator.saas.url = 'http://localhost:{}/submit/'.format(port)
    c.coredump_policy.coredump_processor.aggregator.saas.service_name = 'test_spec_coredump'
    c.coredump_policy.coredump_processor.aggregator.saas.gdb.exec_path = '/usr/bin/gdb'
    c.coredump_policy.coredump_processor.probability = 100

    h = c.readiness_probe.handlers.add()
    h.type = types_pb2.Handler.EXEC
    h.exec_action.command.extend(['/bin/sh', '-c', 'exit 0'])

    hq = utils.start_hq_mock(ctl, env, request, resp)

    utils.must_start_instancectl(
        ctl, request, ctl_environment=env, add_args=utils.make_hq_args(hq), console_logging=True
    )

    gevent.sleep(15)

    assert len(hq.app.processed_requests) > 0

    web_server.stop()
    web_thread.kill()

    gevent.sleep(0.5)

    utils.must_stop_instancectl(ctl, check_loop_err=False)

    assert len(os.listdir(core_dir)) == 5
    assert len(received_traces) > 5

    ctype = ''
    ctype_prefix = 'a_ctype_'
    for tag in env['BSCONFIG_ITAGS'].split():
        if tag.startswith(ctype_prefix):
            ctype = tag[len(ctype_prefix):]
            break

    match_abort = re.compile('terminated with signal (SIGABRT|6), Aborted\.\\n\#0  0x[0-9a-f]{16} in ')

    for trace in received_traces:
        assert re.search(match_abort, trace['traces'])

        received_params = trace['params'].to_dict()

        del received_params['time']

        assert received_params == {
            'service': 'test_spec_coredump',
            'ctype': ctype,
            'server': env.get('BSCONFIG_INAME', "")
        }
