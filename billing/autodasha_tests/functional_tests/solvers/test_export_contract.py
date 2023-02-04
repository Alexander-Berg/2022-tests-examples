# -*- coding: utf-8 -*-
from __future__ import unicode_literals


import collections
import datetime as dt
import pytest

import sqlalchemy as sa

from autodasha.core.api.tracker import IssueReport, IssueTransitions
from autodasha.db import mapper as a_mapper
from autodasha.solver_cl import ExportContract, base
from balance import mapper

from tests.autodasha_tests.functional_tests import case_utils
from tests.autodasha_tests.functional_tests import mock_utils
from tests.autodasha_tests.common import db_utils

ERROR_THRESHOLD = 2
WAITING_THRESHOLD = 7
UNEXPORTABLE_TYPES = set(['PREFERRED_DEAL', 'ACQUIRING'])

CHECK_COMMENTS = {
        'multiple_contracts_found_check':
            'Следующим №№ договоров соответствует более одного договора в Биллинге: ',
        'inconsistent_contract_ids_eids_check':
            'Невозможно сопоставить список неоднозначных договоров '
}

COMMENTS = {
    'form_incorrect':
        'Не удалось извлечь необходимые данные из условия задачи. '
        'Пожалуйста, уточни данные и создай новую задачу через ту же форму. ',
    'contracts_not_found':
        'Следующие договоры не найдены в Биллинге: {}. '
        'Пожалуйста, уточни данные и создай новую задачу через ту же форму. ',
    'enqueued':
        'Договоры и их ДС добавлены в очередь на выгрузку в OEBS. ',
    'multiple_contracts_found':
        '%s%s' % (CHECK_COMMENTS['multiple_contracts_found_check'],
        '{}. Для идентификации скопируй, пожалуйста, и приложи адресные строки нужных договоров в одном комментарии построчно. '
        '\nhttps://yandex.ru/images/'),
    'multiple_contracts_found_check':
        CHECK_COMMENTS['multiple_contracts_found_check'],
    'inconsistent_contract_ids_eids':
        '%s%s' % (CHECK_COMMENTS['inconsistent_contract_ids_eids_check'],
        '({}) и ссылки на них. Для идентификации скопируй, пожалуйста, и приложи адресные строки нужных договоров в одном комментарии построчно. '
        '\nhttps://yandex.ru/images/'),
    'inconsistent_contract_ids_eids_check':
        CHECK_COMMENTS['inconsistent_contract_ids_eids_check'],
    'thresholds_exceeded':
        'Данные не были корректно уточнены. '
        'Если задача актуальна, создай, пожалуйста, новую задачу через ту же форму. ',
    'unexportable_types':
        'Договоры типов {} нельзя выгружать в OEBS. '
        'Остальные объекты будут выгружены в стандартном режиме. ',
    'already_solved':
            'Эта задача уже была выполнена. '
            'Направьте новый запрос через '
            '((https://wiki.yandex-team.ru/otdelsoprovozhdeniya/dokument/avtomatformi/ формы)). '
            'Если не найдёте подходящую, заполните, пожалуйста, общую форму.'
}


class AbstractMockTestCase(case_utils.AbstractMockTestCase):
    _default_lines = {
        'summary': 'Перевыгрузить Договор и ДС в OEBS',
        'contract_eids': 'Список договоров:%s',
        'comment': 'Комментарий:-'
    }

    @staticmethod
    def unique_eids(contracts):
        return ', '.join(list(collections.OrderedDict.fromkeys(c.external_id for c in contracts)))


class AbstractParseFailTestCase(AbstractMockTestCase):
    _cases = []
    __metaclass__ = case_utils.TestCaseMeta


class AbstractParseSuccessTestCase(AbstractMockTestCase):
    _cases = []
    __metaclass__ = case_utils.TestCaseMeta


class InvalidContractListCase(AbstractParseFailTestCase):
    _representation = 'invalid_contract_list'

    def get_data(self, mock_manager):
        lines = [
            self._get_default_line(contract_eids=' ,,, , , '),
            self._get_default_line('comment')
        ]
        return self._get_default_line('summary'), lines

    def get_result(self):
        return COMMENTS['form_incorrect']


class MissingContractCase(AbstractParseFailTestCase):
    _representation = 'single_contract_not_found'

    def get_data(self, mock_manager):
        lines = [
            self._get_default_line(contract_eids='PAC-0'),
            self._get_default_line('comment')
        ]
        return self._get_default_line('summary'), lines

    def get_result(self):
        return COMMENTS['contracts_not_found'].format('PAC-0')


