# -*- coding: utf-8 -*-

from __future__ import unicode_literals

import collections
import datetime
import datetime as dt
import decimal

import pytest
import mock

import balance.mapper as mapper
from balance import muzzle_util as ut
from balance.actions.consumption import reverse_consume

from autodasha.solver_cl import ReturnReceipt, ParseException
from autodasha.core.api.tracker import IssueTransitions

from tests.autodasha_tests.functional_tests import case_utils
from tests.autodasha_tests.functional_tests import mock_utils
from tests.autodasha_tests.common import db_utils
from tests import object_builder as ob


D = decimal.Decimal


def is_good_man_patch(self):
    return self.issue.author == 'good'


class AbstractParseTestCase(case_utils.AbstractMockTestCase):
    _default_lines = {
        'summary': 'Зачисление средств на заказ по счету',
        'invoice': '№ счета: %s',
        'order': '№ заказа для зачисления средств: %s-%s',
        'sum_': 'Сумма зачисления: %s',
        'reason': 'Причина зачисления: %s'
    }

    def __init__(self):
        self._invoice = None
        self._order = None
        self._sum = None

    def _create_objects(self, mock_manager):
        self._invoice = mock_utils.create_invoice(mock_manager, external_id='Б-666666-66')
        self._order = mock_utils.create_order(mock_manager, service_id=7, service_order_id=666666)
        self._sum = 100500


class AbstractValidTestCase(AbstractParseTestCase):
    _cases = []
    __metaclass__ = case_utils.TestCaseMeta


class AbstractInvalidTestCase(AbstractParseTestCase):
    _cases = []
    __metaclass__ = case_utils.TestCaseMeta


class GoodCase(AbstractValidTestCase):
    _representation = 'is good man'

    author = 'good'

    def get_data(self, mock_manager):
        self._create_objects(mock_manager)
        lines = [
            self._get_default_line(invoice=self._invoice.external_id),
            self._get_default_line(order=(self._order.service_id, self._order.service_order_id)),
            self._get_default_line(sum_=self._sum),
            self._get_default_line('reason')
        ]
        return self._get_default_line('summary'), lines

    def get_result(self):
        return {
            'invoice': self._invoice,
            'order': self._order,
            'sum_': self._sum
        }


class IsBadManCase(AbstractInvalidTestCase):
    _representation = 'is bad man'

    author = 'bad'

    def get_data(self, mock_manager):
        self._create_objects(mock_manager)
        lines = [
            self._get_default_line(invoice=self._invoice.external_id),
            self._get_default_line(order=(self._order.service_id, self._order.service_order_id)),
            self._get_default_line(sum_=self._sum),
            self._get_default_line('reason')
        ]
        return self._get_default_line('summary'), lines

    def get_result(self):
        return (
            'Инициатором данной заявки может быть только сотрудник отдела оформления документов. '
            'Для добавления в отдел обратитесь в поддержку '
            'через ((https://wiki.yandex-team.ru/otdelsoprovozhdeniya/dokument/avtomatformi/ общую форму)).'
        )


class BadOrderCase(AbstractInvalidTestCase):
    _representation = 'bad order'

    author = 'good'

    def get_data(self, mock_manager):
        self._create_objects(mock_manager)
        lines = [
            self._get_default_line(invoice=self._invoice.external_id),
            self._get_default_line(order=(7, '-11111111')),
            self._get_default_line(sum_=self._sum),
            self._get_default_line('reason')
        ]
        return self._get_default_line('summary'), lines

    def get_result(self):
        return 'Некорректно указан номер заказа. Пример верного формата: 77-123456. Пожалуйста, заполни форму еще раз.'


class BadInvoiceCase(AbstractInvalidTestCase):
    _representation = 'bad invoice'

    author = 'good'

    def get_data(self, mock_manager):
        self._create_objects(mock_manager)
        lines = [
            self._get_default_line(invoice='Б-ЖЖЖЖЖЖЖЖЖЖЖЖЖ-ПЧЕЛА'),
            self._get_default_line(order=(self._order.service_id, self._order.service_order_id)),
            self._get_default_line(sum_=self._sum),
            self._get_default_line('reason')
        ]
        return self._get_default_line('summary'), lines

    def get_result(self):
        return 'Некорректно указан счет. Поправь, пожалуйста, текст задачи и переоткрой ее.'


class BadSumCase(AbstractInvalidTestCase):
    _representation = 'bad sum'

    author = 'good'

    def get_data(self, mock_manager):
        self._create_objects(mock_manager)
        lines = [
            self._get_default_line(invoice=self._invoice.external_id),
            self._get_default_line(order=(self._order.service_id, self._order.service_order_id + 1)),
            self._get_default_line(sum_='тыща'),
            self._get_default_line('reason')
        ]
        return self._get_default_line('summary'), lines

    def get_result(self):
        return 'Некорректно указана сумма. Пожалуйста, заполни форму еще раз.'


