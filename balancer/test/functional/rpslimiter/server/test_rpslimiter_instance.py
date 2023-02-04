# coding=utf-8
import time

from configs import RpslimiterSimpleConfig

from balancer.test.util import asserts
from balancer.test.util.predef import http
from balancer.test.util.predef.handler.server.http import SimpleConfig
from balancer.test.util.rpslimiter import PeerQuotas, PeerQuota, parse_peer_quotas, render_peer_quotas

import Queue
import pytest


def _guaranteed_sleep(dt):
    # TODO(velavokr): not monotonic. But neither balancer is. BALANCER-2380
    t0 = time.time()
    while True:
        t1 = time.time()
        if t1 - t0 >= dt:
            return
        time.sleep(dt - (t1 - t0))


@pytest.mark.parametrize(
    ['print_state', 'log_quota'],
    [[0, 0], [0, 1], [1, 0], [1, 1]],
    ids=["none", "quota", "state", "state_quota"]
)
def test_rpslimiter_api(ctx, print_state, log_quota):
    common_headers = ''

    if print_state:
        common_headers += 'x-yandex-rpslimiter-print-state: 1\r\n'
    if log_quota:
        common_headers += 'x-yandex-rpslimiter-log-quota: 1\r\n'

    req1 = http.request.post(
        '/quota.acquire',
        data=('GET / HTTP/1.1\r\nx-rpslimiter-balancer: namespace1\r\n'
              + common_headers + 'x-yandex-rpslimiter-custom-value: 2\r\n\r\n')
    )
    req2 = http.request.post(
        '/quota.acquire',
        data='GET / HTTP/1.1\r\nx-rpslimiter-balancer: namespace2\r\n' + common_headers + '\r\n'
    )

    ctx.start_balancer(RpslimiterSimpleConfig(quota1=3, interval1=2000, quota2=1, interval2=100))

    # ================= PHASE 1

    resp1 = ctx.perform_request(req1)
    resp2 = ctx.perform_request(req2)

    asserts.status(resp1, 200)
    asserts.no_header(resp1, 'x-forwardtouser-y')

    if log_quota or print_state:
        asserts.header_value(resp1, 'x-yandex-rpslimiter-matched-quota', 'namespace1-quota')
    else:
        asserts.no_header(resp1, 'x-yandex-rpslimiter-matched-quota')

    if print_state:
        asserts.header_value(resp1, 'x-yandex-rpslimiter-current-value', '2')
        asserts.header_value(resp1, 'x-yandex-rpslimiter-quota-limit', '3')
        asserts.header_value(resp1, 'x-yandex-rpslimiter-quota-interval-ms', '2000')
    else:
        asserts.no_header(resp1, 'x-yandex-rpslimiter-current-value')
        asserts.no_header(resp1, 'x-yandex-rpslimiter-quota-limit')
        asserts.no_header(resp1, 'x-yandex-rpslimiter-quota-interval-ms')

    asserts.status(resp2, 200)
    asserts.no_header(resp2, 'x-forwardtouser-y')

    if log_quota or print_state:
        asserts.header_value(resp2, 'x-yandex-rpslimiter-matched-quota', 'namespace2-quota')
    if print_state:
        asserts.header_value(resp2, 'x-yandex-rpslimiter-current-value', '1')
        asserts.header_value(resp2, 'x-yandex-rpslimiter-quota-limit', '1')
        asserts.header_value(resp2, 'x-yandex-rpslimiter-quota-interval-ms', '100')

    stats = ctx.get_unistat()
    for n in ("quotas-namespace1", "quotas-namespace2"):
        assert stats['{}-quota-Requests_summ'.format(n)] == 1
        # assert stats['{}-quota-InvalidRequests_summ'.format(n)] == 0
        assert stats['{}-quota-LimitedRequests_summ'.format(n)] == 0
        assert stats['{}-quota-Limited_summ'.format(n)] == 0
    assert stats['quotas-namespace1-quota-Consumed_summ'] == 2
    assert stats['quotas-namespace1-quota-Limit_axxx'] == 3
    # assert stats['quotas-namespace1-quota-Interval_ms_axxx'] == 2000
    assert stats['quotas-namespace2-quota-Consumed_summ'] == 1
    assert stats['quotas-namespace2-quota-Limit_axxx'] == 1
    # assert stats['quotas-namespace2-quota-Interval_ms_axxx'] == 100

    # ================= PHASE 2

    _guaranteed_sleep(0.2)

    resp1 = ctx.perform_request(req1)
    resp2 = ctx.perform_request(req2)

    asserts.status(resp1, 429)
    asserts.header_value(resp1, 'x-forwardtouser-y', '1')
    if log_quota or print_state:
        asserts.header_value(resp1, 'x-yandex-rpslimiter-matched-quota', 'namespace1-quota')
    else:
        asserts.no_header(resp1, 'x-yandex-rpslimiter-matched-quota')

    if print_state:
        asserts.header_value(resp1, 'x-yandex-rpslimiter-current-value', '4')
        asserts.header_value(resp1, 'x-yandex-rpslimiter-quota-limit', '3')
        asserts.header_value(resp1, 'x-yandex-rpslimiter-quota-interval-ms', '2000')
    else:
        asserts.no_header(resp1, 'x-yandex-rpslimiter-current-value')
        asserts.no_header(resp1, 'x-yandex-rpslimiter-quota-limit')
        asserts.no_header(resp1, 'x-yandex-rpslimiter-quota-interval-ms')

    asserts.status(resp2, 200)

    stats = ctx.get_unistat()
    assert stats['quotas-namespace1-quota-Requests_summ'] == 2
    assert stats['quotas-namespace1-quota-LimitedRequests_summ'] == 1
    assert stats['quotas-namespace1-quota-Consumed_summ'] == 2
    assert stats['quotas-namespace1-quota-Limited_summ'] == 2
    assert stats['quotas-namespace2-quota-Requests_summ'] == 2
    assert stats['quotas-namespace2-quota-LimitedRequests_summ'] == 0
    assert stats['quotas-namespace2-quota-Consumed_summ'] == 2
    assert stats['quotas-namespace2-quota-Limited_summ'] == 0

    _guaranteed_sleep(2)

    # ================= PHASE 3

    for r in [req1, req2]:
        resp = ctx.perform_request(r)
        asserts.status(resp, 200)
        asserts.no_header(resp, 'x-forwardtouser-y')

    stats = ctx.get_unistat()
    assert stats['quotas-namespace1-quota-Requests_summ'] == 3
    assert stats['quotas-namespace1-quota-LimitedRequests_summ'] == 1
    assert stats['quotas-namespace1-quota-Consumed_summ'] == 4
    assert stats['quotas-namespace1-quota-Limited_summ'] == 2
    assert stats['quotas-namespace2-quota-Requests_summ'] == 3
    assert stats['quotas-namespace2-quota-LimitedRequests_summ'] == 0
    assert stats['quotas-namespace2-quota-Consumed_summ'] == 3
    assert stats['quotas-namespace2-quota-Limited_summ'] == 0


