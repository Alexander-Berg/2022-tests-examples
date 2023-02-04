# -*- coding: utf-8 -*-
from __future__ import absolute_import
from __future__ import division
from __future__ import unicode_literals

from future import standard_library
standard_library.install_aliases()

import http.client as http
import pytest
from hamcrest import assert_that, equal_to
from werkzeug.http import parse_cookie

from yb_snout_api.tests_unit.base import TestCaseApiAppBase
from yb_snout_api.tests_unit.fixtures.base import DOMAIN
from yb_snout_api.utils.security import csrf as security_csrf
# noinspection PyUnresolvedReferences
from brest.core.tests.fixtures.security import (
    generate_yandex_uid,
)


@pytest.mark.smoke
class TestCaseSetCSRFTokenToCookie(TestCaseApiAppBase):
    BASE_API = u'/v1/common/hello'

    def test_set_csrf_cookie(self, yandex_uid):
        self.test_client.set_cookie(DOMAIN, "yandexuid", yandex_uid)
        response = self.test_client.get(
            self.BASE_API,
            data={},
            headers={'HOST': DOMAIN},
            is_admin=False,
        )

        assert_that(response.status_code, equal_to(http.OK), 'response code must be OK')

        csrf_cookie = [
            parse_cookie(cookie)
            for cookie in response.headers.getlist('Set-Cookie')
            if "_csrf" in parse_cookie(cookie)
        ][0]

        assert_that(
            security_csrf.check_csrf(
                self.test_session.oper_id,
                yandex_uid,
                csrf_cookie["_csrf"],
            ),
            "Invalid CSRF token",
        )
