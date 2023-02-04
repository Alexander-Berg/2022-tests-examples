# coding: utf-8
import pytest

from btestlib import pagediff


def html(body):
    return "<!DOCTYPE html><html><body>{}</body></html>".format(body)


def xml(body):
    # igogor: оставляем тэги html и body чтобы пути были одинаковые.
    return '<?xml version="1.0" encoding="utf-8"?><!DOCTYPE html><html><body>{}</body></html>'.format(body)


@pytest.mark.parametrize('id, old, new, report', [
    ('one-insert', '', '<p>1</p>',
     '''<!DOCTYPE html>
<html><body><!--pagediff-insert filter_(xpath='/html/body/p', descr=u'Add descr')--><p>1</p></body></html>'''),
    ('two-inserts', '', '<p>1</p><p>2</p>',
     '''<!DOCTYPE html>
<html><body><!--pagediff-insert filter_(xpath='/html/body/p[1]', descr=u'Add descr')--><p>1</p><!--pagediff-insert filter_(xpath='/html/body/p[2]', descr=u'Add descr')--><p>2</p></body></html>'''),
    ('one-edit', '<div><p>1</p></div>', '<div><p id="2">3</p></div>',
     '''<!DOCTYPE html>
<html><body><div><!--pagediff-edit filter_(attributes=['id', 'text_'], xpath='/html/body/div/p', descr=u'Add descr')
id: INSERT(2), text_: REPLACE(1 -> 3)
<p>1</p>--><p id="2">3</p></div></body></html>'''),
    ('two-edits', '<p>1</p><p>2</p>', '<p>1a</p><p>2a</p>',
     '''<!DOCTYPE html>
<html><body><!--pagediff-edit filter_(attributes=['text_'], xpath='/html/body/p[1]', descr=u'Add descr')
text_: REPLACE(1 -> 1a)
<p>1</p>--><p>1a</p><!--pagediff-edit filter_(attributes=['text_'], xpath='/html/body/p[2]', descr=u'Add descr')
text_: REPLACE(2 -> 2a)
<p>2</p>--><p>2a</p></body></html>'''),
    ('edit-and-insert', '<p>1</p>', '<p>1a</p><h>2</p>',
     '''<!DOCTYPE html>
<html><body><!--pagediff-edit filter_(attributes=['text_'], xpath='/html/body/p', descr=u'Add descr')
text_: REPLACE(1 -> 1a)
<p>1</p>--><p>1a</p><!--pagediff-insert filter_(xpath='/html/body/h', descr=u'Add descr')--><h>2</h></body></html>'''),
    ('insert-and-edit', '<p>1</p><p>2</p>', '<h>2</h><p>1</p><p>2a</p>',
     '''<!DOCTYPE html>
<html><body><!--pagediff-insert filter_(xpath='/html/body/h', descr=u'Add descr')--><h>2</h><p>1</p><!--pagediff-edit filter_(attributes=['text_'], xpath='/html/body/p[2]', descr=u'Add descr')
text_: REPLACE(2 -> 2a)
<p>2</p>--><p>2a</p></body></html>'''),
    ('one-delete', '<p>1</p>', '',
     '''<!DOCTYPE html>
<html><body><!--pagediff-delete filter_(xpath='/html/body/p', descr=u'Add descr')
<p>1</p>--></body></html>'''),
    ('two-deletes', '<p>1</p><p>2</p>', '',
     '''<!DOCTYPE html>
<html><body><!--pagediff-delete filter_(xpath='/html/body/p[1]', descr=u'Add descr')
<p>1</p>--><!--pagediff-delete filter_(xpath='/html/body/p[2]', descr=u'Add descr')
<p>2</p>--></body></html>'''),
    ('delete-and-insert', '<p>1</p>', '<h>2</h>',
     '''<!DOCTYPE html>
<html><body><!--pagediff-insert filter_(xpath='/html/body/h', descr=u'Add descr')--><h>2</h><!--pagediff-delete filter_(xpath='/html/body/p', descr=u'Add descr')
<p>1</p>--></body></html>'''),
    ('insert-and-delete', '<p>1</p><p>2</p>', '<p>3</p><p>1</p>',
     '''<!DOCTYPE html>
<html><body><!--pagediff-insert filter_(xpath='/html/body/p[1]', descr=u'Add descr')--><p>3</p><p>1</p><!--pagediff-delete filter_(xpath='/html/body/p[2]', descr=u'Add descr')
<p>2</p>--></body></html>'''),
    ('edit-and-delete', '<p>1</p><h>2</h>', '<p>1a</p>',
     # todo-igogor тут явная ошибка но самого поиска расхождений, а не отчета - отчет верный
     '''<!DOCTYPE html>
<html><body><!--pagediff-insert filter_(xpath='/html/body/p', descr=u'Add descr')--><p>1a</p><!--pagediff-delete filter_(xpath='/html/body/p', descr=u'Add descr')
<p>1</p>--><!--pagediff-delete filter_(xpath='/html/body/h', descr=u'Add descr')
<h>2</h>--></body></html>'''),
    ('delete-and-edit', '<p>1</p><h>2</h>', '<h>2a</h>',
     '''<!DOCTYPE html>
<html><body><!--pagediff-delete filter_(xpath='/html/body/p', descr=u'Add descr')
<p>1</p>--><!--pagediff-edit filter_(attributes=['text_'], xpath='/html/body/h', descr=u'Add descr')
text_: REPLACE(2 -> 2a)
<h>2</h>--><h>2a</h></body></html>'''),
    ('one-move', '<p>1</p><p>2</p>', '<p>2</p><p>1</p>',
     '''<!DOCTYPE html>
<html><body><!--pagediff-move filter_(xpath='/html/body/p[1]', descr=u'Add descr')
<p>1</p>--><p>2</p><!--pagediff-move-to--><p>1</p></body></html>'''),
    ('two-moves', '<p>1</p><p>2</p><p>3</p>', '<p>3</p><p>2</p><p>1</p>',
     '''<!DOCTYPE html>
<html><body><!--pagediff-move-to--><p>3</p><!--pagediff-move filter_(xpath='/html/body/p[1]', descr=u'Add descr')
<p>1</p>--><p>2</p><!--pagediff-move filter_(xpath='/html/body/p[3]', descr=u'Add descr')
<p>3</p>--><!--pagediff-move-to--><p>1</p></body></html>'''),
    ('move-and-insert', '<p>1</p><p>2</p>', '<p>2</p><h>3</h><p>1</p>',
     '''<!DOCTYPE html>
<html><body><!--pagediff-move filter_(xpath='/html/body/p[1]', descr=u'Add descr')
<p>1</p>--><p>2</p><!--pagediff-insert filter_(xpath='/html/body/h', descr=u'Add descr')--><h>3</h><!--pagediff-move-to--><p>1</p></body></html>'''),
    ('insert-and-move', '<p>1</p><p>2</p>', '<h>3</h><p>2</p><p>1</p>',
     '''<!DOCTYPE html>
<html><body><!--pagediff-insert filter_(xpath='/html/body/h', descr=u'Add descr')--><h>3</h><!--pagediff-move filter_(xpath='/html/body/p[1]', descr=u'Add descr')
<p>1</p>--><p>2</p><!--pagediff-move-to--><p>1</p></body></html>'''),
    ('move-and-delete', '<p>1</p><h>2</h><p>3</p>', '<p>3</p><p>1</p>',
     '''<!DOCTYPE html>
<html><body><!--pagediff-move filter_(xpath='/html/body/p[1]', descr=u'Add descr')
<p>1</p>--><p>3</p><!--pagediff-delete filter_(xpath='/html/body/h', descr=u'Add descr')
<h>2</h>--><!--pagediff-move-to--><p>1</p></body></html>'''),
    ('delete-and-move', '<h>2</h><p>1</p><p>3</p>', '<p>3</p><p>1</p>',
     '''<!DOCTYPE html>
<html><body><!--pagediff-delete filter_(xpath='/html/body/h', descr=u'Add descr')
<h>2</h>--><!--pagediff-move-to--><p>3</p><p>1</p><!--pagediff-move filter_(xpath='/html/body/p[2]', descr=u'Add descr')
<p>3</p>--></body></html>'''),
    ('complex-case',
     '''<div id="1"><p id="2">3</p></div>
        <div id="4"><p>5</p></div>
        <div id="6"><p>7</p></div>
        <div id="8"><p>9</p><p>10</p></div>''',
     '''<div id="12"><p>13</p></div>
        <div id="4"></div>
        <div id="1"><p>3a</p></div>
        <div id="6a"><p>8</p><p>7</p><p>9</p></div>
        <div id="8"><p>11</p><p>9</p></div>''',
     '''<!DOCTYPE html>
<html><body><!--pagediff-insert filter_(xpath='/html/body/div[1]', descr=u'Add descr')--><div id="12"><p>13</p></div>
        <!--pagediff-move filter_(xpath='/html/body/div[1]', descr=u'Add descr')
<div id="1"></div>--><div id="4"><!--pagediff-delete filter_(xpath='/html/body/div[2]/p', descr=u'Add descr')
<p>5</p>--></div>
        <!--pagediff-move-to--><div id="1"><!--pagediff-edit filter_(attributes=['id', 'text_'], xpath='/html/body/div[1]/p', descr=u'Add descr')
id: DELETE(2), text_: REPLACE(3 -> 3a)
<p id="2">3</p>--><p>3a</p></div>
        <!--pagediff-edit filter_(attributes=['id'], xpath='/html/body/div[3]', descr=u'Add descr')
id: REPLACE(6 -> 6a)
<div id="6"></div>--><div id="6a"><!--pagediff-insert filter_(xpath='/html/body/div[4]/p[1]', descr=u'Add descr')--><p>8</p><p>7</p><!--pagediff-insert filter_(xpath='/html/body/div[4]/p[3]', descr=u'Add descr')--><p>9</p></div>
        <div id="8"><!--pagediff-insert filter_(xpath='/html/body/div[5]/p[1]', descr=u'Add descr')--><p>11</p><p>9</p><!--pagediff-delete filter_(xpath='/html/body/div[4]/p[2]', descr=u'Add descr')
<p>10</p>--></div></body></html>'''
     )
], ids=lambda id, old, new, report: id)
def test_htmldiff_report(old, new, report, id):
    old_document = html(old)
    new_document = html(new)

    diff = pagediff.htmldiff(new_document, old_document)
    reporter = pagediff.Reporter(new_document, old_document, diff, True, 'test-htmldiff-report' + id)
    try:
        reporter.report()
    except AssertionError:
        pass

    print reporter.get_report_page()
    assert reporter.get_report_page() == report


