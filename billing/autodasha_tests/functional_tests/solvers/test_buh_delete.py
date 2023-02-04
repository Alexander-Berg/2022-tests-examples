# -*- coding: utf-8 -*-

from __future__ import unicode_literals

import pytest

from autodasha.solver_cl import BuhDelete, ParseException
from autodasha.core.api.tracker import IssueTransitions

from tests.autodasha_tests.common import db_utils
from tests.autodasha_tests.functional_tests import case_utils
from tests.autodasha_tests.functional_tests import mock_utils


COMMENTS = {
    #  Не указано ничего про клиента
    'incorrect_client_login_and_id':
        'Неверно указан ID и логин клиента. Пожалуйста, уточни данные и создай новую задачу через ту же форму.',

    #  Указан id клиета
    'incorrect_client_id':
        'Неверно указан ID клиента. Пожалуйста, уточни данные и создай новую задачу через ту же форму.',

    'client_id_not_found':
        'Клиент с ID {} не найден в базе. Пожалуйста, уточни данные и создай новую задачу через ту же форму.',

    #  Указан логин клиента
    'incorrect_client_login':
        'Неверно указан логин клиента. Пожалуйста, уточни данные и создай новую задачу через ту же форму.',

    'client_login_not_found':
        'Логин {} не найден в базе. Пожалуйста, уточни данные и создай новую задачу через ту же форму.',

    'not_unique_client_login':
        'Логин {} не уникальный. Коллеги, посмотрите, пожалуйста.',

    'login_without_client':
        'К логину {} не привязан клиент. '
        'Пожалуйста, уточни данные и создай новую задачу через ту же форму.',

    #  Не указан логин бухгалтера
    'incorrect_buh_login':
        'Неверно указан логин бухгалтера. '
        'Пожалуйста, уточни данные и создай новую задачу через ту же форму.',

    'buhlogin_not_found':
        'Логин бухгалтера {} не найден в базе. '
        'Пожалуйста, уточни данные и создай новую задачу через ту же форму.',

    'not_unique_buhlogin':
        'Логин бухгалтера {} не уникальный. Коллеги, посмотрите, пожалуйста.',

    #  Проверка связи клиента с бухлогином
    'relation_error_wo_logins':
        'Логин {} не является бухгалтерским логином для клиента с ID {}. '
        'У данного клиента нет бухгалтерских логинов. '
        'Пожалуйста, уточни данные и создай новую задачу через ту же форму.',

    'relation_error_w_logins':
        'Логин {} не является бухгалтерским логином для клиента с ID {}. '
        'У данного клиента есть следующие бухгалтерские логины: {}. '
        'Пожалуйста, уточни данные и создай новую задачу через ту же форму.',

    #  Произошло что-то ужасное
    'fatal_error':
        'Что-то пошло не так. Произошла техническая ошибка.'
        'Коллеги, посмотрите, пожалуйста.',

    #  Задача выполнена
    'resolved':
        'Доступ у бухгалтерского логина {} для клиента с ID {} удален.',
}


def create_role_client_user(session, buh_obj, client_obj, role_id=100):
    sql = 'insert into bo.t_role_client_user (passport_id, client_id, role_id) ' \
          'values (:passport_id, :client_id, :role_id)'

    session.execute(sql, {
            'passport_id': buh_obj.passport_id,
            'client_id': client_obj.id,
            'role_id': role_id
        }
    )


class RequiredResult(case_utils.RequiredResult):
    def __init__(self, **kwargs):
        self.roles = {}

        super(RequiredResult, self).__init__(**kwargs)

    def add_role(self, buh_obj, client_obj, role_id):
        if self.roles.get(buh_obj.passport_id):
            self.roles[buh_obj.passport_id].add((client_obj.id, role_id))
        else:
            self.roles[buh_obj.passport_id] = {(client_obj.id, role_id)}


