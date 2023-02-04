# -*- coding: utf-8 -*-
"""
BALANCER-246
"""
import time
import pytest

import balancer.test.plugin.context as mod_ctx

from configs import GeobaseConfig, GeobaseWithFallbackConfig
from balancer.test.util.predef import http
from balancer.test.util.predef.handler.server.http import SimpleConfig, SimpleDelayedConfig
from balancer.test.util.balancer import asserts
from balancer.test.util.is_gdpr_b import parse_is_gdpr_b


class GeoContext(object):
    TAKE_IP_FROM = 'My-Ip'
    LAAS_ANSWER = 'X-LaaS-Answered'
    FORWARDED_FOR = 'X-Forwarded-For-Y'

    def __init__(self):
        super(GeoContext, self).__init__()
        self.unistat_port = self.manager.port.get_port()

    def start_geo_backend(self, config):
        return self.start_backend(config, name='geo_backend')

    def start_module_backend(self, config):
        return self.start_backend(config)

    def start_all_backends(self, geo_response=None):
        self.start_geo_backend(SimpleConfig(response=geo_response))
        self.start_module_backend(SimpleConfig())

    def start_geo_balancer(self, **balancer_kwargs):
        if hasattr(self, 'geo_backend'):
            geo_backend_port = self.geo_backend.server_config.port
        else:
            geo_backend_port = self.manager.port.get_port()

        if hasattr(self, 'backend'):
            backend_port = self.backend.server_config.port
        else:
            backend_port = self.manager.port.get_port()

        if 'take_ip_from' not in balancer_kwargs:
            balancer_kwargs['take_ip_from'] = self.TAKE_IP_FROM
        if 'laas_answer_header' not in balancer_kwargs:
            balancer_kwargs['laas_answer_header'] = self.LAAS_ANSWER
        return self.start_balancer(GeobaseConfig(geo_backend_port, backend_port, self.unistat_port, **balancer_kwargs))

    def start_all(self, geo_response=None, **balancer_kwargs):
        self.start_all_backends(geo_response)
        return self.start_geo_balancer(**balancer_kwargs)

    def start_no_geo(self, **balancer_kwargs):
        self.start_module_backend(SimpleConfig())
        return self.start_geo_balancer(**balancer_kwargs)


geo_ctx = mod_ctx.create_fixture(GeoContext)


def base_url_prefix_test(geo_ctx, path, geo_path):
    geo_ctx.start_all()
    host = 'yandex.ru'
    headers = {'Host': host, geo_ctx.TAKE_IP_FROM: '8.8.8.8'}
    geo_ctx.perform_request(http.request.get(path=path, headers=headers))
    geo_request = geo_ctx.geo_backend.state.get_request()
    request = geo_ctx.backend.state.get_request()

    asserts.header_value(geo_request, 'x-url-prefix', 'http://' + host + geo_path)
    asserts.no_header(request, 'x-url-prefix')


def test_url_prefix(geo_ctx):
    """
    Клиент задает запрос.
    Балансер должен отправить geo-бэкенду в заголовке x-url-prefix значение host/path
    """
    base_url_prefix_test(geo_ctx, '/yandsearch', '/yandsearch')


def test_url_prefix_cgi(geo_ctx):
    """
    Клиент задает запрос с cgi-параметрами.
    Балансер должен отправить geo-бэкенду в заголовке x-url-prefix значение host/path,
    с отрезанными cgi-параметрами.
    """
    base_url_prefix_test(geo_ctx, '/yandsearch?text=test', '/yandsearch')


def test_url_prefix_subpath(geo_ctx):
    """
    BALANCER-881
    В запросе к geo-бэкенду в заголовке x-url-prefix должен быть весь path запроса
    """
    base_url_prefix_test(geo_ctx, '/led/zeppelin/yandsearch?text=metallica', '/led/zeppelin/yandsearch')


def test_url_prefix_no_host_default(geo_ctx):
    """
    Если в запросе отсутствует заголовок Host, то в x-url-prefix надо использовать yabs.yandex.ru.
    """
    geo_ctx.start_all()
    path = '/yandsearch'
    geo_ctx.perform_request(http.request.get(path=path, headers={geo_ctx.TAKE_IP_FROM: '8.8.8.8'}))
    geo_request = geo_ctx.geo_backend.state.get_request()
    request = geo_ctx.backend.state.get_request()

    asserts.header_value(geo_request, 'x-url-prefix', 'http://' + 'yabs.yandex.ru' + path)
    asserts.no_header(request, 'x-url-prefix')


def test_url_prefix_no_host_geo_host(geo_ctx):
    """
    Если в запросе отсутствует заголовок Host, то в x-url-prefix надо использовать значение, указанное в geo_host.
    """
    geo_host = 'led.yandex.ru'
    geo_ctx.start_all(geo_host=geo_host)
    path = '/yandsearch'
    geo_ctx.perform_request(http.request.get(path=path, headers={geo_ctx.TAKE_IP_FROM: '8.8.8.8'}))
    geo_request = geo_ctx.geo_backend.state.get_request()
    request = geo_ctx.backend.state.get_request()

    asserts.header_value(geo_request, 'x-url-prefix', 'http://' + geo_host + path)
    asserts.no_header(request, 'x-url-prefix')


