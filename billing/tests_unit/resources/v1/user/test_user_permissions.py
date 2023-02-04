# -*- coding: utf-8 -*-
from __future__ import absolute_import
from __future__ import division
from __future__ import unicode_literals

from future import standard_library

standard_library.install_aliases()

import pytest
import http.client as http
from hamcrest import (
    assert_that,
    equal_to,
    contains_inanyorder,
    has_entries,
)

from brest.core.tests import security
from yb_snout_api.tests_unit.base import TestCaseApiAppBase
# noinspection PyUnresolvedReferences
from yb_snout_api.tests_unit.fixtures.permissions import create_admin_role


class TestCaseUserPermissions(TestCaseApiAppBase):
    BASE_URL = '/v1/user/permissions'

    @pytest.mark.permissions
    @pytest.mark.parametrize('is_admin', [True, False])
    def test_get_permissions(self, is_admin, admin_role):
        security.set_roles([admin_role])
        response = self.test_client.get(self.BASE_URL, is_admin=is_admin)

        assert_that(response.status_code, equal_to(http.OK), 'response code must be OK')

        response_json = response.get_json().get('data')
        assert_that(
            response_json,
            contains_inanyorder(
                *[
                    has_entries({'id': perm.perm, 'name': perm.name, 'code': perm.code})
                    for perm in admin_role.permissions
                ]  # noqa: C815
            ),
        )
