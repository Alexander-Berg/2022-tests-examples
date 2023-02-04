# -*- coding: utf-8 -*-
from __future__ import unicode_literals

import pytest

from autodasha.core.api.tracker import IssueTransitions
from autodasha.solver_cl import BadDebt
from autodasha.utils.solver_utils import *

import balance.mapper as mapper

from tests.autodasha_tests.common import db_utils
from tests.autodasha_tests.functional_tests import case_utils, mock_utils


ACTS_FIELD = '№ акта'
INVOICES_FIELD = 'Номер счета'
COMMENT_FIELD = 'Комментарий'

COMMON_POSTFIXES = {
    'git_gud': 'Пожалуйста, уточни данные и заполни форму еще раз'
}

COMMENTS = {
    'already_solved':
        'Эта задача уже была выполнена. Направьте новый запрос через '
        '((https://wiki.yandex-team.ru/otdelsoprovozhdeniya/dokument/avtomatformi/ формы)). '
        'Если не найдёте подходящую, заполните, пожалуйста, общую форму.',
    'invalid_form':
        'Задача создана некорректно.\n'
        '{}'
        + COMMON_POSTFIXES['git_gud'] + '.',
    'invalid_field_value':
        'Поле \'{}\' заполнено некорректно.\n',
    'act_not_found':
        'Акт {} не найден.\n',
    'invoice_not_found':
        'Счет {} не найден.\n',
    'unmatched_act':
        'Акт {} выставлен на счет {}, отличный от указанных.\n',
    'act_included':
        'Акт {} включен в расчет лимита кредита.\n',
    'act_already_included':
        'Акт {} уже включен в расчет лимита кредита.\n',
    'fictive_invoice':
        'Счет {} является фиктивным - необходимо указать счет на погашение.\n',
    'non_credit_invoice':
        'Счет {} не является кредитным.\n',
    'git_gud':
        COMMON_POSTFIXES['git_gud'] + ', если необходимо.'
}


class AbstractMockTestCase(case_utils.AbstractMockTestCase):
    _default_lines = {
        'summary': 'Включить акты в расчет лимита кредита',
        'acts': ACTS_FIELD + ': %s',
        'invoices': INVOICES_FIELD + ': %s',
        'comment': COMMENT_FIELD + ': -'
    }


class AbstractParseFailTestCase(AbstractMockTestCase):
    _cases = []
    __metaclass__ = case_utils.TestCaseMeta


class AbstractParseSuccessTestCase(AbstractMockTestCase):
    _cases = []
    __metaclass__ = case_utils.TestCaseMeta


class InvalidActCase(AbstractParseFailTestCase):
    _representation = 'invalid_act'

    def get_data(self, mock_manager):
        lines = [
            self._get_default_line(acts='№123456789'),
            self._get_default_line('comment')
        ]
        return self._get_default_line('summary'), lines

    def get_result(self):
        return COMMENTS['invalid_form'].format(COMMENTS['invalid_field_value'].format(ACTS_FIELD))


class ValidActInvalidInvoiceCase(AbstractParseFailTestCase):
    _representation = 'valid_act_invalid_invoice'

    def get_data(self, mock_manager):
        lines = [
            self._get_default_line(acts='123456789'),
            self._get_default_line(invoices='123456789'),
            self._get_default_line('comment')
        ]
        return self._get_default_line('summary'), lines

    def get_result(self):
        return COMMENTS['invalid_form'].format(COMMENTS['invalid_field_value'].format(INVOICES_FIELD))


class ValidActsValidInvoicesCase(AbstractParseSuccessTestCase):
    _representation = 'valid_acts_valid_invoices'

    def get_data(self, mock_manager):
        self.acts = ['12345678', '23456789', 'YB-34567890', '45678901', 'YB-56789012-13']
        self.invoices = ['Б-12345678-1', 'ЛС-12345678-10', 'SWR-12345678-100', 'ЛСБ-12345678-1000']
        lines = [
            self._get_default_line(acts='{} {}, {}; {} {}'.format(*self.acts)),
            self._get_default_line(invoices='{} {}, {}; {}'.format(*self.invoices)),
            self._get_default_line('comment')
        ]
        return self._get_default_line('summary'), lines

    def get_result(self):
        return {
            'act_eids': self.acts,
            'invoice_eids': self.invoices
        }


