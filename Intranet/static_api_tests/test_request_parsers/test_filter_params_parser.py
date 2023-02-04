# coding: utf-8
from __future__ import unicode_literals

from django.http import QueryDict
import jsonschema
from pretend import stub, raiser
import pytest

from static_api.request_parsers import FilterParamsParser as Parser

from . import schema

@pytest.mark.parametrize('query, expected', [
    ('id=666', {'id': 666}),
    ('login=zombik', {'login': 'zombik'}),
    ('login=', {'login': ''}),
    ('login="zombik"', {'login': 'zombik'}),
    ('favourite_number=10', {'favourite_number': 10}),
    ('is_dismissed=true', {'is_dismissed': True}),
    ('is_dismissed=false', {'is_dismissed': False}),
    ('quit_at=null', {'quit_at': None}),
    ('quit_at=none', {'quit_at': None}),
    ('quit_at=', {'quit_at': None}),
    ('quit_at=2014-01-01', {'quit_at': '2014-01-01'}),
    ('employment=full', {'employment': 'full'}),
    ('employment=full,partial', {
        'employment': {'$in': ['full', 'partial']},
    }),
    ('login=joe,', {
        'login': {'$in': ['joe', '']},
    }),
    ('tshirt=2', {'tshirt': 2}),
    ('akward=10', {'akward': 10}),
    ('akward=blah', {'akward': 'blah'}),
    ('more_akward=10', {'more_akward': 10}),
    ('more_akward=blah', {'more_akward': 'blah'}),
    ('somestring=blah', {'somestring': 'blah'}),
])
def test_parser_field_valid(query, expected):
    resource = stub(schema=stub(
        get_field_schema=lambda key: schema[key],
        validate_field=lambda key, val: True,
    ))

    parser = Parser(QueryDict(query), resource)
    assert parser.is_valid(), parser.errors
    for key, value in expected.items():
        assert key in parser.cleaned_data
        if value in (None, True, False):
            assert parser.cleaned_data[key] is value
        else:
            assert parser.cleaned_data[key] == value


@pytest.mark.parametrize('query', [
    '_wrong_key_=why not?',
    'unknown=whatever',
    'id=hello',
    'favourite_number=blargh!',
    'is_dismissed=no',
    'employment=no,thanks',
    'tshirt=10',
])
def test_parser_field_invalid(query):
    resource = stub(schema=stub(
        get_field_schema=lambda key: schema.get(key),
        validate_field=raiser(jsonschema.ValidationError('ohshit')),
    ))

    parser = Parser(QueryDict(query), resource)
    assert not parser.is_valid(), dict(parser.cleaned_data)
