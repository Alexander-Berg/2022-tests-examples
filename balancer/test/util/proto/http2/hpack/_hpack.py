# -*- coding: utf-8 -*-
import struct
import balancer.test.util.proto.http2.message as mod_msg
import balancer.test.util.proto.http2.hpack.huffman as huffman


def encode_byte(value):
    return struct.pack('!B', value)


def encode_int(n, prefix, value, extra_bytes=0):
    prefix = prefix << n
    max_single_value = 2 ** n - 1
    if value < max_single_value:
        return encode_byte(value | prefix)
    else:
        result = [encode_byte(max_single_value | prefix)]
        value -= max_single_value
        while value >= 128:
            result.append(encode_byte(value % 128 + 128))
            value = value / 128
        if extra_bytes > 0:
            result.append(encode_byte(value + 128))
            result.extend([encode_byte(128)] * (extra_bytes - 1))
            result.append(encode_byte(0))
        else:
            result.append(encode_byte(value))
        return ''.join(result)


def decode_byte(octet):
    return struct.unpack('!B', octet)[0]


def decode_int(n, octets):
    max_single_value = 2 ** n - 1
    first_byte = decode_byte(octets[0])
    prefix = first_byte >> n
    value = first_byte & max_single_value
    if value < max_single_value:
        return prefix, value, octets[1:]
    else:
        power = 0
        i = 1
        fin_flag = False
        while not fin_flag:
            b = decode_byte(octets[i])
            value += (b & 127) * 2 ** power
            power += 7
            fin_flag = b & 128 == 0
            i += 1
        return prefix, value, octets[i:]


class SubTable(object):
    def __init__(self, data, max_size=None):
        super(SubTable, self).__init__()
        self.__data = data
        self.__size = sum([self.__entry_size(name, value) for name, value in data])
        self.__max_size = max_size
        self.__fit_max_size()

    def add(self, name, value):
        self.__data.insert(0, (name, value))
        self.__size += self.__entry_size(name, value)
        self.__fit_max_size()

    def set_max_size(self, max_size):
        self.__max_size = max_size
        self.__fit_max_size()

    def find(self, name, value=None):
        def check_value(v):
            if value is not None:
                return v == value
            else:
                return True

        for index, (n, v) in enumerate(self.__data):
            if n == name and check_value(v):
                return index
        return None

    def __contains__(self, key):
        if isinstance(key, tuple):
            name, value = key
            return self.find(name, value) is not None
        else:
            return self.find(key) is not None

    def __getitem__(self, index):
        return self.__data[index]

    def __len__(self):
        return len(self.__data)

    def __fit_max_size(self):
        if self.__max_size is None:
            return
        while self.__size > self.__max_size:
            name, value = self.__data.pop()
            self.__size -= self.__entry_size(name, value)

    @staticmethod
    def __entry_size(name, value):
        return len(name) + len(value) + 32  # magic constant from rfc

    def items(self):
        return self.__data


STATIC_TABLE = SubTable([
    (':authority', ''),
    (':method', 'GET'),
    (':method', 'POST'),
    (':path', '/'),
    (':path', '/index.html'),
    (':scheme', 'http'),
    (':scheme', 'https'),
    (':status', '200'),
    (':status', '204'),
    (':status', '206'),
    (':status', '304'),
    (':status', '400'),
    (':status', '404'),
    (':status', '500'),
    ('accept-charset', ''),
    ('accept-encoding', 'gzip, deflate'),
    ('accept-language', ''),
    ('accept-ranges', ''),
    ('accept', ''),
    ('access-control-allow-origin', ''),
    ('age', ''),
    ('allow', ''),
    ('authorization', ''),
    ('cache-control', ''),
    ('content-disposition', ''),
    ('content-encoding', ''),
    ('content-language', ''),
    ('content-length', ''),
    ('content-location', ''),
    ('content-range', ''),
    ('content-type', ''),
    ('cookie', ''),
    ('date', ''),
    ('etag', ''),
    ('expect', ''),
    ('expires ', ''),
    ('from', ''),
    ('host', ''),
    ('if-match', ''),
    ('if-modified-since', ''),
    ('if-none-match', ''),
    ('if-range', ''),
    ('if-unmodified-since', ''),
    ('last-modified', ''),
    ('link', ''),
    ('location', ''),
    ('max-forwards', ''),
    ('proxy-authenticate', ''),
    ('proxy-authorization', ''),
    ('range', ''),
    ('referer', ''),
    ('refresh', ''),
    ('retry-after', ''),
    ('server', ''),
    ('set-cookie', ''),
    ('strict-transport-security', ''),
    ('transfer-encoding', ''),
    ('user-agent', ''),
    ('vary', ''),
    ('via', ''),
    ('www-authenticate', ''),
])


DEFAULT_MAX_SIZE = 4096


class HeadersTable(object):
    def __init__(self):
        super(HeadersTable, self).__init__()
        self.__table = SubTable(list(), DEFAULT_MAX_SIZE)

    def add(self, name, value):
        self.__table.add(name, value)

    def set_max_size(self, max_size):
        self.__table.set_max_size(max_size)

    def find(self, name, value=None):
        result = STATIC_TABLE.find(name, value)
        if result is not None:
            return result + 1
        else:
            return self.__table.find(name, value) + len(STATIC_TABLE) + 1

    def resolve(self, field):
        if field.name_element.index is None:
            return field

        name, value = self[field.name_element.index]
        if field.value_element.value is not None:
            value = field.value_element.value
        return mod_msg.HeaderField(
            mod_msg.HName(name, field.name_element.index, field.name_element.compressed),
            mod_msg.HValue(value, field.value_element.compressed)
        )

    def __contains__(self, key):
        return key in STATIC_TABLE or key in self.__table

    def __getitem__(self, index):
        index -= 1
        if index < len(STATIC_TABLE):
            return STATIC_TABLE[index]
        else:
            return self.__table[index - len(STATIC_TABLE)]

    def __len__(self):
        return len(STATIC_TABLE) + len(self.__table)

    def dynamic_items(self):
        return self.__table.items()


