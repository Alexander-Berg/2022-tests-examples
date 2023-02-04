# -*- coding: utf-8 -*-

from __future__ import unicode_literals

import pytest
import mock
import functools
import datetime as dt

from balance.actions.consumption import reverse_consume
import balance.muzzle_util as ut
from balance import mapper, constants
from notifier.data_objects import ClientCashbackInfo

from autodasha.solver_cl import TransferClientCashbackSolver, ParseException
from autodasha.core.api.tracker import IssueReport
from autodasha.core.api.tracker import IssueTransitions
from autodasha.utils.solver_utils import D
from autodasha.utils.xls_reader import xls_reader_contents

from tests import object_builder as ob
from tests.autodasha_tests.functional_tests import case_utils
from tests.autodasha_tests.functional_tests import mock_utils
from tests.autodasha_tests.common import db_utils
from tests.autodasha_tests.common import staff_utils

COMMENTS = {
    'already_solved':
        'Эта задача уже была выполнена. Направьте новый запрос через '
        '((https://wiki.yandex-team.ru/otdelsoprovozhdeniya/dokument/avtomatformi/ '
        'формы)). Если не найдёте подходящую, заполните, пожалуйста, общую форму.',
    'wrong_client_from_id_format':
        'Невозможно определить ID клиента, с которого переносить кэшбек. '
        'Заполни, пожалуйста, форму еще раз, указав корректный ID клиента.',
    'wrong_client_to_id_format':
        'Невозможно определить ID клиента, на которого переносить кэшбек. '
        'Заполни, пожалуйста, форму еще раз, указав корректный ID клиента.',
    'wrong_service_id_format':
        'Невозможно определить сервис. '
        'Заполни, пожалуйста, форму еще раз, указав корректный сервис.',
    'client_not_found':
        'Клиент {} не найден в биллинге. Заполни, пожалуйста, '
        'форму еще раз, указав корректный ID клиента.',
    'service_not_allowed':
        'По сервису {} нельзя переносить кэшбек.',
    'not_one_currency':
        'Невозможно определить валюту кэшбека у клиента {}.',
    'nothing_to_transfer':
        'У клиента {} нет кэшбека, доступного для переноса.',
    'removed_cashback':
        'Сняли кэшбек с заказов клиента {}. Подробная информация во вложении.',
    'done':
        'Перенесли кэшбек на сумму: {}. Баланс кэшбека у клиента {}: {} {}. Дата окончания действия кешбэка: {}',
    'need_developer':
        'Во время трансфера произошла ошибка:\n{}'
        '\nЗадачу посмотрят разработчики.'
    }


@pytest.fixture(name='cashback')
def create_cashback(session, client, **kw):
    return ob.ClientCashbackBuilder.construct(
        session,
        client=client,
        **kw
    )


def _fake_init(self, *args, **kwargs):
    super(TransferClientCashbackSolver, self).__init__(*args, **kwargs)
    self.ro_session = self.session
    self.allowed_services = {777, 1337}


class AbstractMockTestCase(case_utils.AbstractMockTestCase):
    _default_lines = {
        'summary': 'Перенос кэшбека - В АД (666)',
        'from_client': 'С клиента: %s',
        'to_client': 'На клиента: %s',
        'service_id': 'Сервис: %s'
    }


class AbstractParseFailTestCase(AbstractMockTestCase):
    _cases = []
    __metaclass__ = case_utils.TestCaseMeta

    def __init__(self):
        super(AbstractParseFailTestCase, self).__init__()
        self.comments = []

    def add_result_row(self, comment_id, *args, **kwargs):
        self.comments.append(COMMENTS[comment_id].format(*args, **kwargs))

    def prepare_result(self):
        raise NotImplementedError

    def get_result(self):
        req_result = self.prepare_result()
        return req_result, self.comments


class FromClientNotParsed(AbstractParseFailTestCase):
    _representation = 'from_client_not_parsed'

    def get_data(self, mock_manager):
        lines = [
            self._get_default_line(from_client='НуВотЭтотВотЧувак')
        ]
        return self._get_default_line('summary'), lines

    def prepare_result(self):
        self.add_result_row('wrong_client_from_id_format')


