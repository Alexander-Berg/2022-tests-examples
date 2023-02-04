from datetime import datetime

import pytz
from mock import patch

from wiki.notifications.models import PageEvent
from wiki.pages.logic import edit as edit_logic
from wiki.pages.models import Page, PageWatch, Revision
from wiki.users.logic import set_user_setting

from intranet.wiki.tests.wiki_tests.common.base_testcase import BaseTestCase

NOW = datetime(2020, 1, 1, 12, 0, 0, tzinfo=pytz.utc)


@patch('wiki.pages.logic.edit.timezone.now', lambda *a, **kw: NOW)
class CreateTest(BaseTestCase):
    def setUp(self):
        self.thasonic = self.get_or_create_user('thasonic')
        self.volozh = self.get_or_create_user('volozh')

    def do_create_page(self, tag):
        edit_logic.create(
            tag=tag,
            user=self.thasonic,
            body='**wow**',
            title='стр',
        )

    def test_create_page_basic(self):
        self.do_create_page(tag='Страница')

        page = Page.objects.get(tag='Страница')

        self.assertEqual(page.get_authors().count(), 1)
        self.assertTrue(self.thasonic in page.get_authors())
        self.assertEqual(page.last_author, self.thasonic)
        self.assertEqual(page.body, '**wow**')
        self.assertEqual(page.title, 'стр')
        self.assertEqual(page.supertag, 'stranica')
        self.assertEqual(page.modified_at, NOW)

    def test_create_page_revision(self):
        self.do_create_page(tag='Страница')

        page = Page.objects.get(tag='Страница')
        revision = Revision.objects.get(page=page)

        self.assertEqual(revision.page, page)
        self.assertEqual(revision.author, self.thasonic)
        self.assertEqual(revision.title, 'стр')
        self.assertEqual(revision.created_at, NOW)
        self.assertEqual(revision.mds_storage_id, page.mds_storage_id)

    def test_create_page_event(self):
        self.do_create_page(tag='Страница')

        page = Page.objects.get(tag='Страница')
        revision = Revision.objects.get(page=page)
        event = PageEvent.objects.get(page=page)

        self.assertEqual(event.meta, {'revision_id': revision.id})
        self.assertEqual(event.page, page)
        self.assertEqual(event.event_type, PageEvent.EVENT_TYPES.create)
        self.assertEqual(event.author, self.thasonic)

    def test_create_page_subscriptions_no_parent(self):
        set_user_setting(self.thasonic, 'new_subscriptions', False)
        self.do_create_page(tag='Страница')

        page = Page.objects.get(tag='Страница')
        watches = PageWatch.objects.filter(page=page)

        self.assertEqual(len(watches), 1)
        only_watch = watches.get()
        self.assertEqual(only_watch.is_cluster, False)
        self.assertEqual(only_watch.user, self.thasonic.username)

    def test_create_page_subscriptions_with_parent(self):
        set_user_setting(self.thasonic, 'new_subscriptions', False)
        set_user_setting(self.volozh, 'new_subscriptions', False)

        self.do_create_page(tag='Страница')
        page = Page.objects.get(tag='Страница')
        PageWatch.objects.create(page=page, is_cluster=True, user=self.volozh.username)

        self.do_create_page(tag='Страница/подстраница')
        subpage = Page.objects.get(tag='Страница/подстраница')

        watches = PageWatch.objects.filter(page=subpage)

        self.assertEqual(len(watches), 2)
        thasonic_watch = watches.get(user=self.thasonic.username)
        self.assertEqual(thasonic_watch.is_cluster, False)

        volozh_watch = watches.get(user=self.volozh.username)
        self.assertEqual(volozh_watch.is_cluster, True)
