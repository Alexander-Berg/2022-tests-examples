# -*- coding: utf-8 -*-

from __future__ import unicode_literals

import pytest
import mock
import functools
import datetime as dt
from sqlalchemy import orm

import balance.mapper as mapper
import balance.muzzle_util as ut
from balance.actions.consumption import reverse_consume
from balance.actions.process_completions import ProcessCompletions
from autodasha.solver_cl import ReportSolver, ManualDocsRemovingSolver, ParseException
from autodasha.core.api.tracker import IssueReport
from autodasha.core.api.tracker import IssueTransitions
from autodasha.db import mapper as a_mapper

from tests.autodasha_tests.functional_tests import case_utils
from tests.autodasha_tests.functional_tests import mock_utils
from tests.autodasha_tests.common import db_utils
from tests.autodasha_tests.common import staff_utils


def _fake_init(self, *args, **kwargs):
    super(ReportSolver, self).__init__(*args, **kwargs)
    self.ro_session = self.session


def get_approve_message(*args, **kwargs):
    return (
        'Нажми кнопку "Подтвердить" или познай мощь Ктулху! '
        'https://jing.yandex-team.ru/files/autodasha/approve_button_something.png'
    )


BILLING_SHOP_ID = 35
AUTHOR = 'autodasha'
MONTH = dt.datetime.now()
TEMP_CLOSED = 0
LATEST_CLOSED = MONTH.replace(day=1) - dt.timedelta(days=1)

COMMENTS = {
    'get_approve': 'Нажми кнопку "Подтвердить" или познай мощь Ктулху! '
                   'https://jing.yandex-team.ru/files/autodasha/approve_button_something.png',
    'invoice_has_another_acts': '(Акт %s)Счет %s - есть другие акты, действий не выполнено.',
    'not_found_in_db': '%s: %s - фильтр не найден в базе.',
    'wrong_service_id': '%s: %s - есть заказ/заказы с не 35 сервисом, нельзя удалить.',
    'wrong_invoice_type': 'Счет: %s с типом "%s" нельзя удалить.',
    'invoice_has_acts': '%s: %s - есть акты, нельзя удалить.',
    'object_already_removed': '%s: %s - уже удален.',
    'wrong_input_data': 'Проверь, пожалуйста, данные.',
    'execution_exception': 'Произошла ошибка при попытке удалить документы. '
                           'Тикет посмотрят разработчики.',
    'wrong_sum': 'Счет %s - не удален, проблема с суммой счета.',
    'wrong_receipt': 'Счет %s - не удален, проблема с приходом.',
    'wrong_completion_qty': 'Акт %s - не удален, проблема с выполненным и заакченным.',
    'act_removing_failed': 'Акт %s - не удалён, возникла ошибка: !!%s!!',
    'closed_acts': 'Акты %s находятся в закрытом периоде, удаление невозможно.',
    'waiting': 'Ждем разморозки выручки.',
    'waiting_time_expired': 'Привет!\n'
                            'Не получено подтверждение, посмотри пожалуйста.',
    'recount_reward': 'Привет! '
                      'Требуется расчет премии, посмотри, пожалуйста.',
    'done': 'Удаленные объекты: %s.',
    'already_solved': 'Эта задача уже была выполнена.'
}


class AbstractMockTestCase(case_utils.AbstractMockTestCase):
    _default_lines = {
        'summary': 'Удалить ручные документы (35)',
        'acts_external_id': 'Номера актов:\n%s',
        'invoices_external_id': 'Номера счетов:\n%s',
        'recount_reward_confirm': 'Требуется расчет премии:\n%s',
        'invoice_removing_confirm': 'Счет также необходимо удалить:\n%s',
        'approver': 'Подтверждающий руководитель:\n%s',
        'comment': 'Комментарий:\n%s',
    }
    acts = []
    invoices = []


class AbstractGoodMockTestCase(AbstractMockTestCase):
    _cases = []
    __metaclass__ = case_utils.TestCaseMeta


class ActsValuesWithCommentsFilledCase(AbstractGoodMockTestCase):
    _representation = 'acts_values_with_comment_filled'

    def get_data(self, mock_manager):
        self.acts = []
        acts_external_ids = ['111', '32223', '44341']
        for external_id in acts_external_ids:
            invoice = mock_utils.create_invoice(
                mock_manager, external_id='Б-' + external_id + '-1'
            )
            act = mock_utils.create_act(mock_manager, invoice, external_id)
            self.acts.append(act)
        lines = [
            self._get_default_line(acts_external_id='111\n32223\n44341'),
            self._get_default_line(recount_reward_confirm='Да'),
            self._get_default_line(invoice_removing_confirm='No'),
            self._get_default_line(approver='Александр Курицын (nevskiy)'),
            self._get_default_line(comment='sup!'),
        ]
        return self._get_default_line('summary'), lines

    def get_result(self):
        return {
            'objects_type': 'актов',
            'objects': self.acts,
            'recount_reward': 'Да',
            'invoice_removing_confirm': 'No',
            'approver': 'nevskiy',
        }


