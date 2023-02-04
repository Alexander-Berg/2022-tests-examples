# -*- coding: utf-8 -*-

from __future__ import unicode_literals

import itertools
import collections
import datetime as dt

import pytest
import mock

import balance.muzzle_util as ut
from balance import constants as cst
from balance import mapper
from balance.actions.promocodes import tear_promocode_off

from autodasha.solver_cl import (
    PromoConnect,
    BetterChargeNote,
    ParseException
)
from autodasha.core.api.tracker import IssueTransitions
from autodasha.comments.promo_connect import PromoConnectCommentsManager

from tests.autodasha_tests.functional_tests import (
    case_utils,
    mock_utils
)
from tests.autodasha_tests.common import db_utils
from autodasha.utils.solver_utils import D


class AbstractParseTestCase(case_utils.AbstractMockTestCase):
    _default_lines = {
        'summary': 'Зачисление бонуса по промокоду',
        'invoice': '№ счета: %s',
        'pc': 'Промокод: %s',
        'comment': 'Причина, по которой код не зачислился автоматически: тест'
    }


class AbstractSuccessCheckFormTestCase(AbstractParseTestCase):
    _cases = []
    __metaclass__ = case_utils.TestCaseMeta

    def __init__(self):
        self.invoice = None
        self.pc = None


class AbstractFailedCheckFormTestCase(AbstractParseTestCase):
    _cases = []
    __metaclass__ = case_utils.TestCaseMeta

    def __init__(self):
        self.invoice = None
        self.pc = None


class SingleInvoiceCase(AbstractSuccessCheckFormTestCase):
    _representation = 'single_invoice'

    def get_data(self, mock_manager):
        self.pc = mock_utils.create_promocode(mock_manager, 'PROMO666')
        self.invoice = mock_utils.create_invoice(mock_manager, external_id='Б-666-6')

        lines = [
            self._get_default_line(invoice='   Б-666-6 '),
            self._get_default_line(pc='\r\rPROMO-666\n'),
            self._get_default_line('comment')
        ]
        return self._get_default_line('summary'), lines

    def get_result(self):
        return {
            'invoice': self.invoice,
            'promocode': self.pc
        }


class MultipleInvoicesCase(AbstractSuccessCheckFormTestCase):
    _representation = 'multiple_invoices'

    def get_data(self, mock_manager):
        self.pc = mock_utils.create_promocode(mock_manager, 'PROMO666')
        self.invoice = mock_utils.create_invoice(mock_manager, external_id='Б-666-6')

        lines = [
            self._get_default_line(invoice='   Б-666-6, Б-1234-5 '),
            self._get_default_line(pc='\r\rPROMO-666'),
            self._get_default_line('comment')
        ]
        return self._get_default_line('summary'), lines

    def get_result(self):
        return {
            'invoice': self.invoice,
            'promocode': self.pc
        }

class NoDataCase(AbstractFailedCheckFormTestCase):
    _representation = 'no_data'

    def get_data(self, mock_manager):
        lines = [
            self._get_default_line(invoice='номер счета не помню'),
            self._get_default_line(pc='промик'),
            self._get_default_line('comment')
        ]
        return self._get_default_line('summary'), lines

    def get_result(self):
        return 'По указанным данным не удалось распознать номер счета. ' \
               'Проверь, пожалуйста, данные и создай задачу через ту же форму.'


class InvoiceNotFoundCase(AbstractFailedCheckFormTestCase):
    _representation = 'invoice_not_found'

    def get_data(self, mock_manager):
        mock_utils.create_invoice(
            mock_manager, external_id='Б-666-6')
        lines = [
            self._get_default_line(invoice='Б-666-7'),
            self._get_default_line(pc='промик'),
            self._get_default_line('comment')
        ]
        return self._get_default_line('summary'), lines

    def get_result(self):
        return 'Счет не найден в биллинге. ' \
               'Проверь, пожалуйста, данные и создай задачу через ту же форму.'


class IncorrectPromocodeCase(AbstractFailedCheckFormTestCase):
    _representation = 'incorrect_promocode'

    def get_data(self, mock_manager):
        self.invoice = mock_utils.create_invoice(
            mock_manager, external_id='Б-666-6')
        lines = [
            self._get_default_line(invoice='Б-666-6'),
            self._get_default_line(pc='!!промик!!'),
            self._get_default_line('comment')
        ]
        return self._get_default_line('summary'), lines

    def get_result(self):
        return 'Некорректно указан промокод. ' \
               'Проверь, пожалуйста, данные и создай задачу через ту же форму.'


class PromocodeNotFoundCase(AbstractFailedCheckFormTestCase):
    _representation = 'promocode_not_found'

    def get_data(self, mock_manager):
        self.invoice = mock_utils.create_invoice(
            mock_manager, external_id='Б-666-6')
        self.pc = mock_utils.create_promocode(mock_manager, 'PROMO666')
        lines = [
            self._get_default_line(invoice='Б-666-6'),
            self._get_default_line(pc='PROMO777'),
            self._get_default_line('comment')
        ]
        return self._get_default_line('summary'), lines

    def get_result(self):
        return 'Промокод не найден в биллинге. ' \
               'Проверь, пожалуйста, данные и создай задачу через ту же форму.'


class ELSCase(AbstractFailedCheckFormTestCase):
    _representation = 'els_case'

    def get_data(self, mock_manager):
        lines = [
            self._get_default_line(invoice='ЕЛС-1234'),
            self._get_default_line(pc='промик'),
            self._get_default_line('comment')
        ]
        return self._get_default_line('summary'), lines

    def get_result(self):
        return 'К счету с типом personal_account невозможно применить промокод.'


@pytest.mark.parametrize('mock_issue_data',
                         [case() for case in AbstractSuccessCheckFormTestCase._cases],
                         ids=lambda case: str(case),
                         indirect=['mock_issue_data'])
def test_parse_success(mock_queue_object, mock_issue_data):
    issue, case = mock_issue_data
    required_res = case.get_result()
    solver = PromoConnect(mock_queue_object, issue)
    res = solver.parse_issue()

    assert set(required_res) == set(res)


@pytest.mark.parametrize('mock_issue_data',
                         [case() for case in AbstractFailedCheckFormTestCase._cases],
                         ids=lambda case: str(case),
                         indirect=['mock_issue_data'])
def test_parse_failing(mock_queue_object, mock_issue_data):
    issue, case = mock_issue_data

    req_comment = case.get_result()

    solver = PromoConnect(mock_queue_object, issue)
    with pytest.raises(ParseException) as exc:
        solver.parse_issue()
    assert req_comment == exc.value.message


@pytest.fixture
def session(session):
    session.execute(
        "update bo.t_config set value_num=0 "
        "where item = 'CONSUMPTION_NEGATIVE_REVERSE_ALLOWED'"
    )
    return session


class RequiredResult(case_utils.RequiredResult):
    def __init__(self, **kwargs):
        self.invoices_states = []
        self.promocodes_states = []
        self.reservations_states = []
        self.commit = kwargs.get('commit', True)

        super(RequiredResult, self).__init__(**kwargs)

    def set_messages(self, invoice=None, reservation=None, promocode=None, **kwargs):
        if kwargs.get('connected'):
            self.add_message_connected(invoice, reservation, promocode)

    def add_message_connected(self, invoice, reservation=None, promocode=None):
        if promocode is None:
            promocode = reservation.promocode
        self.add_message('Промокод %s активирован в счете %s.' % (promocode.code, invoice.external_id))

    def add_message_ended(self, reservation):
        txt = 'Прошло более 7 дней с даты окончания промокода. Действовал до %s.' % reservation.promocode.end_dt.strftime('%d.%m.%Y')
        self.add_message(txt)

    def set_object_states(self, invoice=None, promocode=None, reservation=None, connected=False, **kwargs):
        if promocode is None:
            promocode = reservation and reservation.promocode

        for row in kwargs.get('invoices_states', []):
            self.add_invoice_state(*row)

        for row in kwargs.get('connected_invoices', [(invoice, )] if connected else []):
            if not isinstance(row, collections.Iterable):
                row = [row]
            self.add_connected_invoice(promocode, *row)

        for row in kwargs.get('unchanged_invoices', [(invoice, )] if not connected else []):
            self.add_unchanged_invoice(*row)

        for row in kwargs.get('promocodes_states', [(promocode, )] if promocode else []):
            self.add_promocode_state(*row)

        for row in kwargs.get('reservations_states', [(reservation, )] if reservation else []):
            self.add_reservation_state(*row)

    def add_connected_invoice(self, promocode, invoice, sum_=300, qty=40):
        self.add_invoice_state(invoice, promocode, sum_, qty)

    def add_unchanged_invoice(self, invoice, sum_=None, qty=None, dt_=None):
        if qty is None:
            qty = sum(q.current_qty for q in invoice.consumes)
        if sum_ is None:
            sum_ = sum(q.current_sum for q in invoice.consumes)

        self.add_invoice_state(invoice, None, sum_, qty)

    def add_invoice_state(self, invoice, promocode, sum_, qty):
        self.invoices_states.append((invoice,
                                     promocode and promocode.id,
                                     sum_,
                                     qty))

    def add_promocode_state(self, pc, start_dt=None, end_dt=None):
        self.promocodes_states.append((pc, start_dt or pc.start_dt, end_dt or pc.end_dt))

    def add_reservation_state(self, reservation, begin_dt=None, end_dt=None):
        self.reservations_states.append((reservation, begin_dt or reservation.begin_dt, end_dt or reservation.end_dt))


class AbstractBaseDBTestCase(case_utils.AbstractDBTestCase):
    _description = '''
№ счета: {identifier}
Промокод: {promocode.code}
Причина, по которой код не зачислился автоматически: а почему вы спрашиваете?
'''.strip()

    _summary = 'Зачисление бонуса по промокоду'

    @staticmethod
    def _get_interval(delta_start, length=30):
        on_dt = ut.trunc_date(dt.datetime.now())
        return on_dt + dt.timedelta(delta_start), on_dt + dt.timedelta(delta_start + length)


class AbstractResTestCase(AbstractBaseDBTestCase):
    _cases = []
    __metaclass__ = case_utils.TestCaseMeta


class EmptyInvoiceCase(AbstractResTestCase):
    _representation = 'empty_invoice'

    def __init__(self):
        self._invoice = None
        self._reservation = None

    def _get_data(self, session):
        client, person = db_utils.create_client_person(session)
        order = db_utils.create_order(session, client=client)
        self._invoice = db_utils.create_invoice(session, client, [(order, 10)], person=person)
        self._invoice.create_receipt(self._invoice.effective_sum)

        start_dt, end_dt = self._get_interval(-15)
        self._reservation = db_utils.create_promocode_reservation(session, client, None,
                                                                  start_dt, end_dt, 30, 10)

        return {
            'identifier': self._invoice.external_id,
            'promocode': self._reservation.promocode
        }

    def get_result(self):
        res = RequiredResult(
                transition=IssueTransitions.fixed,
                invoice=self._invoice, reservation=self._reservation,
                connected=True
        )
        return res


class ConsumedInvoiceCase(AbstractResTestCase):
    _representation = 'consumed_invoice'

    def __init__(self):
        self._invoice = None
        self._reservation = None

    def _get_data(self, session):
        client, person = db_utils.create_client_person(session)
        order = db_utils.create_order(session, client=client)
        self._invoice = db_utils.create_invoice(session, client, [(order, 10)], person=person, turn_on=True)

        start_dt, end_dt = self._get_interval(-15)
        self._reservation = db_utils.create_promocode_reservation(session, client, None,
                                                                  start_dt, end_dt, 30, 10)

        return {
            'identifier': self._invoice.external_id,
            'promocode': self._reservation.promocode
        }

    def get_result(self):
        res = RequiredResult(
                transition=IssueTransitions.fixed,
                invoice=self._invoice, reservation=self._reservation,
                connected=True
        )
        return res


class CurrencyProductCase(AbstractResTestCase):
    _representation = 'currency_product'

    def __init__(self):
        self._invoice = None
        self._reservation = None

    def _get_data(self, session):
        client, person = db_utils.create_client_person(session)
        order = db_utils.create_order(session, client=client, product_id=503162)
        self._invoice = db_utils.create_invoice(session, client, [(order, 720)], person=person, turn_on=True)

        start_dt, end_dt = self._get_interval(-15)
        self._reservation = db_utils.create_promocode_reservation(session, client, None,
                                                                  start_dt, end_dt, 20, 10)

        return {
            'identifier': self._invoice.external_id,
            'promocode': self._reservation.promocode
        }

    def get_result(self):
        res = RequiredResult(
                transition=IssueTransitions.fixed,
                invoice=self._invoice, reservation=self._reservation,
                connected=True,
                connected_invoices=[(self._invoice, 720, 1440)]
        )
        return res


class CompletedInvoiceCase(AbstractResTestCase):
    _representation = 'completed_invoice'

    def __init__(self):
        self._invoice = None
        self._reservation = None

    def _get_data(self, session):
        client, person = db_utils.create_client_person(session)
        order = db_utils.create_order(session, client=client)
        self._invoice = db_utils.create_invoice(session, client, [(order, 10)], person=person, turn_on=True)
        order.do_process_completion(10)

        start_dt, end_dt = self._get_interval(-15)
        self._reservation = db_utils.create_promocode_reservation(session, client, None,
                                                                  start_dt, end_dt, 30, 10)

        return {
            'identifier': self._invoice.external_id,
            'promocode': self._reservation.promocode
        }

    def get_result(self):
        res = RequiredResult(
                transition=IssueTransitions.fixed,
                invoice=self._invoice, reservation=self._reservation,
                connected=True
        )
        return res


class UnmoderatedOrderCase(AbstractResTestCase):
    _representation = 'unmoderated_order'

    def __init__(self):
        self._invoice = None
        self._reservation = None
        self._order = None

    def _get_data(self, session):
        client, person = db_utils.create_client_person(session)

        self._order = db_utils.create_order(session, client=client)
        self._order.unmoderated = 1
        session.flush()

        self._invoice = db_utils.create_invoice(session, client, [(self._order, 10)], person=person, turn_on=True)

        start_dt, end_dt = self._get_interval(-15)
        self._reservation = db_utils.create_promocode_reservation(session, client, None,
                                                                  start_dt, end_dt, 30, 10)

        return {
            'identifier': self._invoice.external_id,
            'promocode': self._reservation.promocode
        }

    def get_result(self):
        res = RequiredResult(transition=IssueTransitions.wont_fix,
                             invoice=self._invoice, reservation=self._reservation)
        res.add_message(PromoConnectCommentsManager()("order_is_unmoderated", self._order.eid))
        return res


class ActedInvoiceCase(AbstractResTestCase):
    _representation = 'acted_invoice'

    def __init__(self):
        self._invoice = None
        self._reservation = None

    def _get_data(self, session):
        client, person = db_utils.create_client_person(session)
        order = db_utils.create_order(session, client=client)
        self._invoice = db_utils.create_invoice(session, client, [(order, 10)], person=person, turn_on=True)
        self._invoice.close_invoice(self._invoice.dt)

        start_dt, end_dt = self._get_interval(-15)
        self._reservation = db_utils.create_promocode_reservation(session, client, None,
                                                                  start_dt, end_dt, 30, 10)

        return {
            'identifier': self._invoice.external_id,
            'promocode': self._reservation.promocode
        }

    def get_result(self):
        res = RequiredResult(transition=IssueTransitions.wont_fix, invoice=self._invoice, reservation=self._reservation)
        txt = 'По счету %s есть акты. По условиям акции промокод по счетам с оказанными заакченными ' \
              'услугами активировать не можем. Клиенту необходимо оплатить новый счет на сумму, указанную '\
              'в условиях акции по промокоду.' % self._invoice.external_id
        res.add_message(txt)
        return res


