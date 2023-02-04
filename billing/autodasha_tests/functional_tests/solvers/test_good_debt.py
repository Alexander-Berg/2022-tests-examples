# -*- coding: utf-8 -*-

from __future__ import unicode_literals

import datetime as dt
import functools
import itertools

import pytest
import mock

import balance.mapper as mapper
from balance.actions.acts.payments import ActPaidAmountProcessor

from autodasha.solver_cl import GoodDebt, ParseException
from autodasha.core.api.tracker import IssueTransitions

from tests.autodasha_tests.functional_tests import case_utils
from tests.autodasha_tests.functional_tests import mock_utils
from tests.autodasha_tests.common import db_utils
from tests.autodasha_tests.common import staff_utils


def get_approve_message(*args, **kwargs):
    return 'Нажми кнопку "Подтвердить" или познай мощь Ктулху!' \
           ' https://jing.yandex-team.ru/files/autodasha/approve_button_something.png'


class AbstractMockTestCase(case_utils.AbstractMockTestCase):
    _default_lines = {
        'summary': 'Исключение актов из расчета лимита кредита',
        'acts': '№ акта: %s',
        'invoices': 'Номер счета: %s',
        'reason': 'Причина исключения: во славу сатаны!',
    }


class AbstractFailingMockTestCase(AbstractMockTestCase):
    _cases = []
    __metaclass__ = case_utils.TestCaseMeta


class InvalidFormActCase(AbstractFailingMockTestCase):
    _representation = 'invalid_act'

    def get_data(self, mock_manager):
        invoice = mock_utils.create_invoice(mock_manager)
        act = mock_utils.create_act(mock_manager, invoice)

        lines = [
            u'Номер акта: %s' % act.external_id,
            self._get_default_line(invoices=invoice.external_id),
            self._get_default_line('reason')
        ]
        return self._get_default_line('summary'), lines

    def get_result(self):
        return 'Некорректно указан акт. Поправь, пожалуйста, текст задачи и переоткрой ее.'


class InvalidFormInvoiceCase(AbstractFailingMockTestCase):
    _representation = 'invalid_invoice'

    def get_data(self, mock_manager):
        invoice = mock_utils.create_invoice(mock_manager)
        act = mock_utils.create_act(mock_manager, invoice)

        lines = [
            self._get_default_line(acts=act.external_id),
            'Номер счетаААААА!!!11: %s' % invoice.external_id,
            self._get_default_line('reason')
        ]
        return self._get_default_line('summary'), lines

    def get_result(self):
        return 'Некорректно указан счет. Поправь, пожалуйста, текст задачи и переоткрой ее.'


class UnknownInvoiceCase(AbstractFailingMockTestCase):
    _representation = 'unknown_invoice'

    def get_data(self, mock_manager):
        invoice = mock_utils.create_invoice(mock_manager, external_id='Б-66666-6')
        act = mock_utils.create_act(mock_manager, invoice)

        lines = [
            self._get_default_line(acts=act.external_id),
            self._get_default_line(invoices='Б-66766-6'),
            self._get_default_line('reason')
        ]
        return self._get_default_line('summary'), lines

    def get_result(self):
        return 'Счет Б-66766-6 не найден в биллинге. Поправь, пожалуйста, текст задачи и переоткрой ее.'


class UnknownActCase(AbstractFailingMockTestCase):
    _representation = 'unknown_act'

    def get_data(self, mock_manager):
        invoice = mock_utils.create_invoice(mock_manager)
        act = mock_utils.create_act(mock_manager, invoice, external_id='123456789')

        lines = [
            self._get_default_line(acts='987654321'),
            self._get_default_line(invoices=invoice.external_id),
            self._get_default_line('reason')
        ]
        return self._get_default_line('summary'), lines

    def get_result(self):
        return 'Акт 987654321 не найден в биллинге. Поправь, пожалуйста, текст задачи и переоткрой ее.'


class UnknownActYBCase(AbstractFailingMockTestCase):
    _representation = 'unknown_act_yb'

    def get_data(self, mock_manager):
        invoice = mock_utils.create_invoice(mock_manager)
        act = mock_utils.create_act(mock_manager, invoice, external_id='YB-123456789')

        lines = [
            self._get_default_line(acts='YB-123456789, YB-987654321, YB-123456789  '),
            self._get_default_line(invoices=invoice.external_id),
            self._get_default_line('reason')
        ]
        return self._get_default_line('summary'), lines

    def get_result(self):
        return 'Акт YB-987654321 не найден в биллинге. Поправь, пожалуйста, текст задачи и переоткрой ее.'


class AbstractGoodMockTestCase(AbstractMockTestCase):
    _cases = []
    __metaclass__ = case_utils.TestCaseMeta


@mock.patch('autodasha.core.api.staff.Staff.__init__', lambda *args: None)
@mock.patch('autodasha.core.api.gap.Gap.__init__', lambda *args: None)
@pytest.mark.parametrize('mock_issue_data',
                         [case() for case in AbstractFailingMockTestCase._cases],
                         ids=lambda case: str(case),
                         indirect=['mock_issue_data'])
def test_parse_failing(mock_queue_object, mock_issue_data):
    issue, case = mock_issue_data

    req_comment = case.get_result()

    solver = GoodDebt(mock_queue_object, issue)
    with pytest.raises(ParseException) as exc:
        solver.parse_issue()

    assert req_comment in exc.value.message


class SingleObjectsCase(AbstractGoodMockTestCase):
    _representation = 'single_objects'

    def __init__(self):
        self._invoice = None
        self._act = None

    def get_data(self, mock_manager):
        self._invoice = mock_utils.create_invoice(mock_manager)
        self._act = mock_utils.create_act(mock_manager, self._invoice)

        lines = [
            self._get_default_line(acts=self._act.external_id),
            self._get_default_line(invoices=self._invoice.external_id),
            self._get_default_line('reason')
        ]
        return self._get_default_line('summary'), lines

    def get_result(self):
        return {'invoices': [self._invoice], 'acts': [self._act]}


