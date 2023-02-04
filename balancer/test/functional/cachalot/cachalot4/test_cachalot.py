# -*- coding: utf-8 -*-
import re
import pytest
import time
from balancer.test.util import asserts
from balancer.test.util import sync
from balancer.test.util.stdlib.multirun import Multirun
import balancer.test.plugin.context as mod_ctx
from balancer.test.util.predef import http
from balancer.test.util.predef.handler.server.http import SimpleConfig, SimpleHandler, State, DummyConfig,\
    ChunkedConfig, BrokenConfig
from configs import CachalotConfig, CacheDaemonConfig


CACHE_HEADERS = [
    'Cache-Control',
    'Content-Location',
    'Date',
    'ETag',
    'Expires',
    'Vary',

    # not actually cache headers, but balancer must proxy them too
    'Last-Modified',
    'Range',
    'Content-Range',
]

KEY = '/key'
CHECK_HEAD = '/check_head'
FUTURE_DATE = 'Mon, 01 Dec 2098 16:00:00 GMT'
NEXT_FUTURE_DATE = 'Tue, 02 Dec 2098 16:00:00 GMT'

CLIENT_DATA = 'client'
CACHER_DATA = 'cacher'
BACKEND_DATA = 'backend'
CHUNKED_DATA = ['Red', 'Hot', 'Chili', 'Peppers']
LENGTH_CHUNKED_DATA = ''.join(['{:X}\r\n{}\r\n'.format(len(data), data) for data in CHUNKED_DATA + ['']])

ETAG = '"zzz"'
PREV_LAST_MODIFIED = 'Mon, 20 Feb 2017 00:00:00 GMT'
LAST_MODIFIED = 'Tue, 21 Feb 2017 00:00:00 GMT'
NEXT_LAST_MODIFIED = 'Wed, 22 Feb 2017 00:00:00 GMT'


class CacheHandler(SimpleHandler):
    def handle_parsed_request(self, raw_request, stream):
        if raw_request.request_line.method == 'GET':
            self.state.get_reqs.put(raw_request)
            key = raw_request.request_line.path
            if key in self.config.data:
                stream.write_response(self.config.data[key])
            else:
                stream.write_response(self.config.get_err_response)
        elif raw_request.request_line.method == 'PUT':
            self.state.put_reqs.put(raw_request)
            stream.write_response(self.config.put_response)
        else:
            stream.write_response(http.response.raw_not_allowed())
        self.finish_response()
        self.force_close()


class CacheState(State):
    def __init__(self, config):
        super(CacheState, self).__init__(config)
        self.get_reqs = sync.Queue(config.queue_timeout)
        self.put_reqs = sync.Queue(config.queue_timeout)


class CacheConfig(SimpleConfig):
    HANDLER_TYPE = CacheHandler
    STATE_TYPE = CacheState

    def __init__(self, data, put_response, get_err_response=None):
        super(CacheConfig, self).__init__()
        self.data = data
        if get_err_response is None:
            get_err_response = http.response.not_found()
        if not get_err_response.is_raw_message():
            get_err_response = get_err_response.to_raw_response()
        self.get_err_response = get_err_response
        if not put_response.is_raw_message():
            put_response = put_response.to_raw_response()
        self.put_response = put_response