def test_rpslimiter_sync_req(ctx):
    peer1_req0 = http.request.post(
        '/state.sync',
        data=render_peer_quotas(
            PeerQuotas(name="peer1", time="1607515330.000000s", quotas=[
                PeerQuota(name="namespace1-quota", window="10s", rate=1),
            ])
        )
    )
    peer1_req1 = http.request.post(
        '/state.sync',
        data=render_peer_quotas(
            PeerQuotas(name="peer1", time="1607515330.000001s", quotas=[
                PeerQuota(name="namespace1-quota", window="10s", rate=1),
            ])
        )
    )
    peer2_req0 = http.request.post(
        '/state.sync',
        data=render_peer_quotas(
            PeerQuotas(name="peer2", time="1607515330.000000s", quotas=[
                PeerQuota(name="namespace2-quota", window="10s", rate=1),
            ])
        )
    )
    peer2_req1 = http.request.post(
        '/state.sync',
        data=render_peer_quotas(
            PeerQuotas(name="peer2", time="1607515330.000001s", quotas=[
                PeerQuota(name="namespace2-quota", window="10s", rate=0),
            ])
        )
    )
    req1 = http.request.post(
        '/quota.acquire',
        data='GET / HTTP/1.1\r\nx-rpslimiter-balancer: namespace1\r\n\r\n'
    )
    req2 = http.request.post(
        '/quota.acquire',
        data='GET / HTTP/1.1\r\nx-rpslimiter-balancer: namespace2\r\n\r\n'
    )

    ctx.start_balancer(RpslimiterSimpleConfig(quota1=1, interval1=10000, quota2=2, interval2=10000))

    for r in [peer1_req0, peer2_req0]:
        resp = ctx.perform_request(r)
        asserts.status(resp, 200)
        pq = parse_peer_quotas(resp.data.content)
        assert pq.name == "self"
        assert len(pq.quotas) == 0

    asserts.status(ctx.perform_request(req1), 429)
    asserts.status(ctx.perform_request(req2), 200)
    asserts.status(ctx.perform_request(req2), 429)

    resp = ctx.perform_request(peer2_req1)
    asserts.status(resp, 200)
    pq = parse_peer_quotas(resp.data.content)
    assert pq.name == "self"
    assert pq.quotas == [PeerQuota(name="namespace2-quota", window="10.000000s", rate=1)]

    asserts.status(ctx.perform_request(req2), 200)
    asserts.status(ctx.perform_request(req2), 429)

    resp = ctx.perform_request(peer1_req1)
    asserts.status(resp, 200)
    pq = parse_peer_quotas(resp.data.content)
    assert pq.name == "self"
    assert pq.quotas == [PeerQuota(name="namespace2-quota", window="10.000000s", rate=2)]


