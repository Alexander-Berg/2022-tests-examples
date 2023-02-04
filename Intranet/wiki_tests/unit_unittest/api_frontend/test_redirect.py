
import ujson as json

from wiki.pages.models import Page
from intranet.wiki.tests.wiki_tests.common.unittest_base import BaseApiTestCase


class RedirectViewTest(BaseApiTestCase):
    def setUp(self):
        super(RedirectViewTest, self).setUp()
        self.setUsers()
        self.client.login('chapson')

        self.page_one = self.create_page(supertag='one', tag='one')
        self.page_two = self.create_page(supertag='two', tag='two')

        self.page_one_grid = self.create_page(supertag='oneg', page_type=Page.TYPES.GRID, tag='oneg')
        self.page_two_grid = self.create_page(supertag='twog', tag='twog')

        self.page_one.redirects_to = self.page_two
        self.page_one.save()

        self.page_one_grid.redirects_to = self.page_two_grid
        self.page_one_grid.save()

    def get_url(self, page):
        return '/'.join(
            [
                self.api_url,
                page.supertag,
            ]
        )

    def get_url_redirect_management(self, page):
        return '/'.join([self.api_url, page.supertag, '.redirect'])

    def test_page_with_redirect(self):
        response = self.client.get(self.get_url_redirect_management(page=self.page_one))

        self.assertEqual(response.status_code, 200)
        data = json.loads(response.content)['data']

        self.assertEqual(data['redirect_to_tag'], self.page_two.supertag)

    def test_page_with_redirect_to_regular_page(self):
        response = self.client.get(self.get_url(page=self.page_one))

        self.assertEqual(response.status_code, 200)
        data = json.loads(response.content)['data']

        self.assertEqual(data['redirect_to_tag'], self.page_two.supertag)
        self.assertEqual(data['page_type'], 'article')

    def test_page_with_redirect_to_regular_grid(self):
        response = self.client.get(self.get_url(page=self.page_one_grid))
        self.assertEqual(response.status_code, 200)
        data = json.loads(response.content)['data']
        self.assertEqual(data['redirect_to_tag'], self.page_two_grid.supertag)
        self.assertEqual(data['page_type'], 'grid')
