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
    stats = cached_ctx.get_unistat()
    assert stats['cachalot-cache_hit_summ'] == 1


@pytest.mark.parametrize(
    ['req', 'resp', 'expected_resp'],
    [
        (
            http.request.get(KEY, headers={'Range': 'bytes=1-3'}),
            http.response.ok(headers={'Expires': FUTURE_DATE}, data='cacher'),
            http.response.partial_content(
                headers={'Expires': FUTURE_DATE, 'Content-Range': 'bytes 1-3/6'},
                data='ach',
            ),
        ),
        (
            http.request.get(KEY, headers={'Range': 'bytes=1-3'}),
            http.response.partial_content(
                headers={'Expires': FUTURE_DATE, 'Content-Range': 'bytes 1-3/6'},
                data='ach',
            ),
            http.response.partial_content(
                headers={'Expires': FUTURE_DATE, 'Content-Range': 'bytes 1-3/6'},
                data='ach',
            ),
        ),
        (
            http.request.get(KEY, headers={'Range': 'bytes=2-3'}),
            http.response.partial_content(
                headers={'Expires': FUTURE_DATE, 'Content-Range': 'bytes 1-4/6'},
                data='ache',
            ),
            http.response.partial_content(
                headers={'Expires': FUTURE_DATE, 'Content-Range': 'bytes 2-3/6'},
                data='ch',
            ),
        ),
        (
            http.request.get(KEY, headers={'Range': 'bytes=2-'}),
            http.response.ok(headers={'Expires': FUTURE_DATE}, data='cacher'),
            http.response.partial_content(
                headers={'Expires': FUTURE_DATE, 'Content-Range': 'bytes 2-5/6'},
                data='cher',
            ),
        ),
        (
            http.request.get(KEY, headers={'Range': 'bytes=-4'}),
            http.response.ok(headers={'Expires': FUTURE_DATE}, data='cacher'),
            http.response.partial_content(
                headers={'Expires': FUTURE_DATE, 'Content-Range': 'bytes 2-5/6'},
                data='cher',
            ),
        ),
    ],
    ids=[
        'put_full',
        'get_eq',
        'get_le',
        'final_bytes',
        'final_bytes_count',
    ],
)
def test_get_from_cachedaemon_range(cached_ctx, req, resp, expected_resp):
    """
    Simple cachdeamon tests -- put and get range data
    """
    base_cachedaemon_test(cached_ctx, req, resp, expected_resp)


def test_cachedaemon_incomplete_range(cached_ctx):
    """
    If not full response stored in cache, then cachedaemon must send 404 (Not Found)
    response to non-range request
    """
    cached_ctx.start_cacher({KEY: http.response.partial_content(
        headers={'Expires': FUTURE_DATE, 'Content-Range': 'bytes 1-3/6'},
        data='ach',
    )})

    cached_ctx.assert_not_in_cache(KEY)


def test_get_from_cachedaemon_multipart_range(cached_ctx):
    """
    Balancer must return multipart range response from cacher and must not send request to backend
    """
    data = 'X' * 100 + 'Y' * 100
    cacher_resp = http.response.ok(headers={'Expires': FUTURE_DATE}, data='X' * 100 + 'Y' * 100)
    req = http.request.get(KEY, headers={'Range': 'bytes=1-3,151-153'})
    cached_ctx.start_all({KEY: cacher_resp})
    cached_ctx.wait_in_cache(req, status=206)

    resp = cached_ctx.perform_request(req)

    asserts.status(resp, 206)

    boundary = get_boundary(resp)
    asserts.content(resp, build_multipart_content(data, [(1, 3), (151, 153)], boundary))
    assert cached_ctx.backend.state.requests.empty()


@pytest.mark.parametrize(
    'expires',
    [FUTURE_DATE, NEXT_FUTURE_DATE],
    ids=['equal_expires', 'diff_expires'],
)
def test_cachedaemon_join_ranges_etag(cached_ctx, expires):
    """
    Cachedaemon must join multiple range responses with same ETag
    """
    cached_ctx.start_all([
        (KEY, http.response.ok(
            headers={'Content-Range': 'bytes 1-3/6', 'ETag': ETAG, 'Expires': FUTURE_DATE},
            data='ach',
        )),
        (KEY, http.response.ok(
            headers={'Content-Range': 'bytes 2-4/6', 'ETag': ETAG, 'Expires': expires},
            data='che',
        )),
    ])
    req = http.request.get(KEY, headers={'Range': 'bytes=1-4'})
    cached_ctx.wait_in_cache(req, status=206)
    resp = cached_ctx.perform_request(req)

    asserts.content(resp, 'ache')
    asserts.header_value(resp, 'ETag', ETAG)