def test_rpslimiter_sync_resp(ctx):
    peer1_resp = http.response.ok(data=render_peer_quotas(
        PeerQuotas(name="peer1", time="1607515330.000000s", quotas=[
            PeerQuota(name="namespace1-quota", window="10s", rate=1),
        ])
    ))
    peer2_resp = http.response.ok(data=render_peer_quotas(
        PeerQuotas(name="peer2", time="1607515330.000000s", quotas=[
            PeerQuota(name="namespace2-quota", window="10s", rate=1),
        ])
    ))
    req1 = http.request.post(
        '/quota.acquire',
        data='GET / HTTP/1.1\r\nx-rpslimiter-balancer: namespace1\r\n\r\n'
    )
    req2 = http.request.post(
        '/quota.acquire',
        data='GET / HTTP/1.1\r\nx-rpslimiter-balancer: namespace2\r\n\r\n'
    )

    peer1 = ctx.start_backend(SimpleConfig(response=peer1_resp), name="peer1")
    peer2 = ctx.start_backend(SimpleConfig(response=peer2_resp), name="peer2")

    ctx.start_balancer(RpslimiterSimpleConfig(
        quota1=1, interval1=10000, quota2=2, interval2=10000, sync_interval='100ms'
    ))

    _guaranteed_sleep(1)

    peer1_req = peer1.state.requests.get().request
    peer2_req = peer2.state.requests.get().request

    for req in [peer1_req, peer2_req]:
        asserts.path(req, "/state.sync")
        data = parse_peer_quotas(req.data.content)
        assert data.name == "self"
        assert len(data.time) >= 18     # definitely not zero time
        assert len(data.quotas) == 0    # nothing to send

    asserts.status(ctx.perform_request(req1), 429)
    asserts.status(ctx.perform_request(req2), 200)
    asserts.status(ctx.perform_request(req2), 429)

    _guaranteed_sleep(1)

    while True:
        try:
            peer1_req = peer1.state.requests.get(block=False).request
        except Queue.Empty:
            break

    while True:
        try:
            peer2_req = peer2.state.requests.get(block=False).request
        except Queue.Empty:
            break

    for req in [peer1_req, peer2_req]:
        data = parse_peer_quotas(req.data.content)
        assert data.name == "self"
        assert len(data.quotas) == 1
        assert data.quotas[0].name == "namespace2-quota"    # the only nonzero local rate
        assert data.quotas[0].rate == 1
        assert data.quotas[0].window == "10.000000s"
