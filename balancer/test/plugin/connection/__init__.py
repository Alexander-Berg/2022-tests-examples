# -*- coding: utf-8 -*-
from balancer.test.util import multiscope
from balancer.test.util.context import ManagerFixture
from balancer.test.util.proto.http.stream import HTTPReaderException

from balancer.test.util.connection import ConnectionManager, HTTPConnectionManager, HTTP2ConnectionManager
from balancer.test.util.proto.http2.framing import frames


pytest_plugins = [
    'balancer.test.plugin.stream',
]


class ConnectionContext(object):
    def __init__(self):
        super(ConnectionContext, self).__init__()
        self.state.register('default_port_func')

    def create_http_connection(self, port=None, host='localhost', timeout=None, source_address=None, http2=False):
        """
        Create a HTTP connection. If port is not specified, create a connection to stored balancer instance.

        :rtype: HTTPConnection
        """
        if port is None:
            port = self.state.default_port_func()
        if http2 is True:
            conn = self.manager.connection.http2.create(
                port,
                host=host,
                timeout=timeout,
            )
            conn.write_preface()
            conn.wait_frame(frames.Settings)
            return conn
        return self.manager.connection.http.create(
            port,
            host=host,
            timeout=timeout,
            source_address=source_address,
        )

    def perform_request(self, request, port=None, host='localhost', timeout=None, source_address=None, http2=False):
        """
        Send request and receive response. If port is not specified request is sent to stored balancer instance.

        :type request: :class:`.HTTPRequest` or :class:`.RawHTTPRequest`
        :param request: request to send

        :rtype: HTTPResponse
        """
        with self.create_http_connection(
            port=port,
            host=host,
            timeout=timeout,
            source_address=source_address,
            http2=http2
        ) as conn:
            return conn.perform_request(request)

    def perform_request_xfail(self, request, port=None, host='localhost', timeout=None, source_address=None):
        """
        Send request, reading response should fail.
        If port is not specified request is sent to stored balancer instance.

        :type request: :class:`.HTTPRequest` or :class:`.RawHTTPRequest`
        :param request: request to send

        :rtype: HTTPReadError
        """
        # pytest.raises raises builtin.Failed exception which cannot be imported and caught in try-except
        # using try-except-else instead
        try:
            self.perform_request(
                request,
                port=port,
                host=host,
                timeout=timeout,
                source_address=source_address,
            )
        except HTTPReaderException, e:
            return e.read_error
        else:
            assert False, 'request performed successfully'


@multiscope.fixture
def connection_manager(resource_manager, stream_manager):
    http_connection_manager = HTTPConnectionManager(resource_manager, stream_manager)
    http2_connection_manager = HTTP2ConnectionManager(resource_manager, stream_manager)
    return ConnectionManager(http_connection_manager, http2_connection_manager)


MANAGERS = [ManagerFixture('connection', 'connection_manager')]
CONTEXTS = [ConnectionContext]