@pytest.mark.parametrize(
    'has_etag',
    [(False, False), (False, True), (True, False)],
    ids=['no_etag', 'etag_in_put', 'etag_in_cache']
)
@pytest.mark.parametrize(
    'expires',
    [FUTURE_DATE, NEXT_FUTURE_DATE],
    ids=['equal_expires', 'diff_expires'],
)
def test_cachedaemon_join_ranges_last_modified(cached_ctx, has_etag, expires):
    """
    If there is no ETag in cache or in PUT request and cache and PUT request have the same Last-Modified value
    then cachedaemon must join stored data with data in PUT request
    """
    first_headers = {'Content-Range': 'bytes 1-3/6', 'Last-Modified': LAST_MODIFIED, 'Expires': FUTURE_DATE}
    if has_etag[0]:
        first_headers['ETag'] = ETAG
    second_headers = {'Content-Range': 'bytes 2-4/6', 'Last-Modified': LAST_MODIFIED, 'Expires': expires}
    if has_etag[1]:
        second_headers['ETag'] = ETAG

    cached_ctx.start_all([
        (KEY, http.response.ok(
            headers=first_headers,
            data='ach',
        )),
        (KEY, http.response.ok(
            headers=second_headers,
            data='che',
        )),
    ])
    req = http.request.get(KEY, headers={'Range': 'bytes=1-4'})
    cached_ctx.wait_in_cache(req, status=206)
    resp = cached_ctx.perform_request(req)

    asserts.content(resp, 'ache')
    asserts.header_value(resp, 'Last-Modified', LAST_MODIFIED)


def test_cachedaemon_join_ranges_full(cached_ctx):
    """
    Cachedaemon must join multiple range responses into full response
    """
    cached_ctx.start_all([
        (KEY, http.response.ok(
            headers={'Content-Range': 'bytes 0-3/6', 'ETag': ETAG, 'Expires': FUTURE_DATE},
            data='cach',
        )),
        (KEY, http.response.ok(
            headers={'Content-Range': 'bytes 2-5/6', 'ETag': ETAG, 'Expires': FUTURE_DATE},
            data='cher',
        )),
    ])
    cached_ctx.wait_in_cache(KEY)
    resp = cached_ctx.perform_request(http.request.get(KEY))

    asserts.status(resp, 200)
    asserts.content(resp, 'cacher')


def test_cachedaemon_ranges_diff_etag(cached_ctx):
    """
    Cachedaemon must use the latest range response and
    must not join multiple range responses with different ETag values
    """
    cached_ctx.start_all([
        (KEY, http.response.ok(
            headers={'Content-Range': 'bytes 0-3/6', 'ETag': '"zzz"', 'Expires': FUTURE_DATE},
            data='cach',
        )),
        (KEY, http.response.ok(
            headers={'Content-Range': 'bytes 2-5/6', 'ETag': '"yyy"', 'Expires': FUTURE_DATE},
            data='cdef',
        )),
    ])
    req = http.request.get(KEY, headers={'Range': 'bytes=2-5'})
    cached_ctx.wait_in_cache(req, 206)
    resp_range = cached_ctx.perform_request(req)

    cached_ctx.assert_not_in_cache(KEY)

    asserts.status(resp_range, 206)
    asserts.content(resp_range, 'cdef')


def test_cachedaemon_not_replace_full_with_range(cached_ctx):
    """
    Cachedaemon must not replace previously stored full response with new range response
    if expiration time is equal
    """
    cached_ctx.start_all({KEY: http.response.ok(
        headers={'Expires': FUTURE_DATE},
        data='old',
    )})
    cached_ctx.wait_in_cache(KEY)

    put_resp = cached_ctx.put_to_cache(KEY, http.response.ok(
        headers={'Content-Range': 'bytes 1-3/6', 'Expires': FUTURE_DATE},
        data='ach',
    ))
    asserts.status(put_resp, 422)

    resp_range = cached_ctx.perform_request(http.request.get(KEY, headers={'Range': 'bytes=1-2'}))

    asserts.status(resp_range, 206)
    asserts.content(resp_range, 'ld')


