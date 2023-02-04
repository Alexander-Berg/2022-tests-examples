# -*- coding: utf-8 -*-

from __future__ import unicode_literals

import itertools
import decimal

import mock
import pytest

import balance.mapper as mapper
import balance.muzzle_util as ut
from balance.actions import consumption

from autodasha.core.api.tracker import IssueReport, IssueTransitions
from autodasha.solver_cl.base_solver import CheckException, ParseException
from autodasha.solver_cl.transfer_order import TransferOrder

from tests import object_builder as ob
from tests.autodasha_tests.functional_tests import case_utils
from tests.autodasha_tests.functional_tests import mock_utils
from tests.autodasha_tests.common import db_utils


COMMENTS = {
    'order_not_found':
        'Заказ %s не найден, создай недовыставленный счет или уточни сервис или укажи корректный заказ.',
    'incorrect_row':
        'Строка %%{row}%%: Некорректно указан номер заказа или сумма. '
        'Пожалуйста, укажи корректно сервис и номер заказа. Например, 70-4000006 или 7-4000008.',
    'no_sum': 'Строка %%{row}%%: не указано количество к переносу и не стоит галка "Перенести всё".',
    'no_orders': 'Нет заказов к переносу.',
    'incorrect_sum': 'Строка %%{row}%%: некорректно указано количество для переноса',
    'wrong_sum_prec': 'Указано недопустимое количество для переноса с заказа {f.eid} на заказ {t.eid} - {qty}.',
    'not_transferred_because': 'Не перенесено с {f.eid} на {t.eid}: {error}',
    'check_services': 'Перенос между разными сервисами запрещен.',
    'check_consumes': 'Перенос невозможен, так как на исходном заказе нет заявок.',
    'overacted':
        'Перенос невозможен, так как по заказу {f.eid} откручено меньше чем заакчено.',
    'overacted_fta':
        'Перенос с заказа {f.eid} в автоматическом режиме невозможен: по заказу откручено меньше, чем заакчено. '
        'Обратитесь в поддержку через '
        '((https://wiki.yandex-team.ru/otdelsoprovozhdeniya/dokument/avtomatformi/ общую форму)).',
    'check_different_clients': 'Заказы принадлежат разным клиентам.',
    'check_client_subclient': 'Перенос средств между разными субклиентами агентства и прямыми клиентами.',
    'check_different_agencies': 'Перенос между разными агентствами.',
    'check_commission_discount_type': 'Различаются типы комиссии или типы скидки в заказах.',
    'check_subclient_limits': 'Запрещён перенос между заказами клиентов с индивидуальным кредитным лимитом.',
    'check_order_unmoderated': 'Заказ {o.eid} непромодерирован, перенос невозможен.',
    'check_invoice_unmoderated': 'Счёт {i.external_id} непромодерирован, перенос невозможен.',
    'incorrect_form': 'Форма заполнена некорректно.',
    'not_enough_money': 'На заказе {f.eid} недостаточно средств для переноса на заказ {t.eid}. Свободно: {qty}.',
    'sum_transferred': 'Сумма {qty} переведена с заказа {f.eid} на заказ {t.eid}.',
    'all_transferred': 'Все свободные средства переведены с заказа {f.eid} на заказ {t.eid}.',
    'cash_back_transfer_denied': 'Перенос между заказами с зачисленным кэшбэком возможен только в рамках одного субклиента.',
    'not_enough_money_cashback': 'В переносе могут участвовать только средства, зачисленные без активированного кэшбэка. Сумма свободных средств недостаточна для переноса.',
    'internal_error': 'Произошла внутренняя ошибка при переносе с {f.eid} на {t.eid}: {err}.'
}


class AbstractParseTestCase(case_utils.AbstractMockTestCase):
    _default_lines = {
        'summary': 'Перенос средств с заказа на заказ',
        'header': 'Данные по заказам (в вышеописаном формате):',
        'all': '%s-%s %s-%s',
        'qty': '%s-%s %s-%s %s',
        'transfer_all': 'Перенести все свободные средства с заказа на заказ: True',
        'footer': 'Перенос подтверждаю под свою ответственность: True'
    }


class AbstractFailedCheckFormTestCase(AbstractParseTestCase):
    _cases = []
    __metaclass__ = case_utils.TestCaseMeta


class WrongHeaderCheckTestCase(AbstractFailedCheckFormTestCase):
    _representation = 'wrong_header'

    def get_data(self, mock_manager):
        return self._get_default_line('summary'), [
            'Данные по заказам (в формате, приснившемся мне в кошмарном сне):'
            'СЕРВИС-САТАНЫ-6666666 666-6666666666 666.666',
            self._get_default_line('footer')
        ]

    def get_result(self):
        return [COMMENTS['incorrect_form']]


class WrongFooterCheckTestCase(AbstractFailedCheckFormTestCase):
    _representation = 'wrong_footer'

    def get_data(self, mock_manager):
        return self._get_default_line('summary'), [
            self._get_default_line('header'),
            '7-1234567 7-7654321 10',
            'Перенос благословлён Господом Богом: True'
        ]

    def get_result(self):
        return [COMMENTS['incorrect_form']]


@pytest.fixture(autouse=True)
def _allow_subclients_transfer(session):
    session.config.__dict__['CHECK_SUBCLIENTS_OVERACT_TRANSFER'] = True


@pytest.mark.parametrize('mock_issue_data',
                         [case() for case in AbstractFailedCheckFormTestCase._cases],
                         ids=lambda case: str(case),
                         indirect=['mock_issue_data'])
def test_failed_check_form(mock_queue_object, mock_issue_data):
    issue, case = mock_issue_data

    required_comments = case.get_result()

    solver = TransferOrder(mock_queue_object, issue)
    ri = IssueReport()
    ri.comment = ''
    with pytest.raises(CheckException) as exc_info:
        solver.check_form(ri)

    assert set(required_comments) == case_utils.prepare_comment(exc_info.value.message)


class AbstractSuccessfullCheckFormTestCase(AbstractParseTestCase):
    _cases = []
    __metaclass__ = case_utils.TestCaseMeta


class NormalQtyCheckFormCase(AbstractSuccessfullCheckFormTestCase):
    _representation = 'normal_qty'

    def get_data(self, mock_manager):
        return self._get_default_line('summary'), [
            self._get_default_line('header'),
            '7-1234567 7-7654321 10',
            '7-5555555 7-6666666 7',
            self._get_default_line('footer')
        ]

    def get_result(self):
        return '7-1234567 7-7654321 10\n7-5555555 7-6666666 7'


class NormalAllCheckFormCase(AbstractSuccessfullCheckFormTestCase):
    _representation = 'normal_all'

    def get_data(self, mock_manager):
        return self._get_default_line('summary'), [
            self._get_default_line('header'),
            '7-1234567 7-7654321',
            '7-5555555 7-6666666',
            self._get_default_line('transfer_all'),
            self._get_default_line('footer')
        ]

    def get_result(self):
        return '7-1234567 7-7654321\n7-5555555 7-6666666\n' + self._get_default_line('transfer_all')


class PushkinCheckFormCase(AbstractSuccessfullCheckFormTestCase):
    _representation = 'pushkin'

    def get_data(self, mock_manager):
        return self._get_default_line('summary'), [
            self._get_default_line('header'),
            'Мой дядя самых честных правил,',
            'Когда не в шутку занемог,',
            'Он уважать себя заставил',
            'И лучше выдумать не мог.',
            'Его пример другим наука;',
            'Но, боже мой, какая скука',
            'С больным сидеть и день и ночь,',
            'Не отходя ни шагу прочь!',
            'Какое низкое коварство',
            'Полу-живого забавлять,',
            'Ему подушки поправлять,',
            'Печально подносить лекарство,',
            'Вздыхать и думать про себя:',
            'Когда же чёрт возьмет тебя!',
            self._get_default_line('footer')
        ]

    def get_result(self):
        return '''
Мой дядя самых честных правил,
Когда не в шутку занемог,
Он уважать себя заставил
И лучше выдумать не мог.
Его пример другим наука;
Но, боже мой, какая скука
С больным сидеть и день и ночь,
Не отходя ни шагу прочь!
Какое низкое коварство
Полу-живого забавлять,
Ему подушки поправлять,
Печально подносить лекарство,
Вздыхать и думать про себя:
Когда же чёрт возьмет тебя!'''.strip()


@pytest.mark.parametrize('mock_issue_data',
                         [case() for case in AbstractSuccessfullCheckFormTestCase._cases],
                         ids=lambda case: str(case),
                         indirect=['mock_issue_data'])
def test_successfull_check_form(mock_queue_object, mock_issue_data):
    issue, case = mock_issue_data

    required_text = case.get_result()

    solver = TransferOrder(mock_queue_object, issue)
    ri = IssueReport()
    ri.comment = ''
    orders_text = solver.check_form(ri)

    assert required_text == orders_text


class AbstractSuccessfullParseTestCase(AbstractParseTestCase):
    _cases = []
    __metaclass__ = case_utils.TestCaseMeta


class PushkinAllParseFormCase(AbstractSuccessfullParseTestCase):
    _representation = 'pushkin_all'

    def __init__(self):
        super(PushkinAllParseFormCase, self).__init__()
        self.of = None
        self.ot = None

    def get_data(self, mock_manager):
        self.of = mock_utils.create_order(mock_manager, 7, 7777777)
        self.ot = mock_utils.create_order(mock_manager, 6, 6666666)

        return self._get_default_line('summary'), [
            self._get_default_line('header'),
            'Мой дядя самых честных правил,',
            'Когда не в шутку занемог,',
            self._get_default_line(all=(self.of.service_id, self.of.service_order_id,
                                        self.ot.service_id, self.ot.service_order_id)),
            'Он уважать себя заставил',
            self._get_default_line('transfer_all'),
            self._get_default_line('footer')
        ]

    def get_result(self):
        return [(self.of, self.ot, None)], [
            COMMENTS['incorrect_row'].format(row='Мой дядя самых честных правил,'),
            COMMENTS['incorrect_row'].format(row='Когда не в шутку занемог,'),
            COMMENTS['incorrect_row'].format(row='Он уважать себя заставил'),
        ]


class PushkinQtyParseFormCase(AbstractSuccessfullParseTestCase):
    _representation = 'pushkin_qty'

    def __init__(self):
        super(PushkinQtyParseFormCase, self).__init__()
        self.of = None
        self.ot = None

    def get_data(self, mock_manager):
        self.of = mock_utils.create_order(mock_manager, 7, 7777777)
        self.ot = mock_utils.create_order(mock_manager, 6, 6666666)

        return self._get_default_line('summary'), [
            self._get_default_line('header'),
            'Но, боже мой, какая скука',
            self._get_default_line(qty=(self.of.service_id, self.of.service_order_id,
                                        self.ot.service_id, self.ot.service_order_id, 10)),
            'С больным сидеть и день и ночь,',
            'Не отходя ни шагу прочь!',
            self._get_default_line('footer')
        ]

    def get_result(self):
        return [(self.of, self.ot, 10)], [
            COMMENTS['incorrect_row'].format(row='Но, боже мой, какая скука'),
            COMMENTS['incorrect_row'].format(row='С больным сидеть и день и ночь,'),
            COMMENTS['incorrect_row'].format(row='Не отходя ни шагу прочь!',),
        ]


