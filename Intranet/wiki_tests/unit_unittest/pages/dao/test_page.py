from wiki.pages.dao.page import get_page_by_supertag, get_pages_by_supertags, page_exists
from wiki.pages.utils.remove import delete_page
from intranet.wiki.tests.wiki_tests.common.fixture import FixtureMixin
from intranet.wiki.tests.wiki_tests.common.wiki_django_testcase import WikiDjangoTestCase


class PageDaoTestCase(FixtureMixin, WikiDjangoTestCase):
    def test_get_page_by_supertag_existing(self):
        page = self.create_page()
        got_page = get_page_by_supertag(page.supertag)
        self.assertEqual(page, got_page)

    def test_get_page_by_supertag_missing(self):
        got_page = get_page_by_supertag('missing page')
        self.assertEqual(None, got_page)

    def test_get_page_by_supertag_inactive_not_included(self):
        page = self.create_page(status=0)
        got_page = get_page_by_supertag(page.supertag)
        self.assertEqual(None, got_page)

    def test_get_page_by_supertag_inactive_included(self):
        page = self.create_page(status=0)
        got_page = get_page_by_supertag(page.supertag, include_inactive=True)
        self.assertEqual(page, got_page)

    def test_get_pages_by_supertags(self):
        # ни одной не нашлось
        got_pages = get_pages_by_supertags(['a', ''])
        self.assertEqual(len(got_pages), 0)

        # одна нашлась, вторая нет
        page = self.create_page()
        got_pages = get_pages_by_supertags([page.supertag, page.supertag + '-missing'])
        self.assertEqual(len(got_pages), 1)
        self.assertEqual(got_pages[0], page)

        page2 = self.create_page(supertag=page.supertag + '2')
        got_pages = get_pages_by_supertags([page.supertag, page2.supertag])
        self.assertEqual(len(got_pages), 2)
        self.assertTrue(page in got_pages)
        self.assertTrue(page2 in got_pages)

    def test_page_exists_when_none(self):
        self.assertFalse(page_exists('Lord/Byron'))

    def test_page_exists(self):
        self.create_page(tag='Lord/Byron')
        self.assertTrue(page_exists('Lord/Byron'))

    def test_page_not_exists_after_deleted(self):
        page = self.create_page(tag='Lord/Byron')
        delete_page(page)
        self.assertFalse(page_exists('Lord/Byron'))
