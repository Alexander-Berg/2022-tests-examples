from datetime import date

import pytest

from django.core.exceptions import ValidationError

from intranet.femida.src.candidates.bulk_upload.forms import (
    _parse_partial_date,
    CommaSeparatedField,
    CommaSeparatedMappingField,
)


def _clean_field(field, new_value, **kwargs):
    kwargs.setdefault('old_value', '')
    kwargs.setdefault('required', False)
    kwargs.setdefault('trim_on_empty', True)
    kwargs.setdefault('base_initial', {})
    kwargs.setdefault('base_data', {})
    return field.clean(new_value, **kwargs)


@pytest.mark.parametrize('value,expected', [
    ('2018', date(2018, 1, 1)),
    ('2018-04', date(2018, 4, 1)),
    ('2018-08-11', date(2018, 8, 11)),
    ('01.02.1993', None),
    ('2017-13-02', None),
])
def test_parse_partial_date(value, expected):
    assert _parse_partial_date(value) == expected


@pytest.mark.parametrize('value,expected', [
    ('single_word', ['single_word']),
    ('word-1,word-2', ['word-1', 'word-2']),
    (' ,, word-1  , word-2  , word-3, ', ['word-1', 'word-2', 'word-3']),
    (',,,', []),
    ('', []),
])
def test_comma_separated_field(value, expected):
    field = CommaSeparatedField()
    result = _clean_field(field, value)
    assert result == expected


@pytest.mark.parametrize('data', [
    {  # 0. Для всего есть соответствие
        'value': '1,2,3',
        'mapping': {
            '1': 'a',
            '2': 'b',
            '3': 'c',
        },
        'expected': ['a', 'b', 'c'],
    },
    {  # 1. НЕ для всего есть соответствие (пропускаем лишние)
        'value': '1,2,3',
        'mapping': {
            '1': 'a',
        },
        'expected': ['a'],
    },
    {  # 2. НЕ для всего есть соответствие (кидаем исключение)
        'value': '1,2,3',
        'strict': True,
        'mapping': {
            '1': 'a',
        },
        'raises_error': True,
    },
    {  # 3. Пропускаем значения в другом регистре
        'value': 'a,B,c',
        'mapping': {
            'a': '1',
            'b': '2',
            'c': '3',
        },
        'expected': ['1', '3'],
    },
    {  # 4. Регистронезависимый маппинг
        'value': 'a,B,c',
        'mapping': {
            'a': '1',
            'b': '2',
            'c': '3',
        },
        'expected': ['1', '2', '3'],
        'case_sensitive': False,
    },
    {  # 5. Дублирующиеся значения
        'value': '2,1,3,2,1,3,2,1,2,2,2,2,3,1,1,1,1,2',
        'mapping': {
            '1': 'a',
            '2': 'b',
            '3': 'c',
        },
        'expected': ['b', 'a', 'c'],
    },
])
def test_comma_separated_mapping_field(data):
    value = data['value']
    mapping = data['mapping']
    expected = data.get('expected', None)
    raises_error = data.get('raises_error', False)

    initial = {
        '_mapping': {
            'data': mapping,
        },
    }
    field = CommaSeparatedMappingField(
        mapping_name='data',
        strict=data.get('strict', False),
        case_sensitive=data.get('case_sensitive', True),
    )
    clean = lambda: _clean_field(field, value, base_initial=initial)

    if raises_error:
        with pytest.raises(ValidationError):
            clean()
    else:
        assert expected == clean()
