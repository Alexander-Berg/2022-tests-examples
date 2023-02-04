# -*- coding: utf-8 -*-

from __future__ import unicode_literals

from collections import namedtuple
import datetime as dt
import itertools

import mock
import pytest

from cluster_tools.autodasha_solver import SolverProcessor
from autodasha.solver_cl.base_solver import SolveResult
from autodasha.core.api.tracker import IssueReport, exceptions

from tests.autodasha_tests.common import db_utils
import case_utils


@pytest.fixture
def loop(config, tracker, tracker_issues):
    processor = SolverProcessor(config, tracker, mock.MagicMock())
    return case_utils.FilteringLoop({i.id for i in tracker_issues}, processor)


class UnhidePersonIssueDescription(case_utils.AbstractDBIssueDescription):
    representation = 'unhide_person'
    solver = 'UnhidePerson'

    summary = 'Разархивирование плательщика'

    _description = '''
Название, id плательщика: {p.name} (ID: {p.id})
Комментарий: патамучта гладиолус
'''.strip()

    assignee = 'autodasha'

    def _init_data(self, session):
        self._person = db_utils.create_person(session)
        self._person.hidden = 1

    def _format_data(self):
        return {
            'p': self._person
        }

    def get_db_objects(self):
        return self._person


queue_new = case_utils.SolverExportQueueDescription(0)


@pytest.mark.usefixtures('solver_queues')
@pytest.mark.parametrize('solver_queue_creators', [[queue_new] * 2], indirect=['solver_queue_creators'], ids=['^'])
@pytest.mark.parametrize('descriptions', [[UnhidePersonIssueDescription] * 2], indirect=['descriptions'], ids=['^'])
def test_solve_multiple(loop, session, db_issues, db_objects_multiple, tracker_issues):
    start_dt = dt.datetime.now().replace(microsecond=0)
    loop.run(session)

    for db_issue, person, tracker_issue in itertools.izip(db_issues, db_objects_multiple, tracker_issues):
        q_solver = db_issue.queue_objects['SOLVER']
        assert q_solver.state == 1
        assert q_solver.rate == 0
        assert q_solver.processed_dt >= start_dt
        assert db_issue.resolve_dt >= start_dt

        comment = list(tracker_issue.comments)[-1]
        assert comment.createdBy.id == 'autodasha'
        req_cmt = {
            'Плательщик {p.name} (ID {p.id}) разархивирован.'.format(p=person),
            'Разархивированный плательщик добавлен в очередь на выгрузку в ОЕБС.'
        }
        assert set(comment.text.split('\n')) == req_cmt
        assert person.hidden == 0

        q_export = db_issue.queue_objects['EXPORT_CHECK']
        assert q_export.state == 0
        assert q_export.rate == 0
        assert q_export.next_dt >= start_dt
        assert q_export.next_dt <= dt.datetime.now()


def solve_delay(self):
    person = self.parse_issue(self.issue.description)
    person.hidden = 0
    self.session.flush()
    return None


@pytest.mark.parametrize('solver_queue_creators', [[queue_new]], indirect=['solver_queue_creators'], ids=['0'])
@pytest.mark.parametrize('descriptions', [[UnhidePersonIssueDescription]],
                         indirect=['descriptions'], ids=['0'])
def test_delay(loop, session, db_issue, solver_queue, db_objects, tracker_issue):
    start_dt = dt.datetime.now()
    with mock.patch('autodasha.solver_cl.unhide_person.UnhidePerson._do_solve', solve_delay):
        loop.run(session)

    assert solver_queue.state == 0
    assert solver_queue.rate == 0
    assert solver_queue.processed_dt is None
    assert solver_queue.issue.resolve_dt is None
    assert solver_queue.next_dt > start_dt
    assert db_issue.resolve_dt is None

    assert not list(tracker_issue.comments)

    person = db_objects
    assert person.hidden == 1


def solve_fail(self):
    person = self.parse_issue(self.issue.description)
    person.hidden = 0
    self.session.flush()
    raise Exception('FAIL FOR FAIL THRONE!')


@pytest.mark.parametrize('solver_queue_creators', [[queue_new]], indirect=['solver_queue_creators'], ids=['X'])
@pytest.mark.parametrize('descriptions', [[UnhidePersonIssueDescription]],
                         indirect=['descriptions'], ids=['X'])
