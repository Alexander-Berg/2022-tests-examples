# -*- coding: utf-8 -*-
from balancer.test.util.proto.http import message as mod_msg


def __message(msg_type, method, path, version, headers, data):  # pylint: disable=too-many-arguments
    return msg_type(
        mod_msg.HTTPRequestLine(method, path, version),
        headers,
        data,
    )


def __get(msg_type, path, version, headers, data):
    return __message(msg_type, 'GET', path, version, headers, data)


def get(path='/', version='HTTP/1.1', headers=None, data=None):
    """Create GET request

    :param str path: request path
    :param str version: protocol version
    :type headers: dict or iterable of pairs (name, value)
    :param headers: message headers
    :type data: None, str, list of str
    :param data: message data. None for no data, string for plain data, list of strings for chunked data. If data
        is chunked and there is no empty chunk at the end, then empty chunk wold be added.

    :rtype: HTTPRequest
    """
    return __get(mod_msg.HTTPRequest, path, version, headers, data)


def raw_get(path='/', version='HTTP/1.1', headers=None, data=None):
    """Create GET raw request

    :param str path: request path
    :param str version: protocol version
    :type headers: dict or iterable of pairs (name, value)
    :param headers: message headers
    :type data: None, str, list of str
    :param data: message data. None for no data, string for plain data, list of strings for chunked data. If data
        is chunked and there is no empty chunk at the end, then empty chunk wold be added.

    :rtype: RawHTTPRequest
    """
    return __get(mod_msg.RawHTTPRequest, path, version, headers, data)


def __post(msg_type, path, version, headers, data):
    return __message(msg_type, 'POST', path, version, headers, data)


def post(path='/', version='HTTP/1.1', headers=None, data=''):
    """Create POST request

    :param str path: request path
    :param str version: protocol version
    :type headers: dict or iterable of pairs (name, value)
    :param headers: message headers
    :type data: None, str, list of str
    :param data: message data. None for no data, string for plain data, list of strings for chunked data. If data
        is chunked and there is no empty chunk at the end, then empty chunk wold be added.

    :rtype: HTTPRequest
    """
    return __post(mod_msg.HTTPRequest, path, version, headers, data)


def raw_post(path='/', version='HTTP/1.1', headers=None, data=''):
    """Create POST raw request

    :param str path: request path
    :param str version: protocol version
    :type headers: dict or iterable of pairs (name, value)
    :param headers: message headers
    :type data: None, str, list of str
    :param data: message data. None for no data, string for plain data, list of strings for chunked data. If data
        is chunked and there is no empty chunk at the end, then empty chunk wold be added.

    :rtype: RawHTTPRequest
    """
    return __post(mod_msg.RawHTTPRequest, path, version, headers, data)


def __head(msg_type, path, version, headers, data):
    return __message(msg_type, 'HEAD', path, version, headers, data)


def head(path='/', version='HTTP/1.1', headers=None, data=None):
    """Create HEAD request

    :param str path: request path
    :param str version: protocol version
    :type headers: dict or iterable of pairs (name, value)
    :param headers: message headers
    :type data: None, str, list of str
    :param data: message data. None for no data, string for plain data, list of strings for chunked data. If data
        is chunked and there is no empty chunk at the end, then empty chunk wold be added.

    :rtype: HTTPRequest
    """
    return __head(mod_msg.HTTPRequest, path, version, headers, data)


def raw_head(path='/', version='HTTP/1.1', headers=None, data=None):
    """Create HEAD raw request

    :param str path: request path
    :param str version: protocol version
    :type headers: dict or iterable of pairs (name, value)
    :param headers: message headers
    :type data: None, str, list of str
    :param data: message data. None for no data, string for plain data, list of strings for chunked data. If data
        is chunked and there is no empty chunk at the end, then empty chunk wold be added.

    :rtype: RawHTTPRequest
    """
    return __head(mod_msg.RawHTTPRequest, path, version, headers, data)


def __put(msg_type, path, version, headers, data):
    return __message(msg_type, 'PUT', path, version, headers, data)


def put(path='/', version='HTTP/1.1', headers=None, data=''):
    """Create PUT request

    :param str path: request path
    :param str version: protocol version
    :type headers: dict or iterable of pairs (name, value)
    :param headers: message headers
    :type data: None, str, list of str
    :param data: message data. None for no data, string for plain data, list of strings for chunked data. If data
        is chunked and there is no empty chunk at the end, then empty chunk wold be added.

    :rtype: HTTPRequest
    """
    return __put(mod_msg.HTTPRequest, path, version, headers, data)


