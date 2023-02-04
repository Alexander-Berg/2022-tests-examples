from intranet.wiki.tests.wiki_tests.common.unittest_base import BaseApiTestCase


class ErrorBoosterTests(BaseApiTestCase):
    def setUp(self):
        super(BaseApiTestCase, self).setUp()
        self.setUsers()
        self.client.login('thasonic')

    def test_500(self):
        response = self.client.get('/_api/svc/.smoke_500')

        # response = self.client.get('/_api/svc/.smoke')
        # response = self.client.post('/_api/svc/.smoke', {'test': 'foo', 'abc': 123})
        self.assertEqual(response.status_code, 500)
