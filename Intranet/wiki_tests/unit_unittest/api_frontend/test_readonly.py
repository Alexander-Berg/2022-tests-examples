
import mock
from django.conf import settings
from ujson import loads

from intranet.wiki.tests.wiki_tests.common.unittest_base import BaseApiTestCase


class ReadonlyTestCase(BaseApiTestCase):
    def test_get_readonly_service(self):
        self.setUsers()
        self.client.login('chapson')

        def return_true():
            return True

        with mock.patch(target='wiki.middleware.read_only.service_is_readonly', new=return_true):
            response = self.client.get('/_api/frontend/.is_readonly')
            self.assertEqual(loads(response.content)['data'], {'service_is_readonly': True})

        def return_false():
            return False

        with mock.patch(target='wiki.middleware.read_only.service_is_readonly', new=return_false):
            assert_queries = 4 if not settings.WIKI_CODE == 'wiki' else 2
            with self.assertNumQueries(assert_queries):
                response = self.client.get('/_api/frontend/.is_readonly')
            self.assertEqual(loads(response.content)['data'], {'service_is_readonly': False})
