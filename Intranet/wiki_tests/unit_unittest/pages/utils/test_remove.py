
from wiki.pages.models import AbsentPage, ActualityMarkLink, Page, PageLink
from wiki.pages.utils.remove import delete_page
from intranet.wiki.tests.wiki_tests.common.base_testcase import BaseTestCase


class PageRemoveTests(BaseTestCase):
    def test_remove_page(self):
        page = self.create_page(tag='test', body='test text')
        modified_at_before = page.modified_at

        delete_page(page)

        self.assertFalse(Page.active.filter(supertag=page.supertag).exists())
        self.assertGreater(page.modified_at, modified_at_before)

    def test_redirects_removed_after_page_remove(self):
        page = self.create_page(tag='test', body='test text')
        redirect_page = self.create_page(tag='redirect', body='redirect text')
        redirect_page.redirects_to = page
        redirect_page.save()

        delete_page(page)

        self.assertFalse(Page.active.filter(supertag='redirect').exists())

    def test_unlink_removed_page(self):
        page = self.create_page(tag='test', body='test text')
        page_with_link = self.create_page(tag='link', body='link text')
        PageLink(from_page=page_with_link, to_page=page).save()

        delete_page(page)

        self.assertTrue(AbsentPage.objects.filter(from_page=page_with_link, to_supertag='test').exists())
        self.assertFalse(PageLink.objects.filter(from_page=page_with_link, to_page=page).exists())

    def test_actuality_marks_removed_after_page_remove(self):
        deprecated_page = self.create_page(tag='deprecated', body='deprecated text')
        actual_page = self.create_page(tag='actual', body='actual text')
        ActualityMarkLink(page=deprecated_page, actual_page=actual_page).save()

        delete_page(actual_page)

        self.assertEqual(ActualityMarkLink.objects.count(), 0)
