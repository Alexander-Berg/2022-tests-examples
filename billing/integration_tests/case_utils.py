# -*- coding: utf-8 -*-

import datetime as dt
import collections

from balance import muzzle_util as ut

from autodasha.core.queue import QueueProcessingLoop
from autodasha.db import mapper as a_mapper


class AbstractIssueDescription(object):
    representation = 'abstract'

    def __str__(self):
        return self.representation

    def __init__(self, session):
        pass

    def get_info(self):
        raise NotImplementedError


def description_ids(case_params):
    return ', '.join(map(lambda c: c.representation, case_params))


class AbstractDBIssueDescription(AbstractIssueDescription):
    ATTRIBUTES = ['assignee', 'type', 'priority', 'author', 'resolution', 'status', 'solver']

    summary = ''
    _description = ''

    def __init__(self, session):
        super(AbstractDBIssueDescription, self).__init__(session)

        self._init_data(session)

    def get_info(self):
        db_data = self._format_data()
        data = {
            'summary': self.summary,
            'description': self._description.format(**db_data)
        }

        attrs = ((k, getattr(self, k, None)) for k in self.ATTRIBUTES)
        data.update({k: v for k, v in attrs if v})
        return ut.Struct(data)

    def get_db_objects(self):
        raise NotImplementedError

    def _init_data(self, session):
        raise NotImplementedError

    def _format_data(self):
        raise NotImplementedError


class FilteringLoop(QueueProcessingLoop):
    def __init__(self, ticket_ids, *args, **kwargs):
        self.ticket_ids = ticket_ids
        super(FilteringLoop, self).__init__(*args, **kwargs)

    def _fetch(self, session):
        while True:
            res = super(FilteringLoop, self)._fetch(session)
            if res is None:
                return None
            elif res.object_id in self.ticket_ids:
                return res
            else:
                res.state = 1
                session.flush()


class AbstractDBCreator(object):
    def __init__(self):
        self._data = None

    def init_db(self, session):
        if self._data is None:
            self._do_init(session)

    def _do_init(self, session):
        raise NotImplemented

    @property
    def data(self):
        return self._data


class DBIssueDescription(object):
    def __init__(self, solver=None, resolve_dt=None, create_dt=None):
        self.solver = solver
        self.resolve_dt = resolve_dt
        self.create_dt = create_dt


class DBIssueCreator(AbstractDBCreator):
    def __init__(self, tracker_issue, description):
        super(DBIssueCreator, self).__init__()
        self._tracker_issue = tracker_issue
        self._description = description

    def _do_init(self, session):
        self._data = a_mapper.Issue(id=self._tracker_issue.id,
                                    key=self._tracker_issue.key,
                                    create_dt=self._description.create_dt or dt.datetime.now(),
                                    solver=self._description.solver,
                                    resolve_dt=self._description.resolve_dt)
        session.add(self._data)


class ExportCheckQueueDescription(object):
    def __init__(self, state):
        self.state = state


class ExportCheckQueueCreator(AbstractDBCreator):
    def __init__(self, description, db_issue, db_objects):
        super(ExportCheckQueueCreator, self).__init__()
        self._description = description
        self._db_issue = db_issue
        self._db_objects = db_objects

    def _do_init(self, session):
        self._db_issue.init_db(session)
        db_issue = self._db_issue.data

        self._data = db_issue.enqueue('EXPORT_CHECK')
        for obj in self._db_objects:
            oebs_export = obj.exports['OEBS']
            self._data.add_object(obj, 'OEBS')
            oebs_export.state = self._description.state


class SolverExportQueueDescription(object):
    def __init__(self, state, processed_dt=None, next_dt=None, solver=None):
        self.state = state
        self.processed_dt = processed_dt
        self.next_dt = next_dt
        self.solver = solver


class SolverQueueCreator(AbstractDBCreator):
    def __init__(self, description, db_issue, issue_info):
        super(SolverQueueCreator, self).__init__()
        self._description = description
        self._db_issue = db_issue
        self._issue_info = issue_info

    def _do_init(self, session):
        self._db_issue.init_db(session)
        db_issue = self._db_issue.data

        self._data = db_issue.enqueue('SOLVER', {'solver': self._description.solver or self._issue_info.solver})
        self._data.state = self._description.state

        if self._description.next_dt:
            self._data.next_dt = self._description.next_dt

        if self._description.processed_dt:
            self._data.processed_dt = self._description.processed_dt