class PersonalAccountCase(AbstractGoodMockTestCase):
    _representation = 'personal_account'

    def __init__(self):
        self._invoice = None
        self._act = None

    def get_data(self, mock_manager):
        self._invoice = mock_utils.create_invoice(mock_manager, external_id='ЛСТ-6666666-4')
        self._act = mock_utils.create_act(mock_manager, self._invoice)

        lines = [
            self._get_default_line(acts=self._act.external_id),
            self._get_default_line(invoices=self._invoice.external_id),
            self._get_default_line('reason')
        ]
        return self._get_default_line('summary'), lines

    def get_result(self):
        return {'invoices': [self._invoice], 'acts': [self._act]}


class MultipleObjectsCase(AbstractGoodMockTestCase):
    _representation = 'multiple_objects'

    def __init__(self):
        self._invoices = None
        self._acts = None

    def get_data(self, mock_manager):
        i1 = mock_utils.create_invoice(mock_manager, external_id='Б-6666666-1')
        i2 = mock_utils.create_invoice(mock_manager, external_id='Б-6666666-2')
        i3 = mock_utils.create_invoice(mock_manager, external_id='Б-6666666-3')
        a1 = mock_utils.create_act(mock_manager, i1, external_id='123456789')
        a2 = mock_utils.create_act(mock_manager, i2, external_id='987654321')
        a3 = mock_utils.create_act(mock_manager, i3, external_id='876543219')

        self._acts = [a1, a2, a3]
        self._invoices = [i1, i2, i3]

        lines = [
            self._get_default_line(acts=', '.join(a.external_id for a in self._acts)),
            self._get_default_line(invoices=', '.join(i.external_id for i in self._invoices)),
            self._get_default_line('reason')
        ]
        return self._get_default_line('summary'), lines

    def get_result(self):
        return {'invoices': self._invoices, 'acts': self._acts}


@mock.patch('autodasha.core.api.staff.Staff.__init__', lambda *args: None)
@mock.patch('autodasha.core.api.gap.Gap.__init__', lambda *args: None)
@pytest.mark.parametrize('mock_issue_data',
                         [case() for case in AbstractGoodMockTestCase._cases],
                         ids=lambda case: str(case),
                         indirect=['mock_issue_data'])
def test_parse_good(mock_queue_object, mock_issue_data):
    issue, case = mock_issue_data

    req_res = case.get_result()

    solver = GoodDebt(mock_queue_object, issue)
    res = solver.parse_issue()

    assert set(req_res['invoices']) == set(res['invoices'])
    assert set(req_res['acts']) == set(res['acts'])


class AbstractDBTestCase(case_utils.AbstractDBTestCase):
    _summary = 'Исключение актов из расчета лимита кредита'
    _description = '''
№ акта: {acts}
Номер счета: {invoices}
Причина исключения: патамучта гладиолус
'''

    _cases = []
    __metaclass__ = case_utils.TestCaseMeta

    def get_description(self, session):
        invoices, acts = self._get_data(session)
        return self._description.format(
                invoices=','.join(i.external_id for i in invoices),
                acts=','.join(a.external_id for a in acts)
        )


class RequiredResult(case_utils.RequiredResult):
    def __init__(self, summonees=None, num_comments=1, **kwargs):
        self.acts_states = []
        self.summonees = summonees
        self.commit = kwargs.get('commit', True)
        self.num_comments = num_comments

        super(RequiredResult, self).__init__(**kwargs)
        self.delay = kwargs.get('delay', self.transition == IssueTransitions.none)

    def set_object_states(self, **kwargs):
        self.acts_states = kwargs.get('act_states', [])
        self.add_good_acts(*kwargs.get('good_acts', []))
        self.add_bad_acts(*kwargs.get('bad_acts', []))

    def set_messages(self, **kwargs):
        super(RequiredResult, self).set_messages(**kwargs)

        self.add_excluded_msg(*kwargs.get('excluded_msg', []))
        self.add_unpaid_msg(*kwargs.get('unpaid_msg', []))
        self.add_unknown_msg(*kwargs.get('unknown_msg', []))
        self.add_already_excluded_msg(*kwargs.get('already_excluded_msg', []))
        self.add_already_paid_msg(*kwargs.get('already_paid_msg', []))

    def add_unpaid_msg(self, *args):
        msg = 'Акт %s также не оплачен до %s сумма акта/оплат: %.2f/%.2f'
        for act in args:
            dt_ = act.payment_term_dt or act.dt
            self.add_message(msg % (act.external_id, dt_.strftime('%d.%m.%Y'), act.amount, act.paid_amount))

    def add_excluded_msg(self, *args):
        msg = 'Акт %s исключен из блокировки кредита.'
        for act in args:
            self.add_message(msg % act.external_id)

    def add_unknown_msg(self, *args):
        msg = 'Акт %s не принадлежит указанным счетам.'
        for act in args:
            self.add_message(msg % act.external_id)

    def add_already_excluded_msg(self, *args):
        msg = 'Акт %s уже исключен из блокировки лимита кредита. ' \
              'Пожалуйста, уточни данные и напиши новую задачу через форму, если необходимо.'
        for act in args:
            self.add_message(msg % act.external_id)

    def add_already_paid_msg(self, *args):
        msg = 'Акт %s уже оплачен. ' \
              'Пожалуйста, уточни данные и напиши новую задачу через форму, если необходимо.'
        for act in args:
            self.add_message(msg % act.external_id)

    def add_good_acts(self, *args):
        self.acts_states.extend((a, 1) for a in args)

    def add_bad_acts(self, *args):
        self.acts_states.extend((a, 0) for a in args)


class RequestConfirmationCase(AbstractDBTestCase):
    _representation = 'request_confirmation'

    def __init__(self):
        self._act = None

    def _get_data(self, session):
        client, person = db_utils.create_client_person(session)
        order = db_utils.create_order(session, client)

        month = mapper.ActMonth()

        invoice = db_utils.create_personal_account(session, client, person)
        invoice.transfer(order, 2, 30, skip_check=True)
        order.calculate_consumption(month.document_dt, {order.shipment_type: 30})
        invoice.generate_act(force=True, backdate=month.document_dt)

        self._act, = {at.act for q in order.consumes for at in q.acttranses}

        return [self._act.invoice], [self._act]

    author = 'some_dick'

    def get_result(self):
        res = RequiredResult(transition=IssueTransitions.none,
                             summonees=['boss'],
                             messages=[get_approve_message()],
                             bad_acts=[self._act])
        return res


