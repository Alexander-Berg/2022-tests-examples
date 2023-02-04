# -*- coding: utf-8 -*-
from __future__ import absolute_import
from __future__ import division

from future import standard_library

standard_library.install_aliases()

import http.client as http
import pytest
from hamcrest import assert_that, equal_to

from balance import mapper

from brest.core.tests import security
from yb_snout_api.tests_unit.base import TestCaseApiAppBase
# noinspection PyUnresolvedReferences
from yb_snout_api.tests_unit.fixtures.act import create_act
# noinspection PyUnresolvedReferences
from yb_snout_api.tests_unit.fixtures.client import create_client
# noinspection PyUnresolvedReferences
from yb_snout_api.tests_unit.fixtures.permissions import create_admin_role, create_support_role


@pytest.mark.smoke
class TestCaseSetGoodDebt(TestCaseApiAppBase):
    BASE_API = u'/v1/act/set-good-debt'

    @pytest.mark.parametrize(
        'mark',
        [True, False],
    )
    def test_set_debt(self, act, mark):
        response = self.test_client.secure_post(
            self.BASE_API,
            data={
                'act_id': act.id,
                'good_debt_mark': mark,
            },
        )
        assert_that(response.status_code, equal_to(http.OK), 'response code must be OK')
        assert_that(act.good_debt, equal_to(mark))

    @pytest.mark.parametrize(
        'w_role',
        [True, False],
    )
    def test_permission(self, client, admin_role, support_role, w_role):
        roles = [admin_role]
        if w_role:
            roles.append(support_role)
        security.set_roles(roles)

        act = create_act(client=client)
        response = self.test_client.secure_post(
            self.BASE_API,
            data={
                'act_id': act.id,
                'good_debt_mark': True,
            },
        )
        assert_that(response.status_code, equal_to(http.OK if w_role else http.FORBIDDEN))
