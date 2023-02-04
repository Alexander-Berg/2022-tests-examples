# -*- coding: utf-8 -*-
import datetime

from bson import ObjectId
from django.utils import timezone
from django.test import TestCase, override_settings
from unittest.mock import patch, ANY

from events.accounts.factories import UserFactory
from events.common_app.blackbox_requests import JsonBlackbox
from events.surveyme.factories import SurveyFactory
from events.surveyme.tasks import check_surveys_publication_status
from events.surveyme.utils import SurveyAutoPublicationCommand
from events.history.models import HistoryRawEntry


class Test__check_surveys_publication_status(TestCase):
    fixtures = ['initial_data.json']

    def test_command_when_date_open_survey_gt_now(self):
        survey = SurveyFactory(
            auto_control_publication_status=True,
            datetime_auto_open=timezone.now() + datetime.timedelta(days=1),
            is_published_external=False,
        )

        check_surveys_publication_status.delay()

        survey.refresh_from_db()
        self.assertFalse(survey.is_published_external)
        self.assertTrue(survey.auto_control_publication_status)

    def test_command_when_date_open_survey_lte_now_without_auto_control(self):
        survey = SurveyFactory(
            auto_control_publication_status=False,
            datetime_auto_open=timezone.now() - datetime.timedelta(days=1),
            is_published_external=False,
        )
        self.assertEqual(HistoryRawEntry.objects.count(), 0)

        check_surveys_publication_status.delay()

        survey.refresh_from_db()
        self.assertFalse(survey.is_published_external)
        self.assertFalse(survey.auto_control_publication_status)
        self.assertIsNotNone(survey.datetime_auto_open)
        self.assertEqual(HistoryRawEntry.objects.count(), 0)

    def test_command_when_date_open_survey_lte_now(self):
        survey = SurveyFactory(
            auto_control_publication_status=True,
            datetime_auto_open=timezone.now() - datetime.timedelta(days=1),
            is_published_external=False,
        )
        self.assertEqual(HistoryRawEntry.objects.count(), 0)

        check_surveys_publication_status.delay()

        survey.refresh_from_db()
        self.assertTrue(survey.is_published_external)
        self.assertFalse(survey.auto_control_publication_status)
        self.assertIsNotNone(survey.datetime_auto_open)
        self.assertEqual(HistoryRawEntry.objects.count(), 1)

    def test_command_when_date_close_survey_gt_now(self):
        survey = SurveyFactory(
            auto_control_publication_status=True,
            datetime_auto_close=timezone.now() + datetime.timedelta(days=1),
            is_published_external=True,
        )
        self.assertEqual(HistoryRawEntry.objects.count(), 0)

        check_surveys_publication_status.delay()

        survey.refresh_from_db()
        self.assertTrue(survey.is_published_external)
        self.assertTrue(survey.auto_control_publication_status)
        self.assertEqual(HistoryRawEntry.objects.count(), 0)

    def test_command_when_date_close_survey_lte_now_without_auto_control(self):
        survey = SurveyFactory(
            auto_control_publication_status=False,
            datetime_auto_close=timezone.now() + datetime.timedelta(days=1),
            is_published_external=True,
        )
        self.assertEqual(HistoryRawEntry.objects.count(), 0)

        check_surveys_publication_status.delay()

        survey.refresh_from_db()
        self.assertTrue(survey.is_published_external)
        self.assertFalse(survey.auto_control_publication_status)
        self.assertEqual(HistoryRawEntry.objects.count(), 0)

    def test_command_when_date_close_survey_lte_now(self):
        survey = SurveyFactory(
            auto_control_publication_status=True,
            datetime_auto_close=timezone.now() - datetime.timedelta(days=1),
            is_published_external=True,
        )
        self.assertEqual(HistoryRawEntry.objects.count(), 0)

        check_surveys_publication_status.delay()

        survey.refresh_from_db()
        self.assertFalse(survey.is_published_external)
        self.assertFalse(survey.auto_control_publication_status)
        self.assertIsNotNone(survey.datetime_auto_close)
        self.assertEqual(HistoryRawEntry.objects.count(), 1)

    def test_command_when_date_close_survey_is_none(self):
        survey1 = SurveyFactory(
            auto_control_publication_status=True,
            datetime_auto_open=None,
            is_published_external=True,
        )
        survey2 = SurveyFactory(is_published_external=False)
        self.assertEqual(HistoryRawEntry.objects.count(), 0)

        check_surveys_publication_status.delay()

        survey1.refresh_from_db()
        survey2.refresh_from_db()

        self.assertTrue(survey1.is_published_external)
        self.assertFalse(survey1.auto_control_publication_status)
        self.assertFalse(survey2.is_published_external)
        self.assertFalse(survey2.auto_control_publication_status)
        self.assertEqual(HistoryRawEntry.objects.count(), 0)

    def test_command_when_date_open_and_date_close_survey_lte_now(self):
        survey = SurveyFactory(
            auto_control_publication_status=True,
            datetime_auto_open=timezone.now() - datetime.timedelta(days=1),
            datetime_auto_close=timezone.now() - datetime.timedelta(hours=1),
            is_published_external=False,
        )
        self.assertEqual(HistoryRawEntry.objects.count(), 0)

        check_surveys_publication_status.delay()

        survey.refresh_from_db()
        self.assertFalse(survey.is_published_external)
        self.assertFalse(survey.auto_control_publication_status)
        self.assertIsNotNone(survey.datetime_auto_open)
        self.assertIsNotNone(survey.datetime_auto_close)
        self.assertEqual(HistoryRawEntry.objects.count(), 0)

    @override_settings(IS_BUSINESS_SITE=True)
    def test_shouldnt_publish_survey_for_blocked_user(self):
        user = UserFactory()
        survey = SurveyFactory(
            user=user,
            auto_control_publication_status=True,
            datetime_auto_open=timezone.now() - datetime.timedelta(days=1),
            is_published_external=False,
        )
        self.assertEqual(HistoryRawEntry.objects.count(), 0)

        command = SurveyAutoPublicationCommand()
        with patch.object(JsonBlackbox, 'userinfo') as mock_userinfo:
            mock_userinfo.return_value = {'users': [{'id': user.uid, 'karma': {'value': 85}}]}
            command.execute()

        survey.refresh_from_db()
        self.assertFalse(survey.is_published_external)
        self.assertFalse(survey.auto_control_publication_status)
        self.assertIsNotNone(survey.datetime_auto_open)
        self.assertEqual(HistoryRawEntry.objects.count(), 0)
        mock_userinfo.assert_called_once_with(uid=user.uid, userip=ANY)

        another_survey = SurveyFactory(
            user=user,
            auto_control_publication_status=True,
            datetime_auto_open=timezone.now() - datetime.timedelta(days=1),
            is_published_external=False,
        )
        self.assertEqual(HistoryRawEntry.objects.count(), 0)

        with patch.object(JsonBlackbox, 'userinfo') as mock_userinfo:
            mock_userinfo.return_value = None
            command.execute()

        another_survey.refresh_from_db()
        self.assertFalse(another_survey.is_published_external)
        self.assertFalse(another_survey.auto_control_publication_status)
        self.assertIsNotNone(another_survey.datetime_auto_open)
        self.assertEqual(HistoryRawEntry.objects.count(), 0)
        mock_userinfo.assert_not_called()

    @override_settings(IS_BUSINESS_SITE=True)
    def test_should_remove_auto_publish_for_blocked_user_with_range(self):
        user = UserFactory()
        survey = SurveyFactory(
            user=user,
            auto_control_publication_status=True,
            datetime_auto_open=timezone.now() - datetime.timedelta(days=1),
            datetime_auto_close=timezone.now() + datetime.timedelta(days=1),
            is_published_external=False,
        )
        self.assertEqual(HistoryRawEntry.objects.count(), 0)

        command = SurveyAutoPublicationCommand()
        with patch.object(JsonBlackbox, 'userinfo') as mock_userinfo:
            mock_userinfo.return_value = {'users': [{'id': user.uid, 'karma': {'value': 85}}]}
            command.execute()

        survey.refresh_from_db()
        self.assertFalse(survey.is_published_external)
        self.assertFalse(survey.auto_control_publication_status)
        self.assertIsNotNone(survey.datetime_auto_open)
        self.assertIsNotNone(survey.datetime_auto_close)
        self.assertEqual(HistoryRawEntry.objects.count(), 0)
        mock_userinfo.assert_called_once_with(uid=user.uid, userip=ANY)

    @override_settings(IS_BUSINESS_SITE=True)
    def test_shouldnt_remove_auto_publish_for_non_blocked_user_with_range(self):
        user = UserFactory()
        survey = SurveyFactory(
            user=user,
            auto_control_publication_status=True,
            datetime_auto_open=timezone.now() - datetime.timedelta(days=1),
            datetime_auto_close=timezone.now() + datetime.timedelta(days=1),
            is_published_external=False,
        )
        self.assertEqual(HistoryRawEntry.objects.count(), 0)

        command = SurveyAutoPublicationCommand()
        with patch.object(JsonBlackbox, 'userinfo') as mock_userinfo:
            mock_userinfo.return_value = {'users': [{'id': user.uid, 'karma': {'value': 0}}]}
            command.execute()

        survey.refresh_from_db()
        self.assertTrue(survey.is_published_external)
        self.assertTrue(survey.auto_control_publication_status)
        self.assertIsNotNone(survey.datetime_auto_open)
        self.assertIsNotNone(survey.datetime_auto_close)
        self.assertEqual(HistoryRawEntry.objects.count(), 1)
        mock_userinfo.assert_called_once_with(uid=user.uid, userip=ANY)

    @override_settings(IS_BUSINESS_SITE=True)
    def test_shouldnt_remove_auto_publish_for_non_blocked_user_with_range_and_cloud_uid(self):
        user = UserFactory(uid=None, cloud_uid=str(ObjectId()))
        survey = SurveyFactory(
            user=user,
            auto_control_publication_status=True,
            datetime_auto_open=timezone.now() - datetime.timedelta(days=1),
            datetime_auto_close=timezone.now() + datetime.timedelta(days=1),
            is_published_external=False,
        )
        self.assertEqual(HistoryRawEntry.objects.count(), 0)

        command = SurveyAutoPublicationCommand()
        with patch.object(JsonBlackbox, 'userinfo') as mock_userinfo:
            command.execute()

        survey.refresh_from_db()
        self.assertTrue(survey.is_published_external)
        self.assertTrue(survey.auto_control_publication_status)
        self.assertIsNotNone(survey.datetime_auto_open)
        self.assertIsNotNone(survey.datetime_auto_close)
        self.assertEqual(HistoryRawEntry.objects.count(), 1)
        mock_userinfo.assert_not_called()

    @override_settings(IS_BUSINESS_SITE=True)
    def test_should_publish_survey_for_non_blocked_user_with_cloud_uid(self):
        user = UserFactory(uid=None, cloud_uid=str(ObjectId()))
        survey = SurveyFactory(
            user=user,
            auto_control_publication_status=True,
            datetime_auto_open=timezone.now() - datetime.timedelta(days=1),
            is_published_external=False,
        )
        self.assertEqual(HistoryRawEntry.objects.count(), 0)

        command = SurveyAutoPublicationCommand()
        with patch.object(JsonBlackbox, 'userinfo') as mock_userinfo:
            command.execute()

        survey.refresh_from_db()
        self.assertTrue(survey.is_published_external)
        self.assertFalse(survey.auto_control_publication_status)
        self.assertIsNotNone(survey.datetime_auto_open)
        self.assertEqual(HistoryRawEntry.objects.count(), 1)
        mock_userinfo.assert_not_called()

        another_survey = SurveyFactory(
            user=user,
            auto_control_publication_status=True,
            datetime_auto_open=timezone.now() - datetime.timedelta(days=1),
            is_published_external=False,
        )
        self.assertEqual(HistoryRawEntry.objects.count(), 1)

        with patch.object(JsonBlackbox, 'userinfo') as mock_userinfo:
            mock_userinfo.return_value = None
            command.execute()

        another_survey.refresh_from_db()
        self.assertTrue(another_survey.is_published_external)
        self.assertFalse(another_survey.auto_control_publication_status)
        self.assertIsNotNone(another_survey.datetime_auto_open)
        self.assertEqual(HistoryRawEntry.objects.count(), 2)
        mock_userinfo.assert_not_called()

    @override_settings(IS_BUSINESS_SITE=True)
    def test_should_publish_survey_for_non_blocked_user(self):
        user = UserFactory()
        survey = SurveyFactory(
            user=user,
            auto_control_publication_status=True,
            datetime_auto_open=timezone.now() - datetime.timedelta(days=1),
            is_published_external=False,
        )
        self.assertEqual(HistoryRawEntry.objects.count(), 0)

        command = SurveyAutoPublicationCommand()
        with patch.object(JsonBlackbox, 'userinfo') as mock_userinfo:
            mock_userinfo.return_value = {'users': [{'id': user.uid, 'karma': {'value': 0}}]}
            command.execute()

        survey.refresh_from_db()
        self.assertTrue(survey.is_published_external)
        self.assertFalse(survey.auto_control_publication_status)
        self.assertIsNotNone(survey.datetime_auto_open)
        self.assertEqual(HistoryRawEntry.objects.count(), 1)
        mock_userinfo.assert_called_once_with(uid=user.uid, userip=ANY)

        another_survey = SurveyFactory(
            user=user,
            auto_control_publication_status=True,
            datetime_auto_open=timezone.now() - datetime.timedelta(days=1),
            is_published_external=False,
        )
        self.assertEqual(HistoryRawEntry.objects.count(), 1)

        with patch.object(JsonBlackbox, 'userinfo') as mock_userinfo:
            mock_userinfo.return_value = None
            command.execute()

        another_survey.refresh_from_db()
        self.assertTrue(another_survey.is_published_external)
        self.assertFalse(another_survey.auto_control_publication_status)
        self.assertIsNotNone(another_survey.datetime_auto_open)
        self.assertEqual(HistoryRawEntry.objects.count(), 2)
        mock_userinfo.assert_not_called()

    @override_settings(IS_BUSINESS_SITE=True)
    def test_should_unpublish_survey_for_blocked_user(self):
        user = UserFactory()
        survey = SurveyFactory(
            user=user,
            auto_control_publication_status=True,
            datetime_auto_close=timezone.now() - datetime.timedelta(days=1),
            is_published_external=True,
        )
        self.assertEqual(HistoryRawEntry.objects.count(), 0)

        command = SurveyAutoPublicationCommand()
        with patch.object(JsonBlackbox, 'userinfo') as mock_userinfo:
            mock_userinfo.return_value = {'users': [{'id': user.uid, 'karma': {'value': 85}}]}
            command.execute()

        survey.refresh_from_db()
        self.assertFalse(survey.is_published_external)
        self.assertFalse(survey.auto_control_publication_status)
        self.assertIsNotNone(survey.datetime_auto_close)
        self.assertEqual(HistoryRawEntry.objects.count(), 1)
        mock_userinfo.assert_called_once_with(uid=user.uid, userip=ANY)

        another_survey = SurveyFactory(
            user=user,
            auto_control_publication_status=True,
            datetime_auto_close=timezone.now() - datetime.timedelta(days=1),
            is_published_external=True,
        )
        self.assertEqual(HistoryRawEntry.objects.count(), 1)

        with patch.object(JsonBlackbox, 'userinfo') as mock_userinfo:
            mock_userinfo.return_value = None
            command.execute()

        another_survey.refresh_from_db()
        self.assertFalse(another_survey.is_published_external)
        self.assertFalse(another_survey.auto_control_publication_status)
        self.assertIsNotNone(another_survey.datetime_auto_close)
        self.assertEqual(HistoryRawEntry.objects.count(), 2)
        mock_userinfo.assert_not_called()

    @override_settings(IS_BUSINESS_SITE=True)
    def test_should_unpublish_survey_for_non_blocked_user(self):
        user = UserFactory()
        survey = SurveyFactory(
            user=user,
            auto_control_publication_status=True,
            datetime_auto_close=timezone.now() - datetime.timedelta(days=1),
            is_published_external=True,
        )
        self.assertEqual(HistoryRawEntry.objects.count(), 0)

        command = SurveyAutoPublicationCommand()
        with patch.object(JsonBlackbox, 'userinfo') as mock_userinfo:
            mock_userinfo.return_value = {'users': [{'id': user.uid, 'karma': {'value': 0}}]}
            command.execute()

        survey.refresh_from_db()
        self.assertFalse(survey.is_published_external)
        self.assertFalse(survey.auto_control_publication_status)
        self.assertIsNotNone(survey.datetime_auto_close)
        self.assertEqual(HistoryRawEntry.objects.count(), 1)
        mock_userinfo.assert_called_once_with(uid=user.uid, userip=ANY)

        another_survey = SurveyFactory(
            user=user,
            auto_control_publication_status=True,
            datetime_auto_close=timezone.now() - datetime.timedelta(days=1),
            is_published_external=True,
        )
        self.assertEqual(HistoryRawEntry.objects.count(), 1)

        with patch.object(JsonBlackbox, 'userinfo') as mock_userinfo:
            mock_userinfo.return_value = None
            command.execute()

        another_survey.refresh_from_db()
        self.assertFalse(another_survey.is_published_external)
        self.assertFalse(another_survey.auto_control_publication_status)
        self.assertIsNotNone(another_survey.datetime_auto_close)
        self.assertEqual(HistoryRawEntry.objects.count(), 2)
        mock_userinfo.assert_not_called()

    def test_command_when_set_date_range_survey(self):
        survey = SurveyFactory(
            auto_control_publication_status=True,
            datetime_auto_open=timezone.now() - datetime.timedelta(hours=1),
            datetime_auto_close=timezone.now() + datetime.timedelta(hours=1),
            is_published_external=False,
        )
        self.assertEqual(HistoryRawEntry.objects.count(), 0)

        check_surveys_publication_status.delay()

        survey.refresh_from_db()
        self.assertTrue(survey.is_published_external)
        self.assertTrue(survey.auto_control_publication_status)
        self.assertIsNotNone(survey.datetime_auto_open)
        self.assertIsNotNone(survey.datetime_auto_close)
        self.assertEqual(HistoryRawEntry.objects.count(), 1)

        survey.datetime_auto_close = timezone.now() - datetime.timedelta(minutes=1)
        survey.save()
        self.assertEqual(HistoryRawEntry.objects.count(), 1)

        check_surveys_publication_status.delay()

        survey.refresh_from_db()
        self.assertFalse(survey.is_published_external)
        self.assertFalse(survey.auto_control_publication_status)
        self.assertIsNotNone(survey.datetime_auto_open)
        self.assertIsNotNone(survey.datetime_auto_close)
        self.assertEqual(HistoryRawEntry.objects.count(), 2)

    def test_command_when_set_date_range_survey_close_gte(self):
        survey = SurveyFactory(
            auto_control_publication_status=True,
            datetime_auto_open=timezone.now() - datetime.timedelta(hours=1),
            datetime_auto_close=timezone.now() + datetime.timedelta(hours=1),
            is_published_external=True,
        )
        self.assertEqual(HistoryRawEntry.objects.count(), 0)

        check_surveys_publication_status.delay()

        survey.refresh_from_db()
        self.assertTrue(survey.is_published_external)
        self.assertTrue(survey.auto_control_publication_status)
        self.assertIsNotNone(survey.datetime_auto_open)
        self.assertIsNotNone(survey.datetime_auto_close)
        self.assertEqual(HistoryRawEntry.objects.count(), 0)
