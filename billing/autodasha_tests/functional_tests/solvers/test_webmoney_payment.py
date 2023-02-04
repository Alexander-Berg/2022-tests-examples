# -*- coding: utf-8 -*-

from __future__ import unicode_literals

import pytest
import datetime as dt

from balance import mapper
from autodasha.solver_cl import WebMoneyPayment, ParseException
from autodasha.core.api.tracker import IssueTransitions
from autodasha.core.api.tracker import IssueReport

from tests.autodasha_tests.common import db_utils
from tests.autodasha_tests.functional_tests import case_utils
from tests.autodasha_tests.functional_tests import mock_utils


def create_wm_payment(session, invoice, success, sys_payment_id):
    terminal = session.query(mapper.Terminal).getone(88001001)
    payment = mapper.WebMoneyPaymasterPayment(invoice, terminal)
    session.add(payment)
    payment.sys_payment_id = sys_payment_id
    if success:
        payment.payment_dt = dt.datetime.now()
    else:
        payment.cancel_dt = dt.datetime.now()
    session.flush()
    return payment


def create_fast_invoice(session, is_wm=True):
    client = db_utils.create_client(session)
    order = db_utils.create_order(session, client)
    invoice = db_utils.create_invoice(session, client, [(order, 1000)],
                                      1052 if is_wm else 1003)
    return invoice


COMMENTS = {
        'not_found_invoice': 'Счет {} не найден.',

        'no_invoices': 'Не найден ни один счет.',

        'not_wm': 'Счет {} имеет способ оплаты, отличный от WebMoney.',

        'no_payments': 'Платежей по счету {} нет.',

        'no_success_payments': 'Успешных платежей по счету {} нет.',

        'single_ok': 'Счет {} - номер платежа {}.',

        'plural_ok': 'Обрати, пожалуйста, внимание, для счета {} нашлось более одной успешной транзакции: {}.',

        'fail_invoice': 'Для счета {} не найден номер платежа WebMoney.',

        'call_manager': 'кто:{}, посмотри, пожалуйста.'
}


class RequiredResult(case_utils.RequiredResult):
    def __init__(self, **kwargs):
        self.commit = kwargs.get('commit')
        self.delay = kwargs.get('delay')

        super(RequiredResult, self).__init__(**kwargs)


class AbstractDBTestCase(case_utils.AbstractDBTestCase):
    _summary = 'Номер платежа WebMoney для возврата средств клиенту'
    _description = '''
№ счета: {invoices}
Комментарий: сорян, забыл убрать до коммита
'''.strip()
    issue_key = 'test_webmoney_payment'

    _cases = []
    __metaclass__ = case_utils.TestCaseMeta

    def __init__(self):
        super(AbstractDBTestCase, self).__init__()
        self.invoices = None


class SuccessSingleInvoiceCase(AbstractDBTestCase):
    _representation = 'success_single_invoice'

    def _get_data(self, session):
        self.invoice = create_fast_invoice(session, is_wm=True)
        create_wm_payment(session, self.invoice, success=True, sys_payment_id='666666')
        create_wm_payment(session, self.invoice, success=False, sys_payment_id=None)
        return {
            'invoices': self.invoice.external_id
        }

    def get_result(self):
        res = RequiredResult(transition=IssueTransitions.fixed, commit=True, delay=False)
        res.add_message(COMMENTS['single_ok'].format(self.invoice.external_id, '666666'))
        return res


class NotWMSingleInvoiceCase(AbstractDBTestCase):
    _representation = 'not_wm_single_invoice'

    def _get_data(self, session):
        self.invoice = create_fast_invoice(session, is_wm=False)
        return {
            'invoices': self.invoice.external_id
        }

    def get_result(self):
        res = RequiredResult(transition=IssueTransitions.fixed, commit=True, delay=False)
        res.add_message(COMMENTS['not_wm'].format(self.invoice.external_id))
        return res