class ExistingContractCase(AbstractParseSuccessTestCase):
    _representation = 'single_contract_found'

    def get_data(self, mock_manager):
        self.contracts = [
            mock_utils.create_contract(mock_manager, external_id='PAC-0')
        ]
        lines = [
            self._get_default_line(contract_eids=self.unique_eids(self.contracts)),
            self._get_default_line('comment')
        ]
        return self._get_default_line('summary'), lines

    def get_result(self):
        return self.contracts


class MissingContractsCase(AbstractParseFailTestCase):
    _representation = 'multiple_contracts_not_found'

    def get_data(self, mock_manager):
        lines = [
            self._get_default_line(contract_eids='PAC-0, PAC-1'),
            self._get_default_line('comment')
        ]
        return self._get_default_line('summary'), lines

    def get_result(self):
        return COMMENTS['contracts_not_found'].format('PAC-0, PAC-1')


class ExistingContractsCase(AbstractParseSuccessTestCase):
    _representation = 'multiple_contracts_found'

    def get_data(self, mock_manager):
        self.contracts = [
            mock_utils.create_contract(mock_manager, external_id='PAC-0'),
            mock_utils.create_contract(mock_manager, external_id='PAC-1')
        ]
        lines = [
            self._get_default_line(contract_eids=self.unique_eids(self.contracts)),
            self._get_default_line('comment')
        ]
        return self._get_default_line('summary'), lines

    def get_result(self):
        return self.contracts


class MissingContractsMixedCase(AbstractParseFailTestCase):
    _representation = 'multiple_contracts_found_not_found'

    def get_data(self, mock_manager):
        self.contracts = [
            mock_utils.create_contract(mock_manager, external_id='PAC-0')
        ]
        lines = [
            self._get_default_line(contract_eids='PAC-0, PAC-1'),
            self._get_default_line('comment')
        ]
        return self._get_default_line('summary'), lines

    def get_result(self):
        return COMMENTS['contracts_not_found'].format('PAC-1')


class NoLinksCase(AbstractParseFailTestCase):
    _representation = 'no_links_in_comment'

    def get_data(self, mock_manager):
        self.contracts = [
            mock_utils.create_contract(mock_manager, external_id='PAC-0')
        ]
        lines = [
            self._get_default_line(contract_eids=self.unique_eids(self.contracts)),
            self._get_default_line('comment')
        ]
        return self._get_default_line('summary'), lines

    def get_comments(self):
        our_comment = COMMENTS['multiple_contracts_found'].format(self.unique_eids(self.contracts))
        link_comment = ' '
        return [
            ('autodasha', our_comment, dt.datetime(2017, 11, 11)),
            ('author', link_comment, dt.datetime(2017, 11, 12))
        ]

    def get_result(self):
        return ''


class NoMoreLinksCase(AbstractParseFailTestCase):
    _representation = 'no_links_in_more_comments'

    def get_data(self, mock_manager):
        self.contracts = [
            mock_utils.create_contract(mock_manager, external_id='PAC-0')
        ]
        lines = [
            self._get_default_line(contract_eids=self.unique_eids(self.contracts)),
            self._get_default_line('comment')
        ]
        return self._get_default_line('summary'), lines

    def get_comments(self):
        our_comment = COMMENTS['inconsistent_contract_ids_eids'].format(self.unique_eids(self.contracts))
        link_comment = ' '
        return [
            ('autodasha', our_comment, dt.datetime(2017, 11, 11)),
            ('author', link_comment, dt.datetime(2017, 11, 12))
        ]

    def get_result(self):
        return ''


@pytest.mark.parametrize('mock_issue_data',
                         [case() for case in AbstractParseFailTestCase._cases],
                         ids=lambda case: str(case),
                         indirect=['mock_issue_data'])
def test_fail_cases(mock_queue_object, mock_issue_data):
    ri = IssueReport()
    issue, case = mock_issue_data
    required_res = case.get_result()
    solver = ExportContract(mock_queue_object, issue)
    with pytest.raises(Exception) as e:
        solver.get_contracts(ri)
    assert required_res == e.value.message


@pytest.mark.parametrize('mock_issue_data',
                         [case() for case in AbstractParseSuccessTestCase._cases],
                         ids=lambda case: str(case),
                         indirect=['mock_issue_data'])
