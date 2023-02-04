import fnmatch
from collections import defaultdict
from datetime import datetime
from typing import Any, Dict, List

from unittest.mock import patch
from django.utils import timezone
from django.test import TestCase, override_settings

from events.accounts.helpers import YandexClient
from events.common_app.sender.client import SenderClient
from events.common_app.redis import Redis
from events.surveyme.factories import (
    SurveyFactory,
    ProfileSurveyAnswerFactory,
)
from events.surveyme_integration.factories import (
    HookSubscriptionNotificationFactory,
    ServiceSurveyHookSubscriptionFactory,
    SurveyHookFactory,
)

from events.surveyme_integration.tasks import (
    build_key_glob_for_get_aggregated_notification_info,
    get_aggregated_notification_info_by_key_glob_from_redis_and_send,
    mark_notification_as_failed,
)


class TestError(Exception):
    pass


class TestError2(Exception):
    pass


class RedisMock:
    def __init__(self):
        self.data = defaultdict(dict)

    def hset(self, name: str, key: str, value: Any):
        self.data[name][key] = value

    def delete(self, *keys: str):
        for key in keys:
            self.data.pop(key)

    def keys(self, pattern: str = '*') -> List[Any]:
        return fnmatch.filter(self.data.keys(), pattern)

    def hincrby(self, name: str, key: str):
        self.data[name][key] = self.data[name].get(key, 0) + 1

    def hgetall(self, name: str) -> Dict[str, Any]:
        return self.data.get(name)


class BaseTestMixin:
    def setUpRedis(self):
        self.redis = RedisMock()

    def mark_notification_as_failed(self, error, amount, notification=None):
        if notification is None:
            notification = self.notification
        with (
            patch.object(Redis, 'hset', self.redis.hset),
            patch.object(Redis, 'hincrby', self.redis.hincrby),
        ):
            for _ in range(amount):
                mark_notification_as_failed(notification, error)

    def get_aggregated_error_info(self, survey=None) -> Dict[str, Any]:
        if survey is None:
            survey = self.survey

        key_glob = build_key_glob_for_get_aggregated_notification_info(survey.follow_type)

        data = {}

        def send_email(self, campaign, args):
            data[args.pop('subject')] = args

        with (
            patch.object(Redis, 'keys', self.redis.keys),
            patch.object(Redis, 'hgetall', self.redis.hgetall),
            patch.object(Redis, 'delete', self.redis.delete),
            patch.object(SenderClient, 'send_email', send_email),
        ):
            get_aggregated_notification_info_by_key_glob_from_redis_and_send(key_glob)

        return data


class TestBuildKeyGlob(TestCase):
    def test_build_key_glob(self):
        dt = datetime(2022, 1, 1, 5, 5, 15, tzinfo=timezone.utc)
        with patch.object(timezone, 'now', return_value=dt):
            self.assertEqual(build_key_glob_for_get_aggregated_notification_info('5m'), 'follow:5m:0500')
            self.assertEqual(build_key_glob_for_get_aggregated_notification_info('1h'), 'follow:1h:04')
            self.assertEqual(build_key_glob_for_get_aggregated_notification_info('1d'), 'follow:1d:')


