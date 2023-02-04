# -*- coding: utf-8 -*-
from django.test import TestCase

from events.surveyme.factories import ProfileSurveyAnswerFactory
from events.surveyme_integration.variables.quiz import (
    QuizTotalScores,
    QuizQuestionCount,
    QuizAnswerScores,
    QuizResultTitle,
    QuizResultDescription,
    QuizQuestionScores,
)


class TestQuizTotalScores(TestCase):
    def setUp(self):
        self.answer = ProfileSurveyAnswerFactory()

    def test_should_return_total_scores(self):
        self.answer.data = {
            'quiz': {
                'total_scores': 42,
            },
        }
        self.answer.save()
        var = QuizTotalScores(answer=self.answer)
        self.assertEqual(var.get_value(), 42)

    def test_should_resturn_none_when_quiz_is_empty(self):
        self.answer.data = {
            'quiz': {},
        }
        self.answer.save()
        var = QuizTotalScores(answer=self.answer)
        self.assertIsNone(var.get_value())

    def test_should_resturn_none_when_quiz_is_none(self):
        var = QuizTotalScores(answer=self.answer)
        self.assertIsNone(var.get_value())


class TestQuizQuestionCount(TestCase):
    def setUp(self):
        self.answer = ProfileSurveyAnswerFactory()

    def test_should_return_question_count(self):
        self.answer.data = {
            'quiz': {
                'question_count': 7,
            },
        }
        self.answer.save()
        var = QuizQuestionCount(answer=self.answer)
        self.assertEqual(var.get_value(), 7)

    def test_should_resturn_none_when_quiz_is_empty(self):
        self.answer.data = {
            'quiz': {},
        }
        self.answer.save()
        var = QuizQuestionCount(answer=self.answer)
        self.assertIsNone(var.get_value())

    def test_should_resturn_none_when_quiz_is_none(self):
        var = QuizQuestionCount(answer=self.answer)
        self.assertIsNone(var.get_value())


class TestQuizAnswerScores(TestCase):
    def setUp(self):
        self.answer = ProfileSurveyAnswerFactory()

    def test_should_return_scores(self):
        self.answer.data = {
            'quiz': {
                'scores': 37,
            },
        }
        self.answer.save()
        var = QuizAnswerScores(answer=self.answer)
        self.assertEqual(var.get_value(), 37)

    def test_should_resturn_none_when_quiz_is_empty(self):
        self.answer.data = {
            'quiz': {},
        }
        self.answer.save()
        var = QuizAnswerScores(answer=self.answer)
        self.assertIsNone(var.get_value())

    def test_should_resturn_none_when_quiz_is_none(self):
        var = QuizAnswerScores(answer=self.answer)
        self.assertIsNone(var.get_value())


class TestQuizResultTitle(TestCase):
    def setUp(self):
        self.answer = ProfileSurveyAnswerFactory()

    def test_should_return_result_title(self):
        self.answer.data = {
            'quiz': {
                'title': 'You are winner',
            },
        }
        self.answer.save()
        var = QuizResultTitle(answer=self.answer)
        self.assertEqual(var.get_value(), 'You are winner')

    def test_should_resturn_none_when_quiz_is_empty(self):
        self.answer.data = {
            'quiz': {},
        }
        self.answer.save()
        var = QuizResultTitle(answer=self.answer)
        self.assertIsNone(var.get_value())

    def test_should_resturn_none_when_quiz_is_none(self):
        var = QuizResultTitle(answer=self.answer)
        self.assertIsNone(var.get_value())


class TestQuizResultDescription(TestCase):
    def setUp(self):
        self.answer = ProfileSurveyAnswerFactory()

    def test_should_return_result_description(self):
        self.answer.data = {
            'quiz': {
                'description': 'You are successed in a competition',
            },
        }
        self.answer.save()
        var = QuizResultDescription(answer=self.answer)
        self.assertEqual(var.get_value(), 'You are successed in a competition')

    def test_should_resturn_none_when_quiz_is_empty(self):
        self.answer.data = {
            'quiz': {},
        }
        self.answer.save()
        var = QuizResultDescription(answer=self.answer)
        self.assertIsNone(var.get_value())

    def test_should_resturn_none_when_quiz_is_none(self):
        var = QuizResultDescription(answer=self.answer)
        self.assertIsNone(var.get_value())


class TestQuizQuestionScores(TestCase):
    def setUp(self):
        self.answer = ProfileSurveyAnswerFactory()

    def test_should_return_question_scores(self):
        self.answer.data = {
            'data': [
                {
                    'question': {
                        'id': 112233,
                    },
                    'scores': 17,
                },
            ],
        }
        self.answer.save()
        var = QuizQuestionScores(answer=self.answer, question=112233)
        self.assertEqual(var.get_value(), 17)

    def test_should_return_nonde_when_scores_doesnt_exist(self):
        self.answer.data = {
            'data': [
                {
                    'question': {
                        'id': 112233,
                    },
                },
            ],
        }
        self.answer.save()
        var = QuizQuestionScores(answer=self.answer, question=112233)
        self.assertIsNone(var.get_value())

    def test_should_return_none_when_question_doesnt_exist(self):
        self.answer.data = {
            'data': [
                {
                    'question': {
                        'id': 112233,
                    },
                    'scores': 17,
                },
            ],
        }
        self.answer.save()
        var = QuizQuestionScores(answer=self.answer, question=332211)
        self.assertIsNone(var.get_value())

    def test_should_return_none_when_data_is_empty(self):
        self.answer.data = {
            'data': [],
        }
        self.answer.save()
        var = QuizQuestionScores(answer=self.answer, question=332211)
        self.assertIsNone(var.get_value())

    def test_shouldnt_return_none_when_data_is_none(self):
        self.answer.data = {
            'data': None,
        }
        self.answer.save()
        var = QuizQuestionScores(answer=self.answer, question=332211)
        self.assertIsNone(var.get_value())