def test_url_prefix_in_request(geo_ctx):
    """
    Если в запросе есть заголовок x-url-prefix, то его не нужно отправлять geo-бэкенду,
    но надо отправить сервисному бэкенду.
    """
    host = 'yandex.ru'
    path = '/yandsearch'
    url_prefix = 'x-url-prefix'
    url_prefix_value = 'led'
    geo_ctx.start_all()
    geo_ctx.perform_request(http.request.get(
        path=path,
        headers={
            'Host': host,
            geo_ctx.TAKE_IP_FROM: '8.8.8.8',
            url_prefix: url_prefix_value
        })
    )
    geo_request = geo_ctx.geo_backend.state.get_request()
    request = geo_ctx.backend.state.get_request()

    asserts.no_header_value(geo_request, url_prefix, url_prefix_value)
    asserts.header_value(geo_request, url_prefix, 'http://' + host + path)
    asserts.header_value(request, url_prefix, url_prefix_value)


def test_geo_host_header_default(geo_ctx):
    """
    В запросе geo-бэкенду значение заголовка Host надо заменить на yabs.yandex.ru.
    """
    geo_ctx.start_all()
    geo_ctx.perform_request(http.request.get(headers={'Host': 'yandex.ru', geo_ctx.TAKE_IP_FROM: '8.8.8.8'}))
    geo_request = geo_ctx.geo_backend.state.get_request()

    asserts.header_value(geo_request, 'host', 'yabs.yandex.ru')


def test_geo_host_header(geo_ctx):
    """
    В запросе geo-бэкенду значение заголовка Host надо заменить на значение, указанное в geo_host.
    """
    geo_host = 'led.yandex.ru'
    geo_ctx.start_all(geo_host=geo_host)
    geo_ctx.perform_request(http.request.get(headers={'Host': 'yandex.ru', geo_ctx.TAKE_IP_FROM: '8.8.8.8'}))
    geo_request = geo_ctx.geo_backend.state.get_request()
    asserts.header_value(geo_request, 'host', geo_host)


def test_geo_host_header_no_host_default(geo_ctx):
    """
    Если в клиентском запросе не было заголовка Host,
    то в запросе к geo-бэкенду балансер должен добавить заголовок Host: yabs.yandex.ru.
    """
    geo_ctx.start_all()
    geo_ctx.perform_request(http.request.get(headers={geo_ctx.TAKE_IP_FROM: '8.8.8.8'}))
    geo_request = geo_ctx.geo_backend.state.get_request()
    asserts.header_value(geo_request, 'host', 'yabs.yandex.ru')


def test_geo_host_header_no_host(geo_ctx):
    """
    Если в клиентском запросе не было заголовка Host,
    то в запросе к geo-бэкенду балансер должен добавить заголовок Host со значением, указанным в geo_host.
    """
    geo_host = 'led.yandex.ru'
    geo_ctx.start_all(geo_host=geo_host)
    geo_ctx.perform_request(http.request.get(headers={geo_ctx.TAKE_IP_FROM: '8.8.8.8'}))
    geo_request = geo_ctx.geo_backend.state.get_request()
    asserts.header_value(geo_request, 'host', geo_host)


def test_non_geo_host_header(geo_ctx):
    """
    Сервисному бэкенду должен отправляться заголовок Host из клиентского запроса.
    """
    geo_ctx.start_all()
    geo_ctx.perform_request(http.request.get(headers={'Host': 'yandex.ru', geo_ctx.TAKE_IP_FROM: '8.8.8.8'}))
    request = geo_ctx.backend.state.get_request()
    asserts.header_value(request, 'host', 'yandex.ru')


def test_client_headers_geo_backend(geo_ctx):
    """
    Балансер должен передать geo-бэкенду все клиентские заголовки, кроме Host.
    """
    headers = {
        'Led': 'Zeppelin',
        'Pink': 'Floyd',
        geo_ctx.TAKE_IP_FROM: '8.8.8.8',
    }
    geo_ctx.start_all()
    geo_ctx.perform_request(http.request.get(headers=headers))
    geo_request = geo_ctx.geo_backend.state.get_request()
    asserts.headers_values(geo_request, headers)


def test_client_headers_non_geo(geo_ctx):
    """
    Балансер должен передать бэкенду все клиентские заголовки.
    """
    headers = {
        'Led': 'Zeppelin',
        'Pink': 'Floyd',
        geo_ctx.TAKE_IP_FROM: '8.8.8.8',
    }
    geo_ctx.start_all()
    geo_ctx.perform_request(http.request.get(headers=headers))
    request = geo_ctx.backend.state.get_request()
    asserts.headers_values(request, headers)


def test_x_region(geo_ctx):
    """
    Балансер должен отправить все заголовки X-Region-* из ответа geo-бэкенда сервисному бэкенду.
    """
    headers = {
        'X-Region-Location': '53.9011, 27.5588, 300, 1429559492',
        'X-Region-City': '123',
        'X-Region-Is-User-Choice': '1',
    }
    geo_ctx.start_all(geo_response=http.response.ok(headers=headers))
    geo_ctx.perform_request(http.request.get(headers={geo_ctx.TAKE_IP_FROM: '8.8.8.8'}))
    request = geo_ctx.backend.state.get_request()
    asserts.headers_values(request, headers)