class UnknownInvoiceCase(AbstractInvalidTestCase):
    _representation = 'unknown invoice'

    author = 'good'

    def get_data(self, mock_manager):
        self._create_objects(mock_manager)
        lines = [
            self._get_default_line(invoice='Б-777777-77'),
            self._get_default_line(order=(self._order.service_id, self._order.service_order_id)),
            self._get_default_line(sum_=self._sum),
            self._get_default_line('reason')
        ]
        return self._get_default_line('summary'), lines

    def get_result(self):
        return 'Счет не найден в биллинге. Поправь, пожалуйста, текст задачи и переоткрой ее.'


class UnknownOrderCase(AbstractInvalidTestCase):
    _representation = 'unknown order'

    author = 'good'

    def get_data(self, mock_manager):
        self._create_objects(mock_manager)
        lines = [
            self._get_default_line(invoice=self._invoice.external_id),
            self._get_default_line(order=(self._order.service_id, self._order.service_order_id + 1)),
            self._get_default_line(sum_=self._sum),
            self._get_default_line('reason')
        ]
        return self._get_default_line('summary'), lines

    def get_result(self):
        return 'Заказ не найден в биллинге. Поправь, пожалуйста, текст задачи и переоткрой ее.'


@pytest.mark.parametrize('mock_issue_data',
                         [case() for case in AbstractValidTestCase._cases],
                         ids=lambda case: str(case),
                         indirect=['mock_issue_data'])
def test_parse_issue(mock_queue_object, mock_issue_data):
    issue, case = mock_issue_data

    required_res = case.get_result()

    solver = ReturnReceipt(mock_queue_object, issue)
    with mock.patch('autodasha.solver_cl.ReturnReceipt._is_good_man', new=is_good_man_patch):
        res = solver.parse_issue()

    assert required_res == res


@pytest.mark.parametrize('mock_issue_data',
                        [case() for case in AbstractInvalidTestCase._cases],
                         ids=lambda case: str(case),
                         indirect=['mock_issue_data'])
def test_parse_issue_fail(mock_queue_object, mock_issue_data):
    issue, case = mock_issue_data

    exception = case.get_result()

    solver = ReturnReceipt(mock_queue_object, issue)
    with pytest.raises(ParseException) as exc:
        with mock.patch('autodasha.solver_cl.ReturnReceipt._is_good_man', new=is_good_man_patch):
            solver.parse_issue()

    assert exc.value.message == exception


def create_instant_payment(session, invoice, sum_):
    payment = mapper.RBSPayment(invoice)
    payment.mdorder = 'trololo'
    session.add(payment)
    session.flush()

    payment.set_state('paid', 'by_my_will', sum_)
    payment.mark_turned_on(dt.datetime.now())
    payment.turn_on_hook_before()
    session.flush()

    return payment


class RequiredResult(case_utils.RequiredResult):
    COMMENTS = {
        'bad_man':
            'Инициатором данной заявки может быть только сотрудник отдела оформления документов. '
            'Для добавления в отдел обратитесь в поддержку '
            'через ((https://wiki.yandex-team.ru/otdelsoprovozhdeniya/dokument/avtomatformi/ общую форму)).',
        'client_mismatch':
            'Заказ {order.eid} и счет {invoice.external_id} принадлежат разным клиентам.'
            ' Зачисление средств невозможно.',
        'discount_type_mismatch':
            'Зачисление средств на заказ по счету с разными типами рекламируемых продуктов запрещено. '
            'Данный вопрос согласован с юристами компании.',
        'order_unmoderated': 'Заказ {order.eid} не промодерирован. Зачисление средств невозможно.',
        'invoice_unmoderated': 'Счет {invoice.external_id} не промодерирован. Зачисление средств невозможно.',
        'fictive_invoice':
            'Зачислить средства на заказ {order.eid} по фиктивному счету {invoice.external_id} невозможно. '
            'Средства были возвращены в кредит. Клиент может самостоятельно выставить новый счет, '
            'зачислив деньги на требуемый заказ',
        'repayment_invoice':
            'Зачислить средства на заказ {order.eid} по постоплатному счету {invoice.external_id} невозможно. '
            'Средства были возвращены в кредит. Клиент может самостоятельно выставить новый счет, '
            'зачислив деньги на требуемый заказ',
        'personal_account':
            'Зачислить средства на заказ {order.eid} по '
            'лицевому счету {invoice.external_id} клиент может самостоятельно. '
            'Для этого ему необходимо перейти в сервис и пополнить баланс кампании',
        'certificate_invoice':
            'Данный счёт не обрабатывается автоматически. Обратитесь в поддержку '
            'через ((https://wiki.yandex-team.ru/otdelsoprovozhdeniya/dokument/avtomatformi/ общую форму)).',
        'not_enough_payments':
            'Средства в сумме {sum:.2f} не могут быть зачислены на заказ {order.eid}, '
            'т.к. сумма больше оплаченной суммы.',
        'invalid_sum_quant':
            'Невозможно зачислить сумму {sum:.2f} на заказ {order.eid} '
            '- стоимость единицы продукта больше данной суммы.',
        'not_ok': 'Зачислить указанную сумму на заказ {order.eid} не получилось.',
    }

    def __init__(self, invoice, order, receipt_sum=None, **kwargs):
        self.invoice = invoice
        self.order = order
        self.receipt_sum = receipt_sum or invoice.receipt_sum
        self.orders_states = {}

        super(RequiredResult, self).__init__(**kwargs)

    def set_object_states(self, **kwargs):
        for q in self.invoice.consumes:
            self.add_order_state(q.order)

    def add_order_state(self, order=None, qty=0, sum_=0):
        if order is None:
            order = self.order
        self.orders_states[order] = {
            'qty': sum(q.current_qty for q in order.consumes) + qty,
            'sum': sum(q.current_sum for q in order.consumes) + sum_
        }

    def transfer_order(self, sum_=None, qty=None):
        if sum_ is None:
            sum_ = self.receipt_sum

        if qty is None:
            qty = sum_ / 30

        self.add_message('Зачислена сумма %.2f на заказ %s.' % (sum_, self.order.eid))
        self.add_order_state(self.order, qty, sum_)

    def fail(self, name, **kwargs):
        self.add_message(self.COMMENTS[name].format(invoice=self.invoice, order=self.order, **kwargs))


