# *-* encoding: utf-8 *-*
import unittest
import pytest
import mock

from at.aux_.models import Person
from at.common import BodyFormatter
from at.common.BodyFormatter import SNIPPET_DOTS
from at.common import Context
from at.common import postlink
from at.aux_ import models

from tests.test_fixtures import main
import importlib

pytestmark = pytest.mark.django_db


def setup_module(module):
    module.real_get_club_id = models.GetClubID
    models.GetClubID = lambda _: 11111


def teardown_module(module):
    models.GetClubID = module.real_get_club_id


def test_remove_tags():
    """BodyFormatter.remove_tags should remove all tags from string"""
    result = "One two three"
    data = """<div>O<b>ne</b> two <a href='http://asdasdas'>th</a><p>ree</p></div>"""

    assert result == BodyFormatter.remove_tags(data)


def test_get_firstline_basic():
    """BodyFormatter.get_firstline should reduce body to maxlen"""
    data = """<body>One two three four five six seven eight nine ten eleven</body>"""
    result = """One"""
    assert result == BodyFormatter.get_firstline(data, 3)


def test_get_first_line_empty():
    """BodyFormatter.get_firstline should return default_text if no text in input"""
    data = """<body></body>"""
    result = "."
    assert result == BodyFormatter.get_firstline(data, default_text=result)


def test_get_first_line_less_than_maxlen():
    """BodyFormatter.get_firstline should return body text if this text less that maxlen"""
    data = """<body>123</body>"""
    result = "123"
    assert result == BodyFormatter.get_firstline(data, 10)


def test_get_first_line_long():
    """BodyFormatter.get_firstline should return truncated text with brake on last space without tags"""
    data = """<body>one two three four five six seven eight</body>"""
    result = "one two"
    assert result == BodyFormatter.get_firstline(data, 10)


def test_get_first_line_no_xml():
    """Should work with non xml"""
    data = """one two three four five six seven eight"""
    result = "one two"
    assert result == BodyFormatter.get_firstline(data, 10)


def test_expand_yauser():
    Person.objects.create(login='zhora',
                          person_id=1120000000017638,
                          title='Георгий Лапин',
                          title_eng='Georgy Lapin',
                          community_type='NONE'
                          )
    """Should expand yauser"""
    body = """<ya user="zhora" uid="1120000000017638"/> test text"""
    with Context.ContextBinder(language='ru'):
        result = BodyFormatter.get_firstline(body)
    assert result == "Георгий Лапин test text"
    with Context.ContextBinder(language='en'):
        result = BodyFormatter.get_firstline(body)
    assert result == "Georgy Lapin test text"



@pytest.mark.parametrize(
    'sample,expected,comment',
    [
        ('<p>%s</p>' % ('a' * 1000), 'a' * 78, 'exactly 78 chars'),
        ('<p><a href="">bbb</a> ccc</p>', 'bbb ccc', 'strip tags'),
        ('<a href="' + 'a' * 1000 + '"/>', SNIPPET_DOTS, 'use dots if empty'),
        ('<p>%s xxx</p>' % ('a' * 77), 'a' * 77, 'word end before maxlen'),
        ('<p>%s</p>' % ('a' * 78), 'a' * 78, 'word end at maxlen exactly'),
        ('<p>xxx %s xxx</p>' % ('a' * 79), 'xxx', 'word end after maxlen'),
        ('<p>some <s>deleted</s> text</p>', 'some  text', 'removes deleted')
    ]
)
def test_simple_generator(sample, expected, comment):
    assert BodyFormatter.get_firstline(sample) == expected, 'fail ' + comment


@pytest.mark.parametrize(
    'sample,expected,comment',
    [
        (
        '<autocut><autocut-text>On June 2nd...</autocut-text>On June 2nd at the opening of WWDC</autocut>',
        '<div>On June 2nd at the opening of WWDC</div>', 'Stripping autocut'),
        ('My friend <ya user="msahnov"/> suggested',
         '<div>My friend <a href="http://msahnov.at.yandex-team.ru/">Mikhail Sakhnov</a> suggested</div>',
         'Ya user resolution'),
        ('We open <cut text="more">cuts and</cut> others',
         '<div>We open cuts and others</div>', 'Cut opening'),
        ('Here<ya-wbr/>we<ya-wbr/>have some &lt;ya-wbr&gt;',
         '<div>Herewehave some &lt;ya-wbr&gt;</div>',
         'Ya-wbr removal'),
        (
        'a ticket <ya-jira project="SEAREL" ticket="1246">SEAREL-1246</ya-jira> here',
        '<div>a ticket <a href="https://st.yandex-team.ru/SEAREL-1246">SEAREL-1246</a> here</div>',
        'ticket resolution'),
    ]
)
def test_generator(sample, expected, comment):
    Person.objects.create(login='msahnov',
                          person_id=12703430003111,
                          title_eng='Mikhail Sakhnov',
                          community_type='NONE'
                          )
    Context.context.language = 'en'
    assert BodyFormatter.fix_text(sample) == expected, comment