class WrongApproverCase(AbstractDBTestCase):
    _representation = 'wrong_approver'

    def __init__(self):
        self._act = None

    def _get_data(self, session):
        client, person = db_utils.create_client_person(session)
        order = db_utils.create_order(session, client)

        month = mapper.ActMonth()

        invoice = db_utils.create_personal_account(session, client, person)
        invoice.transfer(order, 2, 30, skip_check=True)
        order.calculate_consumption(month.document_dt, {order.shipment_type: 30})
        invoice.generate_act(force=True, backdate=month.document_dt)

        self._act, = {at.act for q in order.consumes for at in q.acttranses}

        return [self._act.invoice], [self._act]

    author = 'some_dick'

    def get_comments(self):
        return [('other_dick', 'Подтверждено', dt.datetime.now())]

    def get_result(self):
        res = RequiredResult(transition=IssueTransitions.none,
                             summonees=['boss'],
                             messages=[get_approve_message(),
                                       'Подтверждение ожидаем от руководителя (укого:boss) или от его замещающих.'],
                             bad_acts=[self._act])
        return res


class WrongApproverAlreadyWarnedCase(AbstractDBTestCase):
    _representation = 'wrong_approver_already_warned'

    def __init__(self):
        self._act = None

    def _get_data(self, session):
        client, person = db_utils.create_client_person(session)
        order = db_utils.create_order(session, client)

        month = mapper.ActMonth()

        invoice = db_utils.create_personal_account(session, client, person)
        invoice.transfer(order, 2, 30, skip_check=True)
        order.calculate_consumption(month.document_dt, {order.shipment_type: 30})
        invoice.generate_act(force=True, backdate=month.document_dt)

        self._act, = {at.act for q in order.consumes for at in q.acttranses}

        return [self._act.invoice], [self._act]

    author = 'some_dick'

    def get_comments(self):
        return [('other_dick', 'Подтверждено', dt.datetime.now()),
                ('autodasha', 'Подтверждение ожидаем от руководителя', dt.datetime.now()),]

    def get_result(self):
        return None


class ConfirmedCase(AbstractDBTestCase):
    _representation = 'confirmed'

    def __init__(self):
        self._act = None

    def _get_data(self, session):
        client, person = db_utils.create_client_person(session)
        order = db_utils.create_order(session, client)

        month = mapper.ActMonth()

        invoice = db_utils.create_personal_account(session, client, person)
        invoice.transfer(order, 2, 30, skip_check=True)
        order.calculate_consumption(month.document_dt, {order.shipment_type: 30})
        invoice.generate_act(force=True, backdate=month.document_dt)

        self._act, = {at.act for q in order.consumes for at in q.acttranses}

        return [self._act.invoice], [self._act]

    author = 'some_dick'

    def get_comments(self):
        return [('boss', 'Подтверждено', dt.datetime.now())]

    def get_result(self):
        return RequiredResult(excluded_msg=[self._act], good_acts=[self._act])


class ConfirmedDeputyCase(AbstractDBTestCase):
    _representation = 'confirmed_deputy'

    def __init__(self):
        self._act = None

    def _get_data(self, session):
        client, person = db_utils.create_client_person(session)
        order = db_utils.create_order(session, client)

        month = mapper.ActMonth()

        invoice = db_utils.create_personal_account(session, client, person)
        invoice.transfer(order, 2, 30, skip_check=True)
        order.calculate_consumption(month.document_dt, {order.shipment_type: 30})
        invoice.generate_act(force=True, backdate=month.document_dt)

        self._act, = {at.act for q in order.consumes for at in q.acttranses}

        return [self._act.invoice], [self._act]

    author = 'some_dick'

    def get_comments(self):
        return [('deputy', 'Подтверждено', dt.datetime.now())]

    def get_result(self):
        return RequiredResult(excluded_msg=[self._act], good_acts=[self._act])


class ConfirmedOtherCase(AbstractDBTestCase):
    _representation = 'confirmed_other'

    def __init__(self):
        self._act = None

    def _get_data(self, session):
        client, person = db_utils.create_client_person(session)
        order = db_utils.create_order(session, client)

        month = mapper.ActMonth()

        invoice = db_utils.create_personal_account(session, client, person)
        invoice.transfer(order, 2, 30, skip_check=True)
        order.calculate_consumption(month.document_dt, {order.shipment_type: 30})
        invoice.generate_act(force=True, backdate=month.document_dt)

        self._act, = {at.act for q in order.consumes for at in q.acttranses}

        return [self._act.invoice], [self._act]

    author = 'some_dick'

    def get_comments(self):
        return [('other_boss', 'Подтверждено', dt.datetime.now())]

    def get_result(self):
        return RequiredResult(excluded_msg=[self._act], good_acts=[self._act])


class WaitingCase(AbstractDBTestCase):
    _representation = 'waiting'

    def __init__(self):
        self._act = None

    def _get_data(self, session):
        client, person = db_utils.create_client_person(session)
        order = db_utils.create_order(session, client)

        month = mapper.ActMonth()

        invoice = db_utils.create_personal_account(session, client, person)
        invoice.transfer(order, 2, 30, skip_check=True)
        order.calculate_consumption(month.document_dt, {order.shipment_type: 30})
        invoice.generate_act(force=True, backdate=month.document_dt)

        self._act, = {at.act for q in order.consumes for at in q.acttranses}

        return [self._act.invoice], [self._act]

    author = 'some_dick'

    def get_comments(self):
        return [('autodasha', 'Сударь, вы идиот!', dt.datetime.now())]

    def get_result(self):
        return None


