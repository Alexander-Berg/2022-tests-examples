from wiki.pages.dao.subscription import filter_user_page_watches
from wiki.pages.models import PageWatch
from intranet.wiki.tests.wiki_tests.common.fixture import FixtureMixin
from intranet.wiki.tests.wiki_tests.common.wiki_django_testcase import WikiDjangoTestCase


class FilterUserPageWatchesTestCase(FixtureMixin, WikiDjangoTestCase):
    def test_filter_trivial(self):
        thasonic = self.get_or_create_user('thasonic')
        self.assertEqual(set(), filter_user_page_watches(thasonic))

    def test_filter_by_pages(self):
        page = self.create_page()
        pw = PageWatch(user=page.get_authors().first().username, page=page)
        pw.save()

        self.assertEqual(filter_user_page_watches(page.get_authors().first(), pages=[page]), set([pw]))

    def test_filter_by_supertags(self):
        page = self.create_page()
        pw = PageWatch(user=page.get_authors().first().username, page=page)
        pw.save()

        self.assertEqual(filter_user_page_watches(page.get_authors().first(), supertags=[page.supertag]), set([pw]))

    def test_nothing_to_filter(self):
        page = self.create_page()

        self.assertEqual(filter_user_page_watches(page.get_authors().first(), supertags=[page.supertag]), set([]))

        self.assertEqual(filter_user_page_watches(page.get_authors().first(), pages=[page]), set([]))

        self.assertEqual(
            set([]), filter_user_page_watches(page.get_authors().first(), pages=[page], supertags=[page.supertag])
        )
