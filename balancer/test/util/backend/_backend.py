# -*- coding: utf-8 -*-
import multiprocessing
import threading
import time
import socket
import errno
import itertools

from balancer.test.util.backend import socket_server
from balancer.test.util.process import ProcessException
from balancer.test.util.stream.ssl.stream import SSLServerStream
from balancer.test.util.stream.io import stream
from balancer.test.util.server import ServerConfig
from balancer.test.util.resource import AbstractResource

from balancer.test.util.proto.handler.server import ConfigServerHandlerFactory


class PythonBackendException(Exception):
    pass


def _as_list(item):
    if isinstance(item, list) or isinstance(item, tuple):
        return item
    return [item]


class BackendConfig(ServerConfig):
    def __init__(self, handler_factory):
        super(BackendConfig, self).__init__()
        self.__ports = None
        self.__hosts = None
        self.register_listen_port('port')
        self.handler_factory = handler_factory

    @property
    def port(self):
        return self.__ports[0] if self.__ports else None

    @port.setter
    def port(self, port):
        self.ports = port

    @property
    def ports(self):
        return self.__ports

    @ports.setter
    def ports(self, ports):
        self.__ports = _as_list(ports)

    @property
    def host(self):
        return self.__hosts[0] if self.__hosts else None

    @host.setter
    def host(self, host):
        self.hosts = host

    @property
    def hosts(self):
        return self.__hosts

    @hosts.setter
    def hosts(self, hosts):
        self.__hosts = _as_list(hosts)


class TCPBackendConfig(BackendConfig):
    def __init__(
            self,
            handler_factory,
            family=(socket.AF_INET6, socket.AF_INET),
            host='localhost',
            listen_queue=64
    ):
        super(TCPBackendConfig, self).__init__(handler_factory)
        self.host = host
        self.listen_queue = listen_queue
        self.family = list(family)


class ReuseServer(socket_server.TCPServer):
    def __init__(self, config, *args):
        self.config = config
        self.addrs = list(itertools.product(
            config.family,
            config.hosts,
            config.ports
        ))
        socket_server.TCPServer.__init__(
            self,
            addrs=self.addrs,
            RequestHandlerClass=config.handler_factory,
            bind_and_activate=True,
            listen_queue=config.listen_queue,
            allow_reuse_address=True
        )

    def _get_request(self, sock):
        conn, address = socket_server.TCPServer._get_request(self, sock)
        self.config.handler_factory.state.accepted.inc()
        self.config.handler_factory.state.conn_addrs.put((address[0], address[1]))
        conn = stream.SocketStream(conn)
        return conn, address


class SSLBackendConfig(BackendConfig):
    def __init__(self, handler_factory, key, cert):
        super(SSLBackendConfig, self).__init__(handler_factory)
        self.key = key
        self.cert = cert


class SSLServer(object):
    def __init__(self, config, process_manager, openssl_path):
        super(SSLServer, self).__init__()
        self.__config = config
        self.__socket = SSLServerStream(process_manager, config.port, config.key, config.cert, openssl_path, quiet=True)
        self.server_activate()

    def server_activate(self):
        pass

    def serve_forever(self):
        handler_thread = threading.Thread(target=self.__config.handler_factory, args=(self.__socket, ))
        handler_thread.start()
        self.__config.handler_factory.state.accepted.inc()
        self.__config.handler_factory.state.conn_addrs.put(('', ''))  # FIXME: workaround to be like ReuseServer

    # TODO stop handler_thread (in ReuseServer too)
    def shutdown(self):
        pass

    def server_close(self):
        self.__socket.close()


