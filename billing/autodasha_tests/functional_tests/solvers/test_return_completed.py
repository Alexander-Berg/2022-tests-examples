# -*- coding: utf-8 -*-

from __future__ import unicode_literals

import datetime as dt

import pytest

from balance import mapper

from autodasha.solver_cl import ReturnCompleted
from autodasha.core.api.tracker import IssueTransitions, IssueStatuses

from tests.autodasha_tests.functional_tests import case_utils
from tests.autodasha_tests.common import db_utils


class RequiredResult(case_utils.RequiredResult):
    COMMENTS = {
        'invoice_not_found': 'Счет не найден в биллинге: {i.external_id}.',
        'not_solved': 'Задача не выполнена.',
        'type_pa': 'Счет {i.external_id} лицевой. Сумму к возврату проверим вручную.',
        'type_y':
            'Счет {i.external_id} постоплатный, сформированный на лицевой счет. Сумму к возврату проверим вручную.',
        'type_unknown': 'Счет {i.external_id}. Сумму к возврату проверим вручную.',
        'too_long': 'Более 7 дней не отображается возврат по счету {i.external_id}. Будет ли возврат по нему?',
        'return_done': 'Счет: **{i.external_id}** списание отобразилось: **{sum}**',
        'removed_orderless': 'Стерто беззаказье на сумму {sum}',
        'not_enough_orderless': 'На счете {i.external_id} недостаточно средств на беззаказье',
        'not_enough_payments': 'Зачислено больше, чем есть поступлений',
        'no_returns': 'Возвратов по задаче нет.'
    }

    def __init__(self, **kwargs):
        self.invoices_states = []
        super(RequiredResult, self).__init__(**kwargs)

    def add_state(self, invoice, receipt_sum):
        self.invoices_states.append((invoice, receipt_sum))

    def add_not_changed_state(self, invoice):
        self.add_state(invoice, invoice.receipt_sum)

    def set_messages(self, no_returns=False, not_solved=False, **kwargs):
        if no_returns:
            self.add_message(self.COMMENTS['no_returns'])

        if not_solved:
            self.add_message(self.COMMENTS['not_solved'])

    def invoice_error(self, invoice, error):
        self.add_not_changed_state(invoice)
        self.add_message(self.COMMENTS[error].format(i=invoice))

    def invoice_returned(self, invoice, ret_sum, receipt_sum, msg=None):
        self.add_state(invoice, receipt_sum)
        self.add_message(self.COMMENTS['return_done'].format(i=invoice, sum=ret_sum))
        if msg:
            self.add_message(self.COMMENTS[msg].format(i=invoice, sum=ret_sum))


class AbstractDBTestCase(case_utils.AbstractDBTestCase):
    _cases = []
    __metaclass__ = case_utils.TestCaseMeta

    author = 'accountant'

    summary = 'Вернула'

    def __init__(self):
        self.client = None
        self.person = None
        self.order = None
        self.max_id = 0
        self.invoices = []

    def get_description(self, session):
        invoices = self._get_data(session)

        description = 'Данные по счетам (в вышеописанном формате): %s'

        invoice_lines = '\n'.join('%s %s' % (i.external_id, sum_) for i, sum_ in invoices)
        return description % invoice_lines

    def _init_data(self, session):
        self.client, self.person = db_utils.create_client_person(session)
        self.order = db_utils.create_order(session, self.client, product_id=503162)

    def _create_payment(self, session, invoice, sum_):
        payment = mapper.OebsCashPaymentFact(
                xxar_cash_fact_id=self.max_id - 1,
                amount=sum_,
                receipt_number=invoice.external_id,
                receipt_date=dt.datetime.now(),
                last_updated_by=666,
                last_update_date=dt.datetime.now(),
                created_by=666,
                creation_date=dt.datetime.now(),
                operation_type='INSERT'
        )
        session.add(payment)
        session.flush()
        self.max_id = payment.xxar_cash_fact_id
        return payment

    def _create_invoice(self, session, consume_sum, receipt_sum, income_sum=None, return_sum=None):
        invoice = db_utils.create_invoice(session, self.client, [(self.order, consume_sum)],
                                          person=self.person, turn_on=True)
        invoice.create_receipt(receipt_sum - invoice.receipt_sum)

        if income_sum is not None:
            self._create_payment(session, invoice, income_sum)

        if return_sum is not None:
            self._create_payment(session, invoice, -return_sum)

        session.refresh(invoice)

        return invoice


