import subprocess
import os
import socket
from contextlib import contextmanager
from inspect import currentframe, getframeinfo
from pathlib import Path

from flow_runner.connection import Connection


@contextmanager
def run_app() -> Connection:
    cur_filename = getframeinfo(currentframe()).filename
    cur_folder = Path(cur_filename).resolve().parent
    sock_path = str(cur_folder / 'socket.sock')
    os.environ['SOCKET_PATH'] = sock_path

    sock = socket.socket(socket.AF_UNIX, socket.SOCK_STREAM)
    sock.bind(sock_path)
    sock.listen(1)
    try:
        with subprocess.Popen(['python3', str(cur_folder.parent / 'main.py')]) as proc:
            connection = Connection(sock.accept()[0], autoconnect=False)
            try:
                yield connection, proc
            finally:
                connection.send_data({'type': 'stop_container'})
                code, resp = connection.get_data()
                assert code == 0
                assert resp == {'status': 'closing_app'}
                connection.close()
    finally:
        os.unlink(sock_path)
