from unittest import skipIf
from django.conf import settings
from ujson import loads

from wiki.favorites_v2.models import Folder
from intranet.wiki.tests.wiki_tests.common.unittest_base import BaseApiTestCase
from intranet.wiki.tests.wiki_tests.common.utils import celery_eager


class BaseFoldersTest(BaseApiTestCase):
    def _create_folder(self, name, type):
        resp = self.client.put('{api_url}/.favorites/folders/{name}'.format(api_url=self.api_url, name=name), {})
        self.assertEqual(200, resp.status_code)

        data = loads(resp.content)['data']
        self.assertEqual(name, data['name'])
        self.assertEqual(type, data['type'])
        self.assertEqual(0, data['favorites_count'])

    def _create_bookmark(self, title, tag, folder_name):
        page = self.create_page(
            tag=tag, body='page test', title=tag, authors_to_add=[self.user_thasonic], last_author=self.user_thasonic
        )

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


class APIChangeOrderTest(BaseFoldersTest):
    def test_change_folder_order_for_unknown_folder(self):
        self.setUsers(use_legacy_subscr_favor=True)
        self.client.login('thasonic')
        resp = self.client.post(
            '{api_url}/.favorites/folders/{name}/drag'.format(api_url=self.api_url, name='unknown'),
            {'after': Folder.FAVORITES_FOLDER_NAME},
        )
        self.assertEqual(404, resp.status_code)

    def test_change_folder_order_without_required_parameter(self):
        self.setUsers()
        self.client.login('thasonic')
        folder_name_1 = 'первая папка'
        self._create_folder(folder_name_1, Folder.FOLDER_TYPE_CUSTOM)

        resp = self.client.post(
            '{api_url}/.favorites/folders/{name}/drag'.format(api_url=self.api_url, name=folder_name_1), {}
        )
        self.assertEqual(409, resp.status_code)

    def test_change_folder_order_after_unknown_folder(self):
        self.setUsers(use_legacy_subscr_favor=True)
        self.client.login('thasonic')
        folder_name_1 = 'первая папка'
        self._create_folder(folder_name_1, Folder.FOLDER_TYPE_CUSTOM)

        resp = self.client.post(
            '{api_url}/.favorites/folders/{name}/drag'.format(api_url=self.api_url, name=folder_name_1),
            {'after': 'unknown'},
        )
        self.assertEqual(404, resp.status_code)

    def test_change_folder_order_after_the_same_folder(self):
        self.setUsers(use_legacy_subscr_favor=True)
        self.client.login('thasonic')
        folder_name_1 = 'первая папка'
        self._create_folder(folder_name_1, Folder.FOLDER_TYPE_CUSTOM)

        resp = self.client.post(
            '{api_url}/.favorites/folders/{name}/drag'.format(api_url=self.api_url, name=folder_name_1),
            {'after': folder_name_1},
        )
        self.assertEqual(409, resp.status_code)


