import attr
import pytest

from intranet.table_flow.src.rules import logic


def test_create_parse_simple():
    input_ = [
        ['in_first_in', 'in_second_in', 'out_first_out', 'out_second_out'],
        ['>1', 'fds', 'wer1', 'qwe1'],
        ['2', 'fds', 'wer2', 'qwe2'],
    ]
    rule_obj = logic.parse_rules(input_)
    expected = {
        'in_fields': {
            'first_in': {'field_type': 'int'},
            'second_in': {'field_type': 'str'},
        },
        'out_fields': {
            'first_out': {'field_type': 'str'},
            'second_out': {'field_type': 'str'},
        },
        'cases': [
            {
                'checks': {
                    'first_in': {'gt': 1},
                    'second_in': {'eq': 'fds'},
                },
                'out': {
                    'first_out': 'wer1',
                    'second_out': 'qwe1',
                },
            },
            {
                'checks': {
                    'first_in': {'eq': 2},
                    'second_in': {'eq': 'fds'},
                },
                'out': {
                    'first_out': 'wer2',
                    'second_out': 'qwe2',
                },
            },
        ]
    }
    assert attr.asdict(rule_obj) == expected


@pytest.mark.parametrize(
    'input_, expected_types',
    [
        (
            [['in_first:int', 'in_second:str']],
            {'first': 'int', 'second': 'str'},
        ),
        (
            [
                ['in_first', 'in_second'],
                ['10', 'test'],
            ],
            {'first': 'int', 'second': 'str'},
        ),
        (
            [
                ['in_first'],
                ['<10'],
            ],
            {'first': 'int'},
        ),
        (
            [
                ['in_first'],
                ['<B'],
            ],
            {'first': 'str'},
        ),
        (
            [
                ['in_first:str'],
                ['10'],
            ],
            {'first': 'str'},
        ),
    ]
)
def test_types_detects_correctly(input_, expected_types):
    rule_obj = logic.parse_rules(input_)

    expected_in_fields = {k: {'field_type': v} for k, v in expected_types.items()}
    assert attr.asdict(rule_obj)['in_fields'] == expected_in_fields


@pytest.mark.parametrize(
    'condition, field_type, expected',
    [
        ('> 1', 'int', {'gt': 1}),
        ('> 1', 'str', {'gt': '1'}),
        (' >=1', 'int', {'ge': 1}),
        (' >=1', 'str', {'ge': '1'}),
        ('<1', 'int', {'lt': 1}),
        ('<1', 'str', {'lt': '1'}),
        (' <= 1  ', 'int', {'le': 1}),
        (' <= 1  ', 'str', {'le': '1'}),
        ('2-10', 'int', {'le': 10, 'ge': 2}),
        ('2 -  10', 'int', {'le': 10, 'ge': 2}),

        ('!=1', 'int', {'ne': 1}),
        ('!=sad', 'str', {'ne': 'sad'}),

        ('1', 'int', {'eq': 1}),
        ('>1a', 'str', {'eq': '>1a'}),
        ('<=as', 'str', {'eq': '<=as'}),
        ('>=1 1', 'str', {'eq': '>=1 1'}),
        ('fsa', 'str', {'eq': 'fsa'}),
        ('1-asd', 'str', {'eq': '1-asd'}),
        ('asd-2', 'str', {'eq': 'asd-2'}),
        ('asd-fe', 'str', {'eq': 'asd-fe'}),
        ('2<', 'str', {'eq': '2<'}),
        (
            'sad; 12;1s; sd-as; <2; asd ',
            'str',
            {'contains': ['sad', '12', '1s', 'sd-as', '<2', 'asd']},
        ),
        ('1;2;5', 'int', {'contains': [1, 2, 5]}),
    ]
)
def test_create_check(condition, field_type, expected):
    res = logic.create_check(condition, field_type)
    assert res == expected
