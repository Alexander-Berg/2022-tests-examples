
import ujson as json

from intranet.wiki.tests.wiki_tests.common.unittest_base import BaseApiTestCase


class OwnerViewTest(BaseApiTestCase):
    """
    Тесты для SettingsView.
    """

    def setUp(self):
        super(OwnerViewTest, self).setUp()
        self.setUsers()
        self.page = self.create_page(owner=self.user_thasonic)
        self.client.login(self.user_thasonic.username)

    def get_url(self, page):
        return '/'.join(
            [
                self.api_url,
                page.supertag,
                '.owner',
            ]
        )

    def test_get_owner(self):
        url = self.get_url(self.page)
        response = self.client.get(url)

        self.assertEqual(response.status_code, 200)

        response_data = json.loads(response.content)
        owner = response_data['data']['owner']
        self.assertEqual(owner['login'], 'thasonic')

    def test_change_owner(self):
        url = self.get_url(self.page)
        response = self.client.post(url, {'new_owner': {'uid': self.user_chapson.staff.uid}})

        self.assertEqual(response.status_code, 200)

        page = self.refresh_objects(self.page)
        self.assertEqual(page.owner, self.user_chapson)
