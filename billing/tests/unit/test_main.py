from contextlib import contextmanager

import openpyxl
import pytest

from billing.nirvana_reports.operations.filetype_transformer.lib.main import (
    xlsx_transformer,
    null_transformer,
)


@contextmanager
def does_not_raise():
    yield


@pytest.mark.parametrize(
    'csv_data, expectation, error, expected_side_result',
    [
        ('', pytest.raises(Exception), 'No header', None),
        (
            '"field1","заголовок2","заголовок 3"\n"value 1","значение2","значение3"\n"1.0","12","test"',
            does_not_raise(),
            None,
            [
                ['field1', 'заголовок2', 'заголовок 3'],
                ['value 1', 'значение2', 'значение3'],
                [1, 12, 'test'],
            ],
        ),
        (
            '"field1","заголовок2"\n"value 1","значение2","значение3"\n"1.5","12,0","test"',
            does_not_raise(),
            None,
            [
                ['field1', 'заголовок2'],
                ['value 1', 'значение2', 'значение3'],
                [1.5, '12,0', 'test'],
            ],
        ),
        (
            '"field1","заголовок2","заголовок 3"\n"value 1","значение2"\n"1.0"',
            does_not_raise(),
            None,
            [
                ['field1', 'заголовок2', 'заголовок 3'],
                ['value 1', 'значение2'],
                [1],
            ],
        ),
    ],
)
def test_xlsx_transformer(tmp_path, csv_data, expectation, error, expected_side_result):
    csv_ = tmp_path / 'csv_data'
    with open(csv_, 'w') as f:
        f.write(csv_data)
    output = tmp_path / 'output_data'

    with open(csv_, 'rb') as csv_stream, open(
        output, 'wb'
    ) as output_stream, expectation as exc:
        xlsx_transformer(csv_stream, output_stream)

    if error:
        assert error in str(exc)
    else:
        with open(output, 'rb') as output_stream:
            wb = openpyxl.load_workbook(output_stream, read_only=True)
            output_data = []
            for idx, ws in enumerate(wb.worksheets):
                rows = ws.rows
                try:
                    header = next(rows)
                except StopIteration:
                    continue
                ws_data = []
                if idx == 0:
                    ws_data.append([cell.value for cell in header])
                for row in rows:
                    row_data = []
                    for cell in row:
                        row_data.append(cell.value)
                    ws_data.append(row_data)
                output_data.extend(ws_data)
            assert output_data == expected_side_result

    return


@pytest.mark.parametrize(
    'csv_data, expected_side_result',
    [
        ('', ''),
        (
            '"field1","заголовок2","заголовок 3"\n"value 1","значение2","значение3"\n"1.0","12","test"',
            '"field1","заголовок2","заголовок 3"\n"value 1","значение2","значение3"\n"1.0","12","test"',
        ),
    ],
)
def test_null_transformer(tmp_path, csv_data, expected_side_result):
    csv_ = tmp_path / 'csv_data'
    with open(csv_, 'w') as f:
        f.write(csv_data)
    output = tmp_path / 'output_data'

    with open(csv_, 'r') as csv_stream, open(output, 'w') as output_stream:
        null_transformer(csv_stream, output_stream)

    with open(csv_, 'r') as csv_stream, open(output, 'r') as output_stream:
        assert csv_stream.read() == output_stream.read() == expected_side_result

    return
