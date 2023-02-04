# -*- coding: utf-8 -*-
from balancer.test.util.rpslimiter import PeerQuota, PeerQuotas, render_peer_quotas, parse_peer_quotas


def test_roundtrip():
    o = PeerQuotas(name="xxx", time="123.000000s", quotas=[PeerQuota(name="yyy", window="321.000000s", rate=1.5)])
    assert parse_peer_quotas("XXX") is None
    assert parse_peer_quotas(render_peer_quotas(o)) == o
