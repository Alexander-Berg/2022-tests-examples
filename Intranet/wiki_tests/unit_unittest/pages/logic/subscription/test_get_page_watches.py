
from mock import patch

from wiki.pages.logic.subscription import get_page_watches
from intranet.wiki.tests.wiki_tests.common.unittest_base import BaseApiTestCase


class GetPageWatchesTestCase(BaseApiTestCase):
    def test_trivial(self):
        page_page_watches = []
        page = object()
        with patch('wiki.pages.logic.subscription.filter_pages_watches', lambda *args: {page: page_page_watches}):
            self.assertEqual(page_page_watches, get_page_watches(page))
