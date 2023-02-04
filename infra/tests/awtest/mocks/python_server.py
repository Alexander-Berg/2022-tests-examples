from __future__ import absolute_import

import os
import subprocess

import six

from awtest.core import wait_until
from awtest.network import is_ipv4_only, is_port_open


class PythonServer(object):
    __slots__ = (u'ctx', u'args', u'enable_debug_log', u'_process')
    port = None
    if is_ipv4_only():
        host = u'127.0.0.1'
    else:
        host = u'::1'

    def __init__(self, ctx, args, enable_debug_log=False):
        self.ctx = ctx
        self.args = list(map(six.text_type, args))
        self.enable_debug_log = enable_debug_log
        self._process = None

    def start(self):
        if self._process is not None:
            return
        self.ctx.log.info(u'starting {}'.format(self.__class__.__name__))
        stdbuf_path = 'stdbuf'
        IS_ARCADIA = 'ARCADIA_SOURCE_ROOT' in os.environ
        if IS_ARCADIA:
            from yatest import common
            stdbuf_path = common.build_path('infra/awacs/vendor/awacs/tests/deps/stdbuf')
        self._process = subprocess.Popen([stdbuf_path, u'-o0', u'python', u'-m'] + self.args,
                                         cwd=u'./tests/awtest/mocks/',
                                         stdout=subprocess.PIPE,
                                         stderr=subprocess.PIPE)
        self._check_start()

    def _check_start(self):
        success = wait_until(lambda: is_port_open(self.host, self.port), timeout=3)
        if not success:
            self.terminate(enable_debug_log=True)
            raise RuntimeError(u'Failed to start {}'.format(self.__class__.__name__))

        self.ctx.log.info(u'{} is up and running: {}'.format(self.__class__.__name__, self.args))

    def terminate(self, enable_debug_log=False):
        if self._process is None:
            return None
        self._process.terminate()
        if self.enable_debug_log or enable_debug_log:
            stdout = self._process.stdout.read()
            stderr = self._process.stderr.read()
            if stdout:
                self.ctx.log.info(u'stdout:')
                self.ctx.log.info(stdout)
            if stderr:
                self.ctx.log.info(u'stderr:')
                self.ctx.log.info(stderr)
