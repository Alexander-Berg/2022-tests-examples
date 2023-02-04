# -*- coding: utf-8 -*-

from __future__ import unicode_literals

import pytest
import mock
import functools
import datetime as dt
from decimal import Decimal as D

from autodasha.solver_cl import (
    ReportSolver,
    ParseException,
    Contract3070InvoiceTurnOnSolver,
)
from autodasha.core.api.tracker import IssueReport
from autodasha.core.api.tracker import IssueTransitions

from tests.autodasha_tests.functional_tests import case_utils
from tests.autodasha_tests.functional_tests import mock_utils
from tests.autodasha_tests.common import db_utils
from tests.autodasha_tests.common import staff_utils


def _fake_init(self, *args, **kwargs):
    super(ReportSolver, self).__init__(*args, **kwargs)
    self.ro_session = self.session


COMMENTS = {
    'invoices_required':
        'Необходимо ввести номера счетов.'
        '\nПроверь, пожалуйста, данные и заполни форму ещё раз',
    'not_found_in_db':
        'Счет %s не найден в базе данных, действий не выполнено.',
    'no_invoices_were_found_in_db':
        'Не удалось найти счета в базе данных.'
        '\nПроверь, пожалуйста, данные и заполни форму ещё раз',
    'wrong_department':
        'Данный тип тикетов могут создавать только сотрудники ((https://wiki.yandex-team.ru/sales/processing/gruppaplatezhi/ payment-invoice)).'
        'Обратись, пожалуйста, к ним - %%payment-invoice@yandex-team.ru, доб 2345%%.',
    'does_not_have_receipt':
        'По счету %s нет оплат.'
        '\nПроверь, пожалуйста, данные и заполни форму ещё раз',
    'not_enough_receipt_sum':
        'По счету %s недостаточно оплат. Сейчас %s, требуется %s.'
        '\nПроверь, пожалуйста, данные и заполни форму ещё раз',
    'no_valid_invoices':
        'По счетам не выполнено действий.',
    'unknown_error':
        'При зачислении по счету %s возникла ошибка %s.',
    'execution_failed':
        'По счетам не удалось зачислить средства.',
    'transfer_success':
        'По счету %s выполнено зачисление.',
    'done':
        'Готово.',
}


class AbstractMockTestCase(case_utils.AbstractMockTestCase):

    author = 'ashul'

    _default_lines = {
        'summary': u'Зачислить средства на заказ по счету, договор 30/70',
        'invoices': u'№ счета: %s',
        'comment': u'Комментарий: ну, чтобы да',
    }


class AbstractGoodMockTestCase(AbstractMockTestCase):
    _cases = []
    invoices = []
    __metaclass__ = case_utils.TestCaseMeta

    def get_result(self):
        return self.invoices, None


class SeveralInvoicesParsing(AbstractGoodMockTestCase):
    _representation = 'several_invoices_filled'

    def get_data(self, mock_manager):
        self.invoices = []
        return self._get_default_line('summary'), []


class OneInvoiceParsing(AbstractGoodMockTestCase):
    _representation = 'one_invoice'

    def get_data(self, mock_manager):
        self.invoices = []
        invoices_eid = ['3222311']
        for external_id in invoices_eid:
            invoice = mock_utils.create_invoice(
                mock_manager, external_id='Б-' + external_id + '-1'
            )
            self.invoices.append(invoice)
        lines = [
            self._get_default_line(
                invoices=', '.join(i.external_id for i in self.invoices)
            ),
            self._get_default_line('comment'),
        ]
        return self._get_default_line('summary'), lines


class InvoicesNotAllInDbParsing(AbstractGoodMockTestCase):
    _representation = 'invoices_filled_not_all_in_db'

    def get_data(self, mock_manager):
        self.invoices = []
        invoices_eid = ['3222311', '1123']
        for external_id in invoices_eid:
            invoice = mock_utils.create_invoice(
                mock_manager, external_id='Б-' + external_id + '-1'
            )
            self.invoices.append(invoice)
        lines = [
            self._get_default_line(
                invoices=', '.join(i.external_id for i in self.invoices)
                + ', Б-3-1, Б-23-2'
            ),
            self._get_default_line('comment'),
        ]
        return self._get_default_line('summary'), lines

    def get_result(self):
        comments = ''
        for external_id in ['Б-3-1', 'Б-23-2']:
            comments += COMMENTS.get('not_found_in_db') % external_id + '\n'
        comments = comments.strip()
        return self.invoices, comments


