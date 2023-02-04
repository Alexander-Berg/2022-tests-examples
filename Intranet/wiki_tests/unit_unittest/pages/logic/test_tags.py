
import random
import unicodedata

from django.conf import settings
from mock import patch

from wiki.pages.logic import tags as tags_logic
from wiki.utils.errors import ValidationError
from wiki.utils.supertag import tag_to_supertag
from intranet.wiki.tests.wiki_tests.common.unittest_base import BaseApiTestCase


class PageTagsLogicTestCase(BaseApiTestCase):
    def test_tag_to_supertag(self):
        for tag, supertag in [
            ('Соме/Паге.', 'some/page'),
            ('/Соме/', 'some'),
            ('.', ''),
            ('!', ''),
            ('/', ''),
        ]:
            self.assertEqual(tag_to_supertag(tag), supertag)

    def _test_string_to_tag(self, source_link):
        with patch('wiki.utils.supertag.tag_to_supertag', lambda x: x):
            return tags_logic.string_to_tag(source_link)

    def _test_string_to_tag_valid(self, source_link, result_link=None):
        if result_link is None:
            result_link = source_link
        result = self._test_string_to_tag(source_link)
        self.assertEqual(result, result_link)

    def _test_string_to_tag_invalid(self, link):
        with self.assertRaises(ValidationError) as cm:
            self._test_string_to_tag(link)
        self.assertEqual(cm.exception.invalid_value, link)

    def test_get_page_by_link(self):
        self._test_string_to_tag_valid('стр')
        self._test_string_to_tag_valid('стр/подстр')
        self._test_string_to_tag_valid('/стр', 'стр')
        self._test_string_to_tag_valid('/стр/подстр', 'стр/подстр')
        self._test_string_to_tag_valid('стр//подстр', 'стр/подстр')
        self._test_string_to_tag_valid('/стр/подстр/', 'стр/подстр')
        self._test_string_to_tag_valid('//стр', 'стр')
        self._test_string_to_tag_valid(' //стр ', 'стр')
        self._test_string_to_tag_valid('///стр/', 'стр')

        wiki_host = random.choice(list(settings.FRONTEND_HOSTS))
        self._test_string_to_tag_valid('https://%s/стр/?foo=bar#hello' % wiki_host, 'стр')
        self._test_string_to_tag_valid('%s/стр' % wiki_host, 'стр')
        self._test_string_to_tag_valid('//%s/стр' % wiki_host, 'стр')
        self._test_string_to_tag_valid('/%s/стр' % wiki_host, 'стр')
        self._test_string_to_tag_valid('/%sмусор/стр' % wiki_host, '%sмусор/стр' % wiki_host)
        self._test_string_to_tag_valid('http://%s/page' % wiki_host, 'page')

        self._test_string_to_tag_invalid('')
        self._test_string_to_tag_invalid('/')
        self._test_string_to_tag_invalid('https://%s/' % wiki_host)
        self._test_string_to_tag_invalid('https://%s' % wiki_host)
        self._test_string_to_tag_invalid('https://my.at.yandex-team.ru/page')

    def test_fix_tags_piece_good_tags(self):
        good_tags = [
            'я',
            'яя',
            'x',
            '-x',
            '_x',
            'x_',
            'x-',
            'x.',
            'x:',
            'x+',
        ]
        for tag in good_tags:
            fixed_tag = tags_logic._fix_tag_piece(tag)
            self.assertEqual(fixed_tag, tag)

    def test_fix_tags_piece_fixable_tags(self):
        bad_tags = [
            # bad prefixes
            ('.x', 'x'),
            ('..x', 'x'),
            ('..x.', 'x.'),
            ('+x.', 'x.'),
            ('!x.', 'x.'),
            ('!', ''),
            # bad other symbols
            ('x!', 'x'),
            ('x!y', 'xy'),
            ('x!y&', 'xy'),
            # bad symbols everywhere
            ('.x!y&+.%:', 'xy+.:'),
            ('!&%', ''),
            ('!&%.', ''),
            ('x y', 'x-y'),
            (' x ', '-x-'),
            (' x ', '-x-'),
            ('x' + unicodedata.lookup('EM DASH'), 'x-'),
            ('x' + unicodedata.lookup('NO-BREAK SPACE'), 'x-'),
        ]
        for tag, expected in bad_tags:
            fixed_tag = tags_logic._fix_tag_piece(tag)
            self.assertEqual(fixed_tag, expected)

    def test_fix_tag(self):
        wiki_host = random.choice(list(settings.FRONTEND_HOSTS))
        wiki_url = settings.WIKI_PROTOCOL + '://' + wiki_host
        tags = [
            'утф-8',
            (wiki_url + '/sometag', 'sometag'),
            ('okay/.crap', 'okay/crap'),
            ('+okay/crap', 'okay/crap'),
            ('+okay/&crap/+hi', 'okay/crap/hi'),
            ('+okay/&!/hi', 'okay/hi'),
        ]

        for tag in tags:
            if isinstance(tag, str):
                input = expected = tag
            else:
                input, expected = tag

            fixed_tag = tags_logic.fix_tag(input)
            self.assertEqual(fixed_tag, expected)


valid_tags = (
    'roottÅg',
    'Бла/бла',
    'Бла/бла/',
    'a',
    'aa',
    'a/',
    'aa/',
    'a/b',
    'a:',
    'a::',
    'a::b',
    'a:/b:',
    'a/b:',
    'a-',
    'a--',
    'a--b',
    'a-/b-',
    'a/b-',
    '-',
    '--',
    '-a',
    '-a-b',
    'a/-b',
    '_',
    '__',
    '_a',
    '_a_b',
    'a/_b',
    'a.',
    'a..',
    'a..b',
    'a./b.',
    'a/b.',
    'a+',
    'a++',
    'a++b',
    'a+/b+',
    'a/b+',
)
invalid_tags = (
    '',
    ' ',
    '  ',
    '\n',
    ' a',
    '  a',
    'a b',
    '/',
    '//',
    '/a',
    '/a/b',
    'a//b',
    ':',
    '::',
    ':a',
    ':a:b',
    'a/:b',
    '.',
    '..',
    '.a',
    '.a.b',
    'a/.b',
    '"',
    '""',
    '"a',
    '"a"b',
    'a/"b',
    'a"',
    "'",
    "''",
    "'a",
    "'a'b",
    "a/'b",
    "a'",
    '#',
    '##',
    '#a',
    '#a#b',
    'a/#b',
    'a#',
    '+',
    '++',
    '+a',
    '+a+b',
    'a/+b',
    'a\xa0b',
    'a\u2004b',
    'a\u2003b',
    'a\u2002b',
)


class TagRegexTest(BaseApiTestCase):
    def _test(self, regex, tags, is_valid):
        for tag in tags:
            if bool(regex.match(tag)) != is_valid:
                msg = "Expected tag '%s' validity is %s, but it's not"
                self.fail(msg % (tag, is_valid))

    def test_valid(self):
        self._test(tags_logic.COMPILED_TAG_REGEX, valid_tags, True)

    def test_invalid(self):
        self._test(tags_logic.COMPILED_TAG_REGEX, invalid_tags, False)
