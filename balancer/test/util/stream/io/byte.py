# -*- coding: utf-8 -*-
import balancer.test.util.stream.io.stream as s


class ByteReader(object):
    def __init__(self, stream, newline='\r\n'):
        super(ByteReader, self).__init__()
        self.__stream = stream
        if isinstance(newline, list):
            self.newline = newline
        else:
            self.newline = [newline]
        self.__max_newline = max([len(nl) for nl in self.newline])

    @property
    def sock(self):
        return self.__stream

    def read(self, size=-1):
        return self.__stream.recv(size)

    def read_line(self):
        result = list()
        newline = None
        while newline is None:
            try:
                data = self.__stream.recv(1)
                result.append(data)
                newline = self.__endswith_newline(result)
            except s.EndOfStream, exc:
                raise s.EndOfStream(''.join(result), exc.message)
            except s.StreamTimeout, exc:
                raise s.StreamTimeout(''.join(result), exc.message)
        return ''.join(result)[:-len(newline)]

    def __endswith_newline(self, chars):
        tail = ''.join(chars[-self.__max_newline:])
        for nl in self.newline:
            if tail.endswith(nl):
                return nl
        return None

    def set_timeout(self, timeout):
        self.__stream.set_timeout(timeout)


class ByteWriter(object):
    def __init__(self, stream, newline='\r\n'):
        super(ByteWriter, self).__init__()
        self.__stream = stream
        self.newline = newline

    def write(self, data):
        return self.__stream.send(data)

    def write_line(self, data):
        self.write(data + self.newline)

    def set_timeout(self, timeout):
        self.__stream.set_timeout(timeout)
