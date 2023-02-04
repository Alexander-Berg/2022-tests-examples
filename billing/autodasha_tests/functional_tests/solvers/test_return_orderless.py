# -*- coding: utf-8 -*-

from __future__ import unicode_literals

import collections

import pytest

from balance.actions import consumption

from autodasha.solver_cl.return_orderless import ReturnOrderless
from autodasha.solver_cl import ParseException
from autodasha.core.api.tracker import IssueTransitions

from tests.autodasha_tests.functional_tests import case_utils
from tests.autodasha_tests.functional_tests import mock_utils
from tests.autodasha_tests.common import db_utils


@pytest.fixture(autouse=True)
def use_core_free_funds(request, config):
    config._items['USE_OVERACT_BY_CLIENTS'] = True


class AbstractParseTestCase(case_utils.AbstractMockTestCase):
    _default_lines = {
        'summary': 'Снятие средств со счета',
        'invoice': '№ предоплатного счета: %s',
        'return_all': 'Вернуть все свободные средства по счету: %s',
        'order': '№ заказа: %s-%s',
        'sum_': 'Сумма к возврату: %s'
    }


class AbstractInvalidTestCase(AbstractParseTestCase):
    _cases = []
    __metaclass__ = case_utils.TestCaseMeta


class AbstractValidTestCase(AbstractParseTestCase):
    _cases = []
    __metaclass__ = case_utils.TestCaseMeta


class ValidReturnAllCase(AbstractValidTestCase):
    _representation = 'valid return all'

    def __init__(self):
        self._invoice = None

    def get_data(self, mock_manager):
        self._invoice = mock_utils.create_invoice(mock_manager)
        lines = [
            self._get_default_line(invoice=self._invoice.external_id),
            self._get_default_line(return_all='да')
        ]
        return self._get_default_line('summary'), lines

    def get_result(self):
        return {
            'invoice': self._invoice,
            'return_all': True
        }


class ValidReturnAllYesCase(AbstractValidTestCase):
    _representation = 'valid return all with Yes'

    def __init__(self):
        self._invoice = None

    def get_data(self, mock_manager):
        self._invoice = mock_utils.create_invoice(mock_manager)
        lines = [
            self._get_default_line(invoice=self._invoice.external_id),
            self._get_default_line(return_all='Yes')
        ]
        return self._get_default_line('summary'), lines

    def get_result(self):
        return {
            'invoice': self._invoice,
            'return_all': True
        }


class ValidReturnAllTrueCase(AbstractValidTestCase):
    _representation = 'valid return all with True'

    def __init__(self):
        self._invoice = None

    def get_data(self, mock_manager):
        self._invoice = mock_utils.create_invoice(mock_manager)
        lines = [
            self._get_default_line(invoice=self._invoice.external_id),
            self._get_default_line(return_all='True')
        ]
        return self._get_default_line('summary'), lines

    def get_result(self):
        return {
            'invoice': self._invoice,
            'return_all': True
        }


class ValidReturnAllWOrderCase(AbstractValidTestCase):
    _representation = 'valid return all (with order)'

    def __init__(self):
        self._invoice = None

    def get_data(self, mock_manager):
        self._invoice = mock_utils.create_invoice(mock_manager)
        lines = [
            self._get_default_line(invoice=self._invoice.external_id),
            self._get_default_line(return_all='да'),
            self._get_default_line(order=(7, 666))
        ]
        return self._get_default_line('summary'), lines

    def get_result(self):
        return {
            'invoice': self._invoice,
            'return_all': True
        }


class ValidReturnOrderWReturnAllClauseCase(AbstractValidTestCase):
    _representation = 'valid return order (with return_all)'

    def __init__(self):
        self._invoice = None
        self._order = None
        self._sum = None

    def get_data(self, mock_manager):
        self._invoice = mock_utils.create_invoice(mock_manager)
        self._order = mock_utils.create_order(mock_manager)
        self._sum = 666
        lines = [
            self._get_default_line(invoice=self._invoice.external_id),
            self._get_default_line(return_all='нет'),
            self._get_default_line(order=(7, 666666)),
            self._get_default_line(sum_=self._sum)
        ]
        return self._get_default_line('summary'), lines

    def get_result(self):
        return {
            'invoice': self._invoice,
            'return_all': False,
            'order': self._order,
            'sum_': self._sum
        }


