# -*- coding: utf-8 -*-
import sys
import os
import datetime

from balancer.test.util.stream.io.stream import StringStream, StreamRecvException
from balancer.test.util.proto.http2.framing import frames, flags
from balancer.test.util.stream.io.bit import BitReader, BitWriter
from balancer.test.util.resource import AbstractResource


class HTTP2WriteError(Exception):
    pass


class HTTP2ReadError(Exception):
    pass


class HTTP2ReadException(AssertionError):
    pass


class NoHTTP2FrameException(AssertionError):
    pass


def _write_unit(writer, obj):
    for field in obj.get_definition():
        writer.write_int(getattr(obj, field.name), field.size)


def _write_settings(writer, settings):
    for param in settings:
        _write_unit(writer, param)


def _read_unit(unit_type, reader, ctx):
    for field in unit_type.DEFINITION:
        if isinstance(field, tuple):  # FIXME: move coversion to ParseUnit
            field = frames.Field(field[0], field[1])
        if not field.check(ctx):
            continue
        if field.name in ctx:
            raise HTTP2ReadError('%s already exists' % field.name)
        ctx[field.name] = reader.next_int(field.size)

    for name, constraint in unit_type.CONSTRAINTS.iteritems():
        value = ctx.pop(name)
        if value != constraint:
            raise HTTP2ReadException('bad %s constraint %s value: %d' % (unit_type.__name__, name, value))

    return unit_type(**ctx)


class _StatefulReader(object):
    def __init__(self, reader):
        super(_StatefulReader, self).__init__()
        self.ctx = dict()
        self.__reader = reader

    def read_unit(self, unit_type):
        return _read_unit(unit_type, self.__reader, self.ctx)


def _read_settings(reader, count):
    params = list()
    for _ in range(count):
        params.append(_read_unit(frames.Parameter, reader, dict()))
    return params


def _print_frame(frame, to, marker):
    ln = int(os.environ.get("Y_BALANCER_TESTS_PRINT_HTTP2_FRAMES", 0))
    if not ln:
        return
    r = repr(frame)
    if len(r) > ln + 4:
        r = r[0:ln] + " ..."
    print >> to, datetime.datetime.now(), marker, r


class FrameStream(AbstractResource):
    def __init__(self, sock):
        super(FrameStream, self).__init__()
        self.__sock = sock
        self.__reader = BitReader(self.__sock)
        self.__writer = BitWriter(self.__sock)

    def _finish(self):
        self.__sock.finish()

    def write_frame(self, obj):
        if isinstance(obj, frames.Payload):
            data = StringStream('')
            data_writer = BitWriter(data)
            if isinstance(obj, frames.Settings):
                _write_settings(data_writer, obj.data)
            else:
                data_writer.write_bytes(obj.data)

            if isinstance(obj, frames.Padded) and (obj.flags & flags.PADDED):
                data_writer.write_bytes(obj.padding)
            data = str(data)
        else:
            data = ''

        if obj.length is None:
            obj.length = len(data) + obj.get_meta_length() / 8 - 9
        if isinstance(obj, frames.Padded) and (obj.flags & flags.PADDED) and obj.pad_length is None:
            obj.pad_length = len(obj.padding)
        _write_unit(self.__writer, obj)
        _print_frame(obj, sys.stderr, '>>>')
        self.__writer.write_bytes(data)

    def read_frame(self):
        try:
            reader = _StatefulReader(self.__reader)
            frame = reader.read_unit(frames.Frame)
            if frame.frame_type in frames.FRAME_TYPES:
                frame_cls = frames.FRAME_TYPES[frame.frame_type]
            else:
                frame_cls = frames.Unknown
            result = reader.read_unit(frame_cls)

            if isinstance(result, frames.Payload):
                if result.length is None:
                    raise HTTP2ReadError('total unit length not found')

                payload_length = result.length + 9 - result.get_meta_length() / 8
                payload = self.__reader.read_bytes(payload_length)

                if isinstance(result, frames.Padded) and (result.flags & flags.PADDED):
                    data = payload[:-result.pad_length]
                    result.padding = payload[-result.pad_length:]
                else:
                    data = payload

                if isinstance(result, frames.Settings):
                    if payload_length % 6 != 0:
                        raise HTTP2ReadError('Wrong settings frame payload length')  # FRAME_SIZE_ERROR
                    data_reader = BitReader(StringStream(data))
                    data = _read_settings(data_reader, payload_length / 6)

                result.data = data
        except StreamRecvException:
            raise NoHTTP2FrameException()

        if not self.__reader.buffer_is_empty():
            raise HTTP2ReadException('data is not empty')
        _print_frame(result, sys.stderr, '<<<')
        return result


def serialize(frame, stream_id=0):
    frame.stream_id = stream_id
    stream = StringStream('')
    FrameStream(stream).write_frame(frame)
    return stream.data
