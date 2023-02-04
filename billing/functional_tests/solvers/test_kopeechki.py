# -*- coding: utf-8 -*-

from __future__ import unicode_literals

import mock
import pytest

import balance.mapper as mapper
import balance.muzzle_util as ut

from autodasha.solver_cl import KopeechkiSolver, ParseException
from autodasha.core.api.tracker import IssueTransitions, IssueReport

from tests.autodasha_tests.functional_tests import case_utils
from tests.autodasha_tests.functional_tests import mock_utils
from tests.autodasha_tests.common import db_utils


def get_approve_message(*args, **kwargs):
    return 'Нажми кнопку "Подтвердить", сука! https://jing.yandex-team.ru/files/autodasha/approve_button_something.png'


class RequiredResult(case_utils.RequiredResult):
    COMMENTS = {
        'manager_not_found': 'Не найден менеджер %s.',
        'manager_not_specified': 'Не указан менеджер.',
        'awaiting_confirmation': 'Подтверждение ожидаем от кого:%s.',
        'no_invoices': 'В тексте задачи не указаны счета.',
        'invoices_not_found': 'Счета %s не найдены в биллинге.',
        'not_repayment': 'Счета %s не являются счетами на погашение.',
        'sum_mismatch':
            'Обращаем внимание, что по счёту {i.external_id} сумма зачислений'
            ' по фиктивным счетам ({fict_sum:.2f}{i.currency}) отличается'
            ' от суммы актов ({i.total_act_sum:.2f}{i.currency}).',
        'fixed': 'Исправлены счета %s.',
        'not_fixed': 'Не удалось исправить счета %s.'
    }

    def __init__(self, **kwargs):
        self.invoices_states = []
        self.summonees = kwargs.get('summonees', [])
        super(RequiredResult, self).__init__(**kwargs)

    def add_fixed_state(self, invoice):
        self.invoices_states.append((invoice, invoice.total_act_sum.as_decimal()))

    def add_not_fixed_state(self, invoice):
        self.invoices_states.append((invoice, invoice.effective_sum.as_decimal()))

    def set_messages(self, not_found=None, mismatched=None, **kwargs):
        if not_found:
            self.add_message(self.COMMENTS['invoices_not_found'] % ', '.join(not_found))

        for i, fict_sum in mismatched or []:
            self.add_message(self.COMMENTS['sum_mismatch'].format(i=i, fict_sum=fict_sum))

        if kwargs.get('manager_not_found'):
            self.add_message(self.COMMENTS['manager_not_found'] % kwargs['manager'].name)

        if kwargs.get('manager_not_specified'):
            self.add_message(self.COMMENTS['manager_not_specified'])

        if kwargs.get('awaiting_confirmation'):
            self.add_message(self.COMMENTS['awaiting_confirmation'] % kwargs['manager'].login)
            self.add_message(get_approve_message())

        if kwargs.get('required_confirmation'):
            self.add_message(get_approve_message())

        if kwargs.get('no_invoices'):
            self.add_message(self.COMMENTS['no_invoices'])

    def set_object_states(self, fixed=None, not_fixed=None, not_repayment=None, **kwargs):
        fixed_eids = []
        for i in fixed or []:
            fixed_eids.append(i.external_id)
            self.add_fixed_state(i)
        if fixed_eids:
            self.add_message(self.COMMENTS['fixed'] % ', '.join(fixed_eids))

        not_fixed_eids = []
        for i in not_fixed or []:
            not_fixed_eids.append(i.external_id)
            self.add_not_fixed_state(i)
        if not_fixed_eids:
            self.add_message(self.COMMENTS['not_fixed'] % ', '.join(not_fixed_eids))

        not_repayment_eids = []
        for i in not_repayment or []:
            not_repayment_eids.append(i.external_id)
            self.add_not_fixed_state(i)
        if not_repayment_eids:
            self.add_message(self.COMMENTS['not_repayment'] % ', '.join(not_repayment_eids))