class TransferSingleLineParseFormCase(AbstractSuccessfullParseTestCase):
    _representation = 'transfer_single_line'

    def __init__(self):
        super(TransferSingleLineParseFormCase, self).__init__()
        self.of = None
        self.ot = None

    def get_data(self, mock_manager):
        self.of = mock_utils.create_order(mock_manager, 7, 7777777)
        self.ot = mock_utils.create_order(mock_manager, 6, 6666666)

        return self._get_default_line('summary'), [
            self._get_default_line('header') + ' 7-7777777 6-6666666 10',
            self._get_default_line('footer')
        ]

    def get_result(self):
        return [(self.of, self.ot, 10)], ['']


class OrderNotFoundParseFormCase(AbstractSuccessfullParseTestCase):
    _representation = 'order_not_found'

    def __init__(self):
        super(OrderNotFoundParseFormCase, self).__init__()
        self.of = None
        self.ot = None

    def get_data(self, mock_manager):
        self.of = mock_utils.create_order(mock_manager, 7, 7777777)
        self.ot = mock_utils.create_order(mock_manager, 6, 6666666)

        return self._get_default_line('summary'), [
            self._get_default_line('header'),
            '67-7777777 76-6666666 10',
            '7-7777777 6-6666666 10',
            '77-7777777 67-6666666 10',
            self._get_default_line('footer')
        ]

    def get_result(self):
        return [(self.of, self.ot, 10)], [
            COMMENTS['order_not_found'] % '67-7777777',
            COMMENTS['order_not_found'] % '77-7777777',
        ]


class TransferAllWQtyParseFormCase(AbstractSuccessfullParseTestCase):
    _representation = 'transfer_all_w_qty'

    def __init__(self):
        super(TransferAllWQtyParseFormCase, self).__init__()
        self.of = None
        self.ot = None

    def get_data(self, mock_manager):
        self.of = mock_utils.create_order(mock_manager, 7, 7777777)
        self.ot = mock_utils.create_order(mock_manager, 6, 6666666)

        return self._get_default_line('summary'), [
            self._get_default_line('header'),
            '7-7777777 6-6666666 10',
            self._get_default_line('transfer_all'),
            self._get_default_line('footer')
        ]

    def get_result(self):
        return [(self.of, self.ot, 10)], ['']


class TransferWoAllWoQtyParseFormCase(AbstractSuccessfullParseTestCase):
    _representation = 'transfer_wo_all_wo_qty'

    def __init__(self):
        super(TransferWoAllWoQtyParseFormCase, self).__init__()
        self.of = None
        self.ot = None

    def get_data(self, mock_manager):
        self.of = mock_utils.create_order(mock_manager, 7, 7777777)
        self.ot = mock_utils.create_order(mock_manager, 6, 6666666)

        return self._get_default_line('summary'), [
            self._get_default_line('header'),
            '7-7777777 6-6666666 10',
            '7-7777776 6-6666667',
            self._get_default_line('footer')
        ]

    def get_result(self):
        return [(self.of, self.ot, 10)], [
            COMMENTS['no_sum'].format(row='7-7777776 6-6666667')
        ]


@pytest.mark.parametrize('mock_issue_data',
                         [case() for case in AbstractSuccessfullParseTestCase._cases],
                         ids=lambda case: str(case),
                         indirect=['mock_issue_data'])
def test_parse_succeessfull(mock_queue_object, mock_issue_data):
    issue, case = mock_issue_data

    required_orders, required_comments = case.get_result()

    solver = TransferOrder(mock_queue_object, issue)
    ri = IssueReport()
    ri.comment = ''
    orders_text = solver.check_form(ri)
    orders = solver.parse_issue(ri, orders_text)

    assert set(required_orders) == set(orders)
    assert set(required_comments) == case_utils.prepare_comment(ri.comment)


class AbstractFailedParseTestCase(AbstractParseTestCase):
    _cases = []
    __metaclass__ = case_utils.TestCaseMeta


class NoOrdersFailedParseFormCase(AbstractFailedParseTestCase):
    _representation = 'pushkin'

    def __init__(self):
        super(NoOrdersFailedParseFormCase, self).__init__()

    def get_data(self, mock_manager):
        return self._get_default_line('summary'), [
            self._get_default_line('header'),
            'Мой дядя самых честных правил,',
            self._get_default_line('footer')
        ]

    def get_result(self):
        return [COMMENTS['incorrect_row'].format(row='Мой дядя самых честных правил,')], []


class NoQtyFailedParseFormCase(AbstractFailedParseTestCase):
    _representation = 'no_qty'

    def __init__(self):
        super(NoQtyFailedParseFormCase, self).__init__()
        self.of = None
        self.ot = None

    def get_data(self, mock_manager):
        self.of = mock_utils.create_order(mock_manager, 7, 7777777)
        self.ot = mock_utils.create_order(mock_manager, 6, 7777777)
        return self._get_default_line('summary'), [
            self._get_default_line('header'),
            '7-7777777 6-7777777',
            self._get_default_line('footer')
        ]

    def get_result(self):
        return [COMMENTS['no_sum'].format(row='7-7777777 6-7777777')], []


class OrderFromNotFoundFailedParseFormCase(AbstractFailedParseTestCase):
    _representation = 'order_from_not_found'

    def __init__(self):
        super(OrderFromNotFoundFailedParseFormCase, self).__init__()
        self.of = None
        self.ot = None

    def get_data(self, mock_manager):
        return self._get_default_line('summary'), [
            self._get_default_line('header'),
            '7-77777 6-66666 10',
            self._get_default_line('footer')
        ]

    def get_result(self):
        return [COMMENTS['order_not_found'] % '7-77777'], []


class OrderToNotFoundFailedParseFormCase(AbstractFailedParseTestCase):
    _representation = 'order_to_not_found'

    def __init__(self):
        super(OrderToNotFoundFailedParseFormCase, self).__init__()
        self.of = None
        self.ot = None

    def get_data(self, mock_manager):
        self.of = mock_utils.create_order(mock_manager, 7, 7777777)
        return self._get_default_line('summary'), [
            self._get_default_line('header'),
            '7-7777777 6-66666 10',
            self._get_default_line('footer')
        ]

    def get_result(self):
        return [COMMENTS['order_not_found'] % '6-66666'], []


@pytest.mark.parametrize('mock_issue_data',
                         [case() for case in AbstractFailedParseTestCase._cases],
                         ids=lambda case: str(case),
                         indirect=['mock_issue_data'])
def test_parse_failed(mock_queue_object, mock_issue_data):
    issue, case = mock_issue_data

    required_comments, required_exc_msg = case.get_result()

    solver = TransferOrder(mock_queue_object, issue)
    ri = IssueReport()
    ri.comment = ''
    orders_text = solver.check_form(ri)
    with pytest.raises(ParseException) as exc_info:
        solver.parse_issue(ri, orders_text)

    assert set(required_comments) == case_utils.prepare_comment(ri.comment)
    if required_exc_msg:
        assert set(required_exc_msg) == case_utils.prepare_comment(exc_info.value.message)
    else:
        assert not exc_info.value.message


class RequiredResult(case_utils.RequiredResult):
    def __init__(self, **kwargs):
        super(RequiredResult, self).__init__(**kwargs)
        self.from_states = []
        self.to_states = []

    def set_messages(self, **kwargs):
        self.comments = kwargs.get('messages', [])

    def add_from_state(self, order, consumes_info):
        self.from_states.append((order, list(consumes_info)))

    def add_to_state(self, order, consumes_info):
        state = {i_id: sum(qty for _, qty in rows) for i_id, rows in ut.groupby(consumes_info, lambda (id_, _): id_)}
        self.to_states.append((order, state))

    def add_failed_state(self, order):
        self.add_from_state(order, [(q.invoice.id, q.current_qty) for q in order.consumes])

    def add_transferred_state(self, forder, toder, consumes_qtys):
        zipped = list(itertools.izip_longest(reversed(forder.consumes), consumes_qtys))
        self.add_from_state(forder, reversed([(q.invoice_id, q.current_qty - (qty or 0)) for q, qty in zipped]))
        self.add_to_state(toder, ((q.invoice_id, qty) for q, qty in zipped if qty is not None))

    def fail_check(self, key, forder, torder, params=None):
        self.add_failed_state(forder)
        self.add_failed_state(torder)
        comment = COMMENTS[key].format(**(params or {}))
        self.add_message(COMMENTS['not_transferred_because'].format(f=forder, t=torder, error=comment))

    def fail_transfer(self, reason, forder, torder, free_qty=0):
        self.add_failed_state(forder)
        self.add_failed_state(torder)
        self.add_message(COMMENTS[reason].format(f=forder, t=torder, qty=free_qty))

    def add_comment_format(self, key, **kwargs):
        self.add_message(COMMENTS[key].format(**kwargs))


class AbstractDBTestCase(case_utils.AbstractDBTestCase):
    _summary = 'Перенос средств с заказа на заказ'

    def get_description(self, session):
        orders, transfer_all = self._get_data(session)

        description = u'Данные по заказам (в вышеописаном формате):'
        line_fmt = u'{f.eid} {t.eid} {qty}\n'
        for row in orders:
            row = list(row)
            if len(row) == 2:
                row.append(None)
            description += line_fmt.format(f=row[0], t=row[1], qty=row[2] if row[2] is not None else '')
        if transfer_all:
            description += u'Перенести все свободные средства с заказа на заказ: True\n'

        description += u'Перенос подтверждаю под свою ответственность: True'
        return description

    def setup_config(self, session, config):
        config['responsible_manager'] = 'mscnad7'


class AbstractDBTransferOrderTestCase(AbstractDBTestCase):
    _cases = []
    __metaclass__ = case_utils.TestCaseMeta


# --- Тесты на переносы ---

class TransferAllTest(AbstractDBTransferOrderTestCase):
    _representation = 'transfer_all'

    def __init__(self):
        self.o_from1 = None
        self.o_from2 = None
        self.o_to1 = None
        self.o_to2 = None

    def _get_data(self, session):
        cl, p = db_utils.create_client_person(session)

        self.o_from1 = db_utils.create_order(session, cl)
        self.o_from2 = db_utils.create_order(session, cl)
        self.o_to1 = db_utils.create_order(session, cl)
        self.o_to2 = db_utils.create_order(session, cl)

        i1 = db_utils.create_invoice(session, cl, [(self.o_from1, 20), (self.o_from2, 30)], person=p)
        i2 = db_utils.create_invoice(session, cl, [(self.o_from1, 10), (self.o_from2, 15)], person=p)
        i1.create_receipt(i1.effective_sum)
        i2.create_receipt(i2.effective_sum)

        db_utils.consume_order(i1, self.o_from1, 14)
        db_utils.consume_order(i2, self.o_from1, 7)
        db_utils.consume_order(i2, self.o_from1, 3)
        db_utils.consume_order(i1, self.o_from1, 6)
        self.o_from1.do_process_completion(20)

        db_utils.consume_order(i1, self.o_from2, 22)
        db_utils.consume_order(i2, self.o_from2, 3)
        db_utils.consume_order(i1, self.o_from2, 8)
        db_utils.consume_order(i2, self.o_from2, 12)
        self.o_from2.do_process_completion(26)

        return [(self.o_from1, self.o_to1), (self.o_from2, self.o_to2)], True

    def get_result(self):
        res = RequiredResult(transition=IssueTransitions.fixed)
        res.add_transferred_state(self.o_from1, self.o_to1, [6, 3, 1])
        res.add_transferred_state(self.o_from2, self.o_to2, [12, 7])
        res.add_comment_format('all_transferred', f=self.o_from1, t=self.o_to1)
        res.add_comment_format('all_transferred', f=self.o_from2, t=self.o_to2)
        return res