def test_x_is_tourist(geo_ctx):
    """
    Балансер должен отправить заголовок X-Is-Tourist из ответа geo-бэкенда сервисному бэкенду.
    """
    headers = {
        'X-Is-Tourist': '1',
    }
    geo_ctx.start_all(geo_response=http.response.ok(headers=headers))
    geo_ctx.perform_request(http.request.get(headers={geo_ctx.TAKE_IP_FROM: '8.8.8.8'}))
    request = geo_ctx.backend.state.get_request()
    asserts.headers_values(request, headers)


def test_x_ip_properties(geo_ctx):
    """
    Балансер должен отправить заголовок X-IP-Properties из ответа geo-бэкенда сервисному бэкенду.
    """
    headers = {
        'X-IP-Properties': 'CgNNVFMQ4QE',
    }
    geo_ctx.start_all(geo_response=http.response.ok(headers=headers))
    geo_ctx.perform_request(http.request.get(headers={geo_ctx.TAKE_IP_FROM: '8.8.8.8'}))
    request = geo_ctx.backend.state.get_request()
    asserts.headers_values(request, headers)


def test_x_ip_properties_country(geo_ctx):
    """
    BALANCER-3064
    """
    headers = {
        'X-IP-Properties': 'EOEBGAAgACgAMAA4AEAASABQAFgA',
        'X-IP-Properties-Country': '225',
    }
    geo_ctx.start_all(
        geo_response=http.response.ok(headers=headers),
    )
    geo_ctx.perform_request(http.request.get(headers={geo_ctx.TAKE_IP_FROM: '8.8.8.8'}))
    request = geo_ctx.backend.state.get_request()
    asserts.headers_values(request, headers)


def test_x_region_dummy(geo_ctx):
    """
    BALANCER-1025
    Балансер должен отправить все заголовки X-Region-* из ответа geo-бэкенда сервисному бэкенду.
    В тои числе, если geo бэкэнд не proxy, а errordocument
    """
    headers = {
        'X-Region-Id': '1234',
    }
    geo_ctx.start_all(geo_response=http.response.ok(headers=headers), dummy_in_geo=1, dummy_region_id='1234')
    geo_ctx.perform_request(http.request.get(headers={geo_ctx.TAKE_IP_FROM: '8.8.8.8'}))
    request = geo_ctx.backend.state.get_request()
    asserts.headers_values(request, headers)


def test_non_geo_client_body(geo_ctx):
    """
    Сервисному бэкенду должно отправляться тело клиентского запроса.
    """
    client_data = 'Led Zeppelin'
    geo_data = 'Pink Floyd'
    geo_ctx.start_all(geo_response=http.response.ok(data=geo_data))
    geo_ctx.perform_request(http.request.get(data=client_data, headers={geo_ctx.TAKE_IP_FROM: '8.8.8.8'}))
    request = geo_ctx.backend.state.get_request()
    asserts.content(request, client_data)


def test_x_region_from_client(geo_ctx):
    """
    Если запрос клиента содержит заголовки X-Region-*,
    то они не должны отправляться бэкендам, если мы не доверяем
    пользовательским заголовками и/или в клиентском запросе отсутствует
    laas_answer_header (BALANCER-545)
    """
    x_region_name = 'X-Region-City'
    x_region_value = '123'
    geo_ctx.start_all()
    geo_ctx.perform_request(http.request.get(headers={geo_ctx.TAKE_IP_FROM: '8.8.8.8', x_region_name: x_region_value}))
    geo_request = geo_ctx.geo_backend.state.get_request()
    request = geo_ctx.backend.state.get_request()

    asserts.no_header(geo_request, x_region_name)
    asserts.no_header(request, x_region_name)


def test_x_ip_properties_from_client(geo_ctx):
    """
    Если запрос клиента содержит заголовок X-IP-Properties,
    то он не должен отправляться бэкендам, если мы не доверяем
    пользовательским заголовкам и/или в клиентском запросе отсутствует
    laas_answer_header (BALANCER-545)
    """
    x_ip_properties_name = 'X-IP-Properties'
    x_ip_properties_value = '123'
    geo_ctx.start_all()
    geo_ctx.perform_request(http.request.get(
        headers={
            geo_ctx.TAKE_IP_FROM: '8.8.8.8',
            x_ip_properties_name: x_ip_properties_value,
        })
    )
    geo_request = geo_ctx.geo_backend.state.get_request()
    request = geo_ctx.backend.state.get_request()

    asserts.no_header(geo_request, x_ip_properties_name)
    asserts.no_header(request, x_ip_properties_name)


def test_x_region_collision(geo_ctx):
    """
    Если запрос клиента и ответ geo-бэкенда содержат один и тот же заголовок X-Region-*,
    то сервисному бэкенду должен отправиться заголовок со значением из ответа geo-бэкенда.
    """
    x_region_name = 'X-Region-City'
    x_region_value = '123'
    x_region_client_value = '321'
    geo_ctx.start_all(geo_response=http.response.ok(headers={x_region_name: x_region_value}))
    geo_ctx.perform_request(http.request.get(
        headers={geo_ctx.TAKE_IP_FROM: '8.8.8.8', x_region_name: x_region_client_value}))
    request = geo_ctx.backend.state.get_request()

    asserts.header_value(request, x_region_name, x_region_value)
    asserts.no_header_value(request, x_region_name, x_region_client_value)


