# -*- coding: utf-8 -*-
from django.test import TestCase
from django.utils.translation import override

from events.staff.factories import (
    StaffPersonFactory,
    StaffGroupFactory,
    StaffOfficeFactory,
    StaffOrganizationFactory,
)
from events.data_sources.sources import (
    StaffLoginDataSource,
    StaffGroupDataSource,
    StaffOfficeDataSource,
    StaffOrganizationDataSource,
)


class TestStaffLoginDataSource(TestCase):
    def setUp(self):
        self.staff_logins = {
            'kdunaev': StaffPersonFactory(
                login='kdunaev',
                first_name='Кирилл',
                last_name='Дунаев',
                yandex_uid='123',
                translations={
                    'first_name': {
                        'ru': 'Кирилл',
                        'en': 'Kirill',
                    },
                    'last_name': {
                        'ru': 'Дунаев',
                        'en': 'Dunaev',
                    },
                },
            ),
            'chapson': StaffPersonFactory(
                login='chapson',
                first_name='Антон',
                last_name='Чапоргин',
                yandex_uid='234',
                translations={
                    'first_name': {
                        'ru': 'Антон',
                        'en': 'Anton',
                    },
                    'last_name': {
                        'ru': 'Чапоргин',
                        'en': 'Chaporgin',
                    },
                },
            ),
            'masloval': StaffPersonFactory(
                login='masloval',
                first_name='Любовь',
                last_name='Маслова',
                yandex_uid='345',
                translations={
                    'first_name': {
                        'ru': 'Любовь',
                        'en': 'Liubov',
                    },
                    'last_name': {
                        'ru': 'Маслова',
                        'en': 'Maslova',
                    },
                },
            ),
        }

    def filter_queryset(self, **kwargs):
        return StaffLoginDataSource().get_filtered_queryset(filter_data=kwargs)

    def test_suggest_filter_ru(self):
        with override('ru'):
            response = self.filter_queryset(suggest='Ча')
        self.assertEqual(len(response), 1)
        self.assertEqual(response[0].pk, self.staff_logins['chapson'].pk)

        with override('ru'):
            response = self.filter_queryset(suggest='Ан')
        self.assertEqual(len(response), 1)
        self.assertEqual(response[0].pk, self.staff_logins['chapson'].pk)

        with override('ru'):
            response = self.filter_queryset(suggest='chaps')
        self.assertEqual(len(response), 1)
        self.assertEqual(response[0].pk, self.staff_logins['chapson'].pk)

    def test_suggest_filters_login_on_startswith(self):
        with override('ru'):
            response = self.filter_queryset(suggest='apson')
        self.assertEqual(len(response), 0)

    def test_suggest_filters_login_on_startswith_with_multiple_words_same(self):
        with override('ru'):
            response = self.filter_queryset(suggest='Ант chaps')
        self.assertEqual(len(response), 1)

    def test_suggest_filters_login_on_startswith_with_multiple_words_different(self):
        with override('ru'):
            response = self.filter_queryset(suggest='Ант kdun')
        self.assertEqual(len(response), 2)

    def test_suggest_filter_en(self):
        with override('en'):
            response = self.filter_queryset(suggest='Du')
        self.assertEqual(len(response), 1)
        self.assertEqual(response[0].pk, self.staff_logins['kdunaev'].pk)

        with override('en'):
            response = self.filter_queryset(suggest='Ki')
        self.assertEqual(len(response), 1)
        self.assertEqual(response[0].pk, self.staff_logins['kdunaev'].pk)

        with override('en'):
            response = self.filter_queryset(suggest='kdun')
        self.assertEqual(len(response), 1)
        self.assertEqual(response[0].pk, self.staff_logins['kdunaev'].pk)

    def test_suggest_filter_de(self):
        with override('de'):
            response = self.filter_queryset(suggest='Ma')
        self.assertEqual(len(response), 1)
        self.assertEqual(response[0].pk, self.staff_logins['masloval'].pk)

        with override('de'):
            response = self.filter_queryset(suggest='Li')
        self.assertEqual(len(response), 1)
        self.assertEqual(response[0].pk, self.staff_logins['masloval'].pk)

        with override('de'):
            response = self.filter_queryset(suggest='mas')
        self.assertEqual(len(response), 1)
        self.assertEqual(response[0].pk, self.staff_logins['masloval'].pk)

    def test_text_filter_ru(self):
        with override('ru'):
            response = self.filter_queryset(text='chap')
        self.assertEqual(len(response), 0)

        with override('ru'):
            response = self.filter_queryset(text='chapson')
        self.assertEqual(len(response), 1)
        self.assertEqual(response[0].pk, self.staff_logins['chapson'].pk)

    def test_text_filter_en(self):
        with override('en'):
            response = self.filter_queryset(text='kdun')
        self.assertEqual(len(response), 0)

        with override('en'):
            response = self.filter_queryset(text='kdunaev')
        self.assertEqual(response[0].pk, self.staff_logins['kdunaev'].pk)

    def test_text_filter_de(self):
        with override('de'):
            response = self.filter_queryset(text='mas')
        self.assertEqual(len(response), 0)

        with override('de'):
            response = self.filter_queryset(text='masloval')
        self.assertEqual(response[0].pk, self.staff_logins['masloval'].pk)


