
from wiki.pages.models import Page
from intranet.wiki.tests.wiki_tests.common.unittest_base import BaseApiTestCase


class DuplicateContentTest(BaseApiTestCase):
    def setUp(self):
        super(DuplicateContentTest, self).setUp()
        self.setUsers()
        self.client.login('thasonic')
        self.user = self.user_thasonic

    def test_no_new_revision(self):
        # тест на баг WIKI-13430
        # человек создал из интерфейса страницу

        page_data = {'title': 'dio', 'body': 'yare yare'}

        tag = 'test'
        request_url = '{api_url}/{page_tag}'.format(api_url=self.api_url, page_tag=tag)

        response = self.client.post(request_url, data=page_data)
        self.assertEqual(200, response.status_code)

        page = Page.active.filter(supertag='test').get()
        self.assertEqual(page.title, 'dio')
        self.assertEqual(page.body, 'yare yare')
        self.assertEqual(page.revision_set.all().count(), 1)

        # бот "обновил"

        page_data = {'body': 'yare yare'}

        response = self.client.post(request_url, data=page_data)
        self.assertEqual(200, response.status_code)

        page.refresh_from_db()
        self.assertEqual(page.title, 'dio')
        self.assertEqual(page.body, 'yare yare')
        self.assertEqual(page.revision_set.all().count(), 1)
