
from django.conf import settings
from ujson import loads

from intranet.wiki.tests.wiki_tests.common.unittest_base import BaseApiTestCase


class DebugTestCase(BaseApiTestCase):
    """
    Тесты на дебаговую информацию о странице и вьюхе
    (заодно тестируется и правильность определения страниц и вьюх).
    """

    def setUp(self):
        super(DebugTestCase, self).setUp()
        self.setUsers()
        self.client.login('thasonic')

    def test_correct_existing_page_info(self):
        page = self.create_page(
            tag='Страница',
            body='page test',
        )
        r = self.client.get('{api_url}/Страница'.format(api_url=self.api_url))

        json = loads(r.content)
        self.assertEqual(r.status_code, 200)
        self.assertTrue('debug' in json)
        self.assertEqual(json['debug']['page'], page.supertag)
        self.assertEqual(json['debug']['view'], 'wiki.api_frontend.views.pages.PageView')

        r = self.client.get('{api_url}/Страница/'.format(api_url=self.api_url))

        json = loads(r.content)
        self.assertEqual(r.status_code, 200)
        self.assertTrue('debug' in json)
        self.assertEqual(json['debug']['page'], page.supertag)
        self.assertEqual(json['debug']['view'], 'wiki.api_frontend.views.pages.PageView')

        r = self.client.get('{api_url}/Страница/.files'.format(api_url=self.api_url))

        json = loads(r.content)
        self.assertEqual(r.status_code, 200)
        self.assertTrue('debug' in json)
        self.assertEqual(json['debug']['page'], page.supertag)
        self.assertEqual(json['debug']['view'], 'wiki.api_frontend.views.files.FilesListView')

    def test_correct_nonexistent_page_info(self):
        r = self.client.get('{api_url}/Страница'.format(api_url=self.api_url))

        json = loads(r.content)
        self.assertEqual(r.status_code, 404)
        self.assertTrue('debug' in json)
        self.assertEqual(json['debug']['page'], None)
        self.assertEqual(json['debug']['view'], 'wiki.api_frontend.views.pages.PageView')

    def test_correct_not_a_page_info(self):
        # Не должно матчиться на страницу, поскольку "!" не может быть в теге
        r = self.client.get('{api_url}/!'.format(api_url=self.api_url))

        json = loads(r.content)
        self.assertEqual(r.status_code, 404)
        self.assertTrue('debug' in json)
        self.assertEqual(json['debug']['page'], None)
        self.assertEqual(json['debug']['view'], 'wiki.api_core.framework.NoSuchPageView')

    def test_correct_homepage_info(self):
        page = self.create_page(
            tag=settings.MAIN_PAGE,
            body='main page',
        )
        r = self.client.get('{api_url}/'.format(api_url=self.api_url))

        json = loads(r.content)
        self.assertEqual(r.status_code, 200)
        self.assertTrue('debug' in json)
        self.assertEqual(json['debug']['page'], page.supertag)
        self.assertEqual(json['debug']['view'], 'wiki.api_frontend.views.pages.PageView')

        # Такой адрес может быть у безстраничного хендлера, страница (homepage) не должна определяться в таком случае.
        r = self.client.get('{api_url}/.autocomplete?tag=abc'.format(api_url=self.api_url))

        json = loads(r.content)
        self.assertEqual(r.status_code, 200)
        self.assertTrue('debug' in json)
        self.assertEqual(json['debug']['page'], None)
        self.assertEqual(json['debug']['view'], 'wiki.api_frontend.views.autocomplete.PageSupertagAutocompleteView')

    def test_correct_nonexistent_view_info(self):
        page = self.create_page(
            tag='Страница',
            body='main page',
        )
        r = self.client.get('{api_url}/Страница/.wat'.format(api_url=self.api_url))

        json = loads(r.content)
        self.assertEqual(r.status_code, 404)
        self.assertTrue('debug' in json)
        self.assertEqual(json['debug']['page'], page.supertag)
        self.assertEqual(json['debug']['view'], 'wiki.api_core.framework.NoSuchPageView')

    def test_use_nodejs_frontend(self):
        self.create_page(
            tag='page',
            supertag='page',
            body='main page',
        )
        response = self.client.get('/_api/frontend/page/')
        self.assertEqual(response.status_code, 200)
        json = loads(response.content)
        self.assertTrue('use_nodejs_frontend' in json['user']['settings'])