class CachedContext(object):
    def __init__(self):
        super(CachedContext, self).__init__()
        self.__cacher = None

    @property
    def cacher(self):
        return self.__cacher

    def perform_request_cacher(self, req):
        return self.perform_request(req, port=self.cacher.config.port)

    def put_to_cache(self, path, resp):
        if not resp.is_raw_message():
            resp = resp.to_raw_response()  # FIXME: items() method is useless with not raw message headers
        req = http.request.put(path=path, headers=resp.headers.items(), data=resp.data)
        return self.perform_request_cacher(req)

    def wait_in_cache(self, req, status=200):
        if isinstance(req, str):
            req = http.request.get(req)
        for run in Multirun():
            with run:
                resp = self.perform_request_cacher(req)
                asserts.status(resp, status)

    def start_cacher_check_status(self, data):
        cache_dir = self.manager.fs.create_dir('cache_dir')
        cacher = self.manager.cachedaemon.start(CacheDaemonConfig(cache_dir=cache_dir))
        self.manager.config.add_server('cacher', cacher.config)
        self.__cacher = cacher

        raw_data = [(path, resp.to_raw_response(), status) for path, resp, status in data]
        for path, resp, status in raw_data:
            resp = self.put_to_cache(path, resp)
            asserts.status(resp, status)
        return cacher

    def start_cacher(self, data=None):
        if data is None:
            data = list()
        if isinstance(data, dict):
            data = data.items()
        return self.start_cacher_check_status([(path, resp, 200) for path, resp in data])

    def start_cachalot_balancer(self, **kwargs):
        return self.start_balancer(CachalotConfig(**kwargs))

    def __start_balancer_backend(self, backend_resp=None):
        if backend_resp is None:
            backend_resp = http.response.ok(data='backend')
        self.start_backend(SimpleConfig(response=backend_resp))
        self.start_cachalot_balancer()

    def start_all(self, data=None, backend_resp=None):
        self.start_cacher(data)
        self.__start_balancer_backend(backend_resp)

    def assert_in_cache(self, req, response):
        for run in Multirun():
            with run:
                cacher_response = self.perform_request_cacher(req)
                asserts.status(cacher_response, response.response_line.status)
                # TODO: assert headers
                assert cacher_response.data.content == response.data.content

    def assert_not_in_cache(self, key):
        cacher_response = self.perform_request_cacher(http.request.get(key))
        asserts.status(cacher_response, 404)


cached_ctx = mod_ctx.create_fixture(CachedContext)


def assert_cache_headers(msg, expected):
    expected_headers = {
        name: expected.headers.get_one(name) for name in CACHE_HEADERS if name in expected.headers
    }
    asserts.headers_values(msg, expected_headers)


PREV_LAST_MODIFIED = 'Mon, 20 Feb 2017 00:00:00 GMT'

# copy-pasted from BaseHTTPServer
WEEKDAY_NAME = ['Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat', 'Sun']
MONTH_NAME = [None, 'Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun', 'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec']


def time_format(timestamp):
    year, month, day, hh, mm, ss, wd, y, z = time.gmtime(timestamp)
    s = "%s, %02d %3s %4d %02d:%02d:%02d GMT" % (
        WEEKDAY_NAME[wd],
        day, MONTH_NAME[month], year,
        hh, mm, ss)
    return s


def get_boundary(msg):
    asserts.single_header(msg, 'Content-Type')
    content_type = msg.headers.get_one('Content-Type')
    match_res = re.match(r'.*; boundary=(\S+)', content_type)
    assert match_res is not None, 'cannot parse content-type header value: {}'.format(content_type)
    return match_res.groups()[0]


def build_multipart_content(data, ranges, boundary):
    def build_part(start, fin):
        return '--{}\r\nContent-Range: bytes {}-{}/{}\r\n\r\n{}\r\n'.format(
            boundary, start, fin, len(data), data[start:fin + 1])

    return ''.join([build_part(start, fin) for start, fin in ranges]) + '--' + boundary + '--'


def multipart_response(headers, data, ranges, boundary=None):
    if boundary is None:
        boundary = 'BOUNDARY'

    headers['Content-Type'] = 'multipart/byteranges; boundary={}'.format(boundary)

    return http.response.partial_content(
        headers=headers,
        data=build_multipart_content(data, ranges, boundary),
    )


def base_cachedaemon_test(cached_ctx, req, resp, expected_resp):
    cached_ctx.start_all({req.request_line.path: resp})
    cached_ctx.wait_in_cache(req, expected_resp.status)

    cached_resp = cached_ctx.perform_request(req)
    asserts.status(cached_resp, expected_resp.response_line.status)
    assert_cache_headers(cached_resp, expected_resp)
    asserts.content(cached_resp, expected_resp.data.content)
    assert cached_ctx.backend.state.requests.empty()


def ids_parametrize(name, data):
    ids, values = zip(*data)
    return pytest.mark.parametrize(name, values, ids=ids)


CACHE_REQUESTS = [
    ('simple', http.request.get(KEY)),
    ('max_age', http.request.get(KEY, headers={'Cache-Control': 'max-age=3600'})),
    ('range',  http.request.get(KEY, headers={'Range': 'bytes=1-3'})),
    ('multipart',  http.request.get(KEY, headers={'Range': 'bytes=1-3,151-153'})),
    ('vary_all', http.request.get(KEY, headers={'Vary': '*'})),
    ('vary', http.request.get(KEY, headers={'Vary': 'led', 'led': 'zeppelin'})),
]

ONLY_CACHE_REQUESTS = [
    ('only_if_cached', http.request.get(KEY, headers={'Cache-Control': 'only-if-cached'})),
]