class WaitingTooLongCase(AbstractDBTestCase):
    _representation = 'waiting_too_long'

    def __init__(self):
        self._act = None

    def _get_data(self, session):
        client, person = db_utils.create_client_person(session)
        order = db_utils.create_order(session, client)

        month = mapper.ActMonth()

        invoice = db_utils.create_personal_account(session, client, person)
        invoice.transfer(order, 2, 30, skip_check=True)
        order.calculate_consumption(month.document_dt, {order.shipment_type: 30})
        invoice.generate_act(force=True, backdate=month.document_dt)

        self._act, = {at.act for q in order.consumes for at in q.acttranses}

        return [self._act.invoice], [self._act]

    author = 'some_dick'

    def get_comments(self):
        return [('autodasha', 'Подтверждай, сука!', dt.datetime.now() - dt.timedelta(22))]

    def get_result(self):
        msg = 'Нет подтверждения руководителя. ' \
              'Пожалуйста, уточни данные и напиши новую задачу через форму, если необходимо.'
        return RequiredResult(bad_acts=[self._act],
                              messages=[msg])


class FromApproverCase(AbstractDBTestCase):
    _representation = 'from_approver'

    def __init__(self):
        self._act = None

    def _get_data(self, session):
        client, person = db_utils.create_client_person(session)
        order = db_utils.create_order(session, client)

        month = mapper.ActMonth()

        invoice = db_utils.create_personal_account(session, client, person)
        invoice.transfer(order, 2, 30, skip_check=True)
        order.calculate_consumption(month.document_dt, {order.shipment_type: 30})
        invoice.generate_act(force=True, backdate=month.document_dt)

        self._act, = {at.act for q in order.consumes for at in q.acttranses}

        return [self._act.invoice], [self._act]

    author = 'boss'

    def get_result(self):
        return RequiredResult(excluded_msg=[self._act], good_acts=[self._act])


class TopChiefCase(AbstractDBTestCase):
    _representation = 'top_chief'

    def __init__(self):
        self._act = None

    author = 'holy_spirit'

    def _get_data(self, session):
        client, person = db_utils.create_client_person(session)
        order = db_utils.create_order(session, client)

        month = mapper.ActMonth()

        invoice = db_utils.create_personal_account(session, client, person)
        invoice.transfer(order, 2, 30, skip_check=True)
        order.calculate_consumption(month.document_dt, {order.shipment_type: 30})
        invoice.generate_act(force=True, backdate=month.document_dt)

        self._act, = {at.act for q in order.consumes for at in q.acttranses}

        return [self._act.invoice], [self._act]

    def get_result(self):
        msg = 'кто:mscnad7, не удалось найти подходящего подтверждающего. Посмотри, пожалуйста.'
        res = RequiredResult(transition=IssueTransitions.none,
                             messages=[msg],
                             assignee='mscnad7',
                             commit=True,
                             delay=True,
                             bad_acts=[self._act])
        return res


class TopChiefWaitingCase(AbstractDBTestCase):
    _representation = 'top_chief_waiting'

    def __init__(self):
        self._act = None

    author = 'holy_spirit'

    def get_comments(self):
        autodasha_msg = 'кто:mscnad7, не удалось найти подходящего подтверждающего. Посмотри, пожалуйста.'
        return [('autodasha', autodasha_msg, dt.datetime.now())]

    def _get_data(self, session):
        client, person = db_utils.create_client_person(session)
        order = db_utils.create_order(session, client)

        month = mapper.ActMonth()

        invoice = db_utils.create_personal_account(session, client, person)
        invoice.transfer(order, 2, 30, skip_check=True)
        order.calculate_consumption(month.document_dt, {order.shipment_type: 30})
        invoice.generate_act(force=True, backdate=month.document_dt)

        self._act, = {at.act for q in order.consumes for at in q.acttranses}

        return [self._act.invoice], [self._act]

    def get_result(self):
        return None


class TopChiefApprovedCase(AbstractDBTestCase):
    _representation = 'top_chief_approved'

    def __init__(self):
        self._act = None

    author = 'holy_spirit'

    def get_comments(self):
        autodasha_msg = 'кто:mscnad7, не удалось найти подходящего подтверждающего. Посмотри, пожалуйста.'
        return [
            ('autodasha', autodasha_msg),
            ('boss', 'Подтверждено', dt.datetime.now()),
        ]

    def _get_data(self, session):
        client, person = db_utils.create_client_person(session)
        order = db_utils.create_order(session, client)

        month = mapper.ActMonth()

        invoice = db_utils.create_personal_account(session, client, person)
        invoice.transfer(order, 2, 30, skip_check=True)
        order.calculate_consumption(month.document_dt, {order.shipment_type: 30})
        invoice.generate_act(force=True, backdate=month.document_dt)

        self._act, = {at.act for q in order.consumes for at in q.acttranses}

        return [self._act.invoice], [self._act]

    def get_result(self):
        return RequiredResult(excluded_msg=[self._act], good_acts=[self._act])


class TopChiefWrongApproverCase(AbstractDBTestCase):
    _representation = 'top_chief_wrong_approver'

    def __init__(self):
        self._act = None

    author = 'holy_spirit'

    def get_comments(self):
        autodasha_msg = 'кто:mscnad7, не удалось найти подходящего подтверждающего. Посмотри, пожалуйста.'
        return [
            ('autodasha', autodasha_msg, dt.datetime.now()),
            ('some_dick', 'Подтверждено', dt.datetime.now()),
        ]

    def _get_data(self, session):
        client, person = db_utils.create_client_person(session)
        order = db_utils.create_order(session, client)

        month = mapper.ActMonth()

        invoice = db_utils.create_personal_account(session, client, person)
        invoice.transfer(order, 2, 30, skip_check=True)
        order.calculate_consumption(month.document_dt, {order.shipment_type: 30})
        invoice.generate_act(force=True, backdate=month.document_dt)

        self._act, = {at.act for q in order.consumes for at in q.acttranses}

        return [self._act.invoice], [self._act]

    def get_result(self):
        return None


class AbsentChiefCase(AbstractDBTestCase):
    _representation = 'absent_chief'

    def __init__(self):
        self._act = None

    author = 'subdick'

    def _get_data(self, session):
        client, person = db_utils.create_client_person(session)
        order = db_utils.create_order(session, client)

        month = mapper.ActMonth()

        invoice = db_utils.create_personal_account(session, client, person)
        invoice.transfer(order, 2, 30, skip_check=True)
        order.calculate_consumption(month.document_dt, {order.shipment_type: 30})
        invoice.generate_act(force=True, backdate=month.document_dt)

        self._act, = {at.act for q in order.consumes for at in q.acttranses}

        return [self._act.invoice], [self._act]

    def get_result(self):
        msg = 'кто:mscnad7, подтверждающий и его руководители отсутствуют. Уточни, пожалуйста, кого звать?'
        res = RequiredResult(transition=IssueTransitions.none,
                             messages=[msg],
                             assignee='mscnad7',
                             commit=True,
                             delay=True,
                             bad_acts=[self._act])
        return res


