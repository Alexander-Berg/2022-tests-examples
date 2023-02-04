# -*- coding: utf-8 -*-
import pytest
import time
import datetime

from configs import CacheConfig, CacheWorkersConfig, CacheErrordocConfig, CacheServerConfig, \
    CacheClientConfig, CacheClientResponseHeadersConfig, CacheClientExplicitServerConfig

from balancer.test.util import asserts

import balancer.test.plugin.context as mod_ctx

from balancer.test.util.predef import http
from balancer.test.util.predef.handler.server.http import SimpleConfig, ChunkedConfig, CloseConfig, StaticResponseHandler

from balancer.test.util.proto.handler.server import State
from balancer.test.util.proto.handler.server.http import HTTPConfig, PreparseHandler


ETAG_VALUE = '"abcdef12345678"'
MAX_SIZE = 4000000000
FUTURE_DATE = 'Mon, 19 Jan 2037 08:01:37 GMT'


class CacheNotModified(PreparseHandler):
    name = 'cache_not_modified'

    def is_not_modified(self, raw_request):
        def test(attr_name, header_name):
            if raw_request.headers.get_one(header_name) is None:
                return None
            return raw_request.headers.get_one(header_name) == getattr(self.config, attr_name)

        not_modified_last_modified = test('last_modified', 'If-Modified-Since')
        not_modified_etag = test('etag', 'If-None-Match')
        if not_modified_last_modified is None and not_modified_etag is None:
            return False
        if not_modified_last_modified is False or not_modified_etag is False:
            return False
        return True

    def send_modified_headers(self, raw_request, stream):
        if self.config.last_modified is not None and not self.is_not_modified(raw_request):
            stream.write_header('Last-Modified', self.config.last_modified)
        if self.config.etag is not None:
            stream.write_header('ETag', self.config.etag)

    def handle_parsed_request(self, raw_request, stream):
        if self.is_not_modified(raw_request):
            # FIXME: need more flexible write_request_line
            stream.write_line('HTTP/1.1 304 Not Modified')
        else:
            stream.write_line('HTTP/1.1 200 OK')
        self.send_modified_headers(raw_request, stream)
        if self.config.header is not None:
            stream.write_header(self.config.header, 'SomeCustomValue')
        stream.end_headers()
        if not self.is_not_modified(raw_request) and self.config.data is not None:
            stream.write(self.config.data)
        self.force_close()


class CacheNotModifiedConfig(HTTPConfig):
    HANDLER_TYPE = CacheNotModified

    def __init__(self, last_modified=None, etag=None, header=None, data=None):
        super(CacheNotModifiedConfig, self).__init__()
        self.last_modified = last_modified
        self.etag = etag
        self.header = header
        self.data = data


def _debug_log(msg):
    return
    with open('./backend.log', 'a') as f:
        f.write(msg)
        f.write('\n')


class TestCacheItem(object):
    def __init__(self, data, last_modified=None, valid_until=None, etag=None):
        self.data = data
        self.last_modified = last_modified
        self.valid_until = valid_until
        self.etag = etag

    def __str__(self):
        return 'TestCacheItem(last_modified={last_modified}, valid_until={valid_until}, etag={etag}, {data})'.format(
            last_modified=str(self.last_modified or 'None'),
            valid_until=str(self.valid_until or 'None'),
            etag=str(self.etag or 'None'),
            data=self.data)

    def __repr__(self):
        return str(self)


class TestCacheServerHandler(StaticResponseHandler):
    name = 'cache_server'

    def _write_response(self, response, stream):
        if not response.is_raw_message():
            response = response.to_raw_response()
        stream.write_response(response)
        _debug_log('writing response:\n' + str(response))
        self.force_close()

    def _respond_with_cache_item(self, cache_item, stream):
        _debug_log('_respond_with_cache_item')
        headers = {}

        if cache_item.last_modified:
            headers['Last-Modified'] = cache_item.last_modified.strftime('%a, %d %b %Y %H:%M:%S GMT')

        if cache_item.valid_until:
            headers['Valid-Until'] = cache_item.valid_until.strftime('%a, %d %b %Y %H:%M:%S GMT')

        if cache_item.etag:
            headers['ETag'] = cache_item.etag

        self._write_response(http.response.ok(data=cache_item.data, headers=headers).to_raw_response(), stream)

    def handle_parsed_request(self, raw_request, stream):
        _debug_log(self.state.as_str())
        _debug_log(raw_request.request_line.method)
        for k, v in raw_request.headers.items():
            _debug_log(k + ': ' + v)
        if raw_request.request_line.method == 'GET':
            self.process_get(raw_request, stream)
        elif raw_request.request_line.method == 'PUT':
            self.process_put(raw_request, stream)
        else:
            self._write_response(http.response.some(405, 'Method not allowed'), stream)

    def extract_id(self, raw_request):
        return raw_request.request_line.path

    def process_get(self, raw_request, stream):
        _debug_log('process GET')
        cache_id = self.extract_id(raw_request)
        if self.state.has(cache_id):
            _debug_log('GET found')
            self._respond_with_cache_item(self.state.get(cache_id), stream)
        else:
            _debug_log('GET not found')
            self._write_response(http.response.not_found(), stream)

    def process_put(self, raw_request, stream):
        _debug_log('process PUT')
        cache_id = self.extract_id(raw_request)
        request = raw_request.to_request()
        data = request.data.content
        validate = False
        # if 'Cache-Action' in request.headers:
        #     validate = request.headers['Cache-Action'][0].lower() == 'validate'

        last_modified = None
        if 'Last-Modified' in request.headers:
            last_modified = datetime.datetime.strptime(request.headers['Last-Modified'][0], '%a, %d %b %Y %H:%M:%S GMT')

        valid_until = None
        if self.config.valid_for:
            valid_until = datetime.datetime.utcnow() + self.config.valid_for

        etag = None
        if 'ETag' in request.headers:
            etag = request.headers['ETag'][0]

        if validate:
            raise NotImplementedError()
        else:
            _debug_log('storing cache')
            cache_item = TestCacheItem(data, valid_until=valid_until, last_modified=last_modified, etag=etag)
            self.state.put(cache_id, cache_item)
            headers = {}
            if valid_until:
                headers['Valid-Until'] = valid_until.strftime('%a, %d %b %Y %H:%M:%S GMT')
            self._write_response(http.response.ok(headers=headers), stream)
            _debug_log('responded with ok on storing')


