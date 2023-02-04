
from unittest import skipIf

from django.conf import settings
from lxml import etree
from ujson import loads

from wiki.pages.models import Access
from intranet.wiki.tests.wiki_tests.unit_unittest.actions.base import HttpOldActionTestCase


class MypagesTest(HttpOldActionTestCase):
    def test_no_smoke(self):
        response = self.get()
        html = response.content
        parsed_html = etree.HTML(html)
        mypages_divs = parsed_html.xpath('//*[@class="b-mypages__content"]')
        self.assertEqual(len(mypages_divs), 1)

    @skipIf(settings.IS_BUSINESS, 'wait for fixing for business')
    def test_restricted_pages_are_not_shown(self):
        # Init some other user with two pages: open and restricted
        user100 = self.get_or_create_user('user100')
        self.create_page(tag='Test2', authors_to_add=[user100])
        Access.objects.create(
            page=self.create_page(tag='Test3', authors_to_add=[user100]),
            is_owner=True,
        )

        # See {{mypages}} renders open page and doesn't render restricted one
        response = self.get({'user': 'user100'})
        self.assertEqual(response['content-type'], 'text/html')
        html = response.content
        self.assertIn(b'/test2', html)
        self.assertNotIn(b'/test3', html)

    @skipIf(settings.IS_BUSINESS, 'wait for fixing for business')
    def test_render_json(self):
        self.create_page(tag='First')
        response = self.get({'render_json': ''})
        self.assertEqual(response['content-type'], 'application/json')
        json = loads(response.content)
        self.assertEqual(json['order_by'], 'alphabetically')
        self.assertTrue(json['sort_by_letter'])
        self.assertFalse(json['sort_by_change'])
        self.assertFalse(json['sort_by_date'])
        self.assertEqual(json['login'], 'thasonic')
        split_entities = json['split_entities']
        self.assertEqual(len(split_entities), 2)
        self.assertEqual(split_entities[0]['key'], 'f')
        self.assertEqual(split_entities[1]['key'], 't')
        self.assertEqual(len(split_entities[0]['pages']), 1)
        self.assertEqual(split_entities[0]['pages'][0]['url'], '/first')
        self.assertEqual(split_entities[0]['pages'][0]['tag'], 'First')
        self.assertTrue('modified_at' in split_entities[0]['pages'][0])
        self.assertTrue('created_at' in split_entities[0]['pages'][0])
        self.assertEqual(len(split_entities[1]['pages']), 1)
        self.assertEqual(split_entities[1]['pages'][0]['url'], '/test')
        self.assertEqual(split_entities[1]['pages'][0]['tag'], 'Тест')

    @skipIf(settings.IS_BUSINESS, 'wait for fixing for business')
    def test_render_json_bychange_param(self):
        user100 = self.get_or_create_user('user100')
        self.create_page(tag='Test2', authors_to_add=[user100])

        response = self.get({'render_json': '', 'bychange': True, 'user': 'user100'})
        json = loads(response.content)
        self.assertEqual(json['login'], 'user100')
        self.assertFalse(json['sort_by_date'])
        self.assertTrue(json['sort_by_change'])
        self.assertEqual(json['order_by'], 'by modification date')
        self.assertFalse(json['sort_by_letter'])
        split_entities = json['split_entities']
        self.assertEqual(split_entities[0]['pages'][0]['url'], '/test2')
        self.assertEqual(split_entities[0]['pages'][0]['tag'], 'Test2')

    def test_render_json_bydate_param(self):
        response = self.get({'render_json': '', 'bydate': True})
        json = loads(response.content)
        self.assertTrue(json['sort_by_date'])
        self.assertFalse(json['sort_by_change'])
        self.assertEqual(json['order_by'], 'by creation date')
        self.assertFalse(json['sort_by_letter'])
