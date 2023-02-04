from contextlib import contextmanager
import jinja2.exceptions

import pytest

from billing.monthclosing.operations.monograph_builder.lib.validators import (
    check_uniqueness,
    check_consistency,
    validate_query_task,
    validate_queries_cfg,
    validate_reports_cfg,
    validate_graph_cfg,
    validate_simple_email_report_cfg,
)


SER_CONST_PART = {
    'from_name': '',
    'from_email': '',
    'to': '',
    'cc': '',
    'bcc': '',
    'attachments_type': '',
    'skip_all_empty': '',
}


@contextmanager
def does_not_raise():
    yield


@pytest.mark.parametrize(
    'cfg, accessor, expectation',
    [
        (None, None, pytest.raises(TypeError)),
        (None, 'id', pytest.raises(TypeError)),
        ([{'id': '0'}], None, pytest.raises(KeyError)),
        ([{'id': '0'}, {None: '1'}], None, pytest.raises(KeyError)),
        ([{'id': '0'}], 'id2', pytest.raises(KeyError)),
        ([{'id2': '0'}, {'id': '1'}], 'id2', pytest.raises(KeyError)),
        ([{'id': '0'}, {'ID': '0'}], 'id', pytest.raises(KeyError)),
        ([{'id': '0'}, {'id': '0'}], 'id', pytest.raises(Exception, match='Duplicate')),
        (
            [{'id': '0'}, {'id': '1'}, {'id': '0'}],
            'id',
            pytest.raises(Exception, match='Duplicate'),
        ),
        (
            [{'id': '0', 'id2': '1'}, {'id': '0', 'id2': '2'}],
            'id',
            pytest.raises(Exception, match='Duplicate'),
        ),
        ([], None, does_not_raise()),
        ([], 'id', does_not_raise()),
        ([{'id': '0'}], 'id', does_not_raise()),
        ([{'id': '0'}, {'id': '1'}], 'id', does_not_raise()),
        (
            [{'id': '0', 'id2': '2'}, {'id': '1', 'id2': '2'}],
            'id',
            does_not_raise(),
        ),
    ],
)
def test_check_uniquness(cfg, accessor, expectation):
    with expectation:
        check_uniqueness(cfg, accessor)


@pytest.mark.parametrize(
    'node_keys, schema_keys, strict, expectation',
    [
        (None, {}, True, pytest.raises(TypeError)),
        ({}, None, True, pytest.raises(TypeError)),
        ({'a'}, {}, True, pytest.raises(Exception, match='Malformed node')),
        ({}, {'a'}, True, pytest.raises(Exception, match='Malformed node')),
        ({'A'}, {'a'}, True, pytest.raises(Exception, match='Malformed node')),
        ({'a', 'b'}, {'a'}, True, pytest.raises(Exception, match='Malformed node')),
        ({'b'}, {'a', 'b'}, True, pytest.raises(Exception, match='Malformed node')),
        (['a'], [], True, pytest.raises(Exception, match='Malformed node')),
        ([], ['a'], True, pytest.raises(Exception, match='Malformed node')),
        (['A'], ['a'], True, pytest.raises(Exception, match='Malformed node')),
        (['a', 'b'], ['a'], True, pytest.raises(Exception, match='Malformed node')),
        (['b'], ['a', 'b'], True, pytest.raises(Exception, match='Malformed node')),
        (['a'], ['a', 'a'], True, pytest.raises(Exception, match='Malformed node')),
        ([], ['a', 'a'], True, pytest.raises(Exception, match='Malformed node')),
        (['a', 'b'], ['b', 'a', 'b'], True, pytest.raises(Exception, match='Malformed node')),
        ({}, {}, True, does_not_raise()),
        ({'a'}, {'a'}, True, does_not_raise()),
        ({'a', 'b'}, {'b', 'a'}, True, does_not_raise()),
        ([], [], True, does_not_raise()),
        (['a'], ['a'], True, does_not_raise()),
        (['a', 'b'], ['b', 'a'], True, does_not_raise()),
        (['a', 'b'], ['b', 'a', 'b'], False, does_not_raise()),
        (['b', 'a', 'b'], ['a', 'b'], False, does_not_raise()),
        (['a', 'b'], ['a', 'c', 'b'], False, does_not_raise()),
        (
            ['a', 'c', 'b'],
            ['a', 'b'],
            False,
            pytest.raises(Exception, match='Malformed node'),
        ),
        ({}, {}, False, does_not_raise()),
        ({'a'}, {'a'}, False, does_not_raise()),
        ({'a', 'b'}, {'b', 'a'}, False, does_not_raise()),
        ([], [], False, does_not_raise()),
        (['a'], ['a'], False, does_not_raise()),
        (['a', 'b'], ['b', 'a'], False, does_not_raise()),
    ],
)
def test_check_consistency(node_keys, schema_keys, strict, expectation):
    with expectation:
        check_consistency(node_keys, schema_keys, strict)


