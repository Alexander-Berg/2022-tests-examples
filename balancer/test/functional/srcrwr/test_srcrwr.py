# -*- coding: utf-8 -*-

import pytest

from balancer.test.util.predef import http
from balancer.test.util.predef.handler.server.http import SimpleConfig
from configs import SrcRwrConfig, UntrustedSrcRwrConfig, AnySrcRwrConfig, NestedSrcRwrConfig, SrcRwrMultiConfig, SrcRwrMultiConfig2


HOST_DELIMITER = ';'
SRC_RWR_ID = 'Morpheus'
OTHER_SRC_RWR_ID = 'Merowinger'
BACKENDS_IP = ':127.0.0.1:'
BACKENDS_ADDRESS = ':localhost:'
BACKENDS_IPV6 = ':[::1]:'
BACKEND_INVALID_ADDRESS = ''

SRC_RWR_PATH_START = '/matrix?l7rwr=' + SRC_RWR_ID + BACKENDS_IP
SRC_RWR_PATH_START2 = '/matrix?red_pill=yes&l7rwr=' + SRC_RWR_ID + BACKENDS_IP
SRC_RWR_PATH_START3 = '/matrix?l7rwr=' + SRC_RWR_ID + BACKENDS_IPV6
SRC_RWR_PATH_START4 = '/matrix?name=Neo&red_pill=yes&l7rwr=' + SRC_RWR_ID + BACKENDS_IPV6
SRC_RWR_PATH_START5 = '/matrix?l7rwr=' + SRC_RWR_ID + BACKENDS_ADDRESS
OTHER_SRC_RWR_PATH_START = '/matrix?l7rwr=' + OTHER_SRC_RWR_ID + BACKENDS_ADDRESS
NOT_SRC_RWR_PATH = '/matrix?l7rwr=' + BACKENDS_IP
NOT_SRC_RWR_PATH2 = '/' + SRC_RWR_ID + '?l7rwr='
NOT_SRC_RWR_PATH3 = '/' + SRC_RWR_ID + '?l7rwr'
NOT_SRC_RWR_PATH4 = '/' + SRC_RWR_ID + '?' + SRC_RWR_ID + '=' + SRC_RWR_ID + BACKENDS_IP + '80'
NOT_SRC_RWR_PATH5 = '/' + '?gvhgyugyu*:::::::gy4Ё!"№?*()_;%:{}|:">?<guy/-+%235gyui6t686876oy88py09go08t68fr433s35'
NOT_SRC_RWR_PATH6 = '/matrix?'
NOT_SRC_RWR_PATH7 = '/matrix?l7rwr=Morpheus::'
NOT_SRC_RWR_PATH8 = '/matrix?l7rwr=:'
NOT_SRC_RWR_PATH9 = '/matrix?l7rwr=:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::'
NOT_SRC_RWR_PATH10 = '/matrix?:'
NOT_SRC_RWR_PATH11 = '/matrix?l7rw=' + SRC_RWR_ID + BACKENDS_IP

CORRECT_HEADER = {'Host': "hamster.hi.there", 'X-Yandex-Internal-Request': '1'}
CORRECT_HEADER2 = {'Host': "www.beta.hamster.hi.there"}
OTHER_HOST_HEADER = {'Host': "hamster_is_not_here.hi.there"}
OTHER_HOST_HEADER2 = {'Host': "www.any.host.can.be.here.www."}

HEADER_DELIM = ','
HANDLED_HEADER = {'Host': "hamster.hi.there", 'X-Yandex-Internal-SrcRwr': SRC_RWR_ID}
HANDLED_HEADER2 = {'Host': "www.beta.hamster.hi.there", 'X-Yandex-Internal-SrcRwr': OTHER_SRC_RWR_ID}
HANDLED_HEADER3 = {'Host': "hamster.hi.there", 'X-Yandex-Internal-SrcRwr': SRC_RWR_ID + HEADER_DELIM + OTHER_SRC_RWR_ID}
HANDLED_HEADER4 = {'Host': "www.beta.hamster.hi.there", 'X-Yandex-Internal-SrcRwr': OTHER_SRC_RWR_ID + HEADER_DELIM + SRC_RWR_ID + HEADER_DELIM + 'another_id'}
REQUESTS_FOR_MULTI = 7  # make several requests in order to have chance of starting from both hosts


def start_all(ctx, default_backend_confg, srcrwr_backend_config, **balancer_kwargs):
    ctx.start_backend(default_backend_confg, name='default_backend')
    ctx.start_backend(srcrwr_backend_config, name='srcrwr_backend')
    ctx.start_balancer(SrcRwrConfig(id=SRC_RWR_ID, **balancer_kwargs))


def start_all_untrusted(ctx, default_backend_confg, srcrwr_backend_config, **balancer_kwargs):
    ctx.start_backend(default_backend_confg, name='default_backend')
    ctx.start_backend(srcrwr_backend_config, name='srcrwr_backend')
    ctx.start_balancer(UntrustedSrcRwrConfig(id=SRC_RWR_ID, **balancer_kwargs))


def start_all_any(ctx, default_backend_confg, srcrwr_backend_config, **balancer_kwargs):
    ctx.start_backend(default_backend_confg, name='default_backend')
    ctx.start_backend(srcrwr_backend_config, name='srcrwr_backend')
    ctx.start_balancer(AnySrcRwrConfig(id=SRC_RWR_ID, **balancer_kwargs))


def start_all_nested(ctx, default_backend_confg, srcrwr_backend_config, **balancer_kwargs):
    ctx.start_fake_backend(name='fake_backend')
    ctx.start_backend(default_backend_confg, name='default_backend')
    ctx.start_backend(srcrwr_backend_config, name='srcrwr_backend')
    ctx.start_balancer(NestedSrcRwrConfig(id=SRC_RWR_ID, **balancer_kwargs))


