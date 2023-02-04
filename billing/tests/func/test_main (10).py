from contextlib import contextmanager
import csv
import io
import json
from unittest import mock
import zipfile

import openpyxl
import pytest

from billing.nirvana_reports.operations.filetype_transformer.lib.main import (
    main,
    QueryTaskKeys,
    ReportKeys,
)


@contextmanager
def does_not_raise():
    yield


def patched_main(
    report_cfg, attachments_archive, output_report_cfg, output_attachments_archive
):
    args = [
        __name__,
        '-r',
        str(report_cfg),
        '-a',
        str(attachments_archive),
        '-or',
        str(output_report_cfg),
        '-oa',
        str(output_attachments_archive),
    ]

    with mock.patch('sys.argv', args):
        main()

    return


@pytest.mark.parametrize(
    'report, attachments, expectation, error, expected_output_report, expected_output_attachments',
    [
        (
            {'queries': [{'filename': 'test1'}, {'filename': 'test1'}]},
            {},
            pytest.raises(Exception),
            'Duplicate',
            None,
            None,
        ),
        (
            {
                'queries': [
                    {'filename': 'test0'},
                    {'filename': 'test1'},
                    {'filename': 'test1'},
                ]
            },
            {},
            pytest.raises(Exception),
            'Duplicate',
            None,
            None,
        ),
        (
            {'queries': [{'filename': 'test0'}]},
            {},
            pytest.raises(Exception),
            'Malformed node',
            None,
            None,
        ),
        (
            {
                'queries': [
                    {'filename': 'test0', 'dbname': 'db', 'query': '', 'filetype': ''},
                    {'filename': 'test1'},
                ]
            },
            {},
            pytest.raises(Exception),
            'Malformed node',
            None,
            None,
        ),
        (
            {
                'queries': [
                    {'filename': 'test0', 'dbname': 'db', 'query': '', 'filetype': ''},
                    {'filename': 'test1', 'dbname': 'db', 'query': '', 'filetype': ''},
                ]
            },
            {},
            pytest.raises(Exception),
            'Malformed node',
            None,
            None,
        ),
        (
            {
                'queries': [
                    {'filename': 'test0', 'dbname': 'db', 'query': '', 'filetype': ''},
                    {'filename': 'test1', 'dbname': 'db', 'query': '', 'filetype': ''},
                ]
            },
            {'test2': []},
            pytest.raises(Exception),
            'Malformed node',
            None,
            None,
        ),
        (
            {
                'queries': [
                    {'filename': 'test0', 'dbname': 'db', 'query': '', 'filetype': ''},
                    {'filename': 'test1', 'dbname': 'db', 'query': '', 'filetype': ''},
                ]
            },
            {'test1': []},
            pytest.raises(Exception),
            'Malformed node',
            None,
            None,
        ),
        (
            {
                'queries': [
                    {'filename': 'test0', 'dbname': 'db', 'query': '', 'filetype': ''},
                    {'filename': 'test1', 'dbname': 'db', 'query': '', 'filetype': ''},
                ]
            },
            {'test1': [], 'test2': []},
            pytest.raises(Exception),
            'Malformed node',
            None,
            None,
        ),
        (
            {
                'queries': [
                    {
                        'filename': 'test0',
                        'dbname': 'db',
                        'query': '',
                        'filetype': 'xlsx',
                    },
                    {'filename': 'test1', 'dbname': 'db', 'query': '', 'filetype': ''},
                ]
            },
            {'test0': [], 'test1': []},
            pytest.raises(Exception),
            'Malformed node',
            None,
            None,
        ),
        (
            {
                'queries': [
                    {
                        'filename': 'test0',
                        'dbname': 'db',
                        'query': '',
                        'filetype': 'xlsx',
                    },
                    {
                        'filename': 'test1',
                        'dbname': 'db',
                        'query': '',
                        'filetype': 'csv',
                    },
                ]
            },
            {'test0': [], 'test1': []},
            pytest.raises(Exception),
            'No header',
            None,
            None,
        ),
        (
            {
                'queries': [
                    {
                        'filename': 'test0',
                        'dbname': 'db',
                        'query': '',
                        'filetype': 'xlsx',
                    },
                    {
                        'filename': 'test1',
                        'dbname': 'db',
                        'query': '',
                        'filetype': 'csv',
                    },
                ]
            },
            {'test0': [], 'test1': [['id']]},
            pytest.raises(Exception),
            'No header',
            None,
            None,
        ),
        (
            {
                'queries': [
                    {
                        'filename': 'test0',
                        'dbname': 'db',
                        'query': '',
                        'filetype': 'xlsx',
                    },
                    {
                        'filename': 'test1',
                        'dbname': 'db',
                        'query': '',
                        'filetype': 'csv',
                    },
                    {
                        'filename': 'test2',
                        'dbname': 'db',
                        'query': '',
                        'filetype': 'xlsx',
                    },
                ]
            },
            {
                'test0': [
                    ['field1', 'заголовок2', 'заголовок 3'],
                    ['value 1', 'значение2', 'значение3'],
                ],
                'test1': [
                    ['field1', 'заголовок2', 'заголовок 3'],
                    ['1.5', '12,0', '-5.0'],
                ],
                'test2': [
                    ['field1', 'заголовок2', 'заголовок 3'],
                    ['1.5', '12,0', '-5.0'],
                ],
            },
            does_not_raise(),
            None,
            {
                'queries': [
                    {
                        'filename': 'test0',
                        'dbname': 'db',
                        'query': '',
                        'filetype': 'xlsx',
                    },
                    {
                        'filename': 'test1',
                        'dbname': 'db',
                        'query': '',
                        'filetype': 'csv',
                    },
                    {
                        'filename': 'test2',
                        'dbname': 'db',
                        'query': '',
                        'filetype': 'xlsx',
                    },
                ]
            },
            {
                'test0': [
                    ['field1', 'заголовок2', 'заголовок 3'],
                    ['value 1', 'значение2', 'значение3'],
                ],
                'test1': [
                    ['field1', 'заголовок2', 'заголовок 3'],
                    ['1.5', '12,0', '-5.0'],
                ],
                'test2': [
                    ['field1', 'заголовок2', 'заголовок 3'],
                    [1.5, '12,0', -5],
                ],
            },
        ),
    ],
)
def test_main(
    tmp_path,
    report,
    attachments,
    expectation,
    error,
    expected_output_report,
    expected_output_attachments,
):
    report_cfg = tmp_path / 'report_cfg'
    with open(report_cfg, 'w') as f:
        json.dump(report, f)

    attachments_archive = tmp_path / 'attachments_archive'
    with zipfile.ZipFile(attachments_archive, 'w', zipfile.ZIP_DEFLATED) as zf:
        for filename, data in attachments.items():
            tmp = tmp_path / 'tmp'
            with open(tmp, 'w') as f:
                wr = csv.writer(f, delimiter=',')
                wr.writerows(data)
            with open(tmp, 'r') as f:
                zf.writestr(filename, f.read())

    output_report_cfg = tmp_path / 'output_report_cfg'
    output_attachments_archive = tmp_path / 'output_attachments_archive'
    with expectation as exc:
        patched_main(
            report_cfg,
            attachments_archive,
            output_report_cfg,
            output_attachments_archive,
        )

    if error:
        assert error in str(exc)
    else:
        with open(output_report_cfg, 'r') as f:
            output_report = json.load(f)
            assert output_report == expected_output_report

        with zipfile.ZipFile(
            output_attachments_archive, 'r', zipfile.ZIP_DEFLATED
        ) as zf:
            filetypes = {
                query_task[QueryTaskKeys.FILENAME.value]: query_task[
                    QueryTaskKeys.FILETYPE.value
                ]
                for query_task in report[ReportKeys.QUERIES.value]
            }
            assert set(zf.namelist()) == set(expected_output_attachments.keys())
            for filename, data in expected_output_attachments.items():
                if filetypes[filename] == 'csv':
                    with zf.open(filename, 'r') as f:
                        rd = csv.reader(io.TextIOWrapper(f), delimiter=',')
                        assert list(rd) == data
                elif filetypes[filename] == 'xlsx':
                    with zf.open(filename, 'r') as f:
                        wb = openpyxl.load_workbook(f, read_only=True)
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
                        assert output_data == data

    return
