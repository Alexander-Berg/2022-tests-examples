# -*- coding: utf-8 -*-
from django.test import TestCase
from django.utils.translation import override

from events.abc.factories import AbcServiceFactory
from events.data_sources.sources import AbcServiceDataSource


class TestAbcServiceDataSource(TestCase):
    def setUp(self):
        self.abc_services = {
            'forms': AbcServiceFactory(
                name='Формы',
                slug='forms',
                translations={
                    'name': {
                        'ru': 'Формы',
                        'en': 'Forms',
                    },
                },
            ),
            'tracker': AbcServiceFactory(
                name='Трекер',
                slug='tracker',
                translations={
                    'name': {
                        'ru': 'Трекер',
                        'en': 'Tracker',
                    },
                },
            ),
            'connect': AbcServiceFactory(
                name='Коннект',
                slug='connect',
                translations={
                    'name': {
                        'ru': 'Коннект',
                        'en': 'Connect',
                    },
                },
            ),
        }

    def filter_queryset(self, **kwargs):
        return AbcServiceDataSource().get_filtered_queryset(filter_data=kwargs)

    def test_suggest_filter_ru(self):
        with override('ru'):
            response = self.filter_queryset(suggest='Тре')
        self.assertEqual(len(response), 1)
        self.assertEqual(response[0].pk, self.abc_services['tracker'].pk)

    def test_suggest_filter_en(self):
        with override('en'):
            response = self.filter_queryset(suggest='Fo')
        self.assertEqual(len(response), 1)
        self.assertEqual(response[0].pk, self.abc_services['forms'].pk)

    def test_suggest_filter_de(self):
        with override('de'):
            response = self.filter_queryset(suggest='Co')
        self.assertEqual(len(response), 1)
        self.assertEqual(response[0].pk, self.abc_services['connect'].pk)

    def test_text_filter_ru(self):
        with override('ru'):
            response = self.filter_queryset(text='Тре')
        self.assertEqual(len(response), 0)

        with override('ru'):
            response = self.filter_queryset(text='Трекер')
        self.assertEqual(len(response), 1)
        self.assertEqual(response[0].pk, self.abc_services['tracker'].pk)

    def test_text_filter_en(self):
        with override('en'):
            response = self.filter_queryset(text='Fo')
        self.assertEqual(len(response), 0)

        with override('en'):
            response = self.filter_queryset(text='Forms')
        self.assertEqual(len(response), 1)
        self.assertEqual(response[0].pk, self.abc_services['forms'].pk)

    def test_text_filter_de(self):
        with override('de'):
            response = self.filter_queryset(text='Co')
        self.assertEqual(len(response), 0)

        with override('de'):
            response = self.filter_queryset(text='Connect')
        self.assertEqual(len(response), 1)
        self.assertEqual(response[0].pk, self.abc_services['connect'].pk)
