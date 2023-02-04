import mongomock
import pytest

from maps.garden.libs_server.common.errors import NotFoundException, ConflictException
from maps.garden.libs_server.common.contour_manager import ContourManager, ContourStatus


def test_find_all():
    mongodb = mongomock.MongoClient(tz_aware=True).db
    contour_manager = ContourManager(mongodb)

    contours = list(contour_manager.find_all())
    # There are no migrations with system contours applied in this test
    assert len(contours) == 0


def test_find_nonexistent():
    mongodb = mongomock.MongoClient(tz_aware=True).db
    contour_manager = ContourManager(mongodb)

    with pytest.raises(NotFoundException):
        contour_manager.find("nonexistent")


def test_contour():
    mongodb = mongomock.MongoClient(tz_aware=True).db
    contour_manager = ContourManager(mongodb)

    contour = contour_manager.create("vasya_test", "vasya")
    assert contour.name == "vasya_test"
    assert contour.owner == "vasya"
    assert contour.created_at.tzinfo is not None
    assert not contour.is_system
    assert contour.status == ContourStatus.ACTIVE

    with pytest.raises(ConflictException):
        contour_manager.create("vasya_test", "vasya")

    contour = contour_manager.find("vasya_test")
    assert contour.name == "vasya_test"
    assert contour.owner == "vasya"
    assert contour.created_at.tzinfo is not None
    assert not contour.is_system
    assert contour.status == ContourStatus.ACTIVE

    contours = list(contour_manager.find_all())
    assert len(contours) == 1

    contour_manager.delete(contour)

    contour = contour_manager.find("vasya_test")
    assert contour.status == ContourStatus.DELETING


def test_find_system_contour_names():
    mongodb = mongomock.MongoClient(tz_aware=True).db
    contour_manager = ContourManager(mongodb)

    assert contour_manager.find_system_contour_names() == []

    contour_manager.create("vasya_system", "vasya", is_system=True)

    assert contour_manager.find_system_contour_names() == ["vasya_system"]

    contour_manager.create("vasya_not_system", "vasya", is_system=False)

    assert contour_manager.find_system_contour_names() == ["vasya_system"]


def test_active_module_versions():
    mongodb = mongomock.MongoClient(tz_aware=True).db
    contour_manager = ContourManager(mongodb)

    contour = contour_manager.create("vasya_test", "vasya")
    assert not contour.active_module_versions

    contour_manager.activate_module_version("vasya_test", "some_module", "111")
    contour_manager.activate_module_version("vasya_test", "some_other_module", "aaa")

    contour = contour_manager.find("vasya_test")
    assert contour.active_module_versions == {
        "some_module": "111",
        "some_other_module": "aaa"
    }

    contour_manager.deactivate_module_version("vasya_test", "some_module", "111")
    contour = contour_manager.find("vasya_test")
    assert contour.active_module_versions == {
        "some_other_module": "aaa"
    }
