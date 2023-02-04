# coding: utf-8
import pytest
from lxml import etree

from btestlib import utils
from btestlib.treediff import LxmlElementWrapper, TreeDiff, Filter, Types, Change


def html(body):
    return etree.HTML("<!DOCTYPE html><html><body>{}</body></html>".format(body))


def xml(body):
    # igogor: оставляем тэги html и body чтобы пути были одинаковые.
    return etree.XML('<?xml version="1.0" encoding="utf-8"?><!DOCTYPE html><html><body>{}</body></html>'.format(body))


@pytest.mark.parametrize('id, old, new, changes', [
    ('INSERT', '', '<p>1</p>',
     [Change(type=Types.INSERT, to_id='/html/body/p', to_elem=LxmlElementWrapper(tag='p', data={'text_': u'1'}))]),
    ('DELETE', '<p>1</p>', '',
     [Change(type=Types.DELETE, id='/html/body/p', elem=LxmlElementWrapper(tag='p', data={'text_': u'1'}))]),
    ('EDIT_one_attr', '<p>1</p>', '<p>2</p>',
     [Change(type=Types.EDIT, id='/html/body/p', to_id='/html/body/p',
             diff=[Change(type=Types.REPLACE, id='text_', to_id='text_', elem=u'1', to_elem=u'2')],
             elem=LxmlElementWrapper(tag='p', data={'text_': u'1'}),
             to_elem=LxmlElementWrapper(tag='p', data={'text_': u'2'}))]),
    ('EDIT_two_attrs', '<p id="1">1</p>', '<p id="2">2</p>',
     [Change(type=Types.EDIT, id='/html/body/p', to_id='/html/body/p',
             diff=[Change(type=Types.REPLACE, id='id', to_id='id', elem=u'1', to_elem=u'2'),
                   Change(type=Types.REPLACE, id='text_', to_id='text_', elem=u'1', to_elem=u'2')],
             elem=LxmlElementWrapper(tag='p', data={'id': u'1', 'text_': u'1'}),
             to_elem=LxmlElementWrapper(tag='p', data={'id': u'2', 'text_': u'2'}))]
     ),
    ('EDIT_one_of_two_attrs', '<p>1</p><p>2</p>', '<p>2</p><p>1</p>',
     [Change(type=Types.MOVE, id='/html/body/p[1]', to_id='/html/body/p[2]',
             elem=LxmlElementWrapper(tag='p', data={'text_': u'1'}),
             to_elem=LxmlElementWrapper(tag='p', data={'text_': u'1'}))]),
    ('REPLACE', '<p>1</p>', '<b>1</b>',
     [Change(type=Types.DELETE, id='/html/body/p', elem=LxmlElementWrapper(tag='p', data={'text_': u'1'})),
      Change(type=Types.INSERT, to_id='/html/body/b', to_elem=LxmlElementWrapper(tag='b', data={'text_': u'1'}))]),
    ('SAME', '<p>1</p>', '<p>1</p>', []),
    ('INSERT_DELETE_EDIT_MOVE', '<p>1</p><p>2</p><p id="5">3</p><p>4</p>', '<p id="5">3</p><p>1</p><h>2a</h><p>4a</p>',
     [Change(type=Types.MOVE, id='/html/body/p[3]', to_id='/html/body/p[1]',
             elem=LxmlElementWrapper(tag='p', data={'id': u'5', 'text_': u'3'}),
             to_elem=LxmlElementWrapper(tag='p', data={'id': u'5', 'text_': u'3'})),
      Change(type=Types.DELETE, id='/html/body/p[2]', elem=LxmlElementWrapper(tag='p', data={'text_': u'2'})),
      Change(type=Types.INSERT, to_id='/html/body/h', to_elem=LxmlElementWrapper(tag='h', data={'text_': u'2a'})),
      Change(type=Types.EDIT, id='/html/body/p[4]', to_id='/html/body/p[3]',
             diff=[Change(type=Types.REPLACE, id='text_', to_id='text_', elem=u'4', to_elem=u'4a')],
             elem=LxmlElementWrapper(tag='p', data={'text_': u'4'}),
             to_elem=LxmlElementWrapper(tag='p', data={'text_': u'4a'}))]),
], ids=lambda id, old, new, changes: id)
@pytest.mark.parametrize('parse', [html, xml])
def test_diff(id, old, new, changes, parse):
    old_document = parse(old)
    new_document = parse(new)

    diff = TreeDiff.diff(LxmlElementWrapper.build(old_document), LxmlElementWrapper.build(new_document))
    print diff.changes
    assert diff.changes == changes


@pytest.mark.xfail
@pytest.mark.parametrize('parse', [html, xml])
def test_invisible_move(parse):
    old_document = parse('<div><p>1</p></div>'
                         '<div><p>2</p></div>')
    new_document = parse('<div><p>2</p></div>'
                         '<div><p>1</p></div>')
    diff = TreeDiff.diff(LxmlElementWrapper.build(old_document), LxmlElementWrapper.build(new_document))
    print diff.changes
    assert diff.changes == [Change(type=Types.MOVE, id='/html/body/div[1]', to_id='/html/body/div[2]',
                                   elem=LxmlElementWrapper(tag='div'), to_elem=LxmlElementWrapper(tag='div'))]


@pytest.mark.parametrize('id, old, new, xpath, changes, filtered', [
    ('INSERT', '', '<p id="1">1</p>', "//p[@id='1']",
     [],
     [Change(type=Types.INSERT, to_id='/html/body/p',
             to_elem=LxmlElementWrapper(tag='p', data={'id': u'1', 'text_': u'1'}))]),
    ('DELETE', '<p id="1">1</p>', '', "//p[@id='1']",
     [],
     [Change(type=Types.DELETE, id='/html/body/p',
             elem=LxmlElementWrapper(tag='p', data={'id': u'1', 'text_': u'1'}))]),
    ('EDIT', '<p id="1">1</p>', '<p id="1">2</p>', "//p[@id='1']",
     [],
     [Change(type=Types.EDIT, id='/html/body/p', to_id='/html/body/p',
             diff=[Change(type=Types.REPLACE, id='text_', to_id='text_', elem=u'1', to_elem=u'2')],
             elem=LxmlElementWrapper(tag='p', data={'id': u'1', 'text_': u'1'}),
             to_elem=LxmlElementWrapper(tag='p', data={'id': u'1', 'text_': u'2'}))]),
    ('MOVE', '<p id="1">1</p><b>2</b>', '<b>2</b><p id="1">1</p>', "//p[@id='1']",
     [],
     [Change(type=Types.MOVE, id='/html/body/p', to_id='/html/body/p',
             elem=LxmlElementWrapper(tag='p', data={'id': u'1', 'text_': u'1'}),
             to_elem=LxmlElementWrapper(tag='p', data={'id': u'1', 'text_': u'1'}))]),
    ('INSERT_DELETE_EDIT_MOVE', '<p>1</p><p>2</p><p id="5">3</p><p>4</p>', '<p id="5">3</p><p>1</p><h>2a</h><p>4a</p>',
     "//p[@id='5']",
     [Change(type=Types.DELETE, id='/html/body/p[2]', elem=LxmlElementWrapper(tag='p', data={'text_': u'2'})),
      Change(type=Types.INSERT, to_id='/html/body/h', to_elem=LxmlElementWrapper(tag='h', data={'text_': u'2a'})),
      Change(type=Types.EDIT, id='/html/body/p[4]', to_id='/html/body/p[3]',
             diff=[Change(type=Types.REPLACE, id='text_', to_id='text_', elem=u'4', to_elem=u'4a')],
             elem=LxmlElementWrapper(tag='p', data={'text_': u'4'}),
             to_elem=LxmlElementWrapper(tag='p', data={'text_': u'4a'}))],
     [Change(type=Types.MOVE, id='/html/body/p[3]', to_id='/html/body/p[1]',
             elem=LxmlElementWrapper(tag='p', data={'id': u'5', 'text_': u'3'}),
             to_elem=LxmlElementWrapper(tag='p', data={'id': u'5', 'text_': u'3'}))]),
], ids=lambda id, old, new, xpath, changes, filtered: id)
@pytest.mark.parametrize('parse', [html, xml])
def test_xpath_filter(id, old, new, xpath, changes, filtered, parse):
    old_document = parse(old)
    new_document = parse(new)

    diff = TreeDiff.diff(LxmlElementWrapper.build(old_document), LxmlElementWrapper.build(new_document))
    diff.filter(Filter.Xpath(xpath=xpath, descr='Test'))
    print diff.changes
    assert diff.changes == changes
    actual_filtered = utils.flatten([f.filtered for f in diff.filters])
    print actual_filtered
    assert actual_filtered == filtered