class ValidReturnOrderWOReturnAllClauseCase(AbstractValidTestCase):
    _representation = 'valid return order (without return_all)'

    def __init__(self):
        self._invoice = None
        self._order = None
        self._sum = None

    def get_data(self, mock_manager):
        self._invoice = mock_utils.create_invoice(mock_manager)
        self._order = mock_utils.create_order(mock_manager)
        self._sum = 666
        lines = [
            self._get_default_line(invoice=self._invoice.external_id),
            self._get_default_line(order=(7, 666666)),
            self._get_default_line(sum_=self._sum)
        ]
        return self._get_default_line('summary'), lines

    def get_result(self):
        return {
            'invoice': self._invoice,
            'return_all': False,
            'order': self._order,
            'sum_': self._sum
        }


class ValidReturnOrderExoticServiceCase(AbstractValidTestCase):
    _representation = 'valid return order (exotic service)'

    def __init__(self):
        self._invoice = None
        self._order = None
        self._sum = None

    def get_data(self, mock_manager):
        self._invoice = mock_utils.create_invoice(mock_manager)
        self._order = mock_utils.create_order(mock_manager, service_id=82)
        self._sum = 666
        lines = [
            self._get_default_line(invoice=self._invoice.external_id),
            self._get_default_line(order=(82, 666666)),
            self._get_default_line(sum_=self._sum)
        ]
        return self._get_default_line('summary'), lines

    def get_result(self):
        return {
            'invoice': self._invoice,
            'return_all': False,
            'order': self._order,
            'sum_': self._sum
        }


class ValidReturnAllOverdraftCase(AbstractValidTestCase):
    _representation = 'valid return all (overdraft)'

    def __init__(self):
        self._invoice = None

    def get_data(self, mock_manager):
        self._invoice = mock_utils.create_invoice(mock_manager, type='overdraft')
        lines = [
            self._get_default_line(invoice=self._invoice.external_id),
            self._get_default_line(return_all='да')
        ]
        return self._get_default_line('summary'), lines

    def get_result(self):
        return {
            'invoice': self._invoice,
            'return_all': True
        }


class InvalidInvoiceEIDCase(AbstractInvalidTestCase):
    _representation = 'invalid invoice eid'

    def __init__(self):
        self._invoice = None

    def get_data(self, mock_manager):
        self._invoice = mock_utils.create_invoice(mock_manager, external_id='Б-вжопу-6')
        lines = [
            self._get_default_line(invoice=self._invoice.external_id),
            self._get_default_line(return_all='да')
        ]
        return self._get_default_line('summary'), lines

    def get_result(self):
        return 'Некорректно указан счет. Поправь, пожалуйста, текст задачи и переоткрой её.'


class InvalidNonexistentInvoiceCase(AbstractInvalidTestCase):
    _representation = 'invalid nonexistent invoice'

    def __init__(self):
        self._invoice = None

    def get_data(self, mock_manager):
        lines = [
            self._get_default_line(invoice='Б-666-6'),
            self._get_default_line(return_all='да')
        ]
        return self._get_default_line('summary'), lines

    def get_result(self):
        return 'Счет не найден в биллинге. Поправь, пожалуйста, текст задачи и переоткрой её.'


class InvalidInvoiceLineCase(AbstractInvalidTestCase):
    _representation = 'invalid invoice line'

    def __init__(self):
        self._invoice = None

    def get_data(self, mock_manager):
        self._invoice = mock_utils.create_invoice(mock_manager, external_id='Б-666-6')
        lines = [
            '№ овердрафтного счета: %s' % self._invoice.external_id,
            self._get_default_line(return_all='да')
        ]
        return self._get_default_line('summary'), lines

    def get_result(self):
        return 'Некорректно указан счет. Поправь, пожалуйста, текст задачи и переоткрой её.'


