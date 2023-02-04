"""Tests MongoDB mocking."""
from enum import Enum

import pytest

from mongoengine import NotUniqueError
from mongoengine import Document, StringField
from sepelib.mongo.fields import EnumField

from sepelib.mongo.mock import ObjectMocker
from sepelib.mongo.mock import Database
from sepelib.mongo.util import register_model


@register_model
class Test(Document):
    foo = StringField()


@pytest.fixture
def mocker():
    return ObjectMocker(Test)


def test_database_mocking():
    try:
        db = Database().connection
        assert not (set(db.database_names()) - set(Database._system_databases))

        test = Test(foo="bar").save(force_insert=True)
        assert list(Test.objects) == [test]
    finally:
        db.close()

    # FIXME
    # with pytest.raises(MongoEngineConnectionError):
    #    list(Test.objects)


def test_object_mocker_mock_not_add_not_save(db, mocker):
    assert isinstance(mocker.mock(add=False, save=False), Test)
    assert not len(mocker.objects)
    assert not len(Test.objects)


def test_object_mocker_mock_not_save(db, mocker):
    obj1 = mocker.mock(save=False)
    obj2 = mocker.mock(save=False)
    assert len(mocker.objects) == 2
    assert mocker.objects[0] is obj1
    assert mocker.objects[1] is obj2
    assert not len(Test.objects)


def test_object_mocker_mock(db, mocker):
    obj1 = mocker.mock()
    obj2 = mocker.mock()
    assert len(mocker.objects) == 2
    assert mocker.objects[0] is obj1
    assert mocker.objects[1] is obj2
    assert sorted(obj.to_json() for obj in Test.objects) == sorted(
        obj.to_json() for obj in mocker.objects)


def test_object_mocker_assert_equal(db, mocker):
    mocker.mock()
    mocker.assert_equal()

    obj = mocker.mock({"foo": "bar"})
    mocker.assert_equal()

    obj.foo = "test"
    with pytest.raises(AssertionError):
        mocker.assert_equal()

    obj.save()
    mocker.assert_equal()


def test_indexes(db):
    class Doc(Document):
        foo = StringField(unique=True)

    mocker = ObjectMocker(Doc)
    obj1 = mocker.mock({"foo": "bar"})

    with pytest.raises(NotUniqueError):
        mocker.mock({"foo": "bar"})

    obj2 = mocker.mock({"foo": "baz"})

    assert len(mocker.objects) == 2
    assert mocker.objects[0] is obj1
    assert mocker.objects[1] is obj2
    mocker.assert_equal()


def test_enum_field(db):
    class Doc(Document):
        class Status(Enum):
            OPEN = 1
            CLOSED = 2
            CANCELLED = 3
        status = EnumField(Status)
    d1 = Doc(status=Doc.Status.OPEN).save()
    d2 = Doc(status=Doc.Status.CANCELLED.name).save()
    assert d1.to_mongo()['status'] == 'OPEN'
    assert Doc.objects.with_id(d1.id).status is Doc.Status.OPEN
    assert Doc.objects.with_id(d2.id).status is Doc.Status.CANCELLED
