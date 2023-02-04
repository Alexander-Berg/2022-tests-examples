import decimal
import itertools
import json
from collections import OrderedDict
from unittest import mock

import pytest

from payplatform.balance_support_dev.tools.common.lib.utils import fill_workbook
from payplatform.balance_support_dev.tools.xlsx2json.lib.main import main


def patched_main(input_filename, output_filename):
    with mock.patch(
        'sys.argv', [__name__, '-i', input_filename, '-o', output_filename]
    ):
        main()
    return


def validate_output(input_content, output_content, offset=0):
    output_content = json.loads(output_content)
    if not list(filter(None, input_content)):
        assert not output_content
        return

    field_names = input_content[0][0]
    input_content_merged = []
    for page in input_content:
        input_content_merged.extend(
            OrderedDict(itertools.zip_longest(field_names, row_data))
            for row_data in page[1:]
        )
    input_content = input_content_merged
    assert len(input_content) == len(output_content)
    for input_row, output_row in zip(input_content, output_content):
        assert len(input_row) == len(output_row) + offset
        assert all(k in field_names for k in output_row)

    return


@pytest.mark.parametrize(
    'input_data, expected_output, offset',
    [
        ([], [], 0),
        ([[['COL0', 'COL1', 'COL2']]], [], 0),
        (
            [[['COL0', 'COL1', 'COL2'], ['test0', 'test1']]],
            [{'COL0': 'test0', 'COL1': 'test1', 'COL2': None}],
            0,
        ),
        (
            [
                [['COL0', 'COL1', 'COL2'], ['test0', 1.5, 2]],
                [['COL0', 'COL1', 'COL2'], [0.5, 'test1', 2]],
            ],
            [
                {'COL0': 'test0', 'COL1': '1.5', 'COL2': '2'},
                {'COL0': '0.5', 'COL1': 'test1', 'COL2': '2'},
            ],
            0,
        ),
        (
            [[['COL0', 'COL1', None], ['test0', 'test1', None]]],
            [{'COL0': 'test0', 'COL1': 'test1'}],
            1,
        ),
        (
            [[['COL0', None, 'COL2'], ['test0', None, 'test1']]],
            [{'COL0': 'test0', 'COL2': 'test1'}],
            1,
        ),
        (
            [
                [
                    [None, 'COL0', None, 'COL2', None],
                    [None, 'test0', None, 'test1', None],
                ]
            ],
            [{'COL0': 'test0', 'COL2': 'test1'}],
            1,
        ),
    ],
)
def test_valid_inputs(
    input_filename, output_filename, input_data, expected_output, offset
):
    fill_workbook(input_filename, input_data)

    patched_main(input_filename, output_filename)

    with open(output_filename, 'r') as output_file:
        output_content = output_file.read()
        validate_output(input_data, output_content, offset)
        assert json.loads(output_content) == expected_output

    return


@pytest.mark.parametrize(
    'input_data, expectation, error',
    [
        (
            [[[0, 'COL1', 'COL2']]],
            pytest.raises(RuntimeError),
            'Invalid header A on sheet',
        ),
        (
            [[['COL0', 'COL0', 'COL2']]],
            pytest.raises(RuntimeError),
            'Field names must be unique',
        ),
        (
            [[['COL0', 'COL1', 'COL2'], ['test0', decimal.Decimal('1.50'), 2, 3.5]]],
            pytest.raises(RuntimeError),
            'Invalid header D on sheet',
        ),
        (
            [
                [['COL0', 'COL1', 'COL2'], ['test0', decimal.Decimal('1.50'), 2]],
                [['COL0', 'COL1', 'COL3'], ['test0', decimal.Decimal('1.50'), 2]],
            ],
            pytest.raises(RuntimeError),
            'Header on sheet',
        ),
        (
            [
                [
                    ['COL0', 'COL1', 'COL2', None],
                    ['test0', decimal.Decimal('1.50'), 2, 3.5],
                ]
            ],
            pytest.raises(RuntimeError),
            'Invalid header D on sheet',
        ),
        (
            [
                [
                    [None, 'COL0', None, 'COL2', None],
                    [None, 'test0', decimal.Decimal('1.50'), 2, None],
                ]
            ],
            pytest.raises(RuntimeError),
            'Invalid header C on sheet',
        ),
    ],
)
def test_invalid_inputs(
    input_filename, output_filename, input_data, expectation, error
):
    fill_workbook(input_filename, input_data)

    with expectation as exc:
        patched_main(input_filename, output_filename)

    assert error in str(exc)

    return
