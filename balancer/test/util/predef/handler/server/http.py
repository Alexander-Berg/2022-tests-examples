# -*- coding: utf-8 -*-
import time

from balancer.test.util import sync
from balancer.test.util.predef import http
import balancer.test.util.proto.iface.data as d
import balancer.test.util.proto.http.message as http_message
from balancer.test.util.proto.handler.server import State, HandlerConfigException
from balancer.test.util.proto.handler.server.http import HTTPServerHandler, HTTPConfig, PreparseHandler,\
    StaticResponseHandler, StaticResponseConfig


class SimpleHandler(StaticResponseHandler):
    """
    Very simple handler. Reads request and writes static response, defined in config.
    """
    def handle_parsed_request(self, raw_request, stream):
        stream.write_response(self.config.response)
        self.finish_response()


class SimpleConfig(StaticResponseConfig):
    HANDLER_TYPE = SimpleHandler

    def __init__(self, response=None):
        if response is None:
            response = http.response.ok()
        super(SimpleConfig, self).__init__(response)


class CloseHandler(StaticResponseHandler):
    """
    Just like SimpleHandler, but forces connection close after handling request.
    """
    def handle_parsed_request(self, raw_request, stream):
        stream.write_response(self.config.response)
        self.force_close()


class CloseConfig(SimpleConfig):
    HANDLER_TYPE = CloseHandler


class SimpleDelayedHandler(StaticResponseHandler):
    """
    Not that simple handler. Reads request, sleeps delay time and writes static response, defined in config.
    """
    def handle_parsed_request(self, raw_request, stream):
        time.sleep(self.config.response_delay)
        stream.write_response(self.config.response)
        self.finish_response()


class SimpleDelayedConfig(StaticResponseConfig):
    HANDLER_TYPE = SimpleDelayedHandler

    def __init__(self, response=None, response_delay=0):
        if response is None:
            response = http.response.ok()
        super(SimpleDelayedConfig, self).__init__(response)
        self.response_delay = response_delay


class DelayedCloseHandler(StaticResponseHandler):
    """
    Reads request, writes static response, sleeps delay time and closes connection.
    """
    def handle_parsed_request(self, raw_request, stream):
        stream.write_response(self.config.response)
        time.sleep(self.config.response_delay)
        self.force_close()


class DelayedCloseConfig(StaticResponseConfig):
    HANDLER_TYPE = DelayedCloseHandler

    def __init__(self, response=None, response_delay=0):
        if response is None:
            response = http.response.ok()
        super(DelayedCloseConfig, self).__init__(response)
        self.response_delay = response_delay


class ChunkedHandler(StaticResponseHandler):
    """
    Reads request and writes chunked response with timeout between chunks.
    """
    def handle_parsed_request(self, raw_request, stream):
        stream.write_response_line(self.config.response.response_line)
        stream.write_headers(self.config.response.headers)
        for chunk in self.config.response.data.chunks:
            stream.write_chunk(chunk)
            time.sleep(self.config.chunk_timeout)
        self.finish_response()


class ChunkedConfig(StaticResponseConfig):
    HANDLER_TYPE = ChunkedHandler

    def __init__(self, response=None, chunk_timeout=0):
        """
        :type response: :class:`.HTTPResponse` or :class:`.RawHTTPResponse`
        :param response: static response
        :param float chunk_timeout: timeout between sending response chunks
        """
        if response is not None and not isinstance(response.data, d.ChunkedData):
            raise HandlerConfigException('data should be chunked')

        if response is None:
            response = http.response.ok(data=[])

        super(ChunkedConfig, self).__init__(response=response)
        self.chunk_timeout = chunk_timeout


class BrokenHandler(PreparseHandler):
    """
    Reads request and writes malformed HTTP response.
    """
    def handle_parsed_request(self, raw_request, stream):
        stream.write_line('error')
        stream.write_header('Error-Header', 'Error value')
        stream.end_headers()
        self.force_close()


