# -*- coding: utf-8 -*-
from configs import ThumbsbanConfig

from balancer.test.util import asserts

from balancer.test.util.predef import http

from balancer.test.util.predef.handler.server.http import SimpleConfig, SimpleDelayedConfig


from protobuf.search.idl import meta_pb2 as meta
from protobuf.kernel.querydata.idl import querydata_structs_pb2 as qd


BANNED_MSG = 'Not Found'
OK_MSG = 'Ok'
OK_MSG_LEN = str(len(OK_MSG))
DEFAULT_MATCH = '/?x=(\\d+)'


def build_report(banned_ids):
    report = meta.TReport()

    prop = report.SearcherProp.add()
    prop.Key = 'QueryData.debug'
    query_data = qd.TQueryData()
    for banned_id in banned_ids:
        src_factors = query_data.SourceFactors.add()
        src_factors.SourceName = 'thumbs'
        src_factors.SourceKey = banned_id
    prop.Value = query_data.SerializeToString()

    return report.SerializeToString()


def cgi_request(value):
    return http.request.get('/?x=%s' % value)


def start_balancer_with_backend(ctx, match=DEFAULT_MATCH, checker_timeout=10):
    ctx.start_backend(SimpleConfig(response=http.response.ok(data=OK_MSG)))
    ctx.start_balancer(ThumbsbanConfig(msg=BANNED_MSG, match=match, checker_timeout=checker_timeout))


def start_all(ctx, banned_ids, match=DEFAULT_MATCH):
    report = build_report(banned_ids)
    ctx.start_backend(SimpleConfig(response=http.response.ok(data=report)), name='checker')
    start_balancer_with_backend(ctx, match=match)


def base_ok_test(ctx, banned_ids, uri, match=DEFAULT_MATCH):
    start_all(ctx, banned_ids, match=match)
    response = ctx.perform_request(cgi_request(uri))
    asserts.content(response, OK_MSG)


def base_banned_test(ctx, banned_ids, uri, match=DEFAULT_MATCH):
    start_all(ctx, banned_ids, match=match)
    response = ctx.perform_request(cgi_request(uri))
    asserts.status(response, 404)
    asserts.content(response, BANNED_MSG)


def base_async(ctx, uri, checker_timeout, backend_timeout, wait_timeout):
    report = build_report(['123'])
    ctx.start_backend(SimpleDelayedConfig(response=http.response.ok(data=report), response_delay=checker_timeout),
                      name='checker')
    ctx.start_backend(SimpleDelayedConfig(response=http.response.ok(data=OK_MSG), response_delay=backend_timeout))
    ctx.start_balancer(ThumbsbanConfig(msg=BANNED_MSG))
    return ctx.perform_request(cgi_request(uri), timeout=wait_timeout)


def base_async_banned_test(ctx, checker_timeout, backend_timeout, wait_timeout):
    response = base_async(ctx, '123', checker_timeout, backend_timeout, wait_timeout)
    asserts.status(response, 404)
    asserts.content(response, BANNED_MSG)


def base_async_ok_test(ctx, checker_timeout, backend_timeout, wait_timeout):
    response = base_async(ctx, '456', checker_timeout, backend_timeout, wait_timeout)
    asserts.content(response, OK_MSG)


def test_banned_id(ctx):
    """
    Если id, указанный в запросе клиента,
    встречается в ответе checker-а (хотя бы в одном из SearcherProp.Value.SourceFactors.SourceKey),
    то запрос идет в подмодуль ban_handler
    """
    banned_id = '123'

    base_banned_test(ctx, [banned_id], banned_id)


def test_not_banned_id(ctx):
    """
    Если id, указанный в запросе клиента,
    не встречается в ответе checker-а (ни в одном из SearcherProp.Value.SourceFactors.SourceKey),
    то запрос идет в module
    """
    banned_id = 123
    request_id = banned_id + 1

    base_ok_test(ctx, [str(banned_id)], str(request_id))