class InvalidTypesTest(AbstractDBTestCase):
    _representation = 'invalid_types'

    def _get_data(self, session):
        self._init_data(session)

        y_invoice = self._create_invoice(session, 100, 100)
        y_invoice.type = 'y_invoice'

        pa = self._create_invoice(session, 200, 500)
        pa.type = 'personal_account'

        fictive = self._create_invoice(session, 200, 500)
        fictive.type = 'fictive'

        session.flush()

        self.invoices = [y_invoice, pa, fictive]

        return [(y_invoice, 20), (pa, 300), (fictive, 300)]

    def get_result(self):
        res = RequiredResult(transition=IssueTransitions.need_info, assignee='mscnad7')
        res.invoice_error(self.invoices[0], 'type_y')
        res.invoice_error(self.invoices[1], 'type_pa')
        res.invoice_error(self.invoices[2], 'type_unknown')
        return res


class ReturnedWOrderlessTest(AbstractDBTestCase):
    _representation = 'returned_w_orderless'

    def _get_data(self, session):
        self._init_data(session)

        i1 = self._create_invoice(session, 100, 100, 120, 20)
        i2 = self._create_invoice(session, 200, 500, 500, 300)
        self.invoices = [i1, i2]

        return [(i1, 20), (i2, 300)]

    def get_result(self):
        res = RequiredResult(transition=IssueTransitions.fixed)
        res.invoice_returned(self.invoices[0], 20, 100)
        res.invoice_returned(self.invoices[1], 300, 200, 'removed_orderless')
        return res


class ReturnedNotEnoughOrderlessTest(AbstractDBTestCase):
    _representation = 'returned_not_enough_orderless'

    def _get_data(self, session):
        self._init_data(session)

        i1 = self._create_invoice(session, 100, 100, 120, 20)
        i2 = self._create_invoice(session, 200, 250, 500, 300)
        self.invoices = [i1, i2]

        return [(i1, 20), (i2, 300)]

    def get_result(self):
        res = RequiredResult(transition=IssueTransitions.need_info, assignee='mscnad7')
        res.invoice_returned(self.invoices[0], 20, 100)
        res.invoice_returned(self.invoices[1], 300, 250, 'not_enough_orderless')
        return res


class ReturnedUnderpaymentTest(AbstractDBTestCase):
    _representation = 'returned_underpayment'

    def _get_data(self, session):
        self._init_data(session)

        i1 = self._create_invoice(session, 100, 100, 120, 20)
        i2 = self._create_invoice(session, 201, 200, 500, 300)
        self.invoices = [i1, i2]

        return [(i1, 20), (i2, 300)]

    def get_result(self):
        res = RequiredResult(transition=IssueTransitions.need_info, assignee='mscnad7')
        res.invoice_returned(self.invoices[0], 20, 100)
        res.invoice_returned(self.invoices[1], 300, 200, 'not_enough_payments')
        return res


class NotReturnedNewTest(AbstractDBTestCase):
    _representation = 'not_returned_new'

    issue_dt = dt.datetime.now()

    def _get_data(self, session):
        self._init_data(session)

        i1 = self._create_invoice(session, 100, 100, 120, 20)
        i2 = self._create_invoice(session, 200, 500, 500)
        self.invoices = [i1, i2]

        return [(i1, 20), (i2, 300)]

    def get_result(self):
        res = RequiredResult(transition=IssueTransitions.none)
        res.invoice_returned(self.invoices[0], 20, 100)
        res.add_state(self.invoices[1], 500)
        return res


