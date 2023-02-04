# coding: utf-8

from collections import defaultdict, OrderedDict
from itertools import izip_longest

import attr
from lxml import etree

from btestlib import seqdiff, dictdiff
from btestlib import utils
from btestlib.diffutils import Types, Change, BaseFilter

# todo-igogor пока заточено под lxml, но кмк легко обобщить, если передавать метод создания элемента в diff

OBFUSCATED = '_pff_OBFUSCATED_pff_'


@attr.s()
class LxmlElementWrapper(object):
    tag = attr.ib(default=None)
    data = attr.ib(default=attr.Factory(dict))

    _element = attr.ib(repr=False, cmp=False, hash=False, default=None)  # type: etree._Element
    _path = attr.ib(repr=False, cmp=False, hash=False, default=None)
    _index = attr.ib(repr=False, cmp=False, hash=False, default=None)
    _children = attr.ib(repr=False, cmp=False, hash=False, default=attr.Factory(list))
    parent = attr.ib(repr=False, cmp=False, hash=False, default=None)

    @classmethod
    def build(cls, element):
        # type: (etree._Element) -> LxmlElementWrapper
        data = utils.remove_empty({k: unicode(v) for k, v in element.attrib.iteritems()})
        data['text_'] = LxmlElementWrapper.normalize_text(element.text, element.tail)
        return cls(tag=element.tag, data=data, element=element)

    @property
    def path(self):
        if not self._path:
            self._path = self._element.getroottree().getpath(self._element)
        return self._path

    @property
    def index(self):
        if not self._index:
            self._index = self.parent._element.index(self._element)
        return self._index

    @property
    def text(self):
        return self.data.get('text_', u'')

    @property
    def children(self):
        if not self._children:
            self._children = [LxmlElementWrapper.build(child) for child in list(self._element)]
            for child in self._children:
                child.parent = self
        return self._children

    def parents(self):
        parents_ = []
        current = self
        while current and current.parent:
            parents_.append(current.parent)
            current = current.parent
        return parents_

    @staticmethod
    def normalize_text(text, tail):
        text = unicode(text or '').strip()
        tail = unicode(tail or '').strip()
        # заменяем все пробелы одним пробелом
        normalized_whitespace = u' '.join((text + u' ' + tail).split())
        return normalized_whitespace.strip()


@attr.s
class TreeDiff(object):
    changes = attr.ib(default=attr.Factory(list))
    _other_parent = attr.ib(default=attr.Factory(dict), repr=False, cmp=False, hash=False)
    filters = attr.ib(default=attr.Factory(list))

    @classmethod
    def diff(cls, old_root, new_root):
        # type: (LxmlElementWrapper, LxmlElementWrapper) -> TreeDiff

        tree_diff = cls()
        pairs_stack = [(old_root, new_root)]
        while pairs_stack:
            old, new = pairs_stack.pop()
            pairs_stack.extend(tree_diff.diff_children(old, new))

        return tree_diff

    def diff_children(self, old_parent, new_parent):
        # type: (List[LxmlElementWrapper], List[LxmlElementWrapper]) -> List[Pair]

        children_pairs = list(izip_longest(old_parent.children, new_parent.children))
        if all([old == new for old, new in children_pairs]):
            return children_pairs
        else:
            children_pairs = []

            children_changes = seqdiff.seqdiff(old_parent.children, new_parent.children, add_same=True)

            for seqchange in children_changes:
                if seqchange.type == Types.SAME:
                    children_pairs.append((old_parent.children[seqchange.id], new_parent.children[seqchange.to_id]))
                elif seqchange.type == seqdiff.Types.MOVE:
                    old, new = seqchange.elem, seqchange.to_elem
                    self.changes.append(attr.evolve(seqchange, id=old.path, to_id=new.path))
                    children_pairs.append((old, new))
                elif seqchange.type == seqdiff.Types.INSERT:
                    self.changes.append(attr.evolve(seqchange, to_id=seqchange.to_elem.path))
                elif seqchange.type == seqdiff.Types.DELETE:
                    self.changes.append(attr.evolve(seqchange, id=seqchange.elem.path))
                elif seqchange.type == seqdiff.Types.REPLACE:
                    if seqchange.elem.tag == seqchange.to_elem.tag:
                        self.changes.append(Change(type=Types.EDIT, elem=seqchange.elem, to_elem=seqchange.to_elem,
                                                   id=seqchange.elem.path, to_id=seqchange.to_elem.path,
                                                   diff=dictdiff.dictdiff(seqchange.elem.data,
                                                                          seqchange.to_elem.data).changes))
                        children_pairs.append((seqchange.elem, seqchange.to_elem))
                    else:
                        self.changes.append(Change(type=Types.DELETE, elem=seqchange.elem, id=seqchange.elem.path))
                        self.changes.append(Change(type=Types.INSERT, to_elem=seqchange.to_elem,
                                                   to_id=seqchange.to_elem.path))

                else:
                    raise ValueError("Change type {} is not supported".format(type(seqchange)))

            # todo-igogor костыль, который нужен для отчета
            self._other_parent[old_parent.path] = new_parent

            return children_pairs

    def filter(self, *filters):
        if not filters:
            return
        self.filters.extend(filters)

        filtered_changes = []
        for change in self.changes:
            filters_to_use = []
            result_filtered = None
            for filter_ in filters:
                filtered, kept = filter_.apply(change)
                if not kept:  # какой-то фильтр фильтрует изменение полностью - применяем только его
                    filters_to_use = [filter_]
                    result_filtered = change
                    break
                # фильтры фильтруют изменения частично - ищем комбинацию фильтров,
                # которая отфильтрует большую часть диффа изменения
                elif filtered and kept and result_filtered != change and \
                                result_filtered != filtered.combine(result_filtered):
                    result_filtered = filtered.combine(result_filtered)
                    filters_to_use.append(filter_)

            for filter_ in filters_to_use:
                filter_.filter(change)
            if result_filtered != change:
                filtered_changes.append(change.substract(result_filtered))

        self.changes = filtered_changes


