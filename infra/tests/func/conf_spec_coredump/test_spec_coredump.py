from __future__ import print_function
from __future__ import unicode_literals

import os
import gevent
import pytest

from yp_proto.yp.client.hq.proto import hq_pb2
from yp_proto.yp.client.hq.proto import types_pb2
from conftest import porto_required

import utils


@pytest.mark.skip(reason="cannot run this on sandbox hosts because of cusotm /proc/sys/kernel/core_pattern")
@porto_required
def test_spec_coredump_custom_command_loop_conf(cwd, ctl, request):
    env = utils.get_spec_env()
    env['BSCONFIG_ITAGS'] = env['BSCONFIG_ITAGS'].replace('use_hq_spec', 'enable_hq_poll')

    resp = hq_pb2.GetInstanceRevResponse()
    c = resp.revision.container.add()
    c.name = 'test_spec_coredump_loop_conf'
    c.coredump_policy.type = types_pb2.CoredumpPolicy.CUSTOM_CORE_COMMAND
    c.coredump_policy.custom_processor.command = " ".join(["tee", ctl.dirpath("coredump.bin").strpath])

    hq = utils.start_hq_mock(ctl, env, request, resp)

    utils.must_start_instancectl(
        ctl, request, ctl_environment=env, add_args=utils.make_hq_args(hq), console_logging=True
    )

    gevent.sleep(10)

    assert len(hq.app.processed_requests) > 0
    utils.must_stop_instancectl(ctl, check_loop_err=False)

    assert os.path.exists(ctl.dirpath("coredump.bin").strpath)
    os.remove(ctl.dirpath("coredump.bin").strpath)
