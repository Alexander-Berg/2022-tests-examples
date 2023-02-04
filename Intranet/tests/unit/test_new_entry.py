# coding: utf-8

import datetime
import unittest

import pytest
from lxml import etree as ET

from at.aux_ import entries
from at.aux_.entries import serializers, repositories
from at.aux_.entries.models import Post, Summon, Text
from at.common import utils
import importlib


AI = utils.getGodAuthInfo()


class TestDeserializer(unittest.TestCase):

    def assertIsSubDict(self, dict_one, dict_two):
        diff = {}
        for key, value in list(dict_one.items()):
            if key not in dict_two:
                diff[key] = 'Key not found'
                continue

            value_two = dict_two[key]
            if value != value_two:
                diff[key] = (value, value_two)

        msg = '\n'.join(str(d) for d in (diff, dict_one, dict_two))
        self.assertFalse(diff, msg)

    def test_entry_fields(self):
        xml = ET.XML("""
                <entry>
                <type>text</type>
                <edit_time>1440582470</edit_time>
                <published>1</published>
                <cc_addresses>email@domain.ru</cc_addresses>
                <content>
                    <title>Title мысли</title>
                    <body><div class="wiki-doc i-bem" data-bem="{&quot;wiki-doc&quot;:{&quot;user&quot;:{&quot;codeTheme&quot;:&quot;github&quot;}}}"><div class="wiki-p">Тело мысли</div></div></body>
                    <body-original type="text/wiki">Тело мысли</body-original>
                    <snippet>Тело мысли</snippet>
                </content>
                </entry>
        """)
        data = {
            'entry_type': 'text',
            'published': True,
            'cc_address': 'email@domain.ru',
        }
        self.assertIsSubDict(data, entries.serializers.deserialize_xml_content(Text, xml))

    def test_content_empty_title(self):
        """Should parse empty title as None, not as flag"""
        xml = ET.XML("""
            <entry>
            <type>text</type>
            <filter_type>text</filter_type>
            <edit_time>1440582470</edit_time>
            <published>1</published>
            <content>
                <title></title>
                <body><div class="wiki-doc i-bem" data-bem="{&quot;wiki-doc&quot;:{&quot;user&quot;:{&quot;codeTheme&quot;:&quot;github&quot;}}}"><div class="wiki-p">Тело мысли</div></div></body>
                <body-original type="text/wiki">Тело мысли</body-original>
                <snippet>Тело мысли</snippet>
            </content>
            </entry>
        """)
        data = {
            '_title': '',
            '_body': '<div class="wiki-doc i-bem" data-bem="{&quot;wiki-doc&quot;:{&quot;user&quot;:{&quot;codeTheme&quot;:&quot;github&quot;}}}"><div class="wiki-p">Тело мысли</div></div>',
            'body_original': 'Тело мысли',
            '_snippet': 'Тело мысли',
        }
        self.assertIsSubDict(data, serializers.deserialize_xml_content(Text, xml))

    def test_content_fields(self):
        """Should parse content fields from xml"""
        xml = ET.XML("""
            <entry>
            <type>text</type>
            <edit_time>1440582470</edit_time>
            <published>1</published>
            <content>
                <title>Title мысли</title>
                <body><div class="wiki-doc i-bem" data-bem="{&quot;wiki-doc&quot;:{&quot;user&quot;:{&quot;codeTheme&quot;:&quot;github&quot;}}}"><div class="wiki-p">Тело мысли</div></div></body>
                <body-original type="text/wiki">Тело мысли</body-original>
                <snippet>Тело мысли</snippet>
            </content>
            </entry>
        """)
        data = {
            '_title': 'Title мысли',
            'body_original': 'Тело мысли',
            '_snippet': 'Тело мысли',
            '_body': '<div class="wiki-doc i-bem" data-bem="{&quot;wiki-doc&quot;:{&quot;user&quot;:{&quot;codeTheme&quot;:&quot;github&quot;}}}"><div class="wiki-p">Тело мысли</div></div>'

        }
        self.assertIsSubDict(data, serializers.deserialize_xml_content(Text, xml))

    def test_content_type(self):
        """Should parse content type from xml"""
        xml = ET.XML("""
            <entry>
            <type>text</type>
            <edit_time>1440582470</edit_time>
            <published>1</published>
            <content>
                <title>Title мысли</title>
                <body><div class="wiki-doc i-bem" data-bem="{&quot;wiki-doc&quot;:{&quot;user&quot;:{&quot;codeTheme&quot;:&quot;github&quot;}}}"><div class="wiki-p">Тело мысли</div></div></body>
                <body-original type="text/wiki">Тело мысли</body-original>
                <snippet>Тело мысли</snippet>
            </content>
            </entry>
        """)
        self.assertIsSubDict(
            {'_content_type': "text/wiki"},
            serializers.deserialize_xml_content(Text, xml),
        )

    def test_entry_meta(self):
        """Should parse meta fields from xml"""
        xml = ET.XML("""
            <entry>
            <type>text</type>
            <edit_time>1440582470</edit_time>
            <meta>
                <address>ya-dev@yandex-team.ru</address>
                <uid>123456</uid>
            </meta>
            <published>1</published>
            <content>
                <title>Title мысли</title>
                <body><div class="wiki-doc i-bem" data-bem="{&quot;wiki-doc&quot;:{&quot;user&quot;:{&quot;codeTheme&quot;:&quot;github&quot;}}}"><div class="wiki-p">Тело мысли</div></div></body>
                <body-original type="text/wiki">Тело мысли</body-original>
                <snippet>Тело мысли</snippet>
            </content>
            </entry>
        """)
        self.assertIsSubDict(
            {
                'summon_address': 'ya-dev@yandex-team.ru',
                'summon_uid': 123456
            },
            serializers.deserialize_xml_content(Summon, xml),
        )

    def test_shared_post(self):
        xml = ET.XML("""
                    <entry>
                        <meta>
                            <shared_post_id item_no="85" feed_id="1120000000016603" comment_id="0"/>
                        </meta>
                    </entry>
        """)
        self.assertIsSubDict(
            {
                'shared_item_no': 85,
                'shared_feed_id': 1120000000016603,
                'shared_comment_id': 0
            },
            serializers.deserialize_xml_content(entries.models.Link, xml),
        )


