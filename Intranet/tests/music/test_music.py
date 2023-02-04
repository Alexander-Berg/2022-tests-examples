# -*- coding: utf-8 -*-
import responses

from django.test import TestCase
from django.conf import settings
from requests.exceptions import HTTPError
from events.music.client import MusicGenreClient


class TestMusicGenreClient(TestCase):
    def register_uri(self, json, status=200):
        responses.add(responses.GET, settings.MUSIC_GENRE_URL, json=json, status=status)

    @responses.activate
    def test_json(self):
        data = {
            'result': [
                {
                    'id': 'all',
                    'tracksCount': 100,
                    'title': 'Все',
                    'titles': {
                        'ru': {
                            'title': 'Все',
                        },
                        'en': {
                            'title': 'All',
                        },
                    },
                },
                {
                    'id': 'jazz',
                    'tracksCount': 70,
                    'title': 'Джаз',
                    'titles': {
                        'ru': {
                            'title': 'Джаз',
                        },
                        'en': {
                            'title': 'Jazz',
                        },
                    },
                    'subGenres': [
                        {
                            'id': 'traditional',
                            'tracksCount': 60,
                            'title': 'Традиционный',
                            'titles': {
                                'ru': {
                                    'title': 'Традиционный',
                                },
                                'en': {
                                    'title': 'Traditional',
                                },
                            },
                        },
                        {
                            'id': 'contemporary',
                            'tracksCount': 10,
                            'title': 'Современный',
                            'titles': {
                                'ru': {
                                    'title': 'Современный',
                                },
                                'en': {
                                    'title': 'Contemporary',
                                },
                            },
                        },
                    ],
                },
            ]
        }
        self.register_uri(json=data)
        client = MusicGenreClient()
        response = client.make_request()
        result = {
            it['music_id']: it
            for it in response
        }
        self.assertEqual(4, len(result))
        expected = {
            'all': {
                'music_id': 'all',
                'title': 'Все',
                'translations': {
                    'title': {
                        'ru': 'Все',
                        'en': 'All',
                    },
                },
            },
            'jazz': {
                'music_id': 'jazz',
                'title': 'Джаз',
                'translations': {
                    'title': {
                        'ru': 'Джаз',
                        'en': 'Jazz',
                    },
                },
            },
            'traditional': {
                'music_id': 'traditional',
                'title': 'Джаз \u2192 Традиционный',
                'translations': {
                    'title': {
                        'ru': 'Джаз \u2192 Традиционный',
                        'en': 'Jazz \u2192 Traditional',
                    },
                },
            },
            'contemporary': {
                'music_id': 'contemporary',
                'title': 'Джаз \u2192 Современный',
                'translations': {
                    'title': {
                        'ru': 'Джаз \u2192 Современный',
                        'en': 'Jazz \u2192 Contemporary',
                    },
                },
            },
        }
        self.assertDictEqual(expected, result)

    @responses.activate
    def test_raise_for_status(self):
        self.register_uri(json={}, status=400)
        client = MusicGenreClient()
        with self.assertRaises(HTTPError):
            for it in client.make_request():
                pass
