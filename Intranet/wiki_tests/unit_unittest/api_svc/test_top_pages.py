
from mock import patch

from intranet.wiki.tests.wiki_tests.unit_unittest.api_svc.base import BaseSvcAPITestCase

top_pages_url = [
    'https://wiki.yandex-team.ru/',
    'https://wiki.yandex-team.ru/market/support/berub2c/',
    'https://wiki.yandex-team.ru/moderation/fullmoderation/',
    'https://wiki.yandex-team.ru/call-center/#huetnhunteuhet',
    'https://wiki.yandex-team.ru/eva/Yandex.drive/',
]

top_pages_supertags = ['', 'market/support/berub2c', 'moderation/fullmoderation', 'call-center', 'eva/Yandex.drive']


class TopPagesTest(BaseSvcAPITestCase):
    def setUp(self):
        super(TopPagesTest, self).setUp()
        self.setUsers()
        self.client.login('thasonic')

    @patch('wiki.utils.metrika.get_top_pages', lambda limit, offset: top_pages_url)
    def test_get_top_pages(self):
        request_url = '%s/.top_pages' % self.api_url
        response = self.client.get(request_url)

        self.assertEqual(response.status_code, 200)
        data = response.json()['data']

        for index in range(5):
            self.assertEqual(data[index]['url'], top_pages_url[index])
            self.assertEqual(data[index]['supertag'], top_pages_supertags[index])