class TestStaffGroupDataSource(TestCase):
    def setUp(self):
        self.staff_groups = {
            'pcshka': StaffGroupFactory(name='Группа составления', type='department'),
            'ladoga': StaffGroupFactory(name='Группа Ладога', type='department'),
            '2': StaffGroupFactory(name='OneOne', type='department'),
            '1': StaffGroupFactory(name='One', type='department'),
            '4': StaffGroupFactory(name='OneOneOneOne', type='department'),
            '3': StaffGroupFactory(name='OneOneOne', type='department'),
        }

    def filter_queryset(self, **kwargs):
        return StaffGroupDataSource().get_filtered_queryset(filter_data=kwargs)

    def test_suggest_filter_ru(self):
        with override('ru'):
            response = self.filter_queryset(suggest='Группа со')
        self.assertEqual(len(response), 1)
        self.assertEqual(response[0].pk, self.staff_groups['pcshka'].pk)

    def test_suggest_filter_en(self):
        with override('en'):
            response = self.filter_queryset(suggest='Группа Л')
        self.assertEqual(len(response), 1)
        self.assertEqual(response[0].pk, self.staff_groups['ladoga'].pk)

    def test_text_filter_ru(self):
        with override('ru'):
            response = self.filter_queryset(text='Группа со')
        self.assertEqual(len(response), 0)

        with override('ru'):
            response = self.filter_queryset(text='Группа составления')
        self.assertEqual(len(response), 1)
        self.assertEqual(response[0].pk, self.staff_groups['pcshka'].pk)

    def test_text_filter_en(self):
        with override('en'):
            response = self.filter_queryset(text='Группа Л')
        self.assertEqual(len(response), 0)

        with override('en'):
            response = self.filter_queryset(text='Группа Ладога')
        self.assertEqual(len(response), 1)
        self.assertEqual(response[0].pk, self.staff_groups['ladoga'].pk)

    def test_should_return_one_as_first_item(self):
        response = self.filter_queryset(suggest='On')
        self.assertEqual(len(response), 4)
        self.assertEqual(response[0].pk, self.staff_groups['1'].pk)

    def test_should_return_oneone_as_first_item(self):
        response = self.filter_queryset(suggest='OneOn')
        self.assertEqual(len(response), 3)
        self.assertEqual(response[0].pk, self.staff_groups['2'].pk)