@pytest.mark.parametrize('id, old, new, tags, changes, filtered', [
    ('filter_INSERT', '', '<p>1</p>', ["p"],
     [],
     [Change(type=Types.INSERT, to_id='/html/body/p', to_elem=LxmlElementWrapper(tag='p', data={'text_': u'1'}))]),
    ('filter_DELETE', '<p>1</p>', '', ["p"],
     [],
     [Change(type=Types.DELETE, id='/html/body/p', elem=LxmlElementWrapper(tag='p', data={'text_': u'1'}))]),
    ('filter_EDIT', '<p>1</p>', '<p>2</p>', ["p"],
     [],
     [Change(type=Types.EDIT, id='/html/body/p', to_id='/html/body/p',
             diff=[Change(type=Types.REPLACE, id='text_', to_id='text_', elem=u'1', to_elem=u'2')],
             elem=LxmlElementWrapper(tag='p', data={'text_': u'1'}),
             to_elem=LxmlElementWrapper(tag='p', data={'text_': u'2'}))]),
    ('filter_several_tags_EDIT', '<p>1</p><b>3</b>', '<p>2</p><b>4</b>', ["p", "b"],
     [],
     [Change(type=Types.EDIT, id='/html/body/p', to_id='/html/body/p',
             diff=[Change(type=Types.REPLACE, id='text_', to_id='text_', elem=u'1', to_elem=u'2')],
             elem=LxmlElementWrapper(tag='p', data={'text_': u'1'}),
             to_elem=LxmlElementWrapper(tag='p', data={'text_': u'2'})),
      Change(type=Types.EDIT, id='/html/body/b', to_id='/html/body/b',
             diff=[Change(type=Types.REPLACE, id='text_', to_id='text_', elem=u'3', to_elem=u'4')],
             elem=LxmlElementWrapper(tag='b', data={'text_': u'3'}),
             to_elem=LxmlElementWrapper(tag='b', data={'text_': u'4'}))]),
    ('filter_EDIT_keep_another', '<p>1</p><b>3</b>', '<p>2</p><b>4</b>', ["p"],
     [Change(type=Types.EDIT, id='/html/body/b', to_id='/html/body/b',
             diff=[Change(type=Types.REPLACE, id='text_', to_id='text_', elem=u'3', to_elem=u'4')],
             elem=LxmlElementWrapper(tag='b', data={'text_': u'3'}),
             to_elem=LxmlElementWrapper(tag='b', data={'text_': u'4'}))],
     [Change(type=Types.EDIT, id='/html/body/p', to_id='/html/body/p',
             diff=[Change(type=Types.REPLACE, id='text_', to_id='text_', elem=u'1', to_elem=u'2')],
             elem=LxmlElementWrapper(tag='p', data={'text_': u'1'}),
             to_elem=LxmlElementWrapper(tag='p', data={'text_': u'2'}))]),
    ('filter_MOVE', '<p>1</p><p>2</p>', '<p>2</p><p>1</p>', ["p"],
     [],
     [Change(type=Types.MOVE, id='/html/body/p[1]', to_id='/html/body/p[2]',
             elem=LxmlElementWrapper(tag='p', data={'text_': u'1'}),
             to_elem=LxmlElementWrapper(tag='p', data={'text_': u'1'}))]),
    ('INSERT_DELETE_EDIT_MOVE', '<p>1</p><p>2</p><p id="5">3</p><p>4</p>', '<p id="5">3</p><p>1</p><h>2a</h><p>4a</p>',
     ["p"],
     [Change(type=Types.INSERT, to_id='/html/body/h', to_elem=LxmlElementWrapper(tag='h', data={'text_': u'2a'}))],
     [Change(type=Types.MOVE, id='/html/body/p[3]', to_id='/html/body/p[1]',
             elem=LxmlElementWrapper(tag='p', data={'id': u'5', 'text_': u'3'}),
             to_elem=LxmlElementWrapper(tag='p', data={'id': u'5', 'text_': u'3'})),
      Change(type=Types.DELETE, id='/html/body/p[2]', elem=LxmlElementWrapper(tag='p', data={'text_': u'2'})),
      Change(type=Types.EDIT, id='/html/body/p[4]', to_id='/html/body/p[3]',
             diff=[Change(type=Types.REPLACE, id='text_', to_id='text_', elem=u'4', to_elem=u'4a')],
             elem=LxmlElementWrapper(tag='p', data={'text_': u'4'}),
             to_elem=LxmlElementWrapper(tag='p', data={'text_': u'4a'}))]),
], ids=lambda id, old, new, tags, changes, filtered: id)
@pytest.mark.parametrize('parse', [html, xml])
def test_tag_filter(old, new, tags, changes, filtered, parse, id):
    old_document = parse(old)
    new_document = parse(new)

    diff = TreeDiff.diff(LxmlElementWrapper.build(old_document), LxmlElementWrapper.build(new_document))
    diff.filter(Filter.Tags(tags=tags, descr='Test'))
    print diff.changes
    assert diff.changes == changes
    actual_filtered = utils.flatten([f.filtered for f in diff.filters])
    print actual_filtered
    assert actual_filtered == filtered


