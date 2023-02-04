# -*- coding: utf-8 -*-
from events.common_app.helpers import TestConditionBase
from events.surveyme.factories import SurveyFactory, SurveyQuestionFactory
from events.surveyme.models import Survey
from events.surveyme.makers.survey import (
    make_is_has_questions,
    make_not_is_has_questions,
)


class TestIsHasQuestions(TestConditionBase):
    fixtures = ['initial_data.json']
    model = Survey
    queryset_method_name = None
    instance_property_name = 'is_has_questions'

    def create_instance(self):
        return SurveyFactory()

    def test_is_has_questions(self):
        SurveyQuestionFactory(survey=self.instance)
        self.assertConditionTrue(msg='Survey с SurveyQuestion должен быть is_has_questions')

    def test_not_is_has_questions(self):
        self.assertConditionFalse(msg='Survey без SurveyQuestion не должен быть is_has_questions')

    def test_make_is_has_questions(self):
        make_is_has_questions(self.instance)
        self.assertConditionTrue(msg='make_is_has_questions должен сделать Survey is_has_questions')

    def test_make_not_is_has_questions(self):
        SurveyQuestionFactory(survey=self.instance)
        make_not_is_has_questions(self.instance)
        self.assertConditionFalse(msg='make_is_has_questions должен сделать Survey не is_has_questions')
