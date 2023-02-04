import os.path
import tempfile
import socket

import pytest

from maps.infra.yacare.scripts.request import _yacare_request


def test_ping(unix_socket_path):
    status, _ = _yacare_request(unix_socket_path, '/ping')
    assert status == 200


def test_ping_with_trailing_query_args_delimiter(unix_socket_path):
    status, _ = _yacare_request(unix_socket_path, '/ping?')
    assert status == 200


def test_empty_handle(unix_socket_path):
    with pytest.raises(ValueError):
        _yacare_request(unix_socket_path, '')


def test_invalid_handle(unix_socket_path):
    with pytest.raises(ValueError):
        _yacare_request(unix_socket_path, '?key=value')


def test_query_args(unix_socket_path):
    status, stdout = _yacare_request(unix_socket_path, '/special_param_names?name=kevin&type=human&optional=false')
    assert status == 200
    assert stdout == 'name = kevin\ntype = human\noptional = 0\n'


def test_custom_http_code(unix_socket_path):
    status, _ = _yacare_request(unix_socket_path, '/custom_http_code')
    assert status == 418


def test_detach(unix_socket_path):
    status, _ = _yacare_request(unix_socket_path, '/yacare/detach', method='POST')
    assert status == 200
    status, _ = _yacare_request(unix_socket_path, '/ping')
    assert status == 503
    status, _ = _yacare_request(unix_socket_path, '/yacare/attach', method='POST')
    assert status == 200
    status, _ = _yacare_request(unix_socket_path, '/ping')
    assert status == 200


def test_loglevel(unix_socket_path):
    status, body = _yacare_request(unix_socket_path, '/yacare/loglevel')
    assert status == 200 and body == 'loglevel = debug'
    status, body = _yacare_request(unix_socket_path, '/yacare/loglevel?level=info', method='POST')
    assert status == 200 and body == 'set loglevel to info'
    status, body = _yacare_request(unix_socket_path, '/yacare/loglevel?level=debug', method='POST')
    assert status == 200 and body == 'set loglevel to debug'
    status, _ = _yacare_request(unix_socket_path, '/yacare/loglevel?level=zxc', method='POST')
    assert status == 500


def test_stacktraces(unix_socket_path):
    status, body = _yacare_request(unix_socket_path, '/yacare/stacktraces')
    assert status == 200 and body == 'not printing stacktraces'
    status, body = _yacare_request(unix_socket_path, '/yacare/stacktraces?enabled=yes', method='POST')
    assert status == 200 and body == 'will be printing stacktraces'
    status, body = _yacare_request(unix_socket_path, '/yacare/stacktraces?enabled=no', method='POST')
    assert status == 200 and body == 'will not be printing stacktraces'
    status, _ = _yacare_request(unix_socket_path, '/yacare/stacktraces?enabled=a', method='POST')
    assert status == 500


def test_custom_ping(unix_socket_path):
    status, stdout = _yacare_request(unix_socket_path, '/mtroute/ping')
    assert status == 200
    assert stdout == 'pong\n'


def test_bad_request(unix_socket_path):
    status, _ = _yacare_request(unix_socket_path, '/exception')
    assert status == 500


def test_connect_fails():
    # Connect to nonexistent unix-socket
    with pytest.raises(FileNotFoundError):
        _yacare_request('not_a.sock', '/ping')


def test_connect_refused():
    # Bind socket but do not listen for connections
    socket_path = os.path.join(tempfile.mkdtemp(), 'bad.sock')
    sock = socket.socket(socket.AF_UNIX, socket.SOCK_STREAM, 0)
    sock.bind(socket_path)

    with pytest.raises(ConnectionRefusedError):
        _yacare_request(socket_path, '/ping')


def test_receive_timeout(unix_socket_path):
    with pytest.raises(socket.timeout):
        _yacare_request(unix_socket_path, '/heavy/action')
