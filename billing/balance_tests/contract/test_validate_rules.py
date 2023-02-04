# coding=utf-8
import pytest
from balance.printform.rules import *


@pytest.mark.parametrize(
    'params',
    [
        (
            Rule(Interleave([Terminal('editable', 'FIRM', 1), Terminal('editable', 'COUNTRY', 1)])),
            [
                ['FIRM1', Rule(Terminal('editable', 'FIRM', 1)), '1'],
                ['COUNTRY1', Rule(Terminal('editable', 'COUNTRY', 1)), '2']
            ],
            [{'caption': 'FIRM1', 'external_id': '1'}, {'caption': 'COUNTRY1', 'external_id': '2'}]
        ),
        (
            Rule(Terminal('editable', 'COUNTRY', 1)),
            [['FIRM1COUNTRY1', Rule(Interleave([Terminal('editable', 'FIRM', 1),
                                                Terminal('editable', 'COUNTRY', 1)])), '1']],
            []
        ),
        (
            Rule(Not(Alternation([Not(Terminal('editable', 'FIRM', 1)), Not(Terminal('editable', 'COUNTRY', 1))]))),
            [
                ['FIRM1', Rule(Terminal('editable', 'FIRM', 1)), '1'],
                ['COUNTRY1', Rule(Terminal('editable', 'COUNTRY', 1)), '2']
            ],
            [{'caption': 'FIRM1', 'external_id': '1'}, {'caption': 'COUNTRY1', 'external_id': '2'}]
        ),
        (
            Rule(Not(Interleave([Not(Terminal('editable', 'FIRM', 1)), Not(Terminal('editable', 'COUNTRY', 1))]))),
            [['FIRM1COUNTRY1', Rule(Interleave([Terminal('editable', 'FIRM', 1),
                                                Terminal('editable', 'COUNTRY', 1)])), '1']],
            []
        ),
        (
            Rule(Not(Interleave([Not(Terminal('editable', 'FIRM', 1)), Not(Terminal('editable', 'COUNTRY', 1))]))),
            [
                ['FIRM1', Rule(Terminal('editable', 'FIRM', 1)), '1'],
                ['COUNTRY1', Rule(Terminal('editable', 'COUNTRY', 1)), '2']
            ],
            [{'caption': 'FIRM1', 'external_id': '1'}, {'caption': 'COUNTRY1', 'external_id': '2'}]
        ),
        (
            Rule(Interleave([Terminal('editable', 'FIRM', 1), Terminal('editable', 'COUNTRY', 1)])),
            [
                ['FIRM1', Rule(Not(Terminal('editable', 'FIRM', 1))), '1'],
                ['COUNTRY1', Rule(Terminal('editable', 'COUNTRY', 1)), '2']
            ],
            [{'caption': 'COUNTRY1', 'external_id': '2'}]
        ),
        (
            Rule(Interleave([Terminal('editable', 'FIRM', 1), Terminal('editable', 'DISCOUNT_POLICY_TYPE', 1),
                             Terminal('editable', 'DISCOUNT_POLICY_TYPE', 'ANY')])),
            [
                ['FIRM1', Rule(Terminal('editable', 'FIRM', 1)), '1'],
                ['COUNTRY1', Rule(Terminal('editable', 'DISCOUNT_POLICY_TYPE', 1)), '2']
            ],
            [{'caption': 'FIRM1', 'external_id': '1'}, {'caption': 'COUNTRY1', 'external_id': '2'}]
        ),
        (
            Rule(Interleave([Terminal('editable', 'FIRM', 1), Terminal('editable', 'DISCOUNT_POLICY_TYPE', 1),
                             Terminal('editable', 'DISCOUNT_POLICY_TYPE', 'ANY')])),
            [
                ['FIRM1', Rule(Terminal('editable', 'FIRM', 1)), '1'],
                ['COUNTRY1', Rule(Terminal('editable', 'DISCOUNT_POLICY_TYPE', 'ANY')), '2']
            ],
            [{'caption': 'FIRM1', 'external_id': '1'}, {'caption': 'COUNTRY1', 'external_id': '2'}]
        ),
        (
            Rule(Interleave([Terminal('editable', 'FIRM', 1), Terminal('editable', 'DISCOUNT_POLICY_TYPE', 1),
                             Terminal('editable', 'DISCOUNT_POLICY_TYPE', 'ANY')])),
            [
                ['FIRM1', Rule(Terminal('editable', 'FIRM', 1)), '1'],
                ['COUNTRY1', Rule(Not(Terminal('editable', 'DISCOUNT_POLICY_TYPE', 'ANY'))), '2']
            ],
            [{'caption': 'FIRM1', 'external_id': '1'}]
        ),
    ]
)
def test_validate_rules(params):
    new_rule, current_rules, result = params

    assert validate(new_rule, current_rules) == result


@pytest.mark.parametrize(
    'params',
    [
        (Rule(Terminal('editable', 'FIRM', 1)), {'editable:FIRM:1'}, True),
        (Rule(Not(Alternation([Not(Terminal('editable', 'FIRM', 1)), Not(Terminal('editable', 'COUNTRY', 1))]))),
         {'editable:FIRM:1', 'editable:COUNTRY:1'}, True),
        (Rule(Not(Alternation([Not(Terminal('editable', 'FIRM', 1)), Not(Terminal('editable', 'COUNTRY', 1))]))),
         {'editable:FIRM:1'}, False),
        (Rule(Not(Interleave([Not(Terminal('editable', 'FIRM', 1)), Not(Terminal('editable', 'COUNTRY', 1))]))),
         {'editable:FIRM:1', 'editable:COUNTRY:1'}, True),
        (Rule(Not(Interleave([Not(Terminal('editable', 'FIRM', 1)), Not(Terminal('editable', 'COUNTRY', 1))]))),
         {'editable:FIRM:1'}, True),
        (Rule(Interleave([Terminal('editable', 'FIRM', 1), Terminal('editable', 'DISCOUNT_POLICY_TYPE', 'ANY')])),
         {'editable:FIRM:1', 'editable:DISCOUNT_POLICY_TYPE:1', 'editable:DISCOUNT_POLICY_TYPE:ANY'}, True),
        (Rule(Interleave([Terminal('editable', 'FIRM', 1), Terminal('editable', 'DISCOUNT_POLICY_TYPE', 'ANY')])),
         {'editable:FIRM:1'}, False),
        (Rule(Interleave([Terminal('editable', 'FIRM', 1), Not(Terminal('editable', 'DISCOUNT_POLICY_TYPE', 'ANY'))])),
         {'editable:FIRM:1', 'editable:DISCOUNT_POLICY_TYPE:1', 'editable:DISCOUNT_POLICY_TYPE:ANY'}, False),
        (Rule(Interleave([Terminal('editable', 'FIRM', 1), Not(Terminal('editable', 'DISCOUNT_POLICY_TYPE', 'ANY'))])),
         {'editable:FIRM:1'}, True),
    ]
)
def test_evaling_rules(params):
    rule, attrvalue_set, result = params

    assert rule.eval(attrvalue_set) == result