def start_all_multi(ctx, default_backend_confg, default_backend2_confg, default_backend3_confg,
                    srcrwr_backend_config, **balancer_kwargs):
    ctx.start_fake_backend(name='fake_backend')
    ctx.start_backend(default_backend_confg, name='default_backend')
    ctx.start_backend(default_backend2_confg, name='default_backend2')
    ctx.start_backend(default_backend3_confg, name='default_backend3')
    ctx.start_backend(srcrwr_backend_config, name='srcrwr_backend')
    ctx.start_balancer(SrcRwrMultiConfig(id=SRC_RWR_ID, **balancer_kwargs))


def start_all_multi2(ctx, default_backend_confg, default_backend2_confg, default_backend3_confg,
                     srcrwr_backend_config, srcrwr_backend2_config, **balancer_kwargs):
    ctx.start_fake_backend(name='fake_backend')
    ctx.start_backend(default_backend_confg, name='default_backend')
    ctx.start_backend(default_backend2_confg, name='default_backend2')
    ctx.start_backend(default_backend3_confg, name='default_backend3')
    ctx.start_backend(srcrwr_backend_config, name='srcrwr_backend')
    ctx.start_backend(srcrwr_backend2_config, name='srcrwr_backend2')
    ctx.start_balancer(SrcRwrMultiConfig2(id=SRC_RWR_ID, **balancer_kwargs))


@pytest.mark.parametrize('path_start', [SRC_RWR_PATH_START, SRC_RWR_PATH_START2, SRC_RWR_PATH_START3, SRC_RWR_PATH_START4, SRC_RWR_PATH_START5])
def test_srcrwr_works(ctx, path_start):
    """
    Correct srcrwr request correctly redirected
    """
    start_all(ctx, SimpleConfig(), SimpleConfig())

    path = path_start + str(ctx.balancer.config.srcrwr_backend_port)
    response = ctx.perform_request(http.request.get(path=path, headers=CORRECT_HEADER))
    assert response.status == 200

    assert ctx.default_backend.state.accepted.value == 0
    assert ctx.srcrwr_backend.state.accepted.value == 1


@pytest.mark.parametrize('handled_header', [HANDLED_HEADER, HANDLED_HEADER3, HANDLED_HEADER4])
def test_several_srcrwr_work(ctx, handled_header):
    """
    Several correct srcrwr requests correctly redirected
    """
    start_all(ctx, SimpleConfig(), SimpleConfig())

    path = SRC_RWR_PATH_START + str(ctx.balancer.config.srcrwr_backend_port)
    response = ctx.perform_request(http.request.get(path=path, headers=CORRECT_HEADER))
    assert response.status == 200

    assert ctx.default_backend.state.accepted.value == 0
    assert ctx.srcrwr_backend.state.accepted.value == 1

    response = ctx.perform_request(http.request.get(path=path, headers=HANDLED_HEADER2))
    assert response.status == 200

    assert ctx.default_backend.state.accepted.value == 0
    assert ctx.srcrwr_backend.state.accepted.value == 2

    response = ctx.perform_request(http.request.get(path=path, headers=handled_header))
    assert response.status == 200

    assert ctx.default_backend.state.accepted.value == 1
    assert ctx.srcrwr_backend.state.accepted.value == 2


@pytest.mark.parametrize('handled_header', [HANDLED_HEADER, HANDLED_HEADER3, HANDLED_HEADER4])
@pytest.mark.parametrize('path_start', [SRC_RWR_PATH_START, SRC_RWR_PATH_START2, SRC_RWR_PATH_START3, SRC_RWR_PATH_START4, SRC_RWR_PATH_START5])
def test_several_srcrwr_in_cgi_work(ctx, path_start, handled_header):
    """
    Several srcrwr in cgi correctly redirected
    """
    start_all(ctx, SimpleConfig(), SimpleConfig())

    port = str(ctx.balancer.config.srcrwr_backend_port)
    path = path_start + port + '&l7rwr=' + OTHER_SRC_RWR_ID + BACKENDS_ADDRESS + port
    response = ctx.perform_request(http.request.get(path=path, headers=handled_header))
    assert response.status == 200

    assert ctx.default_backend.state.accepted.value == 1
    assert ctx.srcrwr_backend.state.accepted.value == 0

    response = ctx.perform_request(http.request.get(path=path, headers=HANDLED_HEADER2))
    assert response.status == 200

    assert ctx.default_backend.state.accepted.value == 1
    assert ctx.srcrwr_backend.state.accepted.value == 1


@pytest.mark.parametrize('keepalive_count', [None, 1, 5])
@pytest.mark.parametrize('path_start', [SRC_RWR_PATH_START, SRC_RWR_PATH_START2, SRC_RWR_PATH_START3, SRC_RWR_PATH_START4, SRC_RWR_PATH_START5])
@pytest.mark.parametrize('connection_manager_required', [False, True], ids=['', 'connection_manager_required'])
def test_srcrwr_works_with_keepalive(ctx, path_start, keepalive_count, connection_manager_required):
    """
    with keepalive, both srcrwr and plain requests work correctly
    """

    start_all(ctx, SimpleConfig(), SimpleConfig(), keepalive_count=keepalive_count, connection_manager_required=connection_manager_required)

    for i in range(0, 10):
        path = path_start + str(ctx.balancer.config.srcrwr_backend_port)
        response = ctx.perform_request(http.request.get(path=path, headers=CORRECT_HEADER))
        assert response.status == 200

        assert ctx.default_backend.state.requests.qsize() == i
        assert ctx.srcrwr_backend.state.requests.qsize() == i + 1

        path = path_start
        response = ctx.perform_request(http.request.get(path=path, headers=CORRECT_HEADER))
        assert response.status == 200

        assert ctx.default_backend.state.requests.qsize() == i + 1
        assert ctx.srcrwr_backend.state.requests.qsize() == i + 1

    if keepalive_count:
        assert ctx.default_backend.state.accepted.value == 1
        assert ctx.srcrwr_backend.state.accepted.value == 10
    else:
        assert ctx.default_backend.state.accepted.value == 10
        assert ctx.srcrwr_backend.state.accepted.value == 10