@pytest.mark.parametrize('id, old, new, attributes, changes, filtered', [
    ('keep_INSERT', '', '<p id="1">1</p>', ['id'],
     [Change(type=Types.INSERT, to_id='/html/body/p',
             to_elem=LxmlElementWrapper(tag='p', data={'id': u'1', 'text_': u'1'}))],
     []),
    ('keep_DELETE', '<p id="1">1</p>', '', ['id'],
     [Change(type=Types.DELETE, id='/html/body/p', elem=LxmlElementWrapper(tag='p', data={'id': u'1', 'text_': u'1'}))],
     []),
    ('keep_MOVE', '<p id="1">1</p><b>2</b>', '<b>2</b><p id="1">1</p>', ['id'],
     [Change(type=Types.MOVE, id='/html/body/p', to_id='/html/body/p',
             elem=LxmlElementWrapper(tag='p', data={'id': u'1', 'text_': u'1'}),
             to_elem=LxmlElementWrapper(tag='p', data={'id': u'1', 'text_': u'1'}))],
     []),
    ('keep_REPLACE', '<p id="1">1</p>', '<b>1</b>', ['id'],
     [Change(type=Types.DELETE, id='/html/body/p', elem=LxmlElementWrapper(tag='p', data={'id': u'1', 'text_': u'1'})),
      Change(type=Types.INSERT, to_id='/html/body/b', to_elem=LxmlElementWrapper(tag='b', data={'text_': u'1'}))],
     []),
    ('EDIT_keep_one_of_two_attrs', '<p id="1">1</p>', '<p id="1">2</p>', ['id'],
     [Change(type=Types.EDIT, id='/html/body/p', to_id='/html/body/p',
             diff=[Change(type=Types.REPLACE, id='text_', to_id='text_', elem=u'1', to_elem=u'2')],
             elem=LxmlElementWrapper(tag='p', data={'id': u'1', 'text_': u'1'}),
             to_elem=LxmlElementWrapper(tag='p', data={'id': u'1', 'text_': u'2'}))],
     []),
    ('EDIT_filter_one_of_two_attrs', '<p id="1">1</p>', '<p id="2">1</p>', ['id'],
     [],
     [Change(type=Types.EDIT, id='/html/body/p', to_id='/html/body/p',
             diff=[Change(type=Types.REPLACE, id='id', to_id='id', elem=u'1', to_elem=u'2')],
             elem=LxmlElementWrapper(tag='p', data={'id': u'1', 'text_': u'1'}),
             to_elem=LxmlElementWrapper(tag='p', data={'id': u'2', 'text_': u'1'}))]),
    ('EDIT_keep_one_attr_filter_another', '<p id="1">1</p>', '<p id="2">2</p>', ['id'],
     [Change(type=Types.EDIT, id='/html/body/p', to_id='/html/body/p',
             diff=[Change(type=Types.REPLACE, id='text_', to_id='text_', elem=u'1', to_elem=u'2')],
             elem=LxmlElementWrapper(tag='p', data={'id': u'1', 'text_': u'1'}),
             to_elem=LxmlElementWrapper(tag='p', data={'id': u'2', 'text_': u'2'}))],
     [Change(type=Types.EDIT, id='/html/body/p', to_id='/html/body/p',
             diff=[Change(type=Types.REPLACE, id='id', to_id='id', elem=u'1', to_elem=u'2')],
             elem=LxmlElementWrapper(tag='p', data={'id': u'1', 'text_': u'1'}),
             to_elem=LxmlElementWrapper(tag='p', data={'id': u'2', 'text_': u'2'}))]),
    ('INSERT_DELETE_EDIT_MOVE', '<p>1</p><p>2</p><p id="5">3</p><p>4</p>', '<p id="5">3</p><p>1</p><h>2a</h><p>4a</p>',
     ["text_"],
     [Change(type=Types.MOVE, id='/html/body/p[3]', to_id='/html/body/p[1]',
             elem=LxmlElementWrapper(tag='p', data={'id': u'5', 'text_': u'3'}),
             to_elem=LxmlElementWrapper(tag='p', data={'id': u'5', 'text_': u'3'})),
      Change(type=Types.DELETE, id='/html/body/p[2]', elem=LxmlElementWrapper(tag='p', data={'text_': u'2'})),
      Change(type=Types.INSERT, to_id='/html/body/h', to_elem=LxmlElementWrapper(tag='h', data={'text_': u'2a'}))],
     [Change(type=Types.EDIT, id='/html/body/p[4]', to_id='/html/body/p[3]',
             diff=[Change(type=Types.REPLACE, id='text_', to_id='text_', elem=u'4', to_elem=u'4a')],
             elem=LxmlElementWrapper(tag='p', data={'text_': u'4'}),
             to_elem=LxmlElementWrapper(tag='p', data={'text_': u'4a'}))]),
], ids=lambda id, old, new, attributes, changes, filtered: id)
@pytest.mark.parametrize('parse', [html, xml])
def test_attributes_filter(old, new, attributes, changes, filtered, parse, id):
    old_document = parse(old)
    new_document = parse(new)

    diff = TreeDiff.diff(LxmlElementWrapper.build(old_document), LxmlElementWrapper.build(new_document))
    diff.filter(Filter.Attributes(attributes=attributes))
    print diff.changes
    assert diff.changes == changes
    actual_filtered = utils.flatten([f.filtered for f in diff.filters])
    print actual_filtered
    assert actual_filtered == filtered


@pytest.mark.parametrize('value_descr, value', [('value_in_old', '12'), ('value_in_new', '34')],
                         ids=lambda value_descr, value: value_descr)
