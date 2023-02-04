# -*- coding: utf-8 -*-
import pytest

import balancer.test.plugin.context as mod_ctx

from configs import ExpGetterConfig
from balancer.test.util.predef import http
from balancer.test.util.predef.handler.server.http import SimpleConfig, SimpleDelayedConfig, ChunkedConfig
from balancer.test.util.balancer import asserts
from balancer.test.util.sync import CheckersWatcher


class UaasContext(object):
    def __init__(self):
        super(UaasContext, self).__init__()
        self.__file_switch = self.manager.fs.create_file('file_switch')
        self.manager.fs.remove(self.file_switch)

    @property
    def file_switch(self):
        return self.__file_switch

    def start_uaas_backend(self, config):
        return self.start_backend(config, name='uaas_backend')

    def start_module_backend(self, config):
        return self.start_backend(config)

    def start_all_backends(self, uaas_response=None, response=None):
        self.start_uaas_backend(SimpleConfig(response=uaas_response))
        self.start_module_backend(SimpleConfig(response=response))

    def start_uaas_balancer(self, **balancer_kwargs):
        if hasattr(self, 'uaas_backend'):
            uaas_backend_port = self.uaas_backend.server_config.port
        else:
            uaas_backend_port = self.manager.port.get_port()

        if hasattr(self, 'backend'):
            backend_port = self.backend.server_config.port
        else:
            backend_port = self.manager.port.get_port()

        return self.start_balancer(ExpGetterConfig(
            uaas_backend_port, backend_port, self.file_switch, **balancer_kwargs))

    def start_all(self, uaas_response=None, response=None, **balancer_kwargs):
        self.start_all_backends(uaas_response, response)
        return self.start_uaas_balancer(**balancer_kwargs)

    def start_no_uaas(self, **balancer_kwargs):
        self.start_module_backend(SimpleConfig())
        return self.start_uaas_balancer(**balancer_kwargs)


uaas_ctx = mod_ctx.create_fixture(UaasContext)


def test_uaas_request(uaas_ctx):
    """
    Балансер должен отправить в uaas стартовую строку и заголовки запроса клиента
    """
    uaas_ctx.start_all()
    path = '/pink_floyd'
    headers = {'led': 'zeppelin'}
    uaas_ctx.perform_request(http.request.get(path=path, headers=headers))
    uaas_req = uaas_ctx.uaas_backend.state.get_request()

    asserts.path(uaas_req, path)
    asserts.headers_values(uaas_req, headers)


@pytest.mark.parametrize(
    'data',
    ['aerosmith', ['led', 'zeppelin']],
    ids=['length', 'chunked']
)
def test_uaas_request_cut_body(uaas_ctx, data):
    """
    Если клиентский запрос содержит тело,
    то балансер должен отправить в uaas запрос без тела
    и с исправленными заголовками content-length и transfer-encoding.
    """
    uaas_ctx.start_all()
    uaas_ctx.perform_request(http.request.get(data=data))
    uaas_req = uaas_ctx.uaas_backend.state.get_request()

    asserts.empty_content(uaas_req)


def base_uaas_headers_test(uaas_ctx, headers, expected_headers, no_headers=None, **balancer_kwargs):
    uaas_ctx.start_all(uaas_response=http.response.ok(headers=headers), **balancer_kwargs)
    uaas_ctx.perform_request(http.request.get())
    req = uaas_ctx.backend.state.get_request()

    asserts.headers_values(req, expected_headers)
    if no_headers:
        asserts.no_headers(req, no_headers)


def test_add_uaas_headers(uaas_ctx):
    """
    Балансер должен добавлять заголовки экспериментов,
    которые вернул uaas-бэкенд, в запрос бэкенду.
    """
    headers = {
        'X-Yandex-ExpConfigVersion': 'led',
        'X-Yandex-ExpBoxes': 'zeppelin',
        'X-Yandex-ExpFlags': 'pink',
        'X-Yandex-ExpConfigVersion-Pre': 'floyd',
        'X-Yandex-ExpBoxes-Pre': 'van',
        'X-Yandex-ExpFlags-Pre': 'halen',
    }
    base_uaas_headers_test(uaas_ctx, headers, headers)


