# -*- coding: utf-8 -*-
from __future__ import absolute_import
from __future__ import division
from __future__ import unicode_literals

from future import standard_library

standard_library.install_aliases()


import http.client as http
import hamcrest as hm
import pytest

from balance import constants as cst
from tests.tutils import mock_transactions

from brest.core.tests import security
from yb_snout_api.tests_unit.base import TestCaseApiAppBase
# noinspection PyUnresolvedReferences
from yb_snout_api.tests_unit.fixtures.client import create_client, create_role_client
# noinspection PyUnresolvedReferences
from yb_snout_api.tests_unit.fixtures.permissions import create_role, create_admin_role


@pytest.fixture(name='ov_role')
def create_ov_role():
    return create_role((cst.PermissionCode.SET_CLIENT_OVERDRAFT_BAN, {cst.ConstraintTypes.client_batch_id: None}))


@pytest.mark.smoke
class TestCaseSetOverdraftBan(TestCaseApiAppBase):
    BASE_API = '/v1/client/set-overdraft-ban'

    @pytest.mark.parametrize(
        'match_client, ans',
        [
            pytest.param(True, http.OK, id='all clients in role'),
            pytest.param(False, http.FORBIDDEN, id='forbidden'),
        ],
    )
    def test_remove_overdraft_ban(self, admin_role, ov_role, client, match_client, ans):
        from yb_snout_api.resources.v1.client import enums

        client.overdraft_ban = 1
        self.test_session.flush()

        client_batch_id = create_role_client(client=client if match_client else None).client_batch_id
        roles = [
            admin_role,
            (ov_role, {cst.ConstraintTypes.client_batch_id: client_batch_id}),
        ]

        security.set_roles(roles)
        with mock_transactions():
            res = self.test_client.secure_post(
                self.BASE_API,
                {
                    'ban_status': enums.ClientOverdraftBan.ALLOWED.name,
                    'client_id': client.id,
                },
            )
        hm.assert_that(res.status_code, hm.equal_to(ans))
        hm.assert_that(client.overdraft_ban, hm.equal_to(0 if match_client else 1))
