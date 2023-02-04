import pytest

from common import start_balancer, BACKEND_REQ, STATIC_REQ
from common import CLIENT_HEADERS, CLIENT_HEADERS_CYCLE, CLIENT_HEADERS_CRY, CLIENT_HEADERS_CRY_CYCLE, BACKEND_HEADERS, CRYPROX_HEADERS
from common import CLIENT_RESP_HEADERS, CRYPROX_CRYPT_HEADERS, CRYPROX_CRYPT_HEADERS_CRY

from balancer.test.util import asserts
from balancer.test.util.predef import http
from balancer.test.util.predef.handler.server.http import SimpleConfig


def test_no_cryprox(ctx):
    ctx.start_backend(SimpleConfig(http.response.ok(headers=BACKEND_HEADERS)))
    ctx.start_backend(SimpleConfig(http.response.ok(headers=CRYPROX_HEADERS)), name='cryprox_backend')

    start_balancer(ctx)

    resp = ctx.perform_request(http.request.get(headers=CLIENT_HEADERS))
    asserts.status(resp, 200)
    asserts.headers_values(resp, BACKEND_HEADERS)

    backend_req = ctx.backend.state.get_request()
    asserts.path(backend_req, '/')
    asserts.headers_values(backend_req, CLIENT_HEADERS)

    assert ctx.cryprox_backend.state.requests.empty()

    unistat = ctx.get_unistat()
    assert unistat['cryprox-use_cryprox_summ'] == 0
    assert unistat['cryprox-forward_to_backend_summ'] == 0
    assert unistat['cryprox-forward_to_cryprox_summ'] == 0
    assert unistat['cryprox-cycle_summ'] == 0
    assert unistat['cryprox-cryprox_test_summ'] == 0


def test_forward(ctx):
    ctx.start_backend(SimpleConfig(http.response.ok(headers=BACKEND_HEADERS)))
    ctx.start_backend(SimpleConfig(http.response.ok(headers=CRYPROX_HEADERS)), name='cryprox_backend')

    start_balancer(ctx, disable_cryprox=True)

    resp = ctx.perform_request(http.request.get(STATIC_REQ, headers=CLIENT_HEADERS))
    asserts.status(resp, 200)
    asserts.headers_values(resp, CRYPROX_HEADERS)

    assert ctx.backend.state.requests.empty()

    cryprox_req = ctx.cryprox_backend.state.get_request()
    asserts.path(cryprox_req, STATIC_REQ)
    asserts.headers_values(cryprox_req, CLIENT_HEADERS)

    unistat = ctx.get_unistat()
    assert unistat['cryprox-use_cryprox_summ'] == 0
    assert unistat['cryprox-forward_to_backend_summ'] == 0
    assert unistat['cryprox-forward_to_cryprox_summ'] == 1
    assert unistat['cryprox-cycle_summ'] == 0
    assert unistat['cryprox-cryprox_test_summ'] == 0


def test_forward_cycle(ctx):
    ctx.start_backend(SimpleConfig(http.response.ok(headers=BACKEND_HEADERS)))
    ctx.start_backend(SimpleConfig(http.response.ok(headers=CRYPROX_HEADERS)), name='cryprox_backend')

    start_balancer(ctx)

    resp = ctx.perform_request(http.request.get(STATIC_REQ, headers=CLIENT_HEADERS_CYCLE))
    asserts.status(resp, 200)
    asserts.headers_values(resp, BACKEND_HEADERS)

    backend_req = ctx.backend.state.get_request()
    asserts.path(backend_req, STATIC_REQ)
    asserts.headers_values(backend_req, CLIENT_HEADERS_CYCLE)

    assert ctx.cryprox_backend.state.requests.empty()

    unistat = ctx.get_unistat()
    assert unistat['cryprox-use_cryprox_summ'] == 0
    assert unistat['cryprox-forward_to_backend_summ'] == 0
    assert unistat['cryprox-forward_to_cryprox_summ'] == 0
    assert unistat['cryprox-cycle_summ'] == 1
    assert unistat['cryprox-cryprox_test_summ'] == 0