class HasPromocodeCase(AbstractResTestCase):
    _representation = 'already_has_promocode'

    def __init__(self):
        self._invoice = None
        self._previous_reservation = None
        self._reservation = None

    def _get_data(self, session):
        client, person = db_utils.create_client_person(session)
        order = db_utils.create_order(session, client=client)
        start_dt, end_dt = self._get_interval(-15)
        previous_pc = db_utils.create_promocode(session, start_dt, end_dt, 30, 10, code=u'WHYDIDYOUCOMEHERE')
        self._previous_reservation = db_utils.create_promocode_reservation(session, client, previous_pc, start_dt, end_dt)
        self._invoice = db_utils.create_invoice(session, client, [(order, 10)], person=person)
        self._invoice.create_receipt(10 * 30)
        self._invoice.turn_on_rows(apply_promocode=True)

        start_dt, end_dt = self._get_interval(-15)
        self._reservation = db_utils.create_promocode_reservation(session, client, None,
                                                                  start_dt, end_dt, 30, 10)

        return {
            'identifier': self._invoice.external_id,
            'promocode': self._reservation.promocode
        }

    def get_result(self):
        res = RequiredResult(transition=IssueTransitions.wont_fix, invoice=self._invoice)
        # Have to put the state manually to avoid adding success message
        res.invoices_states.pop()
        res.add_invoice_state(self._invoice, self._previous_reservation.promocode, 300, 40)
        txt = 'К счету уже был применен промокод %s.' % self._previous_reservation.promocode.code
        res.add_message(txt)
        return res


class PromoForRequestCase(AbstractResTestCase):
    _representation = 'promo_for_request'

    def __init__(self):
        self._invoice = None
        self._promocode = None

    def _get_data(self, session):
        client, person = db_utils.create_client_person(session)
        order = db_utils.create_order(session, client=client)
        invoice_w_promo_request = db_utils.create_invoice(session, client, [(order, 10)], person=person)
        start_dt, end_dt = self._get_interval(-15)
        self._promocode = db_utils.create_promocode(session, start_dt, end_dt, 30, 10, code='PROMOFORREQUEST')
        invoice_w_promo_request.request._set_promo_code('PROMOFORREQUEST')

        self._invoice = db_utils.create_invoice(session, client, [(order, 10)], person=person)
        self._invoice.create_receipt(10 * 30)

        return {
            'identifier': self._invoice.external_id,
            'promocode': self._promocode
        }

    def get_result(self):
        res = RequiredResult(transition=IssueTransitions.fixed, invoice=self._invoice, promocode=self._promocode, connected=True)
        return res


class NotEnoughSumCase(AbstractResTestCase):
    _representation = 'not_enough_invoice_sum'

    def __init__(self):
        self._invoice = None
        self._reservation = None

    def _get_data(self, session):
        client, person = db_utils.create_client_person(session)
        order = db_utils.create_order(session, client=client)
        self._invoice = db_utils.create_invoice(session, client, [(order, 10)], person=person)
        self._invoice.create_receipt(666 * 30)

        start_dt, end_dt = self._get_interval(-15)
        self._reservation = db_utils.create_promocode_reservation(session, client, None,
                                                                  start_dt, end_dt, 30, 666)

        return {
            'identifier': self._invoice.external_id,
            'promocode': self._reservation.promocode
        }

    def get_result(self):
        res = RequiredResult(transition=IssueTransitions.wont_fix, invoice=self._invoice, reservation=self._reservation)
        txt = 'Сумма счета меньше, чем нужно для активации промокода: %.2f (нужно %.2f).' % (10 * 30, 666 * 30)
        res.add_message(txt)
        return res


class NotEnoughPaymentsCase(AbstractResTestCase):
    _representation = 'not_enough_payments'

    def __init__(self):
        self._invoice = None
        self._reservation = None

    def _get_data(self, session):
        client, person = db_utils.create_client_person(session)
        order = db_utils.create_order(session, client=client)
        self._invoice = db_utils.create_invoice(session, client, [(order, 10)], person=person)
        self._invoice.create_receipt(5 * 30)

        start_dt, end_dt = self._get_interval(-15)
        self._reservation = db_utils.create_promocode_reservation(session, client, None,
                                                                  start_dt, end_dt, 30, 10)

        return {
            'identifier': self._invoice.external_id,
            'promocode': self._reservation.promocode
        }

    def get_result(self):
        res = RequiredResult(transition=IssueTransitions.wont_fix, invoice=self._invoice, reservation=self._reservation)
        txt = 'Для активации промокода недостаточно средств: %.2f (нужно %.2f).' % (5 * 30, 10 * 30)
        res.add_message(txt)
        return res


class EndedPromocodeCase(AbstractResTestCase):
    _representation = 'ended_promocode'

    def __init__(self):
        self._invoice = None
        self._reservation = None

    def _get_data(self, session):
        client, person = db_utils.create_client_person(session)
        order = db_utils.create_order(session, client=client)
        self._invoice = db_utils.create_invoice(session, client, [(order, 10)], person=person)
        self._invoice.create_receipt(10 * 30)

        start_dt, end_dt = self._get_interval(-45, 15)
        self._reservation = db_utils.create_promocode_reservation(session, client, None,
                                                                  start_dt, end_dt, 30, 10)

        return {
            'identifier': self._invoice.external_id,
            'promocode': self._reservation.promocode
        }

    def get_result(self):
        res = RequiredResult(transition=IssueTransitions.wont_fix, invoice=self._invoice, reservation=self._reservation)
        res.add_message_ended(self._reservation)
        return res


class EndedWithinGracePeriodPromocodeCase(AbstractResTestCase):
    _representation = 'ended_within_grace_promocode'

    def __init__(self):
        self._invoice = None
        self._reservation = None

    def _get_data(self, session):
        client, person = db_utils.create_client_person(session)
        order = db_utils.create_order(session, client=client)
        self._invoice = db_utils.create_invoice(session, client, [(order, 10)], person=person)
        self._invoice.create_receipt(10 * 30)

        start_dt, end_dt = self._get_interval(-10, 4)
        self._reservation = db_utils.create_promocode_reservation(session, client, None,
                                                                  start_dt, end_dt, 30, 10)

        return {
            'identifier': self._invoice.external_id,
            'promocode': self._reservation.promocode
        }

    def get_result(self):
        pcr = self._reservation
        pc = pcr.promocode
        end_dt = ut.trunc_date(dt.datetime.now()) + dt.timedelta(1)

        res = RequiredResult(
                transition=IssueTransitions.fixed,
                invoice=self._invoice, reservation=self._reservation,
                connected=True,
                promocodes_states=[(pc, pc.start_dt, end_dt)],
                reservations_states=[(pcr, pcr.begin_dt, end_dt)]
        )
        return res


class EndedWithinGracePeriodNoReservationPromocodeCase(AbstractResTestCase):
    _representation = 'ended_within_grace_no_reservation_promocode'

    def __init__(self):
        self._invoice = None
        self._promocode = None

    def _get_data(self, session):
        client, person = db_utils.create_client_person(session)
        order = db_utils.create_order(session, client=client)
        self._invoice = db_utils.create_invoice(session, client, [(order, 10)], person=person)
        self._invoice.create_receipt(10 * 30)

        start_dt, end_dt = self._get_interval(-10, 4)
        self._promocode = db_utils.create_promocode(session, start_dt, end_dt, 30, 10)

        return {
            'identifier': self._invoice.external_id,
            'promocode': self._promocode
        }

    def get_result(self):
        pc = self._promocode
        end_dt = ut.trunc_date(dt.datetime.now()) + dt.timedelta(1)

        res = RequiredResult(
                transition=IssueTransitions.fixed,
                invoice=self._invoice, promocode=pc,
                connected=True,
                promocodes_states=[(pc, pc.start_dt, end_dt)]
        )
        return res


class Ended1DayPromocodeCase(AbstractResTestCase):
    _representation = 'ended_promocode_1day'

    def __init__(self):
        self._invoice = None
        self._reservation = None

    def _get_data(self, session):
        client, person = db_utils.create_client_person(session)
        order = db_utils.create_order(session, client=client)
        self._invoice = db_utils.create_invoice(session, client, [(order, 10)], person=person)
        self._invoice.create_receipt(10 * 30)

        start_dt, end_dt = self._get_interval(-30, 30)
        self._reservation = db_utils.create_promocode_reservation(session, client, None,
                                                                  start_dt, end_dt, 30, 10)

        return {
            'identifier': self._invoice.external_id,
            'promocode': self._reservation.promocode
        }

    def get_result(self):
        pcr = self._reservation
        pc = pcr.promocode
        end_dt = ut.trunc_date(dt.datetime.now()) + dt.timedelta(1)

        res = RequiredResult(
                transition=IssueTransitions.fixed,
                invoice=self._invoice, reservation=self._reservation,
                connected=True,
                promocodes_states=[(pc, pc.start_dt, end_dt)],
                reservations_states=[(pcr, pcr.begin_dt, end_dt)]
        )
        return res


class FuturePromocodeCase(AbstractResTestCase):
    _representation = 'future_promocode'

    def __init__(self):
        self._invoice = None
        self._reservation = None

    def _get_data(self, session):
        client, person = db_utils.create_client_person(session)
        order = db_utils.create_order(session, client=client)
        self._invoice = db_utils.create_invoice(session, client, [(order, 10)], person=person)
        self._invoice.create_receipt(10 * 30)

        start_dt, end_dt = self._get_interval(15, 15)
        self._reservation = db_utils.create_promocode_reservation(session, client, None,
                                                                  start_dt, end_dt, 30, 10)

        return {
            'identifier': self._invoice.external_id,
            'promocode': self._reservation.promocode
        }

    def get_result(self):
        pcr = self._reservation
        pc = pcr.promocode
        start_dt = ut.trunc_date(dt.datetime.now())

        res = RequiredResult(
                transition=IssueTransitions.fixed,
                invoice=self._invoice, promocode=pc,
                connected=True,
                promocodes_states=[(pc, start_dt, pc.end_dt)],
                reservations_states=[(pcr, start_dt, pcr.end_dt)]
        )
        return res


class MultipleClientReservationsCase(AbstractResTestCase):
    _representation = 'multiple_clients_reservations'

    def __init__(self):
        self._invoice = None
        self._promocode = None
        self._reservations = None

    def _get_data(self, session):
        client, person = db_utils.create_client_person(session)
        order = db_utils.create_order(session, client=client)
        self._invoice = db_utils.create_invoice(session, client, [(order, 10)], person=person)
        self._invoice.create_receipt(10 * 30)

        pc_start_dt, pc_end_dt = self._get_interval(-100, 200)
        self._promocode = db_utils.create_promocode(session, pc_start_dt, pc_end_dt, 30, 10)

        start_dt, end_dt = self._get_interval(-15, 30)
        reservation = db_utils.create_promocode_reservation(session, client, self._promocode, start_dt, end_dt)

        alien_client = db_utils.create_client(session)
        alien_start_dt, alien_end_dt = self._get_interval(-45, 30)
        alien_reservation = db_utils.create_promocode_reservation(session, alien_client, self._promocode,
                                                                  alien_start_dt, alien_end_dt)

        self._reservations = [reservation, alien_reservation]

        return {
            'identifier': self._invoice.external_id,
            'promocode': self._promocode
        }

    def get_result(self):
        cliend_ids = ', '.join(str(pcr.client_id) for pcr in self._reservations)

        res = RequiredResult(
                transition=IssueTransitions.wont_fix,
                invoice=self._invoice, promocode=self._promocode,
                reservations_states=[(pcr, ) for pcr in self._reservations]
        )
        txt = 'По промокоду несколько резерваций у разных клиентов: %s.' \
              ' Автоматически зачислить промокод невозможно.' % cliend_ids
        res.add_message(txt)
        return res


class OtherClientReservationsCase(AbstractResTestCase):
    _representation = 'other_client_reservation'

    def __init__(self):
        self._invoice = None
        self._reservation = None

    def _get_data(self, session):
        client, person = db_utils.create_client_person(session)
        order = db_utils.create_order(session, client=client)
        self._invoice = db_utils.create_invoice(session, client, [(order, 10)], person=person)
        self._invoice.create_receipt(10 * 30)

        start_dt, end_dt = self._get_interval(-15, 30)
        alien_client = db_utils.create_client(session)
        self._reservation = db_utils.create_promocode_reservation(session, alien_client,
                                                                  None, start_dt, end_dt, 30, 10)

        return {
            'identifier': self._invoice.external_id,
            'promocode': self._reservation.promocode
        }

    def get_result(self):
        pcr = self._reservation
        pc = pcr.promocode
        cl = pcr.client

        res = RequiredResult(transition=IssueTransitions.wont_fix, invoice=self._invoice, reservation=self._reservation)
        txt = 'Промокод %s зарезервирован другим пользователем %s (ID %s).' % (pc.code, cl.name, cl.id)
        res.add_message(txt)
        return res


class NoReservationsCase(AbstractResTestCase):
    _representation = 'no_reservation'

    def __init__(self):
        self._invoice = None
        self._promocode = None

    def _get_data(self, session):
        client, person = db_utils.create_client_person(session)
        order = db_utils.create_order(session, client=client)
        self._invoice = db_utils.create_invoice(session, client, [(order, 10)], person=person)
        self._invoice.create_receipt(10 * 30)

        start_dt, end_dt = self._get_interval(-15, 30)
        self._promocode = db_utils.create_promocode(session, start_dt, end_dt, 30, 10)

        return {
            'identifier': self._invoice.external_id,
            'promocode': self._promocode
        }

    def get_result(self):
        res = RequiredResult(
                transition=IssueTransitions.fixed,
                invoice=self._invoice, promocode=self._promocode,
                connected=True
        )
        return res


class NewAndOldReservationsCase(AbstractResTestCase):
    _representation = 'new_and_old_reservations'

    def __init__(self):
        self._invoice = None
        self._reservation = None
        self._old_reservation = None

    def _get_data(self, session):
        client, person = db_utils.create_client_person(session)
        order = db_utils.create_order(session, client=client)
        self._invoice = db_utils.create_invoice(session, client, [(order, 10)], person=person)
        self._invoice.create_receipt(10 * 30)

        pc_start_dt, pc_end_dt = self._get_interval(-100, 200)
        pc = db_utils.create_promocode(session, pc_start_dt, pc_end_dt, 30, 10)

        start_dt, end_dt = self._get_interval(-20, 30)
        self._reservation = db_utils.create_promocode_reservation(session, client, pc, start_dt, end_dt)

        start_dt, end_dt = self._get_interval(-50, 30)
        self._old_reservation = db_utils.create_promocode_reservation(session, client, pc, start_dt, end_dt)

        return {
            'identifier': self._invoice.external_id,
            'promocode': pc
        }

    def get_result(self):
        res = RequiredResult(
                transition=IssueTransitions.fixed,
                invoice=self._invoice, reservation=self._reservation,
                connected=True,
                reservations_states=[(self._reservation, ), (self._old_reservation, )]
        )
        return res


class MultipleOldReservationsCase(AbstractResTestCase):
    _representation = 'multiple_old_reservations'

    def __init__(self):
        self._invoice = None
        self._reservation = None
        self._old_reservation = None

    def _get_data(self, session):
        client, person = db_utils.create_client_person(session)
        order = db_utils.create_order(session, client=client)
        self._invoice = db_utils.create_invoice(session, client, [(order, 10)], person=person)
        self._invoice.create_receipt(10 * 30)

        pc_start_dt, pc_end_dt = self._get_interval(-100, 200)
        pc = db_utils.create_promocode(session, pc_start_dt, pc_end_dt, 30, 10)

        start_dt, end_dt = self._get_interval(-45, 30)
        self._reservation = db_utils.create_promocode_reservation(session, client, pc, start_dt, end_dt)

        start_dt, end_dt = self._get_interval(-50, 30)
        self._old_reservation = db_utils.create_promocode_reservation(session, client, pc, start_dt, end_dt)

        return {
            'identifier': self._invoice.external_id,
            'promocode': pc
        }

    def get_result(self):
        tomorrow = ut.trunc_date(dt.datetime.now()) + dt.timedelta(1)
        res = RequiredResult(
                transition=IssueTransitions.fixed,
                invoice=self._invoice, reservation=self._reservation,
                connected=True,
                reservations_states=[
                    (self._reservation, self._reservation.begin_dt, tomorrow),
                    (self._old_reservation, )
                ])
        return res


