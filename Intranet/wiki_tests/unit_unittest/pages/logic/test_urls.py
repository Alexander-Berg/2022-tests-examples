
from django.test.utils import override_settings
from pretend import stub

from wiki.pages.logic.urls import url_to_wiki_page_in_frontend
from intranet.wiki.tests.wiki_tests.common.unittest_base import BaseApiTestCase


class PageUrlsLogicTestCase(BaseApiTestCase):
    @override_settings(WIKI_PROTOCOL='https', NGINX_HOST='somehost')
    def test_it_returns_url_to_page(self):
        page = stub(supertag='SomePage')
        self.assertEqual('https://somehost/SomePage/', url_to_wiki_page_in_frontend(page))
        self.assertEqual('https://somehost/SomePage/.edit', url_to_wiki_page_in_frontend(page, action='edit'))
        self.assertEqual('https://somehost/SomePage/?a=b', url_to_wiki_page_in_frontend(page, query_args={'a': 'b'}))
        self.assertEqual(
            'https://somehost/SomePage/.edit?a=b',
            url_to_wiki_page_in_frontend(page, query_args={'a': 'b'}, action='edit'),
        )