def test_decrypt(ctx):
    ctx.start_backend(SimpleConfig(http.response.ok(headers=BACKEND_HEADERS)))
    ctx.start_backend(SimpleConfig(http.response.ok(headers=CRYPROX_HEADERS)), name='cryprox_backend')

    start_balancer(ctx)

    resp = ctx.perform_request(http.request.get(BACKEND_REQ, headers=CLIENT_HEADERS))
    asserts.status(resp, 200)
    asserts.headers_values(resp, CLIENT_RESP_HEADERS)

    backend_req = ctx.backend.state.get_request()
    asserts.path(backend_req, '/index.html')
    asserts.headers_values(backend_req, CLIENT_HEADERS)

    cryprox_req = ctx.cryprox_backend.state.get_request()
    asserts.path(cryprox_req, '/crypt_content')
    asserts.headers_values(cryprox_req, CRYPROX_CRYPT_HEADERS)

    unistat = ctx.get_unistat()
    assert unistat['cryprox-use_cryprox_summ'] == 1
    assert unistat['cryprox-forward_to_backend_summ'] == 1
    assert unistat['cryprox-forward_to_cryprox_summ'] == 0
    assert unistat['cryprox-rewind_summ'] == 0
    assert unistat['cryprox-unable_to_rewind_summ'] == 0
    assert unistat['report-cryprox_backend-backend_fail_summ'] == 0
    assert unistat['report-service_backend-backend_fail_summ'] == 0


def test_decrypt_disable_file(ctx):
    ctx.start_backend(SimpleConfig(http.response.ok(headers=BACKEND_HEADERS)))
    ctx.start_backend(SimpleConfig(http.response.ok(headers=CRYPROX_HEADERS)), name='cryprox_backend')

    start_balancer(ctx, disable_cryprox=True)

    resp = ctx.perform_request(http.request.get(BACKEND_REQ, headers=CLIENT_HEADERS))
    asserts.status(resp, 200)
    asserts.headers_values(resp, BACKEND_HEADERS)

    backend_req = ctx.backend.state.get_request()
    asserts.path(backend_req, '/index.html')
    asserts.headers_values(backend_req, CLIENT_HEADERS)

    assert ctx.cryprox_backend.state.requests.empty()

    unistat = ctx.get_unistat()
    assert unistat['cryprox-use_cryprox_summ'] == 0
    assert unistat['cryprox-forward_to_backend_summ'] == 1
    assert unistat['cryprox-forward_to_cryprox_summ'] == 0
    assert unistat['cryprox-rewind_summ'] == 0
    assert unistat['cryprox-unable_to_rewind_summ'] == 0
    assert unistat['report-cryprox_backend-backend_fail_summ'] == 0
    assert unistat['report-service_backend-backend_fail_summ'] == 0


def test_crypt(ctx):
    ctx.start_backend(SimpleConfig(http.response.ok(headers=BACKEND_HEADERS)))
    ctx.start_backend(SimpleConfig(http.response.ok(headers=CRYPROX_HEADERS)), name='cryprox_backend')

    start_balancer(ctx)

    resp = ctx.perform_request(http.request.get(headers=CLIENT_HEADERS_CRY))
    asserts.status(resp, 200)
    asserts.headers_values(resp, CLIENT_RESP_HEADERS)

    backend_req = ctx.backend.state.get_request()
    asserts.path(backend_req, '/')
    asserts.headers_values(backend_req, CLIENT_HEADERS_CRY)

    cryprox_req = ctx.cryprox_backend.state.get_request()
    asserts.path(cryprox_req, '/crypt_content')
    asserts.headers_values(cryprox_req, CRYPROX_CRYPT_HEADERS_CRY)

    unistat = ctx.get_unistat()
    assert unistat['cryprox-use_cryprox_summ'] == 1
    assert unistat['cryprox-forward_to_backend_summ'] == 0
    assert unistat['cryprox-forward_to_cryprox_summ'] == 0
    assert unistat['cryprox-cycle_summ'] == 0
    assert unistat['cryprox-cryprox_test_summ'] == 0