class OtherEmptyInvoiceCase(AbstractResTestCase):
    _representation = 'other_invoice_empty'

    def __init__(self):
        self._invoice = None
        self._other_invoice = None
        self._reservation = None

    def _get_data(self, session):
        client, person = db_utils.create_client_person(session)
        order = db_utils.create_order(session, client=client)
        self._invoice = db_utils.create_invoice(session, client, [(order, 10)], person=person)
        self._invoice.create_receipt(10 * 30)

        start_dt, end_dt = self._get_interval(-15)
        self._reservation = db_utils.create_promocode_reservation(session, client, None,
                                                                  start_dt, end_dt, 30, 10)

        self._other_invoice = db_utils.create_invoice(session, client, [(order, 10)], person=person,
                                                      promo_code_id=self._reservation.promocode_id)
        self._other_invoice.create_receipt(10 * 30)

        return {
            'identifier': self._invoice.external_id,
            'promocode': self._reservation.promocode
        }

    def get_result(self):
        res = RequiredResult(
                transition=IssueTransitions.fixed,
                invoice=self._invoice, reservation=self._reservation, connected=True,
                unchanged_invoices=[
                    (self._other_invoice, )
                ])
        return res


class OtherConsumedInvoiceCase(AbstractResTestCase):
    _representation = 'other_invoice_consumed'

    def __init__(self):
        self._invoice = None
        self._other_invoice = None
        self._reservation = None

    def _get_data(self, session):
        client, person = db_utils.create_client_person(session)
        order = db_utils.create_order(session, client=client)
        self._invoice = db_utils.create_invoice(session, client, [(order, 10)], person=person)
        self._invoice.create_receipt(10 * 30)

        self._other_invoice = db_utils.create_invoice(session, client, [(order, 10)],
                                                      person=person, turn_on=True)

        start_dt, end_dt = self._get_interval(-15)
        self._reservation = db_utils.create_promocode_reservation(session, client, None,
                                                                  start_dt, end_dt, 30, 10)
        self._other_invoice.promo_code = self._reservation.promocode

        return {
            'identifier': self._invoice.external_id,
            'promocode': self._reservation.promocode
        }

    def get_result(self):
        res = RequiredResult(
                transition=IssueTransitions.fixed,
                invoice=self._invoice, reservation=self._reservation, connected=True,
                unchanged_invoices=[
                    (self._other_invoice, )
                ])
        return res


class OtherDiscountedInvoiceCase(AbstractResTestCase):
    _representation = 'other_invoice_discounted'

    def __init__(self):
        self._invoice = None
        self._other_invoice = None
        self._reservation = None

    def _get_data(self, session):
        client, person = db_utils.create_client_person(session)
        order = db_utils.create_order(session, client=client)
        self._invoice = db_utils.create_invoice(session, client, [(order, 10)], person=person)
        self._invoice.create_receipt(10 * 30)

        start_dt, end_dt = self._get_interval(-15)
        self._reservation = db_utils.create_promocode_reservation(session, client, None,
                                                                  start_dt, end_dt, 30, 10)

        self._other_invoice = db_utils.create_invoice(session, client, [(order, 10)], person=person,
                                                      promo_code_id=self._reservation.promocode_id)
        self._other_invoice.create_receipt(10 * 30)
        self._other_invoice.turn_on_rows(apply_promocode=True)

        return {
            'identifier': self._invoice.external_id,
            'promocode': self._reservation.promocode
        }

    def get_result(self):
        pc = self._reservation.promocode
        res = RequiredResult(
                transition=IssueTransitions.wont_fix,
                reservation=self._reservation,
                connected_invoices=[(self._other_invoice, )],
                unchanged_invoices=[(self._invoice, )]
        )
        res.add_message('Промокод %s уже активирован в счете %s.' % (pc.code, self._other_invoice.external_id))
        return res


class OtherDiscountedPaCase(AbstractResTestCase):
    _representation = 'other_pa_discounted'

    def __init__(self):
        self._invoice = None
        self._reservation = None

    def _get_data(self, session):
        client, person = db_utils.create_client_person(session)
        order = db_utils.create_order(session, client=client)
        contract = db_utils.create_general_contract(session, client, person, personal_account=1)
        self._other_invoice = db_utils.create_personal_account(session, contract=contract, personal_account_fictive=0)
        start_dt, end_dt = self._get_interval(-15)
        pc = db_utils.create_promocode(
            session, start_dt, end_dt, bonus=30, min_qty=5, calc_class_name='FixedSumBonusPromoCodeGroup'
        )
        self._reservation = db_utils.create_promocode_reservation(session, client, pc, start_dt, end_dt)
        self._other_charge_note = db_utils.create_charge_note(
            session, self._other_invoice, [(order, 10)], promo_code_id=self._reservation.promocode.id
        )
        self._other_charge_note = BetterChargeNote(self._other_charge_note)
        op = mapper.Operation(2, invoice=self._other_charge_note)
        self._other_invoice.create_receipt(self._other_charge_note.effective_sum)
        self._other_charge_note.on_turn_on(op, self._other_charge_note.effective_sum)

        self._invoice = db_utils.create_invoice(session, client, [(order, 10)], person=person)
        self._invoice.create_receipt(10 * 30)

        return {
            'identifier': self._invoice.external_id,
            'promocode': self._reservation.promocode
        }

    def get_result(self):
        pc = self._reservation.promocode
        res = RequiredResult(
                transition=IssueTransitions.wont_fix,
                reservation=self._reservation,
                connected_invoices=[
                    (self._other_charge_note, 300, 10 + self._other_charge_note.tax_policy_pct.add_to(30))
                ],
                unchanged_invoices=[(self._invoice, )]
        )
        res.add_message('Промокод %s уже активирован в счете %s.' % (pc.code, self._other_invoice.external_id))
        return res


class OtherDiscountedPaDiscountCase(AbstractResTestCase):
    _representation = 'other_pa_discounted_discount'

    def __init__(self):
        self._invoice = None
        self._reservation = None

    def _get_data(self, session):
        client, person = db_utils.create_client_person(session)
        order = db_utils.create_order(session, client=client)
        contract = db_utils.create_general_contract(session, client, person, personal_account=1)
        self._other_invoice = db_utils.create_personal_account(session, contract=contract, personal_account_fictive=0)
        start_dt, end_dt = self._get_interval(-15)
        pc = db_utils.create_promocode(
            session, start_dt, end_dt, discount_pct=25, calc_class_name='FixedDiscountPromoCodeGroup',
            apply_on_create=True
        )
        self._reservation = db_utils.create_promocode_reservation(session, client, pc, start_dt, end_dt)
        self._other_charge_note = db_utils.create_charge_note(
            session, self._other_invoice, [(order, 10)], promo_code_id=self._reservation.promocode.id
        )
        self._other_charge_note = BetterChargeNote(self._other_charge_note)
        op = mapper.Operation(2, invoice=self._other_charge_note)
        self._other_invoice.create_receipt(self._other_charge_note.effective_sum)
        self._other_charge_note.on_turn_on(op, self._other_charge_note.effective_sum)

        self._invoice = db_utils.create_invoice(session, client, [(order, 10)], person=person)
        self._invoice.create_receipt(10 * 30)

        return {
            'identifier': self._invoice.external_id,
            'promocode': self._reservation.promocode
        }

    def get_result(self):
        pc = self._reservation.promocode
        res = RequiredResult(
                transition=IssueTransitions.wont_fix,
                reservation=self._reservation,
                connected_invoices=[
                    (self._other_charge_note, 225, 10)
                ],
                unchanged_invoices=[(self._invoice, )]
        )
        res.add_message('Промокод %s уже активирован в счете %s.' % (pc.code, self._other_invoice.external_id))
        return res


class OtherTornOffPaCase(AbstractResTestCase):
    _representation = 'other_pa_torn_off'

    def __init__(self):
        self._invoice = None
        self._reservation = None

    def _get_data(self, session):
        client, person = db_utils.create_client_person(session)
        order = db_utils.create_order(session, client=client)
        contract = db_utils.create_general_contract(session, client, person, personal_account=1)
        self._other_invoice = db_utils.create_personal_account(session, contract=contract, personal_account_fictive=0)
        start_dt, end_dt = self._get_interval(-15)
        pc = db_utils.create_promocode(
            session, start_dt, end_dt, bonus=30, min_qty=5, calc_class_name='FixedSumBonusPromoCodeGroup'
        )
        self._reservation = db_utils.create_promocode_reservation(session, client, pc, start_dt, end_dt)
        self._other_charge_note = db_utils.create_charge_note(
            session, self._other_invoice, [(order, 10)], promo_code_id=self._reservation.promocode.id
        )
        self._other_charge_note = BetterChargeNote(self._other_charge_note)
        op = mapper.Operation(2, invoice=self._other_charge_note)
        self._other_invoice.create_receipt(self._other_charge_note.effective_sum)
        self._other_charge_note.on_turn_on(op, self._other_charge_note.effective_sum)
        tear_promocode_off(session, self._other_invoice)

        self._invoice = db_utils.create_invoice(session, client, [(order, 10)], person=person)
        self._invoice.create_receipt(10 * 30)

        return {
            'identifier': self._invoice.external_id,
            'promocode': self._reservation.promocode
        }

    def get_result(self):
        res = RequiredResult(
            transition=IssueTransitions.fixed,
            invoice=self._invoice, reservation=self._reservation,
            connected_invoices=[
                    (self._invoice, 300, 10 + self._invoice.tax_policy_pct.add_to(30))
            ],
            connected=True,
        )
        return res


class OtherTornOffPaDiscountCase(AbstractResTestCase):
    _representation = 'other_pa_torn_off_discount'

    def __init__(self):
        self._invoice = None
        self._reservation = None

    def _get_data(self, session):
        client, person = db_utils.create_client_person(session)
        order = db_utils.create_order(session, client=client)
        contract = db_utils.create_general_contract(session, client, person, personal_account=1)
        self._other_invoice = db_utils.create_personal_account(session, contract=contract, personal_account_fictive=0)
        start_dt, end_dt = self._get_interval(-15)
        pc = db_utils.create_promocode(
            session, start_dt, end_dt, discount_pct=25, calc_class_name='FixedDiscountPromoCodeGroup'
        )
        self._reservation = db_utils.create_promocode_reservation(session, client, pc, start_dt, end_dt)
        self._other_charge_note = db_utils.create_charge_note(
            session, self._other_invoice, [(order, 10)], promo_code_id=self._reservation.promocode.id
        )
        self._other_charge_note = BetterChargeNote(self._other_charge_note)
        op = mapper.Operation(2, invoice=self._other_charge_note)
        self._other_invoice.create_receipt(self._other_charge_note.effective_sum)
        self._other_charge_note.on_turn_on(op, self._other_charge_note.effective_sum)
        tear_promocode_off(session, self._other_invoice)

        self._invoice = db_utils.create_invoice(session, client, [(order, 10)], person=person)
        self._invoice.create_receipt(10 * 30)

        return {
            'identifier': self._invoice.external_id,
            'promocode': self._reservation.promocode
        }

    def get_result(self):
        res = RequiredResult(
            transition=IssueTransitions.fixed,
            invoice=self._invoice, reservation=self._reservation,
            connected_invoices=[
                    (self._invoice, 300, ut.radd_percent(10, -25))
            ],
            connected=True,
        )
        return res


class NotActivatedCase(AbstractResTestCase):
    _representation = 'not_activated'

    def __init__(self):
        self._invoice = None
        self._reservation = None

    def _get_data(self, session):
        client, person = db_utils.create_client_person(session)
        order = db_utils.create_order(session, client=client)
        self._invoice = db_utils.create_invoice(session, client, [(order, 10)], person=person)
        self._invoice.create_receipt(9 * 30)

        start_dt, end_dt = self._get_interval(-15)
        self._reservation = db_utils.create_promocode_reservation(session, client, None,
                                                                  start_dt, end_dt, 30, 5)

        return {
            'identifier': self._invoice.external_id,
            'promocode': self._reservation.promocode
        }

    def get_result(self):
        res = RequiredResult(transition=IssueTransitions.opened,
                             invoice=self._invoice,
                             reservation=self._reservation
                             )
        txt = (
                  'Применить данный промокод к счету %s не получилось. '
                  'Тикет посмотрим в ручном режиме.'
              ) % self._invoice.external_id
        res.add_message(txt)
        return res


class MultiplePromocodesCase(AbstractResTestCase):
    _representation = 'multiple_promocodes'

    def __init__(self):
        self._invoice = None
        self._reservation = None
        self._other_reservation = None

    def _get_data(self, session):
        client, person = db_utils.create_client_person(session)
        order = db_utils.create_order(session, client=client)
        self._invoice = db_utils.create_invoice(session, client, [(order, 10)], person=person)
        self._invoice.create_receipt(10 * 30)

        start_dt, end_dt = self._get_interval(-20, 30)
        pc = db_utils.create_promocode(session, start_dt, end_dt, 30, 5)
        self._other_reservation = db_utils.create_promocode_reservation(session, client, pc, start_dt, end_dt)

        start_dt, end_dt = self._get_interval(-20, 40)
        pc = db_utils.create_promocode(session, start_dt, end_dt, 30, 5, code=pc.code + '1')
        self._reservation = db_utils.create_promocode_reservation(session, client, pc, start_dt, end_dt)

        return {
            'identifier': self._invoice.external_id,
            'promocode': self._reservation.promocode
        }

    def get_result(self):
        res = RequiredResult(
                transition=IssueTransitions.fixed,
                invoice=self._invoice, reservation=self._reservation,
                connected=True,
                reservations_states=[(self._reservation, ), (self._other_reservation, )]
        )
        return res


class StandardCheckFailCase(AbstractResTestCase):
    _representation = 'standard_check_fail'

    def __init__(self):
        self._invoice = None
        self._reservation = None

    def _get_data(self, session):
        client, person = db_utils.create_client_person(session, is_agency=1)
        order = db_utils.create_order(session, client=client)
        self._invoice = db_utils.create_invoice(session, client, [(order, 10)], person=person)
        self._invoice.create_receipt(self._invoice.effective_sum)

        start_dt, end_dt = self._get_interval(-15)
        self._reservation = db_utils.create_promocode_reservation(session, client, None,
                                                                  start_dt, end_dt, 30, 10)

        return {
            'identifier': self._invoice.external_id,
            'promocode': self._reservation.promocode
        }

    def get_result(self):
        res = RequiredResult(
                transition=IssueTransitions.wont_fix,
                invoice=self._invoice, reservation=self._reservation
        )
        res.add_message('Промокод не прошёл проверку: ID_PC_NON_DIRECT_CLIENT.')
        return res


class AlreadySolvedCase(AbstractResTestCase):
    _representation = 'already_solved'

    last_resolved = dt.datetime.now() - dt.timedelta(hours=1)

    def __init__(self):
        self._invoice = None
        self._reservation = None

    def _get_data(self, session):
        client, person = db_utils.create_client_person(session)
        order = db_utils.create_order(session, client=client)
        self._invoice = db_utils.create_invoice(session, client, [(order, 10)], person=person)
        self._invoice.create_receipt(self._invoice.effective_sum)

        start_dt, end_dt = self._get_interval(-15)
        self._reservation = db_utils.create_promocode_reservation(session, client, None,
                                                                  start_dt, end_dt, 30, 10)

        return {
            'identifier': self._invoice.external_id,
            'promocode': self._reservation.promocode
        }

    def get_result(self):
        res = RequiredResult(
                transition=IssueTransitions.wont_fix,
                invoice=self._invoice,
                reservation=self._reservation,
                commit=False
        )
        res.add_message(
            'Эта задача уже была выполнена. Направьте новый запрос через '
            '((https://wiki.yandex-team.ru/otdelsoprovozhdeniya/dokument/avtomatformi/ формы)). '
            'Если не найдёте подходящую, заполните, пожалуйста, общую форму.'
        )
        return res


