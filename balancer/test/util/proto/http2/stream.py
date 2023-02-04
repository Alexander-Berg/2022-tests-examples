# -*- coding: utf-8 -*-
import time
from balancer.test.util.proto.http2.framing import flags
from balancer.test.util.proto.http2.framing import frames
from balancer.test.util.proto.http2 import errors


class StateContext(object):
    def __init__(self, req_builder):
        super(StateContext, self).__init__()
        self.__force = False
        self.__req_builder = req_builder
        self.__timestamp = None

    @property
    def force(self):
        return self.__force

    @property
    def req_builder(self):
        return self.__req_builder

    @property
    def timestamp(self):
        return self.__timestamp

    def enforce(self):
        self.__force = True

    def update_time(self):
        self.__timestamp = time.time()


class ClientStateContext(StateContext):
    @property
    def msg_builder(self):
        return self.req_builder


class ServerStateContext(StateContext):
    def __init__(self, req_builder, resp_builder_factory):
        super(ServerStateContext, self).__init__(req_builder)
        self.__resp_builder_factory = resp_builder_factory
        self.__resp_builder = None
        self.__responses = list()

    @property
    def resp_builder(self):
        return self.__resp_builder

    @property
    def responses(self):
        return self.__responses

    @property
    def msg_builder(self):
        return self.resp_builder

    def new_response(self):
        if self.resp_builder is not None:
            self.__responses.append(self.resp_builder.build())
        self.__resp_builder = self.__resp_builder_factory()


class State(object):
    def __init__(self, ctx):
        super(State, self).__init__()
        self.__ctx = ctx
        self.__ctx.update_time()

    @property
    def ctx(self):
        return self.__ctx

    @property
    def timestamp(self):
        return self.ctx.timestamp

    @property
    def force(self):
        return self.ctx.force

    def recv(self, frame, force=False):
        if force:
            self.ctx.enforce()

        if isinstance(frame, frames.Data):
            return self.recv_data(frame)
        elif isinstance(frame, frames.Headers):
            return self.recv_headers(frame)
        elif isinstance(frame, frames.Priority):
            return self.recv_priority(frame)
        elif isinstance(frame, frames.RstStream):
            return self.recv_rst_stream(frame)
        elif isinstance(frame, frames.PushPromise):
            return self.recv_push_promise(frame)
        elif isinstance(frame, frames.WindowUpdate):
            return self.recv_window_update(frame)
        elif isinstance(frame, frames.Continuation):
            return self.recv_continuation(frame)
        elif isinstance(frame, frames.Unknown):
            return self.recv_unknown(frame)
        else:
            return self._error()

    def recv_data(self, frame):
        return self._recv_default(frame)

    def recv_headers(self, frame):
        return self._recv_default(frame)

    def recv_priority(self, frame):
        return self

    def recv_rst_stream(self, frame):
        return self._recv_default(frame)

    def recv_push_promise(self, frame):
        return self._error(frame)

    def recv_window_update(self, frame):
        return self

    def recv_continuation(self, frame):
        return self._recv_default(frame)

    def recv_unknown(self, frame):
        return self

    def _recv_default(self, frame):
        return self

    def send(self, frame, force=False):
        if force:
            self.ctx.enforce()

        if isinstance(frame, frames.Data):
            return self.send_data(frame)
        elif isinstance(frame, frames.Headers):
            return self.send_headers(frame)
        elif isinstance(frame, frames.Priority):
            return self.send_priority(frame)
        elif isinstance(frame, frames.RstStream):
            return self.send_rst_stream(frame)
        elif isinstance(frame, frames.PushPromise):
            return self.send_push_promise(frame)
        elif isinstance(frame, frames.WindowUpdate):
            return self.send_window_update(frame)
        elif isinstance(frame, frames.Continuation):
            return self.send_continuation(frame)
        elif isinstance(frame, frames.Unknown):
            return self.send_unknown(frame)
        else:
            return self._error()

    def send_data(self, frame):
        return self._send_default(frame)

    def send_headers(self, frame):
        return self._send_default(frame)

    def send_priority(self, frame):
        return self

    def send_rst_stream(self, frame):
        if frame.error_code == errors.NO_ERROR:
            return Close(self.ctx)
        else:
            return self._error(frame.error_code)

    def send_push_promise(self, frame):
        return self._error(frame)

    def send_window_update(self, frame):
        return self

    def send_continuation(self, frame):
        return self._send_default(frame)

    def send_unknown(self, frame):
        return self

    def _send_default(self, frame):
        return self._error()

    def _error(self, error_code=errors.PROTOCOL_ERROR):
        if self.force:
            return Unsafe(self.ctx)
        else:
            return Error(self.ctx, errors.StreamError(error_code))


class BaseWriteHeaders(State):
    def send_priority(self, frame):
        return self._error()

    def send_window_update(self, frame):
        return self._error()

    def send_unknown(self, frame):
        return self._error()

    def send_continuation(self, frame):
        self.ctx.msg_builder.add_headers_frame(frame)
        if frame.flags & flags.END_HEADERS:
            return self._next_state()
        else:
            return self

    def _next_state(self):
        raise NotImplementedError()