@pytest.mark.parametrize('descr, old, new, changes, filtered', [
    ('EDIT_filter_text_completely', '<p>12</p>', '<p>34</p>',
     [],
     [Change(type=Types.EDIT, id='/html/body/p', to_id='/html/body/p',
             diff=[Change(type=Types.REPLACE, id='text_', to_id='text_', elem=u'12',
                          to_elem=u'34')],
             elem=LxmlElementWrapper(tag='p', data={'text_': u'12'}),
             to_elem=LxmlElementWrapper(tag='p', data={'text_': u'34'}))]),
    ('EDIT_filter_text_in_the_end', '<p>before 12</p>', '<p>before 34</p>',
     [],
     [Change(type=Types.EDIT, id='/html/body/p', to_id='/html/body/p',
             diff=[Change(type=Types.REPLACE, id='text_', to_id='text_', elem=u'before 12',
                          to_elem=u'before 34')],
             elem=LxmlElementWrapper(tag='p', data={'text_': u'before 12'}),
             to_elem=LxmlElementWrapper(tag='p', data={'text_': u'before 34'}))]),
    ('EDIT_filter_text_in_the_beginning', '<p>12 after</p>', '<p>34 after</p>',
     [],
     [Change(type=Types.EDIT, id='/html/body/p', to_id='/html/body/p',
             diff=[Change(type=Types.REPLACE, id='text_', to_id='text_', elem=u'12 after',
                          to_elem=u'34 after')],
             elem=LxmlElementWrapper(tag='p', data={'text_': u'12 after'}),
             to_elem=LxmlElementWrapper(tag='p', data={'text_': u'34 after'}))]),
    ('EDIT_filter_text_in_the_middle', '<p>before 12 after</p>', '<p>before 34 after</p>',
     [],
     [Change(type=Types.EDIT, id='/html/body/p', to_id='/html/body/p',
             diff=[Change(type=Types.REPLACE, id='text_', to_id='text_', elem=u'before 12 after',
                          to_elem=u'before 34 after')],
             elem=LxmlElementWrapper(tag='p', data={'text_': u'before 12 after'}),
             to_elem=LxmlElementWrapper(tag='p', data={'text_': u'before 34 after'}))]),
    ('EDIT_filter_text_several_times', '<p>before 12 middle 12 after</p>', '<p>before 34 middle 34 after</p>',
     [],
     [Change(type=Types.EDIT, id='/html/body/p', to_id='/html/body/p',
             diff=[Change(type=Types.REPLACE, id='text_', to_id='text_',
                          elem=u'before 12 middle 12 after',
                          to_elem=u'before 34 middle 34 after')],
             elem=LxmlElementWrapper(tag='p', data={'text_': u'before 12 middle 12 after'}),
             to_elem=LxmlElementWrapper(tag='p', data={'text_': u'before 34 middle 34 after'}))]),
    ('EDIT_filter_attribute', '<p id="12">one</p>', '<p id="34">one</p>',
     [],
     [Change(type=Types.EDIT, id='/html/body/p', to_id='/html/body/p',
             diff=[Change(type=Types.REPLACE, id='id', to_id='id', elem=u'12',
                          to_elem=u'34')],
             elem=LxmlElementWrapper(tag='p', data={'id': u'12', 'text_': u'one'}),
             to_elem=LxmlElementWrapper(tag='p', data={'id': u'34', 'text_': u'one'}))]
     ),
    ('EDIT_filter_one_attribute_keep_another', '<p id="12">one 12 thing</p>', '<p id="34">different 34 something</p>',
     [Change(type=Types.EDIT, id='/html/body/p', to_id='/html/body/p',
             diff=[Change(type=Types.REPLACE, id='text_', to_id='text_',
                          elem=u'one 12 thing',
                          to_elem=u'different 34 something')],
             elem=LxmlElementWrapper(tag='p', data={'id': u'12', 'text_': u'one 12 thing'}),
             to_elem=LxmlElementWrapper(tag='p', data={'id': u'34', 'text_': u'different 34 something'}))],
     [Change(type=Types.EDIT, id='/html/body/p', to_id='/html/body/p',
             diff=[Change(type=Types.REPLACE, id='id', to_id='id',
                          elem=u'12',
                          to_elem=u'34')],
             elem=LxmlElementWrapper(tag='p', data={'id': u'12', 'text_': u'one 12 thing'}),
             to_elem=LxmlElementWrapper(tag='p', data={'id': u'34', 'text_': u'different 34 something'}))]),
    ('INSERT_DELETE_EDIT_MOVE', '<p>1</p><p>2</p><p id="5">3</p><p>12</p>', '<p id="5">3</p><p>1</p><h>34</h><p>34</p>',
     [Change(type=Types.MOVE, id='/html/body/p[3]', to_id='/html/body/p[1]',
             elem=LxmlElementWrapper(tag='p', data={'id': u'5', 'text_': u'3'}),
             to_elem=LxmlElementWrapper(tag='p', data={'id': u'5', 'text_': u'3'})),
      Change(type=Types.DELETE, id='/html/body/p[2]', elem=LxmlElementWrapper(tag='p', data={'text_': u'2'})),
      Change(type=Types.INSERT, to_id='/html/body/h', to_elem=LxmlElementWrapper(tag='h', data={'text_': u'34'}))],
     [Change(type=Types.EDIT, id='/html/body/p[4]', to_id='/html/body/p[3]',
             diff=[Change(type=Types.REPLACE, id='text_', to_id='text_', elem=u'12', to_elem=u'34')],
             elem=LxmlElementWrapper(tag='p', data={'text_': u'12'}),
             to_elem=LxmlElementWrapper(tag='p', data={'text_': u'34'}))]),
], ids=lambda descr, old, new, changes, filtered: descr)
@pytest.mark.parametrize('parse', [html, xml])
def test_value_filter(old, new, value, changes, filtered, parse, descr, value_descr):
    old_document = parse(old)
    new_document = parse(new)

    diff = TreeDiff.diff(LxmlElementWrapper.build(old_document), LxmlElementWrapper.build(new_document))
    diff.filter(Filter.Value(value=value))
    print diff.changes
    assert diff.changes == changes
    actual_filtered = utils.flatten([f.filtered for f in diff.filters])
    print actual_filtered
    assert actual_filtered == filtered


