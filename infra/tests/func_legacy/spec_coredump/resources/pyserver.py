# coding=utf-8

import sys
import socket
from BaseHTTPServer import HTTPServer
from SimpleHTTPServer import SimpleHTTPRequestHandler


class HTTPServerV6(HTTPServer):
    address_family = socket.AF_INET6


def main():
    server = HTTPServerV6(('::', int(sys.argv[1])), SimpleHTTPRequestHandler)
    server.serve_forever()


if __name__ == '__main__':
    main()
