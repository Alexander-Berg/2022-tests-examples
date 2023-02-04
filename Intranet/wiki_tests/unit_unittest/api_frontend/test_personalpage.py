from unittest import skipIf

from django.conf import settings
from ujson import loads

from wiki.personalisation.user_cluster import personal_cluster
from intranet.wiki.tests.wiki_tests.common.unittest_base import BaseApiTestCase


class APIPersonalPageRedirectTest(BaseApiTestCase):

    create_user_clusters = True

    def setUp(self):
        super(APIPersonalPageRedirectTest, self).setUp()
        self.setUsers()
        self.client.login('thasonic')
        self.user = self.user_thasonic
        self.personal_cluster_tag = personal_cluster(self.user).tag

    def test_personal_page(self):
        assert_queries = 5 if not settings.WIKI_CODE == 'wiki' else 3
        with self.assertNumQueries(assert_queries):
            r = self.client.get('{api_url}/~'.format(api_url=self.api_url))

        self.assertEqual(r.status_code, 200)
        json = loads(r.content)
        self.assertTrue('data' in json)
        self.assertTrue('redirect_to_tag' in json['data'])
        self.assertEqual(json['data']['redirect_to_tag'], self.personal_cluster_tag)

    def test_personal_page_subpage(self):
        assert_queries = 5 if not settings.WIKI_CODE == 'wiki' else 3
        with self.assertNumQueries(assert_queries):
            r = self.client.get('{api_url}/~/whatever'.format(api_url=self.api_url))

        self.assertEqual(r.status_code, 200)
        json = loads(r.content)
        self.assertTrue('data' in json)
        self.assertTrue('redirect_to_tag' in json['data'])
        self.assertEqual(json['data']['redirect_to_tag'], self.personal_cluster_tag + '/whatever')

    def test_personal_page_trailing_slahses(self):
        r = self.client.get('{api_url}/~/'.format(api_url=self.api_url))

        self.assertEqual(r.status_code, 200)
        json = loads(r.content)
        self.assertTrue('data' in json)
        self.assertTrue('redirect_to_tag' in json['data'])
        self.assertEqual(json['data']['redirect_to_tag'], self.personal_cluster_tag)

    def test_personal_page_subpage_trailing_slashes(self):
        r = self.client.get('{api_url}/~/whatever/'.format(api_url=self.api_url))

        self.assertEqual(r.status_code, 200)
        json = loads(r.content)
        self.assertTrue('data' in json)
        self.assertTrue('redirect_to_tag' in json['data'])
        self.assertEqual(json['data']['redirect_to_tag'], self.personal_cluster_tag + '/whatever')

    @skipIf(settings.IS_BUSINESS, 'wait for fixing for business')
    def test_others_personal_page(self):
        r = self.client.get('{api_url}/~volozh'.format(api_url=self.api_url))

        self.assertEqual(r.status_code, 200)
        json = loads(r.content)
        self.assertTrue('data' in json)
        self.assertTrue('redirect_to_tag' in json['data'])
        self.assertEqual(json['data']['redirect_to_tag'], personal_cluster(self.user_volozh).tag)

    @skipIf(settings.IS_BUSINESS, 'wait for fixing for business')
    def test_others_personal_page_subpage(self):
        r = self.client.get('{api_url}/~volozh/whatever'.format(api_url=self.api_url))

        self.assertEqual(r.status_code, 200)
        json = loads(r.content)
        self.assertTrue('data' in json)
        self.assertTrue('redirect_to_tag' in json['data'])
        self.assertEqual(json['data']['redirect_to_tag'], personal_cluster(self.user_volozh).tag + '/whatever')

    def test_others_personal_page_wrong_login(self):
        r = self.client.get('{api_url}/~gorirra'.format(api_url=self.api_url))

        self.assertEqual(r.status_code, 404)

    def test_personal_page_redirected(self):
        my_new_page = self.create_page(tag='ЭтоМояСтраница')
        my_page = personal_cluster(self.user)
        my_page.redirects_to = my_new_page
        my_page.save()

        r = self.client.get('{api_url}/~/whatever'.format(api_url=self.api_url))

        self.assertEqual(r.status_code, 200)
        json = loads(r.content)
        self.assertTrue('data' in json)
        self.assertTrue('redirect_to_tag' in json['data'])
        self.assertEqual(json['data']['redirect_to_tag'], 'ЭтоМояСтраница/whatever')
