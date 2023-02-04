# -*- coding: utf-8 -*-
from django.test import TestCase

from events.accounts.factories import UserFactory
from events.surveyme.factories import ProfileSurveyAnswerFactory
from events.surveyme.models import ProfileSurveyAnswer
from events.takeout import utils
from events.takeout.factories import TakeoutOperationFactory
from events.takeout.models import TakeoutOperation


class TestTakeoutDeleteAnswers(TestCase):
    fixtures = ['initial_data.json']

    def test_relink_user_answers(self):
        user = UserFactory()
        new_user = UserFactory()
        for _ in range(10):
            ProfileSurveyAnswerFactory(user=user)
        utils.relink_user_answers(user.pk, new_user.pk, max_update_size=3)

        user_answers = ProfileSurveyAnswer.objects.filter(user=user).count()
        new_user_answers = ProfileSurveyAnswer.objects.filter(user=new_user).count()
        self.assertEqual(user_answers, 0)
        self.assertEqual(new_user_answers, 10)

    def test_change_operation_status(self):
        user = UserFactory()
        op = TakeoutOperationFactory(user=user)

        utils.change_operation_status(op.task_id, status=TakeoutOperation.SUCCESS)
        op.refresh_from_db()
        self.assertEqual(op.status, TakeoutOperation.SUCCESS)
        self.assertIsNone(op.error)

        utils.change_operation_status(op.task_id, status=TakeoutOperation.ERROR, error='ValueError')
        op.refresh_from_db()
        self.assertEqual(op.status, TakeoutOperation.ERROR)
        self.assertEqual(op.error, 'ValueError')

    def test__get_last_operation(self):
        user = UserFactory()
        tda = utils.TakeoutDeleteAnswers(user)
        self.assertIsNone(tda._get_last_operation())

        operations = [
            TakeoutOperationFactory(user=user)
            for _ in range(10)
        ]
        self.assertEqual(tda._get_last_operation(), operations[-1])

    def test__check_if_answers_exist(self):
        user = UserFactory()
        tda = utils.TakeoutDeleteAnswers(user)
        self.assertFalse(tda._check_if_answers_exist())

        ProfileSurveyAnswerFactory(user=user)
        self.assertTrue(tda._check_if_answers_exist())

    def test_check__error(self):
        user = UserFactory()
        tda = utils.TakeoutDeleteAnswers(user)
        TakeoutOperationFactory(user=user, status=TakeoutOperation.ERROR, error='ValueError')
        expected = {
            'status': 'error',
            'errors': [{'code': 'internal', 'message': 'ValueError'}],
        }
        self.assertEqual(tda.check(), expected)

    def test_check__nothing_to_do(self):
        user = UserFactory()
        tda = utils.TakeoutDeleteAnswers(user)
        expected = {
            'status': 'ok',
            'data': [{'id': '1', 'slug': 'answers', 'state': 'empty'}],
        }
        self.assertEqual(tda.check(), expected)

    def test_check__empty(self):
        user = UserFactory()
        tda = utils.TakeoutDeleteAnswers(user)
        op = TakeoutOperationFactory(user=user, status=TakeoutOperation.SUCCESS)
        expected = {
            'status': 'ok',
            'data': [{
                'id': '1', 'slug': 'answers', 'state': 'empty',
                'update_date': op.created.strftime('%Y-%m-%dT%H:%M:%SZ'),
            }],
        }
        self.assertEqual(tda.check(), expected)

    def test_check__ready_to_delete(self):
        user = UserFactory()
        tda = utils.TakeoutDeleteAnswers(user)
        op = TakeoutOperationFactory(user=user, status=TakeoutOperation.SUCCESS)
        ProfileSurveyAnswerFactory(user=user)
        expected = {
            'status': 'ok',
            'data': [{
                'id': '1', 'slug': 'answers', 'state': 'ready_to_delete',
                'update_date': op.created.strftime('%Y-%m-%dT%H:%M:%SZ'),
            }],
        }
        self.assertEqual(tda.check(), expected)

    def test_check__delete_in_progress(self):
        user = UserFactory()
        tda = utils.TakeoutDeleteAnswers(user)
        TakeoutOperationFactory(user=user, status=TakeoutOperation.INPROGRESS)
        ProfileSurveyAnswerFactory(user=user)
        expected = {
            'status': 'ok',
            'data': [{'id': '1', 'slug': 'answers', 'state': 'delete_in_progress'}],
        }
        self.assertEqual(tda.check(), expected)

    def test_delete(self):
        user = UserFactory()
        tda = utils.TakeoutDeleteAnswers(user)
        for _ in range(10):
            ProfileSurveyAnswerFactory(user=user)
        expected = {'status': 'ok'}
        self.assertEqual(tda.delete(), expected)

        user_answers = ProfileSurveyAnswer.objects.filter(user=user).count()
        self.assertEqual(user_answers, 0)
