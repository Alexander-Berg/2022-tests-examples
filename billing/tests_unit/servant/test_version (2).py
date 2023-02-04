# -*- coding: utf-8 -*-
from __future__ import absolute_import
from __future__ import division
from __future__ import unicode_literals

from future import standard_library

standard_library.install_aliases()

import http.client as http
import responses
import hamcrest as hm

from brest.snout_version import VERSION
from yb_snout_proxy.tests_unit.base import TestCaseProxyAppBase


class TestVersion(TestCaseProxyAppBase):
    BASE_API = '/version'

    @responses.activate
    def test_get_version(self):
        res = self.test_client.get(self.BASE_API)
        hm.assert_that(res.status_code, hm.equal_to(http.OK))

        hm.assert_that(res.response, hm.contains('<UNDEFINED>'))
        hm.assert_that(
            res.headers,
            hm.contains_inanyorder(
                hm.contains('Content-Type', 'text/html; charset=utf-8'),
                hm.contains('Content-Length', '11'),
            ),
        )