class InvalidNotReturnAllNoOrderCase(AbstractInvalidTestCase):
    _representation = 'invalid no order'

    def __init__(self):
        self._invoice = None

    def get_data(self, mock_manager):
        self._invoice = mock_utils.create_invoice(mock_manager, external_id='Б-666-6')
        lines = [
            self._get_default_line(invoice=self._invoice.external_id),
            self._get_default_line(return_all='нет'),
            self._get_default_line(sum_=666),
        ]
        return self._get_default_line('summary'), lines

    def get_result(self):
        return 'Некорректно указан заказ. Заполни, пожалуйста, форму еще раз указав сервис заказа: 7-123456'


class InvalidNotReturnAllNonexistentOrderCase(AbstractInvalidTestCase):
    _representation = 'invalid nonexistent order'

    def __init__(self):
        self._invoice = None

    def get_data(self, mock_manager):
        self._invoice = mock_utils.create_invoice(mock_manager, external_id='Б-666-6')
        lines = [
            self._get_default_line(invoice=self._invoice.external_id),
            self._get_default_line(return_all='нет'),
            self._get_default_line(order=(7, 666)),
            self._get_default_line(sum_=666),
        ]
        return self._get_default_line('summary'), lines

    def get_result(self):
        return 'Заказ не найден в биллинге. Поправь, пожалуйста, текст задачи и переоткрой ее.'


class InvalidNotReturnAllNoSumCase(AbstractInvalidTestCase):
    _representation = 'invalid no sum'

    def __init__(self):
        self._invoice = None
        self._order = None

    def get_data(self, mock_manager):
        self._invoice = mock_utils.create_invoice(mock_manager, external_id='Б-666-6')
        self._order = mock_utils.create_order(mock_manager)
        lines = [
            self._get_default_line(invoice=self._invoice.external_id),
            self._get_default_line(return_all='No'),
            self._get_default_line(order=(7, 666666))
        ]
        return self._get_default_line('summary'), lines

    def get_result(self):
        return 'Некорректно указана сумма. Поправь, пожалуйста, текст задачи и переоткрой её.'


class InvalidReturnAllInvoiceTypeCase(AbstractInvalidTestCase):
    _representation = 'invalid invoice type'

    def __init__(self):
        self._invoice = None

    def get_data(self, mock_manager):
        self._invoice = mock_utils.create_invoice(mock_manager, external_id='Б-666-6', type='fictive')
        lines = [
            self._get_default_line(invoice=self._invoice.external_id),
            self._get_default_line(return_all='да')
        ]
        return self._get_default_line('summary'), lines

    def get_result(self):
        return 'Счет не является предоплатным или овердрафтным.' \
               ' Для снятия средств с кредитного счета заполни, пожалуйста, другую форму на wiki ' \
               '- https://wiki.yandex-team.ru/otdelsoprovozhdeniya/dokument/avtomatformi/Vozvratcredit/'


@pytest.mark.parametrize('mock_issue_data',
                         [case() for case in AbstractValidTestCase._cases],
                         ids=lambda case: str(case),
                         indirect=['mock_issue_data'])
def test_parse_issue(mock_queue_object, mock_issue_data):
    issue, case = mock_issue_data

    required_res = case.get_result()

    solver = ReturnOrderless(mock_queue_object, issue)
    res = solver.parse_issue()

    assert required_res == res


@pytest.mark.parametrize('mock_issue_data',
                        [case() for case in AbstractInvalidTestCase._cases],
                         ids=lambda case: str(case),
                         indirect=['mock_issue_data'])
def test_parse_issue_fail(mock_queue_object, mock_issue_data):
    issue, case = mock_issue_data

    exception = case.get_result()

    solver = ReturnOrderless(mock_queue_object, issue)
    with pytest.raises(ParseException) as exc:
        solver.parse_issue()

    assert exc.value.message == exception


