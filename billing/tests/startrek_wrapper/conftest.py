import pytest
from startrek_client.exceptions import NotFound

from billing.apikeys.apikeys.mapper import StartrekConfig
from billing.apikeys.apikeys.service_config import OAuthServiceConfig
from billing.apikeys.apikeys.startrek_wrapper.startrek import StartrekCollectionElementCheckerMixin


class FakeStartrekNotFound(NotFound):
    def __init__(self):
        pass


class FakeStartrekCollectionElement:

    def __init__(self, data=None):
        self._data = data or {}


class FakeStartrekCollection:

    def __init__(self, data=None):
        self._data = data or {}

    def get(self, item):
        if item not in self._data:
            raise FakeStartrekNotFound
        return self._data[item]

    def add(self, key, value=None):
        self._data[key] = value or FakeStartrekCollectionElement()

    def remove(self, key):
        self._data.pop(key, None)


class FakeStartrek:

    def __init__(self, *args, **kwargs):
        self.queues = FakeStartrekCollection()
        self.users = FakeStartrekCollection()


class FakeCustomStartrek(StartrekCollectionElementCheckerMixin, FakeStartrek):

    def __init__(self, connect_config):
        super().__init__(useragent=connect_config.service_name, token=connect_config.token,
                                                 base_url=connect_config.url)
        self.maillists = FakeStartrekCollection()


@pytest.fixture
def st_client():
    connect_config = OAuthServiceConfig('apikeys_startrek', 'url', 'token')
    return FakeCustomStartrek(connect_config)


@pytest.fixture
def st_config_user_observer():
    return StartrekConfig(queue='QUEUE', observer='user_observer')


@pytest.fixture
def st_config_maillist_observer():
    return StartrekConfig(queue='QUEUE', observer='maillist_observer@support.yandex.ru')
