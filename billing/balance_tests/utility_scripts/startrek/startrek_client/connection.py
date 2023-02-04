# coding: utf-8

import logging
import requests
import json

from pkg_resources import get_distribution, DistributionNotFound
import six
from six.moves.urllib.parse import urlparse, urljoin
from six.moves import range

from . import exceptions
from . import uriutils
from .objects import Reference, Resource, PaginatedList, SeekablePaginatedList

logger = logging.getLogger(__name__)


def bind_method(name):
    def method(self, *args, **kws):
        return self.request(name, *args, **kws)

    method.func_name = name.lower()
    return method


class Connection(object):

    def __init__(self,
                 useragent,
                 token=None,
                 session_id=None,
                 base_url="https://st-api.yandex-team.ru",
                 timeout=10,
                 retries=10):

        self.session = requests.Session()
        if session_id is not None:
            self.session.cookies['Session_id'] = session_id
        else:
            self.session.headers['Authorization'] = 'OAuth ' + token
        self.session.headers['Content-Type'] = 'application/json'

        try:
            version = get_distribution('startrek_client').version
        except DistributionNotFound:
            version = 'developer'
        useragent += ' via python-startrek-client/' + version
        self.session.headers['User-Agent'] = useragent

        self.base_url = base_url
        self.timeout = timeout
        self.retries = retries

    get = bind_method('GET')
    put = bind_method('PUT')
    post = bind_method('POST')
    patch = bind_method('PATCH')
    delete = bind_method('DELETE')

    def stream(self, path, params=None):
        url = urljoin(self.base_url, uriutils.expand(path, params))
        return self._request('GET', url, stream=True).iter_content(8 * 1024)

    def link(self, path, resource, rel, params=None, version=None):
        return self._link('LINK', path, resource, rel, params, version)

    def unlink(self, path, resource, rel, params=None, version=None):
        return self._link('UNLINK', path, resource, rel, params, version)

    def _link(self, method, path, resource, rel, params=None, version=None):
        url = urljoin(self.base_url, uriutils.expand(path, params))
        link = '<{resource}>; rel="{rel}"'.format(
            resource=resource,
            rel=rel
        )
        response = self._request(method, url, headers={'Link': link}, version=version)
        return decode_response(response, self)

    def request(self, method, path, params=None, data=None, files=None,
                version=None, **kwargs):
        logger.info("%s %s <- %r", method, path, params)
        url = urljoin(self.base_url, uriutils.expand(path, params))
        response = self._request(method, url, data, files, version, **kwargs)
        return decode_response(response, self)

    def _request(self, method, url, data=None, files=None, version=None, headers=None, stream=False):
        if headers is None:
            headers = {}
        # XXX: API does not always respect If-Match header :(
        if version is not None:
            headers['If-Match'] = '"{}"'.format(version)
        if data is not None:
            data = json.dumps(data, default=encode_resource)
        logger.debug("HTTP %s %s DATA=%s", method, url, data)

        if files:
            headers['Content-Type'] = None

        return self._try_request(
            method=method,
            url=url,
            data=data,
            files=files,
            timeout=self.timeout,
            headers=headers,
            stream=stream
        )

    def _try_request(self, **kws):
        response = None
        exception = None
        iterations = max(self.retries + 1, 1)

        for retry in range(iterations):
            try:
                response = self.session.request(**kws)
            except Exception as e:
                exception = e
            else:
                exception = None
                if 500 <= response.status_code < 600:
                    logger.warning(
                        "Request failed with status %d, retrying (%d)...",
                        response.status_code, retry
                    )
                    self._log_error(logging.WARNING, response)
                else:
                    break
            # XXX: sleep?

        if exception is not None:
            raise exceptions.StartrekRequestError(exception)
        elif 500 <= response.status_code < 600:
            raise exceptions.OutOfRetries(response)
        elif 400 <= response.status_code < 500:
            self._log_error(logging.ERROR, response)
            exc_class = exceptions.STATUS_CODES.get(
                response.status_code,
                exceptions.StartrekServerError
            )
            raise exc_class(response)
        return response

    def _log_error(self, level, response):
        try:
            data = response.json()
            logger.log(level, 'Startrek errors: %s %s',
                       data.get('statusCode'), data.get('errors'))
            messages = data.get('errorMessages', ())
            logger.log(level, '%d messages follow:', len(messages))
            for msg in messages:
                logger.log(level, ' - %s', msg)
        except Exception:
            logger.log(level, 'Strange startrek error: %s',
                       response.text)


def decode_response(response, conn):
    if not response.content:
        return None

    def decode_object(obj):
        if 'self' in obj:
            url = obj['self'].encode('utf-8') if six.PY2 else obj['self']
            path = urlparse(url).path

            return Reference(conn, path, obj)
        return obj

    decoded = response.json(object_hook=decode_object)

    if isinstance(decoded, Reference):
        return Resource(conn, decoded._path, decoded._value)
    elif isinstance(decoded, list):
        items = [Resource(conn, item._path, item._value)
                 for item in decoded]
        if 'next' in response.links:
            params = {
                'connection': conn,
                'head': items,
                'response': response,
            }
            if 'seek' in response.links:
                return SeekablePaginatedList(**params)
            else:
                return PaginatedList(**params)
        else:
            return items
    else:
        # XXX: dunno what to do
        return decoded


def encode_resource(obj):
    if isinstance(obj, (Resource, Reference)):
        attribute = next((attribute for attribute
                          in ('key', 'uid', 'url', 'id')
                          if hasattr(obj, attribute)),
                         None)
        if attribute:
            return {attribute: getattr(obj, attribute)}
    raise exceptions.UnencodableValue(obj)