@pytest.mark.parametrize(
    'queries_cfg, expectation',
    [
        ([{'id': '0'}, {'id': '0'}], pytest.raises(Exception, match='Duplicate')),
        (
            [{'id': '0'}, {'id': '1'}, {'id': '0'}],
            pytest.raises(Exception, match='Duplicate'),
        ),
        ([{'id2': '0'}], pytest.raises(KeyError)),
        ([{'id': '0'}, {'id2': '0'}], pytest.raises(KeyError)),
        ([{'id2': '0', 'id': '0'}], pytest.raises(Exception, match='Malformed node')),
        (
            [{'id': '0', 'query': ''}, {'id': '1'}],
            pytest.raises(Exception, match='Malformed node'),
        ),
        (
            [{'id': '0', 'query': ''}, {'id': '1', 'query': '', 'extra': ''}],
            pytest.raises(Exception, match='Malformed node'),
        ),
        (None, pytest.raises(TypeError)),
        ([], does_not_raise()),
        ([{'id': '0', 'query': ''}], does_not_raise()),
        ([{'id': '0', 'query': ''}, {'id': '1', 'query': ''}], does_not_raise()),
    ],
)
def test_validate_queries_cfg(queries_cfg, expectation):
    with expectation:
        validate_queries_cfg(queries_cfg)


