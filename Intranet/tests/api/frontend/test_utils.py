# coding: utf-8


import pytest
from django.db.models import F

from idm.api.frontend.utils import OrderingAlias, OrderingField


def test_field_parsing():
    field = OrderingField.from_string('field')
    assert field.name == 'field'
    assert isinstance(field.lookup, F)
    assert field.lookup.name == 'field'
    assert field.descending is False

    field = OrderingField.from_string('relation__attribute')
    assert field.name == 'relation'
    assert field.lookup.name == 'relation__attribute'
    assert field.descending is False

    field = OrderingField.from_string('-role')
    assert field.name == 'role'
    assert field.lookup.name == 'role'
    assert field.descending is True


def test_field_encoding():
    assert str(OrderingField("relation__attribute")) == "relation__attribute"
    assert str(OrderingField("field", descending=True)) == "-field"


def test_field_invert():
    f1 = OrderingField("rel__attribute")

    f2 = ~f1
    assert f2 is not f1
    assert f2.name == f1.name
    assert f2.lookup == f1.lookup
    assert f2.descending is True

    f3 = ~f2
    assert f3 is not f2
    assert f3.descending is False


def test_alias_constructor():
    f1 = OrderingField("f1")
    alias = OrderingAlias(f1, 'f2')

    assert len(alias.fields) == 2
    assert alias.fields[0] is f1
    assert isinstance(alias.fields[1], OrderingField)
    assert alias.fields[1].name == 'f2'

    with pytest.raises(TypeError):
        OrderingAlias('f1', 2)


def test_alias_invert():
    a1 = OrderingAlias(OrderingField("f1", descending=True), OrderingField("f2"))
    a2 = ~a1

    assert a2 is not a1

    assert len(a2.fields) == len(a1.fields)

    assert a2.fields[0] is not a1.fields[0]
    assert a2.fields[0].name == 'f1'
    assert a2.fields[0].descending is False

    assert a2.fields[1] is not a1.fields[0]
    assert a2.fields[1].name == 'f2'
    assert a2.fields[1].descending is True
