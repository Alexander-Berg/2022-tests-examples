
import base64
import re

from django.conf import settings
from mock import patch
from pretend import stub

from wiki.utils import mail
from intranet.wiki.tests.wiki_tests.common.unittest_base import BaseApiTestCase

DUMMY_SUBJECT = 'Hello'
DUMMY_BODY = 'Psst, wanna some information?'


class BuildEmailMessageTest(BaseApiTestCase):
    @patch('wiki.utils.mail.yenv', stub(type='production'))
    def test_only_required_params_env_prod(self):

        message = mail.build_email_message(
            to='somebody@yandex-team.ru',
            subject=DUMMY_SUBJECT,
            body=DUMMY_BODY,
            from_email='spammer@mail.ru',
        )

        self.assertEqual(message.to, ['somebody@yandex-team.ru'])
        self.assertEqual(message.cc, [])
        self.assertEqual(message.bcc, [])
        self.assertEqual(message.subject, DUMMY_SUBJECT)
        self.assertEqual(message.body, DUMMY_BODY)
        self.assertEqual(message.extra_headers, {})
        self.assertEqual(message.content_subtype, 'plain')
        self.assertIn('spammer@mail.ru', message.from_email)

    @patch('wiki.utils.mail.yenv', stub(type='production'))
    def test_more_params_env_prod(self):

        message = mail.build_email_message(
            to=['somebody@yandex-team.ru', 'buddy@yandex-team.ru'],
            cc='boss@yandex-team.ru',
            bcc='bcc@yandex-team.ru',
            reply_to='replyme@yandex-team.ru',
            subject=DUMMY_SUBJECT,
            body=DUMMY_BODY,
            from_email='spammer@mail.ru',
            author_name='Dirty Spammer',
            content_subtype='html',
        )

        self.assertEqual(message.to, ['somebody@yandex-team.ru', 'buddy@yandex-team.ru'])
        self.assertEqual(message.cc, ['boss@yandex-team.ru'])
        self.assertEqual(message.bcc, ['bcc@yandex-team.ru'])
        self.assertEqual(message.subject, DUMMY_SUBJECT)
        self.assertEqual(message.body, DUMMY_BODY)
        self.assertEqual(
            message.extra_headers,
            {
                'Reply-To': 'replyme@yandex-team.ru',
            },
        )

        self.assertIn('spammer@mail.ru', message.from_email)

        name = message.from_email.split('<')[0].strip()
        self.assertIn('Dirty Spammer', name)
        self.assertEqual(message.content_subtype, 'html')

    def test_env_dev_routing(self):
        message = mail.build_email_message(
            to=['somebody@yandex-team.ru'],
            cc='boss@yandex-team.ru',
            bcc='bcc@yandex-team.ru',
            subject=DUMMY_SUBJECT,
            body=DUMMY_BODY,
            from_email='spammer@mail.ru',
        )

        self.assertEqual(message.to, [settings.DEBUG_EMAIL])
        self.assertEqual(message.cc, [])
        self.assertEqual(message.bcc, [])
        self.assertEqual(message.subject, DUMMY_SUBJECT)
        self.assertIn(DUMMY_BODY, message.body)
        self.assertIn('Original To: [\'somebody@yandex-team.ru\']', message.body)
        self.assertIn('CC: [\'boss@yandex-team.ru\']', message.body)
        self.assertIn('BCC: [\'bcc@yandex-team.ru\']', message.body)


class BuildWikiEmailMessageTest(BaseApiTestCase):
    @patch('wiki.utils.mail.yenv', stub(type='production'))
    def test_many_params_env_prod(self):
        message = mail.build_wiki_email_message(
            to='somebody@yandex-team.ru',
            cc='boss@yandex-team.ru',
            bcc='bcc@yandex-team.ru',
            reply_to='replyme@yandex-team.ru',
            subject=DUMMY_SUBJECT,
            body=DUMMY_BODY,
            from_email='spammer@mail.ru',
            author_name='Dirty Spammer',
            content_subtype='html',
            supertag='homefuckingpage',
        )

        self.assertEqual(message.to, ['somebody@yandex-team.ru'])
        self.assertEqual(message.cc, ['boss@yandex-team.ru'])
        self.assertEqual(message.bcc, ['bcc@yandex-team.ru'])
        self.assertEqual(message.subject, DUMMY_SUBJECT)
        self.assertEqual(message.body, DUMMY_BODY)
        self.assertEqual(message.extra_headers['Reply-To'], 'replyme@yandex-team.ru')
        self.assertEqual(message.extra_headers['X-Yandex-Wiki-Path'], 'homefuckingpage')
        self.assertIn('References', message.extra_headers)
        self.assertIn('X-Yandex-Service', message.extra_headers)
        self.assertIn('Date', message.extra_headers)

        self.assertIn('spammer@mail.ru', message.from_email)

        name = message.from_email.split('<')[0].strip()
        self.assertIn('Dirty Spammer', name)
        self.assertEqual(message.content_subtype, 'html')
