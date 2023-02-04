import pytest

from billing.apikeys.apikeys.service_config import OAuthServiceConfig
from billing.apikeys.apikeys.service_config.exceptions import BadParameterError, BadConfigError


def test_create_config():
    SERVICE_NAME = 'test-service'
    TOKEN = 'token'
    URL = 'url'

    # в конфиге приложения нет OAuth конфига для сервиса
    sc = OAuthServiceConfig(SERVICE_NAME)
    sc.__dict__['config'] = {}
    with pytest.raises(BadConfigError) as exc_info:
        repr(sc.token)
    assert exc_info.value.msg == BadConfigError.default_message % SERVICE_NAME
    with pytest.raises(BadConfigError):
        repr(sc.url)

    # в конфиге приложения есть OAuth конфиг для сервиса
    sc = OAuthServiceConfig(SERVICE_NAME)
    sc.__dict__['config'] = {'OAuth': TOKEN, 'URL': URL}
    assert sc.token == TOKEN and sc.url == URL

    # в конфиге приложения есть OAuth конфиг для сервиса, но он невалидный
    sc = OAuthServiceConfig(SERVICE_NAME)
    sc.__dict__['config'] = {'OAuth': None, 'URL': None}
    with pytest.raises(BadParameterError, match='token'):
        repr(sc.token)
    with pytest.raises(BadParameterError, match='url'):
        repr(sc.url)

    # попытка создания конфига с невалидными данными
    with pytest.raises(BadParameterError, match='token'):
        sc = OAuthServiceConfig(SERVICE_NAME, URL, '')
    with pytest.raises(BadParameterError, match='url'):
        sc = OAuthServiceConfig(SERVICE_NAME, '', TOKEN)

    # создание конфига с валидными данными и последующее извлечение этих данных
    sc = OAuthServiceConfig(SERVICE_NAME, URL, TOKEN)
    assert sc.token == TOKEN and sc.url == URL

    # попытка сохранить невалидные данные в существующий OAuthServiceConfig
    sc = OAuthServiceConfig(SERVICE_NAME)
    with pytest.raises(BadParameterError, match='token'):
        sc.token = None
    with pytest.raises(BadParameterError, match='url'):
        sc.url = None

    # сохранение валидных данных в сущесвующий OAuthServiceConfig и последующее извлечение этих данных
    sc = OAuthServiceConfig(SERVICE_NAME)
    sc.token, sc.url = TOKEN, URL
    assert sc.token == TOKEN and sc.url == URL
