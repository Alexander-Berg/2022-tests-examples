# coding=utf-8
import time

from configs import RpslimiterCombinedConfig

from balancer.test.util import asserts
from balancer.test.util.predef import http


def test_rpslimiter_combined(ctx):
    req1 = http.request.get("/ns1")
    req2 = http.request.get("/ns2")

    ctx.start_balancer(RpslimiterCombinedConfig(quota1=1, interval1=1000, quota2=1, interval2=100))

    def check_resp_200(req):
        resp = ctx.perform_request(req)
        asserts.status(resp, 200)
        asserts.content(resp, req.request_line.path)

    for r in [req1, req2]:
        check_resp_200(r)

    time.sleep(0.5)

    asserts.status(ctx.perform_request(req1), 429)
    check_resp_200(req2)

    time.sleep(1)

    for r in [req1, req2]:
        check_resp_200(r)
