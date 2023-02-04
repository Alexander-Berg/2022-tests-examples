# -*- coding: utf-8 -*-
import errno
import socket
import time
import OpenSSL
from balancer.test.util.stream.io.stream import SocketStream, PySslStream
from balancer.test.util.stream.ssl.stream import SSLClientStream, SSLClientOptions


class StreamManager(object):
    def __init__(self, resource_manager, fs_manager, openssl_path, process_manager):
        super(StreamManager, self).__init__()
        self.__resource_manager = resource_manager
        self.__fs_manager = fs_manager
        self.__openssl_path = openssl_path
        self.__process_manager = process_manager
        self.__first_connect = True

    @property
    def openssl_path(self):
        return self.__openssl_path

    def create(self, port, host='localhost', timeout=None, source_address=None):
        sock = SocketStream.from_address(host, port, timeout=timeout, source_address=source_address)
        self.__resource_manager.register(sock)
        return sock

    def create_ssl(self, port, ssl_options=None, host='localhost', check_closed=True, conn_timeout=None):
        if ssl_options is None:
            ssl_options = SSLClientOptions()
        if ssl_options.sess_out is None:
            ssl_options.sess_out = self.__fs_manager.create_file('%s:%d.session' % (host, port))
        sock = SSLClientStream(
            self.__process_manager,
            host, port,
            ssl_options,
            self.__openssl_path,
            check_closed=check_closed,
            conn_timeout=conn_timeout,
        )
        self.__resource_manager.register(sock)
        return sock

    def create_pyssl(self, port, ssl_options=None, host='localhost'):
        if ssl_options is None:
            ssl_options = SSLClientOptions()
        ssl_ctx = OpenSSL.SSL.Context(OpenSSL.SSL.TLSv1_2_METHOD)
        if ssl_options.alpn:
            ssl_ctx.set_alpn_protos([ssl_options.alpn])

        delay = 0.1
        if self.__first_connect:
            retries = 5
            self.__first_connect = False
        else:
            retries = 1

        for i in range(retries):
            ssl_sock = OpenSSL.SSL.Connection(ssl_ctx)
            ssl_sock.set_connect_state()
            inner_sock = socket.create_connection((host, port), timeout=SocketStream.TIMEOUT)
            sock = PySslStream(inner_sock, ssl_sock)
            self.__resource_manager.register(sock)
            try:
                sock.do_handshake()
            except socket.error, err:
                if err.errno == errno.ECONNRESET and i < retries - 1:
                    time.sleep(delay)
                    delay *= 2
                    continue
                else:
                    raise
            else:
                break
        return sock
