# -*- coding: utf-8 -*-
from django.test import TestCase

from events.surveyme.factories import SurveyQuestionShowConditionNodeItemFactory
from events.surveyme.models import SurveyQuestionShowConditionNodeItem
from events.conditions.factories import ContentTypeAttributeFactory


class TestSurveyQuestionShowConditionNodeItem(TestCase):
    fixtures = ['initial_data.json']

    def test_ordering(self):
        content_type_attribute = ContentTypeAttributeFactory()
        SurveyQuestionShowConditionNodeItemFactory(position=3, id=1, content_type_attribute=content_type_attribute)
        SurveyQuestionShowConditionNodeItemFactory(position=2, id=2, content_type_attribute=content_type_attribute)
        SurveyQuestionShowConditionNodeItemFactory(position=1, id=3, content_type_attribute=content_type_attribute)
        response = list(SurveyQuestionShowConditionNodeItem.objects.all().values_list('id', flat=True))
        msg = 'по дефолту SurveyQuestionShowConditionNodeItem должен сортироваться по position'
        self.assertEqual(response, [3, 2, 1], msg)
