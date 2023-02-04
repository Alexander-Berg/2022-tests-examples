# -*- coding: utf-8 -*-
import os
import pytest
import time
import random

from configs import DynamicSimpleConfig, DynamicHashingConfig, DynamicWithActiveConfig, DynamicHashingWithActiveConfig, DynamicOverNonProxyModule

from balancer.test.util.balancer import asserts
from balancer.test.util.predef.handler.server.http import SimpleConfig, StaticResponseHandler, StaticResponseConfig, SimpleHandler
from balancer.test.util.predef import http
from balancer.test.util.process import BalancerStartError


class ActiveCheckHandler(StaticResponseHandler):
    def handle_parsed_request(self, raw_request, stream):
        stream.write_response(self.config.response)
        self.finish_response()


class ActiveCheckConfig(StaticResponseConfig):
    HANDLER_TYPE = SimpleHandler

    def __init__(self, weight=10):
        response = http.response.ok(headers={
            'RS-Weight': str(weight)
        })
        super(ActiveCheckConfig, self).__init__(response)


def test_sanity(ctx):
    ctx.start_backend(SimpleConfig(), name='backend0')
    ctx.start_backend(SimpleConfig(), name='backend1')
    ctx.start_balancer(DynamicSimpleConfig())

    for _ in range(500):
        ctx.perform_request(http.request.get('/'))

    assert ctx.backend0.state.accepted.value + ctx.backend1.state.accepted.value == 500


@pytest.mark.parametrize(['param', 'value'], [
    ('max_pessimized_share', 1.1),
    ('max_pessimized_share', -0.2),
    ('weight_increase_step', 0),
    ('weight_increase_step', 1.1),
])
def test_invalid_params(ctx, param, value):
    ctx.start_backend(SimpleConfig(), name='backend0')
    ctx.start_backend(SimpleConfig(), name='backend1')

    params = {
        param: value
    }
    with pytest.raises(BalancerStartError):
        ctx.start_balancer(DynamicSimpleConfig(**params))


@pytest.mark.parametrize(['param', 'value'], [
    ('request', None),
    ('delay', None),
])
def test_invalid_active_params(ctx, param, value):
    ctx.start_backend(SimpleConfig(), name='backend0')
    ctx.start_backend(SimpleConfig(), name='backend1')
    ctx.start_backend(SimpleConfig())

    params = {
        param: value
    }
    with pytest.raises(BalancerStartError):
        ctx.start_balancer(DynamicWithActiveConfig(**params))


# TODO: Redesign greater_than_1 test. It's flaky, because it depends on several random timings
# (backend0's weight increases from 1 up to 10 while requests are performing)
@pytest.mark.parametrize(['config'], [(DynamicHashingWithActiveConfig,), (DynamicWithActiveConfig,)])
@pytest.mark.parametrize(['weights', 'real_weights'], [
    ((100, 20), (100.0 / 120, 20.0 / 120)),
    ((1000, 100), (0.9, 0.1)),
], ids=['mps_0.5', 'greater_than_1'])
def test_use_backend_weight(ctx, weights, real_weights, config):
    backend0_weight, backend1_weight = weights

    ctx.start_backend(ActiveCheckConfig(weight=backend0_weight), name='backend0')
    ctx.start_backend(ActiveCheckConfig(weight=backend1_weight), name='backend1')
    ctx.start_balancer(config(
        max_pessimized_share=0.5,
        use_backend_weight=True,
        weight_normalization_coeff=100,
        weight_increase_step=1,
        request='GET /check HTTP/1.1\r\n\r\n',
        delay='1s',
        disable_defaults=True
    ))

    # wait for active check
    while ctx.backend0.state.accepted.value == 0 or ctx.backend1.state.accepted.value == 0:
        time.sleep(0.5)

    # wait for balancing state update
    time.sleep(3)

    n_requests = 1000
    for i in range(n_requests):
        ctx.perform_request(http.request.get('/?text=' + str(i)))

    backend0_real_weight, backend1_real_weight = real_weights

    assert abs(ctx.backend0.state.accepted.value / (1.0 * n_requests) - backend0_real_weight) < 0.1
    assert abs(ctx.backend1.state.accepted.value / (1.0 * n_requests) - backend1_real_weight) < 0.1