class TestNotificationFailedIntegrationFollowType1d(TestCase, BaseTestMixin):
    fixtures = ['initial_data.json']
    client_class = YandexClient

    def setUp(self):
        self.user = self.client.login_yandex(email='test@yandex-team.ru')
        self.survey = SurveyFactory(follow_type='1d')
        self.answer = ProfileSurveyAnswerFactory(survey=self.survey)
        self.user.follow(self.survey)
        survey_hook = SurveyHookFactory(survey=self.survey)
        self.subscription = ServiceSurveyHookSubscriptionFactory(
            survey_hook=survey_hook,
            service_type_action_id=4,  # rpc post
            http_url='http://yandex.ru/test_url/',
        )
        self.notification = HookSubscriptionNotificationFactory(
            survey=self.survey,
            answer=self.answer,
            subscription=self.subscription,
            trigger_slug='create',
        )
        self.setUpRedis()

    @override_settings(USE_NEW_FOLLOWERS=True)
    def test_one_error(self):
        self.mark_notification_as_failed(TestError(), 1)
        self.assertEqual(len(self.redis.keys()), 1)

        data = self.get_aggregated_error_info()

        self.assertEqual(len(self.redis.keys()), 0)

        subject = f'[Ошибка интеграции] {self.survey.name}'
        self.assertEqual(len(data), 1)
        email_body = data[subject]
        self.assertEqual(email_body['survey_name'], self.survey.name)
        self.assertEqual(len(email_body['subscription_errors']), 1)
        error = email_body['subscription_errors']['1:TestError']
        self.assertEqual(error['notifications_count'], 1)

    @override_settings(USE_NEW_FOLLOWERS=True)
    def test_five_errors_one_type(self):
        self.mark_notification_as_failed(TestError(), 5)
        self.assertEqual(len(self.redis.keys()), 1)

        data = self.get_aggregated_error_info()

        self.assertEqual(len(self.redis.keys()), 0)

        subject = f'[Ошибка интеграции] {self.survey.name}'
        self.assertEqual(len(data), 1)
        email_body = data[subject]
        self.assertEqual(email_body['survey_name'], self.survey.name)
        self.assertEqual(len(email_body['subscription_errors']), 1)
        error = email_body['subscription_errors']['1:TestError']
        self.assertEqual(error['notifications_count'], 5)

    @override_settings(USE_NEW_FOLLOWERS=True)
    def test_two_errors_different_types(self):
        self.mark_notification_as_failed(TestError(), 1)
        self.mark_notification_as_failed(TestError2(), 1)
        self.assertEqual(len(self.redis.keys()), 2)

        data = self.get_aggregated_error_info()

        self.assertEqual(len(self.redis.keys()), 0)

        subject = f'[Ошибка интеграции] {self.survey.name}'
        self.assertEqual(len(data), 1)
        email_body = data[subject]
        self.assertEqual(email_body['survey_name'], self.survey.name)
        self.assertEqual(len(email_body['subscription_errors']), 2)
        error_first = email_body['subscription_errors']['1:TestError']
        self.assertEqual(error_first['notifications_count'], 1)
        error_second = email_body['subscription_errors']['1:TestError2']
        self.assertEqual(error_second['notifications_count'], 1)

    @override_settings(USE_NEW_FOLLOWERS=True)
    def test_two_surveys(self):
        other_survey = SurveyFactory(follow_type='1d')
        self.user.follow(other_survey)

        other_notification = HookSubscriptionNotificationFactory(
            survey=other_survey,
            answer=self.answer,
            subscription=self.subscription,
            trigger_slug='create',
        )

        self.mark_notification_as_failed(TestError(), 1)
        self.mark_notification_as_failed(TestError2(), 3, other_notification)

        self.assertEqual(len(self.redis.keys()), 2)

        data = self.get_aggregated_error_info()

        self.assertEqual(len(self.redis.keys()), 0)

        subject = f'[Ошибка интеграции] {self.survey.name}'
        other_subject = f'[Ошибка интеграции] {other_survey.name}'
        self.assertEqual(len(data), 2)

        email_body = data[subject]
        self.assertEqual(email_body['survey_name'], self.survey.name)
        self.assertEqual(len(email_body['subscription_errors']), 1)
        error = email_body['subscription_errors']['1:TestError']
        self.assertEqual(error['notifications_count'], 1)

        other_email_body = data[other_subject]
        self.assertEqual(other_email_body['survey_name'], other_survey.name)
        self.assertEqual(len(other_email_body['subscription_errors']), 1)
        error = other_email_body['subscription_errors']['1:TestError2']
        self.assertEqual(error['notifications_count'], 3)


