# -*- coding: utf-8 -*-
import urlparse

from balancer.test.util.proto.iface import message as mod_msg
from balancer.test.util.proto.iface import headers as mod_headers
from balancer.test.util.proto.iface import data as mod_data


def _build_raw_headers(msg, new_headers):
    result = list(new_headers)
    for name in msg.headers.get_names():
        result.extend([(name, value) for value in msg.headers.get_all(name)])
    return [HeaderField(name, value) for name, value in result]


def _from_raw_headers(raw_headers, skip_headers):
    result = list()
    for name, value in raw_headers.items():
        if name not in skip_headers:
            result.append((name, value))
    return result


def build_data(data):
    """
    :type data: None, str, list of str
    :param data: message data. List of strings converts to list of DataFrame.
        None equals to empty list, str equals to list with one string.

    :return: self
    """
    if isinstance(data, mod_data.ChunkedData):
        return data
    else:
        if data is None:
            msg_data = list()
        elif isinstance(data, str):
            msg_data = [data]
        elif isinstance(data, list):
            msg_data = data
        else:
            raise mod_data.WrongDataTypeException('invalid data type: {}'.format(data.__class__))
        return mod_data.ChunkedData(msg_data)


class HTTP2RequestLine(mod_msg.RequestLine):
    def __init__(self, method, scheme, authority, path):
        super(HTTP2RequestLine, self).__init__()
        self.__method = method
        self.__path = path
        self.__scheme = scheme
        self.__authority = authority
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
    def scheme(self):
        """
        :rtype: str
        """
        return self.__scheme

    @property
    def authority(self):
        """
        :rtype: str
        """
        return self.__authority

    def __eq__(self, other):
        return \
            isinstance(other, HTTP2RequestLine) and \
            self.method == other.method and \
            self.path == other.path and \
            self.scheme == other.scheme and \
            self.authority == other.authority

    def __ne__(self, other):
        return not self == other

    def __repr__(self):
        return ' '.join([self.method, self.path, self.scheme, self.authority])


class HTTP2StatusLine(mod_msg.StatusLine):
    def __init__(self, status):
        super(HTTP2StatusLine, self).__init__()
        self.__status = status

    @property
    def status(self):
        """
        :rtype: int
        """
        return self.__status

    def __eq__(self, other):
        return \
            isinstance(other, HTTP2StatusLine) and \
            self.status == other.status

    def __ne__(self, other):
        return not self == other

    def __repr__(self):
        return str(self.status)


class HTTP2Request(mod_msg.Request):
    def __init__(self, request_line, headers, data, frames=None):
        super(HTTP2Request, self).__init__(headers, build_data(data))
        self.__request_line = request_line
        self.__frames = frames

    @property
    def request_line(self):
        """
        :rtype: HTTP2RequestLine
        """
        return self.__request_line

    def to_raw_request(self):
        """Convert to raw HTTP2 request

        :rtype: RawHTTP2Request
        """
        return RawHTTP2Message(
            _build_raw_headers(self, [
                (':method', self.request_line.method),
                (':scheme', self.request_line.scheme),
                (':authority', self.request_line.authority),
                (':path', self.request_line.path),
            ]),
            self.data,
            self.frames,
        )


class HTTP2Response(mod_msg.Response):
    def __init__(self, status_line, headers, data, frames=None):
        super(HTTP2Response, self).__init__(headers, build_data(data), frames)
        self.__status_line = status_line

    @property
    def status_line(self):
        """
        :rtype: HTTP2StatusLine
        """
        return self.__status_line

    @property
    def status(self):
        """Alias for status_line.status

        :rtype: int
        """
        return self.status_line.status

    def to_raw_response(self):
        """Convert to raw HTTP2 response

        :rtype: RawHTTP2Response
        """
        return RawHTTP2Message(
            _build_raw_headers(self, [
                (':status', self.status_line.status),
            ]),
            self.data,
            self.frames,
        )


class HElement(object):
    def __init__(self, value=None, compressed=False):
        super(HElement, self).__init__()
        self.__value = value
        self.__compressed = compressed

    @property
    def value(self):  # TODO: choose a better name for this property
        return self.__value

    @property
    def compressed(self):
        return self.__compressed