# WARNING: not concurrent-safe
class TestCacheServerState(State):
    def __init__(self, config):
        super(TestCacheServerState, self).__init__(config)
        self.__cache = {}

    def has(self, key):
        return key in self.__cache

    def get(self, key):
        return self.__cache.get(key)

    def put(self, key, value):
        self.__cache[key] = value

    def as_str(self):
        return str(self.__cache)


class TestCacheServerConfig(HTTPConfig):
    HANDLER_TYPE = TestCacheServerHandler
    STATE_TYPE = TestCacheServerState

    def __init__(self, valid_for=None):
        super(TestCacheServerConfig, self).__init__()
        self.valid_for = valid_for  # or datetime.timedelta(days=365)


class Request(object):
    ID = 0

    def __init__(self, m, n):
        super(Request, self).__init__()
        self.m = m
        self.n = n
        self.request_id = Request.ID
        Request.ID += 1

    def __str__(self):
        return '/?m=%d&n=%d&id=%d' % (self.m, self.n, self.request_id)

    def build(self):
        return http.request.get(path=str(self))

    def copy(self):
        return Request(self.m, self.n)


class CacheContext(object):
    def __init__(self):
        super(CacheContext, self).__init__()
        self.__backend_reqs = list()

    def from_cache(self, request):
        time.sleep(0.5)  # FIXME
        while not self.backend.state.requests.empty():
            self.__backend_reqs.append(self.backend.state.get_request())
        ids = [int(req.request_line.cgi['id'][0]) for req in self.__backend_reqs]
        return request.request_id not in ids

    def assert_from_cache(self, request):
        from_cache = self.from_cache(request)
        assert from_cache

    def assert_not_from_cache(self, request):
        assert not self.from_cache(request)


cache_ctx = mod_ctx.create_fixture(CacheContext)


def base_equal_test(cache_ctx, backend_response, headers):
    r1 = Request(1, 2)
    r2 = r1.copy()
    cache_ctx.start_backend(SimpleConfig(response=backend_response))
    cache_ctx.start_backend(TestCacheServerConfig(), name='server')
    cache_ctx.start_balancer(CacheClientConfig())

    first_resp = cache_ctx.perform_request(http.request.get(path=str(r1), headers=headers))
    time.sleep(0.5)
    second_resp = cache_ctx.perform_request(http.request.get(path=str(r2), headers=headers))

    asserts.status(first_resp, 200)
    assert first_resp == second_resp
    cache_ctx.assert_not_from_cache(r1)
    cache_ctx.assert_from_cache(r2)
    if backend_response.status_line.version == 'HTTP/1.0':
        assert first_resp.status_line.status == backend_response.status_line.status
        assert first_resp.headers == backend_response.headers
        assert first_resp.data.content == backend_response.data.content
    else:
        assert first_resp == backend_response


def test_cached_value(cache_ctx):
    """
    Если не истек ttl запрашиваемого id, и кэш не переполнился, то вернуть значение из кэша
    """
    _debug_log('=== test_cached_value ====')
    base_equal_test(cache_ctx, http.response.ok(), {})


def test_cached_value_response_headers(cache_ctx):
    """
    BALANCER-992
    Балансер должен добавлять response_headers к результатам из кэша
    """
    r1 = Request(1, 0)
    r2 = Request(2, 0)
    backend_response = http.response.ok()
    backend_response_with_headers = http.response.ok(headers={"x-led": "zeppelin"})

    cache_ctx.start_backend(SimpleConfig(response=backend_response))
    cache_ctx.start_backend(TestCacheServerConfig(), name='server')
    cache_ctx.start_balancer(CacheClientResponseHeadersConfig())

    # Sending the first request. The only purpose of this is to initialize cache with response WITHOUT x-led header
    first_resp = cache_ctx.perform_request(http.request.get(path=str(r1)))
    time.sleep(0.5)

    # This response has to be from cache. Balancer has to explicitly add the x-led header in the response_headers outer scope
    second_resp = cache_ctx.perform_request(http.request.get(path=str(r2)))

    asserts.status(first_resp, 200)
    assert first_resp == backend_response  # cached response has to be equal to backend response
    cache_ctx.assert_not_from_cache(r1)
    cache_ctx.assert_from_cache(r2)
    assert second_resp == backend_response_with_headers  # balancer has to add response headers to the cached response


