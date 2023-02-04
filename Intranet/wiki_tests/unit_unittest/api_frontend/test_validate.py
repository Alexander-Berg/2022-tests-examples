
from django.conf import settings
from ujson import loads

from intranet.wiki.tests.wiki_tests.common.unittest_base import BaseApiTestCase


class APIValidateTagTest(BaseApiTestCase):
    """
    Тесты для TagValidationView.
    """

    def setUp(self):
        super(APIValidateTagTest, self).setUp()
        self.setUsers()
        self.client.login('chapson')

    def _test(self, tag):
        request_url = '{api_url}/.validate_tag?tag={tag}'.format(api_url=self.api_url, tag=tag)
        response = self.client.get(request_url)

        self.assertEqual(200, response.status_code)

        parsed_data = loads(response.content)['data']
        return parsed_data['success']

    def test_valid(self):
        assert_queries = 44 if not settings.WIKI_CODE == 'wiki' else 2
        with self.assertNumQueries(assert_queries):
            self.assertTrue(self._test('valid:tag/tag-valid') is True)

    def test_invalid(self):
        request_url = '{api_url}/.validate_tag?tag={tag}'.format(api_url=self.api_url, tag='invalid tag/tag$invalid#')
        assert_queries = 44 if not settings.WIKI_CODE == 'wiki' else 2
        with self.assertNumQueries(assert_queries):
            response = self.client.get(request_url)
        self.assertEqual(409, response.status_code)

    def test_without_tag_parameter(self):
        request_url = '{api_url}/.validate_tag'.format(api_url=self.api_url)
        assert_queries = 44 if not settings.WIKI_CODE == 'wiki' else 2
        with self.assertNumQueries(assert_queries):
            response = self.client.get(request_url)
        self.assertEqual(409, response.status_code)