@pytest.mark.parametrize('path', [NOT_SRC_RWR_PATH,
                                  NOT_SRC_RWR_PATH2,
                                  NOT_SRC_RWR_PATH3,
                                  NOT_SRC_RWR_PATH4,
                                  NOT_SRC_RWR_PATH5,
                                  NOT_SRC_RWR_PATH6,
                                  NOT_SRC_RWR_PATH7,
                                  NOT_SRC_RWR_PATH8,
                                  NOT_SRC_RWR_PATH9,
                                  NOT_SRC_RWR_PATH10,
                                  NOT_SRC_RWR_PATH11])
def test_srcrwr_with_not_srcrwr_path_not_works(ctx, path):
    """
    srcrwr request with not srcrwr CGI is not handled
    """
    start_all(ctx, SimpleConfig(), SimpleConfig())

    port = str(ctx.balancer.config.srcrwr_backend_port)
    path += port
    ctx.perform_request(http.request.get(path=path, headers=CORRECT_HEADER))

    assert ctx.default_backend.state.accepted.value == 1
    assert ctx.srcrwr_backend.state.accepted.value == 0

    ctx.perform_request(http.request.get(path=SRC_RWR_PATH_START + port, headers=CORRECT_HEADER))

    assert ctx.default_backend.state.accepted.value == 1
    assert ctx.srcrwr_backend.state.accepted.value == 1


@pytest.mark.parametrize('srcrwr_address', [BACKEND_INVALID_ADDRESS])
def test_srcrwr_with_invalid_address_not_works(ctx, srcrwr_address):
    """
    srcrwr request with invalid address in CGI not redirected
    """
    start_all(ctx, SimpleConfig(), SimpleConfig())

    path = '/matrix?l7rwr=' + SRC_RWR_ID + srcrwr_address + str(ctx.balancer.config.srcrwr_backend_port)
    ctx.perform_request(http.request.get(path=path, headers=CORRECT_HEADER))

    assert ctx.default_backend.state.accepted.value == 1
    assert ctx.srcrwr_backend.state.accepted.value == 0


@pytest.mark.parametrize('path_start', [SRC_RWR_PATH_START, SRC_RWR_PATH_START2, SRC_RWR_PATH_START3, SRC_RWR_PATH_START4, SRC_RWR_PATH_START5])
def test_srcrwr_with_invalid_port_not_works(ctx, path_start):
    """
    srcrwr request with invalid port in CGI not redirected
    """
    start_all(ctx, SimpleConfig(), SimpleConfig())

    path = path_start + 'q'
    ctx.perform_request(http.request.get(path=path, headers=CORRECT_HEADER))

    assert ctx.default_backend.state.accepted.value == 1
    assert ctx.srcrwr_backend.state.accepted.value == 0

    path2 = path_start + '123456'
    ctx.perform_request(http.request.get(path=path2, headers=CORRECT_HEADER))

    assert ctx.default_backend.state.accepted.value == 2
    assert ctx.srcrwr_backend.state.accepted.value == 0

    path3 = path_start + '-1'
    ctx.perform_request(http.request.get(path=path3, headers=CORRECT_HEADER))

    assert ctx.default_backend.state.accepted.value == 3
    assert ctx.srcrwr_backend.state.accepted.value == 0


@pytest.mark.parametrize('path_start', [SRC_RWR_PATH_START, SRC_RWR_PATH_START2, SRC_RWR_PATH_START3, SRC_RWR_PATH_START4, SRC_RWR_PATH_START5])
def test_srcrwr_middle_pos_works(ctx, path_start):
    """
    Correct srcrwr request with srcrwr param in the middle of CGI correctly redirected
    """
    start_all(ctx, SimpleConfig(), SimpleConfig())

    path = path_start + str(ctx.balancer.config.srcrwr_backend_port) + '&blue_pill=no'
    response = ctx.perform_request(http.request.get(path=path, headers=CORRECT_HEADER2))
    assert response.status == 200

    assert ctx.default_backend.state.accepted.value == 0
    assert ctx.srcrwr_backend.state.accepted.value == 1


@pytest.mark.parametrize('path_start', [SRC_RWR_PATH_START, SRC_RWR_PATH_START2, SRC_RWR_PATH_START3, SRC_RWR_PATH_START4, SRC_RWR_PATH_START5])
def test_srcrwr_with_correct_header_works(ctx, path_start):
    """
    Correct srcrwr request with correct header redirected
    """
    start_all(ctx, SimpleConfig(), SimpleConfig())

    path = path_start + str(ctx.balancer.config.srcrwr_backend_port)
    response = ctx.perform_request(http.request.get(path=path, headers=CORRECT_HEADER2))
    assert response.status == 200

    assert ctx.default_backend.state.accepted.value == 0
    assert ctx.srcrwr_backend.state.accepted.value == 1


