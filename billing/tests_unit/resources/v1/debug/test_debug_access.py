# -*- coding: utf-8 -*-
from __future__ import absolute_import
from __future__ import division

from future import standard_library

standard_library.install_aliases()

import pytest
import http.client as http
import hamcrest as hm

from balance import constants as cst

from brest.core.tests import security
from yb_snout_api.tests_unit.base import TestCaseApiAppBase
# noinspection PyUnresolvedReferences
from yb_snout_api.tests_unit.fixtures.permissions import create_role, create_admin_role


@pytest.fixture(name='view_dev_data_role')
def create_view_dev_data_role():
    return create_role(cst.PermissionCode.VIEW_DEV_DATA)


@pytest.mark.parametrize(
    'url, params',
    [
        pytest.param('/v1/debug/dump', {}, id='/dump'),
        pytest.param('/v1/debug/raise-exception', {'level': 3, 'text': 'test test test'}, id='/raise-exception'),
        pytest.param('/v1/debug/resource/list', {}, id='/resource/list'),
        pytest.param('/v1/debug/resource/list/xls', {'filename': 'file.xls'}, id='/resource/list/xls'),
        pytest.param('/v1/debug/stats', {}, id='/stats'),
    ],
)
@pytest.mark.parametrize(
    'w_role',
    [
        pytest.param(True, id='w_role'),
        pytest.param(False, id='wo_role'),
    ],
)
@pytest.mark.parametrize(
    'is_admin',
    [
        pytest.param(True, id='admin ui'),
        pytest.param(False, id='client ui'),
    ],
)
class TestCaseDebugAccess(TestCaseApiAppBase):

    def test_get_from_admin_ui(
            self,
            url,
            params,
            admin_role,
            view_dev_data_role,
            w_role,
            is_admin,
    ):
        roles = [admin_role]
        if w_role:
            roles.append(view_dev_data_role)

        security.set_roles(roles)
        response = self.test_client.get(url, params, is_admin=is_admin)

        if not w_role:
            res = http.FORBIDDEN
        elif url in ('/v1/debug/raise-exception', '/v1/debug/stats'):
            res = http.INTERNAL_SERVER_ERROR
        else:
            res = http.OK
        hm.assert_that(response.status_code, hm.equal_to(res))
