from contextlib import contextmanager
import csv
import io
import json
from unittest import mock
import zipfile

import pytest

from billing.nirvana_reports.operations.attachments_type_transformer.lib.main import (
    main,
    ATTACHMENTS_FILENAME,
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
            {
                'attachments_type': 'txt',
                'queries': [{'filename': 'test0'}, {'filename': 'test1'}],
            },
            {},
            pytest.raises(Exception),
            'Malformed node',
            None,
            None,
        ),
        (
            {
                'attachments_type': 'raw',
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
                ],
            },
            {
                'test0': [
                    ['field1', 'заголовок2', 'заголовок 3'],
                    ['value 1', 'значение2', 'значение3'],
                ],
                'test1': [
                    ['field1', 'заголовок2', 'заголовок 3'],
                    ['1.0', '12', 'test'],
                ],
            },
            does_not_raise(),
            None,
            {
                'attachments_type': 'raw',
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
                ],
            },
            {
                'test0': [
                    ['field1', 'заголовок2', 'заголовок 3'],
                    ['value 1', 'значение2', 'значение3'],
                ],
                'test1': [
                    ['field1', 'заголовок2', 'заголовок 3'],
                    ['1.0', '12', 'test'],
                ],
            },
        ),
        (
            {
                'attachments_type': 'zip',
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
                ],
            },
            {
                'test0': [
                    ['field1', 'заголовок2', 'заголовок 3'],
                    ['value 1', 'значение2', 'значение3'],
                ],
                'test1': [
                    ['field1', 'заголовок2', 'заголовок 3'],
                    ['1.0', '12', 'test'],
                ],
            },
            does_not_raise(),
            None,
            {
                'attachments_type': 'zip',
                'queries': [{'filename': ATTACHMENTS_FILENAME}],
            },
            {
                'test0': [
                    ['field1', 'заголовок2', 'заголовок 3'],
                    ['value 1', 'значение2', 'значение3'],
                ],
                'test1': [
                    ['field1', 'заголовок2', 'заголовок 3'],
                    ['1.0', '12', 'test'],
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
            if output_report['attachments_type'] == 'raw':
                zf_ = zf
            elif output_report['attachments_type'] == 'zip':
                zf_ = zipfile.ZipFile(
                    zf.open(ATTACHMENTS_FILENAME, 'r'), 'r', zipfile.ZIP_DEFLATED
                )
                assert len(zf.namelist()) == 1 and ATTACHMENTS_FILENAME in zf.namelist()
            assert set(zf_.namelist()) == set(expected_output_attachments.keys())
            for filename, data in expected_output_attachments.items():
                with zf_.open(filename, 'r') as f:
                    rd = csv.reader(io.TextIOWrapper(f), delimiter=',')
                    assert list(rd) == data

    return