@pytest.mark.parametrize('mock_issue_data',
                         [case() for case in (ValidActsValidInvoicesCase,)],
                         ids=lambda case: str(case),
                         indirect=['mock_issue_data'])
def test_parse_success_cases(mock_queue_object, mock_issue_data):
    issue, case = mock_issue_data
    required_res = case.get_result()
    solver = BadDebt(mock_queue_object, issue)
    res = solver.parse_issue()
    assert required_res == res


class ActNotFoundCase(AbstractParseFailTestCase):
    _representation = 'act_not_found'

    def get_data(self, mock_manager):
        self.invoice = mock_utils.create_invoice(mock_manager)
        self.act = mock_utils.create_act(mock_manager, self.invoice, external_id='12345678')
        lines = [
            self._get_default_line(acts='123456789'),
            self._get_default_line(invoices=self.invoice.external_id),
            self._get_default_line('comment')
        ]
        return self._get_default_line('summary'), lines

    def get_result(self):
        return COMMENTS['invalid_form'].format(COMMENTS['act_not_found'].format('123456789'))


class ActNotFoundMixedCase(AbstractParseFailTestCase):
    _representation = 'act_not_found_mixed'

    def get_data(self, mock_manager):
        self.invoice = mock_utils.create_invoice(mock_manager)
        self.act = mock_utils.create_act(mock_manager, self.invoice, external_id='YB-12345678')
        lines = [
            self._get_default_line(acts='YB-12345678, 12345678'),
            self._get_default_line(invoices=self.invoice.external_id),
            self._get_default_line('comment')
        ]
        return self._get_default_line('summary'), lines

    def get_result(self):
        return COMMENTS['invalid_form'].format(COMMENTS['act_not_found'].format('12345678'))


class ActFoundInvoiceNotFoundCase(AbstractParseFailTestCase):
    _representation = 'act_found_invoice_not_found'

    def get_data(self, mock_manager):
        self.invoice = mock_utils.create_invoice(mock_manager)
        self.act = mock_utils.create_act(mock_manager, self.invoice)
        lines = [
            self._get_default_line(acts=self.act.external_id),
            self._get_default_line(invoices='Б-12345678-1'),
            self._get_default_line('comment')
        ]
        return self._get_default_line('summary'), lines

    def get_result(self):
        return COMMENTS['invalid_form'].format(COMMENTS['invoice_not_found'].format('Б-12345678-1'))


class FoundActsFoundInvoicesCase(AbstractParseSuccessTestCase):
    _representation = 'found_acts_found_invoices'

    def get_data(self, mock_manager):
        self.acts = ['12345678', '23456789', 'YB-34567890', 'YB-45678901-1']
        self.invoices = ['Б-12345678-1', 'ЛС-12345678-10', 'SWR-12345678-100', 'ЛСБ-12345678-1000']
        self.invoices = [mock_utils.create_invoice(mock_manager, external_id=eid) for eid in self.invoices]
        self.acts = [mock_utils.create_act(mock_manager, i, external_id=eid)
                     for i, eid in zip(self.invoices, self.acts)]
        lines = [
            self._get_default_line(acts='{} {}, {}; {}'.format(*(a.external_id for a in self.acts))),
            self._get_default_line(invoices='{} {}, {}; {}'.format(*(i.external_id for i in self.invoices))),
            self._get_default_line('comment')
        ]
        return self._get_default_line('summary'), lines

    def get_result(self):
        return {
            'acts': self.acts,
            'invoices': self.invoices
        }


@pytest.mark.parametrize('mock_issue_data',
                         [case() for case in (FoundActsFoundInvoicesCase,)],
                         ids=lambda case: str(case),
                         indirect=['mock_issue_data'])
