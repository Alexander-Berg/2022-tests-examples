
from intranet.wiki.tests.wiki_tests.common.unittest_base import BaseApiTestCase


class SubpagesViewTest(BaseApiTestCase):
    def setUp(self):
        super(SubpagesViewTest, self).setUp()
        self.setUsers()
        self.client.login('thasonic')
        self.create_page(tag='Рики', authors_to_add=[self.user_thasonic])
        self.create_page(tag='Рики/Рут', authors_to_add=[self.user_thasonic])
        self.create_page(tag='Рики/Рут/Мидл1', authors_to_add=[self.user_chapson])
        self.create_page(tag='Рики/Рут/Мидл2', authors_to_add=[self.user_thasonic])
        self.create_page(tag='Рики/Рут/Мидл1/Чайлд1', authors_to_add=[self.user_chapson])
        self.create_page(tag='Рики/Рут/Мидл1/Чайлд2', authors_to_add=[self.user_thasonic])

    def test(self):
        url = '{api_url}/riki/.subpages'.format(api_url=self.api_url)
        response = self.client.get(url)

        self.assertEqual(200, response.status_code)
        self.assertEqual(
            {
                'urls': [
                    '/Рики/Рут',
                    '/Рики/Рут/Мидл1',
                    '/Рики/Рут/Мидл1/Чайлд1',
                    '/Рики/Рут/Мидл1/Чайлд2',
                    '/Рики/Рут/Мидл2',
                ],
                'limit_exceeded': False,
            },
            response.data['data'],
        )

    def test_owner(self):
        url = '{api_url}/riki/.subpages?owner=!'.format(api_url=self.api_url)
        response = self.client.get(url)

        self.assertEqual(200, response.status_code)
        self.assertEqual(
            {
                'urls': [
                    '/Рики/Рут',
                    '/Рики/Рут/Мидл1/Чайлд2',
                    '/Рики/Рут/Мидл2',
                ],
                'limit_exceeded': False,
            },
            response.data['data'],
        )
