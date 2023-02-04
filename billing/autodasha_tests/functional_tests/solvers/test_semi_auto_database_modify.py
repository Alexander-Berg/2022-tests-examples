# -*- coding: utf-8 -*-

from __future__ import unicode_literals

import pytest
import mock
import functools
import datetime as dt

from autodasha.solver_cl import (
    ReportSolver,
    ParseException,
    SemiAutoDatabaseModifySolver,
)
from autodasha.core.api.tracker import IssueReport
from autodasha.core.api.tracker import IssueTransitions

from tests.autodasha_tests.functional_tests import case_utils
from tests.autodasha_tests.common import db_utils
from tests.autodasha_tests.common import staff_utils


def _fake_init(self, *args, **kwargs):
    super(ReportSolver, self).__init__(*args, **kwargs)
    self.ro_session = self.session


def _execute_queries(self, database_id, queries):
    if 'raise' in queries:
        exc = 1 / 0
    execution_session = self.session
    with execution_session.begin():
        query = '\n'.join(['begin', queries, 'end;'])
        cursor = execution_session.connection().connection.cursor()
        cursor.execute(query)
    return query


def get_approve_message(*args, **kwargs):
    return (
        'Нажми кнопку "Подтвердить" или познай мощь Ктулху! '
        'https://jing.yandex-team.ru/files/autodasha/approve_button_something.png'
    )


BILLING_SHOP_ID = 35
MONTH = dt.datetime.now()
TEMP_CLOSED = 0
LATEST_CLOSED = MONTH.replace(day=1) - dt.timedelta(days=1)

_try_again_cmt = (
    '\n\nТы можешь поправить запрос прямо в описании задачи. '
    'Как будет готово, призови меня с комментарием "Исправлено". '
    'Либо просто закрой задачу, если она неактуальна.'
)

COMMENTS = {
    'database_id_is_required': 'Не указана база данных для выполнения запроса.',
    'queries_is_required': 'Не указан запрос.',
    'approver_is_required': 'Не выбран подтверждающий.',
    'wrong_approver': 'Автор и подтверждающий совпадают.\nПожалуйста, укажи другого подтверждаюищего.',
    'execution_failed': '!!**Произошла ошибка**!!\n\n%%\n{_error}\n%%' + _try_again_cmt,
    'has_been_terminated': 'Задача не была выполнена, время выполнениям запроса превысило максимальное'
                           ' - {time_out} секунд.' + _try_again_cmt,
    'done': '!!(зел)**Выполнено успешно**!!\n\n'
            'Обработано запросов: {len_queries}\n\n{execution_info}',
    'already_solved': 'Эта задача уже была выполнена.',
    'get_approve': 'Нажми кнопку "Подтвердить" или познай мощь Ктулху! '
                   'https://jing.yandex-team.ru/files/autodasha/approve_button_something.png',
}


class AbstractMockTestCase(case_utils.AbstractMockTestCase):

    author = 'autodasha'

    _default_lines = {
        'summary': 'Non! Rien de rien - modify',
        'automatic_execution': '__{automatic_execution}__\n\n'.format(automatic_execution='%s'),
        'reason': '**Обоснование:**\n<[Non! Je ne regrette rien]>\n\n',
        'database_id': '**БД:**\n{symbol}{database_id}{symbol} \n\n'.format(
            database_id='%s', symbol='%%' + '%%'
        ),
        'queries': '**Скрипт:**\n{symbol}{queries}{symbol}\n\n'.format(
            queries='%s', symbol='%%' + '%%'
        ),
        'execution_with_author': '**Выполнять в присутствии автора тикета:** {execution_with_author}\n\n'.format(
            execution_with_author='%s'
        ),
        'summon': '**Подтверждающий менеджер:**\n{summon}\n\n'.format(summon='%s'),
        'finally': '--\nСоздано через формы',
    }


class AbstractGoodMockTestCase(AbstractMockTestCase):
    _cases = []
    __metaclass__ = case_utils.TestCaseMeta


class HasQueryAndExecutionWithAuthorCase(AbstractGoodMockTestCase):
    _representation = 'automatic_execution_and_execution_with_author'

    def get_data(self, mock_manager):
        lines = [
            self._get_default_line(
                automatic_execution='У меня готовый sql-запрос для БД Баланса или Меты'
            ),
            self._get_default_line('reason'),
            self._get_default_line(database_id=' Balance Oracle '),
            self._get_default_line(
                queries='\n dbms_output.put_line(\'__\');\nupdate dual set 1=1;\n'
            ),
            self._get_default_line(execution_with_author='Да'),
            self._get_default_line(summon='Александр Курицын (nevskiy)'),
            self._get_default_line('finally'),
        ]
        return self._get_default_line('summary'), lines

    def get_result(self):
        return {
            'database_id': 'balance',
            'automatic_execution': True,
            'queries': 'dbms_output.put_line(\'__\');\nupdate dual set 1=1;',
            'execution_with_author': True,
            'summonees': ['nevskiy', self.author],
        }


class HasQueryAndExecutionWOAuthorCase(AbstractGoodMockTestCase):
    _representation = 'automatic_execution_and_execution_wo_author'

    def get_data(self, mock_manager):
        lines = [
            self._get_default_line(
                automatic_execution='У меня готовый sql-запрос для БД Баланса или Меты'
            ),
            self._get_default_line('reason'),
            self._get_default_line(database_id=' Balance Oracle '),
            self._get_default_line(queries='\n dbms_output.put_line(\'__\');\n'),
            self._get_default_line(execution_with_author='No'),
            self._get_default_line(summon='Александр Курицын (nevskiy)'),
            self._get_default_line('finally'),
        ]
        return self._get_default_line('summary'), lines

    def get_result(self):
        return {
            'database_id': 'balance',
            'automatic_execution': True,
            'queries': 'dbms_output.put_line(\'__\');',
            'execution_with_author': False,
            'summonees': ['nevskiy'],
        }


