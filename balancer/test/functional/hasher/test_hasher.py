# -*- coding: utf-8 -*-
import time
import itertools

from configs import HasherConfig

from balancer.test.util import asserts

from balancer.test.util.predef.handler.server.http import SimpleConfig

from balancer.test.util.predef import http


RESPONSES = ['1', '2', '3']
TEST_PATH = '/test.html'


def build_test_request(ctx):
    return ctx.build_uri_request('test.html')


def start_all(ctx, **balancer_kwargs):
    ctx.start_backend(SimpleConfig(response=http.response.ok(data=RESPONSES[0])), name='backend1')
    ctx.start_backend(SimpleConfig(response=http.response.ok(data=RESPONSES[1])), name='backend2')
    ctx.start_backend(SimpleConfig(response=http.response.ok(data=RESPONSES[2])), name='backend3')
    ctx.start_balancer(HasherConfig(**balancer_kwargs))


def base_test_simple(ctx, mode):
    start_all(ctx, mode=mode)
    response = ctx.perform_request(http.request.get())
    asserts.status(response, 200)


def base_test(ctx, requests, **balancer_kwargs):
    start_all(ctx, **balancer_kwargs)

    def do_requests(backend_id):
        for req in requests[backend_id]:
            response = ctx.perform_request(req)
            asserts.status(response, 200)
            asserts.content(response, RESPONSES[backend_id])

    for backend_id in [0, 1, 2]:
        do_requests(backend_id)


def build_uri_requests(uri_list):
    return [http.request.get(path=uri) for uri in uri_list]


def build_ip_requests(uri_list, ip_prefix, ip_postfixes):
    def build(ip_postfix):
        return [http.request.get(path=uri, headers={'X-Forwarded-For': ip_prefix + ip_postfix}) for uri in uri_list]

    return list(itertools.chain(*[build(postfix) for postfix in ip_postfixes]))


def test_barnavig(ctx):
    """
    Запросы, имеющие один и тот же параметр ui, идут на один и тот же backend
    """
    req_1 = build_uri_requests(['/?ui=4&param=value', '/?x=y&ui=4'])
    req_2 = build_uri_requests(['/?yandsearch=text&ui=3&a=b', '/?ui=3', '/?abc=xyz&ui=3'])
    req_3 = build_uri_requests(['/?text=123&ui=2', '/?ui=2&1=iu'])

    base_test(ctx, [req_1, req_2, req_3], mode='barnavig')


def test_request(ctx):
    """
    Запросы с одинаковыми uri идут на один и тот же backend
    """
    def build_requests(uri):
        return [
            http.request.get(path=uri),
            http.request.get(path=uri, headers={
                'Y-abc-987-a-x-2-ZZZ': '7',
                'X-ab0-987-b-x-2-ZZZ': '6',
                'X-abc-98a-a-x-2-ZZZ': '5',
                'X-abc-987-c-x-2-ZZZ': '4',
                'X-abc-987-b-z-2-ZZZ': '3',
                'X-abc-987-a-x-4-ZZZ': '2',
                'X-abc-987-b-x-2-ZZ': '1',
                'X-abc-987-b-x-2-ZZZ': '0',
            }),
            http.request.get(path=uri, headers={
                'If-Modified-Since': 'Mon, 14 Oct 2013 11:00:00 GMT',
            }),
        ]

    req_1 = build_requests('/9')
    req_2 = build_requests('/3')
    req_3 = build_requests('/7')

    base_test(ctx, [req_1, req_2, req_3], mode='request')


def test_request_cgi(ctx):
    """
    SEPE-7600
    Запросы с одинаковыми path но разными cgi идут в разные backend-ы
    """
    req_1 = http.request.get(path='/yandsearch?text=2')
    req_2 = http.request.get(path='/yandsearch')
    req_3 = http.request.get(path='/yandsearch?text=1')

    base_test(ctx, [[req_1], [req_2], [req_3]], mode='request')


