# -*- coding: utf-8 -*-

from __future__ import unicode_literals

import pytest
import itertools

from autodasha.solver_cl import BlacklistedUsers, ParseException
from autodasha.core.api.tracker import IssueTransitions
from autodasha.core.api.tracker import IssueReport

from tests.autodasha_tests.functional_tests import case_utils
from tests.autodasha_tests.functional_tests import mock_utils
from tests.autodasha_tests.common import db_utils


DEFAULT_MASKED_PAN = '111111****2222'


COMMENTS = {
    'payment_not_found':
        'Платеж с trust_payment_id {} не найден в базе данных.\n'
        'Посмотрите, пожалуйста.',
    'invalid_issue_data':
        'Неверно указан trust_payment_id платежа.\n'
        'Должен состоять из 24-х символов.',
    'additional_support_comment':
        '//Убедись с помощью комментария выше, '
        'что точно нужно писать в процессинг по поводу фрода.//\n'
        'Нужно запросить у процессинга:\n'
        '%%Коллеги, добрый день!\n'
        'Платёж по заказу {trust_payment_id} не прошел с ошибкой по фроду.\n'
        'Подскажите, пожалуйста, в чем причина?\n'
        'Также по указанному заказу просьба добавить карту в WL.\n'
        'Спасибо.%%',
    'info_comment':
        '**UID пользователя**: {user_uid}\n'
        '**Маска карты**: {masked_pan}\n'
        '**trust_payment_id**: {trust_payment_id}\n'
        '**Процессинг**: {processing_info}\n'
        '**Сервис**: {service_info}\n'
        '**RESP_DESC платежа**: {payment_resp_desc}\n'
        '**Правила**: {fraud_resp_desc}',
    'unknown_error':
        'Произошла техническая ошибка',
    'additional_fraud_comment':
        'Посмотрите, пожалуйста.',
}


class RequiredResult(case_utils.RequiredResult):
    def __init__(self, **kwargs):
        self.commit = kwargs.get('commit')
        self.delay = kwargs.get('delay')
        self.summonees = kwargs.get('summonees')
        self.tags = ['blacklisted'] + kwargs.get('tags', [])
        super(RequiredResult, self).__init__(**kwargs)


class AbstractDBTestCase(case_utils.AbstractDBTestCase):
    _summary = 'Непрохождение платежа (blacklisted)'
    _description = '''
Сервис: Такси
Trust_payment_id: {trust_payment_id}
Комментарий:

'''.strip()
    issue_key = 'test_blacklisted_users'

    _cases = []
    __metaclass__ = case_utils.TestCaseMeta

    def __init__(self):
        super(AbstractDBTestCase, self).__init__()
        self.trust_payment = None
        self.config = None

    def setup_config(self, session, config):
        config['BLACKLISTED_USERS_SETTINGS'] = {
            'fraud_managers': ['fraud_m1', 'fraud_m2'],
            'support_manager': 'super_support',
            'allowed_service_ids': [125, 605],
            'allowed_processing_ids': [10105],
            'white_rules': ['lalka1', 'lalka2'],
            'incorrect_rule_patterns': ['.+?DIRECT_PAYMENT policy has error.+?', '.+?bad argument.+?',
                                        '.+?attempt to compare.+?']
        }
        self.config = config

    def get_payment_data(self, fraud_rules):
        processing_info = '%s (%s)' % (self.trust_payment.terminal.processing.cc,
                                       self.trust_payment.terminal.processing_id)
        service_info = '%s (%s)' % (self.trust_payment.service.name,
                                    self.trust_payment.service_id)

        data = {
            'masked_pan':
                DEFAULT_MASKED_PAN.replace('*', 'X'),
            'user_uid':
                self.trust_payment.creator_uid,
            'trust_payment_id':
                self.trust_payment.trust_payment_id,
            'service_info':
                service_info,
            'payment_resp_desc':
                self.trust_payment.resp_desc,
            'fraud_resp_desc':
                ', '.join(fraud_rules) if fraud_rules else 'отсутствуют',
            'processing_info': processing_info
        }

        return data


class NoPFSStandartCase(AbstractDBTestCase):
    _representation = 'no_pfs_standart_case'

    def _get_data(self, session):
        self.trust_payment = db_utils.create_trust_payment(session, 124, 10001)
        return {
            'trust_payment_id': self.trust_payment.trust_payment_id
        }

    def get_result(self):
        res = RequiredResult(transition=IssueTransitions.opened,
                             commit=True, delay=False)
        res.add_message(COMMENTS['info_comment'].format(**self.get_payment_data([])))
        res.add_message(COMMENTS['additional_support_comment'].format(
            trust_payment_id=self.trust_payment.trust_payment_id))
        res.assignee = 'super_support'
        res.summonees = ['super_support']
        return res