class HasQueryAndPrimaryManagersAndWithAuthorCase(AbstractGoodMockTestCase):
    _representation = 'automatic_execution_and_primary_managers_and_with_author'

    author = 'arkasha_primary'

    def get_data(self, mock_manager):
        lines = [
            self._get_default_line(
                automatic_execution='У меня готовый sql-запрос для БД Баланса или Меты'
            ),
            self._get_default_line('reason'),
            self._get_default_line(database_id=' Balance Oracle '),
            self._get_default_line(queries='\n dbms_output.put_line(\'__\');\n'),
            self._get_default_line(execution_with_author=True),
            self._get_default_line(summon='Александр Курицын (nevskiy)'),
            self._get_default_line('finally'),
        ]
        return self._get_default_line('summary'), lines

    def get_result(self):
        return {
            'database_id': 'balance',
            'automatic_execution': True,
            'queries': 'dbms_output.put_line(\'__\');',
            'execution_with_author': True,
            'summonees': [self.author],
        }


class HasQueryAndPrimaryManagersAndWOAuthorCase(AbstractGoodMockTestCase):
    _representation = 'automatic_execution_and_primary_managers_and_wo_author'

    author = 'arkasha_primary'

    def get_data(self, mock_manager):
        lines = [
            self._get_default_line(
                automatic_execution='У меня готовый sql-запрос для БД Баланса или Меты'
            ),
            self._get_default_line('reason'),
            self._get_default_line(database_id=' Meta Oracle  '),
            self._get_default_line(queries='\n dbms_output.put_line(\'__\'); dbms_output.put_line(\'test\');\n'),
            self._get_default_line(execution_with_author='Нет'),
            self._get_default_line(summon='Александр Курицын (arkasha_primary)'),
            self._get_default_line('finally'),
        ]
        return self._get_default_line('summary'), lines

    def get_result(self):
        return {
            'database_id': 'meta',
            'automatic_execution': True,
            'queries': 'dbms_output.put_line(\'__\'); dbms_output.put_line(\'test\');',
            'execution_with_author': False,
            'summonees': None,
        }


class WOQueryAndExecutionWithAuthorCase(AbstractGoodMockTestCase):
    _representation = 'wo_queries_and_execution_with_author'

    def get_data(self, mock_manager):
        lines = [
            self._get_default_line(automatic_execution='bla bla bla'),
            self._get_default_line('reason'),
            self._get_default_line(database_id='bla bla bla'),
            self._get_default_line(queries=''),
            self._get_default_line(execution_with_author='Да'),
            self._get_default_line(summon='Александр Курицын (nevskiy)'),
            self._get_default_line('finally'),
        ]
        return self._get_default_line('summary'), lines

    def get_result(self):
        return {
            'database_id': None,
            'automatic_execution': False,
            'queries': '',
            'execution_with_author': True,
            'summonees': ['nevskiy', self.author],
        }


class WOQueryAndExecutionWOAuthorCase(AbstractGoodMockTestCase):
    _representation = 'wo_queries_and_execution_wo_author'

    def get_data(self, mock_manager):
        lines = [
            self._get_default_line(automatic_execution='bla bla bla'),
            self._get_default_line('reason'),
            self._get_default_line(database_id='bla bla bla'),
            self._get_default_line(queries=''),
            self._get_default_line(execution_with_author='False'),
            self._get_default_line(summon='Александр Курицын (nevskiy)'),
            self._get_default_line('finally'),
        ]
        return self._get_default_line('summary'), lines

    def get_result(self):
        return {
            'database_id': None,
            'automatic_execution': False,
            'queries': '',
            'execution_with_author': False,
            'summonees': ['nevskiy'],
        }


class WOQueryAndPrimaryManagersAndWithAuthorCase(AbstractGoodMockTestCase):
    _representation = 'wo_queries_and_primary_managers_and_with_author'

    author = 'arkasha_primary'

    def get_data(self, mock_manager):
        lines = [
            self._get_default_line(automatic_execution='bla bla bla'),
            self._get_default_line('reason'),
            self._get_default_line(database_id='bla bla bla'),
            self._get_default_line(queries=''),
            self._get_default_line(execution_with_author='Yes'),
            self._get_default_line(summon='Александр Курицын (nevskiy)'),
            self._get_default_line('finally'),
        ]
        return self._get_default_line('summary'), lines

    def get_result(self):
        return {
            'database_id': None,
            'automatic_execution': False,
            'queries': '',
            'execution_with_author': True,
            'summonees': [self.author],
        }


class WOQueryAndPrimaryManagersAndWOAuthorCase(AbstractGoodMockTestCase):
    _representation = 'wo_queries_and_primary_managers_and_wo_author'

    author = 'arkasha_primary'

    def get_data(self, mock_manager):
        lines = [
            self._get_default_line(automatic_execution='bla bla bla'),
            self._get_default_line('reason'),
            self._get_default_line(database_id='bla bla bla'),
            self._get_default_line(queries=''),
            self._get_default_line(execution_with_author='Нет'),
            self._get_default_line(summon='Александр Курицын (nevskiy)'),
            self._get_default_line('finally'),
        ]
        return self._get_default_line('summary'), lines

    def get_result(self):
        return {
            'database_id': None,
            'automatic_execution': False,
            'queries': '',
            'execution_with_author': False,
            'summonees': None,
        }


@mock.patch('autodasha.solver_cl.base_solver.ReportSolver.__init__', _fake_init)
@pytest.mark.parametrize(
    'mock_issue_data',
    [case() for case in AbstractGoodMockTestCase._cases],
    ids=lambda case: str(case),
    indirect=['mock_issue_data'],
)
def test_parse_good(mock_queue_object, mock_issue_data):
    issue, case = mock_issue_data

    req_res = case.get_result()

    solver = SemiAutoDatabaseModifySolver(mock_queue_object, issue)
    issue.author = case.author
    res = solver.parse_issue()
    for k, v in req_res.items():
        assert v == res.get(k)


class AbstractFailingMockTestCase(AbstractMockTestCase):
    _cases = []
    __metaclass__ = case_utils.TestCaseMeta