def test_add_staff_login_header(uaas_ctx):
    """
    Балансер должен добавлять заголовок стафф-логина,
    который вернул uaas-бэкенд, в запрос бэкенду.
    """
    headers = {
        'X-Yandex-ExpConfigVersion': 'led',
        'X-Yandex-ExpBoxes': 'zeppelin',
        'X-Yandex-ExpFlags': 'pink',
        'X-Yandex-ExpConfigVersion-Pre': 'floyd',
        'X-Yandex-ExpBoxes-Pre': 'van',
        'X-Yandex-ExpFlags-Pre': 'halen',
        'X-Yandex-Is-Staff-Login': '1',
    }
    base_uaas_headers_test(uaas_ctx, headers, headers)


def test_add_uaas_headers_errordocument(uaas_ctx):
    """
    BALANCER-1024
    Балансер должен добавлять заголовки экспериментов,
    которые вернул uaas-бэкенд, в запрос бэкенду.
    Если uaas не proxy, всё равно должны получить ответ
    """
    headers = {
        'X-Yandex-ExpConfigVersion': 'led',
        'X-Yandex-ExpBoxes': 'zeppelin',
        'X-Yandex-ExpFlags': 'pink',
        'X-Yandex-ExpConfigVersion-Pre': 'floyd',
        'X-Yandex-ExpBoxes-Pre': 'van',
        'X-Yandex-ExpFlags-Pre': 'halen',
    }
    uaas_ctx.start_all(uaas_response=http.response.ok(headers=headers), errordocument_in_uaas=True, remain_headers='X-Yandex-Exp.*')
    uaas_ctx.perform_request(http.request.get(headers=headers))
    req = uaas_ctx.backend.state.get_request()

    asserts.headers_values(req, headers)


def test_not_all_uaas_headers(uaas_ctx):
    """
    Если ответ uaas-бэкенда содержит не все заголовки экспериментов,
    то балансер не должен сам добавлять оставшиеся заголовки.
    """
    headers = {
        'X-Yandex-ExpConfigVersion': 'led',
        'X-Yandex-ExpBoxes': 'zeppelin',
        'X-Yandex-ExpBoxes-Pre': 'van',
        'X-Yandex-ExpFlags-Pre': 'halen',
    }
    base_uaas_headers_test(uaas_ctx, headers, headers,
                           ['X-Yandex-ExpFlags', 'X-Yandex-ExpConfigVersion-Pre'])


def test_uaas_unexpected_headers(uaas_ctx):
    """
    Если ответ uaas-бэкенда содержит заголовки,
    которые не являются заголовками экспериментов,
    то балансер не должен добавлять их в запрос бэкенду.
    """
    headers = {
        'X-Yandex-ExpUnexpected': 'led',
        'X-Unknown-Header': 'zeppelin',
    }
    base_uaas_headers_test(uaas_ctx, headers, {}, headers.keys())


def test_uaas_exp_headers(uaas_ctx):
    """
    Если в exp_getter указан exp_headers, то скопировать
    нужно именно эти заголовки из ответа uaas
    """
    expected_headers = {
        'Black': 'Iron',
        'Sabbath': 'Maiden',
    }
    no_headers = {
        'X-Yandex-ExpConfigVersion': 'led',
        'X-Yandex-ExpBoxes': 'zeppelin',
    }

    headers = {}
    headers.update(no_headers)
    headers.update(expected_headers)

    base_uaas_headers_test(uaas_ctx, headers, expected_headers, no_headers=no_headers, exp_headers='Black|Sabbath')


def test_pass_client_request(uaas_ctx):
    """
    Балансер должен прокидывать запрос клиента бэкенду.
    """
    path = '/pink_floyd'
    headers = {'led': 'zeppelin'}
    data = ['van', 'halen']
    uaas_ctx.start_all()
    uaas_ctx.perform_request(http.request.get(path=path, headers=headers, data=data))
    req = uaas_ctx.backend.state.get_request()

    asserts.path(req, path)
    asserts.headers_values(req, headers)
    asserts.content(req, ''.join(data))


def test_return_backend_response(uaas_ctx):
    """
    Балансер должен передать ответ бэкенда клиенту.
    """
    headers = {'pink': 'floyd', 'led': 'zeppelin'}
    data = ['van', 'halen']
    uaas_ctx.start_all(response=http.response.ok(headers=headers, data=data))
    resp = uaas_ctx.perform_request(http.request.get())

    asserts.status(resp, 200)
    asserts.headers_values(resp, headers)
    asserts.content(resp, ''.join(data))