class ActsValuesWithoutCommentsFilledCase(AbstractGoodMockTestCase):
    _representation = 'acts_values_without_comment_filled'

    def get_data(self, mock_manager):
        self.acts = []
        acts_external_ids = ['111', '32223', '44341']
        for external_id in acts_external_ids:
            invoice = mock_utils.create_invoice(
                mock_manager, external_id='Б-' + external_id + '-1'
            )
            act = mock_utils.create_act(mock_manager, invoice, external_id)
            self.acts.append(act)
        lines = [
            self._get_default_line(acts_external_id='111\n32223\n44341'),
            self._get_default_line(recount_reward_confirm='Да'),
            self._get_default_line(invoice_removing_confirm='Да'),
            self._get_default_line(approver='Александр Курицын (nevskiy)'),
        ]
        return self._get_default_line('summary'), lines

    def get_result(self):
        return {
            'objects_type': 'актов',
            'objects': self.acts,
            'recount_reward': 'Да',
            'invoice_removing_confirm': 'Да',
            'approver': 'nevskiy',
        }


class InvoiceValuesWithoutCommentsFilledCase(AbstractGoodMockTestCase):
    _representation = 'invoice_values_without_comment_filled'

    def get_data(self, mock_manager):
        self.invoices = []
        invoice_external_ids = ['1115', '322235', '443415']
        for external_id in invoice_external_ids:
            invoice = mock_utils.create_invoice(mock_manager, external_id=external_id)
            self.invoices.append(invoice)
        lines = [
            self._get_default_line(invoices_external_id='1115\n322235\n443415'),
            self._get_default_line(recount_reward_confirm='Нет'),
            self._get_default_line(approver='Александр Курицын (nevskiy)'),
        ]
        return self._get_default_line('summary'), lines

    def get_result(self):
        return {
            'objects_type': 'счетов',
            'objects': self.invoices,
            'recount_reward': 'Нет',
            'approver': 'nevskiy',
        }


@mock.patch('autodasha.solver_cl.base_solver.ReportSolver.__init__', _fake_init)
@pytest.mark.parametrize(
    'mock_issue_data',
    [case() for case in AbstractGoodMockTestCase._cases],
    ids=lambda case: str(case),
    indirect=['mock_issue_data'],
)
def test_parse_good(mock_queue_object, mock_issue_data):
    ri = IssueReport()
    issue, case = mock_issue_data

    req_res = case.get_result()

    solver = ManualDocsRemovingSolver(mock_queue_object, issue)
    res = solver.parse_issue(ri)
    if solver.invoices:
        for obj in solver.invoices:
            assert obj in req_res['objects']
    if solver.acts:
        for obj in solver.acts:
            assert obj in req_res['objects']
    assert req_res['objects_type'] == res.get('objects_type')
    if req_res.get('invoice_removing_confirm'):
        assert req_res['invoice_removing_confirm'] == res.get(
            'invoice_removing_confirm'
        )
    assert req_res['recount_reward'] == res.get('recount_reward')
    assert req_res['approver'] == res.get('approver')


class AbstractFailingMockTestCase(AbstractMockTestCase):
    _cases = []
    __metaclass__ = case_utils.TestCaseMeta


class NotFoundDataInDbCase(AbstractFailingMockTestCase):
    _representation = 'not_found_in_db'

    def get_data(self, mock_manager):
        lines = [
            self._get_default_line(acts_external_id='5111\n532223\n544341'),
            self._get_default_line(recount_reward_confirm='Да'),
            self._get_default_line(invoice_removing_confirm='Да'),
            self._get_default_line(approver='Александр Курицын (nevskiy)'),
            self._get_default_line(comment='sup!'),
        ]
        return self._get_default_line('summary'), lines

    def get_result(self):
        comments = ''
        for external_id in ['5111', '532223', '544341']:
            comments += COMMENTS.get('not_found_in_db') % ('Акт', external_id) + '\n'
        comments = comments[:-1]
        comments += COMMENTS.get('wrong_input_data')
        return {}, comments


class NotAllObjectsWereFoundInDbCase(AbstractFailingMockTestCase):
    _representation = 'not_all_objects_were_found_in_db'

    def get_data(self, mock_manager):
        self.acts = []
        external_id = '5111'
        invoice = mock_utils.create_invoice(
            mock_manager, external_id='Б-' + external_id + '-1'
        )
        act = mock_utils.create_act(mock_manager, invoice, external_id)
        self.acts.append(act)
        lines = [
            self._get_default_line(acts_external_id='5111\n532223\n544341'),
            self._get_default_line(recount_reward_confirm='Yes'),
            self._get_default_line(invoice_removing_confirm='Нет'),
            self._get_default_line(approver='Александр Курицын (nevskiy)'),
            self._get_default_line(comment='sup!'),
        ]
        return self._get_default_line('summary'), lines

    def get_result(self):
        comments = ''
        for external_id in ['532223', '544341']:
            comments += COMMENTS.get('not_found_in_db') % ('Акт', external_id) + '\n'
        comments = comments[:-1]
        return (
            {
                'objects_type': 'актов',
                'objects': self.acts,
                'recount_reward': 'Yes',
                'invoice_removing_confirm': 'Нет',
                'approver': 'nevskiy',
            },
            comments,
        )


