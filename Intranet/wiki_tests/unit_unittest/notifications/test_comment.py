
from hashlib import md5

import django.core.mail
import mock
from django.conf import settings
from django.core.management import call_command

from wiki.pages.logic.comment import add_comment
from wiki.pages.models import Comment
from intranet.wiki.tests.wiki_tests.common.unittest_base import BaseApiTestCase
from intranet.wiki.tests.wiki_tests.common.utils import celery_eager


class CommentTest(BaseApiTestCase):
    def setUp(self):
        super(CommentTest, self).setUp()
        self.setUsers()

    @celery_eager
    def test_comment(self):
        from wiki.utils.timezone import make_aware_utc

        self.client.login('chapson')
        page = self.create_page(tag='page', supertag='page', authors_to_add=[self.user_chapson], body='')
        self.client.post('/_api/frontend/page/.watch')

        self.client.login('thasonic')
        self.client.post('/_api/frontend/page/.watch')
        add_comment(user=self.user_chapson, page=page, body='comment', parent_id=None)

        # очень давно
        Comment.objects.filter(page__supertag='page').update(created_at=make_aware_utc(2012, 10, 10, 0, 0, 0))

        call_command('notify', verbosity=0)

        self.assertEqual(len(django.core.mail.outbox), 1)
        message = django.core.mail.outbox[0]
        self.assertEqual('addcomment', message.extra_headers['X-Yandex-Wiki-Notification'])

        self.assertEqual('page', message.extra_headers['X-Yandex-Wiki-Path'])
        self.assertEqual(
            '<%s.%s@yandex-team.ru>' % (md5(page.supertag.encode('utf-8')).hexdigest(), settings.WIKI_CODE),
            message.extra_headers['References'],
        )

    @mock.patch('yenv.type', 'production')
    @celery_eager
    def test_reply_to_comment(self):
        self.client.login('chapson')
        page = self.create_page(tag='page', supertag='page', authors_to_add=[self.user_chapson], body='')
        self.client.post('/_api/frontend/page/.unwatch')
        add_comment(user=self.user_chapson, page=page, body='comment', parent_id=None)

        latest_comment_id = list(Comment.objects.all().values_list('id', flat=True))[-1]
        self.client.login('thasonic')
        add_comment(user=self.user_thasonic, page=page, body='comment', parent_id=int(latest_comment_id))
        call_command('notify', verbosity=0)

        self.assertEqual(len(django.core.mail.outbox), 1)
        message = django.core.mail.outbox[0]
        self.assertEqual('addcomment', message.extra_headers['X-Yandex-Wiki-Notification'])
        self.assertEqual('page', message.extra_headers['X-Yandex-Wiki-Path'])
        self.assertEqual(
            '<%s.%s@yandex-team.ru>' % (md5(page.supertag.encode('utf-8')).hexdigest(), settings.WIKI_CODE),
            message.extra_headers['References'],
        )
        self.assertEqual(['Anton Chaporgin <chapson@yandex-team.ru>'], message.to)