class TestNotificationFailedIntegrationFollowType1h(TestCase, BaseTestMixin):
    fixtures = ['initial_data.json']
    client_class = YandexClient

    def setUp(self):
        self.user = self.client.login_yandex(email='test@yandex-team.ru')
        self.survey = SurveyFactory(follow_type='1h')
        self.answer = ProfileSurveyAnswerFactory(survey=self.survey)
        self.user.follow(self.survey)
        survey_hook = SurveyHookFactory(survey=self.survey)
        self.subscription = ServiceSurveyHookSubscriptionFactory(
            survey_hook=survey_hook,
            service_type_action_id=4,  # rpc post
            http_url='http://yandex.ru/test_url/',
        )
        self.notification = HookSubscriptionNotificationFactory(
            survey=self.survey,
            answer=self.answer,
            subscription=self.subscription,
            trigger_slug='create',
        )
        self.setUpRedis()

    @override_settings(USE_NEW_FOLLOWERS=True)
    def test_one_error(self):
        dt = datetime(2022, 1, 1, 5, 5, 15, tzinfo=timezone.utc)
        with patch.object(timezone, 'now', return_value=dt):
            self.mark_notification_as_failed(TestError(), 1)

        self.assertEqual(len(self.redis.keys()), 1)

        dt = datetime(2022, 1, 1, 6, 5, 15, tzinfo=timezone.utc)
        with patch.object(timezone, 'now', return_value=dt):
            data = self.get_aggregated_error_info()

        self.assertEqual(len(self.redis.keys()), 0)

        subject = f'[Ошибка интеграции] {self.survey.name}'
        self.assertEqual(len(data), 1)
        email_body = data[subject]
        self.assertEqual(email_body['survey_name'], self.survey.name)
        self.assertEqual(len(email_body['subscription_errors']), 1)
        error = email_body['subscription_errors']['1:TestError']
        self.assertEqual(error['notifications_count'], 1)

    @override_settings(USE_NEW_FOLLOWERS=True)
    def test_five_errors(self):
        dt = datetime(2022, 1, 1, 5, 5, 15, tzinfo=timezone.utc)
        with patch.object(timezone, 'now', return_value=dt):
            self.mark_notification_as_failed(TestError(), 5)

        self.assertEqual(len(self.redis.keys()), 1)

        dt = datetime(2022, 1, 1, 6, 5, 15, tzinfo=timezone.utc)
        with patch.object(timezone, 'now', return_value=dt):
            data = self.get_aggregated_error_info()

        self.assertEqual(len(self.redis.keys()), 0)

        subject = f'[Ошибка интеграции] {self.survey.name}'
        self.assertEqual(len(data), 1)
        email_body = data[subject]
        self.assertEqual(email_body['survey_name'], self.survey.name)
        self.assertEqual(len(email_body['subscription_errors']), 1)
        error = email_body['subscription_errors']['1:TestError']
        self.assertEqual(error['notifications_count'], 5)

    @override_settings(USE_NEW_FOLLOWERS=True)
    def test_two_errors_different_types(self):
        dt = datetime(2022, 1, 1, 5, 5, 15, tzinfo=timezone.utc)
        with patch.object(timezone, 'now', return_value=dt):
            self.mark_notification_as_failed(TestError(), 1)
            self.mark_notification_as_failed(TestError2(), 1)

        self.assertEqual(len(self.redis.keys()), 2)

        dt = datetime(2022, 1, 1, 6, 5, 15, tzinfo=timezone.utc)
        with patch.object(timezone, 'now', return_value=dt):
            data = self.get_aggregated_error_info()

        self.assertEqual(len(self.redis.keys()), 0)

        subject = f'[Ошибка интеграции] {self.survey.name}'
        self.assertEqual(len(data), 1)
        email_body = data[subject]
        self.assertEqual(email_body['survey_name'], self.survey.name)
        self.assertEqual(len(email_body['subscription_errors']), 2)
        error_first = email_body['subscription_errors']['1:TestError']
        self.assertEqual(error_first['notifications_count'], 1)
        error_second = email_body['subscription_errors']['1:TestError2']
        self.assertEqual(error_second['notifications_count'], 1)

    @override_settings(USE_NEW_FOLLOWERS=True)
    def test_two_surveys(self):
        other_survey = SurveyFactory(follow_type='1h')
        self.user.follow(other_survey)

        other_notification = HookSubscriptionNotificationFactory(
            survey=other_survey,
            answer=self.answer,
            subscription=self.subscription,
            trigger_slug='create',
        )
        dt = datetime(2022, 1, 1, 5, 5, 15, tzinfo=timezone.utc)
        with patch.object(timezone, 'now', return_value=dt):
            self.mark_notification_as_failed(TestError(), 1)
            self.mark_notification_as_failed(TestError2(), 3, other_notification)

        self.assertEqual(len(self.redis.keys()), 2)

        dt = datetime(2022, 1, 1, 6, 5, 15, tzinfo=timezone.utc)
        with patch.object(timezone, 'now', return_value=dt):
            data = self.get_aggregated_error_info()

        self.assertEqual(len(self.redis.keys()), 0)

        subject = f'[Ошибка интеграции] {self.survey.name}'
        other_subject = f'[Ошибка интеграции] {other_survey.name}'
        self.assertEqual(len(data), 2)

        email_body = data[subject]
        self.assertEqual(email_body['survey_name'], self.survey.name)
        self.assertEqual(len(email_body['subscription_errors']), 1)
        error = email_body['subscription_errors']['1:TestError']
        self.assertEqual(error['notifications_count'], 1)

        other_email_body = data[other_subject]
        self.assertEqual(other_email_body['survey_name'], other_survey.name)
        self.assertEqual(len(other_email_body['subscription_errors']), 1)
        error = other_email_body['subscription_errors']['1:TestError2']
        self.assertEqual(error['notifications_count'], 3)


