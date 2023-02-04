# -*- coding: utf-8 -*-
import re
from collections import namedtuple

from balancer.test.util.proto.ws.stream import WSStream
from balancer.test.util.proto.iface.message import RawMessageBuilder
import balancer.test.util.proto.http.message as http_message
import balancer.test.util.proto.iface.data as d
from balancer.test.util.stream.io import stream, byte


VERSIONS = ['HTTP/1.0', 'HTTP/1.1']


class RawHTTPRequestBuilder(RawMessageBuilder):
    def __init__(self):
        super(RawHTTPRequestBuilder, self).__init__()
        self.__method = None
        self.__path = None
        self.__version = None

    def set_method(self, method):
        self.__method = method
        return self

    def set_path(self, path):
        self.__path = path
        return self

    def set_version(self, version):
        self.__version = version
        return self

    def build_request_line(self):
        return http_message.HTTPRequestLine(self.__method, self.__path, self.__version)

    def build(self):
        result = http_message.RawHTTPRequest(
            self.build_request_line(),
            self.build_headers(),
            self.build_data(),
        )
        result.data._set_raw(self._raw_data)
        return result


class RawHTTPResponseBuilder(RawMessageBuilder):
    def __init__(self):
        super(RawHTTPResponseBuilder, self).__init__()
        self.__version = None
        self.__status = None
        self.__reason_phrase = None

    def set_version(self, version):
        self.__version = version
        return self

    def set_status(self, status):
        self.__status = status
        return self

    def set_reason_phrase(self, reason_phrase):
        self.__reason_phrase = reason_phrase
        return self

    def build_response_line(self):
        return http_message.HTTPStatusLine(self.__version, self.__status, self.__reason_phrase)

    def build(self):
        result = http_message.RawHTTPResponse(
            self.build_response_line(),
            self.build_headers(),
            self.build_data(),
        )
        result.data._set_raw(self._raw_data)
        return result


class _Result(object):
    def __init__(self, value):
        super(_Result, self).__init__()
        self.value = value


class _Ok(_Result):
    pass


class _Err(_Result):
    def __init__(self, value, msg):
        super(_Err, self).__init__(value)
        self.msg = msg


class HTTPReaderStateException(Exception):
    pass


HTTPReadError = namedtuple('HTTPReadError', ['raw_message', 'tail', 'state'])


class HTTPReaderException(AssertionError):
    def __init__(self, read_error, msg):
        super(HTTPReaderException, self).__init__(msg)
        self.read_error = read_error


class HTTPState(object):
    TERMINAL = False

    def __init__(self, reader, builder):
        super(HTTPState, self).__init__()
        self._reader = reader
        self._builder = builder

    def _error(self, tail, msg):
        return HTTPErrorState(self._reader, self._builder, self, tail, msg)

    def _read_line(self):
        try:
            return _Ok(self._reader.read_line())
        except stream.EndOfStream, err:
            return _Err(err.data, 'connection unexpectedly closed')
        except stream.StreamTimeout, err:
            return _Err(err.data, 'connection timed out')
        except stream.StreamRst, err:
            return _Err(err.data, 'connection reset')

    def _match_regexp(self, regexp, data):
        res = regexp.match(data)
        if res is None:
            return _Err(data, 'cannot match: {}'.format(data))  # FIXME regexp in error msg
        else:
            return _Ok(res.groups())

    def _parse_int(self, value, base=10, msg=None):
        try:
            return _Ok(int(value, base=base))
        except ValueError, err:
            if msg is None:
                msg = err.message
            return _Err(value, msg)

    def handle_input(self):
        raise NotImplementedError()


class HTTPStartingLineState(HTTPState):
    pass


