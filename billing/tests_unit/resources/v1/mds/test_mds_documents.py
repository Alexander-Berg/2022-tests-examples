# -*- coding: utf-8 -*-
from __future__ import absolute_import
from __future__ import division
from __future__ import unicode_literals

from future import standard_library

standard_library.install_aliases()

import pytest
import hamcrest as hm
import http.client as http

from brest.core.tests import security

from yb_snout_api.tests_unit.base import TestCaseApiAppBase
# noinspection PyUnresolvedReferences
from yb_snout_api.tests_unit.fixtures.mds import create_report
# noinspection PyUnresolvedReferences
from yb_snout_api.tests_unit.fixtures.permissions import create_admin_role


@pytest.mark.smoke
class TestCaseMdsDocuments(TestCaseApiAppBase):
    BASE_API = u'/v1/mds/documents/'

    def test_get_as_admin(self, admin_role, report):
        security.set_roles([admin_role])
        res = self.test_client.get(
            self.BASE_API + report.key,
            is_admin=False,
        )
        hm.assert_that(res.status_code, hm.equal_to(http.OK))

        hm.assert_that(
            res.headers,
            hm.has_items(
                hm.contains('X-Snout-Accel-Redirect', hm.ends_with('/get-balance/%s' % report.key)),
                hm.contains('Content-Disposition', 'attachment; filename="%s"' % report.key.split('/')[-1]),
            ),
        )

    def test_get_as_owner(self):
        report = create_report(passport_id=self.test_session.passport.passport_id)

        security.set_roles([])
        res = self.test_client.get(
            self.BASE_API + report.key,
            {'key': report.key},
            is_admin=False,
        )
        hm.assert_that(res.status_code, hm.equal_to(http.OK))

        hm.assert_that(
            res.headers,
            hm.has_items(
                hm.contains('X-Snout-Accel-Redirect', hm.ends_with('/get-balance/%s' % report.key)),
                hm.contains('Content-Disposition', 'attachment; filename="%s"' % report.key.split('/')[-1]),
            ),
        )

    def test_forbidden(self, report):
        security.set_roles([])
        res = self.test_client.get(
            self.BASE_API + report.key,
            {'key': report.key},
            is_admin=False,
        )
        hm.assert_that(res.status_code, hm.equal_to(http.FORBIDDEN))

    def test_not_found(self):
        res = self.test_client.get(
            self.BASE_API,
            {'key': 'stay_home_2019'},
            is_admin=False,
        )
        hm.assert_that(res.status_code, hm.equal_to(http.NOT_FOUND))
