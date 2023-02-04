# coding: utf-8
from collections import OrderedDict

import attr
from enum import Enum


class Types(Enum):
    DELETE = 0
    INSERT = 1
    REPLACE = 2
    EDIT = 3
    MOVE = 4
    SAME = 6


@attr.s(repr=False)
class Change(object):
    type = attr.ib()
    id = attr.ib(default=None)
    to_id = attr.ib(default=None)
    diff = attr.ib(default=attr.Factory(list))
    elem = attr.ib(default=None)
    to_elem = attr.ib(default=None)

    # todo-igogor как же не хотелось добавлять этот метод. Вынести в утилитную функцию?
    def combine(self, change):
        if not change:
            return attr.evolve(self)
        if attr.evolve(self, diff=[]) != attr.evolve(change, diff=[]):
            raise ValueError('Can combine only changes of same type and same elements')
        return attr.evolve(change,
                           diff=sorted([d for d in self.diff + change.diff
                                        if (d in self.diff and d not in change.diff)
                                        or (d in change.diff and d not in self.diff)],
                                       key=lambda x: x.id))

    def substract(self, change):
        if not change:
            return attr.evolve(self)
        if attr.evolve(self, diff=[]) != attr.evolve(change, diff=[]):
            raise ValueError('Can substract only changes of same type and same elements')
        else:
            return attr.evolve(self, diff=sorted([d for d in self.diff if d not in change.diff], key=lambda x: x.id))

    def __repr__(self):
        def _optional(attr_name, val):
            return ", {}={}".format(attr_name, repr(val)) if val not in [None, []] else ''

        optional_params = ''.join([_optional(attribute.name, getattr(self, attribute.name))
                                   for attribute in attr.fields(type(self)) if attribute.name != 'type'])
        return '{}(type={}{})'.format(type(self).__name__, str(self.type), optional_params)

    def report(self):
        if self.type is Types.SAME:
            return repr(self.to_elem)
        elif self.type is Types.DELETE:
            return u'Delete({})'.format(repr(self.elem))
        elif self.type is Types.INSERT:
            return u'Insert({})'.format(repr(self.to_elem))
        elif self.type is Types.REPLACE:
            return u'Replace({}, to={})'.format(repr(self.elem), repr(self.to_elem))
        else:
            # todo-igogor
            raise NotImplementedError('Change type is not supported: {}'.format(self.type.name))


@attr.s
class BaseFilter(object):
    filtered = attr.ib(init=False, default=attr.Factory(list), repr=False, cmp=False, hash=False)

    def apply(self, change):
        # type: (Change) -> Tuple[Change, Change]
        raise NotImplementedError('Has to be overriden in subclass')

    def filter(self, change):
        filtered, kept = self.apply(change)
        if filtered:
            self.filtered.append(filtered)

    def smart_repr(self):
        raise NotImplementedError('Has to be overriden in subclass')

    def return_value(self, filtered=None, kept=None):
        """
        Утилитный метод чтобы не перепутать порядо возвращаемых значений apply метода
        """
        return filtered, kept


# todo-igogor эти 2 метода спорные
def report_changes_as_list(changes):
    changes_reports = []
    for change in changes:
        if change.type is Types.INSERT:
            changes_reports.append('{}({}: "{}")'.format(change.type.name, change.to_id, change.to_elem))
        elif change.type is Types.DELETE:
            changes_reports.append('{}({}: "{}")'.format(change.type.name, change.id, change.elem))
        elif change.type is Types.MOVE:
            changes_reports.append('{}({} -> {}: "{}")'.format(change.type.name, change.id, change.to_id, change.elem))
        elif change.type is Types.REPLACE:
            changes_reports.append('{}({}: "{}" -> "{}")'.format(change.type.name, change.id,
                                                                 change.elem, change.to_elem))
        elif change.type is Types.EDIT:
            changes_reports.append('{}({}: "{}")'.format(change.type.name, change.id,
                                                         report_changes_as_dict(change.diff)))
        else:
            raise ValueError('Change type is not supported: {}'.format(change.type))


def report_changes_as_dict(changes):
    changes_str_dict = OrderedDict()
    for change in changes:
        if change.type is Types.INSERT:
            changes_str_dict[change.to_id] = u'{}({})'.format(change.type.name, change.to_elem)
        elif change.type is Types.DELETE:
            changes_str_dict[change.id] = u'{}({})'.format(change.type.name, change.elem)
        elif change.type is Types.REPLACE:
            changes_str_dict[change.id] = u'{}({} -> {})'.format(change.type.name, change.elem, change.to_elem)
        else:
            raise ValueError('Change type is not supported: {}'.format(str(change.type)))
    return changes_str_dict


def sort_by_to_id_and_id(changes):
    return sorted(changes,
                  key=lambda x: (x.id, None) if x.to_id is None else (x.to_id, None) if x.id is None else (
                      x.to_id, x.id))


@attr.s()
class Filtered(object):
    # todo-igogor для модифицирующих фильтров здесь также будут нужны filtered, kept?
    change = attr.ib()
    filter_ = attr.ib()

    def report(self):
        return u'Filtered({}, {})'.format(self.change.report(), self.filter_.report())


def key(operation):
    if isinstance(operation, Filtered):
        operation = operation.change
    return operation.id if operation.type is Types.DELETE else operation.to_id
