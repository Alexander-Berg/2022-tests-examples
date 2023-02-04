# -*- coding: utf-8 -*-
import socket
import errno
import balancer.test.util.proto.http2.framing.stream as frame_stream
import balancer.test.util.proto.iface.data as mod_data
import balancer.test.util.proto.iface.headers as mod_headers
import balancer.test.util.proto.http2.message as mod_msg
import balancer.test.util.proto.http2.stream as stream
import balancer.test.util.proto.http2.framing.frames as frames
import balancer.test.util.proto.http2.framing.flags as mod_flags
import balancer.test.util.proto.http2.hpack._hpack as hpack
from balancer.test.util.resource import AbstractResource


class RawHTTP2HeadersBuilder(object):
    def __init__(self, decoder):
        super(RawHTTP2HeadersBuilder, self).__init__()
        self.__decoder = decoder
        self.__headers_frames = list()
        self.__headers = None

    def add_frame(self, frame):
        self.__headers_frames.append(frame.data)
        if frame.flags & mod_flags.END_HEADERS:
            data = ''.join(self.__headers_frames)
            self.__headers = self.__decoder.decode(data)

    def build(self):
        return self.__headers


class RawHTTP2MessageBuilder(object):
    def __init__(self, decoder):
        super(RawHTTP2MessageBuilder, self).__init__()
        self.__headers_builder = RawHTTP2HeadersBuilder(decoder)
        self.__data = list()
        self.__trailing_headers_builder = RawHTTP2HeadersBuilder(decoder)
        self.__frames = list()

    def add_headers_frame(self, frame):
        self.__add_frame(frame)
        self.__headers_builder.add_frame(frame)

    def add_data_frame(self, frame):
        self.__add_frame(frame)
        self.__data.append(frame.data)

    def add_trailing_headers_frame(self, frame):
        self.__add_frame(frame)
        self.__trailing_headers_builder.add_frame(frame)

    def build_headers(self):
        return self.__headers_builder.build()

    def build(self):
        return mod_msg.RawHTTP2Message(
            self.build_headers(),
            self.__data,
            self.__trailing_headers_builder.build(),
            self.__frames,
        )

    def __add_frame(self, frame):
        self.__frames.append(frame)


class MockBuilder(object):
    def add_headers_frame(self, frame):
        pass

    def add_data_frame(self, frame):
        pass

    def add_trailing_headers_frame(self, frame):
        pass

    def build(self):
        raise NotImplementedError()


class HTTP2ConnectionError(Exception):
    pass


class ConnectionContext(object):
    def __init__(self):
        super(ConnectionContext, self).__init__()
        self.__open = dict()
        self.__closed = dict()

    def stream_exists(self, stream_id):
        return stream_id in self.__open or stream_id in self.__closed

    def is_open_stream(self, stream_id):
        return stream_id in self.__open

    def get_stream(self, stream_id):
        if stream_id in self.__open:
            return self.__open[stream_id]
        elif stream_id in self.__closed:
            return self.__closed[stream_id]
        else:
            raise HTTP2ConnectionError('stream {} is in idle state'.format(stream_id))

    def add_stream(self, stream_id, stream):
        self.__open[stream_id] = stream

    def update_stream_state(self, stream_id):
        value = self.get_stream(stream_id)
        if value.is_closed() and stream_id in self.__open:
            del self.__open[stream_id]
            self.__closed[stream_id] = value
        # TODO: Error state


_PARAM = frames.Parameter


