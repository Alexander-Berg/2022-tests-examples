
from intranet.wiki.tests.wiki_tests.common.unittest_base import BaseApiTestCase


class WikiAPIViewTestCase(BaseApiTestCase):
    def setUp(self):
        super(WikiAPIViewTestCase, self).setUp()

    def test_403_for_anonymous(self):
        # дергаем любую одну ручку, которая реализована наследником WikiAPIView

        self.client.logout()
        response = self.client.get('{api_url}/.is_readonly'.format(api_url=self.api_url))
        self.assertEqual(response.status_code, 403)