class TransferQtyTest(AbstractDBTransferOrderTestCase):
    _representation = 'transfer_qty'

    def __init__(self):
        self.o_from1 = None
        self.o_from2 = None
        self.o_to1 = None
        self.o_to2 = None

    def _get_data(self, session):
        cl, p = db_utils.create_client_person(session)

        self.o_from1 = db_utils.create_order(session, cl)
        self.o_from2 = db_utils.create_order(session, cl)
        self.o_to1 = db_utils.create_order(session, cl)
        self.o_to2 = db_utils.create_order(session, cl)

        i1 = db_utils.create_invoice(session, cl, [(self.o_from1, 20), (self.o_from2, 30)], person=p)
        i2 = db_utils.create_invoice(session, cl, [(self.o_from1, 10), (self.o_from2, 15)], person=p)
        i1.create_receipt(i1.effective_sum)
        i2.create_receipt(i2.effective_sum)

        db_utils.consume_order(i1, self.o_from1, 14)
        db_utils.consume_order(i2, self.o_from1, 7)
        db_utils.consume_order(i2, self.o_from1, 3)
        db_utils.consume_order(i1, self.o_from1, 6)
        self.o_from1.do_process_completion(20)

        db_utils.consume_order(i1, self.o_from2, 22)
        db_utils.consume_order(i2, self.o_from2, 3)
        db_utils.consume_order(i1, self.o_from2, 8)
        db_utils.consume_order(i2, self.o_from2, 12)
        self.o_from2.do_process_completion(26)

        return [
                   (self.o_from1, self.o_to1, decimal.Decimal('7.01')),
                   (self.o_from2, self.o_to2, decimal.Decimal('20.666'))
               ], False

    def get_result(self):
        res = RequiredResult(transition=IssueTransitions.fixed)
        res.add_transferred_state(self.o_from1, self.o_to1, [6, decimal.Decimal('1.01')])
        res.fail_transfer('not_enough_money', self.o_from2, self.o_to2, 19)
        res.add_comment_format('sum_transferred', f=self.o_from1, t=self.o_to1, qty=decimal.Decimal('7.01'))
        return res


class TransferQtyCashbackTest(AbstractDBTransferOrderTestCase):
    _representation = 'transfer_qty_cashback'

    def __init__(self):
        self.o_from1 = None
        self.o_from2 = None
        self.o_to1 = None
        self.o_to2 = None

    def _get_data(self, session):
        cl, p = db_utils.create_client_person(session)

        self.o_from1 = db_utils.create_order(session, cl)
        self.o_from2 = db_utils.create_order(session, cl)
        self.o_to1 = db_utils.create_order(session, cl)
        self.o_to2 = db_utils.create_order(session, cl)

        i1 = db_utils.create_invoice(session, cl, [(self.o_from1, 20), (self.o_from2, 30)], person=p)
        i2 = db_utils.create_invoice(session, cl, [(self.o_from1, 10), (self.o_from2, 15)], person=p)
        i1.create_receipt(i1.effective_sum)
        i2.create_receipt(i2.effective_sum)

        db_utils.consume_order(i1, self.o_from1, 14)
        db_utils.consume_order(i2, self.o_from1, 7)
        db_utils.consume_order(i2, self.o_from1, 3)
        db_utils.consume_order(i1, self.o_from1, 6)
        self.o_from1.do_process_completion(20)

        cashback = ob.ClientCashbackBuilder.construct(
            session,
            client=self.o_from2.client,
            service_id=self.o_from2.service_id,
            iso_currency=self.o_from2.product_iso_currency,
            bonus=500,
        )
        cashback_usage = ob.CashbackUsageBuilder.construct(session, client_cashback=cashback)
        discount_obj = mapper.DiscountObj(0, 0, None, cashback_base=1, cashback_bonus=1,
                                          cashback_usage_id=cashback_usage.id)

        db_utils.consume_order(i1, self.o_from2, 22, discount_obj=discount_obj)
        db_utils.consume_order(i2, self.o_from2, 3)
        db_utils.consume_order(i1, self.o_from2, 8)
        db_utils.consume_order(i2, self.o_from2, 12, discount_obj=discount_obj)
        self.o_from2.do_process_completion(26)

        return [
                   (self.o_from1, self.o_to1, decimal.Decimal('7.01')),
                   (self.o_from2, self.o_to2, decimal.Decimal('20'))
               ], False

    def get_result(self):
        res = RequiredResult(transition=IssueTransitions.fixed)
        res.add_transferred_state(self.o_from1, self.o_to1, [6, decimal.Decimal('1.01')])
        res.fail_transfer('not_enough_money', self.o_from2, self.o_to2, 19)
        res.add_comment_format('sum_transferred', f=self.o_from1, t=self.o_to1, qty=decimal.Decimal('7.01'))
        return res


class NotEnoughFundsCashbackTransferDenied(AbstractDBTransferOrderTestCase):
    _representation = 'not_enough_money_cashback_denied'

    def __init__(self):
        self.o_from = None
        self.o_to = None

    def _get_data(self, session):
        session.config.__dict__['CASHBACK_TURN_ON_SKIP_CASHBACK4CLIENTS_IN_TRANSFER'] = True
        agency = db_utils.create_client(session, create_agency=True, is_agency=1)
        cl = db_utils.create_client(session, name='lol', agency=agency)
        cl2 = db_utils.create_client(session, name='kek', agency=agency)
        self.o_from = db_utils.create_order(session, cl, agency=agency, service_id=7)
        self.o_to = db_utils.create_order(session, cl2, agency=agency, service_id=7)
        i1 = db_utils.create_invoice(session, agency, [(self.o_from, 10)])
        i1.create_receipt(i1.effective_sum)

        cashback = ob.ClientCashbackBuilder.construct(
            session,
            client=self.o_from.client,
            service_id=self.o_from.service_id,
            iso_currency=self.o_from.product_iso_currency,
            bonus=500,
        )
        cashback_usage = ob.CashbackUsageBuilder.construct(session, client_cashback=cashback)
        discount_obj = mapper.DiscountObj(0, 0, None, cashback_base=1, cashback_bonus=1,
                                          cashback_usage_id=cashback_usage.id)
        q = db_utils.consume_order(i1, self.o_from, 6, discount_obj=discount_obj)
        self.o_from.do_process_completion(5)
        assert q.cashback_usage_id

        db_utils.consume_order(i1, self.o_from, 4)

        return [(self.o_from, self.o_to, 5)], False

    def get_result(self):
        res = RequiredResult(transition=IssueTransitions.wont_fix)
        res.add_message(COMMENTS.get('not_enough_money_cashback'))
        return res


class NotEnoughFundsCashbackTransferAllowed(AbstractDBTransferOrderTestCase):
    _representation = 'not_enough_money_cashback_allowed'

    def __init__(self):
        self.o_from = None
        self.o_to = None

    def _get_data(self, session):
        session.config.__dict__['CASHBACK_TURN_ON_SKIP_CASHBACK4CLIENTS_IN_TRANSFER'] = False
        agency = db_utils.create_client(session, create_agency=True, is_agency=1)
        cl = db_utils.create_client(session, name='lol', agency=agency)
        cl2 = db_utils.create_client(session, name='kek', agency=agency)
        self.o_from = db_utils.create_order(session, cl, agency=agency, service_id=7)
        self.o_to = db_utils.create_order(session, cl2, agency=agency, service_id=7)
        i1 = db_utils.create_invoice(session, agency, [(self.o_from, 10)])
        i1.create_receipt(i1.effective_sum)

        cashback = ob.ClientCashbackBuilder.construct(
            session,
            client=self.o_from.client,
            service_id=self.o_from.service_id,
            iso_currency=self.o_from.product_iso_currency,
            bonus=500,
        )
        cashback_usage = ob.CashbackUsageBuilder.construct(session, client_cashback=cashback)
        discount_obj = mapper.DiscountObj(0, 0, None, cashback_base=1, cashback_bonus=1,
                                          cashback_usage_id=cashback_usage.id)
        q = db_utils.consume_order(i1, self.o_from, 6, discount_obj=discount_obj)
        self.o_from.do_process_completion(5)
        assert q.cashback_usage_id

        db_utils.consume_order(i1, self.o_from, 4)

        return [(self.o_from, self.o_to, 5)], False

    def get_result(self):
        res = RequiredResult(transition=IssueTransitions.wont_fix)
        res.add_message(COMMENTS.get('cash_back_transfer_denied'))
        return res


class TransferRoundedTest(AbstractDBTransferOrderTestCase):
    _representation = 'transfer_qty_rounded'

    def __init__(self):
        self.o_from1 = None
        self.o_from2 = None
        self.o_to1 = None
        self.o_to2 = None

    def _get_data(self, session):
        cl, p = db_utils.create_client_person(session)

        self.o_from1 = db_utils.create_order(session, cl, product_id=503300)
        self.o_from2 = db_utils.create_order(session, cl, product_id=503300)
        self.o_from3 = db_utils.create_order(session, cl, product_id=503300)
        self.o_to1 = db_utils.create_order(session, cl, product_id=503300)
        self.o_to2 = db_utils.create_order(session, cl, product_id=503300)

        inv_orders = [(self.o_from1, 4), (self.o_from2, 6), (self.o_from3, 6)]
        db_utils.create_invoice(session, cl, inv_orders, person=p, turn_on=True)

        return [
                   (self.o_from1, self.o_to1, decimal.Decimal('3.2')),
                   (self.o_from2, self.o_to2, decimal.Decimal('0.4')),
                   (self.o_from3, self.o_to2, 0)
               ], False

    def get_result(self):
        res = RequiredResult(transition=IssueTransitions.fixed)
        res.add_transferred_state(self.o_from1, self.o_to1, [3])
        res.add_failed_state(self.o_from2)
        res.add_failed_state(self.o_from3)
        res.add_failed_state(self.o_to2)
        res.add_comment_format('sum_transferred', f=self.o_from1, t=self.o_to1, qty=3)
        res.add_comment_format('wrong_sum_prec', f=self.o_from2, t=self.o_to2, qty=decimal.Decimal('0.4'))
        res.add_comment_format('wrong_sum_prec', f=self.o_from3, t=self.o_to2, qty=0)
        return res