@mock.patch('autodasha.solver_cl.base_solver.ReportSolver.__init__', _fake_init)
@pytest.mark.parametrize(
    'mock_issue_data',
    [case() for case in AbstractFailingMockTestCase._cases],
    ids=lambda case: str(case),
    indirect=['mock_issue_data'],
)
def test_parse_failing(mock_queue_object, mock_issue_data):
    ri = IssueReport()
    issue, case = mock_issue_data

    req_res, req_comment = case.get_result()

    solver = ManualDocsRemovingSolver(mock_queue_object, issue)
    if not req_res:
        with pytest.raises(ParseException) as exc:
            solver.parse_issue(ri)
            if ri.comments:
                comments = [text.get('text') for text in ri.comments]
                comments = ' '.join(comments)
                if comments:
                    comments += exc.value.message
                assert req_comment == comments
            else:
                assert req_comment in exc.value.message
    else:
        res = solver.parse_issue(ri)
        comments = [text.get('text') for text in ri.comments]
        comments = ' '.join(comments)
        assert req_comment == comments
        if solver.invoices:
            for obj in solver.invoices:
                assert obj in req_res['objects']
        if solver.acts:
            for obj in solver.acts:
                assert obj in req_res['objects']
        assert req_res['objects_type'] == res.get('objects_type')
        if req_res.get('invoice_removing_confirm'):
            assert req_res['invoice_removing_confirm'] == res.get(
                'invoice_removing_confirm'
            )
        assert req_res['recount_reward'] == res.get('recount_reward')
        assert req_res['approver'] == res.get('approver')


class RequiredResult(case_utils.RequiredResult):
    def __init__(self, **kwargs):
        self.commit = kwargs.get('commit')
        self.delay = kwargs.get('delay')
        self.summonees = kwargs.get('summonees', [])
        self.assignee = 'autodasha'
        self.obj_for_export = []
        self.acts = []
        self.invoices = []
        self.has_deleted_acts = False
        super(RequiredResult, self).__init__(**kwargs)

    @staticmethod
    def get_comment(key, *args, **kwargs):
        return COMMENTS.get(key).format(*args, **kwargs)


class AbstractDBTestCase(case_utils.AbstractDBTestCase):
    summary = 'Удалить ручные документы (35)'

    _description = '''
Номера {objects_type}:\n{external_ids}
Требуется расчет премии:\n{recount_reward}
Счет также необходимо удалить:\n{invoice_removing_confirm}
Подтверждающий руководитель:\n{approver}
Комментарий:\n
    '''.strip()
    issue_key = 'test_manual_docs_removing'

    _cases = []
    __metaclass__ = case_utils.TestCaseMeta

    bad_invoice = None

    def __init__(self):
        super(AbstractDBTestCase, self).__init__()
        self.invoices = []
        self.acts = []

    def setup_config(self, session, config):
        config['FRAUD_CLEANER_SETTINGS'] = {'support_manager': 'ashul'}
        config['changeable_acts_dt'] = session.execute(
            '''select value_dt from autodasha.t_config where item = 'LATEST_CLOSED' '''
        ).fetchone()[0]

    def _get_data(self, session):
        return self.prepare_data(session)


class RequestApproveDBCase(AbstractDBTestCase):
    _representation = 'request_approve'

    def prepare_data(self, session):
        client, person = db_utils.create_client_person(session)
        order = db_utils.create_order(session, client, service_id=35)
        consume_qty = 666

        invoice = db_utils.create_invoice(session, client, [(order, consume_qty)])
        invoice.create_receipt(invoice.effective_sum)
        invoice.transfer(order, 2, consume_qty, skip_check=True)
        order.calculate_consumption(MONTH, {order.shipment_type: consume_qty})
        act = invoice.generate_act(force=True, backdate=MONTH)
        self.invoices.append(invoice)
        self.acts.extend(act)
        external_ids = act[0].external_id
        return {
            'objects_type': 'актов',
            'external_ids': external_ids,
            'recount_reward': 'Нет',
            'invoice_removing_confirm': 'Да',
            'approver': 'Александр Курицын (nevskiy)',
        }

    def get_result(self):
        res = RequiredResult(commit=True, delay=True)
        res.transition = IssueTransitions.none
        res.acts = self.acts
        res.invoices = self.invoices
        res.summonees = 'nevskiy'
        res.add_message(COMMENTS['get_approve'])
        return res


class RequestExpiredApproveDBCase(AbstractDBTestCase):
    _representation = 'request_expired'

    def get_comments(self):
        return [
            {
                'author': AUTHOR,
                'text': COMMENTS['get_approve'],
                'summonees': ['nevskiy'],
                'dt': dt.datetime.now() - dt.timedelta(days=15),
            }
        ]

    def prepare_data(self, session):
        client, person = db_utils.create_client_person(session)
        order = db_utils.create_order(session, client, service_id=35)
        consume_qty = 666

        invoice = db_utils.create_invoice(session, client, [(order, consume_qty)])
        invoice.create_receipt(invoice.effective_sum)
        invoice.transfer(order, 2, consume_qty, skip_check=True)
        order.calculate_consumption(MONTH, {order.shipment_type: consume_qty})
        act = invoice.generate_act(force=True, backdate=MONTH)
        self.invoices.append(invoice)
        self.acts.extend(act)
        external_ids = act[0].external_id
        return {
            'objects_type': 'актов',
            'external_ids': external_ids,
            'recount_reward': 'Нет',
            'invoice_removing_confirm': 'Да',
            'approver': 'Александр Курицын (nevskiy)',
        }

    def get_result(self):
        res = RequiredResult(commit=True, delay=True)
        res.transition = IssueTransitions.none
        res.acts = self.acts
        res.invoices = self.invoices
        res.summonees = 'nevskiy'
        return res


