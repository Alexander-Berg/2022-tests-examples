# -*- coding: utf-8 -*-
from abc import ABCMeta, abstractmethod
import os
import struct
import socket
import select
import errno
import time
import StringIO
import OpenSSL
from balancer.test.util.process import ProcessException
from balancer.test.util.resource import AbstractResource


class StreamException(AssertionError):
    def __init__(self, data, *args, **kwargs):
        super(StreamException, self).__init__(*args, **kwargs)
        self.data = data


class StreamRecvException(StreamException):
    pass


class EndOfStream(StreamRecvException):
    pass


class StreamTimeout(StreamRecvException):
    pass


class StreamRst(StreamRecvException):
    pass


class NonEmptyStream(StreamException):
    pass


class Stream(AbstractResource):
    __metaclass__ = ABCMeta

    def __init__(self):
        super(Stream, self).__init__()
        self.__data = list()

    close = AbstractResource.finish

    @abstractmethod
    def _finish(self):
        raise NotImplementedError()

    def clean(self):
        self.__data = list()

    @property
    def data(self):
        if len(self.__data) != 1:
            self.__data = [''.join(self.__data)]
        return self.__data[0]

    def recv(self, size=-1):
        try:
            result = self._recv(size)
            self.__data.append(result)
            # noinspection PyChainedComparisons
            if size > 0 and len(result) != size:
                raise EndOfStream(''.join(result))
            else:
                return result
        except StreamRecvException, exc:
            self.__data.append(exc.data)
            raise

    @abstractmethod
    def _recv(self, size=-1):
        raise NotImplementedError()

    def recv_quiet(self, size=-1):
        try:
            return self.recv(size)
        except StreamRecvException, exc:
            return exc.data

    @abstractmethod
    def has_data(self):
        raise NotImplementedError()


class FileStream(Stream):

    def __init__(self, file_obj):
        super(FileStream, self).__init__()
        self.__file = file_obj

    def _recv(self, size=-1):
        return self.__file.read(size)

    def has_data(self):
        return self.__file.tell() < os.fstat(self.__file.fileno()).st_size

    def _finish(self):
        self.__file.close()


class StringStream(Stream):

    def __init__(self, s):
        super(StringStream, self).__init__()
        self.__string = StringIO.StringIO(s)

    def _recv(self, size=-1):
        return self.__string.read(size)

    def has_data(self):
        return self.__string.tell() < self.__string.len

    def send(self, data):
        self.__string.write(data)

    def _finish(self):
        pass

    def __str__(self):
        return self.__string.getvalue()


class _SocketStream(Stream):
    CHECK_TIMEOUT = 0.5
    BUF_SIZE = 8192
    MILLISECONDS = 1000
    TIMEOUT = 20

    def __init__(self, sock):
        super(_SocketStream, self).__init__()
        self._socket = sock
        self.__poll = select.poll()
        self.__timeout = self._socket.gettimeout()
        self.__poll.register(self._socket.fileno(), select.POLLIN | select.POLLHUP)

    def fileno(self):
        return self._socket.fileno()

    def set_timeout(self, timeout):
        self._socket.settimeout(timeout)

    def restore_timeout(self):
        self._socket.settimeout(self.__timeout)

    def shutdown(self, how):
        self._socket.shutdown(how)

    def _finish(self):
        self._socket.close()

    def is_closed(self, timeout=None):
        if timeout is None:
            timeout = self.CHECK_TIMEOUT
        fds = self.__poll.poll(timeout * self.MILLISECONDS)
        if fds:
            try:
                data = self._socket.recv(self.BUF_SIZE, socket.MSG_PEEK)
                if data:
                    raise NonEmptyStream(data, 'Socket not empty')
                else:
                    return True
            except socket.error, err:
                if err.errno == errno.ECONNRESET:
                    return True
                else:
                    raise
        else:
            return False

    def has_data(self):
        fds = self.__poll.poll(self.CHECK_TIMEOUT * self.MILLISECONDS)
        if fds:
            data = self._socket.recv(self.BUF_SIZE, socket.MSG_PEEK)
            return data != ''
        else:
            return False

    def poll(self, timeout=None):
        fds = self.__poll.poll(timeout * self.MILLISECONDS)
        return fds != []

    def send(self, data):
        self._socket.sendall(data)

    def _recv(self, size=-1):
        result = list()
        try:
            if size < 0:
                while True:
                    data = self._socket.recv(self.BUF_SIZE)
                    if not data:
                        break
                    result.append(data)
            else:
                while size > 0:
                    data = self._socket.recv(min(size, self.BUF_SIZE))
                    size -= len(data)
                    if not data:
                        break
                    result.append(data)
        except socket.timeout, exc:
            raise StreamTimeout(''.join(result), exc.message)
        except socket.error, err:
            if err.errno == errno.ECONNRESET:
                raise StreamRst(''.join(result), err.message)
            else:
                raise
        return ''.join(result)


