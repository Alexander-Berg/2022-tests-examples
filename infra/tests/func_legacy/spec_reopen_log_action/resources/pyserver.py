import sys
import socket
from BaseHTTPServer import HTTPServer
from SimpleHTTPServer import SimpleHTTPRequestHandler


class HTTPServerV6(HTTPServer):
    address_family = socket.AF_INET6


class Handler(SimpleHTTPRequestHandler):

    def do_GET(self):
        self.send_response(200)
        open('reopened.txt', 'w').close()


def main():
    server = HTTPServerV6(('::', int(sys.argv[1])), Handler)
    server.serve_forever()


if __name__ == '__main__':
    main()