class WaitingDBCase(AbstractDBTestCase):
    _representation = 'waiting'

    def __init__(self):
        dt_ = ut.trunc_date(dt.datetime.now()).replace(day=1)
        super(WaitingDBCase, self).__init__()
        self._temp_closed = 1
        self._latest_closed = dt_ - dt.timedelta(days=31)
        self._month_dt = dt_

    def setup_config(self, session, config):
        config['LATEST_CLOSED'] = self._latest_closed
        config['TEMP_CLOSED'] = self._temp_closed
        config['changeable_acts_dt'] = self._latest_closed
        config['is_temporary_closed'] = self._temp_closed

        session.execute(
            "update autodasha.t_config set value_num = :val where item = 'TEMP_CLOSED'",
            {'val': self._temp_closed},
        )

        session.execute(
            "update autodasha.t_config set value_dt = :val where item = 'LATEST_CLOSED'",
            {'val': self._latest_closed},
        )

    def get_comments(self):
        return [
            {
                'author': AUTHOR,
                'text': COMMENTS['get_approve'],
                'summonees': ['nevskiy'],
                'dt': dt.datetime.now(),
            },
            ('nevskiy', 'Подтверждено'),
        ]

    def prepare_data(self, session):
        client, person = db_utils.create_client_person(session)
        order_35 = db_utils.create_order(session, client, service_id=35)
        consume_qty = 666
        act_dt = self._month_dt - dt.timedelta(days=16)
        invoice = db_utils.create_invoice(
            session, client, [(order_35, consume_qty)], turn_on=True
        )
        order_35.calculate_consumption(MONTH, {order_35.shipment_type: consume_qty})
        act = invoice.generate_act(force=True, backdate=MONTH)
        act[0].dt = act_dt
        self.acts.extend(act)
        invoice.create_receipt(invoice.effective_sum)
        invoice.transfer(order_35, 2, consume_qty, skip_check=True)
        order_35.calculate_consumption(MONTH, {order_35.shipment_type: consume_qty * 2})
        act = invoice.generate_act(force=True, backdate=MONTH)
        act[0].dt = self._month_dt + dt.timedelta(days=1)
        self.acts.extend(act)

        external_ids = '\n'.join(a.external_id for a in self.acts)
        return {
            'objects_type': 'актов',
            'external_ids': external_ids,
            'recount_reward': 'Нет',
            'invoice_removing_confirm': 'Нет',
            'approver': 'Александр Курицын (nevskiy)',
        }

    def get_result(self):
        res = RequiredResult(commit=True, delay=True)
        res.transition = IssueTransitions.waiting
        res.acts = self.acts
        res.summonees = 'nevskiy'
        res.add_message(COMMENTS['waiting'])
        return res


class ActAlreadyHiddenDBCase(AbstractDBTestCase):
    _representation = 'act_already_hidden'

    def get_comments(self):
        return [
            {
                'author': AUTHOR,
                'text': COMMENTS['get_approve'],
                'summonees': ['nevskiy'],
                'dt': dt.datetime.now(),
            },
            ('nevskiy', 'Подтверждено'),
        ]

    def prepare_data(self, session):
        client, person = db_utils.create_client_person(session)
        order_11 = db_utils.create_order(session, client, service_id=11)
        consume_qty = 666

        invoice = db_utils.create_invoice(
            session, client, [(order_11, consume_qty)], turn_on=True
        )
        order_11.calculate_consumption(MONTH, {order_11.shipment_type: consume_qty})
        act = invoice.generate_act(force=True, backdate=MONTH)
        act[0].hidden = 4
        self.acts.extend(act)
        external_ids = act[0].external_id
        return {
            'objects_type': 'актов',
            'external_ids': external_ids,
            'recount_reward': 'Нет',
            'invoice_removing_confirm': 'Нет',
            'approver': 'Александр Курицын (nevskiy)',
        }

    def get_result(self):
        res = RequiredResult(commit=True, delay=False)
        res.transition = IssueTransitions.wont_fix
        res.acts = self.acts
        res.summonees = 'nevskiy'
        res.add_message(
            COMMENTS['object_already_removed'] % ('Акт', self.acts[0].external_id)
        )
        res.add_message(COMMENTS['wrong_input_data'])
        return res


class ActHasWrongRowsDBCase(AbstractDBTestCase):
    _representation = 'act_has_wrong_rows'

    def get_comments(self):
        return [
            {
                'author': AUTHOR,
                'text': COMMENTS['get_approve'],
                'summonees': ['nevskiy'],
                'dt': dt.datetime.now(),
            },
            ('nevskiy', 'Подтверждено'),
        ]

    def prepare_data(self, session):
        client, person = db_utils.create_client_person(session)
        order_35 = db_utils.create_order(session, client, service_id=35)
        order_11 = db_utils.create_order(session, client, service_id=11)
        consume_qty = 666
        invoice = db_utils.create_invoice(
            session,
            client,
            [(order_35, consume_qty), (order_11, consume_qty)],
            turn_on=True,
        )
        order_35.calculate_consumption(MONTH, {order_35.shipment_type: consume_qty})
        order_11.calculate_consumption(MONTH, {order_11.shipment_type: consume_qty})
        act = invoice.generate_act(force=True, backdate=MONTH)
        self.invoices.append(invoice)
        self.acts.extend(act)
        external_ids = act[0].external_id
        return {
            'objects_type': 'актов',
            'external_ids': external_ids,
            'recount_reward': 'Нет',
            'invoice_removing_confirm': 'Нет',
            'approver': 'Александр Курицын (nevskiy)',
        }

    def get_result(self):
        res = RequiredResult(commit=True, delay=False)
        res.transition = IssueTransitions.wont_fix
        # потому что после парсилки будут акты и инвойсы
        res.acts = self.acts
        res.invoices = self.invoices
        res.summonees = 'nevskiy'
        res.add_message(
            COMMENTS['wrong_service_id'] % ('Акт', self.acts[0].external_id)
        )
        res.add_message(COMMENTS['wrong_input_data'])
        return res


