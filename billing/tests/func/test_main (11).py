from contextlib import contextmanager
import csv
import io
import jinja2.exceptions
import json
import os.path
from unittest import mock
import zipfile

import pytest

from billing.nirvana_reports.operations.inline_only_transformer.lib.main import main


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


BODY_TEMPLATE_0 = \
    '''{% for row in context['test0'] %}
{% if row['field1'] -%}
    {{ row['заголовок2'] }} с НДС: {{ row['заголовок 3'] }}
{% else -%}
    {{ row['заголовок2'] }} без НДС: {{ row['заголовок 3'] }}
{% endif %}
{% endfor %}'''

BODY_TEMPLATE_1 = \
    '''{% for row in context['test1'] %}
{{ row['заголовок2'] }} : {{ row['заголовок 3'] }}
{% endfor %}'''


@pytest.mark.parametrize(
    'report, attachments, expectation, expected_output_report, expected_output_attachments',
    [
        (
            {'queries': [{'filename': 'test1'}, {'filename': 'test1'}]},
            {},
            pytest.raises(Exception, match='Duplicate'),
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
            pytest.raises(Exception, match='Duplicate'),
            None,
            None,
        ),
        (
            {
                'body_template': 'toss a coin to your witcher',
                'queries': [
                    {
                        'filename': 'test0',
                        'dbname': 'db',
                        'query': '',
                        'filetype': 'xlsx',
                        'inline_only': False,
                    },
                    {
                        'filename': 'test1',
                        'dbname': 'db',
                        'query': '',
                        'filetype': 'csv',
                        'inline_only': False,
                    },
                ],
            },
            {
                'test0': [
                    ['field1', 'заголовок2', 'заголовок 3'],
                    ['value 1', 'значение2', 'значение3'],
                    ['value 4', 'значение5', 'значение6'],
                ],
                'test1': [
                    ['field1', 'заголовок2', 'заголовок 3'],
                    ['1.0', '12', 'test'],
                ],
            },
            does_not_raise(),
            {
                'body_template': 'toss a coin to your witcher',
                'queries': [
                    {
                        'filename': 'test0',
                        'dbname': 'db',
                        'query': '',
                        'filetype': 'xlsx',
                        'inline_only': False,
                    },
                    {
                        'filename': 'test1',
                        'dbname': 'db',
                        'query': '',
                        'filetype': 'csv',
                        'inline_only': False,
                    },
                ],
            },
            {
                'test0': [
                    ['field1', 'заголовок2', 'заголовок 3'],
                    ['value 1', 'значение2', 'значение3'],
                    ['value 4', 'значение5', 'значение6'],
                ],
                'test1': [
                    ['field1', 'заголовок2', 'заголовок 3'],
                    ['1.0', '12', 'test'],
                ],
            },
        ),
        (
            {
                'body_template': 'toss a coin to your witcher',
                'queries': [
                    {
                        'filename': 'test0',
                        'dbname': 'db',
                        'query': '',
                        'filetype': 'xlsx',
                        'inline_only': True,
                    },
                    {
                        'filename': 'test1',
                        'dbname': 'db',
                        'query': '',
                        'filetype': 'csv',
                        'inline_only': False,
                    },
                ],
            },
            {
                'test0': [
                    ['field1', 'заголовок2', 'заголовок 3'],
                    ['value 1', 'значение2', 'значение3'],
                    ['value 4', 'значение5', 'значение6'],
                ],
                'test1': [
                    ['field1', 'заголовок2', 'заголовок 3'],
                    ['1.0', '12', 'test'],
                ],
            },
            does_not_raise(),
            {
                'body_template': 'toss a coin to your witcher',
                'queries': [
                    {
                        'filename': 'test1',
                        'dbname': 'db',
                        'query': '',
                        'filetype': 'csv',
                        'inline_only': False,
                    },
                ],
            },
            {
                'test1': [
                    ['field1', 'заголовок2', 'заголовок 3'],
                    ['1.0', '12', 'test'],
                ],
            },
        ),
        (
            {
                'body_template': 'toss a coin to your witcher',
                'queries': [
                    {
                        'filename': 'test0',
                        'dbname': 'db',
                        'query': '',
                        'filetype': 'xlsx',
                        'inline_only': True,
                    },
                    {
                        'filename': 'test1',
                        'dbname': 'db',
                        'query': '',
                        'filetype': 'csv',
                        'inline_only': True,
                    },
                ],
            },
            {
                'test0': [
                    ['field1', 'заголовок2', 'заголовок 3'],
                    ['value 1', 'значение2', 'значение3'],
                    ['value 4', 'значение5', 'значение6'],
                ],
                'test1': [
                    ['field1', 'заголовок2', 'заголовок 3'],
                    ['1.0', '12', 'test'],
                ],
            },
            does_not_raise(),
            {'body_template': 'toss a coin to your witcher', 'queries': []},
            None,
        ),
        (
            {
                'body_template': BODY_TEMPLATE_1,
                'queries': [
                    {
                        'filename': 'test0',
                        'dbname': 'db',
                        'query': '',
                        'filetype': 'xlsx',
                        'inline_only': False,
                    },
                    {
                        'filename': 'test1',
                        'dbname': 'db',
                        'query': '',
                        'filetype': 'csv',
                        'inline_only': False,
                    },
                ],
            },
            {
                'test0': [
                    ['field1', 'заголовок2', 'заголовок 3'],
                    ['value 1', 'значение2', 'значение3'],
                    ['value 4', 'значение5', 'значение6'],
                ],
                'test1': [
                    ['field1', 'заголовок2', 'заголовок 3'],
                    ['1.0', '12', 'test'],
                ],
            },
            does_not_raise(),
            {
                'body_template':
                    '''12 : test
''',
                'queries': [
                    {
                        'filename': 'test0',
                        'dbname': 'db',
                        'query': '',
                        'filetype': 'xlsx',
                        'inline_only': False,
                    },
                    {
                        'filename': 'test1',
                        'dbname': 'db',
                        'query': '',
                        'filetype': 'csv',
                        'inline_only': False,
                    },
                ],
            },
            {
                'test0': [
                    ['field1', 'заголовок2', 'заголовок 3'],
                    ['value 1', 'значение2', 'значение3'],
                    ['value 4', 'значение5', 'значение6'],
                ],
                'test1': [
                    ['field1', 'заголовок2', 'заголовок 3'],
                    ['1.0', '12', 'test'],
                ],
            },
        ),
        (
            {
                'body_template': BODY_TEMPLATE_0,
                'queries': [
                    {
                        'filename': 'test0',
                        'dbname': 'db',
                        'query': '',
                        'filetype': 'xlsx',
                        'inline_only': True,
                    },
                    {
                        'filename': 'test1',
                        'dbname': 'db',
                        'query': '',
                        'filetype': 'csv',
                        'inline_only': False,
                    },
                ],
            },
            {
                'test0': [
                    ['field1', 'заголовок2', 'заголовок 3'],
                    ['value 1', 'значение2', 'значение3'],
                    ['value 4', 'значение5', 'значение6'],
                ],
                'test1': [
                    ['field1', 'заголовок2', 'заголовок 3'],
                    ['1.0', '12', 'test'],
                ],
            },
            does_not_raise(),
            {
                'body_template':
                    '''значение2 с НДС: значение3
значение5 с НДС: значение6
''',
                'queries': [
                    {
                        'filename': 'test1',
                        'dbname': 'db',
                        'query': '',
                        'filetype': 'csv',
                        'inline_only': False,
                    },
                ],
            },
            {
                'test1': [
                    ['field1', 'заголовок2', 'заголовок 3'],
                    ['1.0', '12', 'test'],
                ],
            },
        ),
        (
            {
                'body_template': '{{context[\'test\']}}',
                'queries': [
                    {
                        'filename': 'test0',
                        'dbname': 'db',
                        'query': '',
                        'filetype': 'xlsx',
                        'inline_only': True,
                    },
                    {
                        'filename': 'test1',
                        'dbname': 'db',
                        'query': '',
                        'filetype': 'csv',
                        'inline_only': False,
                    },
                ],
            },
            {
                'test0': [
                    ['field1', 'заголовок2', 'заголовок 3'],
                    ['value 1', 'значение2', 'значение3'],
                    ['value 4', 'значение5', 'значение6'],
                ],
                'test1': [
                    ['field1', 'заголовок2', 'заголовок 3'],
                    ['1.0', '12', 'test'],
                ],
            },
            pytest.raises(jinja2.exceptions.UndefinedError),
            None,
            None
        ),
    ],
)
def test_main(
    tmp_path,
    report,
    attachments,
    expectation,
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
    with expectation:
        patched_main(
            report_cfg,
            attachments_archive,
            output_report_cfg,
            output_attachments_archive,
        )

        with open(output_report_cfg, 'r') as f:
            output_report = json.load(f)
            assert output_report == expected_output_report

        if expected_output_attachments is None:
            assert not os.path.isfile(output_attachments_archive)
        else:
            with zipfile.ZipFile(
                output_attachments_archive, 'r', zipfile.ZIP_DEFLATED
            ) as zf:
                assert set(zf.namelist()) == set(expected_output_attachments.keys())
                for filename, data in expected_output_attachments.items():
                    with zf.open(filename, 'r') as f:
                        rd = csv.reader(io.TextIOWrapper(f), delimiter=',')
                        assert list(rd) == data
