# -*- coding: utf-8 -*-
from __future__ import absolute_import
from __future__ import division

from builtins import int
from future import standard_library

standard_library.install_aliases()

import http.client as http
import pytest
from mock import patch
from hamcrest import assert_that, equal_to, none, has_entries

from brest.core.typing import PassportId, ObjectId
from yb_snout_api.tests_unit.base import TestCaseApiAppBase
# noinspection PyUnresolvedReferences
from brest.core.tests.fixtures.security import (
    generate_oper_id,
    generate_yandex_uid,
    generate_session_id,
    generate_session_id2,
)
from yb_snout_api.utils.security import csrf, secure_auth


@pytest.mark.smoke
class TestCaseSecurity(TestCaseApiAppBase):
    def test_csrf_valid(self, oper_id, yandex_uid):
        from muzzle.security import csrf as muzzle_csrf

        csrf_token = muzzle_csrf.get_secret_key(oper_id, yandex_uid)
        is_token_valid = csrf.check_csrf(oper_id, yandex_uid, csrf_token)

        assert_that(is_token_valid)

    def test_csrf_invalid(self, oper_id, yandex_uid):
        from muzzle.security import csrf as muzzle_csrf

        # Swap args
        csrf_token = muzzle_csrf.get_secret_key(yandex_uid, oper_id)
        is_token_valid = csrf.check_csrf(oper_id, yandex_uid, csrf_token)

        assert_that(not is_token_valid)
        assert_that(not csrf.check_csrf(oper_id, yandex_uid, None))

    def test_csrf_send_feedback_without_token(self):
        response = self.test_client.post('/v1/common/feedback/send-error')

        expected_data = {
            'description': 'Specified CSRF token is not valid',
            'error': 'SnoutInvalidCSRFTokenException',
        }

        assert_that(response.status_code, equal_to(http.FORBIDDEN), 'response code must be FORBIDDEN')
        assert_that(response.get_json(), has_entries(expected_data))

    def test_csrf_send_feedback_with_token(self):
        with patch('muzzle.api.common.send_error_report', return_value=None):
            response = self.test_client.secure_post(
                '/v1/common/feedback/send-error',
                data={},
            )

        assert_that(response.status_code, equal_to(http.OK), 'response code must be OK')
        assert_that(response.get_json().get('data'), none())

    def test_secure_auth_valid(self, oper_id, yandex_uid, session_id, session_id2):
        # type: (str, str, str, str) -> None
        from muzzle.security import sauth as muzzle_sauth

        passport_id = PassportId(ObjectId(int(oper_id)))
        sauth_token = muzzle_sauth.get_secret_key(passport_id, session_id, session_id2, yandex_uid)
        is_token_valid = muzzle_sauth.check_secret_key(passport_id, session_id, session_id2, yandex_uid, sauth_token)

        assert_that(is_token_valid)

    def test_secure_auth_invalid(self, oper_id, yandex_uid, session_id, session_id2):
        # type: (str, str, str, str) -> None
        from muzzle.security import sauth as muzzle_sauth

        passport_id = PassportId(ObjectId(int(oper_id)))
        # Shuffle args
        sauth_token = muzzle_sauth.get_secret_key(passport_id, yandex_uid, session_id2, session_id)
        is_token_valid = muzzle_sauth.check_secret_key(passport_id, session_id, session_id2, yandex_uid, sauth_token)

        assert_that(not is_token_valid)