class AbstractMockTestCase(case_utils.AbstractMockTestCase):
    _default_lines = {
        'summary': 'Расхождения на копейки между счетами и актами за 06.6666 %s',
        'header': '''
Манагер, добрый день!
Из-за нашей внутренней ошибки у твоего клиента, для которого счета на погашение формируются по открученному, сумма счета не совпала с суммой акта на копейки.
'''.strip(),
        'row': '%s\t%s\t%s',
        'footer': '''
Мы можем это исправить, удалять акт для этого не потребуется.
Подтверждаешь?
'''.strip()
    }

    def get_data(self, mock_manager):
        manager, invoices = self._create_objects(mock_manager)
        lines = [self._get_default_line('header')]
        for i in invoices:
            lines.append(self._get_default_line(row=(i.external_id, i.effective_sum, i.total_act_sum)))
        lines.append(self._get_default_line('footer'))
        return self._get_default_line(summary=manager.name), lines

    def _create_objects(self, mock_manager):
        raise NotImplementedError


class AbstractManagerMockTestCase(AbstractMockTestCase):
    _cases = []
    __metaclass__ = case_utils.TestCaseMeta

    def __init__(self):
        self.manager = None
        self.invoice = None
        super(AbstractManagerMockTestCase, self).__init__()


class ManagerNotSpecifiedCase(AbstractManagerMockTestCase):
    _representation = 'manager_not_specified'

    def _create_objects(self, mock_manager):
        self.manager = mock_utils.create_manager(mock_manager, name='')
        self.invoice = mock_utils.create_invoice(mock_manager, effective_sum=6666, total_act_sum=666)

        return self.manager, [self.invoice]

    def get_result(self):
        return RequiredResult(transition=IssueTransitions.wont_fix,
                              manager_not_specified=True)


class ManagerNotFoundCase(AbstractManagerMockTestCase):
    _representation = 'manager_not_found'

    def _create_objects(self, mock_manager):
        manager = mock_utils.create_manager(mock_manager, name='Петров')
        self.manager = ut.Struct(name='Сидоров')
        self.invoice = mock_utils.create_invoice(mock_manager, effective_sum=6666, total_act_sum=666)

        return self.manager, [self.invoice]

    def get_result(self):
        return RequiredResult(transition=IssueTransitions.wont_fix,
                              manager=self.manager,
                              manager_not_found=True)


class ManagerNotApprovedCase(AbstractManagerMockTestCase):
    _representation = 'not_approved'

    def _create_objects(self, mock_manager):
        self.manager = mock_utils.create_manager(mock_manager)
        self.invoice = mock_utils.create_invoice(mock_manager, effective_sum=6666, total_act_sum=666)

        return self.manager, [self.invoice]

    def get_result(self):
        return RequiredResult(transition=IssueTransitions.none,
                              required_confirmation=True,
                              summonees=[u'manager'])


class ManagerWrongApproverCase(AbstractManagerMockTestCase):
    _representation = 'wrong_approver'

    def _create_objects(self, mock_manager):
        self.manager = mock_utils.create_manager(mock_manager)
        self.invoice = mock_utils.create_invoice(mock_manager, effective_sum=6666, total_act_sum=666)

        return self.manager, [self.invoice]

    def get_comments(self):
        return [('some_dick', 'Подтверждено')]

    def get_result(self):
        return RequiredResult(transition=IssueTransitions.none,
                              manager=self.manager,
                              awaiting_confirmation=True,
                              summonees=['manager'])


class ManagerWrongApproveCommentCase(AbstractManagerMockTestCase):
    _representation = 'wrong_approve_comment'

    def _create_objects(self, mock_manager):
        self.manager = mock_utils.create_manager(mock_manager)
        self.invoice = mock_utils.create_invoice(mock_manager, effective_sum=6666, total_act_sum=666)

        return self.manager, [self.invoice]

    def get_comments(self):
        return [('manager', 'Ну я тут типа подтверждаю, внатуре')]

    def get_result(self):
        return RequiredResult(transition=IssueTransitions.none,
                              manager=self.manager,
                              required_confirmation=True,
                              summonees=['manager'])


class ManagerAlreadyRequestedApproveCase(AbstractManagerMockTestCase):
    _representation = 'already_requested'

    def _create_objects(self, mock_manager):
        self.manager = mock_utils.create_manager(mock_manager)
        self.invoice = mock_utils.create_invoice(mock_manager, effective_sum=6666, total_act_sum=666)

        return self.manager, [self.invoice]

    def get_comments(self):
        return [
            ('autodasha', get_approve_message())
        ]

    def get_result(self):
        return None


