# -*- coding: utf-8 -*-
from django.test import TestCase
from django.utils.translation import override

from events.music.factories import MusicGenreFactory
from events.data_sources.sources import MusicGenreDataSource


class TestMusicGenreDataSource(TestCase):
    def setUp(self):
        self.music_genres = {
            'rock': MusicGenreFactory(
                title='Рок',
                music_id='rock',
                translations={
                    'title': {
                        'ru': 'Рок',
                        'en': 'Rock',
                    },
                },
            ),
            'jazz': MusicGenreFactory(
                title='Джаз',
                music_id='jazz',
                translations={
                    'title': {
                        'ru': 'Джаз',
                        'en': 'Jazz',
                    },
                },
            ),
            'classic': MusicGenreFactory(
                title='Классика',
                music_id='classic',
                translations={
                    'title': {
                        'ru': 'Классика',
                        'en': 'Classic',
                    },
                },
            ),
        }

    def filter_queryset(self, **kwargs):
        return MusicGenreDataSource().get_filtered_queryset(filter_data=kwargs)

    def test_suggest_filter_ru(self):
        with override('ru'):
            response = self.filter_queryset(suggest='Ро')
        self.assertEqual(len(response), 1)
        self.assertEqual(response[0].pk, self.music_genres['rock'].pk)

    def test_suggest_filter_en(self):
        with override('en'):
            response = self.filter_queryset(suggest='Ja')
        self.assertEqual(len(response), 1)
        self.assertEqual(response[0].pk, self.music_genres['jazz'].pk)

    def test_suggest_filter_de(self):
        with override('de'):
            response = self.filter_queryset(suggest='Cl')
        self.assertEqual(len(response), 1)
        self.assertEqual(response[0].pk, self.music_genres['classic'].pk)

    def test_text_filter_ru(self):
        with override('ru'):
            response = self.filter_queryset(text='Ро')
        self.assertEqual(len(response), 0)

        with override('ru'):
            response = self.filter_queryset(text='Рок')
        self.assertEqual(len(response), 1)
        self.assertEqual(response[0].pk, self.music_genres['rock'].pk)

    def test_text_filter_en(self):
        with override('en'):
            response = self.filter_queryset(text='Ja')
        self.assertEqual(len(response), 0)

        with override('en'):
            response = self.filter_queryset(text='Jazz')
        self.assertEqual(len(response), 1)
        self.assertEqual(response[0].pk, self.music_genres['jazz'].pk)

    def test_text_filter_de(self):
        with override('de'):
            response = self.filter_queryset(text='Cl')
        self.assertEqual(len(response), 0)

        with override('de'):
            response = self.filter_queryset(text='Classic')
        self.assertEqual(len(response), 1)
        self.assertEqual(response[0].pk, self.music_genres['classic'].pk)