class AbstractDBTestCase(case_utils.AbstractDBTestCase):
    _cases = []
    __metaclass__ = case_utils.TestCaseMeta

    _summary = 'Снятие средств со счета'

    def get_description(self, session):
        invoice, return_all, order, sum_ = self._get_data(session)

        description = '№ предоплатного счета: %s\n' % invoice.external_id
        if return_all:
            description += 'Вернуть все свободные средства по счету: да\n'
        if order:
            description += '№ заказа: %s-%s\n' % (order.service_id, order.service_order_id)
        if sum_:
            description += 'Сумма к возврату: %s' % sum_
        description += 'Причина возврата: патамучта гладиолус'
        return description


class RequiredResult(case_utils.RequiredResult):
    def __init__(self, invoice, orderless_sum, orders_qtys, invoice_type='prepayment', **kwargs):
        self.invoice = invoice
        self.orders_qtys = orders_qtys
        self.orderless_sum = orderless_sum
        self.invoice_type = invoice_type

        super(RequiredResult, self).__init__(transition=IssueTransitions.fixed, **kwargs)

    def set_object_states(self, **kwargs):
        pass

    def msg_returned_order(self, sum_=None, order=None):
        if order is None and len(self.orders_qtys) == 1:
            order, = self.orders_qtys

        if sum_ is None:
            sum_ = self.orderless_sum

        res_data = (sum_, self.invoice.external_id, order.eid)
        self.add_message('Сумма %.2f снята на беззаказье по счету %s с заказа %s' % res_data)

    def msg_not_enough(self, sum_):
        self.add_message('Недостаточно средств для снятия, доступно %.2f' % sum_)

    def msg_not_enough_overacted(self, sum_):
        self.add_message('Недостаточно средств для снятия, доступно %.2f (по счёту есть переакченное)' % sum_)

    def msg_unequal(self, req_sum, valid_sum):
        msg = 'Невозможно снять в точности %.2f. Допустимая сумма - %.2f.' \
              ' Поправь, пожалуйста, текст задачи и переоткрой её.' % (req_sum, valid_sum)
        self.add_message(msg)

    def msg_no_free_sum(self):
        self.add_message('По счету %s не осталось свободных средств' % self.invoice.external_id)


class ReturnSumCase(AbstractDBTestCase):
    _representation = 'return sum'

    def __init__(self, qty=15, return_sum=300):
        super(ReturnSumCase, self).__init__()
        self._qty = qty
        self._return_sum = return_sum

        self._invoice = None
        self._order_1 = None
        self._order_2 = None

    def _get_data(self, session):
        cl = db_utils.create_client(session, 'test')
        self._order_1 = db_utils.create_order(session, cl)
        self._order_2 = db_utils.create_order(session, cl)
        orders_qtys = [(self._order_1, self._qty), (self._order_2, self._qty)]
        self._invoice = db_utils.create_invoice(session, cl, orders_qtys, turn_on=True)

        return self._invoice, False, self._order_1, self._return_sum

    def get_result(self):
        orders_qtys = {self._order_1: self._qty - self._return_sum / 30, self._order_2: self._qty}
        res = RequiredResult(self._invoice, self._return_sum, orders_qtys)
        res.msg_returned_order(order=self._order_1)
        return res


class ReturnSumFullyCompletedCase(AbstractDBTestCase):
    _representation = 'return sum - fully completed'

    def __init__(self, qty=15, return_sum=300):
        super(ReturnSumFullyCompletedCase, self).__init__()
        self._qty = qty
        self._return_sum = return_sum

        self._invoice = None
        self._order = None

    def _get_data(self, session):
        cl = db_utils.create_client(session, 'test')
        self._order = db_utils.create_order(session, cl)
        self._invoice = db_utils.create_invoice_simple(session, self._order, [self._qty])
        self._order.do_process_completion(self._qty)

        return self._invoice, False, self._order, self._return_sum

    def get_result(self):
        res = RequiredResult(self._invoice, 0, {self._order: self._qty})
        res.msg_no_free_sum()
        return res