@pytest.mark.parametrize('path_start', [SRC_RWR_PATH_START, SRC_RWR_PATH_START2, SRC_RWR_PATH_START3, SRC_RWR_PATH_START4, SRC_RWR_PATH_START5])
def test_srcrwr_with_not_matched_host_not_redirected(ctx, path_start):
    """
    Correct srcrwr request with other host is not redirected
    """
    start_all(ctx, SimpleConfig(), SimpleConfig())

    path = path_start + str(ctx.balancer.config.srcrwr_backend_port)
    response = ctx.perform_request(http.request.get(path=path, headers=OTHER_HOST_HEADER))
    assert response.status == 200

    assert ctx.default_backend.state.accepted.value == 1
    assert ctx.srcrwr_backend.state.accepted.value == 0


@pytest.mark.parametrize('handled_header', [HANDLED_HEADER, HANDLED_HEADER3, HANDLED_HEADER4])
@pytest.mark.parametrize('path_start', [SRC_RWR_PATH_START, SRC_RWR_PATH_START2, SRC_RWR_PATH_START3, SRC_RWR_PATH_START4, SRC_RWR_PATH_START5])
def test_handled_srcrwr_not_redirected(ctx, path_start, handled_header):
    """
    Handled srcrwr request is not redirected
    """
    start_all(ctx, SimpleConfig(), SimpleConfig())

    path = SRC_RWR_PATH_START + str(ctx.balancer.config.srcrwr_backend_port)
    request = http.request.get(path=path, headers=handled_header)
    response = ctx.perform_request(request)
    assert response.status == 200

    assert ctx.default_backend.state.accepted.value == 1
    assert ctx.srcrwr_backend.state.accepted.value == 0


@pytest.mark.parametrize('path_start', [SRC_RWR_PATH_START, SRC_RWR_PATH_START2, SRC_RWR_PATH_START3, SRC_RWR_PATH_START4, SRC_RWR_PATH_START5])
def test_other_srcrwr_handled_this_redirected(ctx, path_start):
    """
    Handled srcrwr request is not redirected
    """
    start_all(ctx, SimpleConfig(), SimpleConfig())

    path = SRC_RWR_PATH_START + str(ctx.balancer.config.srcrwr_backend_port)
    request = http.request.get(path=path, headers=HANDLED_HEADER2)
    response = ctx.perform_request(request)
    assert response.status == 200

    assert ctx.default_backend.state.accepted.value == 0
    assert ctx.srcrwr_backend.state.accepted.value == 1


@pytest.mark.parametrize('path_start', [SRC_RWR_PATH_START, SRC_RWR_PATH_START2, SRC_RWR_PATH_START3, SRC_RWR_PATH_START4, SRC_RWR_PATH_START5])
def test_untrusted_srcrwr_not_works(ctx, path_start):
    """
    Correct srcrwr request not from trusted network is not handled
    """
    start_all_untrusted(ctx, SimpleConfig(), SimpleConfig())

    path = path_start + str(ctx.balancer.config.srcrwr_backend_port)
    response = ctx.perform_request(http.request.get(path=path, headers=CORRECT_HEADER))
    assert response.status == 200

    assert ctx.default_backend.state.accepted.value == 1
    assert ctx.srcrwr_backend.state.accepted.value == 0


@pytest.mark.parametrize('path_start', [SRC_RWR_PATH_START, SRC_RWR_PATH_START2, SRC_RWR_PATH_START3, SRC_RWR_PATH_START4, SRC_RWR_PATH_START5])
def test_untrusted_srcrwr_not_works2(ctx, path_start):
    """
    Correct srcrwr request not from trusted network is not handled
    """
    start_all_untrusted(ctx, SimpleConfig(), SimpleConfig())

    path = path_start + str(ctx.balancer.config.srcrwr_backend_port)
    response = ctx.perform_request(http.request.get(path=path, headers=CORRECT_HEADER2))
    assert response.status == 200

    assert ctx.default_backend.state.accepted.value == 1
    assert ctx.srcrwr_backend.state.accepted.value == 0


@pytest.mark.parametrize('path_start', [SRC_RWR_PATH_START, SRC_RWR_PATH_START2, SRC_RWR_PATH_START3, SRC_RWR_PATH_START4, SRC_RWR_PATH_START5])
def test_ip_any_mask(ctx, path_start):
    """
    universal IP mask work
    """
    start_all_any(ctx, SimpleConfig(), SimpleConfig())

    path = path_start + str(ctx.balancer.config.srcrwr_backend_port)
    response = ctx.perform_request(http.request.get(path=path, headers=CORRECT_HEADER))
    assert response.status == 200

    assert ctx.default_backend.state.accepted.value == 0
    assert ctx.srcrwr_backend.state.accepted.value == 1


@pytest.mark.parametrize('path_start', [SRC_RWR_PATH_START, SRC_RWR_PATH_START2, SRC_RWR_PATH_START3, SRC_RWR_PATH_START4, SRC_RWR_PATH_START5])
def test_host_any_mask(ctx, path_start):
    """
    universal host mask work
    """
    start_all_any(ctx, SimpleConfig(), SimpleConfig())

    path = path_start + str(ctx.balancer.config.srcrwr_backend_port)
    response = ctx.perform_request(http.request.get(path=path, headers=CORRECT_HEADER2))
    assert response.status == 200

    assert ctx.default_backend.state.accepted.value == 0
    assert ctx.srcrwr_backend.state.accepted.value == 1

    response = ctx.perform_request(http.request.get(path=path, headers=OTHER_HOST_HEADER2))
    assert response.status == 200

    assert ctx.default_backend.state.accepted.value == 0
    assert ctx.srcrwr_backend.state.accepted.value == 2