class AbstractDBTestCase(case_utils.AbstractDBTestCase):
    _cases = []
    __metaclass__ = case_utils.TestCaseMeta

    summary = 'Зачисление средств на заказ по счету'

    _description = '''
№ счета: {invoice.external_id}
№ заказа для зачисления средств: {order.eid}
Сумма зачисления: {sum}
Причина зачисления: патамучта гладиолус
'''.strip()

    author = 'good'

    def __init__(self):
        self._invoice = None
        self._order = None


class FailedParseCase(AbstractDBTestCase):
    _representation = 'failed parse'

    author = 'bad'

    def _get_data(self, session):
        client = db_utils.create_client(session)
        self._order = db_utils.create_order(session, client)
        self._invoice = db_utils.create_invoice(session, client, [(self._order, 10)])
        create_instant_payment(session, self._invoice, 300)

        self._invoice.create_receipt(150)
        self._invoice.transfer(self._order, 1, 150)

        return {'invoice': self._invoice, 'order': self._order, 'sum': 150}

    def get_result(self):
        res = RequiredResult(self._invoice, self._order, 150, transition=IssueTransitions.wont_fix)
        res.fail('bad_man')
        return res


class NoReceiptCase(AbstractDBTestCase):
    _representation = 'no receipt'

    def _get_data(self, session):
        client = db_utils.create_client(session)
        self._order = db_utils.create_order(session, client)
        self._invoice = db_utils.create_invoice(session, client, [(self._order, 10)])
        create_instant_payment(session, self._invoice, 300)

        self._invoice.create_receipt(150)
        self._invoice.transfer(self._order, 1, 150)

        return {'invoice': self._invoice, 'order': self._order, 'sum': 150}

    def get_result(self):
        res = RequiredResult(self._invoice, self._order, 300, transition=IssueTransitions.fixed)
        res.transfer_order(150)
        return res


class ReceiptCase(AbstractDBTestCase):
    _representation = 'receipt'

    def _get_data(self, session):
        client = db_utils.create_client(session)
        self._order = db_utils.create_order(session, client)
        self._invoice = db_utils.create_invoice(session, client, [(self._order, 10)])
        create_instant_payment(session, self._invoice, 300)
        self._invoice.create_receipt(300)
        self._invoice.transfer(self._order, 1, 150)

        return {'invoice': self._invoice, 'order': self._order, 'sum': 120}

    def get_result(self):
        res = RequiredResult(self._invoice, self._order, transition=IssueTransitions.fixed)
        res.transfer_order(120)
        return res


class OverdraftCase(AbstractDBTestCase):
    _representation = 'overdraft'

    def _get_data(self, session):
        client = db_utils.create_client(session)
        self._order = db_utils.create_order(session, client)
        self._invoice = db_utils.create_invoice(session, client, [(self._order, 10)], overdraft=1)
        self._invoice.receipt_sum_1c = 300
        self._invoice.create_receipt(300)
        self._invoice.transfer(self._order, 1, 150)

        return {'invoice': self._invoice, 'order': self._order, 'sum': 120}

    def get_result(self):
        res = RequiredResult(self._invoice, self._order, transition=IssueTransitions.fixed)
        res.transfer_order(120)
        return res


