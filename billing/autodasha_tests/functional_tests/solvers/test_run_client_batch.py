# -*- coding: utf-8 -*-

from __future__ import unicode_literals

import pytest
from sqlalchemy import orm

import balance.mapper as mapper

from autodasha.db import mapper as a_mapper
from autodasha.core.api.tracker import IssueTransitions
from autodasha.solver_cl import RunClientBatch

from tests.autodasha_tests.functional_tests import case_utils
from tests.autodasha_tests.functional_tests import mock_utils
from tests.autodasha_tests.common import db_utils


class RequiredResult(case_utils.RequiredResult):

    def __init__(self, client_id, check_msg=None, **kwargs):
        self.client_id = client_id
        self.check_msg = check_msg
        self.reexported_clients = []
        super(RequiredResult, self).__init__(**kwargs)

    def set_messages(self, enqueued=False, **kwargs):
        if enqueued:
            self.reexported_clients = [self.client_id]
            self.add_message('Расчёт запущен.')


class AbstractDBTestCase(case_utils.AbstractDBTestCase):
    _cases = []
    __metaclass__ = case_utils.TestCaseMeta

    summary = 'Запуск расчётов'
    issue_key = 'test_client_batch'

    def __init__(self):
        super(AbstractDBTestCase, self).__init__()
        self.client_id = None

    def get_description(self, session):
        calc_type, client_id = self._get_data(session)

        description = 'Тип расчёта: %s' % calc_type
        if client_id:
            description += '\nObject id: %s' % client_id

        return description


class PremiumCaseCase(AbstractDBTestCase):
    _representation = 'premium'

    def _get_data(self, session):
        self.client_id = db_utils.create_client(session).id

        return 'Премия', self.client_id

    def get_result(self):
        return RequiredResult(self.client_id, '/usr/bin/yb-ar-calculate', transition=IssueTransitions.none, enqueued=1)


class CommissionCaseCase(AbstractDBTestCase):
    _representation = 'commission'

    def _get_data(self, session):
        self.client_id = db_utils.create_client(session).id

        return 'Комиссия', self.client_id

    def get_result(self):
        return RequiredResult(self.client_id, 'bo.pk_comm.calc2()', transition=IssueTransitions.none, enqueued=1)


class RetroCaseCase(AbstractDBTestCase):
    _representation = 'retro'

    def _get_data(self, session):
        self.client_id = db_utils.create_client(session).id

        return 'Ретроскидка', self.client_id

    def get_result(self):
        return RequiredResult(self.client_id, "bo.pk_comm.calc_comm('retro', 0, 1)",
                              transition=IssueTransitions.none, enqueued=1)


@pytest.mark.parametrize('issue_data',
                         [_case() for _case in AbstractDBTestCase._cases],
                         ids=lambda case: str(case),
                         indirect=['issue_data'])
def test_db(session, issue_data):
    queue_object, st_issue, case = issue_data
    req_res = case.get_result()

    solver = RunClientBatch(queue_object, st_issue)
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
    assert set(req_res.comments) == case_utils.prepare_comment(report.comment)
    assert req_res.transition == report.transition
    assert req_res.assignee == report.assignee

    client = session.query(mapper.Client).getone(req_res.client_id)
    exp = client.exports.get('CLIENT_BATCH')
    if req_res.check_msg:
        assert exp.state == 0
        assert req_res.check_msg in exp.input
    else:
        assert not exp or exp.state == 1

    try:
        export_queue = session.query(a_mapper.QueueObject).\
            filter(a_mapper.QueueObject.issue == queue_object.issue,
                   a_mapper.QueueObject.processor == 'EXPORT_CHECK').\
            one()
    except orm.exc.NoResultFound:
        assert not req_res.reexported_clients
    else:
        assert all(obj.classname == 'Client' and obj.type == 'CLIENT_BATCH' for obj in export_queue.proxies)
        req_reexp_clients = {obj.object_id for obj in export_queue.proxies}

        assert req_reexp_clients == set(req_res.reexported_clients)
