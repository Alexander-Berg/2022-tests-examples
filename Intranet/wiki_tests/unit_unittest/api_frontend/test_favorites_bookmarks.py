from django.conf import settings
from django.test import override_settings
from ujson import loads

from wiki.favorites_v2.models import Folder
from wiki.favorites_v2.tasks.update_autofolders import update_autofolders

from intranet.wiki.tests.wiki_tests.common.unittest_base import BaseApiTestCase


class BaseBookmarksTest(BaseApiTestCase):
    def _create_folder(self, name, type):
        resp = self.client.put('{api_url}/.favorites/folders/{name}'.format(api_url=self.api_url, name=name), {})
        self.assertEqual(200, resp.status_code)

        data = loads(resp.content)['data']
        self.assertEqual(name, data['name'])
        self.assertEqual(type, data['type'])
        self.assertEqual(0, data['favorites_count'])

    def _create_page(self, tag):
        return self.create_page(
            tag=tag, body='page test', title=tag, authors_to_add=[self.user_thasonic], last_author=self.user_thasonic
        )

    def _create_bookmark(self, title, tag, folder_name):
        page = self._create_page(tag)

        resp = self.client.put(
            '{api_url}/.favorites/bookmarks'.format(api_url=self.api_url),
            {'folder_name': folder_name, 'title': title, 'url': page.absolute_url},
        )
        self.assertEqual(200, resp.status_code)
        data = loads(resp.content)['data']
        self.assertEqual(data['url'], page.absolute_url)
        self.assertEqual(data['page_last_editor'], self.user_thasonic.username)
        self.assertTrue(data['page_modified_at'])
        self.assertTrue(data['id'])

        return resp


