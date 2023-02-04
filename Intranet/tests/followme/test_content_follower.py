# -*- coding: utf-8 -*-
from django.test import TestCase
from django.core.exceptions import ValidationError
from django.db.utils import IntegrityError

from events.followme.factories import ContentFollowerFactory
from events.accounts.factories import UserFactory
from events.surveyme.factories import SurveyFactory


class TestContentFollower(TestCase):
    fixtures = ['initial_data.json']

    def setUp(self):
        self.user = UserFactory()
        self.content_follower = ContentFollowerFactory(type='mail_list', email='test-email@yandex.ru')
        self.survey = SurveyFactory()

    def test_validation_should_be_raised_error_if_profile_and_email_is_empty(self):
        self.content_follower.type = 'user'
        self.content_follower.email = None
        self.content_follower.user = self.user
        self.content_follower.clean_fields()

        self.content_follower.type = 'mail_list'
        self.content_follower.email = 'test-email@yandex.ru'
        self.content_follower.user = None
        self.content_follower.clean_fields()

        self.content_follower.type = 'mail_list'
        self.content_follower.email = None
        self.content_follower.user = None
        self.assertRaises(ValidationError, self.content_follower.clean_fields)

        self.content_follower.type = 'user'
        self.content_follower.email = None
        self.content_follower.user = None
        self.assertRaises(ValidationError, self.content_follower.clean_fields)

    def test_should_be_raised_error_if_is_not_unique_email(self):
        first_content_follower = ContentFollowerFactory(email='first@yandex.ru', type='mail_list', content_object=self.survey)
        second_content_follower = ContentFollowerFactory(email='second@yandex.ru', type='mail_list', content_object=self.survey)
        third_content_follower = ContentFollowerFactory(email='third@yandex.ru', type='mail_list', content_object=self.survey)
        first_content_follower.save()
        second_content_follower.save()
        third_content_follower.save()

        second_content_follower.email = first_content_follower.email
        self.assertRaises(IntegrityError, second_content_follower.save)

    def test_should_be_raised_error_if_is_not_unique_user(self):
        first_content_follower = ContentFollowerFactory(email='first@yandex.ru', type='mail_list', content_object=self.survey)
        second_content_follower = ContentFollowerFactory(email='second@yandex.ru', type='mail_list', content_object=self.survey)

        first_content_follower.user = self.user
        first_content_follower.email = None
        first_content_follower.type = 'user'
        first_content_follower.content_object = self.survey
        first_content_follower.save()

        second_content_follower.user = self.user
        second_content_follower.email = None
        second_content_follower.type = 'user'
        second_content_follower.content_object = self.survey
        self.assertRaises(IntegrityError, second_content_follower.save)

    def test_get_email_for_user_type(self):
        self.content_follower.type = 'user'
        self.content_follower.user = self.user
        self.user.username = 'akhmetov'
        self.user.save()

        self.assertEqual(self.content_follower.get_email(), '%s@yandex-team.ru' % self.user.username)

    def test_get_email_for_mail_list_type(self):
        self.content_follower.type = 'mail_list'
        self.content_follower.email = 'bbs@yandex-team.ru'

        self.assertEqual(self.content_follower.get_email(), self.content_follower.email)