class ActRemovingFailedDBCase(AbstractDBTestCase):
    _representation = 'act_removing_failed_case'

    def get_comments(self):
        return [
            {
                'author': AUTHOR,
                'text': COMMENTS['get_approve'],
                'summonees': ['nevskiy'],
                'dt': dt.datetime.now(),
            },
            ('nevskiy', 'Подтверждено'),
        ]

    def prepare_data(self, session):
        month = mapper.ActMonth(current_month=MONTH + dt.timedelta(31))
        client, person = db_utils.create_client_person(session)
        contract = db_utils.create_general_contract(session, client, person, ut.trunc_date(dt.datetime.now().replace(day=1)), payment_type=3)
        order = db_utils.create_order(session, client, service_id=35)
        consume_qty = 666

        invoice = db_utils.create_fictive_invoice(
            session, contract, [(order, consume_qty)]
        )
        order.calculate_consumption(month.document_dt, {order.shipment_type: consume_qty})
        self.acts.extend(db_utils.generate_acts(client, month, dps=[invoice.deferpay.id]))
        if invoice.consumes:
            op = mapper.Operation(type_id=666, invoice=invoice, memo='Test')
            for q in invoice.consumes:
                reverse_consume(q, op, q.current_qty)
                if q.completion_qty > 0:
                    op = mapper.Operation(type_id=666, invoice=invoice, memo='Test')
                    ProcessCompletions(
                        q.order, on_dt=q.order.shipment.dt
                    ).calculate_consumption(
                        {
                            q.order.shipment_type: q.order.shipment.consumption
                            - q.completion_qty
                        }
                    )
        invoice.create_receipt(-invoice.receipt_sum)
        invoice.hidden = 2
        self.invoices.append(self.acts[0].invoice)
        external_ids = '\n'.join(obj.external_id for obj in self.acts)
        return {
            'objects_type': 'актов',
            'external_ids': external_ids,
            'recount_reward': 'No',
            'invoice_removing_confirm': 'Да',
            'approver': 'Александр Курицын (nevskiy)',
        }

    def get_result(self):
        res = RequiredResult(commit=True, delay=False)
        res.transition = IssueTransitions.none
        res.assignee = 'robot-octopool'
        # потому что после парсилки будут акты и инвойсы
        res.acts = self.acts
        res.invoices = self.invoices
        res.summonees = 'nevskiy'
        res.add_message(COMMENTS['act_removing_failed'] % (self.acts[0].external_id, 'ORA-02290: check constraint'))
        res.add_message(COMMENTS['execution_exception'])
        res.add_message(COMMENTS['wrong_sum'] % self.invoices[0].external_id)
        return res


class ActWrongDateDBCase(AbstractDBTestCase):
    _representation = 'act_has_wrong_date'

    def get_comments(self):
        return [
            {
                'author': AUTHOR,
                'text': COMMENTS['get_approve'],
                'summonees': ['nevskiy'],
                'dt': dt.datetime.now(),
            },
            ('nevskiy', 'Подтверждено'),
        ]

    def prepare_data(self, session):
        client, person = db_utils.create_client_person(session)
        order_35 = db_utils.create_order(session, client, service_id=35)
        consume_qty = 666

        invoice = db_utils.create_invoice(
            session, client, [(order_35, consume_qty)], turn_on=True
        )
        order_35.calculate_consumption(MONTH, {order_35.shipment_type: consume_qty})
        act = invoice.generate_act(force=True, backdate=MONTH)
        act[0].dt = dt.datetime(2019, 11, 1)
        self.invoices.append(invoice)
        self.acts.extend(act)
        external_ids = act[0].external_id
        return {
            'objects_type': 'актов',
            'external_ids': external_ids,
            'recount_reward': 'Нет',
            'invoice_removing_confirm': 'Нет',
            'approver': 'Александр Курицын (nevskiy)',
        }

    def get_result(self):
        res = RequiredResult(commit=True, delay=False)
        res.transition = IssueTransitions.wont_fix
        # потому что после парсилки будут акты и инвойсы
        res.acts = self.acts
        res.invoices = self.invoices
        res.summonees = 'nevskiy'
        res.add_message(COMMENTS['closed_acts'] % (self.acts[0].external_id))
        return res


