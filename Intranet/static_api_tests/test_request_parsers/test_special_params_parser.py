# coding: utf-8
from __future__ import unicode_literals

import pytest

from pretend import stub

from django.http import QueryDict
from django.conf import settings
from django.core.validators import ValidationError

from static_api.request_parsers.special_params import SpecialParamsParser as Parser, _query_fields_gen
from static_api.fields import Fieldset

from .. import SCHEMAS



class TestFieldset(Fieldset):
    thumbnail = {
        '_id': 0,
        'id': 1,
        'login': 1,
    }

    one = {
        '_id': 0,
        'official': 0,
        'uid': 0,
        'id': 0,
        'foobar.buz': 0,
    }


resource = stub(
    schema=SCHEMAS['person_test'],
    default_filters={},
    ignore_fields={},
    indexes_hint=None,
    fieldset_cls=TestFieldset,
)

@pytest.mark.parametrize('query, expected', [
    ('', {'_doc': False}),
    ('', {'_one': False}),
    ('', {'_pretty': False}),
    ('', {'_write': False}),
    ('', {'_page': 1}),
    ('', {'_limit': settings.PAGE_SIZE}),
    ('', {'_query': {}}),
    ('', {'_sort': []}),
    ('', {'_fields': {u'_id': 0, u'id': 1, u'login': 1}}),
    ('_page=20', {'_page': 20}),
    ('_doc=0', {'_doc': False}),
    ('_doc=1', {'_doc': True}),
    ('_one=0', {'_one': False}),
    ('_pretty=0', {'_pretty': False}),
    ('_pretty=1', {'_pretty': True}),
    ('_write=0', {'_write': False}),
    ('_write=1', {'_write': True}),
    ('_page=1', {'_page': 1}),
    ('_page=20', {'_page': 20}),
    ('_limit=1', {'_limit': 1}),
    ('_limit=100', {'_limit': 100}),
    ('_query=id==10', {'_query': {'id': 10}}),
    ('_sort=id,-login', {'_sort': ['id', '-login']}),
    ('_fields=login,id,invalid,invalid.,invalid.$,invalid.$.id', {'_fields': {'_id': 0, 'login': 1, 'id': 1}}),
])
def test_parser_field_valid(query, expected):
    parser = Parser(resource, TestFieldset.thumbnail.keys(), QueryDict(query))
    assert parser.is_valid(), dict(parser.errors)
    for key, value in expected.items():
        assert key in parser.cleaned_data
        if value in (None, True, False):
            assert parser.cleaned_data[key] is value
        else:
            assert parser.cleaned_data[key] == value


@pytest.mark.parametrize('query, expected, permitted', [
    ('_fields=_all', {'_fields': {key: 1 for key in resource.schema.flat_leaf}}, set(resource.schema.flat)),
    (
        '_one=1',
        {'_one': True, '_fields': {
            '_id': 0, 'idd': 1, 'login': 1,
            'foobar.bar': 1, 'foobar.foo.en': 1, 'foobar.foo.ru': 1,
            'name.first.en': 1, 'name.first.ru': 1, 'name.last.en': 1, 'name.last.ru': 1
        }},
        {'idd', 'login', 'foobar.bar', 'foobar.foo.en', 'foobar.foo.ru',
         'name.first.en', 'name.first.ru', 'name.last.en', 'name.last.ru'},
    ),
    (
        '_fields=official&_debug=1',
        {'_fields': {
            '_id': 0, 'official.is_boss': 1, 'official.is_ext': 1,
            'official': 1, 'official.is_robot.ru': 1, 'official.is_robot.en': 1,
        }},
        set(resource.schema.flat),
    ),
    ('_debug=1', {'_fields': {'_id': 0, 'id': 1, 'login': 1, }}, {'id', 'login'}),
])
def test_parser_field_valid_with_permissions(query, expected, permitted):
    parser = Parser(resource, permitted, QueryDict(query))
    assert parser.is_valid(), dict(parser.errors)
    for key, value in expected.items():
        assert key in parser.cleaned_data
        if value in (None, True, False):
            assert parser.cleaned_data[key] is value
        else:
            assert parser.cleaned_data[key] == value


