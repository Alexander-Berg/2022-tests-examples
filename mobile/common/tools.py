# -*- coding: utf-8 -*-
import re
import urllib
from collections import OrderedDict

import calendar
import yenv
from codecs import BOM_UTF8
from django.conf import settings
from requests import adapters, Session
from string import printable
from urllib3.util.retry import Retry
from urlparse import urlparse, parse_qsl, urlunparse

STRICT_VERSION_REGEXP = re.compile(r'[\d.]+')

BOM_LENGTH = len(BOM_UTF8)
TRANSLATION_CHAR_LIMIT = 10000

RETRY_METHODS = frozenset(('GET', 'POST'))
RETRY_STATUSES = frozenset((451, 500, 502, 504))


def make_mds_url(query, host=None):
    """
    Production MDS is accessible from internet, but testing is not.
    So, we do reverse proxy for testing MDS through our backend.
    """
    url = settings.MDS_URL_TEMPLATE % query
    if yenv.type != 'production' and host is not None:
        url = rewrite_url(url, host)
    return url


def make_s3_mds_url(query, host=None):
    """
    Production S3 MDS is accessible from internet, but testing is not.
    So, we do reverse proxy for testing S3 MDS through our backend.
    """
    url = settings.S3_MDS_URL_TEMPLATE % query
    if yenv.type != 'production' and host is not None:
        url = rewrite_url(url, host, path_prefix='get-lnchr-s3')
    return url


def make_http_header(header):
    return 'HTTP_%s' % header.upper().replace('-', '_').replace('.', '_')


def drop_non_printable(string):
    return filter(lambda x: x in printable, string)


def get_http_header(request, header):
    header_string = request.META.get(make_http_header(header))
    if header_string is not None:
        return drop_non_printable(header_string)


def rewrite_url(url, host, scheme='https', path_prefix=''):
    parsed = urlparse(url)
    # noinspection PyProtectedMember
    replaced = parsed._replace(scheme=scheme, netloc=host, path=path_prefix + parsed.path)
    return replaced.geturl()


def rewrite_params_in_url(url, params, params_to_delete=None):
    """
    Function updates url adding new parameters passed in params.
    If a parameter already exists, it will be rewritten. If there are duplicating parameters, only
    the last one will be included in resulting string. Function keeps the order of parameters.
    :param url: string
    :param params: dict
    :return: string
    """
    if not params:
        return url

    parsed_url = urlparse(url)
    query = OrderedDict(parse_qsl(parsed_url.query))
    query.update(params)
    if params_to_delete:
        for key in params_to_delete:
            query.pop(key, None)

    # noinspection PyProtectedMember
    return urlunparse(parsed_url._replace(query=urllib.urlencode(query, True)))


def remove_bom(content):
    """
    some html page has BOM from beginning of content and sometimes it placed at not first position,
    so, that malformed pages can not be normally processed by .decode('utf-8-sig') call
    here implemented simple remove algorithm suitable for this cases.
    for example, see http://angarsk-school4.ru/ malformed page
    there are first bytes: '\x0d\x0a\xef\xbb\xbf<!DOCTYPE...'
    remove_BOM('\x0d\x0a\xef\xbb\xbf<!DOCTYPE...') will return '<!DOCTYPE...'
    :param content: content of html page, such as `response.content`
    :return: content with remove prefix contained BOM or the same data if BOM not found
    """
    if BOM_UTF8 in content[:20]:  # looks like encoded with BOM
        content = content[content.index(BOM_UTF8) + BOM_LENGTH:]
    return content


def clear_version(version):
    """
    Remove .beta/dev/etc suffixes
    """

    version = STRICT_VERSION_REGEXP.match(str(version)).group()
    if version.endswith('.'):
        return version[:-1]

    return version


def strip_package_name(package_name):
    result = package_name
    if result.startswith(settings.GIFT_PACKAGE_NAME_PREFIX):  # for special case 'gift:package_name'
        result = result[len(settings.GIFT_PACKAGE_NAME_PREFIX):]
    return result


def to_timestamp(dt):
    return int(calendar.timegm(dt.timetuple()))


def make_requests_session(max_retries=None,
                          pool_maxsize=adapters.DEFAULT_POOLSIZE,
                          pool_connections=adapters.DEFAULT_POOLSIZE,
                          pool_block=adapters.DEFAULT_POOLBLOCK,
                          prefixes=('http://', 'https://')):
    session = Session()
    if max_retries is None:
        retry = adapters.DEFAULT_RETRIES
    elif isinstance(max_retries, Retry):
        retry = max_retries
    else:
        retry = Retry(
            total=max_retries,
            read=max_retries,
            connect=max_retries,
            status=max_retries,
            status_forcelist=RETRY_STATUSES,
            method_whitelist=RETRY_METHODS,
        )
    adapter = adapters.HTTPAdapter(max_retries=retry,
                                   pool_maxsize=pool_maxsize,
                                   pool_connections=pool_connections,
                                   pool_block=pool_block)
    for prefix in prefixes:
        session.mount(prefix, adapter)
    return session


def add_authorization_header(authorization_token, headers=None):
    if headers is None:
        headers = {}
    headers.update({'Authorization': 'OAuth %s' % authorization_token})
    return headers