class ForbiddenInvoiceTypeCase(AbstractResTestCase):
    _representation = 'forbidden_invoice_type'

    def __init__(self):
        self._invoice = None
        self._reservation = None

    def _get_data(self, session):
        client, person = db_utils.create_client_person(session)
        order = db_utils.create_order(session, client=client)
        self._invoice = db_utils.create_invoice(
            session, client, [(order, 10)], person=person, overdraft=1
        )

        start_dt, end_dt = self._get_interval(-15, 30)
        self._reservation = db_utils.create_promocode_reservation(
            session, client, None, start_dt, end_dt, 30, 10
        )

        return {
            'identifier': self._invoice.external_id,
            'promocode': self._reservation.promocode
        }

    def get_result(self):
        res = RequiredResult(
            transition=IssueTransitions.wont_fix,
            invoice=self._invoice,
            reservation=self._reservation
        )
        res.add_message('К счету с типом overdraft невозможно применить промокод.')
        return res


class EmptyPaCase(AbstractResTestCase):
    _representation = 'empty_pa_case'

    def __init__(self):
        self._invoice = None
        self._reservation = None

    def _get_data(self, session):
        client, person = db_utils.create_client_person(session)
        order = db_utils.create_order(session, client=client)
        contract = db_utils.create_general_contract(
            session, client, person, personal_account=1
        )
        pa = db_utils.create_personal_account(
            session, contract=contract, personal_account_fictive=0
        )

        self._charge_note = db_utils.create_charge_note(
            session, pa, [(order, 10)]
        )
        self._charge_note = BetterChargeNote(self._charge_note)

        op = mapper.Operation(2, invoice=self._charge_note)
        self._charge_note.on_turn_on(op, self._charge_note.effective_sum)

        pa.create_receipt(self._charge_note.effective_sum)

        start_dt, end_dt = self._get_interval(-15)
        pc = db_utils.create_promocode(
            session, start_dt, end_dt, bonus=30, min_qty=5, calc_class_name='FixedSumBonusPromoCodeGroup'
        )
        self._reservation = db_utils.create_promocode_reservation(
            session, client, pc, start_dt, end_dt
        )

        return {
            'identifier': self._charge_note._external_id,
            'promocode': self._reservation.promocode
        }

    def get_result(self):
        res = RequiredResult(
                transition=IssueTransitions.fixed,
                invoice=self._charge_note, reservation=self._reservation,
                connected_invoices=[(self._charge_note, 300, 10 + self._charge_note.tax_policy_pct.add_to(30))],
                connected=True
        )
        return res


class EmptyPaDiscountCase(AbstractResTestCase):
    _representation = 'empty_pa_discount_case'

    def __init__(self):
        self._invoice = None
        self._reservation = None

    def _get_data(self, session):
        client, person = db_utils.create_client_person(session)
        order = db_utils.create_order(session, client=client)
        contract = db_utils.create_general_contract(
            session, client, person, personal_account=1
        )
        pa = db_utils.create_personal_account(
            session, contract=contract, personal_account_fictive=0
        )

        self._charge_note = db_utils.create_charge_note(
            session, pa, [(order, 10)]
        )
        self._charge_note = BetterChargeNote(self._charge_note)

        op = mapper.Operation(2, invoice=self._charge_note)
        self._charge_note.on_turn_on(op, self._charge_note.effective_sum)

        pa.create_receipt(self._charge_note.effective_sum)

        start_dt, end_dt = self._get_interval(-15)
        pc = db_utils.create_promocode(
            session, start_dt, end_dt, discount_pct=15, calc_class_name='FixedDiscountPromoCodeGroup'
        )
        self._reservation = db_utils.create_promocode_reservation(
            session, client, pc, start_dt, end_dt
        )

        return {
            'identifier': self._charge_note._external_id,
            'promocode': self._reservation.promocode
        }

    def get_result(self):
        res = RequiredResult(
                transition=IssueTransitions.fixed,
                invoice=self._charge_note, reservation=self._reservation,
                connected_invoices=[(self._charge_note, 300, ut.radd_percent(10, -15))],
                connected=True
        )
        return res


class PaNoFreeConsumesCase(AbstractResTestCase):
    _representation = 'pa_no_free_consumes'

    def __init__(self):
        self._invoice = None
        self._reservation = None

    def _get_data(self, session):
        client, person = db_utils.create_client_person(session)
        order = db_utils.create_order(session, client=client)
        contract = db_utils.create_general_contract(
            session, client, person, personal_account=1
        )
        pa = db_utils.create_personal_account(
            session, contract=contract, personal_account_fictive=0
        )

        self._charge_note = db_utils.create_charge_note(
            session, pa, [(order, 10)]
        )
        self._charge_note = BetterChargeNote(self._charge_note)

        op = mapper.Operation(2, invoice=self._charge_note)
        self._charge_note.on_turn_on(op, self._charge_note.effective_sum)

        pa.create_receipt(self._charge_note.effective_sum)
        pa.close_invoice(self._charge_note.dt)

        start_dt, end_dt = self._get_interval(-15)
        pc = db_utils.create_promocode(
            session, start_dt, end_dt, bonus=30, min_qty=5, calc_class_name='FixedSumBonusPromoCodeGroup'
        )
        self._reservation = db_utils.create_promocode_reservation(
            session, client, pc, start_dt, end_dt
        )

        return {
            'identifier': self._charge_note._external_id,
            'promocode': self._reservation.promocode
        }

    def get_result(self):
        res = RequiredResult(
                transition=IssueTransitions.wont_fix,
                invoice=self._charge_note, reservation=self._reservation,
        )
        txt = 'Отсутствуют непотраченные средства для перерасчета скидки. '\
              'Промокод активировать невозможно. Клиент может применить его при новой оплате.'
        res.add_message(txt)
        return res


class PaNoFreeConsumesDiscountApikeysCase(AbstractResTestCase):
    _representation = 'pa_no_free_consumes_discount_apikeys'

    def __init__(self):
        self._invoice = None
        self._reservation = None

    def _get_data(self, session):
        client, person = db_utils.create_client_person(session)
        order = db_utils.create_order(session, client=client, service_id=642)
        contract = db_utils.create_general_contract(
            session, client, person, personal_account=1
        )
        pa = db_utils.create_personal_account(
            session, contract=contract, personal_account_fictive=0
        )

        self._charge_note = db_utils.create_charge_note(
            session, pa, [(order, 10)]
        )
        self._charge_note = BetterChargeNote(self._charge_note)

        op = mapper.Operation(2, invoice=self._charge_note)
        self._charge_note.on_turn_on(op, self._charge_note.effective_sum)

        pa.create_receipt(self._charge_note.effective_sum)
        pa.close_invoice(self._charge_note.dt)

        start_dt, end_dt = self._get_interval(-15)
        pc = db_utils.create_promocode(
            session, start_dt, end_dt, discount_pct=15, calc_class_name='FixedDiscountPromoCodeGroup'
        )
        self._reservation = db_utils.create_promocode_reservation(
            session, client, pc, start_dt, end_dt
        )

        return {
            'identifier': self._charge_note._external_id,
            'promocode': self._reservation.promocode
        }

    def get_result(self):
        res = RequiredResult(
                transition=IssueTransitions.wont_fix,
                invoice=self._charge_note, reservation=self._reservation,
        )
        txt = 'Отсутствуют непотраченные средства для перерасчета скидки. ' \
              'Промокод активировать невозможно. Клиент может применить его при новой оплате.'
        res.add_message(txt)
        return res


class PaNoFreeConsumesDiscountApikeys1Case(AbstractResTestCase):
    _representation = 'pa_no_free_consumes_discount_apikeys1'

    def __init__(self):
        self._invoice = None
        self._reservation = None

    def _get_data(self, session):
        client, person = db_utils.create_client_person(session)
        order = db_utils.create_order(session, client=client, service_id=642)
        contract = db_utils.create_general_contract(
            session, client, person, personal_account=1
        )
        pa = db_utils.create_personal_account(
            session, contract=contract, personal_account_fictive=0
        )

        self._charge_note = db_utils.create_charge_note(
            session, pa, [(order, 10)]
        )
        self._charge_note = BetterChargeNote(self._charge_note)

        pa.create_receipt(self._charge_note.effective_sum + 30)

        start_dt, end_dt = self._get_interval(-15)
        pc = db_utils.create_promocode(
            session, start_dt, end_dt, discount_pct=15, calc_class_name='FixedDiscountPromoCodeGroup'
        )
        self._reservation = db_utils.create_promocode_reservation(
            session, client, pc, start_dt, end_dt
        )

        return {
            'identifier': self._charge_note._external_id,
            'promocode': self._reservation.promocode
        }

    def get_result(self):
        res = RequiredResult(
            transition=IssueTransitions.fixed,
            invoice=self._charge_note, reservation=self._reservation,
            connected_invoices=[(self._charge_note, 300 + 30, 1 + ut.radd_percent(10, -15))],
            connected=True
        )
        return res


class PaNoFreeConsumesDiscountApikeys2Case(AbstractResTestCase):
    _representation = 'pa_no_free_consumes_discount_apikeys2'

    def __init__(self):
        self._invoice = None
        self._reservation = None

    def _get_data(self, session):
        client, person = db_utils.create_client_person(session)
        order = db_utils.create_order(session, client=client, service_id=642)
        contract = db_utils.create_general_contract(
            session, client, person, personal_account=1
        )
        pa = db_utils.create_personal_account(
            session, contract=contract, personal_account_fictive=0
        )

        self._charge_note = db_utils.create_charge_note(
            session, pa, [(order, 10)]
        )
        self._charge_note = BetterChargeNote(self._charge_note)

        op = mapper.Operation(2, invoice=self._charge_note)
        self._charge_note.on_turn_on(op, self._charge_note.effective_sum)

        pa.create_receipt(self._charge_note.effective_sum + 30)

        order.do_process_completion(5)
        pa.generate_act(force=1)

        start_dt, end_dt = self._get_interval(-15)
        pc = db_utils.create_promocode(
            session, start_dt, end_dt, discount_pct=15, calc_class_name='FixedDiscountPromoCodeGroup'
        )
        self._reservation = db_utils.create_promocode_reservation(
            session, client, pc, start_dt, end_dt
        )

        return {
            'identifier': self._charge_note._external_id,
            'promocode': self._reservation.promocode
        }

    def get_result(self):
        res = RequiredResult(
            transition=IssueTransitions.fixed,
            invoice=self._charge_note, reservation=self._reservation,
            connected_invoices=[(self._charge_note, 300, ut.radd_percent(10, -15))],
            connected=True
        )
        return res


class PaNoFreeConsumesDiscountApikeys3Case(AbstractResTestCase):
    _representation = 'pa_no_free_consumes_discount_apikeys3'

    def __init__(self):
        self._invoice = None
        self._reservation = None

    def _get_data(self, session):
        client, person = db_utils.create_client_person(session)
        order = db_utils.create_order(session, client=client, service_id=642)
        contract = db_utils.create_general_contract(
            session, client, person, personal_account=1
        )
        pa = db_utils.create_personal_account(
            session, contract=contract, personal_account_fictive=0
        )

        self._charge_note = db_utils.create_charge_note(
            session, pa, [(order, 10)]
        )
        self._charge_note = BetterChargeNote(self._charge_note)

        op = mapper.Operation(2, invoice=self._charge_note)
        self._charge_note.on_turn_on(op, self._charge_note.effective_sum)

        pa.create_receipt(self._charge_note.effective_sum + 30)

        order.do_process_completion(10)
        pa.generate_act(force=1)

        start_dt, end_dt = self._get_interval(-15)
        pc = db_utils.create_promocode(
            session, start_dt, end_dt, discount_pct=15, calc_class_name='FixedDiscountPromoCodeGroup'
        )
        self._reservation = db_utils.create_promocode_reservation(
            session, client, pc, start_dt, end_dt
        )

        return {
            'identifier': self._charge_note._external_id,
            'promocode': self._reservation.promocode
        }

    def get_result(self):
        res = RequiredResult(
            transition=IssueTransitions.fixed,
            invoice=self._charge_note, reservation=self._reservation,
            connected_invoices=[(self._charge_note, 330, 1 + ut.radd_percent(10, -15))],
            connected=True
        )
        return res


class PaNoFreeConsumesDiscountApikeysCase(AbstractResTestCase):
    _representation = 'pa_no_free_consumes_apikeys'

    def __init__(self):
        self._invoice = None
        self._reservation = None

    def _get_data(self, session):
        client, person = db_utils.create_client_person(session)
        order = db_utils.create_order(session, client=client, service_id=642)
        contract = db_utils.create_general_contract(
            session, client, person, personal_account=1
        )
        pa = db_utils.create_personal_account(
            session, contract=contract, personal_account_fictive=0
        )

        self._charge_note = db_utils.create_charge_note(
            session, pa, [(order, 10)]
        )
        self._charge_note = BetterChargeNote(self._charge_note)

        op = mapper.Operation(2, invoice=self._charge_note)
        self._charge_note.on_turn_on(op, self._charge_note.effective_sum)

        pa.create_receipt(self._charge_note.effective_sum)
        pa.close_invoice(self._charge_note.dt)

        start_dt, end_dt = self._get_interval(-15)
        pc = db_utils.create_promocode(
            session, start_dt, end_dt, bonus=30, min_qty=5, calc_class_name='FixedSumBonusPromoCodeGroup'
        )
        self._reservation = db_utils.create_promocode_reservation(
            session, client, pc, start_dt, end_dt
        )

        return {
            'identifier': self._charge_note._external_id,
            'promocode': self._reservation.promocode
        }

    def get_result(self):
        res = RequiredResult(
                transition=IssueTransitions.wont_fix,
                invoice=self._charge_note, reservation=self._reservation,
        )
        txt = 'Отсутствуют непотраченные средства для перерасчета скидки. ' \
              'Промокод активировать невозможно. Клиент может применить его при новой оплате.'
        res.add_message(txt)
        return res


class PaNoFreeConsumesApikeys1Case(AbstractResTestCase):
    _representation = 'pa_no_free_consumes_apikeys1'

    def __init__(self):
        self._invoice = None
        self._reservation = None

    def _get_data(self, session):
        client, person = db_utils.create_client_person(session)
        order = db_utils.create_order(session, client=client, service_id=642)
        contract = db_utils.create_general_contract(
            session, client, person, personal_account=1
        )
        pa = db_utils.create_personal_account(
            session, contract=contract, personal_account_fictive=0
        )

        self._charge_note = db_utils.create_charge_note(
            session, pa, [(order, 10)]
        )
        self._charge_note = BetterChargeNote(self._charge_note)

        pa.create_receipt(self._charge_note.effective_sum + 30)

        start_dt, end_dt = self._get_interval(-15)
        pc = db_utils.create_promocode(
            session, start_dt, end_dt, bonus=30, min_qty=5, calc_class_name='FixedSumBonusPromoCodeGroup'
        )
        self._reservation = db_utils.create_promocode_reservation(
            session, client, pc, start_dt, end_dt
        )

        return {
            'identifier': self._charge_note._external_id,
            'promocode': self._reservation.promocode
        }

    def get_result(self):
        res = RequiredResult(
            transition=IssueTransitions.fixed,
            invoice=self._charge_note, reservation=self._reservation,
            connected_invoices=[(self._charge_note, 300 + 30, 10 + 1 + self._charge_note.tax_policy_pct.add_to(30))],
            connected=True
        )
        return res


