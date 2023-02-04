import time
import threading
import configs

from balancer.test.util.predef import http
from balancer.test.util.predef.handler.server.http import SimpleConfig


def send_async_requests(ctx, request, i):
    def run_request():
        try:
            ctx.perform_request(request)
        except:
            pass

    threads = []

    x = 0
    for x in range(i):
        threads.append(threading.Thread(target=run_request))

    for t in threads:
        t.start()

    for t in threads:
        t.join()


def send_async_multiple_requests(ctx, request1, request2, i):
    def run_request(req):
        try:
            ctx.perform_request(req)
        except:
            pass

    threads = []

    x = 0
    for x in range(i):
        threads.append(threading.Thread(target=run_request, args={request1}))
        threads.append(threading.Thread(target=run_request, args={request2}))

    for t in threads:
        t.start()

    for t in threads:
        t.join()


def should_cache_test(ctx, request, response, ignore_cc=False):
    ctx.start_backend(SimpleConfig(response), name='backend1')
    ctx.start_balancer(configs.CacheConfig(ignore_cache_control=ignore_cc))
    time.sleep(1)

    send_async_requests(ctx, request, 10)
    time.sleep(1)
    send_async_requests(ctx, request, 10)

    time.sleep(1)
    unistat = ctx.get_unistat()
    assert unistat['cache2-cache_hit_summ'] <= 15


def should_not_cache_test(ctx, request, response):
    ctx.start_backend(SimpleConfig(response), name='backend1')
    ctx.start_balancer(configs.CacheConfig())
    time.sleep(1)

    send_async_requests(ctx, request, 10)
    time.sleep(1)
    send_async_requests(ctx, request, 10)

    time.sleep(1)
    unistat = ctx.get_unistat()
    assert unistat['cache2-cache_hit_summ'] == 0


def should_not_cache_differently_requests_test(ctx, request1, request2,
                                               response):
    ctx.start_backend(SimpleConfig(response), name='backend1')
    ctx.start_balancer(configs.CacheConfig())
    time.sleep(1)

    send_async_requests(ctx, request1, 10)
    time.sleep(1)
    send_async_multiple_requests(ctx, request1, request2, 10)

    time.sleep(1)
    unistat = ctx.get_unistat()
    assert unistat['cache2-cache_hit_summ'] == 10


def should_cache_differently_requests_test(ctx, request1, request2, response):
    ctx.start_backend(SimpleConfig(response), name='backend1')
    ctx.start_balancer(configs.CacheConfig())
    time.sleep(1)

    send_async_requests(ctx, request1, 10)
    time.sleep(2)
    send_async_multiple_requests(ctx, request1, request2, 5)

    time.sleep(1)
    unistat = ctx.get_unistat()
    assert unistat['cache2-cache_hit_summ'] <= 15


def test_should_cache(ctx):
    should_cache_test(
        ctx, http.request.get(path='/123'),
        http.response.ok(data='backend1',
                         headers={'Cache-Control': 'max-age=100'}))
    should_cache_test(
        ctx, http.request.get(path='/123'),
        http.response.ok(data='backend1',
                         headers={'Expires': 'Wed, 21 Oct 2025 07:28:00 GMT'}))
    should_cache_test(
        ctx, http.request.get(path='/123'),
        http.response.ok(data='backend1',
                         headers={
                             'Expires': 'Wed, 21 Oct 2025 07:28:00 GMT',
                             'Vary': 'Accept-Encoding'
                         }))


def test_should_cache_ignore_cache_control(ctx):
    should_cache_test(ctx,
                      http.request.get(path='/123'),
                      http.response.ok(data='backend1',
                                       headers={'Cache-Control': 'no-cache'}),
                      ignore_cc=True)
    should_cache_test(ctx,
                      http.request.get(path='/123'),
                      http.response.ok(data='backend1',
                                       headers={'Cache-Control': 'no-store'}),
                      ignore_cc=True)


def test_should_not_cache(ctx):
    should_not_cache_test(ctx, http.request.get(path='/123'),
                          http.response.ok(data='backend1'))
    should_not_cache_test(
        ctx, http.request.get(path='/123'),
        http.response.ok(data='backend1',
                         headers={
                             'Expires': 'Wed, 21 Oct 2025 07:28:00 GMT',
                             'Vary': 'Any,Accept-Encoding'
                         }))
    should_not_cache_test(
        ctx, http.request.get(path='/123'),
        http.response.ok(data='backend1',
                         headers={
                             'Cache-Control': 'max-age=100',
                             'Vary': 'Any, Accept-Encoding'
                         }))
    should_not_cache_test(
        ctx, http.request.get(path='/123'),
        http.response.ok(data='backend1',
                         headers={
                             'Expires': 'Wed, 21 Oct 2025 07:28:00 GMT',
                             'Vary': 'Any'
                         }))
    should_not_cache_test(
        ctx, http.request.get(path='/123'),
        http.response.ok(data='backend1',
                         headers={'Cache-Control': 'no-store'}))

    should_not_cache_test(
        ctx,
        http.request.get(path='/123', headers={'Cache-Control': 'no-cache'}),
        http.response.ok(data='backend1'))


def test_should_cache_differently(ctx):
    should_cache_differently_requests_test(
        ctx, http.request.get(path='/333'), http.request.get(path='/1234'),
        http.response.ok(data='backend1',
                         headers={'Cache-Control': 'max-age=100'}))
    should_cache_differently_requests_test(
        ctx, http.request.get(path='/333'),
        http.request.get(path='/333?cgi=123'),
        http.response.ok(data='backend1',
                         headers={'Cache-Control': 'max-age=100'}))
    should_cache_differently_requests_test(
        ctx, http.request.get(path='/333', headers={'Accept-Encoding':
                                                    'gzip'}),
        http.request.get(path='/333', headers={'Accept-Encoding': 'br'}),
        http.response.ok(data='backend1',
                         headers={'Cache-Control': 'max-age=100'}))


def test_cleanup_after_ttl(ctx):
    ctx.start_backend(SimpleConfig(
        http.response.ok(data='backend1',
                         headers={'Cache-Control': 'max-age=100'})),
                      name='backend1')
    ctx.start_balancer(configs.CacheConfig(cache_ttl='2s'))

    time.sleep(1)
    send_async_requests(ctx, http.request.get(path='/333'), 1)
    time.sleep(5)
    send_async_requests(ctx, http.request.get(path='/333'), 1)

    time.sleep(1)
    unistat = ctx.get_unistat()
    assert unistat['cache2-cache_hit_summ'] == 0
