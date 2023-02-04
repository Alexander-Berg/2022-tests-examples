# -*- coding: utf-8 -*-
from django.test import TestCase

from events.support import serializers


class TestChangeOwnerSerializer(TestCase):
    def test_success_1(self):
        data = {
            'survey_id': 123,
            'uid': '2345',
            'cloud_uid': 'abcd',
            'email': 'user@yandex.ru',
        }
        serializer = serializers.ChangeOwnerSerializer(data=data)
        self.assertTrue(serializer.is_valid())

    def test_success_2(self):
        data = {
            'survey_id': 123,
            'uid': '2345',
        }
        serializer = serializers.ChangeOwnerSerializer(data=data)
        self.assertTrue(serializer.is_valid())

    def test_success_3(self):
        data = {
            'survey_id': 123,
            'cloud_uid': 'abcd',
        }
        serializer = serializers.ChangeOwnerSerializer(data=data)
        self.assertTrue(serializer.is_valid())

    def test_success_4(self):
        data = {
            'survey_id': 123,
            'email': 'user@yandex.ru',
        }
        serializer = serializers.ChangeOwnerSerializer(data=data)
        self.assertTrue(serializer.is_valid())

    def test_error_1(self):
        data = {
            'survey_id': 123,
        }
        serializer = serializers.ChangeOwnerSerializer(data=data)
        self.assertFalse(serializer.is_valid())

    def test_error_2(self):
        data = {
            'survey_id': 123,
            'uid': 'abcd',
        }
        serializer = serializers.ChangeOwnerSerializer(data=data)
        self.assertFalse(serializer.is_valid())

    def test_error_3(self):
        data = {
            'survey_id': 123,
            'email': 'user-at-yandex.ru',
        }
        serializer = serializers.ChangeOwnerSerializer(data=data)
        self.assertFalse(serializer.is_valid())


class TestChangeOrganizationSerializer(TestCase):
    def test_success_1(self):
        data = {
            'survey_id': 123,
            'dir_id': '2345',
        }
        serializer = serializers.ChangeOrganizationSerializer(data=data)
        self.assertTrue(serializer.is_valid())

    def test_success_2(self):
        data = {
            'survey_id': 123,
            'dir_id': None,
        }
        serializer = serializers.ChangeOrganizationSerializer(data=data)
        self.assertTrue(serializer.is_valid())

    def test_error_1(self):
        data = {
            'survey_id': 123,
            'dir_id': 'abcd',
        }
        serializer = serializers.ChangeOrganizationSerializer(data=data)
        self.assertFalse(serializer.is_valid())
