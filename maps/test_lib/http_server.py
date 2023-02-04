import sys
import contextlib
import time
from socket import socket, AF_INET, SOCK_STREAM
from threading import Thread
from yatest.common.network import PortManager
from yandex.maps.test_utils.network import port_is_open
from yandex.maps.test_utils.common import wait_until

from werkzeug.serving import make_server
from werkzeug.wrappers import Request


_LOCAL_HOST = '127.0.0.1'


def _log(message):
    print(f"{time.ctime()}: {message}", file=sys.stderr, flush=True)


class MockHttpServer(Thread):
    def __init__(self, host, port, handler):
        self.host = host
        self.port = port
        self.server = None
        self.handler = handler
        super(MockHttpServer, self).__init__()

    def url(self):
        return "http://{}:{}".format(self.host, self.port)

    def run(self):
        _log("Running mock http server {} on port {}".format(self.host, self.port))

        def handler_wrapper(environ, start_response):
            request = Request(environ)
            response = self.handler(environ, start_response)
            _log(f"Handler {self.host}:{self.port} {request.method} {request.path} is called")
            return response

        self.server = make_server(host=self.host, port=self.port, app=handler_wrapper)
        self.server.serve_forever()

    def stop(self):
        if self.server:
            self.server.server_close()
            self.server.shutdown()


def find_free_port():
    """Find a free port by binding to port 0 then unbinding"""
    sock = socket(AF_INET, SOCK_STREAM)
    sock.bind(('', 0))
    free_port = sock.getsockname()[1]
    sock.close()
    return free_port


@contextlib.contextmanager
def mock_http_server(handler):
    with PortManager() as pm:
        port = pm.get_port()
        mock_server = MockHttpServer(_LOCAL_HOST, port, handler)
        mock_server.daemon = True
        mock_server.start()

        if not wait_until(lambda: port_is_open(_LOCAL_HOST, port)):
            raise RuntimeError('Could not connect to just launched ya courier server.')

        try:
            yield mock_server.url()
        finally:
            mock_server.stop()
