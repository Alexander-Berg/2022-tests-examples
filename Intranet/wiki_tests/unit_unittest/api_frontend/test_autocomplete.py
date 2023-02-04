
from django.conf import settings
from ujson import loads

from wiki import access as wiki_access
from wiki.api_core.utils import API_USER_ACCESS
from wiki.pages.access import access_status as wiki_access_status
from intranet.wiki.tests.wiki_tests.common.unittest_base import BaseApiTestCase


class APIPageSupertagAutocompleteHandlerTest(BaseApiTestCase):
    """
    Tests for api autocomplete handler
    """

    def setUp(self):
        super(APIPageSupertagAutocompleteHandlerTest, self).setUp()
        self.setUsers()
        self.client.login('thasonic')

    def test_autocomplete_works(self):
        """
        simple autocomplete
        """

        self.create_page(
            supertag='autocomplete0',
            tag='АвтоКомплит0',
        )

        self.create_page(
            supertag='autocomplete1',
            tag='АвтоКомплит1',
        )

        self.create_page(
            supertag='autocomplete2',
            tag='АвтоКомплит2',
        )

        request_url = '{api_url}/.autocomplete?tag={tagstart}&offset=0&limit=10'.format(
            api_url=self.api_url, tagstart='autoco'
        )
        response = self.client.get(request_url)

        resp_data = loads(response.content)['data']

        self.assertEqual(resp_data['total'], 3)
        self.assertEqual(
            resp_data['data'][0]['user_access'], API_USER_ACCESS.get_name(wiki_access_status.ACCESS_COMMON)
        )
        self.assertEqual(resp_data['data'][0]['tag'], 'АвтоКомплит0')
        self.assertEqual(resp_data['data'][0]['url'], '/autocomplete0')

    def test_only_one_level_returned(self):
        """
        autocomplete returns only domains of same level
        """

        self.create_page(
            supertag='autocomplete0',
        )

        self.create_page(
            supertag='autocomplete2',
        )

        self.create_page(
            supertag='level1/autocomplete1',
        )

        self.create_page(
            supertag='level1/autocomplete1/subpage',
        )

        request_url = '{api_url}/.autocomplete?tag={tagstart}&offset=0&limit=10'.format(
            api_url=self.api_url, tagstart='autoco'
        )
        response = self.client.get(request_url)

        resp_data = loads(response.content)['data']

        self.assertEqual(resp_data['total'], 2)

        request_url = '{api_url}/.autocomplete?tag={tagstart}&offset=0&limit=10'.format(
            api_url=self.api_url, tagstart='level1/autoco'
        )

        assert_queries = 8 if not settings.WIKI_CODE == 'wiki' else 6
        with self.assertNumQueries(assert_queries):
            response = self.client.get(request_url)

        resp_data = loads(response.content)['data']

        self.assertEqual(resp_data['total'], 1)

    def test_only_accessible_returns(self):
        """
        autocomplete returns only accessible for user pages
        """
        self.create_page(
            supertag='autocomplete0',
        )

        self.create_page(
            supertag='autocomplete1',
        )

        p3 = self.create_page(supertag='autocomplete2', authors_to_add=[self.user_chapson])

        wiki_access.set_access(p3, wiki_access.TYPES.OWNER, self.user_chapson)

        request_url = '{api_url}/.autocomplete?tag={tagstart}&offset=0&limit=10'.format(
            api_url=self.api_url, tagstart='autoco'
        )
        response = self.client.get(request_url)

        resp_data = loads(response.content)['data']

        self.assertEqual(resp_data['total'], 2)
        self.assertEqual(
            resp_data['data'][0]['user_access'], API_USER_ACCESS.get_name(wiki_access_status.ACCESS_COMMON)
        )

    def test_param_with_absolute_url(self):
        """
        Проверить случай, когда в значении параметра tag передается авбсолютный url вместо обычного тэга.
        """

        self.create_page(
            supertag='autocomplete0',
        )

        self.create_page(
            supertag='autocomplete2',
        )

        self.create_page(
            supertag='level1/autocomplete1',
        )

        self.create_page(
            supertag='level1/autocomplete1/subpage',
        )

        request_url = '{api_url}/.autocomplete?tag={tagstart}&offset=0&limit=10'.format(
            api_url=self.api_url, tagstart='https://wiki.yandex-team.ru/autoco'
        )
        response = self.client.get(request_url)

        resp_data = loads(response.content)['data']

        self.assertEqual(resp_data['total'], 2)

        request_url = '{api_url}/.autocomplete?tag={tagstart}&offset=0&limit=10'.format(
            api_url=self.api_url, tagstart='https://wiki.yandex-team.ru/level1/autoco'
        )

        assert_queries = 8 if not settings.WIKI_CODE == 'wiki' else 6
        with self.assertNumQueries(assert_queries):
            response = self.client.get(request_url)

        resp_data = loads(response.content)['data']

        self.assertEqual(resp_data['total'], 1)