@pytest.mark.parametrize(
    'query_task, queries_cfg, expectation',
    [
        ({}, {}, pytest.raises(KeyError)),
        ({}, {'0': ''}, pytest.raises(KeyError)),
        ({'query_id': '0', 'query': ''}, [], pytest.raises(Exception, match='No query')),
        (
            {'query_id': '0', 'query': ''},
            [{'1': ''}],
            pytest.raises(Exception, match='No query'),
        ),
        (
            {'query_id': '0', 'query': 'select 1 from dual'},
            [],
            pytest.raises(Exception, match='Single query'),
        ),
        (
            {'query_id': '0', 'query': 'select 1 from dual'},
            [{'0': ''}],
            pytest.raises(Exception, match='Single query'),
        ),
        ({'query_id': '', 'query': ''}, [], pytest.raises(Exception, match='Single query')),
        (
            {
                'query_id': '0',
                'query': '',
                'query_params': [
                    {'key': 'contract_id', 'value': ''},
                    {'key': 'contract_id', 'value': ''},
                ],
            },
            {'0': ''},
            pytest.raises(Exception, match='Duplicate'),
        ),
        (
            {
                'query_id': '0',
                'query': '',
                'query_params': [
                    {'key': 'dt', 'value': ''},
                    {'key': 'contract_id', 'value': ''},
                    {'key': 'contract_id', 'value': ''},
                ],
            },
            {'0': ''},
            pytest.raises(Exception, match='Duplicate'),
        ),
        (
            {
                'query_id': '0',
                'query': '',
                'query_params': [{'key': 'dt', 'value': '', 'extra': ''}],
            },
            {'0': ''},
            pytest.raises(Exception, match='Malformed node'),
        ),
        (
            {'query_id': '0', 'query': '', 'query_params': [{'key': 'dt'}]},
            {'0': ''},
            pytest.raises(Exception, match='Malformed node'),
        ),
        (
            {'query_id': '0', 'query': '', 'query_params': [{'value': ''}]},
            {'0': ''},
            pytest.raises(KeyError),
        ),
        (
            {
                'query_id': '0',
                'query': '',
                'query_params': [
                    {'key': 'contract_id', 'value': ''},
                    {'key': 'dt', 'value': '', 'extra': ''},
                ],
            },
            {'0': ''},
            pytest.raises(Exception, match='Malformed node'),
        ),
        (
            {
                'query_id': '0',
                'query': '',
                'query_params': [
                    {'key': 'contract_id', 'value': ''},
                    {'key': '', 'extra': ''},
                ],
            },
            {'0': ''},
            pytest.raises(Exception, match='Malformed node'),
        ),
        (
            {'query_id': '0', 'query': '', 'query_params': []},
            {'0': '{{}}'},
            pytest.raises(jinja2.exceptions.TemplateSyntaxError),
        ),
        (
            {'query_id': '0', 'query': '', 'query_params': []},
            {'0': '{{contract_id}}'},
            pytest.raises(Exception, match='Malformed node'),
        ),
        (
            {
                'query_id': '0',
                'query': '',
                'query_params': [{'key': 'ContractID', 'value': ''}],
            },
            {'0': '{{contract_id}}'},
            pytest.raises(Exception, match='Malformed node'),
        ),
        (
            {
                'query_id': '0',
                'query': '',
                'query_params': [
                    {'key': 'ContractID', 'value': ''},
                    {'key': 'contract_id', 'value': ''},
                ],
            },
            {'0': '{{contract_id}}'},
            pytest.raises(Exception, match='Malformed node'),
        ),
        (
            {
                'query_id': '0',
                'query': '',
                'query_params': [{'key': 'contract_id', 'value': ''}],
            },
            {'0': '{{contract_id}} {{ContractID}}'},
            pytest.raises(Exception, match='Malformed node'),
        ),
        (
            {
                'query_id': '0',
                'query': '',
                'query_params': [{'key': 'contract_id', 'value': ''}],
            },
            {'0': None},
            pytest.raises(Exception, match='Malformed node'),
        ),
        (
            {'query_id': '0', 'query': '', 'query_params': []},
            {'0': 'select 1 from dual'},
            does_not_raise(),
        ),
        (
            {
                'query_id': '0',
                'query': '',
                'query_params': [{'key': 'contract_id', 'value': ''}],
            },
            {'0': '{{contract_id}}'},
            does_not_raise(),
        ),
        (
            {
                'query_id': '0',
                'query': '',
                'query_params': [
                    {'key': 'contract_id', 'value': '0'},
                    {'key': 'dt', 'value': 'date\'1970-01-01\''},
                ],
            },
            {'0': 'select {{contract_id}} contract_id, {{dt}} dt from dual'},
            does_not_raise(),
        ),
        (
            {'query_id': '', 'query': '{{}}', 'query_params': []},
            {'0': '{{}}'},
            pytest.raises(jinja2.exceptions.TemplateSyntaxError),
        ),
        (
            {'query_id': '', 'query': '{{contract_id}}', 'query_params': []},
            {'0': '{{contract_id}}'},
            pytest.raises(Exception, match='Malformed node'),
        ),
        (
            {
                'query_id': '',
                'query': '{{contract_id}}',
                'query_params': [{'key': 'ContractID', 'value': ''}],
            },
            {'0': '{{contract_id}}'},
            pytest.raises(Exception, match='Malformed node'),
        ),
        (
            {
                'query_id': '',
                'query': '{{contract_id}}',
                'query_params': [
                    {'key': 'ContractID', 'value': ''},
                    {'key': 'contract_id', 'value': ''},
                ],
            },
            {'0': '{{contract_id}}'},
            pytest.raises(Exception, match='Malformed node'),
        ),
        (
            {
                'query_id': '',
                'query': '{{contract_id}} {{ContractID}}',
                'query_params': [{'key': 'contract_id', 'value': ''}],
            },
            {'0': '{{contract_id}} {{ContractID}}'},
            pytest.raises(Exception, match='Malformed node'),
        ),
        (
            {'query_id': '', 'query': 'select 1 from dual', 'query_params': []},
            {'0': 'select'},
            does_not_raise(),
        ),
        (
            {
                'query_id': '',
                'query': '{{contract_id}}',
                'query_params': [{'key': 'contract_id', 'value': ''}],
            },
            {'0': '{{contract_id}}'},
            does_not_raise(),
        ),
        (
            {
                'query_id': '',
                'query': 'select {{contract_id}} contract_id, {{dt}} dt from dual',
                'query_params': [
                    {'key': 'contract_id', 'value': '0'},
                    {'key': 'dt', 'value': 'date\'1970-01-01\''},
                ],
            },
            {'0': 'select'},
            does_not_raise(),
        ),
    ],
)
def test_validate_query_task(query_task, queries_cfg, expectation):
    with expectation:
        validate_query_task(query_task, queries_cfg)