class TestNotificationFailedIntegrationFollowType5m(TestCase, BaseTestMixin):
    fixtures = ['initial_data.json']
    client_class = YandexClient

    def setUp(self):
        self.user = self.client.login_yandex(email='test@yandex-team.ru')
        self.survey = SurveyFactory(follow_type='5m')
        self.answer = ProfileSurveyAnswerFactory(survey=self.survey)
        self.user.follow(self.survey)
        survey_hook = SurveyHookFactory(survey=self.survey)
        self.subscription = ServiceSurveyHookSubscriptionFactory(
            survey_hook=survey_hook,
            service_type_action_id=4,  # rpc post
            http_url='http://yandex.ru/test_url/',
        )
        self.notification = HookSubscriptionNotificationFactory(
            survey=self.survey,
            answer=self.answer,
            subscription=self.subscription,
            trigger_slug='create',
        )
        self.setUpRedis()

    @override_settings(USE_NEW_FOLLOWERS=True)
    def test_one_error(self):
        dt = datetime(2022, 1, 1, 5, 5, 15, tzinfo=timezone.utc)
        with patch.object(timezone, 'now', return_value=dt):
            self.mark_notification_as_failed(TestError(), 1)

        self.assertEqual(len(self.redis.keys()), 1)

        dt = datetime(2022, 1, 1, 5, 10, 15, tzinfo=timezone.utc)
        with patch.object(timezone, 'now', return_value=dt):
            data = self.get_aggregated_error_info()

        self.assertEqual(len(self.redis.keys()), 0)

        subject = f'[Ошибка интеграции] {self.survey.name}'
        self.assertEqual(len(data), 1)
        email_body = data[subject]
        self.assertEqual(email_body['survey_name'], self.survey.name)
        self.assertEqual(len(email_body['subscription_errors']), 1)
        error = email_body['subscription_errors']['1:TestError']
        self.assertEqual(error['notifications_count'], 1)

    @override_settings(USE_NEW_FOLLOWERS=True)
    def test_five_errors(self):
        dt = datetime(2022, 1, 1, 5, 5, 15, tzinfo=timezone.utc)
        with patch.object(timezone, 'now', return_value=dt):
            self.mark_notification_as_failed(TestError(), 5)

        self.assertEqual(len(self.redis.keys()), 1)

        dt = datetime(2022, 1, 1, 5, 10, 15, tzinfo=timezone.utc)
        with patch.object(timezone, 'now', return_value=dt):
            data = self.get_aggregated_error_info()

        self.assertEqual(len(self.redis.keys()), 0)

        subject = f'[Ошибка интеграции] {self.survey.name}'
        self.assertEqual(len(data), 1)
        email_body = data[subject]
        self.assertEqual(email_body['survey_name'], self.survey.name)
        self.assertEqual(len(email_body['subscription_errors']), 1)
        error = email_body['subscription_errors']['1:TestError']
        self.assertEqual(error['notifications_count'], 5)

    @override_settings(USE_NEW_FOLLOWERS=True)
    def test_two_errors_different_types(self):
        dt = datetime(2022, 1, 1, 5, 5, 15, tzinfo=timezone.utc)
        with patch.object(timezone, 'now', return_value=dt):
            self.mark_notification_as_failed(TestError(), 1)
            self.mark_notification_as_failed(TestError2(), 1)

        self.assertEqual(len(self.redis.keys()), 2)

        dt = datetime(2022, 1, 1, 5, 10, 15, tzinfo=timezone.utc)
        with patch.object(timezone, 'now', return_value=dt):
            data = self.get_aggregated_error_info()

        self.assertEqual(len(self.redis.keys()), 0)

        subject = f'[Ошибка интеграции] {self.survey.name}'
        self.assertEqual(len(data), 1)
        email_body = data[subject]
        self.assertEqual(email_body['survey_name'], self.survey.name)
        self.assertEqual(len(email_body['subscription_errors']), 2)
        error_first = email_body['subscription_errors']['1:TestError']
        self.assertEqual(error_first['notifications_count'], 1)
        error_second = email_body['subscription_errors']['1:TestError2']
        self.assertEqual(error_second['notifications_count'], 1)

    @override_settings(USE_NEW_FOLLOWERS=True)
    def test_two_surveys(self):
        other_survey = SurveyFactory(follow_type='5m')
        self.user.follow(other_survey)

        other_notification = HookSubscriptionNotificationFactory(
            survey=other_survey,
            answer=self.answer,
            subscription=self.subscription,
            trigger_slug='create',
        )
        dt = datetime(2022, 1, 1, 5, 5, 15, tzinfo=timezone.utc)
        with patch.object(timezone, 'now', return_value=dt):
            self.mark_notification_as_failed(TestError(), 1)
            self.mark_notification_as_failed(TestError2(), 3, other_notification)

        self.assertEqual(len(self.redis.keys()), 2)

        dt = datetime(2022, 1, 1, 5, 10, 15, tzinfo=timezone.utc)
        with patch.object(timezone, 'now', return_value=dt):
            data = self.get_aggregated_error_info()

        self.assertEqual(len(self.redis.keys()), 0)

        subject = f'[Ошибка интеграции] {self.survey.name}'
        other_subject = f'[Ошибка интеграции] {other_survey.name}'
        self.assertEqual(len(data), 2)

        email_body = data[subject]
        self.assertEqual(email_body['survey_name'], self.survey.name)
        self.assertEqual(len(email_body['subscription_errors']), 1)
        error = email_body['subscription_errors']['1:TestError']
        self.assertEqual(error['notifications_count'], 1)

        other_email_body = data[other_subject]
        self.assertEqual(other_email_body['survey_name'], other_survey.name)
        self.assertEqual(len(other_email_body['subscription_errors']), 1)
        error = other_email_body['subscription_errors']['1:TestError2']
        self.assertEqual(error['notifications_count'], 3)