class DatabaseIdFieldIsEmptyCase(AbstractFailingMockTestCase):
    _representation = 'database_field_is_empty'

    def get_data(self, mock_manager):
        lines = [
            self._get_default_line(
                automatic_execution='У меня готовый sql-запрос для БД Баланса или Меты'
            ),
            self._get_default_line('reason'),
            self._get_default_line(database_id=''),
            self._get_default_line(queries='1'),
            self._get_default_line(execution_with_author='Нет'),
            self._get_default_line(summon='Александр Курицын (nevskiy)'),
            self._get_default_line('finally'),
        ]
        return self._get_default_line('summary'), lines

    def get_result(self):
        comments = COMMENTS.get('database_id_is_required')
        return comments


class QueryFieldIsEmptyCase(AbstractFailingMockTestCase):
    _representation = 'queries_field_is_empty'

    def get_data(self, mock_manager):
        lines = [
            self._get_default_line(
                automatic_execution='У меня готовый sql-запрос для БД Баланса или Меты'
            ),
            self._get_default_line('reason'),
            self._get_default_line(database_id=' Meta Oracle  '),
            self._get_default_line(queries=''),
            self._get_default_line(execution_with_author='Нет'),
            self._get_default_line(summon='Александр Курицын (nevskiy)'),
            self._get_default_line('finally'),
        ]
        return self._get_default_line('summary'), lines

    def get_result(self):
        comments = COMMENTS.get('queries_is_required')
        return comments


class ApproverFieldIsEmptyCase(AbstractFailingMockTestCase):
    _representation = 'approver_field_is_empty'

    def get_data(self, mock_manager):
        lines = [
            self._get_default_line(
                automatic_execution='У меня готовый sql-запрос для БД Баланса или Меты'
            ),
            self._get_default_line('reason'),
            self._get_default_line(database_id=' Meta Oracle  '),
            self._get_default_line(queries='123'),
            self._get_default_line(execution_with_author='Нет'),
            self._get_default_line(summon=''),
            self._get_default_line('finally'),
        ]
        return self._get_default_line('summary'), lines

    def get_result(self):
        comments = COMMENTS.get('approver_is_required')
        return comments


class AuthorApproverSameCase(AbstractFailingMockTestCase):
    _representation = 'author_and_approver_are_the_same_author_not_in_primary_managers'
    author = 'nevskiy'

    def get_data(self, mock_manager):
        lines = [
            self._get_default_line(
                automatic_execution='У меня готовый sql-запрос для БД Баланса или Меты'
            ),
            self._get_default_line('reason'),
            self._get_default_line(database_id=' Meta Oracle  '),
            self._get_default_line(queries='123'),
            self._get_default_line(execution_with_author='Нет'),
            self._get_default_line(summon='nevskiy'),
            self._get_default_line('finally'),
        ]
        return self._get_default_line('summary'), lines

    def get_result(self):
        comments = COMMENTS.get('wrong_approver')
        return comments


@mock.patch('autodasha.solver_cl.base_solver.ReportSolver.__init__', _fake_init)
@pytest.mark.parametrize(
    'mock_issue_data',
    [case() for case in AbstractFailingMockTestCase._cases],
    ids=lambda case: str(case),
    indirect=['mock_issue_data'],
)
def test_parse_failing(mock_queue_object, mock_issue_data):
    ri = IssueReport()
    issue, case = mock_issue_data

    req_comment = case.get_result()

    solver = SemiAutoDatabaseModifySolver(mock_queue_object, issue)
    issue.author = case.author

    with pytest.raises(ParseException) as exc:
        solver.parse_issue()

    if ri.comments:
        comments = [text.get('text') for text in ri.comments]
        comments = ' '.join(comments)
        if comments:
            comments += exc.value.message
        assert req_comment == comments
    else:
        assert req_comment in exc.value.message


class RequiredResult(case_utils.RequiredResult):
    def __init__(self, **kwargs):
        self.commit = kwargs.get('commit')
        self.delay = kwargs.get('delay')
        self.summonees = kwargs.get('summonees', [])
        self.assignee = 'autodasha'
        self.sleep_time = 1
        self.time_out = 1500
        self.modify = False
        self.several_modify = False
        self.execution_time_out = False
        self.execution_error = False
        self.second_query_error = False
        self.json = False
        super(RequiredResult, self).__init__(**kwargs)

    @staticmethod
    def get_comment(key, *args, **kwargs):
        return COMMENTS.get(key).format(*args, **kwargs)


class AbstractDBTestCase(case_utils.AbstractDBTestCase):
    author = 'arkasha'
    summary = 'Тру ля lya - modify'

    _description = '''
__{automatic_execution}__\n\n
**Обоснование:**
<[Non! Je ne regrette rien]>


**БД:**
%%{database_id}%%


**Скрипт:**
%%{queries}%%


**Выполнять в присутствии автора тикета:**{execution_with_author}


**Подтверждающий менеджер:**
{summon}


--
Создано через формы
    '''.strip()
    issue_key = 'test_semi_auto_database_modify'

    _cases = []
    __metaclass__ = case_utils.TestCaseMeta

    def __init__(self):
        super(AbstractDBTestCase, self).__init__()

    def _get_data(self, session):
        return self.prepare_data(session)

    def _get_success_comment(self, queries):
        return COMMENTS['done'].format(
            len_queries=1,
            execution_info='<{sql\n%s%s%s\n}>' % ('%%', '\n'.join(['begin', queries, 'end;']), '%%')
        )


class WOQueryRequestApproveWithAuthorDBCase(AbstractDBTestCase):
    _representation = 'wo_queries_request_approve_with_author'

    def prepare_data(self, session):
        return {
            'automatic_execution': 'неть',
            'database_id': 'не важно',
            'queries': '''
            ''',
            'execution_with_author': 'Да',
            'summon': 'Александр Курицын (nevskiy)',
        }

    def get_result(self):
        res = RequiredResult(commit=True, delay=False)
        res.transition = IssueTransitions.none
        res.summonees = ('nevskiy', self.author)
        res.add_message(COMMENTS['get_approve'])
        return res