def test_cachedaemon_not_replace_range_with_range(cached_ctx):
    """
    Cachedaemon must not replace previously stored range response with new rangne response
    if expiration time is equal
    """
    req = http.request.get(KEY, headers={'Range': 'bytes=2-3'})
    cached_ctx.start_all({KEY: http.response.ok(
        headers={'Content-Range': 'bytes 1-3/6', 'Expires': FUTURE_DATE},
        data='old',
    )})
    cached_ctx.wait_in_cache(req, status=206)

    put_resp = cached_ctx.put_to_cache(KEY, http.response.ok(
        headers={'Content-Range': 'bytes 2-4/6', 'Expires': FUTURE_DATE},
        data='new',
    ))
    asserts.status(put_resp, 422)

    resp_range = cached_ctx.perform_request(req)

    asserts.status(resp_range, 206)
    asserts.content(resp_range, 'ld')


def test_cachedaemon_replace_full_with_range_last_modified(cached_ctx):
    """
    Cachedaemon must replace stored full response with new range response with more recent Last-Modified value
    """
    cached_ctx.start_all([
        (KEY, http.response.ok(
            headers={'Last-Modified': LAST_MODIFIED, 'Expires': FUTURE_DATE},
            data='cacher',
        )),
        (KEY, http.response.ok(
            headers={'Last-Modified': NEXT_LAST_MODIFIED, 'Expires': FUTURE_DATE,
                     'Content-Range': 'bytes 1-3/6'},
            data='new',
        )),
    ])
    for run in Multirun():
        with run:
            resp = cached_ctx.perform_request(http.request.get(KEY, headers={'Range': 'bytes=1-3'}))

            asserts.status(resp, 206)
            asserts.content(resp, 'new')


def test_cachedaemon_if_range_etag_match(cached_ctx):
    """
    If If-Range header value is equal to stored ETag
    then cached must send 206 response with range data
    """
    cached_ctx.start_all({KEY: http.response.ok(
        headers={'Expires': FUTURE_DATE, 'ETag': ETAG},
        data=CACHER_DATA,
    )})
    cached_ctx.wait_in_cache(KEY)

    resp = cached_ctx.perform_request(http.request.get(KEY, headers={
        'Range': 'bytes=1-3',
        'If-Range': ETAG,
    }))

    asserts.status(resp, 206)
    asserts.content(resp, CACHER_DATA[1:4])


def test_cachedaemon_if_range_etag_fail(cached_ctx):
    """
    If If-Range header value is not equal to stored ETag
    then cached must send 200 response with full data
    """
    cached_ctx.start_all({KEY: http.response.ok(
        headers={'Expires': FUTURE_DATE, 'ETag': ETAG},
        data=CACHER_DATA,
    )})
    cached_ctx.wait_in_cache(KEY)

    resp = cached_ctx.perform_request(http.request.get(KEY, headers={
        'Range': 'bytes=1-3',
        'If-Range': '"yyy"',
    }))

    asserts.status(resp, 200)
    asserts.content(resp, CACHER_DATA)


def test_cachedaemon_if_range_etag_fail_invalid_range(cached_ctx):
    """
    If If-Range header value is not equal to stored ETag
    then cached must send 200 response with full data even if value of Range header is invalid
    """
    cached_ctx.start_all({KEY: http.response.ok(
        headers={'Expires': FUTURE_DATE, 'ETag': ETAG},
        data=CACHER_DATA,
    )})
    cached_ctx.wait_in_cache(KEY)

    resp = cached_ctx.perform_request(http.request.get(KEY, headers={
        'Range': 'error',
        'If-Range': '"yyy"',
    }))

    asserts.status(resp, 200)
    asserts.content(resp, CACHER_DATA)


def test_cachedaemon_if_range_no_range(cached_ctx):
    """
    If there is not Range header in request then cached must ignore If-Range header
    """
    cached_ctx.start_all({KEY: http.response.ok(
        headers={'Expires': FUTURE_DATE, 'ETag': ETAG},
        data=CACHER_DATA,
    )})
    cached_ctx.wait_in_cache(KEY)

    resp = cached_ctx.perform_request(http.request.get(KEY, headers={
        'If-Range': ETAG,
    }))

    asserts.status(resp, 200)
    asserts.content(resp, CACHER_DATA)