@pytest.mark.parametrize('id, old, new, filter, changes, filtered', [
    ('not_tag_INSERT', '', '<p>1</p><b>2</b>',
     lambda: Filter.Tags(tags=['p'], descr=''),
     [Change(type=Types.INSERT, to_id='/html/body/p', to_elem=LxmlElementWrapper(tag='p', data={'text_': u'1'}))],
     [Change(type=Types.INSERT, to_id='/html/body/b', to_elem=LxmlElementWrapper(tag='b', data={'text_': u'2'}))]),
    ('not_xpath_DELETE', '<p>1</p><b>2</b>', '',
     lambda: Filter.Xpath(xpath="//p", descr=''),
     [Change(type=Types.DELETE, id='/html/body/p', elem=LxmlElementWrapper(tag='p', data={'text_': u'1'}))],
     [Change(type=Types.DELETE, id='/html/body/b', elem=LxmlElementWrapper(tag='b', data={'text_': u'2'}))]),
    ('not_attribute_EDIT', '<p id="1">1</p>', '<p id="2">2</p>',
     lambda: Filter.Attributes(attributes=['id'], descr=''),
     [Change(type=Types.EDIT, id='/html/body/p', to_id='/html/body/p',
             diff=[Change(type=Types.REPLACE, id='id', to_id='id', elem=u'1', to_elem=u'2')],
             elem=LxmlElementWrapper(tag='p', data={'id': u'1', 'text_': u'1'}),
             to_elem=LxmlElementWrapper(tag='p', data={'id': u'2', 'text_': u'2'}))],
     [Change(type=Types.EDIT, id='/html/body/p', to_id='/html/body/p',
             diff=[Change(type=Types.REPLACE, id='text_', to_id='text_', elem=u'1', to_elem=u'2')],
             elem=LxmlElementWrapper(tag='p', data={'id': u'1', 'text_': u'1'}),
             to_elem=LxmlElementWrapper(tag='p', data={'id': u'2', 'text_': u'2'}))]),
    ('not_value_EDIT', '<p id="11">a 33 b</p>', '<p id="22">a 44 b</p>',
     lambda: Filter.Value(value='33', descr=''),
     [Change(type=Types.EDIT, id='/html/body/p', to_id='/html/body/p',
             diff=[Change(type=Types.REPLACE, id='text_', to_id='text_', elem=u'a 33 b',
                          to_elem=u'a 44 b')],
             elem=LxmlElementWrapper(tag='p', data={'id': u'11', 'text_': u'a 33 b'}),
             to_elem=LxmlElementWrapper(tag='p', data={'id': u'22', 'text_': u'a 44 b'}))],
     [Change(type=Types.EDIT, id='/html/body/p', to_id='/html/body/p',
             diff=[Change(type=Types.REPLACE, id='id', to_id='id', elem=u'11', to_elem=u'22')],
             elem=LxmlElementWrapper(tag='p', data={'id': u'11', 'text_': u'a 33 b'}),
             to_elem=LxmlElementWrapper(tag='p', data={'id': u'22', 'text_': u'a 44 b'}))]),
    ('not_tags_EDIT', '<p>1</p><h>2</h><b>3</b>', '<p>11</p><h>22</h><b>33</b>',
     lambda: Filter.Tags(tags=['p', 'h']),
     [Change(type=Types.EDIT, id='/html/body/p', to_id='/html/body/p',
             diff=[Change(type=Types.REPLACE, id='text_', to_id='text_', elem=u'1', to_elem=u'11')],
             elem=LxmlElementWrapper(tag='p', data={'text_': u'1'}),
             to_elem=LxmlElementWrapper(tag='p', data={'text_': u'11'})),
      Change(type=Types.EDIT, id='/html/body/h', to_id='/html/body/h',
             diff=[Change(type=Types.REPLACE, id='text_', to_id='text_', elem=u'2', to_elem=u'22')],
             elem=LxmlElementWrapper(tag='h', data={'text_': u'2'}),
             to_elem=LxmlElementWrapper(tag='h', data={'text_': u'22'}))],
     [Change(type=Types.EDIT, id='/html/body/b', to_id='/html/body/b',
             diff=[Change(type=Types.REPLACE, id='text_', to_id='text_', elem=u'3', to_elem=u'33')],
             elem=LxmlElementWrapper(tag='b', data={'text_': u'3'}),
             to_elem=LxmlElementWrapper(tag='b', data={'text_': u'33'}))]),
    ('INSERT_DELETE_EDIT_MOVE_filter_attributes',
     '<p>1</p><p>2</p><p id="5">3</p><p class="8">4</p>', '<p id="5">3</p><p>1</p><h>6</h><p id="7" class="8a">4a</p>',
     lambda: Filter.Attributes(['text_']),
     [Change(type=Types.MOVE, id='/html/body/p[3]', to_id='/html/body/p[1]',
             elem=LxmlElementWrapper(tag='p', data={'id': u'5', 'text_': u'3'}),
             to_elem=LxmlElementWrapper(tag='p', data={'id': u'5', 'text_': u'3'})),
      Change(type=Types.DELETE, id='/html/body/p[2]', elem=LxmlElementWrapper(tag='p', data={'text_': u'2'})),
      Change(type=Types.INSERT, to_id='/html/body/h', to_elem=LxmlElementWrapper(tag='h', data={'text_': u'6'})),
      Change(type=Types.EDIT, id='/html/body/p[4]', to_id='/html/body/p[3]',
             diff=[Change(type=Types.REPLACE, id='text_', to_id='text_', elem=u'4', to_elem=u'4a')],
             elem=LxmlElementWrapper(tag='p', data={'class': u'8', 'text_': u'4'}),
             to_elem=LxmlElementWrapper(tag='p', data={'id': u'7', 'text_': u'4a', 'class': u'8a'}))],
     [Change(type=Types.EDIT, id='/html/body/p[4]', to_id='/html/body/p[3]',
             diff=[Change(type=Types.REPLACE, id='class', to_id='class', elem=u'8', to_elem=u'8a'),
                   Change(type=Types.INSERT, to_id='id', to_elem=u'7')],
             elem=LxmlElementWrapper(tag='p', data={'class': u'8', 'text_': u'4'}),
             to_elem=LxmlElementWrapper(tag='p', data={'id': u'7', 'text_': u'4a', 'class': u'8a'}))]),
    ('INSERT_DELETE_EDIT_MOVE_filter_xpath',
     '<p>1</p><p>2</p><p id="5">3</p><p class="8">4</p>', '<p id="5">3</p><p>1</p><h>6</h><p id="7" class="8a">4a</p>',
     lambda: Filter.Xpath('//p'),
     [Change(type=Types.MOVE, id='/html/body/p[3]', to_id='/html/body/p[1]',
             elem=LxmlElementWrapper(tag='p', data={'id': u'5', 'text_': u'3'}),
             to_elem=LxmlElementWrapper(tag='p', data={'id': u'5', 'text_': u'3'})),
      Change(type=Types.DELETE, id='/html/body/p[2]', elem=LxmlElementWrapper(tag='p', data={'text_': u'2'})),
      Change(type=Types.EDIT, id='/html/body/p[4]', to_id='/html/body/p[3]',
             diff=[Change(type=Types.REPLACE, id='class', to_id='class', elem=u'8', to_elem=u'8a'),
                   Change(type=Types.INSERT, to_id='id', to_elem=u'7'),
                   Change(type=Types.REPLACE, id='text_', to_id='text_', elem=u'4', to_elem=u'4a')],
             elem=LxmlElementWrapper(tag='p', data={'class': u'8', 'text_': u'4'}),
             to_elem=LxmlElementWrapper(tag='p', data={'id': u'7', 'text_': u'4a', 'class': u'8a'}))],
     [Change(type=Types.INSERT, to_id='/html/body/h', to_elem=LxmlElementWrapper(tag='h', data={'text_': u'6'}))]),
    ('INSERT_DELETE_EDIT_MOVE_filter_value',
     '<p>1</p><p>2</p><p id="5">3</p><p class="8">4</p>', '<p id="5">3</p><p>1</p><h>6</h><p id="7" class="8a">4a</p>',
     lambda: Filter.Value('4a'),
     [Change(type=Types.MOVE, id='/html/body/p[3]', to_id='/html/body/p[1]',
             elem=LxmlElementWrapper(tag='p', data={'id': u'5', 'text_': u'3'}),
             to_elem=LxmlElementWrapper(tag='p', data={'id': u'5', 'text_': u'3'})),
      Change(type=Types.DELETE, id='/html/body/p[2]', elem=LxmlElementWrapper(tag='p', data={'text_': u'2'})),
      Change(type=Types.INSERT, to_id='/html/body/h', to_elem=LxmlElementWrapper(tag='h', data={'text_': u'6'})),
      Change(type=Types.EDIT, id='/html/body/p[4]', to_id='/html/body/p[3]',
             diff=[Change(type=Types.REPLACE, id='text_', to_id='text_', elem=u'4', to_elem=u'4a')],
             elem=LxmlElementWrapper(tag='p', data={'class': u'8', 'text_': u'4'}),
             to_elem=LxmlElementWrapper(tag='p', data={'id': u'7', 'text_': u'4a', 'class': u'8a'}))],
     [Change(type=Types.EDIT, id='/html/body/p[4]', to_id='/html/body/p[3]',
             diff=[Change(type=Types.REPLACE, id='class', to_id='class', elem=u'8', to_elem=u'8a'),
                   Change(type=Types.INSERT, to_id='id', to_elem=u'7')],
             elem=LxmlElementWrapper(tag='p', data={'class': u'8', 'text_': u'4'}),
             to_elem=LxmlElementWrapper(tag='p', data={'id': u'7', 'text_': u'4a', 'class': u'8a'}))]),
], ids=lambda id, old, new, filter, changes, filtered: id)
@pytest.mark.parametrize('parse', [html, xml])
def test_not_filter(id, old, new, filter, changes, filtered, parse):
    old_document = parse(old)
    new_document = parse(new)

    diff = TreeDiff.diff(LxmlElementWrapper.build(old_document), LxmlElementWrapper.build(new_document))
    # todo-igogor убрать descr. Сделать функцию? Добавить filtered в аггрегирующие фильтры и не наследовать?
    diff.filter(Filter.Not(filter()))
    print diff.changes
    assert diff.changes == changes
    actual_filtered = utils.flatten([f.filtered for f in diff.filters])
    print actual_filtered
    assert actual_filtered == filtered