def test_fail(loop, session, db_issue, solver_queue, db_objects, tracker_issue):
    start_dt = dt.datetime.now()
    with mock.patch('autodasha.solver_cl.unhide_person.UnhidePerson.solve', solve_fail):
        loop.run(session)

    assert solver_queue.state == 0
    assert solver_queue.rate == 1
    assert solver_queue.processed_dt is None
    assert solver_queue.issue.resolve_dt is None
    assert solver_queue.next_dt > start_dt
    assert db_issue.resolve_dt is None

    assert not list(tracker_issue.comments)

    person = db_objects
    assert person.hidden == 1


def solve_comment_commit_delay(self):
    person = self.parse_issue(self.issue.description)
    person.hidden = 0
    self.session.flush()
    return SolveResult(IssueReport(comment='Ждём у моря погоды'), True, True)


@pytest.mark.parametrize('solver_queue_creators', [[queue_new]], indirect=['solver_queue_creators'], ids=['>'])
@pytest.mark.parametrize('descriptions', [[UnhidePersonIssueDescription]],
                         indirect=['descriptions'], ids=[']:'])
def test_comment_commit_delay(loop, session, db_issue, solver_queue, db_objects, tracker_issue):
    start_dt = dt.datetime.now()
    with mock.patch('autodasha.solver_cl.unhide_person.UnhidePerson.solve', solve_comment_commit_delay):
        loop.run(session)

    assert solver_queue.state == 0
    assert solver_queue.rate == 0
    assert solver_queue.processed_dt is None
    assert solver_queue.issue.resolve_dt is None
    assert solver_queue.next_dt > start_dt
    assert db_issue.resolve_dt is None

    assert [c['text'] for c in tracker_issue.comments] == ['Ждём у моря погоды']

    person = db_objects
    assert person.hidden == 0


def solve_comment_delay(self):
    person = self.parse_issue(self.issue.description)
    person.hidden = 0
    self.session.flush()
    return SolveResult(IssueReport(comment='Ждём у моря погоды'), False, True)


@pytest.mark.parametrize('solver_queue_creators', [[queue_new]], indirect=['solver_queue_creators'], ids=['>'])
@pytest.mark.parametrize('descriptions', [[UnhidePersonIssueDescription]],
                         indirect=['descriptions'], ids=['>'])
def test_comment_delay(loop, session, db_issue, solver_queue, db_objects, tracker_issue):
    start_dt = dt.datetime.now()
    with mock.patch('autodasha.solver_cl.unhide_person.UnhidePerson.solve', solve_comment_delay):
        loop.run(session)

    assert solver_queue.state == 0
    assert solver_queue.rate == 0
    assert solver_queue.processed_dt is None
    assert solver_queue.issue.resolve_dt is None
    assert solver_queue.next_dt > start_dt
    assert db_issue.resolve_dt is None

    assert [c['text'] for c in tracker_issue.comments] == ['Ждём у моря погоды']

    person = db_objects
    assert person.hidden == 1


def solve_comment(self):
    person = self.parse_issue(self.issue.description)
    person.hidden = 0
    self.session.flush()
    return SolveResult(IssueReport(comment='Ждём у моря погоды'), False, False)


@pytest.mark.parametrize('solver_queue_creators', [[queue_new]], indirect=['solver_queue_creators'], ids=['<'])
@pytest.mark.parametrize('descriptions', [[UnhidePersonIssueDescription]],
                         indirect=['descriptions'], ids=['<'])
def test_comment(loop, session, db_issue, solver_queue, db_objects, tracker_issue):
    start_dt = dt.datetime.now().replace(microsecond=0)
    with mock.patch('autodasha.solver_cl.unhide_person.UnhidePerson.solve', solve_comment):
        loop.run(session)

    assert solver_queue.state == 1
    assert solver_queue.rate == 0
    assert solver_queue.processed_dt >= start_dt
    assert solver_queue.issue.resolve_dt >= start_dt
    assert solver_queue.next_dt <= start_dt
    assert db_issue.resolve_dt >= start_dt

    assert [c['text'] for c in tracker_issue.comments] == ['Ждём у моря погоды']

    person = db_objects
    assert person.hidden == 1