class TestNotificationFailedIntegrationFollowTypeNone(TestCase):
    fixtures = ['initial_data.json']
    client_class = YandexClient

    def setUp(self):
        self.user = self.client.login_yandex(email='test@yandex-team.ru')
        self.survey = SurveyFactory(follow_type=None)
        self.answer = ProfileSurveyAnswerFactory(survey=self.survey)
        self.user.follow(self.survey)
        survey_hook = SurveyHookFactory(survey=self.survey)
        self.subscription = ServiceSurveyHookSubscriptionFactory(
            survey_hook=survey_hook,
            service_type_action_id=4,  # rpc post
            http_url='http://yandex.ru/test_url/',
        )
        self.notification = HookSubscriptionNotificationFactory(
            survey=self.survey,
            answer=self.answer,
            subscription=self.subscription,
            trigger_slug='create',
        )

    @override_settings(USE_NEW_FOLLOWERS=True)
    def test_one_error_one_email(self):
        data = {}

        def send_email(self, campaign, args):
            data[args.pop('subject')] = args

        with (
            patch.object(SenderClient, 'send_email', send_email),
        ):
            mark_notification_as_failed(self.notification, TestError())

        subject = f'[Ошибка интеграции] {self.survey.name}'
        self.assertEqual(len(data), 1)
        email_body = data[subject]
        self.assertEqual(email_body['survey_name'], self.survey.name)
        self.assertEqual(email_body['error'], 'TestError')
        self.assertEqual(email_body['subscription_id'], self.subscription.id)

    @override_settings(USE_NEW_FOLLOWERS=True)
    def test_two_errors_two_emais(self):
        class SenderCounter:
            def __init__(self):
                self.count = 0

            def send_email(self, campaign, args):
                self.count += 1

        sender_counter = SenderCounter()

        with (
            patch.object(SenderClient, 'send_email', sender_counter.send_email),
        ):
            mark_notification_as_failed(self.notification, TestError())
            mark_notification_as_failed(self.notification, TestError())

        self.assertEqual(sender_counter.count, 2)


