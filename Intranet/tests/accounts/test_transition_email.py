# -*- coding: utf-8 -*-
from django.test import TestCase
from unittest.mock import patch, ANY

from events.accounts.factories import UserFactory, GrantTransitionInfoFactory
from events.accounts.models import GrantTransitionInfo
from events.accounts.tasks import (
    get_transition_objects,
    make_transition_emails,
    send_transition_email,
)
from events.accounts.utils import TransitionEmail
from events.common_app.helpers import MockResponse
from events.common_app.sender.client import SenderClient
from events.surveyme.factories import SurveyFactory, SurveyGroupFactory


class TestMakeTransitionEmails(TestCase):
    def test_should_make_email(self):
        user1 = UserFactory()
        GrantTransitionInfoFactory(
            new_user=user1, old_user=UserFactory(),
            content_object=SurveyFactory(),
        )
        GrantTransitionInfoFactory(
            new_user=user1, old_user=UserFactory(),
            content_object=SurveyGroupFactory(),
        )
        user2 = UserFactory()
        GrantTransitionInfoFactory(
            new_user=user2, old_user=UserFactory(),
            content_object=SurveyFactory(),
        )
        with patch.object(send_transition_email, 'delay') as mock_send_email:
            make_transition_emails.delay()

        self.assertEqual(mock_send_email.call_count, 2)
        result = {}

        args, kwargs = mock_send_email.call_args_list[0]
        new_user, objects = args
        result[new_user] = objects

        args, kwargs = mock_send_email.call_args_list[1]
        new_user, objects = args
        result[new_user] = objects

        self.assertSetEqual({user1, user2}, set(result.keys()))
        self.assertEqual(GrantTransitionInfo.objects.count(), 0)

    def test_shouldnt_make_email(self):
        with patch.object(send_transition_email, 'delay') as mock_send_email:
            make_transition_emails.delay()
        mock_send_email.assert_not_called()

    def test_should_return_transition_objects(self):
        new_user = UserFactory()
        old_user = UserFactory()
        another_user = UserFactory()

        expected = {old_user: {}, another_user: {}}
        it = GrantTransitionInfoFactory(new_user=new_user, old_user=old_user, content_object=SurveyFactory())
        expected[old_user][it.content_type] = [str(it.object_pk)]

        it = GrantTransitionInfoFactory(new_user=new_user, old_user=old_user, content_object=SurveyGroupFactory())
        expected[old_user][it.content_type] = [str(it.object_pk)]

        it = GrantTransitionInfoFactory(new_user=new_user, old_user=old_user, content_object=SurveyFactory())
        expected[old_user][it.content_type].append(str(it.object_pk))

        it = GrantTransitionInfoFactory(new_user=new_user, old_user=another_user, content_object=SurveyFactory())
        expected[another_user][it.content_type] = [str(it.object_pk)]

        queryset = (
            GrantTransitionInfo.objects.filter(new_user=new_user)
            .order_by('old_user', 'content_type__model', 'object_pk')
        )

        result = get_transition_objects(queryset)
        self.assertEqual(len(result), 2)
        self.assertDictEqual(result, expected)

    def test_shouldnt_return_transition_objects(self):
        new_user = UserFactory()
        queryset = GrantTransitionInfo.objects.filter(new_user=new_user)

        result = get_transition_objects(queryset)
        self.assertEqual(len(result), 0)

    def test_should_send_transition_email(self):
        new_user = UserFactory()
        old_user = UserFactory()
        it = GrantTransitionInfoFactory(new_user=new_user, old_user=old_user, content_object=SurveyFactory())
        objects = {old_user: {it.content_type: [str(it.object_pk)]}}
        with patch.object(TransitionEmail, 'send_email') as mock_send_email:
            mock_send_email.return_value = MockResponse({'task_id': 'abcd'})
            task = send_transition_email.delay(new_user, objects)
        self.assertEqual(task.result, (200, '{"task_id": "abcd"}', 'application/json'))
        mock_send_email.assert_called_once_with(new_user, objects)

    def test_shouldnt_send_transition_email(self):
        new_user = UserFactory()
        objects = {}
        with patch.object(TransitionEmail, 'send_email') as mock_send_email:
            mock_send_email.return_value = None
            task = send_transition_email.delay(new_user, objects)
        self.assertIsNone(task.result)
        mock_send_email.assert_called_once_with(new_user, objects)


class TestTransitionEmail(TestCase):
    def test_should_send_transition_email(self):
        new_user = UserFactory()
        old_user = UserFactory()
        it = GrantTransitionInfoFactory(new_user=new_user, old_user=old_user, content_object=SurveyFactory())
        objects = {old_user: {it.content_type: [str(it.object_pk)]}}
        with patch.object(SenderClient, 'send_email') as mock_send_email:
            TransitionEmail().send_email(new_user, objects)
        mock_send_email.assert_called_once_with('internal', ANY)

    def test_shouldnt_send_email(self):
        new_user = UserFactory()
        objects = {}
        with patch.object(SenderClient, 'send_email') as mock_send_email:
            TransitionEmail().send_email(new_user, objects)
        mock_send_email.assert_not_called()
