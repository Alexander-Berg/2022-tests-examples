
from wiki.pages.logic import keywords
from intranet.wiki.tests.wiki_tests.common.base_testcase import BaseTestCase


class UpdateKeywordsTest(BaseTestCase):
    def test_update_keywords_added(self):
        page = self.create_page()

        self.assertEqual(page.keywords_list, [])

        keywords.update_keywords(
            page=page,
            user=page.get_authors().first(),
            keywords=['kotiki', 'video'],
        )

        page = self.refresh_objects(page)

        self.assertEqual(page.keywords_list, ['kotiki', 'video'])
