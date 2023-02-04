# -*- coding: utf-8 -*-
from balancer.test.util.proto.http import message as mod_msg


def __custom(msg_type, status, reason, version, headers, data):
    return msg_type(
        mod_msg.HTTPStatusLine(version, status, reason),
        headers,
        data,
    )


def __ok(msg_type, version, headers, data):
    return __custom(msg_type, 200, 'OK', version, headers, data)


def ok(version='HTTP/1.1', headers=None, data=''):  # pylint: disable=invalid-name
    """Create 200 OK response

    :param str version: protocol version
    :type headers: dict or iterable of pairs (name, value)
    :param headers: message headers
    :type data: None, str, list of str
    :param data: message data. None for no data, string for plain data, list of strings for chunked data. If data
        is chunked and there is no empty chunk at the end, then empty chunk wold be added.

    :rtype: HTTPResponse
    """
    return __ok(mod_msg.HTTPResponse, version, headers, data)


def raw_ok(version='HTTP/1.1', headers=None, data=''):
    """Create 200 OK raw response

    :param str version: protocol version
    :type headers: dict or iterable of pairs (name, value)
    :param headers: message headers
    :type data: None, str, list of str
    :param data: message data. None for no data, string for plain data, list of strings for chunked data. If data
        is chunked and there is no empty chunk at the end, then empty chunk wold be added.

    :rtype: RawHTTPResponse
    """
    return __ok(mod_msg.RawHTTPResponse, version, headers, data)


def __no_content(msg_type, version, headers, data):
    return __custom(msg_type, 204, 'No Content', version, headers, data)


def no_content(version='HTTP/1.1', headers=None, data=None):  # pylint: disable=invalid-name
    """Create 204 No Content response

    :param str version: protocol version
    :type headers: dict or iterable of pairs (name, value)
    :param headers: message headers
    :type data: None, str, list of str
    :param data: message data. None for no data, string for plain data, list of strings for chunked data. If data
        is chunked and there is no empty chunk at the end, then empty chunk wold be added.

    :rtype: HTTPResponse
    """
    return __no_content(mod_msg.HTTPResponse, version, headers, data)


def raw_no_content(version='HTTP/1.1', headers=None, data=None):
    """Create 204 No Content raw response

    :param str version: protocol version
    :type headers: dict or iterable of pairs (name, value)
    :param headers: message headers
    :type data: None, str, list of str
    :param data: message data. None for no data, string for plain data, list of strings for chunked data. If data
        is chunked and there is no empty chunk at the end, then empty chunk wold be added.

    :rtype: RawHTTPResponse
    """
    return __no_content(mod_msg.RawHTTPResponse, version, headers, data)


def __not_modified(msg_type, version, headers, data):
    return __custom(msg_type, 304, 'Not Modified', version, headers, data)


def not_modified(version='HTTP/1.1', headers=None, data=None):
    """Create 304 Not Modified response

    :param str version: protocol version
    :type headers: dict or iterable of pairs (name, value)
    :param headers: message headers
    :type data: None, str, list of str
    :param data: message data. None for no data, string for plain data, list of strings for chunked data. If data
        is chunked and there is no empty chunk at the end, then empty chunk wold be added.

    :rtype: HTTPResponse
    """
    return __not_modified(mod_msg.HTTPResponse, version, headers, data)


def raw_not_modified(version='HTTP/1.1', headers=None, data=None):
    """Create 304 Not Modified raw response

    :param str version: protocol version
    :type headers: dict or iterable of pairs (name, value)
    :param headers: message headers
    :type data: None, str, list of str
    :param data: message data. None for no data, string for plain data, list of strings for chunked data. If data
        is chunked and there is no empty chunk at the end, then empty chunk wold be added.

    :rtype: RawHTTPResponse
    """
    return __not_modified(mod_msg.RawHTTPResponse, version, headers, data)


def __forbidden(msg_type, version, headers, data):
    return __custom(msg_type, 403, 'Forbidden', version, headers, data)


