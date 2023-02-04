# -*- coding: utf-8 -*-
import datetime

from django.db import connection
from django.test import TestCase
from django.utils import timezone
from unittest.mock import patch

from events.countme.factories import AnswerCountByDateFactory
from events.surveyme.drop_utils import (
    DropNotifications,
    DropDeletedAnswers,
    DropUnusedAnswers,
)
from events.surveyme.tasks import (
    drop_notifications,
    drop_deleted_answers,
    drop_unused_answers,
)
from events.surveyme.factories import SurveyFactory, ProfileSurveyAnswerFactory
from events.surveyme.models import ProfileSurveyAnswer
from events.surveyme_integration.factories import HookSubscriptionNotificationFactory
from events.surveyme_integration.models import HookSubscriptionNotification


class TestDropNotifications(TestCase):
    fixtures = ['initial_data.json']

    def test_invoke_task(self):
        with patch.object(DropNotifications, 'execute_once') as mock_execute_once:
            mock_execute_once.return_value = 0
            drop_notifications.delay()

        mock_execute_once.assert_called_once()

    def test_should_drop_old_notifications(self):
        survey = SurveyFactory()
        notifications = [
            HookSubscriptionNotificationFactory(survey=survey),
            HookSubscriptionNotificationFactory(survey=survey),
            HookSubscriptionNotificationFactory(survey=survey),
            HookSubscriptionNotificationFactory(survey=survey),
        ]

        notifications[0].date_created -= datetime.timedelta(days=10)
        notifications[0].save()

        notifications[1].date_created -= datetime.timedelta(days=9)
        notifications[1].save()

        notifications[2].date_created -= datetime.timedelta(days=7)
        notifications[2].save()

        today = datetime.date.today()
        op = DropNotifications(today - datetime.timedelta(days=7), 2)
        self.assertEqual(op.execute(), 2)
        self.assertEqual(HookSubscriptionNotification.objects.filter(survey=survey).count(), 2)


class TestDropDeletedAnswers(TestCase):
    fixtures = ['initial_data.json']

    def test_invoke_task(self):
        with patch.object(DropDeletedAnswers, 'execute') as mock_execute:
            mock_execute.return_value = 0
            drop_deleted_answers.delay()

        mock_execute.assert_called_once()

    def change_date_updated(self, survey, date_updated):
        with connection.cursor() as c:
            c.execute(
                'update surveyme_survey set date_updated = %s where id = %s',
                (date_updated, survey.pk)
            )

    def test_should_drop_deleted_answers(self):
        date_updated = timezone.now() - datetime.timedelta(days=9)

        deleted_survey = SurveyFactory(is_deleted=True)
        self.change_date_updated(deleted_survey, date_updated)

        survey = SurveyFactory()
        self.change_date_updated(survey, date_updated)

        answers = [
            ProfileSurveyAnswerFactory(survey=deleted_survey),
            ProfileSurveyAnswerFactory(survey=deleted_survey),
            ProfileSurveyAnswerFactory(survey=survey),
            ProfileSurveyAnswerFactory(survey=survey),
        ]
        for answer in answers:
            answer.date_created -= datetime.timedelta(days=10)
            answer.save()

        survey.answercount.count = 2
        survey.answercount.save()
        AnswerCountByDateFactory(survey=survey, count=2, created=survey.date_updated)

        deleted_survey.answercount.count = 2
        deleted_survey.answercount.save()
        AnswerCountByDateFactory(survey=deleted_survey, count=2, created=deleted_survey.date_updated)

        today = datetime.date.today()
        op = DropDeletedAnswers(today - datetime.timedelta(days=7), 2)
        self.assertEqual(op.execute(), 1)
        self.assertEqual(ProfileSurveyAnswer.objects.filter(survey=deleted_survey).count(), 0)
        self.assertEqual(ProfileSurveyAnswer.objects.filter(survey=survey).count(), 2)


class TestDropUnusedAnswers(TestCase):
    fixtures = ['initial_data.json']

    def test_invoke_task(self):
        with patch.object(DropUnusedAnswers, 'execute') as mock_execute:
            mock_execute.return_value = 0
            drop_unused_answers.delay()

        mock_execute.assert_called_once()

    def test_should_drop_unused_answers(self):
        date_created = timezone.now() - datetime.timedelta(days=10)

        survey = SurveyFactory(date_exported=timezone.now())
        unused_survey = SurveyFactory()

        answers = [
            ProfileSurveyAnswerFactory(survey=survey),
            ProfileSurveyAnswerFactory(survey=survey),
            ProfileSurveyAnswerFactory(survey=unused_survey),
            ProfileSurveyAnswerFactory(survey=unused_survey),
        ]
        for answer in answers:
            answer.date_created = date_created
            answer.save()

        survey.answercount.count = 2
        survey.answercount.save()
        AnswerCountByDateFactory(survey=survey, count=2, created=date_created)

        unused_survey.answercount.count = 2
        unused_survey.answercount.save()
        AnswerCountByDateFactory(survey=unused_survey, count=2, created=date_created)

        today = datetime.date.today()
        op = DropUnusedAnswers(today - datetime.timedelta(days=7), 2)
        self.assertEqual(op.execute(), 1)
        self.assertEqual(ProfileSurveyAnswer.objects.filter(survey=survey).count(), 2)
        self.assertEqual(ProfileSurveyAnswer.objects.filter(survey=unused_survey).count(), 0)
