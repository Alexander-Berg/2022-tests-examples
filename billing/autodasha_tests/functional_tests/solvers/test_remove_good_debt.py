# -*- coding: utf-8 -*-

from __future__ import unicode_literals

import StringIO
import datetime as dt

import pytest

from balance import mapper
from balance.xls_export import get_excel_document
from balance import muzzle_util as ut

from autodasha.solver_cl import RemoveGoodDebt
from autodasha.core.api.tracker import IssueTransitions

from tests.autodasha_tests.functional_tests import case_utils
from tests.autodasha_tests.common import db_utils


class RequiredResult(case_utils.RequiredResult):
    _comments = {
        'already_solved': 'Эта задача уже была выполнена.',
        'no_file': 'Список по задаче отсутствует.',
        'wrong_file_format': 'Неправильный формат файла.',
        'act_not_found': 'Акт %s не обнаружен в балансе',
        'not_paid': 'Акт %s не был включен в расчёт лимита кредита по причине отсутствия оплаты.',
        'no_acts': 'Не актов для включения в расчёт лимита кредита.',
        'partial_ok': 'Акты включены в расчёт лимита кредита.',
        'full_ok': 'Все акты включены в расчёт лимита кредита.',
        'not_good': 'Акт %s не был обработан, т.к. был включен в расчет лимита кредита другим сотрудником.',
        'not_paid_good': 'По акту %s нет оплат, акт включен в расчет лимита кредита другим сотрудником.',
    }

    def __init__(self, **kwargs):
        self.acts_states = []

        super(RequiredResult, self).__init__(**kwargs)

    def set_messages(self, **kwargs):
        if kwargs.get('no_acts'):
            self.add_message(self._comments['no_acts'])

        if kwargs.get('partial_ok'):
            self.add_message(self._comments['partial_ok'])

        if kwargs.get('no_file'):
            self.add_message(self._comments['no_file'])

        if kwargs.get('wrong_file_format'):
            self.add_message(self._comments['wrong_file_format'])

        if kwargs.get('full_ok'):
            self.add_message(self._comments['full_ok'])

        for eid in kwargs.get('not_found', []):
            self.add_message(self._comments['act_not_found'] % eid)

    def add_state(self, act, good_debt):
        self.acts_states.append((act, good_debt))

    def set_object_states(self, bad=None, unpaid=None, bad_unpaid=None, not_good=None, **kwargs):
        for act in bad or []:
            self.add_state(act, 0)

        for act in unpaid or []:
            self.add_state(act, 1)
            self.add_message(self._comments['not_paid'] % act.external_id)

        for act in bad_unpaid or []:
            self.add_state(act, 0)
            self.add_message(self._comments['not_paid_good'] % act.external_id)

        for act in not_good or []:
            self.add_state(act, 0)
            self.add_message(self._comments['not_good'] % act.external_id)


class AbstractDBTestCase(case_utils.AbstractDBTestCase):
    _cases = []
    __metaclass__ = case_utils.TestCaseMeta

    summary = 'Разблокированы оплаченные акты'
    _description = 'Были найдены оплаченные акты исключенные из расчета лимита кредита.'

    def __init__(self):
        self._client = None
        self._person = None
        self._order = None
        self._pa = None

    @staticmethod
    def _get_file(acts):
        buffer = StringIO.StringIO()

        titles = ['act_id', 'dt', 'amount', 'paid_amount', 'invoice_id', 'manager_name', 'email']

        def get_row(a):
            return a.external_id, a.dt, a.amount, a.paid_amount, a.invoice.external_id, '', ''

        buffer.write(get_excel_document(titles, map(get_row, acts), 0))
        return buffer.getvalue()

    @staticmethod
    def _get_fake_act(eid):
        return ut.Struct(
            external_id=eid,
            dt=dt.datetime.now(),
            amount=766,
            paid_amount=667,
            invoice=ut.Struct(external_id='Б-123456-7')
        )

    def _init_objects(self, session):
        month = mapper.ActMonth()
        self._client, self._person = db_utils.create_client_person(session, create_agency=True)
        self._order = db_utils.create_order(session, self._client)

        self._pa = db_utils.create_personal_account(session, self._client.agency, self._person, dt_=month.begin_dt)

    def _get_act(self, session):
        if not self._pa:
            self._init_objects(session)

        month = mapper.ActMonth()
        self._pa.transfer(self._order, 2, 10, skip_check=True)
        self._order.calculate_consumption(month.document_dt,
                                          {self._order.shipment_type: self._order.completion_qty + 10})
        act, = db_utils.generate_acts(self._pa.client, month, invoices=[self._pa.id])
        return act

    def _make_paid(self, act, delta=0):
        act.invoice.receipt_sum_1c = act.invoice.total_act_sum - delta
        act.invoice.update_paid_amount()