def test_cachedaemon_if_range_last_modified_match(cached_ctx):
    """
    If If-Range header value is equal (not earlier than or equal) to stored Last-Modified
    then cached must send 206 response with range data
    """
    cached_ctx.start_all({KEY: http.response.ok(
        headers={'Expires': FUTURE_DATE, 'Last-Modified': LAST_MODIFIED},
        data=CACHER_DATA,
    )})
    cached_ctx.wait_in_cache(KEY)

    resp = cached_ctx.perform_request(http.request.get(KEY, headers={
        'Range': 'bytes=1-3',
        'If-Range': LAST_MODIFIED,
    }))

    asserts.status(resp, 206)
    asserts.content(resp, CACHER_DATA[1:4])


@pytest.mark.parametrize(
    'if_range_value',
    [PREV_LAST_MODIFIED, NEXT_LAST_MODIFIED],
    ids=['before', 'after']
)
def test_cachedaemon_if_range_last_modified_fail(cached_ctx, if_range_value):
    """
    If If-Range header value is not equal to stored Last-Modified
    then cached must send 200 response with full data
    """
    cached_ctx.start_all({KEY: http.response.ok(
        headers={'Expires': FUTURE_DATE, 'Last-Modified': LAST_MODIFIED},
        data=CACHER_DATA,
    )})
    cached_ctx.wait_in_cache(KEY)

    resp = cached_ctx.perform_request(http.request.get(KEY, headers={
        'Range': 'bytes=1-3',
        'If-Range': if_range_value,
    }))

    asserts.status(resp, 200)
    asserts.content(resp, CACHER_DATA)


def test_cachedaemon_if_range_last_modified_invalid(cached_ctx):
    """
    If If-Range header value is an invalid HTTP-date
    then cached must send 400 response
    """
    cached_ctx.start_all({KEY: http.response.ok(
        headers={'Expires': FUTURE_DATE, 'Last-Modified': LAST_MODIFIED},
        data=CACHER_DATA,
    )})
    cached_ctx.wait_in_cache(KEY)

    resp = cached_ctx.perform_request_cacher(http.request.get(KEY, headers={
        'Range': 'bytes=1-3',
        'If-Range': 'Mon, 01 Dec 2098 25:00:00 GMT',
    }))

    asserts.status(resp, 400)


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


def base_get_from_cache_test(ctx, req, expected_resp):
    ctx.start_backend(SimpleConfig(response=http.response.ok(data='backend')))
    ctx.start_balancer(CachalotConfig())

    resp = ctx.perform_request(req)

    cacher_req = ctx.cacher.state.get_request()

    asserts.status(resp, expected_resp.response_line.status)
    asserts.content(resp, expected_resp.data.content)
    assert_cache_headers(resp, expected_resp)
    assert_cache_headers(cacher_req, req)
    assert ctx.backend.state.requests.empty()


ERR_CACHER_CONFIGS = [(name, SimpleConfig(response=response)) for name, response in ERR_CACHER_RESPONSES]


def base_get_from_cache_head_test(ctx, req, expected_resp):
    ctx.start_backend(SimpleConfig(response=http.response.ok(data='backend')))
    ctx.start_balancer(CachalotConfig())

    with ctx.create_http_connection() as conn:
        resp = conn.perform_request(req)
        # to be sure that there is no data left after previous response
        conn.perform_request(http.request.get(CHECK_HEAD))

    cacher_req = ctx.cacher.state.get_request()

    asserts.status(resp, expected_resp.response_line.status)
    assert_cache_headers(resp, expected_resp)
    assert_cache_headers(cacher_req, req)
    assert ctx.backend.state.requests.empty()


ERR_CACHER_HEAD_CONFIGS = [(name, SimpleConfig(response=response)) for name, response
                           in build_empty_data_responses(ERR_CACHER_RESPONSES)]


