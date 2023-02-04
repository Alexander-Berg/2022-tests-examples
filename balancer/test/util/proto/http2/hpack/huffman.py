# -*- coding: utf-8 -*-
import huffman_table
import balancer.test.util.stream.io.stream as stream
import balancer.test.util.stream.io.bit as bit


class DecodeError(Exception):
    pass


class Node(object):
    def __init__(self, height):
        super(Node, self).__init__()
        self.__height = height
        self.__zero = None
        self.__one = None
        self.__value = None

    @property
    def __lazy_zero(self):
        if self.__zero is None:
            self.__zero = Node(self.__height + 1)
        return self.__zero

    @property
    def __lazy_one(self):
        if self.__one is None:
            self.__one = Node(self.__height + 1)
        return self.__one

    def add_value(self, value, code, pos):
        if pos == -1:
            self.__value = value
        else:
            cur_bit = code & (1 << pos)
            if cur_bit:
                self.__lazy_one.add_value(value, code, pos - 1)
            else:
                self.__lazy_zero.add_value(value, code, pos - 1)

    def __find(self, reader, code):
        if self.__value is not None:
            return self.__value
        elif reader.has_data():
            cur_bit = reader.read_bit()
            code = (code << 1) | cur_bit
            if cur_bit:
                return self.__lazy_one.__find(reader, code)
            else:
                return self.__lazy_zero.__find(reader, code)
        else:
            if code != 2 ** self.__height - 1:
                raise DecodeError('EOS expected at the end of stream')
            return ''

    def find(self, reader):
        return self.__find(reader, 0)


def __build_tree(table):
    result = Node(0)
    for value, (code, bits) in enumerate(table):
        result.add_value(chr(value), code, bits - 1)
    return result


TREE = __build_tree(huffman_table.TABLE)


def encode(data):
    string_stream = stream.StringStream('')
    writer = bit.BitWriter(string_stream)
    for sym in data:
        value, num_bits = huffman_table.TABLE[ord(sym)]
        writer.write_int_not_aligned(value, num_bits)
    if writer.buffer_length != 0:
        eos_len = bit.BYTE_LENGTH - writer.buffer_length
        writer.write_int(2 ** eos_len - 1, eos_len)
    return str(string_stream)


def decode(data):
    string_stream = stream.StringStream(data)
    reader = bit.BitReader(string_stream)
    result = list()
    sym = TREE.find(reader)
    while sym:
        result.append(sym)
        sym = TREE.find(reader)
    return ''.join(result)
