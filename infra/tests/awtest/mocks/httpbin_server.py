from __future__ import absolute_import

import logging
import os

from awacs.lib import context
from awtest.mocks import ports
from awtest.mocks.python_server import PythonServer
from awtest.network import is_ipv4_only


IS_ARCADIA = 'ARCADIA_SOURCE_ROOT' in os.environ


class Httpbin(PythonServer):
    port = ports.HTTPBIN_BASE_PORT

    def __init__(self, port=None):
        if self.port is not None:
            self.port = port
        ctx = context.BackgroundCtx().with_op(op_id=u'httpbin', log=logging.getLogger(u'awacs-tests-httpbin'))
        args = [u'httpbin_server_app', u'--log-to-stdout', u'--port', self.port]
        if is_ipv4_only():
            args.append(u'--ipv4')
        super(Httpbin, self).__init__(ctx, args)

    if IS_ARCADIA:
        def start(self):
            from yatest import common
            if self._process is not None:
                return
            self.ctx.log.info(u'starting {}'.format(self.__class__.__name__))
            sdstub_path = common.binary_path('infra/awacs/vendor/awacs/tests/awtest/mocks/httpbin_server_app/httpbin_server_app')
            stdbuf_path = common.build_path('infra/awacs/vendor/awacs/tests/deps/stdbuf')
            self._process = common.execute([stdbuf_path, u'-o0', sdstub_path] + self.args[1:], wait=False, close_fds=True)
            self._check_start()