@pytest.mark.parametrize('chunked', [False, True], ids=['length', 'chunked'])
@pytest.mark.parametrize('size', [512, 1024 * 512], ids=['small', 'big'])
@pytest.mark.parametrize('version', ['HTTP/1.0', 'HTTP/1.1'], ids=['http10', 'http11'])
def test_cached_value_body(cache_ctx, size, version, chunked):
    """
    Если не истек ttl запрашиваемого id, и кэш не переполнился, то вернуть значение из кэша
    """
    _debug_log('=== test_cached_value_body ====')
    if chunked:
        multiplier = 64
        body = ['z' * multiplier] * (size / multiplier)
    else:
        body = 'z' * size
    base_equal_test(cache_ctx, http.response.ok(version=version, data=body), {})


def test_modified(cache_ctx):
    """
    SEPE-4605
    Если клиент присылает заголовок If-Modified-Since, а в ответе бэкенда присутствует Last-Modified,
    значение которого > запрошенного клиентом, то клиенту отсылается ответ backend-а
    """
    _debug_log('=== test_modified ====')
    base_equal_test(cache_ctx, http.response.ok(headers={'last-modified': 'Mon, 14 Oct 2013 11:00:01 GMT'}),
                    {'If-Modified-Since': 'Mon, 14 Oct 2013 11:00:00 GMT'})


def test_modified_http10(cache_ctx):
    """
    SEPE-4605
    Если клиент присылает заголовок If-Modified-Since, а в ответе бэкенда присутствует Last-Modified,
    значение которого > запрошенного клиентом, то клиенту отсылается ответ backend-а
    случай для HTTP/1.0 backend-а
    """
    base_equal_test(cache_ctx,
                    http.response.ok(version='HTTP/1.0', headers={'last-modified': 'Mon, 14 Oct 2013 11:00:01 GMT'}),
                    {'If-Modified-Since': 'Mon, 14 Oct 2013 11:00:00 GMT'})


def base_different_test(cache_ctx, cache_server_config, r1=None, r2=None, sleep=0, response=None):
    if r1 is None or r2 is None:
        r1 = Request(1, 2)
        r2 = r1.copy()
    if response is None:
        response = http.response.ok()
    cache_ctx.start_backend(SimpleConfig(response=response))
    cache_ctx.start_backend(cache_server_config, name='server')
    cache_ctx.start_balancer(CacheClientConfig())

    first_resp = cache_ctx.perform_request(http.request.get(path=str(r1)))
    time.sleep(sleep)
    second_resp = cache_ctx.perform_request(http.request.get(path=str(r2)))

    asserts.status(first_resp, response.response_line.status)
    asserts.status(second_resp, response.response_line.status)
    cache_ctx.assert_not_from_cache(r1)
    cache_ctx.assert_not_from_cache(r2)


def test_cached_value_time_drop(cache_ctx):
    """
    Если истек ttl запрашиваемого id, то запросить новое значение у backend-а
    """
    _debug_log('=== test_cached_value_time_drop ====')
    ttl = 3

    base_different_test(cache_ctx, TestCacheServerConfig(valid_for=datetime.timedelta(seconds=ttl)), sleep=ttl + 2)


def test_no_param_concatenation(cache_ctx):
    """
    Запросы, пары параметров которых различны, должны соответствовать различным ключам
    (ключ не должен быть простой конкатенацией параметров)
    """
    _debug_log('=== test_no_param_concatenation ====')
    r1 = Request(123, 4)
    r2 = Request(1, 234)

    base_different_test(cache_ctx, TestCacheServerConfig(), r1=r1, r2=r2)


def test_not_found(cache_ctx):
    """
    Если на запрос клиента backend отвечает 404,
    то ответ backend-а не кешируется
    """
    _debug_log('=== test_no_param_concatenation ====')
    base_different_test(cache_ctx, TestCacheServerConfig(), response=http.response.not_found())


def test_5xx_backend(cache_ctx):
    """
    Если backend отвечает 5xx, то балансер должен передать ответ клиенту
    и ничего не складывать в кеш
    """
    base_different_test(cache_ctx, TestCacheServerConfig(), response=http.response.service_unavailable())


def test_headers_after_validation(cache_ctx):
    """
    Проверка правильности ответа из кеша после валидации
    (тест на баг, когда балансер возвращает заголовки внутри тела ответа)
    """
    ttl = 3
    request = Request(1, 2)
    header_name = 'SomeCustomHeader'
    body = 'Some data'
    cache_ctx.start_backend(CacheNotModifiedConfig(last_modified='Mon, 14 Oct 2013 11:00:00 GMT',
                                                   header=header_name, data=body))
    cache_ctx.start_backend(TestCacheServerConfig(valid_for=datetime.timedelta(seconds=ttl)), name='server')
    cache_ctx.start_balancer(CacheClientConfig())

    first_resp = cache_ctx.perform_request(http.request.get(str(request)))
    time.sleep(ttl + 2)
    second_resp = cache_ctx.perform_request(http.request.get(str(request)))

    asserts.status(first_resp, 200)
    asserts.header(first_resp, header_name)
    asserts.content(first_resp, body)
    asserts.status(second_resp, 200)
    asserts.header(second_resp, header_name)
    asserts.content(second_resp, body)