class AbstractDBTestCase(case_utils.AbstractDBTestCase):
    _summary = 'Удаление логина бухгалтера'
    _description = '''
Логин (id) клиента: {client_data}
Логин бухгалтера: {buh_login}
Причина, по которой нужно удалить логин: да потому что я так хочу епта
'''
    issue_key = 'test_buh_delete'

    _cases = []
    __metaclass__ = case_utils.TestCaseMeta

    def __init__(self):
        super(AbstractDBTestCase, self).__init__()
        self.buh_login = None
        self.client_data = None


class DeleteSuccessCase(AbstractDBTestCase):
    _representation = 'delete_success'

    def _get_data(self, session):
        self.client_obj = db_utils.create_client(session)
        self.buh_obj = db_utils.create_passport(session)
        self.random_buh_obj = db_utils.create_passport(session, 'randomlogin2017pro', 2130000022965666)
        self.random_client_obj = db_utils.create_client(session)

        create_role_client_user(session, self.buh_obj, self.client_obj)
        create_role_client_user(session, self.buh_obj, self.client_obj, role_id=200)
        create_role_client_user(session, self.random_buh_obj, self.client_obj)
        create_role_client_user(session, self.buh_obj, self.random_client_obj)

        return {
                'client_data': self.client_obj.id,
                'buh_login': self.buh_obj.login
        }

    def get_result(self):
        res = RequiredResult(transition=IssueTransitions.fixed)
        res.add_role(self.buh_obj, self.client_obj, 200)
        res.add_role(self.random_buh_obj, self.client_obj, 100)
        res.add_role(self.buh_obj, self.random_client_obj, 100)
        res.add_message(COMMENTS['resolved'].format(self.buh_obj.login, self.client_obj.id))
        return res


class DeleteWBadLoginCase(AbstractDBTestCase):
    _representation = 'delete_w_bad_login'

    def _get_data(self, session):
        self.client_obj = db_utils.create_client(session)
        self.buh_obj = db_utils.create_passport(session)
        self.random_buh_obj = db_utils.create_passport(session, 'randomlogin2017pro', 2130000022965666)
        create_role_client_user(session, self.random_buh_obj, self.client_obj)
        return {
            'client_data': self.client_obj.id,
            'buh_login': self.buh_obj.login
        }

    def get_result(self):
        res = RequiredResult(transition=IssueTransitions.wont_fix)
        res.add_role(self.random_buh_obj, self.client_obj, 100)
        res.add_message(COMMENTS['relation_error_w_logins'].format(self.buh_obj.login, self.client_obj.id, 'randomlogin2017pro'))
        return res


class DeleteWBadClientCase(AbstractDBTestCase):
    _representation = 'delete_w_bad_client'

    def _get_data(self, session):
        self.client_obj = db_utils.create_client(session)
        self.buh_obj = db_utils.create_passport(session)
        self.random_client_obj = db_utils.create_client(session)
        create_role_client_user(session, self.buh_obj, self.random_client_obj)
        return {
            'client_data': self.client_obj.id,
            'buh_login': self.buh_obj.login
        }

    def get_result(self):
        res = RequiredResult(transition=IssueTransitions.wont_fix)
        res.add_role(self.buh_obj, self.random_client_obj, 100)
        res.add_message(COMMENTS['relation_error_wo_logins'].format(self.buh_obj.login, self.client_obj.id))
        return res


class NotUniqueLoginCase(AbstractDBTestCase):
    _representation = 'not_unique_login'

    def _get_data(self, session):
        self.client_data = db_utils.create_passport(session, login='superpuperuniqe', uid=2130000022965346)
        self.random_client_data = db_utils.create_passport(session, login='superpuperuniqe', uid=2130000022965347)
        self.buh_obj = db_utils.create_passport(session)
        return {
            'client_data': self.client_data.login,
            'buh_login': self.buh_obj.login
        }

    def get_result(self):
        res = RequiredResult(transition=IssueTransitions.opened)
        res.assignee = 'mscnad7'
        res.add_message(COMMENTS['not_unique_client_login'].format(self.client_data.login))
        return res


