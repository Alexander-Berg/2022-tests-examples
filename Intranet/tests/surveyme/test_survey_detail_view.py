# -*- coding: utf-8 -*-
from django.test import TestCase

from events.surveyme.factories import (
    SurveyFactory,
    SurveyQuestionFactory,
)
from events.surveyme.models import AnswerType
from events.accounts.helpers import YandexClient


class TestSurveyQuestionCount(TestCase):
    fixtures = ['initial_data.json']
    client_class = YandexClient

    def setUp(self):
        # создаем опрос
        self.survey = SurveyFactory()
        self.count_url = f'/admin/api/v2/surveys/{self.survey.pk}/questions-count/'
        self.client.login_yandex(is_superuser=True)

    def create_questions_for_survey(self, survey):
        answer_long_text = AnswerType.objects.get(slug='answer_long_text')
        answer_boolean = AnswerType.objects.get(slug='answer_boolean')

        # создаем вопросы к опросу
        self.questions = {
            'О себе': SurveyQuestionFactory(survey=survey, label='О себе', answer_type=answer_long_text, param_is_required=False),
            'Согласен?': SurveyQuestionFactory(survey=survey, label='Согласен?', answer_type=answer_boolean),
        }

    def test_should_return_count_if_questions(self):
        self.create_questions_for_survey(survey=self.survey)
        response = self.client.get(self.count_url)
        assert response.status_code == 200
        assert response.json() == {'questions_count': 2}

    def test_should_return_zero_without_questions(self):
        response = self.client.get(self.count_url)
        assert response.status_code == 200
        assert response.json() == {'questions_count': 0}