class ToClientNotParsed(AbstractParseFailTestCase):
    _representation = 'to_client_not_parsed'

    def get_data(self, mock_manager):
        lines = [
            self._get_default_line(from_client='666'),
            self._get_default_line(to_client='НуВонТотЧувак')
        ]
        return self._get_default_line('summary'), lines

    def prepare_result(self):
        self.add_result_row('wrong_client_to_id_format')


class ServiceNotParsed(AbstractParseFailTestCase):
    _representation = 'service_id_not_parsed'

    def get_data(self, mock_manager):
        lines = [
            self._get_default_line(from_client='666'),
            self._get_default_line(to_client='777'),
            self._get_default_line(service_id='Яндекс.Кладбище')
        ]
        return self._get_default_line('summary'), lines

    def prepare_result(self):
        self.add_result_row('wrong_service_id_format')


class ServiceNotAllowed(AbstractParseFailTestCase):
    _representation = 'service_id_not_allowed'

    def get_data(self, mock_manager):
        lines = [
            self._get_default_line(from_client='666'),
            self._get_default_line(to_client='777'),
            self._get_default_line(service_id='Яндекс.Чистилище (666)')
        ]
        return self._get_default_line('summary'), lines

    def prepare_result(self):
        self.add_result_row('service_not_allowed', 666)


class FromClientNotFound(AbstractParseFailTestCase):
    _representation = 'from_client_not_found'

    def get_data(self, mock_manager):
        lines = [
            self._get_default_line(from_client='666'),
            self._get_default_line(to_client='777'),
            self._get_default_line(service_id='Яндекс.Рай (777)')
        ]
        return self._get_default_line('summary'), lines

    def prepare_result(self):
        self.add_result_row('client_not_found', 666)


class ToClientNotFound(AbstractParseFailTestCase):
    _representation = 'to_client_not_found'

    def get_data(self, mock_manager):
        mock_utils.create_client(mock_manager, id_=666)

        lines = [
            self._get_default_line(from_client='666'),
            self._get_default_line(to_client='777'),
            self._get_default_line(service_id='Яндекс.Рай (777)')
        ]
        return self._get_default_line('summary'), lines

    def prepare_result(self):
        self.add_result_row('client_not_found', 777)


@mock.patch('autodasha.solver_cl.transfer_client_cashback.'
            'TransferClientCashbackSolver.__init__', _fake_init)
@pytest.mark.parametrize('mock_issue_data',
                         [case() for case in AbstractParseFailTestCase._cases],
                         ids=lambda case: str(case),
                         indirect=['mock_issue_data'])
def test_parse_failing(mock_queue_object, mock_issue_data):
    issue, case = mock_issue_data
    ri = IssueReport()

    req_result, req_comment = case.get_result()

    solver = TransferClientCashbackSolver

    solver_res = solver(mock_queue_object, issue)
    if not req_result:
        with pytest.raises(ParseException) as exc:
            solver_res.parse_issue()
        assert req_comment[0] in exc.value.message
    else:
        solver_res.parse_issue()
        comments = [text.get('text') for text in ri.comments]
        comments = ' '.join(comments)
        assert req_comment[0] == comments


class AbstractParseSuccessTestCase(AbstractMockTestCase):
    _cases = []
    __metaclass__ = case_utils.TestCaseMeta


class SaulGoodman(AbstractParseSuccessTestCase):
    _representation = 'success_parse'

    def get_data(self, mock_manager):
        self.from_client = mock_utils.create_client(mock_manager, id_=666)
        self.to_client = mock_utils.create_client(mock_manager, id_=777)

        lines = [
            self._get_default_line(from_client='666'),
            self._get_default_line(to_client='777'),
            self._get_default_line(service_id='Яндекс.Рай (777)')
        ]
        return self._get_default_line('summary'), lines

    def get_result(self):
        return ut.Struct(client_from=self.from_client,
                         client_to=self.to_client,
                         service_id=777)


@mock.patch('autodasha.solver_cl.transfer_client_cashback.'
            'TransferClientCashbackSolver.__init__', _fake_init)
