# -*- coding: utf-8 -*-
from __future__ import unicode_literals

import json

import factory as factory_boy
import pytest

from django.conf import settings
from django.core.urlresolvers import reverse
from django.db.models import signals
from django.test import TestCase

from core import models
from core.tests import factory


pytestmark = pytest.mark.django_db


class RewardAPITest(TestCase):

    def setUp(self):
        self.reporter = models.Reporter.objects.create(username='user' * 8, uid=1)
        self.vulnerability = factory.VulnerabilityFactory.create()
        self.product = factory.ProductFactory.create()
        # These attrs will be compared to attrs of created Reward.
        self.reward_attrs = {
            'user_uid': self.reporter.uid,
            'staff_uid': 1000,
            'staff_login': 'admin',
            'startrek_ticket_code': '100',
            'payment_amount_usd': 100,
            'payment_amount_rur': 3000,
            'points': 1,
        }
        # These params will be posted to API from future OTRS form.
        # https://st.yandex-team.ru/BUGBOUNTY-268
        self.post_data = self.reward_attrs.copy()
        self.post_data.update({
            'user_login': self.reporter.username,
            'auth_token': 'CHANGEME',
            'department': self.product.pk,
            'vulnerability_type': self.vulnerability.pk,
            'ticket_created': '2014-10-30 10:38:06',
        })

    def assertRewardAttrsEqual(self, json_content, attrs):
        """Check reward attrs' values.

        Check that reward with given attrs exists and they have given
        values."""
        pk = json.loads(json_content)['id']
        reward = models.Reward.objects.get(pk=pk)
        for key, value in attrs.items():
            self.assertEqual(value, getattr(reward, key))
        self.assertEqual(reward.reporter, self.reporter)
        self.assertEqual(reward.vulnerability_type, self.vulnerability)
        self.assertEqual(reward.product, self.product)

    @factory_boy.django.mute_signals(signals.pre_save, signals.post_save)
    def test_reward_is_created(self):
        """Check that reward is created with minimum number of attrs.

        For example with absent ticket_info field
        https://st.yandex-team.ru/BUGBOUNTY-265
        """
        response = self.client.post(reverse('reward:api'), data=self.post_data)
        self.assertEquals(response.status_code, 200)
        self.assertRewardAttrsEqual(response.content, self.reward_attrs)

    @factory_boy.django.mute_signals(signals.pre_save, signals.post_save)
    def test_reward_is_created_with_ticket_number(self):
        """Check that reward is created with ticket_number parameter.

        https://st.yandex-team.ru/BUGBOUNTY-344
        """
        post_data = self.post_data.copy()
        post_data['ticket_number'] = 1
        reward_attrs = self.reward_attrs.copy()
        reward_attrs['ticket_number'] = 1
        response = self.client.post(reverse('reward:api'), data=post_data)
        self.assertEquals(response.status_code, 200)
        self.assertRewardAttrsEqual(response.content, reward_attrs)

    def test_reporter_does_not_exist(self):
        self.post_data['user_uid'] = 2
        response = self.client.post(reverse('reward:api'), data=self.post_data)
        self.assertEquals(response.status_code, 400)
        expected_response = json.dumps({
            'status': 'validation_error',
            'errors': {'__all__': ['reporter with uid 2 is not registered']},
        })
        self.assertJSONEqual(response.content, expected_response)


class ConstantsApiTest(TestCase):

    def setUp(self):
        factory.ProductFactory.create()

    def assert_response_length(self, url_name, expected_items):
        response = self.client.get(reverse(url_name))
        self.assertEqual(response['Content-Type'], 'application/json')
        items = json.loads(response.content)
        self.assertEqual(len(items), len(expected_items))

    def test_departments_api(self):
        self.assert_response_length(
            'reward:departments', models.Product.objects.all())

    def test_currency_api(self):
        self.assert_response_length(
            'reward:currency', models.Reward.CURRENCY)


@pytest.fixture
def reward_api_post_data(vulnerability, product):
   return {
       'staff_uid': 1000,
       'staff_login': 'admin',
       'startrek_ticket_code': 1,
       'department': product.pk,
       'payment_amount_usd': 100,
       'payment_amount_rur': 3000,  # Т_Т
       'points': 100,
       'auth_token': 'CHANGEME',
       'vulnerability_type': vulnerability.pk,
       'ticket_created': '2014-10-30 10:38:06',
   }


@pytest.fixture
def former_reward_api_post_data(reward_api_post_data, former_yandex_sponsored_reporter):
    reward_api_post_data['user_uid'] = former_yandex_sponsored_reporter.uid,
    return reward_api_post_data


@pytest.fixture
def sponsored_reward_api_post_data(reward_api_post_data, yandex_sponsored_reporter):
    reward_api_post_data['user_uid'] = yandex_sponsored_reporter.uid,
    return reward_api_post_data