@pytest.mark.parametrize('path_start', [SRC_RWR_PATH_START, SRC_RWR_PATH_START2, SRC_RWR_PATH_START3, SRC_RWR_PATH_START4, SRC_RWR_PATH_START5])
def test_with_all_headers_works_as_expected(ctx, path_start):
    """
    check that several requests work as expected
    """
    start_all(ctx, SimpleConfig(), SimpleConfig())

    path = path_start + str(ctx.balancer.config.srcrwr_backend_port)
    response = ctx.perform_request(http.request.get(path=path, headers=CORRECT_HEADER))
    assert response.status == 200

    assert ctx.default_backend.state.accepted.value == 0
    assert ctx.srcrwr_backend.state.accepted.value == 1

    response = ctx.perform_request(http.request.get(path=path, headers=OTHER_HOST_HEADER))
    assert response.status == 200

    assert ctx.default_backend.state.accepted.value == 1
    assert ctx.srcrwr_backend.state.accepted.value == 1

    response = ctx.perform_request(http.request.get(path=path, headers=HANDLED_HEADER2))
    assert response.status == 200

    assert ctx.default_backend.state.accepted.value == 1
    assert ctx.srcrwr_backend.state.accepted.value == 2

    response = ctx.perform_request(http.request.get(path=path, headers=CORRECT_HEADER2))
    assert response.status == 200

    assert ctx.default_backend.state.accepted.value == 1
    assert ctx.srcrwr_backend.state.accepted.value == 3

    response = ctx.perform_request(http.request.get(path=path, headers=HANDLED_HEADER))
    assert response.status == 200

    assert ctx.default_backend.state.accepted.value == 2
    assert ctx.srcrwr_backend.state.accepted.value == 3


@pytest.mark.parametrize('path_start', [SRC_RWR_PATH_START, SRC_RWR_PATH_START2, SRC_RWR_PATH_START3, SRC_RWR_PATH_START4, SRC_RWR_PATH_START5])
def test_empty_header_routed_to_default(ctx, path_start):
    """
    srcrwr request with empty field in header is correctly redirected
    """
    start_all(ctx, SimpleConfig(), SimpleConfig())

    path = path_start + str(ctx.balancer.config.srcrwr_backend_port)
    headers = {}
    response = ctx.perform_request(http.request.get(path=path, headers=headers))
    assert response.status == 200

    assert ctx.default_backend.state.accepted.value == 1
    assert ctx.srcrwr_backend.state.accepted.value == 0

    # Check that srcrwr is not broken after empty header request
    response2 = ctx.perform_request(http.request.get(path=path, headers=CORRECT_HEADER))
    assert response2.status == 200

    assert ctx.default_backend.state.accepted.value == 1
    assert ctx.srcrwr_backend.state.accepted.value == 1


@pytest.mark.parametrize('path_start', [SRC_RWR_PATH_START, SRC_RWR_PATH_START2, SRC_RWR_PATH_START3, SRC_RWR_PATH_START4, SRC_RWR_PATH_START5])
def test_several_requests_handled_as_expected(ctx, path_start):
    """
    Several srcrwr/not srcrwr requests are handled as expected
    """
    start_all(ctx, SimpleConfig(), SimpleConfig())

    path = path_start + str(ctx.balancer.config.srcrwr_backend_port)
    headers = {}
    for x in xrange(5):
        response = ctx.perform_request(http.request.get(path=path, headers=headers))
        assert response.status == 200

    assert ctx.default_backend.state.accepted.value == 5
    assert ctx.srcrwr_backend.state.accepted.value == 0

    # Check that srcrwr is not broken after empty header request
    for x in xrange(3):
        response = ctx.perform_request(http.request.get(path=path, headers=CORRECT_HEADER))
        assert response.status == 200

    assert ctx.default_backend.state.accepted.value == 5
    assert ctx.srcrwr_backend.state.accepted.value == 3

    for x in xrange(2):
        response = ctx.perform_request(http.request.get(path=path, headers=CORRECT_HEADER2))
        assert response.status == 200

    assert ctx.default_backend.state.accepted.value == 5
    assert ctx.srcrwr_backend.state.accepted.value == 5

    ctx.perform_request(http.request.get(path=NOT_SRC_RWR_PATH, headers=CORRECT_HEADER2))
    ctx.perform_request(http.request.get(path=NOT_SRC_RWR_PATH2, headers=CORRECT_HEADER))
    ctx.perform_request(http.request.get(path=path, headers=HANDLED_HEADER2))
    ctx.perform_request(http.request.get(path=path, headers=HANDLED_HEADER))
    ctx.perform_request(http.request.get(path=NOT_SRC_RWR_PATH3, headers=CORRECT_HEADER2))
    ctx.perform_request(http.request.get(path=NOT_SRC_RWR_PATH4, headers=CORRECT_HEADER))
    ctx.perform_request(http.request.get(path=NOT_SRC_RWR_PATH5, headers=CORRECT_HEADER2))
    ctx.perform_request(http.request.get(path=NOT_SRC_RWR_PATH6, headers=CORRECT_HEADER))
    ctx.perform_request(http.request.get(path=NOT_SRC_RWR_PATH7, headers=CORRECT_HEADER))

    assert ctx.default_backend.state.accepted.value == 13
    assert ctx.srcrwr_backend.state.accepted.value == 6

    for x in xrange(2):
        response = ctx.perform_request(http.request.get(path=path, headers=CORRECT_HEADER))
        assert response.status == 200

    assert ctx.default_backend.state.accepted.value == 13
    assert ctx.srcrwr_backend.state.accepted.value == 8

    ctx.perform_request(http.request.get(path=path, headers=OTHER_HOST_HEADER))
    ctx.perform_request(http.request.get(path=path, headers=CORRECT_HEADER2))
    ctx.perform_request(http.request.get(path=path, headers=HANDLED_HEADER))

    assert ctx.default_backend.state.accepted.value == 15
    assert ctx.srcrwr_backend.state.accepted.value == 9

    for x in xrange(4):
        response = ctx.perform_request(http.request.get(path=path, headers=CORRECT_HEADER))
        assert response.status == 200
        response = ctx.perform_request(http.request.get(path=path, headers=CORRECT_HEADER2))
        assert response.status == 200

    ctx.perform_request(http.request.get(path=NOT_SRC_RWR_PATH8, headers=CORRECT_HEADER))
    ctx.perform_request(http.request.get(path=NOT_SRC_RWR_PATH9, headers=CORRECT_HEADER))

    assert ctx.default_backend.state.accepted.value == 17
    assert ctx.srcrwr_backend.state.accepted.value == 17

    for x in xrange(2):
        response = ctx.perform_request(http.request.get(path=path, headers=CORRECT_HEADER))
        assert response.status == 200

    # Hard match, srcrwr wins
    assert ctx.default_backend.state.accepted.value == 17
    assert ctx.srcrwr_backend.state.accepted.value == 19