@pytest.mark.parametrize('mock_issue_data',
                         [case() for case in AbstractParseSuccessTestCase._cases],
                         ids=lambda case: str(case),
                         indirect=['mock_issue_data'])
def test_parse_good(mock_queue_object, mock_issue_data):
    issue, case = mock_issue_data
    req_comment = case.get_result()

    solver = TransferClientCashbackSolver

    solver_res = solver(mock_queue_object, issue)
    parsed_data = solver_res.parse_issue()

    assert req_comment == parsed_data


class RequiredResult(case_utils.RequiredResult):
    def __init__(self, **kwargs):
        self.commit = kwargs.get('commit')
        self.assignee = 'autodasha'
        self.reversed_qty = 0
        self.transferred_qty = 0
        super(RequiredResult, self).__init__(**kwargs)

    @staticmethod
    def get_comment(key, *args, **kwargs):
        return COMMENTS.get(key).format(*args, **kwargs)


class AbstractDBTestCase(case_utils.AbstractDBTestCase):
    summary = 'Перенос кэшбека - В АД (666)'
    _description = '''
    С клиента: {from_client}
    На клиента: {to_client}
    Сервис: {service_id}

    '''.strip()

    issue_key = 'test_transfer_client_cashback'

    _cases = []
    __metaclass__ = case_utils.TestCaseMeta

    def __init__(self):
        super(AbstractDBTestCase, self).__init__()
        self.config = None

    def setup_config(self, session, config):
        config['TRANSFER_CLIENT_CASHBACK_SETTINGS'] = {'allowed_services': {7}}


class NoCashbackCurrency(AbstractDBTestCase):
    _representation = 'no_cashback_currency'

    def _get_data(self, session):
        self.from_client = db_utils.create_client(session)
        self.to_client = db_utils.create_client(session)

        return {
            'from_client': str(self.from_client.id),
            'to_client': str(self.to_client.id),
            'service_id': 'Тинькофф (7)'
        }

    def get_result(self):
        res = RequiredResult(commit=True)
        res.transition = IssueTransitions.opened
        res.assignee = 'robot-octopool'
        res.add_message(
            COMMENTS['need_developer'].format(
                COMMENTS['not_one_currency'].format(self.from_client.id)
            )
        )
        return res


class MultipleCashbackCurrency(AbstractDBTestCase):
    _representation = 'multiple_cashback_currency'

    def _get_data(self, session):
        self.from_client = db_utils.create_client(session)
        self.to_client = db_utils.create_client(session)
        create_cashback(session, self.from_client)
        create_cashback(session, self.from_client, iso_currency='PLN')

        return {
            'from_client': str(self.from_client.id),
            'to_client': str(self.to_client.id),
            'service_id': 'Тинькофф (7)'
        }

    def get_result(self):
        res = RequiredResult(commit=True)
        res.transition = IssueTransitions.opened
        res.assignee = 'robot-octopool'
        res.add_message(
            COMMENTS['need_developer'].format(
                COMMENTS['not_one_currency'].format(self.from_client.id)
            )
        )
        return res


class NoCashbackConsumesNoCashbackBonus(AbstractDBTestCase):
    _representation = 'no_cashback_consumes_no_cashback_bonus'

    def _get_data(self, session):
        self.from_client = db_utils.create_client(session)
        self.to_client = db_utils.create_client(session)
        create_cashback(session, self.from_client, bonus=D(0))

        return {
            'from_client': str(self.from_client.id),
            'to_client': str(self.to_client.id),
            'service_id': 'Тинькофф (7)'
        }

    def get_result(self):
        res = RequiredResult(commit=True)
        res.transition = IssueTransitions.fixed
        res.add_message(COMMENTS['nothing_to_transfer'].format(self.from_client.id))

        return res


class CashbackBonusNoCashbackConsumes(AbstractDBTestCase):
    _representation = 'cashback_bonus_no_cashback_consumes'

    def _get_data(self, session):
        self.from_client = db_utils.create_client(session)
        self.to_client = db_utils.create_client(session)
        create_cashback(session, self.from_client)

        return {
            'from_client': str(self.from_client.id),
            'to_client': str(self.to_client.id),
            'service_id': 'Тинькофф (7)'
        }

    def get_result(self):
        res = RequiredResult(commit=True)
        res.transition = IssueTransitions.fixed
        res.add_message(COMMENTS['done'].format(1, self.to_client.id, 1, 'RUB', 'бессрочно'))

        return res