class NotReturnedOldTest(AbstractDBTestCase):
    _representation = 'not_returned_old'

    issue_dt = dt.datetime.now() - dt.timedelta(8)

    def _get_data(self, session):
        self._init_data(session)

        i1 = self._create_invoice(session, 100, 100, 120, 20)
        i2 = self._create_invoice(session, 200, 500, 500)
        self.invoices = [i1, i2]

        return [(i1, 20), (i2, 300)]

    def get_result(self):
        res = RequiredResult(transition=IssueTransitions.need_info)
        res.invoice_returned(self.invoices[0], 20, 100)
        res.invoice_error(self.invoices[1], 'too_long')
        return res


class NotReturnedOldCommentedTest(AbstractDBTestCase):
    _representation = 'not_returned_old_commented'

    issue_dt = dt.datetime.now() - dt.timedelta(8)

    def _get_data(self, session):
        self._init_data(session)

        i1 = self._create_invoice(session, 100, 100, 120, 20)
        i2 = self._create_invoice(session, 200, 500, 500)
        self.invoices = [i1, i2]

        return [(i1, 20), (i2, 300)]

    def get_comments(self):
        cmt = 'Более 7 дней не отображается возврат по счету %s. Будет ли возврат по нему?' \
              % self.invoices[1].external_id
        return [('autodasha', cmt)]

    def get_result(self):
        res = RequiredResult(transition=IssueTransitions.none)
        res.invoice_returned(self.invoices[0], 20, 100)
        res.add_not_changed_state(self.invoices[1])
        return res


class NotReturnedOldManagerCommentTest(AbstractDBTestCase):
    _representation = 'not_returned_old_manager_comment'

    issue_dt = dt.datetime.now() - dt.timedelta(8)

    def _get_data(self, session):
        self._init_data(session)

        i = self._create_invoice(session, 200, 500, 500)
        self.invoices = [i]

        return [(i, 300)]

    def get_comments(self):
        cmt = 'Более 7 дней не отображается возврат по счету %s. Будет ли возврат по нему?' \
              % self.invoices[0].external_id
        return [('autodasha', cmt),
                ('manager', 'Траляля %s' % self.invoices[0].external_id)]

    def get_result(self):
        res = RequiredResult(transition=IssueTransitions.none)
        res.add_not_changed_state(self.invoices[0])
        return res


class ReturnedOldCommentedTest(AbstractDBTestCase):
    _representation = 'returned_old_commented'

    issue_dt = dt.datetime.now() - dt.timedelta(8)

    def _get_data(self, session):
        self._init_data(session)

        i1 = self._create_invoice(session, 100, 100, 120, 20)
        i2 = self._create_invoice(session, 200, 500, 500, 300)
        self.invoices = [i1, i2]

        return [(i1, 20), (i2, 300)]

    def get_comments(self):
        cmt = 'Более 7 дней не отображается возврат по счету %s. Будет ли возврат по нему?' \
              % self.invoices[1].external_id
        return [('autodasha', cmt)]

    def get_result(self):
        res = RequiredResult(transition=IssueTransitions.fixed)
        res.invoice_returned(self.invoices[0], 20, 100)
        res.invoice_returned(self.invoices[1], 300, 200, 'removed_orderless')
        return res


class ReturnedOldProcessedTest(AbstractDBTestCase):
    _representation = 'returned_old_processed'

    issue_dt = dt.datetime.now() - dt.timedelta(8)

    def _get_data(self, session):
        self._init_data(session)

        i1 = self._create_invoice(session, 100, 100, 120, 20)
        i2 = self._create_invoice(session, 200, 500, 500, 300)
        self.invoices = [i1, i2]

        return [(i1, 20), (i2, 300)]

    def get_comments(self):
        i = self.invoices[1].external_id
        cmt1 = 'Более 7 дней не отображается возврат по счету %s. Будет ли возврат по нему?' % i
        cmt2 = 'Счет: **%s** списание отобразилось: **300**' % i
        return [('autodasha', cmt1), ('autodasha', cmt2)]

    def get_result(self):
        res = RequiredResult(transition=IssueTransitions.fixed)
        res.invoice_returned(self.invoices[0], 20, 100)
        res.add_not_changed_state(self.invoices[1])
        return res