class InvoiceAlreadyHiddenDBCase(AbstractDBTestCase):
    _representation = 'invoice_already_hidden'

    def get_comments(self):
        return [
            {
                'author': AUTHOR,
                'text': COMMENTS['get_approve'],
                'summonees': ['nevskiy'],
                'dt': dt.datetime.now(),
            },
            ('nevskiy', 'Подтверждено')
        ]

    def prepare_data(self, session):
        client, person = db_utils.create_client_person(session)
        order_11 = db_utils.create_order(session, client, service_id=11)
        consume_qty = 666
        invoice = db_utils.create_invoice(
            session, client, [(order_11, consume_qty)], turn_on=True
        )
        invoice.hidden = 2
        self.invoices.append(invoice)
        external_ids = invoice.external_id
        return {
            'objects_type': 'счетов',
            'external_ids': external_ids,
            'recount_reward': 'Нет',
            'invoice_removing_confirm': 'Нет',
            'approver': 'Александр Курицын (nevskiy)',
        }

    def get_result(self):
        res = RequiredResult(commit=True, delay=False)
        res.transition = IssueTransitions.wont_fix
        # потому что после парсилки будут инвойсы
        res.invoices = self.invoices
        res.summonees = 'nevskiy'
        res.add_message(
            COMMENTS['object_already_removed'] % ('Счет', self.invoices[0].external_id)
        )
        res.add_message(COMMENTS['wrong_input_data'])
        return res


class InvoiceHasWrongConsumesDBCase(AbstractDBTestCase):
    _representation = 'invoice_has_wrong_consume_service'

    def get_comments(self):
        return [
            {
                'author': AUTHOR,
                'text': COMMENTS['get_approve'],
                'summonees': ['nevskiy'],
                'dt': dt.datetime.now(),
            },
            ('nevskiy', 'Подтверждено')
        ]

    def prepare_data(self, session):
        client, person = db_utils.create_client_person(session)
        order_35 = db_utils.create_order(session, client, service_id=35)
        order_11 = db_utils.create_order(session, client, service_id=11)
        consume_qty = 666

        invoice = db_utils.create_invoice(
            session, client, [(order_35, consume_qty), (order_11, consume_qty)]
        )
        invoice.create_receipt(invoice.effective_sum)
        invoice.transfer(order_35, 2, consume_qty, skip_check=True)
        order_35.calculate_consumption(MONTH, {order_35.shipment_type: consume_qty})
        act = invoice.generate_act(force=True, backdate=MONTH)
        invoice.transfer(order_11, 2, consume_qty, skip_check=True)
        order_11.calculate_consumption(MONTH, {order_11.shipment_type: consume_qty})
        self.invoices.append(invoice)
        self.acts.extend(act)
        external_ids = act[0].external_id
        return {
            'objects_type': 'актов',
            'external_ids': external_ids,
            'recount_reward': 'Yes',
            'invoice_removing_confirm': 'Да',
            'approver': 'Александр Курицын (nevskiy)',
        }

    def get_result(self):
        res = RequiredResult(commit=True, delay=False)
        res.transition = IssueTransitions.wont_fix
        # потому что после парсилки будут акты и инвойсы
        res.acts = self.acts
        res.invoices = self.invoices
        res.summonees = 'nevskiy'
        res.add_message(
            COMMENTS['wrong_service_id'] % ('Счет', self.invoices[0].external_id)
        )
        res.add_message(COMMENTS['wrong_input_data'])
        return res


class InvoiceHasAnotherActConsumesDBCase(AbstractDBTestCase):
    _representation = 'invoice_has_another_act'

    def get_comments(self):
        return [
            {
                'author': AUTHOR,
                'text': COMMENTS['get_approve'],
                'summonees': ['nevskiy'],
                'dt': dt.datetime.now(),
            },
            ('nevskiy', 'Подтверждено'),
        ]

    def prepare_data(self, session):
        client, person = db_utils.create_client_person(session)
        order_35 = db_utils.create_order(session, client, service_id=35)
        consume_qty = 666

        invoice = db_utils.create_invoice(
            session, client, [(order_35, consume_qty)], person=person, turn_on=True
        )
        order_35.calculate_consumption(MONTH, {order_35.shipment_type: consume_qty})
        invoice.generate_act(force=True, backdate=MONTH)
        invoice.create_receipt(invoice.effective_sum)
        invoice.transfer(order_35, 2, consume_qty, skip_check=True)
        order_35.calculate_consumption(MONTH, {order_35.shipment_type: consume_qty * 2})
        act = invoice.generate_act(force=True, backdate=MONTH)
        self.acts.extend(act)
        self.invoices.append(invoice)
        external_ids = act[0].external_id
        return {
            'objects_type': 'актов',
            'external_ids': external_ids,
            'recount_reward': 'Yes',
            'invoice_removing_confirm': 'Да',
            'approver': 'Александр Курицын (nevskiy)',
        }

    def get_result(self):
        res = RequiredResult(commit=True, delay=False)
        res.transition = IssueTransitions.wont_fix
        res.summonees = 'nevskiy'
        res.add_message(
            COMMENTS['invoice_has_another_acts']
            % (self.acts[0].external_id, self.invoices[0].external_id)
        )
        res.add_message(COMMENTS['wrong_input_data'])
        return res


