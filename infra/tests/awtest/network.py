import contextlib
import socket
import os

import ipaddr
import mock
import six
from boltons import cacheutils, funcutils

from awacs import resolver


base_socket_connect = socket.socket.connect
base_getaddrinfo = socket.getaddrinfo


class SocketConnectBlockedError(RuntimeError):
    def __init__(self, allowed, host, method, *_, **__):
        if allowed:
            allowed = u', '.join(allowed)
        super(SocketConnectBlockedError, self).__init__(
            u'A test tried to use {2}() with host "{0}" (allowed hosts: {1}).'.format(host, allowed, method)
        )


def socket_allow_hosts(allowed):
    """
    adapted from https://github.com/miketheman/pytest-socket
    """

    def guarded_connect(inst, address):
        host = None
        if isinstance(address, tuple) and isinstance(address[0], (str, six.text_type)):
            host = address[0]
        if host is None or host in allowed:
            return base_socket_connect(inst, address)
        raise SocketConnectBlockedError(allowed, host, u'socket.socket.connect')

    def guarded_getaddrinfo(host, port, *args, **kwargs):
        if host is None or host in allowed:
            return base_getaddrinfo(host, port, *args, **kwargs)
        raise SocketConnectBlockedError(allowed, host, u'socket.getaddrinfo')

    socket.socket.connect = guarded_connect
    socket.getaddrinfo = guarded_getaddrinfo


def server_ip_address():
    import netifaces
    eth0 = netifaces.ifaddresses('eth0')
    return (eth0.get(netifaces.AF_INET) or eth0[netifaces.AF_INET6])[0]['addr']


# copy-paste from https://a.yandex-team.ru/arc/trunk/arcadia/contrib/python/jaeger-client/py3/jaeger_client/utils.py?rev=r9530491#L63
def local_ip():
    """Get the local network IP of this machine"""
    try:
        ip = socket.gethostbyname(socket.gethostname())
    except IOError:
        ip = socket.gethostbyname('localhost')
    if ip.startswith('127.'):
        ip = get_local_ip_by_interfaces()
        if ip is None:
            ip = get_local_ip_by_socket()
    return ip


def get_local_ip_by_socket():
    # Explanation : https://stackoverflow.com/questions/166506
    s = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
    try:
        # doesn't even have to be reachable
        s.connect(('10.255.255.255', 1))
        ip = s.getsockname()[0]
    except IOError:
        ip = None
    finally:
        s.close()
    return ip


def get_local_ip_by_interfaces():
    ip = None
    interfaces = [b'veth', b'ip6tnl0'] + [
        i + bytes(n) for i in (b'eth', b'en', b'wlan') for n in range(6)
    ]  # :(
    for interface in interfaces:
        try:
            ip = interface_ip(interface)
            if ip is not None:
                break
        except IOError:
            pass
    return ip


def interface_ip(interface):
    import netifaces
    try:
        return netifaces.ifaddresses(interface)[netifaces.AF_INET6][0]['addr']
    except:
        return None


# ///

@cacheutils.cached(cacheutils.LRU(max_size=1))
def get_local_ip_v4():
    return os.popen("ip route get 77.88.8.8 2>/dev/null| awk '/src/ {print $NF}'").readline().strip()


@cacheutils.cached(cacheutils.LRU(max_size=1))
def get_local_ip_v6():
    return os.popen("ip route get 2a00:1450:4010:c05::65 2>/dev/null | "
                    "grep -oE '2a02[:0-9a-f]+' | tail -1").readline().strip()


@cacheutils.cached(cacheutils.LRU(max_size=1))
def get_local_ip():
    rv = get_local_ip_v6() or get_local_ip_v4() or local_ip()  # or server_ip_address()
    if not rv:
        import netifaces
        for i in netifaces.interfaces():
            if i == b'lo' or i == 'lo':
                continue
            data = netifaces.ifaddresses(i)
            if netifaces.AF_INET6 in data:
                rv = data[netifaces.AF_INET6][0]['addr']
                break
    if not rv:
        message = 'ifaces {}\n'.format(netifaces.interfaces())
        for i in netifaces.interfaces():
            message += str(netifaces.ifaddresses(i)) + '\n'
        raise AssertionError(message)
    return rv


@cacheutils.cached(cacheutils.LRU(max_size=1))
def is_ipv4_only():
    return bool(get_local_ip_v4() and not get_local_ip_v6())


def is_port_open(host, port):
    with contextlib.closing(
        socket.socket(socket.AF_INET6 if is_ipv6_address(host) else socket.AF_INET, socket.SOCK_STREAM)) as sock:
        return sock.connect_ex((host, port)) == 0


def is_ipv6_address(hostname):
    try:
        ipaddr.IPv6Address(hostname)
    except ipaddr.AddressValueError:
        return False
    else:
        return True


def worker_id_to_offset(worker_id):
    if worker_id.startswith('gw'):
        return int(worker_id[2:])
    else:
        return 0


def get_free_port(host):
    family = socket.AF_INET6 if is_ipv6_address(host) else socket.AF_INET
    with contextlib.closing(socket.socket(family, socket.SOCK_STREAM)) as sock:
        sock.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
        sock.bind((host, 0))
        return sock.getsockname()[1]


def mock_resolve_host(hosts):
    base_resolve = resolver.resolve_host

    def resolve(host, *rargs, **rkwargs):
        if host in hosts:
            return hosts[host]
        return base_resolve(host, *rargs, **rkwargs)

    def outer(func):
        @funcutils.wraps(func)
        def run(*args, **kwargs):
            with mock.patch.object(resolver, u'resolve_host', resolve):
                return func(*args, **kwargs)

        return run

    return outer


@contextlib.contextmanager
def mocked_resolve_host(hosts):
    base_resolve = resolver.resolve_host

    def resolve(host, *args, **kwargs):
        if host in hosts:
            return hosts[host]
        return base_resolve(host, *args, **kwargs)

    with mock.patch.object(resolver, u'resolve_host', resolve):
        yield