def solve_silent(self):
    person = self.parse_issue(self.issue.description)
    person.hidden = 0
    self.session.flush()
    return SolveResult(None, False, False)


@pytest.mark.parametrize('solver_queue_creators', [[queue_new]], indirect=['solver_queue_creators'], ids=['<'])
@pytest.mark.parametrize('descriptions', [[UnhidePersonIssueDescription]],
                         indirect=['descriptions'], ids=['>'])
def test_silent(loop, session, db_issue, solver_queue, db_objects, tracker_issue):
    start_dt = dt.datetime.now().replace(microsecond=0)
    with mock.patch('autodasha.solver_cl.unhide_person.UnhidePerson.solve', solve_silent):
        loop.run(session)

    assert solver_queue.state == 1
    assert solver_queue.rate == 0
    assert solver_queue.processed_dt >= start_dt
    assert solver_queue.issue.resolve_dt >= start_dt
    assert solver_queue.next_dt <= start_dt
    assert db_issue.resolve_dt >= start_dt

    assert not list(tracker_issue.comments)

    person = db_objects
    assert person.hidden == 1


def issue_commit_fail(*args, **kwargs):
    raise Exception('Задач биль изменёна, насяльникамана')


@pytest.mark.parametrize('solver_queue_creators', [[queue_new]], indirect=['solver_queue_creators'], ids=['x'])
@pytest.mark.parametrize('descriptions', [[UnhidePersonIssueDescription]], indirect=['descriptions'], ids=['x'])
def test_st_fail(loop, session, db_issue, db_objects, solver_queue, tracker_issue):
    start_dt = dt.datetime.now().replace(microsecond=0)

    with mock.patch('autodasha.core.api.tracker.Issue.commit_changes', issue_commit_fail):
        loop.run(session)

    assert solver_queue.state == 0
    assert solver_queue.rate == 1
    assert solver_queue.processed_dt is None
    assert solver_queue.issue.resolve_dt is None
    assert solver_queue.next_dt > start_dt
    assert db_issue.resolve_dt is None

    assert not list(tracker_issue.comments)

    person = db_objects
    assert person.hidden == 1


def issue_commit_delay(*args, **kwargs):
    raise exceptions.Conflict(namedtuple('Response', ['status_code', 'reason'])(409, 'already changed'))


@pytest.mark.parametrize('solver_queue_creators', [[queue_new]], indirect=['solver_queue_creators'], ids=['x'])
@pytest.mark.parametrize('descriptions', [[UnhidePersonIssueDescription]], indirect=['descriptions'], ids=['x'])
def test_st_delay(loop, session, db_issue, db_objects, solver_queue, tracker_issue):
    start_dt = dt.datetime.now().replace(microsecond=0)

    with mock.patch('autodasha.core.api.tracker.Issue.commit_changes', issue_commit_delay):
        loop.run(session)

    assert solver_queue.state == 0
    assert solver_queue.rate == 0
    assert solver_queue.processed_dt is None
    assert solver_queue.issue.resolve_dt is None
    assert solver_queue.next_dt > start_dt
    assert db_issue.resolve_dt is None

    assert not list(tracker_issue.comments)

    person = db_objects
    assert person.hidden == 1


@pytest.mark.parametrize('solver_queue_creators', [[queue_new]], indirect=['solver_queue_creators'], ids=['<'])
@pytest.mark.parametrize('descriptions', [[UnhidePersonIssueDescription]],
                         indirect=['descriptions'], ids=['>'])
def test_stopped(loop, session, db_issue, solver_queue, db_objects, tracker_issue):
    start_dt = dt.datetime.now().replace(microsecond=0)
    tracker_issue.update(tags=tracker_issue.tags + ['stop_dasha'])
    loop.run(session)

    assert solver_queue.state == 1
    assert solver_queue.rate == 0
    assert solver_queue.processed_dt >= start_dt
    assert solver_queue.issue.resolve_dt >= start_dt
    assert solver_queue.next_dt <= start_dt
    assert db_issue.resolve_dt >= start_dt

    assert not list(tracker_issue.comments)

    person = db_objects
    assert person.hidden == 1
