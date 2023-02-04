# coding: utf-8


import pytest
from django.core import mail
from django.test import TestCase
from mock import patch

from idm.notification import utils
from idm.notification.models import Notice
from idm.tests.utils import create_user


def fake_renderer(template_name, context):
    """
    хелпер, чтобы не указывать в тестах реальный шаблон
    """
    return 'faaaaake'


class NotificationsTest(TestCase):

    def test_send_notification(self):
        """Проверяем создание уведомлений в базе и отправку их по почте в send_notification"""
        user = create_user('terran')
        self.assertEqual(user.username, 'terran')
        self.assertEqual(0, Notice.objects.count())
        self.assertEqual(0, len(mail.outbox))

        template_name = 'fake_template'
        with patch.object(utils, 'render_to_string', new=fake_renderer):
            utils.send_notification('msg test', template_name, [user], context={})

        self.assertEqual(1, Notice.objects.count())
        self.assertEqual(1, len(mail.outbox))

    def test_notify_bad_emails(self):
        """
        Проверяем создание уведомлений в базе и отправку их по почте в случае, когда пришел плохой список получателей
        """
        # получатель - робот без email
        user = create_user('mecha-terran', email='')
        self.assertEqual(user.username, 'mecha-terran')
        self.assertEqual(0, Notice.objects.count())
        self.assertEqual(0, len(mail.outbox))

        template_name = 'fake_template'
        with patch.object(utils, 'render_to_string', new=fake_renderer):
            utils.send_notification('msg test', template_name, [user], context={})

        self.assertEqual(1, Notice.objects.count())
        # уведомления на пустой адрес не посылаются
        self.assertEqual(0, len(mail.outbox))

        # в список получателей закралась пустая строка и None
        with patch.object(utils, 'render_to_string', new=fake_renderer):
            utils.send_notification('msg test', template_name, [None, ''], context={})

        # Notice создаются лишь когда получатель - пользователь
        self.assertEqual(1, Notice.objects.count())
        # уведомления на пустой адрес все равно не посылаются
        self.assertEqual(0, len(mail.outbox))