def test_get_success_cases(mock_queue_object, mock_issue_data):
    issue, case = mock_issue_data
    required_res = case.get_result()
    solver = BadDebt(mock_queue_object, issue)
    res = solver.get_objects(**solver.parse_issue())
    assert required_res == res


class FictiveCase(AbstractParseFailTestCase):
    _representation = 'fictive_invoice'

    def get_data(self, mock_manager):
        self.acts = ['12345678']
        self.invoices = ['Б-12345678-1']
        self.types = ['fictive']
        self.invoices = [mock_utils.create_invoice(mock_manager, external_id=eid, type=type)
                         for eid, type in zip(self.invoices, self.types)]
        self.acts = [mock_utils.create_act(mock_manager, i, external_id=eid)
                     for i, eid in zip(self.invoices, self.acts)]
        lines = [
            self._get_default_line(acts='{}'.format(*(a.external_id for a in self.acts))),
            self._get_default_line(invoices='{}'.format(*(i.external_id for i in self.invoices))),
            self._get_default_line('comment')
        ]
        return self._get_default_line('summary'), lines

    def get_result(self):
        return COMMENTS['invalid_form'].format(COMMENTS['fictive_invoice'].format('Б-12345678-1'))


class FictiveMixedCase(AbstractParseFailTestCase):
    _representation = 'fictive_invoice_mixed'

    def get_data(self, mock_manager):
        self.acts = ['12345678', '23456789']
        self.invoices = ['Б-12345678-1', 'ЛС-12345678-10']
        self.types = ['fictive', 'personal_account']
        self.invoices = [mock_utils.create_invoice(mock_manager, external_id=eid, type=type)
                         for eid, type in zip(self.invoices, self.types)]
        self.acts = [mock_utils.create_act(mock_manager, i, external_id=eid)
                     for i, eid in zip(self.invoices, self.acts)]
        lines = [
            self._get_default_line(acts=','.join(a.external_id for a in self.acts)),
            self._get_default_line(invoices=','.join(i.external_id for i in self.invoices)),
            self._get_default_line('comment')
        ]
        return self._get_default_line('summary'), lines

    def get_result(self):
        return COMMENTS['invalid_form'].format(COMMENTS['fictive_invoice'].format('Б-12345678-1'))


class NonCreditCase(AbstractParseFailTestCase):
    _representation = 'non_credit_invoice'

    def get_data(self, mock_manager):
        self.acts = ['12345678', '23456789', '34567890', '45678901']
        self.invoices = ['Б-12345678-1', 'ЛС-12345678-10', 'SWR-12345678-100', 'ЛСБ-12345678-1000']
        self.types = ['charge_note', 'prepayment', 'overdraft', 'bonus_account']
        self.invoices = [mock_utils.create_invoice(mock_manager, external_id=eid, type=type)
                         for eid, type in zip(self.invoices, self.types)]
        self.acts = [mock_utils.create_act(mock_manager, i, external_id=eid)
                     for i, eid in zip(self.invoices, self.acts)]
        lines = [
            self._get_default_line(acts=','.join(a.external_id for a in self.acts)),
            self._get_default_line(invoices=','.join(i.external_id for i in self.invoices)),
            self._get_default_line('comment')
        ]
        return self._get_default_line('summary'), lines

    def get_result(self):
        return COMMENTS['invalid_form'].format(
            ''.join(COMMENTS['non_credit_invoice'].format(i.external_id) for i in self.invoices))


