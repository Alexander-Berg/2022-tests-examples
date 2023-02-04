from django.conf import settings
from ujson import loads

from wiki import access as wiki_access
from wiki.pages.models import Page
from intranet.wiki.tests.wiki_tests.common.unittest_base import BaseApiTestCase


class APITreeClusterTest(BaseApiTestCase):
    def setUp(self):
        super(APITreeClusterTest, self).setUp()
        self.setUsers()
        self.client.login('thasonic')
        self.user = self.user_thasonic

        self.root_page = self.create_page(tag='Тест', title='test page title')
        self.page1 = self.create_page(tag='Тест/Тест1', title='1 Title')
        self.create_page(tag='Тест/Тест1/Тест11', title='11 title')
        self.create_page(tag='Тест/Тест1/Тест12', title='12 title')
        self.page2 = self.create_page(tag='Тест/Тест2', title='2 title')
        self.page3 = self.create_page(tag='Тест/Тест3', title='3 title')
        self.create_page(tag='Тест/Тест3/Тест31', title='31 title')
        self.grid_page = self.create_page(tag='Тест/Грид', page_type=Page.TYPES.GRID, title='grid page title')

        self.create_page(tag='Тест/Мисс1/Тест41', title='41 title')
        self.create_page(tag='Тест/Мисс1/Тест42', title='42 title')

        self.create_page(tag='Тест/Мисс2/Мисс21/Тест511', title='511 title')
        self.create_page(tag='Тест/Мисс2/Мисс21/Тест512', title='512 title')

        p1 = self.create_page(tag='Тест/Редир')
        p2 = self.create_page(tag='Соме/Отхер/Паге')

        p1.redirects_to = p2
        p1.save()

        self.page3.last_author = self.user_chapson
        self.page3.save()
        self.page3.authors.clear()
        self.page3.authors.add(self.user_chapson)

        wiki_access.set_access(self.page3, wiki_access.TYPES.OWNER, self.user_chapson)

    def test_no_mandatory_params(self):
        response = self.client.get(
            '{api_url}/{page_tag}/.treecluster'.format(api_url=self.api_url, page_tag=self.root_page.supertag)
        )

        self.assertEqual(response.status_code, 409)

    def test_non_existing_page(self):
        response = self.client.get(
            '{api_url}/{page_tag}/.treecluster?sort_order={sort_order}'.format(
                api_url=self.api_url, page_tag='unknown', sort_order='tag'
            )
        )

        self.assertEqual(response.status_code, 404)

    def test_get_all_pages(self):
        assert_queries = 52 if not settings.WIKI_CODE == 'wiki' else 10
        with self.assertNumQueries(assert_queries):
            response = self.client.get(
                '{api_url}/{page_tag}/.treecluster?sort_order={sort_order}&page_types={page_types}'.format(
                    api_url=self.api_url, page_tag=self.root_page.supertag, sort_order='tag', page_types='G,R,403,404'
                )
            )

        self.assertEqual(response.status_code, 200)
        data = loads(response.content)['data']
        self.assertEqual(len(data), 7)

        self.assertEqual(data[0]['tag'], 'test/miss1')
        self.assertEqual(data[0]['url'], '/test/miss1')
        self.assertEqual(data[0]['children_count'], 2)
        self.assertEqual(data[0]['page_type'], '404')

        self.assertEqual(data[1]['tag'], 'test/miss2')
        self.assertEqual(data[1]['url'], '/test/miss2')
        self.assertEqual(data[1]['children_count'], 1)
        self.assertEqual(data[1]['page_type'], '404')

        self.assertEqual(data[2]['tag'], 'Тест/Грид')
        self.assertEqual(data[2]['url'], '/test/grid')
        self.assertEqual(data[2]['children_count'], 0)
        self.assertEqual(data[2]['page_type'], 'G')
        self.assertEqual(data[2]['title'], 'grid page title')

        self.assertEqual(data[3]['tag'], 'Тест/Редир')
        self.assertEqual(data[3]['url'], '/test/redir')
        self.assertEqual(data[3]['children_count'], 0)
        self.assertEqual(data[3]['page_type'], 'R')

        self.assertEqual(data[4]['tag'], 'Тест/Тест1')
        self.assertEqual(data[4]['url'], '/test/test1')
        self.assertEqual(data[4]['children_count'], 2)
        self.assertEqual(data[4]['page_type'], 'P')

        self.assertEqual(data[5]['tag'], 'Тест/Тест2')
        self.assertEqual(data[5]['url'], '/test/test2')
        self.assertEqual(data[5]['children_count'], 0)
        self.assertEqual(data[5]['page_type'], 'P')

        self.assertEqual(data[6]['tag'], 'Тест/Тест3')
        self.assertEqual(data[6]['url'], '/test/test3')
        self.assertEqual(data[6]['children_count'], 1)
        self.assertEqual(data[6]['page_type'], '403')

    def test_sorting_by_title(self):
        response = self.client.get(
            '{api_url}/{page_tag}/.treecluster?sort_order={sort_order}&page_types={page_types}'.format(
                api_url=self.api_url, page_tag=self.root_page.supertag, sort_order='title', page_types='G,R,403,404'
            )
        )

        self.assertEqual(response.status_code, 200)
        data = loads(response.content)['data']
        self.assertEqual(data[0]['title'], None)
        self.assertEqual(data[3]['title'], '1 Title')

    def test_sorting_by_created_at(self):
        response = self.client.get(
            '{api_url}/{page_tag}/.treecluster?sort_order={sort_order}&page_types={page_types}'.format(
                api_url=self.api_url,
                page_tag=self.root_page.supertag,
                sort_order='created_at',
                page_types='G,R,403,404',
            )
        )

        self.assertEqual(response.status_code, 200)
        data = loads(response.content)['data']
        self.assertEqual(data[-1]['created_at'], None)
        self.assertNotEqual(data[0]['created_at'], None)

    def test_sorting_by_modified_at(self):
        response = self.client.get(
            '{api_url}/{page_tag}/.treecluster?sort_order={sort_order}&page_types={page_types}'.format(
                api_url=self.api_url,
                page_tag=self.root_page.supertag,
                sort_order='modified_at',
                page_types='G,R,403,404',
            )
        )

        self.assertEqual(response.status_code, 200)
        data = loads(response.content)['data']
        self.assertEqual(data[-1]['modified_at'], None)
        self.assertNotEqual(data[0]['modified_at'], None)

    def test_returned_types_empty(self):
        response = self.client.get(
            '{api_url}/{page_tag}/.treecluster?sort_order={sort_order}'.format(
                api_url=self.api_url, page_tag=self.root_page.supertag, sort_order='tag'
            )
        )

        self.assertEqual(response.status_code, 200)
        data = loads(response.content)['data']
        self.assertEqual(len(data), 2)
        self.assertEqual(data[0]['tag'], 'Тест/Тест1')
        self.assertEqual(data[1]['tag'], 'Тест/Тест2')

    def test_returned_types_g_403(self):
        response = self.client.get(
            '{api_url}/{page_tag}/.treecluster?sort_order={sort_order}&page_types={page_types}'.format(
                api_url=self.api_url, page_tag=self.root_page.supertag, sort_order='tag', page_types='G,403'
            )
        )

        self.assertEqual(response.status_code, 200)
        data = loads(response.content)['data']
        self.assertEqual(len(data), 4)
        self.assertEqual(data[0]['tag'], 'Тест/Грид')
        self.assertEqual(data[1]['tag'], 'Тест/Тест1')
        self.assertEqual(data[2]['tag'], 'Тест/Тест2')
        self.assertEqual(data[3]['tag'], 'Тест/Тест3')

    def test_returned_types_r_404(self):
        response = self.client.get(
            '{api_url}/{page_tag}/.treecluster?sort_order={sort_order}&page_types={page_types}'.format(
                api_url=self.api_url, page_tag=self.root_page.supertag, sort_order='tag', page_types='R,404'
            )
        )

        self.assertEqual(response.status_code, 200)
        data = loads(response.content)['data']
        self.assertEqual(len(data), 5)
        self.assertEqual(data[0]['tag'], 'test/miss1')
        self.assertEqual(data[1]['tag'], 'test/miss2')
        self.assertEqual(data[2]['tag'], 'Тест/Редир')
        self.assertEqual(data[3]['tag'], 'Тест/Тест1')
        self.assertEqual(data[4]['tag'], 'Тест/Тест2')

    def test_filtering(self):
        """Тест фильтрации данных - фикс баги WIKI-6995"""
        self.create_page(tag='Тест/Грид/паге', title='child of grid page')

        response = self.client.get(
            '{api_url}/{page_tag}/.treecluster?sort_order={sort_order}&page_types={page_types}'.format(
                api_url=self.api_url, page_tag=self.root_page.supertag, sort_order='tag', page_types='R,404'
            )
        )

        self.assertEqual(response.status_code, 200)
        data = loads(response.content)['data']
        self.assertEqual(len(data), 5)
        for page in data:
            self.assertNotEqual(page['tag'], self.grid_page.tag)