@pytest.mark.parametrize('path_start', [SRC_RWR_PATH_START, SRC_RWR_PATH_START2, SRC_RWR_PATH_START3, SRC_RWR_PATH_START4, SRC_RWR_PATH_START5])
def test_srcrwr_nested_balancer_works(ctx, path_start):
    """
    srcrwr inside balancer2 works
    """
    start_all_nested(ctx, SimpleConfig(), SimpleConfig())

    path = path_start + str(ctx.balancer.config.srcrwr_backend_port)
    response = ctx.perform_request(http.request.get(path=path, headers=CORRECT_HEADER))
    assert response.status == 200

    assert ctx.default_backend.state.accepted.value == 0
    assert ctx.srcrwr_backend.state.accepted.value == 1


def test_srcrwr_nested_balancer_clean_up_works(ctx):
    """
    srcrwr performs clean up when flow goes upper to another tree branch
    """
    start_all_nested(ctx, SimpleConfig(), SimpleConfig())

    path = SRC_RWR_PATH_START + str(ctx.balancer.config.fake_backend_port)
    response = ctx.perform_request(http.request.get(path=path, headers=CORRECT_HEADER))
    assert response.status == 200

    assert ctx.default_backend.state.accepted.value == 1
    assert ctx.srcrwr_backend.state.accepted.value == 0


def test_srcrwr_two_valid_hosts_work(ctx):
    """
    srcrwr works with 2 valid hosts
    """
    start_all_multi2(ctx, SimpleConfig(), SimpleConfig(), SimpleConfig(), SimpleConfig(), SimpleConfig())
    path = '/matrix?l7rwr=' + SRC_RWR_ID + ':[::1]:' + str(ctx.balancer.config.srcrwr_backend_port) + HOST_DELIMITER \
           + 'localhost:' + str(ctx.balancer.config.srcrwr_backend2_port)

    for _ in range(REQUESTS_FOR_MULTI):
        response = ctx.perform_request(http.request.get(path=path, headers=CORRECT_HEADER))
        assert response.status == 200

    assert ctx.default_backend.state.accepted.value == 0
    assert ctx.default_backend2.state.accepted.value == 0
    assert ctx.default_backend3.state.accepted.value == 0
    assert ctx.srcrwr_backend.state.accepted.value != 0
    assert ctx.srcrwr_backend2.state.accepted.value != 0
    assert ctx.srcrwr_backend.state.accepted.value + ctx.srcrwr_backend2.state.accepted.value == REQUESTS_FOR_MULTI


@pytest.mark.parametrize('path_start', ['/matrix?l7rwr=', '/matrix?name=Neo&red_pill=yes&l7rwr='])
def test_several_srcrwr_with_two_hosts_in_cgi_work(ctx, path_start):
    """
    Several srcrwr with several hosts in cgi correctly redirected
    """
    start_all_multi2(ctx, SimpleConfig(), SimpleConfig(), SimpleConfig(), SimpleConfig(), SimpleConfig())
    path = path_start + SRC_RWR_ID + ':[::1]:' + str(ctx.balancer.config.srcrwr_backend_port) + HOST_DELIMITER \
            + 'localhost:' + str(ctx.balancer.config.srcrwr_backend_port) \
            + '&l7rwr=' + OTHER_SRC_RWR_ID + ':127.0.0.1:' + str(ctx.balancer.config.srcrwr_backend2_port) + HOST_DELIMITER \
            + 'localhost:' + str(ctx.balancer.config.srcrwr_backend2_port)

    response = ctx.perform_request(http.request.get(path=path, headers=CORRECT_HEADER))
    assert response.status == 200

    for _ in range(3):
        response = ctx.perform_request(http.request.get(path=path, headers=HANDLED_HEADER))
        assert response.status == 200

    response = ctx.perform_request(http.request.get(path=path, headers=HANDLED_HEADER2))
    assert response.status == 200

    assert ctx.default_backend.state.accepted.value + ctx.default_backend2.state.accepted.value \
           + ctx.default_backend3.state.accepted.value == 3
    assert ctx.srcrwr_backend.state.accepted.value == 2
    assert ctx.srcrwr_backend2.state.accepted.value == 0