def test_x_ip_properties_collision(geo_ctx):
    """
    Если запрос клиента и ответ geo-бэкенда содержат один и тот же заголовок X-IP-Properties,
    то сервисному бэкенду должен отправиться заголовок со значением из ответа geo-бэкенда.
    """
    x_ip_properties_name = 'X-IP-Properties'
    x_ip_properties_value = '123'
    x_ip_properties_client_value = '321'
    geo_ctx.start_all(geo_response=http.response.ok(headers={x_ip_properties_name: x_ip_properties_value}))
    geo_ctx.perform_request(http.request.get(
        headers={geo_ctx.TAKE_IP_FROM: '8.8.8.8', x_ip_properties_name: x_ip_properties_client_value}))
    request = geo_ctx.backend.state.get_request()

    asserts.header_value(request, x_ip_properties_name, x_ip_properties_value)
    asserts.no_header_value(request, x_ip_properties_name, x_ip_properties_client_value)


def test_laas_answer_header_ok(geo_ctx):
    """
    Если ответ geo-бэкенда содержит хотя бы один заголовок X-Region-*,
    то сервисному бэкенду надо отправить запрос с заголовком laas_answer_header со значением 1.
    """
    geo_ctx.start_all(geo_response=http.response.ok(headers={'X-Region-City': '123'}))
    geo_ctx.perform_request(http.request.get(headers={geo_ctx.TAKE_IP_FROM: '8.8.8.8'}))
    request = geo_ctx.backend.state.get_request()

    asserts.header_value(request, geo_ctx.LAAS_ANSWER, '1')


def test_laas_answer_header_fail(geo_ctx):
    """
    Если ответ geo-бэкенда не содержит заголовков X-Region-*,
    то сервисному бэкенду надо отправить запрос с заголовком laas_answer_header со значением 0.
    """
    geo_ctx.start_all()
    geo_ctx.perform_request(http.request.get(headers={geo_ctx.TAKE_IP_FROM: '8.8.8.8'}))
    request = geo_ctx.backend.state.get_request()

    asserts.header_value(request, geo_ctx.LAAS_ANSWER, '0')


def test_laas_answer_header_no_geo_backend(geo_ctx):
    """
    Если geo-бэкенд не отвечает,
    то сервисному бэкенду надо отправить запрос с заголовком laas_answer_header со значением 0.
    """
    geo_ctx.start_no_geo()
    geo_ctx.perform_request(http.request.get(headers={geo_ctx.TAKE_IP_FROM: '8.8.8.8'}))
    request = geo_ctx.backend.state.get_request()

    asserts.header_value(request, geo_ctx.LAAS_ANSWER, '0')


def test_laas_answer_header_in_request_ok(geo_ctx):
    """
    Запрос содержит заголовок laas_answer_header, не содержит X-Region-.* заголовков,
    ответ geo-бэкенда содержит заголовок X-Region-*.
    Исходный заголовок laas_answer_header надо вырезать,
    сервисному бэкенду надо отправить запрос с заголовком laas_answer_header со значением 1.
    """
    laas_answer_value = 'led'
    geo_ctx.start_all(geo_response=http.response.ok(headers={'X-Region-City': '123'}))
    geo_ctx.perform_request(http.request.get(
        headers={geo_ctx.TAKE_IP_FROM: '8.8.8.8', geo_ctx.LAAS_ANSWER: laas_answer_value}))
    geo_request = geo_ctx.geo_backend.state.get_request()
    request = geo_ctx.backend.state.get_request()

    asserts.no_header(geo_request, geo_ctx.LAAS_ANSWER)
    asserts.header_value(request, geo_ctx.LAAS_ANSWER, '1')
    asserts.no_header_value(request, geo_ctx.LAAS_ANSWER, laas_answer_value)


def test_laas_answer_header_in_request_fail(geo_ctx):
    """
    Запрос содержит заголовок laas_answer_header, не содержит X-Region-.* заголовков,
    ответ geo-бэкенда не содержит заголовков X-Region-*.
    Исходный заголовок laas_answer_header надо вырезать,
    сервисному бэкенду надо отправить запрос с заголовком laas_answer_header со значением 0.
    """
    laas_answer_value = 'led'
    geo_ctx.start_all()
    geo_ctx.perform_request(http.request.get(
        headers={geo_ctx.TAKE_IP_FROM: '8.8.8.8', geo_ctx.LAAS_ANSWER: laas_answer_value}))
    geo_request = geo_ctx.geo_backend.state.get_request()
    request = geo_ctx.backend.state.get_request()

    asserts.no_header(geo_request, geo_ctx.LAAS_ANSWER)
    asserts.header_value(request, geo_ctx.LAAS_ANSWER, '0')
    asserts.no_header_value(request, geo_ctx.LAAS_ANSWER, laas_answer_value)


def test_laas_answer_header_in_request_no_geo_backend(geo_ctx):
    """
    Запрос содержит заголовок laas_answer_header, не содержит
    X-Region-.* заголовков, geo-бэкенда не отвечает.
    Сервисному бэкенду надо отправить запрос с заголовком laas_answer_header со значением 0.
    """
    laas_answer_value = 'led'
    geo_ctx.start_no_geo()
    geo_ctx.perform_request(http.request.get(headers={geo_ctx.TAKE_IP_FROM: '8.8.8.8', geo_ctx.LAAS_ANSWER: laas_answer_value}))
    request = geo_ctx.backend.state.get_request()

    asserts.header_value(request, geo_ctx.LAAS_ANSWER, '0')
    asserts.no_header_value(request, geo_ctx.LAAS_ANSWER, laas_answer_value)