class PaNoFreeConsumesApikeys2Case(AbstractResTestCase):
    _representation = 'pa_no_free_consumes_apikeys2'

    def __init__(self):
        self._invoice = None
        self._reservation = None

    def _get_data(self, session):
        client, person = db_utils.create_client_person(session)
        order = db_utils.create_order(session, client=client, service_id=642)
        contract = db_utils.create_general_contract(
            session, client, person, personal_account=1
        )
        pa = db_utils.create_personal_account(
            session, contract=contract, personal_account_fictive=0
        )

        self._charge_note = db_utils.create_charge_note(
            session, pa, [(order, 10)]
        )
        self._charge_note = BetterChargeNote(self._charge_note)

        op = mapper.Operation(2, invoice=self._charge_note)
        self._charge_note.on_turn_on(op, self._charge_note.effective_sum)

        pa.create_receipt(self._charge_note.effective_sum + 30)

        order.do_process_completion(5)
        pa.generate_act(force=1)

        start_dt, end_dt = self._get_interval(-15)
        pc = db_utils.create_promocode(
            session, start_dt, end_dt, bonus=30, min_qty=5, calc_class_name='FixedSumBonusPromoCodeGroup'
        )
        self._reservation = db_utils.create_promocode_reservation(
            session, client, pc, start_dt, end_dt
        )

        return {
            'identifier': self._charge_note._external_id,
            'promocode': self._reservation.promocode
        }

    def get_result(self):
        res = RequiredResult(
            transition=IssueTransitions.fixed,
            invoice=self._charge_note, reservation=self._reservation,
            connected_invoices=[(self._charge_note, 300, 10 + self._charge_note.tax_policy_pct.add_to(30))],
            connected=True
        )
        return res


class PaNoFreeConsumesApikeys3Case(AbstractResTestCase):
    _representation = 'pa_no_free_consumes_apikeys3'

    def __init__(self):
        self._invoice = None
        self._reservation = None

    def _get_data(self, session):
        client, person = db_utils.create_client_person(session)
        order = db_utils.create_order(session, client=client, service_id=642)
        contract = db_utils.create_general_contract(
            session, client, person, personal_account=1
        )
        pa = db_utils.create_personal_account(
            session, contract=contract, personal_account_fictive=0
        )

        self._charge_note = db_utils.create_charge_note(
            session, pa, [(order, 10)]
        )
        self._charge_note = BetterChargeNote(self._charge_note)

        op = mapper.Operation(2, invoice=self._charge_note)
        self._charge_note.on_turn_on(op, self._charge_note.effective_sum)

        pa.create_receipt(self._charge_note.effective_sum + 30)

        order.do_process_completion(10)
        pa.generate_act(force=1)

        start_dt, end_dt = self._get_interval(-15)
        pc = db_utils.create_promocode(
            session, start_dt, end_dt, bonus=30, min_qty=5, calc_class_name='FixedSumBonusPromoCodeGroup'
        )
        self._reservation = db_utils.create_promocode_reservation(
            session, client, pc, start_dt, end_dt
        )

        return {
            'identifier': self._charge_note._external_id,
            'promocode': self._reservation.promocode
        }

    def get_result(self):
        res = RequiredResult(
            transition=IssueTransitions.fixed,
            invoice=self._charge_note, reservation=self._reservation,
            connected_invoices=[(self._charge_note, 300 + 30, 10 + 1 + self._charge_note.tax_policy_pct.add_to(30))],
            connected=True
        )
        return res


class PaNoFreeConsumesDiscountCase(AbstractResTestCase):
    _representation = 'pa_no_free_consumes_discount'

    def __init__(self):
        self._invoice = None
        self._reservation = None

    def _get_data(self, session):
        client, person = db_utils.create_client_person(session)
        order = db_utils.create_order(session, client=client)
        contract = db_utils.create_general_contract(
            session, client, person, personal_account=1
        )
        pa = db_utils.create_personal_account(
            session, contract=contract, personal_account_fictive=0
        )

        self._charge_note = db_utils.create_charge_note(
            session, pa, [(order, 10)]
        )
        self._charge_note = BetterChargeNote(self._charge_note)

        op = mapper.Operation(2, invoice=self._charge_note)
        self._charge_note.on_turn_on(op, self._charge_note.effective_sum)

        pa.create_receipt(self._charge_note.effective_sum)
        pa.close_invoice(self._charge_note.dt)

        start_dt, end_dt = self._get_interval(-15)
        pc = db_utils.create_promocode(
            session, start_dt, end_dt, discount_pct=10, calc_class_name='FixedDiscountPromoCodeGroup'
        )
        self._reservation = db_utils.create_promocode_reservation(
            session, client, pc, start_dt, end_dt
        )

        return {
            'identifier': self._charge_note._external_id,
            'promocode': self._reservation.promocode
        }

    def get_result(self):
        res = RequiredResult(
                transition=IssueTransitions.wont_fix,
                invoice=self._charge_note, reservation=self._reservation,
        )
        txt = 'Отсутствуют непотраченные средства для перерасчета скидки. ' \
              'Промокод активировать невозможно. Клиент может применить его при новой оплате.'
        res.add_message(txt)
        return res


class NotEnoughChargeNoteSumCase(AbstractResTestCase):
    _representation = 'not_enough_cn_sum'

    def __init__(self):
        self._invoice = None
        self._reservation = None

    def _get_data(self, session):
        client, person = db_utils.create_client_person(session)
        order = db_utils.create_order(session, client=client)
        contract = db_utils.create_general_contract(
            session, client, person, personal_account=1
        )
        pa = db_utils.create_personal_account(
            session, contract=contract, personal_account_fictive=0
        )

        self._charge_note = db_utils.create_charge_note(
            session, pa, [(order, 4)]
        )
        self._charge_note = BetterChargeNote(self._charge_note)

        op = mapper.Operation(2, invoice=self._charge_note)
        self._charge_note.on_turn_on(op, self._charge_note.effective_sum)

        pa.create_receipt(self._charge_note.effective_sum)

        start_dt, end_dt = self._get_interval(-15)
        pc = db_utils.create_promocode(
            session, start_dt, end_dt, bonus=30, min_qty=5, calc_class_name='FixedSumBonusPromoCodeGroup'
        )
        self._reservation = db_utils.create_promocode_reservation(
            session, client, pc, start_dt, end_dt
        )

        return {
            'identifier': self._charge_note._external_id,
            'promocode': self._reservation.promocode
        }

    def get_result(self):
        res = RequiredResult(
            transition=IssueTransitions.wont_fix,
            invoice=self._charge_note, reservation=self._reservation,
        )
        txt = 'Сумма счета меньше, чем нужно для активации промокода: 120.00 (нужно 180.00).'
        res.add_message(txt)
        return res


class NotEnoughChargeNoteSumDiscountCase(AbstractResTestCase):
    _representation = 'not_enough_cn_sum_discount'

    def __init__(self):
        self._invoice = None
        self._reservation = None

    def _get_data(self, session):
        client, person = db_utils.create_client_person(session)
        order = db_utils.create_order(session, client=client)
        contract = db_utils.create_general_contract(
            session, client, person, personal_account=1
        )
        pa = db_utils.create_personal_account(
            session, contract=contract, personal_account_fictive=0
        )

        self._charge_note = db_utils.create_charge_note(
            session, pa, [(order, 4)]
        )
        self._charge_note = BetterChargeNote(self._charge_note)

        op = mapper.Operation(2, invoice=self._charge_note)
        self._charge_note.on_turn_on(op, self._charge_note.effective_sum)

        pa.create_receipt(self._charge_note.effective_sum)

        start_dt, end_dt = self._get_interval(-15)
        pc = db_utils.create_promocode(
            session, start_dt, end_dt, discount_pct=10, calc_class_name='FixedDiscountPromoCodeGroup'
        )
        self._reservation = db_utils.create_promocode_reservation(
            session, client, pc, start_dt, end_dt
        )

        return {
            'identifier': self._charge_note._external_id,
            'promocode': self._reservation.promocode
        }

    def get_result(self):
        res = RequiredResult(
            transition=IssueTransitions.fixed,
            invoice=self._charge_note, reservation=self._reservation,
            connected_invoices=[(self._charge_note, 120, ut.radd_percent(4, -10))],
            connected=True
        )
        return res


class PaPartiallyActedCase(AbstractResTestCase):
    _representation = 'pa_partially_acted'

    def __init__(self):
        self._invoice = None
        self._reservation = None

    def _get_data(self, session):
        client, person = db_utils.create_client_person(session)
        order = db_utils.create_order(session, client=client)
        contract = db_utils.create_general_contract(
            session, client, person, personal_account=1
        )
        pa = db_utils.create_personal_account(
            session, contract=contract, personal_account_fictive=0
        )

        self._charge_note = db_utils.create_charge_note(
            session, pa, [(order, 10)]
        )
        self._charge_note = BetterChargeNote(self._charge_note)

        op = mapper.Operation(2, invoice=self._charge_note)
        self._charge_note.on_turn_on(op, self._charge_note.effective_sum)

        pa.create_receipt(self._charge_note.effective_sum)

        order.do_process_completion(5)
        pa.generate_act(force=1)

        start_dt, end_dt = self._get_interval(-15)
        pc = db_utils.create_promocode(
            session, start_dt, end_dt, bonus=30, min_qty=5, calc_class_name='FixedSumBonusPromoCodeGroup'
        )
        self._reservation = db_utils.create_promocode_reservation(
            session, client, pc, start_dt, end_dt
        )

        return {
            'identifier': self._charge_note._external_id,
            'promocode': self._reservation.promocode
        }

    def get_result(self):
        res = RequiredResult(
            transition=IssueTransitions.fixed,
            invoice=self._charge_note, reservation=self._reservation,
            connected=True,
            connected_invoices=[
                (self._charge_note, 300, 10 + self._charge_note.tax_policy_pct.add_to(30))
            ]
        )
        return res


class PaPartiallyActedDiscountCase(AbstractResTestCase):
    _representation = 'pa_partially_acted_discount'

    def __init__(self):
        self._invoice = None
        self._reservation = None

    def _get_data(self, session):
        client, person = db_utils.create_client_person(session)
        order = db_utils.create_order(session, client=client)
        contract = db_utils.create_general_contract(
            session, client, person, personal_account=1
        )
        pa = db_utils.create_personal_account(
            session, contract=contract, personal_account_fictive=0
        )

        start_dt, end_dt = self._get_interval(-15)
        pc = db_utils.create_promocode(
            session, start_dt, end_dt, discount_pct=25, calc_class_name='FixedDiscountPromoCodeGroup',
            apply_on_create=True
        )
        self._reservation = db_utils.create_promocode_reservation(
            session, client, pc, start_dt, end_dt
        )

        self._charge_note = db_utils.create_charge_note(
            session, pa, [(order, 10)], promo_code_id=pc.id
        )
        self._charge_note = BetterChargeNote(self._charge_note)

        pa.create_receipt(self._charge_note.effective_sum)
        pa.transfer(order)

        order.do_process_completion(5)
        pa.generate_act(force=1)

        return {
            'identifier': self._charge_note._external_id,
            'promocode': self._reservation.promocode
        }

    def get_result(self):
        res = RequiredResult(
            transition=IssueTransitions.fixed,
            invoice=self._charge_note, reservation=self._reservation,
            connected=True,
            connected_invoices=[
                (self._charge_note, 225, 10)
            ]
        )
        return res


class PaNotEnoughReceiptsCase(AbstractResTestCase):
    _representation = 'pa_not_enough_receipts'

    def __init__(self):
        self._invoice = None
        self._reservation = None

    def _get_data(self, session):
        client, person = db_utils.create_client_person(session)
        order = db_utils.create_order(session, client=client)
        contract = db_utils.create_general_contract(
            session, client, person, personal_account=1
        )
        pa = db_utils.create_personal_account(
            session, contract=contract, personal_account_fictive=0
        )

        self._charge_note = db_utils.create_charge_note(
            session, pa, [(order, 10)]
        )
        self._charge_note = BetterChargeNote(self._charge_note)

        op = mapper.Operation(2, invoice=self._charge_note)
        self._charge_note.on_turn_on(op, self._charge_note.effective_sum)

        pa.create_receipt(self._charge_note.effective_sum - 10)

        start_dt, end_dt = self._get_interval(-15)
        pc = db_utils.create_promocode(
            session, start_dt, end_dt, bonus=30, min_qty=5, calc_class_name='FixedSumBonusPromoCodeGroup'
        )
        self._reservation = db_utils.create_promocode_reservation(
            session, client, pc, start_dt, end_dt
        )

        return {
            'identifier': self._charge_note._external_id,
            'promocode': self._reservation.promocode
        }

    def get_result(self):
        res = RequiredResult(
            transition=IssueTransitions.wont_fix,
            invoice=self._charge_note, reservation=self._reservation,
        )
        txt = 'Сумма оплат меньше суммы квитанции: 290.00 (нужно 300.00).'
        res.add_message(txt)
        return res


class PaNotEnoughReceiptsDiscountCase(AbstractResTestCase):
    _representation = 'pa_not_enough_receipts_discount'

    def __init__(self):
        self._invoice = None
        self._reservation = None

    def _get_data(self, session):
        client, person = db_utils.create_client_person(session)
        order = db_utils.create_order(session, client=client)
        contract = db_utils.create_general_contract(
            session, client, person, personal_account=1
        )
        pa = db_utils.create_personal_account(
            session, contract=contract, personal_account_fictive=0
        )

        self._charge_note = db_utils.create_charge_note(
            session, pa, [(order, 10)]
        )
        self._charge_note = BetterChargeNote(self._charge_note)

        op = mapper.Operation(2, invoice=self._charge_note)
        self._charge_note.on_turn_on(op, self._charge_note.effective_sum)

        pa.create_receipt(self._charge_note.effective_sum - 10)

        start_dt, end_dt = self._get_interval(-15)
        pc = db_utils.create_promocode(
            session, start_dt, end_dt, discount_pct=15, calc_class_name='FixedDiscountPromoCodeGroup'
        )
        self._reservation = db_utils.create_promocode_reservation(
            session, client, pc, start_dt, end_dt
        )

        return {
            'identifier': self._charge_note._external_id,
            'promocode': self._reservation.promocode
        }

    def get_result(self):
        res = RequiredResult(
            transition=IssueTransitions.wont_fix,
            invoice=self._charge_note, reservation=self._reservation,
        )
        txt = 'Сумма оплат меньше суммы квитанции: 290.00 (нужно 300.00).'
        res.add_message(txt)
        return res


class PaBadPromoTypeCase(AbstractResTestCase):
    _representation = 'pa_bad_promo_type'

    def __init__(self):
        self._invoice = None
        self._reservation = None

    def _get_data(self, session):
        client, person = db_utils.create_client_person(session)
        order = db_utils.create_order(session, client=client)
        contract = db_utils.create_general_contract(
            session, client, person, personal_account=1
        )
        pa = db_utils.create_personal_account(
            session, contract=contract, personal_account_fictive=0
        )

        self._charge_note = db_utils.create_charge_note(
            session, pa, [(order, 10)]
        )
        self._charge_note = BetterChargeNote(self._charge_note)

        op = mapper.Operation(2, invoice=self._charge_note)
        self._charge_note.on_turn_on(op, self._charge_note.effective_sum)

        pa.create_receipt(self._charge_note.effective_sum)
        pa.close_invoice(self._charge_note.dt)

        start_dt, end_dt = self._get_interval(-15)
        pc = db_utils.create_promocode(session, start_dt, end_dt, bonus=30, min_qty=10)  # LegacyPromoCodeGroup
        self._reservation = db_utils.create_promocode_reservation(
            session, client, pc, start_dt, end_dt
        )

        return {
            'identifier': self._charge_note._external_id,
            'promocode': self._reservation.promocode
        }

    def get_result(self):
        res = RequiredResult(
                transition=IssueTransitions.opened,
                invoice=self._charge_note, reservation=self._reservation,
        )
        txt = 'Автоматическое применение промокодов типа LegacyPromoCodeGroup не поддерживается. ' \
            'Тикет посмотрят разработчики.'
        res.add_message(txt)
        return res


