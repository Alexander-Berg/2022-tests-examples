# -*- coding: utf-8 -*-
import uuid

from django.test import TestCase
from unittest.mock import patch

from events.accounts.factories import UserFactory
from events.surveyme.factories import ProfileSurveyAnswerFactory
from events.takeout import tasks
from events.takeout.models import TakeoutOperation


class TestTakeoutTasks(TestCase):
    fixtures = ['initial_data.json']

    def test_relink_user_answers__success(self):
        user = UserFactory()
        new_user = UserFactory()
        for _ in range(10):
            ProfileSurveyAnswerFactory(user=user)

        task_id = str(uuid.uuid4())
        args = (user.pk, new_user.pk)
        kwargs = {'task_id': task_id}
        task = tasks.relink_user_answers.apply_async(args, kwargs, task_id=task_id)

        op = TakeoutOperation.objects.get(task_id=task.task_id)
        self.assertEqual(op.status, TakeoutOperation.SUCCESS)

    def test_relink_user_answers__error(self):
        user = UserFactory()
        new_user = UserFactory()
        for _ in range(10):
            ProfileSurveyAnswerFactory(user=user)

        task_id = str(uuid.uuid4())
        args = (user.pk, new_user.pk)
        kwargs = {'task_id': task_id}
        with patch('events.takeout.utils.relink_user_answers') as mock_relink:
            mock_relink.side_effect = ValueError('internal error')
            task = tasks.relink_user_answers.apply_async(args, kwargs, task_id=task_id)

        mock_relink.assert_called_once_with(user.pk, new_user.pk)

        op = TakeoutOperation.objects.get(task_id=task.task_id)
        self.assertEqual(op.status, TakeoutOperation.ERROR)
        self.assertEqual(op.error, 'internal error')
