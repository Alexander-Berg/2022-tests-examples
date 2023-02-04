# -*- coding: utf-8 -*-
from __future__ import absolute_import
from __future__ import unicode_literals

import pytest

from django.core.urlresolvers import reverse
from django.test import TestCase
from django.test import modify_settings
from django.test import override_settings

from core.tests import YauthTestCase


@override_settings(YAUTH_TEST_USER=False)
@pytest.mark.skip('Сейчас не используется (BUGBOUNTY-517)')
class ReportAnonymousTest(TestCase):
    # Содержимое базы задается json-ом из external/src/app/fixtures/
    fixtures = ['test_views']

    def test_anonymous_redirect(self):
        response = self.client.get(reverse('report'))
        self.assertRedirects(response, reverse('login-required'))


@override_settings(YAUTH_TEST_USER='1')
class ReportTest(YauthTestCase):
    # Содержимое базы задается json-ом из external/src/app/fixtures/
    fixtures = ['test_views']

    def test_login_required_view_redirect(self):
        response = self.client.get(reverse('login-required'))
        self.assertRedirects(response, reverse('main'))

    def test_email_required_view_redirect(self):
        response = self.client.get(reverse('email-required'))
        self.assertRedirects(response, reverse('main'))


@override_settings(YAUTH_TEST_USER={'login': '1', 'default_email': ''})
@pytest.mark.skip('Сейчас не используется (BUGBOUNTY-517)')
class ReportNoEmailTest(YauthTestCase):
    # Содержимое базы задается json-ом из external/src/app/fixtures/
    fixtures = ['test_views']

    def test_missing_email_redirect(self):
        """Redirect reporter without email to passport."""
        response = self.client.get(reverse('report'), follow=True)
        self.assertTemplateUsed(response, 'email_required.html')