class NotTransferActedAllTest(AbstractDBTransferOrderTestCase):
    _representation = 'not_transfer_acted_all'

    def __init__(self):
        self.o_from = None
        self.o_to = None

    def _get_data(self, session):
        cl, p = db_utils.create_client_person(session, person_type='ur')

        self.o_from = db_utils.create_order(session, cl)
        self.o_to = db_utils.create_order(session, cl)

        month = mapper.ActMonth()

        i1 = db_utils.create_invoice(session, cl, [(self.o_from, 10)], person=p, turn_on=True, dt=month.begin_dt)
        i2 = db_utils.create_invoice(session, cl, [(self.o_from, 10)], person=p, turn_on=True, dt=month.begin_dt)
        i3 = db_utils.create_invoice(session, cl, [(self.o_from, 10)], person=p, turn_on=True, dt=month.begin_dt)

        self.o_from.calculate_consumption(month.begin_dt, {self.o_from.shipment_type: 17})
        i2.generate_act(force=True, backdate=month.document_dt)

        self.o_from.calculate_consumption(month.begin_dt, {self.o_from.shipment_type: 5})
        i1.generate_act(force=True, backdate=month.document_dt)

        self.o_from.calculate_consumption(month.begin_dt, {self.o_from.shipment_type: 6})

        return [(self.o_from, self.o_to)], True

    def get_result(self):
        res = RequiredResult(transition=IssueTransitions.fixed)
        res.add_transferred_state(self.o_from, self.o_to, [10, 3, 4])
        res.add_comment_format('all_transferred', f=self.o_from, t=self.o_to)
        return res


# class NotTransferActedAllFailTest(AbstractDBTransferOrderTestCase):
#     _representation = 'not_transfer_acted_all_fail'
#
#     def __init__(self):
#         self.o_from = None
#         self.o_to = None
#
#     def _get_data(self, session):
#         cl, p = db_utils.create_client_person(session, person_type='ur')
#
#         self.o_from = db_utils.create_order(session, cl)
#         self.o_to = db_utils.create_order(session, cl)
#         o_add = db_utils.create_order(session, cl)
#
#         month = mapper.ActMonth()
#
#         i1 = db_utils.create_invoice(session, cl, [(self.o_from, 10)], person=p, turn_on=True, dt=month.begin_dt)
#         i2 = db_utils.create_invoice(session, cl, [(self.o_from, 10)], person=p, turn_on=True, dt=month.begin_dt)
#
#         self.o_from.calculate_consumption(month.begin_dt, {self.o_from.shipment_type: 17})
#         i2.generate_act(force=True, backdate=month.document_dt)
#
#         self.o_from.calculate_consumption(month.begin_dt, {self.o_from.shipment_type: 5})
#         i1.generate_act(force=True, backdate=month.document_dt)
#
#         self.o_from.calculate_consumption(month.begin_dt, {self.o_from.shipment_type: 6})
#
#         consumption.reverse_consume(i2.consumes[0], None, 8)
#         i2.transfer(o_add)
#
#         return [(self.o_from, self.o_to)], True
#
#     def get_result(self):
#         res = RequiredResult(transition=IssueTransitions.wont_fix)
#         res.fail_transfer('overacted_fta', self.o_from, self.o_to)
#         return res


class NotTransferActedQtyOkTest(AbstractDBTransferOrderTestCase):
    _representation = 'not_transfer_acted_qty_ok'

    def __init__(self):
        self.o_from = None
        self.o_to = None

    def _get_data(self, session):
        cl, p = db_utils.create_client_person(session, person_type='ur')

        self.o_from = db_utils.create_order(session, cl)
        self.o_to = db_utils.create_order(session, cl)

        month = mapper.ActMonth()

        i1 = db_utils.create_invoice(session, cl, [(self.o_from, 10)], person=p, turn_on=True, dt=month.begin_dt)
        i2 = db_utils.create_invoice(session, cl, [(self.o_from, 10)], person=p, turn_on=True, dt=month.begin_dt)
        i3 = db_utils.create_invoice(session, cl, [(self.o_from, 10)], person=p, turn_on=True, dt=month.begin_dt)

        self.o_from.calculate_consumption(month.begin_dt, {self.o_from.shipment_type: 17})
        i2.generate_act(force=True, backdate=month.document_dt)

        self.o_from.calculate_consumption(month.begin_dt, {self.o_from.shipment_type: 5})
        i1.generate_act(force=True, backdate=month.document_dt)

        self.o_from.calculate_consumption(month.begin_dt, {self.o_from.shipment_type: 6})

        return [(self.o_from, self.o_to, 15)], False

    def get_result(self):
        res = RequiredResult(transition=IssueTransitions.fixed)
        res.add_transferred_state(self.o_from, self.o_to, [10, 3, 2])
        res.add_comment_format('sum_transferred', f=self.o_from, t=self.o_to, qty=15)
        return res


class NotTransferActedQtyFailTest(AbstractDBTransferOrderTestCase):
    _representation = 'not_transfer_acted_qty_fail'

    def __init__(self):
        self.o_from = None
        self.o_to = None

    def _get_data(self, session):
        cl, p = db_utils.create_client_person(session, person_type='ur')

        self.o_from = db_utils.create_order(session, cl)
        self.o_to = db_utils.create_order(session, cl)

        month = mapper.ActMonth()

        i1 = db_utils.create_invoice(session, cl, [(self.o_from, 10)], person=p, turn_on=True, dt=month.begin_dt)
        i2 = db_utils.create_invoice(session, cl, [(self.o_from, 10)], person=p, turn_on=True, dt=month.begin_dt)
        i3 = db_utils.create_invoice(session, cl, [(self.o_from, 10)], person=p, turn_on=True, dt=month.begin_dt)

        self.o_from.calculate_consumption(month.begin_dt, {self.o_from.shipment_type: 17})
        i2.generate_act(force=True, backdate=month.document_dt)

        self.o_from.calculate_consumption(month.begin_dt, {self.o_from.shipment_type: 5})
        i1.generate_act(force=True, backdate=month.document_dt)

        self.o_from.calculate_consumption(month.begin_dt, {self.o_from.shipment_type: 6})

        return [(self.o_from, self.o_to, 18)], False

    def get_result(self):
        res = RequiredResult(transition=IssueTransitions.wont_fix)
        res.fail_transfer('not_enough_money', self.o_from, self.o_to, 17)
        return res


class TransferActedAllTest(AbstractDBTransferOrderTestCase):
    _representation = 'transfer_acted_all'

    def __init__(self):
        self.o_from = None
        self.o_to = None

    def _get_data(self, session):
        cl, p = db_utils.create_client_person(session, person_type='ph')

        self.o_from = db_utils.create_order(session, cl)
        self.o_to = db_utils.create_order(session, cl)

        month = mapper.ActMonth()

        i1 = db_utils.create_invoice(session, cl, [(self.o_from, 10)], person=p, turn_on=True, dt=month.begin_dt)
        i2 = db_utils.create_invoice(session, cl, [(self.o_from, 10)], person=p, turn_on=True, dt=month.begin_dt)
        i3 = db_utils.create_invoice(session, cl, [(self.o_from, 10)], person=p, turn_on=True, dt=month.begin_dt)

        self.o_from.calculate_consumption(month.begin_dt, {self.o_from.shipment_type: 17})
        i2.generate_act(force=True, backdate=month.document_dt)

        self.o_from.calculate_consumption(month.begin_dt, {self.o_from.shipment_type: 5})
        i1.generate_act(force=True, backdate=month.document_dt)

        self.o_from.calculate_consumption(month.begin_dt, {self.o_from.shipment_type: 6})

        return [(self.o_from, self.o_to)], True

    def get_result(self):
        res = RequiredResult(transition=IssueTransitions.fixed)
        res.add_transferred_state(self.o_from, self.o_to, [10, 10, 4])
        res.add_comment_format('all_transferred', f=self.o_from, t=self.o_to)
        return res


class TransferActedQtyOkTest(AbstractDBTransferOrderTestCase):
    _representation = 'transfer_acted_qty_ok'

    def __init__(self):
        self.o_from = None
        self.o_to = None

    def _get_data(self, session):
        cl, p = db_utils.create_client_person(session, person_type='ph')

        self.o_from = db_utils.create_order(session, cl)
        self.o_to = db_utils.create_order(session, cl)

        month = mapper.ActMonth()

        i1 = db_utils.create_invoice(session, cl, [(self.o_from, 10)], person=p, turn_on=True, dt=month.begin_dt)
        i2 = db_utils.create_invoice(session, cl, [(self.o_from, 10)], person=p, turn_on=True, dt=month.begin_dt)
        i3 = db_utils.create_invoice(session, cl, [(self.o_from, 10)], person=p, turn_on=True, dt=month.begin_dt)

        self.o_from.calculate_consumption(month.begin_dt, {self.o_from.shipment_type: 17})
        i2.generate_act(force=True, backdate=month.document_dt)

        self.o_from.calculate_consumption(month.begin_dt, {self.o_from.shipment_type: 5})
        i1.generate_act(force=True, backdate=month.document_dt)

        self.o_from.calculate_consumption(month.begin_dt, {self.o_from.shipment_type: 6})

        return [(self.o_from, self.o_to, 23)], False

    def get_result(self):
        res = RequiredResult(transition=IssueTransitions.fixed)
        res.add_transferred_state(self.o_from, self.o_to, [10, 10, 3])
        res.add_comment_format('sum_transferred', f=self.o_from, t=self.o_to, qty=23)
        return res


class TransferActedQtyFailTest(AbstractDBTransferOrderTestCase):
    _representation = 'transfer_acted_qty_fail'

    def __init__(self):
        self.o_from = None
        self.o_to = None

    def _get_data(self, session):
        cl, p = db_utils.create_client_person(session, person_type='ph')

        self.o_from = db_utils.create_order(session, cl)
        self.o_to = db_utils.create_order(session, cl)

        month = mapper.ActMonth()

        i1 = db_utils.create_invoice(session, cl, [(self.o_from, 10)], person=p, turn_on=True, dt=month.begin_dt)
        i2 = db_utils.create_invoice(session, cl, [(self.o_from, 10)], person=p, turn_on=True, dt=month.begin_dt)
        i3 = db_utils.create_invoice(session, cl, [(self.o_from, 10)], person=p, turn_on=True, dt=month.begin_dt)

        self.o_from.calculate_consumption(month.begin_dt, {self.o_from.shipment_type: 17})
        i2.generate_act(force=True, backdate=month.document_dt)

        self.o_from.calculate_consumption(month.begin_dt, {self.o_from.shipment_type: 5})
        i1.generate_act(force=True, backdate=month.document_dt)

        self.o_from.calculate_consumption(month.begin_dt, {self.o_from.shipment_type: 6})

        return [(self.o_from, self.o_to, 25)], False

    def get_result(self):
        res = RequiredResult(transition=IssueTransitions.wont_fix)
        res.fail_transfer('not_enough_money', self.o_from, self.o_to, 24)
        return res