class OEBSPaymentCase(AbstractDBTestCase):
    _representation = 'oebs'

    def _get_data(self, session):
        client = db_utils.create_client(session)
        self._order = db_utils.create_order(session, client)
        self._invoice = db_utils.create_invoice(session, client, [(self._order, 10)])
        self._invoice.receipt_sum_1c = 300
        self._invoice.create_receipt(300)
        self._invoice.transfer(self._order, 1, 150)

        return {'invoice': self._invoice, 'order': self._order, 'sum': 150}

    def get_result(self):
        res = RequiredResult(self._invoice, self._order, transition=IssueTransitions.fixed)
        res.transfer_order(150)
        return res


class ManualDiscountCase(AbstractDBTestCase):
    _representation = 'manual_discount'

    def _get_data(self, session):
        client = db_utils.create_client(session)
        self._order = db_utils.create_order(session, client)
        self._invoice = db_utils.create_invoice(session, client, [(self._order, 10)])
        self._invoice.receipt_sum_1c = 300
        self._invoice.create_receipt(150)
        res = self._invoice.transfer(self._order, 2, 20, discount_pct=50, skip_check=True)
        reverse_consume(res.consume, None, 10)

        return {'invoice': self._invoice, 'order': self._order, 'sum': 150}

    def get_result(self):
        res = RequiredResult(self._invoice, self._order, 300, transition=IssueTransitions.fixed)
        res.transfer_order(150, 5)
        return res


class PromoDiscountCase(AbstractDBTestCase):
    _representation = 'promo_discount'

    def _get_data(self, session):
        start_dt = datetime.datetime.today() - datetime.timedelta(days=1)
        finish_dt = datetime.datetime.today() + datetime.timedelta(days=1)
        client = db_utils.create_client(session)
        self._order = db_utils.create_order(session, client)
        db_utils.create_promocode_reservation(session, client, start_dt=start_dt, end_dt=finish_dt, discount_pct=50)
        self._invoice = db_utils.create_invoice(session, client, [(self._order, 10)])
        self._invoice.receipt_sum_1c = 10 * 30
        self._invoice.create_receipt(10 * 30)
        self._invoice.turn_on_rows(apply_promocode=True)
        reverse_consume(self._invoice.consumes[0], None, 5)

        return {'invoice': self._invoice, 'order': self._order, 'sum': 75}

    def get_result(self):
        res = RequiredResult(self._invoice, self._order, 300, transition=IssueTransitions.fixed)
        res.transfer_order(75, 5)
        return res


class PromoDiscountExpiredCase(AbstractDBTestCase):
    _representation = 'promo_discount_expired'

    def _get_data(self, session):
        start_dt = datetime.datetime.today() - datetime.timedelta(days=1)
        finish_dt = datetime.datetime.today() + datetime.timedelta(days=1)
        client = db_utils.create_client(session)
        self._order = db_utils.create_order(session, client)
        pcr = db_utils.create_promocode_reservation(
            session, client, start_dt=start_dt, end_dt=finish_dt, discount_pct=50
        )
        self._invoice = db_utils.create_invoice(session, client, [(self._order, 10)])
        self._invoice.receipt_sum_1c = 10 * 30
        self._invoice.create_receipt(10 * 30)
        self._invoice.turn_on_rows(apply_promocode=True)
        reverse_consume(self._invoice.consumes[0], None, 5)
        pcr.promocode.group.end_dt = start_dt
        session.flush()

        return {'invoice': self._invoice, 'order': self._order, 'sum': 75}

    def get_result(self):
        res = RequiredResult(self._invoice, self._order, 300, transition=IssueTransitions.fixed)
        res.transfer_order(75, D('5'))
        return res


class CashbackUsedCase(AbstractDBTestCase):
    _representation = 'cashback_used'

    def _get_data(self, session):
        client = db_utils.create_client(session)
        self._order = db_utils.create_order(session, client)
        self._invoice = db_utils.create_invoice(session, client, [(self._order, 10)])
        self._invoice.receipt_sum_1c = 10 * 30
        self._invoice.create_receipt(10 * 30)
        res = self._invoice.transfer(self._order, 2, 10)
        reverse_consume(res.consume, None, 5)
        ob.ClientCashbackBuilder.construct(session, client=client, bonus=100)

        return {'invoice': self._invoice, 'order': self._order, 'sum': 150}

    def get_result(self):
        res = RequiredResult(self._invoice, self._order, 300, transition=IssueTransitions.fixed)
        # ~5 + (5 * 100 / 150)
        res.transfer_order(150, D('8.333333'))
        return res


