# -*- coding: utf-8 -*-
from __future__ import absolute_import
from __future__ import division

from future import standard_library

standard_library.install_aliases()

import pytest
import hamcrest as hm
import http.client as http

from brest.core.tests import security
from yb_snout_api.tests_unit.base import TestCaseApiAppBase
# noinspection PyUnresolvedReferences
from yb_snout_api.tests_unit.fixtures.client import create_client, create_role_client
# noinspection PyUnresolvedReferences
from yb_snout_api.tests_unit.fixtures.permissions import (
    create_admin_role,
    create_view_client_role,
)
# noinspection PyUnresolvedReferences
from yb_snout_api.tests_unit.fixtures.order import create_order


class TestCaseserviceGroups(TestCaseApiAppBase):
    BASE_API = u'/v1/service/groups'

    @pytest.mark.parametrize(
        'is_admin, w_passport',
        [
            pytest.param(True, True, id='admin w passport'),
            pytest.param(True, False, id='admin wo passport'),
            pytest.param(False, False, id='client'),
        ],
    )
    def test_base_test(self, admin_role, view_client_role, order, is_admin, w_passport):
        passport = self.test_session.passport
        service_group = order.service.group

        roles = []
        if is_admin:
            roles.append(admin_role)
        security.set_passport_client(order.client)
        security.set_roles(roles)

        res = self.test_client.get(
            self.BASE_API,
            params={'passport_id': passport.passport_id} if w_passport else {},
            is_admin=is_admin,
        )
        hm.assert_that(res.status_code, hm.equal_to(http.OK))

        data = res.get_json()['data']
        if is_admin and not w_passport:
            hm.assert_that(
                data,
                hm.has_entries({
                    'total_count': hm.greater_than_or_equal_to(22),
                }),
            )
        else:
            hm.assert_that(
                data,
                hm.has_entries({
                    'items': hm.contains(
                        hm.has_entries({
                            'id': service_group.id,
                            'name': service_group.name,
                            'group_code': service_group.group_code,
                        }),
                    ),
                    'total_count': 1,
                }),
            )
