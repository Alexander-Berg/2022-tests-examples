#!/usr/bin/env python
from __future__ import absolute_import

import gevent.monkey


gevent.monkey.patch_all()

import sys
import signal
import time
import socket
import argparse
import six.moves.BaseHTTPServer


try:
    from .pb import api_pb2
except ValueError:  # ValueError: Attempted relative import in non-package
    from infra.awacs.vendor.awacs.tests.awtest.mocks.sdstub_app.proto import api_pb2

LOG_FILE = None


class Handler(six.moves.BaseHTTPServer.BaseHTTPRequestHandler):
    protocol_version = "HTTP/1.0"
    IPV4 = False
    HTTPBIN_HOST = 'localhost'
    HTTPBIN_PORT = 22222

    def _set_headers(self, n):
        self.send_response(200)
        self.send_header('Content-Length', str(n))
        self.send_header('Access-Control-Allow-Origin', '*')
        self.end_headers()

    def do_GET(self):
        self._set_headers(0)
        self.wfile.write('')

    def do_POST(self):
        content_len = int(self.headers.getheader('content-length', 0))
        req_pb = api_pb2.TReqResolveEndpoints()
        req_pb.MergeFromString(self.rfile.read(content_len))
        resp_pb = api_pb2.TRspResolveEndpoints(timestamp=int(time.time() * (10 ** 9)))
        resp_pb.endpoint_set.endpoint_set_id = req_pb.endpoint_set_id
        self._fill_endpoints(resp_pb.endpoint_set.endpoints)
        resp_pb.resolve_status = api_pb2.OK
        resp = resp_pb.SerializeToString()
        self._set_headers(len(resp))
        self.wfile.write(resp)

    def _fill_endpoints(self, endpoints_pb):
        endpoint_pb = endpoints_pb.add(
            id='httpbin',
            protocol='TCP',
            fqdn=self.HTTPBIN_HOST,
            port=self.HTTPBIN_PORT,
            ready=True
        )
        if self.IPV4:
            endpoint_pb.ip4_address = '127.0.0.1'
        else:
            endpoint_pb.ip6_address = '::1'


class HTTPServer6(six.moves.BaseHTTPServer.HTTPServer):
    address_family = socket.AF_INET6


def run():
    def handler(signum, frame):
        sys.exit(0)

    signal.signal(signal.SIGINT, handler)
    signal.signal(signal.SIGTERM, handler)

    global LOG_FILE
    parser = argparse.ArgumentParser(description='SD stub')
    parser.add_argument('--ipv4', action='store_true')
    parser.add_argument('--log-to-stdout', action='store_true')
    parser.add_argument('--port', type=int)
    parser.add_argument('--httpbin-port', type=int)
    args = parser.parse_args()
    if args.log_to_stdout:
        LOG_FILE = sys.stdout
    else:
        LOG_FILE = open('/tmp/sdstub.out-{}'.format(time.time()), 'wb', 0)
    Handler.IPV4 = args.ipv4
    Handler.HTTPBIN_PORT = args.httpbin_port
    httpd = HTTPServer6(('', args.port), Handler)
    print('OK')
    httpd.serve_forever()


if __name__ == "__main__":
    run()
