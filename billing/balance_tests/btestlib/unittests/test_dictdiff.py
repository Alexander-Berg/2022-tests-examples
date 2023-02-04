# coding: utf-8
from collections import OrderedDict

import pytest

from btestlib import dictdiff
from btestlib.dictdiff import FilterKeys, FilterValue
from btestlib.diffutils import Change, Types, Filtered


@pytest.mark.parametrize('id_, old, new, result', [
    ('edit', {'a': 1}, {'a': 2},
     [Change(type=Types.REPLACE, id='a', to_id='a', elem=1, to_elem=2)]),
    ('delete', {'a': 1}, {},
     [Change(type=Types.DELETE, id='a', elem=1)]),
    ('insert', {}, {'a': 1},
     [Change(type=Types.INSERT, to_id='a', to_elem=1)]),
    ('replace', {'a': 1}, {'b': 1},
     [Change(type=Types.DELETE, id='a', elem=1), Change(type=Types.INSERT, to_id='b', to_elem=1)]),
    ('same', {'a': 1}, {'a': 1},
     []),
    ('edit_delete', {'a': 1, 'b': 2}, {'a': 3},
     [Change(type=Types.REPLACE, id='a', to_id='a', elem=1, to_elem=3), Change(type=Types.DELETE, id='b', elem=2)]),
    ('edit_insert', {'a': 1}, {'a': 3, 'b': 2},
     [Change(type=Types.REPLACE, id='a', to_id='a', elem=1, to_elem=3),
      Change(type=Types.INSERT, to_id='b', to_elem=2)]),
], ids=lambda id_, old, new, result: id_)
def test_diff_changes(old, new, result, id_):
    diff = dictdiff.dictdiff(old, new)
    print diff.changes
    assert diff.changes == result


@pytest.mark.parametrize('id_, old, new, result', [
    ('same', {'a': 1}, {'a': 1},
     [Change(type=Types.SAME, id='a', to_id='a', elem=1, to_elem=1)]),
    ('edit', {'a': 1}, {'a': 2},
     [Change(type=Types.REPLACE, id='a', to_id='a', elem=1, to_elem=2)]),
    ('delete', {'a': 1}, {},
     [Change(type=Types.DELETE, id='a', elem=1)]),
    ('insert', {}, {'a': 1},
     [Change(type=Types.INSERT, to_id='a', to_elem=1)]),
    ('replace', {'a': 1}, {'b': 1},
     [Change(type=Types.DELETE, id='a', elem=1), Change(type=Types.INSERT, to_id='b', to_elem=1)]),
    ('edit_delete', {'a': 1, 'b': 2}, {'a': 3},
     [Change(type=Types.REPLACE, id='a', to_id='a', elem=1, to_elem=3), Change(type=Types.DELETE, id='b', elem=2)]),
    ('edit_insert', {'a': 1}, {'a': 3, 'b': 2},
     [Change(type=Types.REPLACE, id='a', to_id='a', elem=1, to_elem=3),
      Change(type=Types.INSERT, to_id='b', to_elem=2)]),
    ('edit_same', {'a': 1, 'b': 2}, {'a': 3, 'b': 2},
     [Change(type=Types.REPLACE, id='a', to_id='a', elem=1, to_elem=3),
      Change(type=Types.SAME, id='b', to_id='b', elem=2, to_elem=2)]),
    ('delete_same', {'a': 1, 'b': 2}, {'b': 2},
     [Change(type=Types.DELETE, id='a', elem=1), Change(type=Types.SAME, id='b', to_id='b', elem=2, to_elem=2)]),
    ('insert_same', {'b': 2}, {'a': 1, 'b': 2},
     [Change(type=Types.INSERT, to_id='a', to_elem=1), Change(type=Types.SAME, id='b', to_id='b', elem=2, to_elem=2)]),
], ids=lambda id_, old, new, result: id_)
def test_operations(old, new, result, id_):
    diff = dictdiff.dictdiff(old, new)
    print diff.operations
    assert diff.operations == result


