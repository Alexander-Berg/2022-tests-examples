# -*- coding: utf-8 -*-
import urlparse

from balancer.test.util.proto.iface import message as mod_msg
from balancer.test.util.proto.iface import data as mod_data


__HOP_BY_HOP_HEADERS = [
    'connection',
    'content-length',
    'transfer-encoding',
]


def _build_raw_headers(msg, version):
    raw_headers = list()

    for name in msg.headers.get_names():
        for value in msg.headers.get_all(name):
            raw_headers.append((name, value))

    if version == 'HTTP/1.0':
        raw_headers.append(('connection', 'keep-alive'))

    if isinstance(msg.data, mod_data.PlainData):
        raw_headers.append(('content-length', len(msg.data.content)))
    elif isinstance(msg.data, mod_data.ChunkedData):
        raw_headers.append(('transfer-encoding', 'chunked'))

    return raw_headers


def _filter_hop_by_hop_headers(raw_headers):
    return [(name, value) for name, value in raw_headers.items() if name.lower() not in __HOP_BY_HOP_HEADERS]


def build_data(data):
    """
    :type data: None, str, list of str
    :param data: message data. None for no data, string for plain data, list of strings for chunked data.

    :return: AbstractData
    """
    if isinstance(data, mod_data.AbstractData):
        return data
    elif data is None:
        return mod_data.EmptyData()
    elif isinstance(data, unicode):
        return mod_data.PlainData(data.encode('utf-8'))
    elif isinstance(data, str):
        return mod_data.PlainData(data)
    elif isinstance(data, list):
        if (len(data) == 0 or data[-1] != ''):
            data = data + ['']
        return mod_data.ChunkedData(data)
    else:
        raise mod_data.WrongDataTypeException('invalid data type: {}'.format(type(data)))


class HTTPRequestLine(mod_msg.RequestLine):
    def __init__(self, method, path, version):
        super(HTTPRequestLine, self).__init__()
        self.__method = method
        self.__path = path
        self.__version = version
        self.__cgi = None

    @property
    def method(self):
        """
        :rtype: str
        """
        return self.__method

    @property
    def path(self):
        """
        :rtype: str
        """
        return self.__path

    @property
    def cgi(self):
        """
        :rtype: dict
        """
        if self.__cgi is None:
            query = urlparse.urlparse(self.path).query
            self.__cgi = urlparse.parse_qs(query, keep_blank_values=True)
        return self.__cgi

    @property
    def version(self):
        """
        :rtype: str
        """
        return self.__version

    def __eq__(self, other):
        return \
            isinstance(other, HTTPRequestLine) and \
            self.method == other.method and \
            self.path == other.path and \
            self.version == other.version

    def __ne__(self, other):
        return not self == other

    def __repr__(self):
        return ' '.join([self.method, self.path, self.version])


class HTTPStatusLine(mod_msg.StatusLine):
    def __init__(self, version, status, reason_phrase):
        super(HTTPStatusLine, self).__init__()
        self.__version = version
        self.__status = status
        self.__reason_phrase = reason_phrase

    @property
    def version(self):
        """
        :rtype: str
        """
        return self.__version

    @property
    def status(self):
        """
        :rtype: int
        """
        return self.__status

    @property
    def reason_phrase(self):
        """
        :rtype: str
        """
        return self.__reason_phrase

    def __eq__(self, other):
        return \
            isinstance(other, HTTPStatusLine) and \
            self.version == other.version and \
            self.status == other.status and \
            self.reason_phrase == other.reason_phrase

    def __ne__(self, other):
        return not self == other

    def __repr__(self):
        return ' '.join([self.version, str(self.status), self.reason_phrase])


class HTTPRequest(mod_msg.Request):
    def __init__(self, request_line, headers, data):
        super(HTTPRequest, self).__init__(headers, build_data(data))
        self.__request_line = request_line

    @property
    def request_line(self):
        """
        :rtype: HTTPRequestLine
        """
        return self.__request_line

    def to_raw_request(self):
        """Convert to raw HTTP request

        :rtype: RawHTTPRequest
        """
        return RawHTTPRequest(
            self.request_line,
            _build_raw_headers(self, self.request_line.version),
            self.data,
        )


class HTTPResponse(mod_msg.Response):
    def __init__(self, status_line, headers, data):
        super(HTTPResponse, self).__init__(headers, build_data(data))
        self.__status_line = status_line

    @property
    def status_line(self):
        """
        :rtype: HTTPStatusLine
        """
        return self.__status_line

    @property
    def status(self):
        """Alias for status_line.status

        :rtype: int
        """
        return self.status_line.status

    def to_raw_response(self):
        """Convert to raw HTTP response

        :rtype: RawHTTPResponse
        """
        return RawHTTPResponse(
            self.status_line,
            _build_raw_headers(self, self.status_line.version),
            self.data,
        )


class RawHTTPRequest(mod_msg.RawMessage):
    def __init__(self, request_line, headers, data):
        super(RawHTTPRequest, self).__init__(
            headers,
            build_data(data),
        )
        self.__request_line = request_line

    @property
    def request_line(self):
        """
        :rtype: HTTPRequestLine
        """
        return self.__request_line

    def __eq__(self, other):
        return \
            isinstance(other, RawHTTPRequest) and \
            super(RawHTTPRequest, self).__eq__(other) and \
            self.request_line == other.request_line

    def __repr__(self):
        return '\r\n'.join((repr(self.request_line), repr(self.headers), repr(self.data)))

    def to_request(self):
        """Convert to HTTP request

        :rtype: HTTPRequest
        """
        return HTTPRequest(
            self.request_line,
            _filter_hop_by_hop_headers(self.headers),
            self.data,
        )


class RawHTTPResponse(mod_msg.RawMessage):
    def __init__(self, status_line, headers, data):
        super(RawHTTPResponse, self).__init__(
            headers,
            build_data(data),
        )
        self.__status_line = status_line

    @property
    def status_line(self):
        """
        :rtype: HTTPStatusLine
        """
        return self.__status_line

    response_line = status_line

    @property
    def status(self):
        """Alias for status_line.status

        :rtype: int
        """
        return self.status_line.status

    def __eq__(self, other):
        return \
            isinstance(other, RawHTTPResponse) and \
            super(RawHTTPResponse, self).__eq__(other) and \
            self.status_line == other.status_line

    def __repr__(self):
        return '\r\n'.join((repr(self.response_line), repr(self.headers), repr(self.data)))

    def to_response(self):
        """Convert to HTTP response

        :rtype: HTTPResponse
        """
        return HTTPResponse(
            self.status_line,
            _filter_hop_by_hop_headers(self.headers),
            self.data,
        )