class VariedTAQtyOkTest(AbstractDBTransferOrderTestCase):
    _representation = 'varied_ta_qty_ok'

    def __init__(self):
        self.o_from = None
        self.o_to = None

    def _get_data(self, session):
        cl, p_ph = db_utils.create_client_person(session, person_type='ph')
        p_ur = db_utils.create_person(session, cl, person_type='ur')

        self.o_from = db_utils.create_order(session, cl)
        self.o_to = db_utils.create_order(session, cl)

        month = mapper.ActMonth()

        i1 = db_utils.create_invoice(session, cl, [(self.o_from, 10)], person=p_ur, turn_on=True, dt=month.begin_dt)
        i2 = db_utils.create_invoice(session, cl, [(self.o_from, 10)], person=p_ph, turn_on=True, dt=month.begin_dt)
        i3 = db_utils.create_invoice(session, cl, [(self.o_from, 10)], person=p_ur, turn_on=True, dt=month.begin_dt)

        self.o_from.calculate_consumption(month.begin_dt, {self.o_from.shipment_type: 17})
        i2.generate_act(force=True, backdate=month.document_dt)

        self.o_from.calculate_consumption(month.begin_dt, {self.o_from.shipment_type: 6})
        i1.generate_act(force=True, backdate=month.document_dt)

        self.o_from.calculate_consumption(month.begin_dt, {self.o_from.shipment_type: 5})

        return [(self.o_from, self.o_to, 19)], False

    def get_result(self):
        res = RequiredResult(transition=IssueTransitions.fixed)
        res.add_transferred_state(self.o_from, self.o_to, [10, 9])
        res.add_comment_format('sum_transferred', f=self.o_from, t=self.o_to, qty=19)
        return res


class VariedTAQtyOveractedOkTest(AbstractDBTransferOrderTestCase):
    _representation = 'varied_ta_qty_overacted_ok'

    def __init__(self):
        self.o_from = None
        self.o_to = None

    def _get_data(self, session):
        cl, p_ph = db_utils.create_client_person(session, person_type='ph')
        p_ur = db_utils.create_person(session, cl, person_type='ur')

        self.o_from = db_utils.create_order(session, cl)
        self.o_to = db_utils.create_order(session, cl)

        month = mapper.ActMonth()

        i1 = db_utils.create_invoice(session, cl, [(self.o_from, 10)], person=p_ur, turn_on=True, dt=month.begin_dt)
        i2 = db_utils.create_invoice(session, cl, [(self.o_from, 10)], person=p_ph, turn_on=True, dt=month.begin_dt)
        i3 = db_utils.create_invoice(session, cl, [(self.o_from, 10)], person=p_ur, turn_on=True, dt=month.begin_dt)

        self.o_from.calculate_consumption(month.begin_dt, {self.o_from.shipment_type: 17})
        i2.generate_act(force=True, backdate=month.document_dt)

        self.o_from.calculate_consumption(month.begin_dt, {self.o_from.shipment_type: 7})
        i1.generate_act(force=True, backdate=month.document_dt)

        self.o_from.calculate_consumption(month.begin_dt, {self.o_from.shipment_type: 5})

        return [(self.o_from, self.o_to, 23)], False

    def get_result(self):
        res = RequiredResult(transition=IssueTransitions.fixed)
        res.add_transferred_state(self.o_from, self.o_to, [10, 10, 3])
        res.add_comment_format('sum_transferred', f=self.o_from, t=self.o_to, qty=23)
        return res


class VariedTAQtyOveractedFailTest(AbstractDBTransferOrderTestCase):
    _representation = 'varied_ta_qty_overacted_fail'

    def __init__(self):
        self.o_from = None
        self.o_to = None

    def _get_data(self, session):
        cl, p_ph = db_utils.create_client_person(session, person_type='ph')
        p_ur = db_utils.create_person(session, cl, person_type='ur')

        self.o_from = db_utils.create_order(session, cl)
        self.o_to = db_utils.create_order(session, cl)

        month = mapper.ActMonth()

        i1 = db_utils.create_invoice(session, cl, [(self.o_from, 10)], person=p_ur, turn_on=True, dt=month.begin_dt)
        i2 = db_utils.create_invoice(session, cl, [(self.o_from, 10)], person=p_ph, turn_on=True, dt=month.begin_dt)
        i3 = db_utils.create_invoice(session, cl, [(self.o_from, 10)], person=p_ur, turn_on=True, dt=month.begin_dt)

        self.o_from.calculate_consumption(month.begin_dt, {self.o_from.shipment_type: 17})
        i2.generate_act(force=True, backdate=month.document_dt)

        self.o_from.calculate_consumption(month.begin_dt, {self.o_from.shipment_type: 7})
        i1.generate_act(force=True, backdate=month.document_dt)

        self.o_from.calculate_consumption(month.begin_dt, {self.o_from.shipment_type: 5})

        return [(self.o_from, self.o_to, 24)], False

    def get_result(self):
        res = RequiredResult(transition=IssueTransitions.wont_fix)
        res.fail_transfer('not_enough_money', self.o_from, self.o_to, 23)
        return res


class VariedTAAllOkTest(AbstractDBTransferOrderTestCase):
    _representation = 'varied_ta_all_ok'

    def __init__(self):
        self.o_from = None
        self.o_to = None

    def _get_data(self, session):
        cl, p_ph = db_utils.create_client_person(session, person_type='ph')
        p_ur = db_utils.create_person(session, cl, person_type='ur')

        self.o_from = db_utils.create_order(session, cl)
        self.o_to = db_utils.create_order(session, cl)

        month = mapper.ActMonth()

        i1 = db_utils.create_invoice(session, cl, [(self.o_from, 10)], person=p_ur, turn_on=True, dt=month.begin_dt)
        i2 = db_utils.create_invoice(session, cl, [(self.o_from, 10)], person=p_ph, turn_on=True, dt=month.begin_dt)
        i3 = db_utils.create_invoice(session, cl, [(self.o_from, 10)], person=p_ur, turn_on=True, dt=month.begin_dt)

        self.o_from.calculate_consumption(month.begin_dt, {self.o_from.shipment_type: 17})
        i2.generate_act(force=True, backdate=month.document_dt)

        self.o_from.calculate_consumption(month.begin_dt, {self.o_from.shipment_type: 6})
        i1.generate_act(force=True, backdate=month.document_dt)

        self.o_from.calculate_consumption(month.begin_dt, {self.o_from.shipment_type: 5})

        return [(self.o_from, self.o_to)], True

    def get_result(self):
        res = RequiredResult(transition=IssueTransitions.fixed)
        res.add_transferred_state(self.o_from, self.o_to, [10, 10, 4])
        res.add_comment_format('all_transferred', f=self.o_from, t=self.o_to,)
        return res


class SplitActTAAllOkTest(AbstractDBTransferOrderTestCase):
    _representation = 'split_act_ta_all_ok'

    def __init__(self):
        self.o_from = None
        self.o_to = None

    def _get_data(self, session):
        cl, p = db_utils.create_client_person(session, person_type='ph')

        self.o_from = db_utils.create_order(session, cl)
        self.o_to = db_utils.create_order(session, cl)

        month = mapper.ActMonth()

        i = db_utils.create_invoice(session, cl, [(self.o_from, 10)], person=p, dt=month.begin_dt)
        i.create_receipt(i.effective_sum)
        q1 = db_utils.consume_order(i, self.o_from, 4)
        q2 = db_utils.consume_order(i, self.o_from, 3)
        q3 = db_utils.consume_order(i, self.o_from, 3)

        self.o_from.calculate_consumption(month.begin_dt, {self.o_from.shipment_type: 4})
        i.generate_act(force=True, backdate=month.document_dt)

        self.o_from.calculate_consumption(month.begin_dt, {self.o_from.shipment_type: 2})
        consumption.reverse_consume(q1, None, 2)

        return [(self.o_from, self.o_to)], True

    def get_result(self):
        res = RequiredResult(transition=IssueTransitions.fixed)
        res.add_transferred_state(self.o_from, self.o_to, [3, 3])
        res.add_comment_format('all_transferred', f=self.o_from, t=self.o_to)
        return res


class SplitActTAQtyOkTest(AbstractDBTransferOrderTestCase):
    _representation = 'split_act_ta_qty_ok'

    def __init__(self):
        self.o_from = None
        self.o_to = None

    def _get_data(self, session):
        cl, p = db_utils.create_client_person(session, person_type='ph')

        self.o_from = db_utils.create_order(session, cl)
        self.o_to = db_utils.create_order(session, cl)

        month = mapper.ActMonth()

        i = db_utils.create_invoice(session, cl, [(self.o_from, 10)], person=p, dt=month.begin_dt)
        i.create_receipt(i.effective_sum)
        q1 = db_utils.consume_order(i, self.o_from, 4)
        q2 = db_utils.consume_order(i, self.o_from, 3)
        q3 = db_utils.consume_order(i, self.o_from, 3)

        self.o_from.calculate_consumption(month.begin_dt, {self.o_from.shipment_type: 4})
        i.generate_act(force=True, backdate=month.document_dt)

        self.o_from.calculate_consumption(month.begin_dt, {self.o_from.shipment_type: 2})
        consumption.reverse_consume(q1, None, 2)

        return [(self.o_from, self.o_to, 5)], False

    def get_result(self):
        res = RequiredResult(transition=IssueTransitions.fixed)
        res.add_transferred_state(self.o_from, self.o_to, [3, 2])
        res.add_comment_format('sum_transferred', f=self.o_from, t=self.o_to, qty=5)
        return res


class SplitActTAQtyFailTest(AbstractDBTransferOrderTestCase):
    _representation = 'split_act_ta_qty_fail'

    def __init__(self):
        self.o_from = None
        self.o_to = None

    def _get_data(self, session):
        cl, p = db_utils.create_client_person(session, person_type='ph')

        self.o_from = db_utils.create_order(session, cl)
        self.o_to = db_utils.create_order(session, cl)

        month = mapper.ActMonth()

        i = db_utils.create_invoice(session, cl, [(self.o_from, 10)], person=p, dt=month.begin_dt)
        i.create_receipt(i.effective_sum)
        q1 = db_utils.consume_order(i, self.o_from, 4)
        q2 = db_utils.consume_order(i, self.o_from, 3)
        q3 = db_utils.consume_order(i, self.o_from, 3)

        self.o_from.calculate_consumption(month.begin_dt, {self.o_from.shipment_type: 4})
        i.generate_act(force=True, backdate=month.document_dt)

        self.o_from.calculate_consumption(month.begin_dt, {self.o_from.shipment_type: 2})
        consumption.reverse_consume(q1, None, 2)

        return [(self.o_from, self.o_to, 7)], False

    def get_result(self):
        res = RequiredResult(transition=IssueTransitions.wont_fix)
        res.fail_transfer('not_enough_money', self.o_from, self.o_to, 6)
        return res