@pytest.mark.parametrize(
    'reports_cfg, queries_cfg, expectation',
    [
        (
            [{'id': 'report0'}, {'id': 'report0'}],
            {},
            pytest.raises(Exception, match='Duplicate'),
        ),
        (
            [{'id': 'report0'}, {'id': 'report1'}, {'id': 'report0'}],
            {},
            pytest.raises(Exception, match='Duplicate'),
        ),
        ([{'id': 'report0'}], {}, pytest.raises(Exception, match='Missing mandatory keys')),
        (
            [{'id': 'report0', 'report_type': ''}],
            {},
            pytest.raises(Exception, match='Missing mandatory keys'),
        ),
        (
            [{'id': 'report0', 'queries': []}],
            {},
            pytest.raises(Exception, match='Missing mandatory keys'),
        ),
        (
            [
                {
                    'id': 'report-1',
                    'report_type': 'simple_email_report',
                    'queries': [],
                    'subject_template': '',
                    'body_template': '',
                    'template_params': '',
                },
                {'id': 'report0'},
            ],
            {},
            pytest.raises(Exception, match='Missing mandatory keys'),
        ),
        (
            [{'id': 'report0', 'report_type': '', 'queries': []}],
            {},
            pytest.raises(Exception, match='Unknown report type'),
        ),
        (
            [
                {'id': 'report-1', 'report_type': '', 'queries': []},
                {'id': 'report0', 'report_type': 'simple_email_report', 'queries': []},
            ],
            {},
            pytest.raises(Exception, match='Unknown report type'),
        ),
        (
            [
                {
                    'id': 'report0',
                    'report_type': 'simple_email_report',
                    'queries': [],
                    'subject_template': '',
                    'body_template': '',
                    'template_params': '',
                }
            ],
            {},
            does_not_raise(),
        ),
        (
            [
                {
                    'id': 'report0',
                    'report_type': 'simple_email_report',
                    'queries': [
                        {
                            'filename': '',
                            'filetype': '',
                            'dbname': '',
                            'query': ' ',
                            'query_id': '',
                            'query_params': '',
                        }
                    ],
                    'subject_template': '',
                    'body_template': '',
                    'template_params': '',
                },
                {
                    'id': 'report1',
                    'report_type': 'simple_email_report',
                    'queries': [
                        {
                            'filename': '',
                            'filetype': '',
                            'dbname': '',
                            'query': '',
                            'query_id': '0',
                            'query_params': [{'key': 'contract_id', 'value': ''}],
                        }
                    ],
                    'subject_template': '',
                    'body_template': '',
                    'template_params': '',
                },
            ],
            {'0': 'select {{contract_id}} from dual'},
            does_not_raise(),
        ),
    ],
)
def test_validate_reports_cfg(reports_cfg, queries_cfg, expectation):
    for report_cfg in reports_cfg:
        report_cfg.update(SER_CONST_PART)

    with expectation:
        validate_reports_cfg(reports_cfg, queries_cfg)


