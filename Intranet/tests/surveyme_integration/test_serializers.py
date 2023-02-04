from django.test import TestCase
from events.accounts.factories import UserFactory
from events.surveyme.factories import ProfileSurveyAnswerFactory
from events.surveyme_integration.api_admin.v2.serializers import (
    HookSubscriptionNotificationResultSerializer,
)
from events.surveyme_integration.factories import (
    HookSubscriptionNotificationFactory,
    ServiceSurveyHookSubscriptionFactory,
)
from events.surveyme_integration.models import HookSubscriptionNotification


class TestHookSubscriptionNotificationResultSerializer(TestCase):
    fixtures = ['initial_data.json']
    serializer = HookSubscriptionNotificationResultSerializer

    def setUp(self):
        self.subscription = ServiceSurveyHookSubscriptionFactory(
            service_type_action_id=7,  # startrek/create ticket
            title='Title goes here',
            body='Body goes here',
            follow_result=True,
        )
        self.survey = self.subscription.survey_hook.survey
        self.user = UserFactory()
        self.answer = ProfileSurveyAnswerFactory(
            survey=self.survey,
            user=self.user,
        )
        self.answer.data = {
            'id': self.answer.pk,
            'survey': {
                'id': str(self.survey.pk),
            },
            'data': [],
        }
        self.answer.save()

    def test_startrek_with_summary(self):
        notification = HookSubscriptionNotificationFactory(
            subscription=self.subscription,
            survey=self.survey,
            answer=self.answer,
            user=self.user,
            status='success',
            response={
                'issue': {
                    'key': 'TESTME-123',
                    'summary': 'Title goes here',
                },
            },
        )
        queryset = (
            HookSubscriptionNotification.objects.filter(pk=notification.pk)
            .filter_notifications()
        )
        serializer = self.serializer(queryset, many=True)
        self.assertIn(str(notification.pk), serializer.data)
        result = serializer.data[str(notification.pk)]
        self.assertEqual(result['integration_type'], 'startrek')
        self.assertEqual(result['status'], 'success')
        self.assertEqual(result['resources'], {
            'key': 'TESTME-123: Title goes here',
            'link': 'https://st.test.yandex-team.ru/TESTME-123',
        })

    def test_startrek_without_summary(self):
        notification = HookSubscriptionNotificationFactory(
            subscription=self.subscription,
            survey=self.survey,
            answer=self.answer,
            user=self.user,
            status='success',
            response={
                'issue': {
                    'key': 'TESTME-123',
                },
            },
            context={
                'title': 'Title goes here',
            },
        )
        queryset = (
            HookSubscriptionNotification.objects.filter(pk=notification.pk)
            .filter_notifications()
        )
        serializer = self.serializer(queryset, many=True)
        self.assertIn(str(notification.pk), serializer.data)
        result = serializer.data[str(notification.pk)]
        self.assertEqual(result['integration_type'], 'startrek')
        self.assertEqual(result['status'], 'success')
        self.assertEqual(result['resources'], {
            'key': 'TESTME-123: Title goes here',
            'link': 'https://st.test.yandex-team.ru/TESTME-123',
        })

    def test_startrek_error_with_message(self):
        notification = HookSubscriptionNotificationFactory(
            subscription=self.subscription,
            survey=self.survey,
            answer=self.answer,
            user=self.user,
            status='error',
            response={
            },
            error={
                'message': 'Permission denied',
            },
        )
        queryset = (
            HookSubscriptionNotification.objects.filter(pk=notification.pk)
            .filter_notifications()
        )
        serializer = self.serializer(queryset, many=True)
        self.assertIn(str(notification.pk), serializer.data)
        result = serializer.data[str(notification.pk)]
        self.assertEqual(result['integration_type'], 'startrek')
        self.assertEqual(result['status'], 'error')
        self.assertEqual(result['resources'], {
            'message': 'Permission denied',
        })