class APIChangeOrderTest(BaseBookmarksTest):
    def setUp(self):
        super().setUp()
        self.setUsers(use_legacy_subscr_favor=True)
        self.client.login('thasonic')
        self.user = self.user_thasonic

    def test_change_bookmark_order(self):
        resp = self._create_bookmark('первая закладка', 'Страница 1', Folder.FAVORITES_FOLDER_NAME)
        id1 = loads(resp.content)['data']['id']
        resp = self._create_bookmark('вторая закладка', 'Страница 2', Folder.FAVORITES_FOLDER_NAME)
        id2 = loads(resp.content)['data']['id']
        resp = self._create_bookmark('третья закладка', 'Страница 3', Folder.FAVORITES_FOLDER_NAME)
        id3 = loads(resp.content)['data']['id']
        self._create_bookmark('четвертая закладка', 'Страница 4', Folder.FAVORITES_FOLDER_NAME)
        resp = self._create_bookmark('пятая закладка', 'Страница 5', Folder.FAVORITES_FOLDER_NAME)
        id5 = loads(resp.content)['data']['id']

        assert_queries = 11 if not settings.WIKI_CODE == 'wiki' else 9
        with self.assertNumQueries(assert_queries):
            # переместить закладку 'вторая закладка' на место после u'пятая закладка'
            resp = self.client.post(
                '{api_url}/.favorites/bookmarks/{id}/drag'.format(api_url=self.api_url, id=id2), {'after': id5}
            )
            self.assertEqual(200, resp.status_code)

        resp = self.client.get(
            '{api_url}/.favorites/folders/{folder_name}'.format(
                api_url=self.api_url, folder_name=Folder.FAVORITES_FOLDER_NAME
            )
        )
        self.assertEqual(200, resp.status_code)
        data = loads(resp.content)['data']
        self.assertEqual(5, len(data))
        self.assertEqual('пятая закладка', data[0]['title'])
        self.assertEqual('вторая закладка', data[1]['title'])
        self.assertEqual('четвертая закладка', data[2]['title'])
        self.assertEqual('третья закладка', data[3]['title'])
        self.assertEqual('первая закладка', data[4]['title'])

        # переместить закладку 'вторая закладка' на первое место
        resp = self.client.post('{api_url}/.favorites/bookmarks/{id}/drag'.format(api_url=self.api_url, id=id2), {})
        self.assertEqual(200, resp.status_code)

        resp = self.client.get(
            '{api_url}/.favorites/folders/{folder_name}'.format(
                api_url=self.api_url, folder_name=Folder.FAVORITES_FOLDER_NAME
            )
        )
        self.assertEqual(200, resp.status_code)
        data = loads(resp.content)['data']
        self.assertEqual(5, len(data))
        self.assertEqual('вторая закладка', data[0]['title'])
        self.assertEqual('пятая закладка', data[1]['title'])
        self.assertEqual('четвертая закладка', data[2]['title'])
        self.assertEqual('третья закладка', data[3]['title'])
        self.assertEqual('первая закладка', data[4]['title'])

        # переместить закладку 'пятая закладка' на место после 'третья закладка'
        resp = self.client.post(
            '{api_url}/.favorites/bookmarks/{id}/drag'.format(api_url=self.api_url, id=id5), {'after': id3}
        )
        self.assertEqual(200, resp.status_code)

        resp = self.client.get(
            '{api_url}/.favorites/folders/{folder_name}'.format(
                api_url=self.api_url, folder_name=Folder.FAVORITES_FOLDER_NAME
            )
        )
        self.assertEqual(200, resp.status_code)
        data = loads(resp.content)['data']
        self.assertEqual(5, len(data))
        self.assertEqual('вторая закладка', data[0]['title'])
        self.assertEqual('четвертая закладка', data[1]['title'])
        self.assertEqual('третья закладка', data[2]['title'])
        self.assertEqual('пятая закладка', data[3]['title'])
        self.assertEqual('первая закладка', data[4]['title'])

        # переместить закладку 'вторая закладка' на последнее место
        resp = self.client.post(
            '{api_url}/.favorites/bookmarks/{id}/drag'.format(api_url=self.api_url, id=id2), {'after': id1}
        )
        self.assertEqual(200, resp.status_code)

        resp = self.client.get(
            '{api_url}/.favorites/folders/{folder_name}'.format(
                api_url=self.api_url, folder_name=Folder.FAVORITES_FOLDER_NAME
            )
        )
        self.assertEqual(200, resp.status_code)
        data = loads(resp.content)['data']
        self.assertEqual(5, len(data))
        self.assertEqual('четвертая закладка', data[0]['title'])
        self.assertEqual('третья закладка', data[1]['title'])
        self.assertEqual('пятая закладка', data[2]['title'])
        self.assertEqual('первая закладка', data[3]['title'])
        self.assertEqual('вторая закладка', data[4]['title'])

    def test_change_bookmark_order_for_unknown_id(self):
        resp = self._create_bookmark('первая закладка', 'Страница 1', Folder.FAVORITES_FOLDER_NAME)
        id1 = loads(resp.content)['data']['id']

        resp = self.client.post(
            '{api_url}/.favorites/bookmarks/{id}/drag'.format(api_url=self.api_url, id=10101), {'after': id1}
        )
        self.assertEqual(404, resp.status_code)

    def test_change_bookmark_order_after_unknown_id(self):
        resp = self._create_bookmark('первая закладка', 'Страница 1', Folder.FAVORITES_FOLDER_NAME)
        id1 = loads(resp.content)['data']['id']

        resp = self.client.post(
            '{api_url}/.favorites/bookmarks/{id}/drag'.format(api_url=self.api_url, id=id1), {'after': 10101}
        )
        self.assertEqual(404, resp.status_code)

    def test_change_bookmark_order_after_the_same_bookmark(self):
        resp = self._create_bookmark('первая закладка', 'Страница 1', Folder.FAVORITES_FOLDER_NAME)
        id1 = loads(resp.content)['data']['id']

        resp = self.client.post(
            '{api_url}/.favorites/bookmarks/{id}/drag'.format(api_url=self.api_url, id=id1), {'after': id1}
        )
        self.assertEqual(409, resp.status_code)

    def test_change_bookmark_order_after_bookmark_from_other_folder(self):
        resp = self._create_bookmark('первая закладка', 'Страница 1', Folder.FAVORITES_FOLDER_NAME)
        id1 = loads(resp.content)['data']['id']

        self._create_folder('Другая папка', Folder.FOLDER_TYPE_CUSTOM)
        resp = self._create_bookmark('вторая закладка', 'Страница 2', 'Другая папка')
        id2 = loads(resp.content)['data']['id']

        resp = self.client.post(
            '{api_url}/.favorites/bookmarks/{id}/drag'.format(api_url=self.api_url, id=id1), {'after': id2}
        )
        self.assertEqual(409, resp.status_code)

    def test_edit_bookmark(self):
        resp = self._create_bookmark('моя первая закладка', 'НоваяСтраница', Folder.FAVORITES_FOLDER_NAME)
        data = loads(resp.content)['data']
        id = data['id']
        url = data['url']

        assert_queries = 6 if not settings.WIKI_CODE == 'wiki' else 4
        with self.assertNumQueries(assert_queries):
            resp = self.client.post(
                '{api_url}/.favorites/bookmarks/{id}'.format(api_url=self.api_url, id=id), {'title': 'новое название'}
            )

        self.assertEqual(200, resp.status_code)
        data = loads(resp.content)['data']
        self.assertEqual(data['url'], url)
        self.assertEqual(data['page_last_editor'], self.user_thasonic.username)
        self.assertTrue(data['page_modified_at'])
        id = data['id']
        self.assertTrue(id)
        self.assertEqual(data['title'], 'новое название')

        resp = self.client.get(
            '{api_url}/.favorites/folders/{folder_name}'.format(
                api_url=self.api_url, folder_name=Folder.FAVORITES_FOLDER_NAME
            )
        )
        self.assertEqual(200, resp.status_code)
        data = loads(resp.content)['data']
        self.assertEqual(1, len(data))
        self.assertEqual('новое название', data[0]['title'])

    def test_edit_unknown_bookmark(self):
        resp = self.client.post('{api_url}/.favorites/bookmarks/{id}'.format(api_url=self.api_url, id=10101), {})
        self.assertEqual(404, resp.status_code)

    def test_edit_bookmark_without_required_parameter(self):
        resp = self._create_bookmark('моя первая закладка', 'НоваяСтраница', Folder.FAVORITES_FOLDER_NAME)
        id = loads(resp.content)['data']['id']

        resp = self.client.post('{api_url}/.favorites/bookmarks/{id}'.format(api_url=self.api_url, id=id), {})

        self.assertEqual(409, resp.status_code)