class HTTPRequestLineState(HTTPStartingLineState):
    REQUEST = re.compile(r'''(\S+)\ +  # method
                             (\S+)\ +  # path
                             (\S+)     # HTTP version
                             ''', re.VERBOSE)

    def __init__(self, reader):
        super(HTTPRequestLineState, self).__init__(reader, RawHTTPRequestBuilder())

    def handle_input(self):
        res_req_line = self._read_line()
        if isinstance(res_req_line, _Err):
            return self._error(res_req_line.value, 'Cannot read request line: {}'.format(res_req_line.msg))

        match = self._match_regexp(self.REQUEST, res_req_line.value)
        if isinstance(match, _Err):
            return self._error(match.value, 'Cannot parse request line: {}'.format(match.msg))

        method, path, version = match.value
        if version not in VERSIONS:
            return self._error('', 'Unsupported protocol version: "{}"'.format(version))

        self._builder.\
            set_method(method).\
            set_path(path).\
            set_version(version)
        return HTTPRequestHeaderState(self._reader, self._builder)


class HTTPResponseLineState(HTTPStartingLineState):
    RESPONSE = re.compile(r'''^(\S+)\ +    # HTTP version
                               (\d+)\ *    # status code
                               ([^\r\n]*)  # reason phrase
                               ''', re.VERBOSE)

    def __init__(self, reader, request=None):
        super(HTTPResponseLineState, self).__init__(reader, RawHTTPResponseBuilder())
        self.__request = request

    def handle_input(self):
        res_resp_line = self._read_line()
        if isinstance(res_resp_line, _Err):
            return self._error(res_resp_line.value, 'Cannot read response line: {}'.format(res_resp_line.msg))

        match = self._match_regexp(self.RESPONSE, res_resp_line.value)
        if isinstance(match, _Err):
            return self._error(match.value, 'Cannot parse response line: {}'.format(match.msg))

        version, status, reason_phrase = match.value
        if version not in VERSIONS:
            return self._error('', 'Unsupported protocol version: "{}"'.format(version))
        status = self._parse_int(status)
        if isinstance(status, _Err):
            return self._error('', 'Bad status code: "{}"'.format(status.value))

        self._builder.\
            set_version(version).\
            set_status(status.value).\
            set_reason_phrase(reason_phrase)
        return HTTPResponseHeaderState(self._reader, self._builder, self.__request)


class HTTPHeaderState(HTTPState):
    HEADER = re.compile(r'''^([^\s:]+)\s*  # header name
                            :\s*
                            (.*)           # header value
                            ''', re.VERBOSE)

    def _data_state(self):
        raise NotImplementedError()

    def handle_input(self):
        res_line = self._read_line()
        if isinstance(res_line, _Err):
            return self._error(res_line.value, 'cannot read header: {}'.format(res_line.msg))
        line = res_line.value
        if not line:
            self._reader.sock.clean()
            return self._data_state()
        else:
            match = self._match_regexp(self.HEADER, line)
            if isinstance(match, _Err):
                return self._error(line, 'cannot parse header: {}'.format(line))
            name, value = match.value
            self._builder.add_header(name, value)
            return self


class HTTPRequestHeaderState(HTTPHeaderState):
    def _data_state(self):
        version = self._builder.build_request_line().version
        headers = self._builder.build_headers()
        has_length = 'content-length' in headers
        if version == 'HTTP/1.0':
            if has_length:
                length = self._parse_int(headers.get_one('content-length'))  # FIXME check multiple content-length headers
                if isinstance(length, _Err):
                    return self._error('', 'Bad content-length: {}'.format(length.value))
                return HTTPPlainLengthDataState(self._reader, self._builder, length.value)
            else:
                return HTTPEmptyDataState(self._reader, self._builder)
        else:
            is_chunked = 'chunked' in headers.get_all('transfer-encoding')
            if is_chunked:
                return HTTPChunkedDataState(self._reader, self._builder)
            elif has_length:
                length = self._parse_int(headers.get_one('content-length'))  # TODO get rid of copypaste
                if isinstance(length, _Err):
                    return self._error('', 'Bad content-length: {}'.format(length.value))
                return HTTPPlainLengthDataState(self._reader, self._builder, length.value)
            else:
                return HTTPEmptyDataState(self._reader, self._builder)


