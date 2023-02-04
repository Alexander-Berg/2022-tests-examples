"""
Проверяет, что приходящие от клиента в заголовках значения для сервисных
параметров (service, from) передаются в vh, ott, видеопоиск и др.
"""
import logging
from unittest.mock import Mock

import pytest
from django.test import Client
from requests.exceptions import ConnectionError
from smarttv.droideka.proxy.api.access_config import get_access_config, tv_default
from smarttv.droideka.proxy.api.es import client as es_client
from smarttv.droideka.proxy.api.ott import client as ott_client
from smarttv.droideka.proxy.api.vh import client as vh_client
from smarttv.droideka.proxy.views.search import get_videosearch_client
from smarttv.droideka.utils import PlatformInfo, PlatformType

http_client = Client(content_type='application/json')
logger = logging.getLogger(__name__)

VERSION_SENDS_HEADERS = '2.109'
VERSION_OLD = '2.100'


@pytest.fixture
def module():
    return PlatformInfo(PlatformType.ANDROID, quasar_platform='yandexmodule_2', app_version='2.1000')


@pytest.fixture
def module_broken():
    return PlatformInfo(PlatformType.ANDROID, quasar_platform='yandexmodule_2', app_version='2.107')


@pytest.fixture
def device_2_105():
    return PlatformInfo(PlatformType.ANDROID, app_version='2.105.9.4427')


@pytest.fixture
def tv():
    return PlatformInfo(PlatformType.ANDROID, app_version='1.1')


class TestVh:
    def test_default_service_params(self, rf):
        request = rf.get('/')
        params = vh_client.get_service_params(request)

        assert params['service'] == 'ya-tv-android'
        assert params['from'] == 'tvandroid'

    def test_service_params_from_request_headers(self, rf, tv):
        """
        Сервисные параметры от клиентов берутся из заголовков
        """
        headers = {'HTTP_X_OTTSERVICENAME': 'spacedep', 'HTTP_X_STRMFROM': 'moon'}
        request = rf.get('/', **headers)
        request.platform_info = tv

        params = vh_client.get_service_params(request)

        assert params['service'] == 'spacedep'
        assert params['from'] == 'moon'

    def test_service_params_from_request_headers_broken_clients(self, rf, module_broken):
        """
        Хедеры от клиентов 2.96 <= версии < 2.118 игнорируются
        """
        headers = {'HTTP_X_OTTSERVICENAME': 'spacedep', 'HTTP_X_STRMFROM': 'moon'}
        request = rf.get('/', **headers)
        request.platform_info = module_broken

        params = vh_client.get_service_params(request)

        assert params['service'] == 'ya-tv-android'
        assert params['from'] == 'tvandroid'


class TestVideoSearch:
    def test_default(self, rf):
        request = rf.get('/')
        client_parameter = get_videosearch_client(request)
        assert client_parameter == 'tvandroid'

    def test_service_params_from_request_headers(self, rf):
        headers = {'HTTP_X_VIDEOPOISKCLIENT': 'mars'}
        request = rf.get('/', **headers)

        assert get_videosearch_client(request) == 'mars'


class TestEntitySearch:
    def test_default(self, rf):
        request = rf.get('/')
        assert es_client.get_client(request) == 'tvandroid'

    def test_service_params_from_request_headers(self, rf):
        headers = {'HTTP_X_OBJECTRESPONSECLIENT': 'pluto'}
        request = rf.get('/', **headers)
        assert es_client.get_client(request) == 'pluto'


class TestOttClient:
    def test_selections_default_service_param(self, rf, responses):
        request = rf.get('/')
        request.request_info = Mock()
        request.request_info.quasar_device_id = ''

        with pytest.raises(ConnectionError):
            ott_client.selections('', '', {}, request)

        assert responses.calls[0].request.params['serviceId'] == '167'
        assert responses.calls[0].request.params['from'] == 'ya-tv-android'

    def test_get_default_service_params(self, rf):
        request = rf.get('/')

        assert ott_client.get_service_id(request) == '167'  # tv default

    def test_get_service_params_module_platform(self, rf, module):
        """
        У модуля свой service_id, если в заголовках не указано иное
        """
        request = rf.get('/')
        request.platform_info = module

        assert ott_client.get_service_id(request) == '234'  # module default

    def test_get_service_params_from_headers(self, rf):
        """
        Если сервис указан в заголовке, используется он
        """
        headers = {'HTTP_X_OTTSERVICEID': '777'}
        request = rf.get('/', **headers)
        request.request_info = Mock()

        assert ott_client.get_service_id(request) == '777'

    def test_get_service_params_broken_version(self, rf, module_broken):
        """
        У модуля сломанных версий не смотрим на заголовки
        """
        headers = {'HTTP_X_OTTSERVICEID': '777'}
        request = rf.get('/', **headers)
        request.platform_info = module_broken

        assert ott_client.get_service_id(request) == '234'  # используется default


class TestAccessConfigGetter:
    def test_works_without_request(self):
        assert get_access_config(None) == tv_default()