class BrokenConfig(HTTPConfig):
    HANDLER_TYPE = BrokenHandler


class DummyHandler(PreparseHandler):
    """
    Reads request, waits timeout and forces connection close without writing any data.
    """
    def handle_parsed_request(self, raw_request, stream):
        time.sleep(float(self.config.timeout))
        self.force_close()


class DummyConfig(HTTPConfig):
    HANDLER_TYPE = DummyHandler

    def __init__(self, timeout=0):
        """
        :param float timeout: timeout before closing connection
        """
        super(DummyConfig, self).__init__()
        self.timeout = timeout


class CounterState(State):
    def __init__(self, config):
        super(CounterState, self).__init__(config)
        self.__counter = sync.Counter(0)

    @property
    def counter(self):
        return self.__counter


class TwoModeHandler(HTTPServerHandler):
    def handle_request(self, stream):
        counter = self.state.counter.inc()
        if counter < self.config.prefix:
            self.handle_first(stream)
        else:
            self.handle_second(stream)

    def handle_first(self, stream):
        raise NotImplementedError()

    def handle_second(self, stream):
        raise NotImplementedError()


class TwoModeConfig(HTTPConfig):
    HANDLER_TYPE = TwoModeHandler
    STATE_TYPE = CounterState

    def __init__(self, prefix=0):
        super(TwoModeConfig, self).__init__()
        self.prefix = prefix


class ThreeModeHandler(StaticResponseHandler):
    def handle_parsed_request(self, raw_request, stream):
        counter = self.state.counter.inc()
        if counter < self.config.prefix:
            self.handle_prefix(raw_request, stream)
        else:
            value = (counter - self.config.prefix) % (self.config.first + self.config.second)
            if value < self.config.first:
                self.handle_first(raw_request, stream)
            else:
                self.handle_second(raw_request, stream)

    def handle_prefix(self, raw_request, stream):
        self.handle_second(raw_request, stream)

    def handle_first(self, raw_request, stream):
        stream.write_response(self.config.response)
        self.finish_response()

    def handle_second(self, raw_request, stream):
        self.force_close()


class ThreeModeConfig(StaticResponseConfig):
    """ counter += 1
        if counter < prefix:
            fail()
        else:
            value = (counter - prefix) % (first + second)
            if value < first:
                succeed()
            else:
                fail()
    """
    HANDLER_TYPE = ThreeModeHandler
    STATE_TYPE = CounterState

    def __init__(self, prefix=0, first=10, second=10, response=None):
        if response is None:
            response = http.response.ok()
        super(ThreeModeConfig, self).__init__(response)
        self.prefix = prefix
        self.first = first
        self.second = second


class MultiActionHandler(StaticResponseHandler):
    def handle_parsed_request(self, raw_request, stream):
        counter = self.state.counter.inc()

        if counter >= len(self.config.actions):
            self.config.default_action(self, stream, raw_request)
        else:
            self.config.actions[counter](self, stream, raw_request)


class MultiActionConfig(StaticResponseConfig):
    HANDLER_TYPE = MultiActionHandler
    STATE_TYPE = CounterState

    @staticmethod
    def close_conn(handler, stream, raw_request):
        handler.force_close()

    @staticmethod
    def send_response(handler, stream, raw_request):
        stream.write_response(handler.config.response)
        handler.finish_response()

    @staticmethod
    def raise_error(handler, stream, raw_request):
        raise Exception()

    def __init__(self, actions, response=None, default_action=raise_error):
        if response is None:
            response = http.response.ok()
        super(MultiActionConfig, self).__init__(response)
        self.actions = actions
        self.default_action = default_action


class ThreeModeChunkedDelayHandler(ThreeModeHandler):
    def handle_second(self, raw_request, stream):
        stream.write_response_line(self.config.response.response_line)
        stream.write_headers(self.config.response.headers)
        for chunk in self.config.response.data.chunks:
            stream.write_chunk(chunk)
            time.sleep(self.config.chunk_timeout)
        self.finish_response()


