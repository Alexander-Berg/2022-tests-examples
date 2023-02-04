# -*- coding: utf-8 -*-

from __future__ import unicode_literals

import pytest
import mock

from autodasha.solver_cl import ReportSolver, FreeFundsSolver, ParseException
from autodasha.core.api.tracker import IssueReport

from tests.autodasha_tests.functional_tests import case_utils
from tests.autodasha_tests.functional_tests import mock_utils


def _fake_init(self, *args, **kwargs):
    super(ReportSolver, self).__init__(*args, **kwargs)
    self.ro_session = self.session


COMMENTS = {
        'empty_result': 'Нет свободных средств.',
        'parsed_no_data': 'Не указаны данные для выполнения выгрузки.',
        'already_solved':
            'Эта задача уже была выполнена. Направьте новый запрос через '
            '((https://wiki.yandex-team.ru/otdelsoprovozhdeniya/dokument/avtomatformi/ формы)). '
            'Если не найдёте подходящую, заполните, пожалуйста, общую форму.',
        'empty_free_funds': 'Нет свободных средст.',
        'done': 'Выгрузка во вложении.',
        'wrong_input_data': 'По указанным параметрам нельзя сделать выгрузку. Проверь вводимые данные.',
        'not_found_in_db': '%s: %s - фильтр не найден в базе, проверь данные, пожалуйста.',
        'found_in_db': 'Использовался фильтр %s: %s.',
        'login_without_client': 'У представителя %s нет привязанного клиента.'
    }


class AbstractMockTestCase(case_utils.AbstractMockTestCase):
    _default_lines = {
        'summary': 'Выгрузка свободных средств',
        'contract_external_id': 'Номер договора: %s',
        'agency_id': 'ID агентства: %s',
        'client_id': 'ID клиента: %s',
        'login': 'Login представителя: %s',
        'comment': 'Ваш комментарий к задаче: %s'
    }

class AbstractFailingMockTestCase(AbstractMockTestCase):
    _cases = []
    __metaclass__ = case_utils.TestCaseMeta


class NotFoundDataInDbCase(AbstractFailingMockTestCase):
    _representation = 'not_found_in_db'

    def get_data(self, mock_manager):
        mock_utils.create_contract(mock_manager, external_id='111111111113113')
        lines = [
            self._get_default_line(contract_external_id='1111вв111111133'),
            self._get_default_line(agency_id=' aaaa'),
            self._get_default_line(client_id=' '),
            self._get_default_line(login=''),
            self._get_default_line(comment='comment suka')
        ]
        return self._get_default_line('summary'), lines

    def get_result(self):
        comments = (COMMENTS.get('not_found_in_db') % ('contract', '1111вв111111133')) + COMMENTS.get('wrong_input_data')
        return comments


class NoValuersFilledCase(AbstractFailingMockTestCase):
    _representation = 'no_values_filled'

    def get_data(self, mock_manager):
        lines = [
            self._get_default_line(contract_external_id=''),
            self._get_default_line(agency_id='  '),
            self._get_default_line(client_id=' '),
            self._get_default_line(login=''),
            self._get_default_line(comment='comment suka')
        ]
        return self._get_default_line('summary'), lines

    def get_result(self):
        return 'Не указаны данные для выполнения выгрузки.'


class LoginWithoutClientCase(AbstractFailingMockTestCase):
    _representation = 'login_without_client'

    def get_data(self, mock_manager):
        mock_utils.create_passport(mock_manager, login='login')
        lines = [
            self._get_default_line(contract_external_id=''),
            self._get_default_line(agency_id='  '),
            self._get_default_line(client_id=' '),
            self._get_default_line(login='login'),
            self._get_default_line(comment='comment suka')
        ]
        return self._get_default_line('summary'), lines

    def get_result(self):
        comments = (COMMENTS.get('login_without_client') % 'login') + COMMENTS.get('wrong_input_data')
        return comments


@mock.patch('autodasha.solver_cl.base_solver.ReportSolver.__init__', _fake_init)
@pytest.mark.parametrize('mock_issue_data',
                         [case() for case in AbstractFailingMockTestCase._cases],
                         ids=lambda case: str(case),
                         indirect=['mock_issue_data'])
def test_parse_failing(mock_queue_object, mock_issue_data):
    ri = IssueReport()
    issue, case = mock_issue_data

    req_comment = case.get_result()

    solver = FreeFundsSolver(mock_queue_object, issue)
    with pytest.raises(ParseException) as exc:
        solver.parse_issue(ri)

    # assert req_comment in exc.value.message
    if ri.comments:
        comments = [text.get('text') for text in ri.comments]
        comments = ' '.join(comments)
        if comments:
            comments += exc.value.message
        assert req_comment == comments
    else:
        assert req_comment in exc.value.message


class AbstractGoodMockTestCase(AbstractMockTestCase):
    _cases = []
    __metaclass__ = case_utils.TestCaseMeta


class AllValuesFilledCase(AbstractGoodMockTestCase):
    _representation = 'all_values_filled'

    def get_data(self, mock_manager):
        mock_utils.create_contract(mock_manager, external_id='333333/24')
        mock_utils.create_client(mock_manager, id_=123)
        mock_utils.create_client(mock_manager, id_=311)
        mock_utils.create_passport(mock_manager, login='login', client_id=311)
        lines = [
            self._get_default_line(contract_external_id='333333/24 привет'),
            self._get_default_line(agency_id='123    '),
            self._get_default_line(client_id='   311'),
            self._get_default_line(login='login pidor'),
            self._get_default_line(comment='comment suka')
        ]
        return self._get_default_line('summary'), lines

    def get_result(self):
        return {
            'contract': '333333/24',
            'agency_id': '123',
            'client_id': '311',
            'login': 'login'
        }


class NotAllValuesFilledCase(AbstractGoodMockTestCase):
    _representation = 'not_all_values_filled'

    def get_data(self, mock_manager):
        mock_utils.create_contract(mock_manager, external_id='353333/24')
        mock_utils.create_client(mock_manager, id_=321)
        lines = [
            self._get_default_line(contract_external_id='   353333/24 привет'),
            self._get_default_line(agency_id='  '),
            self._get_default_line(client_id='321'),
            self._get_default_line(login='   '),
            self._get_default_line(comment='comment suka')
        ]
        return self._get_default_line('summary'), lines

    def get_result(self):
        return {
            'contract': '353333/24',
            'agency_id': None,
            'client_id': '321',
            'login': None
        }


@mock.patch('autodasha.solver_cl.base_solver.ReportSolver.__init__', _fake_init)
@pytest.mark.parametrize('mock_issue_data',
                         [case() for case in AbstractGoodMockTestCase._cases],
                         ids=lambda case: str(case),
                         indirect=['mock_issue_data'])
def test_parse_good(mock_queue_object, mock_issue_data):
    ri = IssueReport()
    issue, case = mock_issue_data

    req_res = case.get_result()

    solver = FreeFundsSolver(mock_queue_object, issue)
    res = solver.parse_issue(ri)

    assert req_res['contract'] == res.get('contract')
    assert req_res['agency_id'] == res.get('agency_id')
    assert req_res['client_id'] == res.get('client_id')
    assert req_res['login'] == res.get('login')