class InvoiceRemovingDBCase(AbstractDBTestCase):
    _representation = 'invoice_removing_case'

    def get_comments(self):
        return [
            {
                'author': AUTHOR,
                'text': COMMENTS['get_approve'],
                'summonees': ['nevskiy'],
                'dt': dt.datetime.now(),
            },
            ('nevskiy', 'Подтверждено'),
        ]

    def prepare_data(self, session):
        client, person = db_utils.create_client_person(session)
        contract = db_utils.create_general_contract(session, client, person, dt.datetime.now(), payment_type=3)
        order_35 = db_utils.create_order(session, client, service_id=35)
        order2_35 = db_utils.create_order(session, client, service_id=35)
        consume_qty = 666
        temp_invoices = []

        invoice = db_utils.create_invoice(
            session, client, [(order_35, consume_qty)], person=person, turn_on=True
        )
        invoice2 = db_utils.create_fictive_invoice(
            session, contract, [(order2_35, consume_qty)]
        )
        invoice3 = db_utils.create_invoice(
            session, client, [(order_35, consume_qty)], person=person, turn_on=True
        )
        for invoice, order_35 in [(invoice, order_35), (invoice2, order2_35), (invoice3, order_35)]:
            order_35.calculate_consumption(MONTH, {order_35.shipment_type: consume_qty})
            temp_invoices.append(invoice)
        external_ids = '\n'.join(i.external_id for i in temp_invoices)
        temp_invoices.remove(invoice2)
        self.invoices = temp_invoices
        self.bad_invoice = invoice2
        return {
            'objects_type': 'счетов',
            'external_ids': external_ids,
            'recount_reward': 'Нет',
            'invoice_removing_confirm': ' ',
            'approver': 'Александр Курицын (nevskiy)',
        }

    def get_result(self):
        res = RequiredResult(commit=True, delay=False)
        res.transition = IssueTransitions.none
        res.summonees = 'nevskiy'
        objects = ''
        for invoice in sorted(self.invoices):
            objects += 'счет %s, ' % invoice.external_id
        objects = objects[:-2]
        res.obj_for_export.extend(self.invoices)
        res.invoices.extend(self.invoices)
        res.add_message(COMMENTS['wrong_invoice_type'] % (self.bad_invoice.external_id, 'фиктивный'))
        res.add_message(COMMENTS['done'] % objects)
        return res


class InvoiceRemovingDBCaseHasHiddenAct(AbstractDBTestCase):
    _representation = 'invoice_removing_case_has_hidden_act_and_fictive_personal_account'

    def get_comments(self):
        return [
            {
                'author': AUTHOR,
                'text': COMMENTS['get_approve'],
                'summonees': ['nevskiy'],
                'dt': dt.datetime.now(),
            },
            ('nevskiy', 'Подтверждено'),
        ]

    def prepare_data(self, session):
        client, person = db_utils.create_client_person(session)
        contract = db_utils.create_general_contract(session, client, person, personal_account=1, personal_account_fictive=1)
        order_35 = db_utils.create_order(session, client, service_id=35)
        consume_qty = 666

        invoice = db_utils.create_invoice(
            session, client, [(order_35, consume_qty)], person=person, turn_on=True
        )
        order_35.calculate_consumption(MONTH, {order_35.shipment_type: consume_qty})
        act = invoice.generate_act(force=True, backdate=MONTH)[0]
        act.hide('test')
        invoice2 = db_utils.create_invoice(
            session, client, [(order_35, consume_qty)], person=person, turn_on=True
        )
        order_35.calculate_consumption(MONTH, {order_35.shipment_type: consume_qty})
        invoice3 = db_utils.create_personal_account(
            session, contract=contract
        )
        temp_invoices = [invoice, invoice2, invoice3]
        external_ids = '\n'.join(i.external_id for i in temp_invoices)
        temp_invoices.remove(invoice3)
        self.invoices = temp_invoices
        self.bad_invoice = invoice3
        return {
            'objects_type': 'счетов',
            'external_ids': external_ids,
            'recount_reward': 'Нет',
            'invoice_removing_confirm': ' ',
            'approver': 'Александр Курицын (nevskiy)',
        }

    def get_result(self):
        res = RequiredResult(commit=True, delay=False)
        res.transition = IssueTransitions.none
        res.summonees = 'nevskiy'
        objects = ''
        for invoice in sorted(self.invoices):
            objects += 'счет %s, ' % invoice.external_id
        objects = objects[:-2]
        res.obj_for_export.extend(self.invoices)
        res.invoices.extend(self.invoices)
        res.add_message(COMMENTS['wrong_invoice_type'] % (self.bad_invoice.external_id, 'лицевой новый'))
        res.add_message(COMMENTS['done'] % objects)
        return res