class PaNoBonusForCurrencyCase(AbstractResTestCase):
    _representation = 'pa_no_bonus_for_currency'

    def __init__(self):
        self._invoice = None
        self._reservation = None

    def _get_data(self, session):
        client, person = db_utils.create_client_person(session)
        order = db_utils.create_order(session, client=client)
        contract = db_utils.create_general_contract(
            session, client, person, personal_account=1
        )
        pa = db_utils.create_personal_account(
            session, contract=contract, personal_account_fictive=0
        )

        self._charge_note = db_utils.create_charge_note(
            session, pa, [(order, 10)]
        )
        self._charge_note.iso_currency = 'USD'
        self._charge_note = BetterChargeNote(self._charge_note)

        op = mapper.Operation(2, invoice=self._charge_note)
        self._charge_note.on_turn_on(op, self._charge_note.effective_sum)

        pa.create_receipt(self._charge_note.effective_sum)

        start_dt, end_dt = self._get_interval(-15)
        pc = db_utils.create_promocode(
            session, start_dt, end_dt, bonus=30, min_qty=5, calc_class_name='FixedSumBonusPromoCodeGroup'
        )
        self._reservation = db_utils.create_promocode_reservation(
            session, client, pc, start_dt, end_dt
        )

        return {
            'identifier': self._charge_note._external_id,
            'promocode': self._reservation.promocode
        }

    def get_result(self):
        res = RequiredResult(
                transition=IssueTransitions.wont_fix,
                invoice=self._charge_note, reservation=self._reservation,
        )
        txt = 'Промокод не предусматривает бонус в валюте счета: USD.'
        res.add_message(txt)
        return res


class PaErrorTooBigCase(AbstractResTestCase):
    _representation = 'pa_error_too_big'

    def __init__(self):
        self._invoice = None
        self._reservation = None

    def _get_data(self, session):
        client, person = db_utils.create_client_person(session)
        order = db_utils.create_order(session, client=client, product_id=503162)
        contract = db_utils.create_general_contract(
            session, client, person, personal_account=1
        )
        pa = db_utils.create_personal_account(
            session, contract=contract, personal_account_fictive=0
        )

        self._charge_note = db_utils.create_charge_note(
            session, pa, [(order, 200)]
        )
        self._charge_note = BetterChargeNote(self._charge_note)

        op = mapper.Operation(2, invoice=self._charge_note)
        self._charge_note.on_turn_on(op, self._charge_note.effective_sum)
        pa.create_receipt(self._charge_note.effective_sum)

        # Leave very little amount unacted to get a large discount (and error)
        order.do_process_completion(199)
        pa.generate_act(force=1)

        start_dt, end_dt = self._get_interval(-15)
        pc = db_utils.create_promocode(
            session, start_dt, end_dt, bonus=30, min_qty=5, calc_class_name='FixedSumBonusPromoCodeGroup'
        )
        self._reservation = db_utils.create_promocode_reservation(
            session, client, pc, start_dt, end_dt
        )

        return {
            'identifier': self._charge_note._external_id,
            'promocode': self._reservation.promocode
        }

    def get_result(self):
        res = RequiredResult(
                transition=IssueTransitions.opened,
                invoice=self._charge_note, reservation=self._reservation,
        )
        txt = 'Незаакченных средств недостаточно для точного перерасчета скидки (погрешность бонуса 5.41%). ' \
            'Тикет посмотрят разработчики.'
        res.add_message(txt)
        return res


class PaErrorTooBigDiscountCase(AbstractResTestCase):
    _representation = 'pa_error_too_big_discount'

    def __init__(self):
        self._invoice = None
        self._reservation = None

    def _get_data(self, session):
        client, person = db_utils.create_client_person(session)
        order = db_utils.create_order(session, client=client, product_id=503162)
        contract = db_utils.create_general_contract(
            session, client, person, personal_account=1
        )
        pa = db_utils.create_personal_account(
            session, contract=contract, personal_account_fictive=0
        )

        self._charge_note = db_utils.create_charge_note(
            session, pa, [(order, 200)]
        )
        self._charge_note = BetterChargeNote(self._charge_note)

        op = mapper.Operation(2, invoice=self._charge_note)
        self._charge_note.on_turn_on(op, self._charge_note.effective_sum)
        pa.create_receipt(self._charge_note.effective_sum)

        # Leave very little amount unacted to get a large discount (and error)
        order.do_process_completion(199)
        pa.generate_act(force=1)

        start_dt, end_dt = self._get_interval(-15)
        pc = db_utils.create_promocode(
            session, start_dt, end_dt, discount_pct=0.005, calc_class_name='FixedDiscountPromoCodeGroup'
        )
        self._reservation = db_utils.create_promocode_reservation(
            session, client, pc, start_dt, end_dt
        )

        return {
            'identifier': self._charge_note._external_id,
            'promocode': self._reservation.promocode
        }

    def get_result(self):
        res = RequiredResult(
                transition=IssueTransitions.opened,
                invoice=self._charge_note, reservation=self._reservation,
        )
        txt = 'Незаакченных средств недостаточно для точного перерасчета скидки (погрешность бонуса 0.51%). ' \
            'Тикет посмотрят разработчики.'
        res.add_message(txt)
        return res


class PaWithDiscountedConsumeCase(AbstractResTestCase):
    """
    Consumes that already have a discount should be skipped.
    """
    _representation = 'pa_w_discount'

    def __init__(self):
        self._invoice = None
        self._reservation = None

    def _get_data(self, session):
        client, person = db_utils.create_client_person(session)
        order = db_utils.create_order(session, client=client)
        contract = db_utils.create_general_contract(
            session, client, person, personal_account=1
        )
        pa = db_utils.create_personal_account(
            session, contract=contract, personal_account_fictive=0
        )
        start_dt, end_dt = self._get_interval(-15)
        existing_pc = db_utils.create_promocode(
            session, start_dt, end_dt, bonus=30, min_qty=5, calc_class_name='FixedSumBonusPromoCodeGroup',
            code='PROMOCODEFORPROMOCODENOOB666'
        )
        db_utils.create_promocode_reservation(
            session, client, existing_pc, start_dt, end_dt
        )

        other_charge_note = db_utils.create_charge_note(
            session, pa, [(order, 10)]
        )
        other_charge_note = BetterChargeNote(other_charge_note)
        op = mapper.Operation(2, invoice=other_charge_note)
        other_charge_note.on_turn_on(op, other_charge_note.effective_sum)

        pa.create_receipt(other_charge_note.effective_sum)

        # Another promocode already applied above

        self._charge_note = db_utils.create_charge_note(
            session, pa, [(order, 10)]
        )
        pa.create_receipt(self._charge_note.effective_sum)
        pc = db_utils.create_promocode(
            session, start_dt, end_dt, bonus=30, min_qty=5, calc_class_name='FixedSumBonusPromoCodeGroup'
        )
        self._reservation = db_utils.create_promocode_reservation(
            session, client, pc, start_dt, end_dt
        )

        return {
            'identifier': self._charge_note._external_id,
            'promocode': self._reservation.promocode
        }

    def get_result(self):
        res = RequiredResult(
                transition=IssueTransitions.wont_fix,
                invoice=self._charge_note, reservation=self._reservation,
        )
        txt = 'Отсутствуют непотраченные средства для перерасчета скидки. ' \
              'Промокод активировать невозможно. Клиент может применить его при новой оплате.'
        res.add_message(txt)
        return res


class PaWithDiscountedConsumeDiscountCase(AbstractResTestCase):
    """
    Consumes that already have a discount should be skipped.
    """
    _representation = 'pa_w_discount_discount'

    def __init__(self):
        self._invoice = None
        self._reservation = None

    def _get_data(self, session):
        client, person = db_utils.create_client_person(session)
        order = db_utils.create_order(session, client=client)
        contract = db_utils.create_general_contract(
            session, client, person, personal_account=1
        )
        pa = db_utils.create_personal_account(
            session, contract=contract, personal_account_fictive=0
        )
        start_dt, end_dt = self._get_interval(-15)
        existing_pc = db_utils.create_promocode(
            session, start_dt, end_dt, bonus=30, min_qty=5, calc_class_name='FixedSumBonusPromoCodeGroup',
            code='PROMOCODEFORPROMOCODENOOB666'
        )
        db_utils.create_promocode_reservation(
            session, client, existing_pc, start_dt, end_dt
        )

        other_charge_note = db_utils.create_charge_note(
            session, pa, [(order, 10)]
        )
        other_charge_note = BetterChargeNote(other_charge_note)
        op = mapper.Operation(2, invoice=other_charge_note)
        other_charge_note.on_turn_on(op, other_charge_note.effective_sum)

        pa.create_receipt(other_charge_note.effective_sum)

        # Another promocode already applied above

        self._charge_note = db_utils.create_charge_note(
            session, pa, [(order, 10)]
        )
        pa.create_receipt(self._charge_note.effective_sum)
        pc = db_utils.create_promocode(
            session, start_dt, end_dt, discount_pct=10, calc_class_name='FixedDiscountPromoCodeGroup'
        )
        self._reservation = db_utils.create_promocode_reservation(
            session, client, pc, start_dt, end_dt
        )

        return {
            'identifier': self._charge_note._external_id,
            'promocode': self._reservation.promocode
        }

    def get_result(self):
        res = RequiredResult(
                transition=IssueTransitions.wont_fix,
                invoice=self._charge_note, reservation=self._reservation,
        )
        txt = 'Отсутствуют непотраченные средства для перерасчета скидки. ' \
              'Промокод активировать невозможно. Клиент может применить его при новой оплате.'
        res.add_message(txt)
        return res


class MultipleConsumesPaCurrencyProductCase(AbstractResTestCase):
    _representation = 'multiple_consumes_pa_with_currency_product'

    def __init__(self):
        self._invoice = None
        self._reservation = None

    def _get_data(self, session):
        client, person = db_utils.create_client_person(session)
        order = db_utils.create_order(session, client=client, product_id=503162)
        order1 = db_utils.create_order(session, client=client, product_id=503162)
        order2 = db_utils.create_order(session, client=client, product_id=503162)
        order3 = db_utils.create_order(session, client=client, product_id=503162)
        contract = db_utils.create_general_contract(
            session, client, person, personal_account=1
        )
        pa = db_utils.create_personal_account(
            session, contract=contract, personal_account_fictive=0
        )
        other_charge_note = db_utils.create_charge_note(
            session, pa, [(order1, 100), (order2, 200), (order3, 200)]
        )

        op = mapper.Operation(2, invoice=other_charge_note)
        other_charge_note.on_turn_on(op, other_charge_note.effective_sum)
        pa.create_receipt(other_charge_note.effective_sum)

        self._charge_note = db_utils.create_charge_note(
            session, pa, [(order, 720)]
        )
        self._charge_note = BetterChargeNote(self._charge_note)
        self._charge_note.on_turn_on(op, self._charge_note.effective_sum)

        pa.create_receipt(self._charge_note.effective_sum)

        start_dt, end_dt = self._get_interval(-15)
        pc = db_utils.create_promocode(
            session, start_dt, end_dt, bonus=30, min_qty=5, calc_class_name='FixedSumBonusPromoCodeGroup'
        )
        self._reservation = db_utils.create_promocode_reservation(
            session, client, pc, start_dt, end_dt
        )

        return {
            'identifier': self._charge_note._external_id,
            'promocode': self._reservation.promocode
        }

    def get_result(self):
        res = RequiredResult(
            transition=IssueTransitions.fixed,
            invoice=self._charge_note,
            reservation=self._reservation,
            connected=True,
            connected_invoices=[
                (self._charge_note, 1220, 1220 + self._charge_note.tax_policy_pct.add_to(30*30))
            ]  # sum/qty = 1 for currency product
        )
        return res


class MultipleConsumesPaCurrencyProductDiscountCase(AbstractResTestCase):
    _representation = 'multiple_consumes_pa_with_currency_product_discount'

    def __init__(self):
        self._invoice = None
        self._reservation = None

    def _get_data(self, session):
        client, person = db_utils.create_client_person(session)
        order = db_utils.create_order(session, client=client, product_id=503162)
        order1 = db_utils.create_order(session, client=client, product_id=503162)
        order2 = db_utils.create_order(session, client=client, product_id=503162)
        order3 = db_utils.create_order(session, client=client, product_id=503162)
        contract = db_utils.create_general_contract(
            session, client, person, personal_account=1
        )
        pa = db_utils.create_personal_account(
            session, contract=contract, personal_account_fictive=0
        )
        other_charge_note = db_utils.create_charge_note(
            session, pa, [(order1, 100), (order2, 200), (order3, 200)]
        )

        op = mapper.Operation(2, invoice=other_charge_note)
        other_charge_note.on_turn_on(op, other_charge_note.effective_sum)
        pa.create_receipt(other_charge_note.effective_sum)

        self._charge_note = db_utils.create_charge_note(
            session, pa, [(order, 720)]
        )
        self._charge_note = BetterChargeNote(self._charge_note)
        self._charge_note.on_turn_on(op, self._charge_note.effective_sum)

        pa.create_receipt(self._charge_note.effective_sum)

        start_dt, end_dt = self._get_interval(-15)
        pc = db_utils.create_promocode(
            session, start_dt, end_dt, discount_pct=25, calc_class_name='FixedDiscountPromoCodeGroup'
        )
        self._reservation = db_utils.create_promocode_reservation(
            session, client, pc, start_dt, end_dt
        )

        return {
            'identifier': self._charge_note._external_id,
            'promocode': self._reservation.promocode
        }

    def get_result(self):
        res = RequiredResult(
            transition=IssueTransitions.fixed,
            invoice=self._charge_note,
            reservation=self._reservation,
            connected=True,
            connected_invoices=[
                (self._charge_note, 1220, 500 + ut.radd_percent(720, -25))
            ]  # sum/qty = 1 for currency product
        )
        return res


class MultipleConsumesPaCurrencyProductDiscount2Case(AbstractResTestCase):
    _representation = 'multiple_consumes_pa_with_currency_product_discount2'

    def __init__(self):
        self._invoice = None
        self._reservation = None

    def _get_data(self, session):
        client, person = db_utils.create_client_person(session)
        order = db_utils.create_order(session, client=client, product_id=503162)
        order1 = db_utils.create_order(session, client=client, product_id=503162)
        order2 = db_utils.create_order(session, client=client, product_id=503162)
        order3 = db_utils.create_order(session, client=client, product_id=503162)
        contract = db_utils.create_general_contract(
            session, client, person, personal_account=1
        )
        pa = db_utils.create_personal_account(
            session, contract=contract, personal_account_fictive=0
        )
        other_charge_note = db_utils.create_charge_note(
            session, pa, [(order1, 100), (order2, 200), (order3, 200)]
        )

        op = mapper.Operation(2, invoice=other_charge_note)
        other_charge_note.on_turn_on(op, other_charge_note.effective_sum)
        pa.create_receipt(other_charge_note.effective_sum)

        start_dt, end_dt = self._get_interval(-15)
        pc = db_utils.create_promocode(
            session, start_dt, end_dt, discount_pct=25, calc_class_name='FixedDiscountPromoCodeGroup',
            apply_on_create=True
        )
        self._reservation = db_utils.create_promocode_reservation(
            session, client, pc, start_dt, end_dt
        )
        self._charge_note = db_utils.create_charge_note(
            session, pa, [(order, 720)], promo_code_id=pc.id
        )
        self._charge_note = BetterChargeNote(self._charge_note)

        pa.create_receipt(self._charge_note.effective_sum)
        pa.transfer(order)

        return {
            'identifier': self._charge_note._external_id,
            'promocode': self._reservation.promocode
        }

    def get_result(self):
        res = RequiredResult(
            transition=IssueTransitions.fixed,
            invoice=self._charge_note,
            reservation=self._reservation,
            connected=True,
            connected_invoices=[
                (self._charge_note, 1040, 1220)
            ]  # sum/qty = 1 for currency product
        )
        return res


