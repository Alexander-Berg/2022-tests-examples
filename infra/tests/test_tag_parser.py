import unittest

from infra.cores.app import tag_parser


class TestTagParser(unittest.TestCase):
    def test_ride_tag(self):
        r = tag_parser.tags_parser('ride:123')
        result_url = '{}/{}'.format(tag_parser.SDC_WWW_RIDES, 123)
        self.assertEqual(r.text, result_url)
        self.assertEqual(r.tag_name, 'ride')
        self.assertEqual(r.tag_type, 'url')
        self.assertEqual(r.title, '123')

    def test_simple_tag(self):
        tag = 'simple_tag'
        self.assertEqual(tag_parser.tags_parser(tag).text, tag)

    def test_empty_tags(self):
        r = tag_parser.get_rendered_tags('')
        self.assertEqual(r, [])
        r = tag_parser.get_rendered_tags(None)
        self.assertEqual(r, [])

    def test_space_split_tags(self):
        r = tag_parser.get_rendered_tags('one two three')
        self.assertEqual(r[0].text, 'one two three')

    def test_filled_structure_k_v(self):
        r = tag_parser.tags_parser('k_v:tag')

        self.assertEqual(r.text, 'tag')
        self.assertEqual(r.tag_name, 'k_v')
        self.assertEqual(r.tag_type, 'plain-text')
        self.assertEqual(r.title, 'tag')

    def test_filled_structure_simple(self):
        r = tag_parser.tags_parser('simple_tag')

        self.assertEqual(r.text, 'simple_tag')
        self.assertEqual(r.title, 'simple_tag')
        self.assertEqual(r.tag_type, 'plain-text')
        self.assertEqual(r.tag_name, 'tag_name')

    def test_url_tag(self):
        r0 = tag_parser.tags_parser('named_link:https://ya.ru:YaRu')

        self.assertEqual(r0.tag_name, 'named_link')
        self.assertEqual(r0.title, 'YaRu')
        self.assertEqual(r0.text, 'https://ya.ru')
        self.assertEqual(r0.tag_type, 'url')

        r1 = tag_parser.tags_parser('named_link:http://domain.ru/:id/:/file-stderr.log:stderr')
        self.assertEqual(r1.text, 'http://domain.ru/:id/:/file-stderr.log')
        self.assertEqual(r1.title, 'stderr')
        self.assertEqual(r1.tag_name, 'named_link')
