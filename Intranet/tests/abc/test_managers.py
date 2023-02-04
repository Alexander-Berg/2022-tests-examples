# -*- coding: utf-8 -*-
import responses

from django.test import TestCase

from events.abc.models import AbcService


class TestAbcServiceManager(TestCase):

    def get_data_1(self):  # {{{
        return {
            'next': 'https://abc-back.test.yandex-team.ru/api/v4/services/?cursor=5-0&fields=id%2Cslug%2Cname%2Cstate&page_size=5',
            'previous': None,
            'results': [
                {
                    'id': 1,
                    'name': {
                        'ru': 'Портал',
                        'en': 'Портал'
                    },
                    'slug': 'portal',
                    'state': 'deleted'
                },
                {
                    'id': 2,
                    'name': {
                        'ru': 'Главная страница (Морда)',
                        'en': 'Portal'
                    },
                    'slug': 'home',
                    'state': 'develop'
                },
                {
                    'id': 3,
                    'name': {
                        'ru': 'www.ya.ru',
                        'en': 'www.ya.ru'
                    },
                    'slug': 'wwwyaru',
                    'state': 'deleted'
                },
                {
                    'id': 4,
                    'name': {
                        'ru': 'firefox.yandex.ru',
                        'en': 'firefox.yandex.ru'
                    },
                    'slug': 'home5',
                    'state': 'deleted'
                },
                {
                    'id': 5,
                    'name': {
                        'ru': 'Виджетная технология и кабинет разработчика',
                        'en': 'Widgets technology'
                    },
                    'slug': 'home3',
                    'state': 'deleted'
                }
            ]
        }
    # }}}

    def get_data_2(self):  # {{{
        return {
            'next': None,
            'previous': 'https://abc-back.test.yandex-team.ru/api/v4/services/?cursor=7-1&fields=id%2Cslug%2Cname%2Cstate&page_size=5',
            'results': [
                {
                    'id': 7,
                    'name': {
                        'ru': 'Раздел мобильных приложений на мобильном портале',
                        'en': 'Category of mobile applications on mobile portal'
                    },
                    'slug': 'mobile2',
                    'state': 'deleted'
                },
                {
                    'id': 9,
                    'name': {
                        'ru': 'API определялки (детектор)',
                        'en': 'API определялки (детектор)'
                    },
                    'slug': 'apiopredeljalkidetektor',
                    'state': 'deleted'
                },
                {
                    'id': 10,
                    'name': {
                        'ru': 'wap-конвертер',
                        'en': 'wap-конвертер'
                    },
                    'slug': 'wapconverter',
                    'state': 'deleted'
                },
                {
                    'id': 11,
                    'name': {
                        'ru': 'Промо-сайт mobile.yandex.ru',
                        'en': 'Промо-сайт mobile.yandex.ru'
                    },
                    'slug': 'mobileyandex',
                    'state': 'develop'
                },
                {
                    'id': 12,
                    'name': {
                        'ru': 'Контент служба Виджетов',
                        'en': 'Контент служба Виджетов'
                    },
                    'slug': 'supportwgts',
                    'state': 'deleted'
                }
            ]
        }
    # }}}

    def register_uri(self, data):
        responses.add(
            responses.GET,
            'https://abc-back.test.yandex-team.ru/api/v4/services/',
            json=data,
        )

    @responses.activate
    def test_abc_sync_with_source(self):
        self.register_uri(self.get_data_1())
        self.register_uri(self.get_data_2())

        AbcService.objects.sync_with_source()

        self.assertEqual(len(responses.calls), 2)
        self.assertTrue(AbcService.objects.exists())

        abc_service = AbcService.objects.first()
        self.assertIsNotNone(abc_service.name)
        self.assertIsNotNone(abc_service.translations)
        self.assertIn('name', abc_service.translations)
        self.assertIn('ru', abc_service.translations['name'])
        self.assertIn('en', abc_service.translations['name'])
        self.assertEqual(abc_service.translations['name']['ru'], abc_service.name)
