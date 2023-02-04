from django.conf import settings

from wiki.favorites_v2.dao import create_bookmark, get_folder, get_or_create_folder, get_user_bookmarks_by_folders
from wiki.favorites_v2.models import Folder
from wiki.favorites_v2.tasks.update_autofolders import update_autofolders
from intranet.wiki.tests.wiki_tests.common.base_testcase import BaseTestCase


class FavoritesDaoTestCase(BaseTestCase):
    def setUp(self):
        super(FavoritesDaoTestCase, self).setUp()
        self.setUsers()
        self.client.login('thasonic')
        self.user = self.user_thasonic

    def _create_bookmark(self, tag, title, folder_name):
        page = self.create_page(tag=tag, authors_to_add=[self.user_thasonic], last_author=self.user_thasonic)

        folder = get_or_create_folder(self.user, folder_name)
        url = 'https://{wiki_host}/{supertag}'.format(wiki_host=settings.NGINX_HOST, supertag=page.supertag)
        return create_bookmark(folder, title, url)

    def test_create_folder(self):
        folder_name = 'My Folder 1'
        self.assertIsNone(get_folder(folder_name, self.user))
        folder = get_or_create_folder(self.user, folder_name)
        self.assertIsNotNone(get_folder(folder_name, self.user))
        self.assertEqual(folder.name, folder_name)

    def test_create_bookmark(self):
        tag = 'new page'
        title = 'new page title'
        folder_name = 'My folder 2'
        bookmark = self._create_bookmark(tag, title, folder_name)
        self.assertEqual(bookmark.title, title)
        self.assertEqual(bookmark.folder.name, folder_name)

    def test_get_user_bookmarks_by_folders(self):
        self._create_bookmark('first bookmark', 'Page 1', Folder.FAVORITES_FOLDER_NAME)
        self._create_bookmark('second bookmark', 'Page 2', Folder.FAVORITES_FOLDER_NAME)
        self._create_bookmark('третья закладка', 'Page 3', Folder.FAVORITES_FOLDER_NAME)

        custom_folder_name = 'My Folder'
        bookmark_title_in_custom_folder = 'вторая закладка'
        self._create_bookmark('Страница 2', bookmark_title_in_custom_folder, custom_folder_name)

        update_autofolders()

        result = get_user_bookmarks_by_folders(self.user)
        self.assertEqual(3, len(result))
        self.assertEqual(1, len(result[custom_folder_name]))
        self.assertEqual(3, len(result[Folder.FAVORITES_FOLDER_NAME]))
        self.assertEqual(4, len(result[Folder.OWNER_AUTOFOLDER_NAME]))
        self.assertEqual(bookmark_title_in_custom_folder, result[custom_folder_name][0].title)
