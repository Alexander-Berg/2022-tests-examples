# coding: utf-8

from __future__ import unicode_literals

import pytest
from django.conf import settings

from tasha import environment
from tasha.external.telethon import api as telethon_api
from tasha.tests import mock_objects, factories


environment.setup_environment()


@pytest.fixture
def true_bot():
    true_bot = factories.TelegramAccountFactory(is_bot=True, username='true_bot', telegram_id=901)
    settings.TELEGRAM_BOT_ACCOUNT_ID = true_bot.id
    return true_bot


@pytest.fixture(autouse=True)
def clean_cache():
    from tasha.lib import cache_storage
    cache_storage._cached_objects = {}


@pytest.fixture(autouse=True)
def mock_tg_client(monkeypatch):
    class dict_wrapper:
        def __init__(self, input_dict):
            self.inner_dict = input_dict

        def to_dict(self):
            return self.inner_dict

    _entities = {}
    for obj in mock_objects._mock_telethon_api_fetch_chats():
        _entities[obj['id']] = dict_wrapper(obj)

    def _mocked_get_client():
        def wrapper(*args, **kwargs):
            raise ValueError('You forget to mock telethon_api.get_client')

        def get_entity(entity):
            if entity not in _entities:
                raise ValueError(
                    'Cannot get entity from a channel (or group) '
                    'that you are not part of. Join the group and retry'
                )
            return _entities[entity]

        def download_profile_photo(chat):
            return 'tasha/tests/data/avatar.png'

        def attr_wrapper(cls, key):
            raise Exception('You forget to mock telethon_api.get_client')

        wrapper.__getattr__ = attr_wrapper
        wrapper.get_entity = get_entity
        wrapper.download_profile_photo = download_profile_photo
        return wrapper

    monkeypatch.setattr(telethon_api, 'get_client', _mocked_get_client)


@pytest.fixture(autouse=True)
def no_external_calls(monkeypatch):
    """All tests should runs without external calls"""
    def opener(patched):
        def wrapper(url, *args, **kwargs):
            raise Exception("You forget to mock %s locally, it tries to open %s" % (patched, url))
        return wrapper
    monkeypatch.setattr('requests.request', opener('requests.request'))

    def get_class(patched):
        def wrapper(*args, **kwargs):
            raise Exception("You forget to mock %s locally" % patched)
        return wrapper
    monkeypatch.setattr(
        'requests.packages.urllib3.connection.VerifiedHTTPSConnection',
        get_class('requests.packages.urllib3.connection.VerifiedHTTPSConnection')
    )
    monkeypatch.setattr(
        'requests.packages.urllib3.connection.HTTPConnection',
        get_class('requests.packages.urllib3.connection.HTTPConnection')
    )
    monkeypatch.setattr(
        'urllib3.connection.HTTPConnection',
        get_class('urllib3.connection.HTTPConnection')
    )


@pytest.fixture
def telegram_test_users():
    users = {}
    for username in ['frodo', 'sam', 'bilbo', 'meriadoc', 'peregrin']:
        users[username] = factories.TelegramAccountFactory(username=username)
    return users
