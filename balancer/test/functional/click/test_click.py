# -*- coding: utf-8 -*-
import time
import threading
from configs import ClickConfig

from balancer.test.util import asserts
from balancer.test.util.stdlib.multirun import Multirun

from balancer.test.util.predef.handler.server.http import ChunkedConfig, CloseConfig, SimpleDelayedConfig
from balancer.test.util.predef import http


DATA = 'A' * 50

REDIR_PATH = (
    '/redir/lyBG7xl2OZnf8mVbHwkAu870jTE4Nc-AIbbTU5xdsYPb1lNKwK2gRqNJlV_f64lrTaRbPvctd4T7n1CzoBFRmcfqUTp0QCivZBgcMCRBXHw'
    '?data=cFFxNnZ1ak5FdGN6WDF4dW5nVmxnRmIycTV2bmk1eUZjX0hETkRjekV1aEdRTVhhN0wzRGFQWHZvN3Q0Szh3Z052bmtvNk5POTctVEpHMWNV'
    'SGtsMlE&b64e=2&sign=f8b8324516b73bb87e51a01ca234cce7&keyno=9'
)

SAFECLICK_PATH = (
    '/safeclick/data=AiuY0DBWFJ7IXge4WdYJQW9NxnjWmbq1hNjSRxpoFBRuGu1PZDUoPyQ2oZeg0d6aM2T0cfnrpo9rdzrflO1SDBsRhzsfN8cJAP'
    'N7XxbpaDp2iOlKpEf7OIlVmZob6u90X0RIfc4ae9Q/sign=1af6ce178c5acd4e96948ea062470391/keyno=0/path=405.493.85/par=qq/*ht'
    'tp://yandex.ru/'
)

CLICK_PATH = '/click/key1=val1/key2=val2/*http://url.ru/'

JCLICK_PATH = (
    '/jclck/dtype=stred/pid=0/cid=72043/path=M0/reqid=1403000941230849-1781107356270484874126423-1-006/rnd=140300094383'
    '7/*data=url%3Dhttp%253A%252F%252Fbeta.yandex.ru%252Fyandsearch%253Fl10n%253Dru%2526text%253D%2525D0%2525BA%2525D0%'
    '2525B0%2525D0%2525BB%2525D1%25258C%2525D0%2525BA%2525D1%252583%2525D0%2525BB%2525D1%25258F%2525D1%252582%2525D0%25'
    '25BE%2525D1%252580%2526lr%253D213%2526test-mode%253D1%2526no-tests%253D1'
)

NOT_CLICK_PATH = '/yandsearch?text=abc'

JSREDIR_PATH = (
    '/jsredir?from=from&text=text&uuid=&state=0y7OGf7LqDYZrH_6YcotOFNIXlhXKR-l8Wip6Bz48VizILVZJLnL5s05ZYQpyzuY&data=cFF'
    'xNnZ1ak5FdGN6WDF4dW5nVmxnS0hKemY2UlpZbHhrSTNTR0NQN1RVcEVLWFA1amZUQVNJck9uRkdYd2xjR3N5TUVwZ0FmUk5HdC12V18tdExqYlR4Z'
    '2hhUjlfZGNw&b64e=2&sign=844fa367b3b4793ed964fd8934a84b02&keyno=9'
)
JSREDIR_DATA = (
    '<html><head><meta name="referrer" content="always"/><noscript><META http-equiv="refresh" content="0;URL=\'http://n'
    'ikonschool.ru/wedding/\'"></noscript></head><body><script>(function(e){if(/MSIE (\\d+\\.\\d+);/.test(navigator.use'
    'rAgent)){var t=document.createElement("a");t.href=e;document.body.appendChild(t);t.click()}else{if (navigator.user'
    'Agent.indexOf("YaBrowser") > -1) {try{window.opener=null} catch (exc){};}location.replace(e)}})("http://nikonschoo'
    'l.ru/wedding/")</script></body></html>'
)