def forbidden(version='HTTP/1.1', headers=None, data=''):
    """Create 403 Forbidden response

    :param str version: protocol version
    :type headers: dict or iterable of pairs (name, value)
    :param headers: message headers
    :type data: None, str, list of str
    :param data: message data. None for no data, string for plain data, list of strings for chunked data. If data
        is chunked and there is no empty chunk at the end, then empty chunk wold be added.

    :rtype: HTTPResponse
    """
    return __forbidden(mod_msg.HTTPResponse, version, headers, data)


def __not_found(msg_type, version, headers, data):
    return __custom(msg_type, 404, 'Not Found', version, headers, data)


def not_found(version='HTTP/1.1', headers=None, data=''):
    """Create 404 Not Found response

    :param str version: protocol version
    :type headers: dict or iterable of pairs (name, value)
    :param headers: message headers
    :type data: None, str, list of str
    :param data: message data. None for no data, string for plain data, list of strings for chunked data. If data
        is chunked and there is no empty chunk at the end, then empty chunk wold be added.

    :rtype: HTTPResponse
    """
    return __not_found(mod_msg.HTTPResponse, version, headers, data)


def raw_not_found(version='HTTP/1.1', headers=None, data=''):
    """Create 404 Not Found raw response

    :param str version: protocol version
    :type headers: dict or iterable of pairs (name, value)
    :param headers: message headers
    :type data: None, str, list of str
    :param data: message data. None for no data, string for plain data, list of strings for chunked data. If data
        is chunked and there is no empty chunk at the end, then empty chunk wold be added.

    :rtype: RawHTTPResponse
    """
    return __not_found(mod_msg.RawHTTPResponse, version, headers, data)


def __not_allowed(msg_type, version, headers, data):
    return __custom(msg_type, 405, 'Method Not Allowed', version, headers, data)


def not_allowed(version='HTTP/1.1', headers=None, data=''):
    """Create 405 Method Not Allowed response

    :param str version: protocol version
    :type headers: dict or iterable of pairs (name, value)
    :param headers: message headers
    :type data: None, str, list of str
    :param data: message data. None for no data, string for plain data, list of strings for chunked data. If data
        is chunked and there is no empty chunk at the end, then empty chunk wold be added.

    :rtype: HTTPResponse
    """
    return __not_allowed(mod_msg.HTTPResponse, version, headers, data)


def raw_not_allowed(version='HTTP/1.1', headers=None, data=''):
    """Create 405 Method Not Allowed raw response

    :param str version: protocol version
    :type headers: dict or iterable of pairs (name, value)
    :param headers: message headers
    :type data: None, str, list of str
    :param data: message data. None for no data, string for plain data, list of strings for chunked data. If data
        is chunked and there is no empty chunk at the end, then empty chunk wold be added.

    :rtype: RawHTTPResponse
    """
    return __not_allowed(mod_msg.RawHTTPResponse, version, headers, data)


def __service_unavailable(msg_type, version, headers, data):
    return __custom(msg_type, 503, 'Service Unavailable', version, headers, data)


def service_unavailable(version='HTTP/1.1', headers=None, data=''):
    """Create 503 Service Unavailable response

    :param str version: protocol version
    :type headers: dict or iterable of pairs (name, value)
    :param headers: message headers
    :type data: None, str, list of str
    :param data: message data. None for no data, string for plain data, list of strings for chunked data. If data
        is chunked and there is no empty chunk at the end, then empty chunk wold be added.

    :rtype: HTTPResponse
    """
    return __service_unavailable(mod_msg.HTTPResponse, version, headers, data)


def raw_service_unavailable(version='HTTP/1.1', headers=None, data=''):
    """Create 503 Service Unavailable raw response

    :param str version: protocol version
    :type headers: dict or iterable of pairs (name, value)
    :param headers: message headers
    :type data: None, str, list of str
    :param data: message data. None for no data, string for plain data, list of strings for chunked data. If data
        is chunked and there is no empty chunk at the end, then empty chunk wold be added.

    :rtype: RawHTTPResponse
    """
    return __service_unavailable(mod_msg.RawHTTPResponse, version, headers, data)