class ActRemovingDBCase(AbstractDBTestCase):
    _representation = 'act_removing_case'

    def get_comments(self):
        return [
            {
                'author': AUTHOR,
                'text': COMMENTS['get_approve'],
                'summonees': ['nevskiy'],
                'dt': dt.datetime.now(),
            },
            ('nevskiy', 'Подтверждено'),
        ]

    def prepare_data(self, session):
        client, person = db_utils.create_client_person(session)
        order = db_utils.create_order(session, client, service_id=35)
        consume_qty = 666

        invoice = db_utils.create_invoice(
            session, client, [(order, consume_qty)], person=person, turn_on=True
        )
        order.calculate_consumption(MONTH, {order.shipment_type: consume_qty})
        self.acts.extend(invoice.generate_act(force=True, backdate=MONTH))
        invoice.create_receipt(invoice.effective_sum)
        invoice.transfer(order, 2, consume_qty, skip_check=True)
        order.calculate_consumption(MONTH + dt.timedelta(days=1), {order.shipment_type: consume_qty * 2})
        self.acts.extend(invoice.generate_act(force=True, backdate=MONTH + dt.timedelta(days=1)))
        self.invoices.append(invoice)
        external_ids = '\n'.join(obj.external_id for obj in self.acts)
        return {
            'objects_type': 'актов',
            'external_ids': external_ids,
            'recount_reward': 'No',
            'invoice_removing_confirm': 'Да',
            'approver': 'Александр Курицын (nevskiy)',
        }

    def get_result(self):
        res = RequiredResult(commit=True, delay=False)
        res.transition = IssueTransitions.none
        res.summonees = 'nevskiy'
        objects = ''
        for act in sorted(self.acts, key=lambda a_: a_.dt, reverse=True):
            objects += 'акт %s, ' % act.external_id
        for invoice in sorted(self.invoices):
            objects += 'счет %s, ' % invoice.external_id
        objects = objects[:-2]
        res.obj_for_export.extend(self.acts)
        res.obj_for_export.extend(self.invoices)
        res.acts.extend(self.acts)
        res.invoices.extend(self.invoices)
        res.add_message(COMMENTS['done'] % objects)
        res.has_deleted_acts = True
        return res


class ActRemovingRecountDBCase(AbstractDBTestCase):
    _representation = 'act_removing_recount_reward_case'

    def get_comments(self):
        return [
            {
                'author': AUTHOR,
                'text': COMMENTS['get_approve'],
                'summonees': ['nevskiy'],
                'dt': dt.datetime.now(),
            },
            ('nevskiy', 'Подтверждено'),
        ]

    def prepare_data(self, session):
        client, person = db_utils.create_client_person(session)
        order = db_utils.create_order(session, client, service_id=35)
        consume_qty = 666

        invoice = db_utils.create_invoice(
            session, client, [(order, consume_qty)], person=person, turn_on=True
        )
        order.calculate_consumption(MONTH, {order.shipment_type: consume_qty})
        act = invoice.generate_act(force=True, backdate=MONTH)
        self.invoices.append(invoice)
        self.acts.extend(act)
        external_ids = act[0].external_id
        return {
            'objects_type': 'актов',
            'external_ids': external_ids,
            'recount_reward': 'Yes',
            'invoice_removing_confirm': 'Да',
            'approver': 'Александр Курицын (nevskiy)',
        }

    def get_result(self):
        res = RequiredResult(commit=True, delay=False)
        res.transition = IssueTransitions.none
        res.assignee = 'ashul'
        res.summonees = 'nevskiy'
        objects = 'акт %s, счет %s' % (
            self.acts[0].external_id,
            self.invoices[0].external_id,
        )
        res.obj_for_export.extend(self.acts)
        res.obj_for_export.extend(self.invoices)
        res.acts.extend(self.acts)
        res.invoices.extend(self.invoices)
        res.add_message(COMMENTS['done'] % objects)
        res.add_message(COMMENTS['recount_reward'])
        res.has_deleted_acts = True
        return res


def mock_staff(testfunc):
    other_dick = staff_utils.Person('nevskiy')
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
@mock.patch(
    'autodasha.solver_cl.base_solver.BaseSolver.get_approve_message',
    get_approve_message,
)
@pytest.mark.parametrize(
    'issue_data',
    [case() for case in AbstractDBTestCase._cases],
    ids=lambda case: str(case),
    indirect=['issue_data'],
)
def test_db(session, issue_data):
    queue_object, st_issue, case = issue_data
    solver = ManualDocsRemovingSolver(queue_object, st_issue)
    res = solver.solve()

    session.flush()
    req_res = case.get_result()
    report = res.issue_report

    if solver.invoices:
        for obj in solver.invoices:
            assert obj in req_res.invoices

    if solver.acts:
        for obj in solver.acts:
            assert obj in req_res.acts

            if req_res.has_deleted_acts:
                orders = {row.consume.order for row in obj.rows}
                for order in orders:
                    assert order.shipment.dt is None
                    assert not session.query(mapper.CompletionHistory).filter(
                        mapper.CompletionHistory.order_id == order.id,
                        mapper.CompletionHistory.end_dt >= obj.dt
                    ).exists()

    assert res.commit == req_res.commit
    assert res.delay == req_res.delay

    report_comments = []
    for c in report.comments:
        c_text = c['text']
        for part in c_text.strip().split('\n'):
            if 'ORA-02290: check constraint' in part:
                part = part.split('!!')[0] + '!!ORA-02290: check constraint!!'
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

    assert report.transition == req_res.transition
    assert report.assignee == req_res.assignee

    try:
        export_queue = (
            session.query(a_mapper.QueueObject)
            .filter(
                a_mapper.QueueObject.issue == queue_object.issue,
                a_mapper.QueueObject.processor == 'EXPORT_CHECK',
            )
            .one()
        )
    except orm.exc.NoResultFound:
        assert not req_res.obj_for_export
    else:
        req_objecs_for_export = {obj.object_id for obj in export_queue.proxies}
        objecs_for_export = {obj.id for obj in req_res.obj_for_export}
        assert req_objecs_for_export == objecs_for_export