class HTTPResponseHeaderState(HTTPHeaderState):
    def __init__(self, reader, builder, request):
        super(HTTPResponseHeaderState, self).__init__(reader, builder)
        self.__request = request

    def _data_state(self):
        resp_line = self._builder.build_response_line()
        headers = self._builder.build_headers()
        has_length = 'content-length' in headers
        empty_data = (
            (self.__request is not None and self.__request.request_line.method == 'HEAD') or
            resp_line.status / 100 == 1 or
            resp_line.status == 204 or
            resp_line.status == 304
        )
        if empty_data:
            return HTTPEmptyDataState(self._reader, self._builder)
        if resp_line.version == 'HTTP/1.0':
            if has_length:
                length = self._parse_int(headers.get_one('content-length'))  # TODO get rid of copypaste
                if isinstance(length, _Err):
                    return self._error('', 'Bad content-length: {}'.format(length.value))
                return HTTPPlainLengthDataState(self._reader, self._builder, length.value)
            else:
                return HTTPPlainCloseDataState(self._reader, self._builder)
        else:
            is_chunked = 'chunked' in headers.get_all('transfer-encoding')
            if is_chunked:
                return HTTPChunkedDataState(self._reader, self._builder)
            elif has_length:
                length = self._parse_int(headers.get_one('content-length'))  # TODO get rid of copypaste
                if isinstance(length, _Err):
                    return self._error('', 'Bad content-length: {}'.format(length.value))
                return HTTPPlainLengthDataState(self._reader, self._builder, length.value)
            else:
                return HTTPPlainCloseDataState(self._reader, self._builder)


class HTTPDataState(HTTPState):
    pass


class HTTPEmptyDataState(HTTPDataState):
    def handle_input(self):
        self._builder.set_data(d.EmptyData())
        return HTTPFinState(self._reader, self._builder)


class HTTPPlainCloseDataState(HTTPDataState):
    def handle_input(self):
        try:
            data = self._reader.read()
            self._builder.set_data(data)
            return HTTPFinState(self._reader, self._builder)
        except stream.StreamTimeout, err:
            self._builder.set_data(err.data)
            return self._error('', 'EOF not received')
        except stream.StreamRst, err:
            self._builder.set_data(err.data)
            return self._error('', 'connection reset')


class HTTPPlainLengthDataState(HTTPDataState):
    def __init__(self, reader, builder, length):
        super(HTTPPlainLengthDataState, self).__init__(reader, builder)
        self.__data = list()
        self.__length = length

    def read(self, length):
        if length > self.__length:
            raise HTTPReaderStateException('Invalid length')
        try:
            data = self._reader.read(length)
            self.__data.append(data)
            self.__length -= length
            if self.__length == 0:
                self._builder.set_data(''.join(self.__data))
                return data, HTTPFinState(self._reader, self._builder)
            else:
                return data, self
        except stream.StreamRecvException, err:
            self.__data.append(err.data)
            self._builder.set_data(''.join(self.__data))
            return None, self._error('', 'Cannot receive data')

    def handle_input(self):
        return self.read(self.__length)[1]


class HTTPChunkedDataState(HTTPDataState):
    def __init__(self, reader, builder):
        super(HTTPChunkedDataState, self).__init__(reader, builder)
        self.__chunks = list()

    def _error(self, tail, msg):
        self._builder.set_data(self.__chunks)
        return super(HTTPChunkedDataState, self)._error(tail, msg)

    def read_chunk(self):
        res_chunk_length_str = self._read_line()
        if isinstance(res_chunk_length_str, _Err):
            return None, self._error(
                res_chunk_length_str.value,
                'Cannot read chunk length: {}'.format(res_chunk_length_str.msg)
            )
        chunk_length_str = res_chunk_length_str.value

        res_chunk_length = self._parse_int(chunk_length_str, base=16)
        if isinstance(res_chunk_length, _Err):
            return None, self._error(chunk_length_str, 'Cannot parse chunk length')
        chunk_length = res_chunk_length.value

        try:
            chunk_data = self._reader.read(chunk_length)
            chunk = d.Chunk(chunk_length, chunk_data)
            self.__chunks.append(chunk)
            try:
                newline = self._reader.read_line()
                if newline:
                    return None, self._error(newline,
                                             '"{}" found instead of newline at the end of chunk'.format(repr(newline)))
                if chunk_length == 0:
                    self._builder.set_data(self.__chunks)
                    return chunk, HTTPFinState(self._reader, self._builder)
                else:
                    return chunk, self
            except stream.StreamRecvException, err:
                return None, self._error(err.data, 'Newline not found at the end of chunk')
        except stream.StreamRecvException, err:
            self.__chunks.append(d.Chunk(chunk_length, err.data))
            return None, self._error('', 'Cannot receive chunk')

    def handle_input(self):
        chunk, state = self.read_chunk()
        while isinstance(state, HTTPChunkedDataState):
            chunk, state = self.read_chunk()
        return state