class StateContext(object):
    _MAX_STREAM_ID = 2 ** 31

    def __init__(self, conn_context, stream_factory, last_stream_id):
        super(StateContext, self).__init__()
        self._conn_context = conn_context
        self._stream_factory = stream_factory
        self._last_stream_id = last_stream_id
        self.__force = False
        self.__go_away = False
        self.__settings = {
            _PARAM.HEADER_TABLE_SIZE: 4096,
            _PARAM.ENABLE_PUSH: 1,
            _PARAM.MAX_CONCURRENT_STREAMS: None,
            _PARAM.INITIAL_WINDOW_SIZE: 2 ** 16 - 1,
            _PARAM.MAX_FRAME_SIZE: 2 ** 14,
            _PARAM.MAX_HEADER_LIST_SIZE: None,
        }

    @property
    def force(self):
        return self.__force

    def enforce(self):
        self.__force = True

    @property
    def go_away(self):
        return self.__go_away

    def set_go_away(self):
        self.__go_away = True

    @property
    def settings(self):
        return self.__settings

    def stream_exists(self, stream_id):
        return self._conn_context.stream_exists(stream_id)

    def is_open_stream(self, stream_id):
        return self._conn_context.is_open_stream(stream_id)

    def open_stream(self, stream_id):
        if self.is_valid_new_stream_id(stream_id) or self.force:
            self._last_stream_id = stream_id
        else:
            raise HTTP2ConnectionError('invalid stream id')
        result = self._create_stream()
        self._conn_context.add_stream(stream_id, result)
        return result

    def _create_stream(self):
        raise NotImplementedError()

    def is_valid_new_stream_id(self, stream_id):
        raise NotImplementedError()

    def next_stream_id(self):
        result = self._last_stream_id + 2
        if not (self.is_valid_new_stream_id(result) or self.force):
            raise HTTP2ConnectionError('no more stream ids are left')
        return result

    def handle_frame(self, frame):
        stream_id = frame.stream_id
        stream = self._conn_context.get_stream(stream_id)
        self._handle_frame(stream, frame)
        self._conn_context.update_stream_state(stream_id)
        if stream.is_error() and not self.force:  # FIXME: find more suitable place
            raise stream.error

    def _handle_frame(self, stream, frame):
        raise NotImplementedError()


class ClientStateContext(StateContext):
    def __init__(self, conn_context, stream_factory):
        super(ClientStateContext, self).__init__(conn_context, stream_factory, -1)

    def _create_stream(self):
        return self._stream_factory.new_client_stream()

    def _handle_frame(self, stream, frame):
        stream.handle_client_frame(frame, self.force)

    def is_valid_new_stream_id(self, stream_id):
        return self._last_stream_id < stream_id < self._MAX_STREAM_ID and stream_id % 2 == 1


class ServerStateContext(StateContext):
    def __init__(self, conn_context, stream_factory):
        super(ServerStateContext, self).__init__(conn_context, stream_factory, 0)

    def _create_stream(self):
        return self._stream_factory.new_server_stream()

    def _handle_frame(self, stream, frame):
        stream.handle_server_frame(frame, self.force)

    def is_valid_new_stream_id(self, stream_id):
        return self._last_stream_id < stream_id < self._MAX_STREAM_ID and stream_id % 2 == 0


class State(object):
    def __init__(self, context):
        super(State, self).__init__()
        self.__context = context

    @property
    def context(self):
        return self.__context

    def handle(self, frame, force=False):
        if force:
            self.__context.enforce()
        return self._handle(frame)

    def error(self):
        if self.__context.force:
            return Unsafe(self.__context)
        else:
            return Error(self.__context)

    def _handle(self, frame):
        raise NotImplementedError()


class Unsafe(State):
    def handle(self, frame, force=False):
        if frame.stream_id != 0:
            if self.context.is_open_stream(frame.stream_id):  # TODO: not opened stream
                self.context.handle_frame(frame, force=True)
        return self


def _conn_frame(func):
    def result_func(state, frame):
        if frame.stream_id != 0:
            return state.error()
        else:
            return func(state, frame)

    return result_func