NO_CACHE_REQUESTS = [
    ('no_cache', http.request.get(headers={'Cache-Control': 'no-cache'})),
]

NO_STORE_REQUESTS = [
    ('no_store', http.request.get(headers={'Cache-Control': 'no-store'})),
    ('authorization', http.request.get(headers={'Authorization': 'Led Zeppelin'})),
]

NO_CACHE_NO_STORE_REQUESTS = [
    ('put', http.request.put(KEY, data=CLIENT_DATA)),
    ('post', http.request.put(KEY, data=CLIENT_DATA)),
    ('chunked_put', http.request.put(KEY, data=CHUNKED_DATA)),
    ('chunked_post', http.request.put(KEY, data=CHUNKED_DATA)),
    ('no_cache_no_store', http.request.get(headers={'Cache-Control': 'no-cache,no-store'}, data=CLIENT_DATA)),
    # ('unknown', http.request.custom('UNKNOWN', KEY)),
]


def build_head_requests(requests):
    def build_one(req):
        is_raw = req.is_raw_message()
        if not is_raw:
            req = req.to_raw_request()
        result = http.request.raw_head(
            path=req.request_line.path,
            version=req.request_line.version,
            headers=req.headers.items(),
        )
        if not is_raw:
            result = result.to_request()
        return result

    return [(name, build_one(req)) for name, req in requests if req.request_line.method == 'GET']


__CACHE_RESPONSES = [
    ('expires', http.response.ok(headers={'Expires': FUTURE_DATE}, data=CACHER_DATA)),
    ('last_modified_future', http.response.ok(headers={'Expires': FUTURE_DATE, 'Last-Modified': FUTURE_DATE},
                                              data=CACHER_DATA)),
    ('chunked', http.response.ok(headers={'Expires': FUTURE_DATE}, data=CHUNKED_DATA)),
    ('range', http.response.partial_content(headers={'Expires': FUTURE_DATE, 'Content-Range': 'bytes 1-3/6'},
                                            data='ach')),
    ('multipart', multipart_response(headers={'Expires': FUTURE_DATE}, data='X' * 100 + 'Y' * 100,
                                     ranges=[(1, 3), (151, 153)])),
    ('max_age', http.response.ok(headers={'Cache-Control': 'max-age=3600'}, data=CACHER_DATA)),
    ('zero_max_age', http.response.ok(headers={'Cache-Control': 'max-age=0'}, data=CACHER_DATA)),
    ('length_chunked',  http.response.raw_ok(
        headers={'Expires': FUTURE_DATE, 'Transfer-Encoding': 'chunked', 'Content-Length': len(LENGTH_CHUNKED_DATA)},
        data=CHUNKED_DATA,
    )),
    ('must_revalidate', http.response.ok(headers={'Expires': FUTURE_DATE, 'Cache-Control': 'must-revalidate'},
                                         data=CACHER_DATA)),
    ('public',  http.response.ok(headers={'Expires': FUTURE_DATE, 'Cache-Control': 'public'}, data=CACHER_DATA)),
    ('s_maxage', http.response.ok(headers={'Cache-Control': 's-maxage=3600'}, data=CACHER_DATA)),
]

__NO_CACHE_OK_RESPONSES = [
    ('no_expiration_time', http.response.ok(headers={'Cache-Control': 'public'}, data=CACHER_DATA)),
    ('no_cache', http.response.ok(headers={'Expires': FUTURE_DATE, 'Cache-Control': 'no-cache'}, data=CACHER_DATA)),
    ('private', http.response.ok(headers={'Expires': FUTURE_DATE, 'Cache-Control': 'private'}, data=CACHER_DATA)),
    ('private_public', http.response.ok(headers={'Expires': FUTURE_DATE, 'Cache-Control': 'private, public'},
                                        data=CACHER_DATA)),
    ('public_private', http.response.ok(headers={'Expires': FUTURE_DATE, 'Cache-Control': 'public, private'},
                                        data=CACHER_DATA)),
    ('vary_all', http.response.ok(headers={'Expires': FUTURE_DATE, 'Vary': '*'}, data=CLIENT_DATA)),
    ('vary', http.response.ok(headers={'Expires': FUTURE_DATE, 'Vary': 'led', 'led': 'zeppelin'}, data=CLIENT_DATA)),
    ('not_modified_etag', http.response.not_modified(headers={'Expires': FUTURE_DATE, 'ETag': ETAG})),
    ('not_modified_last_modified', http.response.not_modified(
        headers={'Expires': FUTURE_DATE, 'Last-Modified': LAST_MODIFIED}
    )),
]

