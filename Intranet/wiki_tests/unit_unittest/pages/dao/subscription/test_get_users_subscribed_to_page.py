from wiki.pages.dao.subscription import get_users_subscribed_to_page
from wiki.pages.models import PageWatch
from intranet.wiki.tests.wiki_tests.common.fixture import FixtureMixin
from intranet.wiki.tests.wiki_tests.common.wiki_django_testcase import WikiDjangoTestCase


class GetUsersSubscribedToPageTestCase(FixtureMixin, WikiDjangoTestCase):
    def test_trivial_filter_page_watches_trivial(self):
        page = self.create_page()
        self.assertEqual([], list(get_users_subscribed_to_page(page)))

    def test_two_users(self):
        page = self.create_page()
        xxxx = self.get_or_create_user('xxxx')
        pw = PageWatch(user=xxxx.username, page=page)
        pw.save()
        self.assertEqual([xxxx], list(get_users_subscribed_to_page(page)))

    def test_subscribed_to_cluster(self):
        page = self.create_page()
        pw_cluster = PageWatch(user=page.get_authors().first().username, page=page, is_cluster=False)
        pw_cluster.save()
        xxxx = self.get_or_create_user('xxxx')
        pw_not_cluster = PageWatch(user=xxxx.username, page=page, is_cluster=True)
        pw_not_cluster.save()
        self.assertEqual([xxxx], list(get_users_subscribed_to_page(page, subscribed_to_cluster=True)))
        self.assertEqual(
            [page.get_authors().first()], list(get_users_subscribed_to_page(page, subscribed_to_cluster=False))
        )