def test_combine_hashes(ctx):
    """
    BALANCER-3068
    """
    ctx.start_backend(SimpleConfig(response=http.response.ok(data=RESPONSES[0])), name='backend1')
    ctx.start_backend(SimpleConfig(response=http.response.ok(data=RESPONSES[1])), name='backend2')
    ctx.start_backend(SimpleConfig(response=http.response.ok(data=RESPONSES[2])), name='backend3')
    ctx.start_balancer(HasherConfig(mode='request', combine_hashes=True, header_name_prev='A'))

    resp = ctx.perform_request(http.request.get(path='/yandsearch?text=1'))
    asserts.status(resp, 200)
    asserts.content(resp, RESPONSES[2])

    resp = ctx.perform_request(http.request.get(path='/yandsearch?text=1', headers={'A': 'XXX'}))
    asserts.status(resp, 200)
    asserts.content(resp, RESPONSES[0])


def test_subnet_default_v4_mask(ctx):
    """
    SEPE-6929
    Запросы, пришедшие с IP-адресов, находящихся в одной подсети, идут на один и тот же backend
    Проверяется маска по-умолчанию -- /16
    """
    prefix_1 = '178.158.'
    prefix_2 = '178.154.'
    prefix_3 = '178.153.'
    postfixes = ['239.126', '113.119', '218.146', '213.107', '254.37',
                 '75.9', '11.131', '142.144', '93.187', '80.103']

    req_1 = build_ip_requests(['hello', 'world'], prefix_1, postfixes)
    req_2 = build_ip_requests(['echo', 'cat'], prefix_2, postfixes)
    req_3 = build_ip_requests(['test', 'text'], prefix_3, postfixes)

    base_test(ctx, [req_1, req_2, req_3], mode='subnet')


def test_subnet_v4_mask(ctx):
    """
    SEPE-6929
    Запросы, пришедшие с IP-адресов, находящихся в одной подсети, идут на один и тот же backend
    Проверяется маска, указанная в конфиге
    """
    prefix_1 = '178.158.202.'
    prefix_2 = '178.154.200.'
    prefix_3 = '178.158.200.'
    postfixes = ['148', '190', '172', '186', '190', '223', '97', '68', '12', '159']

    req_1 = build_ip_requests(['hello', 'world'], prefix_1, postfixes)
    req_2 = build_ip_requests(['echo', 'cat'], prefix_2, postfixes)
    req_3 = build_ip_requests(['test', 'text'], prefix_3, postfixes)

    base_test(ctx, [req_1, req_2, req_3], mode='subnet', subnet_v4_mask=24)


def test_subnet_default_v6_mask(ctx):
    """
    SEPE-6929
    Запросы, пришедшие с одного и того же IP-адреса, идут на один и тот же backend
    Проверяется IPv6 адрес и маска по-умолчанию -- /64
    """
    prefix_1 = '1:2:3:7:'
    prefix_2 = '1:2:3:8:'
    prefix_3 = '1:2:3:4:'
    postfixes = ['f000:431d:4eb5:212c', 'd33c:773d:ea77:a35', 'a2e6:d7c:d4ac:990f', '4d5b:5ceb:96af:bda3',
                 'd4bc:99a4:c569:af0f', 'c3d9:e2b8:5657:3614', '2d89:e349:f8d1:90a5', '1f11:42c0:2249:62c1',
                 '5c94:2c37:eae6:1597', 'e840:278c:59bd:cef']

    req_1 = build_ip_requests(['hello', 'world'], prefix_1, postfixes)
    req_2 = build_ip_requests(['echo', 'cat'], prefix_2, postfixes)
    req_3 = build_ip_requests(['test', 'text'], prefix_3, postfixes)

    base_test(ctx, [req_1, req_2, req_3], mode='subnet')


def test_subnet_v6_mask(ctx):
    """
    SEPE-6929
    Запросы, пришедшие с IP-адресов, находящихся в одной подсети, идут на один и тот же backend
    Проверяется IPv6 адрес и маска, указанная в конфиге
    """
    prefix_1 = '1:2:3:7:0:'
    prefix_2 = '1:2:3:8:0:'
    prefix_3 = '1:2:3:7:4:'
    postfixes = ['9cb8:4d3f:1e08', '7f5f:b1f5:d4f1', '5025:5c6b:2a12', '2090:1443:e80b', 'c04a:22fc:8ebc',
                 'bf:20c:3fb0', 'e0f0:1dc3:4e48', '44b7:d7c0:b6fa', '2e26:bcc4:2f6', '168c:8cab:d3cf']

    req_1 = build_ip_requests(['hello', 'world'], prefix_1, postfixes)
    req_2 = build_ip_requests(['echo', 'cat'], prefix_2, postfixes)
    req_3 = build_ip_requests(['test', 'text'], prefix_3, postfixes)

    base_test(ctx, [req_1, req_2, req_3], mode='subnet', subnet_v6_mask=80)


