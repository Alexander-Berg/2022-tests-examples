# -*- coding: utf-8 -*-
import struct
import os


def mask_ws(mask, payload):
    transformed_octet = []
    for i in xrange(0, len(payload)):
        transformed_octet.append(chr(ord(payload[i]) ^ ord(mask[i % 4])))
    return ''.join(transformed_octet)


class WebSocketOpcode:
    WS_CONT_FRAME = 0x00
    WS_TEXT_FRAME = 0x01
    WS_BIN_FRAME = 0x02
    # 3 - 7 reserved
    WS_CONN_CLOSE_FRAME = 0x08
    WS_PING_FRAME = 0x09
    WS_PONG_FRAME = 0x0a
    # 0x0b - 0xff reserved


class WSReaderException(Exception):
    pass


class WebSocketFrame(object):
    def __init__(self, fin=0, rsv=0, opcode=0, mask=0, mask_key=os.urandom(4), payload=0, flags=0):
        self.__fin = fin
        self.__rsv = rsv
        self.__opcode = opcode
        self.__mask = mask
        self.__mask_key = mask_key
        self.__payload = payload
        self.__flags = flags

    def __repr__(self):
        return """WebSocketFrame({addr}):
        FIN: {fin}
        RSV: {rsv}
        OPCODE: {opcode}
        MASK: {mask}
        MASK_KEY: {mask_key}
        PAYLOAD: {payload}
        FLAGS: {flags}
        """.format(
            addr=hex(id(self)), fin=self.__fin, rsv=self.__rsv, opcode=self.__opcode,
            mask=self.__mask, mask_key=self.__mask_key, payload=self.__payload,
            flags=self.__flags)

    @property
    def opcode(self):
        return self.__opcode

    @property
    def payload(self):
        return self.__payload

    def to_request(self):
        return self

    def to_raw(self):
        header = 0x0
        payload_size = len(self.__payload)

        if self.__opcode & WebSocketOpcode.WS_CONN_CLOSE_FRAME:
            if not self.__fin or payload_size > 125:
                raise WSReaderException("Control frame payload max size 125 bytes and fin bit must be set")

        fin = 0
        if self.__fin:
            fin = 0x80
        header = struct.pack("B", fin | self.__opcode | self.__flags)

        mask = 0
        if self.__mask == 1:
            mask = 0x80

        if payload_size < 126:
            header += struct.pack("B", payload_size | mask)
        elif payload_size <= 0xFFFF:
            header += struct.pack("!BH", 126 | mask, payload_size)
        else:
            header += struct.pack("!BQ", 127 | mask, payload_size)

        if self.__mask == 1:
            header += self.__mask_key

        payload = self.__payload
        if self.__mask == 1:
            payload = mask_ws(self.__mask_key, self.__payload)

        h = ""
        for i in header:
            h += hex(ord(i))
        for i in payload:
            h += hex(ord(i))
        return header + payload

    @staticmethod
    def from_raw(reader):
        first_byte, second_byte = struct.unpack("BB", reader.read(2))

        fin = first_byte & 0x80 == 0x80
        rsv = first_byte & 0x70
        opcode = first_byte & 0x0f

        mask = second_byte & 0x80 == 0x80

        payload_len = second_byte & 0x7f

        if opcode & WebSocketOpcode.WS_CONN_CLOSE_FRAME and payload_len > 125:
            raise WSReaderException("Control frame payload max size 125 bytes")

        if payload_len == 126:
            payload_len = struct.unpack("!H", reader.read(2))
        elif payload_len == 127:
            payload_len = struct.unpack("!Q", reader.read(8))

        mask_key = 0
        if mask == 1:
            mask_key = reader.read(4)
            st = ""
            for i in mask_key:
                st += hex(ord(i))

        payload = reader.read(payload_len)
        if mask:
            payload = mask_ws(mask_key, payload)

        return WebSocketFrame(fin=fin, rsv=rsv, opcode=opcode, mask=mask, mask_key=mask_key, payload=payload, flags=0)


class WSStream(object):
    def __init__(self, reader, writer):
        self._reader = reader
        self._writer = writer

    def read_request(self):
        """
        :rtype: RawHTTPRequest
        """
        return WebSocketFrame.from_raw(self._reader)

    def read_response(self):
        return self.read_request()

    def write_response(self, response):
        """
        :type response: RawHTTPResponse
        :param response: raw HTTP response
        """
        self._writer.write(response.to_raw())

    def write_request(self, request):
        self.write_response(request)

    def set_timeout(self, timeout):
        self._writer.set_timeout(timeout)
        self._reader.set_timeout(timeout)