class CashbackConsumesNoCashbackBonus(AbstractDBTestCase):
    _representation = 'cashback_consumes_no_cashback_bonus'

    def _get_data(self, session):
        self.from_client = db_utils.create_client(session)
        self.to_client = db_utils.create_client(session)
        self.cashback = create_cashback(session, self.from_client)

        order = db_utils.create_order(
            session, self.from_client, service_id=7, product_id=503162
        )
        consume_qty = 10

        invoice = db_utils.create_invoice(
            session,
            self.from_client,
            [(order, consume_qty)],
        )
        invoice.create_receipt(invoice.effective_sum)
        invoice.turn_on_rows()

        session.refresh(self.cashback)

        return {
            'from_client': str(self.from_client.id),
            'to_client': str(self.to_client.id),
            'service_id': 'Тинькофф (7)'
        }

    def get_result(self):
        res = RequiredResult(commit=True)
        assert self.cashback.bonus == D(0)
        res.transition = IssueTransitions.fixed
        res.reversed_qty = 11
        res.transferred_qty = 10
        res.add_message(COMMENTS['removed_cashback'].format(self.from_client.id))
        res.add_message(COMMENTS['done'].format(1, self.to_client.id, 1, 'RUB', 'бессрочно'))

        return res


class ToClientWithoutCashback(AbstractDBTestCase):
    _representation = 'to_client_without_cashback'

    def _get_data(self, session):
        self.from_client = db_utils.create_client(session)
        self.to_client = db_utils.create_client(session)
        self.cashback = create_cashback(session, self.from_client)

        order = db_utils.create_order(
            session, self.from_client, service_id=7, product_id=503162
        )
        consume_qty = 10

        invoice = db_utils.create_invoice(
            session,
            self.from_client,
            [(order, consume_qty)],
        )
        invoice.create_receipt(invoice.effective_sum)
        invoice.turn_on_rows()

        session.refresh(self.cashback)
        self.cashback.bonus += 1

        return {
            'from_client': str(self.from_client.id),
            'to_client': str(self.to_client.id),
            'service_id': 'Тинькофф (7)'
        }

    def get_result(self):
        res = RequiredResult(commit=True)
        assert self.cashback.bonus == D(0)
        res.transition = IssueTransitions.fixed
        res.reversed_qty = 11
        res.transferred_qty = 10
        res.add_message(COMMENTS['removed_cashback'].format(self.from_client.id))
        res.add_message(COMMENTS['done'].format(2, self.to_client.id, 2, 'RUB', 'бессрочно'))

        return res


