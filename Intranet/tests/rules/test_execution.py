import pytest

from django.forms import ValidationError
from intranet.table_flow.src.rules import domain_objs


@pytest.mark.parametrize('check_dict,value,expected', [
    ({'lt': 10}, 11, False),
    ({'lt': 10}, 10, False),
    ({'lt': 10}, 9, True),
    ({'le': 10}, 11, False),
    ({'le': 10}, 10, True),
    ({'le': 10}, 9, True),
    ({'gt': 10}, 11, True),
    ({'gt': 10}, 10, False),
    ({'gt': 10}, 9, False),
    ({'ge': 10}, 11, True),
    ({'ge': 10}, 10, True),
    ({'ge': 10}, 9, False),

    ({'ne': 10}, 10, False),
    ({'ne': 10}, 9, True),
    ({'eq': 10}, 10, True),
    ({'eq': 10}, 9, False),
    ({'ne': 'str'}, 'str', False),
    ({'ne': 'str'}, 'str2', True),
    ({'eq': 'str'}, 'str', True),
    ({'eq': 'str'}, 'str2', False),

    ({'contains': ['a', 'b', 'c']}, 'a', True),
    ({'contains': ['a', 'b', 'c']}, 'd', False),
    ({'contains': [1, 2, 3]}, 1, True),
    ({'contains': [1, 2, 3]}, 4, False),

    ({'le': 10, 'ge': 5}, 6, True),
    ({'le': 10, 'ge': 5}, 4, False),
    ({'le': 10, 'ge': 5}, 11, False),
    ({'le': 10, 'ge': 5}, [20, 9, 0], True),
    ({'le': 10, 'ge': 5}, [20, 12, 0], False),
])
def test_check_execution(check_dict, value, expected):
    check = domain_objs.Check(check_dict)
    assert check.execute(value) == expected


@pytest.mark.parametrize('in_values, expected', [
    ({'grade': 10, 'department': 'ext'}, {'salary': 'mass'}),
    ({'grade': 10, 'department': 'not_ext'}, None),
    ({'grade': 11, 'department': 'not_ext'}, None),
    ({'grade': 11, 'department': 'ext'}, None),
])
def test_case_execution(in_values, expected):
    case = domain_objs.Case.from_dict({
        'checks': {
            'grade': {'eq': 10},
            'department': {'eq': 'ext'},
        },
        'out': {
            'salary': 'mass'
        },
    })

    assert case.execute(in_values) == expected


@pytest.mark.parametrize('in_values, expected', [
    ({'grade': 9, 'department': 'ext'}, {'grade': 9, 'department': 'ext'}),
    ({'grade': '9', 'department': 'ext'}, {'grade': 9, 'department': 'ext'}),
    ({'grade': '9', 'department': 10}, {'grade': 9, 'department': '10'}),
])
def test_rule_validation_clean_good_values(in_values, expected):
    rule = domain_objs.Rule.from_dict({
        'in_fields': {
            'grade': {'field_type': 'int'},
            'department': {'field_type': 'str'},
        },
        'out_fields': {},
        'cases': []
    })
    assert rule.validate_in_values(in_values) == expected


@pytest.mark.parametrize('in_values, bad_fields', [
    ({'department': 'ext'}, ['grade']),
    ({'grade': 9}, ['department']),
    ({}, ['department', 'grade']),
    ({'grade': 'not_num', 'department': 'ext'}, ['grade']),
])
def test_rule_validation_raise_bad_value(in_values, bad_fields):
    rule = domain_objs.Rule.from_dict({
        'in_fields': {
            'grade': {'field_type': 'int'},
            'department': {'field_type': 'str'},
        },
        'out_fields': {},
        'cases': []
    })
    with pytest.raises(ValidationError) as excinfo:
        rule.validate_in_values(in_values)
    assert sorted(excinfo.value.message_dict.keys()) == sorted(bad_fields)


@pytest.mark.parametrize('in_values, expected', [
    ({'grade': 9}, {'salary': 'low'}),
    ({'grade': 10}, {'salary': 'low'}),
    ({'grade': 11}, {'salary': 'medium'}),
    ({'grade': 16}, {'salary': 'medium'}),
    ({'grade': 17}, None),
    ({'grade': 9, 'fetch_all': 'true'}, {'results': [{'salary': 'low'}]}),
    ({'grade': 10, 'fetch_all': 'true'}, {'results': [{'salary': 'low'}, {'salary': 'medium'}]}),
    ({'grade': 17, 'fetch_all': 'false'}, None)
])
def test_rule_extecution(in_values, expected):
    rule = domain_objs.Rule.from_dict({
        'in_fields': {
            'grade': {'field_type': 'int'},
        },
        'out_fields': {
            'salary': {'field_type': 'str'},
        },
        'cases': [
            {'checks': {'grade': {'le': 10}}, 'out': {'salary': 'low'}},
            {'checks': {'grade': {'ge': 10, 'le': 16}}, 'out': {'salary': 'medium'}},
        ]
    })

    assert rule.execute(in_values) == expected