def test_success_cases(mock_queue_object, mock_issue_data):
    ri = IssueReport()
    issue, case = mock_issue_data
    required_res = case.get_result()
    solver = ExportContract(mock_queue_object, issue)
    res = solver.get_contracts(ri)
    assert required_res == res



class RequiredResult(case_utils.RequiredResult):

    def __init__(self, **kwargs):
        self.contracts = kwargs.get('contracts')
        self.c_exports = []
        self.col_exports = []
        self.delay = False
        self.commit = True
        super(RequiredResult, self).__init__(**kwargs)


class AbstractDBTestCase(case_utils.AbstractDBTestCase):
    _cases = []
    __metaclass__ = case_utils.TestCaseMeta

    _summary = 'Перевыгрузить Договор и ДС в OEBS'
    _description = '''
Список договоров:{contract_eids}
Комментарий:-
'''.strip()

    def __init__(self):
        super(AbstractDBTestCase, self).__init__()
        self.contracts = None
        self.link = 'https://admin.balance.yandex.ru/contract.xml?contract_id={}'
        self.author = 'iuriiz'

    @staticmethod
    def unique_eids(contracts):
        return ', '.join(list(collections.OrderedDict.fromkeys(c.external_id for c in contracts)))


class DiffCaseContractCase(AbstractDBTestCase):
    _representation = 'multiple_diff_case_contract_eids'

    def _get_data(self, session):
        client = db_utils.create_client(session)
        person = db_utils.create_person(session, client)
        self.contracts = [
            db_utils.create_general_contract(session, client, person, external_id='PAC-0'),
            db_utils.create_general_contract(session, client, person, external_id='PAC-1')
        ]
        return {
            'contract_eids': ' pAc-0 , Pac-1 '
        }

    def get_result(self):
        res = RequiredResult(transition=IssueTransitions.none, contracts=self.contracts)
        res.add_message((COMMENTS['enqueued'], ''))
        res.c_exports = [c.id for c in self.contracts]
        res.col_exports = reduce(lambda x, y: x + [col.id for col in y if y],
                                 map(lambda c: c.collaterals, self.contracts), [])
        res.commit = True
        res.delay = False
        return res


class DuplicateContractCase(AbstractDBTestCase):
    _representation = 'single_duplicate_contract'

    def _get_data(self, session):
        client = db_utils.create_client(session)
        person = db_utils.create_person(session, client)
        self.contracts = [
            db_utils.create_general_contract(session, client, person, external_id='PAC-0'),
            db_utils.create_general_contract(session, client, person, external_id='PAC-0')
        ]
        return {
            'contract_eids': self.unique_eids(self.contracts)
        }

    def get_result(self):
        res = RequiredResult(transition=IssueTransitions.none)
        res.add_message((COMMENTS['multiple_contracts_found'].format(self.unique_eids(self.contracts)), self.author))
        res.commit = True
        res.delay = True
        return res


class MixedContractsCase(AbstractDBTestCase):
    _representation = 'multiple_contracts_unique_duplicate'

    def _get_data(self, session):
        client = db_utils.create_client(session)
        person = db_utils.create_person(session, client)
        self.contracts = [
            db_utils.create_general_contract(session, client, person, external_id='PAC-0'),
            db_utils.create_general_contract(session, client, person, external_id='PAC-0'),
            db_utils.create_general_contract(session, client, person, external_id='PAC-1')
        ]
        return {
            'contract_eids': self.unique_eids(self.contracts)
        }

    def get_result(self):
        res = RequiredResult(transition=IssueTransitions.none)
        res.add_message((COMMENTS['multiple_contracts_found'].format(self.contracts[0].external_id), self.author))
        res.commit = True
        res.delay = True
        return res


class DuplicateContractLinkCase(AbstractDBTestCase):
    _representation = 'single_duplicate_contract_link'

    def _get_data(self, session):
        client = db_utils.create_client(session)
        person = db_utils.create_person(session, client)
        self.contracts = [
            db_utils.create_general_contract(session, client, person, external_id='PAC-0'),
            db_utils.create_general_contract(session, client, person, external_id='PAC-0')
        ]
        for c in self.contracts:
            db_utils.add_collateral(c)
        return {
            'contract_eids': self.unique_eids(self.contracts)
        }

    def get_comments(self):
        return [
            ('autodasha', COMMENTS['multiple_contracts_found'].format(self.unique_eids(self.contracts)),
               dt.datetime.today()),
            (self.author, self.link.format(self.contracts[0].id), dt.datetime.now())
        ]

    def get_result(self):
        res = RequiredResult(transition=IssueTransitions.none, contracts=[self.contracts[0]])
        res.add_message((COMMENTS['enqueued'], ''))
        res.c_exports = [self.contracts[0].id]
        res.col_exports = [col.id for col in self.contracts[0].collaterals]
        res.commit = True
        res.delay = False
        return res


