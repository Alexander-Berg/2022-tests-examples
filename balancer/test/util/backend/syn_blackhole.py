# -*- coding: utf-8 -*-
import itertools
import socket


def create_syn_blackhole(ports, timeout=0.005):
    for host, port in itertools.product([
        '0100::2',          # rfc6666, 0100::/64
        '87.250.233.254',   # yandex specific, 87.250.233.254/32 (blackhole.yandex.net)
        '10.66.66.2',       # yandex specific, 10.66.66.0/24
        '2a02:6b8:6666::2'  # yandex specific, 2a02:6b8:6666::/64
    ], ports):
        try:
            conn = socket.create_connection((host, port), timeout=timeout)
        except socket.timeout:
            return host, port
        except:
            continue
        conn.close()
    return None, None
