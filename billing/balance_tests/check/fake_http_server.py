# coding: utf-8
import logging
import sys

import bottle

from check.defaults import DATA_DIR

logger = logging.getLogger('fake_http_server')


def setup_logger():
    logger.setLevel(logging.DEBUG)

    handler = logging.StreamHandler(sys.stdout)
    handler.setLevel(logging.DEBUG)
    handler.setFormatter(logging.Formatter(
        '%(asctime)s - %(name)s - %(levelname)s - %(message)s'
    ))

    logger.addHandler(handler)


setup_logger()


class WSGIRefServer(bottle.ServerAdapter):
    """
    Код скопирован из bottle.py ради добавления этой строчки:
    app.server = srv
    """
    def run(self, app): # pragma: no cover
        from wsgiref.simple_server import WSGIRequestHandler, WSGIServer
        from wsgiref.simple_server import make_server
        import socket

        class FixedHandler(WSGIRequestHandler):
            def address_string(self): # Prevent reverse DNS lookups please.
                return self.client_address[0]
            def log_request(*args, **kw):
                if not self.quiet:
                    return WSGIRequestHandler.log_request(*args, **kw)

        handler_cls = self.options.get('handler_class', FixedHandler)
        server_cls  = self.options.get('server_class', WSGIServer)

        if ':' in self.host: # Fix wsgiref for IPv6 addresses.
            if getattr(server_cls, 'address_family') == socket.AF_INET:
                class server_cls(server_cls):
                    address_family = socket.AF_INET6

        srv = make_server(self.host, self.port, app, server_cls, handler_cls)
        app.server = srv
        srv.serve_forever()


class FakeApp(bottle.Bottle):
    server = None


FAKE_APP = FakeApp()


@FAKE_APP.route('/static/<filename>')
def static_files_handler(filename):
    logger.debug('Got request for file: %s' % filename)
    try:
        return bottle.static_file(filename, root=DATA_DIR)
    except Exception:
        logger.exception('Exception in static_files_handler (filename=%s):' % filename)
        raise


def start_server(port, host='::'):
    logger.debug('Trying to start')
    try:
        bottle.run(app=FAKE_APP, server=WSGIRefServer, port=port, host=host)
    except Exception:
        logger.exception('bottle.run failed:')
        raise
