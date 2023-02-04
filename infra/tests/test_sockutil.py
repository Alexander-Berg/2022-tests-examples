import errno
import socket

from infra.orly.lib import sockutil


def test_create_server_socket():
    s, err = sockutil.create_server_socket('::', 0, backlog=42)
    assert err is None
    assert s.gettimeout() == 0
    assert s.family == socket.AF_INET6
    assert s.type == socket.SOCK_STREAM
    try:
        assert s.getsockopt(socket.SOL_SOCKET, socket.SO_ACCEPTCONN) == 1, "Not listening"
        assert s.getsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR) == 1, "No SO_REUSEADDR"
    except socket.error as e:
        # Not available on Mac OS
        if e.errno != errno.ENOPROTOOPT:
            raise