class MultipleConsumesPaCase(AbstractResTestCase):
    """
    На ЛСе включена другая квитанция в том числе с тем же заказом,
    что и в квитанции на входе
    """
    _representation = 'multiple_consumes_pa'

    def __init__(self):
        self._invoice = None
        self._reservation = None

    def _get_data(self, session):
        client, person = db_utils.create_client_person(session)
        order = db_utils.create_order(session, client=client)
        order1 = db_utils.create_order(session, client=client)
        order2 = db_utils.create_order(session, client=client)
        order3 = db_utils.create_order(session, client=client)
        contract = db_utils.create_general_contract(
            session, client, person, personal_account=1
        )
        pa = db_utils.create_personal_account(
            session, contract=contract, personal_account_fictive=0
        )
        other_charge_note = db_utils.create_charge_note(
            session, pa, [(order1, 10), (order2, 10), (order3, 10), (order, 15)]
        )

        op = mapper.Operation(2, invoice=other_charge_note)
        other_charge_note.on_turn_on(op, other_charge_note.effective_sum)
        pa.create_receipt(other_charge_note.effective_sum)

        self._charge_note = db_utils.create_charge_note(
            session, pa, [(order, 10)]
        )
        self._charge_note = BetterChargeNote(self._charge_note)
        self._charge_note.on_turn_on(op, self._charge_note.effective_sum)

        pa.create_receipt(self._charge_note.effective_sum)

        start_dt, end_dt = self._get_interval(-15)
        pc = db_utils.create_promocode(
            session, start_dt, end_dt, bonus=30, min_qty=5, calc_class_name='FixedSumBonusPromoCodeGroup'
        )
        self._reservation = db_utils.create_promocode_reservation(
            session, client, pc, start_dt, end_dt
        )

        return {
            'identifier': self._charge_note._external_id,
            'promocode': self._reservation.promocode
        }

    def get_result(self):
        res = RequiredResult(
                transition=IssueTransitions.fixed,
                invoice=self._charge_note,
                reservation=self._reservation,
                connected=True,
                connected_invoices=[(self._charge_note, 1650, 55 + self._charge_note.tax_policy_pct.add_to(30))]
        )
        return res


class MultipleConsumesPaDiscountCase(AbstractResTestCase):
    """
    На ЛСе включена другая квитанция в том числе с тем же заказом,
    что и в квитанции на входе
    """
    _representation = 'multiple_consumes_pa_discount'

    def __init__(self):
        self._invoice = None
        self._reservation = None

    def _get_data(self, session):
        client, person = db_utils.create_client_person(session)
        order = db_utils.create_order(session, client=client)
        order1 = db_utils.create_order(session, client=client)
        order2 = db_utils.create_order(session, client=client)
        order3 = db_utils.create_order(session, client=client)
        contract = db_utils.create_general_contract(
            session, client, person, personal_account=1
        )
        pa = db_utils.create_personal_account(
            session, contract=contract, personal_account_fictive=0
        )
        other_charge_note = db_utils.create_charge_note(
            session, pa, [(order1, 10), (order2, 10), (order3, 10), (order, 15)]
        )

        op = mapper.Operation(2, invoice=other_charge_note)
        other_charge_note.on_turn_on(op, other_charge_note.effective_sum)
        pa.create_receipt(other_charge_note.effective_sum)

        self._charge_note = db_utils.create_charge_note(
            session, pa, [(order, 10)]
        )
        self._charge_note = BetterChargeNote(self._charge_note)
        self._charge_note.on_turn_on(op, self._charge_note.effective_sum)

        pa.create_receipt(self._charge_note.effective_sum)

        start_dt, end_dt = self._get_interval(-15)
        pc = db_utils.create_promocode(
            session, start_dt, end_dt, discount_pct=10, calc_class_name='FixedDiscountPromoCodeGroup'
        )
        self._reservation = db_utils.create_promocode_reservation(
            session, client, pc, start_dt, end_dt
        )

        return {
            'identifier': self._charge_note._external_id,
            'promocode': self._reservation.promocode
        }

    def get_result(self):
        res = RequiredResult(
                transition=IssueTransitions.fixed,
                invoice=self._charge_note,
                reservation=self._reservation,
                connected=True,
                connected_invoices=[(self._charge_note, 1650, 45 + ut.radd_percent(10, -10))]
        )
        return res


class MultipleOrdersPaCase(AbstractResTestCase):
    _representation = 'multiple_orders_pa'

    def __init__(self):
        self._invoice = None
        self._reservation = None

    def _get_data(self, session):
        client, person = db_utils.create_client_person(session)
        order1 = db_utils.create_order(session, client=client)
        order2 = db_utils.create_order(session, client=client)
        order3 = db_utils.create_order(session, client=client)
        contract = db_utils.create_general_contract(
            session, client, person, personal_account=1
        )
        pa = db_utils.create_personal_account(
            session, contract=contract, personal_account_fictive=0
        )
        self._charge_note = db_utils.create_charge_note(
            session, pa, [(order1, 10), (order2, 10), (order3, 10)]
        )
        op = mapper.Operation(2, invoice=self._charge_note)
        pa.create_receipt(self._charge_note.effective_sum)
        self._charge_note = BetterChargeNote(self._charge_note)
        self._charge_note.on_turn_on(op, self._charge_note.effective_sum)

        start_dt, end_dt = self._get_interval(-15)
        pc = db_utils.create_promocode(
            session, start_dt, end_dt, bonus=30, min_qty=5, calc_class_name='FixedSumBonusPromoCodeGroup'
        )
        self._reservation = db_utils.create_promocode_reservation(
            session, client, pc, start_dt, end_dt
        )

        return {
            'identifier': self._charge_note._external_id,
            'promocode': self._reservation.promocode
        }

    def get_result(self):
        res = RequiredResult(
                transition=IssueTransitions.fixed,
                invoice=self._charge_note,
                reservation=self._reservation,
                connected=True,
                connected_invoices=[(self._charge_note, 900, 30 + self._charge_note.tax_policy_pct.add_to(30))]
        )
        return res


class MultipleOrdersPaDiscountCase(AbstractResTestCase):
    _representation = 'multiple_orders_pa_discount'

    def __init__(self):
        self._invoice = None
        self._reservation = None

    def _get_data(self, session):
        client, person = db_utils.create_client_person(session)
        order1 = db_utils.create_order(session, client=client)
        order2 = db_utils.create_order(session, client=client)
        order3 = db_utils.create_order(session, client=client)
        contract = db_utils.create_general_contract(
            session, client, person, personal_account=1
        )
        pa = db_utils.create_personal_account(
            session, contract=contract, personal_account_fictive=0
        )
        self._charge_note = db_utils.create_charge_note(
            session, pa, [(order1, 10), (order2, 10), (order3, 10)]
        )
        op = mapper.Operation(2, invoice=self._charge_note)
        pa.create_receipt(self._charge_note.effective_sum)
        self._charge_note = BetterChargeNote(self._charge_note)
        self._charge_note.on_turn_on(op, self._charge_note.effective_sum)

        start_dt, end_dt = self._get_interval(-15)
        pc = db_utils.create_promocode(
            session, start_dt, end_dt, discount_pct=15, calc_class_name='FixedDiscountPromoCodeGroup'
        )
        self._reservation = db_utils.create_promocode_reservation(
            session, client, pc, start_dt, end_dt
        )

        return {
            'identifier': self._charge_note._external_id,
            'promocode': self._reservation.promocode
        }

    def get_result(self):
        res = RequiredResult(
                transition=IssueTransitions.fixed,
                invoice=self._charge_note,
                reservation=self._reservation,
                connected=True,
                connected_invoices=[(self._charge_note, 900, ut.radd_percent(30, -15))]
        )
        return res


class MultipleOrdersPartiallyActedPaCase(AbstractResTestCase):
    _representation = 'multiple_orders_partially_acted_pa'

    def __init__(self):
        self._invoice = None
        self._reservation = None

    def _get_data(self, session):
        client, person = db_utils.create_client_person(session)
        order1 = db_utils.create_order(session, client=client)
        order2 = db_utils.create_order(session, client=client)
        order3 = db_utils.create_order(session, client=client)
        contract = db_utils.create_general_contract(
            session, client, person, personal_account=1
        )
        pa = db_utils.create_personal_account(
            session, contract=contract, personal_account_fictive=0
        )
        self._charge_note = db_utils.create_charge_note(
            session, pa, [(order1, 10), (order2, 10), (order3, 10)]
        )
        op = mapper.Operation(2, invoice=self._charge_note)
        pa.create_receipt(self._charge_note.effective_sum)
        self._charge_note = BetterChargeNote(self._charge_note)
        self._charge_note.on_turn_on(op, self._charge_note.effective_sum)
        order1.do_process_completion(5)
        pa.generate_act(force=1)

        start_dt, end_dt = self._get_interval(-15)
        pc = db_utils.create_promocode(
            session, start_dt, end_dt, bonus=30, min_qty=5, calc_class_name='FixedSumBonusPromoCodeGroup'
        )
        self._reservation = db_utils.create_promocode_reservation(
            session, client, pc, start_dt, end_dt
        )

        return {
            'identifier': self._charge_note._external_id,
            'promocode': self._reservation.promocode
        }

    def get_result(self):
        res = RequiredResult(
                transition=IssueTransitions.fixed,
                invoice=self._charge_note,
                reservation=self._reservation,
                connected=True,
                connected_invoices=[(self._charge_note, 900, 30 + self._charge_note.tax_policy_pct.add_to(30))]
        )
        return res


class MultipleOrdersPartiallyActedPaDiscountCase(AbstractResTestCase):
    _representation = 'multiple_orders_partially_acted_pa_discount'

    def __init__(self):
        self._invoice = None
        self._reservation = None

    def _get_data(self, session):
        client, person = db_utils.create_client_person(session)
        order1 = db_utils.create_order(session, client=client)
        order2 = db_utils.create_order(session, client=client)
        order3 = db_utils.create_order(session, client=client)
        contract = db_utils.create_general_contract(
            session, client, person, personal_account=1
        )
        pa = db_utils.create_personal_account(
            session, contract=contract, personal_account_fictive=0
        )
        self._charge_note = db_utils.create_charge_note(
            session, pa, [(order1, 10), (order2, 10), (order3, 10)]
        )
        op = mapper.Operation(2, invoice=self._charge_note)
        pa.create_receipt(self._charge_note.effective_sum)
        self._charge_note = BetterChargeNote(self._charge_note)
        self._charge_note.on_turn_on(op, self._charge_note.effective_sum)
        order1.do_process_completion(5)
        pa.generate_act(force=1)

        start_dt, end_dt = self._get_interval(-15)
        pc = db_utils.create_promocode(
            session, start_dt, end_dt, discount_pct=20, calc_class_name='FixedDiscountPromoCodeGroup'
        )
        self._reservation = db_utils.create_promocode_reservation(
            session, client, pc, start_dt, end_dt
        )

        return {
            'identifier': self._charge_note._external_id,
            'promocode': self._reservation.promocode
        }

    def get_result(self):
        res = RequiredResult(
                transition=IssueTransitions.fixed,
                invoice=self._charge_note,
                reservation=self._reservation,
                connected=True,
                connected_invoices=[(self._charge_note, 900, ut.radd_percent(30, -20))]
        )
        return res


class MultipleOrdersOneActedPaCase(AbstractResTestCase):
    _representation = 'multiple_orders_one_acted_pa'

    def __init__(self):
        self._invoice = None
        self._reservation = None

    def _get_data(self, session):
        client, person = db_utils.create_client_person(session)
        order1 = db_utils.create_order(session, client=client)
        order2 = db_utils.create_order(session, client=client)
        order3 = db_utils.create_order(session, client=client)
        contract = db_utils.create_general_contract(
            session, client, person, personal_account=1
        )
        pa = db_utils.create_personal_account(
            session, contract=contract, personal_account_fictive=0
        )
        self._charge_note = db_utils.create_charge_note(
            session, pa, [(order1, 10), (order2, 10), (order3, 10)]
        )
        op = mapper.Operation(2, invoice=self._charge_note)
        pa.create_receipt(self._charge_note.effective_sum)
        self._charge_note = BetterChargeNote(self._charge_note)
        self._charge_note.on_turn_on(op, self._charge_note.effective_sum)
        order1.do_process_completion(10)
        pa.generate_act(force=1)

        start_dt, end_dt = self._get_interval(-15)
        pc = db_utils.create_promocode(
            session, start_dt, end_dt, bonus=30, min_qty=5, calc_class_name='FixedSumBonusPromoCodeGroup'
        )
        self._reservation = db_utils.create_promocode_reservation(
            session, client, pc, start_dt, end_dt
        )

        return {
            'identifier': self._charge_note._external_id,
            'promocode': self._reservation.promocode
        }

    def get_result(self):
        res = RequiredResult(
                transition=IssueTransitions.wont_fix,
                invoice=self._charge_note,
                reservation=self._reservation,
        )
        txt = 'Отсутствуют непотраченные средства для перерасчета скидки. ' \
              'Промокод активировать невозможно. Клиент может применить его при новой оплате.'
        res.add_message(txt)
        return res


class MultipleOrdersOneActedPaDiscountCase(AbstractResTestCase):
    _representation = 'multiple_orders_one_acted_pa_discount'

    def __init__(self):
        self._invoice = None
        self._reservation = None

    def _get_data(self, session):
        client, person = db_utils.create_client_person(session)
        order1 = db_utils.create_order(session, client=client)
        order2 = db_utils.create_order(session, client=client)
        order3 = db_utils.create_order(session, client=client)
        contract = db_utils.create_general_contract(
            session, client, person, personal_account=1
        )
        pa = db_utils.create_personal_account(
            session, contract=contract, personal_account_fictive=0
        )
        self._charge_note = db_utils.create_charge_note(
            session, pa, [(order1, 10), (order2, 10), (order3, 10)]
        )
        op = mapper.Operation(2, invoice=self._charge_note)
        pa.create_receipt(self._charge_note.effective_sum)
        self._charge_note = BetterChargeNote(self._charge_note)
        self._charge_note.on_turn_on(op, self._charge_note.effective_sum)
        order1.do_process_completion(10)
        pa.generate_act(force=1)

        start_dt, end_dt = self._get_interval(-15)
        pc = db_utils.create_promocode(
            session, start_dt, end_dt, discount_pct=30, calc_class_name='FixedDiscountPromoCodeGroup'
        )
        self._reservation = db_utils.create_promocode_reservation(
            session, client, pc, start_dt, end_dt
        )

        return {
            'identifier': self._charge_note._external_id,
            'promocode': self._reservation.promocode
        }

    def get_result(self):
        res = RequiredResult(
                transition=IssueTransitions.wont_fix,
                invoice=self._charge_note,
                reservation=self._reservation,
        )
        txt = 'Отсутствуют непотраченные средства для перерасчета скидки. ' \
              'Промокод активировать невозможно. Клиент может применить его при новой оплате.'
        res.add_message(txt)
        return res


class LogTariffOrderPrepaymentInvoiceTestCase(AbstractResTestCase):
    _representation = 'log_tariff_order_prepayment'

    def __init__(self):
        self._invoice = None
        self._reservation = None

    def _get_data(self, session):
        client, person = db_utils.create_client_person(session)
        order = db_utils.create_order(session, client=client)
        self._invoice = db_utils.create_invoice(session, client, [(order, 10)],
                                                person=person, turn_on=True)
        self._invoice.create_receipt(10 * 30)
        order.do_process_completion(5)
        order._is_log_tariff = 4
        session.flush()

        start_dt, end_dt = self._get_interval(-15)
        pc = db_utils.create_promocode(
            session, start_dt, end_dt, bonus=30, min_qty=5,
            calc_class_name='FixedSumBonusPromoCodeGroup'
        )
        self._reservation = db_utils.create_promocode_reservation(session, client, pc,
                                                                  start_dt, end_dt, 30, 10)

        return {
            'identifier': self._invoice.external_id,
            'promocode': self._reservation.promocode
        }

    def get_result(self):
        res = RequiredResult(
            transition=IssueTransitions.fixed,
            invoice=self._invoice,
            reservation=self._reservation,
            connected=True,
            connected_invoices=[(self._invoice, 300, 10 + self._invoice.tax_policy_pct.add_to(30))]
        )
        return res