class PFSWithNoSuccessStatusCase(AbstractDBTestCase):
    _representation = 'pfs_with_no_success_status'

    def _get_data(self, session):
        afs_data = {
            'afs_status': 'lal',
            'afs_resp_desc': 'oioi'
        }
        self.trust_payment = db_utils.create_trust_payment(session, 124, 10001, afs_data)
        return {
            'trust_payment_id': self.trust_payment.trust_payment_id
        }

    def get_result(self):
        res = RequiredResult(transition=IssueTransitions.opened,
                             commit=True, delay=False)
        res.add_message(COMMENTS['info_comment'].format(**self.get_payment_data([])))
        res.add_message(COMMENTS['additional_support_comment']
                        .format(trust_payment_id=self.trust_payment.trust_payment_id))
        res.assignee = 'super_support'
        res.summonees = ['super_support']
        return res


# class PFSWithNoSuccessStatusAndAllowedProcessingIDCase(AbstractDBTestCase):
#     _representation = 'pfs_with_no_success_status_and_allowed_processing_id'
#
#     def _get_data(self, session):
#         afs_data = {
#             'afs_status': 'lal',
#             'afs_resp_desc': 'oioi'
#         }
#         self.trust_payment = db_utils.create_trust_payment(session, 124, 10105, afs_data)
#         return {
#             'trust_payment_id': self.trust_payment.trust_payment_id
#         }
#
#     def get_result(self):
#         res = RequiredResult(transition=IssueTransitions.opened,
#                              commit=True, delay=False)
#         res.add_message(COMMENTS['success_comment'].format(**self.get_payment_data([])))
#         res.assignee = 'super_support'
#         res.summonees = ['super_support']
#         return res


# class PFSWithSuccessStatusWithoutAfsRespDescCase(AbstractDBTestCase):
#     _representation = 'pfs_with_success_status_wo_afs_resp_desc'
#
#     def _get_data(self, session):
#         afs_data = {
#             'afs_status': 'success',
#         }
#         self.trust_payment = db_utils.create_trust_payment(session, 124, 10105, afs_data)
#         return {
#             'trust_payment_id': self.trust_payment.trust_payment_id
#         }
#
#     def get_result(self):
#         res = RequiredResult(transition=IssueTransitions.opened,
#                              commit=True, delay=False)
#         res.add_message(COMMENTS['fraud_manager_success_comment']
#                         .format(**self.get_payment_data([])))
#         res.assignee = 'super_support'
#         res.summonees = ['super_support']
#         return res


# class NoPFSWithAllowedServiceIDCase(AbstractDBTestCase):
#     _representation = 'no_pfs_with_allowed_service_id'
#
#     def _get_data(self, session):
#         self.trust_payment = db_utils.create_trust_payment(session, 125, 10001)
#         return {
#             'trust_payment_id': self.trust_payment.trust_payment_id
#         }
#
#     def get_result(self):
#         res = RequiredResult(transition=IssueTransitions.opened,
#                              commit=True, delay=False)
#         res.add_message(COMMENTS['fraud_manager_success_comment']
#                         .format(**self.get_payment_data([])))
#         res.assignee = 'super_support'
#         res.summonees = ['super_support']
#         return res


# class NoPFSWithAllowedProcessingIDCase(AbstractDBTestCase):
#     _representation = 'no_pfs_with_allowed_processing_id'
#
#     def _get_data(self, session):
#         self.trust_payment = db_utils.create_trust_payment(session, 124, 10105)
#         return {
#             'trust_payment_id': self.trust_payment.trust_payment_id
#         }
#
#     def get_result(self):
#         res = RequiredResult(transition=IssueTransitions.opened,
#                              commit=True, delay=False)
#         res.add_message(COMMENTS['fraud_manager_success_comment']
#                         .format(**self.get_payment_data([])))
#         res.assignee = 'super_support'
#         res.summonees = ['super_support']
#         return res


class PFSWithIncorrectRuleCase(AbstractDBTestCase):
    _representation = 'pfs_with_incorrect_rule'

    def _get_data(self, session):
        afs_data = {
            'afs_status': 'success',
            'afs_resp_desc': 'In DIRECT_PAYMENT policy has error: 509841a2-4e9c-492c-ae6a-6c0d5b95c8f5:1'
        }
        self.trust_payment = db_utils.create_trust_payment(session, 124, 10001, afs_data)
        return {
            'trust_payment_id': self.trust_payment.trust_payment_id
        }

    def get_result(self):
        res = RequiredResult(transition=IssueTransitions.opened,
                             commit=True, delay=False)
        res.add_message(COMMENTS['info_comment'].format(**self.get_payment_data([])))
        res.add_message(COMMENTS['additional_support_comment']
                        .format(trust_payment_id=self.trust_payment.trust_payment_id))
        res.assignee = 'super_support'
        res.summonees = ['super_support']
        self.config
        return res