def test_active_tcp_check_and_http_check(ctx):
    ctx.start_fake_backend(name='backend0')
    ctx.start_fake_backend(name='backend1')
    with pytest.raises(BalancerStartError):
        ctx.start_balancer(DynamicWithActiveConfig(
            request='GET / HTTP/1.1\r\n\r\n', tcp_check=True
        ))


def test_active_tcp_check(ctx):
    ctx.start_backend(SimpleConfig(), name='backend0')
    ctx.start_fake_backend(name='backend1')
    ctx.start_balancer(DynamicWithActiveConfig(workers=3, request=None, tcp_check=True, max_pessimized_share=0))

    time.sleep(2)

    unistat = ctx.get_unistat(port=ctx.balancer.config.stats_port)
    assert unistat['failed_pings_summ'] > 0
    assert unistat['connection_other_error_pings_summ'] > 0
    assert unistat['dynamic-over_pessimization_ammv'] > 0
    asserts.status(ctx.perform_request(http.request.get()), 200)


@pytest.mark.parametrize(["config", "params"], [
    (DynamicSimpleConfig, {}),
    (DynamicHashingConfig, {"backend_count": 2})
])
def test_backends_blacklist(ctx, config, params):
    blacklist_file = ctx.manager.fs.create_file('blacklist')
    ctx.start_backend(SimpleConfig(), name='backend0')
    ctx.start_backend(SimpleConfig(), name='backend1')

    params = {
        "backends_blacklist": blacklist_file,
    }
    params.update(params)
    cfg = config(**params)
    if config is DynamicHashingConfig:
        cfg.add_backend('backend0')
        cfg.add_backend('backend1')
    ctx.start_balancer(cfg)

    # disable first backend
    ctx.manager.fs.rewrite(blacklist_file, 'backend0\n')
    time.sleep(5)
    for _ in range(100):
        ctx.perform_request(http.request.get('/'))
    assert ctx.backend0.state.accepted.value == 0

    # disable first backend without extra \n
    ctx.manager.fs.rewrite(blacklist_file, 'backend0')
    time.sleep(5)
    for _ in range(100):
        ctx.perform_request(http.request.get('/'))
    assert ctx.backend0.state.accepted.value == 0

    # disable second backend
    backend2_accepted = ctx.backend1.state.accepted.value
    ctx.manager.fs.rewrite(blacklist_file, 'backend1')
    time.sleep(5)
    for _ in range(100):
        ctx.perform_request(http.request.get('/'))
    assert backend2_accepted == ctx.backend1.state.accepted.value

    # remove blacklist file
    backend1_accepted = ctx.backend0.state.accepted.value
    backend2_accepted = ctx.backend1.state.accepted.value
    os.unlink(blacklist_file)
    time.sleep(5)
    for _ in range(100):
        ctx.perform_request(http.request.get('/'))
    assert ctx.backend0.state.accepted.value > backend1_accepted
    assert ctx.backend1.state.accepted.value > backend2_accepted


def test_dynamic_over_non_proxy_modules(ctx):
    with pytest.raises(BalancerStartError):
        ctx.start_balancer(DynamicOverNonProxyModule())


def test_skip_keepalive_in_ping(ctx):
    """
    BALANCER-2874 ping requests should not use keepalive connections
    """
    backend = ctx.start_backend(SimpleConfig(), name='backend1')
    ctx.start_backend(SimpleConfig(), name='backend2')
    ctx.start_balancer(DynamicWithActiveConfig(delay=0.1))

    tcpdump = ctx.manager.tcpdump.start(backend.server_config.port)
    time.sleep(2)
    tcpdump.read_all()
    closed_sessions = tcpdump.get_closed_sessions()
    sessions = tcpdump.get_sessions()
    assert len(sessions) > 10
    assert len(closed_sessions) > 10


