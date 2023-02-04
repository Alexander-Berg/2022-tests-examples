# -*- coding: utf-8 -*-
import hashlib
import urllib.parse

from unittest.mock import Mock, call

from django.conf import settings
from django.test import TestCase

from events.common_app.kinopoisk import KinopoiskAPIClient


class TestKinopoiskMockAPIClient(TestCase):
    def test_must_return_xuid_param_with_mixed_secret_key(self):
        test_uid = '0'
        xuid = KinopoiskAPIClient()._get_xuid_param(test_uid)
        exp_xuid = '%s:%s' % (test_uid, hashlib.md5((test_uid + settings.KINOPOISK_API_SECRET).encode('utf-8')).hexdigest())
        self.assertEqual(xuid, exp_xuid)

    def test_get_user_profile_data_should_call_make_request_with_xuid_param(self):
        test_uid = '0'
        mocked_request = Mock(return_value=(None, None))
        kp = KinopoiskAPIClient()
        kp._make_get_request = mocked_request
        kp.get_user_profile_data(test_uid)
        self.assertEqual(mocked_request.call_count, 1)
        exp_xuid = '%s:%s' % (test_uid, hashlib.md5((test_uid + settings.KINOPOISK_API_SECRET).encode('utf-8')).hexdigest())
        exp_call = call(
            '0',
            urllib.parse.urljoin(settings.KINOPOISK_API_URL, 'users'),
            {'xUid': exp_xuid},
            timeout=60,
            use_cache=True,
        )
        self.assertEqual(mocked_request.call_args, exp_call)
