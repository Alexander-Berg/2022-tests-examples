# -*- coding: utf-8 -*-
import responses

from django.test import TestCase
from unittest.mock import patch, Mock

from events.surveyme_integration.exceptions import WIKI_PAGE_DOESNT_EXISTS_MESSAGE
from events.surveyme_integration.models import WikiSubscriptionData, HookSubscriptionNotification
from events.surveyme_integration.helpers import IntegrationTestMixin
from events.yauth_contrib.auth import TvmAuth


@patch.object(TvmAuth, '_get_service_ticket', Mock('123'))
class TestWikiIntegration(IntegrationTestMixin, TestCase):
    fixtures = ['initial_data.json']

    def setUp(self):
        super().setUp()
        self.subscription.service_type_action_id = 9  # wiki
        self.subscription.title = 'Title goes here'
        self.subscription.body = 'Body goes here'
        self.subscription.save()

        self.wiki_subscriptiondata = WikiSubscriptionData.objects.create(
            subscription=self.subscription,
            supertag='test/me',
            text='some text'
        )

    @responses.activate
    def test_should_add_data_to_page(self):
        responses.add(
            responses.GET,
            'https://wiki-api.test.yandex-team.ru/_api/frontend/test/me/.raw',
            json={
                'data': {
                    'title': 'testme',
                    'body': 'test it\n',
                    'version': '27289577',
                    'supertag': 'test/me'
                },
            },
        )
        responses.add(
            responses.POST,
            'https://wiki-api.test.yandex-team.ru/_api/frontend/test/me',
            json={},
        )

        self.post_data()  # BANG!

        notifications = list(HookSubscriptionNotification.objects.filter(subscription=self.subscription))
        self.assertEqual(len(notifications), 1)
        notification = notifications[0]
        self.assertEqual(notification.status, 'success')
        self.assertTrue(notification.is_visible)
        self.assertEqual(len(responses.calls), 2)

    @responses.activate
    def test_should_throw_not_found_error(self):
        responses.add(
            responses.GET,
            'https://wiki-api.test.yandex-team.ru/_api/frontend/test/me/.raw',
            json={
                'error_code': 'NOT_FOUND',
                'level': 'ERROR',
                'message': 'Page cannot be found',
            },
            status=404,
        )

        self.post_data()  # BANG!

        notifications = list(HookSubscriptionNotification.objects.filter(subscription=self.subscription))
        self.assertEqual(len(notifications), 1)
        notification = notifications[0]
        self.assertEqual(notification.status, 'error')
        self.assertTrue(notification.is_visible)
        self.assertEqual(len(responses.calls), 1)
        self.assertEqual(
            notification.error['message'],
            WIKI_PAGE_DOESNT_EXISTS_MESSAGE % self.wiki_subscriptiondata.supertag,
        )
