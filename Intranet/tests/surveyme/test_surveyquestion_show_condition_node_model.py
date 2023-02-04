# -*- coding: utf-8 -*-
from django.test import TestCase

from events.surveyme.factories import SurveyQuestionShowConditionNodeFactory
from events.surveyme.models import SurveyQuestionShowConditionNode


class TestSurveyQuestionShowConditionNode(TestCase):
    fixtures = ['initial_data.json']

    def test_ordering(self):
        SurveyQuestionShowConditionNodeFactory(position=3, id=1)
        SurveyQuestionShowConditionNodeFactory(position=2, id=2)
        SurveyQuestionShowConditionNodeFactory(position=1, id=3)
        response = list(SurveyQuestionShowConditionNode.objects.all().values_list('id', flat=True))
        msg = 'по дефолту SurveyQuestionShowConditionNode должен сортироваться по position'
        self.assertEqual(response, [3, 2, 1], msg)
