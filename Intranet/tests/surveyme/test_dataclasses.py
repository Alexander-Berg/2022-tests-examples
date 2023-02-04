# -*- coding: utf-8 -*-
from django.test import TestCase
from events.surveyme.dataclasses import (
    ParamQuiz,
    AnswerQuestion,
    SurveyQuiz,
    SurveyQuizItem,
)
from events.media.factories import ImageFactory


class TestParamQuiz(TestCase):
    def test_correct_data(self):
        data = {
            'enabled': True,
            'answers': [
                {
                    'correct': True,
                    'scores': 3.14,
                    'value': 'pi',
                },
                {
                    'correct': True,
                    'scores': 2.71,
                    'value': 'e',
                },
                {
                    'correct': False,
                    'scores': 0,
                    'value': None,
                },
            ],
        }
        param_quiz = ParamQuiz(data)

        self.assertIsNotNone(param_quiz)
        self.assertTrue(param_quiz.enabled)
        self.assertEqual(len(param_quiz.answers), 3)
        self.assertTrue(param_quiz.answers[0].correct)
        self.assertEqual(param_quiz.answers[0].scores, 3.14)
        self.assertEqual(param_quiz.answers[0].value, 'pi')
        self.assertTrue(param_quiz.answers[1].correct)
        self.assertEqual(param_quiz.answers[1].scores, 2.71)
        self.assertEqual(param_quiz.answers[1].value, 'e')
        self.assertFalse(param_quiz.answers[2].correct)
        self.assertEqual(param_quiz.answers[2].scores, 0)
        self.assertIsNone(param_quiz.answers[2].value)

    def test_incorrect_data(self):
        data = {
            'enabled': True,
            'answers': [
                {
                    'correctness': True,
                    'numbers': 3.14,
                    'text': 'pi',
                },
            ],
        }
        param_quiz = ParamQuiz(data)

        self.assertIsNotNone(param_quiz)
        self.assertTrue(param_quiz.enabled)
        self.assertEqual(len(param_quiz.answers), 1)
        self.assertFalse(param_quiz.answers[0].correct)
        self.assertIsNone(param_quiz.answers[0].scores)
        self.assertIsNone(param_quiz.answers[0].value)

    def test_empty_data(self):
        data = {}
        param_quiz = ParamQuiz(data)

        self.assertIsNotNone(param_quiz)
        self.assertFalse(param_quiz.enabled)
        self.assertEqual(len(param_quiz.answers), 0)

    def test_none_data(self):
        data = None
        param_quiz = ParamQuiz(data)

        self.assertIsNotNone(param_quiz)
        self.assertFalse(param_quiz.enabled)
        self.assertEqual(len(param_quiz.answers), 0)


class TestAnswer(TestCase):
    def test_answer_short_text(self):
        data = {
            'id': 2,
            'slug': 'answer_short_text_2',
            'answer_type': {
                'id': 1,
                'slug': 'answer_short_text',
            },
            'value': 'testme',
        }

        answer_question = AnswerQuestion(data)
        self.assertIsNotNone(answer_question)
        self.assertEqual(answer_question.id, 2)
        self.assertEqual(answer_question.slug, 'answer_short_text_2')
        self.assertEqual(answer_question.answer_type.id, 1)
        self.assertEqual(answer_question.answer_type.slug, 'answer_short_text')
        self.assertIsNotNone(answer_question.options)

        value = answer_question.get_value()
        self.assertIsNotNone(value)
        self.assertEqual(value, 'testme')

    def test_answer_choices(self):
        data = {
            'id': 2,
            'slug': 'answer_choices_2',
            'answer_type': {
                'id': 3,
                'slug': 'answer_choices',
            },
            'options': {
                'data_source': 'survey_question_choice',
            },
            'value': [
                {'key': '100', 'slug': '100', 'text': 'one hundred'},
                {'key': '200', 'slug': '200', 'text': 'two hundreds'},
            ],
        }

        answer_question = AnswerQuestion(data)
        self.assertIsNotNone(answer_question)
        self.assertEqual(answer_question.id, 2)
        self.assertEqual(answer_question.slug, 'answer_choices_2')
        self.assertEqual(answer_question.answer_type.id, 3)
        self.assertEqual(answer_question.answer_type.slug, 'answer_choices')
        self.assertIsNotNone(answer_question.options)
        self.assertEqual(answer_question.options.data_source, 'survey_question_choice')

        value = answer_question.get_value()
        self.assertIsNotNone(value)
        self.assertEqual(len(value), 2)
        self.assertEqual(value[0].key, '100')
        self.assertEqual(value[0].slug, '100')
        self.assertEqual(value[0].text, 'one hundred')
        self.assertEqual(value[1].key, '200')
        self.assertEqual(value[1].slug, '200')
        self.assertEqual(value[1].text, 'two hundreds')

    def test_answer_number(self):
        data = {
            'id': 2,
            'slug': 'answer_number_2',
            'answer_type': {
                'id': 1,
                'slug': 'answer_number',
            },
            'value': 42,
        }

        answer_question = AnswerQuestion(data)
        self.assertIsNotNone(answer_question)
        self.assertEqual(answer_question.id, 2)
        self.assertEqual(answer_question.slug, 'answer_number_2')
        self.assertEqual(answer_question.answer_type.id, 1)
        self.assertEqual(answer_question.answer_type.slug, 'answer_number')
        self.assertIsNotNone(answer_question.options)

        value = answer_question.get_value()
        self.assertIsNotNone(value)
        self.assertEqual(value, 42)