def test_not_pass_uaas_headers_to_client(uaas_ctx):
    """
    Балансер не должен добавлять заголовки ответа uaas-бэкенда в ответ клиенту.
    """
    headers = {
        'X-Yandex-ExpConfigVersion': '42',
        'pink': 'floyd',
        'led': 'zeppelin',
    }
    uaas_ctx.start_all(uaas_response=http.response.ok(headers=headers))
    resp = uaas_ctx.perform_request(http.request.get())

    asserts.no_headers(resp, headers.keys())


@pytest.mark.parametrize(
    'uaas_data',
    ['led zeppelin', ['led', 'zeppelin']],
    ids=['length', 'chunked']
)
def test_ignore_uaas_body(uaas_ctx, uaas_data):
    """
    Балансер должен игнорировать тело ответа uaas-бэкенда
    """
    req_data = ['pink', 'floyd']
    resp_data = ['van', 'halen']
    uaas_ctx.start_all(uaas_response=http.response.ok(data=uaas_data),
                       response=http.response.ok(data=resp_data))
    resp = uaas_ctx.perform_request(http.request.get(data=req_data))
    req = uaas_ctx.backend.state.get_request()

    asserts.content(resp, ''.join(resp_data))
    asserts.content(req, ''.join(req_data))


def test_no_uaas_backend(uaas_ctx):
    """
    Если uaas-бэкенд не отвечает, то балансер должен отправить запрос бэкенду и вернуть ответ клиенту.
    """
    path = '/led'
    uaas_ctx.start_no_uaas()
    resp = uaas_ctx.perform_request(http.request.get(path=path))
    req = uaas_ctx.backend.state.get_request()

    asserts.path(req, path)
    asserts.status(resp, 200)


def test_uaas_backend_timeout(uaas_ctx):
    """
    Если uaas-бэкенд таймаутится, то балансер должен отправить запрос бэкенду и вернуть ответ клиенту.
    """
    timeout = 1
    path = '/led'
    uaas_ctx.start_uaas_backend(SimpleDelayedConfig(response_delay=timeout + 1))
    uaas_ctx.start_no_uaas(uaas_backend_timeout=timeout)
    resp = uaas_ctx.perform_request(http.request.get(path=path))
    req = uaas_ctx.backend.state.get_request()

    asserts.path(req, path)
    asserts.status(resp, 200)


def test_uaas_backend_body_timeout(uaas_ctx):
    """
    Если uaas-бэкенд отправил стартовую строку и заголовки, но затаймаутился при отправке тела,
    то балансер не должен добавлять заголовки из его ответа к запросу в бэкенд.
    """
    timeout = 1
    path = '/led'
    headers = {'X-Yandex-ExpConfigVersion': '42'}
    uaas_ctx.start_uaas_backend(ChunkedConfig(
        response=http.response.ok(
            data=['pink', 'floyd'],
            headers=headers
        ),
        chunk_timeout=timeout + 1))
    uaas_ctx.start_no_uaas(uaas_backend_timeout=timeout)
    resp = uaas_ctx.perform_request(http.request.get(path=path))
    req = uaas_ctx.backend.state.get_request()

    asserts.path(req, path)
    asserts.no_headers(req, headers.keys())
    asserts.status(resp, 200)


def test_not_trusted(uaas_ctx):
    """
    Если trusted = false то балансер должен удалить из запроса клиента uaas-заголовки.
    """
    req_headers = {'X-Yandex-ExpConfigVersion': '42', 'X-Yandex-ExpBoxes': 'led'}
    uaas_headers = {'X-Yandex-ExpBoxes': 'pink', 'X-Yandex-ExpFlags': 'floyd'}
    uaas_ctx.start_all(uaas_response=http.response.ok(headers=uaas_headers), trusted=False)
    uaas_ctx.perform_request(http.request.get(headers=req_headers))
    uaas_req = uaas_ctx.uaas_backend.state.get_request()
    req = uaas_ctx.backend.state.get_request()

    asserts.no_headers(uaas_req, req_headers.keys())
    asserts.no_header(req, 'X-Yandex-ExpConfigVersion')
    asserts.no_header_value(req, 'X-Yandex-ExpBoxes', 'led')
    asserts.headers_values(req, uaas_headers)