class MixedContractsLinkCase(AbstractDBTestCase):
    _representation = 'multiple_contracts_unique_duplicate_link'

    def _get_data(self, session):
        client = db_utils.create_client(session)
        person = db_utils.create_person(session, client)
        self.contracts = [
            db_utils.create_general_contract(session, client, person, external_id='PAC-0'),
            db_utils.create_general_contract(session, client, person, external_id='PAC-0'),
            db_utils.create_general_contract(session, client, person, external_id='PAC-1')
        ]
        for c in self.contracts:
            db_utils.add_collateral(c)
        return {
            'contract_eids': self.unique_eids(self.contracts)
        }

    def get_comments(self):
        return [
            ('autodasha', COMMENTS['multiple_contracts_found'].format(self.contracts[1].external_id),
               dt.datetime.today()),
            (self.author, self.link.format(self.contracts[1].id), dt.datetime.now())
        ]

    def get_result(self):
        res = RequiredResult(transition=IssueTransitions.none, contracts=self.contracts[1:])
        res.add_message((COMMENTS['enqueued'], ''))
        res.c_exports = [c.id for c in self.contracts[1:]]
        res.col_exports = reduce(lambda x, y: x + [col.id for col in y if y],
                                 map(lambda c: c.collaterals, self.contracts[1:]), [])
        res.commit = True
        res.delay = False
        return res


class DuplicateContractLinkRetryCase(AbstractDBTestCase):
    _representation = 'single_duplicate_contract_link_retry'

    def _get_data(self, session):
        client = db_utils.create_client(session)
        person = db_utils.create_person(session, client)
        self.contracts = [
            db_utils.create_general_contract(session, client, person, external_id='PAC-0'),
            db_utils.create_general_contract(session, client, person, external_id='PAC-0')
        ]
        return {
            'contract_eids': self.unique_eids(self.contracts)
        }

    def get_comments(self):
        return [
            ('autodasha', COMMENTS['multiple_contracts_found'].format(self.unique_eids(self.contracts)),
               dt.datetime.today()),
            (self.author, self.link.format('1{}').format(self.contracts[0].id), dt.datetime.now())
        ]

    def get_result(self):
        res = RequiredResult(transition=IssueTransitions.none)
        res.add_message((COMMENTS['inconsistent_contract_ids_eids'].format(self.contracts[0].external_id), self.author))
        res.commit = True
        res.delay = True
        return res


class MixedContractsLinkRetryCase(AbstractDBTestCase):
    _representation = 'multiple_contracts_unique_duplicate_link_retry'

    def _get_data(self, session):
        client = db_utils.create_client(session)
        person = db_utils.create_person(session, client)
        self.contracts = [
            db_utils.create_general_contract(session, client, person, external_id='PAC-0'),
            db_utils.create_general_contract(session, client, person, external_id='PAC-0'),
            db_utils.create_general_contract(session, client, person, external_id='PAC-1')
        ]
        return {
            'contract_eids': self.unique_eids(self.contracts)
        }

    def get_comments(self):
        return [
            ('autodasha', COMMENTS['multiple_contracts_found'].format(self.contracts[0].external_id),
               dt.datetime.today()),
            (self.author, self.link.format('1{}').format(self.contracts[0].id), dt.datetime.now())
        ]

    def get_result(self):
        res = RequiredResult(transition=IssueTransitions.none)
        res.add_message((COMMENTS['inconsistent_contract_ids_eids'].format(self.contracts[0].external_id), self.author))
        res.commit = True
        res.delay = True
        return res