class Wait(State):
    def __init__(self, context):
        super(Wait, self).__init__(context)

    def _handle(self, frame):
        if isinstance(frame, frames.Settings):
            return self._handle_settings(frame)
        elif isinstance(frame, frames.Ping):
            return self._handle_ping(frame)
        elif isinstance(frame, frames.Goaway):
            return self._handle_goaway(frame)
        elif isinstance(frame, frames.WindowUpdate):
            return self._handle_window_update(frame)
        elif isinstance(frame, frames.Unknown):
            return self._handle_unknown(frame)
        else:
            return self._handle_stream_frame(frame)

    @_conn_frame
    def _handle_settings(self, frame):
        for param in frame.data:
            self.context.settings[param.identifier] = param.value  # TODO: check values
        return self

    @_conn_frame
    def _handle_ping(self, frame):
        return self

    @_conn_frame
    def _handle_goaway(self, frame):
        self.context.set_go_away()
        return self

    def _handle_window_update(self, frame):
        if frame.stream_id == 0:
            return self
        elif self.context.stream_exists(frame.stream_id):
            self.context.handle_frame(frame)
            return self
        else:
            return self.error()

    def _handle_unknown(self, frame):
        if frame.stream_id != 0 and self.context.stream_exists(frame.stream_id):
            self.context.handle_frame(frame)
        return self

    def _handle_stream_frame(self, frame):
        if frame.stream_id == 0:
            return self.error()
        if isinstance(frame, frames.Headers):
            return self._handle_headers(frame)
        elif isinstance(frame, frames.PushPromise):
            return self._handle_push_promise(frame)
        elif isinstance(frame, frames.Priority):
            return self._handle_priority(frame)
        elif isinstance(frame, frames.RstStream):
            return self._handle_rst_stream(frame)
        else:
            return self._handle_stream_frame_default(frame)

    def _handle_headers(self, frame):
        stream_id = frame.stream_id
        if not self.context.stream_exists(stream_id):
            self.context.open_stream(stream_id)
        elif not self.context.is_open_stream(stream_id):
            return self.error()

        if not frame.flags & mod_flags.END_HEADERS:
            self.context.handle_frame(frame)
            return self._goto_write_headers(stream_id)
        else:
            return self._handle_stream_frame_default(frame)

    def _handle_push_promise(self, frame):
        return self._handle_stream_frame_default(frame)

    def _handle_priority(self, frame):
        if self.context.stream_exists(frame.stream_id):
            self.context.handle_frame(frame)
        return self

    def _handle_rst_stream(self, frame):
        if self.context.stream_exists(frame.stream_id):
            self.context.handle_frame(frame)
        else:
            pass  # TODO
        return self

    def _handle_stream_frame_default(self, frame):
        if not self.context.is_open_stream(frame.stream_id):
            return self.error()
        else:
            self.context.handle_frame(frame)
            return self

    def _goto_write_headers(self, stream_id):
        raise NotImplementedError()


class WriteHeaders(State):
    def __init__(self, context, stream_id):
        super(WriteHeaders, self).__init__(context)
        self.__stream_id = stream_id

    def _handle(self, frame):
        if isinstance(frame, frames.Continuation) and frame.stream_id == self.__stream_id:
            self.context.handle_frame(frame)
            if frame.flags & mod_flags.END_HEADERS:
                return self._goto_wait()
            else:
                return self
        else:
            return self.error()

    def _goto_wait(self):
        raise NotImplementedError()


class ClientWait(Wait):
    def _goto_write_headers(self, stream_id):
        return ClientWriteHeaders(self.context, stream_id)


class ClientWriteHeaders(WriteHeaders):
    def _goto_wait(self):
        return ClientWait(self.context)


class ServerWait(Wait):
    def _handle_push_promise(self, frame):
        stream_id = frame.stream_id
        if not self.context.stream_exists(stream_id):
            self.context.open_stream(stream_id)
        else:
            return self.error()

        if not frame.flags & mod_flags.END_HEADERS:
            self.context.handle_frame(frame)
            return self._goto_write_headers(stream_id)
        else:
            return self._handle_stream_frame_default(frame)

    def _goto_write_headers(self, stream_id):
        return ServerWriteHeaders(self.context, stream_id)


class ServerWriteHeaders(WriteHeaders):
    def _goto_wait(self):
        return ServerWait(self.context)