class NotUniqueBuhLoginCase(AbstractDBTestCase):
    _representation = 'not_unique_buh_login'

    def _get_data(self, session):
        self.client_data = db_utils.create_client(session)
        self.buh_obj = db_utils.create_passport(session, login='superpuperuniqe', uid=2130000022965347)
        self.random_buh_obj = db_utils.create_passport(session, login='superpuperuniqe', uid=2130000022965348)
        return {
            'client_data': self.client_data.id,
            'buh_login': self.buh_obj.login
        }

    def get_result(self):
        res = RequiredResult(transition=IssueTransitions.opened)
        res.assignee = 'mscnad7'
        res.add_message(COMMENTS['not_unique_buhlogin'].format(self.buh_obj.login))
        return res


class DeleteWBadRoleCase(AbstractDBTestCase):
    _representation = 'delete_w_bad_role'

    def _get_data(self, session):
        self.client_obj = db_utils.create_client(session)
        self.buh_obj = db_utils.create_passport(session)
        create_role_client_user(session, self.buh_obj, self.client_obj, role_id=200)
        return {
            'client_data': self.client_obj.id,
            'buh_login': self.buh_obj.login
        }

    def get_result(self):
        res = RequiredResult(transition=IssueTransitions.wont_fix)
        res.add_role(self.buh_obj, self.client_obj, 200)
        res.add_message(COMMENTS['relation_error_wo_logins'].format(self.buh_obj.login, self.client_obj.id))
        return res


@pytest.mark.parametrize('issue_data',
                         [case() for case in AbstractDBTestCase._cases],
                         ids=lambda case: str(case),
                         indirect=['issue_data'])
def test_db(session, issue_data):
    queue_object, st_issue, case = issue_data
    req_res = case.get_result()

    solver = BuhDelete(queue_object, st_issue)
    res = solver.solve()
    session.flush()

    if req_res is None:
        assert res.commit is False
        assert res.delay is True
        assert res.issue_report is None
        return

    assert res.commit is True
    assert res.delay is False
    report = res.issue_report

    comments_parts = filter(None, map(unicode.strip, report.comment.strip().split('\n')))
    assert set(req_res.comments) == set(comments_parts)
    assert len(report.comments) <= 1
    assert report.transition == req_res.transition
    assert report.assignee == req_res.assignee

    sql = """
        select client_id, role_id
        from bo.t_role_client_user
        where passport_id = :passport_id
    """

    for passport_id, req_data in req_res.roles.items():
        curr_data = map(tuple, session.execute(sql, {'passport_id': passport_id}).fetchall())
        assert set(curr_data) == req_data


class AbstractParseTestCase(case_utils.AbstractMockTestCase):
    _default_lines = {
        'summary': 'Удаление логина бухгалтера',
        'client': 'Логин (id) клиента: %s',
        'buh_login': 'Логин бухгалтера: %s',
        'reason': 'Причина, по которой нужно удалить логин: удаляй молча, тварь'
    }

    def __init__(self):
        self.client = None
        self.buh_login = None


class AbstractFailedCheckFormTestCase(AbstractParseTestCase):
    _cases = []
    __metaclass__ = case_utils.TestCaseMeta


class AbstractSuccessCheckFormTestCase(AbstractParseTestCase):
    _cases = []
    __metaclass__ = case_utils.TestCaseMeta


class InvalidFormClientCheckTestCase(AbstractFailedCheckFormTestCase):
    _representation = 'wrong_client_form'

    def get_data(self, mock_manager):
        client = mock_utils.create_client(mock_manager)
        buh_login = mock_utils.create_passport(mock_manager)

        lines = [
            'Логин логина: %s' % client.id,
            self._get_default_line(buh_login=buh_login.login),
            self._get_default_line('reason')
        ]
        return self._get_default_line('summary'), lines

    def get_result(self):
        return COMMENTS['incorrect_client_login_and_id']