class AbsentChiefWaitingCase(AbstractDBTestCase):
    _representation = 'absent_chief_waiting'

    def __init__(self):
        self._act = None

    author = 'subdick'

    def get_comments(self):
        autodasha_msg = 'кто:mscnad7, подтверждающий и его руководители отсутствуют. Уточни, пожалуйста, кого звать?'
        return [('autodasha', autodasha_msg, dt.datetime.now())]

    def _get_data(self, session):
        client, person = db_utils.create_client_person(session)
        order = db_utils.create_order(session, client)

        month = mapper.ActMonth()

        invoice = db_utils.create_personal_account(session, client, person)
        invoice.transfer(order, 2, 30, skip_check=True)
        order.calculate_consumption(month.document_dt, {order.shipment_type: 30})
        invoice.generate_act(force=True, backdate=month.document_dt)

        self._act, = {at.act for q in order.consumes for at in q.acttranses}

        return [self._act.invoice], [self._act]

    def get_result(self):
        return None


class AlreadyExcludedCase(AbstractDBTestCase):
    _representation = 'pa_already_good'

    def __init__(self):
        self._act = None
        self._act_already = None

    def _get_data(self, session):
        client, person = db_utils.create_client_person(session)
        order = db_utils.create_order(session, client)

        month = mapper.ActMonth()

        invoice = db_utils.create_personal_account(session, client, person)
        invoice.transfer(order, 2, 30, skip_check=True)
        order.calculate_consumption(month.document_dt, {order.shipment_type: 15})
        invoice.generate_act(force=True, backdate=month.document_dt)

        order.calculate_consumption(month.document_dt, {order.shipment_type: 30})
        invoice.generate_act(force=True, backdate=month.document_dt)

        self._act, self._act_already = {at.act for q in order.consumes for at in q.acttranses}
        self._act.external_id = 'YB-' + self._act.external_id
        self._act_already.good_debt = 1

        return [self._act.invoice], [self._act, self._act_already]

    author = 'boss'

    def get_result(self):
        return RequiredResult(excluded_msg=[self._act],
                              already_excluded_msg=[self._act_already],
                              good_acts=[self._act, self._act_already])


class AlreadyExcludedUnconfirmedCase(AbstractDBTestCase):
    _representation = 'pa_already_good_unconfirmed'

    def __init__(self):
        self._act = None

    def _get_data(self, session):
        client, person = db_utils.create_client_person(session)
        order = db_utils.create_order(session, client)

        month = mapper.ActMonth()

        invoice = db_utils.create_personal_account(session, client, person)
        invoice.transfer(order, 2, 30, skip_check=True)
        order.calculate_consumption(month.document_dt, {order.shipment_type: 30})
        invoice.generate_act(force=True, backdate=month.document_dt)

        self._act, = {at.act for q in order.consumes for at in q.acttranses}
        self._act.external_id = 'YB-' + self._act.external_id + '-1'
        self._act.good_debt = 1

        return [self._act.invoice], [self._act]

    author = 'some_dick'

    def get_result(self):
        return RequiredResult(transition=IssueTransitions.wont_fix,
                              already_excluded_msg=[self._act],
                              good_acts=[self._act])


class AlreadyPaidCase(AbstractDBTestCase):
    _representation = 'pa_already_paid'

    def __init__(self):
        self._act = None
        self._act_already = None

    def _get_data(self, session):
        client, person = db_utils.create_client_person(session)
        order = db_utils.create_order(session, client)

        month = mapper.ActMonth()

        invoice = db_utils.create_personal_account(session, client, person)
        invoice.transfer(order, 2, 30, skip_check=True)
        order.calculate_consumption(month.document_dt, {order.shipment_type: 15})
        invoice.generate_act(force=True, backdate=month.document_dt)

        order.calculate_consumption(month.document_dt, {order.shipment_type: 30})
        invoice.generate_act(force=True, backdate=month.document_dt)

        self._act, self._act_already = {at.act for q in order.consumes for at in q.acttranses}
        self._act_already.invoice.receipt_sum_1c = self._act_already.invoice.total_act_sum
        self._act_already.invoice.update_paid_amount()

        return [self._act.invoice], [self._act, self._act_already]

    author = 'boss'

    def get_result(self):
        return RequiredResult(excluded_msg=[self._act],
                              already_paid_msg=[self._act_already],
                              good_acts=[self._act],
                              bad_acts=[self._act_already])


class AlreadyPaidUnconfirmedCase(AbstractDBTestCase):
    _representation = 'pa_already_paid_unconfirmed'

    def __init__(self):
        self._act = None

    def _get_data(self, session):
        client, person = db_utils.create_client_person(session)
        order = db_utils.create_order(session, client)

        month = mapper.ActMonth()

        invoice = db_utils.create_personal_account(session, client, person)
        invoice.transfer(order, 2, 30, skip_check=True)
        order.calculate_consumption(month.document_dt, {order.shipment_type: 30})
        invoice.generate_act(force=True, backdate=month.document_dt)

        self._act, = {at.act for q in order.consumes for at in q.acttranses}
        self._act.invoice.receipt_sum_1c = self._act.invoice.total_act_sum
        self._act.invoice.update_paid_amount()

        return [self._act.invoice], [self._act]

    author = 'some_dick'

    def get_result(self):
        return RequiredResult(transition=IssueTransitions.wont_fix,
                              already_paid_msg=[self._act],
                              bad_acts=[self._act])