def test_barnavig_without_ui(ctx):
    """
    Запросы, не содержащие параметра ui, идут на любой backend
    """
    base_test_simple(ctx, mode='barnavig')


def test_subnet_without_ip(ctx):
    """
    Запросы, в которых не указан IP-адрес клиента, идут на любой backend
    """
    base_test_simple(ctx, mode='subnet')


def get_reqs(backend):
    result = list()
    for _ in range(backend.state.requests.qsize()):
        req = backend.state.get_request()
        result.append(req)
    return result


def test_active_request(ctx):
    """
    Для проверки состояния backend-ов на них задается тестовый запрос, указанный в конфиге
    """
    ctx.start_backend(SimpleConfig(), name='backend1')
    ctx.start_backend(SimpleConfig(), name='backend2')
    ctx.start_backend(SimpleConfig(), name='backend3')
    ctx.start_balancer(HasherConfig(request=http.request.get(path=TEST_PATH), delay=0.7))

    ctx.perform_request(http.request.get())
    time.sleep(1)

    reqs_1 = get_reqs(ctx.backend1)
    reqs_2 = get_reqs(ctx.backend2)
    reqs_3 = get_reqs(ctx.backend3)

    assert any(map(lambda x: x.request_line.path == TEST_PATH, reqs_1))
    assert any(map(lambda x: x.request_line.path == TEST_PATH, reqs_2))
    assert any(map(lambda x: x.request_line.path == TEST_PATH, reqs_3))


def test_active_broken_backend(ctx):
    ctx.start_backend(SimpleConfig(), name='backend1')
    ctx.start_backend(SimpleConfig(), name='backend2')
    ctx.start_backend(SimpleConfig(), name='backend3')
    ctx.start_balancer(HasherConfig(request=http.request.get(path=TEST_PATH), delay=0.7, attempts=2))

    for i in xrange(10):
        ctx.perform_request(http.request.get())

    reqs_1 = get_reqs(ctx.backend1)
    reqs_2 = get_reqs(ctx.backend2)
    reqs_3 = get_reqs(ctx.backend3)
    active_backend = max(len(reqs_1), len(reqs_2), len(reqs_3))

    stoped_backend = 3

    if active_backend == len(reqs_1):
        ctx.backend1.stop()
        stoped_backend = 1
    elif active_backend == len(reqs_2):
        ctx.backend2.stop()
        stoped_backend = 2
    else:
        ctx.backend3.stop()

    time.sleep(1)

    for i in xrange(10):
        ctx.perform_request(http.request.get())

    stoped_backend = get_reqs(getattr(ctx, 'backend' + str(stoped_backend)))

    assert ctx.backend1.state.requests.qsize() > 9 or ctx.backend2.state.requests.qsize() > 9 or ctx.backend3.state.requests.qsize() > 9
    assert all(map(lambda x: x.request_line.path == TEST_PATH, stoped_backend))


def test_steady_active(ctx):
    """
    BALANCER-1287: Test requests must come repeatedly when steady option equals True even without client requests
    """
    ctx.start_backend(SimpleConfig(), name='backend1')
    ctx.start_backend(SimpleConfig(), name='backend2')
    ctx.start_backend(SimpleConfig(), name='backend3')
    ctx.start_balancer(HasherConfig(request=http.request.get(path=TEST_PATH), steady=True, delay=0.7))

    time.sleep(5)

    for backend in [ctx.backend1, ctx.backend2, ctx.backend3]:
        assert backend.state.requests.qsize() >= 2
        asserts.path(backend.state.get_request(), TEST_PATH)
