from __future__ import print_function
from __future__ import unicode_literals

import gevent
from yp_proto.yp.client.hq.proto import hq_pb2
from yp_proto.yp.client.hq.proto import types_pb2
from conftest import porto_required
from flask import Flask, request as flask_request
from sepelib.flask.server import WebServer

import utils
import os
import re


@porto_required
def test_spec_porto_access(ctl, request):
    env = utils.get_spec_env()

    resp = hq_pb2.GetInstanceRevResponse()
    resp.revision.type = types_pb2.InstanceRevision.APP_CONTAINER
    c = resp.revision.container.add()
    c.name = 'test_spec_porto_access'
    c.restart_policy.max_period_seconds = 1

    stdout_path = ctl.dirpath('stdout.txt').strpath

    c.command.extend([ctl.dirpath('porto_test.py').strpath, stdout_path])
    c.readiness_probe.initial_delay_seconds = 3
    c.readiness_probe.max_period_seconds = 3
    h = c.readiness_probe.handlers.add()
    c.security_context.porto_access_policy = 'isolate'
    h.type = types_pb2.Handler.EXEC
    h.exec_action.command.extend(['/bin/sh', '-c', 'exit 0'])

    hq = utils.start_hq_mock(ctl, env, request, resp)

    utils.must_start_instancectl(
        ctl, request, ctl_environment=env, add_args=utils.make_hq_args(hq), console_logging=True
    )

    gevent.sleep(10)

    assert len(hq.app.processed_requests) > 0
    utils.must_stop_instancectl(ctl, check_loop_err=False)

    with open(stdout_path) as f:
        assert f.read() == "OK"

    os.remove(stdout_path)