class AlreadyPaidUnconfirmedPartialCase(AbstractDBTestCase):
    _representation = 'pa_partial_already_paid_unconfirmed'

    def __init__(self):
        self._act = None
        self._act_already = None

    def _get_data(self, session):
        client, person = db_utils.create_client_person(session)
        order = db_utils.create_order(session, client)

        month = mapper.ActMonth()

        invoice = db_utils.create_personal_account(session, client, person)
        invoice.transfer(order, 2, 30, skip_check=True)
        order.calculate_consumption(month.document_dt, {order.shipment_type: 15})
        invoice.generate_act(force=True, backdate=month.document_dt)

        order.calculate_consumption(month.document_dt, {order.shipment_type: 30})
        invoice.generate_act(force=True, backdate=month.document_dt)

        self._act, self._act_already = {at.act for q in order.consumes for at in q.acttranses}
        self._act_already.invoice.receipt_sum_1c = self._act_already.invoice.total_act_sum
        self._act_already.invoice.update_paid_amount()

        return [self._act.invoice], [self._act, self._act_already]

    author = 'some_dick'

    def get_result(self):
        res = RequiredResult(transition=IssueTransitions.none,
                             num_comments=2,
                             summonees=['boss'],
                             messages=[get_approve_message()],
                             already_paid_msg=[self._act_already],
                             bad_acts=[self._act, self._act_already])
        return res


class AlreadyPaidConfirmedPartialCase(AbstractDBTestCase):
    _representation = 'pa_partial_already_paid_confirmed'

    def __init__(self):
        self._act = None
        self._act_already = None

    def get_comments(self):
        act_msg = 'Акт %s уже оплачен. Пожалуйста, уточни данные ' \
                  'и напиши новую задачу через форму, если необходимо.' % self._act_already.external_id
        return [
            ('autodasha', act_msg),
            ('autodasha', get_approve_message()),
            ('boss', 'Подтверждено', dt.datetime.now())
        ]

    def _get_data(self, session):
        client, person = db_utils.create_client_person(session)
        order = db_utils.create_order(session, client)

        month = mapper.ActMonth()

        invoice = db_utils.create_personal_account(session, client, person)
        invoice.transfer(order, 2, 30, skip_check=True)
        order.calculate_consumption(month.document_dt, {order.shipment_type: 15})
        invoice.generate_act(force=True, backdate=month.document_dt)

        order.calculate_consumption(month.document_dt, {order.shipment_type: 30})
        invoice.generate_act(force=True, backdate=month.document_dt)

        self._act, self._act_already = {at.act for q in order.consumes for at in q.acttranses}
        self._act_already.invoice.receipt_sum_1c = self._act_already.invoice.total_act_sum
        self._act_already.invoice.update_paid_amount()

        return [self._act.invoice], [self._act, self._act_already]

    author = 'some_dick'

    def get_result(self):
        res = RequiredResult(excluded_msg=[self._act],
                             good_acts=[self._act],
                             bad_acts=[self._act_already])
        return res


class AccountAdditionalPaidCase(AbstractDBTestCase):
    _representation = 'pa_add_paid_act'

    def __init__(self):
        self._acts = None
        self._act_skip = None

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

        act1, act2, self._act_skip = {at.act for q in order.consumes for at in q.acttranses}

        self._acts = [act1, act2]
        ActPaidAmountProcessor(session, self._act_skip.invoice, self._act_skip, self._act_skip.amount).run()

        return [invoice], self._acts

    author = 'boss'

    def get_result(self):
        return RequiredResult(excluded_msg=self._acts,
                              good_acts=self._acts,
                              bad_acts=[self._act_skip])


class AccountAdditionalUnpaidCase(AbstractDBTestCase):
    _representation = 'pa_add_unpaid'

    def __init__(self):
        self._acts = None
        self._act_skip = None

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

        act1, act2, self._act_skip = {at.act for q in order.consumes for at in q.acttranses}

        self._acts = [act1, act2]

        return [invoice], self._acts

    author = 'boss'

    def get_result(self):
        return RequiredResult(excluded_msg=self._acts,
                              unpaid_msg=[self._act_skip],
                              good_acts=self._acts,
                              bad_acts=[self._act_skip])


class AccountUnknownInvoiceCase(AbstractDBTestCase):
    _representation = 'pa_unknown_invoice'

    def __init__(self):
        self._act = None
        self._act_skip = None
        self._act_bad = None

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

        self._act, self._act_skip, self._act_bad = list({at.act for q in order.consumes for at in q.acttranses})

        return [self._act.invoice], [self._act, self._act_bad]

    author = 'boss'

    def get_result(self):
        return RequiredResult(excluded_msg=[self._act],
                              unknown_msg=[self._act_bad],
                              unpaid_msg=[self._act_skip],
                              good_acts=[self._act],
                              bad_acts=[self._act_bad, self._act_skip])


class YInvoiceAddPaidCase(AbstractDBTestCase):
    _representation = 'y_invoice_add_paid_act'

    def __init__(self):
        self._acts = None
        self._act_skip = None

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

        act1, act2, self._act_skip = {at.act for q in order.consumes for at in q.acttranses}
        self._acts = [act1, act2]

        ActPaidAmountProcessor(session, self._act_skip.invoice, self._act_skip, self._act_skip.amount).run()

        return [act1.invoice, act2.invoice], self._acts

    author = 'boss'

    def get_result(self):
        return RequiredResult(excluded_msg=self._acts,
                              good_acts=self._acts,
                              bad_acts=[self._act_skip])


class YInvoiceAddUnpaidCase(AbstractDBTestCase):
    _representation = 'y_invoice_add_unpaid_act'

    def __init__(self):
        self._acts = None
        self._act_skip = None

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

        act1, act2, self._act_skip = {at.act for q in order.consumes for at in q.acttranses}
        self._acts = [act1, act2]

        return [act1.invoice, act2.invoice], self._acts

    author = 'boss'

    def get_result(self):
        return RequiredResult(excluded_msg=self._acts,
                              unpaid_msg=[self._act_skip],
                              good_acts=self._acts,
                              bad_acts=[self._act_skip])


class YInvoiceMixedPAUnpaidCase(AbstractDBTestCase):
    _representation = 'y_invoice_w_pa'

    def __init__(self):
        self._acts = None
        self._act_skip = None

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

        act1, act2, self._act_skip = {at.act for q in order.consumes for at in q.acttranses}
        self._acts = [act1, act2]

        return [act1.invoice, act2.invoice, self._act_skip.invoice, invoice], self._acts

    author = 'boss'

    def get_result(self):
        return RequiredResult(excluded_msg=self._acts,
                              unpaid_msg=[self._act_skip],
                              good_acts=self._acts,
                              bad_acts=[self._act_skip])


