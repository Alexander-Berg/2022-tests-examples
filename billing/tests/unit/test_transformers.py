from contextlib import contextmanager

import pytest

from billing.monthclosing.operations.monograph_builder.lib.transformers import (
    transform_query_task,
    transform_graph_cfg,
    transform_simple_email_report_cfg,
)
from billing.monthclosing.operations.monograph_builder.lib.validators import (
    validate_query_task,
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
    'query_task, queries_cfg, expected_result',
    [
        (
            {'query_id': '', 'query': 'select 1 from dual', 'query_params': []},
            {'0': 'select'},
            {'query': 'select 1 from dual', 'query_id': '', 'query_params': []},
        ),
        (
            {
                'query_id': '',
                'query': 'select {{contract_id}} from dual',
                'query_params': [{'key': 'contract_id', 'value': ''}],
            },
            {'0': '{{contract_id}}'},
            {'query': 'select  from dual', 'query_id': '', 'query_params': []},
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
            {
                'query': 'select 0 contract_id, date\'1970-01-01\' dt from dual',
                'query_id': '',
                'query_params': [],
            },
        ),
    ],
)
def test_transform_query_task(query_task, queries_cfg, expected_result):
    result = transform_query_task(query_task, queries_cfg)

    assert result == expected_result

    validate_query_task(result, queries_cfg)
    validate_query_task(result, [])

    return


@pytest.mark.parametrize(
    'report_cfg, queries_cfg, execute, expected_result',
    [
        (
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
            {'0': 'select {{contract_id}} from dual'},
            False,
            {
                'id': 'report1',
                'report_type': 'simple_email_report',
                'queries': [
                    {
                        'filename': '',
                        'filetype': '',
                        'dbname': '',
                        'query': 'select  from dual',
                        'query_id': '',
                        'query_params': [],
                    }
                ],
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
        ),
        (
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
                'subject_template': '{{date}}',
                'body_template': '{{text}} {{date}}',
                'template_params': [
                    {
                        'key': 'date',
                        'value': 'date = common.datetime.datetime(2020, 5, 18).strftime(\'%m.%Y\')',
                    },
                    {'key': 'text', 'value': 'text = \'test letter\''},
                ],
            },
            {'0': 'select {{contract_id}} from dual'},
            True,
            {
                'id': 'report1',
                'report_type': 'simple_email_report',
                'queries': [
                    {
                        'filename': '',
                        'filetype': '',
                        'dbname': '',
                        'query': 'select  from dual',
                        'query_id': '',
                        'query_params': [],
                    }
                ],
                'subject_template': '05.2020',
                'body_template': 'test letter 05.2020',
                'template_params': [],
            },
        ),
        (
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
                'template_params': [],
            },
            {
                '0': 'select {{contract_id}} from dual',
                '1': 'select {{ dt }}, {{ contract_id }} from dual',
            },
            True,
            {
                'id': 'report1',
                'report_type': 'simple_email_report',
                'queries': [
                    {
                        'filename': '',
                        'filetype': '',
                        'dbname': '',
                        'query': 'select  from dual',
                        'query_id': '',
                        'query_params': [],
                    },
                    {
                        'filename': '',
                        'filetype': '',
                        'dbname': '',
                        'query': 'select sysdate, 0 from dual',
                        'query_id': '',
                        'query_params': [],
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
                'template_params': [],
            },
        ),
    ],
)
def test_transform_report_cfg(report_cfg, queries_cfg, execute, expected_result):
    report_cfg.update(SER_CONST_PART)
    expected_result.update(SER_CONST_PART)

    result = transform_simple_email_report_cfg(report_cfg, queries_cfg, execute)

    assert result == expected_result

    validate_simple_email_report_cfg(result)

    return


@pytest.mark.parametrize(
    'graph_cfg, reports_cfg, queries_cfg, execute, expected_result',
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
            False,
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
                    'subject_template': '{{date}}',
                    'body_template': '{{text}} {{date}}',
                    'template_params': [
                        {
                            'key': 'date',
                            'value': 'date = common.datetime.datetime(2020, 5, 18).strftime(\'%m.%Y\')',
                        },
                        {'key': 'text', 'value': 'text = \'test letter\''},
                    ],
                },
            },
            {
                '0': 'select {{contract_id}} from dual',
                '1': 'select {{ dt }}, {{ contract_id }} from dual',
            },
            False,
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
                    'params': {
                        'id': 'node1',
                        'report_type': 'simple_email_report',
                        'queries': [
                            {
                                'filename': '',
                                'filetype': '',
                                'dbname': '',
                                'query': 'select  from dual',
                                'query_id': '',
                                'query_params': [],
                            },
                            {
                                'filename': '',
                                'filetype': '',
                                'dbname': '',
                                'query': 'select sysdate, 0 from dual',
                                'query_id': '',
                                'query_params': [],
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
                        'subject_template': '{{date}}',
                        'body_template': '{{text}} {{date}}',
                        'template_params': [
                            {
                                'key': 'date',
                                'value': 'date = common.datetime.datetime(2020, 5, 18).strftime(\'%m.%Y\')',
                            },
                            {'key': 'text', 'value': 'text = \'test letter\''},
                        ],
                    },
                },
            ],
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
                    'subject_template': '{{date}}',
                    'body_template': '{{text}} {{date}}',
                    'template_params': [
                        {
                            'key': 'date',
                            'value': 'date = common.datetime.datetime(2020, 5, 18).strftime(\'%m.%Y\')',
                        },
                        {'key': 'text', 'value': 'text = \'test letter\''},
                    ],
                },
            },
            {
                '0': 'select {{contract_id}} from dual',
                '1': 'select {{ dt }}, {{ contract_id }} from dual',
            },
            True,
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
                    'params': {
                        'id': 'node1',
                        'report_type': 'simple_email_report',
                        'queries': [
                            {
                                'filename': '',
                                'filetype': '',
                                'dbname': '',
                                'query': 'select  from dual',
                                'query_id': '',
                                'query_params': [],
                            },
                            {
                                'filename': '',
                                'filetype': '',
                                'dbname': '',
                                'query': 'select sysdate, 0 from dual',
                                'query_id': '',
                                'query_params': [],
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
                        'subject_template': '05.2020',
                        'body_template': 'test letter 05.2020',
                        'template_params': [],
                    },
                },
            ],
        ),
    ],
)
def test_transform_graph_cfg(
    graph_cfg, reports_cfg, queries_cfg, execute, expected_result
):
    for name_id, report_cfg in reports_cfg.items():
        report_cfg.update(SER_CONST_PART)
    for node in expected_result:
        if node['nirvana_operation'] == 'simple_email_report':
            node['params'].update(SER_CONST_PART)

    result = transform_graph_cfg(graph_cfg, reports_cfg, queries_cfg, execute)

    assert result == expected_result

    validate_graph_cfg(result, reports_cfg, queries_cfg)
    validate_graph_cfg(result, [], [])

    return
