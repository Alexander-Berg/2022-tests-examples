from urllib.parse import urlencode

from mock import patch
from ujson import loads

from wiki.api_frontend.views.tree import PagesTree
from wiki.pages.models import Access
from wiki.pages.pages_tree_new import Sort, SortBy
from intranet.wiki.tests.wiki_tests.common.unittest_base import BaseApiTestCase


class TreeViewTest(BaseApiTestCase):
    def setUp(self):
        super(TreeViewTest, self).setUp()
        self.setUsers()
        self.client.login(self.user_thasonic.username)
        self.other_user = self.user_chapson.staff

    def test_nonexistent_page(self):
        response = self.client.get('{api_url}/nonexistent/.tree'.format(api_url=self.api_url))
        self.assertEqual(200, response.status_code)

    def test_closed_page(self):
        page = self.create_page(
            supertag='closed',
        )
        page.authors.add(self.other_user.user)
        Access(
            page=page,
            staff=self.other_user,
            is_owner=True,
        ).save()

        response = self.client.get('{api_url}/closed/.tree'.format(api_url=self.api_url))
        self.assertEqual(200, response.status_code)

    def test_page_authors(self):
        page = self.create_page(
            supertag='root',
        )

        self.create_page(
            supertag='root/page_with_one_author',
        )

        page2 = self.create_page(
            supertag='root/page_with_two_authors',
        )
        page2.authors.add(self.other_user.user)

        response = self.client.get('{api_url}/{page}/.tree'.format(api_url=self.api_url, page=page.supertag))
        self.assertEqual(200, response.status_code)
        json = loads(response.content)
        self.assertEqual(2, json['data']['subpages_count'])

        response = self.client.get(
            '{api_url}/{page}/.tree?{params}'.format(
                api_url=self.api_url, page=page.supertag, params=urlencode({'authors': self.other_user.username})
            )
        )
        self.assertEqual(200, response.status_code)
        json = loads(response.content)
        self.assertEqual(1, json['data']['subpages_count'])

        response = self.client.get(
            '{api_url}/{page}/.tree?{params}'.format(
                api_url=self.api_url,
                page=page.supertag,
                params=urlencode({'authors': '{},{}'.format(self.other_user.username, self.user_thasonic.username)}),
            )
        )
        self.assertEqual(200, response.status_code)
        json = loads(response.content)
        self.assertEqual(2, json['data']['subpages_count'])

        response = self.client.get(
            '{api_url}/{page}/.tree?{params}'.format(
                api_url=self.api_url,
                page=page.supertag,
                params=urlencode({'authors': '{},{}'.format(self.other_user.username, self.user_kolomeetz.username)}),
            )
        )
        self.assertEqual(200, response.status_code)
        json = loads(response.content)
        self.assertEqual(1, json['data']['subpages_count'])

    def test_pages_tree_call(self):
        params = {
            'depth': '2',
            'show_redirects': '1',
            'show_grids': '0',
            'show_files': '1',
            'show_owners': '1',
            'show_titles': '0',
            'show_created_at': '0',
            'show_modified_at': '1',
            'sort_by': SortBy.MODIFIED_AT,
            'sort': Sort.DESC,
        }

        class PagesTreeMock(PagesTree):
            def __init__(self, **kwargs):
                kwargs.pop('expand_subtree_url_builder')
                kwargs.pop('user')
                kwargs.pop('from_yandex_server')
                self.data = kwargs

        with patch('wiki.api_frontend.views.tree.PagesTree', PagesTreeMock):
            response = self.client.get(
                '{api_url}/wiki/api/.tree?{params}'.format(api_url=self.api_url, params=urlencode(params))
            )

            self.assertEqual(200, response.status_code)
            self.assertEqual(
                {
                    'root_supertag': 'wiki/api',
                    'depth': 2,
                    'show_redirects': True,
                    'show_grids': False,
                    'show_files': True,
                    'show_owners': True,
                    'show_titles': False,
                    'show_created_at': False,
                    'show_modified_at': True,
                    'sort_by': SortBy.MODIFIED_AT,
                    'sort': Sort.DESC,
                    'authors': [],
                },
                response.data['data'],
            )
