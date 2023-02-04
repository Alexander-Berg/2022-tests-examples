# -*- coding: utf-8 -*-

"""
DNS server for functional tests. Based on https://pypi.org/project/dnslib/.
"""

from server import DNSServer
from zoneresolver import ZoneResolver


class DnsFakeManager(object):
    def __init__(self):
        self._resolver = None
        self._logger = None
        self._udp_server = None
        self._records = None

    def start(self, port, ip, resolve_ipv4=None, resolve_ipv6=None, delay_sec=0):
        if resolve_ipv4 is not None:
            assert(resolve_ipv6 is None)
            self._records = "pbcznloiqpakow2g.man.yp-c.yandex.net. 60 IN A " + resolve_ipv4
        elif resolve_ipv6 is not None:
            assert(resolve_ipv4 is None)
            self._records = "pbcznloiqpakow2g.man.yp-c.yandex.net. 60 IN AAAA " + resolve_ipv6
        else:
            self._records = "pbcznloiqpakow2g.man.yp-c.yandex.net. 60 IN A 127.0.0.1\r\npbcznloiqpakow2g.man.yp-c.yandex.net. 60 IN A 127.0.0.2\r\npbcznloiqpakow2g.man.yp-c.yandex.net. 60 IN AAAA ::1"

        self._resolver = ZoneResolver(self._records, delay_sec)
        self._udp_server = DNSServer(self._resolver, port=port, address=ip, logger=self._logger)

        self._udp_server.start_thread()

    def stop(self):
        self._udp_server.stop()

    def is_alive(self):
        return self._udp_server.isAlive()
