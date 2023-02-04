# -*- coding: utf-8 -*-

from __future__ import unicode_literals

import itertools
import datetime as dt

from sqlalchemy import orm
import pytest
import mock

from balance import muzzle_util as ut

from cluster_tools.autodasha_enqueue import enqueue_all
from autodasha.core.api.staff import Staff
from autodasha.core.api.tracker import IssueStatuses, IssueResolutions
from autodasha.db import mapper as a_mapper

import case_utils


class BuhDelete(case_utils.AbstractIssueDescription):
    representation = 'buh_delete'

    def get_info(self):
        return ut.Struct(
            summary='Удаление логина бухгалтера',
            description='Test',
            solver='BuhDelete'
        )


class ChangePersonNew(case_utils.AbstractIssueDescription):
    representation = 'change_person_new'

    def get_info(self):
        return ut.Struct(
            summary='Изменение плательщика в счетах',
            description='Test',
            solver='ChangePerson'
        )


class ChangePersonSolved(case_utils.AbstractIssueDescription):
    representation = 'change_person_solved'

    def get_info(self):
        return ut.Struct(
            summary='Изменение плательщика в счетах',
            description='Test',
            status=IssueStatuses.resolved,
            resolution=IssueResolutions.fixed,
            solver='ChangePerson'
        )


class RandomCrap(case_utils.AbstractIssueDescription):
    representation = 'random_crap'

    def get_info(self):
        return ut.Struct(
            summary='Рандомный тестовый тикет',
            description='Кто-то что-то непонятное хочет'
        )


class PromoConnect(case_utils.AbstractIssueDescription):
    representation = 'promo_connect'

    def get_info(self):
        return ut.Struct(
            summary='Зачисление бонуса по промокоду',
            description='Test',
            solver='PromoConnect'
        )


class ReturnCompletedAuthor(case_utils.AbstractIssueDescription):
    representation = 'return_completed_author'

    def get_info(self):
        return ut.Struct(
            summary='Вернула',
            description='Test',
            author='robot-octopool',
            solver='ReturnCompleted'
        )


queue_solved = case_utils.SolverExportQueueDescription(1, dt.datetime.now().replace(microsecond=0))

queue_processing = case_utils.SolverExportQueueDescription(
    0,
    next_dt=dt.datetime.now().replace(microsecond=0) + dt.timedelta(1),
    solver='TEST'
)

queue_not_closed = case_utils.SolverExportQueueDescription(
    1,
    dt.datetime.now().replace(microsecond=0) + dt.timedelta(1)
)

__dt = dt.datetime.now().replace(microsecond=0) - dt.timedelta(1)
queue_reopened = case_utils.SolverExportQueueDescription(
    1,
    __dt,
    __dt
)

db_issue_new = case_utils.DBIssueDescription('TEST')
db_issue_solved = case_utils.DBIssueDescription('TEST', dt.datetime.now().replace(microsecond=0))


@pytest.fixture(scope='session')
def app(app):
    address = app.cfg.findtext('AutodashaSettings/Staff/Address')
    token = app.cfg.findtext('AutodashaSettings/OAuth/StaffToken')
    staff_client = Staff(token, address)
    return app


@pytest.fixture
def tracker(tracker, tracker_issues):
    # тестовый стартрек с завидной регулярность не возвращает в find свежесозданные тикеты
    def issues_find(query):
        for ti in tracker_issues:
            yield tracker.st_client.issues[ti.key]

    patcher = mock.patch.object(tracker.st_client.issues, 'find', issues_find)
    patcher.start()
    yield tracker
    patcher.stop()


@pytest.mark.parametrize('descriptions', [[BuhDelete, ChangePersonNew, PromoConnect]],
                         indirect=['descriptions'], ids=['*'])