class TestStaffOfficeDataSource(TestCase):
    def setUp(self):
        self.staff_offices = {
            'morozov': StaffOfficeFactory(
                name='Морозов',
                translations={
                    'name': {
                        'ru': 'Морозов',
                        'en': 'Morozov',
                    },
                },
            ),
            'mamontov': StaffOfficeFactory(
                name='Мамонтов',
                translations={
                    'name': {
                        'ru': 'Мамонтов',
                        'en': 'Mamontov',
                    },
                },
            ),
            'stroganov': StaffOfficeFactory(
                name='Строганов',
                translations={
                    'name': {
                        'ru': 'Строганов',
                        'en': 'Stroganov',
                    },
                },
            ),
        }

    def filter_queryset(self, **kwargs):
        return StaffOfficeDataSource().get_filtered_queryset(filter_data=kwargs)

    def test_suggest_filter_ru(self):
        with override('ru'):
            response = self.filter_queryset(suggest='Моро')
        self.assertEqual(len(response), 1)
        self.assertEqual(response[0].pk, self.staff_offices['morozov'].pk)

    def test_suggest_filter_en(self):
        with override('en'):
            response = self.filter_queryset(suggest='Mamo')
        self.assertEqual(len(response), 1)
        self.assertEqual(response[0].pk, self.staff_offices['mamontov'].pk)

    def test_suggest_filter_de(self):
        with override('de'):
            response = self.filter_queryset(suggest='Stro')
        self.assertEqual(len(response), 1)
        self.assertEqual(response[0].pk, self.staff_offices['stroganov'].pk)

    def test_text_filter_ru(self):
        with override('ru'):
            response = self.filter_queryset(text='Моро')
        self.assertEqual(len(response), 0)

        with override('ru'):
            response = self.filter_queryset(text='Морозов')
        self.assertEqual(len(response), 1)
        self.assertEqual(response[0].pk, self.staff_offices['morozov'].pk)

    def test_text_filter_en(self):
        with override('en'):
            response = self.filter_queryset(text='Mamo')
        self.assertEqual(len(response), 0)

        with override('en'):
            response = self.filter_queryset(text='Mamontov')
        self.assertEqual(len(response), 1)
        self.assertEqual(response[0].pk, self.staff_offices['mamontov'].pk)

    def test_text_filter_de(self):
        with override('de'):
            response = self.filter_queryset(text='Stro')
        self.assertEqual(len(response), 0)

        with override('de'):
            response = self.filter_queryset(text='Stroganov')
        self.assertEqual(len(response), 1)
        self.assertEqual(response[0].pk, self.staff_offices['stroganov'].pk)


class TestStaffOrganozationDataSource(TestCase):
    def setUp(self):
        self.staff_organizations = {
            'serp': StaffOrganizationFactory(
                name='Поисковый портал',
            ),
            'taxi': StaffOrganizationFactory(
                name='Яндекс Такси',
            ),
        }

    def filter_queryset(self, **kwargs):
        return StaffOrganizationDataSource().get_filtered_queryset(filter_data=kwargs)

    def test_suggest_filter_ru(self):
        with override('ru'):
            response = self.filter_queryset(suggest='Поис')
        self.assertEqual(len(response), 1)
        self.assertEqual(response[0].pk, self.staff_organizations['serp'].pk)

    def test_suggest_filter_en(self):
        with override('en'):
            response = self.filter_queryset(suggest='Янде')
        self.assertEqual(len(response), 1)
        self.assertEqual(response[0].pk, self.staff_organizations['taxi'].pk)

    def test_text_filter_ru(self):
        with override('ru'):
            response = self.filter_queryset(text='Поис')
        self.assertEqual(len(response), 0)

        with override('ru'):
            response = self.filter_queryset(text='Поисковый портал')
        self.assertEqual(len(response), 1)
        self.assertEqual(response[0].pk, self.staff_organizations['serp'].pk)

    def test_text_filter_en(self):
        with override('en'):
            response = self.filter_queryset(text='Янде')
        self.assertEqual(len(response), 0)

        with override('en'):
            response = self.filter_queryset(text='Яндекс Такси')
        self.assertEqual(len(response), 1)
        self.assertEqual(response[0].pk, self.staff_organizations['taxi'].pk)