class ManagerAlreadyRequestedWrongConfirmCase(AbstractManagerMockTestCase):
    _representation = 'already_requested_wrong_confirm'

    def _create_objects(self, mock_manager):
        self.manager = mock_utils.create_manager(mock_manager)
        self.invoice = mock_utils.create_invoice(mock_manager, effective_sum=6666, total_act_sum=666)

        return self.manager, [self.invoice]

    def get_comments(self):
        return [
            ('autodasha', get_approve_message()),
            ('some_dick', 'Подтверждено')
        ]

    def get_result(self):
        return RequiredResult(transition=IssueTransitions.none,
                              manager=self.manager,
                              awaiting_confirmation=True,
                              summonees=['manager'])


class ManagerAlreadyRequestedWrongConfirmRerequestedCase(AbstractManagerMockTestCase):
    _representation = 'already_requested_wrong_confirm_rerequested'

    def _create_objects(self, mock_manager):
        self.manager = mock_utils.create_manager(mock_manager)
        self.invoice = mock_utils.create_invoice(mock_manager, effective_sum=6666, total_act_sum=666)

        return self.manager, [self.invoice]

    def get_comments(self):
        return [
            ('autodasha', get_approve_message()),
            ('some_dick', 'Подтверждено'),
            ('autodasha', 'Подтверждение ожидаем от кого:manager')
        ]

    def get_result(self):
        return None


def mock_process_approved(self):
    raise Exception('That should not be called')


@mock.patch('autodasha.solver_cl.base_solver.BaseSolver.get_approve_message', get_approve_message)
@mock.patch('autodasha.solver_cl.kopeechki.KopeechkiSolver.process_approved', mock_process_approved)
@pytest.mark.parametrize('mock_issue_data',
                         [case() for case in AbstractManagerMockTestCase._cases],
                         ids=lambda case: str(case),
                         indirect=['mock_issue_data'])
def test_mock_manager(mock_queue_object, mock_issue_data):
    issue, case = mock_issue_data

    required_res = case.get_result()

    solver = KopeechkiSolver(mock_queue_object, issue)
    res = solver.solve()

    if required_res is None:
        assert res.commit is False
        assert res.delay is True
        assert res.issue_report is None
        return

    assert res.commit is True
    assert res.delay is (required_res.transition == IssueTransitions.none)
    report = res.issue_report

    assert len(report.comments) <= 1
    assert set(required_res.comments) == case_utils.prepare_comment(report.comment)
    assert required_res.transition == report.transition
    assert required_res.assignee == report.assignee
    assert set(required_res.summonees) == set(report.summonees or [])


class AbstractParseInvoiceMockTestCase(AbstractMockTestCase):
    _cases = []
    __metaclass__ = case_utils.TestCaseMeta

    def __init__(self):
        self.manager = None
        self.invoices = None
        super(AbstractParseInvoiceMockTestCase, self).__init__()


class InvoiceParseMockTestCase(AbstractParseInvoiceMockTestCase):
    def _create_objects(self, mock_manager):
        manager = mock_utils.create_manager(mock_manager)
        self.invoices = [
            mock_utils.create_invoice(mock_manager, external_id='Б-666666-6', effective_sum=6666, total_act_sum=666),
            mock_utils.create_invoice(mock_manager, external_id='Б-666666-7', effective_sum=6666, total_act_sum=666)
        ]

        return manager, self.invoices + [
            ut.Struct(external_id='Б-666666-8', effective_sum=6666, total_act_sum=666),
            ut.Struct(external_id='Б-666666-9', effective_sum=6666, total_act_sum=666)
        ]

    def get_result(self):
        res = RequiredResult(not_found=['Б-666666-8', 'Б-666666-9'])
        res.invoices_states = self.invoices
        return res


@pytest.mark.parametrize('mock_issue_data',
                         [case() for case in AbstractParseInvoiceMockTestCase._cases],
                         ids=lambda case: str(case),
                         indirect=['mock_issue_data'])