# class PFSWithIncorrectRuleAndAllowedServiceIDCase(AbstractDBTestCase):
#     _representation = 'pfs_with_incorrect_rule_and_allowed_service_id'
#
#     def _get_data(self, session):
#         afs_data = {
#             'afs_status': 'success',
#             'afs_resp_desc': 'In DIRECT_PAYMENT policy has error: 509841a2-4e9c-492c-ae6a-6c0d5b95c8f5:1'
#         }
#         self.trust_payment = db_utils.create_trust_payment(session, 125, 10001, afs_data)
#         return {
#             'trust_payment_id': self.trust_payment.trust_payment_id
#         }
#
#     def get_result(self):
#         res = RequiredResult(transition=IssueTransitions.opened,
#                              commit=True, delay=False)
#         res.add_message(COMMENTS['fraud_manager_success_comment']
#                         .format(**self.get_payment_data([])))
#         res.assignee = 'super_support'
#         res.summonees = ['super_support']
#         self.config
#         return res


class PFSWithWhiteRule(AbstractDBTestCase):
    _representation = 'pfs_with_white_rule'

    def _get_data(self, session):
        afs_data = {
            'afs_status': 'success',
            'afs_resp_desc': 'lalka1'
        }
        self.trust_payment = db_utils.create_trust_payment(session, 124, 10001, afs_data)
        return {
            'trust_payment_id': self.trust_payment.trust_payment_id
        }

    def get_result(self):
        res = RequiredResult(transition=IssueTransitions.opened,
                             commit=True, delay=False, tags=['lalka1'])
        res.add_message(COMMENTS['info_comment'].format(**self.get_payment_data(['lalka1'])))

        #  Тут саппорта зовем комментом для фрода
        res.add_message(COMMENTS['additional_fraud_comment'])
        res.assignee = 'super_support'
        res.summonees = ['super_support']
        self.config
        return res


class PFSWithMixedRules(AbstractDBTestCase):
    _representation = 'pfs_with_mixed_rules'

    def _get_data(self, session):
        afs_data = {
            'afs_status': 'success',
            'afs_resp_desc': 'lalka1,rule1'
        }
        self.trust_payment = db_utils.create_trust_payment(session, 124, 10001, afs_data)
        return {
            'trust_payment_id': self.trust_payment.trust_payment_id
        }

    def get_result(self):
        res = RequiredResult(transition=IssueTransitions.need_info,
                             commit=True, delay=False, tags=['lalka1', 'rule1'])
        res.add_message(COMMENTS['info_comment'].format(**self.get_payment_data(['lalka1', 'rule1'])))
        res.add_message(COMMENTS['additional_fraud_comment'])
        res.assignee = 'super_support'
        res.summonees = ['fraud_m1', 'fraud_m2']
        self.config
        return res


class PFSWithNoWhiteRule(AbstractDBTestCase):
    _representation = 'pfs_with_no_white_rule'

    def _get_data(self, session):
        afs_data = {
            'afs_status': 'success',
            'afs_resp_desc': 'rule1'
        }
        self.trust_payment = db_utils.create_trust_payment(session, 124, 10001, afs_data)
        return {
            'trust_payment_id': self.trust_payment.trust_payment_id
        }

    def get_result(self):
        res = RequiredResult(transition=IssueTransitions.need_info,
                             commit=True, delay=False, tags=['rule1'])
        res.add_message(COMMENTS['info_comment'].format(**self.get_payment_data(['rule1'])))
        res.add_message(COMMENTS['additional_fraud_comment'])
        res.assignee = 'super_support'
        res.summonees = ['fraud_m1', 'fraud_m2']
        self.config
        return res


@pytest.mark.parametrize('issue_data',
                         [case() for case in AbstractDBTestCase._cases],
                         ids=lambda case: str(case),
                         indirect=['issue_data'])