def base_no_put_to_cache_test(ctx, req, backend_resp):
    ctx.start_backend(SimpleConfig(response=http.response.not_found()), name='cacher')
    ctx.start_backend(SimpleConfig(response=backend_resp))
    ctx.start_balancer(CachalotConfig())

    resp = ctx.perform_request(req)

    for run in Multirun():
        with run:
            log = ctx.manager.fs.read_file(ctx.balancer.config.accesslog)
            assert 'cacher' in log

    backend_req = ctx.backend.state.get_request()
    cacher_get_req = ctx.cacher.state.get_request()

    if backend_resp.is_raw_message():
        backend_resp = backend_resp.to_response()
    assert resp == backend_resp

    asserts.method(cacher_get_req, 'GET')
    asserts.path(cacher_get_req, req.request_line.path)
    assert_cache_headers(cacher_get_req, req)

    asserts.path(backend_req, req.request_line.path)
    assert_cache_headers(backend_req, req)

    assert ctx.cacher.state.requests.empty()


@ids_parametrize('req', CACHE_REQUESTS)
@ids_parametrize('backend_resp', NO_CACHE_BACKEND_RESPONSES)
def test_no_put_to_cache_not_cachable_response(ctx, req, backend_resp):
    """
    Balancer must not put not cachable backend response to cacher
    """
    base_no_put_to_cache_test(ctx, req, backend_resp)


@ids_parametrize('req', NO_STORE_REQUESTS)
@ids_parametrize('backend_resp', CACHE_BACKEND_RESPONSES + NO_CACHE_BACKEND_RESPONSES)
def test_no_put_to_cache_no_store_request(ctx, req, backend_resp):
    """
    Balancer must not put response to request with no-store Cache-Control directive to cacher
    """
    base_no_put_to_cache_test(ctx, req, backend_resp)


@ids_parametrize('req', build_head_requests(CACHE_REQUESTS + NO_STORE_REQUESTS))
@ids_parametrize('backend_resp', build_empty_data_responses(CACHE_BACKEND_RESPONSES + NO_CACHE_BACKEND_RESPONSES))
def test_no_put_to_cache_head(ctx, req, backend_resp):
    ctx.start_backend(SimpleConfig(response=http.response.not_found()), name='cacher')
    ctx.start_backend(SimpleConfig(response=backend_resp))
    ctx.start_balancer(CachalotConfig())

    with ctx.create_http_connection() as conn:
        resp = conn.perform_request(req)
        # to be sure that there is no data left after previous response
        conn.perform_request(http.request.get(CHECK_HEAD))

    for run in Multirun():
        with run:
            log = ctx.manager.fs.read_file(ctx.balancer.config.accesslog)
            assert 'cacher' in log

    backend_req = ctx.backend.state.get_request()
    cacher_head_req = ctx.cacher.state.get_request()

    asserts.status(resp, backend_resp.response_line.status)
    assert_cache_headers(resp, backend_resp)

    asserts.method(cacher_head_req, 'HEAD')
    asserts.path(cacher_head_req, req.request_line.path)
    assert_cache_headers(cacher_head_req, req)

    asserts.path(backend_req, req.request_line.path)
    assert_cache_headers(backend_req, req)

    assert ctx.cacher.state.requests.empty()

ERR_CACHER_CONFIGS = [(name, SimpleConfig(response=response)) for name, response in ERR_CACHER_RESPONSES]


@ids_parametrize('req', ONLY_CACHE_REQUESTS)
@ids_parametrize('cacher_config', ERR_CACHER_CONFIGS + INVALID_CACHER_CONFIGS)
def test_only_if_cached_not_in_cache(ctx, req, cacher_config):
    """
    If request contains header Cache-Control: only-if-cached then balancer must not
    send request to backend even if cacher is broken
    """
    ctx.start_backend(cacher_config, name='cacher')
    base_get_from_cache_test(ctx, req, http.response.gateway_timeout())


@ids_parametrize('req', build_head_requests(CACHE_REQUESTS + ONLY_CACHE_REQUESTS))
@ids_parametrize('cacher_resp', build_empty_data_responses(OK_CACHER_RESPONSES))
def test_get_from_cache_head(ctx, req, cacher_resp):
    """
    Balancer must return response from cacher and must not send request to backend
    """
    ctx.start_backend(SimpleConfig(response=cacher_resp), name='cacher')
    base_get_from_cache_head_test(ctx, req, cacher_resp)


ERR_CACHER_HEAD_CONFIGS = [(name, SimpleConfig(response=response)) for name, response
                           in build_empty_data_responses(ERR_CACHER_RESPONSES)]


