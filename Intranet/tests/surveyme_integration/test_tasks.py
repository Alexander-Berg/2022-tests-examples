# -*- coding: utf-8 -*-
from django.test import TestCase

from unittest.mock import patch, Mock, call

from events.surveyme_integration.tasks import calculate_notification_context
from events.surveyme_integration.factories import (
    ServiceSurveyHookSubscriptionFactory,
    HookSubscriptionNotificationFactory,
)
from events.surveyme.factories import SurveyFactory, ProfileSurveyAnswerFactory
from events.accounts.factories import UserFactory


class Test__calculate_notification_context(TestCase):
    fixtures = ['initial_data.json']

    def setUp(self):
        self.subscription = ServiceSurveyHookSubscriptionFactory()
        self.user = UserFactory(uid=None)
        self.survey = SurveyFactory()
        self.answer = ProfileSurveyAnswerFactory(user=self.user, survey=self.survey)
        self.notification = HookSubscriptionNotificationFactory(
            survey=self.survey, user=self.user,
            answer=self.answer, subscription=self.subscription
        )

    def test_should_override_language_for_context_calculation(self):
        mocked_manager = Mock()
        mocked_manager.__enter__ = Mock()
        mocked_manager.__exit__ = Mock()
        mocked_override_lang = Mock(return_value=mocked_manager)

        experiments = [
            {'exp_lang': 'ru', 'accept-language': 'en', 'context_language': 'ru'},
            {'exp_lang': 'en', 'accept-language': 'tr', 'context_language': 'en'},
            {'exp_lang': 'tr', 'accept-language': 'tr', 'context_language': 'from_request'},
        ]

        with patch('events.surveyme_integration.tasks.override_lang', mocked_override_lang):
            for experiment in experiments:
                self.subscription.context_language = experiment['context_language']
                self.subscription.save()
                self.answer.source_request = {'headers': {'accept-language': experiment['accept-language']}}
                self.user.save()

                service_class = self.subscription.service_type_action.service_type.get_service_class()

                calculate_notification_context(service_class, self.notification)
                self.assertEqual(call(experiment['exp_lang']), mocked_override_lang.call_args)