class NoSuccessPaymentsSingleInvoiceCase(AbstractDBTestCase):
    _representation = 'no_success_payments_single_invoice_case'

    def _get_data(self, session):
        self.invoice = create_fast_invoice(session, is_wm=True)
        create_wm_payment(session, self.invoice, success=False, sys_payment_id='666666')
        return {
            'invoices': self.invoice.external_id
        }

    def get_result(self):
        res = RequiredResult(transition=IssueTransitions.fixed, commit=True, delay=False)
        res.add_message(COMMENTS['no_success_payments'].format(self.invoice.external_id))
        return res


class NoPaymentsSingleInvoiceCase(AbstractDBTestCase):
    _representation = 'no_payments_single_invoice_case'

    def _get_data(self, session):
        self.invoice = create_fast_invoice(session, is_wm=True)
        return {
            'invoices': self.invoice.external_id
        }

    def get_result(self):
        res = RequiredResult(transition=IssueTransitions.fixed, commit=True, delay=False)
        res.add_message(COMMENTS['no_payments'].format(self.invoice.external_id))
        return res


class PaymentWOSysPaymentIdCase(AbstractDBTestCase):
    _representation = 'payment_wo_sys_payment_id'

    def _get_data(self, session):
        self.invoice = create_fast_invoice(session, is_wm=True)
        create_wm_payment(session, self.invoice, success=True, sys_payment_id=None)
        return {
            'invoices': self.invoice.external_id
        }

    def get_result(self):
        res = RequiredResult(transition=IssueTransitions.opened, commit=False, delay=False)
        res.assignee = 'mscnad7'
        res.add_message(COMMENTS['call_manager'].format('mscnad7'))
        res.add_message(COMMENTS['fail_invoice'].format(self.invoice.external_id))
        return res


class PluralSuccessPaymentsCase(AbstractDBTestCase):
    _representation = 'plural_success_payments_case'

    def _get_data(self, session):
        self.invoice = create_fast_invoice(session, is_wm=True)
        create_wm_payment(session, self.invoice, success=True, sys_payment_id='1234')
        create_wm_payment(session, self.invoice, success=True, sys_payment_id='4321')
        return {
            'invoices': self.invoice.external_id
        }

    def get_result(self):
        res = RequiredResult(transition=IssueTransitions.fixed, commit=True, delay=False)
        # TODO: Исправить FLAKY-тест
        #       Тест падает, когда платежи из базы прилетают в другом порядке - '4321, 1234'
        res.add_message(COMMENTS['plural_ok'].format(self.invoice.external_id, '1234, 4321'))
        return res


@pytest.mark.parametrize('issue_data',
                         [case() for case in AbstractDBTestCase._cases],
                         ids=lambda case: str(case),
                         indirect=['issue_data'])
def test_db(session, issue_data):
    queue_object, st_issue, case = issue_data
    req_res = case.get_result()

    solver = WebMoneyPayment(queue_object, st_issue)
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

    comments_parts = filter(None, map(unicode.strip, report.comment.strip().split('\n')))
    assert set(req_res.comments) == set(comments_parts)
    assert len(report.comments) <= 1
    assert report.transition == req_res.transition
    assert report.assignee == req_res.assignee


class AbstractParseTestCase(case_utils.AbstractMockTestCase):
    _default_lines = {
        'summary': 'Номер платежа WebMoney для возврата средств клиенту',
        'invoices': '№ счета: %s',
        'comment': 'Комментарий: просто дай мне этот номер'
    }

    def __init__(self):
        self.invoices = None

    @property
    def external_ids(self):
        return ', '.join(i.external_id for i in self.invoices)


class AbstractFailedCheckFormTestCase(AbstractParseTestCase):
    _cases = []
    __metaclass__ = case_utils.TestCaseMeta


class AbstractSuccessCheckFormTestCase(AbstractParseTestCase):
    _cases = []
    __metaclass__ = case_utils.TestCaseMeta


class GoodSingleInvoiceTestCase(AbstractSuccessCheckFormTestCase):
    _representation = 'good_single_invoice'

    def get_data(self, mock_manager):
        self.invoices = [mock_utils.create_invoice(mock_manager, external_id='Б-66666-6')]
        lines = [
            self._get_default_line(invoices=self.external_ids),
            self._get_default_line('comment')
        ]
        return self._get_default_line('summary'), lines

    def get_result(self):
        return self.invoices