@pytest.mark.parametrize('id, old, new, filters, changes, filtered', [
    ('tag_and_attribute_INSERT', '', '<p>1</p>',
     [Filter.Tags(tags=['p'], descr=''), Filter.Attributes(attributes=['text_'], descr='')],
     [Change(type=Types.INSERT, to_id='/html/body/p', to_elem=LxmlElementWrapper(tag='p', data={'text_': u'1'}))],
     []),
    ('tag_and_xpath_DELETE', '<p>1</p>', '',
     [Filter.Tags(tags=['p'], descr=''), Filter.Xpath(xpath='//p', descr='')],
     [],
     [Change(type=Types.DELETE, id='/html/body/p', elem=LxmlElementWrapper(tag='p', data={'text_': u'1'}))]),
    ('attribute_and_tag_EDIT_one_attribute', '<p>1</p>', '<p>2</p>',
     [Filter.Tags(tags=['p'], descr=''), Filter.Attributes(attributes=['text_'], descr='')],
     [],
     [Change(type=Types.EDIT, id='/html/body/p', to_id='/html/body/p',
             diff=[Change(type=Types.REPLACE, id='text_', to_id='text_', elem=u'1', to_elem=u'2')],
             elem=LxmlElementWrapper(tag='p', data={'text_': u'1'}),
             to_elem=LxmlElementWrapper(tag='p', data={'text_': u'2'}))]),
    ('attribute_and_xpath_EDIT_several_attributes', '<p id="1">2</p>', '<p id="2">3</p>',
     [Filter.Attributes(attributes=['id'], descr=''), Filter.Xpath(xpath='//p', descr='')],
     [Change(type=Types.EDIT, id='/html/body/p', to_id='/html/body/p',
             diff=[Change(type=Types.REPLACE, id='text_', to_id='text_', elem=u'2', to_elem=u'3')],
             elem=LxmlElementWrapper(tag='p', data={'id': u'1', 'text_': u'2'}),
             to_elem=LxmlElementWrapper(tag='p', data={'id': u'2', 'text_': u'3'}))],
     [Change(type=Types.EDIT, id='/html/body/p', to_id='/html/body/p',
             diff=[Change(type=Types.REPLACE, id='id', to_id='id', diff=[], elem=u'1', to_elem=u'2')],
             elem=LxmlElementWrapper(tag='p', data={'id': u'1', 'text_': u'2'}),
             to_elem=LxmlElementWrapper(tag='p', data={'id': u'2', 'text_': u'3'}))]),
    ('attribute_and_value_EDIT', '<p id="111">111</p>', '<p id="222">222</p>',
     [Filter.Attributes(attributes=['id'], descr=''), Filter.Value(value='222', descr='')],
     [Change(type=Types.EDIT, id='/html/body/p', to_id='/html/body/p',
             diff=[Change(type=Types.REPLACE, id='text_', to_id='text_', elem=u'111', to_elem=u'222')],
             elem=LxmlElementWrapper(tag='p', data={'id': u'111', 'text_': u'111'}),
             to_elem=LxmlElementWrapper(tag='p', data={'id': u'222', 'text_': u'222'}))],
     [Change(type=Types.EDIT, id='/html/body/p', to_id='/html/body/p',
             diff=[Change(type=Types.REPLACE, id='id', to_id='id', elem=u'111', to_elem=u'222')],
             elem=LxmlElementWrapper(tag='p', data={'id': u'111', 'text_': u'111'}),
             to_elem=LxmlElementWrapper(tag='p', data={'id': u'222', 'text_': u'222'}))]),
    ('INSERT_DELETE_EDIT_MOVE_filter_xpath_and_value',
     '<p>1</p><p>2</p><p id="5">3</p><p class="8">4</p>', '<p id="5">3</p><p>1</p><h>6</h><p id="7" class="8a">4a</p>',
     [Filter.Xpath("//p"), Filter.Value('4a')],
     [Change(type=Types.MOVE, id='/html/body/p[3]', to_id='/html/body/p[1]',
             elem=LxmlElementWrapper(tag='p', data={'id': u'5', 'text_': u'3'}),
             to_elem=LxmlElementWrapper(tag='p', data={'id': u'5', 'text_': u'3'})),
      Change(type=Types.DELETE, id='/html/body/p[2]', elem=LxmlElementWrapper(tag='p', data={'text_': u'2'})),
      Change(type=Types.INSERT, to_id='/html/body/h', to_elem=LxmlElementWrapper(tag='h', data={'text_': u'6'})),
      Change(type=Types.EDIT, id='/html/body/p[4]', to_id='/html/body/p[3]',
             diff=[Change(type=Types.INSERT, to_id='id', to_elem=u'7'),
                   Change(type=Types.REPLACE, id='class', to_id='class', elem=u'8', to_elem=u'8a')],
             elem=LxmlElementWrapper(tag='p', data={'class': u'8', 'text_': u'4'}),
             to_elem=LxmlElementWrapper(tag='p', data={'id': u'7', 'text_': u'4a', 'class': u'8a'}))],
     [Change(type=Types.EDIT, id='/html/body/p[4]', to_id='/html/body/p[3]',
             diff=[Change(type=Types.REPLACE, id='text_', to_id='text_', elem=u'4', to_elem=u'4a')],
             elem=LxmlElementWrapper(tag='p', data={'class': u'8', 'text_': u'4'}),
             to_elem=LxmlElementWrapper(tag='p', data={'id': u'7', 'text_': u'4a', 'class': u'8a'}))]),
    ('INSERT_DELETE_EDIT_MOVE_filter_xpath_and_not_value',
     '<p>1</p><p>2</p><p id="5">3</p><p class="8">4</p>', '<p id="5">3</p><p>1</p><h>6</h><p id="7" class="8a">4a</p>',
     [Filter.Xpath("//p"), Filter.Not(Filter.Value('4a'))],
     [Change(type=Types.MOVE, id='/html/body/p[3]', to_id='/html/body/p[1]',
             elem=LxmlElementWrapper(tag='p', data={'id': u'5', 'text_': u'3'}),
             to_elem=LxmlElementWrapper(tag='p', data={'id': u'5', 'text_': u'3'})),
      Change(type=Types.DELETE, id='/html/body/p[2]', elem=LxmlElementWrapper(tag='p', data={'text_': u'2'})),
      Change(type=Types.INSERT, to_id='/html/body/h', to_elem=LxmlElementWrapper(tag='h', data={'text_': u'6'})),
      Change(type=Types.EDIT, id='/html/body/p[4]', to_id='/html/body/p[3]',
             diff=[Change(type=Types.REPLACE, id='text_', to_id='text_', elem=u'4', to_elem=u'4a')],
             elem=LxmlElementWrapper(tag='p', data={'class': u'8', 'text_': u'4'}),
             to_elem=LxmlElementWrapper(tag='p', data={'id': u'7', 'text_': u'4a', 'class': u'8a'}))],
     [Change(type=Types.EDIT, id='/html/body/p[4]', to_id='/html/body/p[3]',
             diff=[Change(type=Types.INSERT, to_id='id', to_elem=u'7'),
                   Change(type=Types.REPLACE, id='class', to_id='class', elem=u'8', to_elem=u'8a')],
             elem=LxmlElementWrapper(tag='p', data={'class': u'8', 'text_': u'4'}),
             to_elem=LxmlElementWrapper(tag='p', data={'id': u'7', 'text_': u'4a', 'class': u'8a'}))]),
], ids=lambda id, old, new, filters, changes, filtered: id)
@pytest.mark.parametrize('parse', [html, xml])
def test_all_filter(id, old, new, filters, changes, filtered, parse):
    old_document = parse(old)
    new_document = parse(new)

    diff = TreeDiff.diff(LxmlElementWrapper.build(old_document), LxmlElementWrapper.build(new_document))
    # todo-igogor убрать descr. Сделать функцию? Добавить filtered в аггрегирующие фильтры и не наследовать?
    diff.filter(Filter.All(filters))
    print diff.changes
    assert diff.changes == changes
    actual_filtered = utils.flatten([f.filtered for f in diff.filters])
    print actual_filtered
    assert actual_filtered == filtered