class NonCreditMixedCase(AbstractParseFailTestCase):
    _representation = 'non_credit_invoice_mixed'

    def get_data(self, mock_manager):
        self.acts = ['12345678', '23456789', '34567890', '45678901', '56789012']
        self.invoices = ['Б-12345678-1', 'ЛС-12345678-10', 'SWR-12345678-100', 'ЛСБ-12345678-1000', 'ЛСД-1234-1']
        self.types = ['charge_note', 'prepayment', 'overdraft', 'bonus_account', 'personal_account']
        self.invoices = [mock_utils.create_invoice(mock_manager, external_id=eid, type=type)
                         for eid, type in zip(self.invoices, self.types)]
        self.acts = [mock_utils.create_act(mock_manager, i, external_id=eid)
                     for i, eid in zip(self.invoices, self.acts)]
        lines = [
            self._get_default_line(acts=','.join(a.external_id for a in self.acts)),
            self._get_default_line(invoices=','.join(i.external_id for i in self.invoices)),
            self._get_default_line('comment')
        ]
        return self._get_default_line('summary'), lines

    def get_result(self):
        return COMMENTS['invalid_form'].format(
            ''.join(COMMENTS['non_credit_invoice'].format(i.external_id) for i in self.invoices[:-1]))


class BadInvoiceMixedCase(AbstractParseFailTestCase):
    _representation = 'non_credit_and_fictive_invoice_mixed'

    def get_data(self, mock_manager):
        self.acts = ['12345678', '23456789', '34567890', '45678901', '56789012']
        self.invoices = ['Б-12345678-1', 'ЛС-12345678-10', 'SWR-12345678-100', 'ЛСБ-12345678-1000', 'ЛСД-1234-1']
        self.types = ['charge_note', 'prepayment', 'overdraft', 'fictive', 'personal_account']
        self.invoices = [mock_utils.create_invoice(mock_manager, external_id=eid, type=type)
                         for eid, type in zip(self.invoices, self.types)]
        self.acts = [mock_utils.create_act(mock_manager, i, external_id=eid)
                     for i, eid in zip(self.invoices, self.acts)]
        lines = [
            self._get_default_line(acts=','.join(a.external_id for a in self.acts)),
            self._get_default_line(invoices=','.join(i.external_id for i in self.invoices)),
            self._get_default_line('comment')
        ]
        return self._get_default_line('summary'), lines

    def get_result(self):
        return COMMENTS['invalid_form'].format(
            ''.join(COMMENTS['non_credit_invoice'].format(i.external_id) for i in self.invoices[:-2])
            + ''.join(COMMENTS['fictive_invoice'].format(i.external_id) for i in self.invoices[-2:-1]))


@pytest.mark.parametrize('mock_issue_data',
                         [case() for case in AbstractParseFailTestCase._cases],
                         ids=lambda case: str(case),
                         indirect=['mock_issue_data'])
def test_parse_fail_cases(mock_queue_object, mock_issue_data):
    issue, case = mock_issue_data
    required_res = case.get_result()
    solver = BadDebt(mock_queue_object, issue)
    with pytest.raises(Exception) as e:
        solver.execute(**solver.check_objects(**solver.get_objects(**solver.parse_issue())))
    assert required_res == e.value.message


class RequiredResult(case_utils.RequiredResult):

    def __init__(self, num_comments=1, **kwargs):
        self.acts_states = []
        self.num_comments = num_comments

        super(RequiredResult, self).__init__(**kwargs)

    def add_good_acts(self, *args):
        self.acts_states.extend((a, 0) for a in args)

    def add_bad_acts(self, *args):
        self.acts_states.extend((a, 1) for a in args)


class AbstractDBTestCase(case_utils.AbstractDBTestCase):
    _cases = []
    __metaclass__ = case_utils.TestCaseMeta

    _summary = 'Включить акты в расчет лимита кредита'
    _description = ("""
%s: {acts}
%s: {invoices}
%s: -
""" % (ACTS_FIELD, INVOICES_FIELD, COMMENT_FIELD)).strip()

    def __init__(self):
        super(AbstractDBTestCase, self).__init__()

    def get_description(self, session):
        acts, invoices = self._get_data(session)
        return self._description.format(
            acts='\n'.join(a.external_id for a in acts),
            invoices='\n'.join(i.external_id for i in invoices)
        )


