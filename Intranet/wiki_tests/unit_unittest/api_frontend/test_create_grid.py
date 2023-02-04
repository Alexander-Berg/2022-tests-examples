
from rest_framework import status
from ujson import loads

from wiki.notifications.models import PageEvent
from wiki.pages.models import Page, PageWatch
from intranet.wiki.tests.wiki_tests.common.unittest_base import BaseApiTestCase, now_for_tests


class APICreateGridHandlerTest(BaseApiTestCase):
    """
    Tests for pages api handlers
    """

    def setUp(self):
        super(APICreateGridHandlerTest, self).setUp()
        self.setUsers()
        self.client.login('thasonic')
        self.user = self.user_thasonic

    def _test_success(self, tag, url=None, inherited_watchers=tuple()):
        title = 'тест тайтл'
        momentBefore = now_for_tests()

        if url is None:
            url = '/' + tag
        request_url = '{api_url}/{page_tag}/.grid/create'.format(api_url=self.api_url, page_tag=tag)
        response = self.client.post(request_url, data={'title': '  ' + title + '  '})

        # проверка ответа
        self.assertEqual(200, response.status_code)
        data = loads(response.content)['data']
        self.assertEqual(data['page_type'], 'grid')
        self.assertEqual(data['tag'], tag)
        self.assertEqual(data['url'], url)

        # проверка созданного грида
        grid = Page.active.get(tag=tag)
        momentAfter = now_for_tests()
        self.assertEqual(grid.page_type, Page.TYPES.GRID)
        self.assertTrue(self.user in grid.get_authors())
        self.assertEqual(grid.last_author, self.user)
        self.assertEqual(grid.title, title)
        self.assertTrue(momentBefore <= grid.created_at <= momentAfter)

        # проверка наблюдателей
        watches = list(PageWatch.objects.filter(page=grid))
        self.assertEqual(len(watches), 1 + len(inherited_watchers))
        actual_watchers = tuple()
        for watch in watches:
            actual_watchers += (watch.user,)
            self.assertTrue(watch.is_cluster)
            self.assertTrue(momentBefore <= watch.created_at <= momentAfter)
        expected_watchers = (self.user.username,) + inherited_watchers
        self.assertEqual(set(expected_watchers), set(actual_watchers))

        # проверка нотификации
        event = PageEvent.objects.get(page=grid)
        self.assertEqual(event.author, self.user)
        self.assertEqual(event.event_type, PageEvent.EVENT_TYPES.create)
        self.assertTrue(momentBefore <= event.created_at <= momentAfter)

        # проверка, что получение грида работает
        request_url = '{api_url}{page_url}'.format(api_url=self.api_url, page_url=url)
        response = self.client.get(request_url)
        self.assertEqual(200, response.status_code)
        data = loads(response.content)['data']
        self.assertEqual(data['title'], title)
        self.assertEqual(data['page_type'], 'grid')
        self.assertEqual(data['tag'], tag)
        self.assertEqual(data['url'], url)

    def test_success_root(self):
        """
        Успешное создание на верхнем уровне (в смысле тэга).
        """
        self._test_success(tag='ТестГрид', url='/testgrid')

    def test_success_child(self):
        """
        Успешное создание не на верхнем уровне (в смысле тэга).
        """

        # родительская страница есть, наследуемых наблюдателей нет
        self.create_page(tag='РоотТаг', body='body')
        self._test_success('РоотТаг/ТестГрид', url='/roottag/testgrid')

        # родительская страница есть, наследуемые наблюдатели есть
        page = self.create_page(tag='roottag2', authors_to_add=[self.user_kolomeetz])
        PageWatch(user=self.user_kolomeetz.username, page=page, is_cluster=True).save()
        self._test_success(tag='roottag2/testgrid', inherited_watchers=(self.user_kolomeetz.username,))

    def _test_invalid(self, tag, data):
        request_url = '{api_url}/{page_supertag}/.grid/create'.format(api_url=self.api_url, page_supertag=tag)
        response = self.client.post(request_url, data=data)

        # проверка ответа
        self.assertEqual(status.HTTP_409_CONFLICT, response.status_code)
        return loads(response.content)['error']

    def _test_invalid_title(self, tag, data):
        error_data = self._test_invalid(tag, data)
        self.assertEqual(error_data['error_code'], 'CLIENT_SENT_INVALID_DATA')
        self.assertTrue(len(error_data['errors']['title']) > 0)

    def test_invalid(self):
        tag = 'testgrid'
        self._test_invalid_title(tag, {})
        self._test_invalid_title(tag, {'title': ''})
        self._test_invalid_title(tag, {'title': '  '})

        # проверка отсутствия грида
        self.assertFalse(Page.active.filter(supertag=tag).exists())

    def test_already_exists(self):
        tag = 'testgrid'
        self.create_page(tag=tag)
        request_url = '{api_url}/{page_supertag}/.grid/create'.format(api_url=self.api_url, page_supertag=tag)
        response = self.client.post(request_url, data={'title': 'тайтл'})

        # проверка ответа
        self.assertEqual(status.HTTP_409_CONFLICT, response.status_code)
        error = loads(response.content)['error']
        self.assertEqual(error['error_code'], 'ALREADY_EXISTS')

    def test_cant_create_grid_over_page(self):
        supertag = 'super/tag'

        page_data = {'title': 'Title', 'body': 'Body'}

        request_url = '{api_url}/{page_supertag}'.format(api_url=self.api_url, page_supertag=supertag)
        response = self.client.post(request_url, data=page_data)
        self.assertEqual(200, response.status_code)

        request_url = '{api_url}/{page_supertag}/.grid/create'.format(api_url=self.api_url, page_supertag=supertag)
        response = self.client.post(request_url, data={'title': 'Test'})
        self.assertEqual(response.status_code, 409)

        # Страница не изменилась
        request_url = '{api_url}/{page_supertag}/.raw'.format(api_url=self.api_url, page_supertag=supertag)
        response = self.client.get(request_url)
        self.assertEqual(200, response.status_code)
        data = loads(response.content)['data']
        self.assertEqual(data['body'], 'Body')
