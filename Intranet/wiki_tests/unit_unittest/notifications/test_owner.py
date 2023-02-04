from datetime import datetime

from wiki.notifications.generators import change_owner as MailGen
from wiki.notifications.models import PageEvent
from wiki.pages.models import Page
from intranet.wiki.tests.wiki_tests.common.base_testcase import BaseTestCase
from intranet.wiki.tests.wiki_tests.common.ddf_compat import get


class OwnerTest(BaseTestCase):
    def setUp(self):
        super(OwnerTest, self).setUp()
        self.setPageEvents()

    def test_wiki_2932(self):
        """
        Admin gives ownership of a page to himself
        He should not get a letter
        """

        self.user_chapson.is_superuser = True
        self.user_chapson.save()
        PageEvent.objects.all().delete()
        testinfo = Page.objects.get(supertag='testinfo')
        testinfo.save()
        testinfo.authors.add(self.user_chapson)
        changes_started = datetime(2011, 11, 16, 12, 15, 00)
        pe = get(
            PageEvent,
            created_at=changes_started,
            timeout=changes_started,
            page=testinfo,
            author=self.user_chapson,
            sent_at=None,
            event_type=PageEvent.EVENT_TYPES.change_owner,
            notify=True,
            meta={},
        )
        pe.created_at = changes_started
        pe.meta = {'owner_name': 'chapson', 'previous_owner': 'thasonic', 'with_children': False}
        generator = MailGen
        result = generator.generate([pe], {})
        self.assertEqual(len(list(result.values())[0]), 1)
        details = list(result.keys())[0]
        self.assertTrue('thasonic' in details.receiver_email)
        self.assertEqual('Alexander Pokatilov', details.receiver_name)