def test_not_modified_from_backend(cache_ctx):
    """
    Бэкенд, умеющий обрабатывать If-Modified-Since,
    должен получить корректный заголовок от балансера и вернуть 304
    """
    _debug_log('==== test_not_modified_from_backend ====')
    r1 = Request(1, 2)
    mod_time = 'Mon, 14 Oct 2013 11:00:00 GMT'
    cache_ctx.start_backend(CacheNotModifiedConfig(last_modified=mod_time))
    cache_ctx.start_backend(TestCacheServerConfig(), name='server')
    cache_ctx.start_balancer(CacheClientConfig())
    first_resp = cache_ctx.perform_request(http.request.get(path=str(r1), headers={'If-Modified-Since': mod_time}))

    asserts.status(first_resp, 304)
    cache_ctx.assert_not_from_cache(r1)


@pytest.mark.parametrize('version', ['HTTP/1.0', 'HTTP/1.1'], ids=['http10', 'http11'])
def test_not_modified(cache_ctx, version):
    """
    SEPE-4605
    Если клиент присылает заголовок If-Modified-Since, а в ответе бэкенда присутствует Last-Modified,
    значение которого <= запрошенного клиентом, то клиенту отсылается 304 Not Modified с пустым телом
    """
    _debug_log('==== test_not_modified ====')
    r1 = Request(1, 2)
    r2 = r1.copy()
    mod_time = 'Mon, 14 Oct 2013 11:00:00 GMT'
    cache_ctx.start_backend(SimpleConfig(http.response.ok(version=version,
                                                          headers={'Last-Modified': 'Mon, 14 Oct 2013 10:59:59 GMT'})))
    cache_ctx.start_backend(TestCacheServerConfig(valid_for=datetime.timedelta(seconds=10)), name='server')
    cache_ctx.start_balancer(CacheClientConfig())

    first_resp = cache_ctx.perform_request(http.request.get(path=str(r1), headers={'If-Modified-Since': mod_time}))
    time.sleep(0.1)
    second_resp = cache_ctx.perform_request(http.request.get(path=str(r2), headers={'If-Modified-Since': mod_time}))

    asserts.status(first_resp, 200)
    asserts.status(second_resp, 304)
    cache_ctx.assert_not_from_cache(r1)
    cache_ctx.assert_from_cache(r2)


def test_etag_match(cache_ctx):
    """
    Клиент посылает запрос с правильным If-None-Match, бэкенд отдает 304 ответ,
    ответ бэкенда пробрасывается клиенту.
    """
    r1 = Request(1, 2)
    cache_ctx.start_backend(CacheNotModifiedConfig(etag=ETAG_VALUE))
    cache_ctx.start_backend(TestCacheServerConfig(), name='server')
    cache_ctx.start_balancer(CacheClientConfig())

    resp = cache_ctx.perform_request(http.request.get(path=str(r1), headers={'If-None-Match': ETAG_VALUE}))
    asserts.header_value(resp, 'ETag', ETAG_VALUE)
    asserts.status(resp, 304)
    cache_ctx.assert_not_from_cache(r1)


def test_etag_mismatch(cache_ctx):
    """
    Клиент посылает запрос с неправильным If-None-Match, бэкенд отдает полный ответ,
    ответ бэкенда пробрасывается клиенту.
    """
    r1 = Request(1, 2)
    custom_etag_value = '"something"'
    cache_ctx.start_backend(CacheNotModifiedConfig(etag=custom_etag_value))
    cache_ctx.start_backend(TestCacheServerConfig(), name='server')
    cache_ctx.start_balancer(CacheClientConfig())

    resp = cache_ctx.perform_request(http.request.get(path=str(r1), headers={'If-None-Match': ETAG_VALUE}))
    asserts.header_value(resp, 'ETag', custom_etag_value)
    asserts.status(resp, 200)
    cache_ctx.assert_not_from_cache(r1)


def test_etag_from_cache(cache_ctx):
    """
    Клиент посылает запрос с If-None-Match, совпадающим с ETag закешированного запроса.
    Балансер отдает 304 из кеша.
    """
    r1 = Request(1, 2)
    cache_ctx.start_backend(CacheNotModifiedConfig(etag=ETAG_VALUE))
    cache_ctx.start_backend(TestCacheServerConfig(), name='server')
    cache_ctx.start_balancer(CacheClientConfig())

    resp = cache_ctx.perform_request(http.request.get(path=str(r1)))
    asserts.header_value(resp, 'ETag', ETAG_VALUE)
    asserts.status(resp, 200)
    cache_ctx.assert_not_from_cache(r1)

    r2 = r1.copy()
    resp = cache_ctx.perform_request(http.request.get(path=str(r2), headers={'If-None-Match': ETAG_VALUE}))
    # TODO: возвращать правильные заголовки при 304 с балансера
    # asserts.header_value(resp, 'ETag', ETAG_VALUE)
    asserts.status(resp, 304)
    cache_ctx.assert_from_cache(r2)


