# coding: utf-8
from irt.multik.pylib.yt_orm import fields

import pytest

import datetime
import inspect
import time


@pytest.fixture(scope="session")
def all_fields():
    return [
        field for field in inspect.getmembers(fields)
        if inspect.isclass(field) and issubclass(field, fields.Field)
    ]


def test_none(all_fields):
    for field in all_fields:
        assert field().to_yt(None) is None
        assert field().to_query(None) == "null"


def test_int():
    field = fields.IntegerField()
    assert field.to_yt(3) == 3
    assert field.to_yt(3.5) == 3
    assert field.to_yt("3") == 3
    with pytest.raises(ValueError):
        field.to_yt("")

    assert field.to_query(3) == "3"
    assert field.to_query(3.5) == "3"
    assert field.to_query("3") == "3"
    with pytest.raises(ValueError):
        field.to_yt("")


def test_string():
    field = fields.CharField()
    assert field.to_yt(3) == "3"
    assert field.to_yt(3.5) == "3.5"
    assert field.to_yt("3") == "3"
    assert field.to_yt("Юникод") == u"Юникод"
    assert field.to_yt(u"Юникод") == u"Юникод"
    assert field.to_yt(b"ascii") == "ascii"

    assert field.to_query(3) == "'3'"
    assert field.to_query(3.5) == "'3\\.5'"
    assert field.to_query("3") == "'3'"
    assert field.to_query("Юникод") == u"'Юникод'"
    assert field.to_query(u"Юникод") == u"'Юникод'"
    assert field.to_query(b"ascii") == "'ascii'"


def test_datetime():
    field = fields.DateTimeField()

    ts = int(time.time())
    dt = datetime.datetime.fromtimestamp(ts)

    assert field.to_yt(0) == 0
    assert field.to_yt(dt) == ts
    with pytest.raises(TypeError):
        field.to_yt("")

    assert field.to_python(ts) == dt