class CashbackReusedCase(AbstractDBTestCase):
    _representation = 'cashback_reused'

    def _get_data(self, session):
        client = db_utils.create_client(session)
        self._order = db_utils.create_order(session, client)
        self._invoice = db_utils.create_invoice(session, client, [(self._order, 10)])
        self._invoice.receipt_sum_1c = 10 * 30
        self._invoice.create_receipt(10 * 30)
        ob.ClientCashbackBuilder.construct(session, client=client, bonus=100)
        res = self._invoice.transfer(self._order, 2, 10)
        reverse_consume(res.consume, None, 5)

        return {'invoice': self._invoice, 'order': self._order, 'sum': 150}

    def get_result(self):
        res = RequiredResult(self._invoice, self._order, 300, transition=IssueTransitions.fixed)
        # ~ 5 + (5 * 100 / 150)
        res.transfer_order(150, D('8.333333'))
        return res


class CashbackReusedChangedCase(AbstractDBTestCase):
    _representation = 'cashback_reused_changed'

    def _get_data(self, session):
        client = db_utils.create_client(session)
        self._order = db_utils.create_order(session, client)
        self._invoice = db_utils.create_invoice(session, client, [(self._order, 10)])
        self._invoice.receipt_sum_1c = 10 * 30
        self._invoice.create_receipt(10 * 30)
        ob.ClientCashbackBuilder.construct(session, client=client, bonus=100)
        res = self._invoice.transfer(self._order, 2, 10)
        reverse_consume(res.consume, None, 5)
        client.cashbacks[(self._order.service_id, self._invoice.iso_currency, None, None)].bonus += 200
        session.flush()

        return {'invoice': self._invoice, 'order': self._order, 'sum': 150}

    def get_result(self):
        res = RequiredResult(self._invoice, self._order, 300, transition=IssueTransitions.fixed)
        # ~ 5 + (5 * 300 / 150)
        res.transfer_order(150, 15)
        return res


class PromoDiscountWithCashbackCase(AbstractDBTestCase):
    _representation = 'promo_discount_with_cashback'

    def _get_data(self, session):
        start_dt = datetime.datetime.today() - datetime.timedelta(days=1)
        finish_dt = datetime.datetime.today() + datetime.timedelta(days=1)
        client = db_utils.create_client(session)
        self._order = db_utils.create_order(session, client)
        db_utils.create_promocode_reservation(session, client, start_dt=start_dt, end_dt=finish_dt, discount_pct=50)
        self._invoice = db_utils.create_invoice(session, client, [(self._order, 10)])
        self._invoice.receipt_sum_1c = 10 * 30
        self._invoice.create_receipt(10 * 30)
        self._invoice.turn_on_rows(apply_promocode=True)
        reverse_consume(self._invoice.consumes[0], None, 5)
        ob.ClientCashbackBuilder.construct(session, client=client, bonus=100)

        return {'invoice': self._invoice, 'order': self._order, 'sum': 75}

    def get_result(self):
        res = RequiredResult(self._invoice, self._order, 300, transition=IssueTransitions.fixed)
        # ~ 5 + ((75 / 30) * (100 / 75))
        res.transfer_order(75, D('8.333333'))
        return res


class OtherOrderDiscountCase(AbstractDBTestCase):
    _representation = 'other_discount'

    def _get_data(self, session):
        client = db_utils.create_client(session)
        self._order = db_utils.create_order(session, client)
        order = db_utils.create_order(session, client)
        self._invoice = db_utils.create_invoice(session, client, [(self._order, 10), (order, 10)])
        self._invoice.receipt_sum_1c = 600
        self._invoice.create_receipt(300)

        self._invoice.transfer(order, 2, 5)
        res = self._invoice.transfer(self._order, 2, 20, discount_pct=50, skip_check=True)
        reverse_consume(res.consume, None, 10)

        return {'invoice': self._invoice, 'order': self._order, 'sum': 150}

    def get_result(self):
        res = RequiredResult(self._invoice, self._order, 450, transition=IssueTransitions.fixed)
        res.transfer_order(150, 5)
        return res