class ReturnSumPartiallyCompletedOkCase(AbstractDBTestCase):
    _representation = 'return sum - partially completed, ok'

    def __init__(self, qty=15, compl_qty=3, return_sum=300):
        super(ReturnSumPartiallyCompletedOkCase, self).__init__()
        self._qty = qty
        self._compl_qty = compl_qty
        self._return_sum = return_sum

        self._invoice = None
        self._order = None

    def _get_data(self, session):
        cl = db_utils.create_client(session, 'test')
        self._order = db_utils.create_order(session, cl)
        self._invoice = db_utils.create_invoice_simple(session, self._order, [self._qty])
        self._order.do_process_completion(self._compl_qty)

        return self._invoice, False, self._order, self._return_sum

    def get_result(self):
        res = RequiredResult(self._invoice, self._return_sum, {self._order: self._qty - self._return_sum / 30})
        res.msg_returned_order()
        return res


class ReturnSumPartiallyCompletedFailedCase(AbstractDBTestCase):
    _representation = 'return sum - partially completed, failed'

    def __init__(self, qty=15, compl_qty=6, return_sum=300):
        super(ReturnSumPartiallyCompletedFailedCase, self).__init__()
        self._qty = qty
        self._compl_qty = compl_qty
        self._return_sum = return_sum

        self._invoice = None
        self._order = None

    def _get_data(self, session):
        cl = db_utils.create_client(session, 'test')
        self._order = db_utils.create_order(session, cl)
        self._invoice = db_utils.create_invoice_simple(session, self._order, [self._qty])
        self._order.do_process_completion(self._compl_qty)

        return self._invoice, False, self._order, self._return_sum

    def get_result(self):
        res = RequiredResult(self._invoice, 0, {self._order: self._qty})
        res.msg_not_enough((self._qty - self._compl_qty) * 30)
        return res


class ReturnSumOveractedOkCase(AbstractDBTestCase):
    _representation = 'return sum - overacted, ok'

    def __init__(self, qty=15, overacted_qty=3, return_sum=300):
        super(ReturnSumOveractedOkCase, self).__init__()
        self._qty = qty
        self._overacted_qty = overacted_qty
        self._return_sum = return_sum

        self._invoice = None
        self._order = None
        self._overacted_order = None

    def _get_data(self, session):
        cl = db_utils.create_client(session, 'test')
        self._order = db_utils.create_order(session, cl)
        self._overacted_order = db_utils.create_order(session, cl)
        basket_data = [(self._order, self._qty), (self._overacted_order, self._overacted_qty)]
        self._invoice = db_utils.create_invoice(session, cl, basket_data, turn_on=True)

        self._overacted_order.do_process_completion(self._overacted_qty)
        self._invoice.generate_act(force=True)
        self._overacted_order.do_process_completion(0)

        overacted_consume, = [q for q in self._invoice.consumes if q.order is self._overacted_order]
        consumption.reverse_consume(overacted_consume, None, overacted_consume.current_qty)
        self._invoice.create_receipt(-self._overacted_qty * 30)

        return self._invoice, False, self._order, self._return_sum

    def get_result(self):
        orders = {
            self._order: self._qty - self._return_sum / 30,
            self._overacted_order: 0
        }
        res = RequiredResult(self._invoice, self._return_sum, orders)
        res.msg_returned_order(order=self._order)
        return res