class TestEntryClass(unittest.TestCase):

    class Formatter(object):
        def format_body(self, value):
            return "BODY:%s" % value

        def format_title(self, value):
            return "TITLE:%s" % value

    def setUp(self):
        importlib.reload(entries)

    def testSetBodyInvokesFormatter(self):
        """Should check that setting body attribute invokates formatter"""
        body_text = "Body Text"
        post = entries.models.Post(AI, 0, 0)
        post._formatter = self.Formatter()
        post.body = body_text
        self.assertEqual(post.body,
                          "BODY:%s" % body_text)

    @pytest.mark.skip
    def testWorksWithSpecialChars(self):
        try:
            post = entries.models.Post(AI, AI.uid, 0)
            post.body = 'aaa < & >'
            post.title = 'aaa < & >'
        except ET.XMLSyntaxError:
            raise AssertionError

    def test_bodies_default_values_used(self):
        entry = Text(AI, 0, 0)
        deserializer_data = {
            'title': 'I am title',
        }

        entry.update_fields(deserializer_data)

        self.assertEqual(entry._body, '')
        self.assertEqual(entry.body_original, '')

    def test_bodies_default_values_not_used(self):
        entry = Text(AI, 0, 0)
        deserializer_data = {
            'title': 'I am title',
            '_body': '<div>body</div>',
            'body_original': '<div>body-orig</div>',

        }

        entry.update_fields(deserializer_data)

        self.assertEqual(entry._body, '<div>body</div>')
        self.assertEqual(entry.body_original, '<div>body-orig</div>')