def test_crypt_cycle(ctx):
    ctx.start_backend(SimpleConfig(http.response.ok(headers=BACKEND_HEADERS)))
    ctx.start_backend(SimpleConfig(http.response.ok(headers=CRYPROX_HEADERS)), name='cryprox_backend')

    start_balancer(ctx)

    resp = ctx.perform_request(http.request.get(headers=CLIENT_HEADERS_CRY_CYCLE))
    asserts.status(resp, 200)
    asserts.headers_values(resp, BACKEND_HEADERS)

    backend_req = ctx.backend.state.get_request()
    asserts.path(backend_req, '/')
    asserts.headers_values(backend_req, CLIENT_HEADERS_CRY_CYCLE)

    assert ctx.cryprox_backend.state.requests.empty()

    unistat = ctx.get_unistat()
    assert unistat['cryprox-use_cryprox_summ'] == 0
    assert unistat['cryprox-forward_to_backend_summ'] == 0
    assert unistat['cryprox-forward_to_cryprox_summ'] == 0
    assert unistat['cryprox-cycle_summ'] == 1
    assert unistat['cryprox-cryprox_test_summ'] == 0


def test_crypt_disable_file(ctx):
    ctx.start_backend(SimpleConfig(http.response.ok(headers=BACKEND_HEADERS)))
    ctx.start_backend(SimpleConfig(http.response.ok(headers=CRYPROX_HEADERS)), name='cryprox_backend')

    start_balancer(ctx, disable_cryprox=True)

    resp = ctx.perform_request(http.request.get(headers=CLIENT_HEADERS_CRY))
    asserts.status(resp, 200)
    asserts.headers_values(resp, BACKEND_HEADERS)

    backend_req = ctx.backend.state.get_request()
    asserts.path(backend_req, '/')
    asserts.headers_values(backend_req, CLIENT_HEADERS_CRY)

    assert ctx.cryprox_backend.state.requests.empty()

    unistat = ctx.get_unistat()
    assert unistat['cryprox-use_cryprox_summ'] == 0
    assert unistat['cryprox-forward_to_backend_summ'] == 0
    assert unistat['cryprox-forward_to_cryprox_summ'] == 0
    assert unistat['cryprox-cycle_summ'] == 0
    assert unistat['cryprox-cryprox_test_summ'] == 0


@pytest.mark.parametrize('header', ['x-aab-http-check', 'x-aab-jstracer'])
def test_crypt_test(ctx, header):
    ctx.start_backend(SimpleConfig(http.response.ok(headers=BACKEND_HEADERS)))
    ctx.start_backend(SimpleConfig(http.response.ok(headers=CRYPROX_HEADERS)), name='cryprox_backend')

    start_balancer(ctx, disable_cryprox=True)

    client_headers = dict(CLIENT_HEADERS)
    client_headers[header] = '1'
    resp = ctx.perform_request(http.request.get(headers=client_headers))
    asserts.status(resp, 200)
    asserts.headers_values(resp, CRYPROX_HEADERS)

    assert ctx.backend.state.requests.empty()

    cryprox_req = ctx.cryprox_backend.state.get_request()
    asserts.path(cryprox_req, '/')
    asserts.headers_values(cryprox_req, client_headers)

    unistat = ctx.get_unistat()
    assert unistat['cryprox-use_cryprox_summ'] == 0
    assert unistat['cryprox-forward_to_backend_summ'] == 0
    assert unistat['cryprox-forward_to_cryprox_summ'] == 0
    assert unistat['cryprox-cycle_summ'] == 0
    assert unistat['cryprox-cryprox_test_summ'] == 1