class ReturnSumOveractedFailedCase(AbstractDBTestCase):
    _representation = 'return sum - overacted, fail'

    def __init__(self, qty=15, overacted_qty=6, return_sum=300):
        super(ReturnSumOveractedFailedCase, self).__init__()
        self._qty = qty
        self._overacted_qty = overacted_qty
        self._return_sum = return_sum

        self._invoice = None
        self._order = None
        self._overacted_order = None

    def _get_data(self, session):
        cl = db_utils.create_client(session, 'test')
        self._order = db_utils.create_order(session, cl)
        self._overacted_order = db_utils.create_order(session, cl)
        basket_data = [(self._order, self._qty), (self._overacted_order, self._overacted_qty)]
        self._invoice = db_utils.create_invoice(session, cl, basket_data, turn_on=True)

        self._overacted_order.do_process_completion(self._overacted_qty)
        self._invoice.generate_act(force=True)
        self._overacted_order.do_process_completion(0)

        overacted_consume, = [q for q in self._invoice.consumes if q.order is self._overacted_order]
        consumption.reverse_consume(overacted_consume, None, overacted_consume.current_qty)
        self._invoice.create_receipt(-self._overacted_qty * 30)

        return self._invoice, False, self._order, self._return_sum

    def get_result(self):
        res = RequiredResult(self._invoice, 0, {self._order: self._qty, self._overacted_order: 0})
        res.msg_not_enough_overacted((self._qty - self._overacted_qty) * 30)
        return res


class ReturnSumPrecisionFailCase(AbstractDBTestCase):
    _representation = 'return sum - precision fail'

    def __init__(self, qty=2, return_sum=200000, real_sum=150000):
        super(ReturnSumPrecisionFailCase, self).__init__()
        self._qty = qty
        self._return_sum = return_sum
        self._real_sum = real_sum

        self._invoice = None
        self._order = None

    def _get_data(self, session):
        cl = db_utils.create_client(session, 'test')
        self._order = db_utils.create_order(session, cl, product_id=503849)
        self._invoice = db_utils.create_invoice_simple(session, self._order, [self._qty])

        return self._invoice, False, self._order, self._return_sum

    def get_result(self):
        res = RequiredResult(self._invoice, 0, {self._order: self._qty})
        res.msg_unequal(self._return_sum, self._real_sum)
        return res


class ReturnAllCase(AbstractDBTestCase):
    _representation = 'return all'

    def __init__(self, qty=15):
        super(ReturnAllCase, self).__init__()
        self._qty = qty

        self._invoice = None
        self._order_1 = None
        self._order_2 = None

    def _get_data(self, session):
        cl = db_utils.create_client(session, 'test')
        self._order_1 = db_utils.create_order(session, cl)
        self._order_2 = db_utils.create_order(session, cl)
        orders_qtys = [(self._order_1, self._qty), (self._order_2, self._qty)]
        self._invoice = db_utils.create_invoice(session, cl, orders_qtys, turn_on=True)

        return self._invoice, True, None, None

    def get_result(self):
        res = RequiredResult(self._invoice, self._qty * 30 * 2, {self._order_1: 0, self._order_2: 0})
        res.msg_returned_order(sum_=self._qty * 30, order=self._order_1)
        res.msg_returned_order(sum_=self._qty * 30, order=self._order_2)
        return res


class ReturnAllFullyCompletedCase(AbstractDBTestCase):
    _representation = 'return all - fully completed'

    def __init__(self, qty=15):
        super(ReturnAllFullyCompletedCase, self).__init__()
        self._qty = qty

        self._invoice = None
        self._order = None

    def _get_data(self, session):
        cl = db_utils.create_client(session, 'test')
        self._order = db_utils.create_order(session, cl)
        self._invoice = db_utils.create_invoice_simple(session, self._order, [self._qty])
        self._order.do_process_completion(self._qty)

        return self._invoice, True, None, None

    def get_result(self):
        res = RequiredResult(self._invoice, 0, {self._order: self._qty})
        res.msg_no_free_sum()
        return res


