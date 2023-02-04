from django.utils import timezone

from wiki.pages.models import Page
from wiki.ping.tasks import DebugTask
from wiki.utils import lock
from intranet.wiki.tests.wiki_tests.common.unittest_base import BaseApiTestCase


class DbFwCase(BaseApiTestCase):
    def setUp(self):
        super(DbFwCase, self).setUp()
        self.page = Page.objects.create(supertag='test', title='test', modified_at=timezone.now())

        assert self.page.id is not None

    def test_db_access(self):
        Page.objects.get(pk=self.page.id)

    def test_lock(self):
        def _lockedFn():
            pass

        lock.execute_with_lock('my_lock', _lockedFn)

    def test_celery(self):
        DebugTask().apply_async()
        assert 1 + 1 == 2
