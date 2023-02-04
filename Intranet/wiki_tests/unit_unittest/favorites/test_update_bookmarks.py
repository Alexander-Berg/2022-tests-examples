from datetime import datetime
from unittest import skipIf

import pytz
from django.conf import settings
from ujson import loads

from wiki.favorites_v2.models import AutoBookmark, Bookmark, Folder
from wiki.favorites_v2.tasks.update_autofolders import update_autofolders
from wiki.favorites_v2.tasks.update_bookmarks import update_bookmarks
from wiki.pages.models import Page, PageWatch
from wiki.utils.timezone import make_aware
from intranet.wiki.tests.wiki_tests.common.unittest_base import BaseApiTestCase
from intranet.wiki.tests.wiki_tests.unit_unittest.api_frontend.base import create_grid

GRID_STRUCTURE = """
{
  "title" : "List of conferences",
  "width" : "100%",
  "sorting" : [],
  "fields" : [
    {
      "name" : "name",
      "title" : "Name of conference"
    }
  ]
}
"""


class UpdateBookmarksTest(BaseApiTestCase):
    """
    Тесты обновления атрибутов закладок.
    """

    create_user_clusters = True

    def setUp(self):
        super(UpdateBookmarksTest, self).setUp()
        self.setUsers(use_legacy_subscr_favor=True)
        self.setPages()
        self.client.login(self.user_thasonic.username)

    def _create_bookmark(self, page, folder_name):
        self.client.put(
            '{api_url}/.favorites/bookmarks'.format(api_url=self.api_url),
            {'folder_name': folder_name, 'title': page.title, 'url': page.absolute_url},
        )

    def test_update_bookmarks(self):
        # создадим несколько закладок в папке Избранное
        pages = Page.active.filter(authors=self.user_thasonic).order_by('created_at')
        for page in pages:
            self.client.put(
                '{api_url}/.favorites/bookmarks'.format(api_url=self.api_url),
                {'folder_name': Folder.FAVORITES_FOLDER_NAME, 'title': page.title, 'url': page.absolute_url},
            )

        resp = self.client.get(
            '{api_url}/.favorites/folders/{name}'.format(api_url=self.api_url, name=Folder.FAVORITES_FOLDER_NAME)
        )
        self.assertEqual(200, resp.status_code)
        data = loads(resp.content)['data']
        self.assertEqual(2, len(data))
        self.assertEqual(self.user_thasonic.username, data[0]['page_last_editor'])

        # отредактируем одну из страниц пользователя user_thasonic как пользователь user_kolomeetz
        test_page = Page.active.get(authors=self.user_thasonic, supertag='users/thasonic')
        test_page.last_author = self.user_kolomeetz
        # Закладка попадает в любом случае в автопапку, но дата влияет на сортировку,
        # так как выборка сортируется по modified_at.
        # Последняя добавленная закладка должна быть первой в списке.
        # Поэтому ставим очень большую дату.
        test_page.modified_at = make_aware(pytz.timezone(self.user_kolomeetz.staff.tz), datetime(2615, 3, 10, 2, 2, 2))
        test_page.save()
        test_page = Page.objects.get(id=test_page.id)
        self.assertEqual(self.user_kolomeetz.username, test_page.last_author.username)

        # обновим пользовательские папки
        update_bookmarks(Bookmark.objects.filter(supertag__isnull=False), Bookmark)

        # пооверим, что закладка на отредактированную страницу изменилась и находится первой в выборке
        resp = self.client.get(
            '{api_url}/.favorites/folders/{name}'.format(api_url=self.api_url, name=Folder.FAVORITES_FOLDER_NAME)
        )
        self.assertEqual(200, resp.status_code)
        data = loads(resp.content)['data']
        self.assertEqual(2, len(data))
        self.assertEqual(self.user_thasonic.username, data[0]['page_last_editor'])
        self.assertEqual(self.user_kolomeetz.username, data[1]['page_last_editor'])
        self.assertEqual('2615-03-10T02:02:02', data[1]['page_modified_at'])

    @skipIf(settings.IS_BUSINESS, 'wait for fixing for business')
    def test_update_autobookmarks(self):
        # подписать user_thasonic на страницы user_chapson
        pages = Page.active.filter(authors=self.user_chapson)
        for page in pages:
            PageWatch(page=page, user=self.user_thasonic.username).save()

        # обновим автопапки
        update_autofolders()

        resp = self.client.get(
            '{api_url}/.favorites/folders/{name}'.format(api_url=self.api_url, name=Folder.OWNER_AUTOFOLDER_NAME)
        )
        self.assertEqual(200, resp.status_code)
        data = loads(resp.content)['data']
        self.assertEqual(4, len(data))
        self.assertEqual(self.user_thasonic.username, data[0]['page_last_editor'])

        resp = self.client.get(
            '{api_url}/.favorites/folders/{name}'.format(api_url=self.api_url, name=Folder.WATCHER_AUTOFOLDER_NAME)
        )
        self.assertEqual(200, resp.status_code)
        data = loads(resp.content)['data']
        self.assertEqual(8, len(data))
        self.assertEqual(self.user_chapson.username, data[0]['page_last_editor'])

        # отредактируем одну из страниц пользователя user_thasonic как пользователь user_kolomeetz
        test_page = Page.active.filter(authors=self.user_thasonic)[1]
        test_page.last_author = self.user_kolomeetz
        # см test_update_bookmarks за пояснением зачем это сделано.
        test_page.modified_at = make_aware(pytz.timezone(self.user_kolomeetz.staff.tz), datetime(2615, 3, 10, 2, 2, 2))
        test_page.save()
        test_page = Page.objects.get(id=test_page.id)
        self.assertEqual(self.user_kolomeetz.username, test_page.last_author.username)

        # отредактируем одну из страниц пользователя user_chapson как пользователь user_kolomeetz
        test_page2 = Page.active.filter(authors=self.user_chapson)[1]
        test_page2.last_author = self.user_kolomeetz
        test_page2.modified_at = make_aware(pytz.timezone(self.user_kolomeetz.staff.tz), datetime(2615, 3, 10, 2, 2, 2))
        test_page2.save()
        test_page2 = Page.objects.get(id=test_page2.id)
        self.assertEqual(self.user_kolomeetz.username, test_page2.last_author.username)

        # обновим закладки в автопапках
        update_bookmarks(AutoBookmark.objects.all(), AutoBookmark)

        # проверим, что закладки на отредактированные страницы изменились и находятся первыми в выборке
        resp = self.client.get(
            '{api_url}/.favorites/folders/{name}'.format(api_url=self.api_url, name=Folder.OWNER_AUTOFOLDER_NAME)
        )
        self.assertEqual(200, resp.status_code)
        data = loads(resp.content)['data']
        self.assertEqual(4, len(data))
        self.assertEqual(self.user_kolomeetz.username, data[0]['page_last_editor'])
        self.assertEqual('2615-03-10T02:02:02', data[0]['page_modified_at'])
        self.assertEqual(self.user_thasonic.username, data[1]['page_last_editor'])

        resp = self.client.get(
            '{api_url}/.favorites/folders/{name}'.format(api_url=self.api_url, name=Folder.WATCHER_AUTOFOLDER_NAME)
        )
        self.assertEqual(200, resp.status_code)
        data = loads(resp.content)['data']
        self.assertEqual(8, len(data))
        self.assertEqual(self.user_kolomeetz.username, data[0]['page_last_editor'])
        self.assertEqual('2615-03-10T02:02:02', data[0]['page_modified_at'])
        self.assertEqual(self.user_chapson.username, data[1]['page_last_editor'])

    def test_update_bookmarks_by_supertag(self):
        # добавим в закладки обычную страницу

        update_autofolders()
        test_page = Page.active.get(supertag='users/thasonic/notes')
        self._create_bookmark(test_page, Folder.FAVORITES_FOLDER_NAME)

        # отредактируем страницу как пользователь user_kolomeetz
        test_page.last_author = self.user_kolomeetz
        # см test_update_bookmarks за пояснением зачем это сделано.
        test_page.modified_at = make_aware(pytz.timezone(self.user_kolomeetz.staff.tz), datetime(2615, 3, 10, 2, 2, 2))
        old_title = test_page.title
        test_page.title = 'new title'
        test_page.save()
        test_page = Page.objects.get(id=test_page.id)
        self.assertEqual(self.user_kolomeetz, test_page.last_author)

        # обновим закладки
        update_bookmarks(Bookmark.objects.filter(supertag=test_page.supertag), Bookmark)

        update_bookmarks(AutoBookmark.objects.filter(supertag=test_page.supertag), AutoBookmark)

        # проверим, что закладки на отредактированную страницу изменились
        resp = self.client.get(
            '{api_url}/.favorites/folders/{name}'.format(api_url=self.api_url, name=Folder.FAVORITES_FOLDER_NAME)
        )
        self.assertEqual(200, resp.status_code)
        data = loads(resp.content)['data']
        self.assertEqual(1, len(data))
        self.assertEqual(self.user_kolomeetz.username, data[0]['page_last_editor'])
        self.assertEqual('2615-03-10T02:02:02', data[0]['page_modified_at'])
        self.assertEqual(old_title.strip(), data[0]['title'])

        resp = self.client.get(
            '{api_url}/.favorites/folders/{name}'.format(api_url=self.api_url, name=Folder.OWNER_AUTOFOLDER_NAME)
        )
        self.assertEqual(200, resp.status_code)
        data = loads(resp.content)['data']
        self.assertEqual(self.user_kolomeetz.username, data[0]['page_last_editor'])
        self.assertEqual('2615-03-10T02:02:02', data[0]['page_modified_at'])
        self.assertEqual(test_page.title, data[0]['title'])

        # добавим в закладки grid
        grid_supertag = 'grid'
        create_grid(self, grid_supertag, GRID_STRUCTURE, self.user_thasonic)

        grid_page = Page.objects.get(supertag=grid_supertag)
        self._create_bookmark(grid_page, Folder.FAVORITES_FOLDER_NAME)

        # отредактируем страницу как пользователь user_kolomeetz
        grid_page.last_author = self.user_kolomeetz
        grid_page.modified_at = make_aware(pytz.timezone(self.user_kolomeetz.staff.tz), datetime(2615, 3, 10, 2, 2, 2))
        grid_page.save()
        grid_page = Page.objects.get(id=grid_page.id)
        self.assertEqual(self.user_kolomeetz.username, grid_page.last_author.username)

        # обновим закладки
        update_bookmarks(Bookmark.objects.filter(supertag=grid_page.supertag), Bookmark)

        # проверим, что закладка на отредактированную страницу изменилась
        resp = self.client.get(
            '{api_url}/.favorites/folders/{name}'.format(api_url=self.api_url, name=Folder.FAVORITES_FOLDER_NAME)
        )
        self.assertEqual(200, resp.status_code)
        data = loads(resp.content)['data']
        self.assertEqual(2, len(data))
        self.assertEqual(self.user_kolomeetz.username, data[0]['page_last_editor'])
        self.assertEqual('2615-03-10T02:02:02', data[0]['page_modified_at'])