class Close(State):
    def _handle(self, frame):
        return self


class Error(State):
    def _handle(self, frame):
        return self


class StreamFactory(object):
    def __init__(self, decoder):
        super(StreamFactory, self).__init__()
        self.__decoder = decoder

    def _create_builder(self):
        return RawHTTP2MessageBuilder(self.__decoder)

    def _create_mock_builder(self):
        return MockBuilder()

    def new_client_stream(self):
        raise NotImplementedError()

    def new_server_stream(self):
        raise NotImplementedError()


class ClientStreamFactory(StreamFactory):
    def new_client_stream(self):
        return stream.Stream(self._create_mock_builder(), self._create_builder)

    def new_server_stream(self):
        return stream.Stream(self._create_builder(), self._create_builder)


class ServerStreamFactory(StreamFactory):
    def new_client_stream(self):
        return stream.Stream(self._create_builder(), self._create_mock_builder)

    def new_server_stream(self):
        return stream.Stream(self._create_mock_builder(), self._create_mock_builder)


def _map_chunk(chunk):  # TODO: get rid of Chunk class
    if isinstance(chunk, mod_data.Chunk):
        return chunk.data
    else:
        return chunk


class Connection(AbstractResource):
    PREFACE = 'PRI * HTTP/2.0\r\n\r\nSM\r\n\r\n'

    def __init__(self, stream_factory):
        super(Connection, self).__init__()
        self.__max_length = 2 ** 14
        self.__new_encoder_table_size = list()
        self.__encoder = hpack.Encoder()
        self._context = ConnectionContext()
        self._client_state = ClientWait(ClientStateContext(self._context, stream_factory))
        self._server_state = ServerWait(ServerStateContext(self._context, stream_factory))

    @property
    def client_settings(self):
        return self._client_state.context.settings

    @property
    def server_settings(self):
        return self._server_state.context.settings

    def get_stream_last_frame_timestamp(self, stream_id):
        return self._context.get_stream(stream_id).last_frame_timestamp

    def get_stream_close_timestamp(self, stream_id):
        return self._context.get_stream(stream_id).close_timestamp

    def stream_is_closed(self, stream_id):
        return self._context.get_stream(stream_id).is_closed()

    def _client_frame(self, frame, force=False):
        self._client_state = self._client_state.handle(frame, force=force)

    def _server_frame(self, frame, force=False):
        self._server_state = self._server_state.handle(frame, force=force)

    @property
    def encoder(self):
        return self.__encoder

    def set_max_length(self, max_length):
        self.__max_length = max_length

    def update_encoder_table_size(self, max_size):
        # FIXME: update policy (google accepts only two updates)
        if isinstance(max_size, int):
            max_size = [max_size]
        self.__new_encoder_table_size.extend(max_size)

    def write_frame(self, frame):
        raise NotImplementedError()

    def read_frame(self):
        raise NotImplementedError()

    def wait_frame(self, frame_type):
        frame = self.read_frame()
        while not isinstance(frame, frame_type):
            frame = self.read_frame()
        return frame

    def read_message(self, stream_id):
        stream_obj = self._context.get_stream(stream_id)
        if not stream_obj.is_closed():  # FIXME: split is_closed into closed(local) and closed(remote)
            eos_headers = False

            while True:
                frame = self.read_frame()

                if frame.stream_id != stream_id:
                    continue

                if isinstance(frame, frames.Data) and \
                        frame.flags & mod_flags.END_STREAM:
                    break

                if isinstance(frame, frames.Headers) and \
                        frame.flags & mod_flags.END_STREAM:
                    eos_headers = True

                if isinstance(frame, frames.Headers) and not (frame.flags & mod_flags.END_HEADERS):
                    while True:
                        frame = self.read_frame()
                        assert isinstance(frame, frames.Continuation) and frame.stream_id == stream_id
                        if frame.flags & mod_flags.END_HEADERS:
                            break

                if eos_headers:
                    break

        return stream_obj.resp_builder.build()

    def read_headers(self, stream_id):
        stream_obj = self._context.get_stream(stream_id)
        end_headers = False  # FIXME: wait for appropriate stream state
        while not end_headers:
            frame = self.read_frame()
            end_headers = (frame.stream_id == stream_id) and (frame.flags & mod_flags.END_HEADERS)
        return mod_headers.RawHeaders(stream_obj.resp_builder.build_headers())

    def read_chunk(self, stream_id):
        frame = self.read_frame()
        while not (isinstance(frame, frames.Data) and frame.stream_id == stream_id):
            frame = self.read_frame()
        return frame.data

    def write_headers(self, headers, stream_id, flags=0):
        size_updates = list()
        for table_size in self.__new_encoder_table_size:
            size_updates.append(self.__encoder.encode_max_size_update(table_size))
        self.__new_encoder_table_size = list()
        full_data = ''.join(size_updates) + self.__encoder.encode(headers)
        data = list()
        while full_data:
            data.append(full_data[:self.__max_length])
            full_data = full_data[self.__max_length:]
        self.write_encoded_headers(data, stream_id, flags)

    def write_encoded_headers(self, data, stream_id, flags=0):
        if isinstance(data, str):
            data = [data]
        if len(data) == 1:
            flags |= mod_flags.END_HEADERS
        self.write_frame(frames.Headers(
            length=None, flags=flags, reserved=0, stream_id=stream_id,
            data=data[0]
        ))
        if len(data) > 1:
            self.write_continuation(data[1:], stream_id)

    def write_continuation(self, data, stream_id):
        assert len(data) > 0
        for header_block in data[:-1]:
            self.write_frame(frames.Continuation(
                length=None, flags=0, reserved=0, stream_id=stream_id,
                data=header_block,
            ))
        self.write_frame(frames.Continuation(
            length=None, flags=mod_flags.END_HEADERS, reserved=0, stream_id=stream_id,
            data=data[-1],
        ))

    def write_data(self, data, stream_id, end_stream):
        if isinstance(data, str):
            data = [data]
        data = [_map_chunk(chunk) for chunk in data]
        for chunk in data[:-1]:
            self.write_chunk(chunk, stream_id, False)
        self.write_chunk(data[-1], stream_id, end_stream)

    def write_chunk(self, chunk, stream_id, end_stream):
        flags = 0
        if end_stream:
            flags |= mod_flags.END_STREAM
        self.write_frame(frames.Data(
            length=None, flags=flags, reserved=0, stream_id=stream_id,
            data=chunk,
        ))

    def write_message(self, msg, stream_id):
        # TODO: trailing headers
        has_data = len(msg.data.content) != 0
        if has_data:
            headers_flags = 0
        else:
            headers_flags = mod_flags.END_STREAM
        self.write_headers(msg.headers.headers, stream_id, headers_flags)
        if has_data:
            self.write_data(msg.data.chunks, stream_id, end_stream=True)

    def write_settings(self, params):
        self.write_frame(frames.Settings(length=None, flags=0, reserved=0, data=[
            frames.Parameter(param_id, param_value) for (param_id, param_value) in params
        ]))

    def write_window_update(self, window_size_increment, stream_id=0):
        self.write_frame(frames.WindowUpdate(
            flags=0, reserved=0, stream_id=stream_id,
            window_update_reserved=0,
            window_size_increment=window_size_increment,
        ))

    def goaway(self, last_stream_id, error_code, data=None):
        if data is None:
            data = ''
        self.write_frame(frames.Goaway(
            length=None, flags=0, reserved=0, goaway_reserved=0,
            last_stream_id=last_stream_id, error_code=error_code, data=data,
        ))