class NoDiscountWNewReservationCase(AbstractDBTestCase):
    _representation = 'no_discount_new_reservation'

    def _get_data(self, session):
        self._start_dt = ut.trunc_date(datetime.datetime.today() - datetime.timedelta(days=1))
        self._finish_dt = ut.trunc_date(datetime.datetime.today() + datetime.timedelta(days=1))
        client = db_utils.create_client(session)
        self._order = db_utils.create_order(session, client)
        self._reservation = db_utils.create_promocode_reservation(
            session, client, start_dt=self._start_dt, end_dt=self._finish_dt, discount_pct=25
        )
        self._invoice = db_utils.create_invoice(session, client, [(self._order, 10)])
        self._invoice.receipt_sum_1c = 600
        self._invoice.create_receipt(300)

        self._invoice.transfer(self._order, 2, 10)

        return {'invoice': self._invoice, 'order': self._order, 'sum': 150}

    def get_result(self):
        res = RequiredResult(self._invoice, self._order, 450, transition=IssueTransitions.fixed)
        res.transfer_order(150, 5)
        assert self._invoice.promo_code is None
        session = self._invoice.session
        reservations = session.query(mapper.PromoCodeReservation).filter(
            mapper.PromoCodeReservation.client_id == self._order.client_id
        ).all()
        assert len(reservations) == 1 and reservations[0].begin_dt == self._start_dt and reservations[0].end_dt == self._finish_dt
        return res


class DiscountWNewReservationCase(AbstractDBTestCase):
    _representation = 'discount_new_reservation'

    def _get_data(self, session):
        self._start_dt = ut.trunc_date(datetime.datetime.today() - datetime.timedelta(days=1))
        self._finish_dt = ut.trunc_date(datetime.datetime.today() + datetime.timedelta(days=1))
        client = db_utils.create_client(session)
        self._order = db_utils.create_order(session, client)
        reservation = db_utils.create_promocode_reservation(
            session, client, start_dt=self._start_dt, end_dt=self._finish_dt, discount_pct=40
        )
        reservation.promocode.code += '-1'
        self._invoice = db_utils.create_invoice(session, client, [(self._order, 10)])
        self._invoice.receipt_sum_1c = 300
        self._invoice.create_receipt(300)
        self._invoice.turn_on_rows(apply_promocode=True)
        reverse_consume(self._invoice.consumes[0], None, 5)

        self._reservation = db_utils.create_promocode_reservation(
            session, client, start_dt=self._start_dt, end_dt=self._finish_dt, discount_pct=25
        )

        return {'invoice': self._invoice, 'order': self._order, 'sum': 90}

    def get_result(self):
        res = RequiredResult(self._invoice, self._order, 300, transition=IssueTransitions.fixed)
        res.transfer_order(90, 5)
        assert self._invoice.promo_code is not None
        assert self._invoice.promo_code.group
        session = self._invoice.session
        reservations = session.query(mapper.PromoCodeReservation).filter(
            mapper.PromoCodeReservation.client_id == self._order.client_id,
            mapper.PromoCodeReservation.promocode == self._reservation.promocode,
        ).all()
        assert len(reservations) == 1 and reservations[0].begin_dt == self._start_dt and reservations[0].end_dt == self._finish_dt
        return res


class ExpiredDiscountWNewReservationCase(AbstractDBTestCase):
    _representation = 'expired_discount_new_reservation'

    def _get_data(self, session):
        self._start_dt = ut.trunc_date(datetime.datetime.today() - datetime.timedelta(days=1))
        self._finish_dt = ut.trunc_date(datetime.datetime.today() + datetime.timedelta(days=1))
        client = db_utils.create_client(session)
        self._order = db_utils.create_order(session, client)
        reservation = db_utils.create_promocode_reservation(
            session, client, start_dt=self._start_dt, end_dt=self._finish_dt, discount_pct=40
        )
        reservation.promocode.code += '-1'
        self._invoice = db_utils.create_invoice(session, client, [(self._order, 10)])
        self._invoice.receipt_sum_1c = 300
        self._invoice.create_receipt(300)
        self._invoice.turn_on_rows(apply_promocode=True)
        reverse_consume(self._invoice.consumes[0], None, 5)
        reservation.promocode.group.end_dt = self._start_dt

        self._reservation = db_utils.create_promocode_reservation(
            session, client, start_dt=self._start_dt, end_dt=self._finish_dt, discount_pct=25
        )
        self._group = self._invoice.promo_code.group

        return {'invoice': self._invoice, 'order': self._order, 'sum': 90}

    def get_result(self):
        res = RequiredResult(self._invoice, self._order, 300, transition=IssueTransitions.fixed)
        res.transfer_order(90, 5)
        assert self._invoice.promo_code is not None
        assert self._invoice.promo_code.group is self._group
        session = self._invoice.session
        reservations = session.query(mapper.PromoCodeReservation).filter(
            mapper.PromoCodeReservation.client_id == self._order.client_id,
            mapper.PromoCodeReservation.promocode == self._reservation.promocode,
        ).all()
        assert len(reservations) == 1 and reservations[0].begin_dt == self._start_dt and reservations[0].end_dt == self._finish_dt
        return res


