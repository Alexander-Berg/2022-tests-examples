# -*- coding: utf-8 -*-
from django.test import TestCase, override_settings
from functools import partial
from unittest.mock import patch

from events.surveyme_integration.services.startrek.action_processors import StartrekBaseActionProcessor


@patch.object(StartrekBaseActionProcessor, 'is_cloud_uid_in_org')
@patch.object(StartrekBaseActionProcessor, 'is_uid_in_org')
class TestStartrekActionProcessorIntranet(TestCase):
    def setUp(self):
        self.data = {'queue': 'test'}
        self.processor = partial(StartrekBaseActionProcessor, status='test')

    def test_should_leave_author_blank(self, mock_uid, mock_cloud_uid):
        data = self.processor(data=self.data).get_data_for_insert()
        self.assertIsNone(data.get('author'))

    def test_should_leave_author_as_is(self, mock_uid, mock_cloud_uid):
        self.data['author'] = 'myname'
        data = self.processor(data=self.data).get_data_for_insert()
        self.assertEqual(data['author'], 'myname')

    def test_should_set_author_to_yandexuid(self, mock_uid, mock_cloud_uid):
        self.data['author'] = None
        self.data['yandexuid'] = '1234'
        self.data['cloud_uid'] = 'abcd'

        data = self.processor(data=self.data).get_data_for_insert()

        mock_uid.assert_not_called()
        mock_cloud_uid.assert_not_called()
        self.assertEqual(data['author'], '1234')


@override_settings(IS_BUSINESS_SITE=True, IS_INTERNAL_SITE=False)
@patch.object(StartrekBaseActionProcessor, 'is_cloud_uid_in_org')
@patch.object(StartrekBaseActionProcessor, 'is_uid_in_org')
class TestStartrekActionProcessorBusiness(TestCase):
    def setUp(self):
        self.data = {'queue': 'test'}
        self.processor = partial(StartrekBaseActionProcessor, status='test')

    def test_should_leave_author_blank(self, mock_uid, mock_cloud_uid):
        data = self.processor(data=self.data).get_data_for_insert()
        self.assertIsNone(data.get('author'))

    def test_should_leave_author_as_is(self, mock_uid, mock_cloud_uid):
        self.data['author'] = 'myname'
        data = self.processor(data=self.data).get_data_for_insert()

        mock_uid.assert_not_called()
        mock_cloud_uid.assert_not_called()
        self.assertEqual(data['author'], 'myname')

    def test_should_set_author_as_uid_if_in_org(self, mock_uid, mock_cloud_uid):
        self.data['author'] = None
        self.data['yandexuid'] = '1234'
        self.data['cloud_uid'] = 'abcd'
        self.data['org_dir_id'] = '2345'

        mock_uid.return_value = True
        data = self.processor(data=self.data).get_data_for_insert()

        mock_uid.assert_called_once_with('2345', '1234')
        mock_cloud_uid.assert_not_called()
        self.assertEqual(data['author'], {'uid': '1234'})

    def test_shouldnt_set_author_as_uid_if_not_in_org(self, mock_uid, mock_cloud_uid):
        self.data['author'] = None
        self.data['yandexuid'] = '1234'
        self.data['cloud_uid'] = 'abcd'
        self.data['org_dir_id'] = '2345'

        mock_uid.return_value = False
        data = self.processor(data=self.data).get_data_for_insert()

        mock_uid.assert_called_once_with('2345', '1234')
        mock_cloud_uid.assert_not_called()
        self.assertTrue('author' not in data)

    def test_shouldnt_leave_author_if_org_dir_id_doesnt_set(self, mock_uid, mock_cloud_uid):
        self.data['author'] = None
        self.data['yandexuid'] = '1234'
        self.data['cloud_uid'] = 'abcd'
        self.data['org_dir_id'] = None

        data = self.processor(data=self.data).get_data_for_insert()

        mock_uid.assert_not_called()
        mock_cloud_uid.assert_not_called()
        self.assertTrue('author' not in data)

    def test_should_set_author_as_cloud_uid_if_in_org(self, mock_uid, mock_cloud_uid):
        self.data['author'] = None
        self.data['yandexuid'] = None
        self.data['cloud_uid'] = 'abcd'
        self.data['org_dir_id'] = '2345'

        mock_cloud_uid.return_value = True
        data = self.processor(data=self.data).get_data_for_insert()

        mock_uid.assert_not_called()
        mock_cloud_uid.assert_called_once_with('2345', 'abcd')
        self.assertEqual(data['author'], {'cloudUid': 'abcd'})

    def test_shouldnt_set_author_as_cloud_uid_if_not_in_org(self, mock_uid, mock_cloud_uid):
        self.data['author'] = None
        self.data['yandexuid'] = None
        self.data['cloud_uid'] = 'abcd'
        self.data['org_dir_id'] = '2345'

        mock_cloud_uid.return_value = False
        data = self.processor(data=self.data).get_data_for_insert()

        mock_uid.assert_not_called()
        mock_cloud_uid.assert_called_once_with('2345', 'abcd')
        self.assertTrue('author' not in data)