def test_mock_parse_invoice(mock_queue_object, mock_issue_data):
    issue, case = mock_issue_data

    required_res = case.get_result()

    issue_report = IssueReport()

    solver = KopeechkiSolver(mock_queue_object, issue)
    invoices = solver.parse_invoices(issue_report)

    assert set(required_res.comments) == case_utils.prepare_comment(issue_report.comment)
    assert invoices == required_res.invoices_states


class AbstractParseInvoiceExcMockTestCase(AbstractMockTestCase):
    _cases = []
    __metaclass__ = case_utils.TestCaseMeta

    def __init__(self):
        super(AbstractParseInvoiceExcMockTestCase, self).__init__()


class NoInvoicesMockTestCase(AbstractParseInvoiceExcMockTestCase):
    _representation = 'no_invoices'

    def _create_objects(self, mock_manager):
        manager = mock_utils.create_manager(mock_manager)

        return manager, []

    def get_result(self):
        return RequiredResult(no_invoices=True)


@pytest.mark.parametrize('mock_issue_data',
                         [case() for case in AbstractParseInvoiceExcMockTestCase._cases],
                         ids=lambda case: str(case),
                         indirect=['mock_issue_data'])
def test_mock_parse_invoice(mock_queue_object, mock_issue_data):
    issue, case = mock_issue_data

    required_res = case.get_result()

    issue_report = IssueReport()

    solver = KopeechkiSolver(mock_queue_object, issue)

    with pytest.raises(ParseException) as exc_info:
        solver.parse_invoices(issue_report)

    assert set(required_res.comments) == case_utils.prepare_comment(exc_info.value.message)
    assert not issue_report.comment


class AbstractDBTestCase(case_utils.AbstractDBTestCase):
    _cases = []
    __metaclass__ = case_utils.TestCaseMeta

    summary = 'Расхождения на копейки между счетами и актами за 66.6666 Абдурахман Ибн Хасанович'

    def get_description(self, session):
        invoices = self._get_data(session)

        description = '''
Манагер, добрый день!
Из-за нашей внутренней ошибки у твоего клиента, для которого счета на погашение формируются по открученному, сумма счета не совпала с суммой акта на копейки.
%s
Мы можем это исправить, удалять акт для этого не потребуется.
Подтверждаешь?'''

        invoice_lines = '\n'.join('%s\t%s\t%s' % (i.external_id, i.effective_sum, i.total_act_sum) for i in invoices)
        return description % invoice_lines

    def get_comments(self):
        return [('manager', 'Подтверждено')]


class ChangeInvoicesCase(AbstractDBTestCase):
    _representation = 'change_invoices'

    def __init__(self):
        super(ChangeInvoicesCase, self).__init__()
        self.rep_good = None
        self.rep_mismatch = None
        self.prep = None

    def _get_data(self, session):
        client, person = db_utils.create_client_person(session)
        order1 = db_utils.create_order(session, client)
        order2 = db_utils.create_order(session, client)

        month = mapper.ActMonth()
        contract = db_utils.create_general_contract(session, client, person, month.begin_dt, payment_type=3)

        fictive1 = db_utils.create_fictive_invoice(session, contract, [(order1, 10)])
        order1.calculate_consumption(month.document_dt, {order1.shipment_type: 7})
        act1, = db_utils.generate_acts(client, month, dps=[fictive1.deferpay.id])

        fictive2 = db_utils.create_fictive_invoice(session, contract, [(order2, 10)])
        order2.calculate_consumption(month.document_dt, {order2.shipment_type: 7})
        act2, = db_utils.generate_acts(client, month, dps=[fictive2.deferpay.id])

        self.rep_good = act1.invoice
        self.rep_mismatch = act2.invoice
        self.prep = db_utils.create_invoice_simple(session, order2, [10], paysys_id=1000)

        db_utils.cut_agava(client, month, [fictive1.deferpay, fictive2.deferpay])

        fictive2.transfer(order2, 2, 5, skip_check=True)

        return [self.rep_mismatch, self.rep_good, self.prep]

    def get_result(self):
        return RequiredResult(transition=IssueTransitions.fixed,
                              fixed=[self.rep_mismatch, self.rep_good],
                              not_repayment=[self.prep],
                              mismatched=[(self.rep_mismatch, 360)])