class SocketStream(_SocketStream):

    @classmethod
    def from_address(cls, host, port, timeout=None, source_address=None):
        if timeout is None:
            timeout = cls.TIMEOUT
        sock = socket.create_connection(
            (host, port),
            timeout=timeout,
            source_address=source_address,
        )
        return SocketStream(sock)

    def set_recv_buffer_size(self, buffer_size):
        self._socket.setsockopt(socket.SOL_SOCKET, socket.SO_RCVBUF, buffer_size)

    def set_send_buffer_size(self, buffer_size):
        self._socket.setsockopt(socket.SOL_SOCKET, socket.SO_SNDBUF, buffer_size)

    @property
    def sock_ip(self):
        return self._socket.getsockname()[0]

    @property
    def sock_port(self):
        return self._socket.getsockname()[1]

    def send_rst(self):
        self._socket.setsockopt(socket.SOL_SOCKET, socket.SO_LINGER, struct.pack('ii', 1, 0))
        self.close()
        time.sleep(0.5)


class PySslStream(_SocketStream):
    def __init__(self, sock, ssl_conn):
        super(PySslStream, self).__init__(sock)
        self.__ssl_conn = ssl_conn

    def send(self, data):
        while data:
            cnt = self.__ssl_send(data)
            data = data[cnt:]

    def is_closed(self, timeout=None):
        try:
            if timeout is None:
                timeout = self.CHECK_TIMEOUT
            self.set_timeout(timeout)
            data = self.__ssl_recv(self.BUF_SIZE, socket.MSG_PEEK)
            if data:
                raise NonEmptyStream(data, 'Socket not empty')
            else:
                return True
        except OpenSSL.SSL.ZeroReturnError:
            return True
        except OpenSSL.SSL.Error:
            return False
        finally:
            self.restore_timeout()

    def _recv(self, size=-1):
        result = list()
        try:
            if size < 0:
                while True:
                    data = self.__ssl_recv(self.BUF_SIZE)
                    if not data:
                        break
                    result.append(data)
            else:
                data = '1'
                while size > 0 and data:
                    data = self.__ssl_recv(min(size, self.BUF_SIZE))
                    size -= len(data)
                    result.append(data)
        except socket.timeout, exc:
            raise StreamTimeout(''.join(result), exc.message)
        except socket.error, err:
            if err.errno == errno.ECONNRESET:
                raise StreamRst(''.join(result), err.message)
            else:
                raise
        return ''.join(result)

    def do_handshake(self):
        while True:
            try:
                self.__ssl_conn.do_handshake()
                return
            except OpenSSL.SSL.WantReadError:
                self.__bio_write()
            except OpenSSL.SSL.WantWriteError:
                self.__bio_read()

    def __ssl_send(self, data):
        while True:
            try:
                cnt = self.__ssl_conn.send(data)
                self.__bio_read()
                return cnt
            except OpenSSL.SSL.WantReadError:
                self.__bio_write()
            except OpenSSL.SSL.WantWriteError:
                self.__bio_read()

    def __ssl_recv(self, size, flags=0):
        while True:
            try:
                return self.__ssl_conn.recv(size, flags)
            except OpenSSL.SSL.WantReadError:
                self.__bio_write()
            except OpenSSL.SSL.WantWriteError:
                self.__bio_read()

    def __bio_read(self, from_write=False):
        raw_data = '1'
        while raw_data:
            try:
                raw_data = self.__ssl_conn.bio_read(self.BUF_SIZE)
                super(PySslStream, self).send(raw_data)
            except OpenSSL.SSL.WantReadError:
                return

    def __bio_write(self, from_read=False):
        if not from_read:
            self.__bio_read(from_write=True)
        raw_data = self._socket.recv(self.BUF_SIZE)
        self.__ssl_conn.bio_write(raw_data)


class ProcessStream(_SocketStream):
    def __init__(self, cmd, process_manager, multiprocess=True, timeout=None, conn_timeout=None):  # FIXME: pass running process
        proc_sock, self_sock = socket.socketpair()
        proc_sock.settimeout(self.TIMEOUT)
        self_sock.settimeout(conn_timeout or self.TIMEOUT)
        try:
            self.__process = process_manager.start(cmd=cmd, stdin=proc_sock.fileno(), stdout=proc_sock.fileno(),
                                                   multiprocess=multiprocess, timeout=timeout)
        except ProcessException:
            self_sock.close()
            raise socket.error(errno.ECONNREFUSED, '')
        finally:
            proc_sock.close()

        super(ProcessStream, self).__init__(self_sock)

    def is_closed(self, timeout=None):
        if timeout is None:
            timeout = self.CHECK_TIMEOUT
        time.sleep(timeout)
        return not self.__process.is_alive()

    def _finish(self):
        super(ProcessStream, self)._finish()
        try:
            self.__process.stop()
        except OSError:
            pass