class FullOkCase(AbstractDBTestCase):
    _representation = 'full_ok'

    def __init__(self):
        super(FullOkCase, self).__init__()
        self._acts = None

    def _get_data(self, session):
        act1 = self._get_act(session)
        self._make_paid(act1)
        act1.good_debt = 1

        act2 = self._get_act(session)
        self._make_paid(act2)
        act2.good_debt = 1

        self._acts = [act1, act2]

        session.flush()
        return {}

    def get_result(self):
        return RequiredResult(transition=IssueTransitions.fixed,
                              bad=self._acts,
                              full_ok=True)

    def get_attachments(self):
        return [('data0.xls', self._get_file(self._acts))]


class NotPaidPartialCase(AbstractDBTestCase):
    _representation = 'not_paid_partial'

    def __init__(self):
        super(NotPaidPartialCase, self).__init__()
        self._paid_act = None
        self._unpaid_act = None

    def _get_data(self, session):
        self._paid_act = self._get_act(session)
        self._make_paid(self._paid_act)
        self._paid_act.good_debt = 1

        self._unpaid_act = self._get_act(session)
        self._unpaid_act.good_debt = 1

        session.flush()
        return {}

    def get_result(self):
        return RequiredResult(transition=IssueTransitions.none,
                              assignee='mscnad7',
                              bad=[self._paid_act],
                              unpaid=[self._unpaid_act],
                              partial_ok=True)

    def get_attachments(self):
        return [('data0.xls', self._get_file([self._unpaid_act, self._paid_act]))]


class NotPaidAllCase(AbstractDBTestCase):
    _representation = 'not_paid_all'

    def __init__(self):
        super(NotPaidAllCase, self).__init__()
        self._acts = None

    def _get_data(self, session):
        act1 = self._get_act(session)
        self._make_paid(act1, 10)
        act1.good_debt = 1

        act2 = self._get_act(session)
        act2.good_debt = 1

        self._acts = [act1, act2]

        session.flush()
        return {}

    def get_result(self):
        return RequiredResult(transition=IssueTransitions.none,
                              assignee='mscnad7',
                              unpaid=self._acts,
                              no_acts=True)

    def get_attachments(self):
        return [('data0.xls', self._get_file(self._acts))]


class NotPaidNotGoodCase(AbstractDBTestCase):
    _representation = 'not_paid_not_good'

    def __init__(self):
        super(NotPaidNotGoodCase, self).__init__()
        self._good = None
        self._bad_unpaid = None
        self._bad_part_unpaid = None

    def _get_data(self, session):
        self._good = self._get_act(session)
        self._make_paid(self._good)
        self._good.good_debt = 1

        self._bad_unpaid = self._get_act(session)
        self._bad_unpaid.good_debt = 0

        self._bad_part_unpaid = self._get_act(session)
        self._make_paid(self._bad_part_unpaid, 10)
        self._bad_part_unpaid.good_debt = 0

        session.flush()
        return {}

    def get_result(self):
        return RequiredResult(transition=IssueTransitions.none,
                              assignee='mscnad7',
                              bad=[self._good],
                              bad_unpaid=[self._bad_unpaid, self._bad_part_unpaid],
                              partial_ok=True)

    def get_attachments(self):
        return [('data0.xls', self._get_file([self._good, self._bad_part_unpaid, self._bad_unpaid]))]


class NotGoodCase(AbstractDBTestCase):
    _representation = 'not_good'

    def __init__(self):
        super(NotGoodCase, self).__init__()
        self._good = None
        self._bad = None

    def _get_data(self, session):
        self._good = self._get_act(session)
        self._make_paid(self._good)
        self._good.good_debt = 1

        self._bad = self._get_act(session)
        self._make_paid(self._bad)
        self._bad.good_debt = 0

        session.flush()
        return {}

    def get_result(self):
        return RequiredResult(transition=IssueTransitions.none,
                              assignee='mscnad7',
                              bad=[self._good],
                              not_good=[self._bad],
                              partial_ok=True)

    def get_attachments(self):
        return [('data0.xls', self._get_file([self._good, self._bad]))]