__ERR_CACHE_RESPONSES = [
    ('not_found', http.response.not_found(headers={'Expires': FUTURE_DATE}, data=CACHER_DATA)),
    ('service_unavailable', http.response.service_unavailable(headers={'Expires': FUTURE_DATE}, data=CACHER_DATA)),
    # ('unknown', http.response.custom(900, reason='UNKNOWN', headers={'Cache-Control': 'public'})),
]

OK_CACHER_RESPONSES = __CACHE_RESPONSES + __NO_CACHE_OK_RESPONSES
ERR_CACHER_RESPONSES = __ERR_CACHE_RESPONSES

CACHE_BACKEND_RESPONSES = __CACHE_RESPONSES
NO_CACHE_BACKEND_RESPONSES = __NO_CACHE_OK_RESPONSES + __ERR_CACHE_RESPONSES

CACHER_PUT_RESPONSES = [
    ('ok', http.response.ok()),
    ('not_found', http.response.not_found()),
    ('service_unavailable', http.response.service_unavailable()),
]


def build_empty_data_responses(responses):
    def build_one(resp):
        is_raw = resp.is_raw_message()
        if not is_raw:
            resp = resp.to_raw_response()
        result = http.response.raw_custom(
            status=resp.response_line.status,
            reason=resp.response_line.reason_phrase,
            version=resp.response_line.version,
            headers=resp.headers.items(),
            data=None,
        )
        if not is_raw:
            result = result.to_response()
        return result

    return [(name, build_one(resp)) for name, resp in responses]


def base_no_cached_value_test(ctx, req, backend_resp):
    ctx.start_backend(SimpleConfig(response=backend_resp))
    ctx.start_balancer(CachalotConfig())

    resp = ctx.perform_request(req)

    for run in Multirun():
        with run:
            log = ctx.manager.fs.read_file(ctx.balancer.config.accesslog)
            assert 'cacher' in log

    backend_req = ctx.backend.state.get_request()
    cacher_get_req = ctx.cacher.state.get_request()
    cacher_put_req = ctx.cacher.state.get_request()

    asserts.status(resp, backend_resp.response_line.status)
    assert_cache_headers(resp, backend_resp)
    asserts.content(resp, backend_resp.data.content)

    asserts.method(cacher_get_req, 'GET')
    asserts.path(cacher_get_req, req.request_line.path)
    assert_cache_headers(cacher_get_req, req)

    asserts.path(backend_req, req.request_line.path)
    assert_cache_headers(backend_req, req)

    asserts.method(cacher_put_req, 'PUT')
    asserts.path(cacher_put_req, req.request_line.path)
    asserts.content(cacher_put_req, backend_resp.data.content)
    assert_cache_headers(cacher_put_req, backend_resp)


INVALID_CACHER_CONFIGS = [
    ('dummy', DummyConfig()),
    ('broken', BrokenConfig()),
    ('timeout', ChunkedConfig(
        response=http.response.ok(data=CHUNKED_DATA),
        chunk_timeout=3
    )),
]


@pytest.mark.parametrize(
    'resp',
    [
        http.response.ok(data=CACHER_DATA),
        http.response.ok(headers={'Expires': FUTURE_DATE, 'Cache-Control': 'no-cache'}, data=CACHER_DATA),
        http.response.ok(headers={'Expires': FUTURE_DATE, 'Cache-Control': 'no-store'}, data=CACHER_DATA),
        http.response.ok(headers={'Expires': FUTURE_DATE, 'Cache-Control': 'private'}, data=CACHER_DATA),
        http.response.ok(headers={'Expires': FUTURE_DATE, 'Content-Range': 'bytes 5-0/6'}, data=CACHER_DATA),
        http.response.ok(headers={'Expires': FUTURE_DATE, 'Vary': '*'}, data=CACHER_DATA),
        http.response.ok(headers={'Expires': FUTURE_DATE, 'Vary': 'led', 'led': 'zeppelin'}, data=CACHER_DATA),
    ],
    ids=[
        'no_explicit_expiration_time',
        'no_cache',
        'no_store',
        'private',
        'wrong_bytes',
        'vary_all',
        'vary',
    ]
)
def test_put_not_cachable_to_cachedaemon(cached_ctx, resp):
    """
    Cachedaemon must not store not cachable responses
    """
    cached_ctx.start_cacher()
    put_resp = cached_ctx.put_to_cache(KEY, resp)

    asserts.status(put_resp, 400)
    cached_ctx.assert_not_in_cache(KEY)


