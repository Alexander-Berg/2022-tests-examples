from datetime import datetime
from unittest import skipIf

from django.conf import settings
from ujson import loads

from wiki.notifications.generators import watch as watch_gen
from wiki.notifications.models import PageEvent
from wiki.pages.models import Page, PageWatch
from intranet.wiki.tests.wiki_tests.common.base_testcase import BaseTestCase


class WatchTest(BaseTestCase):

    API_URL = '/_api/frontend'
    MIMETYPE_JSON = 'application/json'

    def setUp(self):
        super(WatchTest, self).setUp()
        self.setUsers(use_legacy_subscr_favor=True)
        self.setPages()

    @skipIf(settings.IS_BUSINESS, 'wait for fixing for business')
    def test_wiki_3292(self):
        """thasonic has subscribed to the whole cluster "testinfo", then unsubscribed from "testinfo/testinfogem"
        chapson must get 1 letter with list of pages without "testinfo/testinfogem"
        kolomeetz owns page testinfo/bla, testinfo/redirectpage, he should also get 1 letter
        thasonic owns 1 page,  testinfo/thasonic. He does NOT get a letter
        """
        testinfo = Page.objects.get(supertag='testinfo')
        PageWatch(page=testinfo, user='chapson').save()

        # set kolomeetz as owner
        pages = Page.objects.filter(supertag__in=('testinfo/bla', 'testinfo/redirectpage'))
        for page in pages:
            page.authors.clear()
            page.authors.add(self.user_kolomeetz)
        PageWatch(page=Page.objects.get(supertag='testinfo/bla'), user='kolomeetz').save()
        PageWatch(page=Page.objects.get(supertag='testinfo/redirectpage'), user='kolomeetz').save()
        # thasonic's page
        self.create_page(tag='testinfo/thasonic', page_type=Page.TYPES.PAGE)
        # thasonic will be subscribed to every page in testinfo, except for testinfo/testinfogem
        pages_in_cluster_qs = Page.objects.filter(supertag__startswith='testinfo')
        for page in pages_in_cluster_qs.exclude(supertag='testinfo/testinfogem'):
            PageWatch(page=page, user='thasonic', is_cluster=True).save()

        pe = PageEvent(
            event_type=PageEvent.EVENT_TYPES.watch,
            page=testinfo,
            author=self.user_thasonic,
            timeout=datetime(2011, 12, 28, 11, 0o3, 00),
        )
        pe.meta = {'is_cluster': True, 'pages': [_.supertag for _ in pages_in_cluster_qs]}

        generated = watch_gen.generate([pe])

        self.assertEqual(len(generated), 2)
        self.assertTrue(any(_.receiver_email.find('chapson') > -1 for _ in generated), 'Must send email to chapson')
        self.assertTrue(any(_.receiver_email.find('kolomeetz') > -1 for _ in generated), 'Must send email to kolomeetz')

        # please, also check that letter to chapson does not contain "testinfo/testinfogem"

    @skipIf(settings.IS_BUSINESS, 'wait for fixing for business')
    def test_mass_watch_more_users_1(self):
        PageEvent.objects.all().delete()
        self.client.login(self.user_thasonic)
        page = self.create_page(tag='cluster_page')
        self.create_page(tag='cluster_page/page1')
        self.create_page(tag='cluster_page/page2')

        request_url = '{api_url}/{page_supertag}/.masswatch'.format(api_url=self.API_URL, page_supertag=page.supertag)

        request_data = {
            'uids': [self.user_thasonic.staff.uid, self.user_chapson.staff.uid, self.user_kolomeetz.staff.uid],
            'comment': 'You subscribed!',
        }

        r = self.client.post(request_url, data=request_data, content_type=self.MIMETYPE_JSON)
        self.assertEqual(200, r.status_code)
        data = loads(r.content)['data']
        self.assertEqual(data['pages_count'], 9)

        events = PageEvent.objects.all()
        generated = watch_gen.generate(events)
        # должно быть отправлено 4 письма: два - user_thasonic о том, что user_chapson и user_kolomeetz подписались на
        # его кластер, 1 - user_chapson и 1 - user_kolomeetz о том, что user_thasonic их подписал на свой кластер
        self.assertEqual(len(generated), 4)
        receivers = ''.join([item.receiver_email for item in generated])

        self.assertEqual(receivers.count('chapson'), 1)
        self.assertEqual(receivers.count('kolomeetz'), 1)
        self.assertEqual(receivers.count('thasonic'), 2)

        emails = ''.join([item[0] for item in list(generated.values())])
        self.assertEqual(emails.count('You subscribed!'), 2)

    @skipIf(settings.IS_BUSINESS, 'wait for fixing for business')
    def test_mass_watch_more_users_2(self):
        PageEvent.objects.all().delete()
        self.client.login(self.user_thasonic)
        page = self.create_page(tag='cluster_page')
        self.create_page(tag='cluster_page/page1')
        self.create_page(tag='cluster_page/page2')
        self.client.login(self.user_chapson)

        request_url = '{api_url}/{page_supertag}/.masswatch'.format(api_url=self.API_URL, page_supertag=page.supertag)

        request_data = {
            'uids': [self.user_kolomeetz.staff.uid],
        }

        r = self.client.post(request_url, data=request_data, content_type=self.MIMETYPE_JSON)
        self.assertEqual(200, r.status_code)
        data = loads(r.content)['data']
        self.assertEqual(data['pages_count'], 3)

        events = PageEvent.objects.all()
        generated = watch_gen.generate(events)
        # должно быть отправлено 2 письма: user_kolomeetz о том, что user_chapson подписал его на кластер и
        # user_thasonic о том, что user_kolomeetz подписался на его кластер
        self.assertEqual(len(generated), 2)

        receivers = ''.join([item.receiver_email for item in generated])

        self.assertEqual(receivers.count('chapson'), 0)
        self.assertEqual(receivers.count('kolomeetz'), 1)
        self.assertEqual(receivers.count('thasonic'), 1)