class SplitActNotTAAllOkTest(AbstractDBTransferOrderTestCase):
    _representation = 'split_act_not_ta_all_fail'

    def __init__(self):
        self.o_from = None
        self.o_to = None

    def _get_data(self, session):
        cl, p = db_utils.create_client_person(session, person_type='ur')

        self.o_from = db_utils.create_order(session, cl)
        self.o_to = db_utils.create_order(session, cl)

        month = mapper.ActMonth()

        i = db_utils.create_invoice(session, cl, [(self.o_from, 10)], person=p, dt=month.begin_dt)
        i.create_receipt(i.effective_sum)
        q1 = db_utils.consume_order(i, self.o_from, 4)
        q2 = db_utils.consume_order(i, self.o_from, 3)
        q3 = db_utils.consume_order(i, self.o_from, 3)

        self.o_from.calculate_consumption(month.begin_dt, {self.o_from.shipment_type: 4})
        i.generate_act(force=True, backdate=month.document_dt)

        self.o_from.calculate_consumption(month.begin_dt, {self.o_from.shipment_type: 2})
        consumption.reverse_consume(q1, None, 2)

        return [(self.o_from, self.o_to)], True

    def get_result(self):
        res = RequiredResult(transition=IssueTransitions.wont_fix)
        res.fail_transfer('overacted', self.o_from, self.o_to)
        return res


class SplitActNotTAQtyOkTest(AbstractDBTransferOrderTestCase):
    _representation = 'split_act_not_ta_qty_ok'

    def __init__(self):
        self.o_from = None
        self.o_to = None

    def _get_data(self, session):
        cl, p = db_utils.create_client_person(session, person_type='ur')

        self.o_from = db_utils.create_order(session, cl)
        self.o_to = db_utils.create_order(session, cl)

        month = mapper.ActMonth()

        i = db_utils.create_invoice(session, cl, [(self.o_from, 10)], person=p, dt=month.begin_dt)
        i.create_receipt(i.effective_sum)
        q1 = db_utils.consume_order(i, self.o_from, 4)
        q2 = db_utils.consume_order(i, self.o_from, 3)
        q3 = db_utils.consume_order(i, self.o_from, 3)

        self.o_from.calculate_consumption(month.begin_dt, {self.o_from.shipment_type: 4})
        i.generate_act(force=True, backdate=month.document_dt)

        self.o_from.calculate_consumption(month.begin_dt, {self.o_from.shipment_type: 3})
        consumption.reverse_consume(q1, None, 1)

        return [(self.o_from, self.o_to, 5)], False

    def get_result(self):
        res = RequiredResult(transition=IssueTransitions.fixed)
        res.add_transferred_state(self.o_from, self.o_to, [3, 2])
        res.add_comment_format('sum_transferred', f=self.o_from, t=self.o_to, qty=5)
        return res


class SplitActNotTAQtyFailTest(AbstractDBTransferOrderTestCase):
    _representation = 'split_act_not_ta_qty_fail'

    def __init__(self):
        self.o_from = None
        self.o_to = None

    def _get_data(self, session):
        cl, p = db_utils.create_client_person(session, person_type='ur')

        self.o_from = db_utils.create_order(session, cl)
        self.o_to = db_utils.create_order(session, cl)

        month = mapper.ActMonth()

        i = db_utils.create_invoice(session, cl, [(self.o_from, 10)], person=p, dt=month.begin_dt)
        i.create_receipt(i.effective_sum)
        q1 = db_utils.consume_order(i, self.o_from, 4)
        q2 = db_utils.consume_order(i, self.o_from, 3)
        q3 = db_utils.consume_order(i, self.o_from, 3)

        self.o_from.calculate_consumption(month.begin_dt, {self.o_from.shipment_type: 4})
        i.generate_act(force=True, backdate=month.document_dt)

        self.o_from.calculate_consumption(month.begin_dt, {self.o_from.shipment_type: 3})
        consumption.reverse_consume(q1, None, 1)

        return [(self.o_from, self.o_to, 6)], False

    def get_result(self):
        res = RequiredResult(transition=IssueTransitions.wont_fix)
        res.fail_transfer('overacted', self.o_from, self.o_to)
        return res


class SplitActNotTAQtyUnactedOkTest(AbstractDBTransferOrderTestCase):
    _representation = 'split_act_not_ta_qty_unacted_ok'

    def __init__(self):
        self.o_from = None
        self.o_to = None

    def _get_data(self, session):
        cl, p = db_utils.create_client_person(session, person_type='ur')

        self.o_from = db_utils.create_order(session, cl)
        self.o_to = db_utils.create_order(session, cl)

        month = mapper.ActMonth()

        i = db_utils.create_invoice(session, cl, [(self.o_from, 10)], person=p, dt=month.begin_dt)
        i.create_receipt(i.effective_sum)
        q1 = db_utils.consume_order(i, self.o_from, 4)
        q2 = db_utils.consume_order(i, self.o_from, 3)
        q3 = db_utils.consume_order(i, self.o_from, 3)

        self.o_from.calculate_consumption(month.begin_dt, {self.o_from.shipment_type: 4})
        i.generate_act(force=True, backdate=month.document_dt)

        self.o_from.calculate_consumption(month.begin_dt, {self.o_from.shipment_type: 3})

        return [(self.o_from, self.o_to, 6)], False

    def get_result(self):
        res = RequiredResult(transition=IssueTransitions.fixed)
        res.add_transferred_state(self.o_from, self.o_to, [3, 3])
        res.add_comment_format('sum_transferred', f=self.o_from, t=self.o_to, qty=6)
        return res


class AbstactSubclientsOverachCheckTestCase(AbstractDBTransferOrderTestCase):
    @staticmethod
    def _mk_overact(invoice, order, month, qty):
        consume = db_utils.consume_order(invoice, order, qty)
        order.calculate_consumption(month.begin_dt, {order.shipment_type: qty})
        invoice.generate_act(force=True, backdate=month.document_dt)
        order.calculate_consumption(month.begin_dt, {order.shipment_type: 0})
        consumption.reverse_consume(consume, None, qty)


class TransferAllSubclientsOveractCheckTest(AbstactSubclientsOverachCheckTestCase):
    _representation = 'transfer_all_subclients_overact_check'

    def __init__(self):
        self.o_from = None
        self.o_to = None

    def _get_data(self, session):
        ag, p = db_utils.create_client_person(session, person_type='ur', is_agency=True)
        cl1 = db_utils.create_client(session, agency=ag)
        cl2 = db_utils.create_client(session, agency=ag)

        self.o_from = db_utils.create_order(session, cl1, agency=ag)
        self.o_to = db_utils.create_order(session, cl2, agency=ag)
        overact_o = db_utils.create_order(session, cl1, agency=ag)

        month = mapper.ActMonth()

        i = db_utils.create_invoice(session, ag, [(self.o_from, 10)], person=p, dt=month.begin_dt)
        i.create_receipt(666)

        self._mk_overact(i, overact_o, month, 10)

        db_utils.consume_order(i, self.o_from, 12)

        return [(self.o_from, self.o_to)], True

    def get_result(self):
        res = RequiredResult(transition=IssueTransitions.fixed)
        res.add_transferred_state(self.o_from, self.o_to, [2])
        res.add_comment_format('all_transferred', f=self.o_from, t=self.o_to)
        return res


class TransferQtySubclientsOveractCheckTest(AbstactSubclientsOverachCheckTestCase):
    _representation = 'transfer_qty_subclients_overact_check'

    def __init__(self):
        self.o_from1 = None
        self.o_from2 = None
        self.o_to1 = None
        self.o_to2 = None

    def _get_data(self, session):
        ag, p = db_utils.create_client_person(session, person_type='ur', is_agency=True)
        cl1 = db_utils.create_client(session, agency=ag)
        cl2 = db_utils.create_client(session, agency=ag)

        overact_o = db_utils.create_order(session, cl1, agency=ag)
        self.o_from1 = db_utils.create_order(session, cl1, agency=ag)
        self.o_from2 = db_utils.create_order(session, cl1, agency=ag)
        self.o_to1 = db_utils.create_order(session, cl2, agency=ag)
        self.o_to2 = db_utils.create_order(session, cl2, agency=ag)

        month = mapper.ActMonth()

        i = db_utils.create_invoice(session, ag, [(overact_o, 666)], person=p, dt=month.begin_dt)
        i.create_receipt(666666)

        self._mk_overact(i, overact_o, month, 10)

        db_utils.consume_order(i, self.o_from1, 15)
        db_utils.consume_order(i, self.o_from2, 17)

        return [(self.o_from1, self.o_to1, 15), (self.o_from2, self.o_to2, 7)], False

    def get_result(self):
        res = RequiredResult(transition=IssueTransitions.fixed)
        res.add_transferred_state(self.o_from1, self.o_to1, [15])
        res.add_transferred_state(self.o_from2, self.o_to2, [7])
        res.add_comment_format('sum_transferred', f=self.o_from1, t=self.o_to1, qty=15)
        res.add_comment_format('sum_transferred', f=self.o_from2, t=self.o_to2, qty=7)
        return res


class TransferQtySubclientsOveractCheckFailTest(AbstactSubclientsOverachCheckTestCase):
    _representation = 'transfer_qty_subclients_overact_check_fail'

    def __init__(self):
        self.o_from = None
        self.o_to = None

    def _get_data(self, session):
        ag, p = db_utils.create_client_person(session, person_type='ur', is_agency=True)
        cl1 = db_utils.create_client(session, agency=ag)
        cl2 = db_utils.create_client(session, agency=ag)

        overact_o = db_utils.create_order(session, cl1, agency=ag)
        self.o_from = db_utils.create_order(session, cl1, agency=ag)
        self.o_to = db_utils.create_order(session, cl2, agency=ag)

        month = mapper.ActMonth()

        i = db_utils.create_invoice(session, ag, [(overact_o, 666)], person=p, dt=month.begin_dt)
        i.create_receipt(666666)

        self._mk_overact(i, overact_o, month, 10)

        db_utils.consume_order(i, self.o_from, 15)

        return [(self.o_from, self.o_to, 6)], False

    def get_result(self):
        res = RequiredResult(transition=IssueTransitions.wont_fix)
        res.add_comment_format('not_enough_money_cashback')
        return res


# --- Тесты на проверки ---

class DifferentServicesTest(AbstractDBTransferOrderTestCase):
    _representation = 'different_services'

    def __init__(self):
        self.o_from = None
        self.o_to = None

    def _get_data(self, session):
        cl, p = db_utils.create_client_person(session)
        self.o_from = db_utils.create_order(session, cl, product_id=1475)
        self.o_to = db_utils.create_order(session, cl, product_id=2136)
        db_utils.create_invoice_simple(session, self.o_from, [10], person=p)
        return [(self.o_from, self.o_to)], True

    def get_result(self):
        res = RequiredResult(transition=IssueTransitions.wont_fix)
        res.fail_check('check_services', self.o_from, self.o_to)
        return res


class EmptyOrderTest(AbstractDBTransferOrderTestCase):
    _representation = 'no_consumes'

    def __init__(self):
        self.o_from = None
        self.o_to = None

    def _get_data(self, session):
        cl = db_utils.create_client(session)
        self.o_from = db_utils.create_order(session, cl)
        self.o_to = db_utils.create_order(session, cl)
        return [(self.o_from, self.o_to)], True

    def get_result(self):
        res = RequiredResult(transition=IssueTransitions.wont_fix)
        res.fail_check('check_consumes', self.o_from, self.o_to)
        return res


