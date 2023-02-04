from contextlib import contextmanager
import json
from unittest import mock

import pytest

from billing.monthclosing.operations.monograph_builder.lib.main import main


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


def patched_main(graph_cfg, reports_cfg, queries_cfg, execute, monograph_cfg):
    args = [__name__, '-o', str(monograph_cfg)]
    if graph_cfg:
        args.extend(['-g', str(graph_cfg)])
    if reports_cfg:
        args.extend(['-r', str(reports_cfg)])
    if queries_cfg:
        args.extend(['-q', str(queries_cfg)])
    if execute:
        args.append('-e')
    with mock.patch('sys.argv', args):
        main()

    return


@pytest.mark.parametrize(
    'graph, reports, queries, execute, expectation, error, expected_side_result',
    [
        ([], None, None, False, does_not_raise(), None, []),
        ([], [], None, False, does_not_raise(), None, []),
        ([], None, [], False, does_not_raise(), None, []),
        ([], [], [], False, does_not_raise(), None, []),
        (None, None, None, False, does_not_raise(), None, []),
        (None, [], None, False, does_not_raise(), None, []),
        (None, None, [], False, does_not_raise(), None, []),
        (None, [], [], False, does_not_raise(), None, []),
        (
            [
                {
                    'id': -1,
                    'name_id': 'node0',
                    'title': '',
                    'dependencies': [],
                    'offset': 0,
                    'nirvana_operation': 'run',
                }
            ],
            None,
            None,
            False,
            does_not_raise(),
            None,
            [
                {
                    'id': -1,
                    'name_id': 'node0',
                    'title': '',
                    'dependencies': [],
                    'offset': 0,
                    'nirvana_operation': 'run',
                }
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
                }
            ],
            [],
            None,
            False,
            does_not_raise(),
            None,
            [
                {
                    'id': -1,
                    'name_id': 'node0',
                    'title': '',
                    'dependencies': [],
                    'offset': 0,
                    'nirvana_operation': 'run',
                }
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
                }
            ],
            None,
            [],
            False,
            does_not_raise(),
            None,
            [
                {
                    'id': -1,
                    'name_id': 'node0',
                    'title': '',
                    'dependencies': [],
                    'offset': 0,
                    'nirvana_operation': 'run',
                }
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
                }
            ],
            [],
            [],
            False,
            does_not_raise(),
            None,
            [
                {
                    'id': -1,
                    'name_id': 'node0',
                    'title': '',
                    'dependencies': [],
                    'offset': 0,
                    'nirvana_operation': 'run',
                }
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
                }
            ],
            [
                {
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
                            'query': 'select sysdate from dual',
                            'query_id': '',
                            'query_params': [],
                        },
                    ],
                    'subject_template': '{{param}} {{date}}',
                    'body_template': '{{date}} {{text}}',
                    'template_params': [
                        {'key': 'param', 'value': ''},
                        {
                            'key': 'date',
                            'value': 'date = common.datetime.datetime(2020, 5, 18).strftime(\'%m.%Y\')',
                        },
                        {'key': 'text', 'value': 'text = \'test letter\''},
                    ],
                }
            ],
            [{'id': '0', 'query': 'select {{contract_id}} from dual'}],
            False,
            does_not_raise(),
            None,
            [
                {
                    'id': -1,
                    'name_id': 'node0',
                    'title': '',
                    'dependencies': [],
                    'offset': 0,
                    'nirvana_operation': 'run',
                }
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
                }
            ],
            [
                {
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
                            'query': 'select sysdate from dual',
                            'query_id': '',
                            'query_params': [],
                        },
                    ],
                    'subject_template': '{{param}} {{date}}',
                    'body_template': '{{date}} {{text}}',
                    'template_params': [
                        {'key': 'param', 'value': ''},
                        {
                            'key': 'date',
                            'value': 'date = common.datetime.datetime(2020, 5, 18).strftime(\'%m.%Y\')',
                        },
                        {'key': 'text', 'value': 'text = \'test letter\''},
                    ],
                }
            ],
            [{'id': '0', 'query': 'select {{contract_id}} from dual'}],
            True,
            does_not_raise(),
            None,
            [
                {
                    'id': -1,
                    'name_id': 'node0',
                    'title': '',
                    'dependencies': [],
                    'offset': 0,
                    'nirvana_operation': 'run',
                }
            ],
        ),
        (
            [],
            [
                {
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
                            'query': 'select sysdate from dual',
                            'query_id': '',
                            'query_params': [],
                        },
                    ],
                    'subject_template': '{{param}} {{date}}',
                    'body_template': '{{date}} {{text}}',
                    'template_params': [
                        {'key': 'param', 'value': ''},
                        {
                            'key': 'date',
                            'value': 'date = common.datetime.datetime(2020, 5, 18).strftime(\'%m.%Y\')',
                        },
                        {'key': 'text', 'value': 'text = \'test letter\''},
                    ],
                }
            ],
            [],
            False,
            pytest.raises(Exception),
            'No query',
            None,
        ),
        (
            [],
            [
                {
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
                            'query': 'select sysdate from dual',
                            'query_id': '',
                            'query_params': [],
                        },
                    ],
                    'subject_template': '{{param}} {{date}}',
                    'body_template': '{{date}} {{text}}',
                    'template_params': [
                        {'key': 'param', 'value': ''},
                        {
                            'key': 'date',
                            'value': 'date = common.datetime.datetime(2020, 5, 18).strftime(\'%m.%Y\')',
                        },
                        {'key': 'text', 'value': 'text = \'test letter\''},
                    ],
                }
            ],
            [{'id': '0', 'query': 'select {{contract_id}} from dual'}],
            True,
            does_not_raise(),
            None,
            [],
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
            [
                {
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
                    'subject_template': '{{param}} {{date}}',
                    'body_template': '{{date}} {{text}}',
                    'template_params': [
                        {'key': 'param', 'value': ''},
                        {
                            'key': 'date',
                            'value': 'date = common.datetime.datetime(2020, 5, 18).strftime(\'%m.%Y\')',
                        },
                        {'key': 'text', 'value': 'text = \'test letter\''},
                    ],
                },
            ],
            [
                {'id': '0', 'query': 'select {{contract_id}} from dual'},
                {'id': '1', 'query': 'select {{ dt }}, {{ contract_id }} from dual'},
            ],
            False,
            does_not_raise(),
            None,
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
                        'subject_template': '{{param}} {{date}}',
                        'body_template': '{{date}} {{text}}',
                        'template_params': [
                            {'key': 'param', 'value': ''},
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
            [
                {
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
                    'subject_template': '{{param}} {{date}}',
                    'body_template': '{{date}} {{text}}',
                    'template_params': [
                        {'key': 'param', 'value': ''},
                        {
                            'key': 'date',
                            'value': 'date = common.datetime.datetime(2020, 5, 18).strftime(\'%m.%Y\')',
                        },
                        {'key': 'text', 'value': 'text = \'test letter\''},
                    ],
                },
            ],
            [
                {'id': '0', 'query': 'select {{contract_id}} from dual'},
                {'id': '1', 'query': 'select {{ dt }}, {{ contract_id }} from dual'},
            ],
            True,
            does_not_raise(),
            None,
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
                        'subject_template': ' 05.2020',
                        'body_template': '05.2020 test letter',
                        'template_params': [],
                    },
                },
            ],
        ),
        (
            None,
            [
                {
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
                    'subject_template': '{{param}} {{date}}',
                    'body_template': '{{date}} {{text}}',
                    'template_params': [
                        {'key': 'param', 'value': ''},
                        {
                            'key': 'date',
                            'value': 'date = common.datetime.datetime(2020, 5, 18).strftime(\'%m.%Y\')',
                        },
                        {'key': 'text', 'value': 'text = \'test letter\''},
                    ],
                },
            ],
            [
                {'id': '0', 'query': 'select {{contract_id}} from dual'},
                {'id': '1', 'query': 'select {{ dt }}, {{ contract_id }} from dual'},
            ],
            True,
            does_not_raise(),
            None,
            [
                {
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
                    'subject_template': ' 05.2020',
                    'body_template': '05.2020 test letter',
                    'template_params': [],
                },
            ],
        ),
    ],
)
def test_main(
    tmp_path,
    graph,
    reports,
    queries,
    execute,
    expectation,
    error,
    expected_side_result,
):
    graph_cfg = tmp_path / 'graph_cfg'
    reports_cfg = tmp_path / 'reports_cfg'
    queries_cfg = tmp_path / 'queries_cfg'
    monograph_cfg = tmp_path / 'monograph_cfg'
    if graph is not None:
        with open(graph_cfg, 'w') as f:
            json.dump(graph, f)
    else:
        graph_cfg = None
    if reports is not None:
        for report_cfg in reports:
            report_cfg.update(SER_CONST_PART)
        with open(reports_cfg, 'w') as f:
            json.dump(reports, f)
    else:
        reports_cfg = None
    if queries is not None:
        with open(queries_cfg, 'w') as f:
            json.dump(queries, f)
    else:
        queries_cfg = None

    with expectation as exc:
        patched_main(graph_cfg, reports_cfg, queries_cfg, execute, monograph_cfg)

    if error:
        assert error in str(exc)
    else:
        if graph is not None:
            for node in expected_side_result:
                if node['nirvana_operation'] == 'simple_email_report':
                    node['params'].update(SER_CONST_PART)
        else:
            for report_cfg in expected_side_result:
                report_cfg.update(SER_CONST_PART)
        with open(monograph_cfg, 'r') as f:
            side_result = json.load(f)
            assert side_result == expected_side_result
            side_cfg = tmp_path / 'side_cfg'
            with open(side_cfg, 'w') as f:
                json.dump(side_result, f)
            if graph is not None:
                patched_main(side_cfg, None, None, execute, monograph_cfg)
            else:
                patched_main(None, side_cfg, None, execute, monograph_cfg)

    return