@mock.patch('autodasha.solver_cl.base_solver.ReportSolver.__init__', _fake_init)
@pytest.mark.parametrize(
    'mock_issue_data',
    [SeveralInvoicesParsing()],
    ids=lambda args: ','.join(args),
    indirect=['mock_issue_data'],
)
@pytest.mark.parametrize('delimiter', [', ', ' ', '; ', ': '])
def test_several_delimiters_parse_good(mock_queue_object, mock_issue_data, mock_manager, delimiter):
    ri = IssueReport()
    issue, case = mock_issue_data
    invoices = []
    invoices_eid = ['32223', '44341']
    for external_id in invoices_eid:
        invoice = mock_utils.create_invoice(
            mock_manager, external_id='Б-' + external_id + '-1'
        )
        invoices.append(invoice)

    lines = [
        case._get_default_line(
            invoices=delimiter.join(i.external_id for i in invoices)
        ),
        case._get_default_line('comment'),
    ]
    issue.description = '\n'.join(lines)
    case.invoices = invoices
    req_res, req_comment = case.get_result()

    solver = Contract3070InvoiceTurnOnSolver(mock_queue_object, issue)
    res = solver.parse_issue(ri)

    assert sorted(req_res) == sorted(res)

    if req_comment:
        comments = [text.get('text') for text in ri.comments]
        comments = ' '.join(comments)
        assert sorted(req_comment) == sorted(comments)


@mock.patch('autodasha.solver_cl.base_solver.ReportSolver.__init__', _fake_init)
@pytest.mark.parametrize(
    'mock_issue_data',
    [case() for case in AbstractGoodMockTestCase._cases if case != SeveralInvoicesParsing],
    ids=lambda case: str(case),
    indirect=['mock_issue_data'],
)
def test_parse_good(mock_queue_object, mock_issue_data):
    ri = IssueReport()
    issue, case = mock_issue_data

    req_res, req_comment = case.get_result()

    solver = Contract3070InvoiceTurnOnSolver(mock_queue_object, issue)
    res = solver.parse_issue(ri)

    assert sorted(req_res) == sorted(res)

    if req_comment:
        comments = [text.get('text') for text in ri.comments]
        comments = ' '.join(comments)
        assert sorted(req_comment) == sorted(comments)


class AbstractBadMockTestCase(AbstractMockTestCase):
    _cases = []
    __metaclass__ = case_utils.TestCaseMeta


class InvoicesWereNotEnteredParsing(AbstractBadMockTestCase):
    _representation = 'invoices_were_not_entered'

    def get_data(self, mock_manager):
        lines = [
            self._get_default_line(invoices=''),
            self._get_default_line('comment'),
        ]
        return self._get_default_line('summary'), lines

    def get_result(self):
        return COMMENTS.get('invoices_required')

class InvoicesNoFoundInDbParsing(AbstractBadMockTestCase):
    _representation = 'invoices_not_found_in_db'

    def get_data(self, mock_manager):
        self.invoices = []
        lines = [
            self._get_default_line(invoices='Б-35-1, Б-233-2'),
            self._get_default_line('comment'),
        ]
        return self._get_default_line('summary'), lines

    def get_result(self):
        comments = ''
        for external_id in ['Б-35-1', 'Б-233-2']:
            comments += COMMENTS.get('not_found_in_db') % external_id + '\n'
        comments += COMMENTS.get('no_invoices_were_found_in_db')
        return comments


class WrongAuthorParsing(AbstractBadMockTestCase):
    _representation = 'wrong_author'

    def get_data(self, mock_manager):
        self.author = 'vova'
        lines = [
            self._get_default_line(
                invoices=''
            ),
            self._get_default_line('comment'),
        ]
        return self._get_default_line('summary'), lines

    def get_result(self):
        return ''


@mock.patch('autodasha.solver_cl.base_solver.ReportSolver.__init__', _fake_init)
@pytest.mark.parametrize(
    'mock_issue_data',
    [case() for case in AbstractBadMockTestCase._cases],
    ids=lambda case: str(case),
    indirect=['mock_issue_data'],
)
def test_parse_bad(mock_queue_object, mock_issue_data):
    ri = IssueReport()
    issue, case = mock_issue_data

    req_comment = case.get_result()
    solver = Contract3070InvoiceTurnOnSolver(mock_queue_object, issue)

    with pytest.raises(ParseException) as exc:
        solver.parse_issue(ri)

    if ri.comments:
        comments = [text.get('text') for text in ri.comments]
        comments = ' '.join(comments)
        if comments:
            comments += '\n' + exc.value.message
        assert req_comment == comments
    else:
        assert req_comment in exc.value.message