class PythonBackend(AbstractResource):
    __HOST = 'localhost'
    __START_TIME = 5
    __SLEEP_TIMEOUT = 0.01
    __JOIN_TIMEOUT = 10

    # FIXME avoid stream_manager, logger, fs_manager
    def __init__(self, server_type, server_config, connect_func, logger, process_manager, openssl_path):
        super(PythonBackend, self).__init__()
        self.__name = server_config.handler_factory.name

        self.__connect_func = connect_func
        self.__logger = logger
        self.__process_manager = process_manager
        self.__openssl_path = openssl_path

        self.__server_config = server_config
        self.__server_type = server_type
        self.__stop_flag = multiprocessing.Event()
        self.__server_stopped = multiprocessing.Event()
        self.__server_process = multiprocessing.Process(target=self.__start_server)

    @property
    def server_config(self):
        """
        :rtype: BackendConfig
        """
        return self.__server_config

    @property
    def state(self):
        """
        :rtype: AbstractState
        """
        return self.__server_config.handler_factory.state

    def start(self, check_timeout=None):
        if self.__server_process.ident is not None or self.__stop_flag.is_set():
            raise PythonBackendException('backend already running')
        self.__stop_flag.value = False
        self.__server_stopped.value = False
        self.__server_process.start()

        if check_timeout:
            time.sleep(check_timeout)

        self.__check_process()

    stop = AbstractResource.finish

    def _finish(self):
        self.__stop_flag.set()
        self.__server_stopped.wait(self.__JOIN_TIMEOUT)
        self.state.finish()
        self.__server_process.join(self.__JOIN_TIMEOUT)
        if self.__server_process.is_alive():
            self.__logger.error('backend process not finished')
            self.__server_process.terminate()
        self.__server_config.finish()

    def __start_server(self):
        server_ = self.__server_type(self.__server_config, self.__process_manager, self.__openssl_path)
        server_thread = threading.Thread(target=server_.serve_forever)
        server_thread.start()
        self.__stop_flag.wait()
        server_.shutdown()
        server_.server_close()
        server_thread.join(self.__JOIN_TIMEOUT)
        self.__server_stopped.set()
        if server_thread.is_alive():
            self.__logger.error('backend thread not finished')
            server_thread.terminate()

    # TODO check backend with condition variable
    def __check_process(self):
        start_time = time.time()

        def check_time():
            if time.time() - start_time > self.__START_TIME:
                raise ProcessException('%s timed out' % self.__name)

        while True:
            try:
                sock = self.__connect_func(host=self.__server_config.host or 'localhost', port=self.__server_config.port)
                sock.close()
                while self.state.accepted.value == 0:
                    time.sleep(self.__SLEEP_TIMEOUT)
                    check_time()
                self.state.accepted.reset()
                self.state.conn_addrs.get()
                break
            except socket.error, err:
                if err.errno == errno.ECONNREFUSED:
                    pass
                else:
                    raise ProcessException('%s exception: %s' % (self.__name, str(err)))

            check_time()
            time.sleep(self.__SLEEP_TIMEOUT)

    def get_name(self):
        return self.__name


class BackendManager(object):
    def __init__(self, logger, process_manager, resource_manager, config_manager, stream_manager, openssl_path):
        super(BackendManager, self).__init__()
        self.__logger = logger
        self.__process_manager = process_manager
        self.__stream_manager = stream_manager
        self.__openssl_path = openssl_path

        self.__resource_manager = resource_manager
        self.__config_manager = config_manager

    def start(self, handler_config, state=None, port=None, listen_queue=64, host=None, family=None):
        """
        :param Config handler_config: backend handler config
        :rtype: PythonBackend
        """
        if state is None:
            state = handler_config.STATE_TYPE(handler_config)
        handler_factory = ConfigServerHandlerFactory(state, handler_config)
        server_config = TCPBackendConfig(handler_factory=handler_factory, listen_queue=listen_queue)
        self.__config_manager.fill(server_config)
        if port is not None:
            server_config.port = port
        if host is not None:
            server_config.host = host
        if family is not None:
            server_config.family = family
        return self.start_generic(server_config)

    def start_ssl(self, handler_config, key, cert):
        handler_factory = ConfigServerHandlerFactory(handler_config.STATE_TYPE(handler_config), handler_config)
        server_config = SSLBackendConfig(handler_factory=handler_factory, key=key, cert=cert)
        self.__config_manager.fill(server_config)
        return self.start_generic(server_config)

    def start_generic(self, server_config):
        """
        :param BackendConfig server_config: backend config
        :rtype: PythonBackend
        """
        if isinstance(server_config, TCPBackendConfig):
            connect_func = self.__stream_manager.create
            server_type = ReuseServer
        else:
            connect_func = self.__stream_manager.create_ssl
            server_type = SSLServer
        backend = PythonBackend(
            server_type,
            server_config,
            connect_func,
            self.__logger,
            self.__process_manager,
            self.__openssl_path
        )
        self.__resource_manager.register(backend)
        backend.start()
        return backend
