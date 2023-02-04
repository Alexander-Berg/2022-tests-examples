# -*- coding: utf-8 -*-

from __future__ import unicode_literals

import itertools
import datetime as dt

import pytest
import mock

from balance import muzzle_util as ut
from balance import mapper

from cluster_tools.autodasha_export_check import ExportCheckProcessor

from tests.autodasha_tests.common import db_utils
import case_utils


@pytest.fixture
def loop(config, tracker, tracker_issues):
    processor = ExportCheckProcessor(config, tracker)
    return case_utils.FilteringLoop({i.id for i in tracker_issues}, processor)


class ChangePersonIssueDescription(case_utils.AbstractDBIssueDescription):
    representation = 'change_person'

    summary = 'Изменение плательщика в счетах'

    _description = '''
№ счета: {invoices}
Название плательщика (на которого надо поменять) и его ID: {person.name} (ID: {person.id})
Способ оплат: {paysys.name}
Причина изменения: патамучта гладиолус'''.strip()

    assignee = 'autodasha'

    def _init_data(self, session):
        dt_ = dt.datetime.now()
        client, person = db_utils.create_client_person(session)

        self._invoices = []
        self._acts = []
        for i in range(2):
            order = db_utils.create_order(session, client, product_id=503995)
            invoice = db_utils.create_invoice(session, client, [(order, 1)], turn_on=1, person=person)
            order.calculate_consumption(dt_ - dt.timedelta(1), {order.shipment_type: 1})
            act, = invoice.generate_act(force=True, backdate=dt_)
            self._invoices.append(invoice)
            self._acts.append(act)

    def _format_data(self):
        return {
            'person': ut.Struct(name='Федя Пупкин', id='666'),
            'paysys': ut.Struct(name='Натурой'),
            'invoices': ' '.join(i.external_id for i in self._invoices)
        }

    def get_db_objects(self):
        return self._invoices + self._acts


def _mk_fail_comment(db_object):
    if isinstance(db_object, mapper.Act):
        cmt = 'Акт {obj.external_id} (счёт {obj.invoice.external_id}) не выгрузился в ОЕБС.'
    elif isinstance(db_object, mapper.Invoice):
        cmt = 'Счёт {obj.external_id} не выгрузился в ОЕБС.'
    else:
        raise TypeError('Unknown object type: %s' % db_object)

    return cmt.format(obj=db_object)


new_queue = case_utils.ExportCheckQueueDescription(state=0)
solved_queue = case_utils.ExportCheckQueueDescription(state=1)
failed_queue = case_utils.ExportCheckQueueDescription(state=2)


@pytest.mark.usefixtures('export_check_queues')
@pytest.mark.parametrize('export_check_queue_creators', [[solved_queue, solved_queue]],
                         indirect=['export_check_queue_creators'], ids=['^'])
@pytest.mark.parametrize('descriptions', [[ChangePersonIssueDescription] * 2],
                         indirect=['descriptions'], ids=['^'])
def test_check_multiple(loop, session, db_issues, tracker_issues):
    start_dt = dt.datetime.now().replace(microsecond=0)
    loop.run(session)

    for db_issue, tracker_issue in itertools.izip(db_issues, tracker_issues):
        q = db_issue.queue_objects['EXPORT_CHECK']
        assert q.state == 1
        assert q.rate == 0
        assert q.processed_dt >= start_dt

        comment = list(tracker_issue.comments)[-1]
        assert comment.createdBy.id == 'autodasha'
        assert comment.text == 'Все объекты успешно выгружены в ОЕБС.'


@pytest.mark.parametrize('export_check_queue_creators', [[new_queue]],
                         indirect=['export_check_queue_creators'], ids=[''])
@pytest.mark.parametrize('descriptions', [[ChangePersonIssueDescription]],
                         indirect=['descriptions'], ids=case_utils.description_ids)
def test_check_waiting(loop, session, export_check_queue, tracker_issue):
    start_dt = dt.datetime.now().replace(microsecond=0)
    loop.run(session)

    assert export_check_queue.state == 0
    assert export_check_queue.rate == 0
    assert not export_check_queue.processed_dt
    assert export_check_queue.next_dt > start_dt
    assert not list(tracker_issue.comments)


@pytest.mark.parametrize('export_check_queue_creators', [[failed_queue]],
                         indirect=['export_check_queue_creators'], ids=[''])
@pytest.mark.parametrize('descriptions', [[ChangePersonIssueDescription]],
                         indirect=['descriptions'], ids=case_utils.description_ids)
def test_check_error(loop, session, export_check_queue, db_objects, tracker_issue):
    start_dt = dt.datetime.now().replace(microsecond=0)
    loop.run(session)

    assert export_check_queue.state == 1
    assert export_check_queue.rate == 0
    assert export_check_queue.processed_dt >= start_dt

    comment = list(tracker_issue.comments)[-1]
    assert comment.createdBy.id == 'autodasha'

    comment_parts = set(comment.text.split('\n'))
    req_comments_parts = set(_mk_fail_comment(obj) for obj in db_objects)
    assert comment_parts == req_comments_parts


def issue_commit_fail(*args, **kwargs):
    raise Exception('Задач биль изменёна, насяльникамана')


@pytest.mark.parametrize('export_check_queue_creators', [[solved_queue]],
                         indirect=['export_check_queue_creators'], ids=[''])
@pytest.mark.parametrize('descriptions', [[ChangePersonIssueDescription]],
                         indirect=['descriptions'], ids=case_utils.description_ids)
def test_check_st_fail(loop, session, export_check_queue, tracker_issue):
    start_dt = dt.datetime.now().replace(microsecond=0)

    with mock.patch('autodasha.core.api.tracker.Issue.commit_changes', issue_commit_fail):
        loop.run(session)

    assert export_check_queue.state == 0
    assert export_check_queue.rate == 1
    assert not export_check_queue.processed_dt
    assert export_check_queue.next_dt > start_dt
    assert {(s, lr) for s, lr, _ in export_check_queue.parameters.itervalues()} == {(0, 0)}
    assert not list(tracker_issue.comments)
