
import itertools
from unittest import mock

import openpyxl
import pytest

from payplatform.balance_support_dev.tools.common.lib.utils import fill_json
from payplatform.balance_support_dev.tools.json2xlsx.lib.main import main, XLSX_ROW_LIMIT


def patched_main(input_filename, output_filename):
    with mock.patch('sys.argv', [__name__, '-i', input_filename, '-o', output_filename]):
        main()
    return


def validate_output(input_content, output_content):
    output_content = [
        [
            [cell for cell in row_data] for row_data in page]
        for page in output_content
    ]
    if not list(filter(None, output_content)):
        assert not input_content
        return

    field_names = input_content[0].keys()
    for idx, page in enumerate(output_content):
        assert set(page[0]) == set(field_names)
        page_content = [dict(itertools.zip_longest(page[0], row_data)) for row_data in page[1:]]
        for input_row, page_row in zip(input_content[idx * XLSX_ROW_LIMIT:(idx + 1) * XLSX_ROW_LIMIT], page_content):
            assert input_row == page_row

    return


@pytest.mark.parametrize('input_data, expected_output', [
    ([], [[]]),

    ([
        {'COL0': 'test0', 'COL1': '1.5', 'COL2': None}
    ], [
        [['COL0', 'COL1', 'COL2'], ['test0', '1.5']]
    ]),

    ([
        {'COL0': 'test0', 'COL1': None, 'COL2': '2'}
    ], [
        [['COL0', 'COL1', 'COL2'], ['test0', None, '2']]
    ]),

    ([
        {'COL0': 'test0', 'COL1': '1.5', 'COL2': '2'},
        {'COL0': '0.5', 'COL1': 'test1', 'COL2': '2'}
    ], [
        [['COL0', 'COL1', 'COL2'], ['test0', '1.5', '2'], ['0.5', 'test1', '2']]
    ]),

    # ([
    #     {'A': '1'}
    # ] * 1050000, [
    #     [['A'], ['1'] * (XLSX_ROW_LIMIT - 1)],
    #     [['A'], ['1'] * (1050000 - (XLSX_ROW_LIMIT - 1))]
    # ])
])
def test_valid_inputs(input_filename, output_filename, input_data, expected_output):
    fill_json(input_filename, input_data)

    patched_main(input_filename, output_filename)

    with open(output_filename, 'rb') as output_file:
        wb = openpyxl.load_workbook(output_file, read_only=True)
        output_content = [
            [
                [cell.value and str(cell.value) for cell in row] for row in page.rows]
            for page in wb.worksheets
        ]
        validate_output(input_data, output_content)
        assert output_content == expected_output

    return


@pytest.mark.parametrize('input_data, expectation, error', [
    ([
        {'COL0': 'test0', 'COL1': '1.5', 'COL2': '2'},
        {'COL0': '0.5', 'COL1': 'test1', 'COL3': '2'}
    ], pytest.raises(RuntimeError), 'Different keysets in JSON array')
])
def test_invalid_inputs(input_filename, output_filename, input_data, expectation, error):
    fill_json(input_filename, input_data)

    with expectation as exc:
        patched_main(input_filename, output_filename)

    assert error in str(exc)

    return
