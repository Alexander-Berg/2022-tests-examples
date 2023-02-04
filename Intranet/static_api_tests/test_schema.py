# coding: utf-8
from __future__ import unicode_literals

from . import SCHEMAS


def test_get_field_schema():
    schema = SCHEMAS['person_test']
    field = schema.get_field_schema('login')
    assert field == {'is_leaf': True, 'type': 'string'}

    field2 = schema.get_field_schema('name.first.en')
    assert field2 == {'is_leaf': True, 'type': 'string'}

    field2 = schema.get_field_schema('official.is_ext')
    assert field2 == {'is_leaf': True, 'type': 'boolean'}

    field2 = schema.get_field_schema('official')
    assert not field2['is_leaf']