class TestNotificationFailedIntegrationDifferentFollowTypes(TestCase, BaseTestMixin):
    fixtures = ['initial_data.json']
    client_class = YandexClient

    def setUp(self):
        self.user = self.client.login_yandex(email='test@yandex-team.ru')
        self.survey_5m = SurveyFactory(follow_type='5m')
        self.answer_5m = ProfileSurveyAnswerFactory(survey=self.survey_5m)
        self.user.follow(self.survey_5m)
        survey_hook = SurveyHookFactory(survey=self.survey_5m)
        self.subscription_5m = ServiceSurveyHookSubscriptionFactory(
            survey_hook=survey_hook,
            service_type_action_id=4,  # rpc post
            http_url='http://yandex.ru/test_url/',
        )
        self.notification_5m = HookSubscriptionNotificationFactory(
            survey=self.survey_5m,
            answer=self.answer_5m,
            subscription=self.subscription_5m,
            trigger_slug='create',
        )
        self.survey_1h = SurveyFactory(follow_type='1h')
        self.answer_1h = ProfileSurveyAnswerFactory(survey=self.survey_1h)
        self.user.follow(self.survey_1h)
        survey_hook = SurveyHookFactory(survey=self.survey_1h)
        self.subscription_1h = ServiceSurveyHookSubscriptionFactory(
            survey_hook=survey_hook,
            service_type_action_id=4,  # rpc post
            http_url='http://yandex.ru/test_url/',
        )
        self.notification_1h = HookSubscriptionNotificationFactory(
            survey=self.survey_1h,
            answer=self.answer_1h,
            subscription=self.subscription_1h,
            trigger_slug='create',
        )
        self.survey_1d = SurveyFactory(follow_type='1d')
        self.answer_1d = ProfileSurveyAnswerFactory(survey=self.survey_1d)
        self.user.follow(self.survey_1d)
        survey_hook = SurveyHookFactory(survey=self.survey_1d)
        self.subscription_1d = ServiceSurveyHookSubscriptionFactory(
            survey_hook=survey_hook,
            service_type_action_id=4,  # rpc post
            http_url='http://yandex.ru/test_url/',
        )
        self.notification_1d = HookSubscriptionNotificationFactory(
            survey=self.survey_1d,
            answer=self.answer_1d,
            subscription=self.subscription_1d,
            trigger_slug='create',
        )
        self.setUpRedis()

    @override_settings(USE_NEW_FOLLOWERS=True)
    def test_get_only_necessary_errors(self):
        self.mark_notification_as_failed(TestError(), 1, self.notification_1d)
        dt = datetime(2022, 1, 1, 5, 5, 15, tzinfo=timezone.utc)
        with patch.object(timezone, 'now', return_value=dt):
            self.mark_notification_as_failed(TestError(), 1, self.notification_1h)
            self.mark_notification_as_failed(TestError(), 1, self.notification_5m)

        self.assertEqual(len(self.redis.keys()), 3)

        self.get_aggregated_error_info(self.survey_1d)

        self.assertEqual(len(self.redis.keys()), 2)

        dt = datetime(2022, 1, 1, 6, 5, 15, tzinfo=timezone.utc)
        with patch.object(timezone, 'now', return_value=dt):
            self.get_aggregated_error_info(self.survey_1h)

        self.assertEqual(len(self.redis.keys()), 1)

        dt = datetime(2022, 1, 1, 5, 10, 15, tzinfo=timezone.utc)
        with patch.object(timezone, 'now', return_value=dt):
            self.get_aggregated_error_info(self.survey_5m)

        self.assertEqual(len(self.redis.keys()), 0)
