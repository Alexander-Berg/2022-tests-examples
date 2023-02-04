# -*- coding: utf-8 -*-
from django.test import TestCase
from guardian.shortcuts import assign_perm, remove_perm
from guardian.models import UserObjectPermission

from events.surveyme.factories import SurveyFactory, SurveyGroupFactory
from events.accounts.helpers import YandexClient


class TestBehavior_permissions_for_simple_survey(TestCase):
    fixtures = ['initial_data.json']
    client_class = YandexClient

    def setUp(self):
        self.survey = SurveyFactory(group=None)
        self.profile = self.client.login_yandex()
        self.profile.is_staff = True
        self.profile.save()

    def test_permissions_for_simple_survey(self):
        assign_perm('surveyme.change_survey', self.profile, self.survey)
        response = self.client.get(f'/admin/api/v2/surveys/{self.survey.pk}/')
        self.assertEqual(response.status_code, 200)

        remove_perm('surveyme.change_survey', self.profile, self.survey)
        response = self.client.get(f'/admin/api/v2/surveys/{self.survey.pk}/')
        self.assertEqual(response.status_code, 403)

    def test_remove_perms_connected_with_object__surveygroup(self):
        group = SurveyGroupFactory()
        assign_perm('surveyme.change_surveygroup', self.profile, group)
        self.assertEqual(UserObjectPermission.objects.filter(object_pk=group.pk).count(), 1)
        group.delete()
        self.assertEqual(UserObjectPermission.objects.filter(object_pk=group.pk).count(), 0)

    def test_remove_perms_connected_with_object__survey(self):
        assign_perm('surveyme.change_survey', self.profile, self.survey)
        self.assertEqual(UserObjectPermission.objects.filter(object_pk=self.survey.pk).count(), 1)
        self.survey.delete()
        self.assertEqual(UserObjectPermission.objects.filter(object_pk=self.survey.pk).count(), 0)

    def test_permissions_for_simple_survey_in_group(self):
        group = SurveyGroupFactory()
        self.survey.group = group
        self.survey.save()

        response = self.client.get(f'/admin/api/v2/surveys/{self.survey.pk}/')
        self.assertEqual(response.status_code, 403)

        assign_perm('surveyme.change_surveygroup', self.profile, group)
        response = self.client.get(f'/admin/api/v2/surveys/{self.survey.pk}/')
        self.assertEqual(response.status_code, 200)

        data = {'auto_control_publication_status': True}
        response = self.client.patch(f'/admin/api/v2/surveys/{self.survey.pk}/', data=data, format='json')
        self.assertEqual(response.status_code, 200)