class HName(HElement):
    def __init__(self, value=None, index=None, compressed=False):
        super(HName, self).__init__(value, compressed)
        self.__index = index

    @property
    def index(self):
        return self.__index

    def __eq__(self, other):
        if not isinstance(other, HName):
            return False
        if self.value != other.value:
            return False
        if self.index is not None and other.index is not None and self.index != other.index:
            return False
        if self.compressed != other.compressed:
            return False
        return True

    def __repr__(self):
        if self.index is not None:
            prefix = '(idx={})'.format(self.index)
        elif self.compressed:
            prefix = '(Huffman)'
        else:
            prefix = ''
        return '{} {}'.format(prefix, self.value)


class HValue(HElement):
    def __eq__(self, other):
        return \
            isinstance(other, HValue) and \
            self.value == other.value and \
            self.compressed == other.compressed

    def __repr__(self):
        if self.compressed:
            prefix = '(Huffman)'
        else:
            prefix = ''
        return '{} {}'.format(prefix, self.value)


class Indexing(object):
    YES = 0
    NO = 1
    NEVER = 2


class HeaderField(mod_headers.HeaderField):
    def __init__(self, name, value=None, indexing=Indexing.NO, index=None):
        if isinstance(name, str):
            name = HName(name)
        elif isinstance(name, int):
            name = HName(index=name)
        elif not isinstance(name, HName):
            raise TypeError('name must be of type str, int or HName, not {}'.format(type(name)))

        if name.index is not None and index is not None:
            raise ValueError('bad indexed header field representation')
        if name.index is None and value is None and index is None:
            raise ValueError('header value not specified')

        if name.index is not None and value is None and index is None:
            if name.value is not None:
                raise ValueError('wrong header name')
            index = name.index
            name = HName(None)

        if index is not None and indexing == Indexing.YES:
            raise ValueError('indexing is not possible for indexed header field representation')

        self.__index = index
        self.__indexing = indexing

        if value is None:
            value = HValue(None)
        elif not isinstance(value, HValue):
            value = HValue(str(value))

        super(HeaderField, self).__init__(name, value)

    @property
    def index(self):
        return self.__index

    @property
    def name(self):
        return self._name.value

    @property
    def value(self):
        return self._value.value

    @property
    def name_element(self):
        return self._name

    @property
    def value_element(self):
        return self._value

    @property
    def indexing(self):
        return self.__indexing

    def __eq__(self, other):
        return \
            isinstance(other, HeaderField) and \
            self.name_element == other.name_element and \
            self.value_element == other.value_element and \
            self.indexing == other.indexing


class RawHTTP2Message(mod_msg.RawMessage):
    def __init__(self, headers, data, trailing_headers=None, frames=None):
        super(RawHTTP2Message, self).__init__(
            self.build_headers(headers),
            build_data(data),
            frames,
        )
        self.__trailing_headers = trailing_headers  # TODO: unused for now

    @staticmethod
    def build_headers(headers):
        result = list()
        for field in mod_headers.iter_headers(headers):
            if not isinstance(field, HeaderField):
                if isinstance(field, tuple):
                    field = HeaderField(field[0], field[1])
                else:
                    field = HeaderField(field)
            result.append(field)
        return result

    @property
    def trailing_headers(self):
        return self.__trailing_headers

    def __eq__(self, other):
        return \
            isinstance(other, RawHTTP2Message) and \
            super(RawHTTP2Message, self).__eq__(other)

    def __repr__(self):
        return '\r\n'.join((repr(self.headers), repr(self.data)))

    def to_request(self):
        """Convert to HTTP2 request

        :rtype: HTTP2Request
        """
        return HTTP2Request(
            HTTP2RequestLine(
                self.headers.get_one(':method'),
                self.headers.get_one(':scheme'),
                self.headers.get_one(':authority'),
                self.headers.get_one(':path'),
            ),
            _from_raw_headers(self.headers, [':method', ':scheme', ':authority', ':path']),
            self.data,
            self.frames,
        )

    def to_response(self):
        """Convert to HTTP2 response

        :rtype: HTTP2Response
        """
        return HTTP2Response(
            HTTP2StatusLine(
                int(self.headers.get_one(':status')),
            ),
            _from_raw_headers(self.headers, [':status']),
            self.data,
            self.frames,
        )
