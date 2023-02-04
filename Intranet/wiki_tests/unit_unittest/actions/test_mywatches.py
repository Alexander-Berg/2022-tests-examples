from lxml import etree
from ujson import loads

from intranet.wiki.tests.wiki_tests.unit_unittest.actions.base import HttpOldActionTestCase


class MywatchesTest(HttpOldActionTestCase):
    def test_no_smoke(self):
        page = self.create_page(tag='МоиВотчи/Тест1')
        page.pagewatch_set.create(user=self.request.user.username, is_cluster=False)

        response = self.get()
        self.assertEqual(response['content-type'], 'text/html')
        html = response.content
        parsed_html = etree.HTML(html)
        mypages_divs = parsed_html.xpath('//*[@class="b-mywatches__content"]')
        self.assertEqual(len(mypages_divs), 1)
        page_link = parsed_html.xpath('//*[@href="/moivotchi/test1"]')
        self.assertEqual(len(page_link), 1)
        self.assertContains(response, 'МоиВотчи/Тест1')

    def test_render_json(self):
        page = self.create_page(tag='Test1')
        page.pagewatch_set.create(user=self.request.user.username, is_cluster=False)

        page2 = self.create_page(tag='Test2')
        page2.pagewatch_set.create(user=self.request.user.username, is_cluster=False)

        response = self.get({'render_json': ''})
        self.assertEqual(response['content-type'], 'application/json')
        json = loads(response.content)
        self.assertEqual(len(json), 2)
        self.assertEqual(json[0]['url'], '/test1')
        self.assertEqual(json[0]['tag'], 'Test1')
        self.assertEqual(json[1]['url'], '/test2')
        self.assertEqual(json[1]['tag'], 'Test2')