class ToClientWithoutCashbacks(AbstractDBTestCase):
    _representation = 'to_client_without_cashbacks'

    def _get_data(self, session):
        self.from_client = db_utils.create_client(session)
        self.to_client = db_utils.create_client(session)
        self.cashback_1 = create_cashback(session, self.from_client)
        self.finish_dt = ut.trunc_date(dt.datetime.now() + dt.timedelta(60))
        self.cashback_2 = create_cashback(
            session,
            self.from_client,
            start_dt=ut.trunc_date(dt.datetime.now() - dt.timedelta(60)),
            finish_dt=self.finish_dt
        )
        self.cashback_3 = create_cashback(
            session,
            self.from_client,
            start_dt=ut.trunc_date(dt.datetime.now() - dt.timedelta(30)),
            finish_dt=self.finish_dt
        )
        self.cashback_4 = create_cashback(
            session,
            self.from_client,
            start_dt=ut.trunc_date(dt.datetime.now() - dt.timedelta(60)),
            finish_dt=ut.trunc_date(dt.datetime.now() + dt.timedelta(1))
        )

        order = db_utils.create_order(
            session, self.from_client, service_id=7, product_id=503162
        )
        consume_qty = 10

        invoice = db_utils.create_invoice(
            session,
            self.from_client,
            [(order, consume_qty)],
        )
        invoice.create_receipt(invoice.effective_sum)
        invoice.turn_on_rows()

        self.cashback_4.finish_dt = ut.trunc_date(dt.datetime.now() - dt.timedelta(1))
        session.flush()

        session.refresh(self.cashback_1)
        session.refresh(self.cashback_2)
        session.refresh(self.cashback_3)
        session.refresh(self.cashback_4)
        self.cashback_1.bonus += 1
        self.cashback_2.bonus += 1
        self.cashback_3.bonus += 1
        self.cashback_4.bonus += 1

        return {
            'from_client': str(self.from_client.id),
            'to_client': str(self.to_client.id),
            'service_id': 'Тинькофф (7)'
        }

    def get_result(self):
        res = RequiredResult(commit=True)
        assert self.cashback_1.bonus == D(0)
        assert self.cashback_2.bonus == D(0)
        assert self.cashback_3.bonus == D(0)
        assert self.cashback_4.bonus == D(2)
        res.transition = IssueTransitions.fixed
        res.reversed_qty = 14
        res.transferred_qty = 10
        res.add_message(COMMENTS['removed_cashback'].format(self.from_client.id))
        res.add_message(COMMENTS['done'].format(2, self.to_client.id, 2, 'RUB', 'бессрочно'))
        res.add_message(COMMENTS['done'].format(2, self.to_client.id, 2, 'RUB', self.finish_dt))
        res.add_message(COMMENTS['done'].format(2, self.to_client.id, 2, 'RUB', self.finish_dt))

        return res


class ToClientWithCashback(AbstractDBTestCase):
    _representation = 'to_client_with_cashback'

    def _get_data(self, session):
        self.from_client = db_utils.create_client(session)
        self.to_client = db_utils.create_client(session)
        self.cashback = create_cashback(session, self.from_client)
        create_cashback(session, self.to_client)

        order = db_utils.create_order(
            session, self.from_client, service_id=7, product_id=503162
        )
        consume_qty = 10

        invoice = db_utils.create_invoice(
            session,
            self.from_client,
            [(order, consume_qty)],
        )
        invoice.create_receipt(invoice.effective_sum)
        invoice.turn_on_rows()

        session.refresh(self.cashback)
        self.cashback.bonus += 1

        return {
            'from_client': str(self.from_client.id),
            'to_client': str(self.to_client.id),
            'service_id': 'Тинькофф (7)'
        }

    def get_result(self):
        res = RequiredResult(commit=True)
        assert self.cashback.bonus == D(0)
        res.transition = IssueTransitions.fixed
        res.reversed_qty = 11
        res.transferred_qty = 10
        res.add_message(COMMENTS['removed_cashback'].format(self.from_client.id))
        res.add_message(COMMENTS['done'].format(2, self.to_client.id, 3, 'RUB', 'бессрочно'))

        return res