@pytest.mark.parametrize('id, old, new, filters, changes, filtered', [
    ('two_filters_two_elems_one_filtered_other_not_INSERT', '', '<p>1</p><b>2</b>',
     lambda: [Filter.Tags(tags=['p']), Filter.Xpath(xpath='//h')],
     [Change(type=Types.INSERT, to_id='/html/body/b', to_elem=LxmlElementWrapper(tag='b', data={'text_': u'2'}))],
     [Change(type=Types.INSERT, to_id='/html/body/p', to_elem=LxmlElementWrapper(tag='p', data={'text_': u'1'}))]),
    ('both_filters_filter_same_element_DELETE', '<p>1</p><b>2</b>', '',
     lambda: [Filter.Tags(tags=['p']), Filter.Xpath(xpath='//p')],
     [Change(type=Types.DELETE, id='/html/body/b', elem=LxmlElementWrapper(tag='b', data={'text_': u'2'}))],
     [Change(type=Types.DELETE, id='/html/body/p', elem=LxmlElementWrapper(tag='p', data={'text_': u'1'}))]),
    ('two_filters_filter_different_elements_MOVE', '<p>1</p><b>2</b><h>3</h>', '<h>3</h><b>2</b><p>1</p>',
     lambda: [Filter.Xpath(xpath='//p'), Filter.Xpath(xpath='//h')],
     [],
     [Change(type=Types.MOVE, id='/html/body/p', to_id='/html/body/p',
             elem=LxmlElementWrapper(tag='p', data={'text_': u'1'}),
             to_elem=LxmlElementWrapper(tag='p', data={'text_': u'1'})),
      Change(type=Types.MOVE, id='/html/body/h', to_id='/html/body/h',
             elem=LxmlElementWrapper(tag='h', data={'text_': u'3'}),
             to_elem=LxmlElementWrapper(tag='h', data={'text_': u'3'}))]
     ),
    ('complete_and_partial_filtering_same_element_EDIT', '<p id="2">3</p>', '<p id="22">33</p>',
     lambda: [Filter.Xpath(xpath='//p'), Filter.Attributes(['id'])],
     [],
     [Change(type=Types.EDIT, id='/html/body/p', to_id='/html/body/p',
             diff=[Change(type=Types.REPLACE, id='id', to_id='id', elem=u'2', to_elem=u'22'),
                   Change(type=Types.REPLACE, id='text_', to_id='text_', elem=u'3', to_elem=u'33')],
             elem=LxmlElementWrapper(tag='p', data={'id': u'2', 'text_': u'3'}),
             to_elem=LxmlElementWrapper(tag='p', data={'id': u'22', 'text_': u'33'}))]

     ),
    ('partial_and_complete_filtering_same_element_EDIT', '<p id="2">3</p>', '<p id="22">33</p>',
     lambda: [Filter.Attributes(['id']), Filter.Xpath(xpath='//p')],
     [],
     [Change(type=Types.EDIT, id='/html/body/p', to_id='/html/body/p',
             diff=[Change(type=Types.REPLACE, id='id', to_id='id', elem=u'2', to_elem=u'22'),
                   Change(type=Types.REPLACE, id='text_', to_id='text_', elem=u'3', to_elem=u'33')],
             elem=LxmlElementWrapper(tag='p', data={'id': u'2', 'text_': u'3'}),
             to_elem=LxmlElementWrapper(tag='p', data={'id': u'22', 'text_': u'33'}))]),
    ('full_filter_from_two_partial_EDIT', '<p id="2">3</p>', '<p id="22">33</p>',
     lambda: [Filter.Attributes(['id']), Filter.Attributes(['text_'])],
     [],
     # todo-igogor с одной стороны правильно что 2 изменения т.к. отфильтровано 2мя фильтрами, с другой можно объединить
     [Change(type=Types.EDIT, id='/html/body/p', to_id='/html/body/p',
             diff=[Change(type=Types.REPLACE, id='id', to_id='id', elem=u'2', to_elem=u'22')],
             elem=LxmlElementWrapper(tag='p', data={'id': u'2', 'text_': u'3'}),
             to_elem=LxmlElementWrapper(tag='p', data={'id': u'22', 'text_': u'33'})),
      Change(type=Types.EDIT, id='/html/body/p', to_id='/html/body/p',
             diff=[Change(type=Types.REPLACE, id='text_', to_id='text_', elem=u'3', to_elem=u'33')],
             elem=LxmlElementWrapper(tag='p', data={'id': u'2', 'text_': u'3'}),
             to_elem=LxmlElementWrapper(tag='p', data={'id': u'22', 'text_': u'33'}))]),
    ('two_not_filters_EDIT', '<p>1</p><b>2</b><p id="3"></p>', '<p>11</p><b>22</b><p id="33"></p>',
     lambda: [Filter.Not(Filter.Tags(['p'])), Filter.Not(Filter.Attributes(['text_']))],
     [Change(type=Types.EDIT, id='/html/body/p[1]', to_id='/html/body/p[1]',
             diff=[Change(type=Types.REPLACE, id='text_', to_id='text_', elem=u'1', to_elem=u'11')],
             elem=LxmlElementWrapper(tag='p', data={'text_': u'1'}),
             to_elem=LxmlElementWrapper(tag='p', data={'text_': u'11'}))],
     [Change(type=Types.EDIT, id='/html/body/b', to_id='/html/body/b',
             diff=[Change(type=Types.REPLACE, id='text_', to_id='text_', elem=u'2', to_elem=u'22')],
             elem=LxmlElementWrapper(tag='b', data={'text_': u'2'}),
             to_elem=LxmlElementWrapper(tag='b', data={'text_': u'22'})),
      Change(type=Types.EDIT, id='/html/body/p[2]', to_id='/html/body/p[2]',
             diff=[Change(type=Types.REPLACE, id='id', to_id='id', elem=u'3', to_elem=u'33')],
             elem=LxmlElementWrapper(tag='p', data={'id': u'3', 'text_': u''}),
             to_elem=LxmlElementWrapper(tag='p', data={'id': u'33', 'text_': u''}))]),
    ('two_not_filters_with_multiple_options_EDIT',
     '<p>1</p><h id="3"></h><b>2</b><p class="4"></p><h class="5">6</h>',
     '<p>11</p><h id="33"></h><b>22</b><p class="44"></p><h class="55">66</h>',
     lambda: [Filter.Not(Filter.Tags(['p', 'h'])), Filter.Not(Filter.Attributes(['text_', 'id']))],
     [Change(type=Types.EDIT, id='/html/body/p[1]', to_id='/html/body/p[1]',
             diff=[Change(type=Types.REPLACE, id='text_', to_id='text_', elem=u'1', to_elem=u'11')],
             elem=LxmlElementWrapper(tag='p', data={'text_': u'1'}),
             to_elem=LxmlElementWrapper(tag='p', data={'text_': u'11'})),
      Change(type=Types.EDIT, id='/html/body/h[1]', to_id='/html/body/h[1]',
             diff=[Change(type=Types.REPLACE, id='id', to_id='id', elem=u'3', to_elem=u'33')],
             elem=LxmlElementWrapper(tag='h', data={'id': u'3', 'text_': u''}),
             to_elem=LxmlElementWrapper(tag='h', data={'id': u'33', 'text_': u''})),
      Change(type=Types.EDIT, id='/html/body/h[2]', to_id='/html/body/h[2]',
             diff=[Change(type=Types.REPLACE, id='text_', to_id='text_', elem=u'6', to_elem=u'66')],
             elem=LxmlElementWrapper(tag='h', data={'class': u'5', 'text_': u'6'}),
             to_elem=LxmlElementWrapper(tag='h', data={'class': u'55', 'text_': u'66'}))],
     [Change(type=Types.EDIT, id='/html/body/b', to_id='/html/body/b',
             diff=[Change(type=Types.REPLACE, id='text_', to_id='text_', elem=u'2', to_elem=u'22')],
             elem=LxmlElementWrapper(tag='b', data={'text_': u'2'}),
             to_elem=LxmlElementWrapper(tag='b', data={'text_': u'22'})),
      Change(type=Types.EDIT, id='/html/body/p[2]', to_id='/html/body/p[2]',
             diff=[Change(type=Types.REPLACE, id='class', to_id='class', elem=u'4', to_elem=u'44')],
             elem=LxmlElementWrapper(tag='p', data={'class': u'4', 'text_': u''}),
             to_elem=LxmlElementWrapper(tag='p', data={'class': u'44', 'text_': u''})),
      Change(type=Types.EDIT, id='/html/body/h[2]', to_id='/html/body/h[2]',
             diff=[Change(type=Types.REPLACE, id='class', to_id='class', elem=u'5', to_elem=u'55')],
             elem=LxmlElementWrapper(tag='h', data={'class': u'5', 'text_': u'6'}),
             to_elem=LxmlElementWrapper(tag='h', data={'class': u'55', 'text_': u'66'}))]),
    ('INSERT_DELETE_EDIT_MOVE_filter_half',
     '<p>1</p><p>2</p><p id="5">3</p><p class="8">4</p>', '<p id="5">3</p><p>1</p><h>6</h><p id="7" class="8a">4a</p>',
     lambda: [Filter.Xpath("//h"), Filter.Attributes(['id']), Filter.Value('4a')],
     [Change(type=Types.MOVE, id='/html/body/p[3]', to_id='/html/body/p[1]',
             elem=LxmlElementWrapper(tag='p', data={'id': u'5', 'text_': u'3'}),
             to_elem=LxmlElementWrapper(tag='p', data={'id': u'5', 'text_': u'3'})),
      Change(type=Types.DELETE, id='/html/body/p[2]', elem=LxmlElementWrapper(tag='p', data={'text_': u'2'})),
      Change(type=Types.EDIT, id='/html/body/p[4]', to_id='/html/body/p[3]',
             diff=[Change(type=Types.REPLACE, id='class', to_id='class', elem=u'8', to_elem=u'8a')],
             elem=LxmlElementWrapper(tag='p', data={'class': u'8', 'text_': u'4'}),
             to_elem=LxmlElementWrapper(tag='p', data={'id': u'7', 'text_': u'4a', 'class': u'8a'}))],
     [Change(type=Types.INSERT, to_id='/html/body/h', to_elem=LxmlElementWrapper(tag='h', data={'text_': u'6'})),
      Change(type=Types.EDIT, id='/html/body/p[4]', to_id='/html/body/p[3]',
             diff=[Change(type=Types.INSERT, to_id='id', to_elem=u'7')],
             elem=LxmlElementWrapper(tag='p', data={'class': u'8', 'text_': u'4'}),
             to_elem=LxmlElementWrapper(tag='p', data={'id': u'7', 'text_': u'4a', 'class': u'8a'})),
      Change(type=Types.EDIT, id='/html/body/p[4]', to_id='/html/body/p[3]',
             diff=[Change(type=Types.REPLACE, id='text_', to_id='text_', elem=u'4', to_elem=u'4a')],
             elem=LxmlElementWrapper(tag='p', data={'class': u'8', 'text_': u'4'}),
             to_elem=LxmlElementWrapper(tag='p', data={'id': u'7', 'text_': u'4a', 'class': u'8a'}))]),
], ids=lambda id, old, new, filters, changes, filtered: id)
@pytest.mark.parametrize('parse', [html, xml])
def test_filter_combinations(old, new, filters, changes, filtered, parse, id):
    old_document = parse(old)
    new_document = parse(new)

    diff = TreeDiff.diff(LxmlElementWrapper.build(old_document), LxmlElementWrapper.build(new_document))
    diff.filter(*filters())
    print diff.changes
    assert diff.changes == changes
    actual_filtered = utils.flatten([f.filtered for f in diff.filters])
    print actual_filtered
    assert actual_filtered == filtered


