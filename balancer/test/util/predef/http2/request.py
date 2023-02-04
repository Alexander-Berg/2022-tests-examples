# -*- coding: utf-8 -*-
import balancer.test.util.proto.http2.message as mod_msg


def request(method='GET', scheme='https', authority='localhost', path='/', headers=None, data=None):
    return mod_msg.HTTP2Request(
        mod_msg.HTTP2RequestLine(
            method,
            scheme,
            authority,
            path,
        ),
        headers,
        data,
    )


def raw_request(method='GET', scheme='https', authority='localhost', path='/', headers=None, data=None):
    if headers is None:
        headers = list()
    if isinstance(headers, dict):
        headers = headers.items()
    return mod_msg.RawHTTP2Message(
        [
            (':method', method),
            (':scheme', scheme),
            (':authority', authority),
            (':path', path),
        ] + headers,
        data,
    )


def get(path='/', scheme='https', authority='localhost', headers=None, data=None):
    """Create GET request

    :param str path: request path
    :param str scheme: request scheme
    :param str authority: request authority
    :type headers: dict or iterable of pairs (name, value)
    :param headers: message headers
    :type data: None, str, list of str
    :param data: message data. List of strings converts to list of DataFrame.
        None equals to empty list, str equals to list with one string.

    :rtype: HTTP2Request
    """
    return request('GET', scheme, authority, path, headers, data)


def raw_get(path='/', scheme='https', authority='localhost', headers=None, data=None):
    """Create raw GET request

    :param str path: request path
    :param str scheme: request scheme
    :param str authority: request authority
    :type headers: dict or iterable of pairs (name, value)
    :param headers: message headers
    :type data: None, str, list of str
    :param data: message data. List of strings converts to list of DataFrame.
        None equals to empty list, str equals to list with one string.

    :rtype: RawHTTP2Message
    """
    return raw_request('GET', scheme, authority, path, headers, data)


def post(path='/', scheme='https', authority='localhost', headers=None, data=None):
    """Create POST request

    :param str path: request path
    :param str scheme: request scheme
    :param str authority: request authority
    :type headers: dict or iterable of pairs (name, value)
    :param headers: message headers
    :type data: None, str, list of str
    :param data: message data. List of strings converts to list of DataFrame.
        None equals to empty list, str equals to list with one string.

    :rtype: HTTP2Request
    """
    return request('POST', scheme, authority, path, headers, data)


def raw_post(path='/', scheme='https', authority='localhost', headers=None, data=None):
    """Create raw POST request

    :param str path: request path
    :param str scheme: request scheme
    :param str authority: request authority
    :type headers: dict or iterable of pairs (name, value)
    :param headers: message headers
    :type data: None, str, list of str
    :param data: message data. List of strings converts to list of DataFrame.
        None equals to empty list, str equals to list with one string.

    :rtype: RawHTTP2Message
    """
    return raw_request('POST', scheme, authority, path, headers, data)


def head(path='/', scheme='https', authority='localhost', headers=None, data=None):
    """Create HEAD request

    :param str path: request path
    :param str scheme: request scheme
    :param str authority: request authority
    :type headers: dict or iterable of pairs (name, value)
    :param headers: message headers
    :type data: None, str, list of str
    :param data: message data. List of strings converts to list of DataFrame.
        None equals to empty list, str equals to list with one string.

    :rtype: HTTP2Request
    """
    return request('HEAD', scheme, authority, path, headers, data)


def raw_head(path='/', scheme='https', authority='localhost', headers=None, data=None):
    """Create raw HEAD request

    :param str path: request path
    :param str scheme: request scheme
    :param str authority: request authority
    :type headers: dict or iterable of pairs (name, value)
    :param headers: message headers
    :type data: None, str, list of str
    :param data: message data. List of strings converts to list of DataFrame.
        None equals to empty list, str equals to list with one string.

    :rtype: RawHTTP2Message
    """
    return raw_request('HEAD', scheme, authority, path, headers, data)


def raw_custom(headers=None, data=None):
    """Create custom raw request

    :type headers: dict or iterable of pairs (name, value)
    :param headers: message headers
    :type data: None, str, list of str
    :param data: message data. List of strings converts to list of DataFrame.
        None equals to empty list, str equals to list with one string.

    :rtype: RawHTTP2Message
    """
    if headers is None:
        headers = list()
    return mod_msg.RawHTTP2Message(headers, data)
