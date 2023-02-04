import json
from functools import cached_property
from unittest import TestCase

import pytest
from lxml import etree
from lxml.etree import Element

from general_feed_generator.feed_builder import get_feed
from general_feed_generator.models.parsing import ParsedProfile


@pytest.mark.usefixtures("resources_class")
class TestFeedBuilder(TestCase):
    feed_parser: str
    parse_result: str

    @cached_property
    def get_feed(self) -> Element:
        profile = ParsedProfile(**json.loads(self.parse_result)[0])
        feed = get_feed(profile)
        self.assertIsNotNone(feed)
        return feed

    def test_get_feed(self) -> None:
        feed = self.get_feed
        root = etree.XML(self.feed_parser)
        schema = etree.XMLSchema(root)
        parser = etree.XMLParser(schema=schema)
        xml_str = etree.tostring(feed, encoding="utf-8", xml_declaration=True)
        root = etree.fromstring(xml_str, parser)
        self.assertEqual(root.xpath("count(//offer)"), 95)
