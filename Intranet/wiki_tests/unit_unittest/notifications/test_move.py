from datetime import timedelta
from unittest import skipIf

from django.conf import settings

from wiki import access as wiki_access
from wiki.notifications.generators import creation as CreateGen
from wiki.notifications.generators import move_cluster as MoveGen
from wiki.notifications.models import PageEvent
from wiki.pages.logic.move import move_clusters
from wiki.pages.models import Access, Page
from wiki.subscriptions.logic import create_subscription
from wiki.utils import timezone
from intranet.wiki.tests.wiki_tests.common.base_testcase import BaseTestCase


class MoveTest(BaseTestCase):
    def setUp(self):
        super(MoveTest, self).setUp()
        self.setPageEvents()

    @skipIf(settings.IS_BUSINESS, 'wait for fixing for business')
    def test_wiki_3052(self):
        """Complex mailing example

        chapson moved cluster testinfo/testinfogem to homepage/testinfogem right after it was created
        Dont mail subscribers of page testinfo, and notify subscribers of homepage only of creation, but not about move
        """
        PageEvent.objects.all().delete()
        Access.objects.all().delete()
        foopage = self.create_page(tag='foo', supertag='foo', body='example text')
        foobarpage = self.create_page(tag='foo/bar', supertag='foo/bar', body='example text')
        alicecluster = self.create_page(tag='alicecluster', supertag='alicecluster', body='example text')

        create_subscription(self.user_thasonic, foopage)
        create_subscription(self.user_thasonic, foobarpage)
        create_subscription(self.user_thasonic, alicecluster)

        pe = PageEvent(
            event_type=PageEvent.EVENT_TYPES.create,
            page=foobarpage,
            author=self.user_chapson,
            timeout=timezone.now() - timedelta(minutes=10),
            meta={},
        )
        pe.save()

        wiki_access.set_access(
            alicecluster, wiki_access.TYPES.RESTRICTED, self.user_chapson, staff_models=[self.user_chapson.staff]
        )

        result = move_clusters(self.user_chapson, {foobarpage.supertag: 'homepage/testinfogem'}, with_children=True)

        self.assertEqual(result[0], 200)
        events = PageEvent.objects.filter(page=foobarpage)
        result = MoveGen.generate(events, {})
        self.assertEqual(0, len(result), 'Must be no move events')
        result = CreateGen.generate(events, {})
        self.assertEqual(1, len(result), 'Must be one letter')
        the_receiver = list(result.keys())[0]
        self.assertTrue('thasonic' in the_receiver.receiver_email)
        self.assertTrue('homepage/testinfogem' in result[the_receiver][0])

    def test_wiki_3052_normal_flow(self):
        PageEvent.objects.all().delete()
        self.create_page(tag='foo', supertag='foo', body='example text')
        foobarpage = self.create_page(tag='foo/bar', supertag='foo/bar', body='example text')
        self.create_page(tag='alicecluster', supertag='alicecluster', body='example text')
        alice_page = Page.objects.get(supertag='alicecluster')
        create_subscription(self.user_thasonic, alice_page)
        wiki_access.set_access(
            alice_page,
            wiki_access.TYPES.RESTRICTED,
            self.user_chapson,
            staff_models=[self.user_chapson.staff],
        )
        PageEvent.objects.all().delete()
        result = move_clusters(self.user_chapson, {'foo/bar': 'alicecluster/testinfogem'}, with_children=True)
        events = PageEvent.objects.filter(page=foobarpage)
        result = MoveGen.generate(events, {})
        self.assertEqual(1, len(result))
        the_receiver = list(result.keys())[0]
        self.assertTrue('thasonic' in the_receiver.receiver_email)
        self.assertEqual(1, len(result))