@pytest.mark.parametrize('id_, old, new, result', [
    ('same', [('a', 1)], [('a', 1)],
     []),
    ('edit', [('a', 1)], [('a', 2)],
     [Change(type=Types.REPLACE, id='a', to_id='a', elem=1, to_elem=2)]),
    ('delete', [('a', 1)], [],
     [Change(type=Types.DELETE, id='a', elem=1)]),
    ('insert', [], [('a', 1)],
     [Change(type=Types.INSERT, to_id='a', to_elem=1)]),
    ('replace', [('a', 1)], [('b', 1)],
     [Change(type=Types.DELETE, id='a', elem=1), Change(type=Types.INSERT, to_id='b', to_elem=1)]),
    ('edit_delete', [('a', 1), ('b', 2)], [('a', 3)],
     [Change(type=Types.REPLACE, id='a', to_id='a', elem=1, to_elem=3), Change(type=Types.DELETE, id='b', elem=2)]),
    ('delete_edit', [('a', 1), ('b', 2)], [('b', 3)],
     [Change(type=Types.DELETE, id='a', elem=1), Change(type=Types.REPLACE, id='b', to_id='b', elem=2, to_elem=3)]),
    ('edit_insert', [('a', 1)], [('a', 3), ('b', 2)],
     [Change(type=Types.REPLACE, id='a', to_id='a', elem=1, to_elem=3),
      Change(type=Types.INSERT, to_id='b', to_elem=2)]),
    ('insert_edit', [('b', 1)], [('a', 3), ('b', 2)],
     [Change(type=Types.INSERT, to_id='a', to_elem=3),
      Change(type=Types.REPLACE, id='b', to_id='b', elem=1, to_elem=2)]),
    ('edit_same', [('a', 1), ('b', 2)], [('a', 3), ('b', 2)],
     [Change(type=Types.REPLACE, id='a', to_id='a', elem=1, to_elem=3)]),
    ('same_edit', [('a', 1), ('b', 2)], [('a', 1), ('b', 3)],
     [Change(type=Types.REPLACE, id='b', to_id='b', elem=2, to_elem=3)]),
    ('delete_same', [('a', 1), ('b', 2)], [('b', 2)],
     [Change(type=Types.DELETE, id='a', elem=1)]),
    ('same_delete', [('a', 1), ('b', 2)], [('a', 1)],
     [Change(type=Types.DELETE, id='b', elem=2)]),
    ('insert_same', [('b', 2)], [('a', 1), ('b', 2)],
     [Change(type=Types.INSERT, to_id='a', to_elem=1)]),
    ('same_insert', [('a', 1)], [('a', 1), ('b', 2)],
     [Change(type=Types.INSERT, to_id='b', to_elem=2)]),
], ids=lambda id_, old, new, result: id_)
def test_diff_order(old, new, result, id_):
    diff = dictdiff.dictdiff(OrderedDict(old), OrderedDict(new), diff_order=True)
    print diff.changes
    assert diff.changes == result


@pytest.mark.parametrize('id_, old, new, result', [
    ('same', [('a', 1)], [('a', 1)],
     [Change(type=Types.SAME, id='a', to_id='a', elem=1, to_elem=1)]),
    ('edit', [('a', 1)], [('a', 2)],
     [Change(type=Types.REPLACE, id='a', to_id='a', elem=1, to_elem=2)]),
    ('delete', [('a', 1)], {},
     [Change(type=Types.DELETE, id='a', elem=1)]),
    ('insert', {}, [('a', 1)],
     [Change(type=Types.INSERT, to_id='a', to_elem=1)]),
    ('replace', [('a', 1)], [('b', 1)],
     [Change(type=Types.DELETE, id='a', elem=1), Change(type=Types.INSERT, to_id='b', to_elem=1)]),
    ('edit_delete', [('a', 1), ('b', 2)], [('a', 3)],
     [Change(type=Types.REPLACE, id='a', to_id='a', elem=1, to_elem=3), Change(type=Types.DELETE, id='b', elem=2)]),
    ('delete_edit', [('a', 1), ('b', 2)], [('b', 3)],
     [Change(type=Types.DELETE, id='a', elem=1), Change(type=Types.REPLACE, id='b', to_id='b', elem=2, to_elem=3)]),
    ('edit_insert', [('a', 1)], [('a', 3), ('b', 2)],
     [Change(type=Types.REPLACE, id='a', to_id='a', elem=1, to_elem=3),
      Change(type=Types.INSERT, to_id='b', to_elem=2)]),
    ('insert_edit', [('b', 1)], [('a', 3), ('b', 2)],
     [Change(type=Types.INSERT, to_id='a', to_elem=3),
      Change(type=Types.REPLACE, id='b', to_id='b', elem=1, to_elem=2)]),
    ('edit_same', [('a', 1), ('b', 2)], [('a', 3), ('b', 2)],
     [Change(type=Types.REPLACE, id='a', to_id='a', elem=1, to_elem=3),
      Change(type=Types.SAME, id='b', to_id='b', elem=2, to_elem=2)]),
    ('same_edit', [('a', 1), ('b', 2)], [('a', 1), ('b', 3)],
     [Change(type=Types.SAME, id='a', to_id='a', elem=1, to_elem=1),
      Change(type=Types.REPLACE, id='b', to_id='b', elem=2, to_elem=3)]),
    ('delete_same', [('a', 1), ('b', 2)], [('b', 2)],
     [Change(type=Types.DELETE, id='a', elem=1), Change(type=Types.SAME, id='b', to_id='b', elem=2, to_elem=2)]),
    ('same_delete', [('a', 1), ('b', 2)], [('a', 1)],
     [Change(type=Types.SAME, id='a', to_id='a', elem=1, to_elem=1), Change(type=Types.DELETE, id='b', elem=2)]),
    ('insert_same', [('b', 2)], [('a', 1), ('b', 2)],
     [Change(type=Types.INSERT, to_id='a', to_elem=1), Change(type=Types.SAME, id='b', to_id='b', elem=2, to_elem=2)]),
    ('same_insert', [('a', 1)], [('a', 1), ('b', 2)],
     [Change(type=Types.SAME, id='a', to_id='a', elem=1, to_elem=1), Change(type=Types.INSERT, to_id='b', to_elem=2)]),
], ids=lambda id_, old, new, result: id_)
def test_operations_diff_order(old, new, result, id_):
    diff = dictdiff.dictdiff(OrderedDict(old), OrderedDict(new), diff_order=True)
    print diff.operations
    assert diff.operations == result