@pytest.mark.parametrize(
    'graph_cfg, reports_cfg, queries_cfg, expectation',
    [
        (
            [
                {
                    'id': 0,
                    'name_id': 'node1',
                    'title': '',
                    'dependencies': ['node0'],
                    'offset': 0,
                    'nirvana_operation': 'run',
                },
            ],
            {},
            {},
            does_not_raise(),
        ),
        (
            [
                {
                    'id': 0,
                    'name_id': 'node1',
                    'title': '',
                    'dependencies': ['node0'],
                    'offset': 0,
                    'nirvana_operation': 'simple_email_report',
                    'params': {},
                },
            ],
            {},
            {},
            pytest.raises(Exception, match='No report cfg'),
        ),
        (
            [
                {
                    'id': 0,
                    'name_id': 'node1',
                    'title': '',
                    'dependencies': ['node0'],
                    'offset': 0,
                    'nirvana_operation': 'simple_email_report',
                },
            ],
            {},
            {},
            pytest.raises(Exception, match='No report cfg'),
        ),
        (
            [
                {
                    'id': -1,
                    'name_id': 'node0',
                    'title': '',
                    'dependencies': [],
                    'offset': 0,
                    'nirvana_operation': 'run',
                },
                {
                    'id': 0,
                    'name_id': 'node1',
                    'title': '',
                    'dependencies': ['node0'],
                    'offset': 0,
                    'nirvana_operation': 'simple_email_report',
                },
            ],
            {
                'node1': {
                    'id': 'node1',
                    'report_type': 'simple_email_report',
                    'queries': [
                        {
                            'filename': '',
                            'filetype': '',
                            'dbname': '',
                            'query': '',
                            'query_id': '0',
                            'query_params': [{'key': 'contract_id', 'value': ''}],
                        },
                        {
                            'filename': '',
                            'filetype': '',
                            'dbname': '',
                            'query': '',
                            'query_id': '1',
                            'query_params': [
                                {'key': 'contract_id', 'value': '0'},
                                {'key': 'dt', 'value': 'sysdate'},
                            ],
                        },
                        {
                            'filename': '',
                            'filetype': '',
                            'dbname': '',
                            'query': 'select sysdate from dual',
                            'query_id': '',
                            'query_params': [],
                        },
                    ],
                    'subject_template': '',
                    'body_template': '',
                    'template_params': '',
                },
            },
            {
                '0': 'select {{contract_id}} from dual',
                '1': 'select {{ dt }}, {{ contract_id }} from dual',
            },
            does_not_raise(),
        ),
    ],
)
def test_validate_graph_cfg(graph_cfg, reports_cfg, queries_cfg, expectation):
    for name_id, report_cfg in reports_cfg.items():
        report_cfg.update(SER_CONST_PART)

    with expectation:
        validate_graph_cfg(graph_cfg, reports_cfg, queries_cfg)