CLICK_LIB_404_PATH = (
    '/click/dtype=stred/pid=1/cid=72202/path=690.1201/reqid=1406899503957464-244648864461038264719279-ws11-422-REASK/va'
    'rs=13=http%3A%2F%2Fyandex.ru%2Fyandsearch%3Fcallback%3DjQuery18305468255374580622_1406899506537%26yu%3D70422322514'
    '00230933%26staticVersion%3D0xb7ad46d%26rf%3Dhttp%253A%252F%252Fyandex.ru%252Fyandsearch%253Ftext%253D1.%252520%252'
    '5D0%2525A1%2525D0%2525B8%2525D1%252581%2525D1%252582%2525D0%2525B5%2525D0%2525BC%2525D0%2525B0%2525D1%252582%2525D'
    '0%2525B8%2525D1%252587%2525D0%2525B5%2525D1%252581%2525D0%2525BA%2525D0%2525B8%2525D0%2525B9%252520%2525D1%252580%'
    '2525D0%2525B8%2525D1%252581%2525D0%2525BA%252520%2525D0%2525B2%252520%2525D1%252580%2525D0%2525B0%2525D0%2525BC%25'
    '25D0%2525BA%2525D0%2525B0%2525D1%252585%252520%2525D1%252582%2525D0%2525B5%2525D0%2525BE%2525D1%252580%2525D0%2525'
    'B8%2525D0%2525B8%252520%2525D0%2525BF%2525D0%2525BE%2525D1%252580%2525D1%252582%2525D1%252584%2525D0%2525B5%2525D0'
    '%2525BB%2525D1%25258F%252520%2525D0%2525BE%2525D1%252586%2525D0%2525B5%2525D0%2525BD%2525D0%2525B8%2525D0%2525B2%2'
    '525D0%2525B0%2525D0%2525B5%2525D1%252582%2525D1%252581%2525D1%25258F%252520%2525D1%252581%252520%252520%2525D0%252'
    '5BF%2525D0%2525BE%2525D0%2525BC%2525D0%2525BE%2525D1%252589%2525D1%25258C%2525D1%25258E%25253A%252520%2525D0%2525B'
    '0)%252520%2525CE%2525BB%252520%2525E2%252580%252593%252520%2525D0%2525BA%2525D0%2525BE%2525D1%25258D%2525D1%252584'
    '%2525D1%252584%2525D0%2525B8%2525D1%252586%2525D0%2525B8%2525D0%2525B5%2525D0%2525BD%2525D1%252582%2525D0%2525B0%2'
    '5253B%252520%2525D0%2525B1)%252520%2525CE%2525B2%252520%2525E2%252580%252593%252520%2525D0%2525BA%2525D0%2525BE%25'
    '25D1%25258D%2525D1%252584%2525D1%252584%2525D0%2525B8%2525D1%252586%2525D0%2525B8%2525D0%2525B5%2525D0%2525BD%2525'
    'D1%252582%2525D0%2525B0%25253B%252520%2525D0%2525B2)%252520%2525D1%252581%2525D1%252580%2525D0%2525B5%2525D0%2525B'
    '4%2525D0%2525BD%2525D0%2525'
)
JSREDIR_PROXY_TRUE = (
    '/jsredir?from=gas.serp.yandex.ru%3Bsearch%2F%3Bweb%3B%3B&text=&etext=1150.g_lIW3NdJssOQKwcsGmsQGf_5s3kJ7xPu6GLxhJp'
    'OSg.b3c2a0f6b8a1e1661157a7615b305eda8b2c512e&uuid=&state=ZpOnm2f2Yydx2nY8eUKfHX8Z8FX64Ch3chI4OmL70ZWpqANJ8alY6laZb'
    'xMRs4yLF2Ca3TlEj6elSC7I8gpWLw&data=UlNrNmk5WktYejR0eWJFYk1LdmtxczJadmZYWW50TTNVVGJJay0wM2VYNkhHNW5mRnc2NXJyaXZfemZ'
    'FcFlRWHc2MEd2Mi0zdXhiWEpBcXJjc3ItYVhzQ1VrLVN4QWt4SFJCNGdiYmg3NkpaMVU5MDBvVlYwRHpxQ0Y2WWlES3RwME1fX3lpRmZSRVdQT1N4a'
    '0huMGFBbWxzWi1WYlBDdG5JeXFfb2ZJVTFYdFRMODYyc1lQUWF1U2N5UXdsanJYU2h2cm5zOFdjQmZUUXpIRGVYN1M0STlVem0wTU1pNnNXOUNzZTV'
    'JWGJJeHJFT1RKdGNkbHRGNFRDNk9kc2p0ZUpQcm85TkRnV0VVYjlzZkxUdW5TQmJvZnZTYkx0ZmNXTy03enRTOTFONENRS29EVi0zVm40UTdUaldRR'
    'TZhdHlmT1RESjRQR05mbVJXTlFzMlMxVnBpZ0FOM2hwZVd1MW1TTGNQQkQ0WjY5NWVDemxWVGxnN3dBSUxQVzQ4SS0yeDQxV2p4ZHFENEFwbVM0dFV'
    'Ia0VqRFVxUG9wZ3N4SldhVjB2bDIxdmpINEVQWE5iclJ3TGNrLVY3NEdIand2bU1WVE9UVVRCUHd3eUtnQ0d6LVRPQ1JNUUtLV1JmME9abE9LUllRU'
    'WVib0FRSXdOY3pwVWJyTnBsRUppQVM4TjRBc2dBQmM0T0Zpc3JjV2NDbHlEZzdRbFBBUzdrSEdxVQ&b64e=2&sign=439aee9425d4666d9920f717'
    'd1902fdb&keyno=0'
)
JSREDIR_PROXY_FALSE = (
    '/jsredir?from=gas.serp.yandex.ru%3Bsearch%2F%3Bweb%3B%3B&text=&etext=1150.gOV5HLS_zs7YKh3i8q8fLAxVReujrvqMQcRCn0QR'
    'ypI.1220d66fcd531e4a5ef43df49afc7ca73c9dd574&uuid=&state=GBA4PjJEy4NvR7JgmD1zLTDRUsxaDPZRcRL6Yb_uI0DVk1UJnDsNZqsT_'
    'uE7vUBFahU0Y-563GAgDELEPKURW0C6G1K_ivNbxO2LVkUX6l8&data=YTJoTDdjS1JYNHhGYmt2OWRKMDFVWVhzaXhmZnNabEh4NDdCcEZPM2lBUW'
    'hoU2F5UlNuTFo3X0dkVHZQMm5hUUdlZGhQd3NCbkM4bFBOM2laU1d0TWhjeG04dEpKeGxBZy1kaXk2bDNockoyUk5IbWFiTG5fQ0ZxTlhYeVRueVo&'
    'b64e=2&sign=73aa528a05de83ec69378a1ccdd7e38f&keyno=19&cst=egTgYitFrP7EwKmKUmxd8iysIhgeOVYqKVs336gTXcm5JFw9ewsV1WL0'
    'x9m_a-gfyUrUPM5WO3GO-6-qJDICL8fpRY2nfmJaeI9MXnV5Tj07CQ7iajdSo6Y-_rttwwgpHUQzxDYirRWOTJYr1MfQRSS1xsNHpN9_nSjgiGRppf'
    'UwDum65vljYB5Zib6sWcTy&ref=-N2ur5JCmmQNapbvAJdq2TShak7bM2cfx6UTZW50e58DjBNU-nyDpvm5Zi87B9qcWxvWVlquzn5k6Lt0x5FMGw&'
    'l10n=ru&cts=1474559776302&mc=1'
)