class ClientsMismatchCase(AbstractDBTestCase):
    _representation = 'clients mismatch'

    def _get_data(self, session):
        client = db_utils.create_client(session)
        order = db_utils.create_order(session, client)
        self._order = db_utils.create_order(session)
        self._invoice = db_utils.create_invoice(session, client, [(order, 10)])
        self._invoice.receipt_sum_1c = 300
        self._invoice.create_receipt(300)

        return {'invoice': self._invoice, 'order': self._order, 'sum': 150}

    def get_result(self):
        res = RequiredResult(self._invoice, self._order, transition=IssueTransitions.wont_fix)
        res.fail('client_mismatch')
        return res


class DiscountTypesMismatchCase(AbstractDBTestCase):
    _representation = 'discount types mismatch'

    def _get_data(self, session):
        client = db_utils.create_client(session)
        order = db_utils.create_order(session, client)
        self._order = db_utils.create_order(session, client, product_id=2136)
        self._invoice = db_utils.create_invoice(session, client, [(order, 10)])
        self._invoice.receipt_sum_1c = 100500
        self._invoice.create_receipt(100500)

        return {'invoice': self._invoice, 'order': self._order, 'sum': 150}

    def get_result(self):
        res = RequiredResult(self._invoice, self._order, transition=IssueTransitions.wont_fix)
        res.fail('discount_type_mismatch')
        return res


class OrderUnmoderatedCase(AbstractDBTestCase):
    _representation = 'order unmoderated'

    def _get_data(self, session):
        client = db_utils.create_client(session)
        order = db_utils.create_order(session, client)
        self._order = db_utils.create_order(session, client)
        self._order.unmoderated = 1

        self._invoice = db_utils.create_invoice(session, client, [(order, 10)])
        self._invoice.receipt_sum_1c = 300
        self._invoice.create_receipt(300)

        return {'invoice': self._invoice, 'order': self._order, 'sum': 150}

    def get_result(self):
        res = RequiredResult(self._invoice, self._order,  transition=IssueTransitions.wont_fix)
        res.fail('order_unmoderated')
        return res


class InvoiceUnmoderatedCase(AbstractDBTestCase):
    _representation = 'invoice unmoderated'

    def _get_data(self, session):
        client = db_utils.create_client(session)
        order = db_utils.create_order(session, client)
        order.unmoderated = 1

        self._order = db_utils.create_order(session, client)
        self._invoice = db_utils.create_invoice(session, client, [(order, 10)])
        self._invoice.receipt_sum_1c = 300
        self._invoice.create_receipt(300)

        return {'invoice': self._invoice, 'order': self._order, 'sum': 150}

    def get_result(self):
        res = RequiredResult(self._invoice, self._order,  transition=IssueTransitions.wont_fix)
        res.fail('invoice_unmoderated')
        return res


class FictiveInvoiceCase(AbstractDBTestCase):
    _representation = 'fictive invoice'

    def _get_data(self, session):
        client, person = db_utils.create_client_person(session)
        month = mapper.ActMonth()
        contract = db_utils.create_general_contract(session, client, person, month.begin_dt, payment_type=3)
        self._order = db_utils.create_order(session, client)
        self._invoice = db_utils.create_fictive_invoice(session, contract, [(self._order, 10)])
        self._invoice.create_receipt(300)

        return {'invoice': self._invoice, 'order': self._order, 'sum': 150}

    def get_result(self):
        res = RequiredResult(self._invoice, self._order,  transition=IssueTransitions.wont_fix)
        res.fail('fictive_invoice')
        return res


class RepaymentInvoiceCase(AbstractDBTestCase):
    _representation = 'repayment invoice'

    def _get_data(self, session):
        client, person = db_utils.create_client_person(session)
        month = mapper.ActMonth()
        contract = db_utils.create_general_contract(session, client, person, month.begin_dt, payment_type=3)

        self._order = db_utils.create_order(session, client)
        fictive_invoice = db_utils.create_fictive_invoice(session, contract, [(self._order, 10)])

        self._order.calculate_consumption(month.document_dt, {self._order.shipment_type: 10})
        act, = db_utils.generate_acts(client, month, dps=[fictive_invoice.deferpay.id])

        self._invoice = act.invoice
        self._invoice.receipt_sum_1c = 300
        self._invoice.create_receipt(300)

        return {'invoice': self._invoice, 'order': self._order, 'sum': 150}

    def get_result(self):
        res = RequiredResult(self._invoice, self._order,  transition=IssueTransitions.wont_fix)
        res.fail('repayment_invoice')
        return res


class PersonalAccountCase(AbstractDBTestCase):
    _representation = 'personal account'

    def _get_data(self, session):
        client, person = db_utils.create_client_person(session)

        self._order = db_utils.create_order(session, client)
        self._invoice = db_utils.create_personal_account(session, client, person)
        self._invoice.create_receipt(300)

        return {'invoice': self._invoice, 'order': self._order, 'sum': 150}

    def get_result(self):
        res = RequiredResult(self._invoice, self._order,  transition=IssueTransitions.wont_fix)
        res.fail('personal_account')
        return res