class HTTPFinState(HTTPState):
    TERMINAL = True

    def __init__(self, reader, builder):
        super(HTTPFinState, self).__init__(reader, builder)
        self._builder.set_raw_data(self._reader.sock.data)
        self.message = self._builder.build()


class HTTPErrorState(HTTPState):
    TERMINAL = True

    def __init__(self, reader, builder, state, tail, msg):
        super(HTTPErrorState, self).__init__(reader, builder)
        self._builder.set_raw_data(self._reader.sock.data)
        self.msg = msg
        self.state = state
        self.tail = tail
        self.raw_message = self._builder.build()


class HTTPStream(object):
    def __init__(self, reader, writer, state=None):
        super(HTTPStream, self).__init__()
        self._reader = reader
        self._writer = writer
        self._state = state

    def _check_state_type(self, state_type):
        if not isinstance(self._state, state_type):
            raise HTTPReaderStateException('bad state')

    def _check_error_state(self):
        if isinstance(self._state, HTTPErrorState):
            # TODO replace HTTPReadError with HTTPErrorState
            self._state.state._reader = None  # FIXME workaround to make state pickleable
            raise HTTPReaderException(
                HTTPReadError(self._state.raw_message, self._state.tail, self._state.state), self._state.msg)

    def _handle_input(self):
        self._state = self._state.handle_input()

    def _handle_input_check_error(self):
        self._handle_input()
        self._check_error_state()

    def _read_message(self):
        self._check_state_type(HTTPStartingLineState)
        while not self._state.TERMINAL:
            self._handle_input()
        self._check_error_state()
        return self.get_message()

    def read_headers(self):
        self._check_state_type(HTTPHeaderState)
        while isinstance(self._state, HTTPHeaderState):
            self._handle_input_check_error()

    def read_data(self):
        self._check_state_type(HTTPDataState)
        self._handle_input_check_error()

    def get_message(self):
        if isinstance(self._state, HTTPFinState):
            return self._state.message
        else:
            raise HTTPReaderStateException('what?!')

    def chunk_read_pending(self):
        return isinstance(self._state, HTTPChunkedDataState)

    def read_chunk(self):
        self._check_state_type(HTTPChunkedDataState)
        _, self._state = self._state.read_chunk()
        self._check_error_state()

    def write(self, data):
        """Write raw data"""
        self._writer.write(data)

    def write_line(self, data):
        """Write data, followed by newline"""
        self._writer.write_line(data)

    def write_header(self, name, value):
        """
        :type name: str
        :param name: header name
        :type value: str
        :param value: header value
        """
        self._writer.write_line('{}: {}'.format(name, value))

    def write_headers(self, headers):
        """Write message headers, followed by empty line

        :type headers: RawHeaders
        :param headers: raw HTTP headers
        """
        for name, value in headers.items():
            self.write_header(name, value)
        self.end_headers()

    def end_headers(self):
        """Write empty line after message headers"""
        self._writer.write_line('')

    def write_chunk(self, chunk):
        """
        :type chunk: str, Chunk
        :param chunk: data chunk
        """
        self._writer.write_line('%X' % len(chunk))
        self._writer.write_line(str(chunk))

    def write_data(self, data):
        """
        :type data: AbstractData
        :param data: message body
        """
        if isinstance(data, d.PlainData) and data.content:
            self._writer.write(data.content)
        elif isinstance(data, d.ChunkedData):
            for chunk in data.chunks:
                self.write_chunk(chunk)