def test_validate_cached_etag(cache_ctx):
    """
    Клиент посылает запрос без If-None-Match, в кеше есть протухший запрос с правильным ETag.
    Клиенту отдается правильный 200 ответ.
    """
    r1 = Request(1, 2)
    ttl = 1
    cache_ctx.start_backend(CacheNotModifiedConfig(etag=ETAG_VALUE))
    # if you use module cache_server ere, then it responds on the second request with HTTP 200 OK
    # but with valid-until some time in the past and last-modified set to some insane value like
    # Last-Modified: Wed, 19 Jan 586524 08:01:49 GMT
    cache_ctx.start_backend(TestCacheServerConfig(valid_for=datetime.timedelta(seconds=ttl)), name='server')
    cache_ctx.start_balancer(CacheClientConfig())

    resp = cache_ctx.perform_request(http.request.get(path=str(r1)))
    asserts.header_value(resp, 'ETag', ETAG_VALUE)
    asserts.status(resp, 200)
    cache_ctx.assert_not_from_cache(r1)

    time.sleep(ttl + 1)

    r2 = r1.copy()
    resp = cache_ctx.perform_request(http.request.get(path=str(r2)))
    # TODO: возвращать правильные заголовки при 304 с балансера
    # asserts.header_value(resp, 'ETag', ETAG_VALUE)
    asserts.status(resp, 200)
    cache_ctx.assert_not_from_cache(r2)


def test_etag_last_modified(cache_ctx):
    """
    Клиент посылает запрос с тем же If-None-Match, но более ранним Last-Modified, чем данные в кеше.
    Клиенту отдается 200 ответ.
    """
    r1 = Request(1, 2)
    cache_ctx.start_backend(CacheNotModifiedConfig(etag=ETAG_VALUE, last_modified='Mon, 14 Oct 2013 11:00:00 GMT'))
    cache_ctx.start_backend(TestCacheServerConfig(), name='server')
    cache_ctx.start_balancer(CacheClientConfig())

    cache_ctx.perform_request(http.request.get(path=str(r1)))

    r2 = r1.copy()
    resp = cache_ctx.perform_request(http.request.get(path=str(r2), headers={
        'If-None-Match': ETAG_VALUE,
        'If-Modified-Since': 'Mon, 14 Oct 2013 10:59:59 GMT',
    }))
    asserts.status(resp, 200)


@pytest.mark.parametrize('data', ['ok', ['ok']], ids=['length', 'chunked'])
def test_backend_response(cache_ctx, data):
    """
    На запрос клиента backend отвечает с телом
    Балансер должен вернуть ответ клиенту и сохранить его в кеше
    """
    r1 = Request(1, 2)
    r2 = r1.copy()
    response = http.response.ok(data=data)
    cache_ctx.start_backend(SimpleConfig(response=response))
    cache_ctx.start_backend(TestCacheServerConfig(), name='server')
    cache_ctx.start_balancer(CacheClientConfig())

    first_resp = cache_ctx.perform_request(http.request.get(path=str(r1)))
    time.sleep(0.1)
    second_resp = cache_ctx.perform_request(http.request.get(path=str(r2)))

    asserts.status(first_resp, 200)
    asserts.content(first_resp, response.data.content)
    assert first_resp == second_resp
    cache_ctx.assert_not_from_cache(r1)
    cache_ctx.assert_from_cache(r2)


def check_ok_requests(cache_ctx, requests):
    for r in requests:
        time.sleep(0.1)
        response = cache_ctx.perform_request(http.request.get(path=str(r)))
        asserts.status(response, 200)


def test_small_memory_limit(cache_ctx):
    """
    SEPE-4690
    Ограничение по памяти выставляется равным 1024 байта
    Задается запрос GET /id=1&n=2 HTTP/1.1, затем дважды GET /id=2&n=3 HTTP/1.1
    На все три запроса балансер должен ответить 200
    """
    r1 = Request(1, 2)
    r2 = Request(2, 3)
    r3 = r2.copy()
    cache_ctx.start_backend(SimpleConfig())
    cache_ctx.start_balancer(CacheConfig(mem=1024))

    check_ok_requests(cache_ctx, [r1, r2, r3])


def test_cached_value_memory_drop(cache_ctx):
    """
    SEPE-4791
    При достижении ограничения по памяти старые запросы вытесняются из кеша новыми
    """
    _debug_log('=== test_cached_value_memory_drop ====')
    mem = 1024
    data = 'A' * (mem / 2)
    first_1 = Request(1, 2)
    first_2 = first_1.copy()
    second_1 = Request(3, 4)
    second_2 = second_1.copy()
    first_3 = first_1.copy()

    cache_ctx.start_backend(SimpleConfig(http.response.ok(data=data)))
    cache_ctx.start_balancer(CacheConfig(mem=mem))

    check_ok_requests(cache_ctx, [first_1, first_2, second_1, second_2, first_3])

    cache_ctx.assert_not_from_cache(first_1)
    cache_ctx.assert_from_cache(first_2)
    cache_ctx.assert_not_from_cache(second_1)
    cache_ctx.assert_from_cache(second_2)
    cache_ctx.assert_not_from_cache(first_3)


def test_process_memory_usage(cache_ctx):
    """
    SEPE-4764
    Если балансер запущен с дочерними процессами, то память под кеш должна выделяться только в одном из процессов
    """
    mem = 1024 * 1024 * 1024
    cache_ctx.start_backend(SimpleConfig())
    cache_ctx.start_balancer(CacheWorkersConfig(mem=mem))
    time.sleep(5)

    mem_usage = cache_ctx.balancer.get_memory_usage()
    high_mem_usage = filter(lambda usage: usage > mem, mem_usage)
    assert len(high_mem_usage) == 1