class WOQueryRequestApproveWOAuthorDBCase(AbstractDBTestCase):
    _representation = 'wo_queries_request_approve_wo_author'

    def prepare_data(self, session):
        return {
            'automatic_execution': 'неть',
            'database_id': 'не важно',
            'queries': '''
            ''',
            'execution_with_author': 'No',
            'summon': 'Александр Курицын (nevskiy)',
        }

    def get_result(self):
        res = RequiredResult(commit=True, delay=False)
        res.transition = IssueTransitions.none
        res.summonees = 'nevskiy'
        res.add_message(COMMENTS['get_approve'])
        return res


class WOQueryWithAuthorHasApproveDBCase(AbstractDBTestCase):
    _representation = 'wo_queries_with_author_has_approve'

    def get_comments(self):
        return [
            {
                'author': 'autodasha',
                'text': COMMENTS['get_approve'],
                'summonees': ['arkasha', 'nevskiy'],
            },
            ('ashul', 'Подтверждено'),
            ('nevskiy', 'Подтверждено'),
            ('arkasha', 'Подтверждено'),
        ]

    def prepare_data(self, session):
        return {
            'automatic_execution': 'неть',
            'database_id': 'не важно',
            'queries': '''
            ''',
            'execution_with_author': 'Да',
            'summon': 'Александр Курицын (nevskiy)',
        }

    def get_result(self):
        res = RequiredResult(commit=True, delay=False)
        res.transition = IssueTransitions.none
        return res


class RequestApproveWithAuthorDBCase(AbstractDBTestCase):
    _representation = 'request_approve_automatic_execution_with_author'

    def prepare_data(self, session):
        return {
            'automatic_execution': 'У меня готовый sql-запрос для БД Баланса или Меты',
            'database_id': 'Balance Oracle',
            'queries': '''
update bo.t_export
set state=1
where classname = 'RawRegister'
and type='OEBS'
and object_id = 123
;
            ''',
            'execution_with_author': 'Да',
            'summon': 'Александр Курицын (nevskiy)',
        }

    def get_result(self):
        res = RequiredResult(commit=True, delay=True)
        res.transition = IssueTransitions.none
        res.summonees = ('nevskiy', self.author)
        res.add_message(COMMENTS['get_approve'])
        return res


class RequestApproveWOAuthorDBCase(AbstractDBTestCase):
    _representation = 'request_approve_automatic_execution_wo_author'

    def prepare_data(self, session):
        return {
            'automatic_execution': 'У меня готовый sql-запрос для БД Баланса или Меты',
            'database_id': 'Balance Oracle',
            'queries': '''
update bo.t_export
set state=1
where classname = 'RawRegister'
and type='OEBS'
and object_id = 123
;
            ''',
            'execution_with_author': 'No',
            'summon': 'Александр Курицын (nevskiy)',
        }

    def get_result(self):
        res = RequiredResult(commit=True, delay=True)
        res.transition = IssueTransitions.none
        res.summonees = ('nevskiy',)
        res.add_message(COMMENTS['get_approve'])
        return res


class RequestApprovePrimaryManagersWithAuthorDBCase(AbstractDBTestCase):
    _representation = 'request_approve_automatic_execution_primary_managers_with_author'

    author = 'arkasha_primary'

    def prepare_data(self, session):
        return {
            'automatic_execution': 'У меня готовый sql-запрос для БД Баланса или Меты',
            'database_id': 'Balance Oracle',
            'queries': '''
update bo.t_export
set state=1
where classname = 'RawRegister'
and type='OEBS'
and object_id = 123
;
            ''',
            'execution_with_author': 'Да',
            'summon': 'Александр Курицын (nevskiy)',
        }

    def get_result(self):
        res = RequiredResult(commit=True, delay=True)
        res.transition = IssueTransitions.none
        res.summonees = ('arkasha_primary',)
        res.add_message(COMMENTS['get_approve'])
        return res


class RequestApprovePrimaryManagersWOAuthorDBCase(AbstractDBTestCase):
    _representation = 'request_approve_automatic_execution_primary_managers_wo_author'

    author = 'arkasha_primary'

    def prepare_data(self, session):
        return {
            'automatic_execution': 'У меня готовый sql-запрос для БД Баланса или Меты',
            'database_id': 'Balance Oracle',
            'queries': '''
update bo.t_export
set state=1
where classname = 'RawRegister'
and type='OEBS'
and object_id = 123
;
            ''',
            'execution_with_author': 'False',
            'summon': 'Александр Курицын (nevskiy)',
        }

    def get_result(self):
        res = RequiredResult(commit=True, delay=True)
        res.transition = IssueTransitions.none
        res.summonees = None
        return res


class HasApproveWithAuthorWOAuthorApproveDBCase(AbstractDBTestCase):
    _representation = 'wo_author_approve_automatic_execution_with_author_has_other_confirmation'

    author = 'arkasha'

    def get_comments(self):
        return [
            {
                'author': 'autodasha',
                'text': COMMENTS['get_approve'],
                'summonees': ['arkasha', 'nevskiy'],
            },
            ('ashul', 'Подтверждено'),
            ('nevskiy', 'Подтверждено'),
        ]

    def prepare_data(self, session):
        return {
            'automatic_execution': 'У меня готовый sql-запрос для БД Баланса или Меты',
            'database_id': 'Balance Oracle',
            'queries': '''
update bo.t_export
set state=1
where classname = 'RawRegister'
and type='OEBS'
and object_id = 123
;
            ''',
            'execution_with_author': 'Да',
            'summon': 'Александр Курицын (nevskiy)',
        }

    def get_result(self):
        res = RequiredResult(commit=True, delay=True)
        res.transition = IssueTransitions.none
        res.summonees = None
        return res


class HasApprovePrimaryManagersWithAuthorWOSupportApproveDBCase(AbstractDBTestCase):
    _representation = (
        'wo_support_approve_automatic_execution_with_author_primary_managers_has_other_confirmation'
    )

    author = 'arkasha_primary'

    def get_comments(self):
        return [
            {
                'author': 'autodasha',
                'text': COMMENTS['get_approve'],
                'summonees': ['arkasha_primary'],
            },
            ('arkasha_primary', 'Подтверждено'),
        ]

    def prepare_data(self, session):
        return {
            'automatic_execution': 'У меня готовый sql-запрос для БД Баланса или Меты',
            'database_id': 'Balance Oracle',
            'queries': '''
update bo.t_export
set state=1
where classname = 'RawRegister'
and type='OEBS'
and object_id = 123
;
            ''',
            'execution_with_author': 'True',
            'summon': 'Александр Курицын (nevskiy)',
        }

    def get_result(self):
        res = RequiredResult(commit=True, delay=True)
        res.transition = IssueTransitions.none
        res.summonees = None
        return res