class InvalidFormBuhCheckTestCase(AbstractFailedCheckFormTestCase):
    _representation = 'wrong_buh_form'

    def get_data(self, mock_manager):
        client = mock_utils.create_client(mock_manager)
        buh_login = mock_utils.create_passport(mock_manager)

        lines = [
            self._get_default_line(client=client.id),
            'логин бахгулеоаыта: %s' % buh_login.login,
            self._get_default_line('reason')
        ]
        return self._get_default_line('summary'), lines

    def get_result(self):
        return COMMENTS['incorrect_buh_login']


class UnknownClientIdCase(AbstractFailedCheckFormTestCase):
    _representation = 'unknown_client_id'

    def get_data(self, mock_manager):
        mock_utils.create_client(mock_manager, id_='7777')
        buh_login = mock_utils.create_passport(mock_manager)
        lines = [
            self._get_default_line(client=6666),
            self._get_default_line(buh_login=buh_login.login),
            self._get_default_line('reason')
        ]
        return self._get_default_line('summary'), lines

    def get_result(self):
        return COMMENTS['client_id_not_found'].format(6666)


class BadClientLoginCase(AbstractFailedCheckFormTestCase):
    _representation = 'bad_client_login_case'

    def get_data(self, mock_manager):
        lines = [
            self._get_default_line(client='11111lalalalalala'),
            self._get_default_line(buh_login='lala'),
            self._get_default_line('reason')
        ]
        return self._get_default_line('summary'), lines

    def get_result(self):
        return COMMENTS['incorrect_client_login']


class BadBuhLoginCase(AbstractFailedCheckFormTestCase):
    _representation = 'bad_buh_login_case'

    def get_data(self, mock_manager):
        client = mock_utils.create_client(mock_manager, id_=6666)
        client_login = mock_utils.create_passport(mock_manager, login='lalala', client=client).login
        lines = [
            self._get_default_line(client=client_login),
            self._get_default_line(buh_login='1lala'),
            self._get_default_line('reason')
        ]
        return self._get_default_line('summary'), lines

    def get_result(self):
        return COMMENTS['incorrect_buh_login']


class UnknownClientLoginCase(AbstractFailedCheckFormTestCase):
    _representation = 'unknown_client_login'

    def get_data(self, mock_manager):
        client = mock_utils.create_client(mock_manager, id_=6666)
        mock_utils.create_passport(mock_manager, login='login', client=client)
        buh_login = mock_utils.create_passport(mock_manager)
        lines = [
            self._get_default_line(client='ne_login'),
            self._get_default_line(buh_login=buh_login.login),
            self._get_default_line('reason')
        ]
        return self._get_default_line('summary'), lines

    def get_result(self):
        return COMMENTS['client_login_not_found'].format('ne_login')


class UnknownBuhLoginCase(AbstractFailedCheckFormTestCase):
    _representation = 'unknown_buh_login'

    def get_data(self, mock_manager):
        client = mock_utils.create_client(mock_manager, id_='6666666666')
        mock_utils.create_passport(mock_manager, login='kartofanchik')

        lines = [
            self._get_default_line(client=client.id),
            self._get_default_line(buh_login='kartofanchik1234'),
            self._get_default_line('reason')
        ]
        return self._get_default_line('summary'), lines

    def get_result(self):
        return COMMENTS['buhlogin_not_found'].format('kartofanchik1234')


class ClientLoginWOClientIdCase(AbstractFailedCheckFormTestCase):
    _representation = 'client_login_without_client'

    def get_data(self, mock_manager):
        client_login = mock_utils.create_passport(mock_manager, login='lalka', client=None)
        buh_login = mock_utils.create_passport(mock_manager, login='buhlogin2017')
        lines = [
            self._get_default_line(client=client_login.login),
            self._get_default_line(buh_login=buh_login.login),
            self._get_default_line('reason')
        ]
        return self._get_default_line('summary'), lines

    def get_result(self):
        return COMMENTS['login_without_client'].format('lalka')


