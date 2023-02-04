import BaseHTTPServer
import threading


class HttpServer(object):
    """
    Represents a local HTTP-server running on a separate thread
    """
    def __init__(self, port, handler_cls):
        self._srv = BaseHTTPServer.HTTPServer(('127.0.0.1', port), handler_cls)

    def start(self):
        def _start():
            self._srv.serve_forever()

        self._thread = threading.Thread(target=_start)
        self._thread.start()

    def stop(self):
        self._srv.shutdown()

        if self._thread.isAlive():
            self._thread.join()


class HttpHandler(BaseHTTPServer.BaseHTTPRequestHandler):
    def log_message(format, *args, **kwargs):
        pass