@pytest.mark.parametrize('handled_header', [HANDLED_HEADER, HANDLED_HEADER3, HANDLED_HEADER4])
@pytest.mark.parametrize('path_start', ['/matrix?l7rwr=', '/matrix?name=Neo&red_pill=yes&l7rwr='])
def test_several_srcrwr_with_two_hosts_in_cgi_work2(ctx, path_start, handled_header):
    """
    Several srcrwr with several hosts in cgi correctly redirected
    """
    start_all_multi2(ctx, SimpleConfig(), SimpleConfig(), SimpleConfig(), SimpleConfig(), SimpleConfig())
    path = path_start + OTHER_SRC_RWR_ID + ':[::1]:' + str(ctx.balancer.config.srcrwr_backend_port) + HOST_DELIMITER \
           + 'localhost:' + str(ctx.balancer.config.srcrwr_backend_port) \
           + '&l7rwr=' + SRC_RWR_ID + ':127.0.0.1:' + str(ctx.balancer.config.srcrwr_backend2_port) + HOST_DELIMITER \
           + 'localhost:' + str(ctx.balancer.config.srcrwr_backend2_port) + '&blue_pill=no'

    response = ctx.perform_request(http.request.get(path=path, headers=CORRECT_HEADER))
    assert response.status == 200

    for _ in range(3):
        response = ctx.perform_request(http.request.get(path=path, headers=handled_header))
        assert response.status == 200

    response = ctx.perform_request(http.request.get(path=path, headers=HANDLED_HEADER2))
    assert response.status == 200

    assert ctx.default_backend.state.accepted.value + ctx.default_backend2.state.accepted.value \
           + ctx.default_backend3.state.accepted.value == 3
    assert ctx.srcrwr_backend.state.accepted.value == 0
    assert ctx.srcrwr_backend2.state.accepted.value == 2


@pytest.mark.parametrize('valid_host', ['127.0.0.1:', 'localhost:', '[::1]:'])
def test_srcrwr_second_host_works(ctx, valid_host):
    """
    srcrwr uses second host from srcrwr param
    """
    start_all_multi(ctx, SimpleConfig(), SimpleConfig(), SimpleConfig(), SimpleConfig())
    path = '/matrix?l7rwr=' + SRC_RWR_ID + ':localhost:11' + HOST_DELIMITER \
           + valid_host + str(ctx.balancer.config.srcrwr_backend_port)

    for _ in range(REQUESTS_FOR_MULTI):
        try:
            response = ctx.perform_request(http.request.get(path=path, headers=CORRECT_HEADER))
            assert response.status == 200
        except:
            pass

    assert ctx.default_backend.state.accepted.value == 0
    assert ctx.default_backend2.state.accepted.value == 0
    assert ctx.default_backend3.state.accepted.value == 0
    assert ctx.srcrwr_backend.state.accepted.value == REQUESTS_FOR_MULTI


def test_srcrwr_first_in_three_hosts_works(ctx):
    """
    srcrwr uses first host from srcrwr param
    """
    start_all_multi(ctx, SimpleConfig(), SimpleConfig(), SimpleConfig(), SimpleConfig(), attempts=3)
    path = '/matrix?l7rwr=' + SRC_RWR_ID + ':localhost:' + str(ctx.balancer.config.srcrwr_backend_port) \
           + HOST_DELIMITER + '127.0.0.1:10' + HOST_DELIMITER + '[::1]:11'

    for _ in range(REQUESTS_FOR_MULTI):
        try:
            response = ctx.perform_request(http.request.get(path=path, headers=CORRECT_HEADER))
            assert response.status == 200
        except:
            pass

    assert ctx.default_backend.state.accepted.value == 0
    assert ctx.default_backend2.state.accepted.value == 0
    assert ctx.default_backend3.state.accepted.value == 0
    assert ctx.srcrwr_backend.state.accepted.value == REQUESTS_FOR_MULTI


def test_srcrwr_second_from_three_hosts_works(ctx):
    """
    srcrwr uses second host from srcrwr param
    """
    start_all_multi(ctx, SimpleConfig(), SimpleConfig(), SimpleConfig(), SimpleConfig(), attempts=3)
    path = '/matrix?l7rwr=' + SRC_RWR_ID + ':localhost:10' + HOST_DELIMITER \
           + '127.0.0.1:' + str(ctx.balancer.config.srcrwr_backend_port) \
           + HOST_DELIMITER + '127.0.0.1:11'

    for _ in range(REQUESTS_FOR_MULTI):
        try:
            response = ctx.perform_request(http.request.get(path=path, headers=CORRECT_HEADER))
            assert response.status == 200
        except:
            pass

    assert ctx.default_backend.state.accepted.value == 0
    assert ctx.default_backend2.state.accepted.value == 0
    assert ctx.default_backend3.state.accepted.value == 0
    assert ctx.srcrwr_backend.state.accepted.value == REQUESTS_FOR_MULTI


def test_srcrwr_third_host_works(ctx):
    """
    srcrwr uses third host from srcrwr param
    """
    start_all_multi(ctx, SimpleConfig(), SimpleConfig(), SimpleConfig(), SimpleConfig(), attempts=3)
    path = '/matrix?l7rwr=' + SRC_RWR_ID + ':localhost:10' + HOST_DELIMITER + '127.0.0.1:11' \
           + HOST_DELIMITER + '127.0.0.1:' + str(ctx.balancer.config.srcrwr_backend_port)

    for _ in range(REQUESTS_FOR_MULTI):
        try:
            response = ctx.perform_request(http.request.get(path=path, headers=CORRECT_HEADER))
            assert response.status == 200
        except:
            pass

    assert ctx.default_backend.state.accepted.value == 0
    assert ctx.default_backend2.state.accepted.value == 0
    assert ctx.default_backend3.state.accepted.value == 0
    assert ctx.srcrwr_backend.state.accepted.value == REQUESTS_FOR_MULTI


