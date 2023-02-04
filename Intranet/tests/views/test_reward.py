# -*- coding: utf-8 -*-
from __future__ import unicode_literals

import factory
import pytest

from django.core import mail
from django.core.urlresolvers import reverse
from django.db.models import signals

from app.tests.views import MessagesMixin
from app.tests.views import YauthAdminTestCase
from core.models import MailTemplate
from core.models import PaymentInfo
from core.models import Reward
from core.utils.blackbox import BlackboxUserError

from mock import patch
from core.utils import blackbox


class RewardListTest(YauthAdminTestCase):
    # Содержимое базы задается json-ом из internal/src/app/fixtures/
    fixtures = ['views/test_reward']
    url_name = 'reward:list'

    def test_template(self):
        with patch('app.views.reward.external_userinfo_by_uid') as bbpatch:
            bbpatch.return_value = blackbox.BlackboxUser(uid=1)
            response = self._get_request()
        self.assertTemplateUsed(response, 'rewards.html')

    def test_context(self):
        with patch('app.views.reward.external_userinfo_by_uid') as bbpatch:
            bbpatch.return_value = blackbox.BlackboxUser(uid=1)
            response = self._get_request()
        assert 'rewards' in response.context
        assert len(response.context['rewards']) >= 1


@pytest.fixture
def form_data(request):
    request.cls.form_data = {
        'product': 1,
        'payment_currency': 100500,  # должно проигнорироваться
        'payment_amount_usd': 10,
        'payment_amount_rur': 300,
        'points': 1,
        'status': Reward.ST_FINISHED,
        'vulnerability_type': 1,
    }


@pytest.mark.usefixtures('form_data')
@pytest.mark.usefixtures('patch_otrs_client')
@pytest.mark.usefixtures('patch_external_userinfo_by_uid')
class RewardEditTest(MessagesMixin, YauthAdminTestCase):
    # Содержимое базы задается json-ом из internal/src/app/fixtures/
    fixtures = ['views/test_reward']
    url_name = 'reward-edit'

    def test_redirect(self):
        with patch('app.views.reward.external_userinfo_by_uid') as bbpatch:
            bbpatch.return_value = blackbox.BlackboxUser(uid=1)
            response = self._post_request(1)
            self.assertRedirects(response, reverse('reward:list'))

    def test_success_message(self):
        with patch('app.views.reward.external_userinfo_by_uid') as bbpatch:
            bbpatch.return_value = blackbox.BlackboxUser(uid=1)
            response = self._post_request_follow_redirect(1)
        self.assert_success_message(
            response, 'Reward was successfully updated')


@pytest.fixture
def create_data(request):
    request.cls.form_data = {
        'product': 1,
        'payment_currency': 1,
        'payment_amount_usd': 10,
        'payment_amount_rur': 300,
        'points': 1,
        'vulnerability_type': 1,
        'comment': 'abacaba',
        'ticket_created': '1111-11-11',
        'startrek_ticket_code': '1234',
        'reporter': 'somelogin',
        'staff_login': 'somestafflogin',
        'staff_uid': '1100111001100100100',
    }


@pytest.mark.usefixtures('create_data')
class RewardAddTest(YauthAdminTestCase):
    url_name = 'reward:add'
    # Содержимое базы задается json-ом из internal/src/app/fixtures/
    fixtures = ['views/test_reward_create']

    def test_add_reward_success(self):
        with patch('app.forms.reward.external_userinfo_by_login') as bbpatch:
            bbpatch.return_value = blackbox.BlackboxUser(uid=1)
            self._post_request()
            assert Reward.objects.count() == 1

    def test_add_reward_reporter_does_not_exist(self):
        with patch('app.forms.reward.external_userinfo_by_login') as bbpatch:
            bbpatch.return_value = blackbox.BlackboxUser(uid=2)
            self._post_request()
            assert Reward.objects.count() == 0

    def test_add_reward_login_does_not_exist(self):
        def blackbox_error_patch(*args, **kwargs):
            raise BlackboxUserError
        with patch('app.forms.reward.external_userinfo_by_login') as bbpatch:
            bbpatch.side_effect = blackbox_error_patch
            self._post_request()
            assert Reward.objects.count() == 0
