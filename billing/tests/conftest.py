import os

import pytest
from unittest import mock
from mongoengine import connection
from billing.apikeys.apikeys.mapper.base import cache
from billing.apikeys.apikeys.cacher._pycache import LFUCache

from billing.apikeys.apikeys import mapper
from billing.apikeys.apikeys.application import ApikeysApplication
from billing.apikeys.apikeys.butils_port.application import getApplication
from .base_logic import logic_base
from .fixtures_db import (
    unit_fabric,
    reason_fabric,
    service_fabric,
    ph_person,
    ur_person,
    balance_contract_config,
    balance_config,
    hits_unit,
    hits_delayed_unit,
    simple_service,
    simple_service_delayed,
    empty_tariff,
    empty_tariff_delayed,
    empty_contractless_tariff,
    empty_contractless_tariff_more_expensive,
    empty_contractless_tariff_delayed,
    empty_contractless_accessable_tariff,
    empty_contractless_accessable_tariff_delayed,
    empty_tariffless_tariff,
    manager_permission_set,
    user,
    user_manager,
    user_support_ro,
    user_admin,
    project,
    simple_link,
    simple_link_delayed,
    link_with_fake_tariff,
    link_with_fake_tariff_delayed,
    link_with_fake_contractless_tariff,
    order_for_link_with_empty_contractless_tariff,
    order_for_link_with_empty_contractless_tariff_more_expensive,
    link_with_fake_contractless_tariff_delayed,
    link_without_any_tariff,
    simple_key,
    simple_key_delayed,
    simple_keys_pair,
    tariff_to_upgrade_from,
    tariff_to_upgrade_to,
    link_upgradable_with_discount,
    link_with_most_expensive_tariff,
    order_for_link_upgradable_with_discount_with_tariff_tier_1,
    order_for_link_upgradable_with_discount_with_tariff_tier_2,
)

__all__ = [
    'blackbox_mock',
    'empty_blackbox_mock',
    'logic_base',
    'unit_fabric',
    'reason_fabric',
    'service_fabric',
    'ph_person',
    'ur_person',
    'balance_contract_config',
    'balance_config',
    'hits_unit',
    'hits_delayed_unit',
    'simple_service',
    'simple_service_delayed',
    'empty_tariff',
    'empty_tariff_delayed',
    'empty_contractless_tariff',
    'empty_contractless_tariff_more_expensive',
    'empty_contractless_tariff_delayed',
    'empty_contractless_accessable_tariff',
    'order_for_link_with_empty_contractless_tariff',
    'order_for_link_with_empty_contractless_tariff_more_expensive',
    'empty_contractless_accessable_tariff_delayed',
    'empty_tariffless_tariff',
    'manager_permission_set',
    'user',
    'user_manager',
    'user_support_ro',
    'user_admin',
    'project',
    'simple_link',
    'simple_link_delayed',
    'link_with_fake_tariff',
    'link_with_fake_tariff_delayed',
    'link_with_fake_contractless_tariff',
    'link_with_fake_contractless_tariff_delayed',
    'link_without_any_tariff',
    'simple_key',
    'simple_key_delayed',
    'simple_keys_pair',
    'tests_path',
    'mongomock',
    'getone_without_cache',
    'app',
    'tariff_to_upgrade_from',
    'tariff_to_upgrade_to',
    'link_upgradable_with_discount',
    'link_with_most_expensive_tariff',
    'order_for_link_upgradable_with_discount_with_tariff_tier_1',
    'order_for_link_upgradable_with_discount_with_tariff_tier_2',
]


@pytest.fixture(autouse=True)
def clear_cache():
    if isinstance(cache, LFUCache):
        cache._lookup = {}


@pytest.fixture()
def tests_path():
    try:
        from yatest.common import source_path
        yield source_path('billing/apikeys/tests')
    except (ImportError, AttributeError):
        yield os.path.dirname(os.path.abspath(__file__))


@classmethod
def mock_ensure_indexes(cls, force=False):
    super(mapper.ApiKeysDocument, cls).ensure_indexes()


@pytest.fixture()
def mongomock(tests_path):
    import bson
    import pytz
    bson.tz_util.utc = pytz.utc

    try:
        getApplication()
    except RuntimeError:
        ApikeysApplication(os.path.join(tests_path, 'tests.cfg.xml'))

    for connection_name, connection_settings in connection._connection_settings.items():
        assert isinstance(connection_settings, dict)
        if not connection_settings.get('is_mock', None):
            connection._connection_settings[connection_name]['is_mock'] = True
            connection._connection_settings[connection_name]['username'] = None
            connection._connection_settings[connection_name]['password'] = None
            connection._connections.pop(connection_name, None)

    db = connection.get_db('apikeys-cloud')
    mapper.HourlyStat.ensure_indexes()

    with mock.patch('billing.apikeys.apikeys.mapper.base.ApiKeysDocument.ensure_indexes', new=mock_ensure_indexes):
        yield

    for collection in db.list_collection_names():
        db.drop_collection(collection)


@pytest.fixture(scope='session', autouse=True)
def getone_without_cache():
    with mapper.context.NoCacheSettings() as _fixture:
        yield _fixture


@pytest.fixture
def blackbox_mock():
    def mock_get_passport_info_by_uid(intranet, uid, user_ip, passport_name=None):
        return dict(
            uid=uid,
            login='mock_login_{}'.format(str(uid)),
            fio='Mock Mock',
            email='{}@ya.mock'.format(str(uid)),
            lang='',
        )
    with mock.patch(
            'billing.apikeys.apikeys.butils_port.passport.get_passport_info_by_uid',
            new=mock_get_passport_info_by_uid
    ):
        yield None


@pytest.fixture
def empty_blackbox_mock():
    def mock_get_passport_info_by_uid(intranet, uid, user_ip, passport_name=None):
        return dict(
            uid=None,
            login=None,
            fio=None,
            email=None,
            lang=None,
        )
    with mock.patch(
            'billing.apikeys.apikeys.butils_port.passport.get_passport_info_by_uid',
            new=mock_get_passport_info_by_uid
    ):
        yield None


@pytest.fixture
def app(blackbox_mock):

    class FakeApp:

        def __init__(self, config=None):
            self._config = config or {}

        def get_component_cfg(self, component_name):
            return self._config.get(component_name) or {}

    return FakeApp