CLICK_LIB_404_HEADERS = {
    'User-Agent': 'curl/7.22.0 (x86_64-pc-linux-gnu) libcurl/7.22.0 OpenSSL/1.0.1 zlib/1.2.3.4 libidn/1.23 librtmp/2.3',
    'Accept': '*/*',
    'Host': 'yandex.ru',
}


def keys(ctx):
    return ctx.certs.abs_path('clickdaemon.keys')


def start_all(ctx, timeout=0, file_switch=None, response_data=None):
    if response_data is None:
        response_data = ['A' * 10] * 5
    ctx.start_backend(ChunkedConfig(response=http.response.ok(data=response_data), chunk_timeout=timeout))
    ctx.start_balancer(ClickConfig(keys=keys(ctx), file_switch=file_switch))


def base_click_test(ctx, request, file_switch=None):
    start_all(ctx, file_switch=file_switch)
    if file_switch is not None:
        time.sleep(2)
    response = ctx.create_http_connection().perform_request_raw_response(request)
    time.sleep(1)
    assert ctx.backend.state.requests.qsize() == 1
    return response


def test_redir(ctx):
    """
    На запрос /redir балансер должен отдать ответ из библотеки и отправить запрос backend-у
    """
    response = base_click_test(ctx, http.request.get(path=REDIR_PATH))
    asserts.status(response, 302)
    asserts.header_value(response, 'Location', 'http://company.yandex.com/')