def test_xmldiff_report():
    old_document = xml('''<div id="1"><p id="2">3</p></div>
        <div id="4"><p>5</p></div>
        <div id="6"><p>7</p></div>
        <div id="8"><p>9</p><p>10</p></div>''')
    new_document = xml('''<div id="12"><p>13</p></div>
        <div id="4"></div>
        <div id="1"><p>3a</p></div>
        <div id="6a"><p>8</p><p>7</p><p>9</p></div>
        <div id="8"><p>11</p><p>9</p></div>''')

    diff = pagediff.xmldiff(new_document, old_document)
    reporter = pagediff.Reporter(new_document, old_document, diff, False, 'test-xmldiff-report')
    try:
        reporter.report()
    except AssertionError:
        pass

    print reporter.get_report_page()
    assert reporter.get_report_page() == '''<!DOCTYPE html>
<html><body><!--pagediff-insert filter_(xpath='/html/body/div[1]', descr=u'Add descr')--><div id="12"><p>13</p></div>
        <!--pagediff-move filter_(xpath='/html/body/div[1]', descr=u'Add descr')
<div id="1"/>--><div id="4"><!--pagediff-delete filter_(xpath='/html/body/div[2]/p', descr=u'Add descr')
<p>5</p>--></div>
        <!--pagediff-move-to--><div id="1"><!--pagediff-edit filter_(attributes=['id', 'text_'], xpath='/html/body/div[1]/p', descr=u'Add descr')
id: DELETE(2), text_: REPLACE(3 -> 3a)
<p id="2">3</p>--><p>3a</p></div>
        <!--pagediff-edit filter_(attributes=['id'], xpath='/html/body/div[3]', descr=u'Add descr')
id: REPLACE(6 -> 6a)
<div id="6"/>--><div id="6a"><!--pagediff-insert filter_(xpath='/html/body/div[4]/p[1]', descr=u'Add descr')--><p>8</p><p>7</p><!--pagediff-insert filter_(xpath='/html/body/div[4]/p[3]', descr=u'Add descr')--><p>9</p></div>
        <div id="8"><!--pagediff-insert filter_(xpath='/html/body/div[5]/p[1]', descr=u'Add descr')--><p>11</p><p>9</p><!--pagediff-delete filter_(xpath='/html/body/div[4]/p[2]', descr=u'Add descr')
<p>10</p>--></div></body></html>'''
