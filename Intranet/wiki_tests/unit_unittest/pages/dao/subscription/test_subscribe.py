from wiki.pages.dao.subscription import create_subscriptions
from wiki.pages.models import PageWatch
from intranet.wiki.tests.wiki_tests.common.fixture import FixtureMixin
from intranet.wiki.tests.wiki_tests.common.wiki_django_testcase import WikiDjangoTestCase


class SubscribePageWatchTestCase(FixtureMixin, WikiDjangoTestCase):
    def test_it_fails_if_page_watch_exists(self):
        from django.db import IntegrityError

        page = self.create_page()
        pw = PageWatch(user=page.get_authors().first().username, page=page)
        pw.save()
        with self.assertRaises(IntegrityError):
            create_subscriptions([pw])

    def test_it_creates_page_watch(self):
        page = self.create_page()
        pw = PageWatch(user=page.get_authors().first().username, page=page)
        self.assertEqual(len(PageWatch.objects.all()), 0)
        self.assertEqual([pw], create_subscriptions([pw]))
        self.assertEqual(len(PageWatch.objects.all()), 1)