def test_geo_path(geo_ctx):
    """
    geo-бэкенду должен уйти запрос с path, указанным в geo_path.
    В подмодуль должен уйти запрос с path исходного запроса.
    """
    path = '/led/zeppelin'
    geo_path = '/pink/floyd'
    geo_ctx.start_all(geo_path=geo_path)
    geo_ctx.perform_request(http.request.get(path=path, headers={geo_ctx.TAKE_IP_FROM: '8.8.8.8'}))
    request = geo_ctx.backend.state.get_request()
    geo_request = geo_ctx.geo_backend.state.get_request()

    asserts.path(request, path)
    asserts.path(geo_request, geo_path)


def test_geo_path_default(geo_ctx):
    """
    Если в конфиге не указан geo_path, то в запросе к geo_backend-у path должен быть
    /region?response_format=header&version=1&service=balancer.
    """
    path = '/led/zeppelin'
    geo_ctx.start_all()
    geo_ctx.perform_request(http.request.get(path=path, headers={geo_ctx.TAKE_IP_FROM: '8.8.8.8'}))
    request = geo_ctx.backend.state.get_request()
    geo_request = geo_ctx.geo_backend.state.get_request()

    asserts.path(request, path)
    asserts.path(geo_request, '/region?response_format=header&version=1&service=balancer')


def test_take_ip_from(geo_ctx):
    """
    В запрос к геомодулю надо добавить заголовок X-Forwarded-For-Y
    со значением заголовка, указанного в take_ip_from.
    В запросе в подмодуль заголовов X-Forwarded-For-Y добавляться не должен,
    заголовок take_ip_from должен быть с исходным значением.
    """
    ip = '8.8.8.8'
    geo_ctx.start_all()
    geo_ctx.perform_request(http.request.get(headers={geo_ctx.TAKE_IP_FROM: ip}))
    request = geo_ctx.backend.state.get_request()
    geo_request = geo_ctx.geo_backend.state.get_request()

    asserts.header_value(geo_request, geo_ctx.FORWARDED_FOR, ip)
    asserts.header_value(geo_request, geo_ctx.TAKE_IP_FROM, ip)
    asserts.header_value(request, geo_ctx.TAKE_IP_FROM, ip)
    asserts.no_header(request, geo_ctx.FORWARDED_FOR)


def test_multiple_take_ip_from(geo_ctx):
    """
    Если в запросе несколько take_ip_from,
    то при добавлении заголовка X-Forwarded-For-Y должно быть указано значение первого из них.
    """
    first_ip = '8.8.8.8'
    second_ip = '9.9.9.9'
    headers = [
        (geo_ctx.TAKE_IP_FROM, first_ip),
        (geo_ctx.TAKE_IP_FROM, second_ip),
    ]
    geo_ctx.start_all()
    geo_ctx.perform_request(http.request.raw_get(headers=headers))
    geo_request = geo_ctx.geo_backend.state.get_request()

    asserts.header_value(geo_request, geo_ctx.FORWARDED_FOR, first_ip)
    asserts.no_header_value(geo_request, geo_ctx.FORWARDED_FOR, second_ip)
    asserts.headers_values(geo_request, headers)


def test_take_ip_from_x_forwarded_for(geo_ctx):
    """
    Если take_ip_from == 'X-Forwarded-For-Y',
    то при запросе в геомодуль заголовок менять не надо.
    """
    ip = '8.8.8.8'
    geo_ctx.start_all(take_ip_from=geo_ctx.FORWARDED_FOR)
    geo_ctx.perform_request(http.request.get(headers={geo_ctx.FORWARDED_FOR: ip}))
    request = geo_ctx.backend.state.get_request()
    geo_request = geo_ctx.geo_backend.state.get_request()

    asserts.header_value(geo_request, geo_ctx.FORWARDED_FOR, ip)
    asserts.header_value(request, geo_ctx.FORWARDED_FOR, ip)


def test_multiple_take_ip_from_x_forwarded_for(geo_ctx):
    """
    Если take_ip_from == 'X-Forwarded-For-Y' и в запросе несколько заголовков X-Forwarded-For-Y,
    то в геомодуль надо отправить все заголовки X-Forwarded-For-Y.
    """
    forwarded_for_led = 'led'
    forwarded_for_zeppelin = 'zeppelin'
    headers = [
        (geo_ctx.FORWARDED_FOR, forwarded_for_led),
        (geo_ctx.FORWARDED_FOR, forwarded_for_zeppelin),
    ]
    geo_ctx.start_all(take_ip_from=geo_ctx.FORWARDED_FOR)
    geo_ctx.perform_request(http.request.raw_get(headers=headers))
    request = geo_ctx.backend.state.get_request()
    geo_request = geo_ctx.geo_backend.state.get_request()

    asserts.headers_values(geo_request, headers)
    asserts.headers_values(request, headers)