class ThreeModeChunkedDelayConfig(ThreeModeConfig):
    HANDLER_TYPE = ThreeModeChunkedDelayHandler

    def __init__(self, prefix=0, first=10, second=10, response=None, chunk_timeout=0):
        super(ThreeModeChunkedDelayConfig, self).__init__(
            prefix=prefix, first=first, second=second, response=response)
        self.chunk_timeout = chunk_timeout


class SlowBackendReadHandler(HTTPServerHandler):
    def __init__(self, state, sock, config):
        super(SlowBackendReadHandler, self).__init__(state, sock, config)
        if config.recv_buffer_size is not None:
            sock.set_recv_buffer_size(config.recv_buffer_size)

    """
    Slowly reads the request
    """
    def handle_request(self, stream):
        if self.config.headers_delay:
            time.sleep(self.config.headers_delay)
        stream.read_request_line()
        stream.read_headers()

        if self.config.response_before_body:
            stream.write_response(self.config.response)

        if stream.chunk_read_pending():
            while stream.chunk_read_pending():
                if self.config.chunk_delay:
                    time.sleep(self.config.chunk_delay)
                stream.read_chunk()
        else:
            if self.config.chunk_delay:
                time.sleep(self.config.chunk_delay)
            stream.read_data()

        raw_request = stream.get_message()
        self.append_request(raw_request)

        if not self.config.response_before_body:
            stream.write_response(self.config.response)


class SlowBackendReadConfig(StaticResponseConfig):
    HANDLER_TYPE = SlowBackendReadHandler

    def __init__(self, response=None, headers_delay=0, chunk_delay=0, response_before_body=False, recv_buffer_size=None):
        super(SlowBackendReadConfig, self).__init__(response)
        self.headers_delay = headers_delay
        self.chunk_delay = chunk_delay
        self.response_before_body = response_before_body
        self.recv_buffer_size=recv_buffer_size


class ContinueHandler(HTTPServerHandler):
    def handle_request(self, stream):
        stream.read_request_line()
        stream.read_headers()
        stream.write_response(self.config.continue_response)
        stream.read_data()
        self.append_request(stream.request)
        stream.write_response(self.config.response)


class ContinueConfig(StaticResponseConfig):
    HANDLER_TYPE = ContinueHandler

    def __init__(self, continue_response=None, response=None):
        super(ContinueConfig, self).__init__(response=response)
        if isinstance(continue_response, http_message.HTTPResponse):
            self.continue_response = continue_response.to_raw_response()
        elif isinstance(continue_response, http_message.RawHTTPResponse):
            self.continue_response = continue_response
        else:
            raise HandlerConfigException('bad response')
        if self.continue_response.response_line.status != 100:
            raise HandlerConfigException('bad response status')


class NoReadHandler(HTTPServerHandler):

    def handle_request(self, stream):
        stream.read_request_line()
        stream.read_headers()
        stream.write_response(self.config.response)
        if self.config.force_close:
            self.force_close()
        else:
            stream.read_request()


class NoReadConfig(SimpleConfig):
    HANDLER_TYPE = NoReadHandler

    def __init__(self, force_close, response):
        super(NoReadConfig, self).__init__(response=response)
        self.force_close = force_close


class MultiResponseHandler(PreparseHandler):
    """
    Reads request and writes a matching static response defined in config.
    """
    def handle_parsed_request(self, raw_request, stream):
        resp = self.config.responses[raw_request.request_line.path]
        stream.write_response(resp)
        self.finish_response_impl(resp)


class MultiResponseConfig(HTTPConfig):
    HANDLER_TYPE = MultiResponseHandler

    def __init__(self, responses):
        super(MultiResponseConfig, self).__init__()
        self.responses = {}
        for k, v in responses.items():
            if isinstance(v, http_message.HTTPResponse):
                v = v.to_raw_response()
            self.responses[k] = v
