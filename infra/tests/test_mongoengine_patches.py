"""Tests our MongoEngine patches."""

from __future__ import unicode_literals

import pytest

from bson import ObjectId
from mongoengine import Document, StringField, IntField, EmbeddedDocument, EmbeddedDocumentField
from mongoengine.errors import InvalidDocumentError, InvalidQueryError

from sepelib.mongo.mock import ObjectMocker

import sepelib.mongo.util
sepelib.mongo.util.patch()


class EmbeddedDoc(EmbeddedDocument):
    int_value = IntField()
    string_value = StringField()


class Doc(Document):
    name = StringField()
    value = IntField()
    embedded = EmbeddedDocumentField(EmbeddedDoc)


@pytest.fixture
def mocker():
    return ObjectMocker(Doc)


def test_find_and_modify(db, mocker):
    mocker.mock({
        "name": "one",
        "value": 1,
    })

    doc = mocker.mock({
        "name": "two",
        "value": 2,
    })

    old_doc = Doc.objects(name="two").modify(set__value=3)
    assert old_doc.to_json() == doc.to_json()
    doc.value = 3
    mocker.assert_equal()

    new_doc = Doc.objects(value=3).modify(set__name="three", new=True)
    doc.name = "three"
    assert new_doc.to_json() == doc.to_json()
    mocker.assert_equal()

    assert Doc.objects(value=2).modify(set__name="two") is None
    mocker.assert_equal()

    assert Doc.objects(value=2).modify(remove=True) is None
    mocker.assert_equal()

    old_doc = Doc.objects(value=3).modify(remove=True)
    assert old_doc.to_json() == doc.to_json()
    mocker.remove(doc)
    mocker.assert_equal()


def test_find_and_modify_fields_support(db, mocker):
    doc = mocker.mock({
        "name": "one",
        "value": 1,
    })

    old_doc = Doc.objects(name="one").only("value").modify(set__value=2)
    assert old_doc.to_mongo() == {"_id": doc.id, "value": 1}
    doc.value = 2
    mocker.assert_equal()


def test_modify_document_empty(db, mocker):
    mocker.mock()

    with pytest.raises(InvalidDocumentError):
        Doc().modify({})

    mocker.assert_equal()


def test_modify_document_invalid_query(db, mocker):
    doc1 = mocker.mock({
        "id": ObjectId(),
        "name": "one",
        "value": 1,
    })

    doc2 = mocker.mock({
        "id": ObjectId(),
        "name": "two",
        "value": 2,
    })

    with pytest.raises(InvalidQueryError):
        doc1.modify(dict(id=doc2.id), set__value=20)

    mocker.assert_equal()


def test_modify_document_match_another_document(db, mocker):
    doc1 = mocker.mock({
        "name": "one",
        "value": 1,
    })

    doc2 = mocker.mock({
        "name": "two",
        "value": 2,
    })

    assert not doc1.modify(dict(name=doc2.name), set__value=20)

    mocker.assert_equal()


def test_modify_document_not_exists(db, mocker):
    mocker.mock({
        "name": "one",
        "value": 1,
    })

    doc = mocker.mock({
        "id": ObjectId(),
        "name": "two",
        "value": 2,
    }, save=False, add=False)

    assert not doc.modify(dict(name=doc.name), set__value=20)

    mocker.assert_equal()


def test_modify_document_update(db, mocker):
    mocker.mock({
        "name": "one",
        "value": 1,
    })

    doc = mocker.mock({
        "id": ObjectId(),
        "name": "two",
        "value": 2,
        "embedded": EmbeddedDoc(int_value=10, string_value="test")
    })

    doc_copy = doc._from_son(doc.to_mongo())

    # this changes must go away
    doc.name = "twenty"
    doc.embedded.int_value = 100
    doc.embedded.string_value = "change"

    assert doc.modify(dict(name="two"), set__value=20, set__embedded__int_value=99, unset__embedded__string_value=True)
    doc_copy.value = 20
    doc_copy.embedded.int_value = 99
    del doc_copy.embedded.string_value

    assert doc.to_json() == doc_copy.to_json()
    assert doc._get_changed_fields() == []

    mocker.assert_equal()