class APIBookmarksTest(BaseBookmarksTest):
    def setUp(self):
        super().setUp()
        self.setUsers()
        self.user_thasonic.profile['new_favorites'] = True
        self.user_thasonic.save()
        self.client.login('thasonic')
        self.user = self.user_thasonic

    def test_create_bookmark(self):
        with self.assertNumQueries(33 if settings.IS_INTRANET else 69):
            self._create_bookmark('моя первая закладка', 'НоваяСтраница', Folder.FAVORITES_FOLDER_NAME)

        resp = self.client.get('{api_url}/.favorites/folders'.format(api_url=self.api_url))
        self.assertEqual(200, resp.status_code)
        data = loads(resp.content)['data']
        self.assertEqual(Folder.FAVORITES_FOLDER_NAME, data[0]['name'])
        self.assertEqual(Folder.FOLDER_TYPE_FAVORITES, data[0]['type'])
        self.assertEqual(1, data[0]['favorites_count'])

        # добавим еще одну закладку в Избранное
        self._create_bookmark('вторая закладка', 'другая страница', Folder.FAVORITES_FOLDER_NAME)

        resp = self.client.get('{api_url}/.favorites/folders'.format(api_url=self.api_url))
        self.assertEqual(200, resp.status_code)
        data = loads(resp.content)['data']
        self.assertEqual(Folder.FAVORITES_FOLDER_NAME, data[0]['name'])
        self.assertEqual(Folder.FOLDER_TYPE_FAVORITES, data[0]['type'])
        self.assertEqual(2, data[0]['favorites_count'])

        # получить список закладок и проверить порядок
        request_url = '{api_url}/.favorites/folders/{folder_name}'.format(
            api_url=self.api_url, folder_name=Folder.FAVORITES_FOLDER_NAME
        )
        resp = self.client.get(request_url)
        self.assertEqual(200, resp.status_code)
        data = loads(resp.content)['data']
        self.assertEqual(2, len(data))
        self.assertEqual(data[0]['title'], 'другая страница')
        self.assertEqual(data[1]['title'], 'НоваяСтраница')

    def test_create_bookmark_twice(self):
        title = 'моя первая закладка'
        tag = 'НоваяСтраница'
        with self.assertNumQueries(33 if settings.IS_INTRANET else 69):
            self._create_bookmark(title, tag, Folder.FAVORITES_FOLDER_NAME)

        resp = self.client.get('{api_url}/.favorites/folders'.format(api_url=self.api_url))
        self.assertEqual(200, resp.status_code)
        data = loads(resp.content)['data']
        self.assertEqual(Folder.FAVORITES_FOLDER_NAME, data[0]['name'])
        self.assertEqual(Folder.FOLDER_TYPE_FAVORITES, data[0]['type'])
        self.assertEqual(1, data[0]['favorites_count'])

        # пробуем добавить закладку еще раз
        page = self._create_page(tag)
        resp = self.client.put(
            '{api_url}/.favorites/bookmarks'.format(api_url=self.api_url),
            {'folder_name': Folder.FAVORITES_FOLDER_NAME, 'title': title, 'url': page.absolute_url},
        )

        self.assertEqual(409, resp.status_code)

    def test_create_bookmark_in_custom_folder(self):
        custoom_folder_name = 'Моя папка'
        assert_queries = 48 if not settings.WIKI_CODE == 'wiki' else 6
        with self.assertNumQueries(assert_queries):
            self._create_folder(custoom_folder_name, Folder.FOLDER_TYPE_CUSTOM)

        self._create_bookmark('моя первая закладка', 'НоваяСтраница', custoom_folder_name)

        # добавим еще одну закладку в папку "Моя папка"
        self._create_bookmark('вторая закладка', 'другая страница', custoom_folder_name)

        resp = self.client.get('{api_url}/.favorites/folders'.format(api_url=self.api_url))
        self.assertEqual(200, resp.status_code)
        data = loads(resp.content)['data']
        self.assertEqual(custoom_folder_name, data[1]['name'])
        self.assertEqual(Folder.FOLDER_TYPE_CUSTOM, data[1]['type'])
        self.assertEqual(2, data[1]['favorites_count'])

        # получить список закладок и проверить порядок
        request_url = '{api_url}/.favorites/folders/{folder_name}'.format(
            api_url=self.api_url, folder_name=custoom_folder_name
        )
        resp = self.client.get(request_url)
        self.assertEqual(200, resp.status_code)
        data = loads(resp.content)['data']
        self.assertEqual(2, len(data))
        self.assertEqual(data[0]['title'], 'другая страница')
        self.assertEqual(data[1]['title'], 'НоваяСтраница')

    def test_create_bookmark_in_autofolder(self):
        page = self.create_page(
            tag='НоваяСтраница', body='page test', authors_to_add=[self.user_thasonic], last_author=self.user_thasonic
        )

        resp = self.client.put(
            '{api_url}/.favorites/bookmarks'.format(api_url=self.api_url),
            {'folder_name': Folder.OWNER_AUTOFOLDER_NAME, 'title': 'Закладка 1', 'url': page.absolute_url},
        )

        self.assertEqual(409, resp.status_code)

    def test_create_bookmark_without_required_parameters(self):
        self.create_page(
            tag='НоваяСтраница', body='page test', authors_to_add=[self.user_thasonic], last_author=self.user_thasonic
        )

        resp = self.client.put('{api_url}/.favorites/bookmarks'.format(api_url=self.api_url), {})
        self.assertEqual(409, resp.status_code)

    def test_delete_bookmark(self):
        resp = self._create_bookmark('моя первая закладка', 'НоваяСтраница', Folder.FAVORITES_FOLDER_NAME)
        id = loads(resp.content)['data']['id']

        resp = self.client.get('{api_url}/.favorites/folders'.format(api_url=self.api_url))
        self.assertEqual(200, resp.status_code)
        data = loads(resp.content)['data']
        self.assertEqual(1, data[0]['favorites_count'])

        resp = self.client.get(
            '{api_url}/.favorites/folders/{folder_name}'.format(
                api_url=self.api_url, folder_name=Folder.FAVORITES_FOLDER_NAME
            )
        )
        self.assertEqual(200, resp.status_code)
        data = loads(resp.content)['data']
        self.assertEqual(1, len(data))
        self.assertEqual('НоваяСтраница', data[0]['title'])

        assert_queries = 9 if not settings.WIKI_CODE == 'wiki' else 7
        with self.assertNumQueries(assert_queries):
            resp = self.client.delete('{api_url}/.favorites/bookmarks/{id}'.format(api_url=self.api_url, id=id), {})
        self.assertEqual(200, resp.status_code)

        resp = self.client.get('{api_url}/.favorites/folders'.format(api_url=self.api_url))
        self.assertEqual(200, resp.status_code)
        data = loads(resp.content)['data']
        self.assertEqual(0, data[0]['favorites_count'])

        resp = self.client.get(
            '{api_url}/.favorites/folders/{folder_name}'.format(
                api_url=self.api_url, folder_name=Folder.FAVORITES_FOLDER_NAME
            )
        )
        self.assertEqual(200, resp.status_code)
        data = loads(resp.content)['data']
        self.assertEqual(0, len(data))

    def test_delete_unknown_bookmark(self):
        resp = self.client.delete('{api_url}/.favorites/bookmarks/{id}'.format(api_url=self.api_url, id=10101), {})
        self.assertEqual(404, resp.status_code)

    def test_move_bookmark(self):
        self._create_folder('Другая папка', Folder.FOLDER_TYPE_CUSTOM)

        resp = self._create_bookmark('моя первая закладка', 'Новая Страница', Folder.FAVORITES_FOLDER_NAME)
        id = loads(resp.content)['data']['id']

        resp = self.client.get('{api_url}/.favorites/folders'.format(api_url=self.api_url))
        self.assertEqual(200, resp.status_code)
        data = loads(resp.content)['data']
        self.assertEqual(Folder.FAVORITES_FOLDER_NAME, data[0]['name'])
        self.assertEqual(1, data[0]['favorites_count'])
        self.assertEqual('Другая папка', data[1]['name'])
        self.assertEqual(0, data[1]['favorites_count'])

        assert_queries = 13 if not settings.WIKI_CODE == 'wiki' else 11
        with self.assertNumQueries(assert_queries):
            # переместим закладку в папку "Другая папка"
            resp = self.client.post(
                '{api_url}/.favorites/bookmarks/{id}/move'.format(api_url=self.api_url, id=id),
                {'target_folder_name': 'Другая папка'},
            )
        self.assertEqual(200, resp.status_code)

        resp = self.client.get('{api_url}/.favorites/folders'.format(api_url=self.api_url))
        self.assertEqual(200, resp.status_code)
        data = loads(resp.content)['data']
        self.assertEqual(Folder.FAVORITES_FOLDER_NAME, data[0]['name'])
        self.assertEqual(0, data[0]['favorites_count'])
        self.assertEqual('Другая папка', data[1]['name'])
        self.assertEqual(1, data[1]['favorites_count'])

        # переместим закладку обратно в папку Избранное
        resp = self.client.post(
            '{api_url}/.favorites/bookmarks/{id}/move'.format(api_url=self.api_url, id=id),
            {'target_folder_name': Folder.FAVORITES_FOLDER_NAME},
        )

        resp = self.client.get('{api_url}/.favorites/folders'.format(api_url=self.api_url))
        self.assertEqual(200, resp.status_code)
        data = loads(resp.content)['data']
        self.assertEqual(Folder.FAVORITES_FOLDER_NAME, data[0]['name'])
        self.assertEqual(1, data[0]['favorites_count'])
        self.assertEqual('Другая папка', data[1]['name'])
        self.assertEqual(0, data[1]['favorites_count'])

    def test_move_bookmark_to_new_folder(self):
        resp = self._create_bookmark('моя первая закладка', 'Новая Страница', Folder.FAVORITES_FOLDER_NAME)
        id = loads(resp.content)['data']['id']

        resp = self.client.get('{api_url}/.favorites/folders'.format(api_url=self.api_url))
        self.assertEqual(200, resp.status_code)
        data = loads(resp.content)['data']
        self.assertEqual(Folder.FAVORITES_FOLDER_NAME, data[0]['name'])
        self.assertEqual(1, data[0]['favorites_count'])
        self.assertEqual(4, len(data))

        # переместим закладку в папку "Новая папка"
        resp = self.client.post(
            '{api_url}/.favorites/bookmarks/{id}/move'.format(api_url=self.api_url, id=id),
            {'target_folder_name': 'Новая папка'},
        )
        self.assertEqual(200, resp.status_code)

        resp = self.client.get('{api_url}/.favorites/folders'.format(api_url=self.api_url))
        self.assertEqual(200, resp.status_code)
        data = loads(resp.content)['data']
        self.assertEqual(Folder.FAVORITES_FOLDER_NAME, data[0]['name'])
        self.assertEqual(0, data[0]['favorites_count'])
        self.assertEqual('Новая папка', data[1]['name'])
        self.assertEqual(1, data[1]['favorites_count'])

    def test_move_bookmark_to_autofolder(self):
        resp = self._create_bookmark('моя первая закладка', 'Новая Страница', Folder.FAVORITES_FOLDER_NAME)
        id = loads(resp.content)['data']['id']

        # переместим закладку в папку "Новая папка"
        resp = self.client.post(
            '{api_url}/.favorites/bookmarks/{id}/move'.format(api_url=self.api_url, id=id),
            {'target_folder_name': Folder.OWNER_AUTOFOLDER_NAME},
        )
        self.assertEqual(409, resp.status_code)

    def test_move_bookmark_with_unknown_id(self):
        # переместим закладку в папку "Новая папка"
        resp = self.client.post(
            '{api_url}/.favorites/bookmarks/{id}/move'.format(api_url=self.api_url, id=10101),
            {'target_folder_name': Folder.FAVORITES_FOLDER_NAME},
        )
        self.assertEqual(404, resp.status_code)

    def test_move_bookmark_without_required_parameter(self):
        resp = self._create_bookmark('моя первая закладка', 'Новая Страница', Folder.FAVORITES_FOLDER_NAME)
        id = loads(resp.content)['data']['id']

        resp = self.client.post('{api_url}/.favorites/bookmarks/{id}/move'.format(api_url=self.api_url, id=id), {})
        self.assertEqual(409, resp.status_code)

    def test_delete_bookmarks_by_supertag(self):
        page = self.create_page(
            tag='test page', body='page body', authors_to_add=[self.user_thasonic], last_author=self.user_thasonic
        )

        resp = self.client.put(
            '{api_url}/.favorites/bookmarks'.format(api_url=self.api_url),
            {'folder_name': Folder.FAVORITES_FOLDER_NAME, 'title': 'моя первая закладка', 'url': page.absolute_url},
        )
        b1_id = loads(resp.content)['data']['id']

        resp = self.client.post(
            '{api_url}/.favorites/bookmarks'.format(api_url=self.api_url), {'supertag': page.supertag}
        )
        self.assertEqual(200, resp.status_code)

        resp = self.client.get('{api_url}/.favorites/folders'.format(api_url=self.api_url))
        self.assertEqual(200, resp.status_code)
        data = loads(resp.content)['data']
        self.assertEqual(4, len(data))
        self.assertEqual(0, data[0]['favorites_count'])

        resp = self.client.post(
            '{api_url}/.favorites/bookmarks/{id}/move'.format(api_url=self.api_url, id=b1_id),
            {'target_folder_name': 'Новая папка'},
        )
        self.assertEqual(404, resp.status_code)

    def test_delete_bookmarks_by_supertag_without_required_parameter(self):
        resp = self._create_bookmark('моя первая закладка', 'Новая Страница', Folder.FAVORITES_FOLDER_NAME)

        resp = self.client.post('{api_url}/.favorites/bookmarks'.format(api_url=self.api_url), {})
        self.assertEqual(409, resp.status_code)

    def test_delete_bookmarks_by_nonexistent_supertag(self):
        resp = self.client.post(
            '{api_url}/.favorites/bookmarks'.format(api_url=self.api_url), {'supertag': 'nonexistentsupertag'}
        )
        self.assertEqual(200, resp.status_code)

    def test_get_empty_bookmarks_list(self):
        resp = self.client.get('{api_url}/.favorites/bookmarks'.format(api_url=self.api_url))
        self.assertEqual(200, resp.status_code)
        data = loads(resp.content)['data']
        self.assertEqual(0, len(data))

    @override_settings(CELERY_ALWAYS_EAGER=False, CELERY_TASK_ALWAYS_EAGER=False)
    def test_get_bookmarks_list(self):
        self._create_bookmark('first bookmark', 'Page 1', Folder.FAVORITES_FOLDER_NAME)
        self._create_bookmark('second bookmark', 'Page 2', Folder.FAVORITES_FOLDER_NAME)
        self._create_bookmark('третья закладка', 'Page 3', Folder.FAVORITES_FOLDER_NAME)
        update_autofolders()

        self._create_folder('Другая папка', Folder.FOLDER_TYPE_CUSTOM)
        self._create_bookmark('вторая закладка', 'Страница 2', 'Другая папка')

        resp = self.client.get('{api_url}/.favorites/bookmarks'.format(api_url=self.api_url))

        self.assertEqual(200, resp.status_code)
        data = loads(resp.content)['data']
        self.assertEqual(2, len(data))
        bookmarks_count_dict = {Folder.FAVORITES_FOLDER_NAME: 3, 'Другая папка': 1}
        for b_dict in data:
            self.assertEqual(bookmarks_count_dict[b_dict['folder_name']], len(b_dict['bookmarks']))

        resp = self.client.get('{api_url}/.favorites/folders/__INBOX__'.format(api_url=self.api_url))
        self.assertEqual(200, resp.status_code)
        data = loads(resp.content)['data']
        self.assertEqual(3, len(data))

        resp = self.client.get('{api_url}/.favorites/folders/Другая%20папка'.format(api_url=self.api_url))
        self.assertEqual(200, resp.status_code)
        data = loads(resp.content)['data']
        self.assertEqual(1, len(data))

        resp = self.client.get('{api_url}/.favorites/folders/__OWNER__'.format(api_url=self.api_url))
        self.assertEqual(200, resp.status_code)

        resp = self.client.get('{api_url}/.favorites/folders/__LAST_EDIT__'.format(api_url=self.api_url))
        self.assertEqual(200, resp.status_code)