@pytest.mark.parametrize('id_, old, new, result', [
    ('one_move', OrderedDict([('a', 1), ('b', 2)]), OrderedDict([('b', 2), ('a', 1)]),
     [Change(type=Types.SAME, id='b', to_id='b', elem=2, to_elem=2),
      Change(type=Types.MOVE, id='a', to_id='a', elem=1, to_elem=1)]),
    ('two_moves', OrderedDict([('a', 1), ('b', 2), ('c', 3)]), OrderedDict([('c', 3), ('b', 2), ('a', 1)]),
     [Change(type=Types.MOVE, id='c', to_id='c', elem=3, to_elem=3),
      Change(type=Types.SAME, id='b', to_id='b', elem=2, to_elem=2),
      Change(type=Types.MOVE, id='a', to_id='a', elem=1, to_elem=1)])
], ids=lambda id_, old, new, result: id_)
def test_diff_move(old, new, result, id_):
    diff = dictdiff.dictdiff(old, new, diff_order=True)
    print diff.operations
    assert diff.operations == result


@pytest.mark.parametrize('id_, old, new, ignore, changes, filtered, operations', [
    ('filter_replace_keep_none', {'a': 1}, {'a': 2}, lambda: dictdiff.ignore(keys=['a']),
     [],
     [Change(type=Types.REPLACE, id='a', to_id='a', elem=1, to_elem=2)],
     [Filtered(change=Change(type=Types.REPLACE, id='a', to_id='a', elem=1, to_elem=2),
               filter_=FilterKeys(keys=['a'], descr='Add descr'))]),
    ('filter_delete_keep_insert', {'a': 1}, {'b': 2}, lambda: dictdiff.ignore(keys=['a']),
     [Change(type=Types.INSERT, to_id='b', to_elem=2)],
     [Change(type=Types.DELETE, id='a', elem=1)],
     [Filtered(change=Change(type=Types.DELETE, id='a', elem=1), filter_=FilterKeys(keys=['a'], descr='Add descr')),
      Change(type=Types.INSERT, to_id='b', to_elem=2)]),
    ('filter_insert_keep_replace', {'b': 1}, {'a': 2, 'b': 3}, lambda: dictdiff.ignore(keys=['a']),
     [Change(type=Types.REPLACE, id='b', to_id='b', elem=1, to_elem=3)],
     [Change(type=Types.INSERT, to_id='a', to_elem=2)],
     [Filtered(change=Change(type=Types.INSERT, to_id='a', to_elem=2),
               filter_=FilterKeys(keys=['a'], descr='Add descr')),
      Change(type=Types.REPLACE, id='b', to_id='b', elem=1, to_elem=3)]),
], ids=lambda id_, old, new, ignore, changes, filtered, operations: id_)
def test_keys_filter(old, new, ignore, changes, filtered, operations, id_):
    diff = dictdiff.dictdiff(old, new, filters=ignore())
    print diff.changes
    assert diff.changes == changes
    print diff.filtered
    assert diff.filtered == filtered
    print diff.operations
    assert diff.operations == operations


