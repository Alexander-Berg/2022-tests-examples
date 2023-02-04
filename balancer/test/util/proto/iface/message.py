# -*- coding: utf-8 -*-
from balancer.test.util.proto.iface import headers as hdr


class BaseMessage(object):
    def __init__(self, data, frames=None):
        super(BaseMessage, self).__init__()
        self.__data = data
        self.__frames = frames

    @property
    def headers(self):
        """
        :rtype: AbstractHeaders
        """
        raise NotImplementedError()

    @property
    def data(self):
        """
        :rtype: AbstractData
        """
        return self.__data

    @property
    def frames(self):
        return self.__frames

    def _build_data(self, data):
        raise NotImplementedError()

    def __eq__(self, other):
        return \
            isinstance(other, BaseMessage) and \
            self.headers == other.headers and \
            self.data.content == other.data.content

    def __ne__(self, other):
        return not self == other


class Message(BaseMessage):
    def __init__(self, headers, data, frames=None):
        super(Message, self).__init__(data, frames)
        if isinstance(headers, hdr.Headers):
            self.__headers = headers
        else:
            self.__headers = hdr.Headers(headers)

    @property
    def headers(self):
        """
        :rtype: Headers
        """
        return self.__headers

    @staticmethod
    def is_raw_message():
        return False


class RawMessage(BaseMessage):
    def __init__(self, headers, data, frames=None):
        super(RawMessage, self).__init__(data, frames)
        if isinstance(headers, hdr.RawHeaders):
            self.__headers = headers
        else:
            self.__headers = hdr.RawHeaders(headers)

    @property
    def headers(self):
        """
        :rtype: RawHeaders
        """
        return self.__headers

    @staticmethod
    def is_raw_message():
        return True


class RequestLine(object):
    pass


class Request(Message):
    @property
    def request_line(self):
        """
        :rtype: RequestLine
        """
        raise NotImplementedError()

    def __eq__(self, other):
        return \
            isinstance(other, Request) and \
            super(Request, self).__eq__(other) and \
            self.request_line == other.request_line

    def __repr__(self):
        return '\r\n'.join((repr(self.request_line), repr(self.headers), repr(self.data)))


class StatusLine(object):
    pass


class Response(Message):
    @property
    def status_line(self):
        """
        :rtype: StatusLine
        """
        raise NotImplementedError()

    @property
    def response_line(self):
        """Alias for status_line

        :rtype: StatusLine
        """
        return self.status_line

    def __eq__(self, other):
        return \
            isinstance(other, Response) and \
            super(Response, self).__eq__(other) and \
            self.status_line == other.status_line

    def __repr__(self):
        return '\r\n'.join((repr(self.response_line), repr(self.headers), repr(self.data)))


class RawMessageBuilder(object):
    def __init__(self):
        super(RawMessageBuilder, self).__init__()
        self.__headers = list()
        self.__data = None
        self.__raw_data = None

    def add_header(self, name, value):
        self.__headers.append((name, value))
        return self

    def add_headers(self, headers):
        if isinstance(headers, dict):
            headers = headers.iteritems()
        for name, value in headers:
            self.add_header(name, value)
        return self

    def build_headers(self):
        return hdr.RawHeaders(self.__headers)

    def set_data(self, data):
        self.__data = data
        return self

    def set_raw_data(self, raw_data):
        self.__raw_data = raw_data
        return self

    def build_data(self):
        return self.__data

    @property
    def _raw_data(self):
        return self.__raw_data

    def build(self):
        """
        :rtype: RawMessage
        """
        raise NotImplementedError()