def test_take_ip_from_and_x_forwarded_for(geo_ctx):
    """
    Если take_ip_from != 'X-Forwarded-For-Y' и в запросе клиента есть и take_ip_from и X-Forwarded-For-Y,
    то в запросе к геомодулю надо удалить исходные X-Forwarded-For-Y и добавить со значением из take_ip_from.
    В подмодуль надо отрпавить запрос с исходными заголовками.
    """
    ip = '8.8.8.8'
    forwarded_for_led = 'led'
    forwarded_for_zeppelin = 'zeppelin'
    headers = {
        geo_ctx.TAKE_IP_FROM: ip,
        geo_ctx.FORWARDED_FOR: forwarded_for_led,
        geo_ctx.FORWARDED_FOR: forwarded_for_zeppelin,
    }
    geo_ctx.start_all()
    geo_ctx.perform_request(http.request.raw_get(headers=headers))
    request = geo_ctx.backend.state.get_request()
    geo_request = geo_ctx.geo_backend.state.get_request()

    asserts.header_value(geo_request, geo_ctx.FORWARDED_FOR, ip)
    asserts.no_header_value(geo_request, geo_ctx.FORWARDED_FOR, forwarded_for_led)
    asserts.no_header_value(geo_request, geo_ctx.FORWARDED_FOR, forwarded_for_zeppelin)
    asserts.headers_values(request, headers)


def test_no_take_ip_from(geo_ctx):
    """
    Если в запросе отсутствует заголовок, указанный в take_ip_from,
    то запрос не должен отправляться в геомодуль,
    а сразу отправляться клиенту с заголовком laas_answer_header: 0
    """
    path = '/led/zeppelin'
    geo_ctx.start_all()
    geo_ctx.perform_request(http.request.get(path=path))
    request = geo_ctx.backend.state.get_request()

    assert geo_ctx.geo_backend.state.requests.empty()
    asserts.path(request, path)
    asserts.header_value(request, geo_ctx.LAAS_ANSWER, '0')


def test_no_take_ip_from_cut_x_region(geo_ctx):
    """
    Если в запросе отсутствует заголовок take_ip_from, и есть заголовки X-Region-* и laas_answer_header.
    В запросе в подмодуль должны быть вырезаны заголовки X-Region-*,
    значение laas_answer_header должно быть заменено на 0
    """
    geo_ctx.start_all()
    geo_ctx.perform_request(http.request.get(headers={
        'X-Region-City': '123',
        'X-Region-Is-User-Choice': '1',
        geo_ctx.LAAS_ANSWER: 'pink',
        geo_ctx.LAAS_ANSWER: 'floyd'}))
    request = geo_ctx.backend.state.get_request()

    asserts.no_header(request, 'X-Region-City')
    asserts.no_header(request, 'X-Region-Is-User-Choice')
    asserts.no_header_value(request, geo_ctx.LAAS_ANSWER, 'pink')
    asserts.no_header_value(request, geo_ctx.LAAS_ANSWER, 'floyd')
    asserts.header_value(request, geo_ctx.LAAS_ANSWER, '0')


def test_file_switch_disable(geo_ctx):
    """
    При появлении файла file_switch балансер должен отключить логику работы с гемодулем
    и сразу отправлять запрос в подмодуль, вырезав заголовки X-Region-* и laas_answer_header
    и добавив заголовок laas_answer_header со значением 0.
    """
    laas_led = 'led'
    laas_zeppelin = 'zeppelin'
    file_switch = geo_ctx.manager.fs.create_file('file_switch')
    geo_ctx.start_all(file_switch=file_switch)
    time.sleep(2)
    geo_ctx.perform_request(http.request.raw_get(headers={
        ('X-Region-City', '123'),
        ('x-Region-Is-User-Choice', '1'),
        (geo_ctx.TAKE_IP_FROM, '8.8.8.8'),
        (geo_ctx.LAAS_ANSWER, laas_led),
        (geo_ctx.LAAS_ANSWER, laas_zeppelin),
    }))
    request = geo_ctx.backend.state.get_request()

    assert geo_ctx.geo_backend.state.requests.empty()
    asserts.no_header(request, 'X-Region-City')
    asserts.no_header(request, 'X-Region-Is-User-Choice')
    asserts.header_value(request, geo_ctx.LAAS_ANSWER, '0')
    asserts.no_header_value(request, geo_ctx.LAAS_ANSWER, laas_zeppelin)
    asserts.no_header_value(request, geo_ctx.LAAS_ANSWER, laas_zeppelin)


def test_file_switch_enable(geo_ctx):
    """
    После удаления файла file_switch балансер должен снова включить логику работы с геомодулем.
    """
    ip = '8.8.8.8'
    laas_led = 'led'
    laas_zeppelin = 'zeppelin'
    file_switch = geo_ctx.manager.fs.create_file('file_switch')
    geo_ctx.start_all(file_switch=file_switch)
    time.sleep(2)
    geo_ctx.manager.fs.remove(file_switch)
    time.sleep(2)
    geo_ctx.perform_request(http.request.raw_get(headers={
        ('X-Region-City', '123'),
        ('x-Region-Is-User-Choice', '1'),
        (geo_ctx.TAKE_IP_FROM, ip),
        (geo_ctx.LAAS_ANSWER, laas_led),
        (geo_ctx.LAAS_ANSWER, laas_zeppelin),
    }))
    request = geo_ctx.backend.state.get_request()
    geo_request = geo_ctx.geo_backend.state.get_request()

    asserts.header_value(geo_request, geo_ctx.FORWARDED_FOR, ip)
    asserts.no_header(request, 'X-Region-City')
    asserts.no_header(request, 'X-Region-Is-User-Choice')
    asserts.header_value(request, geo_ctx.LAAS_ANSWER, '0')
    asserts.no_header_value(request, geo_ctx.LAAS_ANSWER, laas_zeppelin)
    asserts.no_header_value(request, geo_ctx.LAAS_ANSWER, laas_zeppelin)