class Encoder(object):
    def __init__(self, greedy_huffman=False):
        super(Encoder, self).__init__()
        self.__table = HeadersTable()
        self.__sizes = list()
        self.__greedy_huffman = greedy_huffman

    @property
    def table(self):
        return self.__table

    def update_size(self, max_size):
        self.__sizes.append(max_size)

    def encode(self, fields):
        result = list()
        for max_size in self.__sizes:
            result.append(self.encode_max_size_update(max_size))
        self.__sizes = list()
        for field in fields:
            result.append(self.encode_one(field))
        return ''.join(result)

    def encode_max_size_update(self, max_size):
        self.__table.set_max_size(max_size)
        return encode_int(5, 1, max_size)

    def encode_one(self, field):
        if field.indexing == mod_msg.Indexing.YES:
            if field.name_element.index is not None:
                name = self.__table[field.name_element.index][0]
                if field.name_element.value is not None and field.name_element.value != name:
                    raise ValueError('field name id and value don\'t match')
            else:
                name = field.name_element.value
            self.__table.add(name, field.value_element.value)
        return self.pack(field)

    def pack(self, field):
        if field.index is not None:
            return encode_int(7, 1, field.index)
        else:
            if field.indexing == mod_msg.Indexing.YES:
                n = 6
                prefix = 1
            elif field.indexing == mod_msg.Indexing.NO:
                n = 4
                prefix = 0
            elif field.indexing == mod_msg.Indexing.NEVER:
                n = 4
                prefix = 1
            else:
                raise ValueError('invalid indexing value: {}'.format(field.indexing))

            result = list()
            if field.name_element.index is not None:
                result.append(encode_int(n, prefix, field.name_element.index))
            else:
                result.append(encode_int(n, prefix, 0))
                # never indexed option should not affect name compression
                result.append(self.__pack_element(field.name_element, mod_msg.Indexing.YES))
            result.append(self.__pack_element(field.value_element, field.indexing))
            return ''.join(result)

    def __pack_element(self, elem, indexing):
        if self.__greedy_huffman:
            if indexing == mod_msg.Indexing.NEVER:
                h_bit = 0
                data = elem.value
            else:
                plain_data = elem.value
                compressed_data = huffman.encode(plain_data)
                if len(plain_data) > len(compressed_data):
                    h_bit = 1
                    data = compressed_data
                else:
                    h_bit = 0
                    data = plain_data
        else:
            if elem.compressed:
                h_bit = 1
                data = huffman.encode(elem.value)
            else:
                h_bit = 0
                data = elem.value
        return encode_int(7, h_bit, len(data)) + data


class DecoderError(Exception):
    pass


class Decoder(object):
    def __init__(self):
        super(Decoder, self).__init__()
        self.__table = HeadersTable()

    @property
    def table(self):
        return self.__table

    def decode(self, data):
        result = list()
        if data:
            field = None
            while (field is None) and data:
                field, data = self.decode_one(data, is_first=True)
            if field is not None:
                result.append(field)
            while data:
                field, data = self.decode_one(data, is_first=False)
                result.append(field)
        return result

    def decode_one(self, data, is_first=False):
        n, prefix = self.__unpack_prefix(data[0])
        tmp, index, data = decode_int(n, data)
        if n == 7:
            if index > len(self.__table):
                raise DecoderError('invalid index')
            name, value = self.__table[index]
            return mod_msg.HeaderField(mod_msg.HName(name), mod_msg.HValue(value), index=index), data
        elif n == 5:
            if is_first:
                self.__table.set_max_size(index)
                return None, data
            else:
                raise DecoderError('dynamic table size update must be at the beginning of header block')
        else:
            if index != 0:
                if index > len(self.__table):
                    raise DecoderError('invalid index')
                name = mod_msg.HName(self.__table[index][0], index)
            else:
                name_compressed, name_str, data = self.__unpack_element(data)
                name = mod_msg.HName(name_str, compressed=name_compressed)

            value_compressed, value_str, data = self.__unpack_element(data)
            value = mod_msg.HValue(value_str, compressed=value_compressed)

            if n == 6:
                indexing = mod_msg.Indexing.YES
                self.__table.add(name.value, value.value)
            elif prefix == 0:
                indexing = mod_msg.Indexing.NO
            else:
                indexing = mod_msg.Indexing.NEVER

            return mod_msg.HeaderField(name, value, indexing), data

    @staticmethod
    def __unpack_element(data):
        compressed, length, data = decode_int(7, data)
        value = data[:length]
        data = data[length:]
        if compressed:
            return compressed, huffman.decode(value), data
        else:
            return compressed, value, data

    @staticmethod
    def __unpack_prefix(octet):
        value = decode_byte(octet)
        for i in range(7, 3, -1):
            if value & (2 ** i):
                return i, 1
        return 4, 0