class AlreadyIncludedCase(AbstractDBTestCase):
    _representation = 'already_included'

    def _get_data(self, session):
        client, person = db_utils.create_client_person(session)
        order = db_utils.create_order(session, client)

        month = mapper.ActMonth()

        invoice = db_utils.create_personal_account(session, client, person)
        invoice.transfer(order, 2, 30, skip_check=True)
        order.calculate_consumption(month.document_dt, {order.shipment_type: 30})
        invoice.generate_act(force=True, backdate=month.document_dt)

        self.act, = {at.act for q in order.consumes for at in q.acttranses}
        self.act.good_debt = 0

        return [self.act], [self.act.invoice]

    def get_result(self):
        res = RequiredResult(transition=IssueTransitions.wont_fix)
        res.add_good_acts(self.act)
        res.add_message(COMMENTS['act_already_included'].format(self.act.external_id) + COMMENTS['git_gud'])
        return res


class AlreadyIncludedYBCase(AbstractDBTestCase):
    _representation = 'already_included_yb'

    def _get_data(self, session):
        client, person = db_utils.create_client_person(session)
        order = db_utils.create_order(session, client)

        month = mapper.ActMonth()

        invoice = db_utils.create_personal_account(session, client, person)
        invoice.transfer(order, 2, 30, skip_check=True)
        order.calculate_consumption(month.document_dt, {order.shipment_type: 30})
        invoice.generate_act(force=True, backdate=month.document_dt)

        self.act, = {at.act for q in order.consumes for at in q.acttranses}
        self.act.external_id = 'YB-' + self.act.external_id
        self.act.good_debt = 0

        return [self.act], [self.act.invoice]

    def get_result(self):
        res = RequiredResult(transition=IssueTransitions.wont_fix)
        res.add_good_acts(self.act)
        res.add_message(COMMENTS['act_already_included'].format(self.act.external_id) + COMMENTS['git_gud'])
        return res


class UnmatchedActCase(AbstractDBTestCase):
    _representation = 'unmatched_act'

    def _get_data(self, session):
        client, person = db_utils.create_client_person(session)
        order = db_utils.create_order(session, client)

        month = mapper.ActMonth()

        invoice = db_utils.create_personal_account(session, client, person)
        invoice.transfer(order, 2, 30, skip_check=True)

        order.calculate_consumption(month.document_dt, {order.shipment_type: 30})
        invoice.generate_act(force=True, backdate=month.document_dt)

        invoice = db_utils.create_personal_account(session, client, person)

        self.act, = {at.act for q in order.consumes for at in q.acttranses}
        self.act.good_debt = 1

        return [self.act], [invoice]

    def get_result(self):
        res = RequiredResult(transition=IssueTransitions.wont_fix)
        res.add_bad_acts(self.act)
        res.add_message(COMMENTS['unmatched_act'].format(self.act.external_id, self.act.invoice.external_id)
                        + COMMENTS['git_gud'])
        return res


class IncludedAndUnmatchedActMixedCase(AbstractDBTestCase):
    _representation = 'already_included_unmatched_act_mixed'

    def _get_data(self, session):
        client, person = db_utils.create_client_person(session)
        order = db_utils.create_order(session, client)

        month = mapper.ActMonth()

        invoice = db_utils.create_personal_account(session, client, person)
        invoice.transfer(order, 2, 30, skip_check=True)

        order.calculate_consumption(month.document_dt, {order.shipment_type: 10})
        invoice.generate_act(force=True, backdate=month.document_dt)

        order.calculate_consumption(month.document_dt, {order.shipment_type: 20})
        invoice.generate_act(force=True, backdate=month.document_dt)

        self.act_included, self.act_unmatched = list({at.act for q in order.consumes for at in q.acttranses})
        self.act_included.good_debt = 0
        self.act_unmatched.good_debt = 1

        return [self.act_included, self.act_unmatched], [self.act_included.invoice]

    def get_result(self):
        res = RequiredResult(transition=IssueTransitions.wont_fix)
        res.add_good_acts(self.act_included)
        res.add_bad_acts(self.act_unmatched)
        res.add_message(
            COMMENTS['act_already_included'].format(self.act_included.external_id) + COMMENTS['unmatched_act'].format(
                self.act_unmatched.external_id, self.act_unmatched.invoice.external_id)
            + COMMENTS['git_gud'])
        return res


