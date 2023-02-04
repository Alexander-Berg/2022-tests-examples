# -*- coding: utf-8 -*-
from __future__ import absolute_import
from __future__ import unicode_literals

import pytest

from django.contrib import messages
from django.contrib.messages.storage.base import Message
from django.core.urlresolvers import reverse
from django.test import override_settings
from django.utils.translation import gettext_lazy as _

from core.models import Administrator
from core.tests import YauthTestCase


@override_settings(YAUTH_TEST_USER={'uid': 1, 'login': 'admin'})
class YauthAdminTestCase(YauthTestCase):
    url_name = None

    def setUp(self):
        super(YauthAdminTestCase, self).setUp()
        Administrator.objects.create_superuser('admin', 'password', 'email')

    def _url(self, *args, **kwargs):
        return reverse(self.url_name, args=args, kwargs=kwargs)

    def _get_request(self, *args, **kwargs):
        url = self._url(*args, **kwargs)
        return self.client.get(url)

    def _post_request(self, *args, **kwargs):
        url = self._url(*args, **kwargs)
        return self.client.post(url, self.form_data)

    def _post_request_follow_redirect(self, *args, **kwargs):
        url = self._url(*args, **kwargs)
        return self.client.post(url, self.form_data, follow=True)


class MessagesMixin(object):

    def _message(self, level, text):
        return Message(level, _(text))

    def _context_messages(self, response):
        return response.context['messages']

    def assert_success_message(self, response, message_text):
        message = self._message(messages.SUCCESS, message_text)
        context_messages = self._context_messages(response)
        assert message in context_messages

    def assert_error_message(self, response, message_text):
        message = self._message(messages.ERROR, message_text)
        context_messages = self._context_messages(response)
        assert message in context_messages