class StreamProxy(object):
    def __init__(self, stream_id, conn):
        super(StreamProxy, self).__init__()
        self.__stream_id = stream_id
        self.__conn = conn

    @property
    def stream_id(self):
        return self.__stream_id

    def is_closed(self):
        return self.__conn.stream_is_closed(self.__stream_id)

    @property
    def last_frame_timestamp(self):
        return self.__conn.get_stream_last_frame_timestamp(self.__stream_id)

    @property
    def close_timestamp(self):
        return self.__conn.get_stream_close_timestamp(self.__stream_id)

    def write_frame(self, frame, force=False):
        frame.stream_id = self.__stream_id
        self.__conn.write_frame(frame, force=force)

    def read_frame(self):
        frame = self.__conn.read_frame()
        while frame.stream_id != self.__stream_id:
            frame = self.__conn.read_frame()
        return frame

    def wait_frame(self, frame_type):  # TODO: avoid copy-paste
        frame = self.read_frame()
        while not isinstance(frame, frame_type):
            frame = self.read_frame()
        return frame

    def write_headers(self, headers, end_stream):
        if end_stream:
            headers_flags = mod_flags.END_STREAM
        else:
            headers_flags = 0
        self.__conn.write_headers(headers.headers, self.__stream_id, headers_flags)

    def write_encoded_headers(self, data, end_stream):
        if end_stream:
            headers_flags = mod_flags.END_STREAM
        else:
            headers_flags = 0
        self.__conn.write_encoded_headers(data, self.__stream_id, headers_flags)

    def write_data(self, data):
        self.__conn.write_data(data.chunks, self.__stream_id, end_stream=True)

    def write_chunk(self, chunk, end_stream=False):
        self.__conn.write_chunk(chunk, self.__stream_id, end_stream=end_stream)

    def write_message(self, raw_request):
        self.__conn.write_message(raw_request, self.__stream_id)

    def write_window_update(self, window_size_increment):
        self.__conn.write_window_update(window_size_increment, self.__stream_id)

    def write_priority(self, exclusive, stream_dependency, weight):
        if isinstance(stream_dependency, StreamProxy):
            stream_dependency = stream_dependency.stream_id
        self.__conn.write_frame(frames.Priority(
            flags=0, reserved=0, stream_id=self.__stream_id,
            exclusive=exclusive, stream_dependency=stream_dependency, weight=weight,
        ))

    def read_message(self):
        return self.__conn.read_message(self.__stream_id)

    def read_headers(self):
        return self.__conn.read_headers(self.__stream_id)

    def read_chunk(self):
        return self.__conn.read_chunk(self.__stream_id)

    def reset(self, error_code):
        self.write_frame(
            frames.RstStream(flags=0, reserved=0, stream_id=None, error_code=error_code),
            force=True,
        )