@pytest.mark.parametrize(
    'target,is_club,result',
    [
        ('kukutz', False, BodyFormatter.FeedRepresentation),
        ('theigel@yandex-team.ru', False, BodyFormatter.FeedRepresentation),
        ('mag', True, BodyFormatter.FeedRepresentation),
        (1120000000016603, False, BodyFormatter.FeedRepresentation),
        ('shdgf@example.com', False, None)
    ],
)
@mock.patch(
    'at.common.BodyFormatter.Maillists.is_email_ml',
    new=lambda email: False
)
def test_representations_generator(target, is_club, result):

    Person.objects.create(login='kukutz',
                          person_id=1120000000000227,
                          community_type='NONE'
                          )
    Person.objects.create(login='theigel',
                          person_id=23456,
                          community_type='NONE',
                          email='theigel@yandex-team.ru'
                          )
    try:
        t = BodyFormatter.makeRepresentation(target, is_club)
    except ValueError as e:
        assert result is None, (target, is_club, e)
    else:
        assert isinstance(t, result), (target, is_club, result, t)


class FixTextTests(unittest.TestCase):
    def setUp(self):
        importlib.reload(BodyFormatter)

    def testGetTitleShouldReturnSlugIfNoTitle(self):
        """Use slug instead of title/title_eng"""
        target = BodyFormatter.makeRepresentation(4611686018427388226)
        source = models.Person(login='testuser')
        source.title = ''
        source.title_eng = ''
        target._FeedRepresentation__source = source
        self.assertEqual(target.title, source.login)

    def testGetTitleReturnsCurrentLanguage(self):
        feed_id = 1120000000016603
        Person.objects.create(person_id=feed_id, login='', community_type='NONE')
        target = BodyFormatter.makeRepresentation(feed_id)
        source = target.source
        with Context.ContextBinder(language='ru'):
            self.assertEqual(target.title, source.title)
            self.assertEqual(BodyFormatter.get_feed_title_xslt(None, [feed_id]), source.title)
        with Context.ContextBinder(language='en'):
            target = BodyFormatter.makeRepresentation(feed_id)
            self.assertEqual(target.title, target.source.title_eng)

    def testShouldWrapMediaContentRus(self):
        """Should subtitute iframe with infomessage"""
        Person.objects.create(person_id=1120000000016603, login='msahnov', community_type='NONE')
        text = """<p>Hi there!</p><iframe src="blabla"/><p>Bye</p>"""
        resultNeed = """<div><p>Hi there!</p><p style="font-style: italic">
         Тут должен быть видеоролик, но отобразить его невозможно. \
<a href="https://msahnov.%s/1">Перейдите к записи</a>, \
чтобы посмотреть его.
     </p><p>Bye</p></div>""" % postlink.DOMAIN
        with Context.ContextBinder(
                feed_id=1120000000016603,
                item_no=1,
                comment_id=0,
                language='ru'):
            resultHave = BodyFormatter.fix_text(text)
        self.assertEqual(resultHave, resultNeed)

    def testShouldWrapMediaContentEng(self):
        """Should subtitute iframe with infomessage"""
        Person.objects.create(person_id=1120000000016603, login='msahnov', community_type='NONE')
        text = """<p>Hi there!</p><iframe src="blabla"/><p>Bye</p>"""
        resultNeed = """<div><p>Hi there!</p><p style="font-style: italic">
         Here should be an embedded video, but it can't be displayed. \
<a href="https://msahnov.%s/1">\
Go to the original entry</a> to watch it.
     </p><p>Bye</p></div>""" % postlink.DOMAIN
        with Context.ContextBinder(feed_id=1120000000016603,
                                   item_no=1,
                                   comment_id=0,
                                   language='en'
                                   ):
            resultHave = BodyFormatter.fix_text(text)
        self.assertEqual(resultHave, resultNeed)

    def testClearLinksFromHideRef(self):
        """Should remove hideref from href attributes"""
        text = """<a href="https://h.yandex-team.ru/?http%3A%2F%2Fnews.yandex.ru">news</a>"""
        self.assertEqual(BodyFormatter.fix_text(text),
                         """<div><a href="http://news.yandex.ru">news</a></div>""")
        text = """<a href="//h.yandex-team.ru/?http%3A%2F%2Fnews.yandex.ru">news</a>"""
        self.assertEqual(BodyFormatter.fix_text(text),
                         """<div><a href="http://news.yandex.ru">news</a></div>""")
        text = """<a href="http://h.yandex-team.ru/?http%3A%2F%2Fnews.yandex.ru">news</a>"""
        self.assertEqual(BodyFormatter.fix_text(text),
                         """<div><a href="http://news.yandex.ru">news</a></div>""")

    def testHideRefClearingShouldNotBreaksOnPunnyCode(self):
        text = ('<a href="https://h.yandex-team.ru/'
                '?http%3A%2F%2Fru.wikipedia.org%2Fwiki'
                '%2F%25D0%25A1%25D0%25BA%25D0%25B0%25D0'
                '%25B9%25D0%25BB%25D1%258D%25D0%25B1">Skylab</a>')
        result = ('<a href="http://ru.wikipedia.org/wiki/'
                  '%D0%A1%D0%BA%D0%B0%D0%B9%D0%BB%D1%8D%D0%B1">Skylab</a>')
        self.assertEqual(BodyFormatter.fix_text(text), "<div>%s</div>" % result)

    def testHideRefClearingShouldNotAffectHrefWhenThereIsNoHideRef(self):
        text = ('<a href="http://ru.wikipedia.org/wiki/'
                  '%D0%A1%D0%BA%D0%B0%D0%B9%D0%BB%D1%8D%D0%B1">Skylab</a>')
        self.assertEqual(BodyFormatter.fix_text(text), '<div>%s</div>' % text)

    def testReplaceWikiParagraphsWithReal(self):
        """Should replace div.wiki-p with p"""
        text = """<div class="wiki-p">I am text</div>"""
        self.assertEqual(BodyFormatter.fix_text(text),
                         """<div><p class="wiki-p">I am text</p></div>""")

    def testFirstLineShouldReplaceLineBreaksIntoSpaces(self):
        text = ("test\n"
                "another line")
        result = "test another line"
        self.assertEqual(result, BodyFormatter.get_firstline(text))

    def testFirstLineShouldReplaceBrIntoSpaces(self):
        text = ("<p>123<br/>123</p>")
        result = "123 123"
        self.assertEqual(result, BodyFormatter.get_firstline(text))


