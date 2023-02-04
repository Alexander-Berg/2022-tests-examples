__author__ = 'chapson'

from datetime import datetime, timedelta
from hashlib import md5

import django.core.mail
from django.conf import settings
from django.core.management import call_command
from django.utils import formats

from wiki.notifications.generators import PageEdition as EditionGen
from wiki.notifications.generators.utils import format_for_nice_datetime
from wiki.notifications.models import PageEvent
from wiki.notifications.queue import Queue
from wiki.pages.models import Page, Revision
from wiki.subscriptions.models import Subscription
from wiki.utils.timezone import make_aware_current, make_naive_current, now
from intranet.wiki.tests.wiki_tests.common.base_testcase import BaseTestCase
from intranet.wiki.tests.wiki_tests.common.ddf_compat import get
from intranet.wiki.tests.wiki_tests.common.utils import celery_eager


class EditionTest(BaseTestCase):
    def setUp(self):
        super(EditionTest, self).setUp()
        self.setPageEvents()

        # Make sure you're sending emails to the memory

        self.assertEqual(settings.EMAIL_BACKEND, 'django.core.mail.backends.locmem.EmailBackend')

    @staticmethod
    def _now():
        return make_naive_current(now())

    @staticmethod
    def _get_formated_date(date, seconds):
        return formats.date_format(date, format_for_nice_datetime(seconds=seconds))

    def _assertionAboutRevisionDateInMessage(self, is_in, revision, chunks):
        # В зависимости от даты ревизий, дата отображается с секундами
        # или без, поэтому проверяем оба формата операцией OR.
        date_in_message = (self._get_formated_date(revision.created_at, seconds=True) in str(chunks)) or (
            self._get_formated_date(revision.created_at, seconds=False) in str(chunks)
        )

        self.assertEqual(is_in, date_in_message)

    def _assertRevisionDateInMessage(self, revision, chunks):
        self._assertionAboutRevisionDateInMessage(True, revision, chunks)

    def _assertRevisionDateNotInMessage(self, revision, chunks):
        self._assertionAboutRevisionDateInMessage(False, revision, chunks)

    def _create_revision(self, page, author, created_at, body, sent_at=None):
        revision = Revision(
            page=page,
            title=page.title,
            author=author,
            created_at=created_at,
        )
        revision.body = body
        revision.save()

        event = get(
            PageEvent,
            timeout=created_at + timedelta(minutes=20),
            page=page,
            author=author,
            sent_at=sent_at,
            event_type=PageEvent.EVENT_TYPES.edit,
            notify=True,
            meta={},
        )
        # Поле created_at имеет параметр auto_now_add=True,
        # любое значение, переданное при создании игнорируется,
        # поэтому обновляем его отдельно
        event.created_at = created_at
        event.save()

        return revision

    def _get_test_page(self):
        testinfo = Page.objects.get(supertag='testinfo')
        testinfo.body = 'Test page body'
        testinfo.title = 'Test page title'
        testinfo.save()
        return testinfo

    def test_aggregate_revisions_for_1_author(self):
        """
        Тест проверяет агрегирование (склеивание) изменений в двух ревизиях от одного автора, сделанных через короткий
        промежуток времени.
        Проверка попавших в письмо (в html формате) ревизий возможна только путем поиска в теле письма ссылки на нее,
        где идентификатором ревизии является дата и время ее создания.

        """
        PageEvent.objects.all().delete()
        testinfo = self._get_test_page()

        last_notif_date = self._now() - timedelta(days=45)
        get(Subscription, user=self.user_thasonic, page=testinfo)
        get(Subscription, user=self.user_chapson, page=testinfo)

        rev_before = self._create_revision(
            testinfo,
            self.user_thasonic,
            last_notif_date - timedelta(minutes=30),
            'Revision before last notification',
            last_notif_date,
        )

        rev_50 = self._create_revision(
            testinfo, self.user_chapson, self._now() - timedelta(minutes=50), 'Revision 50_min before now'
        )

        rev_40 = self._create_revision(
            testinfo, self.user_chapson, self._now() - timedelta(minutes=40), 'Revision 40_min before now'
        )

        rev_30 = self._create_revision(
            testinfo, self.user_chapson, self._now() - timedelta(minutes=30), 'Revision 30_min before now'
        )

        queue = Queue()
        new_events = list(queue.new_events(testinfo.id))
        self.assertEqual(len(new_events), 3, 'Must be 3 events objects')

        generator = EditionGen()
        result = generator.generate(new_events, {})
        self.assertEqual(len(result), 1, 'Notification must receive one person: thasonic')
        for to, chunks in result.items():
            if '<thasonic@yandex-team.ru>' in to.receiver_email:
                self.assertEqual(len(chunks), 1, 'thasonic must get 1 diff in notification')
                self._assertRevisionDateInMessage(rev_before, chunks)
                self._assertRevisionDateNotInMessage(rev_50, chunks)
                self._assertRevisionDateNotInMessage(rev_40, chunks)
                self._assertRevisionDateInMessage(rev_30, chunks)
            else:
                raise AssertionError('Unknown email address: %s' % to.receiver_email)

    def test_aggregate_revisions_for_2_authors(self):
        """
        Тест проверяет агрегирование (склеивание) изменений в ревизиях от двух разных пользователей
        на основе значения MAX_REVISION_TIMEOUT.
        Проверка попавших в письмо (в html формате) ревизий возможна только путем поиска в теле письма ссылки на нее,
        где идентификатором ревизии является дата и время ее создания.
        """
        PageEvent.objects.all().delete()
        testinfo = self._get_test_page()

        last_notif_date = self._now() - timedelta(days=30)
        get(Subscription, user=self.user_thasonic, page=testinfo)
        get(Subscription, user=self.user_chapson, page=testinfo)

        rev_before_n6 = self._create_revision(
            testinfo,
            self.user_chapson,
            last_notif_date - timedelta(minutes=30),
            'Revision before last notification',
            last_notif_date,
        )

        rev_100_n7 = self._create_revision(
            testinfo, self.user_chapson, self._now() - timedelta(hours=100), 'Revision 100_HOURS before now'
        )

        rev_80_n8 = self._create_revision(
            testinfo, self.user_chapson, self._now() - timedelta(hours=80), 'Revision 80_HOURS before now'
        )

        rev_70_n9 = self._create_revision(
            testinfo, self.user_chapson, self._now() - timedelta(hours=70), 'Revision 70_HOURS before now'
        )

        rev_60_n10 = self._create_revision(
            testinfo, self.user_thasonic, self._now() - timedelta(hours=60), 'Revision 60_HOURS before now'
        )

        rev_20_n11 = self._create_revision(
            testinfo, self.user_thasonic, self._now() - timedelta(hours=20), 'Revision 20_HOURS before now'
        )

        rev_10_n12 = self._create_revision(
            testinfo, self.user_chapson, self._now() - timedelta(hours=10), 'Revision 10_HOURS before now'
        )

        rev_now_n13 = self._create_revision(
            testinfo, self.user_chapson, self._now() - timedelta(minutes=30), 'Revision NOW'
        )

        queue = Queue()
        new_events = list(queue.new_events(testinfo.id))
        self.assertEqual(len(new_events), 7, 'Must be 7 events objects')

        generator = EditionGen()
        result = generator.generate(new_events, {})
        self.assertEqual(len(result), 2, 'Notification must receive two persons: chapson and thasonic')
        for to, chunks in result.items():
            if '<chapson@yandex-team.ru>' in to.receiver_email:
                self.assertEqual(len(chunks), 1, 'chapson must get 1 diff in notification')
                self._assertRevisionDateNotInMessage(rev_before_n6, chunks)
                self._assertRevisionDateNotInMessage(rev_100_n7, chunks)
                self._assertRevisionDateNotInMessage(rev_80_n8, chunks)
                self._assertRevisionDateInMessage(rev_70_n9, chunks)
                self._assertRevisionDateNotInMessage(rev_60_n10, chunks)
                self._assertRevisionDateInMessage(rev_20_n11, chunks)
                self._assertRevisionDateNotInMessage(rev_10_n12, chunks)
                self._assertRevisionDateNotInMessage(rev_now_n13, chunks)
            elif '<thasonic@yandex-team.ru>' in to.receiver_email:
                self.assertEqual(len(chunks), 2, 'thasonic must get 2 diff in notification')
                self._assertRevisionDateInMessage(rev_before_n6, chunks)
                self._assertRevisionDateNotInMessage(rev_100_n7, chunks)
                self._assertRevisionDateNotInMessage(rev_80_n8, chunks)
                self._assertRevisionDateInMessage(rev_70_n9, chunks)
                self._assertRevisionDateNotInMessage(rev_60_n10, chunks)
                self._assertRevisionDateInMessage(rev_20_n11, chunks)
                self._assertRevisionDateNotInMessage(rev_10_n12, chunks)
                self._assertRevisionDateInMessage(rev_now_n13, chunks)
            else:
                raise AssertionError('Unknown email address: %s' % to.receiver_email)

    def test_aggregate_revisions_for_3_authors(self):
        """
        Тест проверяет агрегирование (склеивание) изменений в ревизиях от трех разных пользователей
        на основе значения MAX_REVISION_TIMEOUT.
        """
        PageEvent.objects.all().delete()
        testinfo = self._get_test_page()

        last_notif_date = self._now() - timedelta(days=30)
        get(Subscription, user=self.user_thasonic, page=testinfo)
        get(Subscription, user=self.user_chapson, page=testinfo)
        get(Subscription, user=self.user_kolomeetz, page=testinfo)

        rev_before_n6 = self._create_revision(
            testinfo,
            self.user_chapson,
            last_notif_date - timedelta(days=30),
            'Revision before last notification',
            last_notif_date,
        )

        rev_109_n7 = self._create_revision(
            testinfo, self.user_chapson, self._now() - timedelta(hours=109), 'Revision 108 hours before now'
        )

        rev_97_n8 = self._create_revision(
            testinfo, self.user_kolomeetz, self._now() - timedelta(hours=97), 'Revision 96 hours before now'
        )

        rev_48_n9 = self._create_revision(
            testinfo, self.user_kolomeetz, self._now() - timedelta(hours=48), 'Revision 48 hours before now'
        )

        rev_36_n10 = self._create_revision(
            testinfo, self.user_thasonic, self._now() - timedelta(hours=36), 'Revision 36 hours before now'
        )

        rev_30_n11 = self._create_revision(
            testinfo, self.user_chapson, self._now() - timedelta(hours=30), 'Revision 30 hours before now'
        )

        rev_now_n12 = self._create_revision(
            testinfo, self.user_kolomeetz, self._now() - timedelta(minutes=31), 'Revision now'
        )

        queue = Queue()
        new_events = list(queue.new_events(testinfo.id))
        self.assertEqual(len(new_events), 6)

        generator = EditionGen()
        result = generator.generate(new_events, {})
        self.assertEqual(len(result), 3, 'Notification must receive three persons: chapson, thasonic and kolomeetz')
        for to, chunks in result.items():
            if '<chapson@yandex-team.ru>' in to.receiver_email:
                self.assertEqual(len(chunks), 2, 'chapson must get 2 diff in notification')
                self._assertRevisionDateInMessage(rev_109_n7, chunks)
                self._assertRevisionDateInMessage(rev_36_n10, chunks)
                self._assertRevisionDateInMessage(rev_30_n11, chunks)
                self._assertRevisionDateInMessage(rev_now_n12, chunks)
                self._assertRevisionDateNotInMessage(rev_before_n6, chunks)
                self._assertRevisionDateNotInMessage(rev_97_n8, chunks)
                self._assertRevisionDateNotInMessage(rev_48_n9, chunks)
            elif '<thasonic@yandex-team.ru>' in to.receiver_email:
                self.assertEqual(len(chunks), 2, 'thasonic must get 2 diff in notification')
                self._assertRevisionDateInMessage(rev_before_n6, chunks)
                self._assertRevisionDateNotInMessage(rev_109_n7, chunks)
                self._assertRevisionDateNotInMessage(rev_97_n8, chunks)
                self._assertRevisionDateInMessage(rev_48_n9, chunks)
                self._assertRevisionDateInMessage(rev_36_n10, chunks)
                self._assertRevisionDateNotInMessage(rev_30_n11, chunks)
                self._assertRevisionDateInMessage(rev_now_n12, chunks)
            elif '<kolomeetz@yandex-team.ru>' in to.receiver_email:
                self.assertEqual(len(chunks), 2, 'kolomeetz must get 2 diff in notification')
                self._assertRevisionDateInMessage(rev_before_n6, chunks)
                self._assertRevisionDateInMessage(rev_109_n7, chunks)
                self._assertRevisionDateNotInMessage(rev_97_n8, chunks)
                self._assertRevisionDateInMessage(rev_48_n9, chunks)
                self._assertRevisionDateNotInMessage(rev_36_n10, chunks)
                self._assertRevisionDateInMessage(rev_30_n11, chunks)
                self._assertRevisionDateNotInMessage(rev_now_n12, chunks)
            else:
                raise AssertionError('Unknown email address: %s' % to.receiver_email)

    def test_exclude_old_revisions(self):
        """
        Выкинуть ревизии старше MAX_REVISION_DAYS_COUNT
        Несмотря на то, что по факту таких ревизий всего одна, для сравнения оставляется еще одна самая последняя
        из устаревших ревизий.
        """
        PageEvent.objects.all().delete()
        testinfo = self._get_test_page()

        last_notif_date = datetime(2013, 9, 10, 12, 15, 00)
        get(Subscription, user=self.user_thasonic, page=testinfo)
        get(Subscription, user=self.user_chapson, page=testinfo)

        self._create_revision(
            testinfo,
            self.user_chapson,
            datetime(2013, 9, 1, 12, 15, 00),
            'Revision 1 sept - before last notification',
            last_notif_date,
        )

        self._create_revision(
            testinfo, self.user_chapson, datetime(2013, 9, 15, 12, 15, 00), 'Revision 15 sept - after last notification'
        )

        self._create_revision(
            testinfo,
            self.user_thasonic,
            datetime(2013, 9, 20, 12, 15, 00),
            'Revision 20 sept - after last notification',
        )

        self._create_revision(
            testinfo, self.user_thasonic, datetime(2013, 10, 1, 12, 15, 00), 'Revision 1 oсt - after last notification'
        )

        self._create_revision(
            testinfo, self.user_chapson, datetime(2013, 11, 1, 12, 15, 00), 'Revision 1 nov - after last notification'
        )

        self._create_revision(
            testinfo,
            self.user_thasonic,
            make_aware_current(2013, 11, 2, 12, 15, 00),
            'Revision 2 nov - after last notification',
        )

        rev_now = self._create_revision(
            testinfo, self.user_thasonic, self._now() - timedelta(minutes=30), 'Revision now'
        )

        queue = Queue()
        new_events = list(queue.new_events(testinfo.id))
        self.assertEqual(len(new_events), 6)

        generator = EditionGen()
        result = generator.generate(new_events, {})
        self.assertEqual(len(result), 1)
        for to, chunks in result.items():
            if '<chapson@yandex-team.ru>' in to.receiver_email:
                self.assertEqual(len(chunks), 1, 'chapson must get 1 diff in notification')
                self._assertRevisionDateInMessage(rev_now, chunks)
            else:
                raise AssertionError('Unknown email address: %s' % to.receiver_email)

    def test_edition(self):
        """
        EditionGen must generate 2 notifications

        """
        PageEvent.objects.all().delete()
        testinfo = Page.objects.get(supertag='testinfo')
        testinfo.body = 'Hi! Party hard!'
        testinfo.title = 'Party cat'
        testinfo.save()
        changes_started = datetime(2011, 11, 16, 12, 15, 00)

        get(Subscription, user=self.user_kolomeetz, page=testinfo)
        get(Subscription, user=self.user_thasonic, page=testinfo)
        get(Subscription, user=self.user_chapson, page=testinfo)

        revision = Revision(
            page=testinfo,
            title=testinfo.title,
            author=self.user_thasonic,
            created_at=changes_started,
        )
        revision.body = 'Good day, party-cat!'
        revision.save()
        revision_before_sent_at = Revision(
            page=testinfo,
            title=testinfo.title,
            author=testinfo.last_author or testinfo.get_authors().first(),
            created_at=datetime(2011, 10, 20, 12, 10, 0o2),
        )
        revision_before_sent_at.body = 'Sleepy head\n Do you know where Anton\'s at?'
        revision_before_sent_at.save()
        pe = get(
            PageEvent,
            timeout=changes_started,
            page=testinfo,
            author=self.user_thasonic,
            sent_at=None,
            event_type=PageEvent.EVENT_TYPES.edit,
            notify=True,
            meta={},
        )
        pe.meta['revision_id'] = revision.id
        pe.created_at = changes_started
        pe.save()
        queue = Queue()
        new_events = list(queue.new_events(testinfo.id))
        self.assertEqual(len(new_events), 1)
        generator = EditionGen()
        result = generator.generate(new_events, {})
        self.assertEqual(len(result), 2, 'chapson and kolomeetz must get notifications')

    @celery_eager
    def test_wiki_env_is_sent(self):

        # Send some notifications
        call_command('notify', verbosity=0)

        # Take any of the sent messages and see X-Yandex-Wiki-Env is set

        message = django.core.mail.outbox[0]
        self.assertIn('X-Yandex-Wiki-Env', message.extra_headers)
        self.assertEqual(
            '<%s.%s@yandex-team.ru>' % (md5(self.supertag.encode('utf-8')).hexdigest(), settings.WIKI_CODE),
            message.extra_headers['References'],
        )

    @celery_eager
    def test_wiki_env_is_something_appropriate(self):

        # Send some notifications

        call_command('notify', verbosity=0)

        # Take any of the sent messages and see X-Yandex-Wiki-Env is
        # something meaningful

        message = django.core.mail.outbox[0]
        self.assertIn(message.extra_headers['X-Yandex-Wiki-Env'], ['development', 'testing', 'production'])
        self.assertEqual(
            '<%s.%s@yandex-team.ru>' % (md5(self.supertag.encode('utf-8')).hexdigest(), settings.WIKI_CODE),
            message.extra_headers['References'],
        )