def test_trusted_with_uaas_headers(uaas_ctx):
    """
    Если trusted = true и запрос клиента содержит uaas-заголовки,
    то балансер не должен ходить в uaas-бэкенд, а сразу отправить запрос бэкенду.
    """
    headers = {'X-Yandex-ExpConfigVersion': '42'}
    uaas_ctx.start_all(trusted=True)
    uaas_ctx.perform_request(http.request.get(headers=headers))
    req = uaas_ctx.backend.state.get_request()

    assert uaas_ctx.uaas_backend.state.requests.empty()
    asserts.headers_values(req, headers)


def test_trusted_no_uaas_headers(uaas_ctx):
    """
    Если trusted = true но запрос клиента не содержит uaas-заголовков,
    то балансер должен отправить запрос в uaas-бэкенд и uaas-заголовки из его ответа
    добавить в запрос бэкенду.
    """
    path = '/led'
    headers = {'X-Yandex-ExpConfigVersion': '42'}
    uaas_ctx.start_all(uaas_response=http.response.ok(headers=headers), trusted=True)
    uaas_ctx.perform_request(http.request.get(path=path))
    uaas_req = uaas_ctx.uaas_backend.state.get_request()
    req = uaas_ctx.backend.state.get_request()

    asserts.path(uaas_req, path)
    asserts.path(req, path)
    asserts.headers_values(req, headers)


def test_file_switch_disable_uaas(uaas_ctx):
    """
    При появлении файла file_switch балансер должен перестать ходить в uaas.
    """
    watcher = CheckersWatcher(uaas_ctx, uaas_ctx.file_switch)
    uaas_ctx.start_all()
    watcher.wait_checker(is_exists=False)
    uaas_ctx.manager.fs.rewrite(uaas_ctx.file_switch, '')
    watcher.wait_checker(is_exists=True)
    uaas_ctx.perform_request(http.request.get())

    assert uaas_ctx.uaas_backend.state.requests.empty()


def test_file_switch_disable_uaas_on_start(uaas_ctx):
    """
    Если при старте балансер есть файл file_switch, то балансер не должен ходить в uaas.
    """
    uaas_ctx.manager.fs.rewrite(uaas_ctx.file_switch, '')
    watcher = CheckersWatcher(uaas_ctx, uaas_ctx.file_switch)
    uaas_ctx.start_all()
    watcher.wait_checker(is_exists=True)
    uaas_ctx.perform_request(http.request.get())

    assert uaas_ctx.uaas_backend.state.requests.empty()


def test_file_switch_enable_uaas(uaas_ctx):
    """
    При исчезновении файла file_switch балансер должен начать ходить в uaas.
    """
    path = '/led'
    headers = {'X-Yandex-ExpConfigVersion': '42'}
    uaas_ctx.manager.fs.rewrite(uaas_ctx.file_switch, '')
    watcher = CheckersWatcher(uaas_ctx, uaas_ctx.file_switch)
    uaas_ctx.start_all(uaas_response=http.response.ok(headers=headers))
    watcher.wait_checker(is_exists=True)

    uaas_ctx.manager.fs.remove(uaas_ctx.file_switch)
    watcher.wait_checker(is_exists=False)

    uaas_ctx.perform_request(http.request.get(path=path))
    uaas_req = uaas_ctx.uaas_backend.state.get_request()
    req = uaas_ctx.backend.state.get_request()

    asserts.path(uaas_req, path)
    asserts.path(req, path)
    asserts.headers(req, headers)


def test_service_name_sent_to_uaas(uaas_ctx):
    """
    Если заданы service_name и service_name_header, то в запросе к uaas
    должен присутствовать заголовок service_name_header: service_name
    """
    service_name_header = 'X-ExpService'
    service_name = 'Scorpions'
    uaas_ctx.start_all(service_name_header=service_name_header, service_name=service_name)

    uaas_ctx.perform_request(http.request.get())

    uaas_req = uaas_ctx.uaas_backend.state.get_request()
    asserts.header_value(uaas_req, service_name_header, service_name)

    backend_req = uaas_ctx.backend.state.get_request()
    asserts.no_header(backend_req, service_name_header)