class ToClientWithCashbacks(AbstractDBTestCase):
    _representation = 'to_client_with_cashbacks'

    def _get_data(self, session):
        self.from_client = db_utils.create_client(session)
        self.to_client = db_utils.create_client(session)
        self.cashback_1 = create_cashback(session, self.from_client)
        self.finish_dt = ut.trunc_date(dt.datetime.now() + dt.timedelta(60))
        self.cashback_2 = create_cashback(
            session,
            self.from_client,
            start_dt=ut.trunc_date(dt.datetime.now() - dt.timedelta(60)),
            finish_dt=self.finish_dt
        )
        self.cashback_3 = create_cashback(
            session,
            self.from_client,
            start_dt=ut.trunc_date(dt.datetime.now() - dt.timedelta(30)),
            finish_dt=self.finish_dt
        )
        self.cashback_4 = create_cashback(
            session,
            self.from_client,
            start_dt=ut.trunc_date(dt.datetime.now() - dt.timedelta(60)),
            finish_dt=ut.trunc_date(dt.datetime.now() + dt.timedelta(1))
        )
        create_cashback(session, self.to_client)
        create_cashback(
            session,
            self.to_client,
            start_dt=ut.trunc_date(dt.datetime.now() - dt.timedelta(60)),
            finish_dt=self.finish_dt)
        create_cashback(
            session,
            self.to_client,
            start_dt=ut.trunc_date(dt.datetime.now() - dt.timedelta(30)),
            finish_dt=self.finish_dt)

        order = db_utils.create_order(
            session, self.from_client, service_id=7, product_id=503162
        )
        consume_qty = 10

        invoice = db_utils.create_invoice(
            session,
            self.from_client,
            [(order, consume_qty)],
        )
        invoice.create_receipt(invoice.effective_sum)
        invoice.turn_on_rows()

        self.cashback_4.finish_dt = ut.trunc_date(dt.datetime.now() - dt.timedelta(1))
        session.flush()

        session.refresh(self.cashback_1)
        session.refresh(self.cashback_2)
        session.refresh(self.cashback_3)
        session.refresh(self.cashback_4)
        self.cashback_1.bonus += 1
        self.cashback_2.bonus += 1
        self.cashback_3.bonus += 1
        self.cashback_4.bonus += 1

        return {
            'from_client': str(self.from_client.id),
            'to_client': str(self.to_client.id),
            'service_id': 'Тинькофф (7)'
        }

    def get_result(self):
        res = RequiredResult(commit=True)
        assert self.cashback_1.bonus == D(0)
        assert self.cashback_2.bonus == D(0)
        assert self.cashback_3.bonus == D(0)
        assert self.cashback_4.bonus == D(2)
        res.transition = IssueTransitions.fixed
        res.reversed_qty = 14
        res.transferred_qty = 10
        res.add_message(COMMENTS['removed_cashback'].format(self.from_client.id))
        res.add_message(COMMENTS['done'].format(2, self.to_client.id, 3, 'RUB', 'бессрочно'))
        res.add_message(COMMENTS['done'].format(2, self.to_client.id, 3, 'RUB', self.finish_dt))
        res.add_message(COMMENTS['done'].format(2, self.to_client.id, 3, 'RUB', self.finish_dt))

        return res


class CashbackConsumeWithPromoCode(AbstractDBTestCase):
    _representation = 'cashback_consume_with_promo_code'

    def _get_data(self, session):
        self.from_client = db_utils.create_client(session)
        self.to_client = db_utils.create_client(session)
        self.cashback = create_cashback(session, self.from_client)
        db_utils.create_promocode_reservation(
            session,
            self.from_client,
            start_dt=ut.trunc_date(dt.datetime.now() - dt.timedelta(1)),
            end_dt=ut.trunc_date(dt.datetime.now() + dt.timedelta(1)),
            bonus=30,
            min_qty=0
        )

        order = db_utils.create_order(
            session, self.from_client, service_id=7, product_id=503162
        )
        consume_qty = 10

        invoice = db_utils.create_invoice(
            session,
            self.from_client,
            [(order, consume_qty)],
        )
        invoice.create_receipt(invoice.effective_sum)
        invoice.turn_on_rows(apply_promocode=True)

        session.refresh(self.cashback)
        self.cashback.bonus += 1

        return {
            'from_client': str(self.from_client.id),
            'to_client': str(self.to_client.id),
            'service_id': 'Тинькофф (7)'
        }

    def get_result(self):
        res = RequiredResult(commit=True)
        assert self.cashback.bonus == D(0)
        res.transition = IssueTransitions.fixed
        res.reversed_qty = D('1087.956500')
        res.transferred_qty = D('1086.956500')
        res.add_message(COMMENTS['removed_cashback'].format(self.from_client.id))
        res.add_message(COMMENTS['done'].format(2, self.to_client.id, 2, 'RUB', 'бессрочно'))

        return res


