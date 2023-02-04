# coding: utf-8


import attr

from btestlib import seqdiff, utils, diffutils, reporter
from btestlib.diffutils import Change, Types, BaseFilter


def dictdiff(old_dict, new_dict, diff_order=False, filters=None):
    ddiff = DictDiff.diff(old=old_dict, new=new_dict, diff_order=diff_order)
    ddiff.filter(filters=filters)
    return ddiff


def ignore(keys=None, value=None, descr='Add descr'):
    filters = []
    if keys:
        filters.append(FilterKeys(keys=keys, descr=descr))
    if value:
        filters.append(FilterValue(value=value, descr=descr))

    if not filters:
        return ValueError('Must specify at least one parameter except descr')
    else:
        return filters


@attr.s
class DictDiff(object):
    old = attr.ib()
    new = attr.ib()

    operations = attr.ib(default=attr.Factory(list))
    filters = attr.ib(default=attr.Factory(list))

    @property
    def filtered(self):
        filtered_ = [operation.change for operation in self.operations if isinstance(operation, diffutils.Filtered)]
        return diffutils.sort_by_to_id_and_id(filtered_)

    @property
    def changes(self):
        return [change for change in self.operations if getattr(change, u'type', Types.SAME) is not Types.SAME]

    @classmethod
    def diff(cls, old, new, diff_order=False):

        old_keys = old.keys() if diff_order else sorted(old.keys())
        new_keys = new.keys() if diff_order else sorted(new.keys())
        # делаем так чтобы соблюсти порядок для сравнения сортированных словарей
        keys_changes = seqdiff.seqdiff(old_keys, new_keys, add_same=True)
        operations = []  # todo-igogor тупое название
        for key_change in keys_changes:
            if key_change.type is Types.INSERT:
                key = key_change.to_elem
                operations.append(Change(type=Types.INSERT, to_id=key, to_elem=new[key]))
            elif key_change.type is Types.DELETE:
                key = key_change.elem
                operations.append(Change(type=Types.DELETE, id=key, elem=old[key]))
            # todo-igogor не имеет смысла, т.к. в текущей модели для словаря не указывает откуда куда
            # в seqdiff разбивать Move на move_from - Move(id=, elem=) и move_to - Move(to_id=, to_elem=) ?
            # решить прямо здесь?
            elif key_change.type is Types.MOVE:
                key = key_change.elem
                operations.append(Change(type=key_change.type, id=key, to_id=key,
                                         elem=old[key], to_elem=new[key]))
            # todo-igogor для словарей (да и деревьев) лучше чтобы seqdiff не возвращат replace
            elif key_change.type is Types.REPLACE:
                key = key_change.elem
                operations.append(Change(type=Types.DELETE, id=key, elem=old[key]))
                to_key = key_change.to_elem
                operations.append(Change(type=Types.INSERT, to_id=to_key, to_elem=new[to_key]))
            elif key_change.type is Types.SAME:
                key = key_change.elem
                if old[key] != new[key]:
                    operations.append(Change(type=Types.REPLACE, id=key, to_id=key, elem=old[key], to_elem=new[key]))
                else:
                    operations.append(Change(type=Types.SAME, id=key, to_id=key, elem=old[key], to_elem=new[key]))
            else:
                raise ValueError('Change type is not supported: {}'.format(key_change.type.name))

        return cls(old=old, new=new, operations=operations)

    def filter(self, filters):
        if not filters:
            return
        self.filters.extend(filters)

        after_filtration = []
        for operation in self.operations:
            if operation.type is Types.SAME:
                after_filtration.append(operation)
                continue

            for filter_ in filters:
                filtered, kept = filter_.apply(operation)
                # igogor: пока считаем для простоты, что модифицирующих фильтров нет.
                if filtered:
                    operation = diffutils.Filtered(change=operation, filter_=filter_)
                    break
            after_filtration.append(operation)
        self.operations = after_filtration


@attr.s()
class FilterKeys(BaseFilter):
    keys = attr.ib()
    descr = attr.ib(default='Add descr')

    def apply(self, change):
        if change.id in self.keys or change.to_id in self.keys:
            return self.return_value(filtered=change)
        else:
            return self.return_value(kept=change)

    def report(self):
        return u'ignore(keys={}, descr={})'.format(self.keys, repr(self.descr))


@attr.s()
class FilterValue(BaseFilter):
    value = attr.ib()
    pair = attr.ib(default=None)
    descr = attr.ib(default='Add descr')

    @staticmethod
    def find_pair(value, change):
        # type: (unicode, Change) -> Tuple[unicode, unicode]

        # todo-igogor преобразования в строку могут выстрелить в ногу.
        value = unicode(value)

        corresponding_in_new = utils.corresponding_substring(value, unicode(change.elem), unicode(change.to_elem))
        if corresponding_in_new:
            return value, corresponding_in_new
        else:
            corresponding_in_old = utils.corresponding_substring(value, unicode(change.to_elem), unicode(change.elem))
            if corresponding_in_old:
                return corresponding_in_old, value
            else:
                return None, None

    def apply(self, change):
        if change.elem is None or change.to_elem is None:
            return self.return_value(kept=change)
        pair_candidate = FilterValue.find_pair(self.value, change)
        if pair_candidate == (None, None):
            return self.return_value(kept=change)
        elif self.pair and self.pair != pair_candidate:
            raise ValueError('There are several possible pairs for given value: {}. Contact igogor@')
        else:
            self.pair = pair_candidate

        obfuscate_in_old, obfuscate_in_new = self.pair
        # todo-igogor преобразования в строку могут выстрелить в ногу.
        obfuscated_old = unicode(change.elem).replace(obfuscate_in_old, u'')
        obfuscated_new = unicode(change.to_elem).replace(obfuscate_in_new, u'')

        if obfuscated_old == obfuscated_new:
            return self.return_value(filtered=change)
        else:
            return self.return_value(kept=change)

    def report(self):
        return u'ignore(value={}, descr={})'.format(self.value, repr(self.descr))


def report(diff):
    with reporter.step(u'Строим отчет по {}'.format(u'расхождениям' if diff.changes else u'фильтрам')):
        reporter.attach(u'Ожидаемый словарь', diff.old)
        reporter.attach(u'Текущий словарь', diff.new)
        reporter.attach(u'Полный отчет с {}'.format(u'расхождениями' if diff.changes else u'фильтрами'),
                        _report(diff.operations))
        if diff.changes:
            reporter.attach(u'Расхождения', _report(diff.changes))
            raise utils.ServiceError('There are differences in dicts')


def _report(operations):
    return u'{\n' + u',\n'.join([u"'{}': {}".format(diffutils.key(operation), operation.report())
                                 for operation in operations]) + u'\n}'