class AllProcessedTest(AbstractDBTestCase):
    _representation = 'all_processed'

    issue_dt = dt.datetime.now() - dt.timedelta(8)

    def _get_data(self, session):
        self._init_data(session)

        i1 = self._create_invoice(session, 100, 100, 120, 20)
        i2 = self._create_invoice(session, 200, 200, 500, 300)
        self.invoices = [i1, i2]

        return [(i1, 20), (i2, 300)]

    def get_comments(self):
        cmt1 = 'Счет: **%s** списание отобразилось: **20**' % self.invoices[0].external_id
        cmt2 = 'Счет: **%s** списание отобразилось: **300**' % self.invoices[1].external_id
        return [('autodasha', cmt1), ('autodasha', cmt2)]

    def get_result(self):
        res = RequiredResult(transition=IssueTransitions.fixed, no_returns=True)
        res.add_not_changed_state(self.invoices[0])
        res.add_not_changed_state(self.invoices[1])
        return res


class AlreadyNeedInfoTest(AbstractDBTestCase):
    _representation = 'already_need_info'

    status = IssueStatuses.need_info
    assignee = 'some_dick'

    def _get_data(self, session):
        self._init_data(session)

        i1 = self._create_invoice(session, 100, 100, 120, 20)
        i2 = self._create_invoice(session, 200, 250, 500, 300)
        self.invoices = [i1, i2]

        return [(i1, 20), (i2, 300)]

    def get_result(self):
        res = RequiredResult(transition=IssueTransitions.none, assignee=None)
        res.invoice_returned(self.invoices[0], 20, 100)
        res.invoice_returned(self.invoices[1], 300, 250, 'not_enough_orderless')
        return res


class AlreadyNeedInfoAllProcessedTest(AbstractDBTestCase):
    _representation = 'already_need_info_all_processed'

    status = IssueStatuses.need_info
    assignee = 'some_dick'

    def _get_data(self, session):
        self._init_data(session)

        i1 = self._create_invoice(session, 100, 100, 120, 20)
        i2 = self._create_invoice(session, 200, 200, 500, 300)
        self.invoices = [i1, i2]

        return [(i1, 20), (i2, 300)]

    def get_comments(self):
        cmt1 = 'Счет: **%s** списание отобразилось: **20**' % self.invoices[0].external_id
        cmt2 = 'Счет: **%s** списание отобразилось: **300**' % self.invoices[1].external_id
        return [('autodasha', cmt1), ('autodasha', cmt2)]

    def get_result(self):
        res = RequiredResult(transition=IssueTransitions.none, assignee=None)
        res.add_not_changed_state(self.invoices[0])
        res.add_not_changed_state(self.invoices[1])
        return res


@pytest.mark.parametrize('issue_data',
                         [case() for case in AbstractDBTestCase._cases],
                         ids=lambda case: str(case),
                         indirect=['issue_data'])
def test_db(session, issue_data):
    queue_object, st_issue, case = issue_data
    required_res = case.get_result()

    solver = ReturnCompleted(queue_object, st_issue)
    res = solver.solve()

    if required_res is None:
        assert res.commit is False
        assert res.delay is True
        assert not res.issue_report

    assert res.commit is True
    assert res.delay is (required_res.transition != IssueTransitions.fixed)
    report = res.issue_report

    assert len(report.comments) <= 1
    if not required_res.comments:
        assert not report.comment
    else:
        assert set(required_res.comments) == case_utils.prepare_comment(report.comment)
    assert required_res.transition == report.transition
    assert required_res.assignee == report.assignee

    for invoice, sum_ in required_res.invoices_states:
        assert invoice.receipt_sum == sum_