class TransferOveracted(AbstractDBTestCase):
    _representation = 'transfer_overacted'

    def _get_data(self, session):
        self.from_client = db_utils.create_client(session)
        self.to_client = db_utils.create_client(session)
        self.cashback = create_cashback(session, self.from_client)

        order1 = db_utils.create_order(
            session, self.from_client, service_id=7, product_id=503162
        )
        order2 = db_utils.create_order(
            session, self.from_client, service_id=7, product_id=503162
        )
        consume_qty = 10

        invoice = db_utils.create_invoice(
            session,
            self.from_client,
            [(order1, consume_qty)],
        )
        invoice.create_receipt(invoice.effective_sum)
        invoice.turn_on_rows()
        db_utils.consume_order(invoice, order2, consume_qty)

        session.refresh(self.cashback)

        order1.calculate_consumption(dt.datetime.now(), {order1.shipment_type: 5})
        invoice.generate_act(backdate=dt.datetime.now(), force=1)

        consume, = [q for q in invoice.consumes if q.act_qty > 0]
        operation = mapper.Operation(666, invoice)
        reverse_consume(
            consume,
            operation,
            consume.current_qty - max(consume.completion_qty, consume.act_qty)
        )
        consume.current_qty = 0
        consume.current_sum = 0
        session.flush()

        return {
            'from_client': str(self.from_client.id),
            'to_client': str(self.to_client.id),
            'service_id': 'Тинькофф (7)'
        }

    def get_result(self):
        res = RequiredResult(commit=True)
        assert self.cashback.bonus == D(0)
        res.transition = IssueTransitions.fixed
        res.add_message(
            COMMENTS['done'].format('0.545455', self.to_client.id, '0.545455', 'RUB', 'бессрочно')
        )

        return res


class TransferOveracted2(AbstractDBTestCase):
    _representation = 'transfer_overacted_2'

    def _get_data(self, session):
        self.from_client = db_utils.create_client(session)
        self.to_client = db_utils.create_client(session)
        self.cashback = create_cashback(session, self.from_client)

        order1 = db_utils.create_order(
            session, self.from_client, service_id=7, product_id=503162
        )
        order2 = db_utils.create_order(
            session, self.from_client, service_id=7, product_id=503162
        )
        consume_qty = 10

        invoice = db_utils.create_invoice(
            session,
            self.from_client,
            [(order1, consume_qty), (order2, consume_qty)],
        )
        invoice.create_receipt(invoice.effective_sum)
        invoice.turn_on_rows()

        session.refresh(self.cashback)

        order1.calculate_consumption(dt.datetime.now(), {order1.shipment_type: 5})
        invoice.generate_act(backdate=dt.datetime.now(), force=1)

        consume, = [q for q in invoice.consumes if q.act_qty > 0]
        operation = mapper.Operation(666, invoice)
        reverse_consume(
            consume,
            operation,
            consume.current_qty - max(consume.completion_qty, consume.act_qty)
        )
        consume.current_qty = 0
        consume.current_sum = 0
        session.flush()

        return {
            'from_client': str(self.from_client.id),
            'to_client': str(self.to_client.id),
            'service_id': 'Тинькофф (7)'
        }

    def get_result(self):
        res = RequiredResult(commit=True)
        assert self.cashback.bonus == D(0)
        res.transition = IssueTransitions.fixed
        res.reversed_qty = D(5.502)
        res.transferred_qty = D(5.24)
        res.add_message(COMMENTS['removed_cashback'].format(self.from_client.id))
        res.add_message(
            COMMENTS['done'].format('0.523905', self.to_client.id, '0.523905', 'RUB', 'бессрочно')
        )

        return res


class TwoConsumesOneOrder(AbstractDBTestCase):
    _representation = 'two_consumes_one_order'

    def _get_data(self, session):
        self.from_client = db_utils.create_client(session)
        self.to_client = db_utils.create_client(session)
        self.cashback = create_cashback(session, self.from_client)

        order = db_utils.create_order(
            session, self.from_client, service_id=7, product_id=503162
        )
        consume_qty = 10

        invoice = db_utils.create_invoice(
            session,
            self.from_client,
            [(order, consume_qty), (order, consume_qty)],
        )

        invoice.create_receipt(invoice.effective_sum)
        invoice.turn_on_rows()

        session.refresh(self.cashback)
        self.cashback.bonus += 1

        return {
            'from_client': str(self.from_client.id),
            'to_client': str(self.to_client.id),
            'service_id': 'Тинькофф (7)'
        }

    def get_result(self):
        res = RequiredResult(commit=True)
        assert self.cashback.bonus == D(0)
        res.transition = IssueTransitions.fixed
        res.reversed_qty = 21
        res.transferred_qty = 20
        res.add_message(COMMENTS['removed_cashback'].format(self.from_client.id))
        res.add_message(COMMENTS['done'].format(2, self.to_client.id, 2, 'RUB', 'бессрочно'))

        return res


