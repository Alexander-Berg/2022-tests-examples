# -*- coding: utf-8 -*-
from django.test import TestCase
from unittest.mock import patch

from events.geobase_contrib.client import GeobaseClient
from events.surveyme.factories import ProfileSurveyAnswerFactory
from events.surveyme_integration.variables.geobase import (
    GeobaseIdVariable,
    GeobaseNameVariable,
)


class TestGeobaseIdVariable(TestCase):
    def setUp(self):
        self.answer = ProfileSurveyAnswerFactory()

    def test_by_x_geobase_id(self):
        self.answer.source_request = {'x-geobase-id': '213'}
        var = GeobaseIdVariable(answer=self.answer)
        with patch.object(GeobaseClient, 'get_region_by_id') as mock_get_region_by_id:
            mock_get_region_by_id.return_value = {
                'id': 213,
                'name': 'Москва',
                'en_name': 'Moscow',
            }
            self.assertEqual(var.get_value(), '213')
        mock_get_region_by_id.assert_called_once_with('213')

    def test_by_x_real_ip(self):
        self.answer.source_request = {'ip': '95.108.174.99'}
        var = GeobaseIdVariable(answer=self.answer)
        with patch.object(GeobaseClient, 'get_region_by_id') as mock_get_region_by_id:
            with patch.object(GeobaseClient, 'get_region_by_ip') as mock_get_region_by_ip:
                mock_get_region_by_ip.return_value = {
                    'id': 213,
                    'name': 'Москва',
                    'en_name': 'Moscow',
                }
                self.assertEqual(var.get_value(), '213')
        mock_get_region_by_ip.assert_called_once_with('95.108.174.99')
        mock_get_region_by_id.assert_not_called()


class TestGeobaseNameVariable(TestCase):
    def setUp(self):
        self.answer = ProfileSurveyAnswerFactory()
        self.answer.source_request = {'x-geobase-id': '213'}

    def test_get_lang(self):
        var = GeobaseNameVariable(answer=self.answer)
        self.assertEqual(var.get_lang(), 'ru')

        self.answer.source_request['accept-language'] = 'en-US;q=0.7,ru;q=0.5,en;q=0.3'
        var = GeobaseNameVariable(answer=self.answer)
        self.assertEqual(var.get_lang(), 'en-US')

        self.answer.source_request['accept-language'] = 'ru,en-US;q=0.7,en;q=0.3'
        var = GeobaseNameVariable(answer=self.answer)
        self.assertEqual(var.get_lang(), 'ru')

    def test_by_default_language(self):
        var = GeobaseNameVariable(answer=self.answer)
        with patch.object(GeobaseNameVariable, 'get_region') as mock_get_region:
            mock_get_region.return_value = {
                'id': 213,
                'name': 'Москва',
                'en_name': 'Moscow',
            }
            self.assertEqual(var.get_value(), 'Москва')

    def test_by_russian_language(self):
        self.answer.source_request['accept-language'] = 'ru,en-US;q=0.7,en;q=0.3'
        var = GeobaseNameVariable(answer=self.answer)
        with patch.object(GeobaseNameVariable, 'get_region') as mock_get_region:
            mock_get_region.return_value = {
                'id': 213,
                'name': 'Москва',
                'en_name': 'Moscow',
            }
            self.assertEqual(var.get_value(), 'Москва')

    def test_by_english_language(self):
        self.answer.source_request['accept-language'] = 'en-US;q=0.7,ru;q=0.5,en;q=0.3'
        var = GeobaseNameVariable(answer=self.answer)
        with patch.object(GeobaseNameVariable, 'get_region') as mock_get_region:
            mock_get_region.return_value = {
                'id': 213,
                'name': 'Москва',
                'en_name': 'Moscow',
            }
            self.assertEqual(var.get_value(), 'Moscow')
