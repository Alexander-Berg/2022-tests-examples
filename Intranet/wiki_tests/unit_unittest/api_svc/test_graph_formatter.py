import tempfile
from os import path, remove
import ujson as json
import pytest
from django.test import override_settings
from intranet.wiki.tests.wiki_tests.common.data_helper import read_test_asset
from intranet.wiki.tests.wiki_tests.common.unittest_base import BaseApiTestCase

BLOCKDIAG_COLORS_CASE = """{

Service [label="Services"]
Issue [label="Issue"]
IssueGroup [label="IssueGroup"]
IssueWeight [label="IssueWeight"]
ExecutionType [label="ExecutionType"]
ExecutionStep [label="ExecutionStep"]
ExecutionThreshold [label="ExecutionThreshold"]
ExecutionChain [label="ExecutionChain"]

ServiceIssue [label="ServiceIssue"]
ServiceExecutionAction [label="ServiceAction"]

group {
    orientation = portrait;
    color = "#FF0000";
    Service; ServiceIssue; ServiceExecutionAction
  }

group {
    orientation = portrait;
    color = "#FF0000";
    ExecutionThreshold; ExecutionChain; IssueGroup;
  }

Service <- ServiceIssue
ServiceIssue -> Issue
ExecutionThreshold <- ServiceIssue
ServiceIssue <- ServiceExecutionAction
ServiceExecutionAction -> ExecutionStep
ExecutionStep -> ExecutionType
IssueWeight -> Issue -> IssueGroup
Issue -> ExecutionChain
ExecutionChain <- ExecutionStep
ExecutionChain <- ExecutionThreshold -> IssueGroup

}"""

BLOCKDIAG_CASE = """{
            box [shape = "box"];
            roundedbox [shape = "roundedbox"];
            box -> roundedbox;
}"""

SEQDIAG_CASE = '''{
A -> B [label = "normal edge"];
B --> C [label = "dotted edge"];
}'''

GRAPHVIZ = '''digraph A {a -> b}'''

CASES = [
    {
        'case_name': 'Seqdiag: broken markup',
        'request': {'formatter': 'seqdiag', 'output': 'svg', 'payload': '{'},
        'successful': False,
        'error_contains': "Can't process input",
    },
    {
        'case_name': 'Blockdiag: broken markup',
        'request': {'formatter': 'blockdiag', 'output': 'svg', 'payload': '{'},
        'successful': False,
        'error_contains': "Can't process input",
    },
    {
        'case_name': 'Graphviz: broken markup',
        'request': {'formatter': 'graphviz', 'output': 'svg', 'payload': '''digraph A {a -> b'''},
        'successful': False,
        'error_contains': 'syntax error in line 1',
    },
    {
        'case_name': 'Graphviz: svg',
        'request': {'formatter': 'graphviz', 'output': 'svg', 'payload': GRAPHVIZ},
        'successful': True,
        'output': 'diags/graphviz_case1.svg',
    },
    {
        'case_name': 'Seqdiag: svg',
        'request': {'formatter': 'seqdiag', 'output': 'svg', 'payload': SEQDIAG_CASE},
        'successful': True,
        'output': 'diags/seqdiag_case1.svg',
    },
    {
        'case_name': 'Blockdiag: svg',
        'request': {'formatter': 'blockdiag', 'output': 'svg', 'payload': BLOCKDIAG_CASE},
        'successful': True,
        'output': 'diags/blockdiag_case1.svg',
    },
    {
        'case_name': 'Blockdiag: colors',
        'request': {'formatter': 'blockdiag', 'output': 'svg', 'payload': BLOCKDIAG_COLORS_CASE},
        'successful': True,
        'output': 'diags/blockdiag_case2.svg',
    },
]


class GraphFormatterTest(BaseApiTestCase):
    def setUp(self):
        super(GraphFormatterTest, self).setUp()
        self.setUsers()
        self.client.login('thasonic')

    def test_webcolors_import(self):
        # webcolors по какой-то мистической причине постоянно отклеивается
        # а так как он вызывается динамически где-то в глубине blockdiag мы
        # об этом узнаем поздно
        import webcolors

        assert webcolors.name_to_hex('red') == '#ff0000'

    def test_if_tmp_accessible(self):
        # вызов graphviz blockdiag и seqdiag работают через временные
        # файлы. если тестраннер не дает создавать их, нам нечего тестировать
        _, tmp_file_name = tempfile.mkstemp(prefix='wf_diagram_src', suffix='.diag')
        W = 'tmp is okay'
        with open(tmp_file_name, 'w') as file_obj:
            file_obj.write(W)

        with open(tmp_file_name, 'r') as file_obj:
            res = file_obj.read()
            assert res == W

        if path.isfile(tmp_file_name):
            remove(tmp_file_name)

    @pytest.mark.skip('blockdiag requires fonts to be installed on machine')
    @override_settings(AUTH_TEST_MECHANISM='tvm')
    def test_graph_formatters(self):
        """
        Иногда надо переснять примеры диаграмм (скажем, либу обновили)
        :return:
        """
        RECORD_CASES = False

        for case in CASES:
            response = self.client.post('/_api/svc/.graph_formatter', case['request'])
            if case['successful']:
                self.assertEqual(
                    response.status_code, 200, 'Case %s; Content: %s' % (case['case_name'], response.content)
                )

                image_source = json.loads(response.content)['data']['image_source']
                image_source_must_equal = read_test_asset(case['output'])
                if image_source_must_equal is None:
                    raise ValueError('%s case asset %s not found ' % (case['case_name'], case['output']))
                if RECORD_CASES:
                    print(case['output'])
                    print('>' * 80)
                    print(image_source.strip())
                    print('<' * 80)
                    print()
                else:
                    self.assertEqual(
                        image_source.strip(),
                        image_source_must_equal.strip().decode(),
                        '%s case output mismatched' % case['case_name'],
                    )

            else:
                self.assertEqual(response.status_code, 409, 'Case %s' % case['case_name'])
                response_dict = json.loads(response.content)
                self.assertIn(
                    case['error_contains'], response_dict['error']['message'][0], 'Case %s' % case['case_name']
                )
