from wiki.pages.dao.subscription import filter_pages_watches
from wiki.pages.models import PageWatch
from intranet.wiki.tests.wiki_tests.common.fixture import FixtureMixin
from intranet.wiki.tests.wiki_tests.common.wiki_django_testcase import WikiDjangoTestCase


class FilterPageWatchesTestCase(FixtureMixin, WikiDjangoTestCase):
    def test_filter_page_watches_trivial(self):
        page = self.create_page()
        pw = PageWatch(user=page.get_authors().first().username, page=page)
        pw.save()
        self.assertDictEqual(filter_pages_watches([page]), {page: [pw]})

    def test_trivial_filter(self):
        page = self.create_page()
        self.assertDictEqual(filter_pages_watches([page]), {page: []})