@pytest.mark.parametrize('laas_answer', ['1', '0'])
def test_trusted_ok(geo_ctx, laas_answer):
    """
    BALANCER-545
    Если включён trusted флаг, и клиент приходит с laas_answer_header
    и хотя бы одним X-Region-.*, то считаем этот ответ верным
    и не ходим в геоподмодуль и не модифицируем исходный запрос
    """
    geo_ctx.start_all(trusted=True)
    geo_ctx.perform_request(http.request.get(headers={geo_ctx.LAAS_ANSWER: laas_answer, 'X-Region-Id': '213'}))
    request = geo_ctx.backend.state.get_request()
    asserts.header_value(request, geo_ctx.LAAS_ANSWER, laas_answer)
    asserts.header_value(request, 'X-Region-Id', '213')


@pytest.mark.parametrize('laas_answer', ['1', '0'])
def test_trusted_no_region(geo_ctx, laas_answer):
    """
    BALANCER-545
    Если включён trusted флаг, и клиент приходит с laas_answer_header
    и без X-Region-.*, то считаем этот ответ плохим
    и ходим в геоподмодуль (и локальную геобазу, если тот не сработал)
    """
    geo_ctx.start_no_geo(trusted=True)
    geo_ctx.perform_request(http.request.get(headers={geo_ctx.LAAS_ANSWER: laas_answer}))
    request = geo_ctx.backend.state.get_request()
    asserts.header_value(request, geo_ctx.LAAS_ANSWER, '0')


def test_trusted_no_laas_answered(geo_ctx):
    """
    BALANCER-545
    Если включён trusted флаг, и клиент приходит без laas_answer_header,
    но с X-Region-.*, то считаем этот ответ плохим
    и ходим в геоподмодуль (и локальную геобазу, если тот не сработал)
    """
    geo_ctx.start_no_geo(trusted=True)
    geo_ctx.perform_request(http.request.get(headers={'X-Region-Id': '213'}))
    request = geo_ctx.backend.state.get_request()
    asserts.header_value(request, geo_ctx.LAAS_ANSWER, '0')


def test_trusted_no_region_no_laas_answered(geo_ctx):
    """
    BALANCER-545
    Если включён trusted флаг, и клиент приходит без laas_answer_header,
    и без X-Region-.*, то считаем этот ответ плохим
    и ходим в геоподмодуль (и локальную геобазу, если тот не сработал)
    """
    geo_ctx.start_no_geo(trusted=True)
    geo_ctx.perform_request(http.request.get())
    request = geo_ctx.backend.state.get_request()
    asserts.header_value(request, geo_ctx.LAAS_ANSWER, '0')


@pytest.mark.parametrize('laas_answer', ['1', '0'])
def test_file_switch_trusted_ok(geo_ctx, laas_answer):
    """
    BALANCER-621
    Если есть file_switch, флаг trusted включен и запрос содержит заголовки
    laas_answer_header и X-Region-*, то балансер не должен ходить в геоподмодуль
    и должен передать запрос в подмодуль как есть, не вырезая никаких заголовков
    """
    file_switch = geo_ctx.manager.fs.create_file('file_switch')
    geo_ctx.start_all(trusted=True, file_switch=file_switch)
    geo_ctx.perform_request(http.request.get(headers={geo_ctx.LAAS_ANSWER: laas_answer, 'X-Region-Id': '213'}))
    request = geo_ctx.backend.state.get_request()
    asserts.header_value(request, geo_ctx.LAAS_ANSWER, laas_answer)
    asserts.header_value(request, 'X-Region-Id', '213')


@pytest.mark.parametrize('laas_answer', ['1', '0'])
def test_file_switch_trusted_no_region(geo_ctx, laas_answer):
    """
    BALANCER-621
    Если есть file_switch, флаг trusted включен и запрос содержит заголовок laas_answer_header,
    но не содержит заголовков X-Region-*, то балансер не должен ходить в геоподмодуль
    и должен передать запрос в подмодуль, указав laas_answer_header: 0
    """
    file_switch = geo_ctx.manager.fs.create_file('file_switch')
    geo_ctx.start_all(trusted=True, file_switch=file_switch)
    geo_ctx.perform_request(http.request.get(headers={geo_ctx.LAAS_ANSWER: laas_answer}))
    request = geo_ctx.backend.state.get_request()
    asserts.header_value(request, geo_ctx.LAAS_ANSWER, '0')
    asserts.no_header(request, 'X-Region-Id')


def test_file_switch_trusted_no_laas_answered(geo_ctx):
    """
    BALANCER-621
    Если есть file_switch, флаг trusted включен и запрос содержит заголовок X-Region-*,
    но не содержит заголовок laas_answer_header, то балансер не должен ходить в геоподмодуль
    и должен передать запрос в подмодуль, указав laas_answer_header: 0 и вырезав заголовок X-Region-*
    """
    file_switch = geo_ctx.manager.fs.create_file('file_switch')
    geo_ctx.start_all(trusted=True, file_switch=file_switch)
    geo_ctx.perform_request(http.request.get(headers={'X-Region-Id': '213'}))
    request = geo_ctx.backend.state.get_request()
    asserts.header_value(request, geo_ctx.LAAS_ANSWER, '0')
    asserts.no_header(request, 'X-Region-Id')


