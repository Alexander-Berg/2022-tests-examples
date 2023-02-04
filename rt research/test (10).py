from contextlib import closing
import requests
import socket
from threading import Thread

from irt.utils import get_version

from irt.multik.liveness_server import LivenessServer


def find_free_port():
    with closing(socket.socket(socket.AF_INET, socket.SOCK_STREAM)) as s:
        s.bind(('', 0))
        s.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
        return s.getsockname()[1]


def test_liveness_server():
    port = find_free_port()
    server = LivenessServer(port=port)
    th = Thread(target=server.run)
    th.daemon = True
    th.start()

    assert requests.get('http://localhost:{}/alive'.format(port)).json() == {"alive": "ok"}
    assert requests.get('http://localhost:{}/ping'.format(port)).json() == {"ping": "pong"}
    assert requests.get('http://localhost:{}/status'.format(port)).json() == {"alive": "ok", "version": get_version()}
    assert requests.get('http://localhost:{}/version'.format(port)).json() == {"version": get_version()}

    assert requests.get('http://localhost:{}/not_found'.format(port)).json() == {
        "code": "404",
        "message": "Not Found",
        "explain": "Nothing matches the given URI"
    }

    server.shutdown()