def test_db(session, issue_data):
    queue_object, st_issue, case = issue_data
    req_res = case.get_result()

    solver = BlacklistedUsers(queue_object, st_issue)
    res = solver.solve()
    session.flush()

    if req_res is None:
        assert res.commit is False
        assert res.delay is True
        assert res.issue_report is None
        return

    assert res.commit == req_res.commit
    assert res.delay == req_res.delay
    report = res.issue_report

    report_comments_set = set()
    for c in report.comments:
        c_text = c['text']
        for part in c_text.strip().split('\n'):
            report_comments_set.add(part.strip())

    req_res_comments_set = set()
    for c in req_res.comments:
        # print c
        for part in c.strip().split('\n'):
            req_res_comments_set.add(part.strip())
    # print req_res_comments_set - report_comments_set
    # print report_comments_set - req_res_comments_set
    assert req_res_comments_set == report_comments_set
    assert len(report.comments) == 2
    assert report.transition == req_res.transition
    assert report.assignee == req_res.assignee
    assert set(req_res.tags) == set(report.tags)
    assert report.type == 52  # Внешний запрос

    summonees = set(itertools.chain.from_iterable(cmt.summonees or [] for cmt in report.comments))
    assert set(req_res.summonees) == (summonees or None)


class AbstractParseTestCase(case_utils.AbstractMockTestCase):
    _default_lines = {
        'summary': 'Непрохождение платежа (blacklisted)',
        'trust_payment_id': 'Trust_payment_id: %s',
        'comment': 'Комментарий: вах вах'
    }

    def __init__(self):
        self.trust_payment = None


class AbstractFailedCheckFormTestCase(AbstractParseTestCase):
    _cases = []
    __metaclass__ = case_utils.TestCaseMeta


class AbstractSuccessCheckFormTestCase(AbstractParseTestCase):
    _cases = []
    __metaclass__ = case_utils.TestCaseMeta


class GoodPaymentTestCase(AbstractSuccessCheckFormTestCase):
    _representation = 'good_payment'

    def get_data(self, mock_manager):
        self.trust_payment = mock_utils.create_trust_payment(mock_manager)
        lines = [
            self._get_default_line(trust_payment_id=self.trust_payment.trust_payment_id),
            self._get_default_line('comment')
        ]
        return self._get_default_line('summary'), lines

    def get_result(self):
        return self.trust_payment


class PaymentNotFoundTestCase(AbstractFailedCheckFormTestCase):
    _representation = 'payment_not_found'

    def get_data(self, mock_manager):
        self.trust_payment = mock_utils.create_trust_payment(mock_manager, '1х1х1х1х1х1х1х1х1х1х1х1х')
        lines = [
            self._get_default_line(trust_payment_id='2х2х2х2х2х2х2х2х2х2х2х2х'),
            self._get_default_line('comment')
        ]
        return self._get_default_line('summary'), lines

    def get_result(self):
        return COMMENTS['payment_not_found'].format('2х2х2х2х2х2х2х2х2х2х2х2х')


class IncorrectTrustPaymentIdTestCase(AbstractFailedCheckFormTestCase):
    _representation = 'incorrect_trust_payment_id'

    def get_data(self, mock_manager):
        lines = [
            self._get_default_line(trust_payment_id='lalala'),
            self._get_default_line('comment')
        ]
        return self._get_default_line('summary'), lines

    def get_result(self):
        return COMMENTS['invalid_issue_data']


class NoTrustPaymentIdTestCase(AbstractFailedCheckFormTestCase):
    _representation = 'no_trust_payment_id'

    def get_data(self, mock_manager):
        lines = [
            self._get_default_line(trust_payment_id=''),
            self._get_default_line('comment')
        ]
        return self._get_default_line('summary'), lines

    def get_result(self):
        return COMMENTS['invalid_issue_data']


class InvalidFormTestCase(AbstractFailedCheckFormTestCase):
    _representation = 'invalid_form'

    def get_data(self, mock_manager):
        lines = [
            'Trust_paymentffefwwefw',
            self._get_default_line('comment')
        ]
        return self._get_default_line('summary'), lines

    def get_result(self):
        return COMMENTS['invalid_issue_data']


@pytest.mark.parametrize('mock_issue_data',
                         [case() for case in AbstractSuccessCheckFormTestCase._cases],
                         ids=lambda case: str(case),
                         indirect=['mock_issue_data'])
def test_parse_success(mock_queue_object, mock_issue_data):
    ri = IssueReport()
    issue, case = mock_issue_data
    required_res = case.get_result()
    solver = BlacklistedUsers(mock_queue_object, issue)
    res = solver.parse_issue(ri)

    assert required_res == res


@pytest.mark.parametrize('mock_issue_data',
                         [case() for case in AbstractFailedCheckFormTestCase._cases],
                         ids=lambda case: str(case),
                         indirect=['mock_issue_data'])
def test_parse_failing(mock_queue_object, mock_issue_data):
    issue, case = mock_issue_data
    ri = IssueReport()

    req_comment = case.get_result()

    solver = BlacklistedUsers(mock_queue_object, issue)
    with pytest.raises(ParseException):
        solver.parse_issue(ri)

    res_comment = ri.comment
    assert req_comment == res_comment
