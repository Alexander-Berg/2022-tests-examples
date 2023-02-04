from copy import deepcopy

import pytest

from maps.garden.sdk.resources.python import PythonResource
from maps.garden.sdk.core import GardenError, Version

from maps.garden.libs_server.resource_storage.resource_meta import ResourceMeta
from maps.garden.libs_server.resource_storage.exceptions import NotFoundInStorageError


FIRST = PythonResource(name="name")
FIRST.version = Version(properties={"day": "date"}, hash_string="57")

SECOND = PythonResource(name="name")
SECOND.version = Version(properties={"day": "date"}, hash_string="57")

THIRD = PythonResource(name="name")
THIRD.version = Version(properties={"day": "another_date"}, hash_string="1543")

FOURTH = PythonResource(name="first name")
FOURTH.version = Version(properties={"day": "word"}, hash_string="word")

FIFTH = PythonResource(name="name suffix")
FIFTH.version = Version(properties={"day": "date 1"}, hash_string="1189")

SIXTH = PythonResource(name="some name")
SIXTH.version = Version(properties={"day": "some date"}, hash_string="200")


def _make_resource_meta(resource: PythonResource):
    return ResourceMeta(resource.name, resource.version)


def _equal(lhs: PythonResource, rhs: PythonResource):
    return lhs.name == rhs.name and lhs.version == rhs.version


def test_is_empty(resource_storage):
    assert len(resource_storage) == 0


def test_bson_crippled(resource_storage):
    resource_storage.save(FIRST)
    extracted = resource_storage.get(FIRST.key)
    assert type(extracted) is type(FIRST)
    assert _equal(extracted, FIRST)


def test_save(mocker, resource_storage):
    resource_storage.save(FIRST)
    resource_storage.save(FIRST)
    resource_storage.save(SECOND)
    assert len(resource_storage) == 1
    assert _equal(next(iter(resource_storage))[1], SECOND)
    assert FIRST.key in resource_storage
    assert THIRD.key not in resource_storage
    resource_storage.save(THIRD)
    assert len(resource_storage) == 2

    SIXTH.contour_name = "test_contour"
    resource_storage.save(SIXTH)
    assert len(resource_storage) == 3

    resource_storage.remove(metas=resource_storage.find_versions(contour_name="test_contour"))
    assert len(resource_storage) == 2

    resource_storage.remove(metas=resource_storage.find_versions())
    assert len(resource_storage) == 0


def test_meta_by_key(resource_storage):
    resource_storage.save(FIRST)
    found = resource_storage.meta(FIRST.key)
    assert found == _make_resource_meta(FIRST)

    found = resource_storage.meta(FIFTH.key)
    assert found is None


def test_metas_by_keys(resource_storage):
    resource_storage.save(FIRST)
    resource_storage.save(THIRD)
    resource_storage.save(FIFTH)
    found = resource_storage.metas([FIRST.key], allow_missing_keys=True)
    assert found == {FIRST.key: _make_resource_meta(FIRST)}

    not_found = resource_storage.metas([FOURTH.key], allow_missing_keys=True)
    assert not_found == {}

    found = resource_storage.metas(
        [FIFTH.key, FIRST.key],
        allow_missing_keys=True,
    )
    assert found == {
        FIRST.key: _make_resource_meta(FIRST),
        FIFTH.key: _make_resource_meta(FIFTH),
    }

    not_found = resource_storage.metas(
        [FIFTH.key, FIRST.key, FOURTH.key],
        allow_missing_keys=True,
    )
    assert not_found == {
        FIRST.key: _make_resource_meta(FIRST),
        FIFTH.key: _make_resource_meta(FIFTH),
    }

    with pytest.raises(KeyError):
        resource_storage.metas([FOURTH.key])


def test_find_versions(resource_storage):
    resource_storage.save(FIRST)
    resource_storage.save(THIRD)
    resource_storage.save(FOURTH)
    resource_storage.save(FIFTH)
    SIXTH.contour_name = "test_contour"
    resource_storage.save(SIXTH)
    found = resource_storage.find_versions(
        name_pattern="^" + FIRST.name + "$")
    assert len(list(found)) == 2

    found = resource_storage.find_versions(name_pattern=FIRST.name)
    assert len(list(found)) == 2

    found = resource_storage.find_versions(properties_pattern={"day": "date"})
    assert len(list(found)) == 1

    found_res = list(
        resource_storage.find_versions(properties_pattern={"day": "word"})
    )
    assert len(found_res) == 1
    assert found_res[0] == _make_resource_meta(FOURTH)

    found = resource_storage.find_versions(contour_name="test_contour")
    assert len(list(found)) == 1


def test_save_without_key(resource_storage):
    resource = deepcopy(FIRST)
    resource.version.key = None
    assert resource.key is None
    with pytest.raises(GardenError):
        resource_storage.save(resource)


def test_delitem(resource_storage):
    resource_storage.save(FIRST)
    resource_storage.save(THIRD)
    del resource_storage[FIRST.key]

    with pytest.raises(NotFoundInStorageError):
        del resource_storage[FIRST.key]


def test_get(resource_storage):
    resource_storage.save(FIRST)
    found = resource_storage.get(FIRST.key)
    not_found = resource_storage.get(FOURTH.key)
    assert _equal(found, FIRST)
    assert not_found is None

    with pytest.raises(NotFoundInStorageError):
        del resource_storage[FOURTH.key]


def test_remove_check(resource_storage):
    resource_storage.save(FIRST)
    resource_storage.save(THIRD)
    resource_storage.save(FOURTH)
    assert len(resource_storage) == 3

    def check(key):
        return key != FIRST.key

    resource_storage.remove(
        resources=[FIRST, THIRD, FOURTH],
        remove_check=check
    )

    assert len(resource_storage) == 1

    resource_storage.remove_by_key(FIRST.key)
    assert len(resource_storage) == 0