def test_file_switch_trusted_no_region_no_laas_answered(geo_ctx):
    """
    BALANCER-621
    Если есть file_switch, флаг trusted включен и запрос не содержит заголовков laas_answer_header и X-Region-*,
    то балансер не должен ходить в геоподмодуль и должен передать запрос в подмодуль, указав laas_answer_header: 0
    """
    file_switch = geo_ctx.manager.fs.create_file('file_switch')
    geo_ctx.start_all(trusted=True, file_switch=file_switch)
    geo_ctx.perform_request(http.request.get())
    request = geo_ctx.backend.state.get_request()
    asserts.header_value(request, geo_ctx.LAAS_ANSWER, '0')
    asserts.no_header(request, 'X-Region-Id')


def base_stats_test(geo_ctx, path, request=None):
    if request is None:
        request = http.request.get(headers={'Host': 'yandex.ru', geo_ctx.TAKE_IP_FROM: '8.8.8.8'})
    count = 10
    for _ in range(count):
        geo_ctx.perform_request(request)

    unistat = geo_ctx.get_unistat(geo_ctx.unistat_port)
    signal_name = 'geobase-' + path.split('/')[-1] + '_summ'
    assert unistat[signal_name] == count


def test_stats_laas_no_answer(geo_ctx):
    """
    BALANCER-469
    Если geo-бэкенд не отвечает, то в статистике должен увеличиться счетчик laas_no_answer
    """
    geo_ctx.start_module_backend(SimpleConfig())
    geo_ctx.start_geo_balancer()
    base_stats_test(geo_ctx, './geobase/laas_no_answer')


def test_stats_laas_no_header(geo_ctx):
    """
    BALANCER-469
    Если geo-бэкенд отвечает без заголовков X-Region-*, то в статистике должен увеличиться счетчик laas_no_answer
    """
    geo_ctx.start_all()
    base_stats_test(geo_ctx, './geobase/laas_no_header')


def test_stats_no_ip(geo_ctx):
    """
    BALANCER-469
    Если в запросе отсутствует заголовок с IP адресом, то в статистике должен увеличиться счетчик no_ip
    """
    geo_ctx.start_all()
    base_stats_test(geo_ctx, './geobase/no_ip', http.request.get())


def test_x_region_fallback(ctx):
    """
    BALANCER-1923
    Experiment with fallback
    """
    headers = {
        'X-Region-Id': '18',
        'X-Region-Location': '61.787374, 34.354325, 15000, 1544023646',
        'X-Region-Suspected-Location': '61.787374, 34.354325, 15000, 1544023646',
        'X-Region-City-Id': '18',
        'X-Region-Suspected-City': '18',
        'X-Region-By-IP': '18',
        'X-Region-Is-User-Choice': '0',
        'X-Region-Suspected': '18',
        'X-Region-Should-Update-Cookie': '0',
        'X-Region-Precision': '2',
        'X-Region-Suspected-Precision': '2',
        'X-IP-Properties': 'EOEBGAAgACgA',
        'X-Region-Affected-Exps': '',
        'X-Region-Fallback': '1',
    }
    ctx.start_backend(SimpleConfig(response=http.response.ok(headers=headers)), name='geo_fallback_backend')
    ctx.start_backend(SimpleConfig(), name='backend')
    geo_fallback_backend_port = ctx.geo_fallback_backend.server_config.port
    backend_port = ctx.backend.server_config.port
    ctx.start_balancer(GeobaseWithFallbackConfig(geo_backend_port=None, geo_fallback_backend_port=geo_fallback_backend_port, backend_port=backend_port))
    ctx.perform_request(http.request.get(headers={'My-Ip': '8.8.8.8'}))
    request = ctx.backend.state.get_request()
    asserts.headers_values(request, headers)


def test_processing_time_header(geo_ctx):
    geo_ctx.start_geo_backend(SimpleDelayedConfig(response_delay=2))
    geo_ctx.start_module_backend(SimpleConfig())
    geo_ctx.start_geo_balancer()

    geo_ctx.perform_request(http.request.get(headers={geo_ctx.TAKE_IP_FROM: '8.8.8.8'}))
    request = geo_ctx.backend.state.get_request()
    processing_time_headers = request.headers.get_all('X-Yandex-Balancer-LaaS-ProcessingTime')
    assert len(processing_time_headers) == 1
    assert int(processing_time_headers[0]) >= 2 * 1000 * 1000


def test_x_ip_properties_gdpr(geo_ctx):
    """
    BALANCER-2863 using x-ip-properties as a source for gdpr geolocation
    """
    geo_ctx.start_all(
        geo_response=http.response.ok(headers={
            # IsGdpr: true
            'X-IP-Properties': 'QAE=',
        }),
    )
    resp = geo_ctx.perform_request(http.request.get(headers={
        geo_ctx.TAKE_IP_FROM: '8.8.8.8',
        'host': 'yandex.tech',
    }))
    res = resp.headers.get_all('set-cookie')
    assert len(res) == 1
    nv, _ = res[0].split(';', 1)
    n, v = nv.split('=', 1)
    assert n == "is_gdpr_b"
    is_gdpr, is_gdpr_no_vpn = parse_is_gdpr_b(v)
    assert is_gdpr
    assert is_gdpr_no_vpn
