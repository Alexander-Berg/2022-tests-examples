# -*- coding: utf-8 -*-

from events.common_app.helpers import TestConditionBase
from events.surveyme.factories import SurveyFactory, ProfileSurveyAnswerFactory
from events.surveyme.models import Survey
from events.surveyme.makers.survey import (
    make_is_already_answered_by_profile,
    make_not_is_already_answered_by_profile,
)
from events.accounts.factories import UserFactory


class TestSurvey__is_already_answered_by_profile(TestConditionBase):
    fixtures = ['initial_data.json']
    model = Survey
    queryset_method_name = None
    instance_property_name = 'is_already_answered_by_profile'

    def create_instance(self):
        self.user = UserFactory()
        return SurveyFactory()

    def test_is_already_answered_by_profile(self):
        ProfileSurveyAnswerFactory(survey=self.instance, user=self.user)
        kwargs = {
            'user': self.user
        }
        self.assertConditionTrue(kwargs=kwargs)

    def test_is_not_already_answered_by_profile(self):
        ProfileSurveyAnswerFactory(survey=self.instance)
        kwargs = {
            'user': self.user
        }
        self.assertConditionFalse(kwargs=kwargs)

    def test_make_is_already_answered_by_profile(self):
        ProfileSurveyAnswerFactory(survey=self.instance)
        kwargs = {
            'user': self.user
        }
        make_is_already_answered_by_profile(self.instance, self.user)
        self.assertConditionTrue(kwargs=kwargs, msg='make_is_already_answered_by_profile должен делать Survey is_already_answered_by_profile')

    def test_make_not_is_already_answered_by_profile(self):
        ProfileSurveyAnswerFactory(survey=self.instance, user=self.user)
        kwargs = {
            'user': self.user
        }
        make_not_is_already_answered_by_profile(self.instance, self.user)
        self.assertConditionFalse(kwargs=kwargs, msg='make_not_is_already_answered_by_profile должен делать Survey не is_already_answered_by_profile')
