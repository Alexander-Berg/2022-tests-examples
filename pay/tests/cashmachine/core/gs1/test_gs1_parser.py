from typing import Dict

import pytest

from kkt_srv.cashmachine.core.gs1.gs1_parser import *


def gs1(gtin: str = None, serial_number: str = None, price_per_unit: str = None,
        additional_elements: Dict[int, str] = None):
    elements = {}
    if gtin:
        elements[GTIN_TAG_ID] = gtin
    if serial_number:
        elements[SERIAL_NUMBER_TAG_ID] = serial_number
    if price_per_unit:
        elements[PRICE_PER_UNIT_TAG_ID] = price_per_unit
    if additional_elements:
        elements.update(additional_elements)
    return GS1ItemCode(elements=elements)


@pytest.mark.parametrize(
    ('reason', 'data'),
    [
        ('no tag number at start', 'abcdef',),
        ('invalid tag number at start', '1abcdef',),
        # See next test scenario
        # ('gtin is shorter than 14 symbols', '0101234567890'),
        # ('gtin contains non-numeric symbols', '0101234567890abcd',),
        ('serial_number contains non-7bit ASCII symbol', '21ущъэльэ'),
        ('serial_number contains non-allowed symbol', '21    '),
        ('no tag number after end of previous element', '01012345678901234' + 'abcdef',),
        ('no delimiter after end of non-last var-size element', '21JgXJ5.T' + '01012345678901234'),
    ]
)
def test_gs1_parse_fails_on_illformed_data(reason, data):
    assert GS1ItemCode.from_text(data) == None, f'Should fail, reason=[{reason}]'


@pytest.mark.parametrize(
    ('data', 'result'),
    [
        ('0101234567890', gs1(additional_elements={101: '234567890'})),
        ('0101234567890abcd', gs1(additional_elements={101: '234567890abcd'})),
    ]
)
def test_gs1_parse_unexpectedly_correct_data(data, result):
    assert GS1ItemCode.from_text(data) == result


@pytest.mark.parametrize(
    ('data_equivalents', 'result'),
    [
        (
            [
                '0104600439931256',
                '\u00e80104600439931256',   # FNC1 symbol should start data sequence, but usually is not present
                '0104600439931256\u00e8',   # FNC1 or GS symbol may terminate data sequence, but it is not necessary
                '0104600439931256\u001d',
            ],
            gs1('04600439931256'),
        ),
        (
            [
                '010460043993125621JgXJ5.T',
                '0104600439931256\u001d21JgXJ5.T',  # FNC1 or GS symbol may be present at the end of fixed-size
                '0104600439931256\u00e821JgXJ5.T',  # element string, but it is not necessary
                '21JgXJ5.T\u001d0104600439931256',  # Delimiter symbol must be present at the end of var-size element
            ],
            gs1('04600439931256', 'JgXJ5.T'),
        ),
        (
            [
                '010460043993125621JgXJ5.T\u001d8005112000',        # Elements may be present in any order as long as
                '21JgXJ5.T\u001d8005112000\u001d0104600439931256',  # delimiters are used after each var-size element
                '8005112000\u001d0104600439931256\u001d21JgXJ5.T',
            ],
            gs1('04600439931256', 'JgXJ5.T', '112000'),
        ),
        (
            ['010460043993125621JgXJ5.T\u001d8005112000\u001d666XXXXX'],
            gs1('04600439931256', 'JgXJ5.T', '112000', {666: 'XXXXX'}),
        ),
    ]
)
def test_gs1_parse_correct_data(data_equivalents, result):
    for data in data_equivalents:
        assert GS1ItemCode.from_text(data) == result
