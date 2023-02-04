# -*- coding: utf-8 -*-

from __future__ import unicode_literals

import time
import uuid
import functools
import itertools
import os
import datetime as dt

import pytest
import sqlalchemy as sa

from balance.application import getApplication, Application

from autodasha.core.api.tracker import TrackerAgent
from autodasha.db import mapper as a_mapper

from tests import test_application
from tests.autodasha_tests.common import TestConfig
from case_utils import DBIssueCreator, ExportCheckQueueCreator, SolverQueueCreator, DBIssueDescription


@pytest.fixture(scope='session')
def app():
    os.environ['NLS_LANG'] = 'AMERICAN_CIS.UTF8'
    os.environ['NLS_NUMERIC_CHARACTERS'] = '. '
    try:
        return getApplication()
    except RuntimeError:
        return test_application.ApplicationForTests()


@pytest.fixture(scope='session')
def tracker(app):
    address = app.cfg.findtext('AutodashaSettings/Tracker/Address')
    token = app.cfg.findtext('AutodashaSettings/OAuth/StartrekToken')
    return TrackerAgent(token, address)


@pytest.fixture(scope='session')
def st_client(tracker):
    return tracker.st_client


@pytest.fixture
def run_tag():
    return uuid.uuid4().hex


@pytest.fixture
def session(app):
    dbhelper = app.get_new_dbhelper(database_id=app.database_id)
    sessionmaker, _ = dbhelper.create_sessionmaker(app.database_id)
    sessionmaker.configure(autocommit=True)

    session = sessionmaker()
    session.oper_id = app.cfg.findtext('AutodashaSettings/OperID')

    return session


@pytest.fixture
def config(session, app, run_tag):
    items = session.query(a_mapper.TblConfig).all()
    items = dict((item.item, item.value) for item in items)
    items['ISSUES_QUERY'] = 'Queue: PAYSUP Tags: %s' % run_tag
    items['responsible_manager'] = 'mscnad7'
    items['accountants_departments'] = ['yandex_fin_control_acc_income', 'yandex_rkub_taxi_fin_4582', 'yandex_monetize_market_5629_dep24331']
    items['accountants_logins'] = ['bahira']
    items['support_logins'] = ['autodasha', 'mscnad7', app.cfg.findtext('AutodashaSettings/SelfLogin')]
    items['STARTREK_NEW_COMMIT_SCHEME'] = True
    solvers = session.query(a_mapper.SolverSettings).\
        filter(a_mapper.SolverSettings.enabled == sa.text('1'))
    items['solvers'] = {row.classname for row in solvers}

    # т.к. конфиг - синглтон, то принудительно обновляем параметры
    # иначе иногда он переиспользуется со старыми параметрами и печальными последствиями
    config_ = TestConfig()
    config_._items = items

    return config_


@pytest.fixture
def descriptions(request, session):
    with session.begin():
        res = [cls(session) for cls in request.param]
    return res


@pytest.fixture
def description(descriptions):
    description, = descriptions
    return description


@pytest.fixture
def issues_info(descriptions):
    return [desc.get_info() for desc in descriptions]


@pytest.fixture
def db_objects_multiple(descriptions):
    return [desc.get_db_objects() for desc in descriptions]


@pytest.fixture
def db_objects(db_objects_multiple):
    db_objects, = db_objects_multiple
    return db_objects


def _create_issue(tracker, run_tag, issue_info):
    st_client = tracker.st_client
    create_params_lst = ['assignee', 'type', 'priority']
    update_params_lst = ['author', 'resolution']

    create_params = ((k, getattr(issue_info, k, None)) for k in create_params_lst)
    create_params = {k: v for k, v in create_params if v}

    update_params = ((k, getattr(issue_info, k, None)) for k in update_params_lst)
    update_params = {k: v for k, v in update_params if v}

    issue = st_client.issues.create(
        queue='PAYSUP',
        summary=issue_info.summary,
        description=issue_info.description,
        tags=[run_tag],
        components=['balance-support'],
        **create_params
    )

    update_func = issue.update
    status = getattr(issue_info, 'status', None)
    if status:
        transitions = [transition for transition in issue.transitions if transition.to.key == status]
        if transitions:
            transition, = transitions
            update_func = transition.execute

    update_func(**update_params)
    return issue


@pytest.fixture
def tracker_issues(issues_info, tracker, run_tag):
    res = map(functools.partial(_create_issue, tracker, run_tag), issues_info)
    time.sleep(5)
    return res


@pytest.fixture
def tracker_issue(tracker_issues):
    tracker_issue, = tracker_issues
    return tracker_issue


@pytest.fixture
def db_issue_creators(request, tracker_issues):
    descrs = getattr(request, 'param', [])
    data_rows = itertools.izip_longest(tracker_issues, descrs, fillvalue=DBIssueDescription())
    return map(lambda x: DBIssueCreator(*x), data_rows)


@pytest.fixture
def export_check_queue_creators(request, db_issue_creators, db_objects_multiple):
    data_rows = itertools.izip(request.param, db_issue_creators, db_objects_multiple)
    return map(lambda x: ExportCheckQueueCreator(*x), data_rows)


@pytest.fixture
def solver_queue_creators(request, db_issue_creators, issues_info):
    data_rows = itertools.izip(request.param, db_issue_creators, issues_info)
    return map(lambda x: SolverQueueCreator(*x), data_rows)


@pytest.fixture
def db_init(request, session):
    req_fixtures = {'solver_queue_creators', 'export_check_queue_creators', 'db_issue_creators'}
    with session.begin():
        for fixture_name in req_fixtures & set(request.fixturenames):
            for val in request.getfixturevalue(fixture_name):
                val.init_db(session)


@pytest.fixture
def db_issues(db_issue_creators, db_init):
    return [o.data for o in db_issue_creators]


@pytest.fixture
def db_issue(db_issues):
    db_issue, = db_issues
    return db_issue


@pytest.fixture
def export_check_queues(export_check_queue_creators, db_init):
    return [o.data for o in export_check_queue_creators]


@pytest.fixture
def export_check_queue(export_check_queues):
    export_check_queue, = export_check_queues
    return export_check_queue


@pytest.fixture
def solver_queues(solver_queue_creators, db_init):
    return [o.data for o in solver_queue_creators]


@pytest.fixture
def solver_queue(solver_queues):
    solver_queue, = solver_queues
    return solver_queue
