
from mock import patch
from pretend import stub

from wiki.pages.logic.subscription import is_subscribed_to
from intranet.wiki.tests.wiki_tests.common.unittest_base import BaseApiTestCase


class IsSubscribedToTestCase(BaseApiTestCase):
    def test_tags_true_trivial(self):
        tags = ['a', 'b', 'c']

        def filter_page_watches(user, pages=None, supertags=None):
            return [object(), object(), object()] if supertags == tags else []

        with patch('wiki.pages.logic.subscription.filter_user_page_watches', filter_page_watches):
            self.assertEqual(True, is_subscribed_to(stub(), tags=tags))

    def test_tags_false_trivial(self):
        tags = ('a', 'b', 'c')

        def filter_page_watches(user, pages=None, supertags=None):
            return []

        with patch('wiki.pages.logic.subscription.filter_user_page_watches', filter_page_watches):
            self.assertEqual(False, is_subscribed_to(stub(), tags=tags))

    def test_tags_into_supertags(self):
        tags = ('антон', 'иван')

        def filter_page_watches(user, pages=None, supertags=None):
            return [object(), object()] if supertags == ['anton', 'ivan'] else []

        with patch('wiki.pages.logic.subscription.filter_user_page_watches', filter_page_watches):
            self.assertEqual(True, is_subscribed_to(stub(), tags=tags))

    def test_pages(self):
        passed_pages = (object(), object())

        def filter_page_watches(user, pages=None, supertags=None):
            return [object(), object()] if pages == passed_pages else []

        with patch('wiki.pages.logic.subscription.filter_user_page_watches', filter_page_watches):
            self.assertEqual(True, is_subscribed_to(stub(), pages=passed_pages))