class AbstractDifferentClientsTest(AbstractDBTransferOrderTestCase):
    def __init__(self, agency_from=True, agency_to=True, same_agency=False):
        self.agency_from = agency_from or same_agency
        self.agency_to = agency_to or same_agency
        self.same_agency = same_agency
        self.o_from = None
        self.o_to = None

    def _get_data(self, session):
        cl_from, person = db_utils.create_client_person(session, create_agency=self.agency_from)
        if self.same_agency:
            cl_to = db_utils.create_client(session, agency=cl_from.agency)
        else:
            cl_to = db_utils.create_client(session, create_agency=self.agency_to)

        self.o_from = db_utils.create_order(session, cl_from, agency=cl_from.agency)
        self.o_to = db_utils.create_order(session, cl_to, agency=cl_to.agency)

        db_utils.create_invoice_simple(session, self.o_from, [10], person=person)
        return [(self.o_from, self.o_to)], True


class DifferentClientsTest(AbstractDifferentClientsTest):
    _representation = 'different_clients'

    def __init__(self):
        super(DifferentClientsTest, self).__init__(False, False)

    def get_result(self):
        res = RequiredResult(transition=IssueTransitions.wont_fix)
        res.fail_check('check_different_clients', self.o_from, self.o_to)
        return res


class ClientAgencyTest(AbstractDifferentClientsTest):
    _representation = 'client_agency'

    def __init__(self):
        super(ClientAgencyTest, self).__init__(False, True)

    def get_result(self):
        res = RequiredResult(transition=IssueTransitions.wont_fix)
        res.fail_check('check_client_subclient', self.o_from, self.o_to)
        return res


class AgencyClientTest(AbstractDifferentClientsTest):
    _representation = 'agency_client'

    def __init__(self):
        super(AgencyClientTest, self).__init__(True, False)

    def get_result(self):
        res = RequiredResult(transition=IssueTransitions.wont_fix)
        res.fail_check('check_client_subclient', self.o_from, self.o_to)
        return res


class DifferentAgenciesTest(AbstractDifferentClientsTest):
    _representation = 'different_agencies'

    def __init__(self):
        super(DifferentAgenciesTest, self).__init__(True, True)

    def get_result(self):
        res = RequiredResult(transition=IssueTransitions.wont_fix)
        res.fail_check('check_different_agencies', self.o_from, self.o_to)
        return res


class SameAgencyTest(AbstractDifferentClientsTest):
    _representation = 'same_agency'

    def __init__(self):
        super(SameAgencyTest, self).__init__(same_agency=True)

    def get_result(self):
        res = RequiredResult(transition=IssueTransitions.fixed)
        res.add_comment_format('all_transferred', f=self.o_from, t=self.o_to)
        res.add_transferred_state(self.o_from, self.o_to, [10])
        return res


class FromOrderUnmoderatedTest(AbstractDBTransferOrderTestCase):
    _representation = 'from_order_unmoderated'

    def __init__(self):
        self.o_from = None
        self.o_to = None

    def _get_data(self, session):
        cl, p = db_utils.create_client_person(session)

        self.o_from = db_utils.create_order(session, cl)
        self.o_from.unmoderated = 1
        self.o_to = db_utils.create_order(session, cl)

        db_utils.create_invoice_simple(session, self.o_from, [10], person=p)
        return [(self.o_from, self.o_to)], True

    def get_result(self):
        res = RequiredResult(transition=IssueTransitions.wont_fix)
        res.fail_check('check_order_unmoderated', self.o_from, self.o_to, {'o': self.o_from})
        return res


class ToOrderUnmoderatedTest(AbstractDBTransferOrderTestCase):
    _representation = 'to_order_unmoderated'

    def __init__(self):
        self.o_from = None
        self.o_to = None

    def _get_data(self, session):
        cl, p = db_utils.create_client_person(session)

        self.o_from = db_utils.create_order(session, cl)
        self.o_to = db_utils.create_order(session, cl)
        self.o_to.unmoderated = 1

        db_utils.create_invoice_simple(session, self.o_from, [10], person=p)
        return [(self.o_from, self.o_to)], True

    def get_result(self):
        res = RequiredResult(transition=IssueTransitions.wont_fix)
        res.fail_check('check_order_unmoderated', self.o_from, self.o_to, {'o': self.o_to})
        return res


class InvoiceUnmoderatedTest(AbstractDBTransferOrderTestCase):
    _representation = 'invoice_unmoderated'

    def __init__(self):
        self.o_from = None
        self.o_to = None
        self.i = None

    def _get_data(self, session):
        cl, p = db_utils.create_client_person(session)

        self.o_from = db_utils.create_order(session, cl)
        self.o_to = db_utils.create_order(session, cl)
        unm_o = db_utils.create_order(session, cl)

        self.i = db_utils.create_invoice(session, cl, [(self.o_from, 10), (unm_o, 10)], person=p, turn_on=True)
        unm_o.unmoderated = 1

        return [(self.o_from, self.o_to)], True

    def get_result(self):
        res = RequiredResult(transition=IssueTransitions.wont_fix)
        res.fail_check('check_invoice_unmoderated', self.o_from, self.o_to, {'i': self.i})
        return res


class DifferentCommissionTypesTest(AbstractDBTransferOrderTestCase):
    _representation = 'different_commission_types'

    def __init__(self):
        self.o_from = None
        self.o_to = None

    def _get_data(self, session):
        cl, p = db_utils.create_client_person(session)

        self.o_from = db_utils.create_order(session, cl, product_id=504075)
        self.o_to = db_utils.create_order(session, cl, product_id=503297)

        db_utils.create_invoice_simple(session, self.o_from, [10])

        return [(self.o_from, self.o_to, 10)], False

    def get_result(self):
        res = RequiredResult(transition=IssueTransitions.wont_fix)
        res.fail_check('check_commission_discount_type', self.o_from, self.o_to)
        return res


class DifferentDiscountTypesTest(AbstractDBTransferOrderTestCase):
    _representation = 'different_discount_types'

    def __init__(self):
        self.o_from = None
        self.o_to = None

    def _get_data(self, session):
        cl, p = db_utils.create_client_person(session)

        self.o_from = db_utils.create_order(session, cl, product_id=503273)
        self.o_to = db_utils.create_order(session, cl, product_id=503274)

        db_utils.create_invoice_simple(session, self.o_from, [10])

        return [(self.o_from, self.o_to, 10)], False

    def get_result(self):
        res = RequiredResult(transition=IssueTransitions.wont_fix)
        res.fail_check('check_commission_discount_type', self.o_from, self.o_to)
        return res


class SubclientLimitsTest(AbstractDBTransferOrderTestCase):
    _representation = 'subclient_limits'

    def __init__(self):
        self.o_from = None
        self.o_to = None

    def _get_data(self, session):
        cl_from, person = db_utils.create_client_person(session, create_agency=True)
        cl_to = db_utils.create_client(session, agency=cl_from.agency)

        self.o_from = db_utils.create_order(session, cl_from, agency=cl_from.agency)
        self.o_to = db_utils.create_order(session, cl_to, agency=cl_to.agency)

        personal_account = db_utils.create_personal_account(session, cl_from.agency, person, subclient=cl_from)
        personal_account.transfer(self.o_from, 2, 10, skip_check=True)

        return [(self.o_from, self.o_to, 10)], False

    def get_result(self):
        res = RequiredResult(transition=IssueTransitions.wont_fix)
        res.fail_check('check_subclient_limits', self.o_from, self.o_to)
        return res


class CashBackTransferDenied(AbstractDBTransferOrderTestCase):
    _representation = 'cash_back_transfer_denied'

    def __init__(self):
        self.o_from = None
        self.o_to = None

    def _get_data(self, session):
        agency = db_utils.create_client(session, create_agency=True, is_agency=1)
        cl = db_utils.create_client(session, name='lol', agency=agency)
        session.execute('''
        insert into bo.t_client_cashback(id, client_id, service_id, iso_currency, bonus)
        values (666, {id_}, 7, 'RUB', 5)
        '''.format(id_=cl.id))
        cl2 = db_utils.create_client(session, name='kek', agency=agency)
        self.o_from = db_utils.create_order(session, cl, agency=agency, service_id=7)
        self.o_to = db_utils.create_order(session, cl2, agency=agency, service_id=7)

        db_utils.create_invoice_simple(session, self.o_from, [10])

        return [(self.o_from, self.o_to, 10)], False

    def get_result(self):
        res = RequiredResult(transition=IssueTransitions.wont_fix)
        res.add_message(COMMENTS.get('cash_back_transfer_denied'))
        return res


@pytest.mark.parametrize('issue_data',
                         [case() for case in AbstractDBTransferOrderTestCase._cases],
                         ids=lambda case: str(case),
                         indirect=['issue_data'])
def test_db(session, issue_data):
    queue_object, st_issue, case = issue_data

    req_res = case.get_result()
    solver = TransferOrder(queue_object, st_issue)

    res = solver.solve()
    if req_res is None:
        assert res.commit is False
        assert res.delay is True
        assert res.issue_report is None
        return

    assert res.commit is True
    assert res.delay is False
    report = res.issue_report

    assert len(report.comments) <= 1
    assert case_utils.prepare_comment(report.comment) == set(req_res.comments)
    assert report.transition == req_res.transition
    assert report.assignee == req_res.assignee
    assert report.transition == req_res.transition

    for order, state in req_res.from_states:
        cur_state = [(q.invoice_id, q.current_qty) for q in order.consumes]
        assert cur_state == state

    for order, state in req_res.to_states:
        cur_state = {i_id: sum(q.current_qty for q in rows) for i_id, rows
                     in ut.groupby(order.consumes, lambda q: q.invoice_id)}
        assert cur_state == state


class AbstractDBPatchFTATestCase(AbstractDBTestCase):
    _cases = []
    __metaclass__ = case_utils.TestCaseMeta


class VariedFTAQtyOkTest(AbstractDBPatchFTATestCase):
    _representation = 'varied_fta_qty_ok'

    def __init__(self):
        self.o_from = None
        self.o_to = None

    def _get_data(self, session):
        cl, p = db_utils.create_client_person(session, person_type='ur')
        pu = db_utils.create_person(session, cl, person_name='MrPutin', person_type='ur')

        self.o_from = db_utils.create_order(session, cl)
        self.o_to = db_utils.create_order(session, cl)

        month = mapper.ActMonth()

        i1 = db_utils.create_invoice(session, cl, [(self.o_from, 10)], person=p, turn_on=True, dt=month.begin_dt)
        i2 = db_utils.create_invoice(session, cl, [(self.o_from, 10)], person=pu, turn_on=True, dt=month.begin_dt)
        i3 = db_utils.create_invoice(session, cl, [(self.o_from, 10)], person=p, turn_on=True, dt=month.begin_dt)

        self.o_from.calculate_consumption(month.begin_dt, {self.o_from.shipment_type: 17})
        i2.generate_act(force=True, backdate=month.document_dt)

        self.o_from.calculate_consumption(month.begin_dt, {self.o_from.shipment_type: 6})
        i1.generate_act(force=True, backdate=month.document_dt)

        self.o_from.calculate_consumption(month.begin_dt, {self.o_from.shipment_type: 5})

        return [(self.o_from, self.o_to, 19)], False

    def get_result(self):
        res = RequiredResult(transition=IssueTransitions.fixed)
        res.add_transferred_state(self.o_from, self.o_to, [10, 9])
        res.add_comment_format('sum_transferred', f=self.o_from, t=self.o_to, qty=19)
        return res