class APIFoldersTest(BaseFoldersTest):
    def setUp(self):
        super().setUp()
        self.setUsers()
        self.user_thasonic.profile['new_favorites'] = True
        self.user_thasonic.save()
        self.client.login('thasonic')

    @skipIf(settings.IS_BUSINESS, 'wait for fixing for business')
    @celery_eager
    def test_get_empty_folders_list(self):
        request_url = '{api_url}/.favorites/folders'.format(api_url=self.api_url)
        resp = self.client.get(request_url)
        self.assertEqual(200, resp.status_code)

        data = loads(resp.content)['data']
        self.assertEqual(4, len(data))
        self.assertEqual(Folder.FAVORITES_FOLDER_NAME, data[0]['name'])
        self.assertEqual(Folder.FOLDER_TYPE_FAVORITES, data[0]['type'])
        self.assertEqual(0, data[0]['favorites_count'])
        self.assertEqual(Folder.OWNER_AUTOFOLDER_NAME, data[1]['name'])
        self.assertEqual(Folder.FOLDER_TYPE_AUTO, data[1]['type'])
        self.assertEqual(0, data[1]['favorites_count'])
        self.assertEqual(Folder.WATCHER_AUTOFOLDER_NAME, data[2]['name'])
        self.assertEqual(Folder.FOLDER_TYPE_AUTO, data[2]['type'])
        self.assertEqual(0, data[2]['favorites_count'])
        self.assertEqual(Folder.LAST_EDIT_AUTOFOLDER_NAME, data[3]['name'])
        self.assertEqual(Folder.FOLDER_TYPE_AUTO, data[3]['type'])
        self.assertEqual(0, data[3]['favorites_count'])

    @skipIf(settings.IS_BUSINESS, 'wait for fixing for business')
    @celery_eager
    def test_get_folders_list(self):
        self._create_folder('моя папка', Folder.FOLDER_TYPE_CUSTOM)
        self._create_folder('васина папка', Folder.FOLDER_TYPE_CUSTOM)

        request_url = '{api_url}/.favorites/folders'.format(api_url=self.api_url)
        resp = self.client.get(request_url)
        self.assertEqual(200, resp.status_code)

        data = loads(resp.content)['data']
        self.assertEqual(6, len(data))
        self.assertEqual(Folder.FAVORITES_FOLDER_NAME, data[0]['name'])
        self.assertEqual(Folder.FOLDER_TYPE_FAVORITES, data[0]['type'])
        self.assertEqual(0, data[0]['favorites_count'])
        self.assertEqual('васина папка', data[1]['name'])
        self.assertEqual(Folder.FOLDER_TYPE_CUSTOM, data[1]['type'])
        self.assertEqual(0, data[1]['favorites_count'])
        self.assertEqual('моя папка', data[2]['name'])
        self.assertEqual(Folder.FOLDER_TYPE_CUSTOM, data[2]['type'])
        self.assertEqual(0, data[2]['favorites_count'])
        self.assertEqual(Folder.OWNER_AUTOFOLDER_NAME, data[3]['name'])
        self.assertEqual(Folder.FOLDER_TYPE_AUTO, data[3]['type'])
        self.assertEqual(0, data[3]['favorites_count'])
        self.assertEqual(Folder.WATCHER_AUTOFOLDER_NAME, data[4]['name'])
        self.assertEqual(Folder.FOLDER_TYPE_AUTO, data[4]['type'])
        self.assertEqual(0, data[4]['favorites_count'])
        self.assertEqual(Folder.LAST_EDIT_AUTOFOLDER_NAME, data[5]['name'])
        self.assertEqual(Folder.FOLDER_TYPE_AUTO, data[5]['type'])
        self.assertEqual(0, data[5]['favorites_count'])

    def test_create_folder_with_existent_name(self):
        folder_name = 'Новая папка'
        self._create_folder(folder_name, Folder.FOLDER_TYPE_CUSTOM)

        resp = self.client.put('{api_url}/.favorites/folders/{name}'.format(api_url=self.api_url, name=folder_name), {})
        self.assertEqual(409, resp.status_code)

        error = loads(resp.content)['error']
        self.assertEqual('ALREADY_EXISTS', error['error_code'])

    def test_create_folder_with_reserved_name(self):
        resp = self.client.put(
            '{api_url}/.favorites/folders/{name}'.format(
                api_url=self.api_url, name=Folder.RESERVED_FOLDER_NAMES_LIST[0]
            ),
            {},
        )
        self.assertEqual(409, resp.status_code)

    def test_edit_folder_name(self):
        folder_name = 'Новая папка'
        self._create_folder(folder_name, Folder.FOLDER_TYPE_CUSTOM)
        self._create_bookmark('первая закладка', 'какая-то страница', folder_name)

        folders_list_request_url = '{api_url}/.favorites/folders'.format(api_url=self.api_url)
        resp = self.client.get(folders_list_request_url)
        self.assertEqual(200, resp.status_code)

        data = loads(resp.content)['data']
        self.assertEqual(5, len(data))
        self.assertEqual(folder_name, data[1]['name'])

        new_folder_name = 'Мои закладки'
        assert_queries = 7 if not settings.WIKI_CODE == 'wiki' else 5
        with self.assertNumQueries(assert_queries):
            resp = self.client.post(
                '{api_url}/.favorites/folders/{name}'.format(api_url=self.api_url, name=folder_name),
                {'new_name': new_folder_name},
            )
        self.assertEqual(200, resp.status_code)
        data = loads(resp.content)['data']
        self.assertEqual(new_folder_name, data['name'])
        self.assertEqual(1, data['favorites_count'])
        self.assertEqual(Folder.FOLDER_TYPE_CUSTOM, data['type'])

        resp = self.client.get(folders_list_request_url)
        self.assertEqual(200, resp.status_code)
        data = loads(resp.content)['data']
        self.assertEqual(5, len(data))
        self.assertEqual(new_folder_name, data[1]['name'])

    def test_edit_unknown_folder_name(self):
        folder_name = 'Новая папка'
        self._create_folder(folder_name, Folder.FOLDER_TYPE_CUSTOM)

        resp = self.client.post(
            '{api_url}/.favorites/folders/{name}'.format(api_url=self.api_url, name='xxx'), {'new_name': 'Мои закладки'}
        )
        self.assertEqual(404, resp.status_code)

    def test_edit_folder_name_without_required_parameter(self):
        folder_name = 'Новая папка'
        self._create_folder(folder_name, Folder.FOLDER_TYPE_CUSTOM)

        resp = self.client.post(
            '{api_url}/.favorites/folders/{name}'.format(api_url=self.api_url, name=folder_name), {}
        )
        self.assertEqual(409, resp.status_code)

    def test_rename_folder_to_existent_name(self):
        folder_name = 'Новая папка'
        existent_folder_name = 'Мои закладки'
        self._create_folder(folder_name, Folder.FOLDER_TYPE_CUSTOM)
        self._create_folder(existent_folder_name, Folder.FOLDER_TYPE_CUSTOM)

        resp = self.client.post(
            '{api_url}/.favorites/folders/{name}'.format(api_url=self.api_url, name=folder_name),
            {'new_name': existent_folder_name},
        )
        self.assertEqual(409, resp.status_code)
        error = loads(resp.content)['error']
        self.assertEqual('ALREADY_EXISTS', error['error_code'])

    def test_edit_autofolder(self):
        resp = self.client.post(
            '{api_url}/.favorites/folders/{name}'.format(api_url=self.api_url, name=Folder.WATCHER_AUTOFOLDER_NAME),
            {'new_name': 'some new name'},
        )
        self.assertEqual(409, resp.status_code)

    def test_delete_folder(self):
        folder_name = 'Новая папка'
        self._create_folder(folder_name, Folder.FOLDER_TYPE_CUSTOM)
        self._create_bookmark('первая закладка', 'какая-то страница', folder_name)
        self._create_bookmark('вторая закладка', 'еще одна страница', folder_name)

        assert_queries = 23 if not settings.WIKI_CODE == 'wiki' else 21
        with self.assertNumQueries(assert_queries):
            resp = self.client.delete(
                '{api_url}/.favorites/folders/{name}'.format(api_url=self.api_url, name=folder_name), {}
            )
            self.assertEqual(200, resp.status_code)

        folders_list_request_url = '{api_url}/.favorites/folders'.format(api_url=self.api_url)
        resp = self.client.get(folders_list_request_url)
        self.assertEqual(200, resp.status_code)

        data = loads(resp.content)['data']
        self.assertEqual(4, len(data))
        self.assertEqual(Folder.FAVORITES_FOLDER_NAME, data[0]['name'])
        self.assertEqual(2, data[0]['favorites_count'])
        self.assertEqual(Folder.OWNER_AUTOFOLDER_NAME, data[1]['name'])
        self.assertEqual(Folder.WATCHER_AUTOFOLDER_NAME, data[2]['name'])
        self.assertEqual(Folder.LAST_EDIT_AUTOFOLDER_NAME, data[3]['name'])

        request_url = '{api_url}/.favorites/folders/{folder_name}'.format(
            api_url=self.api_url, folder_name=Folder.FAVORITES_FOLDER_NAME
        )
        resp = self.client.get(request_url)
        self.assertEqual(200, resp.status_code)
        data = loads(resp.content)['data']
        self.assertEqual(2, len(data))
        self.assertEqual(data[0]['title'], 'еще одна страница')
        self.assertEqual(data[1]['title'], 'какая-то страница')

    def test_delete_unknown_folder(self):
        resp = self.client.delete(
            '{api_url}/.favorites/folders/{name}'.format(api_url=self.api_url, name='unknown'), {}
        )
        self.assertEqual(404, resp.status_code)

    def test_delete_folder_without_bookmarks(self):
        folder_name = 'Новая папка'
        self._create_folder(folder_name, Folder.FOLDER_TYPE_CUSTOM)

        resp = self.client.delete(
            '{api_url}/.favorites/folders/{name}'.format(api_url=self.api_url, name=folder_name), {}
        )
        self.assertEqual(200, resp.status_code)

        folders_list_request_url = '{api_url}/.favorites/folders'.format(api_url=self.api_url)
        resp = self.client.get(folders_list_request_url)
        self.assertEqual(200, resp.status_code)

        data = loads(resp.content)['data']
        self.assertEqual(4, len(data))
        self.assertEqual(Folder.FAVORITES_FOLDER_NAME, data[0]['name'])
        self.assertEqual(0, data[0]['favorites_count'])
        self.assertEqual(Folder.OWNER_AUTOFOLDER_NAME, data[1]['name'])
        self.assertEqual(Folder.WATCHER_AUTOFOLDER_NAME, data[2]['name'])
        self.assertEqual(Folder.LAST_EDIT_AUTOFOLDER_NAME, data[3]['name'])

    def test_delete_favorites_folder(self):
        resp = self.client.delete(
            '{api_url}/.favorites/folders/{name}'.format(api_url=self.api_url, name=Folder.FAVORITES_FOLDER_NAME), {}
        )
        self.assertEqual(409, resp.status_code)

    def test_delete_autofolder(self):
        resp = self.client.delete(
            '{api_url}/.favorites/folders/{name}'.format(api_url=self.api_url, name=Folder.OWNER_AUTOFOLDER_NAME), {}
        )
        self.assertEqual(409, resp.status_code)