def test_detect_multiple(session, tracker, config, tracker_issues):
    start_dt = dt.datetime.now().replace(microsecond=0)
    enqueue_all(session, tracker, config)

    solvers = ['BuhDelete', 'ChangePerson', 'PromoConnect']

    for issue, solver_name in itertools.izip(tracker_issues, solvers):
        db_issue = session.query(a_mapper.Issue).filter(a_mapper.Issue.key == issue.key).one()
        queue = db_issue.queue_objects['SOLVER']
        assert queue.parameters == {'solver': solver_name}
        assert queue.state == 0
        assert queue.rate == 0
        assert start_dt <= queue.next_dt <= dt.datetime.now()

        assert db_issue.solver == solver_name


@pytest.mark.parametrize('descriptions', [[ReturnCompletedAuthor], [ChangePersonSolved], [RandomCrap]],
                         indirect=['descriptions'], ids=case_utils.description_ids)
def test_undetectable_new(session, tracker, config, tracker_issue):
    enqueue_all(session, tracker, config)

    with pytest.raises(orm.exc.NoResultFound):
        session.query(a_mapper.Issue).\
            filter(a_mapper.Issue.key == tracker_issue.key).\
            one()


@pytest.mark.parametrize('solver_queue_creators', [[queue_solved]],
                         indirect=['solver_queue_creators'], ids=[''])
@pytest.mark.parametrize('descriptions', [[ReturnCompletedAuthor], [ChangePersonSolved]],
                         indirect=['descriptions'], ids=case_utils.description_ids)
def test_undetectable_processed(session, tracker, config, solver_queue):
    enqueue_all(session, tracker, config)

    assert solver_queue.state == 1
    assert solver_queue.processed_dt == queue_solved.processed_dt


@pytest.mark.parametrize('solver_queue_creators', [[queue_processing]], indirect=['solver_queue_creators'], ids=[''])
@pytest.mark.parametrize('db_issue_creators', [[db_issue_new]], indirect=['db_issue_creators'], ids=[''])
@pytest.mark.parametrize('descriptions', [[ChangePersonNew]], indirect=['descriptions'], ids=case_utils.description_ids)
def test_still_processing(session, tracker, config, db_issue, solver_queue):
    enqueue_all(session, tracker, config)

    assert solver_queue.next_dt == queue_processing.next_dt
    assert solver_queue.parameters == {'solver': queue_processing.solver}
    assert db_issue.solver == db_issue_new.solver


@pytest.mark.parametrize('solver_queue_creators', [[queue_not_closed]], indirect=['solver_queue_creators'], ids=[''])
@pytest.mark.parametrize('db_issue_creators', [[db_issue_solved]], indirect=['db_issue_creators'], ids=[''])
@pytest.mark.parametrize('descriptions', [[ChangePersonNew]], indirect=['descriptions'], ids=case_utils.description_ids)
def test_processed_not_closed(session, tracker, config, db_issue, solver_queue):
    enqueue_all(session, tracker, config)

    assert solver_queue.state == 1
    assert solver_queue.processed_dt == queue_not_closed.processed_dt
    assert db_issue.solver == db_issue_solved.solver
    assert db_issue.resolve_dt == db_issue_solved.resolve_dt


@pytest.mark.parametrize('solver_queue_creators', [[queue_reopened]], indirect=['solver_queue_creators'], ids=[''])
@pytest.mark.parametrize('db_issue_creators', [[db_issue_solved]], indirect=['db_issue_creators'], ids=[''])
@pytest.mark.parametrize('descriptions', [[ChangePersonNew]], indirect=['descriptions'], ids=case_utils.description_ids)
def test_reopened_after_processing(session, tracker, config, db_issue, solver_queue):
    enqueue_all(session, tracker, config)

    assert solver_queue.state == 0
    assert solver_queue.next_dt > queue_reopened.next_dt
    assert solver_queue.processed_dt == queue_reopened.processed_dt

    assert db_issue.solver == 'ChangePerson'
    assert db_issue.resolve_dt is None

