# -*- coding: utf-8 -*-
from django.contrib.contenttypes.models import ContentType
from django.test import TestCase
from unittest.mock import patch

from events.accounts.helpers import YandexClient
from events.accounts.factories import UserFactory
from events.accounts.managers import UserManager
from events.surveyme.factories import SurveyFactory
from events.followme.factories import ContentFollowerFactory


class TestSurveyFollower(TestCase):
    fixtures = ['initial_data.json']
    client_class = YandexClient

    def setUp(self):
        self.user = self.client.login_yandex(is_superuser=True)
        self.survey = SurveyFactory()

    def test_shouldnt_return_followers(self):
        response = self.client.get(f'/admin/api/v2/surveys/{self.survey.pk}/followers/')
        self.assertEqual(response.status_code, 200)
        self.assertListEqual(response.data['results'], [])

    def test_should_return_follower(self):
        ContentFollowerFactory(
            type='user',
            user=self.user,
            content_type=ContentType.objects.get_for_model(self.survey),
            object_id=self.survey.pk,
        )
        response = self.client.get(f'/admin/api/v2/surveys/{self.survey.pk}/followers/')
        self.assertEqual(response.status_code, 200)
        self.assertEqual(len(response.data['results']), 1)
        follower = response.data['results'][0]
        self.assertEqual(follower['type'], 'user')
        self.assertEqual(follower['profile']['id'], self.user.pk)

    def test_should_return_mail_list(self):
        ContentFollowerFactory(
            type='mail_list',
            email='forms-dev@yandex-team.ru',
            content_type=ContentType.objects.get_for_model(self.survey),
            object_id=self.survey.pk,
        )
        response = self.client.get(f'/admin/api/v2/surveys/{self.survey.pk}/followers/')
        self.assertEqual(response.status_code, 200)
        self.assertEqual(len(response.data['results']), 1)
        follower = response.data['results'][0]
        self.assertEqual(follower['type'], 'mail_list')
        self.assertEqual(follower['email'], 'forms-dev@yandex-team.ru')

    def test_should_throw_bad_request(self):
        url = f'/admin/api/v2/surveys/{self.survey.pk}/followers/keep-only/'
        data = {}
        response = self.client.post(url, data=data, format='json')
        self.assertEqual(response.status_code, 400)

        data = {
            'followers': [{
                'type': 'incorrect',
            }],
        }
        response = self.client.post(url, data=data, format='json')
        self.assertEqual(response.status_code, 400)

        data = {
            'auth_type': 'incorrect',
            'followers': [],
        }
        response = self.client.post(url, data=data, format='json')
        self.assertEqual(response.status_code, 400)

    def test_should_keep_only_user(self):
        ContentFollowerFactory(
            type='mail_list',
            email='forms-dev@yandex-team.ru',
            content_type=ContentType.objects.get_for_model(self.survey),
            object_id=self.survey.pk,
        )
        url = f'/admin/api/v2/surveys/{self.survey.pk}/followers/keep-only/'
        data = {
            'followers': [{
                'type': 'user',
                'login': self.user.username,
            }],
        }
        with patch.object(UserManager, 'get_or_create_user', return_value=self.user):
            with patch('events.followme.api_admin.v2.views.get_user_uid_by_login', return_value=self.user.uid):
                response = self.client.post(url, data=data, format='json')
        self.assertEqual(response.status_code, 200)
        self.assertEqual(len(response.data['results']), 1)
        follower = response.data['results'][0]
        self.assertEqual(follower['type'], 'user')
        self.assertEqual(follower['profile']['id'], self.user.pk)

    def test_should_keep_only_mail_list(self):
        ContentFollowerFactory(
            type='user',
            user=self.user,
            content_type=ContentType.objects.get_for_model(self.survey),
            object_id=self.survey.pk,
        )
        url = f'/admin/api/v2/surveys/{self.survey.pk}/followers/keep-only/'
        data = {
            'followers': [{
                'type': 'mail_list',
                'email': 'forms-dev@yandex-team.ru',
            }],
        }
        response = self.client.post(url, data=data, format='json')
        self.assertEqual(response.status_code, 200)
        self.assertEqual(len(response.data['results']), 1)
        follower = response.data['results'][0]
        self.assertEqual(follower['type'], 'mail_list')
        self.assertEqual(follower['email'], 'forms-dev@yandex-team.ru')

    def test_should_drop_followers_list(self):
        ContentFollowerFactory(
            type='mail_list',
            email='forms-dev@yandex-team.ru',
            content_type=ContentType.objects.get_for_model(self.survey),
            object_id=self.survey.pk,
        )
        ContentFollowerFactory(
            type='user',
            user=self.user,
            content_type=ContentType.objects.get_for_model(self.survey),
            object_id=self.survey.pk,
        )
        url = f'/admin/api/v2/surveys/{self.survey.pk}/followers/keep-only/'
        data = {
            'followers': [],
        }
        response = self.client.post(url, data=data, format='json')
        self.assertEqual(response.status_code, 200)
        self.assertListEqual(response.data['results'], [])

    def test_should_add_new_followers(self):
        user1 = UserFactory()
        user2 = UserFactory()
        url = f'/admin/api/v2/surveys/{self.survey.pk}/followers/keep-only/'
        data = {
            'followers': [
                {'type': 'user', 'login': user1.username},
                {'type': 'user', 'login': user2.username},
            ],
        }
        with patch('events.followme.api_admin.v2.views.get_user_uid_by_login', side_effect=[user1.uid, user2.uid]):
            response = self.client.post(url, data=data, format='json')
        self.assertEqual(response.status_code, 200)

        self.survey.refresh_from_db()
        followers = list(self.survey.followers)
        self.assertEqual(len(followers), 2)
        users = {follower.user.pk for follower in followers}
        self.assertTrue(user1.pk in users)
        self.assertTrue(user2.pk in users)
