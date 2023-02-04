
from hashlib import md5
from unittest import skipIf

import django.core.mail
from django.conf import settings
from django.core.management import call_command

from wiki.notifications.generators.base import BaseGen
from wiki.notifications.models import PageEvent
from intranet.wiki.tests.wiki_tests.common.base_testcase import BaseTestCase
from intranet.wiki.tests.wiki_tests.common.utils import celery_eager


class NotificationsTest(BaseTestCase):
    @celery_eager
    @skipIf(settings.IS_BUSINESS, 'wait for fixing for business')
    def test_basics(self):
        self.setPageEvents()
        call_command('notify', simulate=False, verbosity=0)
        self.assertEqual(len(django.core.mail.outbox), 4 if settings.IS_INTRANET else 2)
        message = django.core.mail.outbox[0]
        self.assertTrue('X-Yandex-Service' in message.extra_headers, 'Must have signature for yandex-mail')
        self.assertEqual(
            '<%s.%s@yandex-team.ru>' % (md5(self.supertag.encode('utf-8')).hexdigest(), settings.WIKI_CODE),
            message.extra_headers['References'],
        )

    def test_empty_data(self):
        PageEvent.objects.all().delete()
        call_command('notify', simulate=False, verbosity=0)
        self.assertEqual(0, len(django.core.mail.outbox))

    if settings.IS_INTRANET:

        def test_no_email_language_for_robots(self):
            """
            generator must return None as robot email and language in strict mode
            and must raise Staff.DoesNotExist if normal mode.
            """
            self.setPageEvents()
            generator = BaseGen()

            robot = self.get_or_create_user('_rpc_zapp')
            robot.staff.is_robot = True
            self.assertEqual(generator.email_language(robot, strict_mode=True), None)

            self.assertEqual(generator.email_language(robot)[1], 'tools@')