class NoGoodInvoicesCase(AbstractDBTestCase):
    _representation = 'no_good_invoices'

    def __init__(self):
        super(NoGoodInvoicesCase, self).__init__()
        self.prep = None

    def _get_data(self, session):
        client, person = db_utils.create_client_person(session)
        order = db_utils.create_order(session, client)
        self.prep = db_utils.create_invoice_simple(session, order, [10], paysys_id=1000)

        return [self.prep]

    def get_result(self):
        return RequiredResult(transition=IssueTransitions.wont_fix, not_repayment=[self.prep])


class NothingChangedCase(AbstractDBTestCase):
    _representation = 'nothing_changed'

    def __init__(self):
        super(NothingChangedCase, self).__init__()
        self.rep = None

    def _get_data(self, session):
        client, person = db_utils.create_client_person(session)
        order = db_utils.create_order(session, client, product_id=504056)

        month = mapper.ActMonth()
        contract = db_utils.create_general_contract(session, client, person, month.begin_dt,
                                                    payment_type=3, services={7, 70})
        fictive = db_utils.create_fictive_invoice(session, contract, [(order, 10)])
        order.calculate_consumption(month.document_dt, {order.shipment_type: 10})
        act, = db_utils.generate_acts(client, month, dps=[fictive.deferpay.id])

        self.rep = act.invoice
        io, = self.rep.rows
        io.forced_discount_pct = 10
        self.rep.update()

        return [self.rep]

    def get_result(self):
        return RequiredResult(transition=IssueTransitions.wont_fix, not_fixed=[self.rep])


class SomethingNotChangedCase(AbstractDBTestCase):
    _representation = 'something_not_changed'

    def __init__(self):
        super(SomethingNotChangedCase, self).__init__()
        self.rep_good = None
        self.rep_bad = None

    def _get_data(self, session):
        client, person = db_utils.create_client_person(session)
        order_good = db_utils.create_order(session, client)
        order_bad = db_utils.create_order(session, client, product_id=504056)

        month = mapper.ActMonth()
        contract = db_utils.create_general_contract(session, client, person, month.begin_dt,
                                                    payment_type=3, services={7, 70})
        fictive_good = db_utils.create_fictive_invoice(session, contract, [(order_good, 10)])
        fictive_bad = db_utils.create_fictive_invoice(session, contract, [(order_bad, 10)])

        order_good.calculate_consumption(month.document_dt, {order_good.shipment_type: 7})
        order_bad.calculate_consumption(month.document_dt, {order_bad.shipment_type: 10})

        act_good, = db_utils.generate_acts(client, month, dps=[fictive_good.deferpay.id])
        act_bad, = db_utils.generate_acts(client, month, dps=[fictive_bad.deferpay.id])

        self.rep_good = act_good.invoice
        self.rep_bad = act_bad.invoice

        db_utils.cut_agava(client, month, [fictive_good.deferpay])

        io, = self.rep_bad.rows
        io.forced_discount_pct = 10
        self.rep_bad.update()

        return [self.rep_bad, self.rep_good]

    def get_result(self):
        return RequiredResult(transition=IssueTransitions.fixed,
                              fixed=[self.rep_good],
                              not_fixed=[self.rep_bad])


def parse_manager(*args, **kwargs):
    return 'manager'


@mock.patch('autodasha.solver_cl.kopeechki.KopeechkiSolver.parse_manager', parse_manager)
@pytest.mark.parametrize('issue_data',
                         [case() for case in AbstractDBTestCase._cases],
                         ids=lambda case: str(case),
                         indirect=['issue_data'])
def test_db(session, issue_data):
    queue_object, st_issue, case = issue_data
    required_res = case.get_result()

    solver = KopeechkiSolver(queue_object, st_issue)
    res = solver.solve()

    if required_res is None:
        assert res.commit is False
        assert res.delay is True
        assert res.issue_report is None
        return

    assert res.commit is True
    assert res.delay is (required_res.transition == IssueTransitions.none)
    report = res.issue_report

    assert len(report.comments) <= 1
    assert set(required_res.comments) == case_utils.prepare_comment(report.comment)
    assert required_res.transition == report.transition
    assert required_res.assignee == report.assignee
    assert set(required_res.summonees) == set(report.summonees or [])

    for invoice, sum_ in required_res.invoices_states:
        assert invoice.effective_sum == sum_