class NotFoundPartialCase(AbstractDBTestCase):
    _representation = 'not_found_partial'

    def __init__(self):
        super(NotFoundPartialCase, self).__init__()
        self._found_act = None
        self._not_found_eid = None

    def _get_data(self, session):
        self._found_act = self._get_act(session)
        self._make_paid(self._found_act)
        self._found_act.good_debt = 1

        self._not_found_eid = 'i_love_cactuses'

        session.flush()
        return {}

    def get_result(self):
        return RequiredResult(transition=IssueTransitions.none,
                              assignee='mscnad7',
                              bad=[self._found_act],
                              not_found=[self._not_found_eid],
                              partial_ok=True)

    def get_attachments(self):
        return [('data0.xls', self._get_file([self._found_act, self._get_fake_act(self._not_found_eid)]))]


class NotFoundAllCase(AbstractDBTestCase):
    _representation = 'not_found_all'

    def __init__(self):
        super(NotFoundAllCase, self).__init__()
        self._eids = None

    def _get_data(self, session):
        self._eids = [
            'i_hate_animals',
            'i_hate_people',
            'i_hate_nature',
            'i_am_samvel94'
        ]
        return {}

    def get_result(self):
        return RequiredResult(transition=IssueTransitions.none,
                              assignee='mscnad7',
                              not_found=self._eids,
                              no_acts=True)

    def get_attachments(self):
        acts = map(self._get_fake_act, self._eids)
        return [('data0.xls', self._get_file(acts))]


class NoFileCase(AbstractDBTestCase):
    _representation = 'no_file'

    def __init__(self):
        super(NoFileCase, self).__init__()

    def _get_data(self, session):
        return {}

    def get_result(self):
        return RequiredResult(transition=IssueTransitions.none,
                              no_file=True,
                              assignee='mscnad7')

    def get_attachments(self):
        return [('data666.xls', '111')]


class WrongColumnsCase(AbstractDBTestCase):
    _representation = 'wrong_columns'

    def __init__(self):
        super(WrongColumnsCase, self).__init__()

    def _get_data(self, session):
        return {}

    def get_result(self):
        return RequiredResult(transition=IssueTransitions.none,
                              wrong_file_format=True,
                              assignee='mscnad7')

    def get_attachments(self):
        buffer = StringIO.StringIO()

        titles = ['act', 'dt']
        rows = [('123456', dt.datetime(2016, 1, 1))]

        buffer.write(get_excel_document(titles, rows, 0))
        return [('data0.xls', buffer.getvalue())]


class WrongFileTypeCase(AbstractDBTestCase):
    _representation = 'wrong_file_type'

    def __init__(self):
        super(WrongFileTypeCase, self).__init__()

    def _get_data(self, session):
        return {}

    def get_result(self):
        return RequiredResult(transition=IssueTransitions.none,
                              wrong_file_format=True,
                              assignee='mscnad7')

    def get_attachments(self):
        return [('data0.xls', 'превед роботам!')]


class AlreadyCommentedCase(AbstractDBTestCase):
    _representation = 'already_commented'

    assignee = 'mscnad7'

    def __init__(self):
        super(AlreadyCommentedCase, self).__init__()

    def _get_data(self, session):
        return {}

    def get_comments(self):
        return [('autodasha', 'Все акты включены в расчёт лимита кредита.')]

    def get_result(self):
        return None


@pytest.mark.parametrize('issue_data',
                         [case() for case in AbstractDBTestCase._cases],
                         ids=lambda case: str(case),
                         indirect=['issue_data'])
def test_db(session, issue_data):
    queue_object, st_issue, case = issue_data
    required_res = case.get_result()

    solver = RemoveGoodDebt(queue_object, st_issue)
    res = solver.solve()

    if required_res is None:
        assert res.commit is False
        assert res.delay is False
        assert res.issue_report is None
        return

    assert res.commit is True
    assert res.delay is False
    report = res.issue_report

    assert len(report.comments) <= 1
    assert set(required_res.comments) == case_utils.prepare_comment(report.comment)
    assert required_res.transition == report.transition
    assert required_res.assignee == report.assignee

    for act, state in required_res.acts_states:
        assert act.good_debt == state