class TestSurveyQuizItem(TestCase):
    fixtures = ['initial_data.json']

    def test_item_with_image(self):
        image = ImageFactory()
        data = {
            'title': 'title1',
            'description': 'description1',
            'image_id': image.pk,
        }
        item = SurveyQuizItem(data)
        self.assertEqual(item.title, 'title1')
        self.assertEqual(item.description, 'description1')
        self.assertEqual(item.image_id, image.pk)
        self.assertEqual(item.get_image_path(), image.image.name)

    def test_item_without_image(self):
        data = {
            'title': 'title1',
            'description': 'description1',
        }
        item = SurveyQuizItem(data)
        self.assertEqual(item.title, 'title1')
        self.assertEqual(item.description, 'description1')
        self.assertIsNone(item.image_id)
        self.assertIsNone(item.get_image_path())

    def test_item_with_wrong_image(self):
        data = {
            'title': 'title1',
            'description': 'description1',
            'image_id': 123456,
        }
        item = SurveyQuizItem(data)
        self.assertEqual(item.title, 'title1')
        self.assertEqual(item.description, 'description1')
        self.assertEqual(item.image_id, 123456)
        self.assertIsNone(item.get_image_path())


class TestSurveyQuiz(TestCase):
    fixtures = ['initial_data.json']

    def test_get_item_by_range(self):
        quiz = SurveyQuiz({
            'calc_method': 'range',
            'items': [
                {'title': 'title1'},
                {'title': 'title2'},
                {'title': 'title3'},
            ],
        })

        item = quiz.get_item(12, 0)
        self.assertEqual(item, quiz.items[0])

        item = quiz.get_item(12, 1)
        self.assertEqual(item, quiz.items[0])

        item = quiz.get_item(12, 3.9)
        self.assertEqual(item, quiz.items[0])

        item = quiz.get_item(12, 4)
        self.assertEqual(item, quiz.items[1])

        item = quiz.get_item(12, 7.9)
        self.assertEqual(item, quiz.items[1])

        item = quiz.get_item(12, 8)
        self.assertEqual(item, quiz.items[2])

        item = quiz.get_item(12, 11.9)
        self.assertEqual(item, quiz.items[2])

        item = quiz.get_item(12, 12)
        self.assertEqual(item, quiz.items[2])

        item = quiz.get_item(12, 13)
        self.assertIsNotNone(item)
        self.assertIsNone(item.title)

        item = quiz.get_item(0, 0)
        self.assertEqual(item, quiz.items[0])

        item = quiz.get_item(0, 5)
        self.assertEqual(item, quiz.items[0])

        item = quiz.get_item(0, 9)
        self.assertEqual(item, quiz.items[0])

    def test_get_item_by_pass_scores(self):
        quiz = SurveyQuiz({
            'calc_method': 'scores',
            'pass_scores': 7,
            'items': [
                {'title': 'title1'},
                {'title': 'title2'},
            ],
        })

        item = quiz.get_item(12, 0)
        self.assertEqual(item, quiz.items[0])

        item = quiz.get_item(12, 1)
        self.assertEqual(item, quiz.items[0])

        item = quiz.get_item(12, 6.9)
        self.assertEqual(item, quiz.items[0])

        item = quiz.get_item(12, 7)
        self.assertEqual(item, quiz.items[1])

        item = quiz.get_item(12, 11.9)
        self.assertEqual(item, quiz.items[1])

        item = quiz.get_item(12, 12)
        self.assertEqual(item, quiz.items[1])

        item = quiz.get_item(12, 13)
        self.assertIsNotNone(item)
        self.assertIsNone(item.title)

        item = quiz.get_item(0, 0)
        self.assertEqual(item, quiz.items[0])

        item = quiz.get_item(0, 8)
        self.assertEqual(item, quiz.items[0])

    def test_should_return_empty_item(self):
        quiz = SurveyQuiz()
        item = quiz.get_item(12, 0)
        self.assertIsNotNone(item)
        self.assertIsNone(item.title)
        self.assertIsNone(item.description)
        self.assertIsNone(item.get_image_path())