class YInvoiceCase(AbstractDBTestCase):
    _representation = 'y_invoice'

    def _get_data(self, session):
        client, person = db_utils.create_client_person(session)
        month = mapper.ActMonth()

        self._order = db_utils.create_order(session, client)
        pa = db_utils.create_personal_account(session, client, person)
        pa.create_receipt(300)
        pa.transfer(self._order)

        self._order.calculate_consumption(month.document_dt, {self._order.shipment_type: 10})
        act, = db_utils.generate_acts(client, month, invoices=[pa.id])
        self._invoice = act.invoice

        return {'invoice': self._invoice, 'order': self._order, 'sum': 150}

    def get_result(self):
        res = RequiredResult(self._invoice, self._order,  transition=IssueTransitions.wont_fix)
        res.fail('repayment_invoice')
        return res


class CertificateInvoiceCase(AbstractDBTestCase):
    _representation = 'certificate'

    def _get_data(self, session):
        client = db_utils.create_client(session)

        self._order = db_utils.create_order(session, client)
        self._invoice = db_utils.create_invoice(session, client, [(self._order, 10)], paysys_id=1006)
        self._invoice.receipt_sum_1c = 300
        self._invoice.create_receipt(300)

        return {'invoice': self._invoice, 'order': self._order, 'sum': 150}

    def get_result(self):
        res = RequiredResult(self._invoice, self._order,  transition=IssueTransitions.wont_fix)
        res.fail('certificate_invoice')
        return res


class NoPaymentCase(AbstractDBTestCase):
    _representation = 'no payment receipt'

    def _get_data(self, session):
        client = db_utils.create_client(session)
        self._order = db_utils.create_order(session, client)
        self._invoice = db_utils.create_invoice(session, client, [(self._order, 10)])
        self._invoice.create_receipt(300)
        self._invoice.transfer(self._order, 1, 150)

        return {'invoice': self._invoice, 'order': self._order, 'sum': 150}

    def get_result(self):
        res = RequiredResult(self._invoice, self._order, transition=IssueTransitions.wont_fix)
        res.fail('not_enough_payments', sum=150)
        return res


class NoPaymentNoReceiptCase(AbstractDBTestCase):
    _representation = 'no payment no receipt'

    def _get_data(self, session):
        client = db_utils.create_client(session)
        self._order = db_utils.create_order(session, client)
        self._invoice = db_utils.create_invoice(session, client, [(self._order, 10)])
        self._invoice.create_receipt(150)
        self._invoice.transfer(self._order, 1, 150)

        return {'invoice': self._invoice, 'order': self._order, 'sum': 150}

    def get_result(self):
        res = RequiredResult(self._invoice, self._order, transition=IssueTransitions.wont_fix)
        res.fail('not_enough_payments', sum=150)
        return res


class InvalidQuantCase(AbstractDBTestCase):
    _representation = 'quant'

    def _get_data(self, session):
        client = db_utils.create_client(session)
        self._order = db_utils.create_order(session, client, product_id=503849)
        self._invoice = db_utils.create_invoice(session, client, [(self._order, 1)], turn_on=True)
        self._invoice.receipt_sum_1c = self._invoice.effective_sum.as_decimal() * 2
        self._invoice.create_receipt(self._invoice.receipt_sum_1c.as_decimal())

        return {'invoice': self._invoice, 'order': self._order, 'sum': 100}

    def get_result(self):
        res = RequiredResult(self._invoice, self._order, transition=IssueTransitions.wont_fix)
        res.fail('invalid_sum_quant', sum=100)
        return res


@pytest.mark.parametrize('issue_data',
                         [case() for case in AbstractDBTestCase._cases],
                         ids=lambda case: str(case),
                         indirect=['issue_data'])
def test_db(session, issue_data):
    queue_object, st_issue, case = issue_data
    req_res = case.get_result()

    solver = ReturnReceipt(queue_object, st_issue)
    with mock.patch('autodasha.solver_cl.ReturnReceipt._is_good_man', new=is_good_man_patch):
        res = solver.solve()

    assert res.commit is True
    assert res.delay is False
    report = res.issue_report

    invoice = req_res.invoice
    orders_qtys = collections.defaultdict(lambda: {'qty': 0, 'sum': 0})
    for consume in invoice.consumes:
        orders_qtys[consume.order]['qty'] += consume.current_qty
        orders_qtys[consume.order]['sum'] += consume.current_sum

    assert len(report.comments) <= 1
    assert set(req_res.comments) == {report.comment.strip()}
    assert invoice.receipt_sum == req_res.receipt_sum
    assert orders_qtys == req_res.orders_states
    assert report.transition == req_res.transition
    assert report.assignee == req_res.assignee