@pytest.mark.parametrize('query', [
    '_doc=3',
    '_one=666',
    '_pretty=42',
    '_write=WAT',
    '_page=0',
    '_page=TEXT',
    '_limit=0',
    '_limit=TEXT',
    '_query=INVALID',
    '_sort=INVALID',
    '_fields=official,foobar',
    '_fields=_all',
    '_one=1',
])
def test_parser_field_invalid(query):
    parser = Parser(resource, QueryDict(query))
    assert not parser.is_valid(), {'cd': dict(parser.cleaned_data), 'err': parser.errors}


def test__query_fields_gen():
    data = {
        'a': '248',
        'b': 'sys',
        'l': {'$geoWithin': {'$center': [[1, 2], 3]}},
        '$or': [{'a': 1}, {'b': 2}],
        '$not': {'a': 1},
        'd': [1, 2, 3],
        'f': {'foo': 1},
    }
    assert set(_query_fields_gen(data)) == {'a', 'b', 'l', 'a', 'b', 'a', 'd', 'f'}


def test__check_field_and_roles():
    parser = Parser(resource, permitted_fields=[])
    with pytest.raises(ValidationError):
        parser._check_field_and_access('invalid')

    with pytest.raises(ValidationError):
        parser._check_field_and_access('name')

    with pytest.raises(ValidationError):
        parser._check_field_and_access('login')

    parser = Parser(resource, permitted_fields=['name'])
    assert parser._check_field_and_access('name') is None


@pytest.mark.parametrize(
    'query,errors',
    [
        ('uid=10101267', {}),
        ('uid=10101267&_fileds=id,foobar.buz', {}),
        ('_fields=id,official', {}),
        ('unknown=10101267', {'_query': ['Unknown field `unknown`']}),  # `unknown` in filter
        ('uid=123&_fields=id,uid,unknown', {}),  # `unknown` in _fields
    ]
)
def test_check_fields_with_permissions(settings, query, errors):
    settings.STATIC_API_CHECK_FIELDS_ACCESS = True

    parser = Parser(resource, settings.STATIC_API_WILDCARD_FIELD_ACCESS, QueryDict(query))

    assert parser.is_valid() != bool(errors)
    assert parser.errors == errors


@pytest.mark.parametrize(
    'query,permitted,unavailable',
    [
        ('uid=10101267', {'uid'}, {'_fields': ['login', 'id']}),
        ('uid=10101267', {'uid', 'login'}, {'_fields': ['id']}),
        ('uid=10101267', {'uid', 'login', 'id'}, {}),

        ('uid=10101267&id=1000', {'uid'}, {'_fields': ['login', 'id'], '_query': ['id']}),
        ('uid=10101267&id=1000', {'uid', 'id'}, {'_fields': ['login']}),

        ('official.is_robot.ru=test', {'uid', 'id'}, {'_fields': ['login'], '_query': ['official.is_robot.ru']}),

        (
            'uid=10101267&_fields=official',
            {'uid', 'login', 'id'},
            {'_fields': [
                'official.is_robot.ru',
                'official.is_robot.en',
                'official.is_ext',
                'official.is_boss',
                'official',
            ]},
        ),
    ]
)
def test_check_fields_with_permissions(settings, query, permitted, unavailable):
    settings.STATIC_API_CHECK_FIELDS_ACCESS = True
    parser = Parser(resource, permitted, QueryDict(query))

    assert parser.is_valid() != bool(unavailable)
    if unavailable:
        for key, field_names in unavailable.items():
            assert key in parser.errors
            for field_name in field_names:
                error_text = 'Forbidden field `%s`' % field_name
                assert error_text in parser.errors[key]
