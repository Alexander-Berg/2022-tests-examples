# -*- coding: utf-8 -*-
import struct


BYTE_LENGTH = 8
INT_SIZE = 4


class BitIOException(Exception):
    pass


class BitReader(object):

    def __init__(self, sock):
        super(BitReader, self).__init__()
        self.__sock = sock
        self.__buffer = 0
        self.__buffer_length = 0

    def read_int(self, num_bits=32):
        """
        Allowed:
            values with num_bits < 8 within a single octet:
                buffer_length = 5, num_bits = 3, value = 5: |___101??|
            right-aligned values with num_bits >= 8:
                buffer_length = 7, num_bits = 15, value = 2 ** 14 + 1: |_1000000|00000001|
        Not allowed:
            values with num_bits < 8 not within a single octet:
                buffer_length = 2, num_bits = 5, error: |______10|011?????|
            not aligned to right values with num_bits >= 8:
                buffer_length = 8, num_bits = 15, error: |10000000|0000001?|
        """
        if num_bits < BYTE_LENGTH:
            if self.__buffer_length == 0:
                self.__buffer = struct.unpack('!B', self.__sock.recv(1))[0]
                self.__buffer_length = BYTE_LENGTH
            if num_bits > self.__buffer_length:
                raise BitIOException('not within a single octet')
            tail_length = self.__buffer_length - num_bits
            mask = (2 ** num_bits - 1) << tail_length
            result = (self.__buffer & mask) >> tail_length
            self.__buffer &= 2 ** tail_length - 1
            self.__buffer_length = tail_length
            return result
        else:
            if (num_bits - self.__buffer_length) % BYTE_LENGTH != 0:
                raise BitIOException('data not aligned to right')
            if num_bits > 32:  # TODO: num_bits == 64
                raise BitIOException('num_bits must not be greater than 32')
            if self.__buffer_length > 0:
                buf_prefix = struct.pack('!B', self.__buffer)
                self.__buffer = 0
                self.__buffer_length = 0
            else:
                buf_prefix = ''
            recv_len = (num_bits - self.__buffer_length) / BYTE_LENGTH
            data = self.__sock.recv(recv_len)
            data = buf_prefix + data
            data = '\x00' * (INT_SIZE - len(data)) + data
            return struct.unpack('!I', data)[0]

    def read_bit(self):
        return self.read_int(num_bits=1)

    def read_bytes(self, num_bytes):
        if self.__buffer_length != 0:
            raise BitIOException('data not aligned')
        if num_bytes == 0:
            return ''
        else:
            return self.__sock.recv(num_bytes)

    def next_int(self, num_bits=32):
        return self.read_int(num_bits=num_bits)

    def buffer_is_empty(self):
        return self.__buffer_length == 0

    def has_data(self):
        return self.__buffer_length != 0 or self.__sock.has_data()


class BitWriter(object):
    def __init__(self, sock):
        super(BitWriter, self).__init__()
        self.__sock = sock
        self.__buffer = 0
        self.__buffer_length = 0

    @property
    def buffer_length(self):
        return self.__buffer_length

    def __write_small_int(self, value, num_bits):
        self.__buffer_length += num_bits
        self.__buffer |= value << (BYTE_LENGTH - self.__buffer_length)
        if self.__buffer_length == BYTE_LENGTH:
            self.__sock.send(struct.pack('!B', self.__buffer))
            self.__buffer = 0
            self.__buffer_length = 0

    def write_int(self, value, num_bits=32):
        """
        Allowed:
            values with num_bits < 8 within a single octet:
                buffer_length = 3, num_bits = 3, value = 5: |???101__|
            right-aligned values with num_bits >= 8:
                buffer_length = 1, num_bits = 15, value = 2 ** 14 + 1: |?1000000|00000001|
        Not allowed:
            values with num_bits < 8 not within a single octet:
                buffer_length = 6, num_bits = 5, error: |??????10|011_____|
            not aligned to right values with num_bits >= 8:
                buffer_length = 0, num_bits = 15, error: |10000000|0000001_|
        """
        if value & (2 ** num_bits - 1) != value:
            raise BitIOException('value {} has more than {} significant bits'.format(value, num_bits))
        if num_bits < BYTE_LENGTH:
            if num_bits + self.__buffer_length > BYTE_LENGTH:
                raise BitIOException('not within a single octet')
            self.__write_small_int(value, num_bits)
        else:
            if (num_bits + self.__buffer_length) % BYTE_LENGTH != 0:
                raise BitIOException('not aligned to right')
            if num_bits > 32:  # TODO: num_bits == 64
                raise BitIOException('num_bits must not be greater than 32')
            data = struct.pack('!I', value)
            full_bytes = num_bits / BYTE_LENGTH
            if num_bits % BYTE_LENGTH != 0:
                prefix = struct.unpack('!B', data[-full_bytes - 1])[0]
                self.__write_small_int(prefix, num_bits % BYTE_LENGTH)
            self.__sock.send(data[-full_bytes:])

    def write_int_not_aligned(self, value, num_bits=32):
        if self.__buffer_length + num_bits < BYTE_LENGTH:
            self.write_int(value, num_bits)
        else:
            suffix_len = (self.__buffer_length + num_bits) % BYTE_LENGTH
            self.write_int(value >> suffix_len, num_bits - suffix_len)
            if suffix_len != 0:
                self.write_int(value & (2 ** suffix_len - 1), suffix_len)

    def write_bit(self, value):
        self.write_int(value, num_bits=1)

    def write_bytes(self, data):
        if self.__buffer_length != 0:
            raise BitIOException('data not aligned')
        self.__sock.send(data)
