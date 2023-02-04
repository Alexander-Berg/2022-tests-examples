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
from yb_snout_api.tests_unit.fixtures.person import create_person
# noinspection PyUnresolvedReferences
from yb_snout_api.tests_unit.fixtures.permissions import (
    create_admin_role,
    create_view_client_role,
)
# noinspection PyUnresolvedReferences
from yb_snout_api.tests_unit.fixtures.invoice import create_invoice, create_custom_invoice

PAYSYS_ID_BANK_KZ = 1021


class TestCaseClientPaymentMethods(TestCaseApiAppBase):
    BASE_API = u'/v1/client/currencies'

    @pytest.mark.parametrize(
        'is_admin',
        [True, False],
    )
    def test_client_currencies(self, client, is_admin):
        _invoice_rub = create_custom_invoice(client=client)
        _invoice_kzt = create_custom_invoice(
            client=client,
            person=create_person(client=client, type='kzu'),
            product_id=cst.DIRECT_PRODUCT_KZT_ID,
            firm_id=cst.FirmId.UBER_KZ,
            paysys_id=PAYSYS_ID_BANK_KZ,
        )

        if not is_admin:
            security.set_roles([])
            security.set_passport_client(client)

        res = self.test_client.get(
            self.BASE_API,
            {'client_id': client.id} if is_admin else {},
            is_admin=is_admin,
        )
        hm.assert_that(res.status_code, hm.equal_to(http.OK))

        data = res.get_json()['data']
        hm.assert_that(
            data,
            hm.has_entries({
                'items': hm.contains_inanyorder(
                    hm.has_entries({'iso_code': 'RUB', 'iso_num_code': 643}),
                    hm.has_entries({'iso_code': 'KZT', 'iso_num_code': 398}),
                ),
                'total_count': 2,
            }),
        )

    def test_nobody(self, client):
        security.set_roles([])
        res = self.test_client.get(self.BASE_API, is_admin=False)
        hm.assert_that(res.status_code, hm.equal_to(http.NOT_FOUND))