class GoodMultipleInvoicesWithHardTextTestCase(AbstractSuccessCheckFormTestCase):
    _representation = 'good_multiple_invoices'

    def get_data(self, mock_manager):
        self.invoices = [
            mock_utils.create_invoice(mock_manager, external_id='Б-66666-6'),
            mock_utils.create_invoice(mock_manager, external_id='Б-123456-6'),
            mock_utils.create_invoice(mock_manager, external_id='Б-4324324324-6')
        ]
        lines = [
            self._get_default_line(invoices='Мне бы вот по таким счетам получить \
                                            номера: Б-66666-6,Б-123456-6   Б-4324324324-6'),
            self._get_default_line('comment')
        ]
        return self._get_default_line('summary'), lines

    def get_result(self):
        return self.invoices


class GoodAndBadInvoicesTestCase(AbstractSuccessCheckFormTestCase):
    _representation = 'good_and_bad_invoices'

    def get_data(self, mock_manager):
        self.invoices = [
            mock_utils.create_invoice(mock_manager, external_id='Б-66666-6'),
            mock_utils.create_invoice(mock_manager, external_id='Б-123456-6'),
            mock_utils.create_invoice(mock_manager, external_id='Б-4324324324-6')
        ]
        lines = [
            self._get_default_line(invoices='Мне бы вот по таким счетам получить \
                                            номера: Б-66666-6,Б-123456-6   Б-4324324324-6, Б-34343434-1, Б-1-1'),
            self._get_default_line('comment')
        ]
        return self._get_default_line('summary'), lines

    def get_result(self):
        return self.invoices


class IncorrectInvoiceCase(AbstractFailedCheckFormTestCase):
    _representation = 'incorrect_invoice_case'

    def get_data(self, mock_manager):
        lines = [
            self._get_default_line(invoices='Хер, а не счет'),
            self._get_default_line('comment')
        ]
        return self._get_default_line('summary'), lines

    def get_result(self):
        return [COMMENTS['no_invoices']]


class NotFoundedInvoicesCase(AbstractFailedCheckFormTestCase):
    _representation = 'not_found_invoices_case'

    def get_data(self, mock_manager):
        self.invoices = [
            mock_utils.create_invoice(mock_manager, external_id='Б-66666-6'),
            mock_utils.create_invoice(mock_manager, external_id='Б-123456-6'),
            mock_utils.create_invoice(mock_manager, external_id='Б-4324324324-6')
        ]
        lines = [
            self._get_default_line(invoices='Б-7777-1, Б-533536456456546645-1'),
            self._get_default_line('comment')
        ]
        return self._get_default_line('summary'), lines

    def get_result(self):
        return [
            COMMENTS['not_found_invoice'].format('Б-7777-1'),
            COMMENTS['not_found_invoice'].format('Б-533536456456546645-1')
        ]


@pytest.mark.parametrize('mock_issue_data',
                         [case() for case in AbstractSuccessCheckFormTestCase._cases],
                         ids=lambda case: str(case),
                         indirect=['mock_issue_data'])
def test_parse_success(mock_queue_object, mock_issue_data):
    ri = IssueReport()
    issue, case = mock_issue_data
    required_res = case.get_result()
    solver = WebMoneyPayment(mock_queue_object, issue)
    res = solver.parse_issue(ri)

    assert required_res == res


@pytest.mark.parametrize('mock_issue_data',
                         [case() for case in AbstractFailedCheckFormTestCase._cases],
                         ids=lambda case: str(case),
                         indirect=['mock_issue_data'])
def test_parse_failing(mock_queue_object, mock_issue_data):
    issue, case = mock_issue_data
    ri = IssueReport()

    req_comments = case.get_result()

    solver = WebMoneyPayment(mock_queue_object, issue)
    with pytest.raises(ParseException):
        solver.parse_issue(ri)

    res_comment = ri.comment
    res_comments = res_comment.splitlines()
    assert set(req_comments) == set(res_comments)
