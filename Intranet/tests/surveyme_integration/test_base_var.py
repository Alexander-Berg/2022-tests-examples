# -*- coding: utf-8 -*-
from django.test import TestCase
from django.utils.translation import ugettext as _

from events.accounts.helpers import YandexClient
from events.surveyme_integration.variables import variable_categories_list
from events.surveyme_integration.variables.form import FormVariableCategory


class TestVariableCategories(TestCase):
    fixtures = ['initial_data.json']
    client_class = YandexClient

    def setUp(self):
        self.client.login_yandex(is_superuser=True)

    def test_categories_type(self):
        self.assertTrue(isinstance(variable_categories_list, list))
        self.assertTrue(variable_categories_list[0] is FormVariableCategory)

    def test_categories_request(self):
        response = self.client.get('/admin/api/v2/variable-categories/')
        self.assertEqual(response.status_code, 200)
        self.assertEqual(response.data[0], {'name': 'form', 'title': _('Форма')})