class HasApprovePrimaryManagersWOAuthorWOSupportApproveDBCase(AbstractDBTestCase):
    _representation = (
        'wo_support_approve_automatic_execution_primary_managers_wo_author_has_other_confirmation'
    )

    author = 'arkasha_primary'

    def prepare_data(self, session):
        return {
            'automatic_execution': 'У меня готовый sql-запрос для БД Баланса или Меты',
            'database_id': 'Balance Oracle',
            'queries': '''
update bo.t_export
set state=1
where classname = 'RawRegister'
and type='OEBS'
and object_id = 123
;
            ''',
            'execution_with_author': 'False',
            'summon': 'Александр Курицын (nevskiy)',
        }

    def get_result(self):
        res = RequiredResult(commit=True, delay=True)
        res.transition = IssueTransitions.none
        res.summonees = None
        return res


class ExecutionHasQueryWithAuthorDBCase(AbstractDBTestCase):
    _representation = 'execution_automatic_execution_with_author'

    author = 'arkasha'

    def get_comments(self):
        return [
            {
                'author': 'autodasha',
                'text': COMMENTS['get_approve'],
                'summonees': ['arkasha', 'nevskiy'],
            },
            ('truba', 'Подтверждено'),
            ('arkasha', 'Подтверждено'),
            ('nevskiy', 'Подтверждено'),
        ]

    def prepare_data(self, session):
        name = 'test_semi_auto_database_modify'
        cl = db_utils.create_client(session, name)
        self.queries = '''
update bo.t_client
  set name = 'test_semi_auto_database_modify_after_update'
where id = {cl_id}
    and name = 'test_semi_auto_database_modify'
;
dbms_output.put_line('test');
            '''.format(
                cl_id=cl.id
            )
        return {
            'automatic_execution': 'У меня готовый sql-запрос для БД Баланса или Меты',
            'database_id': 'Balance Oracle',
            'queries': self.queries,
            'execution_with_author': 'Да',
            'summon': 'Александр Курицын (nevskiy)',
        }

    def get_result(self):
        res = RequiredResult(commit=True, delay=False)
        res.modify = True
        res.transition = IssueTransitions.fixed
        res.summonees = None
        res.add_message(self._get_success_comment(self.queries))
        return res


class ExecutionHasQueryWOAuthorDBCase(AbstractDBTestCase):
    _representation = 'execution_automatic_execution_wo_author'

    author = 'arkasha'

    def get_comments(self):
        return [
            {
                'author': 'autodasha',
                'text': COMMENTS['get_approve'],
                'summonees': ['nevskiy'],
            },
            ('truba', 'Подтверждено'),
            ('nevskiy', 'Подтверждено'),
        ]

    def prepare_data(self, session):
        name = 'test_semi_auto_database_modify'
        cl = db_utils.create_client(session, name)
        self.queries = '''
update bo.t_client
  set name = 'test_semi_auto_database_modify_after_update'
where id = {cl_id}
    and name = 'test_semi_auto_database_modify'
;
begin
dbms_output.put_line('test');
end;
            '''.format(
                cl_id=cl.id
            )
        return {
            'automatic_execution': 'У меня готовый sql-запрос для БД Баланса или Меты',
            'database_id': 'Balance Oracle',
            'queries': self.queries,
            'execution_with_author': 'No',
            'summon': 'Александр Курицын (nevskiy)',
        }

    def get_result(self):
        res = RequiredResult(commit=True, delay=False)
        res.modify = True
        res.transition = IssueTransitions.fixed
        res.summonees = None
        res.add_message(self._get_success_comment(self.queries))
        return res


class ExecutionHasQueryWOAuthorJsonDBCase(AbstractDBTestCase):
    _representation = 'execution_automatic_execution_wo_author_json'

    author = 'arkasha'

    def get_comments(self):
        return [
            {
                'author': 'autodasha',
                'text': COMMENTS['get_approve'],
                'summonees': ['nevskiy'],
            },
            ('truba', 'Подтверждено'),
            ('nevskiy', 'Подтверждено'),
        ]

    def prepare_data(self, session):
        name = 'test_semi_auto_database_modify'
        cl = db_utils.create_client(session, name)
        self.queries = '''
begin
update bo.t_client
  set name = '{"who":100, "care": "blablabla"}'
where id = %s
    and name = 'test_semi_auto_database_modify'
;
end;
            ''' % cl.id
        return {
            'automatic_execution': 'У меня готовый sql-запрос для БД Баланса или Меты',
            'database_id': 'Balance Oracle',
            'queries': self.queries,
            'execution_with_author': 'No',
            'summon': 'Александр Курицын (nevskiy)',
        }

    def get_result(self):
        res = RequiredResult(commit=True, delay=False)
        res.json = True
        res.transition = IssueTransitions.fixed
        res.summonees = None
        res.add_message(self._get_success_comment(self.queries))
        return res


class ExecutionHasQueryPrimaryManagersWithAuthorDBCase(AbstractDBTestCase):
    _representation = 'execution_automatic_execution_primary_managers_with_author'

    author = 'arkasha_primary'

    def get_comments(self):
        return [
            {
                'author': 'autodasha',
                'text': COMMENTS['get_approve'],
                'summonees': ['arkasha_primary'],
            },
            ('truba', 'Подтверждено'),
            ('arkasha_primary', 'Подтверждено'),
        ]

    def prepare_data(self, session):
        name = 'test_semi_auto_database_modify'
        cl = db_utils.create_client(session, name)
        self.queries = '''
update bo.t_client
  set name = 'test_semi_auto_database_modify_after_update'
where id = {cl_id}
    and name = 'test_semi_auto_database_modify'
;
            '''.format(
                cl_id=cl.id
            )
        return {
            'automatic_execution': 'У меня готовый sql-запрос для БД Баланса или Меты',
            'database_id': 'Balance Oracle',
            'queries': self.queries,
            'execution_with_author': 'Да',
            'summon': 'Александр Курицын (nevskiy)',
        }

    def get_result(self):
        res = RequiredResult(commit=True, delay=False)
        res.modify = True
        res.transition = IssueTransitions.fixed
        res.summonees = None
        res.add_message(self._get_success_comment(self.queries))
        return res