@pytest.mark.parametrize(
    'report_cfg, expectation',
    [
        (
            {
                'id': 'report0',
                'report_type': 'simple_email_report',
                'queries': [],
                'subject_template': '',
                'body_template': '',
                'template_params': [],
                'extra': '',
            },
            pytest.raises(Exception, match='Malformed node'),
        ),
        (
            {
                'id': 'report0',
                'report_type': 'simple_email_report',
                'queries': [],
                'subject_template': '',
                'body_template': '',
                'template_params': [
                    {'key': 'param1', 'value': ''},
                    {'key': 'param1', 'value': ''},
                ],
            },
            pytest.raises(Exception, match='Duplicate'),
        ),
        (
            {
                'id': 'report0',
                'report_type': 'simple_email_report',
                'queries': [],
                'subject_template': '',
                'body_template': '',
                'template_params': [
                    {'key': 'param0', 'value': ''},
                    {'key': 'param1', 'value': ''},
                    {'key': 'param1', 'value': ''},
                ],
            },
            pytest.raises(Exception, match='Duplicate'),
        ),
        (
            {
                'id': 'report0',
                'report_type': 'simple_email_report',
                'queries': [],
                'subject_template': '',
                'body_template': '',
                'template_params': [{'key': 'param1'}],
            },
            pytest.raises(Exception, match='Malformed node'),
        ),
        (
            {
                'id': 'report0',
                'report_type': 'simple_email_report',
                'queries': [],
                'subject_template': '',
                'body_template': '',
                'template_params': [{'key': 'param0', 'value': ''}, {'key': 'param1'}],
            },
            pytest.raises(Exception, match='Malformed node'),
        ),
        (
            {
                'id': 'report0',
                'report_type': 'simple_email_report',
                'queries': [],
                'subject_template': '',
                'body_template': '',
                'template_params': [{'key': 'param0', 'value': 'a'}],
            },
            pytest.raises(Exception),
        ),
        (
            {
                'id': 'report0',
                'report_type': 'simple_email_report',
                'queries': [],
                'subject_template': '{{date}}',
                'body_template': '',
                'template_params': [],
            },
            pytest.raises(Exception, match='Malformed node'),
        ),
        (
            {
                'id': 'report0',
                'report_type': 'simple_email_report',
                'queries': [],
                'subject_template': '{{date}}',
                'body_template': '',
                'template_params': [{'key': 'param0', 'value': ''}],
            },
            pytest.raises(Exception, match='Malformed node'),
        ),
        (
            {
                'id': 'report0',
                'report_type': 'simple_email_report',
                'queries': [],
                'subject_template': '',
                'body_template': '{{date}}',
                'template_params': [],
            },
            pytest.raises(Exception, match='Malformed node'),
        ),
        (
            {
                'id': 'report0',
                'report_type': 'simple_email_report',
                'queries': [],
                'subject_template': '',
                'body_template': '{{date}}',
                'template_params': [{'key': 'param0', 'value': ''}],
            },
            pytest.raises(Exception, match='Malformed node'),
        ),
        (
            {
                'id': 'report0',
                'report_type': 'simple_email_report',
                'queries': [],
                'subject_template': '',
                'body_template': '{{date}}',
                'template_params': [{'key': 'param0', 'value': ''}],
            },
            pytest.raises(Exception, match='Malformed node'),
        ),
        (
            {
                'id': 'report0',
                'report_type': 'simple_email_report',
                'queries': [],
                'subject_template': '',
                'body_template': '{{date}}',
                'template_params': [{'key': 'date', 'value': 'date = 5'}],
            },
            pytest.raises(Exception, match='date: expected'),
        ),
        (
            {
                'id': 'report0',
                'report_type': 'simple_email_report',
                'queries': [],
                'subject_template': '{{date}}',
                'body_template': '{{text}} {{date}}',
                'template_params': [
                    {
                        'key': 'date',
                        'value': 'date = common.get_last_day_prev_month().strftime(\'%m.%Y\')',
                    },
                    {'key': 'text', 'value': 'text = \'test letter\''},
                ],
            },
            does_not_raise(),
        ),
        (
            {
                'id': 'report0',
                'report_type': 'simple_email_report',
                'queries': [],
                'subject_template': '{{date}}',
                'body_template': '{{text}} {{date}}',
                'template_params': [
                    {
                        'key': 'date',
                        'value': 'date = common.get_last_day_prev_month().strftime(\'%m.%Y\')',
                    },
                    {'key': 'text', 'value': 'text = \'{{test_letter}}\''},
                ],
            },
            does_not_raise(),
        ),
        (
            {
                'id': 'report0',
                'report_type': 'simple_email_report',
                'queries': [],
                'subject_template': '{{date}}',
                'body_template': '{{text}} {{date}}',
                'template_params': [
                    {
                        'key': 'date',
                        'value': 'date = common.get_last_day_prev_month().strftime(\'%m.%Y\')',
                    },
                    {'key': 'text', 'value': 'text = \'{{test_letter}\''},
                ],
            },
            pytest.raises(jinja2.exceptions.TemplateError),
        ),
    ],
)
def test_validate_simple_email_report_cfg(report_cfg, expectation):
    report_cfg.update(SER_CONST_PART)

    with expectation:
        validate_simple_email_report_cfg(report_cfg)
