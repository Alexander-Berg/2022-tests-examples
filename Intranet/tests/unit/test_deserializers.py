# coding: utf-8
import pytest
from lxml import etree as ET

from tests import base

from at.aux_.entries import serializers
from at.aux_.entries.models import Post

pytestmark = pytest.mark.django_db


class TestDeserializers(base.BaseRepoTestCase):
    def test_xml_deserializer_parse_content_of_post(self):
        post_xml = """
<feed>
  <item>
    <id>
      <uid>1120000000023943</uid>
      <item_no>1000</item_no>
      <asString>1120000000023943.1000</asString>
    </id>
    <StoreTime>1577859320</StoreTime>
    <StoreTimeUsec>1577859320343986</StoreTimeUsec>
    <ItemTime>1580627781</ItemTime>
    <UpdateTime>1577859320</UpdateTime>
    <LastAuthorUID>0</LastAuthorUID>
    <LastCommentID>0</LastCommentID>
    <CommentCount>0</CommentCount>
    <Item-No>1000</Item-No>
    <AccessType>public</AccessType>
    <PostType>35</PostType>
    <item>
      <author>
        <uid>1120000000023943</uid>
      </author>
      <dont-send-comments>0</dont-send-comments>
      <block-comments>0</block-comments>
      <block-trackbacks>0</block-trackbacks>
      <tag-list>
        <tag id="0"/>
      </tag-list>
      <access>public</access>
      <entry>
        <content>
          <body>
            <div class="wiki-doc i-bem" data-bem="{&quot;wiki-doc&quot;:{&quot;user&quot;:{&quot;codeTheme&quot;:&quot;github&quot;}}}"><div class="wiki-p"><strong class="wiki-bold">Hello world</strong></div></div></body>
          <body-original type="text/wiki">**Hello world**</body-original>
        </content>
      </entry>
    </item>
  </item>
</feed>"""
        result = serializers.deserialize_xml_content(
            Post, ET.XML(post_xml).xpath('item/item/entry')[0])

        self.assertEqual(
            result['_body'].strip(),
            '''<div class="wiki-doc i-bem" data-bem="{&quot;wiki-doc&quot;:{&quot;user&quot;:{&quot;codeTheme&quot;:&quot;github&quot;}}}"><div class="wiki-p"><strong class="wiki-bold">Hello world</strong></div></div>''',
        )
        self.assertEqual(
            result['body_original'],
            '**Hello world**',
        )