class ClientIdle(State):
    def send_headers(self, frame):
        self.ctx.msg_builder.add_headers_frame(frame)
        full_mask = flags.END_HEADERS | flags.END_STREAM
        if frame.flags & full_mask == full_mask:
            return Close(self.ctx)
        elif frame.flags & flags.END_STREAM:
            return WriteESHeaders(self.ctx)
        elif frame.flags & flags.END_HEADERS:
            return Open(self.ctx)
        else:
            return WriteHeaders(self.ctx)

    def _recv_default(self, frame):
        return self._error()


class ServerIdle(State):
    def recv_headers(self, frame):
        return ReservedRemote(self.ctx)

    def _recv_default(self, frame):
        return self._error()


class ReservedRemote(State):
    def send_headers(self, frame):
        self.ctx.new_response()
        self.ctx.msg_builder.add_headers_frame(frame)
        full_mask = flags.END_HEADERS | flags.END_STREAM
        if frame.flags & full_mask == full_mask:
            return Close(self.ctx)
        elif frame.flags & flags.END_STREAM:
            return WriteESHeaders(self.ctx)
        elif frame.flags & flags.END_HEADERS:
            return self
        else:
            return WriteServerHeaders(self.ctx)

    def send_data(self, frame):
        self.ctx.msg_builder.add_data_frame(frame)
        if frame.flags & flags.END_STREAM:
            return Close(self.ctx)
        else:
            return Open(self.ctx)


class WriteHeaders(BaseWriteHeaders):
    def _next_state(self):
        return Open(self.ctx)


class WriteServerHeaders(BaseWriteHeaders):
    def _next_state(self):
        return ReservedRemote(self.ctx)


class Open(State):
    def send_data(self, frame):
        self.ctx.msg_builder.add_data_frame(frame)
        if frame.flags & flags.END_STREAM:
            return Close(self.ctx)
        else:
            return self

    def send_headers(self, frame):
        self.ctx.msg_builder.add_trailing_headers_frame(frame)
        full_mask = flags.END_HEADERS | flags.END_STREAM
        if frame.flags & full_mask == full_mask:
            return Close(self.ctx)
        elif frame.flags & flags.END_STREAM:
            return WriteTrailingHeaders(self.ctx)
        else:
            return self._error()


class WriteESHeaders(BaseWriteHeaders):
    def _next_state(self):
        return Close(self.ctx)


class WriteTrailingHeaders(State):
    def _next_state(self):
        return Close(self.ctx)


class Close(State):
    pass


class Error(State):
    def __init__(self, ctx, error):
        super(Error, self).__init__(ctx)
        self.__error = error

    @property
    def error(self):
        return self.__error

    def _recv_default(self, frame):
        return self

    def _send_default(self, frame):
        return self

    def _error(self, error_code=errors.PROTOCOL_ERROR):
        return Error(self.ctx, errors.StreamError(error_code))


class Unsafe(State):
    def _recv_default(self, frame):
        return self

    def _send_default(self, frame):
        return self

    def _error(self, *args, **kwargs):
        return self


class Stream(object):
    def __init__(self, req_builder, resp_builder_factory):
        super(Stream, self).__init__()
        self.__client = ClientIdle(ClientStateContext(req_builder))
        self.__server = ServerIdle(ServerStateContext(req_builder, resp_builder_factory))
        self.__error = None
        self.__close_timestamp = None

    @property
    def last_frame_timestamp(self):
        return max(self.__client.timestamp, self.__server.timestamp)

    @property
    def close_timestamp(self):
        return self.__close_timestamp

    @property
    def error(self):
        return self.__error

    @property
    def req_builder(self):
        return self.__client.ctx.req_builder

    @property
    def resp_builder(self):
        return self.__server.ctx.resp_builder

    def handle_client_frame(self, frame, force=False):
        self.__client = self.__client.send(frame, force=force)
        self.__server = self.__server.recv(frame, force=force)
        self.__check_state()

    def handle_server_frame(self, frame, force=False):
        self.__server = self.__server.send(frame, force=force)
        self.__client = self.__client.recv(frame, force=force)
        self.__check_state()

    def is_closed(self):
        return isinstance(self.__client, Close) and isinstance(self.__server, Close)

    def is_error(self):
        return isinstance(self.__client, Error) or isinstance(self.__server, Error)

    def __repr__(self):
        return 'Stream({}, {})'.format(self.__client.__class__.__name__, self.__server.__class__.__name__)

    def __check_state(self):
        if isinstance(self.__client, Error):
            self.__error = self.__client.error
        elif isinstance(self.__server, Error):
            self.__error = self.__server.error
        elif self.is_closed() and self.__close_timestamp is None:
            self.__close_timestamp = self.last_frame_timestamp
