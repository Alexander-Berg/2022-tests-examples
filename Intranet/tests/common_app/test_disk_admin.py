# -*- coding: utf-8 -*-
from django.test import TestCase
from django.test.utils import override_settings

from unittest.mock import call, patch, Mock

from events.common_app.disk_admin import DiskAdminAPIClient
from events.common_app.disk_admin.mock_client import (
    VALID_DATA,
    INVALID_DATA,
    DISK_ADMIN_API_DATA,
    mocked_requests,
)


class TestDiskAdminAPIClient(TestCase):
    def setUp(self):
        self.valid_uid = '1'
        self.invalid_uid = '2'
        DISK_ADMIN_API_DATA.valid_uids = [self.valid_uid]
        DISK_ADMIN_API_DATA.invalid_uids = [self.invalid_uid]
        self.disk_client = DiskAdminAPIClient()

    def test_must_return_valid_data(self):
        self.assertEqual(self.disk_client.get_carma(self.valid_uid), VALID_DATA['carma']['user_carma'])

    def test_must_return_invalid_data(self):
        self.assertEqual(self.disk_client.get_carma(self.invalid_uid), '{%s}' % INVALID_DATA['error'])

    @override_settings(DISK_ADMIN_API_LOGIN='test-login', DISK_ADMIN_API_PASSWORD='test-pass')
    def test_must_send_get_request_with_login_and_password(self):
        mocked_auth = Mock()
        with patch('events.common_app.disk_admin.api_client.HTTPBasicAuth', mocked_auth):
            self.disk_client._make_request(url='', params={})

        self.assertEqual(mocked_requests.get.call_count, 1)

        msg = 'Клиент API админки диска должен ходить с логином и паролем'
        self.assertEqual(call('test-login', 'test-pass'), mocked_auth.call_args, msg=msg)