@pytest.mark.parametrize('id_, old, new, value, changes, filtered, operations', [
    ('filter_replace_keep_none', {'a': u'12'}, {'a': 34}, 12,
     [],
     [Change(type=Types.REPLACE, id='a', to_id='a', elem=u'12', to_elem=34)],
     [Filtered(change=Change(type=Types.REPLACE, id='a', to_id='a', elem=u'12', to_elem=34),
               filter_=FilterValue(value=12, pair=(u'12', u'34'), descr='Add descr'))]),
    ('keep_insert', {}, {'a': u'34'}, '34',
     [Change(type=Types.INSERT, to_id='a', to_elem=u'34')],
     [],
     [Change(type=Types.INSERT, to_id='a', to_elem=u'34')]),
    ('keep_delete', {'a': 12}, {}, u'12',
     [Change(type=Types.DELETE, id='a', elem=12)],
     [],
     [Change(type=Types.DELETE, id='a', elem=12)]),
    ('filter_replace_keep_insert', {'a': 12}, {'a': u'34', 'b': u'34'}, 34,
     [Change(type=Types.INSERT, to_id='b', to_elem=u'34')],
     [Change(type=Types.REPLACE, id='a', to_id='a', elem=12, to_elem=u'34')],
     [Filtered(change=Change(type=Types.REPLACE, id='a', to_id='a', elem=12, to_elem=u'34'),
               filter_=FilterValue(value=34, pair=(u'12', u'34'), descr='Add descr')),
      Change(type=Types.INSERT, to_id='b', to_elem=u'34')]),
    ('filter_replace_keep_delete', {'a': 12, 'b': 12}, {'a': 34}, 12,
     [Change(type=Types.DELETE, id='b', elem=12)],
     [Change(type=Types.REPLACE, id='a', to_id='a', elem=12, to_elem=34)],
     [Filtered(change=Change(type=Types.REPLACE, id='a', to_id='a', elem=12, to_elem=34),
               filter_=FilterValue(value=12, pair=(u'12', u'34'), descr='Add descr')),
      Change(type=Types.DELETE, id='b', elem=12)]),
], ids=lambda id_, old, new, value, changes, filtered, operations: id_)
def test_value_filter(old, new, value, changes, filtered, operations, id_):
    diff = dictdiff.dictdiff(old, new, filters=dictdiff.ignore(value=value))
    print diff.changes
    assert diff.changes == changes
    print diff.filtered
    assert diff.filtered == filtered
    print diff.operations
    assert diff.operations == operations


@pytest.mark.parametrize('id_, old, new, ignore, report', [
    ('without_changes', {'a': 1, 'b': 2}, {'a': 1, 'b': 2}, lambda: None,
     '''{
'a': 1,
'b': 2
}'''),
    ('with_changes', {'a': 1, 'c': 3}, {'b': 2, 'c': 4}, lambda: None,
     '''{
'a': Delete(1),
'b': Insert(2),
'c': Replace(3, to=4)
}'''),
    ('with_filtered', {'a': 1, 'c': 3, 'd': 5, 'e': 7}, {'b': 2, 'c': 4, 'd': 6, 'e': 7},
     lambda: dictdiff.ignore(keys=['a', 'b'], value='4', descr=u'Описание'),
     '''{
'a': Filtered(Delete(1), ignore(keys=['a', 'b'], descr=u'\u041e\u043f\u0438\u0441\u0430\u043d\u0438\u0435')),
'b': Filtered(Insert(2), ignore(keys=['a', 'b'], descr=u'\u041e\u043f\u0438\u0441\u0430\u043d\u0438\u0435')),
'c': Filtered(Replace(3, to=4), ignore(value=4, descr=u'\u041e\u043f\u0438\u0441\u0430\u043d\u0438\u0435')),
'd': Replace(5, to=6),
'e': 7
}'''),
], ids=lambda id_, old, new, ignore, report: id_)
def test_report(old, new, ignore, report, id_):
    diff = dictdiff.dictdiff(old, new, filters=ignore())
    report_dict = dictdiff._report(diff.operations)
    print report_dict
    assert report_dict == report
