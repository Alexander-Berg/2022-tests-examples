from wiki.pages.models import Page, PageWatch
from wiki.utils import timezone
from intranet.wiki.tests.wiki_tests.common.base_testcase import BaseTestCase
from intranet.wiki.tests.wiki_tests.common.ddf_compat import get


class BaseWatchTestCase(BaseTestCase):
    """
    Extension of BaseTestCase with a few helpers for subscription related
    tests.
    """

    def assertSubscribed(self, login, supertag):
        page = Page.objects.get(supertag=supertag)
        subscriptions_qs = page.pagewatch_set.filter(user=login)
        self.assertTrue(subscriptions_qs.exists())

    def assertNotSubscribed(self, login, supertag):
        page = Page.objects.get(supertag=supertag)
        subscriptions_qs = page.pagewatch_set.filter(user=login)
        self.assertFalse(subscriptions_qs.exists())

    def assertClusterSubscription(self, login, supertag):
        page = Page.objects.get(supertag=supertag)
        subscription = page.pagewatch_set.filter(user=login)[0]
        self.assertTrue(subscription.is_cluster)

    def subscribe(self, login, supertag, is_cluster=False):
        return get(
            PageWatch,
            user=login,
            page=Page.objects.get(supertag=supertag),
            created_at=timezone.now(),
            is_cluster=is_cluster,
        )