def test_jsredir(ctx):
    """
    На запрос /jsredir балансер должен отдать ответ из библотеки
    (200 OK c софтредиректом на таргет урл) и отправить запрос backend-у
    """
    response = base_click_test(ctx, http.request.get(path=JSREDIR_PATH))
    asserts.status(response, 200)
    asserts.content(response, JSREDIR_DATA)


def test_safeclick(ctx):
    """
    На запрос /safeclick балансер должен отдать ответ из библотеки и отправить запрос backend-у
    """
    response = base_click_test(ctx, http.request.get(path=SAFECLICK_PATH))
    asserts.status(response, 200)
    asserts.image(response)


def test_click(ctx):
    """
    На запрос /click балансер должен отдать ответ из библотеки и отправить запрос backend-у
    """
    response = base_click_test(ctx, http.request.get(path=CLICK_PATH))
    asserts.status(response, 200)
    asserts.image(response)


def test_jclick(ctx):
    """
    На запрос /jclick балансер должен отдать ответ из библотеки и отправить запрос backend-у
    """
    response = base_click_test(ctx, http.request.get(path=JCLICK_PATH))
    asserts.status(response, 200)
    asserts.content(response, '/* counted */')


def test_click_keepalive(ctx):
    """
    Проверка keepalive при ответе кликовой библиотекой
    """
    start_all(ctx)
    conn = ctx.create_http_connection()
    resp1 = conn.perform_request_raw_response(http.request.get(path=CLICK_PATH))
    resp2 = conn.perform_request_raw_response(http.request.get(path=CLICK_PATH))
    asserts.is_not_closed(conn.sock)
    asserts.image(resp1)
    asserts.image(resp2)
    asserts.no_header_value(resp1, 'connection', 'close')
    asserts.no_header_value(resp2, 'connection', 'close')


def test_click_keepalive_slow_backend(ctx):
    """
    BALANCER-1295
    Balancer must not wait for previous click backend response
    while processing next keepalive client request
    """
    ctx.start_backend(SimpleDelayedConfig(response=http.response.ok(data='A' * 10), response_delay=30))
    ctx.start_balancer(ClickConfig(keys=keys(ctx), backend_timeout=60))
    conn = ctx.create_http_connection()
    resp1 = conn.perform_request_raw_response(http.request.get(path=JSREDIR_PATH))
    resp2 = conn.perform_request_raw_response(http.request.get(path=JSREDIR_PATH))
    asserts.content(resp1, JSREDIR_DATA)
    asserts.content(resp2, JSREDIR_DATA)


def test_click_slow_backend_shutdown(ctx):
    """
    BALANCER-1295
    Balancer must not wait for click backend response on graceful shutdown
    """
    ctx.start_backend(SimpleDelayedConfig(response=http.response.ok(data='A' * 10), response_delay=30))
    ctx.start_balancer(ClickConfig(keys=keys(ctx), backend_timeout=60))
    conn = ctx.create_http_connection()
    conn.perform_request_raw_response(http.request.get(path=JSREDIR_PATH))
    stream = conn.create_stream()
    stream.write_request(http.request.get(path=JSREDIR_PATH).to_raw_request())

    event = threading.Event()
    event.clear()

    def run_request():
        event.set()
        ctx.graceful_shutdown(timeout='{}s'.format(10))

    thread = threading.Thread(target=run_request)
    thread.start()
    start_time = time.time()
    event.wait()
    thread.join()

    for run in Multirun(plan=[0.1] * 15):
        with run:
            assert not ctx.balancer.is_alive(), 'time taken: {}'.format(time.time() - start_time)
    ctx.balancer.set_finished()