def filter_(descr=u'Add descr', tags=None, xpath=None, attributes=None, value=None):
    filters_ = []
    if tags:
        filters_.append(Filter.Tags(tags=tags))
    if xpath:
        filters_.append(Filter.Xpath(xpath=xpath))
    if attributes:
        filters_.append(Filter.Attributes(attributes=attributes))
    if value:
        filters_.append(Filter.Value(value=value))
    if len(filters_) > 1:
        return Filter.All(filters_=filters_, descr=descr)
    elif len(filters_) == 1:
        return filters_[0]
    else:
        raise ValueError('You must specify at least on of: tags, xpath, attributes, value')


def all_(descr, *filters):
    return Filter.All(filters_=filters, descr=descr)


def not_(descr, filter):
    return Filter.Not(filter_=filter, descr=descr)


class Filter(object):
    @staticmethod
    def for_change(change):
        if change.type in [Types.DELETE, Types.INSERT, Types.MOVE]:
            element = change.elem or change.to_elem
            return Filter.Xpath(xpath=element.path)
        if change.type is Types.EDIT:
            return Filter.All([Filter.Attributes(attributes=[attrchange.id or attrchange.to_id
                                                             for attrchange in change.diff]),
                               Filter.Xpath(xpath=change.elem.path)])
        else:
            return None

    @attr.s
    class Tags(BaseFilter):
        tags = attr.ib()

        descr = attr.ib(default='Add descr')
        _ignore_types = attr.ib(default=attr.Factory(list), repr=False)

        def apply(self, change):
            # type: (Change) -> Tuple[Change, Change]
            if (change.elem and change.elem.tag) in self.tags or (change.to_elem and change.to_elem.tag in self.tags):
                return self.return_value(filtered=change)
            else:
                return self.return_value(kept=change)

        def smart_repr(self):
            return u"filter_(tags={}, descr=u'{}')".format(self.tags, self.descr)

    @attr.s
    class Xpath(BaseFilter):
        xpath = attr.ib()

        descr = attr.ib(default='Add descr')
        _ignore_types = attr.ib(default=attr.Factory(list), repr=False)

        def apply(self, change):
            # type: (Change) -> Tuple[Change, Change]
            element = change.elem or change.to_elem
            tree = element._element.getroottree()  # type: etree._ElementTree
            if element.path in [LxmlElementWrapper.build(elem).path for elem in tree.xpath(self.xpath)]:
                return self.return_value(filtered=change)
            else:
                return self.return_value(kept=change)

        def smart_repr(self):
            return u"filter_(xpath='{}', descr=u'{}')".format(self.xpath, self.descr)

    @attr.s
    class Attributes(BaseFilter):
        attributes = attr.ib()
        # todo-igogor вообще говоря это поле класса
        _ignore_types = attr.ib(default=attr.Factory(lambda: [Types.INSERT, Types.DELETE, Types.MOVE]), repr=False)

        descr = attr.ib(default='Add descr')

        def apply(self, change):
            # type: (LxmlElementWrapper) -> Tuple[Change, Change]
            if change.type not in self._ignore_types:
                keep_attr_changes = []
                filter_attr_changes = []
                for attr_change in change.diff:
                    if attr_change.id in self.attributes or attr_change.to_id in self.attributes:
                        filter_attr_changes.append(attr_change)
                    else:
                        keep_attr_changes.append(attr_change)

                if not keep_attr_changes:
                    return self.return_value(filtered=change)
                elif filter_attr_changes:
                    filtered = attr.evolve(change, diff=filter_attr_changes)
                    kept = attr.evolve(change, diff=keep_attr_changes)

                    return self.return_value(filtered=filtered, kept=kept)
            return self.return_value(kept=change)

        def smart_repr(self):
            return u"filter_(attributes={}, descr=u'{}')".format(self.attributes, self.descr)

    @attr.s
    class Value(BaseFilter):
        value = attr.ib()
        pair = attr.ib(default=None)
        # todo-igogor вообще говоря это поле класса
        _ignore_types = attr.ib(default=attr.Factory(lambda: [Types.INSERT, Types.DELETE, Types.MOVE]), repr=False)

        descr = attr.ib(default='Add descr')

        @staticmethod
        def find_pair(value, change):
            # type: (unicode, lxml._ElementTree, lxml._ElementTree) -> Tuple[unicode, unicode]
            def find_corresponding_value(value, element, corresponding_element):
                shortest = None

                # todo-igogor здесь на самом деле можно было бы ограничиться только теми аттрибутами которые изменились
                for attr_name, attr_value in element.data.items():
                    corresponding_attr_value = corresponding_element.data.get(attr_name, u'')
                    # todo-igogor это костыль т.к. если в исходной строке будет много спецсимволов будет ошибка
                    try:
                        corresponding_value = utils.corresponding_substring(value, attr_value,
                                                                            corresponding_attr_value)
                    except AssertionError:
                        corresponding_value = u''
                    if corresponding_value and (not shortest or len(corresponding_value) < len(shortest)):
                        shortest = corresponding_value
                return shortest

            corresponding_in_new = find_corresponding_value(value, change.elem, change.to_elem)
            if corresponding_in_new:
                return value, corresponding_in_new
            else:
                corresponding_in_old = find_corresponding_value(value, change.to_elem, change.elem)
                if corresponding_in_old:
                    return corresponding_in_old, value
                else:
                    return None, None

        def apply(self, change):
            # type: (Change) -> Tuple[Change, Change]
            if change.type not in self._ignore_types:
                if not self.pair:
                    pair_candidate = Filter.Value.find_pair(self.value, change)
                    if pair_candidate != (None, None):
                        self.pair = pair_candidate
                    else:
                        return self.return_value(kept=change)

                obfuscate_in_old, obfuscate_in_new = self.pair

                filter_attr_changes = [
                    c for c in change.diff
                    if c.type is Types.REPLACE and
                    c.elem.replace(obfuscate_in_old, u'') == c.to_elem.replace(obfuscate_in_new, u'')]
                keep_attr_changes = [c for c in change.diff if c not in filter_attr_changes]

                filtered = attr.evolve(change, diff=filter_attr_changes)
                kept = attr.evolve(change, diff=keep_attr_changes)
                if not keep_attr_changes:
                    return self.return_value(filtered=change)
                elif filter_attr_changes:
                    return self.return_value(filtered=filtered, kept=kept)
            return self.return_value(kept=change)

        def smart_repr(self):
            return u"filter_(value=u'{}', descr=u'{}')".format(self.value, self.descr)

    @attr.s
    class Not(BaseFilter):
        filter_ = attr.ib()

        descr = attr.ib(default='Add descr')

        def apply(self, change):
            if change.type in self.filter_._ignore_types:
                return self.return_value(kept=change)

            filtered, kept = self.filter_.apply(change)
            return self.return_value(filtered=kept, kept=filtered)

        def smart_repr(self):
            return u"not_({}, descr=u'{}')".format(self.filter_.smart_repr(), self.descr)

    @attr.s()
    class All(BaseFilter):
        filters_ = attr.ib()

        descr = attr.ib(default='Add descr')

        def apply(self, change):
            # todo-igogor как должны работать _ignore_types?

            filter_results = [f.apply(change) for f in self.filters_]
            if all([filtered is not None for filtered, kept in filter_results]):
                result_kept = None
                for filtered, kept in filter_results:
                    if kept is not None:
                        result_kept = result_kept or kept
                        result_kept.combine(kept)
                result_filtered = change.substract(result_kept) if result_kept else change
                return self.return_value(filtered=result_filtered, kept=result_kept)
            else:
                return self.return_value(kept=change)

        # todo-igogor монструозно
        def smart_repr(self):
            subfilter_types_count = defaultdict(int)
            for subfilter in self.filters_:
                subfilter_types_count[type(subfilter).__name__] += 1
            if any([count > 1 for count in subfilter_types_count.values()]) or \
                            Filter.Not.__name__ in subfilter_types_count or Filter.All.__name__ in subfilter_types_count:
                return u"all_(descr=u'{}', {})".format(self.descr, u', '.join([f.smart_repr() for f in self.filters_]))
            else:
                params = OrderedDict()
                for subfilter in self.filters_:
                    if hasattr(subfilter, 'tags'):
                        params['tags'] = getattr(subfilter, 'tags')
                    elif hasattr(subfilter, 'xpath'):
                        params['xpath'] = "'{}'".format(getattr(subfilter, 'xpath'))
                    elif hasattr(subfilter, 'attributes'):
                        params['attributes'] = getattr(subfilter, 'attributes')
                    elif hasattr(subfilter, 'value'):
                        params['value'] = u"u'{}'".format(getattr(subfilter, 'value'))

                return u"filter_({}, descr=u'{}')".format(
                    u', '.join([u'{}={}'.format(key, val) for key, val in params.iteritems()]),
                    self.descr)