class ClientConnection(Connection):
    def __init__(self, sock):
        super(ClientConnection, self).__init__(ClientStreamFactory(hpack.Decoder()))
        self.__sock = sock
        self.__stream = frame_stream.FrameStream(sock)

    @property
    def sock(self):
        return self.__sock

    def _finish(self):
        self.__stream.finish()

    def close(self):
        self.finish()

    def write_preface(self):
        self.__sock.send(self.PREFACE)

    def write_frame(self, frame, force=False):
        self._client_frame(frame, force=force)
        try:
            self.__stream.write_frame(frame)
        except socket.error, err:
            # TODO: wrap it in io.stream module
            if force and err.errno == errno.EPIPE:  # server may close connection without reading the whole frame
                pass
            else:
                raise

    def read_frame(self):
        frame = self.__stream.read_frame()
        self._server_frame(frame)
        return frame

    def perform_request_raw_response(self, req):
        if isinstance(req, mod_msg.HTTP2Request):
            req = req.to_raw_request()  # TODO: compression
        stream_id = self.__next_stream_id()
        self.write_message(req, stream_id)
        return self.read_message(stream_id)

    def perform_request(self, req):
        resp = self.perform_request_raw_response(req)
        return resp.to_response()

    def create_stream(self, stream_id=None):
        if stream_id is None:
            stream_id = self.__next_stream_id()
        return StreamProxy(stream_id, self)

    def __next_stream_id(self):
        return self._client_state.context.next_stream_id()
