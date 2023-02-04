from django.conf import settings
from ujson import loads

from wiki import access as wiki_access
from intranet.wiki.tests.wiki_tests.common.unittest_base import BaseApiTestCase


class APIExplainLinksTest(BaseApiTestCase):
    def setUp(self):
        super(APIExplainLinksTest, self).setUp()
        self.setUsers()
        self.setGroups()
        self.setGroupMembers()
        self.client.login('thasonic')

    def test_explain_links(self):
        root_page = self.create_page(tag='root')
        page1 = self.create_page(tag='Owner/Main')
        page2 = self.create_page(tag='Limited/Main')
        page3 = self.create_page(tag='Common/Main')
        page4 = self.create_page(tag='Owner/Redirect')
        page5 = self.create_page(tag='Limited/Redirect')
        page6 = self.create_page(tag='Common/Redirect')

        page4.redirects_to = page1
        page4.save()
        page5.redirects_to = page2
        page5.save()
        page6.redirects_to = page3
        page6.save()

        wiki_access.set_access(
            page1, wiki_access.TYPES.OWNER, self.user_thasonic, staff_models=[self.user_thasonic.staff]
        )
        wiki_access.set_access(
            page2,
            wiki_access.TYPES.RESTRICTED,
            self.user_thasonic,
            staff_models=[self.user_thasonic.staff, self.user_kolomeetz.staff],
        )

        self.client.login('kolomeetz')

        request_url = '{api_url}/.explainlinks'.format(api_url=self.api_url)
        with self.assertNumQueries(17 if settings.IS_INTRANET else 57):
            r = self.client.post(
                request_url,
                {
                    'links': {
                        root_page.supertag: [
                            'Owner/Main',
                            'Limited/Main',
                            'Common/Main',
                            'Owner/Redirect',
                            'Limited/Redirect',
                            'Common/Redirect',
                        ]
                    }
                },
                content_type='application/json',
            )
        self.assertEqual(200, r.status_code)

        data = loads(r.content)['data']

        self.assertTrue('links' in data)
        self.assertEqual(
            data['links'],
            {
                'Owner/Main': 0,
                'Limited/Main': 2,
                'Common/Main': 1,
                'Owner/Redirect': 0,
                'Limited/Redirect': 2,
                'Common/Redirect': 1,
            },
        )

    def test_external_links_ignored(self):
        root_page = self.create_page(tag='root')
        self.create_page(tag='Athos')
        self.create_page(tag='Porthos')

        request_url = '{api_url}/.explainlinks'.format(api_url=self.api_url)
        r = self.client.post(
            request_url,
            {
                'links': {
                    root_page.supertag: [
                        'Athos',
                        'https://%s/Porthos' % settings.NGINX_HOST,
                        'http://%s/Aramis' % settings.NGINX_HOST,
                        'http://www.yandex.ru/',
                    ]
                }
            },
            content_type='application/json',
        )
        self.assertEqual(200, r.status_code)

        data = loads(r.content)['data']

        self.assertTrue('links' in data)
        self.assertEqual(
            data['links'],
            {
                'Athos': 1,
                'https://%s/Porthos' % settings.NGINX_HOST: 1,
                'http://%s/Aramis' % settings.NGINX_HOST: -1,
            },
        )

    def test_stripped_slashes(self):
        root_page = self.create_page(tag='root')
        self.create_page(tag='Athos')
        self.create_page(tag='Porthos')
        self.create_page(tag='Aramis')

        request_url = '{api_url}/.explainlinks'.format(api_url=self.api_url)
        r = self.client.post(
            request_url,
            {
                'links': {
                    root_page.supertag: [
                        '/Athos',
                        'Porthos/',
                        '/Aramis/',
                        'Aramis',
                    ]
                }
            },
            content_type='application/json',
        )
        self.assertEqual(200, r.status_code)

        data = loads(r.content)['data']

        self.assertTrue('links' in data)
        self.assertEqual(
            data['links'],
            {
                '/Athos': 1,
                'Porthos/': 1,
                '/Aramis/': 1,
                'Aramis': 1,
            },
        )

    def test_not_found_pages(self):
        root_page = self.create_page(tag='root')
        self.create_page(tag='Something')

        request_url = '{api_url}/.explainlinks'.format(api_url=self.api_url)
        r = self.client.post(
            request_url,
            {
                'links': {
                    root_page.supertag: [
                        'Something',
                        'Wrong',
                    ]
                }
            },
            content_type='application/json',
        )
        self.assertEqual(200, r.status_code)

        data = loads(r.content)['data']

        self.assertTrue('links' in data)
        self.assertEqual(
            data['links'],
            {
                'Something': 1,
                'Wrong': -1,
            },
        )

    def test_ignored_handlers(self):
        root_page = self.create_page(tag='root')
        self.create_page(tag='Something')
        self.create_page(tag='Something/Else')

        request_url = '{api_url}/.explainlinks'.format(api_url=self.api_url)
        r = self.client.post(
            request_url,
            {
                'links': {
                    root_page.supertag: [
                        'http://%s/Something/.show' % settings.NGINX_HOST,
                        '/Something/Else/.ru-en',
                    ]
                }
            },
            content_type='application/json',
        )
        self.assertEqual(200, r.status_code)

        data = loads(r.content)['data']

        self.assertTrue('links' in data)
        self.assertEqual(
            data['links'],
            {
                'http://%s/Something/.show' % settings.NGINX_HOST: -1,
                '/Something/Else/.ru-en': -1,
            },
        )

    def test_main_page(self):
        root_page = self.create_page(tag='root')
        self.create_page(tag=settings.MAIN_PAGE)

        request_url = '{api_url}/.explainlinks'.format(api_url=self.api_url)
        r = self.client.post(request_url, {'links': {root_page.supertag: ['/']}}, content_type='application/json')
        self.assertEqual(200, r.status_code)

        data = loads(r.content)['data']

        self.assertTrue('links' in data)
        self.assertEqual(
            data['links'],
            {
                '/': 1,
            },
        )

    def test_links_with_hashes(self):
        root_page = self.create_page(tag='root')
        self.create_page(tag='Bolek')
        self.create_page(tag='Lolek')

        request_url = '{api_url}/.explainlinks'.format(api_url=self.api_url)
        r = self.client.post(
            request_url,
            {
                'links': {
                    root_page.supertag: [
                        'bolek#hello',
                        'bolek#goodbye',
                        '/lolek/.show#hi',
                    ]
                }
            },
            content_type='application/json',
        )
        self.assertEqual(200, r.status_code)

        data = loads(r.content)['data']

        self.assertTrue('links' in data)
        self.assertEqual(
            data['links'],
            {
                'bolek#hello': 1,
                'bolek#goodbye': 1,
                '/lolek/.show#hi': -1,
            },
        )

    def test_urlencoded_links(self):
        root_page = self.create_page(tag='root')
        self.create_page(tag='HR/Филиалы')
        request_url = '{api_url}/.explainlinks'.format(api_url=self.api_url)
        r = self.client.post(
            request_url,
            {
                'links': {
                    root_page.supertag: [
                        'HR/%D0%A4%D0%B8%D0%BB%D0%B8%D0%B0%D0%BB%D1%8B',
                        'HR/%D0%A4%D0%B8%D0%BB%D0%B8%D0%B0%D0%BB%D1%8B#anchor',
                    ]
                }
            },
            content_type='application/json',
        )
        self.assertEqual(200, r.status_code)

        data = loads(r.content)['data']

        self.assertTrue('links' in data)
        self.assertEqual(
            data['links'],
            {
                'HR/%D0%A4%D0%B8%D0%BB%D0%B8%D0%B0%D0%BB%D1%8B': 1,
                'HR/%D0%A4%D0%B8%D0%BB%D0%B8%D0%B0%D0%BB%D1%8B#anchor': 1,
            },
        )