class RequiredResult(case_utils.RequiredResult):
    def __init__(self, **kwargs):
        self.commit = kwargs.get('commit')
        self.delay = kwargs.get('delay')
        self.summonees = kwargs.get('summonees', [])
        self.assignee = 'autodasha'
        self.invoices = []
        super(RequiredResult, self).__init__(**kwargs)

    @staticmethod
    def get_comment(key, *args, **kwargs):
        return COMMENTS.get(key).format(*args, **kwargs)


class AbstractDBTestCase(case_utils.AbstractDBTestCase):
    author = 'ashul'
    summary = 'Зачислить средства на заказ по счету, договор 30/70'

    _description = '''
№ счета: {invoices}
Комментарий: ну, чтобы да
--
Создано через формы
    '''.strip()
    issue_key = 'test_contract_30_70_turn_on'

    _cases = []
    __metaclass__ = case_utils.TestCaseMeta

    def __init__(self):
        super(AbstractDBTestCase, self).__init__()
        self.invoices = []

    def prepare_data(self, session):
        raise NotImplementedError()

    def _get_data(self, session):
        return self.prepare_data(session)


class GoodDBCase(AbstractDBTestCase):
    _representation = 'good_case'

    def prepare_data(self, session):
        client = db_utils.create_client(session)
        order = db_utils.create_order(session, client, service_id=7)
        invoice = db_utils.create_invoice(
            session, client, [(order, 100)], paysys_id=1003
        )
        invoice.create_receipt(invoice.effective_sum * D('0.3'))
        self.invoices.append(invoice)

        return {'invoices': self.invoices[0].external_id}

    def get_result(self):
        res = RequiredResult(commit=True, delay=False)
        res.transition = IssueTransitions.fixed
        res.add_message(COMMENTS['transfer_success'] % self.invoices[0].external_id)
        res.add_message(COMMENTS['done'])
        res.invoices = self.invoices
        return res


class GoodCaseWithConsumes(AbstractDBTestCase):
    _representation = 'good_case_with_consumes'

    def prepare_data(self, session):
        client = db_utils.create_client(session)
        order = db_utils.create_order(session, client, service_id=7)
        invoice = db_utils.create_invoice(
            session, client, [(order, 100)], paysys_id=1003
        )
        invoice.create_receipt(invoice.effective_sum * D('0.3'))
        db_utils.consume_order(invoice, order, 30)
        db_utils.consume_order(invoice, order, 10)
        self.invoices.append(invoice)

        return {'invoices': self.invoices[0].external_id}

    def get_result(self):
        res = RequiredResult(commit=True, delay=False)
        res.transition = IssueTransitions.fixed
        res.add_message(COMMENTS['transfer_success'] % self.invoices[0].external_id)
        res.add_message(COMMENTS['done'])
        res.invoices = self.invoices
        return res


class GoodCaseSeveralOrdersWithConsumes(AbstractDBTestCase):
    _representation = 'good_case_several_orders_with_consumes'

    def prepare_data(self, session):
        client = db_utils.create_client(session)
        order = db_utils.create_order(session, client, service_id=7)
        order2 = db_utils.create_order(session, client, service_id=7)
        invoice = db_utils.create_invoice(
            session, client, [(order, 100), (order2, 75)], paysys_id=1003
        )
        invoice.create_receipt(invoice.effective_sum * D('0.3'))
        db_utils.consume_order(invoice, order, 30)
        db_utils.consume_order(invoice, order2, 10)
        self.invoices.append(invoice)

        return {'invoices': self.invoices[0].external_id}

    def get_result(self):
        res = RequiredResult(commit=True, delay=False)
        res.transition = IssueTransitions.fixed
        res.add_message(COMMENTS['transfer_success'] % self.invoices[0].external_id)
        res.add_message(COMMENTS['done'])
        res.invoices = self.invoices
        return res


class GoodCaseMainOrder(AbstractDBTestCase):
    _representation = 'good_case_main_order'

    def prepare_data(self, session):
        client = db_utils.create_client(session)
        main_order = db_utils.create_order(session, client, service_id=7, main_order=1)
        invoice_main = db_utils.create_invoice(session, client, [(main_order, 50)])
        invoice_main.create_receipt(invoice_main.effective_sum * D('0.3'))
        db_utils.consume_order(invoice_main, main_order, 30)
        order = db_utils.create_order(session, client, service_id=7, group_order_id=main_order.id)
        db_utils.create_invoice(session, client, [(order, 50)], turn_on=True)
        self.invoices.append(invoice_main)

        return {
            'invoices': self.invoices[0].external_id
        }

    def get_result(self):
        res = RequiredResult(commit=True, delay=False)
        res.transition = IssueTransitions.fixed
        res.add_message(COMMENTS['transfer_success'] % self.invoices[0].external_id)
        res.add_message(COMMENTS['done'])
        res.invoices = self.invoices
        return res