def test_service_name_sent_to_backend(uaas_ctx):
    service_name_to_backend_header = 'X-Yandex-ExpServiceName'
    service_name = 'Scorpions'
    uaas_ctx.start_all(
        service_name_header='X-ExpService',
        service_name_to_backend_header=service_name_to_backend_header,
        service_name=service_name
    )

    uaas_ctx.perform_request(http.request.get())

    backend_req = uaas_ctx.backend.state.get_request()
    asserts.header_value(backend_req, service_name_to_backend_header, service_name)


def test_service_name_sent_to_uaas_with_client_headers(uaas_ctx):
    """
    Если заданы service_name и service_name_header, то в запросе к uaas
    должен присутствовать заголовок service_name_header: service_name.
    Если Такой заголовок пришёл от клиента, то в запросе к uaas должны быть
    заголовки, указанные в конфиге, а в основной бэкэнд - пользовательские.
    """
    service_name_header = 'X-ExpService'
    service_name = 'Scorpions'
    user_service_names = ['Rammstein', 'Annihilator']
    user_headers = {service_name_header: value for value in user_service_names}
    uaas_ctx.start_all(service_name_header=service_name_header, service_name=service_name)

    uaas_ctx.perform_request(http.request.get(headers=user_headers))

    uaas_req = uaas_ctx.uaas_backend.state.get_request()
    asserts.header_value(uaas_req, service_name_header, service_name)
    for k, v in user_headers.iteritems():
        asserts.no_header_value(uaas_req, k, v)

    backend_req = uaas_ctx.backend.state.get_request()
    asserts.headers_values(backend_req, user_headers)


@pytest.mark.parametrize(
    'method',
    [
        'GET',
        'POST',
        'HEAD',
        'PUT',
        'DELETE',
        'PATCH',
        'CONNECT',
        'OPTIONS'
    ]
)
def test_check_allowed_methods(uaas_ctx, method):
    """
    exp_getter не посылает запрос в uaas если используется метод OPTIONS/CONNECT
    USEREXP-7668
    """
    uaas_ctx.start_all()

    uaas_req = uaas_ctx.perform_request(http.request.custom(method))

    if method not in ['OPTIONS', 'CONNECT']:
        uaas_req = uaas_ctx.uaas_backend.state.get_request()
        asserts.method(uaas_req, method)
    else:
        assert uaas_ctx.uaas_backend.state.requests.empty()

    assert not uaas_ctx.backend.state.requests.empty()


def test_processing_time_header(uaas_ctx):
    uaas_ctx.start_uaas_backend(SimpleDelayedConfig(response_delay=2))
    uaas_ctx.start_module_backend(SimpleConfig())
    uaas_ctx.start_uaas_balancer()

    uaas_ctx.perform_request(http.request.get())
    request = uaas_ctx.backend.state.get_request()
    processing_time_headers = request.headers.get_all('X-Yandex-Balancer-ExpGetter-ProcessingTime')
    assert len(processing_time_headers) == 1
    assert int(processing_time_headers[0]) >= 2 * 1000 * 1000


@pytest.mark.parametrize('limit', [1, None], ids=['limited', 'not limited'])
@pytest.mark.parametrize('service_name', ['Scorpions', None])
def test_limited_headers(uaas_ctx, limit, service_name):
    """
    Если заголовки, подставленные из uaas, превышают лимит,
    должен увеличиться счетчик limited_headers
    """
    if service_name:
        service_name_header = 'X-ExpService'
    else:
        service_name_header = None
    uaas_ctx.start_all(service_name_header=service_name_header, service_name=service_name, headers_size_limit=limit)

    path = '/path/subpath?x=y'
    uaas_ctx.perform_request(http.request.get(path=path))

    unistat = uaas_ctx.get_unistat()
    if service_name:
        signal = 'exp-service-' + service_name + '-limited_headers_summ'
    else:
        signal = 'exp-default-limited_headers_summ'
    total_signal = 'exp-total-limited_headers_summ'
    if limit:
        assert unistat[signal] == 1
        assert unistat[total_signal] == 1
    else:
        assert unistat[signal] == 0
        assert unistat[total_signal] == 0
