# -*- coding: utf-8 -*-

"""
Мок стаффа
"""

from __future__ import unicode_literals

import itertools
import collections
import datetime as dt

from autodasha.utils.singleton import Singleton
from autodasha.core.api.staff import Staff
from autodasha.core.api.gap import Gap
from autodasha.core.logic.approvers import ApproversSettings
import autodasha.db.scheme as a_scheme

__all__ = ['StaffMock', 'GapMock', 'Person', 'Department', 'create_approvers_settings']

_cr_session = collections.namedtuple('Session', 'execute')
DepartmentSettings = collections.namedtuple('ds', ['solver', 'department', 'person', 'type', 'hierarchical'])
PersonSettings = collections.namedtuple('ps', ['solver', 'src_person', 'dst_person', 'replace'])


def create_approvers_settings(department_rows=None, person_rows=None):
    department_rows = department_rows or []
    person_rows = person_rows or []

    def f_execute(select, **kwargs):
        t_dept = a_scheme.department_approvers
        t_appr = a_scheme.approver_replacements
        sel_ordering = select._order_by_clause.clauses

        if select.froms == [t_dept]:
            assert sel_ordering == [t_dept.c.solver, t_dept.c.department]
            return sorted(department_rows, key=lambda row: (row.solver, row.department))
        elif select.froms == [t_appr]:
            assert sel_ordering == [t_appr.c.solver, t_appr.c.src_person]
            return sorted(person_rows, key=lambda row: (row.solver, row.src_person))
        else:
            raise ValueError('Unknown selectable, gtfo')

    return ApproversSettings(_cr_session(f_execute))


class Department(object):
    def __init__(self, id_, chiefs=None, deputies=None, members=None, childs=None):
        self.id = id_
        self.chiefs = chiefs or []
        self.deputies = deputies or []
        self.members = (members or []) + self.chiefs + self.deputies
        self.childs = childs or []

    def __hash__(self):
        return hash(self.id)

    @property
    def as_dict(self):
        heads = [pi.as_chief for pi in self.chiefs] + [pi.as_deputy for pi in self.deputies]
        return {
            'url': self.id,
            'is_deleted': False,
            'heads': heads,
            'name': {'full': {'ru': 'Department %s' % self.id}}
        }


class Person(object):
    def __init__(self, login):
        self.login = login

    def __hash__(self):
        return hash(self.login)

    @property
    def as_dict(self):
        return {
            "is_deleted": False,
            "official": {
                "is_dismissed": False,
                "is_robot": False
            },
            "login": self.login,
        }

    def as_head(self, role):
        return {
            "person": self.as_dict,
            "role": role,
        }

    @property
    def as_chief(self):
        return self.as_head('chief')

    @property
    def as_deputy(self):
        return self.as_head('deputy')


class DisabledSingleton(Singleton):
    def __call__(cls, *args, **kwargs):
        return super(Singleton, cls).__call__(*args, **kwargs)


class StaffMock(Staff):
    __metaclass__ = DisabledSingleton

    def __init__(self, parent_department):
        self._login2dept = {}
        self._url2dept = {}
        self._dept2parent = {}

        depts = collections.deque([parent_department])
        while depts:
            cur_dept = depts.popleft()
            self._url2dept[cur_dept.id] = cur_dept
            self._login2dept.update({pi.login: cur_dept for pi in cur_dept.members})

            for child in cur_dept.childs:
                depts.append(child)
                self._dept2parent[child] = cur_dept

    def _dept_parents(self, dept):
        parent = self._dept2parent.get(dept)
        while parent:
            yield parent
            dept = parent
            parent = self._dept2parent.get(dept)

    def _dept_childs(self, dept):
        q = collections.deque(dept.childs)
        while q:
            dept = q.popleft()
            yield dept
            q.extend(dept.childs)

    def _get_person_data(self, login):
        dept = self._login2dept[login]
        ancestors = reversed(list(self._dept_parents(dept)))

        return {
            'login': login,
            'work_email': '%s@y-t.ru' % login,
            'department_group': {
                'is_deleted': False,
                'department': dept.as_dict,
                'ancestors': [{'department': d_.as_dict, 'is_deleted': False} for d_ in ancestors]
            }
        }

    def _get_department_data(self, url):
        dept = self._url2dept[url]
        res_depts = itertools.chain([dept], self._dept_childs(dept))

        def di2dict(di):
            return {
                'department': di.as_dict,
                'members': [{'person': pi.as_dict} for pi in di.members]
            }

        return map(di2dict, res_depts)

    def is_person_related_to_departments(self, login, deps):
        deps = set(deps)
        member_data = self._get_person_data(login)
        member_dep = member_data['department_group']['department']['url']

        if member_dep in deps:
            return True

        ancestors_data = member_data['department_group']['ancestors']
        member_ancestors_deps = {dep['department']['url'] for dep in ancestors_data}

        if member_ancestors_deps.intersection(deps):
            return True

        return False


class PersonGap(object):
    def __init__(self, login, type='vacation', full_day=True, work_in_absence=False):
        self.login = login
        self.type = type
        self.full_day = full_day
        self.work_in_absence = work_in_absence

    def as_dict(self, date_from=None, date_to=None):
        date_to = date_to or date_from or dt.datetime.now()
        date_from = date_from or dt.datetime.now()

        return {
            'comment': 'Идите в жопу, я в отпуске!',
            'workflow': self.type,
            'work_in_absence': self.work_in_absence,
            'full_day': self.full_day,
            'person_login': self.login,
            'date_from': date_from.strftime('%Y-%m-%dT%H:%M:%S'),
            'date_to': date_to.strftime('%Y-%m-%dT%H:%M:%S'),
        }


class GapMock(Gap):
    __metaclass__ = DisabledSingleton

    def __init__(self, absent_persons=None):
        absent_persons = absent_persons or []
        self._persons = {p.login: p for p in absent_persons}

    def _find_gaps(self, login, date_from=None, date_to=None):
        p = self._persons.get(login)
        if p:
            return [p.as_dict(date_from, date_to)]
        else:
            return []