def __partial_content(msg_type, version, headers, data):
    return __custom(msg_type, 206, 'Partial Content', version, headers, data)


def partial_content(version='HTTP/1.1', headers=None, data=''):
    """Create 206 Partial Content response

    :param str version: protocol version
    :type headers: dict or iterable of pairs (name, value)
    :param headers: message headers
    :type data: None, str, list of str
    :param data: message data. None for no data, string for plain data, list of strings for chunked data. If data
        is chunked and there is no empty chunk at the end, then empty chunk wold be added.

    :rtype: HTTPResponse
    """
    return __partial_content(mod_msg.HTTPResponse, version, headers, data)


def raw_partial_content(version='HTTP/1.1', headers=None, data=''):
    """Create 206 Partial Content raw response

    :param str version: protocol version
    :type headers: dict or iterable of pairs (name, value)
    :param headers: message headers
    :type data: None, str, list of str
    :param data: message data. None for no data, string for plain data, list of strings for chunked data. If data
        is chunked and there is no empty chunk at the end, then empty chunk wold be added.

    :rtype: RawHTTPResponse
    """
    return __partial_content(mod_msg.RawHTTPResponse, version, headers, data)


def __gateway_timeout(msg_type, version, headers, data):
    return __custom(msg_type, 504, 'Gateway Timeout', version, headers, data)


def gateway_timeout(version='HTTP/1.1', headers=None, data=''):
    """Create 504 Gateway Timeout response

    :param str version: protocol version
    :type headers: dict or iterable of pairs (name, value)
    :param headers: message headers
    :type data: None, str, list of str
    :param data: message data. None for no data, string for plain data, list of strings for chunked data. If data
        is chunked and there is no empty chunk at the end, then empty chunk wold be added.

    :rtype: HTTPResponse
    """
    return __gateway_timeout(mod_msg.HTTPResponse, version, headers, data)


def raw_gateway_timeout(version='HTTP/1.1', headers=None, data=''):
    """Create 504 Gateway Timeout raw response

    :param str version: protocol version
    :type headers: dict or iterable of pairs (name, value)
    :param headers: message headers
    :type data: None, str, list of str
    :param data: message data. None for no data, string for plain data, list of strings for chunked data. If data
        is chunked and there is no empty chunk at the end, then empty chunk wold be added.

    :rtype: RawHTTPResponse
    """
    return __gateway_timeout(mod_msg.RawHTTPResponse, version, headers, data)


def custom(status, reason, version='HTTP/1.1', headers=None, data=''):
    """Create custom response

    :param int status: response status
    :param str reason: response reason phrase
    :param str version: protocol version
    :type headers: dict or iterable of pairs (name, value)
    :param headers: message headers
    :type data: None, str, list of str
    :param data: message data. None for no data, string for plain data, list of strings for chunked data. If data
        is chunked and there is no empty chunk at the end, then empty chunk wold be added.

    :rtype: HTTPResponse
    """
    return __custom(mod_msg.HTTPResponse, status, reason, version, headers, data)


def raw_custom(status, reason, version='HTTP/1.1', headers=None, data=''):
    """Create custom raw response

    :param int status: response status
    :param str reason: response reason phrase
    :param str version: protocol version
    :type headers: dict or iterable of pairs (name, value)
    :param headers: message headers
    :type data: None, str, list of str
    :param data: message data. None for no data, string for plain data, list of strings for chunked data. If data
        is chunked and there is no empty chunk at the end, then empty chunk wold be added.

    :rtype: RawHTTPResponse
    """
    return __custom(mod_msg.RawHTTPResponse, status, reason, version, headers, data)


some = custom  # deprecated
