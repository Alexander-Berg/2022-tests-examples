# -*- coding: utf-8 -*-
from abc import ABCMeta, abstractmethod

from datetime import datetime
from balancer.test.util.proto.handler.server._common import ConfigServerHandler, HandlerConfigException, Config,\
    RequestInfo, StreamInfo
from balancer.test.util.stream.io.byte import ByteReader, ByteWriter
from balancer.test.util.proto.http.stream import HTTPServerStream, HTTPReaderException
from balancer.test.util.proto.ws.stream import WSStream, WSReaderException
import balancer.test.util.proto.http.message as http_message


class HTTPServerHandler(ConfigServerHandler):
    __metaclass__ = ABCMeta
    POLL_TIMEOUT = 0.5

    def __init__(self, state, sock, config):
        super(HTTPServerHandler, self).__init__(state, sock, config)
        self.__sock_reader = ByteReader(self.sock)
        self.__sock_writer = ByteWriter(self.sock)
        self.__close_connection = False
        self.__start_time = None
        self.__websocket_mode = False

    def _ws_on(self):
        self.__websocket_mode = True

    def _ws_off(self):
        self.__websocket_mode = False

    def _ws_status(self):
        return self.__websocket_mode

    def force_close(self):
        """Close connection to client after handling current request"""
        self.__close_connection = True

    def handle(self):
        self.__close_connection = False
        while not self.__close_connection:
            while not self.sock.poll(self.POLL_TIMEOUT):
                pass
            if self.sock.has_data():
                try:
                    self.__start_time = datetime.now()
                    if self.__websocket_mode:
                        stream = WSStream(self.__sock_reader, self.__sock_writer)
                    else:
                        stream = HTTPServerStream(self.__sock_reader, self.__sock_writer)
                    self.handle_request(stream)
                    fin_time = datetime.now()
                    self.state.streams.put(StreamInfo(self.__start_time, fin_time))
                except (HTTPReaderException, WSReaderException) as exc:
                    self.state.read_errors.put(exc.read_error)
                    self.force_close()
            else:
                return

    def append_request(self, raw_request):
        """
        Append received request to queues in state

        :param RawHTTPRequest raw_request: received request
        """
        fin_time = datetime.now()
        self.state.requests.put(RequestInfo(raw_request, self.__start_time, fin_time))

    @abstractmethod
    def handle_request(self, stream):
        """
        Called for each new client request. Should be overridden in subclass

        :type stream: HTTPServerStream
        :param stream: server stream object
        """
        raise NotImplementedError()


class HTTPConfig(Config):
    def __init__(self):
        super(HTTPConfig, self).__init__()


class PreparseHandler(HTTPServerHandler):
    __metaclass__ = ABCMeta

    def handle_request(self, stream):
        raw_request = stream.read_request()
        self.append_request(raw_request)
        self.handle_parsed_request(raw_request, stream)

    @abstractmethod
    def handle_parsed_request(self, raw_request, stream):
        """
        :param RawHTTPRequest raw_request: received request
        :param HTTPServerStream stream: server stream object
        """
        raise NotImplementedError()

    # FIXME don't ignore request connection header
    def finish_response_impl(self, response):
        """
        Close or keep connection alive according to HTTP specification
        """
        conn_header = response.headers.get_one('connection')
        if conn_header is None:
            if response.response_line.version == 'HTTP/1.0':
                self.force_close()
        else:
            if conn_header.lower() == 'close':
                self.force_close()


class StaticResponseHandler(PreparseHandler):
    __metaclass__ = ABCMeta

    @abstractmethod
    def handle_parsed_request(self, raw_request, stream):
        raise NotImplementedError()

    # FIXME don't ignore request connection header
    def finish_response(self):
        self.finish_response_impl(self.config.response)


class StaticResponseConfig(HTTPConfig):
    def __init__(self, response):
        if isinstance(response, http_message.HTTPResponse):
            self.response = response.to_raw_response()
        elif isinstance(response, http_message.RawHTTPResponse):
            self.response = response
        else:
            raise HandlerConfigException('bad response: ' + str(response)[:512])

        super(StaticResponseConfig, self).__init__()
