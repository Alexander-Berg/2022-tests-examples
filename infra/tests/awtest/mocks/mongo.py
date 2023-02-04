# coding: utf-8
import contextlib
import errno
import shutil
import socket
import subprocess
import tempfile
import time
import six


_START_TIMEOUT = 30
_POLLING_INTERVAL = 0.01
_TERMINATION_TIMEOUT = 10


class MongoDb(object):
    """Mocks MongoDB database."""

    def __init__(self):
        self.connected = False
        self.empty = True
        self.dirty = False

        self._host = 'localhost'
        self._port = self._get_free_port()
        self._db_path = None
        self._output = None
        self._process = None

        try:
            self._start()
        except:
            self.kill()
            raise

    @property
    def host(self):
        return '{}:{}'.format(self._host, self._port)

    def kill(self):
        if self._process is not None:
            self._kill_process()

        if self._output is not None:
            self._output.close()
            self._output = None

        if self._db_path is not None:
            shutil.rmtree(self._db_path)
            self._db_path = None

    def _kill_process(self):
        terminate_timeout = time.time() + _TERMINATION_TIMEOUT

        while self._process.poll() is None:
            timed_out = time.time() >= terminate_timeout

            try:
                if timed_out:
                    self._process.kill()
                else:
                    self._process.terminate()
            except EnvironmentError as e:
                if e.errno == errno.ESRCH:
                    break
                else:
                    raise
            else:
                time.sleep(_POLLING_INTERVAL)

        self._process = None

    def get_mongod_version(self):
        output = tempfile.NamedTemporaryFile()
        args = ['mongod', '--version']
        process = subprocess.Popen(args, stdout=output)
        while process.poll() is None:
            time.sleep(_POLLING_INTERVAL)
        with open(output.name) as f:
            v = f.readline().split()
        return v[-1] if v else ''

    def _start(self):
        self._db_path = tempfile.mkdtemp(prefix='mongodb-mock-', dir='/var/tmp')
        if six.PY2:
            self._output = tempfile.NamedTemporaryFile(bufsize=0)
        else:
            self._output = tempfile.NamedTemporaryFile(buffering=0)

        args = ['mongod', '--nojournal', '--dbpath', self._db_path, '--port', str(self._port)]
        if not self.get_mongod_version().startswith('v3.6') and not self.get_mongod_version().startswith('v4.'):
            # Deprecated in 3.6
            args.append('--nohttpinterface')
        if not self.get_mongod_version().startswith('v4.'):
            args.append('--noprealloc')

        self._process = subprocess.Popen(
            args, stdout=self._output, stderr=self._output)

        self._wait_for_start()

    def _wait_for_start(self):
        start_timeout = time.time() + _START_TIMEOUT

        while self._process.poll() is None and time.time() < start_timeout:
            try:
                socket.create_connection((self._host, self._port)).close()
            except socket.error:
                time.sleep(_POLLING_INTERVAL)
            else:
                break
        else:
            if self._process.poll() is None:
                raise Exception(
                    'Unable to connect to the MongoDB server. '
                    'MongoDB output:\n' + self._get_output())
            else:
                raise Exception('Failed to start MongoDB:\n' + self._get_output())

    def _get_free_port(self):
        with contextlib.closing(socket.socket(socket.AF_INET, socket.SOCK_STREAM)) as sock:
            sock.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
            sock.bind((self._host, 0))
            return sock.getsockname()[1]

    def _get_output(self):
        with open(self._output.name) as output:
            return output.read()