@ids_parametrize('req', build_head_requests(ONLY_CACHE_REQUESTS))
@ids_parametrize('cacher_config', ERR_CACHER_HEAD_CONFIGS + [
    ('dummy', DummyConfig()),
    ('broken', BrokenConfig()),
])
def test_only_if_cached_not_in_cache_head(ctx, req, cacher_config):
    """
    If request contains header Cache-Control: only-if-cached then balancer must not
    send request to backend even if cacher is broken
    """
    ctx.start_backend(cacher_config, name='cacher')
    base_get_from_cache_test(ctx, req, http.response.gateway_timeout())


@ids_parametrize('req', NO_CACHE_NO_STORE_REQUESTS)
@ids_parametrize('backend_resp', CACHE_BACKEND_RESPONSES + NO_CACHE_BACKEND_RESPONSES)
def test_no_cache_no_store(ctx, req, backend_resp):
    """
    Balancer must not try to get response from cacher or put backend response to cacher for no-cache & no-store request
    """
    ctx.start_backend(SimpleConfig(response=http.response.ok(data=CACHER_DATA)), name='cacher')
    ctx.start_backend(SimpleConfig(response=backend_resp))
    ctx.start_balancer(CachalotConfig())

    resp = ctx.perform_request(req)

    for run in Multirun():
        with run:
            log = ctx.manager.fs.read_file(ctx.balancer.config.accesslog)
            assert 'cachalot' in log and 'cacher' not in log

    backend_req = ctx.backend.state.get_request()

    asserts.status(resp, backend_resp.response_line.status)
    assert_cache_headers(resp, backend_resp)
    asserts.content(resp, backend_resp.data.content)

    asserts.path(backend_req, req.request_line.path)
    asserts.content(backend_req, req.data.content)

    assert ctx.cacher.state.requests.empty()


@ids_parametrize('req', build_head_requests(NO_CACHE_NO_STORE_REQUESTS + NO_CACHE_REQUESTS))
@ids_parametrize('backend_resp', build_empty_data_responses(CACHE_BACKEND_RESPONSES + NO_CACHE_BACKEND_RESPONSES))
def test_no_cache_no_store_head(ctx, req, backend_resp):
    """
    Balancer must not try to get response from cacher or put backend response to cacher for no-cache & no-store request
    """
    ctx.start_backend(SimpleConfig(response=http.response.ok(data=CACHER_DATA)), name='cacher')
    ctx.start_backend(SimpleConfig(response=backend_resp))
    ctx.start_balancer(CachalotConfig())

    with ctx.create_http_connection() as conn:
        resp = conn.perform_request(req)
        # to be sure that there is no data left after previous response
        conn.perform_request(http.request.get(CHECK_HEAD))

    for run in Multirun():
        with run:
            log = ctx.manager.fs.read_file(ctx.balancer.config.accesslog)
            assert 'cachalot' in log and 'cacher' not in log

    backend_req = ctx.backend.state.get_request()

    asserts.status(resp, backend_resp.response_line.status)
    assert_cache_headers(resp, backend_resp)

    asserts.method(backend_req, 'HEAD')
    asserts.path(backend_req, req.request_line.path)

    assert ctx.cacher.state.requests.empty()


@ids_parametrize('req', NO_CACHE_REQUESTS)
@ids_parametrize('backend_resp', CACHE_BACKEND_RESPONSES)
def test_no_cache(ctx, req, backend_resp):
    """
    Balancer must not try to get response from cacher but must put backend response to cacher
    """
    ctx.start_backend(SimpleConfig(response=http.response.ok(data=CACHER_DATA)), name='cacher')
    ctx.start_backend(SimpleConfig(response=backend_resp))
    ctx.start_balancer(CachalotConfig())

    resp = ctx.perform_request(req)

    for run in Multirun():
        with run:
            log = ctx.manager.fs.read_file(ctx.balancer.config.accesslog)
            assert 'cachalot' in log

    backend_req = ctx.backend.state.get_request()
    cacher_put_req = ctx.cacher.state.get_request()

    asserts.content(resp, backend_resp.data.content)

    asserts.path(backend_req, req.request_line.path)
    asserts.content(backend_req, req.data.content)

    asserts.method(cacher_put_req, 'PUT')
    asserts.path(cacher_put_req, req.request_line.path)
    asserts.content(cacher_put_req, backend_resp.data.content)
    assert_cache_headers(cacher_put_req, backend_resp)