class LogTariffOrderChargeNoteInvoiceTestCase(AbstractResTestCase):
    _representation = 'log_tariff_order_charge_note'

    def __init__(self):
        self._invoice = None
        self._reservation = None

    def _get_data(self, session):
        client, person = db_utils.create_client_person(session)
        order = db_utils.create_order(session, client=client)
        contract = db_utils.create_general_contract(
            session, client, person, personal_account=1
        )
        pa = db_utils.create_personal_account(
            session, contract=contract, personal_account_fictive=0
        )
        self._charge_note = db_utils.create_charge_note(
            session, pa, [(order, 10)]
        )
        op = mapper.Operation(2, invoice=self._charge_note)
        pa.create_receipt(self._charge_note.effective_sum)
        self._charge_note = BetterChargeNote(self._charge_note)
        self._charge_note.on_turn_on(op, self._charge_note.effective_sum)
        order.do_process_completion(5)
        order._is_log_tariff = 4
        session.flush()

        start_dt, end_dt = self._get_interval(-15)
        pc = db_utils.create_promocode(
            session, start_dt, end_dt, bonus=30, min_qty=5, calc_class_name='FixedSumBonusPromoCodeGroup'
        )
        self._reservation = db_utils.create_promocode_reservation(
            session, client, pc, start_dt, end_dt
        )

        return {
            'identifier': self._charge_note._external_id,
            'promocode': self._reservation.promocode
        }

    def get_result(self):
        res = RequiredResult(
                transition=IssueTransitions.fixed,
                invoice=self._charge_note,
                reservation=self._reservation,
                connected=True,
                connected_invoices=[(self._charge_note, 300, 10 + self._charge_note.tax_policy_pct.add_to(30))]
        )
        return res


class SkipCheckCase(AbstractResTestCase):
    _representation = 'skip_check_sum'

    def __init__(self):
        self._invoice = None
        self._reservation = None

    def _get_data(self, session):
        client, person = db_utils.create_client_person(session)
        order = db_utils.create_order(session, client=client)
        contract = db_utils.create_general_contract(
            session, client, person, personal_account=1
        )
        pa = db_utils.create_personal_account(
            session, contract=contract, personal_account_fictive=0
        )

        self._charge_note = db_utils.create_charge_note(
            session, pa, [(order, 10)]
        )
        self._charge_note = BetterChargeNote(self._charge_note)
        op = mapper.Operation(2, invoice=self._charge_note)
        self._charge_note.on_turn_on(op, self._charge_note.effective_sum)

        pa.create_receipt(self._charge_note.effective_sum)
        from balance.actions.consumption import reverse_consume
        reverse_consume(self._charge_note.consumes[0], None, -2)
        session.flush()

        start_dt, end_dt = self._get_interval(-15)
        pc = db_utils.create_promocode(
            session, start_dt, end_dt, bonus=30, min_qty=5, calc_class_name='FixedSumBonusPromoCodeGroup'
        )
        self._reservation = db_utils.create_promocode_reservation(
            session, client, pc, start_dt, end_dt
        )

        return {
            'identifier': self._charge_note._external_id,
            'promocode': self._reservation.promocode
        }

    def get_result(self):
        res = RequiredResult(
            transition=IssueTransitions.fixed,
            invoice=self._charge_note,
            reservation=self._reservation,
            connected=True,
            connected_invoices=[(self._charge_note, 360,
                                 12 + self._charge_note.tax_policy_pct.add_to(30))]
        )
        return res


@pytest.mark.parametrize('issue_data',
                         [case() for case in AbstractResTestCase._cases],
                         ids=lambda case: str(case),
                         indirect=['issue_data'])
def test_db(session, issue_data):
    queue_object, st_issue, case = issue_data
    required_res = case.get_result()

    solver = PromoConnect(queue_object, st_issue)
    res = solver.solve()

    assert res.commit is required_res.commit
    assert res.delay is False
    report = res.issue_report

    assert len(report.comments) <= 1
    assert set(required_res.comments) == {report.comment.strip()}
    assert required_res.transition == report.transition
    assert required_res.assignee == report.assignee

    for invoice, promocode_id, consume_sum, consume_qty in required_res.invoices_states:
        assert promocode_id == invoice.promo_code_id
        assert consume_sum == sum(q.current_sum for q in invoice.consumes)
        assert abs(consume_qty - sum(q.current_qty for q in invoice.consumes)) < 0.1

    for promocode, begin_dt, end_dt in required_res.promocodes_states:
        assert begin_dt == promocode.start_dt
        assert end_dt == promocode.end_dt

    for reservation, begin_dt, end_dt in required_res.reservations_states:
        assert begin_dt == reservation.begin_dt
        assert end_dt == reservation.end_dt


class AbstractProcessCompletionTestCase(AbstractBaseDBTestCase):
    _cases = []
    __metaclass__ = case_utils.TestCaseMeta


class BaseProcessCompletionCase(AbstractProcessCompletionTestCase):
    _representation = 'base_pc'

    def __init__(self):
        self._invoice = None
        self._order = None

    def _get_data(self, session):
        client, person = db_utils.create_client_person(session)
        self._order = db_utils.create_order(session, client=client)
        order_qtys = [(self._order, 5), (self._order, 5)]
        self._invoice = db_utils.create_invoice(session, client, order_qtys, person=person, turn_on=True)
        self._order.do_process_completion(10)

        start_dt, end_dt = self._get_interval(-15)
        reservation = db_utils.create_promocode_reservation(session, client, None,
                                                            start_dt, end_dt, 30, 10)

        return {
            'identifier': self._invoice.external_id,
            'promocode': reservation.promocode
        }

    def get_result(self):
        return [
            (self._invoice, {self._order: [(0, 0), (0, 0), (20, 10), (20, 0)]})
        ]


class MultiInvoiceProcessCompletionCase(AbstractProcessCompletionTestCase):
    _representation = 'multiple_invoices_pc'

    def __init__(self):
        self._invoice_pc = None
        self._invoice_add = None
        self._order = None

    def _get_data(self, session):
        client, person = db_utils.create_client_person(session)
        self._order = db_utils.create_order(session, client=client)

        order_qtys_pc = [(self._order, 5), (self._order, 5)]
        self._invoice_pc = db_utils.create_invoice(session, client, order_qtys_pc, person=person, turn_on=True)

        order_qtys_add = [(self._order, 3), (self._order, 4)]
        self._invoice_add = db_utils.create_invoice(session, client, order_qtys_add, person=person, turn_on=True)
        self._order.do_process_completion(6)

        start_dt, end_dt = self._get_interval(-15)
        reservation = db_utils.create_promocode_reservation(session, client, None,
                                                            start_dt, end_dt, 30, 10)

        return {
            'identifier': self._invoice_pc.external_id,
            'promocode': reservation.promocode
        }

    def get_result(self):
        return [
            (self._invoice_pc, {self._order: [(0, 0), (0, 0), (20, 0), (20, 0)]}),
            (self._invoice_add, {self._order: [(3, 3), (4, 3)]})
        ]


@pytest.mark.parametrize('issue_data',
                         [case() for case in AbstractProcessCompletionTestCase._cases],
                         ids=lambda case: str(case),
                         indirect=['issue_data'])
def test_db_process_completions(session, issue_data):
    queue_object, st_issue, case = issue_data

    invoices_state = case.get_result()

    solver = PromoConnect(queue_object, st_issue)
    solver.solve()

    for invoice, order_data in invoices_state:
        for order, req_consumes_data in order_data.iteritems():
            order_consumes = itertools.ifilter(lambda q: q.order is order, invoice.consumes)
            consumes_data = [(q.current_qty, q.completion_qty) for q in order_consumes]
            assert req_consumes_data == consumes_data


class AbstractBankTestCase(AbstractBaseDBTestCase):
    _cases = []
    __metaclass__ = case_utils.TestCaseMeta

    def get_bank_response(self):
        res = self._get_bank_data()
        return mock.MagicMock(
            text=str(res),
            json=lambda: res
        )

    def __init__(self):
        self._invoice = None
        self._reservation = None

    def _get_data(self, session):
        client, person = db_utils.create_client_person(session, person_type='ur')
        person.inn = '6666666666'
        order = db_utils.create_order(session, client=client)
        self._invoice = db_utils.create_invoice(session, client, [(order, 10)], person=person, paysys_id=1003)
        self._invoice.create_receipt(self._invoice.effective_sum)

        start_dt, end_dt = self._get_interval(-15)
        promocode = db_utils.create_promocode(session, start_dt, end_dt, 30, 10, code='ALD666666DLA')
        self._reservation = db_utils.create_promocode_reservation(session, client, promocode, start_dt, end_dt)

        return {
            'identifier': self._invoice.external_id,
            'promocode': self._reservation.promocode
        }

    def _get_bank_data(self):
        raise NotImplementedError


class BankSuccessCase(AbstractBankTestCase):
    _representation = 'bank_success'

    def _get_bank_data(self):
        return {
            'code': 'ALD6-6666-6DLA',
            'inn': '6666666666'
        }

    def get_result(self):
        res = RequiredResult(
                transition=IssueTransitions.fixed,
                invoice=self._invoice, reservation=self._reservation,
                connected=True
        )
        return res


class BankInstantPaysysCase(AbstractBankTestCase):
    _representation = 'bank_instant_paysys'

    def _get_bank_data(self):
        return {
            'code': 'ALD6-6666-6DLA',
            'inn': '6666666666'
        }

    def _get_data(self, session):
        res = super(BankInstantPaysysCase, self)._get_data(session)
        self._invoice.paysys_id = 1002  # as
        self._invoice.payment_method_id = cst.PaymentMethodIDs.credit_card
        return res

    def get_result(self):
        res = RequiredResult(
                transition=IssueTransitions.wont_fix,
                invoice=self._invoice, reservation=self._reservation,
        )
        res.add_message('Промокод не прошёл проверку в выдавшем банке: '
                        'промокод нельзя применить к счёту с оплатой не через банк.')
        return res


class BankPhisPersonCase(AbstractBankTestCase):
    _representation = 'bank_phis_person'

    def _get_bank_data(self):
        return {
            'code': 'ALD6-6666-6DLA',
            'inn': '6666666666'
        }

    def _get_data(self, session):
        res = super(BankPhisPersonCase, self)._get_data(session)
        self._invoice.person.type = 'ph'
        session.flush()
        return res

    def get_result(self):
        res = RequiredResult(
                transition=IssueTransitions.wont_fix,
                invoice=self._invoice, reservation=self._reservation,
        )
        res.add_message('Промокод не прошёл проверку в выдавшем банке: '
                        'промокод нельзя применять к счёту физлица.')
        return res


class BankWrongPersonTypeCase(AbstractBankTestCase):
    """
    Суть теста:
    Промокод начинается на ALKZ (для kzu). Хотим применить на ur.
    Должно сработать исключение WRONG_PERSON_TYPE
    """

    _representation = 'bank_wrong_person_type'

    def _get_bank_data(self):
        return {
            'code': 'ALKZ-6666-6DLA',
            'inn': '6666666666'
        }

    def _get_data(self, session):
        client, person = db_utils.create_client_person(session, person_type='ur')
        person.inn = '6666666666'
        order = db_utils.create_order(session, client=client)
        self._invoice = db_utils.create_invoice(session, client, [(order, 10)], person=person, paysys_id=1003)
        self._invoice.create_receipt(self._invoice.effective_sum)

        start_dt, end_dt = self._get_interval(-15)
        promocode = db_utils.create_promocode(session, start_dt, end_dt, 30, 10, code='ALKZ66666DLA')
        self._reservation = db_utils.create_promocode_reservation(session, client, promocode, start_dt, end_dt)

        return {
            'identifier': self._invoice.external_id,
            'promocode': self._reservation.promocode
        }

    def get_result(self):
        res = RequiredResult(
            transition=IssueTransitions.wont_fix,
            invoice=self._invoice, reservation=self._reservation,
        )
        res.add_message('Промокод не прошёл проверку в выдавшем банке: '
                        'промокод нельзя применить к данному типу плательщика.')
        return res


class BankWrongINNCase(AbstractBankTestCase):
    _representation = 'bank_wrong_inn'

    def _get_bank_data(self):
        return {
            'code': 'ALD6-6666-6DLA',
            'inn': '66666'
        }

    def get_result(self):
        res = RequiredResult(
                transition=IssueTransitions.wont_fix,
                invoice=self._invoice, reservation=self._reservation,
        )
        res.add_message('Промокод не прошёл проверку в выдавшем банке: '
                        'закреплённый за промокодом ИНН 66666 отличается от ИНН плательшика указанного счёта 6666666666.')
        return res


class BankUnknownErrorCase(AbstractBankTestCase):
    _representation = 'bank_unknown_error'

    def _get_bank_data(self):
        return {
            'code': 666,
            'message': 'Не было ничего, ничего не было'
        }

    def get_result(self):
        res = RequiredResult(
            transition=IssueTransitions.wont_fix,
            invoice=self._invoice, reservation=self._reservation,
        )
        res.add_message('Промокод не прошёл проверку в выдавшем банке: '
                        'произошла техническая ошибка.')
        return res


class Bank401Case(AbstractBankTestCase):
    _representation = 'bank_401'

    def _get_bank_data(self):
        return {
            'code': 401,
            'message': 'Превед медвед!'
        }

    def get_result(self):
        res = RequiredResult(
                transition=IssueTransitions.wont_fix,
                invoice=self._invoice, reservation=self._reservation,
        )
        res.add_message('Промокод не прошёл проверку в выдавшем банке: '
                        'неправильный формат промокода.')
        return res


class Bank404Case(AbstractBankTestCase):
    _representation = 'bank_404'

    def _get_bank_data(self):
        return {
            'code': 404,
            'message': 'Иди в пень'
        }

    def get_result(self):
        res = RequiredResult(
                transition=IssueTransitions.wont_fix,
                invoice=self._invoice, reservation=self._reservation,
        )
        res.add_message('Промокод не прошёл проверку в выдавшем банке: '
                        'промокод не найден (нет в базе).')
        return res


class Bank500Case(AbstractBankTestCase):
    _representation = 'bank_500'

    def _get_bank_data(self):
        return {
            'code': 500,
            'message': 'Не было ничего, ничего не было'
        }

    def get_result(self):
        res = RequiredResult(
                transition=IssueTransitions.wont_fix,
                invoice=self._invoice, reservation=self._reservation,
        )
        res.add_message('Промокод не прошёл проверку в выдавшем банке: '
                        'промокод найден, но еще не выдан никакой компании.')
        return res


@pytest.mark.parametrize('issue_data',
                         [case_() for case_ in AbstractBankTestCase._cases],
                         ids=lambda case: str(case),
                         indirect=['issue_data'])
def test_db_bank(session, issue_data):
    queue_object, st_issue, case = issue_data
    required_res = case.get_result()

    bank_response = case.get_bank_response()

    solver = PromoConnect(queue_object, st_issue)
    with mock.patch('requests.get', lambda *args, **kwargs: bank_response):
        res = solver.solve()

    assert res.commit is True
    assert res.delay is False
    report = res.issue_report

    assert len(report.comments) <= 1
    assert set(required_res.comments) == {report.comment.strip()}
    assert required_res.transition == report.transition
    assert required_res.assignee == report.assignee

    for invoice, promocode_id, consume_sum, consume_qty in required_res.invoices_states:
        assert promocode_id == invoice.promo_code_id
        assert consume_sum == sum(q.current_sum for q in invoice.consumes)
        assert consume_qty == sum(q.current_qty for q in invoice.consumes)

    for promocode, begin_dt, end_dt in required_res.promocodes_states:
        assert begin_dt == promocode.start_dt
        assert end_dt == promocode.end_dt

    for reservation, begin_dt, end_dt in required_res.reservations_states:
        assert begin_dt == reservation.begin_dt
        assert end_dt == reservation.end_dt
