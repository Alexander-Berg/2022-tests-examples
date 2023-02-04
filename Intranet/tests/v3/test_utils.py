from django.test import TestCase

from events.surveyme_integration.models import ServiceTypeAction
from events.v3.types import SubscriptionType
from events.v3.utils import get_subscription_type_by_action_id


class TestGetSubsctiptionTypeByActionId(TestCase):
    fixtures = ['initial_data.json']

    def test_get_subscription_type_by_action_id_should_return_subscription_type(self):
        action_ids = ServiceTypeAction.objects.values_list('pk', flat=True)
        self.assertTrue(len(action_ids) > 0)
        for action_id in action_ids:
            self.assertTrue(isinstance(get_subscription_type_by_action_id(action_id), SubscriptionType))

        self.assertEqual(get_subscription_type_by_action_id(3), SubscriptionType.email)
        self.assertEqual(get_subscription_type_by_action_id(4), SubscriptionType.post)
        self.assertEqual(get_subscription_type_by_action_id(6), SubscriptionType.jsonrpc)
        self.assertEqual(get_subscription_type_by_action_id(7), SubscriptionType.tracker)
        self.assertEqual(get_subscription_type_by_action_id(8), SubscriptionType.tracker)
        self.assertEqual(get_subscription_type_by_action_id(9), SubscriptionType.wiki)
        self.assertEqual(get_subscription_type_by_action_id(10), SubscriptionType.put)
        self.assertEqual(get_subscription_type_by_action_id(11), SubscriptionType.http)

    def test_get_subscription_type_by_action_id_should_raise_value_error(self):
        with self.assertRaises(ValueError):
            get_subscription_type_by_action_id(999)
