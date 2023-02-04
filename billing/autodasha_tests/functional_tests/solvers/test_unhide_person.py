# -*- coding: utf-8 -*-

from __future__ import unicode_literals

import pytest
from sqlalchemy import orm

from balance import muzzle_util as ut

from autodasha.db import mapper as a_mapper
from autodasha.core.api.tracker import IssueTransitions
from autodasha.solver_cl import UnhidePerson

from tests.autodasha_tests.functional_tests import case_utils
from tests.autodasha_tests.common import db_utils


class RequiredResult(case_utils.RequiredResult):
    def __init__(self, person, is_changed=False, **kwargs):
        self.person = person
        self.is_hidden = None
        self.check_state = None
        if is_changed:
            self.is_hidden = 0
            self.check_state = {('Person', 'OEBS', self.person.id)}
        elif person:
            self.is_hidden = person.hidden
            self.check_state = set()

        super(RequiredResult, self).__init__(**kwargs)


class AbstractDBTestCase(case_utils.AbstractDBTestCase):
    _summary = 'Разархивирование плательщика'
    _description = '''
Название, id плательщика: Иванов Иван Иванович (ID: {p.id})
Комментарий: патамучта гладиолус
'''

    issue_key = 'test_unhide_person'

    _cases = []
    __metaclass__ = case_utils.TestCaseMeta

    def __init__(self):
        super(AbstractDBTestCase, self).__init__()
        self.person = None


class ChangeCase(AbstractDBTestCase):
    _representation = 'change'

    def _get_data(self, session):
        self.person = db_utils.create_person(session)
        self.person.hidden = 1
        return {'p': self.person}

    def get_result(self):
        res = RequiredResult(self.person, True, transition=IssueTransitions.none)
        res.add_message('Плательщик %s (ID %s) разархивирован.' % (self.person.name, self.person.id))
        res.add_message('Разархивированный плательщик добавлен в очередь на выгрузку в ОЕБС.')
        return res


class AlreadyUnhiddenCase(AbstractDBTestCase):
    _representation = 'already_unhidden'

    def _get_data(self, session):
        self.person = db_utils.create_person(session)
        return {'p': self.person}

    def get_result(self):
        res = RequiredResult(self.person, False, transition=IssueTransitions.wont_fix)
        msg = 'Плательщик %s (ID %s) сейчас не заархивирован.' \
              ' Если плательщик был указан неправильно, пожалуйста, заполни форму с верными данными еще раз.'
        res.add_message(msg % (self.person.name, self.person.id))
        return res


class NonexistentIDCase(AbstractDBTestCase):
    _representation = 'nonexistent_id'

    def _get_data(self, session):
        return {'p': ut.Struct(id='0000', name='Медвежатко')}

    def get_result(self):
        res = RequiredResult(None, False, transition=IssueTransitions.wont_fix)
        msg = 'Некорректно указан ID плательщика. Пожалуйста, заполни форму с верными данными еще раз.'
        res.add_message(msg)
        return res


class AlreadyCommentedCase(AbstractDBTestCase):
    _representation = 'already_commented'

    def _get_data(self, session):
        return {'p': ut.Struct(id='0000', name='Медвежатко')}

    def get_comments(self):
        return [('autodasha', 'Плательщик Медвежатко (ID -666666) разархивирован.')]

    def get_result(self):
        return None


@pytest.mark.parametrize('issue_data',
                         [case() for case in AbstractDBTestCase._cases],
                         ids=lambda case: str(case),
                         indirect=['issue_data'])
def test_db(session, issue_data):
    queue_object, st_issue, case = issue_data
    req_res = case.get_result()

    solver = UnhidePerson(queue_object, st_issue)
    res = solver.solve()
    session.flush()

    if req_res is None:
        assert res.commit is False
        assert res.delay is False
        assert res.issue_report is None
        return

    assert res.commit is True
    assert res.delay is False
    report = res.issue_report

    assert len(report.comments) <= 1
    comments_parts = filter(None, map(unicode.strip, report.comment.strip().split('\n')))
    assert set(req_res.comments) == set(comments_parts)
    assert report.transition == req_res.transition
    assert report.assignee == req_res.assignee

    if req_res.person:
        person = req_res.person
        assert person.hidden == req_res.is_hidden

        try:
            export_queue = session.query(a_mapper.QueueObject). \
                filter(a_mapper.QueueObject.issue == queue_object.issue,
                       a_mapper.QueueObject.processor == 'EXPORT_CHECK'). \
                one()
        except orm.exc.NoResultFound:
            assert not req_res.check_state
        else:
            check_state = {(obj.classname, obj.type, obj.object_id) for obj in export_queue.proxies}
            assert check_state == req_res.check_state