class AccountIndividualPaidCase(AbstractDBTestCase):
    _representation = 'individual_add_paid_act'

    def __init__(self):
        self._act_gen = None
        self._act = None
        self._act_skip = None

    def _get_data(self, session):
        agency, person = db_utils.create_client_person(session, is_agency=True)

        client = db_utils.create_client(session, agency=agency)
        order = db_utils.create_order(session, client, agency=agency)

        client_ind = db_utils.create_client(session, agency=agency)
        order_ind = db_utils.create_order(session, client_ind, agency=agency)

        month = mapper.ActMonth()

        invoice = db_utils.create_personal_account(session, agency, person)
        invoice.transfer(order, 2, 30, skip_check=True)
        order.calculate_consumption(month.document_dt, {order.shipment_type: 30})
        invoice.generate_act(force=True, backdate=month.document_dt)

        invoice_ind = db_utils.create_personal_account(session, contract=invoice.contract, subclient=client_ind)
        invoice_ind.transfer(order_ind, 2, 30, skip_check=True)

        order_ind.calculate_consumption(month.document_dt, {order_ind.shipment_type: 15})
        invoice_ind.generate_act(force=True, backdate=month.document_dt)

        order_ind.calculate_consumption(month.document_dt, {order_ind.shipment_type: 30})
        invoice_ind.generate_act(force=True, backdate=month.document_dt)

        self._act_gen, = {at.act for q in order.consumes for at in q.acttranses}
        self._act, self._act_skip = {at.act for q in order_ind.consumes for at in q.acttranses}

        ActPaidAmountProcessor(session, self._act_skip.invoice, self._act_skip, self._act_skip.amount).run()

        return [invoice_ind], [self._act]

    author = 'boss'

    def get_result(self):
        return RequiredResult(excluded_msg=[self._act],
                              good_acts=[self._act],
                              bad_acts=[self._act_skip, self._act_gen])


class AccountIndividualUnpaidCase(AbstractDBTestCase):
    _representation = 'individual_add_unpaid_act'

    def __init__(self):
        self._act_gen = None
        self._act = None
        self._act_skip = None

    def _get_data(self, session):
        agency, person = db_utils.create_client_person(session, is_agency=True)

        client = db_utils.create_client(session, agency=agency)
        order = db_utils.create_order(session, client, agency=agency)

        client_ind = db_utils.create_client(session, agency=agency)
        order_ind = db_utils.create_order(session, client_ind, agency=agency)

        month = mapper.ActMonth()

        invoice = db_utils.create_personal_account(session, agency, person)
        invoice.transfer(order, 2, 30, skip_check=True)
        order.calculate_consumption(month.document_dt, {order.shipment_type: 30})
        invoice.generate_act(force=True, backdate=month.document_dt)

        invoice_ind = db_utils.create_personal_account(session, contract=invoice.contract, subclient=client_ind)
        invoice_ind.transfer(order_ind, 2, 30, skip_check=True)

        order_ind.calculate_consumption(month.document_dt, {order_ind.shipment_type: 15})
        invoice_ind.generate_act(force=True, backdate=month.document_dt)

        order_ind.calculate_consumption(month.document_dt, {order_ind.shipment_type: 30})
        invoice_ind.generate_act(force=True, backdate=month.document_dt)

        self._act_gen, = {at.act for q in order.consumes for at in q.acttranses}
        self._act, self._act_skip = {at.act for q in order_ind.consumes for at in q.acttranses}

        return [invoice_ind], [self._act]

    author = 'boss'

    def get_result(self):
        return RequiredResult(excluded_msg=[self._act],
                              unpaid_msg=[self._act_skip],
                              good_acts=[self._act],
                              bad_acts=[self._act_skip, self._act_gen])


class RepaymentAdditionalPaidCase(AbstractDBTestCase):
    _representation = 'repayment_add_paid_act'

    def __init__(self):
        self._acts = None
        self._act_skip = None

    def _get_data(self, session):
        client, person = db_utils.create_client_person(session)
        order = db_utils.create_order(session, client)

        month = mapper.ActMonth()

        contract = db_utils.create_general_contract(session, client, person, month.begin_dt, payment_type=3)

        invoice1 = db_utils.create_fictive_invoice(session, contract, [(order, 10)])
        invoice2 = db_utils.create_fictive_invoice(session, contract, [(order, 10)])
        invoice3 = db_utils.create_fictive_invoice(session, contract, [(order, 10)])

        order.calculate_consumption(month.document_dt, {order.shipment_type: 30})

        act1, = db_utils.generate_acts(client, month, dps=[invoice1.deferpay.id])
        act2, = db_utils.generate_acts(client, month, dps=[invoice2.deferpay.id])
        self._act_skip, = db_utils.generate_acts(client, month, dps=[invoice3.deferpay.id])

        self._acts = [act1, act2]
        ActPaidAmountProcessor(session, self._act_skip.invoice, self._act_skip, self._act_skip.amount).run()

        return [invoice1.repayment, invoice2.repayment], self._acts

    author = 'boss'

    def get_result(self):
        return RequiredResult(excluded_msg=self._acts,
                              good_acts=self._acts,
                              bad_acts=[self._act_skip])


class RepaymentAdditionalUnpaidCase(AbstractDBTestCase):
    _representation = 'repayment_add_unpaid_act'

    def __init__(self):
        self._acts = None
        self._act_skip = None

    def _get_data(self, session):
        client, person = db_utils.create_client_person(session)
        order = db_utils.create_order(session, client)

        month = mapper.ActMonth()

        contract = db_utils.create_general_contract(session, client, person, month.begin_dt, payment_type=3)

        invoice1 = db_utils.create_fictive_invoice(session, contract, [(order, 10)])
        invoice2 = db_utils.create_fictive_invoice(session, contract, [(order, 10)])
        invoice3 = db_utils.create_fictive_invoice(session, contract, [(order, 10)])

        order.calculate_consumption(month.document_dt, {order.shipment_type: 30})

        act1, = db_utils.generate_acts(client, month, dps=[invoice1.deferpay.id])
        act2, = db_utils.generate_acts(client, month, dps=[invoice2.deferpay.id])
        self._act_skip, = db_utils.generate_acts(client, month, dps=[invoice3.deferpay.id])

        self._acts = [act1, act2]

        return [invoice1.repayment, invoice2.repayment], self._acts

    author = 'boss'

    def get_result(self):
        return RequiredResult(excluded_msg=self._acts,
                              unpaid_msg=[self._act_skip],
                              good_acts=self._acts,
                              bad_acts=[self._act_skip])