class ResizerTests(unittest.TestCase):
    def setUp(self):
        importlib.reload(BodyFormatter)

    def testDontUseResizerOnIntranetLinks(self):
        """Use direct links on intranet pictures"""
        urls = ("https://wiki.yandex-team.ru/.files/msahnov/image.png",
                "http://wiki.yandex-team.ru/.files/msahnov/image.png")
        for url in urls:
            self.assertEqual(url, BodyFormatter.resize_image(None, [url]))

    def testSubstituteSizeInFotkiLinks(self):
        """Change size in fotki urls"""
        url = 'http://img-fotki.yandex-team.ru/get/39/1120000000016603.1/0_3b_afeb4ed8_orig.jpg'
        final_url = 'http://img-fotki.yandex-team.ru/get/39/1120000000016603.1/0_3b_afeb4ed8_L.jpg'
        self.assertEqual(final_url, BodyFormatter.resize_image(None, [url]))

    def testUseResizerForOtherLinks(self):
        """Use resizer on outworld links"""
        url = "http://cool-pictures.com/cool-picture.png"
        args = []

        def resizer_callback(url, width):
            args.append((url, width))

        BodyFormatter.resizer.get_resizer = lambda: main.FakeResizer(resizer_callback)
        BodyFormatter.resize_image(None, [url])
        self.assertEqual(args, [(url, BodyFormatter.MAX_IMAGE_WIDTH)])

    def testResizerShouldNotResizeOnDataSrc(self):
        resize_image = lambda url: url
        BodyFormatter.resizer.get_resizer = lambda: main.FakeResizer(resize_image)
        url = "data:image/gif;base64,R0lGODlhAQABAIAAAP///wAAACH5BAEAAAAALAAAAAABAAEAAAICRAEAOw=="
        self.assertEqual(BodyFormatter.resize_image(None, [url]), url)

    def testRemoveHideRefFromImageLinks(self):
        """Should remove hideref from img src's"""
        resize_image = lambda url: url
        BodyFormatter.xslt_custom_function('resize-image')(resize_image)
        text = """<img src="//h.yandex-team.ru/?http%3A%2F%2Fwww.adom.de%2Fhome%2Fimg%2Fparallax01.png"/>"""
        self.assertEqual(BodyFormatter.fix_text(text),
        """<div><img src="http://www.adom.de/home/img/parallax01.png" """
        """style="max-width: 600px; max-height: 600px; width: auto;"/></div>""")



