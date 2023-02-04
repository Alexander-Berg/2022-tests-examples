import unittest

from staff.emission.django.emission_master.utils import import_object
from staff.emission.django.emission_master.exceptions import EmissionImproperlyConfigured


class ImportTest(unittest.TestCase):
    def test_import_class(self):
        # """Импорт. Класс модели"""
        from django.contrib.auth.models import User

        imported_model = import_object('django.contrib.auth.models.User')

        self.assertIs(User, imported_model)

    def test_import_module(self):
        # """Импорт. Модуль"""
        from django.contrib.auth import models

        imported_module = import_object('django.contrib.auth.models')

        self.assertIs(models, imported_module)

    def test_import_exceptions(self):
        # """Импорт. Обработка неправильных данных"""
        with self.assertRaises(EmissionImproperlyConfigured):
            import_object('blah!')
        with self.assertRaises(EmissionImproperlyConfigured):
            import_object('django.blah')
        with self.assertRaises(EmissionImproperlyConfigured):
            import_object('django.contrib.auth.models.Nonexistent')
