# -*- coding: utf-8 -*-
from balancer.test.util.resource import AbstractResource
from balancer.test.util.stream.io.byte import ByteReader, ByteWriter
from balancer.test.util.proto.http.stream import HTTPClientStream
import balancer.test.util.proto.http.message as http_message


class HTTPConnection(AbstractResource):
    def __init__(self, sock, newline='\r\n'):
        super(HTTPConnection, self).__init__()
        self.__sock = sock
        self.__writer = ByteWriter(self.__sock, newline=newline)
        self.__reader = ByteReader(self.__sock, newline=newline)

    def perform_request(self, request):
        """
        :type request: :class:`.HTTPRequest` or :class:`.RawHTTPRequest`
        :param request: request to send

        :rtype: HTTPResponse
        """
        return self.perform_request_raw_response(request).to_response()

    def perform_request_raw_response(self, request):
        """
        :type request: :class:`.HTTPRequest` or :class:`.RawHTTPRequest`
        :param request: request to send

        :rtype: RawHTTPResponse
        """
        if isinstance(request, http_message.HTTPRequest):
            request = request.to_raw_request()
        stream = self.create_stream()
        stream.write_request(request)
        return stream.read_response()

    def create_stream(self):
        """
        :rtype: HTTPClientStream
        """
        return HTTPClientStream(self.__reader, self.__writer)

    close = AbstractResource.finish

    def _finish(self):
        self.__sock.close()

    @property
    def sock(self):
        return self.__sock