@pytest.mark.parametrize('valid_host', [BACKENDS_IP, BACKENDS_ADDRESS, BACKENDS_IPV6])
def test_srcrwr_first_host_works(ctx, valid_host):
    """
    srcrwr uses first host from srcrwr param
    """
    start_all_multi(ctx, SimpleConfig(), SimpleConfig(), SimpleConfig(), SimpleConfig())
    path = '/matrix?l7rwr=' + SRC_RWR_ID + valid_host + str(ctx.balancer.config.srcrwr_backend_port) \
           + HOST_DELIMITER + '[::1]:11'

    for _ in range(REQUESTS_FOR_MULTI):
        try:
            response = ctx.perform_request(http.request.get(path=path, headers=CORRECT_HEADER))
            assert response.status == 200
        except:
            pass

    assert ctx.default_backend.state.accepted.value == 0
    assert ctx.default_backend2.state.accepted.value == 0
    assert ctx.default_backend3.state.accepted.value == 0
    assert ctx.srcrwr_backend.state.accepted.value == REQUESTS_FOR_MULTI


def test_srcrwr_unreachable_hosts_not_work(ctx):
    """
    default backends are not used even if srcrwr hosts unreachable
    """
    start_all_multi(ctx, SimpleConfig(), SimpleConfig(), SimpleConfig(), SimpleConfig())
    path = '/matrix?l7rwr=' + SRC_RWR_ID + ':localhost:11' + HOST_DELIMITER + '127.0.0.1:10'

    for _ in range(REQUESTS_FOR_MULTI):
        try:
            ctx.perform_request(http.request.get(path=path, headers=CORRECT_HEADER))
        except:
            pass

    assert ctx.default_backend.state.accepted.value == 0
    assert ctx.default_backend2.state.accepted.value == 0
    assert ctx.default_backend3.state.accepted.value == 0
    assert ctx.srcrwr_backend.state.accepted.value == 0


def test_srcrwr_unreachable_hosts_not_work_with_multiattempts(ctx):
    """
    default backends are not used even if srcrwr hosts unreachable
    """
    start_all_multi(ctx, SimpleConfig(), SimpleConfig(), SimpleConfig(), SimpleConfig(), attempts=6)
    path = '/matrix?l7rwr=' + SRC_RWR_ID + ':localhost:11' + HOST_DELIMITER + 'localhost:10'

    for _ in range(REQUESTS_FOR_MULTI):
        try:
            ctx.perform_request(http.request.get(path=path, headers=CORRECT_HEADER))
        except:
            pass

    assert ctx.default_backend.state.accepted.value == 0
    assert ctx.default_backend2.state.accepted.value == 0
    assert ctx.default_backend3.state.accepted.value == 0
    assert ctx.srcrwr_backend.state.accepted.value == 0


def test_srcrwr_hosts_not_resolved_not_work(ctx):
    """
    default backends are not used even if srcrwr hosts are not resolved
    """
    start_all_multi(ctx, SimpleConfig(), SimpleConfig(), SimpleConfig(), SimpleConfig(), attempts=4)
    path = '/matrix?l7rwr=' + SRC_RWR_ID + ':nkiijm2iopj3oi5pjio:443' \
           + HOST_DELIMITER + 'joi6jou7ihui9hui3huihuih:443'

    for _ in range(REQUESTS_FOR_MULTI):
        try:
            ctx.perform_request(http.request.get(path=path, headers=CORRECT_HEADER))
        except:
            pass

    assert ctx.default_backend.state.accepted.value == 0
    assert ctx.default_backend2.state.accepted.value == 0
    assert ctx.default_backend3.state.accepted.value == 0
    assert ctx.srcrwr_backend.state.accepted.value == 0


@pytest.mark.parametrize('invalid_hosts', [':127.0.0.1;joi6jou7ihui9hui3huihuih:',
                                           ':localhost:10;[::1]',
                                           ':[::1]:10:localhost:'])
def test_srcrwr_invalid_format_hosts_default_backends_work(ctx, invalid_hosts):
    """
    default backends are used if srcrwr hosts in invalid format
    """
    start_all_multi(ctx, SimpleConfig(), SimpleConfig(), SimpleConfig(), SimpleConfig(), attempts=4)
    path = '/matrix?l7rwr=' + SRC_RWR_ID + invalid_hosts + str(ctx.balancer.config.srcrwr_backend_port)

    try:
        ctx.perform_request(http.request.get(path=path, headers=CORRECT_HEADER))
    except:
        pass

    assert ctx.default_backend.state.accepted.value + ctx.default_backend2.state.accepted.value \
           + ctx.default_backend3.state.accepted.value == 1
    assert ctx.srcrwr_backend.state.accepted.value == 0


@pytest.mark.parametrize('postfix', [',', ':'])
def test_srcrwr_invalid_format_hosts_default_backends_work_2(ctx, postfix):
    """
    default backends are used if srcrwr hosts in invalid format
    """
    start_all_multi(ctx, SimpleConfig(), SimpleConfig(), SimpleConfig(), SimpleConfig())
    path = '/matrix?l7rwr=' + SRC_RWR_ID + ':[::1]:' + str(ctx.balancer.config.srcrwr_backend_port) \
           + HOST_DELIMITER + 'localhost:' + str(ctx.balancer.config.srcrwr_backend_port) + postfix

    try:
        ctx.perform_request(http.request.get(path=path, headers=CORRECT_HEADER))
    except:
        pass

    assert ctx.default_backend.state.accepted.value + ctx.default_backend2.state.accepted.value \
           + ctx.default_backend3.state.accepted.value == 1
    assert ctx.srcrwr_backend.state.accepted.value == 0
