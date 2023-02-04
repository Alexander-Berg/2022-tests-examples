import BaseHTTPServer
import argparse
import socket

from infra.yp_service_discovery.api.api_pb2 import TRspResolveEndpoints, TReqResolveEndpoints


class Handler(BaseHTTPServer.BaseHTTPRequestHandler):
    protocol_version = "HTTP/1.0"

    def _set_headers(self, n):
        self.send_response(200)
        self.send_header('Content-Length', str(n))
        self.send_header('Access-Control-Allow-Origin', '*')
        self.end_headers()

    def do_GET(self):
        self._set_headers(0)
        self.wfile.write('')

    def _fill_endpoints(self, endpoints_pb):
        raise NotImplementedError

    def do_POST(self):
        content_len = int(self.headers.getheader('content-length', 0))
        req_pb = TReqResolveEndpoints()
        req_pb.MergeFromString(self.rfile.read(content_len))
        resp_pb = TRspResolveEndpoints(timestamp=1674122245607260460)
        resp_pb.endpoint_set.endpoint_set_id = req_pb.endpoint_set_id
        self._fill_endpoints(resp_pb.endpoint_set.endpoints)
        resp = resp_pb.SerializeToString()
        self._set_headers(len(resp))
        self.wfile.write(resp)


class CommonHandler(Handler):
    def _fill_endpoints(self, endpoints_pb):
        endpoints_pb.add(
            id='xxx',
            protocol='TCP',
            fqdn='xxx.vla.yp-c.yandex.net',
            ip6_address='2a02:6b8:c0f:180d:10d:bd77:57eb:0',
            port=3388
        )


class HTTPServer6(BaseHTTPServer.HTTPServer):
    address_family = socket.AF_INET6


def run(port=8080):
    parser = argparse.ArgumentParser(description='SD stub')
    parser.add_argument('port', type=int)
    args = parser.parse_args()

    handler = CommonHandler
    httpd = HTTPServer6(('', args.port), handler)
    print('OK')
    httpd.serve_forever()


if __name__ == "__main__":
    run()
