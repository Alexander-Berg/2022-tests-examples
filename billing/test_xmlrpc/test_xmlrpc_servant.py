# -*- coding: utf-8 -*-

import logging

from butils.http_message import HttpResponse
from butils.wsgi_util import CachePinger, WsgiDispatcher
from butils.invoker import request_version

from balance.application import ServantApp, getApplication

from medium.medium_servant import MediumXmlRpcInvoker

from test_xmlrpc.uwsgi_run import PING_CACHE

try:
    from balance.version import VERSION
except ImportError:
    VERSION = '1.0-debug'

from butils import invoker

import balance.muzzle_util as ut

# typing imports
from butils.application import Application

log = logging.getLogger('')


class TestXmlRpcInvoker(MediumXmlRpcInvoker):
    name = 'test_xmlrpc'

    def import_logic(self, namespace):
        if namespace in self.logic:
            return

        session = getApplication().new_session()
        if session.config.get('INHERIT_TEST_XMLRPC_FROM_MEDIUM'):
            self.logic[namespace] = __import__(
                self.handlers[namespace], globals(), locals(), ['LogicWithMedium']
            ).LogicWithMedium
        else:
            super(TestXmlRpcInvoker, self).import_logic(namespace)


class CoverageInvoker(invoker.XmlRpcInvoker):
    name = 'coverage_xmlrpc'

    def __call__(self, environ, start_response):
        method_name = 'some_method'
        try:
            request_body = self.get_request_body(environ)
            method_ns, method_name = self.decode_method_name(environ, request_body)
            log.info('Method %s.%s started.', method_ns, method_name)
            with self.timer(method_name):
                params = [method_ns, method_name,
                          self.filter_environ(environ), request_body]
                response = self._process(*params)
        except Exception as ex:
            response = self.handle_exception(method_name, ex)

        start_response('%d %s' % (response.status, response.reason), response.headers.items())
        return response.body

    def handle_exception(self, method_name, ex):
        import xmlrpclib

        env_type = getApplication().get_current_env_type()
        ex_str = ut.formatExceptionInfo(level=40, include_traceback=(env_type != 'prod'))
        log.error(ex_str)

        body = self._dump_xmlrpc(xmlrpclib.Fault(-1, ex_str), methodresponse=0)
        return HttpResponse(500, body=body, content_type=self.content_type)


class TestXmlRpcPinger(CachePinger):
    cache_name = PING_CACHE
    cache_key = "TestXMLRPC"
    expires_sec = 15


class TestXmlRpcApp(ServantApp):
    name = 'TestXmlRpc'
    # database_id = "balance_1"
    __version__ = VERSION

    def checkalive(self):  # type: () -> bool
        session = self.new_session()
        return session.ping()


def request_svn_info(environ, start_response):
    from butils.arcadia_utils import get_svn_info

    http_status = '200'
    headers = [('Content-type', 'text/plain')]
    start_response(http_status, headers)
    yield "\n".join(str(k) + ": " + str(v) for (k, v) in get_svn_info().items())


def create_dispatcher(app):  # type: (Application) -> WsgiDispatcher
    # app не используется, но Application должен быть инициализирован до создания диспатчера

    pinger = TestXmlRpcPinger()
    test_xmlrpc_handlers = {
        "/xmlrpc": TestXmlRpcInvoker(
            {
                'Balance': 'test_xmlrpc.test_xmlrpc_logic',
                'Balance2': 'test_xmlrpc.test_xmlrpc_logic',
                'TestBalance': 'test_xmlrpc.test_xmlrpc_logic',
            },
            timeout_map={},
        ),
        '/version': request_version,
        "/svnvinfo": request_svn_info,
        "/ping": pinger,
        "/coverage": CoverageInvoker({'Coverage': 'test_xmlrpc.coverage_logic'}),
    }

    return WsgiDispatcher(handlers=test_xmlrpc_handlers)
# vim:ts=4:sts=4:sw=4:et