class IncorrectBuhLoginCase(AbstractFailedCheckFormTestCase):
    _representation = 'incorrect_buh_login'

    def get_data(self, mock_manager):
        self.client = mock_utils.create_client(mock_manager, id_='6666')
        client_login = mock_utils.create_passport(mock_manager, login='lalka', client=self.client)
        buh_login = mock_utils.create_passport(mock_manager, login='12345')
        lines = [
            self._get_default_line(client=client_login.login),
            self._get_default_line(buh_login=buh_login.login),
            self._get_default_line('reason')
        ]
        return self._get_default_line('summary'), lines

    def get_result(self):
        return COMMENTS['incorrect_buh_login']


class SeveralBuhLoginCase(AbstractFailedCheckFormTestCase):
    _representation = 'several_buh_logins'

    def get_data(self, mock_manager):
        self.client = mock_utils.create_client(mock_manager, id_='6666')
        lines = [
            self._get_default_line(client=self.client.id),
            self._get_default_line(buh_login=' lala, lololo, lelele '),
            self._get_default_line('reason')
        ]
        return self._get_default_line('summary'), lines

    def get_result(self):
        return COMMENTS['incorrect_buh_login']


class SeveralClientLoginCase(AbstractFailedCheckFormTestCase):
    _representation = 'several_client_logins'

    def get_data(self, mock_manager):
        lines = [
            self._get_default_line(client=' client1, client2, client3 '),
            self._get_default_line(buh_login='lala'),
            self._get_default_line('reason')
        ]
        return self._get_default_line('summary'), lines

    def get_result(self):
        return COMMENTS['incorrect_client_login']


class SeveralClientIdsCase(AbstractFailedCheckFormTestCase):
    _representation = 'several_client_ids'

    def get_data(self, mock_manager):
        lines = [
            self._get_default_line(client=' 1113, 1114, 1114 '),
            self._get_default_line(buh_login='lala'),
            self._get_default_line('reason')
        ]
        return self._get_default_line('summary'), lines

    def get_result(self):
        return COMMENTS['incorrect_client_id']


class HardClientIdSuccessCase(AbstractSuccessCheckFormTestCase):
    _representation = 'hard_id_case'

    def get_data(self, mock_manager):
        self.client = mock_utils.create_client(mock_manager, id_='66666')
        self.buh_login = mock_utils.create_passport(mock_manager, login='buhlogin')
        lines = [
            self._get_default_line(client='Тестим автодашу. Найди давай вот этот ID - 66666 ахахах'),
            self._get_default_line(buh_login=self.buh_login.login),
            self._get_default_line('reason')
        ]
        return self._get_default_line('summary'), lines

    def get_result(self):
        return self.buh_login, self.client


class HardLoginsSuccessCase(AbstractSuccessCheckFormTestCase):
    _representation = 'hard_logins_case'

    def get_data(self, mock_manager):
        self.client = mock_utils.create_client(mock_manager, id_='66666')
        client_login = mock_utils.create_passport(mock_manager, login='l-43242432_1_l', client=self.client).login
        self.buh_login = mock_utils.create_passport(mock_manager, login='yandex-ruuu-15')
        lines = [
            self._get_default_line(client=client_login),
            self._get_default_line(buh_login='yandex.ruuu-15'),
            self._get_default_line('reason')
        ]
        return self._get_default_line('summary'), lines

    def get_result(self):
        return self.buh_login, self.client


class JustIdSuccessCase(AbstractSuccessCheckFormTestCase):
    _representation = 'just_id_success_parse'

    def get_data(self, mock_manager):
        self.client = mock_utils.create_client(mock_manager, id_='66666')
        self.buh_login = mock_utils.create_passport(mock_manager, login='buhlogin')
        lines = [
            self._get_default_line(client=self.client.id),
            self._get_default_line(buh_login=self.buh_login.login),
            self._get_default_line('reason')
        ]
        return self._get_default_line('summary'), lines

    def get_result(self):
        return self.buh_login, self.client