def test_not_click(ctx):
    """
    Если запрос не может быть обработан кликовой библиотекой, то надо вернуть ответ backend-а
    """
    start_all(ctx)
    response = ctx.perform_request(http.request.get(path=NOT_CLICK_PATH))
    asserts.status(response, 200)
    asserts.content(response, DATA)


def test_not_click_backend_timeout(ctx):
    """
    Если запрос не может быть обработан кликовой библиотекой и backend таймаутится,
    то балансер должен закрыть соединение с клиентом
    """
    start_all(ctx, 8)
    err = ctx.perform_request_xfail(http.request.get(path=NOT_CLICK_PATH))
    asserts.content(err.raw_message, DATA[:10])


def test_not_click_backend_broken(ctx):
    """
    Если запрос не может быть обработан кликовой библиотекой и backend обрывает соединение,
    то балансер должен закрыть соединение с клиентом
    """
    data = '5\r\nabcde\r\n'
    ctx.start_backend(CloseConfig(response=http.response.raw_ok(headers={'Transfer-encoding': 'chunked'}, data=data)))
    ctx.start_balancer(ClickConfig(keys=keys(ctx)))
    err = ctx.perform_request_xfail(http.request.get(path=NOT_CLICK_PATH))
    asserts.raw_data(err.raw_message, data)


def test_redir_http10(ctx):
    """
    На запрос /redir по HTTP/1.0 балансер должен отдать ответ из библотеки с Content-Length
    и отправить запрос backend-у
    """
    response = base_click_test(ctx, http.request.get(path=REDIR_PATH, version='HTTP/1.0'))
    asserts.status(response, 302)
    asserts.header_value(response, 'Location', 'http://company.yandex.com/')
    asserts.is_content_length(response)


def test_safeclick_http10(ctx):
    """
    На запрос /safeclick по HTTP/1.0 балансер должен отдать ответ из библотеки с Content-Length
    и отправить запрос backend-у
    """
    response = base_click_test(ctx, http.request.get(path=SAFECLICK_PATH, version='HTTP/1.0'))
    asserts.status(response, 200)
    asserts.image(response)
    asserts.is_content_length(response)


def test_click_http10(ctx):
    """
    На запрос /click по HTTP/1.0 балансер должен отдать ответ из библотеки с Content-Length
    и отправить запрос backend-у
    """
    response = base_click_test(ctx, http.request.get(path=CLICK_PATH, version='HTTP/1.0'))
    asserts.status(response, 200)
    asserts.image(response)
    asserts.is_content_length(response)


def test_jclick_http10(ctx):
    """
    На запрос /jclick по HTTP/1.0 балансер должен отдать ответ из библиотеки с Content-Length
    и отправить запрос backend-у
    """
    response = base_click_test(ctx, http.request.get(path=JCLICK_PATH, version='HTTP/1.0'))
    asserts.status(response, 200)
    asserts.content(response, '/* counted */')
    asserts.is_content_length(response)


def test_not_click_http10(ctx):
    """
    Если запрос по HTTP/1.0 не может быть обработан кликовой библиотекой,
    то надо вернуть ответ backend-а
    """
    start_all(ctx)
    response = ctx.create_http_connection().perform_request_raw_response(
        http.request.get(path=NOT_CLICK_PATH, version='HTTP/1.0'))
    asserts.status(response, 200)
    asserts.content(response, DATA)


def test_click_keepalive_http10(ctx):
    """
    Проверка keepalive при ответе кликовой библиотекой
    """
    start_all(ctx)
    conn = ctx.create_http_connection()
    resp1 = conn.perform_request_raw_response(
        http.request.get(path=CLICK_PATH, headers={'Connection': 'Keep-alive'}, version='HTTP/1.0'))
    resp2 = conn.perform_request_raw_response(
        http.request.get(path=CLICK_PATH, headers={'Connection': 'Keep-alive'}, version='HTTP/1.0'))
    asserts.image(resp1)
    asserts.image(resp2)
    print resp1
    print resp2
    asserts.header_value(resp1, 'Connection', 'Keep-Alive')
    asserts.header_value(resp2, 'Connection', 'Keep-Alive')
    asserts.no_header_value(resp1, 'Connection', 'close')
    asserts.no_header_value(resp2, 'Connection', 'close')