def test_dynamic_hashing_same_request_same_backend(ctx):
    config = DynamicHashingConfig(backend_count=50)
    for i in range(50):
        name = "backend" + str(i)
        ctx.start_backend(SimpleConfig(http.response.ok(data=name)), name=name)
        config.add_backend(name)

    ctx.start_balancer(config)

    content = None
    for _ in range(1000):
        resp = ctx.perform_request(http.request.get('/?text=123'))
        if content is None:
            content = resp.data.content
        else:
            assert content == resp.data.content


def run_requests(ctx, n_backends, n_requests, param=None, max_skew=None):
    random.seed(18)

    config = DynamicHashingConfig(backend_count=n_backends, max_skew=max_skew)
    backends = []
    dist = []

    for i in range(n_backends):
        name = "backend" + str(i)
        backends.append(ctx.start_backend(SimpleConfig(http.response.ok(data=name)), name=name))
        config.add_backend(name)
        dist.append(0)

    ctx.start_balancer(config)

    req_to_backend = []
    for i in range(n_requests):
        resp = ctx.perform_request(http.request.get('/?text=' + param if param else str(random.uniform(1, n_requests))))
        assert resp.data.content.startswith("backend")
        dist[int(resp.data.content.strip()[7:])] += 1
        req_to_backend.append(resp.data.content)

    return req_to_backend, dist, backends


@pytest.mark.parametrize(["backends_a", "backends_b"], [
    (50, 100),
    (100, 50),
    (100, 10),
    (10, 100),
    (100, 90),
    (90, 100),
])
def test_dynamic_hashing_consistency(ctx, backends_a, backends_b):
    n_requests = 1000
    req_to_backend_a, _, _ = run_requests(ctx, backends_a, n_requests)
    req_to_backend_b, _, _ = run_requests(ctx, backends_b, n_requests)

    changed = 0
    for a, b in zip(req_to_backend_a, req_to_backend_b):
        if a != b:
            changed += 1

    max_relocated_share = 1 - min(backends_a, backends_b) / max(backends_a, backends_b)
    assert changed <= max_relocated_share * n_requests


@pytest.mark.parametrize("n_backends", [10, 25, 50])
def test_dynamic_hashing_distribution(ctx, n_backends):
    n_requests = 1000
    _, dist, _ = run_requests(ctx, n_backends, n_requests)
    perfect = n_requests / n_backends

    for x in dist:
        assert abs(perfect - x) / perfect <= 0.1


@pytest.mark.parametrize("n_backends", [3, 5, 10])
@pytest.mark.parametrize("max_skew", [0.1, 0.2, 0.5, 1.0])
def test_dynamic_hashing_max_skew(ctx, n_backends, max_skew):
    n_requests = 10000
    _, dist, _ = run_requests(ctx, n_backends, n_requests, param="123", max_skew=max_skew)
    perfect = n_requests / n_backends
    limit = (1.0 + max_skew) * perfect
    consumers = n_requests / limit
    dev = 0.1 * limit

    used = 0
    m = 0
    for x in dist:
        if x > dev:
            used += 1
            if m < x:
                m = x
    assert abs(limit - m) <= dev
    assert consumers + 1 >= used