class BadGoodActsMixedCase(AbstractDBTestCase):
    _representation = 'bad_good_acts_mixed'

    def _get_data(self, session):
        client, person = db_utils.create_client_person(session)
        order = db_utils.create_order(session, client)

        month = mapper.ActMonth()

        invoice = db_utils.create_personal_account(session, client, person)
        invoice.transfer(order, 2, 30, skip_check=True)

        order.calculate_consumption(month.document_dt, {order.shipment_type: 10})
        invoice.generate_act(force=True, backdate=month.document_dt)

        order.calculate_consumption(month.document_dt, {order.shipment_type: 20})
        invoice.generate_act(force=True, backdate=month.document_dt)

        order.calculate_consumption(month.document_dt, {order.shipment_type: 30})
        invoice.generate_act(force=True, backdate=month.document_dt)

        self.act_included, self.act_unmatched, self.act_good = list({at.act for q in order.consumes
                                                                     for at in q.acttranses})
        self.act_included.good_debt = 0
        self.act_unmatched.good_debt = 1
        self.act_good.good_debt = 1

        return [self.act_included, self.act_unmatched, self.act_good],\
               [self.act_included.invoice, self.act_good.invoice]

    def get_result(self):
        res = RequiredResult(transition=IssueTransitions.fixed, num_comments=2)
        res.add_bad_acts(self.act_unmatched)
        res.add_good_acts(self.act_included)
        res.add_good_acts(self.act_good)
        res.add_message(
            COMMENTS['act_already_included'].format(self.act_included.external_id) + COMMENTS['unmatched_act'].format(
                self.act_unmatched.external_id, self.act_unmatched.invoice.external_id)
            + COMMENTS['git_gud'])
        res.add_message(COMMENTS['act_included'].format(self.act_good.external_id))
        return res


class GoodActCase(AbstractDBTestCase):
    _representation = 'good_act'

    def _get_data(self, session):
        client, person = db_utils.create_client_person(session)
        order = db_utils.create_order(session, client)

        month = mapper.ActMonth()

        invoice = db_utils.create_personal_account(session, client, person)
        invoice.transfer(order, 2, 30, skip_check=True)

        order.calculate_consumption(month.document_dt, {order.shipment_type: 10})
        invoice.generate_act(force=True, backdate=month.document_dt)

        order.calculate_consumption(month.document_dt, {order.shipment_type: 20})
        invoice.generate_act(force=True, backdate=month.document_dt)

        order.calculate_consumption(month.document_dt, {order.shipment_type: 30})
        invoice.generate_act(force=True, backdate=month.document_dt)

        self.act_included, self.act_unmatched, self.act_good = list({at.act for q in order.consumes
                                                                     for at in q.acttranses})
        self.act_included.good_debt = 0
        self.act_unmatched.good_debt = 1
        self.act_good.good_debt = 1
        self.act_good.external_id = 'YB-' + self.act_good.external_id

        return [self.act_good], [self.act_good.invoice]

    def get_result(self):
        res = RequiredResult(transition=IssueTransitions.fixed, num_comments=1)
        res.add_good_acts(self.act_good)
        res.add_message(COMMENTS['act_included'].format(self.act_good.external_id))
        return res


@pytest.mark.parametrize('issue_data',
                         [case() for case in AbstractDBTestCase._cases],
                         ids=lambda case: str(case),
                         indirect=['issue_data'])
def test_db(session, issue_data):
    queue_object, st_issue, case = issue_data

    req_res = case.get_result()

    solver = BadDebt(queue_object, st_issue)
    res = solver.solve()
    session.flush()

    assert res and req_res

    report = res.issue_report

    assert len(report.comments) == req_res.num_comments
    assert report.transition == req_res.transition
    assert report.assignee == req_res.assignee
    assert all(act.good_debt == state for act, state in req_res.acts_states)
    assert [cmt.text for cmt in report.comments] == req_res.comments
