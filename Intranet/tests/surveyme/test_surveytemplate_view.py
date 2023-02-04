# -*- coding: utf-8 -*-
from django.test import TestCase
from events.surveyme.factories import SurveyTemplateFactory


class TestSurveyTemplate(TestCase):
    fixtures = ['initial_data.json']

    def setUp(self):
        self.templates = [
            SurveyTemplateFactory(),
            SurveyTemplateFactory(data={
                'survey': {},
                'texts': [],
            }),
            SurveyTemplateFactory(is_personal=True),
        ]

    def test_should_return_list_of_templates(self):
        response = self.client.get('/admin/api/v2/survey-templates/')
        self.assertEqual(response.status_code, 200)

        data = response.data['results']
        self.assertEqual(len(data), 2)
        result = {
            template['id']
            for template in data
        }
        expected = {
            template.pk
            for template in self.templates
            if not template.is_personal
        }

        self.assertSetEqual(result, expected)

    def test_should_return_template(self):
        response = self.client.get('/admin/api/v2/survey-templates/%s/' % self.templates[1].pk)
        self.assertEqual(response.status_code, 200)

        self.assertEqual(response.data['id'], self.templates[1].pk)

    def test_shouldnt_return_template(self):
        response = self.client.get('/admin/api/v2/survey-templates/%s/' % self.templates[2].pk)
        self.assertEqual(response.status_code, 404)

    def test_shouldnt_support_template_changing(self):
        data = {
            'survey': {},
        }
        response = self.client.patch('/admin/api/v2/survey-templates/%s/' % self.templates[0].pk, data=data, format='json')
        self.assertEqual(response.status_code, 405)
