# -*- coding: utf-8 -*-

from __future__ import unicode_literals

import collections
import functools
import itertools
import uuid

from rep.utils import balance_utils as ut
from rep.core.config import AbstractConfig

__all__ = ['TestConfig', 'create_issue']


class TestConfig(AbstractConfig):
    def __init__(self, items=None):
        self._items = items or dict()

    def __getitem__(self, item):
        return self._items[item.upper()]

    def __setitem__(self, key, value):
        self._items[key.upper()] = value

    def __getattr__(self, item):
        try:
            return self._items[item.upper()]
        except KeyError as e:
            raise AttributeError(*e.args)

    def get(self, key, default=None):
        return self._items.get(key.upper(), default)


def user(login):
    return ut.Struct(id=login, login=login)


def status(key):
    return ut.Struct(key=key)


def type_(key):
    return ut.Struct(key=key)


def priority(id_):
    return ut.Struct(id=str(id_))


def component(id_):
    return ut.Struct(id=int(id_))


def components(comp):
    if isinstance(comp, basestring):
        return [component(comp)]
    elif isinstance(comp, collections.Iterable):
        return [component(c) for c in comp]
    else:
        return [component(comp)]


def queue(key):
    return ut.Struct(key=key)


def changelog_item(obj_info):
    login, dt_, params = obj_info

    res = ut.Struct(
        updatedAt=dt_,
        updatedBy=user(login),
        id=uuid.uuid4().hex
    )
    fields = list()
    res['fields'] = fields

    fields_creators = {
        'type': type_,
        'status': status,
        'queue': queue,
        'assignee': user,
        'createdBy': user,
        'priority': priority,
        'components': components,
    }

    for field, (from_, to) in params.iteritems():
        creator = fields_creators.get(field)
        if creator:
            if from_ is not None:
                from_ = creator(from_)

            if to is not None:
                to = creator(to)

        fields.append({
            'field': ut.Struct(id=field),
            'from': from_,
            'to': to
        })

    return res


def iterable(objects, wrap=None):
    if wrap:
        return itertools.imap(wrap, objects)
    else:
        return iter(objects)


class Collection(object):
    def __init__(self, objects, wrap=None):
        if wrap:
            self._objects = map(wrap, objects)
        else:
            self._objects = list(objects)

    def __iter__(self):
        return iter(self._objects)

    def get_all(self, field=None):
        if field is None:
            return iter(self._objects)
        else:
            fields = field.split(',')
            return itertools.ifilter(lambda cl_el: any(f['field'].id in fields for f in cl_el.fields), self._objects)


def create_issue(params):
    attrs_creators = {
        'assignee': user,
        'createdBy': user,
        'status': status,
        'type': type_,
        'priority': priority,
        'queue': queue,
        'components': functools.partial(iterable, wrap=component),
        'tags': iterable,
        'changelog': functools.partial(Collection, wrap=changelog_item)
    }
    res = ut.Struct()
    for param, value in params:
        creator = attrs_creators.get(param)
        if creator and value is not None:
            value = creator(value)

        res[param] = value

    return res
