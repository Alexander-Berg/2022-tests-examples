from __future__ import unicode_literals

import os
import sys

import gevent
import fallocate
from yp_proto.yp.client.hq.proto import hq_pb2

from instancectl import constants
import utils


def is_fallocate_collapse_available(cwd):
    if not sys.platform.startswith('linux'):
        return False
    t = cwd.join('test.txt')
    t.write('1' * 1024 * 1024)
    with open(t.strpath, 'r+') as f:
        fd = f.fileno()
        s = os.fstat(fd)
        try:
            fallocate.fallocate(fd, 0, s.st_blksize, constants.FALLOC_FL_COLLAPSE_RANGE)
        except Exception:
            return False
        return True


def test_spec_rotate_stdout(cwd, ctl, request):
    env = utils.get_spec_env()

    resp = hq_pb2.GetInstanceRevResponse()
    c = resp.revision.container.add()
    c.name = 'test_rotate_stdout'
    c.command.extend(['/bin/sleep', '1000'])
    hq = utils.start_hq_mock(ctl, env, request, resp)
    megabyte = 1024 * 1024

    out = cwd.join('test_rotate_stdout.out')
    out.write('x' * (2 * megabyte))
    assert out.size() == 2 * megabyte

    err = cwd.join('test_rotate_stdout.err')
    err.write('x' * (2 * megabyte))
    assert err.size() == 2 * megabyte

    utils.must_start_instancectl(ctl, request, ctl_environment=env, add_args=utils.make_hq_args(hq))
    gevent.sleep(10)
    assert len(hq.app.processed_requests) > 0
    req = hq.app.processed_requests[-1]
    assert req.status.ready.status == 'True'
    assert req.status.ready.reason == 'RevisionReady'
    assert req.status.installed.status == 'True'
    assert req.status.installed.reason == 'RevisionInstalled'

    utils.must_stop_instancectl(ctl, check_loop_err=False)

    if is_fallocate_collapse_available(cwd):
        assert out.size() == megabyte / 2
        assert err.size() == megabyte / 2
    else:
        assert out.size() == 0
        assert err.size() == 0