@ids_parametrize('req', CACHE_REQUESTS)
@ids_parametrize('backend_resp', CACHE_BACKEND_RESPONSES)
@ids_parametrize('cacher_get_resp', ERR_CACHER_RESPONSES)
@ids_parametrize('cacher_put_resp', CACHER_PUT_RESPONSES)
def test_put_to_cache(ctx, req, backend_resp, cacher_get_resp, cacher_put_resp):
    """
    Balancer must put backend response to cacher
    """
    ctx.start_backend(CacheConfig({}, put_response=cacher_put_resp, get_err_response=cacher_get_resp), name='cacher')
    base_no_cached_value_test(ctx, req, backend_resp)


@pytest.mark.parametrize(
    ['req', 'backend_resp'],
    [
        (
            http.request.get('/key'),
            http.response.ok(headers={'Expires': FUTURE_DATE}, data='Y' * 10),
        ),
        (
            http.request.get('/key'),
            http.response.ok(headers={'Expires': FUTURE_DATE}, data=CHUNKED_DATA),
        ),
        (
            http.request.get('/key'),
            http.response.ok(headers={'Expires': FUTURE_DATE, 'Cache-Control': 'public'}, data='Y' * 10),
        ),
        (
            http.request.get('/key', headers={'Range': 'bytes=1-3'}),
            http.response.partial_content(
                headers={'Expires': FUTURE_DATE, 'Content-Range': 'bytes 1-3/6'},
                data='ach',
            ),
        ),
        (
            http.request.get('/key'),
            http.response.ok(headers={'Cache-Control': 'max-age=3600'}, data='Y' * 10),
        ),
        (
            http.request.get('/key'),
            http.response.ok(headers={'Cache-Control': 's-maxage=3600'}, data='Y' * 10),
        ),
    ],
    ids=[
        'not_expired',
        'chunked',
        'cache_control_public',
        'range',
        'max_age',
        's_maxage',
    ]
)
def test_put_to_cachedaemon(cached_ctx, req, backend_resp):
    """
    Balancer must put backend response to cacher
    """
    cached_ctx.start_all(backend_resp=backend_resp)

    resp = cached_ctx.perform_request(req)

    assert resp == backend_resp
    asserts.path(cached_ctx.backend.state.get_request(), req.request_line.path)
    cached_ctx.assert_in_cache(req, backend_resp)


@pytest.mark.parametrize(
    ['req', 'backend_resp', 'second_req', 'encoded_result', 'expected_body'],
    [
        (
            http.request.get('/key'),
            http.response.ok(data='Y' * 10, headers={'Cache-Control': 's-maxage=3600'}),
            http.request.get('/key', headers={'Accept-Encoding': 'gzip'}),
            True,
            'H4sIAAAAAAAAA4uMhAEAj9h6NgoAAAA='  # base64
        ),
        (
            http.request.get('/key', headers={'Accept-Encoding': 'gzip'}),
            http.response.ok(headers={'Content-Encoding': 'gzip', 'Cache-Control': 's-maxage=3600'}, data=('Y' * 10).encode("zlib")),
            http.request.get('/key', headers={'Accept-Encoding': 'gzip'}),
            True,
            'H4sIAAAAAAAAA4uMhAEAj9h6NgoAAAA='  # base64
        ),
        (
            http.request.get('/key', headers={'Accept-Encoding': 'gzip'}),
            http.response.ok(headers={'Content-Encoding': 'gzip', 'Cache-Control': 's-maxage=3600'}, data=('Y' * 10).encode("zlib")),
            http.request.get('/key'),
            False,
            ('Y' * 10)
        ),
    ],
    ids=[
        'no_encoding-gzip',
        'gzip-gzip',
        'gzip-no_encoding',
    ]
)
def test_put_content_encoding(cached_ctx, req, backend_resp, second_req, encoded_result, expected_body):
    cached_ctx.start_all(backend_resp=backend_resp)

    resp = cached_ctx.perform_request(req)
    time.sleep(2)
    second_resp = cached_ctx.perform_request(second_req)

    assert resp == backend_resp
    if not encoded_result:
        assert expected_body in second_resp.data.content
    else:
        import base64
        assert base64.b64decode(expected_body) in second_resp.data.content
