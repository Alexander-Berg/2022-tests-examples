# -*- coding: utf-8 -*-
import balancer.test.util.proto.http2.connection as http2_conn
from balancer.test.util.proto.handler.client.http import HTTPConnection


class HTTPConnectionManager(object):
    def __init__(self, resource_manager, stream_manager):
        super(HTTPConnectionManager, self).__init__()
        self.__resource_manager = resource_manager
        self.__stream_manager = stream_manager

    def __create(self, sock, newline='\r\n'):
        conn = HTTPConnection(sock, newline=newline)
        self.__resource_manager.register(conn)
        return conn

    def create(self, port, timeout=None, host='localhost', source_address=None, newline='\r\n'):
        """
        :rtype: HTTPConnection
        """
        return self.__create(self.__stream_manager.create(port, timeout=timeout, host=host, source_address=source_address), newline=newline)

    def create_ssl(self, port, ssl_options=None, host='localhost', check_closed=True, conn_timeout=None):
        """
        :rtype: HTTPConnection
        """
        return self.__create(self.__stream_manager.create_ssl(port, ssl_options, host=host, check_closed=check_closed, conn_timeout=conn_timeout))

    def create_pyssl(self, port, ssl_options=None, host='localhost'):
        return self.__create(self.__stream_manager.create_pyssl(port, ssl_options, host=host))


class HTTP2ConnectionManager(object):
    def __init__(self, resource_manager, stream_manager):
        super(HTTP2ConnectionManager, self).__init__()
        self.__resource_manager = resource_manager
        self.__stream_manager = stream_manager

    def __create(self, sock):
        conn = http2_conn.ClientConnection(sock)
        self.__resource_manager.register(conn)
        return conn

    def create(self, port, timeout=None, host='localhost'):
        return self.__create(self.__stream_manager.create(port, timeout=timeout, host=host))

    def create_ssl(self, port, ssl_options=None, host='localhost'):
        return self.__create(self.__stream_manager.create_ssl(port, ssl_options, host=host))

    def create_pyssl(self, port, ssl_options=None, host='localhost'):
        return self.__create(self.__stream_manager.create_pyssl(port, ssl_options, host=host))


class ConnectionManager(object):
    def __init__(self, http_connection_manager, http2_connection_manager):
        super(ConnectionManager, self).__init__()
        self.__http = http_connection_manager
        self.__http2 = http2_connection_manager

    @property
    def http(self):
        """
        :rtype: HTTPConnectionManager
        """
        return self.__http

    @property
    def http2(self):
        return self.__http2
