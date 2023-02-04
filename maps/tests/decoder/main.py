from BaseHTTPServer import BaseHTTPRequestHandler, HTTPServer
from sandbox.projects.maps.b2bgeo.lib.deeplink import YaCourierDeepLinkDecoder

import json
import sys
import urlparse


class Handler(BaseHTTPRequestHandler):

    def do_GET(self):
        self.send_response(200)
        self.end_headers()

    def do_POST(self):
        request = urlparse.urlparse(self.requestline.split()[1])
        yacourier = urlparse.parse_qs(request.query)['yacourier'][0]
        try:
            params = self.server.decoder.decode(yacourier)
        except Exception as ex:
            self.send_response(500)
            self.send_header("Content-type", "text/plain")
            self.end_headers()
            self.wfile.write(str(ex))
            return
        self.send_response(200)
        self.send_header("Content-type", "application/json")
        self.end_headers()
        json.dump(params, self.wfile)


class Decoder(HTTPServer):

    def __init__(self, key):
        self.decoder = YaCourierDeepLinkDecoder(key)
        port = int(sys.argv[1])
        HTTPServer.__init__(self, ('', port), Handler)


def main():
    try:
        server = Decoder(sys.argv[2])
        server.serve_forever()
    except KeyboardInterrupt:
        server.socket.close()