class InvoiceWoReceipt(AbstractDBTestCase):
    _representation = 'invoice_wo_receipt'

    def prepare_data(self, session):
        client = db_utils.create_client(session)
        order = db_utils.create_order(session, client, service_id=7)
        invoice = db_utils.create_invoice(
            session, client, [(order, 100)], paysys_id=1003
        )
        self.invoices.append(invoice)

        return {'invoices': self.invoices[0].external_id}

    def get_result(self):
        res = RequiredResult(commit=True, delay=False)
        res.transition = IssueTransitions.wont_fix
        res.add_message(
            COMMENTS['does_not_have_receipt'] % self.invoices[0].external_id
        )
        res.add_message(COMMENTS['no_valid_invoices'])
        return res


class InvoiceNotEnoughReceipt(AbstractDBTestCase):
    _representation = 'invoice_not_enough_receipt'

    def prepare_data(self, session):
        client = db_utils.create_client(session)
        order = db_utils.create_order(session, client, service_id=7)
        invoice = db_utils.create_invoice(
            session, client, [(order, 100)], paysys_id=1003
        )
        invoice.create_receipt(invoice.effective_sum * D('0.2'))
        self.invoices.append(invoice)

        return {'invoices': self.invoices[0].external_id}

    def get_result(self):
        res = RequiredResult(commit=True, delay=False)
        res.transition = IssueTransitions.wont_fix
        res.add_message(
            COMMENTS['not_enough_receipt_sum']
            % (
                self.invoices[0].external_id,
                self.invoices[0].receipt_sum,
                self.invoices[0].effective_sum * D('0.3'),
            )
        )
        res.add_message(COMMENTS['no_valid_invoices'])
        return res


class MixedInvoice(AbstractDBTestCase):
    _representation = 'mixed_invoice'

    def prepare_data(self, session):
        client = db_utils.create_client(session)
        order = db_utils.create_order(session, client, service_id=7)
        invoice = db_utils.create_invoice(
            session, client, [(order, 100)], paysys_id=1003
        )
        invoice.create_receipt(invoice.effective_sum * D('0.3'))

        order2 = db_utils.create_order(session, client, service_id=7)
        invoice2 = db_utils.create_invoice(
            session, client, [(order2, 100)], paysys_id=1003
        )
        invoice2.create_receipt(invoice2.effective_sum * D('0.2'))

        order3 = db_utils.create_order(session, client, service_id=7)
        invoice3 = db_utils.create_invoice(
            session, client, [(order3, 100)], paysys_id=1003
        )
        invoice3.create_receipt(invoice3.effective_sum * D('0.3'))
        db_utils.consume_order(invoice3, order, 30)

        self.invoices.extend([invoice, invoice2, invoice3])

        return {'invoices': u', '.join(i.external_id for i in self.invoices)}

    def get_result(self):
        res = RequiredResult(commit=True, delay=False)
        res.transition = IssueTransitions.wont_fix
        res.add_message(COMMENTS['transfer_success'] % self.invoices[0].external_id)
        res.add_message(COMMENTS['transfer_success'] % self.invoices[2].external_id)
        res.add_message(
            COMMENTS['not_enough_receipt_sum']
            % (
                self.invoices[1].external_id,
                self.invoices[1].receipt_sum,
                self.invoices[1].effective_sum * D('0.3'),
            )
        )
        res.add_message(COMMENTS['done'])
        return res


@pytest.mark.parametrize(
    'issue_data',
    [case() for case in AbstractDBTestCase._cases],
    ids=lambda case: str(case),
    indirect=['issue_data'],
)
def test_db(session, issue_data):
    queue_object, st_issue, case = issue_data
    solver = Contract3070InvoiceTurnOnSolver(queue_object, st_issue)
    req_res = case.get_result()

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

    report_comments = sorted(report_comments)
    req_res_comments = sorted(req_res_comments)

    assert len(report_comments) == len(req_res_comments)
    for i in range(len(req_res_comments)):
        assert req_res_comments[i] == report_comments[i]

    if req_res.invoices:
        for i in req_res.invoices:
            assert i.consume_sum == i.effective_sum
