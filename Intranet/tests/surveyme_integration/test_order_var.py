# -*- coding: utf-8 -*-
from django.conf import settings
from django.test import TestCase
from unittest.mock import patch

from events.accounts.models import User
from events.balance.balance_client import BalanceClient
from events.balance.factories import OrderFactory, BalanceOrderFactory
from events.surveyme.factories import ProfileSurveyAnswerFactory
from events.surveyme_integration.variables import (
    OrderKeysFromBundleVariable,
)
from events.surveyme_keys.factories import SurveyKeysBundleFactory


class TestOrderKeysFromBundleVariable(TestCase):
    fixtures = ['initial_data.json']

    def setUp(self):
        self.answer = ProfileSurveyAnswerFactory()
        self.notification_id = '541c3d9b1c9eab1963657d26'
        self.bundle = SurveyKeysBundleFactory()
        self.robot_profile = User.objects.get(id=settings.ROBOT_USER_ID)

    def init_balance_orders(self):
        self.order = OrderFactory(profile_survey_answer=self.answer)

        # создадим 3 позиции в заказе с разным кол-вом купленных сущностей
        self.balance_orders = [
            BalanceOrderFactory(order=self.order, quantity=1),
            BalanceOrderFactory(order=self.order, quantity=2),
            BalanceOrderFactory(order=self.order, quantity=3),
        ]

        # общее кол-во сущностей
        self.full_quantity = sum([i.quantity for i in self.balance_orders])

        self.trigger_data = {
            'order_id': self.order.id
        }
        self.var_kwargs = {
            'answer': self.answer,
            'trigger_data': self.trigger_data,
            'notification_id': self.notification_id,
            'survey_keys_bundle': self.bundle.id
        }

    def test_should_generate_keys_in_bundle_for_every_item_in_order(self):
        with patch.object(BalanceClient, 'create_service_product'):
            self.init_balance_orders()

        OrderKeysFromBundleVariable(**self.var_kwargs).get_value()  # BANG!
        self.assertEqual(self.bundle.keys.count(), self.full_quantity)
        for key in self.bundle.keys.all():
            self.assertEqual(key.user, self.robot_profile)
            self.assertTrue(bool(key.value))
            self.assertTrue(key.is_active)
            self.assertFalse(bool(key.date_deactivated))
            self.assertFalse(bool(key.deactivated_by_answer))
            self.assertFalse(key.is_available)
            self.assertEqual(key.source, 'integration')
            self.assertEqual(key.integration_id, self.notification_id)

    def test_should_return_frontend_urls_of_generated_keys(self):
        with patch.object(BalanceClient, 'create_service_product'):
            self.init_balance_orders()

        response = OrderKeysFromBundleVariable(**self.var_kwargs).get_value()  # BANG!
        frontend_urls = [i.get_frontend_url() for i in self.bundle.keys.all().order_by('id')]
        expected = '\n'.join(['- %s' % i for i in frontend_urls])
        self.assertEqual(response, expected)

    def test_should_return_frontend_urls_of_keys_that_bound_to_this_integration_id(self):
        with patch.object(BalanceClient, 'create_service_product'):
            self.init_balance_orders()

        self.bundle.generate_keys(count=10, user_id=self.robot_profile.pk, is_available=False)
        response = OrderKeysFromBundleVariable(**self.var_kwargs).get_value()  # BANG!
        expected_keys = self.bundle.keys.all().filter(integration_id=self.notification_id)
        frontend_urls = [i.get_frontend_url() for i in expected_keys.order_by('id')]
        expected = '\n'.join(['- %s' % i for i in frontend_urls])
        self.assertEqual(response, expected)