class IdWithSpacesSuccessCase(AbstractSuccessCheckFormTestCase):
    _representation = 'id_with_spaces'

    def get_data(self, mock_manager):
        self.client = mock_utils.create_client(mock_manager, id_='66666')
        self.buh_login = mock_utils.create_passport(mock_manager, login='buhlogin')
        lines = [
            self._get_default_line(client=' %s ' % self.client.id),
            self._get_default_line(buh_login=self.buh_login.login),
            self._get_default_line('reason')
        ]
        return self._get_default_line('summary'), lines

    def get_result(self):
        return self.buh_login, self.client


class LoginsWithSpacesSuccessCase(AbstractSuccessCheckFormTestCase):
    _representation = 'logins_with_spaces'

    def get_data(self, mock_manager):
        self.client = mock_utils.create_client(mock_manager, id_='6666')
        client_login = mock_utils.create_passport(mock_manager, login='lalka', client=self.client).login
        self.buh_login = mock_utils.create_passport(mock_manager, login='buhlogin')
        lines = [
            self._get_default_line(client=' %s ' % client_login),
            self._get_default_line(buh_login=' %s ' % self.buh_login.login),
            self._get_default_line('reason')
        ]
        return self._get_default_line('summary'), lines

    def get_result(self):
        return self.buh_login, self.client


class JustLoginSuccessCase(AbstractSuccessCheckFormTestCase):
    _representation = 'just_login_success_parse'

    def get_data(self, mock_manager):
        self.client = mock_utils.create_client(mock_manager, id_='6666')
        client_login = mock_utils.create_passport(mock_manager, login='login', client=self.client).login
        self.buh_login = mock_utils.create_passport(mock_manager, login='buhlogin')
        lines = [
            self._get_default_line(client=client_login),
            self._get_default_line(buh_login=self.buh_login.login),
            self._get_default_line('reason')
        ]
        return self._get_default_line('summary'), lines

    def get_result(self):
        return self.buh_login, self.client


class LoginWithIdSuccessCase(AbstractSuccessCheckFormTestCase):
    _representation = 'login_with_id_success_parse'

    def get_data(self, mock_manager):
        self.client = mock_utils.create_client(mock_manager, id_='6666')
        client_login = mock_utils.create_passport(mock_manager, login='login666', client=self.client).login
        self.buh_login = mock_utils.create_passport(mock_manager, login='buhlogin')
        lines = [
            self._get_default_line(client='%s (%s)' % (client_login, self.client.id)),
            self._get_default_line(buh_login=self.buh_login.login),
            self._get_default_line('reason')
        ]
        return self._get_default_line('summary'), lines

    def get_result(self):
        return self.buh_login, self.client


@pytest.mark.parametrize('mock_issue_data',
                         [case() for case in AbstractSuccessCheckFormTestCase._cases],
                         ids=lambda case: str(case),
                         indirect=['mock_issue_data'])
def test_parse_success(mock_queue_object, mock_issue_data):
    issue, case = mock_issue_data
    required_res = case.get_result()
    solver = BuhDelete(mock_queue_object, issue)
    res = solver.parse_issue()

    assert required_res == res


@pytest.mark.parametrize('mock_issue_data',
                         [case() for case in AbstractFailedCheckFormTestCase._cases],
                         ids=lambda case: str(case),
                         indirect=['mock_issue_data'])
def test_parse_failing(mock_queue_object, mock_issue_data):
    issue, case = mock_issue_data

    req_comment = case.get_result()

    solver = BuhDelete(mock_queue_object, issue)
    with pytest.raises(ParseException) as exc:
        solver.parse_issue()

    assert req_comment in exc.value.message
