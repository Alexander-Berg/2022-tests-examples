#!/usr/bin/env python
from __future__ import absolute_import  # noqa

import gevent.monkey  # noqa

gevent.monkey.patch_all()  # noqa

import sys
import signal
import time
import argparse

from six.moves import BaseHTTPServer, StringIO
from gevent import pywsgi
from infra.swatlib.webserver import WebServer
from httpbin.core import app as httpbin_app, Response, request


HTTPBIN_HOST = 'localhost'
HTTPBIN_IP_V4 = '0.0.0.0'
HTTPBIN_IP_V6 = '::1'

LOG_FILE = None


class HTTPRequest(BaseHTTPServer.BaseHTTPRequestHandler):
    def __init__(self, request_text):
        self.rfile = StringIO(request_text)
        self.raw_requestline = self.rfile.readline()
        self.error_code = self.error_message = None
        self.parse_request()

    def send_error(self, code, message):
        self.error_code = code
        self.error_message = message


@httpbin_app.route('/fullreq', methods=["GET", "POST"])
def showcaptcha_page():
    LOG_FILE.write('/fullreq data: {!r}\n'.format(request.data))

    request_headers = dict(HTTPRequest(request.data).headers.items())
    if 'z-i-swear-i-am-not-a-robot' in request_headers:
        return Response(status=200, headers={'X-ForwardToUser-Y': '0'})

    message = 'CAPTCHA' if 'IAMROBOT' in request.data else 'NONCAPTCHA'
    response_headers = {}
    for name, value in request_headers.items():
        # we copy only "interesting" headers to avoid copying content-length, encoding and other headers that can
        # confuse our client
        if any(infix in name.lower() for infix in ('x-forwarded-for-y', 'yandex', 'icookie', 'antirobot')):
            response_headers[name] = value
    response_headers['X-ForwardToUser-Y'] = '1'
    return Response(message, status=200, headers=response_headers)


class WSGIHandler(pywsgi.WSGIHandler):
    def process_result(self):
        self.result = list(self.result)
        LOG_FILE.write('==REQUEST:\n')
        pywsgi.WSGIHandler.log_request(self)
        LOG_FILE.write('==RESPONSE:\n')
        LOG_FILE.write(''.join(self.result) + '\n\n\n')
        pywsgi.WSGIHandler.process_result(self)

    def log_request(self):
        pass


class WSGIServer(pywsgi.WSGIServer):
    handler_class = WSGIHandler

    def get_environ(self):
        # This is a non-standard flag indicating that our input stream is
        # self-terminated (returns EOF when consumed).
        # See https://github.com/gevent/gevent/issues/1308
        env = pywsgi.WSGIServer.get_environ(self)
        env['wsgi.input_terminated'] = True
        # To keep httpbin happy
        # (https://github.com/postmanlabs/httpbin/blob/master/httpbin/core.py#L204):
        env['SERVER_SOFTWARE'] = 'gunicorn/lapapam'
        return env


def run():
    def handler(signum, frame):
        sys.exit(0)

    signal.signal(signal.SIGINT, handler)
    signal.signal(signal.SIGTERM, handler)

    global LOG_FILE
    parser = argparse.ArgumentParser(description='httpbin')
    parser.add_argument('--ipv4', action='store_true')
    parser.add_argument('--log-to-stdout', action='store_true')
    parser.add_argument('--port', type=int)
    args = parser.parse_args()
    if args.log_to_stdout:
        LOG_FILE = sys.stdout
    else:
        LOG_FILE = open('/tmp/httpbin.out-{}'.format(time.time()), 'wb', 0)

    if args.ipv4:
        host = HTTPBIN_IP_V4
    else:
        host = HTTPBIN_IP_V6
    listener = WebServer._create_server_socket(host, args.port)
    w = WSGIServer(listener=listener, application=httpbin_app,
                   log=LOG_FILE, error_log=LOG_FILE)
    print('OK')
    w.serve_forever()


if __name__ == "__main__":
    run()