def test_errordoc(cache_ctx):
    """
    SEPE-4693
    Кеш не должен зависать, если под ним errordocument
    """
    cache_ctx.start_balancer(CacheErrordocConfig())
    response = cache_ctx.perform_request(http.request.get(path='/id=1&n=2'))

    asserts.status(response, 200)
    asserts.content(response, 'ololo')


def test_async_cache_init(cache_ctx):
    """
    Пока кеш не инициализировался запросы должны перенаправляться backend-у
    """
    r1 = Request(1, 2)
    r2 = r1.copy()
    r3 = r2.copy()
    cache_ctx.start_backend(SimpleConfig())
    cache_ctx.start_balancer(CacheWorkersConfig(mem=MAX_SIZE, async_init=True))

    check_ok_requests(cache_ctx, [r1])
    time.sleep(10)
    check_ok_requests(cache_ctx, [r2, r3])

    cache_ctx.assert_not_from_cache(r1)
    cache_ctx.assert_not_from_cache(r2)
    cache_ctx.assert_from_cache(r3)


def base_server_test(cache_ctx, put_request):
    cache_ctx.start_balancer(CacheServerConfig())

    response = cache_ctx.perform_request(put_request)
    asserts.status(response, 200)

    response = cache_ctx.perform_request(http.request.get(path=put_request.request_line.path))
    asserts.status(response, 200)
    asserts.content(response, put_request.data.content)
    return response


def test_server_put_get(cache_ctx):
    """
    SEPE-5561
    Положить значение на standalone сервер и получить это значение по ключу
    """
    # FIXME
    time.sleep(2)
    base_server_test(cache_ctx, http.request.put(path='/key', data='12345'))


def test_cache_server_no_last_modified(cache_ctx):
    """
    Если на cache server приходит PUT-запрос без заголовка Last-Modified,
    то должно сохраниться значение с Last-Modified: Wed, 19 Jan 586524 08:01:49 GMT
    """
    response = base_server_test(cache_ctx, http.request.put(path='/key', data='12345'))
    asserts.header_value(response, 'Last-Modified', 'Wed, 19 Jan 586524 08:01:49 GMT')


def test_cache_server_last_modified_from_future(cache_ctx):
    """
    Если на cache server приходит PUT-запрос с заголовком Last-Modified из будущего,
    то значение Last-Modified должно сохраниться
    """
    response = base_server_test(cache_ctx, http.request.put(path='/key', headers={'Last-Modified': FUTURE_DATE},
                                                            data='12345'))
    asserts.header_value(response, 'Last-Modified', FUTURE_DATE)


def test_server_not_found(cache_ctx):
    """
    SEPE-5561
    Если запросить у сервера значение по неизвестному ключу,
    то сервер вернет 404
    """
    cache_ctx.start_balancer(CacheServerConfig())

    response = cache_ctx.perform_request(http.request.get(path='/key'))
    asserts.status(response, 404)
    asserts.reason_phrase(response, 'Not found')


def test_client_server(cache_ctx):
    """
    Проверить клиент и сервер, запущенные в разных процессах
    """
    r1 = Request(1, 2)
    r2 = r1.copy()
    server = cache_ctx.start_balancer(CacheServerConfig())
    cache_ctx.start_backend(SimpleConfig())
    cache_ctx.start_balancer(CacheClientExplicitServerConfig(server_port=server.config.port))

    check_ok_requests(cache_ctx, [r1, r2])

    cache_ctx.assert_not_from_cache(r1)
    cache_ctx.assert_from_cache(r2)


def test_http10_not_chunked(cache_ctx):
    """
    SEPE-6742
    Backend всегда отвечает чанками и по HTTP/1.1
    Клиент задает запрос по HTTP/1.1, ответ попадает в кеш с чанками
    Затем клиент задает запрос с тем же ключом по HTTP/1.0
    Должен прийти ответ с content-length
    """
    r1 = Request(1, 2)
    cache_ctx.start_backend(SimpleConfig(response=http.response.ok(data=['ok'])))
    cache_ctx.start_backend(TestCacheServerConfig(), name='server')
    cache_ctx.start_balancer(CacheClientConfig())

    cache_ctx.perform_request(http.request.get(path=str(r1)))
    conn = cache_ctx.create_http_connection()
    response = conn.perform_request_raw_response(http.request.get(path=str(r1), version='HTTP/1.0'))

    asserts.is_content_length(response)


def test_http10_request_http10_backend(cache_ctx):
    """
    Если в кеше есть ответ backend-а по HTTP/1.0,
    то на HTTP/1.0 запрос балансер должен отдать закешированное значение
    """
    r1 = Request(1, 2)
    data = 'A' * 10
    cache_ctx.start_backend(SimpleConfig(response=http.response.ok(version='HTTP/1.0', data=data)))
    cache_ctx.start_backend(TestCacheServerConfig(), name='server')
    cache_ctx.start_balancer(CacheClientConfig())

    cache_ctx.perform_request(http.request.get(path=str(r1), version='HTTP/1.0'))

    conn = cache_ctx.create_http_connection()
    response = conn.perform_request_raw_response(http.request.get(path=str(r1), version='HTTP/1.0'))

    asserts.version(response, 'HTTP/1.0')
    asserts.is_content_length(response)
    asserts.content(response, data)