class PrepaymentCase(AbstractDBTestCase):
    _representation = 'prepayment'

    def __init__(self):
        self._act = None

    def _get_data(self, session):
        client, person = db_utils.create_client_person(session)
        order = db_utils.create_order(session, client)

        month = mapper.ActMonth()

        invoice = db_utils.create_invoice_simple(session, order, [10], person=person, dt=month.begin_dt)
        invoice.close_invoice(month.document_dt)
        self._act, = invoice.acts

        return [invoice], [self._act]

    author = 'some_dick'

    def get_result(self):
        msg = u'Счёт %s не является кредитным. Поправь, пожалуйста, текст задачи и переоткрой её.'
        return RequiredResult(transition=IssueTransitions.wont_fix,
                              bad_acts=[self._act], messages=[msg % self._act.invoice.external_id])


class FictiveCase(AbstractDBTestCase):
    _representation = 'fictive'

    def __init__(self):
        self._act = None
        self._fictive = None

    def _get_data(self, session):
        client, person = db_utils.create_client_person(session)
        order = db_utils.create_order(session, client)

        month = mapper.ActMonth()

        contract = db_utils.create_general_contract(session, client, person, month.begin_dt, payment_type=3)
        self._fictive = db_utils.create_fictive_invoice(session, contract, [(order, 10)])
        order.calculate_consumption(month.document_dt, {order.shipment_type: 10})

        self._act, = db_utils.generate_acts(client, month, dps=[self._fictive.deferpay.id])

        return [self._fictive], [self._act]

    author = 'boss'

    def get_result(self):
        msg = 'Счёт %s является фиктивным - необходимо указать счёт на погашение. ' \
              'Пожалуйста, уточни данные и заполни форму еще раз.'
        return RequiredResult(transition=IssueTransitions.wont_fix,
                              bad_acts=[self._act],
                              messages=[msg % self._fictive.external_id])


def mock_staff(testfunc):
    some_dick = staff_utils.Person('some_dick')
    deputy = staff_utils.Person('deputy')
    boss = staff_utils.Person('boss')
    some_dept = staff_utils.Department('some_dep', [boss], [deputy], [some_dick])

    other_dick = staff_utils.Person('other_dick')
    other_boss = staff_utils.Person('other_boss')
    other_dept = staff_utils.Department('other_dept', [other_boss], [], [other_dick])

    subdick = staff_utils.Person('subdick')
    subboss1 = staff_utils.Person('subboss1')
    subdept1 = staff_utils.Department('subdept1', [subboss1], [], [subdick])

    subboss2 = staff_utils.Person('subboss2')
    subdept2 = staff_utils.Department('subdept2', [subboss2], [], [], [subdept1])

    subboss3 = staff_utils.Person('subboss3')
    subdept3 = staff_utils.Department('subdept3', [subboss3], [], [], [subdept2])

    holy_spirit = staff_utils.Person('holy_spirit')
    son = staff_utils.Person('son')
    god = staff_utils.Person('god')
    yandex = staff_utils.Department('yandex', [god], [], [son, holy_spirit], [some_dept, other_dept, subdept3])

    staff = staff_utils.StaffMock(yandex)

    staff_path = 'autodasha.core.api.staff.Staff.%s'

    @mock.patch(staff_path % '_get_person_data', lambda s, *a, **k: staff._get_person_data(*a, **k))
    @mock.patch(staff_path % '_get_department_data', lambda s, *a, **k: staff._get_department_data(*a, **k))
    @mock.patch(staff_path % '__init__', lambda *args: None)
    @functools.wraps(testfunc)
    def deco(session, issue_data):
        return testfunc(session, issue_data)

    return deco


def gap_patch(testfunc):
    subboss1 = staff_utils.PersonGap('subboss1')
    subboss2 = staff_utils.PersonGap('subboss2')
    subboss3 = staff_utils.PersonGap('subboss3')
    gap = staff_utils.GapMock([subboss1, subboss2, subboss3])

    gap_path = 'autodasha.core.api.gap.Gap.%s'

    @mock.patch(gap_path % '_find_gaps', lambda s, *a, **k: gap._find_gaps(*a, **k))
    @mock.patch(gap_path % '__init__', lambda *args: None)
    @functools.wraps(testfunc)
    def deco(session, issue_data):
        return testfunc(session, issue_data)

    return deco


@pytest.fixture
def config(config):
    old_ = config._items.get('common_approve_departments')
    config._items['common_approve_departments'] = ['yandex']

    yield config

    config._items['common_approve_departments'] = old_


@mock.patch('autodasha.solver_cl.base_solver.BaseSolver.get_approve_message', get_approve_message)
@mock_staff
@gap_patch
@pytest.mark.usefixtures('config')
@pytest.mark.parametrize('issue_data',
                         [case() for case in AbstractDBTestCase._cases],
                         ids=lambda case: str(case),
                         indirect=['issue_data'])
def test_db(session, issue_data):
    queue_object, st_issue, case = issue_data

    req_res = case.get_result()

    solver = GoodDebt(queue_object, st_issue)
    res = solver.solve()
    session.flush()

    if req_res is None:
        assert res.commit is False
        assert res.delay is True
        assert res.issue_report is None
        return

    assert res.commit is req_res.commit
    assert res.delay is req_res.delay
    report = res.issue_report

    assert len(report.comments) == req_res.num_comments
    assert case_utils.prepare_comment('\n'.join(c.text for c in report.comments)) == set(req_res.comments)

    assert report.transition == req_res.transition
    assert report.assignee == req_res.assignee
    for act, state in req_res.acts_states:
        assert act.good_debt == state

    summonees = set(itertools.chain.from_iterable(c.summonees or [] for c in report.comments))
    assert summonees == set(req_res.summonees or [])