class ReturnAllOveractedCase(AbstractDBTestCase):
    _representation = 'return all - overacted'

    def __init__(self, qty=15, overacted_qty=7):
        super(ReturnAllOveractedCase, self).__init__()
        self._qty = qty
        self._overacted_qty = overacted_qty

        self._invoice = None
        self._order = None
        self._overacted_order = None

    def _get_data(self, session):
        cl = db_utils.create_client(session, 'test')
        self._order = db_utils.create_order(session, cl)
        self._overacted_order = db_utils.create_order(session, cl)
        basket_data = [(self._order, self._qty), (self._overacted_order, self._overacted_qty)]
        self._invoice = db_utils.create_invoice(session, cl, basket_data, turn_on=True)

        self._overacted_order.do_process_completion(self._overacted_qty)
        self._invoice.generate_act(force=True)
        self._overacted_order.do_process_completion(0)

        overacted_consume, = [q for q in self._invoice.consumes if q.order is self._overacted_order]
        consumption.reverse_consume(overacted_consume, None, overacted_consume.current_qty)
        self._invoice.create_receipt(-self._overacted_qty * 30)

        return self._invoice, True, None, None

    def get_result(self):
        ret_qty = self._qty - self._overacted_qty

        res = RequiredResult(self._invoice, ret_qty * 30, {self._order: self._overacted_qty, self._overacted_order: 0})
        res.msg_returned_order(order=self._order)
        return res


class ReturnSumOverdraftCase(AbstractDBTestCase):
    _representation = 'return_sum_overdraft'

    def __init__(self, qty=15, return_sum=300):
        super(ReturnSumOverdraftCase, self).__init__()
        self._qty = qty
        self._return_sum = return_sum

        self._invoice = None
        self._order_1 = None
        self._order_2 = None

    def _get_data(self, session):
        cl = db_utils.create_client(session, 'test')
        self._order_1 = db_utils.create_order(session, cl)
        self._order_2 = db_utils.create_order(session, cl)
        orders_qtys = [(self._order_1, self._qty), (self._order_2, self._qty)]
        self._invoice = db_utils.create_invoice(session, cl, orders_qtys, overdraft=1)
        self._invoice.turn_on_rows()

        return self._invoice, False, self._order_1, self._return_sum

    def get_result(self):
        orders_qtys = {self._order_1: self._qty - self._return_sum / 30, self._order_2: self._qty}
        res = RequiredResult(self._invoice, -600, orders_qtys, invoice_type='overdraft')
        res.msg_returned_order(order=self._order_1, sum_=self._return_sum)
        return res


class ReturnAllOverdraftCase(AbstractDBTestCase):
    _representation = 'return_all_overdraft'

    def __init__(self, qty=15):
        super(ReturnAllOverdraftCase, self).__init__()
        self._qty = qty

        self._invoice = None
        self._order_1 = None
        self._order_2 = None

    def _get_data(self, session):
        cl = db_utils.create_client(session, 'test')
        self._order_1 = db_utils.create_order(session, cl)
        self._order_2 = db_utils.create_order(session, cl)
        orders_qtys = [(self._order_1, self._qty), (self._order_2, self._qty)]
        self._invoice = db_utils.create_invoice(session, cl, orders_qtys, overdraft=1)
        self._invoice.turn_on_rows()

        return self._invoice, True, None, None

    def get_result(self):
        res = RequiredResult(self._invoice, 0, {self._order_1: 0, self._order_2: 0}, invoice_type='prepayment')
        res.msg_returned_order(sum_=self._qty * 30, order=self._order_1)
        res.msg_returned_order(sum_=self._qty * 30, order=self._order_2)
        return res


@pytest.mark.parametrize('issue_data',
                         [case() for case in AbstractDBTestCase._cases],
                         ids=lambda case: str(case),
                         indirect=['issue_data'])
def test_db(session, issue_data):
    queue_object, st_issue, case = issue_data
    required_res = case.get_result()
    invoice = required_res.invoice

    solver = ReturnOrderless(queue_object, st_issue)
    res = solver.solve()

    assert res.commit is True
    assert res.delay is False
    report = res.issue_report

    orders_qtys = collections.defaultdict(int)
    for consume in invoice.consumes:
        orders_qtys[consume.order] += consume.current_qty

    assert len(report.comments) <= 1
    assert set(required_res.comments) == case_utils.prepare_comment(report.comment)
    assert report.transition == required_res.transition
    assert report.assignee == required_res.assignee
    assert invoice.receipt_sum - invoice.consume_sum == required_res.orderless_sum
    assert orders_qtys == required_res.orders_qtys
    assert required_res.invoice_type == invoice.type
