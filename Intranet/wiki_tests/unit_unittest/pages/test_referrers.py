
from wiki.pages.logic.referrers import get_links_from_external_pages, get_links_from_wiki_pages
from wiki.pages.models import PageLink, Referer
from intranet.wiki.tests.wiki_tests.common.base_testcase import BaseTestCase


class LinksFromWikiPagesTest(BaseTestCase):
    def test(self):
        to_page = self.create_page(tag='to_page')
        from_page_1 = self.create_page(tag='from_page_1')
        from_page_2 = self.create_page(tag='from_page_2')

        PageLink(from_page=from_page_1, to_page=to_page).save()
        PageLink(from_page=from_page_2, to_page=to_page).save()

        with self.assertNumQueries(1):
            wiki_pages = get_links_from_wiki_pages(to_page)

        self.assertEqual(2, len(wiki_pages))
        self.assertEqual('from_page_1', wiki_pages[0].tag)
        self.assertEqual('from_page_2', wiki_pages[1].tag)


class LinksFromExternalPagesTest(BaseTestCase):
    def _set_referrers(self, to_page, referer, ref_count):
        for i in range(ref_count):
            Referer(page=to_page, referer=referer).save()

    def test(self):
        to_page = self.create_page(tag='to_page')

        self._set_referrers(to_page, 'http://search.yandex-team.ru/search?text=blabla', 3)
        self._set_referrers(to_page, 'http://search.yandex-team.ru/search?text=foo', 2)

        with self.assertNumQueries(1):
            external_pages = get_links_from_external_pages(to_page)

        self.assertEqual(
            [
                ('http://search.yandex-team.ru/search?text=blabla', 3),
                ('http://search.yandex-team.ru/search?text=foo', 2),
            ],
            external_pages,
        )