###############
def test_http11_request_http10_backend(cache_ctx):
    """
    Если в кеше есть ответ backend-а по HTTP/1.0,
    то на HTTP/1.1 запрос балансер должен отдать закешированное значение
    """
    r1 = Request(1, 2)
    data = 'A' * 10
    cache_ctx.start_backend(SimpleConfig(response=http.response.ok(version='HTTP/1.0', data=data)))
    cache_ctx.start_backend(TestCacheServerConfig(), name='server')
    cache_ctx.start_balancer(CacheClientConfig())
    cache_ctx.perform_request(http.request.get(path=str(r1), version='HTTP/1.0'))

    conn = cache_ctx.create_http_connection()
    response = conn.perform_request_raw_response(http.request.get(path=str(r1)))
    asserts.version(response, 'HTTP/1.0')
    asserts.is_content_length(response)
    asserts.content(response, data)


def test_no_cache_server(cache_ctx):
    """
    Если не работает cache server, то надо перенаправить запрос backend-у и отдать его ответ клиенту
    """
    r1 = Request(1, 2)
    data = 'A' * 10
    cache_ctx.start_fake_backend(name='server')
    cache_ctx.start_backend(SimpleConfig(response=http.response.ok(data=data)))
    cache_ctx.start_balancer(CacheClientConfig())
    response = cache_ctx.perform_request(http.request.get(path=str(r1)))

    asserts.content(response, data)


def test_cache_server_timeout(cache_ctx):
    """
    Если cache server таймаутится, то надо перенаправить запрос backend-у и отдать его ответ клиенту
    """
    r1 = Request(1, 2)
    data = 'A' * 10
    cache_ctx.start_backend(ChunkedConfig(response=http.response.ok(data=[data] * 4), chunk_timeout=3), name='server')
    cache_ctx.start_backend(SimpleConfig(response=http.response.ok(data=data)))
    cache_ctx.start_balancer(CacheClientConfig())
    response = cache_ctx.perform_request(http.request.get(path=str(r1)))

    asserts.content(response, data)


def test_cache_server_broken(cache_ctx):
    """
    Если cache server во время ответа обрубает соединение,
    то надо перенаправить запрос backend-у и отдать его ответ клиенту
    """
    r1 = Request(1, 2)
    data = 'A' * 10
    cache_ctx.start_backend(CloseConfig(
        response=http.response.raw_ok(headers={'Transfer-Encoding': 'chunked'}, data='5\r\nabcde\r\n')),
        name='server')
    cache_ctx.start_backend(SimpleConfig(response=http.response.ok(data=data)))
    cache_ctx.start_balancer(CacheClientConfig())
    response = cache_ctx.perform_request(http.request.get(path=str(r1)))

    asserts.content(response, data)


def assert_no_put_requests(cache_ctx):
    time.sleep(0.5)  # FIXME
    while not cache_ctx.server.state.requests.empty():
        req = cache_ctx.server.state.get_request()
        assert req.request_line.method != 'PUT'


def test_backend_timeout(cache_ctx):
    """
    Если во время ответа backend таймаутится, то балансер должен разорвать соединение с клиентом
    и ничего не складывать в кеш
    """
    r1 = Request(1, 2)
    cache_ctx.start_backend(SimpleConfig(), name='server')
    cache_ctx.start_backend(ChunkedConfig(response=http.response.ok(data=['A' * 10] * 4), chunk_timeout=3))
    cache_ctx.start_balancer(CacheClientConfig())
    cache_ctx.perform_request_xfail(http.request.get(str(r1)))

    assert_no_put_requests(cache_ctx)


@pytest.mark.parametrize('response_data',
                         ['server_response', ['server', '_', 'response']],
                         ids=['content_length', 'chunked'])
@pytest.mark.parametrize('request_version', ['HTTP/1.0', 'HTTP/1.1'], ids=['http10', 'http11'])
def test_cache_wrong_thumbdb_format(cache_ctx, request_version, response_data):
    """
    BALANCER-968
    If server responsds with http 200 and body does not contain
    full http response, request should not hang
    """
    cache_ctx.start_backend(SimpleConfig())
    now = datetime.datetime.utcnow()
    last_modified = now - datetime.timedelta(days=365)
    last_modified_str = last_modified.strftime('%a, %d %B %Y %H:%M:%S GMT')
    client_response = http.response.ok(data=response_data, headers={'Last-Modified': last_modified_str})
    cache_ctx.start_backend(SimpleConfig(response=client_response), name='server')
    cache_ctx.start_balancer(CacheClientConfig(match='(.*)'))
    start_time = datetime.datetime.now()
    cache_ctx.perform_request_xfail(http.request.get(version=request_version))
    end_time = datetime.datetime.now()
    assert (end_time - start_time) < datetime.timedelta(seconds=2)  # timeout for test is 10s


def test_broken_backend(cache_ctx):
    """
    Если во время ответа backend отрубается, то балансер должен разорвать соединение с клиентом
    и ничего не складывать в кеш
    """
    r1 = Request(1, 2)
    cache_ctx.start_backend(SimpleConfig(), name='server')
    cache_ctx.start_backend(CloseConfig(
        response=http.response.raw_ok(headers={'Transfer-Encoding': 'chunked'}, data='5\r\nabcde\r\n')))
    cache_ctx.start_balancer(CacheClientConfig())
    cache_ctx.perform_request_xfail(http.request.get(str(r1)))

    assert_no_put_requests(cache_ctx)


