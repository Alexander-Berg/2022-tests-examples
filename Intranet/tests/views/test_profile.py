# -*- coding: utf-8 -*-
from __future__ import unicode_literals

import pytest

from django.core.urlresolvers import reverse
from django import test

import app.views.profile
import app.views.payment_info

from app.tests.views import BaseProfileTestCase
from core import forms
from core.tests import YauthTestCase
from core.utils import blackbox


class BaseProfileMixin(object):
    """Mixin for profile visibility tests."""

    # Содержимое базы задается json-ом из external/src/app/fixtures/
    fixtures = ['test_views']

    def _get_response(self, username):
        return self.client.get(
            reverse('reporter-by-name', kwargs={'username': username}))


@test.override_settings(YAUTH_TEST_USER=False)
@pytest.mark.usefixtures('patch_blackbox_userinfo')
@pytest.mark.skip('Сейчас не используется (BUGBOUNTY-517)')
class AnonymousProfileTest(BaseProfileMixin, test.TestCase):
    """Test if profile is visible to anonymous user."""

    def test_hidden_reporter_profile(self):
        response = self._get_response('hidden')
        self.assertEqual(response.status_code, 404)

    def test_reporter_not_found(self):
        response = self._get_response('notfound')
        assert response.status_code == 404


@pytest.mark.usefixtures('patch_blackbox_userinfo')
@pytest.mark.skip('Сейчас не используется (BUGBOUNTY-517)')
class ProfileTest(BaseProfileMixin, YauthTestCase):
    """Test if profile is visible to authorized user."""

    def test_profile_by_name(self):
        response = self._get_response('user')
        self.assertContains(response, 'user')

    def test_hidden_reporter_profile(self):
        response = self._get_response('hidden')
        assert response.status_code == 404

    @test.override_settings(YAUTH_TEST_USER='2')
    def test_own_hidden_profile(self):
        response = self._get_response('hidden')
        assert response.status_code == 200


@pytest.mark.usefixtures('patch_blackbox_userinfo')
@pytest.mark.skip('Сейчас не используется (BUGBOUNTY-517)')
class EditProfileTest(BaseProfileTestCase):
    """Test profile edit view."""

    # Сейчас не используется (BUGBOUNTY-517)
    # url = reverse('edit-profile')

    def test_form_in_context(self):
        context = self._get_response_context()
        assert isinstance(context['form'], forms.ReporterForm)


@pytest.fixture
def patch_blackbox_userinfo(monkeypatch):
    from core.models import user
    """Patch external_userinfo_by_login in app.views.profile."""
    def userinfo_mock(username):
        BLACKBOX = {'user': 1, 'hidden': 2, 'notfound': ''}
        return blackbox.BlackboxUser(uid=BLACKBOX.get(username))
    def userinfo_by_uid_mock(uid):
        return blackbox.BlackboxUser(uid=uid)
    monkeypatch.setattr(
        app.views.profile.blackbox, 'external_userinfo_by_login', userinfo_mock)
    monkeypatch.setattr(
        user, 'external_userinfo_by_uid', userinfo_by_uid_mock)
