import pytest

from django.conf import settings

from static_api import storage
from staff_api.v3_0.idm.db_collections import access_rules_collection, idm_roles_collection


settings.CACHES = {'default': {'BACKEND': 'django.core.cache.backends.locmem.LocMemCache'}}
storage.manager.ensure_misc_indexes()


@pytest.fixture()
def test_idm_roles_collection():
    idm_roles_collection.delete_many({})
    return idm_roles_collection


@pytest.fixture()
def test_access_rules_collection():
    access_rules_collection.delete_many({})
    return access_rules_collection


@pytest.fixture()
def test_persons_collection():
    collection = storage.manager.db['person']
    collection.delete_many({})
    return collection