def test_cache_server_no_length_chunked(cache_ctx):
    """
    SEPE-6730
    Если на cache server приходит PUT-запрос без заголовков Content-Length и Transfer-Encoding,
    то балансер должен сохранить пустое значение
    """
    key = '/key'
    cache_ctx.start_balancer(CacheServerConfig())

    response = cache_ctx.perform_request(http.request.raw_put(path=key))
    asserts.status(response, 200)

    response = cache_ctx.perform_request(http.request.get(path=key))
    asserts.status(response, 200)
    asserts.empty_content(response)


def test_cache_server_empty_value(cache_ctx):
    """
    SEPE-6730
    Если на cache server приходит PUT-запрос c пустым телом,
    то балансер должен сохранить пустое значение
    """
    key = '/key'
    cache_ctx.start_balancer(CacheServerConfig())

    response = cache_ctx.perform_request(http.request.put(path=key, data=[]))
    asserts.status(response, 200)

    response = cache_ctx.perform_request(http.request.get(path=key))
    asserts.status(response, 200)
    asserts.empty_content(response)


def test_cache_server_http10_length_chunked(cache_ctx):
    """
    Если в запросе PUT по HTTP/1.0 есть и Content-Length и Transfer-Encoding,
    то данные должны читаться как при Content-Length
    """
    key = '/key'
    data = '5\r\nabcde\r\n5\r\n12345\r\n0\r\n\r\n'
    cache_ctx.start_balancer(CacheServerConfig())

    response = cache_ctx.perform_request(http.request.raw_put(
        path=key, version='HTTP/1.0',
        headers={'Transfer-Encoding': 'chunked', 'Content-Length': len(data)},
        data=data))
    asserts.status(response, 200)

    response = cache_ctx.perform_request(http.request.get(path=key))
    asserts.content(response, data)


def test_cache_server_http11_length_chunked(cache_ctx):
    """
    Если в запросе PUT по HTTP/1.1 есть и Content-Length и Transfer-Encoding,
    то данные должны читаться как при Transfer-Encoding
    """
    key = '/key'
    data = '5\r\nabcde\r\n5\r\n12345\r\n0\r\n\r\n'
    request = http.request.raw_put(
        path=key,
        headers={'Transfer-Encoding': 'chunked', 'Content-Length': len(data)},
        data=data)
    cache_ctx.start_balancer(CacheServerConfig())

    response = cache_ctx.perform_request(request)
    asserts.status(response, 200)

    response = cache_ctx.perform_request(http.request.get(path=key))
    asserts.content(response, 'abcde12345')


def test_cache_server_client_close(cache_ctx):
    """
    Если при передаче данных клиент отрубается,
    то в кеше ничего не должно сохраниться
    """
    key = '/key'
    cache_ctx.start_balancer(CacheServerConfig())

    conn = cache_ctx.create_http_connection()
    stream = conn.create_stream()
    stream.write_request(http.request.raw_put(path=key, headers={'Transfer-Encoding': 'chunked'}, data='5\r\n1234'))
    conn.close()

    response = cache_ctx.perform_request(http.request.get(path=key))
    asserts.status(response, 404)


def test_different_methods_not_mix(cache_ctx):
    """
    Если приходят запросы с методом, отличным от закешированного,
    то закешированное значение не должно возвращаться
    """
    r1 = Request(1, 2)
    r2 = r1.copy()
    cache_ctx.start_backend(SimpleConfig())
    cache_ctx.start_backend(TestCacheServerConfig(), name='server')
    cache_ctx.start_balancer(CacheClientConfig())

    resp1 = cache_ctx.perform_request(http.request.head(path=str(r1)))
    resp2 = cache_ctx.perform_request(http.request.get(path=str(r2)))

    asserts.status(resp1, 200)
    asserts.status(resp2, 200)
    cache_ctx.assert_not_from_cache(r2)


def test_values_for_multiple_methods(cache_ctx):
    """
    В кеше могут одновременно находиться запросы с одним путем, заданные с разными методами
    """
    r1 = Request(1, 2)
    r2 = r1.copy()
    r3 = r1.copy()
    r4 = r1.copy()
    cache_ctx.start_backend(SimpleConfig())
    cache_ctx.start_backend(TestCacheServerConfig(), name='server')
    cache_ctx.start_balancer(CacheClientConfig())

    resp1 = cache_ctx.perform_request(http.request.get(path=str(r1)))
    resp2 = cache_ctx.perform_request(http.request.head(path=str(r2)))
    resp3 = cache_ctx.perform_request(http.request.get(path=str(r3)))
    resp4 = cache_ctx.perform_request(http.request.head(path=str(r4)))

    asserts.status(resp1, 200)
    asserts.status(resp2, 200)
    asserts.status(resp3, 200)
    asserts.status(resp4, 200)
    cache_ctx.assert_from_cache(r3)
    cache_ctx.assert_from_cache(r4)


def test_no_cache_forbidden_method(cache_ctx):
    """
    При запросе с неподходящим методом результат кешироваться не будет
    """
    r1 = Request(1, 2)
    r2 = r1.copy()
    cache_ctx.start_backend(SimpleConfig())
    cache_ctx.start_backend(TestCacheServerConfig(), name='server')
    cache_ctx.start_balancer(CacheClientConfig())

    resp1 = cache_ctx.perform_request(http.request.put(path=str(r1)))
    resp2 = cache_ctx.perform_request(http.request.put(path=str(r2)))

    asserts.status(resp1, 200)
    asserts.status(resp2, 200)
    cache_ctx.assert_not_from_cache(r2)
