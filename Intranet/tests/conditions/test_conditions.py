# -*- coding: utf-8 -*-
from django.db import models
from django.contrib.contenttypes.models import ContentType
from django.test import TestCase

from events.accounts.models import User
from events.conditions.models import (
    ConditionItemBase,
    ConditionNodeBase,
    ContentTypeAttribute,
)
from events.surveyme.models import SurveyQuestion


class ConditionNode(ConditionNodeBase):
    class Meta:
        app_label = 'conditions'


class ConditionItem(ConditionItemBase):
    node = models.ForeignKey(ConditionNode, related_name='items', on_delete=models.DO_NOTHING)  # you must add node FK
    survey_question = models.ForeignKey(SurveyQuestion, null=True, blank=True, on_delete=models.CASCADE)

    class Meta:
        app_label = 'conditions'

    def is_true(self, **kwargs):
        return self.value == 'True'


class TestConditionNodeBase__is_true(TestCase):
    def setUp(self):
        self.content_type_attribute = ContentTypeAttribute.objects.create(
            title='User',
            content_type=ContentType.objects.get_for_model(User),
            attr='username'
        )
        self.node = ConditionNode.objects.create()

    def test_should_be_false_if_no_items(self):
        self.assertFalse(self.node.is_true())

    def test_experiments(self):
        experiments = [
            {
                'items': [
                    {'operator': 'and', 'is_true': True},
                ],
                'expected': True
            },
            {
                'items': [
                    {'operator': 'and', 'is_true': False},
                ],
                'expected': False
            },

            {
                'items': [
                    {'operator': 'or', 'is_true': True},
                ],
                'expected': True
            },
            {
                'items': [
                    {'operator': 'or', 'is_true': False},
                ],
                'expected': False
            },

            {
                'items': [
                    {'operator': 'and', 'is_true': True},
                    {'operator': 'and', 'is_true': True},
                ],
                'expected': True
            },
            {
                'items': [
                    {'operator': 'and', 'is_true': True},
                    {'operator': 'and', 'is_true': False},
                ],
                'expected': False
            },
            {
                'items': [
                    {'operator': 'and', 'is_true': False},
                    {'operator': 'and', 'is_true': True},
                ],
                'expected': False
            },
            {
                'items': [
                    {'operator': 'and', 'is_true': False},
                    {'operator': 'and', 'is_true': False},
                ],
                'expected': False
            },

            {
                'items': [
                    {'operator': 'or', 'is_true': True},
                    {'operator': 'or', 'is_true': True},
                ],
                'expected': True
            },
            {
                'items': [
                    {'operator': 'or', 'is_true': True},
                    {'operator': 'or', 'is_true': False},
                ],
                'expected': True
            },
            {
                'items': [
                    {'operator': 'or', 'is_true': False},
                    {'operator': 'or', 'is_true': True},
                ],
                'expected': True
            },
            {
                'items': [
                    {'operator': 'or', 'is_true': False},
                    {'operator': 'or', 'is_true': False},
                ],
                'expected': False
            },

            # mixed conditions
            {
                'items': [
                    {'operator': 'and', 'is_true': True},
                    {'operator': 'and', 'is_true': False},
                    {'operator': 'or', 'is_true': True},
                ],
                'expected': True
            },

            {
                'items': [
                    {'operator': 'and', 'is_true': True},
                    {'operator': 'and', 'is_true': False},
                    {'operator': 'and', 'is_true': True},
                ],
                'expected': False
            },
            {
                'items': [
                    {'operator': 'or', 'is_true': False},
                    {'operator': 'or', 'is_true': False},
                    {'operator': 'or', 'is_true': True},
                ],
                'expected': True
            },
        ]

        for exp in experiments:
            self.node.items.all().delete()
            for item in exp['items']:
                self.node.items.create(
                    operator=item['operator'],
                    content_type_attribute=self.content_type_attribute,
                    condition='eq',  # any condition, it doesn't used in these tests
                    value=str(item['is_true'])
                )
            self.assertEqual(self.node.is_true(), exp['expected'], msg=exp)