class ExecutionPrimaryManagersWOAuthorDBCase(AbstractDBTestCase):
    _representation = 'execution_primary_managers_wo_author'

    author = 'arkasha_primary'

    def get_comments(self):
        return [
            ('ashul', 'Подтверждено'),
        ]

    def prepare_data(self, session):
        name = r'test_semi_auto_database_modify:100'
        cl = db_utils.create_client(session, name)
        self.queries = '''
update bo.t_client
  set name = 'test_semi_auto_database_modify_after_update'
where id = {cl_id}
    and name = 'test_semi_auto_database_modify:100'
;
            '''.format(
                cl_id=cl.id
            )
        return {
            'automatic_execution': 'У меня готовый sql-запрос для БД Баланса или Меты',
            'database_id': 'Balance Oracle',
            'queries': self.queries,
            'execution_with_author': 'False',
            'summon': 'Александр Курицын (nevskiy)',
        }

    def get_result(self):
        res = RequiredResult(commit=True, delay=False)
        res.modify = True
        res.transition = IssueTransitions.fixed
        res.summonees = None
        res.add_message(self._get_success_comment(self.queries))
        return res


class ExecutionPrimaryManagersWOAuthorSeveralQueriesDBCase(AbstractDBTestCase):
    _representation = 'execution_primary_managers_wo_author_several_queries'

    author = 'arkasha_primary'

    def get_comments(self):
        return [
            ('ashul', 'Подтверждено'),
        ]

    def prepare_data(self, session):
        name = 'test_semi_auto_database_modify'
        name2 = 'test_semi_auto_database_modify2'
        cl = db_utils.create_client(session, name)
        cl2 = db_utils.create_client(session, name2)
        self.queries = '''
update bo.t_client
  set name = 'test_semi_auto_database_modify_after_update'
where id = {cl_id}
    and name = 'test_semi_auto_database_modify'
;update bo.t_client
  set name = 'test_semi_auto_database_modify__after_update2'
where id = {cl2_id}
    and name = 'test_semi_auto_database_modify2'
;
dbms_output.put_line('test');
            '''.format(
                cl_id=cl.id, cl2_id=cl2.id
            )
        return {
            'automatic_execution': 'У меня готовый sql-запрос для БД Баланса или Меты',
            'database_id': 'Balance Oracle',
            'queries': self.queries,
            'execution_with_author': 'False',
            'summon': 'Александр Курицын (nevskiy)',
        }

    def get_result(self):
        res = RequiredResult(commit=True, delay=False)
        res.several_modify = True
        res.transition = IssueTransitions.fixed
        res.summonees = None
        res.add_message(self._get_success_comment(self.queries))
        return res


class ExecutionManyQueriesDBCase(AbstractDBTestCase):
    _representation = 'execution_many_queries'

    author = 'arkasha_primary'

    def get_comments(self):
        return [
            ('ashul', 'Подтверждено'),
        ]

    def prepare_data(self, session):
        import random

        query = ''
        for _ in range(50):
            v = random.randrange(10000000, 40000000, 1)
            query += '''insert into bo.t_enums_tree(id, parent_id, code, value) values(%s, 10, '%s', 'test_6666');''' % (v, v)
        self.queries = '''
{query}
            '''.format(
                query=query
            )
        return {
            'automatic_execution': 'У меня готовый sql-запрос для БД Баланса или Меты',
            'database_id': 'Balance Oracle',
            'queries': self.queries,
            'execution_with_author': 'False',
            'summon': 'Александр Курицын (nevskiy)',
        }

    def get_result(self):
        res = RequiredResult(commit=True, delay=False)
        res.transition = IssueTransitions.fixed
        res.summonees = None
        res.add_message(self._get_success_comment(self.queries))
        return res


class ExecutionTimeOutDBCase(AbstractDBTestCase):
    _representation = 'execution_time_out'

    author = 'arkasha_primary'

    def get_comments(self):
        return [
            ('ashul', 'Подтверждено'),
        ]

    def prepare_data(self, session):
        name = 'test_semi_auto_database_modify_wo_update'
        cl = db_utils.create_client(session, name)
        return {
            'automatic_execution': 'У меня готовый sql-запрос для БД Баланса или Меты',
            'database_id': 'Balance Oracle',
            'queries': '''
update bo.t_client
  set name = 'test_semi_auto_database_modify_after_update'
where id = {cl_id}
    and name = 'test_semi_auto_database_modify_wo_update'
;
update bo.t_payment
  set source_scheme = 'bs'
where id in (
  select
    id
  from bo.t_payment p
    join bo.t_payment p2
  on p.dt = p2.payment_dt
)
;
update bo.t_payment
  set source_scheme = 'bs'
where id in (
  select
    p.id
  from bo.t_payment p
  join bo.t_payment p2
    on p.amount = p2.id
)
;
            '''.format(
                cl_id=cl.id
            ),
            'execution_with_author': 'False',
            'summon': 'Александр Курицын (nevskiy)',
        }

    def get_result(self):
        res = RequiredResult(commit=True, delay=True)
        res.time_out = -1
        res.execution_time_out = True
        res.transition = IssueTransitions.none
        res.summonees = None
        res.add_message(COMMENTS['has_been_terminated'].format(time_out=res.time_out))
        return res


class ExecutionErrorDBCase(AbstractDBTestCase):
    _representation = 'execution_error'

    author = 'arkasha_primary'

    def get_comments(self):
        return [
            ('ashul', 'Подтверждено'),
        ]

    def prepare_data(self, session):
        return {
            'automatic_execution': 'У меня готовый sql-запрос для БД Баланса или Меты',
            'database_id': 'Balance Oracle',
            'queries': '''
raise
            ''',
            'execution_with_author': 'False',
            'summon': 'Александр Курицын (nevskiy)',
        }

    def get_result(self):
        res = RequiredResult(commit=True, delay=True)
        res.transition = IssueTransitions.none
        res.summonees = self.author
        res.add_message(
            COMMENTS['execution_failed'].format(
                _error='integer division or modulo by zero'
            )
        )
        return res