class HTTPServerStream(HTTPStream):
    def __init__(self, reader, writer):
        super(HTTPServerStream, self).__init__(reader, writer, HTTPRequestLineState(reader))

    @property
    def request(self):
        """
        Request, received by server

        :rtype: RawHTTPRequest
        """
        if isinstance(self._state, HTTPFinState):
            return self._state.message
        else:
            raise HTTPReaderStateException('request has not been read yet')

    def read_request_line(self):
        self._check_state_type(HTTPRequestLineState)
        self._handle_input_check_error()

    def read_request(self):
        """
        :rtype: RawHTTPRequest
        """
        return self._read_message()

    def write_response_line(self, response_line):
        """
        :type response_line: HTTPResponseLine
        :param response_line: HTTP response line
        """
        self._writer.write_line(str(response_line))

    def write_response(self, response):
        """
        :type response: RawHTTPResponse
        :param response: raw HTTP response
        """
        assert isinstance(response, http_message.RawHTTPResponse), 'wrong response type: expected {}, found {}' \
            .format(http_message.RawHTTPResponse, type(response))
        self.write_response_line(response.response_line)
        self.write_headers(response.headers)
        self.write_data(response.data)


class HTTPClientStream(HTTPStream):
    def __init__(self, reader, writer):
        super(HTTPClientStream, self).__init__(reader, writer)
        self.__request = None

    def __init_next_response(self):
        if self._state is not None:
            self._check_state_type(HTTPFinState)
        self._state = HTTPResponseLineState(self._reader, self.__request)

    def read_response_line(self):
        self.__init_next_response()
        self._handle_input_check_error()

    def read_response(self):
        """
        :rtype: RawHTTPResponse
        """
        return self.read_next_response()

    def read_next_response(self):
        """
        :rtype: RawHTTPResponse
        """
        self.__init_next_response()
        return self._read_message()

    def read_all_responses(self):
        """
        :rtype: list of RawHTTPResponse
        """
        response = self.read_next_response()
        result = [response]
        while response.response_line.status == 100:
            response = self.read_next_response()
            result.append(response)
        return result

    def write_request_line(self, request_line):
        """
        :type request_line: HTTPRequestLine
        :param request_line: HTTP request line
        """
        self._writer.write_line(str(request_line))

    def write_request(self, request):
        """
        :type request: RawHTTPRequest
        :param request: raw HTTP request
        """
        assert isinstance(request, http_message.RawHTTPRequest), 'wrong request type: expected {}, found {}' \
            .format(http_message.RawHTTPRequest, type(request))
        self.__request = request
        self.write_request_line(request.request_line)
        self.write_headers(request.headers)
        self.write_data(request.data)

    def switch_ws(self):
        return WSStream(self._reader, self._writer)


def parse_request(raw_str):
    server_stream = HTTPServerStream(
        byte.ByteReader(stream.StringStream(raw_str), newline=['\r\n', '\n']),
        byte.ByteReader(stream.StringStream('')))
    return server_stream.read_request()


def serialize_request(request):
    if isinstance(request, http_message.HTTPRequest):
        request = request.to_raw_request()
    sstream = stream.StringStream('')
    client_stream = HTTPClientStream(byte.ByteReader(stream.StringStream('')), byte.ByteWriter(sstream))
    client_stream.write_request(request)
    return str(sstream)


def parse_response(raw_str, request=None):
    client_stream = HTTPClientStream(
        byte.ByteReader(stream.StringStream(raw_str), newline=['\r\n', '\n']),
        byte.ByteReader(stream.StringStream('')),
    )
    if request is not None:
        client_stream.write_request(request)
    return client_stream.read_response()


def serialize_response(response, request=None):
    if isinstance(response, http_message.HTTPResponse):
        response = response.to_raw_response()
    sstream = stream.StringStream('')
    if request is not None:
        server_stream = HTTPServerStream(
            byte.ByteReader(stream.StringStream(serialize_request(request))),
            byte.ByteWriter(sstream)
        )
        server_stream.read_request()
    else:
        server_stream = HTTPServerStream(
            byte.ByteReader(stream.StringStream('')),
            byte.ByteWriter(sstream)
        )
    server_stream.write_response(response)
    return str(sstream)
