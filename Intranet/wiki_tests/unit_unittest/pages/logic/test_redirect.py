
from datetime import timedelta

from wiki.pages.logic import redirect as redirect_logic
from wiki.pages.models.page import RedirectLoopException
from wiki.utils import timezone
from intranet.wiki.tests.wiki_tests.common.base_testcase import BaseTestCase


class RedirectLogicTest(BaseTestCase):
    def setUp(self):
        self.setGroups()
        self.page_one = self.create_page(supertag='redirect_from')
        self.page_two = self.create_page(supertag='redirect_to')
        self.page_three = self.create_page(supertag='somepage')
        self.user = self.page_one.get_authors().first()
        super(RedirectLogicTest, self).setUp()

    def test_set_redirect(self):
        redirect_logic.set_redirect(page=self.page_one, user=self.user, destination=self.page_two)

        page_one = self.refresh_objects(self.page_one)
        self.assertEqual(page_one.redirects_to, self.page_two)

        redirect_logic.set_redirect(page=self.page_one, user=self.user, destination=self.page_three)

        page_one = self.refresh_objects(self.page_one)
        self.assertEqual(page_one.redirects_to, self.page_three)

    def test_check_redirect_loop_while_set_redirect(self):
        redirect_logic.set_redirect(page=self.page_one, user=self.user, destination=self.page_two)

        redirect_logic.set_redirect(page=self.page_two, user=self.user, destination=self.page_three)

        self.assertRaises(
            RedirectLoopException,
            lambda: redirect_logic.set_redirect(page=self.page_three, user=self.user, destination=self.page_one),
        )

    def test_remove_redirect(self):
        redirect_logic.set_redirect(
            page=self.page_one,
            user=self.user,
            destination=self.page_two,
        )

        redirect_logic.remove_redirect(page=self.page_one, user=self.user)

        page_one = self.refresh_objects(self.page_one)
        self.assertIsNone(page_one.redirects_to)

    def test_set_redirect_changes_modified_at_for_index(self):
        created_at = timezone.now() - timedelta(days=1)
        page = self.create_page(
            supertag='another_page',
            created_at=created_at,
            modified_at=created_at,
            modified_at_for_index=created_at,
        )

        date_before = page.modified_at_for_index

        redirect_logic.set_redirect(page=page, user=self.user, destination=self.page_two)

        page = self.refresh_objects(page)

        self.assertLess(date_before, page.modified_at_for_index)