@pytest.mark.parametrize('id_, filter_, expected_repr', [
    ('filter_tags', Filter.Tags(descr='test', tags=['a', 'b']), u"filter_(tags=['a', 'b'], descr=u'test')"),
    ('filter_xpath', Filter.Xpath(descr=u'тест', xpath='//p'), u"filter_(xpath='//p', descr=u'тест')"),
    ('filter_attributes', Filter.Attributes(descr=u'тест', attributes=['a', 'b']),
     u"filter_(attributes=['a', 'b'], descr=u'тест')"),
    ('filter_value_unicode', Filter.Value(descr=u'тест', value=u'значение'),
     u"filter_(value=u'значение', descr=u'тест')"),
    ('filter_value', Filter.Value(descr=u'тест', value=u'value'), u"filter_(value=u'value', descr=u'тест')"),
    ('not_filter_tags', Filter.Not(Filter.Tags(descr=u'тест', tags=['a', 'b']), descr=u'not тест'),
     u"not_(filter_(tags=['a', 'b'], descr=u'тест'), descr=u'not тест')"),
    ('not_filter_xpath', Filter.Not(Filter.Xpath(descr=u'тест', xpath='//p'), descr=u'not тест'),
     u"not_(filter_(xpath='//p', descr=u'тест'), descr=u'not тест')"),
    ('not_filter_attributes', Filter.Not(Filter.Attributes(descr=u'тест', attributes=['a', 'b']), descr=u'not тест'),
     u"not_(filter_(attributes=['a', 'b'], descr=u'тест'), descr=u'not тест')"),
    ('not_filter_value', Filter.Not(Filter.Value(descr=u'тест', value=u'значение'), descr=u'not тест'),
     u"not_(filter_(value=u'значение', descr=u'тест'), descr=u'not тест')"),
    ('all_xpath_attributes', Filter.All([Filter.Xpath(xpath='//p'), Filter.Attributes(attributes=['a', 'b'])]),
     u"filter_(xpath='//p', attributes=['a', 'b'], descr=u'Add descr')"),
    ('all_xpath_attributes_tags_value',
     Filter.All([Filter.Xpath(xpath='//p'), Filter.Attributes(attributes=['a', 'b']), Filter.Tags(tags=['p']),
                 Filter.Value(value=u'значение')]),
     u"filter_(xpath='//p', attributes=['a', 'b'], tags=['p'], value=u'значение', descr=u'Add descr')"),
    ('all_xpath_not_attribute',
     Filter.All([Filter.Xpath(xpath='//p'), Filter.Not(Filter.Attributes(attributes=['a', 'b']))]),
     u"all_(descr=u'Add descr', filter_(xpath='//p', descr=u'Add descr'), not_(filter_(attributes=['a', 'b'], descr=u'Add descr'), descr=u'Add descr'))"),
    ('all_xpath_all_attribute_value',
     Filter.All([Filter.Xpath(xpath='//p'),
                 Filter.All([Filter.Attributes(attributes=['a', 'b']), Filter.Value(value=u'значение')])]),
     u"all_(descr=u'Add descr', filter_(xpath='//p', descr=u'Add descr'), filter_(attributes=['a', 'b'], value=u'значение', descr=u'Add descr'))"),
    ('all_too_many', Filter.All([Filter.Xpath(xpath='//p'), Filter.Xpath(xpath='//h')]),
     u"all_(descr=u'Add descr', filter_(xpath='//p', descr=u'Add descr'), filter_(xpath='//h', descr=u'Add descr'))")
], ids=lambda id_, filter_, expected_repr: id_)
def test_filter_smart_repr(filter_, expected_repr, id_):
    print filter_.smart_repr()
    assert filter_.smart_repr() == expected_repr