class SecondQueryExecutionErrorDBCase(AbstractDBTestCase):
    _representation = 'second_query_execution_error'

    author = 'arkasha_primary'

    def get_comments(self):
        return [
            ('ashul', 'Подтверждено'),
        ]

    def prepare_data(self, session):
        name = 'test_semi_auto_database_modify_second_query_error'
        cl = db_utils.create_client(session, name)
        return {
            'automatic_execution': 'У меня готовый sql-запрос для БД Баланса или Меты',
            'database_id': 'Balance Oracle',
            'queries': '''
update bo.t_client
  set name = 'test_semi_auto_database_modify_second_query_error_after_update'
where id = {cl_id}
    and name = 'test_semi_auto_database_modify_second_query_error'
;
bla bla bla
;
            '''.format(cl_id=cl.id),
            'execution_with_author': 'False',
            'summon': 'Александр Курицын (nevskiy)',
        }

    def get_result(self):
        res = RequiredResult(commit=True, delay=True)
        res.second_query_error = True
        res.transition = IssueTransitions.none
        res.summonees = self.author
        res.add_message(
            COMMENTS['execution_failed'].format(
                _error='integer division or modulo by zero'
            )
        )
        return res


class ExecutionErrorNotFixedDBCase(AbstractDBTestCase):
    _representation = 'execution_error_not_fixed'

    author = 'arkasha_primary'

    def get_comments(self):
        return [
            ('ashul', 'Подтверждено'),
            ('autodasha', COMMENTS['execution_failed'].format(_error='integer division or modulo by zero')),
        ]

    def prepare_data(self, session):
        return {
            'automatic_execution': 'У меня готовый sql-запрос для БД Баланса или Меты',
            'database_id': 'Balance Oracle',
            'queries': '''
raise
            ''',
            'execution_with_author': 'False',
            'summon': 'Александр Курицын (nevskiy)',
        }

    def get_result(self):
        res = RequiredResult(commit=True, delay=True)
        res.transition = IssueTransitions.none
        return res


class ExecutionErrorFixedWOSummonDBCase(AbstractDBTestCase):
    _representation = 'execution_error_fixed_wo_summon'

    author = 'arkasha_primary'

    def get_comments(self):
        return [
            ('ashul', 'Подтверждено'),
            ('autodasha', COMMENTS['execution_failed'].format(_error='integer division or modulo by zero')),
            (self.author, 'Исправлено'),
        ]

    def prepare_data(self, session):
        return {
            'automatic_execution': 'У меня готовый sql-запрос для БД Баланса или Меты',
            'database_id': 'Balance Oracle',
            'queries': '''
raise
            ''',
            'execution_with_author': 'False',
            'summon': 'Александр Курицын (nevskiy)',
        }

    def get_result(self):
        res = RequiredResult(commit=True, delay=True)
        res.transition = IssueTransitions.none
        return res


class ExecutionErrorFixedWrongDBCase(AbstractDBTestCase):
    _representation = 'execution_error_fixed_wrong'

    author = 'arkasha_primary'

    def get_comments(self):
        return [
            ('ashul', 'Подтверждено'),
            ('autodasha', COMMENTS['execution_failed'].format(_error='integer division or modulo by zero')),
            {
                'author': self.author,
                'text': 'Исправлено.',
                'summonees': ['autodasha']
            },
        ]

    def prepare_data(self, session):
        return {
            'automatic_execution': 'У меня готовый sql-запрос для БД Баланса или Меты',
            'database_id': 'Balance Oracle',
            'queries': '''
raise
            ''',
            'execution_with_author': 'False',
            'summon': 'Александр Курицын (nevskiy)',
        }

    def get_result(self):
        res = RequiredResult(commit=True, delay=True)
        res.transition = IssueTransitions.none
        res.summonees = self.author
        res.add_message(
            COMMENTS['execution_failed'].format(
                _error='integer division or modulo by zero'
            )
        )
        return res


class ExecutionErrorFixedDBCase(AbstractDBTestCase):
    _representation = 'execution_error_fixed'

    author = 'arkasha_primary'

    def get_comments(self):
        return [
            ('ashul', 'Подтверждено'),
            ('autodasha', COMMENTS['execution_failed'].format(_error='integer division or modulo by zero')),
            {
                'author': self.author,
                'text': 'Исправлено.',
                'summonees': ['autodasha']
            },
        ]

    def prepare_data(self, session):
        name = 'test_semi_auto_database_modify_error_fixed'
        cl = db_utils.create_client(session, name)
        self.queries = '''
update bo.t_client
  set name = 'test_semi_auto_database_modify_error_fixed_after_update'
where id = {cl_id}
    and name = 'test_semi_auto_database_modify_error_fixed'
;
            '''.format(cl_id=cl.id)
        return {
            'automatic_execution': 'У меня готовый sql-запрос для БД Баланса или Меты',
            'database_id': 'Balance Oracle',
            'queries': self.queries,
            'execution_with_author': 'False',
            'summon': 'Александр Курицын (nevskiy)',
        }

    def get_result(self):
        res = RequiredResult(commit=True, delay=False)
        res.transition = IssueTransitions.fixed
        res.summonees = None
        res.add_message(self._get_success_comment(self.queries))
        return res


