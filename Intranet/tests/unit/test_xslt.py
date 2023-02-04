# *-* encoding: utf-8 *-*

from collections import defaultdict

from at.common.xslt.transform import *
from at.common import BodyFormatter


transform = XSLTransformer.getInstance()


def setup_module(module):
    XSLTransformer.register_function('my-test', xslt_function)
    XSLTransformer.register_function('get-feed-title', fake_get_feed_title_xslt)


def teardown_module(module):
    XSLTransformer.register_function('get-feed-title', BodyFormatter.get_feed_title_xslt)


def xslt_function(context, *args, **kwargs):
    return " ".join((str(el) for el in args))


def fake_get_feed_title_xslt(ctx, feed_id, inf=None, person=True):
    feed_id = feed_id.pop()
    try:
        inf = inf.pop()
    except (IndexError, AttributeError):
        inf = "im"
    persons_data = {
        "theigel": {
            "rod": "Сергея Чистовича",
            "im": "Сергей Чистович"
        },
        "BAD-USER": defaultdict(lambda: "BAD-USER")
    }
    clubs_data = {
        "4611686018427388522": "zhezhezhe",
    }
    if not person:
        return clubs_data.get(feed_id) or feed_id
    else:
        return persons_data[feed_id][inf]

MAX_IMAGE_WIDTH = BodyFormatter.MAX_IMAGE_WIDTH


tests = {
    'test': [
        (
            '<custom-functions></custom-functions>',
            None,
            'Simple custom functions check'
        ),
       (
           '<test>This text will deleted</test>',
            '<ok/>',
            'Simple check if transformation performs'
        ),
    ],
    'body-formatter': [
        (
             '<img src="http://fotki.yandex-team.ru/aaa_orig"/>',
             '<img src="http://fotki.yandex-team.ru/aaa_L" style="max-width: %(max_size)spx; max-height: %(max_size)spx; width: auto;"/>' % {'max_size': MAX_IMAGE_WIDTH},
             'Image resizer for Fotki'
        ),
        (
             '<img src="http://fotki.yandex-team.ru/aaa_orig" style="fake"/>',
             '<img src="http://fotki.yandex-team.ru/aaa_L" style="max-width: %(max_size)spx; max-height: %(max_size)spx; width: auto;"/>' % {'max_size': MAX_IMAGE_WIDTH},
             'Template style more prior'
        ),
       (
           '<div><p>Hello, world!</p></div>',
            '<div><p>Hello, world!</p></div>',
            'Perform transformation without matches'
        ),
       (
           '<test><ya-jira project="AT" ticket="666"/></test>',
        '<test><a href="https://st.yandex-team.ru/AT-666">AT-666</a></test>',
        'Jira-link'),
       ('<test><ya user="theigel"/></test>',
        '<test><a href="http://theigel.at.yandex-team.ru/">Сергей Чистович</a></test>',
        'Ya user: title from source'),
       ('<test><ya user="theigel" title="THEIGEL"/></test>',
        '<test><a href="http://theigel.at.yandex-team.ru/">THEIGEL</a></test>',
        'Ya user with title'),
       ('<test><ya user="theigel" inf="rod"/></test>',
        '<test><a href="http://theigel.at.yandex-team.ru/">Сергея Чистовича</a></test>',
        'Ya user inflection'),
       ('<test><ya user="BAD-USER"/></test>',
        '<test><a href="http://BAD-USER.at.yandex-team.ru/">BAD-USER</a></test>',
        'Ya user bad login'
        ),
        (
            '<test><ya club="zhezhezhe"/></test>',
            '<test><a href="http://clubs.at.yandex-team.ru/zhezhezhe">zhezhezhe</a></test>',
            'Ya user club resolve by slug'
        ),
       (
            '<test><ya club="4611686018427388522"/></test>',
            '<test><a href="http://clubs.at.yandex-team.ru/4611686018427388522">zhezhezhe</a></test>',
            'Ya user club resolve by id'
        ),
       (
            '<test><ya club="7878787878"/></test>',
            '<test><a href="http://clubs.at.yandex-team.ru/7878787878">7878787878</a></test>',
            'Ya user club bad id'
        ),
        (
            '<test><autocut-text>123123</autocut-text><ya-wbr/><cut>123</cut><autocut>123123</autocut></test>',
            '<test>123123123</test>',
            'Delete cuts and other'
        ),
        (
            '<img src="https://img-fotki.yandex-team.ru/get/146/1120000000017429.0/0_63_cee91bd0_orig.jpg"/>',
            '<img src="https://img-fotki.yandex-team.ru/get/146/1120000000017429.0/0_63_cee91bd0_L.jpg" style="max-width: %(max_size)spx; max-height: %(max_size)spx; width: auto;"/>' % {'max_size': MAX_IMAGE_WIDTH},
            'Fotki URL change'
        ),
        (
            '<img src="https://img-fotki.yandex-team.ru/get/146/1120000000017429.0/0_63_cee91bd0_M.jpg" width="100" height="200"/>',
            '<img src="https://img-fotki.yandex-team.ru/get/146/1120000000017429.0/0_63_cee91bd0_M.jpg" style="max-width: %(max_size)spx; max-height: %(max_size)spx; width: auto;"/>' % {'max_size': MAX_IMAGE_WIDTH},
            'Fotki small URL not changed and size attrs cleaned'
        ),
    ],
    'textify': [
        (
            '<root>I want to <b>break free</b></root>',
            'I want to break free',
            'From tags to plain text'
        ),
        (
            '<root>I like AT-618</root>',
            'I like AT-618',
            'Plaintext tickets'
        ),
        (
            '<root>I am <ya user="theigel" title="Ежила"/></root>',
            'I am Ежила',
            'Plaintext ya users'
        ),
        (
            '<root>Some escaped &lt;tag&gt;</root>',
            'Some escaped <tag>',
            'Entities should be decoded'
        ),
        (
            '<div><ul><li>First</li><li>Second</li></ul> <ol><li>First</li><li>Second</li></ol></div>',
            '* First * Second  1. First 2. Second ',
            'List dehtml'
        )
    ]
}


# def test_generator():
#     for template, data in tests.items():
#         for inp, out, description in data:
#             test_generator.compat_func_name = "[Template: %s] %s" % (template, description)
#             yield check, template, inp, out
#
#
# def check(template, body, result):
#     """Template test function"""
#     _t = transform.apply_template
#     if template == 'textify':
#         process = lambda body: _t(_t(body, 'body-formatter', True), template, False).getroot().text
#     else:
#         process = lambda body: _t(body, template, True)
#     have_result = process(body)
#     if isinstance(result, str):
#         result = unicode(result)
#     assert result.strip() == have_result.strip(), (result, have_result)