def raw_put(path='/', version='HTTP/1.1', headers=None, data=''):
    """Create PUT raw request

    :param str path: request path
    :param str version: protocol version
    :type headers: dict or iterable of pairs (name, value)
    :param headers: message headers
    :type data: None, str, list of str
    :param data: message data. None for no data, string for plain data, list of strings for chunked data. If data
        is chunked and there is no empty chunk at the end, then empty chunk wold be added.

    :rtype: RawHTTPRequest
    """
    return __put(mod_msg.RawHTTPRequest, path, version, headers, data)


def __patch(msg_type, path, version, headers, data):
    return __message(msg_type, 'PATCH', path, version, headers, data)


def patch(path='/', version='HTTP/1.1', headers=None, data=''):
    """Create PATCH request

    :param str path: request path
    :param str version: protocol version
    :type headers: dict or iterable of pairs (name, value)
    :param headers: message headers
    :type data: None, str, list of str
    :param data: message data. None for no data, string for plain data, list of strings for chunked data. If data
        is chunked and there is no empty chunk at the end, then empty chunk wold be added.

    :rtype: HTTPRequest
    """
    return __patch(mod_msg.HTTPRequest, path, version, headers, data)


def raw_patch(path='/', version='HTTP/1.1', headers=None, data=''):
    """Create PATCH raw request

    :param str path: request path
    :param str version: protocol version
    :type headers: dict or iterable of pairs (name, value)
    :param headers: message headers
    :type data: None, str, list of str
    :param data: message data. None for no data, string for plain data, list of strings for chunked data. If data
        is chunked and there is no empty chunk at the end, then empty chunk wold be added.

    :rtype: RawHTTPRequest
    """
    return __patch(mod_msg.RawHTTPRequest, path, version, headers, data)


def __trace(msg_type, path, version, headers, data):
    return __message(msg_type, 'TRACE', path, version, headers, data)


def trace(path='/', version='HTTP/1.1', headers=None, data=None):
    """Create TRACE request

    :param str path: request path
    :param str version: protocol version
    :type headers: dict or iterable of pairs (name, value)
    :param headers: message headers
    :type data: None, str, list of str
    :param data: message data. None for no data, string for plain data, list of strings for chunked data. If data
        is chunked and there is no empty chunk at the end, then empty chunk wold be added.

    :rtype: HTTPRequest
    """
    return __trace(mod_msg.HTTPRequest, path, version, headers, data)


def raw_trace(path='/', version='HTTP/1.1', headers=None, data=None):
    """Create TRACE raw request

    :param str path: request path
    :param str version: protocol version
    :type headers: dict or iterable of pairs (name, value)
    :param headers: message headers
    :type data: None, str, list of str
    :param data: message data. None for no data, string for plain data, list of strings for chunked data. If data
        is chunked and there is no empty chunk at the end, then empty chunk wold be added.

    :rtype: RawHTTPRequest
    """
    return __trace(mod_msg.RawHTTPRequest, path, version, headers, data)


def custom(method, path='/', version='HTTP/1.1', headers=None, data=None):
    """Create custom request

    :param str path: request path
    :param str version: protocol version
    :type headers: dict or iterable of pairs (name, value)
    :param headers: message headers
    :type data: None, str, list of str
    :param data: message data. None for no data, string for plain data, list of strings for chunked data. If data
        is chunked and there is no empty chunk at the end, then empty chunk wold be added.

    :rtype: HTTPRequest
    """
    return __message(mod_msg.HTTPRequest, method, path, version, headers, data)


def raw_custom(method, path='/', version='HTTP/1.1', headers=None, data=None):
    """Create custom raw request

    :param str path: request path
    :param str version: protocol version
    :type headers: dict or iterable of pairs (name, value)
    :param headers: message headers
    :type data: None, str, list of str
    :param data: message data. None for no data, string for plain data, list of strings for chunked data. If data
        is chunked and there is no empty chunk at the end, then empty chunk wold be added.

    :rtype: RawHTTPRequest
    """
    return __message(mod_msg.RawHTTPRequest, method, path, version, headers, data)
