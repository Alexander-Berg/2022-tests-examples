# -*- coding: utf-8 -*-
from __future__ import absolute_import
from __future__ import division

from future import standard_library

standard_library.install_aliases()

import pytest
import hamcrest as hm
import http.client as http

from balance import constants as cst

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
from yb_snout_api.tests_unit.fixtures.invoice import create_invoice


class TestCaseUserPaysyses(TestCaseApiAppBase):
    BASE_API = u'/v1/client/paysyses'

    @pytest.mark.parametrize(
        'is_admin',
        [True, False],
    )
    def test_base_test(self, admin_role, view_client_role, invoice, is_admin):
        client = invoice.client
        paysys = invoice.paysys

        if is_admin:
            role_client = create_role_client(client)
            roles = [
                admin_role,
                (view_client_role, {cst.ConstraintTypes.client_batch_id: role_client.client_batch_id}),
            ]
            security.set_roles(roles)
        else:
            security.set_roles([])
            security.set_passport_client(client)

        res = self.test_client.get(
            self.BASE_API,
            params={'client_id': client.id},
            is_admin=False,
        )
        hm.assert_that(res.status_code, hm.equal_to(http.OK))

        data = res.get_json()['data']
        hm.assert_that(
            data,
            hm.has_entries({
                'items': hm.contains(
                    hm.has_entries({
                        'id': paysys.id,
                        'name': paysys.name,
                        'char_code': paysys.cc,
                        'weight': paysys.weight,
                    }),
                ),
                'total_count': 1,
            }),
        )

    def test_nobody(self, client):
        security.set_roles([])
        res = self.test_client.get(
            self.BASE_API,
            {'client_id': client.id},
            is_admin=False,
        )
        hm.assert_that(res.status_code, hm.equal_to(http.FORBIDDEN))