class TimeoutFixedDBCase(AbstractDBTestCase):
    _representation = 'timeout_fixed'

    author = 'arkasha_primary'

    def get_comments(self):
        return [
            ('ashul', 'Подтверждено'),
            ('autodasha', COMMENTS['has_been_terminated'].format(time_out=300)),
            {
                'author': self.author,
                'text': 'Исправлено.',
                'summonees': ['autodasha']
            },
        ]

    def prepare_data(self, session):
        name = 'test_semi_auto_database_modify_timeout_fixed'
        cl = db_utils.create_client(session, name)
        self.queries = '''
update bo.t_client
  set name = 'test_semi_auto_database_modify_timeout_fixed_after_update'
where id = {cl_id}
    and name = 'test_semi_auto_database_modify_timeout_fixed'
;
            '''.format(cl_id=cl.id)
        return {
            'automatic_execution': 'У меня готовый sql-запрос для БД Баланса или Меты',
            'database_id': 'Balance Oracle',
            'queries': self.queries,
            'execution_with_author': 'False',
            'summon': 'Александр Курицын (nevskiy)',
        }

    def get_result(self):
        res = RequiredResult(commit=True, delay=False)
        res.transition = IssueTransitions.fixed
        res.summonees = None
        res.add_message(self._get_success_comment(self.queries))
        return res


class ExecutionErrorFixedSecondAttemptDBCase(AbstractDBTestCase):
    _representation = 'execution_error_fixed_second_attempt'

    author = 'arkasha_primary'

    def get_comments(self):
        return [
            ('ashul', 'Подтверждено'),
            ('autodasha', COMMENTS['execution_failed'].format(_error='integer division or modulo by zero')),
            {
                'author': self.author,
                'text': 'Исправлено.',
                'summonees': ['autodasha']
            },
            ('autodasha', COMMENTS['execution_failed'].format(_error='integer division or modulo by zero')),
            {
                'author': self.author,
                'text': 'Исправлено.',
                'summonees': ['autodasha']
            },
        ]

    def prepare_data(self, session):
        name = 'test_semi_auto_database_modify_error_fixed_second_attempt'
        cl = db_utils.create_client(session, name)
        self.queries = '''
update bo.t_client
  set name = 'test_semi_auto_database_modify_error_fixed_second_attempt_after_update'
where id = {cl_id}
    and name = 'test_semi_auto_database_modify_error_fixed_second_attempt'
;
            '''.format(cl_id=cl.id)
        return {
            'automatic_execution': 'У меня готовый sql-запрос для БД Баланса или Меты',
            'database_id': 'Balance Oracle',
            'queries': self.queries,
            'execution_with_author': 'False',
            'summon': 'Александр Курицын (nevskiy)',
        }

    def get_result(self):
        res = RequiredResult(commit=True, delay=False)
        res.transition = IssueTransitions.fixed
        res.summonees = None
        res.add_message(self._get_success_comment(self.queries))
        return res


def mock_staff(testfunc):
    autodasha = staff_utils.Person('autodasha')
    person = staff_utils.Person('nevskiy')
    person_2 = staff_utils.Person('ashul')
    person_3 = staff_utils.Person('truba')
    person_4 = staff_utils.Person('arkasha_primary')
    person_5 = staff_utils.Person('arkasha')
    other_boss = staff_utils.Person('other_boss')
    other_dept = staff_utils.Department(
        'other_dept',
        [other_boss],
        [],
        [autodasha, person, person_2, person_3, person_4, person_5],
    )

    yandex = staff_utils.Department('yandex', childs=[other_dept])

    staff = staff_utils.StaffMock(yandex)

    staff_path = 'autodasha.core.api.staff.Staff.%s'

    @mock.patch(
        staff_path % '_get_person_data',
        lambda s, *a, **k: staff._get_person_data(*a, **k),
    )
    @mock.patch(staff_path % '__init__', lambda *args: None)
    @functools.wraps(testfunc)
    def deco(session, issue_data):
        return testfunc(session, issue_data)

    return deco


@mock_staff
@mock.patch(
    'autodasha.solver_cl.semi_auto_database_modify.SemiAutoDatabaseModifySolver._execute_queries',
    _execute_queries,
)
@mock.patch(
    'autodasha.solver_cl.base_solver.BaseSolver.get_approve_message',
    get_approve_message,
)
@pytest.mark.parametrize(
    'issue_data',
    [case() for case in AbstractDBTestCase._cases],
    ids=lambda case: str(case),
    indirect=['issue_data'],
)
def test_db(session, issue_data):
    queue_object, st_issue, case = issue_data
    solver = SemiAutoDatabaseModifySolver(queue_object, st_issue)
    req_res = case.get_result()
    solver.sleep_time = req_res.sleep_time
    solver.time_out = req_res.time_out

    res = solver.solve()
    report = res.issue_report

    assert res.commit == req_res.commit
    assert res.delay == req_res.delay

    report_comments = []
    for c in report.comments:
        c_text = c['text']
        for part in c_text.strip().split('\n'):
            report_comments.append(part.strip())

    req_res_comments = []
    for c in req_res.comments:
        for part in c.strip().split('\n'):
            req_res_comments.append(part.strip())

    report_comments = sorted(filter(None, report_comments))
    req_res_comments = sorted(filter(None, req_res_comments))

    if req_res.second_query_error:
        cl = (
            session.query(db_utils.mapper.Client)
                .filter_by(name='test_semi_auto_database_modify_second_query_error_after_update')
                .first()
        )
        assert not cl
    else:
        assert len(report_comments) == len(req_res_comments)
        for i in range(len(req_res_comments)):
            assert req_res_comments[i] == report_comments[i]

        assert report.transition == req_res.transition
        assert report.assignee == req_res.assignee

    if req_res.modify:
        cl = (
            session.query(db_utils.mapper.Client)
            .filter_by(name='test_semi_auto_database_modify_after_update')
            .first()
        )
        assert cl

    if req_res.json:
        cl = (
            session.query(db_utils.mapper.Client)
                .filter_by(name='{"who":100, "care": "blablabla"}')
                .first()
        )
        assert cl

    if req_res.several_modify:
        cl = (
            session.query(db_utils.mapper.Client)
            .filter_by(name='test_semi_auto_database_modify_after_update')
            .first()
        )
        cl2 = (
            session.query(db_utils.mapper.Client)
            .filter_by(name='test_semi_auto_database_modify__after_update2')
            .first()
        )
        assert cl
        assert cl2

    if req_res.execution_time_out:
        cl = (
            session.query(db_utils.mapper.Client)
            .filter_by(name='test_semi_auto_database_modify_wo_update')
            .first()
        )
        assert cl

