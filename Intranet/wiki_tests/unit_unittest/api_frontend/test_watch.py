from django.conf import settings
from ujson import loads

from wiki.pages.logic.subscription import create_watch
from intranet.wiki.tests.wiki_tests.common.unittest_base import BaseApiTestCase


class APIWatchTest(BaseApiTestCase):
    def setUp(self):
        super(APIWatchTest, self).setUp()
        self.setUsers(use_legacy_subscr_favor=True)
        self.client.login('thasonic')
        self.user = self.user_thasonic

    def test_watch(self):
        page = self.create_page(tag='Уотч')

        request_url = '{api_url}/{page_supertag}/.watch'.format(api_url=self.api_url, page_supertag=page.supertag)

        assert_queries = 51 if not settings.WIKI_CODE == 'wiki' else 9
        with self.assertNumQueries(assert_queries):
            response = self.client.post(request_url)
        self.assertEqual(200, response.status_code)
        data = loads(response.content)['data']
        self.assertEqual(data['pages_count'], 1)

        response = self.client.post(request_url)
        self.assertEqual(200, response.status_code)
        data = loads(response.content)['data']
        self.assertEqual(data['pages_count'], 0)

        request_url = '{api_url}/{page_supertag}/.unwatch'.format(api_url=self.api_url, page_supertag=page.supertag)

        response = self.client.post(request_url)
        self.assertEqual(200, response.status_code)
        data = loads(response.content)['data']
        self.assertEqual(data['pages_count'], 1)

        response = self.client.post(request_url)
        self.assertEqual(200, response.status_code)
        data = loads(response.content)['data']
        self.assertEqual(data['pages_count'], 0)

    def test_watch_to_root_cluster_page_only(self):
        page = self.create_page(tag='МассУотч')
        page1 = self.create_page(tag='МассУотч/Раз')
        page2 = self.create_page(tag='МассУотч/Два')

        request_url = '{api_url}/{page_supertag}/.masswatch'.format(api_url=self.api_url, page_supertag=page.supertag)
        response = self.client.post(request_url)
        self.assertEqual(200, response.status_code)

        request_url = '{api_url}/{page_supertag}/.watch'.format(api_url=self.api_url, page_supertag=page.supertag)
        request_data = {'unwatch_children': True}

        response = self.client.post(request_url, request_data)
        self.assertEqual(200, response.status_code)
        data = loads(response.content)['data']
        self.assertEqual(data['pages_count'], 1)

        self.assertTrue(page.pagewatch_set.filter(user=self.user_thasonic.username).exists())
        self.assertFalse(page1.pagewatch_set.filter(user=self.user_thasonic.username).exists())
        self.assertFalse(page2.pagewatch_set.filter(user=self.user_thasonic.username).exists())

    def test_mass_watch(self):
        page = self.create_page(tag='МассУотч')
        self.create_page(tag='МассУотч/Раз')
        self.create_page(tag='МассУотч/Два')

        request_url = '{api_url}/{page_supertag}/.masswatch'.format(api_url=self.api_url, page_supertag=page.supertag)

        assert_queries = 59 if not settings.WIKI_CODE == 'wiki' else 17
        with self.assertNumQueries(assert_queries):
            response = self.client.post(request_url)
        self.assertEqual(200, response.status_code)
        data = loads(response.content)['data']
        self.assertEqual(data['pages_count'], 3)

        response = self.client.post(request_url)
        self.assertEqual(200, response.status_code)
        data = loads(response.content)['data']
        self.assertEqual(data['pages_count'], 0)

        request_url = '{api_url}/{page_supertag}/.massunwatch'.format(api_url=self.api_url, page_supertag=page.supertag)

        response = self.client.post(request_url)
        self.assertEqual(200, response.status_code)
        data = loads(response.content)['data']
        self.assertEqual(data['pages_count'], 3)

        response = self.client.post(request_url)
        self.assertEqual(200, response.status_code)
        data = loads(response.content)['data']
        self.assertEqual(data['pages_count'], 0)

    def test_mass_watch_more_users(self):
        page = self.create_page(tag='cluster_page')
        self.create_page(tag='cluster_page/page1')
        self.create_page(tag='cluster_page/page2')

        request_url = '{api_url}/{page_supertag}/.masswatch'.format(api_url=self.api_url, page_supertag=page.supertag)

        page_data = {
            'uids': [self.user_thasonic.staff.uid, self.user_chapson.staff.uid, self.user_kolomeetz.staff.uid],
            'comment': 'You subscribed',
        }

        assert_queries = 77 if not settings.WIKI_CODE == 'wiki' else 37
        with self.assertNumQueries(assert_queries):
            response = self.client.post(request_url, data=page_data)
        self.assertEqual(200, response.status_code)
        data = loads(response.content)['data']
        self.assertEqual(data['pages_count'], 9)

        self.assertTrue(page.pagewatch_set.filter(user=self.user_thasonic.username).exists())
        self.assertTrue(page.pagewatch_set.filter(user=self.user_chapson.username).exists())
        self.assertTrue(page.pagewatch_set.filter(user=self.user_kolomeetz.username).exists())

        response = self.client.post(request_url, data=page_data)
        self.assertEqual(200, response.status_code)
        data = loads(response.content)['data']
        self.assertEqual(data['pages_count'], 0)

    def test_mass_watch_with_empty_uids(self):
        page = self.create_page(tag='cluster_page')
        self.create_page(tag='cluster_page/page1')
        self.create_page(tag='cluster_page/page2')

        request_url = '{api_url}/{page_supertag}/.masswatch'.format(api_url=self.api_url, page_supertag=page.supertag)

        page_data = {'uids': []}

        r = self.client.post(request_url, data=page_data)
        self.assertEqual(200, r.status_code)
        data = loads(r.content)['data']
        self.assertEqual(data['pages_count'], 3)

        self.assertTrue(page.pagewatch_set.filter(user=self.user_thasonic.username).exists())

        r = self.client.post(request_url, data=page_data)
        self.assertEqual(200, r.status_code)
        data = loads(r.content)['data']
        self.assertEqual(data['pages_count'], 0)

    def test_watch_generates_event_on_subscription(self):
        page = self.create_page(tag='Уотч')

        num_before_request = self.getNumEvents('watch', 'thasonic', page.supertag)

        request_url = '{api_url}/{page_supertag}/.watch'.format(api_url=self.api_url, page_supertag=page.supertag)
        self.client.post(request_url)

        num_after_request = self.getNumEvents('watch', 'thasonic', page.supertag)

        self.assertEqual(num_after_request, num_before_request + 1)

    def test_watch_doesnt_generate_event_if_was_already_subscribed(self):
        page = self.create_page(tag='Уотч')

        request_url = '{api_url}/{page_supertag}/.watch'.format(api_url=self.api_url, page_supertag=page.supertag)
        self.client.post(request_url)

        num_before_request = self.getNumEvents('watch', 'thasonic', page.supertag)

        self.client.post(request_url)

        num_after_request = self.getNumEvents('watch', 'thasonic', page.supertag)

        self.assertEqual(num_after_request, num_before_request)

    def test_unwatch_generates_event_on_unsubscription(self):
        page = self.create_page(tag='Уотч')

        request_url = '{api_url}/{page_supertag}/.watch'.format(api_url=self.api_url, page_supertag=page.supertag)
        self.client.post(request_url)

        num_before_request = self.getNumEvents('unwatch', 'thasonic', page.supertag)

        request_url = '{api_url}/{page_supertag}/.unwatch'.format(api_url=self.api_url, page_supertag=page.supertag)
        self.client.post(request_url)

        num_after_request = self.getNumEvents('unwatch', 'thasonic', page.supertag)

        self.assertEqual(num_after_request, num_before_request + 1)

    def test_unwatch_doesnt_generate_event_if_was_not_subscribed(self):
        page = self.create_page(tag='Уотч')

        num_before_request = self.getNumEvents('unwatch', 'thasonic', page.supertag)

        request_url = '{api_url}/{page_supertag}/.unwatch'.format(api_url=self.api_url, page_supertag=page.supertag)
        self.client.post(request_url)

        num_after_request = self.getNumEvents('unwatch', 'thasonic', page.supertag)

        self.assertEqual(num_after_request, num_before_request)

    def test_watchers(self):
        page = self.create_page(tag='Уотч')

        request_url = '{api_url}/{page_supertag}/.watchers'.format(api_url=self.api_url, page_supertag=page.supertag)

        response = self.client.get(request_url)
        data = loads(response.content)['data']

        self.assertEqual(200, response.status_code)
        self.assertEqual([], data)

        create_watch(page, self.user_thasonic, False)
        create_watch(page, self.user_chapson, False)

        response = self.client.get(request_url)
        data = loads(response.content)['data']

        self.assertEqual(200, response.status_code)
        self.assertEqual(2, len(data))
        self.assertEqual('chapson', data[0]['login'])
        self.assertEqual('thasonic', data[1]['login'])
