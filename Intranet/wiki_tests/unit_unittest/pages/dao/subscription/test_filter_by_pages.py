from wiki.pages.dao.subscription import _filter_by_pages_and_user
from wiki.pages.models import PageWatch
from intranet.wiki.tests.wiki_tests.common.fixture import FixtureMixin
from intranet.wiki.tests.wiki_tests.common.wiki_django_testcase import WikiDjangoTestCase


class FilterByPagesTestCase(FixtureMixin, WikiDjangoTestCase):
    def test_filter_by_pages_and_user_non_empty(self):
        page = self.create_page()
        pw = PageWatch(user=page.get_authors().first().username, page=page)
        pw.save()
        self.assertEqual([pw], list(_filter_by_pages_and_user([page], page.get_authors().first())))

    def test_filter_by_pages_and_user_empty(self):
        page = self.create_page()
        self.assertEqual([], list(_filter_by_pages_and_user([page], page.get_authors().first())))

    def test_filter_by_unsubscribed_user(self):
        page = self.create_page()
        user = self.get_or_create_user('xxxxxx')
        self.assertEqual([], list(_filter_by_pages_and_user([page], user)))
