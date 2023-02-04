# -*- coding: utf-8 -*-
from unittest.mock import Mock

from django.test import TestCase

from events.data_sources.sources import UserEmailListDataSource
from events.accounts.factories import UserFactory


class TestUserEmailListDataSource(TestCase):
    def setUp(self):
        self.user = UserFactory()

    def test_queryset_should_return_all_emails_from_yauser(self):
        response = UserEmailListDataSource(user=self.user).get_filtered_queryset()
        self.assertEqual(len(response), 1)
        self.assertEqual(response[0], {'value': self.user.email})

    def test_queryset_should_return_all_emails_from_yauser_not_saved(self):
        user = UserFactory(uid='11591999')
        response = UserEmailListDataSource(user=user).get_filtered_queryset()
        self.assertEqual(len(response), 1)
        self.assertEqual(response[0], {'value': user.email})

    def test_should_return_empty_list_if_no_profile(self):
        self.assertEqual(UserEmailListDataSource().get_filtered_queryset(), [])

    def test_should_return_empty_list_if_profile_is_anonymous(self):
        self.assertEqual(
            UserEmailListDataSource(user=UserFactory(uid=None)).get_filtered_queryset(),
            []
        )

    def test_should_filter_by_ids(self):
        data_source_instance = UserEmailListDataSource()
        data_source_instance.get_queryset = Mock(return_value=[
            {'value': 'email@one.com'},
            {'value': 'email@two.com'}
        ])
        response = data_source_instance.get_filtered_queryset(filter_data={
            'id': ['email@one.com']
        })
        results = {
            it['value']: it['value']
            for it in response
        }
        expected = {
            'email@one.com': 'email@one.com',
        }
        self.assertEqual(len(results), len(expected))
        self.assertDictEqual(results, expected)

    def test_should_filter_by_text(self):
        data_source_instance = UserEmailListDataSource()
        data_source_instance.get_queryset = Mock(return_value=[
            {'value': 'email@one.com'},
            {'value': 'email@two.com'}
        ])
        response = data_source_instance.get_filtered_queryset(filter_data={
            'text': 'email@one.com'
        })
        results = {
            it['value']: it['value']
            for it in response
        }
        expected = {
            'email@one.com': 'email@one.com',
        }
        self.assertEqual(len(results), len(expected))
        self.assertDictEqual(results, expected)

    def test_serializer(self):
        response = UserEmailListDataSource.serializer_class(
            UserEmailListDataSource(user=self.user).get_filtered_queryset(),
            many=True
        ).data
        results = {
            it['id']: it['text']
            for it in response
        }
        expected = {
            self.user.email: self.user.email,
        }
        self.assertEqual(len(results), len(expected))
        self.assertDictEqual(results, expected)