class DuplicateContractLinkNRetryCase(AbstractDBTestCase):
    _representation = 'single_duplicate_contract_link_N_retry'

    def _get_data(self, session):
        client = db_utils.create_client(session)
        person = db_utils.create_person(session, client)
        self.contracts = [
            db_utils.create_general_contract(session, client, person, external_id='PAC-0'),
            db_utils.create_general_contract(session, client, person, external_id='PAC-0')
        ]
        return {
            'contract_eids': self.unique_eids(self.contracts)
        }

    def get_comments(self):
        return [
            ('autodasha', COMMENTS['inconsistent_contract_ids_eids'].format(self.unique_eids(self.contracts)),
                dt.datetime.today() - dt.timedelta(days=(ERROR_THRESHOLD) - i))
            for i in range(ERROR_THRESHOLD)
        ] + [
            (self.author, self.link.format('1{}').format(self.contracts[0].id), dt.datetime.now())
        ]

    def get_result(self):
        res = RequiredResult(transition=IssueTransitions.wont_fix)
        res.add_message((COMMENTS['thresholds_exceeded'], self.author))
        res.commit = True
        res.delay = False
        return res


class MixedContractsLinkNRetryCase(AbstractDBTestCase):
    _representation = 'multiple_contracts_unique_duplicate_link_N_retry'

    def _get_data(self, session):
        client = db_utils.create_client(session)
        person = db_utils.create_person(session, client)
        self.contracts = [
            db_utils.create_general_contract(session, client, person, external_id='PAC-0'),
            db_utils.create_general_contract(session, client, person, external_id='PAC-0'),
            db_utils.create_general_contract(session, client, person, external_id='PAC-1')
        ]
        return {
            'contract_eids': self.unique_eids(self.contracts)
        }

    def get_comments(self):
        return [
            ('autodasha', COMMENTS['inconsistent_contract_ids_eids'].format(self.unique_eids(self.contracts)),
                dt.datetime.today() - dt.timedelta(days=(ERROR_THRESHOLD) - i))
            for i in range(ERROR_THRESHOLD)
        ] + [
            (self.author, self.link.format('1{}').format(self.contracts[0].id), dt.datetime.now())
        ]

    def get_result(self):
        res = RequiredResult(transition=IssueTransitions.wont_fix)
        res.add_message((COMMENTS['thresholds_exceeded'], self.author))
        res.commit = True
        res.delay = False
        return res


class WaitingThresholdExceededCase(AbstractDBTestCase):
    _representation = 'waiting_threshold_exceeded'

    def _get_data(self, session):
        client = db_utils.create_client(session)
        person = db_utils.create_person(session, client)
        self.contracts = [
            db_utils.create_general_contract(session, client, person)
        ]
        return {
            'contract_eids': self.unique_eids(self.contracts)
        }

    def get_comments(self):
        return [
            ('autodasha', COMMENTS['multiple_contracts_found'].format(self.contracts[0].external_id),
                dt.datetime.today() - dt.timedelta(days=WAITING_THRESHOLD))
        ]

    def get_result(self):
        res = RequiredResult(transition=IssueTransitions.wont_fix)
        res.add_message((COMMENTS['thresholds_exceeded'], self.author))
        res.commit = True
        res.delay = False
        return res


class WaitingThresholdExceededRetryCase(AbstractDBTestCase):
    _representation = 'waiting_threshold_exceeded_after_retry'

    def _get_data(self, session):
        client = db_utils.create_client(session)
        person = db_utils.create_person(session, client)
        self.contracts = [
            db_utils.create_general_contract(session, client, person)
        ]
        return {
            'contract_eids': self.unique_eids(self.contracts)
        }

    def get_comments(self):
        return [
            ('autodasha', COMMENTS['inconsistent_contract_ids_eids'].format(self.contracts[0].external_id),
                dt.datetime.today() - dt.timedelta(days=WAITING_THRESHOLD))
        ]

    def get_result(self):
        res = RequiredResult(transition=IssueTransitions.wont_fix)
        res.add_message((COMMENTS['thresholds_exceeded'], self.author))
        res.commit = True
        res.delay = False
        return res


class UnexportableContractsCase(AbstractDBTestCase):
    _representation = 'unexportable_contracts'

    def _get_data(self, session):
        client = db_utils.create_client(session)
        person = db_utils.create_person(session, client)
        self.contracts = []
        self.contracts.append(db_utils.create_acquiring_contract(session, client, person, external_id='a1826'))
        return {
            'contract_eids': self.unique_eids(self.contracts)
        }

    def get_result(self):
        res = RequiredResult(transition=IssueTransitions.wont_fix,
                             contracts=filter(lambda c: c.type not in UNEXPORTABLE_TYPES, self.contracts))
        res.add_message((COMMENTS['unexportable_types'].format(', '.join(UNEXPORTABLE_TYPES)), ''))
        res.commit = True
        res.delay = False
        return res


