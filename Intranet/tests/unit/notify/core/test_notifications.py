from django.test import TestCase
from mock import patch

from plan.notify.core.notifications import Notification

DUMMY_CONTEXT = {}


class NotificationTest(TestCase):
    @patch('plan.notify.core.notifications.loader')
    def test_load_template_one_lang(self, loader):
        notification = Notification(id='notify.hello_cruel_world')

        notification.load_template(
            format='html',
            lang='kz',
        )

        loader.select_template.assert_called_with([
            'notify/hello_cruel_world_kz.html',
        ])

    @patch('plan.notify.core.notifications.loader')
    def test_load_template_multiple_lang(self, loader):
        notification = Notification(id='notify.hello_cruel_world')

        notification.load_template(
            format='html',
            lang=['kz', 'en'],
        )

        loader.select_template.assert_called_with([
            'notify/hello_cruel_world_kz.html',
            'notify/hello_cruel_world_en.html',
        ])