def test_not_matching_request(ctx):
    """
    Если uri в запросе клиента не матчится регулярным выражением, указанным в конфиге,
    то запрос идет в module
    """
    banned_id = '123'
    uri = 'uri'

    base_ok_test(ctx, [banned_id], uri)


def test_broken_checker(ctx):
    """
    Если не удается разобрать ответ checker-а, то клиентский запрос идет в module
    """
    check_response = 'error'
    request_id = '123'

    ctx.start_backend(SimpleConfig(response=http.response.ok(data=check_response)), name='checker')
    start_balancer_with_backend(ctx)

    response = ctx.perform_request(cgi_request(request_id))

    asserts.content(response, OK_MSG)


def test_no_checker(ctx):
    """
    Если checker не отвечает, то клиентский запрос идет в module
    """
    request_id = '123'

    ctx.start_fake_backend(name='checker')
    start_balancer_with_backend(ctx)

    response = ctx.perform_request(cgi_request(request_id))

    asserts.content(response, OK_MSG)


def test_checker_timeout(ctx):
    """
    SEPE-4542
    Если checker не отвечает за timeout, то клиентский запрос идет в module
    """
    checker_timeout = 1
    request_id = '123'
    report = build_report([request_id])

    ctx.start_backend(SimpleDelayedConfig(response=http.response.ok(data=report), response_delay=checker_timeout * 5),
                      name='checker')
    start_balancer_with_backend(ctx, checker_timeout=checker_timeout)

    response = ctx.perform_request(cgi_request(request_id))

    asserts.content(response, OK_MSG)


def test_several_groups_in_regexp_banned(ctx):
    """
    Условия:
    если в регулярном выражении указано несколько групп, то id берется из первой группы
    id, находящийся в первой группе, забанен

    Поведение:
    клиентский запрос уходит в ban_handler
    """
    banned_id = '123'
    match = '/?x=(\\d+)-(\\d+)-(\\d+)'

    base_banned_test(ctx, [banned_id], '%s-456-789' % banned_id, match=match)


def test_several_groups_in_regexp_ok(ctx):
    """
    Условия:
    если в регулярном выражении указано несколько групп, то id берется из первой группы
    id, находящийся в первой группе, не забанен

    Поведение:
    клиентский запрос уходит в module
    """
    banned_id = '456'
    match = '/?x=(\\d+)-(\\d+)-(\\d+)'

    base_ok_test(ctx, [banned_id], '123-%s-789' % banned_id, match=match)


def test_async_slow_checker_banned(ctx):
    """
    IMAGES-5004
    Запросы в checker и module должны идти параллельно
    Если module ответил раньше, нужно дождаться ответа checker'а
    Запрос забанен
    """
    checker_timeout = 5
    backend_timeout = 3
    wait_timeout = checker_timeout + 1
    base_async_banned_test(ctx, checker_timeout, backend_timeout, wait_timeout)


def test_async_slow_checker_ok(ctx):
    """
    IMAGES-5004
    Запросы в checker и module должны идти параллельно
    Если module ответил раньше, нужно дождаться ответа checker'а
    Запрос не забанен
    """
    checker_timeout = 5
    backend_timeout = 3
    wait_timeout = checker_timeout + 1
    base_async_ok_test(ctx, checker_timeout, backend_timeout, wait_timeout)


def test_async_slow_module_banned(ctx):
    """
    IMAGES-5004
    Запросы в checker и module должны идти параллельно
    Если checker ответил, что модуль забанен, а ответ из module еще не пришел, вернуть ответ из ban_handler
    """
    checker_timeout = 3
    backend_timeout = 5
    wait_timeout = checker_timeout + 1
    base_async_banned_test(ctx, checker_timeout, backend_timeout, wait_timeout)


def test_async_slow_module_ok(ctx):
    """
    IMAGES-5004
    Запросы в checker и module должны идти параллельно
    Если checker ответил, что модуль не забанен, а ответ из module еще не пришел, дождаться ответа от module
    """
    checker_timeout = 3
    backend_timeout = 5
    wait_timeout = backend_timeout + 1
    base_async_ok_test(ctx, checker_timeout, backend_timeout, wait_timeout)