class UnexportableContractsMixedCase(AbstractDBTestCase):
    _representation = 'unexportable_contracts_mixed'

    def _get_data(self, session):
        client = db_utils.create_client(session)
        person = db_utils.create_person(session, client)
        self.contracts = [
            db_utils.create_general_contract(session, client, person)
        ]
        self.contracts.append(db_utils.create_acquiring_contract(session, client, person, external_id='a1826'))
        self.contracts.append(db_utils.create_preferred_deal_contract(session, client, person, external_id='b1826'))
        return {
            'contract_eids': self.unique_eids(self.contracts)
        }

    def get_result(self):
        contracts = filter(lambda c: c.type not in UNEXPORTABLE_TYPES, self.contracts)
        res = RequiredResult(transition=IssueTransitions.none,
                             contracts=contracts)
        res.add_message(('\n'.join((COMMENTS['unexportable_types'].format(', '.join(UNEXPORTABLE_TYPES)),
                                    COMMENTS['enqueued'])), ''))
        res.c_exports = [c.id for c in contracts]
        res.col_exports = reduce(lambda x, y: x + [col.id for col in y if y],
                                  map(lambda c: c.collaterals, contracts), [])
        res.commit = True
        res.delay = False
        return res


class ExportEnqueuedCase(AbstractDBTestCase):
    _representation = 'export_enqueued'

    def _get_data(self, session):
        client = db_utils.create_client(session)
        person = db_utils.create_person(session, client)
        self.contracts = [
            db_utils.create_general_contract(session, client, person)
        ]
        return {
            'contract_eids': self.unique_eids(self.contracts)
        }

    def get_comments(self):
        return [
            ('autodasha', COMMENTS['enqueued'])
        ]

    def get_result(self):
        return None


class AlreadySolvedCase(AbstractDBTestCase):
    _representation = 'already_solved'

    last_resolved = True

    def _get_data(self, session):
        client = db_utils.create_client(session)
        person = db_utils.create_person(session, client)
        self.contracts = [
            db_utils.create_general_contract(session, client, person)
        ]
        return {
            'contract_eids': self.unique_eids(self.contracts)
        }

    def get_comments(self):
        return [
            ('autodasha', COMMENTS['enqueued'])
        ]

    def get_result(self):
        res = RequiredResult(transition=IssueTransitions.wont_fix, contracts=self.contracts)
        res.add_message((COMMENTS['already_solved'], ''))
        res.commit = False
        res.delay = False
        return res


@pytest.mark.parametrize('issue_data',
                         [case() for case in AbstractDBTestCase._cases],
                         ids=lambda case: str(case),
                         indirect=['issue_data'])
def test_db(session, issue_data):
    queue_object, st_issue, case = issue_data

    required_res = case.get_result()
    solver = ExportContract(queue_object, st_issue)
    res = solver.solve()
    session.flush()

    if required_res is None:
        assert res.commit is False
        assert res.delay is False
        assert res.issue_report is None
        return

    assert res.commit == required_res.commit
    assert res.delay == required_res.delay
    report = res.issue_report
    assert len(report.comments) <= 1
    assert set([(comment.text, ', '.join(comment.summonees or [])) for comment in report.comments]) \
           == set([(comment.text, ', '.join(comment.summonees or []))
                   for comment in map(lambda cmt: res.issue_report._create_comment(text=cmt[0], summonees=[cmt[1]]),
                       required_res.comments)])
    assert report.transition == required_res.transition
    assert report.assignee == required_res.assignee

    try:
        export_queue = session.query(a_mapper.QueueObject) \
            .filter(a_mapper.QueueObject.issue == queue_object.issue,
                    a_mapper.QueueObject.processor == 'EXPORT_CHECK') \
            .one()
    except sa.orm.exc.NoResultFound:
        assert not (required_res.c_exports or required_res.col_exports)
    else:
        c_exports = {obj.object_id for obj in export_queue.proxies if obj.classname == 'Contract'}
        col_exports = {obj.object_id for obj in export_queue.proxies if obj.classname == 'ContractCollateral'}
        assert c_exports | col_exports == {obj.object_id for obj in export_queue.proxies}

        assert c_exports == set(required_res.c_exports)
        assert col_exports == set(required_res.col_exports)
