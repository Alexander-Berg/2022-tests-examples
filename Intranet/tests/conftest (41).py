# coding: utf-8

from __future__ import unicode_literals

import pytest
from _pytest.monkeypatch import MonkeyPatch
from django.conf import settings
from django.forms import model_to_dict
from django.utils import timezone
from vins_core.dm.sample_processors.misspell import MisspellSamplesProcessor

from uhura import models
from uhura.external import staff
from uhura.tests import utils as tests_utils


@pytest.fixture(autouse=True)
def enable_db_access_for_all_tests(transactional_db):
    pass


@pytest.fixture(autouse=True)
def always_can_use_uhura(monkeypatch):
    monkeypatch.setattr('uhura.app.can_use_uhura', lambda *args, **kwargs: True)


@pytest.fixture(autouse=True, scope='function')
def clean_db(enable_db_access_for_all_tests):
    models.User.objects.all().delete()
    models.TelegramUsername.objects.all().delete()
    models.EmergencyNotifier.objects.all().delete()
    models.EmergencyNotification.objects.all().delete()
    models.EmergencyTestingList.objects.all().delete()
    models.Office.objects.all().delete()


@pytest.fixture(scope='session')
def mocked_bot():
    monkeypatch = MonkeyPatch()
    sent_messages = []

    class FakeBot:
        def __init__(self, *args, **kwargs):
            return

        def send_message(self, chat_id, text, *args, **kwargs):
            sent_messages.append(text)

    class FakeUpdater:
        def __init__(self, *args, **kwargs):
            return

    monkeypatch.setattr('telegram.Bot', FakeBot)
    monkeypatch.setattr('telegram.bot.Bot', FakeBot)
    monkeypatch.setattr('telegram.ext.Updater', FakeUpdater)
    monkeypatch.setattr('telegram.utils.request.Request', FakeUpdater)
    monkeypatch.setattr('yamb.Bot', FakeBot)
    return {'sent_messages': sent_messages}


@pytest.fixture(scope='session')
def tg_app(mocked_bot):
    from uhura import app
    from uhura.lib.vins.connectors.tg_connector import TelegramConnector

    connector = TelegramConnector(None, app.TelegramApp())
    setattr(app, 'app_connector', connector)

    vins_app = connector.vins_app
    setattr(vins_app, 'sent_messages', mocked_bot['sent_messages'])
    return vins_app


@pytest.fixture(autouse=True)
def clear_sent_messages(tg_app):
    del tg_app.sent_messages[:]


@pytest.fixture(scope='session')
def celery_config():
    return {
        'broker': settings.MONGO_HOST,
        'ignore_results': True
    }


@pytest.fixture(scope='session')
def celery_includes():
    return [
        'uhura.tasks.emergency'
    ]


@pytest.fixture(autouse=True)
def staff_data(monkeypatch):
    monkeypatch.setattr(staff, 'get_timezone_by_login', lambda x: 'Europe/Moscow')
    monkeypatch.setattr(
        staff, 'get_person_data_by_telegram_username', tests_utils.get_person_data_by_telegram_username
    )
    monkeypatch.setattr(
        staff, 'get_person_data_by_userphone', tests_utils.get_person_data_by_userphone
    )


@pytest.fixture(autouse=True)
def mock_misspell(monkeypatch):
    monkeypatch.setattr(MisspellSamplesProcessor, '_fix_misspells', lambda self, text: text)


@pytest.fixture
def uid(enable_db_access_for_all_tests):
    uid = tests_utils.get_uid()
    user = models.User.objects.create(
        username='robot-uhura',
        uid=uid,
        leave_at=timezone.now(),
        quit_at=timezone.now(),
        email='r@ya.ru'
    )
    assert_fields_are_filled(user)

    models.TelegramUsername.objects.create(
        user=user,
        username='uhuraappbot',
        telegram_id=uid,
    )
    return uid


def assert_fields_are_filled(obj):
    assert all(value is not None for value in model_to_dict(obj).values())


@pytest.fixture(scope='session')
def no_external_calls():
    """All tests should runs without external calls"""
    monkeypatch = MonkeyPatch()

    def opener(patched):
        def wrapper(url, *args, **kwargs):
            raise Exception("You forget to mock %s locally, it tries to open %s" % (patched, url))
        return wrapper
    monkeypatch.setattr('requests.request', opener('requests.request'))

    def get_class(patched):
        def wrapper(*args, **kwargs):
            raise Exception("You forget to mock %s locally" % patched)

        def init_wrapper(self, *args, **kwargs):
            raise Exception("You forget to mock %s locally" % patched)

        def attr_wrapper(cls, key):
            raise Exception("You forget to mock %s locally" % patched)
        wrapper.__getattr__ = attr_wrapper
        wrapper.__init__ = init_wrapper
        return wrapper

    monkeypatch.setattr('urllib.URLopener', get_class('urllib.URLopener'))
    monkeypatch.setattr('httplib.HTTPConnection', get_class('httplib.HTTPConnection'))
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