class VariedFTAQtyOveractedFailTest(AbstractDBPatchFTATestCase):
    _representation = 'varied_fta_qty_overacted_fail'

    def __init__(self):
        self.o_from = None
        self.o_to = None

    def _get_data(self, session):
        cl, p = db_utils.create_client_person(session, person_type='ur')
        pu = db_utils.create_person(session, cl, person_name='MrPutin', person_type='ur')

        self.o_from = db_utils.create_order(session, cl)
        self.o_to = db_utils.create_order(session, cl)

        month = mapper.ActMonth()

        i1 = db_utils.create_invoice(session, cl, [(self.o_from, 10)], person=p, turn_on=True, dt=month.begin_dt)
        i2 = db_utils.create_invoice(session, cl, [(self.o_from, 10)], person=pu, turn_on=True, dt=month.begin_dt)
        i3 = db_utils.create_invoice(session, cl, [(self.o_from, 10)], person=p, turn_on=True, dt=month.begin_dt)

        self.o_from.calculate_consumption(month.begin_dt, {self.o_from.shipment_type: 17})
        i2.generate_act(force=True, backdate=month.document_dt)

        self.o_from.calculate_consumption(month.begin_dt, {self.o_from.shipment_type: 8})
        i1.generate_act(force=True, backdate=month.document_dt)

        self.o_from.calculate_consumption(month.begin_dt, {self.o_from.shipment_type: 5})

        return [(self.o_from, self.o_to, 23)], False

    def get_result(self):
        res = RequiredResult(transition=IssueTransitions.wont_fix)
        res.fail_transfer('overacted_fta', self.o_from, self.o_to)
        return res


class VariedFTAQtyOveractedOkTest(AbstractDBPatchFTATestCase):
    _representation = 'varied_fta_qty_overacted_ok'

    def __init__(self):
        self.o_from = None
        self.o_to = None

    def _get_data(self, session):
        cl, p = db_utils.create_client_person(session, person_type='ur')
        pu = db_utils.create_person(session, cl, person_name='MrPutin', person_type='ur')

        self.o_from = db_utils.create_order(session, cl)
        self.o_to = db_utils.create_order(session, cl)

        month = mapper.ActMonth()

        i1 = db_utils.create_invoice(session, cl, [(self.o_from, 10)], person=p, turn_on=True, dt=month.begin_dt)
        i2 = db_utils.create_invoice(session, cl, [(self.o_from, 10)], person=pu, turn_on=True, dt=month.begin_dt)
        i3 = db_utils.create_invoice(session, cl, [(self.o_from, 10)], person=p, turn_on=True, dt=month.begin_dt)

        self.o_from.calculate_consumption(month.begin_dt, {self.o_from.shipment_type: 17})
        i2.generate_act(force=True, backdate=month.document_dt)

        self.o_from.calculate_consumption(month.begin_dt, {self.o_from.shipment_type: 6})
        i1.generate_act(force=True, backdate=month.document_dt)

        self.o_from.calculate_consumption(month.begin_dt, {self.o_from.shipment_type: 5})

        return [(self.o_from, self.o_to, 21)], False

    def get_result(self):
        res = RequiredResult(transition=IssueTransitions.fixed)
        res.add_transferred_state(self.o_from, self.o_to, [10, 10, 1])
        res.add_comment_format('sum_transferred', f=self.o_from, t=self.o_to, qty=21)
        return res


class VariedFTAAllFailTest(AbstractDBPatchFTATestCase):
    _representation = 'varied_fta_all_fail'

    def __init__(self):
        self.o_from = None
        self.o_to = None

    def _get_data(self, session):
        cl, p = db_utils.create_client_person(session, person_type='ur')
        pu = db_utils.create_person(session, cl, person_name='MrPutin', person_type='ur')

        self.o_from = db_utils.create_order(session, cl)
        self.o_to = db_utils.create_order(session, cl)

        month = mapper.ActMonth()

        i1 = db_utils.create_invoice(session, cl, [(self.o_from, 10)], person=p, turn_on=True, dt=month.begin_dt)
        i2 = db_utils.create_invoice(session, cl, [(self.o_from, 10)], person=pu, turn_on=True, dt=month.begin_dt)
        i3 = db_utils.create_invoice(session, cl, [(self.o_from, 10)], person=p, turn_on=True, dt=month.begin_dt)

        self.o_from.calculate_consumption(month.begin_dt, {self.o_from.shipment_type: 17})
        i2.generate_act(force=True, backdate=month.document_dt)

        self.o_from.calculate_consumption(month.begin_dt, {self.o_from.shipment_type: 6})
        i1.generate_act(force=True, backdate=month.document_dt)

        self.o_from.calculate_consumption(month.begin_dt, {self.o_from.shipment_type: 5})

        return [(self.o_from, self.o_to)], True

    def get_result(self):
        res = RequiredResult(transition=IssueTransitions.wont_fix)
        res.fail_transfer('overacted_fta', self.o_from, self.o_to)
        return res


class SplitActFTAAllOkTest(AbstractDBPatchFTATestCase):
    _representation = 'split_act_fta_all_ok'

    def __init__(self):
        self.o_from = None
        self.o_to = None

    def _get_data(self, session):
        cl, p = db_utils.create_client_person(session, person_type='ur', person_name='MrPutin')

        self.o_from = db_utils.create_order(session, cl)
        self.o_to = db_utils.create_order(session, cl)

        month = mapper.ActMonth()

        i = db_utils.create_invoice(session, cl, [(self.o_from, 10)], person=p, dt=month.begin_dt)
        i.create_receipt(i.effective_sum)
        q1 = db_utils.consume_order(i, self.o_from, 4)
        q2 = db_utils.consume_order(i, self.o_from, 3)
        q3 = db_utils.consume_order(i, self.o_from, 3)

        self.o_from.calculate_consumption(month.begin_dt, {self.o_from.shipment_type: 4})
        i.generate_act(force=True, backdate=month.document_dt)

        self.o_from.calculate_consumption(month.begin_dt, {self.o_from.shipment_type: 2})
        consumption.reverse_consume(q1, None, 2)

        return [(self.o_from, self.o_to)], True

    def get_result(self):
        res = RequiredResult(transition=IssueTransitions.fixed)
        res.add_transferred_state(self.o_from, self.o_to, [3, 3])
        res.add_comment_format('all_transferred', f=self.o_from, t=self.o_to)
        return res


class SplitActFTAQtyOkTest(AbstractDBPatchFTATestCase):
    _representation = 'split_act_fta_qty_ok'

    def __init__(self):
        self.o_from = None
        self.o_to = None

    def _get_data(self, session):
        cl, p = db_utils.create_client_person(session, person_type='ur', person_name='MrPutin')

        self.o_from = db_utils.create_order(session, cl)
        self.o_to = db_utils.create_order(session, cl)

        month = mapper.ActMonth()

        i = db_utils.create_invoice(session, cl, [(self.o_from, 10)], person=p, dt=month.begin_dt)
        i.create_receipt(i.effective_sum)
        q1 = db_utils.consume_order(i, self.o_from, 4)
        q2 = db_utils.consume_order(i, self.o_from, 3)
        q3 = db_utils.consume_order(i, self.o_from, 3)

        self.o_from.calculate_consumption(month.begin_dt, {self.o_from.shipment_type: 4})
        i.generate_act(force=True, backdate=month.document_dt)

        self.o_from.calculate_consumption(month.begin_dt, {self.o_from.shipment_type: 2})
        consumption.reverse_consume(q1, None, 2)

        return [(self.o_from, self.o_to, 5)], False

    def get_result(self):
        res = RequiredResult(transition=IssueTransitions.fixed)
        res.add_transferred_state(self.o_from, self.o_to, [3, 2])
        res.add_comment_format('sum_transferred', f=self.o_from, t=self.o_to, qty=5)
        return res


class SplitActFTAQtyFailTest(AbstractDBPatchFTATestCase):
    _representation = 'split_act_fta_qty_fail'

    def __init__(self):
        self.o_from = None
        self.o_to = None

    def _get_data(self, session):
        cl, p = db_utils.create_client_person(session, person_type='ur', person_name='MrPutin')

        self.o_from = db_utils.create_order(session, cl)
        self.o_to = db_utils.create_order(session, cl)

        month = mapper.ActMonth()

        i = db_utils.create_invoice(session, cl, [(self.o_from, 10)], person=p, dt=month.begin_dt)
        i.create_receipt(i.effective_sum)
        q1 = db_utils.consume_order(i, self.o_from, 4)
        q2 = db_utils.consume_order(i, self.o_from, 3)
        q3 = db_utils.consume_order(i, self.o_from, 3)

        self.o_from.calculate_consumption(month.begin_dt, {self.o_from.shipment_type: 4})
        i.generate_act(force=True, backdate=month.document_dt)

        self.o_from.calculate_consumption(month.begin_dt, {self.o_from.shipment_type: 2})
        consumption.reverse_consume(q1, None, 2)

        return [(self.o_from, self.o_to, 7)], False

    def get_result(self):
        res = RequiredResult(transition=IssueTransitions.wont_fix)
        res.fail_transfer('not_enough_money', self.o_from, self.o_to, 6)
        return res


def patch_need_fta(self, invoice, order):
    return invoice.person.name == 'MrPutin'


@mock.patch('autodasha.solver_cl.transfer_order.TransferOrder.need_force_transfer_acted', patch_need_fta)
@pytest.mark.parametrize('issue_data',
                         [case() for case in AbstractDBPatchFTATestCase._cases],
                         ids=lambda case: str(case),
                         indirect=['issue_data'])
def test_patched_force_transfer_acted(session, issue_data):
    queue_object, st_issue, case = issue_data

    req_res = case.get_result()
    solver = TransferOrder(queue_object, st_issue)

    res = solver.solve()
    if req_res is None:
        assert res.commit is False
        assert res.delay is True
        assert res.issue_report is None
        return

    assert res.commit is True
    assert res.delay is False
    report = res.issue_report

    assert len(report.comments) <= 1
    assert case_utils.prepare_comment(report.comment) == set(req_res.comments)
    assert report.transition == req_res.transition
    assert report.assignee == req_res.assignee
    assert report.transition == req_res.transition

    for order, state in req_res.from_states:
        cur_state = [(q.invoice_id, q.current_qty) for q in order.consumes]
        assert cur_state == state

    for order, state in req_res.to_states:
        cur_state = {i_id: sum(q.current_qty for q in rows) for i_id, rows
                     in ut.groupby(order.consumes, lambda q: q.invoice_id)}
        assert cur_state == state