def test_404_lib_answer(ctx):
    """
    Запрос, на который кликовая бибилотека отвечает 404 Connection: close
    должен быть с валидным ответом
    """
    start_all(ctx)
    conn = ctx.create_http_connection()
    resp = conn.perform_request_raw_response(http.request.get(path=CLICK_LIB_404_PATH, headers=CLICK_LIB_404_HEADERS))
    asserts.status(resp, 404)
    asserts.header(resp, 'Content-Length')


def test_click_logging(ctx):
    """
    BALANCER-138
    Балансер должен логировать, удалось ли кликовой библиотеке обработать
    запрос, или пришлось передавать возможность ответа в подмодуль.
    Неуспех - 0, успех - статус кода ответа в кликовой библиотеке.
    """
    response = base_click_test(ctx, http.request.get(path=REDIR_PATH))
    asserts.status(response, 302)
    time.sleep(1)
    accesslog = ctx.manager.fs.read_file(ctx.balancer.config.accesslog)
    assert '[click click_lib_answer 302' in accesslog

    response = ctx.perform_request(http.request.get(path=NOT_CLICK_PATH))
    asserts.status(response, 200)
    time.sleep(1)
    accesslog = ctx.manager.fs.read_file(ctx.balancer.config.accesslog)
    assert '[click click_lib_answer 0' in accesslog


def test_click_file_switch(ctx):
    """
    BALANCER-711
    При наличии файла file_switch балансер не должен пытаться использовать
    кликовую библиотеку, а сразу идти в бэкэнд
    """
    file_switch = ctx.manager.fs.create_file('file_switch')
    response = base_click_test(ctx, http.request.get(path=REDIR_PATH), file_switch=file_switch)
    asserts.status(response, 200)
    for run in Multirun():
        with run:
            accesslog = ctx.manager.fs.read_file(ctx.balancer.config.accesslog)
            assert '[click click_lib_disabled' in accesslog

    ctx.manager.fs.remove(file_switch)
    time.sleep(3)

    response = base_click_test(ctx, http.request.get(path=REDIR_PATH), file_switch=file_switch)
    asserts.status(response, 302)
    for run in Multirun():
        with run:
            accesslog = ctx.manager.fs.read_file(ctx.balancer.config.accesslog)
            assert '[click click_lib_answer 302' in accesslog


def test_post_passed(ctx):
    """
    BALANCER-808
    If there is request with body, it must be passed to submodule as is
    """
    response_body = '/* counted */'
    start_all(ctx, response_data=list(response_body))
    path = '/jclck/'
    headers = {'Content-Type': 'application/x-www-form-urlencoded'}
    data = '/dtype=bebr/pid=30/session_id=1466005868244_641658/user_timestamp=1466005932043/events=exp_config_version=5003/*'
    request = http.request.post(
        path=path,
        headers=headers,
        data=data,
    )
    response = ctx.perform_request(request)
    asserts.status(response, 200)
    asserts.content(response, response_body)

    time.sleep(1)
    accesslog = ctx.manager.fs.read_file(ctx.balancer.config.accesslog)
    assert '[click click_lib_answer' not in accesslog


def test_proxy_true(ctx):
    """
    BALANCER-933
    If Proxy == true in decoded url, balancer should return answer from submodule
    """
    start_all(ctx)
    response = ctx.perform_request(http.request.get(path=JSREDIR_PROXY_TRUE))

    asserts.status(response, 200)
    asserts.content(response, DATA)
    for run in Multirun():
        with run:
            accesslog = ctx.manager.fs.read_file(ctx.balancer.config.accesslog)
            assert '[click click_lib_proxy' in accesslog


def test_proxy_false(ctx):
    """
    BALANCER-933
    If Proxy == false in decoded url, balancer should return answer from click library and pass request to submodule
    """
    start_all(ctx)
    response = ctx.perform_request(http.request.get(path=JSREDIR_PROXY_FALSE))

    asserts.status(response, 200)
    assert '<html><head><meta name="referrer" content="always"/>' in response.data.content
    for run in Multirun():
        with run:
            accesslog = ctx.manager.fs.read_file(ctx.balancer.config.accesslog)
            assert '[click click_lib_answer 200' in accesslog