def test_backends_groups_by_hash(ctx):
    config = DynamicHashingWithActiveConfig(
        request='GET /check HTTP/1.1\r\n\r\n',
        delay='1s',
        disable_defaults=True,
        use_backends_grouping=True,
        backend_count=4
    )
    group='g1'
    name='backend0'
    ctx.start_backend(SimpleConfig(http.response.ok(data=name, headers={'RS-Group': group})), name=name)
    config.add_backend(name)
    name='backend1'
    ctx.start_backend(SimpleConfig(http.response.ok(data=name, headers={'RS-Group': group})), name=name)
    config.add_backend(name)
    group='g2'
    name='backend2'
    ctx.start_backend(SimpleConfig(http.response.ok(data=name, headers={'RS-Group': group})), name=name)
    config.add_backend(name)
    name='backend3'
    ctx.start_backend(SimpleConfig(http.response.ok(data=name, headers={'RS-Group': group})), name=name)
    config.add_backend(name)

    ctx.start_balancer(config)

    # wait for active check
    while ctx.backend0.state.accepted.value == 0 or ctx.backend1.state.accepted.value == 0 or \
    ctx.backend2.state.accepted.value == 0 or ctx.backend3.state.accepted.value == 0:
        time.sleep(0.5)

    # wait for balancing state update
    time.sleep(3)

    groups={'g1': 0, 'g2': 0}
    backends={'backend0': 0, 'backend1': 0, 'backend2': 0, 'backend3': 0}
    count=1000
    expected = count / 2
    for _ in range(count):
        resp = ctx.perform_request(http.request.get('/?text=123'))
        groups[resp.headers.get_one('RS-Group')] += 1
        backends[resp.data.content] += 1
    if groups['g1'] > 0:
        assert groups['g1'] == count
        assert groups['g2'] == 0
        assert abs(expected - backends['backend0']) / expected <= 0.1
        assert abs(expected - backends['backend1']) / expected <= 0.1
        assert backends['backend0'] + backends['backend1'] == count
        assert backends['backend2'] == 0
        assert backends['backend3'] == 0
    else:
        assert groups['g2'] == count
        assert groups['g1'] == 0
        assert abs(expected - backends['backend2']) / expected <= 0.1
        assert abs(expected - backends['backend3']) / expected <= 0.1
        assert backends['backend2'] + backends['backend3'] == count
        assert backends['backend0'] == 0
        assert backends['backend1'] == 0


@pytest.mark.parametrize("matched_group", [True, False])
def test_backends_groups_by_group_name(ctx, matched_group):
    config = DynamicHashingWithActiveConfig(
        request='GET /check HTTP/1.1\r\n\r\n',
        delay='1s',
        disable_defaults=True,
        use_backends_grouping=True,
        backend_count=4
    )
    group='g1'
    name='backend0'
    ctx.start_backend(SimpleConfig(http.response.ok(data=name, headers={'RS-Group': group})), name=name)
    config.add_backend(name)
    name='backend1'
    ctx.start_backend(SimpleConfig(http.response.ok(data=name, headers={'RS-Group': group})), name=name)
    config.add_backend(name)
    group='g2'
    name='backend2'
    ctx.start_backend(SimpleConfig(http.response.ok(data=name, headers={'RS-Group': group})), name=name)
    config.add_backend(name)
    name='backend3'
    ctx.start_backend(SimpleConfig(http.response.ok(data=name, headers={'RS-Group': group})), name=name)
    config.add_backend(name)

    ctx.start_balancer(config)

    # wait for active check
    while ctx.backend0.state.accepted.value == 0 or ctx.backend1.state.accepted.value == 0 or \
    ctx.backend2.state.accepted.value == 0 or ctx.backend3.state.accepted.value == 0:
        time.sleep(0.5)

    # wait for balancing state update
    time.sleep(3)

    resp = ctx.perform_request(http.request.get('/?text=123'))
    # select group, so that by hash routing routes to another group
    # to assure that we are routed by group name, not by hash
    if resp.headers.get_one('RS-Group') == 'g1':
        group = 'g2'
    else:
        group = 'g1'
    request = http.request.get('/?text=123', headers={'X-Group': (group if matched_group else 'g3')})

    groups={'g1': 0, 'g2': 0}
    backends={'backend0': 0, 'backend1': 0, 'backend2': 0, 'backend3': 0}
    count=1000
    expected = count / 2
    for _ in range(count):
        resp = ctx.perform_request(request)
        groups[resp.headers.get_one('RS-Group')] += 1
        backends[resp.data.content] += 1

    assert groups[group] == (count if matched_group else 0)
    if groups['g1'] > 0:
        assert groups['g1'] == count
        assert groups['g2'] == 0
        assert abs(expected - backends['backend0']) / expected <= 0.1
        assert abs(expected - backends['backend1']) / expected <= 0.1
        assert backends['backend0'] + backends['backend1'] == count
        assert backends['backend2'] == 0
        assert backends['backend3'] == 0
    else:
        assert groups['g2'] == count
        assert groups['g1'] == 0
        assert abs(expected - backends['backend2']) / expected <= 0.1
        assert abs(expected - backends['backend3']) / expected <= 0.1
        assert backends['backend2'] + backends['backend3'] == count
        assert backends['backend0'] == 0
        assert backends['backend1'] == 0
