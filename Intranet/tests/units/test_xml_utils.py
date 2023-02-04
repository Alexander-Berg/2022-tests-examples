import pytest

from lxml import etree

from intranet.search.core.utils.xml import parse_html
from intranet.search.core.sources.utils import get_text_content, split_by_sub_titles


@pytest.mark.parametrize('case', (
    ('<div>div1</div><div>div2<div>', 'div1\ndiv2'),
    ('<span class="wiki-username__first-letter">t</span>malikova@</span>', 'tmalikova@'),
    ('<span>М</span>аликова', 'Маликова'),
    ('abc<br>def', 'abc\ndef'),
    ('abc<br/>def', 'abc\ndef'),
    ('abc\ndef', 'abc\ndef'),
    ('ab<code>cd</code>ef', 'abcdef'),
    ('ab <code>cd</code> ef', 'ab cd ef'),
    ('<ul><li>ab</li><li>cd</li></ul><div>ef</div>', 'ab cd\nef'),
    ('H<sub>2</sub>0', 'H20'),
    ('E=mc<sup>2</sup>', 'E=mc2'),
    ('ab<div>cd</div>ef', 'ab\ncd\nef'),
    ('<span>ab</span><div>cd</div><span>ef</span>', 'ab\ncd\nef'),
    ('<div>кириллический текст <span>и ко</span></div>', 'кириллический текст и ко'),
    ('<?xml version="1.0" encoding="utf-8"?> <div>текст</div>', 'текст'),
    ('<title>заголовок страницы</title>', 'заголовок страницы'),
))
def test_get_text_content(case):
    value, expected = case
    assert get_text_content(value) == expected, case


@pytest.mark.parametrize('html, expected', (
    (
        '<h2 id="h2-1">h21</h2><p>text 1</p><h2 id="h2-2">h22</h2><p>text 2</p>',
        [
            {
                'title': '<h2 id="h2-1">h21</h2>',
                'content': '<div><h2 id="h2-1">h21</h2><p>text 1</p></div>',
            },
            {
                'title': '<h2 id="h2-2">h22</h2>',
                'content': '<div><h2 id="h2-2">h22</h2><p>text 2</p></div>',
            },
        ],
    ),
    (
        '<h2 id="h2">h21</h2><p>t1</p><h3 id="h3">h3</h3><p>t2</p>',
        [
            {
                'title': '<h2 id="h2">h21</h2>',
                'content': '<div><h2 id="h2">h21</h2><p>t1</p></div>',
            },
            {
                'title': '<h3 id="h3">h3</h3>',
                'content': '<div><h3 id="h3">h3</h3><p>t2</p></div>',
            },
        ],
    ),
    (
        '<p>intro 1</p><h2 id="h2-1">h21</h2><p>text 1</p><h2 id="h2-2">h22</h2><p>text 2</p>',
        [
            {
                'title': None,
                'content': '<div><p>intro 1</p></div>',
            },
            {
                'title': '<h2 id="h2-1">h21</h2>',
                'content': '<div><h2 id="h2-1">h21</h2><p>text 1</p></div>',
            },
            {
                'title': '<h2 id="h2-2">h22</h2>',
                'content': '<div><h2 id="h2-2">h22</h2><p>text 2</p></div>',
            },
        ],
    ),

    # документы без параграфов сохраняем как целые
    ('<h2>count</h2><p>some text</p>', []),
    (
        '<h3 id="h3">count</h3><p>some text</p>',
        [
            {
                'title': '<h3 id="h3">count</h3>',
                'content': '<div><h3 id="h3">count</h3><p>some text</p></div>',
            },
        ],
    ),
    (
        '<h4 id="h3">count</h4><p>some text</p>',
        [
            {
                'title': '<h4 id="h3">count</h4>',
                'content': '<div><h4 id="h3">count</h4><p>some text</p></div>',
            },
        ],
    ),
))
def test_split_by_sub_titles(html, expected):
    result = split_by_sub_titles(parse_html(html))
    to_check = []

    for part in result:
        to_check.append({
            'title': etree.tostring(part['title']).decode('utf-8')
            if part['title'] is not None
            else None,
            'content': etree.tostring(part['content']).decode('utf-8'),
        })
    assert to_check == expected