def mock_staff(testfunc):
    other_dick = staff_utils.Person('pepe')
    other_boss = staff_utils.Person('other_boss')
    other_dept = staff_utils.Department('other_dept', [other_boss], [], [other_dick])

    yandex = staff_utils.Department(
        'yandex', childs=[other_dept]
    )

    staff = staff_utils.StaffMock(yandex)

    staff_path = 'autodasha.core.api.staff.Staff.%s'

    @mock.patch(
        staff_path % '_get_person_data',
        lambda s, *a, **k: staff._get_person_data(*a, **k),
    )
    @mock.patch(staff_path % '__init__', lambda *args: None)
    @functools.wraps(testfunc)
    def deco(session, issue_data):
        return testfunc(session, issue_data)

    return deco


@mock_staff
@pytest.mark.parametrize(
    'issue_data',
    [case() for case in AbstractDBTestCase._cases],
    ids=lambda case: str(case),
    indirect=['issue_data'],
)
def test_db(session, issue_data):
    queue_object, st_issue, case = issue_data
    solver = TransferClientCashbackSolver(queue_object, st_issue)

    try:
        old_from_cb = session.query(mapper.ClientCashback).filter_by(client_id=case.from_client.id, service_id=7).one_or_none()
        old_to_cb = session.query(mapper.ClientCashback).filter_by(client_id=case.to_client.id, service_id=7).one_or_none()
    except:
        old_from_cb = None
        old_to_cb = None
    if old_from_cb:
        old_from_cbi = ClientCashbackInfo(session, old_from_cb.id, constants.NOTIFY_CLIENT_CASHBACK_OPCODE)
        old_from_cbi.load()
    if old_to_cb:
        old_to_cbi = ClientCashbackInfo(session, old_to_cb.id, constants.NOTIFY_CLIENT_CASHBACK_OPCODE)
        old_to_cbi.load()

    solver.session = session
    solver_res = solver.solve()

    session.flush()
    session.expire_all()

    req_res = case.get_result()

    if old_from_cb:
        new_from_cbi = ClientCashbackInfo(session, old_from_cb.id, constants.NOTIFY_CLIENT_CASHBACK_OPCODE)
        new_from_cbi.load()
        assert new_from_cbi.data['bonus'] == 0
    if old_to_cb:
        new_to_cbi = ClientCashbackInfo(session, old_to_cb.id, constants.NOTIFY_CLIENT_CASHBACK_OPCODE)
        new_to_cbi.load()
        assert (
            old_from_cbi.data['bonus'] + old_from_cbi.data['consumed_bonus']
            - new_from_cbi.data['bonus'] - new_from_cbi.data['consumed_bonus']
            == new_to_cbi.data['bonus'] - old_to_cbi.data['bonus']
        )

    report = solver_res.issue_report

    assert solver_res.commit == req_res.commit

    report_comments = []
    for c in report.comments:
        c_text = c['text']
        c_attach = c['attachments']
        for part in c_text.strip().split('\n'):
            if part:
                report_comments.append(part.strip())

        if c_attach:
            (res_attachment, ) = filter(lambda a:
                                        a.name == '%s_result.xls' % st_issue.key,
                                        c_attach)
            res_list = list(xls_reader_contents(res_attachment.getvalue()))
            reversed_qty = 0
            transferred_qty = 0
            for row in res_list:
                reversed_qty += D(row['Reversed Qty'])
                transferred_qty += D(row['Transferred back to order Qty'])

            assert req_res.reversed_qty == reversed_qty
            assert req_res.transferred_qty == transferred_qty

    req_res_comments = []
    for c in req_res.comments:
        for part in c.strip().split('\n'):
            if part:
                req_res_comments.append(part.strip())

    report_comments = sorted(report_comments)
    req_res_comments = sorted(req_res_comments)

    assert len(report_comments) == len(req_res_comments)

    for i in range(len(req_res_comments)):
        assert req_res_comments[i] == report_comments[i]

    assert report.transition == req_res.transition
    assert report.assignee == req_res.assignee
